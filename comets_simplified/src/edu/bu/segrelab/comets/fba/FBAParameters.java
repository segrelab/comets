package edu.bu.segrelab.comets.fba;

import java.io.PrintWriter;
import java.util.Map;
import java.util.HashMap;

import edu.bu.segrelab.comets.Comets;
import edu.bu.segrelab.comets.Model;
import edu.bu.segrelab.comets.PackageParameters;
import edu.bu.segrelab.comets.fba.ui.DiffusionParametersPanel;
import edu.bu.segrelab.comets.fba.ui.ExchangeParametersPanel;
import edu.bu.segrelab.comets.fba.ui.LogParametersPanel;
import edu.bu.segrelab.comets.fba.ui.ThreadParametersPanel;
import edu.bu.segrelab.comets.ui.ParametersPanel;
import edu.bu.segrelab.comets.util.ParameterState;
import edu.bu.segrelab.comets.ParameterType;

/**
 * FBAParameters
 * -------------
 * Defines the <code>PackageParameters</code> specific for doing FBA.
 * This cribs some of the code and style from the CometsParameters class, especially
 * how it loads and populates itself.
 *
 * @author Bill Riehl briehl@bu.edu
 */
public class FBAParameters implements PackageParameters
{
	public enum LogFormat
	{
		MATLAB("Matlab"),
		COMETS("COMETS");

		private String name;
		private LogFormat(String name)
		{
			this.name = name;
		}

		public String toString()
		{
			return name;
		}

		public static LogFormat findByName(String name)
		{
			for (LogFormat format : LogFormat.values())
			{
				if (format.toString().equalsIgnoreCase(name))
					return format;
			}
			return null;
		}
	}

	public enum BiomassMotionStyle
	{
		DIFFUSION_CN("Diffusion 2D(Crank-Nicolson)"),
		DIFFUSION_EP("Diffusion 2D(Eight Point)"),
		DIFFUSION_3D("Diffusion 3D"),
		CONVECTION_2D("Convection 2D"),
		CONVECTION_3D("Convection 3D");
		//LEVEL_SET("Level Set Relaxation");

		private String name;
		private BiomassMotionStyle(String name)
		{
			this.name = name;
		}

		public String getName()
		{
			return name;
		}

		public String toString()
		{
			return name;
		}

		public static BiomassMotionStyle findByName(String name)
		{
			//Before 3D was introduced, old versions didn't include "2D" in
			//this style. This check is needed to not break older input files
			if (name.equalsIgnoreCase("Diffusion (Crank-Nicolson)")){
				return DIFFUSION_CN;
			}

			for (BiomassMotionStyle style : BiomassMotionStyle.values())
			{
				if (style.toString().equalsIgnoreCase(name))
					return style;
			}
			return null;
		}
	}

	public enum ExchangeStyle 
	{
		STANDARD("Standard FBA"),
		MONOD("Monod Style"),
		PSEUDO_MONOD("Pseudo-Monod Style");

		private String name;
		private ExchangeStyle(String name)
		{
			this.name = name;
		}

		public String getName()
		{
			return name;
		}

		public String toString()
		{
			return getName();
		}

		public static ExchangeStyle findByName(String name)
		{
			for (ExchangeStyle style : ExchangeStyle.values())
			{
				if (style.toString().equalsIgnoreCase(name))
					return style;
			}
			return null;
		}
	}

	/*---------------------- some constants ----------------------*/
	public static final int MATLAB_FORMAT = 0;				// Matlab log file format
	public static final int COMETS_FORMAT = 1;				// "COMETS" log file format (something I whipped up)

	public static final int BIOMASS_DIFFUSION = 6;
	public static final int BIOMASS_LEVEL_SET = 7;

	/*------------------------ FBAComets Parameters -------------------------*/
	private boolean writeFluxLog,
	writeMediaLog,
	writeBiomassLog,
	writeVelocityLog,
	writeTotalBiomassLog,
	writeMatFile,
	useLogNameTimeStamp,
	randomOrder = true, //shuffle the order each model in a cell is run
	monodOverride,
	pseudoOverride; 

	private String fluxLogName,
	mediaLogName,
	biomassLogName,
	velocityLogName,
	totalBiomassLogName,
	matFileName;

	private String manifestFileName = "COMETS_manifest.txt";
	private final String nopathManifestFileName="COMETS_manifest.txt";

	private int numRunThreads = 1,
			numDiffPerStep = 10,
			fluxLogRate = 1,
			mediaLogRate = 1,
			biomassLogRate = 1,
			velocityLogRate = 1,
			totalBiomassLogRate = 1,
			numExRxnSubsteps = 12, //12 chosen as default so if timestep is 1h, minimum substep is < 1sec
			matFileRate = 1;

	private long randomSeed=0;

	private ExchangeStyle exchangeStyle = ExchangeStyle.STANDARD;

	private BiomassMotionStyle biomassMotionStyle = BiomassMotionStyle.DIFFUSION_CN;

