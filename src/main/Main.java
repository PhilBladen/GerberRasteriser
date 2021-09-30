package main;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import main.Config.UnitType;
import main.GeometricPrimitives.Coordinate;
import main.GeometricPrimitives.GeometricPrimitive;

import static main.Utils.*;

public class Main
{
	private final String APP_NAME = "Gerber Rasteriser";
	private final String APP_VERSION = "v0.1";

	private HashMap<String, Aperture> apertureDictionary = new HashMap<>();
	private HashMap<String, Aperture> apertureTemplateDictionary = new HashMap<>();
	
	private Coordinate currentPoint = new Coordinate(0, 0);
	private Aperture selectedAperture = null;
	
	private boolean inRegion = false;
	
	private Renderer renderer;

	private static class Timer
	{
		private static long start_ms;

		public static void tic()
		{
			start_ms = System.currentTimeMillis();
		}

		public static long toc()
		{
			return System.currentTimeMillis() - start_ms;
		}
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
		// TODO
	}
	
	private void flash(Coordinate position)
	{
		if (selectedAperture == null)
			return;
//			throw new RuntimeException("Flash requested but no aperture selected.");
		
		for (GeometricPrimitive template : selectedAperture.geometricPrimitives)
		{
			GeometricPrimitive g = template.clone();
			g.offset.x = currentPoint.x;
			g.offset.y = currentPoint.y;
			renderer.primitives.add(g);
		}
		
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
			Pattern p = Pattern.compile("^FS(LA)X([0-9])([0-9])Y([0-9])([0-9])$");
			Matcher m = p.matcher(commandWord);
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
			Pattern p = Pattern.compile("^ADD(\\d\\d+)([A-Z0-9]+)[,]?([+-]?(?:[0-9]*[.])?[0-9]+)?(?:X([+-]?(?:[0-9]*[.])?[0-9]+))?(?:X([+-]?(?:[0-9]*[.])?[0-9]+))?(?:X([+-]?(?:[0-9]*[.])?[0-9]+))?$");
			Matcher m = p.matcher(commandWord);
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
					aperture = new Aperture.Polygon(argsconv[0], (int) args[1], args[2]);
				else if (numArgs == 4)
					aperture = new Aperture.Polygon(argsconv[0], (int) args[1], args[2], argsconv[3]);
				else
					throw new RuntimeException("Unexpected number of arguments for rectangle aperture.");
			}
			else
			{
				// Custom?
			}
			
			if (aperture == null)
			{
//				throw new RuntimeException("Aperture failed to be created.");
				// TODO
			}
			else
				apertureDictionary.put(id, aperture);
		}
		else if (commandWord.startsWith("AM")) // Aperture macro
		{
			if (!(command instanceof ExtendedCommand))
				throw new RuntimeException("Illegal AM command found: not extended.");
			
			ExtendedCommand extCmd = (ExtendedCommand) command;
			
			Pattern p = Pattern.compile("^AM([A-Z0-9]+)$");
			Matcher m = p.matcher(commandWord);
			if (!m.matches())
				throw new RuntimeException("Illegal aperture macro: " + commandWord);
			
			String apertureTemplateID = m.group(1);
			
			addApertureTemplate(extCmd, apertureTemplateID);
		}
		else if (commandWord.startsWith("D"))
		{
			Pattern p = Pattern.compile("^D(\\d+)$");
			Matcher m = p.matcher(commandWord);
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
						log("INTERPOLATE.");
						break;
					case 2: // Move
						log("MOVE.");
						// Ignored
						break;
					case 3: // Flash
						flash(currentPoint);
						log("FLASH.");
						break;
					default:
						throw new RuntimeException("Invalid draw operation: " + commandWord);
				}
			}
		}
		else if (commandWord.startsWith("G01"))
		{

		}
		else if (commandWord.startsWith("G02"))
		{

		}
		else if (commandWord.startsWith("G03"))
		{

		}
		else if (commandWord.startsWith("G75"))
		{

		}
		else if (commandWord.startsWith("LP"))
		{

		}
		else if (commandWord.startsWith("LM"))
		{

		}
		else if (commandWord.startsWith("LR"))
		{

		}
		else if (commandWord.startsWith("LS"))
		{

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

		}
		else if (commandWord.startsWith("SR"))
		{

		}
		else if (commandWord.startsWith("TF"))
		{

		}
		else if (commandWord.startsWith("TA"))
		{

		}
		else if (commandWord.startsWith("TO"))
		{

		}
		else if (commandWord.startsWith("TD"))
		{

		}
		else if (commandWord.startsWith("X") || commandWord.startsWith("Y") || commandWord.startsWith("I") || commandWord.startsWith("J")) // Coordinate
		{
			Pattern p = Pattern.compile("^(?:X([+-]?\\d+))?(?:Y([+-]?\\d+))?(?:I([+-]?\\d+))?(?:J([+-]?\\d+))?(D0\\d)$");
			Matcher m = p.matcher(commandWord);
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
			
//			int X = args[0];
//			int Y = args[1];
//			int I = args[2];
//			int J = args[3];
			
			String operation = m.group(5);
			switch (operation)
			{
				case "D01": // Interpolate
					// Ignore
					log("INTERP.");
					break;
				case "D02": // Move
					if (args[0] == null && args[1] == null)
						throw new RuntimeException("Invalid parameters for draw operation: " + commandWord);
					
					if (args[0] != null)
						currentPoint.x = args[0];
					if (args[1] != null)
						currentPoint.y = args[1];
					
					log("MOVE 2: " + currentPoint.x + ", " + currentPoint.y);
					break;
				case "D03": // Flash
					if (args[0] == null && args[1] == null)
						throw new RuntimeException("Invalid parameters for flash operation: " + commandWord);
					
					if (args[0] != null)
						currentPoint.x = args[0];
					if (args[1] != null)
						currentPoint.y = args[1];
					
					flash(currentPoint);
					log("FL.");
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

		}
		else if (commandWord.startsWith("G55"))
		{

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

		}
		else if (commandWord.startsWith("M01"))
		{

		}
		else if (commandWord.startsWith("IP"))
		{

		}
		else if (commandWord.startsWith("AS"))
		{

		}
		else if (commandWord.startsWith("IR"))
		{

		}
		else if (commandWord.startsWith("MI"))
		{

		}
		else if (commandWord.startsWith("OF"))
		{

		}
		else if (commandWord.startsWith("SF"))
		{

		}
		else if (commandWord.startsWith("IN"))
		{

		}
		else if (commandWord.startsWith("LN"))
		{

		}
		else if (commandWord.startsWith("G74"))
		{

		}
		else
			err("Skipped " + command);
	}

	public Main()
	{
		log("Running " + APP_NAME + " " + APP_VERSION);

		File testGerberFile = new File("./Reference files/STAR-XL CCT.GTL");
		if (!testGerberFile.exists())
			throw new RuntimeException("Failed to open gerber file. Are you sure it exists?");
		log("Found gerber file.");
		
		renderer = new Renderer();
		renderer.createWindow();

		Timer.tic();
		ArrayList<Command> commands = new ArrayList<>();
		try (InputStream in = new FileInputStream(testGerberFile))
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
		log("Parsed gerber file in " + String.format("%.2fs.", Timer.toc() * 0.001) + "\n");

		Timer.tic();
		for (Command command : commands)
		{
			processCommand(command);
		}
		log("Processed gerber file in " + String.format("%.2fs.", Timer.toc() * 0.001) + "\n");
		
//		for (Aperture a : apertureDictionary)
		
//		renderer.primitives
		
		renderer.redraw();

		log("Complete!");
	}
}
