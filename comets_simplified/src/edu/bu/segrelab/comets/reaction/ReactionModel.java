package edu.bu.segrelab.comets.reaction;

import java.util.Arrays;
import java.util.List;

import javax.swing.JComponent;

import edu.bu.segrelab.comets.CometsConstants;
import edu.bu.segrelab.comets.IWorld;
import edu.bu.segrelab.comets.Model;
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
	protected boolean isSetUp;

	//These store the initial values. Since the model addition/removal process may be called multiple times,
	//we would otherwise get in trouble if setup() is invoked more than once 
	protected double[][] initialExRxnStoich; 
	protected double[] initialExRxnRateConstants; 
	protected int[] initialExRxnEnzymes; 
	protected double[][] initialExRxnParams; 
	protected String[] initialMetNames;

	
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
		if (!isSetUp) return 0; //don't do anything if there aren't reactions to run
		
		int[] worldIdxs = getMediaIdxs(); //locations of the media in the world's lists
		//loop over all cells
		int[] dims = world.getDims();
		for (int x = 0; x < dims[0]; x++){
			for (int y = 0; y < dims[1]; y++){
				for (int z = 0; z < dims[2]; z++){
					//pull the concentrations of the media involved in the reactions
					double[] worldMedia = world.getMediaAt(x, y, z);
					double[] rxnMedia = new double[worldIdxs.length];
					for (int i = 0; i < worldIdxs.length; i++){
						rxnMedia[i] = worldMedia[worldIdxs[i]];
					}
					
					//do the math
					double timestep_seconds = world.getComets().getParameters().getTimeStep() * 60 * 60;
					ExternalReactionCalculator calc = 
							new ExternalReactionCalculator(rxnMedia,exRxnEnzymes,exRxnRateConstants,exRxnStoich,exRxnParams,timestep_seconds);
					double[] result = calc.rk4();
					
					if (DEBUG){
						String resStr = "";
						for (double d : result) resStr = resStr + " " + String.valueOf(d);
						System.out.println("Extracellular reaction results: " + resStr);
					}
					
					//apply the changed media to the appropriate position in the full media list
					for (int i = 0; i < worldIdxs.length; i++){
						worldMedia[worldIdxs[i]] = result[i];
					}
					
					world.setMedia(x, y, z, worldMedia); //update the World.media
				}
			}
		}
		return 1;
	}
	
	/**The World may have sorted its media field. This function returns indexes to map
	 * the new media order to the order media are listed in the ReactionModel's fields
	 * 
	 * @return
	 */
	protected int[] getMediaIdxs(){
		int[] worldIdxs = new int[metNames.length];
		List<String> worldNames = Arrays.asList(world.getMediaNames());
		for (int i = 0; i < metNames.length; i++){
			String name = metNames[i];
			int newIdx = worldNames.indexOf(name);
			worldIdxs[i] = newIdx;
		}
		return worldIdxs;
	}

	@Override
	public String[] getMediaNames() {
		return metNames;
	}

	@Override
	public String getModelName() {
		return "ExtracellularReactionModel";
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

	public int getNrxns() {
		return nrxns;
	}

	public void setMediaNames(String[] metNames) {
		this.metNames = metNames;
	}

	public double[][] getExRxnStoich() {
		return exRxnStoich;
	}

	public void setExRxnStoich(double[][] exRxnStoich) {
		this.exRxnStoich = exRxnStoich;
		isSetUp = false;
	}

	public double[] getExRxnRateConstants() {
		return exRxnRateConstants;
	}

	public void setExRxnRateConstants(double[] exRxnRateConstants) {
		this.exRxnRateConstants = exRxnRateConstants;
	}

	public int[] getExRxnEnzymes() {
		return exRxnEnzymes;
	}

	public void setExRxnEnzymes(int[] exRxnEnzymes) {
		this.exRxnEnzymes = exRxnEnzymes;
	}

	public double[][] getExRxnParams() {
		return exRxnParams;
	}

	public void setExRxnParams(double[][] exRxnParams) {
		this.exRxnParams = exRxnParams;
	}

	public IWorld getWorld() {
		return world;
	}

	/**Metabolite indexes in the input file referred to positions in
	 * the complete list of media. This method collapses the arrays
	 * by removing any media which have no role in any reaction.
	 * 
	 * It also populates the metNames array and sets nrxns and nmets.
	 * 
	 */
	public void setup() {
		if (initialExRxnParams == null || initialExRxnRateConstants == null || initialExRxnStoich == null){
			if ((exRxnStoich == null ||	exRxnRateConstants == null || exRxnEnzymes == null)){
				//There isn't enough information to go on. Do nothing
				return;
			}
				else saveState(); //the current arrays will be the new initial state
		}
		reset(); //Because setup should only be run when arrays are in their initial state
		
		String[] allNames = initialMetNames;
		int totalMedia = allNames.length;
		
		if (totalMedia < 1) return; //there's no media, so nothing to do here.
		
		boolean[] used = new boolean[totalMedia];
		nrxns = exRxnRateConstants.length;
		for (int rxn = 0; rxn < nrxns; rxn++){
			if (exRxnEnzymes[rxn] >= 0) used[exRxnEnzymes[rxn]] = true;
			for (int met = 0; met < totalMedia; met++){
				if ((exRxnStoich[rxn][met] != 0) ||
						(exRxnParams[rxn][met] != 0)) used[met] = true;
			}
		}
		
		//count the number of mets used
		int count = 0;
		for (boolean b : used){
			if (b) count++;
		}
		nmets = count;
		
		//build the mapping to new indexes
		int[] idxmap = new int[totalMedia]; //index is the old Idx, value is the new one
		String[] newNames = new String[count]; //get the names while we're here
		int newIdx = 0;
		for (int i = 0; i < totalMedia; i++){
			boolean b = used[i];
			if (b){
				idxmap[i] = newIdx;
				newNames[newIdx] = allNames[i];
				newIdx++;
			}
			else idxmap[i] = -1; //the metabolite is never used
		}
		
		//collapse the arrays
		double[][] newStoich = new double[nrxns][nmets];
		double[][] newParams = new double[nrxns][nmets];
		int[] newEnzymes = new int[nrxns];
		
		for (int rxn = 0; rxn < nrxns; rxn++){
			if (exRxnEnzymes[rxn] == -1){newEnzymes[rxn] = -1;}
			else {newEnzymes[rxn] = idxmap[exRxnEnzymes[rxn]];} //exRxnEnzymes values are indexes
			
			for (int met = 0; met < totalMedia; met++){
				if (exRxnStoich[rxn][met] != 0){
					newStoich[rxn][idxmap[met]] = exRxnStoich[rxn][met];
				}
				if (exRxnParams[rxn][met] != 0){
					newParams[rxn][idxmap[met]] = exRxnParams[rxn][met];
				}
			}
		}
		
		//set the new values
		exRxnEnzymes = newEnzymes;
		exRxnStoich = newStoich;
		exRxnParams = newParams;
		metNames = newNames;
		
		isSetUp = true;
	}

	@Override
	public Model clone() {
		// TODO Auto-generated method stub
		return null;
	}

	//lock the current arrays in as the "initial" values.
	public void saveState(){
		initialExRxnEnzymes = exRxnEnzymes;
		initialExRxnParams = exRxnParams;
		initialExRxnRateConstants = exRxnRateConstants;
		initialExRxnStoich = exRxnStoich;
		initialMetNames = metNames;
	}
	
	/**Restore the saved "initial" values. A process which reorders the media
	*(such as world.changeModelsInWorld() should call this class's setup() function
	*which organizes arrays based on the input file. So this is how we load the state
	*after the initial loading process.
	***/
	public void reset(){
		isSetUp = false;

		metNames = initialMetNames;
		nmets = 0;
		if (metNames != null) nmets = metNames.length;
		nrxns = 0;
		int initialmets = 0;
		if (initialExRxnStoich != null)	{
			nrxns = initialExRxnStoich.length;
			if (initialExRxnStoich.length >= 1)	initialmets = initialExRxnStoich[0].length;
		}

		exRxnEnzymes = new int[nrxns];
		exRxnParams = new double[nrxns][nmets];
		exRxnRateConstants = new double[nrxns];
		exRxnStoich = new double[nrxns][nmets];
		
		//don't just assign the old arrays to the new ones. Doing it this way
		//pads the ends of the arrays to all have the proper length of metabolites
		if (nmets > 0 && nrxns > 0){
			for (int r = 0; r < nrxns; r++){
				exRxnEnzymes[r] = initialExRxnEnzymes[r];
				exRxnRateConstants[r] = initialExRxnRateConstants[r];
				for (int m = 0; m < initialmets; m++){
					exRxnParams[r][m] = initialExRxnParams[r][m];
					exRxnStoich[r][m] = initialExRxnStoich[r][m];
				}
			}
		}		
	}
	
	public boolean isSetUp(){
		return isSetUp;
	}
	
	public void setInitialMetNames(String[] arr){
		initialMetNames = arr;
		metNames = arr;
	}

	public String[] getInitialMetNames() {return initialMetNames;}
	
}
