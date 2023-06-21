package edu.bu.segrelab.comets.fba;


import com.google.ortools.Loader;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPObjective;
import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPVariable;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Collections;

/**
 * This class provides the GLOP optimizer functionality for the FBAModel class.
 * It extends the abstract FBAOptimizer class.
 * <p>
 * The best way to use this class is as follows:
 * <ol>
 * <li>The user loads a GLOP optimizer by using a constructor directly.
 * <li>Set the lower bound for the exchange fluxes with setExchLowerBounds(int[],double[]).
 * <li>Run the optimizer with run(int).
 * <li>Fetch the results with various accessory methods: getFluxes(), getExchangeFluxes(),
 *     getObjectiveSolution() etc.
 * </ol>
 * <p>
 * 
 * Note that this class depends on the availability of the OR-Tools package, which must be
 * accessible either through the classpath or the library path. Be sure to add the ortools .jar libraries
 * to the classpath and, if on a POSIX-based system (Mac or Linux), add the path to the 
 * installed OR-Tools libraries in -Djava.library.path
 * 
 * @author Ilija Dukovski ilija.dukovski@gmail.com
 * created 28 July 2022
 * 
 * 
 */
public class FBAOptimizerGlop extends edu.bu.segrelab.comets.fba.FBAOptimizer 
implements edu.bu.segrelab.comets.CometsConstants
{

	// Create the linear solver with the GLOP backend.
	private MPSolver solver;//= MPSolver.createSolver("GLOP");
	private MPVariable[] rxnFluxes;
	private MPConstraint[] rxnExpressions;
	private MPObjective objective;//= solver.objective();
	private MPSolver.ResultStatus resultStatus;

	private boolean runSuccess;
	private int numMetabs;
	private int numRxns;
	private int objIndex;

	private double[][] stoichMatrix;
	private int[] objReactions; //for consistency, this should ALWAYS be 1-ordered!
	private boolean[] objMaximize;
	
	/**
	 * Create a new simple FBAOptimizerGurobi without any stoichiometry information loaded.
	 * Creates the Glop environment and model only.
	 */
	private FBAOptimizerGlop()
	{
		runSuccess = false;
		Loader.loadNativeLibraries();
		solver = MPSolver.createSolver("GLOP");
		objective = solver.objective();
	}

	/**
	 * Create a new FBAOptimizerGurobi with stoichiometric matrix m, lower bounds l, upper bounds u,
	 * and objective reaction r. 
	 * @param m Stoichiometric matrix
	 * @param l lower bounds
	 * @param u upper bounds
	 * @param objIdxs list of indexes for objective reactions, from 1 to N, in order of priority with the highest first
	 * @param objMaximize list signifying if the corresponding objective rxn should be maximized, otherwise it will be minimized
	 */
	public FBAOptimizerGlop(double[][] m, double[] l, double[] u, int[] objIdxs, boolean[] objMax)
	{
		this();
		
		stoichMatrix=m;
		objReactions=objIdxs;
		objMaximize=objMax;
		sortByColumn(m, 0);
		
		// everything below here is for initializing the basic model
		Double mtb = m[m.length-1][0];		
		numMetabs = mtb.intValue();  
		numRxns = l.length;
		
		rxnFluxes = new MPVariable[numRxns];
		rxnExpressions = new MPConstraint[numMetabs];
		
		// all our variables (fluxes) are always continuous
		for(int i=0;i<numRxns;i++){
			rxnFluxes[i] = solver.makeNumVar(l[i], u[i], "Flux_"+String.valueOf(i));
		}
		
		for(int j=0; j<numMetabs; j++) {
			rxnExpressions[j] = solver.makeConstraint(0.0, 0.0, "c_"+String.valueOf(j));
			for(int i=0; i<numRxns; i++)rxnExpressions[j].setCoefficient(rxnFluxes[i], 0);
		}
		for(int j=0; j<m.length; j++) {
			Double m_local_zero=m[j][0];
			Double m_local_one=m[j][1];
			Double m_local_two=m[j][2];
			rxnExpressions[m_local_zero.intValue()-1].setCoefficient(rxnFluxes[m_local_one.intValue()-1], m_local_two);
		}
		
		for(int i=0;i<numRxns;i++){
			objective.setCoefficient(rxnFluxes[i], 0);
		}
		objective.setCoefficient(rxnFluxes[objIdxs[0]], 1);
		objIndex=objIdxs[0];
	}

	/**
	 * Performs an FBA run with the loaded model, constraints, and bounds. Fluxes
	 * and solutions are stored in the FBAOptimizerGurobi class and can be used through their
	 * accessory functions.
	 * @return the GLPK status code 5 (this is legacy from GLPK) TODO change this,
	 * or MODEL_NOT_INITIALIZED if there's no problem to solve
	 * @see getFluxes()
	 * @see getObjectiveSolution()
	 */
	public synchronized int run(int objSty)
	{
		runSuccess = false;
		if (numMetabs == 0 || numRxns == 0)
		{
			return MODEL_NOT_INITIALIZED;
		}
		// an internal status checker. If this = 0 after a run, everything is peachy.
		int ret = -1;

		// gurobi optimization status.
		// 2 = optimal, 3 = infeasible, 4 = infeasible or unbounded
		// to parallel the GLPK implementation, if gurobi returns 2, then
		// run() returns 5 (which is GLPK's optimal status)
		// otherwise it will return 4 (GLPK's infeasible status)
		int status = -1; 
		switch(objSty)
		{
		// the usual, just max the objective
		case FBAModel.MAXIMIZE_OBJECTIVE_FLUX:
			objective.setMaximization();
			resultStatus = solver.solve();
			break;
		default:

			break;
		}

		if (ret == 0)
		{   
			runSuccess = true;
		}

		// convert gurobi status indicators into GLPK statuses
		if (status == 2){
			status = 5;
		}else if(status == 3){
			status = 4;
		}

		return status;
	}

	/**
	 * @return The fluxes from the most recent FBA run
	 */
	public double[] getFluxes()
	{
		double[] v = new double[numRxns];
		if (resultStatus == MPSolver.ResultStatus.OPTIMAL) {
				for(int i=0;i<numRxns;i++) {
					v[i]=rxnFluxes[i].solutionValue();
				}
			} else {
			  System.err.println("The problem does not have an optimal solution!");
			}
		return v;
	}


	/**
	 * If the FBA run was successful, this returns
	 * the value of the objective solution. Otherwise, it returns -Double.MAX_VALUE
	 * @return either the objective solution or -Double.MAX_VALUE
	 * @param objreact Objective reaction index.
	 */
	public double getObjectiveSolution(int objreact)
	{		
			if (resultStatus == MPSolver.ResultStatus.OPTIMAL)
				return rxnFluxes[objreact-1].solutionValue();
			else System.err.println("The problem does not have an optimal solution!");
		return -Double.MAX_VALUE;
	}
	
	public double[] getObjectiveSolutions(int[] objs)
	{		
		double[] res = new double[objs.length];
		for (int i = 0; i < objs.length; i ++) {
			res[i] = getObjectiveSolution(objs[i]);
		}
		return res;
	}
	
	public double getObjectiveFluxSolution(int objreact)
	{		
			if (resultStatus == MPSolver.ResultStatus.OPTIMAL)
				return rxnFluxes[objreact-1].solutionValue();
			else System.err.println("The problem does not have an optimal solution!");
		return -Double.MAX_VALUE;
	}
	
	/**
	 * Returns the status of FBA (feasible or infeasible).
	 * @return
	 */
	public int getFBAstatus()
	{
		if (resultStatus == MPSolver.ResultStatus.OPTIMAL)
			return 1; // feasible
		else
			return 0; // not feasible
	}
	
	/**
	 * Produces a clone of this <code>FBAOptimizerGlop</code> with all parameters intact.
	 */

	public FBAOptimizerGlop clone()
	{
		
		//FBAOptimizerGlop optimizerCopy=new FBAOptimizerGlop(m,l,u,objs,max);
		MPVariable[] rxnFluxesLocal= this.rxnFluxes;
		double[] l=new double[rxnFluxesLocal.length];
		double[] u=new double[rxnFluxesLocal.length];
		//double[][] m=new double[][];
		//int r = objReaction;
		//int obj = objIndex;
		//boolean[] max = objMaximize;
		
		for(int k=0;k<rxnFluxesLocal.length;k++)
		{
				l[k]=rxnFluxesLocal[k].lb();
				u[k]=rxnFluxesLocal[k].ub();
		}
		
		FBAOptimizerGlop optimizerCopy=new FBAOptimizerGlop(stoichMatrix,l,u,objReactions,objMaximize);

		return optimizerCopy;
	}
	/**
	 * Sets the current upper bounds for the FBA problem
	 * @param nrxns
	 * @param ub upper bounds array
	 * @return PARAMS_OK if the ub array is of the appropriate length,
	 * MODEL_NOT_INITIALIZED if nrxns is zero.
	 */

	public int setUpperBounds(int nrxns, double[] ub)
	{
		return PARAMS_OK;
	}
	public int setLowerBounds(int nrxns, double[] lb)
	{
		return PARAMS_OK;
	}
	
	public double[] getUpperBounds(int nrxns)
	{	
		MPVariable[] rxnFluxesLocal= this.rxnFluxes;
		double[] u=new double[rxnFluxesLocal.length];
		
		for(int k=0;k<rxnFluxesLocal.length;k++)
		{
				u[k]=rxnFluxesLocal[k].ub();
		}
		return u;
	}
	public double[] getLowerBounds(int nrxns)
	{	
		MPVariable[] rxnFluxesLocal= this.rxnFluxes;
		double[] l=new double[rxnFluxesLocal.length];
		
		for(int k=0;k<rxnFluxesLocal.length;k++)
		{
				l[k]=rxnFluxesLocal[k].lb();
		}
		return l;
	}
	public int setObjectiveMaximize(boolean[] objMax) {
		this.objMaximize = objMax;
		return PARAMS_OK;
	}
	public int setObjectiveReaction(int a, int b) {
		//this.objMaximize = objMax;
		return PARAMS_OK;
	}
	public int setObjectiveReaction(int a, int[] b) {
		//this.objMaximize = objMax;
		return PARAMS_OK;
	}
	public int setObjectiveUpperBound(int a, double b) {
		//this.objMaximize = objMax;
		return PARAMS_OK;
	}
	public int setObjectiveLowerBound(int a, double b) {
		//this.objMaximize = objMax;
		return PARAMS_OK;
	}
	public int setExchLowerBounds(int[] exch, double[] lb)
	{
		return PARAMS_OK;
	}
	public int setExchUpperBounds(int[] exch, double[] ub)
	{	
		return PARAMS_OK;
	}
	public double[] getExchangeFluxes(int[] exch)
	{	
		return PARAMS_OK;
	}
	
	/* 
	 * method to sort a double 2D array according to one column (used for m) (djordje)
	 */
	private void sortByColumn (double arr[][], int col) 
	{
		Arrays.sort(arr, new Comparator <double[]>() {
			
			@Override
			public int compare(final double[] a, final double[] b) {
				return Double.compare(a[0], b[0]);
			}
		});
	}
	
}