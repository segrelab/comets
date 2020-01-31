package edu.bu.segrelab.comets.test.integration;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import gurobi.*;

/**A class to test anything related to the objective functions used by the Gurobi optimizer
 * 
 * @author mquintin
 */
public class TestGurobiObjectiveFunctions {

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
	public void test() throws GRBException {
		checkGurobiVersion();
		//fail("Not yet implemented");
	}
	
	public void checkGurobiVersion() throws GRBException{
		try {
			GRBEnv env = new GRBEnv();
			int x = 1;
			String str = env.toString();
			int y = x;
			System.out.println(str);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		//gurobi.GurobiJni.version(null);
	}

}
