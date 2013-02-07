package org.proofpad;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.Scrollable;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.proofpad.Repl.Message;
import org.proofpad.Repl.MsgType;

public class OutputWindow extends PPDialog {
	private static final long serialVersionUID = -763205019202829248L;
	private final PPWindow ideWindow;
	private Runnable afterPreview;

	public OutputWindow(PPWindow ideWindow) {
		super((PPWindow)null, "");
		getRootPane().putClientProperty("Window.style", "small");
		setDefaultCloseOperation(HIDE_ON_CLOSE);
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
		addWindowListener(new WindowAdapter() {
			@Override public void windowClosing(WindowEvent e) {
				hideWindow();
			}
		});
		getRootPane().add(bottom, BorderLayout.PAGE_END);
	}

	public void showWithText(String output, MsgType type, Runnable after) {
		JPanel summaries = new JPanel();
		summaries.setLayout(new BoxLayout(summaries, BoxLayout.Y_AXIS));
		List<Message> msgs = Repl.summarize(output, type);
		for (Message msg : msgs) {
			JComponent line = ideWindow.repl.createSummary(msg.msg, msg.type, null);
			summaries.add(line);
		}
		JComponent comp;
		if (Repl.isTestResults(output)) {
			comp = new DoubleCheckResult(output);
		} else {
			final JTextArea textComp = new JTextArea(output);
			textComp.setEditable(false);
			if (!msgs.isEmpty()) {
				changeToDisabledColor(textComp);
				textComp.addMouseListener(new MouseAdapter() {
					@Override public void mouseExited(MouseEvent arg0) {
						changeToDisabledColor(textComp);
					}
					@Override public void mouseEntered(MouseEvent arg0) {
						changeToEnabledColor(textComp);
					}
				});
			}
			comp = textComp;
		}
		comp.setFont(Prefs.font.get());
		comp.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		JScrollPane scroller = new JScrollPane(comp);
		
		Component oldPgStartComp = ((BorderLayout) getRootPane().getLayout())
				.getLayoutComponent(BorderLayout.PAGE_START);
		if (oldPgStartComp != null) getRootPane().remove(oldPgStartComp);
		getRootPane().add(summaries, BorderLayout.PAGE_START);
		if (afterPreview != null) afterPreview.run();
		afterPreview = after;
		Component oldCenterComp = ((BorderLayout) getRootPane().getLayout())
				.getLayoutComponent(BorderLayout.CENTER);
		if (oldCenterComp != null) getRootPane().remove(oldCenterComp);
		scroller.setBorder(BorderFactory.createEmptyBorder());
		scroller.getVerticalScrollBar().setUnitIncrement(20);
		getRootPane().add(scroller, BorderLayout.CENTER);
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
	
	static void changeToDisabledColor(JTextArea textComp) {
		Object disabledBgColor = UIManager.get("TextField.disabledBackground");
		if (disabledBgColor != null && disabledBgColor instanceof Color) {
			textComp.setBackground((Color) disabledBgColor);
		}
		textComp.setForeground(textComp.getDisabledTextColor());
	}
	
	static void changeToEnabledColor(JTextArea textComp) {
		textComp.setBackground(Color.WHITE);
		textComp.setForeground(Color.BLACK);
	}

	public void hideWindow() {
		setVisible(false);
		if (afterPreview != null) {
			afterPreview.run();
		}
		afterPreview = null;
	}

}
