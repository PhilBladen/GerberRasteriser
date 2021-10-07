package main.graphicalobjects;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;

import main.Config;
import main.Utils;
import main.Layer.Modifiers;
import main.Layer.Modifiers.Polarity;

public class Interpolation implements Renderable
{
	private Modifiers modifiers;
	private Shape s;
	private int thickness;

	public Interpolation(Modifiers m, int thickness, Shape s)
	{
		this.modifiers = m;
		this.thickness = thickness;
		this.s = s;
	}

	@Override
	public void render(Graphics2D g)
	{
		Composite c = g.getComposite();
		
		if (modifiers.polarity == Polarity.DARK)
			g.setColor(Color.WHITE);
		else
			g.setComposite(AlphaComposite.Clear);

		g.setStroke(new BasicStroke((float) Math.ceil(Utils.toPixels(thickness)), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.draw(s);
		
		g.setComposite(c); // Restore
		
		if (Config.drawBoundingBoxes)
		{
			g.setColor(Color.ORANGE);
			g.setStroke(new BasicStroke(1));
			g.draw(s.getBounds());
		}
	}

	@Override
	public void setModifiers(Modifiers m)
	{
		modifiers = m;
	}

	@Override
	public Rectangle2D getBounds()
	{
		Rectangle2D bounds = s.getBounds2D();
		double thickness_pixels = Utils.toPixels(thickness);
		return new Rectangle2D.Double(bounds.getX() - thickness_pixels * 0.5, bounds.getY() - thickness_pixels * 0.5, bounds.getWidth() + thickness_pixels, bounds.getHeight() + thickness_pixels);
	}
	
	public int getThickness()
	{
		return thickness;
	}
	
	public Shape getShape()
	{
		return s;
	}

	@Override
	public Modifiers getModifiers()
	{
		return modifiers;
	}
}