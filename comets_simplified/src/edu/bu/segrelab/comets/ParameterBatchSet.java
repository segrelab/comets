package edu.bu.segrelab.comets;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import edu.bu.segrelab.comets.exception.CometsArgumentException;

public class ParameterBatchSet
{
	private Comets c;
	private CometsParameters cpBackup;
	private PackageParameters ppBackup;
	private int numSets = 0;
	private int setPtr = 0;
	List<ParameterBatch> parameterList;

	public ParameterBatchSet(Comets c, Set<String[]> parameterSet) throws CometsArgumentException
	{
		this.c = c;
		cpBackup = c.getParameters();
		ppBackup = c.getPackageParameters();
		
		cpBackup.saveParameterState();
		ppBackup.saveParameterState();
		parameterList = new ArrayList<ParameterBatch>();
		
		parseParameterSet(parameterSet);
	}
	
	private void parseParameterSet(Set<String[]> parameterSet) throws CometsArgumentException
	{
		for (String[] param : parameterSet)
		{
			/**
			 * 1. make sure the param is real
			 *    a. look in CometsParameters
			 *    b. if not, then ask ppBatch
			 * 2. if so, parse out the set of values
			 *    (a. store them? or just procedurally generate?)
			 * 3. calculate how many sets there will be
			 *    (if stored, no biggie)
			 *    (if not, then calc as you go. i guess just multiply each range?)
			 */
			
			if (param.length == 0)
				throw new CometsArgumentException("Empty parameter set found while preparing batch");
			
			String name = param[0];
			ParameterType type;
			if (cpBackup.hasParameter(name))
			{
				type = cpBackup.getType(name);
			}
			else if (ppBackup.hasParameter(name))
			{
				type = ppBackup.getType(name);
			}
			else
			{
				throw new CometsArgumentException("Unknown batch parameter: " + name);
			}
			
			switch(type)
			{
				case BOOLEAN:
					parameterList.add(new ParameterBatch(name, 0, 1, 1));
					break;
				case INT:
					if (param.length < 4)
						throw new CometsArgumentException("Any numerical batch parameter must have 3 values: start, increment, and end");
					parameterList.add(new ParameterBatch(name, Integer.parseInt(param[1]), Integer.parseInt(param[2]), Integer.parseInt(param[3])));
					break;
				case DOUBLE:
					if (param.length < 4)
						throw new CometsArgumentException("Any numerical batch parameter must have 3 values: start, increment, and end");
					parameterList.add(new ParameterBatch(name, Double.parseDouble(param[1]), Double.parseDouble(param[2]), Double.parseDouble(param[3])));
					break;
				default:
					throw new CometsArgumentException("Only boolean, integer, and floating point parameters may be batched");
			}
		}
		
		numSets = 0;
		for (ParameterBatch batch : parameterList) {
			numSets += batch.getNumSteps();
		}
	}
	
	public void applyParameterSet(int n)
	{
		if (n >= numSets) 
			throw new IllegalArgumentException("Set " + n + " beyond range of " + numSets);
		
		for (ParameterBatch batch : parameterList)
		{
			batch.resetBatch();
		}
		
		setPtr = 0;
		for (int i=0; i<n; i++)
		{
			parameterList.get(setPtr).incrementStep();
			while (!parameterList.get(setPtr).hasMoreSteps() && setPtr < parameterList.size())
			{
				parameterList.get(setPtr).resetBatch();
				setPtr++;
				parameterList.get(setPtr).incrementStep();
			}
			setPtr = 0;
		}
		
		applyCurrentParameterSet();
	}
	
	public void applyCurrentParameterSet()
	{
		cpBackup.loadParameterState();
		ppBackup.loadParameterState();
		
		for(ParameterBatch batch : parameterList)
		{
			String name = batch.getName();
			if (cpBackup.hasParameter(name))
			{
				if (cpBackup.getType(name) == ParameterType.BOOLEAN)
					cpBackup.setParameter(name, batch.getCurrentStep() == 0 ? "false" : "true");
				cpBackup.setParameter(name, String.valueOf(batch.getCurrentStep()));
			}
			else
			{
				if (ppBackup.getType(name) == ParameterType.BOOLEAN)
					ppBackup.setParameter(name, batch.getCurrentStep() == 0 ? "false" : "true");
				ppBackup.setParameter(name, String.valueOf(batch.getCurrentStep()));
			}
		}
		
		c.setParameters(cpBackup);
		c.setPackageParameters(ppBackup);
	}
	
	public void applyNextParameterSet()
	{
		/**
		 * We've got an ordered parameterList of ParameterBatch
		 * Pointer to the one being changed
		 * 
		 * sets a, b, c.
		 * pointer ptr -> a
		 * 
		 * prepare values(a, b, c)
		 * 
		 * inc(value[ptr])
		 * while (value[ptr] == end)
		 *   loop(value[ptr])
		 *   ptr++
		 *
		 * translate this thingy and win.
		 * 
		 * for (my $i=0; $i<60; $i++) {
		 * 		print "$p->[0]->[$ptrs[0]] $p->[1]->[$ptrs[1]] $p->[2]->[$ptrs[2]]\n";
				$ptrs[$j]++;
				while( $j < 3 && $ptrs[$j] >= scalar(@{ $p->[$j] })) {
					$ptrs[$j] = 0;
					$j++;
					$ptrs[$j]++ if ($j<3);
				}
				$j=0;
			}
		 */
	}

	public void resetParameters()
	{
		c.setParameters(cpBackup);
		c.setPackageParameters(ppBackup);
	}
	
	public void saveList(String filename) throws IOException
	{
		PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(new File(filename))));
		for (int i=0; i<numSets; i++)
		{
			pw.println("Set " + (i+1));
			applyParameterSet(i);
			for (int j=0; j<parameterList.size(); j++)
			{
				ParameterBatch batch = parameterList.get(i);
				pw.println("\t" + batch.getName() + " = " + batch.getCurrentStep());
			}
		}
		pw.close();
	}
	
	public int size()
	{
		return numSets;
	}
}