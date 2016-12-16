package edu.bu.segrelab.comets.test.integration;

import edu.bu.segrelab.comets.Comets;
import edu.bu.segrelab.comets.fba.FBACometsLoader;

/**
 * Class to run the GUI for manual testing
 * @author mquintin
 * December 2016
 */
public class RunGui {

	public static void main(String[] args) {
		Comets comets = new Comets(new String[]{"-loader", FBACometsLoader.class.getName()});
		//FBACometsLoader loader = new FBACometsLoader();
	}

}
