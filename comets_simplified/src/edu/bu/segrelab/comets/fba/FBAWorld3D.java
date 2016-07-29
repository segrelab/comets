package edu.bu.segrelab.comets.fba;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.io.IOException;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.Stack;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import edu.bu.segrelab.comets.Cell;
import edu.bu.segrelab.comets.Comets;
import edu.bu.segrelab.comets.CometsConstants;
import edu.bu.segrelab.comets.Model;
import edu.bu.segrelab.comets.World2D;
import edu.bu.segrelab.comets.World3D;
import edu.bu.segrelab.comets.util.Circle;
import edu.bu.segrelab.comets.util.Utility;

import edu.bu.segrelab.comets.fba.FBAParameters;

import java.io.*;

/**
 * FBAWorld
 * --------
 * The <code>World</code> extension for the FBAComets package. This contains
 * all the information and the runtime logic for running an FBA simulation in 
 * COMETS (which is, well, the core part of COMETS as it'll be published).
 * @author Bill Riehl briehl@bu.edu
 */
public class FBAWorld3D extends World3D 
					  implements CometsConstants
{
	private static int THREAD_COUNT = 0;

	private double[] nutrientDiffConsts;		// The diffusion constants of all media
	private boolean[][][] dirichlet;				// If true, treat space [x][y][z] as a Dirichlet boundary for diffusion
	private boolean[][][][] diffuseBiomassIn;		// If [x][y][z][m] is true, then biomass m can diffuse in at point x,y,z
	private boolean[][][][] diffuseBiomassOut;	// as above, but diffusing out
	private boolean[][][][] diffuseMediaIn;		// as above, but for media
	private boolean[][][][] diffuseMediaOut;
	private int threadLock;						// while threaded FBA is running, this = number of cells remaining

	private FBAParameters pParams;
	private List<Cell> deadCells;				// list of cells to be removed at the end of a run
	private Stack<Cell> runCells;				// stack of cells yet to be run by threads

	private long currentTimePoint;				// current simulation time cycle
	private FBAModel[] models;					// FBA models in this system
	
	private List<int[]> modelExchList;			// indices of the exchange reactions for each model
	//private SpaceInfoPanel infoPanel;			// the info panel for the spaces in the world
												// see the SpaceInfoPanel inner class below

	private FBARunThread3D[] runThreads;			// array of run threads
	private ThreadGroup threadGroup;			// thread group that they all belong to

	private PrintWriter mediaLogWriter,	
						fluxLogWriter,
						biomassLogWriter,
						totalBiomassLogWriter;
	
	private Set<Circle> circleSet;

	private double defaultDiffConst;
	
	/**
	 * Initialize a new, empty world, tied the current <code>Comets</code> with a given
	 * number of medium components
	 * @param c
	 * @param numMedia
	 */
	public FBAWorld3D(Comets c, int numMedia)
	{
		super(c, numMedia);
	}
	
	/**
	 * Initialize a new FBAWorld connected to the given <code>Comets</code> class with 
	 * initial global media and their names, and associated FBAModels
	 * @param c
	 * @param mediaNames names of each initial medium component (should match the
	 * extracellular metabolite names of each FBAModel)
	 * @param startingMedia amount of media to start in each space
	 * @param models models to apply to the world
	 */
	public FBAWorld3D(Comets c, String[] mediaNames, double[] startingMedia,
					Model[] models)
	{
		this(c, startingMedia.length);
		pParams = (FBAParameters)c.getPackageParameters();
//		this.models = models;
		numModels = models.length;
		this.models = new FBAModel[numModels];
		for (int i=0; i<numModels; i++)
		{
			this.models[i] = (FBAModel)models[i];
		}
		dirichlet = new boolean[numCols][numRows][numLayers];
		diffuseMediaIn = new boolean[numCols][numRows][numLayers][numMedia];
		diffuseMediaOut = new boolean[numCols][numRows][numLayers][numMedia];
		diffuseBiomassIn = new boolean[numCols][numRows][numLayers][numModels];
		diffuseBiomassOut = new boolean[numCols][numRows][numLayers][numModels];
		nutrientDiffConsts = new double[numMedia];
		/*
		 * Initialize everything so that it can diffuse everywhere,
		 * and the startingMedia is uniform across the grid.
		 */
		for (int i = 0; i < numCols; i++)
		{
			for (int j = 0; j < numRows; j++)
			{
				for(int l = 0; l < numLayers; l++)
				{
					for (int k = 0; k < numMedia; k++)
					{
						media[i][j][l][k] = startingMedia[k];
						diffuseMediaIn[i][j][l][k] = true;
						diffuseMediaOut[i][j][l][k] = true;
					}
					for (int k = 0; k < numModels; k++)
					{
						diffuseBiomassIn[i][j][l][k] = true;
						diffuseBiomassOut[i][j][l][k] = true;
					}
				}
			}
		}
		defaultDiffConst = pParams.getDefaultDiffusionConstant();
		for (int i = 0; i < numMedia; i++)
		{
			nutrientDiffConsts[i] = defaultDiffConst;
		}
		this.mediaNames = mediaNames;

		runCells = new Stack<Cell>();
		circleSet = null;

		// applies all models to the world - puts names, etc, in the right order,
		// and sets media diffusion constants where appropriate
		changeModelsInWorld(models, models);
		threadLock = 0;
	}
	
	public void setDefaultMediaDiffusionConstant(double defaultDiffConst)
	{
		for (int i=0; i<nutrientDiffConsts.length; i++)
		{
			if (nutrientDiffConsts[i] == this.defaultDiffConst)
				nutrientDiffConsts[i] = defaultDiffConst;
		}
		this.defaultDiffConst = defaultDiffConst;
	}

	/**
	 * Initializes the simulation by preparing log files to be written and doing any other
	 * pre-set work. If any log file cannot be written to (e.g., the file cannot be opened), 
	 * an error message is written, and that file is ignored).
	 */
	public void initSimulation()
	{
		System.out.print("medialist");
		for (int i=0; i<mediaNames.length; i++)
		{
			System.out.print("\t" + mediaNames[i]);
		}
		System.out.println();
		
		
		currentTimePoint = 0;
		DateFormat df = new SimpleDateFormat("_yyyyMMddHHmmss");
		String timeStamp = df.format(new Date()); 
		
		// Init Flux log and write the first line
		if (pParams.writeFluxLog())
		{
			String name = adjustLogFileName(pParams.getFluxLogName(), timeStamp);
			try
			{
				fluxLogWriter = new PrintWriter(new FileWriter(new File(name)));
				writeFluxLog();
				//Write the file name in the manifest file.
				try
				{
					FileWriter manifestWriter=new FileWriter(new File(pParams.getManifestFileName()),true);
					manifestWriter.write("FluxFileName: "+name+System.getProperty("line.separator"));
					manifestWriter.close();
				}
				catch (IOException e)
				{
					System.out.println("Unable to initialize manifest file. \nContinuing without writing manifest file.");
				}		
			}
			catch (IOException e)
			{
				System.out.println("Unable to initialize flux log file '" + name + "'\nContinuing without saving log.");
				fluxLogWriter = null;
			}
		}

		// Init media log and write the first line
		if (pParams.writeMediaLog())
		{
			String name = adjustLogFileName(pParams.getMediaLogName(), timeStamp);
			try
			{
				mediaLogWriter = new PrintWriter(new FileWriter(new File(name)));
				
				// init the media log writer.
				mediaLogWriter.print("media_names = { '" + mediaNames[0] + "'");
				for (int i=1; i<mediaNames.length; i++)
				{
					mediaLogWriter.print(", '" + mediaNames[i] + "'");
				}
				mediaLogWriter.println("};");
				writeMediaLog();
				//Write the file name in the manifest file.
				try
				{
					FileWriter manifestWriter=new FileWriter(new File(pParams.getManifestFileName()),true);
					manifestWriter.write("MediaFileName: "+name+System.getProperty("line.separator"));
					manifestWriter.close();
				}
				catch (IOException e)
				{
					System.out.println("Unable to initialize manifest file. \nContinuing without writing manifest file.");
				}
			}
			catch (IOException e)
			{
				System.out.println("Unable to initialize media log file '" + name + "'\nContinuing without saving log.");
				mediaLogWriter = null;
			}
		}
		
		// Init biomass log and write the first line
		if (pParams.writeBiomassLog())
		{
			String name = adjustLogFileName(pParams.getBiomassLogName(), timeStamp);
			try
			{
				biomassLogWriter = new PrintWriter(new FileWriter(new File(name)));
				writeBiomassLog();
				//Write the file name in the manifest file.
				try
				{
					FileWriter manifestWriter=new FileWriter(new File(pParams.getManifestFileName()),true);
					manifestWriter.write("BiomassFileName: "+name+System.getProperty("line.separator"));
					manifestWriter.close();
				}
				catch (IOException e)
				{
					System.out.println("Unable to initialize manifest file. \nContinuing without writing manifest file.");
				}
			}
			catch (IOException e)
			{
				System.out.println("Unable to initialize biomass log file '" + name + "'\nContinuing without saving log.");
				biomassLogWriter = null;
			}
		}
		
		// Init the total biomass log and write the first line
		if (pParams.writeTotalBiomassLog())
		{
			String name = adjustLogFileName(pParams.getTotalBiomassLogName(), timeStamp);
			try
			{
				totalBiomassLogWriter = new PrintWriter(new FileWriter(new File(name)));
				writeTotalBiomassLog();
				//Write the file name in the manifest file.
				try
				{
					FileWriter manifestWriter=new FileWriter(new File(pParams.getManifestFileName()),true);
					manifestWriter.write("TotalBiomassFileName: "+name+System.getProperty("line.separator"));
					manifestWriter.close();
				}
				catch (IOException e)
				{
					System.out.println("Unable to initialize manifest file. \nContinuing without writing manifest file.");
				}
			}
			catch (IOException e)
			{
				System.out.println("Unable to initialize total biomass log file '" + name + "'\nContinuing without saving log.");
				totalBiomassLogWriter = null;
			}
		}
	}
	
	/**
	 * Does cleanup work at the end of the simulation. Closes log files, etc.
	 */
	public void endSimulation()
	{
		if (fluxLogWriter != null)
		{
			fluxLogWriter.flush();
			fluxLogWriter.close();
		}
		if (mediaLogWriter != null)
		{
			mediaLogWriter.flush();
			mediaLogWriter.close();
		}
		if (biomassLogWriter != null)
		{
			biomassLogWriter.flush();
			biomassLogWriter.close();
		}
	}
	
	/**
	 * Adjusts the name of a log file to include a time stamp before the file 
	 * extension.
	 * @param name the log file name
	 * @param timeStamp the time stamp string to use as a suffix
	 * @return
	 */
	private String adjustLogFileName(String name, String timeStamp)
	{
		if (pParams.useLogNameTimeStamp())
		{
			/* 
			 * the time stamp should go between the name and the suffix.
			 * e.g. "flux_log.txt" should become "flux_log_20100719125503.txt"
			 */
			int idx = name.lastIndexOf('.');
			if (idx == -1)
				name += timeStamp;
			else
				name = name.substring(0, idx) + timeStamp + name.substring(idx);
		}
		return name;
	}

	/**
	 * Initializes FBARunThreads for doing lots of FBA runs in parallel.
	 */
	public void initRunThreads()
	{
		// if we're just doing one, do it in the main simulation thread
		if (pParams.getNumRunThreads() > 1) 
		{
			/*
			 * if we already have threads built and running, and we have a NEW
			 * number of threads to build, then join() all the currently running
			 * threads and re-initialize the thread array.
			 */
			killRunThreads();
			threadGroup = new ThreadGroup("FBARunThreadGroup");
			runThreads = new FBARunThread3D[pParams.getNumRunThreads()];
			for (int i = 0; i < runThreads.length; i++)
			{
				runThreads[i] = new FBARunThread3D(this, threadGroup, cParams, models);
				runThreads[i].setName("FBARunThread-" + THREAD_COUNT++);
			}
			for (int i = 0; i < runThreads.length; i++)
			{
				runThreads[i].start();
			}
		}
	}

	/**
	 * On this class's death, make sure to get rid of any excess objects that might
	 * not get consumed by Java's garbage collector. Like any running FBA threads, 
	 * if necessary.
	 */
	public void destroy()
	{
		killRunThreads();
	}

	/**
	 * Tells all run threads to stop what they're doing and expire.
	 */
	public void killRunThreads()
	{
		if (runThreads != null)
		{
			synchronized (runCells)
			{
				for (int i = 0; i < runThreads.length; i++)
				{
					runThreads[i].die();
				}
				runCells.notifyAll();
			}
		}
	}

	/**
	 * If models are updated (e.g. added or removed), then the FBAWorld needs to
	 * know about it. Running this allows the FBAWorld to add or remove layers of
	 * media, make sure all the medium elements are in the right order, and are all
	 * correctly linked to their FBAModels.
	 * @param oldModels the array of models that is expiring
	 * @param newModels the new array of models that's replacing them
	 */
	public void changeModelsInWorld(Model[] oldModels, Model[] newModels)
	{
//		System.out.println("changing models in world");
		/*
		 * How to go about this: 
		 * 1. Get the exchange metab names of everything.
		 * 2. Compare with the list of what's known now (exchMetabNames) 
		 * 3a. If there's more in exchMetabNames, then we have to remove some. Mark the
		 *     right media layers for removal, make a new Media set without those.
		 * 3b. If there's fewer in exchMetabNames, then we have to add some.
		 *     Slap them on the end, I guess. 
		 * 4. run synchronizeWithModels() to fix all the index references.
		 */

		Map<String, Integer> mediaNamesMap = new HashMap<String, Integer>();
		Map<String, Double> newDiffConsts = new HashMap<String, Double>();
		for (int i = 0; i < newModels.length; i++)
		{
			String[] names = ((FBAModel) newModels[i]).getExchangeMetaboliteNames();
			if (DEBUG) System.out.println(names.length + " metabs in model " + i);
			double[] diffConsts = ((FBAModel)newModels[i]).getExchangeMetaboliteDiffusionConstants();
			for (int j = 0; j < names.length; j++)
			{
				mediaNamesMap.put(names[j], new Integer(1));
				newDiffConsts.put(names[j], diffConsts[j]);
			}
		}
		if (DEBUG) System.out.println(mediaNamesMap.size() + " total nutrients");
		
		/*
		 * Now, figure out what stays and what goes. For each of the current
		 * metab names, look it up in the map. If it's there, keen. Remove it
		 * from the map. If it's NOT there, make a note to remove that row. At
		 * the end, if there's elements remaining in the map, those have to go
		 * in the media list.
		 * 
		 * More notes after the first part.
		 */
		String[] newMetabNames = new String[mediaNamesMap.size()];
		List<Integer> removedMetabs = new ArrayList<Integer>();

		int j = 0;
		for (int i = 0; i < mediaNames.length; i++)
		{
			// if it's there, don't do anything. just keep the order
			// and put it in the new metab list
			if (mediaNamesMap.containsKey(mediaNames[i]))
			{
				newMetabNames[j] = mediaNames[i];
				j++;
				mediaNamesMap.remove(mediaNames[i]);
			}
			// if it's not present (i.e. - has been removed), then
			// add the index to the removedMetabs list
			else
			{
				removedMetabs.add(new Integer(i));
			}
		}
		// if there's any remaining, then put them all at the end of
		// newMetabNames in any order (we'll sort later)
		if (mediaNamesMap.size() > 0)
		{
//			firstNewMetab = j;
			Iterator<String> it = mediaNamesMap.keySet().iterator();
			while (it.hasNext())
			{
				newMetabNames[j] = it.next();
				j++;
			}
		}

		/*
		 * Now the finish. 1. Sort newMetabNames
		 */
		Arrays.sort(newMetabNames);
		double[][][][] newMedia = new double[numCols][numRows][numLayers][newMetabNames.length];
		boolean[][][][] newDiffMediaIn = new boolean[numCols][numRows][numLayers][newMetabNames.length];
		boolean[][][][] newDiffMediaOut = new boolean[numCols][numRows][numLayers][newMetabNames.length];
		double[] newMediaRefresh = new double[newMetabNames.length];
		double[] newStaticMedia = new double[newMetabNames.length];
		boolean[] newIsStatic = new boolean[newMetabNames.length];

		// init everything to zeros and true.
		for (int x = 0; x < numCols; x++)
		{
			for (int y = 0; y < numRows; y++)
			{
				for (int l = 0; l < numLayers; l++)
				{
					for (int z = 0; z < newMetabNames.length; z++)
					{
						newMedia[x][y][l][z] = 0;
						newDiffMediaIn[x][y][l][z] = true;
						newDiffMediaOut[x][y][l][z] = true;
					}
				}
			}
		}

		/*
		 * Now, find where all the old media components should be in the new
		 * indexing way (e.g., find the index of each exchMetabName in
		 * newMetabNames) and set that layer to be the equal to the old media
		 * values
		 */
		int[] newMediaIndices = new int[newMetabNames.length];
		for (int k=0; k<newMetabNames.length; k++)
			newMediaIndices[k] = -1;
		for (int k=0; k<mediaNames.length; k++)
		{
			int idx = Arrays.binarySearch(newMetabNames, mediaNames[k]);
			if (idx >= 0)
			{
				if (DEBUG)
					System.out.println(mediaNames[k] + " from " + k + " -> " + idx);
				newMediaIndices[k] = idx;
			}
		}
		
		/* newMediaIndices contains the translation indices for each media component.
		 * So, the value of each element i is the new position for the old elements.
		 * The rest should be unchanged.
		 * For example.
		 * if newMediaIndices[5] = 9, then what was in position 9 in the old set, is now
		 * in position 5.
		 * if newMediaIndices[i] = -1, then what was in position i before is still there and
		 * remains unchanged.
		 */
		for (int x = 0; x < numCols; x++)
		{
			for (int y = 0; y < numRows; y++)
			{
				for (int z = 0; z < numLayers; z++)
				{
					for (int k = 0; k < newMediaIndices.length; k++)
					{
						if (newMediaIndices[k] != -1)
						{
							newMedia[x][y][z][newMediaIndices[k]] = media[x][y][z][k];
							newDiffMediaIn[x][y][z][newMediaIndices[k]] = diffuseMediaIn[x][y][z][k];
							newDiffMediaOut[x][y][z][newMediaIndices[k]] = diffuseMediaOut[x][y][z][k];						
						}
					}
					if (staticPoints[x][y][z] != null)
					{
						double[] newStaticPointMedia = new double[newMetabNames.length];
						boolean[] newStaticPointSet = new boolean[newMetabNames.length]; //default = false
						double[] curStaticPointMedia = staticPoints[x][y][z].getMedia();
						boolean[] curStaticPointSet = staticPoints[x][y][z].getStaticSet();
						for (int k = 0; k < newMediaIndices.length; k++)
						{
							if (newMediaIndices[k] != -1)
							{
								newStaticPointMedia[newMediaIndices[k]] = curStaticPointMedia[k];
								newStaticPointSet[newMediaIndices[k]] = curStaticPointSet[k];
							}
						}
						staticPoints[x][y][z].setValues(newStaticPointMedia, newStaticPointSet);
					}
					if (refreshPoints[x][y][z] != null)
					{
						double[] newRefreshPointMedia = new double[newMetabNames.length];
						double[] curRefreshPointMedia = refreshPoints[x][y][z].getMediaRefresh();
						for (int k = 0; k < newMediaIndices.length; k++)
						{
							if (newMediaIndices[k] != -1)
								newRefreshPointMedia[newMediaIndices[k]] = curRefreshPointMedia[k];
						}
						refreshPoints[x][y][z].setValues(newRefreshPointMedia);
					}
				}
			}
		}

		// Apply the new media indices to the order of the global refresh and static media
		for (int k = 0; k < newMediaIndices.length; k++)
		{
			if (newMediaIndices[k] != -1)
			{
				newMediaRefresh[newMediaIndices[k]] = mediaRefresh[k];
				newStaticMedia[newMediaIndices[k]] = staticMedia[k];
				newIsStatic[newMediaIndices[k]] = isStatic[k];
			}
		}

		// Apply the new model orders to the diffusion permissions
		boolean[][][][] newDiffBiomassIn = new boolean[numCols][numRows][numLayers][newModels.length];
		boolean[][][][] newDiffBiomassOut = new boolean[numCols][numRows][numLayers][newModels.length];
		// init everything to true.
		for (int x = 0; x < numCols; x++)
		{
			for (int y = 0; y < numRows; y++)
			{
				for (int z = 0; z < numLayers; z++)
				{
					for (int k = 0; k < newModels.length; k++)
					{
						newDiffBiomassIn[x][y][z][k] = true;
						newDiffBiomassOut[x][y][z][k] = true;
					}
				}
			}
		}
		
		// Make the changes
		for (int k = 0; k < oldModels.length; k++)
		{
			int idx = -1;
			for (int z = 0; z < oldModels.length; z++)
			{
				if (newModels[z].equals(oldModels[k]))
					idx = z;
			}
			if (idx != -1) // found it!
			{
				for (int x = 0; x < numCols; x++)
				{
					for (int y = 0; y < numRows; y++)
					{
						for(int z = 0; z < numLayers; z++)
						{
						newDiffBiomassIn[x][y][z][idx] = diffuseBiomassIn[x][y][z][k];
						newDiffBiomassOut[x][y][z][idx] = diffuseBiomassOut[x][y][z][k];
						}
					}
				}
			}
		}

		// now, final housekeeping and variable setting
		models = new FBAModel[newModels.length];
		for (int i = 0; i < newModels.length; i++)
		{
			models[i] = (FBAModel)newModels[i];
		}
		mediaNames = newMetabNames;
		media = newMedia;
		isStatic = newIsStatic;
		mediaRefresh = newMediaRefresh;
		staticMedia = newStaticMedia;
		diffuseMediaIn = newDiffMediaIn;
		diffuseMediaOut = newDiffMediaOut;
		diffuseBiomassIn = newDiffBiomassIn;
		diffuseBiomassOut = newDiffBiomassOut;
		numMedia = mediaNames.length;
		numModels = newModels.length;

		synchronizeWithModels();
	}

	private void synchronizeWithModels()
	{
		synchronizeWithModels(models);
	}

	/**
	 * Synchronizes the World with the set of Models by making sure all of the media names
	 * are in the right order, and that all of the exchange metabolites are named and
	 * labeled correctly.
	 * @param models
	 */
	private void synchronizeWithModels(Model[] models)
	{
		// 1. Make up an "array" of integers, indexed by each exchange
		// metabolite
		Map<String, Integer> metabMap = new HashMap<String, Integer>();
		for (int i = 0; i < mediaNames.length; i++)
		{
			metabMap.put(mediaNames[i], new Integer(i));    
			// a way to quickly look up strings.
		}

		/*
		 * 2. modelExchList = an ArrayList of arrays of exchange metabolite
		 * indices. Each model gets its own array, where each member of that
		 * array is (in order) the index in this World of which metabolite
		 * corresponds to each exchange reaction that model has.
		 * 
		 * In short, it's a way to map each model's exchange metabs to the right
		 * media indices.
		 * 
		 * and,
		 * 3. nutrientDiffConsts = a double[] of diffusion constants for all
		 * extracellular metabolites. This has the same indices as exchMetabNames, 
		 * so it's parallel with both that array and the media[][][] matrix.
		 */
		numModels = models.length;
		
		modelExchList = new ArrayList<int[]>();
		Map<String, Double> allExchMetabMap = new HashMap<String, Double>();
		for (int i = 0; i < numModels; i++)
		{
			int[] exchIdx = ((FBAModel)models[i]).getExchangeIndices();
			String[] exchMetabs = ((FBAModel)models[i]).getExchangeMetaboliteNames();
			double[] diffConsts = ((FBAModel)models[i]).getExchangeMetaboliteDiffusionConstants();
			int[] worldExchIdx = new int[exchIdx.length];
			for (int j = 0; j < exchMetabs.length; j++)
			{
				worldExchIdx[j] = ((Integer) metabMap.get(exchMetabs[j])).intValue();
				allExchMetabMap.put(exchMetabs[j], new Double(diffConsts[j]));
			}
			modelExchList.add(worldExchIdx);
		}
		
//		nutrientDiffConsts = new double[numMedia];
//		for (int i = 0; i < mediaNames.length; i++)
//		{
//			nutrientDiffConsts[i] = allExchMetabMap.get(mediaNames[i]).doubleValue();
//		}
	}

	public double[] getMediaDiffusionConstants()
	{
		return nutrientDiffConsts;
	}
	
	/**
	 * Returns an array of Points adjacent to the space (x, y). That is, this
	 * should return all points around (x, y).
	 * 
	 * @param x
	 * @param y
	 * @return an array of <code>Point</code>s.
	 */
	public Point[] getAllAdjacentSpaces(int x, int y, int z)
	{
		return getSpacesAround(x, y, z, ANY_SPACE);
	}

	/**
	 * Returns an array of <code>Points</code> around (x, y) that do not have
	 * any cells present. Note that this might be an empty array if there's
	 * <code>Cells</code> all around.
	 * 
	 * @param x
	 * @param y
	 * @return a possibly empty array of <code>Points</code>
	 */
	public Point[] getEmptyAdjacentSpaces(int x, int y, int z)
	{
		return getSpacesAround(x, y, z, EMPTY_SPACE);
	}

	/**
	 * Returns an array of <code>Points</code> around (x, y) that have cells
	 * present.
	 * 
	 * @param x
	 * @param y
	 * @return a possibly empty array of <code>Points</code>
	 */
	public Point[] getFilledAdjacentSpaces(int x, int y, int z)
	{
		return getSpacesAround(x, y, z, FILLED_SPACE);
	}

	/**
	 * Returns an array of (up to) all 27 <code>Points</code> around (x, y, z) that
	 * are of a certain type
	 * @param x
	 * @param y
	 * @param z
	 * @param type either FILLED_SPACE, EMPTY_SPACE, or the agnostic ANY_SPACE
	 * @return an array of <code>Points</code> (which may be empty)
	 */
	private Point[] getSpacesAround(int x, int y, int z, int type)
	{
		x = adjustX(x);
		y = adjustY(y);
		z = adjustZ(z);
		// There's up to 8, so start with that many.
		Point[] pArr = new Point[27];
		int numPoints = 0;

		for (int i = x - 1; i <= x + 1; i++)
		{
			for (int j = y - 1; j <= y + 1; j++)
			{
				for (int k = z - 1; k <= z + 1; k++)
				{
					int adjX = adjustX(i);
					int adjY = adjustY(j);
					int adjZ = adjustZ(k);
					// the point (adjX, adjY) is adjacent to (x,y).
					// complex ugly if-statement to see if this is one we want goes
					// here:
					if ((adjX != x || adjY != y || adjZ != z)
							&& ((type == EMPTY_SPACE && cellGrid[adjX][adjY][adjZ] == null)
									|| (type == FILLED_SPACE && cellGrid[adjX][adjY][adjZ] != null) || (type == ANY_SPACE)))
					{
						pArr[numPoints] = new Point(adjX, adjY);
						numPoints++;
					}
				}
			}
		}

		// reduce the array so that we only send back an array with
		// the required point set.
		// ---- alternately, use a Set of some sort to do this and make an array
		// from it.
		// TODO: test to see if this would be catastrophically slower.

		Point[] retArr = new Point[numPoints];
		for (int i = 0; i < numPoints; i++)
			retArr[i] = pArr[i];

		return retArr;
	}

	/**
	 * Removes a cell from the FBAWorld at point(x, y) and returns it to the user.
	 * @param x
	 * @param y
	 * @return
	 */
	public Cell removeCell(int x, int y, int z)
	{
		Cell rem = super.removeCell(x, y, z );
		for (int i = 0; i < numModels; i++)
		{
			diffuseBiomassIn[x][y][z][i] = true;
			diffuseBiomassOut[x][y][z][i] = true;
		}
		return rem;
	}

	/**
	 * Sets the space at (x, y) to have (or not) a barrier
	 * @param x
	 * @param y
	 * @param b if true, set a barrier at (x, y), if not remove it
	 */
	public int setBarrier(int x, int y, int z, boolean b)
	{
		int ret = super.setBarrier(x, y, z, b);
		if (ret == PARAMS_OK)
		{
			for (int i = 0; i < numMedia; i++)
			{
				diffuseMediaIn[x][y][z][i] = !b;
				diffuseMediaOut[x][y][z][i] = !b;
			}
			for (int i = 0; i < numModels; i++)
			{
				diffuseBiomassIn[x][y][z][i] = !b;
				diffuseBiomassOut[x][y][z][i] = !b;
			}
		}
		return ret;
	}

	/**
	 * Returns true if the given media component index can diffuse into the space
	 * at (x, y). If the World is not modeled as a torus and (x, y) is out of range, 
	 * false is returned.
	 * @param x
	 * @param y
	 * @param media
	 * @return true if that media component can diffuse in
	 */
	public boolean canDiffuseMediaIn(int x, int y, int z, int media)
	{
		if (cParams.isToroidalGrid())
		{
			x = adjustX(x);
			y = adjustY(y);
			z = adjustZ(z);
		} 
		else if (media < 0 || media >= numMedia || !isOnGrid(x, y, z))
			return false;
		return diffuseMediaIn[x][y][z][media];
	}

	/**
	 * Returns true if the given media component index can diffuse out of the space
	 * at (x, y). If the World is not modeled as a torus and (x, y) is out of range, 
	 * false is returned.
	 * @param x
	 * @param y
	 * @param media
	 * @return true if that media component can diffuse out
	 */
	public boolean canDiffuseMediaOut(int x, int y, int z, int media)
	{
		if (cParams.isToroidalGrid())
		{
			x = adjustX(x);
			y = adjustY(y);
		} else if (media < 0 || media >= numMedia || !isOnGrid(x, y, z))
			return false;
		return diffuseMediaOut[x][y][z][media];
	}

	/**
	 * Returns true if biomass of the given species index can diffuse into the space
	 * at (x, y). If the World is not modeled as a torus and (x, y) is out of range, 
	 * false is returned.
	 * @param x
	 * @param y
	 * @param species
	 * @return true if biomass of that species can diffuse in
	 */
	public boolean canDiffuseBiomassIn(int x, int y, int z, int species)
	{
		if (cParams.isToroidalGrid())
		{
			x = adjustX(x);
			y = adjustY(y);
		} else if (species < 0 || species >= numModels || !isOnGrid(x, y, z))
			return false;
		return diffuseBiomassIn[x][y][z][species];
	}

	/**
	 * Returns true if biomass of the given species index can diffuse out of the space
	 * at (x, y). If the World is not modeled as a torus and (x, y) is out of range, 
	 * false is returned.
	 * @param x
	 * @param y
	 * @param species
	 * @return true if biomass of that species can diffuse out
	 */
	public boolean canDiffuseBiomassOut(int x, int y, int z, int species)
	{
		if (cParams.isToroidalGrid())
		{
			x = adjustX(x);
			y = adjustY(y);
		} else if (species < 0 || species >= numModels || !isOnGrid(x, y, z))
			return false;
		return diffuseBiomassOut[x][y][z][species];
	}

	/**
	 * Sets the ability for biomass from the given model to diffuse out of the space
	 * given as (x, y). If the World is not modeled as a torus and (x, y) is out of range, 
	 * PARAMS_ERROR is returned
	 * @param x
	 * @param y
	 * @param model the index of the model to adjust
	 * @param b true if biomass of that model should be able to diffuse out
	 * @return PARAMS_OK or PARAMS_ERROR if (x, y) is out of range or model is out of range
	 */
	public int setDiffuseBiomassOut(int x, int y, int z, int model, boolean b)
	{
		if (cParams.isToroidalGrid())
		{
			x = adjustX(x);
			y = adjustY(y);
		} else if (model < 0 || model >= numModels || !isOnGrid(x, y, z))
			return BOUNDS_ERROR;
		diffuseBiomassOut[x][y][z][model] = b;
		return PARAMS_OK;
	}

	/**
	 * Sets the ability for biomass from the given model to diffuse in to the space
	 * given as (x, y). If the World is not modeled as a torus and (x, y) is out of range, 
	 * PARAMS_ERROR is returned
	 * @param x
	 * @param y
	 * @param model the index of the model to adjust
	 * @param b true if biomass of that model should be able to diffuse in
	 * @return PARAMS_OK or PARAMS_ERROR if (x, y) is out of range or model is out of range
	 */
	public int setDiffuseBiomassIn(int x, int y, int z, int model, boolean b)
	{
		if (cParams.isToroidalGrid())
		{
			x = adjustX(x);
			y = adjustY(y);
			z = adjustZ(z);
		} else if (model < 0 || model >= numModels || !isOnGrid(x, y, z))
			return BOUNDS_ERROR;
		diffuseBiomassIn[x][y][z][model] = b;
		return PARAMS_OK;
	}

	/**
	 * Sets the ability for media from the given index to diffuse out of the space
	 * given as (x, y). If the World is not modeled as a torus and (x, y) is out of range, 
	 * PARAMS_ERROR is returned
	 * @param x
	 * @param y
	 * @param media the index of the media component to adjust
	 * @param b true if media from that index should be able to diffuse out
	 * @return PARAMS_OK or PARAMS_ERROR if (x, y) is out of range or model is out of range
	 */
	public int setDiffuseMediaOut(int x, int y, int z, int media, boolean b)
	{
		if (cParams.isToroidalGrid())
		{
			x = adjustX(x);
			y = adjustY(y);
			z = adjustZ(z);
		} else if (media < 0 || media >= numMedia || !isOnGrid(x, y, z))
			return BOUNDS_ERROR;
		diffuseMediaOut[x][y][z][media] = b;
		return PARAMS_OK;
	}

	/**
	 * Sets the ability for media from the given index to diffuse into the space
	 * given as (x, y). If the World is not modeled as a torus and (x, y) is out of range, 
	 * PARAMS_ERROR is returned
	 * @param x
	 * @param y
	 * @param media the index of the media component to adjust
	 * @param b true if media from that index should be able to diffuse in
	 * @return PARAMS_OK or PARAMS_ERROR if (x, y) is out of range or model is out of range
	 */
	public int setDiffuseMediaIn(int x, int y, int z, int i, boolean b)
	{
		if (cParams.isToroidalGrid())
		{
			x = adjustX(x);
			y = adjustY(y);
			z = adjustZ(z);
		}
		if (isOnGrid(x, y, z))
		{
			diffuseMediaIn[x][y][z][i] = b;
			return PARAMS_OK;
		}
		else
			return BOUNDS_ERROR;
	}

	/**
	 * Returns the levels of media in position (x, y) that are extracellular metabolites
	 * for <code>FBAModel</code> i. These are reordered to be appropriate to the exchange fluxes for that
	 * model as well.
	 * @param x
	 * @param y
	 * @param i
	 * @return
	 */
	public synchronized double[] getModelMediaAt(int x, int y, int z, int i)
	{
		if (cParams.isToroidalGrid())
		{
			x = adjustX(x);
			y = adjustY(y);
			z = adjustZ(z);
		} 
		if (isOnGrid(x, y, z))
		{
			int[] mediaList = (int[])modelExchList.get(i);
			double[] modelMedia = new double[mediaList.length];
			for (int j = 0; j < modelMedia.length; j++)
			{
				modelMedia[j] = media[x][y][z][mediaList[j]];
			}
			return modelMedia;			
		}
		else
			return null;
	}

	/**
	 * Changes the level of media in space (x, y) as modified by the given model. The
	 * mediaDelta array should hava only the media change calculated by that FBAModel,
	 * and not necessarily all medium components.
	 * @param x
	 * @param y
	 * @param model
	 * @param mediaDelta
	 * @return
	 */
	public synchronized int changeModelMedia(int x, int y, int z, int model,
			double[] mediaDelta)
	{
		if (model < 0 || model > numModels - 1)
			return PARAMS_ERROR;
		int[] mediaList = (int[]) modelExchList.get(model);
		if (mediaList.length != mediaDelta.length)
			return PARAMS_ERROR;
		if (cParams.isToroidalGrid())
		{
			x = adjustX(x);
			y = adjustY(y);
			z = adjustZ(z);
		} 
		if (isOnGrid(x, y, z))
		{
			for (int i = 0; i < mediaList.length; i++)
			{
				media[x][y][z][mediaList[i]] += mediaDelta[i];
				if (media[x][y][z][mediaList[i]] < 0)
					media[x][y][z][mediaList[i]] = 0;
			}
			return PARAMS_OK;			
		}
		else
			return BOUNDS_ERROR;
	}

	@Override
	/**
	 * Updates the world whenever a size (number of columns or rows) occurs. Any new
	 * spaces are initialized with default values - no biomass, no Dirichlet boundaries, 
	 * and everything can be freely diffused.
	 * @return PARAMS_OK once its done.
	 */
	public int updateWorld()
	{
		// Covers most everything, except for the diffusion parameters and the infoPanel.
		// So we just compare the NEW numRows and numCols to the size of those arrays, and
		// make updates as necessary.
		super.updateWorld();
		if (numCols != dirichlet.length || numRows != dirichlet[0].length)
		{
			// update all the parameters that deal with the grid.
			// this'll be fun.
			int oldNumCols = dirichlet.length;
			int oldNumRows = dirichlet[0].length;
			int oldNumLayers = dirichlet[1].length;
			
			boolean[][][] newDirichlet = new boolean[numCols][numRows][numLayers];
			boolean[][][][] newDiffuseBiomassIn = new boolean[numCols][numRows][numLayers][numModels];
			boolean[][][][] newDiffuseBiomassOut = new boolean[numCols][numRows][numLayers][numModels];
			boolean[][][][] newDiffuseMediaIn = new boolean[numCols][numRows][numLayers][numMedia];
			boolean[][][][] newDiffuseMediaOut = new boolean[numCols][numRows][numLayers][numMedia];

			int minRows = Math.min(numRows, oldNumRows);
			int minCols = Math.min(numCols, oldNumCols);
			int minLayers= Math.min(numLayers, oldNumLayers);
			for (int i=0; i<minCols; i++)
			{
				for (int j=0; j<minRows; j++)
				{
					for (int l = 0; l < minLayers; l++)
					{
						newDirichlet[i][j][l] = dirichlet[i][j][l];
						for (int k=0; k<numModels; k++)
						{
							newDiffuseBiomassIn[i][j][l][k] = diffuseBiomassIn[i][j][l][k];
							newDiffuseBiomassOut[i][j][l][k] = diffuseBiomassOut[i][j][l][k];
						}
						for (int k=0; k<numMedia; k++)
						{
							newDiffuseMediaIn[i][j][l][k] = diffuseMediaIn[i][j][l][k];
							newDiffuseMediaOut[i][j][l][k] = diffuseMediaOut[i][j][l][k];
						}
					}
				}
			}
			
			dirichlet = newDirichlet;
			diffuseBiomassIn = newDiffuseBiomassIn;
			diffuseBiomassOut = newDiffuseBiomassOut;
			diffuseMediaIn = newDiffuseMediaIn;
			diffuseMediaOut = newDiffuseMediaOut;
		}
//		if (infoPanel != null)
//		{
//			infoPanel.rebuildInfoPanel(c.getModelNames(), mediaNames, this);
//			updateInfoPanel();
//		}
		return PARAMS_OK;
	}

	/**
	 * Diffuses media according to Fick's second law in the 2D system. Each media layer
	 * is diffused separately, by calling Utility.diffusionFick() on each one in turn.
	 */
	private void diffuseMediaFick()
	{
		if (pParams.getNumDiffusionsPerStep() == 0)
			return;
		
//		long time = System.currentTimeMillis();
		/**
		 * Foreach of the media types, copy each media layer into a
		 * new double[][], pipe over to Utility.diffusionFick(), then
		 * copy the solutions back into that layer.
		 * 
		 * The added io time for copying, etc, might make it take longer,
		 * but it will more than make up for it in readability/
		 * maintainability.
		 */
		double dT = cParams.getTimeStep() * 3600; // time units = hours ( as in fba ), convert to seconds
		double dX = cParams.getSpaceWidth();
		
		if (DEBUG)
		{
			System.out.println("dT: " + dT + "\tdX: " + dX);
			System.out.println("media diff consts:");
			for (int i=0; i<nutrientDiffConsts.length; i++)
			{
				System.out.print("\t" + nutrientDiffConsts[i]);
			}
			System.out.println();
		}
		
		for (int k=0; k<numMedia; k++)
		{
			double mediaLayerTotal = 0;
			double[][][] mediaLayer = new double[numCols][numRows][numLayers];
			for (int i=0; i<numCols; i++)
			{
				for (int j=0; j<numRows; j++)
				{
					for (int l = 0; l < numLayers; l++)
					{
						mediaLayer[i][j][l] = media[i][j][l][k];
						mediaLayerTotal+=media[i][j][l][k];
					}
				}
			}
			if (mediaLayerTotal > 0)
			{
				double diffConst = nutrientDiffConsts[k];
				// if the constant's illegal (less than 0), use the default.
				if (diffConst < 0)
					diffConst = pParams.getDefaultDiffusionConstant();
				if (diffConst > 0)
				{
					mediaLayer = Utility.diffuse3D(mediaLayer, barrier, diffConst, dT/pParams.getNumDiffusionsPerStep(), dX);
					for (int i=0; i<numCols; i++)
					{
						for (int j=0; j<numRows; j++)
						{
							for (int l = 0; l < numLayers; l++)
							{
								media[i][j][l][k] = mediaLayer[i][j][l];
							}
						}
					}
				}
			}
		}
	}
	
	/**
	 * Diffuses biomass in a new, novel fashion.
	 * @author Amrita Kar
	 */
	@SuppressWarnings("unused")
	private void diffuseBiomassSpread()
	{
		/* Yet another biomass diffusion method.
		 * 
		 * Only spread out biomass from spaces that have more than the maximum 
		 * allowed biomass level. REQUIRES the tag that allows for more biomass
		 * production than the highest level. E.g., NO FBA CAP on biomass 
		 * production, just preventing extra biomass production after the fact.
		 * 
		 * For each box with too much biomass, spread out the excess to adjacent 
		 * boxes using a second order diffusion approximation (a la BacSim - 
		 * Kreft et al, 1998)
		 *
		 * Finally, after doing this spreading out, check the grid again for 
		 * any new boxes that may have filled up as a result of the spreading.
		 * Apply the spreading until:
		 *    (a) There are no more overfilled boxes.
		 *    (b) There are overfilled boxes, but these are all surrounded by
		 *        a boundary.
		 *
		 * Main Parameter Cases:
		 * 1. Allowing species overlap.
		 *    If species can overlap, then an amount of all species - in the
		 *    same proportion as how much is in the box - is spread to adjacent
		 *    boxes.
		 * 2. No species overlap.
		 *    a. Treat boxes with different species as barriers.
		 *    b. Randomize the order in which boxes are spread out, and take
		 *       into account the spreading part as boundaries. E.g., if 
		 *       box1 and box2 have different spp., and box2 spreads out first,
		 *       then treat the spread-out areas from box2 as boundaries for
		 *       box1.
		 *
		 * Secondary Cases:
		 * 1. If adjacent boxes are boundaries (e.g. barrier, other species, 
		 *    or full of biomass), then the quantity of biomass that was to
		 *    move there is distributed among the remaining open boxes.
		 * 2. If ALL adjacent boxes are boundaries, no biomass moves (gives
		 *    kind of a fuzzy upper limit).
		 */
		
//		double[][][] deltaBiomass = new double[numModels][numCols][numRows];
		
		/* Foreach overfull box
		 *   1. Initialize 3x3 multiplier matrix
		 *   2. Get boundary neighborhood
		 *      a. Barriers = boundary
		 *      b. full boxes (sum of biomass >= limit) = boundary
		 *      c. boxes with biomass from other spp = boundary if no overlap
		 *         flag is set
		 *      d. edges of the grid = boundary
		 *   3. Update multiplier using this initial boundary info.
		 *   4. Apply multiplier to the deltaB value.
		 *   ---- now it gets tricky ----
		 *   5. For each surrounding box that gets overfilled due to this, only
		 *      move the amount of biomass that fills up that box.
		 *   6. Set those newly filled boxes as boundaries.
		 *   7. Run the boundary neighborhood method again.
		 */
		
		
	}
	
	@SuppressWarnings("unused")
	private double[][][] neighborhoodMult(boolean[][][] mask)
	{
		/* Walk around the mask neighborhood (not the center) and figure out
		 * how many neighboring boxes get to have more biomass
		 */
		return null;
	}
	
	/**
	 * Diffuses all biomass according to Fick's second law of diffusion with a few caveats
	 * and adjustments.
	 * <p>
	 * First, if species overlap is not allowed, diffuse one species at a time in random order.
	 * Spaces occupied by every other species than the one is currently diffusing are treated
	 * as Neumann boundaries.
	 * <p>
	 * Second, if a space is full, treat that space as a Neumann boundary as well.
	 * <p>
	 * Although this is eventually going to be deprecated, there are still problems with
	 * overfilling areas due to this method. Any space that gets overfilled should be 
	 * readjusted in some kind of post-processing manner.
	 * //TODO ^^^this 
	 */
	private void diffuseBiomass(FBAParameters.BiomassMotionStyle style)
	{
		/* similar to the new media diffusion method,
		 * this one will condense all biomass concentrations into a
		 * single matrix, hand it off to Utility.diffuseFick(),
		 * then use the results to make new cells as necessary.
		 */
		
		/* more problems.
		 * 1. cell overlap.
		 * if we allow cell overlap, there's no problem. just do each
		 * biomass type in turn.
		 * if we prevent cell overlap, there's kind of a big problem.
		 * how do we handle - using equations - the case where cells are
		 * blocked by each other?
		 * 
		 * 2. options
		 * a. random barrier choice.
		 * As far as each cell type is concerned, each space occupied by
		 * a cell acts as a Neumann boundary. So just add those boundaries 
		 * to each calculation on each diffusion cycle.
		 */
		double[] totalBiomass = calculateTotalBiomass();
		double[][][][] biomassGrowthState = new double[numModels][numCols][numRows][numLayers];
		double[][][][] biomassFlowState = new double[numModels][numCols][numRows][numLayers];
		Iterator<Cell> it = c.getCells().iterator();
		
		// capture the current biomass state
		while (it.hasNext())
		{
			FBACell cell = (FBACell)it.next();
			double[] biomass = cell.getBiomass(); // total biomass
			double[] deltaBiomass = cell.getDeltaBiomass(); // biomass produced this step
			int x = cell.getX();
			int y = cell.getY();
			int z = cell.getZ();
			for (int k=0; k<numModels; k++)
			{
				if (deltaBiomass[k] > 0)
				{
					biomassGrowthState[k][x][y][z] = deltaBiomass[k];
					biomassFlowState[k][x][y][z] = biomass[k] - deltaBiomass[k];
				}
				else
				{
					biomassGrowthState[k][x][y][z] = 0;
					biomassFlowState[k][x][y][z] = biomass[k];
				}
			}
		}
		// diffuse all types of biomass
		double dT = cParams.getTimeStep() * 3600; // time step is in hours, diffusion is in seconds
		double dX = cParams.getSpaceWidth();
		if (cParams.allowCellOverlap())
		{
			for (int k=0; k<numModels; k++)
			{
				if(cParams.getSimulateActivation() && !((FBAModel)models[k]).getActive())
				{
					continue;
				}
				if (((FBAModel)models[k]).getGrowthDiffusionConstant() > 0)
				{
					//if (style == FBAParameters.BiomassMotionStyle.DIFFUSION_CN)
					//	biomassGrowthState[k] = Utility.diffuse3D(biomassGrowthState[k], barrier, ((FBAModel)models[k]).getGrowthDiffusionConstant(), dT/pParams.getNumDiffusionsPerStep(), dX);
						//biomassGrowthState[k] = Utility.diffuseFick3D(biomassGrowthState[k], barrier, dirichlet, ((FBAModel)models[k]).getGrowthDiffusionConstant(), dT/pParams.getNumDiffusionsPerStep(), dX, pParams.getNumDiffusionsPerStep());
					//if (style == FBAParameters.BiomassMotionStyle.DIFFUSION_EP)
					//	;//biomassGrowthState[k] = Utility.diffuseEightPoint(biomassGrowthState[k], dX, ((FBAModel)models[k]).getGrowthDiffusionConstant(), dT/pParams.getNumDiffusionsPerStep(), barrier);
					if (style == FBAParameters.BiomassMotionStyle.DIFFUSION_3D)
						biomassGrowthState[k] = Utility.diffuse3D(biomassGrowthState[k], barrier, ((FBAModel)models[k]).getGrowthDiffusionConstant(), dT/pParams.getNumDiffusionsPerStep(), dX);
					if (style != FBAParameters.BiomassMotionStyle.DIFFUSION_3D)
						System.out.println("No biomass diffusion! The diffusion parameter must be set to 'Diffusion 3D'. ");
				}
				if (((FBAModel)models[k]).getFlowDiffusionConstant() > 0)
				{
					//if (style == FBAParameters.BiomassMotionStyle.DIFFUSION_CN)
					//	biomassFlowState[k] = Utility.diffuse3D(biomassFlowState[k], barrier, ((FBAModel)models[k]).getFlowDiffusionConstant(), dT/pParams.getNumDiffusionsPerStep(), dX);
						//biomassFlowState[k] = Utility.diffuseFick3D(biomassFlowState[k], barrier, dirichlet, ((FBAModel)models[k]).getFlowDiffusionConstant(), dT/pParams.getNumDiffusionsPerStep(), dX, pParams.getNumDiffusionsPerStep());
					//if (style == FBAParameters.BiomassMotionStyle.DIFFUSION_EP)
					//	;//biomassFlowState[k] = Utility.diffuseEightPoint(biomassFlowState[k], dX, ((FBAModel)models[k]).getFlowDiffusionConstant(), dT/pParams.getNumDiffusionsPerStep(), barrier);
					if (style == FBAParameters.BiomassMotionStyle.DIFFUSION_3D)
						biomassFlowState[k] = Utility.diffuse3D(biomassFlowState[k], barrier, ((FBAModel)models[k]).getFlowDiffusionConstant(), dT/pParams.getNumDiffusionsPerStep(), dX);
					if (style != FBAParameters.BiomassMotionStyle.DIFFUSION_3D)
						System.out.println("No biomass diffusion! The diffusion parameter must be set to 'Diffusion 3D'. ");
				}
			}
		}
		else
		{
			int[] order = Utility.randomOrder(numModels);
			//Commenting the above line and uncommenting below takes out 
			//the randomization of the order in which the models are updated
			//I.Dukovski
			//int[] order = new int[numModels];
			//for (int a=0; a<numModels; a++)
			//{
			//	order[a]=a;
			//}
			//
			for (int k=0; k<order.length; k++)
			{
				int curModel = order[k];
				// figure out where species k can't go.
				// then diffuse it across that area
				// this just went from being O(n) --> O(n^2). :(
				if(cParams.getSimulateActivation() && !((FBAModel)models[k]).getActive())
				{
					continue;
				}
				boolean[][][] barrierState = new boolean[numCols][numRows][numLayers];
				if (DEBUG) System.out.println("setting barrier state");
				for (int i=0; i<numCols; i++)
				{
					for (int j=0; j<numRows; j++)
					{
						for (int m = 0; m < numLayers; m++)
						{
							double otherBiomass = 0;
							for (int l = 0; l<numModels; l++)
							{
								if (l!=curModel)
									otherBiomass += biomassGrowthState[l][i][j][m] + biomassFlowState[l][i][j][m];
							}
							barrierState[i][j][m] = barrier[i][j][m] || (otherBiomass > 0);
						}
					}
				}
				//if (style == FBAParameters.BiomassMotionStyle.DIFFUSION_CN)
				//{
				//	biomassGrowthState[curModel] = Utility.diffuse3D(biomassGrowthState[curModel], barrierState, ((FBAModel)models[curModel]).getGrowthDiffusionConstant(), dT/pParams.getNumDiffusionsPerStep(), dX);
				//	biomassFlowState[curModel] = Utility.diffuse3D(biomassFlowState[curModel], barrierState, ((FBAModel)models[curModel]).getFlowDiffusionConstant(), dT/pParams.getNumDiffusionsPerStep(), dX);
				//}
				//if (style == FBAParameters.BiomassMotionStyle.DIFFUSION_EP)
				//{
				//	;//biomassGrowthState[curModel] = Utility.diffuseEightPoint(biomassGrowthState[curModel], dX, ((FBAModel)models[curModel]).getGrowthDiffusionConstant(), dT/pParams.getNumDiffusionsPerStep(), barrierState);
				//	//biomassFlowState[curModel] = Utility.diffuseEightPoint(biomassFlowState[curModel], dX, ((FBAModel)models[curModel]).getFlowDiffusionConstant(), dT/pParams.getNumDiffusionsPerStep(), barrierState);				
				//}
				if (style == FBAParameters.BiomassMotionStyle.DIFFUSION_3D)
				{
					biomassGrowthState[curModel] = Utility.diffuse3D(biomassGrowthState[curModel], barrierState, ((FBAModel)models[curModel]).getGrowthDiffusionConstant(), dT/pParams.getNumDiffusionsPerStep(), dX);
					biomassFlowState[curModel] = Utility.diffuse3D(biomassFlowState[curModel], barrierState, ((FBAModel)models[curModel]).getFlowDiffusionConstant(), dT/pParams.getNumDiffusionsPerStep(), dX);
				}
				if (style == FBAParameters.BiomassMotionStyle.DIFFUSION_CN)
					System.out.println("No biomass diffusion! The diffusion parameter must be set to 'Diffusion 3D'. ");	
				if (style == FBAParameters.BiomassMotionStyle.DIFFUSION_EP)
					System.out.println("No biomass diffusion! The diffusion parameter must be set to 'Diffusion 3D'. ");
				
				
				/* There's a numerical problem inherent to doing diffusion:
				 * Even having a small concentration in a single space can diffuse out to 
				 * a tiny concentration on the fringes of the grid (even down to ~1e-20),
				 * effectively blocking any diffusion of other species. 
				 * Even though, when new cells are built from the fresh diffusion, any 
				 * tiny concentrations will just disappear and "die". 
				 * 
				 * To counter this, run a check - any space in the freshly diffused areas
				 * that do not meet the minimum biomass threshold should just be wiped out.
				 * 
				 * This is only a problem here because each species is diffused separately,
				 * with the new diffusion used to make Neumann boundaries for the next
				 * species.
				 */
				for (int i=0; i<numCols; i++)
				{
					for (int j=0; j<numRows; j++)
					{
						for (int l = 0; l < numLayers; l++)
						{
							if (biomassGrowthState[curModel][i][j][l] + biomassFlowState[curModel][i][j][l] < cParams.getMinSpaceBiomass())
							{
								biomassGrowthState[curModel][i][j][l] = 0;
								biomassFlowState[curModel][i][j][l] = 0;
							}
						}
					}
				}
			}
		}
		// update the world with the results.

		for (int i=0; i<numCols; i++)
		{
			for (int j=0; j<numRows; j++)
			{
				for ( int l = 0; l < numLayers; l++)
				{
					// if there's some value at biomassState[][i][j];
					double[] newBiomass = new double[numModels];
					for (int k=0; k<numModels; k++)
						newBiomass[k] = biomassGrowthState[k][i][j][l] + biomassFlowState[k][i][j][l];
				
					if (Utility.hasNonzeroValue(newBiomass))
					{
						if (isOccupied(i,j,l))
						{
							Cell cell = (Cell)getCellAt(i,j,l);
							cell.setBiomass3D(newBiomass);
						}
						else // make a new Cell here
						{
							Cell cell = new FBACell(i, j, l, newBiomass, this, (FBAModel[])models, cParams, pParams);
							c.getCells().add(cell);
						}
					}
				}
			}
		}
		double[] totalBiomassDiff = calculateTotalBiomass();
		if (DEBUG)
		{
			for (int i=0; i<totalBiomass.length; i++)
			{
				System.out.println("biomass_differr" + i + " " + (totalBiomassDiff[i]-totalBiomass[i]));
			}
		}
	}
	
	/**
	 * The 3D convection model for biomass transport. 
	 *
	 */
	private void convection3DBiomass()
	{

		
		/* more problems.
		 * 1. cell overlap.
		 * if we allow cell overlap, there's no problem. just do each
		 * biomass type in turn.
		 * if we prevent cell overlap, there's kind of a big problem.
		 * how do we handle - using equations - the case where cells are
		 * blocked by each other?
		 * 
		 * 2. options
		 * a. random barrier choice.
		 * As far as each cell type is concerned, each space occupied by
		 * a cell acts as a Neumann boundary. So just add those boundaries 
		 * to each calculation on each diffusion cycle.
		 */
		
		double[][][][] deltaDensity = new double[numModels][numCols][numRows][numLayers];
		double[][][][] biomassDensity = new double[numModels][numCols][numRows][numLayers];
		double[][][][] biomassDensityIntermediate = new double[numModels][numCols][numRows][numLayers];
		double[][][][] convectionRHS  = new double[numModels][numCols][numRows][numLayers];
		double[][][][] convectionRHS1 = new double[numModels][numCols][numRows][numLayers];
		double[][][][] convectionRHS2 = new double[numModels][numCols][numRows][numLayers];
		Iterator<Cell> it = c.getCells().iterator();
		
		
		double dT = cParams.getTimeStep() * 3600; // time step is in hours, diffusion is in seconds
		double dX = cParams.getSpaceWidth();
		// capture the current biomass state
		while (it.hasNext())
		{
			FBACell cell = (FBACell)it.next();
			double[] biomass = cell.getBiomass();  // total biomass
			double[] deltaBiomass = cell.getDeltaBiomass(); // biomass produced this step
			//System.out.println(deltaBiomass[0]);
			
			int x = cell.getX();
			int y = cell.getY();
			int z = cell.getZ();
			
			for (int k=0; k<numModels; k++)
			{
				biomassDensity[k][x][y][z]=biomass[k];// - deltaBiomass[k];
				deltaDensity[k][x][y][z]=deltaBiomass[k];
				//growthRate[k][x][y]=deltaBiomass[k]/(dT*(biomass[k] - deltaBiomass[k]));
				convectionRHS1[k][y][y][z]=cell.getConvectionRHS1()[k];
				convectionRHS2[k][x][y][z]=cell.getConvectionRHS2()[k];
			}
		}
		
		//System.out.println(cParams.allowCellOverlap());
		
		if (cParams.allowCellOverlap())
		{
			for (int k=0; k<numModels; k++)
			{
				if(cParams.getSimulateActivation() && !((FBAModel)models[k]).getActive())
				{
					continue;
				} 
				double[][][] convDiffConstField=new double[numCols][numRows][numLayers];
				for (int i=0; i<numCols; i++)
				{
					for (int j=0; j<numRows; j++)
					{
						for (int l=0; l<numLayers; l++)
						{
							convDiffConstField[i][j][l]=((FBAModel)models[k]).getConvDiffConstant();
						}
					}
				}
				
				convectionRHS[k]=Utility.getConvectionRHS3D(biomassDensity[k],convDiffConstField,((FBAModel)models[k]).getPackedDensity(),barrier,dX,((FBAModel)models[k]).getElasticModulusConstant(),((FBAModel)models[k]).getFrictionConstant()); 	
				for(int i=0;i<numCols;i++)
				{
					for(int j=0;j<numRows;j++)
					{
						for (int l=0;l<numLayers;l++)
						{
							biomassDensityIntermediate[k][i][j][l]=biomassDensity[k][i][j][l]+dT*(23.0*convectionRHS[k][i][j][l]-16.0*convectionRHS1[k][i][j][l]+5.0*convectionRHS2[k][i][j][l])/12.0;
						}
					}
				}
				for(int i=0;i<numCols;i++)
				{
					for(int j=0;j<numRows;j++)
					{
						for(int l=0;l<numLayers;l++)
						{
							convectionRHS2[k][i][j][l]=convectionRHS1[k][i][j][l];
							convectionRHS1[k][i][j][l]=convectionRHS[k][i][j][l];
						}
					}
				}
				
				convectionRHS[k]=Utility.getConvectionRHS3D(biomassDensityIntermediate[k],convDiffConstField,((FBAModel)models[k]).getPackedDensity(),barrier,dX,((FBAModel)models[k]).getElasticModulusConstant(),((FBAModel)models[k]).getFrictionConstant());
				for(int i=0;i<numCols;i++)
				{
					for(int j=0;j<numRows;j++)
					{   
						for(int l=0;l<numLayers;l++)
						{
							biomassDensity[k][i][j][l]=biomassDensity[k][i][j][l]+dT*(5.0*convectionRHS[k][i][j][l]+8.0*convectionRHS1[k][i][j][l]-1.0*convectionRHS2[k][i][j][l])/12.0;
							//add random gaussioan noise
							//System.out.println(((FBAModel)models[k]).getNoiseVariance());
							//System.out.println(Utility.gaussianNoise(((FBAModel)models[k]).getNoiseVariance()));
							biomassDensity[k][i][j][l]=biomassDensity[k][i][j][l]+deltaDensity[k][i][j][l]*Utility.gaussianNoise(((FBAModel)models[k]).getNoiseVariance());
						}
					}
				}

				
			}
		}
		else
		{
			int[] order = Utility.randomOrder(numModels);
			//Commenting the above line and uncommenting below takes out 
			//the randomization of the order in which the models are updated
			//I.Dukovski
			//int[] order = new int[numModels];
			//for (int a=0; a<numModels; a++)
			//{
			//	order[a]=a;
			//}
			//
			for (int k=0; k<order.length; k++)
			{
				int curModel = order[k];
				// figure out where species k can't go.
				// then diffuse it across that area
				// this just went from being O(n) --> O(n^2). :(
				if(cParams.getSimulateActivation() && !((FBAModel)models[curModel]).getActive())
				{
					continue;
				}
				boolean[][][] barrierState = new boolean[numCols][numRows][numLayers];
				if (DEBUG) System.out.println("setting barrier state");
				for (int i=0; i<numCols; i++)
				{
					for (int j=0; j<numRows; j++)
					{
						for(int m=0;m<numLayers;m++)
						{
							double otherBiomass = 0;
							for (int l=0; l<numModels; l++)
							{
								if (l!=curModel)
									otherBiomass+=biomassDensity[l][i][j][m];
							}
							barrierState[i][j][m] = barrier[i][j][m] || (otherBiomass > 0);
						}
					}
				}
				double[][][] convDiffConstField=new double[numCols][numRows][numLayers];
				for (int i=0; i<numCols; i++)
				{
					for (int j=0; j<numRows; j++)
					{
						for(int l=0;l<numLayers;l++)
						{
							convDiffConstField[i][j][l]=((FBAModel)models[curModel]).getConvDiffConstant();
						}
					}
				}
				convectionRHS[curModel]=Utility.getConvectionRHS3D(biomassDensity[curModel],convDiffConstField,((FBAModel)models[curModel]).getPackedDensity(),barrierState,dX,((FBAModel)models[curModel]).getElasticModulusConstant(),((FBAModel)models[curModel]).getFrictionConstant()); 	
				for(int i=0;i<numCols;i++)
				{
					for(int j=0;j<numRows;j++)
					{
						for(int l=0;l<numLayers;l++)
						{
							biomassDensityIntermediate[curModel][i][j][l]=biomassDensity[curModel][i][j][l]+dT*(23.0*convectionRHS[curModel][i][j][l]-16.0*convectionRHS1[curModel][i][j][l]+5.0*convectionRHS2[curModel][i][j][l])/12.0;
						}
					}
				}
				for(int i=0;i<numCols;i++)
				{
					for(int j=0;j<numRows;j++)
					{
						for(int l=0;l<numLayers;l++)
						{
							convectionRHS2[curModel][i][j][l]=convectionRHS1[curModel][i][j][l];
							convectionRHS1[curModel][i][j][l]=convectionRHS[curModel][i][j][l];
						}
					}
				}
				
				convectionRHS[curModel]=Utility.getConvectionRHS3D(biomassDensityIntermediate[curModel],convDiffConstField,((FBAModel)models[curModel]).getPackedDensity(),barrierState,dX,((FBAModel)models[curModel]).getElasticModulusConstant(),((FBAModel)models[curModel]).getFrictionConstant());
				for(int i=0;i<numCols;i++)
				{
					for(int j=0;j<numRows;j++)
					{   
						for(int l=0;l<numLayers;l++)
						{
							biomassDensity[curModel][i][j][l]=biomassDensity[curModel][i][j][l]+dT*(5.0*convectionRHS[curModel][i][j][l]+8.0*convectionRHS1[curModel][i][j][l]-1.0*convectionRHS2[curModel][i][j][l])/12.0;
							//System.out.println(biomassDensity[k][i][j]);
							biomassDensity[k][i][j][l]=biomassDensity[k][i][j][l]+deltaDensity[k][i][j][l]*Utility.gaussianNoise(((FBAModel)models[k]).getNoiseVariance());
						}
					}
				}


				/* There's a numerical problem inherent to doing diffusion:
				 * Even having a small concentration in a single space can diffuse out to 
				 * a tiny concentration on the fringes of the grid (even down to ~1e-20),
				 * effectively blocking any diffusion of other species. 
				 * Even though, when new cells are built from the fresh diffusion, any 
				 * tiny concentrations will just disappear and "die". 
				 * 
				 * To counter this, run a check - any space in the freshly diffused areas
				 * that do not meet the minimum biomass threshold should just be wiped out.
				 * 
				 * This is only a problem here because each species is diffused separately,
				 * with the new diffusion used to make Neumann boundaries for the next
				 * species.
				 */
				for (int i=0; i<numCols; i++)
				{
					for (int j=0; j<numRows; j++)
					{
						for (int l=0;l<numLayers;l++)
						{
							if (biomassDensity[curModel][i][j][l] < cParams.getMinSpaceBiomass())
							{
								biomassDensity[curModel][i][j][l] = 0;
							}
						}
					}
				}
			}
		}
		// update the world with the results.
		
		for (int i=0; i<numCols; i++)
		{
			for (int j=0; j<numRows; j++)
			{
				for(int l=0;l<numLayers;l++)
				{
					// if there's some value at biomassState[][i][j];
					double[] newBiomass = new double[numModels];
					double[] newConvectionRHS1=new double[numModels];
					double[] newConvectionRHS2=new double[numModels];
				
					for (int k=0; k<numModels; k++)
					{
						newBiomass[k] = biomassDensity[k][i][j][l];
						newConvectionRHS1[k]=convectionRHS1[k][i][j][l];
						newConvectionRHS2[k]=convectionRHS2[k][i][j][l];
						//System.out.println(i+","+j+"     "+biomassDensity[k][i][j]);
					}
			
					if (Utility.hasNonzeroValue(newBiomass) || Utility.hasNonzeroValue(newConvectionRHS1) || Utility.hasNonzeroValue(newConvectionRHS1))
					{
						//System.out.println(i+"  "+j);
						//System.out.println("OK");
						if (isOccupied(i,j,l))
						{
							//System.out.println("OK1");
							Cell cell = (Cell)getCellAt(i,j,l);
							cell.setBiomass3D(newBiomass);
							cell.setConvectionRHS1(newConvectionRHS1);
							cell.setConvectionRHS2(newConvectionRHS2);
						}
						else // make a new Cell here
						{   
							//System.out.println("OK2");
							Cell cell = new FBACell(i, j, l, newBiomass, this, (FBAModel[])models, cParams, pParams);
							cell.setConvectionRHS1(newConvectionRHS1);
							cell.setConvectionRHS2(newConvectionRHS2);
							c.getCells().add(cell);
						}
					}
				}
			}
		}
	}
	
/************** OLD DIFFUSION METHODS ************************/
	
//	public void diffuseMedia()
//	{
//		diffuseMedia(new int[0]);
//	}
//
//	public void diffuseMedia(int[] excluded)
//	{
//		long time = System.currentTimeMillis();
//		System.out.println("diffusing media");
//		double[][][] nextMedia = new double[numCols][numRows][numMedia];
//
//		// diffuses out all media except for those indices in the excluded list
//		for (int i = 0; i < numCols; i++) // world x
//		{
//			for (int j = 0; j < numRows; j++) // world y
//			{
//				if (!isBarrier(i, j))
//				{
//					if (cParams.isToroidalGrid())
//					{
//						i = adjustX(i);
//						j = adjustY(j);
//					}
//
//					double[] diff = diffuseMediaBox(i, j);
//					int exPtr = 0;
//					for (int k = 0; k < numMedia; k++) // media idx
//					{
//						if (exPtr < excluded.length && k == excluded[exPtr])
//						{
//							exPtr++;
//							nextMedia[i][j][k] = media[i][j][k];
//						} else
//						{
//							nextMedia[i][j][k] = media[i][j][k] + diff[k];
//						}
//					}
//				}
//			}
//		}
//		media = nextMedia;
//		System.out.println("difftime = " + (System.currentTimeMillis() - time));
//	}
//
//	private double[] diffuseMediaBox(int x, int y)
//	{
//		double[] diff = new double[numMedia];
//		int a = 0;
//		int b = 0;
//		double[] totalLost = new double[numMedia];
//
//		for (int i = x - 1; i <= x + 1; i++) // local neighborhood rows
//		{
//			b = 0;
//
//			for (int j = y - 1; j <= y + 1; j++) // local neighborhood columns
//			{
//				int iReal = i;
//				int jReal = j;
//
//				if (cParams.isToroidalGrid())
//				{
//					iReal = adjustX(i);
//					jReal = adjustY(j);
//				}
//				if (iReal != x || jReal != y) // skip the center space - we'll
//												// diffuse from that when we
//												// know how much to diffuse
//				{
//					// println(x + " " + y);
//					// println(i + " " + j + "...");
//					// if space (i,j) can be diffused FROM, then we add some
//					// amount to diff
//					for (int k = 0; k < numMedia; k++)
//					{
//						// println("k = " + k);
//						// println("a,b = " + a + " " + b);
//						if (canDiffuseMediaOut(iReal, jReal, k)
//								&& canDiffuseMediaIn(x, y, k))
//						{
//							diff[k] += DIFFUSION_SCALE[a][b]
//									* media[iReal][jReal][k];
//						}
//						if (canDiffuseMediaIn(iReal, jReal, k)
//								&& canDiffuseMediaOut(x, y, k))
//						{
//							totalLost[k] += DIFFUSION_SCALE[a][b];
//						}
//					}
//				}
//				b++;
//			}
//			a++;
//		}
//
//		for (int i = 0; i < numMedia; i++)
//		{
//			diff[i] -= totalLost[i] * media[x][y][i];
//			diff[i] = cParams.getTimeStep() * cParams.getDiffusionRate()
//					* diff[i] / 2;
//		}
//
//		return diff;
//	}

	/**
	 * Runs one cycle of the simulation on this <code>FBAWorld</code>. The simulation flow
	 * works as follows:
	 * <ol>
	 * <li>Run FBA on all cells (either through the threaded method or not). Any cell that
	 * returns a Cell.CELL_DEAD value is added to a list.
	 * <li>Remove any dead cells //TODO - honestly, why do this? What's wrong with just
	 * leaving cells with zero biomass, other than memory problems? It might speed things up.
	 * <li>Diffuse media and biomass //TODO Amrita's new biomass methods insert here.
	 * <li>Update any media that's supposed to be static
	 * <li>Refresh any media that's supposed to be
	 * <li>Update the InfoPanel if one's made
	 * <li>(DEBUG) print the total biomass 
	 * <li>Write to logs, where appropriate
	 * <li>Return a status value
	 * @return PARAMS_OK 
	 */
	public int run()
	{
		int ret = PARAMS_OK;
		if (pParams.getNumRunThreads() > 1)
		{	
			//try
			//{
			//	PrintWriter out1 = new PrintWriter(new FileWriter("fbatime_multithread.txt",true));
			//    long t = System.currentTimeMillis();
			    ret = runThreaded();
			    //System.out.println("total mt fba time = "+(System.currentTimeMillis()-t));
			    //out1.println((System.currentTimeMillis()-t));
            //    out1.close();
		    //}
		    //catch (IOException e)
		    //{
			//    System.out.println("File fbatime_mutithread.dat not found");
		    //}
		}
		else
		{
			if (DEBUG)
			{
				for (int i=0; i<models.length; i++)
				{
					System.out.println("model " + i + " flowDiffConst = " + models[i].getFlowDiffusionConstant());
					System.out.println("model " + i + " growthDiffConst = " + models[i].getGrowthDiffusionConstant());
				}
			}
			// if (models == null)
			// models = (FBAModel[])c.getModels();

			// 2. tell all the cells to run
			List<Cell> deadCells = new ArrayList<Cell>();
			//try
			//{
			//	PrintWriter out1 = new PrintWriter(new FileWriter("fbatime_singlethread.txt",true));
			//    long t = System.currentTimeMillis();
			    for (int i = 0; i < c.getCells().size(); i++)
			    { 
				    // print("running cell " + i + "...");
				    Cell cell = (Cell) c.getCells().get(i);
				    int alive = cell.run();
				    if (alive == Cell.CELL_DEAD)
					    deadCells.add(cell);
				    // println(" done!");
			    }
			    //System.out.println("total st fba time = " + (System.currentTimeMillis() - t));
                //out1.println((System.currentTimeMillis() - t));
             //   out1.close();
			//}
			//catch(IOException e)
			//{
			//	System.out.println("File fbatime_singlethread.txt not found.");
			//}
			// remove dead cells.
			switch(pParams.getBiomassMotionStyle())
			{
				case CONVECTION_3D:
					;
					break;
				default:
					for (int i = 0; i < deadCells.size(); i++)
					{
						// System.out.println("removing deadcell " + i + " of " +
						// deadCells.size());
						Cell cell = (Cell) c.getCells().remove(
								c.getCells().indexOf(deadCells.get(i)));
						removeCell(cell.getX(), cell.getY(), cell.getZ());
					}
					break;
			}
			deadCells.clear();
		}
        
		//try
		//{
		//    long t = System.currentTimeMillis();
		    // 3. diffuse media and biomass
        //    PrintWriter out1 = new PrintWriter(new FileWriter("difftime.txt",true));
//System.out.println("ok"+cParams.getDiffusionsPerStep());
//System.out.println("OK"+pParams.getNumDiffusionsPerStep());
//System.out.println("flow"+pParams.getFlowDiffRate());
//System.out.println("growth"+pParams.getGrowthDiffRate());
//if(pParams.getExchangeStyle() instanceof FBAParameters.ExchangeStyle)
//System.out.println("style "+pParams.getExchangeStyle());
//System.out.println("numDiff "+pParams.getNumDiffusionsPerStep());
			//for (int i = 0; i < cParams.getDiffusionsPerStep(); i++)
            //for (int i = 0; i < pParams.getNumDiffusionsPerStep(); i++)
		    //{          	      	
			    diffuseMediaFick();
			    //diffuseBiomass(pParams.getBiomassMotionStyle());
			    switch (pParams.getBiomassMotionStyle())
			    {
			    	case DIFFUSION_3D :
			    		diffuseBiomass(pParams.getBiomassMotionStyle());
			    		break;
			    	case CONVECTION_3D:
			    		convection3DBiomass();
			    		break;
//				    case DIFFUSION_CN :
//					    diffuseBiomass(pParams.getBiomassMotionStyle());
//					    break;
//				    case DIFFUSION_EP :
//					    diffuseBiomass(pParams.getBiomassMotionStyle());
//					    break;
//				    default :
//					    break;
			    }

		    //}
//out1.println((System.currentTimeMillis() - t));
         //   out1.close();
		//}
		//catch(IOException e)
		//{
		//	System.out.println("File difftime.txt not found.");
		//}
		//System.out.println("total st diffusion time = " + (System.currentTimeMillis() - t));
		
		//t = System.currentTimeMillis();
		// 4. set static media
		applyStaticMedia();
		//System.out.println("total applyStaticMedia = " + (System.currentTimeMillis() - t));
		
		//t = System.currentTimeMillis();
		// 5. refresh media, if we're supposed to.
		refreshMedia();
		//System.out.println("total refreshMedia = " + (System.currentTimeMillis() - t));
		
		//t = System.currentTimeMillis();
		//if (!cParams.isCommandLineOnly())
		//	updateInfoPanel();

		double[] totalBiomass = calculateTotalBiomass();
		System.out.println ("Total biomass:");
		for (int i=0; i<totalBiomass.length; i++)
		{
			System.out.println("Model " + i + ": " + totalBiomass[i]);
		}
		//System.out.println("total calculateBiomass = " + (System.currentTimeMillis() - t));
		
		currentTimePoint++;
		if (pParams.writeFluxLog() && currentTimePoint % pParams.getFluxLogRate() == 0)
			writeFluxLog();
		if (pParams.writeMediaLog() && currentTimePoint % pParams.getMediaLogRate() == 0)
			writeMediaLog();
		if (pParams.writeBiomassLog() && currentTimePoint % pParams.getBiomassLogRate() == 0)
			writeBiomassLog();
		if (pParams.writeTotalBiomassLog() && currentTimePoint % pParams.getTotalBiomassLogRate() == 0)
			writeTotalBiomassLog();
		return ret;
	}
	
	/**
	 * Performs the FBA phase of the simulation run using the <code>FBARunThread</code> group.
	 * If there are no threads, it makes them first, then runs them. This also removes
	 * any dead cells at the end of the run.
	 * @return
	 */
	private int runThreaded()
	{
		// spawn a bunch of threads.
		// wait for them to all finish.
		// return.
		if (runThreads == null || (pParams.getNumRunThreads() != runThreads.length))
			initRunThreads();

		/*
		 * runThreads = new FBARunThread[cParams.getNumRunThreads()]; for (int
		 * i=0; i<runThreads.length; i++) { runThreads[i] = new
		 * FBARunThread(this, cParams, (FBAModel[])c.getModels());
		 * runThreads[i].setName("FBARunThread-" + i); } for (int i=0;
		 * i<runThreads.length; i++) { runThreads[i].start(); }
		 */
		runCells.addAll(c.getCells());
		deadCells = new ArrayList<Cell>();
		int numCells = runCells.size();
		threadLock = 0;
		synchronized (runCells)
		{
			runCells.notifyAll();
		}

		while (!runCells.isEmpty() || threadLock < numCells)
		{
			// just a hack to make sure that the main thread
			// doesn't jump ahead too far.
		}

		// remove dead cells.
		switch(pParams.getBiomassMotionStyle())
		{
			case CONVECTION_3D:
				;
				break;
			default:
				for (int i = 0; i < deadCells.size(); i++)
				{
					// System.out.println("removing deadcell " + i + " of " +
					// deadCells.size());
					Cell cell = (Cell) c.getCells().remove(
							c.getCells().indexOf(deadCells.get(i)));
					removeCell(cell.getX(), cell.getY(), cell.getZ());
				}
				break;
		}
		deadCells.clear();
		return 0;
	}

	/**
	 * When an FBARunThread wants to signal that it's finished running a cell, it should call
	 * this.
	 */
	public synchronized void finishedRunningCell(FBACell cell, int returnCode)
	{
		threadLock++;
		if (returnCode == Cell.CELL_DEAD)
			deadCells.add(cell);
	}
	
	/**
	 * This doles out a new <code>FBACell</code> to any thread that is ready to run FBA on it. 
	 * @return an FBACell for the threads to run
	 */
	public FBACell getNextRunCell()
	{
		// System.out.println("fetching run cell...");
		synchronized (runCells)
		{
			if (runCells.isEmpty())
			{
				// System.out.println("runCells is empty!");
				try
				{
					// notify();
					runCells.wait();
				} catch (InterruptedException e)
				{
				}
			}
			if (!runCells.isEmpty())
			{
				FBACell cell = (FBACell)runCells.pop();
				return cell;
			}
				//return (FBACell) runCells.pop();
		}
		return null;
	}
	
	/**
	 * Writes to the currently initialized flux log, if it is the right time. See documentation
	 * for the formats.
	 */
	private void writeFluxLog()
	{
		if (fluxLogWriter != null && (currentTimePoint == 1 || currentTimePoint % pParams.getFluxLogRate() == 0)) // log writer is initialized
		{			
			NumberFormat nf = NumberFormat.getInstance();
			nf.setGroupingUsed(false);
			nf.setMaximumFractionDigits(4);

			switch(pParams.getFluxLogFormat())
			{
				case MATLAB:
					/*
					 * Matlab .m file format:
					 * fluxes{time}{x}{y}{species} = [array];
					 * so it'll be one bigass structure.
					 */
					Iterator<Cell> it = c.getCells().iterator();
					while (it.hasNext())
					{
						FBACell cell = (FBACell)it.next();
						double fluxes[][] = cell.getFluxes();
						if (fluxes == null)
							continue; // fluxes uninitialized.
						else
						{
							for (int i=0; i<fluxes.length; i++)
							{
								if (fluxes[i] != null)
								{
									fluxLogWriter.write("fluxes{" + (currentTimePoint) + "}{" + (cell.getX()+1) + "}{" + (cell.getY()+1) + "}{"+ (cell.getZ()+1) + "}{" + (i+1) + "} = [");
									for (int j=0; j<fluxes[i].length; j++)
									{
										fluxLogWriter.write(nf.format(fluxes[i][j]) + " ");
									}
									fluxLogWriter.write("];\n");
								}
							}
						}
					}
					break;
					
				default:
					/* print all fluxes from each cell.
					 * format:
					 * timepoint\n
					 * x y fluxes_species_0 fluxes_species_1 ... fluxes_species_n\n
					 * x y fluxes_species_0 fluxes_species_1 ... fluxes_species_n\n
					 * ...
					 */
					fluxLogWriter.println(currentTimePoint);
					it = c.getCells().iterator();
					while (it.hasNext())
					{
						// blah print
						FBACell cell = (FBACell)it.next();
						double[][] fluxes = cell.getFluxes();
						if (fluxes[0] == null) // e.g., FBA hasn't been run yet.
							continue;
						else
						{
							fluxLogWriter.print(cell.getX() + " " + cell.getY()+" " + cell.getZ());
							for (int i=0; i<fluxes.length; i++)
							{
								for (int j=0; j<fluxes[i].length; j++)
								{
									fluxLogWriter.print(" " + nf.format(fluxes[i][j]));
								}
							}
							fluxLogWriter.print("\n");
						}
					}
					break;
			}
			fluxLogWriter.flush();
		}
	}
	
	/**
	 * Writes to the media log if it is the right time point. See documentation for the format.
	 */
	private void writeMediaLog()
	{
		System.out.println("WRITING MEDIA LOG");
		if (mediaLogWriter != null && (currentTimePoint == 1 || currentTimePoint % pParams.getMediaLogRate() == 0))
		{
			//NumberFormat nf = NumberFormat.getInstance();
			//nf.setGroupingUsed(false);
			//nf.setMaximumFractionDigits(9);
			NumberFormat nf = new DecimalFormat("0.##########E0");
			
			for (int k=0; k<numMedia; k++)
			{
				mediaLogWriter.println("media_" + currentTimePoint + "{" + (k+1) + "} = sparse(zeros(" + numCols + ", " + numRows + ", " + numLayers + "));");
				for (int i=0; i<numCols; i++)
				{
					for (int j=0; j<numRows; j++)
					{
						for (int l = 0; l < numLayers; l++)
						{
							if (media[i][j][l][k] != 0)
								mediaLogWriter.println("media_" + currentTimePoint + "{" + (k+1) + "}(" + (i+1) + ", " + (j+1) + ", " + (l+1)+") = " + nf.format(media[i][j][l][k]) + ";");
						}
					}
				}
			}
			mediaLogWriter.flush();
		}
	}

	/**
	 * Writes the current status to the biomass log if it is at the right time point.
	 * See documentation for formats.
	 */
	private void writeBiomassLog()
	{
		if (biomassLogWriter != null)// && (currentTimePoint == 1 || currentTimePoint % pParams.getBiomassLogRate() == 0))
		{
			NumberFormat nf = NumberFormat.getInstance();
			nf.setGroupingUsed(false);
			nf.setMaximumFractionDigits(100);
			switch(pParams.getBiomassLogFormat())
			{
				/* Matlab .m file format:
				 * biomass_<time>_<species> = sparse(<num_rows>, <num_cols>);
				 * biomass_<time>_<species>(<row>, <col>) = <biomass>
				 * ...
				 * and so on.
				 */
				case MATLAB:
					for (int i=0; i<models.length; i++)
					{
						String varName = "biomass_" + currentTimePoint + "_" + i;
						biomassLogWriter.println(varName + " = sparse(" + numLayers+", "+ numRows + ", " + numCols +");");
						Iterator<Cell> it = c.getCells().iterator();
						while (it.hasNext())
						{
							FBACell cell = (FBACell)it.next();
							double[] biomass = cell.getBiomass();
							biomassLogWriter.println(varName + "("+(cell.getZ()+1)+", " + (cell.getY()+1) + ", " + (cell.getX()+1) + ") = " + nf.format(biomass[i]) + ";");
						}
					}
					break;
					
				default:
					/*
					 * Comets file format:
					 * currentTimePoint on a line
					 * x y biomass1 biomass2 ... biomassN
					 * x y ...
					 * etc.
					 */
					biomassLogWriter.println(currentTimePoint);
					Iterator<Cell> it = c.getCells().iterator();
					while (it.hasNext())
					{
						FBACell cell = (FBACell)it.next();
						double[] biomass = cell.getBiomass();
						biomassLogWriter.print(cell.getX() + " " + cell.getY()+ " " + cell.getZ());
						for (int i=0; i<biomass.length; i++)
						{
							biomassLogWriter.print(" " + nf.format(biomass[i]));
						}
						biomassLogWriter.print("\n");
					}
					break;
			}
			biomassLogWriter.flush();
		}
	}

	/**
	 * Writes to the total biomass log if it's at the correct time point. See documentation
	 * for formats.
	 */
	private void writeTotalBiomassLog()
	{
		if (totalBiomassLogWriter != null)
		{
			NumberFormat nf = NumberFormat.getInstance();
			nf.setGroupingUsed(false);
			nf.setMaximumFractionDigits(100);
			double[] curBiomass = calculateTotalBiomass();
			
			totalBiomassLogWriter.print(currentTimePoint);
			for (int i=0; i<curBiomass.length; i++)
			{
				totalBiomassLogWriter.print("\t" + curBiomass[i]);
			}
			totalBiomassLogWriter.println();
			
			totalBiomassLogWriter.flush();
		}
	}

	/**
	 * Sets the state of a given FBACell to be either dead or alive.
	 * @param cell
	 * @param state
	 */
	public synchronized void setCellState(FBACell cell, int state)
	{
		if (state == FBACell.CELL_DEAD)
			deadCells.add(cell);
	}

	/************ VEERRRRY OLD DIFFUSION METHODS ******************/
//	/**
//	 * Here's how this works. It figures out how much each biomass component
//	 * should change in this space using the approximation model in the BacSim
//	 * paper. (Kreft et al.)
//	 * 
//	 * Instead of - like the paper does - calculating how much each set of
//	 * neighboring points should get, it calculated how much the central point
//	 * gets from each of its neighbors, along with how much that central point
//	 * should give out to each of its neighbors.
//	 * 
//	 * @param x
//	 * @param y
//	 * @return
//	 */
//	public double[] diffuseBiomassBox(int x, int y, boolean allowOverlap)
//	{
//		// System.out.println("Calcing biomass diffusion on space (" + x + ", "
//		// + y + ")");
//		double[] diff = new double[numModels];
//		// if we don't allow overlap and this site is empty of biomass,
//		// return the zero array. this case will be dealt with elsewhere.
//		if (!allowOverlap && !isOccupied(x, y))
//			return diff;
//
//		double[] xyCellBiomass = new double[numModels];
//		double[] xyCellDeltaBiomass = new double[numModels];
//		if (isOccupied(x, y))
//		{
//			xyCellBiomass = ((FBACell) getCellAt(x, y)).getBiomass();
//			xyCellDeltaBiomass = ((FBACell) getCellAt(x, y)).getDeltaBiomass();
//		}
//		int a = 0, b = 0;
//
//		/*
//		 * Simplest case! -------------- Get biomass[] and deltaBiomass[] arrays
//		 * from the cell at (x,y), and surrounding cells.
//		 * 
//		 * OUTFLOW DIFFUSION if deltaBiomass <= 0, then only the biomass[] value
//		 * is used for diffusing out.
//		 * 
//		 * if deltaBiomass > 0, then we do a 2-stage diffusion. 1.
//		 * (biomass-deltaBiomass)*model.getFlowDiffusionConstant() 2.
//		 * deltaBiomass*model.getGrowthDiffusionConstant()
//		 * 
//		 * INFLOW DIFFUSION Basically like the outflow diffusion from the point
//		 * of view of all surrounding cells.
//		 */
//		for (int i = x - 1; i <= x + 1; i++)
//		{
//			b = 0;
//			for (int j = y - 1; j <= y + 1; j++)
//			{
//				if (i != x || j != y) // ignore the center one. we diffuse to
//										// and from the outer neighborhood
//				{
//					// first, diffuse FROM (i,j)
//					if (isOccupied(i, j))
//					{
//						double[] diffCellBiomass = getCellAt(i, j).getBiomass();
//						double[] diffCellDeltaBiomass = ((FBACell) getCellAt(i,
//								j)).getDeltaBiomass();
//
//						for (int k = 0; k < numModels; k++)
//						{
//							if (canDiffuseBiomassOut(i, j, k)
//									&& canDiffuseBiomassIn(x, y, k))
//							{
//								if (diffCellDeltaBiomass[k] <= 0)
//									diff[k] += DIFFUSION_SCALE[a][b]
//											* diffCellBiomass[k]
//											* fbaModels[k].getFlowDiffusionConstant();
//								else
//								{
//									diff[k] += DIFFUSION_SCALE[a][b]
//											* (diffCellBiomass[k] - diffCellDeltaBiomass[k])
//											* fbaModels[k].getFlowDiffusionConstant();
//									diff[k] += DIFFUSION_SCALE[a][b]
//											* diffCellDeltaBiomass[k]
//											* fbaModels[k].getGrowthDiffusionConstant();
//								}
//							}
//						}
//					}
//
//					// now, diffuse TO (i,j)
//					if (isOccupied(x, y))
//					{
//						for (int k = 0; k < numModels; k++)
//						{
//							/*
//							 * if allowOverlap, then just check if it can
//							 * diffuse to. if !allowOverlap, then make sure
//							 * there's biomass there to diffuse into. --
//							 * diffusing into an empty space when overlap isn't
//							 * allowed is a special case handled by the calling
//							 * function. so, given that the diffusion can go,
//							 * allow this to proceed if either allowOverlap is
//							 * true, OR allowOverlap is false but the target
//							 * spot is occupied
//							 */
//							if ((canDiffuseBiomassIn(i, j, k) && canDiffuseBiomassOut(
//									x, y, k))
//									&& (allowOverlap || (!allowOverlap && isOccupied(
//											i, j))))
//							{
//								if (xyCellDeltaBiomass[k] <= 0)
//									diff[k] -= DIFFUSION_SCALE[a][b]
//											* xyCellBiomass[k]
//											* fbaModels[k].getFlowDiffusionConstant();
//								else
//								{
//									diff[k] -= DIFFUSION_SCALE[a][b]
//											* (xyCellBiomass[k] - xyCellDeltaBiomass[k])
//											* fbaModels[k].getFlowDiffusionConstant();
//									diff[k] -= DIFFUSION_SCALE[a][b]
//											* xyCellDeltaBiomass[k]
//											* fbaModels[k].getGrowthDiffusionConstant();
//								}
//							}
//						}
//					}
//				}
//				b++;
//			}
//			a++;
//		}
//		for (int i = 0; i < diff.length; i++)
//		{
//			diff[i] *= cParams.getDiffusionRate() * cParams.getTimeStep();
//		}
//		return diff;
//	}
//
//	public void diffuseBiomass()
//	{
//		System.out.println("Biomass pre-diffusion:");
//		double[] totalBiomass = calculateTotalBiomass();
//		for (int i = 0; i < totalBiomass.length; i++)
//		{
//			System.out.print(totalBiomass[i] + " ");
//		}
//		System.out.println("");
//
//		for (int z = 0; z < 1; /* cParams.getDiffusionsPerStep(); */z++)
//		{
//			double[][][] biomassDiffBuffer = new double[numCols][numRows][numModels];
//			for (int i = 0; i < numCols; i++)
//			{
//				for (int j = 0; j < numRows; j++)
//				{
//					// if there's something in the cell OR we allow for overlap,
//					// then calculate the buffer in one way.
//					if (isOccupied(i, j) || cParams.allowCellOverlap())
//					{
//						double[] buffer = diffuseBiomassBox(i, j, cParams
//								.allowCellOverlap());
//						for (int k = 0; k < buffer.length; k++)
//							biomassDiffBuffer[i][j][k] += buffer[k];
//					}
//					// otherwise (if that space is empty, or overlap isn't
//					// allowed)
//					// do something different.
//					else
//					{
//						/*
//						 * do something completely different. this requires
//						 * access to the deltaBiomass[][][] matrix, too. so i
//						 * guess we cram it in here.
//						 */
//
//						double[][][] deltaBox = new double[3][3][numModels];
//						/*
//						 * deltaBox will house how much of each surrounding
//						 * space will contribute to the center one. (center
//						 * should be 0).
//						 */
//
//						/*
//						 * the center space can be any. this leads to another
//						 * case - if the center is on an edge and this is a
//						 * torus, then some of the outer boxes could map to the
//						 * other side of the world.
//						 * 
//						 * i think that'll be taken care of below. hopefully.
//						 */
//
//						double[] totalDeltaBox = new double[numModels];
//						for (int a = 0; a < 3; a++)
//						{
//							for (int b = 0; b < 3; b++)
//							{
//								if ((a == b && b == 1)
//										|| !isOccupied(i + a - 1, j + b - 1))
//									continue;
//
//								double[] biomass = getCellAt(i + a - 1,
//										j + b - 1).getBiomass();
//								double[] deltaBiomass = ((FBACell) getCellAt(i
//										+ a - 1, j + b - 1)).getDeltaBiomass();
//
//								for (int m = 0; m < numModels; m++)
//								{
//									if (!canDiffuseBiomassIn(i, j, m)
//											|| !canDiffuseBiomassOut(i + a - 1,
//													j + b - 1, m))
//										continue;
//
//									if (deltaBiomass[m] <= 0)
//										deltaBox[a][b][m] = DIFFUSION_SCALE[a][b]
//												* biomass[m]
//												* fbaModels[m].getFlowDiffusionConstant();
//									else
//									{
//										deltaBox[a][b][m] = DIFFUSION_SCALE[a][b]
//												* (biomass[m] - deltaBiomass[m])
//												* fbaModels[m].getFlowDiffusionConstant();
//										deltaBox[a][b][m] = DIFFUSION_SCALE[a][b]
//												* deltaBiomass[m]
//												* fbaModels[m].getGrowthDiffusionConstant();
//									}
//									totalDeltaBox[m] += deltaBox[a][b][m];
//								}
//							}
//						}
//
//						/*
//						 * now we have a potential set of biomass flowing into
//						 * the center space. from totalDeltaBox we can find the
//						 * maximum value, and dub the center space a new cell
//						 * with that kind of biomass.
//						 * 
//						 * but for now, we'll just fill in the
//						 * deltaBiomass[][][] array with the right value, and
//						 * update the surrounding elements with the adjusted
//						 * values. remember to deal with toroidal crap!
//						 */
//						int maxIndex = 0;
//						double maxValue = totalDeltaBox[0];
//
//						for (int w = 1; w < totalDeltaBox.length; w++)
//						{
//							if (totalDeltaBox[w] > maxValue)
//							{
//								maxIndex = w;
//								maxValue = totalDeltaBox[w];
//							} else if (totalDeltaBox[w] == maxValue
//									&& Utility.randomInt(2) == 1)
//							{
//								maxIndex = w;
//							}
//						}
//						// now we have a winning value and model index.
//						// update the deltaBiomass buffer with that value
//						// and apply the changes around (i,j)
//						if (maxValue > 0)
//						{
//							biomassDiffBuffer[i][j][maxIndex] = maxValue
//									* cParams.getDiffusionRate()
//									* cParams.getTimeStep();
//
//							for (int a = 0; a < 3; a++)
//							{
//								for (int b = 0; b < 3; b++)
//								{
//									int adjX = adjustX(i + a - 1);
//									int adjY = adjustY(j + b - 1);
//									biomassDiffBuffer[adjX][adjY][maxIndex] -= deltaBox[a][b][maxIndex]
//											* cParams.getDiffusionRate()
//											* cParams.getTimeStep();
//								}
//							}
//						}
//					}
//				}
//			}
//
//			for (int i = 0; i < numCols; i++)
//			{
//				for (int j = 0; j < numRows; j++)
//				{
//					// now we have the total buffer. apply it all, and make new
//					// cells as
//					// necessary.
//					// if there's any value in deltaBiomass
//					if (Utility.hasNonzeroValue(biomassDiffBuffer[i][j]))
//					{
//						// if there's a cell there, just update it.
//						if (isOccupied(i, j))
//						{
//							// update biomass
//							Cell cell = (Cell) getCellAt(i, j);
//							cell.changeBiomass(biomassDiffBuffer[i][j]);
//						}
//						// otherwise, make a new cell there.
//						else
//						{
//							// At this point, we have the new biomass ready to
//							// rock.
//							// Make a new cell from it!
//							Cell cell = new FBACell(i, j,
//									biomassDiffBuffer[i][j], this,
//									fbaModels, cParams);
//							c.getCells().add(cell);
//						}
//					}
//				}
//			}
//		}
//		System.out.println("Biomass post-diffusion:");
//		totalBiomass = calculateTotalBiomass();
//		for (int i = 0; i < totalBiomass.length; i++)
//		{
//			System.out.print(totalBiomass[i] + " ");
//		}
//		System.out.println("");
//	}

	/**
	 * @return the number of empty spaces remaining in the FBAWorld. An empty space is 
	 * one that isn't occupied by biomass or a barrier.
	 */
	public int numEmptySpaces()
	{
		int empty = numCols * numRows;
		for (int i = 0; i < numCols; i++)
		{
			for (int j = 0; j < numRows; j++)
			{
				for (int l = 0; l < numLayers; l++)
				{
					if (isBarrier(i, j, l) || isOccupied(i, j, l))
						empty--;
				}
			}
		}
		return empty;
	}

	/**
	 * Calculates and returns the number of empty spaces in the rectangle where (x,y)
	 * is the upper left hand corner, with width w and height h.
	 * @param x
	 * @param y
	 * @param w
	 * @param h
	 * @return the number of empty spaces in the rectangle
	 * @see numEmptySpaces()
	 */
	public int numEmptySpaces(int x, int y, int z, int w, int h, int l)
	{
		int empty = w * h;
		for (int i = x; i < x + w; i++)
		{
			for (int j = y; j < y + h; j++)
			{
				for (int k = z; k < z + l; k++)
				if (isBarrier(i, j, k) || isOccupied(i, j, k))
					empty--;
			}
		}
		return empty;
	}

	@Override
	/**
	 * Makes and returns a clone of the current <code>FBAWorld</code>, cast as its parent
	 * <code>World2D</code>
	 */
	public World3D backup()
	{
		FBAWorld3D bak = new FBAWorld3D(c, mediaNames, new double[numMedia], models);
		bak.setMediaRefreshAmount(mediaRefresh);

		for (int i = 0; i < numCols; i++)
		{
			for (int j = 0; j < numRows; j++)
			{
				for (int l = 0; l < numLayers; l++)
				{
					bak.setBarrier(i, j, l, barrier[i][j][l]);
					bak.setMedia(i, j, l, media[i][j][l]);
					for (int k = 0; k < numModels; k++)
					{
						bak.setDiffuseBiomassIn(i, j, l, k, diffuseBiomassIn[i][j][l][k]);
						bak.setDiffuseBiomassOut(i, j, l, k,
								diffuseBiomassOut[i][j][l][k]);
					}
					for (int k = 0; k < numMedia; k++)
					{
						bak.setDiffuseMediaIn(i, j, l, k, diffuseMediaIn[i][j][l][k]);
						bak.setDiffuseMediaOut(i, j, l, k, diffuseMediaOut[i][j][l][k]);
					}
					if (refreshPoints[i][j][l] != null)
					{
						bak.addMediaRefreshSpace(i, j, l, refreshPoints[i][j][l].getMediaRefresh());
					}
					if (staticPoints[i][j][l] != null)
					{
						bak.addStaticMediaSpace(i, j, l, staticPoints[i][j][l].getMedia(), staticPoints[i][j][l].getStaticSet());
					}
				}
			}
		}

		return bak;
	}

	/**
	 * Changes the biomass present in space (x, y) by the values in biomassDelta. If
	 * x and y are out of range or biomassDelta has the wrong number of elements, nothing
	 * is done.
	 * @param x
	 * @param y
	 * @param biomassDelta how much to adjust the biomass of each species in space (x,y)
	 */
	public void changeBiomass(int x, int y, int z, double[] biomassDelta)
	{
		setBiomass(x, y, z, biomassDelta, true);
	}

	/**
	 * Sets the biomass in space (x, y) to be the values in biomass. If
	 * x and y are out of range or biomass has the wrong number of elements, nothing
	 * is done.
	 * @param x
	 * @param y
	 * @param biomass how much to adjust the biomass of each species in space (x,y)
	 */
	public void setBiomass(int x, int y, int z, double[] biomass)
	{
		setBiomass(x, y, z, biomass, false);
	}

	/**
	 * Sets or adjusts the biomass in space (x, y) according to the values array.
	 * If delta is true, this is a summed adjustment, otherwise the current values are
	 * overwritten.  If x and y are out of range or biomassDelta has the wrong number
	 * of elements, nothing is done. 
	 * @param x
	 * @param y
	 * @param values
	 * @param delta
	 */
	private void setBiomass(int x, int y, int z, double[] values, boolean delta)
	{
		/*
		 * if delta == true, then we're adjusting the biomass by values[].
		 * otherwise, we set the total biomass those those values.
		 */

		/*
		 * 3 cases! -------- 1. cell is present, delta doesn't set cell biomass
		 * to 0. a. FBACell.changeBiomass() returns CELL_OK. 2. cell is present,
		 * delta sets all biomass to 0. a. FBACell.changeBiomass() returns
		 * CELL_DEAD. b. remove the dead cell from cellList... or signal the
		 * Comets object to do that? 3. cell is not present, at least one
		 * biomassDelta value > 0 a. Make cell, add it to the cellList.
		 */

		if (isOccupied(x, y, z))
		{
			Cell cell = getCellAt(x, y, z);
			int status;
			if (delta)
				status = cell.changeBiomass(values);
			else
			{
				status = cell.setBiomass(values);
			}
			if (status == Cell.CELL_DEAD)
			{
				c.getCells().remove(c.getCells().indexOf(cell));
				removeCell(x, y, z);
			}
		}
		else
		{
			boolean makeCell = false;
			// only make a cell if at least one of the biomassDelta values is >
			// 0.
			// oh, and any negative values to be 0.
			for (int i = 0; i < values.length; i++)
			{
				if (values[i] < 0)
					values[i] = 0;
				else if (values[i] > 0)
					makeCell = true;
			}
			if (makeCell)
			{
				c.getCells().add(new FBACell(x, y, z, values, this, models, cParams, pParams));
			}
		}
	}

	
	/**
	 * Returns the info panel associated with this FBAWorld, showing information at 
	 * point (x, y).
	 */
	//public JComponent getInfoPanel(int x, int y, int z)
	//{
	//	updateInfoPanel(x, y, z);
	//	return infoPanel;
	//}

	/**
	 * Update the info panel to show information at space (x, y)
	 */
	//public void updateInfoPanel(int x, int y, int z)
	//{
	//	x = adjustX(x);
	//	y = adjustY(y);
	//	z = adjustZ(z);
	//	if (infoPanel == null)
	//		infoPanel = new SpaceInfoPanel(x, y, z, c.getModelNames(),
	//				mediaNames, this);
	//	else
	//		infoPanel.updateInfoPanel(x, y, z);
	//}

	/**
	 * Updates the info panel to display any newly changed information. If there is no
	 * info panel owned by this <code>FBAWorld</code>, one is created.
	 */
	//public void updateInfoPanel()
	//{
	//	if (infoPanel == null)
	//		updateInfoPanel(0, 0, 0);
	//	else
	//		infoPanel.updateInfoPanel();
	//}
	
	
	/**
	 * This inner class defines a <code>JPanel</code> that contains various information about
	 * a given space in the world. This includes whether or not there is a barrier, 
	 * how much biomass of each species is present, and how much of each medium component
	 * is present. 
	 * @author Bill Riehl briehl@bu.edu
	 */
	//private class SpaceInfoPanel extends JPanel
	//{
		/**
		 * 
		 */
	//	private static final long serialVersionUID = 9037512261938143370L;
	//	private int x, 
	//	            y;
	//	private JLabel spaceLabel, 
	//				   biomassBarrierLabel, 
	//				   mediaBarrierLabel,
	//				   mediaNameLabel,
	//				   mediaConcLabel,
	//				   mediaStaticLabel,
	//				   mediaRefreshLabel;
	//	private JLabel[] modelNameLabels, 
	//	                 biomassLabels, 
	//	                 mediaNameLabels,
	//	                 mediaValueLabels,
	//	                 staticMediaLabels,
	//	                 mediaRefreshLabels;
	//	private FBAWorld3D world;
	//	private DecimalFormat numFormat;

		/**
		 * The main constructor for the SpaceInfoPanel, it fetches information from
		 * the FBAWorld at space (x, y) and makes a pretty interface so one can see what's
		 * going on there.
		 * @param x
		 * @param y
		 * @param modelNames
		 * @param mediaNames
		 * @param world
		 */
	//	public SpaceInfoPanel(int x, int y, String[] modelNames,
	//			String[] mediaNames, FBAWorld3D world)
	//	{
	//		this.x = x;
	//		this.y = y;
	//		rebuildInfoPanel(modelNames, mediaNames, world);
	//	}

		/**
		 * Makes a new SpaceInfoPanel pointing to space (0, 0).
		 * @param modelNames
		 * @param mediaNames
		 * @param world
		 */
	//	@SuppressWarnings("unused")
	//	public SpaceInfoPanel(String[] modelNames, String[] mediaNames,	FBAWorld3D world)
	//	{
	//		this(0, 0, modelNames, mediaNames, world);
	//	}

		/**
		 * Rebuilds the SpaceInfoPanel whenever something major changes, such as the addition
		 * or removal of a model. This is also analogous to the heavy-lifting done by the
		 * constructor, as it initializes any internal Swing components it needs.
		 * @param modelNames names of all models
		 * @param mediaNames names of all media species
		 * @param world the FBAWorld this applies to
		 */
	//	public void rebuildInfoPanel(String[] modelNames, String[] mediaNames, FBAWorld3D world)
	//	{
	//		this.world = world;
	//		
	//		removeAll();
	//		boolean barrier = world.isBarrier(x, y);
	//		spaceLabel = new JLabel("Space: (" + x + ", " + y + ")",
	//				JLabel.LEFT);
	//		biomassBarrierLabel = new JLabel("Barrier space: no biomass",
	//				JLabel.LEFT);
	//		mediaBarrierLabel = new JLabel("Barrier space: no media",
	//				JLabel.LEFT);
	//		mediaNameLabel = new JLabel("Name", JLabel.CENTER);
	//		mediaConcLabel = new JLabel("Quantity (mmol)", JLabel.CENTER);
	//		mediaStaticLabel = new JLabel("Static", JLabel.CENTER);
	//		mediaRefreshLabel = new JLabel("Refresh (mmol)", JLabel.CENTER);
	//		
	//		numFormat = new DecimalFormat();
	//		numFormat.setMaximumFractionDigits(4);
	//		numFormat.setMinimumFractionDigits(1);
    //
	//		modelNameLabels = new JLabel[modelNames.length];
	//		biomassLabels = new JLabel[modelNames.length];
	//		double[] biomass = new double[modelNames.length];
	//		if (world.isOccupied(x, y))
	//			biomass = world.getCellAt(x, y).getBiomass();
	//		for (int i = 0; i < modelNames.length; i++)
	//		{
	//			if (modelNames[i] == null || modelNames[i].length() == 0)
	//				modelNames[i] = "Model " + (i + 1);
	//			modelNameLabels[i] = new JLabel(modelNames[i], JLabel.LEFT);
	//			biomassLabels[i] = new JLabel(numFormat.format(biomass[i]),
	//					JLabel.RIGHT);
	//		}
	//
	//		mediaNameLabels = new JLabel[mediaNames.length];
	//		mediaValueLabels = new JLabel[mediaNames.length];
	//		double[] media = world.getMediaAt(x, y);
	//		staticMediaLabels = new JLabel[mediaNames.length];
	//		boolean[] staticSet = world.getStaticMediaSet(x, y);
	//		mediaRefreshLabels = new JLabel[mediaNames.length];
	//		double[] mediaRefresh = world.getMediaRefreshAmount(x, y);
	//		for (int i = 0; i < mediaNames.length; i++)
	//		{
	//			mediaNameLabels[i] = new JLabel(mediaNames[i], JLabel.LEFT);
	//			mediaValueLabels[i] = new JLabel(numFormat.format(media[i]),
	//					JLabel.RIGHT);
	//			String s = "";
	//			if (staticSet[i])
	//				s = "X";
	//			staticMediaLabels[i] = new JLabel(s, JLabel.CENTER);
	//			mediaRefreshLabels[i] = new JLabel(numFormat.format(mediaRefresh[i]), JLabel.RIGHT);
	//		}
	//		
    //
	//		GridBagConstraints gbc = new GridBagConstraints();
	//		gbc.fill = GridBagConstraints.BOTH;
	//		gbc.insets = new Insets(0, 5, 0, 5);
	//		gbc.gridx = 0;
	//		gbc.gridy = 0;
    //
	//		JPanel biomassPanel = new JPanel(new GridBagLayout());
	//		biomassPanel.add(biomassBarrierLabel, gbc);
	//		biomassBarrierLabel.setVisible(barrier);
    //
	//		for (int i = 0; i < modelNameLabels.length; i++)
	//		{
	//			gbc.gridy = i;
	//			gbc.gridx = 0;
	//			biomassPanel.add(modelNameLabels[i], gbc);
	//			gbc.gridx = 1;
	//			biomassPanel.add(biomassLabels[i], gbc);
	//			modelNameLabels[i].setVisible(!barrier);
	//			biomassLabels[i].setVisible(!barrier);
	//		}
	//		biomassPanel.setBorder(BorderFactory.createTitledBorder("Biomass"));
    //
	//		JPanel mediaPanel = new JPanel(new GridBagLayout());
    //
	//		gbc.gridx = 0;
	//		gbc.gridy = 0;
	//		mediaPanel.add(mediaBarrierLabel, gbc);
	//		mediaBarrierLabel.setVisible(barrier);
	//		mediaPanel.add(mediaNameLabel, gbc);
	//		gbc.gridx = 1;
	//		mediaPanel.add(mediaConcLabel, gbc);
	//		gbc.gridx = 2;
	//		mediaPanel.add(mediaStaticLabel, gbc);
	//		gbc.gridx = 3;
	//		mediaPanel.add(mediaRefreshLabel, gbc);
	//		
	//		mediaNameLabel.setVisible(!barrier);
	//		mediaConcLabel.setVisible(!barrier);
	//		mediaStaticLabel.setVisible(!barrier);
	//		mediaRefreshLabel.setVisible(!barrier);
	//		for (int i = 0; i < mediaNameLabels.length; i++)
	//		{
	//			gbc.gridy = i+1;
	//			gbc.gridx = 0;
	//			mediaPanel.add(mediaNameLabels[i], gbc);
	//			gbc.gridx = 1;
	//			mediaPanel.add(mediaValueLabels[i], gbc);
	//			gbc.gridx = 2;
	//			mediaPanel.add(staticMediaLabels[i], gbc);
	//			gbc.gridx = 3;
	//			mediaPanel.add(mediaRefreshLabels[i], gbc);
	//			mediaNameLabels[i].setVisible(!barrier);
	//			mediaValueLabels[i].setVisible(!barrier);
	//			staticMediaLabels[i].setVisible(!barrier);
	//			mediaRefreshLabels[i].setVisible(!barrier);
	//		}
	//		// mediaPanel.setBorder(BorderFactory.createTitledBorder("Media"));
    //
	//		JScrollPane mediaScrollPane = new JScrollPane(mediaPanel);
	//		mediaScrollPane
	//				.setBorder(BorderFactory.createTitledBorder("Media"));
	//		if (mediaNameLabels.length > 8)
	//			mediaScrollPane.setMinimumSize(new Dimension(500, 400));
    //
	//		setLayout(new GridBagLayout());
	//		gbc.gridx = 0;
	//		gbc.gridy = 0;
	//		add(spaceLabel, gbc);
	//		gbc.gridy = 1;
	//		add(biomassPanel, gbc);
	//		gbc.gridy = 2;
	//		add(mediaScrollPane, gbc);
	//		revalidate();
	//	}
	//	
		/**
		 * Updates the SpaceInfoPanel to fetch information from space (x, y)
		 * @param x
		 * @param y
		 */
	//	public void updateInfoPanel(int x, int y)
	//	{
	//		this.x = x;
	//		this.y = y;
    //
	//		updateInfoPanel();
	//	}

		/**
		 * Updates the info panel to fetch any new data that might be present.
		 */
	//	public void updateInfoPanel()
	//	{
	//		spaceLabel.setText("Space: (" + x + ", " + y + ")");
    //
	//		boolean barrier = world.isBarrier(x, y);
	//		biomassBarrierLabel.setVisible(barrier);
	//		mediaBarrierLabel.setVisible(barrier);
    //
	//		double[] biomass = new double[biomassLabels.length];
	//		if (world.isOccupied(x, y))
	//			biomass = world.getCellAt(x, y).getBiomass();
	//		for (int i = 0; i < biomassLabels.length; i++)
	//		{
	//			biomassLabels[i].setText(numFormat.format(biomass[i]));
	//			biomassLabels[i].setVisible(!barrier);
	//			modelNameLabels[i].setVisible(!barrier);
	//		}
    //
	//		double[] media = world.getMediaAt(x, y);
	//		boolean[] staticSet = world.getStaticMediaSet(x, y);
	//		double[] mediaRefresh = world.getMediaRefreshAmount(x, y);
	//		for (int i = 0; i < mediaValueLabels.length; i++)
	//		{
	//			mediaValueLabels[i].setText(numFormat.format(media[i]));
	//			String s = "";
	//			if (staticSet[i])
	//				s = "X";
	//			staticMediaLabels[i].setText(s);
	//			mediaRefreshLabels[i].setText(numFormat.format(mediaRefresh[i]));
	//			mediaValueLabels[i].setVisible(!barrier);
	//			mediaNameLabels[i].setVisible(!barrier);
	//			staticMediaLabels[i].setVisible(!barrier);
	//			mediaRefreshLabels[i].setVisible(!barrier);
	//		}
	//		mediaNameLabel.setVisible(!barrier);
	//		mediaConcLabel.setVisible(!barrier);
	//		mediaStaticLabel.setVisible(!barrier);
	//		mediaRefreshLabel.setVisible(!barrier);
	//		repaint();
	//	}
    //
	//	public Dimension getPreferredSize()
	//	{
	//		return new Dimension(600, 600);
	//	}
	//}


	//public void setCircles(Set<Circle> circleSet)
	//{
	//	if (circleSet != null)
	//		this.circleSet = circleSet;
	//}
	//
	//public Set<Circle> getCircles() 
	//{
	//	return circleSet;
	//}
	
	public void setDiffusionConstants(final double[] diffConsts)
	{
		if(numMedia == diffConsts.length)
			this.nutrientDiffConsts = diffConsts;
	}
}