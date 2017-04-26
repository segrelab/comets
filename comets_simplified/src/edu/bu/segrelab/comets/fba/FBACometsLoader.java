/**
 * FBACometsLoader 
 * ----------------
 * A class that implements the CometsLoader, this acts as the gateway to the FBA functionality
 * of the edu.bu.segrelab.comets.fba package.
 * 
 * @author Bill Riehl briehl@bu.edu
 */
package edu.bu.segrelab.comets.fba;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import edu.bu.segrelab.comets.Cell;
import edu.bu.segrelab.comets.Comets;
import edu.bu.segrelab.comets.CometsConstants;
import edu.bu.segrelab.comets.CometsLoader;
import edu.bu.segrelab.comets.CometsParameters;
import edu.bu.segrelab.comets.Model;
import edu.bu.segrelab.comets.PackageParameterBatch;
import edu.bu.segrelab.comets.ParametersFileHandler;
import edu.bu.segrelab.comets.RefreshPoint;
import edu.bu.segrelab.comets.StaticPoint;
import edu.bu.segrelab.comets.World2D;
import edu.bu.segrelab.comets.World3D;
import edu.bu.segrelab.comets.exception.LayoutFileException;
import edu.bu.segrelab.comets.exception.ModelFileException;
import edu.bu.segrelab.comets.exception.ParameterFileException;
import edu.bu.segrelab.comets.fba.ui.LayoutSavePanel;
import edu.bu.segrelab.comets.reaction.ReactionModel;
import edu.bu.segrelab.comets.util.Circle;
import edu.bu.segrelab.comets.util.Utility;
import edu.bu.segrelab.comets.util.Point3d;

