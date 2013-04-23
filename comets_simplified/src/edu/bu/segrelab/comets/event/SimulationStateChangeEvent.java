package edu.bu.segrelab.comets.event;

import java.util.EventObject;

@SuppressWarnings("serial")
public class SimulationStateChangeEvent extends EventObject
{

	public enum State
	{
		START,
		PAUSE,
		END
	}
	
	private State state;
	public SimulationStateChangeEvent(State state)
	{
		super(state);
		this.state = state;
	}
	
	public State getState()
	{
		return state;
	}

}
