package edu.bu.segrelab.comets.reaction;

import java.util.ArrayList;
import java.util.List;

import edu.bu.segrelab.comets.fba.FBACell;
import edu.bu.segrelab.comets.fba.FBAWorld;
import edu.bu.segrelab.comets.fba.FBAWorld3D;
import edu.bu.segrelab.comets.util.Utility;

public class ExternalReactionCalculator{
	double[] concentrations; //in *millimolar* concentration
	int[] exRxnEnzymes; //index of the enzyme that catalyzes the i-th reaction, or -1 if rxn i doesn't have an enzyme
	double[] exRxnRateConstants; //units are per second
	double[][] stoich; //stoichiometry of reactants and products
	double[][] params; //either the Michaelis constant or the reaction order
	double timestep; //in *SECONDS*, whereas Comets stores the timestep in hours
	
	public ExternalReactionCalculator(double[] concentrations, int[] exRxnEnzymes, double[] exRxnRateConstants, double[][]stoich, double[][] params, double timestep_seconds){
		this.concentrations = concentrations;
		this.exRxnEnzymes = exRxnEnzymes;
		this.exRxnRateConstants = exRxnRateConstants;
		this.stoich = stoich;
		this.params = params;
		this.timestep = timestep_seconds;
	}

	//Factory method to make it simpler to create one of these from an FBA cell
	public static ExternalReactionCalculator createCalculator(FBACell cell){
		double[] concentrations = cell.getMedia(); //assumes that the media array in the World is *concentration*, not amount
		double timestep_seconds = (double) cell.getComets().getParameters().getTimeStep() / (60 * 60);
		int[] exRxnEnzymes = null;
		double[] exRxnRateConstants = null;
		double[][] stoich = null;
		double[][] params = null;
		
		if(cell.getComets().getParameters().getNumLayers() == 1){ //2d World
			FBAWorld world = (FBAWorld) cell.getComets().getWorld();
			exRxnEnzymes = world.getExRxnEnzymes();
			exRxnRateConstants = world.getExRxnRateConstants();
			stoich = world.getExRxnStoich();
			params = world.getExRxnParams();
		}
		else if (cell.getComets().getParameters().getNumLayers() > 1){ //3D world
			FBAWorld3D world = (FBAWorld3D) cell.getComets().getWorld3D();
			exRxnEnzymes = world.getExRxnEnzymes();
			exRxnRateConstants = world.getExRxnRateConstants();
			stoich = world.getExRxnStoich();
			params = world.getExRxnParams();
		}
		return new ExternalReactionCalculator(concentrations, exRxnEnzymes, exRxnRateConstants, stoich, params, timestep_seconds);
	}
	
	//Get the rate according to Michaelis-Menten kinetics
	public static double calcEnzymaticRate(double substrateConc, double enzConc, double km, double kcat){
		return (kcat * enzConc) * substrateConc / (km + substrateConc);
	}
	
	//Get the rate according to the equation r = k * [A]^m * [B]^n
	/*public static double calcSecondOrderRate(double conc1, double order1, double conc2, double order2, double k){
		return k * Math.pow(conc1,order1) * Math.pow(conc2, order2);
	}*/
	
	//return a vector containing dS/dt for all substrates in the given media
	//requires that the order of in the 'concentrations' argument corresponds to the order in the second
	//dimension of the given matrixes
	public double[] calcRxnRates(double[] concs){
		double[] rates = new double[concs.length];
		for (int i = 0; i < concs.length; i++){
			//TODO: This method has been changed to no longer return the value for a certain metabolite...   rates[i] = calcRxnRate(stoich,params,kcats,concentrations,i);
			rates[i] = calcRxnRate(i, concs);
		}
		return rates;
	}
	public double[] calcRxnRates(){
		return calcRxnRates(concentrations);
	}
		
	/**Calculate the rate of a single reaction
	 * 
	 * Dimensions for the first three arguments are RxnIdx by MetaboliteIdx
	 * 
	 * @param idx Index of the reaction of interest in the first dimension of the matrixes
	 * @return 
	 */
	public double calcRxnRate(int rxnIdx, double[] concentrations){
		double rate = 0.0;
		
		boolean hasEnz = exRxnEnzymes[rxnIdx] >= 0;
		if (hasEnz){ //enzymatic reaction
			int eIdx = exRxnEnzymes[rxnIdx]; //enzyme index
			double eCon = concentrations[eIdx];
			double km = params[rxnIdx][eIdx];
			double kcat = exRxnRateConstants[rxnIdx];
			
			int subIdx = ((int[]) Utility.findNonzeroValues(params[rxnIdx]))[0]; //substrate index
			double subCon = concentrations[subIdx];
			
			rate = (kcat * eCon * subCon) / (km + subCon); //Michaelis-Menten rate
		}
		else { //Simple reaction. r = k * [A]^a * [B]^b ...
			int[] subIdxs = Utility.findNonzeroValues(params[rxnIdx]);
			double k = exRxnRateConstants[rxnIdx];
			rate = k;
			for (int i = 0; i < subIdxs.length; i ++){
				rate *= Math.pow(concentrations[subIdxs[i]], params[rxnIdx][i]);
			}
		}
		return rate;
	}
	
