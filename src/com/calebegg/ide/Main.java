package com.calebegg.ide;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Iterator;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.apple.eawt.AppEvent.OpenFilesEvent;

/*  Proof Pad: An IDE for ACL2.
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

public class Main {
	
	public static final String displayName = "Proof Pad (alpha)";
	
	public static boolean startingUp = true;
	public static long startTime = System.currentTimeMillis();
	public static CacheData cache;
	
	public static void main(String[] args) throws FileNotFoundException, IOException,
			ClassNotFoundException {
		logtime("Starting main");
		System.setProperty("apple.awt.brushMetalLook", "true");
		System.setProperty("apple.laf.useScreenMenuBar", "true");
		System.setProperty("apple.awt.useSystemHelp", "true");
		
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
//			UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		logtime("Start loading cache");
		ObjectInputStream ois = new ObjectInputStream(new FileInputStream("cache.dat"));
		cache = (CacheData) ois.readObject();
		logtime("Loaded cache");
		
		if (IdeWindow.isMac) {
			com.apple.eawt.Application app = com.apple.eawt.Application.getApplication();
			app.setOpenFileHandler(new com.apple.eawt.OpenFilesHandler() {
				@Override
				public void openFiles(OpenFilesEvent e) {
					for (Object file : e.getFiles()) {
						IdeWindow win = new IdeWindow((File) file);
						win.setVisible(true);
					}
				}
			});
			app.setAboutHandler(new com.apple.eawt.AboutHandler() {
				@Override
				public void handleAbout(com.apple.eawt.AppEvent.AboutEvent e) {
					new AboutWindow().setVisible(true);
				}
			});
			app.setQuitHandler(new com.apple.eawt.QuitHandler() {
				@Override
				public void handleQuitRequestWith(com.apple.eawt.AppEvent.QuitEvent qe,
						com.apple.eawt.QuitResponse qr) {
					for (Iterator<IdeWindow> ii = IdeWindow.windows.iterator(); ii.hasNext();) {
						IdeWindow win = ii.next();
						win.promptIfUnsavedAndQuit(ii);
							//ii.remove();
					}
					IdeWindow.updateWindowMenu();
					if (IdeWindow.windows.size() <= 0) {
						System.exit(0);
					} else {
						qr.cancelQuit();
					}
				}
			});
			app.setPreferencesHandler(new com.apple.eawt.PreferencesHandler() {
				@Override
				public void handlePreferences(com.apple.eawt.AppEvent.PreferencesEvent arg0) {
					if (IdeWindow.prefsWindow == null) {
						IdeWindow.prefsWindow = new PrefsWindow();
					}
					IdeWindow.prefsWindow.setVisible(true);
				}
			});
			app.addAppEventListener(new com.apple.eawt.AppForegroundListener() {
				@Override
				public void appMovedToBackground(com.apple.eawt.AppEvent.AppForegroundEvent arg0) {
					if (IdeWindow.windows.size() == 0 && !startingUp) {
						System.exit(0);
					}
				}
				public void appRaisedToForeground(com.apple.eawt.AppEvent.AppForegroundEvent arg0) {}
			});
			app.addAppEventListener(new com.apple.eawt.AppReOpenedListener() {
				@Override
				public void appReOpened(com.apple.eawt.AppEvent.AppReOpenedEvent arg0) {
					if (IdeWindow.windows.size() == 0 && !startingUp) {
						SwingUtilities.invokeLater(new Runnable() {
							public void run() {
								new IdeWindow().setVisible(true);
							}
						});
					}
				}
			});
			MenuBar defaultMenuBar = new MenuBar(null);
			app.setDefaultMenuBar(defaultMenuBar);
			PopupMenu dockMenu = new PopupMenu();
			MenuItem item = new MenuItem("New");
			item.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					IdeWindow ide = new IdeWindow();
					ide.setVisible(true);
				}
			});
			//dockMenu.add(defaultMenuBar.recentMenu);
			dockMenu.add(item);
			app.setDockMenu(dockMenu);

			// TODO: :-(
			//com.apple.eawt.FullScreenUtilities.setWindowCanFullScreen(this, true);
		}
		SwingUtilities.invokeLater(new Runnable() {
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
