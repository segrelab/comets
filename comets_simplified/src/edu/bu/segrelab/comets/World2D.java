/**
 * 
 */
package edu.bu.segrelab.comets;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.JComponent;

import cern.jet.random.engine.*;
import cern.jet.random.*;

import edu.bu.segrelab.comets.util.Utility;

/**
 * The <code>World2D</code> is, along with the <code>Model</code> and <code>Cell</code> 
 * classes, the main scientific part of the COMETS program. It handles most of the 
 * simulation events, schedules running each of the <code>Models</code>, constructs and
 * deletes new and dead <code>Cells</code>, writes log file outputs, and so on.
 * <p> 
 * Many of these functions should be unique to the extending classes designed for different
 * packages, but there are a number of core functions that all <code>World2D</code> 
 * extensions must implement.
 * 
 * @author Bill Riehl briehl@bu.edu
 */
public abstract class World2D implements CometsConstants, IWorld
{
	public static final int EMPTY_SPACE = 1,
							FILLED_SPACE = 2,
							ANY_SPACE = 3;

	protected int numCols,   // number of grid columns (x)
				  numRows,   // number of grid rows (y)
				  numMedia,  // number of media elements
				  numModels; // number of currently loaded Models

	protected Comets c;						  // the World2D has a reference to Comets
	protected CometsParameters cParams;		  // ... and the current CometsParams
	protected PackageParameters pParams; 	  // ... and the current PackageParams
	protected Cell[][] cellGrid;		 	  // a 2D matrix representation of the main grid
	protected double[][][] media;		 	  // (x, y, z) refers to the level of medium component
											  // z in position (x,y)
	protected boolean[][] barrier;			  // true if that space is a barrier
	protected Model[] models;				  // reference to the list of Models
	protected String[] mediaNames;			  // list of names of all media, in order
	protected String[] initialMediaNames;	  // list of names in the order given in the input file
	protected RefreshPoint[][] refreshPoints; // grid of RefreshPoints
	protected double[] mediaRefresh;		  // amount of media to refresh across the World2D
	
	protected StaticPoint[][] staticPoints;	  // grid of StaticPoints
	protected double[] staticMedia;			  // amount of media to maintain as static
	protected boolean[] isStatic;			  // each true element i refers to a medium component
											  // that is to remain static, at a value given
											  // by staticMedia[i]
	
	protected DRand randomGenerator; //djordje

	/**
	 * The main constructor for the World2D. Every extending class should call this
	 * first.
	 * @param c	the Comets this is being used in
	 * @param numMedia the initial number of medium components, used to initialize the world
	 */
	public World2D(Comets c, int numMedia)
	{
		this.c = c;
		cParams = c.getParameters();
		pParams = c.getPackageParameters();
		numCols = cParams.getNumCols();
		numRows = cParams.getNumRows();
		this.numMedia = numMedia;
		cellGrid = new Cell[numCols][numRows];
		media = new double[numCols][numRows][numMedia];
		barrier = new boolean[numCols][numRows];
		models = c.getModels();
		mediaNames = new String[numMedia];

		mediaRefresh = new double[numMedia];
		staticMedia = new double[numMedia];
		isStatic = new boolean[numMedia];
		
		refreshPoints = new RefreshPoint[numCols][numRows];
		staticPoints = new StaticPoint[numCols][numRows];
		randomGenerator = new DRand(new java.util.Date()); // djordje
	}

	public boolean isOnGrid(int x, int y)
	{
		return (x >= 0 && x < numCols &&
				y >= 0 && y < numRows);
	}
	
	/**
	 * @return the number of columns in the World2D
	 */
	public int getNumCols()
	{
		return numCols;
	}
	
	/**
	 * @return the number of rows in the World2D
	 */
	public int getNumRows()
	{
		return numRows;
	}
	
	public int getNumModels()
	{
		return numModels;
	}

	// DJORDJE                                                                                                 
	// used when adding models in mutation                                                                     
	public void setNumModels(int newNumModels)
	{
		numModels = newNumModels;
	}
	
	/**
	 * Makes a copy of this <code>World2D</code> that doesn't share any instances
	 * with the old one. Since this requires making a new instance of a World2D 
	 * (this IS an abstract class, after all), this method is left to any 
	 * extending classes.
	 * @return a new <code>World2D</code>
	 */
	public abstract World2D backup();

	/**
	 * Calculate the total biomass present among all <code>Cells</code> in the <code>World2D</code>.
	 * @return an array of biomasses - one for each <code>Model</code> loaded
	 */
	public double[] calculateTotalBiomass()
	{		
		double[] totalBiomass = new double[numModels];
		Iterator<Cell> it = c.getCells().iterator();
		while (it.hasNext())
		{
			Cell cell = (Cell) it.next();
			double[] curBiomass = cell.getBiomass();
			
			for (int i = 0; i < curBiomass.length; i++)
			{
				totalBiomass[i] += curBiomass[i];
			}
		}
		return totalBiomass;
	}
	
