package main;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

import main.Layer.Modifiers;

public interface Renderable
{
	public void render(Graphics2D g);
	public void setModifiers(Modifiers m);
	public Rectangle2D getBounds();
}