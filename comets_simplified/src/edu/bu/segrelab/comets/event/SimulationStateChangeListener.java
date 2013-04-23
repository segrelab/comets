package edu.bu.segrelab.comets.event;

import java.util.EventListener;

public interface SimulationStateChangeListener extends EventListener
{
	public void onStateChange(SimulationStateChangeEvent e);
}
