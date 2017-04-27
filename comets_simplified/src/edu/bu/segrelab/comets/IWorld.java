package edu.bu.segrelab.comets;

import edu.bu.segrelab.comets.reaction.ReactionModel;

/**An interface to hold components that are shared between 2D and 3D worlds
 * 
 * @author mquintin
 */
public interface IWorld {
	
	final ReactionModel reactionModel = new ReactionModel();
	
	abstract double[] getMediaAt(int x, int y, int z);
	
	/**Get the size of the world. Should always return an array with
	 * exactly three members 
	 * @return [X, Y, Z]
	 */
	abstract int[] getDims();
	
	/**
	 * Sets the media values at (x, y, z) to the values in delta. Note that this <b>sets</b> the
	 * values, it doesn't add or subtract them. 
	 * @param x
	 * @param y
	 * @param delta
	 * @return <code>CometsConstants.PARAMS_OK</code> if successful, 
	 * <code>CometsConstants.PARAMS_ERROR</code> if the delta array is the wrong length, 
	 * and <code>CometsConstants.BOUNDS_ERROR</code> if the location is out of bounds.
	 */
	abstract int setMedia(int x, int y, int z, double[] delta);
	
	default boolean is3D(){
		return getDims()[2] > 1;
	}
}
