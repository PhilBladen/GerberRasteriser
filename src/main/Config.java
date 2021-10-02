package main;

public class Config
{
	public static final double rasterDPI = 2540;
	public static final double nanosToPixels = (rasterDPI / 25.4) * 1E-6;
	public static final boolean use16BitColor = false; // Reduced memory footprint but can significantly slow rendering on certain machines
	public static final double maxZoom = 10;
	public static final double minZoom = 0.05;
	public static final boolean drawBoundingBoxes = false;
	public static final boolean drawOuterBoundingBox = false;
	public static final int exportBorderSize = 50;
}
