package main;

import java.util.ArrayList;

import main.GeometricPrimitives.GeometricPrimitive;

public class Aperture
{
	protected ArrayList<GeometricPrimitive> geometricPrimitives = new ArrayList<>();
	
	public static class Circle extends Aperture
	{
		public Circle(int diameter)
		{
			this(diameter, 0);
		}
		
		public Circle(int diameter, int holeDiameter)
		{
			geometricPrimitives.add(new GeometricPrimitives.Circle(true, diameter, GeometricPrimitives.origin, 0));
			geometricPrimitives.add(new GeometricPrimitives.Circle(false, holeDiameter, GeometricPrimitives.origin, 0));
		}
	}
	
	public static class Rectangle extends Aperture
	{
		public int x, y, holeDiameter;
		
		public Rectangle(int x, int y)
		{
			this(x, y, 0);
		}
		
		public Rectangle(int x, int y, int holeDiameter)
		{
			this.x = x;
			this.y = y;
			this.holeDiameter = holeDiameter;
		}
	}
	
	public static class ObRound extends Aperture
	{
		public int x, y, holeDiameter;
		
		public ObRound(int x, int y)
		{
			this(x, y, 0);
		}
		
		public ObRound(int x, int y, int holeDiameter)
		{
			this.x = x;
			this.y = y;
			this.holeDiameter = holeDiameter;
		}
	}
	
	public static class Polygon extends Aperture
	{
		public int outerDiameter, numVertices, holeDiameter;
		public double rotation;
		
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
			this.outerDiameter = outerDiameter;
			this.numVertices = numVertices;
			this.rotation = rotation;
			this.holeDiameter = holeDiameter;
		}
	}
	
	public static class Custom extends Aperture
	{
		// FIXME
	}
}
