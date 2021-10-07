package main.graphicalobjects;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;

import main.Config;
import main.Layer.Modifiers;
import main.Layer.Modifiers.Polarity;

public class Region implements Renderable
{
	private Modifiers modifiers;
	private Path2D p;

	public Region(Path2D p, Modifiers modifiers)
	{
		this.p = p;
		this.modifiers = modifiers;
	}

	@Override
	public void render(Graphics2D g)
	{
		Composite c = g.getComposite();

		if (Config.renderRegionAsOutline)
		{
			g.setColor(Color.WHITE);
			if (modifiers.polarity == Polarity.DARK)
				g.setStroke(new BasicStroke((float) 1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			else			
				g.setStroke(new BasicStroke((float) 1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[]{9}, 0));
			g.draw(p);
		}
		else
		{
			if (modifiers.polarity == Polarity.DARK)
				g.setColor(Color.WHITE);
			else
				g.setComposite(AlphaComposite.Clear);
			
			g.fill(p);
		}
		
		g.setComposite(c); // Restore
		
		if (Config.drawBoundingBoxes)
		{
			g.setColor(Color.ORANGE);
			g.setStroke(new BasicStroke(1));
			g.draw(p.getBounds());
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
		return p.getBounds2D();
	}

	@Override
	public Modifiers getModifiers()
	{
		return modifiers;
	}
}