	private LogFormat biomassLogFormat = LogFormat.MATLAB,
			mediaLogFormat = LogFormat.MATLAB,
			fluxLogFormat = LogFormat.MATLAB,
			velocityLogFormat = LogFormat.MATLAB;

	private static double growthDiffRate = 1e-7,
			flowDiffRate = 1e-7,
			defaultVmax = 10,
			defaultKm = 5,
			defaultHill = 2,
			defaultAlpha = 1,
			defaultW = 10,
			defaultDiffConst = 1e-5,
			minConcentration = 1e-26; //Here's hoping 1 atom per liter is enough precision

	private double[] defaultVelocityVector={0.0,0.0,0.0};


	private Map<String, ParametersPanel> parametersPanels;

	private Map<String, Object> paramValues;
	private Map<String, ParameterType> paramTypes;

	private Comets c;

	/**
	 * Initializes the <code>FBAParameters</code> with the default values.
	 * @param c
	 */
	public FBAParameters(Comets c)
	{
		this.c = c;
		writeFluxLog = false;
		writeMediaLog = false;
		writeBiomassLog = false;
		writeVelocityLog = false;
		writeTotalBiomassLog = false;
		writeMatFile = false;
		useLogNameTimeStamp = true;

		fluxLogName = "flux_log.txt";
		mediaLogName = "media_log.txt";
		biomassLogName = "biomass_log.txt";
		velocityLogName = "velocity_log.txt";
		matFileName = "comets_log.mat";
		totalBiomassLogName = "total_biomass_log.txt";

		paramValues = new HashMap<String, Object>();
		paramTypes = new HashMap<String, ParameterType>();

		saveParameterState();

		parametersPanels = new HashMap<String, ParametersPanel>();
		DiffusionParametersPanel dpp = new DiffusionParametersPanel(this);
		ExchangeParametersPanel epp = new ExchangeParametersPanel(this);
		LogParametersPanel lpp = new LogParametersPanel(this);
		ThreadParametersPanel tpp = new ThreadParametersPanel(this);
		parametersPanels.put(dpp.getName(), dpp);
		parametersPanels.put(epp.getName(), epp);
		parametersPanels.put(lpp.getName(), lpp);
		parametersPanels.put(tpp.getName(), tpp);
	}

	public void saveParameterState()
	{
		paramValues.put("writefluxlog", new Boolean(writeFluxLog));
		paramTypes.put("writefluxlog", ParameterType.BOOLEAN);

		paramValues.put("writemedialog", new Boolean(writeMediaLog));
		paramTypes.put("writemedialog", ParameterType.BOOLEAN);

		paramValues.put("writebiomasslog", new Boolean(writeBiomassLog));
		paramTypes.put("writebiomasslog", ParameterType.BOOLEAN);

		paramValues.put("writevelocitylog", new Boolean(writeVelocityLog));
		paramTypes.put("writevelocitylog", ParameterType.BOOLEAN);
		
		paramValues.put("writetotalbiomasslog", new Boolean(writeTotalBiomassLog));
		paramTypes.put("writetotalbiomasslog", ParameterType.BOOLEAN);

		paramValues.put("writematfile", new Boolean(writeMatFile));
		paramTypes.put("writematfile", ParameterType.BOOLEAN);

		paramValues.put("uselognametimestamp", new Boolean(useLogNameTimeStamp));
		paramTypes.put("uselognametimestamp", ParameterType.BOOLEAN);

		paramValues.put("fluxlogname", fluxLogName);
		paramTypes.put("fluxlogname", ParameterType.STRING);

		paramValues.put("medialogname", mediaLogName);
		paramTypes.put("medialogname", ParameterType.STRING);

		paramValues.put("biomasslogname", biomassLogName);
		paramTypes.put("biomasslogname", ParameterType.STRING);

		paramValues.put("velocitylogname", velocityLogName);
		paramTypes.put("velocitylogname", ParameterType.STRING);
		
		paramValues.put("totalbiomasslogname", totalBiomassLogName);
		paramTypes.put("totalbiomasslogname", ParameterType.STRING);

		paramValues.put("matfilename", matFileName);
		paramTypes.put("matfilename", ParameterType.STRING);

		paramValues.put("fluxlogformat", fluxLogFormat);
		paramTypes.put("fluxlogformat", ParameterType.STRING);

		paramValues.put("medialogformat", mediaLogFormat);
		paramTypes.put("medialogformat", ParameterType.STRING);

		paramValues.put("biomasslogformat", biomassLogFormat);
		paramTypes.put("biomasslogformat", ParameterType.STRING);
		
		paramValues.put("velocitylogformat", velocityLogFormat);
		paramTypes.put("velocitylogformat", ParameterType.STRING);

		paramValues.put("numrunthreads", new Integer(numRunThreads));
		paramTypes.put("numrunthreads", ParameterType.INT);

		paramValues.put("growthdiffrate", new Double(growthDiffRate));
		paramTypes.put("growthdiffrate", ParameterType.DOUBLE);

		paramValues.put("flowdiffrate", new Double(flowDiffRate));
		paramTypes.put("flowdiffrate", ParameterType.DOUBLE);		

		paramValues.put("exchangestyle", exchangeStyle);
		paramTypes.put("exchangestyle", ParameterType.STRING);

		paramValues.put("biomassmotionstyle", biomassMotionStyle);
		paramTypes.put("biomassmotionstyle", ParameterType.STRING);

		paramValues.put("defaultvmax", new Double(defaultVmax));
		paramTypes.put("defaultvmax", ParameterType.DOUBLE);

		paramValues.put("defaultkm", new Double(defaultKm));
		paramTypes.put("defaultkm", ParameterType.DOUBLE);

		paramValues.put("defaulthill", new Double(defaultHill));
		paramTypes.put("defaulthill", ParameterType.DOUBLE);

		paramValues.put("defaultalpha", new Double(defaultAlpha));
		paramTypes.put("defaultalpha", ParameterType.DOUBLE);

		paramValues.put("defaultw", new Double(defaultW));
		paramTypes.put("defaultw", ParameterType.DOUBLE);

		paramValues.put("fluxlograte", new Integer(fluxLogRate));
		paramTypes.put("fluxlograte", ParameterType.INT);

		paramValues.put("medialograte", new Integer(mediaLogRate));
		paramTypes.put("medialograte", ParameterType.INT);

		paramValues.put("biomasslograte", new Integer(biomassLogRate));
		paramTypes.put("biomasslograte", ParameterType.INT);
		
		paramValues.put("velocitylograte", new Integer(velocityLogRate));
		paramTypes.put("velocitylograte", ParameterType.INT);

		paramValues.put("totalbiomasslograte", new Integer(totalBiomassLogRate));
		paramTypes.put("totalbiomasslograte", ParameterType.INT);

		paramValues.put("matfilerate", new Integer(matFileRate));
		paramTypes.put("matfilerate", ParameterType.INT);

		paramValues.put("defaultdiffconst", new Double(defaultDiffConst));
		paramTypes.put("defaultdiffconst", ParameterType.DOUBLE);

		paramValues.put("numdiffperstep", new Integer(numDiffPerStep));
		paramTypes.put("numdiffperstep", ParameterType.INT);

		paramValues.put("numexrxnsubsteps", new Integer(numExRxnSubsteps));
		paramTypes.put("numexrxnsubsteps", ParameterType.INT);

		paramValues.put("randomseed", new Long(randomSeed));
		paramTypes.put("randomseed", ParameterType.LONG);

		paramValues.put("randomorder", new Boolean(randomOrder));
		paramTypes.put("randomorder", ParameterType.BOOLEAN);
	}

