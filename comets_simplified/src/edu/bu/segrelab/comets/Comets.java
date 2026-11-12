/**
 * The entry class for running the COMETS program. Instantiating this class
 * runs the program from the command line.
 * <p>
 * The main program flow idea is this:<br>
 * A World2D is the uniform grid in which the simulation takes place. Each point 
 * on the grid contains a quantity of media (as defined by the union of the loaded
 * Models) and up to one Cell.
 * <p>
 * Cells are intuited as collections of biomass from various species. Each species
 * known by a cell acts according to its Model.
 * <p>
 * Models are constructs that decide the behavior of various species in the system.
 * <p>
 * World2D, Cell, and Model are all abstract classes, and are expected to have their
 * behavior defined by the Package that is loaded. The Package also defines its own
 * specific parameters for how it functions.
 * <p>
 * //TODO add a couple sentences about the usual program flow. Starts as
 * SETUP_MODE --> SIMULATION_MODE, etc.
 * @author Bill Riehl
 */
package edu.bu.segrelab.comets;



import edu.bu.segrelab.comets.event.CometsChangeEvent;
import edu.bu.segrelab.comets.event.CometsChangeListener;
import edu.bu.segrelab.comets.event.CometsLoadEvent;
import edu.bu.segrelab.comets.event.CometsLoadListener;
import edu.bu.segrelab.comets.event.SimulationStateChangeEvent;
import edu.bu.segrelab.comets.event.SimulationStateChangeListener;
import edu.bu.segrelab.comets.exception.CometsArgumentException;
import edu.bu.segrelab.comets.exception.ParameterFileException;
import edu.bu.segrelab.comets.util.Utility;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;

