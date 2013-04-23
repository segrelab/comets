package edu.bu.segrelab.comets.ui;

import edu.bu.segrelab.comets.Comets;

public interface ParametersPanel
{
	public void applyChanges();
	
	public String getName();
	
	public void resetChanges();
	
	public void updateState(Comets c);
}
