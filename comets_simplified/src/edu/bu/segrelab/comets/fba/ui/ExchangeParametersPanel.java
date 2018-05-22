package edu.bu.segrelab.comets.fba.ui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import edu.bu.segrelab.comets.Comets;
import edu.bu.segrelab.comets.CometsConstants;
import edu.bu.segrelab.comets.fba.FBAParameters;
import edu.bu.segrelab.comets.fba.FBAParameters.ExchangeStyle;
import edu.bu.segrelab.comets.ui.DoubleField;
import edu.bu.segrelab.comets.ui.ParametersPanel;

@SuppressWarnings("serial")
public class ExchangeParametersPanel extends JPanel
									 implements ParametersPanel, CometsConstants
{
	private static final String	STANDARD_INFO	  = "<HTML>Max exchange fluxes are set by the amount of<BR>media / biomass * dT</HTML>",
								MONOD_INFO		  = "<HTML>Max exchange fluxes are set by calculating the Monod equation<BR>using given parameters</HTML>",
								PSEUDO_MONOD_INFO = "<HTML>Max exchange fluxes are set by calculating a simple linear<BR>approximation of the Monod equation</HTML>",
								VMAX_LBL		  = "Default Vmax: ",
								K_LBL 			  = "Default K: ",
								HILL_LBL 		  = "Default Hill coefficient: ",
								ALPHA_LBL  		  = "Default alpha: ",
								W_LBL 			  = "Default W: ",
								EXCH_LBL 		  = "Metabolite uptake style: ",
								BORDER			  = "Metabolite Uptake Parameters",
								NAME			  = "Exchange",
								MODEL_OVERRIDE    = "Override models' individual defaults?";
	
	
	private DoubleField vMaxField,
						kmField,
						hillField,
						alphaField,
						wField;
	
	private JCheckBox monodOverrideCheckBox,
					  pseudoOverrideCheckBox;
	
	private JLabel vMaxLbl,
				   kmLbl,
				   hillLbl,
				   alphaLbl,
				   wLbl,
				   standardExLbl,
				   monodExLbl,
				   pseudoMonodExLbl,
				   exchStyleLbl,
				   monodOverrideLbl,
				   pseudoOverrideLbl;

	private JComboBox exchStyleComboBox;

	private FBAParameters fbaParams;
	
	public ExchangeParametersPanel(FBAParameters fbaParams)
	{
		super(new GridBagLayout());
		this.fbaParams = fbaParams;
		initPanelWidgets();
		bindEvents();
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
		this.add(exchStyleLbl, gbc);
		
		gbc.gridx = 1;
		gbc.anchor = GridBagConstraints.WEST;
		this.add(exchStyleComboBox, gbc);
		
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.gridwidth = 2;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		this.add(standardExLbl, gbc);
		this.add(monodExLbl, gbc);
		this.add(pseudoMonodExLbl, gbc);
		
		gbc.gridwidth = 1;
		gbc.gridx = 0;
		gbc.gridy = 2;
		gbc.anchor = GridBagConstraints.EAST;
		this.add(vMaxLbl, gbc);
		gbc.gridx = 1;
		gbc.anchor = GridBagConstraints.WEST;
		this.add(vMaxField, gbc);
		
		gbc.gridx = 0;
		gbc.gridy = 3;
		gbc.anchor = GridBagConstraints.EAST;
		this.add(kmLbl, gbc);
		gbc.gridx = 1;
		gbc.anchor = GridBagConstraints.WEST;
		this.add(kmField, gbc);
		
		gbc.gridx = 0;
		gbc.gridy = 4;
		gbc.anchor = GridBagConstraints.EAST;
		this.add(hillLbl, gbc);
		gbc.gridx = 1;
		gbc.anchor = GridBagConstraints.WEST;
		this.add(hillField, gbc);
		
		gbc.gridx = 0;
		gbc.gridy = 5;
		gbc.anchor = GridBagConstraints.EAST;
		this.add(monodOverrideLbl, gbc);
		gbc.gridx = 1;
		gbc.anchor = GridBagConstraints.WEST;
		this.add(monodOverrideCheckBox, gbc);
		
		// some overlap, but only one set will be visible at a time
		
		gbc.gridx = 0;
		gbc.gridy = 2;
		gbc.anchor = GridBagConstraints.EAST;
		this.add(alphaLbl, gbc);
		gbc.gridx = 1;
		gbc.anchor = GridBagConstraints.WEST;
		this.add(alphaField, gbc);
		
		gbc.gridx = 0;
		gbc.gridy = 3;
		gbc.anchor = GridBagConstraints.EAST;
		this.add(wLbl, gbc);
		gbc.gridx = 1;
		gbc.anchor = GridBagConstraints.WEST;
		this.add(wField, gbc);
		
		gbc.gridx = 0;
		gbc.gridy = 4;
		gbc.anchor = GridBagConstraints.EAST;
		this.add(pseudoOverrideLbl, gbc);
		gbc.gridx = 1;
		gbc.anchor = GridBagConstraints.WEST;
		this.add(pseudoOverrideCheckBox, gbc);
	}

	private void bindEvents()
	{

		exchStyleComboBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
//				FBAParameters.ExchangeStyle selStyle = exchStyleDataMap.get(exchStyleComboBox.getSelectedItem());
				updateWidgetView((FBAParameters.ExchangeStyle)exchStyleComboBox.getSelectedItem());

				// kind of a hack, but it works.
				// this only works if the Parameters panel is shoved
				// into a JDialog. because that's what it gets casted as.
//				((JDialog)exchStyleComboBox.getTopLevelAncestor()).pack();
			}
		});
		
	}

	private void initPanelWidgets()
	{
		standardExLbl = new JLabel(STANDARD_INFO);
		monodExLbl = new JLabel(MONOD_INFO);
		pseudoMonodExLbl = new JLabel(PSEUDO_MONOD_INFO);
		
		vMaxField = new DoubleField(fbaParams.getDefaultVmax(), 6, false);
		kmField = new DoubleField(fbaParams.getDefaultKm(), 6, false);
		hillField = new DoubleField(fbaParams.getDefaultHill(), 6, false);
		monodOverrideCheckBox = new JCheckBox();
		
		vMaxLbl = new JLabel(VMAX_LBL, JLabel.LEFT);
		kmLbl = new JLabel(K_LBL, JLabel.LEFT);
		hillLbl = new JLabel(HILL_LBL, JLabel.LEFT);
		monodOverrideLbl = new JLabel(MODEL_OVERRIDE, JLabel.LEFT);
		
		alphaField = new DoubleField(fbaParams.getDefaultAlpha(), 6, false);
		wField = new DoubleField(fbaParams.getDefaultW(), 6, false);
		pseudoOverrideCheckBox = new JCheckBox();
		
		alphaLbl = new JLabel(ALPHA_LBL, JLabel.LEFT);
		wLbl = new JLabel(W_LBL, JLabel.LEFT);
		pseudoOverrideLbl = new JLabel(MODEL_OVERRIDE, JLabel.LEFT);

		exchStyleLbl = new JLabel(EXCH_LBL, JLabel.LEFT);

		exchStyleComboBox = new JComboBox(FBAParameters.ExchangeStyle.values());
		FBAParameters.ExchangeStyle exchangeStyle = fbaParams.getExchangeStyle();
		exchStyleComboBox.setSelectedItem(exchangeStyle);
		updateWidgetView(exchangeStyle);
	}

	private void updateWidgetView(FBAParameters.ExchangeStyle exchangeStyle)
	{
		standardExLbl.setVisible(exchangeStyle == FBAParameters.ExchangeStyle.STANDARD);
		
		monodExLbl.setVisible(exchangeStyle == FBAParameters.ExchangeStyle.MONOD);
		vMaxField.setVisible(exchangeStyle == FBAParameters.ExchangeStyle.MONOD);
		kmField.setVisible(exchangeStyle == FBAParameters.ExchangeStyle.MONOD);
		hillField.setVisible(exchangeStyle == FBAParameters.ExchangeStyle.MONOD);
		vMaxLbl.setVisible(exchangeStyle == FBAParameters.ExchangeStyle.MONOD);
		kmLbl.setVisible(exchangeStyle == FBAParameters.ExchangeStyle.MONOD);
		hillLbl.setVisible(exchangeStyle == FBAParameters.ExchangeStyle.MONOD);
		monodOverrideCheckBox.setVisible(exchangeStyle == FBAParameters.ExchangeStyle.MONOD);
		monodOverrideLbl.setVisible(exchangeStyle == FBAParameters.ExchangeStyle.MONOD);
		
		pseudoMonodExLbl.setVisible(exchangeStyle == FBAParameters.ExchangeStyle.PSEUDO_MONOD);
		alphaField.setVisible(exchangeStyle == FBAParameters.ExchangeStyle.PSEUDO_MONOD);
		wField.setVisible(exchangeStyle == FBAParameters.ExchangeStyle.PSEUDO_MONOD);
		alphaLbl.setVisible(exchangeStyle == FBAParameters.ExchangeStyle.PSEUDO_MONOD);
		wLbl.setVisible(exchangeStyle == FBAParameters.ExchangeStyle.PSEUDO_MONOD);
		pseudoOverrideCheckBox.setVisible(exchangeStyle == FBAParameters.ExchangeStyle.PSEUDO_MONOD);
		pseudoOverrideLbl.setVisible(exchangeStyle == FBAParameters.ExchangeStyle.PSEUDO_MONOD);
	}
	
	@Override
	public void applyChanges()
	{
		fbaParams.setExchangeStyle((FBAParameters.ExchangeStyle)exchStyleComboBox.getSelectedItem());
		
		boolean monodOverride = false;
		boolean pseudoOverride = false;
		if (fbaParams.getExchangeStyle() == ExchangeStyle.MONOD) monodOverride = monodOverrideCheckBox.isSelected();
		if (fbaParams.getExchangeStyle() == ExchangeStyle.PSEUDO_MONOD) pseudoOverride = monodOverrideCheckBox.isSelected();
		
		fbaParams.setDefaultVmax(vMaxField.getDoubleValue(),monodOverride);
		fbaParams.setDefaultKm(kmField.getDoubleValue(),monodOverride);
		fbaParams.setDefaultHill(hillField.getDoubleValue(),monodOverride);
		fbaParams.setDefaultAlpha(alphaField.getDoubleValue(),pseudoOverride);
		fbaParams.setDefaultW(wField.getDoubleValue(),pseudoOverride);
		fbaParams.monodOverride(monodOverride);
		fbaParams.pseudoOverride(pseudoOverride);
		
	}

	@Override
	public void resetChanges()
	{
		FBAParameters.ExchangeStyle exchangeStyle = fbaParams.getExchangeStyle();
		exchStyleComboBox.setSelectedItem(exchangeStyle);
		
		vMaxField.setValue(fbaParams.getDefaultVmax());
		kmField.setValue(fbaParams.getDefaultKm());
		hillField.setValue(fbaParams.getDefaultHill());
		alphaField.setValue(fbaParams.getDefaultAlpha());
		wField.setValue(fbaParams.getDefaultW());
		monodOverrideCheckBox.setSelected(fbaParams.getMonodOverride());
		pseudoOverrideCheckBox.setSelected(fbaParams.getPseudoOverride());
		
		updateWidgetView(exchangeStyle);
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