	/**
	 * Changes biomass levels at space (x, y) by some delta value. 
	 * @param x - the x-coordinate
	 * @param y - the y-coordinate
	 * @param biomassDelta - the array of values to change biomass by
	 */
	public abstract void changeBiomass(int x, int y, double[] biomassDelta);

	/**
	 * Changes media levels at space (x, y) by some delta value.
	 * @param x
	 * @param y
	 * @param delta
	 * @return <code>CometsConstants.PARAMS_OK</code> if successful, 
	 * <code>CometsConstants.PARAMS_ERROR</code> if the delta array is the wrong length, 
	 * and <code>CometsConstants.BOUNDS_ERROR</code> if the location is out of bounds.
	 */
	public int changeMedia(int x, int y, double[] delta)
	{
		if (delta.length != numMedia)
			return PARAMS_ERROR;
		if (isOnGrid(x, y))
		{
			for (int i = 0; i < delta.length; i++)
			{
				media[x][y][i] += delta[i];
				if (media[x][y][i] < 0)
					media[x][y][i] = 0;
			}
			return PARAMS_OK;
		}
		else
			return BOUNDS_ERROR;
	}


	/**
	 * Does some cleanup and ends the <code>World2D's</code> simulation engine.
	 */
	public abstract void endSimulation();
	
	/**
	 * Returns the biomass at location (x, y).
	 * @param x - the x-coordinate
	 * @param y - the y-coordinate
	 * @return an array of biomass present at that location, one element for each <code>Model</code>
	 * loaded
	 */
	public double[] getBiomassAt(int x, int y)
	{
		if (isOccupied(x, y))
			return getCellAt(x, y).getBiomass();
		else
			return new double[numModels];
	}

	/**
	 * Returns a 3D matrix with the levels of biomass in every spot that contains a Cell.
	 * @return a 3D matrix
	 */
	public double[][][] getBiomass()
	{
		Iterator<Cell> it = c.getCells().iterator();
		double[][][] biomass = new double[numCols][numRows][numModels];
		while (it.hasNext())
		{
			Cell cell = it.next();
			double[] cellBiomass = cell.getBiomass();
			int x = cell.getX();
			int y = cell.getY();
			for (int z=0; z<numModels; z++)
			{
				biomass[x][y][z] = cellBiomass[z];
			}
		}
		return biomass;
	}
	
	/**
	 * Returns the entire 3D media matrix that this <code>World2D</code> is currently holding.
	 * The first dimension is the columns (x), the second is the rows (y), and the third is
	 * the media array at that point.
	 * @return a 3D array of media.
	 */
	public double[][][] getAllMedia()
	{
		return media;
	}
	
	/**
	 * Returns the <code>Cell</code> at point (x, y) or <code>null</code> if there's nothing
	 * there.
	 * @param x
	 * @param y
	 * @return either a <code>Cell</code> or <code>null</code>
	 */
	public Cell getCellAt(int x, int y)
	{
		if (isOnGrid(x, y))
			return cellGrid[x][y];
		else
			return null;
	}

	/**
	 * Gets the media array at point (x, y). If the coordinate (x, y) is out of bounds,
	 * this array will be full of zeros.
	 * @param x
	 * @param y
	 * @return a double array of media components
	 */
	public double[] getMediaAt(int x, int y)
	{
		if (isOnGrid(x, y))
			return media[x][y];
		else
			return null;
	}
	
	public double[] getMediaAt(int x, int y, int z){
		if (z > 0){
			throw new IllegalArgumentException("Attempted to lookup media in a position where z (" + 
					Integer.valueOf(z).toString() + ") > 0, but the world is 2D.");
		}
		return getMediaAt(x,y);
	}

	/**
	 * @return the number of nutrient components in the currently loaded media
	 */
	public int getNumMedia()
	{
		return numMedia;
	}
	
	/**
	 * Initializes the <code>World2D's</code> simulation engine.
	 */
	public abstract void initSimulation();
	
	/**
	 * Returns true if the point (x, y) is either barrier or out of bounds.
	 * @param x
	 * @param y
	 * @return
	 */
	public boolean isBarrier(int x, int y)
	{
		if (isOnGrid(x, y))
			return barrier[x][y];
		else
			return true;
	}
	
	/**
	 * Returns true if there is a <code>Cell</code> at (x, y). Like <code>getCellAt(x, y) != null;</code>
	 * @param x
	 * @param y
	 * @return true if there is a <code>Cell</code> at (x, y).
	 */
	public boolean isOccupied(int x, int y)
	{
		if (isOnGrid(x, y))
			return cellGrid[x][y] != null;
		else
			return false;
	}

