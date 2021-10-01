package main;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Composite;
import java.awt.CompositeContext;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.MemoryImageSource;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import main.Utils.Timer;

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
			
			byte[] srcData = new byte[w1 * h1];
			src.getDataElements(0, 0, w1, h1, srcData);
			
			
			int[] dstData = new int[w1 * h1];

			
			for (int x = 0; x < dstIn.getWidth(); x++)
			{
				for (int y = 0; y < dstIn.getHeight(); y++)
				{
					if (srcData[y * w1 + x] != 0)
						dstData[y * w1 + x] = 0x00FFFFFF;
					
//					float[] pxSrc = null;
//					pxSrc = src.getPixel(x, y, pxSrc);
//					float[] pxDst = null;
//					pxDst = dstIn.getPixel(x, y, pxDst);
//
//					float alpha = 255;
//					if (pxSrc.length > 3)
//					{
//						alpha = pxSrc[3];
//					}
//					
////					if (fun == 0)
////					{
////						pxSrc[1] = 0;
////						pxSrc[2] = 0;
////					}
////					else if (fun == 1)
////					{
////						pxSrc[0] = 0;
////						pxSrc[2] = 0;
////					}
////					else
////					{
////						pxSrc[0] = 0;
////						pxSrc[1] = 0;
////					}
////					
//
//					for (int i = 0; i < 3 && i < minCh; i++)
//					{
//						pxDst[i] = Math.min(255, (pxSrc[i] * (alpha / 255)) + (pxDst[i]));
//						dstOut.setPixel(x, y, pxDst);
//					}
				}
			}
			

			dstOut.setDataElements(0, 0, w1, h1, dstData);
		}

		public void dispose()
		{
		}
	}
	
	int numRunningThreads = 0;
	Object lock = new Object();

	public void addLayers(Layer... layers)
	{
		for (Layer l : layers)
			this.layers.add(l);

		ArrayList<BufferedImage> layerImages = new ArrayList<>();
		
//		int size = 42000;
		int size = 10000;

		for (Layer l : layers)
		{
			Timer.tic();
			
			BufferedImage bufferedImage = new BufferedImage(size, size, BufferedImage.TYPE_BYTE_GRAY);
			layerImages.add(bufferedImage);

			Graphics2D layerG2D = (Graphics2D) bufferedImage.getGraphics();
			
			layerG2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			
			// Flip
			layerG2D.translate(0, size);
			layerG2D.scale(1, -1);

			layerG2D.setColor(Color.WHITE);
			for (Renderable r : l.objects)
				r.render(layerG2D);
			
			Utils.log("Layer render time: " + (Timer.toc() * 0.001));
		}
		
		everything = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);		
		
		Raster l1 = layerImages.get(0).getData();
		Raster l2 = layerImages.get(1).getData();
		Raster l3 = layerImages.get(2).getData();
				
		byte[] srcData1 = ((DataBufferByte) l1.getDataBuffer()).getData();
		byte[] srcData2 = ((DataBufferByte) l2.getDataBuffer()).getData();
		byte[] srcData3 = ((DataBufferByte) l3.getDataBuffer()).getData();
		
		int[] dstData = ((DataBufferInt) everything.getData().getDataBuffer()).getData();
		
		int numCores = 4;
		
		final int parallelHeight = size / numCores;
		
		Timer.tic();
				
		for (int threadIndex = 0; threadIndex < numCores; threadIndex++)
		{
			final int actualThreadIndex = threadIndex;
			
			numRunningThreads++;
			
			new Thread(new Runnable()
			{
				@Override
				public void run()
				{
//					int startRow = actualThreadIndex * parallelHeight;
//					int endRow = (actualThreadIndex + 1) * parallelHeight;
//					
//					for (int rowIndex = startRow; rowIndex < endRow; rowIndex++)
//					{
//						if (rowIndex % 250 == 0)
//							Utils.log(rowIndex);
//						
//						byte[] srcData1 = new byte[size];
//						l1.getDataElements(0, rowIndex, size, 1, srcData1);
//						
//						byte[] srcData2 = new byte[size * size];
//						l2.getDataElements(0, rowIndex, size, 1, srcData2);
//						
//						byte[] srcData3 = new byte[size * size];
//						l3.getDataElements(0, rowIndex, size, 1, srcData3);
//						
//						int[] dstData = new int[size];
//						
//						for (int pixelIndex = 0; pixelIndex < size; pixelIndex++)
//						{
//							dstData[pixelIndex] |= srcData1[pixelIndex] & 0x000000FF;
//							dstData[pixelIndex] |= (srcData2[pixelIndex] << 8) & 0x0000FF00;
//							dstData[pixelIndex] |= (srcData3[pixelIndex] << 16) & 0x00FF0000;
//						}
//						
//						everything.getRaster().setDataElements(0, rowIndex, size, 1, dstData);
//					}
					
					int startPixel = actualThreadIndex * parallelHeight * size;
					int endPixel = (actualThreadIndex + 1) * parallelHeight * size;
										
					for (int pixelIndex = startPixel; pixelIndex < endPixel; pixelIndex++)
					{
						dstData[pixelIndex] |= srcData1[pixelIndex] & 0x000000FF;
						dstData[pixelIndex] |= (srcData2[pixelIndex] << 8) & 0x0000FF00;
						dstData[pixelIndex] |= (srcData3[pixelIndex] << 16) & 0x00FF0000;
					}
					
					synchronized (lock)
					{						
						numRunningThreads--;
					}
				}
			}).start();
		}
		
		while (numRunningThreads > 0)
		{
			try
			{
				Thread.sleep(1);
			}
			catch (InterruptedException e)
			{
			}
		}

		everything.getRaster().setDataElements(0, 0, size, size, dstData);
		
		Utils.log("Composite time: " + (Timer.toc() * 0.001));

		
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
