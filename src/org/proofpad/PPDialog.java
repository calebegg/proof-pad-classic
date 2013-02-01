package org.proofpad;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowListener;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.KeyStroke;

public class PPDialog extends JDialog {

	private static final int BORDER = 10;
	private static final long serialVersionUID = 2364282576842584082L;
	
	public PPDialog(PPWindow parent, String string) {
		super(parent, string);
		getRootPane().setBorder(BorderFactory.createEmptyBorder(BORDER, BORDER, BORDER, BORDER));
		setResizable(false);
		
		getRootPane().getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "HIDE");
		getRootPane().getActionMap().put("HIDE", new AbstractAction() {
			private static final long serialVersionUID = -5310527346557745533L;
			@Override public void actionPerformed(ActionEvent e) {
				WindowListener[] windowListeners = getWindowListeners();
				for (WindowListener windowListener : windowListeners) {
					windowListener.windowClosing(null);
					windowListener.windowClosed(null);
				}
				switch (getDefaultCloseOperation()) {
				case DISPOSE_ON_CLOSE:
					dispose();
					break;
				case DO_NOTHING_ON_CLOSE:
					break;
				case HIDE_ON_CLOSE:
					setVisible(false);
					break;
				default:
					dispose();
				}
			}
		});
	}

}
