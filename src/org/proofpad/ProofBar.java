package org.proofpad;
import java.util.List;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.UndoManager;
import org.fife.ui.rsyntaxtextarea.Token;

public class ProofBar extends JComponent {
	
	public interface ReadOnlyIndexChangeListener {

		void readOnlyIndexChanged(int newIndex);

	}

	static Color provedColor = new Color(.8f, 1f, .8f);
	static Color untriedColor = new Color(.82f, .8f, .8f);
	static Color provingColor = new Color(.5f, .8f, 1f);
	static Color errorColor = new Color(1f, .8f, .8f);
	public static final ImageIcon errorIcon = new ImageIcon(ProofBar.class.getResource("/media/error.png"));
	public static final ImageIcon successIcon = new ImageIcon(ProofBar.class.getResource("/media/check.png"));
	static LinearGradientPaint prove = new LinearGradientPaint(0, 0, 8, 10,
			new float[] {0f, .8f, .81f, 1f},
			new Color[] {provedColor, provedColor, untriedColor, untriedColor},
			MultipleGradientPaint.CycleMethod.REPEAT);
	static LinearGradientPaint unprove = new LinearGradientPaint(0, 0, 8, 10,
			new float[] {0f, .2f, .21f, 1f},
			new Color[] {provedColor, provedColor, untriedColor, untriedColor},
			MultipleGradientPaint.CycleMethod.REPEAT);

	private static final long serialVersionUID = 8267405348010307267L;
	
	private java.util.List<Expression> expressions;
	private java.util.List<Expression> proofQueue;
	private int my;
	private boolean hover = false;
	final private static int width = 25;
	private static int lineHeight;
	private final Acl2 acl2;
	
	private int numProved;
	private int numProving;
	private boolean error;
	
	private int readOnlyIndex = -1;
	private Expression tried;
	private Image inProgressThrobber = 
			new ImageIcon(getClass().getResource("/media/in-progress-blue.gif")).getImage();;
	private int flashIndex;
	private int flashPhase;
	private Runnable flashTimeout;
	protected Thread flashThread;
	public int readOnlyHeight;
	UndoManager undoManager;
	private List<ReadOnlyIndexChangeListener> readOnlyIndexListeners =
			new LinkedList<ReadOnlyIndexChangeListener>();

