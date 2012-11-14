package org.proofpad;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.DefaultFocusTraversalPolicy;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class ErrorWindow extends JDialog {
	private static final int MAX_HEIGHT = 700;
	private static final int MAX_WIDTH = 700;
	private static final long serialVersionUID = -7011768662687047633L;

	public ErrorWindow(Throwable e) {
		super((JFrame)null, "Error");
		getContentPane().setLayout(new BorderLayout());
		getRootPane().setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
		StringBuilder sb = new StringBuilder(e + ": " + e.getMessage());
		StackTraceElement[] stes = e.getStackTrace();
		for (StackTraceElement ste : stes) {
			sb.append("\n" + ste);
		}
		final String stackTrace = sb.toString();
		JLabel errLabel = new JLabel("<html>An unexpected error occurred:<br>" + stackTrace.split("\n")[0]
				+ "</html>");
		errLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
		add(errLabel, BorderLayout.PAGE_START);
		final JPanel options = new JPanel();
		options.setLayout(new BoxLayout(options, BoxLayout.X_AXIS));
		options.add(Box.createHorizontalGlue());
		final JButton close = new JButton("Ignore");
		setFocusTraversalPolicy(new DefaultFocusTraversalPolicy() {
			private static final long serialVersionUID = -3529575318468515194L;
			@Override public Component getDefaultComponent(Container aContainer) {
				return close;
			}
		});
		close.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent ev) {
				dispose();
			}
		});
		final JButton copy = new JButton("Copy");
		copy.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent ev) {
				Toolkit.getDefaultToolkit().getSystemClipboard()
						.setContents(new StringSelection(stackTrace), null);
			}
		});
		final JButton disclose = new JButton("Details...");
		final JButton quit = new JButton(termWord());
		quit.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent ev) {
				Object[] opts = new Object[] { termWord(), "Cancel" };
				int choice = JOptionPane.showOptionDialog(ErrorWindow.this,
						"Are you sure you want to " + termWord().toLowerCase() + " Proof Pad? " +
								"Any unsaved work will be lost.",
						"",
						JOptionPane.OK_CANCEL_OPTION,
						JOptionPane.WARNING_MESSAGE,
						null,
						opts,
						opts[1]);
				if (choice == JOptionPane.OK_OPTION) {
					Main.quit();
				}
				
			}
		});
		disclose.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent ev) {
				JTextArea errorText = new JTextArea();
				errorText.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
				JScrollPane sp = new JScrollPane(errorText);
				errorText.setText(stackTrace);
				errorText.setCaretPosition(0);
				errorText.setEditable(false);
				Dimension size = errorText.getPreferredScrollableViewportSize();
				int scrollbarSize = new JScrollBar().getPreferredSize().width;
				sp.setPreferredSize(new Dimension(Math.min(MAX_WIDTH, size.width + 5 + scrollbarSize),
						Math.min(MAX_HEIGHT, size.height + 5 + scrollbarSize)));
				add(sp, BorderLayout.CENTER);
				getRootPane().revalidate();
				options.remove(disclose);
				options.remove(quit);
				options.remove(close);
				options.add(copy);
				options.add(quit);
				options.add(close);
				pack();
				setLocationRelativeTo(null);
			}
		});
		options.add(disclose);
		options.add(quit);
		options.add(close);
		add(options, BorderLayout.PAGE_END);
		pack();
		setLocationRelativeTo(null);
		Main.userData.addError(stackTrace);
	}

	static String termWord() {
		return Main.OSX ? "Force Quit" : "Terminate";
	}

}
