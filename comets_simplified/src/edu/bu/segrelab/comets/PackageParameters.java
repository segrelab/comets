package edu.bu.segrelab.comets;

import java.io.PrintWriter;
import java.util.Map;

import edu.bu.segrelab.comets.ui.ParametersPanel;
import edu.bu.segrelab.comets.util.ParameterState;

/**
 * This interface defines the methods necessary for a class that describes
 * the parameters in a given Comets-extending package (e.g. FBAComets).
 * 
 * Since the parameters themselves (and their accessory methods) will be package-
 * dependent, this only details what kind of GUI methods are necessary.
 * @author Bill Riehl briehl@bu.edu
 *
 */
public interface PackageParameters
{
	/**
	 * Gets a set of ParametersPanels from the package. While these implement ParametersPanel, they should also
	 * be a JComponent of some sort.
	 * @return
	 */
	public Map<String, ParametersPanel> getParametersPanels();
	
	public ParameterState setParameter(String name, String value);
	
	public void dumpToFile(PrintWriter writer);

	public void saveParameterState();

	public void loadParameterState();
	
	public boolean hasParameter(String param);
	
	public ParameterType getType(String param);
}