	public void loadParameterState()
	{
		writeFluxLog(((Boolean)paramValues.get("writefluxlog")).booleanValue());
		writeMediaLog(((Boolean)paramValues.get("writemedialog")).booleanValue());
		writeBiomassLog(((Boolean)paramValues.get("writebiomasslog")).booleanValue());
		writeVelocityLog(((Boolean)paramValues.get("writevelocitylog")).booleanValue());
		writeTotalBiomassLog(((Boolean)paramValues.get("writetotalbiomasslog")).booleanValue());
		writeMatFile(((Boolean)paramValues.get("writematfile")).booleanValue());
		useLogNameTimeStamp(((Boolean)paramValues.get("uselognametimestamp")).booleanValue());
		setFluxLogName((String)paramValues.get("fluxlogname"));
		setMediaLogName((String)paramValues.get("medialogname"));
		setBiomassLogName((String)paramValues.get("biomasslogname"));
		setVelocityLogName((String)paramValues.get("velocitylogname"));
		setTotalBiomassLogName((String)paramValues.get("totalbiomasslogname"));
		setMatFileName((String)paramValues.get("matfilename"));
		setRandomOrder(((Boolean)paramValues.get("randomorder")).booleanValue());

		if(paramValues.get("fluxlogformat") instanceof String)
			setFluxLogFormat(LogFormat.findByName((String)paramValues.get("fluxlogformat")));
		else
			setFluxLogFormat((LogFormat)paramValues.get("fluxlogformat"));

		//setMediaLogFormat((LogFormat)paramValues.get("medialogformat"));

		if(paramValues.get("medialogformat") instanceof String)
			setMediaLogFormat(LogFormat.findByName((String)paramValues.get("medialogformat")));
		else
			setMediaLogFormat((LogFormat)paramValues.get("medialogformat"));

		//setBiomassLogFormat((LogFormat)paramValues.get("biomasslogformat"));

		if(paramValues.get("biomasslogformat") instanceof String)
			setBiomassLogFormat(LogFormat.findByName((String)paramValues.get("biomasslogformat")));
		else
			setBiomassLogFormat((LogFormat)paramValues.get("biomasslogformat"));
		
		if(paramValues.get("velocitylogformat") instanceof String)
			setVelocityLogFormat(LogFormat.findByName((String)paramValues.get("velocitylogformat")));
		else
			setVelocityLogFormat((LogFormat)paramValues.get("velocitylogformat"));
		
		setNumRunThreads(((Integer)paramValues.get("numrunthreads")).intValue());
		setGrowthDiffRate(((Double)paramValues.get("growthdiffrate")).doubleValue());
		setFlowDiffRate(((Double)paramValues.get("flowdiffrate")).doubleValue());
		//setExchangeStyle(((ExchangeStyle)paramValues.get("exchangestyle")));

		if(paramValues.get("exchangestyle") instanceof String)
			setExchangeStyle((ExchangeStyle.findByName((String)paramValues.get("exchangestyle"))));
		else 
			setExchangeStyle(((ExchangeStyle)paramValues.get("exchangestyle")));

		//setBiomassMotionStyle((BiomassMotionStyle)paramValues.get("biomassmotionstyle"));

		if(paramValues.get("biomassmotionstyle") instanceof String)
			setBiomassMotionStyle(BiomassMotionStyle.findByName((String)paramValues.get("biomassmotionstyle")));
		else
			setBiomassMotionStyle((BiomassMotionStyle)paramValues.get("biomassmotionstyle"));

		setDefaultVmax(((Double)paramValues.get("defaultvmax")).doubleValue());
		setDefaultKm(((Double)paramValues.get("defaultkm")).doubleValue());
		setDefaultHill(((Double)paramValues.get("defaulthill")).doubleValue());
		setDefaultAlpha(((Double)paramValues.get("defaultalpha")).doubleValue());
		setDefaultW(((Double)paramValues.get("defaultw")).doubleValue());
		setFluxLogRate(((Integer)paramValues.get("fluxlograte")).intValue());
		setMediaLogRate(((Integer)paramValues.get("medialograte")).intValue());
		setBiomassLogRate(((Integer)paramValues.get("biomasslograte")).intValue());
		setVelocityLogRate(((Integer)paramValues.get("velocitylograte")).intValue());
		setTotalBiomassLogRate(((Integer)paramValues.get("totalbiomasslograte")).intValue());
		setMatFileRate(((Integer)paramValues.get("matfilerate")).intValue());
		setNumDiffusionsPerStep(((Integer)paramValues.get("numdiffperstep")).intValue());
		setDefaultDiffusionConstant(((Double)paramValues.get("defaultdiffconst")).doubleValue());
		setRandomSeed(((Long)paramValues.get("randomseed")).longValue());
	}