	/**
	 * Tells the <code>World2D</code> to refresh the amount of media it knows to refresh, where
	 * its supposed to refresh it.
	 */
	public void refreshMedia()
	{
		refreshMedia(mediaRefresh);
	}

	/**
	 * Tells the <code>World2D</code> to refresh the media in every space in the world by the
	 * amounts given in delta.
	 * 
	 * If delta doesn't have the same length as the number of medium components, nothing is done.
	 * @param delta the amount to change each media component
	 */
	public void refreshMedia(double[] delta)
	{
		if (delta == null || delta.length != numMedia)
			return;

		for (int i = 0; i < numCols; i++)
		{
			for (int j = 0; j < numRows; j++)
			{
				for (int k = 0; k < numMedia; k++)
				{
					media[i][j][k] += delta[k];
					if (media[i][j][k] < 0)
						media[i][j][k] = 0;
				}
				if (refreshPoints[i][j] != null)
				{
					changeMedia(i, j, refreshPoints[i][j].getMediaRefresh());
				}
			}
		}
	}
	
	/**
	 * This is the main thing that the <code>World2D</code> should do - run a simulation. Each
	 * time this method is invoked, a run cycle is performed, and a return value appropriate
	 * to the model is returned. Often this will be either PARAMS_OK or PARAMS_ERROR.
	 * <p>
	 * The details of running a cycle are left to the implementing package.
	 * @return some return value after running.
	 */
	public abstract int run();
	
	/**
	 * This sets the space at (x, y) to either be a barrier or not, according to the
	 * barrier variable.
	 * @param x
	 * @param y
	 * @param barrier - true if the space at (x, y) should be a barrier
	 * @return <code>CometsConstants.PARAMS_OK</code> if successful 
	 * and <code>CometsConstants.BOUNDS_ERROR</code> if the location is out of bounds.
	 */
	public int setBarrier(int x, int y, boolean b)
	{
		if (isOnGrid(x, y))
		{
			barrier[x][y] = b;
			return PARAMS_OK;			
		}
		else
			return BOUNDS_ERROR;
	}
	
	/**
	 * Sets the biomass value to values given by biomassDelta. Note that this <b>sets</b> the
	 * values, it doesn't add or subtract them. If biomassDelta is the wrong length, nothing
	 * is done.
	 * 
	 * This is left to any extending class - if there was no biomass there to begin with,
	 * that would necessitate making a new Cell, which is dealt with differently by different
	 * packages.
	 * 
	 * @param x
	 * @param y
	 * @param biomassDelta
	 */
	public abstract void setBiomass(int x, int y, double[] biomassDelta);
	
	/**
	 * Sets the media values at (x, y) to the values in delta. Note that this <b>sets</b> the
	 * values, it doesn't add or subtract them. 
	 * @param x
	 * @param y
	 * @param delta
	 * @return <code>CometsConstants.PARAMS_OK</code> if successful, 
	 * <code>CometsConstants.PARAMS_ERROR</code> if the delta array is the wrong length, 
	 * and <code>CometsConstants.BOUNDS_ERROR</code> if the location is out of bounds.
	 */
	public int setMedia(int x, int y, double[] delta)
	{
		if (delta.length != numMedia)
			return PARAMS_ERROR;
		if (isOnGrid(x, y))
		{
			for (int i = 0; i < delta.length; i++)
			{
				media[x][y][i] = delta[i];
				if (media[x][y][i] < 0)
					media[x][y][i] = 0;
			}
			return PARAMS_OK;
		}
		else
			return BOUNDS_ERROR;
	}
	
	public int setMedia(int x, int y, int z, double[] delta){
		if (z > 0)
			return BOUNDS_ERROR;
		else return setMedia(x,y,delta);
	}
	
