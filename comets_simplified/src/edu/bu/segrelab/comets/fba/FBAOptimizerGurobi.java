package edu.bu.segrelab.comets.fba;

import gurobi.*;

/**
 * This class provides the GUROBI optimizer functionality for the FBAModel class.
 * It extends the abstract FBAOptimizer class.
 * <p>
 * The best way to use this class is as follows:
 * <ol>
 * <li>The user loads a GUROBI optimizer by using a constructor directly.
 * <li>Set the lower bound for the exchange fluxes with setExchLowerBounds(int[],double[]).
 * <li>Run the optimizer with run(int).
 * <li>Fetch the results with various accessory methods: getFluxes(), getExchangeFluxes(),
 *     getObjectiveSolution() etc.
 * </ol>
 * <p>
 * 
 * Note that this class depends on the availability of the GUROBI package, which must be
 * accessible either through the classpath or the library path. Be sure to add gurobi.jar
 * to the classpath and, if on a POSIX-based system (Mac or Linux), add the path to the 
 * installed GUROBI and jni libraries in -Djava.library.path
 * 
 * @author Ilija Dukovski ilija.dukovski@gmail.com
 * created 11 Mar 2014
 */
public class FBAOptimizerGurobi extends edu.bu.segrelab.comets.fba.FBAOptimizer 
					  implements edu.bu.segrelab.comets.CometsConstants
{

	private GRBEnv env;     //Gurobi environment
	private GRBModel model; //Gurobi model
	private GRBVar[] rxnFluxes; //These are optimization variables, i.e. fluxes
	private GRBLinExpr[] rxnExpressions; //These are constraints on fluxes, one for each metabolite
	private double[] fluxesModel;
	
	private double[][] stoichMatrix; //This is used only in the clone() method. TODO eliminate this.
	
	//These are variables for the sum of absolute values minimization, the Abs is for absolute.
	private GRBEnv envAbs;
	private GRBModel modelAbs;
	private GRBVar[] rxnFluxesPlus;
	private GRBVar[] rxnFluxesMinus;
	private GRBVar[] rxnFluxesAbs;
	
	private boolean runSuccess;
	private int numMetabs;
	private int numRxns;
	private int objReaction;

	
	/**
	 * Create a new simple FBAOptimizerGurobi without any stoichiometry information loaded.
	 * Creates the Gurobi environment and model only.
	 */
	private FBAOptimizerGurobi()
	{
		runSuccess = false;
		
		try {
		env = new GRBEnv();
		env.set(GRB.IntParam.OutputFlag,0);
		model = new GRBModel(env);
		
		//Environment and model for the sum of absolute values problem
		envAbs = new GRBEnv();
		envAbs.set(GRB.IntParam.OutputFlag,0);
		modelAbs = new GRBModel(envAbs);
		
	    } catch (GRBException e) {
	      System.out.println("Error code: " + e.getErrorCode() + ". " +
	                         e.getMessage());
	    }
	}

	/**
	 * Create a new FBAOptimizerGurobi with stoichiometric matrix m, lower bounds l, upper bounds u,
	 * and objective reaction r. 
	 * @param m
	 * @param l
	 * @param u
	 * @param r
	 */
	public FBAOptimizerGurobi(double[][] m, double[] l, double[] u, int r)
	{
		this();
		
		stoichMatrix=m;
		fluxesModel = new double[l.length];
		double[] objective=new double[l.length];
		char[] types=new char[l.length];
		for(int i=0;i<l.length;i++){
			objective[i]=0;
			types[i]=GRB.CONTINUOUS;
		}
		
		try{
			
			rxnFluxes = model.addVars(l, u, objective, types, null);
			model.update();
			
			double[] rhsValues=new double[m.length];
			char[] senses=new char[m.length];
			rxnExpressions=new GRBLinExpr[m.length];
			
			for(int i=0;i<m.length;i++){
				rxnExpressions[i]=new GRBLinExpr();
				rxnExpressions[i].addTerms(m[i], rxnFluxes);
				senses[i]=GRB.EQUAL;
				rhsValues[i]=0.0;
			}

			model.addConstrs( rxnExpressions, senses, rhsValues, null);
			model.update();
	
			numMetabs = m.length;
			numRxns = m[0].length;
			
			setObjectiveReaction(numRxns, r);
			
			//Now populate the modelAbs for the minimization of sum of absolute values
			double[] objectiveAbs=new double[2*l.length];
			double[] lAbs=new double[2*l.length];
			double[] uAbs=new double[2*l.length];
			char[] typesAbs=new char[2*l.length];
			for(int i=0;i<2*l.length;i++){
				objectiveAbs[i]=0;
				typesAbs[i]=GRB.CONTINUOUS;
				lAbs[i]=0.0;
				uAbs[i]=Math.max(Math.abs(u[i%u.length]),Math.abs(l[i%u.length]));
			}
			
				//add the variables V+ and V-
				rxnFluxesAbs = modelAbs.addVars(lAbs, uAbs, objectiveAbs, typesAbs, null);
				modelAbs.update();
				rxnFluxesPlus=new GRBVar[l.length];
				rxnFluxesMinus=new GRBVar[l.length];
				for(int i=0;i<l.length;i++){
					rxnFluxesPlus[i]=rxnFluxesAbs[i];
					rxnFluxesMinus[i]=rxnFluxesAbs[l.length+i];
				}
				
				//Add the stoichiometric constraints
				double[] rhsValuesAbs=new double[m.length];
				char[] sensesAbs=new char[m.length];
				GRBLinExpr[] rxnExpressionsAbs=new GRBLinExpr[m.length];
				
				//Local copy of the negative of the stoichiometric matrix. 
				double[][] mNegative=new double[m.length][m[0].length];
				for(int i=0;i<m.length;i++){
					for(int j=0;j<m[0].length;j++){
						mNegative[i][j]=-1*m[i][j];
					}
				}
				
				
				for(int i=0;i<m.length;i++){
					rxnExpressionsAbs[i]=new GRBLinExpr();
					rxnExpressionsAbs[i].addTerms(m[i], rxnFluxesPlus);
					rxnExpressionsAbs[i].addTerms(mNegative[i],rxnFluxesMinus);
					sensesAbs[i]=GRB.EQUAL;
					rhsValuesAbs[i]=0.0;
				}
				
				modelAbs.addConstrs( rxnExpressionsAbs, sensesAbs, rhsValuesAbs, null);
				//Add bounds for vPlus-vMinus
				GRBLinExpr[] rxnExpressionsAbsLB=new GRBLinExpr[l.length];
				GRBLinExpr[] rxnExpressionsAbsUB=new GRBLinExpr[l.length];
				char[] sensesAbsLB=new char[l.length];
				char[] sensesAbsUB=new char[l.length];
				double[] rhsValuesAbsLB=new double[l.length];
				double[] rhsValuesAbsUB=new double[l.length];
				
				for(int i=0;i<l.length;i++){
					rxnExpressionsAbsLB[i]=new GRBLinExpr();
					rxnExpressionsAbsLB[i].addTerm(1, rxnFluxesPlus[i]);
					rxnExpressionsAbsLB[i].addTerm(-1, rxnFluxesMinus[i]);
					sensesAbsLB[i]=GRB.GREATER_EQUAL;
					rhsValuesAbsLB[i]=l[i];
					
					rxnExpressionsAbsUB[i]=new GRBLinExpr();
					rxnExpressionsAbsUB[i].addTerm(1, rxnFluxesPlus[i]);
					rxnExpressionsAbsUB[i].addTerm(-1, rxnFluxesMinus[i]);
					sensesAbsUB[i]=GRB.LESS_EQUAL;
					rhsValuesAbsUB[i]=u[i];
				}
				
				modelAbs.addConstrs( rxnExpressionsAbsLB, sensesAbsLB, rhsValuesAbsLB, null);
				modelAbs.update();
				modelAbs.addConstrs( rxnExpressionsAbsUB, sensesAbsUB, rhsValuesAbsUB, null);
				modelAbs.update();
		}
		catch(GRBException e){
			System.out.println("Error code: " + e.getErrorCode() + ". " +
                    e.getMessage());
		}
	}





	/**
	 * Sets the current lower bounds (e.g. -uptake rates) for the exchange reactions
	 * in the model.
	 * @param exch
	 * @param lb
	 * @return PARAMS_OK if the lb and exch array are of the same length; 
	 * PARAMS_ERROR if not.
	 */
	
	public int setExchLowerBounds(int[] exch, double[] lb)
	{
		if (lb.length != exch.length)
			return PARAMS_ERROR;
		GRBVar[] exchFluxes=new GRBVar[exch.length];
		for (int i=0; i<exch.length; i++)
		{   
			try{
				rxnFluxes[exch[i]-1].set(GRB.DoubleAttr.LB, lb[i]);
				exchFluxes[i]=rxnFluxes[exch[i]-1];
			}
			catch(GRBException e)
			{
				System.out.println("Error code: " + e.getErrorCode() + ". " +
                        e.getMessage());
			}
		}
		try{
			model.set(GRB.DoubleAttr.LB, exchFluxes,lb);
			model.update();
		}
		catch(GRBException e)
		{
			System.out.println("Error code: " + e.getErrorCode() + ". " +
                    e.getMessage());
		}
		return PARAMS_OK;
	}

	/**
	 * Sets the current upper bounds (e.g. excretion rates) for the exchange reactions
	 * in the model.
	 * @param exch
	 * @param ub
	 * @return PARAMS_OK if the ub and exch arrays are of the same length; 
	 * PARAMS_ERROR if not.
	 */
	
	public int setExchUpperBounds(int[] exch, double[] ub)
	{
		if (ub.length != exch.length)
			return PARAMS_ERROR;
		GRBVar[] exchFluxes=new GRBVar[exch.length];
		for (int i=0; i<exch.length; i++)
		{
			try{
				rxnFluxes[exch[i]].set(GRB.DoubleAttr.UB, ub[i]);
				exchFluxes[i]=rxnFluxes[exch[i]-1];
			}
			catch(GRBException e)
			{
				System.out.println("Error code: " + e.getErrorCode() + ". " +
                        e.getMessage());
			}
		}
		try{
			model.set(GRB.DoubleAttr.UB, exchFluxes,ub);
			model.update();
		}
		catch(GRBException e)
		{
			System.out.println("Error code: " + e.getErrorCode() + ". " +
                    e.getMessage());
		}
		return PARAMS_OK;
	}

	/**
	 * Sets the array of lower bounds for all fluxes.
	 * Returns PARAMS_OK if number of reactions nrxns higher than zero,
	 * MODEL_NOT_INITIALIZED if it is zero.
	 */	
	public int setLowerBounds(int nrxns, double[] lb)
	{
		if (nrxns == 0)
		{
			return MODEL_NOT_INITIALIZED;
		}
		for (int i = 0; i < nrxns; i++)
		{
			try{
				rxnFluxes[i].set(GRB.DoubleAttr.LB, lb[i]);
			}
			catch(GRBException e)
			{
				System.out.println("Error code: " + e.getErrorCode() + ". " +
                        e.getMessage());
			}
		}
		return PARAMS_OK;
	}
	
	/**
	 * @return the array of lower bounds for all fluxes.
	 * @param nrxns
	 */
	
	public double[] getLowerBounds(int nrxns)
	{   
		GRBVar[] rxnFluxesLocal=model.getVars();
		double[] l = new double[nrxns];
		for (int i = 0; i < nrxns; i++)
		{
			try{
				l[i]=rxnFluxesLocal[i].get(GRB.DoubleAttr.LB);
			}
			catch(GRBException e)
			{
				System.out.println("Error code: " + e.getErrorCode() + ". " +
                        e.getMessage());
			}
		}
		return l;
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
		if (nrxns == 0)
		{
			return MODEL_NOT_INITIALIZED;
		}
		for (int i = 0; i < nrxns; i++)
		{
			try{
				rxnFluxes[i].set(GRB.DoubleAttr.UB, ub[i]);
			}
			catch(GRBException e)
			{
				System.out.println("Error code: " + e.getErrorCode() + ". " +
                        e.getMessage());
			}
		}
		return PARAMS_OK;
	}

	/**
	 * @return the current upper bounds for all fluxes
	 * @param nrxns
	 */

	public double[] getUpperBounds(int nrxns)
	{
		GRBVar[] rxnFluxesLocal=model.getVars();
		double[] u = new double[nrxns];
		for (int i = 0; i < nrxns; i++)
		{
			try{
				u[i]=rxnFluxesLocal[i].get(GRB.DoubleAttr.UB);
			}
			catch(GRBException e)
			{
				System.out.println("Error code: " + e.getErrorCode() + ". " +
                        e.getMessage());
			}
		}
		return u;
	}


	
	/**
	 * Sets the objective reaction. Maximizing growth is probably
	 * the most common, but any reaction can be used as the objective.
	 * @param nrxns number of reactions.
	 * @param r the index of the objective reaction (1->N)
	 * @return PARAMS_OK if successful, PARAMS_ERROR if r < 1 or r > nrxns, and
	 * MODEL_NOT_INITIALIZED if there's nothing ready
	 */
	public int setObjectiveReaction(int nrxns, int r)
	{
		if (r < 1 || r > nrxns)
		{
			return PARAMS_ERROR;
		}
		if (numMetabs == 0 || numRxns == 0)
		{
			return MODEL_NOT_INITIALIZED;
		}
		GRBLinExpr expr = new GRBLinExpr();
	    expr.addTerm(1.0, rxnFluxes[r-1]);
	    
	    try{
	    	model.setObjective(expr, GRB.MAXIMIZE);
	    }
	    catch(GRBException e)
	    {
	    	System.out.println("Error code: " + e.getErrorCode() + ". " +
                    e.getMessage());
	    }
	    
		objReaction = r;
		return PARAMS_OK;
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
		switch(objSty)
		{
			// the usual, just max the objective
			case FBAModel.MAXIMIZE_OBJECTIVE_FLUX:
				try{
					GRBLinExpr objective=new GRBLinExpr();
					objective.addTerm(1.0, rxnFluxes[objReaction-1]);
					model.setObjective(objective, GRB.MAXIMIZE);
					model.update();
					model.optimize();
					rxnFluxes=model.getVars();
					// check to see if optimal
					int optimstatus = model.get(GRB.IntAttr.Status);
					if (optimstatus == GRB.Status.OPTIMAL) {
				    	for(int i=0;i<rxnFluxes.length;i++){
							fluxesModel[i]=rxnFluxes[i].get(GRB.DoubleAttr.X);
						}
				    	ret=0;
				    } else {
				    	System.out.println("   Model is not feasible");
				    	for(int i=0;i<rxnFluxes.length;i++){
							fluxesModel[i]=0;
						}
				    }
				}
				catch(GRBException e){
					System.out.println("Error code: " + e.getErrorCode() + ". " +
	                         e.getMessage());
				}
				break;
			case FBAModel.MAX_OBJECTIVE_MIN_TOTAL:
				try{
					GRBLinExpr objective=new GRBLinExpr();
					objective.addTerm(1.0, rxnFluxes[objReaction-1]);
					model.setObjective(objective, GRB.MAXIMIZE);
					model.update();
					model.optimize();

					rxnFluxes=model.getVars();
					// check to see if optimal
					int optimstatus = model.get(GRB.IntAttr.Status);
					if (optimstatus == GRB.Status.OPTIMAL) {
						//Now do the minimization of the sum of absolute values.	
						//Define new model for variables vPlus=(v+|v|)/2 and vMinus=(|v|-v)/2
						//minimize Sum|v|=Sum(vPlus+vMinus)
						
						// Fix the objective function value from the initial maximization
						// 1*pos_obj_val - 1*neg_obj_val = obj_val
						GRBLinExpr rxnExpressionAbsObj=new GRBLinExpr();
						rxnExpressionAbsObj.addTerm(1, rxnFluxesPlus[objReaction-1]);
						rxnExpressionAbsObj.addTerm(-1, rxnFluxesMinus[objReaction-1]);
						char senseAbsObj=GRB.EQUAL;
						double rhsValueAbsObj=rxnFluxes[objReaction-1].get(GRB.DoubleAttr.X);
						modelAbs.addConstr( rxnExpressionAbsObj, senseAbsObj, rhsValueAbsObj, null);
						// Minimize the sum of absolute fluxes
						// min(1*pos_flux + 1*neg_flux)
						GRBLinExpr objectiveAbs=new GRBLinExpr();
						for(int k=0;k<rxnFluxesPlus.length;k++){
							objectiveAbs.addTerm(1.0, rxnFluxesPlus[k]);
							objectiveAbs.addTerm(1.0, rxnFluxesMinus[k]);
						}
						modelAbs.setObjective(objectiveAbs,GRB.MINIMIZE);
						modelAbs.update();
						modelAbs.optimize();
						
						rxnFluxesAbs=modelAbs.getVars();
						// check to see if optimal
						int optimstatusAbs = modelAbs.get(GRB.IntAttr.Status);
						if (optimstatusAbs == GRB.Status.OPTIMAL) {
							for(int k=0;k<rxnFluxesPlus.length;k++){
								rxnFluxesPlus[k]=rxnFluxesAbs[k];
								rxnFluxesMinus[k]=rxnFluxesAbs[rxnFluxesPlus.length+k];
								
								//Finally set the fluxes to a set that minimizes the sum of absolute values
								fluxesModel[k]=rxnFluxesPlus[k].get(GRB.DoubleAttr.X)-rxnFluxesMinus[k].get(GRB.DoubleAttr.X);
							}
							ret=0;
						} else {
							System.out.println("   Model is not feasible in second optimization");
						//Set fluxes to non-minimized values
							for(int i=0;i<rxnFluxes.length;i++){
								fluxesModel[i]=rxnFluxes[i].get(GRB.DoubleAttr.X);
							}
					    	ret=0;
						}
					} else {
						System.out.println("   Model is not feasible");
						for(int i=0;i<rxnFluxes.length;i++){
							fluxesModel[i]=0;
						}
					}
					
				}
				catch(GRBException e){
					System.out.println("Error code: " + e.getErrorCode() + ". " +
	                         e.getMessage());
				}
				break;
				
			default:
		
				break;
		}
		
		if (ret == 0)
		{   
			runSuccess = true;
		}
		
        return 5;
	}

	
	/**
	 * @return The fluxes from the most recent FBA run
	 */
	public double[] getFluxes()
	{

		double[] v = new double[numRxns];
		
		if (runSuccess)
		{
			for (int i = 0; i < numRxns; i++)
			{
				v[i]=fluxesModel[i];
			}
		}
		return v;
	}
    
	
	// Debugging code
	/*
	public double[] getFluxesTest(double[] test)
	{
		double[] v = new double[numRxns];
		if (runSuccess)
		{
			for (int i = 0; i < numRxns; i++)
			{
				try{
					v[i] = rxnFluxes[i].get(GRB.DoubleAttr.X);
				}
				catch(GRBException e)
				{
					System.out.println("Error code: " + e.getErrorCode() + ". " +
	                         e.getMessage());
				}
				//v[i]=fluxes[i];
				System.out.println(fluxesModel[i]+" "+v[i]+" "+test[i]);
			}
		}
		return v;
	}
	*/
	
	/**
	 * @return the exchange fluxes from the most recent FBA run
	 * @param exch list of exchange reactions
	 */
	public double[] getExchangeFluxes(int[] exch)
	{
		double[] v = new double[exch.length];
		if (runSuccess)
		{
			for (int i = 0; i < exch.length; i++)
			{
				v[i]=fluxesModel[exch[i]-1];
			}
		}
		return v;
	}





	/**
	 * If the FBA run was successful, this returns
	 * the value of the objective solution. Otherwise, it returns -Double.MAX_VALUE
	 * @return either the objective solution of -Double.MAX_VALUE
	 * @param objreact Objective reaction index.
	 */
	public double getObjectiveSolution(int objreact)
	{
		try{
			if (runSuccess)
				return rxnFluxes[objreact-1].get(GRB.DoubleAttr.X);
		}
		catch(GRBException e){
			System.out.println("Error code: " + e.getErrorCode() + ". " +
                    e.getMessage());
		}
		return -Double.MAX_VALUE;
	}
	
	/**
	 * A repeat of getObjective Solution due to GLPK legacy.
	 * TODO Fix this. 
	 * @return
	 */
	public double getObjectiveFluxSolution(int objreact)
	{
		try{
			if (runSuccess)
				return rxnFluxes[objreact-1].get(GRB.DoubleAttr.X);
		}
		catch(GRBException e){
			System.out.println("Error code: " + e.getErrorCode() + ". " +
                    e.getMessage());
		}
		return -Double.MAX_VALUE;
	}
	
	/**
	 * Returns the status of FBA (feasible or infeasible).
	 * @return
	 */
	public int getFBAstatus()
	{
		if (runSuccess)
			return 1; // feasible
		else
			return 0; // not feasible
	}



	/**
	 * Sets the upper bound on the objective reaction. 
	 * @param objreact
	 * @param ub
	 * @return PARAMS_OK 
	 */
	
	public int setObjectiveUpperBound(int objreact, double ub)
	{
		GRBVar[] objFlux=new GRBVar[1];
		double[] ubarray=new double[1];
		try{
			rxnFluxes[objreact-1].set(GRB.DoubleAttr.UB, ub);
			objFlux[0]=rxnFluxes[objreact-1];
			ubarray[0]=ub;
			model.set(GRB.DoubleAttr.UB,objFlux,ubarray);
			model.update();
		}
		catch(GRBException e)
		{
			System.out.println("Error code: " + e.getErrorCode() + ". " +
                    e.getMessage());
		}
		
		return PARAMS_OK;
	}
	
	/**
	 * Sets the lower bound on the objective reaction. 
	 * @param objreact
	 * @param lb
	 * @return PARAMS_OK 
	 */
	
	public int setObjectiveLowerBound(int objreact, double lb)
	{
		GRBVar[] objFlux=new GRBVar[1];
		double[] lbarray=new double[1];
		try{
			rxnFluxes[objreact-1].set(GRB.DoubleAttr.LB, lb);
			objFlux[0]=rxnFluxes[objreact-1];
			lbarray[0]=lb;
			model.set(GRB.DoubleAttr.LB,objFlux,lbarray);
			model.update();
		}
		catch(GRBException e)
		{
			System.out.println("Error code: " + e.getErrorCode() + ". " +
                    e.getMessage());
		}
		
		return PARAMS_OK;
	}
	
	
	/**
	 * Produces a clone of this <code>FBAOptimizerGurobi</code> with all parameters intact.
	 */
	
	public FBAOptimizerGurobi clone()
	{

		GRBVar[] rxnFluxesLocal= model.getVars();
		double[] l=new double[rxnFluxesLocal.length];
		double[] u=new double[rxnFluxesLocal.length];
		//double[][] m=new double[][];
		int r = objReaction;
		try{
			for(int k=0;k<rxnFluxesLocal.length;k++)
			{
				l[k]=rxnFluxesLocal[k].get(GRB.DoubleAttr.LB);
				u[k]=rxnFluxesLocal[k].get(GRB.DoubleAttr.UB);
			}
		}
		catch(GRBException e){
			System.out.println("Error code: " + e.getErrorCode() + ". " +
                    e.getMessage());
		}
		FBAOptimizerGurobi optimizerCopy=new FBAOptimizerGurobi(stoichMatrix,l,u,r);

		return optimizerCopy;
	}



}