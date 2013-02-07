package org.proofpad;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

public class BookViewer extends PPDialog {

	private static final String SYSTEM_BOOKS_SYMBOL = ":system";
	private static final String DRACULA_SYMBOL = ":teachpacks";
	String draculaPath;
	String systemPath;
	
	private static final long serialVersionUID = 1276853161919844567L;
	
	private class BookView {
		private final File file;
		private final String symbol;
		private File currentDir;
		public BookView(File f, String symbol) {
			this.file = f;
			this.symbol = symbol;
		}
		@Override
		public String toString() {
			return file.getName();
		}
		public String getDirSymbol() {
			return symbol;
		}
		public String getPath() {
			String path = file.getAbsolutePath();
			if (currentDir != null && path.startsWith(currentDir.getAbsolutePath())) {
				path = path.substring(currentDir.getAbsolutePath().length() + 1);
			}
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
			if (Main.WIN) {
				path = path.replace("\\", "/");
			}
			return path.substring(pathLen, path.length() - 5);
		}
		public boolean isBook() {
			return file.getName().endsWith(".lisp");
		}
		public void setCurrentDir(File currentDir) {
			this.currentDir = currentDir;
		}
	}
	
	public BookViewer(final PPWindow parent) {
		super(parent, "Include a book");
		String acl2Dir = new File(parent.acl2.acl2Path.replaceAll("\\\\ ", " ")).getParent();
		systemPath = acl2Dir + "/books";
		draculaPath =  acl2Dir + "/dracula";
		System.out.println(systemPath);
		getRootPane().putClientProperty("apple.awt.brushMetalLook", "false");
		BorderLayout bl = new BorderLayout();
		bl.setHgap(8);
		bl.setVgap(8);
		getContentPane().setLayout(bl);
		DefaultMutableTreeNode root = new DefaultMutableTreeNode("Books");
		if (parent.openFile != null) {
			File workingDir = parent.openFile.getParentFile();
			DefaultMutableTreeNode currDirBooks =
					nodeFromFile(workingDir, null, 10, workingDir);
			currDirBooks.setUserObject("Current Directory");
			root.add(currDirBooks);
		}
		DefaultMutableTreeNode dracula =
				nodeFromFile(new File(draculaPath), DRACULA_SYMBOL, 10);
		dracula.setUserObject(MenuBar.applyTitleCase("Teach packs"));
		root.add(dracula);
		DefaultMutableTreeNode sysBooks =
				nodeFromFile(new File(systemPath), SYSTEM_BOOKS_SYMBOL, 10);
		sysBooks.setUserObject(MenuBar.applyTitleCase("System books"));
		root.add(sysBooks);
		final JTree tree = new JTree(root);
		for (int i = tree.getRowCount(); i > 0; i--) {
			tree.expandRow(i);
		}
		tree.setRootVisible(false);
		tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		tree.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				int selRow = tree.getRowForLocation(e.getX(), e.getY());
				TreePath selPath = tree.getPathForLocation(e.getX(), e.getY());
				if (selPath == null) return;
				DefaultMutableTreeNode last =
						(DefaultMutableTreeNode) selPath.getLastPathComponent();
				if(selRow != -1 && e.getClickCount() == 2 && last.getChildCount() == 0) {
					if (!(last.getUserObject() instanceof BookView)) {
						return;
					}
					BookView bv = (BookView) last.getUserObject();
					if (bv.isBook()) {
						parent.includeBookAtCursor(bv.getDirSymbol(), bv.getPath());
						dispose();
					}
				}
			}
		});
		add(new JScrollPane(tree));
		JPanel buttons = new JPanel();
		buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));
		JButton include = new JButton("Include");
		include.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				DefaultMutableTreeNode node =
						(DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
				if (!(node.getUserObject() instanceof BookView)) {
					return;
				}
				BookView bv = (BookView) node.getUserObject();
				if (!bv.isBook()) {
					return;
				}
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
		if (Main.OSX) {
			buttons.add(cancel);
			buttons.add(include);
		} else {
			buttons.add(include);
			buttons.add(cancel);
		}
		add(buttons, BorderLayout.PAGE_END);
		setPreferredSize(new Dimension(300, 600));
		pack();
		setLocationRelativeTo(parent);
	}
	
	private DefaultMutableTreeNode nodeFromFile(File dir, String sym, int maxdepth) {
		return nodeFromFile(dir, sym, maxdepth, null);
	}

	private DefaultMutableTreeNode nodeFromFile(File dir, String sym, int maxdepth, File workingDir) {
		DefaultMutableTreeNode r = new DefaultMutableTreeNode(new BookView(dir, null));
		if (maxdepth == 0 || !dir.isDirectory()) return r;
		for (File f : dir.listFiles()) {
			if (f.isDirectory()) {
				r.add(nodeFromFile(f, sym, maxdepth - 1));
			} else {
				if (f.getName().endsWith(".lisp")) {
					BookView bookView = new BookView(f, sym);
					bookView.setCurrentDir(workingDir);
					r.add(new DefaultMutableTreeNode(bookView));
				}
			}
		}
		return r;
	}
}
