package org.proofpad;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ConcurrentModificationException;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JComponent;

import org.proofpad.ProofBar.ExpData;

public class MoreBar extends JComponent {
	
	private static final ImageIcon moreIcon = Repl.moreIcon;
	private static final long serialVersionUID = -2510084974061378819L;
	private static final int width = 20;

	List<ExpData> data;
	
	public MoreBar(final IdeWindow win) {
		setPreferredSize(new Dimension(width, 0));
		setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, Color.LIGHT_GRAY));
		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				int offset = 0;
				if (data == null) return;
				for (ExpData ex : data) {
					if (ex == null) continue;
					if (e.getY() > offset && e.getY() <= offset + ex.getHeight()
							&& ex.output.length() > 0) {
						win.setPreviewText(ex.output);
						return;
					}
					offset += ex.getHeight();
				}
			}
		});
	}
	
	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		int offset = 0;
		if (data == null) return;
		try {
			for (ExpData ex : data) {
				if (ex == null) continue;
				int height = ex.getHeight();
				g.setColor(Color.GRAY);
				g.drawLine(0, offset + height, getWidth(), offset + height);
				if (ex.output.length() > 0) {
					g.drawImage(moreIcon.getImage(), (getWidth() - 19) / 2, (height - 19) / 2 + offset,
							this);
				}
				offset += height;
			}
		} catch (ConcurrentModificationException e) {
			repaint();
		}
	}
	
	public void updateWith(List<ExpData> d) {
		data = d;
		repaint();
	}
}
