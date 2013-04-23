/**
 * FBARunThread
 * ------------
 * //TODO Fix this so that it calls the FBACell's run function. Maybe pass it the Model[] it
 * //controls and make it synchronized? Would that be enough? As it stands, each FBACell is only
 * //passed to ONE FBAThread per run, so this might be okay...
 * // DONE! 10/6/2011
 * <p>
 * This class implements a threaded version of the FBA solver core to COMETS. That is to say,
 * the FBA process itself isn't threaded, but the number of FBA processes that can run 
 * concurrently is increased by constructing a set of these.
 * <p>
 * Each individual thread is constructed to act as a worker. As it runs, it fetches the next
 * <code>FBACell</code> to be analyzed from the <code>FBAWorld</code>, performs the analysis,
 * then fetches the next one.
 * <p>
 * The major trade off is that each FBARunThread gets its own copy of each FBAModel in the
 * system. So if there's several genome-scale models and many threads running, the memory
 * footprint can grow pretty quickly. On the upside, this makes things process very, very fast.
 * 
 * @author Bill Riehl briehl@bu.edu
 */
package edu.bu.segrelab.comets.fba;

import edu.bu.segrelab.comets.CometsParameters;
import edu.bu.segrelab.comets.Model;

public class FBARunThread extends Thread
{
	private FBAWorld world;			// The FBAWorld containing the cells and models of note
	private FBAModel[] models;		// Copies of the FBAModels[] kept by the thread - lost once
									// the thread shuts down.
	private boolean die = false;	

	/**
	 * Constructor for the <code>FBARunThread</code>. This does the work of making
	 * copies of each <code>FBAModel</code>, keeping track of the world and parameters,
	 * and adding itself to the <code>ThreadGroup</code>.
	 * @param world
	 * @param threadGroup
	 * @param cParams
	 * @param fbaModels
	 */
	public FBARunThread(FBAWorld world, ThreadGroup threadGroup, 
						CometsParameters cParams, Model[] fbaModels)
	{
		super(threadGroup, "FBARunThread");
		
		this.world = world;
		// need to make COPIES here.
		models = new FBAModel[fbaModels.length];
		for (int i=0; i<models.length; i++)
		{
			models[i] = ((FBAModel)fbaModels[i]).clone();
		}
	}
	
	/**
	 * Tells this <code>FBARunThread</code> to shut down at the end of the FBA processing cycle.
	 */
	public void die()
	{
		System.out.println(getName() + " got kill signal");
		die = true;
	}
	
	/**
	 * Runs by waiting for a cell to run, fetching the <code>FBACell</code> from the
	 * <code>FBAWorld</code>, running the FBA process using its data (e.g., using its copy
	 * of the FBAModels to run), then signaling that it's finished.
	 */
	public void run()
	{
		System.out.println(getName() + " starting up.");
		while (!die)
		{
		//	System.out.println(getName() + " looping!");		
			FBACell cell = null;
			while((cell = world.getNextRunCell()) != null)
			{
//				System.out.println(getName() + " working on cell: " + cell.getID());

				int ret = cell.run(models);
				world.finishedRunningCell(cell, ret);
				
			}
		}
		//System.out.println(getName() + " finishing up");
	}
}
