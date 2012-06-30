package org.proofpad;
import java.util.LinkedList;
import java.util.List;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.undo.UndoManager;

import org.proofpad.ProofBar.UnprovenExp.Status;
import org.proofpad.SExpUtils.ExpType;

public class ProofBar extends JComponent {
	
	public interface ReadOnlyIndexChangeListener {
		void readOnlyIndexChanged(int newIndex);
	}
	
	public class ExpData {
		Expression exp;
		String output;
		public ExpData(Expression exp, String output) {
			this.exp = exp;
			this.output = output;
		}
		public int getHeight() {
			return pixelHeight(exp);
		}
		@Override
		public String toString() {
			return "<" + getHeight() + ", " + output + ">";
		}
	}
	ArrayList<ExpData> data = new ArrayList<ExpData>();

	static Color provedColor = new Color(.8f, 1f, .8f);
	static Color untriedColor = new Color(.9f, .9f, .9f);
	static Color provingColor = new Color(.5f, .8f, 1f);
	static Color errorColor = new Color(1f, .8f, .8f);
	public static final ImageIcon errorIcon = new ImageIcon(
			ProofBar.class.getResource("/media/error.png"));
	public static final ImageIcon successIcon = new ImageIcon(
			ProofBar.class.getResource("/media/check.png"));
	
	static LinearGradientPaint diagonalPaint(Color a, Color b, int step, float dist) {
		return new LinearGradientPaint(0, 0, step, step,
				new float[] {0f, dist, dist + .01f, 1f},
				new Color[] {a, a, b, b},
				MultipleGradientPaint.CycleMethod.REPEAT);
	}
	
	static Paint prove = diagonalPaint(provedColor, untriedColor, 8, .8f);
	static Paint unprove = diagonalPaint(provedColor, untriedColor, 8, .2f);

	private static final long serialVersionUID = 8267405348010307267L;
	
	List<Expression> expressions;
	List<Expression> proofQueue = new LinkedList<Expression>();
	int my;
	boolean hover = false;
	final static int width = 20;
	static int lineHeight;
	final Acl2 acl2;
	
	int numProved;
	int numProving;
	boolean error;
	List<Integer> admissionIndices = new ArrayList<Integer>();
	
	private int readOnlyIndex = -1;
	Expression tried;
	private Image inProgressThrobber = 
			new ImageIcon(getClass().getResource("/media/in-progress-blue.gif")).getImage();
	private int flashIndex;
	int flashPhase;
	Runnable flashTimeout;
	protected Thread flashThread;
	public int readOnlyHeight;
	UndoManager undoManager;
	private List<ReadOnlyIndexChangeListener> readOnlyIndexListeners =
			new LinkedList<ReadOnlyIndexChangeListener>();
	
	static class UnprovenExp {
		enum Status { SUCCESS, FAILURE, UNTRIED }
		public int hash;
		public Status status;
	}
	private List<UnprovenExp> unprovenStates = new ArrayList<UnprovenExp>();

	final MoreBar mb;

