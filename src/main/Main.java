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
		Layer solderResist = new Layer(new File("Reference files/STAR-XL CCT.GTS"));
		renderer.addLayers(copper, silk, solderResist);
//		renderer.addLayers(silk);
		
//		Layer copper = new Layer(new File("Reference files/GerberTest/GerberFiles/copper_top.gbr"));
//		Layer silk = new Layer(new File("Reference files/GerberTest/GerberFiles/silkscreen_top.gbr"));
//		Layer solderResist = new Layer(new File("Reference files/GerberTest/GerberFiles/soldermask_top.gbr"));
//		renderer.addLayers(copper, silk, solderResist);

		
		//		Layer copper2 = new Layer(new File("E:/PJB/Programming/Java/Workspace/GerberRasteriser/Reference files/GerberTest/GerberFiles/copper_top.gbr"));
		
		renderer.finishedLoadingGerber();

		log("All layers loaded.");
	}
	
	public static void main(String[] args)
	{
		new Main();
	}
}
