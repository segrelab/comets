package edu.bu.segrelab.comets.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

import edu.bu.segrelab.comets.Comets;
import edu.bu.segrelab.comets.CometsConstants;

/**
 * A <code>JMenuBar</code> extension to handle the main menus in COMETS.
 * <p>
 * This was made into a separate class to keep the Comets base class uncluttered.
 * <p>
 * Also, this way, a simple call to <code>setMode</code> will enable/disable the 
 * right menus/menuItems.
 *
 * @author Bill Riehl briehl@bu.edu
 *
 */
@SuppressWarnings("serial")
public class CometsMenuBar extends JMenuBar implements CometsConstants
{
	private Comets c;
	private JMenu fileMenu,
				  editMenu,
				  layoutMenu,
				  simMenu,
				  helpMenu,
				  viewMenu;
	private JMenuItem loadLayoutFileItem,
					  loadLayoutDBItem,
					  saveLayoutFileItem,
					  saveLayoutFileAsItem,
//					  saveLayoutDBItem,
					  saveParametersItem,
					  loadParametersItem,
					  loadModelFileItem,
					  loadModelDBItem,
					  exitItem,
					  setSizeItem,
					  editParametersItem,
					  editModelParametersItem,
//					  editPackageParametersItem,
					  startSimItem,
					  endSimItem,
					  setExpansionItem,
					  saveBiomassItem,
					  resetSimItem,
					  takeSnapshotItem,
					  saveStateItem,
					  undoItem,
					  addMediaNoiseItem,
					  addBiomassNoiseItem,
					  redoItem;
	private JCheckBoxMenuItem pauseItem,
							  pauseCycleItem,
							  layoutToolbarItem,
							  viewSpaceInfoItem,
							  viewModelInfoItem;
	
	/**
	 * Initializes the menu bar to be used by a given <code>Comets</code> main class. 
	 * The startingMode parameter should be either <code>CometsConstants.SETUP_MODE</code>
	 * or <code>CometsConstants.SIMULATION_MODE</code>.
	 * @param c - the <code>Comets</code> object that will be making use of this menu bar.
	 * @param startingMode - the initial mode of this menu bar
	 */
	public CometsMenuBar(Comets c, int startingMode)
	{
		super();
		this.c = c;
		setupMenus();
		setMode(startingMode);
	}
	
