package org.proofpad;

import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.util.prefs.Preferences;

import javax.swing.ButtonGroup;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.KeyStroke;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultEditorKit;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextAreaEditorKit;

public class MenuBar extends JMenuBar {
	static final int RECENT_MENU_ITEMS = 10;
	private static final boolean WIN = IdeWindow.WIN || Main.FAKE_WINDOWS;
	private static final boolean OSX = IdeWindow.OSX && !Main.FAKE_WINDOWS;
	private static final boolean TITLE_CASE = !WIN;
	private static final long serialVersionUID = -3469258243341307842L;
	static final int modKey = Main.FAKE_WINDOWS ? KeyEvent.CTRL_DOWN_MASK : Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
	public JMenuItem undo;
	public JMenuItem redo;
	private JMenu windowMenu;
	protected Frame parent;
	public JMenuItem saveItem;
	JMenu recentMenu;
	JMenuItem parentItem;

	public MenuBar(final IdeWindow parent) {
		this.parent = parent;
		
		final ActionListener prefsAction = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (IdeWindow.prefsWindow == null) {
					IdeWindow.prefsWindow = new PrefsWindow();
				}
				IdeWindow.prefsWindow.setVisible(true);
			}
		};
		
		JMenu menu = new JMenu("File");
		JMenuItem item = new JMenuItem("New");
		item.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				IdeWindow window = new IdeWindow();
				window.setVisible(true);
			}
		});
		item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, modKey));
		menu.add(item);
		
		item = new JMenuItem("Open...");
		item.addActionListener(IdeWindow.openAction);
		item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, modKey));
		menu.add(item);
		recentMenu = new JMenu("Open " + (TITLE_CASE  ? 'r' : 'R') + "ecent");
		updateRecentMenu();
		menu.add(recentMenu);
		
		menu.addSeparator();
		
		item = new JMenuItem("Close");
		item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, modKey));
		if (parent == null) {
			item.setEnabled(false);
		}
		item.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (parent != null && parent.promptIfUnsavedAndClose()) {
					IdeWindow.updateWindowMenu();
				}
			}
		});
		menu.add(item);
		
		item = new JMenuItem("Save");
		saveItem = item;
		if (parent == null) {
			item.setEnabled(false);
		} else {
			item.addActionListener(parent.saveAction);
		}
		item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, modKey));
		menu.add(item);
		
		item = new JMenuItem("Duplicate");
		if (parent == null) {
			item.setEnabled(false);
		} else {
			item.addActionListener(new ActionListener(){
				@Override
				public void actionPerformed(ActionEvent e) {
					IdeWindow dupWin = new IdeWindow(parent.editor.getText());
					dupWin.setVisible(true);
				}
			});
		}
		menu.add(item);

		menu.addSeparator();
		
		item = new JMenuItem("Print...");
		if (parent == null)  {
			item.setEnabled(false);
		} else {
			item.addActionListener(parent.printAction);
		}
		item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, modKey));
		menu.add(item);
		
		if (!OSX) {
			menu.addSeparator();
			item = new JMenuItem(WIN ? "Exit" : "Quit");
			item.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					for (IdeWindow w : IdeWindow.windows) {
						w.promptIfUnsavedAndClose();
					}
				}
			});
			if (!WIN) {
				item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, modKey));
			}
			menu.add(item);
		}
		
		add(menu);
		
		/* ******* Edit Menu ******* */
		menu = new JMenu("Edit");
		item = new JMenuItem("Undo");
		undo = item;
		if (parent == null) {
			item.setEnabled(false);
		} else {
			item.addActionListener(parent.undoAction);
		}
		item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, modKey));
		menu.add(item);
		
		item = new JMenuItem("Redo");
		redo = item;
		if (parent == null) {
			item.setEnabled(false);
		} else {
			item.addActionListener(parent.redoAction);
		}
		if (OSX) {
			item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, modKey | KeyEvent.SHIFT_DOWN_MASK));
		} else {
			item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Y, modKey));
		}
		menu.add(item);
		
		menu.addSeparator();
		
		item = new JMenuItem(new DefaultEditorKit.CutAction());
		item.setText("Cut");
		item.setEnabled(parent != null);
		item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, modKey));
		menu.add(item);
		
		item = new JMenuItem(new DefaultEditorKit.CopyAction());
		item.setText("Copy");
		item.setEnabled(parent != null);
		item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, modKey));
		menu.add(item);
		
		item = new JMenuItem(new DefaultEditorKit.PasteAction());
		item.setText("Paste");
		item.setEnabled(parent != null);
		item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, modKey));
		menu.add(item);
		
		item = new JMenuItem("Delete");
		if (parent == null) {
			item.setEnabled(false);
		} else {
			item.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					try {
						int selStart = parent.editor.getSelectionStart();
						int selEnd = parent.editor.getSelectionEnd();
						parent.editor.getDocument().remove(selStart, selEnd - selStart);
					} catch (BadLocationException e1) { }
				}
			});
		}
		menu.add(item);
		
		item = new JMenuItem("Select " + (TITLE_CASE ? 'A' : 'a') + "ll");
		item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, modKey));
		if (parent == null) {
			item.setEnabled(false);
		} else {
			item.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					parent.editor.selectAll();
				}
			});
		}
		
		menu.addSeparator();
		
		item = new JMenuItem("Find");
		if (parent == null) {
			item.setEnabled(false);
		} else {
			item.addActionListener(parent.findAction);
		}
		item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, modKey));
		menu.add(item);
		
		menu.addSeparator();
		
		item = new JMenuItem("Toggle " + (TITLE_CASE ? 'C' : 'c') + "omment");
		item.setEnabled(parent != null);
		item.addActionListener(new RSyntaxTextAreaEditorKit.ToggleCommentAction());
		item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_SEMICOLON, modKey));
		menu.add(item);
		item = new JMenuItem("Reindent " + (TITLE_CASE? 'S' : 's') + "elected " +
				(TITLE_CASE ? 'L' : 'l') + "ines");
		item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, modKey));
		if (parent == null) {
			item.setEnabled(false);
		} else {
			item.addActionListener(parent.reindentAction);
		}
		menu.add(item);
		if (!OSX && !WIN) {
			menu.addSeparator();
			item = new JMenuItem("Preferences...");
			item.addActionListener(prefsAction);
			menu.add(item);
		}
		add(menu);
		/* ******* ACL2 Menu ******* */
		menu = new JMenu("Tools");
		item = new JMenuItem("Restart ACL2");
		item.setEnabled(parent != null);
		item.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if (parent == null) return;
				try {
					parent.acl2.restart();
					parent.proofBar.resetProgress();
				} catch (IOException e) {
					JOptionPane.showMessageDialog(parent, "ACL2 executable not found", "Error",
							JOptionPane.ERROR_MESSAGE);
				}
			}
		});
		menu.add(item);
		item = new JMenuItem("Interrupt ACL2");
		if (parent == null) {
			item.setEnabled(false);
		} else {
			item.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					if (parent.acl2 != null && parent.acl2.isAlive()) {
						parent.acl2.interrupt();
					}
				}
			});
		}
		item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.CTRL_DOWN_MASK));
		menu.add(item);
		item = new JMenuItem("Build");
		item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_B, modKey));
		if (parent == null) {
			item.setEnabled(false);
		} else {
			item.addActionListener(parent.buildAction);
		}
		menu.add(item);
		menu.addSeparator();
		item = new JMenuItem("Include " + (TITLE_CASE ? 'B' : 'b') + "ook...");
		if (parent == null) {
			item.setEnabled(false);
		} else {
			item.addActionListener(parent.includeBookAction);
		}
		menu.add(item);
		add(menu);
		if (WIN) {
			item = new JMenuItem("Options...");
			item.addActionListener(prefsAction);
			menu.addSeparator();
			menu.add(item);
		}
		/* ******* Window Menu (Mac only) ******* */
		if (OSX) {
			menu = new JMenu("Window");
			windowMenu = menu;
			add(menu);
			updateWindowMenu();
		}
		/* ******* */
		menu = new JMenu(OSX ? "Help " : "Help");
		item = new JMenuItem("Index");
		item = new JMenuItem("Quick " + (TITLE_CASE ? 'G' : 'g') + "uide");
		item = new JMenuItem("Look up...");
		item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, modKey));
		if (parent == null) {
			item.setEnabled(false);
		} else {
			item.addActionListener(parent.helpAction);
		}
		menu.add(item);
		item = new JMenuItem("Tutorial");
		if (parent == null) {
			item.setEnabled(false);
		} else {
			item.addActionListener(parent.tutorialAction);
		}
		menu.add(item);
		if (!OSX) {
			item = new JMenuItem("About");
			item.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					new AboutWindow().setVisible(true);
				}
			});
			menu.add(item);
		}
		add(menu);
	}

	public void updateRecentMenu() {
		recentMenu.removeAll();
		final Preferences prefs = Preferences.userNodeForPackage(Main.class);
		for (int i = 1; i <= RECENT_MENU_ITEMS; i++) {
			String path = prefs.get("recent" + i, null);
			if (path == null) {
				break;
			}
			final File file = new File(path);
			JMenuItem item = new JMenuItem(file.getName());
			item.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					new IdeWindow(file).setVisible(true);
				}
				
			});
			recentMenu.add(item);
		}
		if (recentMenu.getItemCount() == 0) {
			recentMenu.setEnabled(false);
		}
		JMenuItem clearItem = new JMenuItem("Clear Menu");
		clearItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				for (int i = 1; i <= RECENT_MENU_ITEMS; i++) {
					prefs.remove("recent" + i);
				}
				for (IdeWindow w : IdeWindow.windows) {
					w.menuBar.updateRecentMenu();
				}
			}
		});
		recentMenu.addSeparator();
		recentMenu.add(clearItem);
	}

	public void updateWindowMenu() {
		if (!OSX) return;
		windowMenu.removeAll();
		JMenuItem item = new JMenuItem();
		item = new JMenuItem("Minimize");
		item.setEnabled(parent != null);
		item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M, modKey));
		item.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				parent.setState(Frame.ICONIFIED);
			}
		});
		windowMenu.add(item);
		item = new JMenuItem("Zoom");
		item.setEnabled(parent != null);
		item.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (parent.getState() == Frame.MAXIMIZED_BOTH) {
					parent.setState(Frame.NORMAL);
				} else {
					parent.setState(Frame.MAXIMIZED_BOTH);
				}
			}
		});
		windowMenu.add(item);
		if (IdeWindow.windows.size() <= 0) {
			return;
		}
		windowMenu.addSeparator();
		final ButtonGroup winItems = new ButtonGroup();
		for (final IdeWindow win : IdeWindow.windows) {
			JRadioButtonMenuItem winItem = new JRadioButtonMenuItem(win.getTitle(), win.isFocused());
			winItems.add(winItem);
			if (win == parent) {
				winItem.setSelected(true);
				parentItem = winItem;
			}
			winItem.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					win.toFront();
					parentItem.setSelected(true);
				}
			});
			windowMenu.add(winItem);
		}
	}

}
