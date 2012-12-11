package org.proofpad;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Date;
import java.util.Iterator;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.ProgressMonitor;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;

import com.apple.eawt.AboutHandler;
import com.apple.eawt.AppEvent.AboutEvent;
import com.apple.eawt.AppEvent.AppForegroundEvent;
import com.apple.eawt.AppEvent.AppReOpenedEvent;
import com.apple.eawt.AppEvent.OpenFilesEvent;
import com.apple.eawt.AppEvent.PreferencesEvent;
import com.apple.eawt.AppEvent.QuitEvent;
import com.apple.eawt.AppForegroundListener;
import com.apple.eawt.AppReOpenedListener;
import com.apple.eawt.Application;
import com.apple.eawt.OpenFilesHandler;
import com.apple.eawt.PreferencesHandler;
import com.apple.eawt.QuitHandler;
import com.apple.eawt.QuitResponse;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.StaxDriver;

public class Main {
	public static final String displayName = "Proof Pad";
	public static final int RELEASE = 3;
	public static final Border WINDOW_BORDER = BorderFactory.createEmptyBorder(4, 4, 4, 4);
	public static final boolean OSX = System.getProperty("os.name").indexOf("Mac") != -1;
	public static final boolean WIN =
	System.getProperty("os.name").toLowerCase().indexOf("windows") != -1;
	public static final boolean JAVA_7 = System.getProperty("java.version").startsWith("1.7");
	
