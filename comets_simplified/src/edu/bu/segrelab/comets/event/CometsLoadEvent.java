package edu.bu.segrelab.comets.event;

import java.util.EventObject;

@SuppressWarnings("serial")
public class CometsLoadEvent extends EventObject
{
	public static enum Type {
		LAYOUT,
		MODEL,
		UNDO,
		REDO,
		RESET,
		PARAMETERS
	}
	
	private Type type;
	
	public CometsLoadEvent(Object source, Type type)
	{
		super(source);
		this.type = type;
	}
	
	public Type getType()
	{
		return type;
	}
}
