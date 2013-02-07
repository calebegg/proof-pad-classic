package org.proofpad;

import java.awt.*;
import java.io.File;
import java.io.Serializable;
import java.util.List;

public class Session implements Serializable {
	private static final long serialVersionUID = 4806457808825043549L;

	private static class SavedWindow implements Serializable {
		public SavedWindow() { }
		private static final long serialVersionUID = -3654242987588009951L;
		File file;
		Rectangle location;
		String contents;
		@SuppressWarnings("unused") int admittedLoc;
		@SuppressWarnings("unused") int scrollPos;
	}
	
	private final SavedWindow[] savedWindows;
	
	public Session(List<PPWindow> windows) {
		savedWindows = new SavedWindow[windows.size()];
		int i = 0;
		for (PPWindow win : windows) {
			SavedWindow sw = new SavedWindow();
			sw.file = win.openFile;
			sw.location = win.getBounds();
			sw.admittedLoc = win.proofBar.getReadOnlyIndex();
			if (!win.isSaved) {
				sw.contents = win.editor.getText();
			} else {
				sw.contents = null;
			}
			savedWindows[i] = sw;
			i++;
		}
	}

	public void restore() {
		for (SavedWindow sw : savedWindows) {
			if (sw == null) continue;
			PPWindow win = new PPWindow(sw.file);
			win.setBounds(sw.location);
			if (sw.contents != null) {
				win.editor.setText(sw.contents);
			}
			win.setVisible(true);
		}
	}
	
	
}
