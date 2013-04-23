package edu.bu.segrelab.comets.fba.ui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import edu.bu.segrelab.comets.Comets;
import edu.bu.segrelab.comets.ui.DoubleField;
import edu.bu.segrelab.comets.ui.IntField;

@SuppressWarnings("serial")
public class LayoutSavePanel extends JPanel
{

	public enum BiomassStyle
	{
		NORMAL("As on screen", ""),
		RANDOM("Random", "random"),
		RANDOM_BOX("Random region", "random_rect"),
		FILLED("Filled", "filled"),
		FILLED_BOX("Filled region", "filled_rect"),
		SQUARE("Central square", "square");
		
		private final String displayName,
							 layoutName;
		
		private BiomassStyle(String displayName, String layoutName)
		{
			this.displayName = displayName;
			this.layoutName = layoutName;
		}
		
		public String getDisplayName()
		{
			return displayName;
		}
		
		public String getLayoutName()
		{
			return layoutName;
		}
		
		public BiomassStyle getStyleFromName(String name)
		{
			for (BiomassStyle s : BiomassStyle.values())
			{
				if (s.getDisplayName().equals(name) || s.getLayoutName().equals(name))
					return s;
			}
			return null;
		}
		
		public String toString()
		{
			return displayName;
		}
	}
	
	
	private JCheckBox biomassStyleBox;
	private JComboBox biomassStyleComboBox;
	private Comets c;
	private List<DoubleField> quantityFieldList;
	private List<IntField> spacesFieldList;
	private List<JLabel> speciesLabelList;
	private IntField xField,
					 yField,
					 wField,
					 hField;
	private JLabel xLabel,
				   yLabel,
				   wLabel,
				   hLabel,
				   numSpacesLabel,
				   quantityLabel,
				   biomassStyleLabel;
	private static final String X = "X:",
						 		Y = "Y:",
						 		W = "Width:",
						 		H = "Height:",
						 		LAYOUT_STYLE = "Biomass layout style",
						 		NUM_SPACES = "# spaces",
						 		QUANTITY = "quantity (g)";
	private BiomassStyle style;
	private JPanel rectDataPanel,
				   quantityPanel,
				   spacesPanel,
				   labelsPanel;
	
	public LayoutSavePanel(Comets c)
	{
		this.c = c;
		biomassStyleBox = new JCheckBox(LAYOUT_STYLE);
		biomassStyleComboBox = new JComboBox(BiomassStyle.values());
		quantityFieldList = new ArrayList<DoubleField>();
		spacesFieldList = new ArrayList<IntField>();
		speciesLabelList = new ArrayList<JLabel>();
		xField = new IntField(0, 3, false);
		yField = new IntField(0, 3, false);
		wField = new IntField(0, 3, false);
		hField = new IntField(0, 3, false);
		xLabel = new JLabel(X);
		yLabel = new JLabel(Y);
		wLabel = new JLabel(W);
		hLabel = new JLabel(H);
		biomassStyleLabel = new JLabel(LAYOUT_STYLE);
		numSpacesLabel = new JLabel(NUM_SPACES);
		quantityLabel = new JLabel(QUANTITY);

		rectDataPanel = new JPanel(new GridBagLayout());
		quantityPanel = new JPanel(new GridBagLayout());
		spacesPanel = new JPanel(new GridBagLayout());
		labelsPanel = new JPanel(new GridBagLayout());
		
		this.style = BiomassStyle.NORMAL;

		initSubPanels();
		doPanelLayout();
		bindWidgets();
		init();
	}
	
	private void doPanelLayout()
	{
		this.setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 4;
		
		this.add(biomassStyleLabel, gbc);
		gbc.gridy = 1;
		this.add(biomassStyleComboBox, gbc);
		gbc.gridx = 0;
		gbc.gridy = 2;
		gbc.gridwidth = 4;
		this.add(rectDataPanel, gbc);
		gbc.gridx = 1;
		gbc.gridy = 3;
		gbc.gridwidth = 1;
		this.add(labelsPanel, gbc);
		gbc.gridx = 2;
		this.add(spacesPanel, gbc);
		gbc.gridx = 3;
		this.add(quantityPanel, gbc);
	}
	
	private void initSubPanels()
	{
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridwidth = 1;
		gbc.gridx = 0;
		gbc.gridy = 0;
		
		rectDataPanel.add(xLabel, gbc);
		gbc.gridx++;
		rectDataPanel.add(xField, gbc);
		gbc.gridx+=2;
		rectDataPanel.add(yLabel, gbc);
		gbc.gridx++;
		rectDataPanel.add(yField, gbc);

		gbc.gridx = 0;
		gbc.gridy++;
		
		rectDataPanel.add(wLabel, gbc);
		gbc.gridx++;
		rectDataPanel.add(wField, gbc);
		gbc.gridx+=2;
		rectDataPanel.add(hLabel, gbc);
		gbc.gridx++;
		rectDataPanel.add(hField, gbc);
		
		gbc.gridx = 0;
		gbc.gridy = 0;
		
		quantityPanel.add(quantityLabel, gbc);
		spacesPanel.add(numSpacesLabel, gbc);
		
	}
	
