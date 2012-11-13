package org.proofpad;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.Stack;
import java.util.logging.Logger;

import javax.swing.text.BadLocationException;

import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.parser.AbstractParser;
import org.fife.ui.rsyntaxtextarea.parser.DefaultParseResult;
import org.fife.ui.rsyntaxtextarea.parser.DefaultParserNotice;
import org.fife.ui.rsyntaxtextarea.parser.ParseResult;
import org.fife.ui.rsyntaxtextarea.parser.ParserNotice;

public class Acl2Parser extends AbstractParser {
	
    public interface ParseListener {
		void wasParsed();
	}

	private static Logger logger = Logger.getLogger(Acl2Parser.class.toString());
	
	public static class CacheKey implements Serializable {
		private static final long serialVersionUID = -4201796432147755450L;
		private final File book;
		private final long mtime;

		public CacheKey(File book, long mtime) {
			this.book = book;
			this.mtime = mtime;
		}
		@Override
		public int hashCode() {
			return book.hashCode() ^ Long.valueOf(mtime).hashCode();
		}
		@Override
		public boolean equals(Object other) {
			return (other instanceof CacheKey &&
					((CacheKey) other).book.equals(this.book) &&
					((CacheKey) other).mtime == this.mtime);
		}
	}
	
	private static class Range implements Serializable {
		private static final long serialVersionUID = 3510110011135344206L;
		public final int lower;
		public final int upper;
		Range(int lower, int upper) {
			this.lower = lower;
			this.upper = upper;
		}
	}
	
	static class CacheSets implements Serializable {
		private static final long serialVersionUID = -2233827686979689741L;
		public Set<String> functions;
		public Set<String> macros;
		public Set<String> constants;		
	}

	public Set<String> functions;
	public Set<String> macros;
	public Set<String> constants;
	public File workingDir;
	private Map<CacheKey, CacheSets> cache = Main.cache.getBookCache();
	private File acl2Dir;
	private final List<ParseListener> parseListeners = new LinkedList<Acl2Parser.ParseListener>();
	
	public Acl2Parser(File workingDir, File acl2Dir) {
		this.workingDir = workingDir;
		this.setAcl2Dir(acl2Dir);
	}

