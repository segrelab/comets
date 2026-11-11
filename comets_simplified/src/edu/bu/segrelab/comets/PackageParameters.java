package edu.bu.segrelab.comets;

import java.io.PrintWriter;
import java.util.Map;

import edu.bu.segrelab.comets.util.ParameterState;

/**
 * This interface defines the methods necessary for a class that describes
 * the parameters in a given Comets-extending package (e.g. FBAComets).
 * 
 * Since the parameters themselves (and their accessory methods) will be package-
 * dependent, this only details the methods needed to set, load, and save them.
 * @author Bill Riehl briehl@bu.edu
 *
 */
public interface PackageParameters
{
	
	public ParameterState setParameter(String name, String value);
	
	public void dumpToFile(PrintWriter writer);

	public void saveParameterState();

	public void loadParameterState();
	
	public boolean hasParameter(String param);
	
	public ParameterType getType(String param);
}
