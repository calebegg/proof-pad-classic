package org.proofpad;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.text.DefaultEditorKit;
import java.awt.event.ActionListener;

public class ContextMenu extends JPopupMenu {
	private static final long serialVersionUID = 16174547297114415L;
	public ContextMenu(final CodePane parent) {
		final JMenuItem lookUp = new JMenuItem();
		lookUp.setText("Look up...");
		lookUp.setEnabled(false);
		lookUp.addActionListener(new UserData.LogUse("contextLookUp"));
		add(lookUp);
		addPopupMenuListener(new PopupMenuListener() {
			@Override public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
				String word = parent.getSelectedText();
				parent.getMousePosition();
				if (word == null || word.isEmpty()) {
					word = parent.getWordAtMouse();
				}
				if (word != null && Main.cache.getDocs().containsKey(word.toUpperCase())) {
					lookUp.setText("Look up \"" + word + "\"");
					lookUp.addActionListener(parent.new LookUpListener(word));
					lookUp.setEnabled(true);
				} else {
					for (ActionListener al : lookUp.getActionListeners()) {
						if (al instanceof CodePane.LookUpListener) {
							lookUp.removeActionListener(al);
						}
					}
					lookUp.setText("Look up...");
					lookUp.setEnabled(false);
				}
			}

			@Override public void popupMenuWillBecomeInvisible(PopupMenuEvent arg0) { }
			@Override public void popupMenuCanceled(PopupMenuEvent arg0) { }
		});
		JMenuItem item = new JMenuItem(new DefaultEditorKit.CutAction());
		item.setText("Cut");
		item.addActionListener(new UserData.LogUse("contextCut"));
		add(item);
		item = new JMenuItem(new DefaultEditorKit.CopyAction());
		item.setText("Copy");
		item.addActionListener(new UserData.LogUse("contextCopy"));
		add(item);
		item = new JMenuItem(new DefaultEditorKit.PasteAction());
		item.setText("Paste");
		item.addActionListener(new UserData.LogUse("contextPaste"));
		add(item);
	}
}
