package edu.bu.segrelab.comets;

import java.awt.Color;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import edu.bu.segrelab.comets.ParameterType;
import edu.bu.segrelab.comets.util.ParameterState;
import edu.bu.segrelab.comets.util.Utility;

/**
 * This implements a set of global parameters used throughout the COMETS program.
 * Although none of these are static or anything, it is intended that only one
 * <code>CometsParameters</code> object is instantiated for a given run of COMETS,
 * then passed around to whatever classes need it.
 * 
 * From the programming side, this class knows what parameter names it has and 
 * what they are. So loading from a file is pretty simple - just put the name
 * of the parameter and the value in a whitespace-delimited way, and this class
 * can read the rest. It makes things a bit cluttered toward the top, where it has
 * to set up a couple of HashMaps to learn everything, but it works well and makes
 * for each IO error checking.
 * @author Bill Riehl briehl@bu.edu
 *
 */
public class CometsParameters implements CometsConstants
{
//	public enum ParameterType
//	{
//		BOOLEAN,
//		COLOR,
//		DOUBLE,
//		INT,
//		STRING;
//	}
//	private enum Params
//	{
//		TIME_STEP("timestep", Type.DOUBLE, Double.valueOf(1)),
//		DEATH_RATE("deathrate", Type.DOUBLE, Double.valueOf(0.1)),
//		MAX_SPACE_BIOMASS("maxspacebiomass", Type.DOUBLE, Double.valueOf(10)),
//		MIN_SPACE_BIOMASS("minspacebiomass", Type.DOUBLE, Double.valueOf(1e-10)),
//		SPACE_WIDTH("spacewidth", Type.DOUBLE, Double.valueOf(0.1)),
//		SLIDESHOW_COLOR_VALUE("slideshowcolorvalue", Type.DOUBLE, Double.valueOf(10)),
//		SPACE_VOLUME("spacevolume", Type.DOUBLE, Double.valueOf(1)),
//		
//		IS_COMMAND_LINE("iscommandline", Type.BOOLEAN, Boolean.valueOf(false)),
//		SHOW_GRAPHICS("showgraphics", Type.BOOLEAN, Boolean.valueOf(true)),
//		ALLOW_CELL_OVERLAP("allowcelloverlap", Type.BOOLEAN, Boolean.valueOf(false)),
//		SHOW_CYCLE_TIME("showcycletime", Type.BOOLEAN, Boolean.valueOf(true)),
//		SHOW_CYCLE_COUNT("showcyclecount", Type.BOOLEAN, Boolean.valueOf(true)),
//		PAUSE("pause", Type.BOOLEAN, Boolean.valueOf(true)),
//		String name;
//		Type type;
//		Object value;
//		Params(String name, Type type, Object value)
//		{
//			this.name = name;
//			this.type = type;
//			this.value = value;
//		}
//	}
	
	private double timeStep = 1,				// hours
				   deathRate = 0.1,				// percent per time point
				   maxSpaceBiomass = 10,		// grams
				   minSpaceBiomass = 1e-10,		// grams
				   spaceWidth = 0.1,			// cm
				   slideshowColorValue = 10,	
				   spaceVolume = 1;				// ml
	
	private boolean isCommandLine = false,
					showGraphics = true,
					allowCellOverlap = false,
					toroidalWorld = false,
					showCycleTime = true,
					showCycleCount = true,
					pause = true,
					pauseOnStep = true,
					saveSlideshow = false,
					showText = true,
					colorRelative = true,
					slideshowColorRelative = true;
	
	private int displayLayer = 0,
				pixelScale = 4,
				gridRows = 100,
				gridCols = 100,
				gridLayers = 1,
				maxCycles = UNLIMITED_CYCLES,
				diffusionsPerStep = 1,			//not used
				mediaRespawnRate = 1,			//not used (CHECK)
				slideshowRate = 1,
				slideshowLayer = 0,
				barrierColor = 0xff7D7D7D,
				backgroundColor = 0xff000000;
	
	private String slideshowExt = "png",
				   slideshowName = "slideshow",
				   lastDirectory = ".";

	private Map<String, Object> paramValues;
	private Map<String, ParameterType> paramTypes;

	// bad form or not, I kind of like how this works.
	/**
	 * Loads a new CometsParameters with default values.
	 */
	public CometsParameters() 
	{ 
		paramValues = new HashMap<String, Object>();
		paramTypes = new HashMap<String, ParameterType>();

		saveParameterState();
	}