	public ProofBar(final Acl2 acl2, final MoreBar mb) {
		super();
		this.acl2 = acl2;
		this.mb = mb;
		mb.updateWith(data);
		acl2.addRestartListener(new Acl2.RestartListener() {
			@Override
			public void acl2Restarted() {
				numProved = 0;
				numProving = 0;
				admissionIndices.clear();
				repaint();
			}
		});
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
				int admissionIndexSoFar = acl2.numInitExps;
				int i = 0;
				for (Expression ex : expressions) {
					int height = pixelHeight(ex);
					height += addToNextHeight;
					addToNextHeight = 0;
					if (provedSoFar > 0) {
						// Going through expressions that have been proven. A
						// click here means we need to undo.
						int admissionIndex = admissionIndices.get(i);
						provedSoFar--;
						if (provedSoFar == 0) {
							addToNextHeight = ex.nextGapHeight * lineHeight / 2 - 1;
							height -= addToNextHeight;
						}
						if (e.getY() < begin + height) {
							for (; admissionIndexSoFar < admissionIndex; admissionIndexSoFar++) {
								acl2.undo();
							}
							data.set(ex.expNum, null);
							mb.repaint();
							numProved--;
							setReadOnlyIndex(Math.min(getReadOnlyIndex(),
									ex.prev == null ? -1 : ex.prev.nextIndex - 1));
							admissionIndices.remove(i);
							i--;
						}
						i++;
						if (admissionIndex > admissionIndexSoFar) {
							admissionIndexSoFar = admissionIndex;
						}
					} else {
						// A click here means we need to admit some new forms
						if (error) {
							break;
						}
						if (e.getY() > begin && ex.firstType != ExpType.FINAL) {
							proofQueue.add(ex);
							numProving++;
							setReadOnlyIndex(Math.max(getReadOnlyIndex(), ex.nextIndex - 1));
						}
					}
					begin += height;
				}
				toLogicMode();
				proveNext();
				error = false;
				repaint();
			}
			@Override
			public void mouseEntered(MouseEvent e) {
				hover = true;
				repaint();
			}
			@Override
			public void mouseExited(MouseEvent e) {
				hover = false;
				repaint();
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
						repaint();
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
		int unprovenIdx = 0;
		Status expStatus = Status.UNTRIED;
		for (Expression e: expressions) {
			int height = pixelHeight(e);
			height += addToNextHeight;
			addToNextHeight = 0;
			if (provedSoFar == 0 && unprovenStates.size() > unprovenIdx) {
				expStatus = unprovenStates.get(unprovenIdx).status;
				unprovenIdx++;
			}
			if (provedSoFar > 0) {
				// Drawing proved terms
				provedSoFar--;
				if (provedSoFar == 0) {
					addToNextHeight = e.nextGapHeight * lineHeight / 2 - 1;
					height -= addToNextHeight;
					setReadOnlyHeight(begin + height);
				}
				if (hover && my < begin + height) {
					g.setPaint(unprove);
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
					g.drawImage(successIcon.getImage(), (width - 19) / 2, (height - 19) / 2 + begin, this);
				}
			} else if ((isError && provedSoFar == 0) || expStatus == Status.FAILURE) {
				// Draw error box
				isError = false;
				g.setColor(errorColor);
				g.fillRect(0, begin, 30, height);
				if (hover && my > begin && my <= begin + height) {
					setToolTipText("An error occured. See the log below for details.");
				}
				g.drawImage(errorIcon.getImage(), (width - 19) / 2, (height - 19) / 2 + begin, this); 
			} else if (provingSoFar > 0) {
				// Drawing in-progress terms
				provingSoFar--;
				if (provingSoFar == 0) {
					setReadOnlyHeight(begin + height - e.nextGapHeight * lineHeight / 2);
				}
				g.setColor(provingColor);
				g.fillRect(0, begin, 30, height);
				g.drawImage(inProgressThrobber, (width - 16) / 2, begin + (height - 16) / 2, this);
			} else if (e.firstType != SExpUtils.ExpType.FINAL) {
				// Drawing untried terms
				if (hover && my > begin && !error && !e.contents.equals("")) {
					g.setColor(provedColor);
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
			}
			g.setColor(Color.GRAY);
			g.drawLine(0, begin, width, begin);
			begin += height;
		}
		setPreferredSize(new Dimension(width, begin + 50));
	}

	private void setReadOnlyHeight(int newHeight) {
		readOnlyHeight = newHeight;
	}

	static int pixelHeight(Expression ex) {
		if (ex.prev == null) {
			return (ex.lines + ex.prevGapHeight) * lineHeight + ex.nextGapHeight * lineHeight / 2;
		} else if (ex.firstType == ExpType.FINAL) {
			return (ex.lines + ex.nextGapHeight) * lineHeight + ex.prevGapHeight * lineHeight / 2;
		} else {
			return ex.lines * lineHeight + (ex.prevGapHeight + ex.nextGapHeight) * lineHeight / 2;
			
		}
	}
	
	public static int getLineHeight() {
		return lineHeight;
	}

	public void setLineHeight(int lineHeight) {
		ProofBar.lineHeight = lineHeight;
		this.repaint();
	}
	
	public void adjustHeights(java.util.LinkedList<Expression> newExps) {
		expressions = newExps;
		int i = 0;
		for (Expression ex : expressions) {
			ex.expNum = i;
			i++;
			if (i < data.size()) {
				data.get(i).exp = ex;
			}
		}
		clearProgramModeData();
		error = false;
		for (UnprovenExp e : unprovenStates) {
			e.status = Status.UNTRIED;
		}
		repaint();
	}

	private void clearProgramModeData() {
		if (admissionIndices.size() == 0) {
			data.clear();
		} else if (data.size() > 0) {
			data.subList(admissionIndices.size(), data.size()).clear();
		}
		mb.repaint();
	}
	
	/**
	 * Called by {@link proveNext} when the function is admitted.
	 * @param success
	 */
	void proofCallback(boolean success) {
		if (tried == null) return;
		if (success && numProving > 0) {
			numProved++;
			numProving--;
			if (numProving > 0) {
				proveNext();				
			} else {
				admitUnprovenExps();
			}
			repaint();
		} else if (!success) {
			numProving = 0;
			setReadOnlyIndex(tried.prev == null ? -1 : tried.prev.nextIndex - 1);
			proofQueue.clear();
			error = true;
			repaint();
		}
	}
	
	void toLogicMode() {
		// Undo everything since the last proven form
		int idx;
		if (admissionIndices.size() == 0) {
			idx = acl2.numInitExps + 1;
		} else {
			idx = admissionIndices.get(admissionIndices.size() - 1) + 1;
		}
		clearProgramModeData();
		acl2.admit(":ubt! " + idx + "\n", Acl2.doNothingCallback);
		// Enter logic mode
		acl2.admit(":logic\n", Acl2.doNothingCallback);
	}

	/**
	 * Prove the next item in the proof queue, if there is one.
	 */
	void proveNext() {
		if (proofQueue.size() == 0) {
			numProving = 0;
			admitUnprovenExps();
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
			@Override
			public boolean run(final boolean outerSuccess, String response) {
				setExpData(tried, response);
				if (!outerSuccess) {
					proofCallback(outerSuccess);
					return false;
				}
				// Get the index
				acl2.admit(":pbt :here", new Acl2.Callback() {
					@Override
					public boolean run(boolean s, String r) {
						int idx = -1;
						try {
							idx = Integer.parseInt(r.substring(4, r.length()).split(":")[0].trim());
						} catch (NumberFormatException e) { }
						if (outerSuccess) {
							admissionIndices.add(admissionIndices.size(), idx);
						}
						proofCallback(outerSuccess);
						return false;
					}
				});
				return false;
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
				toLogicMode();
				proveNext();
				break;
			}
		}
		repaint();
	}
	
	public void undoPrevForm() {
		int ignore = numProved + numProving - 1;
		int i = 0;
		numProved--;
		int admissionIndexSoFar = 0;
		int admissionIndex = 0;
		for (Expression ex : expressions) {
			admissionIndex = admissionIndices.get(i);
			if (ignore > 0) {
				ignore--;
			} else {
				for (; admissionIndexSoFar < admissionIndex; admissionIndexSoFar++) {
					acl2.undo();
				}
				setReadOnlyIndex(Math.min(getReadOnlyIndex(),
						ex.prev == null ? -1 : ex.prev.nextIndex - 1));
				admissionIndices.remove(i);
				break;
			}
			if (admissionIndex > admissionIndexSoFar) {
				admissionIndexSoFar = admissionIndex;
			}
			i++;
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
			@Override
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


	public void admitUnprovenExps() {
		if (numProving > 0) return;
		int provedSoFar = numProved;
		acl2.admit(":program", Acl2.doNothingCallback);
		acl2.admit("(set-ld-redefinition-action '(:doit . :erase) state)", Acl2.doNothingCallback);
		unprovenStates = new ArrayList<UnprovenExp>();
		int unprovenIdx = 0;
		for (final Expression ex : expressions) {
			if (provedSoFar > 0) {
				provedSoFar--;
			} else {
				final UnprovenExp ue;
				if (unprovenIdx < unprovenStates.size()) {
					ue = unprovenStates.get(unprovenIdx);
					unprovenIdx++;
					if (ue.hash == ex.contents.hashCode()) {
						continue;
					}
				} else {
					unprovenIdx++;
					ue = new UnprovenExp();
					unprovenStates.add(ue);
				}
				ue.status = Status.UNTRIED;
				ue.hash = ex.contents.hashCode();
				acl2.admit(ex.contents, new Acl2.Callback() {
					@Override
					public boolean run(boolean success, String response) {
						setExpData(ex, response);
						repaint();
						ue.status = success ? Status.SUCCESS : Status.FAILURE;
						return false;
					}
				});
			}
		}
	}

	void setExpData(Expression exp, String response) {
		while (data.size() <= exp.expNum) {
			data.add(null);
		}
		data.subList(exp.expNum, data.size() - 1).clear();
		data.set(exp.expNum, new ExpData(exp, response));
		mb.repaint();
	}

}
