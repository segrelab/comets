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
 * 
 * 
 * 26 Aug 2015: min of sum of abs. flux algorithm 
 * implemented following the implementation in
 * FBA_min_fluxAV.m (from Segre FBA_showcase) 
 * @author Jeremy M. Chacon  chaco001@umn.edu
 * 
 * 
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
	
	
	//Chacon sum of abs min vars
	private GRBEnv envMin;
	private GRBModel modelMin;
	private GRBVar[] modelMinVars;
	private int nVars; // will be double the amount of reactions in S
	private final String biomassConstraintName = "biomassConstraint"; // useful for getting this constraint to reset the right-hand-side
	
	
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
		
		
		// this should probably only be bothered with if we're doing max/min, 
		// but right now this class doesn't know the objective style until run()
		initializeAbsModel(m, l, u, r);
		
		//everything below here is for initializing the basic model
		stoichMatrix=m;
		fluxesModel = new double[l.length];
		double[] objective=new double[l.length];
		char[] types=new char[l.length]; // continuous vs. binary, etc
		//make vector of zeros for the objective, which will get filled in with a '1' later
		//all our variables (fluxes) are always continuous
		for(int i=0;i<l.length;i++){
			objective[i]=0;
			types[i]=GRB.CONTINUOUS;
		}
		
		try{
			rxnFluxes = model.addVars(l, u, objective, types, null);
			//must either update() or optimize() for changes to take effect
			model.update();
			
			double[] rhsValues=new double[m.length]; // right-hand-side values, usually zero (Sv=0)
			char[] senses=new char[m.length]; // represents equals, less than, etc. this is equals for most of ours (Sv EQUALS 0)
			rxnExpressions=new GRBLinExpr[m.length]; //generate expressions to represent each matrix row
			
			//this makes each expression, which corresponds to a row of Sv = 0. 
			// e.g. if S = [[-1 0 -1], [1 -1 0]] then 
			// rxnExpression[0] is basically  -1*v1 + 0*v2 + -1*v3 = 0.0
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
			
		}
		catch(GRBException e){
			System.out.println("Error code: " + e.getErrorCode() + ". " +
                    e.getMessage());
		}
	}
	
	/**
	 * 
	 * @param m the stoichoimetric matrix
	 * @param l lower bounds
	 * @param u upper bounds
	 * @param r the reaction to be maximized (the biomass reaction)
	 */
	public void initializeAbsModel(double[][] m, double[] l, double[] u, int r){
		/*
		 * basically, if we start with this S matrix:
		 * 
		 * S = [-1  0 -1    = 0
		 *       1 -1  0]   = 0
		 * 
		 * lb = [0  0 -1]
		 * ub = [1  1  1]
		 * c =  [0  1  0]
		 * 
		 * we want to create this S matrix in order to minimize flux while holding the obj. constant:
		 * 
		 *    orig. vars  new dummy vars
		 *       -------  -------
		 * S = [-1  0 -1  0  0  0    =  0
		 *       1 -1  0  0  0  0    =  0
		 *       0  1  0  0  0  0    =  whatever the optimized flux was (i.e. gets set each time step)
		 *       1  0  0 -1  0  0    <= 0
		 *      -1  0  0 -1  0  0    <= 0   
		 *       0  1  0  0 -1  0    <= 0
		 *       0 -1  0  0 -1  0    <= 0
		 *       0  0  1  0  0 -1    <= 0
		 *       0  0 -1  0  0 -1]   <= 0
		 *       
		 * lb =  0  0 -1  0  0  0       i.e.  original lb then zeros 
		 * ub =  1  1  1  Inf Inf Inf   i.e.  original ub then max value allowed
		 * c  =  0  0  0  1  1  1       i.e.  zeroes for original flux variables, ones for the "dummy" variables
		 * 
		 *       So everything can be set in one step, except:
		 *       
		 *       1. the right-hand-side of the "biomass" constraint
		 *       2. the lower bounds of any exchange metabolites, which depend on media availability
		 *       
		 *       Note that I use vars and flux reactions interchangeably in some comments. 
		 *       
		 *       While the bounds are a form of constraints, below I typically only
		 *       refer to row constraints by the term constraint. 
		 */
		
		
		//make as many vars as bounds * 2
		nVars = l.length * 2;
		createEmptyModelMin();
		createVarsModelMin(l, u); // generates all variables, including the dummies
		createObjFuncModelMin(); // generates the minimization objective function of half zeros, half ones
		addOrigConstraintsModelMin(m); // adds the baseline Sv = 0 constraints (the stoichiometry constraints)
		addBiomassConstraintModelMin(r); // adds the constraint that will be used to fix biomass
		addAbsSumConstraintsModelMin();	// adds the constraints that are req'd to minimize sum of abs. fluxes
	}
	
	/**
	 * createEmptyModelMin generates an empty model for use with
	 * minimization of sum of absolute value of fluxes
	 */
	private void createEmptyModelMin(){
		try{
			envMin = new GRBEnv();
			envMin.set(GRB.IntParam.OutputFlag,0);
			modelMin = new GRBModel(envMin);
		}
		catch (GRBException e) {
		      System.out.println("Error code: " + e.getErrorCode() + ". " +
                      e.getMessage());
		}
	}
	
	private void createVarsModelMin(double[] l, double[] u){
		double[] lbMin = new double[nVars];
		double[] ubMin = new double[nVars];
		double[] objMin = new double[nVars];
		char[] cTypeMin = new char[nVars];
		// populate the vars as in the max/min example in FBA_showcase
		// first copy the bounds for the original rxns
		for (int k = 0; k < nVars/2; k++){
			lbMin[k] = l[k];
			ubMin[k] = u[k];
			objMin[k] = 0;
			cTypeMin[k] = GRB.CONTINUOUS;
		}
		// now populate the bounds for the dummy vars
		for (int k = nVars/2; k < nVars; k++){
			lbMin[k] = 0;
			ubMin[k] = Double.MAX_VALUE;
			objMin[k] = 1;
			cTypeMin[k] = GRB.CONTINUOUS;			
		}
		// now create the variables
		try{
			modelMinVars = modelMin.addVars(lbMin, ubMin, objMin, cTypeMin, null);
			modelMin.update();
		}		
		catch(GRBException e){
			System.out.println("  error in createVarsModelMin: ");
			System.out.println("Error code: " + e.getErrorCode() + ". " +
                    e.getMessage());
		}

	}
	
	private void createObjFuncModelMin(){
		//now setup the objective. this is only 1's for the dummy variables, hence the forloop start spot
		GRBLinExpr objectiveFunc = new GRBLinExpr();
		for (int k = nVars/2; k < nVars; k++){
			objectiveFunc.addTerm(1.0, modelMinVars[k]);		
		}
	    try{
	    	modelMin.setObjective(objectiveFunc, GRB.MINIMIZE); // have to un-hard code this probably
	    }
	    catch(GRBException e)
	    {
			System.out.println("  error in createObjFuncModelMin: ");
	    	System.out.println("Error code: " + e.getErrorCode() + ". " +
                    e.getMessage());
	    }
	}

	private void addOrigConstraintsModelMin(double[][] m){
		//add original constraints (Sv = 0)  -- rows 1,2 in the example above
		// no need to go through all of the variables because the
		// stoichoimetric matrix we are putting together has zeros
		// for these variables in the original row
		int nMetabolites = m.length;
		GRBLinExpr[] origConstraints = new GRBLinExpr[nMetabolites];
		char[] senses = new char[nMetabolites]; // holds the "=" in Sv = 0
		double[] rhs = new double[nMetabolites]; // holds the "0" in Sv = 0
		
		for (int k = 0; k < nMetabolites; k++){
			// make the left-hand-side expressions
			origConstraints[k] = new GRBLinExpr();
			for (int j = 0; j < nVars / 2; j++){
				origConstraints[k].addTerm(m[k][j],	modelMinVars[j]);
			}
			// for these expressions, all senses are =, all rhs are 0
			senses[k] = GRB.EQUAL;
			rhs[k] = 0;
		}
		
		//add the constraints to the model
		try{
			modelMin.addConstrs(origConstraints, senses, rhs, null);
			modelMin.update();
		}		
		catch(GRBException e){
			System.out.println("  error in addOrigConstrainsModelMin: ");
			System.out.println("Error code: " + e.getErrorCode() + ". " +
                    e.getMessage());
		}
	}

	private void addBiomassConstraintModelMin(int biomassVarNum){
		/* adds the biomass constraint -- row three in the example above
		 * this add a new row to the S matrix with a 1 in the optimized reaction
		 * (e.g. biomass) and sets it equal to the optimized value. I think
		 * the way to do this is to set up the entire constraint, including the 
		 * right-hand side, which I'll set to zero. I'll give this constraint 
		 * the name "biomass," then update its right-hand-side each timestep
		 */
		GRBLinExpr biomassConstraint = new GRBLinExpr();
		char biomassSense = GRB.EQUAL;
		double biomassRHS = 0; // this is what changes dynamically
		double biomassCoef = 1;
		biomassConstraint.addTerm(biomassCoef, modelMinVars[biomassVarNum - 1]); // need minus one because this will be one-ordered, must have zero-ordered
		
		//add the constraints to the model
		try{
			modelMin.addConstr(biomassConstraint, biomassSense, biomassRHS, biomassConstraintName);
			modelMin.update();
		}		
		catch(GRBException e){
			System.out.println("  error in addBiomassConstraintModelMin: ");
			System.out.println("Error code: " + e.getErrorCode() + ". " +
                    e.getMessage());
		}
	}

	private void addAbsSumConstraintsModelMin(){
		// add the constraints that force the min absolute value of the fluxes -- rows 4-9 in the example above
		// note that all these constraints are <= zero, in contrast to the typical FBA constraints
		GRBLinExpr[] absSumConstraints = new GRBLinExpr[nVars];
		int counter = 0;
		int nOrigReactions = nVars / 2;
		char[] senses = new char[nVars];
		double[] rhs = new double[nVars];
		
		//generate the expressions
		for (int k = 0; k < nOrigReactions; k++){
			// add the constraint that minimizes positive flux through this reaction
			absSumConstraints[counter] = new GRBLinExpr();
			absSumConstraints[counter].addTerm(1, modelMinVars[k]);
			absSumConstraints[counter].addTerm(-1, modelMinVars[k + nOrigReactions]);
			counter++;
			// add the constraint the minimized negative flux through this reaction
			absSumConstraints[counter] = new GRBLinExpr();
			absSumConstraints[counter].addTerm(-1, modelMinVars[k]);
			absSumConstraints[counter].addTerm(-1, modelMinVars[k + nOrigReactions]);
			counter++;
		}
		
		//populate the senses and rhs
		for (int k = 0; k < nVars; k++){
			senses[k] = GRB.LESS_EQUAL;
			rhs[k] = 0;
		}
		
		//add the constraints to the model
		try{
			modelMin.addConstrs(absSumConstraints, senses, rhs, null);
			modelMin.update();
		}		
		catch(GRBException e){
			System.out.println("  error in addAbsSumConstraintsModelMin: ");
			System.out.println("Error code: " + e.getErrorCode() + ". " +
                    e.getMessage());
		}		
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
		
		
		setExchLowerBoundsModelMin(exch, lb);
		return PARAMS_OK;
	}
	
	/**
	 * Sets the lower bounds for the min sub abs flux model
	 * Called by setExchLowerBounds()
	 * @param exch a list of the exchange reaction indices
	 * @param lb the new lower bounds for these reactions
	 */
	private void setExchLowerBoundsModelMin(int[] exch, double[] lb){
		GRBVar[] exchFluxes = new GRBVar[exch.length];
		// grab exchange flux vars from the model
		for (int k = 0; k < exch.length; k++){
			exchFluxes[k] = modelMinVars[exch[k] - 1]; // minus one because the exchange indices are 1-ordered
		}
		// set the exchange flux lower bounds all at once
		try{
			modelMin.set(GRB.DoubleAttr.LB,  exchFluxes, lb);
		}
		catch(GRBException e)
		{
			System.out.println("Error code: " + e.getErrorCode() + ". " +
                    e.getMessage());
		}
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
				try{
					GRBLinExpr objective=new GRBLinExpr();
					objective.addTerm(1.0, rxnFluxes[objReaction-1]);
					model.setObjective(objective, GRB.MAXIMIZE);
					model.update();
					model.optimize();
					
					status = model.get(GRB.IntAttr.Status);
					
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
					
					// first find the maximized objective with vanilla FBA (e.g. biomass)
					GRBLinExpr objective=new GRBLinExpr();
					objective.addTerm(1.0, rxnFluxes[objReaction-1]);
					model.setObjective(objective, GRB.MAXIMIZE);
					model.update();
					model.optimize();
					
					
					rxnFluxes=model.getVars();
					
					// now fix the biomass in the min abs. sum. model, then run that one.
					double maximizedObjective = rxnFluxes[objReaction-1].get(GRB.DoubleAttr.X);
					setObjectiveFluxToSpecificValue(maximizedObjective);
					
					modelMin.optimize();
					
					status = model.get(GRB.IntAttr.Status);

					
					// save the new fluxes.
					modelMinVars =modelMin.getVars();
					for (int k = 0; k < modelMinVars.length / 2; k++){
						fluxesModel[k] = modelMinVars[k].get(GRB.DoubleAttr.X);
					}
					
					ret=0;
					
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
		
		// convert gurobi status indicators into GLPK statuses
		if (status == 2){
			status = 5;
		}else if(status == 3){
			status = 4;
		}
		
        return status;
	}
	
	/**
	 * setObjectiveFluxToSpecificValue is used when minimizing the sum of the absolute value of the fluxes, while keeping the flux
	 * of the biomass reaction constant.
	 * 
	 * @param objectiveFlux the amount at which the objective flux reaction, typically biomass, should be fixed 
	 */
	private void setObjectiveFluxToSpecificValue(double objectiveFlux){
		GRBConstr[] biomassFluxVar = new GRBConstr[1]; 
		// grab the biomass constraint by name (this is the row constraint associated with the objective reaction -- row 3 in the example) 
		try{
			biomassFluxVar[0] = modelMin.getConstrByName(biomassConstraintName);
		}
		catch(GRBException e){
			System.out.println("   Error in setObjectiveFluxToSpecificValue");
			System.out.println("Error code: " + e.getErrorCode() + ". " +
                     e.getMessage());
		}
		double[] v = new double[1];
		v[0] = objectiveFlux;
		// change the right-hand-side attribute of the 
		try{
			modelMin.set(GRB.DoubleAttr.RHS, biomassFluxVar, v);
		}
		catch(GRBException e){
			System.out.println("   Error in setObjectiveFluxToSpecificValue");
			System.out.println("Error code: " + e.getErrorCode() + ". " +
                     e.getMessage());
		}
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