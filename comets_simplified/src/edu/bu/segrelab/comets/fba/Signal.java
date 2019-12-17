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
	
	/**
	 * creates a Signal object.  
	 * @throws ModelFileException 
	 * @todo have these test for correct values (esp reactions and exch_met)
	 * 
	 */
	public Signal(boolean lb, boolean ub, boolean consume_met, int reaction, int exch_met, String function,
			double[] parameters){
		this.lb = lb;
		this.ub = ub;
		this.consume_met = consume_met;
		this.reaction = reaction;
		this.exch_met = exch_met - 1;  // don't ask me why this is off-by-one but not reaction!?!?
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
		case bounded_linear:
			if (this.parameters.size() == 4){
				return true;
			}
		case linear:
			if (this.parameters.size() >= 1){
				return true;
			}
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
		double bound = A + (K - A) / Math.pow(C + Q * Math.exp(-B * (met_conc - M)),
				1.0 / v);
		return bound;
	}
	
	public double calculateBound(double met_conc) {
		double bound = 0;
		switch(this.function){
		case generalized_logistic:
			bound = generalizedLogistic(met_conc);
		case bounded_linear:
			bound = boundedLinear(met_conc);
		case linear:
			bound = linear(met_conc);
		}
//		System.out.println("met_conc: " + met_conc);
//		System.out.println ("bound: " + bound);
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
		

}