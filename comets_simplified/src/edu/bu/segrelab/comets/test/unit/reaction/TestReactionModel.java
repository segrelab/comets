package edu.bu.segrelab.comets.test.unit.reaction;

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
import edu.bu.segrelab.comets.fba.FBAParameters;
import edu.bu.segrelab.comets.reaction.ReactionModel;
import edu.bu.segrelab.comets.test.classes.TComets;
import edu.bu.segrelab.comets.test.etc.TestKineticParameters;

public class TestReactionModel {

	private static TComets comets;
	private ReactionModel reactionModel;
	
	//used to reset between tests
	private static int[] defaultExRxnEnzymes;
	private static double[][] defaultExRxnParams;
	private static double[] defaultExRxnRateConstants;
	private static double[][] defaultExRxnStoich;
	private static String[] defaultMediaNames;
	private static double[] defaultWorldMedia;
	private static int defaultNSteps;
	
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
		
		URL scriptURL = TestReactionModel.class.getResource("../../resources/resKineticParameters/comets_script_exRxn.txt");

		comets = new TComets(new String[]{"-loader", FBACometsLoader.class.getName(),
				"-script", scriptURL.getPath()});
		comets.loadScript();
		defaultNSteps = ((FBAParameters) comets.getPackageParameters()).getNumExRxnSubsteps();
		ReactionModel rm = (ReactionModel) IWorld.getReactionModel();
		defaultExRxnEnzymes = rm.getExRxnEnzymes();
		defaultExRxnParams = rm.getExRxnParams();
		defaultExRxnRateConstants = rm.getExRxnRateConstants();
		defaultExRxnStoich = rm.getExRxnStoich();
		defaultMediaNames = rm.getMediaNames();
		defaultWorldMedia = rm.getWorld().getMediaAt(0,0,0);
	}
	
	@Before
	public void setUp() throws Exception {
		reactionModel = IWorld.getReactionModel();
		((FBAParameters) comets.getPackageParameters()).setNumExRxnSubsteps(defaultNSteps);
		reactionModel.getWorld().setMedia(0,0,0,defaultWorldMedia);
		reactionModel.setExRxnEnzymes(defaultExRxnEnzymes);
		reactionModel.setExRxnParams(defaultExRxnParams);
		reactionModel.setExRxnRateConstants(defaultExRxnRateConstants);
		reactionModel.setExRxnStoich(defaultExRxnStoich);
		reactionModel.setMediaNames(defaultMediaNames);
		reactionModel.saveState();
		reactionModel.reset();
		reactionModel.setup();
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		//comets.exitProgram();
	}

	@Test
	public void testRun() {
		//fail("Not yet implemented");
	}

	@Test
	public void testSetup() {
		//fail("Not yet implemented");
	}

	@Test
	public void testSaveState() {
		//fail("Not yet implemented");
	}

	@Test
	public void testReset() {
		//fail("Not yet implemented");
	}

	@Test
	public void testIsSetUp() {
		//fail("Not yet implemented");
	}

	//Test executeODE with a single exponential decay reaction
	@Test
	public void testExecuteODE_singleRxn() {
		//start with 10mmol reactant, decays at 10%/s, run for 10 seconds
		//Based on the decay reaction N(t) = N0 e^-lambda*t, expect 3.6788 mmol remaining 
		double[] rxnMedia = {10.0};
		int[] exRxnEnzymes = {-1};
		double[] exRxnRateConstants = {0.1};
		double[][] exRxnStoich = {{-1}};
		double[][] exRxnParams = {{1}};
		double timestep_seconds = 10;
		int maxIterations = 10000;
		
		comets.getParameters().setTimeStep(timestep_seconds / 3600);
		((FBAParameters) comets.getPackageParameters()).setNumExRxnSubsteps(maxIterations);
		reactionModel.setExRxnEnzymes(exRxnEnzymes);
		reactionModel.setExRxnParams(exRxnParams);
		reactionModel.setExRxnRateConstants(exRxnRateConstants);
		reactionModel.setExRxnStoich(exRxnStoich);
		reactionModel.setMediaNames(new String[] {"MetA"});
		reactionModel.saveState();
		reactionModel.reset();
		
		double rate = reactionModel.getReactionODE().calcRxnRate(0,new double[]{10.0});
		assertEquals(1.0,rate,0.0);
		double[] yDot = reactionModel.getReactionODE().retrieveDerivatives(timestep_seconds,new double[]{10.0});
		assertEquals(-1,yDot[0],0.0);
		
		try {
			double[] res = reactionModel.executeODE(rxnMedia, timestep_seconds, maxIterations);
			double con = res[0];
			assertEquals(3.6788,con,0.001);
		}
		catch (Exception e) {
			e.printStackTrace();
			fail("Exception in executeODE. See stack for details.");
		}
		
	}
	
	/*Check that increasing the number of substeps doesn't produce radically different results, after a point */
	@Test
	public void testStabilityOfDifferentStepsizes() {
		boolean printAll = false;
		//start with 10mmol reactant, decays at 10%/s, run for 10 seconds
		//Based on the decay reaction N(t) = N0 e^-lambda*t, expect 3.6788 mmol remaining 
		double[] rxnMedia = {10.0};
		int[] exRxnEnzymes = {-1};
		double[] exRxnRateConstants = {0.1};
		double[][] exRxnStoich = {{-1}};
		double[][] exRxnParams = {{1}};
		double timestep_seconds = 10;

		comets.getParameters().setTimeStep(timestep_seconds / 3600);
		reactionModel.setExRxnEnzymes(exRxnEnzymes);
		reactionModel.setExRxnParams(exRxnParams);
		reactionModel.setExRxnRateConstants(exRxnRateConstants);
		reactionModel.setExRxnStoich(exRxnStoich);
		reactionModel.setMediaNames(new String[] {"MetA"});
		reactionModel.saveState();
		reactionModel.reset();

		int[] iterations = {100, 500, 1000, 5000, 10000, 50000, 100000, 500000};
		String[] iterstrings = {"   100", "   500", "  1000", "  5000", " 10000", " 50000", "100000", "500000"};
		double[] res = new double[iterations.length];
		System.out.println("Execution of integrator with various stepsizes (expect 3.6788):");
		for (int i = 0; i < iterations.length; i++){
			((FBAParameters) comets.getPackageParameters()).setNumExRxnSubsteps(iterations[i]);
			double[] exec = reactionModel.executeODE(rxnMedia, timestep_seconds, iterations[i]);
			res[i] = exec[0];
			if (printAll) System.out.println(iterstrings[i] + ": " + String.valueOf(exec[0]));
			assertEquals(3.6788,res[i],0.001);
		}
	}
	
	@Test
	public void testExecuteODE_multiRxn() {
		int[] worldIdxs = reactionModel.getMediaIdxs();
		double timestep_seconds = comets.getParameters().getTimeStep() * 60 * 60;
		int maxIterations = ((FBAParameters) comets.getPackageParameters()).getNumExRxnSubsteps();
		double[] worldMedia = comets.getWorld().getMediaAt(0,0,0);
		double[] rxnMedia = new double[worldIdxs.length];
		for (int i = 0; i < worldIdxs.length; i++){
			rxnMedia[i] = worldMedia[worldIdxs[i]];
		}
		
		double[] res = reactionModel.executeODE(rxnMedia, timestep_seconds, maxIterations);
		//TODO: calculate what these results should be by hand, then check them
		int x = 1;
	}

		
	@Test
	public void testRunAsSingleStep() {
		//fail("Not yet implemented");
	}

	@Test
	public void testRunAsSubsteps() {
		//fail("Not yet implemented");
	}

}
