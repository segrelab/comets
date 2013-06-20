/**
 * created 3 Mar 2010
 */
package edu.bu.segrelab.comets.ui;

import com.jogamp.opengl.util.gl2.GLUT;
import javax.media.opengl.*;
import javax.media.opengl.awt.GLJPanel;

import edu.bu.segrelab.comets.Cell;
import edu.bu.segrelab.comets.Comets;
import edu.bu.segrelab.comets.CometsConstants;
import edu.bu.segrelab.comets.CometsParameters;
import edu.bu.segrelab.comets.StaticPoint;
import edu.bu.segrelab.comets.World2D;
import edu.bu.segrelab.comets.ui.tools.AbstractTool;
import edu.bu.segrelab.comets.util.Utility;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * The <code>CometsSetupPanel</code> has the role of letting the user initialize and
 * customize the contents of the world before running a simulation.
 * <p>
 * This also contains a separate <code>GraphicsSetupPanel</code> class that handles the
 * graphic display of what's being setup in the world. It pains me to say that much of that
 * code is copied from the <code>CometsRunPanel</code>. Eventually, I plan to merge the two,
 * once I work out some threading issues. Maybe if I put the graphical updating in its own 
 * thread? Mimic the PApplet thread system somehow?
 * <p>
 * For now, they're separate. That makes the code more functional than pretty, I'm afraid. 
 * Eric Roberts would have my liver if he knew what this former 106A student did.
 * <p>
 * Anyway.
 * <p>
 * The setup panel also builds and controls a <code>CometsToolbarPanel</code> that works
 * as a toolbar palette - similar to common paint programs. Most of the tools work by
 * interacting directly with the <code>GraphicSetupPanel</code>: any mouse command that is
 * done to the graphic panel is just passed along to the selected tool.
 * <p>
 * Right clicking on the <code>GraphicSetupPanel</code> will allow the user to set biomass
 * and media in either individual spaces, or in a set of selected spaces. This is done through
 * locally maintained <code>DataContentPanels</code> (separate from those used by the 
 * <code>CometsToolbarPanel</code> for tool interactions).
 * @author Bill Riehl briehl@bu.edu
 * 
 */
@SuppressWarnings("serial")
public class CometsSetupPanel extends JPanel implements CometsConstants
{
	/**
	 * For use with different tool shapes.
	 */
	public static final int BOX_STYLE = 0;
	/**
	 * For use with different tool shapes.
	 */
	public static final int CIRCLE_STYLE = 1;
	/**
	 * For use with different tool shapes.
	 */
	public static final int LINE_STYLE = 2;
	
	private Comets c;
	private GraphicSetupPanel gsp;
	private GraphicSetupPanel3D gsp3d;
	private JPanel displayTogglePanel;
	private JComboBox displayComboBox;
	private JCheckBox colorStyleBox;
	protected CometsToolbarPanel tbp;
	private CometsSimControlPanel simControls;
	private DataContentPanel biomassPanel,
				   			 mediaPanel;
	private GridBagConstraints gbc;

	/**
	 * Builds a new <code>CometsSetupPanel</code> and links it to the <code>Comets</code>
	 * instance.
	 * @param c - the <code>Comets</code> that will own this setup panel.
	 */
	public CometsSetupPanel(Comets c)
	{
		super();
		this.c = c;
		
		//();
		//setupMediaPanel();
		
		setLayout(new GridBagLayout());
		gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.BOTH;
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridheight = GridBagConstraints.REMAINDER;
		
		//gsp = new GraphicSetupPanel(c, this);
		//add(gsp, gbc);
		
		gbc.gridx = 1;
		gbc.gridheight = 1;
		
		buildDisplayPanel();
		add(displayTogglePanel, gbc);
		
		gbc.gridy = 1;
		tbp = new CometsToolbarPanel(c, this);
		add(tbp, gbc);
		
		simControls = new CometsSimControlPanel(c);
		simControls.setVisible(false);
		add(simControls, gbc);
		
		gbc.gridy = 2;
		gbc.gridheight = GridBagConstraints.REMAINDER;
		add(new JPanel(), gbc);
	}

	public void setMode(int mode)
	{
		switch(mode)
		{
			case(SIMULATION_MODE) :
				tbp.setVisible(false);
				simControls.reset();
				simControls.setVisible(true);
				break;
			case(SETUP_MODE) :
				simControls.setVisible(false);
				simControls.reset();
				if(gsp != null)
					tbp.setVisible(true);
				else if(gsp == null)
					tbp.setVisible(false);
				break;
			default:
				break;
		}
	}
	
	public void repaint()
	{
		super.repaint();
		if (gsp != null)
			gsp.repaint();
		if (gsp3d!=null)
			gsp3d.repaint();
	}
	
	/**
	 * Builds a small <code>JPanel</code> that contains display options for the 
	 * <code>GraphicSetupPanel</code>. These include a <code>JComboBox</code> to choose which
	 * nutrient layer (or biomass) to display, a <code>JCheckBox</code> to toggle between
	 * relative and absolute color modes, and a <code>JSpinner</code> to control the top color
	 * level in absolute color more.
	 * <p>
	 * This panel is stored and used internally to the <code>CometsSetupPanel</code>.
	 */
	private void buildDisplayPanel()
	{
		displayTogglePanel = new JPanel();
		displayTogglePanel.setLayout(new GridBagLayout());
		
		displayTogglePanel.setBorder(BorderFactory.createTitledBorder("Display"));
		displayComboBox = new JComboBox();
		displayComboBox.addActionListener( 
				new ActionListener() {
					public void actionPerformed(ActionEvent e)
					{
						if (c.getWorld() != null)
						{
							JComboBox cb = (JComboBox)e.getSource();
							c.getParameters().setDisplayLayer(cb.getSelectedIndex());
							gsp.repaint();
//							gsp.setDisplayToggle(cb.getSelectedIndex());
						}
						else if (c.getWorld3D() != null)
						{
							JComboBox cb = (JComboBox)e.getSource();
							c.getParameters().setDisplayLayer(cb.getSelectedIndex());
							gsp3d.repaint();
//							gsp.setDisplayToggle(cb.getSelectedIndex());
						}
					}
				});
		
		colorStyleBox = new JCheckBox("Relative Color Style");
		colorStyleBox.setSelected(c.getParameters().getColorRelative());

		/* Make a specialized SpinnerNumberModel specific for the absolute color style
		 * widget. Basically, if the value is below 1, each click should increase or decrease
		 * by 1/2, but if it's over 1, it should increase or decrease by 1.
		 */
		SpinnerNumberModel colorValModel = new SpinnerNumberModel(10, 0.001, 100000.0, 1.0) 
		{
			private static final long serialVersionUID = 7434998278315415342L;

			// Increment by 1 if over 1, or by 1/2 if under 1.
			// Also, if we're crossing from < 1 -> 1, then just bump up to 1.0
			// eg. if we're at 0.75, and we click up, then just go to 1 and not to 1.5.
			public Object getNextValue() 
			{
				double val = ((Double)getValue()).doubleValue();
				if (val >= 1)
					return new Double(val+1);
				else
				{
					double newVal = val*2;
					if (newVal > 1)
						newVal = 1;
					return new Double(newVal);
				}
			}
			
			// decrement by 1 if the value's greater than 1, or by 1/2 if less than 1
			public Object getPreviousValue() 
			{ 
				double val = ((Double)getValue()).doubleValue();
				if (val > 1)
					return new Double(val-1);
				else if (val > ((Double)getMinimum()).doubleValue())
					return new Double(val/2);
				return null;
			}
		};
		// init the JSpinner
		final JSpinner colorValSpinner = new JSpinner(colorValModel);
		colorValSpinner.addChangeListener(
				new ChangeListener() {
					public void stateChanged(ChangeEvent e)
					{
						if (c.getWorld() != null)
							gsp.setColorStyle(false, ((Double)colorValSpinner.getValue()).doubleValue());
						else if(c.getWorld3D() != null)
							gsp3d.setColorStyle(false, ((Double)colorValSpinner.getValue()).doubleValue());
					}
				});
		colorValSpinner.setEnabled(false);

		// init a simple JLabel
		final JLabel colorLabel = new JLabel("Color value:  ");
		colorLabel.setEnabled(false);

		// give the color style box a basic function to activate/deactivate the spinner
		// and JLabel, as well as updating the GraphicSetupPanel
		colorStyleBox.addActionListener(
				new ActionListener() {
					public void actionPerformed(ActionEvent e)
					{
						colorValSpinner.setEnabled(!colorStyleBox.isSelected());
						colorLabel.setEnabled(!colorStyleBox.isSelected());
						if (c.getWorld() != null)
							gsp.setColorStyle(colorStyleBox.isSelected(), ((Double)colorValSpinner.getValue()).doubleValue());
						else if(c.getWorld3D() != null)
							gsp3d.setColorStyle(colorStyleBox.isSelected(), ((Double)colorValSpinner.getValue()).doubleValue());
					}
				});

		// Finally, wrap it up with a GridBagLayout.
		// God, the GBL code is fugly.
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.BOTH;
		gbc.gridwidth = 2;
		gbc.gridheight = 1;
		gbc.gridx = 0;
		gbc.gridy = 0;
		displayTogglePanel.add(displayComboBox, gbc);

		gbc.gridy = 1;
		displayTogglePanel.add(colorStyleBox, gbc);
		
		gbc.gridy = 2;
		gbc.gridwidth = 1;
		displayTogglePanel.add(colorLabel, gbc);
		
		gbc.gridx = 1;
		displayTogglePanel.add(colorValSpinner, gbc);

		resetDisplayPanel();
	}

	
	/**
	 * Resets the internally stored panel containing display modifying widgets. This removes
	 * all media names from the <code>JComboBox</code> and reloads them with whatever is 
	 * present in the world.
	 */
	private void resetDisplayPanel()
	{
		int layer = c.getParameters().getDisplayLayer();
		displayComboBox.removeAllItems();
		if (c.getWorld() != null)
		{
			String[] mediaNames = c.getWorld().getMediaNames();
			for (int i=0; i<mediaNames.length; i++)
			{
				displayComboBox.addItem(mediaNames[i]);
			}
			displayComboBox.addItem("Biomass");
			
			// set the currently selected one to be the biomass one.
			if (layer > displayComboBox.getItemCount())
			{
				displayComboBox.setSelectedIndex(mediaNames.length);
				c.getParameters().setDisplayLayer(mediaNames.length);
			}
			else
				displayComboBox.setSelectedIndex(layer);
			
			colorStyleBox.setSelected(c.getParameters().getColorRelative());

			gsp.repaint();
		}
		else if (c.getWorld3D() != null)
		{
			String[] mediaNames = c.getWorld3D().getMediaNames();
			for (int i=0; i<mediaNames.length; i++)
			{
				displayComboBox.addItem(mediaNames[i]);
			}
			displayComboBox.addItem("Biomass");
			
			// set the currently selected one to be the biomass one.
			if (layer > displayComboBox.getItemCount())
			{
				displayComboBox.setSelectedIndex(mediaNames.length);
				c.getParameters().setDisplayLayer(mediaNames.length);
			}
			else
				displayComboBox.setSelectedIndex(layer);
			
			colorStyleBox.setSelected(c.getParameters().getColorRelative());

			gsp3d.repaint();
		}
		else
		{
			displayComboBox.addItem("No World Loaded");
		}
	}
	
