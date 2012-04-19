package org.proofpad;
import java.io.*;
import java.util.*;

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
	private BufferedWriter out;
	final String acl2Path;
	private boolean errorOccured;
	private StringBuilder sb;
	private OutputEventListener outputEventListener;
	private List<Callback> callbacks = new LinkedList<Callback>();
	private List<OutputEvent> outputQueue = new LinkedList<OutputEvent>();
	private int backoff = 1;
	private Acl2TokenMaker tm = new Acl2TokenMaker();
	File workingDir;

	private List<RestartListener> restartListeners = new LinkedList<RestartListener>();

	private int procId;

	private boolean fullSuccess = true;
	private String fullOutput = "";

	int numInitExps;

	private boolean initializing;

	private Set<String> functionsToTrace = new HashSet<String>(Arrays.asList(new String[] {
		"IN-PACKAGE",
		"THM",
		"AREF1",
		"AREF2",
		"ASET1",
		"ASET2",
		"COMPRESS1",
		"COMPRESS2",
		"ARRAY1P",
		"ARRAY2P",
		"DEFAULT",
		"DIMENSIONS",
		"FLUSH-COMPRESS",
		"HEADER",
		"MAXIMUM-LENGTH",
		"*",
		"+",
		"-",
		"/",
		"/=",
		"1+",
		"1-",
		"<",
		"<=",
		"=",
		">",
		">=",
		"ABS",
		"ACL2-NUMBERP",
		"ACL2-USER",
		"ACONS",
		"ADD-TO-SET",
		"ADD-TO-SET-EQ",
		"ADD-TO-SET-EQL",
		"ADD-TO-SET-EQUAL",
		"ALISTP",
		"ALLOCATE-FIXNUM-RANGE",
		"ALPHA-CHAR-P",
		"ALPHORDER",
		"AND",
		"APPEND",
		"ARRAYS",
		"ASH",
		"ASSERT$",
		"ASSOC",
		"ASSOC-EQ",
		"ASSOC-EQUAL",
		"ASSOC-KEYWORD",
		"ASSOC-STRING-EQUAL",
		"ATOM",
		"ATOM-LISTP",
		"BINARY-*",
		"BINARY-+",
		"BINARY-APPEND",
		"BOOLE$",
		"BOOLEANP",
		"BUTLAST",
	/*  "CAAAAR",
			"CAAADR",
			"CAAAR",
			"CAADAR",
			"CAADDR",
			"CAADR",
			"CAAR",
			"CADAAR",
			"CADADR",
			"CADAR",
			"CADDAR",
			"CADDDR",
			"CADDR",
			"CADR", */
		"CAR",
		"CASE",
		"CASE-MATCH",
		/*			"CDAAAR",
			"CDAADR",
			"CDAAR",
			"CDADAR",
			"CDADDR",
			"CDADR",
			"CDAR",
			"CDDAAR",
			"CDDADR",
			"CDDAR",
			"CDDDAR",
			"CDDDDR",
			"CDDDR",
			"CDDR", */
		"CDR",
		"CEILING",
		"CERTIFY-BOOK",
		"CHAR",
		"CHAR-CODE",
		"CHAR-DOWNCASE",
		"CHAR-EQUAL",
		"CHAR-UPCASE",
		"CHAR<",
		"CHAR<=",
		"CHAR>",
		"CHAR>=",
		"CHARACTER-ALISTP",
		"CHARACTER-LISTP",
		"CHARACTERP",
		"CHARACTERS",
		"CLOSE-INPUT-CHANNEL",
		"CLOSE-OUTPUT-CHANNEL",
		"CODE-CHAR",
		"COERCE",
		"COMP",
		"COMP-GCL",
		"COMPILATION",
		"COMPLEX",
		"COMPLEX-RATIONALP",
		"COMPLEX/COMPLEX-RATIONALP",
		"CONCATENATE",
		"COND",
		"CONJUGATE",
		"CONS",
		"CONSP",
		"COUNT",
		"CPU-CORE-COUNT",
		"CW",
		"CW!",
		"DECLARE",
		"DEFMACRO-LAST",
		"DELETE-ASSOC",
		"DELETE-ASSOC-EQ",
		"DELETE-ASSOC-EQUAL",
		"DENOMINATOR",
		"DIGIT-CHAR-P",
		"DIGIT-TO-CHAR",
		"E0-ORD-<",
		"E0-ORDINALP",
		"EC-CALL",
		"EIGHTH",
		"ENDP",
		"EQ",
		"EQL",
		"EQLABLE-ALISTP",
		"EQLABLE-LISTP",
		"EQLABLEP",
		"EQUAL",
		"EQUALITY-VARIANTS",
		"ER",
		"ER-PROGN",
		"ERROR1",
		"EVENP",
		"EXPLODE-NONNEGATIVE-INTEGER",
		"EXPT",
		"FIFTH",
		"FIRST",
		"FIX",
		"FIX-TRUE-LIST",
		"FLET",
		"FLOOR",
/*		"FMS",
		"FMS!",
		"FMS!-TO-STRING",
		"FMS-TO-STRING",
		"FMT",
		"FMT!",
		"FMT!-TO-STRING",
		"FMT-TO-COMMENT-WINDOW",
		"FMT-TO-STRING",
		"FMT1",
		"FMT1!",
		"FMT1!-TO-STRING",
		"FMT1-TO-STRING", */
		"FOURTH",
		"GET-OUTPUT-STREAM-STRING$",
		"GETENV$",
		"GETPROP",
		"GOOD-ATOM-LISTP",
		"HARD-ERROR",
		"IDENTITY",
//		"IF",
		"IFF", /*
		"IFIX",
		"IGNORABLE",
		"IGNORE",
		"ILLEGAL",
		"IMAGPART",
		"IMPLIES",
		"IMPROPER-CONSP",
		"INT=",
		"INTEGER-LENGTH",
		"INTEGER-LISTP",
		"INTEGERP",
		"INTERN",
		"INTERN$",
		"INTERN-IN-PACKAGE-OF-SYMBOL",
		"INTERSECTION$",
		"INTERSECTION-EQ",
		"INTERSECTION-EQUAL",
		"INTERSECTP",
		"INTERSECTP-EQ",
		"INTERSECTP-EQUAL",
		"IRRELEVANT-FORMALS",
		"KEYWORD-VALUE-LISTP",
		"KEYWORDP",
		"KWOTE",
		"KWOTE-LST",
		"LAST",
		"LEN",
		"LENGTH",
		"LET",
		"LET*",
		"LEXORDER",
		"LIST",
		"LIST*",
		"LISTP",
		"LOGAND",
		"LOGANDC1",
		"LOGANDC2",
		"LOGBITP",
		"LOGCOUNT",
		"LOGEQV",
		"LOGIOR",
		"LOGNAND",
		"LOGNOR",
		"LOGNOT",
		"LOGORC1",
		"LOGORC2",
		"LOGTEST",
		"LOGXOR",
		"LOWER-CASE-P",
		"MAKE-CHARACTER-LIST",
		"MAKE-LIST",
		"MAKE-ORD",
		"MAX",
		"MBE",
		"MBE1",
		"MBT",
		"MEMBER",
		"MEMBER-EQ",
		"MEMBER-EQUAL",
		"MIN",
		"MINUSP",
		"MOD",
		"MOD-EXPT",
		"MUST-BE-EQUAL",
		"MUTUAL-RECURSION",
		"MV",
		"MV-LET",
		"MV-LIST",
		"MV-NTH",
		"MV?",
		"MV?-LET",
		"NATP",
		"NFIX",
		"NINTH",
		"NO-DUPLICATESP",
		"NO-DUPLICATESP-EQ",
		"NO-DUPLICATESP-EQUAL",
		"NONNEGATIVE-INTEGER-QUOTIENT",
		"NOT",
		"NTH",
		"NTHCDR",
		"NULL",
		"NUMERATOR",
		"O-FINP",
		"O-FIRST-COEFF",
		"O-FIRST-EXPT",
		"O-INFP",
		"O-P",
		"O-RST",
		"O<",
		"O<=",
		"O>",
		"O>=",
		"OBSERVATION",
		"OBSERVATION-CW",
		"ODDP",
		"OPEN-INPUT-CHANNEL",
		"OPEN-INPUT-CHANNEL-P",
		"OPEN-OUTPUT-CHANNEL",
		"OPEN-OUTPUT-CHANNEL-P",
		"OPTIMIZE",
		"OR",
		"PAIRLIS",
		"PAIRLIS$",
		"PEEK-CHAR$",
		"PKG-IMPORTS",
		"PKG-WITNESS",
		"PLUSP", */
		"POSITION",
		"POSITION-EQ",
		"POSITION-EQUAL",
		"POSP",
		"PPROGN",
//		"PRINT-OBJECT$",
//		"PROG2$",
//		"PROGN$",
//		"PROOFS-CO",
//		"PROPER-CONSP",
//		"PUT-ASSOC",
//		"PUT-ASSOC-EQ",
//		"PUT-ASSOC-EQL",
//		"PUT-ASSOC-EQUAL",
//		"PUTPROP",
//		"QUOTE",
//		"R-EQLABLE-ALISTP",
//		"R-SYMBOL-ALISTP",
//		"RANDOM$",
//		"RASSOC",
//		"RASSOC-EQ",
//		"RASSOC-EQUAL",
//		"RATIONAL-LISTP",
//		"RATIONALP",
//		"READ-BYTE$",
//		"READ-CHAR$",
//		"READ-OBJECT",
//		"REAL/RATIONALP",
//		"REALFIX",
//		"REALPART",
//		"REM",
//		"REMOVE",
//		"REMOVE-DUPLICATES",
//		"REMOVE-DUPLICATES-EQ",
//		"REMOVE-DUPLICATES-EQUAL",
//		"REMOVE-EQ",
//		"REMOVE-EQUAL",
//		"REMOVE1",
//		"REMOVE1-EQ",
//		"REMOVE1-EQUAL",
//		"REST",
//		"RETURN-LAST",
//		"REVAPPEND",
//		"REVERSE",
//		"RFIX",
//		"ROUND",
//		"SEARCH",
//		"SECOND",
//		"SET-DIFFERENCE$",
//		"SET-DIFFERENCE-EQ",
//		"SET-DIFFERENCE-EQUAL",
//		"SETENV$",
//		"SEVENTH",
//		"SIGNUM",
//		"SIXTH",
//		"STANDARD-CHAR-LISTP",
//		"STANDARD-CHAR-P",
//		"STANDARD-STRING-ALISTP",
//		"STRING",
//		"STRING-APPEND",
//		"STRING-DOWNCASE",
//		"STRING-EQUAL",
//		"STRING-LISTP",
//		"STRING-UPCASE",
//		"STRING<",
//		"STRING<=",
//		"STRING>",
//		"STRING>=",
//		"STRINGP",
//		"STRIP-CARS",
//		"STRIP-CDRS",
//		"SUBLIS",
//		"SUBSEQ",
//		"SUBSETP",
//		"SUBSETP-EQ",
//		"SUBSETP-EQUAL",
//		"SUBST",
//		"SUBSTITUTE",
//		"SYMBOL-<",
//		"SYMBOL-ALISTP",
//		"SYMBOL-LISTP",
//		"SYMBOL-NAME",
//		"SYMBOL-PACKAGE-NAME",
//		"SYMBOLP",
//		"SYS-CALL",
//		"SYS-CALL-STATUS",
//		"TAKE",
//		"TENTH",
//		"THE",
		"THIRD",
		"TIME$",
		"TRACE",
		"TRUE-LIST-LISTP",
		"TRUE-LISTP",
		"TRUNCATE",
		"TYPE",
		"TYPE-SPEC",
		"UNARY--",
		"UNARY-/",
		"UNION$",
		"UNION-EQ",
		"UNION-EQUAL",
		"UPDATE-NTH",
		"UPPER-CASE-P",
		"WITH-LIVE-STATE",
		"WRITE-BYTE$",
		"XOR",
		"ZEROP",
		"ZIP",
		"ZP",
		"ZPF",
	}));

	private final static String marker = "PROOFPAD-MARKER:" + "proofpad".hashCode();
	private final static List<Character> markerChars = stringToCharacterList(marker);

	public Acl2(String acl2Path, File workingDir) {
		this(acl2Path, workingDir, null);
	}
	public Acl2(String acl2Path, File workingDir, Callback callback) {
		this.acl2Path = acl2Path;
		sb = new StringBuilder();
		this.workingDir = workingDir;
		// Startup callback
		callbacks.add(callback);
	}

	@Override
	public void run() {
		List<Character> buffer = new LinkedList<Character>();
		if (acl2 == null) {
			throw new NullPointerException("Call initialize() first");
		}
		outputQueue.add(null); // to not eat the initial message
		while (true) {
			synchronized (this) {
				try {
					try {
						acl2.exitValue();
						// If we get here, the process has terminated.
						failAllCallbacks();
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
								String response = sb.toString().substring(0, sb.length() - p.size());
								if (outputQueue.size() > 0) {
									fullSuccess &= !errorOccured;
									fullOutput += response;
								}
								outputQueue.add(new OutputEvent(response,
										errorOccured ? Repl.MsgType.ERROR : Repl.MsgType.SUCCESS));
//								System.out.println("RESPONSE: [[[" + response + "]]]");
								sb = new StringBuilder();
								errorOccured = false;
								break;
							}
						}
						if (markerChars != null &&
								buffer.size() > markerChars.size() &&
								buffer.subList(buffer.size() - markerChars.size(), buffer.size()).equals(markerChars)) {
//							System.out.println("READ A MARKER");
							fireOutputEvents(fullSuccess);
							fullSuccess = true;
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
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
		out.write("(cw \"" + marker + "\")\n");
		String draculaPath;
		if (IdeWindow.isWindows) {
			draculaPath = "/PROGRA~1/PROOFP~1/acl2/dracula";
		} else {
			try {
				draculaPath = new File(acl2Path).getParent().replaceAll("\\\\", "") + "/dracula";
			} catch (Exception e) {
				draculaPath = "";
			}
		}
		initializing = true;
		numInitExps = 0;
		admit("(add-include-book-dir :teachpacks \"" + draculaPath + "\")", doNothingCallback);
		admit("(set-compile-fns nil)", doNothingCallback);
		admit("(defmacro __trace-wrap (name args body)\n" +
				"   `(prog2$ (cw \"__trace-enter-(~x0 ~*1)~%\"\n" +
				"                (quote ,name)\n" +
				"                (list \"\" \"~x*\" \"~x* \" \"~x* \" ,args))\n" +
				"         (let ((__value ,body))\n" +
				"              (prog2$ (cw \"__trace-exit- = ~x1~%\"\n" +
				"                          (quote ,name)\n" +
				"                          __value)\n" +
				"                      __value))))\n" +
				"\n" +
				"(defmacro __trace-defun (name args body)\n" +
				"   `(defun ,name ,args\n" +
				"      (declare (xargs :mode :program))\n" +
				"       (__trace-wrap ,name\n" +
				"                     (list ,@args)\n" +
				"                     ,body)))\n" +
				"\n" +
				"(defmacro __trace-builtin (trace-name name)\n" +
				"   `(defmacro ,trace-name (&rest args)\n" +
				"       `(__trace-wrap ,(quote ,name)\n" +
				"                      (quote (,@args))\n" +
				"                      (,(quote ,name)\n" +
				"                        ,@args))))\n"
, doNothingCallback);
		for (String fun : functionsToTrace) {
			admit("(__trace-builtin __trace-" + fun + " " + fun + ")", doNothingCallback);
		}
		//admit("(set-gag-mode t)", doNothingCallback);
		errorOccured = false;
		initializing = false;
	}
	
	public void admit(String code, Callback callback) {
		admit(code, callback, false);
	}
	
	private void admit(String code, Callback callback, boolean trace) {
		code = code + '\n';
		code = code
				.replaceAll(";.*?\r?\n", "")
				.replaceAll("^\\:(.*?)\r?\n", "\\($1\\)")
				.replaceAll("\r?\n", " ")
				.replaceAll("#\\|.*?\\|#", "")
				.trim();
		if (code.isEmpty()) {
			return;
		}
		int parenLevel = 0;
		boolean isTracing = false;
		int traceIdx = 0;
		StringBuilder exp = new StringBuilder();
		StringBuilder traceExp = new StringBuilder();
		List<String> exps = new LinkedList<String>();
		
		Token t = tm.getTokenList(new Segment(code.toCharArray(), 0, code.length()), Token.NULL, 0);
		while (t != null && t.offset != -1) {
			if (t.isSingleChar('(')) {
				parenLevel++;
			} else if (t.isSingleChar(')')) {
				parenLevel--;
			} else if (parenLevel == 1 && t.getLexeme().equalsIgnoreCase("defun")) {
				traceExp.append('(');
				isTracing = true;
			}
			if (isTracing || trace) {
				if (t.type == Token.RESERVED_WORD || t.type == Token.RESERVED_WORD_2) {
					String name = t.getLexeme();
					if (functionsToTrace.contains(name.toUpperCase()) || name.equalsIgnoreCase("defun")) {
						traceExp.append("__trace-" + name);						
					} else {
						traceExp.append(name);
					}
				} else if (t.type == Token.IDENTIFIER) {
					traceExp.append("__trace-" + t.getLexeme());
				} else {
					traceExp.append(t.getLexeme());
				}
			}
			exp.append(t.getLexeme());
			if (parenLevel == 0 && !exp.toString().matches("\\s*")) {
				if (!trace) {
					exps.add(exp.toString());
				} else {
					exps.add(traceExp.toString());
				}
				if (isTracing) {
					isTracing = false;
					exps.add(traceIdx, traceExp.toString());
					traceIdx++;
				}
				exp = new StringBuilder();
				traceExp = new StringBuilder();
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
			try {
				out.write(current + "\n");
				out.write("(cw \"" + marker + "\")\n");
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
				//acl2.waitFor();
			} catch (IOException e) { }
		}
		acl2.destroy();
	}
	
	@Override
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
}