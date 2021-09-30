package main;

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
		System.out.println(o.toString());
	}

	public static void err(Object o)
	{
		System.err.println(o.toString());
	}
}