	/**
	 * Clears the content of the setup panel. This removes any content panel information
	 * set by the user, and affects both the information from this setup panel and the
	 * associated toolbar panel.
	 */
	public void clear()
	{
		biomassPanel = null;
		mediaPanel = null;
		if(gsp != null)
		    gsp.clear();
		//if(gsp3d != null)
			//gsp3d.clear();
		tbp.clearContent();
		resetDisplayPanel();
	}
	
	/**
	 * This sets up a <code>DataContentPanel</code> that stores information about
	 * the biomass content that gets placed into the world by the user. If the user just
	 * clicks on a single space, then this content panel is populated with the concentrations
	 * currently present in the space, otherwise it's initialized with zeros.
	 * <p>
	 * It fetches all the information it needs from the <code>Models</code> that are loaded
	 * into the <code>Comets</code> object.
	 */
	private void setupBiomassPanel()
	{
		String[] names = new String[c.getModels().length];
		for (int i=0; i<names.length; i++)
		{
			names[i] = c.getModels()[i].getModelName();
			if (names[i] == null || names[i].length() == 0)
				names[i] = "Model " + i + ": ";
		}
//		double[] values = new double[c.getModels().length];
		biomassPanel = new DataContentPanel(names, new String[]{"g"}, false, null);
	}
	
	/**
	 * This sets up a <code>DataContentPanel</code> that stores information about
	 * the media content that gets placed into the world by the user. If the user just
	 * clicks on a single space, then this content panel is populated with the concentrations
	 * currently present in the space, otherwise it's initialized with zeros.
	 * <p>
	 * It fetches all the information it needs from the <code>Models</code> that are loaded
	 * into the <code>Comets</code> object.
	 */
	private void setupMediaPanel()
	{
		String[] names = c.getWorld().getMediaNames();
		mediaPanel = new DataContentPanel(names, new String[]{"mmol"}, true, new boolean[names.length]);
	}

	/**
	 * Returns the <code>DataContentPanel</code> used for representing media, or <code>null</code>
	 * if no <code>Models</code> are loaded. 
	 * @return a <code>DataContentPanel</code> with media information, or <code>null</code>
	 */
	public DataContentPanel getMediaPanel() 
	{
		if (c.getModels().length == 0)
			return null;

		else if (mediaPanel == null)
			setupMediaPanel();
		return mediaPanel; 
	}

	/**
	 * Returns the <code>DataContentPanel</code> used for representing biomass, or <code>null</code>
	 * if no <code>Models</code> are loaded. 
	 * @return a <code>DataContentPanel</code> with biomass information, or <code>null</code>
	 */
	public DataContentPanel getBiomassPanel() 
	{
		if (c.getModels().length == 0)
			return null;
		
		else if (mediaPanel == null)
			setupBiomassPanel();
		return biomassPanel; 
	}

	/**
	 * Either shows or hides the <code>CometsToolbarPanel</code> containing a tool palette.
	 * @param b - if true, show the panel, if false, hide it.
	 */
	public void showLayoutToolbar(boolean b)
	{
		tbp.showLayoutToolbar(b);
		revalidate();
	}

	/**
	 * @return the currently selected tool in the <code>CometsToolbarPanel</code>.
	 */
	public AbstractTool getActiveTool()
	{
		return tbp.getActiveTool();
	}
	
	/**
	 * Sets a selection shape in the <code>GraphicSetupPanel</code>. The main function of
	 * this shape is to show the user what region is selected/being drawn on/moused over/etc.
	 * This shape is drawn by the <code>GraphicSetupPanel</code>.
	 * @param shape - a <code>Shape</code> to be drawn on the screen.
	 */
	public void setSelectionShape(Shape shape)
	{
		gsp.setSelectionShape(shape);
	}
	
	public Shape getSelectionShape()
	{
		return gsp.getSelectionShape();
	}

	/**
	 * "Selects" a set of spaces in the <code>GraphicSetupPanel</code>. Any right-click
	 * operation done on that panel is done to the entire set of selected spaces. Every
	 * <code>Point</code> in the passed <code>List</code> is expected to correspond
	 * to a space on the world grid.
	 * @param points - an <code>List</code> of <code>Points</code>.
	 */
	public void selectSpaces(List<Point> points)
	{
		gsp.selectSpaces(points);
	}

	/**
	 * Adds a single media refresh space to the world at the given point, and overwrites the
	 * one present if necessary.
	 * @param values
	 * @param p
	 * @param overwrite
	 */
	public void addMediaRefreshSpace(double[] values, Point p, boolean overwrite)
	{
		addMediaRefreshSpace(values, (int)p.getX(), (int)p.getY(), overwrite);
	}
	
	/**
	 * Adds a single media refresh space to the world at the given point, and overwrites
	 * the one present if necessary
	 * @param values
	 * @param x
	 * @param y
	 * @param overwrite
	 */
	public void addMediaRefreshSpace(double[] values, int x, int y, boolean overwrite)
	{
		if (overwrite)
			removeMediaRefreshSpace(x, y);
		c.getWorld().addMediaRefreshSpace(x, y, values);
	}
	
	/**
	 * Adds a set of media refresh spaces to the world at the given points, each with
	 * the same associated values, and overwrites the ones present if necessary. 
	 * @param values
	 * @param spaces
	 * @param overwrite
	 */
	public void addMediaRefreshSpaces(double[] values, List<Point> spaces, boolean overwrite)
	{
		Iterator<Point> it = spaces.iterator();
		while (it.hasNext())
			addMediaRefreshSpace(values, it.next(), overwrite);
	}

	/**
	 * Removes a media refresh space from the world at the given point.
	 * @param p
	 */
	public void removeMediaRefreshSpace(Point p)
	{
		removeMediaRefreshSpace((int)p.getX(), (int)p.getY());
	}
	
	/**
	 * Removes a media refresh space from the world at the given (x,y) coordinate.
	 * @param x
	 * @param y
	 */
	public void removeMediaRefreshSpace(int x, int y)
	{
		c.getWorld().removeMediaRefreshSpace(x, y);
	}

	/**
	 * Removes a set of media refresh spaces from the world at the given points.
	 * @param spaces
	 */
	public void removeMediaRefreshSpaces(List<Point> spaces)
	{
		Iterator<Point> it = spaces.iterator();
		while(it.hasNext())
			removeMediaRefreshSpace(it.next());
	}

