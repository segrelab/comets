package edu.bu.segrelab.comets.fba;

//TODO everything: complete get/set methods, make constructors
/**
 * FBACell
 * --------------------------------
 * This acts as the Cell class in the FBA package. Every FBACell doesn't act as a single
 * "cell", but as a quantity of biomass. However, as in the COMETS design, only one FBACell
 * can exist in one space in the FBAWorld.
 * <p>
 * Each FBACell keeps track of the total biomass it contains from each FBAModel species in
 * the simulation, the total set of fluxes from the most recent run, and the most recent
 * change in biomass (as given by FBA).
 * @author Bill Riehl briehl@bu.edu
 */
public class FBASubstrate extends edu.bu.segrelab.comets.Substrate 
					 implements edu.bu.segrelab.comets.CometsConstants
{
	private double[] biomassDiff;
	private double[] mediaDiff;
	
	public FBASubstrate(double[] biomassDiff, double[] mediaDiff){
		this.biomassDiff = biomassDiff;
		this.mediaDiff = mediaDiff;
	}
	
	public void setMediaDiff(double[] mediaDiff){
		this.mediaDiff = mediaDiff;
	}
	
	public void setBiomassDiff(double[] biomassDiff){
		this.biomassDiff = biomassDiff;
	}
	
	public double getMediaDiff(int i){
		return mediaDiff[i];
	}
	
	public double getBiomassDiff(int i){
		return biomassDiff[i];
	}
}
