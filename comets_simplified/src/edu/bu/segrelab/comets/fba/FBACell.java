package edu.bu.segrelab.comets.fba;

import edu.bu.segrelab.comets.Cell;
import edu.bu.segrelab.comets.Comets;
import edu.bu.segrelab.comets.CometsParameters;
import edu.bu.segrelab.comets.Model;
import edu.bu.segrelab.comets.World;
import edu.bu.segrelab.comets.World2D;
import edu.bu.segrelab.comets.World3D;
import edu.bu.segrelab.comets.reaction.ReactionModel;
import edu.bu.segrelab.comets.util.Utility;

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
public class FBACell extends edu.bu.segrelab.comets.Cell 
					 implements edu.bu.segrelab.comets.CometsConstants
{
	private FBAModel[] fbaModels;
	private FBAWorld world;
	private FBAWorld3D world3D;
	
	private final int id;
	private int cellColor;
	private double[] biomass;
	private double[] convectionRHS1;
	private double[] convectionRHS2;
	private double[] deltaBiomass;
	private double[][] fluxes;
	private int[] FBAstatus;
	  
	private CometsParameters cParams;
	private FBAParameters pParams;
	
	/**
	 * Creates a new <code>FBACell</code> with randomized biomass from 0->1 g for each species.
	 * @param x the new cell's column
	 * @param y the new cell's row
	 * @param world the <code>FBAWorld</code> where this cell will live
	 * @param fbaModels the array of <code>Models</code> (FBAModels) that will act in this cell
	 * @param cParams the parameters from the currently loaded <code>Comets</code>
	 * @param pParams the currently loaded package parameters
	 */
	public FBACell(int x, int y, 
				   FBAWorld world, 
				   FBAModel[] fbaModels, 
				   CometsParameters cParams, 
				   FBAParameters pParams)
	{
		this(x, y,
			 new double[fbaModels.length],
			 world,
			 fbaModels,
			 cParams,
			 pParams);
		for (int i=0; i<biomass.length; i++)
		{
			biomass[i] = Utility.randomDouble(); 
		}
	}

	/**
	 * Creates a new <code>FBACell</code> with the given levels of biomass.
	 * @param x the new cell's column
	 * @param y the new cell's row
	 * @param world the <code>FBAWorld</code> where this cell will live
	 * @param fbaModels the array of <code>Models</code> (FBAModels) that will act in this cell
	 * @param cParams the parameters from the currently loaded <code>Comets</code>
	 * @param pParams the currently loaded package parameters
	 */
	public FBACell(int x, int y, 
				   double[] biomass,
				   FBAWorld world,
				   FBAModel[] fbaModels,
				   CometsParameters cParams,
				   FBAParameters pParams)
	{
		this.x = x;
		this.y = y;
		id = getNewCellID();
		deltaBiomass = new double[biomass.length];
		FBAstatus = new int[biomass.length];
		this.fbaModels = fbaModels;
		this.world = world;
		this.cParams = cParams;
		this.pParams = pParams;
		this.biomass = new double[fbaModels.length];
		setBiomass(biomass);
		this.convectionRHS1=new double[biomass.length];
		this.convectionRHS2=new double[biomass.length];

		if (cParams.showGraphics())
			cellColor = calculateColor();
		else
			cellColor = 0;
		fluxes = new double[fbaModels.length][];
		world.putCellAt(x, y, this);
		
		updateDiffusibility();
		/*

		if (!cParams.allowCellOverlap())
		{
			for (int i=0; i<biomass.length; i++)
			{
				if (biomass[i] == 0)
				{
					world.setDiffuseBiomassOut(x, y, i, false);
					world.setDiffuseBiomassIn(x, y, i, false);
				}
			}
		}
		*/
	}
	/**
	 * Creates a new 3D <code>FBACell</code> with the given levels of biomass.
	 * @param x the new cell's column
	 * @param y the new cell's row
	 * @param world the <code>FBAWorld</code> where this cell will live
	 * @param fbaModels the array of <code>Models</code> (FBAModels) that will act in this cell
	 * @param cParams the parameters from the currently loaded <code>Comets</code>
	 * @param pParams the currently loaded package parameters
	 */
	public FBACell(int x, int y, int z,
				   double[] biomass,
				   FBAWorld3D world3D,
				   FBAModel[] fbaModels,
				   CometsParameters cParams,
				   FBAParameters pParams)
	{
		this.x = x;
		this.y = y;
		this.z = z;
		id = getNewCellID();
		deltaBiomass = new double[biomass.length];
		FBAstatus = new int[biomass.length];
		this.fbaModels = fbaModels;
		this.world3D = world3D;
		this.cParams = cParams;
		this.pParams = pParams;
		this.biomass = new double[fbaModels.length];
		setBiomass3D(biomass);
		this.convectionRHS1=new double[biomass.length];
		this.convectionRHS2=new double[biomass.length];
		
		if (cParams.showGraphics())
			cellColor = calculateColor();
		else
			cellColor = 0;
		fluxes = new double[fbaModels.length][];
		world3D.putCellAt(x, y, z, this);		
		updateDiffusibility3D();
		/*

		if (!cParams.allowCellOverlap())
		{
			for (int i=0; i<biomass.length; i++)
			{
				if (biomass[i] == 0)
				{
					world.setDiffuseBiomassOut(x, y, i, false);
					world.setDiffuseBiomassIn(x, y, i, false);
				}
			}
		}
		*/
	}
	
	
	/**
	 * Changes the biomass in this <code>FBACell</code> by the given delta amount.
	 * <br>
	 * If any particular delta value sets that species' biomass below zero, it is reset
	 * to zero.
	 * <br>
	 * If the biomass of all species associated with this FBACell falls to zero, the 
	 * FBACell is counted as dead and removed. 
	 * @param delta the amount of biomass change for each species - in the same order
	 * as the Model[] array owned by <code>Comets</code>
	 * @return <code>Cell.CELL_OK</code> if there remains a positive amount 
	 * of biomass, or <code>Cell.CELL_DEAD</code> if not.
	 * 
	 */
	public int changeBiomass(double[] delta)
	{
		return setBiomass(delta, true);
	}
	
	/**
	 * Sets the biomass in this <code>FBACell</code> to be the given amount.
	 * <br>
	 * If any particular value sets that species' biomass below zero, it is reset
	 * to zero.
	 * <br>
	 * If the biomass of all species associated with this FBACell is zero, the 
	 * FBACell is counted as dead, and likely removed on the next world update. 
	 * @param values the amount of biomass to set for each species - in the same order
	 * as the Model[] array owned by <code>Comets</code>
	 * @return <code>Cell.CELL_OK</code> if there remains a positive amount 
	 * of biomass, or <code>Cell.CELL_DEAD</code> if not.
	 */
	public int setBiomass(double[] values)
	{
		return setBiomass(values, false);
	}
	/**
	 * Sets the biomass in this <code>FBACell</code> to be the given amount.
	 * <br>
	 * If any particular value sets that species' biomass below zero, it is reset
	 * to zero.
	 * <br>
	 * If the biomass of all species associated with this FBACell is zero, the 
	 * FBACell is counted as dead, and likely removed on the next world update. 
	 * @param values the amount of biomass to set for each species - in the same order
	 * as the Model[] array owned by <code>Comets</code>
	 * @return <code>Cell.CELL_OK</code> if there remains a positive amount 
	 * of biomass, or <code>Cell.CELL_DEAD</code> if not.
	 */
	public int setBiomass3D(double[] values)
	{
		return setBiomass3D(values, false);
	}	
	
	/**
	 * Sets the previous step convectionRHS1.
	 * @param values
	 */
	public void setConvectionRHS1(double[] values)
	{
		for (int i = 0; i < convectionRHS1.length; i++)
		{
				convectionRHS1[i] = values[i];
		}
	}
	
	/**
	 * Sets the previous step convectionRHS2.
	 * @param values
	 */
	public void setConvectionRHS2(double[] values)
	{
		for (int i = 0; i < convectionRHS2.length; i++)
		{
				convectionRHS2[i] = values[i];
		}
	}
	
	/**
	 * Either sets the biomass for this Cell to the quantities given in <code>values</code>
	 * (if delta is false), or adjusts it by those values if delta is true. 
	 * <br>
	 * If the biomass of all species associated with this FBACell is zero, the 
	 * FBACell is counted as dead, and likely removed on the next world update. 
	 * @param values the amount of biomass to set for each species - in the same order
	 * as the Model[] array owned by <code>Comets</code>
	 * @param delta if true, sum the values to the current biomass; if false, replace them
	 * @return <code>Cell.CELL_OK</code> if there remains a positive amount 
	 * of biomass, or <code>Cell.CELL_DEAD</code> if not.
	 */
	private int setBiomass(double[] values, boolean delta)
	{
		int numLost = 0;
		for (int i = 0; i < biomass.length; i++)
		{
			if (delta)
				biomass[i] += values[i];
			else
			{
				if (values[i] < 0)
					biomass[i] = 0;
				else
					biomass[i] = values[i];
			}
			if (biomass[i] < cParams.getMinSpaceBiomass())
			{
				biomass[i] = 0;
				numLost++;
			}
		}
		if (numLost == biomass.length)
			return die();
		if (cParams.showGraphics())
		{
			cellColor = calculateColor();
			// cellWorld.updateSpace(x, y);
		}
		updateDiffusibility();
		return CELL_OK;
	}
	/**
	 * Either sets the biomass for this Cell to the quantities given in <code>values</code>
	 * (if delta is false), or adjusts it by those values if delta is true. 
	 * <br>
	 * If the biomass of all species associated with this FBACell is zero, the 
	 * FBACell is counted as dead, and likely removed on the next world update. 
	 * @param values the amount of biomass to set for each species - in the same order
	 * as the Model[] array owned by <code>Comets</code>
	 * @param delta if true, sum the values to the current biomass; if false, replace them
	 * @return <code>Cell.CELL_OK</code> if there remains a positive amount 
	 * of biomass, or <code>Cell.CELL_DEAD</code> if not.
	 */
	private int setBiomass3D(double[] values, boolean delta)
	{
		int numLost = 0;
		for (int i = 0; i < biomass.length; i++)
		{
			if (delta)
				biomass[i] += values[i];
			else
			{
				if (values[i] < 0)
					biomass[i] = 0;
				else
					biomass[i] = values[i];
			}
			if (biomass[i] < cParams.getMinSpaceBiomass())
			{
				biomass[i] = 0;
				numLost++;
			}
		}
		if (numLost == biomass.length)
			return die();
		if (cParams.showGraphics())
		{
			cellColor = calculateColor();
			// cellWorld.updateSpace(x, y);
		}
		updateDiffusibility3D();
		return CELL_OK;
	}
	
	/**
	 * Updates the diffusability of the biomass in this <code>FBACell</code>. This should
	 * be called whenever there's a change to the biomass quantities controlled by the cell.
	 * <p>
	 * Essentially, this adjusts and enforces the rules of biomass diffusability into and out
	 * of the space this cell occupies, esspecially in the case where only one species can 
	 * be in a space at a time. E.g., if the biomass levels were changed so that one species
	 * replaces another, then more biomass from that species should be allowed to diffuse in, 
	 * and all other species should be prevented.
	 * <p>
	 * Finally, if this FBACell is full (the total biomass exceeds cParams.getMaxSpaceBiomass()),
	 * block all inward movement.
	 */
	private void updateDiffusibility()
	{
		// first, reset to original cell constraints
		// e.g. if we don't allow overlap, set in-diffusion to only
		// the currently assigned biomass
		// otherwise, everything's welcome in.
		for (int i=0; i<biomass.length; i++)
		{
			world.setDiffuseBiomassIn(x, y, i, true);
			if (biomass[i] == 0 && !cParams.allowCellOverlap())
			{
				world.setDiffuseBiomassIn(x, y, i, false);
				world.setDiffuseBiomassOut(x, y, i, false);
			}
		}

		// next, set diffusability based on how much biomass is present.
		if (Utility.sum(biomass) >= cParams.getMaxSpaceBiomass())
			for (int i=0; i<biomass.length; i++)
				world.setDiffuseBiomassIn(x, y, i, false);
	}
	/**
	 * Updates the diffusability of the biomass in this <code>FBACell</code>. This should
	 * be called whenever there's a change to the biomass quantities controlled by the cell.
	 * <p>
	 * Essentially, this adjusts and enforces the rules of biomass diffusability into and out
	 * of the space this cell occupies, esspecially in the case where only one species can 
	 * be in a space at a time. E.g., if the biomass levels were changed so that one species
	 * replaces another, then more biomass from that species should be allowed to diffuse in, 
	 * and all other species should be prevented.
	 * <p>
	 * Finally, if this FBACell is full (the total biomass exceeds cParams.getMaxSpaceBiomass()),
	 * block all inward movement.
	 */
	private void updateDiffusibility3D()
	{
		// first, reset to original cell constraints
		// e.g. if we don't allow overlap, set in-diffusion to only
		// the currently assigned biomass
		// otherwise, everything's welcome in.
		for (int i=0; i<biomass.length; i++)
		{
			world3D.setDiffuseBiomassIn(x, y, z, i, true);
			if (biomass[i] == 0 && !cParams.allowCellOverlap())
			{
				world3D.setDiffuseBiomassIn(x, y, z, i, false);
				world3D.setDiffuseBiomassOut(x, y, z, i, false);
			}
		}

		// next, set diffusability based on how much biomass is present.
		if (Utility.sum(biomass) >= cParams.getMaxSpaceBiomass())
			for (int i=0; i<biomass.length; i++)
				world3D.setDiffuseBiomassIn(x, y, z, i, false);
	}

	/* (non-Javadoc)
	 * @see edu.bu.segrelab.comets.Cell#attributesString()
	 */
	@Override
	public String attributesString()
	{
		return "temp attributes string";
	}

	/**
	 * Calculates and the color of this cell, based on how much biomass of each species is
	 * present. Currently only deals with up to 3 concurrent species in the cell.
	 * 
	 * //TODO Extend this to N species...
	 * 
	 * @return an unsigned int representing the color 
	 * @see Utility.pColor()
	 */
	private int calculateColor()
	{
		if (biomass.length == 2)
			return Utility.pColor((int)biomass[1]*(255/10), (int)biomass[0]*255/10, 55);
		if (biomass.length == 3)
			return Utility.pColor((int)biomass[1]*(255/10), (int)biomass[0]*255/10, 55 + (int)biomass[2]*255/10);
		return Utility.pColor(0, (int)biomass[0]*(255/10), 55);
    }
	
	/* (non-Javadoc)
	 * @see edu.bu.segrelab.comets.Cell#cellType()
	 */
	@Override
	public String cellType()
	{
		return "FBACell";
	}

	/**
	 * Signals to the cell that it should die, and returns the status of the newly dead FBACell.
	 * @return Cell.CELL_DEAD unless a problem occurs.
	 */
	public int die()
	{
		return CELL_DEAD;
	}

	/* (non-Javadoc)
	 * @see edu.bu.segrelab.comets.Cell#getColor()
	 */
	@Override
	/**
	 * Returns the current color of the <code>FBACell</code> as an integer
	 * @return an int representing the cell color
	 * @see Utility.pColor()
	 */
	public int getColor()
	{
		return cellColor;
	}

	/**
	 * Returns the current biomass present in the <code>FBACell</code>
	 * @return a double[] containing the current total biomass in the <code>FBACell</code>.
	 */
	public synchronized double[] getBiomass()
	{
		return biomass;
	}
	
	/**
	 * Returns the convectionRHS1 from the previous step in the <code>FBACell</code>
	 * @return a double[] containing the calculated ConvectionRHS1 from a previous step in the <code>FBACell</code>.
	 */
	public synchronized double[] getConvectionRHS1()
	{
		return convectionRHS1;
	}
	
	/**
	 * Returns the convectionRHS1 from 2 steps away in the <code>FBACell</code>
	 * @return a double[] containing the total convectionRHS2 from two steps away in the <code>FBACell</code>.
	 */
	public synchronized double[] getConvectionRHS2()
	{
		return convectionRHS2;
	}
	
	/**
	 * @return the most recent change in biomass that occurred in the cell, typically due
	 * to cell growth through FBA.
	 */
	public double[] getDeltaBiomass()
	{
		return deltaBiomass;
	}
	
	/**
	 * @return the status of FBA (feasible or infeasible).
	 */
	public int[] getFBAstatus()
	{
		return FBAstatus;
	}
	
	/**
	 * @return the fluxes calculated from the most recent FBA run for all species in the cell.
	 * This is a 2D double array. Each double[i][j] represents flux j from species i.
	 */
	public double[][] getFluxes() 
	{ 
		return fluxes; 
	}
	
	/* (non-Javadoc)
	 * @see edu.bu.segrelab.comets.Cell#getID()
	 */
	@Override
	public int getID()
	{
		return id;
	}

	/* (non-Javadoc)
	 * @see edu.bu.segrelab.comets.Cell#run()
	 */	
	@Override
	/**
	 * Tells the <code>FBACell</code> to run (e.g. perform FBA) on the given Models[] and
	 * biomass it contains, using the media set from the space it occupies in the
	 * <code>FBAWorld</code>. This should be called when the <code>FBACell</code> is to be 
	 * run with default behavior.
	 */
	public int run()
	{
		return run(fbaModels);
	}

	/**
	 * Works similar to FBACell.run(), but uses a passed set of models. If the number of models
	 * passed into this method is different than the number of individual biomasses that this
	 * <code>FBACell</code> is aware of, CometsConstants.PARAMS_ERROR is returned.
	 * <p>
	 * This is primarily used in conjunction with the multi-threaded FBA runs, but one can
	 * use it with mutated models and such.
	 * <p>
	 * It works by fetching the amount of media present in its surroundings in the FBAWorld,
	 * using that to calculate the bounds on exchange fluxes for the current FBAModel, then
	 * running the FBAModel. It then caches the flux solution and updates the biomass and
	 * media environment using the solution. If there are multiple species present in the
	 * <code>FBACell</code>, they are calculated in a random order.
	 * 
	 * @param models the array of models to be run with FBA
	 * @return Cell.CELL_OK if the run was successful, biomass was updated, and the local
	 * environment was updated; Cell.CELL_DEAD if there is no remaining biomass in the
	 * FBACell, and CometsConstants.PARAMS_ERROR if the number of models passed doesn't match
	 * the number of different species this FBACell is aware of. 
	 */
	public synchronized int run(FBAModel[] models)
	{
//		if (Comets.DIFFUSION_TEST_MODE)
//			return CELL_OK;
		deltaBiomass = new double[models.length];
		FBAstatus = new int[models.length];
		
		double rho = 1.0;
		
		// If we have multiple concurrent models in the cell, we want to update
		// them all in random order.
		int[] updateOrder = Utility.randomOrder(models.length);
		//unless cParams.randomOrder is false, 
		//in which case we run each model in the same order every time
		if (!pParams.getRandomOrder()){ 
			updateOrder = new int[models.length];
			for (int a=0; a<models.length; a++)
			{
				updateOrder[a]=a;
			}
		}
		
		for (int a=0; a<updateOrder.length; a++)
		{
			// i = the current model index to run.
			int i = updateOrder[a];

			// if no biomass, or the total biomass has overflowed, skip to the next.
			if (biomass[i] == 0 || Utility.sum(biomass) >= cParams.getMaxSpaceBiomass())
			{
				continue;
			}
			//try to activate, if not active skip to next.
		    if(cParams.getSimulateActivation() && !((FBAModel)models[i]).activate(cParams.getActivateRate()))
		    {
		    	continue;
		    }
			/************************* CALCULATE MAX EXCHANGE FLUXES ******************************/
			double[] media=null;//=world3D.getModelMediaAt(x, y, z, i);
			if(cParams.getNumLayers() == 1)
				media = world.getModelMediaAt(x, y, i);
			else if (cParams.getNumLayers() > 1)
				media = world3D.getModelMediaAt(x, y, z, i);
			
			double modelBiomass = biomass[i];
			
		    double[] lb = calculateMaxExchangeFluxes((FBAModel)models[i], media, modelBiomass, rho);
		    
		    ((FBAModel)models[i]).setExchLowerBounds(lb);
		    
			/************************* SET MAX BIOMASS *****************************/
			((FBAModel)models[i]).setBiomassUpperBound((cParams.getMaxSpaceBiomass() - (Utility.sum(biomass) + Utility.sum(deltaBiomass))) / (biomass[i] * cParams.getTimeStep()));
			
			if (DEBUG)
			{
				System.out.println("ALL FLUX BOUNDS");
				double[] debug_lb = ((FBAModel)models[i]).getLowerBounds();
				double[] debug_ub = ((FBAModel)models[i]).getUpperBounds();
				for (int j=0; j<lb.length; j++)
				{
					System.out.println(debug_lb[j] + "\t" + debug_ub[j]);
				}
			}
			
			/*************************** RUN THE FBA! ****************************/
			
			int stat = models[i].run();
			fluxes[i] = ((FBAModel)models[i]).getFluxes();
			if (stat != 5 && stat != 180)
			{
				// failure! don't do anything right now.
				// System.out.println("FBA failure status: " + stat);
			}
			if (stat == 5 || stat == 180)
			{
				// We have a valid solution, so update this cell and the world.

				/***************** GET MEDIA CONCENTRATION CHANGE ********************/
				double[] exchFlux = ((FBAModel)models[i]).getExchangeFluxes();
				double[] mediaDelta = new double[exchFlux.length];

				// modify the media (in mmol) by changing the fluxes back
				// into concentrations
				// delta = v * biomass * time_step

//				System.out.print("flux");
				for (int j=0; j<mediaDelta.length; j++)
				{
					mediaDelta[j] = (double)exchFlux[j] * biomass[i] * cParams.getTimeStep();
//					System.out.print("\t" + exchFlux[j]);
				}
				if(cParams.getNumLayers() == 1)
					world.changeModelMedia(x, y, i, mediaDelta);
				else if (cParams.getNumLayers() > 1)
					world3D.changeModelMedia(x, y, z, i, mediaDelta);

				/***************** GET BIOMASS CONCENTRATION CHANGE ****************/
				// biomass is in grams
				deltaBiomass[i] = (double)(((FBAModel)models[i]).getBiomassFluxSolution()) * cParams.getTimeStep() * biomass[i];
				if(deltaBiomass[i]<0.0)deltaBiomass[i]=0.0;
//				deltaBiomass[i] = (double)(((FBAModel)models[i]).getObjectiveFluxSolution()) * cParams.getTimeStep();
//				deltaBiomass[i] = (double)(((FBAModel)models[i]).getObjectiveFluxSolution());
//				System.out.println("solution: " + ((FBAModel)models[i]).getObjectiveSolution());
				
				if (cParams.showGraphics())
					cellColor = calculateColor();
				
				/***************** REPORT IF THERE IS AN INFEASIBLE SOLUTION ****************/
				
			}
			else  //there's an error
			{
//				System.out.print("flux");
//				double[] exchFlux = ((FBAModel)fbaModels[i]).getExchangeFluxes();
//				for (int j=0; j<exchFlux.length; j++)
//				{
//					System.out.print("\t" + 0);
//				}
//				System.out.println();
			}
		}

		return updateCellData(deltaBiomass, fluxes);
	}
	
	/**Returns the maximum exchange flux (lower bound) based on the set ExchangeStyle
	 * 
	 * @param model
	 * @param media the model-relevant media in the current cell as evaluated by 
	 * FBAWorld.getModelMediaAt(), *Not* the complete list of media
	 * @param modelBiomass the biomass of the model in the current cell
	 * @param rho
	 * @return
	 */
	private double[] calculateMaxExchangeFluxes(FBAModel model, double[] media, double modelBiomass, double rho){

		double[] lb = model.getBaseExchLowerBounds();
		//double[] ub = model.getBaseExchUpperBounds();
		
		if (DEBUG)
			System.out.println("Exchange reaction bounds:");

		double[] rates = new double[lb.length];
		
		switch (pParams.getExchangeStyle())
		{
			case MONOD :
				double[] kmArr = model.getExchangeKm();
				double[] vMaxArr = model.getExchangeVmax();
				double[] hillCoeffArr = model.getExchangeHillCoefficients();

//				double[] vTilde = new double[hillCoeffArr.length];

				for (int j=0; j<lb.length; j++)
				{
					double km = pParams.getDefaultKm();
					if (kmArr != null && kmArr.length > j && kmArr[j] > 0)
						km = kmArr[j];
					double vMax = pParams.getDefaultVmax();
					if (vMaxArr != null && vMaxArr.length > j && vMaxArr[j] > 0)
						vMax = vMaxArr[j];
					double hill = pParams.getDefaultHill();
					if (hillCoeffArr != null && hillCoeffArr.length > j && hillCoeffArr[j] > 0)
						hill = hillCoeffArr[j];

					// Start of modified code corrected lb 9/19/13 Ilija D.
					if(media[j]/(cParams.getTimeStep()*modelBiomass)<calcMichaelisMentenRate(media[j]/cParams.getSpaceVolume(), km, vMax, hill))
					{
						//If lb is positive, set rate to negative lb:
						if(lb[j]>0)
						{
							rates[j] = -Math.abs(lb[j]);
						}
						//If lb is negative, take absolute minimum of lb or monod:
						else
						{
							rates[j] = Math.min(Math.abs(lb[j]),Math.abs(media[j]/(cParams.getTimeStep()*modelBiomass)));
						}
					}
					else
					{
						//If lb is positive, set rate to negative lb:
						if(lb[j]>0)
						{
							rates[j] = -Math.abs(lb[j]);
						}
						//If lb is negative, take absolute minimum of lb or monod:
						else
						{
							rates[j] = Math.min(Math.abs(lb[j]),Math.abs(calcMichaelisMentenRate(media[j]/cParams.getSpaceVolume(), km, vMax, hill)));
						}
					}
				}

				break;
				
				
			case PSEUDO_MONOD :
				double[] alphaArr = model.getExchangeAlphaCoefficients();
				double[] wArr = model.getExchangeWCoefficients();
				
				for (int j=0; j<lb.length; j++)
				{
					double alpha = pParams.getDefaultAlpha();
					if (alphaArr != null && alphaArr.length > j && alphaArr[j] > 0)
						alpha = alphaArr[j];
					
					double w = pParams.getDefaultW();
					if (wArr != null && wArr.length > j && wArr[j] > 0)
						w = wArr[j];
					
					rates[j] = Math.min(Math.abs(lb[j]),
										Math.abs(calcPseudoMonodRate(media[j]/cParams.getSpaceVolume(), alpha, w)));
				}
				break;
				
				
			default :  // STANDARD_EXCHANGE
				for (int j=0; j<lb.length; j++)
				{
					rates[j] = Math.min(Math.abs(lb[j]),
										Math.abs(calcStandardExchange(media[j]/cParams.getSpaceVolume())));
				}	
				break;
		}

		for (int j=0; j<lb.length; j++)
		{
			lb[j] = -1 * rates[j]/rho;
		}
		
		if (DEBUG)
		{
			System.out.println("LOWER BOUNDS");
			for (int j=0; j<lb.length; j++) 
			{
				System.out.println(lb[j]);
			}
			System.out.println("//");
		}
		
		return lb;
	}
	
	private double calcMichaelisMentenRate(double mediaConc, double km, double vMax, double hill)
	{
		return mediaConc * vMax / (km + mediaConc);
	}
	
	private double calcPseudoMonodRate(double mediaConc, double alpha, double w)
	{
		return Math.min(mediaConc * alpha, w);
	}
	
	private double calcStandardExchange(double mediaConc)
	{
		return mediaConc;
	}
	
	/**
	 * Updates the data owned by this FBACell. The fluxes are stored internally, while the
	 * deltaBiomass is used to calculate the change in biomass applied to the FBACell.
	 * @param deltaBiomass the change in biomass to apply to this FBACell
	 * @param fluxes the fluxes to store in this FBACell
	 * @return Cell.CELL_DEAD if this cell has no more active biomass, Cell.CELL_OK 
	 * otherwise
	 */
	public int updateCellData(double[] deltaBiomass, double[][] fluxes)
	{
		this.deltaBiomass = deltaBiomass;
		this.fluxes = fluxes;
		
		// apply biomass death rate, regardless of whether growth is feasible.
		int numDead = 0;
		for (int i=0; i<biomass.length; i++)
		{
			deltaBiomass[i] -= cParams.getDeathRate() * biomass[i] * cParams.getTimeStep();
			biomass[i] += deltaBiomass[i];
			
			if (biomass[i] < cParams.getMinSpaceBiomass())
				biomass[i] = 0;
			if (biomass[i] == 0)
				numDead++;
		}
		if (cParams.showGraphics())
			cellColor = calculateColor();
		
		if (numDead == biomass.length)
			return die();
		if(cParams.getNumLayers()==1)
			updateDiffusibility();
		else if(cParams.getNumLayers()>1)
			updateDiffusibility3D();
		
		return CELL_OK;
	}

	/* (non-Javadoc)
	 * @see edu.bu.segrelab.comets.Cell#statisticsString()
	 */
	@Override
	public String statisticsString()
	{
		return "temp statistics string";
	}

	/* (non-Javadoc)
	 * @see edu.bu.segrelab.comets.Cell#toString()
	 */
	@Override
	public String toString()
	{
		String str = ("FBA Cell\nPos: (" + x + ", " + y + ")\nBiomass: " + biomass[0]);
		for (int i = 1; i < biomass.length; i++)
		{
			str += (", " + biomass[i]);
		}
		str += ("\n");
		if (fluxes != null)
		{
			for (int i=0; i<fluxes.length; i++)
			{
				str += "model " + i + ": ";
				if (fluxes[i] == null)
					str += "no current fluxes";
				else
				{
					str += "fluxes: ";
					for (int j=0; j<fluxes[i].length; j++)
					{
						str += (fluxes[i][j] + "\t");
					}
				}
				str += ("\n");
			}
		}
		return str;
		
	}

	/**
	 * Creates a clone of this <code>FBACell</code> linked to the passed backupWorld.
	 * @return a clone of this <code>FBACell</code> cast as a <code>Cell</code>
	 */
	public Cell backup(World2D backupWorld)
	{
		FBACell bak = new FBACell(x, y, biomass.clone(), (FBAWorld)backupWorld, fbaModels, cParams, pParams);
		
		return bak;
	}
	
	/**
	 * Creates a clone of this <code>FBACell</code> linked to the passed backupWorld.
	 * @return a clone of this <code>FBACell</code> cast as a <code>Cell</code>
	 */
	public Cell backup(World3D backupWorld)
	{
		FBACell bak = new FBACell(x, y, z, biomass.clone(), (FBAWorld3D)backupWorld, fbaModels, cParams, pParams);
		
		return bak;
	}
	
	@Override
	/**
	 * Changes the models known by this <code>FBACell</code>. If the newModels array contains
	 * a subset of the oldModels array, then the internal data storage (biomass, fluxes, etc.)
	 * is adjusted to match. Otherwise it is reset
	 * @param oldModels the old array of <code>Models</code> being changed
	 * @param newModels the new array of <code>Models</code> to be used by this FBACell 
	 */
	public void changeModelsInCell(Model[] oldModels, Model[] newModels)
	{
		/*
		 *  update the old models with new models
		 *  this means...
		 *  1. Models[] array
		 *  2. biomass[] array
		 *  3. deltaBiomass[] array
		 *  4. fluxes[][] array
		 */
		
		// idx will map the index of the new models to the old set.
		// i.e. if oldModels[i] == newModels[j], idx[i] = j
		// otherwise, idx[i] = -1
		int[] idx = new int[oldModels.length];
		for (int i=0; i<oldModels.length; i++)
			idx[i] = -1;

		// Do the mapping
		for (int i=0; i<oldModels.length; i++)
		{
			for (int j=0; j<newModels.length; j++)
			{
				if (oldModels[i].equals(newModels[j]))
					idx[i] = j;
			}
		}

		double[] newBiomass = new double[newModels.length];
		double[] newDeltaBiomass = new double[newModels.length];
		double[][] newFluxes = new double[newModels.length][];

		for (int i=0; i<idx.length; i++)
		{
			// If the new model index matches an older one (and not -1),
			// move the appropriate information into the new location.
			if (idx[i] != -1)
			{
				newBiomass[idx[i]] = biomass[i];
				newDeltaBiomass[idx[i]] = deltaBiomass[i];
				newFluxes[idx[i]] = fluxes[i];
			}
		}

		// Clean up and save the new stuff
		biomass = newBiomass;
		deltaBiomass = newDeltaBiomass;
		fluxes = newFluxes;
		fbaModels = new FBAModel[newModels.length];
		for (int i=0; i<newModels.length; i++)
			fbaModels[i] = (FBAModel)newModels[i];
	}
	
	/**Return the full list of media in this cell position
	 * 
	 * @return
	 */
	public double[] getMedia(){
		if(cParams.getNumLayers() == 1) //2d World
			return world.getMediaAt(x,y);
		else if (cParams.getNumLayers() > 1) //3D world
			return world3D.getMediaAt(x, y, z);
		return null;
	}
	
	public Comets getComets(){
		if(cParams.getNumLayers() == 1) //2d World
			return world.getComets();
		else if (cParams.getNumLayers() > 1) //3D world
			return world3D.getComets();
		return null;
	}
}
