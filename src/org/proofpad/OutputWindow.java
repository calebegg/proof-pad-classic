package org.proofpad;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.Scrollable;
import javax.swing.SwingUtilities;

import org.proofpad.Repl.Message;
import org.proofpad.Repl.MsgType;

public class OutputWindow extends JFrame {
	private static final long serialVersionUID = -763205019202829248L;
	private final PPWindow ideWindow;
	private Runnable afterPreview;

	public OutputWindow(PPWindow ideWindow) {
		getRootPane().putClientProperty("Window.style", "small");
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		this.ideWindow = ideWindow;
		setVisible(false);
		getRootPane().setBorder(BorderFactory.createEmptyBorder());
		getRootPane().setLayout(new BorderLayout());
		JPanel bottom = new JPanel();
		bottom.setLayout(new BoxLayout(bottom, BoxLayout.X_AXIS));
		final JCheckBox showEveryTime = new JCheckBox("Show this window automatically for each error.");
		showEveryTime.setSelected(Prefs.showOutputOnError.get());
		showEveryTime.addItemListener(new ItemListener() {
			@Override public void itemStateChanged(ItemEvent e) {
				Prefs.showOutputOnError.set(showEveryTime.isSelected());
			}
		});		bottom.add(showEveryTime);
		bottom.add(Box.createHorizontalGlue());
		JButton closeButton = new JButton("Close");
		closeButton.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) {
				hideWindow();
			}
		});
		bottom.add(closeButton);
		addWindowListener(new WindowListener() {
			@Override public void windowOpened(WindowEvent e) { }
			@Override public void windowIconified(WindowEvent e) { }
			@Override public void windowDeiconified(WindowEvent e) { }
			@Override public void windowDeactivated(WindowEvent e) { }
			@Override public void windowClosing(WindowEvent e) {
				hideWindow();
			}
			@Override public void windowClosed(WindowEvent e) { }
			@Override public void windowActivated(WindowEvent e) { }
		});
		getRootPane().add(bottom, BorderLayout.PAGE_END);
	}

	public void showWithText(String output, MsgType type, Runnable after) {
		JComponent comp;
		if (Repl.isTestResults(output)) {
			comp = new DoubleCheckResult(output);
		} else {
			JTextArea textComp = new JTextArea(output);
			textComp.setEditable(false);
			comp = textComp;
		}
		comp.setFont(Prefs.font.get());
		comp.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		
		JPanel summaries = new JPanel();
		summaries.setLayout(new BoxLayout(summaries, BoxLayout.Y_AXIS));
		List<Message> msgs = Repl.summarize(output, type);
		for (Message msg : msgs) {
			JComponent line = ideWindow.repl.createSummary(msg.msg, msg.type, null);
			summaries.add(line);
		}
		Component oldPgStartComp = ((BorderLayout) getRootPane().getLayout())
				.getLayoutComponent(BorderLayout.PAGE_START);
		if (oldPgStartComp != null) getRootPane().remove(oldPgStartComp);
		getRootPane().add(summaries, BorderLayout.PAGE_START);
		if (afterPreview != null) afterPreview.run();
		afterPreview = after;
		Component oldCenterComp = ((BorderLayout) getRootPane().getLayout())
				.getLayoutComponent(BorderLayout.CENTER);
		if (oldCenterComp != null) getRootPane().remove(oldCenterComp);
		JScrollPane textScroller = new JScrollPane(comp);
		textScroller.setBorder(BorderFactory.createEmptyBorder());
		textScroller.getVerticalScrollBar().setUnitIncrement(20);
		getRootPane().add(textScroller, BorderLayout.CENTER);
		int height;
		if (comp instanceof Scrollable) {
		    height = ((Scrollable) comp).getPreferredScrollableViewportSize().height + 100;
		} else {
			height = comp.getPreferredSize().height + 100;
		}
		int width = 80 * getFontMetrics(Prefs.font.get()).charWidth('a');
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		if (height > screenSize.height) {
			height = screenSize.height;
			width += new JScrollBar().getPreferredSize().width;
		}
		setSize(width, height);
		if (!isVisible()) {
			Point whereToGo = ideWindow.moveForOutputWindow();
			setLocation(whereToGo);
		}
		setFocusableWindowState(false);
		setVisible(true);
		SwingUtilities.invokeLater(new Runnable() {
			@Override public void run() {
				toFront();
				setFocusableWindowState(true);
				new Thread(new Runnable() {
					@Override public void run() {
						try {
							Thread.sleep(3000);
						} catch (InterruptedException e) { }
						getRootPane().revalidate();
						repaint();
					}
				}).start();
				getRootPane().revalidate();
				repaint();
			}
		});
	}
	
	public void hideWindow() {
		setVisible(false);
		if (afterPreview != null) {
			afterPreview.run();
		}
		afterPreview = null;
	}

}