	/**
	 * Loads a new CometsParameters with values set by the file in 
	 * <code>filename</code>. Any parameters not set by the file are set to their
	 * default values.
	 * <p>
	 * If the file doesn't exist or is incorrectly formatted, an IOException is thrown.
	 * @param filename
	 */
//	public CometsParameters(String filename) throws IOException
//	{
//		this();
//		
//		// load parameters file.
//		int lineCount = 0;
//		BufferedReader reader = new BufferedReader(new FileReader(new File(filename)));
//		String line;
//		while ((line = reader.readLine()) != null)
//		{
//			lineCount++;
//			String[] splitLine = line.split("=");
//			if (splitLine.length != 2)
//				throw new IOException("Parameters file error on line " + lineCount);
//
//			setParameter(splitLine[0].trim().toLowerCase(), splitLine[1].trim());
//		}
//		reader.close();
//		
//		loadParameterState();
//	}
	
	public void loadParameterState()
	{
		setTimeStep(((Double)paramValues.get("timestep")).doubleValue());
		setDeathRate(((Double)paramValues.get("deathrate")).doubleValue());
		setMaxSpaceBiomass(((Double)paramValues.get("maxspacebiomass")).doubleValue());
		setMinSpaceBiomass(((Double)paramValues.get("minspacebiomass")).doubleValue());
		setSpaceWidth(((Double)paramValues.get("spacewidth")).doubleValue());
		setSpaceVolume(Math.pow(((Double)paramValues.get("spacewidth")), 3));

		setCommandLineOnly(((Boolean)paramValues.get("iscommandline")).booleanValue());
		showGraphics(((Boolean)paramValues.get("showgraphics")).booleanValue());
		allowCellOverlap(((Boolean)paramValues.get("allowcelloverlap")).booleanValue());
		setToroidalGrid(((Boolean)paramValues.get("toroidalworld")).booleanValue()); 
		showCycleTime(((Boolean)paramValues.get("showcycletime")).booleanValue());
		showCycleCount(((Boolean)paramValues.get("showcyclecount")).booleanValue());
		pause(((Boolean)paramValues.get("pause")).booleanValue());
		pauseOnStep(((Boolean)paramValues.get("pauseonstep")).booleanValue());
		saveSlideshow(((Boolean)paramValues.get("saveslideshow")).booleanValue());
		showText(((Boolean)paramValues.get("showtext")).booleanValue());
		setColorRelative(((Boolean)paramValues.get("colorrelative")).booleanValue());
		
		setPixelScale(((Integer)paramValues.get("pixelscale")).intValue());
		setNumRows(((Integer)paramValues.get("gridrows")).intValue());
		setNumCols(((Integer)paramValues.get("gridcols")).intValue());
		setNumLayers(((Integer)paramValues.get("gridlayers")).intValue());
		setMaxCycles(((Integer)paramValues.get("maxcycles")).intValue());
		setDiffusionsPerStep(((Integer)paramValues.get("diffusionsperstep")).intValue());
		setMediaRespawnRate(((Integer)paramValues.get("mediarespawnrate")).intValue());
		setSlideshowRate(((Integer)paramValues.get("slideshowrate")).intValue());
		setBarrierColor(((Integer)paramValues.get("barriercolor")).intValue());
		setBackgroundColor(((Integer)paramValues.get("backgroundcolor")).intValue());
		setDisplayLayer(((Integer)paramValues.get("displaylayer")).intValue());
		setSlideshowLayer(((Integer)paramValues.get("slideshowlayer")).intValue());		

		setSlideshowExt(((String)paramValues.get("slideshowext")));
		setSlideshowName(((String)paramValues.get("slideshowname")));
		setLastDirectory(((String)paramValues.get("lastdirectory")));

	}
	
