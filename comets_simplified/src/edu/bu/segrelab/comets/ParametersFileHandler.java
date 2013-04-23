package edu.bu.segrelab.comets;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.JFrame;

import edu.bu.segrelab.comets.exception.ParameterFileException;
import edu.bu.segrelab.comets.util.ParameterState;

public class ParametersFileHandler
{
	public static void loadParametersFile(String filename, CometsParameters cParams, PackageParameters pParams) throws IOException,
																													   ParameterFileException
	{
		cParams.saveParameterState();
		pParams.saveParameterState();
		BufferedReader reader = new BufferedReader(new FileReader(new File(filename)));
		String line = "";
		int lineCount = 0;
		try {
			while ((line = reader.readLine()) != null)
			{
				parseParameterLine(line, lineCount++, cParams, pParams);
			}
		} catch (ParameterFileException e) {
			reader.close();
			throw e;
		}
		reader.close();

		cParams.loadParameterState();
		pParams.loadParameterState();
	}
	
	private static void parseParameterLine(String line, int lineNum, CometsParameters cParams, PackageParameters pParams) throws ParameterFileException
	{
		line = line.trim();
		if (line.length() == 0)
			return;
		
		String[] splitLine = line.split("\\s*=\\s*");
		if (splitLine.length != 2)
			throw new ParameterFileException("Parameters file error", lineNum);
		
		setParameter(cParams, pParams, splitLine[0].trim().toLowerCase(), splitLine[1].trim(), lineNum);
	}
	
	public static void parseParameterList(List<String> paramList, CometsParameters cParams, PackageParameters pParams) throws ParameterFileException
	{
		cParams.saveParameterState();
		pParams.saveParameterState();
		int lineNum = 0;
		for (String line : paramList)
		{
			parseParameterLine(line, lineNum++, cParams, pParams);
		}
		cParams.loadParameterState();
		pParams.loadParameterState();
	}
	
	private static void setParameter(CometsParameters cParams, PackageParameters pParams, 
									 String name, String value, int line) throws ParameterFileException
	{
		ParameterState state = cParams.setParameter(name, value);
		switch(state)
		{
			case WRONG_TYPE :
				throw new ParameterFileException("Invalid parameter value for " + name, line);
			case NOT_FOUND :
				ParameterState nextTry = pParams.setParameter(name, value);
				switch(nextTry)
				{
					case WRONG_TYPE :
						throw new ParameterFileException("Invalid parameter value for " + name, line);
					case NOT_FOUND :
						throw new ParameterFileException("Unknown parameter " + name, line);
					default :
						break;
				}
				break;
			default :
				break;
		}
	}
	
	public static void saveParametersFile(String filename, CometsParameters cParams, PackageParameters pParams) throws IOException
	{
		cParams.saveParameterState();
		pParams.saveParameterState();
		PrintWriter writer = new PrintWriter(new FileWriter(new File(filename)));
		cParams.dumpToFile(writer);
		pParams.dumpToFile(writer);
		writer.close();
	}
	
	public static void loadParametersFile(JFrame owner, CometsParameters cParams, PackageParameters pParams) throws IOException,
																													ParameterFileException
	{
		JFileChooser chooser = new JFileChooser();
		int returnVal = chooser.showOpenDialog(owner);
		if (returnVal == JFileChooser.APPROVE_OPTION)
		{
			loadParametersFile(chooser.getSelectedFile().getPath(), cParams, pParams);
		}
	}
	
	public static void saveParametersFile(JFrame owner, CometsParameters cParams, PackageParameters pParams) throws IOException
	{
		JFileChooser chooser = new JFileChooser();
		int returnVal = chooser.showSaveDialog(owner);
		if (returnVal == JFileChooser.APPROVE_OPTION)
		{
			saveParametersFile(chooser.getSelectedFile().getPath(), cParams, pParams);
		}
	}
}
