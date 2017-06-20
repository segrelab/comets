package edu.bu.segrelab.comets.reaction;

import java.util.List;
import java.util.Stack;

import edu.bu.segrelab.comets.Cell;
import edu.bu.segrelab.comets.Comets;
import edu.bu.segrelab.comets.fba.FBACell;
import edu.bu.segrelab.comets.fba.FBAParameters;
import edu.bu.segrelab.comets.fba.FBAWorld;
import edu.bu.segrelab.comets.fba.FBAWorld3D;

/**A class to execute external reaction using the Runge-Kutta algorithm in a multithreaded
 * environment
 * 
 * @author mquintin
 * @date 4/21/2017
 */
public class RK4Runner {

	private Stack<FBACell> cellsToRun;
	public boolean done = false;
	public double[][][][] result;
	private int liveThreads;
	private int maxThreads;
	private RK4Calc[] threads;

	public RK4Runner(Comets c){
		cellsToRun = new Stack<FBACell>(); 
		List<Cell> fbacells = c.getCells();
		for (Cell cell : fbacells){ 
			cellsToRun.add((FBACell) cell);
			//TODO: Find a way to future proof this instead of casting? 
			//For now we're assuming this class will only ever be invoked 
			//by an FBAWorld or an FBAWorld3D
			}
		FBAParameters pParams = (FBAParameters) c.getPackageParameters();
		maxThreads = Math.min(pParams.getNumRunThreads(), cellsToRun.size());
	}
	
	public void run(){
		//create threads
		threads = new RK4Calc[maxThreads];
		for (int i = 0; i < maxThreads; i++){
			threads[i] = new RK4Calc(this);
		}
		//Kick off the threads
		for (int i = 0; i < maxThreads; i++){
			threads[i].run();
		}
		//loop until the threads are done. This is probably not how you're supposed to do this...
		while (!done){ }
	}
	
	//count the number of active threads so we'll know we're not done
	public synchronized void registerLiveThread(){
		liveThreads += 1;
		done = false;
	}
	
	//count the number of completed threads so we'll know when we're done
	public synchronized void registerFinishedThread(){
		liveThreads -= 1;
		if (liveThreads == 0) done = true;
	}
	
	public synchronized boolean processAndResetCalc(RK4Calc calc){
		//add the result to the matrix
		if (calc.result != null) this.result[calc.x][calc.y][calc.z] = calc.result;
		//reload the calculator
		boolean resume = !cellsToRun.isEmpty();
		if (resume){
			FBACell fbacell = cellsToRun.pop();
			calc.setFBACell(fbacell);
		}
		//tell the thread whether it should continue
		return resume;
	}
	
	//Runnable wrapper for the ExternalReactionCalculator that does the math
	protected class RK4Calc extends Thread{
		private RK4Runner runner; //the RK4Runner that holds this thread
		private ExternalReactionCalculator calc;
		public double[] result;
		public boolean done = false;
		public int x,y,z; //coordinates of the FBAcell in the world
		
		public RK4Calc(RK4Runner runner){
			this.runner = runner;
		}

		public void setFBACell(FBACell fbacell){
			x = fbacell.getX();
			y = fbacell.getY();
			z = fbacell.getZ(); //always 0 for 2D cells
			calc = ExternalReactionCalculator.createCalculator(fbacell);
			result = null;
			done = false;
		}
		
		@Override
		public void run() {
			runner.registerLiveThread();
			while (runner.processAndResetCalc(this)){
				result = calc.rk4();
			}
			done = true;
			runner.registerFinishedThread();
		}
	}
}