	/**
	 * Adds a static media space to the world at the given point, and overwrites if necessary.
	 * @param values
	 * @param staticSet
	 * @param p
	 * @param overwrite
	 */
	public void addStaticMediaSpace(double[] values, boolean[] staticSet, Point p, boolean overwrite)
	{
		addStaticMediaSpace(values, staticSet, (int)p.getX(), (int)p.getY(), overwrite);
	}
	
	/**
	 * Adds a static media space to the world at the given (x,y) coordinate, and overwrites if necessary.
	 * @param values
	 * @param staticSet
	 * @param x
	 * @param y
	 * @param overwrite
	 */
	public void addStaticMediaSpace(double[] values, boolean[] staticSet, int x, int y, boolean overwrite)
	{
		if (overwrite)
			removeStaticMediaSpace(x, y);
		c.getWorld().addStaticMediaSpace(x, y, values, staticSet);
	}
	
	/**
	 * Adds a set of static media spaces to the world at the given points, and overwrites if necessary.
	 * @param values
	 * @param staticSet
	 * @param spaces
	 * @param overwrite
	 */
	public void addStaticMediaSpaces(double[] values, boolean[] staticSet, List<Point> spaces, boolean overwrite)
	{
		Iterator<Point> it = spaces.iterator();
		while (it.hasNext())
			addStaticMediaSpace(values, staticSet, it.next(), overwrite);
	}
	
	/**
	 * Removes a static media space from the world at the given point.
	 * @param p
	 */
	public void removeStaticMediaSpace(Point p)
	{
		removeStaticMediaSpace((int)p.getX(), (int)p.getY());
	}
	
	/**
	 * Removes a static media space from the world at the given (x,y) coordinate.
	 * @param x
	 * @param y
	 */
	public void removeStaticMediaSpace(int x, int y)
	{
		c.getWorld().removeStaticMediaSpace(x, y);
	}
	
	/**
	 * Removes a set of static media spaces from the world at the given points.
	 * @param spaces
	 */
	public void removeStaticMediaSpaces(List<Point> spaces)
	{
		Iterator<Point> it = spaces.iterator();
		while(it.hasNext())
			removeStaticMediaSpace(it.next());
	}
	
	/**
	 * Changes the biomass concentrations in the world at the given point p. The
	 * new values used are set by the values in the <code>DataContentPanel</code>. If
	 * the overwrite parameter is false, then the values in the biomass content panel
	 * are added to the current concentrations in the space.
	 * @param p - the point to apply the new biomass.
	 * @param overwrite - if true, set the concentrations at p to be those in the 
	 * biomass <code>DataContentPanel</code>, otherwise just add to the current values
	 * at p.
	 * @see #getBiomassPanel()
	 */
	public void applyBiomassChange(Point p, boolean overwrite)
	{
		applyBiomassChange(biomassPanel.getValues(), p, overwrite);
	}
	
	/**
	 * Changes the biomass concentrations in the world at the given point p. If
	 * the overwrite parameter is false, then the passed values are added to the 
	 * current concentrations in the space.
	 * @param values - the biomass values to adjust the space with.
	 * @param p - the point to apply the new biomass.
	 * @param overwrite - if true, set the concentrations at p to be those in the 
	 * values parameter, otherwise just add to the current values at p.
	 */
	public void applyBiomassChange(double[] values, Point p, boolean overwrite)
	{
		if (overwrite)
			c.getWorld().setBiomass((int)p.getX(), (int)p.getY(), values);
		else
			c.getWorld().changeBiomass((int)p.getX(), (int)p.getY(), values);		
	}
	
	/**
	 * Changes the biomass concentrations in the world at all points in the given
	 * <code>List</code>. The new values used are set by the values in the 
	 * <code>DataContentPanel</code>. If the overwrite parameter is false, then 
	 * the values in the biomass content panel are added to the current 
	 * concentrations in the space.
	 * @param points - an <code>List</code> of <code>Points</code> to update. 
	 * @param overwrite - if true, set the concentrations at each point to be those in the 
	 * biomass <code>DataContentPanel</code>, otherwise just add to the current values
	 * at each of the points.
	 * @see #getBiomassPanel()
	 */
	public void applyBiomassChange(List<Point> points, boolean overwrite)
	{
		applyBiomassChange(biomassPanel.getValues(), points, overwrite);
	}
	
	/**
	 * Changes the biomass concentrations in the world at all points in the given
	 * <code>List</code>. The new values used are set by the values in the 
	 * passed array. If the overwrite parameter is false, then 
	 * the passed values added to the current 
	 * concentrations in each space in the <code>List</code>.
	 * @param values - the biomass values to adjust the space with.
	 * @param points - an <code>List</code> of <code>Points</code> to update. 
	 * @param overwrite - if true, set the concentrations at each point to be those in the 
	 * values parameter, otherwise just add to the current values at each point.
	 */
	public void applyBiomassChange(double[] values, List<Point> points, boolean overwrite)
	{
		Iterator<Point> it = points.iterator();
		while (it.hasNext())
		{
			Point p = it.next();
			applyBiomassChange(values, p, overwrite);
		}
	}

	/**
	 * Changes the media concentrations in the world at the given point p. The
	 * new values used are set by the values in the <code>DataContentPanel</code>. If
	 * the overwrite parameter is false, then the values in the media content panel
	 * are added to the current concentrations in the space.
	 * @param p - the point to apply the new media.
	 * @param overwrite - if true, set the concentrations at p to be those in the 
	 * media <code>DataContentPanel</code>, otherwise just add to the current values
	 * at p.
	 * @see #getMediaPanel()
	 */
	public void applyMediaChange(Point p, boolean overwrite)
	{
		applyMediaChange(mediaPanel.getValues(), p, overwrite);
	}
	
	/**
	 * Changes the media concentrations in the world at the given point p. If
	 * the overwrite parameter is false, then the passed values are added to the 
	 * current concentrations in the space.
	 * @param values - the media values to adjust the space with.
	 * @param p - the point to apply the new media.
	 * @param overwrite - if true, set the concentrations at p to be those in the 
	 * values parameter, otherwise just add to the current values at p.
	 */
	public void applyMediaChange(double[] values, Point p, boolean overwrite)
	{
		if (overwrite)
			c.getWorld().setMedia((int)p.getX(), (int)p.getY(), values);
		else
			c.getWorld().changeMedia((int)p.getX(), (int)p.getY(), values);
	}
	
	/**
	 * Changes the media concentrations in the world at all points in the given
	 * <code>List</code>. The new values used are set by the values in the 
	 * <code>DataContentPanel</code>. If the overwrite parameter is false, then 
	 * the values in the media content panel are added to the current 
	 * concentrations in the space.
	 * @param points - an <code>List</code> of <code>Points</code> to update. 
	 * @param overwrite - if true, set the concentrations at each point to be those in the 
	 * media <code>DataContentPanel</code>, otherwise just add to the current values
	 * at each of the points.
	 * @see #getMediaPanel()
	 */
	public void applyMediaChange(List<Point> points, boolean overwrite)
	{
		applyMediaChange(mediaPanel.getValues(), points, overwrite);
	}
	
	/**
	 * Changes the media concentrations in the world at all points in the given
	 * <code>List</code>. The new values used are set by the values in the 
	 * passed array. If the overwrite parameter is false, then 
	 * the passed values added to the current 
	 * concentrations in each space in the <code>List</code>.
	 * @param values - the media values to adjust the space with.
	 * @param points - an <code>List</code> of <code>Points</code> to update. 
	 * @param overwrite - if true, set the concentrations at each point to be those in the 
	 * values parameter, otherwise just add to the current values at each point.
	 */
	public void applyMediaChange(double[] values, List<Point> points, boolean overwrite)
	{
		Iterator<Point> it = points.iterator();
		while (it.hasNext())
		{
			Point p = it.next();
			applyMediaChange(values, p, overwrite);
		}
	}

	/**
	 * Adds or removes a barrier at world point p.
	 * @param p - the grid coordinate to add or remove a barrier space.
	 * @param barrier - if true, set the space to be a barrier, otherwise remove the barrier.
	 */
	public void setBarrier(Point p, boolean barrier)
	{
		c.getWorld().setBarrier((int)p.getX(), (int)p.getY(), barrier);
	}
	
	/**
	 * Adds or removes a barrier from each space signified by the <code>List</code>
	 * of <code>Points</code>.
	 * @param points - an <code>List</code> of <code>Points</code> in the world
	 * @param barrier - if true, set all spaces to be a barrier, otherwise remove the barrier
	 * from all given spaces.
	 */
	public void setBarrier(List<Point> points, boolean barrier)
	{
		Iterator<Point> it = points.iterator();
		while (it.hasNext())
		{
			Point p = it.next();
			setBarrier(p, barrier);
		}
	}
	
