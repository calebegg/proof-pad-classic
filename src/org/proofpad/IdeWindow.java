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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.BadLocationException;
import javax.swing.undo.UndoManager;

import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rtextarea.SearchContext;
import org.fife.ui.rtextarea.SearchEngine;
import org.proofpad.PrefsWindow.FontChangeListener;
import org.proofpad.Repl.MsgType;

//import com.apple.eawt.FullScreenAdapter;
//import com.apple.eawt.AppEvent.FullScreenEvent;
//import com.apple.eawt.FullScreenUtilities;


public class IdeWindow extends JFrame {
	public static final boolean OSX = System.getProperty("os.name").indexOf("Mac") != -1;
	public static final boolean WIN =
			System.getProperty("os.name").toLowerCase().indexOf("windows") != -1;
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
				return f.getName().endsWith(".lisp") || f.getName().endsWith(".lsp")
						|| f.getName().endsWith(".acl2");
			}
		});
	}
	public static final ActionListener openAction = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			File file;
			if (OSX) {
				FileDialog fd = new FileDialog((Frame)null, "Open file");
				fd.setFilenameFilter(new FilenameFilter() {
					@Override
					public boolean accept(File dir, String name) {
						return name.endsWith(".lisp") || name.endsWith(".lsp")
								|| name.endsWith(".acl2");
					}
				});
				fd.setVisible(true);
				String filename = fd.getFile();
				file = filename == null ? null : new File(fd.getDirectory(), filename);
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
	
	File openFile;
	boolean isSaved = true;
	IdeWindow that = this;
	private File workingDir;
	private Acl2Parser parser;
	Repl repl;
	Toolbar toolbar;

	JSplitPane previewSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true);
	final int splitDividerDefaultSize = previewSplit.getDividerSize();
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
	ActionListener undoOneAction;
	protected int dY;
	protected int dX;
	ActionListener tutorialAction;
	TraceResult activeTrace;

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
		previewSplit.setBorder(BorderFactory.createEmptyBorder());
		previewSplit.setDividerSize(0);
		previewSplit.setResizeWeight(0);
		final JPanel splitMain = new JPanel();
		previewSplit.setLeftComponent(splitMain);
		splitMain.setLayout(new BorderLayout());
		add(previewSplit);
		final JPanel splitTop = new JPanel();
		splitTop.setLayout(new BorderLayout());
		final JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true);
		split.setTopComponent(splitTop);
		split.setOneTouchExpandable(true);
		split.setResizeWeight(1);
		splitMain.add(split, BorderLayout.CENTER);
		editorScroller = new JScrollPane();
		editorScroller.setBorder(BorderFactory.createEmptyBorder());
		editorScroller.setViewportBorder(BorderFactory.createEmptyBorder());
		this.getRootPane().setBorder(BorderFactory.createEmptyBorder());
		splitTop.add(editorScroller, BorderLayout.CENTER);

		final Preferences prefs = Preferences.userNodeForPackage(Main.class);
		
		final String acl2Path;
		if (prefs.getBoolean("customacl2", false)) {
			acl2Path = prefs.get("acl2Path", "");
		} else {
			if (WIN) {
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
		
		parser = new Acl2Parser(workingDir, new File(acl2Path).getParentFile());
		acl2 = new Acl2(acl2Path, workingDir, parser);
		proofBar = new ProofBar(acl2);
		editor = new CodePane(proofBar);
		editorScroller.setViewportView(editor);
		editorScroller.setRowHeaderView(proofBar);
		helpAction = editor.getHelpAction();
		repl = new Repl(this, acl2, editor);
		proofBar.setLineHeight(editor.getLineHeight());
		final IdeDocument doc = new IdeDocument(proofBar);
		editor.setDocument(doc);
		parser.addParseListener(new Acl2Parser.ParseListener() {
			@Override
			public void wasParsed() {
				proofBar.admitUnprovenExps();
				if (activeTrace != null) {
					repl.traceExp(activeTrace.input);
				}
			}
		});
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
			@Override
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
				if (!saveFile()) {
					JOptionPane.showMessageDialog(that,
							"Save the current file in order to build", "Error",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
				final BuildWindow builder = new BuildWindow(openFile, acl2Path);
				builder.setVisible(true);
				builder.build();
			}
		};
		
		includeBookAction = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				BookViewer viewer = new BookViewer(that);
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
				proofBar.undoPrevForm();
			}
		};
		
		reindentAction = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				int beginLine, endLine;
				try {
					beginLine = editor.getLineOfOffset(editor.getSelectionStart());
					endLine = editor.getLineOfOffset(editor.getSelectionEnd());
				} catch (BadLocationException e) {
					return;
				}
				for (int line = beginLine; line <= endLine; line++) {
					int offset;
					try {
						offset = editor.getLineStartOffset(line);
					} catch (BadLocationException e) {
						return;
					}
					int eolLen = 1;
					try {
						String lineStr = editor.getText(offset, editor.getLineEndOffset(line) - offset);
						Matcher whitespace = Pattern.compile("^[ \t]*").matcher(lineStr);
						whitespace.find();
						int whitespaceLen = whitespace.group().length();
						
						doc.remove(offset - eolLen, eolLen + whitespaceLen);
						doc.insertString(offset - eolLen, "\n", null);
					} catch (BadLocationException e) { }
				}
			}
		};

		setGlassPane(new JComponent() {
			{
				setVisible(false);
				addMouseListener(new MouseAdapter() {
					@Override
					public void mouseClicked(MouseEvent e) {
						setVisible(false);
					}
				});
			}
			private static final long serialVersionUID = 1L;
			@Override
			public boolean contains(int x, int y) {
				return y > toolbar.getHeight();
			}
			@Override
			public void paintComponent(Graphics g) {
				// More opaque over areas we want to write on
				g.setColor(new Color(.95f, .95f, .95f, .7f));
				g.fillRect(proofBar.getWidth() + 2, toolbar.getHeight(), getWidth(), editorScroller.getHeight() + 3);
				g.fillRect(0, getHeight() - repl.getHeight(),
						getWidth(), repl.getHeight() - repl.getInputHeight() - 10);
				g.setColor(new Color(.9f, .9f, .9f, .4f));
				g.fillRect(0, toolbar.getHeight(), getWidth(), getHeight());
				g.setColor(new Color(0f, 0f, .7f));
				g.setFont(editor.getFont().deriveFont(16f).deriveFont(Font.BOLD));
				int lineHeight = (int) g.getFontMetrics().getLineMetrics("", g).getHeight();
				g.drawString("1. Write your functions here.",
						proofBar.getWidth() + 20,
						toolbar.getHeight() + 30);
				int step2Y = toolbar.getHeight() + editorScroller.getHeight() / 6 + 40;
				g.drawString("2. Click to admit them.",
						proofBar.getWidth() + 30,
						step2Y);
				g.drawLine(proofBar.getWidth() + 24,
						   step2Y - lineHeight / 4,
						   proofBar.getWidth() - 10,
						   step2Y - lineHeight / 4);
				g.drawString("3. Test them here.",
						proofBar.getWidth() + 20,
						getHeight() - (int) repl.input.getPreferredScrollableViewportSize().getHeight() - 30);
			}
		});
		tutorialAction = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				getGlassPane().setVisible(!getGlassPane().isVisible());
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

		findBar.setLayout(new BoxLayout(findBar, BoxLayout.X_AXIS));
		findBar.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createMatteBorder(0, 0, 1, 0, Color.BLACK),
				BorderFactory.createEmptyBorder(0, 5, 0, 5)));
		findBar.setBackground(new Color(.9f, .9f, .9f));
		if (OSX) {
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
			
			@Override
			public void keyReleased(KeyEvent arg0) {
			}
			
			@Override
			public void keyTyped(KeyEvent e) {
				char c = e.getKeyChar();
				if (c == KeyEvent.CHAR_UNDEFINED || c == '\n' || c == '\b'
						|| e.isAltDown() || e.isMetaDown() || e.isControlDown()) {
					return;
				}
				if (prefs.getBoolean("incsearch", true)) {
					editor.markAll(searchField.getText() + c, false, false,
							false);
				}
			}
		});
		searchField.putClientProperty("JTextField.variant", "search");
		findBar.add(searchField);
		JButton forward = new JButton(new ImageIcon("media/find_down.png"));
		forward.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				searchFor(searchField.getText(), true);
			}
		});
		JButton back = new JButton(new ImageIcon("media/find_up.png"));
		back.addActionListener(new ActionListener() {
			@Override
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
		splitMain.add(toolbar, BorderLayout.NORTH);
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
				if (editor.getLastVisibleOffset() - 1 <= newIndex) {
					editor.append("\n");
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
				+ (!OSX ? " - Proof Pad" : ""));
			untitledCount++;
		}
		updateWindowMenu();

		setPreferredSize(new Dimension(600, 600));
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		setMinimumSize(new Dimension(550, 300));

		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
		if (WIN || OSX) {
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
		
		adjustMaximizedBounds();
		pack();
		editor.requestFocus();
		split.setDividerLocation(.65);

	}

	static void updateWindowMenu() {
		for (IdeWindow window : windows) {
			MenuBar bar = window.menuBar;
			if (bar != null) {
				bar.updateWindowMenu();
			}
		}
	}
	
	public void setPreviewComponent(JComponent c) {
		if (!isVisible()) return;
		if (c instanceof TraceResult) {
			activeTrace = (TraceResult) c;
		} else {
			activeTrace = null;
		}
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		previewSplit.setRightComponent(panel);
		JScrollPane scroller = new JScrollPane();
		scroller.setViewportView(c);
		panel.add(scroller, BorderLayout.CENTER);
		JButton closeButton = new JButton("<<");
		JPanel bottom = new JPanel();
		bottom.setLayout(new BoxLayout(bottom, BoxLayout.X_AXIS));
		bottom.add(Box.createGlue());
		bottom.add(closeButton);
		closeButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				int paneWidth = previewSplit.getRightComponent().getWidth();
				Point loc = getLocationOnScreen();
				setBounds(loc.x, loc.y, getWidth() - paneWidth - splitDividerDefaultSize, getHeight());
				previewSplit.setRightComponent(null);
				previewSplit.setDividerSize(0);
				activeTrace = null;
			}
		});
		panel.add(bottom, BorderLayout.SOUTH);
		boolean wasPreviewOpen = previewSplit.getDividerSize() != 0;
		previewSplit.setDividerSize(splitDividerDefaultSize);
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		c.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		Point loc = getLocationOnScreen();
		int paneWidth = 75 * getFontMetrics(c.getFont()).charWidth('a');
		final int winWidth = getWidth();
		int newX = (int) Math.min(screenSize.getWidth() - winWidth - paneWidth, loc.x);
		if (!wasPreviewOpen) {
			setBounds(newX, loc.y, winWidth + paneWidth + splitDividerDefaultSize, getHeight());
			// TODO: This doesn't work well on OS X, but might work on Windows or Linux.
//			final int steps = 10;
//			final int dx = (newX - loc.x) / steps;
//			final int dw = (paneWidth + splitDividerDefaultSize) / steps;
//			new Timer(30, new ActionListener() {
//				int i = 0;
//				@Override
//				public void actionPerformed(ActionEvent e) {
//					RepaintManager.currentManager(that.getRootPane()).markCompletelyClean(that.getRootPane());
//					i++;
//					Point winLoc = getLocationOnScreen();
//					if (i == steps) {
//						((Timer) e.getSource()).stop();
//					} else {
//						setBounds(winLoc.x + dx, winLoc.y, getWidth() + dw, getHeight());
//					}
//				}
//			}).start();
			previewSplit.setDividerLocation(winWidth + splitDividerDefaultSize);
		} else {
			previewSplit.setDividerLocation(winWidth - paneWidth);
		}
		adjustMaximizedBounds();
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
			if (windows.size() == 0 && !OSX) {
				System.exit(0);
			}
			return true;
		}
		return false;
	}

	boolean saveFile() {
		if (openFile == null) {
			File file = null;
			if (OSX) {
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
				
				markOpenFileAsRecent();
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
			setTitle(openFile.getName() + (!OSX ? " - Proof Pad" : ""));
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
		setTitle(file.getName() + (!OSX ? " - Proof Pad" : ""));
		openFile = file;
		Scanner scan = null;
		try {
			scan = new Scanner(openFile);
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
			return;
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
		
		markOpenFileAsRecent();
	}
	
	private void markOpenFileAsRecent() {
		// Update recent files
		Preferences prefs = Preferences.userNodeForPackage(Main.class);
		int downTo = 10;
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
		for (IdeWindow w : windows) {
			w.menuBar.updateRecentMenu();
		}
		if (OSX && !Main.FAKE_WINDOWS) {
			Main.menuBar.updateRecentMenu();
		}
	}

	public void adjustMaximizedBounds() {
		if (!OSX) return;
		Dimension visibleSize = editorScroller.getViewport().getExtentSize();
		Dimension textSize = editor.getPreferredScrollableViewportSize();
		int maxWidth = Math.max(getWidth() - visibleSize.width + textSize.width
				+ proofBar.getWidth(), 550);
		int maxHeight = getHeight() - visibleSize.height + Math.max(textSize.height, 200);
		if (previewSplit.getDividerSize() > 0) {
			maxWidth += previewSplit.getRightComponent().getWidth();
		}
		setMaximizedBounds(new Rectangle(getLocation(), new Dimension(
				maxWidth + 5, maxHeight + 5)));
	}

	void searchFor(String text, boolean forward) {
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

	public void includeBookAtCursor(String dirName, String path) {
		editor.insert("(include-book \"" + path + "\""
				       + (dirName == null ? "" : " :dir " + dirName) + ")" +
				       "\n",
				       editor.getCaretPosition() - editor.getCaretOffsetFromLineStart());
	}
}