	public void saveParameterState()
	{
		paramValues.put("timestep", new Double(timeStep));
		paramTypes.put("timestep", ParameterType.DOUBLE);
		
		paramValues.put("deathrate", new Double(deathRate));
		paramTypes.put("deathrate", ParameterType.DOUBLE);
		
		paramValues.put("maxspacebiomass", new Double(maxSpaceBiomass));
		paramTypes.put("maxspacebiomass", ParameterType.DOUBLE);
		
		paramValues.put("minspacebiomass", new Double(minSpaceBiomass));
		paramTypes.put("minspacebiomass", ParameterType.DOUBLE);
		
		paramValues.put("spacewidth", new Double(spaceWidth));
		paramTypes.put("spacewidth", ParameterType.DOUBLE);
				
		paramValues.put("iscommandline", new Boolean(isCommandLine));
		paramTypes.put("iscommandline", ParameterType.BOOLEAN);
		
		paramValues.put("showgraphics", new Boolean(showGraphics));
		paramTypes.put("showgraphics", ParameterType.BOOLEAN);
		
		paramValues.put("toroidalworld", new Boolean(toroidalWorld));
		paramTypes.put("toroidalworld", ParameterType.BOOLEAN);
		
		paramValues.put("allowcelloverlap", new Boolean(allowCellOverlap));
		paramTypes.put("allowcelloverlap", ParameterType.BOOLEAN);
		
		paramValues.put("showcycletime", new Boolean(showCycleTime));
		paramTypes.put("showcycletime", ParameterType.BOOLEAN);
		
		paramValues.put("showcyclecount", new Boolean(showCycleCount));
		paramTypes.put("showcyclecount", ParameterType.BOOLEAN);
	
		paramValues.put("pause", new Boolean(pause));
		paramTypes.put("pause", ParameterType.BOOLEAN);
		
		paramValues.put("pauseonstep", new Boolean(pauseOnStep));
		paramTypes.put("pauseonstep", ParameterType.BOOLEAN);
		
		paramValues.put("saveslideshow", new Boolean(saveSlideshow));
		paramTypes.put("saveslideshow", ParameterType.BOOLEAN);
		
		paramValues.put("showtext", new Boolean(showText));
		paramTypes.put("showtext", ParameterType.BOOLEAN);
		
		paramValues.put("pixelscale", new Integer(pixelScale));
		paramTypes.put("pixelscale", ParameterType.INT);
		
		paramValues.put("gridrows", new Integer(gridRows));
		paramTypes.put("gridrows", ParameterType.INT);
		
		paramValues.put("gridcols", new Integer(gridCols));
		paramTypes.put("gridcols", ParameterType.INT);
		
		paramValues.put("gridlayers", new Integer(gridLayers));
		paramTypes.put("gridlayers", ParameterType.INT);
	
		paramValues.put("maxcycles", new Integer(maxCycles));
		paramTypes.put("maxcycles", ParameterType.INT);
		
		paramValues.put("diffusionsperstep", new Integer(diffusionsPerStep));
		paramTypes.put("diffusionsperstep", ParameterType.INT);
		
		paramValues.put("mediarespawnrate", new Integer(mediaRespawnRate));
		paramTypes.put("mediarespawnrate", ParameterType.INT);
		
		paramValues.put("slideshowrate", new Integer(slideshowRate));
		paramTypes.put("slideshowrate", ParameterType.INT);
		
		paramValues.put("barriercolor", new Integer(barrierColor));
		paramTypes.put("barriercolor", ParameterType.COLOR);
		
		paramValues.put("backgroundcolor", new Integer(backgroundColor));
		paramTypes.put("backgroundcolor", ParameterType.COLOR);
				
		paramValues.put("slideshowext", slideshowExt);
		paramTypes.put("slideshowext", ParameterType.STRING);
		
		paramValues.put("slideshowname", slideshowName);
		paramTypes.put("slideshowname", ParameterType.STRING);
		
		paramValues.put("colorrelative", new Boolean(colorRelative));
		paramTypes.put("colorrelative", ParameterType.BOOLEAN);
		
		paramValues.put("displaylayer", new Integer(displayLayer));
		paramTypes.put("displaylayer", ParameterType.INT);
		
		paramValues.put("slideshowlayer", new Integer(slideshowLayer));
		paramTypes.put("slideshowlayer", ParameterType.INT);
		
		paramValues.put("slideshowcolorvalue", new Double(slideshowColorValue));
		paramTypes.put("slideshowcolorvalue", ParameterType.DOUBLE);
		
		paramValues.put("slideshowcolorrelative", new Boolean(slideshowColorRelative));
		paramTypes.put("slideshowcolorrelative", ParameterType.BOOLEAN);
		
		paramValues.put("lastdirectory", new String(lastDirectory));
		paramTypes.put("lastdirectory", ParameterType.STRING);
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
			case COLOR :
				// broken. don't wanna fix it right now. TODO
//				try
//				{
//					int newValue = Integer.parseInt(v, 32);
//					paramValues.put(p, new Integer(newValue));
//				}
//				catch (NumberFormatException e)
//				{
//					return ParameterState.WRONG_TYPE;
//				}
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
			case STRING :
				paramValues.put(p, v);
				break;
			default :
				break;
		}
		return ParameterState.OK;
	}
	
//	private void setParameter_OLD(String p, String v) throws IOException
//	{
//		Object value = paramValues.get(p);
//		if (value == null)
//			throw new IOException("Unknown parameter '" + p + "'");
//		String dataType = paramTypes.get(p);
//		if (dataType.equals("int"))
//		{
//			// int!
//			try
//			{
//				int newValue = Integer.parseInt(v);
//				paramValues.put(p, new Integer(newValue));
//			}
//			catch (NumberFormatException e)
//			{
//				throw new IOException("Parameter '" + p + "' must be an integer");
//			}
//		}
//		else if (dataType.equals("color"))
//		{
//			try
//			{
//				int newValue = Integer.parseInt(v, 32);
//				paramValues.put(p, new Integer(newValue));
//			}
//			catch (NumberFormatException e)
//			{
//				throw new IOException("Parameter '" + p + "' must be in the hexadecimal color format");
//			}
//		}
//		else if (dataType.equals("double"))
//		{
//			// double!
//			try
//			{
//				double newValue = Double.parseDouble(v);
//				paramValues.put(p, new Double(newValue));
//			}
//			catch (NumberFormatException e)
//			{
//				throw new IOException("Parameter '" + p + "' must be a double");
//			}
//		}
//		else if (dataType.equals("boolean"))
//		{
//			// boolean!
//			if (!v.equalsIgnoreCase("false") && !v.equalsIgnoreCase("true"))
//				throw new IOException("Parameter '" + p + "' must be a boolean value (either 'true' or 'false')");
//			paramValues.put(p, new Boolean(Boolean.parseBoolean(v)));
//		}
//		else if (dataType.equals("string"))
//		{
//			// string!
//			paramValues.put(p, v);
//		}
//	}

