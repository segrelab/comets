package edu.bu.segrelab.comets.test.etc;

import static org.junit.Assert.*;

import java.net.URL;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
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

import edu.bu.segrelab.comets.Comets;
import edu.bu.segrelab.comets.PackageParameters;
import edu.bu.segrelab.comets.fba.FBACometsLoader;
public class TestKineticParameters {

	static Comets comets;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		//Load layout and models
		URL scriptURL = TestKineticParameters.class.getResource("../resources/resKineticParameters/comets_script.txt");
		comets = new Comets(new String[]{"-loader", FBACometsLoader.class.getName(),
				"-script", scriptURL.getPath()});
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
		comets.resetSimulationToSavedState();
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test() {
		fail("Not yet implemented");
	}

}
