package edu.bu.segrelab.comets.fba.ui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import edu.bu.segrelab.comets.Comets;
import edu.bu.segrelab.comets.CometsConstants;
import edu.bu.segrelab.comets.fba.FBAParameters;
import edu.bu.segrelab.comets.ui.DoubleField;
import edu.bu.segrelab.comets.ui.IntField;
import edu.bu.segrelab.comets.ui.ParametersPanel;

@SuppressWarnings("serial")
public class DiffusionParametersPanel extends JPanel
									  implements ParametersPanel, CometsConstants
{
	private static final String BORDER 			= "Diffusion Parameters",
								BIOMASS_LBL		= "Biomass relaxation method: ",
								DIFF_CONST_LBL 	= "Default diffusion constant (cm^2/s): ",
								DIFF_STEP_LBL 	= "Number of diffusions per step: ",
								NAME			= "Diffusion";
	
	private DoubleField diffConstField;
	
	private IntField diffPerStepField;
	
	private JLabel diffConstLbl,
				   diffStepLbl,
				   biomassStyleLbl;

	private JComboBox biomassStyleComboBox;

	private FBAParameters fbaParams;
	
	public DiffusionParametersPanel(FBAParameters fbaParams)
	{
		super(new GridBagLayout());
		this.fbaParams = fbaParams;
		initPanelWidgets();
		assemblePanelWidgets();
	}

	private void assemblePanelWidgets()
	{
		/**************** BUILD PARAMETERS JPANEL *********************/
		
		/* This panel will be built as a conglomeration of multiple
		 * sub-panels. It will use a simple BoxLayout (Y-AXIS) where
		 * each sub-panel is layed out vertically.
		 */

		this.setLayout(new GridBagLayout());
		this.setBorder(BorderFactory.createTitledBorder(BORDER));
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.ipadx = 10;
		gbc.insets = new Insets(5,5,5,5);
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = gbc.weighty = 0.0;

		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.anchor = GridBagConstraints.EAST;
		this.add(diffConstLbl, gbc);
		gbc.gridx = 1;
		gbc.anchor = GridBagConstraints.WEST;
		this.add(diffConstField, gbc);
		
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.anchor = GridBagConstraints.EAST;
		this.add(diffStepLbl, gbc);
		gbc.gridx = 1;
		gbc.anchor = GridBagConstraints.WEST;
		this.add(diffPerStepField, gbc);
		
		gbc.gridx = 0;
		gbc.gridy = 2;
		this.add(biomassStyleLbl, gbc);
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridx = 1;
		this.add(biomassStyleComboBox, gbc);
	}

	private void initPanelWidgets()
	{
		diffStepLbl = new JLabel(DIFF_STEP_LBL, JLabel.LEFT);
		diffPerStepField = new IntField(fbaParams.getNumDiffusionsPerStep(), 6, false);
		
		diffConstLbl = new JLabel(DIFF_CONST_LBL, JLabel.LEFT);
		diffConstField = new DoubleField(fbaParams.getDefaultDiffusionConstant(), 6, false);
		
		biomassStyleLbl = new JLabel(BIOMASS_LBL, JLabel.LEFT);
		biomassStyleComboBox = new JComboBox(FBAParameters.BiomassMotionStyle.values());
		biomassStyleComboBox.setSelectedItem(fbaParams.getBiomassMotionStyle());
	}

	@Override
	public void applyChanges()
	{
		fbaParams.setNumDiffusionsPerStep(diffPerStepField.getIntValue());
		fbaParams.setDefaultDiffusionConstant(diffConstField.getDoubleValue());
		
		fbaParams.setBiomassMotionStyle((FBAParameters.BiomassMotionStyle)biomassStyleComboBox.getSelectedItem());
		
	}

	@Override
	public void resetChanges()
	{
		diffConstField.setValue(fbaParams.getDefaultDiffusionConstant());
		diffPerStepField.setValue(fbaParams.getNumDiffusionsPerStep());
		
		biomassStyleComboBox.setSelectedItem(fbaParams.getBiomassMotionStyle());
	}

	@Override
	public void updateState(Comets c)
	{
		// TODO Auto-generated method stub
		
	}
	
	public String getName()
	{
		return NAME;
	}

}
