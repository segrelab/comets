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
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;

import edu.bu.segrelab.comets.Comets;
import edu.bu.segrelab.comets.ui.CometsSetupPanel;
import edu.bu.segrelab.comets.ui.CometsToolbarPanel;
import edu.bu.segrelab.comets.ui.DataContentPanel;


/**
 * The <code>GradientTool</code> is a part of the tool palette used in modifying the initial
 * layout in the <code>CometsSetupPanel</code>.
 * <p>
 * After selecting this tool, the user performs two steps. The first is the selection of 
 * the area to fill with a gradient, done by dragging out a shape (see ShapeTool). The
 * second is the choice of the gradient's direction, done by drawing an arbitrary line (see
 * LineTool).
 * @author Bill Riehl briehl@bu.edu
 */
public class GradientTool extends AbstractTool
{
	/**
	 * 
	 */
	private static final int LINEAR_GRADIENT = 0;
	private static final int RADIAL_GRADIENT = 1;

	private static final int NOT_STARTED = 2;
	private static final int SELECTING_SHAPE = 3;
	private static final int SELECTING_DIR = 4;
	
	private static final int FILL_STYLE = 5;

	private static final long serialVersionUID = -6754520428531175935L;
	
	private Comets c;
	private Shape gradientShape,
				  dirShape;
	private Point startPoint,
				  endPoint,
				  mouseDown;
	private int selectStyle,
				gradientStyle,
				state;
	private List<Point> gradientSpaces;
	private boolean overwrite = false;
	private DataContentPanel biomassGradientPanel,
							 mediaGradientPanel;
	
	public GradientTool(CometsSetupPanel csp, CometsToolbarPanel ctp, Comets c)
	{
		super(csp, ctp, c.getParameters());
		this.c = c;
		setText("Gradient");

		selectStyle = CometsSetupPanel.BOX_STYLE;
		gradientStyle = LINEAR_GRADIENT;
		
		startPoint = null;
		endPoint = null;
		gradientShape = null;
		biomassGradientPanel = null;
		mediaGradientPanel = null;
		
		state = NOT_STARTED;
	}
	
	protected void buildControlPanel()
	{
		controlPanel = new JPanel(new GridBagLayout());
		controlPanel.setBorder(BorderFactory.createTitledBorder("Gradient options"));

		final JToggleButton[] gradientStyleButtons = new JToggleButton[2];
		gradientStyleButtons[0] = new JToggleButton("linear");
		gradientStyleButtons[0].setSelected(true);
		gradientStyleButtons[1] = new JToggleButton("radial");

		for (int i=0; i<gradientStyleButtons.length; i++)
		{
			final JToggleButton sButton = gradientStyleButtons[i];
			sButton.addActionListener(
					new ActionListener() {
						public void actionPerformed(ActionEvent e)
						{
							for (int j=0; j<gradientStyleButtons.length; j++)
								gradientStyleButtons[j].setSelected(false);
							sButton.setSelected(true);
							if (sButton.getText().equals("linear"))
								gradientStyle = LINEAR_GRADIENT;
							else if (sButton.getText().equals("radial"))
								gradientStyle = RADIAL_GRADIENT;
							else
								gradientStyle = LINEAR_GRADIENT;
							resetGradientSelection();
						}
					});
		}

		final JToggleButton[] selectStyleButtons = new JToggleButton[3];
		selectStyleButtons[0] = new JToggleButton("b");
		selectStyleButtons[0].setSelected(true);
		selectStyleButtons[1] = new JToggleButton("c");
		selectStyleButtons[2] = new JToggleButton("fill");

		for (int i=0; i<selectStyleButtons.length; i++)
		{
			final JToggleButton sButton = selectStyleButtons[i];
			sButton.addActionListener(
					new ActionListener() {
						public void actionPerformed(ActionEvent e)
						{
							for (int j=0; j<selectStyleButtons.length; j++)
								selectStyleButtons[j].setSelected(false);
							sButton.setSelected(true);
							if (sButton.getText().equals("b"))
								selectStyle = CometsSetupPanel.BOX_STYLE;
							else if (sButton.getText().equals("c"))
								selectStyle = CometsSetupPanel.CIRCLE_STYLE;
							else if (sButton.getText().equals("fill"))
								selectStyle = FILL_STYLE;
							else 
								selectStyle = CometsSetupPanel.BOX_STYLE; //LASSO_STYLE;
							
							resetGradientSelection();
						}
					});
		}

		final JCheckBox overwriteBox = new JCheckBox("overwrite");
		overwriteBox.setSelected(overwrite);
		overwriteBox.addActionListener(
				new ActionListener() {
					public void actionPerformed(ActionEvent e)
					{
						overwrite = overwriteBox.isSelected();
					}
				});
		
		
		JButton contentButton = new JButton("Set Gradient...");
		contentButton.addActionListener(
				new ActionListener() {
					public void actionPerformed(ActionEvent e)
					{
						// popup JOptionPane with gradient settings.
						showGradientSettings();
					}
				});
		
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.BOTH;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;


		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.insets = new Insets(0,0,5,0);
		for (int i=0; i<gradientStyleButtons.length; i++)
		{
			gbc.gridx = i;
			controlPanel.add(gradientStyleButtons[i], gbc);
		}
		gbc.gridx++;
		controlPanel.add(overwriteBox, gbc);
		
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.gridx = 0;
		gbc.gridy = 1;
		controlPanel.add(new JSeparator(SwingConstants.HORIZONTAL), gbc);
		gbc.gridwidth = 1;
		gbc.gridy = 2;
		for (int i=0; i<selectStyleButtons.length; i++)
		{
			gbc.gridx = i;
			controlPanel.add(selectStyleButtons[i], gbc);
		}
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.gridx = 0;
		gbc.gridy = GridBagConstraints.RELATIVE;
		controlPanel.add(new JSeparator(SwingConstants.HORIZONTAL), gbc);
		
		gbc.insets = new Insets(0,0,0,0);
		controlPanel.add(contentButton, gbc);
	}
	
