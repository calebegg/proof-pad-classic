package org.proofpad;

import java.awt.Desktop;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.prefs.Preferences;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.KeyStroke;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultEditorKit;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextAreaEditorKit;
import org.proofpad.PrefsWindow.ToolbarVisibleListener;

/*
 * GNOME HIG: http://developer.gnome.org/hig-book/3.4/menus-standard.html.en
 * Windows HIG: http://msdn.microsoft.com/en-us/library/windows/desktop/aa511502.aspx
 * https://developer.apple.com/library/mac/#documentation/userexperience/Conceptual/AppleHIGuidelines/Menus/Menus.html#//apple_ref/doc/uid/TP30000356-SW3
 */

public class MenuBar extends JMenuBar {
	static final int RECENT_MENU_ITEMS = 10;
	private static final boolean WIN = Main.WIN || Main.FAKE_WINDOWS;
	private static final boolean OSX = Main.OSX && !Main.FAKE_WINDOWS;
	private static final boolean LINUX = !OSX && !WIN;
	private static final boolean TITLE_CASE = !WIN;
	private static final long serialVersionUID = -3469258243341307842L;
	static final int modKey = Main.FAKE_WINDOWS ? KeyEvent.CTRL_DOWN_MASK : Toolkit
			.getDefaultToolkit().getMenuShortcutKeyMask();
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
			@Override public void actionPerformed(ActionEvent e) {
				PrefsWindow.getInstance().setVisible(true);
			}
		};
		
		JMenu menu = new JMenu("File");
		JMenuItem item = new JMenuItem("New");
		item.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) {
				IdeWindow window = new IdeWindow();
				window.setVisible(true);
			}
		});
		item.addActionListener(new UserData.LogUse("newMenuItem"));
		item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, modKey));
		menu.add(item);
		
		item = new JMenuItem("Open...");
		item.addActionListener(OpenAction.instance);
		item.addActionListener(new UserData.LogUse("openMenuItem"));
		item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, modKey));
		menu.add(item);
		recentMenu = new JMenu(applyTitleCase("Open recent"));
		updateRecentMenu();
		menu.add(recentMenu);
		
		if (OSX) menu.addSeparator();
		
		item = new JMenuItem("Close");
		item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, modKey));
		if (parent == null) {
			item.setEnabled(false);
		}
		item.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) {
				if (parent != null && parent.promptIfUnsavedAndClose()) {
					IdeWindow.updateWindowMenu();
				}
			}
		});
		item.addActionListener(new UserData.LogUse("closeMenuItem"));
		menu.add(item);
		
		if (!OSX) menu.addSeparator();
		
		item = new JMenuItem("Save");
		saveItem = item;
		if (parent == null) {
			item.setEnabled(false);
		} else {
			item.addActionListener(parent.saveAction);
			item.addActionListener(new UserData.LogUse("saveMenuItem"));
		}
		item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, modKey));
		menu.add(item);
		
		if (OSX) {
			item = new JMenuItem("Duplicate");
			if (parent == null) {
				item.setEnabled(false);
			} else {
				item.addActionListener(new ActionListener(){
					@Override public void actionPerformed(ActionEvent e) {
						IdeWindow dupWin = new IdeWindow(parent.editor.getText());
						dupWin.setVisible(true);
					}
				});
				item.addActionListener(new UserData.LogUse("duplicateMenuItem"));
			}
			menu.add(item);
		} else {
			item = new JMenuItem("Save As...");
			if (parent == null) {
				item.setEnabled(false);
			} else {
				item.addActionListener(parent.saveAsAction);
				item.addActionListener(new UserData.LogUse("saveAsMenuItem"));
			}
			menu.add(item);
		}

		// TODO: Save a copy... (Linux only)
		// TODO: Revert (Linux only)
		
		menu.addSeparator();
		
		item = new JMenuItem("Print...");
		if (parent == null)  {
			item.setEnabled(false);
		} else {
			item.addActionListener(parent.printAction);
			item.addActionListener(new UserData.LogUse("printMenuItem"));
		}
		item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, modKey));
		menu.add(item);
		
		if (!OSX) {
			menu.addSeparator();
			item = new JMenuItem(WIN ? "Exit" : "Quit");
			item.addActionListener(new ActionListener() {
				@Override public void actionPerformed(ActionEvent e) {
					new UserData.LogUse("quitMenuItem").actionPerformed(e);
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
			item.addActionListener(new UserData.LogUse("undoMenuItem"));
		}
		item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, modKey));
		menu.add(item);
		
		item = new JMenuItem("Redo");
		redo = item;
		if (parent == null) {
			item.setEnabled(false);
		} else {
			item.addActionListener(parent.redoAction);
			item.addActionListener(new UserData.LogUse("redoMenuItem"));
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
		item.addActionListener(new UserData.LogUse("cutMenuItem"));
		menu.add(item);
		
		item = new JMenuItem(new DefaultEditorKit.CopyAction());
		item.setText("Copy");
		item.setEnabled(parent != null);
		item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, modKey));
		item.addActionListener(new UserData.LogUse("copyMenuItem"));
		menu.add(item);
		
		item = new JMenuItem(new DefaultEditorKit.PasteAction());
		item.setText("Paste");
		item.setEnabled(parent != null);
		item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, modKey));
		item.addActionListener(new UserData.LogUse("pasteMenuItem"));
		menu.add(item);
		
		JMenuItem selectAllItem = new JMenuItem(applyTitleCase("Select all"));
		selectAllItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, modKey));
		if (parent == null) {
			selectAllItem.setEnabled(false);
		} else {
			selectAllItem.addActionListener(new ActionListener() {
				@Override public void actionPerformed(ActionEvent arg0) {
					parent.editor.selectAll();
				}
			});
			selectAllItem.addActionListener(new UserData.LogUse("selectAllMenuItem"));
		}
		
		if (!OSX) {
			menu.addSeparator();
			menu.add(selectAllItem);
			menu.addSeparator();
		}
		
		item = new JMenuItem("Delete");
		if (parent == null) {
			item.setEnabled(false);
		} else {
			item.addActionListener(new ActionListener() {
				@Override public void actionPerformed(ActionEvent e) {
					try {
						int selStart = parent.editor.getSelectionStart();
						int selEnd = parent.editor.getSelectionEnd();
						parent.editor.getDocument().remove(selStart, selEnd - selStart);
					} catch (BadLocationException e1) { }
				}
			});
			item.addActionListener(new UserData.LogUse("deleteMenuItem"));
		}
		menu.add(item);
		
		if (OSX) menu.add(selectAllItem);
		
		menu.addSeparator();
		
		item = new JMenuItem("Find");
		if (parent == null) {
			item.setEnabled(false);
		} else {
			item.addActionListener(parent.findAction);
			item.addActionListener(new UserData.LogUse("findMenuItem"));
		}
		item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, modKey));
		menu.add(item);
		
		menu.addSeparator();
		
		item = new JMenuItem(applyTitleCase("Toggle comment"));
		item.setEnabled(parent != null);
		item.addActionListener(new RSyntaxTextAreaEditorKit.ToggleCommentAction());
		item.addActionListener(new UserData.LogUse("toggleCommentMenuItem"));
		item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_SEMICOLON, modKey));
		menu.add(item);
		item = new JMenuItem(applyTitleCase("Reindent selection"));
		item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, modKey));
		if (parent == null) {
			item.setEnabled(false);
		} else {
			item.addActionListener(parent.reindentAction);
			item.addActionListener(new UserData.LogUse("reindentMenuItem"));
		}
		menu.add(item);
		if (LINUX) {
			menu.addSeparator();
			item = new JMenuItem("Preferences...");
			item.addActionListener(prefsAction);
			item.addActionListener(new UserData.LogUse("preferencesMenuItem"));
			menu.add(item);
		}
		add(menu);
		
		/* ******* View Menu ******* */
		menu = new JMenu("View");
		final JMenuItem showToolbarItem;
		if (OSX) {
			showToolbarItem = new JMenuItem(getToolbarLabelPrefix() + "Toolbar");
			PrefsWindow.addToolbarVisibleListener(new ToolbarVisibleListener() {
				@Override public void toolbarVisible(boolean visible) {
					showToolbarItem.setText(getToolbarLabelPrefix() + "Toolbar");
				}
			});
		} else {
			showToolbarItem = new JCheckBoxMenuItem("Toolbar");
			showToolbarItem.setSelected(Prefs.showToolbar.get());
			PrefsWindow.addToolbarVisibleListener(new ToolbarVisibleListener() {
				@Override public void toolbarVisible(boolean visible) {
					showToolbarItem.setSelected(visible);
				}
			});
		}
		showToolbarItem.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) {
				PrefsWindow.toggleToolbarVisible();
				showToolbarItem.setText(getToolbarLabelPrefix() + "Toolbar");
			}
		});
		showToolbarItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T, modKey
				| KeyEvent.ALT_DOWN_MASK));
		menu.add(showToolbarItem);
		item = new JMenuItem(applyTitleCase("Zoom in"));
		if (OSX) {
			item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_PERIOD, modKey
					| KeyEvent.SHIFT_DOWN_MASK));
		} else {
			item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, modKey));
		}
		item.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) {
				Font font = Prefs.font.get();
				Prefs.font.set(font.deriveFont((float) (font.getSize() + 2)));
				PrefsWindow.fireFontChangeEvent();
			}
		});
		menu.add(item);
		item = new JMenuItem(applyTitleCase("Zoom out"));
		if (OSX) {
			item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_COMMA, modKey
					| KeyEvent.SHIFT_DOWN_MASK));
		} else {
			item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, modKey));
		}		item.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) {
				Font font = Prefs.font.get();
				Prefs.font.set(font.deriveFont((float) (font.getSize() - 2)));
				PrefsWindow.fireFontChangeEvent();
			}
		});
		menu.add(item);
		if (LINUX) {
			item = new JMenuItem(applyTitleCase("Normal size"));
			item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_0, modKey));
			item.addActionListener(new ActionListener() {
				@Override public void actionPerformed(ActionEvent e) {
					Font font = Prefs.font.get();
					Prefs.font.set(font.deriveFont((float) Prefs.fontSize.def));
					PrefsWindow.fireFontChangeEvent();
				}
			});
			menu.add(item);
		}
		item = new JMenuItem(applyTitleCase("Full screen"));
		if (OSX) {
			item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.SHIFT_DOWN_MASK
					| KeyEvent.CTRL_DOWN_MASK));
		} else {
			item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0));
		}
		if (parent == null) item.setEnabled(false);
		item.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent arg0) {
				GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().setFullScreenWindow(parent);
			}
		});
		if (!OSX) {
			menu.addSeparator();
			menu.add(item);
		}
		add(menu);
		
		/* ******* Tools/ACL2 Menu ******* */
		menu = new JMenu("Tools"); // Formerly "ACL2"
		item = new JMenuItem("Restart ACL2");
		item.setEnabled(parent != null);
		item.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent arg0) {
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
		item.addActionListener(new UserData.LogUse("restartAcl2MenuItem"));
		menu.add(item);
		item = new JMenuItem("Interrupt ACL2");
		if (parent == null) {
			item.setEnabled(false);
		} else {
			item.addActionListener(new ActionListener() {
				@Override public void actionPerformed(ActionEvent e) {
					if (parent.acl2 != null && parent.acl2.isAlive()) {
						parent.acl2.ctrlc();
					}
				}
			});
			item.addActionListener(new UserData.LogUse("interruptAcl2MenuItem"));
		}
		if (OSX) {
			item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.CTRL_DOWN_MASK));
		} else {
			item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_K, modKey));
		}
		menu.add(item);
		menu.addSeparator();
		item = new JMenuItem(applyTitleCase("Admit next item"));
		if (OSX) {
			item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN,
					modKey | KeyEvent.ALT_DOWN_MASK));
		} else {
			item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, modKey));
		}
		if (parent == null) {
			item.setEnabled(false);
		} else {
			item.addActionListener(parent.admitNextAction);
			item.addActionListener(new UserData.LogUse("admitNextMenuItem"));
		}
		menu.add(item);
		item = new JMenuItem(applyTitleCase("Un-admit one item"));
		if (OSX) {
			item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_UP,
					modKey | KeyEvent.ALT_DOWN_MASK));
		} else {
			item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_UP, modKey));
		}
		if (parent == null) {
			item.setEnabled(false);
		} else {
			item.addActionListener(parent.undoPrevAction);
			item.addActionListener(new UserData.LogUse("unAdmitOneMenuItem"));
		}
		menu.add(item);
		menu.addSeparator();
		item = new JMenuItem(applyTitleCase("Clear REPL scrollback"));
		item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, modKey));
		if (parent == null) {
			item.setEnabled(false);
		} else {
			item.addActionListener(parent.clearReplScrollback);
			item.addActionListener(new UserData.LogUse("clearScrollbackMenuItem"));
		}
		menu.add(item);
		menu.addSeparator();
		item = new JMenuItem("Build");
		item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_B, modKey));
		if (parent == null) {
			item.setEnabled(false);
		} else {
			item.addActionListener(parent.buildAction);
			item.addActionListener(new UserData.LogUse("buildMenuItem"));
		}
		menu.add(item);
		menu.addSeparator();
		item = new JMenuItem(applyTitleCase("Include book..."));
		if (parent == null) {
			item.setEnabled(false);
		} else {
			item.addActionListener(parent.includeBookAction);
			item.addActionListener(new UserData.LogUse("includeBookMenuItem"));
		}
		menu.add(item);
		add(menu);
		if (WIN) {
			item = new JMenuItem("Options...");
			item.addActionListener(prefsAction);
			item.addActionListener(new UserData.LogUse("prefsMenuItem"));
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
		item = new JMenuItem("Look up...");
		item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, modKey | KeyEvent.ALT_DOWN_MASK));
		if (parent == null) {
			item.setEnabled(false);
		} else {
			item.addActionListener(parent.helpAction);
			item.addActionListener(new UserData.LogUse("lookUpMenuItem"));
		}
		menu.add(item);
		item = new JMenuItem("Tutorial");
		if (parent == null) {
			item.setEnabled(false);
		} else {
			item.addActionListener(parent.tutorialAction);
			item.addActionListener(new UserData.LogUse("tutorialMenuItem"));
		}
		menu.add(item);
		menu.addSeparator();
		item = new JMenuItem(applyTitleCase("Report a bug"));
		item.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) {
				try {
					Desktop.getDesktop().browse(new URI("https://github.com/calebegg/proof-pad/issues/new"));
				} catch (Exception ex) { }
			}
		});
		item.addActionListener(new UserData.LogUse("reportBugMenuItem"));
		menu.add(item);
		if (!OSX) {
			item = new JMenuItem("About Proof Pad");
			item.addActionListener(new ActionListener() {
				@Override public void actionPerformed(ActionEvent e) {
					new AboutWindow().setVisible(true);
				}
			});
			item.addActionListener(new UserData.LogUse("aboutMenuItem"));
			menu.add(item);
		}
		item = new JMenuItem(applyTitleCase("Show full ACL2 output"));
		if (parent == null) {
			item.setEnabled(false);
		} else {
			item.addActionListener(parent.showAcl2Output);
			item.addActionListener(new UserData.LogUse("acl2FullOutput"));
		}
		menu.add(item);
		add(menu);
	}

	static String getToolbarLabelPrefix() {
		String prefix = "";
		if (OSX) {
			prefix = "Hide ";
			if (!Prefs.showToolbar.get()) {
				prefix = "Show ";
			}
		}
		return prefix;
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
				@Override public void actionPerformed(ActionEvent e) {
					new IdeWindow(file).setVisible(true);
				}
			});
			item.addActionListener(new UserData.LogUse("recentFileMenuItem"));
			recentMenu.add(item);
		}
		
		recentMenu.setEnabled(recentMenu.getItemCount() > 0);
		
		JMenuItem clearItem = new JMenuItem("Clear Menu");
		clearItem.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) {
				for (int i = 1; i <= RECENT_MENU_ITEMS; i++) {
					prefs.remove("recent" + i);
				}
				for (IdeWindow w : IdeWindow.windows) {
					w.menuBar.updateRecentMenu();
				}
			}
		});
		clearItem.addActionListener(new UserData.LogUse("clearRecentMenuItem"));
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
			@Override public void actionPerformed(ActionEvent arg0) {
				parent.setState(Frame.ICONIFIED);
			}
		});
		item.addActionListener(new UserData.LogUse("minimizeMenuItem"));
		windowMenu.add(item);
		item = new JMenuItem("Zoom");
		item.setEnabled(parent != null);
		item.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) {
				if (parent.getState() == Frame.MAXIMIZED_BOTH) {
					parent.setState(Frame.NORMAL);
				} else {
					parent.setState(Frame.MAXIMIZED_BOTH);
				}
			}
		});
		item.addActionListener(new UserData.LogUse("zoomMenuItem"));
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
				@Override public void actionPerformed(ActionEvent arg0) {
					win.toFront();
					parentItem.setSelected(true);
				}
			});
			item.addActionListener(new UserData.LogUse("windowSwitchMenuItem"));
			windowMenu.add(winItem);
		}
	}
	
	private static String applyTitleCase(String phrase) {
		if (!TITLE_CASE || phrase.isEmpty()) {
			return phrase;
		}
		
		char[] cs = phrase.toCharArray();
		boolean afterSpace = false;
		for (int i = 0; i < cs.length; i++) {
			if (afterSpace && cs[i]>='a' && cs[i]<='z') {
				cs[i] += 'A'-'a';
			}
			afterSpace = cs[i]==' ';
		}
		return new String(cs);
	}

}