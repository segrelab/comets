package edu.bu.segrelab.comets.ui;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;


import edu.bu.segrelab.comets.Comets;
import edu.bu.segrelab.comets.event.CometsChangeEvent;
import edu.bu.segrelab.comets.event.CometsChangeListener;
import edu.bu.segrelab.comets.ui.tools.AbstractTool;
import edu.bu.segrelab.comets.ui.tools.EraserTool;
import edu.bu.segrelab.comets.ui.tools.FillTool;
import edu.bu.segrelab.comets.ui.tools.GradientTool;
import edu.bu.segrelab.comets.ui.tools.LineTool;
import edu.bu.segrelab.comets.ui.tools.PencilTool;
import edu.bu.segrelab.comets.ui.tools.PipetTool;
import edu.bu.segrelab.comets.ui.tools.SelectTool;
import edu.bu.segrelab.comets.ui.tools.ShapeTool;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;


/**
 * This implements a toolbar for use with the <code>CometsSetupPanel</code>. The toolbar
 * is designed to be analogous to tool palettes common to paint programs and Photoshop, for
 * example. The idea here is that the user can use these tools to "paint" the world with
 * biomass, media, and barrier spaces.
 * <p>
 * A series of tools (extensions of <code>AbstractTool</code>) is instantiated and linked to
 * a set of buttons that reside in the main section of the toolbar. Each of these tools
 * has its own control panel (as required by the <code>AbstractTool</code> which gets
 * temporarily added to the toolbar when the user selects each tool.
 * <p> 
 * Many of these tools implement some kind of mechanic for adding biomass, media, etc. to the
 * world through interacting with the <code>CometsSetupPanel</code>. The content added is saved
 * in the <code>CometsToolbarPanel</code> for use across all such tools.
 * 
 * @see AbstractTool
 * @see EraserTool
 * @see FillTool
 * @see GradientTool
 * @see PencilTool
 * @see PipetTool
 * @see SelectTool
 * @see ShapeTool
 * @author Bill Riehl briehl@bu.edu
 *
 */
@SuppressWarnings("serial")
public class CometsToolbarPanel extends JPanel
{
	private Comets c;
	private CometsSetupPanel csp;
	private JPanel layoutToolbar;
//				   toolControlPanel;
	private boolean showLayoutToolbar,
					barrier = false,
					useRefresh = false;
	private GridBagConstraints cpc; //"control panel constraints" - where the control panel should go.
//	private int toolRows = 2;
	
	private AbstractTool activeTool;
	private List<AbstractTool> layoutTools;
	
	private DataContentPanel biomassPanel,
							 mediaPanel,
							 mediaRefreshPanel;
	public static final int MIN_STROKE = 1;
	public static final int MAX_STROKE = 10;
	
	
	/**
	 * Creates a new <code>CometsToolbarPanel</code> linked to the given <code>CometsSetupPanel</code>
	 * and <code>Comets</code> objects. This automatically initializes the set of tools to be
	 * used and sets the <code>SelectTool</code> to be initially active.
	 * @param c - the main <code>Comets</code> object.
	 * @param csp - the associated <code>CometsSetupPanel</code>
	 */
	public CometsToolbarPanel(Comets c, CometsSetupPanel csp)
	{
		super();
		setBorder(BorderFactory.createTitledBorder("Layout Tools"));
		this.c = c;
		this.csp = csp;
		layoutTools = new ArrayList<AbstractTool>();
		
		cpc = new GridBagConstraints();
		cpc.fill = GridBagConstraints.BOTH;
		cpc.gridx = 0;
		cpc.gridy = 0;
		cpc.gridwidth = GridBagConstraints.REMAINDER;
		setLayout(new GridBagLayout());

		layoutToolbar = buildLayoutToolbar();
		add(layoutToolbar, cpc);
		cpc.gridy = 1;
		cpc.gridheight = GridBagConstraints.REMAINDER;
		
		/* Stack all the control panels on top of each other in the layout,
		 * and only set the active one to be visible.
		 * I'm honestly not sure if this is a kosher solution or not - if its
		 * "good Java style" or whatever - but f it, it works.
		 */
		for (int i=0; i<layoutTools.size(); i++)
		{
			add(layoutTools.get(i).getControlPanel(), cpc);
			layoutTools.get(i).getControlPanel().setVisible(false);
		}
		
		// initially, the selection tool is selected... first one in the list.
		((AbstractTool)layoutTools.get(0)).setSelected(true);
		setActiveTool((AbstractTool)layoutTools.get(0));
	}

