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
		this.paramArray = new double [numCols][numRows][numMedia][4];
		this.funcArray = new String [numCols][numRows][numMedia];
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
		for (int i=0; i<numCols; i++) {
			for (int j=0; j<numRows; j++) {
				this.funcArray[i][j][metIndex] = funcName;
				for (int l=0; l<4; l++) {
					this.paramArray[i][j][metIndex][l] = funcParams[l];
				}
			}
		}
	}
	public void setCell(int x, int y, int metIndex, String funcName, double [] funcParams){
		this.isSet = true;
		this.mediaIsSet[metIndex] = true;
		funcArray[x][y][metIndex] = funcName;
		for (int l=0; l<4; l++) {
			paramArray[x][y][metIndex][l] = funcParams[l];
		}
	}
	public boolean isPeriodic(int x, int y, int metIndex) {
		if (paramArray[x][y][metIndex][0] != 0) {
			return true;
		}
		return false;
	}
	public double getValue(double time, int x, int y, int metIndex) {
		String funcName = funcArray[x][y][metIndex];
		double amplitude = paramArray[x][y][metIndex][0];
		double period = paramArray[x][y][metIndex][1];
		double phase = paramArray[x][y][metIndex][2];
		double offset = paramArray[x][y][metIndex][3];
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
		double [][][][] newParamArray = new double [numCols][numRows][newNumMedia][4];
		String [][][] newFuncArray = new String [numCols][numRows][newNumMedia];
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
				for (int i = 0; i < numCols; i++){
					for (int j = 0; j < numRows; j++){
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
