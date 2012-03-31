package org.proofpad;

import java.awt.Desktop;
import java.awt.event.*;
import java.net.URI;

import javax.swing.*;
import javax.swing.event.*;

public class Toolbar extends JPanel {
	private static final boolean isMac = IdeWindow.isMac;
	private static final boolean isWindows = IdeWindow.isWindows;
	private static final long serialVersionUID = -333358626303272834L;

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
		button = new JButton();
		button.putClientProperty("JButton.buttonType", "help");
		if (!isMac) {
			button.setText("Help");
		}
		final JButton helpButton = button;
		button.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent event) {
				if (!isWindows) {
					showMenu();
				}
			}
			public void mouseClicked(MouseEvent arg0) {
				if (isWindows) {
					showMenu();
				}
			}
			public void showMenu() {
				JPopupMenu helpMenu = new JPopupMenu();
				helpMenu.addPopupMenuListener(new PopupMenuListener() {
					@Override
					public void popupMenuCanceled(PopupMenuEvent arg0) {
					}

					@Override
					public void popupMenuWillBecomeInvisible(PopupMenuEvent arg0) {
						helpButton.setSelected(false);
					}

					@Override
					public void popupMenuWillBecomeVisible(PopupMenuEvent arg0) {
					}
				});
				JMenuItem item;
				if (parent.editor.getSelectionStart() != parent.editor.getSelectionEnd()) {
					item = new JMenuItem("Look up \""
							+ parent.editor.getSelectedText() + "\"");
					item.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
						}
					});
					helpMenu.add(item);
				}
				item = new JMenuItem("Quick Guide");
				item.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
					}
				});
				helpMenu.add(item);
				item = new JMenuItem("Index");
				item.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						try {
							Desktop.getDesktop()
									.browse(new URI(
											"http://www.cs.utexas.edu/users/moore/acl2/v4-3/acl2-doc-major-topics.html"));
						} catch (Exception e1) {
							e1.printStackTrace();
						}
					}
				});
				helpMenu.add(item);
				item = new JMenuItem("Tutorial");
				item.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
					}
				});
				helpMenu.add(item);
				helpButton.setSelected(true);
				helpMenu.show(helpButton, 0, helpButton.getHeight());
			}
		});
		add(Box.createGlue());
		add(button);
	}
}