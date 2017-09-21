/**
 * 
 */
package edu.bu.segrelab.comets.test.unit.fba;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

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

	/*The TemporaryFolder Rule allows creation of files and folders that should 
	 * be deleted when the test method finishes (whether it passes or fails). Whether 
	 * the deletion is successful or not is not checked by this rule. No exception 
	 * will be thrown in case the deletion fails.
	 */
	@Rule
	public TemporaryFolder tempdir= new TemporaryFolder();

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
		try {
			String res1 = runCometsLayout2Carbons();
			System.out.println(res1);
			String res2 = runCometsLayout2CarbonsMultiObj();
			System.out.println(res2);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			fail();
			e.printStackTrace();
		} //maximize biomass
		//String res2 = runCometsLayout2CarbonsMultiObj(); //maximize Carbon1 uptake, then biomass
		//System.out.println(res2);
	}

	private String runCometsLayout2Carbons() throws IOException {
		FBAModel model = tcomets.runModelString(testmodel_2carbons, tempdir);
		double[] fluxes = model.getFluxes();
		String result = "Single Objective Flux Solutions:";
		for (int i = 0; i < fluxes.length; i++) {
			result = result + " " + String.valueOf(fluxes[i]);
		}
		return result;
	}

	private String runCometsLayout2CarbonsMultiObj() throws IOException {
		FBAModel model = tcomets.runModelString(testmodel_2carbons_multiObj, tempdir);
		double[] fluxes = model.getFluxes();
		String result = "Multi  Objective Flux Solutions:";
		for (int i = 0; i < fluxes.length; i++) {
			result = result + " " + String.valueOf(fluxes[i]);
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

	/* -------------Testing File Contents ---------------------- */

	public static String testmodel_2carbons = "SMATRIX  7  10\r\n" + 
			"    1   1   -1.000000\r\n" + 
			"    1   3   -1.000000\r\n" + 
			"    1   6   -1.000000\r\n" + 
			"    2   1   -6.000000\r\n" + 
			"    2   2   -3.000000\r\n" + 
			"    2   8   -1.000000\r\n" + 
			"    3   1   -6.000000\r\n" + 
			"    3   2   -3.000000\r\n" + 
			"    3   9   -1.000000\r\n" + 
			"    4   1   6.000000\r\n" + 
			"    4   2   3.000000\r\n" + 
			"    4   3   -10.000000\r\n" + 
			"    4   4   -10.000000\r\n" + 
			"    5   2   -1.000000\r\n" + 
			"    5   4   -2.000000\r\n" + 
			"    5   7   -1.000000\r\n" + 
			"    6   3   -1.000000\r\n" + 
			"    6   4   -1.000000\r\n" + 
			"    6   10   -1.000000\r\n" + 
			"    7   3   1.000000\r\n" + 
			"    7   4   1.000000\r\n" + 
			"    7   5   -1.000000\r\n" + 
			"//\r\n" + 
			"BOUNDS  -1000  1000\r\n" + 
			"    1   0.000000   1000.000000\r\n" + 
			"    2   0.000000   1000.000000\r\n" + 
			"    3   0.000000   1000.000000\r\n" + 
			"    4   0.000000   1000.000000\r\n" + 
			"    5   0.000000   1000.000000\r\n" + 
			"    6   -10.000000   1000.000000\r\n" + 
			"    7   -10.000000   1000.000000\r\n" + 
			"    8   -10.000000   1000.000000\r\n" + 
			"    9   -10.000000   1000.000000\r\n" + 
			"    10   -10.000000   1000.000000\r\n" + 
			"//\r\n" + 
			"OBJECTIVE\r\n" + 
			"    5\r\n" + 
			"//\r\n" + 
			"METABOLITE_NAMES\r\n" + 
			"    c1\r\n" + 
			"    o\r\n" + 
			"    p\r\n" + 
			"    atp\r\n" + 
			"    c2\r\n" + 
			"    n\r\n" + 
			"    bio\r\n" + 
			"//\r\n" + 
			"REACTION_NAMES\r\n" + 
			"    atp_1\r\n" + 
			"    atp_2\r\n" + 
			"    biomass_1\r\n" + 
			"    biomass_2\r\n" + 
			"    objective\r\n" + 
			"    EX_c1\r\n" + 
			"    EX_c2\r\n" + 
			"    EX_o\r\n" + 
			"    EX_p\r\n" + 
			"    EX_n\r\n" + 
			"//\r\n" + 
			"EXCHANGE_REACTIONS\r\n" + 
			" 6 7 8 9 10\r\n" + 
			"//\r\n" + 
			"";

	public static String testmodel_2carbons_multiObj = "SMATRIX  7  10\r\n" + 
			"    1   1   -1.000000\r\n" + 
			"    1   3   -1.000000\r\n" + 
			"    1   6   -1.000000\r\n" + 
			"    2   1   -6.000000\r\n" + 
			"    2   2   -3.000000\r\n" + 
			"    2   8   -1.000000\r\n" + 
			"    3   1   -6.000000\r\n" + 
			"    3   2   -3.000000\r\n" + 
			"    3   9   -1.000000\r\n" + 
			"    4   1   6.000000\r\n" + 
			"    4   2   3.000000\r\n" + 
			"    4   3   -10.000000\r\n" + 
			"    4   4   -10.000000\r\n" + 
			"    5   2   -1.000000\r\n" + 
			"    5   4   -2.000000\r\n" + 
			"    5   7   -1.000000\r\n" + 
			"    6   3   -1.000000\r\n" + 
			"    6   4   -1.000000\r\n" + 
			"    6   10   -1.000000\r\n" + 
			"    7   3   1.000000\r\n" + 
			"    7   4   1.000000\r\n" + 
			"    7   5   -1.000000\r\n" + 
			"//\r\n" + 
			"BOUNDS  -1000  1000\r\n" + 
			"    1   0.000000   1000.000000\r\n" + 
			"    2   0.000000   1000.000000\r\n" + 
			"    3   0.000000   1000.000000\r\n" + 
			"    4   0.000000   1000.000000\r\n" + 
			"    5   0.000000   1000.000000\r\n" + 
			"    6   -10.000000   1000.000000\r\n" + 
			"    7   -10.000000   1000.000000\r\n" + 
			"    8   -10.000000   1000.000000\r\n" + 
			"    9   -10.000000   1000.000000\r\n" + 
			"    10   -10.000000   1000.000000\r\n" + 
			"//\r\n" + 
			"OBJECTIVE\r\n" + 
			"    4 -3\r\n" + 
			"//\r\n" + 
			"METABOLITE_NAMES\r\n" + 
			"    c1\r\n" + 
			"    o\r\n" + 
			"    p\r\n" + 
			"    atp\r\n" + 
			"    c2\r\n" + 
			"    n\r\n" + 
			"    bio\r\n" + 
			"//\r\n" + 
			"REACTION_NAMES\r\n" + 
			"    atp_1\r\n" + 
			"    atp_2\r\n" + 
			"    biomass_1\r\n" + 
			"    biomass_2\r\n" + 
			"    objective\r\n" + 
			"    EX_c1\r\n" + 
			"    EX_c2\r\n" + 
			"    EX_o\r\n" + 
			"    EX_p\r\n" + 
			"    EX_n\r\n" + 
			"//\r\n" + 
			"EXCHANGE_REACTIONS\r\n" + 
			" 6 7 8 9 10\r\n" + 
			"//\r\n" + 
			"";
}
