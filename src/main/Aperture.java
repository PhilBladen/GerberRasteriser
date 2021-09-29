package main;

import java.util.ArrayList;

import main.GeometricPrimitives.Coordinate;
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
		public Rectangle(int x, int y)
		{
			this(x, y, 0);
		}
		
		public Rectangle(int x, int y, int holeDiameter)
		{
			geometricPrimitives.add(new GeometricPrimitives.Rectangle(true, x, y, GeometricPrimitives.origin, 0));
			geometricPrimitives.add(new GeometricPrimitives.Circle(false, holeDiameter, GeometricPrimitives.origin, 0));
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
				geometricPrimitives.add(new GeometricPrimitives.Circle(true, x, GeometricPrimitives.origin, 0));
			}
			else if (x > y)
			{
				int d = x - y;
				
				geometricPrimitives.add(new GeometricPrimitives.Circle(true, y, new Coordinate(-d / 2, 0), 0));
				geometricPrimitives.add(new GeometricPrimitives.Circle(true, y, new Coordinate(d / 2, 0), 0));
				geometricPrimitives.add(new GeometricPrimitives.Rectangle(true, d, y, GeometricPrimitives.origin, 0));
			}
			else
			{
				int d = y - x;
				
				geometricPrimitives.add(new GeometricPrimitives.Circle(true, x, new Coordinate(0, -d / 2), 0));
				geometricPrimitives.add(new GeometricPrimitives.Circle(true, x, new Coordinate(0, d / 2), 0));
				geometricPrimitives.add(new GeometricPrimitives.Rectangle(true, x, d, GeometricPrimitives.origin, 0));
			}
			geometricPrimitives.add(new GeometricPrimitives.Circle(false, holeDiameter, GeometricPrimitives.origin, 0));
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
