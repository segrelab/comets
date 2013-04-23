package edu.bu.segrelab.comets.ui.tools;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;


import edu.bu.segrelab.comets.CometsParameters;
import edu.bu.segrelab.comets.ui.CometsSetupPanel;
import edu.bu.segrelab.comets.ui.CometsToolbarPanel;

/**
 * The <code>ShapeTool</code> is a part of the tool palette used in modifying the initial
 * layout in the <code>CometsSetupPanel</code>.
 * <p>
 * With this tool selected the user can draw a shape of biomass and media on the layout panel.
 * This is done by clicking and dragging to select the area covered by the shape, then releasing
 * to populate that shape with biomass or media.
 * <p>
 * This tool's control panel allows the user to make either a square or circular shape, with 
 * stroke width between 1 and 10 spaces, with this choice of whether or not to fill the shape.
 * @author Bill Riehl briehl@bu.edu
 */
public class ShapeTool extends AbstractTool
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 8318987001890974632L;
	private int style,
				strokeWidth;
	private boolean fill = false;
	private Shape shape;			// if we fill it in, use this variable
	private Area area;				// if NOT filling it in, this is the difference
									// between the shape and a gap in the middle, leaving
									// enough for a proper stroke width.
	private Point mouseDown;

	/**
	 * Initializes a new <code>ShapeTool</code> linked to a given <code>CometsSetupPanel</code>,
	 * set in a given <code>CometsToolbarPanel</code>, and using a set of given
	 * <code>CometsParameters</code>
	 * @param csp - a <code>CometsSetupPanel</code>
	 * @param ctp - a <code>CometsToolbarPanel</code>
	 * @param cParams - a <code>CometsParameters</code>
	 */
	public ShapeTool(CometsSetupPanel csp, CometsToolbarPanel ctp, CometsParameters cParams)
	{
		super(csp, ctp, cParams);
		setText("Shape");
		style = CometsSetupPanel.BOX_STYLE;
		strokeWidth = cParams.getPixelScale();
	}
	
	/**
	 * Constructs a control panel for the <code>ShapeTool</code>. This has four main options:<br>
	 * 1. Make either a box (b) or circle (c) shape.<br>
	 * 2. A checkbox for choosing to fill the box or leave it as a line.
	 * 3. A <code>JSlider</code> for setting the width of the stroke - the line outlining 
	 * the shape. This pretty much irrelevant if the fill box is checked.
	 * 4. A button to set the content of the shape.
	 */
	protected void buildControlPanel()
	{
		controlPanel = new JPanel(new GridBagLayout());
		controlPanel.setBorder(BorderFactory.createTitledBorder("Shape Options"));

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.BOTH;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;

		final JToggleButton[] styleButtons = new JToggleButton[2];
		styleButtons[0] = new JToggleButton("b");
		styleButtons[1] = new JToggleButton("c");
		styleButtons[0].setSelected(true);
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
							else
								style = CometsSetupPanel.CIRCLE_STYLE;
						}
					});
		}

		JCheckBox fillBox = new JCheckBox("Fill?");
		fillBox.setSelected(fill);
		fillBox.addItemListener(
				new ItemListener() {
					public void itemStateChanged(ItemEvent e)
					{
						fill = e.getStateChange() == ItemEvent.SELECTED;
					}
				});
		
		JButton contentButton = new JButton("Set Content...");
		contentButton.addActionListener(
				new ActionListener() {
					public void actionPerformed(ActionEvent e)
					{
						// popup JOptionPane with content settings.
						// maybe make this a separate object that can
						// do so itself?
						// fetched from csp?
						// DEFINITELY fetched from csp. so it can be used to paint.
						ctp.showContentSettings();
					}
				});
		
		JLabel sLabel = new JLabel("Stroke width: ");
		final JLabel vLabel = new JLabel(String.valueOf(CometsToolbarPanel.MIN_STROKE));
		JSlider strokeSlider = new JSlider(JSlider.HORIZONTAL, 
										   CometsToolbarPanel.MIN_STROKE, 
										   CometsToolbarPanel.MAX_STROKE, 
										   CometsToolbarPanel.MIN_STROKE);
		strokeSlider.addChangeListener(
				new ChangeListener() {
					public void stateChanged(ChangeEvent e)
					{
						JSlider source = (JSlider)e.getSource();
						strokeWidth = (source.getValue())*cParams.getPixelScale();
						vLabel.setText(String.valueOf(source.getValue()));
					}
				});
		strokeSlider.setMajorTickSpacing(1);
		strokeSlider.setPaintTicks(true);
		strokeSlider.setSnapToTicks(true);

		gbc.gridwidth = 1;
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
		gbc.gridy++;
		controlPanel.add(new JSeparator(SwingConstants.HORIZONTAL), gbc);
		gbc.insets = new Insets(0,0,0,0);
		gbc.gridy++;
		controlPanel.add(fillBox, gbc);

		gbc.gridwidth = 1;
		gbc.gridx = 0;
		gbc.gridy++;
		controlPanel.add(sLabel, gbc);

		gbc.gridx = 1;
		controlPanel.add(vLabel, gbc);
	
		gbc.gridx = 0;
		gbc.gridwidth = 2;
		gbc.gridy++;
		gbc.insets = new Insets(0, 0, 10, 0);
		strokeSlider.setPreferredSize(new Dimension(150, (int)strokeSlider.getPreferredSize().getHeight()));
		controlPanel.add(strokeSlider, gbc);
		
		gbc.gridy++;
		controlPanel.add(contentButton, gbc);
	}
	
	@Override
	/**
	 * Required by MouseListener - not used
	 */
	public void mouseClicked(MouseEvent e) { }

	@Override
	/**
	 * Required by MouseListener - not used
	 */
	public void mouseEntered(MouseEvent e) { }

	@Override
	/**
	 * Required by MouseListener - not used
	 */
	public void mouseExited(MouseEvent e) {	}

	@Override
	/**
	 * When the mouse is initially pressed on the <code>CometsSetupPanel</code>, a new,
	 * nearly dimensionless shape is made at that point, and the initial click-point is stored.
	 */
	public void mousePressed(MouseEvent e)
	{
		if ((e.getModifiersEx() & InputEvent.BUTTON1_DOWN_MASK) == 0)
			return;

		mouseDown = e.getPoint();
		if (mouseDown.getX() > cParams.getPixelScale() * cParams.getNumCols() - 1)
			mouseDown.setLocation(cParams.getPixelScale() * cParams.getNumCols() - 1, mouseDown.getY());
		if (mouseDown.getY() > cParams.getPixelScale() * cParams.getNumRows() - 1)
			mouseDown.setLocation(mouseDown.getX(), cParams.getPixelScale() * cParams.getNumRows() - 1);
		
		//selectionShape = new Area();
		switch(style)
		{
			case CometsSetupPanel.BOX_STYLE:
				shape = new Rectangle2D.Double(mouseDown.getX(), mouseDown.getY(), 0, 0);
				break;
			case CometsSetupPanel.CIRCLE_STYLE:
				shape = new Ellipse2D.Double(mouseDown.getX(), mouseDown.getY(), 0, 0);
				break;
			default:
				shape = null;
				break;
		}

		csp.setSelectionShape(shape);
		csp.repaint();
	}

	@Override
	/**
	 * When the mouse is released, if the shape has any width and height to it, it is
	 * filled in with whatever values are set for the content.
	 */
	public void mouseReleased(MouseEvent e)
	{
		if (shape.getBounds().getWidth() != 0 || shape.getBounds().getHeight() != 0)
		{
			paintShape();
		}
		shape = null;
		csp.setSelectionShape(null);
	}

	/**
	 * This fills in the currently selected shape with selected content. It does this by
	 * using the <code>CometsToolbarPanel</code> to get a list of currently selected spaces,
	 * then sending that list with some concentration parameters to the <code>CometsSetupPanel</code>
	 * to be filled in.
	 */
	private void paintShape()
	{
		// Fill a List with points to paint in.
		// This is a different list depending on whether or not we're filling
		// the shape.
		// Then, send the list back to the CometsSetupPanel for filling.
		
		List<Point> fillPoints = new ArrayList<Point>();
		if (fill || area == null)
		{
			// if we fill - use the "shape" variable
			fillPoints = ctp.getShapePoints(shape, false);
		}
		else
		{
			// if not filling, we have a funky area instead. use the "area" variable
			fillPoints = ctp.getShapePoints(area, false);
		}
		paintSpaces(fillPoints, true);
		fireCometsChangePerformed();

//		csp.applyBiomassChange(ctp.getBiomassPanel().getValues(), fillPoints, true);
//		csp.applyMediaChange(ctp.getMediaPanel().getValues(), fillPoints, true);
//		csp.setBarrier(fillPoints, ctp.getBarrier());
//		csp.repaint();
	}
	
	@Override
	/**
	 * When the mouse is dragged around, the shape is updated around the point that was
	 * initially pressed. At the end of this updating, the whole shape is snapped to the grid
	 * as best as possible.
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
				((Rectangle2D.Double)shape).setRect(newX, newY, width, height);
				break;
			case CometsSetupPanel.CIRCLE_STYLE:
				((Ellipse2D.Double)shape).setFrame(newX, newY, width, height);
				break;
			default:
				break;
		}
		ctp.snapShapeToGrid(shape, style);
		
		// Now, if this shape is larger across and down than the 
		// stroke width, get rid of a bit in the middle.
		width = shape.getBounds().getWidth();
		height = shape.getBounds().getHeight();
		if (!fill && width > strokeWidth*2 && height > strokeWidth*2)
		{
			// convert to an Area
			// remove a gap in the center. how big? i'm working on it!
			area = new Area(shape);
			double remX = shape.getBounds().getX() + strokeWidth;
			double remY = shape.getBounds().getY() + strokeWidth;
			double remW = width - 2*strokeWidth;
			double remH = height - 2*strokeWidth;
			switch(style)
			{
				case CometsSetupPanel.BOX_STYLE:
					area.subtract(new Area(new Rectangle2D.Double(remX, remY, remW, remH)));
					break;
				case CometsSetupPanel.CIRCLE_STYLE:
					area.subtract(new Area(new Ellipse2D.Double(remX, remY, remW, remH)));
					break;
			}
			csp.setSelectionShape(area);
		}
		else
		{
			area = null;
			csp.setSelectionShape(shape);
		}
	}

	@Override
	/**
	 * Required by MouseMotionListener - not used
	 */
	public void mouseMoved(MouseEvent e) { }

}