	//Implement Runge-Kutta algorithm to solve ODEs
	
	/**Implement the Runge-Kutta algorithm to solve the ODE system and return the concentrations 
	 * after the given number of timesteps
	 * 
	 * @return
	 */
	public double[] rk4(){
		int nmets = concentrations.length;
		int nrxns = stoich.length;
		//delta_y1 = delta_t * f(t0, y0)
		double[] dy1 = new double[nmets];
		double[] rates = calcRxnRates(concentrations); //units are mmol/s
		for (int i = 0; i < nmets; i++){
			double delta = 0.0;
			for (int j = 0; j < nrxns; j++){
				double s = stoich[j][i];
				double k = rates[j];
				delta += s * k;
			}
			delta *= timestep;
			dy1[i] = delta; //units are mmol
		}
		
		//delta_y2 = delta_t * f(t0 + 1/2 delta_t, y0 + 1/2 delta_y1)
		double[] y2 = new double[nmets];
		for (int i = 0; i < nmets; i++) y2[i] = concentrations[i] + (dy1[i] / 2);
		double[] dy2 = new double[nmets];
		rates = calcRxnRates(y2);
		for (int i = 0; i < nmets; i++){
			double delta = 0.0;
			for (int j = 0; j < nrxns; j++){
				double s = stoich[j][i];
				double k = rates[j];
				delta += s * k;
			}
			delta *= timestep;
			dy2[i] = delta;
		}
		
		//delta_y3 = delta_t * f(t0 + 1/2 delta_t, y0 + 1/2 delta_y2)
		double[] y3 = new double[nmets];
		for (int i = 0; i < nmets; i++) y3[i] = concentrations[i] + (dy2[i] / 2);
		double[] dy3 = new double[nmets];
		rates = calcRxnRates(y3);
		for (int i = 0; i < nmets; i++){
			double delta = 0.0;
			for (int j = 0; j < nrxns; j++){
				double s = stoich[j][i];
				double k = rates[j];
				delta += s * k;
			}
			delta *= timestep;
			dy3[i] = delta;
		}
		
		//delta_y4 = delta_t * f(t0 + delta_t, y0 + delta_y3)
		double[] y4 = new double[nmets];
		for (int i = 0; i < nmets; i++) y4[i] = concentrations[i] + dy3[i];
		double[] dy4 = new double[nmets];
		rates = calcRxnRates(y4);
		for (int i = 0; i < nmets; i++){
			double delta = 0.0;
			for (int j = 0; j < nrxns; j++){
				double s = stoich[j][i];
				double k = rates[j];
				delta += s * k;
			}
			delta *= timestep;
			dy4[i] = delta;
		}
		
		//y_final = y0 + some linear combination of the deltas
		double[] yf = new double[nmets];
		for (int i = 0; i < nmets; i++)
			//yf = y0 + (1/6)(delta_y1 + 2*delta_y2 + 2*delta_y3 + delta_y4)
			yf[i] = concentrations[i] + ((dy1[i] + (2*dy2[i]) + (2*dy3[i]) + dy4[i])/6);
		
		return yf;
	}
	
	/**Return the concentrations of each media component after running for
	 * the given amount of time at the initial reaction velocity
	 * 
	 * @param concentrations
	 * @param exRxnEnzymes
	 * @param exRxnRateConstants
	 * @param stoich
	 * @param params
	 * @param timestep
	 * @return
	 */
	/*public static double[] euler(double[] concentrations, int[] exRxnEnzymes, double[] exRxnRateConstants, double[][]stoich, double[][] params, double timestep){
		int nrxns = stoich.length;
		double[] rates = calcRxnRates(concentrations, exRxnEnxymes, stoich, params, )
		double[] res = new double[nrxns];
		for (int i = 0; i < nrxns; i++){
			res[i] = 
		}
		
		
		return res;
	}*/

}
