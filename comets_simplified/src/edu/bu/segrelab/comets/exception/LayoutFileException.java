package edu.bu.segrelab.comets.exception;

@SuppressWarnings("serial")
public class LayoutFileException extends Exception
{
	int line;
	public LayoutFileException(String msg, int line) {
		super(msg);
		this.line = line;
	}
}
