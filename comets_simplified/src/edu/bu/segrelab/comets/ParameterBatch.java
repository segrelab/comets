package edu.bu.segrelab.comets;

public class ParameterBatch
{
	private String name;
	private double start,
				   inc,
				   end,
				   curValue;
	private int numSteps;
	
	public ParameterBatch(String name, double start, double inc, double end) {
		if (inc == 0)
			throw new IllegalArgumentException("ParameterBatch error: cannot increment a batch parameter by 0");
		if ((start > end && inc > 0) || (start < end && inc < 0))
			throw new IllegalArgumentException("ParameterBatch error: incrementing " + start + " by " + inc + " will never reach " + end);
		
		this.name = name;
		this.start = start;
		this.inc = inc;
		this.end = end;
		curValue = start;
		
		numSteps = (int)((end-start)/inc) + 1;
	}
	
	public void incrementStep()
	{
		curValue += inc;
	}
	
	public double getCurrentStep()
	{
		return curValue;
	}
	
	public boolean hasMoreSteps()
	{
		if (inc > 0)
			return curValue > end;
		else
			return curValue < end;
	}
	
	public double getNextStep()
	{
		double next = curValue;
		curValue += inc;
		return next;
	}
	
	public void resetBatch()
	{
		curValue = start;
	}
	
	public int getNumSteps()
	{
		return numSteps;
	}
	
	public String getName()
	{
		return name;
	}
}
