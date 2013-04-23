package edu.bu.segrelab.comets.exception;

import edu.bu.segrelab.comets.CometsConstants;

@SuppressWarnings("serial")
public class ParameterFileException extends Exception
									implements CometsConstants
{
	int line;
	private static final String DEFAULT_ERROR = "Parameter file error";

	public ParameterFileException(int line)
	{
		super(DEFAULT_ERROR);
		this.line = line;
	}
	
	public ParameterFileException(String error, int line)
	{
		super(error);
		this.line = line;
	}

	public int getLine()
	{
		return line;
	}
	
}