	/**
	 * This removes any previously set biomass and media content that was set by the 
	 * user, resetting it to be all zeros.
	 */
	public void clearContent()
	{
		biomassPanel = null;
		mediaPanel = null;
		mediaRefreshPanel = null;
		Iterator<AbstractTool> it = layoutTools.iterator();
		while (it.hasNext())
		{
			AbstractTool tool = it.next();
			tool.reset();
		}
	}
	
	/**
	 * This returns a <code>DataContentPanel</code> with biomass information fetched from
	 * the <code>Comets</code> object. This panel has text fields that allow the user to
	 * set how much of each biomass should be added.
	 * @return a <code>DataContentPanel</code> with biomass information.
	 */
	public DataContentPanel getBiomassPanel()
	{
		if (c.getModels() == null || c.getModels().length == 0)
			return null;
		if (biomassPanel == null)
			setupBiomassPanel();
		return biomassPanel;
	}
	
	/**
	 * This sets up the internal <code>DataContentPanel</code> that stores information about
	 * the biomass content that should be placed into the world by various tools (line, shape,
	 * pencil, etc.).
	 * <p>
	 * It fetches all the information it needs from the <code>Models</code> that are loaded
	 * into the <code>Comets</code> object.
	 */
	private void setupBiomassPanel()
	{
		if (c.getModels() == null || c.getModels().length == 0)
			return;
		String[] names = c.getModelNames();
		for (int i=0; i<names.length; i++)
		{
			if (names[i] == null || names[i].length() == 0)
				names[i] = "Model " + i + ": ";
		}
		biomassPanel = new DataContentPanel(names, new String[]{"grams"}, false, null);
		biomassPanel.setBorder(BorderFactory.createTitledBorder("Biomass"));
	}
	
	/**
	 * This returns a <code>DataContentPanel</code> with media information fetched from
	 * the <code>Comets</code> object. This panel has text fields that allow the user to
	 * set how much of each nutrient should be added.
	 * @return a <code>DataContentPanel</code> with media information.
	 */
	public DataContentPanel getMediaPanel()
	{
		if (c.getModels() == null || c.getModels().length == 0)
			return null;
		if (mediaPanel == null)
			setupMediaPanel();
		return mediaPanel;
	}

	public DataContentPanel getMediaRefreshPanel()
	{
		if (c.getModels() == null || c.getModels().length == 0)
			return null;
		if (mediaRefreshPanel == null)
			setupMediaRefreshPanel();
		return mediaRefreshPanel;
	}
	
	public boolean useMediaRefresh()
	{
		return useRefresh;
	}
	
	public void setMediaRefresh(boolean b)
	{
		useRefresh = b;
	}
	
	/**
	 * This sets up the internal <code>DataContentPanel</code> that stores information about
	 * the media content that should be placed into the world by various tools (line, shape,
	 * pencil, etc.).
	 * <p>
	 * It fetches all the information it needs from the <code>Models</code> that are loaded
	 * into the <code>Comets</code> object.
	 */
	private void setupMediaPanel()
	{
		if (c.getModels() == null || c.getModels().length == 0)
			return;
		
		mediaPanel = new DataContentPanel(c.getMediaNames(), new String[]{"mmol"}, true, new boolean[c.getMediaNames().length]);
//		mediaPanel.setBorder(BorderFactory.createTitledBorder("Media"));
	}

	private void setupMediaRefreshPanel()
	{
		if (c.getModels() == null || c.getModels().length == 0)
			return;
		
		mediaRefreshPanel = new DataContentPanel(c.getMediaNames(), new String[]{"mmol"}, false, null);
//		mediaRefreshPanel.setBorder(BorderFactory.createTitledBorder("Media Refresh"));
	}
	
