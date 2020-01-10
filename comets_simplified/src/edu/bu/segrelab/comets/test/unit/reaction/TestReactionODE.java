package edu.bu.segrelab.comets.test.unit.reaction;

import static edu.bu.segrelab.comets.IWorld.reactionModel;
import static org.junit.Assert.*;

import java.io.File;
import java.io.FileWriter;
import java.net.URL;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import edu.bu.segrelab.comets.Comets;
import edu.bu.segrelab.comets.IWorld;
import edu.bu.segrelab.comets.fba.FBACometsLoader;
import edu.bu.segrelab.comets.reaction.ReactionModel;
import edu.bu.segrelab.comets.reaction.ReactionODE;
import edu.bu.segrelab.comets.test.classes.TComets;
import edu.bu.segrelab.comets.test.etc.TestKineticParameters;

public class TestReactionODE {

	private static TComets comets;
	private static ReactionModel reactionModel;
	private ReactionODE reactionODE;
	
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
	}
	
	@Before
	public void setUp() throws Exception {
		reactionModel = IWorld.reactionModel;
		reactionODE = reactionModel.getReactionODE();
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		//comets.exitProgram();
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testComputeDerivatives() {
		//fail("Not yet implemented");
	}

	@Test
	public void testCalcRxnRate() {
		//fail("Not yet implemented");
	}

	@Test
	public void testGetDimension() {
		//the standard template should have dimensionality of 7
		//It contains 8 metabolites, but enzyme concentration is constant
		int dim = reactionODE.getDimension();
		//assertEquals(7,dim);
		//TODO: this concept may not be right, since the solver is expecting the dimension to equal the number of metabolites
		//so it should be reporting 8 until we change how the enzyme concentrations are passed.
		assertEquals(8,dim);
	}

}
