package edu.bu.segrelab.comets.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;

import edu.bu.segrelab.comets.Comets;
import edu.bu.segrelab.comets.CometsParameters;

@SuppressWarnings("serial")
public class DisplayParametersPanel extends JPanel
									implements ParametersPanel
{
	private CometsParameters cParams;

	private static final String		CYCLE_TIME_LBL			=	"Show cycle time",
		   							CYCLE_COUNT_LBL 		=	"Show cycle count",
		   							PIXEL_SCALE_LBL			=	"Size of each box (pixels):",
					   				BG_COLOR_LBL			=	"Background color:",
					   				BARRIER_COLOR_LBL		=	"Barrier color:",
					   				SET_LBL					=	"Set",
					   				SHOW_GRAPHICS_LBL 		= 	"Show graphics",
					   				PAUSE_CYCLE_LBL 		=	"Pause on cycle",
					   				NAME					=	"Display";
	
	private JLabel 		pixelScaleLbl,
				   		bgColorLbl,
				   		barColorLbl;

	private IntField 	pixelScaleField;
	
	private JCheckBox	showGraphicsBox,
						showTimeBox,
						showCountBox,
						pauseOnCycleBox;
	
	private JPanel		bgColorPreview,
						barColorPreview;
	
	private JButton		bgColorButton,
						barColorButton;

	public DisplayParametersPanel(CometsParameters cParams)
	{
		super(new GridBagLayout());
		this.cParams = cParams;
		initPanelWidgets();
		bindEvents();
		assemblePanelWidgets();
	}
	
	private void assemblePanelWidgets()
	{
		/*************************************************
		 * POPULATE THE GENERAL DISPLAY PANE
		 *************************************************/
		Border generalBorder = BorderFactory.createTitledBorder("General Display");
		setBorder(generalBorder);

		GridBagConstraints gbc = new GridBagConstraints();

		gbc = new GridBagConstraints();

		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.ipadx = 10;
		gbc.insets = new Insets(5,5,5,5);
		gbc.gridwidth = 3;
		gbc.gridheight = 1;
		gbc.weightx = gbc.weighty = 0.0;
		gbc.gridx = 0;
		gbc.gridy = 0;
		add(showGraphicsBox, gbc);

		gbc.gridx = 3;

		gbc.gridx = 0;
		gbc.gridy = 1;
		add(showTimeBox, gbc);

		gbc.gridx = 3;
		gbc.gridy = 1;
		add(showCountBox, gbc);

		gbc.gridx = 0;
		gbc.gridy = 2;
		add(pauseOnCycleBox, gbc);

		gbc.gridwidth = 1;
		gbc.gridx = 3;
		add(pixelScaleLbl, gbc);
		
		gbc.gridwidth = 1;
		gbc.gridx = 4;
		add(pixelScaleField, gbc);
		
		gbc.gridwidth = 1;
		gbc.gridx = 0;
		gbc.gridy = 3;
		add(bgColorLbl, gbc);

		gbc.gridx = 1;
		add(bgColorPreview, gbc);

		gbc.gridx = 2;
		add(bgColorButton, gbc);

		gbc.gridx = 3;
		add(barColorLbl, gbc);

		gbc.gridx = 4;
		add(barColorPreview, gbc);

		gbc.gridx = 5;
		add(barColorButton, gbc);
		
	}

	private void bindEvents()
	{
		bgColorButton.addActionListener(
				new ActionListener() 
				{
					public void actionPerformed(ActionEvent e)
					{
						// spawn a JColorChooser.
						Color newColor = JColorChooser.showDialog(DisplayParametersPanel.this, "Choose a background color", bgColorPreview.getBackground());
						if (newColor != null)
							bgColorPreview.setBackground(newColor);
					}
				});

		barColorButton.addActionListener(
				new ActionListener() 
				{
					public void actionPerformed(ActionEvent e)
					{
						// spawn a JColorChooser.
						Color newColor = JColorChooser.showDialog(DisplayParametersPanel.this, "Choose a barrier color", bgColorPreview.getBackground());
						if (newColor != null)
							barColorPreview.setBackground(newColor);

					}
				});
	}

	private void initPanelWidgets()
	{
		pixelScaleLbl 		= new JLabel(PIXEL_SCALE_LBL, JLabel.LEFT);
		bgColorLbl 			= new JLabel(BG_COLOR_LBL, JLabel.LEFT);
		barColorLbl 		= new JLabel(BARRIER_COLOR_LBL, JLabel.LEFT);

		pixelScaleField 	= new IntField(cParams.getPixelScale(), 3, false);
		
		showGraphicsBox 	= new JCheckBox(SHOW_GRAPHICS_LBL, cParams.showGraphics());
		
		showTimeBox			= new JCheckBox(CYCLE_TIME_LBL, cParams.showCycleTime());
		showCountBox 		= new JCheckBox(CYCLE_COUNT_LBL, cParams.showCycleCount());
		pauseOnCycleBox 	= new JCheckBox(PAUSE_CYCLE_LBL, cParams.pauseOnStep());

		// Make a little box for a background color preview.
		bgColorPreview = new JPanel();
		bgColorPreview.setBackground(new Color(cParams.getBackgroundColor()));
		bgColorPreview.setPreferredSize(new Dimension(20, 20));

		// Make a little box for the barrier color preview.
		barColorPreview = new JPanel();
		barColorPreview.setBackground(new Color(cParams.getBarrierColor()));
		barColorPreview.setPreferredSize(new Dimension(20, 20));

		// To choose a new background color, use a JColorChooser.
		bgColorButton = new JButton(SET_LBL);
		// Use a JColorChooser for the barrier color, too.
		barColorButton = new JButton(SET_LBL);
	}

	@Override
	public void applyChanges()
	{
		cParams.showGraphics(showGraphicsBox.isSelected());
		cParams.showCycleTime(showTimeBox.isSelected());
		cParams.showCycleCount(showCountBox.isSelected());
		cParams.pauseOnStep(pauseOnCycleBox.isSelected());
		cParams.setBackgroundColor(bgColorPreview.getBackground());
		cParams.setBarrierColor(barColorPreview.getBackground());
		cParams.setPixelScale(pixelScaleField.getIntValue());
	}

	@Override
	public void resetChanges()
	{
		pixelScaleField.setValue(cParams.getPixelScale());
		showGraphicsBox.setSelected(cParams.showGraphics());
		showTimeBox.setSelected(cParams.showCycleTime());
		showCountBox.setSelected(cParams.showCycleCount());
		pauseOnCycleBox.setSelected(cParams.pauseOnStep());
		bgColorPreview.setBackground(new Color(cParams.getBackgroundColor()));
		barColorPreview.setBackground(new Color(cParams.getBarrierColor()));
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