	@Override
	public void mouseClicked(MouseEvent e)
	{
		// TODO Auto-generated method stub
	}

	@Override
	public void mouseEntered(MouseEvent e)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseExited(MouseEvent e)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void mousePressed(MouseEvent e)
	{
		if ((e.getModifiersEx() & InputEvent.BUTTON1_DOWN_MASK) == 0)
			return;

		if (state == NOT_STARTED)
		{
			mouseDown = e.getPoint();
			if (mouseDown.getX() > cParams.getPixelScale() * cParams.getNumCols() - 1)
				mouseDown.setLocation(cParams.getPixelScale() * cParams.getNumCols() - 1, mouseDown.getY());
			if (mouseDown.getY() > cParams.getPixelScale() * cParams.getNumRows() - 1)
				mouseDown.setLocation(mouseDown.getX(), cParams.getPixelScale() * cParams.getNumRows() - 1);
			
			//selectionShape = new Area();
			switch(selectStyle)
			{
				case CometsSetupPanel.BOX_STYLE:
					gradientShape = new Rectangle2D.Double(mouseDown.getX(), mouseDown.getY(), 0, 0);
					break;
				case CometsSetupPanel.CIRCLE_STYLE:
					gradientShape = new Ellipse2D.Double(mouseDown.getX(), mouseDown.getY(), 0, 0);
					break;
				default:
					gradientShape = null;
					break;
			}
	
			csp.setSelectionShape(gradientShape);
			csp.repaint();
			state = SELECTING_SHAPE;
		}
		else if (state == SELECTING_DIR)
		{
			startPoint = e.getPoint();
			endPoint = e.getPoint();
			dirShape = new Line2D.Double();
			csp.setSelectionShape(dirShape);
			updateDirShape();
		}
	}

	/**
	 * Updates the internal line shape.
	 */
	private void updateDirShape()
	{
		((Line2D.Double)dirShape).setLine(startPoint, endPoint);
		csp.repaint();
	}
	
	@Override
	public void mouseReleased(MouseEvent e)
	{
		if (state == SELECTING_SHAPE)
		{
			// make a shape, get the points.
			if (gradientShape.getBounds().getWidth() == 0 &&
				gradientShape.getBounds().getHeight() == 0)
			{
				resetGradientSelection();
			}
			else
			{
				ctp.snapShapeToGrid(gradientShape, selectStyle);
				gradientSpaces = ctp.getShapePoints(gradientShape, false);
//				dirShape = new Line2D.Double();
				state = SELECTING_DIR;
			}
		}
		else if (state == SELECTING_DIR)
		{
			// make a line, make the gradient. done.
			endPoint = e.getPoint();
			createGradient();

			resetGradientSelection();
		}
	}

