package edu.bu.segrelab.comets.fba;

import com.google.ortools.Loader;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPObjective;
import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPSolverParameters;
import com.google.ortools.linearsolver.MPVariable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

/**
 * This class provides a ManyLP-style optimizer for the FBAModel class.
 * It extends the abstract FBAOptimizer class.
 * <p>
 * The method follows the certify-and-repair approach of manylp
 * (F. Cumbo, "manylp: reproducible, exact, and batched linear programming for
 * genome-scale dynamic flux balance analysis", 2026; https://github.com/cumbof/manylp).
 * All the LPs that COMETS solves for one species share the stoichiometric matrix and
 * the objective and differ only in the flux bounds. An optimal basis stays dual feasible
 * when only the bounds change, so a cached basis certifies a new LP if the basic
 * variables it implies stay within the new bounds. In that case the optimal vertex is
 * obtained with one sparse triangular solve instead of a simplex run.
 * <p>
 * Each LP is handled as follows:
 * <ol>
 * <li>Certify with the basis that solved the previous LP of this optimizer (warm).
 * <li>Certify with the most recently used bases of the shared basis pool (pool).
 *     The pool is shared by all clones of the optimizer, i.e. by all threads and
 *     all mutants of the same species, since they share the matrix and the objective.
 * <li>If no cached basis certifies the LP, solve it with the GLOP dual simplex (repair),
 *     verify the returned basis independently in double precision, and add it to the pool.
 * </ol>
 * The certification checks, per LP, (i) that every nonbasic variable can sit at a
 * finite bound consistent with the sign of its reduced cost (dual feasibility), and
 * (ii) that the basic variables respect their bounds (primal feasibility), plus a
 * residual check of S v = 0. Together these are the KKT conditions, so a certified
 * vertex is an optimal solution of the LP.
 * <p>
 * The structural constraints S v = 0 are written with row logicals s = S v, so the
 * standard form is [S, -I] z = 0 with z = (v, s), and all variation between LPs is in
 * the bounds of z (the logicals are fixed at 0).
 * <p>
 * Parsimonious FBA (MAX_OBJECTIVE_MIN_TOTAL) is solved as a second bounds-parametric LP
 * family: v = p - q with p, q &gt;= 0, minimize sum(p + q) subject to S p - S q = 0,
 * with the objective reaction fixed at its optimal value. The bounds of p and q are
 * p in [max(l,0), max(u,0)] and q in [max(-u,0), max(-l,0)], so this second family
 * has its own basis pool and is certified the same way.
 * <p>
 * Usage is the same as for the other optimizers:
 * <ol>
 * <li>Create the optimizer with the constructor.
 * <li>Set the lower bounds of the exchange fluxes with setExchLowerBounds(int[],double[]).
 * <li>Run the optimizer with run(int).
 * <li>Fetch the results with getFluxes(), getExchangeFluxes(), getObjectiveSolution() etc.
 * </ol>
 * <p>
 * Infeasible LPs: the bound boxes that GLOP proved infeasible are cached, and an LP whose
 * bounds lie inside one of them is reported infeasible without a simplex run (in dFBA
 * a starving cell's bounds only shrink, so this catches most repeated infeasible LPs).
 * <p>
 * Differences from the Python manylp: there is no GPU kernel and no batching across
 * cells (FBAModel solves one LP per call), the infeasibility cache uses bound boxes
 * instead of Farkas rays, there is no persistent basis atlas, and no uniqueness
 * certificate. Only the first objective reaction is used, as in FBAOptimizerGlop.
 * <p>
 * This class depends on OR-Tools (GLOP) for the repair step, exactly like FBAOptimizerGlop.
 *
 * @author Ilija Dukovski ilija.dukovski@gmail.com
 * created 12 Nov 2026
 */
