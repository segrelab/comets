package edu.bu.segrelab.comets.fba;


import org.gnu.glpk.GLPK;
import org.gnu.glpk.GLPKConstants;
import org.gnu.glpk.SWIGTYPE_p_double;
import org.gnu.glpk.SWIGTYPE_p_int;
import org.gnu.glpk.glp_iptcp;
import org.gnu.glpk.glp_prob;
import org.gnu.glpk.glp_smcp;

/**
 * This class provides the GLPK optimizer functionality for the FBAModel class.
 * It extends the abstract FBAOptimizer class.
 * <p>
 * The best way to use this class is as follows:
 * <ol>
 * <li>The user loads a GLPK optimizer by using a constructor directly.
 * <li>Set the lower bound for the exchange fluxes with setExchLowerBounds(int[],double[]).
 * <li>Run the optimizer with run(int).
 * <li>Fetch the results with various accessory methods: getFluxes(), getExchangeFluxes(),
 *     getObjectiveSolution() etc.
 * </ol>
 * <p>
 * 
 * Note that this class depends on the availability of the GLPK package, which must be
 * accessible either through the classpath or the library path. Be sure to add glpk.jar
 * to the classpath and, if on a POSIX-based system (Mac or Linux), add the path to the 
 * installed GLPK and jni libraries in -Djava.library.path
 * 
 * @author Bill Riehl briehl@bu.edu, Ilija Dukovski ilija.dukovski@gmail.com
 * created 3 Mar 2010; modified 11 Mar 2014
 */