	@Override
	public void mouseDragged(MouseEvent e)
	{
		if (state == SELECTING_SHAPE)
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
			else if (hereX > cParams.getPixelScale() * cParams.getNumCols() - 1)
				hereX = cParams.getPixelScale() * cParams.getNumCols() - 1;
			
			if (hereY < 0)
				hereY = 0;
			else if (hereY > cParams.getPixelScale() * cParams.getNumRows() - 1)
				hereY = cParams.getPixelScale() * cParams.getNumRows() - 1;
			
			double newX = Math.min(downX, hereX);
			double newY = Math.min(downY, hereY);
			double width = Math.abs(hereX - downX);
			double height = Math.abs(hereY - downY);
			
			switch(selectStyle)
			{
				case CometsSetupPanel.BOX_STYLE:
					((Rectangle2D.Double)gradientShape).setRect(newX, newY, width, height);
					break;
				case CometsSetupPanel.CIRCLE_STYLE:
					((Ellipse2D.Double)gradientShape).setFrame(newX, newY, width, height);
					break;
				default:
					break;
			}
			ctp.snapShapeToGrid(gradientShape, selectStyle);
		}
		else if (state == SELECTING_DIR)
		{
			endPoint = e.getPoint();
			updateDirShape();
		}
	}

	@Override
	public void mouseMoved(MouseEvent e)
	{
		// TODO Auto-generated method stub

	}

	private void resetGradientSelection()
	{
		gradientSpaces = null;
		startPoint = null;
		endPoint = null;
		gradientShape = null;
		
		if (selectStyle == FILL_STYLE)
			state = SELECTING_DIR;
		else
			state = NOT_STARTED;

		csp.setSelectionShape(null);
		csp.repaint();
	}

	private void createGradient()
	{
		if (startPoint == null ||
			endPoint == null)
			return;
		else if (gradientSpaces == null)
		{
			if (selectStyle != FILL_STYLE)
				return;
			else
			{
				gradientSpaces = new ArrayList<Point>();
				for (int i=0; i<cParams.getNumCols(); i++)
				{
					for (int j=0; j<cParams.getNumRows(); j++)
					{
						gradientSpaces.add(new Point(i, j));
					}
				}
			}
		}
		// DO EET.

		Point S = new Point((int)Math.floor(startPoint.getX()/cParams.getPixelScale()),
							(int)Math.floor(startPoint.getY()/cParams.getPixelScale()));
		Point E = new Point((int)Math.floor(endPoint.getX()/cParams.getPixelScale()),
							(int)Math.floor(endPoint.getY()/cParams.getPixelScale()));
		
		if (S.getX() == E.getX() && S.getY() == E.getY())
			return;
		
		double deltaX = E.getX() - S.getX();
		double deltaY = E.getY() - S.getY();
		double distSE = Math.sqrt(Math.pow(deltaX, 2) + Math.pow(deltaY, 2));
		// problem! m might go to infinity if we're close enough to a vertical line.
//		double m = (E.getY()-S.getY())/(E.getX()-S.getX());
//		double b = E.getY() - m * E.getX();
		
//		double denom = m*m+1;
		Iterator<Point> it = gradientSpaces.iterator();
		
		double[][] biomassGradient = biomassGradientPanel.getAllValues();
		double[][] mediaGradient = mediaGradientPanel.getAllValues();
		boolean[] staticMedia = mediaGradientPanel.getStatic();
		boolean useStatic = false;
		for (int i=0; i<staticMedia.length; i++)
		{
			useStatic = useStatic || staticMedia[i];
		}
		
		while (it.hasNext())
		{
			Point P = it.next();
//			/**
//			 * foreach biomass and metabolite component i
//			 * {
//			 *    conc i at space P = 
//			 *    		conc i at S + (conc i at E - conc i at S * (distSQ/distSE))
//			 * }
//			 */
			
			// distSQ = distance between S and an imaginary point Q on the line between
			// S and E, and perpendicular to P
			
			double distOnLine = 0;
			double distSP = Math.sqrt(Math.pow(P.getX() - S.getX(), 2) + Math.pow(P.getY() - S.getY(), 2));
			if (gradientStyle == LINEAR_GRADIENT)
			{
//				distOnLine = Math.sqrt(Math.pow((P.getX() + m*P.getY() - m*b)/denom - S.getX(), 2) +
//									   Math.pow((m*P.getX() + m*m*P.getY() + b)/denom - S.getY(), 2));
				double d = Math.abs((deltaX * (S.getY() - P.getY()) - ((S.getX() - P.getX()) * deltaY)))/distSE;
				distOnLine = Math.sqrt(distSP*distSP - d*d);
			}
			else if (gradientStyle == RADIAL_GRADIENT)
				distOnLine = distSP;// Math.sqrt(Math.pow(P.getX() - S.getX(), 2) + Math.pow(P.getY() - S.getY(), 2));
			double[] deltaBiomass = new double[biomassGradient.length];
			double[] deltaMedia = new double[mediaGradient.length];
			
			for (int i=0; i<deltaBiomass.length; i++)
			{
				deltaBiomass[i] = biomassGradient[i][0] + ((biomassGradient[i][1] - biomassGradient[i][0]) * distOnLine/distSE);
				deltaBiomass[i] = Math.max(deltaBiomass[i], 0);
				deltaBiomass[i] = Math.min(deltaBiomass[i], Math.max(biomassGradient[i][0], biomassGradient[i][1]));
			}
			
			for (int i=0; i<deltaMedia.length; i++)
			{
				deltaMedia[i] = mediaGradient[i][0] + ((mediaGradient[i][1] - mediaGradient[i][0]) * distOnLine/distSE);
				deltaMedia[i] = Math.max(deltaMedia[i], 0);
				deltaMedia[i] = Math.min(deltaMedia[i], Math.max(mediaGradient[i][0], mediaGradient[i][1]));
			}
			
			csp.applyBiomassChange(deltaBiomass, P, overwrite);
			csp.applyMediaChange(deltaMedia, P, overwrite);

			if (useStatic)
			{
				csp.addStaticMediaSpace(deltaMedia, staticMedia, P, overwrite);
			}
		}
		fireCometsChangePerformed();
	}

	private void setupBiomassGradientPanel()
	{
		if (c.getModels() == null || c.getModels().length == 0)
			return;
		String[] names = c.getModelNames();
		for (int i=0; i<names.length; i++)
		{
			if (names[i] == null || names[i].length() == 0)
				names[i] = "Model " + i + ": ";
		}
		biomassGradientPanel = new DataContentPanel(names, new String[]{"Start", "End"}, false, null);
		biomassGradientPanel.setBorder(BorderFactory.createTitledBorder("Biomass"));
	}
	
	private void setupMediaGradientPanel()
	{
		if (c.getModels() == null || c.getModels().length == 0)
			return;
		mediaGradientPanel = new DataContentPanel(c.getMediaNames(), new String[]{"Start", "End"}, true, new boolean[c.getMediaNames().length]);
		mediaGradientPanel.setBorder(BorderFactory.createTitledBorder("Media"));
	}
	
	private void showGradientSettings()
	{
		if (c.getModels() == null || c.getModels().length == 0)
		{
			JOptionPane.showMessageDialog(csp, new JLabel("No models loaded!"), "Error!", JOptionPane.ERROR_MESSAGE);
			return;
		}
		if (biomassGradientPanel == null)
			setupBiomassGradientPanel();
		if (mediaGradientPanel == null)
			setupMediaGradientPanel();
		
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

		if (c.getModels().length > 8)
		{
			JScrollPane scrollPane = new JScrollPane(biomassGradientPanel);
			scrollPane.setPreferredSize(new Dimension(200, 300));
			panel.add(scrollPane);
		}
		else
			panel.add(biomassGradientPanel);
		
		if (c.getWorld().getNumMedia() > 8)
		{
			JScrollPane scrollPane = new JScrollPane(mediaGradientPanel);
			scrollPane.setPreferredSize(new Dimension(200, 300));
			panel.add(scrollPane);
		}
		else
			panel.add(mediaGradientPanel);

		int result = JOptionPane.showConfirmDialog(csp, 
												   panel, 
												   "Set tool content", 
												   JOptionPane.OK_CANCEL_OPTION, 
												   JOptionPane.PLAIN_MESSAGE);
		if (result == JOptionPane.OK_OPTION)
		{
			biomassGradientPanel.updateValues();
			mediaGradientPanel.updateValues();
		}
		else if (result == JOptionPane.CANCEL_OPTION)
		{
			biomassGradientPanel.resetValues();
			mediaGradientPanel.resetValues();
		}
	}
	
	public void reset()
	{
		biomassGradientPanel = null;
		mediaGradientPanel = null;
	}
}
