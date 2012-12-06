package org.proofpad;

import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FilenameFilter;

import javax.swing.JFileChooser;

class OpenAction implements ActionListener {
	public static final ActionListener instance = new OpenAction();
	@Override
	public void actionPerformed(ActionEvent e) {
		File file;
		if (Main.OSX) {
			FileDialog fd = new FileDialog((Frame)null, "Open file");
			fd.setFilenameFilter(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					return name.endsWith(".lisp") || name.endsWith(".lsp")
							|| name.endsWith(".acl2");
				}
			});
			fd.setVisible(true);
			String filename = fd.getFile();
			file = filename == null ? null : new File(fd.getDirectory(), filename);
		} else {
			int response = PPWindow.fc.showOpenDialog(null);
			file = response == JFileChooser.APPROVE_OPTION ? PPWindow.fc.getSelectedFile() : null;
		}
		if (file != null) {
			PPWindow.createOrReuse(file);
		}
	}
}