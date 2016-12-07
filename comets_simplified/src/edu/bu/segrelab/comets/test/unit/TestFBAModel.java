/**
 * 
 */
package edu.bu.segrelab.comets.test.unit;

import static org.junit.Assert.*;

import java.net.URL;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import edu.bu.segrelab.comets.Comets;
import edu.bu.segrelab.comets.Model;
import edu.bu.segrelab.comets.fba.FBACometsLoader;
import edu.bu.segrelab.comets.fba.FBAModel;

/**An incomplete test case for the FBAModel class. As of December 2016 tests in this
 * file are only intended to confirm functionality of a change that allows the user
 * to specify a biomass reaction separately from the objective reaction
 * 
 * Note: You may experience an error along the lines of "no gurobijni70 in java.library.path"
 * The simplest solution is to copy the .dll files it's asking for from the /bin folder
 * of your Gurobi installation into the /bin of the JRE you're using
 * 
 * @author mquintin
 *
 */
public class TestFBAModel {

	private static FBAModel biomassUndeclared;
	private static FBAModel biomassDeclared;
	
	/**Functions that only get called once, before instantiating this test class
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		URL url = TestFBAModel.class.getResource("../resources/model_CSP.txt");
		biomassUndeclared = FBAModel.loadModelFromFile(url.getPath());
		url = TestFBAModel.class.getResource("../resources/model_CSP_biomass.txt");
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

	/**
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
