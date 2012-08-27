package org.proofpad;
import java.util.LinkedList;
import java.util.List;

import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.Token;


public class SExpUtils {
	static enum ExpType {
		NORMAL,
		FINAL,
		UNDOABLE
	}
	static enum Lex {
		NONE,
		STRING,
		QUOTE,
		ID,
		SL_COMMENT,
		ML_COMMENT,
		HASH,
		CHAR
	}
	
	static List<Expression> topLevelExps(RSyntaxDocument doc) {
		List<Expression> r = new LinkedList<Expression>();
		int parenLevel = 0;
		int height = 0;
		boolean first = false;
		StringBuilder contents = new StringBuilder();
		boolean contentsAdmittable = true;
		ExpType firstType = ExpType.FINAL;
		int charIndex = -1;
		Expression prev = null;
		int gapHeight = 0;
		for (int i = 0; i < doc.getDefaultRootElement().getElementCount(); i++) {
			height++;
			Token token = doc.getTokenListForLine(i);
			while (token != null && token.offset != -1) {
				//System.out.println(token);
				if (charIndex == -1) {
					charIndex = token.offset;
				}
				if (!token.isComment() && !token.isWhitespace() && !token.isSingleChar('\r')) {
					contents.append(token.text, token.textOffset, token.textCount);
					if (firstType == ExpType.FINAL && !token.isSingleChar('(')) {
						firstType = token.type == Token.RESERVED_WORD_2 ? ExpType.UNDOABLE : ExpType.NORMAL;
						if (token.getLexeme().equalsIgnoreCase("defproperty")) {
							firstType = ExpType.UNDOABLE;
						}
						gapHeight = height - 1;
						height = 1;
					}
					contentsAdmittable = true;
				} else if (token.isWhitespace()) {
					contents.append(token.text, token.textOffset, token.textCount);
				}
				if (token.isSingleChar('(')) {
					parenLevel++;
				} else if (token.isSingleChar(')') && parenLevel > 0) {
					parenLevel--;
				}
                boolean nextTokenIsNull = token.getNextToken() == null ||
                		token.getNextToken().type == Token.NULL;

				if (parenLevel == 0 && nextTokenIsNull && contents.length() != 0 && contentsAdmittable) {
					if (prev != null) {
						prev.nextGapHeight = gapHeight;
					}
					prev = new Expression(height, contents.toString(), firstType, charIndex,
							token.offset + token.textCount, prev);
					prev.prevGapHeight = gapHeight;
					r.add(prev);
					firstType = ExpType.FINAL;
					charIndex = -1;
					contents = new StringBuilder();
					contentsAdmittable = false;
					height = 0;
				}
				if (first) first = false;
				token = token.getNextToken();
			}
			contents.append('\n');
		}
		r.add(new Expression(height, "", ExpType.FINAL, charIndex, -1, prev));
		//for (Expression exp : r) {
		//	System.out.println(exp.first == null ? "null" : exp.first);
		//}
		return r;
	}
}
