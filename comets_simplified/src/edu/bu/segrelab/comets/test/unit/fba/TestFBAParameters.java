/**
 * 
 */
package edu.bu.segrelab.comets.test.unit.fba;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import edu.bu.segrelab.comets.Comets;
import edu.bu.segrelab.comets.fba.FBAParameters;

/**Test class for {@link FBAParameters}
 * 
 * This class is being built up gradually. Currently tested features are:
 * 	randomOrder: proper behavior in getter/setter and set/save/loadParameterState()
 * 
 * TODO: how to create a Comets object for the constructor call in setUp()?
 * 
 * @author mquintin
 * Last update 12/19/2016
 */
public class TestFBAParameters{

	private FBAParameters pParams;
	
	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		pParams = new FBAParameters((Comets) null);
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
	}

	/**
	 * Test method for {@link edu.bu.segrelab.comets.CometsParameters#loadParameterState()}.
	 */
	@Test
	public void testLoadParameterState() {
		//TODO: This is failing because the Comets object given to pParams's constructor is null,
		//so its fields can't be set back to the defaults
		pParams.setParameter("randomorder", "true");
		pParams.loadParameterState();
		assertTrue(pParams.getRandomOrder());

		pParams.setParameter("randomorder", "false");
		pParams.loadParameterState();
		assertFalse(pParams.getRandomOrder());
	}

	/**
	 * Test method for {@link edu.bu.segrelab.comets.FBAParameters#saveParameterState()}.
	 */
	@Test
	public void testSaveParameterState() {
		fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for {@link edu.bu.segrelab.comets.FBAParameters#setParameter(java.lang.String, java.lang.String)}.
	 */
	@Test
	public void testSetParameter() {
		fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for {@link edu.bu.segrelab.comets.FBAParameters#getRandomOrder()}.
	 */
	@Test
	public void testGetAndSetRandomOrder() {
		//fail("Not yet implemented"); // TODO
		
	}

	private static String parametersFileBlock = "	maxCycles = 200\r\n" + 
			"	pixelScale = 5\r\n" + 
			"	saveslideshow = false\r\n" + 
			"	slideshowExt = png\r\n" + 
			"	slideshowColorRelative = true\r\n" + 
			"	slideshowRate = 1\r\n" + 
			"	slideshowLayer = 324\r\n" + 
			"	writeFluxLog = false\r\n" + 
			"	FluxLogName = ./flux.txt\r\n" + 
			"	FluxLogRate = 1\r\n" + 
			"	writeMediaLog = false\r\n" + 
			"	MediaLogName = ./media.txt\r\n" + 
			"	MediaLogRate = 1\r\n" + 
			"	writeBiomassLog = false\r\n" + 
			"	BiomassLogName = ./biomass.txt\r\n" + 
			"	BiomassLogRate = 1\r\n" + 
			"	writeTotalBiomassLog = false\r\n" + 
			"	totalBiomassLogRate = 1\r\n" + 
			"	TotalbiomassLogName = ./total_biomass\r\n" + 
			"	useLogNameTimeStamp = false\r\n" + 
			"	slideshowName = ./res.png\r\n" + 
			"	numDiffPerStep = 10\r\n" + 
			"	numRunThreads = 1\r\n" + 
			"	growthDiffRate = 0\r\n" + 
			"	flowDiffRate = 3e-09\r\n" + 
			"	exchangestyle = Monod Style\r\n" + 
			"	defaultKm = 0.01\r\n" + 
			"	defaultHill = 1\r\n" + 
			"	timeStep = 0.01\r\n" + 
			"	deathRate = 0\r\n" + 
			"	spaceWidth = 0.01\r\n" + 
			"	randomOrder = false\r\n" +
			"	maxSpaceBiomass = 0.00022\r\n" + 
			"	minSpaceBiomass = 2.5e-11\r\n" + 
			"	allowCellOverlap = true\r\n" + 
			"	toroidalWorld = false\r\n" + 
			"	showCycleTime = true\r\n" + 
			"	showCycleCount = true\r\n" + 
			"	defaultVmax = 10\r\n" + 
			"	biomassMotionStyle = Diffusion 2D(Crank-Nicolson)\r\n" + 
			"	exchangeStyle = Standard FBA\r\n";
	
}
