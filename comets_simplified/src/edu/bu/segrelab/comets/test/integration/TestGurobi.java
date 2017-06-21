package edu.bu.segrelab.comets.test.integration;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import gurobi.*;

public class TestGurobi {

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

	/**Used to check environment configuration in debug mode
	 * 
	 */
	@Test
	public void testEnvironment() {
		try {
			String jpath = System.getProperty("java.library.path");
			@SuppressWarnings("unused")
			GRBEnv env = new GRBEnv();
		} catch (GRBException e) {
			e.printStackTrace();
		}
	}

}
