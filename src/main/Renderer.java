package main;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Composite;
import java.awt.CompositeContext;
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
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

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

	double maxX = 0, maxY = 0;

	transient boolean loadedGerber = false;

	private ArrayList<Layer> layers = new ArrayList<>();

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

	BufferedImage everything;

	public class AdditiveComposite implements Composite
	{
		public AdditiveComposite()
		{
			super();
		}

		public CompositeContext createContext(ColorModel srcColorModel, ColorModel dstColorModel, RenderingHints hints)
		{
			return new AdditiveCompositeContext();
		}
	}
	
	public int fun = 0;

	public class AdditiveCompositeContext implements CompositeContext
	{
		public AdditiveCompositeContext()
		{
		};

		public void compose(Raster src, Raster dstIn, WritableRaster dstOut)
		{
			int w1 = src.getWidth();
			int h1 = src.getHeight();
			int chan1 = src.getNumBands();
			int w2 = dstIn.getWidth();
			int h2 = dstIn.getHeight();
			int chan2 = dstIn.getNumBands();

			int minw = Math.min(w1, w2);
			int minh = Math.min(h1, h2);
			int minCh = Math.min(chan1, chan2);

			// This bit is horribly inefficient,
			// getting individual pixels rather than all at once.
			
//			float[] srcData = src.getPixels(0, 0, w1, h1, (float[]) null);
			
			for (int x = 0; x < dstIn.getWidth(); x++)
			{
				for (int y = 0; y < dstIn.getHeight(); y++)
				{
					float[] pxSrc = null;
					pxSrc = src.getPixel(x, y, pxSrc);
					float[] pxDst = null;
					pxDst = dstIn.getPixel(x, y, pxDst);

					float alpha = 255;
					if (pxSrc.length > 3)
					{
						alpha = pxSrc[3];
					}
					
					if (fun == 0)
					{
						pxSrc[1] = 0;
						pxSrc[2] = 0;
					}
					else if (fun == 1)
					{
						pxSrc[0] = 0;
						pxSrc[2] = 0;
					}
					else
					{
						pxSrc[0] = 0;
						pxSrc[1] = 0;
					}
					

					for (int i = 0; i < 3 && i < minCh; i++)
					{
						pxDst[i] = Math.min(255, (pxSrc[i] * (alpha / 255)) + (pxDst[i]));
						dstOut.setPixel(x, y, pxDst);
					}
				}
			}
		}

		public void dispose()
		{
		}
	}

	public void addLayers(Layer... layers)
	{
		for (Layer l : layers)
			this.layers.add(l);

		ArrayList<BufferedImage> layerImages = new ArrayList<>();
		
//		int size = 32000;
		int size = 1000;

		for (Layer l : layers)
		{
			BufferedImage bufferedImage = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
			layerImages.add(bufferedImage);

			Graphics2D layerG2D = (Graphics2D) bufferedImage.getGraphics();
			
			layerG2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			
			// Flip
			layerG2D.translate(0, size);
			layerG2D.scale(1, -1);

			layerG2D.setColor(Color.WHITE);
			for (Renderable r : l.objects)
				r.render(layerG2D);
		}

		everything = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
		Graphics2D layerG2D = (Graphics2D) everything.getGraphics();
//		layerG2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		
		layerG2D.setColor(Color.WHITE);
		fun = 2;
		layerG2D.setXORMode(Color.YELLOW);
		layerG2D.drawImage(layerImages.get(0), 0, 0, null);
		
//		layerG2D.setComposite(new AdditiveComposite());

//		layerG2D.setColor(Color.BLUE);
		fun = 0;
		layerG2D.setXORMode(Color.CYAN);
		layerG2D.drawImage(layerImages.get(1), 0, 0, null);

//		layerG2D.setColor(Color.GREEN);
		fun = 1;
		layerG2D.setXORMode(Color.MAGENTA);
		layerG2D.drawImage(layerImages.get(2), 0, 0, null);
		
//		layerG2D.get
		
//		File outputfile = new File("layers.png");
//		try
//		{
//			ImageIO.write(everything, "png", outputfile);
//		}
//		catch (IOException e)
//		{
//			e.printStackTrace();
//		}
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
			graphics2D.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON); // Set anti-alias for text

			// Draw background:
			g.setColor(Color.BLACK);
			g.fillRect(0, 0, getWidth(), getHeight());

			if (!loadedGerber)
				return;

			graphics2D.translate(renderOffset.x, renderOffset.y);
			graphics2D.scale(scale, scale);
//			graphics2D.translate(0, -getHeight());

			graphics2D.drawImage(everything, 0, 0, null);

			graphics2D.setTransform(transform);
			g.setFont(new Font("Consolas", Font.PLAIN, 20));
			g.setColor(Color.WHITE);
			graphics2D.drawString(String.format("X: %.0f", (mousePosition.x - currentOffset.x) / scale), 5, 20);
			graphics2D.drawString(String.format("Y: %.0f", (mousePosition.y - currentOffset.y) / scale), 5, 40);
		}
	}
}