	/**
	 * Updates the world. This should be called whenever something major and structural has
	 * been done - resizing the number of rows and columns, or loading or removing a model, 
	 * for example.
	 * 
	 * This base version covers changes to the world done when the number of rows and/or 
	 * columns changes. Extending classes should cover everything else.
	 * @return PARAMS_OK if everything gets updated okay.
	 */
	public int updateWorld()
	{
		//might be a bit redundant, but...
		this.cParams = c.getParameters();
		if (cParams.getNumRows() != numRows || cParams.getNumCols() != numCols)
		{
			// update all the parameters that deal with the grid.
			// this'll be fun.
			int newNumRows = cParams.getNumRows();
			int newNumCols = cParams.getNumCols();
			
			Cell[][] newCellGrid = new Cell[newNumCols][newNumRows];
			double[][][] newMedia = new double[newNumCols][newNumRows][numMedia];
			boolean[][] newBarrier = new boolean[newNumCols][newNumRows];
			RefreshPoint[][] newRefreshPoints = new RefreshPoint[newNumCols][newNumRows];
			StaticPoint[][] newStaticPoints = new StaticPoint[newNumCols][newNumRows];
			
			int minRows = Math.min(numRows, newNumRows);
			int minCols = Math.min(numCols, newNumCols);
			for (int i=0; i<minCols; i++)
			{
				for (int j=0; j<minRows; j++)
				{
					newCellGrid[i][j] = cellGrid[i][j];
					newBarrier[i][j] = barrier[i][j];
					newRefreshPoints[i][j] = refreshPoints[i][j];
					newStaticPoints[i][j] = staticPoints[i][j];
					for (int k=0; k<numMedia; k++)
					{
						newMedia[i][j][k] = media[i][j][k];
					}
				}
			}
			
			if (newNumCols < numCols)
			{
				// remove the extra stuff on the x-axis
				for (int i=newNumCols; i<numCols; i++)
				{
					for (int j=0; j<numRows; j++)
					{
						Cell cell = removeCell(i, j);
						if (cell != null)
							c.getCells().remove(cell);
					}
				}
			}
			if (newNumRows < numRows)
			{
				// remove the extra stuff on the y-axis
				for (int i=0; i<numCols-1; i++)
				{
					for (int j=newNumRows; j<numRows; j++)
					{
						Cell cell = removeCell(i, j);
						if (cell != null)
							c.getCells().remove(cell);
					}
				}
			}
			
			cellGrid = newCellGrid;
			barrier = newBarrier;
			refreshPoints = newRefreshPoints;
			staticPoints = newStaticPoints;
			media = newMedia;
			
			numCols = newNumCols;
			numRows = newNumRows;
		}
		return PARAMS_OK;
	}
	
	/**
	 * Removes a <code>Cell</code> from the grid at position (x, y), and returns it to the user.
	 * If there is no <code>Cell</code> at (x, y) (or that point is out of bounds), <code>null</code>
	 * is returned
	 * @param x the column from which to remove the <code>Cell</code>
	 * @param y the row from which to remove the <code>Cell</code>
	 * @return the <code>Cell</code> removed from the grid
	 */
	public Cell removeCell(int x, int y)
	{
		if (isOnGrid(x, y))
		{
			Cell cell = cellGrid[x][y];
			cellGrid[x][y] = null;
			return cell;			
		}
		else
			return null;
	}

	/**
	 * Gets a <code>JComponent</code> with information about the world at point (x, y).
	 * Note that this component is stored internally, too. Instead of making multiple calls
	 * to getInfoPanel(int, int), it is best to make a single call, then multiple calls to
	 * updateInfoPanel(int, int) or updateInfoPanel().
	 * @param x
	 * @param y
	 * @return a <code>JComponent</code> with information of the space at (x, y).
	 * @see #updateInfoPanel()
	 * @see #updateInfoPanel(int, int)
	 */
	public abstract JComponent getInfoPanel(int x, int y);
	
	/**
	 * Updates the internal information panel to show information at (x, y). Note that
	 * because the actual object is owned by this <code>World2D</code>, any pointer to that
	 * panel (e.g., fetched through getInfoPanel(int, int)) will also be updated. 
	 * @param x
	 * @param y
	 */
	public abstract void updateInfoPanel(int x, int y);
	
	/**
	 * Updates the internal information panel to refresh the information shown at the current
	 * point. This should also update any pointers to the information panel.
	 */
	public abstract void updateInfoPanel();
	
	/**
	 * Destroys the current <code>World2D</code> by removing any package-specific necessities.
	 * For example, the <code>FBAWorld.destory()</code> kills all running <code>FBARunThreads</code>.
	 */
	public abstract void destroy();
	
	/**
	 * Implementation is left to extending packages, since each different world will need to deal
	 * with this in a separate way.
	 * @param oldModels
	 * @param newModels
	 */
	public abstract void changeModelsInWorld(Model[] oldModels, Model[] newModels);

