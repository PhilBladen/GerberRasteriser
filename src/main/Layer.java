package main;

import static main.Utils.err;
import static main.Utils.log;
import static main.Utils.warn;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import main.Aperture.Custom;
import main.Config.UnitType;
import main.Layer.Modifiers.Mirroring;
import main.Layer.Modifiers.Polarity;
import main.Utils.Timer;

public class Layer
{
	private HashMap<String, Aperture> apertureDictionary = new HashMap<>();
	private HashMap<String, Aperture> apertureTemplateDictionary = new HashMap<>();

	private final Pattern formatSpecificationPattern = Pattern.compile("^FS(LA)X([0-9])([0-9])Y([0-9])([0-9])$");
	private final Pattern apertureDefinitionPattern = Pattern.compile("^ADD(\\d\\d+)([A-Z0-9]+)[,]?([+-]?(?:[0-9]*[.])?[0-9]+)?(?:X([+-]?(?:[0-9]*[.])?[0-9]+))?(?:X([+-]?(?:[0-9]*[.])?[0-9]+))?(?:X([+-]?(?:[0-9]*[.])?[0-9]+))?$");
	private final Pattern apertureMacroPattern = Pattern.compile("^AM([A-Z0-9]+)$");;
	private final Pattern coordinatePattern = Pattern.compile("^(?:X([+-]?\\d+))?(?:Y([+-]?\\d+))?(?:I([+-]?\\d+))?(?:J([+-]?\\d+))?(D0\\d)$");
	private final Pattern operationPattern = Pattern.compile("^D(\\d+)$");
	private final Pattern LRPattern = Pattern.compile("^LR([+-]?(?:[0-9]*[.])?[0-9]+)$");
	private final Pattern LSPattern = Pattern.compile("^LS([+-]?(?:[0-9]*[.])?[0-9]+)$");

	/** State variables: */
	private Vector2i currentPoint = new Vector2i(0, 0);
	private Aperture selectedAperture = null;
	private ArrayList<Shape> currentRegion = null;
	private boolean inRegion = false;
	private InterpolationMode interpolationMode = InterpolationMode.NONE;
	private Modifiers globalModifiers = new Modifiers();

	public ArrayList<Renderable> objects = new ArrayList<>();

	public Layer(File file)
	{
		Timer.tic();
		ArrayList<Command> commands = parseGerberFile(file);
		log("Parsed " + file.getName() + " in " + String.format("%.2fs.", Timer.toc() * 0.001) + "\n");

		Timer.tic();
		for (Command command : commands)
			processCommand(command);
		log("Processed " + file.getName() + " in " + String.format("%.2fs.", Timer.toc() * 0.001) + "\n");
	}

	public static class Modifiers
	{
		Polarity polarity = Polarity.DARK;
		Mirroring mirroring = Mirroring.NONE;
		double rotation = 0.0;
		double scaling = 0.0;

		// FIXME add mirroring, rotation and scaling functionality

		public Modifiers()
		{
		}

		public Modifiers(Polarity p, Mirroring m, double r, double s)
		{
			polarity = p;
			mirroring = m;
			rotation = r;
			scaling = s;
		}

		public enum Polarity
		{
			CLEAR, DARK
		}

		public enum Mirroring
		{
			NONE, X, Y, XY
		}

		public Modifiers clone()
		{
			Modifiers newModifiers = new Modifiers();
			newModifiers.polarity = polarity;
			newModifiers.mirroring = mirroring;
			newModifiers.rotation = rotation;
			newModifiers.scaling = scaling;
			return newModifiers;
		}
	}

	public class Interpolation implements Renderable
	{
		private Modifiers modifiers;
		private Shape s;
		private int thickness;

		public Interpolation(Modifiers m)
		{
			this.modifiers = m;
		}

