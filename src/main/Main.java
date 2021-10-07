package main;

import static main.Utils.log;

import java.io.File;

import javax.swing.JFileChooser;

public class Main
{
	private final String APP_NAME = "Gerber Rasteriser";
	private final String APP_VERSION = "v0.2";

	private Renderer renderer;
	
	private Layer loadLayer(String chooserTitle)
	{
		JFileChooser j = new JFileChooser();
		j.setDialogType(JFileChooser.OPEN_DIALOG);
		j.setDialogTitle(chooserTitle);
		j.setCurrentDirectory(new File(Config.defaultOpenPath));
		if (j.showOpenDialog(null) != JFileChooser.APPROVE_OPTION)
			return null;
		
		Config.setDefaultPath(j.getSelectedFile().getParent());
		
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
		
		Config.load();

		renderer = new Renderer();
		renderer.createWindow();
		
		Layer copper = loadLayer("Open copper (blue) gerber file");
		Layer solderResist = loadLayer("Open solder resist (green) gerber file");
		Layer silk = loadLayer("Open silk (red) gerber file");
		renderer.addLayers(copper, solderResist, silk);
	}
	
	public static void main(String[] args)
	{
		new Main();
	}
}
