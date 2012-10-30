package org.proofpad;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.EventListener;
import java.util.LinkedList;
import java.util.List;
import java.util.prefs.Preferences;

import javax.swing.text.Segment;

import org.fife.ui.rsyntaxtextarea.Token;
import org.proofpad.Repl.MsgType;


public class Acl2 extends Thread {
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
		/**
		 * 
		 * @param success
		 * @param response
		 * @return Whether or not to print this output to the REPL.
		 */
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
	private Process acl2;
	private BufferedReader in;
	BufferedWriter out;
	private boolean errorOccured;
	private StringBuilder sb;
	private OutputEventListener outputEventListener;
	private final List<Callback> callbacks = new LinkedList<Callback>();
	private final List<OutputEvent> outputQueue = new LinkedList<OutputEvent>();
	private int backoff = 1;
	private final Acl2TokenMaker tm = new Acl2TokenMaker();
	File workingDir;

	private final List<RestartListener> restartListeners = new LinkedList<RestartListener>();

	private int procId;

	private boolean fullSuccess = true;
	private String fullOutput = "";

	int numInitExps;

	private boolean initializing;


	private final List<String> acl2Paths;

	String acl2Path;

	private final static String marker = "PROOFPAD-MARKER:" + "proofpad".hashCode();
	private final static List<Character> markerChars = stringToCharacterList(marker);

	public Acl2(List<String> acl2Paths, File workingDir, Acl2Parser parser) {
		this(acl2Paths, workingDir, null, parser);
	}
	public Acl2(List<String> acl2Paths, File workingDir, Callback callback, Acl2Parser parser) {
		this.acl2Paths = acl2Paths;
		sb = new StringBuilder();
		this.workingDir = workingDir;
		// Startup callback
		callbacks.add(callback);
	}

