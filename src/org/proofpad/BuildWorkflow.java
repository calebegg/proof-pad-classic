package org.proofpad;

import org.proofpad.Acl2.Callback;
import org.proofpad.Acl2.RestartListener;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class BuildWorkflow implements ActionListener {

	public static class BuildWindow extends PPDialog {
		private static final long serialVersionUID = 8394742808899908090L;
		static final boolean OSX = Main.OSX && !Main.FAKE_WINDOWS;
		static final boolean WIN = Main.WIN || Main.FAKE_WINDOWS;
		private final String acl2Dir;
//		private final JProgressBar progress;
		Acl2 builder;
		final FilePicker destinationPicker;
	    private final File sourceFile;
		public BuildWindow(PPWindow parent, final File sourceFile, String acl2Dir, boolean tempFile) {
			super(parent, "Build an executable");
			this.sourceFile = sourceFile;
			getRootPane().putClientProperty("apple.awt.brushMetalLook", "false");
			this.acl2Dir = acl2Dir;
			getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
			JLabel destLabel = new JLabel("Destination:");
			destLabel.setAlignmentX(LEFT_ALIGNMENT);
			destLabel.setMaximumSize(new Dimension(2<<16, 2<<16));
			add(destLabel);
			add(Box.createVerticalStrut(8));
			destinationPicker = new FilePicker("Choose destination for executable...", FileDialog.SAVE);
			if (!tempFile) {
				destinationPicker.setPath(sourceFile.getAbsolutePath().replaceFirst(".lisp$",
                        WIN ? ".exe" : ""));
			}
			destinationPicker.setAlignmentX(LEFT_ALIGNMENT);
			final JButton buildButton = new JButton("Build");
			buildButton.setEnabled(!destinationPicker.getPath().isEmpty());
			destinationPicker.addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent arg0) {
                    buildButton.setEnabled(!destinationPicker.getPath().isEmpty());
                }
            });
			add(destinationPicker);
			add(Box.createVerticalStrut(8));
			JPanel buttonPanel = new JPanel();
			buttonPanel.setAlignmentX(LEFT_ALIGNMENT);
			buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
			buttonPanel.add(Box.createGlue());
			JButton cancelButton = new JButton("Cancel");
			cancelButton.addActionListener(new ActionListener() {
				@Override public void actionPerformed(ActionEvent arg0) {
					if (builder != null)
						builder.terminate();
					dispose();
				}
			});
			buildButton.addActionListener(new ActionListener() {
				@Override public void actionPerformed(ActionEvent e) {
					if (destinationPicker.getFile().exists()) {
						int resp = JOptionPane.showOptionDialog(BuildWindow.this,
								destinationPicker.getPath() + " already exists", "File exists",
								JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null,
								new String[] { "Overwrite", "Cancel" }, "Overwrite");
						if (resp == 1) return;
					}
					setTitle("Building...");
					build(sourceFile.getAbsolutePath(), destinationPicker.getPath());
					buildButton.setEnabled(false);
					destinationPicker.setEnabled(false);
				}
			});
			if (OSX) {
				buttonPanel.add(cancelButton);
				buttonPanel.add(buildButton);
			} else {
				buttonPanel.add(buildButton);
				buttonPanel.add(cancelButton);
			}
			add(buttonPanel);
			setMinimumSize(new Dimension(400, 0));
			pack();
			setLocationRelativeTo(null);
			getRootPane().setDefaultButton(buildButton);
			buildButton.requestFocus();
		}

		public void build(String source, final String dest) {
			List<String> acl2Paths = new ArrayList<String>();
			acl2Paths.add(acl2Dir);
			builder = new Acl2(acl2Paths, sourceFile.getParentFile(), true);
			try {
				builder.initialize();
				builder.start();
				builder.admit("(defttag builder)", null);
				builder.admit(":set-raw-mode t", null);
				String escDest = dest;
				if (WIN) {
					source = source.replace('\\', '/');
					escDest = dest.replace('\\', '/');
				}
				builder.admit("(load \"" + source + "\" )", null);
				builder.admit("(defun __main__ () (main state))", null);
				String buildCmd = "(ccl:save-application \"" + escDest
						+ "\" :toplevel-function #'__main__ :prepend-kernel t)";
				System.out.println("buildCmd: " + buildCmd);
				builder.addRestartListener(new RestartListener() {
					@Override public void acl2Restarted() {
						dispose();
						if (WIN) {
							try {
								Runtime.getRuntime().exec("explorer /select, " + dest);
							} catch (IOException ignored) { }
						} else if (OSX) {
							try {
								Runtime.getRuntime().exec("open -R " + dest);
							} catch (IOException ignored) { }
						} else if (Desktop.isDesktopSupported()) {
							Desktop desktop = Desktop.getDesktop();
							try {
								desktop.open(new File(dest).getParentFile());
							} catch (IOException ignored) { }
						}
					}
				});
				builder.admit(buildCmd, new Callback() {
					@Override public boolean run(boolean success, String response) {
						return false;
					}
				});
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private final PPWindow ppWindow;

	BuildWorkflow(PPWindow ppWindow) {
		this.ppWindow = ppWindow;
	}

	@Override public void actionPerformed(ActionEvent e) {
		File sourceFile;
		boolean tempFile;
		if (ppWindow.openFile == null) {
			// Use a temporary file instead
			try {
				sourceFile = File.createTempFile("proof-pad-build-source", null);
				tempFile = true;
			} catch (IOException ex) {
				JOptionPane.showMessageDialog(null, "Proof Pad was unable to create a temporary " +
						"file. Save the file you want to build and try again.", "Build error",
						JOptionPane.ERROR_MESSAGE);
				return;
			}
		} else {
			this.ppWindow.saveFile();
			sourceFile = ppWindow.openFile;
			tempFile = false;
		}
		final BuildWindow builder = new BuildWindow(ppWindow, sourceFile,
				this.ppWindow.acl2.getAcl2Path(), tempFile);
		builder.setVisible(true);
	}
}