package edu.bu.segrelab.comets.ui;

import java.awt.Dimension;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import edu.bu.segrelab.comets.Comets;
import edu.bu.segrelab.comets.CometsConstants;
import edu.bu.segrelab.comets.CometsParameters;
import edu.bu.segrelab.comets.event.CometsLoadEvent;
import edu.bu.segrelab.comets.event.CometsLoadListener;

@SuppressWarnings("serial")
public class CometsParametersPanel extends JPanel
								   implements CometsConstants, ParametersPanel
{
	private Map<String, ParametersPanel> panelMap;
	private Map<String, ParametersPanel> packagePanelMap;
	private JList panelList;
	private JScrollPane listPane,
						paramPanelPane;
	private JSplitPane splitPane;
	private Dimension minListSize,
					  minPanelSize,
					  prefDimension;
	
	public CometsParametersPanel(Comets c, CometsParameters cParams)
	{
		super();
		
		panelMap = new HashMap<String, ParametersPanel>();
		
		DisplayParametersPanel dispPanel = new DisplayParametersPanel(cParams);
		SimulationParametersPanel simPanel = new SimulationParametersPanel(cParams);
		SlideshowParametersPanel slidePanel = new SlideshowParametersPanel(cParams);
		
		panelMap.put(dispPanel.getName(), dispPanel);
		panelMap.put(simPanel.getName(), simPanel);
		panelMap.put(slidePanel.getName(), slidePanel);
		
		minListSize = new Dimension(100, 50);
		minPanelSize = new Dimension(700, 200);
		prefDimension = new Dimension(800, 250);
		
		initPanelWidgets();
		bindEvents();
		assemblePanelWidgets();
		
		updateVisiblePanel(panelMap.get(panelList.getSelectedValue()));
		c.addCometsLoadListener(new CometsLoadListener() {
			public void cometsLoadPerformed(CometsLoadEvent e)
			{
				resetChanges();
			}
		});
	}
	
	public void setPackageParametersPanels(Map<String, ParametersPanel> packagePanelMap)
	{
		if (this.packagePanelMap != null && this.packagePanelMap.size() != 0)
		{
			for (String name : this.packagePanelMap.keySet())
			{
				panelMap.remove(name);
			}
		}
		
		this.packagePanelMap = packagePanelMap;
		panelMap.putAll(packagePanelMap);
		
		initPanelList();
	}
	
	private void assemblePanelWidgets()
	{
		listPane.setMinimumSize(minListSize);
		listPane.setMaximumSize(minListSize);
		paramPanelPane.setMinimumSize(minPanelSize);
		
		splitPane.setPreferredSize(prefDimension);
		this.add(splitPane);

	}
	
	private void bindEvents()
	{
		panelList.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				updateVisiblePanel(panelMap.get(panelList.getSelectedValue()));
			}
		});
	}
	
	private void initPanelList()
	{
		String[] panelNames = panelMap.keySet().toArray(new String[0]);
		Arrays.sort(panelNames);
		panelList.setListData(panelNames);
		panelList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		panelList.setSelectedIndex(0);
	}
	
	private void initPanelWidgets()
	{
		panelList = new JList();
		initPanelList();
		
		listPane = new JScrollPane(panelList);
		
		paramPanelPane = new JScrollPane();
		
		splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, listPane, paramPanelPane);
		splitPane.setOneTouchExpandable(false);
//		splitPane.set
	}

	private void updateVisiblePanel(ParametersPanel parametersPanel)
	{
		splitPane.setRightComponent((JPanel)parametersPanel);
//		paramPanelPane.removeAll();
//		paramPanelPane.add((JPanel)parametersPanel);
//		paramPanelPane.revalidate();
	}

	@Override
	public void applyChanges()
	{
		for (ParametersPanel p : panelMap.values())
			p.applyChanges();
	}

	@Override
	public void resetChanges()
	{
		for (ParametersPanel p : panelMap.values())
			p.resetChanges();
		panelList.setSelectedIndex(0);
	}

	@Override
	public void updateState(Comets c)
	{
		for (ParametersPanel p : panelMap.values())
			p.updateState(c);
	}
	