	public ParameterState setParameter(String p, String v)
	{
		ParameterType t = paramTypes.get(p);
		if (t == null)
			return ParameterState.NOT_FOUND;

		switch(t)
		{
		case BOOLEAN :
			if (!v.equalsIgnoreCase("false") && !v.equalsIgnoreCase("true"))
				return ParameterState.WRONG_TYPE;
			paramValues.put(p, new Boolean(Boolean.parseBoolean(v)));
			break;
		case DOUBLE :
			try
			{
				paramValues.put(p, Double.parseDouble(v));
			}
			catch (NumberFormatException e)
			{
				return ParameterState.WRONG_TYPE;
			}
			break;
		case INT :
			try
			{
				paramValues.put(p, Integer.parseInt(v));
			}
			catch (NumberFormatException e)
			{
				return ParameterState.WRONG_TYPE;
			}
			break;
		case LONG :
			try
			{
				paramValues.put(p, Long.parseLong(v));
			}
			catch (NumberFormatException e)
			{
				return ParameterState.WRONG_TYPE;
			}
			break;
		case STRING :
			paramValues.put(p, v);
			break;
		default :
			break;
			//			case LOG_FORMAT :
			//				LogFormat format = LogFormat.findByName(v);
			//				if (format == null)
			//					return ParameterState.WRONG_TYPE;
			//				else
			//					paramValues.put(p, format);
			//				break;
			//			case BIOMASS_MOTION_STYLE :
			//				BiomassMotionStyle biomassStyle = BiomassMotionStyle.findByName(v);
			//				if (biomassStyle == null)
			//					return ParameterState.WRONG_TYPE;
			//				else
			//					paramValues.put(p, biomassStyle);
			//				break;
			//			case EXCHANGE_STYLE :
			//				ExchangeStyle exchStyle = ExchangeStyle.findByName(v);
			//				if (exchStyle == null)
			//					return ParameterState.WRONG_TYPE;
			//				break;
		}
		return ParameterState.OK;
	}

	public double getDefaultDiffusionConstant()
	{
		return defaultDiffConst;
	}

	public double[] getDefaultVelocityVector()
	{
		return defaultVelocityVector;
	}

	public void setDefaultDiffusionConstant(double d)
	{
		if (d < 0)
			return;

		defaultDiffConst = d;
		//		Model[] models = c.getModels();
		//		for (int i=0; i<models.length; i++)
		//		{
		//			((FBAModel)models[i]).setDefaultMetabDiffConst(d);
		//		}
		if (c.getWorld() != null)
			((FBAWorld)c.getWorld()).setDefaultMediaDiffusionConstant(d);
	}

	public void setDefaultVelocityVector(double Vx, double Vy, double Vz)
	{
		defaultVelocityVector[0] = Vx;
		defaultVelocityVector[1] = Vy;
		defaultVelocityVector[2] = Vz;
	}

