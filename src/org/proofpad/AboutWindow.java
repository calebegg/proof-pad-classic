package org.proofpad;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;

public class AboutWindow extends JDialog {
	class License {
		final String title;
		final String body;

		public License(String title, String body) {
			this.title = title;
			this.body = body;
		}
	}

	License[] licenses = new License[] {
			new License("RSyntaxTextArea",
					"Copyright (c) 2012, Robert Futrell\n" +
					"All rights reserved.\n" +
					"\n" +
					"Redistribution and use in source and binary forms, with or without\n" +
					"modification, are permitted provided that the following conditions are met:\n" +
					"    * Redistributions of source code must retain the above copyright\n" +
					"      notice, this list of conditions and the following disclaimer.\n" +
					"    * Redistributions in binary form must reproduce the above copyright\n" +
					"      notice, this list of conditions and the following disclaimer in the\n" +
					"      documentation and/or other materials provided with the distribution.\n" +
					"    * Neither the name of the author nor the names of its contributors may\n" +
					"      be used to endorse or promote products derived from this software\n" +
					"      without specific prior written permission.\n" +
					"\n" +
					"THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS \"AS IS\" AND\n" +
					"ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED\n" +
					"WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE\n" +
					"DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY\n" +
					"DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES\n" +
					"(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;\n" +
					"LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND\n" +
					"ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT\n" +
					"(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS\n" +
					"SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.\n"),
			new License("ACL2",
					"ACL2 Version 4.3 -- A Computational Logic for Applicative Common Lisp\n" +
					"Copyright (C) 2011 University of Texas at Austin\n" +
					"This version of ACL2 is a descendent of ACL2 Version 1.9, Copyright (C) 1997\n" +
					"Computational Logic, Inc.\n" +
					"\n" +
					"This program is free software; you can redistribute it and/or modify it under the\n" +
					"terms of the GNU General Public License as published by the Free Software\n" +
					"Foundation; either version 2 of the License, or (at your option) any later version.\n" +
					"\n" +
					"This program is distributed in the hope that it will be useful, but WITHOUT ANY\n" +
					"WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A\n" +
					"PARTICULAR PURPOSE. See the GNU General Public License for more details.\n" +
					"\n" +
					"You should have received a copy of the GNU General Public License along with this\n" +
					"program; if not, write to the Free Software Foundation, Inc., 675 Mass Ave,\n" +
					"Cambridge, MA 02139, USA.\n" +
					"\n" +
					"Matt Kaufmann (Kaufmann@cs.utexas.edu)\n" +
					"J Strother Moore (Moore@cs.utexas.edu)\n" +
					"\n" +
					"Department of Computer Sciences\n" +
					"University of Texas at Austin\n" +
					"Austin, TX 78712-1188 U.S.A.\n"),
			// TODO: finish these.
			new License("Proof Pad",
					""),
			// TODO: Dracula's license?
			new License("DrACLua",
					""),
			new License("ACL2s",
					"")};

	private static final long serialVersionUID = 2263577634446940344L;

	public static final Icon icon64 = new ImageIcon("media/IDEIcon64.png");

	int widthFor(Font font, int chars) {
		char[] line = new char[chars];
		Arrays.fill(line, 'X');
		
		return getFontMetrics(font).stringWidth(new String(line));
	}
	
	public AboutWindow() {
		super((JFrame)null, "About Proof Pad");
		getRootPane().putClientProperty("apple.awt.brushMetalLook", "false");
		getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
		getRootPane().setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		JLabel label = new JLabel();
		label.setIcon(icon64);
		label.setAlignmentX(CENTER_ALIGNMENT);
		add(label);
		add(Box.createVerticalStrut(6));
		label = new JLabel("Proof Pad");
		label.setAlignmentX(CENTER_ALIGNMENT);
		label.setFont(label.getFont().deriveFont(Font.BOLD, 16));
		add(label);
		
		label = new JLabel("\u00A9 2012 Caleb Eggensperger");
		label.setAlignmentX(CENTER_ALIGNMENT);
		add(label);
		
		
		Font f = new Font("Monospaced", Font.PLAIN, 12);
		int licenseWidth = widthFor(f, 89);
		JTabbedPane licenseTabs = new JTabbedPane();
		licenseTabs.setAlignmentX(CENTER_ALIGNMENT);
		for (License license: licenses) {
			JTextArea licenseText = new JTextArea(license.body);
			licenseText.setFont(f);
			licenseText.setEditable(false);
			final JScrollPane textScroller = new JScrollPane(licenseText);
			textScroller.setPreferredSize(new Dimension(licenseWidth, 200));
			
			licenseTabs.addTab(license.title, textScroller);
		}
		add(licenseTabs);
		
		add(Box.createVerticalStrut(6));
		
		if (!IdeWindow.isMac) {
			JButton close = new JButton("OK");
			close.setAlignmentX(RIGHT_ALIGNMENT);
			close.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					dispose();
				}
			});
			add(close);
		}
		
		pack();
		setLocationRelativeTo(null);
	}
}