//	public void setDefaultDiffusionConstant(double d)
//	{
//		if (d >= 0) // zero means that no diffusion occurs.
//		{
//			defaultDiffConst = d;
//		}
//	}
//	
//	public double getDefaultDiffusionConstant()
//	{
//		return defaultDiffConst;
//	}
	
	public void setSpaceVolume(double d)
	{
		if (d > 0)
			spaceVolume = d;
	}
	
	public double getSpaceVolume()
	{
		return spaceVolume;
	}
	
	public void setLastDirectory(String dir)
	{
		lastDirectory = dir;
	}
	
	public String getLastDirectory()
	{
		return lastDirectory;
	}
	
	public boolean getColorRelative()
	{
		return colorRelative;
	}
	
	public void setColorRelative(boolean b)
	{
		colorRelative = b;
	}
	
	public int getDisplayLayer()
	{
		return displayLayer;
	}
	
	/**
	 * The user should do their own upper-bound error checking here, since CometsParameters
	 * doesn't know how many layers there should be.  
	 * @param layer
	 */
	public void setDisplayLayer(int layer)
	{
		if (layer < 0)
			layer = 0;
//		System.out.println("setting display layer to " + layer);
		displayLayer = layer;
	}
	
	/**
	 * @return the width of each grid space. Since we're modeling square
	 * spaces, this is also the height. Future versions may model non-square
	 * grids, but that's not yet
	 */
	public double getSpaceWidth()
	{
		return spaceWidth;
	}
	
	/**
	 * Sets the width of each space in the grid. We're modeling grids of 
	 * square spaces, so this is also the height of each space. For some applications,
	 * spaces are modelled volumetrically as well - this assumes that each space is
	 * a cube, and the volume is calculated accordingly.
	 * @param width the width of each space in the grid
	 */
	public void setSpaceWidth(double width)
	{
		if (width > 0)
		{
			spaceWidth = width;
			setSpaceVolume(Math.pow(width, 3));
		}
	}
	
	/**
	 * @return the maximum allowed biomass in any given grid space. Overflow
	 * should be handled by the <code>World2D</code> or <code>Cell</code> classes.
	 */
	public double getMaxSpaceBiomass() 
	{ 
		return maxSpaceBiomass; 
	}
	
	/**
	 * Sets the maximum allowed biomass concentration in any given space. This value must
	 * be greater than zero and greater than the currently allowed minimum value. If 
	 * either of these rules are broken, nothing is changed.
	 * 
	 * @param max the maximum biomass concentration 
	 * @see getMinSpaceBiomassConcentration()
	 */
	public void setMaxSpaceBiomass(double max)
	{
		if (max >= 0 && max > minSpaceBiomass)
			maxSpaceBiomass = max;
	}

	public double getMinSpaceBiomass()
	{
		return minSpaceBiomass;
	}
	
	/**
	 * Sets the minimum allowed biomass concentration in any given space. This acts as a 
	 * lower threshold for biomass - any space with less than this concentration is assumed
	 * to be empty, and is cleared out. This is mainly in here to avoid any numerical 
	 * instability issues.
	 * 
	 * This value must be greater than zero and less than the currently allowed maximum 
	 * value. If either of these rules are broken, nothing is changed.
	 * 
	 * @param min the new minimum biomass concentration 
	 * @see getMaxSpaceBiomassConcentration()
	 */
	public void setMinSpaceBiomass(double min)
	{
		if (min >= 0 && min < maxSpaceBiomass)
			minSpaceBiomass = min;
	}
	
	/**
	 * Fetches the current background color of the world as a packed 32 bit integer. Ignoring
	 * the alpha channel, the red channel is the second pair of bytes, the green is the
	 * third pair, and the blue is the last pair.
	 * <p>
	 * These can be unraveled by doing something like:
	 * <br>
	 * <code>r = bgcolor | bgcolor >> 16<br>
	 * g = bgcolor >> 8 & 0xFF;<br>
	 * b = bgcolor & 0xFF;<br>
	 * </code>
	 * This returns an int value used in the Processing implementation of the
	 * <code>CometsRunPanel</code>.
	 * @return an integer used for representing a color
	 */
	public int getBackgroundColor() 
	{ 
		return backgroundColor; 
	}
	
	/**
	 * Sets the background color to be a combination of three rgb channels. Each
	 * value is expected to be in the range of 0-255, and will be adjusted up or down
	 * as needed
	 * @param r the value of the red channel
	 * @param g the value of the green channel
	 * @param b the value of the blue channel
	 */
	public void setBackgroundColor(int r, int g, int b)
	{
		backgroundColor = Utility.pColor(r, g, b);
	}

	/**
	 * Sets the background color to the three rgb channels packaged as a 32 bit integer.
	 * This is intended for use with the Processing package, so the first 2 bytes are the 
	 * alpha channel (not used for background color), followed by the red, green, and blue
	 * channels, e.g. <code> rgb = 0x00000000 | r << 16 | g << 8 | b; </code> 
	 * @param rgb the integer value of the three rgb color channels
	 */
	public void setBackgroundColor(int rgb)
	{
		backgroundColor = rgb;
	}
	
	/**
	 * Sets the background color to a standard Java <code>Color</code>. 
	 * @param c the color to set
	 */
	public void setBackgroundColor(Color c)
	{
		setBackgroundColor(c.getRGB());
	}
	
	/**
	 * Returns the barrier color as a packaged 32 bit integer.
	 * @see #getBackgroundColor()
	 * @return an integer used for representing the barrier color
	 */
	public int getBarrierColor() 
	{ 
		return barrierColor; 
	}
	
	/**
	 * Sets the barrier color using red, blue, and green channels, with values expected
	 * to be in the range of 0-255
	 * @see #setBackgroundColor(int, int, int)
	 * @param r the value of the red channel
	 * @param g the value of the green channel
	 * @param b the value of the blue channel
	 */
	public void setBarrierColor(int r, int g, int b)
	{
		barrierColor = Utility.pColor(r, g, b);
	}
	
	/**
	 * Sets the barrier color to the three rgb channels packed into a 32-bit integer.
	 * @see #setBackgroundColor(int)
	 * @param rgb the integer value of the three packaged channels
	 */
	public void setBarrierColor(int rgb)
	{
		barrierColor = rgb;
	}
	
	/**
	 * Sets the barrier color to a standard Java <code>Color</code> object.
	 * @param c the color to use for barriers
	 */
	public void setBarrierColor(Color c)
	{
		setBarrierColor(c.getRGB());
	}
	
	/**
	 * @return the death rate for all modeled cell types, as a decimal value between 0 and 1.
	 * @see #setDeathRate(double)
	 */
	public double getDeathRate() 
	{ 
		return deathRate; 
	}

	/**
	 * Sets the death rate for all modeled cell types, to be applied on each cycle. For example, 
	 * if the death rate is 0.1, then 10% of the biomass will "die" (be removed) on each cycle.
	 * This is expected to be between 0 and 1, and will be either increased or decreased to 
	 * fit that range.
	 * @param rate a decimal value between 0 and 1
	 */
	public void setDeathRate(double rate)
	{
		if (rate < 0)
			rate = 0;
		else if (rate > 1)
			rate = 1;
		deathRate = rate;
	}
	
	/**
	 * Returns the root name of the slideshow series. 
	 * @see #setSlideshowName(String) 
	 * @see #saveSlideshow(boolean)
	 * @return a <code>String</code> for the name of a slideshow
	 */
	public String getSlideshowName() 
	{ 
		return slideshowName; 
	}

	/**
	 * Sets the root name of the slideshow that comes from running a Comets simulation.
	 * This string is the prefix of every image file made, followed by a number for each
	 * frame of the slideshow
	 * @param name a <code>String</code> representing the root name of the slideshow
	 */
	public void setSlideshowName(String name) 
	{ 
		slideshowName = name; 
	}
	
	/**
	 * Returns the extension (image type) of the slideshow.
	 * @return a <code>String</code> containing the extension name of each slideshow image file. 
	 */
	public String getSlideshowExt() 
	{ 
		return slideshowExt; 
	}
	
	/**
	 * Sets the extension (e.g. image type) of each slideshow image file to be saved.
	 * If it is an invalid format, this will automatically reset to saving .png files.
	 * @see #saveSlideshow(boolean)
	 * @param ext an image file extension.
	 */
	public void setSlideshowExt(String ext)
	{
		//TODO: code to make sure it's a correct filetype.
		slideshowExt = ext;
	}
	
	/**
	 * Returns the rate (in number of frames) at which a slideshow frame is captured
	 * using the given name and image format.
	 * @see #setSlideshowName(String)
	 * @see #setSlideshowExt(String)
	 * @see #saveSlideshow(boolean)
	 * @return an integer rate for saving the slideshow
	 */
	public int getSlideshowRate() 
	{ 
		return slideshowRate; 
	}
	
	/**
	 * Sets the rate (in number of frames) at which a slideshow frame is captured
	 * using the given name and image format. This value is expected to be at least
	 * 1 - if less than one, it will be adjusted up to 1.
	 * @see #setSlideshowName(String)
	 * @see #setSlideshowExt(String)
	 * @see #saveSlideshow(boolean)
	 * @param rate an integer rate for saving the slideshow
	 */
	public void setSlideshowRate(int rate)
	{
		if (rate < 1)
			rate = 1;
		slideshowRate = rate;
	}

	public void setSlideshowColorRelative(boolean b)
	{
		slideshowColorRelative = b;
	}
	public boolean getSlideshowColorRelative()
	{
		return slideshowColorRelative;
	}

	public void setSlideshowLayer(int layer)
	{
		slideshowLayer = layer;
	}
	
	public int getSlideshowLayer()
	{
		return slideshowLayer;
	}

	public void setSlideshowColorValue(double val)
	{
		if (slideshowColorValue > 0)
			slideshowColorValue = val;
	}
	public double getSlideshowColorValue()
	{
		return slideshowColorValue;
	}
	
	
	/**
	 * Returns the rate at which the media is respawned as number of cycles. This value
	 * can also be <code>CometsConstants.NEVER_RESPAWN</code>.
	 * @return either a positive integer or <code>CometsConstants.NEVER_RESPAWN</code>
	 */
	public int getMediaRespawnRate() 
	{
		return mediaRespawnRate; 
	}

	/**
	 * Sets the rate at which media respawns in the world. This value can either be
	 * <code>CometsContants.NEVER_RESPAWN</code> or a positive integer. If the rate is a 
	 * positive number, then it is used to count the number of cycles between media respawns 
	 * in the world. For example, if rate = 1, then media is reloaded after every cycle, if 
	 * rate = 2, then it's done every other cycle, and so on.
	 * @param rate
	 */
	public void setMediaRespawnRate(int rate)
	{
		if (rate != NEVER_RESPAWN && rate <= 0)
		{
			rate = 1;
		}
		mediaRespawnRate = rate;
	}

	/**
	 * Returns the number of diffusion steps performed on each simulation cycle.
	 * @see #setDiffusionsPerStep(int)
	 * @return a positive integer
	 */
	public int getDiffusionsPerStep()
	{
		return diffusionsPerStep; 
	}

	/**	
	 * Sets the number of diffusion steps to be performed on each simulation cycle.
	 * This number has a lower limit of 1, and will be set to 1 if the passed value is less.
	 * <p>
	 * Each diffusion step is performed as explained elsewhere (TODO - add this, eh?). Multiple
	 * diffusion steps just push the diffusion closer to equilibrium at a single step.
	 * <p>
	 * Note that differs from the diffusion <i>rate</i>.
	 * @param count
	 */
	public void setDiffusionsPerStep(int count)
	{
		if (count < 1)
			count = 1;
		diffusionsPerStep = count;
	}
	
