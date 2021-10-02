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

	final public double rasterDPI = 2540;
	final public double nanosToPixels = (rasterDPI / 25.4) * 1E-6;
	final public boolean use16BitColor = true; // Improves memory usage but can significantly slow renderering in certain machines
}
