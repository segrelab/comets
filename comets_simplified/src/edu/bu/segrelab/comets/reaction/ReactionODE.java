package edu.bu.segrelab.comets.reaction;

import org.apache.commons.math3.exception.DimensionMismatchException;
import org.apache.commons.math3.exception.MaxCountExceededException;
import org.apache.commons.math3.ode.FirstOrderDifferentialEquations;

import edu.bu.segrelab.comets.util.Utility;

/*An experiment in implementing the Apache math package to solve ODEs instead of using my home-brewed Runge-Kutta*/
public class ReactionODE implements FirstOrderDifferentialEquations {

	protected double[][] exRxnStoich; //dimensions are ReactionID by MetID
	protected double[] exRxnRateConstants; //Kcat for enzymatic reactions, or the forward reaction rate for simple reactions
	protected int[] exRxnEnzymes; //index of the corresponding reaction's enzyme in the World Media list. Non-enzymatic reactions have -1 here
	protected double[][] exRxnParams; //same dims as exRxnStoich. Stores either the Michaelis constant or reaction order

	public ReactionODE(double[][] exRxnStoich, double[] exRxnRateConstants, int[] exRxnEnzymes, double[][] exRxnParams) {
		this.exRxnStoich = exRxnStoich;
		this.exRxnRateConstants = exRxnRateConstants;
		this.exRxnEnzymes = exRxnEnzymes;
		this.exRxnParams = exRxnParams;
	}
	
	@Override
	//TODO: assuming y is limited to the metabolites involved in the reactions, not all World media
	//		assuming exRxnEnzymes refers to concentrations of the enzyme in y
	//		CHECK HOW HIGH-ORDER REACTIONS GET SOLVED (eg: S^2->P)
	/**Calculate dy/dt for each metabolite, and store it in the vector yDot
	 * 
	 */
	public void computeDerivatives(double t, double[] y, double[] yDot)
			throws MaxCountExceededException, DimensionMismatchException {
		//make sure yDot is blank. Don't make a new array though, we need to preserve the pointer
		for (int i = 0; i < yDot.length; i++) {yDot[i] = 0.0;}

		int nreactions = exRxnStoich.length;
		int nmets = exRxnStoich[0].length;
		
		for (int i = 0; i<nreactions; i++) {
			//calculate the instantaneous rate of each reaction
			double rate = calcRxnRate(i,y);
			//for each reactant and product in reaction, update yDot with the derivative over time
			double[] stoich = exRxnStoich[i];
			for (int j = 0; j < nmets; j++) {
				double delta = stoich[j] * rate;
				yDot[j] = yDot[j] + delta;
			}		
		}
	}
	
	/**For testing purposes. Calls computeDerivatives then returns the derivative vector yDot */
	public double[] retrieveDerivatives(double t, double[] y) {
		double[] yDot = new double[y.length];
		computeDerivatives(t,y,yDot);
		return yDot;
	}
	
	/**Calculate the instantaneous rate of a single reaction
	 * 
	 * The upper bound of the reaction rate is capped at consuming all of its substrate
	 * 
	 * @param idx Index of the reaction of interest 
	 * @return 
	 */
	public double calcRxnRate(int rxnIdx, double[] concentrations){
		double rate = 0.0;
		
		boolean hasEnz = exRxnEnzymes[rxnIdx] >= 0;
		if (hasEnz){ //enzymatic reaction
			int eIdx = exRxnEnzymes[rxnIdx]; //enzyme index
			double eCon = concentrations[eIdx];
			double kcat = exRxnRateConstants[rxnIdx];

			//these parameters are used if km==0, then the substrate concentration doesn't matter
			//and v = kcat*eCon
			double subCon = 1.0;
			double km = 0.0;
			
			int[] subIdxArr = Utility.findNonzeroValues(exRxnParams[rxnIdx]);
			if (subIdxArr.length > 0){
				int subIdx = subIdxArr[0]; //should only be one
				subCon = concentrations[subIdx];
				km = exRxnParams[rxnIdx][subIdx];
			}
			
			double denom = km + subCon;
			if (denom == 0.0){ rate = kcat * eCon;}
			else{
				rate = (kcat * eCon * subCon) / (km + subCon); //Michaelis-Menten rate
			}

			if (subIdxArr.length == 0){ //for the edge case/hack where km = 0
				//find the substrate by the stoichiometry table instead
				subIdxArr = Utility.findNonzeroValues(exRxnStoich[rxnIdx]);
			}
			if (subIdxArr.length > 0){
				for (int i = 0; i < subIdxArr.length; i++){
					//one of these will be the substrate. The rest are products
					int idx = subIdxArr[i];
					double s = exRxnStoich[rxnIdx][idx];
					if (s < 0){
						subCon = concentrations[idx];
						double v = subCon / -s;
						rate = Math.min(rate, v);
						rate = Math.max(rate, 0); //TODO: Fix the calculation so we can remove this line
					}
				}
			}
			
		}
		else { //Simple reaction. r = k * [A]^a * [B]^b ...
			int[] subIdxs = Utility.findNonzeroValues(exRxnParams[rxnIdx]);
			double k = exRxnRateConstants[rxnIdx];
			rate = k;
			double maxrate = -1;
			for (int i = 0; i < subIdxs.length; i ++){
				double irate = Math.pow(concentrations[subIdxs[i]], exRxnParams[rxnIdx][subIdxs[i]]);
				rate *= irate;
				if (maxrate<0 || irate < maxrate) maxrate = irate;
			}
			if (maxrate < 0) maxrate = rate; //If for some reason there are no substrates, but this isn't where the breakage should be
			rate = Math.min(rate, maxrate);
		}
		return rate;
	}

	@Override
	/*how many moving parts does the problem have? Eg how many unique reactants/products?*/
	public int getDimension() {
		//TODO: encountering a bug with the interpolator when the enzyme is not a reactant/product in 
		//any reactions, and it thinks we're lying about the dimensionality. Should refactor so the 
		//enzyme concentrations in those cases is passed separately?
		//For now, giving it a higher dimension should work but it's less efficient
		return exRxnStoich[0].length;
		
		/*
		//Scan each column in Stoich, mark each nonzero metabolite, then count how many marked.
		//Don't just take the length of stoich because there might be enzymes which never act as reactants/products
		int nreactions = exRxnStoich.length;
		int nmets = exRxnStoich[0].length;
		int[] found = new int[nmets];
		
		for (int i = 0; i < nreactions; i++) {
			for (int j = 0; j < nmets; j++) {
				if (exRxnStoich[i][j] != 0) found[j] = 1;
			}
		}
		int sum = 0;
		for (int j = 0; j < nmets; j++) {
			sum = sum + found[j];
		}
		return sum;*/
	}

}
