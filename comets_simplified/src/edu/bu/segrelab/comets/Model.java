/**
 * created 3 Mar 2010
 */
package edu.bu.segrelab.comets;



import javax.swing.JComponent;
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
	 * Each model should provide some kind of informational panel that the COMETS
	 * program can use to show the user:<br/>
	 * 1. What state the model may be in<br/>
	 * 2. What parameters the model has<br/>
	 * 3. Anything else of note.
	 * @return a <code>JComponent</code> with model information
	 */
	public abstract JComponent getInfoPanel();
	
	/**
	 * Each model should provide a <code>JComponent</code> that provides a set of
	 * external parameters that can be set by the user. Any changes to this panel
	 * should be stored by the <code>Model</code> and applied on request.
	 * @return a <code>JComponent</code> with modifiable parameters.
	 */
	public abstract JComponent getParametersPanel();

	/**
	 * Applies the parameter changes from the parameters panel.
	 */
	public abstract void applyParameters();
	
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
	
//	public void setColor(Color color)
//	{
//		this.color = color;
//	}
}
