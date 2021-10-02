package main;

import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.io.PrintStream;
import java.util.regex.Matcher;

public class Utils
{
	// NOTE: The whole system has a base unit of nanometers
	
	public static BufferedImage blur(BufferedImage image, int radius)
	{
		float[] matrix = new float[radius * radius];
		for (int i = 0; i < radius * radius; i++)
			matrix[i] = 1.0f/(radius * radius);
		
		Kernel kernel = new Kernel(radius, radius, matrix);
		BufferedImageOp op = new ConvolveOp(kernel);
		image = op.filter(image, null);
		return image;
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
