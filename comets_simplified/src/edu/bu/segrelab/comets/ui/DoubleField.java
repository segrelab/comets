package edu.bu.segrelab.comets.ui;

import javax.swing.JTextField;

/**
 * A <code>JTextField</code> extension that only allows double values to be 
 * entered into the box. There is also a separate toggle to allow only positive
 * or also negative values. There is also an <code>IntField</code> available.
 * @see edu.bu.segrelab.comets.ui.IntField
 * @author Bill Riehl briehl@bu.edu
 */
@SuppressWarnings("serial")
public class DoubleField extends JTextField
{
	@SuppressWarnings("unused")
	private boolean allowNegative;
	private double currentValue;
	
	/**
	 * Constructs a new <code>DoubleField</code>.
	 * @param allowNegative - if true, allow negative numbers to be entered.
	 */
	public DoubleField(boolean allowNegative)
	{
		super();
		this.allowNegative = allowNegative;
		currentValue = 0;
	}
	
	/**
	 * Constructs a new <code>DoubleField</code> with a given number of columns.
	 * @param cols - number of columns in the field.
	 * @param allowNegative - if true, allow negative numbers to be entered.
	 */
	public DoubleField(int cols, boolean allowNegative)
	{
		super(cols);
		this.allowNegative = allowNegative;
		currentValue = 0;
	}
	
	/**
	 * Constructs a new <code>DoubleField</code>, initialized with some value.
	 * Note that if the initial value is negative and allowNegative is false,
	 * no error will be thrown. So check your variables before initializing!
	 * @param initialValue - initial value this field will hold.
	 * @param allowNegative - if true, allow negative numbers to be entered.
	 */
	public DoubleField(double initialValue, boolean allowNegative)
	{
		super(String.valueOf(initialValue));
		this.allowNegative = allowNegative;	
		currentValue = initialValue;
	}
	
	/**
	 * Constructs a new <code>DoubleField</code> with some initial value and
	 * a given number of columns.
	 * @param initialValue - initial value this field will hold.
	 * @param cols - number of columns in the field.
	 * @param allowNegative - if true, allow negative numbers to be entered.
	 */
	public DoubleField(double initialValue, int cols, boolean allowNegative)
	{
		super(String.valueOf(initialValue), cols);
		this.allowNegative = allowNegative;
		currentValue = initialValue;
	}
	
	/**
	 * Returns the value currently in the field as a <code>double</code>. 
	 * @return a <code>double</code> from the <code>DoubleField</code>
	 */
	public double getDoubleValue()
	{
		double value = currentValue;
		if (getText().length() == 0)
		{
			value = 0;
			setText("0");
		}
		try 
		{
			value = Double.valueOf(getText());
			currentValue = value;
		}
		catch (NumberFormatException e) 
		{
			value = currentValue;
			setText(String.valueOf(currentValue));
		}
		return value;
	}
	
	/**
	 * Sets the text to be the value given in d.
	 * @param d
	 */
	public void setValue(double d)
	{
		setText(String.valueOf(d));
	}
}
