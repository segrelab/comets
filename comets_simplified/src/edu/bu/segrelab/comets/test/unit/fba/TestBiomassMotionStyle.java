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

import edu.bu.segrelab.comets.fba.FBAParameters.BiomassMotionStyle;

/**A test case for the Enum FBAParameters.BiomassMotionStyle
 * 
 * @author mquintin
 * Last update 2/14/2017
 */
public class TestBiomassMotionStyle{


	/** Versions of COMETS up to v2.2.2 used the value "Diffusion (Crank-Nicolson)"
	 * instead of "Diffusion 2D(Crank-Nicolson)".
	 * Test that trying to invoke BiomassMotionStyle with either version returns
	 * DIFFUSION_CN, and that the value of DIFFUSION_CN contains "2D"
	 * 
	 * This allows backwards compatibility with files created for v2.2.2 or earlier
	 * @author mquintin
	 * 14 February 2017
	 */
	@Test
	public void testLegacyCN(){
		String oldCN = "Diffusion (Crank-Nicolson)";
		String newCN = "Diffusion 2D(Crank-Nicolson)";

		BiomassMotionStyle sans2d = BiomassMotionStyle.findByName(oldCN);
		assertEquals(sans2d, BiomassMotionStyle.DIFFUSION_CN);
		
		BiomassMotionStyle with2d = BiomassMotionStyle.findByName(newCN);
		assertEquals(with2d, BiomassMotionStyle.DIFFUSION_CN);
		
		assertEquals(newCN, BiomassMotionStyle.DIFFUSION_CN.getName());
	}

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

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
	}

}
