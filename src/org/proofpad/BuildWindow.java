package org.proofpad;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import javax.swing.*;

public class BuildWindow extends JFrame {

	private static final long serialVersionUID = 8394742808899908090L;
	private static final boolean isMac = IdeWindow.isMac;
	private static final boolean isWindows = IdeWindow.isWindows;
	private File file;
	private String acl2Dir;
	private JProgressBar progress;
	private Acl2 builder;

	public BuildWindow(File file, String acl2Dir) {
		super("Building " + file.getName() + "...");
		getRootPane().putClientProperty("apple.awt.brushMetalLook", "false");
		this.file = file;
		this.acl2Dir = acl2Dir;
		getContentPane().setLayout(new BoxLayout(this.getContentPane(), BoxLayout.Y_AXIS));
		getRootPane().setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
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
		builder = new Acl2(acl2Dir, file.getParentFile());
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
			if (isMac) {
				FileDialog fc = new FileDialog(this, "Save Executable...");
				fc.setMode(FileDialog.SAVE);
				fc.setDirectory(file.getPath());
				fc.setFile(file.getName().split("\\.")[0]
						+ (isWindows ? ".exe" : ""));
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
					+ "\" :toplevel-function #'__main__\n"
					+ ":prepend-kernel t)",
					new Acl2.Callback() {
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
