package edu.bu.segrelab.comets.fba;

import edu.bu.segrelab.comets.Comets;
import edu.bu.segrelab.comets.PackageParameterBatch;

public class FBAParameterBatch implements PackageParameterBatch
{
	private Comets c;
	private FBAParameters ppBackup;
	public FBAParameterBatch(Comets c)
	{
		this.c = c;
		ppBackup = (FBAParameters)c.getPackageParameters();
	}
	@Override
	public void resetParameters()
	{
		c.setPackageParameters(ppBackup);
	}
}
