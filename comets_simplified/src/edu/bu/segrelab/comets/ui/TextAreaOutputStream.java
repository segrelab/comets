package edu.bu.segrelab.comets.ui;

import java.io.IOException;
import java.io.OutputStream;

import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JScrollBar;

/**
 * A modified output stream that dumps things to a <code>JTextArea</code> with integrated
 * scrollbar. The benefit here is an autoscrolling mechanism - if the scrollbar is near the
 * bottom when there's a text update, it will automatically scroll down. 
 * @author Bill Riehl briehl@bu.edu
 */
public final class TextAreaOutputStream extends OutputStream 
{
	private final JTextArea textArea;
	private final StringBuilder sb = new StringBuilder();
	private boolean autoScroll = true;
	private final JScrollBar vBar;

	/**
	 * Initialize the <code>TextAreaOutputStream</code> to output text to the given
	 * <code>JTextArea</code> and follow the position of the scrollbar in the given 
	 * <code>JScrollPane</code>.
	 * @param textArea - the textarea to update
	 * @param scrollPane - the scrollpane to monitor for position, and autoscroll
	 */
	public TextAreaOutputStream(final JTextArea textArea, final JScrollPane scrollPane)
	{
		vBar = scrollPane.getVerticalScrollBar();
		this.textArea = textArea;
	}
	
	public void flush() { }
	
	public void close() { }

	/**
	 * Overrides the write command to automatically scroll.
	 * @see OutputStream#write(byte[], int, int)
	 * @param cbuf
	 * @param off
	 * @param len
	 * @throws IOException
	 */
	public void write(char[] cbuf, int off, int len) throws IOException
	{
		textArea.append(new String(cbuf, off, len));
		updateAutoScroll();
	}

	/**
	 * Overrides the write(int) method to automatically scroll.
	 * @see OutputStream#write(int).
	 * @param b - the ASCII character to write out
	 */
	public void write(int b) throws IOException
	{
		if (b == '\r')
			return;
		if (b == '\n')
		{
			textArea.append(sb.toString());
			sb.setLength(0);
			updateAutoScroll();
		}
		
		sb.append((char)b);
	}
	
	/**
	 * Updates the automatic scroller. If the scrollbar is at least 95% of the way toward the
	 * bottom, it will jump all the way to the bottom.
	 */
	private void updateAutoScroll()
	{
		autoScroll = (vBar.getValue() + vBar.getVisibleAmount()) >= 0.95*vBar.getMaximum();
		if (autoScroll)
			textArea.setCaretPosition(textArea.getDocument().getLength());
	}
}
