package org.proofpad;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.Font;
import java.awt.IllegalComponentStateException;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.prefs.Preferences;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.undo.UndoManager;

import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rtextarea.Gutter;
import org.proofpad.Acl2.ErrorListener;
import org.proofpad.InfoBar.InfoButton;
import org.proofpad.PrefsWindow.FontChangeListener;
import org.proofpad.Repl.MsgType;

public class PPWindow extends JFrame {
	static final Color activeToolbar = new Color(.8627f, .8627f, .8627f);
	static final Color inactiveToolbar = new Color(.9529f, .9529f, .9529f);
	public static final Color transparent = new Color(1f, 1f, 1f, 0f);
	static JFileChooser fc = new JFileChooser();
	static {
		fc.addChoosableFileFilter(new FileFilter() {
			@Override
			public String getDescription() {
				return "Lisp files";
			}
			@Override
			public boolean accept(File f) {
				if (f.isDirectory()) {
					return true;
				}
				return f.getName().endsWith(".lisp") || f.getName().endsWith(".lsp")
						|| f.getName().endsWith(".acl2");
			}
		});
	}
	private static final long serialVersionUID = -7435370608709935765L;
	private static final int WINDOW_GAP = 10;
	static List<PPWindow> windows = new LinkedList<PPWindow>();
	private static int untitledCount = 1;
	private FileMonitor monitor;
	File openFile;
	boolean isSaved = true;
	private File workingDir;
	private Acl2Parser parser;
	Repl repl;
	Toolbar toolbar;

	final CodePane editor;
	JButton undoButton;
	JButton redoButton;
	Token tokens;
	Acl2 acl2;
	ProofBar proofBar;
	MenuBar menuBar;
	JScrollPane editorScroller;
	JButton saveButton;

	ActionListener saveAction;
	ActionListener undoAction;
	ActionListener redoAction;
	ActionListener printAction;
	ActionListener findAction;
	ActionListener buildAction;
	ActionListener includeBookAction;
	ActionListener helpAction;
	ActionListener reindentAction;
	ActionListener admitNextAction;
	ActionListener undoPrevAction;
	ActionListener clearReplScrollback;
	ActionListener tutorialAction;
	ActionListener saveAsAction;
	ActionListener showAcl2Output;

	MoreBar moreBar;
	Gutter gutter;
	Runnable afterPreview;
	JPanel westPanel;
	public OutputWindow outputWindow;
	boolean findBarIsOpen;
	private JPanel splitTop;
	boolean userLocation;
	InfoBar infoBar;

	public PPWindow() {
		this((File)null);
	}

	public PPWindow(String text) {
		this((File)null);
		editor.setText(text);
	}

	public PPWindow(File file) {
		super();
//		FullScreenUtilities.setWindowCanFullScreen(this, true);
		getRootPane().putClientProperty("apple.awt.brushMetalLook", true);
		
		windows.add(this);
		openFile = file;
		workingDir = openFile == null ? null : openFile.getParentFile();
		outputWindow = new OutputWindow(this);
		final JPanel splitMain = new JPanel();
		splitMain.setLayout(new BorderLayout());
		add(splitMain);
		splitTop = new JPanel();
		splitTop.setLayout(new BorderLayout());
		final JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true);
		split.setTopComponent(splitTop);
		split.setResizeWeight(1);
		splitMain.add(split, BorderLayout.CENTER);
		editorScroller = new JScrollPane();
		editorScroller.setBorder(BorderFactory.createEmptyBorder());
		editorScroller.setViewportBorder(BorderFactory.createEmptyBorder());
		getRootPane().setBorder(BorderFactory.createEmptyBorder());
		splitTop.add(editorScroller, BorderLayout.CENTER);

		List<String> acl2Paths = new ArrayList<String>();
		if (Prefs.customAcl2.get()) {
			acl2Paths.add(Prefs.acl2Path.get());
		} else {
			if (Main.WIN) {
				// HACK: oh no oh no oh no
				acl2Paths.add("C:\\PROGRA~1\\PROOFP~1\\acl2\\run_acl2.exe");
				acl2Paths.add("C:\\PROGRA~2\\PROOFP~1\\acl2\\run_acl2.exe");
				acl2Paths.add("C:\\PROGRA~3\\PROOFP~1\\acl2\\run_acl2.exe");
			}
			try {
				String maybeAcl2Path = "";
				String jarPath = Main.getJarPath();
				File jarFile = new File(jarPath);
				String sep = System.getProperty("file.separator");
				maybeAcl2Path = jarFile.getParent() + sep + "acl2" + sep + "run_acl2";
				maybeAcl2Path = maybeAcl2Path.replaceAll(" ", "\\\\ ");
				acl2Paths.add(maybeAcl2Path);
			} catch (NullPointerException e) {
				System.err.println("Built-in ACL2 not found.");
			}
		}
		