	/**
	 * Adds <code>GraphicsSetupPanel</code> to <code>CometsSetupPanel</code>.
 	 * @param dimensionality - if <code>DIMENSIONALITY_3D</code> adds a 3D panel,
	 * if <code>DIMENSIONALITY_2D</code> adds a 2D panel.
	 */
	public void addGraphicsSetupPanel(int dimensionality)
	{
		gbc.fill = GridBagConstraints.BOTH;
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridheight = GridBagConstraints.REMAINDER;
		
		switch(dimensionality)
		{
			case(DIMENSIONALITY_2D) :
				gsp = new GraphicSetupPanel(c, this);
				add(gsp, gbc);
				break;
			case(DIMENSIONALITY_3D) :
				gsp3d = new GraphicSetupPanel3D(c);
			    add(gsp3d, gbc);
				break;
			default:
				break;
		}
	}
	
	/**
	 * Removes existing <code>GraphicsSetupPanel</code> from <code>CometsSetupPanel</code>.
	 */
	public void removeGraphicSetupPanel()
	{
		for(int i=0;i<this.getComponents().length;i++)
		{
			if(this.getComponents()[i] instanceof GraphicSetupPanel)
				remove(this.getComponents()[i]);
		}
		for(int i=0;i<this.getComponents().length;i++)
		{
			if(this.getComponents()[i] instanceof GraphicSetupPanel3D)
				remove(this.getComponents()[i]);
		}
	}
}




/* An ugly piece of code I'm embarrassed to claim credit for. It copies so much from all 
 * over the place instead of linking things together in one place.
 * <p>
 * Still, it works, and at this point, I'm more interested in graduating than writing pretty
 * code. I feel bad for whoever follows up on this.
 */
/**
 * The <code>GraphicSetupPanel</code> is the graphical component of the <code>CometsSetupPanel</code>.
 * This handles showing off the state of the currently loaded world in an interactive way
 * that allows the user to add and remove biomass and media components from it.
 * <p> 
 * This class is set in the <code>CometsSetupPanel</code> file since that's the only class
 * that should directly interact with it. Any other interaction is done indirectly through 
 * Java's mouse interface.
 * @author Bill Riehl
 *
 */
