package edu.bu.segrelab.comets.test.integration;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;

import javax.swing.JFileChooser;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import edu.bu.segrelab.comets.test.classes.TComets;
import edu.bu.segrelab.comets.test.etc.TestKineticParameters;

/**A class to include complete simulation runs in order to check their results
 * 
 * @author mquintin
 *
 */
public class IntTestRunningLayouts {

	TComets comets;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
		comets = new TComets();
	}

	@After
	public void tearDown() throws Exception {
	}
	
	/*//testing Eclipse file IO
	@Test
	public void testFileWriter() throws IOException{
		URL scriptFolderURL = TestKineticParameters.class.getResource("../resources/");
		String folderPath = scriptFolderURL.getPath();
		//String scriptPath = folderPath + File.separator + "comets_script_temp.txt";
		//String layoutPath = folderPath + File.separator + layoutFileName;
		String scriptPath = folderPath + "testFileWriter.txt";
		String layoutPath = folderPath + "comets_layout_unnecessary_cellulase.txt";
		//scriptPath = "C:/Users/mquintin/userworkspace/comets/src/edu/bu/segrelab/comets/test/resources";
		////Don't use absolute paths!
		FileWriter fw = new FileWriter(new File(scriptPath), false);
		fw.write("load_layout " + layoutPath);
		fw.close();
		
		FileReader fr = new FileReader(new File(scriptPath));
		int readresult = fr.read();
		System.out.println(readresult);
		fr.close();
	} */
	
	/*//testing Eclipse file IO
	@Test
	public void testPrintWriter() throws IOException{
		URL scriptFolderURL = TestKineticParameters.class.getResource("../resources/");
		String folderPath = scriptFolderURL.getPath();
		//String scriptPath = folderPath + File.separator + "comets_script_temp.txt";
		//String layoutPath = folderPath + File.separator + layoutFileName;
		String scriptPath = folderPath + "testPrintWriter.txt";
		String layoutPath = folderPath + "comets_layout_unnecessary_cellulase.txt";
		PrintWriter fw = new PrintWriter(new File(scriptPath), "UTF-8");
		fw.write("load_layout " + layoutPath);
		fw.close();
		
	}*/
	
	/*Test a layout with extracellular enzymatic and decay reactions, and all media components
	 * set to excessive levels.
	 * This test created c.a. 1/21/2020 version 2.7.2 because the model for 
	 * testGrowthWithUnnecessaryExtracellularCellulase was being called infeasible, but older 
	 * versions of Comets were able to get growth from the same files.
	 * If the other test was set up with a skipped essential metabolite, this should work.
	 * 
	 * uses the files yeastGEMxml.txt, comets_layout_excessall_withrxns.txt
	 */
	@Test
	public void testGrowthWithExcessMediaAndExtracellularReactions() throws IOException {
		String layoutFilePath = "comets_layout_excessall_withrxns.txt";
		String scriptFilePath = comets.createScriptForLayout(layoutFilePath);
		comets.loadScript(scriptFilePath);
		double initBiomass = comets.getWorld().getBiomassAt(0, 0)[0];
		comets.run();
		double finalBiomass = comets.getWorld().getBiomassAt(0, 0)[0];
		assert(finalBiomass > initBiomass);
	}


	/*Test a layout with extracellular enzymatic and enzyme decay reactions. 
	 * Cellulose in the media is converted to glc__D_e by enzyme_e
	 * Enzyme_e also decays over time
	 * This layout includes glucose, so the cellulase is not actually necessary for growth. This test
	 * simply confirms that running the extracellular reaction engine doesn't break feasibility of the
	 * model or the media contents of the FBA cell
	 * 
	 * uses the files yeastGEMxml.txt, comets_layout_unnecessary_cellulase.txt
	 * 
	 * Putting this out of commission for now, it appears the model is just not being given the right 
	 * metabolites
	 */
	@Test
	public void testGrowthWithUnnecessaryExtracellularCellulase() throws IOException {
		String layoutFilePath = "comets_layout_unnecessary_cellulase.txt";
		String scriptFilePath = comets.createScriptForLayout(layoutFilePath);
		comets.loadScript(scriptFilePath);
		double initBiomass = comets.getWorld().getBiomassAt(0, 0)[0];
		comets.run();
		double finalBiomass = comets.getWorld().getBiomassAt(0, 0)[0];
		assert(finalBiomass > initBiomass);
	}

}
