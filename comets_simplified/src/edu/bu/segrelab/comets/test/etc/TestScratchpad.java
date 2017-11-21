package edu.bu.segrelab.comets.test.etc;

import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import edu.bu.segrelab.comets.CometsConstants;
import edu.bu.segrelab.comets.test.classes.TComets;
/**A class for miscellaneous testing and experimentation
 * 
 * @author mquintin
 *
 */
public class TestScratchpad {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testDoubleArr() {
		//This should be second nature but I've been using too much Matlab :|
		//confirm how to manipulate nested arrays and check emptiness
		double[][] nest = new double[3][3]; //initializes all values to 0
		nest[2][2] = 2.0;
		double[] line1 = nest[1];
		double[] line2 = nest[2];
		
		//to get the sum
		double s = Arrays.stream(line2).sum();
		
		//to check if any elements !=0
		boolean nonzero = false;
		for (double d : line2){
			if (d != 0.0){
				nonzero = true;
				break;
			}
		}
		
		boolean pause = true;
		if (pause){
			int i = 1; //just here for the breakpoint
		}
		
	}

	@Test
	public void testNoisyExternalReactionRates() {
		/*The external reaction products in a certain layout are showing 
		 * a noisy timecourse when I expect them to be relatively smooth.
		 * This function is to explore what's going on.
		 */
		TComets tc = new TComets();
		tc.setScriptFileName("C:/sync/biomes/cellulose/optima/temp/comets_script.txt");
		//CometsConstants.DEBUG = true;
		tc.run();
	}
}
