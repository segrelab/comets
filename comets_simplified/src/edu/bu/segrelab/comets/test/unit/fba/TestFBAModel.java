/**
 * 
 */
package edu.bu.segrelab.comets.test.unit.fba;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileWriter;
import java.net.URL;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import edu.bu.segrelab.comets.Comets;
import edu.bu.segrelab.comets.exception.ModelFileException;
import edu.bu.segrelab.comets.fba.FBACometsLoader;
import edu.bu.segrelab.comets.fba.FBAModel;
import edu.bu.segrelab.comets.test.classes.TComets;
import edu.bu.segrelab.comets.test.etc.TestKineticParameters;

/**An incomplete test case for the FBAModel class.
 * 
 * Note: You may experience an error along the lines of "no gurobijni70 in java.library.path"
 * The simplest solution is to copy the .dll files it's asking for from the /bin folder
 * of your Gurobi installation into the /bin of the JRE you're using. The more involved solution
 * is to check that your IDE can handle compiling C++
 * 
 * @author mquintin
 *
 */
public class TestFBAModel {

	private static FBAModel biomassUndeclared;
	private static FBAModel biomassDeclared;
	
	private static TComets tcomets = new TComets();

	/**Functions that only get called once, before instantiating this test class
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		URL url = TestFBAModel.class.getResource("../../resources/model_CSP.txt");
		biomassUndeclared = FBAModel.loadModelFromFile(url.getPath());
		url = TestFBAModel.class.getResource("../../resources/model_CSP_biomass.txt");
		biomassDeclared = FBAModel.loadModelFromFile(url.getPath());
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	/**Build objects to be fed to the Constructor
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		////This is leftover from an incorrect attempt at initializing the FBAModels for testing.
		////I'm leaving it here temporarily to bookmark how to start Comets with command line args
		//Comets comets = new Comets(new String[]{"-loader", FBACometsLoader.class.getName()});
		//FBACometsLoader loader = new FBACometsLoader();
		//biomassUndeclared = (FBAModel) loader.loadModelFromFile(comets,
		//		"/comets_simplified/src/edu/bu/segrelab/comets/test/resources/model_CSP.txt");
		//biomassDeclared = (FBAModel) loader.loadModelFromFile(comets, 
		//		"/comets_simplified/src/edu/bu/segrelab/comets/test/resources/model_CSP_biomass.txt");
		/*Comets.EXIT_AFTER_SCRIPT = false;

		//create the comets_script files in the proper location, and populate it with the absolute path to the layout
		URL scriptFolderURL = TestKineticParameters.class.getResource("../resources/");
		String folderPath = scriptFolderURL.getPath();
		String scriptPath = folderPath + File.separator + "comets_script_2carbons.txt";
		String layoutPath = folderPath + File.separator + "comets_layout_2carbons.txt";
		FileWriter fw = new FileWriter(new File(scriptPath), false);
		fw.write("load_layout " + layoutPath);
		fw.close();
		
		scriptFolderURL = TestKineticParameters.class.getResource("../resources/");
		folderPath = scriptFolderURL.getPath();
		scriptPath = folderPath + File.separator + "comets_script_2carbons_multiObj.txt";
		layoutPath = folderPath + File.separator + "comets_layout_2carbons_multiObj.txt";
		fw = new FileWriter(new File(scriptPath), false);
		fw.write("load_layout " + layoutPath);
		fw.close();
		*/
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testConstructors(){

	}

	//confirm that building the models in setUp() worked
	//this belongs in an integration test...
	@Test
	public void testFBACometsLoaderLoadModelFromFile(){
		assertEquals(27,biomassUndeclared.getObjectiveIndex());
		assertEquals(27,biomassUndeclared.getBiomassReaction());
		assertEquals(19,biomassDeclared.getObjectiveIndex());
		assertEquals(27,biomassDeclared.getBiomassReaction());
	}

	/**
	 * Test method for {@link edu.bu.segrelab.comets.fba.FBAModel#run()}.
	 */
	@Test
	public void testRun() {
		//Assert that the exit code = 5 (success)
		int resUndeclared = biomassUndeclared.run();
		assertEquals(5,resUndeclared);

		int resDeclared = biomassDeclared.run();
		assertEquals(5,resDeclared);
	}

