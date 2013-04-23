package edu.bu.segrelab.comets.fba.ui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import edu.bu.segrelab.comets.Comets;
import edu.bu.segrelab.comets.CometsConstants;
import edu.bu.segrelab.comets.fba.FBAParameters;
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
								NAME			  = "Exchange";
	
	private DoubleField vMaxField,
						kmField,
						hillField,
						alphaField,
						wField;
	
	private JLabel vMaxLbl,
				   kmLbl,
				   hillLbl,
				   alphaLbl,
				   wLbl,
				   standardExLbl,
				   monodExLbl,
				   pseudoMonodExLbl,
				   exchStyleLbl;

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
		
		vMaxLbl = new JLabel(VMAX_LBL, JLabel.LEFT);
		kmLbl = new JLabel(K_LBL, JLabel.LEFT);
		hillLbl = new JLabel(HILL_LBL, JLabel.LEFT);
		
		alphaField = new DoubleField(fbaParams.getDefaultAlpha(), 6, false);
		wField = new DoubleField(fbaParams.getDefaultW(), 6, false);
		
		alphaLbl = new JLabel(ALPHA_LBL, JLabel.LEFT);
		wLbl = new JLabel(W_LBL, JLabel.LEFT);

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
		
		pseudoMonodExLbl.setVisible(exchangeStyle == FBAParameters.ExchangeStyle.PSEUDO_MONOD);
		alphaField.setVisible(exchangeStyle == FBAParameters.ExchangeStyle.PSEUDO_MONOD);
		wField.setVisible(exchangeStyle == FBAParameters.ExchangeStyle.PSEUDO_MONOD);
		alphaLbl.setVisible(exchangeStyle == FBAParameters.ExchangeStyle.PSEUDO_MONOD);
		wLbl.setVisible(exchangeStyle == FBAParameters.ExchangeStyle.PSEUDO_MONOD);

	}
	
	@Override
	public void applyChanges()
	{
		fbaParams.setExchangeStyle((FBAParameters.ExchangeStyle)exchStyleComboBox.getSelectedItem());
		fbaParams.setDefaultVmax(vMaxField.getDoubleValue());
		fbaParams.setDefaultKm(kmField.getDoubleValue());
		fbaParams.setDefaultHill(hillField.getDoubleValue());
		fbaParams.setDefaultAlpha(alphaField.getDoubleValue());
		fbaParams.setDefaultW(wField.getDoubleValue());
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
