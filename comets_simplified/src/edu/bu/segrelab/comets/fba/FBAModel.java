package edu.bu.segrelab.comets.fba;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
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

import org.apache.commons.lang3.ArrayUtils;

import edu.bu.segrelab.comets.exception.ModelFileException;
import edu.bu.segrelab.comets.ui.DoubleField;

// import org.apache.commons.math3.distribution.*;

// import com.sun.xml.internal.ws.util.StringUtils; \\ Unused and causing couldnæt find package

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
 * 
 * @author Bill Riehl briehl@bu.edu; Ilija Dukovski ilija.dukovski@gmail.com
 * created 3 Mar 2010, modified 11 Mar 2014
 */
public class FBAModel extends edu.bu.segrelab.comets.Model
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
	
	private int numRxns;
	private int numMetabs;
	private int numExch;
	private boolean runSuccess;
	
	// evolution related fields 
	private String modelID; // DJORDJE 
	private String ancestor; // DJORDJE 
	private String mutation;	
	
	private int[] exch; // indices of exchange fluxes.
	// As in the GLPK model idiom,
	// these go from 1 -> n, not 0 -> n-1
	private double[] exchDiffConsts; // diffusion constants of extracellular metabolites
	private String[] exchRxnNames;
	private String[] exchMetabNames;
	private String[] rxnNames;
	private String[] metabNames;
	private double[] baseLB; // the base lower bounds that were originally loaded.
	private double[] baseUB; // base upper bounds (as a backup of sorts).
	private double[] baseExchLB;
	private double[] baseExchUB;
	private double[] exchKm;		  // each of these three arrays is applied to the exchange
	private double[] exchVmax;		  // reactions (for now). Eventually, this might move to the
	private double[] exchHillCoeff;   // transporters.
	
	private double[] exchAlpha;	  // another option for creating exchange reactions:
	private double[] exchW; 		  // defined as min(alpha[i] * media[i], W[i] * volume) / biomass
									  // not as "exact" as the kinetic constraints, but still time-independent
	private double[] lightAbsorption; // Absorption coefficients (default 0), also used to know which metabolites / 
									  // exchange reactions that take up light, because they have to be treated differently from normal metabolites 
	
	private double lightAbsSurfaceToWeight;
	private double flowDiffConst; // = 1e-5;
	private double growthDiffConst; // = 5e-5;
	private double elasticModulusConst;
	private double frictionConst;
	private double packedDensity;
	private double convectionDiffConst;
	private double convNonlinDiffZero;
	private double convNonlinDiffN;
	private double convNonlinDiffExponent;
	private double convNonlinDiffHillK;
	private double convNonlinDiffHillN;
	private double noiseVariance;
	private int[] objReactions; //index of reactions, in order of priority
	private boolean[] objMaximize; //is corresponding objective maximized? If not, it's minimized
	private int biomassReaction;
	private int objStyle;
	
	private double defaultLB = 0,
				   defaultUB = 0,
				   defaultKm = -1, //if kinetic params are <0, use the default in the Package Params
				   defaultVmax = -1,
				   defaultHill = -1,
				   defaultAlpha = -1,
				   defaultW = -1,
				   defaultMetabDiffConst = 0, 
				   genomeCost = 0;
	
	private boolean active;     // true is model is active growing, if false the model is asleep 

	public static final int MAXIMIZE_OBJECTIVE_FLUX = 0;
	public static final int MINIMIZE_OBJECTIVE_FLUX = 1;
	public static final int MAXIMIZE_TOTAL_FLUX = 2;
	public static final int MINIMIZE_TOTAL_FLUX = 3;
	public static final int MAX_OBJECTIVE_MIN_TOTAL = 4;
	public static final int MAX_OBJECTIVE_MAX_TOTAL = 5; 
	public static final int MIN_OBJECTIVE_MIN_TOTAL = 6;
	public static final int MIN_OBJECTIVE_MAX_TOTAL = 7;
	
	private FBAOptimizer fbaOptimizer;
	//private int optimizer;
	
	public static final int GUROBI =0;
	public static final int GLPK   =1;
	
	
	private ModelParametersPanel paramsPanel;
	
	
	private double neutralDriftSigma=0.01;
	private boolean neutralDrift=false;
	//The Distributions are moved to FBACell since they depend on the biomass in a cell. 
	//private PoissonDistribution poissonDist;
	//private GammaDistribution gammaDist;

	/**
	 * Create a new simple FBAModel without any stoichiometry information loaded.
	 */
	private FBAModel()
	{
		runSuccess = false;
		objStyle = MAXIMIZE_OBJECTIVE_FLUX;		 // so maximize by default
		//optimizer = GUROBI;
	}


	/**
	 * Create a new FBAModel with stoichiometric matrix m, lower bounds l, upper bounds u,
	 * and objective/biomass reaction r and optimizer optim. Note that, as we're using GLPK,
	 * r is from 1->N, not * 0->N-1.
	 * @param m
	 * @param l
	 * @param u
	 * @param r
	 * @param optim
	 */
	public FBAModel(double[][] m, double[] l, double[] u, int r, int optim)
	{
		this(m, l, u, new int[] {Math.abs(r)}, new boolean[] {r>=0}, Math.abs(r), optim);
	}
	
	/**
	 * Create a new FBAModel with stoichiometric matrix m, lower bounds l, upper bounds u,
	 * objective reaction r, biomass reaction b and optimizer optim. Note that, as we're 
	 * using GLPK, r is from 1->N, not * 0->N-1.
	 * @param m
	 * @param l
	 * @param u
	 * @param r
	 * @param b
	 * @param optim
	 */
	public FBAModel(double[][] m, double[] l, double[] u, int[] objs, boolean[] objsMax, int b, int optim)
	{
		runSuccess = false;
		objStyle = MAX_OBJECTIVE_MIN_TOTAL;
	
		switch(optim){
		case GUROBI:
			fbaOptimizer=new FBAOptimizerGurobi(m, l, u, objs, objsMax);
			break;
		case GLPK:
			fbaOptimizer=new FBAOptimizerGLPK(m,l,u,objs);
		default:
			break;
		}
		
		Double mtb = m[m.length-1][0];
		numMetabs = mtb.intValue();
		
		numRxns = 1;
		for (int i = 0; i < m.length; i++) {
            if (m[i][1] > numRxns) {
            	Double k = m[i][1];
                numRxns = k.intValue();
            }
		}

		setBaseBounds(l, u);
		setObjectiveReactions(objs);
		setObjectiveMaximize(objsMax);
		setBiomassReaction(b);
	}


	/**
	 * Create a new FBAModel with all the trimmings.
	 * @param m Stoichiometric matrix
	 * @param l lower bounds
	 * @param u upper bounds
	 * @param r objective reaction (remember 1->N!)
	 * @param b biomass reaction (remember 1->N)
	 * @param exch indices of exchange reactions
	 * @param exchDiffConsts diffusion constants of all extracellular metabolites
	 * @param exchKm Michaelis constant of each exchange reaction
	 * @param exchVmax Vmax for each exchange reaction (Michaelis-Menten style)
	 * @param exchHillCoeff Hill coefficient for each exchange reaction (Monod style)
	 * @param metabNames array of metabolite names
	 * @param rxnNames array of reaction names
	 * @param optim optimizer
	 */
	public FBAModel(final double[][] m, 
					final double[] l, 
					final double[] u, 
					int[] r,
					boolean[] objMax,
					int b,
					final int[] exch, 
					final double[] exchDiffConsts,
					final double[] exchKm, 
					final double[] exchVmax, 
					final double[] exchHillCoeff,
					final double[] exchAlpha,
					final double[] exchW,
					final double[] lightAbsorption,
					final String[] metabNames, 
					final String[] rxnNames,
					final int objStyle,
					final int optim)
	{
		this(m, l, u, r, objMax, b, optim);
		
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
		//this.optimizer = optim;

		this.metabNames = metabNames;
		this.rxnNames = rxnNames;
		this.numExch = exch.length;
		exchRxnNames = new String[numExch];
		exchMetabNames = new String[numExch];
		
		int[] exch_tmp = exch.clone();
		int cnt = 0;

		for (int i=0; i<m.length; i++)
		{		
			/* look for reaction in exch_tmp; if found, remove from exch_tmp 
			 * and add rxn and metab to their respective arrays
			 */			
			Double curr_rxn = m[i][1];
			Double curr_mtb = m[i][0];
			
			int is_exch = Arrays.binarySearch(exch_tmp, curr_rxn.intValue());
			if (is_exch >= 0)
			{
				int rxn = curr_rxn.intValue();
				int mtb = curr_mtb.intValue();
				// ArrayUtils.removeElement(exch_tmp, rxn);
				exchRxnNames[is_exch] = rxnNames[rxn-1];
				exchMetabNames[is_exch] = metabNames[mtb-1];
				cnt++;
			}				
		}
		
		this.lightAbsorption = lightAbsorption;

		baseExchLB = new double[numExch];
		baseExchUB = new double[numExch];
		for (int i = 0; i < numExch; i++)
		{
			baseExchLB[i] = baseLB[exch[i]-1];
			baseExchUB[i] = baseUB[exch[i]-1];
		}				
	}
	
	/**Constructor added to make the biomass reaction optional. If not given,
	 * the primary objective reaction will be used to calculate biomass flux
	 * @param m Stoichiometric matrix
	 * @param l lower bounds
	 * @param u upper bounds
	 * @param r objective reaction (remember 1->N!)
	 * @param exch indices of exchange reactions
	 * @param exchDiffConsts diffusion constants of all extracellular metabolites
	 * @param exchKm Michaelis constant of each exchange reaction
	 * @param exchVmax Vmax for each exchange reaction (Michaelis-Menten style)
	 * @param exchHillCoeff Hill coefficient for each exchange reaction (Monod style)
	 * @param exchAlpha
	 * @param exchW
	 * @param lightAbsorption
	 * @param metabNames array of metabolite names
	 * @param rxnNames array of reaction names
	 * @param optim optimizer
	 */
	public FBAModel(final double[][] m, 
					final double[] l, 
					final double[] u, 
					int[] r,
					boolean[] objMax,
					final int[] exch, 
					final double[] exchDiffConsts,
					final double[] exchKm, 
					final double[] exchVmax, 
					final double[] exchHillCoeff,
					final double[] exchAlpha,
					final double[] exchW,
					final double[] lightAbsorption,
					final String[] metabNames, 
					final String[] rxnNames,
					final int objStyle,
					final int optim){
		this(m,l,u,r,objMax,r[0],exch,exchDiffConsts,exchKm,exchVmax,exchHillCoeff,exchAlpha,
				exchW,lightAbsorption,metabNames,rxnNames,objStyle,optim);
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
		this.defaultKm = defKm;
	}
	
	public void setDefaultVmax(double defVmax)
	{
		this.defaultVmax = defVmax;
	}
	
	public void setDefaultHill(double defHill)
	{
		this.defaultHill = defHill;
	}
	
	public void setDefaultAlpha(double defAlpha)
	{
		this.defaultAlpha = defAlpha;
	}
	
	public void setDefaultW(double defW)
	{
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
	
	/**
	 * Replaces negative values in the KM vector with the appropriate
	 * default value, first checking the FBAModel's default then the
	 * FBAParamters's default
	 * @return The Michaelis constants for each exchange reaction
	 */
	public double[] getExchangeKmWithDefaults()
	{
		double[] res = new double[exchKm.length];
		for (int i = 0; i < exchKm.length; i++) {
			res[i] = getExchangeKm(i);
		}
		return res;
	}
	
	/**
	 * 
	 * @param i index of the exchange reaction
	 * @return the Michaelis constant for the specified reaction
	 */
	public double getExchangeKm(int i) {
		double d = exchKm[i];
		if (d < 0) { //use Model's default
			d = defaultKm;
			if (d < 0) { //use Package default
				d = FBAParameters.getDefaultKm();
			}
		}
		return d;
	}
	
	public void setExchangeKm(final double[] exchKm)
	{
		if (this.numExch == exchKm.length)
			this.exchKm = exchKm;
	}
	
	/**
	 * @return the Michaelis-Menten Vmax values for each exchange reaction
	 */
	public double[] getExchangeVmax()
	{
		return exchVmax;
	}
	
	/**
	 * Replaces negative values in the Vmax vector with the appropriate
	 * default value, first checking the FBAModel's default then the
	 * FBAParamters's default
	 * @return The Vmax for each exchange reaction
	 */
	public double[] getExchangeVmaxWithDefaults()
	{
		double[] res = new double[exchVmax.length];
		for (int i = 0; i < exchVmax.length; i++) {
			res[i] = getExchangeVmax(i);
		}
		return res;
	}
	
	/**
	 * 
	 * @param i index of the exchange reaction
	 * @return the Michaelis-Menten Vmax for the specified reaction
	 */
	public double getExchangeVmax(int i) {
		double d = exchVmax[i];
		if (d < 0) { //use Model's default
			d = defaultVmax;
			if (d < 0) { //use Package default
				d = FBAParameters.getDefaultVmax();
			}
		}
		return d;
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
	
	/**
	 * Replaces negative values in the Hill vector with the appropriate
	 * default value, first checking the FBAModel's default then the
	 * FBAParamters's default
	 * @return The Hill Coefficient for each exchange reaction
	 */
	public double[] getExchangeHillCoefficientsWithDefaults()
	{
		double[] res = new double[exchHillCoeff.length];
		for (int i = 0; i < exchHillCoeff.length; i++) {
			res[i] = getExchangeHillCoefficient(i);
		}
		return res;
	}
	
	/**
	 * 
	 * @param i index of the exchange reaction
	 * @return the Hill coefficient for the specified reaction
	 */
	public double getExchangeHillCoefficient(int i) {
		double d = exchHillCoeff[i];
		if (d < 0) { //use Model's default
			d = defaultHill;
			if (d < 0) { //use Package default
				d = FBAParameters.getDefaultHill();
			}
		}
		return d;
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
	
	/**
	 * Replaces negative values in the Alpha vector with the appropriate
	 * default value, first checking the FBAModel's default then the
	 * FBAParamters's default
	 * @return The Michaelis constants for each exchange reaction
	 */
	public double[] getExchangeAlphaCoefficientsWithDefaults()
	{
		double[] res = new double[exchAlpha.length];
		for (int i = 0; i < exchAlpha.length; i++) {
			res[i] = getExchangeAlphaCoefficient(i);
		}
		return res;
	}
	
	/**
	 * 
	 * @param i index of the exchange reaction
	 * @return the Alpha coefficient for the specified reaction
	 */
	public double getExchangeAlphaCoefficient(int i) {
		double d = exchAlpha[i];
		if (d < 0) { //use Model's default
			d = defaultAlpha;
			if (d < 0) { //use Package default
				d = FBAParameters.getDefaultAlpha();
			}
		}
		return d;
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
	
	/**
	 * Replaces negative values in the W vector with the appropriate
	 * default value, first checking the FBAModel's default then the
	 * FBAParamters's default
	 * @return The W coefficients for each exchange reaction
	 */
	public double[] getExchangeWCoefficientsWithDefaults()
	{
		double[] res = new double[exchW.length];
		for (int i = 0; i < exchW.length; i++) {
			res[i] = getExchangeWCoefficient(i);
		}
		return res;
	}
	
	/**
	 * 
	 * @param i index of the exchange reaction
	 * @return the W coefficient for the specified reaction
	 */
	public double getExchangeWCoefficient(int i) {
		double d = exchW[i];
		if (d < 0) { //use Model's default
			d = defaultW;
			if (d < 0) { //use Package default
				d = FBAParameters.getDefaultW();
			}
		}
		return d;
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
	
////
	/**
	 * @return the light absorption coefficient for each exchange reaction
	 */
	public double[] getLightAbsorption()
	{
		return lightAbsorption;
	}
	
	/**
	 * 
	 * @param i index of the exchange reaction
	 * @return the light absorption coefficient for the specified reaction
	 */
	public double getLightAbsorption(int i) {
		return lightAbsorption[i];
	}
	
	public void setLightAbsorption(final double[] lightAbsorption)
	{
		if (this.numExch == lightAbsorption.length)
			this.lightAbsorption = lightAbsorption;
	}
	
	/**
	 * 
	 * @param lightAbsSurfaceToWeight The ratio between the light-absorbing surface and the dry weight of a cell 
	 */
	public void setLightAbsSurfaceToWeight(double lightAbsSurfaceToWeight)
	{
		this.lightAbsSurfaceToWeight = lightAbsSurfaceToWeight;
	}
	
	/**
	 * @return the light Light-absorbing surface-to-dry-weight ratio
	 */
	public double getLightAbsSurfaceToWeight()
	{
		return lightAbsSurfaceToWeight;
	}
	
////
	

	/**
	 * @return the model's name
	 * Legacy from old glpk code
	 */

	public String getModelName()
	{
		//return GLPK.glp_get_prob_name(lp);
		return null;
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

	public synchronized double[] getBaseExchLowerBounds()
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
		
		fbaOptimizer.setExchLowerBounds(exch, lb);	
		
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
		
		fbaOptimizer.setExchUpperBounds(exch, ub);

		return PARAMS_OK;
	}

	/**
	 * @return the array of lower bounds for all fluxes.
	 */
	
	public double[] getLowerBounds()
	{   

		double[] l = new double[numRxns];
		l=fbaOptimizer.getLowerBounds(numRxns);

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
		fbaOptimizer.setUpperBounds(numRxns, ub);

		return PARAMS_OK;
	}

	/**
	 * @return the current upper bounds for all fluxes
	 */

	public double[] getUpperBounds()
	{
		double[] u = new double[numRxns];
		u=fbaOptimizer.getUpperBounds(numRxns);
		
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
/*		if (objStyle != MAXIMIZE_TOTAL_FLUX &&
			objStyle != MINIMIZE_TOTAL_FLUX)
			setObjectiveReaction(objReaction);
*/
		return PARAMS_OK;
	}
	
	public int getObjectiveStyle() 
	{ 
		return objStyle; 
	}
	
	public int[] getObjectiveIndexes()
	{
		return objReactions;
	}
	
	/**Returns the index (1 thru N) of the primary objective reaction
	 * 
	 * @return
	 */
	public int getObjectiveIndex() {
		return objReactions[0];
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
		fbaOptimizer.setObjectiveReaction(numRxns, r);
		objReactions = new int[] {r};
		
		return PARAMS_OK;
	}
	
	public int setObjectiveReactions(int[] objs) {
		objReactions = objs;
		return fbaOptimizer.setObjectiveReaction(numRxns, objs);
	}

	public int setObjectiveMaximize(boolean[] objMax) {
		objMaximize = objMax;
		return fbaOptimizer.setObjectiveMaximize(objMax);
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
		ret=fbaOptimizer.run(objStyle);

		if (ret == 5)
		{   
			//rxnFluxes=model.getVars();
			runSuccess = true;
		}
        return ret;
	}

	// evolution related getters and setters
	public String getModelID()
	{
		return modelID; 
	}
	
	public void setModelID(String model_id)
	{
		this.modelID = model_id;
	}
	
	public String getAncestor()
	{
		return ancestor; 
	}

	public void setAncestor(String ancestor_id)
	{
		this.ancestor = ancestor_id;
	}
	
	public void setMutation(String mutation)
	{
		this.mutation = mutation;
	}
	
	public String getMutation()
	{
		return mutation; 
	}
	
	/**
	 * @return The fluxes from the most recent FBA run
	 */
	public double[] getFluxes()
	{

		double[] v = new double[numRxns];
		if (runSuccess)
		{
			v=fbaOptimizer.getFluxes();

		}
		return v;
	}

	/* Debug code	
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
	 */
	
	public double[] getExchangeFluxes()
	{
		double[] v = new double[numExch];
		if (runSuccess)
		{
			v=fbaOptimizer.getExchangeFluxes(exch);
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
	 * If the FBA run was successful (as denoted by the GLPK status code), this returns
	 * the value of the objective solutions. Otherwise, it returns -Double.MAX_VALUE
	 * @return either the objective solution of -Double.MAX_VALUE
	 */
	public double[] getObjectiveSolutions()
	{
		return fbaOptimizer.getObjectiveSolutions(objReactions);
	}
	
	/**
	 * If the FBA run was successful (as denoted by the GLPK status code), this returns
	 * the value of the flux of the objective reaction. Otherwise, it returns 
	 * -Double.MAX_VALUE. Note that this is a specific flux, and not necessarily the
	 * objective solution, which may be a linear combination of many fluxes
	 * @return
	 */
	public double[] getObjectiveFluxSolution()
	{
		return fbaOptimizer.getObjectiveSolutions(objReactions);
	}
	
	public double getBiomassFluxSolution()
	{
		/*TODO: The function this is calling refers to "Objective" in its name, but 
		 * it appears that it can be used to refer to any reaction. Fix this if that's
		 * not the case, or rename the function to getFluxSolution if I've got it right
		 * -MQuintin 12/1/2016
		 */
		return fbaOptimizer.getObjectiveSolution(biomassReaction);
	}
	
	/**
	 * Returns the status of FBA (feasible or infeasible).
	 * @return
	 */
	public int getFBAstatus()
	{
		return fbaOptimizer.getFBAstatus();
	}

	/**
	 * New and improved (hopefully) file loader.
	 * With a reasonable format, similar to what's used for loading world layouts
	 * 
	 */
	public static FBAModel loadModelFromFile(String filename) throws ModelFileException
	{
		try
		{
			int lineNum = 0;
			BufferedReader reader = new BufferedReader(new FileReader(filename));
			int lines_sparse_s = 0;
			BufferedReader reader_2 = new BufferedReader(new FileReader(filename));
			int numMets = 0;  // number of rows in S-matrix
			int numRxns = 0;  // number of cols in S-matrix
			double[][] S = null;
			double[] lb = null;
			double[] ub = null;
			int[] objs = {0};
			boolean[] objMax = {true};
			int bio = 0;
			int objSt = MAXIMIZE_OBJECTIVE_FLUX;
			int optim = GUROBI;
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
			double[] lightAbsorption = null;
			
			double defaultAlpha = -1,
				   defaultW = -1,
				   defaultKm = -1,
				   defaultVmax = -1,
				   defaultHill = -1,
				   defaultLB = -1000,
				   defaultUB = 1000,
				   defaultDiff = 1e-6,
				   elasticModulusConst=1,
				   frictionConst=1,
				   convDiffConst=1,
				   convNonlinDiffZero=1,
				   convNonlinDiffN=1,
				   convNonlinDiffExponent=1,
				   convNonlinDiffHillN=10,
				   convNonlinDiffHillK=0.9,
				   packDensity=1,
				   noiseVariance=0.0,
				   neutralDriftSigma=0.0,
				   lightAbsSurfaceToWeight=1.0; // m^2/gDW
			
			boolean blockOpen = false;

			boolean neutralDrift = false;

			// First, identify lines where S matrix starts and ends, to code it as a sparse matrix 			
			String line_2 = null;
			while ((line_2 = reader_2.readLine()) != null)
			{
				lines_sparse_s++;
				if (line_2.contains("BOUNDS")) {
					break;
				}				
			}
			lines_sparse_s = lines_sparse_s-3;
			reader_2.close();
			
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
					S = new double[lines_sparse_s][3];
					
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
						
						S[lineNum-1][0] = x;
						S[lineNum-1][1] = y;
						S[lineNum-1][2] = stoic;

						lineNum++;

					}
					lineNum++;
					
					blockOpen = false;
					// done!
					// System.out.println("number of rows of S is " + S.length);
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
						//if (parsed.length != 1) {
						//	reader.close();
						//	throw new ModelFileException("There should be just 1 element for the objective line - the index of the reaction.");
						//}
						
						int[] rxns = new int[parsed.length];
						boolean[] maxs = new boolean[parsed.length];
						for (int i = 0; i < parsed.length; i++) {
							String s = parsed[i];
							int val = Integer.parseInt(s);
							val = Math.abs(val); //input may have a negative value to indicate minimizing. Turn it into an index.
							boolean max = !s.contains("-"); //can't use "val<0" because that would miss "-0"
													//Update: Recall that these indexes are 1->N, so there shouldn't be a 0
							rxns[i] = val;
							maxs[i] = max;
						}
						
						objs = rxns;
						objMax = maxs;
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
				 *************** LOAD BIOMASS REACTION ************************
				 **************************************************************/
				else if (tokens[0].equalsIgnoreCase("BIOMASS"))
				{
					if (numRxns <= 0) {
						reader.close();
						throw new ModelFileException("The stoichiometric matrix should be loaded before the biomass reaction at line " + lineNum);
					}

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
							throw new ModelFileException("There should be just 1 element for the biomass line - the index of the reaction.");
						}
						
						int rxn = Integer.parseInt(parsed[0]);
						if (rxn < 1 || rxn > numRxns) {
							reader.close();
							throw new ModelFileException("The reaction index in BIOMASS block line " + lineNum + " should be between 1 and " + numRxns);
						}
						
						bio = rxn;
					}
					lineNum++;
					blockOpen = false;
				}
				
				/**************************************************************
				 **************** LOAD OPTIMIZER ******************************
				 **************************************************************/
				else if (tokens[0].equalsIgnoreCase("OPTIMIZER"))
				{
					if (tokens.length != 2)
					{
						reader.close();
						throw new ModelFileException("The OPTIMIZER should be followed only by the optimizer value. " + lineNum);
					}
					optim=-1;
					if(tokens[1].equalsIgnoreCase("GUROBI"))
						optim= GUROBI;
					else if(tokens[1].equalsIgnoreCase("GLPK"))
						optim= GLPK;
				}
				
				/**************************************************************
				 **************** LOAD NEUTRALDRIFT BOOLEAN *******************
				 **************************************************************/
				else if (tokens[0].equalsIgnoreCase("neutralDrift"))
				{
					if (tokens.length != 2)
					{
						reader.close();
						throw new ModelFileException("The neutralDrift should be followed only by the value true or false at line " + lineNum);
					}
					neutralDrift = Boolean.parseBoolean(tokens[1]);
					//System.out.println(tokens[1]);
					//System.out.println(neutralDrift);
					//if ( neutralDrift != true && neutralDrift != false)
					//{
					//	reader.close();
					//	throw new ModelFileException("The neutral drift value given at line " + lineNum + "should be boolean, true or false.");
					//}
					
				}
				
				/**************************************************************
				 **************** LOAD NEUTRALDRIFTSIGMA VALUE ****************
				 **************************************************************/
				else if (tokens[0].equalsIgnoreCase("neutralDriftSigma"))
				{
					if (tokens.length != 2)
					{
						reader.close();
						throw new ModelFileException("The neutralDriftSigma should be followed only by the value at line " + lineNum);
					}
					neutralDriftSigma = Double.parseDouble(tokens[1]);
					if ( neutralDriftSigma <=0)
					{
						reader.close();
						throw new ModelFileException("The neutral drift sigma value given at line " + lineNum + "should be positive.");
					}
					
				}
				
				/**************************************************************
				 **************** LOAD ELASTIC MODULUS ************************
				 **************************************************************/
				else if (tokens[0].equalsIgnoreCase("elasticModulus"))
				{
					if (tokens.length != 2)
					{
						reader.close();
						throw new ModelFileException("The ElasticModulus should be followed only by the modulus value at line " + lineNum);
					}
					elasticModulusConst = Double.parseDouble(tokens[1]);
					if (elasticModulusConst < 0)
					{
						reader.close();
						throw new ModelFileException("The elastic modulus value given at line " + lineNum + "should be => 0");
					}
					
				}
				
				/**************************************************************
				 **************** LOAD PACKED DENSITY ************************
				 **************************************************************/
				else if (tokens[0].equalsIgnoreCase("packedDensity"))
				{
					if (tokens.length != 2)
					{
						reader.close();
						throw new ModelFileException("The packedDensity should be followed only by the value at line " + lineNum);
					}
					packDensity = Double.parseDouble(tokens[1]);
					if (packDensity < 0)
					{
						reader.close();
						throw new ModelFileException("The packedDensity value given at line " + lineNum + "should be > 0");
					}
					
				}
				
				/**************************************************************
				 **************** LOAD NOISE VARIANCE ************************
				 **************************************************************/
				else if (tokens[0].equalsIgnoreCase("noiseVariance"))
				{
					if (tokens.length != 2)
					{
						reader.close();
						throw new ModelFileException("The noiseVariance should be followed only by the value at line " + lineNum);
					}
					noiseVariance = Double.parseDouble(tokens[1]);
					if (noiseVariance < 0)
					{
						reader.close();
						throw new ModelFileException("The noiseVariance value given at line " + lineNum + "should be => 0");
					}
					
				}
				/**************************************************************
				 **************** LOAD FRICTION CONSTANT **********************
				 **************************************************************/
				else if (tokens[0].equalsIgnoreCase("frictionConstant"))
				{
					if (tokens.length != 2)
					{
						reader.close();
						throw new ModelFileException("The FrictionConstant should be followed only by its value at line " + lineNum);
					}
					frictionConst = Double.parseDouble(tokens[1]);
					if (frictionConst <= 0)
					{
						reader.close();
						throw new ModelFileException("The frictionConstant value given at line " + lineNum + "should be > 0");
					}
					
				}
				
				/**************************************************************
				 **************** LOAD DIFFUSION CONSTANT (CONVECTION MODEL)***
				 **************************************************************/
				else if (tokens[0].equalsIgnoreCase("convDiffConstant"))
				{
					if (tokens.length != 2)
					{
						reader.close();
						throw new ModelFileException("The convDiffConstant should be followed only by its value at line " + lineNum);
					}
					convDiffConst = Double.parseDouble(tokens[1]);
					if (convDiffConst < 0)
					{
						reader.close();
						throw new ModelFileException("The convDiffConstant value given at line " + lineNum + "should be => 0");
					}
					
				}
				
				/**************************************************************
				 *******LOAD Nonlinear DIFFUSION CONSTANT (CONVECTION MODEL)***
				 **************************************************************/
				else if (tokens[0].equalsIgnoreCase("convNonlinDiffZero"))
				{
					if (tokens.length != 2)
					{
						reader.close();
						throw new ModelFileException("The convNonlinDiffZero should be followed only by its value at line " + lineNum);
					}
					convNonlinDiffZero = Double.parseDouble(tokens[1]);
					if (convDiffConst < 0)
					{
						reader.close();
						throw new ModelFileException("The convNonlinDiffZero value given at line " + lineNum + "should be => 0");
					}
					
				}
				
				/**************************************************************
				 *******LOAD Nonlinear DIFFUSION CONSTANT (CONVECTION MODEL)***
				 **************************************************************/
				else if (tokens[0].equalsIgnoreCase("convNonlinDiffN"))
				{
					if (tokens.length != 2)
					{
						reader.close();
						throw new ModelFileException("The convNonlinDiffN should be followed only by its value at line " + lineNum);
					}
					convNonlinDiffN = Double.parseDouble(tokens[1]);
					if (convDiffConst < 0)
					{
						reader.close();
						throw new ModelFileException("The convNonlinDiffN value given at line " + lineNum + "should be => 0");
					}
					
				}
				
				/**************************************************************
				 *******LOAD Nonlinear DIFFUSION EXPONENT (CONVECTION MODEL)***
				 **************************************************************/
				else if (tokens[0].equalsIgnoreCase("convNonlinDiffExponent"))
				{
					if (tokens.length != 2)
					{
						reader.close();
						throw new ModelFileException("The convNonlinDiffExponent should be followed only by its value at line " + lineNum);
					}
					convNonlinDiffExponent = Double.parseDouble(tokens[1]);
					if (convDiffConst < 0)
					{
						reader.close();
						throw new ModelFileException("The convNonlinDiffExponent value given at line " + lineNum + "should be => 0");
					}
					
				}
				
				/**************************************************************
				 *******LOAD Nonlinear DIFFUSION HILL K (CONVECTION MODEL)***
				 **************************************************************/
				else if (tokens[0].equalsIgnoreCase("convNonlinDiffHillK"))
				{
					if (tokens.length != 2)
					{
						reader.close();
						throw new ModelFileException("The convNonlinDiffHillK should be followed only by its value at line " + lineNum);
					}
					convNonlinDiffHillK = Double.parseDouble(tokens[1]);
					if (convNonlinDiffHillK < 0)
					{
						reader.close();
						throw new ModelFileException("The convNonlinDiffHillK value given at line " + lineNum + "should be => 0");
					}
					
				}
				
				/**************************************************************
				 *******LOAD Nonlinear DIFFUSION HILL N (CONVECTION MODEL)***
				 **************************************************************/
				else if (tokens[0].equalsIgnoreCase("convNonlinDiffHillN"))
				{
					if (tokens.length != 2)
					{
						reader.close();
						throw new ModelFileException("The convNonlinDiffHillN should be followed only by its value at line " + lineNum);
					}
					convNonlinDiffHillN = Double.parseDouble(tokens[1]);
					if (convNonlinDiffHillN < 0)
					{
						reader.close();
						throw new ModelFileException("The convNonlinDiffHillN value given at line " + lineNum + "should be => 0");
					}
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
						exchAlpha[i] = -1; //when -1 is found here, the code should lookup the model's defaultAlpha
											//we don't just set it now because it may be changed by the user
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
						exchW[i] = -1;
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
						exchKm[i] = -1;
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
						exchVmax[i] = -1;
						
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
						exchHillCoeff[i] = -1;
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
				
				/**************************************************************
				 ******************* LOAD NEUTRAL DRIFT PARAMETER *******************
				 **************************************************************/
				else if (tokens[0].equalsIgnoreCase("neutralDriftParameter"))
				{
					if (tokens.length != 2)
					{
						reader.close();
						throw new ModelFileException("The neutralDriftParameter should be followed only by its value at line " + lineNum);
					}
					neutralDriftSigma = Double.parseDouble(tokens[1]);
					if (neutralDriftSigma < 0)
					{
						reader.close();
						throw new ModelFileException("The neutralDriftSigma value given at line " + lineNum + "should be => 0");
					}
					
				}
				/**************************************************************
				 ******************* LOAD LIGHT PARAMETERS *******************
				 **************************************************************/
				else if (tokens[0].equalsIgnoreCase("LIGHT"))
				{
					if (numRxns <= 0)
					{
						reader.close();
						throw new ModelFileException("The stoichiometric matrix should be loaded before the Light coefficients at line " + lineNum);
					}
					if (exchRxns == null)
					{
						reader.close();
						throw new ModelFileException("The list of exchange reactions should be loaded before the Light coefficients at line " + lineNum);
					}
					if (tokens.length != 2)
					{
						reader.close();
						throw new ModelFileException("The LIGHT parameter at line " + lineNum + " should be followed by its surface to weight ratio in m^2 per gDW");
					}
					
					lightAbsSurfaceToWeight = Double.parseDouble(tokens[1]);
					lightAbsorption = new double[numExch];
					for (int i=0; i<numExch; i++)
						lightAbsorption[i] = 0;
						
					String lightLine = null;
					blockOpen = true;
					while (!(lightLine = reader.readLine().trim()).equalsIgnoreCase("//"))
					{
						lineNum++;
						String[] parsed = lightLine.split("\\s+");
						if (lightLine.length() == 0)
							continue;
						if (parsed.length != 2)
						{
							reader.close();
							throw new ModelFileException("There should be 2 elements on each line of the LIGHT block at line " + lineNum + ": the exchange reaction index (from 1 to " + numExch + ") and the absorption coefficient of that reaction.");
						}
						
						int rxn = Integer.parseInt(parsed[0]);
						if (rxn < 1 || rxn > numExch)
						{
							reader.close();
							throw new ModelFileException("The reaction index in LIGHT block line " + lineNum + " should be between 1 and " + numExch);
						}
						
						double absorption = Double.parseDouble(parsed[1]);
						if (absorption < 0 || absorption > 1)
						{
							reader.close();
							throw new ModelFileException("The absorption value on line " + lineNum + " should be between 0 and 1");
						}
						
						lightAbsorption[rxn-1] = absorption;
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
			
			if (lightAbsorption == null)
			{
				lightAbsorption = new double[numExch];
				for (int i=0; i<numExch; i++)
					lightAbsorption[i] = 0;
				lightAbsSurfaceToWeight = 0;
			}
			
		
			if (bio == 0){ //if the Biomass reaction wasn't specified, use the primary Objective reaction
				bio = objs[0];
			}
			
			FBAModel model = new FBAModel(S, lb, ub, objs, objMax, bio, exchRxns, diffConsts, exchKm, exchVmax, exchHillCoeff, exchAlpha, exchW, lightAbsorption, metNames, rxnNames, objSt, optim);
			model.setDefaultAlpha(defaultAlpha);
			model.setDefaultW(defaultW);
			model.setDefaultHill(defaultHill);
			model.setDefaultKm(defaultKm);
			model.setDefaultVmax(defaultVmax);
			model.setDefaultLB(defaultLB);
			model.setDefaultUB(defaultUB);
			model.setDefaultMetabDiffConst(defaultDiff);
			model.setElasticModulusConstant(elasticModulusConst);
			model.setFrictionConstant(frictionConst);
			model.setConvDiffConstant(convDiffConst);
			model.setConvNonlinDiffZero(convNonlinDiffZero);
			model.setConvNonlinDiffN(convNonlinDiffN);
			model.setConvNonlinDiffHillK(convNonlinDiffHillK);
			model.setConvNonlinDiffHillN(convNonlinDiffHillN);
			model.setConvNonlinDiffExponent(convNonlinDiffExponent);
			model.setPackedDensity(packDensity);
			model.setNoiseVariance(noiseVariance);
			model.setLightAbsSurfaceToWeight(lightAbsSurfaceToWeight);
			
			model.setNeutralDrift(neutralDrift);
			model.setNeutralDriftSigma(neutralDriftSigma);
			
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
	 * A debug tool that prints the upper and lower bounds to System.out
	 */
/*	
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
*/	
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
	 * Returns the value of packedDensity in g/cm^2 or g/cm^3
	 * @return
	 */
	public double getPackedDensity()
	{
		return packedDensity;
	}
	
	/**
	 * returns the value of packedDensity in g/cm^2 or g/cm^3
	 * @return
	 */
	public void setPackedDensity(double density)
	{
		packedDensity=density;
	}
	
	/**
	 * @return the value of the elastic modulus constant in Pa
	 */
	public double getElasticModulusConstant()
	{
		return elasticModulusConst;
	}
	
	/**
	 * Sets the elastic modulus constant in Pa
	 */
	public void setElasticModulusConstant(double val)
	{
		elasticModulusConst=val;
	}
	
	/**
	 * @return the value of the diffusion constant for the convection model in cm^2/s
	 */
	public double getConvDiffConstant()
	{
		return convectionDiffConst;
	}
	
	/**
	 * Sets the elastic modulus constant in Pa
	 */
	public void setConvDiffConstant(double val)
	{
		convectionDiffConst=val;;
	}
	
	/**
	 * @return the value of the nonlinear diffusion constant D0 in (D0+DN*rho^N) for the convection model in cm^2/s
	 */
	public double getConvNonlinDiffZero()
	{
		return convNonlinDiffZero;
	}
	
	/**
	 * Sets the Diffusion constant D0 in (D0+DN*rho^N) for the convection model.
	 */
	public void setConvNonlinDiffZero(double val)
	{
		convNonlinDiffZero=val;;
	}
	
	/**
	 * @return the value of the nonlinear diffusion constant DN in (D0+DN*rho^N) for the convection model in cm^2/s
	 */
	public double getConvNonlinDiffN()
	{
		return convNonlinDiffN;
	}
	
	/**
	 * Sets the Diffusion constant DN in (D0+DN*rho^N) for the convection model.
	 */
	public void setConvNonlinDiffN(double val)
	{
		convNonlinDiffN=val;;
	}
	
	/**
	 * @return the value of the nonlinear diffusion exponent N in (D0+DN*rho^N) for the convection model in cm^2/s
	 */
	public double getConvNonlinDiffExponent()
	{
		return convNonlinDiffExponent;
	}
	
	/**
	 * Sets the Diffusion constant N in (D0+DN*rho^N) for the convection model.
	 */
	public void setConvNonlinDiffExponent(double val)
	{
		convNonlinDiffExponent=val;;
	}
	
	/**
	 * @return the value of K in the nonlinear diffusion Hill step function (dRho/Rho)^N/(K^N+(dRho/Rho)^N)
	 */
	public double getConvNonlinDiffHillK()
	{
		return convNonlinDiffHillK;
	}
	
	/**
	 * Sets the value of K in the nonlinear diffusion Hill step function (dRho/Rho)^N/(K^N+(dRho/Rho)^N)
	 */
	public void setConvNonlinDiffHillK(double val)
	{
		convNonlinDiffHillK=val;;
	}
	
	/**
	 * @return the value of N in the nonlinear diffusion Hill step function (dRho/Rho)^N/(K^N+(dRho/Rho)^N)
	 */
	public double getConvNonlinDiffHillN()
	{
		return convNonlinDiffHillN;
	}
	
	/**
	 * Sets the value of N in the nonlinear diffusion Hill step function (dRho/Rho)^N/(K^N+(dRho/Rho)^N)
	 */
	public void setConvNonlinDiffHillN(double val)
	{
		convNonlinDiffHillN=val;;
	}
	
	
	public double getNoiseVariance()
	{
		return noiseVariance;
	}
	
	public void setNoiseVariance(double variance)
	{
		noiseVariance=variance;
	}
	/**
	 * @return the value of the friction constant  in cm^/s
	 */
	public double getFrictionConstant()
	{
		return frictionConst;
	}
	
	/**
	 * Sets the elastic modulus constant in Pa
	 */
	public void setFrictionConstant(double val)
	{
		frictionConst=val;;
	}
	
	/**
	 * @return the value of the neutral drift constant.
	 */
	public double getNeutralDriftSigma()
	{
		return neutralDriftSigma;
	}
	
	/**
	 * Sets the value of the neutral drift constant.
	 */
	public void setNeutralDriftSigma(double val)
	{
		neutralDriftSigma=val;
	}
	
	/**
	 * @return the value of the neutral drift boolean.
	 */
	public boolean getNeutralDrift()
	{
		return neutralDrift;
	}
	
	/**
	 * Sets the value of the neutral drift boolean.
	 */
	public void setNeutralDrift(boolean val)
	{
		neutralDrift=val;;
	}

	//public void setFluxesModel(double[] fl)
	//{
	//	fluxesModel=fl;
	//}
	
	//public double[] getFluxesModel()
	//{
	//	return fluxesModel;
	//}
	/**
	 * Sets the upper bound on all objective reactions. 
	 * @param ub
	 * @return PARAMS_ERROR if ub < the current lb for the objective, PARAMS_OK otherwise
	 */
	public int setObjectiveUpperBound(double ub)
	{
		int res = PARAMS_OK;
		for (int objReaction : objReactions) {
			int t = fbaOptimizer.setObjectiveUpperBound(objReaction, ub);
			if (t != PARAMS_OK) res = PARAMS_ERROR;
		}
		return res;
	}
	
	/**Set the upper bounds of the objective reactions to the values in the given list.
	 * If the length of ub < N_Objectives, unpaired objectives will not have bounds set.
	 * 
	 * @param ub
	 * @return
	 */
	public int setObjectiveUpperBounds(double[] ub) {
		int res = PARAMS_OK;
		for (int i = 0; i < ub.length; i++){
			int t = fbaOptimizer.setObjectiveUpperBound(objReactions[i], ub[i]);
			if (t != PARAMS_OK) res = PARAMS_ERROR;
		}
		return res;
	}
	
	/**
	 * Sets the upper bound on the biomass reaction. 
	 * @param ub
	 * @return PARAMS_ERROR if ub < the current lb for the objective, PARAMS_OK otherwise
	 */
	public int setBiomassUpperBound(double ub)
	{
		return fbaOptimizer.setObjectiveUpperBound(biomassReaction, ub);
	}
	
	/**
	 * Sets the lower bound on all objective reactions. 
	 * @param lb
	 * @return PARAMS_ERROR if lb > the current ub for the objective, PARAMS_OK otherwise
	 */
	
	public int setObjectiveLowerBound(double lb)
	{
		int res = PARAMS_OK;
		for (int objReaction : objReactions) {
			int t = fbaOptimizer.setObjectiveLowerBound(objReaction, lb);
			if (t != PARAMS_OK) res = PARAMS_ERROR;
		}
		return res;
	}
	
	/**Set the lower bounds of the objective reactions to the values in the given list.
	 * If the length of lb < N_Objectives, unpaired objectives will not have bounds set.
	 * 
	 * @param lb
	 * @return
	 */
	public int setObjectiveLowerBounds(double[] lb) {
		int res = PARAMS_OK;
		for (int i = 0; i < lb.length; i++){
			int t = fbaOptimizer.setObjectiveLowerBound(objReactions[i], lb[i]);
			if (t != PARAMS_OK) res = PARAMS_ERROR;
		}
		return res;
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
	

	private void setNums(int numMetabs, int numRxns, int numExch)
	{
		//this.lp = lp;
		//this.lpMSA = lpMSA;
		this.numMetabs = numMetabs;
		this.numRxns = numRxns;
		this.numExch = numExch;
	}
	
	/**
	 * Produces a clone of this <code>FBAModel</code> with all parameters intact.
	 */
	
	public FBAModel clone()
	{
		FBAModel modelCopy=new FBAModel();
		modelCopy.setNums(numMetabs, numRxns, numExch);
		modelCopy.fbaOptimizer=fbaOptimizer.clone();
		
		modelCopy.setBaseBounds(getBaseLowerBounds(), getBaseUpperBounds());
		modelCopy.setBaseExchLowerBounds(getBaseExchLowerBounds());
		modelCopy.setBaseExchUpperBounds(getBaseExchUpperBounds());
		modelCopy.setObjectiveReactions(getObjectiveIndexes());
		modelCopy.setBiomassReaction(getBiomassReaction());
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
		modelCopy.setActive(getActive());
		modelCopy.setObjectiveStyle(getObjectiveStyle());
		modelCopy.setFileName(this.getFileName());
		modelCopy.setElasticModulusConstant(getElasticModulusConstant());
		modelCopy.setFrictionConstant(getFrictionConstant());
		modelCopy.setConvDiffConstant(getConvDiffConstant());
		modelCopy.setPackedDensity(getPackedDensity());
		modelCopy.setNoiseVariance(getNoiseVariance());
		modelCopy.setLightAbsorption(getLightAbsorption());
		modelCopy.setLightAbsSurfaceToWeight(getLightAbsSurfaceToWeight());
		
		//modelCopy.setParameters();
		
		return modelCopy;
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
		private FBAModel model;
		private JRadioButton maxObjButton, 
							 minObjButton,
							 maxFluxButton,
							 minFluxButton,
							 maxObjMinFluxButton,
							 minObjMinFluxButton,
							 maxObjMaxFluxButton,
							 minObjMaxFluxButton;
		private DoubleField flowConstField,
							growthConstField,
							elasticModulusField,
							frictionConstField,
		                    convDiffConstField,
		                    packedDensityField,
		                    noiseVarianceField;
		//private JComboBox   optimizerBox;
		
		public ModelParametersPanel(FBAModel model)
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
			JLabel elasticModulusLabel = new JLabel("Elastic modulus constant (Pa): ", JLabel.LEFT);
			elasticModulusField = new DoubleField(model.getElasticModulusConstant(), 6, false);
			JLabel frictionConstLabel = new JLabel("Friction constant (cm^2/s): ", JLabel.LEFT);
			frictionConstField = new DoubleField(model.getFrictionConstant(), 6, false);
			JLabel convDiffConstLabel = new JLabel("Conv. model diffusion constant (cm^2/s): ", JLabel.LEFT);
			convDiffConstField = new DoubleField(model.getConvDiffConstant(), 6, false);
			JLabel packedDensityLabel = new JLabel("Packed Density (g/cm^2) or (g/cm^3): ", JLabel.LEFT);
			packedDensityField = new DoubleField(model.getPackedDensity(), 6, false);
			JLabel noiseVarianceLabel = new JLabel("Noise variance: ", JLabel.LEFT);
			noiseVarianceField = new DoubleField(model.getNoiseVariance(), 6, false);

			
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
			
			
			gbc.gridy++;
			gbc.gridwidth = 1;
			add(flowConstLabel, gbc);
			gbc.gridx = 1;
			add(flowConstField, gbc);
			
			gbc.gridy++;
			gbc.gridx = 0;
			add(growthConstLabel, gbc);
			gbc.gridx = 1;
			add(growthConstField, gbc);
			
			gbc.gridy++;
			gbc.gridx = 0;
			add(elasticModulusLabel, gbc);
			gbc.gridx = 1;
			add(elasticModulusField, gbc);
			
			gbc.gridy++;
			gbc.gridx = 0;
			add(frictionConstLabel, gbc);
			gbc.gridx = 1;
			add(frictionConstField, gbc);
			
			gbc.gridy++;
			gbc.gridx = 0;
			add(convDiffConstLabel, gbc);
			gbc.gridx = 1;
			add(convDiffConstField, gbc);
			
			gbc.gridy++;
			gbc.gridx = 0;
			add(packedDensityLabel, gbc);
			gbc.gridx = 1;
			add(packedDensityField, gbc);
			
			gbc.gridy++;
			gbc.gridx = 0;
			add(noiseVarianceLabel, gbc);
			gbc.gridx = 1;
			add(noiseVarianceField, gbc);
		}
		
		public void updateModelParameters()
		{
			model.setObjectiveReaction(rxnNamesBox.getSelectedIndex()+1);
			model.setObjectiveStyle(getSelectedObjectiveStyle());
			model.setGrowthDiffusionConstant(growthConstField.getDoubleValue());
			model.setFlowDiffusionConstant(flowConstField.getDoubleValue());
			model.setElasticModulusConstant(elasticModulusField.getDoubleValue());
			model.setFrictionConstant(frictionConstField.getDoubleValue());
			model.setConvDiffConstant(convDiffConstField.getDoubleValue());
			model.setPackedDensity(packedDensityField.getDoubleValue());
			model.setNoiseVariance(noiseVarianceField.getDoubleValue());
		}
		
		private void setSelectedObjectiveButton(int objStyle)
		{
			fbaObjGroup.clearSelection();
			switch (objStyle)
			{
				case FBAModel.MAXIMIZE_OBJECTIVE_FLUX:
					maxObjButton.setSelected(true);
					break;
				case FBAModel.MINIMIZE_OBJECTIVE_FLUX:
					minObjButton.setSelected(true);
					break;
				case FBAModel.MAXIMIZE_TOTAL_FLUX:
					maxFluxButton.setSelected(true);
					break;
				case FBAModel.MINIMIZE_TOTAL_FLUX:
					minFluxButton.setSelected(true);
					break;
				case FBAModel.MAX_OBJECTIVE_MIN_TOTAL:
					maxObjMinFluxButton.setSelected(true);
					break;
				case FBAModel.MIN_OBJECTIVE_MIN_TOTAL:
					minObjMinFluxButton.setSelected(true);
					break;
				case FBAModel.MAX_OBJECTIVE_MAX_TOTAL:
					maxObjMaxFluxButton.setSelected(true);
					break;
				case FBAModel.MIN_OBJECTIVE_MAX_TOTAL:
					minObjMaxFluxButton.setSelected(true);
					break;
			}
		}
		
		private int getSelectedObjectiveStyle()
		{
			if (maxObjButton.isSelected())
				return FBAModel.MAXIMIZE_OBJECTIVE_FLUX;
			else if (minObjButton.isSelected())
				return FBAModel.MINIMIZE_OBJECTIVE_FLUX;
			else if (maxFluxButton.isSelected())
				return FBAModel.MAXIMIZE_TOTAL_FLUX;
			else if (minFluxButton.isSelected())
				return FBAModel.MINIMIZE_TOTAL_FLUX;
			else if (maxObjMinFluxButton.isSelected())
				return FBAModel.MAX_OBJECTIVE_MIN_TOTAL;
			else if (minObjMinFluxButton.isSelected())
				return FBAModel.MIN_OBJECTIVE_MIN_TOTAL;
			else if (maxObjMaxFluxButton.isSelected())
				return FBAModel.MAX_OBJECTIVE_MAX_TOTAL;
			else if (minObjMaxFluxButton.isSelected())
				return FBAModel.MIN_OBJECTIVE_MAX_TOTAL;
			else
				return FBAModel.MAXIMIZE_OBJECTIVE_FLUX;
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
	
	/** Get the value of active.
	 * 
	 */
 
	public void setActive(boolean act)
	{
		active=act;
	}
	
	/**
	 * @return the biomassReaction
	 */
	public int getBiomassReaction() {
		return biomassReaction;
	}

	/**
	 * @param biomassReaction the biomassReaction to set
	 */
	public void setBiomassReaction(int biomassReaction)
	{
		this.biomassReaction = biomassReaction;
	}

	/**
	 * Mutation method (this is for deletions only) 
	 */
	public void mutateModel()
	{		
		double[] lBounds = getBaseLowerBounds();
		double[] uBounds = getBaseUpperBounds();
		
		// figure out which reactions have nonzero bounds
		ArrayList<Integer> nonzeroRxns = new ArrayList<Integer>();
		for (int j = 0; j < lBounds.length; j++) {
			if ((lBounds[j] != 0 || uBounds[j] != 0) && !(ArrayUtils.contains(exch, j)))
				nonzeroRxns.add(j);
		}
		
		// select randomly one of these reactions
		int mutReaction = nonzeroRxns.get(new Random().nextInt(nonzeroRxns.size()));
		setMutation("del_" + Integer.toString(mutReaction));
		//System.out.println("mutated reaction: " + mutReaction);
		
		// and update the mutModel model bounds
		lBounds[mutReaction] = 0;
		uBounds[mutReaction] = 0;
		setBaseLowerBounds(lBounds);
		setBaseUpperBounds(uBounds);	
		//JEAN make sure newbounds apply to the optimizer
		fbaOptimizer.setLowerBounds(lBounds.length, lBounds);
		fbaOptimizer.setUpperBounds(uBounds.length, uBounds);

	}
	
	/**
	 * Reaction addition method. 
	 * - Only adds those reactions present in the model with zero bounds.
	 * - Done similarly to mutateModel, but performing the reverse
	 */
	public void addReactionToModel()
	{		
		double[] lBounds = getBaseLowerBounds();
		double[] uBounds = getBaseUpperBounds();
		
		// figure out which reactions have zero bounds
		ArrayList<Integer> nonzeroRxns = new ArrayList<Integer>();
		for (int j = 0; j < lBounds.length; j++) {
			if ((lBounds[j] == 0 && uBounds[j] == 0) && !(ArrayUtils.contains(exch, j)))
				nonzeroRxns.add(j);
		}
		
		// select one of these reactions at random
		int mutReaction = nonzeroRxns.get(new Random().nextInt(nonzeroRxns.size()));
		setMutation("add_" + Integer.toString(mutReaction));
		
		// and update the mutModel model bounds
		uBounds[mutReaction] = 1000;
		setBaseUpperBounds(uBounds);
		//JEAN make sure new bounds apply to the optimizer
		fbaOptimizer.setLowerBounds(lBounds.length, lBounds);
		fbaOptimizer.setUpperBounds(uBounds.length, uBounds);		
	}
	
	public double getGenomeCost()
	{
		return genomeCost;
	}
	
	public void setGenomeCost(double ind_frac_cost)
	{	
		//Jean Updated so that cost is quadratic (see ranea et al 2005)
		double[] lBounds = getBaseLowerBounds();
		double[] uBounds = getBaseUpperBounds();
		
		// figure out how many reactions have nonzero bounds
		int num_reactions = 0;
		for (int j = 0; j < lBounds.length; j++) {
			if (lBounds[j] != 0 || uBounds[j] != 0)
				num_reactions ++;			
		}
		genomeCost = num_reactions * num_reactions * ind_frac_cost;		
	}
}