	/**Test files with multiple objective functions.
	 * For now, spot check that these get a different result.
	 */
	@Test
	public void testMultiObjective() {
		String res1 = runCometsLayout2CarbonsB(); //maximize biomass
		//String res2 = runCometsLayout2CarbonsMultiObj(); //maximize Carbon1 uptake, then biomass
		System.out.println(res1);
		//System.out.println(res2);
	}

	private String runCometsLayout2Carbons() {
		//Comets.EXIT_AFTER_SCRIPT = false;
		int[] targetRxns = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
		URL urlS = TestFBAModel.class.getResource("../../resources/comets_script_2carbons.txt");
		Comets comets = new Comets(new String[]{"-loader", FBACometsLoader.class.getName(),
				"-script", urlS.getPath()});
		String result = "Single Objective Flux Solutions:";
		for (int i : targetRxns) {
			result = result + " " + String.valueOf(((FBAModel) comets.getModels()[0]).getFluxes()[i]);
		}
		return result;
	}
	
	private String runCometsLayout2CarbonsB() {
		String modelPath = "testmodel_2carbons.txt";
		int[] targetRxns = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
				
		double[] fluxes = tcomets.runModelFile(modelPath);
		
		String result = "Single Objective Flux Solutions:";
		for (int i : targetRxns) {
			result = result + " " + String.valueOf(fluxes[i]);
		}
		return result;
	}

	private String runCometsLayout2CarbonsMultiObj() {
		//Comets.EXIT_AFTER_SCRIPT = false;
		int[] targetRxns = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
		URL urlM = TestFBAModel.class.getResource("../../resources/comets_script_2carbons_multiObj.txt");
		Comets comets = new Comets(new String[]{"-loader", FBACometsLoader.class.getName(),
				"-script", urlM.getPath()});
		String result = "Multi  Objective Flux Solutions:";
		for (int i : targetRxns) {
			result = result + " " + String.valueOf(((FBAModel) comets.getModels()[0]).getFluxes()[i]);
		}
		return result;
	}
	/*
	 * Test method for {@link edu.bu.segrelab.comets.fba.FBAModel#FBAModel(double[][], double[], double[], int, int)}.
	 */
	/*@Test
	public void testFBAModelDoubleArrayArrayDoubleArrayDoubleArrayIntInt() {
		//fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for {@link edu.bu.segrelab.comets.fba.FBAModel#FBAModel(double[][], double[], double[], int, int, int)}.
	 */
	/*@Test
	public void testFBAModelDoubleArrayArrayDoubleArrayDoubleArrayIntIntInt() {
		//fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for {@link edu.bu.segrelab.comets.fba.FBAModel#FBAModel(double[][], double[], double[], int, int, int[], double[], double[], double[], double[], double[], double[], java.lang.String[], java.lang.String[], int, int)}.
	 */
	/*@Test
	public void testFBAModelDoubleArrayArrayDoubleArrayDoubleArrayIntIntIntArrayDoubleArrayDoubleArrayDoubleArrayDoubleArrayDoubleArrayDoubleArrayStringArrayStringArrayIntInt() {
		//fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for {@link edu.bu.segrelab.comets.fba.FBAModel#FBAModel(double[][], double[], double[], int, int[], double[], double[], double[], double[], double[], double[], java.lang.String[], java.lang.String[], int, int)}.
	 */
	/*@Test
	public void testFBAModelDoubleArrayArrayDoubleArrayDoubleArrayIntIntArrayDoubleArrayDoubleArrayDoubleArrayDoubleArrayDoubleArrayDoubleArrayStringArrayStringArrayIntInt() {
		//fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for {@link edu.bu.segrelab.comets.fba.FBAModel#getBiomassFluxSolution()}.
	 */
	/*@Test
	public void testGetBiomassFluxSolution() {
		//fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for {@link edu.bu.segrelab.comets.fba.FBAModel#getBiomassReaction()}.
	 */
	/*@Test
	public void testGetBiomassReaction() {
		//fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for {@link edu.bu.segrelab.comets.fba.FBAModel#setBiomassReaction(int)}.
	 */
	/*@Test
	public void testSetBiomassReaction() {
		//fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for {@link edu.bu.segrelab.comets.Model#clone()}.
	 */
	/*@Test
	public void testClone() {
		//fail("Not yet implemented"); // TODO
	}

	 */
}