//	/**
//	 * Returns the diffusion rate, a decimal value between 0 and 1. This value is used to scale
//	 * what concentration diffuses from each space on every cycle. Note that this is different
//	 * from the diffusion <i>step</i>
//	 * @return
//	 */
//	public double getDiffusionRate()
//	{
//		return diffusionRate; 
//	}
//	
//	/**
//	 * Sets the diffusion rate - a decimal value between 0 and 1. Any values outside this range
//	 * will be adjusted to fit.
//	 * @param rate the rate of diffusion to and from each space on each cycle.
//	 */
//	public void setDiffusionRate(double rate)
//	{
//		if (rate < 0)
//			rate = 0;
//		else if (rate > 1)
//			rate = 1;
//		diffusionRate = rate;
//	}

	/**
	 * @return either the maximum number of cycles for the simulation or <code>CometsConstants.UNLIMITED_CYCLES</code>.
	 */
	public int getMaxCycles() 
	{
		return maxCycles; 
	}
	
	/**
	 * Sets the maximum number of cycles for the simulation. This value must be >= 1, or 
	 * <code>CometsConstants.UNLIMITED_CYCLES</code>. If this is violated, the max number of
	 * cycles will be automatically set to 1.
	 * @param max either an integer >= 1 or CometsConstants.UNLIMITED_CYCLES.
	 */
	public void setMaxCycles(int max)
	{
		if (max != UNLIMITED_CYCLES && max < 1)
		{
			max = 1;
		}
		maxCycles = max;
	}

	/**
	 * A flag for whether or not to show text on the <code>CometsRunPanel</code> when
	 * different events occur.
	 * @return true if text is to be shown on various events.
	 */
	public boolean showText() 
	{ 
		return showText; 
	}

	/**
	 * Sets a boolean flag for showing text on the <code>CometsRunPanel</code> when various
	 * events occur. These include pausing/unpausing the simulation, toggling through viewing
	 * different metabolites or biomass, and adjusting the color scheme. 
	 * @param b
	 */
	public void showText(boolean b) { showText = b; }
	


	/**
	 * @return true if a slideshow will be saved.
	 * @see #saveSlideshow(boolean) 
	 */
	public boolean saveSlideshow() { return saveSlideshow; }
	
	/**
	 * Tells COMETS whether or not to save a slideshow. A slideshow is a series of images
	 * representing the changes that occur to biomass over the course of a simulation. If this
	 * is set to true, it is best for the user to set both the root name of the slideshow
	 * and the image type.
	 * <p>
	 * Image files will be named with the root name first, followed by the number of the 
	 * screenshot taken, then the extension.
	 * @see #setSlideshowName(String)
	 * @see #setSlideshowExt(String)
	 * @see #setSlideshowRate(int)
	 * @param b if true, a slideshow will be saved.
	 */
	public void saveSlideshow(boolean b) { saveSlideshow = b; }

	/**
	 * @return true if the simulation pauses after each cycle.
	 */
	public boolean pauseOnStep() { return pauseOnStep; }
	
	/**
	 * A flag for pausing the simulation after each cycle
	 * @param b if true, the simulation will pause after each cycle.
	 */
	public void pauseOnStep(boolean b) { pauseOnStep = b; }
	
	/**
	 * @return true if the simulation is currently paused.
	 */
	public boolean isPaused() { return pause; }
	
	/**
	 * Tells COMETS to pause (or unpause) the currently running simulation.
	 * @param b
	 */
	public void pause(boolean b) { pause = b; }
	
	/**
	 * @return true if the simulation outputs the number of each cycle to the output
	 * panel at the end of each cycle.
	 */
	public boolean showCycleCount() { return showCycleCount; }
	
	/**
	 * If this flag is set to true, then after each cycle is complete, the number of the
	 * cycle completed is output to the console (a panel in the <code>COMETS</code> GUI
	 * or <code>STDOUT</code> if running the command line version)
	 * @param b
	 */
	public void showCycleCount(boolean b) { showCycleCount = b; }
	
	/**
	 * @return true if the simulation should output how long a simulation cycle took to 
	 * complete, in seconds.
	 */
	public boolean showCycleTime() { return showCycleTime; }
	
	/**
	 * If this flag is set to true, then after each cycle is complete, the time required
	 * to finish the cycle is outputted to the console (a panel in the <code>COMETS</code>
	 * GUI or <code>STDOUT</code> in the command line version).
	 * @param b
	 */
	public void showCycleTime(boolean b) { showCycleTime = b; }
	
	/**
	 * @return true if the world is a torus - if moving off of one side just rotates back
	 * around to the other side
	 */
	public boolean isToroidalGrid() { return toroidalWorld; }

	/**
	 * This flag sets the world to either be a torus (moving one square off the top or side moves
	 * to a similar square on the opposite side of the world) or bounded on each side.
	 * @param b if true, the world is treated as a torus
	 */
	public void setToroidalGrid(boolean b) { toroidalWorld = b; }
	
	/**
	 * @return the number of grid rows
	 */
	public int getNumRows() { return gridRows; }
	
	/**
	 * @return the number of grid columns
	 */
	public int getNumCols() { return gridCols; }
	
	/**
	 * @return the number of grid layers (z-coord in 3D mode)
	 */
	public int getNumLayers() { return gridLayers; }
	
	/**
	 * Sets the number of rows in the grid. This must be greater than 0 - if the value passed is
	 * 0 or negative, it will be adjusted up to 1. 
	 * @param numRows the number of rows.
	 */
	public void setNumRows(int numRows) 
	{
		if (numRows <= 0) numRows = 1;
		gridRows = numRows; 
	}
	
	/**
	 * Sets the number of columns in the grid. This must be greater than 0 - if the value 
	 * passed is 0 or negative, it will be adjusted to 1.
	 * @param numCols the number of columns
	 */
	public void setNumCols(int numCols)
	{
		if (numCols <= 0) numCols = 1;
		gridCols = numCols;
	}
	
	/**
	 * Sets the number of layers (z-coordinate) in the grid. This must be greater
	 * than 0 - if the value passed is 0 or negative, it will be adjusted to 1.
	 * @param numLayers the number of grid layers
	 */
	public void setNumLayers(int numLayers)
	{
		if (numLayers <= 0) numLayers = 1;
		gridLayers = numLayers;
	}
	
	/**
	 * Sets the number of rows and columns in the grid. If either of these are less than 1, they
	 * will be bumped up to 1.
	 * @param numRows the number of rows in the grid
	 * @param numCols the number of columns in the grid
	 */
	public void setGridSize(int numRows, int numCols)
	{
		setNumRows(numRows);
		setNumCols(numCols);
	}
	
	/**
	 * @return the number of pixels on each side of a single grid space
	 */
	public int getPixelScale() 
	{
		return pixelScale;	
	}
	
	/**
	 * Sets the scale of each grid space in pixels. This is expected to be at least 1, and
	 * will be adjusted up to 1 if less.
	 * @param pixelScale
	 */
	public void setPixelScale(int pixelScale) 
	{	
		if (pixelScale < 1)
			pixelScale = 1;
		this.pixelScale = pixelScale; 
	}

	/**
	 * Sets the time step of each simulation in arbitrary time units. That is, the time units
	 * are dependent on whatever calculation is going on in the <code>Model</code> being used.
	 * For example, <code>FBAModel</code>s use hours. This also applies to each diffusion across
	 * the world.
	 * <p>
	 * This should be a value between 0 and 1.
	 * @param timeStep
	 */
	public void setTimeStep(double timeStep) 
	{ 
		this.timeStep = timeStep; 
	}
	
	/**
	 * @return the time step of each simulation in arbitrary time units.
	 * @see #setTimeStep(double)
	 */
	public double getTimeStep() 
	{ 
		return timeStep; 
	}

	/**
	 * @return true if graphics are shown in the <code>CometsRunPanel</code>
	 */
	public boolean showGraphics() { return showGraphics; }

	/**
	 * Sets a flag for updating graphics in the <code>CometsRunPanel</code>.
	 * @param b if true, graphics will be updated.
	 */
	public void showGraphics(boolean b) { showGraphics = b; }

	/**
	 * @return true if multiple cell types (e.g. biomass from different <code>Models</code>)
	 * are allowed to occupy the same space.
	 */
	public boolean allowCellOverlap() { return allowCellOverlap; }

	/**
	 * Sets a flag to either allow (or disallow) biomass from multiple <code>Models</code>
	 * to share the same space.
	 * @param b if true, different species can overlap on the same space.
	 */
	public void allowCellOverlap(boolean b) { allowCellOverlap = b; }

