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
		//create the comets_script file in the proper location, and populate it with the absolute path to the layout
		URL scriptFolderURL = TestKineticParameters.class.getResource("../resources/resKineticParameters/");
		String folderPath = scriptFolderURL.getPath();
		String scriptPath = folderPath + File.separator + "comets_script_exRxn.txt";
		String layoutPath = folderPath + File.separator + "sampleRxnLayout.txt";
		FileWriter fw = new FileWriter(new File(scriptPath), false);
		fw.write("load_layout " + layoutPath);
		fw.close();
		
		URL scriptURL = TestKineticParameters.class.getResource("../resources/resKineticParameters/comets_script_exRxn.txt");

		comets = new TComets(new String[]{"-loader", FBACometsLoader.class.getName(),
				"-script", scriptURL.getPath()});
		comets.loadScript();
		//TestExternalReactionCalculator.comets = comets;
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