		parser = new Acl2Parser(workingDir, null);
		acl2 = new Acl2(acl2Paths, workingDir);
		acl2.setErrorListener(new ErrorListener() {
			@Override public InfoBar handleError(String msg, InfoButton[] btns) {
				InfoBar ib = new InfoBar(msg, btns);
				setInfoBar(ib);
				return ib;
			}
		});
		moreBar = new MoreBar(this);
		proofBar = new ProofBar(acl2, moreBar);
		editor = new CodePane(proofBar);
		final JPanel editorContainer = new JPanel();
		editorContainer.setBackground(Color.WHITE);
		editorContainer.setLayout(new BorderLayout());
		westPanel = new JPanel() {
			private static final long serialVersionUID = 31966860737290397L;

			@Override public Dimension getPreferredSize() {
				return new Dimension(super.getPreferredSize().width,
						editor.getHeight());
			}
		};
		westPanel.setLayout(new BoxLayout(westPanel, BoxLayout.X_AXIS));
		westPanel.setBackground(Color.WHITE);
		westPanel.add(proofBar);
		gutter = new Gutter(editor);
		gutter.setBackground(Color.WHITE);
		westPanel.add(gutter);
		editorScroller.setRowHeaderView(westPanel);
//		editorScroller.setVerticalScrollBar(moreBar);
		splitTop.add(moreBar, BorderLayout.LINE_END);
		editorContainer.add(editor, BorderLayout.CENTER);
		editorScroller.getVerticalScrollBar().addAdjustmentListener(new AdjustmentListener() {
			@Override public void adjustmentValueChanged(AdjustmentEvent e) {
				moreBar.setScrollbarVal(editorScroller.getVerticalScrollBar().getValue());
				moreBar.repaint();
			}
		});
		editorScroller.setViewportView(editorContainer);
		editorScroller.getVerticalScrollBar().setUnitIncrement(editor.getLineHeight());
		editorScroller.getHorizontalScrollBar().setUnitIncrement(editor.getLineHeight());
		helpAction = editor.getHelpAction();
		repl = new Repl(this, acl2, editor);
		proofBar.setLineHeight(editor.getLineHeight());
		final PPDocument doc = new PPDocument(proofBar, editor.getCaret());
		editor.setDocument(doc);
		parser.addParseListener(new Acl2Parser.ParseListener() {
			@Override
			public void wasParsed() {
				proofBar.admitUnprovenExps();
			}
		});
		editor.addParser(parser);
		try {
			acl2.initialize();
			acl2.start();
		} catch (IOException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(this, "ACL2 executable not found",
					"Error", JOptionPane.ERROR_MESSAGE);
		}
		if (acl2.getAcl2Path() != null) {
			parser.setAcl2Dir(new File(acl2.getAcl2Path()).getParentFile());
		}

		undoAction = new ActionListener() {
			@Override public void actionPerformed(ActionEvent arg0) {
				editor.undoLastAction();
				fixUndoRedoStatus();
				editor.requestFocus();
			}
		};
		
		redoAction = new ActionListener() {
			@Override public void actionPerformed(ActionEvent arg0) {
				editor.redoLastAction();
				fixUndoRedoStatus();
				editor.requestFocus();
			}
		};
		
