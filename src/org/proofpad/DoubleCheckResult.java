package org.proofpad;

import java.awt.Color;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.UIManager;

public class DoubleCheckResult extends JPanel {
	private static final long serialVersionUID = -2822280272207904268L;
	final static Icon openIcon = (Icon) UIManager.get("Tree.expandedIcon");
	final static Icon closedIcon = (Icon) UIManager.get("Tree.collapsedIcon");
	
	public DoubleCheckResult(String output) {
		setBackground(Color.WHITE);
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		String[] lines = output.split("\n");
		ArrayList<String> failures = new ArrayList<String>();
		ArrayList<String> successes = new ArrayList<String>();
		for (int i = 1; i < lines.length; i++) {
			String header = lines[i];
			if (header.isEmpty()) break;
			StringBuilder sb = new StringBuilder();
			i++;
			if (i >= lines.length) break;
			String line = lines[i];
			do {
				sb.append(line); sb.append('\n');
				i++;
				if (i >= lines.length) break;
				line = lines[i];
			} while (line.startsWith("  "));
			i--;
			if (header.startsWith("Failure")) {
				failures.add(sb.toString());
			} else {
				successes.add(sb.toString());
			}
		}
		
		JToggleButton showHideFailures = new JToggleButton("Failed runs", true);
		JToggleButton showHideSuccesses = new JToggleButton("Successful runs", false);

		JPanel failurePanel = new JPanel();
		failurePanel.setLayout(new BoxLayout(failurePanel, BoxLayout.Y_AXIS));
		failurePanel.setBackground(Color.WHITE);
		JPanel successPanel = new JPanel();
		successPanel.setLayout(new BoxLayout(successPanel, BoxLayout.Y_AXIS));
		successPanel.setBackground(Color.WHITE);
		successPanel.setVisible(false);
		
		addCases(failures, failurePanel);
		linkToggleWithPanel(showHideFailures, failurePanel);
		addCases(successes, successPanel);
		linkToggleWithPanel(showHideSuccesses, successPanel);

		add(showHideFailures);
		add(failurePanel);
		add(showHideSuccesses);
		add(successPanel);
	}

	private static void addCases(ArrayList<String> cases, JPanel casePanel) {
		for (String caseStr : cases) {
			JTextArea caseComp = new JTextArea(caseStr);
			caseComp.setFont(Prefs.font.get());
			caseComp.setEditable(false);
			caseComp.setAlignmentX(LEFT_ALIGNMENT);
			casePanel.add(caseComp);
		}
	}
	
	private static void linkToggleWithPanel(final JToggleButton b, final JPanel p) {
		p.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
		b.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		b.setContentAreaFilled(false);
		b.setIcon(b.isSelected() ? openIcon : closedIcon);
		b.setFont(b.getFont().deriveFont(14f));
		b.addItemListener(new ItemListener() {
			@Override public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					p.setVisible(true);
					b.setIcon(openIcon);
				} else {
					p.setVisible(false);
					b.setIcon(closedIcon);
				}
			}
		});
	}
}
