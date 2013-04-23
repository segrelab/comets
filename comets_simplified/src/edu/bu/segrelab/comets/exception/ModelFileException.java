package edu.bu.segrelab.comets.exception;

/**
 * A <code>ModelFileException</code> is an exception used for issues with a
 * loaded model file (or not loaded, as the exception may be...).
 * 
 * This is a way to lump together problems from multiple types of exceptions - 
 * <code>FileNotFound</code>, <code>NumberFormat</code>, <code>IO</code>, etc.
 * 
 * Thus, along with the usual constructors, there's also separate constructors
 * that take an additional integer, to be chosen from the constants below.
 * This integer signifies the actual problem. 
 * @author Bill Riehl briehl@bu.edu
 *
 */
@SuppressWarnings("serial")
public class ModelFileException extends Exception
{
	/**
	 * 
	 */
	public static int FILE_NOT_FOUND = 0;
	public static int NUMBER_FORMAT_ERROR = 1;
	public static int IO_ERROR = 2;
	
	private int error;
	
	/**
	 * Makes a generic new <code>ModelFileException</code>
	 */
	public ModelFileException()
	{
		super();
	}
	
	/**
	 * Makes a new <code>ModelFileException</code> with a specific <code>String</code> message.
	 * @param msg - an informational message about the nature of the exception.
	 */
	public ModelFileException(String msg)
	{
		super(msg);
	}
	
	/**
	 * Makes a new <code>ModelFileException</code> with a specific error code.
	 * <p>
	 * The <code>error</code> parameter should be one of the following:<br>
	 * <code>ModelFileException.FILE_NOT_FOUND</code><br>
	 * <code>ModelFileException.NUMBER_FORMAT_ERROR</code><br> 
	 * <code>ModelFileException.IO_ERROR</code><br>  
	 * @param error - the error code for this <code>ModelFileException</code>.
	 */
	public ModelFileException(int error)
	{
		super();
		this.error = error;
	}
	
	/**
	 * Makes a new <code>ModelFileException</code> with a specific error code and
	 * error message. The error code should be chosen as in {@link #ModelFileException(int)}.
	 * @param error - the error code for this <code>ModelFileException</code>.
	 * @param msg - an informational message about the nature of the exception.
	 */
	public ModelFileException(int error, String msg)
	{
		super(msg);
		this.error = error; 
	}

	/**
	 * @return the error code for this exception: one of FILE_NOT_FOUND, NUMBER_FORMAT_ERROR,
	 * or IO_ERROR.
	 */
	public int getError() { return error; }
}
