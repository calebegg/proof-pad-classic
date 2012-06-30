package org.proofpad;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.Iterator;

import javax.swing.*;
import javax.swing.border.Border;

import com.apple.eawt.*;
import com.apple.eawt.AppEvent.*;

/*
 *  Proof Pad: An IDE for ACL2.
 *  Copyright (C) 2012 Caleb Eggensperger
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *  
 *  To contact the author, send email to calebegg@gmail.com.
 */

/**
 *  @author Caleb Eggensperger
 *  @version {@value RELEASE}
 */

public class Main {
	
	public static final String displayName = "Proof Pad (alpha)";
	public static final int RELEASE = 1;
	public static final Border WINDOW_BORDER = BorderFactory.createEmptyBorder(4, 4, 4, 4);
	
	public static boolean startingUp = true;
	public static long startTime = System.currentTimeMillis();
	public static CacheData cache;
	
	public static final boolean FAKE_WINDOWS = false;

	public static MenuBar menuBar;
	
	public static void main(String[] args) throws FileNotFoundException, IOException,
			ClassNotFoundException {
		logtime("Starting main");
		System.setProperty("apple.awt.brushMetalLook", "true");
		if (!FAKE_WINDOWS) {
			System.setProperty("apple.laf.useScreenMenuBar", "true");
		}
		
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
//			UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
			@Override
			public void uncaughtException(Thread t, Throwable e) {
				String stackTrace = e + ": " + e.getMessage();
				StackTraceElement[] stes = e.getStackTrace();
				for (StackTraceElement ste : stes) {
					stackTrace += "\n" + ste;
				}
				JTextArea errorText = new JTextArea();
				JScrollPane sp = new JScrollPane(errorText);
				errorText.setText(stackTrace);
				errorText.setCaretPosition(0);
				errorText.setEditable(false);
				JFrame errorWindow = new JFrame("Error");
				errorWindow.add(sp);
				errorWindow.pack();
				errorWindow.setLocationRelativeTo(null);
				errorWindow.setVisible(true);
			}
		});
		
		logtime("Start loading cache");
		ObjectInputStream ois = new ObjectInputStream(Main.class.getResource("/data/cache.dat").openStream());
		cache = (CacheData) ois.readObject();
		logtime("Loaded cache");
		
		if (IdeWindow.OSX) {
			Application app = Application.getApplication();
			app.setOpenFileHandler(new OpenFilesHandler() {
				@Override
				public void openFiles(OpenFilesEvent e) {
					for (Object file : e.getFiles()) {
						IdeWindow win = new IdeWindow((File) file);
						win.setVisible(true);
					}
				}
			});
			app.setAboutHandler(new AboutHandler() {
				@Override
				public void handleAbout(AboutEvent e) {
					new AboutWindow().setVisible(true);
				}
			});
			app.setQuitHandler(new QuitHandler() {
				@Override
				public void handleQuitRequestWith(QuitEvent qe,
						QuitResponse qr) {
					for (Iterator<IdeWindow> ii = IdeWindow.windows.iterator(); ii.hasNext();) {
						IdeWindow win = ii.next();
						if (!win.promptIfUnsavedAndQuit(ii)) {
							break;
						}
					}
					IdeWindow.updateWindowMenu();
					if (IdeWindow.windows.size() <= 0) {
						System.exit(0);
					} else {
						qr.cancelQuit();
					}
				}
			});
			app.setPreferencesHandler(new PreferencesHandler() {
				@Override
				public void handlePreferences(PreferencesEvent arg0) {
					if (IdeWindow.prefsWindow == null) {
						IdeWindow.prefsWindow = new PrefsWindow();
					}
					IdeWindow.prefsWindow.setVisible(true);
				}
			});
			app.addAppEventListener(new AppForegroundListener() {
				@Override
				public void appMovedToBackground(AppForegroundEvent arg0) {
					if (IdeWindow.windows.size() == 0 && !startingUp) {
						System.exit(0);
					}
				}
				@Override
				public void appRaisedToForeground(AppForegroundEvent arg0) {}
			});
			app.addAppEventListener(new AppReOpenedListener() {
				@Override
				public void appReOpened(AppReOpenedEvent arg0) {
					if (IdeWindow.windows.size() == 0 && !startingUp) {
						SwingUtilities.invokeLater(new Runnable() {
							@Override
							public void run() {
								new IdeWindow().setVisible(true);
							}
						});
					}
				}
			});
			if (!FAKE_WINDOWS) {
				menuBar = new MenuBar(null);
				app.setDefaultMenuBar(menuBar);
				PopupMenu dockMenu = new PopupMenu();
				MenuItem item = new MenuItem("New");
				item.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						IdeWindow ide = new IdeWindow();
						ide.setVisible(true);
					}
				});
				//dockMenu.add(defaultMenuBar.recentMenu);
				dockMenu.add(item);
				app.setDockMenu(dockMenu);
			}
		}
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				logtime("Start creating main window");
				IdeWindow win = new IdeWindow();
				startingUp = false;
				win.setVisible(true);
				logtime("Main window visible");
			}
		});
	}
	public static void logtime(String event) {
		System.out.println(event + ": " + (System.currentTimeMillis() - startTime) + "ms");
	}
}
