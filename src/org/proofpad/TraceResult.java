package org.proofpad;

import java.awt.Font;
import java.awt.Frame;

import javax.swing.*;
import javax.swing.tree.*;

public class TraceResult extends ResultWindow {
	private static final long serialVersionUID = 2124327473664301528L;

	public TraceResult(Frame parent, String trace, String input) {
		super(parent, "Trace Output for " + input);
		DefaultMutableTreeNode root = new DefaultMutableTreeNode(input);
		//int level = 0;
		DefaultMutableTreeNode node = root;
		String[] lines = trace.split("\\s*\\n\\s*");
		for (int i = 0; i < lines.length; i++) {
			String line = lines[i];
			if (Acl2.isError(line)) {
				for (i++; i < lines.length; i++) {
					line += " " + lines[i];
				}
				node.add(new DefaultMutableTreeNode(Repl.cleanUpMsg(line)));
				break;
			}
			String uncompiledMarker = "ACL2_\\*1\\*_.*?::";
//			if (!line.matches(".*" + uncompiledMarker)) {
//				System.out.println("Doesn't match: " + line);
//				continue;
//			}
			String oldLine = line;
			line = line.replaceAll(uncompiledMarker, "");
			if (line.equals(oldLine)) {
				continue;
			}
			//int oldLevel = level;
			int spaceIdx = line.indexOf(' ');
			if (line.indexOf(' ') == -1) break;
			if (line.charAt(0) == '<') {
				// Return line
				// level = Integer.parseInt(line.substring(1, spaceIdx));
				line = line.substring(spaceIdx + 1, line.length()).replaceFirst("\\(.*? (.*)\\)", "$1");
				node.setUserObject(node.getUserObject() + " = " + line);
				node = (DefaultMutableTreeNode) node.getParent();
			} else {
				// Call line
				// level = Integer.parseInt(line.substring(0, spaceIdx - 1));		
				line = line.substring(spaceIdx, line.length());
				DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(line);
				node.add(newNode);
				node = newNode;
			}
		}
		JTree tree = new JTree(root);
		if (node != root) {
			tree.expandPath(new TreePath(node.getPath()));
		} else {
			for (int i = 0; i < tree.getRowCount(); i++) {
				tree.expandRow(i);
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
		tree.setCellRenderer(renderer);
		setContent(tree);
	}
}
