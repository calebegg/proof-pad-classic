package com.calebegg.ide;
import java.io.*;
import java.util.*;

import com.calebegg.ide.Repl.MsgType;

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
	private BufferedWriter out;
	final String acl2Path;
	private boolean errorOccured;
	private StringBuilder sb;
	final public Vector<String> exps;
	private OutputEventListener outputEventListener;
	private List<Callback> callbacks;
	private int backoff = 1;
	File workingDir;

	private List<RestartListener> restartListeners = new LinkedList<RestartListener>();

	private int procId;

	public Acl2(String acl2Path, File workingDir) {
		this(acl2Path, workingDir, null);
	}
	public Acl2(String acl2Path, File workingDir, Callback callback) {
		this.acl2Path = acl2Path;
		exps = new Vector<String>();
		sb = new StringBuilder();
		callbacks = new LinkedList<Callback>();
		this.workingDir = workingDir;
		// Startup callback
		callbacks.add(callback);
		// FIXME: Initial message not showing.
	}

	public void run() {
		List<Character> buffer = new LinkedList<Character>();
		if (acl2 == null) {
			throw new NullPointerException("Call initialize() first");
		}
		while (true) {
			synchronized (this) {
				try {
					try {
						acl2.exitValue();
						// If we get here, the process has terminated.
						for (Callback callback : callbacks) {
							if (callback != null) callback.run(false, "");
						}
						callbacks.clear();
						fireRestartEvent();
						fireOutputEvent(new OutputEvent("ACL2 has terminated. Use ACL2 > Restart ACL2 to restart.", MsgType.INFO));
						return;
						//restart();
					} catch (IllegalThreadStateException e) { }
					if (!in.ready()) {
						backoff = Math.min(backoff + 1, 12); // Maxes out at about 4 seconds.
						wait((long)Math.pow(2, backoff));
					}
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
								Callback callback = callbacks.size() > 0 ? callbacks.remove(0) : null;
								String response = sb.toString().substring(0, sb.length() - p.size());
								if (callback == null || callback.run(!errorOccured, response)) {
									fireOutputEvent(new OutputEvent(response,
											errorOccured ? Repl.MsgType.ERROR : Repl.MsgType.SUCCESS));
								}
								sb = new StringBuilder();
								errorOccured = false;
								break;
							}
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
	private void fireOutputEvent(OutputEvent outputEvent) {
		if (getOutputEventListener() != null) {
			getOutputEventListener().handleOutputEvent(outputEvent);
		}
	}

	public void initialize() throws IOException {
		ProcessBuilder processBuilder;
		if (IdeWindow.isWindows) {
			processBuilder = new ProcessBuilder(acl2Path);
			//processBuilder = new ProcessBuilder("hiddencon.exe", acl2Path);
		} else {
			processBuilder = new ProcessBuilder("sh", "-c", "echo \"$$\"; exec \"$0\" \"$@\"" + acl2Path);
		}
		processBuilder.directory(workingDir);
		acl2 = processBuilder.start();
		in = new BufferedReader(new InputStreamReader(acl2.getInputStream()));
		if (!IdeWindow.isWindows) {
			procId = Integer.parseInt(in.readLine());
		}
		out = new BufferedWriter(new OutputStreamWriter(acl2.getOutputStream()));
		admit("(add-include-book-dir :teachpacks \"teachpacks\")", doNothingCallback);
		admit("(set-compile-fns nil)", doNothingCallback);
		//admit("(set-gag-mode t)", doNothingCallback);
		errorOccured = false;
	}
	public void admit(String code, Callback callback) {
		code = code
				.replaceAll("\\)\\(", ") (")
				.replaceAll(";.*?\r?\n", "")
				.replaceAll("#\\|.*?\\|#", "")
				.trim() + '\n';
		if (code.isEmpty()) {
			return;
		}
		int parenLevel = 0;
		boolean isWord = false;
		boolean isColonCmd = false;
		StringBuilder exp = new StringBuilder();
		List<String> exps = new LinkedList<String>();
		for (char c : code.toCharArray()) {
			if (isColonCmd) {
				if (c == '\n') {
					exp.append(')');
					isColonCmd = false;
					exps.add(exp.toString());
					exp = new StringBuilder();
				} else {
					exp.append(c);
				}
				continue;
			}
			if (isWord && Character.isWhitespace(c)) {
				isWord = false;
				exps.add(exp.toString());
				exp = new StringBuilder();
				continue;
			}
			if (c == '(') {
				parenLevel++;
				exp.append(c);
			} else if (c == ')') {
				parenLevel--;
				exp.append(c);
			} else if (c == ':' && !isWord && parenLevel == 0) {
				exp.append('(');
				isColonCmd = true;
				continue;
			} else if (!Character.isWhitespace(c) && exp.length() == 0) {
				// Starting a word
				exp.append(c);
				isWord = true;
				continue;
			} else {
				exp.append(c);
			}
			if (parenLevel == 0 && !exp.toString().matches(("\\s+"))) {
				exps.add(exp.toString());
				exp = new StringBuilder();
			}
		}
		System.out.println(exps);
		for (String current : exps) {
			callbacks.add(callback);
			try {
				out.write(current + "\n");
				out.flush();
			} catch (IOException e) { }
			synchronized (this) {
				backoff = 0;
				notify();
			}
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
		initialize();
		fireRestartEvent();
	}
	private void fireRestartEvent() {
		for (RestartListener l : restartListeners) {
			l.acl2Restarted();
		}
	}
	public void terminate() {
		if (IdeWindow.isWindows) {
			try {
				Runtime.getRuntime().exec(new String[] {"sendbreak.exe", Integer.toString(procId)});
				acl2.waitFor();
			} catch (InterruptedException e) {
			} catch (IOException e) { }
		}
		acl2.destroy();
	}
	
	public void interrupt() {
		backoff = 0;
		if (IdeWindow.isWindows) {
			try {
				Runtime.getRuntime().exec(new String[] {"sendctrlc.exe", Integer.toString(procId)});
			} catch (IOException e) { }
		} else {
			try {
				Runtime.getRuntime().exec(new String[] {"kill", "-s", "INT", Integer.toString(procId)});
			} catch (IOException e) { }			
		}
	}

	public OutputEventListener getOutputEventListener() {
		return outputEventListener;
	}

	public void setOutputEventListener(OutputEventListener outputEventListener) {
		this.outputEventListener = outputEventListener;
	}
	
	public static boolean isError(String line) {
		for (String failure : failures) {
			if (line.startsWith(failure)) {
				return true;
			}
		}
		return false;
	}

	public void addRestartListener(RestartListener restartListener) {
		restartListeners.add(restartListener);
	}
}