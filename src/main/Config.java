package main;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

public class Config
{
	private static Yaml yaml;
	private static File configFile;
	private static Map<String, Object> config;

	/** Config options **/
	public static final int rasterDPI;
	public static final boolean use16BitColor; // Reduced memory footprint but can significantly slow rendering on certain machines
	public static final double maxZoom;
	public static final double minZoom;
	public static final boolean drawBoundingBoxes;
	public static final boolean drawOuterBoundingBox;
	public static final int exportBorderSize;
	public static final boolean renderRegionAsOutline;
	public static String defaultOpenPath;

	/** Derived config **/
	public static final double nanosToPixels;

	static
	{
		configFile = new File("config.yml");

		DumperOptions options = new DumperOptions();
		options.setIndent(2);
		options.setPrettyFlow(true);
		options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
		options.setWidth(9999);
		yaml = new Yaml(options);

		if (!configFile.exists())
		{
			config = null;
		}
		else
		{
			try
			{
				config = yaml.load(new FileInputStream(configFile));
			}
			catch (Exception e)
			{
				config = null;
			}
		}

		if (config == null)
			config = new HashMap<>();

		rasterDPI = intConfigOption("rasterDPI", 2540);
		use16BitColor = booleanConfigOption("use16BitColor", false);
		maxZoom = doubleConfigOption("maxZoom", 10.0);
		minZoom = doubleConfigOption("minZoom", 0.05);
		drawBoundingBoxes = booleanConfigOption("drawBoundingBoxes", false);
		drawOuterBoundingBox = booleanConfigOption("drawOuterBoundingBox", false);
		exportBorderSize = intConfigOption("exportBorderSize", 50);
		renderRegionAsOutline = booleanConfigOption("renderRegionAsOutline", false);
		defaultOpenPath = stringConfigOption("defaultOpenPath", "");

		nanosToPixels = ((double) rasterDPI / 25.4) * 1E-6;

		save();
	}

	private static void save()
	{
		try
		{
			yaml.dump(config, new FileWriter(configFile));
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	private static double doubleConfigOption(String name, double defaultValue)
	{
		try
		{
			if (config.containsKey(name))
				return (Double) config.get(name);
		}
		catch (Throwable t)
		{
		}
		config.put(name, defaultValue);
		return defaultValue;
	}

	private static int intConfigOption(String name, int defaultValue)
	{
		try
		{
			if (config.containsKey(name))
				return (Integer) config.get(name);
		}
		catch (Throwable t)
		{
		}
		config.put(name, defaultValue);
		return defaultValue;
	}

	private static boolean booleanConfigOption(String name, boolean defaultValue)
	{
		try
		{
			if (config.containsKey(name))
				return (Boolean) config.get(name);
		}
		catch (Throwable t)
		{
		}
		config.put(name, defaultValue);
		return defaultValue;
	}

	private static String stringConfigOption(String name, String defaultValue)
	{
		try
		{
			if (config.containsKey(name))
				return (String) config.get(name);
		}
		catch (Throwable t)
		{
		}
		config.put(name, defaultValue);
		return defaultValue;
	}

	public static String getDefaultPath()
	{
		return defaultOpenPath;
	}

	public static void setDefaultPath(String path)
	{
		defaultOpenPath = path;
		config.put("defaultOpenPath", path);
		save();

	}

	public static void load()
	{
		// Function to force static initialisation - do not delete
	}
}
