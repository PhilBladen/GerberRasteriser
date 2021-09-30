package main;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import main.GeometricPrimitives.GeometricPrimitive;

import static main.Utils.*;

public class Renderer
{
	private JFrame frame;

	double scale = 1.0;

	boolean dragging = false;
	PositionD dragStart = new PositionD(0, 0);
	PositionD currentOffset = new PositionD(0, 0);
	PositionD dragOffset = new PositionD(0, 0);
	PositionD renderOffset = new PositionD(0, 0);
	
	public class PositionD
	{
		double x, y;
		
		public PositionD(double x, double y)
		{
			this.x = x;
			this.y = y;
		}
		
		public PositionD add(PositionD p)
		{
			return new PositionD(x + p.x, y + p.y);
		}
		
		public PositionD subtract(PositionD p)
		{
			return new PositionD(x - p.x, y - p.y);
		}
		
		public PositionD multiply(double s)
		{
			return new PositionD(x * s, y * s);
		}
		
		public void set(PositionD p)
		{
			this.x = p.x;
			this.y = p.y;
		}
		
		public void set(double x, double y)
		{
			this.x = x;
			this.y = y;
		}
		
		@Override
		public String toString()
		{
			return String.format("[%f, %f]", x, y);
		}
	}
	
	private void updateRenderOffset()
	{
		renderOffset.set(currentOffset.add(dragOffset));
		
		frame.repaint();
	}

	public void createWindow()
	{
		try
		{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex)
		{
		}
		
		GerberCanvas c = new GerberCanvas();

		frame = new JFrame("Gerber Rasteriser");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setLayout(new BorderLayout());
		frame.add(c);
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);

		c.addMouseWheelListener(new MouseWheelListener()
		{
			@Override
			public void mouseWheelMoved(MouseWheelEvent e)
			{
				log("");
				
				PositionD mousePos = new PositionD(e.getX(), e.getY());
				log("Mouse raw: " + mousePos);
				
				PositionD canvasMousePos = mousePos.subtract(currentOffset);
				log("Mouse on canvas: " + canvasMousePos);
				
				log("Initial offset: " + currentOffset);
								
				double scaleMultiplier;
				final double scalePerClick = 2;
				if (e.getPreciseWheelRotation() < 0)
					scaleMultiplier = scalePerClick;
				else
					scaleMultiplier = 1 / scalePerClick;
				
				scale *= scaleMultiplier;
				
				PositionD newCanvasMousePos = canvasMousePos.multiply(scaleMultiplier);
				log("New mouse on canvas: " + newCanvasMousePos);
				
				currentOffset.set(mousePos.subtract(newCanvasMousePos));				
				log("New offset: " + currentOffset);
				
				updateRenderOffset();
			}
		});

		c.addMouseListener(new MouseListener()
		{
			@Override
			public void mouseReleased(MouseEvent e)
			{
				dragging = false;
				
				currentOffset.set(currentOffset.add(dragOffset));
				dragOffset.set(0, 0);
				
				log("Stop drag");
				
				updateRenderOffset();

			}

			@Override
			public void mousePressed(MouseEvent e)
			{
				dragging = true;
				dragStart.x = e.getX();
				dragStart.y = e.getY();

				log("Dragging");
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				// TODO Auto-generated method stub

			}

			@Override
			public void mouseEntered(MouseEvent e)
			{
				// TODO Auto-generated method stub

			}

			@Override
			public void mouseClicked(MouseEvent e)
			{
				// TODO Auto-generated method stub

			}
		});

		c.addMouseMotionListener(new MouseMotionListener()
		{

			@Override
			public void mouseMoved(MouseEvent e)
			{

			}

			@Override
			public void mouseDragged(MouseEvent e)
			{
				if (dragging)
				{
					PositionD mousePos = new PositionD(e.getX(), e.getY());
					dragOffset.set(mousePos.subtract(dragStart));

					log("Offset: " + renderOffset);

					updateRenderOffset();
				}
			}
		});
	}

	public ArrayList<GeometricPrimitive> primitives = new ArrayList<>();

	public void redraw()
	{
		frame.repaint();
	}

	public class GerberCanvas extends JPanel
	{
		private static final long serialVersionUID = 1L;

		public GerberCanvas()
		{
			super(true);
		}

		@Override
		public Dimension getPreferredSize()
		{
			return new Dimension(1920, 1080);
		}

		@Override
		protected void paintComponent(Graphics g)
		{
			super.paintComponent(g);

			Graphics2D graphics2D = (Graphics2D) g;
			
			AffineTransform a = graphics2D.getTransform();

			 graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); // Set anti-alias!
			// graphics2D.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON); // Set anti-alias for text
			 
			// Draw background:
			g.setColor(Color.BLACK);
			g.fillRect(0, 0, getWidth(), getHeight());

			graphics2D.translate(renderOffset.x, renderOffset.y);
			graphics2D.scale(scale, -scale);
			graphics2D.translate(0, -getHeight());

			for (GeometricPrimitive primitive : primitives)
			{
				primitive.render(graphics2D);
			}
			
//			graphics2D.setTransform(a);
//			g.setColor(Color.red);
//			graphics2D.drawOval(100, 100, 1, 1);
		}
	}
}
