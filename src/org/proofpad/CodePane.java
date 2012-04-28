package org.proofpad;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.List;
import java.awt.*;
import java.awt.event.*;

import javax.swing.BorderFactory;
import javax.swing.ToolTipManager;
import javax.swing.event.CaretEvent;
import javax.swing.text.BadLocationException;
import javax.swing.undo.UndoManager;

import org.fife.ui.rsyntaxtextarea.*;
import org.fife.ui.rtextarea.RUndoManager;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public class CodePane extends RSyntaxTextArea implements Iterable<Token> {

	public interface UndoManagerCreatedListener {
		public void undoManagerCreated(UndoManager undoManager);
	}

	private static final long serialVersionUID = 2585177201079384705L;
	private static final int leftMargin = 2;
	private static final String[] welcomeMessage =
		{"See Help > Tutorial for a basic overview."};
	private ProofBar pb;
	private List<Rectangle> fullMatch = new ArrayList<Rectangle>();
	int widthGuide = -1;
	private UndoManagerCreatedListener undoManagerCreatedListener;
	private RUndoManager undoManager;
	private ActionListener lookUpAction;
	
	public CodePane(final ProofBar pb) {
		this.pb = pb;
		setAntiAliasingEnabled(true);
		setAutoIndentEnabled(false);
		setHighlightCurrentLine(false);
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
		setBackground(IdeWindow.transparent);
		lookUpAction = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// First, check for a visible tooltip.
		        ToolTipManager ttManager = ToolTipManager.sharedInstance();
		        boolean tipShowing = false;
		        int loc = -1;
		        try {
		        	Field f = ttManager.getClass().getDeclaredField("tipShowing");
		        	f.setAccessible(true);
		        	tipShowing = f.getBoolean(ttManager);
		        } catch (Exception ex) {
		        }
		        if (tipShowing) {
		        	Point mouseLoc = MouseInfo.getPointerInfo().getLocation();
		        	Point compLoc = getLocationOnScreen();
					loc = viewToModel(new Point(mouseLoc.x - compLoc.x, mouseLoc.y - compLoc.y));
		        } else {
		        	loc = getCaretPosition();
		        }
		        if (loc == -1) {
		        	return;
		        }
		        int line = 0;
		        try {
		        	line = getLineOfOffset(loc);
		        } catch (BadLocationException e1) { }
		        Token t = getTokenListForLine(line);
		        while (t != null && t.textOffset + t.textCount < loc) {
		        	t = t.getNextToken();
		        }
		        if (t != null) {
		        	String name;
		        	try {
		        		name = t.getLexeme();
		        	} catch (NullPointerException ex) {
		        		return;
		        	}
		        	if (name != null && Main.cache.getDocs().containsKey(name.toUpperCase())) {
		        		try {
		        			Desktop.getDesktop().browse(new URI("http://www.cs.utexas.edu/~moore/acl2/v4-3/"
		        					+ name.toUpperCase() + ".html"));
		        		} catch (IOException e1) {
		        		} catch (URISyntaxException e1) { }
		        	}
		        }
			}
		};
		addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (pb == null) return;
				int readOnlyLine = 0;
				try {
					readOnlyLine = getLineOfOffset(pb.getReadOnlyIndex() + 2) - 1;
				} catch (BadLocationException e1) {
				}
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
				if (getCaretColor() == IdeWindow.transparent) {
					e.consume();
				}
				if (e.getKeyCode() == KeyEvent.VK_LEFT) {
					if (getCaretPosition() - 3 < pb.getReadOnlyIndex()) {
						e.consume();
					}
				}
			}
			
			@Override
			public void keyReleased(KeyEvent e) {
				if (pb == null) return;
				if (pb.getReadOnlyIndex() >= 0
						&& getCaretPosition() < pb.getReadOnlyIndex() + 2) {
					setCaretColor(IdeWindow.transparent);
				} else {
					setCaretColor(Color.BLACK);
				}
			}
		});

	}
	
	@Override
	public void paintComponent(Graphics g) {
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
		// Paint a semi-transparent rectangle over read-only part.
		g.setColor(new Color(1f, 1f, 1f, .2f));
		g.fillRect(0, 0, getWidth(), readOnlyHeight);
		g.setColor(new Color(.4f, .4f, .4f));
		if (getLastVisibleOffset() == 0 && !canUndo() && pb != null) {
			// Paint welcome message.
			g.setColor(Color.BLACK);
			FontMetrics fm = g.getFontMetrics();
			Font originalFont = g.getFont();
			g.setFont(originalFont.deriveFont(originalFont.getSize() + 5.0f));
			FontMetrics bigFm = g.getFontMetrics();
			int lineHeight = (int) fm.getLineMetrics(welcomeMessage[0], g).getHeight() + 1;
			int ySoFar = Math.max(0, (getHeight() - lineHeight * welcomeMessage.length) / 2);
			g.drawString(Main.displayName,
					(getWidth() - bigFm.stringWidth(Main.displayName) - pb.getWidth()) / 2,
					ySoFar);
			ySoFar += (int) bigFm.getLineMetrics(Main.displayName, g).getHeight() + 1;
			g.setFont(originalFont);
			for (String line : welcomeMessage) {
				ySoFar += lineHeight;
				g.drawString(line, (getWidth() - fm.stringWidth(line) - pb.getWidth()) / 2,
						ySoFar);
			}
		}
	}

	public void admitBelowProofLine(String form) {
		try {
			boolean breakBefore = pb.getReadOnlyIndex() >= 0;
			getDocument().insertString(pb.getReadOnlyIndex() + 1, (breakBefore ? System.getProperty("line.separator") : "") + form.trim(), null);
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
	
	@Override
	protected void fireCaretUpdate(CaretEvent e) {
		super.fireCaretUpdate(e);
		highlightBracketMatch();
	}
	
	@Override
	public Iterator<Token> iterator() {
		final CodePane that = this;
		Iterator<Token> it = new Iterator<Token>() {
			int line = -1;
			CodePane pane = that;
			Token token = null;
			boolean first = true;

			@Override
			public boolean hasNext() {
				return first || token != null && token.type != Token.NULL;
			}

			@Override
			public Token next() {
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

			@Override
			public void remove() {
				throw new NotImplementedException();
			}	
		};
		return it;
	}
	
	@Override
	protected RUndoManager createUndoManager() {
		undoManager = new RUndoManager(this);
		if (undoManagerCreatedListener != null) {
			undoManagerCreatedListener.undoManagerCreated(undoManager);
		}
		return undoManager;
	}

	public void SetUndoManagerCreatedListener(
			UndoManagerCreatedListener umcl) {
		this.undoManagerCreatedListener = umcl;
		if (undoManager != null) {
			umcl.undoManagerCreated(undoManager);
		}
	}

	public ActionListener getHelpAction() {
		// TODO Auto-generated method stub
		return lookUpAction;
	}
}
