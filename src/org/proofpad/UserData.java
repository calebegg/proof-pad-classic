package org.proofpad;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;
import java.util.Date;
import java.util.UUID;
import java.util.Vector;

public class UserData implements Serializable {

	static class LogUse implements ActionListener {
		String key;
		public LogUse(String key) {
			this.key = key;
		}
		@Override public void actionPerformed(ActionEvent e) {
			Main.userData.addUse(key, e.getModifiers() != 0);
		}
	}
	
	@SuppressWarnings("unused")
	public static class Use implements Serializable {
		private static final long serialVersionUID = 8381796174167421554L;
		final String key;
		final boolean keyboard;
		final Date when;
		public Use(String key, boolean keyboard, Date when) {
			this.key = key;
			this.keyboard = keyboard;
			this.when = when;
		}
	}
	
	@SuppressWarnings("unused")
	private static class ErrorData implements Serializable {
		private static final long serialVersionUID = 9144659354972366784L;
		private final String error;
		private final Date when;
		private final int release;
		public ErrorData(String error, Date when, int release) {
			this.when = when;
			this.error = error;
			this.release = release;
		}
	}

	private static final long serialVersionUID = -5532381141979625516L;
	
	final Date recordingStart = new Date();
	final UUID uuid = UUID.randomUUID();
	final String systemData;
	final Vector<Use> uses = new Vector<Use>();
	final Vector<ErrorData> errors = new Vector<ErrorData>();
	final Vector<ErrorData> parseErrors = new Vector<ErrorData>();
	final Vector<ErrorData> replMessages = new Vector<ErrorData>();

	
	public UserData() {
		systemData = System.getProperty("os.name") + " " + System.getProperty("os.version") + " " +
				System.getProperty("os.arch");
	}
	
	synchronized void addError(String error) {
		errors.add(new ErrorData(error, new Date(), Main.RELEASE));
	}
	
	synchronized void addUse(String key, boolean keyboard) {
		uses.add(new Use(key, keyboard, new Date()));
	}

	synchronized void addParseError(String msg) {
		parseErrors.add(new ErrorData(msg, new Date(), Main.RELEASE));		
	}

	synchronized void addReplMsg(String msg) {
		replMessages.add(new ErrorData(msg, new Date(), Main.RELEASE));		
	}
}