@SuppressWarnings("serial")
class GraphicSetupPanel extends JPanel implements CometsConstants,
												  MouseListener,
												  MouseMotionListener
{
	private Comets c;
	private CometsSetupPanel csp;
	private Shape selectionShape;
	private CometsParameters cParams;

	private double[] colorScale = {10.0, 10.0, 10.0};
	private JPopupMenu popupMenu;
	private Point //mouseDown,
				  clickPoint;
	private List<Point> selectedSpaces;
	private JRadioButton overButton,
						 modButton;
	private ButtonGroup bgroup;
	
	private JCheckBox barrierBox;
	private JCheckBox[] biomassInBoxes,
						biomassOutBoxes,
						mediaInBoxes,
						mediaOutBoxes;
	
	/**
	 * Instantiates a <code>GraphicSetupPanel</code> to be displayed in a <code>Comets</code>
	 * and controlled by a <code>CometsSetupPanel</code>.
	 * @param c - the <code>Comets</code> object this will sit in.
	 * @param csp - the <code>CometsSetupPanel</code> that will be in control
	 */
	public GraphicSetupPanel(Comets c, CometsSetupPanel csp) 	
	{
		super();
		this.csp = csp;
		this.c = c;
		this.cParams = c.getParameters();
		colorScale = new double[3];
		for (int i=0; i<3; i++)
			colorScale[i] = 10;

		overButton = new JRadioButton("Overwrite current values");
		modButton = new JRadioButton("Modify existing values");
		overButton.setSelected(true);
		bgroup = new ButtonGroup();
		bgroup.add(overButton);
		bgroup.add(modButton);

		// ----------------------- init the popup menu ----------------------
		popupMenu = new JPopupMenu();
		JMenuItem biomassItem = new JMenuItem("Change biomass...");
		biomassItem.addActionListener(
				new ActionListener() {
					public void actionPerformed(ActionEvent e)
					{
						changeSpaceContent(true);
					}
				});
		popupMenu.add(biomassItem);
		
		JMenuItem mediaItem = new JMenuItem("Change media...");
		mediaItem.addActionListener(
				new ActionListener() {
					public void actionPerformed(ActionEvent e)
					{
						changeSpaceContent(false);
					}
				});
		popupMenu.add(mediaItem);
		
		JMenuItem propertyItem = new JMenuItem("Change space properties...");
		propertyItem.addActionListener(
				new ActionListener() {
					public void actionPerformed(ActionEvent e)
					{
						changeSpaceProperties();
					}
				});
		popupMenu.add(propertyItem);
		// ------------------ popup menu done! --------------------------
		
		selectedSpaces = new ArrayList<Point>();
		
//		showGrid = false;
		setPreferredSize(new Dimension(cParams.getNumCols() * cParams.getPixelScale(), cParams.getNumRows() * cParams.getPixelScale()));
		addMouseListener(this);
		addMouseMotionListener(this);
		setOpaque(true);
	}

	/**
	 * Clears all selected spaces and selection shapes from this <code>GraphicSetupPanel</code>.
	 */
	public void clear()
	{
		selectedSpaces.clear();
		selectionShape = null;
		repaint();
	}
	
	/**
	 * Selects a set of spaces signified by the given list of <code>Points</code>.
	 * @param points - the <code>List</code> of <code>Points</code> to select.
	 */
	public void selectSpaces(List<Point> points)
	{
		selectedSpaces = points;
	}
	
	/**
	 * When a user clicks on a single space, the world's info panel gets updated
	 * to point to this space for displaying information. 
	 * @param p - the mouse coordinate clicked (will be translated to world coordinates)
	 * @see Comets#showSpaceInfoFrame(boolean)
	 * @see World2D#updateInfoPanel(int, int)
	 */
	private void clickOnSpace(Point p)
	{
		p = new Point((int)Math.floor(p.getX()/cParams.getPixelScale()),
					  (int)Math.floor(p.getY()/cParams.getPixelScale()));
		if (c.getWorld() != null)
			c.getWorld().updateInfoPanel((int)p.getX(), (int)p.getY());
	}
	
	/**
	 * Updates the biomass or media content of the world. This is called through the 
	 * popup menu. If the user right-clicked on a single point, then only that point's 
	 * biomass/media is modified. Otherwise, the currently selected set of points is updated.
	 * <p>
	 * The first step is to create a modal dialog with the <code>DataContentPanel</code>
	 * for either biomass or media. If only one space is chosen, that panel is updated to
	 * show the current info for that space, otherwise it's filled with zeros.
	 * <p>
	 * If multiple spaces are chosen, the user also has the option to either overwrite the
	 * area, or just add to the concentrations already present.
	 * <p>
	 * Finally, if the user hits 'OK', the values in the <code>DataContentPanel</code>
	 * are used to update the concentrations in the selected spaces.
	 * @param biomass - if true, update the biomass, if false, update the media
	 */
	private void changeSpaceContent(boolean changeBiomass)
	{
		/* 1. get the space(s) to be changed.
		 * If just one, update the textbox info with what's in the space.
		 * If multiple, then leave it all blank, etc.
		 */
		boolean oneSpace = selectedSpaces.size() == 0;
		String type = "biomass";
		if (!changeBiomass)
			type = "media";
		String title = "Adjust " + type + " in ";
		
		if (oneSpace)
			title += "(" + (int)clickPoint.getX() + ", " + (int)clickPoint.getY() + ")";
		else
			title += "selected spaces";

		DataContentPanel dataPanel = csp.getBiomassPanel();
		if (type.equals("media"))
			dataPanel = csp.getMediaPanel();
		
		/* If only modifying one space, then fetch the values
		 * for that space and fill in the blanks with it.
		 * Otherwise, leave it as 0.0.
		 */
		if (oneSpace)
		{
			if (changeBiomass)
				dataPanel.setValues(c.getWorld().getBiomassAt((int)clickPoint.getX(), (int)clickPoint.getY()));
			else
			{
				dataPanel.setValues(c.getWorld().getMediaAt((int)clickPoint.getX(), (int)clickPoint.getY()));
				StaticPoint sp = c.getWorld().getStaticMediaSpace((int)clickPoint.getX(), (int)clickPoint.getY());
				if (sp != null)
					dataPanel.setStatic(sp.getStaticSet());
				else
					dataPanel.setStatic(null);
			}
		}
		
		/* If we're modifying more than one space, then we don't know
		 * how much biomass is already there. So, we're not sure whether
		 * the user wants to set a total amount of biomass in that block,
		 * or to increment (or decrement) the biomass in each cell of that
		 * block. So we need to make a ButtonGroup to give that option.
		 */
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		if (!oneSpace)
		{
			panel.add(overButton);
			panel.add(modButton);
		}
		if (dataPanel.getValues().length > 8)
		{
			JScrollPane scrollPane = new JScrollPane(dataPanel);
			scrollPane.setPreferredSize(new Dimension(200, 300));
			panel.add(scrollPane);
		}
		else
			panel.add(dataPanel);
			
		int result = JOptionPane.showConfirmDialog(this, 
												   panel, 
												   title, 
												   JOptionPane.OK_CANCEL_OPTION, 
												   JOptionPane.PLAIN_MESSAGE);
		if (result == JOptionPane.OK_OPTION)
		{
			// update biomass!
			dataPanel.updateValues();
//			double[] delta = dataPanel.getValues();
			
			if (changeBiomass)
			{
				if (oneSpace)
					csp.applyBiomassChange(clickPoint, true);
				else
					csp.applyBiomassChange(selectedSpaces, !modButton.isSelected());
			}
			else
			{
				if (oneSpace)
					csp.applyMediaChange(clickPoint, true);
				else
					csp.applyMediaChange(selectedSpaces, !modButton.isSelected());
				if (dataPanel.hasStaticChecked())
				{
					if (oneSpace)
						csp.addStaticMediaSpace(dataPanel.getValues(), dataPanel.getStatic(), clickPoint, !modButton.isSelected());
					else
						csp.addStaticMediaSpaces(dataPanel.getValues(), dataPanel.getStatic(), selectedSpaces, !modButton.isSelected());
				}
			}
			c.backupState(true);
			repaint();
		}
	}

	/**
	 * Sets a <code>Shape</code> signifying which area is selected by the user. This
	 * will be drawn on top of everything else in the paintComponent() method.
	 * @param shape - the <code>Shape</code> to show selection.
	 * @see #paintComponent(Graphics)
	 */
	public void setSelectionShape(Shape shape)
	{
		selectionShape = shape;
		repaint();
	}
	
	public Shape getSelectionShape()
	{
		return selectionShape;
	}
	
	/**
	 * Allows the user to change the physical properties of either one space or all 
	 * selected spaces. The include whether the space is a barrier, or the ability to
	 * let biomass or media flow in or out of each space.
	 * <p>
	 * Well, okay, right now the only real property is whether or not a space should
	 * be a barrier. But the code stubs are written for everything else.
	 * <p>
	 * As with {@link #changeSpaceContent(boolean)}, the user is prompted with a dialog
	 * box with the option to either set the selected (or single) spaces to be a barrier 
	 * or not. Once the user clicks 'OK', the command is passed along to the <code>World2D</code>.
	 */
	private void changeSpaceProperties()
	{
		biomassInBoxes = new JCheckBox[c.getModels().length];
		biomassOutBoxes = new JCheckBox[c.getModels().length];
		mediaInBoxes = new JCheckBox[c.getWorld().getNumMedia()];
		mediaOutBoxes = new JCheckBox[c.getWorld().getNumMedia()];
		final JLabel[] biomassLabels = new JLabel[c.getModels().length];
		final JLabel[] mediaLabels = new JLabel[c.getWorld().getNumMedia()];
		final JLabel biomassInLabel = new JLabel("in");
		final JLabel biomassOutLabel = new JLabel("out");
		final JLabel mediaInLabel = new JLabel("in");
		final JLabel mediaOutLabel = new JLabel("out");
		final JLabel selectAllLabel = new JLabel("Select All", JLabel.RIGHT);
		final JCheckBox selectAllBiomassIn,
						selectAllBiomassOut,
						selectAllMediaIn,
						selectAllMediaOut;
		String[] mediaNames = c.getWorld().getMediaNames(); 
		
		boolean oneSpace = selectedSpaces.size() == 0;
		
		String title = "Adjust properties for ";
		if (oneSpace)
			title += "space (" + (int)clickPoint.getX() + ", " + (int)clickPoint.getY() + ")";
		else
			title += "multiple spaces";
		
		for (int i=0; i<biomassInBoxes.length; i++)
		{
			biomassInBoxes[i] = new JCheckBox();
			biomassInBoxes[i].addItemListener(
					new ItemListener() {
						public void itemStateChanged(ItemEvent e)
						{
							if (e.getStateChange() == ItemEvent.DESELECTED)
								barrierBox.setSelected(false);
						}
					});
			biomassOutBoxes[i] = new JCheckBox();
			biomassOutBoxes[i].addItemListener(
					new ItemListener() {
						public void itemStateChanged(ItemEvent e)
						{
							if (e.getStateChange() == ItemEvent.DESELECTED)
								barrierBox.setSelected(false);
						}
					});

			biomassLabels[i] = new JLabel("Model " + (i+1), JLabel.RIGHT);
		}
		selectAllBiomassIn = new JCheckBox();
		selectAllBiomassIn.addItemListener(
				new ItemListener() {
					public void itemStateChanged(ItemEvent e)
					{
						for (int i=0; i<biomassInBoxes.length; i++)
							biomassInBoxes[i].setSelected(e.getStateChange() == ItemEvent.SELECTED);
					}
				});
		selectAllBiomassOut = new JCheckBox();
		selectAllBiomassOut.addItemListener(
				new ItemListener() {
					public void itemStateChanged(ItemEvent e)
					{
						for (int i=0; i<biomassOutBoxes.length; i++)
							biomassOutBoxes[i].setSelected(e.getStateChange() == ItemEvent.SELECTED);
					}
				});

		
		for (int i=0; i<mediaInBoxes.length; i++)
		{
			mediaInBoxes[i] = new JCheckBox();
			mediaInBoxes[i].addItemListener(
					new ItemListener() {
						public void itemStateChanged(ItemEvent e)
						{
							if (e.getStateChange() == ItemEvent.DESELECTED)
								barrierBox.setSelected(false);
						}
					});
			mediaOutBoxes[i] = new JCheckBox();
			mediaOutBoxes[i].addItemListener(
					new ItemListener() {
						public void itemStateChanged(ItemEvent e)
						{
							if (e.getStateChange() == ItemEvent.DESELECTED)
								barrierBox.setSelected(false);
						}
					});
			String s = mediaNames[i];
			if (s.length() == 0)
				s = "Media " + (i+1);
			mediaLabels[i] = new JLabel(s, JLabel.RIGHT);
		}
		selectAllMediaIn = new JCheckBox();
		selectAllMediaIn.addItemListener(
				new ItemListener() {
					public void itemStateChanged(ItemEvent e)
					{
						for (int i=0; i<mediaInBoxes.length; i++)
							mediaInBoxes[i].setSelected(e.getStateChange() == ItemEvent.SELECTED);
					}
				});
		selectAllMediaOut = new JCheckBox();
		selectAllMediaOut.addItemListener(
				new ItemListener() {
					public void itemStateChanged(ItemEvent e)
					{
						for (int i=0; i<mediaOutBoxes.length; i++)
							mediaOutBoxes[i].setSelected(e.getStateChange() == ItemEvent.SELECTED);
					}
				});
		
		// if we're just looking at one space, then update all the box info
		// with the space's properties.
		
		JPanel panel = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.BOTH;
		gbc.ipadx = 10;
		gbc.insets = new Insets(5, 5, 5, 5);
		gbc.gridheight = 1;
		gbc.weightx = gbc.weighty = 0.0;
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		
		int useSelectAll = 0;
		
		final JPanel biomassPanel = new JPanel(new GridBagLayout());
		gbc.gridx = 1;
		biomassPanel.add(biomassInLabel, gbc);
		gbc.gridx = 2;
		biomassPanel.add(biomassOutLabel, gbc);

		if (biomassInBoxes.length > 1)
		{
			useSelectAll = 1;
			gbc.gridy = 1;
			gbc.gridx = 0;
			biomassPanel.add(selectAllLabel, gbc);
			gbc.gridx = 1;
			biomassPanel.add(selectAllBiomassIn, gbc);
			gbc.gridx = 2;
			biomassPanel.add(selectAllBiomassOut, gbc);
		}

		
		for (int i=0; i<biomassInBoxes.length; i++)
		{
			gbc.gridy = i+1+useSelectAll;
			gbc.gridx = 0;
			biomassPanel.add(biomassLabels[i], gbc);
			gbc.gridx = 1;
			biomassPanel.add(biomassInBoxes[i], gbc);
			gbc.gridx = 2;
			biomassPanel.add(biomassOutBoxes[i], gbc);
		}
		biomassPanel.setBorder(BorderFactory.createTitledBorder("Biomass Diffusability"));
		
		
		final JPanel mediaPanel = new JPanel(new GridBagLayout());
		gbc.gridy = 0;
		gbc.gridx = 1;
		mediaPanel.add(mediaInLabel, gbc);
		gbc.gridx = 2;
		mediaPanel.add(mediaOutLabel, gbc);
		
		useSelectAll = 0;
		if (mediaInBoxes.length > 1)
		{
			useSelectAll = 1;
			gbc.gridy = 1;
			gbc.gridx = 0;
			mediaPanel.add(selectAllLabel, gbc);
			gbc.gridx = 1;
			mediaPanel.add(selectAllMediaIn, gbc);
			gbc.gridx = 2;
			mediaPanel.add(selectAllMediaOut, gbc);
		}

		for (int i=0; i<mediaInBoxes.length; i++)
		{
			gbc.gridy = i+1+useSelectAll;
			gbc.gridx = 0;
			mediaPanel.add(mediaLabels[i], gbc);
			gbc.gridx = 1;
			mediaPanel.add(mediaInBoxes[i], gbc);
			gbc.gridx = 2;
			mediaPanel.add(mediaOutBoxes[i], gbc);
		}
		
		JScrollPane scrollPane = new JScrollPane(mediaPanel);
		if (mediaInBoxes.length > 8)
			scrollPane.setPreferredSize(new Dimension(200,300));
		scrollPane.setBorder(BorderFactory.createTitledBorder("Media Diffusability"));

		barrierBox = new JCheckBox();
		barrierBox.addItemListener(
				new ItemListener() {
					public void itemStateChanged(ItemEvent e)
					{
						boolean enable = e.getStateChange() == ItemEvent.DESELECTED;
						biomassInLabel.setEnabled(enable);
						biomassOutLabel.setEnabled(enable);
						mediaInLabel.setEnabled(enable);
						mediaOutLabel.setEnabled(enable);
						selectAllLabel.setEnabled(enable);
						for (int i=0; i<mediaInBoxes.length; i++)
						{
							mediaInBoxes[i].setEnabled(enable);
							mediaOutBoxes[i].setEnabled(enable);
							mediaLabels[i].setEnabled(enable);
						}
						for (int i=0; i<biomassInBoxes.length; i++)
						{
							biomassInBoxes[i].setEnabled(enable);
							biomassOutBoxes[i].setEnabled(enable);
							biomassLabels[i].setEnabled(enable);
						}
						selectAllBiomassIn.setEnabled(enable);
						selectAllBiomassOut.setEnabled(enable);
						selectAllMediaIn.setEnabled(enable);
						selectAllMediaOut.setEnabled(enable);
					}
				});
		if (oneSpace)
		{
			barrierBox.setSelected(c.getWorld().isBarrier((int)clickPoint.getX(), (int)clickPoint.getY()));
		}

		
		gbc.gridx = 0;
		gbc.gridy = 0;
		panel.add(new JLabel("Set Barrier", JLabel.LEFT), gbc);
		gbc.gridx = 1;
		panel.add(barrierBox, gbc);
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.gridwidth = 2;
//		panel.add(biomassPanel, gbc);
		gbc.gridy = 2;
//		panel.add(scrollPane, gbc);
		
		int result = JOptionPane.showConfirmDialog(this, 
				   panel, 
				   title, 
				   JOptionPane.OK_CANCEL_OPTION, 
				   JOptionPane.PLAIN_MESSAGE);
		if (result == JOptionPane.OK_OPTION)
		{
			// update stuff!
			// update barrier stuff for now. ignore the rest, even though i spent
			// fucking hours putting it together.
			if (oneSpace)
				c.getWorld().setBarrier((int)clickPoint.getX(), (int)clickPoint.getY(), barrierBox.isSelected());
			else
			{
				Iterator<Point> it = selectedSpaces.iterator();
				while(it.hasNext())
				{
					Point p = it.next();
					c.getWorld().setBarrier((int)p.getX(), (int)p.getY(), barrierBox.isSelected());
				}
			}
			c.backupState(true);
			repaint();
		}
	}
	
	/**
	 * Gets the current color of the world at space (x, y), relative to the maximum
	 * values given in the m array.
	 * @param x - x-coordinate of the world
	 * @param y - y-coordinate of the world
	 * @param m - a 3-element array of double values, used as a coloring threshold.
	 * @return the scaled color of the world as a packed 32-bit integer - like what Processing
	 * uses for colors.
	 */
	public Color currentWorldColor(int x, int y, double[] m)
	{
		if (c.getWorld().isBarrier(x, y))
			return new Color(cParams.getBarrierColor());

		Color col = null;
		if (cParams.getDisplayLayer() == c.getWorld().getNumMedia()) // if it's numMedia, then display cell concs.
		{
			Cell cell = c.getWorld().getCellAt(x, y);
			if (cell != null)
			{
				double[] biomass = cell.getBiomass();
				double[] channels = new double[3];
				for (int i=0; i<biomass.length; i++)
				{
					//channels[i % 3] += biomass[i];
					if(biomass.length==1 || biomass.length == 2)
					{
						channels[i % 3] += biomass[i];
					}
					else
					{
					    if(i<(int)(biomass.length/2))
					    {
						    channels[2] += 0;
						    channels[1] += biomass[i]*Math.sin(Math.PI*i/(biomass.length-1)+Math.PI/2);
						    channels[0] += biomass[i]*Math.sin(Math.PI*i/(biomass.length-1));
					    }
					    else 
					    {
						    channels[1] += 0;
						    channels[2] += biomass[i]*Math.sin(Math.PI*i/(biomass.length-1)-Math.PI/2);
						    channels[0] += biomass[i]*Math.sin(Math.PI*i/(biomass.length-1));
					    }
					}
				}
				col = new Color((int)Math.min((channels[1]*(255/m[1])), 255), (int)Math.min((channels[0]*(255/m[0])), 255),  (int)Math.min((channels[2]*(255/m[2])), 255));
			}
		}
		else
		{
			//	      double m = util.max(media, displayToggle);
			double[] media = c.getWorld().getMediaAt(x, y);
			col = new Color((int)Math.min(255, media[cParams.getDisplayLayer()]*(255/m[0])), 0, 0);
		}
		return col;
	}
	
	/**
	 * Sets the color style to be shown. If <code>isRelative</code> is true, then the
	 * relative color style is used (e.g. color saturation is all spaces is relative to the
	 * space with the highest concentration), and if false then the absolute color style is 
	 * used (in this case, all colors are a function of the passed <code>colorValue</code>
	 * variable and not the most dense space). 
	 * @param isRelative
	 * @param colorValue
	 */
	public void setColorStyle(boolean isRelative, double colorValue)
	{
		cParams.setColorRelative(isRelative);
//		colorRelative = isRelative;
		colorScale = new double[]{ colorValue, colorValue, colorValue };
		repaint();
	}
	
	/**
	 * The paint command for this graphical beast. This is done in two parts. The first is
	 * similar to what happens in the <code>CometsRunPanel</code> - either biomass or 
	 * nutrient concentrations are filled in. In the second part, the current selection
	 * shape is drawn over the top of them, if there is one that needs drawing. This
	 * is also done using an alpha composite, to make it translucent.
	 */
	public void paintComponent(Graphics g)
	{
		int pixelScale = cParams.getPixelScale();
		setBackground(new Color(cParams.getBackgroundColor()));
		g.setColor(getBackground());
		g.fillRect(0, 0, getWidth(), getHeight());
		if (c.getWorld() != null)
		{
			double[] m = new double[3];
			if (cParams.getColorRelative())
			{
//				displayToggle = c.getWorld().getNumMedia();
				if (cParams.getDisplayLayer() == c.getWorld().getNumMedia())
				{
					for (int i=0; i<cParams.getNumCols(); i++)
					{
						for (int j=0; j<cParams.getNumRows(); j++)
						{
							Cell cell = c.getWorld().getCellAt(i, j);
							if (cell != null)
							{
								double[] biomass = cell.getBiomass();
								double[] sum = new double[3];
								for (int k=0; k<biomass.length; k++)
								{
									sum[k%3] += biomass[k];
								}
								for (int k=0; k<3; k++)
								{
									if (sum[k] > m[k])
										m[k] = sum[k];
								}
							}
						}
					}
				}
				else
					m[0] = Utility.max(c.getWorld().getAllMedia(), cParams.getDisplayLayer());
			}
			else
				m = colorScale;
			
			for (int i=0; i<cParams.getNumCols(); i++)
			{
				for (int j=0; j<cParams.getNumRows(); j++)
				{
					Color c = currentWorldColor(i, j, m);
					if (c != null)
					{
						g.setColor(c);
						g.fillRect(i*pixelScale, j*pixelScale, pixelScale, pixelScale);
					}
				}
			}
/*			if (textDisplayCounter > 0 && displayText)
			{
				fill(255);
				text(textDisplay, 10, 30);
				textDisplayCounter--;
			}
			else if (textDisplayCounter == 0)
				displayText = false;
			// paint world
*/		}
		
//		g.setColor(Color.red);
//		g.fillRect(5, 5, 50, 50);
//		
		// Draw the borders
		g.setColor(Color.white);
		g.drawLine(cParams.getPixelScale() * cParams.getNumCols() - 1, 
				   0, 
				   cParams.getPixelScale() * cParams.getNumCols() - 1, 
				   cParams.getPixelScale() * cParams.getNumRows() - 1);
		g.drawLine(0, 
				   cParams.getPixelScale() * cParams.getNumRows() - 1, 
				   cParams.getPixelScale() * cParams.getNumCols() - 1, 
				   cParams.getPixelScale() * cParams.getNumRows() - 1);
		// ---------------
		
		// Draw a selection shape if there's one in the buffer.
		if (selectionShape != null)
		{
			AlphaComposite ta = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.25f);
			((Graphics2D)g).setComposite(ta);
			((Graphics2D)g).setColor(Color.WHITE);
			((Graphics2D)g).fill(selectionShape);
			((Graphics2D)g).setComposite(AlphaComposite.SrcOver);
			((Graphics2D)g).setColor(Color.WHITE);
			((Graphics2D)g).draw(selectionShape);
		}
	}
	
	/**
	 * This panel's preferred size should always be a function of how many rows and columns
	 * there are in the world's grid.
	 */
	public Dimension getPreferredSize()
	{
		return new Dimension(cParams.getNumCols() * cParams.getPixelScale(), cParams.getNumRows() * cParams.getPixelScale());
	}

	/**
	 * As long as we're not expecting a popup menu to appear from clicking, the click 
	 * command should be passed along to the currently active tool. This panel also 
	 * tells the world to update its current space info panel based on what space was
	 * clicked.
	 * <p>
	 * Note that any mouse command is ignored if there are no <code>Models</code> loaded.
	 */
	public void mouseClicked(MouseEvent e)
	{
		if (!e.isPopupTrigger() && c.getMode() == SETUP_MODE)
			if (csp.getActiveTool() != null &&
				c.hasModelsLoaded())
				csp.getActiveTool().mouseClicked(e);
		clickOnSpace(e.getPoint());
	}
				
	/**
	 * As long as we're not expecting a popup menu to appear from pressing a mouse
	 * key, the click command should be passed along to the currently active tool.
	 * <p>
	 * Note that any mouse command is ignored if there are no <code>Models</code> loaded.	 
	 */
	public void mousePressed(MouseEvent e)
	{
		if (c.getMode() != SETUP_MODE)
			return;
		if (c.hasModelsLoaded())
		{
			if (!e.isPopupTrigger())
			{
				if (csp.getActiveTool() != null &&
					c.hasModelsLoaded())
					csp.getActiveTool().mousePressed(e);
			}
			else
			{
				popupMenu.show(e.getComponent(), e.getX(), e.getY());
				clickPoint = new Point((int)Math.floor(e.getPoint().getX()/cParams.getPixelScale()),
									   (int)Math.floor(e.getPoint().getY()/cParams.getPixelScale()));
			}
		}

	}

	/**
	 * Two cases here -<br>
	 * 1. A right click occurred: record the point and show a popup menu there.<br>
	 * 2. No right click - pass this command off to the currently active tool.
	 * <p>
	 * Note that any mouse command is ignored if there are no <code>Models</code> loaded.
	 */
	public void mouseReleased(MouseEvent e)
	{
		if (c.getMode() != SETUP_MODE)
			return;
		if (c.hasModelsLoaded())
		{
			if (!e.isPopupTrigger())
			{
				if (csp.getActiveTool() != null)
					csp.getActiveTool().mouseReleased(e);
			}
			else
			{
				popupMenu.show(e.getComponent(), e.getX(), e.getY());
				clickPoint = new Point((int)Math.floor(e.getPoint().getX()/cParams.getPixelScale()),
									   (int)Math.floor(e.getPoint().getY()/cParams.getPixelScale()));
			}
		}
	}

	/**
	 * A drag command should just be passed along to the active tool.
	 * <p>
	 * Note that any mouse command is ignored if there are no <code>Models</code> loaded. 
	 */
	public void mouseDragged(MouseEvent e)
	{
		if (c.getMode() != SETUP_MODE)
			return;
		if (GraphicSetupPanel.this.csp.getActiveTool() != null &&
			GraphicSetupPanel.this.c.hasModelsLoaded())
			GraphicSetupPanel.this.csp.getActiveTool().mouseDragged(e);
	}					

	/**
	 * A mouse motion command should just be passed along to the active tool.
	 * <p>
	 * Note that any mouse command is ignored if there are no <code>Models</code> loaded.
	 */
	public void mouseMoved(MouseEvent e)
	{
		if (c.getMode() != SETUP_MODE)
			return;
		if (GraphicSetupPanel.this.csp.getActiveTool() != null &&
			GraphicSetupPanel.this.c.hasModelsLoaded())
			GraphicSetupPanel.this.csp.getActiveTool().mouseMoved(e);						
	}
	
	/**
	 * A mouse entering command should just be passed along to the active tool.
	 * <p>
	 * Note that any mouse command is ignored if there are no <code>Models</code> loaded.
	 */
	public void mouseEntered(MouseEvent e)
	{
		if (c.getMode() != SETUP_MODE)
			return;
		if (GraphicSetupPanel.this.csp.getActiveTool() != null &&
			GraphicSetupPanel.this.c.hasModelsLoaded())
			GraphicSetupPanel.this.csp.getActiveTool().mouseEntered(e);
	}

	/**
	 * A mouse exiting command should just be passed along to the active tool.
	 * <p>
	 * Note that any mouse command is ignored if there are no <code>Models</code> loaded.
	 */
	public void mouseExited(MouseEvent e)
	{
		if (c.getMode() != SETUP_MODE)
			return;
		if (GraphicSetupPanel.this.csp.getActiveTool() != null &&
			GraphicSetupPanel.this.c.hasModelsLoaded())
			GraphicSetupPanel.this.csp.getActiveTool().mouseExited(e);
	}
}


