package org.proofpad;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JScrollBar;

import org.proofpad.ProofBar.ExpData;

public class MoreBar extends JScrollBar {
	
	private static final ImageIcon moreIcon = Repl.moreIcon;
	private static final long serialVersionUID = -2510084974061378819L;
	private static final int width = 20;
	int selectedIdx = -1;
	int oldIdx = -1;
	long rotateStart;

	List<ExpData> data;
	private final IdeWindow win;
	
	public MoreBar(final IdeWindow win) {
		super();
		this.win = win;
		int scrollWidth = new JScrollBar().getPreferredSize().width;
		setPreferredSize(new Dimension(width + scrollWidth, 0));
		setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, Color.LIGHT_GRAY));
		MouseListener[] mls = getMouseListeners();
		// Shift mouse listeners over 10px so they match the scrollbar's expectations
		for (final MouseListener ml : mls) {
			removeMouseListener(ml);
			addMouseListener(new MouseListener() {
				@Override public void mouseReleased(MouseEvent e) {
					ml.mouseReleased(shiftMouseEvent(e));
				}
				@Override public void mousePressed(MouseEvent e) {
					ml.mousePressed(shiftMouseEvent(e));
				}
				@Override public void mouseExited(MouseEvent e) {
					ml.mouseExited(shiftMouseEvent(e));
				}
				@Override public void mouseEntered(MouseEvent e) {
					ml.mouseEntered(shiftMouseEvent(e));					
				}
				@Override public void mouseClicked(MouseEvent e) {
					ml.mouseClicked(shiftMouseEvent(e));
				}
			});
		}
		for (final MouseMotionListener mml : getMouseMotionListeners()) {
			removeMouseMotionListener(mml);
			addMouseMotionListener(new MouseMotionListener() {
				@Override public void mouseMoved(MouseEvent e) {
					mml.mouseMoved(shiftMouseEvent(e));
				}
				@Override public void mouseDragged(MouseEvent e) {
					mml.mouseDragged(shiftMouseEvent(e));
				}
			});
		}
		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getX() >= width) return;
				int top = getValue();
				int offset = -top;
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
		// Draw the old scrollbar
		Rectangle clip = g.getClipBounds();
		clip.width -= width;
		clip.x += width;
		g.setClip(clip);
		g.translate(width / 2, 0); // TODO: Why is this /2?
		super.paintComponent(g);
		g.translate(-width / 2, 0);
		clip.x -= width;
		clip.width += width;
		g.setClip(clip);
		// Draw the More Bar.
		int top = getValue();
		g.setColor(Color.GRAY);
		g.drawLine(width, 0, width, getHeight());
		int offset = -top;
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
					g.setColor(ProofBar.UNTRIED_COLOR);
					g.fillRect(0, offset, width, height);
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
				g.drawLine(0, offset + height, width, offset + height);
				if (ex.output.length() > 0) {
					AffineTransform savedTx = g.getTransform();
					g.rotate(angle, width / 2, offset + height / 2);
					g.drawImage(moreIcon.getImage(), (width - 19) / 2, (height - 19) / 2
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
			win.outputWindow.hideWindow();
		} else {
			win.outputWindow.showWithText(ex.output, new Runnable() {
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
	
	static MouseEvent shiftMouseEvent(MouseEvent e) {
		MouseEvent ret = new MouseEvent((Component) e.getSource(), e.getID(), e.getWhen(),
				e.getModifiers(), e.getX() - width / 2, e.getY(), e.getXOnScreen() - width / 2,
				e.getYOnScreen(), e.getClickCount(), e.isPopupTrigger(), e.getButton());
		return ret;
	}
}
