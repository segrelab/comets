package edu.bu.segrelab.comets.fba;

// import edu.bu.segrelab.comets.CometsParameters;
//import java.lang.*;
import java.util.Arrays;

public class FBAPeriodicMedia{
	private int numCols;
	private int numRows;
	private int numMedia = 0;
	private String [] mediaNames;
	private double [][][][] paramArray;
	private String [][][] funcArray;
	public boolean isSet = false;
	public boolean [] mediaIsSet;
	
	// Constructors
	public FBAPeriodicMedia() {
		this.numRows = 0;
		this.numCols = 0;
	}
	
	// Methods
	public void setSize(int numRows, int numCols, String [] mediaNames) {
		this.numCols = numCols;
		this.numRows = numRows;
		this.numMedia = mediaNames.length;
		this.mediaNames = mediaNames;
		this.paramArray = new double [numRows][numCols][numMedia][4];
		this.funcArray = new String [numRows][numCols][numMedia];
		this.mediaIsSet  = new boolean [numMedia];
		fillParamArray(this.paramArray, 0);
		Arrays.fill(this.mediaIsSet, false);
	}
	public void fillParamArray(double [][][][] pArray, double value) {
		for (double [][][] cube : pArray) {
			for (double [][] square : cube) {
				for (double[] line : square) {
					Arrays.fill(line, value);
			    }
			}
		}
	}

	public void setAllCells(int metIndex, String funcName, double [] funcParams) {
		this.isSet = true;
		this.mediaIsSet[metIndex] = true;
		for (int i=0; i<numRows; i++) {
			for (int j=0; j<numCols; j++) {
				this.funcArray[i][j][metIndex] = funcName;
				for (int l=0; l<4; l++) {
					this.paramArray[i][j][metIndex][l] = funcParams[l];
				}
			}
		}
	}
	public void setCell(int row, int col, int metIndex, String funcName, double [] funcParams){
		this.isSet = true;
		this.mediaIsSet[metIndex] = true;
		funcArray[row][col][metIndex] = funcName;
		for (int l=0; l<4; l++) {
			paramArray[row][col][metIndex][l] = funcParams[l];
		}
	}
	public boolean isPeriodic(int row, int col, int metIndex) {
		if (paramArray[row][col][metIndex][0] != 0) {
			return true;
		}
		return false;
	}
	public double getValue(double time, int row, int col, int metIndex) {
		String funcName = funcArray[row][col][metIndex];
		double amplitude = paramArray[row][col][metIndex][0];
		double period = paramArray[row][col][metIndex][1];
		double phase = paramArray[row][col][metIndex][2];
		double offset = paramArray[row][col][metIndex][3];
		return calculate(time, funcName, amplitude, period, phase, offset);
		
	}
	private double calculate(double time, String funcName, double amplitude, double period, double phase, double offset) {
		double angularFrequency = (2*Math.PI)/period;
		double angularPhase = 2*Math.PI*phase/period;
		if (funcName.equalsIgnoreCase("step")){
			// Periodic step function
			return amplitude*Math.floor((2*(time+phase)/period)%2)+offset;
		}
		else if (funcName.equalsIgnoreCase("half_sin")) {
			// Sin function larger than 0
			// f(x) = max(0, a*sin(b*t+c) + d)
			return Math.max(0, amplitude*Math.sin(angularFrequency*time + angularPhase) + offset);
		} 
		else if (funcName.equalsIgnoreCase("sin")) {
			return amplitude*Math.sin(angularFrequency*time + angularPhase) + offset;			
		}
		else if (funcName.equalsIgnoreCase("half_cos")) {
			// Sin function larger than 0
			// f(x) = max(0, a*sin(b*t+c) + d)
			return Math.max(0, amplitude*Math.cos(angularFrequency*time + angularPhase) + offset);
		} 
		else if (funcName.equalsIgnoreCase("cos")) {
			return amplitude*Math.cos(angularFrequency*time + angularPhase) + offset;				
		}
		else {
			return 0;
		}
	}
	public void reshapeMedia(String [] newMediaNames) {
		
		int newNumMedia = newMediaNames.length;
		double [][][][] newParamArray = new double [numRows][numCols][newNumMedia][4];
		String [][][] newFuncArray = new String [numRows][numCols][newNumMedia];
		boolean [] newMediaIsSet = new boolean [newNumMedia];
		
		// Create mapping array
		int [] mappingArr = new int [newNumMedia]; 
		Arrays.fill(mappingArr,  -1);
		for (int i=0; i<numMedia;i++) {
			if (mediaIsSet[i]) {
				int newIdx = Arrays.binarySearch(newMediaNames, this.mediaNames[i]);
				mappingArr[i] = newIdx;
			}
		}
		
		// Set to 0 or false
		fillParamArray(newParamArray, 0);
		Arrays.fill(newMediaIsSet, false);
		
		// Fill new arrays with values from old arrays
		for (int k = 0; k < this.numMedia; k++){
			if (mappingArr[k] != -1){
				newMediaIsSet[mappingArr[k]] = true;
				for (int i = 0; i < numRows; i++){
					for (int j = 0; j < numCols; j++){
						newFuncArray[i][j][mappingArr[k]] = this.funcArray[i][j][k];
						for (int l = 0; l <4; l++) {
							newParamArray[i][j][mappingArr[k]][l] = this.paramArray[i][j][k][l];
						}
					}	
				}
			}
		}
		this.mediaIsSet = newMediaIsSet;
		this.paramArray = newParamArray;
		this.funcArray = newFuncArray;
		this.numMedia = newNumMedia;
		this.mediaNames = newMediaNames;
	}
}
	
