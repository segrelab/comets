package edu.bu.segrelab.comets.event;

import java.util.EventListener;

public interface CometsLoadListener extends EventListener
{
	public void cometsLoadPerformed(CometsLoadEvent e);
}
