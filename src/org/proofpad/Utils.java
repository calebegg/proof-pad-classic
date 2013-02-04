package org.proofpad;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.text.BadLocationException;

public class Utils {
	public static void reindent(CodePane editor) {
		final PPDocument doc = (PPDocument) editor.getDocument();
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
				String lineStr = editor.getText(offset, editor.getLineEndOffset(line)
						- offset);
				Matcher whitespace = Pattern.compile("^[ \t]*").matcher(lineStr);
				whitespace.find();
				int whitespaceLen = whitespace.group().length();
				
				doc.remove(offset - eolLen, eolLen + whitespaceLen);
				doc.insertString(offset - eolLen, "\n", null);
			} catch (BadLocationException e) { }
		}
	}

	public static void browseTo(String url) {
		try {
			Desktop.getDesktop().browse(new URI(url));
		} catch (IOException e1) {
		} catch (URISyntaxException e1) { }		
	}
}
