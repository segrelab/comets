package edu.bu.segrelab.comets;

import edu.bu.segrelab.comets.util.Point3d;
/**
 * A <code>StaticPoint</code> is a specialized <code>Point</code> used in <code>Comets</code>.
 * Basically, this is a point that contains an array of media levels that should be kept at the
 * same value throughout the simulation. It has a parallel boolean array, where a value of
 * <code>true</code> means that medium component should be static. 
 * @author Bill Riehl briehl@bu.edu
 */

public class StaticPoint extends Point3d
{
	private double[] conc;
	private boolean[] isStatic;
	
	/**
	 * Makes a new <code>StaticPoint</code> at (x, y) with media concentrations conc.
	 * @param x the x-coordinate
	 * @param y the y-coordinate
	 * @param conc the media concentrations to refresh on each refresh cycle
	 * @param isStatic each element that is <code>true</code> corresponds to the 
	 * medium component that should be made static.
	 */
	public StaticPoint(int x, int y, double[] conc, boolean[] isStatic)
	{
		super(x, y);
		setValues(conc, isStatic);
	}
	
	public StaticPoint(int x, int y, int z, double[] conc, boolean[] isStatic)
	{
		super(x, y, z);
		setValues(conc, isStatic);
	}
	
	/**
	 * @return the array of boolean values, where each true element corresponds to a medium component that should be kept static
	 */
	public boolean[] getStaticSet()
	{
		return isStatic;
	}
	
	/**
	 * @return the array of media that is to be kept static (if the corresponding element returned by
	 * getStaticSet() is true)
	 */
	public double[] getMedia()
	{
		return conc;
	}
	
	public void setValues(double[] conc, boolean[] isStatic)
	{
		this.conc = new double[conc.length];
		System.arraycopy(conc, 0, this.conc, 0, conc.length);
		
		this.isStatic = new boolean[isStatic.length];
		System.arraycopy(isStatic, 0, this.isStatic, 0, conc.length);
	}
}
