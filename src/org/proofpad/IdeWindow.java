package org.proofpad;

import java.util.*;
import java.util.List;
import java.awt.*;
import java.awt.event.*;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.net.URLDecoder;
import java.util.prefs.Preferences;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.undo.UndoManager;

import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rtextarea.SearchContext;
import org.fife.ui.rtextarea.SearchEngine;
import org.proofpad.Acl2.OutputEvent;
import org.proofpad.Acl2.OutputEventListener;
import org.proofpad.PrefsWindow.FontChangeListener;
import org.proofpad.Repl.MsgType;


public class IdeWindow extends JFrame {
	public static final boolean isMac = System.getProperty("os.name").indexOf("Mac") != -1;
	public static final boolean isWindows =
			System.getProperty("os.name").toLowerCase().indexOf("windows") != -1;
	public static final Color transparent = new Color(1f, 1f, 1f, 0f);
	private static JFileChooser fc = new JFileChooser();
	static {
		fc.addChoosableFileFilter(new FileFilter() {
			@Override
			public String getDescription() {
				return "Lisp files";
			}
			@Override
			public boolean accept(File f) {
				return f.getName().endsWith(".lisp") || f.getName().endsWith(".lsp")
						|| f.getName().endsWith(".acl2");
			}
		});
	}
	public static final ActionListener openAction = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			File file;
			if (isMac) {
				FileDialog fc = new FileDialog((Frame)null, "Open file");
				fc.setFilenameFilter(new FilenameFilter() {
					@Override
					public boolean accept(File dir, String name) {
						return name.endsWith(".lisp") || name.endsWith(".lsp")
								|| name.endsWith(".acl2");
					}
				});
				fc.setVisible(true);
				String filename = fc.getFile();
				file = filename == null ? null : new File(fc.getDirectory(), filename);
			} else {
				int response = fc.showOpenDialog(null);
				file = response == JFileChooser.APPROVE_OPTION ? fc.getSelectedFile() : null;
			}
			if (file != null) {
				IdeWindow window = new IdeWindow(file);
				if (windows.size() == 1 &&
						windows.get(0).isSaved &&
						windows.get(0).openFile == null) {
					windows.get(0).promptIfUnsavedAndClose();
				}
				window.setVisible(true);
			}
		}
	};

	private static final long serialVersionUID = -7435370608709935765L;
	public static PrefsWindow prefsWindow = null;
	static List<IdeWindow> windows = new LinkedList<IdeWindow>();
	private static int untitledCount = 1;
	
	private File openFile;
	private boolean isSaved = true;
	private IdeWindow that = this;
	private File workingDir;
	private Acl2Parser parser;
	private Repl repl;
	Toolbar toolbar;

	final CodePane editor;
	JButton undoButton;
	JButton redoButton;
	Token tokens;
	Acl2 acl2;
	ProofBar proofBar;
	MenuBar menuBar;
	JScrollPane jsp;
	JButton saveButton;

	public ActionListener saveAction;
	public ActionListener undoAction;
	public ActionListener redoAction;
	public ActionListener printAction;
	public ActionListener findAction;
	public ActionListener buildAction;
	protected int dY;
	protected int dX;

	public IdeWindow() {
		this((File)null);
	}

	public IdeWindow(String text) {
		this((File)null);
		editor.setText(text);
	}

	public IdeWindow(File file) {
		super();
		windows.add(this);
		openFile = file;
		workingDir = openFile == null ? null : openFile.getParentFile();
		setLayout(new BorderLayout());
		final IdeWindow that = this;

		final JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true);
		final JPanel splitTop = new JPanel();
		splitTop.setLayout(new BorderLayout());
		split.setTopComponent(splitTop);
		split.setOneTouchExpandable(true);
		split.setResizeWeight(1);
		add(split, BorderLayout.CENTER);
		jsp = new JScrollPane();
		jsp.setBorder(BorderFactory.createEmptyBorder());
		jsp.setViewportBorder(BorderFactory.createEmptyBorder());
		this.getRootPane().setBorder(BorderFactory.createEmptyBorder());
		splitTop.add(jsp, BorderLayout.CENTER);

		final Preferences prefs = Preferences.userNodeForPackage(Main.class);
		
		final String acl2Path;
		if (prefs.getBoolean("customacl2", false)) {
			acl2Path = prefs.get("acl2Path", "");
		} else {
			if (isWindows) {
				// HACK: oh no oh no oh no
				acl2Path = "C:\\PROGRA~1\\PROOFP~1\\acl2\\run_acl2.exe";
			} else {
				String maybeAcl2Path = "";
				try {
					String jarPath = getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
					jarPath = URLDecoder.decode(jarPath, "UTF-8");
					File jarFile = new File(jarPath);
					maybeAcl2Path = jarFile.getParent() + "/acl2/run_acl2";
					maybeAcl2Path = maybeAcl2Path.replaceAll(" ", "\\\\ ");
				} catch (NullPointerException e) {
					System.err.println("Built-in ACL2 not found.");
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
				acl2Path = maybeAcl2Path;
			}
		}
		System.out.println(acl2Path);

		acl2 = new Acl2(acl2Path, workingDir);
		proofBar = new ProofBar(acl2);
		editor = new CodePane(proofBar);
		jsp.setViewportView(editor);
		jsp.setRowHeaderView(proofBar);
		repl = new Repl(this, acl2, editor);
		proofBar.setLineHeight(editor.getLineHeight());
		final IdeDocument doc = new IdeDocument(proofBar);
		editor.setDocument(doc);
		parser = new Acl2Parser(editor, workingDir, new File(acl2Path).getParentFile());
		editor.addParser(parser);
		try {
			acl2.initialize();
			acl2.start();
		} catch (IOException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(that, "ACL2 executable not found",
					"Error", JOptionPane.ERROR_MESSAGE);
		}

		undoAction = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				editor.undoLastAction();
				fixUndoRedoStatus();
				editor.requestFocus();
			}
		};
		
		redoAction = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				editor.redoLastAction();
				fixUndoRedoStatus();
				editor.requestFocus();
			}
		};
		
		saveAction = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				saveFile();
			}
		};
		
		printAction = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				PrinterJob job = PrinterJob.getPrinterJob();
				job.setPrintable(editor);
				boolean doPrint = job.printDialog();
				if (doPrint) {
					try {
						job.print();
					} catch (PrinterException e1) {
						e1.printStackTrace();
					}
				}
			}
		};
		
		buildAction = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if (!that.saveFile()) {
					JOptionPane.showMessageDialog(that,
							"Save the current file in order to build", "Error",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
				final Acl2 builder = new Acl2(acl2Path, workingDir);
				try {
					builder.initialize();
					builder.start();
					builder.setOutputEventListener(new OutputEventListener() {
						@Override
						public void handleOutputEvent(OutputEvent outputEvent) {
							System.out.println(outputEvent.output);
						}
					});
					builder.admit(":q\n", null);
					builder.admit("(load \"" + openFile.getAbsolutePath()
							+ "\" )", null);
					builder.admit(
							"(defun __main__ () (acl2::main acl2::state))",
							null);
					final String filename;
					if (isMac) {
						FileDialog fc = new FileDialog(that, "Save Executable...");
						fc.setMode(FileDialog.SAVE);
						fc.setDirectory(openFile.getPath());
						fc.setFile(openFile.getName().split("\\.")[0]
								+ (isWindows ? ".exe" : ""));
						fc.setVisible(true);
						filename = fc.getDirectory() + fc.getFile();
					} else {
						JFileChooser fc = new JFileChooser();
						fc.showSaveDialog(that);
						filename = fc.getSelectedFile().getAbsolutePath();
					}
					if (filename == null) {
						builder.terminate();
						return;
					}
					builder.admit(
							"(ccl:save-application \""
							+ filename
							+ "\" :toplevel-function #'__main__\n"
							+ ":prepend-kernel t)",
							new Acl2.Callback() {
								public boolean run(boolean success,
										String response) {
									builder.terminate();
									// buildButton.setIcon(null);
									// buildButton.setText("Build");
									repl.displayResult("Successfully built "
											+ filename, MsgType.INFO);
									return false;
								}
							});
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		};
		final JPanel findBar = new JPanel();
		final JTextField searchField = new JTextField();
		findAction = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				System.out.println(splitTop.getComponentCount());
				if (splitTop.getComponentCount() == 1) {
					splitTop.add(findBar, BorderLayout.NORTH);
				} else {
					splitTop.remove(findBar);
					editor.clearMarkAllHighlights();
				}
				splitTop.revalidate();
				searchField.setText(editor.getSelectedText());
				searchField.requestFocusInWindow();
			}
		};

		/*
		 * TODO: "Provide a Find submenu, if appropriate. You might want to
		 * include a Find submenu if find is important functionality in your
		 * app. Typically, a Find submenu contains Find (Command-F), Find Next
		 * (Command-G), Find Previous (Command-Shift-G), Use Selection for Find
		 * (Command-E), and Jump to Selection (Command-J). If you include a Find
		 * submenu, the Find menu item name should not include an ellipsis
		 * character." -- Apple HIG
		 */
		findBar.setLayout(new BoxLayout(findBar, BoxLayout.X_AXIS));
		findBar.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createMatteBorder(0, 0, 1, 0, Color.BLACK),
				BorderFactory.createEmptyBorder(0, 5, 0, 5)));
		findBar.setBackground(new Color(.9f, .9f, .9f));
		if (isMac) {
			// TODO: make this look more like safari?
			// findBar.add(Box.createGlue());
			// searchField.setPreferredSize(new Dimension(300, 0));
		} else {
			findBar.add(new JLabel("Find: "));
		}
		searchField.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				searchFor(searchField.getText(), true);
			}
		});
		searchField.addKeyListener(new KeyListener() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
					findAction.actionPerformed(null);
				}
			}
			
			public void keyReleased(KeyEvent arg0) {
			}
			
			public void keyTyped(KeyEvent e) {
				char c = e.getKeyChar();
				if (c == KeyEvent.CHAR_UNDEFINED || c == '\n' || c == '\b'
						|| e.isAltDown() || e.isMetaDown() || e.isControlDown()) {
					return;
				}
				if (prefs.getBoolean("incsearch", true)) {
					editor.markAll(searchField.getText() + c, false, false,
							false);
					// textarea.setCaretPosition(textarea.getSelectionStart());
					// searchFor(searchField.getText() + c, true);
				}
			}
		});
		searchField.putClientProperty("JTextField.variant", "search");
		findBar.add(searchField);
		JButton forward = new JButton(new ImageIcon("media/find_down.png"));
		forward.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				searchFor(searchField.getText(), true);
			}
		});
		JButton back = new JButton(new ImageIcon("media/find_up.png"));
		back.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				searchFor(searchField.getText(), false);
			}
		});
		back.putClientProperty("JButton.buttonType", "segmentedRoundRect");
		forward.putClientProperty("JButton.buttonType", "segmentedRoundRect");
		back.putClientProperty("JButton.segmentPosition", "first");
		forward.putClientProperty("JButton.segmentPosition", "last");
		findBar.add(back);
		findBar.add(forward);
		findBar.add(Box.createHorizontalStrut(4));
		JButton done = new JButton("done");
		done.putClientProperty("JButton.buttonType", "roundRect");
		done.addActionListener(findAction);
		findBar.add(done);
		
		toolbar = new Toolbar(this);
		menuBar = new MenuBar(this);
		add(toolbar, BorderLayout.NORTH);
		setJMenuBar(menuBar);
		
		// Preferences
		PrefsWindow.addFontChangeListener(new FontChangeListener() {
			@Override
			public void fontChanged(Font font) {
				editor.setFont(font);
				repl.setFont(font);
				proofBar.setLineHeight(editor.getLineHeight());
			}
		});
		PrefsWindow.addWidthGuideChangeListener(new PrefsWindow.WidthGuideChangeListener() {
			@Override
			public void widthGuideChanged(int value) {
				editor.widthGuide = value;
				editor.repaint();
			}
		});
		PrefsWindow.addToolbarVisibleListener(new PrefsWindow.ToolbarVisibleListener() {
			@Override
			public void toolbarVisible(boolean visible) {
				toolbar.setVisible(visible);
				getRootPane().revalidate();
			}
		});
		
		///// Event Listeners /////

		proofBar.addReadOnlyIndexChangeListener(new ProofBar.ReadOnlyIndexChangeListener() {
			
			@Override
			public void readOnlyIndexChanged(int newIndex) {
				String lineSep = System.getProperty("line.separator");
				if (editor.getLastVisibleOffset() - lineSep.length() <= newIndex) {
					editor.append(lineSep);
				}
				if (editor.getCaretPosition() <= newIndex + 1) {
					editor.setCaretPosition(newIndex + 2);
				}
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						editor.repaint();
					}
				});
			}
		});
				
		editor.getCaret().addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				if (proofBar.getReadOnlyIndex() == -1)
					return;
				if (editor.getCaretPosition()
						- editor.getCaretOffsetFromLineStart() < proofBar.getReadOnlyIndex() + 2) {
					editor.setCaretColor(transparent);
				} else {
					editor.setCaretColor(Color.BLACK);
				}
			}
		});
		
		editor.SetUndoManagerCreatedListener(new CodePane.UndoManagerCreatedListener() {
			public void undoManagerCreated(UndoManager undoManager) {
				proofBar.undoManager = undoManager;
			}
		});
		
		doc.addDocumentListener(new DocumentListener() {
			private void update(DocumentEvent e) {
				// TODO: figure out how to change the delay on the parser manager.
				List<Expression> exps = SExpUtils.topLevelExps(doc);
				proofBar.adjustHeights((LinkedList<Expression>) exps);
				fixUndoRedoStatus();
				adjustMaximizedBounds();
				setSaved(false);
			}
			@Override
			public void changedUpdate(DocumentEvent e) {
				update(e);
			}
			@Override
			public void insertUpdate(DocumentEvent e) {
				update(e);
			}
			@Override
			public void removeUpdate(DocumentEvent e) {
				update(e);
			}
		});

		addWindowListener(new WindowAdapter() {
			@Override
			public void windowActivated(WindowEvent arg0) {
				proofBar.repaint();
			}
			@Override
			public void windowClosing(WindowEvent arg0) {
				if (that.promptIfUnsavedAndClose()) {
					updateWindowMenu();
				}
			}
			@Override
			public void windowDeactivated(WindowEvent e) {
				proofBar.repaint();
			}
			@Override
			public void windowOpened(WindowEvent arg0) {
				proofBar.repaint();
			}
		});

		Toolkit.getDefaultToolkit().setDynamicLayout(false);
		split.setBottomComponent(repl);
		repl.setHeightChangeListener(new Repl.HeightChangeListener() {
			@Override
			public void heightChanged(int delta) {
				// :-( http://lists.apple.com/archives/java-dev/2009/Aug/msg00087.html
				// setSize(new Dimension(getWidth(), getHeight() + delta));
				split.setDividerLocation(split.getDividerLocation() - delta);
			}
		});

		if (openFile != null) {
			openAndDisplay(openFile);
		} else {
			setTitle("untitled"
				+ (untitledCount == 1 ? "" : " " + (untitledCount))
				+ (!isMac ? " - Proof Pad" : ""));
			untitledCount++;
		}
		updateWindowMenu();

		setPreferredSize(new Dimension(600, 600));
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		setMinimumSize(new Dimension(550, 300));

		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
		if (isWindows || isMac) {
			setLocation((screen.width - 600) / 2 + 50 * windows.size(),
					(screen.height - 800) / 2 + 50 * windows.size());
		}

		fixUndoRedoStatus();
		split.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent pce) {
				adjustMaximizedBounds();
			}
		});
		addComponentListener(new ComponentAdapter() {
			@Override
			public void componentMoved(ComponentEvent arg0) {
				adjustMaximizedBounds();
			}			
		});
		addWindowFocusListener(new WindowFocusListener() {
			@Override
			public void windowGainedFocus(WindowEvent e) {
				proofBar.repaint();
			}
			@Override
			public void windowLostFocus(WindowEvent arg0) {
				proofBar.repaint();
			}
		});
		
		if (isMac) {
			// Adapted from http://explodingpixels.wordpress.com/2008/05/03/sexy-swing-app-the-unified-toolbar-now-fully-draggable/
			toolbar.addMouseListener(new MouseAdapter() {
	            @Override
	            public void mousePressed(MouseEvent e) {
	                Point clickPoint = new Point(e.getPoint());
	                SwingUtilities.convertPointToScreen(clickPoint, toolbar);

	                dX = clickPoint.x - that.getX();
	                dY = clickPoint.y - that.getY();
	            }
	        });
			toolbar.addMouseMotionListener(new MouseMotionAdapter() {
				@Override
				public void mouseDragged(MouseEvent e) {
	                Point dragPoint = new Point(e.getPoint());
	                SwingUtilities.convertPointToScreen(dragPoint, toolbar);

	                that.setLocation(dragPoint.x - dX, dragPoint.y - dY);
	            }
			});
		}
		
		adjustMaximizedBounds();
		pack();
		editor.requestFocus();
		split.setDividerLocation(.65);
		// System.out.println(jsp.getViewport().getExtentSize());

	}

	static void updateWindowMenu() {
		for (IdeWindow window : windows) {
			MenuBar bar = window.menuBar;
			if (bar != null) {
				bar.updateWindowMenu();
			}
		}
	}

	public void fixUndoRedoStatus() {
		// TODO: Show what you're undoing in the menu item or tooltip.
		boolean canUndo = editor.canUndo();
		boolean canRedo = editor.canRedo();
		undoButton.setEnabled(canUndo);
		redoButton.setEnabled(canRedo);
		menuBar.undo.setEnabled(canUndo);
		if (canUndo) {
			menuBar.undo.setText("Undo");
		} else {
			menuBar.undo.setText("Can't Undo");
		}
		menuBar.redo.setEnabled(canRedo);
		if (canRedo) {
			menuBar.redo.setText("Redo");
		} else {
			menuBar.redo.setText("Can't Redo");
		}
	}

	boolean promptIfUnsavedAndClose() {
		return promptIfUnsavedAndQuit(null);
	}
	boolean promptIfUnsavedAndQuit(Iterator<IdeWindow> ii) {
		int response = -1;
		if (!isSaved) {
			// TODO: Man it would be awesome if this could be more like the Mac
			// version of this function.
			response = JOptionPane.showOptionDialog(this,
					"You have unsaved changes. Do"
							+ " you want to save before closing?",
					"Unsaved changes", JOptionPane.DEFAULT_OPTION,
					JOptionPane.WARNING_MESSAGE, null, new String[] { "Save",
							"Don't Save", "Cancel" }, "Save");
		}
		if (response == 0) {
			saveFile();
		}
		if (response == 1 || isSaved) {
			dispose();
			acl2.terminate();
			if (ii != null) {
				ii.remove();
			} else {
				windows.remove(that);
			}
			if (windows.size() == 0 && !isMac) {
				System.exit(0);
			}
			return true;
		}
		return false;
	}

	private boolean saveFile() {
		if (openFile == null) {
			File file = null;
			if (isMac) {
				FileDialog fc = new FileDialog(this, "Save As...");
				fc.setMode(FileDialog.SAVE);
				fc.setVisible(true);
				String filename = fc.getFile();
				if (filename != null) {
					if (filename.indexOf('.') == -1) {
						filename += ".lisp";
					}
					file = new File(fc.getDirectory(), filename);
				}
			} else {
				int response = fc.showSaveDialog(this);
				file = response == JFileChooser.APPROVE_OPTION ? fc.getSelectedFile() : null;
			}
			if (file != null) {
				if (file.exists()) {
					int answer = JOptionPane.showConfirmDialog(this,
							file.getName() + " already exists. Do you want to replace it?", "",
							JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
					if (answer == JOptionPane.NO_OPTION) {
						return false;
					}
				}
				openFile = file;
				File oldWorkingDir = workingDir;
				workingDir = openFile.getParentFile();
				acl2.workingDir = workingDir;
				if (oldWorkingDir == null || !oldWorkingDir.equals(workingDir)) {
					try {
						if (repl != null) {
							repl.displayResult("Restarting ACL2 in new working directory",
									MsgType.INFO);
						}
						acl2.restart();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				parser.workingDir = workingDir;
			} else {
				return false;
			}
		}
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(openFile));
			bw.write(editor.getText());
			bw.close();
			setSaved(true);
			getRootPane().putClientProperty("Window.documentFile", openFile);
			setTitle(openFile.getName() + (!isMac ? " - Proof Pad" : ""));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return true;
	}

	public void setSaved(boolean isSaved) {
		this.isSaved = isSaved;
		this.getRootPane().putClientProperty("Window.documentModified",
				!isSaved);
		saveButton.setEnabled(!isSaved);
		menuBar.saveItem.setEnabled(!isSaved);
	}

	protected void openAndDisplay(File file) {
		getRootPane().putClientProperty("Window.documentFile", file);
		setTitle(file.getName() + (!isMac ? " - Proof Pad" : ""));
		openFile = file;
		// build.setToolTipText("Create an executable from " +
		// openFile.getName());
		Scanner scan = null;
		try {
			scan = new Scanner(openFile);
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}
		String content = scan.useDelimiter("\\Z").next();
		editor.setText(content);
		editor.setCaretPosition(0);
		java.util.List<Expression> exps = SExpUtils
				.topLevelExps((IdeDocument) editor.getDocument());
		proofBar.adjustHeights((LinkedList<Expression>) exps);
		setSaved(true);
		adjustMaximizedBounds();
		updateWindowMenu();
		// Update recent files
		Preferences prefs = Preferences.userNodeForPackage(Main.class);
		int downTo = 10;
		for (int i = 1; i <= 10; i++) {
			String temp = prefs.get("recent" + i, "");
			if (temp.equals(openFile.getAbsolutePath())) {
				downTo = i;
				break;
			}
		}
		for (int i = downTo; i >= 2; i--) {
			String maybeNext = prefs.get("recent" + (i - 1), null);
			if (maybeNext == null)
				continue;
			prefs.put("recent" + i, maybeNext);
		}
		prefs.put("recent1", file.getAbsolutePath());
		for (IdeWindow w : windows) {
			w.menuBar.updateRecentMenu();
		}

	}

	public void adjustMaximizedBounds() {
		if (!isMac) return;
		Dimension visibleSize = jsp.getViewport().getExtentSize();
		Dimension textSize = editor.getPreferredScrollableViewportSize();
		int maxWidth = Math.max(getWidth() - visibleSize.width + textSize.width
				+ proofBar.getWidth(), 550);
		int maxHeight = getHeight() - visibleSize.height + Math.max(textSize.height, 200);
		setMaximizedBounds(new Rectangle(getLocation(), new Dimension(
				maxWidth + 5, maxHeight + 5)));
	}

	private void searchFor(String text, boolean forward) {
		editor.clearMarkAllHighlights();
		SearchContext sc = new SearchContext();
		sc.setSearchFor(text);
		sc.setSearchForward(forward);
		boolean found = SearchEngine.find(editor, sc);
		if (!found) {
			int userCaret = editor.getCaretPosition();
			editor.setCaretPosition(forward ? 0 : editor.getText().length());
			found = SearchEngine.find(editor, sc);
			if (!found) {
				editor.setCaretPosition(userCaret);
				Toolkit.getDefaultToolkit().beep();
			}
		}
	}
}
