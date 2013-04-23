package edu.bu.segrelab.comets.fba.ui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

import edu.bu.segrelab.comets.Comets;
import edu.bu.segrelab.comets.fba.FBAParameters;
import edu.bu.segrelab.comets.ui.IntField;
import edu.bu.segrelab.comets.ui.ParametersPanel;

@SuppressWarnings("serial")
public class ThreadParametersPanel extends JPanel
								   implements ParametersPanel
{
	private static final String BORDER 		= "Threads",
								NUM_THREADS = "Number of FBA run threads: ",
								NAME		= "FBA Threads";
	
	private IntField numThreadsField;
	private JLabel numThreadsLbl;
	
	private FBAParameters fbaParams;
	public ThreadParametersPanel(FBAParameters fbaParams)
	{
		super(new GridBagLayout());
		this.fbaParams = fbaParams;
		initPanelWidgets();
		assemblePanelWidgets();
	}

	private void assemblePanelWidgets()
	{
		
		this.setLayout(new GridBagLayout());
		this.setBorder(BorderFactory.createTitledBorder(BORDER));
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.ipadx = 10;
		gbc.insets = new Insets(5,5,5,5);
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.weightx = gbc.weighty = 0.0;
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.anchor = GridBagConstraints.EAST;
		this.add(numThreadsLbl, gbc);
		
		gbc.gridx = 1;
		this.add(numThreadsField, gbc);
	}

	private void initPanelWidgets()
	{
		numThreadsLbl = new JLabel(NUM_THREADS, JLabel.LEFT);
		numThreadsField = new IntField(fbaParams.getNumRunThreads(), 6, false);
	}

	@Override
	public void applyChanges()
	{
		fbaParams.setNumRunThreads(numThreadsField.getIntValue());
	}

	@Override
	public void resetChanges()
	{
		numThreadsField.setValue(fbaParams.getNumRunThreads());
	}

	@Override
	public void updateState(Comets c)
	{
		
	}
	
	public String getName()
	{
		return NAME;
	}


}
