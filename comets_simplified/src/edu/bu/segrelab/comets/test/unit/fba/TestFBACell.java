package edu.bu.segrelab.comets.test.unit.fba;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import edu.bu.segrelab.comets.fba.FBACell;
import edu.bu.segrelab.comets.fba.FBAParameters;
import edu.bu.segrelab.comets.fba.FBAParameters.ExchangeStyle;
import edu.bu.segrelab.comets.test.classes.TComets;

public class TestFBACell {

	TComets comets;
	
	@BeforeAll
	static void setUpBeforeClass() throws Exception {
	}

	@AfterAll
	static void tearDownAfterClass() throws Exception {
	}

	@BeforeEach
	void setUp() throws Exception {
		comets = new TComets();
	}

	@AfterEach
	void tearDown() throws Exception {
	}

	/**Test that the various exchange styles behave similarly under normal circumstances
	 * 
	 */
	@Test
	void testExchangeStyleConsistency() {
		
		//test at timestep = 0.75h
		comets = resetTCometsForTestExchangeStyleConsistency(0.75);
		((FBAParameters) comets.getPackageParameters()).setExchangeStyle(ExchangeStyle.MONOD);
		double initBiomass = comets.getWorld().getBiomassAt(0, 0)[0];
		comets.doCommandLineRunWithoutLoading();
		double monod = comets.getWorld().getBiomassAt(0, 0)[0] - initBiomass;
		comets = resetTCometsForTestExchangeStyleConsistency(0.75);
		((FBAParameters) comets.getPackageParameters()).setExchangeStyle(ExchangeStyle.PSEUDO_MONOD);
		comets.doCommandLineRunWithoutLoading();
		double pseudo = comets.getWorld().getBiomassAt(0, 0)[0] - initBiomass;
		comets = resetTCometsForTestExchangeStyleConsistency(0.75);
		((FBAParameters) comets.getPackageParameters()).setExchangeStyle(ExchangeStyle.STANDARD);
		comets.doCommandLineRunWithoutLoading();
		double standard = comets.getWorld().getBiomassAt(0, 0)[0] - initBiomass;
		assertEquals(monod, standard, standard/100); //1% difference allowed
		assertEquals(pseudo, standard, standard/100);
		double standard1 = standard;
		
		//test at timestep = 1.5h
		comets = resetTCometsForTestExchangeStyleConsistency(1.5);
		((FBAParameters) comets.getPackageParameters()).setExchangeStyle(ExchangeStyle.MONOD);
		comets.doCommandLineRunWithoutLoading();
		monod = comets.getWorld().getBiomassAt(0, 0)[0] - initBiomass;
		comets = resetTCometsForTestExchangeStyleConsistency(1.5);
		((FBAParameters) comets.getPackageParameters()).setExchangeStyle(ExchangeStyle.PSEUDO_MONOD);
		comets.doCommandLineRunWithoutLoading();
		pseudo = comets.getWorld().getBiomassAt(0, 0)[0] - initBiomass;
		comets = resetTCometsForTestExchangeStyleConsistency(1.5);
		((FBAParameters) comets.getPackageParameters()).setExchangeStyle(ExchangeStyle.STANDARD);
		comets.doCommandLineRunWithoutLoading();
		standard = comets.getWorld().getBiomassAt(0, 0)[0] - initBiomass;
		assertEquals(monod, standard, standard/100); //1% difference allowed
		assertEquals(pseudo, standard, standard/100);
		double standard2 = standard;
		
		if (standard1 == standard2) {
			fail("COMETS parameters were not actually changed between tests");
		}
			

	}
	
	//Helper function for the above test
	private TComets resetTCometsForTestExchangeStyleConsistency(double timestep) {
		comets = new TComets();
		String layoutFilePath = "comets_layout_excessall_withrxns.txt";
		String scriptFilePath;
		try {
			scriptFilePath = comets.createScriptForLayout(layoutFilePath);
			comets.loadScript(scriptFilePath);
		} catch (IOException e) {
			e.printStackTrace();
		}
		comets.getParameters().setTimeStep(timestep);
		comets.getParameters().setMaxCycles(20);
		comets.getWorld().updateWorld();
		((FBACell) comets.getCells().get(0)).refreshParameters();

		return comets;
	}

}
