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
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import main.GeometricPrimitives.Coordinate;
import main.GeometricPrimitives.GeometricPrimitive;

public class Renderer
{
	private JFrame frame;

	double scale = 1.0;

	boolean dragging = false;
	Coordinate dragStart = new Coordinate(0, 0);
	Coordinate currentOffset = new Coordinate(0, 0);

	Coordinate renderOffset = new Coordinate(0, 0);

	public void createWindow()
	{
		try
		{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex)
		{
		}

		frame = new JFrame("Gerber Rasteriser");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setLayout(new BorderLayout());
		frame.add(new Canvas());
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);

		frame.addMouseWheelListener(new MouseWheelListener()
		{
			@Override
			public void mouseWheelMoved(MouseWheelEvent e)
			{
				if (e.getPreciseWheelRotation() < 0)
					scale *= 1.1;
				else
					scale /= 1.1;

				frame.repaint();

				System.out.println(scale);
			}
		});

		frame.addMouseListener(new MouseListener()
		{

			@Override
			public void mouseReleased(MouseEvent e)
			{
				dragging = false;
				currentOffset.x = renderOffset.x;
				currentOffset.y = renderOffset.y;
				renderOffset.x = 0;
				renderOffset.y = 0;

			}

			@Override
			public void mousePressed(MouseEvent e)
			{
				dragging = true;
				dragStart.x = e.getX();
				dragStart.y = e.getY();

				System.out.println("Dragging");
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

		frame.addMouseMotionListener(new MouseMotionListener()
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
					renderOffset.x = e.getX() - dragStart.x + currentOffset.x;
					renderOffset.y = e.getY() - dragStart.y + currentOffset.y;

					System.out.println(renderOffset.x + ", " + renderOffset.y);

					frame.repaint();
				}
			}
		});
	}

	public ArrayList<GeometricPrimitive> primitives = new ArrayList<>();

	public void redraw()
	{
		frame.repaint();
	}

	public class Canvas extends JPanel
	{
		private static final long serialVersionUID = 1L;

		public Canvas()
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
		}
	}
}
