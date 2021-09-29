package main;

public class Config
{
	private static Config config = null;
	
	private Config()
	{
	}
		
	public static Config getConfig()
	{
		if (config == null)
			config = new Config();
		return config;
	}
	
	enum UnitType
	{
		NONE, MM, IN;
	}

	UnitType units = UnitType.NONE;
	int multiplier;
}
