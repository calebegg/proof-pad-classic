package org.proofpad;

import org.proofpad.Acl2.OutputChangeListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class Acl2OutputWindow extends JFrame{
	private static final long serialVersionUID = 4417064003350445275L;

	public Acl2OutputWindow(final Acl2 acl2) {
		super("ACL2 Output");
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		final JTextArea output = new JTextArea();
		output.setEditable(false);
		final OutputChangeListener ocl = new OutputChangeListener() {
			@Override public void outputChanged(String string) {
				output.setText(string);
			}
		};
		acl2.addOutputChangeListener(ocl);
		JScrollPane jsp = new JScrollPane(output);
		jsp.setPreferredSize(new Dimension(output.getFontMetrics(output.getFont())
				.charWidth('a') * 80, 9999));
		add(jsp);
		
		addWindowListener(new WindowAdapter() {
			@Override public void windowClosing(WindowEvent e) {
				acl2.removeOutputChangeListener(ocl);
				dispose();
			}
		});
		pack();
	}
}
