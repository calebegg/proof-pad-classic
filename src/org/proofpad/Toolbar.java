package org.proofpad;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Scanner;

public class Toolbar extends JPanel {
	private static final int BUTTON_GAP = 6;
	private static final boolean OSX = Main.OSX;
	private static final String modKeyStr = (OSX ? "\u2318" : "Ctrl + ");
	private static final long serialVersionUID = -333358626303272834L;
	JButton updateButton;
	final JLabel prerelease;

	public Toolbar(final PPWindow parent) {
		if (OSX) {
			setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
		} else {
			setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
		}
		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		JButton button;
		button = new JButton(new ImageIcon(getClass().getResource("/Icons/Open.png")));
		button.setToolTipText("Open a file for editing. (" + modKeyStr + "O)");
		button.addActionListener(OpenAction.instance);
		button.addActionListener(new UserData.LogUse("openButton"));
		button.putClientProperty("JButton.buttonType", "textured");
		add(button);
		add(Box.createHorizontalStrut(BUTTON_GAP));
		button = new JButton(new ImageIcon(getClass().getResource("/Icons/Save.png")));
		parent.saveButton = button;
		button.setToolTipText("Save the current file. (" + modKeyStr + "S)");
		button.addActionListener(parent.saveAction);
		button.addActionListener(new UserData.LogUse("saveButton"));
		button.putClientProperty("JButton.buttonType", "textured");
		add(button);
		add(Box.createHorizontalStrut(BUTTON_GAP * 2));
		button = new JButton(new ImageIcon(getClass().getResource("/Icons/Undo.png")));
		button.setToolTipText("Undo the last action. (" + modKeyStr + "Z)");
		parent.undoButton = button;
		button.addActionListener(parent.undoAction);
		button.addActionListener(new UserData.LogUse("undoButton"));
		button.putClientProperty("JButton.buttonType", "segmentedTextured");
		button.putClientProperty("JButton.segmentPosition", "first");
		if (OSX) {
			button.setMargin(new Insets(2, 0, 2, 0)); // Workaround for JDK 7 bug w/ segmentedTextured
		}
		button.setEnabled(false);
		add(button);
		button = new JButton(new ImageIcon(getClass().getResource("/Icons/Redo.png")));
		button.setToolTipText("Redo the last action. (" + modKeyStr + (OSX ? "\u21e7Z" : "Y" ) + ")");
		parent.redoButton = button;
		button.addActionListener(parent.redoAction);
		button.addActionListener(new UserData.LogUse("redoButton"));
		button.putClientProperty("JButton.buttonType", "segmentedTextured");
		button.putClientProperty("JButton.segmentPosition", "last");
		if (OSX) {
			button.setMargin(new Insets(2, 0, 2, 0));
		}
		button.setEnabled(false);
		add(button);
		add(Box.createHorizontalStrut(BUTTON_GAP * 2));
		button = new JButton(new ImageIcon(getClass().getResource("/Icons/Build.png")));
		final JButton buildButton = button;
		buildButton.addActionListener(parent.buildAction);
		button.addActionListener(new UserData.LogUse("buildButton"));
		button.setToolTipText("Create an executable from your source file. (" +
				modKeyStr + "B)");
		button.putClientProperty("JButton.buttonType", "textured");
		add(button);
		add(Box.createHorizontalStrut(BUTTON_GAP * 2));
		button = new JButton(new ImageIcon(getClass().getResource("/Icons/Book.png")));
		button.setToolTipText("Include an external book.");
		button.putClientProperty("JButton.buttonType", "textured");
		button.addActionListener(parent.includeBookAction);
		button.addActionListener(new UserData.LogUse("includeBookButton"));
		add(button);
		add(Box.createGlue());
		button = new JButton(new ImageIcon(getClass().getResource("/Icons/Update.png")));
		updateButton = button;
		button.setToolTipText("An update is available.");
		button.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent arg0) {
                Utils.browseTo("http://proofpad.org/");
            }
		});
		button.addActionListener(new UserData.LogUse("updateButton"));
		prerelease = new JLabel("Prerelease: " + Main.RELEASE);
		button.setVisible(false);
		prerelease.setVisible(false);
		button.putClientProperty("JButton.buttonType", "textured");
		checkForUpdate();
		add(prerelease);
		add(button);
	}
	
	public void checkForUpdate() {
		new Thread(new Runnable() {
			@Override public void run() {
				try {
					Scanner s = new Scanner(new URL("http://proofpad.org/CURRENT").openStream());
					int currentVersion = s.nextInt();
					if (currentVersion > Main.RELEASE) {
						updateButton.setVisible(true);
					} else if (currentVersion < Main.RELEASE) {
						prerelease.setVisible(true);
					}
					s.close();
				} catch (MalformedURLException ignored) {
				} catch (IOException ignored) { }
			}
		}).start();
	}
}