public class FBAOptimizerManyLP extends edu.bu.segrelab.comets.fba.FBAOptimizer
implements edu.bu.segrelab.comets.CometsConstants
{
	/** Relative primal feasibility tolerance used in the certification. */
	private static final double PRIMAL_TOL = 1e-7;
	/** Reduced costs with absolute value below this are treated as zero (degenerate). */
	private static final double DUAL_TOL = 1e-7;
	/** Maximum allowed residual |S v| of a certified solution (relative). */
	private static final double RESIDUAL_TOL = 1e-6;
	/** Bounds with absolute value at or above this are treated as infinite. */
	private static final double INF_BOUND = 1e30;
	/** Maximum number of bases kept in a shared basis pool. */
	private static final int MAX_BASES = 64;
	/** Maximum number of pool bases tried for one LP before calling the repair solver. */
	private static final int POOL_SCAN = 16;
	/** Maximum number of infeasible bound boxes kept in the infeasibility cache. */
	private static final int MAX_INFEASIBLE_BOXES = 16;

	/** LP solve outcomes. */
	private static final int LP_OPTIMAL = 0;
	private static final int LP_INFEASIBLE = 1;
	private static final int LP_ERROR = 2;

	private static volatile boolean nativeLoaded = false;

	private int numMetabs;
	private int numRxns;

	private double[][] stoichMatrix; // the sparse triplets (metab, rxn, coeff), 1-ordered; kept for clone()
	private int[] sColPtr;           // S in compressed sparse column form, 0-ordered
	private int[] sRowIdx;
	private double[] sVal;

	private double[] lowerBounds;
	private double[] upperBounds;

	private int[] objReactions; //for consistency, this should ALWAYS be 1-ordered!
	private boolean[] objMaximize;
	private int objIndex;            // primary objective reaction, 1-ordered
	private boolean objMax;          // maximize the primary objective?

	private ParametricLP fbaLP;      // max/min c^T v  s.t. S v = 0, l <= v <= u
	private ParametricLP pfbaLP;     // min sum(p+q)  s.t. S p - S q = 0, objective flux fixed

	private double[] fluxes;
	private double[] work;           // structural solution of the FBA LP
	private double[] pWork;          // structural solution of the pFBA LP
	private double[] pLower;
	private double[] pUpper;
	private boolean runSuccess;

	/**
	 * Create a new FBAOptimizerManyLP with stoichiometric matrix m, lower bounds l, upper bounds u,
	 * and objective reactions objIdxs.
	 * @param m Stoichiometric matrix in sparse form: m[k] = {metabolite index (1..M), reaction index (1..N), coefficient}
	 * @param l lower bounds of the reaction fluxes
	 * @param u upper bounds of the reaction fluxes
	 * @param objIdxs list of indexes for objective reactions, from 1 to N, in order of priority with the highest first
	 * @param objMax list signifying if the corresponding objective rxn should be maximized, otherwise it will be minimized
	 */
	public FBAOptimizerManyLP(double[][] m, double[] l, double[] u, int[] objIdxs, boolean[] objMax)
	{
		this(m, l, u, objIdxs, objMax, null);
	}

	/**
	 * Constructor used by clone(): the copy shares the immutable LP structures and the
	 * basis pools of the source optimizer.
	 */
	private FBAOptimizerManyLP(double[][] m, double[] l, double[] u, int[] objIdxs, boolean[] objMaxList,
			FBAOptimizerManyLP source)
	{
		loadNativeLibraries();
		runSuccess = false;

		stoichMatrix = m;
		numRxns = l.length;
		lowerBounds = l.clone();
		upperBounds = u.clone();
		fluxes = new double[numRxns];
		work = new double[numRxns];
		pWork = new double[2 * numRxns];
		pLower = new double[2 * numRxns];
		pUpper = new double[2 * numRxns];

		objReactions = objIdxs;
		objMaximize = objMaxList;
		objIndex = Math.abs(objIdxs[0]);
		objMax = (objMaxList == null || objMaxList.length == 0) ? true : objMaxList[0];

		if (source != null)
		{
			numMetabs = source.numMetabs;
			sColPtr = source.sColPtr;
			sRowIdx = source.sRowIdx;
			sVal = source.sVal;
			// share the structures (and so the basis pools) only if the objective is the same
			if (source.objIndex == objIndex && source.objMax == objMax)
				fbaLP = new ParametricLP(source.fbaLP.structure);
			else
				fbaLP = new ParametricLP(buildFbaStructure());
			pfbaLP = new ParametricLP(source.pfbaLP.structure);
			return;
		}

		sortByColumn(m, 0);     // sort the matrix by ascending metabolite index
		Double mtb = m[m.length-1][0];
		numMetabs = mtb.intValue();
		buildCSC(m);

		fbaLP = new ParametricLP(buildFbaStructure());
		pfbaLP = new ParametricLP(buildPfbaStructure());
	}

	private static void loadNativeLibraries()
	{
		if (!nativeLoaded)
		{
			synchronized (FBAOptimizerManyLP.class)
			{
				if (!nativeLoaded)
				{
					Loader.loadNativeLibraries();
					nativeLoaded = true;
				}
			}
		}
	}

	/**
	 * Builds S in compressed sparse column form from the 1-ordered triplets.
	 * Repeated (metabolite, reaction) entries are summed.
	 */
	private void buildCSC(double[][] m)
	{
		int[] count = new int[numRxns];
		for (int k = 0; k < m.length; k++)
			count[(int)m[k][1] - 1]++;

		int[] ptr = new int[numRxns + 1];
		for (int j = 0; j < numRxns; j++)
			ptr[j+1] = ptr[j] + count[j];

		int[] rows = new int[ptr[numRxns]];
		double[] vals = new double[ptr[numRxns]];
		int[] next = Arrays.copyOf(ptr, numRxns);
		for (int k = 0; k < m.length; k++)
		{
			int j = (int)m[k][1] - 1;
			int i = (int)m[k][0] - 1;
			int dup = -1;
			for (int p = ptr[j]; p < next[j]; p++)
				if (rows[p] == i) { dup = p; break; }
			if (dup >= 0)
				vals[dup] += m[k][2];
			else
			{
				rows[next[j]] = i;
				vals[next[j]] = m[k][2];
				next[j]++;
			}
		}

		// compact, in case there were duplicates, and drop explicit zeros
		sColPtr = new int[numRxns + 1];
		int nnz = 0;
		for (int j = 0; j < numRxns; j++)
		{
			for (int p = ptr[j]; p < next[j]; p++)
			{
				if (vals[p] != 0.0)
				{
					rows[nnz] = rows[p];
					vals[nnz] = vals[p];
					nnz++;
				}
			}
			sColPtr[j+1] = nnz;
		}
		sRowIdx = Arrays.copyOf(rows, nnz);
		sVal = Arrays.copyOf(vals, nnz);
	}

	/** The FBA family: maximize +/- v_obj subject to S v = 0. */
	private LPStructure buildFbaStructure()
	{
		double[] cost = new double[numRxns];
		cost[objIndex - 1] = objMax ? 1.0 : -1.0;
		return new LPStructure(numMetabs, numRxns, sColPtr, sRowIdx, sVal, cost);
	}

	/** The pFBA family: maximize -sum(p+q) subject to S p - S q = 0. */
	private LPStructure buildPfbaStructure()
	{
		int n2 = 2 * numRxns;
		int nnz = sColPtr[numRxns];
		int[] ptr = new int[n2 + 1];
		int[] rows = new int[2 * nnz];
		double[] vals = new double[2 * nnz];
		for (int j = 0; j < numRxns; j++)
			ptr[j+1] = sColPtr[j+1];
		for (int j = 0; j < numRxns; j++)
			ptr[numRxns + j + 1] = nnz + sColPtr[j+1];
		for (int p = 0; p < nnz; p++)
		{
			rows[p] = sRowIdx[p];
			vals[p] = sVal[p];
			rows[nnz + p] = sRowIdx[p];
			vals[nnz + p] = -sVal[p];
		}
		double[] cost = new double[n2];
		Arrays.fill(cost, -1.0);
		return new LPStructure(numMetabs, n2, ptr, rows, vals, cost);
	}

	/**
	 * Performs an FBA run with the loaded model, constraints, and bounds. Fluxes
	 * and solutions are stored in the FBAOptimizerManyLP class and can be used through their
	 * accessory functions.
	 * @return 5 if optimal (legacy GLPK code, as in the other optimizers), 4 if infeasible,
	 * -1 otherwise, or MODEL_NOT_INITIALIZED if there's no problem to solve
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

		if (objSty != FBAModel.MAXIMIZE_OBJECTIVE_FLUX && objSty != FBAModel.MAX_OBJECTIVE_MIN_TOTAL)
		{
			// same as the other optimizers: other objective styles are not implemented
			Arrays.fill(fluxes, 0.0);
			return -1;
		}

		int st = fbaLP.solve(lowerBounds, upperBounds, work);
		if (st == LP_INFEASIBLE)
		{
			Arrays.fill(fluxes, 0.0);
			return 4;
		}
		if (st != LP_OPTIMAL)
		{
			Arrays.fill(fluxes, 0.0);
			return -1;
		}
		System.arraycopy(work, 0, fluxes, 0, numRxns);

		if (objSty == FBAModel.MAX_OBJECTIVE_MIN_TOTAL)
		{
			// Parsimonious FBA: fix the objective flux and minimize the sum of absolute fluxes.
			int n = numRxns;
			for (int j = 0; j < n; j++)
			{
				double lo = lowerBounds[j];
				double hi = upperBounds[j];
				if (j == objIndex - 1)
				{
					lo = work[j];
					hi = work[j];
				}
				pLower[j] = Math.max(lo, 0.0);
				pUpper[j] = Math.max(hi, 0.0);
				pLower[n + j] = Math.max(-hi, 0.0);
				pUpper[n + j] = Math.max(-lo, 0.0);
			}
			int st2 = pfbaLP.solve(pLower, pUpper, pWork);
			//Here we will ignore pFBA if it is infeasible, as in FBAOptimizerGlop.
			if (st2 == LP_OPTIMAL)
			{
				for (int j = 0; j < n; j++)
					fluxes[j] = pWork[j] - pWork[n + j];
				fluxes[objIndex - 1] = work[objIndex - 1];
			}
		}

		runSuccess = true;
		return 5;
	}

	/**
	 * @return The fluxes from the most recent FBA run
	 */
	public double[] getFluxes()
	{
		double[] v = new double[numRxns];
		if (runSuccess)
			System.arraycopy(fluxes, 0, v, 0, numRxns);
		return v;
	}

	/**
	 * @return the exchange fluxes from the most recent FBA run
	 * @param exch list of exchange reactions (1-ordered)
	 */
	public double[] getExchangeFluxes(int[] exch)
	{
		double[] v = new double[exch.length];
		if (runSuccess)
		{
			for (int i = 0; i < exch.length; i++)
				v[i] = fluxes[exch[i]-1];
		}
		return v;
	}

	/**
	 * If the FBA run was successful, this returns
	 * the value of the objective solution. Otherwise, it returns -Double.MAX_VALUE
	 * @return either the objective solution or -Double.MAX_VALUE
	 * @param objreact Objective reaction index (1-ordered).
	 */
	public double getObjectiveSolution(int objreact)
	{
		if (runSuccess)
			return fluxes[objreact-1];
		return -Double.MAX_VALUE;
	}

	public double[] getObjectiveSolutions(int[] objs)
	{
		double[] res = new double[objs.length];
		for (int i = 0; i < objs.length; i++)
			res[i] = getObjectiveSolution(objs[i]);
		return res;
	}

	public double getObjectiveFluxSolution(int objreact)
	{
		return getObjectiveSolution(objreact);
	}

	/**
	 * Returns the status of FBA (feasible or infeasible).
	 * @return 1 if feasible, 0 if not
	 */
	public int getFBAstatus()
	{
		return runSuccess ? 1 : 0;
	}

	/**
	 * Produces a clone of this <code>FBAOptimizerManyLP</code> with all parameters intact.
	 * The clone shares the basis pools of this optimizer, so bases found by one thread or
	 * one mutant are reused by the others.
	 */
	public FBAOptimizerManyLP clone()
	{
		return new FBAOptimizerManyLP(stoichMatrix, lowerBounds, upperBounds, objReactions, objMaximize, this);
	}

	public int setUpperBounds(int nrxns, double[] ub)
	{
		if (ub.length != nrxns || nrxns != numRxns)
			return PARAMS_ERROR;
		System.arraycopy(ub, 0, upperBounds, 0, nrxns);
		return PARAMS_OK;
	}

	public int setLowerBounds(int nrxns, double[] lb)
	{
		if (lb.length != nrxns || nrxns != numRxns)
			return PARAMS_ERROR;
		System.arraycopy(lb, 0, lowerBounds, 0, nrxns);
		return PARAMS_OK;
	}

	public double[] getUpperBounds(int nrxns)
	{
		return upperBounds.clone();
	}

	public double[] getLowerBounds(int nrxns)
	{
		return lowerBounds.clone();
	}

	/**
	 * Sets the current lower bounds (e.g. -uptake rates) for the exchange reactions.
	 * @param exch exchange reaction indices (1-ordered)
	 * @param lb new lower bounds
	 * @return PARAMS_OK if the lb and exch array are of the same length; PARAMS_ERROR if not.
	 */
	public int setExchLowerBounds(int[] exch, double[] lb)
	{
		if (lb.length != exch.length)
			return PARAMS_ERROR;
		for (int i = 0; i < exch.length; i++)
			lowerBounds[exch[i]-1] = lb[i];
		return PARAMS_OK;
	}

	public int setExchUpperBounds(int[] exch, double[] ub)
	{
		if (ub.length != exch.length)
			return PARAMS_ERROR;
		for (int i = 0; i < exch.length; i++)
			upperBounds[exch[i]-1] = ub[i];
		return PARAMS_OK;
	}

	public int setObjectiveUpperBound(int objreact, double ub)
	{
		upperBounds[objreact-1] = ub;
		return PARAMS_OK;
	}

	public int setObjectiveLowerBound(int objreact, double lb)
	{
		lowerBounds[objreact-1] = lb;
		return PARAMS_OK;
	}

	public int setObjectiveReaction(int nrxns, int r)
	{
		return setObjectiveReaction(nrxns, new int[] {r}, new boolean[] {true});
	}

	public int setObjectiveReaction(int nrxns, int[] objs)
	{
		boolean[] max = new boolean[objs.length];
		for (int i = 0; i < objs.length; i++)
			max[i] = (objs[i] >= 0);
		return setObjectiveReaction(nrxns, objs, max);
	}

	/**
	 * Sets the objective reactions. Only the first one is optimized (see the class comment).
	 * Changing the objective discards the FBA basis pool of this optimizer, since the
	 * cached bases are only optimal for the objective they were found with.
	 */
	public int setObjectiveReaction(int nrxns, int[] idxs, boolean[] maximize)
	{
		if (idxs == null || idxs.length == 0)
			return PARAMS_ERROR;
		int[] abs = new int[idxs.length];
		for (int i = 0; i < idxs.length; i++)
		{
			abs[i] = Math.abs(idxs[i]);
			if (abs[i] < 1 || abs[i] > nrxns)
				return PARAMS_ERROR;
		}
		objReactions = abs;
		objMaximize = maximize;
		updatePrimaryObjective(abs[0], maximize == null || maximize.length == 0 ? true : maximize[0]);
		return PARAMS_OK;
	}

	public int setObjectiveMaximize(boolean[] objMax)
	{
		this.objMaximize = objMax;
		if (objMax != null && objMax.length > 0)
			updatePrimaryObjective(objIndex, objMax[0]);
		return PARAMS_OK;
	}

	private void updatePrimaryObjective(int idx, boolean max)
	{
		if (idx == objIndex && max == objMax)
			return;
		objIndex = idx;
		objMax = max;
		fbaLP = new ParametricLP(buildFbaStructure()); // new objective, new basis pool
	}

	/**
	 * @return a short summary of how the LPs of this optimizer were solved
	 */
	public String getStatistics()
	{
		return "FBA: " + fbaLP.statistics() + "; pFBA: " + pfbaLP.statistics();
	}

	/*
	 * method to sort a double 2D array according to one column (used for m) (djordje)
	 */
	private void sortByColumn (double arr[][], int col)
	{
		Arrays.sort(arr, new Comparator <double[]>() {

			@Override
			public int compare(final double[] a, final double[] b) {
				return Double.compare(a[col], b[col]);
			}
		});
	}

	private static boolean isInf(double b)
	{
		return Double.isNaN(b) || Math.abs(b) >= INF_BOUND;
	}

	/* ************************************************************************************
	 * LP family: the immutable part, shared by all clones
	 * ************************************************************************************/

	/**
	 * A family of LPs: maximize cost^T z subject to [A, -I] z = 0, with bounds that vary
	 * between LPs. A has m rows and nStruct columns. Columns nStruct..nStruct+m-1 are the
	 * row logicals, always fixed at 0. Immutable except for the thread-safe basis pool.
	 */
	private static final class LPStructure
	{
		final int m;
		final int nStruct;
		final int[] colPtr;
		final int[] rowIdx;
		final double[] val;
		final double[] cost;
		final BasisPool pool = new BasisPool(MAX_BASES);
		final InfeasibleBoxes infeasible = new InfeasibleBoxes(MAX_INFEASIBLE_BOXES);

		LPStructure(int m, int nStruct, int[] colPtr, int[] rowIdx, double[] val, double[] cost)
		{
			this.m = m;
			this.nStruct = nStruct;
			this.colPtr = colPtr;
			this.rowIdx = rowIdx;
			this.val = val;
			this.cost = cost;
		}

		int nTotal()
		{
			return nStruct + m;
		}
	}

	/* ************************************************************************************
	 * LP family: the per-optimizer part (last basis, repair solver, work arrays)
	 * ************************************************************************************/

	private static final class ParametricLP
	{
		final LPStructure structure;
		private CachedBasis lastBasis;

		// repair solver (GLOP), created at the first repair
		private MPSolver solver;
		private MPVariable[] vars;
		private MPConstraint[] rows;
		private MPSolverParameters params;

		// work arrays
		private final double[] zN;     // values of the nonbasic variables (all columns)
		private final double[] rhs;    // right-hand side for the basic variables
		private final double[] zB;     // basic variable values, by basis position
		private final double[] luWork;

		// statistics
		private long nWarm, nPool, nRepairCertified, nRepairUncertified, nInfeasible, nInfeasibleCached, nError;

		ParametricLP(LPStructure s)
		{
			structure = s;
			zN = new double[s.nTotal()];
			rhs = new double[s.m];
			zB = new double[s.m];
			luWork = new double[s.m];
		}

		/**
		 * Solves the LP with structural bounds lo, hi (length nStruct).
		 * @param out receives the structural part of the solution
		 * @return LP_OPTIMAL, LP_INFEASIBLE or LP_ERROR
		 */
		int solve(double[] lo, double[] hi, double[] out)
		{
			// 1. warm: the basis of the previous LP
			if (lastBasis != null && certify(lastBasis, lo, hi, out))
			{
				nWarm++;
				structure.pool.touch(lastBasis);
				return LP_OPTIMAL;
			}

			// 2. infeasibility cache: a box inside a box known to be infeasible is infeasible
			if (structure.infeasible.contains(lo, hi))
			{
				nInfeasibleCached++;
				return LP_INFEASIBLE;
			}

			// 3. pool: the most recently used bases
			CachedBasis[] candidates = structure.pool.snapshot(POOL_SCAN);
			for (CachedBasis b : candidates)
			{
				if (b == lastBasis)
					continue;
				if (certify(b, lo, hi, out))
				{
					nPool++;
					lastBasis = b;
					structure.pool.touch(b);
					return LP_OPTIMAL;
				}
			}

			// 4. repair: simplex solve, then verify and cache the new basis
			return repair(lo, hi, out);
		}

		/**
		 * Certifies basis b for the bounds lo, hi. On success writes the optimal
		 * structural solution into out and returns true.
		 */
		private boolean certify(CachedBasis b, double[] lo, double[] hi, double[] out)
		{
			final LPStructure s = structure;
			final int m = s.m;
			final int nS = s.nStruct;

			// Place the nonbasic variables at the bound given by the sign of the reduced cost.
			Arrays.fill(rhs, 0.0);
			for (int t = 0; t < b.nonbasic.length; t++)
			{
				int k = b.nonbasic[t];
				double l, u;
				if (k < nS) { l = lo[k]; u = hi[k]; }
				else        { l = 0.0;   u = 0.0;   }
				boolean lInf = isInf(l);
				boolean uInf = isInf(u);
				if (!lInf && !uInf && l > u + PRIMAL_TOL * (1.0 + Math.abs(u)))
					return false; // inconsistent bounds, leave it to the simplex
				double d = b.reducedCost[t];
				double v;
				if (d > DUAL_TOL)
				{
					if (uInf) return false; // would be unbounded in this direction
					v = u;
				}
				else if (d < -DUAL_TOL)
				{
					if (lInf) return false;
					v = l;
				}
				else
				{
					// zero reduced cost: keep the side the basis was found at, if finite
					if (b.atUpper[t] && !uInf) v = u;
					else if (!lInf)           v = l;
					else if (!uInf)           v = u;
					else                      v = 0.0;
				}
				zN[k] = v;
				if (v != 0.0)
				{
					if (k < nS)
					{
						for (int p = s.colPtr[k]; p < s.colPtr[k+1]; p++)
							rhs[s.rowIdx[p]] -= s.val[p] * v;
					}
					else
					{
						rhs[k - nS] += v; // column of the logical is -e_i
					}
				}
			}

			// Basic variables: B zB = -N zN
			b.lu.solve(rhs, zB, luWork);

			for (int p = 0; p < m; p++)
			{
				int k = b.basic[p];
				double v = zB[p];
				if (Double.isNaN(v))
					return false;
				double l, u;
				if (k < nS) { l = lo[k]; u = hi[k]; }
				else        { l = 0.0;   u = 0.0;   }
				if (!isInf(l) && v < l - PRIMAL_TOL * (1.0 + Math.abs(l)))
					return false;
				if (!isInf(u) && v > u + PRIMAL_TOL * (1.0 + Math.abs(u)))
					return false;
				zN[k] = v;
			}

			// Residual check of A v = 0 for the structural part (the logicals are ~0).
			Arrays.fill(rhs, 0.0);
			Arrays.fill(luWork, 0.0); // used as the row scale
			for (int k = 0; k < nS; k++)
			{
				double v = zN[k];
				if (v == 0.0) continue;
				for (int p = s.colPtr[k]; p < s.colPtr[k+1]; p++)
				{
					double t = s.val[p] * v;
					rhs[s.rowIdx[p]] += t;
					luWork[s.rowIdx[p]] += Math.abs(t);
				}
			}
			for (int i = 0; i < m; i++)
			{
				if (Math.abs(rhs[i]) > RESIDUAL_TOL * (1.0 + luWork[i]))
					return false;
			}

			System.arraycopy(zN, 0, out, 0, nS);
			return true;
		}

		/**
		 * Solves the LP with GLOP, verifies the returned basis and adds it to the pool.
		 */
		private int repair(double[] lo, double[] hi, double[] out)
		{
			final LPStructure s = structure;
			final int nS = s.nStruct;
			if (solver == null)
				buildSolver();

			for (int k = 0; k < nS; k++)
			{
				double l = isInf(lo[k]) ? Double.NEGATIVE_INFINITY : lo[k];
				double u = isInf(hi[k]) ? Double.POSITIVE_INFINITY : hi[k];
				vars[k].setBounds(l, u);
			}

			MPSolver.ResultStatus rs = solver.solve(params);
			if (rs == MPSolver.ResultStatus.INFEASIBLE)
			{
				s.infeasible.add(lo, hi);
				nInfeasible++;
				return LP_INFEASIBLE;
			}
			if (rs != MPSolver.ResultStatus.OPTIMAL)
			{
				nError++;
				return LP_ERROR;
			}

			CachedBasis b = CachedBasis.fromSolver(s, vars, rows);
			if (b != null)
			{
				CachedBasis existing = s.pool.find(b);
				if (existing != null)
					b = existing;
				if (certify(b, lo, hi, out))
				{
					if (existing == null)
						s.pool.add(b);
					else
						s.pool.touch(b);
					lastBasis = b;
					nRepairCertified++;
					return LP_OPTIMAL;
				}
			}

			// The basis could not be verified (e.g. tolerances): use the simplex solution as is.
			for (int k = 0; k < nS; k++)
				out[k] = vars[k].solutionValue();
			nRepairUncertified++;
			return LP_OPTIMAL;
		}

		private void buildSolver()
		{
			final LPStructure s = structure;
			solver = MPSolver.createSolver("GLOP");
			vars = new MPVariable[s.nStruct];
			for (int k = 0; k < s.nStruct; k++)
				vars[k] = solver.makeNumVar(0.0, 0.0, "z" + k);
			rows = new MPConstraint[s.m];
			for (int i = 0; i < s.m; i++)
				rows[i] = solver.makeConstraint(0.0, 0.0, "c" + i);
			for (int k = 0; k < s.nStruct; k++)
				for (int p = s.colPtr[k]; p < s.colPtr[k+1]; p++)
					rows[s.rowIdx[p]].setCoefficient(vars[k], s.val[p]);
			MPObjective obj = solver.objective();
			for (int k = 0; k < s.nStruct; k++)
				if (s.cost[k] != 0.0)
					obj.setCoefficient(vars[k], s.cost[k]);
			obj.setMaximization();
			// No presolve: the basis must refer to the original rows and columns,
			// and GLOP then warm starts from the previous basis when only bounds change.
			params = new MPSolverParameters();
			params.setIntegerParam(MPSolverParameters.IntegerParam.PRESOLVE,
					MPSolverParameters.PresolveValues.PRESOLVE_OFF.swigValue());
		}

		String statistics()
		{
			long total = nWarm + nPool + nRepairCertified + nRepairUncertified + nInfeasible + nInfeasibleCached + nError;
			return total + " LPs (warm " + nWarm + ", pool " + nPool + ", repair+cached " + nRepairCertified
					+ ", repair uncertified " + nRepairUncertified + ", infeasible cached " + nInfeasibleCached
					+ ", infeasible simplex " + nInfeasible + ", error " + nError + "), "
					+ structure.pool.size() + " bases in pool";
		}
	}

	/* ************************************************************************************
	 * A verified optimal basis
	 * ************************************************************************************/

	private static final class CachedBasis
	{
		final int[] basic;          // column indices of the basic variables, by basis position
		final int[] nonbasic;       // column indices of the nonbasic variables
		final double[] reducedCost; // reduced costs of the nonbasic variables
		final boolean[] atUpper;    // side of each nonbasic variable when the basis was found
		final SparseLU lu;          // factorization of the basis matrix
		final int hash;

		private CachedBasis(int[] basic, int[] nonbasic, double[] reducedCost, boolean[] atUpper, SparseLU lu)
		{
			this.basic = basic;
			this.nonbasic = nonbasic;
			this.reducedCost = reducedCost;
			this.atUpper = atUpper;
			this.lu = lu;
			this.hash = Arrays.hashCode(basic) * 31 + Arrays.hashCode(atUpper);
		}

		boolean sameAs(CachedBasis o)
		{
			return hash == o.hash && Arrays.equals(basic, o.basic) && Arrays.equals(atUpper, o.atUpper);
		}

		/**
		 * Reads the basis from the solver, factorizes it, and computes the reduced costs.
		 * @return the basis, or null if it is not a valid (square, nonsingular) basis
		 */
		static CachedBasis fromSolver(LPStructure s, MPVariable[] vars, MPConstraint[] rows)
		{
			final int m = s.m;
			final int nS = s.nStruct;
			final int nT = s.nTotal();

			MPSolver.BasisStatus[] status = new MPSolver.BasisStatus[nT];
			int nBasic = 0;
			for (int k = 0; k < nS; k++)
			{
				status[k] = vars[k].basisStatus();
				if (status[k] == MPSolver.BasisStatus.BASIC) nBasic++;
			}
			for (int i = 0; i < m; i++)
			{
				status[nS + i] = rows[i].basisStatus();
				if (status[nS + i] == MPSolver.BasisStatus.BASIC) nBasic++;
			}
			if (nBasic != m)
				return null;

			int[] basic = new int[m];
			int[] nonbasic = new int[nT - m];
			boolean[] atUpper = new boolean[nT - m];
			int nb = 0, nn = 0;
			for (int k = 0; k < nT; k++)
			{
				if (status[k] == MPSolver.BasisStatus.BASIC)
					basic[nb++] = k;
				else
				{
					atUpper[nn] = (status[k] == MPSolver.BasisStatus.AT_UPPER_BOUND);
					nonbasic[nn++] = k;
				}
			}

			// basis matrix columns
			int[][] colRows = new int[m][];
			double[][] colVals = new double[m][];
			for (int p = 0; p < m; p++)
			{
				int k = basic[p];
				if (k < nS)
				{
					int a = s.colPtr[k], e = s.colPtr[k+1];
					colRows[p] = Arrays.copyOfRange(s.rowIdx, a, e);
					colVals[p] = Arrays.copyOfRange(s.val, a, e);
				}
				else
				{
					colRows[p] = new int[] {k - nS};
					colVals[p] = new double[] {-1.0};
				}
			}
			SparseLU lu = SparseLU.factor(m, colRows, colVals);
			if (lu == null)
				return null;

			// reduced costs d = c_N - N^T y, with B^T y = c_B
			double[] cB = new double[m];
			for (int p = 0; p < m; p++)
				cB[p] = basic[p] < nS ? s.cost[basic[p]] : 0.0;
			double[] y = new double[m];
			lu.solveTranspose(cB, y, new double[m]);
			double[] d = new double[nT - m];
			for (int t = 0; t < nonbasic.length; t++)
			{
				int k = nonbasic[t];
				double v;
				if (k < nS)
				{
					v = s.cost[k];
					for (int p = s.colPtr[k]; p < s.colPtr[k+1]; p++)
						v -= s.val[p] * y[s.rowIdx[p]];
				}
				else
				{
					v = y[k - nS]; // 0 - (-e_i)^T y
				}
				if (Double.isNaN(v) || Double.isInfinite(v))
					return null;
				d[t] = v;
			}
			return new CachedBasis(basic, nonbasic, d, atUpper, lu);
		}
	}

	/* ************************************************************************************
	 * Pool of verified bases, most recently used first. Thread safe.
	 * ************************************************************************************/

	private static final class BasisPool
	{
		private final int maxBases;
		private final ArrayList<CachedBasis> list = new ArrayList<CachedBasis>();

		BasisPool(int maxBases)
		{
			this.maxBases = maxBases;
		}

		synchronized CachedBasis[] snapshot(int limit)
		{
			int n = Math.min(limit, list.size());
			CachedBasis[] a = new CachedBasis[n];
			for (int i = 0; i < n; i++)
				a[i] = list.get(i);
			return a;
		}

		synchronized void touch(CachedBasis b)
		{
			if (!list.isEmpty() && list.get(0) == b)
				return;
			for (int i = 1; i < list.size(); i++)
			{
				if (list.get(i) == b)
				{
					list.remove(i);
					list.add(0, b);
					return;
				}
			}
		}

		synchronized CachedBasis find(CachedBasis b)
		{
			for (CachedBasis c : list)
				if (c.sameAs(b))
					return c;
			return null;
		}

		synchronized void add(CachedBasis b)
		{
			list.add(0, b);
			while (list.size() > maxBases)
				list.remove(list.size() - 1);
		}

		synchronized int size()
		{
			return list.size();
		}
	}

	/* ************************************************************************************
	 * Bound boxes for which the simplex proved the LP infeasible. Since the constraints
	 * S v = 0 do not change, any LP whose bounds lie inside such a box is infeasible too.
	 * This is a cheap, exact substitute for the Farkas-ray cache of manylp (GLOP does not
	 * expose the ray through the MPSolver interface). Most recently found first. Thread safe.
	 * ************************************************************************************/

	private static final class InfeasibleBoxes
	{
		private final int maxBoxes;
		private final ArrayList<double[][]> list = new ArrayList<double[][]>();

		InfeasibleBoxes(int maxBoxes)
		{
			this.maxBoxes = maxBoxes;
		}

		synchronized boolean contains(double[] lo, double[] hi)
		{
			for (int b = 0; b < list.size(); b++)
			{
				double[][] box = list.get(b);
				double[] bl = box[0], bu = box[1];
				boolean inside = true;
				for (int k = 0; k < lo.length && inside; k++)
					inside = lo[k] >= bl[k] && hi[k] <= bu[k];
				if (inside)
				{
					if (b > 0)
					{
						list.remove(b);
						list.add(0, box);
					}
					return true;
				}
			}
			return false;
		}

		synchronized void add(double[] lo, double[] hi)
		{
			list.add(0, new double[][] {lo.clone(), hi.clone()});
			while (list.size() > maxBoxes)
				list.remove(list.size() - 1);
		}
	}

	/* ************************************************************************************
	 * Sparse LU factorization of a square matrix, P B Q = L U.
	 * Left-looking Gilbert-Peierls algorithm (as in CSparse cs_lu) with threshold partial
	 * pivoting. Columns are ordered by increasing number of nonzeros, and among the
	 * acceptable pivots the row with the fewest nonzeros is chosen, to limit fill-in.
	 * ************************************************************************************/

	static final class SparseLU
	{
		private static final double PIVOT_THRESHOLD = 0.1;
		private static final double SINGULAR_TOL = 1e-11;

		final int n;
		final int[] q;     // column permutation: column k of LU is basis position q[k]
		final int[] pinv;  // row permutation: row i of B is row pinv[i] of LU
		final int[] Lp, Li, Up, Ui;
		final double[] Lx, Ux;

		private SparseLU(int n, int[] q, int[] pinv, int[] Lp, int[] Li, double[] Lx, int[] Up, int[] Ui, double[] Ux)
		{
			this.n = n;
			this.q = q;
			this.pinv = pinv;
			this.Lp = Lp;
			this.Li = Li;
			this.Lx = Lx;
			this.Up = Up;
			this.Ui = Ui;
			this.Ux = Ux;
		}

		/**
		 * Factorizes the n x n matrix given by its sparse columns.
		 * @return the factorization, or null if the matrix is (numerically) singular
		 */
		static SparseLU factor(final int n, final int[][] colRows, final double[][] colVals)
		{
			// column order: fewest nonzeros first (singletons first)
			Integer[] order = new Integer[n];
			for (int k = 0; k < n; k++) order[k] = k;
			Arrays.sort(order, new Comparator<Integer>() {
				@Override
				public int compare(Integer a, Integer b) {
					return Integer.compare(colRows[a].length, colRows[b].length);
				}
			});
			int[] q = new int[n];
			for (int k = 0; k < n; k++) q[k] = order[k];

			// row counts, used to prefer sparse pivot rows
			int[] rowCount = new int[n];
			for (int k = 0; k < n; k++)
				for (int i : colRows[k]) rowCount[i]++;

			int cap = 4 * n + 16;
			for (int k = 0; k < n; k++) cap += colRows[k].length;
			int[] Lp = new int[n + 1], Up = new int[n + 1];
			int[] Li = new int[cap], Ui = new int[cap];
			double[] Lx = new double[cap], Ux = new double[cap];
			int lnz = 0, unz = 0;

			int[] pinv = new int[n];
			Arrays.fill(pinv, -1);
			double[] x = new double[n];
			int[] xi = new int[2 * n];
			int[] pstack = new int[n];
			boolean[] marked = new boolean[n];

			for (int k = 0; k < n; k++)
			{
				Lp[k] = lnz;
				Up[k] = unz;
				if (lnz + n > Li.length)
				{
					int c = Math.max(2 * Li.length, lnz + n);
					Li = Arrays.copyOf(Li, c);
					Lx = Arrays.copyOf(Lx, c);
				}
				if (unz + n > Ui.length)
				{
					int c = Math.max(2 * Ui.length, unz + n);
					Ui = Arrays.copyOf(Ui, c);
					Ux = Arrays.copyOf(Ux, c);
				}

				int col = q[k];
				int[] bi = colRows[col];
				double[] bx = colVals[col];

				// x = L \ B(:,col), sparse triangular solve
				int top = n;
				for (int i : bi)
					if (!marked[i])
						top = dfs(i, top, xi, pstack, marked, pinv, Lp, Li, n);
				for (int p = top; p < n; p++) marked[xi[p]] = false;
				for (int p = top; p < n; p++) x[xi[p]] = 0.0;
				double colMax = 0.0;
				for (int t = 0; t < bi.length; t++)
				{
					x[bi[t]] = bx[t];
					colMax = Math.max(colMax, Math.abs(bx[t]));
				}
				for (int px = top; px < n; px++)
				{
					int j = xi[px];
					int J = pinv[j];
					if (J < 0) continue;
					double xj = x[j];
					if (xj == 0.0) continue;
					for (int p = Lp[J] + 1; p < Lp[J+1]; p++)
						x[Li[p]] -= Lx[p] * xj;
				}

				// pivot selection
				double amax = -1.0;
				for (int p = top; p < n; p++)
				{
					int i = xi[p];
					if (pinv[i] < 0)
						amax = Math.max(amax, Math.abs(x[i]));
					else
					{
						Ui[unz] = pinv[i];
						Ux[unz++] = x[i];
					}
				}
				if (amax <= SINGULAR_TOL * Math.max(1.0, colMax))
					return null;
				int ipiv = -1;
				int bestCount = Integer.MAX_VALUE;
				double bestAbs = 0.0;
				for (int p = top; p < n; p++)
				{
					int i = xi[p];
					if (pinv[i] >= 0) continue;
					double a = Math.abs(x[i]);
					if (a < PIVOT_THRESHOLD * amax) continue;
					if (rowCount[i] < bestCount || (rowCount[i] == bestCount && a > bestAbs))
					{
						ipiv = i;
						bestCount = rowCount[i];
						bestAbs = a;
					}
				}
				double pivot = x[ipiv];
				Ui[unz] = k;
				Ux[unz++] = pivot;
				pinv[ipiv] = k;
				Li[lnz] = ipiv;
				Lx[lnz++] = 1.0;
				for (int p = top; p < n; p++)
				{
					int i = xi[p];
					if (pinv[i] < 0 && x[i] != 0.0)
					{
						Li[lnz] = i;
						Lx[lnz++] = x[i] / pivot;
					}
					x[i] = 0.0;
				}
			}
			Lp[n] = lnz;
			Up[n] = unz;
			for (int p = 0; p < lnz; p++)
				Li[p] = pinv[Li[p]];

			return new SparseLU(n, q, pinv, Lp, Arrays.copyOf(Li, lnz), Arrays.copyOf(Lx, lnz),
					Up, Arrays.copyOf(Ui, unz), Arrays.copyOf(Ux, unz));
		}

		/** Non-recursive depth-first search in the graph of L (CSparse cs_dfs). */
		private static int dfs(int j, int top, int[] xi, int[] pstack, boolean[] marked,
				int[] pinv, int[] Lp, int[] Li, int n)
		{
			int head = 0;
			xi[0] = j;
			while (head >= 0)
			{
				j = xi[head];
				int jnew = pinv[j];
				if (!marked[j])
				{
					marked[j] = true;
					pstack[head] = (jnew < 0) ? 0 : Lp[jnew];
				}
				boolean done = true;
				int p2 = (jnew < 0) ? 0 : Lp[jnew + 1];
				for (int p = pstack[head]; p < p2; p++)
				{
					int i = Li[p];
					if (marked[i]) continue;
					pstack[head] = p;
					xi[++head] = i;
					done = false;
					break;
				}
				if (done)
				{
					head--;
					xi[--top] = j;
				}
			}
			return top;
		}

		/**
		 * Solves B x = b.
		 * @param b right-hand side, indexed by row
		 * @param x solution, indexed by basis position
		 * @param w work array of length n
		 */
		void solve(double[] b, double[] x, double[] w)
		{
			for (int i = 0; i < n; i++)
				w[pinv[i]] = b[i];
			for (int j = 0; j < n; j++)
			{
				double wj = w[j];
				if (wj == 0.0) continue;
				for (int p = Lp[j] + 1; p < Lp[j+1]; p++)
					w[Li[p]] -= Lx[p] * wj;
			}
			for (int j = n - 1; j >= 0; j--)
			{
				w[j] /= Ux[Up[j+1] - 1];
				double wj = w[j];
				if (wj == 0.0) continue;
				for (int p = Up[j]; p < Up[j+1] - 1; p++)
					w[Ui[p]] -= Ux[p] * wj;
			}
			for (int k = 0; k < n; k++)
				x[q[k]] = w[k];
		}

		/**
		 * Solves B^T y = c.
		 * @param c right-hand side, indexed by basis position
		 * @param y solution, indexed by row
		 * @param w work array of length n
		 */
		void solveTranspose(double[] c, double[] y, double[] w)
		{
			for (int k = 0; k < n; k++)
				w[k] = c[q[k]];
			for (int j = 0; j < n; j++)
			{
				double s = w[j];
				for (int p = Up[j]; p < Up[j+1] - 1; p++)
					s -= Ux[p] * w[Ui[p]];
				w[j] = s / Ux[Up[j+1] - 1];
			}
			for (int j = n - 1; j >= 0; j--)
			{
				double s = w[j];
				for (int p = Lp[j] + 1; p < Lp[j+1]; p++)
					s -= Lx[p] * w[Li[p]];
				w[j] = s;
			}
			for (int i = 0; i < n; i++)
				y[i] = w[pinv[i]];
		}
	}
}
