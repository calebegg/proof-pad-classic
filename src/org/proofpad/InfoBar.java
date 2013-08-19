package org.proofpad;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedList;
import java.util.List;

public class InfoBar extends JPanel {
	public static interface CloseListener {
		void onClose();
	}

	private static final long serialVersionUID = 3897859262368385104L;
	static final CompoundBorder INFO_BAR_BORDER = BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(0, 0, 1, 0, Color.BLACK),
			BorderFactory.createEmptyBorder(0, 5, 0, 5));
	public static class InfoButton {
		public final ActionListener listener;
		public final String text;
		public InfoButton(String text, ActionListener listener) {
			this.text = text;
			this.listener = listener;
		}
	}

	private final List<CloseListener> closeListeners = new LinkedList<CloseListener>();
	public InfoBar(String msg, InfoButton[] buttons) {
		super();
		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		setBackground(Colors.INFO_BAR);
		setBorder(INFO_BAR_BORDER);
		ActionListener closeAction = new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) {
				close();
			}
		};
		add(new JLabel(msg));
		add(Box.createHorizontalGlue());
		for (InfoButton ib : buttons) {
			JButton btn = new JButton(ib.text);
			btn.putClientProperty("JButton.buttonType", "roundRect");
			btn.addActionListener(ib.listener);
			btn.addActionListener(closeAction);
			add(btn);
		}
		JButton closeBtn = new JButton("Close");
		closeBtn.putClientProperty("JButton.buttonType", "roundRect");
		closeBtn.addActionListener(closeAction);
		add(closeBtn);
	}

	void close() {
		Container parent = getParent();
		parent.remove(InfoBar.this);
		((JComponent) parent).revalidate();
		for (CloseListener cl : closeListeners) {
			cl.onClose();
		}
	}

	public void addCloseListener(CloseListener closeListener) {
		closeListeners.add(closeListener);
	}
}