	/**
	 * This sets all tools' content to add (or remove) a barrier from the next place it's
	 * used on the <code>CometsSetupPanel</code> 
	 * @param b - if true, the next place a tool is used will become a barrier
	 */
	public void setBarrier(boolean b)
	{
		barrier = b;
	}
	
	/**
	 * Returns the state of each tool with regard to placing barriers in the world.
	 * @return true if the next tool use will place a barrier in the world.
	 */
	public boolean getBarrier()
	{
		return barrier;
	}

	/**
	 * This builds and shows a model dialog that allows the user to change the biomass and
	 * media content that will be added to the world when various tools are used. Each of these
	 * settings are dictated by what <code>Models</code> and how many of them are loaded.
	 */
	public void showContentSettings()
	{
		if (c.getModels() == null || c.getModels().length == 0)
		{
			JOptionPane.showMessageDialog(csp, new JLabel("No models loaded!"), "Error!", JOptionPane.ERROR_MESSAGE);
			return;
		}

		if (biomassPanel == null)
			setupBiomassPanel();
		if (mediaPanel == null)
			setupMediaPanel();
		if (mediaRefreshPanel == null)
			setupMediaRefreshPanel();
		
		final JTabbedPane mediaTabPane = new JTabbedPane();
		
		if (c.getWorld().getNumMedia() > 8)
		{
			JScrollPane mediaScroll = new JScrollPane(mediaPanel);
			mediaScroll.setPreferredSize(new Dimension(300, 300));
			//panel.add(mediaScroll);
			mediaTabPane.add("Current Media", mediaScroll);
			
			JScrollPane refreshScroll = new JScrollPane(mediaRefreshPanel);
			refreshScroll.setPreferredSize(new Dimension(200, 300));
			mediaTabPane.add("Media Refresh", refreshScroll);
		}
		else
		{
			//panel.add(mediaPanel);
			mediaTabPane.add("Current Media", mediaPanel);
			mediaTabPane.add("Media Refresh", mediaRefreshPanel);
		}

		final JCheckBox refreshMediaBox = new JCheckBox("Refresh Media");
		refreshMediaBox.setSelected(useRefresh);
		refreshMediaBox.addItemListener(
				new ItemListener() {
					public void itemStateChanged(ItemEvent e)
					{
						useRefresh = !useRefresh;
						mediaRefreshPanel.setEnabled(useRefresh);
						mediaTabPane.setEnabledAt(1, useRefresh);
					}
				});

		JCheckBox barrierBox = new JCheckBox("Set Barrier");
		barrierBox.setSelected(barrier);
		barrierBox.addItemListener(
				new ItemListener() {
					public void itemStateChanged(ItemEvent e)
					{
						barrier = !barrier;
						biomassPanel.setEnabled(!barrier);
						mediaPanel.setEnabled(!barrier);
						mediaRefreshPanel.setEnabled(!barrier && useRefresh);
						refreshMediaBox.setEnabled(!barrier);
						mediaTabPane.setEnabledAt(1, !barrier && useRefresh);
						mediaTabPane.setEnabledAt(0, !barrier);
					}
				});
		biomassPanel.setEnabled(!barrier);
		mediaPanel.setEnabled(!barrier);
		mediaRefreshPanel.setEnabled(useRefresh && !barrier);
		mediaTabPane.setEnabledAt(1, useRefresh && !barrier);
		mediaTabPane.setEnabledAt(0, !barrier);

		
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.add(barrierBox);
		panel.add(refreshMediaBox);

		if (c.getModels().length > 8)
		{
			JScrollPane scrollPane = new JScrollPane(biomassPanel);
			scrollPane.setPreferredSize(new Dimension(200, 300));
			panel.add(scrollPane);
		}
		else
			panel.add(biomassPanel);

		panel.add(mediaTabPane);

		boolean curBarrier = barrier;
		boolean curRefresh = useRefresh;
		int result = JOptionPane.showConfirmDialog(csp, 
												   panel, 
												   "Set tool content", 
												   JOptionPane.OK_CANCEL_OPTION, 
												   JOptionPane.PLAIN_MESSAGE);
		if (result == JOptionPane.OK_OPTION)
		{
			biomassPanel.updateValues();
			mediaPanel.updateValues();
			mediaRefreshPanel.updateValues();
		}
		else if (result == JOptionPane.CANCEL_OPTION)
		{
			biomassPanel.resetValues();
			mediaPanel.resetValues();
			mediaRefreshPanel.resetValues();
			useRefresh = curRefresh;
			barrier = curBarrier;
		}
	}
	
