package org.proofpad;

import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class FilePicker extends JPanel {

	private static final long serialVersionUID = -8959812772239934064L;
	final JTextField pathBox;
	final ArrayList<ChangeListener> changeListeners = new ArrayList<ChangeListener>();
	private final JButton browseButton;

	public FilePicker(final String title, final int mode) {
		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		pathBox = new JTextField();
		pathBox.setMinimumSize(new Dimension(200, 0));
		browseButton = new JButton("Browse...");
		browseButton.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) {
				String path = null;
				if (Main.OSX) {
					FileDialog fc = new FileDialog((Frame) null, title);
					fc.setMode(mode);
					fc.setVisible(true);
					try {
						path = new File(fc.getDirectory(), fc.getFile()).getAbsolutePath();
						pathBox.setText(path);
					} catch (Exception ex) { }
				} else {
					JFileChooser fc = new JFileChooser();
					fc.setSelectedFile(new File(pathBox.getText()));
					int response;
					if (mode == FileDialog.SAVE) {
						response = fc.showSaveDialog(null);
					} else {
						response = fc.showOpenDialog(null);
					}
					if (response == JFileChooser.APPROVE_OPTION) {
						pathBox.setText(fc.getSelectedFile().getAbsolutePath());
					}
				}
				for (ChangeListener cl : changeListeners) {
					cl.stateChanged(new ChangeEvent(FilePicker.this));
				}
			}
		});
		add(pathBox);
		add(Box.createHorizontalStrut(2));
		add(browseButton);
	}
	
	public File getFile() {
		return new File(pathBox.getText());
	}
	
	public String getPath() {
		return pathBox.getText();
	}

	public void addChangeListener(final ChangeListener changeListener) {
		changeListeners.add(changeListener);
		pathBox.getDocument().addDocumentListener(new DocumentListener() {
			@Override public void removeUpdate(DocumentEvent arg0) {
				changeListener.stateChanged(new ChangeEvent(FilePicker.this));
			}
			@Override public void insertUpdate(DocumentEvent arg0) {
				changeListener.stateChanged(new ChangeEvent(FilePicker.this));
			}
			@Override public void changedUpdate(DocumentEvent arg0) {
				changeListener.stateChanged(new ChangeEvent(FilePicker.this));
			}
		});
	}
	
	@Override public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		pathBox.setEnabled(enabled);
		browseButton.setEnabled(enabled);
	}
}