		@Override
		public void render(Graphics2D g)
		{
			if (modifiers.polarity == Polarity.DARK)
				g.setColor(Color.WHITE);
			else
				g.setColor(Color.BLACK);

			g.setStroke(new BasicStroke((float) Utils.toPixels(thickness), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g.draw(s);
		}

		@Override
		public void setModifiers(Modifiers m)
		{
			modifiers = m;
		}
	}

	public class Region implements Renderable
	{
		private Modifiers modifiers;
		private Path2D p;

		public Region(Path2D p, Modifiers modifiers)
		{
			this.p = p;
			this.modifiers = modifiers;
		}

		@Override
		public void render(Graphics2D g)
		{
			if (modifiers.polarity == Polarity.DARK)
				g.setColor(Color.WHITE);
			else
				g.setColor(Color.BLACK);

			// g.setStroke(new BasicStroke((float) 1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g.fill(p);
		}

		@Override
		public void setModifiers(Modifiers m)
		{
			modifiers = m;
		}
	}

	private enum InterpolationMode
	{
		NONE, LINEAR, CIRC_CW, CIRC_CCW
	}

	private class Command
	{
		ArrayList<String> words = new ArrayList<>();

		Command(String word)
		{
			words.add(word);
		}

		private Command()
		{
		}

		@Override
		public String toString()
		{
			return "Command[" + words.get(0) + "]";
		}
	}

	private class ExtendedCommand extends Command
	{
		void addWord(String word)
		{
			words.add(word);
		}

		@Override
		public String toString()
		{
			String s = "ExtCmd[" + words.get(0) + "]:";
			for (int i = 1; i < words.size(); i++)
			{
				s += "\r\n\t";
				s += words.get(i);
			}
			return s;
		}
	}

	private enum ParserState
	{
		NONE, IN_EXT_CMD, IN_WORD_CMD
	}

	private void setUnitsMM()
	{
		log("Units mm selected.");

		Config.getConfig().units = UnitType.MM;
	}

	private void setUnitsIN()
	{
		log("Units inches selected.");

		Config.getConfig().units = UnitType.IN;
	}

	private void addApertureTemplate(ExtendedCommand cmd, String ID)
	{
		Custom macroAperture = new Custom(globalModifiers.clone());
		for (int wordIndex = 1; wordIndex < cmd.words.size(); wordIndex++)
		{
			String word = cmd.words.get(wordIndex);

			if (word.contains("$"))
			{
				err("Found unsupported parametric aperture template - unable to process.");
				continue;
			}
			// throw new RuntimeException("Cry"); // FIXME

			String parts[] = word.split(",");

			Matrix2D m = new Matrix2D();

			int code = Integer.parseInt(parts[0]);
			Area primitive = null;
			switch (code)
			{
				case 0: // Comment
					break;
				case 1: // Circle
				{
					if (parts.length != 5 && parts.length != 6)
						throw new RuntimeException();

					double diameter = Utils.convertUnits(Double.parseDouble(parts[2]));
					double centerX = Utils.convertUnits(Double.parseDouble(parts[3]));
					double centerY = Utils.convertUnits(Double.parseDouble(parts[4]));
					double rotation;
					if (parts.length == 5)
						rotation = 0;
					else
						rotation = Utils.toRads(Double.parseDouble(parts[5]));

					m.rotate(rotation);
					Vector2i newCenter = m.transform(new Vector2i(centerX, centerY));

					primitive = new GeometricPrimitives.Circle((int) diameter, newCenter, rotation);

					break;
				}
				case 20: // Vector line
				{
					if (parts.length != 8)
						throw new RuntimeException();

					double width = Utils.convertUnits(Double.parseDouble(parts[2]));
					double startX = Utils.convertUnits(Double.parseDouble(parts[3]));
					double startY = Utils.convertUnits(Double.parseDouble(parts[4]));
					double endX = Utils.convertUnits(Double.parseDouble(parts[5]));
					double endY = Utils.convertUnits(Double.parseDouble(parts[6]));
					double rotation = Utils.toRads(Double.parseDouble(parts[7]));

					m.rotate(rotation);
					Vector2i newStart = m.transform(new Vector2i(startX, startY));
					Vector2i newEnd = m.transform(new Vector2i(endX, endY));

					primitive = new GeometricPrimitives.VectorLine((int) width, newStart, newEnd, rotation);

					break;
				}
				case 21: // Rectangle
				{
					if (parts.length != 7)
						throw new RuntimeException();

					double width = Utils.convertUnits(Double.parseDouble(parts[2]));
					double height = Utils.convertUnits(Double.parseDouble(parts[3]));
					double centerX = Utils.convertUnits(Double.parseDouble(parts[4]));
					double centerY = Utils.convertUnits(Double.parseDouble(parts[5]));
					double rotation = Utils.toRads(Double.parseDouble(parts[6]));

					m.rotate(rotation);
					Vector2i newCenter = m.transform(new Vector2i(centerX, centerY));

					primitive = new GeometricPrimitives.Rectangle((int) width, (int) height, newCenter, rotation);

					break;
				}
				case 4: // Outline
				{
					int numVertices = Integer.parseInt(parts[2]) + 1;
					int expectedNumArgs = 4 + 2 * numVertices;
					if (parts.length != expectedNumArgs)
						throw new RuntimeException("Unexpected number of arguments for outline template.");

					double rotation = Utils.toRads(Double.parseDouble(parts[expectedNumArgs - 1]));
					m.rotate(rotation);

					ArrayList<Vector2i> coordinates = new ArrayList<>();
					for (int i = 0; i < numVertices; i++)
					{
						double x = Utils.convertUnits(Double.parseDouble(parts[3 + 2 * i]));
						double y = Utils.convertUnits(Double.parseDouble(parts[4 + 2 * i]));

						coordinates.add(m.transform(new Vector2i(x, y)));
					}

					primitive = new GeometricPrimitives.Outline(numVertices, coordinates, rotation);

					break;
				}
				case 5: // Polygon
				{
					if (parts.length != 7)
						throw new RuntimeException();

					int numVertices = Integer.parseInt(parts[2]);
					double centerX = Utils.convertUnits(Double.parseDouble(parts[3]));
					double centerY = Utils.convertUnits(Double.parseDouble(parts[4]));
					double diameter = Utils.convertUnits(Double.parseDouble(parts[5]));
					double rotation = Utils.toRads(Double.parseDouble(parts[6]));

					m.rotate(rotation);
					Vector2i newCenter = m.transform(new Vector2i(centerX, centerY));

					primitive = new GeometricPrimitives.Polygon(numVertices, (int) diameter, newCenter, rotation);

					break;
				}
				case 7: // Thermal
				{
					if (parts.length != 7)
						throw new RuntimeException();

					double centerX = Utils.convertUnits(Double.parseDouble(parts[1]));
					double centerY = Utils.convertUnits(Double.parseDouble(parts[2]));
					double outerDiameter = Utils.convertUnits(Double.parseDouble(parts[3]));
					double innerDiameter = Utils.convertUnits(Double.parseDouble(parts[4]));
					double gapThickness = Utils.convertUnits(Double.parseDouble(parts[5]));
					double rotation = Utils.toRads(Double.parseDouble(parts[6]));

					m.rotate(rotation);
					Vector2i newCenter = m.transform(new Vector2i(centerX, centerY));

					primitive = new GeometricPrimitives.Thermal(newCenter, (int) outerDiameter, (int) innerDiameter, (int) gapThickness, rotation);

					break;
				}
				default:
					throw new RuntimeException("Invalid macro primitive code.");
			}

			macroAperture.addPrimitive(primitive, code == 7 || Integer.parseInt(parts[1]) == 1);
		}

		apertureTemplateDictionary.put(ID, macroAperture);
		log("Aperture template " + ID + " added.");
	}

	private void flash(Vector2i position)
	{
		if (selectedAperture == null)
			throw new RuntimeException("Flash requested but no aperture selected.");

		Aperture a = selectedAperture.clone();
		a.offset.x = currentPoint.x;
		a.offset.y = currentPoint.y;
		objects.add(a);
	}

	private boolean isCoordinate(String word)
	{
		return coordinatePattern.matcher(word).matches();
	}

	private void processCommand(Command command)
	{
		String commandWord = command.words.get(0);
		if (commandWord.startsWith("G04")) // Comment
		{
			// Ignore comments
		}
		else if (commandWord.startsWith("MO")) // Mode
		{
			if (commandWord.equals("MOMM"))
			{
				setUnitsMM();
			}
			else if (commandWord.equals("MOIN"))
			{
				setUnitsIN();
			}
			else
				throw new RuntimeException("Unknown mode requested: " + commandWord);
		}
		else if (commandWord.startsWith("FS")) // Format specification
		{
			Matcher m = formatSpecificationPattern.matcher(commandWord);
			if (!m.matches())
				throw new RuntimeException("Illegal format specification: " + commandWord);

			if (!m.group(1).equals("LA"))
				throw new RuntimeException("Unsupported format specification used: " + commandWord);

			int xInteger = Integer.parseInt(m.group(2));
			int xDecimal = Integer.parseInt(m.group(3));
			int yInteger = Integer.parseInt(m.group(4));
			int yDecimal = Integer.parseInt(m.group(5));

			if ((xInteger != yInteger) || (xDecimal != yDecimal))
				throw new RuntimeException("Unsupported format specification: mismatching x and y format.");

			Config.getConfig().multiplier = (int) Math.pow(10, 6 - xDecimal);

			log(String.format("Set format X%d.%d Y%d.%d", xInteger, xDecimal, yInteger, yDecimal));
		}
		else if (commandWord.startsWith("AD")) // Aperture define
		{
			Matcher m = apertureDefinitionPattern.matcher(commandWord);
			if (!m.matches())
				throw new RuntimeException("Illegal aperture definition: " + commandWord);

			int numArgs = Utils.countMatchingGroups(m) - 3;

			String id = "D" + m.group(1);
			log("Aperture " + id + " defined.");
			double args[] = new double[numArgs];
			int argsconv[] = new int[numArgs];
			for (int i = 0; i < numArgs; i++)
			{
				String group = m.group(i + 3);
				args[i] = Double.parseDouble(group);
				argsconv[i] = (int) Utils.convertUnits(args[i]);
			}

			Aperture aperture = null;

			String type = m.group(2);
			if (type.equals("C"))
			{
				if (numArgs == 1)
					aperture = new Aperture.Circle(globalModifiers.clone(), argsconv[0]);
				else if (numArgs == 2)
					aperture = new Aperture.Circle(globalModifiers.clone(), argsconv[0], argsconv[1]);
				else
					throw new RuntimeException("Unexpected number of arguments for circle aperture.");
			}
			else if (type.equals("R"))
			{
				if (numArgs == 2)
					aperture = new Aperture.Rectangle(globalModifiers.clone(), argsconv[0], argsconv[1]);
				else if (numArgs == 3)
					aperture = new Aperture.Rectangle(globalModifiers.clone(), argsconv[0], argsconv[1], argsconv[2]);
				else
					throw new RuntimeException("Unexpected number of arguments for rectangle aperture.");
			}
			else if (type.equals("O"))
			{
				if (numArgs == 2)
					aperture = new Aperture.ObRound(globalModifiers.clone(), argsconv[0], argsconv[1]);
				else if (numArgs == 3)
					aperture = new Aperture.ObRound(globalModifiers.clone(), argsconv[0], argsconv[1], argsconv[2]);
				else
					throw new RuntimeException("Unexpected number of arguments for rectangle aperture.");
			}
			else if (type.equals("P"))
			{
				if (numArgs == 2)
					aperture = new Aperture.Polygon(globalModifiers.clone(), argsconv[0], (int) args[1]);
				else if (numArgs == 3)
					aperture = new Aperture.Polygon(globalModifiers.clone(), argsconv[0], (int) args[1], Utils.toRads(args[2]));
				else if (numArgs == 4)
					aperture = new Aperture.Polygon(globalModifiers.clone(), argsconv[0], (int) args[1], Utils.toRads(args[2]), argsconv[3]);
				else
					throw new RuntimeException("Unexpected number of arguments for rectangle aperture.");
			}
			else
			{
				aperture = apertureTemplateDictionary.get(type);
				if (aperture == null)
					throw new RuntimeException("Aperture template " + type + " requested but not found.");
			}

			apertureDictionary.put(id, aperture);
		}
		else if (commandWord.startsWith("AM")) // Aperture macro
		{
			if (!(command instanceof ExtendedCommand))
				throw new RuntimeException("Illegal AM command found: not extended.");

			ExtendedCommand extCmd = (ExtendedCommand) command;

			Matcher m = apertureMacroPattern.matcher(commandWord);
			if (!m.matches())
				throw new RuntimeException("Illegal aperture macro: " + commandWord);

			String apertureTemplateID = m.group(1);

			addApertureTemplate(extCmd, apertureTemplateID);
		}
		else if (commandWord.startsWith("D")) // Operation
		{
			Matcher m = operationPattern.matcher(commandWord);
			if (!m.matches())
				throw new Exceptions.GerberCommandException("Invalid syntax: " + commandWord);

			int apertureNumber = Integer.parseInt(m.group(1));

			if (inRegion)
			{
				switch (apertureNumber)
				{
					case 1:
						// Achieves nothing; ignore
						break;
					case 2:
						// D02 without coordinates in region mode = end of region

						// Check if valid

						if (currentRegion == null)
						{
							throw new RuntimeException("Attempt to close null region.");
						}

						// Check if contains enough points for a valid closed region
//						if (currentRegion.size() < 3) // TODO CHECK 
//						{
//							throw new RuntimeException("Attempt to close a region with insufficient points.");
//						}

						// Check if closed
						java.awt.geom.Point2D firstPoint = ((Line2D) currentRegion.get(0)).getP1();
						java.awt.geom.Point2D lastPoint = ((Line2D) currentRegion.get(currentRegion.size() - 1)).getP2();

						log("REGION first point: " + firstPoint + ", last point: " + lastPoint + ", BOOL: " + (firstPoint == lastPoint) + ", BOOLX: " + (firstPoint.getX() == lastPoint.getX()) + ", BOOLY: " + (firstPoint.getY() == lastPoint.getY()));

						if ((firstPoint.getX() != lastPoint.getX()) || (firstPoint.getY() != lastPoint.getY()))
						{
							throw new RuntimeException("Attempt to close an open region.");
						}

						Path2D p = new Path2D.Double();

						int i;
						for (i = 0; i < currentRegion.size(); i++)
						{
							if (i == 0)
							{
								p.append(currentRegion.get(i), false);
							}
							else
							{
								p.append(currentRegion.get(i), true);
							}
						}

						objects.add(new Region(p, globalModifiers.clone()));

						currentRegion = null;

						break;
					default:
						throw new RuntimeException("Illegal operation in Region mode: " + commandWord);
				}
			}
			else
			{
				if (apertureNumber >= 10)
				{
					selectedAperture = apertureDictionary.get(commandWord);
					log("Selected aperture " + commandWord);
				}
				else
				{
					switch (apertureNumber)
					{
						case 1: // Interpolate without coordinates: does the same thing as a flash but is technically a line

							if (interpolationMode == InterpolationMode.NONE)
								throw new RuntimeException("Interpolation requested but interpolation mode not set.");

							if (!(selectedAperture instanceof Aperture.Circle))
								throw new RuntimeException("Interpolation requested but selected aperture is not a circle.");

							Integer strokeWidth = ((Aperture.Circle) selectedAperture).diameter;

							Line2D l = new Line2D.Double(Utils.toPixels(currentPoint.x), Utils.toPixels(currentPoint.y), Utils.toPixels(currentPoint.x), Utils.toPixels(currentPoint.y));

							Interpolation t = new Interpolation(globalModifiers.clone());
							t.s = l;
							t.thickness = strokeWidth;
							objects.add(t);

							break;
						case 2: // Move
							// Ignored
							break;
						case 3: // Flash
							flash(currentPoint);
							break;
						default:
							throw new RuntimeException("Invalid draw operation: " + commandWord);
					}
				}
			}
		}
		else if (commandWord.startsWith("G01"))
		{
			interpolationMode = InterpolationMode.LINEAR;
		}
		else if (commandWord.startsWith("G02"))
		{
			interpolationMode = InterpolationMode.CIRC_CW;
		}
		else if (commandWord.startsWith("G03"))
		{
			interpolationMode = InterpolationMode.CIRC_CCW;
		}
		else if (commandWord.startsWith("G75"))
		{
			// Ignored
		}
		else if (commandWord.startsWith("LP"))
		{
			if (commandWord.equals("LPC"))
			{
				globalModifiers.polarity = Polarity.CLEAR;
			}
			else if (commandWord.equals("LPD"))
			{
				globalModifiers.polarity = Polarity.DARK;
			}
			else
				throw new RuntimeException("Invalid LP command: " + commandWord);
		}
		else if (commandWord.startsWith("LM"))
		{
			if (commandWord.equals("LMN"))
			{
				globalModifiers.mirroring = Mirroring.NONE;
			}
			else if (commandWord.equals("LMX"))
			{
				globalModifiers.mirroring = Mirroring.X;
			}
			else if (commandWord.equals("LMY"))
			{
				globalModifiers.mirroring = Mirroring.Y;
			}
			else if (commandWord.equals("LMXY"))
			{
				globalModifiers.mirroring = Mirroring.XY;
			}
			else
				throw new RuntimeException("Invalid LM command: " + commandWord);
		}
		else if (commandWord.startsWith("LR"))
		{
			Matcher m = LRPattern.matcher(commandWord);
			if (!m.matches())
				throw new RuntimeException("Invalid LR command: " + commandWord);

			Double rotation_deg = Double.parseDouble(m.group(1));
			globalModifiers.rotation = Utils.toRads(rotation_deg);
		}
		else if (commandWord.startsWith("LS"))
		{
			Matcher m = LSPattern.matcher(commandWord);
			if (!m.matches())
				throw new RuntimeException("Invalid LS command: " + commandWord);

			Double scale = Double.parseDouble(m.group(1));
			globalModifiers.scaling = scale;
		}
		else if (commandWord.startsWith("G36"))
		{
			inRegion = true;
			currentRegion = new ArrayList<>();
			log("Started region");
		}
		else if (commandWord.startsWith("G37"))
		{
			inRegion = false;

			if (currentRegion != null)
			{
				// Check if contains enough points for a valid closed region
				if (currentRegion.size() < 3)
				{
					throw new RuntimeException("Attempt to close a region with insufficient points.");
				}

				// Check if closed
				java.awt.geom.Point2D firstPoint = ((Line2D) currentRegion.get(0)).getP1();
				java.awt.geom.Point2D lastPoint = ((Line2D) currentRegion.get(currentRegion.size() - 1)).getP2();

				log("REGION first point: " + firstPoint + ", last point: " + lastPoint + ", BOOL: " + (firstPoint == lastPoint) + ", BOOLX: " + (firstPoint.getX() == lastPoint.getX()) + ", BOOLY: " + (firstPoint.getY() == lastPoint.getY()));

				if ((firstPoint.getX() != lastPoint.getX()) || (firstPoint.getY() != lastPoint.getY()))
				{
					throw new RuntimeException("Attempt to close an open region.");
				}

				Path2D p = new Path2D.Double();

				int i;
				for (i = 0; i < currentRegion.size(); i++)
				{
					if (i == 0)
					{
						p.append(currentRegion.get(i), false);
					}
					else
					{
						p.append(currentRegion.get(i), true);
					}
				}

				objects.add(new Region(p, globalModifiers.clone()));

				currentRegion = null;
			}

			inRegion = false;
			log("Exited region");
		}
		else if (commandWord.startsWith("AB"))
		{
			throw new Exceptions.UnsupportedCommandException(commandWord);
		}
		else if (commandWord.startsWith("SR"))
		{
			throw new Exceptions.UnsupportedCommandException(commandWord);
		}
		else if (commandWord.startsWith("TF"))
		{
			// Attribute - don't care
		}
		else if (commandWord.startsWith("TA"))
		{
			// Attribute - don't care
		}
		else if (commandWord.startsWith("TO"))
		{
			// Attribute - don't care
		}
		else if (commandWord.startsWith("TD"))
		{
			// Attribute - don't care
		}
		else if (isCoordinate(commandWord)) // Coordinate
		{
			Matcher m = coordinatePattern.matcher(commandWord);
			if (!m.matches())
				throw new RuntimeException("Invalid draw command: " + commandWord);

			Integer args[] = new Integer[m.groupCount() - 1];
			for (int i = 0; i < m.groupCount() - 1; i++)
			{
				String group = m.group(i + 1);
				if (group == null)
					args[i] = null;
				else
					args[i] = Utils.importCoordinate(Integer.parseInt(group));
			}

			String operation = m.group(5);
			switch (operation)
			{
				case "D01": // Interpolate
				{

					if (interpolationMode == InterpolationMode.NONE)
						throw new RuntimeException("Interpolation requested but interpolation mode not set.");

					Integer strokeWidth = null;

					if (!inRegion)
					{
						if (!(selectedAperture instanceof Aperture.Circle))
							throw new RuntimeException("Interpolation requested but selected aperture is not a circle.");

						strokeWidth = ((Aperture.Circle) selectedAperture).diameter;
					}

					Integer newX = currentPoint.x;
					if (args[0] != null)
						newX = args[0];

					Integer newY = currentPoint.y;
					if (args[1] != null)
						newY = args[1];

					Integer I = args[2];
					Integer J = args[3];

					// if (!inRegion)
					// {
					// // Only handle stroke width if not in a region, else treat hairline
					// if (!renderer.traces.containsKey(strokeWidth))
					// renderer.traces.put(strokeWidth, new ArrayList<>());
					// }

					if (interpolationMode == InterpolationMode.LINEAR)
					{
						Line2D l = new Line2D.Double(Utils.toPixels(currentPoint.x), Utils.toPixels(currentPoint.y), Utils.toPixels(newX), Utils.toPixels(newY));
						if (inRegion)
						{
							currentRegion.add(l);
						}
						else
						{
							Interpolation t = new Interpolation(globalModifiers.clone());
							t.s = l;
							t.thickness = strokeWidth;
							objects.add(t);
						}

					}
					else if (interpolationMode == InterpolationMode.CIRC_CW)
					{
						int radius = (int) Math.hypot(I, J);

						// Both I and J need to be negated to represent the vector to the startpoint FROM the arc centre
						// BUT the Y-coord also needs to be negated back again (!) due opposing coordinate systems of Gerber (+y-up) and Java (+y-down)
						double startAngle_deg = Utils.toDeg(Math.atan2(J, -I));

						// The Y-coord here also needs to be negated per above
						double endAngle_deg = Utils.toDeg(Math.atan2(-(newY - currentPoint.y - J), (newX - currentPoint.x - I)));

						// For CW arcs, endAngle must be greater than startAngle...
						if (endAngle_deg < startAngle_deg)
							endAngle_deg += 360;

						// ...since a positive angle is subtended (note: this should now always be positive by definition).
						double angleSubtended_deg = endAngle_deg - startAngle_deg;

						Arc2D l = new Arc2D.Double();
						l.setArcByCenter(Utils.toPixels(currentPoint.x + I), Utils.toPixels(currentPoint.y + J), Utils.toPixels(radius), startAngle_deg, angleSubtended_deg, Arc2D.OPEN);
						if (inRegion)
						{
							currentRegion.add(l);
						}
						else
						{
							Interpolation t = new Interpolation(globalModifiers.clone());
							t.s = l;
							t.thickness = strokeWidth;
							objects.add(t);
						}
					}
					else if (interpolationMode == InterpolationMode.CIRC_CCW)
					{
						int radius = (int) Math.hypot(I, J);

						// Both I and J need to be negated to represent the vector to the startpoint FROM the arc centre
						// BUT the Y-coord also needs to be negated back again (!) due opposing coordinate systems of Gerber (+y-up) and Java (+y-down)
						double startAngle_deg = Utils.toDeg(Math.atan2(J, -I));

						// The Y-coord here also needs to be negated per above
						double endAngle_deg = Utils.toDeg(Math.atan2(-(newY - currentPoint.y - J), (newX - currentPoint.x - I)));

						// For CCW arcs, endAngle must be less than startAngle...
						if (endAngle_deg > startAngle_deg)
							endAngle_deg -= 360;

						// ...since a negative angle is subtended (note: this should now always be negative by definition).
						double angleSubtended_deg = endAngle_deg - startAngle_deg;

						Arc2D l = new Arc2D.Double();
						l.setArcByCenter(Utils.toPixels(currentPoint.x + I), Utils.toPixels(currentPoint.y + J), Utils.toPixels(radius), startAngle_deg, angleSubtended_deg, Arc2D.OPEN);
						if (inRegion)
						{
							currentRegion.add(l);
						}
						else
						{
							Interpolation t = new Interpolation(globalModifiers.clone());
							t.s = l;
							t.thickness = strokeWidth;
							objects.add(t);
						}

					}

					currentPoint.x = newX;
					currentPoint.y = newY;

					break;
				}
				case "D02": // Move if in region or not in region
					if (args[0] == null && args[1] == null)
						throw new RuntimeException("Invalid parameters for draw operation: " + commandWord);

					if (args[0] != null)
						currentPoint.x = args[0];
					if (args[1] != null)
						currentPoint.y = args[1];
					break;
				case "D03": // Flash
					if (inRegion)
						throw new RuntimeException("Illegal operation in Region mode: " + commandWord);

					if (args[0] == null && args[1] == null)
						throw new RuntimeException("Invalid parameters for flash operation: " + commandWord);

					if (args[0] != null)
						currentPoint.x = args[0];
					if (args[1] != null)
						currentPoint.y = args[1];

					flash(currentPoint);
					break;
				default:
					throw new RuntimeException("Invalid draw operation: " + operation);
			}
		}
		else if (commandWord.startsWith("M02"))
		{
			log("Reached end of file :)"); // TODO check nothing comes after
		}
		// Deprecated commands:
		else if (commandWord.startsWith("G54")) // Select aperture
		{
			// Ignore
		}
		else if (commandWord.startsWith("G55")) // Prepare for flash
		{
			// Ignore
		}
		else if (commandWord.startsWith("G70")) // Set units to inches
		{
			setUnitsIN();
		}
		else if (commandWord.startsWith("G71")) // Set units to mm
		{
			setUnitsMM();
		}
		else if (commandWord.startsWith("G90")) // Set coordinate format absolute
		{
			// Ignore
		}
		else if (commandWord.startsWith("G91")) // Set coordinate format incremental
		{
			throw new Exceptions.UnsupportedCommandException(commandWord);
		}
		else if (commandWord.startsWith("M00")) // Program stop
		{
			// TODO duplicate M02 functionality
		}
		else if (commandWord.startsWith("M01")) // Optional stop
		{
			// Ignore
		}
		else if (commandWord.startsWith("IP")) // Image polarity
		{
			if (commandWord.equals("IPPOS"))
			{
				warn("Deprecated command IP used.");
			}
			else
				throw new Exceptions.UnsupportedCommandException(commandWord);
		}
		else if (commandWord.startsWith("AS")) // Axes correspondence graphics state parameter
		{
			// Ignore
		}
		else if (commandWord.startsWith("IR")) // Image rotation
		{
			// Ignore
		}
		else if (commandWord.startsWith("MI")) // Image mirroring
		{
			// Ignore
		}
		else if (commandWord.startsWith("OF")) // Image offset
		{
			// Ignore
		}
		else if (commandWord.startsWith("SF")) // Scale factor
		{
			// Ignore
		}
		else if (commandWord.startsWith("IN")) // Comment - image filename
		{
			// Ignore
		}
		else if (commandWord.startsWith("LN")) // Comment - load name
		{
			// Ignore
		}
		else if (commandWord.startsWith("G74")) // Single quadrant mode
		{
			// Ignore
		}
		else
			throw new Exceptions.UnsupportedCommandException(commandWord);
	}

	private ArrayList<Command> parseGerberFile(File file)
	{
		if (!file.exists())
			throw new RuntimeException("Failed to open gerber file. Are you sure it exists?");
		log("Found gerber file.");

		ArrayList<Command> commands = new ArrayList<>();
		try (InputStream in = new BufferedInputStream(new FileInputStream(file)))
		{
			int c;
			ParserState parserState = ParserState.NONE;
			String word = "";
			Command currentCommand = null;
			while ((c = in.read()) != -1)
			{
				if (Character.isWhitespace(c))
					continue; // Ignore all whitespace

				if (c == '%')
				{
					if (parserState == ParserState.IN_EXT_CMD)
					{
						parserState = ParserState.NONE;

						if (currentCommand == null || currentCommand.words.size() == 0)
							throw new RuntimeException("Unexpected item in baggage area.");

						commands.add(currentCommand);
						currentCommand = null;
					}
					else if (parserState != ParserState.NONE)
						throw new RuntimeException("Invalid state change. Probably a missing delimiter somewhere...");
					else
						parserState = ParserState.IN_EXT_CMD;
				}
				else if (c == '*')
				{
					if (parserState != ParserState.IN_EXT_CMD)
					{
						currentCommand = new Command(word);
						commands.add(currentCommand);
						currentCommand = null;
					}
					else
					{
						if (currentCommand == null)
							currentCommand = new ExtendedCommand();

						((ExtendedCommand) currentCommand).addWord(word);
					}

					word = "";
				}
				else
				{
					word += (char) c;
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return commands;
	}
}
