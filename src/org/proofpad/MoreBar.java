package org.proofpad;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JComponent;

import org.proofpad.ProofBar.ExpData;

public class MoreBar extends JComponent {
	
	private static final ImageIcon moreIcon = Repl.moreIcon;
	private static final long serialVersionUID = -2510084974061378819L;
	private static final int width = 20;
	int selectedIdx = -1;
	int oldIdx = -1;
	long rotateStart;

	List<ExpData> data;
	private final IdeWindow win;
	
	public MoreBar(final IdeWindow win) {
		this.win = win;
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
						selectExpression(ex);
						return;
					}
					offset += ex.getHeight();
				}
			}
		});
	}
	
	@Override
	protected void paintComponent(Graphics gOld) {
		Graphics2D g = (Graphics2D) gOld;
		super.paintComponent(g);
		int offset = 0;
		if (data == null) return;
		try {
			boolean drewSelected = false;
			for (ExpData ex : data) {
				if (ex == null) continue;
				int height = ex.getHeight();
				double angle = 0;
				long currTime = System.currentTimeMillis();
				if (ex.exp.expNum == selectedIdx) {
					if (currTime - rotateStart < 200) {
						angle = Math.toRadians(180 * (currTime - rotateStart) / 200.0);
						repaintAfter(30);
					} else {
						angle = Math.toRadians(180);
					}
					g.setColor(ProofBar.untriedColor);
					g.fillRect(0, offset, getWidth(), height);
					drewSelected = true;
				} else if (ex.exp.expNum == oldIdx) {
					if (currTime - rotateStart < 200) {
						angle = Math.toRadians(180 - 180 * (currTime - rotateStart) / 200.0);
						repaintAfter(30);
					} else {
						oldIdx = -1;
						angle = 0;
					}
				}
				g.setColor(Color.GRAY);
				g.drawLine(0, offset + height, getWidth(), offset + height);
				if (ex.output.length() > 0) {
					AffineTransform savedTx = g.getTransform();
					g.rotate(angle, getWidth() / 2, offset + height / 2);
					g.drawImage(moreIcon.getImage(), (getWidth() - 19) / 2, (height - 19) / 2
							+ offset, this);
					g.setTransform(savedTx);
				}
				offset += height;
			}
			if (!drewSelected) {
				// We lost the reference to the selected result string; it's gone, so we should
				// deselect it
				selectedIdx = -1;
			}
		} catch (ConcurrentModificationException e) {
			repaint();
		}
	}
	
	public void selectExpression(ExpData ex) {
		if (selectedIdx == ex.exp.expNum) {
			oldIdx = selectedIdx;
			selectedIdx = -1;
			win.closePreviewPane();
		} else {
			win.setPreviewText(ex.output, new Runnable() {
				@Override
				public void run() {
					selectedIdx = -1;
					repaint();
				}
			});
			selectedIdx = ex.exp.expNum;
		}
		rotateStart = System.currentTimeMillis();
		repaint();
	}

	private void repaintAfter(int delay) {
		Timer timer = new Timer();
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				repaint();
			}
		}, delay);
	}
	
	public void updateWith(List<ExpData> d) {
		data = d;
		repaint();
	}
}
