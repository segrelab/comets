package edu.bu.segrelab.comets;

/**
 * The basic, abstract <code>Cell</code> class for use with the COMETS program. As this is
 * an abstract class, actual <code>Cells</code> for use in COMETS shouldn't instantiate
 * this class directly, but only an extension class that is part of a loaded package 
 * (e.g. <code>FBACell</code>).
 * @see edu.bu.segrelab.comets.fba.FBACell
 * @author Bill Riehl briehl@bu.edu
 */
public abstract class Cell implements CometsConstants
{
	public static final int CELL_OK = 1;
	public static final int CELL_DEAD = -1;
	public static int NEXT_CELL_ID = 0;

	protected int x;
	protected int y;
	protected int z;
	protected String cellType;

	/**
	 * Tells the <code>Cell</code> to run an internal process and return some value. This process
	 * can range from an FBA calculation, changing state based on the state of neighboring
	 * <code>Cells</code>, etc.
	 * @return an integer value after it is finished
	 */
	public abstract int run();

	/** 
	 * @return an integer identifier for the <code>Cell</code>.
	 */
	public abstract int getID();

	/**
	 * @return the color of the <code>Cell</code>, packed into a single integer.
	 */
	public abstract int getColor();

	/**
	 * Generates a <code>String</code> containing the attributes of the <code>Cell</code>.
	 * These may include its growth rate, movement rate, reproduction rate, etc.
	 * @return an attributes <code>String</code>.
	 */
	public abstract String attributesString();

	/**
	 * Generates a <code>String</code> containing the current statistics of the <code>Cell</code>.
	 * These may include its position in the world, amount of energy and food it has, etc.
	 * @return a statistics <code>String</code>.
	 */
	public abstract String statisticsString();

	/**
	 * Returns the array of biomasses for each <code>Model</code> that may be represented
	 * by the <code>Cell</code>. It is expected that each <code>Cell</code> would have access
	 * to each <code>Model</code>, so a double array is returned with values corresponding to
	 * the elements of the ordered <code>Model</code> array owned by the <code>Comets</code>
	 * object.
	 * @return a double array of biomass values.
	 */
	public abstract double[] getBiomass();
	
	public abstract double[] getDeltaBiomass();

	
	/**
	 * Modify the biomass of the <code>Cell</code> by an array of delta values, where each
	 * element <i>i</i> corresponds to the <i>i</i>th <code>Model</code> currently loaded. 
	 * @param delta - an array of values to modify each biomass component by.
	 * @return an integer implying the success or failure of the operation
	 */
	public abstract int changeBiomass(double[] delta);

	/**
	 * Returns a backup of this <code>Cell</code>, corresponding to the <code>World2D</code> 
	 * being backed up in parallel.
	 * <p>
	 * The usage here is to create a new <code>Cell</code> that exists in the backupWorld
	 * parameter, and not in its current world.
	 * @param backupWorld 
	 * @return a copy of the <code>Cell</code>
	 */
	public abstract Cell backup(World2D backupWorld);

	/**
	 * Returns a backup of this <code>Cell</code>, corresponding to the <code>World2D</code> 
	 * being backed up in parallel.
	 * <p>
	 * The usage here is to create a new <code>Cell</code> that exists in the backupWorld
	 * parameter, and not in its current world.
	 * @param backupWorld 
	 * @return a copy of the <code>Cell</code>
	 */
	public abstract Cell backup(World3D backupWorld);
	
	/**
	 * @return the current x-coordinate of the <code>Cell</code> on the grid.
	 */
	public int getX() { return x; }

	/**
	 * @return the current y-coordinate of the <code>Cell</code> on the grid.
	 */
	public int getY() { return y; }
	
	/**
	 * @return the current z-coordinate of the <code>Cell</code> on the grid.
	 */
	public int getZ() { return z; }

	/**
	 * @return a <code>String</code> detailing what type of <code>Cell</code> this is.
	 */
	public String cellType() { return cellType; }

	/**
	 * @return a new unique identifier for this <code>Cell</code>.
	 */
	public static int getNewCellID() 
	{ 
		return NEXT_CELL_ID++; 
	}

	/**
	 * Changes the biomass of the <code>Cell</code> to a new array of values, where each
	 * element <i>i</i> corresponds to the <i>i</i>th <code>Model</code> currently loaded. 
	 * @param values - an array of values to set to the biomass of each <code>Model</code>.
	 * @return an integer implying the success or failure of the operation
	 */
	public abstract int setBiomass(double[] values);
	
	/**
	 * Changes the biomass of the <code>Cell</code> to a new array of values, where each
	 * element <i>i</i> corresponds to the <i>i</i>th <code>Model</code> currently loaded. 
	 * @param values - an array of values to set to the biomass of each <code>Model</code>.
	 * @return an integer implying the success or failure of the operation
	 */
	public abstract int setBiomass3D(double[] values);
	
	public abstract void setConvectionRHS1(double[] values);
	public abstract void setConvectionRHS2(double[] values);
	
	public abstract void setConvectionMultiRHS1(double[] values);
	public abstract void setConvectionMultiRHS2(double[] values);
	
	public abstract void setJointRHS1(double value);
	public abstract void setJointRHS2(double value);
	
	public abstract void setConvModelFluxes(double[][] value);
	
	
	/**
	 * 
	 * @param oldModels
	 * @param newModels
	 */
	public abstract void changeModelsInCell(Model[] oldModels, Model[] newModels);
	
	//Jean Sets the cell to not be in stationary phase needed if batch dilute is in use.
	public abstract void setStationaryStatus();
	
	public abstract void setParameters(CometsParameters cParams);
	public abstract void setPackageParameters(PackageParameters pParams);
	
}
