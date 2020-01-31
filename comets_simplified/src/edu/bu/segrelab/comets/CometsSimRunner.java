package edu.bu.segrelab.comets;

import edu.bu.segrelab.comets.event.SimulationStateChangeEvent;
import edu.bu.segrelab.comets.fba.FBAWorld;


/**
 * The CometsSimRunner is an independent thread that runs all of the logic of doing
 * a simulation, independent of the GUI. This prevents the GUI from seizing up
 * while heavy modeling calculations are going on.
 * 
 * @author Bill Riehl briehl@bu.edu
 */
public class CometsSimRunner extends Thread
							 implements CometsConstants
{
	private Comets c;
	private int curCycle;
	private long totalTime;
	private boolean finished = true;
	
	/**
	 * The constructor for the class. This just needs access to a <code>Comets</code>.
	 * @param c
	 */
	public CometsSimRunner(Comets c)
	{
		this.c = c;
		curCycle = 0;
		totalTime = 0;
	}
	
	/**
	 * Returns whether or not the runner is finished. If the runner hasn't been started, this also returns false.
	 * @return true if the runner is finished running.
	 */
	public synchronized boolean isFinished()
	{
		return finished;
	}
	
	/**
	 * This tells the <code>CometsSimRunner</code> to finish up at the end of the next cycle.
	 */
	public synchronized void finish()
	{
		finished = true;
	}
	
	/**
	 * The main running function of the <code>CometsSimRunner</code>. Until told to finish
	 * (via finish()), this will continue through the main Comets simulation logic loop. This is:
	 * <ol>
	 * <li>Show the current cycle number
	 * <li>Take a snapshot of the screen, if required
	 * <li>Test exit conditions (e.g., no more biomass alive, or beyond the max cycle)
	 * <li>Tell the world to run for a single cycle. The package that extends World2D does all the 
	 * hard work of running the simulation.
	 * <li>Clean up by pausing, if required.
	 */
	public void run()
	{
		finished = false;
		c.fireSimulationStateChangeEvent(new SimulationStateChangeEvent(SimulationStateChangeEvent.State.START));
		World.getInstance().initSimulation();
		while (!finished)
		{
			if (!c.getParameters().isPaused())
			{
//				System.out.println("MAX CYCLES = " + c.getParameters().getMaxCycles());
				long start = System.currentTimeMillis();
				curCycle++;
				if (c.getParameters().showCycleCount())
				{
					//show the cycle count somewhere
					System.out.println("Cycle " + curCycle);
				}
				
				// Take a slideshow snapshot, if we're at the right time.
				if (curCycle % c.getParameters().getSlideshowRate() == 0 && c.getParameters().saveSlideshow())
				{
					c.takeSlideshowScreenshot((int)curCycle);
				}
	
				// Test quit conditions, end simulation if required.
				if (checkCompletion())
				{
					World.getInstance().endSimulation();
					finish();
					System.out.println("End of simulation");
					System.out.println("Total time = " + (totalTime/1000.0) + "s");
					c.fireSimulationStateChangeEvent(new SimulationStateChangeEvent(SimulationStateChangeEvent.State.END));
					return;
				}
	
				// Simulate!
				if (c.getParameters().getNumLayers()==1)
				// do batch dilution here
				{
					if (c.getParameters().getBatchDilution())
					{
						if ((curCycle*c.getParameters().getTimeStep()) % c.getParameters().getDilutionTime() == 0) 
						{
							((FBAWorld) World.getInstance()).batchDilute(c.getParameters().getDilutionFactor(), c.getParameters().getCellSize());
							System.out.println("Transfer performed: dilute " + 
												c.getParameters().getDilutionFactor() + 
												" each " + c.getParameters().getDilutionTime() + 'h');
						}
					}

					World.getInstance().run();
				}
				else if (c.getParameters().getNumLayers()>1)
					World.getInstance().run();
				//c.getWorld().run();
	
				// Clean up after a single step.
				if (c.getParameters().pauseOnStep())
				{
					c.getParameters().pause(true);
					c.fireSimulationStateChangeEvent(new SimulationStateChangeEvent(SimulationStateChangeEvent.State.PAUSE));
				}
				if (c.getParameters().showCycleTime())
					System.out.println("Cycle complete in " + (System.currentTimeMillis() - start)/1000.0 + "s");
				totalTime += System.currentTimeMillis() - start;
				c.updateOnCycle();
			}
			else
			{
				if (!c.getParameters().isCommandLineOnly())
				{
					try
					{
						c.getRunBarrier().await();
					}
					catch (Exception e)
					{
						System.out.println(e);
					}
				}
			}
		}
		c.fireSimulationStateChangeEvent(new SimulationStateChangeEvent(SimulationStateChangeEvent.State.END));
	}
	
	//*asynchronous* check of if we should start a new cycle 
	public boolean checkCompletion(){
		boolean complete = false;
		
		if (c.getCells().size() == 0 && World.reactionModel.isSetUp()) {
			complete = true; //there's nothing left to run
		}
		
		if (curCycle > c.getParameters().getMaxCycles() && c.getParameters().getMaxCycles() != UNLIMITED_CYCLES){
			complete = true; //time's up
		}
		
		return complete;
	}
}