	/**
	 * Flags a space for containing static media within the World2D. The coordinates, media,
	 * and indices of media to be made static are given in the <code>StaticPoint</code>.
	 * 
	 * If the number of either media components or media indices are out of bounds, a
	 * <code>PARAMS_ERROR</code> is returned.
	 * 
	 * If the location given by the StaticPoint is out of bounds, a <code>BOUNDS_ERROR</code>
	 * is returned.
	 * 
	 * Otherwise, it returns <code>PARAMS_OK</code>
	 * @param sp
	 * @return see above
	 */
	public int addStaticMediaSpace(StaticPoint sp)
	{
		if (staticPoints[(int)sp.getX()][(int)sp.getY()] != null)
			return addStaticMediaSpace((int)sp.getX(), (int)sp.getY(), sp.getMedia(), sp.getStaticSet());
		else if (sp.getMedia().length != numMedia || sp.getStaticSet().length != numMedia)
			return PARAMS_ERROR;
		else if (isOnGrid(sp.getX(), sp.getY()))
		{
			staticPoints[(int)sp.getX()][(int)sp.getY()] = sp;
			return PARAMS_OK;			
		}
		else
			return BOUNDS_ERROR;
	}
	
	/**
	 * Flags the space (x, y) to contain static media levels, given in <code>staticMedia</code>.
	 * The given boolean indices correspond to which media are to be static.
	 * 
	 * If the number of either media components or media indices are out of bounds, a
	 * <code>PARAMS_ERROR</code> is returned.
	 * 
	 * If the location given by (x, y) is out of bounds, a <code>BOUNDS_ERROR</code>
	 * is returned.
	 * 
	 * Otherwise, it returns <code>PARAMS_OK</code>
	 * @param x
	 * @param y
	 * @param staticMedia
	 * @param isStatic
	 * @return see above
	 */
	public int addStaticMediaSpace(int x, int y, double[] staticMedia, boolean[] isStatic)
	{
		if (staticMedia.length != numMedia || staticMedia.length != isStatic.length)
			return PARAMS_ERROR;
		if (isOnGrid(x, y))
		{
			if (staticPoints[x][y] != null)
			{
				double[] curStaticMedia = staticPoints[x][y].getMedia();
				boolean[] curStatic = staticPoints[x][y].getStaticSet();
				for (int i=0; i<staticMedia.length; i++)
				{
					staticMedia[i] += curStaticMedia[i];
					isStatic[i] = isStatic[i] || curStatic[i];
				}
			}
			staticPoints[x][y] = new StaticPoint(x, y, staticMedia, isStatic);
			return PARAMS_OK;
		}
		else
			return BOUNDS_ERROR;
	}

	/**
	 * Removes the static media space at point (x, y) if there is one present.
	 * @param x
	 * @param y
	 */
	public void removeStaticMediaSpace(int x, int y)
	{
		if (isOnGrid(x, y))
			staticPoints[x][y] = null;
	}

	/**
	 * Returns the <code>StaticPoint</code> at grid point (x, y) if one is present.
	 * If there is no media kept static at (x, y), <code>null</code> is returned
	 * @param x
	 * @param y
	 * @return a <code>StaticPoint</code> or <code>null</code>
	 */
	public StaticPoint getStaticMediaSpace(int x, int y)
	{
		if (isOnGrid(x, y))
			return staticPoints[x][y];
		else
			return null;
	}
	
	/**
	 * Returns a <code>List%lt;StaticPoint%gt;</code> of all static media spaces in the given
	 * <code>World2D</code>. If there aren't any, then it returns an empty <code>List</code> 
	 * @return a <code>List%lt;StaticPoint%gt;</code> of all <code>StaticPoints</code> in the world.
	 */
	public List<StaticPoint> getStaticMediaSpaces()
	{
		List<StaticPoint> points = new ArrayList<StaticPoint>();
		for (int i=0; i<numCols; i++)
		{
			for (int j=0; j<numRows; j++)
			{
				if (staticPoints[i][j] != null)
					points.add(staticPoints[i][j]);
			}
		}
		return points;
	}
	
	/**
	 * Returns a <code>List%lt;RefreshPoint%gt;</code> of all media refresh spaces in the given
	 * <code>World2D</code>. If there aren't any, then it returns an empty <code>List</code> 
	 * @return a <code>List%lt;RefreshPoint%gt;</code> of all <code>RefreshPoints</code> in the world.
	 */
	public List<RefreshPoint> getMediaRefreshSpaces()
	{
		List<RefreshPoint> points = new ArrayList<RefreshPoint>();
		for (int i=0; i<numCols; i++)
		{
			for (int j=0; j<numRows; j++)
			{
				if (refreshPoints[i][j] != null)
					points.add(refreshPoints[i][j]);
			}
		}
		return points;
	}

	/**
	 * Applies the static media change to the <code>World2D</code>. Any spaces that contain a 
	 * <code>StaticPoint</code> will change the media in that space to what is in the static
	 * point.
	 */
	public void applyStaticMedia()
	{
		for (int i=0; i<numCols; i++)
		{
			for (int j=0; j<numRows; j++)
			{
				for (int k=0; k<isStatic.length; k++)
				{
					if (isStatic[k])
						media[i][j][k] = staticMedia[k];
				}
				if (staticPoints[i][j] != null)
				{
					double[] conc = staticPoints[i][j].getMedia();
					boolean[] staticSet = staticPoints[i][j].getStaticSet();
					for (int k=0; k<conc.length; k++)
					{
						if (staticSet[k])
							media[i][j][k] = conc[k];
					}
				}
			}
		}
	}

