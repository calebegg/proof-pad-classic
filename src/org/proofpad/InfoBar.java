package org.proofpad;

import java.awt.Color;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.CompoundBorder;

public class InfoBar extends JPanel {
	public static interface CloseListener {
		void onClose();
	}

	private static final long serialVersionUID = 3897859262368385104L;
	static final CompoundBorder INFO_BAR_BORDER = BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(0, 0, 1, 0, Color.BLACK),
			BorderFactory.createEmptyBorder(0, 5, 0, 5));
	static final Color INFO_BAR_COLOR = new Color(.95f, .95f, .95f);
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
		setBackground(INFO_BAR_COLOR);
		setBorder(INFO_BAR_BORDER);
		add(new JLabel(msg));
		add(Box.createHorizontalGlue());
		for (InfoButton ib : buttons) {
			JButton btn = new JButton(ib.text);
			btn.putClientProperty("JButton.buttonType", "roundRect");
			btn.addActionListener(ib.listener);
			add(btn);
		}
		JButton closeBtn = new JButton("Close");
		closeBtn.putClientProperty("JButton.buttonType", "roundRect");
		closeBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				close();
			}});
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
