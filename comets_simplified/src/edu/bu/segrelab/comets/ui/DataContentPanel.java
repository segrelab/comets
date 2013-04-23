package edu.bu.segrelab.comets.ui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;


/**
 * An extended <code>JPanel</code> that contains an editable list of names and values.
 * In the context of the COMETS program, this is used as a place to create and store
 * a set of either biomass or nutrient values that can be used to update the world.
 * For example, in setting up the initial world layout.
 * <p>
 * The panel is layed out as a vertical list of name/value pairs, with the first
 * name and value at the top of the list.
 * @author Bill Riehl briehl@bu.edu
 */
@SuppressWarnings("serial")
public class DataContentPanel extends JPanel
{
	private DoubleField[][] dataFields;
	private JCheckBox[] staticBoxes;
	private boolean[] isStatic;
	private boolean useStaticBoxes;
	private JLabel[] rowLabels,
					 colLabels;
	private double[][] values;
	private int numCols;
	
	public DataContentPanel(String[] names, String[] headers, boolean useStaticBoxes, boolean[] isStatic)
	{
		super(new GridBagLayout());
		this.numCols = headers.length;
		values = new double[names.length][numCols];
		rowLabels = new JLabel[names.length];
		dataFields = new DoubleField[names.length][numCols];

		for (int i=0; i<names.length; i++)
		{
			rowLabels[i] = new JLabel(names[i], JLabel.LEFT);
			for (int j=0; j<numCols; j++)
				dataFields[i][j] = new DoubleField(0.0, 6, true);
		}

		this.useStaticBoxes = useStaticBoxes;
		if (useStaticBoxes)
		{
			staticBoxes = new JCheckBox[names.length];
			this.isStatic = new boolean[isStatic.length];
			for (int i=0; i<isStatic.length; i++)
			{
				this.isStatic[i] = isStatic[i];
				staticBoxes[i] = new JCheckBox();
				staticBoxes[i].setSelected(isStatic[i]);
			}
			String[] newHeaders = new String[headers.length+1];
			for (int i=0; i<headers.length; i++)
			{
				newHeaders[i] = headers[i];
			}
			newHeaders[newHeaders.length-1] = "static";
			headers = newHeaders;
		}
		else
		{
			this.isStatic = null;
			staticBoxes = null;
		}
		
		colLabels = new JLabel[headers.length];
		for (int i=0; i<headers.length; i++)
		{
			colLabels[i] = new JLabel(headers[i], JLabel.CENTER);
		}

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.BOTH;
		gbc.ipadx = 10;
		gbc.insets = new Insets(5, 5, 5, 5);
		gbc.gridheight = 1;
		gbc.weightx = gbc.weighty = 1.0;
		gbc.gridx = 0;
		gbc.gridy = 0;

		gbc.gridwidth = 1;
		
		for (int i=0; i<colLabels.length; i++)
		{
			gbc.gridx++;
			add(colLabels[i], gbc);
		}
		
		gbc.gridy = 1;
		for (int j=0; j<rowLabels.length; j++)
		{
			gbc.gridx = 0;
			gbc.gridy = j+1;
			gbc.ipadx = 0;
			gbc.gridwidth = 1;
			add(rowLabels[j], gbc);
			for (int k=0; k<numCols; k++)
			{
				gbc.gridx = k+1;
				add(dataFields[j][k], gbc);
			}
			if (useStaticBoxes)
			{
				gbc.gridwidth = 2;
				gbc.gridx = numCols+1;
				add(staticBoxes[j], gbc);
			}
		}
	}

	public DataContentPanel(String[] names, String[] headers, double[][] values, boolean useStaticBoxes, boolean[] isStatic)
	{
		this(names, headers, useStaticBoxes, isStatic);
		setValues(values);
	}
	
	/**
	 * The panel is initialized with a <code>String</code> array of names, and
	 * a <code>double</code> array of starting values. Only up to six 
	 * significant digits are shown in the data fields.
	 * @param names
	 * @param values
	 */
	public DataContentPanel(String[] names, String[] headers, double[] values, boolean useStaticBoxes, boolean[] isStatic)
	{
		this(names, headers, useStaticBoxes, isStatic);
		double[][] dValues = new double[values.length][1];
		for (int i=0; i<values.length; i++)
		{
			dValues[i][0] = values[i];
		}
		setValues(dValues);
	}
	
