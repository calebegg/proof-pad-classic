package org.proofpad;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.fife.ui.rtextarea.SearchContext;
import org.fife.ui.rtextarea.SearchEngine;

public class FindBar extends JPanel {
	private static final long serialVersionUID = 1969572822439206808L;

	enum Direction { UP, DOWN }
	
	public ActionListener findAction;
	boolean findBarIsOpen;
	private final PPWindow window;

	public FindBar(final PPWindow window) {
		this.window = window;
		new JPanel();
		final JTextField searchField = new JTextField();
		findAction = new ActionListener() {

			@Override public void actionPerformed(ActionEvent e) {
				if (!findBarIsOpen) {
					findBarIsOpen = true;
					window.setInfoBar(FindBar.this);
				} else {
					findBarIsOpen = false;
					window.closeInfoBar();
				}
				searchField.setText(window.editor.getSelectedText());
				searchField.requestFocusInWindow();
			}
		};

		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		setBorder(InfoBar.INFO_BAR_BORDER);
		setBackground(Colors.INFO_BAR);
		if (!Main.OSX) {
			add(new JLabel("Find: "));
		}
		searchField.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent arg0) {
				searchFor(searchField.getText(), Direction.DOWN);
			}
		});
		searchField.addKeyListener(new KeyAdapter() {
			@Override public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
					findAction.actionPerformed(null);
				}
			}

			@Override public void keyTyped(KeyEvent e) {
				char c = e.getKeyChar();
				if (c == KeyEvent.CHAR_UNDEFINED || c == '\n' || c == '\b'
						|| e.isAltDown() || e.isMetaDown() || e.isControlDown()) {
					return;
				}
				if (Prefs.incSearch.get()) {
					window.editor.markAll(searchField.getText() + c, false, false,
							false);
				}
			}
		});
		searchField.putClientProperty("JTextField.variant", "search");
		add(searchField);
		JButton forward = new JButton(new ImageIcon(getClass().getResource("/Icons/Find_Down.png")));
		forward.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) {
				searchFor(searchField.getText(), Direction.DOWN);
			}
		});
		JButton back = new JButton(new ImageIcon(getClass().getResource("/Icons/Find_Up.png")));
		back.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) {
				searchFor(searchField.getText(), Direction.UP);
			}
		});
		back.putClientProperty("JButton.buttonType", "segmentedRoundRect");
		forward.putClientProperty("JButton.buttonType", "segmentedRoundRect");
		back.putClientProperty("JButton.segmentPosition", "first");
		forward.putClientProperty("JButton.segmentPosition", "last");
		add(back);
		add(forward);
		add(Box.createHorizontalStrut(4));
		JButton done = new JButton("done");
		done.putClientProperty("JButton.buttonType", "roundRect");
		done.addActionListener(findAction);
		add(done);
	}

	void searchFor(String text, Direction dir) {
		CodePane editor = window.editor;
		editor.clearMarkAllHighlights();
		SearchContext sc = new SearchContext();
		sc.setSearchFor(text);
		sc.setSearchForward(dir == Direction.DOWN);
		boolean found = SearchEngine.find(editor, sc);
		if (!found) {
			int userCaret = editor.getCaretPosition();
			editor.setCaretPosition(dir == Direction.DOWN ? 0 : editor.getText().length());
			found = SearchEngine.find(editor, sc);
			if (!found) {
				editor.setCaretPosition(userCaret);
				Toolkit.getDefaultToolkit().beep();
			}
		}
	}

}