public class Comets implements CometsConstants,
							   //CometsLoadListener,
							   CometsChangeListener
{



	private String versionString = "2.12.10 12 November 2026";



	
	/**
	 * A debugging tool. If this is set to true, then the only running done
	 * by each cell just runs through a diffusion routine.
	 */
	public static boolean DIFFUSION_TEST_MODE = false;

	//More debug/test features
	public static boolean AUTORUN = true; //Have the constructor run the script it's given?
	public static boolean EXIT_AFTER_SCRIPT = true; //Disable to allow debugging & tests
	public static boolean DEBUG_COMMAND_LINE_MODE = false;
	
	// The setup pane 
	private CometsSimRunner runner;
	
	protected CometsParameters cParams;   // the global parameter set
	protected PackageParameters pParams;  // parameters specific to the package
	
	protected Model[] initModels, models;
	protected World2D initWorld;


	protected World2D world;
	protected World3D initWorld3D, world3D;
	protected List<Cell> initCellList, cellList;

	private int mode;
	protected CometsLoader loader = null;
	private String loaderClassName;
	
	
	
	
	
	protected String scriptFileName = null;
	
	private List<CometsChangeListener> cometsChangeListeners;
	private List<CometsLoadListener> cometsLoadListeners;
	private List<SimulationStateChangeListener> simStateChangeListeners;
	
	/**
	 * Creates a new <code>Comets</code> object. If a script is given, it is
	 * run automatically and the program exits.
	 * 
	 * @param args The array of arguments used when starting the program. This list 
	 * can be of many formats, but must always contain at least one string - the name of
	 * the CometsLoader extension class for loading a COMETS package.
	 * <p>
	 * If there is only one argument (e.g. one member of <code>args</code>), then it
	 * is expected to be the package loader class name.
	 * <p>
	 * If there are multiple arguments, there must be a multiple of two, set up as:
	 * <br>
	 * &nbsp;&nbsp;&nbsp;&nbsp;-arg "value"
	 * <br>
	 * with quotes required.
	 * <p>
	 * Valid arguments follow:
	 * <ul>
	 * <li><code>-loader "class loader"</code>: the COMETS package loader class name.
	 * For example, the FBA loader is "<code>edu.bu.segrelab.comets.fba.FBACometsLoader</code>"
	 * <li><code>-script "script"</code>: runs COMETS in automated mode, 
	 * with all parameters and variables set up with a script. See documentation for
	 * format details.
	 * <li><code>-params "param file"</code>: starts COMETS with a given set of parameters
	 * loaded from a separate parameters file. See documentation for format.
	 * <li><code>-pkgparams "param file"</code>: as above, but loads a set of package-specific
	 * parameters. See specific package documentation for format details.
	 * </ul>
	 * Invalid arguments are ignored.
	 */
	public Comets(String[] args)
	{
		/* Handle program arguments.
		 * Cases:
		 * 1. Zero arguments -> ERROR'D. Need at least one.
		 * 2. One argument -> assume it's the CometsLoader subclass name.
		 * 3. More than one argument -> uses -arg "blah" -arg2 "blah"
		 *    a. length of args array is not a multiple of 2 -> fail. 
		 *    b. one of those is not -loader -> fail.
		 */
		

		models = new Model[0];

		mode = SETUP_MODE;
		cParams = new CometsParameters();
		
		
		
		
		cometsChangeListeners = new ArrayList<CometsChangeListener>();
		cometsLoadListeners = new ArrayList<CometsLoadListener>();
		simStateChangeListeners = new ArrayList<SimulationStateChangeListener>();
		
		try
		{
			Map<String, String> argsMap = parseArgs(args);
			applyArgs(argsMap);
		}
		catch(Exception e)
		{
			// fail with a good explanation
			System.out.println(e);
			System.exit(1);
		}
		
		
		// if we have a script file, load and run it, then exit.
		if (scriptFileName != null || DEBUG_COMMAND_LINE_MODE)
		{
			if (AUTORUN){
				runScript(scriptFileName);
			}
			if (EXIT_AFTER_SCRIPT){
				exitProgram();
			}
		}

	}

	public void addCometsChangeListener(CometsChangeListener listener)
	{
		cometsChangeListeners.add(listener);
	}

	public void addCometsLoadListener(CometsLoadListener listener)
	{
		cometsLoadListeners.add(listener);
	}
	
	public void addSimulationStateChangeListener(SimulationStateChangeListener listener)
	{
		simStateChangeListeners.add(listener);
	}

	private void fireLoadEvent(CometsLoadEvent e)
	{
		for (CometsLoadListener l : cometsLoadListeners)
		{
			l.cometsLoadPerformed(e);
		}
	}
	
	@SuppressWarnings("unused")
	private void fireChangeEvent(CometsChangeEvent e)
	{
		for (CometsChangeListener l : cometsChangeListeners)
		{
			l.cometsChangePerformed(e);
		}
	}
	
	public void fireSimulationStateChangeEvent(SimulationStateChangeEvent e)
	{
		for (SimulationStateChangeListener l : simStateChangeListeners)
		{
			l.onStateChange(e);
		}
	}
	
	private void applyArgs(Map<String, String> argsMap) throws CometsArgumentException, 
										  					   IOException
	{
		
		// First, make sure there's a loader
		if (!argsMap.containsKey("-loader"))
		{
			throw new CometsArgumentException("Missing a -loader argument!");
		}

		this.loaderClassName = argsMap.get("-loader");
		initCometsPackage(loaderClassName);
		argsMap.remove("-loader");  		// don't want to do this again - just once!

		for (String arg : argsMap.keySet())
		{
			System.out.println(arg);
			// interpret argument --here--
			if (arg.equalsIgnoreCase("-script"))
			{
				scriptFileName = argsMap.get(arg);
			}
			else if (arg.equalsIgnoreCase("-params") || arg.equalsIgnoreCase("-pkgparams"))
			{
				loadParametersFile(argsMap.get(arg));
			}
			else
			{
				throw new CometsArgumentException("Unknown argument: " + arg);
			}
		}
	}
	

	/**
	 * Exits the program when a command line error occurs, and displays
	 * the program usage.
	 */
	private void exitCommandLineError()
	{
		System.out.println("Comets usage:\n" +
				           "java <jvm arguments> comets \"comets package loader class\"\n" + 
				           "or\n" +
				           "java <jvm arguments> comets <comets arguments>\n" + 
				           "argument list:\n" +
				           "-loader \"class loader\" -- the path to a package class loader\n" +
				           "        (MUST BE THE FIRST ARGUMENT)\n" +
				           "-script \"script file name\" -- the path to an automated comets script file\n" +
				           "-params \"comets parameters file name\" -- the path to a comets parameters file\n" +
				           "-pkgparams \"comets package parameters file name\" -- the path to a comets\n" +
				           "           package parameters file, format unique to each package");
		System.exit(1);
	}
	
	/**
	 * Parses the list of arguments passed to the program in command line. 
	 * Expects to see arguments passed as "-arg1 value1 -arg2 value2 ..."
	 * @param args
	 * @return
	 */
	private Map<String, String> parseArgs(String[] args) throws CometsArgumentException
	{
		/**
		 * checks the arguments for common problems and, upon finding none,
		 * puts the arguments into a map to be read by applyArgs
		 */
		Map<String, String> argsMap = new HashMap<String, String>();

		
		// check if there are zero arguments and if so, recommend help and exit.
		if (args.length == 0) 
		{
			throw new CometsArgumentException("COMETS requires at least one parameter. See -h or -help for more information.");
		}
		
		// if there is just 1 argument, it should be -v or -h
		else if (args.length == 1)
		{
			if (args[0].equalsIgnoreCase("-v") ||
					args[0].equalsIgnoreCase("-version"))
			{
				showVersion();
				System.exit(0);
			}
			else if (args[0].equalsIgnoreCase("-h") ||
					args[0].equalsIgnoreCase("-help"))
			{
				showHelp();
				System.exit(0);
			}
			else
			{
				throw new CometsArgumentException("If only one argument is given to COMETS, it must be -version or -help. You gave the argument " +
			                                      args[0] +
			                                      ".\nsee -help for more information.");
			}
		}
		else if (args.length % 2 != 0)
		{
			throw new CometsArgumentException("COMETS requires an even number of arguments to run a simulation.\nsee -help for more information");
		}
		else if ((args.length == 4) &&
				(args[3].equalsIgnoreCase("-h") ||
				args[3].equalsIgnoreCase("-help")))
		// deal with the weird case where they use comets_scr and then put
		// in -h rather than a script:
		{
			showHelp();
			System.exit(0);
		}

		for (int i=0; i<=args.length-2; i+=2)
		{
			if (!args[i].startsWith("-"))
				return null;
			else
				argsMap.put(args[i], args[i+1]);
		}
		return argsMap;
	}
	
	/**
	 * Called when -h or -help are used as command line arguments
	 */
	private void showHelp()
	{
		System.out.println("comets help:\n" +
						   "COMETS is a program for dynamic and spatial flux-balance analysis modeling.\n" +
						   "for complete instructions, including how to use a python or matlab toolbox, go to https://www.runComets.org\n\n" +
						   "running COMETS without a python or matlab toolbox:\n" +
						   "\tNOTE: if you are running COMETS using comets_scr which was installed by the installer, see the note at the bottom.\n" +
				           "\tCOMETS runs on the command line, by specifying a -loader argument:\n" +
						   "\n\tTo run, use:\n"+
						   "\t\tjava <jvm arguments> edu.bu.segrelab.comets.Comets -loader edu.bu.segrelab.comets.fba.FBACometsLoader -script <scriptname>\n"+
						   "\n" +
						   "\t<jvm arguments> is usually used to set the java -classpath and -Djava.library.path, for example to add the path to the comets.jar. If you receive classdefnotfound errors, this likely needs to be modified.\n" +
						   "\t<scriptname> is the name of a text file containing the location of the layout and parameters file(s). For example, if <scriptname> = simulation_data.txt, then the inside of simulation_data.txt could look like\n" +
						   "\n\t\tload_layout current_layout.txt" +
						   "\n\t\tload_comets_parameters global_params.txt" +
						   "\n\t\tload_package_parameters package_params.txt\n" +
						   "\n\t\tin this example, current_layout.txt is a COMETS layout file, and the other files are COMETS global and package parameters files.\n" +
						   "\nto see the version use:\n" +
						   "\tjava <jvm arguments> edu.bu.segrelab.comets.Comets -version\n" +
						   "\nto see this help use:\n" +
						   "\tjava <jvm arguments> edu.bu.segrelab.comets.Comets -help\n" +
						   "\ncomets_scr: comets_scr simplifies running scripts by removing the need to specify the java classpath or COMETS class information. If you are running COMETS using comets_scr, you must give the filename of the COMETS script as the single required argument.\n" +
						   "\te.g. comets_scr simulation_data.txt\n" +
						   "\tsee <scriptname> under 'running COMETS' above for more details\n" +
						   "\tto show this help, run: comets_scr -help"
				);
	}
	
	private void showVersion()
	{
		System.out.print(versionString);
	}
	
	public void setParameters(CometsParameters cParams)
	{
		this.cParams = cParams;
	}
	
	public void setPackageParameters(PackageParameters pParams)
	{
		if (getPackageParameters().getClass().equals(pParams.getClass()))
			this.pParams = pParams;
	}
	
	/**
	 * Parses and runs a COMETS script.
	 * @param filename
	 */
    protected void runScript(String filename)
    {
		System.out.println("running script file: " + filename);
		System.out.println("Current Java version: " + System.getProperty("java.version"));
		try
		{
			BufferedReader reader = new BufferedReader(new FileReader(new File(filename)));
			String line;
			
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
				System.out.println("MAX CYCLES = " + cParams.getMaxCycles());
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
		
		// everything's loaded... supposedly. make sure, then run.
		if (world == null && world3D == null)
			throw new IOException("No world loaded - halting execution. You might want to check your layout file.");
		else if (models == null || models.length == 0)
			throw new IOException("No models loaded - halting execution. You might want to check the location of the model files or the model files themselves.");
		else if (cellList == null || cellList.size() == 0)
			throw new IOException("No cells on the world - halting execution. You might want to make sure cells are initialized in your layout file");
		else if (cParams.getMaxCycles() == UNLIMITED_CYCLES)
			throw new IOException("Unlimited cycle time selected - halting execution. Please set the maximum number of cycles in your parameters file to something reasonable. For example, something less than infinity.");
		
		ParameterBatchSet batchSet = new ParameterBatchSet(this, parameterSet);
		if (batchSet.size() > 0)
		{
			if (batchListFile.length() == 0)
				throw new IOException("A batch of trials requires a batch_list_file parameter to store the parameter set");
				batchSet.saveList(batchListFile);
				for (int i=0; i<batchSet.size(); i++)
				{
					batchSet.applyParameterSet(i);
					doCommandLineRun();
					batchSet.resetParameters();
				}
			}
			
			else
				doCommandLineRun();
		}
		catch (IOException e)
		{
			System.out.println("Error running script file: " + e.getMessage() + "\n -- halting execution");
		}
		catch (CometsArgumentException e)
		{
			System.out.println("Error in loading parameter batch: " + e.getMessage() + "\n -- halting execution");
		}
    }

	protected void doCommandLineRun()
	{
		cParams.pause(false);
		cParams.pauseOnStep(false);

		runner = new CometsSimRunner(this);
		runner.start();
		
		// The runner spins as a separate thread, so check it once a second.
		while(runner.isAlive())
		{
			try
			{
				Thread.sleep(1000);
			}
			catch(InterruptedException e)
			{
				
			}
		}
	}
	
	/**
	 * Returns the names of all loaded models.
	 * 
	 * @return a String array of model names.
	 */

	public String[] getModelNames()
	{
		String[] modelNames = new String[models.length];
		for (int i=0; i<models.length; i++)
		{
			modelNames[i] = models[i].getModelName();
		}
		return modelNames;
	}

	/**
	 * Returns the names of all media components in the current world.
	 * 
	 * @return a String array of nutrient names
	 */

	public String[] getMediaNames()
	{
		return world.getMediaNames();
	}

	/**
	 * Returns the set of cells being used in the program.
	 * 
	 * @return an <code>ArrayList</code> of <code>Cells</code>
	 */
	public List<Cell> getCells()
	{
		return cellList;
	}
	
	/**
	 * Returns the World2D being used in the program.<br>
	 * Note that this will likely be a subclass (<code>FBAWorld</code>, etc),
	 * but the abstract type is used here.
	 *
	 * @return a World2D or <code>null</code> if it hasn't been instantiated.
	 */
	public World2D getWorld()
	{
		return world;
	}
	/**
	 * Returns the World3D being used in the program.<br>
	 * Note that this will likely be a subclass (<code>FBAWorld</code>, etc),
	 * but the abstract type is used here.
	 *
	 * @return a World2D or <code>null</code> if it hasn't been instantiated.
	 */
	public World3D getWorld3D()
	{
		return world3D;
	}
	/**
	 * Returns the array of loaded Models.<br>
	 * These will likely be subclasses, but the abstract type is used here.
	 * @return an array of Models
	 */
	public Model[] getModels()
	{
		return models;
	}
	
	/**
	 * Returns the current mode that the program is in, either
	 * SIMULATION_MODE or SETUP_MODE.
	 * @return
	 */
	public int getMode()
	{
		return mode;
	}
	
	/**
	 * Returns the <code>CometsParameters</code> object generated and
	 * used by the program. It is intended to be the only such instance
	 * of the object used throughout the program's runtime.
	 *  
	 * @return	a CometsParameters
	 */
	public CometsParameters getParameters()
	{
		return cParams;
	}
	
	/**
	 * Sets the state of the program into simulation mode and starts the
	 * simulation runner.
	 */

	public void startSimulation()
	{
		if (models == null || models.length == 0)
		{
			return;
		}
		else if (cellList.size() == 0)
		{
			return;
		}
		else if (world == null && world3D == null)
		{
			return;
		}

		// from built in data (FBAWorld and such), spawn a runtime object. and make it go!
		mode = SIMULATION_MODE;

		runner = new CometsSimRunner(this);
		runner.start();
	}

	/**
	 * Stops the running simulation and sets the program back in setup mode.
	 */

	public void endSimulation()
	{
		if (runner != null && !runner.isFinished())
		{
			runner.finish();

			try
			{
				runner.join();
			}
			catch(InterruptedException e) {	}
		}
		mode = SETUP_MODE;
		runner = null;
		cParams.pause(true);
	}

	/**
	 * Loads a layout file. This invokes the loader of the currently loaded package, which
	 * does the heavy lifting of creating and populating an initial World2D, List of Cells, 
	 * and array of Models.
	 * @param filename
	 */
	public void loadLayoutFile(String filename)
	{
		if (loader == null)
		{
			initCometsPackage(loaderClassName);
		}
		try
		{
			// Load the layout file.
			loader.loadLayoutFile(filename, this);
			if (world != null)
			{
				world.destroy();
				initWorld.destroy();
				world = null;
				initWorld = null;
			}
			if (world3D != null)
			{
				world3D.destroy();
				initWorld3D.destroy();
				world3D = null;
				initWorld3D = null;
			}
			
			if (cParams.getNumLayers()==1)
			{
				world = loader.getWorld();
			}
			else if(cParams.getNumLayers()>1)
			{
				world3D = loader.getWorld3D();
			}
			models = loader.getModels();
			cellList = loader.getCells();
			if (cParams.getNumLayers()==1)
				initWorld = world.backup();
			else if(cParams.getNumLayers()>1)
				initWorld3D = world3D.backup();
			initCellList = new ArrayList<Cell>();
			for (Cell cell : cellList)
			{
				if (cParams.getNumLayers()==1)
					initCellList.add(cell.backup(initWorld));
				else if(cParams.getNumLayers()>1)
					initCellList.add(cell.backup(initWorld3D));
			}
		}
		catch(IOException e)
		{
			System.out.println(e);
		}
	}
	
	/**
	 * If there's been any changes, then prompt the user to really exit
	 * without saving, then do general cleanup and exit.
	 */
	public void exitProgram()
	{
		//TODO prompt to save changes
		System.exit(0);
	}

	/**
	 * Returns true if there is at least one model loaded.
	 * @return
	 */
	public boolean hasModelsLoaded()
	{
		return (models.length > 0);
	}
	
	/**
	 * Returns the set of parameters from the currently loaded Package.
	 * @return
	 */
	public PackageParameters getPackageParameters()
	{
		return pParams;
	}

	/**
	 * Initializes the Package to be used, and loads an initial set of parameters.
	 * @param packageName
	 */
	private void initCometsPackage(String packageName)
	{
		try
		{
			ClassLoader myClassLoader = this.getClass().getClassLoader(); //ClassLoader.getSystemClassLoader();
			Class<?> cometsLoaderClass = myClassLoader.loadClass(packageName);
			loader = (CometsLoader)cometsLoaderClass.newInstance();
		}
		catch (Exception e)
		{
			System.err.println(e + "\nloader class not found. " +
		"did you mean to use the argument -loader edu.bu.segrelab.comets.fba.FBACometsLoader ?");
			e.printStackTrace();
		}
		
		// load up the parameters, too.
		pParams = loader.getPackageParameters(this);
	}
	
	@Override
	public void cometsChangePerformed(CometsChangeEvent e)
	{
		// TODO Auto-generated method stub
		
	}
	public void loadParametersFile(String filename)
	{
		try
		{
			ParametersFileHandler.loadParametersFile(filename, cParams, pParams);
			fireLoadEvent(new CometsLoadEvent(this, CometsLoadEvent.Type.PARAMETERS));
		}
		catch (ParameterFileException e)
		{
			System.out.println("Parsing error in parameter file:\n" + e.getMessage() + " on line " + e.getLine());
		}
		catch (IOException e)
		{
			System.out.println("Error occurred while loading parameter file:\n" + e.getMessage());
		}
		
	}
	/**
	 * The standard main function.
	 * @param args	command line arguments, see {@link #Comets(String[])}
	 */
	public static void main(String[] args)
	{
		new Comets(args);
	}


}
