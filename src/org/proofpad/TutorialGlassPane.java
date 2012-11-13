package org.proofpad;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JComponent;

final class TutorialGlassPane extends JComponent {
	private static final long serialVersionUID = 6361383354592654266L;
	private final IdeWindow window;

	TutorialGlassPane(IdeWindow ideWindow) {
		window = ideWindow;
		setVisible(false);
		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				setVisible(false);
			}
		});
	}

	@Override
	public boolean contains(int x, int y) {
		return y > window.toolbar.getHeight();
	}

	@Override
	public void paintComponent(Graphics g) {
		// More opaque over areas we want to write on
		g.setColor(new Color(.95f, .95f, .95f, .7f));
		g.fillRect(window.proofBar.getWidth() + 2, window.toolbar.getHeight(), getWidth(),
				window.editorScroller.getHeight() + 3);
		g.fillRect(0, getHeight() - window.repl.getHeight(),
				getWidth(), window.repl.getHeight() - window.repl.getInputHeight() - 10);
		g.setColor(new Color(.9f, .9f, .9f, .4f));
		g.fillRect(0, window.toolbar.getHeight(), getWidth(), getHeight());
		g.setColor(new Color(0f, 0f, .7f));
		g.setFont(window.editor.getFont().deriveFont(16f).deriveFont(Font.BOLD));
		int lineHeight = (int) g.getFontMetrics().getLineMetrics("", g).getHeight();
		g.drawString("1. Write your functions here.",
				window.proofBar.getWidth() + 20,
				window.toolbar.getHeight() + 30);
		int step2Y = window.toolbar.getHeight() + window.editorScroller.getHeight() / 6 + 40;
		g.drawString("2. Click to admit them.",
				window.proofBar.getWidth() + 30,
				step2Y);
		g.drawLine(window.proofBar.getWidth() + 24,
				   step2Y - lineHeight / 4,
				   window.proofBar.getWidth() - 10,
				   step2Y - lineHeight / 4);
		g.drawString("3. Test them here.", window.proofBar.getWidth() + 20, getHeight()
				- (int) window.repl.input.getPreferredScrollableViewportSize().getHeight() - 30);
	}
}