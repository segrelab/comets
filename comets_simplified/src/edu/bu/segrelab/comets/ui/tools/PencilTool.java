package edu.bu.segrelab.comets.ui.tools;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JToggleButton;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import edu.bu.segrelab.comets.CometsParameters;
import edu.bu.segrelab.comets.ui.CometsSetupPanel;
import edu.bu.segrelab.comets.ui.CometsToolbarPanel;

/**
 * The <code>PencilTool</code> is a part of the tool palette used in modifying the initial
 * layout in the <code>CometsSetupPanel</code>.
 * <p>
 * With this tool selected the user can draw biomass and media on the layout panel. This is done
 * by clicking and dragging around the <code>CometsSetupPanel</code>. Any space touched while
 * clicking and dragging is populated with biomass and media.
 * <p>
 * This tool's control panel allows the user to adjust the shape of the tool (square or
 * round), and the size of the tool (a diameter ranging from 1 to 10 grid squares)
 * @author Bill Riehl briehl@bu.edu
 */
public class PencilTool extends AbstractTool
{
	private static final long serialVersionUID = -6068200897874156665L;
	protected int strokeWidth,
				  style;
	protected Shape pencilShape;

	/**
	 * Creates a new <code>PencilTool</code> that acts on the given <code>CometsSetupPanel</code>,
	 * resides in the given <code>CometsToolbarPanel</code>, and uses the given set of
	 * <code>CometsParameters</code>.
	 * @param csp - a <code>CometsSetupPanel</code>
	 * @param ctp - a <code>CometsToolbarPanel</code>
	 * @param cParams - a <code>CometsParameters</code>
	 */
	public PencilTool(CometsSetupPanel csp, CometsToolbarPanel ctp, CometsParameters cParams)
	{
		super(csp, ctp, cParams);
		setText("Pencil");
		strokeWidth = CometsToolbarPanel.MIN_STROKE * cParams.getPixelScale();
		style = CometsSetupPanel.BOX_STYLE;
		resetPencilShape();
	}

	/**
	 * Builds the control panel for this tool. This contains three options:<br>
	 * 1. The shape of the tool (a pair of buttons - either square or circle)<br>
	 * 2. The size of the tool, adjustable with a <code>JSlider</code> to have a diameter
	 * between 1 and 10 spaces.<br>
	 * 3. A button for setting the content of the pencil - how much biomass and media
	 * should be put down.
	 */
	protected void buildControlPanel()
	{
		controlPanel = new JPanel(new GridBagLayout());
		controlPanel.setBorder(BorderFactory.createTitledBorder("Pencil Options"));

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
							resetPencilShape();
						}
					});
		}

		
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

		JLabel sLabel = new JLabel("Size: ");
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

		gbc.gridx = 0;
		gbc.gridy++;
		
		controlPanel.add(sLabel, gbc);
		gbc.gridx = 1;
		controlPanel.add(vLabel, gbc);
		gbc.gridx = 0;
		gbc.gridwidth = 3;
		gbc.gridy++;
		gbc.insets = new Insets(0, 0, 10, 0);
		strokeSlider.setPreferredSize(new Dimension(150, (int)strokeSlider.getPreferredSize().getHeight()));
		controlPanel.add(strokeSlider, gbc);
		
		gbc.gridy++;
		controlPanel.add(contentButton, gbc);
	}
	
	@Override
	/**
	 * Required for MouseListener - not used
	 */
	public void mouseClicked(MouseEvent e)
	{
	}

	@Override
	/**
	 * When the mouse enters the area occupied by the <code>CometsSetupPanel</code>, the
	 * shape this pencil will draw is shown.
	 */
	public void mouseEntered(MouseEvent e)
	{
		ctp.snapShapeToGrid(pencilShape, style);
		csp.setSelectionShape(pencilShape);
	}

	@Override
	/**
	 * When the mouse leaves the area occupied by the <code>CometsSetupPanel</code>, the
	 * outline of the pencil's shape goes away too.
	 */
	public void mouseExited(MouseEvent e)
	{
		csp.setSelectionShape(null);
	}

	@Override
	/**
	 * When any mouse button is pressed, the current spot is drawn in.
	 */
	public void mousePressed(MouseEvent e)
	{
		fillCurrentShape();
	}

	@Override
	/**
	 * Required for MouseListener - not used.
	 */
	public void mouseReleased(MouseEvent e)
	{
		fireCometsChangePerformed();
	}

	@Override
	/**
	 * When the mouse is dragged, any space it lands on is filled with the currently
	 * set content.
	 */
	public void mouseDragged(MouseEvent e)
	{
		mouseMoved(e);
		fillCurrentShape();
	}

	@Override
	/**
	 * When the mouse is moved (whether or not the mouse is clicked down), the tool
	 * updates is position by the proper pencil shape to the grid.
	 */
	public void mouseMoved(MouseEvent e)
	{
		// capture the coordinates of the pencil shape
		updatePencilShape(e.getPoint());
		ctp.snapShapeToGrid(pencilShape, style);
		csp.setSelectionShape(pencilShape);
	}

	/**
	 * Updates the pencil's shape. First, the point is translated from mouse-coordinates
	 * to <code>World2D</code> coordinates. Then either a square or circle is drawn using that
	 * space as its center.
	 * @param p
	 */
	private void updatePencilShape(Point p)
	{
		p = new Point((int)Math.floor(p.getX()/cParams.getPixelScale()),
					  (int)Math.floor(p.getY()/cParams.getPixelScale()));
		Rectangle2D.Double frame = new Rectangle2D.Double(p.getX()*cParams.getPixelScale() - (strokeWidth/2), 
														  p.getY()*cParams.getPixelScale() - (strokeWidth/2),
														  strokeWidth,
														  strokeWidth);
		switch(style)
		{
			case CometsSetupPanel.BOX_STYLE:
				((Rectangle2D.Double)pencilShape).setRect(frame);
				break;
			case CometsSetupPanel.CIRCLE_STYLE:
				((Ellipse2D.Double)pencilShape).setFrame(frame);
		}
	}
	
	/**
	 * Resets the given pencil shape back to either a square or circle with diameter
	 * given by the current stroke width, positioned at (0, 0).
	 * <p>
	 * Used mainly when changing something about the pencil shape (either box&lt;=&rt;circle
	 * or shape size) without it being in the current <code>CometsSetupPanel</code>.
	 */
	protected void resetPencilShape()
	{
		switch(style)
		{
			case CometsSetupPanel.BOX_STYLE:
				pencilShape = new Rectangle2D.Double(0, 0, strokeWidth, strokeWidth);
				break;
			case CometsSetupPanel.CIRCLE_STYLE:
				pencilShape = new Ellipse2D.Double(0, 0, strokeWidth, strokeWidth);
				break;
		}
	}

	/**
	 * Fills the current shape with media, biomass, and/or barrier. This is done by
	 * using the <code>CometsToolbarPanel</code> to get the points occupied by the shape
	 * and filling them in.
	 */
	protected void fillCurrentShape()
	{
		List<Point> spaces = ctp.getShapePoints(pencilShape, false);
		paintSpaces(spaces, true);
	}
}
