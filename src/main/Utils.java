package main;

import main.Config.UnitType;

public class Utils
{
	public static double convertUnits(double in)
	{
		Config config = Config.getConfig();
		if (config.units == UnitType.NONE)
			throw new RuntimeException("Configuration not complete.");
		
		if (config.units == UnitType.IN)
			in *= 25.4;
		
		return in * 1E6;
	}
	
	public static double importCoordinate(double in)
	{
		Config config = Config.getConfig();
		if (config.units == UnitType.NONE)
			throw new RuntimeException("Configuration not complete.");
		
		if (config.units == UnitType.IN)
		{
			in *= 25.4;
		}
		
		return in * config.multiplier;
	}
}