	public int getNumDiffusionsPerStep()
	{
		return numDiffPerStep;
	}

	public void setNumDiffusionsPerStep(int n)
	{
		if (n >= 0)
			numDiffPerStep = n;
	}

	public int getNumExRxnSubsteps()
	{
		return numExRxnSubsteps; 
	}

	public void setNumExRxnSubsteps(int n)
	{
		if (n > 1) numExRxnSubsteps = n;
		else numExRxnSubsteps = 1;
	}

	/**
	 * @return the number of simulation steps that occur between every flux log write
	 */
	public int getFluxLogRate() 
	{
		return fluxLogRate; 
	}

	/**
	 * Sets the number of steps that occur between every flux log write. If <code>i</code>
	 * is less than zero, nothing is changed.
	 * @param i
	 */
	public void setFluxLogRate(int i)
	{
		if (i > 0)
			fluxLogRate = i;
	}

	/**
	 * @return the number of simulation steps that occur between every media log write
	 */
	public int getMediaLogRate() 
	{ 
		return mediaLogRate; 
	}

	/**
	 * Sets the number of steps that occur between every media log write. If <code>i</code>
	 * is less than zero, nothing is changed.
	 * @param i
	 */
	public void setMediaLogRate(int i)
	{
		if (i > 0)
			mediaLogRate = i;
	}

	/**
	 * @return the number of simulation steps that occur between every biomass log write
	 */
	public int getBiomassLogRate() 
	{ 
		return biomassLogRate; 
	}

	/**
	 * Sets the number of steps that occur between every biomass log write. If 
	 * <code>i</code> is less than zero, nothing is changed.
	 * @param i
	 */
	public void setBiomassLogRate(int i)
	{
		if (i > 0)
			biomassLogRate = i;
	}

	/**
	 * @return the number of simulation steps that occur between every flux log write
	 */
	public int getVelocityLogRate() 
	{
		return velocityLogRate; 
	}

	/**
	 * Sets the number of steps that occur between every flux log write. If <code>i</code>
	 * is less than zero, nothing is changed.
	 * @param i
	 */
	public void setVelocityLogRate(int i)
	{
		if (i > 0)
			velocityLogRate = i;
	}
	
	/**
	 * @return the number of simulation steps that occur between every total 
	 * biomass log write
	 */
	public int getTotalBiomassLogRate()
	{
		return totalBiomassLogRate; 
	}

	/**
	 * Sets the number of steps that occur between every total biomass log write. If 
	 * <code>i</code> is less than zero, nothing is changed.
	 * @param i
	 */
	public void setTotalBiomassLogRate(int i)
	{
		if (i > 0)
			totalBiomassLogRate = i;
	}

	/**
	 * @return the number of simulation steps that occur between every .mat file log write
	 */
	public int getMatFileRate()
	{
		return matFileRate; 
	}

	/**
	 * Sets the number of steps that occur between every .mat file log write. If 
	 * <code>i</code> is less than zero, nothing is changed.
	 * @param i
	 */
	public void setMatFileRate(int i)
	{
		if (i > 0)
			matFileRate = i;
	}

	/**
	 * @return the default Vmax for the Michaelis-Menten style media uptake
	 */
	public static double getDefaultVmax() 
	{ 
		return defaultVmax; 
	}

	/**
	 * Sets the default Vmax for the Michaelis-Mented style media uptake
	 * @param d if less than or equal to zero, nothing is changed
	 */
	public static void setDefaultVmax(double d)
	{
		if (d <= 0)
			return;
		defaultVmax = d;
	}

	/**
	 * Sets the default Vmax for the Michaelis-Mented style media uptake
	 * @param d if less than or equal to zero, nothing is changed
	 * @param applyToModels if true then the default Vmax in models will be overridden
	 */
	public void setDefaultVmax(double d, boolean applyToModels)
	{
		if (d <= 0)
			return;
		defaultVmax = d;
		if (applyToModels) {
			Model[] models = c.getModels();
			for (int i=0; i<models.length; i++)
			{
				((FBAModel)models[i]).setDefaultVmax(d);
			}
		}
	}


	/**
	 * @return the default Km for the Michaelis-Menten style media uptake
	 */
	public static double getDefaultKm() 
	{ 
		return defaultKm; 
	}


	/**
	 * Sets the default Km for the Michaelis-Menten style media uptake
	 * @param d if less than zero, nothing is changed
	 */
	public static void setDefaultKm(double d)
	{
		if (d < 0)
			return;
		defaultKm = d;
	}

	/**
	 * Sets the default Km for the Michaelis-Menten style media uptake
	 * @param d if less than zero, nothing is changed
	 * @param applyToModels if true then the default KM in models will be overridden
	 */
	public void setDefaultKm(double d, boolean applyToModels)
	{
		if (d < 0)
			return;

		defaultKm = d;
		if (applyToModels) {
			Model[] models = c.getModels();
			for (int i=0; i<models.length; i++)
			{
				((FBAModel)models[i]).setDefaultKm(d);
			}
		}
	}

