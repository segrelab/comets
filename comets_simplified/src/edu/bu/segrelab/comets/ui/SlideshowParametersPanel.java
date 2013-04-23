package edu.bu.segrelab.comets.ui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import edu.bu.segrelab.comets.Comets;
import edu.bu.segrelab.comets.CometsParameters;
import edu.bu.segrelab.comets.World2D;

@SuppressWarnings("serial")
public class SlideshowParametersPanel extends JPanel
									  implements ParametersPanel
{
	private CometsParameters cParams;
	
	private static final String SLIDESHOW_RATE_LBL 		=	"Snapshot rate:",
								SLIDESHOW_NAME_LBL 		=	"Slideshow name:",
								SLIDESHOW_TYPE_LBL 		= 	"Image type:",
								SLIDESHOW_LAYER_LBL		=	"Graphics layer:",
						   		SLIDESHOW_COLOR_LBL		=	"Color value:",
				   				RELATIVE_COLOR_LBL 		=	"Relative color style",
						   		SAVE_SLIDESHOW_LBL		=	"Save slideshow",
						   		NAME					=	"Slideshow";

	private final String[]  imageTypes	=	{ "png", "jpg", "bmp" };
				   
	private JLabel		slideshowRateLbl,
			   			slideshowNameLbl,
			   			slideshowTypeLbl,
			   			slideshowColorLbl,
			   			slideshowLayerLbl;

	private IntField 	slideshowRateField;
	
	private DoubleField	slideshowColorField;

	private JComboBox 	slideshowTypeList,
						slideshowLayerList;
	
	private JTextField 	slideshowNameField;

	private JCheckBox	saveSlideshowBox,
						slideshowColorBox;
	
	public SlideshowParametersPanel(CometsParameters cParams)
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
		 * POPULATE THE SLIDESHOW PANE
		 *******************************************/
		TitledBorder slideshowBorder = BorderFactory.createTitledBorder("Slideshow");
		setBorder(slideshowBorder);

		GridBagConstraints gbc = new GridBagConstraints();

		gbc.fill = GridBagConstraints.BOTH;
		gbc.ipadx = 10;
		gbc.insets = new Insets(5,5,5,5);
		gbc.gridheight = 1;
		gbc.weightx = gbc.weighty = 0.0;

		gbc.gridwidth = 2;
		gbc.gridx = 0;
		gbc.gridy = 0;
		add(saveSlideshowBox, gbc);

		gbc.gridwidth = 1;
		gbc.gridx = 2;
		gbc.gridy = 0;
		add(slideshowRateLbl, gbc);

		gbc.gridx = 3;
		gbc.gridy = 0;
		add(slideshowRateField, gbc);

		gbc.gridx = 0;
		gbc.gridy = 1;
		add(slideshowNameLbl, gbc);

		gbc.gridx = 1;
		gbc.gridy = 1;
		add(slideshowNameField, gbc);

		gbc.gridx = 2;
		gbc.gridy = 1;
		add(slideshowTypeLbl, gbc);

		gbc.gridx = 3;
		gbc.gridy = 1;
		add(slideshowTypeList, gbc);

		gbc.gridx = 0;
		gbc.gridy = 2;
		gbc.gridwidth = 2;
		add(slideshowColorBox, gbc);
		
		gbc.gridx = 2;
		gbc.gridwidth = 1;
		add(slideshowColorLbl, gbc);
		
		gbc.gridx = 3;
		add(slideshowColorField, gbc);
		
		gbc.gridx = 0;
		gbc.gridy = 3;
		gbc.gridwidth = 2;
		add(slideshowLayerLbl, gbc);
		
		gbc.gridx = 2;
		add(slideshowLayerList, gbc);
	}

	private void bindEvents()
	{
		// Toggle the enabling of all the slideshow stuff on click.
		saveSlideshowBox.addItemListener(
				new ItemListener()
				{
					public void itemStateChanged(ItemEvent e)
					{
						slideshowRateField.setEnabled(e.getStateChange() == ItemEvent.SELECTED);
						slideshowRateLbl.setEnabled(e.getStateChange() == ItemEvent.SELECTED);
						slideshowNameField.setEnabled(e.getStateChange() == ItemEvent.SELECTED);
						slideshowTypeList.setEnabled(e.getStateChange() == ItemEvent.SELECTED);
						slideshowNameLbl.setEnabled(e.getStateChange() == ItemEvent.SELECTED);
						slideshowTypeLbl.setEnabled(e.getStateChange() == ItemEvent.SELECTED);
						slideshowColorBox.setEnabled(e.getStateChange() == ItemEvent.SELECTED);
						slideshowColorLbl.setEnabled(e.getStateChange() == ItemEvent.SELECTED && !slideshowColorBox.isSelected());
						slideshowColorField.setEnabled(e.getStateChange() == ItemEvent.SELECTED && !slideshowColorBox.isSelected());
						slideshowLayerLbl.setEnabled(e.getStateChange() == ItemEvent.SELECTED);
						slideshowLayerList.setEnabled(e.getStateChange() == ItemEvent.SELECTED);
					}
				});

		slideshowColorBox.addItemListener(
				new ItemListener()
				{
					public void itemStateChanged(ItemEvent e)
					{
						slideshowColorField.setEnabled(e.getStateChange() == ItemEvent.DESELECTED);
						slideshowColorLbl.setEnabled(e.getStateChange() == ItemEvent.DESELECTED);
					}
				});
	}

	private void initPanelWidgets()
	{
		slideshowRateLbl 	= new JLabel(SLIDESHOW_RATE_LBL, JLabel.LEFT);
		slideshowNameLbl 	= new JLabel(SLIDESHOW_NAME_LBL, JLabel.LEFT);
		slideshowTypeLbl 	= new JLabel(SLIDESHOW_TYPE_LBL, JLabel.LEFT);
		slideshowLayerLbl 	= new JLabel(SLIDESHOW_LAYER_LBL, JLabel.LEFT);
		slideshowColorLbl 	= new JLabel(SLIDESHOW_COLOR_LBL, JLabel.LEFT);

		slideshowRateField 	= new IntField(cParams.getSlideshowRate(), 3, false);

		slideshowNameField 	= new JTextField(cParams.getSlideshowName());

		slideshowColorField = new DoubleField(cParams.getSlideshowColorValue(), 6, false);

		slideshowTypeList 	= new JComboBox(imageTypes);
		slideshowTypeList.setSelectedIndex(0);

		slideshowLayerList 	= new JComboBox();
		
		saveSlideshowBox 	= new JCheckBox(SAVE_SLIDESHOW_LBL, cParams.saveSlideshow());
		slideshowColorBox	= new JCheckBox(RELATIVE_COLOR_LBL, cParams.getSlideshowColorRelative());

		// If we're not saving a slideshow, just disable all the slideshow widgets.
		slideshowRateField.setEnabled(saveSlideshowBox.isSelected());
		slideshowRateLbl.setEnabled(saveSlideshowBox.isSelected());
		slideshowNameField.setEnabled(saveSlideshowBox.isSelected());
		slideshowTypeList.setEnabled(saveSlideshowBox.isSelected());
		slideshowNameLbl.setEnabled(saveSlideshowBox.isSelected());
		slideshowTypeLbl.setEnabled(saveSlideshowBox.isSelected());
		slideshowColorBox.setEnabled(saveSlideshowBox.isSelected());
		slideshowColorLbl.setEnabled(saveSlideshowBox.isSelected() && !slideshowColorBox.isSelected());
		slideshowColorField.setEnabled(saveSlideshowBox.isSelected() && !slideshowColorBox.isSelected());
		slideshowLayerList.setEnabled(saveSlideshowBox.isSelected()); // && getWorld() != null);
		slideshowLayerLbl.setEnabled(saveSlideshowBox.isSelected());  // && getWorld() != null);		
	}

	@Override
	public void applyChanges()
	{
		cParams.saveSlideshow(saveSlideshowBox.isSelected());
		cParams.setSlideshowExt((String)slideshowTypeList.getSelectedItem());
		cParams.setSlideshowRate(slideshowRateField.getIntValue());
		cParams.setSlideshowName(slideshowNameField.getText());
		cParams.setSlideshowColorRelative(slideshowColorBox.isSelected());
		if (!slideshowColorBox.isSelected())
			cParams.setSlideshowColorValue(slideshowColorField.getDoubleValue());
		cParams.setSlideshowLayer(slideshowLayerList.getSelectedIndex());
	}

	@Override
	public void resetChanges()
	{
		slideshowRateField.setValue(cParams.getSlideshowRate());
		slideshowNameField.setText(cParams.getSlideshowName());
		slideshowColorField.setValue(cParams.getSlideshowColorValue());

		//TODO fix!
		slideshowTypeList.setSelectedItem(cParams.getSlideshowExt());

		//TODO fix!
		int curLayer = cParams.getSlideshowLayer();
		if ((curLayer > slideshowLayerList.getItemCount() || curLayer < 0) && slideshowLayerList.getItemCount() != 0)
			slideshowLayerList.setSelectedIndex(slideshowLayerList.getItemCount());
		else if (slideshowLayerList.getItemCount() > 0) 
			slideshowLayerList.setSelectedIndex(curLayer);

		saveSlideshowBox.setSelected(cParams.saveSlideshow());
		slideshowColorBox.setSelected(cParams.getSlideshowColorRelative());
		
		slideshowRateField.setEnabled(saveSlideshowBox.isSelected());
		slideshowRateLbl.setEnabled(saveSlideshowBox.isSelected());
		slideshowNameField.setEnabled(saveSlideshowBox.isSelected());
		slideshowTypeList.setEnabled(saveSlideshowBox.isSelected());
		slideshowNameLbl.setEnabled(saveSlideshowBox.isSelected());
		slideshowTypeLbl.setEnabled(saveSlideshowBox.isSelected());
		slideshowColorBox.setEnabled(saveSlideshowBox.isSelected());
		slideshowColorLbl.setEnabled(saveSlideshowBox.isSelected() && !slideshowColorBox.isSelected());
		slideshowColorField.setEnabled(saveSlideshowBox.isSelected() && !slideshowColorBox.isSelected());
		slideshowLayerList.setEnabled(saveSlideshowBox.isSelected());
		slideshowLayerLbl.setEnabled(saveSlideshowBox.isSelected());
	}

	@Override
	public void updateState(Comets c)
	{
		/*
		 * If there's a World loaded, populate the slideshowLayerList with the names of all
		 * the medium components and such, followed with biomass. Finally, set it to be
		 * on the selected layer.
		 */
		World2D world = c.getWorld();
		if (world != null)
		{
			String[] mediaNames = world.getMediaNames();
			slideshowLayerList.removeAllItems();
			for (int i=0; i<mediaNames.length; i++)
			{
				slideshowLayerList.addItem(mediaNames[i]);
			}
			slideshowLayerList.addItem("Biomass");
	
			int curLayer = cParams.getSlideshowLayer();
			if (curLayer > mediaNames.length)
			{
				curLayer = 0;
			}
			slideshowLayerList.setSelectedIndex(curLayer);
		}
	}
	
	public String getName() 
	{
		return NAME;
	}
}
