package org.proofpad;

import javax.swing.text.BadLocationException;
import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
			} catch (BadLocationException ignored) { }
		}
	}

	public static void browseTo(String url) {
		try {
			Desktop.getDesktop().browse(new URI(url));
		} catch (IOException ignored) {
		} catch (URISyntaxException ignored) { }
	}

    public static String readFile(File file) {
        Scanner scan;
        try {
            scan = new Scanner(file);
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
            return null;
        }
        String content = scan.useDelimiter("\\Z").next();
        content = content.replaceAll("\\r", "");
        scan.close();
        return content;
    }
}