		saveAction = new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) {
				saveFile();
			}
		};
		
		saveAsAction = new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) {
				saveFileAs();
			}
		};
		
		printAction = new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) {
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
			@Override public void actionPerformed(ActionEvent arg0) {
				if (!saveFile()) {
					JOptionPane.showMessageDialog(PPWindow.this,
							"Save the current file in order to build", "Build did not complete",
							JOptionPane.INFORMATION_MESSAGE);
					return;
				}

				final BuildWindow builder = new BuildWindow(openFile, acl2.getAcl2Path());

				builder.setVisible(true);
				builder.build();
			}
		};
		
		includeBookAction = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				BookViewer viewer = new BookViewer(PPWindow.this);
				viewer.setVisible(true);
			}
		};
		
		admitNextAction = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				proofBar.admitNextForm();
			}
		};
		
		undoPrevAction = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				proofBar.undoOneItem();
			}
		};
		
		clearReplScrollback = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				repl.clearScrollback();
			}
		};
		
		reindentAction = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				Utils.reindent(editor);
			}
		};

		setGlassPane(new TutorialGlassPane(this));
		tutorialAction = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				getGlassPane().setVisible(!getGlassPane().isVisible());
			}
		};
		
		showAcl2Output = new ActionListener() {
			@Override public void actionPerformed(ActionEvent arg0) {
				new Acl2OutputWindow(acl2).setVisible(true);
			}
		};
		
		final FindBar findBar = new FindBar(this);
		findAction = findBar.findAction;
		toolbar = new Toolbar(this);
		if (Main.OSX && Main.JAVA_7) {
			toolbar.setBackground(activeToolbar);
		}
		menuBar = new MenuBar(this);
		splitMain.add(toolbar, BorderLayout.PAGE_START);
		setJMenuBar(menuBar);
		
		// Preferences
		PrefsWindow.addFontChangeListener(new FontChangeListener() {
			@Override
			public void fontChanged(Font font) {
				editor.setFont(font);
				repl.setFont(font);
				proofBar.setLineHeight(editor.getLineHeight());
				editorScroller.getVerticalScrollBar().setUnitIncrement(editor.getLineHeight());
				editorScroller.getHorizontalScrollBar().setUnitIncrement(editor.getLineHeight());
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
		PrefsWindow.addShowLineNumbersListener(new PrefsWindow.ShowLineNumbersListener() {
			@Override
			public void lineNumbersVisible(boolean visible) {
				gutter.setVisible(visible);
				westPanel.revalidate();
			}
		});
		
		///// Event Listeners /////

		proofBar.addReadOnlyIndexChangeListener(new ProofBar.ReadOnlyIndexChangeListener() {
			@Override
			public void readOnlyIndexChanged(int newIndex) {
				if (editor.getLastVisibleOffset() - 1 <= newIndex) {
					editor.append("\n");
				}
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						editor.repaint();
					}
				});
			}
		});
		
		editor.addKeyListener(new KeyAdapter() {
			{
				keyReleased(null);
			}
			@Override
			public void keyReleased(KeyEvent e) {
				adjustGutterSize();
			}
		});
		
		editor.setUndoManagerCreatedListener(new CodePane.UndoManagerCreatedListener() {
			@Override
			public void undoManagerCreated(UndoManager undoManager) {
				proofBar.undoManager = undoManager;
			}
		});
		
		doc.addDocumentListener(new DocumentListener() {
			private void update(DocumentEvent e) {
				List<Expression> exps = SExpUtils.topLevelExps(doc);
				proofBar.adjustHeights((LinkedList<Expression>) exps);
				fixUndoRedoStatus();
				adjustMaximizedBounds();
				setSaved(false);
			}
			@Override public void changedUpdate(DocumentEvent e) {
				update(e);
			}
			@Override public void insertUpdate(DocumentEvent e) {
				update(e);
			}
			@Override public void removeUpdate(DocumentEvent e) {
				update(e);
			}
		});

		addWindowListener(new WindowAdapter() {
			@Override
			public void windowActivated(WindowEvent arg0) {
				proofBar.repaint();
				// TODO: Remove these setBackground calls if this bug is ever fixed:
				// http://java.net/jira/browse/MACOSX_PORT-775
				if (Main.JAVA_7) toolbar.setBackground(activeToolbar);
			}
			@Override
			public void windowDeactivated(WindowEvent e) {
				proofBar.repaint();
				if (Main.JAVA_7) toolbar.setBackground(inactiveToolbar);
			}
			@Override
			public void windowClosing(WindowEvent arg0) {
				if (promptIfUnsavedAndClose()) {
					updateWindowMenu();
				}
			}
			@Override
			public void windowOpened(WindowEvent arg0) {
				proofBar.repaint();
			}
		});
		
		userLocation = false;
		addComponentListener(new ComponentAdapter() {
			@Override public void componentResized(ComponentEvent e) {
				userLocation = true;
			}
			
			@Override public void componentMoved(ComponentEvent e) {
				userLocation = true;
			}
		});

		Toolkit.getDefaultToolkit().setDynamicLayout(false);
		split.setBottomComponent(repl);
		repl.setHeightChangeListener(new Repl.HeightChangeListener() {
			@Override
			public void heightChanged(int delta) {
				setSize(new Dimension(getWidth(), getHeight() + delta));
				split.setDividerLocation(split.getDividerLocation() - delta);
			}
		});

		if (openFile != null) {
			openAndDisplay(openFile);
		} else {
			setTitle("untitled"
				+ (untitledCount == 1 ? "" : " " + (untitledCount))
				+ (!Main.OSX ? " - Proof Pad" : ""));
			untitledCount++;
		}
		updateWindowMenu();

		setPreferredSize(new Dimension(600, 600));
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		setMinimumSize(new Dimension(550, 300));

		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
		if (Main.WIN || Main.OSX) {
			setLocation((screen.width - 600) / 2 + 50 * windows.size(),
					(screen.height - 800) / 2 + 50 * windows.size());
		}

		fixUndoRedoStatus();
		split.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY,
				new PropertyChangeListener() {
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
		
		String[] iconPaths = {
				"icon.iconset/icon_16x16.png",
				"icon.iconset/icon_32x32.png",
				"icon.iconset/icon_128x128.png",
				"icon.iconset/icon_32x32@2x.png"
		};
		
		List<Image> icons = new ArrayList<Image>();
		for (String path : iconPaths) {
			icons.add(new ImageIcon(path).getImage());
		}
		setIconImages(icons );
		
		adjustMaximizedBounds();
		pack();
		editor.requestFocus();
		split.setDividerLocation(.65);
		SwingUtilities.invokeLater(new Runnable() {
			@Override public void run() {
				userLocation = false;
			}
		});
	}

	protected void setInfoBar(JComponent ib) {
		Component child = ((BorderLayout) splitTop.getLayout())
				.getLayoutComponent(BorderLayout.PAGE_START);
		if (child != null) splitTop.remove(child);
		if (ib instanceof InfoBar) {
			infoBar = (InfoBar) ib;
		}
		splitTop.add(ib, BorderLayout.PAGE_START);
		splitTop.revalidate();
	}
	
	protected void closeInfoBar() {
		Component child = ((BorderLayout) splitTop.getLayout())
				.getLayoutComponent(BorderLayout.PAGE_START);
		splitTop.remove(child);
		editor.clearMarkAllHighlights();
		splitTop.revalidate();
	}

	static void updateWindowMenu() {
		for (PPWindow window : windows) {
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
	
	boolean promptIfUnsavedAndQuit(Iterator<PPWindow> ii) {
		int response = -1;
		boolean isEmpty = editor.getText().length() == 0;
		if (!isSaved && !isEmpty) {
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
		if (response == 1 || isSaved || isEmpty) {
			dispose();
			if (acl2 != null) acl2.terminate();
			if (outputWindow != null) outputWindow.dispose();
			if (monitor != null) monitor.requestStop();
			if (ii != null) {
				ii.remove();
			} else {
				windows.remove(PPWindow.this);
			}
			if (windows.size() == 0 && !Main.OSX) {
				Main.saveUserDataAndExit();
			}
			return true;
		}
		return false;
	}
	
	void saveFileAs() {
		File oldOpenFile = openFile;
		openFile = null;
		if (!saveFile()) {
			openFile = oldOpenFile;
		}
	}

	boolean saveFile() {
		if (openFile == null) {
			File file = null;
			if (Main.OSX) {
				FileDialog fd = new FileDialog(this, "Save As...");
				fd.setMode(FileDialog.SAVE);
				fd.setVisible(true);
				String filename = fd.getFile();
				if (filename != null) {
					if (filename.indexOf('.') == -1) {
						filename += ".lisp";
					}
					file = new File(fd.getDirectory(), filename);
				}
			} else {
				int response = fc.showSaveDialog(this);
				file = response == JFileChooser.APPROVE_OPTION ? fc.getSelectedFile() : null;
				if (file != null && file.getName().indexOf('.') == -1) {
					file = new File(file.getParentFile(), file.getName() + ".lisp");
				}
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
				
				resetMonitor();
				markOpenFileAsRecent();
			} else {
				return false;
			}
		}
		try {
			if (monitor != null) monitor.ignoreOne();
			BufferedWriter bw = new BufferedWriter(new FileWriter(openFile));
			bw.write(editor.getText());
			bw.close();
			setSaved(true);
			getRootPane().putClientProperty("Window.documentFile", openFile);
			setTitle(openFile.getName() + (!Main.OSX ? " - Proof Pad" : ""));
		} catch (IOException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(this, "Saving to " + openFile + " failed. Make sure you" +
					" have permission to write to this location, or try a different location." +
					e.getMessage(), "Error saving", JOptionPane.ERROR_MESSAGE);
			setSaved(false);
		}
		return true;
	}

	public void setSaved(boolean isSaved) {
		this.isSaved = isSaved;
		getRootPane().putClientProperty("Window.documentModified",
				!isSaved);
		saveButton.setEnabled(!isSaved);
		menuBar.saveItem.setEnabled(!isSaved);
	}
	
	void adjustGutterSize() {
		int width = ((int) Math.log10(editor.getLineCount()) + 2)
				* getFontMetrics(gutter.getFont()).charWidth('1');
		gutter.setPreferredSize(new Dimension(width, 0));
		gutter.repaint();
		westPanel.revalidate();
	}

	protected void openAndDisplay(File file) {
		getRootPane().putClientProperty("Window.documentFile", file);
		setTitle(file.getName() + (!Main.OSX ? " - Proof Pad" : ""));
		openFile = file;
		Scanner scan = null;
		try {
			scan = new Scanner(openFile);
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
			return;
		}
		String content = scan.useDelimiter("\\Z").next();
		content = content.replaceAll("\\r", "");
		scan.close();
		editor.setText(content);
		editor.setCaretPosition(0);
		java.util.List<Expression> exps = SExpUtils
				.topLevelExps((PPDocument) editor.getDocument());
		proofBar.adjustHeights((LinkedList<Expression>) exps);
		setSaved(true);
		adjustMaximizedBounds();
		updateWindowMenu();
		
		resetMonitor();
		markOpenFileAsRecent();
		adjustGutterSize();
	}
	
	private void resetMonitor() {
		if (monitor != null) {
			monitor.requestStop();
		}
		monitor = new FileMonitor(openFile, new Runnable() {
			@Override public void run() {
				setInfoBar(new InfoBar(openFile.getName() + " has been modified outside Proof Pad", new InfoBar.InfoButton[] {
					new InfoButton("Load", new ActionListener() {
						@Override public void actionPerformed(ActionEvent arg0) {
							openAndDisplay(openFile);
						}
					})
				}));
			}
		});
		monitor.start();
	}

	private void markOpenFileAsRecent() {
		// Update recent files
		int downTo = 10;
		Preferences prefs = Preferences.userNodeForPackage(Main.class);
		for (int i = 1; i <= MenuBar.RECENT_MENU_ITEMS; i++) {
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
		prefs.put("recent1", openFile.getAbsolutePath());
		for (PPWindow w : windows) {
			w.menuBar.updateRecentMenu();
		}
		if (Main.OSX && !Main.FAKE_WINDOWS) {
			Main.menuBar.updateRecentMenu();
		}
	}

	public void adjustMaximizedBounds() {
		if (!Main.OSX) return;
		Dimension visibleSize = editorScroller.getViewport().getExtentSize();
		Dimension textSize = editor.getPreferredScrollableViewportSize();
		int widthGuideWidth = editor.getFontMetrics(editor.getFont()).charWidth('a') *
				Prefs.widthGuide.get();
		int textWidth = Math.max(textSize.width, widthGuideWidth);
		int maxWidth = Math.max(getWidth() - visibleSize.width + textWidth
				+ proofBar.getWidth() + moreBar.getWidth(), 550);
		int maxHeight = getHeight() - visibleSize.height + Math.max(textSize.height, 200);
		setMaximizedBounds(new Rectangle(getLocation(), new Dimension(
				maxWidth + 5, maxHeight + 5999)));
	}

	public void includeBookAtCursor(String dirName, String path) {
		editor.insert("(include-book \"" + path + "\""
				       + (dirName == null ? "" : " :dir " + dirName) + ")" +
				       "\n",
				       editor.getCaretPosition() - editor.getCaretOffsetFromLineStart());
	}

	public Point moveForOutputWindow() {
		Point ret = new Point();
		Point myPos;
		try {
			myPos = getLocationOnScreen();
		} catch (IllegalComponentStateException e) {
			return null;
		}
		ret.y = myPos.y;
		int myWidth = getWidth() + WINDOW_GAP;
		int outputWidth = outputWindow.getWidth() + WINDOW_GAP;
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int newX = screenSize.width - outputWidth - myWidth;
		int newY = myPos.y;
		if (outputWindow.getHeight() > screenSize.height - myPos.y) {
			newY = 0;
			ret.y = 0;
		}
		if (newX <= 0) newX = 0;
		if (myPos.x > newX && !userLocation) {
			ret.x = screenSize.width - outputWidth;
			setLocation(newX, newY);
		} else {
			ret.x = myPos.x + myWidth;
		}
		return ret;
	}

	public static void createOrReuse(File file) {
		if (PPWindow.windows.size() == 1 &&
				PPWindow.windows.get(0).openFile == null &&
				PPWindow.windows.get(0).isSaved == true) {
			PPWindow.windows.get(0).openAndDisplay(file);
		} else {
			PPWindow win = new PPWindow(file);
			win.setVisible(true);
		}		
	}
}