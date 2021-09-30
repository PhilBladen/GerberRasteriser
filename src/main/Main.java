package main;

import static main.Utils.err;
import static main.Utils.log;

import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
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
import main.GeometricPrimitives.Coordinate;
import main.Utils.Timer;

public class Main
{
	private final String APP_NAME = "Gerber Rasteriser";
	private final String APP_VERSION = "v0.1";

	private HashMap<String, Aperture> apertureDictionary = new HashMap<>();
	private HashMap<String, Aperture> apertureTemplateDictionary = new HashMap<>();

	private final Pattern formatSpecificationPattern = Pattern.compile("^FS(LA)X([0-9])([0-9])Y([0-9])([0-9])$");
	private final Pattern apertureDefinitionPattern = Pattern.compile("^ADD(\\d\\d+)([A-Z0-9]+)[,]?([+-]?(?:[0-9]*[.])?[0-9]+)?(?:X([+-]?(?:[0-9]*[.])?[0-9]+))?(?:X([+-]?(?:[0-9]*[.])?[0-9]+))?(?:X([+-]?(?:[0-9]*[.])?[0-9]+))?$");
	private final Pattern apertureMacroPattern = Pattern.compile("^AM([A-Z0-9]+)$");;
	private final Pattern coordinatePattern = Pattern.compile("^(?:X([+-]?\\d+))?(?:Y([+-]?\\d+))?(?:I([+-]?\\d+))?(?:J([+-]?\\d+))?(D0\\d)$");
	private final Pattern operationPattern = Pattern.compile("^D(\\d+)$");

	private Coordinate currentPoint = new Coordinate(0, 0);
	private Aperture selectedAperture = null;

	private boolean inRegion = false;
	private InterpolationMode interpolationMode = InterpolationMode.NONE;

	private Renderer renderer;

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

