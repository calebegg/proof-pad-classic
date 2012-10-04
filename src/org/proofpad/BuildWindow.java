package org.proofpad;

import java.awt.FileDialog;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JProgressBar;

public class BuildWindow extends JFrame {

	private static final long serialVersionUID = 8394742808899908090L;
	private static final boolean OSX = IdeWindow.OSX;
	private static final boolean WIN = IdeWindow.WIN;
	private File file;
	private String acl2Dir;
	private JProgressBar progress;
	Acl2 builder;

	public BuildWindow(File file, String acl2Dir) {
		super("Building " + file.getName() + "...");
		getRootPane().putClientProperty("apple.awt.brushMetalLook", "false");
		this.file = file;
		this.acl2Dir = acl2Dir;
		getContentPane().setLayout(new BoxLayout(this.getContentPane(), BoxLayout.Y_AXIS));
		getRootPane().setBorder(Main.WINDOW_BORDER);
		progress = new JProgressBar();
		progress.setIndeterminate(true);
		progress.setAlignmentX(CENTER_ALIGNMENT);
		add(progress);
		add(Box.createVerticalStrut(8));
		JButton cancel = new JButton("Cancel");
		cancel.setAlignmentX(CENTER_ALIGNMENT);
		cancel.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if (builder != null)
					builder.terminate();
				dispose();
			}
		});
		add(cancel);
		pack();
		setLocationRelativeTo(null);
	}
	
	public void build() {
		List<String> acl2Paths = new ArrayList<String>();
		acl2Paths.add(acl2Dir);
		builder = new Acl2(acl2Paths, file.getParentFile(), null);
		try {
			builder.initialize();
			builder.start();
			builder.admit("(defttag builder)", null);
			builder.admit(":set-raw-mode t", null);
			builder.admit("(load \"" + file.getAbsolutePath()
					+ "\" )", null);
			builder.admit(
					"(defun __main__ () (main state))",
					null);
			final String filename;
			if (OSX) {
				FileDialog fc = new FileDialog(this, "Save Executable...");
				fc.setMode(FileDialog.SAVE);
				fc.setDirectory(file.getPath());
				fc.setFile(file.getName().split("\\.")[0]
						+ (WIN ? ".exe" : ""));
				fc.setVisible(true);
				filename = fc.getDirectory() + fc.getFile();
			} else {
				JFileChooser fc = new JFileChooser();
				fc.showSaveDialog(this);
				filename = fc.getSelectedFile().getAbsolutePath();
			}
			if (filename == null) {
				builder.terminate();
				return;
			}
			builder.admit(
					"(ccl:save-application \""
					+ filename
					+ "\" :toplevel-function #'__main__ :prepend-kernel t)",
					new Acl2.Callback() {
						@Override
						public boolean run(boolean success,
								String response) {
							builder.terminate();
							dispose();
							return false;
						}
					});
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