	/**
	 * This fetches and stores the values from the data fields. This should be called
	 * whenever the user has made changes that should be saved.
	 */
	public void updateValues()
	{
		for (int i=0; i<dataFields.length; i++)
		{
			for (int j=0; j<dataFields[0].length; j++)
			{
				values[i][j] = dataFields[i][j].getDoubleValue();
			}
			if (useStaticBoxes)
				isStatic[i] = staticBoxes[i].isSelected();
		}
	}
	
	/**
	 * This resets the displayed values (those visible in the <code>DoubleFields</code>)
	 * to be the values stored internally.
	 * 
	 * This should be called if any changes are canceled by the user. For example, 
	 * if the user changes some values, then changes their mind and doesn't want them
	 * applied, then calling resetValues() will reset the displayed values.
	 */
	public void resetValues()
	{
		for (int i=0; i<dataFields.length; i++)
		{
			for (int j=0; j<dataFields[0].length; j++)
			{
				dataFields[i][j].setValue(values[i][j]);				
			}
			if (useStaticBoxes)
				staticBoxes[i].setSelected(isStatic[i]);
		}
	}

	/**
	 * @return a <code>double</code> array of the current set of data values
	 */
	public double[] getValues()
	{
		return getValues(0);
	}

	public double[] getValues(int col)
	{
		if (col < 0 || col > numCols-1)
		{
			return null;
		}
		double[] v = new double[values.length];
		for (int i=0; i<v.length; i++)
			v[i] = values[i][col];
		return v;
	}
	
	public double[][] getAllValues()
	{
		return values;
	}
	
	public void setValues(double[] values, int col)
	{
		if (col < 0 || col > numCols-1)
			return;
		for (int i=0; i<values.length; i++)
		{
			this.values[i][col] = values[i];
			dataFields[i][col].setText(String.valueOf(values[i]));
		}		
	}

	public void setStatic(boolean[] isStatic)
	{
		if (isStatic == null)
			isStatic = new boolean[values.length];
		else if (isStatic.length != values.length || !useStaticBoxes)
			return;
		for (int i=0; i<isStatic.length; i++)
		{
			this.isStatic[i] = isStatic[i];
			staticBoxes[i].setSelected(isStatic[i]);
		}
	}
	
	public boolean[] getStatic()
	{
		return isStatic;
	}
	
	/**
	 * Returns true if at least one checkbox in the static column is checked
	 * @return
	 */
	public boolean hasStaticChecked()
	{
		for (int i=0; i<staticBoxes.length; i++)
		{
			if (staticBoxes[i].isSelected())
				return true;
		}
		return false;
	}
	
	/**
	 * Sets both the displayed data values, and the internally stored values.
	 * @param values
	 */
	public void setValues(double[] values)
	{
		setValues(values, 0);
	}

	public void setValues(double[][] values)
	{
		for (int i=0; i<values.length; i++)
		{
			for (int j=0; j<values[0].length; j++)
			{
				this.values[i][j] = values[i][j];
				dataFields[i][j].setText(String.valueOf(values[i][j]));
			}
		}
	}
	
	/**
	 * Overrides <code>JComponent.setEnabled()</code> to apply the command to 
	 * all internal components (the labels and data fields).
	 */
	public void setEnabled(boolean b)
	{
		super.setEnabled(b);
		for (int i=0; i<rowLabels.length; i++)
		{
			rowLabels[i].setEnabled(b);
			for (int j=0; j<dataFields[0].length; j++)
				dataFields[i][j].setEnabled(b);
		}
		if (useStaticBoxes)
		{
			for (int i=0; i<staticBoxes.length; i++)
				staticBoxes[i].setEnabled(b);
		}
		for (int i=0; i<colLabels.length; i++)
		{
			colLabels[i].setEnabled(b);
		}
	}
}