	/**
	 * Adds a media refresh space to the world. The media levels at this point will refresh
	 * according to the information contained in the passed <code>RefreshPoint</code>.
	 * 
	 * If the number of either media components or media indices are out of bounds, a
	 * <code>PARAMS_ERROR</code> is returned.
	 * 
	 * If the location given by the StaticPoint is out of bounds, a <code>BOUNDS_ERROR</code>
	 * is returned.
	 * 
	 * Otherwise, it returns <code>PARAMS_OK</code>
	 * @param rp the <code>RefreshPoint</code> to add to the <code>World2D</code>
	 * @return see above
	 */
	public int addMediaRefreshSpace(RefreshPoint rp)
	{
		if (rp.getMediaRefresh().length != numMedia)
			return PARAMS_ERROR;
		if (rp.getX() < 0 || rp.getX() >= numCols || rp.getY() < 0 || rp.getY() >= numRows)
			return BOUNDS_ERROR;
		refreshPoints[(int)rp.getX()][(int)rp.getY()] = rp;
		return PARAMS_OK;
	}
	
	/**
	 * Adds a media refresh space to the world. The media levels at grid space (x, y) will 
	 * refresh according to the information contained in the passed data.
	 * 
	 * If the number of either media components or media indices are out of bounds, a
	 * <code>PARAMS_ERROR</code> is returned.
	 * 
	 * If the location given by (x, y) is out of bounds, a <code>BOUNDS_ERROR</code>
	 * is returned.
	 * 
	 * Otherwise, it returns <code>PARAMS_OK</code>
	 * @param x
	 * @param y
	 * @param mediaRefresh the amount of each media component to be replenished at this point
	 * @return see above
	 */
	public int addMediaRefreshSpace(int x, int y, double[] mediaRefresh)
	{
//		System.out.println("adding media refresh point at " + x + ", " + y);
		if (mediaRefresh.length != numMedia)
			return PARAMS_ERROR;
		if (isOnGrid(x, y))
		{
			refreshPoints[x][y] = new RefreshPoint(x, y, mediaRefresh);
			return PARAMS_OK;
		}
		else
			return BOUNDS_ERROR;
	}

	/**
	 * Removes the media refresh point from the given space if one is present. 
	 * @param x
	 * @param y
	 */
	public void removeMediaRefreshSpace(int x, int y)
	{
		if (isOnGrid(x, y))
			refreshPoints[x][y] = null;
	}

	/**
	 * Returns the <code>RefreshPoint</code> at (x, y) if one is present. If not,
	 * or if (x, y) is out of bounds, it returns <code>null</code>.
	 * @param x
	 * @param y
	 * @return either a <code>RefreshPoint</code> or <code>null</code>
	 */
	public RefreshPoint getMediaRefreshSpace(int x, int y)
	{
		if (isOnGrid(x, y))
			return refreshPoints[x][y];
		else
			return null;
	}
	
	/**
	 * This returns true if either the entire world is set to have its media refreshed,
	 * or if there is a <code>RefreshPoint</code> at the given location. If the space is
	 * out of bounds, this returns false.
	 * @param x
	 * @param y
	 * @return
	 */
	public boolean isMediaRefreshSpace(int x, int y)
	{
		if (isOnGrid(x, y))
			return (Utility.sum(mediaRefresh) > 0 || refreshPoints[x][y] != null);
		else
			return false;
	}
	
	/**
	 * This returns true if either the entire world has some static media component,
	 * or if there is a <code>StaticPoint</code> at the given location. If the space is
	 * out of bounds, this returns false. 
	 * @param x
	 * @param y
	 * @return
	 */
	public boolean isStaticMediaSpace(int x, int y)
	{
		if (isOnGrid(x, y))
			return (Utility.hasTrue(isStatic) || staticPoints[x][y] != null);
		else
			return false;
	}
	
	/**
	 * This returns the set of media to be refreshed at the given point, if any. If
	 * not (or if (x, y) are out of bounds), then it returns an array of zeros the 
	 * length of the number of medium components.
	 * 
	 * If the entire world is set to have its media refreshed, then the sum of the 
	 * world refresh media and the specific point refresh media is returned.
	 * @param x
	 * @param y
	 * @return
	 */
	public double[] getMediaRefreshAmount(int x, int y)
	{
		if (isOnGrid(x, y))
		{
			double[] conc = new double[numMedia];
			double[] rpConc = new double[numMedia];
			if (refreshPoints[x][y] != null)
				rpConc = refreshPoints[x][y].getMediaRefresh();
			for (int i=0; i<numMedia; i++)
				conc[i] = mediaRefresh[i] + rpConc[i];
			return conc;
		}
		else
			return new double[numMedia];

	}

