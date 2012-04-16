package org.proofpad;

import java.awt.Font;

import javax.swing.*;
import javax.swing.tree.*;

public class TraceResult extends JTree {
	private static final long serialVersionUID = 2124327473664301528L;
	
	public final String input;

	public TraceResult(String trace, String input) {
		super(new DefaultMutableTreeNode(input));
		
		this.input = input;
		
		DefaultMutableTreeNode root = (DefaultMutableTreeNode) getModel().getRoot();
		DefaultMutableTreeNode node = root;
		
		String[] lines = trace.split("\\s*\\n\\s*");
		for (int i = 0; i < lines.length; i++) {
			String line;
			if (i >= 1000) {
				line = "Error: Too much output.";
				i = lines.length;
			} else {
				line = lines[i];
			}
			if (Acl2.isError(line)) {
				for (i++; i < lines.length; i++) {
					line += " " + lines[i];
				}
				node.add(new DefaultMutableTreeNode(Repl.cleanUpMsg(line)));
				break;
			}
			String uncompiledMarker = "ACL2_\\*1\\*_.*?::";
			line = line.replaceAll(uncompiledMarker, "");
			int spaceIdx = line.indexOf(' ');
			if (spaceIdx == -1) break;
			if (line.charAt(0) == '<') {
				// Return line
				line = line.substring(spaceIdx + 1, line.length()).replaceFirst("\\(.*? (.*)\\)", "$1");
				node.setUserObject(node.getUserObject() + " = " + line);
				node = (DefaultMutableTreeNode) node.getParent();
			} else if (line.indexOf('>') > 0) {
				// Call line
				line = line.substring(spaceIdx, line.length());
				DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(line);
				node.add(newNode);
				node = newNode;
			} else {
				continue;
			}
		}
		if (node != root && node.getPath().length > 2) {
			expandPath(new TreePath(node.getPath()));
		} else {
			for (int i = 0; i < getRowCount() /* recalculate every time */; i++) {
				expandRow(i);
			}
		}
		final DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer();
		// TODO: icons?
		renderer.setLeafIcon(null);
		renderer.setOpenIcon(null);
		renderer.setClosedIcon(null);
		PrefsWindow.addFontChangeListener(new PrefsWindow.FontChangeListener() {
			@Override
			public void fontChanged(Font font) {
				renderer.setFont(font);
			}
		});
		setCellRenderer(renderer);
	}
}