	/**
	 * Initializes the toolbar panel. This instantiates all the tools used in the panel
	 * and links them to a set of buttons so that only one button can be selected at a time.
	 * Selecting a button will also clear the buttons control panel from the toolbar and replace
	 * it with a new one.
	 * @return a <code>JPanel</code> holding the contents of the toolbar.
	 */
	private JPanel buildLayoutToolbar()
	{
		JPanel panel = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.BOTH;
		gbc.gridheight = 1;
		gbc.gridwidth = 1;

		gbc.gridx = 0;
		gbc.gridy = 0;
		
		//init a bunch of buttons
		layoutTools.add(new SelectTool(csp, this, c.getParameters()));
		layoutTools.add(new EraserTool(csp, this, c.getParameters()));
		layoutTools.add(new LineTool(csp, this, c.getParameters()));
		layoutTools.add(new PencilTool(csp, this, c.getParameters()));
		layoutTools.add(new ShapeTool(csp, this, c.getParameters()));
		layoutTools.add(new PipetTool(csp, this, c));
		layoutTools.add(new FillTool(csp, this, c.getParameters()));
		layoutTools.add(new GradientTool(csp, this, c));
		
		for (int i=0; i<layoutTools.size(); i++)
		{
			final AbstractTool tool = (AbstractTool)layoutTools.get(i);
			/*
			 * Whenever a tool is clicked, the active tool should be deactivated,
			 * and the one clicked should become active. This also entails
			 * resetting the control panel, but that's handled by setActiveTool().
			 */
			tool.addActionListener(
					new ActionListener() {
						public void actionPerformed(ActionEvent e)
						{
							if (activeTool != null)
								setActiveTool(null);
							tool.setSelected(true);
							if (tool.isSelected())
								setActiveTool(tool);
						}
					});
			tool.addCometsChangeListener(
					new CometsChangeListener() {
						public void cometsChangePerformed(CometsChangeEvent e)
						{
							c.backupState(true);
						}
					});
			// Add the tool to the right place. Should have 2 tools per row.
			gbc.gridx = i % 2;
			gbc.gridy = (int)(Math.floor(i/2));
			panel.add(tool, gbc);
			tool.getControlPanel().setVisible(false);
		}
		
//		panel.setBorder(BorderFactory.createTitledBorder("Layout Tools"));
		return panel;
	}
	
	/**
	 * Shows or hides the layout toolbar.
	 * @param b - if true, shows the toolbar, otherwise hides it.
	 */
	public void showLayoutToolbar(boolean b)
	{
		showLayoutToolbar = b;
		setVisible(showLayoutToolbar);
	}
	
	/**
	 * Returns the currently active tool as an <code>AbstractTool</code>. So if you know
	 * what tool you're grabbing, you'll have to cast it before calling tool-specific
	 * methods.
	 * @return the currently selected <code>AbstractTool</code>
	 */
	public AbstractTool getActiveTool() 
	{
		return activeTool; 
	}

	/**
	 * Sets the currently active tool. This maintains a pointer to the active tool as well as
	 * making the tool's control panel visible below the rest of the toolbar.
	 * @param tool - the <code>AbstractTool</code> to activate.
	 */
	private void setActiveTool(AbstractTool tool) 
	{
		// deactivate the currently active tool, if there is one, and hide
		// its control panel
		if (activeTool != tool && activeTool != null)
		{
			activeTool.setSelected(false);
			activeTool.getControlPanel().setVisible(false);
		}

		activeTool = tool;
		
		if (activeTool != null)
		{
			activeTool.getControlPanel().setVisible(true);
		}
		csp.revalidate();
		revalidate();
		CometsToolbarPanel.this.revalidate();
	}
	