	/**
	 * @return the default Hill parameter for the Monod style media uptake
	 */
	public static double getDefaultHill() 
	{ 
		return defaultHill; 
	}

	/**
	 * Sets the default Hill parameter for the Monod style media uptake
	 * @param d if less than or equal to zero, nothing is changed
	 */
	public static void setDefaultHill(double d)
	{
		if (d <= 0)
			return;
		defaultHill = d;
	}

	/**
	 * Sets the default Hill parameter for the Monod style media uptake
	 * @param d if less than or equal to zero, nothing is changed
	 * @param applyToModels if true then the default coefficient in models will be overridden
	 */
	public void setDefaultHill(double d, boolean applyToModels)
	{
		if (d <= 0)
			return;
		defaultHill = d;
		if (applyToModels) {
			Model[] models = c.getModels();
			for (int i=0; i<models.length; i++)
			{
				((FBAModel)models[i]).setDefaultHill(d);
			}
		}
	}

	public static double getDefaultAlpha()
	{
		return defaultAlpha;
	}

	public static void setDefaultAlpha(double d)
	{
		if (d <= 0)
			return;
		defaultAlpha = d;
	}


	public void setDefaultAlpha(double d, boolean applyToModels)
	{
		if (d <= 0)
			return;
		if (applyToModels) {
			defaultAlpha = d;
			Model[] models = c.getModels();
			for (int i=0; i<models.length; i++)
			{
				((FBAModel)models[i]).setDefaultAlpha(d);
			}
		}
	}

	public static double getDefaultW()
	{
		return defaultW;
	}

	public static void setDefaultW(double d)
	{
		if (d <= 0)
			return;
		defaultW = d;
	}

	public void setDefaultW(double d, boolean applyToModels)
	{
		if (d <= 0)
			return;
		defaultW = d;
		if (applyToModels) {
			Model[] models = c.getModels();
			for (int i=0; i<models.length; i++)
			{
				((FBAModel)models[i]).setDefaultW(d);
			}
		}
	}

	/**
	 * Returns the current style of media exchange occuring within the model
	 * @return either <code>STANDARD_EXCHANGE</code>, <code>MM_EXCHANGE</code>, or
	 * <code>MONOD_EXCHANGE</code>
	 */
	public ExchangeStyle getExchangeStyle() { return exchangeStyle; }

	/**
	 * Sets the exchange style to <code>STANDARD_EXCHANGE</code>, <code>MM_EXCHANGE</code>, or
	 * <code>MONOD_EXCHANGE</code> 
	 * @param style if none of the above, nothing is changed
	 */
	public void setExchangeStyle(ExchangeStyle style)
	{
		exchangeStyle = style;
	}

	public BiomassMotionStyle getBiomassMotionStyle() 
	{ 
		return biomassMotionStyle;
	}

	public void setBiomassMotionStyle(BiomassMotionStyle style)
	{
		biomassMotionStyle = style;
	}

	/**
	 * @return the default growth diffusion rate (e.g., the biomass diffusion that occurs due
	 * to growth)
	 */
	public double getGrowthDiffRate() 
	{ 
		return growthDiffRate; 
	}

	/**
	 * Sets the default growth diffusion rate (e.g., the biomass diffusion that occurs due
	 * to growth)
	 * @param g if less than zero, nothing is changed
	 */
	public void setGrowthDiffRate(double g)
	{
		if (g >= 0)
			growthDiffRate = g;
	}

	/**
	 * @return the default flow diffusion rate (e.g., the biomass diffusion that occurs
	 * regardless of growth)
	 */
	public double getFlowDiffRate() 
	{ 
		return flowDiffRate; 
	}

	/**
	 * Sets the default flow diffusion rate (e.g., the biomass diffusion that occurs
	 * regardless of growth)
	 * @param f
	 */
	public void setFlowDiffRate(double f)
	{
		if (f >= 0)
			flowDiffRate = f;
	}

	/**
	 * @return true if a unique time stamp should be appended to the end of the log file name
	 */
	public boolean useLogNameTimeStamp()
	{ 
		return useLogNameTimeStamp; 
	}

	/**
	 * Set whether or not a unique time stamp should be appended to the end of each log file name.
	 * @param b if true, add a time stamp to the end of each log file.
	 */
	public void useLogNameTimeStamp(boolean b)
	{ 
		useLogNameTimeStamp = b; 
	}

	/**
	 * @return true if a biomass log will be written
	 */
	public boolean writeBiomassLog() 
	{
		return writeBiomassLog; 
	}

	/**
	 * Tells the program to write a cell line log or not. See the COMETS documentation
	 * for format details.
	 * @param b if true, writes a cell line log.
	 */
	public void writeBiomassLog(boolean b) 
	{ 
		writeBiomassLog = b; 
	}

	/**
	 * @return true if a media log will be written.
	 */
	public boolean writeMediaLog() 
	{
		return writeMediaLog; 
	}

	/**
	 * Tells COMETS to write a media log or not. See the COMETS documentation for format
	 * details. 
	 * @param b if true, writes a media log
	 */
	public void writeMediaLog(boolean b)
	{ 
		writeMediaLog = b; 
	}

