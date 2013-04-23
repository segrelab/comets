package edu.bu.segrelab.comets.ui;


import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;

import edu.bu.segrelab.comets.Comets;

/**
 * This is the basis for an information-filled <code>JFrame</code> used by the 
 * <code>Comets</code> program. 
 * <p>
 * It's basically a wrapper around <code>JFrame</code> that encourages the use of
 * only a single informational <code>JComponent</code> provided by the <code>World2D</code>
 * or <code>Model</code> classes, for example.
 * <p>
 * Other functionality might be added later.
 * @author Bill Riehl briehl@bu.edu
 */
@SuppressWarnings("serial")
public class CometsInfoFrame extends JFrame
{
	/**
	 * 
	 */
	@SuppressWarnings("unused")
	private Comets c;
	private JComponent infoPanel;
	
	/**
	 * Constructs an empty (except for a single <code>JLabel</code> saying it's empty)
	 * <code>CometsInfoFrame</code> from a controlling <code>Comets</code> object and
	 * a title <code>String</code>.
	 * @param c - the <code>Comets</code> object that will own this frame
	 * @param title - a title <code>String</code> for this frame 
	 */
	public CometsInfoFrame(Comets c, String title)
	{
		this.c = c;
		setTitle(title);
		getContentPane().add(new JLabel("No information available"));
	}

	/**
	 * Constructs a new <code>CometsInfoFrame</code> linked to a given <code>Comets</code>
	 * object with an informative <code>JComponent</code>. 
	 * @param c - the <code>Comets</code> object that will own this frame
	 * @param title - a title <code>String</code> for this frame 
	 * @param infoPanel - the information panel for this frame
	 */
	public CometsInfoFrame(Comets c, String title, JComponent infoPanel)
	{
		this(c, title);
		setInfoPanel(infoPanel);
	}

	/**
	 * Sets a new <code>JComponent</code> in this frame and removes the old one.
	 * @param infoPanel - the new <code>JComponent</code> for the frame
	 */
	public void setInfoPanel(JComponent infoPanel)
	{
		this.infoPanel = infoPanel;
		getContentPane().removeAll();
		getContentPane().add(infoPanel);
		pack();
	}
	
	/**
	 * @return true if there is an <code>JComponent</code> in this frame, false otherwise.
	 */
	public boolean hasInfoPanel()
	{
		return infoPanel != null;
	}
}
