/**
 * 
 */
package edu.bu.segrelab.comets;

import java.io.IOException;
import java.util.List;

/**
 * The <code>CometsLoader</code> is the interface between the <code>Comets</code>
 * class and any packages that do the hard work of simulations (e.g., those classes in the
 * <code>edu.bu.segrelab.comets.fba</code> package).
 * <p>
 * Internally, implementations of this should handle making sure the members of the package
 * are loaded.
 * <p>
 * Externally, this class is used to load a layout file, model file(s), and all the various
 * trappings of a simulation, and hold those for the <code>Comets</code> main class.
 * <p>
 * For example, code might look like this:
 * <br/>
 * <code>
 * Comets c = new Comets();<br/>
 * CometsLoader loader = new CometsLoader();<br/>
 * int r = loader.loadLayoutFile("layout.txt", c);<br/>
 * World2D world = loader.getWorld();<br/>
 * Model[] modelArr = loader.getModels();<br/>
 * ArrayList cellList = loader.getCells();<br/>
 * .<br/>
 * .<br/>
 * .<br/>
 * </code>
 * <br/>
 * and so on.
 * @author Bill Riehl
 *
 */
public interface CometsLoader
{
	public static final int USE_ORIGINAL_FILE = 0;
	public static final int DO_NOT_SAVE = 1;
	public static final int SAVE_NEW_FILE = 2;
	
	/**
	 * Loads a layout file and stores the results internally, according to the 
	 * file constraints set up by the extending package.
	 * @param filename a <code>String</code> for the file to be loaded.
	 * @param c the <code>Comets</code> that will be linked to this package
	 * @param gui if <code>true</code> allow gui widgets to be used for prompting
	 * the user, etc.
	 * @return <code>CometsConstants.PARAMS_OK</code> if the loading worked,
	 * <code>CometsConstants.PARAMS_ERROR</code> if there was an error,
	 * <code>CometsConstants.LOAD_CANCELED</code> if the loading was canceled,
	 * or any other appropriate integer value. 
	 */
	public int loadLayoutFile(String filename, Comets c, boolean gui) throws IOException;
	
	/**
	 * Retrieves the array of <code>Models</code> loaded in loadLayoutFile().
	 * @return an array of <code>Models</code>
	 */
	public Model[] getModels();
	
	/**
	 * Fetches an <code>ArrayList</code> of loaded cells.
	 * @return an <code>ArrayList</code> of <code>Cells</code>
	 */
	public List<Cell> getCells();

	/**
	 * Saves the state of the world in a new layout file, using the format given
	 * by the extending package.
	 * @param path the path and filename to be saved
	 * @param world the current <code>World2D</code> to be saved
	 * @param cellList the set of <code>Cells</code> in the world
	 * @param cParams the current set of parameters to be saved
	 */
	public void saveLayoutFile(String path, World2D world, Model[] models, List<Cell> cellList, 
							   int saveMediaType, CometsParameters cParams) 
							   throws IOException;
	
	public void saveLayoutFile(Comets c) throws IOException;
	
	/**
	 * Loads a single <code>Model</code> from a file, returning the <code>Model</code>
	 * without storing it anywhere else internally (e.g. <code>getModels()</code> won't
	 * return this loaded <code>Model</code>).
	 * @param path the path and filename to be loaded
	 * @return a <code>Model</code>
	 */
	public Model loadModelFromFile(Comets c, String path);
	
	/**
	 * Creates a new <code>World2D</code> to be used in the current <code>Comets</code>
	 * object, and filled with info from a loaded array of <code>Models</code>. This 
	 * <code>World</code> is not stored internally (e.g. <code>getWorld()</code> won't
	 * return this loaded <code>World2D</code>).
	 * @param c the <code>Comets</code> to be associated with this <code>World2D</code>
	 * @return a new <code>World2D</code>
	 */
	public World2D createNewWorld(Comets c, Model[] models);
	
	/**
	 * Returns (and creates, if necessary) a <code>PackageParameters</code> object,
	 * containing a set of parameters specific to the package this <code>CometsLoader</code>
	 * extension controls.
	 * <p>
	 * Other classes in the package should access the <code>PackageParameters</code> from
	 * <code>Comets</code>, i.e. <code>Comets.getPackageParameters()</code>.
	 * @param c the <code>Comets</code> to be associated with this Package
	 * @return a <code>PackageParameters</code> instance
	 */
	public PackageParameters getPackageParameters(Comets c);

	public PackageParameterBatch getParameterBatch(Comets c);
	
	/**
	 * Creates and returns a <code>PackageParameters</code> object from the given file.
	 * @param c the <code>Comets</code> to be associated with this Package
	 * @param filename the file from which to read parameters
	 * @return a <code>PackageParameters</code> instance
	 * @throws IOException
	 */
//	public PackageParameters getPackageParameters(Comets c, String filename) throws IOException;
}
