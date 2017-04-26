package edu.bu.segrelab.comets;

/**
 * A handy way to avoid having to write CometsConstants.XXXXX over and over throughout
 * the program - just have most of the classes implement this.
 * <p>
 * Most of these are situation- and class-specific. But they should be pretty straightforward
 * in where they ought to be used.
 * @author Bill Riehl briehl@bu.edu
 *
 */
public interface CometsConstants
{
	public static final boolean DEBUG = true;
	
	public static final int PARAMS_ERROR = 0;
	public static final int PARAMS_OK = 1;
	public static final int MODEL_NOT_INITIALIZED = 2;
	public static final int BOUNDS_ERROR = 3;
	
	public static final int UNLIMITED_CYCLES = -1;
	public static final int NEVER_RESPAWN = -2;

	public static final int SIMULATION_ENDED = 4;
	public static final int SIMULATION_CYCLE_OK = 5;
	public static final int SIMULATION_CYCLE_ERROR = 6;
	
	public static final int RANDOM_LAYOUT = 7;
	public static final int SQUARE_LAYOUT = 8;
	public static final int FILLED_LAYOUT = 9;
	public static final int RANDOM_RECTANGLE_LAYOUT = 16;
	public static final int FILLED_RECTANGLE_LAYOUT = 17;
	
	public static final int COLOR_RELATIVE = 10;
	public static final int COLOR_ABSOLUTE = 11;
	
	// No longer used, since solving Fick's diffusion
	public static final double PERCENT_DIFFUSE_APPROX = 0.2;
	
	public static final double X_SCALE_2D = PERCENT_DIFFUSE_APPROX * (0.8 / 4);
	public static final double Y_SCALE_2D = PERCENT_DIFFUSE_APPROX * (0.2 / 4);
	public static final double[][] DIFFUSION_SCALE_2D = {{Y_SCALE_2D, X_SCALE_2D, Y_SCALE_2D},
													     {X_SCALE_2D, -PERCENT_DIFFUSE_APPROX, X_SCALE_2D},
													     {Y_SCALE_2D, X_SCALE_2D, Y_SCALE_2D}};
	
	public static final double X_SCALE_3D = PERCENT_DIFFUSE_APPROX * (0.6 / 6);
	public static final double Y_SCALE_3D = PERCENT_DIFFUSE_APPROX * (0.3 / 12);
	public static final double Z_SCALE_3D = PERCENT_DIFFUSE_APPROX * (0.1 / 8); 
	
	public static final double[][][] DIFFUSION_SCALE_3D = {
		{
			{ Z_SCALE_3D, Y_SCALE_3D, Z_SCALE_3D },
			{ Y_SCALE_3D, X_SCALE_3D, Y_SCALE_3D },
			{ Z_SCALE_3D, Y_SCALE_3D, Z_SCALE_3D },
		},
		{
			{ Y_SCALE_3D,  X_SCALE_3D, Y_SCALE_3D },
			{ X_SCALE_3D, -1.0, X_SCALE_3D },
			{ Y_SCALE_3D,  X_SCALE_3D, Y_SCALE_3D },			
		},
		{
			{ Z_SCALE_3D, Y_SCALE_3D, Z_SCALE_3D },
			{ Y_SCALE_3D, X_SCALE_3D, Y_SCALE_3D },
			{ Z_SCALE_3D, Y_SCALE_3D, Z_SCALE_3D },			
		}
	};
	
	public static final int SETUP_MODE = 12;
	public static final int SIMULATION_MODE = 13;
	
	public static final int LOAD_CANCELED = 14;
	
	public static final int MATLAB_FORMAT = 15;
	
	public static final int MAX_UNDO_DEPTH = 2;
	
	public static final int DIMENSIONALITY_2D = 16;
	public static final int DIMENSIONALITY_3D = 17;
}
