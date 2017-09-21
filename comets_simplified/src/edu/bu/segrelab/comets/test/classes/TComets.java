/**
 * 
 */
package edu.bu.segrelab.comets.test.classes;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;

import org.junit.rules.TemporaryFolder;

import edu.bu.segrelab.comets.Comets;
import edu.bu.segrelab.comets.Model;
import edu.bu.segrelab.comets.fba.FBACometsLoader;
import edu.bu.segrelab.comets.fba.FBAModel;
import edu.bu.segrelab.comets.fba.FBAWorld;
import edu.bu.segrelab.comets.test.etc.TestKineticParameters;

/**A class to allow modifications to the Comets class in order to facilitate 
 * unit and integration tests
 * @author mquintin
 *
 */
public class TComets extends Comets {

	//override static values in the parent class
	static {
	AUTORUN = false;
	EXIT_AFTER_SCRIPT = false;
	DEBUG_COMMAND_LINE_MODE = true; //run in CL mode. Don't launch the GUI
	}

	public static String DEFAULT_TCOMETS_LOADER = FBACometsLoader.class.getName();
	
	/**
	 * @param args
	 */
	public TComets(String[] args) {
		super(args);
	}

	public TComets() {
		this(new String[]{"-loader",DEFAULT_TCOMETS_LOADER});
	}

	/**Creates a script file pointing to the given layout file, and prepares
	 * internal variables to be ready to run the script
	 * 
	 * The layoutFileName must be the relative path to the file starting from
	 * the test/resources folder
	 * 
	 * @param layoutFileName 
	 * @throws IOException
	 */
	public void createScriptForLayout(String layoutFileName) throws IOException {
		URL scriptFolderURL = TestKineticParameters.class.getResource("../resources/");
		String folderPath = scriptFolderURL.getPath();
		String scriptPath = folderPath + File.separator + "comets_script_temp.txt";
		String layoutPath = folderPath + File.separator + layoutFileName;
		FileWriter fw = new FileWriter(new File(scriptPath), false);
		fw.write("load_layout " + layoutPath);
		fw.close();
		scriptFileName = scriptPath;
	}
	
	public void run() {runScript(scriptFileName);}
	
	/**run the optimizer on the given model file without putting it into a layout context
	 * @param filePath
	 */
	public FBAModel runModelFile(String filePath) {
		loader = new FBACometsLoader();
		FBAModel model = (FBAModel) loader.loadModelFromFile(this, filePath);
		int status = model.run();
		if (status != 180 && status != 5) {
			System.out.println("Problem running " + filePath);
			System.out.println("Status of model.run() indicated error or failure");
		}
		return model;
	}
	
	/**Run the optimizer on the given String, which must be valid contents for a model file,
	 * without putting it into a layout context
	 * 
	 * @param modelFileContents
	 * @param tempFolder
	 * @return
	 * @throws IOException 
	 */
	public FBAModel runModelString(String modelFileContents, TemporaryFolder tempFolder) throws IOException {
		File modelFile = tempFolder.newFile();
		String modelFilePath = modelFile.getAbsolutePath();
		
		FileWriter fw = new FileWriter(modelFilePath);
		fw.write(modelFileContents);
		fw.close();
		
		FBAModel model = runModelFile(modelFilePath);
		return model;
	}
}
