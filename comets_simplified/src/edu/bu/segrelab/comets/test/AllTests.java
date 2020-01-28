package edu.bu.segrelab.comets.test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import edu.bu.segrelab.comets.test.integration.IntTestExternalReactions;
import edu.bu.segrelab.comets.test.integration.IntTestFBAModelOptimization;
import edu.bu.segrelab.comets.test.integration.IntTestRunningLayouts;
import edu.bu.segrelab.comets.test.integration.TestGurobi;
import edu.bu.segrelab.comets.test.integration.TestGurobiObjectiveFunctions;
import edu.bu.segrelab.comets.test.integration.TestParametersIntegration;
import edu.bu.segrelab.comets.test.unit.fba.TestBiomassMotionStyle;
import edu.bu.segrelab.comets.test.unit.fba.TestFBACometsLoader;
import edu.bu.segrelab.comets.test.unit.fba.TestFBAOptimizerGurobi;
import edu.bu.segrelab.comets.test.unit.fba.TestFBAParameters;
import edu.bu.segrelab.comets.test.unit.fba.TestFBACell;
import edu.bu.segrelab.comets.test.unit.reaction.TestExternalReactionCalculator;
import edu.bu.segrelab.comets.test.unit.reaction.TestReactionModel;
import edu.bu.segrelab.comets.test.unit.reaction.TestReactionODE;

@RunWith(Suite.class)
@SuiteClasses({
	//Unit
	//FBA
	TestBiomassMotionStyle.class, 
	TestFBACometsLoader.class, 
	TestFBAOptimizerGurobi.class, 
	TestFBAParameters.class,
	TestFBACell.class,
	//Extracellular Reactions
	TestExternalReactionCalculator.class,
	TestReactionModel.class,
	TestReactionODE.class,
	
	//Integration
	IntTestExternalReactions.class,
	IntTestFBAModelOptimization.class,
	IntTestRunningLayouts.class,
	TestGurobi.class,
	TestGurobiObjectiveFunctions.class,
	TestParametersIntegration.class
	})
public class AllTests {

}
