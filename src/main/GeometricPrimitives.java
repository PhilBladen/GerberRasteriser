package main;

import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

import main.math.Vector2i;

public class GeometricPrimitives
{	
	public static class Circle extends Area
	{
		public Circle(int diameter, Vector2i center, double rotation)
		{
			add(new Area(new Ellipse2D.Double(Utils.toPixels(center.x - diameter * 0.5), Utils.toPixels(center.y - diameter * 0.5), Utils.toPixels(diameter), Utils.toPixels(diameter))));
		}
	}
	
	public static class Rectangle extends Area
	{
		public Rectangle(int width, int height, Vector2i center, double rotation)
		{
			add(new Area(new Rectangle2D.Double(Utils.toPixels(center.x - width * 0.5), Utils.toPixels(center.y - height * 0.5), Utils.toPixels(width), Utils.toPixels(height))));
			
			transform(AffineTransform.getRotateInstance(rotation));
		}
	}
	
	public static class VectorLine extends Area
	{
		public VectorLine(int width, Vector2i start, Vector2i end, double rotation)
		{
			double length = Math.hypot(start.x - end.x, start.y - end.y);
			double angle = Math.atan2(start.x - end.x, start.y - end.y);
			
			// TODO FIXME
			
			add(new Area(new Line2D.Double(Utils.toPixels(start.x), Utils.toPixels(start.y), Utils.toPixels(end.x), Utils.toPixels(end.y))));
			
			transform(AffineTransform.getRotateInstance(rotation));
		}
	}
	
	public static class Outline extends Area
	{
		public Outline(int numVertices, ArrayList<Vector2i> points, double rotation)
		{
			Path2D p = new Path2D.Double();
			for (int i = 0; i < numVertices; i++)
			{
				Vector2i point = points.get(i);
				double x = Utils.toPixels(point.x);
				double y = Utils.toPixels(point.y);
				if (i == 0)
					p.moveTo(x, y);
				else
					p.lineTo(x, y);
			}
			add(new Area(p));
			
			transform(AffineTransform.getRotateInstance(rotation));
		}
	}
	
	public static class Polygon extends Area
	{
		public Polygon(int numVertices, int diameter, Vector2i center, double rotation)
		{
			Path2D p = new Path2D.Double();
			double radius = diameter * 0.5;
			for (int i = 0; i < numVertices; i++)
			{
				double angle = i * (2 * Math.PI / numVertices);
				if (i == 0)
					p.moveTo(Utils.toPixels(center.x + Math.cos(angle) * radius), Utils.toPixels(center.y + Math.sin(angle) * radius));
				else
					p.lineTo(Utils.toPixels(center.x + Math.cos(angle) * radius), Utils.toPixels(center.y + Math.sin(angle) * radius));
			}
			add(new Area(p));
			
			transform(AffineTransform.getRotateInstance(rotation));
		}
	}
	
	public static class Thermal extends Area
	{
		public Thermal(Vector2i center, int outerDiameter, int innerDiameter, int gap, double rotation) // FIXME Test this
		{
			Area outerHole = new Area(new Ellipse2D.Double(Utils.toPixels(center.x - outerDiameter * 0.5), Utils.toPixels(center.y - outerDiameter * 0.5), Utils.toPixels(outerDiameter), Utils.toPixels(outerDiameter)));
			Area innerHole = new Area(new Ellipse2D.Double(Utils.toPixels(center.x - outerDiameter * 0.5), Utils.toPixels(center.y - outerDiameter * 0.5), Utils.toPixels(outerDiameter), Utils.toPixels(outerDiameter)));
			Area rectX = new Area(new Rectangle2D.Double(Utils.toPixels(center.x), Utils.toPixels(center.y), Utils.toPixels(outerDiameter), Utils.toPixels(gap)));
			Area rectY = new Area(new Rectangle2D.Double(Utils.toPixels(center.x), Utils.toPixels(center.y), Utils.toPixels(gap), Utils.toPixels(outerDiameter)));
			
			add(outerHole);
			subtract(innerHole);
			subtract(rectX);
			subtract(rectY);
			
			transform(AffineTransform.getRotateInstance(rotation));
		}
	}
	
	public static final Vector2i origin = new Vector2i(0, 0);
}
