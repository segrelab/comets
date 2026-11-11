/**
 * created 3 Mar 2010
 */
package edu.bu.segrelab.comets;



/**
 * The abstract base class for a model to be used in COMETS simulations. Any package
 * used with COMETS must extend this class and provide implementations for the 
 * abstracted methods.
 * <p>
 * Examples include an <code>FBAModel</code> that performs flux balance analysis, or a
 * <code>KineticModel</code> that performs some kind of enzyme kinetics instead, or
 * even an <code>AutomatonModel</code> that provides rules for a cellular automaton.
 * <p>
 * Only the <code>FBAModel</code> has been implemented thus far. The rest are left
 * as an exercise for the student.
 * @see edu.bu.segrelab.comets.fba.FBAModel
 * @author Bill Riehl briehl@bu.edu
 *
 */
public abstract class Model
{
	protected String filename;
	
	/**
	 * Every <code>Model</code> should be instructed at some point to run, and
	 * return some kind of integer state value at the end of its run.
	 * @return an int value describing the result of the run
	 */
	public abstract int run();
	
	/**
	 * Since this <code>Model</code> in the COMETS framework is intended to describe
	 * some kind of biological phenomenon, it should have a list of nutrients that it
	 * uses.
	 * @return a <code>String</code> array of media names this model uses
	 */
	public abstract String[] getMediaNames();
	
	/**
	 * @return the name of this model.
	 */
	public abstract String getModelName();
	
	/**
	 * A debugging tool. Don't worry about this too much.
	 * @return - returns the memory footprint of the model in KB.
	 */
	//public abstract long sizeOf();
	
	public abstract Model clone();
	
	public String getFileName()
	{
		return filename;
	}

	public void setFileName(String filename)
	{
		this.filename = filename;
	}
	
}
