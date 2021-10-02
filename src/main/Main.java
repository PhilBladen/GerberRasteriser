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
		renderer.addLayers(copper, solderResist, silk);
//		renderer.addLayers(copper, copper, copper);
		
//		Layer copper = new Layer(new File("Reference files/STAR-XL CCT.GBL"));
//		Layer silk = new Layer(new File("Reference files/STAR-XL CCT.GBO"));
//		Layer solderResist = new Layer(new File("Reference files/STAR-XL CCT.GBS"));
//		Layer tcopper = new Layer(new File("Reference files/STAR-XL CCT.GTL"));
//		Layer tsilk = new Layer(new File("Reference files/STAR-XL CCT.GTO"));
//		Layer tsolderResist = new Layer(new File("Reference files/STAR-XL CCT.GTS"));
//		renderer.addLayers(tcopper, tsilk, tsolderResist, copper, silk, solderResist);
		
//		Layer copper = new Layer(new File("Reference files/GerberTest/GerberFiles/copper_top.gbr"));
//		Layer silk = new Layer(new File("Reference files/GerberTest/GerberFiles/silkscreen_top.gbr"));
//		Layer solderResist = new Layer(new File("Reference files/GerberTest/GerberFiles/soldermask_top.gbr"));
//		renderer.addLayers(copper, silk, solderResist);

		
		//		Layer copper2 = new Layer(new File("E:/PJB/Programming/Java/Workspace/GerberRasteriser/Reference files/GerberTest/GerberFiles/copper_top.gbr"));
	}
	
	public static void main(String[] args)
	{
		new Main();
	}
}
