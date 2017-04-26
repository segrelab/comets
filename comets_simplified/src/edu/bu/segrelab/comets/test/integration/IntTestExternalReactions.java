/**
 * 
 */
package edu.bu.segrelab.comets.test.integration;

import static org.junit.Assert.*;

import java.net.URL;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import edu.bu.segrelab.comets.Comets;
import edu.bu.segrelab.comets.fba.FBACometsLoader;
import edu.bu.segrelab.comets.test.etc.TestKineticParameters;

/**Integration test class for features introduced in the External Reactions project.
 * 
 * 
 * @author Michael Quintin, April 2017
 *
 */
public class IntTestExternalReactions {
	
	/* Functions to build:
	 * -load rxns from file
	 * -calculate d[S]/dt for a given set of reactants
	 * -apply calculated reactant flux for one (sub)timestep
	 * 
	 * Features to build:
	 * -store reactive molecules in each reaction
	 * -store Km and kcat for each reaction
	 * -differentiate between Substrate and Catalyst role for each reactant in each rxn
	 * 
	 * 
	 * Questions:
	 * -rate equation for rxns where two reactants are consumed, there is no enzyme
	 * -format of input file
	 * 
	 * Considerations to keep in mind:
	 * -scale turnover rates to match the length of Timesteps
	 * -solve d[S]/dt as a system of ODEs, don't just settle for handling 
	 * rxns 1 by 1
	 * -break a timestep into multiple substeps, similar to how diffusion operates
	 */

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
	}

	@Test
	public void testSampleRxnLayout() {
		Comets.EXIT_AFTER_SCRIPT = false;
		//Load layout and models
		URL scriptURL = TestKineticParameters.class.getResource("../resources/sampleRxnLayout.txt");
		Comets comets = new Comets(new String[]{"-loader", FBACometsLoader.class.getName(),
				"-script", scriptURL.getPath()});
		int x = 1; //landing pad for breakpoint
	}

}
