package edu.bu.segrelab.comets.fba;

import java.lang.Math;
/**
 * Signal is a simple class that holds information about how 
 * a particular reaction's bounds are modified by the 
 * local concentration of a metabolite acting as a signal.
 * 
 * FBAModel will likely hold a list of these, read from a 
 * model file.  Functions will be called by FBACell.run(), 
 * when bounds are being calculated.
 * @author jeremy
 *
 */
public class Signal {
	private boolean lb, ub; // True if this signal affects these bounds
	private int reaction; // the reaction number affected
	private int exch_met;// the exchange met number of the affecting metabolite
	//note:  above could easily be turned into a list, if multiple mets
	//   can affect a reaction
	//
	//  main signaling function is Richard's, aka generalized logistic:
	//
	//   bound(met) = A + (K-A) / [(C + Q*exp(-B*(met-M)))^(1/v)]
	private double A, K, C, Q, B, v, M;
	
	/**
	 * creates a Signal object.  
	 * @todo have these test for correct values (esp reactions and exch_met)
	 * 
	 */
	public Signal(boolean lb, boolean ub, int reaction, int exch_met,
			double A, double K, double B, double M, double C, double Q, double v) {
		this.lb = lb;
		this.ub = ub;
		this.reaction = reaction;
		this.exch_met = exch_met - 1;  // don't ask me why this is off-by-one but not reaction!?!?
		this.A = A;
		this.K = K;
		this.B = B;
		this.M = M;
		this.C = C;
		this.Q = Q;
		this.v = v;	
	}
	
	
	
	public boolean affectsLb() {
		return this.lb;
	}
	public boolean affectsUb() {
		return this.ub;
	}
	
	public double calculateBound(double met_conc) {
		double bound = A + (K - A) / Math.pow(C + Q * Math.exp(-B * (met_conc - M)),
				1.0 / v);
		System.out.println ("bound: " + bound);
		return bound;
	}
	
	public int getReaction() {
		return this.reaction;
	}
	
	public int getExchMet() {
		return this.exch_met;
	}
		

}