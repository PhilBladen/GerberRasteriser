package main;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferUShort;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.EmptyBorder;

import main.Utils.Timer;

public class Renderer
{
	private JFrame frame;
	private GerberCanvas c;
	private JProgressBar pbar;

	boolean dragging = false;
	Vector2d dragStart = new Vector2d(0, 0);
	Vector2d currentOffset = new Vector2d(0, 0);
	Vector2d dragOffset = new Vector2d(0, 0);
	Vector2d renderOffset = new Vector2d(0, 0);
	Vector2d mousePosition = new Vector2d(0, 0);
	double scale = 1.0;

	double maxX = 0, maxY = 0;

	transient boolean loadedGerber = false;

	private ArrayList<Layer> layers = new ArrayList<>();

	private void updateRenderOffset()
	{
		renderOffset.set(currentOffset.add(dragOffset));
		
//		if (renderOffset.x < 0)
//			renderOffset.x = 0;
//
//		if (renderOffset.y < 0)
//			renderOffset.y = 0;

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
		
		JPanel bottom = new JPanel();
		BorderLayout l;
		bottom.setLayout(l = new BorderLayout());
		l.setHgap(10);
		bottom.setBorder(new EmptyBorder(0, 0, 0, 10));
		bottom.add(pbar = new JProgressBar(), BorderLayout.CENTER);
		JLabel label;
		bottom.add(label = new JLabel("Loading gerbers..."), BorderLayout.EAST);

		frame = new JFrame("Gerber Rasteriser");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setLayout(new BorderLayout());
		frame.add(c, BorderLayout.CENTER);
		frame.add(bottom, BorderLayout.SOUTH);
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

				Vector2d canvasMousePos = mousePosition.subtract(currentOffset).multiply(1.0 / scale);

				double scaleMultiplier;
				final double scalePerClick = 1.2;
				if (e.getPreciseWheelRotation() < 0.0)
					scaleMultiplier = scalePerClick;
				else
					scaleMultiplier = 1.0 / scalePerClick;

				scale *= scaleMultiplier;
				
				if (scale > 10)
					scale = 10;
				if (scale < 0.1)
					scale = 0.1;

				Vector2d newCanvasMousePos = canvasMousePos.multiply(scale);
				mousePosition.subtract(newCanvasMousePos, currentOffset);

				updateRenderOffset();
			}
		});

		frame.addKeyListener(new KeyListener()
		{
			@Override
			public void keyTyped(KeyEvent e)
			{
			}

			@Override
			public void keyReleased(KeyEvent e)
			{
			}

			@Override
			public void keyPressed(KeyEvent e)
			{
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

	BufferedImage everything;
	Rectangle2D bounds;

	int numRunningThreads = 0;
	Object lock = new Object();

	public void addLayers(Layer... layers)
	{
		for (Layer l : layers)
			this.layers.add(l);

		int width = 10000;
		int height = 10000;

		pbar.setMaximum(100);
		
		double minX = Double.MAX_VALUE;
		double minY = Double.MAX_VALUE;
		double maxX = Double.MIN_VALUE;
		double maxY = Double.MIN_VALUE;
		for (int layerIndex = 0; layerIndex < layers.length; layerIndex++)
		{
			Layer l = layers[layerIndex];
			
			Timer.tic();
			Rectangle2D layerBounds = l.calculateBounds();
			if (layerBounds.getMinX() < minX)
				minX = layerBounds.getMinX();
			if (layerBounds.getMinY() < minY)
				minY = layerBounds.getMinY();
			if (layerBounds.getMaxX() > maxX)
				maxX = layerBounds.getMaxX();
			if (layerBounds.getMaxY() > maxY)
				maxY = layerBounds.getMaxY();
			Utils.log(String.format("Bounds calc time: %.3fs", (Timer.toc() * 0.001)));
		}
		width = (int) (maxX - minX);
		height = (int) (maxY - minY);
		
		everything = new BufferedImage(width, height, BufferedImage.TYPE_USHORT_555_RGB);
		short[] dstData = new short[width * height];

		short color = 0x1F;
		short shift = -3;
		
		for (int layerIndex = 0; layerIndex < layers.length; layerIndex++)
		{
			Layer l = layers[layerIndex];

			Timer.tic();

			BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);

			Graphics2D layerG2D = (Graphics2D) bufferedImage.getGraphics();

			layerG2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			// Flip
			layerG2D.translate(0, height);
			layerG2D.scale(1, -1);

			layerG2D.setColor(Color.WHITE);
			for (Renderable r : l.objects)
			{
				r.render(layerG2D);
			}
			
//			if (layerIndex == 0)
//			{
//				int blurRadius = 3;
//				
//				float[] matrix = new float[blurRadius * blurRadius];
//				for (int i = 0; i < blurRadius * blurRadius; i++)
//					matrix[i] = 1.0f/(blurRadius * blurRadius);
//				
//				Kernel kernel = new Kernel(blurRadius, blurRadius, matrix);
//				BufferedImageOp op = new ConvolveOp(kernel);
//				bufferedImage = op.filter(bufferedImage, null);
//			}
//			else if (layerIndex == 2)
//			{
//				int blurRadius = 5;
//				
//				float[] matrix = new float[blurRadius * blurRadius];
//				for (int i = 0; i < blurRadius * blurRadius; i++)
//					matrix[i] = 1.0f/(blurRadius * blurRadius);
//				
//				Kernel kernel = new Kernel(blurRadius, blurRadius, matrix);
//				BufferedImageOp op = new ConvolveOp(kernel);
//				bufferedImage = op.filter(bufferedImage, null);
//			}
				

			byte[] src = ((DataBufferByte) bufferedImage.getData().getDataBuffer()).getData();

			for (int pixelIndex = 0; pixelIndex < width * height; pixelIndex++)
			{
				if (shift < 0)
					dstData[pixelIndex] |= (src[pixelIndex] >> -shift) & color;
				else
					dstData[pixelIndex] |= (src[pixelIndex] << shift) & color;
			}

			color <<= 5;
			shift += 5;

			Utils.log(String.format("Layer render time: %.2fs", (Timer.toc() * 0.001)));
			
			pbar.setValue(100 * (layerIndex + 1) / (layers.length + 1));
		}

		everything.getRaster().setDataElements(0, 0, width, height, dstData);
		
		
		Utils.log(String.format("Bounds: x: %f, y: %f, X: %f, Y: %f", minX, minY, maxX, maxY));
		bounds = new Rectangle2D.Double(minX, minY, maxX - minX, maxY - minY);
		
		
		scale = 1.0;
//		currentOffset.set((c.getWidth() - (maxX - minX)) * 0.5, ((maxY - minY) - c.getHeight()) * 0.5);
		
		
		
		
		
		Utils.log("Saving output image...");

		// Utils.log("Composite time: " + (Timer.toc() * 0.001));

//		File outputfile = new File("layers.png");
//		try
//		{
//			ImageIO.write(everything, "png", outputfile);
//		}
//		catch (IOException e)
//		{
//			e.printStackTrace();
//		}
		
		pbar.setValue(100);
	}

	public void finishedLoadingGerber()
	{
		// maxX = 0;
		// maxY = 0;
		// for (Aperture a : apertures)
		// {
		// if (a.offset.x > maxX)
		// maxX = a.offset.x;
		// if (a.offset.y > maxY)
		// maxY = a.offset.y;
		//
		//// a.area.getBounds2D(); // TODO
		// }
		// maxX = Utils.toPixels(maxX);
		// maxY = Utils.toPixels(maxY);
		//
		// scale = 1.0;
		// currentOffset.set((c.getWidth() - maxX) * 0.5, (maxY - c.getHeight()) * 0.5);

		System.gc();

		updateRenderOffset();

		loadedGerber = true;
	}

	public class GerberCanvas extends JPanel
	{
		private static final long serialVersionUID = 1L;
		
		private Runtime runtime = Runtime.getRuntime();

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

			graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			graphics2D.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

			// Draw background:
			g.setColor(Color.BLACK);
			g.fillRect(0, 0, getWidth(), getHeight());

			if (loadedGerber)
			{
				graphics2D.translate(renderOffset.x, renderOffset.y);
				graphics2D.scale(scale, scale);
				// graphics2D.translate(0, -getHeight());

				graphics2D.drawImage(everything, 0, 0, null);
				
//				graphics2D.setColor(Color.ORANGE);
//				graphics2D.draw(bounds);
			}

			graphics2D.setTransform(transform);
			
			g.setFont(new Font("Consolas", Font.PLAIN, 20));
			g.setColor(Color.WHITE);
			graphics2D.drawString(String.format("X: %.0f", (mousePosition.x - currentOffset.x) / scale), 5, 20);
			graphics2D.drawString(String.format("Y: %.0f", (mousePosition.y - currentOffset.y) / scale), 5, 40);
			graphics2D.drawString(String.format("Mem total: %.0fMB", (runtime.totalMemory()) * 1E-6), 5, 60);
			graphics2D.drawString(String.format("Mem free: %.0fMB", (runtime.freeMemory()) * 1E-6), 5, 80);
		}
	}
}