	/**
	 * This returns the boolean indices for which medium components are set to 
	 * be maintained as static values at the given point. Note that this is inclusive
	 * with the <code>World2D</code>-wide static media set. To get the set specific to
	 * this point, @see getStaticMediaSpace(x, y)
	 * 
	 * If (x, y) is out of bounds, this returns a boolean[] array of falses the length
	 * of the media set.
	 * @param x
	 * @param y
	 * @return
	 */
	public boolean[] getStaticMediaSet(int x, int y)
	{
		if (isOnGrid(x, y))
		{
			boolean[] set = new boolean[numMedia];
			boolean[] spSet = new boolean[numMedia];
			if (staticPoints[x][y] != null)
				spSet = staticPoints[x][y].getStaticSet();

			for (int i=0; i<numMedia; i++)
				set[i] = isStatic[i] || spSet[i];
			
			return set;			
		}
		else
			return new boolean[numMedia];
	}

	/**
	 * Sets the total amount of media to refresh across the world. This is cumulative
	 * with any per-point media refresh (i.e., set by a <code>RefreshPoint</code>)
	 * @param delta the values of all media to be refreshed.
	 * @return <code>PARAMS_OK</code> if delta has one element for each medium component,
	 * <code>PARAMS_ERROR</code> otherwise
	 */
	public int setMediaRefreshAmount(double[] delta)
	{
		if (delta.length != numMedia)
			return PARAMS_ERROR;
		for (int i=0; i < mediaRefresh.length; i++)
			mediaRefresh[i] = delta[i];
		return PARAMS_OK;
	}

	/**
	 * Sets the media to be maintained as static throughout the <code>World2D</code>. Each
	 * value in <code>staticAmt</code> is applied to the medium component of the same index,
	 * if the same index of <code>globalStatic</code> = true.
	 * @param staticAmt
	 * @param globalStatic
	 * @return <code>PARAMS_OK</code> if both <code>staticAmt</code> and <code>globalStatic</code>
	 * have one element for each medium component, <code>PARAMS_ERROR</code> otherwise.
	 */
	public int setGlobalStaticMedia(double[] staticAmt, boolean[] globalStatic)
	{
		if (staticAmt.length != numMedia || globalStatic.length != numMedia)
			return PARAMS_ERROR;
		for (int k=0; k<staticMedia.length; k++)
		{
			staticMedia[k] = staticAmt[k];
			isStatic[k] = globalStatic[k];
		}
		applyStaticMedia();
		return PARAMS_OK;
	}
	
	/**
	 * @return an array marking the amount of each media component to keep static throughout
	 * the world.
	 */
	public double[] getStaticMediaAmount()
	{
		return staticMedia;
	}
	
	/**
	 * @return an array marking the indices of each media component to keep static throughout the world. 
	 * Each true index corresponds with a static media component.
	 */
	public boolean[] getStaticMediaSet()
	{
		return isStatic;
	}
	
	/**
	 * @return an array marking the amount of each media component to refresh throughout the
	 * <code>World2D</code>
	 */
	public double[] getMediaRefreshAmount()
	{
		return mediaRefresh;
	}
	/**
	 * dilutes the media world-wide by a global dilution coefficient, which is supplied
	 * in the parameters file as metaboliteDilution
	 */
	public void applyMetaboliteDilution()
	{
		double metaboliteDilutionRate = cParams.getMetaboliteDilutionRate();
		if (metaboliteDilutionRate == 0.0)
		{
			return; // don't bother wasting cpu if no dilution applied
		}
		double dt = cParams.getTimeStep();
		for (int i = 0; i < numCols; i++)
		{
			for (int j = 0; j < numRows; j++)
			{
				for (int k = 0; k < numMedia; k++)
				{
					media[i][j][k] -= media[i][j][k] * metaboliteDilutionRate * dt;
					if (media[i][j][k] < 0)
						media[i][j][k] = 0;
				}
			}
		}
	}
	
	/**
	 * Puts the given <code>Cell</code> into the world at (x, y)
	 * @param x
	 * @param y
	 * @param cell
	 * @return PARAMS_OK if all works out, BOUNDS_ERROR if the world is non-toroidal and
	 * x and y are out of range
	 */
	public int putCellAt(int x, int y, Cell cell)
	{
		if (cParams.isToroidalGrid())
		{
			x = adjustX(x);
			y = adjustY(y);
		}
		if (isOnGrid(x, y))
		{
			cellGrid[x][y] = cell;
			return PARAMS_OK;
		}
		else
			return BOUNDS_ERROR;
	}
	
