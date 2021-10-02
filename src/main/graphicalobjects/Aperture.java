package main.graphicalobjects;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;

import main.Config;
import main.GeometricPrimitives;
import main.Layer.Modifiers;
import main.Utils;
import main.math.Vector2i;

public class Aperture implements Renderable
{
	private Modifiers modifiers; // TODO unused
	private AffineTransform transform = null;
	protected Area area = new Area();
	
	public Vector2i offset = new Vector2i(0, 0);
	
	public Aperture(Modifiers m)
	{
		this.modifiers = m;
	}
	
	public Aperture clone()
	{
		Aperture a = new Aperture(modifiers.clone());
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
	
	@Override
	public void render(Graphics2D g)
	{
		pushTransform(g);
		g.translate(Utils.toPixels(offset.x), Utils.toPixels(offset.y));
		
		g.setColor(Color.WHITE);
		g.fill(area);
		
		if (Config.drawBoundingBoxes)
		{
			g.setColor(Color.ORANGE);
			g.setStroke(new BasicStroke(1));
			g.draw(area.getBounds());
		}
		
		popTransform(g);
	}

	@Override
	public void setModifiers(Modifiers m)
	{
		modifiers = m;
	}

	@Override
	public Modifiers getModifiers()
	{
		return modifiers;
	}
	
	@Override
	public Rectangle2D getBounds()
	{
		Rectangle2D r = area.getBounds2D();
		return new Rectangle2D.Double(r.getMinX() + Utils.toPixels(offset.x), r.getMinY() + Utils.toPixels(offset.y), r.getWidth(), r.getHeight());
	}
	
	public static class Circle extends Aperture
	{
		public int diameter;
		
		public Circle(Modifiers m, int diameter)
		{
			this(m, diameter, 0);
		}
		
		public Circle(Modifiers m, int diameter, int holeDiameter)
		{
			super(m);
			
			this.diameter = diameter;
			
			area.add(new GeometricPrimitives.Circle(diameter, GeometricPrimitives.origin, 0));
			area.subtract(new GeometricPrimitives.Circle(holeDiameter, GeometricPrimitives.origin, 0));
		}
	}
	
	public static class Rectangle extends Aperture
	{		
		public Rectangle(Modifiers m, int x, int y)
		{
			this(m, x, y, 0);
		}
		
		public Rectangle(Modifiers m, int x, int y, int holeDiameter)
		{
			super(m);
			
			area.add(new GeometricPrimitives.Rectangle(x, y, GeometricPrimitives.origin, 0));
			area.subtract(new GeometricPrimitives.Circle(holeDiameter, GeometricPrimitives.origin, 0));
		}
	}
	
	public static class ObRound extends Aperture
	{		
		public ObRound(Modifiers m, int x, int y)
		{
			this(m, x, y, 0);
		}
		
		public ObRound(Modifiers m, int x, int y, int holeDiameter)
		{
			super(m);
			
			if (x == y)
			{
				area.add(new GeometricPrimitives.Circle(x, GeometricPrimitives.origin, 0));
			}
			else if (x > y)
			{
				int d = x - y;
				
				area.add(new GeometricPrimitives.Circle(y, new Vector2i(-d / 2, 0), 0));
				area.add(new GeometricPrimitives.Circle(y, new Vector2i(d / 2, 0), 0));
				area.add(new GeometricPrimitives.Rectangle(d, y, GeometricPrimitives.origin, 0));
			}
			else
			{
				int d = y - x;
				
				area.add(new GeometricPrimitives.Circle(x, new Vector2i(0, -d / 2), 0));
				area.add(new GeometricPrimitives.Circle(x, new Vector2i(0, d / 2), 0));
				area.add(new GeometricPrimitives.Rectangle(x, d, GeometricPrimitives.origin, 0));
			}
			area.subtract(new GeometricPrimitives.Circle(holeDiameter, GeometricPrimitives.origin, 0));
		}
	}
	
	public static class Polygon extends Aperture
	{		
		public Polygon(Modifiers m, int outerDiameter, int numVertices)
		{
			this(m, outerDiameter, numVertices, 0);
		}
		
		public Polygon(Modifiers m, int outerDiameter, int numVertices, double rotation)
		{
			this(m, outerDiameter, numVertices, rotation, 0);
		}
		
		public Polygon(Modifiers m, int outerDiameter, int numVertices, double rotation, int holeDiameter)
		{
			super(m);
			
			area.add(new GeometricPrimitives.Polygon(numVertices, outerDiameter, GeometricPrimitives.origin, rotation));
			area.subtract(new GeometricPrimitives.Circle(holeDiameter, GeometricPrimitives.origin, 0));
		}
	}
	
	public static class Custom extends Aperture
	{
		public Custom(Modifiers m)
		{
			super(m);
		}
		
		public void addPrimitive(Area primitive, boolean exposure)
		{
			if (exposure)
				area.add(primitive);
			else
				area.subtract(primitive);
		}
	}
}
