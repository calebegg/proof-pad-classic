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

import org.proofpad.Acl2.RestartListener;

public class BuildWindow extends JFrame {

	private static final long serialVersionUID = 8394742808899908090L;
	private static final boolean OSX = Main.OSX;
	private final File file;
	private final String acl2Dir;
	private final JProgressBar progress;
	Acl2 builder;

	public BuildWindow(File file, String acl2Dir) {
		super("Building " + file.getName() + "...");
		getRootPane().putClientProperty("apple.awt.brushMetalLook", "false");
		this.file = file;
		this.acl2Dir = acl2Dir;
		getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
		getRootPane().setBorder(Main.WINDOW_BORDER);
		progress = new JProgressBar();
		progress.setIndeterminate(true);
		progress.setAlignmentX(CENTER_ALIGNMENT);
		add(progress);
		add(Box.createVerticalStrut(8));
		JButton cancel = new JButton("Cancel");
		cancel.setAlignmentX(CENTER_ALIGNMENT);
		cancel.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent arg0) {
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
		builder = new Acl2(acl2Paths, file.getParentFile());
		try {
			builder.initialize();
			builder.start();
			builder.admit("(defttag builder)", null);
			builder.admit(":set-raw-mode t", null);
			String sourcePath = file.getAbsolutePath();
			if (Main.WIN) {
				sourcePath = sourcePath.replace('\\', '/');
			}
			builder.admit("(load \"" + sourcePath
					+ "\" )", null);
			builder.admit(
					"(defun __main__ () (main state))",
					null);
			String filename;
			if (OSX) {
				FileDialog fc = new FileDialog(this, "Save Executable...");
				fc.setMode(FileDialog.SAVE);
				fc.setDirectory(file.getPath());
				fc.setFile(file.getName().split("\\.")[0]);
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
			if (Main.WIN) {
				filename = filename.replace('\\', '/');
			}
			String buildCmd = "(ccl:save-application \"" + filename
					+ "\" :toplevel-function #'__main__ :prepend-kernel t)";
//			System.out.println("Build commmand: " + buildCmd);
			builder.addRestartListener(new RestartListener() {
				@Override public void acl2Restarted() {
					dispose();
				}
			});
			builder.admit(buildCmd, null);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
