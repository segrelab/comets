package edu.bu.segrelab.comets.exception;

@SuppressWarnings("serial")
public class CometsArgumentException extends Exception
{
	public CometsArgumentException()
	{
		super();
	}
	
	public CometsArgumentException(String error)
	{
		super(error);
	}

}
