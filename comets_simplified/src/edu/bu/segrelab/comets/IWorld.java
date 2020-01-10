package edu.bu.segrelab.comets;

import java.util.ArrayList;
import java.util.List;

import edu.bu.segrelab.comets.reaction.ReactionModel;

/**An interface to hold components that are shared between 2D and 3D worlds.
 *
 * Ideally, all worlds should be 3D worlds but the 2D world implementation exists
 * as a legacy artifact. In the future, 2D worlds should be eliminated and the code
 * in this interface should be folded into World3D.
 * 
 * @author mquintin
 */
public interface IWorld {	
	
	static ReactionModel reactionModel = new ReactionModel(); //static. We only need one.
	//List<String> initialMediaNames = new ArrayList<String>();
	
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
	
	abstract boolean is3D();
		
	abstract void runExternalReactions();
	
	abstract Comets getComets();
	
	abstract String[] getMediaNames();
	
	abstract void setInitialMediaNames(String[] arr);
	
	abstract String[] getInitialMediaNames();

	public static ReactionModel getReactionModel() {
		return reactionModel;
	}
	
}