	/**
	 * @return true if a flux log will be written
	 */
	public boolean writeFluxLog() 
	{ 
		return writeFluxLog; 
	}

	/**
	 * Tells COMETS to write a flux log or not. See the COMETS documentation for format
	 * details.
	 * @param b if true, write a flux log file
	 */
	public void writeFluxLog(boolean b) 
	{
		writeFluxLog = b; 
	}

	
	/**
	 * Sets the name of the flux log file, if one is going to be written.
	 * <br>
	 * See documentation for the format.
	 * @param name the name of the flux log file.
	 */
	public void setFluxLogName(String name)
	{ 
		fluxLogName = name; 
	}

	/**
	 * @return the name of the flux log file.
	 */
	public String getFluxLogName()
	{
		return fluxLogName; 
	}

	/**
	 * Sets the format of the flux log file. Currently only supports either
	 * MATLAB_FORMAT or COMETS_FORMAT, others are ignored.
	 * @param format
	 */
	public void setFluxLogFormat(LogFormat format) 
	{
		fluxLogFormat = format;
	}

	/**
	 * Returns the current flux log file format
	 * @return either MATLAB_FORMAT or COMETS_FORMAT
	 */
	public LogFormat getFluxLogFormat()
	{
		return fluxLogFormat; 
	}

	/**
	 * @return true if a velocity log will be written
	 */
	public boolean writeVelocityLog() 
	{ 
		return writeVelocityLog; 
	}

	/**
	 * Tells COMETS to write a velocity log or not. See the COMETS documentation for format
	 * details.
	 * @param b if true, write a velocity log file
	 */
	public void writeVelocityLog(boolean b) 
	{
		writeVelocityLog = b; 
	}
	
	/**
	 * Sets the name of the flux log file, if one is going to be written.
	 * <br>
	 * See documentation for the format.
	 * @param name the name of the flux log file.
	 */
	public void setVelocityLogName(String name)
	{ 
		velocityLogName = name; 
	}

	/**
	 * @return the name of the flux log file.
	 */
	public String getVelocityLogName()
	{
		return velocityLogName; 
	}

	/**
	 * Sets the format of the velocity log file. Currently only supports either
	 * MATLAB_FORMAT or COMETS_FORMAT, others are ignored.
	 * @param format
	 */
	public void setVelocityLogFormat(LogFormat format) 
	{
		velocityLogFormat = format;
	}

	/**
	 * Returns the current velocity log file format
	 * @return either MATLAB_FORMAT or COMETS_FORMAT
	 */
	public LogFormat getVelocityLogFormat()
	{
		return velocityLogFormat; 
	}

	
	/** Gets the name of the manifest file
	 *  without the path prepended.
	 * 
	 */
	public String getNopathManifestFileName()
	{
		return nopathManifestFileName;
	}

	/** Gets the name of the manifest file
	 * 
	 */
	public String getManifestFileName()
	{
		return manifestFileName;
	}

	/** Sets the name of the manifest file
	 * @param fileName
	 */
	public void setManifestFileName(String fileName)
	{
		manifestFileName = fileName;
	}

	/**
	 * Sets the name of the media log file, if one is going to be written.
	 * <br>
	 * See documentation for format details.
	 * <p>
	 * Note - this file takes a snapshot of the media state of the system at
	 * every time point. That's a matrix of all nutrient components at every 
	 * spot on the <code>FBAWorld</code>. Conservatively, if you have a 50x50
	 * grid, and 10 nutrient components, that's 25,000 32-bit double values
	 * every time point.
	 * <br>
	 * So this file will get VERY BIG, VERY FAST. I recommend changing the
	 * media log rate to something reasonable.
	 * @see setMediaLogRate(int i)
	 * @param name the name of the media log file.
	 */
	public void setMediaLogName(String name) { mediaLogName = name; }

	/**
	 * @return the name of the media log file.
	 */
	public String getMediaLogName() 
	{
		return mediaLogName; 
	}

	/**
	 * Sets the format of the media log file. Currently only supports either
	 * MATLAB_FORMAT or COMETS_FORMAT, others are ignored.
	 * @param format
	 */
	public void setMediaLogFormat(LogFormat format)
	{
		mediaLogFormat = format; 
	}

	/**
	 * Returns the current media log file format
	 * @return either MATLAB_FORMAT or COMETS_FORMAT
	 */
	public LogFormat getMediaLogFormat()
	{
		return mediaLogFormat; 
	}

	/**
	 * Sets the name of the biomass log file, if one is going to be written.
	 * <br>
	 * See documentation for format details.
	 * @param name the name of the biomass log file.
	 */
	public void setBiomassLogName(String name) 
	{ 
		biomassLogName = name; 
	}

	/**
	 * @return the name of the biomass log file.
	 */
	public String getBiomassLogName() 
	{ 
		return biomassLogName; 
	}

	/**
	 * Sets the format of the biomass log file. Currently only supports either
	 * MATLAB_FORMAT or COMETS_FORMAT, others are ignored.
	 * @param format
	 */
	public void setBiomassLogFormat(LogFormat format) 
	{ 
		biomassLogFormat = format; 
	}

