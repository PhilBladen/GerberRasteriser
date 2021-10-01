package main;

import java.io.PrintStream;
import java.util.regex.Matcher;

import main.Config.UnitType;

public class Utils
{
	// NOTE: The whole system has a base unit of nms
	
	public static double convertUnits(double in)
	{
		Config config = Config.getConfig();
		if (config.units == UnitType.NONE)
			throw new RuntimeException("Configuration not complete.");
		
		if (config.units == UnitType.IN)
			in *= 25.4;
		
		return in * 1E6;
	}
	
	public static int importCoordinate(double in)
	{
		Config config = Config.getConfig();
		if (config.units == UnitType.NONE)
			throw new RuntimeException("Configuration not complete.");
		
		if (config.units == UnitType.IN)
		{
			in *= 25.4;
		}
		
		return (int) (in * config.multiplier);
	}
	
	public static double toPixels(double d)
	{
		return Config.getConfig().nanosToPixels * d;
	}
	
	public static double toRads(double deg)
	{
		return 2 * Math.PI * deg / 360.0;
	}
	
	public static double toDeg(double rad)
	{
		return 360.0 * rad / (2 * Math.PI);
	}
	
	public static int countMatchingGroups(Matcher m)
	{
		int numGroups = m.groupCount();
		for (int i = 0; i < m.groupCount(); i++)
		{
			String group = m.group(i);
			if (group == null)
			{
				numGroups = i;
				break;
			}
		}
		return numGroups;
	}
	
	public static void log(Object o)
	{
		log(LogLevel.INFO, o);
	}
	
	public static void warn(Object o)
	{
		log(LogLevel.WARNING, o);
	}

	public static void err(Object o)
	{
		log(LogLevel.ERROR, o);
	}
	
	public enum LogLevel
	{
		INFO("INFO"), WARNING("WARN"), ERROR("ERR");
		
		String name;
		LogLevel(String name)
		{
			this.name = name;
		}
		
		@Override
		public String toString()
		{
			return name;
		}
	}
	
	public static void log(LogLevel level, Object o)
	{
		PrintStream s;
		if (level == LogLevel.INFO)
			s = System.out;
		else
			s = System.err;
		
		s.println("[" + level.toString() + "] " + o.toString());
	}
	
	public static class Timer
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
}
