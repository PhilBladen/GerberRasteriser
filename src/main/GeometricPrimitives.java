package main;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;

public class GeometricPrimitives
{
	public static abstract class GeometricPrimitive
	{
		public Coordinate offset = new Coordinate(0, 0);
		
		public abstract void render(Graphics2D g);
		public GeometricPrimitive clone()
		{
			return null;
		}
	}
	
	public static class Circle extends GeometricPrimitive
	{
		public boolean exposure;
		public int diameter;
		public Coordinate center;
		public double rotation; // TODO is this necessary?
		
		public Circle(boolean exposure, int diameter, Coordinate center, double rotation)
		{
			this.exposure = exposure;
			this.diameter = diameter;
			this.center = center;
			this.rotation = rotation;
		}
		
		@Override
		public GeometricPrimitive clone()
		{
			return new Circle(exposure, diameter, center, rotation);
		}
		
		@Override
		public void render(Graphics2D g)
		{
			AffineTransform a = g.getTransform();
			g.translate(Utils.toPixels(offset.x), Utils.toPixels(offset.y));
			
			g.setColor(exposure ? Color.WHITE : Color.BLACK);
			g.fillOval(Utils.toPixels(center.x - diameter * 0.5), Utils.toPixels(center.y - diameter * 0.5), Utils.toPixels(diameter), Utils.toPixels(diameter));
			
//			System.out.println(String.format("X: %d, Y: %d", center.x + offset.x, center.y + offset.y));
			
			g.setTransform(a);
		}
	}
	
	public static class Line extends GeometricPrimitive
	{
		public boolean exposure;
		public int width;
		public Coordinate start;
		public Coordinate end;
		public double rotation;
		
		@Override
		public void render(Graphics2D g)
		{
		}
	}
	
	public static class Rectangle extends GeometricPrimitive
	{
		public boolean exposure;
		public int width, height;
		public Coordinate center;
		public double rotation;
		
		@Override
		public void render(Graphics2D g)
		{
		}
	}
	
	public static class Outline extends GeometricPrimitive
	{
		public boolean exposure;
		public int numVertices;
		public Coordinate points[];
		public double rotation;
		
		@Override
		public void render(Graphics2D g)
		{
		}
	}
	
	public static class Polygon extends GeometricPrimitive
	{
		public boolean exposure;
		public int numVertices, diameter;
		public Coordinate center;
		public double rotation;
		
		@Override
		public void render(Graphics2D g)
		{
		}
	}
	
	public static class Thermal extends GeometricPrimitive
	{
		public Coordinate center;
		public int outerDiameter, innerDiameter, gap;
		public double rotation;
		
		@Override
		public void render(Graphics2D g)
		{
		}
	}
	
	public static class Coordinate
	{
		public int x, y;
		
		public Coordinate(int x, int y)
		{
			this.x = x;
			this.y = y;
		}
	}
	
	public static final Coordinate origin = new Coordinate(0, 0);
}