	/**
	 * Initializes all menus
	 */
	private void setupMenus()
	{
/******************************** FILE MENU ***************************************/
		fileMenu = new JMenu("File");
		fileMenu.setMnemonic(KeyEvent.VK_F);
		add(fileMenu);

		/* Load layout menu item */
		loadLayoutFileItem = new JMenuItem("Load Layout From File...");
		loadLayoutFileItem.setMnemonic(KeyEvent.VK_L);
		loadLayoutFileItem.addActionListener(
				new ActionListener() {
					public void actionPerformed(ActionEvent e)
					{
						c.loadLayoutFile();
					}
				});
		fileMenu.add(loadLayoutFileItem);
		
		/* Load layout DB menu item */
		loadLayoutDBItem = new JMenuItem("Load Layout From Database...");
		loadLayoutDBItem.setMnemonic(KeyEvent.VK_D);
		loadLayoutDBItem.addActionListener(
				new ActionListener() {
					public void actionPerformed(ActionEvent e)
					{
						c.loadLayoutDB();
					}
				});
		fileMenu.add(loadLayoutDBItem);

		fileMenu.addSeparator();
		
		/* Save layout menu item */
		saveLayoutFileItem = new JMenuItem("Save Layout");
		saveLayoutFileItem.setMnemonic(KeyEvent.VK_S);
		saveLayoutFileItem.addActionListener(
				new ActionListener() {
					public void actionPerformed(ActionEvent e)
					{
						c.saveLayoutFile(false);
					}
				});
		fileMenu.add(saveLayoutFileItem);
		
		/* Save layout as... menu item */
		saveLayoutFileAsItem = new JMenuItem("Save Layout As...");
		saveLayoutFileAsItem.setMnemonic(KeyEvent.VK_A);
		saveLayoutFileAsItem.addActionListener(
				new ActionListener() {
					public void actionPerformed(ActionEvent e)
					{
						c.saveLayoutFile(true);
					}
				});
		fileMenu.add(saveLayoutFileAsItem);
		
		fileMenu.addSeparator();

		loadModelFileItem = new JMenuItem("Load Model From File...");
		loadModelFileItem.setMnemonic(KeyEvent.VK_M);
		loadModelFileItem.addActionListener(
				new ActionListener() {
					public void actionPerformed(ActionEvent e)
					{
						c.loadModelFile();
					}
				});
		fileMenu.add(loadModelFileItem);
		
		loadModelDBItem = new JMenuItem("Load Model From Database...");
		loadModelDBItem.setMnemonic(KeyEvent.VK_T);
		loadModelDBItem.addActionListener(
				new ActionListener() {
					public void actionPerformed(ActionEvent e)
					{
						c.loadModelDB();
					}
				});
		fileMenu.add(loadModelDBItem);
		
		fileMenu.addSeparator();
		
		loadParametersItem = new JMenuItem("Load Parameters File...");
		loadParametersItem.setMnemonic(KeyEvent.VK_P);
		loadParametersItem.addActionListener(
				new ActionListener() {
					public void actionPerformed(ActionEvent e)
					{
						c.loadParametersFile();
					}
				});
		fileMenu.add(loadParametersItem);

		saveParametersItem = new JMenuItem("Save Parameters File...");
		saveParametersItem.setMnemonic(KeyEvent.VK_R);
		saveParametersItem.addActionListener(
				new ActionListener() {
					public void actionPerformed(ActionEvent e)
					{
						c.saveParametersFile();
					}
				});
		fileMenu.add(saveParametersItem);
		
		fileMenu.addSeparator();
		
		setExpansionItem = new JMenuItem("Set COMETS Modeling Package...");
		setExpansionItem.addActionListener(
				new ActionListener() {
					public void actionPerformed(ActionEvent e)
					{
						c.setExpansionPackage();
					}
				});
		fileMenu.add(setExpansionItem);
		
		fileMenu.addSeparator();
		

		/* Exit program menu item */
		exitItem = new JMenuItem("Exit");
		exitItem.addActionListener(
				new ActionListener() {
					public void actionPerformed(ActionEvent e)
					{
						c.exitProgram();
					}
				});
		fileMenu.add(exitItem);

/********************************** EDIT MENU *******************************/
		editMenu = new JMenu("Edit");
		editMenu.setMnemonic(KeyEvent.VK_E);
		add(editMenu);
		
		undoItem = new JMenuItem("Undo");
		undoItem.setMnemonic(KeyEvent.VK_U);
		undoItem.addActionListener(
				new ActionListener() {
					public void actionPerformed(ActionEvent e)
					{
						c.undo();
					}
				});
		editMenu.add(undoItem);
		undoItem.setEnabled(false);
		
		redoItem = new JMenuItem("Redo");
		redoItem.setMnemonic(KeyEvent.VK_R);
		redoItem.addActionListener(
				new ActionListener() {
					public void actionPerformed(ActionEvent e)
					{
						c.redo();
					}
				});
		editMenu.add(redoItem);
		redoItem.setEnabled(false);
		
		editMenu.addSeparator();
		
		/* Set lots of parameters menu item */
		editParametersItem = new JMenuItem("Edit Parameters...");
		editParametersItem.setMnemonic(KeyEvent.VK_P);
		editParametersItem.addActionListener(
				new ActionListener() {
					public void actionPerformed(ActionEvent e)
					{
						c.editParametersDialog();
					}
				});
		editMenu.add(editParametersItem);
		
		editModelParametersItem = new JMenuItem("Edit Model Parameters...");
		editModelParametersItem.setMnemonic(KeyEvent.VK_M);
		editModelParametersItem.addActionListener(
				new ActionListener() {
					public void actionPerformed(ActionEvent e)
					{
						c.editModelParametersDialog();
					}
				});
		editMenu.add(editModelParametersItem);
		
/*********************************** VIEW MENU ****************************************/
		viewMenu = new JMenu("View");
		viewMenu.setMnemonic(KeyEvent.VK_V);
		add(viewMenu);
		
		viewModelInfoItem = new JCheckBoxMenuItem("Show Model Info");
		viewModelInfoItem.setMnemonic(KeyEvent.VK_M);
		viewModelInfoItem.addItemListener(
				new ItemListener() {
					public void itemStateChanged(ItemEvent e)
					{
						c.showModelInfoFrame(e.getStateChange() == ItemEvent.SELECTED);
					}
				});
		viewModelInfoItem.setState(false);
		viewMenu.add(viewModelInfoItem);
		
		
		viewSpaceInfoItem = new JCheckBoxMenuItem("Show Space Info");
		viewSpaceInfoItem.setMnemonic(KeyEvent.VK_S);
		viewSpaceInfoItem.addItemListener(
				new ItemListener() {
					public void itemStateChanged(ItemEvent e)
					{
						c.showSpaceInfoFrame(e.getStateChange() == ItemEvent.SELECTED);
					}
				});
		viewSpaceInfoItem.setState(false);
		viewMenu.add(viewSpaceInfoItem);
		
		
/********************************** LAYOUT MENU **************************************/
		layoutMenu = new JMenu("Layout");
		layoutMenu.setMnemonic(KeyEvent.VK_L);
		add(layoutMenu);
		
		/* Set world size menu item */
		setSizeItem = new JMenuItem("Set World Size...");
		setSizeItem.setMnemonic(KeyEvent.VK_S);
		setSizeItem.addActionListener(
				new ActionListener() {
					public void actionPerformed(ActionEvent e)
					{
						c.setWorldSizeDialog();
					}
				});
		layoutMenu.add(setSizeItem);
		
		/* Add media noise */
		
		addMediaNoiseItem = new JMenuItem("Add Media Noise...");
		addMediaNoiseItem.setMnemonic(KeyEvent.VK_M);
		addMediaNoiseItem.addActionListener(
				new ActionListener() {
					public void actionPerformed(ActionEvent e)
					{
						c.addMediaNoiseDialog();
					}
				});
		layoutMenu.add(addMediaNoiseItem);
		
		/* Add biomass noise */
		
		addBiomassNoiseItem = new JMenuItem("Add Biomass Noise...");
		addBiomassNoiseItem.setMnemonic(KeyEvent.VK_M);
		addBiomassNoiseItem.addActionListener(
				new ActionListener() {
					public void actionPerformed(ActionEvent e)
					{
						c.addBiomassNoiseDialog();
					}
				});
		layoutMenu.add(addBiomassNoiseItem);

		
		/* View layout toolbar */
		
		layoutToolbarItem = new JCheckBoxMenuItem("Show Layout Toolbar");
		layoutToolbarItem.setMnemonic(KeyEvent.VK_T);
		layoutToolbarItem.addItemListener(
				new ItemListener() {
					public void itemStateChanged(ItemEvent e)
					{
						c.showLayoutToolbar(e.getStateChange() == ItemEvent.SELECTED);
					}
				});
		layoutToolbarItem.setState(true);
		layoutMenu.add(layoutToolbarItem);
//		viewMenu.add(layoutToolbarItem);
		
/**************************************** SIMULATION MENU *********************************/
		simMenu = new JMenu("Simulation");
		simMenu.setMnemonic(KeyEvent.VK_S);
		add(simMenu);
		
		/* Start simulation menu option */
		
		startSimItem = new JMenuItem("Start Simulation");
		startSimItem.setMnemonic(KeyEvent.VK_S);
		startSimItem.addActionListener(
				new ActionListener() {
					public void actionPerformed(ActionEvent e)
					{
						c.startSimulation();
					}
				});
		simMenu.add(startSimItem);
		
		/* End simulation menu option */
		endSimItem = new JMenuItem("End Simulation");
		endSimItem.setMnemonic(KeyEvent.VK_E);
		endSimItem.addActionListener(
				new ActionListener() {
					public void actionPerformed(ActionEvent e)
					{
						c.endSimulation();
					}
				});
		simMenu.add(endSimItem);
		
		simMenu.addSeparator();
		
		/* add all the usual keystroke stuff */
		
		pauseCycleItem = new JCheckBoxMenuItem("Pause on Cycle");
		pauseCycleItem.setState(c.getParameters().pauseOnStep());
		pauseCycleItem.setMnemonic(KeyEvent.VK_C);
		pauseCycleItem.addItemListener(
				new ItemListener() {
					public void itemStateChanged(ItemEvent e)
					{
						c.getParameters().pauseOnStep(e.getStateChange() == ItemEvent.SELECTED);
					}
				});
		simMenu.add(pauseCycleItem);
		
		pauseItem = new JCheckBoxMenuItem("Pause Simulation");
		pauseItem.setState(c.getParameters().isPaused());
		pauseItem.setMnemonic(KeyEvent.VK_P);
		pauseItem.addItemListener(
				new ItemListener() {
					public void itemStateChanged(ItemEvent e)
					{
						c.getParameters().pause(e.getStateChange() == ItemEvent.SELECTED);
					}
				});
		simMenu.add(pauseItem);
		
		simMenu.addSeparator();
		
		saveBiomassItem = new JMenuItem("Save Current Biomass...");
		saveBiomassItem.setMnemonic(KeyEvent.VK_B);
		saveBiomassItem.addActionListener(
				new ActionListener() {
					public void actionPerformed(ActionEvent e)
					{
						c.saveBiomassSnapshot();
					}
				});
		simMenu.add(saveBiomassItem);
		
		takeSnapshotItem = new JMenuItem("Take Graphic Snapshot...");
		takeSnapshotItem.setMnemonic(KeyEvent.VK_S);
		takeSnapshotItem.addActionListener(
				new ActionListener() {
					public void actionPerformed(ActionEvent e)
					{
						c.takeUserScreenshot();
					}
				});
		simMenu.add(takeSnapshotItem);

		simMenu.addSeparator();

		saveStateItem = new JMenuItem("Save Simulation State");
		saveStateItem.addActionListener(
				new ActionListener() {
					public void actionPerformed(ActionEvent e)
					{
						c.saveSimulationState();
					}
				});
		simMenu.add(saveStateItem);
		
		resetSimItem = new JMenuItem("Load Simulation State");
		resetSimItem.addActionListener(
				new ActionListener() {
					public void actionPerformed(ActionEvent e)
					{
						c.resetSimulationToSavedState();
					}
				});
		simMenu.add(resetSimItem);

		/*
		 * show number of cells
		 * toggle color style
		 * toggle text display
		 * take picture
		 * pause (checkbox)
		 */
		
		helpMenu = new JMenu("Help");
		helpMenu.setMnemonic(KeyEvent.VK_H);
		add(helpMenu);
		
		JMenuItem aboutItem = new JMenuItem("About COMETS");
		aboutItem.setMnemonic(KeyEvent.VK_A);
		aboutItem.addActionListener(
				new ActionListener() {
					public void actionPerformed(ActionEvent e)
					{
						c.aboutDialog();
					}
				});
		helpMenu.add(aboutItem);
	}
	
