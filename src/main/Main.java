package main;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import main.Config.UnitType;

public class Main
{
	private final String APP_NAME = "Gerber Rasteriser";
	private final String APP_VERSION = "v0.1";

	private HashMap<String, Aperture> apertureTemplateDictionary = new HashMap<>();

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

	private enum State
	{
		NONE, IN_EXT_CMD, IN_WORD_CMD
	}

	public static void main(String[] args)
	{
		new Main();
	}

	private void log(String s)
	{
		System.out.println(s);
	}

	private void err(String s)
	{
		System.err.println(s);
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
			Pattern p = Pattern.compile("^ADD(\\d\\d+)([A-Z]+)[,]?([+-]?(?:[0-9]*[.])?[0-9]+)?(?:X([+-]?(?:[0-9]*[.])?[0-9]+))?(?:X([+-]?(?:[0-9]*[.])?[0-9]+))?(?:X([+-]?(?:[0-9]*[.])?[0-9]+))?$");
			Matcher m = p.matcher(commandWord);
			if (!m.matches())
				throw new RuntimeException("Illegal aperture definition: " + commandWord);
			
			String id = "D" + m.group(1);
			log("Aperture " + id + " defined.");
			int numArgs = m.groupCount() - 3;
					
//			m.matches();
			for (int i = 0; i < m.groupCount(); i++)
			{
				log(m.group(i));
			}
			
			double args[] = new double[numArgs];
			int argsconv[] = new int[numArgs];
			for (int i = 0; i < numArgs; i++)
			{
				args[i] = Double.parseDouble(m.group(i + 3));
				argsconv[i] = (int) Utils.convertUnits(args[i]);
			}
			
			String type = m.group(2);
			if (type.equals("C"))
			{
				if (numArgs == 1)
					new Aperture.Circle(argsconv[0]);
				else if (numArgs == 2)
					new Aperture.Circle(argsconv[0], argsconv[1]);
				else
					throw new RuntimeException("Unexpected number of arguments for circle aperture.");
			}
			else if (type.equals("R"))
			{
				if (numArgs == 2)
					new Aperture.Rectangle(argsconv[0], argsconv[1]);
				else if (numArgs == 3)
					new Aperture.Rectangle(argsconv[0], argsconv[1], argsconv[2]);
				else
					throw new RuntimeException("Unexpected number of arguments for rectangle aperture.");
			}
			else if (type.equals("O"))
			{
//				new Aperture.ObRound();
			}
			else if (type.equals("P"))
			{
//				new Aperture.Polygon();
			}
			else
			{
				
			}
		}
		else if (commandWord.startsWith("AM"))
		{

		}
		else if (commandWord.startsWith("D"))
		{

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

		}
		else if (commandWord.startsWith("G37"))
		{

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
		else if (commandWord.startsWith("M02"))
		{
			log("Reached end of file :)");
		}
		else if (commandWord.startsWith("X"))
		{

		}
		else if (commandWord.startsWith("Y"))
		{

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

		Timer.tic();
		ArrayList<Command> commands = new ArrayList<>();
		try (InputStream in = new FileInputStream(testGerberFile))
		{
			int c;
			State parserState = State.NONE;
			String word = "";
			Command currentCommand = null;
			while ((c = in.read()) != -1)
			{
				if (Character.isWhitespace(c))
					continue; // Ignore all whitespace

				if (c == '%')
				{
					if (parserState == State.IN_EXT_CMD)
					{
						parserState = State.NONE;

						if (currentCommand == null || currentCommand.words.size() == 0)
							throw new RuntimeException("Unexpected item in baggage area.");

						commands.add(currentCommand);
						currentCommand = null;
					}
					else if (parserState != State.NONE)
						throw new RuntimeException("Invalid state change. Probably a missing delimiter somewhere...");
					else
						parserState = State.IN_EXT_CMD;
				}
				else if (c == '*')
				{
					if (parserState != State.IN_EXT_CMD)
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
		log("Parsed gerber file in " + String.format("%.2fs.", Timer.toc() * 0.001));

		for (Command command : commands)
		{
			processCommand(command);
		}

		log("Complete!");
	}
}
