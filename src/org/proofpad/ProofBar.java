package org.proofpad;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.LinearGradientPaint;
import java.awt.MultipleGradientPaint;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.Timer;
import javax.swing.undo.UndoManager;

import org.proofpad.ProofBar.UnprovenExp.Status;
import org.proofpad.Repl.MsgType;
import org.proofpad.SExpUtils.ExpType;

public class ProofBar extends JComponent {
	
	final static Cursor HAND = new Cursor(Cursor.HAND_CURSOR);
	final static int WIDTH = 20;

	private final class PBMouseListener extends MouseAdapter {
		private final Acl2 acl2;
		private final MoreBar mb;

		PBMouseListener(Acl2 acl2, MoreBar mb) {
			this.acl2 = acl2;
			this.mb = mb;
		}

		@Override public void mouseClicked(MouseEvent e) {
			if (e.getButton() != MouseEvent.BUTTON1) {
				return;
			}
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
			generateSegments();
		}

		@Override public void mouseEntered(MouseEvent e) {
			hover = true;
			repaint();
		}

		@Override public void mouseExited(MouseEvent e) {
			hover = false;
			repaint();
		}
	}

	public interface ReadOnlyIndexChangeListener {
		void readOnlyIndexChanged(int newIndex);
	}
	
	public class PBSegment {
		public Color fill = Color.WHITE;
		public boolean proved;
		public int begin;
		public int height;
		public Image icon;
		public String tooltip = "";
	}
	
	private final ArrayList<PBSegment> segments = new ArrayList<PBSegment>();
	
	public class ExpData {
		Expression exp;
		String output;
		MsgType type;
		public ExpData(Expression exp, String output, MsgType type) {
			this.exp = exp;
			this.output = output;
			this.type = type;
		}
		public int getHeight() {
			return pixelHeight(exp);
		}
		@Override public String toString() {
			return "<" + getHeight() + ", " + output.replace('\n', ' ').substring(0, Math.min(10, output.length())) + ", " + type + ">";
		}
	}
	ArrayList<ExpData> data = new ArrayList<ExpData>();

	static final Color PROVED_COLOR = new Color(0x5B9653);
	static final Color UNTRIED_COLOR = new Color (0xDDDDDD);
	static final Color IN_PROGRESS_COLOR = new Color(0xA3D6BC);
	static final Color ERROR_COLOR = new Color(0xFFAAAA);
	static final Color WARNING_COLOR = new Color(0xFFFF00);
	static final Color ADMITTED_COLOR = new Color(0xDDF8CC);
	public static final ImageIcon errorIcon = new ImageIcon(
			ProofBar.class.getResource("/Icons/Error.png"));
	public static final ImageIcon successIcon = new ImageIcon(
			ProofBar.class.getResource("/Icons/Check.png"));
	public static final Icon warningIcon = new ImageIcon(
			ProofBar.class.getResource("/Icons/Error.png")); // FIXME

	static LinearGradientPaint diagonalPaint(Color a, Color b, int step, float dist) {
		return new LinearGradientPaint(0, 0, step, step,
				new float[] {0f, dist, dist + .01f, 1f},
				new Color[] {a, a, b, b},
				MultipleGradientPaint.CycleMethod.REPEAT);
	}
	
	private static final long serialVersionUID = 8267405348010307267L;
	
	List<Expression> expressions;
	List<Expression> proofQueue = new LinkedList<Expression>();
	int my;
	boolean hover = false;
	
	int lineHeight;
	final Acl2 acl2;
	
	int numProved;
	int numProving;
	boolean error;
	List<Integer> admissionIndices = new ArrayList<Integer>();
	
	private int readOnlyIndex = -1;
	Expression tried;
	private final Image inProgressThrobber = 
			new ImageIcon(getClass().getResource("/Icons/in-progress-blue.gif")).getImage();
	private final Image admittingThrobber = 
			new ImageIcon(getClass().getResource("/Icons/admitting.gif")).getImage();
	public int readOnlyHeight;
	UndoManager undoManager;
	private final List<ReadOnlyIndexChangeListener> readOnlyIndexListeners =
			new LinkedList<ReadOnlyIndexChangeListener>();
	
	static class UnprovenExp {
		enum Status { SUCCESS, FAILURE, UNTRIED }
		public Status status;
	}
	private List<UnprovenExp> unprovenStates = new ArrayList<UnprovenExp>();
	final MoreBar mb;
	boolean alreadyShownAnError = false;
	boolean isAdmitting = false;
	private final Acl2Parser parser;
	private final PPWindow window;

