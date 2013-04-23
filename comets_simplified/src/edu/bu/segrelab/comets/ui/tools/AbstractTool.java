package edu.bu.segrelab.comets.ui.tools;


import edu.bu.segrelab.comets.CometsParameters;
import edu.bu.segrelab.comets.event.CometsChangeEvent;
import edu.bu.segrelab.comets.event.CometsChangeListener;
import edu.bu.segrelab.comets.ui.CometsSetupPanel;
import edu.bu.segrelab.comets.ui.CometsToolbarPanel;

import java.awt.Point;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

/**
 * An abstract tool class for use with the <code>CometsSetupPanel</code>. Together, the
 * extensions of this class should be used as a kind of paint program toolkit
 * for putting cells and media into the current <code>World2D</code>.
 * <p>
 * Each of these tools also has its own control panel - a <code>JPanel</code> that
 * implements some kind of customization functionality. For example, the <code>ShapeTool</code>
 * has a control panel that allows the user to change the shape's stroke width, whether
 * it is a circle or square, and whether or not the shape should be filled.
 * <p>
 * This class should not be instantiated directly, but instead be used as a basis for
 * constructing tools.
 * @see EraserTool, FillTool, LineTool, PencilTool, PipetTool, SelectTool, ShapeTool
 * @author Bill Riehl briehl@bu.edu
 */
public abstract class AbstractTool extends JToggleButton 
								   implements MouseListener, 
								   			  MouseMotionListener
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 4460478552780746022L;

	/**
	 * A <code>JPanel</code> containing customization options for the tool.
	 */
	protected JPanel controlPanel;
	
	/**
	 * The <code>CometsSetupPanel</code> that this tool should act on.
	 */
	protected CometsSetupPanel csp;
	protected CometsToolbarPanel ctp;
	protected CometsParameters cParams;

	/**
	 * A simple constructor for this class. Should be called through <code>super()</code>
	 * by any extending class.
	 * @param csp - the <code>CometsSetupPanel</code> that this tool acts on.
	 */
	public AbstractTool(CometsSetupPanel csp, CometsToolbarPanel ctp, CometsParameters cParams)
	{
		this.csp = csp;
		this.ctp = ctp;
		this.cParams = cParams;
		buildControlPanel();
	}

	/**
	 * Creates the listener list
	 */
    protected javax.swing.event.EventListenerList listenerList =
        new javax.swing.event.EventListenerList();

    /**
     * Classes can use this to register for CometsChangeEvents
     * @param listener
     */
    public void addCometsChangeListener(CometsChangeListener listener) {
        listenerList.add(CometsChangeListener.class, listener);
    }

    /**
     * Classes can unregister from listening for CometsChangeEvents.
     * @param listener
     */
    public void removeCometsChangeListener(CometsChangeListener listener) {
        listenerList.remove(CometsChangeListener.class, listener);
    }

    /**
     * Fires CometsChangeEvents when necessary.
     */
    protected synchronized void fireCometsChangePerformed() {
    	CometsChangeEvent e = new CometsChangeEvent(this);
    	Object[] listeners = listenerList.getListenerList();

    	// Each listener occupies two elements - the first is the listener class
        // and the second is the listener instance
        for (int i=0; i<listeners.length; i+=2) {
            if (listeners[i]==CometsChangeListener.class) {
                ((CometsChangeListener)listeners[i+1]).cometsChangePerformed(e);
            }
        }
    }

	/**
	 * Builds a control panel for the tool, full of customization options.  
	 */
	protected void buildControlPanel()
	{
		controlPanel = new JPanel();
	}

	/**
	 * Returns the tool's control panel.
	 * @return the <code>JPanel</code> with the tool's customization options.
	 */
	public JComponent getControlPanel() { return controlPanel; }
	
	/**
	 * "Paints" spaces with biomass or media, according to what's currently set in the
	 * tool's palette.
	 * @param spaces
	 * @param overwrite
	 */
	public void paintSpaces(List<Point> spaces, boolean overwrite)
	{
		csp.applyBiomassChange(ctp.getBiomassPanel().getValues(), spaces, overwrite);
		csp.applyMediaChange(ctp.getMediaPanel().getValues(), spaces, overwrite);
		
		if (ctp.getMediaPanel().hasStaticChecked())
			csp.addStaticMediaSpaces(ctp.getMediaPanel().getValues(), ctp.getMediaPanel().getStatic(), spaces, true);
		else
		{
			if (overwrite)
				csp.removeStaticMediaSpaces(spaces);
		}
		
		if (ctp.useMediaRefresh())
		{
			csp.addMediaRefreshSpaces(ctp.getMediaRefreshPanel().getValues(), spaces, overwrite);
		}
		else
		{
			if (overwrite)
				csp.removeMediaRefreshSpaces(spaces);
		}
		csp.setBarrier(spaces, ctp.getBarrier());
		csp.repaint();
	}

	public void reset() { } 
}