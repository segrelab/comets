/**
 * 
 */
package edu.bu.segrelab.comets.test.integration;

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
 * is to check that your IDE can handle compiling C++, and make sure that your builder has gurobi.jar
 * pointing to the proper bin folder as its native library location
 * 
 * @author mquintin
 *
 */
public class IntTestFBAModelOptimization {

	/*The TemporaryFolder Rule allows creation of files and folders that should 
	 * be deleted when the test method finishes (whether it passes or fails). Whether 
	 * the deletion is successful or not is not checked by this rule. No exception 
	 * will be thrown in case the deletion fails.
	 */
	@Rule
	public TemporaryFolder tempdir= new TemporaryFolder();
	
	private static boolean VERBOSE = false;

	private static FBAModel biomassUndeclared;
	private static FBAModel biomassDeclared;

	private static TComets tcomets = new TComets();

	/**Functions that only get called once, before instantiating this test class
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		URL url = IntTestFBAModelOptimization.class.getResource("../resources/model_CSP.txt");
		biomassUndeclared = FBAModel.loadModelFromFile(url.getPath());
		url = IntTestFBAModelOptimization.class.getResource("../resources/model_CSP_biomass.txt");
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
		//Comets.EXIT_AFTER_SCRIPT = false;

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
	public void testBiomassReactionSetOnLoad(){
		assertEquals(27,biomassUndeclared.getObjectiveIndex());
		assertEquals(27,biomassUndeclared.getBiomassReaction());
		assertEquals(19,biomassDeclared.getObjectiveIndex());
		assertEquals(27,biomassDeclared.getBiomassReaction());
	}

	/**
	 * Test method for {@link edu.bu.segrelab.comets.fba.FBAModel#run()}.
	 * Make sure that models run successfully
	 */
	@Test
	public void testRun() {
		//Assert that the exit code = 5 (success)
		int resUndeclared = biomassUndeclared.run();
		assertEquals(5,resUndeclared);

		//make sure that a model with a declared biomass reaction runs
		int resDeclared = biomassDeclared.run();
		assertEquals(5,resDeclared);
	}

	/**Test files with multiple objective functions.
	 * For now, spot check that these get a different result.
	 */
	@Test
	public void testMultiObjective() {
		try {
			//This model has two "Biomass" reactions, indexes 3 and 4. These create
			//an intermediate metabolite which is converted into biomass in Reaction 5.
			//Reaction 4 is more efficient, so given no constraints Reaction 3
			//should not be used when maximizing Reaction 5.
			//First, run it with just the single objective: [5]
			FBAModel model1 = tcomets.runModelString(testmodel_2carbons, tempdir);
			double[] fluxes1 = model1.getFluxes();
			String result1 = "Single Objective Flux Solutions:";
			for (int i = 0; i < fluxes1.length; i++) {
				result1 = result1 + " " + String.valueOf(fluxes1[i]);
			}
			if (VERBOSE) System.out.println(result1);
			
			double err = 1e-9; //allowable error due to rounding etc
			assertEquals(1.0,fluxes1[4],err); //remember the models start counting at 1 so [4]==5
			assertEquals(1.0,fluxes1[3],err); //rxn4 is maximized
			assertEquals(0.0,fluxes1[2],err); //rxn3 is not used
			
			//run the model with two objectives: [5 -4]
			//First maximize biomass. Then minimize use of the efficient pathway
			FBAModel model2 = tcomets.runModelString(testmodel_2carbons_multiObj, tempdir);
			double[] fluxes2 = model2.getFluxes();
			String result2 = "Multi  Objective Flux Solutions:";
			for (int i = 0; i < fluxes2.length; i++) {
				result2 = result2 + " " + String.valueOf(fluxes2[i]);
			}
			if (VERBOSE) System.out.println(result2);
			
			assertEquals(1.0,fluxes2[4],err); //the primary objective should not be affected
			assertEquals(0.0,fluxes2[3],err); //rxn4 is minimized
			assertEquals(1.0,fluxes2[2],err); //rxn3 picks up the slack

		} catch (IOException e) {
			fail();
			e.printStackTrace();
		}
	}

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
			"    5 -4\r\n" + 
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
