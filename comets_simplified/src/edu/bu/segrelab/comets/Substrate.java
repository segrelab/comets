package edu.bu.segrelab.comets;
/**
 * The basic, abstract <code>Substrate</code> class for use with the COMETS program. As this is
 * an abstract class, actual <code>Substrate</code> for use in COMETS shouldn't instantiate
 * this class directly, but only an extension class that is part of a loaded package 
 * (e.g. <code>FBASubstrate</code>).
 * @see edu.bu.segrelab.comets.fba.FBASubstrate
 * @author Michael Hasson mhasson@bu.edu
 */
public abstract class Substrate implements CometsConstants
{

	protected int numMedia;
	protected int numModels;

	public abstract double getBiomassDiff(int i);
	
	public abstract double getMediaDiff(int i);
	

	/**
	 * @return the current y-coordinate of the <code>Cell</code> on the grid.
	 */
	public int getnumMedia() { return numMedia; }
	
	/**
	 * @return the current z-coordinate of the <code>Cell</code> on the grid.
	 */
	public int getnumModels() { return numModels; }


	/**
	 * Changes the biomass of the <code>Cell</code> to a new array of values, where each
	 * element <i>i</i> corresponds to the <i>i</i>th <code>Model</code> currently loaded. 
	 * @param values - an array of values to set to the biomass of each <code>Model</code>.
	 * @return an integer implying the success or failure of the operation
	 */
	public abstract void setBiomassDiff(double[] values);
	
	public abstract void setMediaDiff(double[] values);
	
}