	/**
	 * Returns the current biomass log file format
	 * @return either MATLAB_FORMAT or COMETS_FORMAT
	 */
	public LogFormat getBiomassLogFormat() 
	{ 
		return biomassLogFormat; 
	}

	/**
	 * @return true if a total biomass log will be written
	 */
	public boolean writeTotalBiomassLog() 
	{ 
		return writeTotalBiomassLog; 
	}

	/**
	 * Tells COMETS to write a total biomass log or not, tracking the sum total
	 * biomass at every time point. See the COMETS documentation for format
	 * details. 
	 * @param b if true, writes a total biomass log
	 */
	public void writeTotalBiomassLog(boolean b) 
	{ 
		writeTotalBiomassLog = b; 
	}

	/**
	 * @return the current file name for the total biomass log
	 */
	public String getTotalBiomassLogName()
	{
		return totalBiomassLogName; 
	}

	/**
	 * Sets the name of the total biomass log file, if one is going to be written.
	 * If there is no string (or an empty string), nothing is changed.
	 * <br>
	 * See documentation for the format.
	 * @param name the name of the flux log file.
	 */
	public void setTotalBiomassLogName(String s)
	{
		if (s.length() > 0)
			totalBiomassLogName = s;
	}


	/**
	 * @return true if a .mat file log will be written
	 */
	public boolean writeMatFile() 
	{ 
		return writeMatFile; 
	}

	/**
	 * Tells COMETS to write a .mat file log or not. 
	 * See the COMETS documentation for format
	 * details. 
	 * @param b if true, writes a .mat file log
	 */
	public void writeMatFile(boolean b) 
	{ 
		writeMatFile = b; 
	}

	/**
	 * @return the current file name for the .mat file log
	 */
	public String getMatFileName()
	{
		return matFileName; 
	}

	/**
	 * Sets the name of the .mat log file, if one is going to be written.
	 * If there is no string (or an empty string), nothing is changed.
	 * <br>
	 * See documentation for the format.
	 * @param name the name of the flux log file.
	 */
	public void setMatFileName(String s)
	{
		if (s.length() > 0)
			matFileName = s;
	}


	/**
	 * @return the number of FBA run threads to be used in simulation
	 */
	public int getNumRunThreads()
	{
		return numRunThreads; 
	}


	/**
	 * Sets the number of FBA run threads. When greater than 1, each thread will act
	 * as a worker during the simulation, calculating FBA solutions on any incomplete 
	 * space. Essentially, all spaces containing biomass are put on an assembly line, and
	 * each available worker gets assigned a model to calculate.
	 * <p>
	 * This works best on a multi-core system.
	 * @param n
	 */
	public void setNumRunThreads(int n)
	{
		if (n < 1)
			n = 1;
		numRunThreads = n;
	}

	/**
	 * Returns the seed of the random number generator.
	 * @return
	 */

	public long getRandomSeed()
	{
		return randomSeed;
	}

	/** 
	 * Sets the seed
	 * @param seed
	 */
	public void setRandomSeed(long seed)
	{
		randomSeed=seed;
	}

	/** Should the models in a cell be run in a random order?
	 * @return the randomOrder
	 */
	public boolean getRandomOrder() {
		return randomOrder;
	}

	/** Should the models in a cell be run in a random order?
	 * @param randomOrder the randomOrder to set
	 */
	public void setRandomOrder(boolean randomOrder) {
		this.randomOrder = randomOrder;
	}

	public String getLastDirectory()
	{
		return c.getParameters().getLastDirectory();
	}

	public void setLastDirectory(String path)
	{
		c.getParameters().setLastDirectory(path);
	}

	/** Lowest concentration allowed by calculations performed by a 
	 * ReactionModel before it gets rounded down to 0
	 * 
	 * @return
	 */
	public double getMinConcentration() {
		return minConcentration;
	}

	public void setMinConcentration(double min) {
		minConcentration = min;
	}

	public Map<String, ParametersPanel> getParametersPanels()
	{
		return parametersPanels;
	}

	public void dumpToFile(PrintWriter writer)
	{
		for (String name : paramValues.keySet())
		{
			writer.println(name + " = " + paramValues.get(name).toString());
		}
	}

	@Override
	public boolean hasParameter(String param)
	{
		return paramTypes.containsKey(param.toLowerCase());
	}

	@Override
	public ParameterType getType(String param)
	{
		return paramTypes.get(param.toLowerCase());
	}
	
	/**
	 * Should models' default KM, Vmax, and Hill Coeff be 
	 * overwritten by the parameters panel?
	 * @param b
	 */
	public void monodOverride(boolean b) { monodOverride = b; }
	
	public boolean getMonodOverride() { return monodOverride; }
	
	/**
	 * Should models' default Alpha and W coeffs be
	 * overwritten by the parameters panel?
	 * @param b
	 */
	public void pseudoOverride(boolean b) { pseudoOverride = b; }
	
	public boolean getPseudoOverride() { return pseudoOverride; }

}
