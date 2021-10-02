package main;

import static main.Utils.log;

import java.io.File;

import javax.swing.JFileChooser;

public class Main
{
	private final String APP_NAME = "Gerber Rasteriser";
	private final String APP_VERSION = "v0.2";

	private Renderer renderer;
	
	private File previousPath = null;
	
	private Layer loadLayer()
	{
		JFileChooser j = new JFileChooser();
		j.setDialogType(JFileChooser.OPEN_DIALOG);
		j.setDialogTitle("Open copper (blue) gerber file");
		j.setCurrentDirectory(previousPath);
		if (j.showOpenDialog(null) != JFileChooser.APPROVE_OPTION)
			return null;
		
		previousPath=  j.getSelectedFile().getParentFile();
		
		File file = j.getSelectedFile();
		if (file != null && file.exists())
		{
			Layer layer = new Layer(file);
			return layer;
		}
		
		return null;
	}

	public Main()
	{
		log("Running " + APP_NAME + " " + APP_VERSION);

		renderer = new Renderer();
		renderer.createWindow();
		
		Layer copper = loadLayer();
		Layer solderResist = loadLayer();
		Layer silk = loadLayer();
		renderer.addLayers(copper, solderResist, silk);
	}
	
	public static void main(String[] args)
	{
		new Main();
	}
}
