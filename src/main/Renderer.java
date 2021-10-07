package main;

import static main.Utils.log;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

import main.Utils.Timer;
import main.graphicalobjects.Renderable;
import main.math.Vector2d;

public class Renderer
{
	private JFrame frame;
	private GerberCanvas c;
	private JProgressBar pbar;
	private JLabel label;

	boolean dragging = false;
	Vector2d dragStart = new Vector2d(0, 0);
	Vector2d currentOffset = new Vector2d(0, 0);
	Vector2d dragOffset = new Vector2d(0, 0);
	Vector2d renderOffset = new Vector2d(0, 0);
	Vector2d mousePosition = new Vector2d(0, 0);
	double scale = 1.0;

	double maxX = 0, maxY = 0;

	transient boolean loadedGerber = false;

	private BufferedImage everything;
	private Rectangle2D bounds;
	
	private Unit units = Unit.MM;
	
	private enum Unit
	{
		MM, MIL
	}

	private void updateRenderOffset()
	{
		renderOffset.set(currentOffset.add(dragOffset));

		// if (renderOffset.x < 0)
		// renderOffset.x = 0;
		//
		// if (renderOffset.y < 0)
		// renderOffset.y = 0;

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
		bottom.add(label = new JLabel("Loading gerbers..."), BorderLayout.EAST);

		JMenuBar menuBar = new JMenuBar();
		JMenu menu;

		menu = new JMenu("File");
		menu.setMnemonic(KeyEvent.VK_F);
		menuBar.add(menu);

		JMenuItem menuItem = new JMenuItem("Export PNG", KeyEvent.VK_E);
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK));
		menuItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				if (everything != null)
				{
					JFileChooser j = new JFileChooser();
					j.setDialogType(JFileChooser.SAVE_DIALOG);
					j.setSelectedFile(new File("Layers.png"));
					j.setFileFilter(new FileNameExtensionFilter("PNG image (*.png)", "png"));
					if (j.showSaveDialog(frame) != JFileChooser.APPROVE_OPTION)
						return;

					File selectedFile = j.getSelectedFile();
					if (selectedFile == null)
						return;

					String fileName = selectedFile.getName();
					if (fileName.lastIndexOf(".") != -1)
						fileName = fileName.substring(0, fileName.lastIndexOf("."));

					selectedFile = new File(selectedFile.getParentFile(), fileName + ".png");
					if (selectedFile.exists())
					{
						int input = JOptionPane.showConfirmDialog(null, selectedFile.getName() + " already exists. Would you like to replace it?");
						if (input != JOptionPane.YES_OPTION)
							return;
					}

					label.setText("Saving output image...");
					pbar.setValue(0);
					Utils.log("Saving output image...");
					try
					{
						ImageIO.write(everything, "png", selectedFile);
					}
					catch (IOException ex)
					{
						ex.printStackTrace();
					}
					pbar.setValue(100);
					label.setText("Done.");
				}
			}
		});
		menu.add(menuItem);
		
		menuItem = new JMenuItem("Toggle units (mm/mil)", KeyEvent.VK_T);
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, 0));
		menuItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				if (units == Unit.MM)
					units = Unit.MIL;
				else
					units = Unit.MM;
				
				c.repaint();
			}
		});
		menu.add(menuItem);

		frame = new JFrame("Gerber Rasteriser");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setLayout(new BorderLayout());
		frame.add(c, BorderLayout.CENTER);
		frame.add(bottom, BorderLayout.SOUTH);
		frame.setJMenuBar(menuBar);
		frame.pack();
		frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
		frame.setLocationRelativeTo(null);

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

				if (scale > Config.maxZoom)
					scale = Config.maxZoom;
				if (scale < Config.minZoom)
					scale = Config.minZoom;

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

		frame.setVisible(true);
	}

	public void addLayers(Layer... layers)
	{
		pbar.setMaximum(100);

		double minX = Double.MAX_VALUE;
		double minY = Double.MAX_VALUE;
		double maxX = Double.MIN_VALUE;
		double maxY = Double.MIN_VALUE;
		for (int layerIndex = 0; layerIndex < layers.length; layerIndex++)
		{
			Layer l = layers[layerIndex];
			if (l == null)
				continue;

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
		int width = (int) (maxX - minX) + Config.exportBorderSize * 2 + 1; // +1 provides additional pixel for anti-aliasing to flow into
		int height = (int) (maxY - minY) + Config.exportBorderSize * 2 + 1;

		Object dstData;
		int color, shift;
		if (Config.use16BitColor)
		{
			everything = new BufferedImage(width, height, BufferedImage.TYPE_USHORT_555_RGB);
			dstData = new short[width * height];
			color = 0x1F;
			shift = -3;
		}
		else
		{
			everything = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
			dstData = new int[width * height];
			color = 0xFF;
			shift = 0;
		}
		
		for (int layerIndex = 0; layerIndex < layers.length; layerIndex++)
		{
			Layer l = layers[layerIndex];
			if (l == null)
				continue;

			Timer.tic();

			BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
			Graphics2D layerG2D = (Graphics2D) bufferedImage.getGraphics();
			layerG2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			// Flip:
			layerG2D.translate(0, height);
			layerG2D.scale(1, -1);

			// Compensate for layer origin:
			layerG2D.translate(-minX + Config.exportBorderSize, -minY + Config.exportBorderSize);

			layerG2D.setColor(Color.WHITE);
			for (Renderable r : l.objects)
				r.render(layerG2D);
			
			Utils.log(String.format("Layer render time: %.2fs", (Timer.toc() * 0.001)));
			
//			 if (layerIndex == 0)
//				 bufferedImage = Utils.blur(bufferedImage, 1);
//			 else if (layerIndex == 2)
//				 bufferedImage = Utils.blur(bufferedImage, 1);

			// Composite onto existing image:
			Timer.tic();
			byte[] src = ((DataBufferByte) bufferedImage.getData().getDataBuffer()).getData();
			if (Config.use16BitColor)
			{
				for (int pixelIndex = 0; pixelIndex < width * height; pixelIndex++)
				{
					if (shift < 0)
						((short[]) dstData)[pixelIndex] |= (src[pixelIndex] >> -shift) & color;
					else
						((short[]) dstData)[pixelIndex] |= (src[pixelIndex] << shift) & color;
				}
				color <<= 5;
				shift += 5;
			}
			else
			{
				for (int pixelIndex = 0; pixelIndex < width * height; pixelIndex++)
					((int[]) dstData)[pixelIndex] |= (src[pixelIndex] << shift) & color;
				color <<= 8;
				shift += 8;
			}
			Utils.log(String.format("Layer composite time: %.2fs", (Timer.toc() * 0.001)));

			pbar.setValue(100 * (layerIndex + 1) / (layers.length + 1));
		}
		everything.getRaster().setDataElements(0, 0, width, height, dstData);

		Utils.log(String.format("Bounds: x: %f, y: %f, X: %f, Y: %f", minX, minY, maxX, maxY));
		bounds = new Rectangle2D.Double(minX, minY, maxX - minX, maxY - minY);

		double scaleRequiredToFitHeight = c.getHeight() / ((double) height + 200);
		double scaleRequiredToFitWidth = c.getWidth() / ((double) width + 200);
		scale = Math.min(scaleRequiredToFitHeight, scaleRequiredToFitWidth);
		currentOffset.set((c.getWidth() - width * scale) * 0.5, (c.getHeight() - height * scale) * 0.5);

		pbar.setValue(100);

		System.gc();

		updateRenderOffset();

		loadedGerber = true;

		log("All layers loaded.");
		label.setText("All layers loaded.");
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

			if (!loadedGerber)
				return;
			
			{
				graphics2D.translate(renderOffset.x, renderOffset.y);
				graphics2D.scale(scale, scale);
				// graphics2D.translate(0, -getHeight());

				graphics2D.drawImage(everything, 0, 0, null);

				if (Config.drawOuterBoundingBox)
				{
					graphics2D.setColor(Color.ORANGE);
					graphics2D.draw(new Rectangle2D.Double(Config.exportBorderSize, Config.exportBorderSize, bounds.getWidth(), bounds.getHeight()));
				}

	

			}
			
			graphics2D.setTransform(transform);
			
			Vector2d gerberOrigin_canvasPixels = new Vector2d(-bounds.getX() + Config.exportBorderSize, (bounds.getHeight() + bounds.getY()) + Config.exportBorderSize);
			graphics2D.translate(renderOffset.x + gerberOrigin_canvasPixels.x * scale, renderOffset.y + gerberOrigin_canvasPixels.y * scale);
			graphics2D.setColor(Color.ORANGE);
			graphics2D.setStroke(new BasicStroke(3));
			graphics2D.draw(new Line2D.Double(-10, 0, 10, 0));
			graphics2D.draw(new Line2D.Double(0, -10, 0, 10));
//			graphics2D.drawLine(0, -10, 0, 10);

			graphics2D.setTransform(transform);

			g.setFont(new Font("Consolas", Font.PLAIN, 20));
			g.setColor(Color.WHITE);
			
			double mouseX_canvasPixels = (mousePosition.x - currentOffset.x) / scale;
			double mouseY_canvasPixels = (mousePosition.y - currentOffset.y) / scale;
			
			double mouseX_gerberPixels = mouseX_canvasPixels - Config.exportBorderSize;
			double mouseY_gerberPixels = (bounds.getHeight() - mouseY_canvasPixels) + Config.exportBorderSize;
			
			double mouseXFromGerberOrigin_gerberPixels = mouseX_gerberPixels + bounds.getX();
			double mouseYFromGerberOrigin_gerberPixels = mouseY_gerberPixels + bounds.getY();
			
			if (units == Unit.MM)
			{
				graphics2D.drawString(String.format("X: %.2fmm", (mouseXFromGerberOrigin_gerberPixels / Config.nanosToPixels) * 1E-6), 5, 20);
				graphics2D.drawString(String.format("Y: %.2fmm", (mouseYFromGerberOrigin_gerberPixels / Config.nanosToPixels) * 1E-6), 5, 40);
			}
			else
			{
				graphics2D.drawString(String.format("X: %.0fmil", (mouseXFromGerberOrigin_gerberPixels / Config.rasterDPI) * 1E3), 5, 20);
				graphics2D.drawString(String.format("Y: %.0fmil", (mouseYFromGerberOrigin_gerberPixels / Config.rasterDPI) * 1E3), 5, 40);
			}
			graphics2D.drawString(String.format("RAM usage: %.0fMB (reserved: %.0fMB)", (runtime.totalMemory() - runtime.freeMemory()) * 1E-6, runtime.totalMemory() * 1E-6), 5, 60);
//			graphics2D.drawString(String.format("Mem free: %.0fMB", (runtime.freeMemory()) * 1E-6), 5, 80);
		}
	}
}