//	/**
//	 * @return the number of active <code>Model</code> simulation threads running
//	 * concurrently.
//	 */
//	public int getNumRunThreads() { return numRunThreads; }
//
//	/**
//	 * Sets the number of simulation threads that can run concurrently. 
//	 * @param n the number of threads. If this value is less than 1, it will be set to 1.
//	 */
//	public void setNumRunThreads(int n)
//	{
//		if (n < 1)
//			n = 1;
//		numRunThreads = n;
//	}
	
	/**
	 * @return true if this is the command-line-only version of COMETS
	 */
	public boolean isCommandLineOnly()
	{
		return isCommandLine;
	}

	/**
	 * Set COMETS to run only as a command line, or to include a GUI. See the COMETS
	 * documentation for the benefits of each.
	 * @param b if true, then only a command line is running
	 */
	public void setCommandLineOnly(boolean b)
	{
		isCommandLine = b;
	}
	
	public String toString()
	{
		String s = "";
		s += "timeStep = " + timeStep + "\n";
//		s += "diffusionRate = " + diffusionRate + "\n";
		s += "deathRate = " + deathRate + "\n";
		s += "maxSpaceBiomassConc = " + maxSpaceBiomass + "\n";
		s += "spaceWidth = " + spaceWidth + "\n";
//		s += "numRunThreads = " + numRunThreads + "\n";

		s += "isCommandLine = " + isCommandLine + "\n";
		s += "showGraphics = " + showGraphics + "\n";
		s += "allowCellOverlap = " + allowCellOverlap + "\n";
		s += "toroidalWorld = " + toroidalWorld + "\n";
		s += "showCycleTime = " + showCycleTime + "\n";
		s += "showCycleCount = " + showCycleCount + "\n";
		s += "pause = " + pause + "\n";
		s += "pauseOnStep = " + pauseOnStep + "\n";

		s += "saveSlideshow = " + saveSlideshow + "\n";
		s += "slideshowRate = " + slideshowRate + "\n";
		s += "slideshowName = " + slideshowName + "\n";
		s += "slideshowExt = " + slideshowExt + "\n";

		s += "showText = " + showText + "\n";
		
		s += "pixelScale = " + pixelScale + "\n"; 
		s += "gridRows = " + gridRows + "\n";
		s += "gridCols = " + gridCols + "\n";
		s += "maxCycles = " + maxCycles + "\n";

		s += "diffusionsPerStep = " + diffusionsPerStep + "\n";
		s += "mediaRespawnRate = " + mediaRespawnRate + "\n";
		
		s += "barrierColor = " + barrierColor + "\n";
		s += "backgroundColor = " + backgroundColor + "\n";

		return s;
	}

	public boolean hasParameter(String param)
	{
		return paramTypes.containsKey(param.toLowerCase());
	}
	
	public void dumpToFile(PrintWriter writer)
	{
		saveParameterState();
		for (String name : paramValues.keySet())
		{
			writer.println(name + " = " + paramValues.get(name).toString());
		}
	}

	public ParameterType getType(String name)
	{
		return paramTypes.get(name);
	}
}