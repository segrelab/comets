package edu.bu.segrelab.comets.ui.tools;

import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

import edu.bu.segrelab.comets.CometsParameters;
import edu.bu.segrelab.comets.ui.CometsSetupPanel;
import edu.bu.segrelab.comets.ui.CometsToolbarPanel;

/**
 * The <code>FillTool</code> is a part of the tool palette used in modifying the initial
 * layout in the <code>CometsSetupPanel</code>.
 * <p>
 * When the user selects this tool and clicks on the setup panel, values are filled in an area.
 * <p>
 * NOT FINISHED YET. So don't use this.
 * @author Bill Riehl
 */
public class FillTool extends AbstractTool
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 2887787851924256163L;

	public FillTool(CometsSetupPanel csp, CometsToolbarPanel ctp, CometsParameters cParams)
	{
		super(csp, ctp, cParams);
		setText("Fill");
	}

	protected void buildControlPanel()
	{
		controlPanel = new JPanel();
		controlPanel.setBorder(BorderFactory.createTitledBorder("Fill Options"));
		controlPanel.add(new JLabel("NOT FINISHED"));
	}
	
	@Override
	public void mouseClicked(MouseEvent e)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseEntered(MouseEvent e)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseExited(MouseEvent e)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void mousePressed(MouseEvent e)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseReleased(MouseEvent e)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseDragged(MouseEvent e)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseMoved(MouseEvent e)
	{
		// TODO Auto-generated method stub

	}

}
