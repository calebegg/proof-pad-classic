package org.proofpad;

import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Scanner;

import javax.swing.*;

public class Toolbar extends JPanel {
	private static final boolean isMac = IdeWindow.isMac;
//	private static final boolean isWindows = IdeWindow.isWindows;
	private static final long serialVersionUID = -333358626303272834L;
private JButton updateButton;

	public Toolbar(final IdeWindow parent) {
		new JPanel();
		if (isMac) {
			setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
		} else {
			setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
		}
		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		JButton button;
		button = new JButton(new ImageIcon(getClass().getResource("/media/open.png")));
		button.setHorizontalTextPosition(SwingConstants.CENTER);
		button.setVerticalTextPosition(SwingConstants.BOTTOM);
		button.setToolTipText("Open a new file for editing");
		button.addActionListener(IdeWindow.openAction);
		button.putClientProperty("JButton.buttonType", "textured");
		add(button);
		add(Box.createHorizontalStrut(4));
		button = new JButton(new ImageIcon(getClass().getResource("/media/save.png")));
		parent.saveButton = button;
		button.addActionListener(parent.saveAction);
		button.putClientProperty("JButton.buttonType", "textured");
		add(button);
		add(Box.createHorizontalStrut(4));
		// button = new JButton("Run in console");
		// button.putClientProperty("JButton.buttonType", "textured");
		// add(button);
		final String buildButtonTooltip = "Create an executable from your source file.";
		button = new JButton(new ImageIcon(getClass().getResource("/media/build.png")));
		final JButton buildButton = button;
		buildButton.addActionListener(parent.buildAction);
		button.setToolTipText(buildButtonTooltip);
		button.putClientProperty("JButton.buttonType", "textured");
		add(button);
		add(Box.createHorizontalStrut(4));
		button = new JButton(new ImageIcon(getClass().getResource("/media/undo.png")));
		parent.undoButton = button;
		button.addActionListener(parent.undoAction);
		button.putClientProperty("JButton.buttonType", "segmentedTextured");
		button.putClientProperty("JButton.segmentPosition", "first");
		button.setEnabled(false);
		add(button);
		button = new JButton(new ImageIcon(getClass().getResource("/media/redo.png")));
		parent.redoButton = button;
		button.addActionListener(parent.redoAction);
		button.putClientProperty("JButton.buttonType", "segmentedTextured");
		button.putClientProperty("JButton.segmentPosition", "last");
		button.setEnabled(false);
		add(button);
		add(Box.createGlue());
		button = new JButton(new ImageIcon(getClass().getResource("/media/update.png")));
		updateButton = button;
		button.setToolTipText("An update is available.");
		button.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				try {
					Desktop.getDesktop().browse(new URI("http://proofpad.org/"));
				} catch (IOException e) {
				} catch (URISyntaxException e) { }
			}
		});
		button.setVisible(false);
		checkForUpdate();
		button.putClientProperty("JButton.buttonType", "textured");
		add(button);
		add(Box.createHorizontalStrut(4));
		button = new JButton();
		button.putClientProperty("JButton.buttonType", "help");
		if (!isMac) {
			button.setText("Help");
		}
		button.addActionListener(parent.tutorialAction);
		add(button);
	}
	
	public void checkForUpdate() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Scanner s = new Scanner(new URL("http://proofpad.org/CURRENT").openStream());
					if (s.nextInt() > Main.RELEASE) {
						updateButton.setVisible(true);
					}
				} catch (MalformedURLException e) {
				} catch (IOException e) { }
			}
		}).start();
	}
}