	/**
	 * Adjusts the x value in one of two ways. If the world is being modeled as a torus, and
	 * x < 0 or x > the number of columns, it reorients x so that it is within those bounds.
	 * Otherwise, if x < 0, it returns 0; and if x > the number of columns - 1, it returns 
	 * numCols-1
	 * @param x
	 * @return an adjusted x value
	 */
	public int adjustX(int x)
	{
		if (cParams.isToroidalGrid())
		{
			while (x < 0)
				x += numCols;
			while (x >= numCols)
				x -= numCols;
		} 
		else
		{
			if (x < 0)
				x = 0;
			if (x >= numCols)
				x = numCols - 1;
		}
		return x;
	}

	/**
	 * Adjusts the y value in one of two ways. If the world is being modeled as a torus, and
	 * y < 0 or y > the number of rows, it reorients y so that it is within those bounds.
	 * Otherwise, if y < 0, it returns 0; and if y > the number of rows - 1, it returns 
	 * numRows-1
	 * @param y
	 * @return an adjusted y value
	 */
	public int adjustY(int y)
	{
		if (cParams.isToroidalGrid())
		{
			while (y < 0)
				y += numRows;
			while (y >= numRows)
				y -= numRows;
		} 
		else
		{
			if (y < 0)
				y = 0;
			if (y >= numRows)
				y = numRows - 1;
		}
		return y;
	}
	
	/**
	 * Adds (or subtracts) a random amount of noise up to the given percentage of 
	 * media present in each grid space. Note that "maxFraction" represents the maximum
	 * fraction of media that can be added or removed from each space, and must be
	 * a value between 0 and 1.
	 * 
	 * E.g. If the <code>percent</code> parameter is 0.1, then a random value between
	 * -0.1 and 0.1 is chosen, and that much more of a type of media is added to the space.
	 * @param percent
	 */
	public void addMediaNoise(double maxFraction)
	{
		maxFraction = Math.abs(maxFraction);
		if (maxFraction == 0 || maxFraction > 1)
			return;
		
		for (int i=0; i<numRows; i++)
		{
			for (int j=0; j<numCols; j++)
			{
				for (int k=0; k<numMedia; k++)
				{
					if (media[i][j][k] > 0)
					{
						media[i][j][k] += media[i][j][k] * maxFraction * (Utility.randomDouble() * 2 - 1);
					}
				}
			}
		}
	}
	
	public void addBiomassNoise(double maxFraction)
	{
		maxFraction = Math.abs(maxFraction);
		if (maxFraction == 0 || maxFraction > 1)
			return;
		
		for (int i=0; i<numRows; i++)
		{
			for (int j=0; j<numCols; j++)
			{
				if (isOccupied(i, j))
				{
					double[] biomass = getCellAt(i,j).getBiomass();
					for (int k=0; k<biomass.length; k++)
					{
						if (biomass[k] > 0)
						{
							biomass[k] += biomass[k] * maxFraction * (Utility.randomDouble() * 2 - 1);
						}
					}
					getCellAt(i,j).setBiomass(biomass);
				}
			}
		}
	}
	
	public int[] getDims(){
		return new int[]{numCols, numRows, 1};
	}
	
	/**
	 * @return A <code>String</code> array of names for each nutrient in the media. This
	 * will be in the same order at the other various media access methods.
	 * @see #getAllMedia()
	 * @see #getMediaAt(int, int)
	 */
	public String[] getMediaNames(){
		return mediaNames;
	}
	
	public Comets getComets(){
		return c;
	}
	
	public void runExternalReactions(){
		//if there's nothing to do, just return
		if (reactionModel.getNrxns() < 1) return;
		else reactionModel.run(); //the ReactionModel handles updating this world's media
	}

	// This function samples cells from a population (expressed as number of
	// cells), given a probability.Internally, it uses binomial when cell
	// numbers
	// are small, and Poisson otherwise.
	// needs import cern.jet.random.engine.*;
	// needs import cern.jet.random.*;
	public int samplePopulation(int population, double prob) 
	{
		double lambda = population * prob;
		Poisson pois;
		Binomial binom;
		int nmut;
		
		if (lambda > 10) {
			pois = new Poisson(lambda, randomGenerator);
			nmut = pois.nextInt();
		} else {
			binom = new Binomial(population, prob, randomGenerator);
			nmut = binom.nextInt();
		}
		return nmut;
	}

	public void setInitialMediaNames(String[] arr){initialMediaNames = arr;}
	
	public String[] getInitialMediaNames(){return initialMediaNames;}

	public boolean is3D(){return false;}
}