	public ProofBar(final Acl2 acl2, final MoreBar mb, final Acl2Parser parser, final PPWindow window) {
		this.acl2 = acl2;
		this.mb = mb;
		this.parser = parser;
		this.window = window;
		mb.updateWith(data);
		setCursor(HAND);
		acl2.addRestartListener(new Acl2.RestartListener() {
			@Override public void acl2Restarted() {
				numProved = 0;
				numProving = 0;
				admissionIndices.clear();
				generateSegments();
			}
		});
		setPreferredSize(new Dimension(WIDTH, 0));
		setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Color.GRAY));
		addMouseListener(new PBMouseListener(acl2, mb));
		addMouseMotionListener(new MouseMotionListener() {
			@Override public void mouseDragged(MouseEvent e) {
				mouseMoved(e);
			}
			@Override public void mouseMoved(MouseEvent e) {
				int begin = 0;
				for (Expression ex : expressions) {
					int height = pixelHeight(ex);
					if (e.getY() >= begin && e.getY() < begin + height) {
						if (my <= begin || my >= begin + height) {
							repaint();
						}
						my = e.getY();
						setCursor(HAND);
						return;
					}
					begin += height;
				}
				my = e.getY();
				setCursor(null);
			}
			
		});
		expressions = new Vector<Expression>();
		numProved = 0;
	}

	@Override protected void paintComponent(Graphics gOld) {
		Graphics2D g = (Graphics2D) gOld;
		// TODO: Draw only what's in clipBounds to scroll faster.
		Rectangle clipBounds = g.getClipBounds();
		int clipYMax = clipBounds.y + clipBounds.height + 10;
		setToolTipText("");
		boolean calloutBelowShown = false;
		boolean calloutAboveShown = false;
		int roundDim = 5;
		g.setColor(Color.GRAY);
		g.draw(clipBounds);
		try {
			//for (PBSegment seg : segments) {
			for (int i = segments.size() - 1; i >= 0; i--) {
				PBSegment seg = segments.get(i);
				if (seg.fill.equals(Color.WHITE)) {
					continue;
				}
				int begin = seg.begin;
				int height = seg.height;
				g.setColor(seg.fill);
				RoundRectangle2D.Double rect = new RoundRectangle2D.Double(0, begin - 10, 19,
						height + 10, roundDim, roundDim);
				g.fill(rect);
				g.setColor(Color.GRAY);
				g.draw(rect);
				if (seg.icon != null) {
					g.drawImage(seg.icon, (WIDTH - seg.icon.getWidth(this)) / 2,
							(height - seg.icon.getHeight(this)) / 2 + begin, this);
				}
				int end = begin + height;
				if (my >= begin && my < end) {
					setToolTipText(seg.tooltip);
				}
				if (seg.fill == ERROR_COLOR) {
					if (begin > clipBounds.y + clipBounds.height) {
						window.showErrorCallout(true, new Rectangle(0, begin, 0, height));
						calloutBelowShown = true;
					} else if (end < clipBounds.y) {
						window.showErrorCallout(false, new Rectangle(0, begin, 0, height));
						calloutAboveShown = true;
					}
				}
				if (hover && numProving == 0) {
					if (seg.proved && my <= end) {
						g.setColor(UNTRIED_COLOR);
						rect = new RoundRectangle2D.Double(0, begin - 10 + (my > begin ? 20 : 0), 19,
								height + 10 - (my > begin ? 20 : 0), roundDim, roundDim);
						g.fill(rect);
						g.setColor(Color.GRAY);
						g.draw(rect);
						g.setColor(UNTRIED_COLOR);
						setToolTipText("Undo admitting this term.");
						if (my > begin) {
							g.fillPolygon(new int[] {1, 1, WIDTH / 2, WIDTH - 1, WIDTH - 1},
										  new int[] {begin + 20, begin + 9, begin, begin + 9, begin + 20},
										  5);
						}
					}
					if (!seg.proved && my >= begin) {
						g.setColor(PROVED_COLOR);
						rect = new RoundRectangle2D.Double(0, begin - 10, 19, height + 10 -
								(my < end ? 10 : 0), roundDim, roundDim);
						g.fill(rect);
						g.setColor(Color.GRAY);
						g.draw(rect);
						g.setColor(PROVED_COLOR);
						if (my < end) {
							g.fillPolygon(new int[] {1, 1, WIDTH / 2, WIDTH - 1, WIDTH - 1},
									  new int[] {end - 20, end - 9, end, end - 9, end - 20},
									  5);
							setToolTipText("Admit this term.");
						}
					}
				}
//				g.setColor(Color.GRAY);
//				g.drawLine(0, begin, width, begin);
				if (!calloutAboveShown) {
					window.hideErrorCallout(false);
				}
				if (!calloutBelowShown) {
					window.hideErrorCallout(true);
				}
				setPreferredSize(new Dimension(clipBounds.x + clipBounds.width, clipYMax));
			}
		} catch (IndexOutOfBoundsException e) {
			repaint();
		}
	}

	void generateSegments() {
		segments.clear();
		// TODO: Draw warnings
		int begin = 0;
		int provedSoFar = numProved;
		int provingSoFar = numProving;
		int addToNextHeight = 0;
		boolean isError = error;
		if (numProving == 0 && numProved == 0) {
			setReadOnlyHeight(0);
		}
		int unprovenIdx = 0;
		Status expStatus = Status.UNTRIED;
		for (Expression e: expressions) {
			PBSegment seg = new PBSegment();
			seg.height = pixelHeight(e);
			seg.height += addToNextHeight;
			addToNextHeight = 0;
			seg.begin = begin;
			if (provedSoFar == 0 && unprovenStates.size() > unprovenIdx) {
				expStatus = unprovenStates.get(unprovenIdx).status;
				unprovenIdx++;
			}
			if (provedSoFar > 0) {
				seg.proved = true;
				provedSoFar--;
				if (provedSoFar == 0) {
					addToNextHeight = e.nextGapHeight * lineHeight / 2 - 1;
					seg.height -= addToNextHeight;
					setReadOnlyHeight(begin + seg.height);
				}
				seg.fill = PROVED_COLOR;
				seg.icon = successIcon.getImage();
			} else if ((isError && provedSoFar == 0) || expStatus == Status.FAILURE) {
				unprovenIdx++;
				isError = false;
				seg.proved = false;
				seg.fill = ERROR_COLOR;
				seg.icon = errorIcon.getImage();
				seg.tooltip = "An error occured. See the log below for details.";
			} else if (provingSoFar > 0) {
				seg.proved = true;
				provingSoFar--;
				if (provingSoFar == 0) {
					setReadOnlyHeight(begin + seg.height - e.nextGapHeight * lineHeight / 2);
				}
				seg.fill = IN_PROGRESS_COLOR;
				seg.icon = inProgressThrobber;
			} else if (e.firstType != SExpUtils.ExpType.FINAL) {
				// Drawing untried or admitted terms
				seg.proved = false;
				Color expColor = expStatus == Status.SUCCESS ? ADMITTED_COLOR : UNTRIED_COLOR;
				seg.fill = expColor;
				if (expStatus == Status.UNTRIED && isAdmitting) {
					seg.icon = admittingThrobber;
				}
			}
			begin += seg.height;
			if (e.contents.isEmpty()) {
				seg.fill = Color.WHITE;
				seg.icon = null;
			}
			segments.add(seg);
		}
		repaint();
	}

	private void setReadOnlyHeight(int newHeight) {
		readOnlyHeight = newHeight;
	}

	int pixelHeight(Expression ex) {
		if (ex.prev == null) {
			return (ex.lines + ex.prevGapHeight) * lineHeight + ex.nextGapHeight * lineHeight / 2;
		} else if (ex.firstType == ExpType.FINAL) {
			return (ex.lines + ex.nextGapHeight) * lineHeight + ex.prevGapHeight * lineHeight / 2;
		} else {
			return ex.lines * lineHeight + (ex.prevGapHeight + ex.nextGapHeight) * lineHeight / 2;
		}
	}
	
	public int getLineHeight() {
		return lineHeight;
	}

	public void setLineHeight(int lineHeight) {
		this.lineHeight = lineHeight;
		generateSegments();
	}
	
	public void adjustHeights(java.util.LinkedList<Expression> newExps) {
		expressions = newExps;
		if (!Main.WIN && isAdmitting) {
			acl2.ctrlc();
			isAdmitting = false;
		}
		int i = 0;
		for (Expression ex : expressions) {
			ex.expNum = i;
			if (i < data.size() && data.get(i) != null) {
				data.get(i).exp = ex;
			}
			i++;
		}
		clearProgramModeData();
		error = false;
		for (UnprovenExp e : unprovenStates) {
			e.status = Status.UNTRIED;
		}
		generateSegments();
	}

	private void clearProgramModeData() {
		if (admissionIndices.size() == 0) {
			data.clear();
		} else if (data.size() > 0) {
			int start = admissionIndices.size();
			int end = data.size();
			if (end >= start) {
				data.subList(start, end).clear();
			}
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
			generateSegments();
		} else if (!success) {
			numProving = 0;
			setReadOnlyIndex(tried.prev == null ? -1 : tried.prev.nextIndex - 1);
			proofQueue.clear();
			error = true;
			generateSegments();
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
		acl2.admit("(set-ld-redefinition-action nil state)", Acl2.doNothingCallback);
	}

	/**
	 * Prove the next item in the proof queue, if there is one.
	 */
	void proveNext() {
		if (proofQueue.size() == 0) {
			numProving = 0;
			admitUnprovenExps();
			generateSegments();
			return;
		}
		tried = proofQueue.remove(0);
		if (tried.contents.equals("")) {
			tried = null;
			numProving = 0;
			generateSegments();
			return;
		}
		final boolean last = proofQueue.isEmpty();
		acl2.admit(tried.contents, new Acl2.Callback() {
			@Override public boolean run(final boolean outerSuccess, String response) {
				setExpData(tried, outerSuccess, response, last);
				if (!outerSuccess) {
					proofCallback(outerSuccess);
					return false;
				}
				// Get the index
				acl2.admit(":pbt :here", new Acl2.Callback() {
					@Override public boolean run(boolean s, String r) {
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
		generateSegments();
	}
	
	public void undoOneItem() {
		if (numProving != 0) return;
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
		generateSegments();
	}

	public void resetProgress() {
		numProved = 0;
		numProving = 0;
		unprovenStates = new ArrayList<UnprovenExp>();
		Timer t = new Timer(1000, new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) {
				admitUnprovenExps();
			}
		});
		t.setRepeats(false);
		t.start();
	}

	public int getReadOnlyIndex() {
		return readOnlyIndex;
	}

	public void setReadOnlyIndex(int readOnlyIndex) {

		generateSegments();
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
		isAdmitting = true;
		generateSegments();
		acl2.admit(":program", Acl2.doNothingCallback);
		acl2.admit("(set-ld-redefinition-action '(:doit . :erase) state)", Acl2.doNothingCallback);
		if (unprovenStates == null) unprovenStates = new ArrayList<UnprovenExp>();
		int unprovenIdx = 0;
		for (int i = 0; i < expressions.size(); i++) {
			final Expression ex = expressions.get(i);
			final boolean lastExp = i == expressions.size() - 2;
			if (provedSoFar > 0) {
				provedSoFar--;
				if (lastExp) {
					alreadyShownAnError = false;
				}
			} else {
				final UnprovenExp ue;				
				if (unprovenIdx < unprovenStates.size()) {
					ue = unprovenStates.get(unprovenIdx);
					unprovenIdx++;
				} else {
					unprovenIdx++;
					ue = new UnprovenExp();
					unprovenStates.add(ue);
				}
				ue.status = Status.UNTRIED;
				acl2.admit(ex.contents, new Acl2.Callback() {
					@Override public boolean run(boolean success, String response) {
						setExpData(ex, success, response, lastExp);
						if (lastExp) {
							alreadyShownAnError = false;
							isAdmitting = false;
						}
						ue.status = success ? Status.SUCCESS : Status.FAILURE;
						generateSegments();
						return false;
					}
				});
			}
		}
	}

	void setExpData(Expression exp, boolean success, String response, boolean last) {
		while (data.size() <= exp.expNum) {
			data.add(null);
		}
		try {
			data.subList(exp.expNum, data.size() - 1).clear();
		} catch (IndexOutOfBoundsException e) {
			return;
		} catch (ConcurrentModificationException e) {
			setExpData(exp, success, response, last);
		}
		ExpData expData = new ExpData(exp, response, success ? MsgType.SUCCESS : MsgType.ERROR);
		while (data.size() <= exp.expNum) {
			data.add(null);
		}
		data.set(exp.expNum, expData);
		mb.repaint();
		if (!success && Prefs.showOutputOnError.get() && !alreadyShownAnError &&
				!parser.isErrorShown()) {
			alreadyShownAnError = true;
			mb.selectExpression(expData);
		} else if (last && !alreadyShownAnError) {
			mb.selectExpression(null);
		}
	}

}
