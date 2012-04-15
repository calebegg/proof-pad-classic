package org.proofpad;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

public class ResultWindow extends JDialog {

	private static final long serialVersionUID = -4360856107145207588L;
	
	JScrollPane scroller;

	public ResultWindow(Frame parent, String string) {
		// TODO: Hook this up to the parent IdeWindow's JMenu on mac.
		super(parent, string);
		getRootPane().putClientProperty("apple.awt.brushMetalLook", "false");
		getRootPane().putClientProperty("Window.style", "small");
		setAlwaysOnTop(true);
		setLayout(new BorderLayout());
		scroller = new JScrollPane();
		add(scroller, BorderLayout.CENTER);
		JPanel bottom = new JPanel();
		bottom.setLayout(new BoxLayout(bottom, BoxLayout.X_AXIS));
		bottom.add(Box.createGlue());
		JButton closeButton = new JButton("Close");
		closeButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		});
		bottom.add(closeButton);
		bottom.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
		add(bottom, BorderLayout.SOUTH);
		getRootPane().setBorder(Main.WINDOW_BORDER);
		setPreferredSize(new Dimension(630, 300));
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		pack();
		setLocationRelativeTo(null);
		
		//TODO: This doesn't work after clicking on the textarea. Some sort of focus issue.
		getRootPane().registerKeyboardAction(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		}, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), 0);
	}
	
	public void setContent(Component comp) {
		scroller.setViewportView(comp);
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				scroller.getVerticalScrollBar().setValue(0);
			}
		});
//		if (comp instanceof Scrollable) {
//			Dimension preferredSize = ((Scrollable) comp).getPreferredScrollableViewportSize();
//			Dimension extentSize = scroller.getViewport().getExtentSize();
//			int maxWidth = getWidth() - extentSize.width + preferredSize.width + 5;
//			int maxHeight = getHeight() - extentSize.height + preferredSize.height + 5;
//			setMaximizedBounds(new Rectangle(getLocation(), new Dimension(maxWidth, maxHeight)));
//		}
	}

}
