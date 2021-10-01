package main;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import main.Main.Modifiers;

public class Renderer
{
	private JFrame frame;
	private GerberCanvas c;

	boolean dragging = false;
	Vector2d dragStart = new Vector2d(0, 0);
	Vector2d currentOffset = new Vector2d(0, 0);
	Vector2d dragOffset = new Vector2d(0, 0);
	Vector2d renderOffset = new Vector2d(0, 0);
	Vector2d mousePosition = new Vector2d(0, 0);
	double scale = 1.0;

//	public ArrayList<Aperture> apertures = new ArrayList<>();
//	public HashMap<Integer, ArrayList<Shape>> traces = new HashMap<>();
//	public ArrayList<Path2D> pours = new ArrayList<>();
	
	public ArrayList<Renderable> objects = new ArrayList<>();
	
	double maxX = 0, maxY = 0;
	
	transient boolean loadedGerber = false;
	
	public interface Renderable
	{
		public void render(Graphics2D g);
		public void setModifiers(Modifiers m);
	}

	private void updateRenderOffset()
	{
		renderOffset.set(currentOffset.add(dragOffset));

		c.repaint();
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

		c = new GerberCanvas();

		frame = new JFrame("Gerber Rasteriser");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setLayout(new BorderLayout());
		frame.add(c);
		frame.pack();
		frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);

		c.addMouseWheelListener(new MouseWheelListener()
		{
			@Override
			public void mouseWheelMoved(MouseWheelEvent e)
			{
				mousePosition.x = e.getX();
				mousePosition.y = e.getY();

				Vector2d canvasMousePos = mousePosition.subtract(currentOffset);

				double scaleMultiplier;
				final double scalePerClick = 1.2;
				if (e.getPreciseWheelRotation() < 0.0)
					scaleMultiplier = scalePerClick;
				else
					scaleMultiplier = 1.0 / scalePerClick;

				scale *= scaleMultiplier;

				Vector2d newCanvasMousePos = canvasMousePos.multiply(scaleMultiplier);
				mousePosition.subtract(newCanvasMousePos, currentOffset);

				updateRenderOffset();
			}
		});

		c.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseReleased(MouseEvent e)
			{
				dragging = false;
				currentOffset.add(dragOffset, currentOffset);
				dragOffset.set(0, 0);
				updateRenderOffset();

			}

			@Override
			public void mousePressed(MouseEvent e)
			{
				dragging = true;
				dragStart.x = e.getX();
				dragStart.y = e.getY();
			}
		});

		c.addMouseMotionListener(new MouseMotionAdapter()
		{
			@Override
			public void mouseMoved(MouseEvent e)
			{
				mousePosition.x = e.getX();
				mousePosition.y = e.getY();
				
				c.repaint();
			}
			
			@Override
			public void mouseDragged(MouseEvent e)
			{
				mousePosition.x = e.getX();
				mousePosition.y = e.getY();
				
				if (dragging)
				{
					mousePosition.subtract(dragStart, dragOffset);
					updateRenderOffset();
				}
			}
		});
	}

	public void finishedLoadingGerber()
	{
//		maxX = 0;
//		maxY = 0;
//		for (Aperture a : apertures)
//		{
//			if (a.offset.x > maxX)
//				maxX = a.offset.x;
//			if (a.offset.y > maxY)
//				maxY = a.offset.y;
//			
////			a.area.getBounds2D(); // TODO
//		}
//		maxX = Utils.toPixels(maxX);
//		maxY = Utils.toPixels(maxY);
//
//		scale = 1.0;
//		currentOffset.set((c.getWidth() - maxX) * 0.5, (maxY - c.getHeight()) * 0.5);

		updateRenderOffset();
		
		loadedGerber = true;
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
			
			AffineTransform transform = graphics2D.getTransform();

			graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); // Set anti-alias!
			// graphics2D.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON); // Set anti-alias for text

			// Draw background:
			g.setColor(Color.BLACK);
			g.fillRect(0, 0, getWidth(), getHeight());
			
			if (!loadedGerber)
				return;

			graphics2D.translate(renderOffset.x, renderOffset.y);
			graphics2D.scale(scale, -scale);
			graphics2D.translate(0, -getHeight());
			
			g.setColor(Color.WHITE);
			for (Renderable r : objects)
				r.render(graphics2D);
			

//			for (Aperture a : apertures)
//			{
//				a.render(graphics2D);
//			}
//			
//
//			for (Entry<Integer, ArrayList<Shape>> e : traces.entrySet())
//			{
//				graphics2D.setStroke(new BasicStroke((float) Utils.toPixels(e.getKey()), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
//				
//				for (Shape l : e.getValue())
//					graphics2D.draw(l);
//			}
//			
//			graphics2D.setStroke(new BasicStroke((float) 1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
//			for (Path2D p : pours)
//			{
//				graphics2D.draw(p); //TODO change to fill when negative pours are achievable
//			}
			
			graphics2D.setTransform(transform);
			g.setFont(new Font("Consolas", Font.PLAIN, 20));
			g.setColor(Color.WHITE);
			graphics2D.drawString(String.format("X: %.0f", (mousePosition.x - currentOffset.x) / scale), 5, 20);
			graphics2D.drawString(String.format("Y: %.0f", (mousePosition.y - currentOffset.y) / scale), 5, 40);
		}
	}
}
