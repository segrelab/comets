/**
 * The entry class for running the COMETS program. Instantiating this class
 * will either build a JFrame with all the necessary components for running the
 * program, or initiate the program from command line.
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

import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import edu.bu.segrelab.comets.event.CometsChangeEvent;
import edu.bu.segrelab.comets.event.CometsChangeListener;
import edu.bu.segrelab.comets.event.CometsLoadEvent;
import edu.bu.segrelab.comets.event.CometsLoadListener;
import edu.bu.segrelab.comets.event.SimulationStateChangeEvent;
import edu.bu.segrelab.comets.event.SimulationStateChangeListener;
import edu.bu.segrelab.comets.exception.CometsArgumentException;
import edu.bu.segrelab.comets.exception.ParameterFileException;
import edu.bu.segrelab.comets.ui.CometsInfoFrame;
import edu.bu.segrelab.comets.ui.CometsMenuBar;
import edu.bu.segrelab.comets.ui.CometsParametersPanel;
import edu.bu.segrelab.comets.ui.CometsSetupPanel;
import edu.bu.segrelab.comets.ui.DoubleField;
import edu.bu.segrelab.comets.ui.IntField;
import edu.bu.segrelab.comets.ui.IntroDialog;
import edu.bu.segrelab.comets.ui.TextAreaOutputStream;
import edu.bu.segrelab.comets.util.Utility;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.CyclicBarrier;

public class Comets implements CometsConstants,
							   CometsLoadListener,
							   CometsChangeListener
{
	private String versionString = "2.5.9, 21 November 2017";
	
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
	
	private CometsParameters cParams;   // the global parameter set
	private PackageParameters pParams;  // parameters specific to the package
	
	private Model[] initModels, models;
	private World2D initWorld, world;
	private World3D initWorld3D, world3D;
	private List<Cell> initCellList, cellList;

	private int mode;
	protected CometsLoader loader = null;
	private String loaderClassName;
	
	private Deque<World2D> worldUndoDeque;
	private Stack<World2D> worldRedoStack;
	
	private Deque<List<Cell>> cellUndoDeque;
	private Stack<List<Cell>> cellRedoStack;
	
	private Deque<Model[]> modelUndoDeque;
	private Stack<Model[]> modelRedoStack;
	
	private CyclicBarrier runBarrier;
	
	protected String scriptFileName = null;
	
	// UI Widgets
	private JFrame 				cFrame;					// main Frame for the program
	private CometsSetupPanel 	setupPane;
	private CometsMenuBar 		cMenuBar;
	private JScrollPane 		cScrollPane;
	private JScrollPane			outputPane;
	private JTextArea 			outputArea;
	private CometsInfoFrame 	siFrame,
								miFrame;
	private CometsParametersPanel cParamsPanel;
	
	private List<CometsChangeListener> cometsChangeListeners;
	private List<CometsLoadListener> cometsLoadListeners;
	private List<SimulationStateChangeListener> simStateChangeListeners;
	
	/**
	 * Creates a new <code>Comets</code> object. This automatically starts
	 * the program by creating and populating a frame.
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
	 * <li><code>-script "script"</code>: starts COMETS in an automated, no GUI mode, 
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
		
		runBarrier = new CyclicBarrier(2);

		models = new Model[0];

		mode = SETUP_MODE;
		cParams = new CometsParameters();
		
		worldUndoDeque = new ArrayDeque<World2D>();
		worldRedoStack = new Stack<World2D>();
		
		cellUndoDeque = new ArrayDeque<List<Cell>>();
		cellRedoStack = new Stack<List<Cell>>();
		
		modelUndoDeque = new ArrayDeque<Model[]>();
		modelRedoStack = new Stack<Model[]>();
		
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
			exitCommandLineError();
		}
		
		
		// if we have a script file, load and run it, then exit.
		if (scriptFileName != null || DEBUG_COMMAND_LINE_MODE)
		{
			cParams.showGraphics(false);
			if (AUTORUN){
				runScript(scriptFileName);
			}
			if (EXIT_AFTER_SCRIPT){
				exitProgram();
			}
		}

		else 
		{
			initCometsUI();
			JPopupMenu.setDefaultLightWeightPopupEnabled(false);
			addCometsLoadListener(this);
			addCometsChangeListener(this);
			
			showIntroDialog();
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
	
	private void initCometsUI()
	{
		// init the controlling JFrame
		cFrame = new JFrame("COMETS");
		cFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		cFrame.setSize(800, 800);

		setupPane = new CometsSetupPanel(this);

		cScrollPane = new JScrollPane();
//		cScrollPane.setPreferredSize(new Dimension(800, 800));
		cScrollPane.add(setupPane);
		cScrollPane.setViewportView(setupPane);
		
		cFrame.getContentPane().add(cScrollPane, BorderLayout.CENTER);

		cMenuBar = new CometsMenuBar(this, mode);
		cFrame.setJMenuBar(cMenuBar);
		cFrame.repaint();

		/*
		 * Make a log dump area. This reroutes any message to System.out and
		 * System.err to this log pane.
		 */
		outputArea = new JTextArea(12,50);
		outputPane = new JScrollPane(outputArea);
		TextAreaOutputStream writer = new TextAreaOutputStream(outputArea, outputPane);
		cFrame.getContentPane().add(outputPane, BorderLayout.SOUTH);
		cFrame.setVisible(true);

		System.setOut(new PrintStream(writer));
		System.setErr(new PrintStream(writer));
		
		cParamsPanel = new CometsParametersPanel(this, cParams);
		if (pParams != null)
			cParamsPanel.setPackageParametersPanels(pParams.getParametersPanels());
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
	
	public CyclicBarrier getRunBarrier()
	{
		return runBarrier;
	}
	
	/**
	 * Parses the list of arguments passed to the program in command line. 
	 * Expects to see arguments passed as "-arg1 value1 -arg2 value2 ..."
	 * @param args
	 * @return
	 */
	private Map<String, String> parseArgs(String[] args) throws CometsArgumentException
	{
		Map<String, String> argsMap = new HashMap<String, String>();
		
		/* Splits the args string array into a set of pairs of arguments.
		 * Each even numbered element (and zero) is expected to start with a '-', and each
		 * odd numbered element is just a string.
		 * 
		 * If any even numbered element doesn't start with a '-', return null.
		 */

		// make sure there's at least one argument, or if there's more than one, that it's a mulitple of 2
		if (args.length < 2 || (args.length > 1 && args.length % 2 != 0))
		{
			throw new CometsArgumentException("COMETS requires at least one parameter - the name of the initial package loader class");
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
	
	public void setParameters(CometsParameters cParams)
	{
		this.cParams = cParams;
	}
	
	public void setPackageParameters(PackageParameters pParams)
	{
		if (getPackageParameters().getClass().equals(this.pParams.getClass()))
			this.pParams = pParams;
	}
	
	/**
	 * Parses and runs a COMETS script.
	 * @param filename
	 */
    protected void runScript(String filename)
    {
		System.out.println("running script file: " + filename);
		try
		{
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

	private void doCommandLineRun()
	{
		cParams.pause(false);
		cParams.pauseOnStep(false);
		cParams.showGraphics(false);
		cParams.setCommandLineOnly(true);

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
	 * Takes a screenshot for the current slideshow and saves it in the 
	 * given directory.
	 * @param curCycle
	 */
	public void takeSlideshowScreenshot(int curCycle)
	{
		BufferedImage image = takeScreenshot(cParams.getSlideshowLayer(), cParams.getSlideshowColorRelative(), cParams.getSlideshowColorValue());
		// done!
		try
		{
			//image.setRGB(0, 0, widthPixels, heightPixels, pixels, 0, 1);
			String imageNum = String.format("%05d", curCycle);
			ImageIO.write(image, cParams.getSlideshowExt(), new File(cParams.getSlideshowName() + "_" + imageNum + "." + cParams.getSlideshowExt()));
		}
		catch (IOException e)
		{
			System.out.println("Error on image writing: " + e);
		}
	}
	
	/**
	 * Takes a screenshot as ordered by the user, and saves it into a user-chosen
	 * file. This version only takes PNGs.
	 * 
	 * //TODO make a widget to allow any type of graphic (BMP, GIF, etc.) 
	 */
	public void takeUserScreenshot()
	{
		if (cParams.isCommandLineOnly())
		{
			System.out.println("This is only available in GUI mode");
			return;
		}
		// user picks the name and so on.
		JFileChooser chooser = new JFileChooser(cParams.getLastDirectory());
		
		int returnVal = chooser.showSaveDialog(cFrame);
		if (returnVal == JFileChooser.APPROVE_OPTION)
		{
			File file = chooser.getSelectedFile();
			String filename = file.getName();
			int extIndex = filename.lastIndexOf(".");
			String ext = filename.substring(extIndex+1, filename.length());
			if (!ext.equalsIgnoreCase("png"))
				file = new File(file.getPath() + ".png");

			BufferedImage image = takeScreenshot(cParams.getDisplayLayer(), cParams.getColorRelative(), cParams.getSlideshowColorValue());
			try
			{
				ImageIO.write(image, "png", file);
			}
			catch (IOException e)
			{
				JOptionPane.showMessageDialog(cFrame, "Unable to save image file:\n  " + e.getMessage(), "Error!", JOptionPane.ERROR_MESSAGE);
			}
			cParams.setLastDirectory(file.getParent());
		}
	}
	
	/**
	 * This does the hard work of taking a screenshot. Given a certain media layer
	 * (or biomass), this calculates the values that should be captured and draws
	 * the image. 
	 * 
	 * @param displayToggle the layer to be displayed. Given n medium components,
	 * values 0->n-1 will snapshot one of those, and a value of n (or higher) will 
	 * take a snapshot of the biomass layer
	 * @param colorRelative if set to true, this sets the intensity to be a function
	 * of the highest value on the grid
	 * @param colorValue used when colorRelative is set to false, this represents the
	 * highest intensity value on the grid, and the color of every space is relative
	 * to that
	 * @return
	 */
	public BufferedImage takeScreenshot(int displayToggle, boolean colorRelative, double colorValue)
	{	
		int numMedia = world.getNumMedia();
		int pixelScale = cParams.getPixelScale();
		int numCols = cParams.getNumCols();
		int numRows = cParams.getNumRows();
		int widthPixels = pixelScale * numCols;
		int heightPixels = pixelScale * numRows;

		BufferedImage image = new BufferedImage(widthPixels, heightPixels, BufferedImage.TYPE_INT_RGB);
		double[] m = new double[3];

		/* relative color mode - scales the color saturation (e.g. brightness) to the
		 * spot with the densest concentration of whatever we're looking at: biomass or media.
		 * this block basically sets the color scale array m[] before applying it
		 * to all spaces on the grid.
		 */
		if (colorRelative)
		{
			if (displayToggle >= numMedia) // get the maximum value for the biomass grid
			{
				Iterator<Cell> it = cellList.iterator();
				while(it.hasNext())
				{
					Cell cell = it.next();
					double[] biomass = cell.getBiomass();
					double[] sum = new double[3];
					for (int k=0; k<biomass.length; k++)
					{
						sum[k%3] += biomass[k];
					}
					for (int k=0; k<3; k++)
					{
						if (sum[k] > m[k])
							m[k] = sum[k];
					}
				}
			}
			else
				m[0] = Utility.max(world.getAllMedia(), displayToggle);
		}
		else
		{
			for (int i=0; i<3; i++)
				m[i] = colorValue;
		}

		// Get yer color on!
		for (int x=0; x<cParams.getNumCols(); x++)
		{
			for (int y=0; y<cParams.getNumRows(); y++)
			{
				// I hate myself for doing this, but here it is.
				// I'm copying and pasting the currentWorldColor() function
				// just to hack something together this afternoon.
				//
				// I'll come back later and do it right.

				int col = cParams.getBackgroundColor();
				if (world.isBarrier(x, y))
				{
					col = Utility.pColor(125, 125, 125);
				}

				else if (displayToggle == numMedia) // if it's numMedia, then display biomass levels.
				{
					Cell cell = world.getCellAt(x, y);
					if (cell != null)
					{
						double[] biomass = cell.getBiomass();
						double[] channels = new double[3];
						for (int i=0; i<biomass.length; i++)
						{
							//channels[i % 3] += biomass[i];
							if(biomass.length==1 || biomass.length == 2)
							{
								channels[i % 3] += biomass[i];
							}
							else
							{
							    if(i<biomass.length/2)
							    {
								    channels[2] += 0;
								    channels[1] += biomass[i]*Math.sin(Math.PI*i/(biomass.length-1)+Math.PI/2);
								    channels[0] += biomass[i]*Math.sin(Math.PI*i/(biomass.length-1));
							    }
							    else 
							    {
								    channels[1] += 0;
								    channels[2] += biomass[i]*Math.sin(Math.PI*i/(biomass.length-1)-Math.PI/2);
								    channels[0] += biomass[i]*Math.sin(Math.PI*i/(biomass.length-1));
							    }
							}
						}
						col = Utility.pColor((int)(channels[1]*(255/m[1])), (int)(channels[0]*(255/m[0])), (int)(channels[2]*(255/m[2])));
					}
				}
				else // otherwise, use media levels
				{
					double[] media = world.getMediaAt(x, y);
					col = Utility.pColor((int)(media[displayToggle]*(255/m[0])), 0, 0);
				}
				
				// fill an appropriate sized box with the new color
				int xStart = x*pixelScale;
				int yStart = y*pixelScale;
				for (int i = xStart; i < xStart + pixelScale; i++)
				{
					for (int j = yStart; j < yStart + pixelScale; j++)
					{
						//pixels[j*widthPixels+i] = col;
						image.setRGB(i, j, col);
					}
				}
			}
		}
		return image;
	}
	
	/**
	 * Shows an introductory <code>JDialog</code> with a welcome message and a
	 * prompt for the user to load initial data: either a layout file, something
	 * from the database, or a model file for manipulation in the layout editor.
	 */
	public void showIntroDialog()
	{
		final IntroDialog dialog = new IntroDialog(cFrame);

		dialog.getFileButton().addActionListener(
			new ActionListener() {
				public void actionPerformed(ActionEvent e)
				{
					dialog.setVisible(false);
					loadLayoutFile();
				}
			});
		
		dialog.getDBButton().addActionListener(
			new ActionListener() {
				public void actionPerformed(ActionEvent e)
				{
					dialog.setVisible(false);
					loadLayoutDB();
				}
			});
		
		dialog.getModelButton().addActionListener(
			new ActionListener() {
				public void actionPerformed(ActionEvent e)
				{
					dialog.setVisible(false);
					loadModelFile();
				}
			});
		
		dialog.setVisible(true);
	}

	/**
	 * Prompts the user to choose a model file for loading into memory.
	 * Uses the current CometsLoader for parsing the file.
	 */
	public void loadModelFile()
	{
		if (mode == SIMULATION_MODE)
		{
			int result = JOptionPane.showConfirmDialog(cFrame, 
													  "Warning: loading a new model will\nstop the currently running simulation.\n\nContinue?", "Stop current simulation?", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
			if (result == JOptionPane.OK_OPTION)
			{
				endSimulation();
			}
			else
				return;
		}

		JFileChooser chooser = new JFileChooser(cParams.getLastDirectory());
		// add filters later, maybe.
		int returnVal = chooser.showOpenDialog(cFrame);
		if (returnVal == JFileChooser.APPROVE_OPTION)
		{
			if (loader == null)
			{
				initCometsPackage(loaderClassName);
			}
			Model model = loader.loadModelFromFile(this, chooser.getSelectedFile().getPath());
			cParams.setLastDirectory(chooser.getSelectedFile().getParent());

			/*
			 * Add the model to the array (make a new array, etc.)
			 * Update the World (make one if there is no world)
			 * Update all Cells (or don't do anything)
			 */
			Model[] newModels = new Model[models.length+1];
			if (models.length > 0) // if the old array had at least a model, copy them over into the same location
			{
				for (int i=0; i<models.length; i++)
					newModels[i] = models[i];
			}
			newModels[newModels.length-1] = model;
			
			//If there is a 3D world get rid of it
			if (world3D != null)
			{
				world3D.destroy();
			}
			//Destroy 2D world too
			if (world != null)
			{
				world.destroy();
			}
			// If there's no world loaded, make a new one! Done!
			if (world == null)
			{
				world = loader.createNewWorld(this, newModels);
				cellList = new ArrayList<Cell>();
				initWorld = world.backup();
				initModels = new Model[newModels.length];
				for (int i=0; i<initModels.length; i++)
				{
					initModels[i] = newModels[i].clone();
				}
				initCellList = new ArrayList<Cell>();
			}

			// Otherwise, update the old one and all cells that may (or may not) exist
/*			else
			{
				world.changeModelsInWorld(models, newModels);
				if (cellList.size() > 0)
				{
					// gotta adjust the models known by all the cells, too.
					Iterator<Cell> it = cellList.iterator();
					while (it.hasNext())
					{
						it.next().changeModelsInCell(models, newModels);
					}
				}
			}
*/
			
			models = newModels;
			world.updateWorld();
			backupState(true);
			if(!cParams.isCommandLineOnly())
			{
					setupPane.removeGraphicSetupPanel();
					setupPane.addGraphicsSetupPanel(DIMENSIONALITY_2D);
				//cMenuBar.setDimension(cParams.getNumLayers());
			}
			if(setupPane != null)
			{
				setupPane.clear();
				setupPane.revalidate();
			}
			fireLoadEvent(new CometsLoadEvent(this, CometsLoadEvent.Type.MODEL));
		}
	}
	
	/**
	 * Loads a model from the P-KOMETS database.
	 * // TODO - make the P-KOMETS database...
	 */
	public void loadModelDB()
	{
		JOptionPane.showMessageDialog(cFrame, "Can't load from DB yet!");
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
	 * Returns the JFrame that composes this class.
	 * 
	 * @return	the master JFrame for the program 
	 */
	public JFrame getFrame()
	{
		return cFrame;
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
	 * Sets the state of the program into simulation mode and places a
	 * simulation pane (<code>CometsRunPane</code>) in the main area of the 
	 * frame. 
	 */
	public void startSimulation()
	{
		if (models == null || models.length == 0)
		{
			JOptionPane.showMessageDialog(cFrame, "No models loaded!");
			return;
		}
		else if (cellList.size() == 0)
		{
			JOptionPane.showMessageDialog(cFrame, "No cells initialized!");
			return;
		}
		else if (world == null && world3D == null)
		{
			JOptionPane.showMessageDialog(cFrame, "World not initialized!");
			return;
		}

		// from built in data (FBAWorld and such), spawn a runtime object. and make it go!
		mode = SIMULATION_MODE;

		cMenuBar.setMode(mode);
		setupPane.setMode(mode);
		cScrollPane.validate();

		runner = new CometsSimRunner(this);
		runner.start();
	}

	/**
	 * Sets the program back in setup mode, hiding the run toolbar
	 * and showing the setup toolbar.
	 */
	public void endSimulation()
	{
		if (runner != null && !runner.isFinished())
		{
			runner.finish();

			if (cParams.isPaused() && !cParams.isCommandLineOnly())
			{
				try
				{
					runBarrier.await();
				}
				catch (Exception ex)
				{
					System.out.println(ex);
				}
			}

			try
			{
				runner.join();
			}
			catch(InterruptedException e) {	}
		}
		mode = SETUP_MODE;
		if (!cParams.isCommandLineOnly() && cParams.getNumLayers()==1)
		{
			backupState(true);
			cMenuBar.setMode(mode);
			cMenuBar.canUndo(worldUndoDeque.size() > 1);
			setupPane.clear();
			setupPane.setMode(mode);
		}
		else if(!cParams.isCommandLineOnly() && cParams.getNumLayers()>1)
		{
			cMenuBar.setMode(mode);
			setupPane.clear();
			setupPane.setMode(mode);
		}
		runner = null;
		cParams.pause(true);
	}

	/**
	 * Opens a dialog that allows users to set the size of the world in
	 * boxes.
	 * 
	 * This should have an immediate effect on the <code>World2D</code> object.
	 */
	public void setWorldSizeDialog()
	{
		IntField rowField = new IntField(cParams.getNumRows(), 3, false);
		IntField colField = new IntField(cParams.getNumCols(), 3, false);
		
		JPanel gridParamPanel = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.BOTH;
		gbc.ipadx = 10;
		gbc.insets = new Insets(5,5,5,5);
		gbc.gridheight = 1;
		gbc.weightx = gbc.weighty = 0.0;

		gbc.gridwidth = 1;
		gbc.gridx = 0;
		gbc.gridy = 0;
		gridParamPanel.add(new JLabel("Grid width: "), gbc);
		
		gbc.gridx = 1;
		gridParamPanel.add(colField, gbc);
		
		gbc.gridx = 0;
		gbc.gridy = 1;
		gridParamPanel.add(new JLabel("Grid height: "), gbc);
		
		gbc.gridx = 1;
		gridParamPanel.add(rowField, gbc);

		int result = JOptionPane.showConfirmDialog(cFrame, gridParamPanel, "Set Grid Parameters", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
		if (result == JOptionPane.OK_OPTION)
		{
			int newCols = colField.getIntValue();
			int newRows = rowField.getIntValue();
			if (mode == SIMULATION_MODE && (newCols != cParams.getNumCols() || newRows != cParams.getNumRows()))
			{
				result = JOptionPane.showConfirmDialog(cFrame, "Changing the world size while a simulation\nis running will end\nthe simulation and may\nresult in a loss of data!\n\nContinue?", "Warning!", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
				if (result != JOptionPane.OK_OPTION)
					return;
			}
			cParams.setNumCols(newCols);
			cParams.setNumRows(newRows);
			world.updateWorld();

			setupPane.revalidate();
		}
	}
	
	/**
	 * Spawns a dialog box with parameter options specific to the currently
	 * loaded COMETS extension package. Right now, the only one is FBAComets, 
	 * so that's that.
	 */
	public void editPackageParametersDialog()
	{
		if (loader == null)
		{
			initCometsPackage(loaderClassName);
		}

		if (pParams == null)
			pParams = loader.getPackageParameters(this);

//		int response = 
//			JOptionPane.showConfirmDialog(cFrame, pParams.getParametersPanel(), "Set Package Parameters", 
//					JOptionPane.OK_CANCEL_OPTION, 
//					JOptionPane.DEFAULT_OPTION);
//		if (response == JOptionPane.OK_OPTION)
//			pParams.applyParametersPanelChange();
//		else
//			pParams.resetParametersPanel();

	}
	
	/**
	 * Spawns a dialog owned by Comets' frame.
	 * <p>
	 * This dialog has options for setting a bunch of parameters specific 
	 * to each type of <code>Model</code>.
	 */
	public void editModelParametersDialog()
	{
		if (world == null && world3D == null)
		{
			JOptionPane.showMessageDialog(cFrame, "No World loaded!");
			return;
		}
		else if (models.length == 0)
		{
			JOptionPane.showMessageDialog(cFrame, "No Models loaded!");
			return;
		}
		JTabbedPane modelsPane = new JTabbedPane();
		for (int i=0; i<models.length; i++)
		{
			modelsPane.add("Model " + (i+1), models[i].getParametersPanel());
		}
		int response = 
			JOptionPane.showConfirmDialog(cFrame, modelsPane, "Set Model Parameters", 
					JOptionPane.OK_CANCEL_OPTION, 
					JOptionPane.DEFAULT_OPTION);
		if (response == JOptionPane.OK_OPTION)
		{
			for (int i=0; i<models.length; i++)
				models[i].applyParameters();
			if (world != null)
			{
				world.updateWorld();
			}
			if (world3D != null)
			{
				world3D.updateWorld();
			}
		}
	}
	
	/**
	 * Spawns a dialog owned by Comets' frame with options for setting a whole bunch of
	 * parameters for the simulator. These only apply to the current run - loading 
	 * a new layout file or from the database may nullify any changes.
	 */
	public void editParametersDialog()
	{
		if (cParamsPanel == null)
			cParamsPanel = new CometsParametersPanel(this, cParams);
		if (pParams != null)
			cParamsPanel.setPackageParametersPanels(pParams.getParametersPanels());

		int response = 
			JOptionPane.showConfirmDialog(cFrame, cParamsPanel, "Set Parameters", 
					JOptionPane.OK_CANCEL_OPTION, 
					JOptionPane.DEFAULT_OPTION);

		switch (response)
		{
			case JOptionPane.OK_OPTION :
				cParamsPanel.applyChanges();
				break;
			case JOptionPane.CANCEL_OPTION :
				cParamsPanel.resetChanges();
				break;
			default :
				cParamsPanel.resetChanges();
				break;
		}
		setupPane.revalidate();
		setupPane.repaint();
		if (world != null)
			world.updateWorld();
	}

	/**
	 * Prompts the user to load a simulation layout from the public P-KOMETS 
	 * database. This is done through a series of menus and loading screens,
	 * but calling this method starts the process.
	 */
	public void loadLayoutDB()
	{
		//TODO
		JOptionPane.showMessageDialog(cFrame, "Can't load from DB yet!");
	}
	
	/**
	 * Saves the current layout state to a layout file.
	 * @param saveAs	If <code>false</code>, automatically overwrite the
	 * 					currently loaded file. If <code>true</code>, prompt
	 * 					the user for a new filename.
	 */
	public void saveLayoutFile(boolean saveAs)
	{
		
		// User hits "save"
		// Prompt for how to save biomass (and media, later)
		// Saves everything!
		// So, we need:
		//   - a widget to deal with how to save biomass
		//   - a widget to pick a filename
		//   - a way to communicate all that with the FBACometsLoader
		
		// Have the user pick a file name, then save the simulation state.
		try
		{
			loader.saveLayoutFile(this);
		}
		catch (IOException e)
		{
			// TODO stuff.
		}
	}

	/**
	 * Happens when a state change is fired.
	 * This adds the current state to the undoWorldDeque and undoCellDeques, and
	 * clears the redo Deques.
	 * Like a good queue, this adds to the back, and removes from the front when it
	 * overflows. Like a good deque, redoing REMOVES from the back.
	 */
	public void backupState(boolean clearRedo)
	{
		if (cParams.isCommandLineOnly())
			return;

		while (worldUndoDeque.size() > MAX_UNDO_DEPTH)
		{
			worldUndoDeque.removeFirst();
			cellUndoDeque.removeFirst();
			modelUndoDeque.removeFirst();
		}
		World2D undoWorld = world.backup();
		List<Cell> undoCells = new ArrayList<Cell>();
		Iterator<Cell> it = cellList.iterator();
		while (it.hasNext())
		{
			undoCells.add(it.next().backup(undoWorld));
		}
		worldUndoDeque.addLast(undoWorld);
		cellUndoDeque.addLast(undoCells);
		
		Model[] undoModels = new Model[models.length];
		for (int i=0; i<models.length; i++)
		{
			undoModels[i] = models[i];  // just copying pointers *SHOULD* work...
		}
		modelUndoDeque.addLast(undoModels);
		
		if (worldUndoDeque.size() >= 1)
			cMenuBar.canUndo(true);
		
		
		if (clearRedo)
		{
			worldRedoStack.clear();
			cellRedoStack.clear();
			modelRedoStack.clear();
			cMenuBar.canRedo(false);
		}
//		System.out.println("undo = " + worldUndoDeque.size() + " redo = " + worldRedoStack.size());
	}
	
	/**
	 * If there's more than one state in the undoDeques, then it pops off the top one and
	 * puts those in the redoDeques. We then set the last-most state in the deque to be the 
	 * currently active one.
	 */
	public void undo()
	{
		if (worldUndoDeque.size() > 1)
		{
			worldRedoStack.push(worldUndoDeque.removeLast());
			cellRedoStack.push(cellUndoDeque.removeLast());
			modelRedoStack.push(modelUndoDeque.removeLast());

//			endSimulation();
			world.destroy();
			
			cMenuBar.canRedo(true);
			resetSimulationToLastUndo();
		}
		if (worldUndoDeque.size() <= 1)
			cMenuBar.canUndo(false);
		fireLoadEvent(new CometsLoadEvent(this, CometsLoadEvent.Type.UNDO));
	}

	/**
	 * If there's at least one state in the redoDeques, then pop it off the end, and put
	 * that back on top of the undoDeques. Again, set the state on top of the undoDeques to
	 * be the active one.
	 */
	public void redo()
	{
		if (!worldRedoStack.isEmpty())
		{
			worldUndoDeque.addLast(worldRedoStack.pop());
			cellUndoDeque.addLast(cellRedoStack.pop());
			modelUndoDeque.addLast(modelRedoStack.pop());
			
//			endSimulation();
			world.destroy();
			
			cMenuBar.canUndo(true);
			resetSimulationToLastUndo();
		}
		if (worldRedoStack.isEmpty())
			cMenuBar.canRedo(false);
		fireLoadEvent(new CometsLoadEvent(this, CometsLoadEvent.Type.REDO));
	}
	
	/**
	 * Resets the simulation to the last undo save point.
	 * This removes all the currently loaded data and replaces it with the top of
	 * the undo stack.
	 */
	private void resetSimulationToLastUndo()
	{
		world = worldUndoDeque.removeLast();
		// here's a disconnect... the old world might have a different
		// size than the new world, but the main place where we keep
		// track of world sizes is in the CometsParameters.
		//
		// so... crap. guess I have to throw in a hack to set that.
		cParams.setNumCols(world.getNumCols());
		cParams.setNumRows(world.getNumRows());
		cellList = cellUndoDeque.removeLast();
		models = modelUndoDeque.removeLast();
		
		backupState(false);
		if(siFrame != null)
		{
			siFrame.dispose();
			siFrame = null;
			cMenuBar.setSpaceInfoActive(false);
		}
		
		if (miFrame != null)
		{
			miFrame.dispose();
			miFrame = null;
			cMenuBar.setModelInfoActive(false);
		}
		
		if (setupPane != null)
		{
			setupPane.clear();
			setupPane.repaint();
		}
		fireLoadEvent(new CometsLoadEvent(this, CometsLoadEvent.Type.RESET));
	}
	
	/**
	 * Saves the current simulation state. Overwrites the previously saved one.
	 */
	public void saveSimulationState()
	{
		if (world != null) // if there's no world, then don't bother backing anything up!
		{
			initWorld = world.backup();
			if (cellList != null || cellList.size() > 0)
			{
				initCellList = new ArrayList<Cell>();
				Iterator<Cell> it = cellList.iterator();
				while (it.hasNext())
				{
					initCellList.add(it.next().backup(initWorld));
				}
			}
		}
	}
	
	/**
	 * Writes the biomass level of each species to a file with a given name.
	 * Format can be either Matlab matrix (CometsConstants.MATLAB_FORMAT) or
	 * COMETS default (CometsConstants.COMETS_FORMAT).
	 * 
	 * Matlab matrix format.
	 * A Matlab script file (.m) is written with a number of variables equal
	 * to the number of species. Each variable is a matrix with dimensions 
	 * equal to the size of the current World, and each element equal to the
	 * amount of biomass in that space.
	 * 
	 * Looks something like:
	 * biomass1 = [xxx xxx xxx
	 *             xxx xxx xxx
	 *             xxx xxx xxx];
	 * biomass2 = [yyy yyy yyy
	 *             yyy yyy yyy
	 *             yyy yyy yyy];
	 * 
	 * COMETS format is similar to the log file formats.
	 * On each line:
	 * <x-coord> <y-coord> <biomass1> <biomass2> ... <biomass n>
	 */
	public void saveBiomassSnapshot()
	{
		if (world == null)
		{
			JOptionPane.showMessageDialog(cFrame, "There's no model loaded!");
			return;
		}
		JFileChooser chooser = new JFileChooser(cParams.getLastDirectory());
		FileNameExtensionFilter matlabFilter = new FileNameExtensionFilter("Matlab script", "m");
		FileNameExtensionFilter defaultFilter = new FileNameExtensionFilter("Default COMETS format", "txt", "comets");
		chooser.addChoosableFileFilter(matlabFilter);
		chooser.addChoosableFileFilter(defaultFilter);
		int returnVal = chooser.showSaveDialog(cFrame);
		if (returnVal == JFileChooser.APPROVE_OPTION)
		{
			File file = chooser.getSelectedFile();
			FileFilter ff = (FileFilter) chooser.getFileFilter();
			try
			{
				FileWriter fw = new FileWriter(file);
				if (ff == matlabFilter)
				{
					for (int i=0; i<models.length; i++)
					{
						fw.write("biomass");
						if (models.length > 1)
							fw.write((i+1));
						fw.write(" = [");
						for (int j=0; j<cParams.getNumCols(); j++)
						{
							fw.write("\n\t");
							for (int k=0; k<cParams.getNumRows(); k++)
							{
								double[] biomass = world.getBiomassAt(j, k);
								fw.write(biomass[i] + " ");
							}
						}
						fw.write("\n];\n");
					}
				}
				else
				{
					for (int i=0; i<cParams.getNumCols(); i++)
					{
						for (int j=0; j<cParams.getNumRows(); j++)
						{
							fw.write(i + "\t" + j);
							double[] biomass = world.getBiomassAt(i, j);
							for (int k=0; k<biomass.length; k++)
							{
								fw.write("\t" + biomass[k]);
							}
							fw.write("\n");
						}
					}
				}
				fw.flush();
				fw.close();
			}
			catch(IOException e)
			{
				JOptionPane.showMessageDialog(cFrame, "IO Error: " + e);
				System.out.println(e);
			}
			cParams.setLastDirectory(chooser.getSelectedFile().getParent());
		}
	}
	
	/**
	 * Sets what expansion package to use. Currently, there's only FBA, but
	 * we should leave in code-stubs for other packages based on the API.
	 */
	public void setExpansionPackage()
	{
		JOptionPane.showMessageDialog(cFrame, "Stuck with FBA for a while!");
	}
	
	/**
	 * Prompts the user to load a layout file from the disk by spawning
	 * a file chooser. If a layout is already loaded, and a simulation is 
	 * running, the simulation is stopped and the layout information is 
	 * cleared.
	 */
	public void loadLayoutFile()
	{
		if (mode == SIMULATION_MODE)
		{
			int result = JOptionPane.showConfirmDialog(cFrame, 
													  "Warning: loading another layout file will\ncancel the currently running simulation\nand remove any loaded data!\n\nContinue?", "Cancel current simulation?", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
			if (result == JOptionPane.OK_OPTION)
			{
//				if (runPane != null)
				endSimulation();
			}
			else
				return;
		}

		JFileChooser chooser = new JFileChooser(cParams.getLastDirectory());

		// add filters later, maybe.
//		JFileFilter filter = new JFileFilter();
//		filter.addType("sbml");
//		filter.addType("txt");
//		filter.setDescription("COMETS files (*.txt, *.sbml)");
//		chooser.addChoosableFileFilter(filter);
		int returnVal = chooser.showOpenDialog(cFrame);
		if (returnVal == JFileChooser.APPROVE_OPTION)
		{
			loadLayoutFile(chooser.getSelectedFile().getPath());
			cParams.setLastDirectory(chooser.getSelectedFile().getParent());
			
			if (siFrame != null)
			{
				siFrame.dispose();
				siFrame = null;
				cMenuBar.setSpaceInfoActive(false);
			}
			
			if (miFrame != null)
			{
				miFrame.dispose();
				miFrame = null;
				cMenuBar.setModelInfoActive(false);
			}
			setupPane.clear();
			setupPane.revalidate();
			fireLoadEvent(new CometsLoadEvent(this, CometsLoadEvent.Type.LAYOUT));
			//setupPane.repaint();
		}
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
			loader.loadLayoutFile(filename, this, !cParams.isCommandLineOnly());
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
			
			if(!cParams.isCommandLineOnly())
			{
				//add the 2D or 3D display 
				if(cParams.getNumLayers()==1)
				{
					setupPane.removeGraphicSetupPanel();
					setupPane.addGraphicsSetupPanel(DIMENSIONALITY_2D);
				}
				else if(cParams.getNumLayers()>1)
				{
					setupPane.removeGraphicSetupPanel();
					setupPane.addGraphicsSetupPanel(DIMENSIONALITY_3D);
				}
			}
			// Fetch the relevant data from the layout file
			
			if (cParams.getNumLayers()==1)
			{
				world = loader.getWorld();
				if(!cParams.isCommandLineOnly())
					this.showLayoutToolbar(true);
			}
			else if(cParams.getNumLayers()>1)
			{
				world3D = loader.getWorld3D();
				if(!cParams.isCommandLineOnly())
					this.showLayoutToolbar(false);
			}
			if(!cParams.isCommandLineOnly())
				this.cMenuBar.setMode(SETUP_MODE);
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
			if (cParams.getNumLayers()==1)
			{
				worldUndoDeque.clear();
				worldRedoStack.clear();
				cellUndoDeque.clear();
				cellRedoStack.clear();
				modelUndoDeque.clear();
				modelRedoStack.clear();
				if (!cParams.isCommandLineOnly())
				{	
					backupState(true);
					cMenuBar.canUndo(false);
				}
			}
		}
		catch(IOException e)
		{
			System.out.println(e);
		}
	}
	
	/**
	 * Resets the currently running simulation to the initial state it was loaded in, 
	 * or the most recently saved layout.
	 */
	public void resetSimulationToSavedState()
	{
		if (world != null)
		{
			String str = "This will reset the simulation to the\nmost recently saved state.\n\nAny unsaved data will be lost!\n\nContinue?";
			int result = JOptionPane.showConfirmDialog(cFrame, 
					  								   str, 
					  								   "Cancel current simulation?", 
					  								   JOptionPane.OK_CANCEL_OPTION, 
					  								   JOptionPane.WARNING_MESSAGE);
			if (result == JOptionPane.OK_OPTION)
			{
				endSimulation();
				world.destroy();
				
				world = initWorld;
				// here's a disconnect... the old world might have a different
				// size than the new world, but the main place where we keep
				// track of world sizes is in the CometsParameters.
				//
				// so... crap. guess I have to throw in a hack to set that.
				cParams.setNumCols(world.getNumCols());
				cParams.setNumRows(world.getNumRows());
				cellList = initCellList;
				
				initWorld = world.backup();
				initCellList = new ArrayList<Cell>();
				Iterator<Cell> it = cellList.iterator();
				while (it.hasNext())
				{
					initCellList.add(it.next().backup(initWorld));
				}
				
				if(siFrame != null)
				{
					siFrame.dispose();
					siFrame = null;
					cMenuBar.setSpaceInfoActive(false);
				}
				
				if (miFrame != null)
				{
					miFrame.dispose();
					miFrame = null;
					cMenuBar.setModelInfoActive(false);
				}
			}
			return;
		}
	}

	/**
	 * Makes a modal "About COMETS" dialog.
	 */
	public void aboutDialog()
	{
		JLabel headerLabel = new JLabel("<html><h3>COMETS: Computation Of Microbial Ecosystems in Time and Space</h3></html>", JLabel.LEFT);
		JLabel cometsLabel = new JLabel("<html><a href=\"http://www.bu.edu/segrelab/comets\">http://www.bu.edu/segrelab/comets</a>", JLabel.LEFT);
		JLabel buLabel = new JLabel("<html>Boston University</html>", JLabel.LEFT);
		JLabel segreLabel = new JLabel("<html>Daniel Segre lab - <a href=\"http://www.bu.edu/segrelab\">http://www.bu.edu/segrelab</a></html>", JLabel.LEFT);
		JLabel versionLabel = new JLabel("<html>Version " + versionString + "</html>");
		
		cometsLabel.addMouseListener(
				new MouseAdapter() {
					public void mouseClicked(MouseEvent e)
					{
						openBrowserURL("http://www.bu.edu/segrelab/comets");
					}
				});

		segreLabel.addMouseListener(
				new MouseAdapter() {
					public void mouseClicked(MouseEvent e)
					{
						openBrowserURL("http://www.bu.edu/segrelab");
					}
				});

		
		JPanel panel = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.BOTH;
		gbc.gridheight = 1;
		gbc.weightx = gbc.weighty = 0.0;

		gbc.gridwidth = 1;
		gbc.gridx = 0;
		gbc.gridy = 0;
		panel.add(headerLabel, gbc);
		gbc.gridy = 1;
		panel.add(cometsLabel, gbc);
		gbc.gridy = 2;
		panel.add(buLabel, gbc);
		gbc.gridy = 3;
		panel.add(segreLabel, gbc);
		gbc.gridy = 4;
		panel.add(versionLabel, gbc);

		JOptionPane.showMessageDialog(cFrame, 
									  panel, 
									  "About COMETS", 
									  JOptionPane.PLAIN_MESSAGE);
	}

	/**
	 * If the desktop mechanism is supported, then this opens the given URL in
	 * the operating system's default web browser
	 * @param url - the URL to open.
	 */
	public void openBrowserURL(String url)
	{
		try
		{
			final URI uri = new URI(url);
			if (Desktop.isDesktopSupported())
			{
				Desktop desktop = Desktop.getDesktop();
				desktop.browse(uri);
			}
		}
		catch(Exception e)
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
	 * Shows or hides the toolbar containing layout tools.
	 * @param b - if true, show the toolbar, otherwise hide it
	 */
	public void showLayoutToolbar(boolean b)
	{
		setupPane.showLayoutToolbar(b);
	}

	/**
	 * Shows or hides a frame with information on the loaded models, as 
	 * provided by the <code>Model</code> objects. 
	 * @param b - if true, show the frame, otherwise hide it
	 */
	public void showModelInfoFrame(boolean b)
	{
		if (miFrame == null)
		{
			miFrame = new CometsInfoFrame(this, "Model info");
			miFrame.addWindowListener(
					new WindowAdapter() {
						public void windowClosing(WindowEvent e)
						{
							cMenuBar.setModelInfoActive(false);
						}
					});
		}
		if (miFrame.hasInfoPanel() == false && models != null && models.length > 0)
		{
			// make a tabbed pane full of Model.getInfoPanel()s.
			JTabbedPane modelPanel = new JTabbedPane();
			for (int i=0; i<models.length; i++)
			{
				modelPanel.add("Model " + (i+1), models[i].getInfoPanel());
			}
			miFrame.setInfoPanel(modelPanel);
		}
		miFrame.setVisible(b);
		
	}

	/**
	 * Shows or hides a JFrame containing information on a given space 
	 * (grid square), as provided by the World2D object.
	 * @param b - If true, show the frame, otherwise hide it.
	 */
	public void showSpaceInfoFrame(boolean b)
	{
		if (siFrame == null)
		{
			siFrame = new CometsInfoFrame(this, "Space info");
			siFrame.addWindowListener(
					new WindowAdapter() {
						public void windowClosing(WindowEvent e)
						{
							cMenuBar.setSpaceInfoActive(false);
						}
					});
		}
		if (siFrame.hasInfoPanel() == false && world != null)
		{
			siFrame.setInfoPanel(world.getInfoPanel(0, 0));
		}
		siFrame.setVisible(b);
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
			System.err.println(e);
			e.printStackTrace();
		}
		
		// load up the parameters, too.
		pParams = loader.getPackageParameters(this);
	}
	
	/**
	 * If there's a GUI being used, force it to update.
	 */
	public void updateOnCycle()
	{
		if (!cParams.isCommandLineOnly())
			setupPane.repaint();
	}
	
	public void addMediaNoiseDialog()
	{
		DoubleField noiseField = new DoubleField(0.1, 3, false);
		JPanel noiseParamPanel = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();

		gbc.fill = GridBagConstraints.BOTH;
		gbc.ipadx = 10;
		gbc.insets = new Insets(5,5,5,5);
		gbc.gridheight = 1;
		gbc.weightx = gbc.weighty = 0.0;

		gbc.gridwidth = 1;
		gbc.gridx = 0;
		gbc.gridy = 0;
		noiseParamPanel.add(new JLabel("Max fraction change (0-1): "), gbc);
		
		gbc.gridx = 1;
		noiseParamPanel.add(noiseField, gbc);
		
		int result = JOptionPane.showConfirmDialog(cFrame, noiseParamPanel, "Add Media Noise", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
		if (result == JOptionPane.OK_OPTION)
		{
			double maxFraction = noiseField.getDoubleValue();
			world.addMediaNoise(maxFraction);
			setupPane.repaint();
		}		
	}
	
	public void addBiomassNoiseDialog()
	{
		DoubleField noiseField = new DoubleField(0.1, 3, false);
		JPanel noiseParamPanel = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();

		gbc.fill = GridBagConstraints.BOTH;
		gbc.ipadx = 10;
		gbc.insets = new Insets(5,5,5,5);
		gbc.gridheight = 1;
		gbc.weightx = gbc.weighty = 0.0;

		gbc.gridwidth = 1;
		gbc.gridx = 0;
		gbc.gridy = 0;
		noiseParamPanel.add(new JLabel("Max fraction change (0-1): "), gbc);
		
		gbc.gridx = 1;
		noiseParamPanel.add(noiseField, gbc);
		
		int result = JOptionPane.showConfirmDialog(cFrame, noiseParamPanel, "Add Biomass Noise", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
		if (result == JOptionPane.OK_OPTION)
		{
			double maxFraction = noiseField.getDoubleValue();
			world.addBiomassNoise(maxFraction);
			setupPane.repaint();
		}				
	}
	@Override
	public void cometsChangePerformed(CometsChangeEvent e)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void cometsLoadPerformed(CometsLoadEvent e)
	{
		cParamsPanel.updateState(this);
		setupPane.revalidate();
		setupPane.repaint();
		if (world != null)
			world.updateWorld();		
	}

	public void loadParametersFile()
	{
		try
		{
			ParametersFileHandler.loadParametersFile(cFrame, cParams, pParams);
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

	public void saveParametersFile()
	{
		try
		{
			ParametersFileHandler.saveParametersFile(cFrame, cParams, pParams);
		}
		catch (IOException e)
		{
			System.out.println("Error occurred while saving parameter file:\n" + e.getMessage());
		}
	}
	
	/**
	 * The standard main function. Running this will initialize the main
	 * COMETS frame and associated components.
	 * @param args	irrelevant in this case
	 */
	public static void main(String[] args)
	{
		new Comets(args);
	}


}