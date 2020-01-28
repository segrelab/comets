/**
 * 
 */
package edu.bu.segrelab.comets.test.classes;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import org.junit.rules.TemporaryFolder;

import edu.bu.segrelab.comets.Comets;
import edu.bu.segrelab.comets.fba.FBACometsLoader;
import edu.bu.segrelab.comets.fba.FBAModel;
import edu.bu.segrelab.comets.test.etc.TestKineticParameters;

/**A class to allow modifications to the Comets class in order to facilitate 
 * unit and integration tests.
 * It has the helpful properties that it doesn't need any arguments to be
 * instantiated, and it doesn't try to run automatically.
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
	public String createScriptForLayout(String layoutFileName) throws IOException {
		URL scriptFolderURL = TestKineticParameters.class.getResource("../resources/");
		String folderPath = scriptFolderURL.getPath();
		String scriptPath = folderPath + "comets_script_temp.txt";
		String layoutPath = folderPath + layoutFileName;
		FileWriter fw = new FileWriter(new File(scriptPath), false);
		fw.write("load_layout " + layoutPath);
		fw.close();
		scriptFileName = scriptPath;
		return scriptFileName;
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
	
	public void setScriptFileName(String s) {
		this.scriptFileName = s;
	}
	
	/**Load the script, building the world and its constituent models**/
	public void loadScript() throws IOException {loadScript(this.scriptFileName);}
	public void loadScript(String filename) throws IOException {
		System.out.println("running script file: " + filename);
		BufferedReader reader = new BufferedReader(new FileReader(new File(filename)));
		String line;
		cParams.setCommandLineOnly(true);
		cParams.showGraphics(false);
		
		Set<String[]> parameterSet = new HashSet<String[]>();
		String batchListFile = "";
		while((line = reader.readLine()) != null)
		{
			line = line.trim();
			String[] parsed = line.split("\\s+");
			//edit MQ 5/17/2017: replace using parsed[1] with targetFile to allow
			//cases where target file path includes spaces
			String command = parsed[0];
			String targetFile = line.substring(line.indexOf(parsed[1]));
		if (command.equalsIgnoreCase("load_comets_parameters") || line.startsWith("load_package_parameters"))
		{
			loadParametersFile(targetFile);
			cParams.setCommandLineOnly(true);
			cParams.showGraphics(false);
		}
		else if (command.equalsIgnoreCase("load_layout"))
		{
			// load a layout file
			loadLayoutFile(targetFile);
		}
		else if (command.equalsIgnoreCase("batch_list_file"))
			batchListFile = targetFile;
		else
		        parameterSet.add(parsed);
	}
	reader.close();
	}
}
