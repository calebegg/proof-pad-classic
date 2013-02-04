package org.proofpad;

import java.awt.Color;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JOptionPane;
import javax.swing.ToolTipManager;
import javax.swing.event.CaretEvent;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.undo.UndoManager;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.RSyntaxUtilities;
import org.fife.ui.rsyntaxtextarea.Style;
import org.fife.ui.rsyntaxtextarea.SyntaxScheme;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rtextarea.RUndoManager;

public class CodePane extends RSyntaxTextArea implements Iterable<Token> {

	final class LookUpListener implements ActionListener {
		private String id;

		public LookUpListener() { }
		
		public LookUpListener(String id) {
			this.id = id;
			
		}

		@Override public void actionPerformed(ActionEvent e) {
			// First, check for a visible tool tip.
			String name;
			if (id == null) {
				ToolTipManager ttManager = ToolTipManager.sharedInstance();
				boolean tipShowing = false;
				try {
					Field f = ttManager.getClass().getDeclaredField("tipShowing");
					f.setAccessible(true);
					tipShowing = f.getBoolean(ttManager);
				} catch (Exception ex) { }
				if (tipShowing) {
					name = getWordAtMouse();
				} else {
					name = getWordAt(getCaretPosition());
				}
			} else {
				name = id;
			}
		    if (name != null) {
				if (Main.cache.getDocs().containsKey(name.toUpperCase())) {
					try {
						Desktop.getDesktop().browse(new URI("http://www.cs.utexas.edu/~moore/acl2/v4-3/"
								+ name.toUpperCase() + ".html"));
					} catch (IOException e1) {
					} catch (URISyntaxException e1) { }
				} else {
					String[] opts = new String[] { "Go to Index", "Close" };
					int choice = JOptionPane.showOptionDialog(null,
							"No documentation found for \"" + name + "\"",
							"Topic not found",
							JOptionPane.OK_CANCEL_OPTION,
							JOptionPane.INFORMATION_MESSAGE,
							null,
							opts,
							opts[1]);
					if (choice == 0) {
						try {
							Desktop.getDesktop().browse(new URI("http://www.cs.utexas.edu/~moore/acl2/v4-3/acl2-doc-index.html"));
						} catch (IOException e1) {
						} catch (URISyntaxException e1) { }
					}
				}
			}
		}
	}

	public interface UndoManagerCreatedListener {
		public void undoManagerCreated(UndoManager undoManager);
	}

	private static final long serialVersionUID = 2585177201079384705L;
	private static final int leftMargin = 2;
//	private static final String[] welcomeMessage =
//		{"See Help > Tutorial for a basic overview."};
	private ProofBar pb;
	private final List<Rectangle> fullMatch = new ArrayList<Rectangle>();
	int widthGuide = -1;
	private UndoManagerCreatedListener undoManagerCreatedListener;
	private RUndoManager undoManager;
	private ActionListener lookUpAction;
	private MenuBar menuBar;
	
