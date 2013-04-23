package edu.bu.segrelab.comets.ui;

import java.awt.Component;
import java.awt.Dimension;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

@SuppressWarnings("serial")
public class IntroDialog extends JDialog
{
	private static final String LOAD_FILE_STRING = "Load layout file",
								LOAD_DB_STRING = "Load from database",
								LOAD_MODEL_STRING = "Load model file",
								LOAD_FILE_LABEL = "Load a pre-made layout file:",
								LOAD_DB_LABEL = "Load a layout from the P-KOMETS DB:",
								LOAD_MODEL_LABEL = "     Start a new layout by loading model files:     ",
								WELCOME_STRING = "Welcome to COMETS!";
	
	private JButton loadFileButton,
					loadDBButton,
					loadModelButton;

	public IntroDialog(JFrame owner)
	{
		super(owner, WELCOME_STRING, true);
	
		loadFileButton = new JButton(LOAD_FILE_STRING);
		loadFileButton.setAlignmentX(Component.CENTER_ALIGNMENT);

		loadDBButton = new JButton(LOAD_DB_STRING);
		loadDBButton.setAlignmentX(Component.CENTER_ALIGNMENT);
		
		loadModelButton = new JButton(LOAD_MODEL_STRING);
		loadModelButton.setAlignmentX(Component.CENTER_ALIGNMENT);
		
		JLabel loadFileLabel = new JLabel(LOAD_FILE_LABEL);
		loadFileLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		JLabel loadDBLabel = new JLabel(LOAD_DB_LABEL);
		loadDBLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		JLabel loadModelLabel = new JLabel(LOAD_MODEL_LABEL);
		loadModelLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
		panel.add(Box.createRigidArea(new Dimension(0, 20)));
	
		panel.add(loadFileLabel);
		panel.add(loadFileButton);
		panel.add(Box.createRigidArea(new Dimension(0, 20)));
		
		panel.add(loadDBLabel);
		panel.add(loadDBButton);
		panel.add(Box.createRigidArea(new Dimension(0, 20)));
	
		panel.add(loadModelLabel);
		panel.add(loadModelButton);
		panel.add(Box.createRigidArea(new Dimension(0, 20)));
		
		this.add(panel);
		this.pack();
		this.setResizable(false);
	}
	
	public JButton getFileButton()
	{
		return loadFileButton;
	}
	
	public JButton getDBButton()
	{
		return loadDBButton;
	}
	
	public JButton getModelButton()
	{
		return loadModelButton;
	}
}