/*
 * public class FBAPeriodicMediaCell { String funcName; String[]
 * availableFunctions = {"sin", "cos", "step", "half_sin", "half_cos"}; double
 * amplitude, period, phase, offset, angularFrequency, angularPhase;
 * 
 * Function to set function name and parameters public void setFunction(String
 * funcName, double[] funcParams) { if (checkFuncNameAndParam(funcName,
 * funcParams)) { this.funcName = funcName; this.amplitude = funcParams[0];
 * this.period = funcParams[1]; this.phase = funcParams[2]; this.offset =
 * funcParams[3]; this.angularFrequency = (2*Math.PI)/period; this.angularPhase
 * = 2*Math.PI*period/phase;
 * 
 * } else { System.out.println("Periodic function not set"); // This throw be an
 * exception }
 * 
 * }
 * 
 * // Function to verify that the function name is correct public boolean
 * checkFuncNameAndParam(String funcName, double[] funcParams) { if
 * (funcParams.length==4) { for (String str: availableFunctions) { if
 * (str.equalsIgnoreCase(funcName)){ return true; } } } return false; }
 * 
 * Function to calculate the function value at a given timepoint public double
 * calculate(double time) { if (this.funcName.equalsIgnoreCase("step")){ //
 * Periodic step function // f(t) = x0 * (
 * 
 * double halfPeriod = period / 2.0; double [] x_arr = {0, halfPeriod}; double
 * [] y_arr = {offset, offset+amplitude};
 * 
 * StepFunction stepFun = new StepFunction(x_arr, y_arr); return
 * stepFun.value(time-phase);
 * 
 * 
 * return amplitude*Math.floor((2*(time+phase)/period)%2)+offset;
 * 
 * 
 * } else if (this.funcName.equalsIgnoreCase("half_sin")) { // Sin function
 * larger than 0 // f(x) = max(0, a*sin(b*t+c) + d) return Math.max(0,
 * amplitude*Math.sin(angularFrequency*time + angularPhase) + offset); } else if
 * (this.funcName.equalsIgnoreCase("sin")) { return
 * amplitude*Math.sin(angularFrequency*time + angularPhase) + offset; } else if
 * (this.funcName.equalsIgnoreCase("half_cos")) { // Sin function larger than 0
 * // f(x) = max(0, a*sin(b*t+c) + d) return Math.max(0,
 * amplitude*Math.cos(angularFrequency*time + angularPhase) + offset); } else if
 * (this.funcName.equalsIgnoreCase("cos")) { return
 * amplitude*Math.cos(angularFrequency*time + angularPhase) + offset; } else {
 * return 0; } } }
 */