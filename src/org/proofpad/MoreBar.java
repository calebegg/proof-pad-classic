package org.proofpad;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

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
		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				int offset = 0;
				for (ExpData ex : data) {
					if (e.getY() > offset && e.getY() <= offset + ex.getHeight()) {
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
		for (ExpData ex : data) {
			if (ex == null) continue;
			int height = ex.getHeight();
			g.drawImage(moreIcon.getImage(), (getWidth() - 19) / 2, (height - 19) / 2 + offset, this);
			offset += height;
		}
	}
	
	public void updateWith(List<ExpData> d) {
		data = d;
		System.out.println(data);
		repaint();
	}
}
