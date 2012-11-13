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

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

public class OutputWindow extends JFrame {
	private static final long serialVersionUID = -763205019202829248L;
	private final IdeWindow ideWindow;
	private Runnable afterPreview;

	public OutputWindow(IdeWindow ideWindow) {
		getRootPane().putClientProperty("Window.style", "small");
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		this.ideWindow = ideWindow;
		this.setVisible(false);
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
		getRootPane().add(bottom, BorderLayout.SOUTH);
	}

	public void showWithText(String output, Runnable after) {
		JTextArea textComp = new JTextArea(output);
		textComp.setFont(Prefs.font.get());
		textComp.setEditable(false);
		textComp.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		if (afterPreview != null) afterPreview.run();
		afterPreview = after;
		Component oldComp = ((BorderLayout) getRootPane().getLayout()).getLayoutComponent(BorderLayout.CENTER);
		if (oldComp != null) getRootPane().remove(oldComp);
		JScrollPane textScroller = new JScrollPane(textComp);
		textScroller.setBorder(BorderFactory.createEmptyBorder());
		getRootPane().add(textScroller, BorderLayout.CENTER);
		int height = textComp.getPreferredScrollableViewportSize().height + 100;
		int width = 80 * getFontMetrics(ideWindow.getFont()).charWidth('a');
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		if (height > screenSize.height) {
			height = screenSize.height;
			System.out.println(width);
			width += new JScrollBar().getPreferredSize().width;
			System.out.println(width);
		}
		setSize(width, height);
		Point whereToGo = ideWindow.moveForOutputWindow();
		setLocation(whereToGo);
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