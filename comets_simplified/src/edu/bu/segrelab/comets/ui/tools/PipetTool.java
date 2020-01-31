package edu.bu.segrelab.comets.ui.tools;


import java.awt.event.MouseEvent;

import javax.swing.JPanel;

import edu.bu.segrelab.comets.Comets;
import edu.bu.segrelab.comets.World;
import edu.bu.segrelab.comets.World2D;
import edu.bu.segrelab.comets.ui.CometsSetupPanel;
import edu.bu.segrelab.comets.ui.CometsToolbarPanel;


/**
 * The <code>PipetTool</code> is the simplest tool in the toolbar palette. When the 
 * user clicks on a space in the <code>CometsSetupPanel</code> with this selected, every
 * property of that space - biomass / media concentrations and barrier status - is copied
 * to the <code>CometsToolbarPanel</code>.
 * <p>
 * These values are then applied whenever the <code>LineTool</code>, <code>ShapeTool</code>, or
 * <code>PencilTool</code> are used.
 * <p>
 * This is analogous to using a pipet to draw from a Petri dish or from a plate. Get it?
 * (also like an eyedropper from common paint programs).
 * @author Bill Riehl briehl@bu.edu
 */
public class PipetTool extends AbstractTool
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 7665777079563682310L;
	private Comets c;
	
	/**
	 * Initializes a new <code>PipetTool</code> for use with a <code>CometsSetupPanel</code>,
	 * <code>CometsToolbarPanel</code>, and <code>Comets</code>.
	 * <p>
	 * It needs direct access to the <code>Comets</code> object in order to get to the world
	 * and everything that's in it. 
	 * @param csp - the <code>CometsSetupPanel</code> to draw data from.
	 * @param ctp - the <code>CometsToolbarPanel</code> to feed pipetted data to.
	 * @param c - the <code>Comets</code> object that hold the <code>World2D</code>
	 */
	public PipetTool(CometsSetupPanel csp, CometsToolbarPanel ctp, Comets c)
	{
		super(csp, ctp, c.getParameters());
		this.c = c;
		setText("Pipet");
	}

	/**
	 * Builds a blank control panel - there's no controls for this tool.
	 */
	protected void buildControlPanel()
	{
		controlPanel = new JPanel();
	}

	@Override
	/**
	 * When the user clicks the mouse, the tool should go into the world, grab information
	 * on the concentrations of media and biomass, and whether or not it's a barrier, and feed
	 * that back to the <code>CometsToolbarPanel</code>.
	 */
	public void mouseClicked(MouseEvent e)
	{
		int x = (int)(Math.floor(e.getPoint().getX()/c.getParameters().getPixelScale()));
		int y = (int)(Math.floor(e.getPoint().getY()/c.getParameters().getPixelScale()));
//		Point p = new Point((int)Math.floor(e.getPoint().getX()/c.getParameters().getPixelScale()),
//						    (int)Math.floor(e.getPoint().getY()/c.getParameters().getPixelScale()));
		if (((World2D) World.getInstance()).isOccupied(x, y))
		{
			double[] biomass = ((World2D) World.getInstance()).getCellAt(x, y).getBiomass();
			ctp.getBiomassPanel().setValues(biomass);
		}
		else
		{
			ctp.getBiomassPanel().setValues(new double[c.getModels().length]);
		}
		
		ctp.getMediaPanel().setValues(((World2D) World.getInstance()).getMediaAt(x, y));
		
		if (((World2D) World.getInstance()).isStaticMediaSpace(x, y))
			ctp.getMediaPanel().setStatic(((World2D) World.getInstance()).getStaticMediaSet(x, y));
		else
			ctp.getMediaPanel().setStatic(null);
		
		if (((World2D) World.getInstance()).isMediaRefreshSpace(x, y))
			ctp.getMediaRefreshPanel().setValues(((World2D) World.getInstance()).getMediaRefreshAmount(x, y));
		else
			ctp.getMediaRefreshPanel().setValues(new double[((World2D) World.getInstance()).getNumMedia()]);

		ctp.setMediaRefresh(((World2D) World.getInstance()).isMediaRefreshSpace(x, y));
		ctp.setBarrier(((World2D) World.getInstance()).isBarrier(x, y));
	}

	@Override
	/**
	 * Required by MouseListener - not used
	 */
	public void mouseEntered(MouseEvent e)	{ }

	@Override
	/**
	 * Required by MouseListener - not used
	 */
	public void mouseExited(MouseEvent e) {	}

	@Override
	/**
	 * Required by MouseListener - mimic with mouseClicked()
	 */
	public void mousePressed(MouseEvent e) 
	{
		mouseClicked(e);
	}

	@Override
	/**
	 * Required by MouseListener - not used
	 */
	public void mouseReleased(MouseEvent e) { }

	@Override
	/**
	 * Dragging the "pipet" around just updates the selected point.
	 */
	public void mouseDragged(MouseEvent e) 
	{
		mouseClicked(e);
	}

	@Override
	/**
	 * Required by MouseMotionListener - not used
	 */
	public void mouseMoved(MouseEvent e) { }

}
