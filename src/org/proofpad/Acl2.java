package org.proofpad;
import org.fife.ui.rsyntaxtextarea.Token;
import org.proofpad.InfoBar.CloseListener;
import org.proofpad.InfoBar.InfoButton;
import org.proofpad.Repl.MsgType;

import javax.swing.text.Segment;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.EventListener;
import java.util.LinkedList;
import java.util.List;

public class Acl2 extends Thread {
	public interface OutputChangeListener {
		void outputChanged(String string);
	}

	private static final int ACL2_IS_SLOW_DELAY = 15000;

	public final StringBuilder logOutput = new StringBuilder();

	private class Spooler extends Thread {
		InputStream in;
		final List<Character> spool = new LinkedList<Character>();
		public Spooler(InputStream in) {
			super("Process spooler");
			this.in = in;
		}
		@Override public void run() {
			while (true) {
				try {
					int c = in.read();
					if (c == -1) {
						return;
					}
					logOutput.append((char) c);
					fireOutputChangeEvent();
//					System.out.print((char) c);
					synchronized (this) {
						spool.add((char) c);
						notify();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	public interface ErrorListener {
		public InfoBar handleError(String msg, InfoButton[] btns);
	}

	public interface RestartListener {
		public void acl2Restarted();
	}

	public interface OutputEventListener extends EventListener {
		public void handleOutputEvent(OutputEvent outputEvent);
	}

	public class OutputEvent {
		final public String output;
		final public MsgType type;
		public OutputEvent(String output, MsgType type) {
			this.output = output;
			this.type = type;
		}
	}

	public interface Callback {
		public boolean run(boolean success, String response);
	}

	public final static Callback doNothingCallback = new Callback() {
		@Override
		public boolean run(boolean success, String response) {
			return false;
		}
	};

	static List<List<Character>> prompt = new LinkedList<List<Character>>();
	static List<List<Character>> failure = new LinkedList<List<Character>>();
	static String[] failures;
	static {
		String[] prompts = {
				"ACL2 !>",
				"ACL2 p>",
				"ACL2 p!>",
				"ACL2 P>",
				"ACL2 >",
				"? [RAW LISP]",
		};
		failures = new String[] {
				"************ ABORTING from raw Lisp ***********",
				"******** FAILED ********",
				"HARD ACL2 ERROR",
				"ACL2 Error",
		};
		for (String p : prompts) {
			prompt.add(stringToCharacterList(p));
		}
		for (String f : failures) {
			failure.add(stringToCharacterList(f));
		}
	}
	Process acl2Process;
	private Spooler sp;

	private boolean errorOccured;
	private StringBuilder sb;
	private OutputEventListener outputEventListener;
	private final List<Callback> callbacks = new LinkedList<Callback>();
	private final List<OutputEvent> outputQueue = new LinkedList<OutputEvent>();
	private final Acl2TokenMaker tm = new Acl2TokenMaker();
	File workingDir;

	final boolean isBuilder;

	private final List<RestartListener> restartListeners = new LinkedList<RestartListener>();

	private int procId;

	private boolean fullSuccess = true;
	boolean acl2IsSlowShown;
	private long lastAdmittedTimestamp = 0;
	private String fullOutput = "";

	int numInitExps;

	private boolean initializing;


	private final List<String> acl2Paths;

	String acl2Path;

	private ErrorListener errorListener;

	InfoBar currentInfobar;

	private boolean isRestarting = false;

	BufferedWriter out;

	boolean isTerminating;

	private final LinkedList<OutputChangeListener> outputChangeListeners = new LinkedList<OutputChangeListener>();

	private boolean hasStarted = false;

	private final static String marker = "PROOFPAD-MARKER:" + "proofpad".hashCode();
	private final static List<Character> markerChars = stringToCharacterList(marker);

	protected void fireOutputChangeEvent() {
		for (OutputChangeListener ocl : outputChangeListeners) {
			ocl.outputChanged(logOutput.toString());
		}
	}
	public void addOutputChangeListener(OutputChangeListener ocl) {
		outputChangeListeners.add(ocl);
		ocl.outputChanged(logOutput.toString());
	}
	public void removeOutputChangeListener(OutputChangeListener ocl) {
		outputChangeListeners.remove(ocl);
	}

	public Acl2(List<String> acl2Paths, File workingDir, boolean isBuilder) {
		super("ACL2 background thread");
		this.isBuilder = isBuilder;
		this.acl2Paths = acl2Paths;
		sb = new StringBuilder();
		this.workingDir = workingDir;
		// Startup callback
		callbacks.add(null);
	}

	public Acl2(List<String> acl2Paths, File workingDir) {
		this(acl2Paths, workingDir, false);
	}

	@Override
	public void run() {
		if (Prefs.firstRun.get()) {
			fireOutputEvent(new OutputEvent("Starting ACL2 for the first time. " +
					"Please be patient.", MsgType.INFO));
			Prefs.firstRun.set(false);
		}
		List<Character> buffer = new LinkedList<Character>();
		if (acl2Process == null) {
			fireOutputEvent(new OutputEvent("ACL2 did not start correctly.", MsgType.ERROR));
			Main.userData.addError("ACL2 failed to start.");
			return;
		}
		outputQueue.add(null); // to not eat the initial message
		while (true) {
			try {
				if (lastAdmittedTimestamp < System.currentTimeMillis() - ACL2_IS_SLOW_DELAY &&
						!acl2IsSlowShown && !callbacks.isEmpty()) {
					acl2IsSlowShown = true;
					currentInfobar = fireErrorEvent("ACL2 is taking a while.", new InfoButton[] {
							new InfoButton("Interrupt",
								new ActionListener() {
									@Override public void actionPerformed(ActionEvent arg0) {
										ctrlc();
									}
								})});
					currentInfobar.addCloseListener(new CloseListener() {
						@Override public void onClose() {
							acl2IsSlowShown = false;
						}
					});
				}
				synchronized(sp) {
					if (sp.spool.isEmpty()) {
						sp.wait();
					}
				}
				if (sp.spool.isEmpty()) continue;
				char c;
				synchronized(sp) {
					c = sp.spool.remove(0);
				}
//				System.out.print(c);
				buffer.add(c);
				if (buffer.size() > 100) {
					buffer.remove(0);
				}
				sb.append(c);
				for (List<Character> f : failure) {
					if (buffer.size() > f.size() &&
							buffer.subList(buffer.size() - f.size(), buffer.size()).equals(f)) {
						errorOccured = true;
						break;
					}
				}
				for (List<Character> p : prompt) {
					if (buffer.size() > p.size() &&
							buffer.subList(buffer.size() - p.size(), buffer.size()).equals(p)) {
						lastAdmittedTimestamp = System.currentTimeMillis();
						if (acl2IsSlowShown && currentInfobar != null) {
							currentInfobar.close();
							acl2IsSlowShown = false;
						}
						String response = sb.toString().substring(0, sb.length() - p.size());
						if (outputQueue.size() > 0) {
							fullSuccess &= !errorOccured;
							fullOutput += response;
						}
						outputQueue.add(new OutputEvent(response,
								errorOccured ? Repl.MsgType.ERROR : Repl.MsgType.SUCCESS));
						if (errorOccured) {
							//Main.userData.addAcl2Error(response);
						}
						sb = new StringBuilder();
						errorOccured = false;
						break;
					}
				}
				if (markerChars != null &&
						buffer.size() > markerChars.size() &&
						buffer.subList(buffer.size() - markerChars.size(), buffer.size()).equals(markerChars)) {
					fireOutputEvents(fullSuccess);
					fullSuccess = true;
				}
			} catch (InterruptedException e) {
				continue;
			} catch (Exception e) {
				e.printStackTrace();
				failAllCallbacks();
				fireRestartEvent();
				terminate();
				showAcl2TerminatedError();
				return;
			}
		}
	}
	void showAcl2TerminatedError() {
		if (isRestarting) return;
		currentInfobar = fireErrorEvent("ACL2 has terminated.", new InfoButton[] { new InfoButton("Restart",
				new ActionListener() {
					@Override public void actionPerformed(ActionEvent arg0) {
						try {
							restart();
						} catch (IOException e) { }
					}
				}) });
		Main.userData.addError("ACL2 terminated.");
	}
	private InfoBar fireErrorEvent(String string, InfoButton[] infoButton) {
		if (errorListener != null) {
			return errorListener.handleError(string, infoButton);
		}
		return null;
	}

	void failAllCallbacks() {
		List<Callback> callbacksCopy = new LinkedList<Callback>(callbacks);
		for (Callback callback : callbacksCopy) {
			if (callback != null) callback.run(false, "");
		}
		callbacks.clear();
	}

	private void fireOutputEvents(boolean success) {
		if (!outputQueue.isEmpty()) {
			outputQueue.remove(0);
		}
		if (!callbacks.isEmpty()) {
			Callback cb = callbacks.remove(0);
			if (cb == null || cb.run(success, fullOutput)) {
				for (OutputEvent oe : outputQueue) {
					fireOutputEvent(oe);
				}
			}
			outputQueue.clear();
			fullOutput = "";
		}
	}
	private void fireOutputEvent(OutputEvent outputEvent) {
		if (getOutputEventListener() != null) {
			getOutputEventListener().handleOutputEvent(outputEvent);
		}
	}

	public void initialize() throws IOException {
		ProcessBuilder processBuilder;
		acl2IsSlowShown = false;
		for (String maybeAcl2Path : acl2Paths) {
			System.out.println(acl2Paths);
			if (Main.WIN) {
//				String ctrlcPath = new File(Main.getJarPath()).getParent() + "\\ctrlc-windows.exe";
//				processBuilder = new ProcessBuilder(ctrlcPath, maybeAcl2Path);
				processBuilder = new ProcessBuilder(maybeAcl2Path);
			} else {
				processBuilder = new ProcessBuilder("sh", "-c", "echo \"$$\"; exec \"$0\" \"$@\"" + maybeAcl2Path);
			}
			File maybeWorkingDir;
			if (workingDir == null) {
				maybeWorkingDir = (new File(maybeAcl2Path).getParentFile());
			} else {
				maybeWorkingDir = workingDir;
			}
			processBuilder.directory(workingDir);
			try {
				acl2Process = processBuilder.start();
			} catch (IOException e) {
				// Try the next path
				e.printStackTrace();
				continue;
			}
			acl2Path = maybeAcl2Path;
			workingDir = maybeWorkingDir;
			if (!Main.WIN) {
				BufferedReader in = new BufferedReader(new InputStreamReader(acl2Process.getInputStream()));
				procId = Integer.parseInt(in.readLine());
			}
			sp = new Spooler(acl2Process.getInputStream());
			sp.start();
			out = new BufferedWriter(new OutputStreamWriter(acl2Process.getOutputStream()));
			writeAndFlush("(cw \"" + marker + "\")\n");
			String draculaPath = "";
			try {
				if (Main.WIN) {
					draculaPath = new File(maybeAcl2Path).getParent().replaceAll("\\\\", "/") + "/dracula";
				} else {
					draculaPath = new File(maybeAcl2Path).getParent().replaceAll("\\\\", "") + "/dracula";
				}
			} catch (Exception ignored) { }
			initializing = true;
			numInitExps = 0;
			admit("(add-include-book-dir :teachpacks \"" + draculaPath + "\")", doNothingCallback);
			admit("(set-compile-fns nil)", doNothingCallback);
			break;
		}
		errorOccured = false;
		// Start a thread to check ACL2 occasionally
		Thread acl2Monitor = new Thread(new Runnable() {
			@Override
			public void run() {
				while (true) {
					try {
						Thread.sleep(isBuilder ? 500 : 4000);
					} catch (InterruptedException ignored) {
					}
					if (acl2Process == null) return;
					try {
						int exitVal = acl2Process.exitValue();
						// If we get here, the process has terminated.
						System.out.println("Exit code: " + exitVal);
						failAllCallbacks();
						fireRestartEvent();
						showAcl2TerminatedError();
						return;
					} catch (IllegalThreadStateException ignored) {
					}
				}
			}
		}, "ACL2 monitor");
		acl2Monitor.start();
		initializing = false;
	}

	private void writeAndFlush(String string) {
		try {
			out.write(string);
			new Thread(new Runnable() {
				@Override public void run() {
					try {
						out.flush();
					} catch (IOException ignored) { }
				}
			}).start();
		} catch (IOException ignored) { }
	}
	public void admit(String code, Callback callback) {
		if (out == null) return;
		lastAdmittedTimestamp = System.currentTimeMillis();
		code = code + '\n';
		code = code
				.replaceAll(";.*?\r?\n", "")
				.replaceAll("(^|\r?\n)\\:(.*?)(\r?\n|$)", "\\($2\\)")
				.replaceAll("\r?\n", " ")
				.replaceAll("#\\|.*?\\|#", "")
				.trim();
		if (code.isEmpty()) {
			callback.run(true, "");
			return;
		}
		int parenLevel = 0;
		StringBuilder exp = new StringBuilder();
		List<String> exps = new LinkedList<String>();

		Token t = tm.getTokenList(new Segment(code.toCharArray(), 0, code.length()), Token.NULL, 0);
		while (t != null && t.offset != -1) {
			if (t.isSingleChar('(')) {
				parenLevel++;
			} else if (t.isSingleChar(')')) {
				parenLevel--;
			}
			exp.append(t.getLexeme());
			if (parenLevel == 0 && !exp.toString().matches("\\s*")) {
				exps.add(exp.toString());

				exp = new StringBuilder();
			}
			t = t.getNextToken();
		}

//		System.out.println(exps);
		for (String current : exps) {
			if (initializing) {
				numInitExps++;
			}
			callbacks.add(callback);
			current = current.replaceAll("\\(q\\)", ":q\n"); // The only :command that has no function equivalent
			writeAndFlush(current + "\n");
			writeAndFlush("(cw \"" + marker + "\")\n");
		}
		if (exps.isEmpty()) {
			callback.run(true, "");
		}
	}
	private static List<Character> stringToCharacterList(String s) {
		List<Character> r = new LinkedList<Character>();
		for (char c : s.toCharArray()) {
			r.add(c);
		}
		return r;
	}

	public void restart() throws IOException {
		isRestarting = true;
		terminate();
		initialize();
		this.interrupt();
		fireRestartEvent();
		System.out.println("ACL2 is restarting");
		failAllCallbacks();
		isRestarting = false;
	}

	void fireRestartEvent() {
		for (RestartListener l : restartListeners) {
			l.acl2Restarted();
		}
	}

	public void terminate() {
		isTerminating = true;
		if (acl2Process == null) return;
		admit("(good-bye)", Acl2.doNothingCallback);
		if (Main.WIN) {
			writeByte(0);
		}
		// Give it five seconds to shut down
		new Thread(new Runnable() {
			@Override public void run() {
				try {
					sleep(5000);
				} catch (InterruptedException e) { }
				if (isTerminating) Acl2.this.interrupt();
			}
		}).start();
		try {
			acl2Process.waitFor();
		} catch (InterruptedException e) {
			acl2Process.destroy();
		}
		isTerminating = false;
	}

	private void writeByte(int b) {
		try {
			out.write(b);
			out.flush();
		} catch (IOException e) { }
	}

	public void ctrlc() {
		if (Main.WIN) {
			writeByte(1);
		} else {
			try {
				Runtime.getRuntime().exec(new String[] {"kill", "-s", "INT", Integer.toString(procId)});
			} catch (IOException e) { }
		}
		fullOutput += sb.toString();
		sb = new StringBuilder();
		fireOutputEvents(false);
		System.out.println("ACL2 was interrupted");
		failAllCallbacks();
	}

	public OutputEventListener getOutputEventListener() {
		return outputEventListener;
	}

	public void setOutputEventListener(OutputEventListener outputEventListener) {
		this.outputEventListener = outputEventListener;
	}

	public static boolean isError(String line) {
		for (String f : failures) {
			if (line.startsWith(f)) {
				return true;
			}
		}
		return false;
	}

	public void addRestartListener(RestartListener restartListener) {
		restartListeners.add(restartListener);
	}
	public void undo() {
		admit(":u\n", doNothingCallback);		
	}
	public String getAcl2Path() {
		return acl2Path;
	}
	public ErrorListener getErrorListener() {
		return errorListener;
	}
	public void setErrorListener(ErrorListener errorListener) {
		this.errorListener = errorListener;
	}
	@Override public synchronized void start() {
		hasStarted = true;
		super.start();
	}
	public boolean hasStarted() {
		return hasStarted;
	}
}