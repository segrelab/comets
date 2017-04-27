package edu.bu.segrelab.comets.reaction;

import javax.swing.JComponent;

import edu.bu.segrelab.comets.CometsConstants;
import edu.bu.segrelab.comets.IWorld;
import edu.bu.segrelab.comets.Model;
import edu.bu.segrelab.comets.World;
import edu.bu.segrelab.comets.World3D;
/**A class to hold the information necessary to run extracellular reactions
 * 
 * @author mquintin
 *
 */
public class ReactionModel extends Model implements CometsConstants {

	protected int nmets;
	protected int nrxns;
	protected String[] metNames;
	protected double[][] exRxnStoich; //dimensions are ReactionID by MetID
	protected double[] exRxnRateConstants; //Kcat for enzymatic reactions, or the forward reaction rate for simple reactions
	protected int[] exRxnEnzymes; //index of the corresponding reaction's enzyme in the World Media list. Non-enzymatic reactions have -1 here
	protected double[][] exRxnParams; //same dims as exRxnStoich. Stores either the Michaelis constant or reaction order
	//depending on if the reaction is enzymatic

	protected IWorld world;
	//protected boolean worldIs3D = false;
	//protected int x,y,z;
	
	//A ReactionModel is created via this constructor during instantiation of a class
	//implementing the IWorld interface
	public ReactionModel() {
		// TODO Auto-generated constructor stub
	}
	
	public void setWorld(IWorld world){
		this.world = world;
	}

	@Override
	public int run() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String[] getMediaNames() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getModelName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public JComponent getInfoPanel() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public JComponent getParametersPanel() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void applyParameters() {
		// TODO Auto-generated method stub

	}

	@Override
	public Model clone() {
		// TODO Auto-generated method stub
		return null;
	}

}