public class FBAOptimizerGLPK extends edu.bu.segrelab.comets.fba.FBAOptimizer 
					  implements edu.bu.segrelab.comets.CometsConstants
{


	/**
	 * Necessary to make sure the system loads up the right libraries.
	 * Later versions might want to have the Windows library not 
	 * be a static name with version. Or at least rename it, or something.
	 * 
	 * //TODO include this as some kind of program argument, NOT just hard coded.
	 */
	static
	{
		try
		{
			System.loadLibrary("glpk_java");
//			GLPK.init_glpk();
		}
		catch (UnsatisfiedLinkError e)
		{
			try {
				System.out.println(e);
				System.loadLibrary("glpk_4_47");
				System.loadLibrary("glpk_4_47_java");				
			} catch (UnsatisfiedLinkError e2) {
				System.out.println(e2);
				System.loadLibrary("glpk_4_44");
				System.loadLibrary("glpk_4_44_java");
			}
		}
	}
	
	public static final int SIMPLEX_METHOD = 0;
	public static final int INTERIOR_POINT_METHOD = 1;

	private int glpkSolverMethod = SIMPLEX_METHOD; // initialize as simplex
	private glp_prob lp;	// standard fba lp model
	private glp_prob lpMSA; // fba lp model that also minimizes the sum of absolute values 
							// of fluxes, while maximizing the objective flux
	private glp_smcp simParam;     // simplex parameter structure
	private glp_iptcp intParam;    // interior-point parameter structure
	private int numRxns;
	private int numMetabs;
	private int numExch;
	private boolean runSuccess;

	// because these are short, store them in the class, so they don't have to be
	// initialized every single time...
	SWIGTYPE_p_int forceIdx;
	SWIGTYPE_p_double forceVal;
	SWIGTYPE_p_int lpRow;
	SWIGTYPE_p_int msaRow;
	
	//private int[] exch; // indices of exchange fluxes.
	// As in the GLPK model idiom,
	// these go from 1 -> n, not 0 -> n-1

	private int objReaction;
	private int objStyle;
	

	private static int[] GLPIntParam = new int[] { 0, 
			1, 0, 1, 0,
			Integer.MAX_VALUE, Integer.MAX_VALUE, 200, 1, 2, 0, 1, 0, 0, 3, 2,
			1, 0, 2, 0, 1 };

	private static double[] GLPRealParam = new double[] { 0.07, 
			1e-7, 1e-7,
			1e-10, -Double.MAX_VALUE, 
			Double.MAX_VALUE, Integer.MAX_VALUE, 
			0.0, 1e-5, 
			1e-7, 0.0 };


	/**
	 * Create a new simple FBAModel without any stoichiometry information loaded.
	 */
	private FBAOptimizerGLPK()
	{
		runSuccess = false;
		lp = GLPK.glp_create_prob();
		lpMSA = GLPK.glp_create_prob();

		forceIdx = GLPK.new_intArray(2);
		forceVal = GLPK.new_doubleArray(2);
		lpRow = GLPK.new_intArray(1);
		msaRow = GLPK.new_intArray(1);

		GLPK.glp_term_hook(null, null);
		GLPK.glp_set_obj_dir(lp, GLPK.GLP_MAX); // this is vanilla FBA,
		GLPK.glp_set_obj_dir(lpMSA, GLPK.GLP_MAX); // this minimizes the sum of |flux|
		objStyle = FBAModel.MAXIMIZE_OBJECTIVE_FLUX;		 // so maximize by default
		
		setParameters();
	}

	/**
	 * Create a new FBAModel with stoichiometric matrix m, lower bounds l, upper bounds u,
	 * and objective reaction r. Note that, as we're using GLPK, r is from 1->N, not
	 * 0->N-1.
	 * @param m
	 * @param l
	 * @param u
	 * @param r
	 */
	public FBAOptimizerGLPK(double[][] m, double[] l, double[] u, int r)
	{
		this();

		setStoichiometricMatrix(m);
		setBounds(l, u);
		//setBaseBounds(l, u);
		setObjectiveReaction(numRxns, r);
	}


	/**
	 * Returns the current solver method being used.
	 * @return either SIMPLEX_METHOD or INTERIOR_POINT_METHOD
	 */
	public int getSolverMethod()
	{
		return glpkSolverMethod;
	}
	
	/**
	 * Sets the numerical method to be used by GLPK.
	 * @param method must be either SIMPLEX_METHOD or INTERIOR_POINT_METHOD, or will be
	 * ignored
	 */
	public void setSolverMethod(int method)
	{
		if (method != SIMPLEX_METHOD && 
			method != INTERIOR_POINT_METHOD)
			return;
		glpkSolverMethod = method;
	}

	/**
	 * Sets the model name.
	 * @param name
	 */
	public void setModelName(String name)
	{
		GLPK.glp_set_prob_name(lp, name);
		GLPK.glp_set_prob_name(lpMSA, name + "_MSA");
	}

	/**
	 * @return the model's name
	 */
	public String getModelName()
	{
		return GLPK.glp_get_prob_name(lp);
	}
	

	/**
	 * Sets the stoichiometric matrix for this FBAModel.
	 * @param m the matrix
	 */
	private void setStoichiometricMatrix(double[][] m)
	{
		/* We're gonna do this twice.
		 * Once for the "standard" FBA (Sv == 0)
		 *    this is the 'lp' variable
		 * Once for the optimal sum of absolute values of fluxes (max/min sum(|vi|))
		 *    this is the 'lpMSA' variable
		 * 
		 * The lp variable is simple, just the MxN S matrix.
		 * 
		 * The lpMSA matrix has a different structure:
		 * 
		 *      flux-variables    dummy vars for |flux|
		 * [     [ S(M x N) ]       [ zero(M x N) ]    ] Standard FBA ( Sv == 0 )
		 * [     [  eye(N)  ]       [  -1*eye(N)  ]    ] v - dummy <= 0 
		 * [     [ -1*eye(N)]       [  -1*eye(N)  ]    ] -v - dummy <= 0
		 *
		 * The total number of rows = M + N + N
		 * Total number of columns = N + N
		 */
		
		numMetabs = m.length;
		numRxns = m[0].length;
		
		/***** INITIALIZE lp VARIABLE - STANDARD FBA *****/
		GLPK.glp_add_cols(lp, numRxns); // number of columns in S-matrix
		GLPK.glp_add_cols(lpMSA, numRxns*2);

		// all columns (flux variables) are continuous
		for (int i = 0; i < numRxns; i++)
		{
			GLPK.glp_set_col_name(lp, i + 1, ("r" + (i + 1)));
			GLPK.glp_set_col_kind(lp, i + 1, GLPK.GLP_CV);
			
			GLPK.glp_set_col_name(lpMSA, i+1, ("r" + (i+1)));
			GLPK.glp_set_col_kind(lpMSA, i+1, GLPK.GLP_CV); 
			
			GLPK.glp_set_col_name(lpMSA, i+numRxns+1, ("d" + (i+1)));
			GLPK.glp_set_col_kind(lpMSA, i+numRxns+1, GLPK.GLP_CV);
		}
		GLPK.glp_add_rows(lp, numMetabs); // number of rows in S-matrix
		GLPK.glp_add_rows(lpMSA, numMetabs + numRxns + numRxns);

		// set the row types (constraints) to be fixed at 0
		// (e.g. all dX/dt == 0) for all metabolites X
		for (int i = 0; i < numMetabs; i++)
		{
			GLPK.glp_set_row_bnds(lp, i + 1, GLPK.GLP_FX, 0, 0);
			GLPK.glp_set_row_bnds(lpMSA, i + 1, GLPK.GLP_FX, 0, 0);
		}

		// Set the dummy variables for the minimizing sum of abs. values. to
		// be upper bounded by 0. E.g. v(i) - dummy(i) <= 0 and -v(i) - dummy(i) <= 0
		for (int i = numMetabs; i < numMetabs+ 2*numRxns; i++)
		{
			GLPK.glp_set_row_bnds(lpMSA, i + 1, GLPK.GLP_UP, 0, 0);
		}

		/*
		 * gotta linearize m for this to work, and provide (i,j) coords in ia
		 * and ja.
		 * 
		 * example: m = [10 20 50] [90 15 30]
		 * 
		 * ia = [1] ja = [1] mLin = [10] |1| |2| |20| |1| |3| |50| |2| |1| |90|
		 * |2| |2| |15| [3] [3] [30]
		 * 
		 * This'll likely get ridiculous for, say the human FBA model, but we'll
		 * see how it goes.
		 */
		SWIGTYPE_p_int ia = GLPK.new_intArray(numMetabs * numRxns + 1);
		SWIGTYPE_p_int ja = GLPK.new_intArray(numMetabs * numRxns + 1);
		SWIGTYPE_p_double mLin = GLPK.new_doubleArray(numMetabs * numRxns + 1);

		int ne = 0;
		for (int i = 0; i < numMetabs; i++)
		{
			for (int j = 0; j < numRxns; j++)
			{
				if (m[i][j] != 0)
				{
					ne++;
					GLPK.intArray_setitem(ia, ne, i + 1);
					GLPK.intArray_setitem(ja, ne, j + 1);
					GLPK.doubleArray_setitem(mLin, ne, m[i][j]);
					// ia[ne] = i+1;
					// ja[ne] = j+1;
					// mLin[ne] = m[i][j];
				}
			}
		}
		GLPK.glp_load_matrix(lp, ne, ia, ja, mLin);

		// Repeat for the min sum abs vals version. This is a little more complex, as it
		// only calculates the indices for nonzero values (outside of the S-matrix)
		SWIGTYPE_p_int iaMSA = GLPK.new_intArray((numMetabs * numRxns) + (4*numRxns) + 1);
		SWIGTYPE_p_int jaMSA = GLPK.new_intArray((numMetabs * numRxns) + (4*numRxns) + 1);
		SWIGTYPE_p_double mLinMSA = GLPK.new_doubleArray((numMetabs * numRxns) + (4*numRxns) + 1);
	
		ne = 0;
		for (int i=0; i<numMetabs + 2*numRxns; i++)
		{
			for (int j=0; j<2*numRxns; j++)
			{
				// case 1: i < numMetabs, j < numRxns == m[i][j]
				if (i < numMetabs && j < numRxns && m[i][j] != 0)
				{
					ne++;
					GLPK.intArray_setitem(iaMSA, ne, i+1);
					GLPK.intArray_setitem(jaMSA, ne, j+1);
					GLPK.doubleArray_setitem(mLinMSA, ne, m[i][j]);
				}
				
				// case 2: i < numMetabs, j >= numRxns == 0
				// we can skip this one -- we only pass nonzero elements
				// to the lp matrix
				else if (i < numMetabs && j >= numRxns)
				{
					continue;
				}
				
				// case 3: numMetabs <= i < numMetabs + numRxns &&
				//         j < numRxns
				// this section is a diagonal of ones. only add one
				// if i-numMetabs == j
				else if ((i >= numMetabs && i < numMetabs + numRxns) &&
						 j < numRxns && 
						 i - numMetabs == j)
				{
					ne++;
					GLPK.intArray_setitem(iaMSA, ne, i+1);
					GLPK.intArray_setitem(jaMSA, ne, j+1);
					GLPK.doubleArray_setitem(mLinMSA, ne, 1.0);
				}
				
				// case 4: numMetabs <= i < numMetabs + numRxns &&
				//         j >= numRxns
				// a diagonal of -1, only set the value if i-numMetabs == j-numRxns
				else if ((i >= numMetabs && i < numMetabs + numRxns) &&
						 j >= numRxns && i - numMetabs == j - numRxns)
				{
					ne++;
					GLPK.intArray_setitem(iaMSA, ne, i+1);
					GLPK.intArray_setitem(jaMSA, ne, j+1);
					GLPK.doubleArray_setitem(mLinMSA, ne, -1.0);
				}
				
				// case 5/6: i >= numMetabs + numRxns, j is irrelevant
				// both of these blocks are negative identity matrices.
				// set the value to be -1 if i-numRxns-numMetabs == j or j-numRxns
				else if (i >= numMetabs + numRxns &&
						 ((i-numMetabs-numRxns == j) ||
						  (i-numMetabs-numRxns == j-numRxns)))
				{
					ne++;
					GLPK.intArray_setitem(iaMSA, ne, i+1);
					GLPK.intArray_setitem(jaMSA, ne, j+1);
					GLPK.doubleArray_setitem(mLinMSA, ne, -1.0);
				}
			}
		}
		GLPK.glp_load_matrix(lpMSA, ne, iaMSA, jaMSA, mLinMSA);

		/* additional stuff to init for lpMSA -
		 * 1. bounds.
		 *    the bounds for the dummy variables (columns numRxns -- 2*numRxns)
		 *	  should range from 0 -- infinity. for some value of infinity.
		 * 2. objective (Cvector)
		 *    this should be the sum of the dummy variables, eg., put a 1 in
		 *    all of the vector positions from N+1..2N
		 * These only get done once, so might as well do them here.
		 */
		for (int i = 1; i <= numRxns*2; i++)
		{
			GLPK.glp_set_obj_coef(lpMSA, i, 0);
		}
		for (int i=numRxns+1; i<=numRxns*2; i++)
		{
			GLPK.glp_set_col_bnds(lpMSA, i, GLPK.GLP_DB, 0, Double.MAX_VALUE);
			// reaction r is the objective, so set only r to be 1.
			GLPK.glp_set_obj_coef(lpMSA, i, 1);
		}
		GLPK.glp_set_obj_name(lpMSA, "sum_values");
	}

	/**
	 * Sets the upper and lower bounds for all fluxes (variables) in the model
	 * @param lb the set of lower bounds
	 * @param ub the set of upper bounds
	 * @return PARAMS_OK if all lower bounds are less than or equal to the upper bounds,
	 * and both arrays are of the appropriate length; PARAMS_ERROR if they're incorrect;
	 * and MODEL_NOT_INITIALIZED if there's no matrix loaded
	 */
	public int setBounds(double[] lb, double[] ub)
	{
		if (lb.length != ub.length)
		{
			return PARAMS_ERROR;
		}
		if (numMetabs == 0 || numRxns == 0)
		{
			return MODEL_NOT_INITIALIZED;
		}
		for (int i = 0; i < lb.length; i++)
		{
			if (lb[i] > ub[i])
				return PARAMS_ERROR;
		}
		for (int i = 0; i < lb.length; i++)
		{
			int type = GLPKConstants.GLP_DB;
			if (lb[i] == ub[i])
				type = GLPKConstants.GLP_FX;
			GLPK.glp_set_col_bnds(lp, i + 1, type, lb[i], ub[i]);
			GLPK.glp_set_col_bnds(lpMSA, i + 1, type, lb[i], ub[i]);
		}
		return PARAMS_OK;
	}

	/**
	 * Sets the current lower bounds for the FBA problem
	 * @param lb lower bounds array
	 * @return PARAMS_OK if the lb array is of the appropriate length; 
	 * PARAMS_ERROR if not,
	 * and MODEL_NOT_INITIALIZED if there's no matrix loaded
	 */
	public int setLowerBounds(int nrxns, double[] lb)
	{
		if (numMetabs == 0 || numRxns == 0)
			return MODEL_NOT_INITIALIZED;
		if (lb.length != nrxns)
			return PARAMS_ERROR;
		for (int i = 0; i < numRxns; i++)
		{
			double u = GLPK.glp_get_col_ub(lp, i + 1);
			int type = GLPKConstants.GLP_DB;
			if (lb[i] == u)
				type = GLPKConstants.GLP_FX;
			
			GLPK.glp_set_col_bnds(lp, i + 1, type, lb[i], u);
			GLPK.glp_set_col_bnds(lpMSA, i + 1, type, lb[i], u);
			
		}
		return PARAMS_OK;
	}

	/**
	 * Sets the current lower bounds (e.g. -uptake rates) for the exchange reactions
	 * in the model
	 * @param lb
	 * @return PARAMS_OK if the lb array is of the appropriate length; 
	 * PARAMS_ERROR if not,
	 * and MODEL_NOT_INITIALIZED if there's no matrix loaded
	 */
	public int setExchLowerBounds(int[] exch, double[] lb)
	{
		//if (numMetabs == 0 || numRxns == 0)
		//	return MODEL_NOT_INITIALIZED;
		//if (lb.length != numExch)
		//	return PARAMS_ERROR;
		for (int i=0; i<exch.length; i++)
		{
			double u = GLPK.glp_get_col_ub(lp, exch[i]);
			int type = GLPKConstants.GLP_DB;
			if (lb[i] == u)
				type = GLPKConstants.GLP_FX;
			GLPK.glp_set_col_bnds(lp, exch[i], type, lb[i], u);
			GLPK.glp_set_col_bnds(lpMSA, exch[i], type, lb[i], u);
		}
		return PARAMS_OK;
	}

	/**
	 * Sets the current upper bounds (e.g. excretion rates) for the exchange reactions
	 * in the model
	 * @param ub
	 * @return PARAMS_OK if the ub array is of the appropriate length; 
	 * PARAMS_ERROR if not,
	 * and MODEL_NOT_INITIALIZED if there's no matrix loaded
	 */
	public int setExchUpperBounds(int[] exch, double[] ub)
	{
		if (numMetabs == 0 || numRxns == 0)
			return MODEL_NOT_INITIALIZED;
		if (ub.length != numExch)
			return PARAMS_ERROR;
		for (int i=0; i<numExch; i++)
		{
			double l = GLPK.glp_get_col_ub(lp, exch[i]);
			int type = GLPKConstants.GLP_DB;
			if (ub[i] == l)
				type = GLPKConstants.GLP_FX;
			GLPK.glp_set_col_bnds(lp, exch[i], type, l, ub[i]);
			GLPK.glp_set_col_bnds(lpMSA, exch[i], type, l, ub[i]);
		}
		return PARAMS_OK;
	}

	/**
	 * @return the array of lower bounds for all fluxes.
	 */
	public double[] getLowerBounds(int nrxns)
	{
		double[] l = new double[nrxns];
		for (int i = 0; i < nrxns; i++)
		{
			l[i] = GLPK.glp_get_col_lb(lp, i + 1);
		}
		return l;
	}

	/**
	 * Sets the current upper bounds for the FBA problem
	 * @param ub upper bounds array
	 * @return PARAMS_OK if the ub array is of the appropriate length; 
	 * PARAMS_ERROR if not,
	 * and MODEL_NOT_INITIALIZED if there's no matrix loaded
	 */
	public int setUpperBounds(int nrxns, double[] ub)
	{
		if (numMetabs == 0 || numRxns == 0)
		{
			return MODEL_NOT_INITIALIZED;
		}
		for (int i = 0; i < nrxns; i++)
		{
			double l = GLPK.glp_get_col_lb(lp, i + 1);
			int type = GLPKConstants.GLP_DB;
			if (l == ub[i])
				type = GLPKConstants.GLP_FX;
			GLPK.glp_set_col_bnds(lp, i + 1, type, l, ub[i]);
			GLPK.glp_set_col_bnds(lpMSA, i + 1, type, l, ub[i]);
		}
		return PARAMS_OK;
	}

	/**
	 * @return the current upper bounds for all fluxes
	 */
	public double[] getUpperBounds(int nrxns)
	{
		double[] u = new double[nrxns];
		for (int i = 0; i < nrxns; i++)
		{
			u[i] = GLPK.glp_get_col_ub(lp, i + 1);
		}
		return u;
	}

	/**
	 * Sets the style of the objective for the linear programming solver. The most
	 * common ones used are probably MAXIMIZE_OBJECTIVE_FLUX or MAX_OBJECTIVE_MIN_TOTAL, 
	 * where the second will maximize the objective flux, then find the flux solution that will
	 * minimize the sum of absolute values of fluxes that will yield the objective solution.
	 * <p>
	 * Many of these are included for the sake of completion, but may not be especially
	 * appropriate for doing FBA, except under very specific circumstances
	 * @param obj = one of
	 * <ul>
	 * <li>MAXIMIZE_OBJECTIVE_FLUX
	 * <li>MINIMIZE_OBJECTIVE_FLUX
	 * <li>MAXIMIZE_TOTAL_FLUX
	 * <li>MINIMIZE_TOTAL_FLUX
	 * <li>MAX_OBJECTIVE_MIN_TOTAL
	 * <li>MAX_OBJECTIVE_MAX_TOTAL
	 * <li>MIN_OBJECTIVE_MIN_TOTAL
	 * <li>MIN_OBJECTIVE_MAX_TOTAL
	 * </ul>
	 * @return PARAMS_OK if obj is an appropriate value, PARAMS_ERROR otherwise
	 */
	public int setObjectiveStyle(int obj)
	{
		if (obj != FBAModel.MAXIMIZE_OBJECTIVE_FLUX && 
			obj != FBAModel.MINIMIZE_OBJECTIVE_FLUX &&
			obj != FBAModel.MAXIMIZE_TOTAL_FLUX &&
			obj != FBAModel.MINIMIZE_TOTAL_FLUX &&
			obj != FBAModel.MAX_OBJECTIVE_MIN_TOTAL &&
			obj != FBAModel.MAX_OBJECTIVE_MAX_TOTAL &&
			obj != FBAModel.MIN_OBJECTIVE_MIN_TOTAL &&
			obj != FBAModel.MIN_OBJECTIVE_MAX_TOTAL)
			return PARAMS_ERROR;
		objStyle = obj;

		// if we're NOT optimizing the total raw fluxes
		// (not abs vals), then we need to reset the 
		// Cvector. Otherwise, that's done as part of the
		// optimization.
		if (objStyle != FBAModel.MAXIMIZE_TOTAL_FLUX &&
			objStyle != FBAModel.MINIMIZE_TOTAL_FLUX)
			setObjectiveReaction(numRxns, objReaction);

		return PARAMS_OK;
	}
	
	public int getObjectiveStyle() 
	{ 
		return objStyle; 
	}
	
	public int getObjectiveIndex()
	{
		return objReaction;
	}
	
	/**
	 * Sets the objective reaction for this FBAModel. Maximizing growth is probably
	 * the most common, but any reaction can be used as the objective.
	 * @param r the index of the objective reaction (1->N)
	 * @return PARAMS_OK if successful, PARAMS_ERROR if r < 1 or r > numReactions, and
	 * MODEL_NOT_INITIALIZED if there's nothing ready
	 */
	public int setObjectiveReaction(int nrxns, int r)
	{
		if (r < 1 || r > nrxns)
		{
			return PARAMS_ERROR;
		}
		if (numMetabs == 0 || nrxns == 0)
		{
			return MODEL_NOT_INITIALIZED;
		}
		
		// reaction r is the objective, so set only r to be 1.
		for (int i = 1; i <= nrxns; i++)
		{
			GLPK.glp_set_obj_coef(lp, i, 0);
		}
		GLPK.glp_set_obj_coef(lp, r, 1);
		GLPK.glp_set_obj_name(lp, "obj");
		objReaction = r;
		return PARAMS_OK;
	}

	/**
	 * Deletes the model from memory. Necessary to clear up the memory used by the GLPK library.
	 */
	public void delete()
	{
		GLPK.glp_delete_prob(lp);
		GLPK.glp_delete_prob(lpMSA);
	}

	/**
	 * Internally initializes and resets the parameters used by GLPK. If you want to 
	 * set specific GLPK parameters, you'll need to call a specialized function.
	 * //TODO make this possible...
	 */
	public void setParameters()
	{
		simParam = new glp_smcp();
		GLPK.glp_init_smcp(simParam);
		intParam = new glp_iptcp();
		GLPK.glp_init_iptcp(intParam);
		
		GLPK.glp_init_smcp(simParam);
		GLPK.glp_init_iptcp(intParam);
		
		// assume lpsolver == 1, and the rest = default, for now.

		// remap of control parameters for simplex method
		simParam.setMsg_lev(GLPKConstants.GLP_MSG_OFF);
		intParam.setMsg_lev(GLPKConstants.GLP_MSG_OFF);
		
		// simplex method: primal/dual
		switch (GLPIntParam[2])
		{
			case 0:
				simParam.setMeth(GLPKConstants.GLP_PRIMAL);
				break;
			case 1:
				simParam.setMeth(GLPKConstants.GLP_DUAL);
				break;
			case 2:
				simParam.setMeth(GLPKConstants.GLP_DUALP);
				break;
			default:
				break;
		}
		// pricing technique
		if (GLPIntParam[3] == 0)
			simParam.setPricing(GLPKConstants.GLP_PT_STD);
		else
			simParam.setPricing(GLPKConstants.GLP_PT_PSE);
		// ratio test

		if (GLPIntParam[20] == 0)
			simParam.setR_test(GLPKConstants.GLP_RT_STD);
		else
			simParam.setR_test(GLPKConstants.GLP_RT_HAR);

		// tolerances
		simParam.setTol_bnd(GLPRealParam[1]); // primal feasible tolerance
		simParam.setTol_dj(GLPRealParam[2]); // dual feasible tolerance
		simParam.setTol_piv(GLPRealParam[3]); // pivot tolerance
		simParam.setObj_ll(GLPRealParam[4]); // lower limit
		simParam.setObj_ul(GLPRealParam[5]); // upper limit

		// iteration limit
		if (GLPIntParam[5] == -1)
			simParam.setIt_lim(Integer.MAX_VALUE);
		else
			simParam.setIt_lim(GLPIntParam[5]);

		// time limit
		if (GLPRealParam[6] == -1)
			simParam.setTm_lim(Integer.MAX_VALUE);
		else
			simParam.setTm_lim((int) GLPRealParam[6]);
		simParam.setOut_frq(GLPIntParam[7]); // output frequency
		simParam.setOut_dly((int) GLPRealParam[7]); // output delay

		// presolver
		if (GLPIntParam[16] != 0)
			simParam.setPresolve(GLPK.GLP_ON);
		else
			simParam.setPresolve(GLPK.GLP_OFF);
	}


	/**
	 * Performs an FBA run with the loaded model, constraints, and bounds. Fluxes
	 * and solutions are stored in the FBAModel class and can be used through their
	 * accessory functions
	 * @return the GLPK status code, or MODEL_NOT_INITIALIZED if there's no problem to solve
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
		objStyle=objSty;
        //System.out.println("GLPK");
		// an internal status checker. If this = 0 after a run, everything is peachy.
		int ret = -1;
		switch(objSty)
		{
			// the usual, just max the objective
			case FBAModel.MAXIMIZE_OBJECTIVE_FLUX:
				GLPK.glp_set_obj_dir(lp, GLPK.GLP_MAX); // this is "vanilla" FBA,
				switch(glpkSolverMethod)
				{
					case SIMPLEX_METHOD:
						ret = GLPK.glp_simplex(lp, simParam);
						break;
					default:
						ret = GLPK.glp_interior(lp, intParam);
						break;
				}
				break;
				
			// tell GLPK to minimize the objective
			case FBAModel.MINIMIZE_OBJECTIVE_FLUX:
				GLPK.glp_set_obj_dir(lp, GLPK.GLP_MIN);
				switch(glpkSolverMethod)
				{
					case SIMPLEX_METHOD:
						ret = GLPK.glp_simplex(lp, simParam);
						break;
					default:
						ret = GLPK.glp_interior(lp, intParam);
						break;
				}
				break;

			// do something special here...
			case FBAModel.MAXIMIZE_TOTAL_FLUX:
				ret = runOptimizeSumFluxes();
				break;
			case FBAModel.MINIMIZE_TOTAL_FLUX:
				ret = runOptimizeSumFluxes();
				break;
			
			default:
				// do double cases!
				ret = runOptimizeSumAbsoluteValuesFluxes();
				break;
		}
		
		if (ret == 0)
		{
			runSuccess = true;
		}
		
		if (objSty == FBAModel.MAXIMIZE_OBJECTIVE_FLUX ||
			objSty == FBAModel.MINIMIZE_OBJECTIVE_FLUX ||
			objSty == FBAModel.MAXIMIZE_TOTAL_FLUX ||
			objSty == FBAModel.MINIMIZE_TOTAL_FLUX)
			return GLPK.glp_get_status(lp);
		else
			return GLPK.glp_get_status(lpMSA);
	}

	/**
	 * Optimizes the objective value, while either maximizing or minizing the sum of fluxes.
	 * Note that this is the raw sum, NOT the absolute values of fluxes.
	 * @return the return status from GLPK
	 */
	private synchronized int runOptimizeSumFluxes()
	{
		// First run - deal with objective.
		// So, FIRST, make sure that we're only optimizing the 
		// objective reaction. Cheater function below.
		setObjectiveReaction(numRxns, objReaction);
		GLPK.glp_set_obj_dir(lp, GLPK.GLP_MAX);
		int ret = -1;
		switch (glpkSolverMethod)
		{
			case SIMPLEX_METHOD:
				ret = GLPK.glp_simplex(lp, simParam);
				break;
			default:
				ret = GLPK.glp_interior(lp, intParam);
				break;
		}
		if (ret != 0)
			return ret;
		
		// Next, set the upper and lower bounds of the flux solution
		// to be the value we want.
		double sol = GLPK.glp_get_obj_val(lp);
		double lb = GLPK.glp_get_col_lb(lp, objReaction);
		double ub = GLPK.glp_get_col_ub(lp, objReaction);
		GLPK.glp_set_col_bnds(lp, objReaction, GLPK.GLP_FX, sol, sol);
		
		// now, set the Cvector to be all ones.
		for (int i = 1; i <= numRxns; i++)
		{
			GLPK.glp_set_obj_coef(lp, i, 1);
		}

		// toggle if we're maximizing or minimizing the total
		if (objStyle == FBAModel.MAXIMIZE_TOTAL_FLUX)
			GLPK.glp_set_obj_dir(lp, GLPK.GLP_MAX);
		else
			GLPK.glp_set_obj_dir(lp, GLPK.GLP_MIN);

		switch(glpkSolverMethod)
		{
			case SIMPLEX_METHOD:
				ret = GLPK.glp_simplex(lp, simParam);
				break;
			default:
				ret = GLPK.glp_interior(lp, intParam);
				break;
		}

		// reset the bounds to what they were. resetting the Cvector is
		// done elsewhere.
		int type = GLPK.GLP_DB;
		if (lb == ub)
			type = GLPK.GLP_FX;
		GLPK.glp_set_col_bnds(lp, objReaction, type, lb, ub);
		
		return ret;
	}
	
	/**
	 * Optimizes the objective while also optimizing the sum of absolute values of fluxes.
	 * @return the GLPK status of the solver
	 */
	private synchronized int runOptimizeSumAbsoluteValuesFluxes()
	{
		// First run - deal with objective flux.		
		int ret = -1;
		if (objStyle == FBAModel.MIN_OBJECTIVE_MAX_TOTAL ||
			objStyle == FBAModel.MIN_OBJECTIVE_MIN_TOTAL)
			GLPK.glp_set_obj_dir(lp, GLPK.GLP_MIN);
		else
			GLPK.glp_set_obj_dir(lp, GLPK.GLP_MAX);

		if (objStyle == FBAModel.MAX_OBJECTIVE_MIN_TOTAL ||
			objStyle == FBAModel.MIN_OBJECTIVE_MIN_TOTAL)
			GLPK.glp_set_obj_dir(lpMSA, GLPK.GLP_MIN);
		else
			GLPK.glp_set_obj_dir(lpMSA, GLPK.GLP_MAX);
		
		// setup done.
		
		int status = -1; //GLPK.glp_simplex(lp, param);
		simParam.setPresolve(GLPK.GLP_OFF);
		switch(glpkSolverMethod)
		{
			case SIMPLEX_METHOD:
				status = GLPK.glp_simplex(lp, simParam);
				break;
			default:
				status = GLPK.glp_interior(lp, intParam);
				break;
		}

		if (status != 0)
			return status;
		
		// now, fetch the objective solution from lp, and fix it as the
		// upper and lower bounds to lpMSA
		double sol = GLPK.glp_get_obj_val(lp);
		GLPK.glp_set_col_bnds(lpMSA, objReaction, GLPK.GLP_FX, sol, sol);

//		ret = GLPK.glp_simplex(lpMSA, param);
		switch(glpkSolverMethod)
		{
			case SIMPLEX_METHOD:
				ret = GLPK.glp_simplex(lpMSA, simParam);
				break;
			default:
				ret = GLPK.glp_interior(lpMSA, intParam);
				break;
		}
		return ret;
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
				if (objStyle == FBAModel.MAXIMIZE_OBJECTIVE_FLUX ||
					objStyle == FBAModel.MINIMIZE_OBJECTIVE_FLUX ||
					objStyle == FBAModel.MAXIMIZE_TOTAL_FLUX ||
					objStyle == FBAModel.MINIMIZE_TOTAL_FLUX)
					v[i] = GLPK.glp_get_col_prim(lp, i + 1);
				else
					v[i] = GLPK.glp_get_col_prim(lpMSA, i + 1);
			}
		}
		return v;
	}
	
	/**
	 * @return the exchange fluxes from the most recent FBA run
	 */
	public double[] getExchangeFluxes(int[] exch)
	{
		double[] v = new double[exch.length];
		if (runSuccess)
		{
			for (int i = 0; i < exch.length; i++)
			{
				if (objStyle == FBAModel.MAXIMIZE_OBJECTIVE_FLUX ||
					objStyle == FBAModel.MINIMIZE_OBJECTIVE_FLUX ||
					objStyle == FBAModel.MAXIMIZE_TOTAL_FLUX ||
					objStyle == FBAModel.MINIMIZE_TOTAL_FLUX)
					v[i] = GLPK.glp_get_col_prim(lp, exch[i]);
				else
					v[i] = GLPK.glp_get_col_prim(lpMSA, exch[i]);
			}
		}
		return v;
	}

	/**
	 * Sets the media conditions for an FBA in terms of lower bounds of fluxes (e.g. the
	 * Palsson standard style) 
	 * @param media
	 */
	public void setMediaConditions(double[] media, int[] exch)
	{
		if (media.length != numExch)
			return;
		// pretty much assume that media is parallel to exch, and that values
		// are negative
		for (int i = 0; i < media.length; i++)
		{
			GLPK.glp_set_col_bnds(lp, exch[i], GLPKConstants.GLP_DB, media[i],
								  GLPK.glp_get_col_ub(lp, exch[i]));
		}
	}

	/**
	 * If the FBA run was successful (as denoted by the GLPK status code), this returns
	 * the value of the objective solution. Otherwise, it returns -Double.MAX_VALUE
	 * @return either the objective solution of -Double.MAX_VALUE
	 */
	public double getObjectiveSolution(int r)
	{
		if (runSuccess)
			if (objStyle == FBAModel.MAXIMIZE_OBJECTIVE_FLUX ||
				objStyle == FBAModel.MINIMIZE_OBJECTIVE_FLUX)
				return GLPK.glp_get_obj_val(lp);
			else
				return GLPK.glp_get_obj_val(lpMSA);
		return -Double.MAX_VALUE;
	}
	
	/**
	 * If the FBA run was successful (as denoted by the GLPK status code), this returns
	 * the value of the flux of the objective reaction. Otherwise, it returns 
	 * -Double.MAX_VALUE. Note that this is a specific flux, and not necessarily the
	 * objective solution, which may be a linear combination of many fluxes
	 * @return
	 */
	public double getObjectiveFluxSolution(int objreact)
	{
        //System.out.println("runSuccess "+runSuccess);		
		if (runSuccess)
			return GLPK.glp_get_col_prim(lp, objreact);
//			return GLPK.glp_get_obj_val(lp);
		return -Double.MAX_VALUE;
	}


	/**
	 * Sets the upper bound on the objective reaction. 
	 * @param ub
	 * @return PARAMS_ERROR if ub < the current lb for the objective, PARAMS_OK otherwise
	 */
	public int setObjectiveUpperBound(int objreact, double ub)
	{
		double lb = GLPK.glp_get_col_lb(lp, objReaction);
		if (ub < lb)
			return PARAMS_ERROR;
		int type = GLPKConstants.GLP_DB;
		if (lb == ub)
			type = GLPKConstants.GLP_FX;

		GLPK.glp_set_col_bnds(lp, objreact, type, lb, ub);
		
		return PARAMS_OK;
	}
	
	/**
	 * Sets the lower bound on the objective reaction. 
	 * @param ub
	 * @return PARAMS_ERROR if ub < the current lb for the objective, PARAMS_OK otherwise
	 */
	public int setObjectiveLowerBound(int objreact, double lb)
	{
		/*
		double ub = GLPK.glp_get_col_ub(lp, objReaction);
		if (ub < lb)
			return PARAMS_ERROR;
		int type = GLPKConstants.GLP_DB;
		if (lb == ub)
			type = GLPKConstants.GLP_FX;

		GLPK.glp_set_col_bnds(up, objreact, type, lb, ub);
		*/
		return PARAMS_OK;
	}
	

	
	protected FBAOptimizerGLPK(glp_prob lp, glp_prob lpMSA, int numMetabs, int numRxns, int numExch)
	{
		this.lp = lp;
		this.lpMSA = lpMSA;
		this.numMetabs = numMetabs;
		this.numRxns = numRxns;
		this.numExch = numExch;
	}
	
	/**
	 * Produces a clone of this <code>FBAModel</code> with all parameters intact.
	 */
	public FBAOptimizerGLPK clone()
	{
		glp_prob lpNew = GLPK.glp_create_prob();
		GLPK.glp_copy_prob(lpNew, lp, GLPK.GLP_ON);

		glp_prob lpMSANew = GLPK.glp_create_prob();
		GLPK.glp_copy_prob(lpMSANew, lpMSA, GLPK.GLP_ON);

		FBAOptimizerGLPK optimizerCopy = new FBAOptimizerGLPK(lpNew, lpMSANew, numMetabs, numRxns, numExch);
		optimizerCopy.setParameters();
		
		return optimizerCopy;
	}



	
	/**
	 * Prints either the lp matrix or the lpMSA matrix, depending on if useMSA is
	 * false or true
	 * @param useMSA if true, print the MSA matrix, otherwise the plain lp matrix
	 */
	/*
	private void printMatrix(boolean useMSA)
	{
		int numRows = numMetabs;
		int numCols = numRxns;
		if (useMSA)
		{
			numRows += numRxns + numRxns;
			numCols += numRxns;
		}
		
		for (int i=0; i<numRows; i++)
		{
			SWIGTYPE_p_int ia;
			SWIGTYPE_p_double val;
			ia = GLPK.new_intArray(numCols);
			val = GLPK.new_doubleArray(numRows);
			int len = 0;
			if (!useMSA)
				len = GLPK.glp_get_mat_row(lp, i+1, ia, val);
			else
				len = GLPK.glp_get_mat_row(lpMSA, i+1, ia, val);
			double[] row = new double[numCols];
//			System.out.println((i+1) + ":");
			for (int j=0; j<len; j++)
			{
//				System.out.println("\t" + GLPK.intArray_getitem(ia, j+1) + "\t" + GLPK.doubleArray_getitem(val, j+1));
				row[GLPK.intArray_getitem(ia, j+1)-1] = GLPK.doubleArray_getitem(val, j+1);
			}
//			System.out.print((i+1) + ": ");
			for (int j=0; j<row.length; j++)
			{
				System.out.print(row[j] + "\t");
			}
			System.out.print("\n");
		}
	}
	
    */
	

	


}