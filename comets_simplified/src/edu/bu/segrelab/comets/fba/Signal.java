package edu.bu.segrelab.comets.fba;

import java.lang.Math;
import java.util.List;
import java.util.ArrayList;

import edu.bu.segrelab.comets.exception.ModelFileException;
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
	private boolean consume_met; // for toxins, if this is true, then the metabolite is consumed at the same rate;
	private int reaction; // the reaction number affected
	private int exch_met;// the exchange met number of the affecting metabolite
	private String function; // The name of the signaling function to use
	private final String generalized_logistic = "generalized_logistic";
	private final String bounded_linear = "bounded_linear";
	private final String linear = "linear";
	private final String[] allowed_functions = {generalized_logistic,
			bounded_linear, linear};
	

	private List<Double> parameters;
	
	private boolean is_multitoxin = false;
	private int[] exch_mets;
	private double[][] multiToxinParms;
	
	/**
	 * creates a Signal object.  
	 * 
	 * These are checked during a COMETS simulation in FBACell.run() 
	 * They code for an interaction between the concentration of an
	 * external metabolite and either a reaction's bound or the death 
	 * rate of a model.
	 * 
	 * There are three allowed functions that describe the relationship
	 * between a signaling metabolite and the effect: generalized_logistic,
	 * bounded_linear, and linear
	 * 
	 * 
	 */
	public Signal(boolean lb, boolean ub, boolean consume_met, int reaction, int exch_met, String function,
			double[] parameters){
		this.lb = lb;
		this.ub = ub;
		this.consume_met = consume_met;
		if (reaction != -1) {
			reaction -= 1; // convert from 1-base to 0-base
		}
		this.reaction = reaction;
		this.exch_met = exch_met - 1;  // because we let people write in terms of 1-base, but java is 0-order
		this.function = function.toLowerCase();
		
		this.parameters = new ArrayList<>();
		for (double p : parameters){
			this.parameters.add(p);
		}
		
		
		// Check for incorrect model stuff given by user
		boolean function_allowed = checkFunction();
		if (!function_allowed){
			throw new IllegalArgumentException("the function for the signal must be one of:\n" +
					"generalized_logistic\nbounded_linear\nlinear");
		}
		
		boolean correct_number_of_parameters = checkParameterNumber();
		if (!correct_number_of_parameters){
			throw new IllegalArgumentException("you must provide the correct number of parameters for your signaling function");
		}
		


		addDefaultParameters();

	}
	
	public Signal(boolean lb, boolean ub, int reaction, 
			int[] exch_mets, double[][] parms){
		this.lb = lb;
		this.ub = ub;
		this.is_multitoxin = true;
		if (reaction != -1) {
			reaction -= 1; // convert from 1-base to 0-base
		}
		this.reaction = reaction;
		for (int i = 0; i < exch_mets.length; i++) {
			exch_mets[i] = exch_mets[i] - 1; // 1-base to 0-base
		}
		this.exch_mets = exch_mets;
		this.multiToxinParms = parms;
	}
	
	
	private boolean checkFunction(){
		for (String func : this.allowed_functions){
			if (this.function.equals(func)){
				return true;
			}
		}
		return false;
	}
	
	private boolean checkParameterNumber(){
		switch(this.function){
		case generalized_logistic:
			if (this.parameters.size() >= 4){
				return true;
			}
			break;
		case bounded_linear:
			if (this.parameters.size() == 4){
				return true;
			}
			break;
		case linear:
			if (this.parameters.size() >= 1){
				return true;
			}
			break;
		}
		return false;
	}
	
	private void addDefaultParameters(){
		if (this.function.equals(generalized_logistic)){
			// A, K, B, M, C, Q, V
			if (this.parameters.size() < 4){
				this.parameters.add(0.0); // M
			}
			if (this.parameters.size() < 5){
				this.parameters.add(1.0); // C
			}
			if (this.parameters.size() < 6){
				this.parameters.add(1.0); // Q
			}
			if (this.parameters.size() < 7){
				this.parameters.add(1.0); // v
			}
		}
		if (this.function.equals(linear)){
			if (this.parameters.size() == 1){
				this.parameters.add(0.0); // B, the y-intercept
			}
		}
	}
	
	private double linear(double met_conc){
		double m = this.parameters.get(0); // slope
		double B = this.parameters.get(1); // intercept
		
		double bound = m * met_conc + B;
		return bound;
	}
	
	private double boundedLinear(double met_conc){
		double A = this.parameters.get(0);  // intercept
		double B = this.parameters.get(1);  // offset
		double C = this.parameters.get(2);  // slope
		double D = this.parameters.get(3);  // point where slope ceases to increase

		double bound = A;
		if (met_conc >= D){
			bound = A + (D - B) * C;
		}else if (met_conc >= B){
			bound = A + (met_conc - B) * C;
		}
		return bound;
		
	}
	
	
	private double generalizedLogistic(double met_conc){
		// parameter_order = A, K, B, M, C, Q, v
		double A = this.parameters.get(0);
		double K = this.parameters.get(1);
		double B = this.parameters.get(2);
		double M = this.parameters.get(3);
		double C = this.parameters.get(4);
		double Q = this.parameters.get(5);
		double v = this.parameters.get(6);
		double bound = A + ((K - A) / Math.pow(C + Q * Math.exp(-B * (met_conc - M)),
				1.0 / v));
		return bound;
	}
	
	// an if statement does this or calculateBound in FBAModel
	public double multipleHillToxin(double[] met_concs) {
		double vmax = this.multiToxinParms[0][0];
		double[] kms = this.multiToxinParms[1]; // 
		double[] hills = this.multiToxinParms[2];
		double bound = vmax;
		for (int i = 0; i < met_concs.length; i++) {
			bound = bound * Math.pow(kms[i], hills[i]) / (Math.pow(met_concs[i], hills[i]) + Math.pow(kms[i], hills[i]));
		}
		return bound;		
	}
	
	public double calculateBound(double met_conc) {
		double bound = 0;
		switch(this.function){
		case generalized_logistic:
			bound = generalizedLogistic(met_conc);
			break;
		case bounded_linear:
			bound = boundedLinear(met_conc);
			break;
		case linear:
			bound = linear(met_conc);
			break;
		}
		return bound;
	}
	
	public double calculateDeathRate(double met_conc){
		return calculateBound(met_conc);
		
	}
	
	public boolean isMetConsumed(){
		return this.consume_met;
	}
	
	public boolean affectsLb() {
		return (this.lb && this.reaction != -1);
	}
	public boolean affectsUb() {
		return (this.ub && this.reaction != -1);
	}
	public boolean affectsDeathRate(){
		return this.reaction == -1;
	}
	
	public int getReaction() {
		return this.reaction;
	}
	
	public int getExchMet() {
		return this.exch_met;
	}
	public int[] getExchMets() {
		//specifically for multitoxins
		return this.exch_mets;
	}
	public boolean isMultiToxin() {
		return this.is_multitoxin;
	}
		

}