	private static Map<String, Range> paramCounts = new HashMap<String, Range>();
	static {
		paramCounts.put("-", new Range(1, 2));
		paramCounts.put("/", new Range(1, 2));
		paramCounts.put("/=", new Range(2, 2));
		paramCounts.put("1+", new Range(1, 1));
		paramCounts.put("1-", new Range(1, 1));
		paramCounts.put("<=", new Range(2, 2));
		paramCounts.put(">", new Range(2, 2));
		paramCounts.put(">=", new Range(2, 2));
		paramCounts.put("abs", new Range(1, 1));
		paramCounts.put("acl2-numberp", new Range(1, 1));
		paramCounts.put("acons", new Range(3, 3));
		paramCounts.put("add-to-set-eq", new Range(2, 2));
		paramCounts.put("add-to-set-eql", new Range(2, 2));
		paramCounts.put("add-to-set-equal", new Range(2, 2));
		paramCounts.put("alistp", new Range(1, 1));
		paramCounts.put("alpha-char-p", new Range(1, 1));
		paramCounts.put("alphorder", new Range(2, 2));
		paramCounts.put("ash", new Range(2, 2));
		paramCounts.put("assert$", new Range(2, 2));
		paramCounts.put("assoc-eq", new Range(2, 2));
		paramCounts.put("assoc-equal", new Range(2, 2));
		paramCounts.put("assoc-keyword", new Range(2, 2));
		paramCounts.put("atom", new Range(1, 1));
		paramCounts.put("atom-listp", new Range(1, 1));
		paramCounts.put("binary-*", new Range(2, 2));
		paramCounts.put("binary-+", new Range(2, 2));
		paramCounts.put("binary-append", new Range(2, 2));
		paramCounts.put("boole$", new Range(3, 3));
		paramCounts.put("booleanp", new Range(1, 1));
		paramCounts.put("butlast", new Range(2, 2));
		paramCounts.put("caaaar", new Range(1, 1));
		paramCounts.put("caaadr", new Range(1, 1));
		paramCounts.put("caaar", new Range(1, 1));
		paramCounts.put("caadar", new Range(1, 1));
		paramCounts.put("caaddr", new Range(1, 1));
		paramCounts.put("caadr", new Range(1, 1));
		paramCounts.put("caar", new Range(1, 1));
		paramCounts.put("cadaar", new Range(1, 1));
		paramCounts.put("cadadr", new Range(1, 1));
		paramCounts.put("cadar", new Range(1, 1));
		paramCounts.put("caddar", new Range(1, 1));
		paramCounts.put("cadddr", new Range(1, 1));
		paramCounts.put("caddr", new Range(1, 1));
		paramCounts.put("cadr", new Range(1, 1));
		paramCounts.put("car", new Range(1, 1));
		paramCounts.put("cdaaar", new Range(1, 1));
		paramCounts.put("cdaadr", new Range(1, 1));
		paramCounts.put("cdaar", new Range(1, 1));
		paramCounts.put("cdadar", new Range(1, 1));
		paramCounts.put("cdaddr", new Range(1, 1));
		paramCounts.put("cdadr", new Range(1, 1));
		paramCounts.put("cdar", new Range(1, 1));
		paramCounts.put("cddaar", new Range(1, 1));
		paramCounts.put("cddadr", new Range(1, 1));
		paramCounts.put("cddar", new Range(1, 1));
		paramCounts.put("cdddar", new Range(1, 1));
		paramCounts.put("cddddr", new Range(1, 1));
		paramCounts.put("cdddr", new Range(1, 1));
		paramCounts.put("cddr", new Range(1, 1));
		paramCounts.put("cdr", new Range(1, 1));
		paramCounts.put("ceiling", new Range(2, 2));
		paramCounts.put("char", new Range(2, 2));
		paramCounts.put("char-code", new Range(1, 1));
		paramCounts.put("char-downcase", new Range(1, 1));
		paramCounts.put("char-equal", new Range(2, 2));
		paramCounts.put("char-upcase", new Range(1, 1));
		paramCounts.put("char<", new Range(2, 2));
		paramCounts.put("char<=", new Range(2, 2));
		paramCounts.put("char>", new Range(2, 2));
		paramCounts.put("char>=", new Range(2, 2));
		paramCounts.put("character-alistp", new Range(1, 1));
		paramCounts.put("character-listp", new Range(1, 1));
		paramCounts.put("characterp", new Range(1, 1));
		paramCounts.put("code-char", new Range(1, 1));
		paramCounts.put("coerce", new Range(2, 2));
		paramCounts.put("comp", new Range(1, 1));
		paramCounts.put("comp-gcl", new Range(1, 1));
		paramCounts.put("complex", new Range(2, 2));
		paramCounts.put("complex-rationalp", new Range(1, 1));
		paramCounts.put("complex/complex-rationalp", new Range(1, 1));
		paramCounts.put("conjugate", new Range(1, 1));
		paramCounts.put("cons", new Range(2, 2));
		paramCounts.put("consp", new Range(1, 1));
		paramCounts.put("cpu-core-count", new Range(1, 1));
		paramCounts.put("delete-assoc-eq", new Range(2, 2));
		paramCounts.put("denominator", new Range(1, 1));
		paramCounts.put("digit-char-p", new Range(1, 2));
		paramCounts.put("digit-to-char", new Range(1, 1));
		paramCounts.put("ec-call", new Range(1, 1));
		paramCounts.put("eighth", new Range(1, 1));
		paramCounts.put("endp", new Range(1, 1));
		paramCounts.put("eq", new Range(2, 2));
		paramCounts.put("eql", new Range(2, 2));
		paramCounts.put("eqlable-alistp", new Range(1, 1));
		paramCounts.put("eqlable-listp", new Range(1, 1));
		paramCounts.put("eqlablep", new Range(1, 1));
		paramCounts.put("equal", new Range(2, 2));
		paramCounts.put("error1", new Range(4, 4));
		paramCounts.put("evenp", new Range(1, 1));
		paramCounts.put("explode-nonnegative-integer", new Range(3, 5));
		paramCounts.put("expt", new Range(2, 2));
		paramCounts.put("fifth", new Range(1, 1));
		paramCounts.put("first", new Range(1, 1));
		paramCounts.put("fix", new Range(1, 1));
		paramCounts.put("fix-true-list", new Range(1, 1));
		paramCounts.put("floor", new Range(2, 2));
		paramCounts.put("fms", new Range(5, 5));
		paramCounts.put("fms!", new Range(5, 5));
		paramCounts.put("fmt", new Range(5, 5));
		paramCounts.put("fmt!", new Range(5, 5));
		paramCounts.put("fmt1", new Range(6, 6));
		paramCounts.put("fmt1!", new Range(6, 6));
		paramCounts.put("fourth", new Range(1, 1));
		paramCounts.put("get-output-stream-string$", new Range(2, 4));
		paramCounts.put("getenv$", new Range(2, 2));
		paramCounts.put("getprop", new Range(5, 5));
		paramCounts.put("good-atom-listp", new Range(1, 1));
		paramCounts.put("hard-error", new Range(3, 3));
		paramCounts.put("identity", new Range(1, 1));
		paramCounts.put("if", new Range(3, 3));
		paramCounts.put("iff", new Range(2, 2));
		paramCounts.put("ifix", new Range(1, 1));
		paramCounts.put("illegal", new Range(3, 3));
		paramCounts.put("imagpart", new Range(1, 1));
		paramCounts.put("implies", new Range(2, 2));
		paramCounts.put("improper-consp", new Range(1, 1));
		paramCounts.put("int=", new Range(2, 2));
		paramCounts.put("integer-length", new Range(1, 1));
		paramCounts.put("integer-listp", new Range(1, 1));
		paramCounts.put("integerp", new Range(1, 1));
		paramCounts.put("intern", new Range(2, 2));
		paramCounts.put("intern$", new Range(2, 2));
		paramCounts.put("intern-in-package-of-symbol", new Range(2, 5));
		paramCounts.put("intersectp-eq", new Range(2, 2));
		paramCounts.put("intersectp-equal", new Range(2, 2));
		paramCounts.put("keywordp", new Range(1, 1));
		paramCounts.put("kwote", new Range(1, 1));
		paramCounts.put("kwote-lst", new Range(1, 1));
		paramCounts.put("last", new Range(1, 1));
		paramCounts.put("len", new Range(1, 1));
		paramCounts.put("length", new Range(1, 1));
		paramCounts.put("lexorder", new Range(2, 2));
		paramCounts.put("listp", new Range(1, 1));
		paramCounts.put("logandc1", new Range(2, 2));
		paramCounts.put("logandc2", new Range(2, 2));
		paramCounts.put("logbitp", new Range(2, 2));
		paramCounts.put("logcount", new Range(1, 1));
		paramCounts.put("lognand", new Range(2, 2));
		paramCounts.put("lognor", new Range(2, 2));
		paramCounts.put("lognot", new Range(1, 1));
		paramCounts.put("logorc1", new Range(2, 2));
		paramCounts.put("logorc2", new Range(2, 2));
		paramCounts.put("logtest", new Range(2, 2));
		paramCounts.put("lower-case-p", new Range(1, 1));
		paramCounts.put("make-ord", new Range(3, 3));
		paramCounts.put("max", new Range(2, 2));
		paramCounts.put("mbe1", new Range(2, 2));
		paramCounts.put("mbt", new Range(1, 1));
		paramCounts.put("member-eq", new Range(2, 2));
		paramCounts.put("member-equal", new Range(2, 2));
		paramCounts.put("min", new Range(2, 2));
		paramCounts.put("minusp", new Range(1, 1));
		paramCounts.put("mod", new Range(2, 2));
		paramCounts.put("mod-expt", new Range(3, 3));
		paramCounts.put("must-be-equal", new Range(2, 2));
		paramCounts.put("mv-list", new Range(2, 2));
		paramCounts.put("mv-nth", new Range(2, 2));
		paramCounts.put("natp", new Range(1, 1));
		paramCounts.put("nfix", new Range(1, 1));
		paramCounts.put("ninth", new Range(1, 1));
		paramCounts.put("no-duplicatesp-eq", new Range(1, 1));
		paramCounts.put("nonnegative-integer-quotient", new Range(2, 4));
		paramCounts.put("not", new Range(1, 1));
		paramCounts.put("nth", new Range(2, 2));
		paramCounts.put("nthcdr", new Range(2, 2));
		paramCounts.put("null", new Range(1, 1));
		paramCounts.put("numerator", new Range(1, 1));
		paramCounts.put("o-finp", new Range(1, 1));
		paramCounts.put("o-first-coeff", new Range(1, 1));
		paramCounts.put("o-first-expt", new Range(1, 1));
		paramCounts.put("o-infp", new Range(1, 1));
		paramCounts.put("o-p", new Range(1, 1));
		paramCounts.put("o-rst", new Range(1, 1));
		paramCounts.put("o<", new Range(2, 2));
		paramCounts.put("o<=", new Range(2, 2));
		paramCounts.put("o>", new Range(2, 2));
		paramCounts.put("o>=", new Range(2, 2));
		paramCounts.put("oddp", new Range(1, 1));
		paramCounts.put("pairlis$", new Range(2, 2));
		paramCounts.put("peek-char$", new Range(2, 2));
		paramCounts.put("pkg-imports", new Range(1, 1));
		paramCounts.put("pkg-witness", new Range(1, 1));
		paramCounts.put("plusp", new Range(1, 1));
		paramCounts.put("position-eq", new Range(2, 2));
		paramCounts.put("position-equal", new Range(2, 2));
		paramCounts.put("posp", new Range(1, 1));
		paramCounts.put("print-object$", new Range(3, 3));
		paramCounts.put("prog2$", new Range(2, 2));
		paramCounts.put("proofs-co", new Range(1, 1));
		paramCounts.put("proper-consp", new Range(1, 1));
		paramCounts.put("put-assoc-eq", new Range(3, 3));
		paramCounts.put("put-assoc-eql", new Range(3, 3));
		paramCounts.put("put-assoc-equal", new Range(3, 3));
		paramCounts.put("putprop", new Range(4, 4));
		paramCounts.put("r-eqlable-alistp", new Range(1, 1));
		paramCounts.put("r-symbol-alistp", new Range(1, 1));
		paramCounts.put("random$", new Range(2, 2));
		paramCounts.put("rassoc-eq", new Range(2, 2));
		paramCounts.put("rassoc-equal", new Range(2, 2));
		paramCounts.put("rational-listp", new Range(1, 1));
		paramCounts.put("rationalp", new Range(1, 1));
		paramCounts.put("read-byte$", new Range(2, 2));
		paramCounts.put("read-char$", new Range(2, 2));
		paramCounts.put("read-object", new Range(2, 2));
		paramCounts.put("real/rationalp", new Range(1, 1));
		paramCounts.put("realfix", new Range(1, 1));
		paramCounts.put("realpart", new Range(1, 1));
		paramCounts.put("rem", new Range(2, 2));
		paramCounts.put("remove-duplicates-eq", new Range(1, 1));
		paramCounts.put("remove-duplicates-equal", new Range(1, 6));
		paramCounts.put("remove-eq", new Range(2, 2));
		paramCounts.put("remove-equal", new Range(2, 2));
		paramCounts.put("remove1-eq", new Range(2, 2));
		paramCounts.put("remove1-equal", new Range(2, 2));
		paramCounts.put("rest", new Range(1, 1));
		paramCounts.put("return-last", new Range(3, 3));
		paramCounts.put("revappend", new Range(2, 2));
		paramCounts.put("reverse", new Range(1, 1));
		paramCounts.put("rfix", new Range(1, 1));
		paramCounts.put("round", new Range(2, 2));
		paramCounts.put("second", new Range(1, 1));
		paramCounts.put("set-difference-eq", new Range(2, 2));
		paramCounts.put("setenv$", new Range(2, 2));
		paramCounts.put("seventh", new Range(1, 1));
		paramCounts.put("signum", new Range(1, 1));
		paramCounts.put("sixth", new Range(1, 1));
		paramCounts.put("standard-char-p", new Range(1, 1));
		paramCounts.put("string", new Range(1, 1));
		paramCounts.put("string-append", new Range(2, 2));
		paramCounts.put("string-downcase", new Range(1, 1));
		paramCounts.put("string-equal", new Range(2, 2));
		paramCounts.put("string-listp", new Range(1, 1));
		paramCounts.put("string-upcase", new Range(1, 1));
		paramCounts.put("string<", new Range(2, 2));
		paramCounts.put("string<=", new Range(2, 2));
		paramCounts.put("string>", new Range(2, 2));
		paramCounts.put("string>=", new Range(2, 2));
		paramCounts.put("stringp", new Range(1, 1));
		paramCounts.put("strip-cars", new Range(1, 1));
		paramCounts.put("strip-cdrs", new Range(1, 1));
		paramCounts.put("sublis", new Range(2, 2));
		paramCounts.put("subseq", new Range(3, 3));
		paramCounts.put("subsetp-eq", new Range(2, 2));
		paramCounts.put("subsetp-equal", new Range(2, 2));
		paramCounts.put("subst", new Range(3, 3));
		paramCounts.put("substitute", new Range(3, 3));
		paramCounts.put("symbol-<", new Range(2, 2));
		paramCounts.put("symbol-alistp", new Range(1, 1));
		paramCounts.put("symbol-listp", new Range(1, 1));
		paramCounts.put("symbol-name", new Range(1, 1));
		paramCounts.put("symbolp", new Range(1, 1));
		paramCounts.put("sys-call-status", new Range(1, 1));
		paramCounts.put("take", new Range(2, 2));
		paramCounts.put("tenth", new Range(1, 1));
		paramCounts.put("the", new Range(2, 2));
		paramCounts.put("third", new Range(1, 1));
		paramCounts.put("true-list-listp", new Range(1, 1));
		paramCounts.put("true-listp", new Range(1, 1));
		paramCounts.put("truncate", new Range(2, 2));
		paramCounts.put("unary--", new Range(1, 1));
		paramCounts.put("unary-/", new Range(1, 1));
		paramCounts.put("union-equal", new Range(2, 2));
		paramCounts.put("update-nth", new Range(3, 3));
		paramCounts.put("upper-case-p", new Range(1, 1));
		paramCounts.put("with-live-state", new Range(1, 1));
		paramCounts.put("write-byte$", new Range(3, 3));
		paramCounts.put("xor", new Range(2, 2));
		paramCounts.put("zerop", new Range(1, 1));
		paramCounts.put("zip", new Range(1, 1));
		paramCounts.put("zp", new Range(1, 1));
		paramCounts.put("zpf", new Range(1, 1));
		paramCounts.put("comp", new Range(1, 1));
		paramCounts.put("defconst", new Range(2, 3));
		paramCounts.put("defdoc", new Range(2, 2));
		paramCounts.put("defpkg", new Range(2, 5));
		paramCounts.put("defproxy", new Range(4, 4));
		paramCounts.put("local", new Range(1, 1));
		paramCounts.put("remove-custom-keyword-hint", new Range(1, 1));
		paramCounts.put("set-body", new Range(2, 2));
		paramCounts.put("show-custom-keyword-hint-expansion", new Range(1, 1));
		paramCounts.put("table", new Range(1, 5));
		paramCounts.put("unmemoize", new Range(1, 1));
		paramCounts.put("add-binop", new Range(2, 2));
		paramCounts.put("add-dive-into-macro", new Range(2, 2));
		paramCounts.put("add-include-book-dir", new Range(2, 2));
		paramCounts.put("add-macro-alias", new Range(2, 2));
		paramCounts.put("add-nth-alias", new Range(2, 2));
		paramCounts.put("binop-table", new Range(1, 1));
		paramCounts.put("delete-include-book-dir", new Range(1, 1));
		paramCounts.put("remove-binop", new Range(1, 1));
		paramCounts.put("remove-default-hints", new Range(1, 1));
		paramCounts.put("remove-default-hints!", new Range(1, 1));
		paramCounts.put("remove-dive-into-macro", new Range(1, 1));
		paramCounts.put("remove-macro-alias", new Range(1, 1));
		paramCounts.put("remove-nth-alias", new Range(1, 1));
		paramCounts.put("remove-override-hints", new Range(1, 1));
		paramCounts.put("remove-override-hints!", new Range(1, 1));
		paramCounts.put("set-backchain-limit", new Range(1, 1));
		paramCounts.put("set-bogus-defun-hints-ok", new Range(1, 1));
		paramCounts.put("set-bogus-mutual-recursion-ok", new Range(1, 1));
		paramCounts.put("set-case-split-limitations", new Range(1, 1));
		paramCounts.put("set-checkpoint-summary-limit", new Range(1, 1));
		paramCounts.put("set-compile-fns", new Range(1, 1));
		paramCounts.put("set-debugger-enable", new Range(1, 1));
		paramCounts.put("set-default-backchain-limit", new Range(1, 1));
		paramCounts.put("set-default-hints", new Range(1, 1));
		paramCounts.put("set-default-hints!", new Range(1, 1));
		paramCounts.put("set-deferred-ttag-notes", new Range(2, 6));
		paramCounts.put("set-enforce-redundancy", new Range(1, 1));
		paramCounts.put("set-gag-mode", new Range(1, 1));
		paramCounts.put("set-guard-checking", new Range(1, 1));
		paramCounts.put("set-ignore-doc-string-error", new Range(1, 1));
		paramCounts.put("set-ignore-ok", new Range(1, 1));
		paramCounts.put("set-inhibit-output-lst", new Range(1, 1));
		paramCounts.put("set-inhibited-summary-types", new Range(1, 1));
		paramCounts.put("set-invisible-fns-table", new Range(1, 1));
		paramCounts.put("set-irrelevant-formals-ok", new Range(1, 1));
		paramCounts.put("set-ld-keyword-aliases", new Range(2, 6));
		paramCounts.put("set-ld-redefinition-action", new Range(2, 5));
		paramCounts.put("set-ld-skip-proofs", new Range(2, 2));
		paramCounts.put("set-let*-abstraction", new Range(1, 1));
		paramCounts.put("set-let*-abstractionp", new Range(1, 1));
		paramCounts.put("set-match-free-default", new Range(1, 1));
		paramCounts.put("set-match-free-error", new Range(1, 1));
		paramCounts.put("set-measure-function", new Range(1, 1));
		paramCounts.put("set-non-linear", new Range(1, 1));
		paramCounts.put("set-non-linearp", new Range(1, 1));
		paramCounts.put("set-nu-rewriter-mode", new Range(1, 1));
		paramCounts.put("set-override-hints", new Range(1, 1));
		paramCounts.put("set-override-hints!", new Range(1, 1));
		paramCounts.put("set-print-clause-ids", new Range(1, 1));
		paramCounts.put("set-prover-step-limit", new Range(1, 1));
		paramCounts.put("set-raw-mode", new Range(1, 1));
		paramCounts.put("set-raw-proof-format", new Range(1, 1));
		paramCounts.put("set-rewrite-stack-limit", new Range(1, 1));
		paramCounts.put("set-ruler-extenders", new Range(1, 1));
		paramCounts.put("set-rw-cache-state", new Range(1, 1));
		paramCounts.put("set-rw-cache-state!", new Range(1, 1));
		paramCounts.put("set-saved-output", new Range(2, 2));
		paramCounts.put("set-state-ok", new Range(1, 1));
		paramCounts.put("set-tainted-ok", new Range(1, 1));
		paramCounts.put("set-tainted-okp", new Range(1, 1));
		paramCounts.put("set-verify-guards-eagerness", new Range(1, 1));
		paramCounts.put("set-waterfall-parallelism", new Range(1, 2));
		paramCounts.put("set-waterfall-printing", new Range(1, 1));
		paramCounts.put("set-well-founded-relation", new Range(1, 1));
		paramCounts.put("set-write-acl2x", new Range(2, 2));
		paramCounts.put("with-guard-checking", new Range(2, 2));
	}

