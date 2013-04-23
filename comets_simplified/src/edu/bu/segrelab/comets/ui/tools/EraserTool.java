package edu.bu.segrelab.comets.ui.tools;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JToggleButton;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import edu.bu.segrelab.comets.CometsParameters;
import edu.bu.segrelab.comets.ui.CometsSetupPanel;
import edu.bu.segrelab.comets.ui.CometsToolbarPanel;

/**
 * The <code>EraserTool</code> is a part of the tool palette used in modifying the initial
 * layout in the <code>CometsSetupPanel</code>.
 * <p>
 * When the user selects this tool and clicks on the setup panel, any added values are removed:
 * any biomass and nutrient values are reset to zero, and any barriers are removed.
 * <p>
 * This tool's control panel allows the user to adjust the shape of the tool (square or
 * round), and the size of the tool (a diameter ranging from 1 to 10 grid squares)
 * @see edu.bu.segrelab.comets.ui.tools.PencilTool
 * @author Bill Riehl briehl@bu.edu
 *
 */
public class EraserTool extends PencilTool
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -6520615935368681787L;

	/**
	 * Constructs a new <code>EraserTool</code> linked to a given <code>CometsSetupPanel</code>,
	 * <code>CometsToolbarPanel</code> and implementing a set of <code>CometsParameters</code>.
	 * @param csp - the <code>CometsSetupPanel</code> where things might be erased
	 * @param ctp - the <code>CometsToolbarPanel</code> where this tool will reside 
	 * @param cParams - a set of <code>CometsParameters</code>
	 */
	public EraserTool(CometsSetupPanel csp, CometsToolbarPanel ctp, CometsParameters cParams)
	{
		super(csp, ctp, cParams);
		setText("Eraser");
		controlPanel.repaint();
	}
	
	/**
	 * Constructs the control panel for this tool. This control panel has two functions:<br/>
	 * 1. a <code>JSlider</code> for adjusting the size of the eraser<br/>
	 * 2. a set of buttons to change the shape of the eraser (square or circle)
	 */
	protected void buildControlPanel()
	{
		controlPanel = new JPanel(new GridBagLayout());
		controlPanel.setBorder(BorderFactory.createTitledBorder("Eraser Options"));

		// Make two buttons: "b"ox and "c"ircle
		final JToggleButton[] styleButtons = new JToggleButton[2];
		styleButtons[0] = new JToggleButton("b");
		styleButtons[1] = new JToggleButton("c");
		styleButtons[0].setSelected(true);
		
		/* Whenever either button is pressed,
		 * it should remain selected, and the other one should
		 * become deselected.
		 * Also, the style should be caught and retained.
		 */
		for (int i=0; i<styleButtons.length; i++)
		{
			final JToggleButton sButton = styleButtons[i];
			sButton.addActionListener(
					new ActionListener() {
						public void actionPerformed(ActionEvent e)
						{
							for (int j=0; j<styleButtons.length; j++)
								styleButtons[j].setSelected(false);
							sButton.setSelected(true);
							if (sButton.getText().equals("b"))
								style = CometsSetupPanel.BOX_STYLE;
							else
								style = CometsSetupPanel.CIRCLE_STYLE;
							resetPencilShape();
						}
					});
		}

		JLabel sLabel = new JLabel("Size: ");
		// Initialize the size slider
		final JLabel vLabel = new JLabel(String.valueOf(CometsToolbarPanel.MIN_STROKE));
		JSlider strokeSlider = new JSlider(JSlider.HORIZONTAL, 
										   CometsToolbarPanel.MIN_STROKE, 
										   CometsToolbarPanel.MAX_STROKE, 
										   CometsToolbarPanel.MIN_STROKE);
		strokeSlider.addChangeListener(
				new ChangeListener() {
					public void stateChanged(ChangeEvent e)
					{
						JSlider source = (JSlider)e.getSource();
						strokeWidth = (source.getValue())*cParams.getPixelScale();
						vLabel.setText(String.valueOf(source.getValue()));
					}
				});
		strokeSlider.setMajorTickSpacing(1);
		strokeSlider.setPaintTicks(true);
		strokeSlider.setSnapToTicks(true);

		// Lay out the components in a GridBagLayout		
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.BOTH;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.gridwidth = 1;
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.insets = new Insets(0,0,5,0);


		for (int i=0; i<styleButtons.length; i++)
		{
			gbc.gridx = i;
			controlPanel.add(styleButtons[i], gbc);
		}

		gbc.gridx = 0;
		gbc.gridy++;

		controlPanel.add(sLabel, gbc);
		gbc.gridx = 1;
		controlPanel.add(vLabel, gbc);
		gbc.gridx = 0;
		gbc.gridwidth = 2;
		gbc.gridy++;
		gbc.insets = new Insets(0, 0, 10, 0);
		gbc.gridwidth = 3;
		strokeSlider.setPreferredSize(new Dimension(150, (int)strokeSlider.getPreferredSize().getHeight()));
		controlPanel.add(strokeSlider, gbc);
	}
	
	/**
	 * Overrides <code>PencilTool</code>'s function. While the <code>PencilTool</code>
	 * fills the shape with whatever is chosen by the user, this <code>EraserTool</code>
	 * removes any value in the setup panel. 
	 */
	protected void fillCurrentShape()
	{
		List<Point> spaces = ctp.getShapePoints(pencilShape, false);
		csp.applyBiomassChange(new double[ctp.getBiomassPanel().getValues().length], spaces, true);
		csp.applyMediaChange(new double[ctp.getMediaPanel().getValues().length], spaces, true);
		csp.removeStaticMediaSpaces(spaces);
		csp.removeMediaRefreshSpaces(spaces);
		csp.setBarrier(spaces, false);
		csp.repaint();
	}
}
