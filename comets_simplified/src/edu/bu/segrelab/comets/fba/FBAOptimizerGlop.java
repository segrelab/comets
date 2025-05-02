package edu.bu.segrelab.comets.fba;


import com.google.ortools.Loader;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPObjective;
import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPVariable;

import java.util.Arrays;
import java.util.Comparator;
//import java.util.Collections;

/**
 * This class provides the GLOP optimizer functionality for the FBAModel class.
 * It extends the abstract FBAOptimizer class.
 * <p>
 * The best way to use this class is as follows:
 * <ol>
 * <li>The user loads a GLOP optimizer by using a constructor directly.
 * <li>Set the lower bound for the exchange fluxes with setExchLowerBounds(int[],double[]). In Comets we assume that uptake is negative exchange flux. 
 * <li>Run the optimizer with run(int). The input integer set the optimization method: Max. the objective (FBA), Max. obj. min. sum of absolute values of fluxes (pFBA). 
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
	private MPSolver solver;  //= MPSolver.createSolver("GLOP");
	private MPVariable[] rxnFluxes;  
	private MPConstraint[] rxnExpressions;
	private MPObjective objective;//= solver.objective();
	private MPSolver.ResultStatus resultStatus;

	//Create the solver for parsimonious FBA,i.e. for minimizing the sum of the absolute values of the fluxes.
	private MPSolver solverMinSumAbs;  //= MPSolver.createSolver("GLOP");
	private MPVariable[] rxnFluxesMinSumAbs;  
	private MPVariable[] rxnAbsFluxesMinSumAbs;  
	private MPConstraint[] rxnExpressionsMinSumAbs;
	private MPConstraint biomassConstraintExpressionsMinSumAbs;
	private MPObjective objectiveMinSumAbs;//= solver.objective();
	private MPSolver.ResultStatus resultStatusMinSumAbs;
	
	private boolean runSuccess;
	private int numMetabs;
	private int numRxns;
	private int objIndex;

	private double[][] stoichMatrix;
	private int[] objReactions; //for consistency, this should ALWAYS be 1-ordered!
	private boolean[] objMaximize;
	
	private double[] fluxes;
	
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
		
		//These are the pasimonious FBA solver and objective.
		//Loader.loadNativeLibraries();
		solverMinSumAbs=MPSolver.createSolver("GLOP");
		objectiveMinSumAbs = solverMinSumAbs.objective();
		
	}

	/**
	 * Create a new FBAOptimizerGlop with stoichiometric matrix m, lower bounds l, upper bounds u,
	 * and objective reaction r. 
	 * @param m Stoichiometric matrix
	 * @param l lower bounds of the reaction fluxes
	 * @param u upper bounds of the reaction fluxes
	 * @param objIdxs list of indexes for objective reactions, from 1 to N, in order of priority with the highest first
	 * @param objMaximize list signifying if the corresponding objective rxn should be maximized, otherwise it will be minimized
	 */
	public FBAOptimizerGlop(double[][] m, double[] l, double[] u, int[] objIdxs, boolean[] objMax)
	{
		this();
		
		stoichMatrix=m; 		// The stoichiometric matrix is in a sparse form m[LineNum][n]. 
								// LinNum is the line number in the model input file. m[LineNum][0] is an index of a metabolite: 1 to NumMetabs, m[][1] of a reaction: 1 to NumRxns, and m[][2] is the stoichiometric 
								// coefficient of the reaction for each linear equation for each metabolite. Ilija D. 09/19/2023  
		objReactions=objIdxs;	// The indices of reactions that go into the lin. combo of the objective.
		 
		sortByColumn(m, 0);     // Here we sort the matrix by ascending metabolite index. 
		
		// everything below here is for initializing the basic model
		Double mtb = m[m.length-1][0];		//The maximum value of the metabolites index, simply number of metabolites.
											//Keep in mind that the indices of the metabolites are the values in the [][0] elements in the sparse m matrix.
		numMetabs = mtb.intValue();         //m is a Double matrix, so needs to be cast to int.
		numRxns = l.length;                 //Number of reactions. It is determined outside of the class. 
		
		fluxes=new double[numRxns];
		
		rxnFluxes = new MPVariable[numRxns];			//glop variables
		rxnExpressions = new MPConstraint[numMetabs];   //glop constraints 
		
		// all our variables (fluxes) are always continuous. Here we set the lower and upper bounds, and name them.
		for(int i=0;i<numRxns;i++){
			rxnFluxes[i] = solver.makeNumVar(l[i], u[i], "Flux_"+String.valueOf(i));
		}
		
		//Here we create the constrains and name them. The first two arguments are lower and upper bound. Here we have 0<rxnEx<0 so rxnEx==0.
		for(int j=0; j<numMetabs; j++) {
			rxnExpressions[j] = solver.makeConstraint(0.0, 0.0, "c_"+String.valueOf(j));   //V1+V2+...=0
			for(int i=0; i<numRxns; i++)rxnExpressions[j].setCoefficient(rxnFluxes[i], 0); //First reset all coeffs. to zero. 
		}
		
		for(int j=0; j<m.length; j++) {
			Double m_local_zero=m[j][0];  //This is the metabolite index, i.e. glop constraint index, but from 1 to N. 
			Double m_local_one=m[j][1];   //This is the reaction index, i.e. glop variable index, also 1-M.
			Double m_local_two=m[j][2];   //This is the stoichiometric coefficient. 
			rxnExpressions[m_local_zero.intValue()-1].setCoefficient(rxnFluxes[m_local_one.intValue()-1], m_local_two); //Here we set the soichiometric coeffs. Also map from 1-N to 0-N-1 for the indices. 
		}
		
		//The objective is a linear combination of the reaction fluxes. Here we reset all coeffs. to zero. 
		for(int i=0;i<numRxns;i++){
			objective.setCoefficient(rxnFluxes[i], 0);
		}
		
		//Here we set only the first coefficient in the list of reactions that go into the objective. 
		//TODO NEED TO WORK TO GENERALIZE THIS Ilija 10.17.2023
		objective.setCoefficient(rxnFluxes[objIdxs[0]-1], 1);
		objIndex=objIdxs[0]; //Not sure why I needed this? Ilija
		
		/*
		/***************************************pFBA**************************************************************
		 ********************************************************************************************************* 
		*/	
		
		// Step 2: pFBA (minimize total flux subject to fixed biomass)
		rxnFluxesMinSumAbs = new MPVariable[numRxns];
		rxnAbsFluxesMinSumAbs = new MPVariable[numRxns];
		rxnExpressionsMinSumAbs = new MPConstraint[numMetabs+2*numRxns];
		
		//We create two sets of fluxes, and their absolute values
		for (int i = 0; i < numRxns; i++) {
			rxnFluxesMinSumAbs[i] = solverMinSumAbs.makeNumVar(l[i], u[i], "v" + i);
			rxnAbsFluxesMinSumAbs[i] = solverMinSumAbs.makeNumVar(0, Double.POSITIVE_INFINITY, "z" + i);
		}

		// z[i] ≥ v[i] and z[i] ≥ -v[i]
		//These are the constraints that define absolute values
		for (int i = 0; i < numRxns; i++) {
		     rxnExpressionsMinSumAbs[i+numMetabs] = solverMinSumAbs.makeConstraint(-Double.POSITIVE_INFINITY, 0);
		     rxnExpressionsMinSumAbs[i+numMetabs].setCoefficient(rxnAbsFluxesMinSumAbs[i], -1);
		     rxnExpressionsMinSumAbs[i+numMetabs].setCoefficient(rxnFluxesMinSumAbs[i], 1);
		     
		     rxnExpressionsMinSumAbs[i+numRxns+numMetabs] = solverMinSumAbs.makeConstraint(-Double.POSITIVE_INFINITY, 0);
		     rxnExpressionsMinSumAbs[i+numRxns+numMetabs].setCoefficient(rxnAbsFluxesMinSumAbs[i], -1);
		     rxnExpressionsMinSumAbs[i+numRxns+numMetabs].setCoefficient(rxnFluxesMinSumAbs[i], -1);
		}

		// Steady-state constraints
		
		//Here we create the constrains and name them. The first two arguments are lower and upper bound. Here we have 0<rxnEx<0 so rxnEx==0.
		for(int j=0; j<numMetabs; j++) {
			rxnExpressionsMinSumAbs[j] = solverMinSumAbs.makeConstraint(0.0, 0.0, "cAbs_"+String.valueOf(j));   //V1+V2+...=0
			for(int i=0; i<numRxns; i++)rxnExpressionsMinSumAbs[j].setCoefficient(rxnFluxesMinSumAbs[i], 0); //First reset all coeffs. to zero. 
		}
		//Here we input the S matrix into the constraints
		for(int j=0; j<m.length; j++) {
			Double m_local_zero=m[j][0];  //This is the metabolite index, i.e. glop constraint index, but from 1 to N. 
			Double m_local_one=m[j][1];   //This is the reaction index, i.e. glop variable index, also 1-M.
			Double m_local_two=m[j][2];   //This is the stoichiometric coefficient.
			rxnExpressionsMinSumAbs[m_local_zero.intValue()-1].setCoefficient(rxnFluxesMinSumAbs[m_local_one.intValue()-1], m_local_two); //Here we set the soichiometric coeffs. Also map from 1-N to 0-N-1 for the indices. 
		}
		
		
		// Biomass fixed at optimal value
		//Initial bounds are arbitrary, they are dynamically changed
		biomassConstraintExpressionsMinSumAbs=solverMinSumAbs.makeConstraint(0.8244150086000599, 0.8244150086000599);
		biomassConstraintExpressionsMinSumAbs.setCoefficient(rxnFluxesMinSumAbs[objIndex-1], 1);
		
		// Objective: Minimize sum of z[i]
		objectiveMinSumAbs = solverMinSumAbs.objective();
		for (int i = 0; i < numRxns; i++) {
		     objectiveMinSumAbs.setCoefficient(rxnAbsFluxesMinSumAbs[i], 1);
		}
		objectiveMinSumAbs.setMinimization();
		
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
		
		int status = -1; 
		switch(objSty)
		{
		// the usual, just max the objective
		case FBAModel.MAXIMIZE_OBJECTIVE_FLUX:
			objective.setMaximization();
			resultStatus = solver.solve();
			for(int i=0;i<numRxns;i++)fluxes[i]=rxnFluxes[i].solutionValue();
			//System.out.println("Biomass flux 1 "+rxnFluxes[objIndex-1].solutionValue());
			//System.out.println("Glucose flux MaxObj glop "+fluxes[27]);
			break;
		//Parsimonious FBA
		case FBAModel.MAX_OBJECTIVE_MIN_TOTAL:
			objective.setMaximization();
			resultStatus = solver.solve();
			
			for(int i=0;i<numRxns;i++)fluxes[i]=rxnFluxes[i].solutionValue();
			
			if (resultStatus == MPSolver.ResultStatus.OPTIMAL)
			{
				biomassConstraintExpressionsMinSumAbs.setBounds(rxnFluxes[objIndex-1].solutionValue(),rxnFluxes[objIndex-1].solutionValue());
				objectiveMinSumAbs.setMinimization();
				resultStatus = solverMinSumAbs.solve();
				if (resultStatus == MPSolver.ResultStatus.OPTIMAL)for(int i=0;i<numRxns;i++)fluxes[i]=rxnFluxesMinSumAbs[i].solutionValue();
			}
			break;
		default:
			break;
		}

		if (resultStatus == MPSolver.ResultStatus.OPTIMAL) {
			  status = 5;
			} else if(resultStatus == MPSolver.ResultStatus.ABNORMAL)  {
			  //System.err.println("The problem does not have an optimal solution!");
			  status = -1;	;
			} else if(resultStatus == MPSolver.ResultStatus.INFEASIBLE)  {
				  //System.err.println("The problem does not have an optimal solution!");
			  status = 4;	;
			} 
			
		//if (ret == 0)
		//{   
		//	runSuccess = true; TODO get rid of this. Ilija 10.17.2023
		//}

		return status;
	}

	/**
	 * @return The fluxes from the most recent FBA run
	 */
	public double[] getFluxes()
	{
		double[] v = new double[numRxns];
		if (resultStatus == MPSolver.ResultStatus.OPTIMAL) 
		{
			for(int i=0;i<numRxns;i++)v[i]=fluxes[i];
			//for(int i=0;i<numRxns;i++)v[i]=rxnFluxes[i].solutionValue();
		} 
		else 
		{
			  //System.err.println("The problem does not have an optimal solution!");
				;
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
			else ;//System.err.println("The problem does not have an optimal solution!");
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
		if (ub.length != nrxns)
			return PARAMS_ERROR;

		for (int i=0; i<ub.length; i++) rxnFluxes[i].setUb(ub[i]); //Attention, the annoying 1-n to 0-n-1 conversion again!
		
		return PARAMS_OK;
	}
	public int setLowerBounds(int nrxns, double[] lb)
	{
		if (lb.length != nrxns)
			return PARAMS_ERROR;

		for (int i=0; i<lb.length; i++) rxnFluxes[i].setLb(lb[i]); //Attention, the annoying 1-n to 0-n-1 conversion again!
			
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
	
	/**
	 * Sets the current lower bounds (e.g. -uptake rates) for the exchange reactions
	 * in the model. Also sets it for the min. abs. sum. flux model, which should probably be changed
	 * to be set only if doing max/min (i.e. right now it sets the exch reaction even if we're just 
	 * doing max obj)
	 * @param exch
	 * @param lb
	 * @return PARAMS_OK if the lb and exch array are of the same length; 
	 * PARAMS_ERROR if not.
	 */

	public int setExchLowerBounds(int[] exch, double[] lb)
	{
		if (lb.length != exch.length)
			return PARAMS_ERROR;

		for (int i=0; i<exch.length; i++) rxnFluxes[exch[i]-1].setLb(lb[i]); //Attention, the annoying 1-n to 0-n-1 conversion again!
		for (int i=0; i<exch.length; i++) rxnFluxesMinSumAbs[exch[i]-1].setLb(lb[i]);
		//setExchLowerBoundsModelMin(exch, lb);
		return PARAMS_OK;
	}

	public int setExchUpperBounds(int[] exch, double[] ub)
	{	
		if (ub.length != exch.length)
			return PARAMS_ERROR;

		for (int i=0; i<exch.length; i++) rxnFluxes[exch[i]-1].setUb(ub[i]); //Attention, the annoying 1-n to 0-n-1 conversion again!
		for (int i=0; i<exch.length; i++) rxnFluxesMinSumAbs[exch[i]-1].setUb(ub[i]);
		//setExchLowerBoundsModelMin(exch, lb);
		return PARAMS_OK;
	}
	
	
	public double[] getExchangeFluxes(int[] exch)
	{	
		double[] v_exch = new double[exch.length];
		//if (runSuccess)
		//{
			for (int i = 0; i < exch.length; i++)
			{
				v_exch[i]=fluxes[exch[i]-1];
				//System.out.println("Exchange flux "+exch[i]+" "+v_exch[i]);
			}
			//System.out.println("Glucose flux "+v_exch[8]);
		//}
		return v_exch;
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