	public CodePane(final ProofBar pb) {
		this.pb = pb;
		setAntiAliasingEnabled(true);
		setAutoIndentEnabled(false);
		setHighlightCurrentLine(pb != null);
		setCurrentLineHighlightColor(new Color(0, .6f, .8f, .1f));
		setBracketMatchingEnabled(false);
		setUseFocusableTips(false);
		SyntaxScheme scheme = getSyntaxScheme();
		Style builtinStyle = scheme.getStyle(Token.RESERVED_WORD);
		Style eventStyle = scheme.getStyle(Token.RESERVED_WORD_2);
		builtinStyle.font = eventStyle.font = builtinStyle.font.deriveFont(Font.PLAIN);
		scheme.getStyle(Token.COMMENT_EOL).foreground = new Color(.4f, .6f, .4f);
		scheme.getStyle(Token.COMMENT_MULTILINE).foreground = new Color(.4f, .6f, .4f);
		scheme.getStyle(Token.RESERVED_WORD_2).foreground = new Color(0f, .3f, .7f);
		scheme.getStyle(Token.SEPARATOR).foreground = Color.black;
		setBorder(BorderFactory.createEmptyBorder(0, leftMargin, 0, 0));
		setTabSize(4);
		ContextMenu menu = new ContextMenu(this);
		setPopupMenu(menu);
		setBackground(PPWindow.transparent);
		lookUpAction = new LookUpListener();
		addKeyListener(new KeyAdapter() {
			@Override public void keyPressed(KeyEvent e) {
				if (pb == null) return;
				if (Main.OSX && e.isAltDown() && e.isMetaDown()
						&& (e.getKeyCode() == KeyEvent.VK_DOWN || e.getKeyCode() == KeyEvent.VK_UP)) {
					// Keyboard shortcuts for admit/unadmit
					return;
				}
				int readOnlyLine = 0;
				try {
					readOnlyLine = getLineOfOffset(pb.getReadOnlyIndex() + 2) - 1;
				} catch (BadLocationException e1) { }
				if (getCaretLineNumber() == readOnlyLine + 1
						&& e.getKeyCode() == KeyEvent.VK_UP) {
					// Up arrow at the top of the readable area moves the cursor
					// to the beginning of the line
					int newCaretPosition;
					if (pb.getReadOnlyIndex() == -1) {
						newCaretPosition = 0;
					} else {
						newCaretPosition = pb.getReadOnlyIndex() + 2;
					}
					if (e.isShiftDown()) {
						setCaretPosition(getSelectionEnd());
						moveCaretPosition(newCaretPosition);
					} else {
						setCaretPosition(newCaretPosition);
					}
					e.consume();
					return;
				} else if (getCaretLineNumber() == getLineCount() - 1
						&& e.getKeyCode() == KeyEvent.VK_DOWN) {
					// Down arrow at bottom moves to end of document
					if (e.isShiftDown()) {
						setSelectionEnd(getLastVisibleOffset());
					} else {
						setCaretPosition(getLastVisibleOffset());
					}
					e.consume();
				}
				if (e.isAltDown() || e.isMetaDown() || e.isControlDown()
						|| pb.getReadOnlyIndex() == -1) {
					return;
				}
				if (e.getKeyCode() == KeyEvent.VK_LEFT) {
					if (getCaretPosition() - 3 < pb.getReadOnlyIndex()) {
						e.consume();
					}
				}
			}

			@Override public void keyTyped(KeyEvent e) {
				// Parentheses wrapping
				int selectionStart = getSelectionStart();
				int selectionEnd = getSelectionEnd();
				if (e.getKeyChar() == '(' && selectionStart != selectionEnd) {
					e.consume();
					try {
						SimpleAttributeSet attrSet = new SimpleAttributeSet();
						attrSet.addAttribute("autoins", Boolean.TRUE);
						getDocument().insertString(selectionStart, "(", attrSet);
						getDocument().insertString(selectionEnd + 1, ")", attrSet);
					} catch (BadLocationException e1) {
						e1.printStackTrace();
					}
					setSelectionStart(selectionStart + 1);
					setSelectionEnd(selectionEnd + 1);
				}
			}
		});

	}
	
	@Override public void paintComponent(Graphics g) {
		int readOnlyHeight = (pb == null ? 0 : pb.readOnlyHeight);
		// Paint read-write background
		g.setColor(Color.WHITE);
		g.fillRect(0, readOnlyHeight, getWidth(), getHeight() - readOnlyHeight);
		// Paint bracket match
		g.setColor(new Color(1f, 1f, .8f));
		for (Rectangle match : fullMatch) {
			g.fillRect(match.x, match.y, match.width, match.height);
		}
		// Paint read only background.
		g.setColor(new Color(240, 235, 231));
		g.fillRect(0, 0, getWidth(), readOnlyHeight);
		g.setColor(new Color(.6f, .6f, .6f));
		g.drawLine(0, readOnlyHeight - 1, getWidth(), readOnlyHeight - 1);
		// Paint width guide
		if (widthGuide != -1) {
			g.setColor(new Color(1f, .8f, .8f));
			int linex = widthGuide * getFontMetrics(getFont()).charWidth('a') + leftMargin + 1;
			g.drawLine(linex, 0, linex, getHeight());
		}
		super.paintComponent(g);
	}

	public void admitBelowProofLine(String form) {
		try {
			boolean breakBefore = pb.getReadOnlyIndex() >= 0;
			getDocument().insertString(pb.getReadOnlyIndex() + 1, (breakBefore ? "\n" : "") + form.trim(), null);
			pb.admitNextForm();
		} catch (BadLocationException e) { }
	}
	
