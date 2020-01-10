package edu.bu.segrelab.comets.test.unit.reaction;

import java.io.File;
import java.io.FileWriter;
import java.net.URL;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import edu.bu.segrelab.comets.Comets;
import edu.bu.segrelab.comets.fba.FBACell;
import edu.bu.segrelab.comets.fba.FBACometsLoader;
import edu.bu.segrelab.comets.reaction.ExternalReactionCalculator;
import edu.bu.segrelab.comets.test.classes.TComets;
import edu.bu.segrelab.comets.test.etc.TestKineticParameters;

public class TestExternalReactionCalculator {

	static TComets comets = null;
	int checking = 1;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		//Load layout and models
		URL scriptURL = TestExternalReactionCalculator.class.getResource("../../resources/resKineticParameters/comets_script_exRxn.txt");
		TComets tcomets = new TComets(new String[]{"-loader", FBACometsLoader.class.getName(),
				"-script", scriptURL.getPath()});
		tcomets.loadScript();
		TestExternalReactionCalculator.comets = tcomets;
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		//comets.exitProgram();
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}
	
	@Test
	public void testCreateCalculator() {
		FBACell cell = (FBACell) comets.getCells().get(0);
		ExternalReactionCalculator calc = ExternalReactionCalculator.createCalculator(cell);

	}
	
	@Test
	public void testCalcEnzymaticRate() {
		
	}
	
	@Test
	public void testCalcRxnRates() {
		
	}
	
	@Test
	public void testRk4() {
		
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}
	
	

}
