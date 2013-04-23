package edu.bu.segrelab.comets.ui;

import javax.swing.JTextField;
import java.awt.event.KeyEvent;

/**
 * A <code>JTextField</code> extension that only allows integer values to be 
 * entered into the box. There is also a separate toggle to allow only positive
 * or also negative values. There is also an <code>DoubleField</code> available.
 * @see edu.bu.segrelab.comets.ui.DoubleField
 * @author Bill Riehl briehl@bu.edu
 */
@SuppressWarnings("serial")
public class IntField extends JTextField
{
	boolean allowNegative;
	
	/**
	 * Constructs a new <code>IntField</code>.
	 * @param allowNegative - if true, allow negative numbers to be entered.
	 */
	public IntField(boolean allowNegative)
	{
		super();
		this.allowNegative = allowNegative;
	}
	
	/**
	 * Constructs a new <code>IntField</code>, initialized with some value.
	 * Note that if the initial value is negative and allowNegative is false,
	 * no error will be thrown. So check your variables before initializing!
	 * @param initialValue - initial value this field will hold.
	 * @param allowNegative - if true, allow negative numbers to be entered.
	 */
	public IntField(int initialValue, boolean allowNegative)
	{
		super(String.valueOf(initialValue));
		this.allowNegative = allowNegative;
	}
	
	/**
	 * Constructs a new <code>IntField</code> with some initial value and
	 * a given number of columns.
	 * @param initialValue - initial value this field will hold.
	 * @param cols - number of columns in the field.
	 * @param allowNegative - if true, allow negative numbers to be entered.
	 */
	public IntField(int initialValue, int cols, boolean allowNegative)
	{
		super(String.valueOf(initialValue), cols);
		this.allowNegative = allowNegative;
	}

	/**
	 * Overrides <code>JTextField</code>'s <code>processKeyEvent()</code> method
	 * to only allow integers to be entered in the field. Also, if the allowNegative 
	 * parameter was set to false in the constructor, then the '-' signifying a 
	 * negative number is not allowed.
	 */
	public void processKeyEvent(KeyEvent e)
	{
		char c = e.getKeyChar();

		if (e.getID() == KeyEvent.KEY_TYPED)
		{
			if (c == '-' && (getText().indexOf('-') != -1 || !allowNegative || getCaretPosition() > 0))
				e.consume();
			else if (!Character.isDigit(c) && c != '-')
				e.consume();
			else
				super.processKeyEvent(e);
		}
		else
			super.processKeyEvent(e);
	}
	
	/**
	 * Returns the integer this field is currently holding
	 * @return - an <code>int</code> from this field
	 */
	public int getIntValue()
	{
		if (getText().length() == 0)
			return 0;
		return Integer.valueOf(getText());
	}
	
	/**
	 * Sets the text to be the value given in i.
	 * @param i
	 */
	public void setValue(int i)
	{
		setText(String.valueOf(i));
	}
}
