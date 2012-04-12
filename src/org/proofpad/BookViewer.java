package org.proofpad;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeSelectionModel;

public class BookViewer extends JFrame {

	private static final String SYSTEM_BOOKS_SYMBOL = ":system";
	private static final String DRACULA_SYMBOL = ":teachpacks";
	String draculaPath;
	String systemPath;
	
	private static final long serialVersionUID = 1276853161919844567L;
	
	private class BookView {
		private File f;
		private String symbol;
		public BookView(File f, String symbol) {
			this.f = f;
			this.symbol = symbol;
		}
		@Override
		public String toString() {
			return f.getName();
		}
		public String getDirSymbol() {
			return symbol;
		}
		public String getPath() {
			String path = f.getAbsolutePath();
			int pathLen;
			if (symbol == null) {
				pathLen = 0;
		    } else if (symbol.equals(SYSTEM_BOOKS_SYMBOL)) {
				pathLen = systemPath.length() + 1;
			} else if (symbol.equals(DRACULA_SYMBOL)) {
				pathLen = draculaPath.length() + 1;
			} else {
				pathLen = 0;
			}
			return path.substring(pathLen, path.length() - 5);
		}
		public boolean isBook() {
			return f.getName().endsWith(".lisp");
		}
	}
	
	public BookViewer(final IdeWindow parent) {
		super("Include a book");
		String acl2Dir = new File(parent.acl2.acl2Path).getParent();
		systemPath = acl2Dir + "/books";
		draculaPath =  acl2Dir + "/dracula";
		System.out.println(systemPath);
		getRootPane().putClientProperty("apple.awt.brushMetalLook", "false");
		BorderLayout bl = new BorderLayout();
		bl.setHgap(8);
		bl.setVgap(8);
		getContentPane().setLayout(bl);
		getRootPane().setBorder(Main.WINDOW_BORDER);
		DefaultMutableTreeNode root = new DefaultMutableTreeNode("Books");
		DefaultMutableTreeNode sysBooks = nodeFromFile(new File(systemPath), SYSTEM_BOOKS_SYMBOL, 10);
		sysBooks.setUserObject(SYSTEM_BOOKS_SYMBOL);
		root.add(sysBooks);
		DefaultMutableTreeNode dracula = nodeFromFile(new File(draculaPath), DRACULA_SYMBOL, 10);
		dracula.setUserObject(":teachpacks");
		root.add(dracula);
		final JTree tree = new JTree(root);
		for (int i = tree.getRowCount(); i > 0; i--) {
			tree.expandRow(i);
		}
		tree.setRootVisible(false);
		tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		add(tree);
		// TODO: Add and remove root directory buttons
		// button.putClientProperty("JButton.buttonType", "gradient")
		JPanel buttons = new JPanel();
		buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));
		JButton include = new JButton("Include");
		include.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
				BookView bv = (BookView) node.getUserObject();
				if (!bv.isBook()) {
					return;
				}
				System.out.println(bv.getDirSymbol());
				parent.includeBookAtCursor(bv.getDirSymbol(), bv.getPath());
			}
		});
		JButton cancel = new JButton("Close");
		cancel.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		});
		buttons.add(Box.createGlue());
		if (IdeWindow.isMac) {
			buttons.add(cancel);
			buttons.add(include);
		} else {
			buttons.add(include);
			buttons.add(cancel);
		}
		add(buttons, BorderLayout.SOUTH);
		setPreferredSize(new Dimension(300, 600));
		pack();
		setLocationRelativeTo(parent);
	}
	
	private DefaultMutableTreeNode nodeFromFile(File dir, String sym, int maxdepth) {
		DefaultMutableTreeNode r = new DefaultMutableTreeNode(new BookView(dir, null));
		if (maxdepth == 0 || !dir.isDirectory()) return r;
		for (File f : dir.listFiles()) {
			if (f.isDirectory()) {
				r.add(nodeFromFile(f, sym, maxdepth - 1));
			} else {
				if (f.getName().endsWith(".lisp")) {
					r.add(new DefaultMutableTreeNode(new BookView(f, sym)));
				}
			}
		}
		return r;
	}
}
