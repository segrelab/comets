package edu.bu.segrelab.comets.test.etc;

import java.net.URL;

import edu.bu.segrelab.comets.Comets;
import edu.bu.segrelab.comets.fba.FBACometsLoader;

/**A class to invoke in order to run the layout located in 
 * test/resources/testLayout. This is useful to explore via
 * breakpoints.
 * 
 * @author mquintin
 * @date 6/7/2017
 */
public class ExectueTestLayout {

	public ExectueTestLayout() {
		// TODO Auto-generated constructor stub
	}

	public static void main(String[] args) {
		//System.load("C:\\Windows\\System32\\GurobiJni70.dll");
		URL scriptURL = TestKineticParameters.class.getResource("../resources/testLayout/comets_script.txt");
		Comets comets = new Comets(new String[]{"-loader", FBACometsLoader.class.getName(),
				"-script", scriptURL.getPath()});
		int x = 1; //breakpoint
		int y = x; //also breakpoint
	}

}
