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
import java.awt.event.MouseEvent;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import edu.bu.segrelab.comets.CometsParameters;
import edu.bu.segrelab.comets.ui.CometsSetupPanel;
import edu.bu.segrelab.comets.ui.CometsToolbarPanel;

/**
 * The <code>LineTool</code> is a part of the tool palette used in modifying the initial
 * layout in the <code>CometsSetupPanel</code>.
 * <p>
 * With this tool selected the user can draw a line of biomass and media on the layout panel.
 * This is done by clicking and dragging to show the area of line drawing, then releasing to
 * populate that line with biomass or media.
 * <p>
 * This tool's control panel allows the user to adjust the width of the line drawn (NOT 
 * FINISHED YET), and set the values that this line will draw.
 * @author Bill Riehl briehl@bu.edu
 * //TODO - make line width changeable
 */
public class LineTool extends AbstractTool
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 7777759762176514143L;
	private int strokeWidth;
	private Shape lineShape;
	private Point startPoint,
				  endPoint;
	
	/**
	 * Constructs a new <code>LineTool</code> linked to a given <code>CometsSetupPanel</code>,
	 * <code>CometsToolbarPanel</code> and implementing a set of <code>CometsParameters</code>.
	 * @param csp - the <code>CometsSetupPanel</code> where lines might be drawn
	 * @param ctp - the <code>CometsToolbarPanel</code> where this tool will reside 
	 * @param cParams - a set of <code>CometsParameters</code>
	 */
	public LineTool(CometsSetupPanel csp, CometsToolbarPanel ctp, CometsParameters cParams)
	{
		super(csp, ctp, cParams);
		setText("Line");
		strokeWidth = CometsToolbarPanel.MIN_STROKE;
		lineShape = new Line2D.Double(0, 0, strokeWidth, strokeWidth);
	}

	/**
	 * Build the control panel for this tool.
	 * The control panel has a <code>JSlider</code> for the width of the line, and
	 * a button for setting the content of the line. 
	 */
	protected void buildControlPanel()
	{
		controlPanel = new JPanel(new GridBagLayout());
		controlPanel.setBorder(BorderFactory.createTitledBorder("Line Controls"));

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.BOTH;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;

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

		JLabel sLabel = new JLabel("Width: ");
		final JLabel vLabel = new JLabel(String.valueOf(strokeWidth));
		JSlider strokeSlider = new JSlider(JSlider.HORIZONTAL, 
										   CometsToolbarPanel.MIN_STROKE, 
										   CometsToolbarPanel.MAX_STROKE, 
										   CometsToolbarPanel.MIN_STROKE);
		strokeSlider.addChangeListener(
				new ChangeListener() {
					public void stateChanged(ChangeEvent e)
					{
						JSlider source = (JSlider)e.getSource();
						strokeWidth = (int)source.getValue();
						vLabel.setText(String.valueOf(strokeWidth));
					}
				});
		strokeSlider.setMajorTickSpacing(1);
		strokeSlider.setPaintTicks(true);
		strokeSlider.setSnapToTicks(true);

		gbc.gridwidth = 1;
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.insets = new Insets(0,0,5,0);

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
	 * Not used
	 */
	public void mouseClicked(MouseEvent e)
	{
	}

	@Override
	/**
	 * When the mouse enters the layout area, it essentially initializes
	 * the functionality of the tool.
	 * However, nothing is done until the mouse is clicked and dragged (e.g.
	 * <code>mouseDragged()</code> is called).
	 */
	public void mouseEntered(MouseEvent e)
	{
		if (csp.getSelectionShape() == null)
		{
			mouseMoved(e);
			csp.setSelectionShape(lineShape);
		}
	}

	/**
	 * When the mouse leaves the layout area, the selection shape should
	 * be removed.
	 */
	@Override
	public void mouseExited(MouseEvent e)
	{
		if ((e.getModifiersEx() & InputEvent.BUTTON1_DOWN_MASK) == 0)
		csp.setSelectionShape(null);
	}

	/**
	 * Not used.
	 */
	@Override
	public void mousePressed(MouseEvent e)
	{
	}

	@Override
	/**
	 * When the mouse is released, then a line of biomass and nutrient should
	 * be filled.
	 */
	public void mouseReleased(MouseEvent e)
	{
		// draw stuff
		// delete the line
		fillLine();
		mouseMoved(e);
	}

	@Override
	public void mouseDragged(MouseEvent e)
	{
		endPoint = e.getPoint();
		updateLineShape();
	}

	@Override
	/**
	 * Whenever the mouse is moved (note that this doesn't involve clicking and dragging -
	 * that's handled by <code>mouseDragged</code>), the line's start and end point
	 * should be set.
	 * Functionally, this means that if the user just clicks on a single point without 
	 * dragging, then a one-square line is drawn.
	 */
	public void mouseMoved(MouseEvent e)
	{
		startPoint = e.getPoint();
		endPoint = e.getPoint(); 
		updateLineShape();
	}

	/**
	 * Updates the internal line shape.
	 */
	private void updateLineShape()
	{
		((Line2D.Double)lineShape).setLine(startPoint, endPoint);
		csp.repaint();
	}

	/**
	 * Fills in the currently selected line with biomass and/or nutrient
	 * as decided by the content being tracked by the <code>CometsToolbarPanel</code>.
	 * <p>
	 * This is currently done by just finding all spaces that the line intersects.
	 * This might be a bit too greedy, though... but I'm not sure how else to handle it
	 * without doing something like implementing a version Bresenham's line drawing 
	 * algorithm or something.
	 * <p>
	 * I might get to that later. But first, there's Science to do.
	 */
	private void fillLine()
	{
		if (lineShape == null)
			return;
		
		List<Point> spaces = new ArrayList<Point>();

		/* not sure how else to do this effectively for all wacky shapes.
		 *
		 * iterate over all spaces on the grid.
		 * if they're present in the Shape (Shape.contains(x, y)), select 'em.
		 * For something so painfully inelegant, it works pretty snappy.
		 */
		int scale = cParams.getPixelScale(); // just because i don't want to fetch it 8 hojillion times.
		for (int i=0; i<cParams.getNumCols(); i++)
		{
			for (int j=0; j<cParams.getNumRows(); j++)
			{
				if (lineShape.intersects(i*scale, j*scale, scale, scale))
					spaces.add(new Point(i, j));
			}
		}
		paintSpaces(spaces, true);
		fireCometsChangePerformed();

//		csp.applyBiomassChange(ctp.getBiomassPanel().getValues(), spaces, true);
//		csp.applyMediaChange(ctp.getMediaPanel().getValues(), spaces, true);
//		if (ctp.getMediaPanel().hasStaticChecked())
//			csp.addStaticMediaSpaces(ctp.getMediaPanel().getValues(), ctp.getMediaPanel().getStatic(), spaces, true);
//		else
//			csp.removeStaticMediaSpaces(spaces);
//		csp.setBarrier(spaces, ctp.getBarrier());
//		csp.repaint();

	}
}