	@Override
	public void run() {
		final Preferences prefs = Preferences.userNodeForPackage(Main.class);
		if (prefs.getBoolean("firstRun", true)) {
			fireOutputEvent(new OutputEvent("Starting ACL2 for the first time. " +
					"Please be patient.", MsgType.INFO));
			prefs.putBoolean("firstRun", false);
		}
		List<Character> buffer = new LinkedList<Character>();
		if (acl2 == null) {
			//throw new NullPointerException("Call initialize() first");
			fireOutputEvent(new OutputEvent("ACL2 was not correctly started.", MsgType.ERROR));
			Main.userData.addError("ACL2 failed to start.");
			return;
		}
		outputQueue.add(null); // to not eat the initial message
		while (true) {
			synchronized (this) {
				try {
					if (!in.ready()) {
						try {
							acl2.exitValue();
							// If we get here, the process has terminated.
							failAllCallbacks();
							fireRestartEvent();
							fireOutputEvent(new OutputEvent("ACL2 has terminated. Use Tools > " +
									"Restart ACL2 to restart.", MsgType.INFO));
							Main.userData.addError("ACL2 terminated.");
							return;
							//restart();
						} catch (IllegalThreadStateException e) { }
						backoff = Math.min(backoff + 1, 12); // Maxes out at about 4 seconds.
						wait((long)Math.pow(2, backoff));
					}
					new Thread(new Runnable() {
						@Override
						public void run() {
							try {
								out.flush();
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					}).start();
					if (in.ready()) {
						backoff = 0;
						char c = (char) in.read();
//						System.out.print(c);
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
								String response = sb.toString().substring(0, sb.length() - p.size());
								if (outputQueue.size() > 0 && !response.contains("DEFUN __TRACE-")) {
									fullSuccess &= !errorOccured;
									fullOutput += response;
								}
								if (!response.contains("DEFUN __TRACE-")) {
									outputQueue.add(new OutputEvent(response,
											errorOccured ? Repl.MsgType.ERROR : Repl.MsgType.SUCCESS));
								}
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
					}
				} catch (Exception e) {
					e.printStackTrace();
					failAllCallbacks();
					fireRestartEvent();
					fireOutputEvent(new OutputEvent("ACL2 has terminated. Use Tools > " +
							"Restart ACL2 to restart.", MsgType.INFO));
					Main.userData.addError(e.getMessage());
					return;
				}
			}
		}
	}
	private void failAllCallbacks() {
		List<Callback> callbacksCopy = new LinkedList<Callback>(callbacks);
		for (Callback callback : callbacksCopy) {
			if (callback != null) callback.run(false, "");
		}
		callbacks.clear();
	}
	private void fireOutputEvents(boolean success) {
		if (outputQueue.size() > 0) {
			outputQueue.remove(0);
		}
		if (callbacks.size() > 0) {
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

		for (String maybeAcl2Path : acl2Paths) {
			if (IdeWindow.WIN) {
				//processBuilder = new ProcessBuilder("ctrlc-windows.exe", acl2Path);
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
				acl2 = processBuilder.start();
			} catch (IOException e) {
				System.out.println("ACL2 failed at: " + maybeAcl2Path);
				e.printStackTrace();
				continue;
			}
			acl2Path = maybeAcl2Path;
			workingDir = maybeWorkingDir;
			in = new BufferedReader(new InputStreamReader(acl2.getInputStream()));
			if (!IdeWindow.WIN) {
				//procId = Integer.parseInt(in.readLine());
			}
			out = new BufferedWriter(new OutputStreamWriter(acl2.getOutputStream()));
			out.write("(cw \"" + marker + "\")\n");
			String draculaPath;
			if (IdeWindow.WIN) {
				draculaPath = maybeAcl2Path.replace("run_acl2.exe", "dracula");
			} else {
				try {
					draculaPath = new File(maybeAcl2Path).getParent().replaceAll("\\\\", "") + "/dracula";
				} catch (Exception e) {
					draculaPath = "";
				}
			}
			initializing = true;
			numInitExps = 0;
			admit("(add-include-book-dir :teachpacks \"" + draculaPath + "\")", doNothingCallback);
			admit("(set-compile-fns nil)", doNothingCallback);
			break;
		}
		errorOccured = false;
		initializing = false;
	}
	
	public void admit(String code, Callback callback) {
		admit(code, callback, false);
	}
	
	private void admit(String code, Callback callback, boolean trace) {
		if (out == null) return;
		code = code + '\n';
		code = code
				.replaceAll(";.*?\r?\n", "")
				.replaceAll("(^|\r?\n)\\:(.*?)(\r?\n|$)", "\\($2\\)")
				.replaceAll("\r?\n", " ")
				.replaceAll("#\\|.*?\\|#", "")
				.trim();
		if (code.isEmpty()) {
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
			if (!current.contains("__trace") || trace) {
				callbacks.add(callback);
			}
			current = current.replaceAll("\\(q\\)", ":q\n"); // The only :command that has no function equivalent
			try {
				out.write(current + "\n");
				if (!current.contains("__trace") || trace) {
					out.write("(cw \"" + marker + "\")\n");
				}
			} catch (IOException e) { }
		}
		synchronized (this) {
			backoff = 0;
			notify();
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
		backoff = 0;
		terminate();
		initialize();
		fireRestartEvent();
	}
	private void fireRestartEvent() {
		for (RestartListener l : restartListeners) {
			l.acl2Restarted();
		}
	}
	
	public void terminate() {
		admit("(good-bye)", Acl2.doNothingCallback);
		if (IdeWindow.WIN) {
			// Depends on ctrlc-windows.exe
//			try {
//				out.write(0);
//				out.flush();
//			} catch (IOException e) { }
		}
		try {
			acl2.waitFor();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void interrupt() {
		backoff = 0;
		if (IdeWindow.WIN) {
			// Depends on ctrlc-windows.exe
			//writeByte(1);
		} else {
			try {
				Runtime.getRuntime().exec(new String[] {"kill", "-s", "INT", Integer.toString(procId)});
			} catch (IOException e) { }
		}
		fireOutputEvents(false);
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
	public void trace(String inputText, Callback callback) {
		admit(inputText, callback, true);
	}
	public String getAcl2Path() {
		return acl2Path;
	}
}