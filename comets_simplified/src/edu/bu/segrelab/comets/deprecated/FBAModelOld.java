package edu.bu.segrelab.comets.deprecated;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Random;

import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import org.gnu.glpk.GLPK;
import org.gnu.glpk.GLPKConstants;
import org.gnu.glpk.SWIGTYPE_p_double;
import org.gnu.glpk.SWIGTYPE_p_int;
import org.gnu.glpk.glp_iptcp;
import org.gnu.glpk.glp_prob;
import org.gnu.glpk.glp_smcp;


import edu.bu.segrelab.comets.exception.ModelFileException;
import edu.bu.segrelab.comets.ui.DoubleField;

/**
 * This class defines the functions necessary to load, process, and execute a flux balance
 * analysis model. It can load a model from different file formats (//TODO add SBML), and
 * run the model under various objectives and styles.
 * <p>
 * The best way to use this class is as follows:
 * <ol>
 * <li>The user loads a model either as FBAModel.loadModelFromFile() or by using a constructor directly.
 * <li>The user can then set the upper and lower bounds of each flux directly.
 * <li>Run the model with model.run()
 * <li>Fetch the results with various accessory methods: getFluxes(), getExchangeFluxes(),
 *    getStatus(), getObjectiveSolution()
 * </ol>
 * <p>
 * This also implements a couple of GUI widgets - a ModelInfoPanel() and ModelParametersPanel(),
 * both of which are required for use in COMETS.
 * <p>
 * Note that this class depends on the availability of the GLPK-java package, which must be
 * accessible either through the classpath or the library path. Be sure to add glpk-java.jar
 * to the classpath and, if on a POSIX-based system (Mac or Linux), add the path to the 
 * installed glpk and jni libraries in -Djava.library.path
 * 
 * @author Bill Riehl briehl@bu.edu
 * created 3 Mar 2010
 */