	private void bindWidgets()
	{
//		biomassStyleBox.setSelected(true);
//		biomassStyleBox.addItemListener(new ItemListener() {
//			public void itemStateChanged(ItemEvent e) {
//				boolean selected = e.getStateChange() == ItemEvent.SELECTED;
//				biomassStyleComboBox.setEnabled(!selected);
//				quantityPanel.setEnabled(!selected);
//				spacesPanel.setEnabled(!selected);
//				rectDataPanel.setEnabled(!selected);
//				labelsPanel.setEnabled(!selected);
//				activateStylePanel(style);
//				
//				if (selected)
//					biomassStyleComboBox.setSelectedItem(BiomassStyle.NORMAL);
//			}
//		});
		
		biomassStyleComboBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				style = (BiomassStyle)biomassStyleComboBox.getSelectedItem();
				activateStylePanel(style);
			}
		});
	}
	
	private void activateStylePanel(BiomassStyle style)
	{
		// hide all
		// activate the passed style (if not null)
		
		rectDataPanel.setVisible(false);
		quantityPanel.setVisible(false);
		spacesPanel.setVisible(false);
		labelsPanel.setVisible(false);
		
		if (style != BiomassStyle.NORMAL)
		{
			quantityPanel.setVisible(true);
			labelsPanel.setVisible(true);
		}

		switch(style) {
			case RANDOM:
				spacesPanel.setVisible(true);
				break;
			case RANDOM_BOX:
				rectDataPanel.setVisible(true);
				spacesPanel.setVisible(true);
				break;
			case FILLED_BOX:
				rectDataPanel.setVisible(true);
				break;
			case SQUARE:
				spacesPanel.setVisible(true);
				break;
			default:
				break;
		}
	}
	
	/**
	 * Syncs this loader with the current state of Comets.
	 * E.g., adds the right number of quantity and box number fields to the respective lists (to account for different
	 * numbers of species)
	 */
	public void init()
	{
		// remove all thingies from panels.
		for (DoubleField x : quantityFieldList)
			quantityPanel.remove(x);
		
		for (IntField x : spacesFieldList)
			spacesPanel.remove(x);
			
		for (JLabel x : speciesLabelList)
			labelsPanel.remove(x);
		
		quantityFieldList.clear();
		spacesFieldList.clear();
		speciesLabelList.clear();
		int numSpecies = c.getModels().length;
		for (int i=0; i<numSpecies; i++)
		{
			quantityFieldList.add(new DoubleField(0.0, 6, false));
			spacesFieldList.add(new IntField(0, 6, false));
			speciesLabelList.add(new JLabel("sp #" + i));
		}
		populatePanels();
		
		quantityPanel.setVisible(false);
		spacesPanel.setVisible(false);
		labelsPanel.setVisible(false);
		rectDataPanel.setVisible(false);
		biomassStyleBox.setSelected(true);
		biomassStyleComboBox.setEnabled(true);
		biomassStyleComboBox.setSelectedItem(BiomassStyle.NORMAL);
	}
	
	private void populatePanels()
	{
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.gridwidth = 1;

		for (int i=0; i<speciesLabelList.size(); i++)
		{
			labelsPanel.add(speciesLabelList.get(i), gbc);
			spacesPanel.add(spacesFieldList.get(i), gbc);
			quantityPanel.add(quantityFieldList.get(i), gbc);
			gbc.gridy++;
		}
	}
	
	public BiomassStyle getBiomassStyle()
	{
		return style;
	}
	
	public int[] getNumSpaces()
	{
		int[] numSpaces = new int[spacesFieldList.size()];
		if (numSpaces.length == 0)
			return numSpaces;
		for (int i=0; i<spacesFieldList.size(); i++)
			numSpaces[i] = spacesFieldList.get(i).getIntValue();
		return numSpaces;
	}
	
	public double[] getBiomassQuantity()
	{
		double[] quantities = new double[quantityFieldList.size()];
		if (quantities.length == 0)
			return quantities;
		for (int i=0; i<quantityFieldList.size(); i++)
			quantities[i] = quantityFieldList.get(i).getDoubleValue();
		return quantities;
		
	}
	
	public Rectangle getBiomassRegion()
	{
		return new Rectangle(xField.getIntValue(), yField.getIntValue(), wField.getIntValue(), hField.getIntValue());
	}
}
