package edu.bu.segrelab.comets.fba.ui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.Border;

import edu.bu.segrelab.comets.Comets;
import edu.bu.segrelab.comets.fba.FBAParameters;
import edu.bu.segrelab.comets.ui.IntField;
import edu.bu.segrelab.comets.ui.ParametersPanel;

@SuppressWarnings("serial")
public class LogParametersPanel extends JPanel
								implements ParametersPanel
{
	private static final int LOG_NAME_COLS = 10,
							 LOG_RATE_COLS = 3;
	
	private static final String WRITE_FLUX = "Write flux log",
								WRITE_MEDIA = "Write media log",
								WRITE_BIOMASS = "Write biomass log",
								WRITE_TOTAL_BIOMASS = "Write total biomass log",
								FLUX_NAME = "Flux log name: ",
								MEDIA_NAME = "Media log name: ",
								BIOMASS_NAME = "Biomass log name: ",
								TOTAL_BIOMASS_NAME = "Total biomass log name: ",
								SELECT = "Select",
								RATE = "Rate: ",
								TIME_STAMP = "Time stamp each log file",
								BORDER = "Log Files",
								NAME = "Output Log",
								WRITE_MAT = "Write .mat log file",
								MAT_FILE_NAME = ".mat file name";
	
	
	private JCheckBox writeFluxLogBox,
					  writeMediaLogBox,
					  writeBiomassLogBox,
					  writeTotalBiomassLogBox,
					  writeMatFileBox,
					  useTimeStampBox;

	private JTextField fluxLogField,
					   mediaLogField,
					   biomassLogField,
					   totalBiomassLogField,
					   matFileField;

	private JButton fluxLogButton,
					mediaLogButton,
					biomassLogButton,
					totalBiomassLogButton,
					matFileButton;
	
	private IntField fluxLogRateField,
					 mediaLogRateField,
					 biomassLogRateField,
					 totalBiomassLogRateField,
					 matFileRateField;
	
	private JLabel fluxLogLbl,
				   mediaLogLbl,
				   biomassLogLbl,
				   totalBiomassLogLbl,
				   fluxLogRateLbl,
				   mediaLogRateLbl,
				   biomassLogRateLbl,
				   totalBiomassLogRateLbl,
				   matFileLbl,
				   matFileRateLbl;
	
	private FBAParameters fbaParams;
	public LogParametersPanel(FBAParameters fbaParams)
	{
		super(new GridBagLayout());
		this.fbaParams = fbaParams;
		initPanelWidgets();
		bindEvents();
		assemblePanelWidgets();
	}
	
	private void assemblePanelWidgets()
	{
		this.setLayout(new GridBagLayout());
		Border logBorder = BorderFactory.createTitledBorder(BORDER);
		this.setBorder(logBorder);
		GridBagConstraints gbc = new GridBagConstraints();
		
		gbc.anchor = GridBagConstraints.WEST;
		gbc.gridx = 0;
		gbc.gridy = 0;
		this.add(writeFluxLogBox, gbc);
		
		gbc.anchor = GridBagConstraints.EAST;
		gbc.gridx = 1;
		this.add(fluxLogRateLbl, gbc);

		gbc.gridx = 2;
		this.add(fluxLogRateField, gbc);
		
		gbc.gridx = 3;
		this.add(fluxLogLbl, gbc);
		
		gbc.gridx = 4;
		this.add(fluxLogField, gbc);
		
		gbc.gridx = 5;
		this.add(fluxLogButton, gbc);
		
		gbc.anchor = GridBagConstraints.WEST;
		gbc.gridx = 0;
		gbc.gridy = 1;
		this.add(writeMediaLogBox, gbc);
		
		gbc.anchor = GridBagConstraints.EAST;
		gbc.gridx = 1;
		this.add(mediaLogRateLbl, gbc);
		
		gbc.gridx = 2;
		this.add(mediaLogRateField, gbc);
		
		gbc.gridx = 3;
		this.add(mediaLogLbl, gbc);
		
		gbc.gridx = 4;
		this.add(mediaLogField, gbc);
		
		gbc.gridx = 5;
		this.add(mediaLogButton, gbc);

		gbc.anchor = GridBagConstraints.WEST;
		gbc.gridx = 0;
		gbc.gridy = 2;
		this.add(writeBiomassLogBox, gbc);
		
		gbc.anchor = GridBagConstraints.EAST;
		gbc.gridx = 1;
		this.add(biomassLogRateLbl, gbc);
		
		gbc.gridx = 2;
		this.add(biomassLogRateField, gbc);
		
		gbc.gridx = 3;
		this.add(biomassLogLbl, gbc);
		
		gbc.gridx = 4;
		this.add(biomassLogField, gbc);
		
		gbc.gridx = 5;
		this.add(biomassLogButton, gbc);
		
		gbc.anchor = GridBagConstraints.WEST;
		gbc.gridx = 0;
		gbc.gridy = 3;
		this.add(writeTotalBiomassLogBox, gbc);

		gbc.anchor = GridBagConstraints.EAST;
		gbc.gridx = 1;
		this.add(totalBiomassLogRateLbl, gbc);
		
		gbc.gridx = 2;
		this.add(totalBiomassLogRateField, gbc);
		
		gbc.gridx = 3;
		this.add(totalBiomassLogLbl, gbc);
		
		gbc.gridx = 4;
		this.add(totalBiomassLogField, gbc);
		
		gbc.gridx = 5;
		this.add(totalBiomassLogButton, gbc);
		
		gbc.anchor = GridBagConstraints.WEST;
		gbc.gridx = 0;
		gbc.gridy = 4;
		this.add(writeMatFileBox, gbc);
		
		gbc.anchor = GridBagConstraints.EAST;
		gbc.gridx = 1;
		this.add(matFileRateLbl, gbc);
		
		gbc.gridx = 2;
		this.add(matFileRateField, gbc);
		
		gbc.gridx = 3;
		this.add(matFileLbl, gbc);
		
		gbc.gridx = 4;
		this.add(matFileField, gbc);
		
		gbc.gridx = 5;
		this.add(matFileButton, gbc);
		
		gbc.gridx = 0;
		gbc.gridy = 5;
		gbc.gridwidth = 3;
		gbc.anchor = GridBagConstraints.WEST;
		this.add(useTimeStampBox, gbc);		
	}

	private void bindEvents()
	{
		
		/*
		 * Whenever the writeFluxLogBox is checked or unchecked, it should
		 * enable/disable the label, text field, and file chooser button
		 * for that log parameter.
		 */
		writeFluxLogBox.addItemListener(
				new ItemListener() 
				{
					public void itemStateChanged(ItemEvent e)
					{
						fluxLogLbl.setEnabled(e.getStateChange() == ItemEvent.SELECTED);
						fluxLogField.setEnabled(e.getStateChange() == ItemEvent.SELECTED);
						fluxLogButton.setEnabled(e.getStateChange() == ItemEvent.SELECTED);
						fluxLogRateLbl.setEnabled(e.getStateChange() == ItemEvent.SELECTED);
						fluxLogRateField.setEnabled(e.getStateChange() == ItemEvent.SELECTED);
					}
				});

		/*
		 * Clicking the flux log button should make a JFileChooser to allow the
		 * user to pick a file instead of just typing in a name.
		 */
		fluxLogButton.addActionListener(
				new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						selectLogFile(fluxLogField);
					}
				});

		/******************* INITIALIZE MEDIA LOG WIDGETS *******************/

		/* 
		 * The UI widgets for the media log and biomass log both behave as
		 * the flux log above. So don't expect too much more commenting.
		 */
		writeMediaLogBox.addItemListener(
			new ItemListener() 
			{
				public void itemStateChanged(ItemEvent e)
				{
					mediaLogLbl.setEnabled(e.getStateChange() == ItemEvent.SELECTED);
					mediaLogField.setEnabled(e.getStateChange() == ItemEvent.SELECTED);
					mediaLogButton.setEnabled(e.getStateChange() == ItemEvent.SELECTED);
					mediaLogRateLbl.setEnabled(e.getStateChange() == ItemEvent.SELECTED);
					mediaLogRateField.setEnabled(e.getStateChange() == ItemEvent.SELECTED);
				}
			}
		);



		mediaLogButton.addActionListener(
			new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					selectLogFile(mediaLogField);
				}
			}
		);

		/****************** INITIALIZE BIOMASS LOG WIDGETS *******************/
		
		writeBiomassLogBox.addItemListener(
			new ItemListener() 
			{
				public void itemStateChanged(ItemEvent e)
				{
					biomassLogLbl.setEnabled(e.getStateChange() == ItemEvent.SELECTED);
					biomassLogField.setEnabled(e.getStateChange() == ItemEvent.SELECTED);
					biomassLogButton.setEnabled(e.getStateChange() == ItemEvent.SELECTED);
					biomassLogRateLbl.setEnabled(e.getStateChange() == ItemEvent.SELECTED);
					biomassLogRateField.setEnabled(e.getStateChange() == ItemEvent.SELECTED);
				}
			}
		);



		biomassLogButton.addActionListener(
				new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						selectLogFile(biomassLogField);
					}
				});
		
		/************* INITIALIZE TOTAL BIOMASS LOG WIDGETS ******************/
		writeTotalBiomassLogBox.addItemListener(
				new ItemListener() 
				{
					public void itemStateChanged(ItemEvent e)
					{
						totalBiomassLogLbl.setEnabled(e.getStateChange() == ItemEvent.SELECTED);
						totalBiomassLogField.setEnabled(e.getStateChange() == ItemEvent.SELECTED);
						totalBiomassLogButton.setEnabled(e.getStateChange() == ItemEvent.SELECTED);
						totalBiomassLogRateLbl.setEnabled(e.getStateChange() == ItemEvent.SELECTED);
						totalBiomassLogRateField.setEnabled(e.getStateChange() == ItemEvent.SELECTED);
					}
				});



		totalBiomassLogButton.addActionListener(
				new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						selectLogFile(totalBiomassLogField);
					}
				});
				
		/************* INITIALIZE MAT FILE WIDGET ******************/
		writeMatFileBox.addItemListener(
				new ItemListener() 
				{
					public void itemStateChanged(ItemEvent e)
					{
						matFileLbl.setEnabled(e.getStateChange() == ItemEvent.SELECTED);
						matFileField.setEnabled(e.getStateChange() == ItemEvent.SELECTED);
						matFileButton.setEnabled(e.getStateChange() == ItemEvent.SELECTED);
						matFileRateLbl.setEnabled(e.getStateChange() == ItemEvent.SELECTED);
						matFileRateField.setEnabled(e.getStateChange() == ItemEvent.SELECTED);
					}
				});


		matFileButton.addActionListener(
				new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						selectLogFile(matFileField);
					}
				});		
	}
	
	private void selectLogFile(JTextField field)
	{
		JFileChooser chooser = new JFileChooser(fbaParams.getLastDirectory());
		int returnVal = chooser.showDialog(this, SELECT);
		if (returnVal == JFileChooser.APPROVE_OPTION)
		{
			field.setText(chooser.getSelectedFile().getPath());
			fbaParams.setLastDirectory(chooser.getSelectedFile().getParent());
		}
	}
	

	private void initPanelWidgets()
	{
		useTimeStampBox = new JCheckBox(TIME_STAMP, fbaParams.useLogNameTimeStamp());

		writeFluxLogBox = new JCheckBox(WRITE_FLUX, fbaParams.writeFluxLog());
		writeMediaLogBox = new JCheckBox(WRITE_MEDIA, fbaParams.writeMediaLog());
		writeBiomassLogBox = new JCheckBox(WRITE_BIOMASS, fbaParams.writeBiomassLog());
		writeTotalBiomassLogBox = new JCheckBox(WRITE_TOTAL_BIOMASS, fbaParams.writeTotalBiomassLog());
		writeMatFileBox = new JCheckBox(WRITE_MAT,fbaParams.writeMatFile());
		
		fluxLogRateLbl = new JLabel(RATE, JLabel.LEFT);
		mediaLogRateLbl = new JLabel(RATE, JLabel.LEFT);
		biomassLogRateLbl = new JLabel(RATE, JLabel.LEFT);
		totalBiomassLogRateLbl = new JLabel(RATE, JLabel.LEFT);
		matFileRateLbl = new JLabel(RATE,JLabel.LEFT);
		
		fluxLogRateField = new IntField(fbaParams.getFluxLogRate(), LOG_RATE_COLS, false);
		mediaLogRateField = new IntField(fbaParams.getMediaLogRate(), LOG_RATE_COLS, false);
		biomassLogRateField = new IntField(fbaParams.getBiomassLogRate(), LOG_RATE_COLS, false);
		totalBiomassLogRateField = new IntField(fbaParams.getTotalBiomassLogRate(), LOG_RATE_COLS, false);
		matFileRateField = new IntField(fbaParams.getMatFileRate(), LOG_RATE_COLS, false);
		
		fluxLogField = new JTextField(fbaParams.getFluxLogName(), LOG_NAME_COLS);
		mediaLogField = new JTextField(fbaParams.getMediaLogName(), LOG_NAME_COLS);
		biomassLogField = new JTextField(fbaParams.getBiomassLogName(), LOG_NAME_COLS);
		totalBiomassLogField = new JTextField(fbaParams.getTotalBiomassLogName(), LOG_NAME_COLS);
		matFileField = new JTextField(fbaParams.getMatFileName(), LOG_NAME_COLS);
		
		fluxLogButton = new JButton(SELECT);
		mediaLogButton = new JButton(SELECT);
		biomassLogButton = new JButton(SELECT);
		totalBiomassLogButton = new JButton(SELECT);
		matFileButton = new JButton(SELECT);
		
		fluxLogLbl = new JLabel(FLUX_NAME, JLabel.LEFT);
		mediaLogLbl = new JLabel(MEDIA_NAME, JLabel.LEFT);
		biomassLogLbl = new JLabel(BIOMASS_NAME, JLabel.LEFT);
		totalBiomassLogLbl = new JLabel(TOTAL_BIOMASS_NAME, JLabel.LEFT);
		matFileLbl = new JLabel(MAT_FILE_NAME, JLabel.LEFT);
		
		/*
		 * Set the initial state of the other widgets.
		 */
		if (!fbaParams.writeFluxLog())
		{
			fluxLogLbl.setEnabled(false);
			fluxLogField.setEnabled(false);
			fluxLogButton.setEnabled(false);
			fluxLogRateLbl.setEnabled(false);
			fluxLogRateField.setEnabled(false);
		}
		
		if (!fbaParams.writeMediaLog())
		{
			mediaLogLbl.setEnabled(false);
			mediaLogField.setEnabled(false);
			mediaLogButton.setEnabled(false);
			mediaLogRateLbl.setEnabled(false);
			mediaLogRateField.setEnabled(false);
		}
		
		if (!fbaParams.writeBiomassLog())
		{
			biomassLogLbl.setEnabled(false);
			biomassLogField.setEnabled(false);
			biomassLogButton.setEnabled(false);
			biomassLogRateLbl.setEnabled(false);
			biomassLogRateField.setEnabled(false);
		}		
		
		if (!fbaParams.writeTotalBiomassLog())
		{
			totalBiomassLogLbl.setEnabled(false);
			totalBiomassLogField.setEnabled(false);
			totalBiomassLogButton.setEnabled(false);
			totalBiomassLogRateLbl.setEnabled(false);
			totalBiomassLogRateField.setEnabled(false);
		}
		

		if (!fbaParams.writeMatFile())
		{
			matFileLbl.setEnabled(false);
			matFileField.setEnabled(false);
			matFileButton.setEnabled(false);
			matFileRateLbl.setEnabled(false);
			matFileRateField.setEnabled(false);
		}

		
	}

	@Override
	public void applyChanges()
	{
		fbaParams.useLogNameTimeStamp(useTimeStampBox.isSelected());

		fbaParams.writeFluxLog(writeFluxLogBox.isSelected());
		fbaParams.writeMediaLog(writeMediaLogBox.isSelected());
		fbaParams.writeBiomassLog(writeBiomassLogBox.isSelected());
		fbaParams.writeTotalBiomassLog(writeTotalBiomassLogBox.isSelected());
		fbaParams.writeMatFile(writeMatFileBox.isSelected());
		
		fbaParams.setFluxLogName(fluxLogField.getText());
		fbaParams.setMediaLogName(mediaLogField.getText());
		fbaParams.setBiomassLogName(biomassLogField.getText());
		fbaParams.setTotalBiomassLogName(totalBiomassLogField.getText());
		fbaParams.setMatFileName(matFileField.getText());
		
		fbaParams.setFluxLogRate(fluxLogRateField.getIntValue());
		fbaParams.setMediaLogRate(mediaLogRateField.getIntValue());
		fbaParams.setBiomassLogRate(biomassLogRateField.getIntValue());
		fbaParams.setTotalBiomassLogRate(totalBiomassLogRateField.getIntValue());
		fbaParams.setMatFileRate(matFileRateField.getIntValue());
	}

	@Override
	public void resetChanges()
	{
		useTimeStampBox.setSelected(fbaParams.useLogNameTimeStamp());
		
		writeFluxLogBox.setSelected(fbaParams.writeFluxLog());
		writeMediaLogBox.setSelected(fbaParams.writeMediaLog());
		writeBiomassLogBox.setSelected(fbaParams.writeBiomassLog());
		writeTotalBiomassLogBox.setSelected(fbaParams.writeTotalBiomassLog());
		
		fluxLogField.setText(fbaParams.getFluxLogName());
		mediaLogField.setText(fbaParams.getMediaLogName());
		biomassLogField.setText(fbaParams.getBiomassLogName());
		totalBiomassLogField.setText(fbaParams.getTotalBiomassLogName());
		
		fluxLogRateField.setValue(fbaParams.getFluxLogRate());
		mediaLogRateField.setValue(fbaParams.getMediaLogRate());
		biomassLogRateField.setValue(fbaParams.getBiomassLogRate());
		totalBiomassLogRateField.setValue(fbaParams.getTotalBiomassLogRate());
		
		fluxLogLbl.setEnabled(fbaParams.writeFluxLog());
		fluxLogField.setEnabled(fbaParams.writeFluxLog());
		fluxLogButton.setEnabled(fbaParams.writeFluxLog());
		fluxLogRateLbl.setEnabled(fbaParams.writeFluxLog());
		fluxLogRateField.setEnabled(fbaParams.writeFluxLog());
		
		mediaLogLbl.setEnabled(fbaParams.writeMediaLog());
		mediaLogField.setEnabled(fbaParams.writeMediaLog());
		mediaLogButton.setEnabled(fbaParams.writeMediaLog());
		mediaLogRateLbl.setEnabled(fbaParams.writeMediaLog());
		mediaLogRateField.setEnabled(fbaParams.writeMediaLog());
		
		biomassLogLbl.setEnabled(fbaParams.writeBiomassLog());
		biomassLogField.setEnabled(fbaParams.writeBiomassLog());
		biomassLogButton.setEnabled(fbaParams.writeBiomassLog());
		biomassLogRateLbl.setEnabled(fbaParams.writeBiomassLog());
		biomassLogRateField.setEnabled(fbaParams.writeBiomassLog());
		
		totalBiomassLogLbl.setEnabled(fbaParams.writeTotalBiomassLog());
		totalBiomassLogField.setEnabled(fbaParams.writeTotalBiomassLog());
		totalBiomassLogButton.setEnabled(fbaParams.writeTotalBiomassLog());
		totalBiomassLogRateLbl.setEnabled(fbaParams.writeTotalBiomassLog());
		totalBiomassLogRateField.setEnabled(fbaParams.writeTotalBiomassLog());
		
		matFileLbl.setEnabled(fbaParams.writeMatFile());
		matFileField.setEnabled(fbaParams.writeMatFile());
		matFileButton.setEnabled(fbaParams.writeMatFile());
		matFileRateLbl.setEnabled(fbaParams.writeMatFile());
		matFileRateField.setEnabled(fbaParams.writeMatFile());
		
	}

	@Override
	public void updateState(Comets c)
	{
		// TODO Auto-generated method stub
		
	}
	
	public String getName()
	{
		return NAME;
	}


}
