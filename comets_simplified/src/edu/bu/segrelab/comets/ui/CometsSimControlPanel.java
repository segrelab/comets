package edu.bu.segrelab.comets.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import edu.bu.segrelab.comets.Comets;
import edu.bu.segrelab.comets.event.SimulationStateChangeEvent;
import edu.bu.segrelab.comets.event.SimulationStateChangeListener;

/**
 * CometsSimControlPanel
 * This class creates a control panel that can be used to control whether or not a simulation is
 * currently running, pause it, and end it.
 * @author Bill Riehl briehl@bu.edu
 */
@SuppressWarnings("serial")
public class CometsSimControlPanel extends JPanel implements SimulationStateChangeListener
{
	private JToggleButton runButton;
	private JCheckBox pauseOnStepBox;
	private JButton stopButton;
	private Comets c;
	private JLabel finishLabel;
	
	public CometsSimControlPanel(Comets comets)
	{
		super();
		this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		c = comets;
		
		finishLabel = new JLabel("Finishing current cycle. Please wait.");
		finishLabel.setVisible(false);
		
		runButton = new JToggleButton("Run/Pause Simulation", false);
		runButton.addItemListener(new ItemListener() {

			@Override
			public void itemStateChanged(ItemEvent e)
			{
				if (runButton.isSelected())
				{
					c.getParameters().pause(false);
					try
					{
						c.getRunBarrier().await();
					}
					catch (Exception ex)
					{
						System.out.println(ex);
					}
				}
				else
				{
					c.getParameters().pause(true);
				}
			}
		});

		pauseOnStepBox = new JCheckBox("Pause after cycle");
		pauseOnStepBox.setSelected(c.getParameters().pauseOnStep());
		pauseOnStepBox.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent arg0)
			{
				c.getParameters().pauseOnStep(pauseOnStepBox.isSelected());
			}
		});
				
		stopButton = new JButton("End");
		stopButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				finishLabel.setVisible(true);
				CometsSimControlPanel.this.revalidate();
				c.updateOnCycle();
				c.endSimulation();
			}
		});
		
		this.setBorder(BorderFactory.createTitledBorder("Simulation Controls"));
		
		this.add(runButton);
		this.add(pauseOnStepBox);
		this.add(stopButton);
		
		this.add(finishLabel);
		
		c.addSimulationStateChangeListener(this);
		reset();
	}
	
	public void reset()
	{
		runButton.setSelected(false);
		finishLabel.setVisible(false);
	}

	@Override
	public void onStateChange(SimulationStateChangeEvent e)
	{
		switch(e.getState())
		{
			case PAUSE :
				runButton.setSelected(false);
				break;
			
			case END :
				runButton.setSelected(false);
				break;
			
			default :
				break;
		}
	}
}