	/**
	 * Switches mode between setup and simulation.
	 * Expects one of two parameters:
	 * CometsConstants.SETUP_MODE = turn on setup/layout mode menu options.
	 * CometsConstants.SIMULATION_MODE = turn on simulation mode menu options.
	 * 
	 * Options are enabled/disabled according to the mode.
	 * @param mode
	 */
	public void setMode(int mode)
	{
		switch(mode)
		{
			case SETUP_MODE :
				// turn on setup menu stuff
				layoutMenu.setEnabled(true);
				startSimItem.setEnabled(true);
				endSimItem.setEnabled(false);
				saveLayoutFileItem.setEnabled(true);
				saveLayoutFileAsItem.setEnabled(true);
				break;
			case SIMULATION_MODE :
				// turn on simulation menu stuff
				layoutMenu.setEnabled(false);
				startSimItem.setEnabled(false);
				endSimItem.setEnabled(true);
				canUndo(false);
				canRedo(false);				
				break;
			default :
				break;
		}
	}
	
	/**
	 * Sets the space info checkbox menu item to be either checked or unchecked.
	 * A little clunky, but this works out okay because the info frames are nonmodal.
	 * @param b
	 */
	public void setSpaceInfoActive(boolean b)
	{
		viewSpaceInfoItem.setSelected(b);
	}
	
	/**
	 * Sets the model info checkbox menu item to be either checked or unchecked.
	 * A little clunky, but this works out okay because the info frames are nonmodal.
	 * @param b
	 */
	public void setModelInfoActive(boolean b)
	{
		viewModelInfoItem.setSelected(b);
	}
	
	public void canUndo(boolean b)
	{
		undoItem.setEnabled(b);
	}
	
	public void canRedo(boolean b)
	{
		redoItem.setEnabled(b);
	}
}