/**
 * The <code>GraphicSetupPanel3D</code> is the 3D graphical component of the <code>CometsSetupPanel</code>.
 * This handles showing off the state of the currently loaded 3D world. 
 * <p> 
 * This class is set in the <code>CometsSetupPanel</code> file since that's the only class
 * that should directly interact with it. Any other interaction is done indirectly through 
 * Java's mouse interface.
 * @author Ilija Dukovski
 *
 */
@SuppressWarnings("serial")
class GraphicSetupPanel3D extends GLJPanel implements 
      CometsConstants,
      GLEventListener,
      MouseMotionListener,
      MouseListener
{
	private final int CANVAS_SIZE = 500;
	private Comets c;
	//private CometsSetupPanel csp;
	//private Shape selectionShape;
	private CometsParameters cParams;

	private double[] colorScale = {10.0, 10.0, 10.0};
	//private JPopupMenu popupMenu;
	//private Point //mouseDown,
	//			  clickPoint;
	//private List<Point> selectedSpaces;
	//private JRadioButton overButton,
	//					 modButton;
	//private ButtonGroup bgroup;
	
	//private JCheckBox barrierBox;
	//private JCheckBox[] biomassInBoxes,
	//					biomassOutBoxes,
	//					mediaInBoxes,
	//					mediaOutBoxes;

	    private boolean resetViewport = false;
	    private double dX = 0;
	    private double dY = 0;
	    private double startX, startY;
        private double[] horAxis={1.0,0.0,0.0};
        private double[] vertAxis={0.0,1.0,0.0};
	    
	    public GraphicSetupPanel3D(Comets c) {
	    	super();
	    	this.c = c;
	    	this.cParams = c.getParameters();
	    	
	        GLProfile glp = GLProfile.getDefault();
	        GLCapabilities caps = new GLCapabilities(glp);
	        GLJPanel canvas = new GLJPanel(caps);

	        canvas.setPreferredSize(new Dimension(CANVAS_SIZE,CANVAS_SIZE));
	        this.add(canvas);
	        this.setVisible(true);
	       
/*
	        frame.addWindowListener(new WindowAdapter() {
	            public void windowClosing(WindowEvent e) {
	                System.exit(0);
	            }
	        });
*/
	        canvas.addGLEventListener(this);
	        canvas.addMouseMotionListener(this);
	        canvas.addMouseListener(this);
	        
	    }

	    @Override
	    public void display(GLAutoDrawable drawable) {
	        render(drawable);
	    }

	    @Override
	    public void dispose(GLAutoDrawable drawable) {
	    }

	    @Override
	    public void init(GLAutoDrawable drawable) {
	    }
	    
	    @Override
	    public void reshape(GLAutoDrawable drawable, int x, int y, int w, int h) {
	    }

	    private void render(GLAutoDrawable drawable) {
	    	
	    	double maxDimension, cubeSize;
	    	
	        GL2 gl = drawable.getGL().getGL2();
	        //GLU glu = new GLU();
	        GLUT glut = new GLUT();
	        
	        maxDimension = Math.max((double)cParams.getNumCols(), (double)cParams.getNumRows());
	        maxDimension = Math.max(maxDimension, (double)cParams.getNumLayers());
	        cubeSize = (double) (1.0/(maxDimension-1.0));
	        
            gl.glMatrixMode(GL2.GL_MODELVIEW);
            gl.glClear(GL2.GL_COLOR_BUFFER_BIT);
            
            gl.glEnable(GL2.GL_DEPTH_TEST);
            gl.glClear(GL2.GL_DEPTH_BUFFER_BIT);
            gl.glClearDepth(0.0);
            gl.glDepthFunc(GL2.GL_GREATER);
            
            //gl.glEnable(GL2.GL_LIGHTING);
            float[] specular={1.0f,1.0f,1.0f,1.0f};
            float[] diffuse={1.0f,1.0f,1.0f,1.0f};
            float[] whiteLight={0.5f,0.5f,0.5f,1.0f};
            float[] ambient={1.0f,1.0f,1.0f,1.0f};
            float[] position={1f,1f,1f,1.0f};
            //gl.glLightfv(GL2.GL_LIGHT0,GL2.GL_DIFFUSE,diffuse,0);
            //gl.glLightfv(GL2.GL_LIGHT0,GL2.GL_SPECULAR,specular,0);
            //gl.glLightfv(GL2.GL_LIGHT0,GL2.GL_AMBIENT,whiteLight,0);
            //gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_POSITION, position, 0);
            //gl.glEnable(GL2.GL_LIGHT0);
            //gl.glEnable(GL2.GL_LIGHT1);
            
            //gl.glEnable(GL2.GL_COLOR_MATERIAL);
            //gl.glColorMaterial(GL2.GL_FRONT,GL2.GL_DIFFUSE);
            //gl.glColorMaterial(GL2.GL_FRONT,GL2.GL_SPECULAR);
            //gl.glColorMaterial(GL2.GL_FRONT,GL2.GL_AMBIENT);
            //gl.glMateriali(GL2.GL_FRONT, GL2.GL_SPECULAR, 128);
            //gl.glMateriali(GL2.GL_FRONT, GL2.GL_AMBIENT, 128);
            //gl.glMateriali(GL2.GL_FRONT, GL2.GL_DIFFUSE, 128);
            
            
            //gl.glEnable(GL2.GL_BLEND);
            //gl.glBlendFunc(GL2.GL_SRC_ALPHA,GL2.GL_ONE_MINUS_SRC_ALPHA);
            
	        if(resetViewport)
	        {
	        	gl.glLoadIdentity();
	            resetViewport=false;
	        }
	        else
	        {
	        	gl.glRotated(-dY, horAxis[0], horAxis[1], horAxis[2]);
	        	gl.glRotated( dX, vertAxis[0], vertAxis[1], vertAxis[2]);
	        }
	        
			if (c.getWorld3D() != null)
			{
				//double[] m = new double[3];
				double[] m={0.0,0.0,0.0};
				if (cParams.getColorRelative())
				{
					if (cParams.getDisplayLayer() == c.getWorld3D().getNumMedia())
					{
						for (int i=0; i<cParams.getNumCols(); i++)
						{
							for (int j=0; j<cParams.getNumRows(); j++)
							{
								for (int l=0; l<cParams.getNumLayers(); l++)
								{
									Cell cell = c.getWorld3D().getCellAt(i, j, l);
									if (cell != null)
									{
										double[] biomass = cell.getBiomass();
										//double[] sum = new double[3];
										double[] sum = {0.0,0.0,0.0};
										for (int k=0; k<biomass.length; k++)
										{
											sum[k%3] += biomass[k];
										}
										for (int k=0; k<3; k++)
										{
											if (sum[k] > m[k])
												m[k] = sum[k];
										}
									}
								}
							}
						}
					}
					else
						m[0] = Utility.max(c.getWorld().getAllMedia(), cParams.getDisplayLayer());
				}
				else
					m = colorScale;
				
				for (int i=0; i<cParams.getNumCols(); i++)
				{
					for (int j=0; j<cParams.getNumRows(); j++)
					{
						for (int l=0; l<cParams.getNumLayers(); l++)
						{

							Color color = currentWorld3DColor(i, j, l, m);
							if (color != null)
							{   
								double x = -0.5+((double)i)/(cParams.getNumCols()-1.0);
								double y = -0.5+((double)j)/(cParams.getNumRows()-1.0);
								double z = -0.5+((double)l)/(cParams.getNumLayers()-1.0);
								
			   					double red   = ((double)color.getRed())/255.0;
			   					double green = ((double)color.getGreen())/255.0;
			   					double blue  = ((double)color.getBlue())/255.0;
			   					//double alpha = (red+green+blue);
			   					double norm = Math.sqrt(red*red+green*green+blue*blue);
			   					
			   					if(color.getRed()!=0 || color.getGreen()!=0 || color.getBlue()!=0)
			   					{
			   							red   /=norm;
			   							green /=norm;
			   							blue  /=norm;
			   							gl.glColor3d(red, green, blue);
			   							gl.glPushMatrix();
			   							gl.glTranslated(x,y,z);
			   							glut.glutSolidCube((float) cubeSize);
			   							gl.glColor3d(0.0, 0.0, 0.0);
			   							glut.glutWireCube((float) cubeSize);
			   							gl.glPopMatrix();
			   					}
							}
						}
					}

				}

			}
			gl.glDisable(GL2.GL_DEPTH_TEST);
	    }

		@Override
		public void mouseDragged(MouseEvent arg0) {
			// TODO Auto-generated method stub
			dX = -(startX-arg0.getX())/10.0;
			dY = (startY-arg0.getY())/10.0;
			display();
			startX=arg0.getX();
			startY=arg0.getY();
		}

		@Override
		public void mouseMoved(MouseEvent arg0) {
			// TODO Auto-generated method stub			
		}

		@Override
		public void mouseClicked(MouseEvent arg0) {
			// TODO Auto-generated method stub
			if(arg0.getClickCount()==2)
			{   
				resetViewport = true;
				display();
			}
		}

		@Override
		public void mouseEntered(MouseEvent arg0) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void mouseExited(MouseEvent arg0) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void mousePressed(MouseEvent arg0) {
			startX=arg0.getX();
			startY=arg0.getY();
			
		}

		@Override
		public void mouseReleased(MouseEvent arg0) {
			// TODO Auto-generated method stub
			
		}

		/**
		 * Gets the current color of the world at space (x, y), relative to the maximum
		 * values given in the m array.
		 * @param x - x-coordinate of the world
		 * @param y - y-coordinate of the world
		 * @param m - a 3-element array of double values, used as a coloring threshold.
		 * @return the scaled color of the world as a packed 32-bit integer - like what Processing
		 * uses for colors.
		 */
		public Color currentWorld3DColor(int x, int y, int z, double[] m)
		{
			if (c.getWorld3D().isBarrier(x, y, z))
				return new Color(cParams.getBarrierColor());

			Color col = null;
			if (cParams.getDisplayLayer() == c.getWorld3D().getNumMedia()) // if it's numMedia, then display cell concs.
			{
				Cell cell = c.getWorld3D().getCellAt(x, y, z);
				if (cell != null)
				{
					double[] biomass = cell.getBiomass();
					double[] channels = {0.0,0.0,0.0};
					double biomassSum=0.0;
					for (int i=0; i<biomass.length; i++)
					{ 
						//channels[i % 3] += biomass[i];
						biomassSum+=biomass[i];
						if(biomass.length==1 || biomass.length == 2)
						{
							channels[i % 3] += biomass[i];
						}
						else
						{
						    if(i<(int)(biomass.length/2))
						    {
							    channels[2] += 0;
							    channels[1] += biomass[i]*Math.sin(Math.PI*i/(biomass.length-1)+Math.PI/2.0);
							    channels[0] += biomass[i]*Math.sin(Math.PI*i/(biomass.length-1));
						    }
						    else 
						    {
							    channels[1] += 0;
							    channels[2] += biomass[i]*Math.sin(Math.PI*i/(biomass.length-1)-Math.PI/2.0);
							    channels[0] += biomass[i]*Math.sin(Math.PI*i/(biomass.length-1));
						    }
						}
					}
					col = new Color((int)Math.min((channels[1]*(255/biomassSum)), 255), (int)Math.min((channels[0]*(255/biomassSum)), 255),  (int)Math.min((channels[2]*(255/biomassSum)), 255));
					//col = new Color((int)Math.min((channels[1]*(255/m[1])), 255), (int)Math.min((channels[0]*(255/m[0])), 255),  (int)Math.min((channels[2]*(255/m[2])), 255));
				}
			}
			else
			{
				//	      double m = util.max(media, displayToggle);
				double[] media = c.getWorld3D().getMediaAt(x, y, z);
				col = new Color((int)Math.min(255, media[cParams.getDisplayLayer()]*(255/m[0])), 0, 0);
			}
			return col;
		}
		
		/**
		 * Sets the color style to be shown. If <code>isRelative</code> is true, then the
		 * relative color style is used (e.g. color saturation is all spaces is relative to the
		 * space with the highest concentration), and if false then the absolute color style is 
		 * used (in this case, all colors are a function of the passed <code>colorValue</code>
		 * variable and not the most dense space). 
		 * @param isRelative
		 * @param colorValue
		 */
		public void setColorStyle(boolean isRelative, double colorValue)
		{
			cParams.setColorRelative(isRelative);
//			colorRelative = isRelative;
			colorScale = new double[]{ colorValue, colorValue, colorValue };
			repaint();
		}

}


