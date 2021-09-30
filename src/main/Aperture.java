package main;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;

import main.GeometricPrimitives.Coordinate;

public class Aperture
{
	private AffineTransform transform = null;
	protected Area area = new Area();
	
	public Coordinate offset = new Coordinate(0, 0);
	
	public Aperture clone()
	{
		Aperture a = new Aperture();
		a.area = (Area) area.clone();
		return a;
	}
	
	private void pushTransform(Graphics2D g)
	{
		transform = g.getTransform();
	}
	
	private void popTransform(Graphics2D g)
	{
		if (transform == null)
			throw new RuntimeException("Pop requested but transform not pushed.");
		g.setTransform(transform);
	}
	
	public void render(Graphics2D g)
	{
		pushTransform(g);
		g.translate(Utils.toPixels(offset.x), Utils.toPixels(offset.y));
		
		g.setColor(Color.WHITE);
		g.fill(area);
		
		popTransform(g);
	}
	
	public static class Circle extends Aperture
	{
		public Circle(int diameter)
		{
			this(diameter, 0);
		}
		
		public Circle(int diameter, int holeDiameter)
		{
			area.add(new GeometricPrimitives.Circle(diameter, GeometricPrimitives.origin, 0));
			area.subtract(new GeometricPrimitives.Circle(holeDiameter, GeometricPrimitives.origin, 0));
		}
	}
	
	public static class Rectangle extends Aperture
	{		
		public Rectangle(int x, int y)
		{
			this(x, y, 0);
		}
		
		public Rectangle(int x, int y, int holeDiameter)
		{
			area.add(new GeometricPrimitives.Rectangle(x, y, GeometricPrimitives.origin, 0));
			area.subtract(new GeometricPrimitives.Circle(holeDiameter, GeometricPrimitives.origin, 0));
		}
	}
	
	public static class ObRound extends Aperture
	{		
		public ObRound(int x, int y)
		{
			this(x, y, 0);
		}
		
		public ObRound(int x, int y, int holeDiameter)
		{
			if (x == y)
			{
				area.add(new GeometricPrimitives.Circle(x, GeometricPrimitives.origin, 0));
			}
			else if (x > y)
			{
				int d = x - y;
				
				area.add(new GeometricPrimitives.Circle(y, new Coordinate(-d / 2, 0), 0));
				area.add(new GeometricPrimitives.Circle(y, new Coordinate(d / 2, 0), 0));
				area.add(new GeometricPrimitives.Rectangle(d, y, GeometricPrimitives.origin, 0));
			}
			else
			{
				int d = y - x;
				
				area.add(new GeometricPrimitives.Circle(x, new Coordinate(0, -d / 2), 0));
				area.add(new GeometricPrimitives.Circle(x, new Coordinate(0, d / 2), 0));
				area.add(new GeometricPrimitives.Rectangle(x, d, GeometricPrimitives.origin, 0));
			}
			area.subtract(new GeometricPrimitives.Circle(holeDiameter, GeometricPrimitives.origin, 0));
		}
	}
	
	public static class Polygon extends Aperture
	{		
		public Polygon(int outerDiameter, int numVertices)
		{
			this(outerDiameter, numVertices, 0);
		}
		
		public Polygon(int outerDiameter, int numVertices, double rotation)
		{
			this(outerDiameter, numVertices, rotation, 0);
		}
		
		public Polygon(int outerDiameter, int numVertices, double rotation, int holeDiameter)
		{
			area.add(new GeometricPrimitives.Polygon(numVertices, outerDiameter, GeometricPrimitives.origin, rotation));
			area.subtract(new GeometricPrimitives.Circle(holeDiameter, GeometricPrimitives.origin, 0));
		}
	}
	
	public static class Custom extends Aperture
	{
		// FIXME
	}
}
