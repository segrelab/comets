package edu.bu.segrelab.comets.ui.tools;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;

import edu.bu.segrelab.comets.CometsParameters;
import edu.bu.segrelab.comets.ui.CometsSetupPanel;
import edu.bu.segrelab.comets.ui.CometsToolbarPanel;

/**
 * The <code>SelectTool</code> allows the user to do two main tasks:<br>
 * 1. Select a (relatively) arbitrary set of spaces in the world.<br>
 * 2. Modify all of those spaces at the same time - add or subtract media or biomass, and set
 * or unset barrier status on those spaces.
 * <p>
 * This is used by clicking and dragging to create a box or ellipse, which can optionally be
 * inverted. The user can then right click on the shape to adjust settings on all selected
 * spaces.
 * @author Bill Riehl briehl@bu.edu
 */
public class SelectTool extends AbstractTool
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -1588005296681695065L;

	private int style = CometsSetupPanel.BOX_STYLE;
	
	private List<Point> selectedSpaces;
	private Point mouseDown;
	private Shape selectionShape;
//				  addonShape;

	/**
	 * Initializes a new <code>SelectTool</code> to work with a given <code>CometsSetupPanel</code>,
	 * <code>CometsToolbarPanel</code>, and <code>CometsParameters</code>.
	 * @param csp - the given <code>CometsSetupPanel</code>
	 * @param ctp - a <code>CometsToolbarPanel</code>
	 * @param cParams - a <code>CometsParameters</code> instance
	 */
	public SelectTool(CometsSetupPanel csp, CometsToolbarPanel ctp, CometsParameters cParams)
	{
		super(csp, ctp, cParams);
		setText("Select");
		selectedSpaces = new ArrayList<Point>();
	}

	/**
	 * Builds a control panel for this tool. This panel is composed of two sets of buttons. The
	 * first is a choice for what kind of selection tool to use: either box, circle, or lasso 
	 * (LASSO NOT FINISHED). The second set is a couple of simple options. The "Clear" button will
	 * clear the selection, and the "Invert" button will invert the selection: any nonselected spaces
	 * become selected while selected spaces become deselected.
	 */
	protected void buildControlPanel()
	{
		controlPanel = new JPanel(new GridBagLayout());
		controlPanel.setBorder(BorderFactory.createTitledBorder("Select Options"));

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.BOTH;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;

		final JToggleButton[] styleButtons = new JToggleButton[3];
		styleButtons[0] = new JToggleButton("b");
		styleButtons[0].setSelected(true);
		styleButtons[1] = new JToggleButton("c");
		styleButtons[2] = new JToggleButton("l");
		styleButtons[2].setEnabled(false);

		for (int i=0; i<styleButtons.length; i++)
		{
			final JToggleButton sButton = styleButtons[i];
			sButton.addActionListener(
					new ActionListener() {
						public void actionPerformed(ActionEvent e)
						{
							for (int j=0; j<styleButtons.length; j++)
								styleButtons[j].setSelected(false);
							sButton.setSelected(true);
							if (sButton.getText().equals("b"))
								style = CometsSetupPanel.BOX_STYLE;
							else if (sButton.getText().equals("c"))
								style = CometsSetupPanel.CIRCLE_STYLE;
							else
								style = CometsSetupPanel.BOX_STYLE; //LASSO_STYLE;
						}
					});
		}

		
		JButton invertButton = new JButton("invert");
		invertButton.addActionListener(
				new ActionListener() {
					public void actionPerformed(ActionEvent e)
					{
						invertSelection();
					}
				});

		JButton clearButton = new JButton("clear");
		clearButton.addActionListener(
				new ActionListener() {
					public void actionPerformed(ActionEvent e)
					{
						selectedSpaces.clear();
						selectionShape = null;
						SelectTool.this.csp.setSelectionShape(null);
					}
				});
		
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.insets = new Insets(0,0,5,0);
		for (int i=0; i<styleButtons.length; i++)
		{
			gbc.gridx = i;
			controlPanel.add(styleButtons[i], gbc);
		}
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.gridx = 0;
		gbc.gridy = GridBagConstraints.RELATIVE;
		controlPanel.add(new JSeparator(SwingConstants.HORIZONTAL), gbc);
		gbc.insets = new Insets(0,0,0,0);
		controlPanel.add(invertButton, gbc);
		controlPanel.add(clearButton, gbc);
	}
	
	@Override
	/**
	 * Required by MouseListener - not used
	 */
	public void mouseClicked(MouseEvent e)
	{
	}

	@Override
	/**
	 * Required by MouseListener - not used
	 */
	public void mouseEntered(MouseEvent e)
	{
	}

	@Override
	/**
	 * Required by MouseListener - not used
	 */
	public void mouseExited(MouseEvent e)
	{
	}

	@Override
	/**
	 * When the 1st mouse button (left-click on Windows systems) is pressed, the selection is
	 * reset to a new empty shape, and any selected spaces are cleared out.
	 */
	public void mousePressed(MouseEvent e)
	{
		if ((e.getModifiersEx() & InputEvent.BUTTON1_DOWN_MASK) == 0)
			return;

		selectedSpaces = new ArrayList<Point>(); 
		mouseDown = e.getPoint();
		if (mouseDown.getX() > cParams.getPixelScale() * cParams.getNumCols() - 1)
			mouseDown.setLocation(cParams.getPixelScale() * cParams.getNumCols() - 1, mouseDown.getY());
		if (mouseDown.getY() > cParams.getPixelScale() * cParams.getNumRows() - 1)
			mouseDown.setLocation(mouseDown.getX(), cParams.getPixelScale() * cParams.getNumRows() - 1);
		
		//selectionShape = new Area();
		switch(style)
		{
			case CometsSetupPanel.BOX_STYLE:
				selectionShape = new Rectangle2D.Double(mouseDown.getX(), mouseDown.getY(), 0, 0);
				break;
			case CometsSetupPanel.CIRCLE_STYLE:
				selectionShape = new Ellipse2D.Double(mouseDown.getX(), mouseDown.getY(), 0, 0);
				break;
			default:
				selectionShape = null;
				break;
		}
		csp.selectSpaces(selectedSpaces);
		csp.setSelectionShape(selectionShape);
		csp.repaint();
	}

	@Override
	/**
	 * When the mouse is released, all the spaces that lie in the selection shape are added to
	 * a list of selected spaces owned by the <code>CometsSetupPanel</code>.
	 */
	public void mouseReleased(MouseEvent e)
	{
		if (selectionShape == null)
			return;
		
		if (selectionShape.getBounds().getWidth() == 0 &&
			selectionShape.getBounds().getHeight() == 0)
		{
			selectionShape = null;
			csp.setSelectionShape(null);
		}
		else
		{
			ctp.snapShapeToGrid(selectionShape, style);
			selectedSpaces = ctp.getShapePoints(selectionShape, false);
			csp.selectSpaces(selectedSpaces);
		}
	}

	@Override
	/**
	 * When the mouse is dragged, the selection shape is updated. A shape is made between
	 * the point that the user initially clicked down and the current point given by the 
	 * <code>MouseEvent</code>. This shape is automatically snapped to the grid - only 
	 * entire grid spaces should be selected.
	 */
	public void mouseDragged(MouseEvent e)
	{
		if ((e.getModifiersEx() & InputEvent.BUTTON1_DOWN_MASK) == 0)
			return;

		// set up new rectangle.
		Point mouseDrag = e.getPoint();
		
		double downX = mouseDown.getX();
		double downY = mouseDown.getY();
		double hereX = mouseDrag.getX();
		double hereY = mouseDrag.getY();
		
		if (hereX < 0)
			hereX = 0;
		else if (hereX > cParams.getPixelScale() * cParams.getNumCols())
			hereX = cParams.getPixelScale() * cParams.getNumCols();
		
		if (hereY < 0)
			hereY = 0;
		else if (hereY > cParams.getPixelScale() * cParams.getNumRows())
			hereY = cParams.getPixelScale() * cParams.getNumRows();
		
		double newX = Math.min(downX, hereX);
		double newY = Math.min(downY, hereY);
		double width = Math.abs(hereX - downX);
		double height = Math.abs(hereY - downY);
		
		switch(style)
		{
			case CometsSetupPanel.BOX_STYLE:
				((Rectangle2D.Double)selectionShape).setRect(newX, newY, width, height);
				break;
			case CometsSetupPanel.CIRCLE_STYLE:
				((Ellipse2D.Double)selectionShape).setFrame(newX, newY, width, height);
				break;
			default:
				break;
		}
		ctp.snapShapeToGrid(selectionShape, style);
	}

	@Override
	/**
	 * Required by MouseMotionListener - not used
	 */
	public void mouseMoved(MouseEvent e) { }

	/**
	 * Inverts the selection. This uses a little trick - a <code>java.awt.geom.Area</code>
	 * object is made around the entire screen, and the current selection shape is substracted
	 * from it - leaving an inverted selection.
	 */
	private void invertSelection()
	{
		Area fullScreen = new Area(new Rectangle2D.Double(0, 0,
										cParams.getPixelScale()*(cParams.getNumCols()), 
										cParams.getPixelScale()*(cParams.getNumRows())));
										
		if (selectionShape != null)
		{
			Area origin = new Area(selectionShape);
			fullScreen.subtract(origin);
		}
		selectionShape = fullScreen;
		csp.setSelectionShape(selectionShape);
		selectedSpaces = ctp.getShapePoints(selectionShape, false);
		csp.selectSpaces(selectedSpaces);
	}
}