	public ProofBar(Acl2 acl2) {
		super();
		this.acl2 = acl2;
		acl2.addRestartListener(new Acl2.RestartListener() {
			public void acl2Restarted() {
				numProved = 0;
				numProving = 0;
				repaint();
			}
		});
		final ProofBar that = this;
		proofQueue = new LinkedList<Expression>();
		setPreferredSize(new Dimension(width, 0));
		setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Color.LIGHT_GRAY));
		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (numProving > 0) {
					return;
				}
				int begin = 0;
				int provedSoFar = numProved;
				int addToNextHeight = 0;
				for (Expression ex : expressions) {
					//System.out.println(ex.first);
					int height = pixelHeight(ex);
					height += addToNextHeight;
					addToNextHeight = 0;
					if (provedSoFar > 0) {
						provedSoFar--;
						if (provedSoFar == 0) {
							addToNextHeight = ex.nextGapHeight * lineHeight / 2 - 1;
							height -= addToNextHeight;
						}
						if (e.getY() < begin + height) {
							if (ex.firstType == Token.RESERVED_WORD_2) {
								that.acl2.admit(":u\n", null);
							}
							numProved--;
							setReadOnlyIndex(Math.min(getReadOnlyIndex(),
									ex.prev == null ? -1 : ex.prev.nextIndex - 1));
						}
					} else {
						if (error) {
							break;
						}
						if (e.getY() > begin) {
							proofQueue.add(ex);
							numProving++;
							setReadOnlyIndex(Math.max(getReadOnlyIndex(), ex.nextIndex - 1));
						}
					}
					begin += height;
				}
				that.proveNext();
				error = false;
				that.repaint();
			}
			public void mouseEntered(MouseEvent e) {
				hover = true;
				that.repaint();
			}
			public void mouseExited(MouseEvent e) {
				hover = false;
				that.repaint();
			}
		});
		addMouseMotionListener(new MouseMotionListener() {
			@Override
			public void mouseDragged(MouseEvent e) {
				mouseMoved(e);
			}
			@Override
			public void mouseMoved(MouseEvent e) {
				int begin = 0;
				for (Expression ex : expressions) {
					int height = pixelHeight(ex);
					if (e.getY() < begin && my >= begin ||
							e.getY() > begin + height && my <= begin + height) {
						my = e.getY();
						that.repaint();
					}
					begin += height;
				}
				my = e.getY();
			}
			
		});
		expressions = new Vector<Expression>();
		numProved = 0;
	}
	

	@Override
	protected void paintComponent(Graphics gOld) {
		Graphics2D g = (Graphics2D) gOld;
		int begin = 0;
		int provedSoFar = numProved;
		int provingSoFar = numProving;
		int addToNextHeight = 0;
		boolean isError = error;
		int flashStartIndex = 0;
		if (numProving == 0 && numProved == 0) {
			setReadOnlyHeight(0);
		}
		for (Expression e: expressions) {
			int height = pixelHeight(e);
			height += addToNextHeight;
			addToNextHeight = 0;
			if (isError && provedSoFar == 0) {
				// Draw error box
				isError = false;
				g.setColor(errorColor);
				g.fillRect(0, begin, 30, height);
				if (hover && my > begin && my <= begin + height) {
					setToolTipText("An error occured. See the log below for details.");
				}
				//drawStringCentered(g, begin, height, "\u2715");
				g.drawImage(errorIcon.getImage(), (width - 19) / 2, (height - 19) / 2 + begin, this); 
			} else if (provedSoFar == 0 && provingSoFar == 0) {
				// Drawing untried terms
				if (hover && my > begin && !error && !e.contents.equals("")) {
					//g.setColor(provedColor.brighter());
					g.setPaint(prove);
					g.fillRect(0, begin, 30, height);
					if (my <= begin + height) {
						g.setColor(untriedColor);
						int yEnd = begin + height;
						g.fillPolygon(new int[] {0, width / 2, width, width, 0, 0},
								new int[] {yEnd - 10, yEnd, yEnd - 10, yEnd, yEnd, yEnd - 10},
								6);
						setToolTipText("Admit this term.");
					}
				} else {
					g.setColor(untriedColor);
					g.fillRect(0, begin, 30, height);
				}
			} else if (provedSoFar > 0) {
				// Drawing proved terms
				provedSoFar--;
				if (provedSoFar == 0) {
					addToNextHeight = e.nextGapHeight * lineHeight / 2 - 1;
					height -= addToNextHeight;
				}
				if (provedSoFar == 0) {
					setReadOnlyHeight(begin + height);
				}
				if (hover && my < begin + height) {
					//g.setColor(untriedColor.brighter());
					g.setPaint(unprove);
					//g.setPaint(new GradientPaint(0, 0, new Color(.7f, .7f, .7f), width, 0, new Color(.8f, .8f, .8f)));
					g.fillRect(0, begin, 30, height);
					if (my >= begin) {
						g.setColor(provedColor);
						g.fillPolygon(new int[] {0, width / 2, width, width, 0, 0},
								new int[] {begin + 10, begin, begin + 10, begin, begin, begin + 10},
						        6);
						setToolTipText("Undo admitting this term.");
					}
				} else {
					g.setColor(provedColor);
					int flashEndIndex = e.nextIndex + e.nextGapHeight / 2;
					if (flashPhase % 2 == 1 && flashIndex <= flashEndIndex && flashIndex > flashStartIndex) {
						g.setColor(provedColor.darker());
					}
					flashStartIndex = flashEndIndex;
					g.fillRect(0, begin, 30, height);
					//drawStringCentered(g, begin, height, "\u2713");
					g.drawImage(successIcon.getImage(), (width - 19) / 2, (height - 19) / 2 + begin, this);
				}
			} else {
				// Drawing in-progress terms
				provingSoFar--;
				if (provingSoFar == 0) {
					setReadOnlyHeight(begin + height - e.nextGapHeight * lineHeight / 2);
				}
				g.setColor(provingColor);
				g.fillRect(0, begin, 30, height);
				g.drawImage(inProgressThrobber, (width - 16) / 2, begin + (height - 16) / 2, this);
			}
			g.setColor(Color.GRAY);
			g.drawLine(0, begin, width, begin);
			begin += height;
		}
		setPreferredSize(new Dimension(width, begin));
	}

	private void setReadOnlyHeight(int newHeight) {
		readOnlyHeight = newHeight;
	}

	private static int pixelHeight(Expression ex) {
		if (ex.prev == null) {
			return (ex.height + ex.prevGapHeight) * lineHeight + ex.nextGapHeight * lineHeight / 2;
		} else if (ex.firstType == -1) {
			return (ex.height + ex.nextGapHeight) * lineHeight + ex.prevGapHeight * lineHeight / 2;
		} else {
			return ex.height * lineHeight + (ex.prevGapHeight + ex.nextGapHeight) * lineHeight / 2;
			
		}
	}
	
	public int getLineHeight() {
		return lineHeight;
	}

	public void setLineHeight(int lineHeight) {
		ProofBar.lineHeight = lineHeight;
		this.repaint();
	}
	
	//@SuppressWarnings("unchecked")
	public void adjustHeights(java.util.LinkedList<Expression> expressions) {
		/*
		Object o = expressions.clone();
		if (o instanceof List<?>) {
			this.expressions = (List<Expression>) o;
		}
		*/
		this.expressions = expressions;
		error = false;
		repaint();
	}
	
	private void proofCallback(boolean success) {
		if (tried == null) return;
		if (success && numProving > 0) {
			numProved++;
			numProving--;
			if (numProving > 0) {
				proveNext();				
			}
			repaint();
			undoManager.addEdit(new AbstractUndoableEdit() {
				private static final long serialVersionUID = -8089563163417075830L;
				@Override
				public void undo() {
					int provedSoFar = numProved;
					for (Expression ex : expressions) {
						provedSoFar--;
						if (provedSoFar > 1) continue;
						if (ex.firstType == Token.RESERVED_WORD_2) {
							acl2.admit(":u\n", null);
						}
						setReadOnlyIndex(Math.min(getReadOnlyIndex(), ex.prev == null ? -1 : ex.prev.nextIndex));
						numProved--;
						repaint();
						break;
					}
				}
				@Override
				public void redo() {
					int ignore = numProved + numProving;
					for (Expression ex : expressions) {
						ignore--;
						if (ignore > 0) continue;
						acl2.admit(ex.contents, new Acl2.Callback() {
							@Override
							public boolean run(boolean success,
									String response) {
								numProving--;
								if (success) {
									numProved++;
								} else {
									error = true;
									setReadOnlyIndex(tried == null || tried.prev == null ?
											-1 : tried.prev.nextIndex - 1);
									proofQueue.clear();
								}
								repaint();
								return true;
							}
							
						});
						numProving++;
						return;
					}
				}
				@Override
				public String getPresentationName() {
					return "Admission";
				}
			});
		} else if (!success) {
			numProving = 0;
			setReadOnlyIndex(tried.prev == null ? -1 : tried.prev.nextIndex - 1);
			proofQueue.clear();
			error = true;
			repaint();
		}
	}

	private void proveNext() {
		if (proofQueue.size() == 0) {
			numProving = 0;
			repaint();
			return;
		}
		tried = proofQueue.remove(0);
		if (tried.contents.equals("")) {
			tried = null;
			numProving = 0;
			repaint();
			return;
		}
		acl2.admit(tried.contents, new Acl2.Callback() {
			public boolean run(boolean success, String response) {
				proofCallback(success);
				return true;
			}
		});
	}
	
	public void admitNextForm() {
		int ignore = numProved + numProving;
		for (Expression ex : expressions) {
			if (ignore > 0) {
				ignore--;
			} else {
				numProving++;
				setReadOnlyIndex(Math.max(getReadOnlyIndex(), ex.nextIndex - 1));
				proofQueue.add(ex);
				proveNext();
				break;
			}
		}
		repaint();
	}
	
	public void resetProgress() {
		numProved = 0;
		numProving = 0;
	}

	public void flashAt(int offs) {
		if (flashThread != null) {
			flashThread.interrupt();
		}
		flashIndex = offs;
		flashPhase = 1;
		flashTimeout = new Runnable() {
			public void run() {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) { return; }
				flashPhase++;
				repaint();
				if (flashPhase <= 5 && !Thread.interrupted()) {
					flashThread = new Thread(flashTimeout);
					flashThread.start();
				} else {
					flashPhase = 6;
				}
			}
		};
		flashThread = new Thread(flashTimeout);
		flashThread.start();
	}

	public int getReadOnlyIndex() {
		return readOnlyIndex;
	}

	public void setReadOnlyIndex(int readOnlyIndex) {
		repaint();
		if (this.readOnlyIndex != readOnlyIndex) {
			fireReadOnlyIndexChange(readOnlyIndex);
		}
		this.readOnlyIndex = readOnlyIndex;
	}

	private void fireReadOnlyIndexChange(int newIndex) {
		for (ReadOnlyIndexChangeListener roil : readOnlyIndexListeners) {
			roil.readOnlyIndexChanged(newIndex);
		}
	}
	
	public void addReadOnlyIndexChangeListener(ReadOnlyIndexChangeListener roil) {
		readOnlyIndexListeners.add(roil);
	}
}
