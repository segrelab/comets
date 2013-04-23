package edu.bu.segrelab.comets.event;

import java.util.EventListener;


public interface CometsChangeListener extends EventListener
{
	public void cometsChangePerformed(CometsChangeEvent e);
}