	public class ParseToken {
		public int offset;
		public int line;
		public String name;
		public List<String> params = new ArrayList<String>();
		public Set<String> vars = new HashSet<String>();
	}

	public class Acl2ParserNotice extends DefaultParserNotice {
		public Acl2ParserNotice(Acl2Parser parser, String msg, int line, int offs, int len, int level) {
			super(parser, msg, line, offs, len);
			//System.out.println("ERROR on line " + line + ": " + msg);
			setLevel(level);
		}
		public Acl2ParserNotice(Acl2Parser parser, String msg,
				ParseToken top, int end) {
			this(parser, msg, top.line, top.offset, end - top.offset, ERROR);
		}
		public Acl2ParserNotice(Acl2Parser parser, String msg, int line,
				Token token, int level) {
			this(parser, msg, line, token.offset, token.textCount, level);
		}
		@Override
		public boolean getShowInEditor() {
			return getLevel() != INFO;
		}
	}
	
	@Override
	public ParseResult parse(RSyntaxDocument doc, String style /* ignored */) {
		DefaultParseResult result = new DefaultParseResult(this);
		int lines = doc.getDefaultRootElement().getElementCount();
		result.setParsedLines(0, lines);
		if (!Prefs.showErrors.get()) {
			for (ParseListener pl : parseListeners) {
				pl.wasParsed();
			}
			return result;
		}
		functions = new HashSet<String>();
		macros = new HashSet<String>(Arrays.asList(new String [] {
				"declare", "include-book", "defproperty", "defttag"
		}));
		constants = new HashSet<String>();
		constants.add("state");
		Stack<ParseToken> s = new Stack<ParseToken>();
		Token token;
		for (int line = 0; line < lines; line++) {
			token = doc.getTokenListForLine(line);
			while (token != null && token.isPaintable()) {
				ParseToken top = (s.empty() ? null : s.peek());
				String tokenName = token.getLexeme().toLowerCase();
				if (top != null && top.name != null && !token.isWhitespace() &&
						!token.isComment() && !token.isSingleChar(')') &&
						!token.isSingleChar('`') && !token.isSingleChar(',') &&
						!token.isSingleChar('\'')) {
					// In a parameter position.
					top.params.add(token.getLexeme());
					if (top.name.equals("defun") && top.params.size() == 1) {
						if (!macros.contains(tokenName) && !functions.contains(tokenName)) {
							functions.add(tokenName);
						} else {
							result.addNotice(new Acl2ParserNotice(this,
									"A function with this name is already defined", line, token,
									ParserNotice.ERROR));
						}
					} else if ((top.name.equals("defmacro") || top.name.equals("defabbrev")) &&
							top.params.size() == 1) {
						if (!functions.contains(tokenName) && !macros.contains(tokenName)) {
							macros.add(tokenName);
						} else {
							result.addNotice(new Acl2ParserNotice(this,
									"A function with this name is already defined", line, token,
									ParserNotice.ERROR));
						}
					} else if (top.name.equals("defconst") && top.params.size() == 1) {
						if (!tokenName.startsWith("*") || !tokenName.endsWith("*")) {
							Main.userData.addParseError("constNames");
							result.addNotice(new Acl2ParserNotice(this, 
									"Constant names must begin and end with *.", line, token,
									ParserNotice.ERROR));
						} else {
							if (!constants.contains(tokenName)) {
								constants.add(tokenName);
							} else {
								result.addNotice(new Acl2ParserNotice(this,
										"A constant with this name is already defined", line, token,
										ParserNotice.ERROR));
							}
						}
					}
				}
				ParseToken parent = s.size() <= 1 ? null : s.get(s.size() - 2);
				ParseToken grandparent = s.size() <= 2 ? null : s.get(s.size() - 3);
				boolean isVariableOfParent = parent != null && parent.name != null &&
						(parent.name.equals("defun") && parent.params.size() == 2 ||
						 parent.name.equals("mv-let") && parent.params.size() == 1);
				boolean isVariableOfGrandparent = (grandparent != null && grandparent.name != null &&
						((grandparent.name.equals("let") || grandparent.name.equals("let*")) &&
								grandparent.params.size() == 1 && top != null && top.params.size() == 0));
				if (isVariableOfParent || isVariableOfGrandparent) {
					if (token.type == Token.IDENTIFIER) {
						if (parent != null && isVariableOfParent) {
							parent.vars.add(token.getLexeme());
						} else if (grandparent != null && isVariableOfGrandparent) {
							grandparent.vars.add(token.getLexeme());
						}
					} else if (token.type != Token.WHITESPACE && !token.isSingleChar(')')) {
						Main.userData.addParseError("expectedVariableName");
						result.addNotice(new Acl2ParserNotice(this, "Expected a variable name",
								line, token, ParserNotice.ERROR));
					}
				}
				boolean isIgnoredBecauseMacro = false;
				boolean isThm = false;
				Set<String> vars = new HashSet<String>();
				for (ParseToken ancestor : s) {
					isIgnoredBecauseMacro |= macros.contains(ancestor.name);
					vars.addAll(ancestor.vars);
					isThm |= ancestor.name != null && (ancestor.name.equals("thm") ||
							ancestor.name.equals("defthm") || ancestor.name.equals("defthmd"));
				}
				boolean isIgnoredBecauseParent = parent != null && parent.name != null &&
						(parent.name.equals("defun") && parent.params.size() == 2 ||
						parent.name.equals("defmacro") && parent.params.size() == 2 ||
						parent.name.equals("mv-let") && parent.params.size() == 1 ||
						parent.name.equals("cond") /* any parameter */ ||
						parent.name.equals("case") /* any parameter */);
				boolean isIgnoredBecauseCurrent = top != null && top.name != null &&
						(top.name.equals("defun") && top.params.size() == 1 ||
						 top.name.equals("defmacro") && top.params.size() == 1 ||
						 top.name.equals("assign") && top.params.size() == 1 ||
						 top.name.equals("@") && top.params.size() == 1);
				boolean isIgnored = isIgnoredBecauseMacro || isIgnoredBecauseParent || isIgnoredBecauseCurrent ||
						(top != null && grandparent != null && grandparent.name != null &&
						(grandparent.name.equals("let") && grandparent.params.size() == 1 && top.params.size() == 0 ||
						grandparent.name.equals("let*") && grandparent.params.size() == 1 && top.params.size() == 0));
				if (token.isSingleChar('(')) {
					if (top != null && top.name == null) top.name = "";
					s.push(new ParseToken());
					s.peek().line = line;
					s.peek().offset = token.offset;
				} else if (token.isSingleChar(')')) {
					if (top == null) {
						Main.userData.addParseError("UnmatchedCloseParen");
						result.addNotice(new Acl2ParserNotice(this, "Unmatched )", line, token.offset, 1,
								ParserNotice.ERROR));
					} else {
						Range range = paramCounts.get(top.name);
						if (range != null && (top.params.size() < range.lower || top.params.size() > range.upper)) {
							String msg;
							if (range.lower == range.upper) {
								msg = "<html><b>" + htmlEncode(top.name) + "</b> expects "
										+ range.lower + " parameter" +
										(range.lower == 1 ? "" : "s")  + ".</html>";
							} else {
								msg = "<html><b>" + htmlEncode(top.name) + "</b> expects between "
										+ range.lower + " and " + range.upper + " parameters.</html>";							
							}
							Main.userData.addParseError("numOfParams");
							result.addNotice(new Acl2ParserNotice(this, msg, top, token.offset + 1));
						}
						s.pop();
						if (top.name != null && top.name.equals("include-book")) {
							String bookName = top.params.get(0);
							int dirLoc = top.params.indexOf(":dir") + 1;
							File dir;
							String dirKey = "";
							if (dirLoc == 0) {
								dir = workingDir;
							} else {
								dirKey = top.params.get(dirLoc);
								if (dirKey.equals(":system")) {
									dir = new File(getAcl2Dir(), "books");
								} else if (dirKey.equals(":teachpacks")) {
									dir = new File(getAcl2Dir(), "dracula");
								} else {
									Main.userData.addParseError("UnrecongizedBookLocation");
									result.addNotice(new Acl2ParserNotice(this,
											"Unrecognized book location: " + dirKey, top,
											token.offset + 1));
									dir = null;
								}
							}
							if (Main.WIN) {
								bookName.replaceAll("\\\\/", "\\");
							}
							File book = new File(dir, bookName.substring(1, bookName.length() - 1) + ".lisp");
							CacheSets bookCache = null;
							long mtime = book.lastModified();
							if (dirKey.equals(":system")) {
								mtime = Long.MAX_VALUE;
							}
							CacheKey key = new CacheKey(book, mtime);
							if (cache.containsKey(key)) {
								bookCache = cache.get(key);
							} else {
								try {
									System.out.println("Book exists? " + book.exists());
									bookCache = parseBook(book, getAcl2Dir(), cache);
									cache.put(key, bookCache);
								} catch (FileNotFoundException e) {
									Main.userData.addParseError("BookNotFound");
									result.addNotice(new Acl2ParserNotice(this,
											"File could not be found.", top, token.offset + 1));
								} catch (BadLocationException e) { }
							}
							if (bookCache != null) {
								functions.addAll(bookCache.functions);
								macros.addAll(bookCache.macros);
								constants.addAll(bookCache.constants);
							}
						}
					}
				} else if (top != null && top.name == null &&
						   !token.isComment() &&
						   !token.isWhitespace()) {
					// This token is at the beginning of an s expression
					top.name = token.getLexeme().toLowerCase();
					if (token.type != Token.RESERVED_WORD &&
							token.type != Token.RESERVED_WORD_2 &&
							!functions.contains(top.name) &&
							!macros.contains(top.name) &&
							!isIgnored) {
						Main.userData.addParseError("undefinedCallable");
						result.addNotice(new Acl2ParserNotice(this, "<html><b>" +
							htmlEncode(top.name) + "</b> is undefined.</html>",
							line, token, ParserNotice.ERROR));
					}
					if (token.type == Token.RESERVED_WORD ||
							token.type == Token.RESERVED_WORD_2) {
						// TODO: Make these more noticeable?
						Map<String, String> docs = Main.cache.getDocs();
						String upperToken = token.getLexeme().toUpperCase();
						if (docs.containsKey(upperToken)) {
							String modKey = Main.OSX ? "\u2325\u2318" : "Ctrl + Alt + ";
							String msg = "<html>" + docs.get(upperToken) + "<br><font " +
									"color=\"gray\" size=\"2\">" + modKey +
									"L for more.</font></html>";
							result.addNotice(new Acl2ParserNotice(this,
									msg, line, token, ParserNotice.INFO));
						}
					}
				} else if (!isThm && !isIgnored && (token.type == Token.IDENTIFIER ||
						token.type == Token.RESERVED_WORD || token.type == Token.RESERVED_WORD_2)
						&& !constants.contains(token.getLexeme()) &&
						!vars.contains(token.getLexeme())) {
					Main.userData.addParseError("undeclaredVariable");
					result.addNotice(new Acl2ParserNotice(this, token.getLexeme() +
							" is undeclared.",
							line, token, ParserNotice.ERROR));
				}
				token = token.getNextToken();
			}
		}
		for (ParseListener pl : parseListeners) {
			pl.wasParsed();
		}
		return result;
	}
	
	public static CacheSets parseBook(File book, File acl2Dir, Map<CacheKey, CacheSets> cache)
			throws FileNotFoundException, BadLocationException {
		CacheSets bookCache;
		Scanner bookScanner = new Scanner(book);
		bookScanner.useDelimiter("\\Z");
		String bookContents = bookScanner.next();
		bookScanner.close();
		logger.info("PARSING: " + book);
		book.lastModified();
		Acl2Parser bookParser = new Acl2Parser(book.getParentFile(), acl2Dir);
		bookParser.cache = cache;
		RSyntaxDocument bookDoc = new IdeDocument(null);
		bookDoc.insertString(0, bookContents, null);
		bookParser.parse(bookDoc, null);
		bookCache = new CacheSets();
		bookCache.functions = bookParser.functions;
		bookCache.constants = bookParser.constants;
		bookCache.macros = bookParser.macros;
		return bookCache;
	}

	private static String htmlEncode(String name) {
		return name.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	public void addParseListener(ParseListener parseListener) {
		parseListeners.add(parseListener);
	}

	public File getAcl2Dir() {
		return acl2Dir;
	}

	public void setAcl2Dir(File acl2Dir) {
		this.acl2Dir = acl2Dir;
	}

}