public class FBACometsLoader implements CometsLoader, 
CometsConstants
{
	private enum LoaderState
	{
		OK,
		CANCELED,
		ERROR
	}

	private FBAWorld world;
	private FBAWorld3D world3D;
	private FBAModel[] models;
	protected ReactionModel reactionModel;
	private List<Cell> cellList;
	private String mediaFileName;
	protected FBAParameters pParams;   // 'pParams' keeps inline with PackageParameters
	private LayoutSavePanel layoutSavePanel = null;
	private boolean useGui;
	private Comets c;
	private int lineCount;
	private boolean substrate = false;
	private boolean modelDiffusion = false;
	private boolean specific = false;
	private boolean friction = false;
	private double[][] substrateDiffConsts;
	private double[][] modelDiffConsts;
	private int[][] substrateLayout;
	private double[][] specificMedia;
	private double[][] substrateFrictionConsts;
	//protected double[][] exRxnStoich; //dimensions are ReactionID by MetID (in World Media list)
	//protected double[][] exRxnParams; //same dims as exRxnStoich. Stores either the Michaelis constant or reaction order
									//depending on if the reaction is enzymatic
	//protected double[] exRxnRateConstants; //Kcat for enzymatic reactions, or the forward reaction rate for simple reactions
	//protected int[] exRxnEnzymes; //index of the corresponding reaction's enzyme in the World Media list. Non-enzymatic reactions have -1 here
	
	private static final String MODEL_FILE = "model_file",
			MODEL_WORLD = "model_world",
			GRID_SIZE = "grid_size",
			WORLD_MEDIA = "world_media",
			MEDIA_REFRESH = "media_refresh",
			STATIC_MEDIA = "static_media",
			INITIAL_POP = "initial_pop",
			//								RANDOM_POP = "random",
			//								RANDOM_RECT_POP = "random_rect",
			//								SQUARE_POP = "square",
			//								CIRCLES_POP = "circles",
			//								FILLED_POP = "filled",
			//								FILLED_RECT_POP = "filled_rect",
			BARRIER = "barrier",
			MEDIA = "media",
			PARAMETERS = "parameters",
			DIFFUSION_CONST = "diffusion_constants",
			SUBSTRATE_DIFFUSIVITY = "substrate_diffusivity",
			SUBSTRATE_FRICTION = "substrate_friction",
			MODEL_DIFFUSIVITY = "model_diffusivity",
			SUBSTRATE_LAYOUT = "substrate_layout",
			SPECIFIC_MEDIA = "specific_media",
			REACTIONS = "reactions",
			REACTIONS_REACTANTS = "reactants",
			REACTIONS_ENZYMES = "enzymes",
			REACTIONS_PRODUCTS = "products";
	/**
	 * Returns the recently loaded World2D.
	 */
	public World2D getWorld() 
	{ 
		return world; 
	}

	/**
	 * Returns the recently loaded World3D.
	 */
	public World3D getWorld3D() 
	{ 
		return world3D; 
	}
	/**
	 * Returns the recently loaded array of <code>Models</code>
	 */
	public Model[] getModels()
	{ 
		return models; 
	}

	/**
	 * Returns the recently loaded <code>List&lt;Cell&gt;</code> of cells (or an
	 * empty list if there's none initialized.
	 */
	public List<Cell> getCells() 
	{ 
		return cellList; 
	}

	/**
	 * Returns the <code>PackageParameters</code> currently known by this loader. If
	 * none has been created/loaded, this initializes a new one.
	 * @param c the <code>Comets</code> to link to this package
	 */
	public FBAParameters getPackageParameters(Comets c)
	{
		if (pParams == null)
			pParams = new FBAParameters(c);
		return pParams;
	}

	/**
	 * Loads the layout file given by <code>filename</code> (must include
	 * complete file path).
	 * <p>
	 * At the very least, a layout file must include a list of models to use
	 * (each as their own filename/path) and a nutrient file to use. If any of
	 * these are absent an error will be thrown.
	 * <p>
	 * Additionally, if the model or nutrient files are invalid (either they
	 * don't exist, or are corrupt, or anything that would cause 
	 * <code>File.isFile()</code> to return <code>false</code>, a file chooser
	 * can be optionally opened to let the user find a new file.
	 * <p>
	 * The file format will be described later, and elsewhere (on the wiki,
	 * I guess).
	 * <p>
	 * Eventually, make a sister-loader with an SBML or CometsML(?) format.
	 * Eventually.
	 * <p>
	 * For now, use my kludged together random format.
	 * <p>
	 * 
	 * 
	 * TODO: I'll write it up later.
	 * @param filename a String representing the pathway to the layout file.
	 * @param c the Comets object that will be loading this world layout
	 */
	public int loadLayoutFile(String filename, Comets c, boolean useGui) throws IOException
	{
		world = null;
		world3D = null;
		cellList = null;
		models = null;
		this.useGui = useGui;
		this.c = c;
		lineCount = 0;

		if (pParams == null)
			getPackageParameters(c);

		System.out.println("Loading layout file '" + filename + "'...");
		// get the path to that file.
		/* First, we get the path to the file.
		 * There's apparently no native Java method to do this.
		 * And that's lame.
		 * So here's what we do:
		 * 1. get the name (f.getName());
		 * 2. compare its length to the passed filename
		 * 3. Everything that's extraneous (0->difference-1) is the path.
		 * 4. woot.
		 */

		File f = new File(filename);
		String path = f.getParent();
		//Write the file name in the manifest file.
		try
		{
			FileWriter manifestWriter=new FileWriter(new File(path+File.separatorChar+pParams.getNopathManifestFileName()),false);
			manifestWriter.write("LayoutFileName: "+filename+System.getProperty("line.separator"));
			manifestWriter.close();
			pParams.setManifestFileName(path+File.separatorChar+pParams.getNopathManifestFileName());
		}
		catch (IOException e)
		{
			System.out.println(e.getMessage());
			System.out.println("Unable to initialize manifest file. \nContinuing without writing manifest file.");
		}		
		/*
		 * makes and stores internally: an FBAWorld, an array of FBAModels, an
		 * ArrayList of FBACells
		 */
		try
		{
			BufferedReader reader = new BufferedReader(new FileReader(filename));
			String line;

			int numMedia;
			double[] mediaRefresh = null,
					staticMedia = null;
			boolean[] globalStatic = null;

			Set<Point> barrier = new HashSet<Point>();
			Set<Point3d> barrier3D = new HashSet<Point3d>();
			Map<Point, double[]> specMedia = new HashMap<Point, double[]>();
			Map<Integer, Double> diffConsts = new HashMap<Integer, Double>();

			List<int[]> noBiomassOut = new ArrayList<int[]>();
			List<int[]> noBiomassIn = new ArrayList<int[]>();
			List<int[]> noMediaOut = new ArrayList<int[]>();
			List<int[]> noMediaIn = new ArrayList<int[]>();
			Set<StaticPoint> staticPoints = new HashSet<StaticPoint>();
			Set<RefreshPoint> refreshPoints = new HashSet<RefreshPoint>();

			while ((line = reader.readLine()) != null)
			{
				line = line.trim();		// removes leading and trailing whitespace
				lineCount++;

				// ignore empty lines.
				if (line.length() == 0)
					continue;

				String[] parsed = line.split("\\s+");
				/*
				 * Each line (or opening line for a block) starts with an
				 * identifying token.
				 * These are...
				 * 
				 * model_file
				 * model_world []
				 * 	media_refresh []
				 * 	grid_size
				 * 	barrier []
				 * 	media []
				 * 	prevent_biomass_out []
				 * 	prevent_biomass_in []
				 * 	prevent_media_out []
				 * 	prevent_media_in []
				 * initial_pop []
				 */

				if (parsed[0].equalsIgnoreCase(MODEL_FILE))
				{
					/* load model files.
					 * wind up with an array of FBAModels at the end of this part.
					 * expect that the remaining tokens are filenames for model files
					 * with real pathnames.
					 * 
					 * ...
					 * Guess I'll have to combine tokens.
					 * eh, later.
					 */

					models = new FBAModel[parsed.length-1];

					LoaderState state = parseModelFileLine(path, parsed, models);
					if (state == LoaderState.CANCELED)
						throw new LayoutFileException("Layout file loading canceled", lineCount);
				}
				else if (parsed[0].equalsIgnoreCase(MODEL_WORLD))
				{
					Map<String, Point2D.Double> media = new HashMap<String, Point2D.Double>();
					LoaderState state = parseModelWorldLine(path, parsed, media);
					if (state == LoaderState.CANCELED)
						throw new LayoutFileException("Layout file loading canceled", lineCount);

					numMedia = media.size();

					/* There's lots of sub-stuff in this block. Sub-blocks, even.
					 * So it all gets parsed separately, until there's a single line
					 * with // on it.
					 */

					String worldLine;
					while(!(worldLine = reader.readLine().trim()).equalsIgnoreCase("//"))
					{
						lineCount++;
						String[] worldParsed = worldLine.split("\\s+");
						// ignore empty lines.
						if (worldLine.length() == 0)
							continue;


						/****************** WORLD MEDIA **********************/

						if (worldParsed[0].equalsIgnoreCase(WORLD_MEDIA))
						{
							/* slurp all lines into a list and pass it to parseWorldMedia */

							List<String> lines = collectLayoutFileBlock(reader);
							media = new HashMap<String, Point2D.Double>();
							state = parseWorldMediaBlock(lines, media);

							numMedia = media.size();
						}

						/****************** MEDIA REFRESH **********************/

						else if (worldParsed[0].equalsIgnoreCase(MEDIA_REFRESH))
						{
							if (worldParsed.length != numMedia + 1)
							{
								throw new IOException("the 'media_refresh' tag must be followed by one number for each of \n     " + 
										"the " + numMedia + " medium components defined by the models. The are\n     " +
										(worldParsed.length-1) + " of " + numMedia + " media components present.");
							}
							List<String> lines = collectLayoutFileBlock(reader);
							mediaRefresh = new double[numMedia];
							if(c.getParameters().getNumLayers()==1)
								state = parseRefreshMediaBlock(worldParsed, lines, mediaRefresh, refreshPoints);
							else if(c.getParameters().getNumLayers()>1)
								state = parseRefreshMediaBlock3D(worldParsed, lines, mediaRefresh, refreshPoints);
						}

						/****************** STATIC MEDIA **********************/

						else if (worldParsed[0].equalsIgnoreCase(STATIC_MEDIA))
						{
							if (worldParsed.length != (2*numMedia) + 1)
							{
								throw new IOException("the 'static_media' tag must be followed by a pair of numbers for each of \n     the " + numMedia + " medium components. The first should be a 1 if that component is set to be\n     static, or a 0 if not, followed by the concentration to remain static.");
							}

							List<String> lines = collectLayoutFileBlock(reader);
							staticMedia = new double[numMedia];
							globalStatic = new boolean[numMedia];
							if(c.getParameters().getNumLayers()==1)
								state = parseStaticMediaBlock(worldParsed, lines, staticMedia, globalStatic, staticPoints);
							else if(c.getParameters().getNumLayers()>1)
								state = parseStaticMediaBlock3D(worldParsed, lines, staticMedia, globalStatic, staticPoints);

						}

						/****************** GRID SIZE ************************/

						else if (worldParsed[0].equalsIgnoreCase(GRID_SIZE))
						{
							// needs to be a total of 3 elements in the array
							// the last 2 need to be integers.
							// that's - numCols, numRows (x, y) or (width, height)
							if (worldParsed.length > 4)
							{
								throw new IOException("the 'grid_size' tag must be followed by two or three positive integers: width, height and layers values");
							}
							c.getParameters().setNumCols(Integer.valueOf(worldParsed[1]));
							c.getParameters().setNumRows(Integer.valueOf(worldParsed[2]));
							if (worldParsed.length == 4)
								c.getParameters().setNumLayers(Integer.valueOf(worldParsed[3]));
							else if(worldParsed.length == 3)
								c.getParameters().setNumLayers(Integer.valueOf(1));

							//if (Integer.valueOf(worldParsed[3])==1)
							//{
							//	throw new IOException("In a 3D simulation the number of layers along the 3rd coordinate must be larger than 1.");
							//}	
						}

						/****************** BARRIER *************************/

						else if (worldParsed[0].equalsIgnoreCase(BARRIER))
						{
							List<String> lines = collectLayoutFileBlock(reader);
							if(c.getParameters().getNumLayers()==1)
							{
								state = parseBarrierBlock(lines, barrier);
							}
							else if(c.getParameters().getNumLayers()>1)
							{
								state = parseBarrierBlock3D(lines, barrier3D);
							}
						}

						/****************** DIFFUSION CONSTANTS ********************/

						else if (worldParsed[0].equalsIgnoreCase(DIFFUSION_CONST))
						{
							if (worldParsed.length != 2)
							{
								throw new IOException("the 'diffusion_constant' tag must be followed by the default diffusion constant value");
							}
							pParams.setDefaultDiffusionConstant(Double.parseDouble(worldParsed[1]));
							List<String> lines = collectLayoutFileBlock(reader);
							state = parseMediaDiffusionConstantsBlock(lines, diffConsts);
						}
						
						/****************** MODEL-FREE REACTIONS ********************/
						else if (worldParsed[0].equalsIgnoreCase(REACTIONS))
						{
							List<String> lines = collectLayoutFileBlock(reader);
							state = parseReactionsBlock(lines);
						}

						/****************** DIFFUSION CONSTANTS BY SUBSTRATE ********************/

						else if (worldParsed[0].equalsIgnoreCase(SUBSTRATE_DIFFUSIVITY))
						{
							substrate = true;
							List<String> lines = collectLayoutFileBlock(reader);
							state = parseSubstrateDiffusionConstantsBlock(lines,numMedia);
						}
						/****************** FRICTION CONSTANTS BY SUBSTRATE ********************/

						else if (worldParsed[0].equalsIgnoreCase(SUBSTRATE_FRICTION))
						{
							friction = true;
							List<String> lines = collectLayoutFileBlock(reader);
							state = parseSubstrateFrictionConstantsBlock(lines);
						}
						/****************** DIFFUSION CONSTANTS BY MODEL ********************/

						else if (worldParsed[0].equalsIgnoreCase(MODEL_DIFFUSIVITY))
						{
							modelDiffusion = true;
							List<String> lines = collectLayoutFileBlock(reader);
							state = parseModelDiffusionConstantsBlock(lines,numMedia);
						}

						/****************** SUBSTRATE LAYOUT ********************/

						else if (worldParsed[0].equalsIgnoreCase(SUBSTRATE_LAYOUT))
						{
							List<String> lines = collectLayoutFileBlock(reader);
							state = parseSubstrateLayoutBlock(lines,c.getParameters().getNumCols(),c.getParameters().getNumRows());
						}

						/****************** SPECIFIC MEDIA ********************/

						else if (worldParsed[0].equalsIgnoreCase(SPECIFIC_MEDIA))
						{
							specific = true;
							List<String> lines = collectLayoutFileBlock(reader);
							state = parseSpecificMediaBlock(lines);
						}

						/****************** MEDIA ***********************/

						else if (worldParsed[0].equalsIgnoreCase(MEDIA))
						{
							List<String> lines = collectLayoutFileBlock(reader);
							state = parseSpecialMediaBlock(lines, numMedia, specMedia);
						}

						else if (worldParsed[0].equalsIgnoreCase("prevent_biomass_out"))
						{
							String bioOutLine;
							while (!(bioOutLine = reader.readLine().trim()).equalsIgnoreCase("//"))
							{
								lineCount++;
								// ignore empty lines.
								if (bioOutLine.length() == 0)
									continue;

								String[] bioOutParsed = bioOutLine.split("\\s+");
								if (bioOutParsed.length < 2 || bioOutParsed.length > 2 + models.length)
								{
									throw new IOException("each line after 'prevent_biomass_out' must be an (x, y) coordinate followed by\n     which model indices are prevented from leaving that space");
								}
								int[] arr = new int[bioOutParsed.length];
								for (int i=0; i<arr.length; i++)
								{
									arr[i] = Integer.valueOf(bioOutParsed[i]);
								}
								noBiomassOut.add(arr);
							}
						}
						else if (worldParsed[0].equalsIgnoreCase("prevent_biomass_in"))
						{
							String bioInLine;
							while (!(bioInLine = reader.readLine().trim()).equalsIgnoreCase("//"))
							{
								lineCount++;
								// ignore empty lines.
								if (bioInLine.length() == 0)
									continue;

								String[] bioInParsed = bioInLine.split("\\s+");
								if (bioInParsed.length < 2 || bioInParsed.length > 2 + models.length)
								{
									throw new IOException("each line after 'prevent_biomass_in' must be an (x, y) coordinate followed by\n     which model indices are prevented from entering that space");
								}
								int[] arr = new int[bioInParsed.length];
								for (int i=0; i<arr.length; i++)
								{
									arr[i] = Integer.valueOf(bioInParsed[i]);
								}
								noBiomassIn.add(arr);
							}
						}
						else if (worldParsed[0].equalsIgnoreCase("prevent_media_out"))
						{
							String mediaOutLine;
							while (!(mediaOutLine = reader.readLine().trim()).equalsIgnoreCase("//"))
							{
								lineCount++;
								// ignore empty lines.
								if (mediaOutLine.length() == 0)
									continue;

								String[] mediaOutParsed = mediaOutLine.split("\\s+");
								if (mediaOutParsed.length < 2 || mediaOutParsed.length > 2 + numMedia)
								{
									throw new IOException("each line after 'prevent_media_out' must be an (x, y) coordinate followed by\n     which media indices are prevented from leaving that space.");
								}
								int[] arr = new int[mediaOutParsed.length];
								for (int i=0; i<arr.length; i++)
								{
									arr[i] = Integer.valueOf(mediaOutParsed[i]);
								}
								noMediaOut.add(arr);
							}
						}
						else if (worldParsed[0].equalsIgnoreCase("prevent_media_in"))
						{
							String mediaInLine;
							while (!(mediaInLine = reader.readLine().trim()).equalsIgnoreCase("//"))
							{
								lineCount++;
								// ignore empty lines.
								if (mediaInLine.length() == 0)
									continue;

								String[] mediaInParsed = mediaInLine.split("\\s+");
								if (mediaInParsed.length < 2 || mediaInParsed.length > 2 + numMedia)
								{
									throw new IOException("each line after 'prevent_media_in' must be an (x, y) coordinate followed by which \n     media indices are prevented from entering that space, or none if no\n     media is allowed to enter.");
								}
								int[] arr = new int[mediaInParsed.length];
								for (int i=0; i<arr.length; i++)
								{
									arr[i] = Integer.valueOf(mediaInParsed[i]);
								}
								noMediaIn.add(arr);
							}
						}
					}
					System.out.println("Constructing world...");

					/******************** assemble the FBAWorld object here. **********************/

					if (numMedia == -1 || media == null) // media hasn't been initialized...
					{
						throw new IOException("The medium is uninitialized! Either link a medium file to the 'model_world' tag, or include a 'world_media' block.");
					}

					String[] mediaNames = new String[numMedia];
					double[] mediaConc = new double[numMedia];
					for (String str : media.keySet())
					{
						Point2D.Double val = media.get(str);
						mediaNames[(int)val.getY()] = str;
						mediaConc[(int)val.getY()] = val.getX();
					}
					//Choose 2D or 3D world given the dimensionality
					//System.out.println("OK"+c.getParameters().getNumLayers());
					if(c.getParameters().getNumLayers()==1)
					{
						world = new FBAWorld(c, mediaNames, mediaConc, models);
						//world = new FBAWorld(w, h, media, models.length, showGraphics, toroidalWorld);
						if (mediaRefresh != null)
						{
							//world.setMediaRefreshAmount(mediaRefresh);
							for (RefreshPoint rp : refreshPoints)
							{
								world.addMediaRefreshSpace(rp);
							}
						}

						if (staticMedia != null && globalStatic != null)
						{
							for (StaticPoint sp : staticPoints)
							{
								world.addStaticMediaSpace(sp);
							}
							//world.setGlobalStaticMedia(staticMedia, globalStatic);
						}

						// set barrier spaces.
						for (Point p : barrier)
						{
							world.setBarrier((int)p.getX(), (int)p.getY(), true);
						}

						// set specifically determined media in given spaces
						for (Point p : specMedia.keySet())
						{
							world.setMedia((int)p.getX(), (int)p.getY(), specMedia.get(p));
						}

						// set spaces where biomass isn't allowed to diffuse out
						Iterator<int[]> itNoBioOut = noBiomassOut.iterator();
						while (itNoBioOut.hasNext())
						{
							int[] arr = itNoBioOut.next();
							if (arr.length > 2)
							{
								for (int i=2; i<arr.length; i++)
								{
									world.setDiffuseBiomassOut(arr[0], arr[1], arr[i], false);
								}
							}
							else
							{
								for (int i=0; i<models.length; i++)
								{
									world.setDiffuseBiomassOut(arr[0], arr[1], i, false);
								}
							}
						}

						// set spaces where biomass isn't allowed to diffuse in
						Iterator<int[]> itNoBioIn = noBiomassIn.iterator();
						while (itNoBioIn.hasNext())
						{
							int[] arr = itNoBioIn.next();
							if (arr.length > 2)
							{
								for (int i=2; i<arr.length; i++)
								{
									world.setDiffuseBiomassIn(arr[0], arr[1], arr[i], false);
								}
							}
							else
							{
								for (int i=0; i<models.length; i++)
								{
									world.setDiffuseBiomassIn(arr[0], arr[1], i, false);
								}
							}
						}

						// set spaces where media isn't allowed to diffuse out
						Iterator<int[]> itNoMediaOut = noMediaOut.iterator();
						while (itNoMediaOut.hasNext())
						{
							int[] arr = itNoMediaOut.next();
							if (arr.length > 2)
							{
								for (int i=2; i<arr.length; i++)
								{
									world.setDiffuseMediaOut(arr[0], arr[1], arr[i], false);
								}
							}
							else
							{
								for (int i=0; i<numMedia; i++)
								{
									world.setDiffuseMediaOut(arr[0], arr[1], i, false);
								}
							}
						}

						// set spaces where media isn't allowed to diffuse in
						Iterator<int[]> itNoMediaIn = noMediaIn.iterator();
						while (itNoMediaIn.hasNext())
						{
							int[] arr = itNoMediaIn.next();
							if (arr.length > 2)
							{
								for (int i=2; i<arr.length; i++)
								{
									world.setDiffuseMediaIn(arr[0], arr[1], arr[i], false);
								}
							}
							else
							{
								for (int i=0; i<numMedia; i++)
								{
									world.setDiffuseMediaIn(arr[0], arr[1], i, false);
								}
							}
						}
						/* Set diffusion constants */
						double[] diffusionConsts = new double[numMedia];
						double defaultDiffConst = pParams.getDefaultDiffusionConstant();
						for (int i=0; i<numMedia; i++)
							diffusionConsts[i] = defaultDiffConst;

						for (int index : diffConsts.keySet())
						{
							if (index >= 0 && index < numMedia)
							{
								diffusionConsts[index] = diffConsts.get(index);
							}
						}
						world.setDiffusionConstants(diffusionConsts);
						if(substrate)
						{
							world.setSubstrateDiffusion(substrateDiffConsts);
							world.setSubstrateLayout(substrateLayout);
						}
						if(modelDiffusion){
							world.setModelDiffusivity(modelDiffConsts);		
						}
						if(specific)
						{
							world.setSpecificMedia(specificMedia);
						}
						if(friction)
						{
							world.setSubstrateFriction(substrateFrictionConsts);
						}
						
						//set global External Reactions
						world.setReactionModel(reactionModel);
						//world.setExRxnEnzymes(exRxnEnzymes);
						//world.setExRxnParams(exRxnParams);
						//world.setExRxnRateConstants(exRxnRateConstants);
						//world.setExRxnStoich(exRxnStoich);
						
						System.out.println("Done!");
					}
					else if(c.getParameters().getNumLayers()>1)
					{

						world3D = new FBAWorld3D(c, mediaNames, mediaConc, models);

						//world = new FBAWorld(w, h, media, models.length, showGraphics, toroidalWorld);
						if (mediaRefresh != null)
						{
							world3D.setMediaRefreshAmount(mediaRefresh);
							for (RefreshPoint rp : refreshPoints)
							{
								world3D.addMediaRefreshSpace(rp);
							}
						}

						if (staticMedia != null && globalStatic != null)
						{
							for (StaticPoint sp : staticPoints)
							{
								world3D.addStaticMediaSpace(sp);
							}
							world3D.setGlobalStaticMedia(staticMedia, globalStatic);
						}

						// set barrier spaces.
						for (Point3d p : barrier3D)
						{
							world3D.setBarrier((int)p.getX(), (int)p.getY(), (int)p.getZ(), true);
						}

						// set specifically determined media in given spaces
						for (Point p : specMedia.keySet())
						{
							//							world3D.setMedia((int)p.getX(), (int)p.getY(), specMedia.get(p));
						}

						// set spaces where biomass isn't allowed to diffuse out
						Iterator<int[]> itNoBioOut = noBiomassOut.iterator();
						while (itNoBioOut.hasNext())
						{
							int[] arr = itNoBioOut.next();
							if (arr.length > 3)
							{
								for (int i=3; i<arr.length; i++)
								{
									world3D.setDiffuseBiomassOut(arr[0], arr[1], arr[2], arr[i], false);
								}
							}
							else
							{
								for (int i=0; i<models.length; i++)
								{
									world3D.setDiffuseBiomassOut(arr[0], arr[1], arr[2], i, false);
								}
							}
						}

						// set spaces where biomass isn't allowed to diffuse in
						Iterator<int[]> itNoBioIn = noBiomassIn.iterator();
						while (itNoBioIn.hasNext())
						{
							int[] arr = itNoBioIn.next();
							if (arr.length > 3)
							{
								for (int i=3; i<arr.length; i++)
								{
									world3D.setDiffuseBiomassIn(arr[0], arr[1], arr[2], arr[i], false);
								}
							}
							else
							{
								for (int i=0; i<models.length; i++)
								{
									world3D.setDiffuseBiomassIn(arr[0], arr[1], arr[2], i, false);
								}
							}
						}

						// set spaces where media isn't allowed to diffuse out
						Iterator<int[]> itNoMediaOut = noMediaOut.iterator();
						while (itNoMediaOut.hasNext())
						{
							int[] arr = itNoMediaOut.next();
							if (arr.length > 3)
							{
								for (int i=3; i<arr.length; i++)
								{
									world3D.setDiffuseMediaOut(arr[0], arr[1], arr[2], arr[i], false);
								}
							}
							else
							{
								for (int i=0; i<numMedia; i++)
								{
									world3D.setDiffuseMediaOut(arr[0], arr[1], arr[2], i, false);
								}
							}
						}

						// set spaces where media isn't allowed to diffuse in
						Iterator<int[]> itNoMediaIn = noMediaIn.iterator();
						while (itNoMediaIn.hasNext())
						{
							int[] arr = itNoMediaIn.next();
							if (arr.length > 3)
							{
								for (int i=3; i<arr.length; i++)
								{
									world3D.setDiffuseMediaIn(arr[0], arr[1], arr[2], arr[i], false);
								}
							}
							else
							{
								for (int i=0; i<numMedia; i++)
								{
									world3D.setDiffuseMediaIn(arr[0], arr[1], arr[2], i, false);
								}
							}
						}

						// Set diffusion constants 
						double[] diffusionConsts = new double[numMedia];
						double defaultDiffConst = pParams.getDefaultDiffusionConstant();
						for (int i=0; i<numMedia; i++)
							diffusionConsts[i] = defaultDiffConst;

						for (int index : diffConsts.keySet())
						{
							if (index >= 0 && index < numMedia)
							{
								diffusionConsts[index] = diffConsts.get(index);
							}
						}
						world3D.setDiffusionConstants(diffusionConsts);

						//set global External Reactions
						world3D.setReactionModel(reactionModel);
						//world3D.setExRxnEnzymes(exRxnEnzymes);
						//world3D.setExRxnParams(exRxnParams);
						//world3D.setExRxnRateConstants(exRxnRateConstants);
						//world3D.setExRxnStoich(exRxnStoich);
						
						System.out.println("Done!");
					}
				}

				/**************** INITIAL CELL POPULATION ***************/
				else if (parsed[0].equalsIgnoreCase(INITIAL_POP))
				{
					cellList = new ArrayList<Cell>();
					if(c.getParameters().getNumLayers()==1)
					{
						if (world == null || models == null)
						{
							throw new IOException("Gotta load FBA models and define the grid first! The 'initial_pop' tag\n     should be at the end of the file!");
						}
					}
					else if(c.getParameters().getNumLayers()>1)
					{
						if (world3D == null || models == null)
						{
							throw new IOException("Gotta load FBA models and define the grid first! The 'initial_pop' tag\n     should be at the end of the file!");
						}
					}
					List<String> lines = collectLayoutFileBlock(reader);
					parseInitialPopBlock(parsed, lines);
				}

				/*************** PARAMETERS ******************/
				else if (parsed[0].equalsIgnoreCase(PARAMETERS))
				{
					List<String> lines = collectLayoutFileBlock(reader);
					ParametersFileHandler.parseParameterList(lines, c.getParameters(), pParams);
				}
			}
		}
		catch(IOException e)
		{
			showGuiLoadError("Unable to load layout file!\nFile error: " + e.getMessage() + "\nIn layout file: " + filename + "\nLine: " + lineCount, 
					"File Error!");
			throw new IOException("File error: " + e.getMessage() + " in layout file: " + filename + " line " + lineCount);
		}
		catch(NumberFormatException e)
		{
			showGuiLoadError("Unable to load layout file!\nNumber format error: " + e.getMessage() + "\nIn layout file: " + filename + "\nLine: " + lineCount, 
					"Numerical Error!");
			throw new IOException("Number format error: " + e.getMessage() + " in layout file: " + filename + " line " + lineCount);
		}
		catch(ModelFileException e)
		{
			showGuiLoadError("Unable to load layout file!\nModel file error: " + e.getMessage() + "\nIn layout file: " + filename + "\nLine: " + lineCount, 
					"Model File Loading Error!");
			throw new IOException("Model file error: " + e.getMessage() + " in layout file: " + filename + " line " + lineCount);
		}
		catch(LayoutFileException e)
		{
			showGuiLoadError("Error with layout file:\n" + e.getMessage() + "\nIn layout file: " + filename + "\nLine: " + lineCount, "Layout file error!");
			throw new IOException("Layout file error: " + e.getMessage() + " in layout file: " + filename + " line " + lineCount);
		}
		catch(ParameterFileException e)
		{
			showGuiLoadError("Error loading parameter block:\n" + e.getMessage() + "\nIn layout file: " + filename, "Layout file error!");
			throw new IOException("Layout file error: " + e.getMessage() + " in layout file: " + filename);
		}
		// at the very end, set the display layer to biomass, since now we know how many layers there are.
		if(c.getParameters().getNumLayers()==1)
			c.getParameters().setDisplayLayer(world.getNumMedia());
		else if(c.getParameters().getNumLayers()>1)
			c.getParameters().setDisplayLayer(world3D.getNumMedia());
		return PARAMS_OK;
	}

	private void showGuiLoadError(String error, String title)
	{
		if (useGui)
			JOptionPane.showMessageDialog(c.getFrame(), error, title, JOptionPane.ERROR_MESSAGE);
	}

	private List<String> collectLayoutFileBlock(BufferedReader reader) throws IOException
	{
		List<String> lines = new ArrayList<String>();
		String line;
		while (!(line = reader.readLine().trim()).equals("//"))
			lines.add(line);

		return lines;
	}

	private LoaderState parseModelFileLine(String path, String[] tokens, FBAModel[] models) throws LayoutFileException,
	ModelFileException
	{
		System.out.println("Found " + (tokens.length-1) + " model files!");
		for (int i=0; i<tokens.length-1; i++)
		{
			String modelFileName = tokens[i+1];
			System.out.println("Loading '" + modelFileName + "' ...");

			//2-level testing.
			// first, check to see if the file, as given, is real.
			File f = new File(modelFileName);
			if (!f.isFile())  // if it's not, prepend the path onto it.
			{
				modelFileName = path + File.separatorChar + modelFileName;
				f = new File(modelFileName);
			}

			//Write the file name in the manifest file.
			try
			{
				FileWriter manifestWriter=new FileWriter(new File(pParams.getManifestFileName()),true);
				manifestWriter.write("ModelFileName: "+modelFileName+System.getProperty("line.separator"));
				manifestWriter.close();
			}
			catch (IOException e)
			{
				System.out.println("Unable to initialize manifest file. \nContinuing without writing manifest file.");
			}		

			// if STILL not found, prompt the user
			while (!f.isFile())
			{
				if (useGui)
				{
					int opt = JOptionPane.showConfirmDialog(c.getFrame(), "Unable to find model file '" + tokens[i+1] + "'\nPlease locate or cancel", "Invalid model file!", JOptionPane.OK_CANCEL_OPTION);
					if (opt == JOptionPane.OK_OPTION)
					{
						// make a JFileChooser, set response to parsed[i+1], update f
						// or return if canceled
						JFileChooser chooser = new JFileChooser(path);
						// add filters later, maybe.
						int returnVal = chooser.showOpenDialog(c.getFrame());
						if (returnVal == JFileChooser.APPROVE_OPTION)
						{
							modelFileName = chooser.getSelectedFile().getPath();
							f = new File(modelFileName);
						}
						else
							return LoaderState.CANCELED;
					}
					else
						return LoaderState.CANCELED;
				}
				else
				{
					throw new LayoutFileException("Unable to load model file '" + tokens[i+1] + "' -- canceling layout file load.", 0);
				}
			}

			models[i] = FBAModel.loadModelFromFile(f.getPath());
			models[i].setFlowDiffusionConstant(pParams.getFlowDiffRate());
			models[i].setGrowthDiffusionConstant(pParams.getGrowthDiffRate());
			models[i].setDefaultHill(pParams.getDefaultHill());
			models[i].setDefaultKm(pParams.getDefaultKm());
			models[i].setDefaultVmax(pParams.getDefaultVmax());
			models[i].setDefaultAlpha(pParams.getDefaultAlpha());
			models[i].setDefaultW(pParams.getDefaultW());

			System.out.println("Done!\n Testing default parameters...");
			int result = models[i].run();
			System.out.print("Done!\nOptimizer status code = " + result + " ");
			if (result == 180 || result == 5)
				System.out.println("(looks ok!)");
			else
				System.out.println("(might be an error?)");
			System.out.println("objective solution = " + models[i].getObjectiveSolution());
			System.out.flush();
		}

		return LoaderState.OK;
	}

	private LoaderState parseModelWorldLine(String path, String[] tokens, Map<String, Point2D.Double> media) throws LayoutFileException,
	IOException
	{
		if (tokens.length > 2)
		{
			System.out.println("The 'model_world' tag must be followed by either a path to the medium file, or nothing");
			return LoaderState.CANCELED;
		}
		// if we have two tokens, then the second one is the medium file
		// otherwise, we'll wait and see if we find a world_media tag
		if (tokens.length == 2)
		{
			mediaFileName = tokens[1];
			System.out.println("loading nutrient file '" + tokens[1] + "'");
			//2-level testing.
			// first, check to see if the file, as given, is real.
			File f = new File(mediaFileName);
			if (!f.isFile())  // if it's not, prepend the path onto it.
			{
				mediaFileName = path + File.separatorChar + tokens[1];
				f = new File(mediaFileName);
			}
			// if STILL not found, prompt the user
			while (!f.isFile())
			{
				if (useGui)
				{
					int opt = JOptionPane.showConfirmDialog(c.getFrame(), "Unable to find nutrient file '" + tokens[1] + "'\nPlease locate or cancel", "Invalid nutrient file!", JOptionPane.OK_CANCEL_OPTION);
					if (opt == JOptionPane.OK_OPTION)
					{
						// make a JFileChooser, set response to parsed[i+1], update f
						// or return if canceled
						JFileChooser chooser = new JFileChooser(path);
						// add filters later, maybe.
						int returnVal = chooser.showOpenDialog(c.getFrame());
						if (returnVal == JFileChooser.APPROVE_OPTION)
						{
							mediaFileName = chooser.getSelectedFile().getPath();
							f = new File(mediaFileName);
						}
						else
							return LoaderState.CANCELED;
					}
					else
						return LoaderState.CANCELED;
				}
				else
				{
					throw new LayoutFileException("Unable to find nutrient file '" + tokens[1] + "' -- canceling layout file load.", 0);
				}
			}

			// load model world, and lots of stuff enclosed in here.
			// can make new FBAWorld at the end of this part.

			// remember:
			// media.get("media name") = (concentration, file order)
			media = loadMediaFile(f.getPath());
		}
		return LoaderState.OK;
	}

	private LoaderState parseSpecialMediaBlock(List<String> lines, int numMedia, Map<Point, double[]> specMedia) throws LayoutFileException, NumberFormatException
	{
		for (String line : lines)
		{
			lineCount++;
			// ignore empty lines.
			if (line.length() == 0)
				continue;

			String[] mediaParsed = line.split("\\s+");
			if (mediaParsed.length != 2 + numMedia)
			{
				throw new LayoutFileException("each line after 'media' must be an (x, y) coordinate followed by concentrations\n     for all " + numMedia + " media elements to be in that space", lineCount);
			}
			Point p = new Point(Integer.parseInt(mediaParsed[0]), Integer.parseInt(mediaParsed[1]));
			double[] arr = new double[numMedia];
			for (int i=0; i<arr.length; i++)
			{
				arr[i] = Double.valueOf(mediaParsed[i+2]);
			}
			specMedia.put(p, arr);
		}
		return LoaderState.OK;
	}

	private LoaderState parseWorldMediaBlock(List<String> lines, Map<String, Point2D.Double> media) throws LayoutFileException,
	NumberFormatException
	{
		/* the 'world_media' block is the same format as a 
		 * media file, just embedded into the layout.
		 * 
		 * so it looks like this:
		 * 
		 * world_media
		 *     <component name> <conc>
		 *     <component name> <conc>
		 *     ...
		 * //
		 * 
		 * Note that these are EXPECTED to be the same names
		 * as what's in the model files. Guess we'll do some checking
		 * on that in the "assemble FBAWorld section" later on...
		 */
		int mediaCount = 0;
		for (String line : lines)
		{
			lineCount++;
			if (line.length() == 0)
				continue;

			String[] mediaParsed = line.split("\\s+");
			if (mediaParsed.length != 2)
			{
				throw new LayoutFileException("Each line of the 'world_media' block should have two tokens.\n The first should be the name of the medium component, followed by the default concentration.", lineCount);
			}
			else
			{
				media.put(mediaParsed[0], new Point2D.Double(Double.parseDouble(mediaParsed[1]), mediaCount));
				mediaCount++;
			}
		}
		return LoaderState.OK;
	}

	private LoaderState parseMediaDiffusionConstantsBlock(List<String> lines, Map<Integer, Double> diffConsts) throws LayoutFileException,
	NumberFormatException
	{
		/* the 'diffusion_constants' block is taken from the way of doing it in the Model files, so it looks like this:
		 * 
		 * diffusion_constants  <default>
		 * 		<medium number> <diff_const>
		 * 		<medium number> <diff_const>
		 * //
		 */
		for (String line : lines)
		{
			lineCount++;
			if (line.length() == 0)
				continue;

			String[] diffConstParsed = line.split("\\s+");
			if (diffConstParsed.length != 2)
				throw new LayoutFileException("Each line of the 'diffusion_constants' block should have two tokens.\n The first should be the index of the medium component, followed by its (non-default) diffusion constant.", lineCount);

			else
			{
				diffConsts.put(Integer.parseInt(diffConstParsed[0]), Double.parseDouble(diffConstParsed[1]));
			}
		}
		return LoaderState.OK;

	}

	private LoaderState parseSubstrateDiffusionConstantsBlock(List<String> lines, int numMedia) throws LayoutFileException,
	NumberFormatException
	{
		/* the 'diffusion_constants' block is taken from the way of doing it in the Model files, so it looks like this:
		 * 
		 * diffusion_constants  <default>
		 * 		<medium number> <diff_const>
		 * 		<medium number> <diff_const>
		 * //
		 */
		Integer i = 0;
		substrateDiffConsts = new double[lines.size()][numMedia];
		for (String line : lines)
		{
			lineCount++;
			if (line.length() == 0)
				continue;

			String[] diffConstParsed = line.split("\\s+");
			if (diffConstParsed.length != numMedia)
				throw new LayoutFileException("Each line of the 'diffusion_constants' block should have two tokens.\n The first should be the index of the medium component, followed by its (non-default) diffusion constant.", lineCount);

			else
			{				
				for(int j = 0;j<numMedia;j++)
				{
					substrateDiffConsts[i][j] = Double.parseDouble(diffConstParsed[j]);
				}
				i++;
			}
		}
		return LoaderState.OK;

	}

	private LoaderState parseSubstrateFrictionConstantsBlock(List<String> lines) throws LayoutFileException,
	NumberFormatException
	{
		/* the 'diffusion_constants' block is taken from the way of doing it in the Model files, so it looks like this:
		 * 
		 * diffusion_constants  <default>
		 * 		<medium number> <diff_const>
		 * 		<medium number> <diff_const>
		 * //
		 */
		Integer i = 0;
		substrateFrictionConsts = new double[lines.size()][models.length];
		for (String line : lines)
		{
			lineCount++;
			if (line.length() == 0)
				continue;

			String[] diffConstParsed = line.split("\\s+");
			if (diffConstParsed.length != models.length)
				throw new LayoutFileException("Each line of the 'diffusion_constants' block should have two tokens.\n The first should be the index of the medium component, followed by its (non-default) diffusion constant.", lineCount);

			else
			{				
				for(int j = 0;j<models.length;j++)
				{
					substrateFrictionConsts[i][j] = Double.parseDouble(diffConstParsed[j]);
				}
				i++;
			}
		}
		return LoaderState.OK;

	}
	private LoaderState parseModelDiffusionConstantsBlock(List<String> lines, int numMedia) throws LayoutFileException,
	NumberFormatException
	{
		/* the 'diffusion_constants' block is taken from the way of doing it in the Model files, so it looks like this:
		 * 
		 * diffusion_constants  <default>
		 * 		<medium number> <diff_const>
		 * 		<medium number> <diff_const>
		 * //
		 */
		Integer i = 0;
		modelDiffConsts = new double[lines.size()][numMedia];
		for (String line : lines)
		{
			lineCount++;
			if (line.length() == 0)
				continue;

			String[] diffConstParsed = line.split("\\s+");
			if (diffConstParsed.length != numMedia)
				throw new LayoutFileException("Each line of the 'diffusion_constants' block should have two tokens.\n The first should be the index of the medium component, followed by its (non-default) diffusion constant.", lineCount);

			else
			{				
				for(int j = 0;j<numMedia;j++)
				{
					modelDiffConsts[i][j] = Double.parseDouble(diffConstParsed[j]);
				}
				i++;
			}
		}
		return LoaderState.OK;

	}

	private LoaderState parseSubstrateLayoutBlock(List<String> lines,int cols,int rows) throws LayoutFileException,
	NumberFormatException
	{
		/* the 'diffusion_constants' block is taken from the way of doing it in the Model files, so it looks like this:
		 * 
		 * diffusion_constants  <default>
		 * 		<medium number> <diff_const>
		 * 		<medium number> <diff_const>
		 * //
		 */
		if (lines.size() != rows)
			throw new LayoutFileException("Each line of the 'diffusion_constants' block should have two tokens.\n The first should be the index of the medium component, followed by its (non-default) diffusion constant.", lineCount);
		else
		{
			Integer i = 0;
			substrateLayout = new int[cols][rows];
			for (String line : lines)
			{
				lineCount++;
				if (line.length() == 0)
					continue;

				String[] layoutParsed = line.split("\\s+");
				if (layoutParsed.length != cols)
					throw new LayoutFileException("Each line of the 'diffusion_constants' block should have two tokens.\n The first should be the index of the medium component, followed by its (non-default) diffusion constant.", lineCount);

				else
				{				
					for(int j = 0;j<cols;j++)
					{
						substrateLayout[j][i] = Integer.parseInt(layoutParsed[j]);
					}
					i++;
				}
			}
		}
		return LoaderState.OK;

	}

	/**The format for this block looks like this:
	 * 	REACTANTS [defaultKm=1]
	 *	rxnIdx metIdx order/|stoich|   //simple rxn
	 *	rxnIdx metIdx km               //catalyzed
	 *	ENZYMES [defaultKcat]
	 *	rxnIdx metIdx kcat
	 *	PRODUCTS 
	 *	rxnIdx metIdx stoich
	 *	//
	 * 
	 * @param lines
	 * @return
	 * @throws LayoutFileException
	 * @throws NumberFormatException
	 * @author mquintin
	 */
	public LoaderState parseReactionsBlock(List<String> lines) throws LayoutFileException, NumberFormatException
	{
		/* The format for this block looks like this:
		 * 	REACTANTS [defaultKm=1]
				rxnIdx metIdx order/|stoich| k+   //simple rxn
				rxnIdx metIdx km               //catalyzed
				ENZYMES [defaultKcat]
				rxnIdx metIdx kcat
				PRODUCTS 
				rxnIdx metIdx stoich
			//
		 */
		
		String mode = null;
		double defaultKcat = pParams.getDefaultVmax(); //TODO? replace with a proper defaultKcat param
		double defaultKm = pParams.getDefaultKm(); //TODO? replace with a param that's separate from the ones used by the exchange style
		double defaultOrder = 1;
		double defaultK = defaultKcat; //rate constant for simple reactions. TODO: parameterize

		//Find out how many reactions and media components there are
		//The first value is always either a reaction index or a header
		//If the first value is a reaction index, the second is a media index
		Integer nrxns = 0;
		Integer nmedia = 0;
		for (String line : lines){
			if (line.length() == 0)
				continue;
			String[] parsed = line.split("\\s+");
			//check that the first value is an integer
			if (parsed[0].matches("[0-9]+")){
				Integer rxn = Integer.valueOf(parsed[0]);
				if ( rxn > nrxns) nrxns = rxn;
				Integer med = Integer.valueOf(parsed[1]);
				if ( med > nmedia) nmedia = med;
			}
		}
		
		//initialize the arrays
		double[][] exRxnStoich = new double[nrxns][nmedia];
		double[][] exRxnParams = new double[nrxns][nmedia];
		double[] exRxnRateConstants = new double[nrxns];
		int[] exRxnEnzymes = new int[nrxns];
		for (int i = 0; i < exRxnEnzymes.length; i++){ exRxnEnzymes[i] = -1;}
		
		//Include a check that a given metabolite only appears for one role in a reaction, and that it's not being set twice
		boolean[][] uniquenessCheck = new boolean[nrxns][nmedia];
		
		//Parse the lines
		//First process the ENZYMES block, since that will affect how we process reactants
		for (String line : lines){
			if (line.length() == 0)
				continue;

			String[] parsed = line.split("\\s+");
			
			switch (parsed[0].toLowerCase()){
			case REACTIONS_REACTANTS: //ignore for now
				mode = REACTIONS_REACTANTS;
				break;
			case REACTIONS_PRODUCTS: //ignore for now
				mode = REACTIONS_PRODUCTS;
				break;
			case REACTIONS_ENZYMES:
				mode = REACTIONS_ENZYMES;
				if (parsed.length > 1) defaultKcat = Double.parseDouble(parsed[1]);
				break;
			default:
				if (REACTIONS_ENZYMES.equals(mode)){ //we're reading an Enzyme definition line
					//values are rxnIdx, metaboliteIdx, kcat
					Integer rxnIdx = Integer.parseInt(parsed[0]) -1; //TODO: Test case for when this isn't an integer
					if (parsed.length == 1){
						throw new LayoutFileException("Each line in the " + mode + " block must include at least two values: "+
					"A reaction number, and the index of a metabolite in the world_media list",lineCount);
					}
					
					Integer metIdx = Integer.parseInt(parsed[1]) -1;
					exRxnEnzymes[rxnIdx] = metIdx;
					
					double k = defaultKcat;
					if (parsed.length >= 3){
						k = Double.valueOf(parsed[2]);
					}					
					exRxnRateConstants[rxnIdx] = k;
				}
			}
		}
		
		//now process the REACTANTS and PRODUCTS blocks
		for (String line : lines){
			lineCount++;
			if (line.length() == 0)
				continue;

			String[] parsed = line.split("\\s+");

			switch (parsed[0].toLowerCase()){
			case REACTIONS_REACTANTS: 
				mode = REACTIONS_REACTANTS;
				if (parsed.length > 1) defaultKm = Double.parseDouble(parsed[1]);
				break;

			case REACTIONS_ENZYMES:
				mode = REACTIONS_ENZYMES; //skip this block since it's been done
				break;

			case REACTIONS_PRODUCTS:
				mode = REACTIONS_PRODUCTS;
				break;

			default: //The first value should be a reaction index. Process it according to the most recent header seen
				Integer rxnIdx = Integer.parseInt(parsed[0]) -1; //TODO: Test case for when this isn't an integer
				
				if (parsed.length == 1){
					throw new LayoutFileException("Each line in the " + mode + " block must include at least two values: A reaction number,"+
				     "and the index of a metabolite in the world_media list",lineCount);
				}
				Integer metIdx = Integer.parseInt(parsed[1]) -1;
				
				//Check that this metabolite hasn't already been used in this reaction
				if (uniquenessCheck[rxnIdx][metIdx]){
					throw new LayoutFileException("A metabolite should only appear once per reaction.",lineCount);
				}
				else (uniquenessCheck[rxnIdx][metIdx]) = true;
								
				switch (mode){
				case REACTIONS_REACTANTS: //rxnIdx metIdx order/stoich/Km
					//determine if this reaction has an enzyme
					boolean hasEnz = exRxnEnzymes[rxnIdx] >= 0;
					
					if (hasEnz){
						exRxnStoich[rxnIdx][metIdx] = -1.0;
						double km = defaultKm;
						if (parsed.length >= 3){
							km = Double.valueOf(parsed[2]);
						}
						exRxnParams[rxnIdx][metIdx] = km;
					}
					else{
						double order = defaultOrder;
						if (parsed.length >= 3){
							order = Double.valueOf(parsed[2]);
						}
						exRxnStoich[rxnIdx][metIdx] = -order;
						exRxnParams[rxnIdx][metIdx] = order;
						
						//only set the rate constant for this reaction once
						if (exRxnRateConstants[rxnIdx] == 0.0 || exRxnRateConstants[rxnIdx] == defaultK){
							double k = defaultK;
							if (parsed.length >= 4){
								k = Double.valueOf(parsed[3]); 
							}
							exRxnRateConstants[rxnIdx] = k;
						}
					}
					break;

				case REACTIONS_PRODUCTS://rxnIdx metIdx stoich
					double stoich = 1.0;
					if (parsed.length >= 3){
						stoich = Double.valueOf(parsed[2]);
					}
					exRxnStoich[rxnIdx][metIdx] = stoich;
					break;

				default:
					if (REACTIONS_ENZYMES.equals(mode)) break;
					else throw new LayoutFileException("The first line after opening the REACTIONS block should begin with " +
				 REACTIONS_REACTANTS.toUpperCase() + ", " + REACTIONS_ENZYMES.toUpperCase() + ", or " + REACTIONS_PRODUCTS.toUpperCase() +
				 ". Subsequent lines should have a reaction ID, a metabolite index corresponding to the world_media block, and an optional" +
				 " kinetic parameter.",lineCount);
				}
				break;
			}
		}
		return LoaderState.OK;
	}

	private LoaderState parseSpecificMediaBlock(List<String> lines) throws LayoutFileException,
	NumberFormatException
	{
		/* the 'diffusion_constants' block is taken from the way of doing it in the Model files, so it looks like this:
		 * 
		 * diffusion_constants  <default>
		 * 		<medium number> <diff_const>
		 * 		<medium number> <diff_const>
		 * //
		 */
		Integer i = 0;
		specificMedia = new double[4][lines.size()];
		for (String line : lines)
		{
			lineCount++;
			if (line.length() == 0)
				continue;

			String[] sMediaParsed = line.split("\\s+");
			if (sMediaParsed.length != 4)
				throw new LayoutFileException("Each line of the 'specific_media' block should have two tokens.\n The first should be the index of the medium component, followed by its (non-default) diffusion constant.", lineCount);

			else
			{				
				for(int j = 0;j<4;j++)
				{
					specificMedia[j][i] = Double.parseDouble(sMediaParsed[j]);
				}
				i++;
			}
		}
		return LoaderState.OK;
	}

	private LoaderState parseInitialPopBlock(String[] header, List<String> lines) throws LayoutFileException,
	NumberFormatException
	{

		/* First case: there's more than two tokens on the 'initial_pop' line
		 */
		if (header.length > 2)
		{
			String result = initializeBiomassPointsBatch(header, models.length, c.getParameters());
			if (result != null)
				throw new LayoutFileException(result, lineCount);
			return LoaderState.OK;
		}

		/* Second case: there's exactly two tokens (check that the last one
		 * is 'circles').
		 */
		else if (header.length == 2)
		{
			if (!header[1].equalsIgnoreCase("circles"))
				throw new LayoutFileException("Unknown tag '" + header[1] + "' following initial_pop tag", lineCount);

			// This hash will get filled with what will eventually become Cells.
			Map<Point, double[]> pointHash = new HashMap<Point,double[]>();
			Set<Circle> circleSet = new HashSet<Circle>();

			for (String line : lines)
			{
				lineCount++;
				// ignore empty lines.
				if (line.length() == 0)
					continue;

				String[] circleInfo = line.split("\\s+");
				if (circleInfo.length != 3 + models.length)
					throw new LayoutFileException("Each line that defines a circle of biomass starts with an (x,y) coordinate, a circle radius, and is followed by the amount of each species in the circle.", lineCount);

				// parse out the (x,y) coord and radius r
				int x = Integer.valueOf(circleInfo[0]);
				int y = Integer.valueOf(circleInfo[1]);
				int r = Integer.valueOf(circleInfo[2]);

				// parse out the starting biomass list and insert into an array
				double[] startingBiomass = new double[models.length];
				for (int i=0; i<startingBiomass.length; i++)
				{
					startingBiomass[i] = Double.valueOf(circleInfo[i+3]);
				}

				// build the set of points to fill in with biomass
				Set<Point> pointSet = Utility.getCirclePoints(x, y, r);
				Circle circle = new Circle((double)x, (double)y, (double)r);
				circleSet.add(circle);
				for(Point p : pointSet)
				{
					pointHash.put(p, startingBiomass);
				}
			}
			for (Point p : pointHash.keySet())
			{
				if (world.isOnGrid((int)p.getX(), (int)p.getY()) && 
						!world.isBarrier((int)p.getX(), (int)p.getY()))
					cellList.add(new FBACell((int)p.getX(), (int)p.getY(), pointHash.get(p), world, models, c.getParameters(), pParams));
			}
			world.setCircles(circleSet);
		}

		/* Final case: it's only the 'initial_pop' tag (only one token)
		 * now we go into another loop.
		 * until we see '//' alone on a line, everything else should look like:
		 * (x,y) m n
		 * where x and y are integers (from [0,w-1] and [0,h-1], respectively) (may or may not be a space after the comma)
		 * m is a positive integer (from 0) representing which species in the model this is
		 * n is a double, representing the initial concentration of species m at that point. 
		 * example: (1, 5) 0 1e-2
		 * this means that at point (1, 5), there's a starting concentration of 1e-2 (mmol/gDW) of species 0 biomass
		 */
		else if (header.length == 1)
		{
			for (String line : lines)
			{
				lineCount++;
				// ignore empty lines.
				if (line.length() == 0)
					continue;

				String[] popInfo = line.split("\\s+");
				if (popInfo.length < 3 || popInfo.length > 3 + models.length)
					throw new LayoutFileException("Each population line starts with an x and y coordinate, and is followed by the concentration of each model at that point", lineCount);
				// the first two are expected to be integers - x and y coords
				if(c.getParameters().getNumLayers()==1)
				{
					int x = Integer.valueOf(popInfo[0]);
					int y = Integer.valueOf(popInfo[1]);
					double[] startingBiomass = new double[models.length];
					for (int i=0; i<startingBiomass.length; i++)
					{
						startingBiomass[i] = Double.valueOf(popInfo[i+2]);
					}
					cellList.add(new FBACell(x, y, startingBiomass, world, models, c.getParameters(), pParams));
				}
				else if(c.getParameters().getNumLayers()>1)
				{
					int x = Integer.valueOf(popInfo[0]);
					int y = Integer.valueOf(popInfo[1]);
					int z = Integer.valueOf(popInfo[2]);
					double[] startingBiomass = new double[models.length];
					for (int i=0; i<startingBiomass.length; i++)
					{
						startingBiomass[i] = Double.valueOf(popInfo[i+3]);
					}
					cellList.add(new FBACell(x, y, z, startingBiomass, world3D, models, c.getParameters(), pParams));
				}
			}
		}
		return LoaderState.OK;

	}

	private LoaderState parseRefreshMediaBlock(String[] header, List<String> lines, double[] mediaRefresh, Set<RefreshPoint> refreshPoints) throws LayoutFileException,
	NumberFormatException
	{
		/* this block is like this:
		 * media_refresh	<nutrient1>	<nutrient2>	<nutrient3> ...
		 * 		<x>	<y>	<nut1>	<nut2>	...
		 * //
		 *
		 * Where the first line (list of nutrient concs) 
		 * represents the amount of nutrient to be refreshed over
		 * the whole grid.
		 * 
		 * Each internal line is the nutrients to be refreshed at
		 * each (x, y) coordinate.
		 */

		for (int i=0; i<mediaRefresh.length; i++)
		{
			mediaRefresh[i] = Double.valueOf(header[i+1]);
		}

		for (String line : lines)
		{
			lineCount++;
			// ignore empty lines.
			if (line.length() == 0)
				continue;

			String[] refreshParsed = line.split("\\s+");
			if (refreshParsed.length != 2 + mediaRefresh.length)
			{
				throw(new LayoutFileException("each line after 'media_refresh' must be two integer coordinates followed by how\n     much of each media component is restored at those coordinates", lineCount));
			}
			double[] refresh = new double[mediaRefresh.length];
			for (int i=0; i<refresh.length; i++)
			{
				refresh[i] = Double.valueOf(refreshParsed[i+2]);
			}
			refreshPoints.add(new RefreshPoint(Integer.valueOf(refreshParsed[0]), Integer.valueOf(refreshParsed[1]), refresh));
		}
		return LoaderState.OK;
	}

	private LoaderState parseRefreshMediaBlock3D(String[] header, List<String> lines, double[] mediaRefresh, Set<RefreshPoint> refreshPoints) throws LayoutFileException,
	NumberFormatException
	{
		/* this block is like this:
		 * media_refresh	<nutrient1>	<nutrient2>	<nutrient3> ...
		 * 		<x>	<y>	<z> <nut1>	<nut2>	...
		 * //
		 *
		 * Where the first line (list of nutrient concs) 
		 * represents the amount of nutrient to be refreshed over
		 * the whole grid.
		 * 
		 * Each internal line is the nutrients to be refreshed at
		 * each (x, y) coordinate.
		 */

		for (int i=0; i<mediaRefresh.length; i++)
		{
			mediaRefresh[i] = Double.valueOf(header[i+1]);
		}

		for (String line : lines)
		{
			lineCount++;
			// ignore empty lines.
			if (line.length() == 0)
				continue;

			String[] refreshParsed = line.split("\\s+");
			if (refreshParsed.length != 3 + mediaRefresh.length)
			{
				throw(new LayoutFileException("each line after 'media_refresh' must be three integer coordinates followed by how\n     much of each media component is restored at those coordinates", lineCount));
			}
			double[] refresh = new double[mediaRefresh.length];
			for (int i=0; i<refresh.length; i++)
			{
				refresh[i] = Double.valueOf(refreshParsed[i+3]);
			}
			refreshPoints.add(new RefreshPoint(Integer.valueOf(refreshParsed[0]), Integer.valueOf(refreshParsed[1]), Integer.valueOf(refreshParsed[2]), refresh));
		}
		return LoaderState.OK;
	}

	private LoaderState parseStaticMediaBlock(String[] header, List<String> lines, double[] staticMedia, boolean[] globalStatic, Set<StaticPoint> staticPoints) throws LayoutFileException,
	NumberFormatException
	{
		/* this block is like this:
		 * static_media		<stat1> <conc1>	<stat2>	<conc2> ...
		 * 		<x>	<y>	<stat1> <conc1> <stat2> <conc2>	...
		 * //
		 *
		 * Where the first line (list of nutrient concs) 
		 * represents the amount of nutrient to remain static over
		 * the whole grid.
		 * 
		 * Each internal line is the nutrients to be refreshed at
		 * each (x, y) coordinate.
		 */
		for (int i=1; i<header.length; i+=2)
		{
			globalStatic[i/2] = Integer.valueOf(header[i]) == 1;
			staticMedia[i/2] = Double.valueOf(header[i+1]);
		}

		for (String line : lines)
		{
			lineCount++;
			// ignore empty lines.
			if (line.length() == 0)
				continue;

			String[] staticParsed = line.split("\\s+");
			if (staticParsed.length != 2 + (staticMedia.length*2))
			{
				throw(new LayoutFileException("each line after 'static_media' must be two integer coordinates followed by a pair of numbers for each of \n     the " + (staticMedia.length) + " medium components. The first should be a 1 if that component is set to be\n     static, or a 0 if not, followed by the concentration to remain static.", lineCount));
			}
			double[] staticConc = new double[staticMedia.length];
			boolean[] isStatic = new boolean[staticMedia.length];
			for (int i=0; i<staticConc.length*2; i+=2)
			{
				isStatic[i/2] = (Integer.valueOf(staticParsed[i+2]) == 1);
				staticConc[i/2] = Double.valueOf(staticParsed[i+3]);
			}
			staticPoints.add(new StaticPoint(Integer.valueOf(staticParsed[0]), Integer.valueOf(staticParsed[1]), staticConc, isStatic));
		}
		return LoaderState.OK;
	}

	private LoaderState parseStaticMediaBlock3D(String[] header, List<String> lines, double[] staticMedia, boolean[] globalStatic, Set<StaticPoint> staticPoints) throws LayoutFileException,
	NumberFormatException
	{
		/* this block is like this:
		 * static_media		<stat1> <conc1>	<stat2>	<conc2> ...
		 * 		<x>	<y>	<z> <stat1> <conc1> <stat2> <conc2>	...
		 * //
		 *
		 * Where the first line (list of nutrient concs) 
		 * represents the amount of nutrient to remain static over
		 * the whole grid.
		 * 
		 * Each internal line is the nutrients to be refreshed at
		 * each (x, y) coordinate.
		 */
		for (int i=1; i<header.length; i+=2)
		{
			globalStatic[i/2] = Integer.valueOf(header[i]) == 1;
			staticMedia[i/2] = Double.valueOf(header[i+1]);
		}

		for (String line : lines)
		{
			lineCount++;
			// ignore empty lines.
			if (line.length() == 0)
				continue;

			String[] staticParsed = line.split("\\s+");
			if (staticParsed.length != 3 + (staticMedia.length*2))
			{
				throw(new LayoutFileException("each line after 'static_media' must be three integer coordinates followed by a pair of numbers for each of \n     the " + (staticMedia.length) + " medium components. The first should be a 1 if that component is set to be\n     static, or a 0 if not, followed by the concentration to remain static.", lineCount));
			}
			double[] staticConc = new double[staticMedia.length];
			boolean[] isStatic = new boolean[staticMedia.length];
			for (int i=0; i<staticConc.length*2; i+=2)
			{
				isStatic[i/2] = (Integer.valueOf(staticParsed[i+3]) == 1);
				staticConc[i/2] = Double.valueOf(staticParsed[i+4]);
			}
			staticPoints.add(new StaticPoint(Integer.valueOf(staticParsed[0]), Integer.valueOf(staticParsed[1]), Integer.valueOf(staticParsed[2]),staticConc, isStatic));
		}
		return LoaderState.OK;
	}


	private LoaderState parseBarrierBlock(List<String> lines, Set<Point> barrier) throws LayoutFileException, 
	NumberFormatException
	{
		for (String line : lines)
		{
			lineCount++;
			// ignore empty lines.
			if (line.length() == 0)
				continue;

			String[] barrierParsed = line.split("\\s+");
			if (barrierParsed.length != 2)
			{
				throw(new LayoutFileException("each line after 'barrier' must be two integers >= 0", lineCount));
			}
			barrier.add(new Point(Integer.valueOf(barrierParsed[0]), Integer.valueOf(barrierParsed[1])));
		}
		return LoaderState.OK;
	}

	private LoaderState parseBarrierBlock3D(List<String> lines, Set<Point3d> barrier3D) throws LayoutFileException, NumberFormatException
	{
		for (String line : lines)
		{
			lineCount++;
			// ignore empty lines.
			if (line.length() == 0)
				continue;

			String[] barrierParsed = line.split("\\s+");
			if (barrierParsed.length != 3)
			{
				throw(new LayoutFileException("each line after 'barrier' must be three integers >= 0", lineCount));
			}
			barrier3D.add(new Point3d(Integer.valueOf(barrierParsed[0]), Integer.valueOf(barrierParsed[1]),Integer.valueOf(barrierParsed[2])));
		}
		return LoaderState.OK;
	}


	private String initializeBiomassPointsBatch(String[] parsed, int numModels, CometsParameters cParams)
	{
		/*
		 * parsed will have several tokens in it.
		 * it starts with 'initial_pop'
		 * then one of 'random', 'random_rect', 'square'
		 * and the rest are context-specific. more or less.
		 * 'random' and 'square' should be followed by pairs of numbers - one int and one float
		 * for each model. So it'll look like:
		 * 'initial_pop (random/square) N1 c1 N2 c2 N3 c3 ...'
		 * 'initial_pop random_rect x y w h N1 c1 N2 c2 N3 c3 ...'
		 */
		int layoutStyle = RANDOM_LAYOUT;
		int parsePointer = 0;
		if (parsed[1].equalsIgnoreCase("random"))
		{
			if (parsed.length != 2 + (2*numModels))
				return "The 'random' layout tag must be followed by one pair of parameters for each\n     species - the number of random spaces, then the concentration of biomass.\n     E.g.: 'initial_pop random N1 conc1 N2 conc2 ...'";
			else
			{
				layoutStyle = RANDOM_LAYOUT;
				parsePointer = 2;
			}
		}
		else if (parsed[1].equalsIgnoreCase("square"))
		{
			if (parsed.length != 2 + (2*numModels))
				return "The 'square' layout tag must be followed by one pair of parameters for each\n     species - the number of random spaces, then the concentration of biomass.\n     E.g.: 'initial_pop square N1 conc1 N2 conc2 ...'";
			else
			{
				layoutStyle = SQUARE_LAYOUT;
				parsePointer = 2;
			}
		}
		else if (parsed[1].equalsIgnoreCase("random_rect"))
		{
			if (parsed.length != 6 + (2*numModels))
				return "The 'random_rect' layout tag must be followed by several parameters - the\n     upper left (x,y) coordinate of the rectangle, the width and height of the\n     rectangle, then a pair of parameters for each species - the number of random\n     spaces, then the concentration of biomass. E.g.: 'initial_pop random_rect\n     x y w h N1 conc1 N2 conc2 N3 conc3 ...'";
			else
			{
				layoutStyle = RANDOM_RECTANGLE_LAYOUT;
				parsePointer = 6;
			}
		}
		else if (parsed[1].equalsIgnoreCase("filled"))
		{
			if (parsed.length != 2 + numModels)
				return "The 'filled' layout tag must be followed by a concentration of biomass for each species";
			else if (numModels > 1 && !cParams.allowCellOverlap())
				return "It doesn't make sense to have multiple species in a 'filled' layout, and preventing them\n     from overlapping. Also, it breaks the layout initializer.";
			else
			{
				layoutStyle = FILLED_LAYOUT;
				parsePointer = 2;
			}
		}
		else if (parsed[1].equalsIgnoreCase("filled_rect"))
		{
			if (parsed.length != 6 + (2*numModels))
				return "The 'filled_rect' layout tag must be followed by several parameters - the\n     upper left (x,y) coordinate of the rectangle, the width and height of the\n     rectangle, then a pair of parameters for each species - the number of random\n     spaces, then the concentration of biomass. E.g.: 'initial_pop random_rect\n     x y w h N1 conc1 N2 conc2 N3 conc3 ...'";
			else
			{
				layoutStyle = FILLED_RECTANGLE_LAYOUT;
				parsePointer = 6;
			}
		}
		else
			return "Unknown layout style '" + parsed[1] + "'";

		// Pre-process numbers of points and such (make sure that they don't
		// outnumber the number of available spaces, right?)
		// HERE
		int[] numStartingSpaces = new int[numModels];
		double[] startingBiomass = new double[numModels];
		int x = 0;
		int y = 0;
		int w = cParams.getNumCols();
		int h = cParams.getNumRows();

		if (layoutStyle == FILLED_RECTANGLE_LAYOUT || layoutStyle == RANDOM_RECTANGLE_LAYOUT)
		{
			x = Integer.parseInt(parsed[2]);
			y = Integer.parseInt(parsed[3]);
			w = Integer.parseInt(parsed[4]);
			h = Integer.parseInt(parsed[5]);
			// Make sure the rectangle bounds are in the right range
			if (x < 0)
				return "The upper-left corner (x-coord) of your initial layout boundary is less than zero.\n     It should be in the range (0, width-1).\n";
			if (y < 0)
				return "The upper-left corner (y-coord) of your initial layout boundary is less than zero.\n     It should be in the range (0, height-1).";
			if (x >= cParams.getNumCols())
				return "The upper-left corner (x-coord) of your initial layout boundary is off the right\n     edge of the world. It should be in the range (0, width-1).";
			if (y >= cParams.getNumCols())
				return "The upper-left corner (y-coord) of your initial layout boundary is off the bottom\n     edge of the world. It should be in the range (0, height-1).";
			if (w <= 0)
				return "The width of your initial layout boundary is less than or equal to zero. It should\n     be in the range (1, " + (cParams.getNumCols() - x - 1) + ")."; 
			if (x+w > cParams.getNumCols())
				return "The width of your initial layout boundary exceeds the right side of the world.\n     It should be in the range (1, " + (cParams.getNumCols() - x - 1) + ").";
			if (h <= 0)
				return "The height of your initial layout boundary is less than or equal to zero. It should\n     be in the range (1, " + (cParams.getNumRows() - y - 1) + ").";
			if (y+h > cParams.getNumRows())
				return "The height of your initial layout boundary exceeds the bottom edge of the world.\n     It should be in the range (1, " + (cParams.getNumRows() - y - 1) + ").";
		}
		if (layoutStyle == FILLED_LAYOUT || layoutStyle == FILLED_RECTANGLE_LAYOUT)
		{
			for (int i=0; i<numModels; i++)
			{
				numStartingSpaces[i] = 0;
				startingBiomass[i] = Double.parseDouble(parsed[parsePointer] + i);
			}
		}
		else
		{
			for (int i=0; i<numModels; i++)
			{
				numStartingSpaces[i] = Integer.parseInt(parsed[parsePointer + (2*i)]);
				startingBiomass[i] = Double.parseDouble(parsed[parsePointer + (2*i + 1)]);
			}
		}

		// Make sure we're not trying to overpopulate the grid.
		if (Utility.sum(numStartingSpaces) > world.numEmptySpaces(x, y, w, h))
		{
			if (!cParams.allowCellOverlap())
			{
				String str = "The number of new spaces this layout is trying to allocate (" + Utility.sum(numStartingSpaces) + ")\n     is larger than the number of empty spaces in the ";
				if (layoutStyle == RANDOM_RECTANGLE_LAYOUT)
					str += "selected area (" + world.numEmptySpaces(x, y, w, h) + ").";
				else
					str += "world (" + world.numEmptySpaces(x, y, w, h) + ").";
				return str;
			}
			else
			{
				layoutStyle = FILLED_LAYOUT;
			}
		}

		// just use this for rapid lookup. don't really care about the value.
		for (int i=0; i<numModels; i++)
		{
			double[] curInitBiomass = new double[numModels];
			for (int j=0; j<numModels; j++)
			{
				curInitBiomass[j] = 0;
			}
			curInitBiomass[i] = startingBiomass[i];

			// Pick a set of starting points for each model.
			// Trust that these points are absolutely correct - chooseSpaces()
			// knows only to use new points for different cell types if 
			// overlap isn't allowed.

			System.out.println("finding starting spaces...");
			System.out.flush();
			Point[] startingPoints = chooseSpaces(numStartingSpaces[i], x, y, w, h, layoutStyle, cParams);
			for (int j=0; j<startingPoints.length; j++)
			{
				Point p = startingPoints[j];

				FBACell c = (FBACell)world.getCellAt((int)p.getX(), (int)p.getY());
				if (c != null) // if there's already a cell there, just add more biomass
				{
					c.changeBiomass(curInitBiomass);
				}
				else
				{
					cellList.add(new FBACell((int)p.getX(), (int)p.getY(),
							curInitBiomass,
							world,
							models,
							cParams,
							pParams));
				}
			}
		}

		return null;
	}


	/**
	 * Loads a media file. Media files are relatively simple, though
	 * they are intrinsically linked to a model file. As described elsewhere,
	 * model files have an ordered list of exchange reactions. The media
	 * file details the initial concentrations of each of these extracellular
	 * metabolites, <i>in the same order</i>.
	 * <p>
	 * Each media is given its own line. The first token is expected to be
	 * a string for the name of the metabolite (as described by the <code>FBAModel</code>
	 * files, and the second is a <code>double</code> value for the concentration
	 * of that metabolite. All other tokens are ignored.
	 * <p>
	 * TODO: add additional token(s) to describe diffusability of each 
	 * metabolite.
	 * 
	 * @param filename	A file and path to the nutrient file.
	 * @return	A <code>Map&lt;String, Point2D.Double&gt;</code> describing the media
	 * file data. This is the closest I can come to doing a triplet using Maps: each String
	 * is the name of that media component, the x-value of the Point2D.Double is the 
	 * concentration, and the y-value is the order in which it appears (important for
	 * matching up with the layout file).
	 * 
	 * TODO: Unify nutrient and layout files to avoid crap like this.
	 */
	public Map<String, Point2D.Double> loadMediaFile(String filename)
	{
		Map<String, Point2D.Double> media = new HashMap<String, Point2D.Double>();
		try
		{
			BufferedReader reader = new BufferedReader(new FileReader(filename));
			String line;

			/* each line looks like this:
			 * name1	XX
			 * name2	YY
			 * name3	ZZ
			 * ...
			 * 
			 * Where each of the names correspond with external metabolite
			 * names from the model files and the numbers are concentrations of
			 * metabolites.
			 */
			int count = 0;
			while ((line = reader.readLine()) != null)
			{
				if (line.length() == 0)
					continue;

				String[] numStr = line.split("\\s+");
				media.put(numStr[0], new Point2D.Double(Double.parseDouble(numStr[1]), count));
				count++;
				//				nameList.add(numStr[0]);
				//				mediaList.add(new Double(numStr[1]));
			}
			reader.close();
		} 
		catch (IOException e)
		{
			System.out.println("Error with nutrient file " + filename);
			e.printStackTrace();
		} 
		catch (NumberFormatException e)
		{
			System.out.println("Numerical error in nutrient file " + filename);
			System.out.println("Note that the beginning of each line MUST be the concentration of nutrient to be available.");
			e.printStackTrace();
		}
		catch (Exception e)
		{
			System.out.println("Error in nutrient file " + filename);
			e.printStackTrace();
		}
		return media;
	}

	public void saveMediaFile(String filename, String[] mediaNames, double[] genMedia) throws IOException
	{
		PrintWriter pw = new PrintWriter(new FileWriter(new File(filename)));
		for (int i=0; i<mediaNames.length; i++)
		{
			pw.println(mediaNames[i] + "\t" + genMedia[i]);
		}
		pw.close();
	}


	/**
	 * An accessory method to <code>chooseSpaces()</code>.
	 * <p>
	 * Chooses n spaces from the world at random from the currently loaded 
	 * world. A world must be loaded to set any restrictions on the point
	 * locations.
	 * 
	 * @param n	The number of points to be chosen
	 * @return	A Map containing the points. Keys are the points in 
	 * <code>String</code> format, and values are the points as 
	 * <code>Point</code> objects. <code>Null</code> is returned if there
	 * is no <code>World</code> or <code>CometsParameters</code> loaded.
	 */
	private Collection<Point> chooseRandomSpaces(int n, int x, int y, int w, int h, CometsParameters cParams)
	{		
		/* rect points: (x,y)----------------(x+w,y)
		 *                |                     |
		 *                |                     |
		 *                |                     |
		 *             (x,y+h)--------------(x+w,y+h)
		 * (inclusive)            
		 * if n = w*h, then collect all points.
		 *
		 */

		Map<String, Point> points = new HashMap<String, Point>();

		for (int i=0; i<n; i++)
		{
			int a = x + Utility.randomInt(w);
			int b = y + Utility.randomInt(h);
			String pStr = Utility.pointToString(a, b);
			// keep looking until we find an empty spot - make sure all startCells are placed
			while (points.containsKey(pStr) || 
					world.isBarrier(a, b) ||
					(!cParams.allowCellOverlap() && world.isOccupied(a, b)))
			{
				a = x + Utility.randomInt(w);
				b = y + Utility.randomInt(h);
				pStr = Utility.pointToString(a, b);
			}
			points.put(pStr, new Point(a, b));
		}
		return points.values();
	}

	/**
	 * An accessory method to <code>chooseSpaces()</code>.
	 * <p>
	 * Chooses spaces in a square with side length n. This square of spaces
	 * appears in the center of the world (offset to the upper-left in
	 * cases where it cannot be exactly centered).
	 * <p>
	 * TODO: add (x,y) option for centering the square.
	 * 
	 * @param n	The length of the side of the point-square to be chosen.
	 * @return	A Map containing the points. Keys are the points in 
	 * <code>String</code> format, and values are the points as 
	 * <code>Point</code> objects. <code>Null</code> is returned if there
	 * is no <code>World</code> or <code>CometsParameters</code> loaded.
	 */
	private Collection<Point> chooseSquareSpaces(int n, CometsParameters cParams)
	{
		Map<String, Point> points = new HashMap<String, Point>();

		if (n > cParams.getNumCols() || n > cParams.getNumRows())
			n = Math.min(cParams.getNumCols(), cParams.getNumRows());

		int startX = (cParams.getNumCols() - n)/2;
		int startY = (cParams.getNumRows() - n)/2;
		for (int i=startX; i<startX + n; i++)
		{
			for (int j=startY; j<startY + n; j++)
			{
				Point p = new Point(i, j);
				String pStr = Utility.pointToString(p);
				if (!world.isBarrier(i, j) &&
						(cParams.allowCellOverlap() || !world.isOccupied(i, j)))
				{
					points.put(pStr, p);
				}
			}
		}
		return points.values();
	}

	/**
	 * An accessory method to <code>chooseSpaces()</code>.
	 * <p>
	 * Chooses all open spaces in the given <code>World</code>.
	 * 
	 * @return	A Map containing the points. Keys are the points in 
	 * <code>String</code> format, and values are the points as 
	 * <code>Point</code> objects. <code>Null</code> is returned if there
	 * is no <code>World</code> or <code>CometsParameters</code> loaded.
	 */
	private Collection<Point> chooseAllSpaces(int x, int y, int w, int h, CometsParameters cParams)
	{
		//check boundary cases - don't overflow the x and y bounds!
		//if there's an overflow, rein it in.
		if (x < 0 || x > cParams.getNumCols()-1 || y < 0 || y > cParams.getNumRows()-1 ||
				x+w < 0 || x+w > cParams.getNumCols()-1 || y+h < 0 || y+h > cParams.getNumRows()-1)
		{
			if (x < 0)
				x = 0;
			if (x+w > cParams.getNumCols())
				w = cParams.getNumCols()-x;
			if (y < 0)
				y = 0;
			if (y+h > cParams.getNumRows())
				h = cParams.getNumRows()-y;
			if (y > cParams.getNumRows()-1)
				y = cParams.getNumRows()-1;
			if (x > cParams.getNumCols()-1)
				x = cParams.getNumCols()-1;
		}

		// now fill in the rectangle.
		List<Point> pointList = new ArrayList<Point>();
		for (int i=x; i<x+w; i++)
		{
			for (int j=y; j<y+h; j++)
			{
				if (!world.isBarrier(i,j))
					pointList.add(new Point(i, j));
			}
		}

		return pointList;
	}

	/**
	 * Builds an array of 2D <code>Points</code> representing spaces in the
	 * <code>World</code>, given the parameters. There are three pairs of
	 * parameter choices:
	 * <ul>
	 * <li>If <code>style</code> = CometsConstants.RANDOM_LAYOUT, then
	 * <code>n</code> random points are chosen on the world.
	 * <li>If <code>style</code> = CometsConstants.SQUARE_LAYOUT, then a square
	 * of <code>n</code> spaces on a side is set in the center of the world.
	 * <li>If <code>style</code> = CometsConstants.FILLED_LAYOUT, then all open
	 * spaces in the world are chosen, regardless of the value of <code>n</code>.
	 * </ul>
	 * 
	 * @param n
	 * @param style
	 * @return a Point[] array with all the chosen points
	 */
	private Point[] chooseSpaces(int n, int x, int y, int w, int h, int style, CometsParameters cParams)
	{
		if (n < 1)
			n = 1;
		// collect a Set of points.
		Collection<Point> points;
		switch(style)
		{
		case RANDOM_LAYOUT:
			points = chooseRandomSpaces(n, x, y, w, h, cParams);
			break;
		case SQUARE_LAYOUT:
			points = chooseSquareSpaces(n, cParams);
			break;
		case FILLED_LAYOUT:
			points = chooseAllSpaces(x, y, w, h, cParams);
			break;
		case RANDOM_RECTANGLE_LAYOUT:
			points = chooseRandomSpaces(n, x, y, w, h, cParams);
			break;
		case FILLED_RECTANGLE_LAYOUT:
			points = chooseAllSpaces(x, y, w, h, cParams);
			break;
		default: // nothing - return empty ArrayList
			points = new ArrayList<Point>();
			break;
		}

		// Process that Collection into an array of Points
		Point[] ret = new Point[points.size()];
		Iterator<Point> it = points.iterator();
		int i=0;
		while (it.hasNext())
		{
			ret[i] = it.next();
			i++;
		}
		return ret;
	}

	/**
	 * Saves the layout embedded in the given <code>FBAWorld</code>, <code>Models</code>, and
	 * <code>List</code> of cells to a file described by <code>path</code>, using the standard 
	 * COMETS format.
	 * 
	 * mediaFileState can be one of three values (any other throws an IOException):
	 * CometsLoader.USE_ORIGINAL_FILE, 
	 * CometsLoader.SAVE_NEW_FILE, (will cause a prompt for a new file) 
	 * CometsLoader.DO_NOT_SAVE
	 * 
	 * @param path the path to the layout file to be saved
	 * @param world the <code>FBAWorld</code> containing data to be saved
	 * @param models the <code>FBAModels</code> containing data to be saved
	 * @param cellList the <code>List</code> containing cell data (biomass, etc) to be saved
	 * @param mediaFileState as above
	 * @param cParams the <code>CometsParameters</code> corresponding to data to be saved
	 */
	public void saveLayoutFile(String path, World2D world, Model[] models, 
			List<Cell> cellList, int mediaFileState, 
			CometsParameters cParams) throws IOException
	{
		// recast into an fbaWorld. This should be used throughout the method.
		FBAWorld fbaWorld = (FBAWorld)world;
		PrintWriter pw = new PrintWriter(new FileWriter(new File(path)));

		pw.print("model_file");

		/**************************** model_file line *******************************/

		for (int i=0; i<models.length; i++)
		{
			pw.print("\t" + models[i].getFileName());
		}
		pw.print("\n");

		/**************************** model_world line ******************************/
		/* First, get the general media set. 
		 * Figure out if there's one overall set we can apply to most of the blocks.
		 * 
		 * Something like this:
		 * foreach spot on the grid
		 *     get media vector at that spot.
		 *     hash the vector into something unique (even just a string of all the values together)
		 *     find the one that's most common AND appears a lot (say, more than half? It would
		 *         be silly to have the grid be heterogeneous except for two points, then
		 *         set the base values to those two points.)
		 *     use that as a base value.
		 *     keep a hash table of all points with that vector, so we know to ignore them.
		 * UNLESS we're keeping the original media file.
		 * Then just load that into genMedia.
		 */

		double[] globalMedia = new double[fbaWorld.getNumMedia()];
		if (mediaFileState == USE_ORIGINAL_FILE)
		{
			Map<String, Point2D.Double> mediaMap = loadMediaFile(mediaFileName);
			if (mediaMap == null)
			{
				System.out.println("Unable to load original medium file '" + mediaFileName + "' - writing all medium data to the layout file instead.\n");
				mediaFileState = DO_NOT_SAVE;
			}
			else if (mediaMap.keySet().size() != world.getNumMedia())
			{
				System.out.println("The number of medium components in the world has been altered since loading the medium file. All medium data will be written to the layout file instead.\n");
			}
			// turn this into FUNK (and by funk, I mean put the values in the right order in globalMedia)
			String[] mediaNames = fbaWorld.getMediaNames();
			for (int i=0; i<mediaNames.length; i++)
			{
				Point2D.Double p = mediaMap.get(mediaNames[i]);
				globalMedia[i] = p.getX();
			}
		}
		else
		{
			// do something crazy like outlined above.
			Map<Integer, double[]> hash2MediaArr = new HashMap<Integer, double[]>();
			Map<Integer, Integer> hashCounts = new HashMap<Integer, Integer>();
			for (int i=0; i<world.getNumCols(); i++)
			{
				for (int j=0; j<world.getNumRows(); j++)
				{
					double[] media = world.getMediaAt(i, j);
					int hash = Arrays.hashCode(media);
					if (!hash2MediaArr.containsKey(hash))
						hash2MediaArr.put(hash, media);

					if (hashCounts.containsKey(hash))
						hashCounts.put(hash, (hashCounts.get(hash))+1);
					else
						hashCounts.put(hash, 1);
				}
			}
			int maxCount = 0;
			int maxHash = 0;
			for (int h : hashCounts.keySet())
			{
				if (hashCounts.get(h) > maxCount)
				{
					maxHash = h;
					maxCount = hashCounts.get(h);
				}
			}
			if (hash2MediaArr.containsKey(maxHash))
				globalMedia = hash2MediaArr.get(maxHash);
		}

		pw.print("\tmodel_world");
		if (mediaFileState == USE_ORIGINAL_FILE)
		{
			pw.println("\t" + mediaFileName);
		}
		else if (mediaFileState == SAVE_NEW_FILE)
		{
			mediaFileName = path + "_medium.txt";
			saveMediaFile(mediaFileName, world.getMediaNames(), globalMedia);
			pw.println("\t" + mediaFileName);
		}
		else if (mediaFileState == DO_NOT_SAVE)
		{
			pw.println();
		}
		pw.println("\t\tgrid_size\t" + cParams.getNumCols() + "\t" + cParams.getNumRows());

		if (mediaFileState == DO_NOT_SAVE)
		{
			// write world_media block
			pw.println("\t\tworld_media");

			/* this block looks like this:
			 * world_media
			 *     name1    conc1
			 *     name2    conc2
			 *     ...etc
			 */
			String[] mediaNames = fbaWorld.getMediaNames();
			for (int i=0; i<mediaNames.length; i++)
			{
				pw.println("\t\t\t" + mediaNames[i] + "\t" + globalMedia[i]);
			}
			pw.println("\t\t//");
		}

		/********************* media block ***********************************/
		/* only write a media line if that space's media differs
		 * from what's in the nutrient file (as told by genMedia)
		 */
		pw.println("\t\tmedia");
		for (int i=0; i<cParams.getNumCols(); i++)
		{
			for (int j=0; j<cParams.getNumRows(); j++)
			{
				double[] worldMedia = fbaWorld.getMediaAt(i, j);
				boolean diff = false;
				for (int k=0; k<worldMedia.length; k++)
				{
					if (worldMedia[k] != globalMedia[k])
					{
						diff = true;
						break;
					}
				}
				if (diff)
				{
					pw.print("\t\t\t" + i + "\t" + j);
					for (int k=0; k<worldMedia.length; k++)
					{
						pw.print("\t" + worldMedia[k]);
					}
					pw.print("\n");
				}
			}
		}
		pw.println("\t\t//");


		/********************* media_refresh block *************************/
		pw.print("\t\tmedia_refresh");
		double[] mediaRefresh = fbaWorld.getMediaRefreshAmount();
		for (int i=0; i<mediaRefresh.length; i++)
		{
			pw.print("\t" + mediaRefresh[i]);
		}
		pw.print("\n");
		List<RefreshPoint> refreshPoints = fbaWorld.getMediaRefreshSpaces();
		Iterator<RefreshPoint> it = refreshPoints.iterator();
		while (it.hasNext())
		{
			RefreshPoint rp = it.next();
			pw.print("\t\t\t" + (int)rp.getX() + "\t" + (int)rp.getY());
			double[] refresh = rp.getMediaRefresh();
			for (int i=0; i<refresh.length; i++)
			{
				pw.print("\t" + refresh[i]);
			}
			pw.print("\n");
		}
		pw.println("\t\t//");
		// -- done! --

		/*********************** static media block **************************/
		pw.print("\t\tstatic_media");
		double[] staticMedia = fbaWorld.getStaticMediaAmount();
		boolean[] staticSet = fbaWorld.getStaticMediaSet();
		for (int i=0; i<staticMedia.length; i++)
		{
			pw.print("\t");
			if (staticSet[i])
				pw.print("1");
			else
				pw.print("0");
			pw.print("\t" + staticMedia[i]);
		}
		pw.print("\n");
		Iterator<StaticPoint> staticIt = fbaWorld.getStaticMediaSpaces().iterator();
		while (staticIt.hasNext())
		{
			StaticPoint sp = staticIt.next();
			pw.print("\t\t\t" + (int)sp.getX() + "\t" + (int)sp.getY());
			double[] staticSpaceMedia = sp.getMedia();
			boolean[] staticSpaceSet = sp.getStaticSet();
			for (int i=0; i<staticSpaceMedia.length; i++)
			{
				pw.print("\t");
				if (staticSpaceSet[i])
					pw.print("1");
				else
					pw.print("0");
				pw.print("\t" + staticSpaceMedia[i]);
			}
			pw.print("\n");
		}
		pw.println("\t\t//");

		/******************** diffusion constants block ************************/
		pw.println("\t\tdiffusion_constants\t" + pParams.getDefaultDiffusionConstant());
		double[] diffusionConsts = fbaWorld.getMediaDiffusionConstants();
		for (int i=0; i<diffusionConsts.length; i++)
		{
			if (diffusionConsts[i] != pParams.getDefaultDiffusionConstant())
				pw.println("\t\t\t" + i + "\t" + diffusionConsts[i]);
		}
		pw.println("\t\t//");
		// -- done with media! --

		/************************ barrier block *******************************/
		pw.println("\t\tbarrier");
		for (int i=0; i<cParams.getNumCols(); i++)
		{
			for (int j=0; j<cParams.getNumRows(); j++)
			{
				if (fbaWorld.isBarrier(i, j))
					pw.println("\t\t\t" + i + "\t" + j);
			}
		}
		pw.println("\t\t//");


		// --- whew. end of world info. ---
		pw.println("\t//");

		/*************************** initial_pop block *******************************/
		// We have lots of options, based on the layoutSavePanel. So ask it what we should do.

		LayoutSavePanel.BiomassStyle style = layoutSavePanel.getBiomassStyle();
		double[] biomass;
		int[] spaces;
		Rectangle rect;
		switch(style)
		{
		case FILLED:
			pw.print("\tinitial_pop\tfilled");
			biomass = layoutSavePanel.getBiomassQuantity();
			for (int i=0; i<biomass.length; i++)
			{
				pw.print("\t" + biomass[i]);
			}
			break;

		case FILLED_BOX:
			pw.print("\tinitial_pop\tfilled_rect\t");
			biomass = layoutSavePanel.getBiomassQuantity();
			rect = layoutSavePanel.getBiomassRegion();
			pw.print("\t" + rect.getX() + "\t" + rect.getY() + "\t" + rect.getWidth() + "\t" + rect.getHeight());
			for (int i=0; i<biomass.length; i++)
			{
				pw.print("\t" + biomass[i]);
			}
			break;

		case RANDOM:
			pw.print("\tinitial_pop\trandom");
			spaces = layoutSavePanel.getNumSpaces();
			biomass = layoutSavePanel.getBiomassQuantity();
			for (int i=0; i<biomass.length; i++)
			{
				pw.print("\t" + spaces[i] + "\t" + biomass[i]);
			}
			break;

		case RANDOM_BOX:
			pw.print("\tinitial_pop\trandom_rect");
			rect = layoutSavePanel.getBiomassRegion();
			spaces = layoutSavePanel.getNumSpaces();
			biomass = layoutSavePanel.getBiomassQuantity();
			pw.print("\t" + rect.getX() + "\t" + rect.getY() + "\t" + rect.getWidth() + "\t" + rect.getHeight());
			for (int i=0; i<biomass.length; i++)
			{
				pw.print("\t" + spaces[i] + "\t" + biomass[i]);
			}				
			break;

		case SQUARE:
			pw.print("\tinitial_pop\tsquare");
			spaces = layoutSavePanel.getNumSpaces();
			biomass = layoutSavePanel.getBiomassQuantity();
			for (int i=0; i<biomass.length; i++)
			{
				pw.print("\t" + spaces[i] + "\t" + biomass[i]);
			}				
			break;

		default: // save as-is
			pw.print("\tinitial_pop");
			for (Cell cell : cellList)
			{
				biomass = cell.getBiomass();
				pw.print("\n\t\t" + cell.getX() + "\t" + cell.getY());
				for (int i=0; i<biomass.length; i++)
				{
					pw.print("\t" + biomass[i]);
				}
			}
			break;
		}

		pw.println("\n\t//");
		// --- aaaaaand DONE! ---

		pw.println("//");
		pw.close();
		//return null;
	}

	public Model loadModelFromFile(Comets c, String path)
	{
		FBAModel model = null;
		if (pParams == null)
			getPackageParameters(c);

		System.out.println("Loading '" + path + "' ...");
		//2-level testing.
		// first, check to see if the file, as given, is real.
		File f = new File(path);
		// if STILL not found, prompt the user
		if (!f.isFile())
		{
			System.out.println("Error: '" + path + "' is an invalid file.");
			return null;
		}

		try
		{
			model = FBAModel.loadModelFromFile(f.getPath());
			model.setFlowDiffusionConstant(pParams.getFlowDiffRate());
			model.setGrowthDiffusionConstant(pParams.getGrowthDiffRate());
			System.out.println("Done!\n Testing default parameters...");
			int result = model.run();
			System.out.print("Done!\nOptimizer status code = " + result + " ");
			if (result == 180 || result == 5)
				System.out.println("(looks ok!)");
			else
				System.out.println("(might be an error?)");
			System.out.println("objective solution = " + model.getObjectiveSolution());
			System.out.flush();
		}
		catch (ModelFileException e)
		{
			System.out.println("Error in model file '" + path + "': " + e.toString());
		}
		return model;
	}

	public World2D createNewWorld(Comets c, Model[] models)
	{
		if (models.length == 0 || c == null)
			return null;

		FBAModel[] fbaModels = new FBAModel[models.length];
		for (int i=0; i<models.length; i++)
		{
			fbaModels[i] = (FBAModel)models[i];
		}

		/* To make an FBAWorld, we need to cobble together (from the models)
		 * a list of extracellular metabolites. These will be accessible from
		 * models.getExchangeMetaboliteNames().
		 * 
		 * Just whip up a Map of those (Each name is a key. It's on the 
		 * user to make the case corrections and all that. Because F THAT).
		 * 
		 * Simple cheesy trick. (String, Integer) pairs - whenever a duplicate
		 * String is stuffed into the Map, it just overwrites the previously
		 * loaded one. As long as Strings are hashed the same way, and if 
		 * not, then JAVA IS DUMB.
		 * 
		 * I've had way too much coffee.
		 */
		Map<String, Integer> mediaNames = new HashMap<String, Integer>();

		for (int i=0; i<models.length; i++)
		{
			String[] names = fbaModels[i].getExchangeMetaboliteNames();
			for (int j=0; j<names.length; j++)
			{
				mediaNames.put(names[j], new Integer(1));
			}
		}

		// got 'em loaded. package things into a String[] array and an
		// empty double[] array (initial concentrations) and done!

		String[] names = new String[mediaNames.size()];
		double[] concs = new double[mediaNames.size()];

		int i=0;
		Iterator<String> it = mediaNames.keySet().iterator();
		while(it.hasNext())
		{
			names[i] = it.next();
			concs[i] = 0;
			i++;
		}

		return new FBAWorld(c, names, concs, fbaModels);
	}

	public void saveLayoutFile(Comets c) throws IOException
	{
		// User hits "save"
		// Prompt for how to save biomass (and media, later)
		// Saves everything!
		// So, we need:
		//   - a widget to deal with how to save biomass
		//   - a widget to pick a filename
		//   - a way to communicate all that with the FBACometsLoader
		if (layoutSavePanel == null)
			layoutSavePanel = new LayoutSavePanel(c);

		else
			layoutSavePanel.init();

		JFileChooser chooser = new JFileChooser();
		chooser.setAccessory(layoutSavePanel);
		int returnVal = chooser.showSaveDialog(c.getFrame());
		if (returnVal == JFileChooser.APPROVE_OPTION)
		{
			saveLayoutFile(chooser.getSelectedFile().getPath(), c.getWorld(), c.getModels(), c.getCells(), CometsLoader.DO_NOT_SAVE, c.getParameters());
		}
	}

	@Override
	public PackageParameterBatch getParameterBatch(Comets c)
	{
		return new FBAParameterBatch(c);
	}

	public double[][] getExRxnStoich() {
		return exRxnStoich;
	}

	public double[][] getExRxnParams() {
		return exRxnParams;
	}

	public double[] getExRxnRateConstants() {
		return exRxnRateConstants;
	}

	public int[] getExRxnEnzymes() {
		return exRxnEnzymes;
	}

}
