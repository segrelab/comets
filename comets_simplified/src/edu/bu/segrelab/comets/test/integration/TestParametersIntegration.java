/**
 * 
 */
package edu.bu.segrelab.comets.test.integration;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.StringReader;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import edu.bu.segrelab.comets.CometsParameters;
import edu.bu.segrelab.comets.PackageParameters;
import edu.bu.segrelab.comets.ParametersFileHandler;
import edu.bu.segrelab.comets.exception.ParameterFileException;

/**A class to test interactions with {@link CometsParameters} and {@link FBAParameters}.
 * Note that universal considerations should always refer to CometsParameters, and any feature
 * which deals with the optimization algorithm itself should refer to a class which implements
 * the {@link PackageParameters} interface, such as FBAParameters
 * 
 * This class is being built up gradually. Currently tested features are:
 * 	Setting {@link CometsParameters#randomOrder randomOrder} from {@link ParametersFileHandler#parseParameterLine}
 * 
 * @author mquintin
 * Last update 12/19/2016
 */
public class TestParametersIntegration {
	
	private CometsParameters cParams;
	private static File paramsFile;

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		//write the parametersFileBlock to a temporary location
		paramsFile = File.createTempFile("tempParams", ".tmp");
		paramsFile.deleteOnExit();
		BufferedWriter bw = new BufferedWriter(new FileWriter(paramsFile));
		bw.write(parametersFileBlock);
		bw.close();
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
		//initialize the CometsParameters object
		cParams = new CometsParameters();
		
		//load the parametersFileBlock
		ParametersFileHandler.loadParametersFile(paramsFile.getAbsolutePath(), cParams, (PackageParameters) cParams);
		
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
	}

	/**
	 * Check the default values
	 */
	@Test
	public void testDefaultCometsParameters() {
		//default to randomizing the order in which the FBA cell runs each model
		
	}

	
	private static String parametersFileBlock = "	maxCycles = 200\r\n" + 
			"	pixelScale = 5\r\n" + 
			"	saveslideshow = false\r\n" + 
			"	slideshowExt = png\r\n" + 
			"	slideshowColorRelative = true\r\n" + 
			"	slideshowRate = 1\r\n" + 
			"	slideshowLayer = 324\r\n" + 
			"	writeFluxLog = false\r\n" + 
			"	FluxLogName = ./flux.txt\r\n" + 
			"	FluxLogRate = 1\r\n" + 
			"	writeMediaLog = false\r\n" + 
			"	MediaLogName = ./media.txt\r\n" + 
			"	MediaLogRate = 1\r\n" + 
			"	writeBiomassLog = false\r\n" + 
			"	BiomassLogName = ./biomass.txt\r\n" + 
			"	BiomassLogRate = 1\r\n" + 
			"	writeTotalBiomassLog = false\r\n" + 
			"	totalBiomassLogRate = 1\r\n" + 
			"	TotalbiomassLogName = ./total_biomass\r\n" + 
			"	useLogNameTimeStamp = false\r\n" + 
			"	slideshowName = ./res.png\r\n" + 
			"	numDiffPerStep = 10\r\n" + 
			"	numRunThreads = 1\r\n" + 
			"	growthDiffRate = 0\r\n" + 
			"	flowDiffRate = 3e-09\r\n" + 
			"	exchangestyle = Monod Style\r\n" + 
			"	defaultKm = 0.01\r\n" + 
			"	defaultHill = 1\r\n" + 
			"	timeStep = 0.01\r\n" + 
			"	deathRate = 0\r\n" + 
			"	spaceWidth = 0.01\r\n" + 
			"	randomOrder = false\r\n" +
			"	maxSpaceBiomass = 0.00022\r\n" + 
			"	minSpaceBiomass = 2.5e-11\r\n" + 
			"	allowCellOverlap = true\r\n" + 
			"	toroidalWorld = false\r\n" + 
			"	showCycleTime = true\r\n" + 
			"	showCycleCount = true\r\n" + 
			"	defaultVmax = 10\r\n" + 
			"	biomassMotionStyle = Diffusion 2D(Crank-Nicolson)\r\n" + 
			"	exchangeStyle = Standard FBA\r\n";
}
