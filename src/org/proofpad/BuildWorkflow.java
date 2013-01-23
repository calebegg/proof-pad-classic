package org.proofpad;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JOptionPane;

final class BuildWorkflow implements ActionListener {
	private final PPWindow ppWindow;

	BuildWorkflow(PPWindow ppWindow) {
		this.ppWindow = ppWindow;
	}

	@Override public void actionPerformed(ActionEvent arg0) {
		if (!this.ppWindow.saveFile()) {
			JOptionPane.showMessageDialog(this.ppWindow,
					"Save the current file in order to build", "Build did not complete",
					JOptionPane.INFORMATION_MESSAGE);
			return;
		}

		final BuildWindow builder = new BuildWindow(this.ppWindow.openFile, this.ppWindow.acl2.getAcl2Path());

		builder.setVisible(true);
		builder.build();
	}
}