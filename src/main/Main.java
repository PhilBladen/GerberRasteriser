package main;

import static main.Utils.log;

import java.io.File;

public class Main
{
	private final String APP_NAME = "Gerber Rasteriser";
	private final String APP_VERSION = "v0.1";

	private Renderer renderer;

	public Main()
	{
		log("Running " + APP_NAME + " " + APP_VERSION);

		renderer = new Renderer();
		renderer.createWindow();
		
		Layer copper = new Layer(new File("Reference files/STAR-XL CCT.GTL"));
		Layer silk = new Layer(new File("Reference files/STAR-XL CCT.GTO"));
//		Layer copper2 = new Layer(new File("E:/PJB/Programming/Java/Workspace/GerberRasteriser/Reference files/GerberTest/GerberFiles/copper_top.gbr"));
		renderer.addLayers(copper, silk);
		
		renderer.finishedLoadingGerber();

		log("Complete!");
	}
	
	public static void main(String[] args)
	{
		new Main();
	}
}
