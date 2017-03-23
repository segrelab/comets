package edu.bu.segrelab.comets.fba;

/**
 * This is the abstract class of the FBAOptimizer. Each specific optimizer class must
 * extend this class and implement its abstract methods. The methods are called in 
 * the FBAModel class. The FBAModel class contains a variable of this class. 
 * 
 * @author Ilija Dukovski ilija.dukovski@gmail.com
 * created 11 Mar 2014
 */
public abstract class FBAOptimizer  implements edu.bu.segrelab.comets.CometsConstants
{
	public abstract int run(int objSty);
	
	public abstract int setExchLowerBounds(int[] exch, double[] lb);
	
	public abstract int setExchUpperBounds(int[] exch, double[] ub);
	
	public abstract int setLowerBounds(int numRxns, double[] lb);
	
	public abstract double[] getLowerBounds(int numRxns);
	
	public abstract int setUpperBounds(int numRxns, double[] ub);
	
	public abstract double[] getUpperBounds(int numRxns);
	
	public abstract int setObjectiveReaction(int numRxns, int r);
	
	public abstract double getObjectiveSolution(int objReaction);
	
	public abstract double getObjectiveFluxSolution(int objReaction);
	
	public abstract int setObjectiveUpperBound(int objReaction, double ub);
	
	public abstract int setObjectiveLowerBound(int objReaction, double lb);
	
	public abstract double[] getFluxes();
	
	public abstract double[] getExchangeFluxes(int[] exch);
	
	public abstract FBAOptimizer clone();
}