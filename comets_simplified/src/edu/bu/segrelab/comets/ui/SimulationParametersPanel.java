package edu.bu.segrelab.comets.ui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;

import edu.bu.segrelab.comets.Comets;
import edu.bu.segrelab.comets.CometsConstants;
import edu.bu.segrelab.comets.CometsParameters;

@SuppressWarnings("serial")
public class SimulationParametersPanel extends JPanel
									   implements ParametersPanel, CometsConstants
{
	private CometsParameters cParams;

	private static final String	MAX_CYCLES_LBL			=	"Maximum simulation cycles:",
						   		TIME_STEP_LBL			=	"Time step (h):",
						   		DEATH_RATE_LBL			=	"Death rate (0-1, fraction per h):",
						   		SPACE_WIDTH_LBL			=	"Space width (cm):",
				   				ALLOW_OVERLAP_LBL 		= 	"Allow cell overlap",
				   				TORUS_WORLD_LBL 		= 	"Make world toroidal",
				   				UNLIMITED_CYCLES_LBL	=	"Unlimited cycles:",
				   				MIN_BIOMASS_LBL			=	"Lower biomass threshold (g):",
				   				MAX_BIOMASS_LBL			=	"Maximum biomass (g):",
				   				NAME					=	"Simulation",
								SIMULATE_ACTIVATION_LBL =   "Simulate activation",
								ACTIVATION_RATE_LBL     =   "Activation rate:";
	
	private JLabel 		maxCyclesLbl,
				   		timeStepLbl,
				   		deathRateLbl,
				   		spaceWidthLbl,
				   		minBiomassLbl,
				   		maxBiomassLbl,
				   		activationRateLbl;

	private IntField 	maxCyclesField;
	
	private DoubleField timeStepField,
						deathRateField,
						spaceWidthField,
						minBiomassField,
						maxBiomassField,
						activationRateField;

	private JCheckBox	allowOverlapBox,
						torusWorldBox,
						unlimitedCyclesBox,
						simulateActivationBox;
	
	public SimulationParametersPanel(CometsParameters cParams)
	{
		super(new GridBagLayout());
		this.cParams = cParams;
		initPanelWidgets();
		bindEvents();
		assemblePanelWidgets();
	}
	
	private void assemblePanelWidgets()
	{

		/*******************************************
		 * POPULATE THE SIMULATION PARAMETERS PANE
		 *******************************************/
		Border simBorder = BorderFactory.createTitledBorder("Simulation Parameters");
		setBorder(simBorder);
		GridBagConstraints gbc = new GridBagConstraints();
		gbc = new GridBagConstraints();

		gbc.fill = GridBagConstraints.BOTH;
		gbc.ipadx = 10;
		gbc.insets = new Insets(5,5,5,5);
		gbc.gridwidth = 2;
		gbc.gridheight = 1;
		gbc.weightx = gbc.weighty = 0.0;
		gbc.gridx = 0;
		gbc.gridy = 0;
		this.add(allowOverlapBox, gbc);
		
//		gbc.gridx = 2;
//		this.add(torusWorldBox, gbc);
		
		gbc.gridx = 0;
		gbc.gridy++;
		gbc.gridwidth = 1;
		this.add(minBiomassLbl, gbc);
		
		gbc.gridx++;
		this.add(minBiomassField, gbc);
		
		gbc.gridx++;
		this.add(maxBiomassLbl, gbc);
		
		gbc.gridx++;
		this.add(maxBiomassField, gbc);
		
		gbc.gridx = 0;
		gbc.gridy++;
		gbc.gridwidth = 1;
		this.add(timeStepLbl, gbc);
		
		gbc.gridx = 1;
		this.add(timeStepField, gbc);
		
		gbc.gridx = 2;
		this.add(deathRateLbl, gbc);
		
		gbc.gridx = 3;
		this.add(deathRateField, gbc);
		
		gbc.gridx = 0;
		gbc.gridy++;
		this.add(spaceWidthLbl, gbc);
		
		gbc.gridx = 1;
		this.add(spaceWidthField, gbc);

		gbc.gridx = 0;
		gbc.gridy++;
		this.add(unlimitedCyclesBox, gbc);
		
		gbc.gridx = 1;
		gbc.gridwidth = 2;
		gbc.anchor = GridBagConstraints.EAST;
		this.add(maxCyclesLbl, gbc);
		
		gbc.gridx = 3;
		gbc.gridwidth = 1;
		gbc.anchor = GridBagConstraints.WEST;
		this.add(maxCyclesField, gbc);
		
		gbc.gridx = 0;
		gbc.gridy++;
		this.add(simulateActivationBox, gbc);
		
		gbc.gridx = 1;
		gbc.gridwidth = 2;
		gbc.anchor = GridBagConstraints.EAST;
		this.add(activationRateLbl, gbc);
		
		gbc.gridx = 3;
		gbc.gridwidth = 1;
		gbc.anchor = GridBagConstraints.WEST;
		this.add(activationRateField, gbc);
		
		
		
	}

	private void bindEvents()
	{
		// When clicking this box off, the user should be able to choose the number of maximum cycles.
		// When on, that choice is disabled
		unlimitedCyclesBox.addItemListener(
			new ItemListener() {
				public void itemStateChanged(ItemEvent e)
				{
					maxCyclesField.setEnabled(e.getStateChange() == ItemEvent.DESELECTED);
					maxCyclesLbl.setEnabled(e.getStateChange() == ItemEvent.DESELECTED);
				}
			}
		);
		
		if (cParams.getMaxCycles() != UNLIMITED_CYCLES)
			maxCyclesField.setText(String.valueOf(cParams.getMaxCycles()));
		else
		{
			maxCyclesField.setEnabled(false);
			maxCyclesLbl.setEnabled(false);
		}
		
		
		//When clicking this box on, the user should be able to enter the activation rate.
		//When off, the choice is disabled
		simulateActivationBox.addItemListener(
			new ItemListener() {
				public void itemStateChanged(ItemEvent e)
				{
					activationRateField.setEnabled(e.getStateChange() == ItemEvent.SELECTED);
					activationRateLbl.setEnabled(e.getStateChange() == ItemEvent.SELECTED);
				}
			}
		);
		
		if (cParams.getSimulateActivation())
			activationRateField.setText(String.valueOf(cParams.getActivateRate()));
		else
		{
			activationRateField.setEnabled(false);
			activationRateLbl.setEnabled(false);
		}
		
	}

	private void initPanelWidgets()
	{
		maxCyclesLbl 		= new JLabel(MAX_CYCLES_LBL, JLabel.LEFT);
		timeStepLbl 		= new JLabel(TIME_STEP_LBL, JLabel.LEFT);
		deathRateLbl 		= new JLabel(DEATH_RATE_LBL, JLabel.LEFT);
		spaceWidthLbl		= new JLabel(SPACE_WIDTH_LBL, JLabel.LEFT);
		minBiomassLbl		= new JLabel(MIN_BIOMASS_LBL, JLabel.LEFT);
		maxBiomassLbl		= new JLabel(MAX_BIOMASS_LBL, JLabel.LEFT);
		activationRateLbl   = new JLabel(ACTIVATION_RATE_LBL, JLabel.LEFT);

		maxCyclesField 		= new IntField(false);

		timeStepField 		= new DoubleField(cParams.getTimeStep(), 6, false);
		deathRateField 		= new DoubleField(cParams.getDeathRate(), 6, false);
		spaceWidthField 	= new DoubleField(cParams.getSpaceWidth(), 6, false);
		minBiomassField 	= new DoubleField(cParams.getMinSpaceBiomass(), 6, false);
		maxBiomassField 	= new DoubleField(cParams.getMaxSpaceBiomass(), 6, false);
		activationRateField = new DoubleField(cParams.getActivateRate(), 6, false);

		allowOverlapBox 	= new JCheckBox(ALLOW_OVERLAP_LBL, cParams.allowCellOverlap());
		torusWorldBox 		= new JCheckBox(TORUS_WORLD_LBL, cParams.isToroidalGrid());
		torusWorldBox.setEnabled(false);
		
		unlimitedCyclesBox 	= new JCheckBox(UNLIMITED_CYCLES_LBL, cParams.getMaxCycles() == UNLIMITED_CYCLES);
		simulateActivationBox = new JCheckBox(SIMULATE_ACTIVATION_LBL, cParams.getSimulateActivation());
	}

	@Override
	public void applyChanges()
	{
		cParams.allowCellOverlap(allowOverlapBox.isSelected());
		cParams.setToroidalGrid(torusWorldBox.isSelected());

		cParams.setTimeStep(timeStepField.getDoubleValue());
		cParams.setDeathRate(deathRateField.getDoubleValue());
		cParams.setSpaceWidth(spaceWidthField.getDoubleValue());
		
		// setting min and max biomass per space at the same time is tricky.
		// first, set the max to be infinity.
		// then set the min.
		// then set the max.
		cParams.setMaxSpaceBiomass(Double.MAX_VALUE);
		cParams.setMinSpaceBiomass(minBiomassField.getDoubleValue());
		cParams.setMaxSpaceBiomass(maxBiomassField.getDoubleValue());
		
		if (unlimitedCyclesBox.isSelected())
			cParams.setMaxCycles(UNLIMITED_CYCLES);
		else
		{
			int max = maxCyclesField.getIntValue();
			if (max == 0)
				max = 1;
			cParams.setMaxCycles(max);
		}
		
		if (simulateActivationBox.isSelected())
		{
			cParams.setSimulateActivation(true);
			double rate = activationRateField.getDoubleValue();
			cParams.setActivateRate(rate);
		}
		else
		{
			cParams.setSimulateActivation(false);
		}
		
	}

	@Override
	public void resetChanges()
	{
		maxCyclesField.setValue(cParams.getMaxCycles());
		activationRateField.setValue(cParams.getActivateRate());
		
		timeStepField.setValue(cParams.getTimeStep());
		deathRateField.setValue(cParams.getDeathRate());
		spaceWidthField.setValue(cParams.getSpaceWidth());
		minBiomassField.setValue(cParams.getMinSpaceBiomass());
		maxBiomassField.setValue(cParams.getMaxSpaceBiomass());

		allowOverlapBox.setSelected(cParams.allowCellOverlap());
		unlimitedCyclesBox.setSelected(cParams.getMaxCycles() == UNLIMITED_CYCLES);
		simulateActivationBox.setSelected(cParams.getSimulateActivation());
		
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