	/**
	 * One of the problems with having a world full of arbitrarily sized boxes that represent
	 * overgrown pixels is that any shape used to draw on that world might take up a fraction
	 * of a space. We can't really select only part of a space, or only put biomass in a piece
	 * of a space, so to make it easier for the user to see what's happening, we tweak the 
	 * drawn shape to fully encapsulate grid spaces.
	 * <p>
	 * This method uses the dimensions of the shape and the size of each grid space to adjust
	 * the shape so that it "snaps" around individual grid spaces.
	 * <p>
	 * This works best, obviously, for rectangular shapes. Circular shapes should be turned into
	 * kinda pixelated round things, but for now they remain round, except that their bounding
	 * rectangles are snapped to the grid.
	 * @param s - the shape to be snapped to the grid
	 * @param style - the style of the shape: one of <code>CometsSetupPanel.BOX_STYLE</code>, 
	 * <code>CometsSetupPanel.CIRCLE_STYLE</code> or <code>CometsSetupPanel.LINE_STYLE</code>.
	 */
	public void snapShapeToGrid(Shape s, int style)
	{
		double scale = c.getParameters().getPixelScale();
		//snap to grid, or something.
		if (style != CometsSetupPanel.LINE_STYLE)
		{
			double newX = Math.floor(s.getBounds().getX() / scale) * scale;
			double newY = Math.floor(s.getBounds().getY() / scale) * scale;
			double newW = (Math.floor((s.getBounds().getX() + s.getBounds().getWidth()) / scale)) * scale - newX;
			double newH = (Math.floor((s.getBounds().getY() + s.getBounds().getHeight()) / scale)) * scale - newY;
		
			switch(style)
			{
				case CometsSetupPanel.BOX_STYLE:
					((Rectangle2D.Double)s).setRect(newX, newY, newW, newH);
					break;
				case CometsSetupPanel.CIRCLE_STYLE:
					/* Circles should be reformed into pixelates messes that clearly
					 * show where their bounds wrap around spaces. But for now, just
					 * twiddle their bounding rectangles to fit the grid.
					 */
					((Ellipse2D.Double)s).setFrame(new Rectangle2D.Double(newX, newY, newW, newH));
					break;
				default:
					break;
			}
		}
		else
		{
			// do something special for lines. not sure what yet, exactly...
			// leave it alone for now. :P
		}
		csp.repaint();
	}

	/**
	 * Builds and returns an <code>List</code> of <code>Points</code> corresponding to
	 * the world spaces whose center is found within the given <code>Shape</code> (this isn't
	 * the best way, but works as a good heuristic for those pesky round shapes). Alternately,
	 * if the intersect parameter is set to true, those spaces that are intersected by the 
	 * <code>Shape</code> are returned - useful for using a thin line to select spaces.
	 * @param s - spaces contained by this <code>Shape</code> are returned.
	 * @param intersect - if set to true, then any space intersected by the shape is returned.
	 * @return an <code>List</code> of <code>Point</code>s contained by the <code>Shape</code> 
	 */
	public List<Point> getShapePoints(Shape s, boolean intersect)
	{
		List<Point> points = new ArrayList<Point>();
		/* not sure how else to do this effectively for all wacky shapes.
		 *
		 * iterate over all spaces on the grid.
		 * if they're present in the Shape (Shape.contains(x, y)), select 'em.
		 * For something so painfully inelegant, it works pretty snappy.
		 */
		int scale = c.getParameters().getPixelScale(); // just because i don't want to fetch it 8 hojillion times.
		for (int i=0; i<c.getParameters().getNumCols(); i++)
		{
			for (int j=0; j<c.getParameters().getNumRows(); j++)
			{
				if (intersect)
				{
					if (s.intersects(i*scale, j*scale, scale, scale))
						points.add(new Point(i, j));					
				}
//				else if (s.contains(i*scale, j*scale, scale, scale))
				else if (s.contains(i*scale + scale/2, j*scale + scale/2))
				{
					points.add(new Point(i, j));
				}
			}
		}
		return points;
	}
}
