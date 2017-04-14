package edu.bu.segrelab.comets.reaction;

import java.util.ArrayList;
import java.util.List;

import edu.bu.segrelab.comets.util.Utility;

public abstract class Rates {

	//Get the rate according to Michaelis-Menten kinetics
	public static double calcEnzymaticRate(double substrateConc, double enzConc, double km, double kcat){
		return (kcat * enzConc) * substrateConc / (km + substrateConc);
	}
	
	//Get the rate according to the equation r = k * [A]^m * [B]^n
	public static double calcSecondOrderRate(double conc1, double order1, double conc2, double order2, double k){
		return k * Math.pow(conc1,order1) * Math.pow(conc2, order2);
	}
	
	//return a vector containing dS/dt for all substrates in the given media
	//requires that the order of in the 'concentrations' argument corresponds to the order in the second
	//dimension of the given matrixes
	public static double[] calcRxnRates(double[][] stoich, double[][] params, double[][]kcats, double[] concentrations){
		double[] rates = new double[concentrations.length];
		for (int i = 0; i < concentrations.length; i++){
			rates[i] = calcRxnRate(stoich,params,kcats,concentrations,i);
		}
		return rates;
	}
	
	/**Calculate the rate of change of a single media component
	 * 
	 * Dimesntions for the first three arguments are RxnIdx by MetaboliteIdx
	 * 
	 * @param stoich Stoichiometry of reactions
	 * @param params Either the Michaelis constant or reaction order
	 * @param kcats Kcat of the corresponding media element acting as a non-consumed catalyst/enzyme
	 * @param concentrations Millimolar concentrations of each media component
	 * @param idx Index of the metabolite of interest in the Concentration list and 2nd dimension of the matrixes
	 * @return 
	 */
	public static double calcRxnRate(double[][]stoich, double[][] params, double[][] kcats, double[] concentrations, int idx){
		int nrxns = stoich.length;
		//get the reactions which consume or produce this molecule
		double[] stoichiometry = new double[nrxns];
		ArrayList<Integer> rxnIndexes = new ArrayList<Integer>();
		for (int j = 0; j < nrxns; j++){
			if (stoich[j][idx] != 0.0) rxnIndexes.add(j);
		}
		
		
		//for (int j = 0; j < nrxns; j++){
		//	boolean hasEnz = Utility.hasNonzeroValue(kcats[j]);
		//	if (hasEnz){ //enzymatic reaction
				
		//	}
		//}
		
	}
}