	static final String SESSION_PATH = new File(getJarPath()).getParent() +
			System.getProperty("file.separator") + "saved_session.dat";
	static final String USER_DATA_PATH = new File(getJarPath()).getParent() +
			System.getProperty("file.separator") +
			"user_data.dat";
    public static final String UPLOAD_URL = "http://www.calebegg.com/ppuserdata";
	public static UserData userData = null;
	static {
		try {
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(USER_DATA_PATH));
			userData = (UserData) ois.readObject();
			ois.close();
		} catch (Exception e) {
			userData = null;
		}
		if (userData == null) {
			userData = new UserData();
		}
	}
	
	public static boolean startingUp = true;
	public static long startTime = System.currentTimeMillis();
	public static CacheData cache;
	
	public static final boolean FAKE_WINDOWS = false;

	public static MenuBar menuBar;
	
	public static void main(final String[] args) throws FileNotFoundException, IOException,
			ClassNotFoundException {
		logtime("Starting main");
		// http://java.net/jira/browse/MACOSX_PORT-764
//		System.setProperty("apple.awt.brushMetalLook", "true");
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
			@Override public void uncaughtException(Thread t, Throwable e) {
				new ErrorWindow(e).setVisible(true);
			}
		});
		
		logtime("Start loading cache");
		ObjectInputStream ois = new ObjectInputStream(Main.class.getResource("/data/cache.dat").openStream());
		cache = (CacheData) ois.readObject();
		logtime("Loaded cache");
		
		if (Main.OSX) {
			Application app = Application.getApplication();
			app.setOpenFileHandler(new OpenFilesHandler() {
				@Override public void openFiles(OpenFilesEvent e) {
					for (Object file : e.getFiles()) {
						PPWindow.createOrReuse((File) file);
					}
				}
			});
			app.setAboutHandler(new AboutHandler() {
				@Override public void handleAbout(AboutEvent e) {
					new AboutWindow().setVisible(true);
				}
			});
			app.setQuitHandler(new QuitHandler() {
				@Override public void handleQuitRequestWith(QuitEvent qe,
						QuitResponse qr) {
					if (!quit()) {
						qr.cancelQuit();
					}
				}
			});
			app.setPreferencesHandler(new PreferencesHandler() {
				@Override public void handlePreferences(PreferencesEvent arg0) {
					PrefsWindow.getInstance().setVisible(true);
				}
			});
			app.addAppEventListener(new AppForegroundListener() {
				@Override public void appMovedToBackground(AppForegroundEvent arg0) {
					if (PPWindow.windows.size() == 0 && !startingUp) {
						quit();
					}
				}
				@Override public void appRaisedToForeground(AppForegroundEvent arg0) {}
			});
			app.addAppEventListener(new AppReOpenedListener() {
				@Override public void appReOpened(AppReOpenedEvent arg0) {
					if (PPWindow.windows.size() == 0 && !startingUp) {
						SwingUtilities.invokeLater(new Runnable() {
							@Override public void run() {
								new PPWindow().setVisible(true);
							}
						});
					}
				}
			});
			if (!FAKE_WINDOWS) {
				menuBar = new MenuBar(null);
				// http://java.net/jira/browse/MACOSX_PORT-775
				if (!JAVA_7) {
					app.setDefaultMenuBar(menuBar);
				}
				PopupMenu dockMenu = new PopupMenu();
				MenuItem item = new MenuItem("New");
				item.addActionListener(new ActionListener() {
					@Override public void actionPerformed(ActionEvent e) {
						PPWindow ide = new PPWindow();
						ide.setVisible(true);
					}
				});
				//dockMenu.add(defaultMenuBar.recentMenu);
				dockMenu.add(item);
				app.setDockMenu(dockMenu);
			}
		}
		SwingUtilities.invokeLater(new Runnable() {
			@Override public void run() {
				logtime("Start restoring session");
				if (Prefs.saveSession.get()) {
					tryToRestoreSession();
				}
				logtime("Session restore complete.");
				for (String arg : args) {
					new PPWindow(new File(arg)).setVisible(true);
				}
				if (PPWindow.windows.isEmpty()) {
					PPWindow win = new PPWindow();
					startingUp = false;
					win.setVisible(true);
				} else {
					startingUp = false;
				}
				logtime("Main window visible");
				Date now = new Date();
				Date oneWeekAgo = new Date(now.getTime() - 1000 * 60 * 60 * 24 * 7);
				if (userData.recordingStart.before(oneWeekAgo)) {
					promptAndSendUserData();
				}
			}
		});
	}
	
	static boolean quit() {
		boolean needToCloseWindows = true;
		if (Prefs.saveSession.get()) {
			needToCloseWindows = false;
			Session session = new Session(PPWindow.windows);
			ObjectOutputStream oos;
			try {
				oos = new ObjectOutputStream(new FileOutputStream(
						SESSION_PATH));
				oos.writeObject(session);
				oos.close();
			} catch (Exception e) {
				e.printStackTrace();
				needToCloseWindows = true;
			}
		}
		if (needToCloseWindows) {
			for (Iterator<PPWindow> ii = PPWindow.windows.iterator(); ii.hasNext();) {
				PPWindow win = ii.next();
				if (!win.promptIfUnsavedAndQuit(ii)) {
					break;
				}
			}
		}
		PPWindow.updateWindowMenu();
		if (!needToCloseWindows || PPWindow.windows.size() <= 0) {
			saveUserDataAndExit();
			return true;
		}
		return false;
	}
	
	protected static void saveUserDataAndExit() {
		ObjectOutputStream oos;
//		System.out.println("Quitting");
		try {
			oos = new ObjectOutputStream(new FileOutputStream(USER_DATA_PATH));
			oos.writeObject(userData);
			oos.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.exit(0);
	}
	
	static String getJarPath() {
		try {
			return URLDecoder.decode(Main.class.getProtectionDomain().getCodeSource().getLocation()
							.getPath(), "UTF-8");
		} catch (UnsupportedEncodingException e) { }
		return "";
	}
	
	public static void logtime(String event) {
		System.out.println(event + ": " + (System.currentTimeMillis() - startTime) + "ms");
	}
	
	static void sendUserData() {
		final ProgressMonitor pm = new ProgressMonitor(null, "Sending user data", null, 0, 100);
		pm.setProgress(0);
		new Thread(new Runnable() {
			@Override public void run() {
				System.out.println("Sending user data");
				XStream xs = new XStream(new StaxDriver());
				String xml = xs.toXML(userData);
				try {
					String data = "data=" + URLEncoder.encode(xml, "UTF-8");
					System.out.println(xml);
					URL url = new URL(UPLOAD_URL);
					URLConnection conn = url.openConnection();
					conn.setDoOutput(true);
					OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
					int chunkSize = 100;
					int chunks = data.length() / chunkSize + 1;
					for (int chunk = 0; chunk < chunks; chunk++) {
						wr.write(data, chunk * chunkSize, Math.min(chunkSize,
								data.length() - chunk * chunkSize));
						pm.setProgress(chunk * 100 / chunks);
						if (pm.isCanceled()) {
							wr.close();
							return;
						}
					}
					wr.close();
					// This makes the connection actually go through.
					conn.getInputStream();
					userData = new UserData();
					pm.setProgress(100);
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				} catch (ConnectException e) {
					System.out.println("User data server down: " + UPLOAD_URL);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}).start();
	}

	static void promptAndSendUserData() {
		int alwaysSend = Prefs.alwaysSend.get();
		if (alwaysSend != Prefs.Codes.ASK_EVERY_TIME) {
			if (alwaysSend == Prefs.Codes.ALWAYS_SEND) {
				sendUserData();
			}
			return;
		}
		JCheckBox saveAction = new JCheckBox("Do the same thing every week");
		Object[] params = {"<html>You've been using Proof Pad for one week. In order to " +
				"<br />continually improve Proof Pad, we ask that you volunteer your " +
				"<br />anonymous usage and error data.</html>", saveAction};
		Object[] options = { "Send data", "Don't send" };
		int shouldSend = JOptionPane.showOptionDialog(null, params, "",
				JOptionPane.YES_NO_OPTION,
				JOptionPane.QUESTION_MESSAGE,
				null,
				options,
				options[0]
				);
		if (saveAction.isSelected()) {
			Prefs.alwaysSend
					.set(shouldSend == JOptionPane.YES_OPTION ? Prefs.Codes.ALWAYS_SEND
							: Prefs.Codes.NEVER_SEND);
		}
		if (shouldSend == JOptionPane.YES_OPTION) {
			sendUserData();
		}
	}

	static void tryToRestoreSession() {
		if (!new File(SESSION_PATH).exists()) return;
		try {
			ObjectInputStream sessionRestoreStream = new ObjectInputStream(
					new FileInputStream(SESSION_PATH));
			Session session = (Session) sessionRestoreStream.readObject();
			sessionRestoreStream.close();
			session.restore();
		} catch (Exception e) {
			e.printStackTrace();
			Prefs.saveSession.set(false);
			JOptionPane.showMessageDialog(null, "Failed to restore from saved session. " +
					"Session saving has been turned off to prevent any future data loss.",
					"Session restore failed", JOptionPane.ERROR_MESSAGE);
		}
	}
}