	public void highlightBracketMatch() {
		fullMatch.clear();
		int matchPos = RSyntaxUtilities.getMatchingBracketPosition(this);
		if (matchPos == -1) {
			repaint();
			return;
		}
		int caret = getCaretPosition();
		try {
			int lineStartOffset = modelToView(getLineStartOffset(0)).x;
			Rectangle bracketRect = modelToView(matchPos);
			Rectangle caretRect = modelToView(caret - 1);
			Rectangle start;
			Rectangle end;
			int cursorLine = getCaretLineNumber();
			int matchLine = getLineOfOffset(matchPos);
			if (cursorLine == matchLine) {
				bracketRect.add(caretRect);
				fullMatch.add(bracketRect);
				repaint();
				return;
			} else if (cursorLine > matchLine) {
				start = bracketRect;
				end = caretRect;
			} else {
				start = caretRect;
				end = bracketRect;
			}
			Rectangle endRect = modelToView(getLineEndOffset(Math.min(cursorLine, matchLine)) - 1);
			int lineEndOffset = endRect.x + endRect.width;
			start.add(new Rectangle(lineEndOffset, start.y, 0, start.height));
			fullMatch.add(start);
			end.add(new Rectangle(lineStartOffset, end.y, 0, end.height));
			fullMatch.add(end);
			for (int line = Math.min(cursorLine, matchLine) + 1; line < Math.max(cursorLine, matchLine); line++) {
				endRect = modelToView(getLineEndOffset(line) - 1);
				lineEndOffset = endRect.x + endRect.width;
				fullMatch.add(new Rectangle(lineStartOffset, line * getLineHeight(), lineEndOffset - lineStartOffset, getLineHeight()));
			}
		} catch (Exception e) {
			return;
		}

		repaint();
	}
	
	@Override protected void fireCaretUpdate(CaretEvent e) {
		super.fireCaretUpdate(e);
		highlightBracketMatch();
		if (getMenuBar() != null) {
			String name = getWordAt(getCaretPosition());
			if (name != null && Main.cache.getDocs().containsKey(name.toUpperCase())) {
				getMenuBar().enableLookUp(name);
			} else {
				getMenuBar().disableLookUp();
			}
		}
	}
	
	@Override public Iterator<Token> iterator() {
		final CodePane that = this;
		Iterator<Token> it = new Iterator<Token>() {
			int line = -1;
			CodePane pane = that;
			Token token = null;
			boolean first = true;

			@Override public boolean hasNext() {
				return first || token != null && token.type != Token.NULL;
			}

			@Override public Token next() {
				first = false;
				if (token != null) {
					token = token.getNextToken();
				}
				while (token == null || token.type == Token.NULL) {
					line++;
					if (line >= pane.getLineCount()) {
						break;
					}
					token = pane.getTokenListForLine(line);
				}
//				System.out.println(token);
				return token;
			}

			@Override public void remove() {
				throw new RuntimeException();
			}	
		};
		return it;
	}
	
	@Override protected RUndoManager createUndoManager() {
		undoManager = new RUndoManager(this);
		if (undoManagerCreatedListener != null) {
			undoManagerCreatedListener.undoManagerCreated(undoManager);
		}
		return undoManager;
	}

	public void setUndoManagerCreatedListener(
			UndoManagerCreatedListener umcl) {
		undoManagerCreatedListener = umcl;
		if (undoManager != null) {
			umcl.undoManagerCreated(undoManager);
		}
	}

	public ActionListener getHelpAction() {
		return lookUpAction;
	}
	
	public String getWordAtMouse() {
		Point mouseLoc = MouseInfo.getPointerInfo().getLocation();
		Point compLoc = getLocationOnScreen();
		return getWordAt(new Point(mouseLoc.x - compLoc.x, mouseLoc.y - compLoc.y));
	}
	
	public String getWordAt(Point p) {
		return getWordAt(viewToModel(p));
	}
	
	public String getWordAt(int loc) {
		if (loc == -1) {
			return null;
		}
		int line = 0;
	    try {
	    	line = getLineOfOffset(loc);
	    } catch (BadLocationException e1) { }
	    Token t = getTokenListForLine(line);
	    while (t != null && t.textOffset + t.textCount < loc) {
	    	t = t.getNextToken();
	    }
	    String name = null;
	    if (t != null) {
	    	try {
	    		name = t.getLexeme();
	    	} catch (NullPointerException ex) {
	    		return null;
	    	}
	    }
		return name;
	}

	public MenuBar getMenuBar() {
		return menuBar;
	}

	public void setMenuBar(MenuBar menuBar) {
		this.menuBar = menuBar;
	}
}
