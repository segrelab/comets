package edu.bu.segrelab.comets.test.etc;

import java.io.File;
import java.io.FileWriter;
import java.net.URL;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import edu.bu.segrelab.comets.Comets;
import edu.bu.segrelab.comets.CometsConstants;
import edu.bu.segrelab.comets.fba.FBACometsLoader;
import edu.bu.segrelab.comets.fba.FBAModel;

/**A test class to experiment with kinetic parameters in an attempt to reconcile 
 * PseudoOrganism behavior with single-enzyme Michaelis-Menten kinetics
 * 
 * The layout is a 1x1 world with two models- the Reactor pseudoorganism performs
 * catalysis and produces products and inactivated enzyme. The Resetter 
 * pseudoorganism converts all inactivated enzyme back into active enzyme
 * 
 * @author mquintin 3/21/2017
 *
 */
public class TestKineticParameters implements CometsConstants{

	static Comets comets;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		Comets.EXIT_AFTER_SCRIPT = false;

		//Load layout and models
		//create the comets_script file in the proper location, and populate it with the absolute path to the layout
		URL scriptFolderURL = TestKineticParameters.class.getResource("../resources/resKineticParameters/");
		String folderPath = scriptFolderURL.getPath();
		String scriptPath = folderPath + File.separator + "comets_script.txt";
		String layoutPath = folderPath + File.separator + "comets_layout.txt";
		FileWriter fw = new FileWriter(new File(scriptPath), false);
		fw.write("load_layout " + layoutPath);
		fw.close();
		
		URL scriptURL = TestKineticParameters.class.getResource("../resources/resKineticParameters/comets_script.txt");

		comets = new Comets(new String[]{"-loader", FBACometsLoader.class.getName(),
				"-script", scriptURL.getPath()});
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		comets.exitProgram();
	}

	@Before
	public void setUp() throws Exception {
		//comets.resetSimulationToSavedState();
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testSubstrateConsumption() {
		double ds = getCometsDS(comets);
		//assertEquals(1,2);
		int x = 1; //breakpoint
	}

	/**Calculate the substrate consumption rate for the last timestep in the given Comets world
	 * 
	 * @param c
	 */
	private static double getCometsDS(Comets c){
		FBAModel reactor = (FBAModel) c.getModels()[0];
		double ds = reactor.getExchangeFluxes()[3];
		return ds;
	}

	/**Calculate what Michaelis-Menten kinetics say the substrate consumption rate should
	 * be for the first timestep in the given Comets world
	 * 
	 * @return
	 */
	private static double getMMDS(Comets c){
		FBAModel reactor = (FBAModel) c.getModels()[0];
		
		
		double kcat=1.0, e=1.0, s=1.0, km=1.0;
		double v = kcat * e * s / (km + s);
		return v;
	}
	
	/**Test a situation where a decaying metabolite is present in very low concentration, such that some steps of
	 * RK4 may set its velocity to be negative. If not well implemented, this will result in it "slingshotting"
	 * up to a high concentration
	 * @throws IOException 
	 * 
	 */
	/*@Test
	public void testRK4Depletion() throws IOException {
		TComets tc = new TComets();
		//TemporaryFolder tempFolder = new TemporaryFolder();
		//tempFolder.create();
		//File layoutFile = tempFolder.newFile();
		//String layoutFilePath = layoutFile.getAbsolutePath();
		//FileWriter fw = new FileWriter(layoutFilePath);
		//fw.write(rk4DepletionTestString);
		//fw.close();
		tc.createScriptForLayout("rk4testlayout.txt");
		tc.run();
	}*/
	
	
	protected static String rk4DepletionTestString = "	parameters\r\n" + 
			"	maxCycles = 10\r\n" + 
			"	timeStep = 0.0167\r\n" + 
			"//\r\n" + 
			"model_file\r\n" + 
			"	model_world\r\n" + 
			"		grid_size 1 1\r\n" + 
			"		world_media\r\n" + 
			"		met 1\r\n" + 
			"	//\r\n" + 
			"	diffusion_constants 1.000000e-06\r\n" + 
			"	//\r\n" + 
			"	media\r\n" + 
			"	//\r\n" + 
			"	media_refresh 0\r\n" + 
			"	//\r\n" + 
			"	static_media 0 0\r\n" + 
			"	//\r\n" + 
			"	barrier\r\n" + 
			"	//\r\n" + 
			"//\r\n" + 
			"initial_pop filled\r\n" + 
			"//\r\n" + 
			"reactions\r\n" + 
			"	reactants\r\n" + 
			"		1 1 1 1.000000e-01\r\n" + 
			"	enzymes\r\n" + 
			"	products\r\n" + 
			"//\r\n" + 
			"";
}