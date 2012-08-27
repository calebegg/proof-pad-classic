package org.proofpad;

import java.awt.Font;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;

public class TraceResult extends JTree {
	private static final long serialVersionUID = 2124327473664301528L;
	
	public final String input;

	public TraceResult(String trace, String input) {
		super(new DefaultMutableTreeNode(input));
		
		this.input = input;
		
		DefaultMutableTreeNode root = (DefaultMutableTreeNode) getModel().getRoot();
		DefaultMutableTreeNode node = root;
		
		String[] lines = trace.split("\\n");
		for (int i = 0; i < lines.length; i++) {
			String line;
			line = lines[i];
//			System.out.println(line);
			line = line.replaceAll("^\\s*<\\d+\\s", "")
					   .replaceAll("^\\s*\\d+>\\s", "")
					   .replaceAll("ACL2_\\*1\\*_ACL2::", "");
			if (Acl2.isError(line)) {
				for (i++; i < lines.length; i++) {
					line += " " + lines[i];
				}
				if (node != null) {
					node.add(new DefaultMutableTreeNode(Repl.cleanUpMsg(line)));
				}
				break;
			}
			final String beginMarker = "__trace-enter-";
			final String endMarker = "__trace-exit-";
			if (line.startsWith(beginMarker)) {
				// Call line
				line = line.substring(beginMarker.length(), line.length());
				line = line.replaceAll("__TRACE-", "");
				line = line.toLowerCase();
				if (node != null) {
					DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(line);
					node.add(newNode);
					node = newNode;
				}
			} else if (line.startsWith(endMarker)) {
				// Return line
				line = line.substring(endMarker.length(), line.length());
				line = line.replaceAll("__TRACE-", "");
				line = line.toLowerCase();
				if (node != null) {
					node.setUserObject(node.getUserObject() + line);
					node = (DefaultMutableTreeNode) node.getParent();
				}
			} else {
				// Part of previous line.
				line = line.replaceAll("__TRACE-", "");
				line = line.toLowerCase();
				if (node != null) {
					node.setUserObject(node.getUserObject() + "\n" + line);
				}
			}
		}
		if (node != null && node != root && node.getPath().length > 2) {
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
