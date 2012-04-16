package org.proofpad;

import java.util.Stack;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;

import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.Token;

public class IdeDocument extends RSyntaxDocument {
	private static final long serialVersionUID = 7048788640273203918L;
	private ProofBar pb;
	class IndentToken {
		public final String name;
		public final int offset;
		public final int type;
		public int params = 0;
		public IndentToken parent;
		public IndentToken(String name, int offset, int type, IndentToken parent) {
			this.name = name;
			this.offset = offset;
			this.type = type;
			this.parent = parent;
		}
		@Override
		public String toString() {
			return "<" + name + ", " + offset + ", " + params + ">";
		}
	}
	@Override
	public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
		// Auto indentation
		if (str.endsWith("\n")) {
			boolean waitingForName = false;
			Stack<IndentToken> s = new Stack<IndentToken>();
			int lineBeginOffset = 0;
			Token token = null;
			int lineIndentLevel = -1;
			IndentToken top = null;
			for (int i = 0; i < lineCount(); i++) {
				token = getTokenListForLine(i);
				lineBeginOffset = token.offset;
				while (token != null && token.offset != -1 && offs > token.offset) {
					top = s.empty() ? null : s.peek();
					if (token.offset == lineBeginOffset && token.isWhitespace()) {
						lineIndentLevel = token.textCount;
					}
					if (token.isSingleChar('(')) {
						if (waitingForName) {
							s.push(new IndentToken(null, token.offset - lineBeginOffset, -1, top));
						}
						waitingForName = true;
					} else if (token.isSingleChar(')')) {
						if (!s.empty()) s.pop();
						if (s.size() > 0) s.peek().params++;
					} else if (!token.isWhitespace() && !token.isComment()) {
						if (s.size() > 0) s.peek().params++;						
					}
					if (waitingForName && (token.type == Token.IDENTIFIER ||
							               token.type == Token.RESERVED_WORD ||
							               token.type == Token.RESERVED_WORD_2)) {
						s.push(new IndentToken(token.getLexeme(), token.offset - lineBeginOffset, token.type, top));
						waitingForName = false;
						lineIndentLevel = -1;
					}
					token = token.getNextToken();
				}
			}
			if (waitingForName && token != null) {
				s.push(new IndentToken(null, token.offset - lineBeginOffset, -1, top));
			}
			if (s.size() > 0) {
				IndentToken it = s.peek();
				int offset = it.offset;
				int indentLevel;
				boolean important = false;
				if (it.type == -1) {
					// Last token is open parenthesis
					if (it.params == 0 || waitingForName) {
						indentLevel = offset + 1;
					} else {
						indentLevel = offset - 1;
					}
				} else if (it.name.equalsIgnoreCase("let") || it.name.equalsIgnoreCase("let*")) {
					if (it.params >= 2) {
						indentLevel = offset + 1;
					} else {
						indentLevel = offset + it.name.length();
					}
				} else if (it.name.equalsIgnoreCase("mv-let")) {
					if (it.params >= 3) {
						indentLevel = offset + 1;
					} else {
						indentLevel = offset + it.name.length();
					}
				} else if (it.parent != null && it.parent.name != null && it.parent.name.equalsIgnoreCase("defproperty") &&
						it.parent.params == 2) {
					indentLevel = offset - 1;
				} else if (it.name.equalsIgnoreCase("defproperty")) {
					if (it.params == 3) {
						important = true;
					}
					indentLevel = offset + 1;
				} else if (it.type == Token.RESERVED_WORD_2) {
					// Events
					indentLevel = offset + 1;
				} else if (it.params == 0) {
					indentLevel = offset - 1;
				} else {
					// Regular functions
					indentLevel = offset + it.name.length();
				}
				if (lineIndentLevel != -1 && !important) {
					indentLevel = lineIndentLevel - 1;
				}
				for (int j = 0; j < indentLevel + 1; j++) {
					str += " ";
				}
			}
		}
		if (pb == null || offs > pb.getReadOnlyIndex()) {
			if (pb != null && pb.getReadOnlyIndex() >= 0 && offs == pb.getReadOnlyIndex() + 1 && !str.startsWith("\n")) {
				str = '\n' + str;
			}
			super.insertString(offs, str, a);
		} else {
			if (pb != null) pb.flashAt(offs);
		}
	}
	@Override
	public void remove(int offs, int len) throws BadLocationException {
		if (pb == null || offs > pb.getReadOnlyIndex()) {
			super.remove(offs, len);
		} else {
			if (pb != null) pb.flashAt(offs);
		}
	}
	public IdeDocument(ProofBar pb) {
		super(SyntaxConstants.SYNTAX_STYLE_LISP);
		this.pb = pb;
		setSyntaxStyle(new Acl2TokenMaker());
	}
	public int lineCount() {
		return getDefaultRootElement().getElementCount();
	}
}