	public static void main(String[] args)
	{
		new Main();
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
		Custom macroAperture = new Custom();
		for (int wordIndex = 1; wordIndex < cmd.words.size(); wordIndex++)
		{
			String word = cmd.words.get(wordIndex);

			if (word.contains("$"))
				continue;
			// throw new RuntimeException("Cry"); // FIXME

			String parts[] = word.split(",");

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

					AffineTransform a = new AffineTransform();
					a.rotate(rotation);
					Point2D newCenter = a.transform(new Point2D.Double(centerX, centerY), null);

					primitive = new GeometricPrimitives.Circle((int) diameter, new Coordinate((int) newCenter.getX(), (int) newCenter.getY()), rotation);

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

					AffineTransform a = new AffineTransform();
					a.rotate(rotation);
					Point2D newStart = a.transform(new Point2D.Double(startX, startY), null);
					Point2D newEnd = a.transform(new Point2D.Double(endX, endY), null);

					primitive = new GeometricPrimitives.VectorLine((int) width, new Coordinate((int) newStart.getX(), (int) newStart.getY()), new Coordinate((int) newEnd.getX(), (int) newEnd.getY()), rotation);

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

					AffineTransform a = new AffineTransform();
					a.rotate(rotation);
					Point2D newCenter = a.transform(new Point2D.Double(centerX, centerY), null);

					primitive = new GeometricPrimitives.Rectangle((int) width, (int) height, new Coordinate((int) newCenter.getX(), (int) newCenter.getY()), rotation);

					break;
				}
				case 4: // Outline
				{
					int numVertices = Integer.parseInt(parts[2]) + 1;
					int expectedNumArgs = 4 + 2 * numVertices;
					if (parts.length != expectedNumArgs)
						throw new RuntimeException("Unexpected number of arguments for outline template.");

					double rotation = Utils.toRads(Double.parseDouble(parts[expectedNumArgs - 1]));

					AffineTransform transform = new AffineTransform();
					transform.rotate(rotation);

					ArrayList<Coordinate> coordinates = new ArrayList<>();

					for (int i = 0; i < numVertices; i++)
					{
						double x = Utils.convertUnits(Double.parseDouble(parts[3 + 2 * i]));
						double y = Utils.convertUnits(Double.parseDouble(parts[4 + 2 * i]));

						Point2D transformedPoint = transform.transform(new Point2D.Double(x, y), null);
						coordinates.add(new Coordinate((int) transformedPoint.getX(), (int) transformedPoint.getY()));
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

					AffineTransform a = new AffineTransform();
					a.rotate(rotation);
					Point2D newCenter = a.transform(new Point2D.Double(centerX, centerY), null);

					primitive = new GeometricPrimitives.Polygon(numVertices, (int) diameter, new Coordinate((int) newCenter.getX(), (int) newCenter.getY()), rotation);

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

					AffineTransform a = new AffineTransform();
					a.rotate(rotation);
					Point2D newCenter = a.transform(new Point2D.Double(centerX, centerY), null);

					primitive = new GeometricPrimitives.Thermal(new Coordinate((int) newCenter.getX(), (int) newCenter.getY()), (int) outerDiameter, (int) innerDiameter, (int) gapThickness, rotation);

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

	private void flash(Coordinate position)
	{
		if (selectedAperture == null)
			return;
		// throw new RuntimeException("Flash requested but no aperture selected.");

		Aperture a = selectedAperture.clone();
		a.offset.x = currentPoint.x;
		a.offset.y = currentPoint.y;
		renderer.apertures.add(a);
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
					aperture = new Aperture.Circle(argsconv[0]);
				else if (numArgs == 2)
					aperture = new Aperture.Circle(argsconv[0], argsconv[1]);
				else
					throw new RuntimeException("Unexpected number of arguments for circle aperture.");
			}
			else if (type.equals("R"))
			{
				if (numArgs == 2)
					aperture = new Aperture.Rectangle(argsconv[0], argsconv[1]);
				else if (numArgs == 3)
					aperture = new Aperture.Rectangle(argsconv[0], argsconv[1], argsconv[2]);
				else
					throw new RuntimeException("Unexpected number of arguments for rectangle aperture.");
			}
			else if (type.equals("O"))
			{
				if (numArgs == 2)
					aperture = new Aperture.ObRound(argsconv[0], argsconv[1]);
				else if (numArgs == 3)
					aperture = new Aperture.ObRound(argsconv[0], argsconv[1], argsconv[2]);
				else
					throw new RuntimeException("Unexpected number of arguments for rectangle aperture.");
			}
			else if (type.equals("P"))
			{
				if (numArgs == 2)
					aperture = new Aperture.Polygon(argsconv[0], (int) args[1]);
				else if (numArgs == 3)
					aperture = new Aperture.Polygon(argsconv[0], (int) args[1], Utils.toRads(args[2]));
				else if (numArgs == 4)
					aperture = new Aperture.Polygon(argsconv[0], (int) args[1], Utils.toRads(args[2]), argsconv[3]);
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
			if (apertureNumber >= 10)
			{
				selectedAperture = apertureDictionary.get(commandWord);
				log("Selected aperture " + commandWord);
			}
			else
			{
				switch (apertureNumber)
				{
					case 1: // Interpolate
						// TODO
						// log("INTERPOLATE.");
						break;
					case 2: // Move
						// log("MOVE.");
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
			// TODO
		}
		else if (commandWord.startsWith("LM"))
		{
			// TODO
		}
		else if (commandWord.startsWith("LR"))
		{
			// TODO
		}
		else if (commandWord.startsWith("LS"))
		{
			// TODO
		}
		else if (commandWord.startsWith("G36"))
		{
			inRegion = true;
			log("Started region");
		}
		else if (commandWord.startsWith("G37"))
		{
			inRegion = false;
			log("Exited region");
		}
		else if (commandWord.startsWith("AB"))
		{
			// TODO
		}
		else if (commandWord.startsWith("SR"))
		{
			// TODO
		}
		else if (commandWord.startsWith("TF"))
		{
			// TODO
		}
		else if (commandWord.startsWith("TA"))
		{
			// TODO
		}
		else if (commandWord.startsWith("TO"))
		{
			// TODO
		}
		else if (commandWord.startsWith("TD"))
		{
			// TODO
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

			// int X = args[0];
			// int Y = args[1];
			// int I = args[2];
			// int J = args[3];

			String operation = m.group(5);
			switch (operation)
			{
				case "D01": // Interpolate
				{
					if (inRegion)
					{
					}
					else
					{
						if (interpolationMode == InterpolationMode.NONE)
							throw new RuntimeException("Interpolation requested but interpolation mode not set.");

						if (!(selectedAperture instanceof Aperture.Circle))
							throw new RuntimeException("Interpolation requested but selected aperture is not a circle.");

						Integer strokeWidth = ((Aperture.Circle) selectedAperture).diameter;

						Integer newX = currentPoint.x;
						if (args[0] != null)
							newX = args[0];

						Integer newY = currentPoint.y;
						if (args[1] != null)
							newY = args[1];

						Integer I = args[2];
						Integer J = args[3];

						if (!renderer.traces.containsKey(strokeWidth))
							renderer.traces.put(strokeWidth, new ArrayList<>());

						if (interpolationMode == InterpolationMode.LINEAR)
						{
							Line2D l = new Line2D.Double(Utils.toPixels(currentPoint.x), Utils.toPixels(currentPoint.y), Utils.toPixels(newX), Utils.toPixels(newY));
							renderer.traces.get(strokeWidth).add(l);
						}
						else if (interpolationMode == InterpolationMode.CIRC_CW)
						{
							// int radius = (int) Math.hypot(I, J);
							//
							// double startAngle_deg = Utils.toDeg(Math.atan2(J, I));
							// double endAngle_deg = Utils.toDeg(Math.atan2(newY - currentPoint.y - J, newX - currentPoint.x - I));
							//
							// Arc2D l = new Arc2D.Double();
							// l.setArcByCenter(Utils.toPixels(currentPoint.x + I), Utils.toPixels(currentPoint.y + J), Utils.toPixels(radius), startAngle_deg - 90, endAngle_deg - startAngle_deg, Arc2D.OPEN);
							// renderer.traces.get(strokeWidth).add(l);
						}
						else if (interpolationMode == InterpolationMode.CIRC_CCW)
						{
							int radius = (int) Math.hypot(I, J);

							double startAngle_deg = Utils.toDeg(Math.atan2(J, I));
							// double endAngle_deg = Utils.toDeg(Math.atan2(J + newY - currentPoint.y, I + newX - currentPoint.x));
							double endAngle_deg = Utils.toDeg(Math.atan2(newY - currentPoint.y - J, newX - currentPoint.x - I));
							double angleSubtended_deg = endAngle_deg - startAngle_deg;
							
//							startAngle_deg = Math.min(startAngle_deg, endAngle_deg)
							
							if (angleSubtended_deg < 0)
								angleSubtended_deg = -angleSubtended_deg;

							Arc2D l = new Arc2D.Double();
							
							if (startAngle_deg < 0)
								startAngle_deg += 360;
							
							if (startAngle_deg >= 0 && startAngle_deg < 180)
							{
								startAngle_deg += 90;
								
							}
							else
							{
								startAngle_deg -= 90;
								
							}

//							l.setArcByCenter(Utils.toPixels(currentPoint.x + I), Utils.toPixels(currentPoint.y + J), Utils.toPixels(radius), startAngle_deg, endAngle_deg - startAngle_deg, Arc2D.OPEN);
							l.setArcByCenter(Utils.toPixels(currentPoint.x + I), Utils.toPixels(currentPoint.y + J), Utils.toPixels(radius), startAngle_deg, angleSubtended_deg, Arc2D.OPEN);
							
							
							log("Arc at (" + Utils.toPixels(currentPoint.x + I) + ", " + Utils.toPixels(currentPoint.y + J) + ") from " + startAngle_deg + " to " + endAngle_deg);
							log("\tcX " + Utils.toPixels(currentPoint.x) + " cY " + Utils.toPixels(currentPoint.y) + " X " + Utils.toPixels(newX) +  " Y " + Utils.toPixels(newY) + " I " + Utils.toPixels(I) + " J " + Utils.toPixels(J));

							renderer.traces.get(strokeWidth).add(l);
						}

						currentPoint.x = newX;
						currentPoint.y = newY;
					}
					break;
				}
				case "D02": // Move
					if (args[0] == null && args[1] == null)
						throw new RuntimeException("Invalid parameters for draw operation: " + commandWord);

					if (args[0] != null)
						currentPoint.x = args[0];
					if (args[1] != null)
						currentPoint.y = args[1];

//					log("MOVE 2: " + currentPoint.x + ", " + currentPoint.y);
					break;
				case "D03": // Flash
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
			log("Reached end of file :)");
		}
		// Deprecated commands:
		else if (commandWord.startsWith("G54"))
		{
			// TODO
		}
		else if (commandWord.startsWith("G55"))
		{
			// TODO
		}
		else if (commandWord.startsWith("G70"))
		{
			setUnitsIN();
		}
		else if (commandWord.startsWith("G71"))
		{
			setUnitsMM();
		}
		else if (commandWord.startsWith("G90"))
		{
			// Ignored
		}
		else if (commandWord.startsWith("G91"))
		{
			throw new RuntimeException("Unsupported command G91.");
		}
		else if (commandWord.startsWith("M00"))
		{
			// TODO
		}
		else if (commandWord.startsWith("M01"))
		{
			// TODO
		}
		else if (commandWord.startsWith("IP"))
		{
			// TODO
		}
		else if (commandWord.startsWith("AS"))
		{
			// TODO
		}
		else if (commandWord.startsWith("IR"))
		{
			// TODO
		}
		else if (commandWord.startsWith("MI"))
		{
			// TODO
		}
		else if (commandWord.startsWith("OF"))
		{
			// TODO
		}
		else if (commandWord.startsWith("SF"))
		{
			// TODO
		}
		else if (commandWord.startsWith("IN"))
		{
			// TODO
		}
		else if (commandWord.startsWith("LN"))
		{
			// TODO
		}
		else if (commandWord.startsWith("G74"))
		{
			// TODO
		}
		else
			err("Skipped " + command);
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

	public Main()
	{
		log("Running " + APP_NAME + " " + APP_VERSION);

		renderer = new Renderer();
		renderer.createWindow();

		Timer.tic();
		ArrayList<Command> commands = parseGerberFile(new File("Reference files/STAR-XL CCT.GTL"));
		// ArrayList<Command> commands = parseGerberFile(new File("E:/PJB/Programming/Java/Workspace/GerberRasteriser/Reference files/GerberTest/GerberFiles/copper_top.gbr"));
		log("Parsed gerber file in " + String.format("%.2fs.", Timer.toc() * 0.001) + "\n");

		Timer.tic();
		for (Command command : commands)
			processCommand(command);
		log("Processed gerber file in " + String.format("%.2fs.", Timer.toc() * 0.001) + "\n");

		renderer.redraw();

		log("Complete!");
	}
}
