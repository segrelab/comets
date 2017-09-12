package edu.bu.segrelab.comets.test.etc;

import static org.junit.Assert.*;

import java.net.URL;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.CyclicBarrier;

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

import edu.bu.segrelab.comets.Cell;
import edu.bu.segrelab.comets.Comets;
import edu.bu.segrelab.comets.CometsConstants;
import edu.bu.segrelab.comets.CometsParameters;
import edu.bu.segrelab.comets.Model;
import edu.bu.segrelab.comets.PackageParameters;
import edu.bu.segrelab.comets.World2D;
import edu.bu.segrelab.comets.event.CometsChangeListener;
import edu.bu.segrelab.comets.event.CometsLoadListener;
import edu.bu.segrelab.comets.event.SimulationStateChangeListener;
import edu.bu.segrelab.comets.fba.FBACometsLoader;
import edu.bu.segrelab.comets.fba.FBAModel;
public class TestKineticParameters implements CometsConstants{

	static Comets comets;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		Comets.EXIT_AFTER_SCRIPT = false;
		//Load layout and models
		URL scriptURL = TestKineticParameters.class.getResource("../resources/resKineticParameters/comets_script.txt");
		//I got lazy and hardcoded the path to the layout in the script. You should change this as appropriate.
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
		getCometsDS(comets);
		assertEquals(1,2);
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
}