//
//	private String 		SLIDESHOW_RATE_LBL 		=	"Snapshot rate:",
//						SLIDESHOW_NAME_LBL 		=	"Slideshow name:",
//				   		SLIDESHOW_TYPE_LBL 		= 	"Image type:",
//				   		SLIDESHOW_LAYER_LBL		=	"Graphics layer:",
//				   		SLIDESHOW_COLOR_LBL		=	"Color value:",
//				   		MAX_CYCLES_LBL			=	"Maximum simulation cycles:",
//				   		DEFAULT_DIFF_CONST_LBL	=	"Default diffusion const.:",
//				   		PIXEL_SCALE_LBL			=	"Size of each box (pixels):",
//				   		TIME_STEP_LBL			=	"Time step (h):",
//				   		DEATH_RATE_LBL			=	"Death rate (0-1, fraction per h):",
//				   		SPACE_WIDTH_LBL			=	"Space width (cm):",
//		   				SHOW_GRAPHICS_LBL 		= 	"Show graphics",
//		   				ALLOW_OVERLAP_LBL 		= 	"Allow cell overlap",
//		   				TORUS_WORLD_LBL 		= 	"Make world toroidal",
//		   				CYCLE_TIME_LBL			=	"Show cycle time",
//		   				CYCLE_COUNT_LBL 		=	"Show cycle count",
//		   				SAVE_SLIDESHOW_LBL 		=	"Save slideshow",
//		   				RELATIVE_COLOR_LBL 		=	"Relative color style",
//		   				SHOW_DISPLAY_TEXT 		=	"Show display text",
//		   				PAUSE_CYCLE_LBL 		=	"Pause on cycle",
//		   				BG_COLOR_LBL			=	"Background color:",
//		   				BARRIER_COLOR_LBL		=	"Barrier color:",
//		   				UNLIMITED_CYCLES_LBL	=	"Unlimited cycles:",
//		   				SET_LBL					=	"Set";
//	
//	private String[] 	imageTypes				=	{ "png", "jpg", "bmp" };
//				   
//	private JLabel 		slideshowRateLbl,
//				   		slideshowNameLbl,
//				   		slideshowTypeLbl,
//				   		slideshowColorLbl,
//				   		slideshowLayerLbl,
//				   		maxCyclesLbl,
//				   		pixelScaleLbl,
//				   		timeStepLbl,
//				   		deathRateLbl,
//				   		spaceWidthLbl,
//				   		bgColorLbl,
//				   		barColorLbl;
//
//	private IntField 	pixelScaleField,
//					 	slideshowRateField,
//					 	maxCyclesField;
//	
//	private DoubleField timeStepField,
//						deathRateField,
//						spaceWidthField,
//						minBiomassField,
//						maxBiomassField,
//						slideshowColorField;
//
//	private JComboBox 	slideshowTypeList,
//						slideshowLayerList;
//	
//	private JTextField 	slideshowNameField;
//
//	private JCheckBox	showGraphicsBox,
//						allowOverlapBox,
//						torusWorldBox,
//						showTimeBox,
//						showCountBox,
//						saveSlideshowBox,
//						slideshowColorBox,
//						showDisplayTextBox,
//						pauseOnCycleBox,
//						unlimitedCyclesBox;
//	
//	private JPanel		bgColorPreview,
//						barColorPreview;
//	
//	private JButton		bgColorButton,
//						barColorButton;
//						
//	public CometsParametersPanel(CometsParameters cParams)
//	{
//		super();
//		
//		this.cParams = cParams;
//		
//		initPanelWidgets();
//		bindEvents();
//		assemblePanelWidgets();
//	}
//	
//	private void initPanelWidgets()
//	{
//		slideshowRateLbl 	= new JLabel(SLIDESHOW_RATE_LBL, JLabel.LEFT);
//		slideshowNameLbl 	= new JLabel(SLIDESHOW_NAME_LBL, JLabel.LEFT);
//		slideshowTypeLbl 	= new JLabel(SLIDESHOW_TYPE_LBL, JLabel.LEFT);
//		maxCyclesLbl 		= new JLabel(MAX_CYCLES_LBL, JLabel.LEFT);
//		pixelScaleLbl 		= new JLabel(PIXEL_SCALE_LBL, JLabel.LEFT);
//		timeStepLbl 		= new JLabel(TIME_STEP_LBL, JLabel.LEFT);
//		deathRateLbl 		= new JLabel(DEATH_RATE_LBL, JLabel.LEFT);
//		spaceWidthLbl		= new JLabel(SPACE_WIDTH_LBL, JLabel.LEFT);
//		slideshowLayerLbl 	= new JLabel(SLIDESHOW_LAYER_LBL, JLabel.LEFT);
//		slideshowColorLbl 	= new JLabel(SLIDESHOW_COLOR_LBL, JLabel.LEFT);
//		bgColorLbl 			= new JLabel(BG_COLOR_LBL, JLabel.LEFT);
//		barColorLbl 		= new JLabel(BARRIER_COLOR_LBL, JLabel.LEFT);
//
//		pixelScaleField 	= new IntField(cParams.getPixelScale(), 3, false);
//		slideshowRateField 	= new IntField(cParams.getSlideshowRate(), 3, false);
//		maxCyclesField 		= new IntField(false);
//
//		slideshowNameField 	= new JTextField(cParams.getSlideshowName());
//
//		timeStepField 		= new DoubleField(cParams.getTimeStep(), 6, false);
//		deathRateField 		= new DoubleField(cParams.getDeathRate(), 6, false);
//		spaceWidthField 	= new DoubleField(cParams.getSpaceWidth(), 6, false);
//		minBiomassField 	= new DoubleField(cParams.getMinSpaceBiomass(), 6, false);
//		maxBiomassField 	= new DoubleField(cParams.getMaxSpaceBiomass(), 6, false);
//		slideshowColorField = new DoubleField(cParams.getSlideshowColorValue(), 6, false);
//
//		slideshowTypeList 	= new JComboBox(imageTypes);
//		slideshowTypeList.setSelectedIndex(0);
//
//		slideshowLayerList 	= new JComboBox();
//		
//		showGraphicsBox 	= new JCheckBox(SHOW_GRAPHICS_LBL, cParams.showGraphics());
//		allowOverlapBox 	= new JCheckBox(ALLOW_OVERLAP_LBL, cParams.allowCellOverlap());
//		torusWorldBox 		= new JCheckBox(TORUS_WORLD_LBL, cParams.isToroidalGrid());
//		torusWorldBox.setEnabled(false);
//		
//		showTimeBox			= new JCheckBox(CYCLE_TIME_LBL, cParams.showCycleTime());
//		showCountBox 		= new JCheckBox(CYCLE_COUNT_LBL, cParams.showCycleCount());
//		saveSlideshowBox 	= new JCheckBox(SAVE_SLIDESHOW_LBL, cParams.saveSlideshow());
//		slideshowColorBox	= new JCheckBox(RELATIVE_COLOR_LBL, cParams.getSlideshowColorRelative());
//		showDisplayTextBox 	= new JCheckBox(SHOW_DISPLAY_TEXT, cParams.showText());
//		pauseOnCycleBox 	= new JCheckBox(PAUSE_CYCLE_LBL, cParams.pauseOnStep());
//		unlimitedCyclesBox 	= new JCheckBox(UNLIMITED_CYCLES_LBL, cParams.getMaxCycles() == UNLIMITED_CYCLES);
//
//
//		// Make a little box for a background color preview.
//		bgColorPreview = new JPanel();
//		bgColorPreview.setBackground(new Color(cParams.getBackgroundColor()));
//		bgColorPreview.setPreferredSize(new Dimension(20, 20));
//
//		// Make a little box for the barrier color preview.
//		barColorPreview = new JPanel();
//		barColorPreview.setBackground(new Color(cParams.getBarrierColor()));
//		barColorPreview.setPreferredSize(new Dimension(20, 20));
//
//		// To choose a new background color, use a JColorChooser.
//		bgColorButton = new JButton(SET_LBL);
//		// Use a JColorChooser for the barrier color, too.
//		barColorButton = new JButton(SET_LBL);
//		
//		// If we're not saving a slideshow, just disable all the slideshow widgets.
//		slideshowRateField.setEnabled(saveSlideshowBox.isSelected());
//		slideshowRateLbl.setEnabled(saveSlideshowBox.isSelected());
//		slideshowNameField.setEnabled(saveSlideshowBox.isSelected());
//		slideshowTypeList.setEnabled(saveSlideshowBox.isSelected());
//		slideshowNameLbl.setEnabled(saveSlideshowBox.isSelected());
//		slideshowTypeLbl.setEnabled(saveSlideshowBox.isSelected());
//		slideshowColorBox.setEnabled(saveSlideshowBox.isSelected());
//		slideshowColorLbl.setEnabled(saveSlideshowBox.isSelected() && !slideshowColorBox.isSelected());
//		slideshowColorField.setEnabled(saveSlideshowBox.isSelected() && !slideshowColorBox.isSelected());
//		slideshowLayerList.setEnabled(saveSlideshowBox.isSelected()); // && getWorld() != null);
//		slideshowLayerLbl.setEnabled(saveSlideshowBox.isSelected());  // && getWorld() != null);
//	}
//	
//	private void bindEvents()
//	{
//		// When clicking this box off, the user should be able to choose the number of maximum cycles.
//		// When on, that choice is disabled
//		unlimitedCyclesBox.addItemListener(
//			new ItemListener() {
//				public void itemStateChanged(ItemEvent e)
//				{
//					maxCyclesField.setEnabled(e.getStateChange() == ItemEvent.DESELECTED);
//					maxCyclesLbl.setEnabled(e.getStateChange() == ItemEvent.DESELECTED);
//				}
//			}
//		);
//		
//		if (cParams.getMaxCycles() != UNLIMITED_CYCLES)
//			maxCyclesField.setText(String.valueOf(cParams.getMaxCycles()));
//		else
//		{
//			maxCyclesField.setEnabled(false);
//			maxCyclesLbl.setEnabled(false);
//		}
//		
//		bgColorButton.addActionListener(
//				new ActionListener() 
//				{
//					public void actionPerformed(ActionEvent e)
//					{
//						// spawn a JColorChooser.
//						Color newColor = JColorChooser.showDialog(CometsParametersPanel.this, "Choose a background color", bgColorPreview.getBackground());
//						if (newColor != null)
//							bgColorPreview.setBackground(newColor);
//					}
//				});
//
//		barColorButton.addActionListener(
//				new ActionListener() 
//				{
//					public void actionPerformed(ActionEvent e)
//					{
//						// spawn a JColorChooser.
//						Color newColor = JColorChooser.showDialog(CometsParametersPanel.this, "Choose a barrier color", bgColorPreview.getBackground());
//						if (newColor != null)
//							barColorPreview.setBackground(newColor);
//
//					}
//				});
//		
//		// Toggle the enabling of all the slideshow stuff on click.
//		saveSlideshowBox.addItemListener(
//				new ItemListener()
//				{
//					public void itemStateChanged(ItemEvent e)
//					{
//						slideshowRateField.setEnabled(e.getStateChange() == ItemEvent.SELECTED);
//						slideshowRateLbl.setEnabled(e.getStateChange() == ItemEvent.SELECTED);
//						slideshowNameField.setEnabled(e.getStateChange() == ItemEvent.SELECTED);
//						slideshowTypeList.setEnabled(e.getStateChange() == ItemEvent.SELECTED);
//						slideshowNameLbl.setEnabled(e.getStateChange() == ItemEvent.SELECTED);
//						slideshowTypeLbl.setEnabled(e.getStateChange() == ItemEvent.SELECTED);
//						slideshowColorBox.setEnabled(e.getStateChange() == ItemEvent.SELECTED);
//						slideshowColorLbl.setEnabled(e.getStateChange() == ItemEvent.SELECTED && !slideshowColorBox.isSelected());
//						slideshowColorField.setEnabled(e.getStateChange() == ItemEvent.SELECTED && !slideshowColorBox.isSelected());
//						slideshowLayerLbl.setEnabled(e.getStateChange() == ItemEvent.SELECTED);
//						slideshowLayerList.setEnabled(e.getStateChange() == ItemEvent.SELECTED);
//					}
//				});
//
//		slideshowColorBox.addItemListener(
//				new ItemListener()
//				{
//					public void itemStateChanged(ItemEvent e)
//					{
//						slideshowColorField.setEnabled(e.getStateChange() == ItemEvent.DESELECTED);
//						slideshowColorLbl.setEnabled(e.getStateChange() == ItemEvent.DESELECTED);
//					}
//				});
//		
//
//	}
//	
//	private void assemblePanelWidgets()
//	{
//		/*******************************************
//		 * POPULATE THE SLIDESHOW PANE
//		 *******************************************/
//		JPanel slideshowPane = new JPanel(new GridBagLayout());
//		TitledBorder slideshowBorder = BorderFactory.createTitledBorder("Slideshow");
//		slideshowPane.setBorder(slideshowBorder);
//
//		GridBagConstraints c = new GridBagConstraints();
//
//		c.fill = GridBagConstraints.BOTH;
//		c.ipadx = 10;
//		c.insets = new Insets(5,5,5,5);
//		c.gridheight = 1;
//		c.weightx = c.weighty = 0.0;
//
//		c.gridwidth = 2;
//		c.gridx = 0;
//		c.gridy = 0;
//		slideshowPane.add(saveSlideshowBox, c);
//
//		c.gridwidth = 1;
//		c.gridx = 2;
//		c.gridy = 0;
//		slideshowPane.add(slideshowRateLbl, c);
//
//		c.gridx = 3;
//		c.gridy = 0;
//		slideshowPane.add(slideshowRateField, c);
//
//		c.gridx = 0;
//		c.gridy = 1;
//		slideshowPane.add(slideshowNameLbl, c);
//
//		c.gridx = 1;
//		c.gridy = 1;
//		slideshowPane.add(slideshowNameField, c);
//
//		c.gridx = 2;
//		c.gridy = 1;
//		slideshowPane.add(slideshowTypeLbl, c);
//
//		c.gridx = 3;
//		c.gridy = 1;
//		slideshowPane.add(slideshowTypeList, c);
//
//		c.gridx = 0;
//		c.gridy = 2;
//		c.gridwidth = 2;
//		slideshowPane.add(slideshowColorBox, c);
//		
//		c.gridx = 2;
//		c.gridwidth = 1;
//		slideshowPane.add(slideshowColorLbl, c);
//		
//		c.gridx = 3;
//		slideshowPane.add(slideshowColorField, c);
//		
//		c.gridx = 0;
//		c.gridy = 3;
//		c.gridwidth = 2;
//		slideshowPane.add(slideshowLayerLbl, c);
//		
//		c.gridx = 2;
//		slideshowPane.add(slideshowLayerList, c);
//		
//		/* SLIDESHOW PANE DONE!! */
//
//
//		/*************************************************
//		 * POPULATE THE GENERAL DISPLAY PANE
//		 *************************************************/
//		JPanel generalPane = new JPanel(new GridBagLayout());
//		Border generalBorder = BorderFactory.createTitledBorder("General Display");
//		generalPane.setBorder(generalBorder);
//
//		c = new GridBagConstraints();
//
//		c.fill = GridBagConstraints.BOTH;
//		c.ipadx = 10;
//		c.insets = new Insets(5,5,5,5);
//		c.gridwidth = 3;
//		c.gridheight = 1;
//		c.weightx = c.weighty = 0.0;
//		c.gridx = 0;
//		c.gridy = 0;
//		generalPane.add(showGraphicsBox, c);
//
//		c.gridx = 3;
//		c.gridy = 0;
//		generalPane.add(showDisplayTextBox, c);
//
//		c.gridx = 0;
//		c.gridy = 1;
//		generalPane.add(showTimeBox, c);
//
//		c.gridx = 3;
//		c.gridy = 1;
//		generalPane.add(showCountBox, c);
//
//		c.gridx = 0;
//		c.gridy = 2;
//		generalPane.add(pauseOnCycleBox, c);
//
//		c.gridwidth = 1;
//		c.gridx = 3;
//		generalPane.add(pixelScaleLbl, c);
//		
//		c.gridwidth = 1;
//		c.gridx = 4;
//		generalPane.add(pixelScaleField, c);
//		
//		c.gridwidth = 1;
//		c.gridx = 0;
//		c.gridy = 3;
//		generalPane.add(bgColorLbl, c);
//
//		c.gridx = 1;
//		generalPane.add(bgColorPreview, c);
//
//		c.gridx = 2;
//		generalPane.add(bgColorButton, c);
//
//		c.gridx = 3;
//		generalPane.add(barColorLbl, c);
//
//		c.gridx = 4;
//		generalPane.add(barColorPreview, c);
//
//		c.gridx = 5;
//		generalPane.add(barColorButton, c);
//		
//		/* GENERAL DISPLAY PANE DONE! */
//		
//		/*******************************************
//		 * POPULATE THE SIMULATION PARAMETERS PANE
//		 *******************************************/
//		JPanel simPane = new JPanel(new GridBagLayout());
//		Border simBorder = BorderFactory.createTitledBorder("Simulation Parameters");
//		simPane.setBorder(simBorder);
//		c = new GridBagConstraints();
//
//		c.fill = GridBagConstraints.BOTH;
//		c.ipadx = 10;
//		c.insets = new Insets(5,5,5,5);
//		c.gridwidth = 2;
//		c.gridheight = 1;
//		c.weightx = c.weighty = 0.0;
//		c.gridx = 0;
//		c.gridy = 0;
//		simPane.add(allowOverlapBox, c);
//		
//		c.gridx = 2;
//		simPane.add(torusWorldBox, c);
//		
//		c.gridx = 0;
//		c.gridy++;
//		c.gridwidth = 1;
//		simPane.add(new JLabel("Lower biomass threshold (g):", JLabel.LEFT), c);
//		
//		c.gridx++;
//		simPane.add(minBiomassField, c);
//		
//		c.gridx++;
//		simPane.add(new JLabel("Maximum biomass (g): ", JLabel.LEFT), c);
//		
//		c.gridx++;
//		simPane.add(maxBiomassField, c);
//		
//		c.gridx = 0;
//		c.gridy++;
//		c.gridwidth = 1;
//		simPane.add(timeStepLbl, c);
//		
//		c.gridx = 1;
//		simPane.add(timeStepField, c);
//		
//		c.gridx = 2;
//		simPane.add(deathRateLbl, c);
//		
//		c.gridx = 3;
//		simPane.add(deathRateField, c);
//		
//		c.gridx = 0;
//		c.gridy++;
//		simPane.add(spaceWidthLbl, c);
//		
//		c.gridx = 1;
//		simPane.add(spaceWidthField, c);
//
////		c.gridx = 2;
////		simPane.add(diffConstLbl, c);
//		
////		c.gridx = 3;
////		simPane.add(diffConstField, c);
//		
//		c.gridx = 0;
//		c.gridy++;
//		simPane.add(unlimitedCyclesBox, c);
//		
//		c.gridx = 1;
//		c.gridwidth = 2;
//		c.anchor = GridBagConstraints.EAST;
//		simPane.add(maxCyclesLbl, c);
//		
//		c.gridx = 3;
//		c.gridwidth = 1;
//		c.anchor = GridBagConstraints.WEST;
//		simPane.add(maxCyclesField, c);
//		
//		/* END SIMULATION PARAMS! */
//		
//		/****************************************
//		 * PUT ALL PANES TOGETHER IN A BOXLAYOUT
//		 ****************************************/
//		this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
//		this.add(generalPane);
//		this.add(slideshowPane);
//		this.add(simPane);
//
//	}
//	
//	/**
//	 * Resets the various fields to the values currently known by the CometsParameters.
//	 */
//	public void resetPanel()
//	{
//		pixelScaleField.setValue(cParams.getPixelScale());
//		slideshowRateField.setValue(cParams.getSlideshowRate());
//		maxCyclesField.setValue(cParams.getMaxCycles());
//		
//		slideshowNameField.setText(cParams.getSlideshowName());
//		
//		timeStepField.setValue(cParams.getTimeStep());
//		deathRateField.setValue(cParams.getDeathRate());
//		spaceWidthField.setValue(cParams.getSpaceWidth());
//		minBiomassField.setValue(cParams.getMinSpaceBiomass());
//		maxBiomassField.setValue(cParams.getMaxSpaceBiomass());
//		slideshowColorField.setValue(cParams.getSlideshowColorValue());
//
//		//TODO fix!
//		slideshowTypeList.setSelectedItem(cParams.getSlideshowExt());
//
//		//TODO fix!
//		int curLayer = cParams.getSlideshowLayer();
//		if (curLayer > slideshowLayerList.getItemCount() || curLayer < 0)
//			slideshowLayerList.setSelectedIndex(slideshowLayerList.getItemCount());
//		else if (slideshowLayerList.getItemCount() > 0) 
//			slideshowLayerList.setSelectedIndex(curLayer);
//		
//		showGraphicsBox.setSelected(cParams.showGraphics());
//		allowOverlapBox.setSelected(cParams.allowCellOverlap());
//		
//		showTimeBox.setSelected(cParams.showCycleTime());
//		showCountBox.setSelected(cParams.showCycleCount());
//		saveSlideshowBox.setSelected(cParams.saveSlideshow());
//		slideshowColorBox.setSelected(cParams.getSlideshowColorRelative());
//		showDisplayTextBox.setSelected(cParams.showText());
//		pauseOnCycleBox.setSelected(cParams.pauseOnStep());
//		unlimitedCyclesBox.setSelected(cParams.getMaxCycles() == UNLIMITED_CYCLES);
//
//
//		bgColorPreview.setBackground(new Color(cParams.getBackgroundColor()));
//		barColorPreview.setBackground(new Color(cParams.getBarrierColor()));
//		
//		slideshowRateField.setEnabled(saveSlideshowBox.isSelected());
//		slideshowRateLbl.setEnabled(saveSlideshowBox.isSelected());
//		slideshowNameField.setEnabled(saveSlideshowBox.isSelected());
//		slideshowTypeList.setEnabled(saveSlideshowBox.isSelected());
//		slideshowNameLbl.setEnabled(saveSlideshowBox.isSelected());
//		slideshowTypeLbl.setEnabled(saveSlideshowBox.isSelected());
//		slideshowColorBox.setEnabled(saveSlideshowBox.isSelected());
//		slideshowColorLbl.setEnabled(saveSlideshowBox.isSelected() && !slideshowColorBox.isSelected());
//		slideshowColorField.setEnabled(saveSlideshowBox.isSelected() && !slideshowColorBox.isSelected());
//		slideshowLayerList.setEnabled(saveSlideshowBox.isSelected());
//		slideshowLayerLbl.setEnabled(saveSlideshowBox.isSelected());
//	}
//	
//	/**
//	 * Applies the current values in the various fields to the CometsParameters.
//	 */
//	public void applyChanges()
//	{
//		cParams.allowCellOverlap(allowOverlapBox.isSelected());
//		cParams.showGraphics(showGraphicsBox.isSelected());
//		cParams.setToroidalGrid(torusWorldBox.isSelected());
//		cParams.showCycleTime(showTimeBox.isSelected());
//		cParams.showCycleCount(showCountBox.isSelected());
//		cParams.showText(showDisplayTextBox.isSelected());
//		cParams.pauseOnStep(pauseOnCycleBox.isSelected());
//
//		cParams.saveSlideshow(saveSlideshowBox.isSelected());
//		cParams.setSlideshowExt((String)slideshowTypeList.getSelectedItem());
//		cParams.setSlideshowRate(slideshowRateField.getIntValue());
//		cParams.setSlideshowName(slideshowNameField.getText());
//		cParams.setSlideshowColorRelative(slideshowColorBox.isSelected());
//		if (!slideshowColorBox.isSelected())
//			cParams.setSlideshowColorValue(slideshowColorField.getDoubleValue());
//		cParams.setSlideshowLayer(slideshowLayerList.getSelectedIndex());
//
//		cParams.setBackgroundColor(bgColorPreview.getBackground());
//		cParams.setBarrierColor(barColorPreview.getBackground());
//		cParams.setPixelScale(pixelScaleField.getIntValue());
//		
//		cParams.setTimeStep(timeStepField.getDoubleValue());
//		cParams.setDeathRate(deathRateField.getDoubleValue());
//		cParams.setSpaceWidth(spaceWidthField.getDoubleValue());
//		
//		// setting min and max biomass per space at the same time is tricky.
//		// first, set the max to be infinity.
//		// then set the min.
//		// then set the max.
//		cParams.setMaxSpaceBiomass(Double.MAX_VALUE);
//		cParams.setMinSpaceBiomass(minBiomassField.getDoubleValue());
//		cParams.setMaxSpaceBiomass(maxBiomassField.getDoubleValue());
//		
//		if (unlimitedCyclesBox.isSelected())
//			cParams.setMaxCycles(UNLIMITED_CYCLES);
//		else
//		{
//			int max = maxCyclesField.getIntValue();
//			if (max == 0)
//				max = 1;
//			cParams.setMaxCycles(max);
//		}
//	}
//	
//	public void updateMediaOptions(String[] mediaNames)
//	{
//		/*
//		 * If there's a World loaded, populate the slideshowLayerList with the names of all
//		 * the medium components and such, followed with biomass. Finally, set it to be
//		 * on the selected layer.
//		 */
//		slideshowLayerList.removeAllItems();
//		for (int i=0; i<mediaNames.length; i++)
//		{
//			slideshowLayerList.addItem(mediaNames[i]);
//		}
//		slideshowLayerList.addItem("Biomass");
//
//		int curLayer = cParams.getSlideshowLayer();
//		if (curLayer > mediaNames.length)
//		{
//			curLayer = 0;
//		}
//		slideshowLayerList.setSelectedIndex(curLayer);
//	}
}