public class FBAModelOld extends edu.bu.segrelab.comets.Model 
					  implements edu.bu.segrelab.comets.CometsConstants
{
	/*
	 * The best way for this class to be used would be in a couple of steps.
	 * 
	 * >FBA model = new FBA(S-matrix, LB, UB, objective_rxn); 
	 * >model.run();
	 * >model.setBounds(LB, UB); //or... >model.setLowerBounds(LB);
	 * >model.setUpperBounds(UB); >model.setObjectiveReaction(n);
	 * 
	 * In essence, for running multiple FBAs one after the other, it should keep
	 * the S-matrix/GLPK model in memory, and only modify it as necessary. I'm
	 * not sure, but I think Matlab's GLPK doesn't do that. Then again, I don't
	 * do lots of consecutive runs with minor changes in Matlab...
	 */

	// Minimize z = (x1-x2) /2 + (1-(x1-x2)) = -.5 * x1 + .5 * x2 + 1
	//
	// subject to
	// 0.0<= x1 - x2 <= 0.2
	// where,
	// 0.0 <= x1 <= 0.5
	// 0.0 <= x2 <= 0.5

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
	
	private int[] exch; // indices of exchange fluxes.
	// As in the GLPK model idiom,
	// these go from 1 -> n, not 0 -> n-1
	private double[] exchDiffConsts; // diffusion constants of extracellular metabolites
	private String[] exchRxnNames;
	private String[] exchMetabNames;
	private String[] rxnNames;
	private String[] metabNames;
	private double[] baseLB; // the base lower bounds that were originally
							 // loaded.
	private double[] baseUB; // base upper bounds (as a backup of sorts).
	private double[] baseExchLB;
	private double[] baseExchUB;
	private double[] exchKm;		  // each of these three arrays is applied to the exchange
	private double[] exchVmax;		  // reactions (for now). Eventually, this might move to the
	private double[] exchHillCoeff;   // transporters.
	
	private double[] exchAlpha;	  // another option for creating exchange reactions:
	private double[] exchW; 		  // defined as min(alpha[i] * media[i], W[i] * volume) / biomass
									  // not as "exact" as the kinetic constraints, but still time-independent

	private double flowDiffConst; // = 1e-5;
	private double growthDiffConst; // = 5e-5;
	private int objReaction;
	private int objStyle;
	
	private double defaultLB = 0,
				   defaultUB = 0,
				   defaultKm = 0,
				   defaultVmax = 0,
				   defaultHill = 0,
				   defaultAlpha = 0,
				   defaultW = 0,
				   defaultMetabDiffConst = 0;
//	private boolean allReactions = false;
	
	private boolean active;     // true is model is active growing, if false the model is asleep 
		
	private static int[] GLPIntParam = new int[] { 0, 
			1, 0, 1, 0,
			Integer.MAX_VALUE, Integer.MAX_VALUE, 200, 1, 2, 0, 1, 0, 0, 3, 2,
			1, 0, 2, 0, 1 };

//	private static int[] INT_PARAM = new int[] { GLPKConstants.LPX_K_MSGLEV,
//			GLPKConstants.LPX_K_SCALE, GLPKConstants.LPX_K_DUAL,
//			GLPKConstants.LPX_K_PRICE, GLPKConstants.LPX_K_ROUND,
//			GLPKConstants.LPX_K_ITLIM, GLPKConstants.LPX_K_ITCNT,
//			GLPKConstants.LPX_K_OUTFRQ, GLPKConstants.LPX_K_MPSINFO,
//			GLPKConstants.LPX_K_MPSOBJ, GLPKConstants.LPX_K_MPSORIG,
//			GLPKConstants.LPX_K_MPSWIDE, GLPKConstants.LPX_K_MPSFREE,
//			GLPKConstants.LPX_K_MPSSKIP, GLPKConstants.LPX_K_BRANCH,
//			GLPKConstants.LPX_K_BTRACK, GLPKConstants.LPX_K_PRESOL,
//			GLPKConstants.LPX_K_USECUTS, GLPKConstants.LPX_K_BINARIZE };

	private static double[] GLPRealParam = new double[] { 0.07, 
			1e-7, 1e-7,
			1e-10, -Double.MAX_VALUE, 
			Double.MAX_VALUE, Integer.MAX_VALUE, 
			0.0, 1e-5, 
			1e-7, 0.0 };

//	private static int[] REAL_PARAM = new int[] { GLPKConstants.LPX_K_RELAX,
//			GLPKConstants.LPX_K_TOLBND, GLPKConstants.LPX_K_TOLDJ,
//			GLPKConstants.LPX_K_TOLPIV, GLPKConstants.LPX_K_OBJLL,
//			GLPKConstants.LPX_K_OBJUL, GLPKConstants.LPX_K_TMLIM,
//			GLPKConstants.LPX_K_OUTDLY, GLPKConstants.LPX_K_TOLINT,
//			GLPKConstants.LPX_K_TOLOBJ, GLPKConstants.LPX_K_MIPGAP };

	public static final int MAXIMIZE_OBJECTIVE_FLUX = 0;
	public static final int MINIMIZE_OBJECTIVE_FLUX = 1;
	public static final int MAXIMIZE_TOTAL_FLUX = 2;
	public static final int MINIMIZE_TOTAL_FLUX = 3;
	public static final int MAX_OBJECTIVE_MIN_TOTAL = 4;
	public static final int MAX_OBJECTIVE_MAX_TOTAL = 5; 
	public static final int MIN_OBJECTIVE_MIN_TOTAL = 6;
	public static final int MIN_OBJECTIVE_MAX_TOTAL = 7;
	
	private ModelParametersPanel paramsPanel;

	/**
	 * Create a new simple FBAModel without any stoichiometry information loaded.
	 */
	private FBAModelOld()
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
		objStyle = MAXIMIZE_OBJECTIVE_FLUX;		 // so maximize by default
		
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
	public FBAModelOld(double[][] m, double[] l, double[] u, int r)
	{
		this();

		setStoichiometricMatrix(m);
		setBounds(l, u);
		setBaseBounds(l, u);
		setObjectiveReaction(r);
	}

	/**
	 * Create a new FBAModel with all the trimmings.
	 * @param m Stoichiometric matrix
	 * @param l lower bounds
	 * @param u upper bounds
	 * @param r objective reaction (remember 1->N!)
	 * @param exch indices of exchange reactions
	 * @param exchDiffConsts diffusion constants of all extracellular metabolites
	 * @param exchKm Michaelis constant of each exchange reaction
	 * @param exchVmax Vmax for each exchange reaction (Michaelis-Menten style)
	 * @param exchHillCoeff Hill coefficient for each exchange reaction (Monod style)
	 * @param metabNames array of metabolite names
	 * @param rxnNames array of reaction names
	 */
	public FBAModelOld(final double[][] m, 
					final double[] l, 
					final double[] u, 
					int r, 
					final int[] exch, 
					final double[] exchDiffConsts,
					final double[] exchKm, 
					final double[] exchVmax, 
					final double[] exchHillCoeff,
					final double[] exchAlpha,
					final double[] exchW,
					final String[] metabNames, 
					final String[] rxnNames,
					final int objStyle)
	{
		this(m, l, u, r);
		
		if (exch == null)
			throw new IllegalArgumentException("There must be an array of exchange reactions.");
		else
			this.exch = exch.clone(); // these represent reaction indices from 1-->N, NOT 0-->N-1 !! (necessary for GLPK to play nice.)

		this.exchDiffConsts = exchDiffConsts;
		
		this.exchKm = exchKm;
		this.exchVmax = exchVmax;
		this.exchHillCoeff = exchHillCoeff;
		this.exchAlpha = exchAlpha;
		this.exchW = exchW;
		this.objStyle = objStyle;

		this.metabNames = metabNames;
		this.rxnNames = rxnNames;
		this.numExch = exch.length;
		exchRxnNames = new String[numExch];
		exchMetabNames = new String[numExch];
		
		for (int i=0; i<numExch; i++)
		{
			exchRxnNames[i] = rxnNames[exch[i]-1];

			// to find the metabolite index (and thus its name) we need to
			// use the exchange reaction index to hunt down the right row, then
			// find the nonzero element.
			
			// assume the first one we come across is the answer.
			// because that's how it freaking should be.
			for (int j=0; j<numMetabs; j++)
			{
				if (m[j][exch[i]-1] != 0)
				{
					exchMetabNames[i] = metabNames[j];
					break;
				}
			}
		}
			
		baseExchLB = new double[numExch];
		baseExchUB = new double[numExch];
		for (int i = 0; i < numExch; i++)
		{
			baseExchLB[i] = baseLB[exch[i]-1];
			baseExchUB[i] = baseUB[exch[i]-1];
		}
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
	
	public double getDefaultLB()
	{
		return defaultLB;
	}
	
	public double getDefaultUB()
	{
		return defaultUB;
	}
	
	public double getDefaultKm()
	{
		return defaultKm;
	}
	
	public double getDefaultVmax()
	{
		return defaultVmax;
	}
	
	public double getDefaultHill()
	{
		return defaultHill;
	}
	
	public double getDefaultAlpha()
	{
		return defaultAlpha;
	}
	
	public double getDefaultW()
	{
		return defaultW;
	}
	
	public double getDefaultMetabDiffConst()
	{
		return defaultMetabDiffConst;
	}
	
	public void setDefaultLB(double defLB)
	{
		this.defaultLB = defLB;
	}
	
	public void setDefaultUB(double defUB)
	{
		this.defaultUB = defUB;
	}
	
	public void setDefaultKm(double defKm)
	{
		for (int i=0; i<exchKm.length; i++)
		{
			if (exchKm[i] == defaultKm)
				exchKm[i] = defKm;
		}
		this.defaultKm = defKm;
	}
	
	public void setDefaultVmax(double defVmax)
	{
		for (int i=0; i<exchVmax.length; i++)
		{
			if (exchVmax[i] == defaultVmax)
				exchVmax[i] = defVmax;
		}
		this.defaultVmax = defVmax;
	}
	
	public void setDefaultHill(double defHill)
	{
		for (int i=0; i<exchHillCoeff.length; i++)
		{
			if (exchHillCoeff[i] == defaultHill)
				exchHillCoeff[i] = defHill;
		}
		this.defaultHill = defHill;
	}
	
	public void setDefaultAlpha(double defAlpha)
	{
		for (int i=0; i<exchAlpha.length; i++)
		{
			if (exchAlpha[i] == defaultAlpha)
				exchAlpha[i] = defAlpha;
		}
		this.defaultAlpha = defAlpha;
	}
	
	public void setDefaultW(double defW)
	{
		for (int i=0; i<exchW.length; i++)
		{
			if (exchW[i] == defaultW)
				exchW[i] = defW;
		}
		this.defaultW = defW;
	}
	
	public void setDefaultMetabDiffConst(double defDiff)
	{
		for (int i=0; i<exchDiffConsts.length; i++)
		{
			if (exchDiffConsts[i] == defaultMetabDiffConst)
				exchDiffConsts[i] = defDiff;
		}
		this.defaultMetabDiffConst = defDiff;
	}
	
	/**
	 * @return the Michaelis constants for each exchange reaction
	 */
	public double[] getExchangeKm()
	{
		return exchKm;
	}
	
	public void setExchangeKm(final double[] exchKm)
	{
		if (this.numExch == exchKm.length)
			this.exchKm = exchKm;
	}
	
	/**
	 * @return the Michaelis-Mented Vmax values for each exchange reaction
	 */
	public double[] getExchangeVmax()
	{
		return exchVmax;
	}
	
	public void setExchangeVmax(final double[] exchVmax)
	{
		if (this.numExch == exchVmax.length)
			this.exchVmax = exchVmax;
	}

	
	/**
	 * @return the Monod-modeling Hill coefficients for each exchange reaction
	 */
	public double[] getExchangeHillCoefficients()
	{
		return exchHillCoeff;
	}

	public void setExchangeHillCoefficients(final double[] exchHillCoeff)
	{
		if (this.numExch == exchHillCoeff.length)
			this.exchHillCoeff = exchHillCoeff;
	}

	
	public double[] getExchangeAlphaCoefficients()
	{
		return exchAlpha;
	}

	public void setExchangeAlphaCoefficients(final double[] exchAlphaCoeff)
	{
		if (this.numExch == exchAlphaCoeff.length)
			this.exchAlpha = exchAlphaCoeff;
	}
	
	public double[] getExchangeWCoefficients()
	{
		return exchW;
	}
	
	public void setExchangeWCoefficients(final double[] exchW)
	{
		if (this.numExch == exchW.length)
			this.exchW = exchW;
	}

	/**
	 * @return the set of diffusion constants (in cm^2/s) for each extracellular metabolite
	 */
	public double[] getExchangeMetaboliteDiffusionConstants()
	{
		return exchDiffConsts.clone();
	}

	public void setExchangeMetaboliteDiffusionConstants(final double[] metabDiffConsts)
	{
		if (this.numExch == metabDiffConsts.length)
			this.exchDiffConsts = metabDiffConsts;
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
	 * Sets the base, permanent upper and lower bounds. These will persist across
	 * FBA runs.
	 * @param lb lower bounds
	 * @param ub upper bounds
	 * @return PARAMS_OK if all lower bounds are less than or equal to the upper bounds,
	 * and both arrays are of the appropriate length; PARAMS_ERROR if they're incorrect;
	 * and MODEL_NOT_INITIALIZED if there's no matrix loaded
	 */
	public int setBaseBounds(double[] lb, double[] ub)
	{
		if (lb.length != ub.length)
			return PARAMS_ERROR;
		for (int i=0; i<lb.length; i++)
		{
			if (lb[i] > ub[i])
				return PARAMS_ERROR;
		}
		int ret = setBaseLowerBounds(lb);
		ret = setBaseUpperBounds(ub);
		return ret;
	}

	/**
	 * Sets the base, persistent lower bounds
	 * @param lb lower bounds array
	 * @return PARAMS_OK if the lb array is of the appropriate length; 
	 * PARAMS_ERROR if not,
	 * and MODEL_NOT_INITIALIZED if there's no matrix loaded
	 */
	public int setBaseLowerBounds(double[] lb)
	{
		if (numMetabs == 0 || numRxns == 0)
		{
			return MODEL_NOT_INITIALIZED;
		}
		else if (lb.length != numRxns)
			return PARAMS_ERROR;
		baseLB = lb;
		return PARAMS_OK;
	}

	/**
	 * Sets the base, persistent upper bounds
	 * @param ub lower bounds array
	 * @return PARAMS_OK if the lb array is of the appropriate length; 
	 * PARAMS_ERROR if not,
	 * and MODEL_NOT_INITIALIZED if there's no matrix loaded
	 */
	public int setBaseUpperBounds(final double[] ub)
	{
		if (numMetabs == 0 || numRxns == 0)
		{
			return MODEL_NOT_INITIALIZED;
		}
		else if (numRxns != ub.length)
			return PARAMS_ERROR;
		baseUB = ub;
		return PARAMS_OK;
	}

	public int setBaseExchLowerBounds(final double[] lb)
	{
		if (numMetabs == 0 || numRxns == 0)
		{
			return MODEL_NOT_INITIALIZED;
		}
		else if (numExch != lb.length)
			return PARAMS_ERROR;
		baseExchLB = lb;
		return PARAMS_OK;
	}
	
	public int setBaseExchUpperBounds(final double[] ub)
	{
		if (numMetabs == 0 || numRxns == 0)
		{
			return MODEL_NOT_INITIALIZED;
		}
		else if (numExch != ub.length)
			return PARAMS_ERROR;
		baseExchUB = ub;
		return PARAMS_OK;
	}

	public double[] getBaseLowerBounds()
	{
		return baseLB.clone();
	}

	public double[] getBaseUpperBounds()
	{
		return baseUB.clone();
	}

	public double[] getBaseExchLowerBounds()
	{
		return baseExchLB.clone();
	}

	public double[] getBaseExchUpperBounds()
	{
		return baseExchUB.clone();
	}
	
	public String[] getReactionNames()
	{
		return rxnNames.clone();
	}
	
	public void setReactionNames(final String[] rxnNames)
	{
		if (this.numRxns == rxnNames.length)
			this.rxnNames = rxnNames;
	}
	
	public String[] getMetaboliteNames()
	{
		return metabNames.clone();
	}
	
	public void setMetaboliteNames(final String[] metabNames)
	{
		if (this.numMetabs == metabNames.length)
			this.metabNames = metabNames;
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
	public int setLowerBounds(double[] lb)
	{
		if (numMetabs == 0 || numRxns == 0)
			return MODEL_NOT_INITIALIZED;
		if (lb.length != numRxns)
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
	public int setExchLowerBounds(double[] lb)
	{
		if (numMetabs == 0 || numRxns == 0)
			return MODEL_NOT_INITIALIZED;
		if (lb.length != numExch)
			return PARAMS_ERROR;
		for (int i=0; i<numExch; i++)
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
	public int setExchUpperBounds(double[] ub)
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
	public double[] getLowerBounds()
	{
		double[] l = new double[numRxns];
		for (int i = 0; i < numRxns; i++)
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
	public int setUpperBounds(double[] ub)
	{
		if (numMetabs == 0 || numRxns == 0)
		{
			return MODEL_NOT_INITIALIZED;
		}
		for (int i = 0; i < numRxns; i++)
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
	public double[] getUpperBounds()
	{
		double[] u = new double[numRxns];
		for (int i = 0; i < numRxns; i++)
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
		if (obj != MAXIMIZE_OBJECTIVE_FLUX && 
			obj != MINIMIZE_OBJECTIVE_FLUX &&
			obj != MAXIMIZE_TOTAL_FLUX &&
			obj != MINIMIZE_TOTAL_FLUX &&
			obj != MAX_OBJECTIVE_MIN_TOTAL &&
			obj != MAX_OBJECTIVE_MAX_TOTAL &&
			obj != MIN_OBJECTIVE_MIN_TOTAL &&
			obj != MIN_OBJECTIVE_MAX_TOTAL)
			return PARAMS_ERROR;
		objStyle = obj;

		// if we're NOT optimizing the total raw fluxes
		// (not abs vals), then we need to reset the 
		// Cvector. Otherwise, that's done as part of the
		// optimization.
		if (objStyle != MAXIMIZE_TOTAL_FLUX &&
			objStyle != MINIMIZE_TOTAL_FLUX)
			setObjectiveReaction(objReaction);

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
	public int setObjectiveReaction(int r)
	{
		if (r < 1 || r > numRxns)
		{
			return PARAMS_ERROR;
		}
		if (numMetabs == 0 || numRxns == 0)
		{
			return MODEL_NOT_INITIALIZED;
		}
		
//		allReactions = false;
		// reaction r is the objective, so set only r to be 1.
		for (int i = 1; i <= numRxns; i++)
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
	 * Runs with the addtional constraint that:
	 * v1 >= alpha*v2
	 * (or, more precisely v1 - alpha*v2 >= 0)
	 * <p>
	 * This is useful, when, for example, executing an FBA model that excretes at least
	 * some amount of some metabolite.
	 * <p>
	 * The method works by adding a row to the model with the given constraint, 
	 * running the model (and storing all data, as usual), then disposing of the additional
	 * row.
	 * <p>
	 * v1 and v2 are expected to be indices (from 1-->N, not 0-->N-1) of reactions.
	 * @param v1 the reaction to be forced to have flux, as above
	 * @param v2 the reaction to be tied to v1, as above
	 * @param alpha the proportion of v2 that v1 must match
	 * @return the GLPK status of the solution (see the GLPK documentation...)
	 */
	public synchronized int runForceFlux(int x1, int x2, double alpha)
	{
		// Add a row to both lp and MSA
		int newLpRow = GLPK.glp_add_rows(lp, 1);
		int newLpMSARow = GLPK.glp_add_rows(lpMSA, 1);
		
		// Have to create the specialized SWIG style numerical types to pass to
		// the models. Fortunately, we can use them twice, since the positions 
		// being modified are the same.

		GLPK.intArray_setitem(forceIdx, 1, x1);
		GLPK.intArray_setitem(forceIdx, 2, x2);
		GLPK.doubleArray_setitem(forceVal, 1, 1);
		GLPK.doubleArray_setitem(forceVal, 2, -alpha);
		
		// Pass the new row values to the models.
		GLPK.glp_set_mat_row(lp, newLpRow, 2, forceIdx, forceVal);
//		GLPK.glp_set_mat_row(lpMSA, newLpMSARow, 2, forceIdx, forceVal);

		// Set the bounds on the row (x1 - alpha x2 >= 0, so lower bound of 0.
		GLPK.glp_set_row_bnds(lp, newLpRow, GLPKConstants.GLP_LO, 0, 0);
		GLPK.glp_set_row_bnds(lpMSA,  newLpMSARow, GLPKConstants.GLP_LO, 0, 0);
		
		// FINALLY we can run the FBA problem
		int ret = run();
		
		// Remove the rows we mucked with
		GLPK.intArray_setitem(lpRow, 1, newLpRow);
		GLPK.intArray_setitem(msaRow, 1, newLpMSARow);
		
		GLPK.glp_del_rows(lp, 1, lpRow);
		GLPK.glp_del_rows(lpMSA, 1, msaRow);
		
		return ret;
	}
	
	/**
	 * Performs an FBA run with the loaded model, constraints, and bounds. Fluxes
	 * and solutions are stored in the FBAModel class and can be used through their
	 * accessory functions
	 * @return the GLPK status code, or MODEL_NOT_INITIALIZED if there's no problem to solve
	 * @see getFluxes()
	 * @see getObjectiveSolution()
	 */
	public synchronized int run()
	{
		runSuccess = false;
		if (numMetabs == 0 || numRxns == 0)
		{
			return MODEL_NOT_INITIALIZED;
		}

		// an internal status checker. If this = 0 after a run, everything is peachy.
		int ret = -1;

		switch(objStyle)
		{
			// the usual, just max the objective
			case MAXIMIZE_OBJECTIVE_FLUX:
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
			case MINIMIZE_OBJECTIVE_FLUX:
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
			case MAXIMIZE_TOTAL_FLUX:
				ret = runOptimizeSumFluxes();
				break;
			case MINIMIZE_TOTAL_FLUX:
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
		
		if (objStyle == MAXIMIZE_OBJECTIVE_FLUX ||
			objStyle == MINIMIZE_OBJECTIVE_FLUX ||
			objStyle == MAXIMIZE_TOTAL_FLUX ||
			objStyle == MINIMIZE_TOTAL_FLUX)
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
		setObjectiveReaction(objReaction);
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
		if (objStyle == MAXIMIZE_TOTAL_FLUX)
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
		if (objStyle == MIN_OBJECTIVE_MAX_TOTAL ||
			objStyle == MIN_OBJECTIVE_MIN_TOTAL)
			GLPK.glp_set_obj_dir(lp, GLPK.GLP_MIN);
		else
			GLPK.glp_set_obj_dir(lp, GLPK.GLP_MAX);
		
		if (objStyle == MAX_OBJECTIVE_MIN_TOTAL ||
			objStyle == MIN_OBJECTIVE_MIN_TOTAL)
			GLPK.glp_set_obj_dir(lpMSA, GLPK.GLP_MIN);
		else
			GLPK.glp_set_obj_dir(lpMSA, GLPK.GLP_MAX);
		
		// setup done.
		
		int status = -1; //GLPK.glp_simplex(lp, param);
		simParam.setPresolve(GLPK.GLP_OFF); //Turn presolver off ID 
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
				if (objStyle == MAXIMIZE_OBJECTIVE_FLUX ||
					objStyle == MINIMIZE_OBJECTIVE_FLUX ||
					objStyle == MAXIMIZE_TOTAL_FLUX ||
					objStyle == MINIMIZE_TOTAL_FLUX)
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
	public double[] getExchangeFluxes()
	{
		double[] v = new double[numExch];
		if (runSuccess)
		{
			for (int i = 0; i < numExch; i++)
			{
				if (objStyle == MAXIMIZE_OBJECTIVE_FLUX ||
					objStyle == MINIMIZE_OBJECTIVE_FLUX ||
					objStyle == MAXIMIZE_TOTAL_FLUX ||
					objStyle == MINIMIZE_TOTAL_FLUX)
					v[i] = GLPK.glp_get_col_prim(lp, exch[i]);
				else
					v[i] = GLPK.glp_get_col_prim(lpMSA, exch[i]);
			}
		}
		return v;
	}

	public void setExchangeReactionNames(final String[] rxnNames)
	{
		this.exchRxnNames = rxnNames;
	}
	
	/**
	 * @return the names of all exchange reactions
	 */
	public String[] getExchangeReactionNames()
	{
		return exchRxnNames.clone();
	}
	
	public void setExchangeMetaboliteNames(final String[] metabNames)
	{
		this.exchMetabNames = metabNames;
	}
	
	/**
	 * @return the names of all extracellular metabolites
	 */
	public String[] getExchangeMetaboliteNames()
	{
		return exchMetabNames.clone();
	}

	/**
	 * @return the indices of all exchange reactions (from 1->N)
	 */
	public int[] getExchangeIndices()
	{
		return exch.clone();
	}
	
	public void setExchangeIndices(final int[] exch)
	{
		if (this.numExch == exch.length)
			this.exch = exch;
	}

	/**
	 * @return the currently known media conditions, modeled as the lower bounds of fluxes.
	 * //TODO translate this method to just lower flux bounds. I don't think I use it
	 * anywhere else anyway... or make it explicit for just FBA and NOT dFBA (or COMETS-style
	 * dFBA)
	 */
	public double[] getMediaConditions()
	{
		/*
		 * We're going with the standard motif that all exchange fluxes are
		 * constructed such that they flow out from the cell. So the
		 * "media conditions" are (for now, I know these are rates and not
		 * concentrations, so YEAH) just the lower bounds on the exchange
		 * fluxes.
		 */
		double[] media = new double[numExch];
		for (int i = 0; i < numExch; i++)
		{
			if (objStyle == MAXIMIZE_OBJECTIVE_FLUX ||
				objStyle == MINIMIZE_OBJECTIVE_FLUX)
				media[i] = -GLPK.glp_get_col_lb(lp, exch[i]);
			else
				media[i] = -GLPK.glp_get_col_lb(lpMSA, exch[i]);
		}
		return media;
	}

	/**
	 * Sets the media conditions for an FBA in terms of lower bounds of fluxes (e.g. the
	 * Palsson standard style) 
	 * @param media
	 */
	public void setMediaConditions(double[] media)
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
	public double getObjectiveSolution()
	{
		if (runSuccess)
			if (objStyle == MAXIMIZE_OBJECTIVE_FLUX ||
				objStyle == MINIMIZE_OBJECTIVE_FLUX)
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
	public double getObjectiveFluxSolution()
	{
		if (runSuccess)
			return GLPK.glp_get_col_prim(lp, objReaction);
//			return GLPK.glp_get_obj_val(lp);
		return -Double.MAX_VALUE;
	}

	/**
	 * New and improved (hopefully) file loader.
	 * With a reasonable format, similar to what's used for loading world layouts
	 * 
	 */
	public static FBAModelOld loadModelFromFile(String filename) throws ModelFileException
	{
		try
		{
			int lineNum = 0;
			BufferedReader reader = new BufferedReader(new FileReader(filename));
			int numMets = 0;  // number of rows in S-matrix
			int numRxns = 0;  // number of cols in S-matrix
			double[][] S = null;
			double[] lb = null;
			double[] ub = null;
			int obj = 0;
			int objSt = MAXIMIZE_OBJECTIVE_FLUX;
			String[] metNames = null;
			String[] rxnNames = null;
			int[] exchRxns = null;
			int numExch = 0;
			double[] diffConsts = null;
			double[] exchAlpha = null;
			double[] exchW = null;
			double[] exchKm = null;
			double[] exchVmax = null;
			double[] exchHillCoeff = null;
			
			double defaultAlpha = 0,
				   defaultW = 0,
				   defaultKm = 0,
				   defaultVmax = 0,
				   defaultHill = 0,
				   defaultLB = -1000,
				   defaultUB = 1000,
				   defaultDiff = 1e-6;
			
			boolean blockOpen = false;
			
			// first thing we need is the S-matrix. That **has** to be the first
			// data block, since it sets the scale for every other array here.
			
			String line = null;
			while ((line = reader.readLine()) != null)
			{
				line = line.trim();
				lineNum++;
				
				// read the file. do necessary stuff
				String[] tokens = line.split("\\s+");  // split the line based on whitespace
				
				// now we have a tokenized block-header. should be one of the following
				if (tokens[0].equalsIgnoreCase("SMATRIX"))
				{
					// load the stoichiometric matrix
					/* This is a sparse matrix. We should expect it to go from
					 * 1..N, NOT 0..N-1 (a la matlab format)
					 * 
					 * I guess this could be changed, but I don't see a real reason to.
					 *
					 * first, we expect to see the number of rows and columns
					 * (metabs and rxns) as part of this line
					 * if not, throw a ModelFileException)
					 */
					if (tokens.length != 3)
					{
						reader.close();
						throw new ModelFileException("The SMATRIX line should include the number of rows and columns of the Stoichiometric matrix on line " + lineNum);
					}
					
					numMets = Integer.parseInt(tokens[1]);
					if (numMets <= 0) {
						reader.close();
						throw new ModelFileException("There must be at least one row (metabolite) in the Stoichiometric matrix.");
					}
					numRxns = Integer.parseInt(tokens[2]);
					if (numRxns <= 0) {
						reader.close();
						throw new ModelFileException("There must be at least one column (reaction) in the Stoichiometric matrix.");
					}
					
					// initialize the S-matrix
					S = new double[numMets][numRxns];
					
					String matLine = null;
					
					/* gonna see the next block A LOT.
					 * the 'while' line loads the next line, trimmed, into matLine.
					 * if it's equal to '//' (i.e.: only has '//' on a line), then
					 * the block is done.
					 * 
					 * otherwise, keep looping
					 */
					
					blockOpen = true;

					while (!(matLine = reader.readLine().trim()).equalsIgnoreCase("//"))
					{
						lineNum++;
						if (matLine.length() == 0)
							continue;
						
						String[] parsed = matLine.split("\\s+");
						if (parsed.length != 3) {
							reader.close();
							throw new ModelFileException("Each line of the SMATRIX block should contain three elements - a row, column, and stoichiometric value for that element on line " + lineNum);
						}
						int x = Integer.parseInt(parsed[0]);
						if (x < 1 || x > numMets) {
							reader.close();
							throw new ModelFileException("The first element of the SMATRIX block at line " + lineNum + " corresponds to the row, and should be between 1 and the number of rows specified.");
						}

						int y = Integer.parseInt(parsed[1]);
						if (x < 1 || x > numMets) {
							reader.close();
							throw new ModelFileException("The second element of the SMATRIX block at line " + lineNum + " corresponds to the column, and should be between 1 and the number of columns specified.");
						}
						
						double stoic = Double.parseDouble(parsed[2]);
						S[x-1][y-1] = stoic;
					}
					lineNum++;
					
					blockOpen = false;
					// done!
				}

				
				/**************************************************************
				 ****************** LOAD UPPER AND LOWER BOUNDS ***************
				 **************************************************************/
				
				else if (tokens[0].equalsIgnoreCase("BOUNDS"))
				{
					// load the upper and lower bounds
					if (numRxns <= 0) {
						reader.close();
						throw new ModelFileException("The stoichiometric matrix should be loaded before the upper and lower bounds at line " + lineNum);
					}
					if (tokens.length != 3) {
						reader.close();
						throw new ModelFileException("The BOUNDS line should contain default lower and upper bound values, in that order at line " + lineNum);
					}
					
					defaultLB = Double.parseDouble(tokens[1]);
					defaultUB = Double.parseDouble(tokens[2]);
					if (defaultLB > defaultUB) {
						reader.close();
						throw new ModelFileException("The default lower bound should be LESS than the default upper bound at line " + lineNum);
					}
					
					lb = new double[numRxns];
					ub = new double[numRxns];
					
					for (int i=0; i<numRxns; i++)
					{
						lb[i] = defaultLB;
						ub[i] = defaultUB;
					}
					
					String boundLine = null;
					blockOpen = true;

					while (!(boundLine = reader.readLine().trim()).equalsIgnoreCase("//"))
					{
						lineNum++;
						if (boundLine.length() == 0)
							continue;

						String[] parsed = boundLine.split("\\s+");
						if (parsed.length != 3) {
							reader.close();
							throw new ModelFileException("There should be 3 elements on the BOUNDS line at file line " + lineNum + ": the reaction index (from 1 to N), the lower bound, and the upper bound.");
						}
						
						int rxn = Integer.parseInt(parsed[0]);
						if (rxn < 1 || rxn > numRxns) {
							reader.close();
							throw new ModelFileException("The reaction index in BOUNDS block line " + lineNum + " should be between 1 and " + numRxns);
						}
						
						double l = Double.parseDouble(parsed[1]);
						double u = Double.parseDouble(parsed[2]);
						if (l > u) {
							reader.close();
							throw new ModelFileException("The lower bound should be less than the upper bound on line " + lineNum);
						}
						
						lb[rxn-1] = l;
						ub[rxn-1] = u;
					}
					lineNum++;
					blockOpen = false;
				}
				
				/**************************************************************
				 ******************** LOAD OBJECTIVE REACTION *****************
				 **************************************************************/
				else if (tokens[0].equalsIgnoreCase("OBJECTIVE"))
				{
					if (numRxns <= 0) {
						reader.close();
						throw new ModelFileException("The stoichiometric matrix should be loaded before the objective reaction at line " + lineNum);
					}

					/* do it this way for two reasons.
					 * 1. consistency - everything else is a block of data
					 * 2. malleability - make it easier to convert to loading a linear
					 * 					 combination of reactions as the objective
					 */
					
					String objLine = null;
					blockOpen = true;

					while (!(objLine = reader.readLine().trim()).equalsIgnoreCase("//"))
					{
						lineNum++;
						if (objLine.length() == 0)
							continue;

						String[] parsed = objLine.split("\\s+");
						if (parsed.length != 1) {
							reader.close();
							throw new ModelFileException("There should be just 1 element for the objective line - the index of the reaction.");
						}
						
						int rxn = Integer.parseInt(parsed[0]);
						if (rxn < 1 || rxn > numRxns) {
							reader.close();
							throw new ModelFileException("The reaction index in OBJECTIVE block line " + lineNum + " should be between 1 and " + numRxns);
						}
						
						obj = rxn;
					}
					lineNum++;
					blockOpen = false;
				}
				
				/**************************************************************
				 **************** LOAD OBJECTIVE REACTION STYLE****************
				 **************************************************************/
				else if (tokens[0].equalsIgnoreCase("OBJECTIVE_STYLE"))
				{
					
					String objStyleLine = null;
					blockOpen = true;

					while (!(objStyleLine = reader.readLine().trim()).equalsIgnoreCase("//"))
					{
						lineNum++;
						if (objStyleLine.length() == 0)
							continue;

						String[] parsed = objStyleLine.split("\\s+");
						if (parsed.length != 1) {
							reader.close();
							throw new ModelFileException("There should be just 1 element for the objective style line - the name of the objective style.");
						}
						
						if(parsed[0].equalsIgnoreCase("MAXIMIZE_OBJECTIVE_FLUX"))
							objSt= MAXIMIZE_OBJECTIVE_FLUX;
						else if(parsed[0].equalsIgnoreCase("MINIMIZE_OBJECTIVE_FLUX"))
							objSt= MINIMIZE_OBJECTIVE_FLUX;
						else if(parsed[0].equalsIgnoreCase("MAXIMIZE_TOTAL_FLUX"))
							objSt= MAXIMIZE_TOTAL_FLUX;
						else if(parsed[0].equalsIgnoreCase("MINIMIZE_TOTAL_FLUX"))
							objSt= MINIMIZE_TOTAL_FLUX;
						else if(parsed[0].equalsIgnoreCase("MAX_OBJECTIVE_MIN_TOTAL"))
							objSt= MAX_OBJECTIVE_MIN_TOTAL;
						else if(parsed[0].equalsIgnoreCase("MAX_OBJECTIVE_MAX_TOTAL"))
							objSt= MAX_OBJECTIVE_MAX_TOTAL;
						else if(parsed[0].equalsIgnoreCase("MIN_OBJECTIVE_MIN_TOTAL"))
						    objSt= MIN_OBJECTIVE_MIN_TOTAL;
						else if(parsed[0].equalsIgnoreCase("MIN_OBJECTIVE_MAX_TOTAL"))
							objSt= MIN_OBJECTIVE_MAX_TOTAL; 
						else
						{
							reader.close();
							throw new ModelFileException("Wrong OBJECTIVE_STYLE input value in model file."); 
						}
					}
					lineNum++;
					blockOpen = false;
				}
				
				/**************************************************************
				 ********************* LOAD METABOLITE NAMES ******************
				 **************************************************************/
				else if (tokens[0].equalsIgnoreCase("METABOLITE_NAMES"))
				{
					if (numMets <= 0) 
					{
						reader.close();
						throw new ModelFileException("The stoichiometric matrix should be loaded before the metabolite names at line " + lineNum);
					}
					
					metNames = new String[numMets];
					String metLine = null;
					int numNames = 0;
					blockOpen = true;

					while (!(metLine = reader.readLine().trim()).equalsIgnoreCase("//"))
					{
						lineNum++;
						// just keep the whole line as a metabolite name
						if (metLine.length() == 0)
							continue;
						
						if (numNames >= numMets) 
						{
							reader.close();
							throw new ModelFileException("There must be one name for each metabolite, on each line of the METABOLITE_NAMES block. There's at least one extra at line " + lineNum);
						}
						
						metNames[numNames] = metLine;
						numNames++;
					}
					lineNum++;
					blockOpen = false;

					if (numNames != numMets)
					{
						reader.close();
						throw new ModelFileException("There must be one name for each metabolite, on each line of the METABOLITE_NAMES block. There are apparently " + (numMets - numNames) + " names missing.");
					}

				}
				
				/**************************************************************
				 ********************** LOAD REACTION NAMES *******************
				 **************************************************************/
				else if (tokens[0].equalsIgnoreCase("REACTION_NAMES"))
				{
					// load reaction names
					if (numRxns <= 0)
					{
						reader.close();
						throw new ModelFileException("The stoichiometric matrix should be loaded before the reaction names at line " + lineNum);
					}
					
					rxnNames = new String[numRxns];
					String rxnLine = null;
					int numNames = 0;
					blockOpen = true;

					while (!(rxnLine = reader.readLine().trim()).equalsIgnoreCase("//"))
					{
						lineNum++;
						// just keep the whole line as a reaction name
						if (rxnLine.length() == 0)
							continue;

						if (numNames >= numRxns)
						{
							reader.close();
							throw new ModelFileException("There must be one name for each reaction, on each line of the REACTION_NAMES block. There's at least one extra at line " + lineNum);
						}
						
						rxnNames[numNames] = rxnLine;
						numNames++;
					}
					lineNum++;
					blockOpen = false;

					if (numNames != numRxns)
					{
						reader.close();
						throw new ModelFileException("There must be one name for each reaction, on each line of the REACTION_NAMES block. There are apparently " + (numRxns - numNames) + " names missing.");
					}
				}
				
				/**************************************************************
				 ******************** LOAD EXCHANGE REACTIONS *****************
				 **************************************************************/
				else if (tokens[0].equalsIgnoreCase("EXCHANGE_REACTIONS"))
				{
					// load exchange reaction indices
					if (numRxns <= 0)
					{
						reader.close();
						throw new ModelFileException("The stoichiometric matrix should be loaded before the exchange reaction list at line " + lineNum);
					}

					/* do it this way for consistency - everything else is a block of data
					 */
					
					String exchLine = null;
					blockOpen = true;

					while (!(exchLine = reader.readLine().trim()).equalsIgnoreCase("//"))
					{
						lineNum++;
						if (exchLine.length() == 0)
							continue;

						String[] parsed = exchLine.split("\\s+");
						if (parsed.length > numRxns)
						{
							reader.close();
							throw new ModelFileException("There should be, at most, " + numRxns + " values in the EXCHANGE_REACTIONS block. Looks like there's " + parsed.length + " instead");
						}
						if (parsed.length == 0)
						{
							exchRxns = new int[0];
						}
						else
						{
							// 1. Dump everything into a unique hash set
							Set<Integer> exchSet = new HashSet<Integer>();
							for (int i=0; i<parsed.length; i++)
							{
								int exch = Integer.parseInt(parsed[i]);
								if (exch < 1 || exch > numRxns)
								{
									reader.close();
									throw new ModelFileException("Each exchange reaction should be between 1 and " + numRxns + " at line " + lineNum);
								}
								exchSet.add(exch);
							}
							// 2. Extract into an int array
							exchRxns = new int[exchSet.size()];
							Iterator<Integer> it = exchSet.iterator();
							int i=0;
							while (it.hasNext())
							{
								exchRxns[i] = it.next().intValue();
								i++;
							}
							
							// 3. Sort the array. And done!
							Arrays.sort(exchRxns);
						}
					}
					lineNum++;
					blockOpen = false;
					numExch = exchRxns.length;
				}
				
				// thankfully, the rest will all have the same code shape.
				// which means a bit of copy / pasting, but, it'll be easier.
				
				/**************************************************************
				 ******************* LOAD DIFFUSION CONSTANTS *****************
				 **************************************************************/
//				else if (tokens[0].equalsIgnoreCase("DIFFUSION_CONSTANTS"))
//				{
//					// load diffusion constants
//					if (numRxns <= 0)
//						throw new ModelFileException("The stoichiometric matrix should be loaded before the diffusion constants at line " + lineNum);
//					if (exchRxns == null)
//						throw new ModelFileException("The list of exchange reactions should be loaded before the diffusion constants at line " + lineNum);
//					
//					if (tokens.length != 2)
//						throw new ModelFileException("The DIFFUSION_CONSTANTS block header should be followed only by the default diffusion constant at line " + lineNum);
//					
//					defaultDiff = Double.parseDouble(tokens[1]);
//					if (defaultDiff < 0)
//						throw new ModelFileException("The default diffusion constant on line " + lineNum + " should be >= 0");
//						
//					diffConsts = new double[numExch];
//					for (int i=0; i<numExch; i++)
//					{
//						diffConsts[i] = defaultDiff;
//					}
//					String diffLine = null;
//					blockOpen = true;
//					while (!(diffLine = reader.readLine().trim()).equalsIgnoreCase("//"))
//					{
//						lineNum++;
//						if (diffLine.length() == 0)
//							continue;
//
//						String[] parsed = diffLine.split("\\s+");
//						if (parsed.length != 2)
//							throw new ModelFileException("There should be 2 elements on each line of the DIFFUSION_CONSTANTS block at line " + lineNum + ": the exchange reaction index (from 1 to " + numExch + ") and the diffusion constant of the extracellular metabolite of that reaction.");
//						
//						int rxn = Integer.parseInt(parsed[0]);
//						if (rxn < 1 || rxn > numExch)
//							throw new ModelFileException("The reaction index in DIFFUSION_CONSTANTS block line " + lineNum + " should be between 1 and " + numExch);
//						
//						double d = Double.parseDouble(parsed[1]);
//						if (d < 0)
//							throw new ModelFileException("The diffusion constant on line " + lineNum + " should be >= 0");
//						
//						diffConsts[rxn-1] = d;
//					}
//					lineNum++;
//					blockOpen = false;
//				}
				
				/**************************************************************
				 *********************** LOAD ALPHA VALUES*********************
				 **************************************************************/
				else if (tokens[0].equalsIgnoreCase("ALPHA_VALUES"))
				{
					// load alpha values
					if (numRxns <= 0)
					{
						reader.close();
						throw new ModelFileException("The stoichiometric matrix should be loaded before the alpha values at line " + lineNum);
					}
					if (exchRxns == null)
					{
						reader.close();
						throw new ModelFileException("The list of exchange reactions should be loaded before the alpha values at line " + lineNum);
					}
					
					if (tokens.length != 2)
					{
						reader.close();
						throw new ModelFileException("The ALPHA_VALUES block header should be followed only by the default alpha value at line " + lineNum);
					}
					
					defaultAlpha = Double.parseDouble(tokens[1]);
					if (defaultAlpha <= 0)
					{
						reader.close();
						throw new ModelFileException("The default alpha value given at line " + lineNum + "should be > 0");
					}

					exchAlpha = new double[numExch];
					for (int i=0; i<numExch; i++)
					{
						exchAlpha[i] = defaultAlpha;
					}
					String alphaLine = null;
					blockOpen = true;
					while (!(alphaLine = reader.readLine().trim()).equalsIgnoreCase("//"))
					{
						lineNum++;
						if (alphaLine.length() == 0)
							continue;

						String[] parsed = alphaLine.split("\\s+");
						if (parsed.length != 2)
						{
							reader.close();
							throw new ModelFileException("There should be 2 elements on each line of the ALPHA_VALUES block at line " + lineNum + ": the exchange reaction index (from 1 to " + numExch + ") and the alpha value of that reaction.");
						}
						
						int rxn = Integer.parseInt(parsed[0]);
						if (rxn < 1 || rxn > numExch)
						{
							reader.close();
							throw new ModelFileException("The reaction index in ALPHA_VALUES block line " + lineNum + " should be between 1 and " + numExch);
						}
						
						double a = Double.parseDouble(parsed[1]);
						if (a <= 0)
						{
							reader.close();
							throw new ModelFileException("The alpha value on line " + lineNum + " should be > 0");
						}
						
						exchAlpha[rxn-1] = a;
					}
					lineNum++;
					blockOpen = false;
				}
				
				/**************************************************************
				 ************************* LOAD W VALUES***********************
				 **************************************************************/
				else if (tokens[0].equalsIgnoreCase("W_VALUES"))
				{
					// load W values
					if (numRxns <= 0)
					{
						reader.close();
						throw new ModelFileException("The stoichiometric matrix should be loaded before the W values at line " + lineNum);
					}
					if (exchRxns == null)
					{
						reader.close();
						throw new ModelFileException("The list of exchange reactions should be loaded before the W values at line " + lineNum);
					}
					
					if (tokens.length != 2)
					{
						reader.close();
						throw new ModelFileException("The W_VALUES block header should be followed only by the default W value at line " + lineNum);
					}
					
					defaultW = Double.parseDouble(tokens[1]);
					if (defaultW <= 0)
					{
						reader.close();
						throw new ModelFileException("The default W value given at line " + lineNum + "should be > 0");
					}

					exchW = new double[numExch];
					for (int i=0; i<numExch; i++)
					{
						exchW[i] = defaultW;
					}
					String wLine = null;
					blockOpen = true;

					while (!(wLine = reader.readLine().trim()).equalsIgnoreCase("//"))
					{
						lineNum++;
						if (wLine.length() == 0)
							continue;

						String[] parsed = wLine.split("\\s+");
						if (parsed.length != 2)
						{
							reader.close();
							throw new ModelFileException("There should be 2 elements on each line of the W_VALUES block at line " + lineNum + ": the exchange reaction index (from 1 to " + numExch + ") and the W value of that reaction.");
						}
						
						int rxn = Integer.parseInt(parsed[0]);
						if (rxn < 1 || rxn > numExch)
						{
							reader.close();
							throw new ModelFileException("The reaction index in W_VALUES block line " + lineNum + " should be between 1 and " + numExch);
						}
						
						double w = Double.parseDouble(parsed[1]);
						if (w <= 0)
						{
							reader.close();
							throw new ModelFileException("The W value on line " + lineNum + " should be > 0");
						}
						
						exchW[rxn-1] = w;
					}
					lineNum++;
					blockOpen = false;
				}
				
				/**************************************************************
				 ************************ LOAD KM VALUES **********************
				 **************************************************************/
				else if (tokens[0].equalsIgnoreCase("KM_VALUES"))
				{
					// load KM values
					if (numRxns <= 0)
					{
						reader.close();
						throw new ModelFileException("The stoichiometric matrix should be loaded before the Km values at line " + lineNum);
					}
					if (exchRxns == null)
					{
						reader.close();
						throw new ModelFileException("The list of exchange reactions should be loaded before the Km values at line " + lineNum);
					}
					
					if (tokens.length != 2)
					{
						reader.close();
						throw new ModelFileException("The KM_VALUES block header should be followed only by the default Km value at line " + lineNum);
					}
					
					defaultKm = Double.parseDouble(tokens[1]);
					if (defaultKm <= 0)
					{
						reader.close();
						throw new ModelFileException("The default Km value given at line " + lineNum + "should be > 0");
					}

					exchKm = new double[numExch];
					for (int i=0; i<numExch; i++)
					{
						exchKm[i] = defaultKm;
					}
					String kmLine = null;
					blockOpen = true;
					while (!(kmLine = reader.readLine().trim()).equalsIgnoreCase("//"))
					{
						lineNum++;
						if (kmLine.length() == 0)
							continue;

						String[] parsed = kmLine.split("\\s+");
						if (parsed.length != 2)
						{
							reader.close();
							throw new ModelFileException("There should be 2 elements on each line of the KM_VALUES block at line " + lineNum + ": the exchange reaction index (from 1 to " + numExch + ") and the Km value of that reaction.");
						}
						
						int rxn = Integer.parseInt(parsed[0]);
						if (rxn < 1 || rxn > numExch)
						{
							reader.close();
							throw new ModelFileException("The reaction index in KM_VALUES block line " + lineNum + " should be between 1 and " + numExch);
						}
						
						double km = Double.parseDouble(parsed[1]);
						if (km <= 0)
						{
							reader.close();
							throw new ModelFileException("The Km value on line " + lineNum + " should be > 0");
						}
						
						exchKm[rxn-1] = km;
					}
					lineNum++;
					blockOpen = false;
				}
				
				/**************************************************************
				 ********************* LOAD VMAX VALUES ***********************
				 **************************************************************/
				else if (tokens[0].equalsIgnoreCase("VMAX_VALUES"))
				{
					if (numRxns <= 0)
					{
						reader.close();
						throw new ModelFileException("The stoichiometric matrix should be loaded before the Vmax values at line " + lineNum);
					}
					if (exchRxns == null)
					{
						reader.close();
						throw new ModelFileException("The list of exchange reactions should be loaded before the Vmax values at line " + lineNum);
					}
					
					if (tokens.length != 2)
					{
						reader.close();
						throw new ModelFileException("The VMAX_VALUES block header should be followed only by the default Vmax value at line " + lineNum);
					}
					
					defaultVmax = Double.parseDouble(tokens[1]);
					if (defaultVmax <= 0)
					{
						reader.close();
						throw new ModelFileException("The default Vmax value given at line " + lineNum + "should be > 0");
					}

					exchVmax = new double[numExch];
					for (int i=0; i<numExch; i++)
					{
						exchVmax[i] = defaultVmax;
					}
					String vMaxLine = null;
					blockOpen = true;
					while (!(vMaxLine = reader.readLine().trim()).equalsIgnoreCase("//"))
					{
						lineNum++;
						String[] parsed = vMaxLine.split("\\s+");
						if (vMaxLine.length() == 0)
							continue;

						if (parsed.length != 2)
						{
							reader.close();
							throw new ModelFileException("There should be 2 elements on each line of the VMAX_VALUES block at line " + lineNum + ": the exchange reaction index (from 1 to " + numExch + ") and the Vmax value of that reaction.");
						}
						
						int rxn = Integer.parseInt(parsed[0]);
						if (rxn < 1 || rxn > numExch)
						{
							reader.close();
							throw new ModelFileException("The reaction index in VMAX_VALUES block line " + lineNum + " should be between 1 and " + numExch);
						}
						
						double vMax = Double.parseDouble(parsed[1]);
						if (vMax <= 0)
						{
							reader.close();
							throw new ModelFileException("The vMax value on line " + lineNum + " should be > 0");
						}
						
						exchVmax[rxn-1] = vMax;
					}
					lineNum++;
					blockOpen = false;
				}
				
				/**************************************************************
				 ******************* LOAD HILL COEFFICIENTS *******************
				 **************************************************************/
				else if (tokens[0].equalsIgnoreCase("HILL_COEFFICIENTS"))
				{
					if (numRxns <= 0)
					{
						reader.close();
						throw new ModelFileException("The stoichiometric matrix should be loaded before the Hill coefficients at line " + lineNum);
					}
					if (exchRxns == null)
					{
						reader.close();
						throw new ModelFileException("The list of exchange reactions should be loaded before the Hill coefficients at line " + lineNum);
					}
					
					if (tokens.length != 2)
					{
						reader.close();
						throw new ModelFileException("The HILL_COEFFICIENTS block header should be followed only by the default Hill coefficient at line " + lineNum);
					}
					
					defaultHill = Double.parseDouble(tokens[1]);
					if (defaultHill < 0)
					{
						reader.close();
						throw new ModelFileException("The default Hill coefficient given at line " + lineNum + "should be >= 0");
					}

					exchHillCoeff = new double[numExch];
					for (int i=0; i<numExch; i++)
					{
						exchHillCoeff[i] = defaultHill;
					}
					String hillLine = null;
					blockOpen = true;
					while (!(hillLine = reader.readLine().trim()).equalsIgnoreCase("//"))
					{
						lineNum++;
						if (hillLine.length() == 0)
							continue;

						String[] parsed = hillLine.split("\\s+");
						if (parsed.length != 2)
						{
							reader.close();
							throw new ModelFileException("There should be 2 elements on each line of the HILL_COEFFICIENTS block at line " + lineNum + ": the exchange reaction index (from 1 to " + numExch + ") and the Hill coefficient of that reaction.");
						}
						
						int rxn = Integer.parseInt(parsed[0]);
						if (rxn < 1 || rxn > numExch)
						{
							reader.close();
							throw new ModelFileException("The reaction index in HILL_COEFFICIENTS block line " + lineNum + " should be between 1 and " + numExch);
						}
						
						double hill = Double.parseDouble(parsed[1]);
						if (hill < 0)
						{
							reader.close();
							throw new ModelFileException("The Hill coefficient on line " + lineNum + " should be >= 0");
						}
						
						exchHillCoeff[rxn-1] = hill;
					}
					lineNum++;
					blockOpen = false;
				}
			}
			reader.close();
			if (blockOpen)
				throw new ModelFileException("Each data block is expected to end with '//' on a single line.");
			// double check for initialization
			if (S == null)
				throw new ModelFileException("To make an FBA model, a Stoichiometric matrix MUST be initialized!");
			if (lb == null || ub == null)
				throw new ModelFileException("To make an FBA model, a set of lower and upper bounds MUST be initialized!");
			if (exchRxns == null)
				throw new ModelFileException("To make an FBA model, a set of exchange reactions MUST be initialized!");
			if (diffConsts == null)
			{
				diffConsts = new double[numExch];
				for (int i=0; i<numExch; i++)
					diffConsts[i] = defaultDiff;
			}
				
			if (exchKm == null)
			{
				exchKm = new double[numExch];
				for (int i=0; i<numExch; i++)
					exchKm[i] = defaultKm;
			}
			
			if (exchVmax == null)
			{
				 exchVmax = new double[numExch];
				 for (int i=0; i<numExch; i++)
					 exchVmax[i] = defaultVmax;
			}
			if (exchHillCoeff == null)
			{
				exchHillCoeff = new double[numExch];
				for (int i=0; i<numExch; i++)
					exchHillCoeff[i] = defaultHill;
			}
			if (exchAlpha == null)
			{
				exchAlpha = new double[numExch];
				for (int i=0; i<numExch; i++)
					exchAlpha[i] = defaultAlpha;
			}
			if (exchW == null)
			{
				exchW = new double[numExch];
				for (int i=0; i<numExch; i++)
					exchW[i] = defaultW;
			}
			
			FBAModelOld model = new FBAModelOld(S, lb, ub, obj, exchRxns, diffConsts, exchKm, exchVmax, exchHillCoeff, exchAlpha, exchW, metNames, rxnNames, objSt);
			model.setDefaultAlpha(defaultAlpha);
			model.setDefaultW(defaultW);
			model.setDefaultHill(defaultHill);
			model.setDefaultKm(defaultKm);
			model.setDefaultVmax(defaultVmax);
			model.setDefaultLB(defaultLB);
			model.setDefaultUB(defaultUB);
			model.setDefaultMetabDiffConst(defaultDiff);
			
			
			model.setFileName(filename);
			return model;
			
		}
		catch (FileNotFoundException e)
		{
			throw new ModelFileException(ModelFileException.FILE_NOT_FOUND, "Unable to find model file '" + filename + "': " + e);
		} 
		catch (IOException e)
		{
			throw new ModelFileException(ModelFileException.IO_ERROR, "I/O error in model file '" + filename + "': " + e);
		} 
		catch (NumberFormatException e)
		{
			throw new ModelFileException(ModelFileException.NUMBER_FORMAT_ERROR, "Number formatting error in model file '" + filename + "': " + e);
		}
	}
	
	
	/**
	 * Constructs and returns a new <code>FBAModel</code> from a given file in
	 * the special COMETS file format. And by special, I mean it should wear a helmet
	 * when it goes outside. I know that's unprofessional and inappropriate, but seriously.
	 * I can't wait until I can focus on writing good and thoughtful code, and not 
	 * hacking at something until it works and just leaving it.
	 * <p>
	 * Anyway. There's documentation on the format. I'm not gonna put it here.
	 * @param filename the name of the COMETS model file to load
	 * @return a shiny new FBAModel
	 * @throws ModelFileException
	 */
//	public static FBAModel loadModelFromFile_old(String filename) throws ModelFileException
//	{
//		/*
//		 * model file format (tab delimited!) FILE_START <num_metabs>
//		 * <num_reactions>\n <tab-delimited S-matrix - expect num_metabs rows of
//		 * num_reactions elements>\n <tab-delimited lower bounds - expect one
//		 * row of num_reactions elements>\n <tab-delimited upper bounds - expect
//		 * one row of num_reactions elements>\n <objective reaction index -
//		 * expect a single integer from 1 to num_reactions>\n
//		 */
//		
//		try
//		{
//			BufferedReader reader = new BufferedReader(new FileReader(filename));
//			String line;
//			int numNonzero = 0;
//			// get the first line
//			line = reader.readLine().trim();
//			// parse out the size of the model
//			String[] dimStr = line.split("\\s+");
//			int[] dim = { Integer.valueOf(dimStr[0]),
//					      Integer.valueOf(dimStr[1]) };
//			if (dimStr.length > 2)
//				numNonzero = Integer.valueOf(dimStr[2]);
//			
//			// now we know the size of the S matrix
//			double[][] S = new double[dim[0]][dim[1]];
//
//			if (numNonzero == 0) // eg., hasn't been initialized
//			{
//				// parse out the next dim[0] lines - these are the S matrix.
//				for (int i = 0; i < dim[0]; i++)
//				{
//					line = reader.readLine().trim();
//					String[] row = line.split("\\s+");
//					for (int j = 0; j < dim[1]; j++)
//					{
//						S[i][j] = Double.valueOf(row[j]);
//					}
//				}
//			}
//			else	// if numNonzero != 0, this is a sparse matrix. so load it as one.
//			{
//				for (int i=0; i<numNonzero; i++)
//				{
//					line = reader.readLine().trim();
//					String[] row = line.split("\\s+");
//					S[Integer.valueOf(row[0])-1][Integer.valueOf(row[1])-1] = Double.valueOf(row[2]);
//				}
//			}
//
//			// the next two lines are the lb and ub
//			double[] lb = new double[dim[1]];
//			double[] ub = new double[dim[1]];
//
//			line = reader.readLine().trim();
//			String[] lbStr = line.split("\\s+");
//			line = reader.readLine().trim();
//			String[] ubStr = line.split("\\s+");
//
//			for (int i = 0; i < dim[1]; i++)
//			{
//				lb[i] = Double.valueOf(lbStr[i]);
//				ub[i] = Double.valueOf(ubStr[i]);
//			}
//
//			// objective line - just one number.
//			line = reader.readLine().trim();
//			int obj = Integer.valueOf(line);
//
//			// Metabolite names
//			line = reader.readLine().trim();
//			String[] metabNames = line.split("\\s+");
//			if (metabNames.length != dim[0])
//				throw new ModelFileException("There should be " + dim[0] + " metabolite names, but " + metabNames.length + " were found in file: " + filename);
//			
//			// Reaction names
//			line = reader.readLine().trim();
//			String[] rxnNames = line.split("\\s+");
//			if (rxnNames.length != dim[1])
//				throw new ModelFileException("There should be " + dim[1] + " reaction names, but " + rxnNames.length + " were found in file: " + filename);
//			
//			// list of exchange fluxes
//			line = reader.readLine();
//			String[] exchIndStr = line.split("\\s+");
//			int[] exch = new int[exchIndStr.length];
//			double diffConsts[] = new double[exchIndStr.length];
//			double exchKm[] = new double[exchIndStr.length];
//			double exchVmax[] = new double[exchIndStr.length];
//			double exchHillCoeff[] = new double[exchIndStr.length];
//			double exchAlpha[] = new double[exchIndStr.length];
//			double exchW[] = new double[exchIndStr.length];
//			
//			for (int i = 0; i < exchIndStr.length; i++)
//			{
//				exch[i] = Integer.valueOf(exchIndStr[i]);
//				diffConsts[i] = -1;
//				exchKm[i] = -1;
//				exchVmax[i] = -1;
//				exchHillCoeff[i] = -1;
//				exchAlpha[i] = -1;
//				exchW[i] = -1;
//			}
//
//			/** The remaining lines are optional, and can be fulfilled by default
//			 *  values by the rest of FBAComets
//			 */
//			
//			// list of diffusion constants for extracellular metabolites
//			line = reader.readLine();
//			if (line != null)
//			{
//				String[] diffConstStr = line.split("\\s+");
//				if (diffConstStr.length != diffConsts.length)
//					throw new ModelFileException("Expected " + diffConsts.length + " diffusion constants, but read " + diffConstStr.length);
//				for (int i = 0; i < diffConstStr.length; i++)
//				{
//					diffConsts[i] = Double.valueOf(diffConstStr[i]);
//				}
//			}
//			
//			// list of Km values for Monod-style uptake constraints
//			line = reader.readLine();
//			if (line != null)
//			{
//				String[] kmStr = line.split("\\s+");
//				if (kmStr.length != exchKm.length)
//					throw new ModelFileException("Expected " + kmStr.length + " Km values, but read " + kmStr.length);
//				for (int i = 0; i < kmStr.length; i++)
//				{
//					exchKm[i] = Double.valueOf(kmStr[i]); 
//				}
//			}
//			
//			// list of Vmax values for Monod-style uptake constraints			
//			line = reader.readLine();
//			if (line != null)
//			{
//				String[] vmaxStr = line.split("\\s+");
//				if (vmaxStr.length != exchVmax.length)
//					throw new ModelFileException("Expected " + vmaxStr.length + " Vmax values, but read " + vmaxStr.length);
//				for (int i = 0; i < vmaxStr.length; i++)
//				{
//					exchVmax[i] = Double.valueOf(vmaxStr[i]);
//				}
//			}
//
//			// list of Hill coefficient values for Monod-style uptake constraints
//			line = reader.readLine();
//			if (line != null)
//			{
//				String[] hillStr = line.split("\\s+");
//				if (hillStr.length != exchHillCoeff.length)
//					throw new ModelFileException("Expected " + hillStr.length + " Hill coefficient values, but read " + hillStr.length);
//				for (int i = 0; i < hillStr.length; i++)
//				{
//					exchHillCoeff[i] = Double.valueOf(hillStr[i]);
//				}
//			}
//
//			reader.close();
//
//			FBAModel model = new FBAModel(S, lb, ub, obj, exch, diffConsts, exchKm, exchVmax, exchHillCoeff, exchAlpha, exchW, metabNames, rxnNames);
//			model.setFileName(filename);
//			return model;
//		}
//		catch (FileNotFoundException e)
//		{
//			throw new ModelFileException(ModelFileException.FILE_NOT_FOUND, "Unable to find model file '" + filename + "'");
//		} 
//		catch (IOException e)
//		{
//			throw new ModelFileException(ModelFileException.IO_ERROR, "Error in model file '" + filename + "'");
//		} 
//		catch (NumberFormatException e)
//		{
//			throw new ModelFileException(ModelFileException.NUMBER_FORMAT_ERROR, "Error in model file '" + filename + "'");
//		}
//	}
	
	/**
	 * A debug tool that prints the upper and lower bounds to System.out
	 */
	public void printBounds()
	{
		double[] lb = getLowerBounds();
		double[] ub = getUpperBounds();
		
		System.out.println("BOUNDARY CONDITIONS");
		for (int i=0; i<lb.length; i++)
		{
			System.out.println(baseLB[i] + "\t" + baseUB[i] + "\t\t" + lb[i] + "\t" + ub[i]);
		}
		for (int i=0; i<baseExchLB.length; i++)
			System.out.println(baseExchLB[i]);
	}
	
	/**
	 * @return the names of all extracellular metabolites
	 */
	public String[] getMediaNames()
	{
		return getExchangeMetaboliteNames();
	}
	
	/**
	 * @return the growth diffusion constant for this model in cm^2/s (e.g., how much
	 * any newly produced biomass should diffuse)
	 */
	public double getGrowthDiffusionConstant() 
	{ 
		return growthDiffConst; 
	}
	
	/**
	 * Sets the growth diffusion constant for this model in cm^2/s
	 * @param val if less than 0, this does nothing
	 */
	public void setGrowthDiffusionConstant(double val)
	{
		if (val >= 0)
			growthDiffConst = val;
	}
	
	/**
	 * @return the flow diffusion constant for this model in cm^2/s (e.g., how much this
	 * model diffuses regardless of whether it is growing)
	 */
	public double getFlowDiffusionConstant() 
	{ 
		return flowDiffConst; 
	}
	
	/**
	 * Sets the flow diffusion constant for this model in cm^2/s
	 * @param val if less than 0, this does nothing
	 */
	public void setFlowDiffusionConstant(double val)
	{
		if (val >= 0)
			flowDiffConst = val;
	}

	/**
	 * Sets the upper bound on the objective reaction. 
	 * @param ub
	 * @return PARAMS_ERROR if ub < the current lb for the objective, PARAMS_OK otherwise
	 */
	public int setObjectiveUpperBound(double ub)
	{
		double lb = GLPK.glp_get_col_lb(lp, objReaction);
		if (ub < lb)
			return PARAMS_ERROR;
		int type = GLPKConstants.GLP_DB;
		if (lb == ub)
			type = GLPKConstants.GLP_FX;

		GLPK.glp_set_col_bnds(lp, objReaction, type, lb, ub);
		
		return PARAMS_OK;
	}
	
	/**
	 * Returns the info panel for this <code>FBAModel</code> (as required by 
	 * the <code>Model</code> class
	 * 
	 * //TODO - do this
	 */
	public JComponent getInfoPanel()
	{
		JPanel panel = new JPanel();
		panel.add(new JLabel("Model info goes here."));
		return panel;
	}
	
	/**
	 * Constructs a new FBAModel using the primary variables of the main one.
	 * This is used to clone a new FBAModel for backup.
	 * @param lp
	 * @param lpMSA
	 * @param baseLB
	 * @param baseUB
	 * @param baseExchLB
	 * @param baseExchUB
	 * @param objReaction
	 * @param exchIndices
	 * @param exchKm
	 * @param exchVmax
	 * @param exchHillCoeff
	 * @param metabNames
	 * @param rxnNames
	 * @param exchMetabNames
	 * @param exchRxnNames
	 */
//	protected FBAModel(glp_prob lp,
//					   glp_prob lpMSA,
//					   double[] baseLB,
//					   double[] baseUB,
//					   double[] baseExchLB,
//					   double[] baseExchUB,
//					   int objReaction,
//					   int[] exchIndices,
//					   double[] exchKm,
//					   double[] exchVmax,
//					   double[] exchHillCoeff,
//					   double[] exchAlpha,
//					   double[] exchW,
//					   String[] metabNames,
//					   String[] rxnNames,
//					   String[] exchMetabNames,
//					   String[] exchRxnNames,
//					   double defaultAlpha,
//					   double defaultW,
//					   double defaultKm,
//					   double defaultVmax,
//					   double defaultHill,
//					   double default)
//	{
//		this.lp = lp;
//		this.lpMSA = lpMSA;
//		
//		this.exch = exchIndices;
//		this.exchKm = exchKm;
//		this.exchVmax = exchVmax;
//		this.exchHillCoeff = exchHillCoeff;
//		
//		this.exchAlpha = exchAlpha;
//		this.exchW = exchW;
//		
//		this.baseLB = baseLB;
//		this.baseUB = baseUB;
//		this.baseExchLB = baseExchLB;
//		this.baseExchUB = baseExchUB;
//			
//		this.rxnNames = rxnNames;
//		this.metabNames = metabNames;
//		this.exchRxnNames = exchRxnNames;
//		this.exchMetabNames = exchMetabNames;
//
//		this.objReaction = objReaction;
//		numMetabs = metabNames.length;
//		numRxns = rxnNames.length;
//		
//		this.defaultAlpha = defaultAlpha;
//		this.defaultHill = defaultHill;
//		this.defaultKm = defaultKm;
//		this.defaultVmax = defaultVmax;
//		this.defaultW = defaultW;
//		this.defaultLB = defaultLB;
//		this.defaultUB = defaultUB;
//		this.defaultMetabDiffConst = defaultMetabDiffConst;
//		
//		setParameters();
//	}
	
//	public void copyTest()
//	{
//		glp_prob lpNew = GLPK.glp_create_prob();
//		GLPK.glp_copy_prob(lpNew, lp, GLPK.GLP_ON);
//		GLPK.glp_set_col_bnds(lpNew, 1, GLPK.GLP_DB, -9000, 9000);
//		System.out.println("LB: " + GLPK.glp_get_col_lb(lpNew, 1) + " UB: " + GLPK.glp_get_col_ub(lpNew, 1));
//		System.out.println("LB: " + GLPK.glp_get_col_lb(lp, 1) + " UB: " + GLPK.glp_get_col_ub(lp, 1));
//	}
	
	protected FBAModelOld(glp_prob lp, glp_prob lpMSA, int numMetabs, int numRxns, int numExch)
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
	public FBAModelOld clone()
	{
		glp_prob lpNew = GLPK.glp_create_prob();
		GLPK.glp_copy_prob(lpNew, lp, GLPK.GLP_ON);

		glp_prob lpMSANew = GLPK.glp_create_prob();
		GLPK.glp_copy_prob(lpMSANew, lpMSA, GLPK.GLP_ON);

		FBAModelOld modelCopy = new FBAModelOld(lpNew, lpMSANew, numMetabs, numRxns, numExch);
		modelCopy.setBaseBounds(getBaseLowerBounds(), getBaseUpperBounds());
		modelCopy.setBaseExchLowerBounds(getBaseExchLowerBounds());
		modelCopy.setBaseExchUpperBounds(getBaseExchUpperBounds());
		modelCopy.setObjectiveReaction(getObjectiveIndex());
		modelCopy.setExchangeIndices(getExchangeIndices());
		modelCopy.setExchangeKm(getExchangeKm());
		modelCopy.setExchangeVmax(getExchangeVmax());
		modelCopy.setExchangeHillCoefficients(getExchangeHillCoefficients());
		modelCopy.setExchangeAlphaCoefficients(getExchangeAlphaCoefficients());
		modelCopy.setExchangeWCoefficients(getExchangeWCoefficients());
		modelCopy.setExchangeMetaboliteDiffusionConstants(getExchangeMetaboliteDiffusionConstants());
		modelCopy.setMetaboliteNames(getMetaboliteNames());
		modelCopy.setReactionNames(getReactionNames());
		modelCopy.setExchangeMetaboliteNames(getExchangeMetaboliteNames());
		modelCopy.setExchangeReactionNames(getExchangeReactionNames());
		modelCopy.setDefaultAlpha(getDefaultAlpha());
		modelCopy.setDefaultW(getDefaultW());
		modelCopy.setDefaultHill(getDefaultHill());
		modelCopy.setDefaultKm(getDefaultKm());
		modelCopy.setDefaultVmax(getDefaultVmax());
		modelCopy.setDefaultLB(getDefaultLB());
		modelCopy.setDefaultUB(getDefaultUB());
		modelCopy.setDefaultMetabDiffConst(getDefaultMetabDiffConst());
		
		
		
		
		
//		FBAModel modelCopy = new FBAModel(lpNew, 
//										  lpMSANew,
//										  getBaseLowerBounds(),
//										  getBaseUpperBounds(),
//										  getBaseExchLowerBounds(),
//										  getBaseExchUpperBounds(),
//										  getObjectiveIndex(),
//										  getExchangeIndices(),
//										  getExchangeKm(),
//										  getExchangeVmax(),
//										  getExchangeHillCoefficients(),
//										  getExchangeAlphaCoefficients(),
//										  getExchangeWCoefficients(),
//										  getMetaboliteNames(),
//										  getReactionNames(),
//										  getExchangeMetaboliteNames(),
//										  getExchangeReactionNames());
		modelCopy.setObjectiveStyle(getObjectiveStyle());
		modelCopy.setFileName(this.getFileName());
		modelCopy.setParameters();
		return modelCopy;
	}

	/**
	 * A little debug code that prints the stoichiometric matrix.
	 */
	public void printMatrix()
	{
		printMatrix(false);
	}
	
	/**
	 * Some debugging code that prints the matrix for the min sum fluxes problem
	 */
	public void printMSAMatrix()
	{
		printMatrix(true);
	}
	
	/**
	 * Prints either the lp matrix or the lpMSA matrix, depending on if useMSA is
	 * false or true
	 * @param useMSA if true, print the MSA matrix, otherwise the plain lp matrix
	 */
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
	
	/**
	 * Some debugging code that prints up a single column of the lp matrix on a single line 
	 * @param j should be a value from 1->N or this does nothing
	 */
	public void printColumn(int j)
	{
		if (j < 1 || j > numRxns)
			return;
		
		SWIGTYPE_p_int ia = GLPK.new_intArray(numMetabs);
		SWIGTYPE_p_double col = GLPK.new_doubleArray(numMetabs);
		int len = GLPK.glp_get_mat_col(lp, j, ia, col);
		
		for (int i=0; i<len; i++)
		{
			System.out.println(GLPK.doubleArray_getitem(col, i+1));
		}
	}
	
	/**
	 * @return the parameters panel for this <code>FBAModel<code>
	 */
	public JComponent getParametersPanel()
	{
		paramsPanel = new ModelParametersPanel(this);
		return paramsPanel;
	}
	
	/**
	 * Applies any parameter change from teh parameters panel to the <code>FBAModel</code> 
	 */
	public void applyParameters()
	{
		if (paramsPanel == null)
			return;
		paramsPanel.updateModelParameters();
	}
	
	/**
	 * //TODO make this...
	 * Sooner or later this will host a set of information about the model that the
	 * user can see at a glance. Things like species name, number and names of 
	 * metabolites, number and names of reactions, formulae, etc. should be here.
	 * <p>
	 * Right now, it's not done.
	 * 
	 * @author Bill Riehl briehl@bu.edu
	 *
	 */
	@SuppressWarnings("unused")
	private class ModelInfoPanel extends JPanel
	{

		/**
		 * 
		 */
		private static final long serialVersionUID = -5975621566079865933L;
		
	}

	/**
	 * A specialized inner class that displays parameters for a user to manipulate. 
	 * @author Bill Riehl briehl@bu.edu
	 */
	private class ModelParametersPanel extends JPanel
	{
		/**
		 * 
		 */
		private static final long serialVersionUID = -6609420365194597487L;
		/**
		 * 
		 */
		private int objRxnIndex,
				    objStyle;
		private JComboBox rxnNamesBox;
		private ButtonGroup fbaObjGroup;
		private FBAModelOld model;
		private JRadioButton maxObjButton, 
							 minObjButton,
							 maxFluxButton,
							 minFluxButton,
							 maxObjMinFluxButton,
							 minObjMinFluxButton,
							 maxObjMaxFluxButton,
							 minObjMaxFluxButton;
		private DoubleField flowConstField,
							growthConstField;
		
		public ModelParametersPanel(FBAModelOld model)
		{
			super();
			this.model = model;
			
			/* Parameters to make.
			 * 1. objective reaction to use
			 * 2. objective type to use
			 * 3.... others to come. let's just make this work tonight.
			 */
			
			objRxnIndex = model.getObjectiveIndex();
			String[] rxns = model.getReactionNames();
			rxnNamesBox = new JComboBox(rxns);
			rxnNamesBox.setSelectedIndex(objRxnIndex-1);
			
			objStyle = model.getObjectiveStyle();
			
			fbaObjGroup = new ButtonGroup();
			maxObjButton = new JRadioButton("Maximize objective reaction");
			minObjButton = new JRadioButton("Minimize objective reaction");
			maxFluxButton = new JRadioButton("Maximize total flux");
			minFluxButton = new JRadioButton("Minimize total flux");
			maxObjMinFluxButton = new JRadioButton("Max objective / Min flux");
			minObjMinFluxButton = new JRadioButton("Min objective / Min flux");
			maxObjMaxFluxButton = new JRadioButton("Max objective / Max flux");
			minObjMaxFluxButton = new JRadioButton("Min objective / Max flux");
			
			maxObjMaxFluxButton.setEnabled(false);
			minObjMaxFluxButton.setEnabled(false);
			
			JLabel flowConstLabel = new JLabel("Flow diffusion constant (cm^2/s): ", JLabel.LEFT); 
			flowConstField = new DoubleField(model.getFlowDiffusionConstant(), 6, false);
			JLabel growthConstLabel = new JLabel("Growth diffusion constant (cm^2/s): ", JLabel.LEFT);
			growthConstField = new DoubleField(model.getGrowthDiffusionConstant(), 6, false);

			fbaObjGroup.add(maxObjButton);
			fbaObjGroup.add(minObjButton);
			fbaObjGroup.add(maxFluxButton);
			fbaObjGroup.add(minFluxButton);
			fbaObjGroup.add(maxObjMinFluxButton);
			fbaObjGroup.add(minObjMinFluxButton);
			fbaObjGroup.add(maxObjMaxFluxButton);
			fbaObjGroup.add(minObjMaxFluxButton);
			
			setSelectedObjectiveButton(objStyle);
			
			GridBagConstraints gbc = new GridBagConstraints();
			setLayout(new GridBagLayout());
			
			gbc.gridx = 0;
			gbc.gridy = 0;
			gbc.gridwidth = 2;
			gbc.anchor = GridBagConstraints.LINE_START;
			add(new JLabel("Objective Reaction"), gbc);
			gbc.gridy++;
			add(rxnNamesBox, gbc);
			gbc.gridy++;
			add(Box.createVerticalStrut(10), gbc);
			gbc.gridy++;
			add(maxObjButton, gbc);
			gbc.gridy++;
			add(minObjButton, gbc);
			gbc.gridy++;
			add(maxFluxButton, gbc);
			gbc.gridy++;
			add(minFluxButton, gbc);
			gbc.gridy++;
			add(maxObjMinFluxButton, gbc);
			gbc.gridy++;
			add(minObjMinFluxButton, gbc);
			gbc.gridy++;
//			add(maxObjMaxFluxButton, gbc);
//			gbc.gridy++;
//			add(minObjMaxFluxButton, gbc);
//			gbc.gridy++;

			gbc.gridwidth = 1;
			add(flowConstLabel, gbc);
			gbc.gridx = 1;
			add(flowConstField, gbc);
			
			gbc.gridy++;
			gbc.gridx = 0;
			add(growthConstLabel, gbc);
			gbc.gridx = 1;
			add(growthConstField, gbc);
		}
		
		public void updateModelParameters()
		{
			model.setObjectiveReaction(rxnNamesBox.getSelectedIndex()+1);
			model.setObjectiveStyle(getSelectedObjectiveStyle());
			model.setGrowthDiffusionConstant(growthConstField.getDoubleValue());
			model.setFlowDiffusionConstant(flowConstField.getDoubleValue());
		}
		
		private void setSelectedObjectiveButton(int objStyle)
		{
			fbaObjGroup.clearSelection();
			switch (objStyle)
			{
				case FBAModelOld.MAXIMIZE_OBJECTIVE_FLUX:
					maxObjButton.setSelected(true);
					break;
				case FBAModelOld.MINIMIZE_OBJECTIVE_FLUX:
					minObjButton.setSelected(true);
					break;
				case FBAModelOld.MAXIMIZE_TOTAL_FLUX:
					maxFluxButton.setSelected(true);
					break;
				case FBAModelOld.MINIMIZE_TOTAL_FLUX:
					minFluxButton.setSelected(true);
					break;
				case FBAModelOld.MAX_OBJECTIVE_MIN_TOTAL:
					maxObjMinFluxButton.setSelected(true);
					break;
				case FBAModelOld.MIN_OBJECTIVE_MIN_TOTAL:
					minObjMinFluxButton.setSelected(true);
					break;
				case FBAModelOld.MAX_OBJECTIVE_MAX_TOTAL:
					maxObjMaxFluxButton.setSelected(true);
					break;
				case FBAModelOld.MIN_OBJECTIVE_MAX_TOTAL:
					minObjMaxFluxButton.setSelected(true);
					break;
			}
		}
		
		private int getSelectedObjectiveStyle()
		{
			if (maxObjButton.isSelected())
				return FBAModelOld.MAXIMIZE_OBJECTIVE_FLUX;
			else if (minObjButton.isSelected())
				return FBAModelOld.MINIMIZE_OBJECTIVE_FLUX;
			else if (maxFluxButton.isSelected())
				return FBAModelOld.MAXIMIZE_TOTAL_FLUX;
			else if (minFluxButton.isSelected())
				return FBAModelOld.MINIMIZE_TOTAL_FLUX;
			else if (maxObjMinFluxButton.isSelected())
				return FBAModelOld.MAX_OBJECTIVE_MIN_TOTAL;
			else if (minObjMinFluxButton.isSelected())
				return FBAModelOld.MIN_OBJECTIVE_MIN_TOTAL;
			else if (maxObjMaxFluxButton.isSelected())
				return FBAModelOld.MAX_OBJECTIVE_MAX_TOTAL;
			else if (minObjMaxFluxButton.isSelected())
				return FBAModelOld.MIN_OBJECTIVE_MAX_TOTAL;
			else
				return FBAModelOld.MAXIMIZE_OBJECTIVE_FLUX;
		}
	}

	/** Set the active parameter to true (activate the model) with 
	 * an exponential probability with given activation probability rate.
	 * @param activationRate is the activation probability rate. 
	 */
	
	public boolean activate(double activationRate)
	{
		if(active!=true)
		{
			Random random = new Random();
		    double r = random.nextDouble();
		    if(r<activationRate)
		    {
		    	active=true;
		    }
		}
		
		return active;
		
	}
	
	/** Get the value of active.
	 * 
	 */
 
	public boolean getActive()
	{
		return active;
	}
		
	/**
	 * Some testing and trial code for this thing.
	 * @param args
	 */
	public static void main(String[] args)
	{
		try
		{
//			FBAModel model = FBAModel.loadModelFromFile("C:/!COMETS/ecoli_testing/ec_iAF1260.txt");
			FBAModelOld model = FBAModelOld.loadModelFromFile("C:/Documents and Settings/briehl.FLUX4/Desktop/omics.mod");
			
			System.out.println("loaded!");
			model.setSolverMethod(SIMPLEX_METHOD);
//			model.printMatrix();
//			model.printMSAMatrix();
//			model.printColumn(model.getObjectiveIndex());

			double[] exchLB = new double[model.getBaseExchLowerBounds().length];
//			exchLB[0] = -100;
//			exchLB[1] = -5;
//			exchLB[2] = -50;
//			exchLB[3] = -50;
//			exchLB[4] = -50;
//			exchLB[5] = -10;
//			exchLB[6] = -10;
			
			// M9 media for E. coli, glucose and O2 limited
//			exchLB[59]  = -1000;
//			exchLB[61]  = -0.01;
//			exchLB[66]  = -1000;
//			exchLB[68]  = -1000;
//			exchLB[69]  = -1000;
//			exchLB[75]  = -1000;
//			exchLB[107] = -1000;
//			exchLB[108] = -1000;
//			exchLB[143] = -8;
//			exchLB[166] = -1000;
//			exchLB[168] = -1000;
//			exchLB[185] = -1000;
//			exchLB[210] = -1000;
//			exchLB[213] = -1000;
//			exchLB[215] = -1000;
//			exchLB[218] = -1000;
//			exchLB[220] = -1000;
//			exchLB[227] = -18.5;
//			exchLB[238] = -1000;
//			exchLB[259] = -1000;
//			exchLB[279] = -1000;
//			exchLB[298] = -1000;

			model.setExchLowerBounds(exchLB);
			exchLB = model.getMediaConditions();
			double[] exchUB = model.getBaseExchUpperBounds();
			String[] names = model.getExchangeReactionNames();
			for (int i=0; i<names.length; i++)
			{
				System.out.println(names[i] + "\t" + exchLB[i] + "\t" + exchUB[i]);
			}
			
			long t = System.currentTimeMillis();
			int stat = 0;

			stat = model.run();
			System.out.println("model run: " + (System.currentTimeMillis() - t));
			System.out.println("fba status: " + stat);
			System.out.println("fba objective: " + model.getObjectiveFluxSolution());

//			stat = model.runForceFlux(805, model.getObjectiveIndex(), 1);
//			System.out.println("fba status: " + stat);
//			System.out.println("fba objective: " + model.getObjectiveFluxSolution());
			
//			double[] fluxes = model.getFluxes();
//			model.setObjectiveStyle(FBAModel.MINIMIZE_TOTAL_FLUX);
//			
//			stat = model.run();
//			System.out.println("fba_mav status: " + stat);
//			double[] msaFluxes = model.getFluxes();
//			double totalStd = 0, 
//				   totalMSA = 0;
//			for (int i=0; i<fluxes.length; i++)
//			{
//				totalStd += Math.abs(fluxes[i]);
//				totalMSA += Math.abs(msaFluxes[i]);
//				System.out.println(fluxes[i]);
//			}
//			System.out.println("\n" + totalStd + "\t" + totalMSA);
			System.out.println("took " + (System.currentTimeMillis() - t) + " ms");
		}
		catch (Exception e)
		{
			System.out.println(e);
		}
	}

}