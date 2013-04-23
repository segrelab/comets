package edu.bu.segrelab.comets;

import edu.bu.segrelab.comets.util.Point3d;

/**
 * A <code>RefreshPoint</code> is a specialized <code>Point</code> used in <code>Comets</code>.
 * Basically, this is a point that also contains an array of media concentrations that
 * should be refreshed at that point by the <code>World2D</code>. 
 * @author Bill Riehl briehl@bu.edu
 */
public class RefreshPoint extends Point3d
{
	private double[] conc;
	/**
	 * Makes a new <code>RefreshPoint</code> at (x, y) with media concentrations conc.
	 * @param x the x-coordinate
	 * @param y the y-coordinate
	 * @param conc the media concentrations to refresh on each refresh cycle
	 */
	public RefreshPoint(int x, int y, double[] conc)
	{
		super(x, y);
		setValues(conc);
	}

	public RefreshPoint(int x, int y, int z, double[] conc)
	{
		super(x, y, z);
		setValues(conc);
	}
	
	/**
	 * Returns the array of media concentrations to be refreshed into the <code>World2D</code>
	 * @return a <code>double</code> array of media concentrations to be refreshed
	 */
	public double[] getMediaRefresh() { return conc; }
	
	public void setValues(double[] conc)
	{
		this.conc = new double[conc.length];
		System.arraycopy(conc, 0, this.conc, 0, conc.length);
	}
}