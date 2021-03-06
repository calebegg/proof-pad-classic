/*
 * Modified substantially 2/26/2012 for ACL2.
 *
 * 11/13/2004
 *
 * LispTokenMaker.java - Scanner for the Lisp programming language.
 * Copyright (C) 2004 Robert Futrell
 * robert_futrell at users.sourceforge.net
 * http://fifesoft.com/rsyntaxtextarea
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307 USA.
 */
package org.proofpad;

import java.io.*;
import java.util.*;
import javax.swing.text.Segment;

import org.fife.ui.rsyntaxtextarea.*;
import org.fife.util.DynamicIntArray;

@SuppressWarnings("all")
%%

%public
%class Acl2TokenMaker
%extends AbstractJFlexTokenMaker
%unicode
%ignorecase
%type org.fife.ui.rsyntaxtextarea.Token


%{

	private int symbolParenLevel = 0;
	// This is basically an imitation of the token type array in RSyntaxDocument, but the logic has
	// to go here because it's not possible to override/recreate the method in the correct class
	// (IdeDocument).
	private TreeMap<Integer, Integer> parenLevelForOffset = new TreeMap<Integer, Integer>();


	public Acl2TokenMaker() {
	}

	private void addHyperlinkToken(int start, int end, int tokenType) {
		int so = start + offsetShift;
		addToken(zzBuffer, start,end, tokenType, so, true);
	}

	private void addToken(int tokenType) {
		addToken(zzStartRead, zzMarkedPos-1, tokenType);
	}

	private void addToken(int start, int end, int tokenType) {
		int so = start + offsetShift;
		addToken(zzBuffer, start,end, tokenType, so, false);
	}

	public void addToken(char[] array, int start, int end, int tokenType,
						int startOffset, boolean hyperlink) {
		super.addToken(array, start,end, tokenType, startOffset, hyperlink);
		zzStartRead = zzMarkedPos;
		parenLevelForOffset.put(startOffset, symbolParenLevel);		
	}

	public String[] getLineCommentStartAndEnd() {
		return new String[] { ";", null };
	}
	
	public Token getTokenList(Segment text, int initialTokenType, int startOffset) {
		resetTokenList();
		this.offsetShift = -text.offset + startOffset;

		// Start off in the proper state.
		int state = Token.NULL;
		switch (initialTokenType) {
			case Token.COMMENT_MULTILINE:
				state = MLC;
				start = text.offset;
				break;
			case Token.LITERAL_STRING_DOUBLE_QUOTE:
				state = STRING;
				start = text.offset;
				break;
			default:
				state = Token.NULL;
		}
		

		Map.Entry<Integer, Integer> entry = parenLevelForOffset.floorEntry(startOffset - 1);
		if (entry != null && entry.getValue() > 0) {
			symbolParenLevel = entry.getValue();
			if (symbolParenLevel > 0) {
				state = SYMBOL_PAREN;
				start = text.offset;
			}
		} else {
		    symbolParenLevel = 0;
		}
		
		for (Iterator<Map.Entry<Integer, Integer>> ii = parenLevelForOffset.entrySet().iterator();
				ii.hasNext();) {
			Map.Entry<Integer, Integer> e = ii.next();
			if (e.getKey() > startOffset) {
				ii.remove();
			}
		}

		s = text;
		try {
			yyreset(zzReader);
			yybegin(state);
			return yylex();
		} catch (IOException ioe) {
			ioe.printStackTrace();
			return new DefaultToken();
		}
	}

	private boolean zzRefill() throws java.io.IOException {
		return zzCurrentPos>=s.offset+s.count;
	}

	public final void yyreset(java.io.Reader reader) throws java.io.IOException {
		// 's' has been updated.
		zzBuffer = s.array;
		zzStartRead = s.offset;
		zzEndRead = zzStartRead + s.count - 1;
		zzCurrentPos = zzMarkedPos = s.offset;
		zzLexicalState = YYINITIAL;
		zzReader = reader;
		zzAtBOL  = true;
		zzAtEOF  = false;
	}

%}

Letter						= [A-Za-z]
LetterOrUnderscore			= ({Letter}|"_")
NonzeroDigit				= [1-9]
Digit						= ("0"|{NonzeroDigit})
HexDigit					= [0-9A-Fa-f]
IdentifierStart				= ([^\t\f\r\n\ \(\)\;\|\.&\"\':,`])
IdentifierPart				= ([^\t\f\r\n\ \(\)\;\|\.\"\':,`])
Identifier					= ({IdentifierStart}{IdentifierPart}*)|"|"[^|]*"|"

LineTerminator				= (\n)
WhiteSpace					= ([ \t\f])

MLCBegin					= "#|"
MLCEnd						= "|#"
LineCommentBegin			= ";"

IntegerLiteral				= (-?{Digit}+)
HexIntegerLiteral			= (#[Xx]?-?{HexDigit}+)
RationalLiteral				= ({IntegerLiteral}"/"{Digit}+)
HexRationalLiteral			= ({HexIntegerLiteral}"/"{HexDigit}+)
CharacterLiteral			= "#\\"(.|"Space"|"Tab"|"Newline"|"Page"|"Rubout")

Symbol						= ("'"{Identifier}|":"{Identifier})

Separator					= ([\(\)])


URLGenDelim				= ([:\/\?#\[\]@])
URLSubDelim				= ([\!\$&'\(\)\*\+,;=])
URLUnreserved			= ({LetterOrUnderscore}|{Digit}|[\-\.\~])
URLCharacter			= ({URLGenDelim}|{URLSubDelim}|{URLUnreserved}|[%])
URLCharacters			= ({URLCharacter}*)
URLEndCharacter			= ([\/\$]|{Letter}|{Digit})
URL						= (((https?|f(tp|ile))"://"|"www.")({URLCharacters}{URLEndCharacter})?)


%state STRING
%state MLC
%state EOL_COMMENT
%state SYMBOL_PAREN

%%

<YYINITIAL> {
	"IN-PACKAGE" |
	"THM" |
	"AREF1" |
    "AREF2" |
    "ASET1" |
    "ASET2" |
    "COMPRESS1" |
    "COMPRESS2" |
    "ARRAY1P" |
    "ARRAY2P" |
    "DEFAULT" |
    "DIMENSIONS" |
    "FLUSH-COMPRESS" |
    "HEADER" |
    "MAXIMUM-LENGTH" |
    "ASSIGN" |
    "@" |
	"*" |
	"+" |
	"-" |
	"/" |
	"/=" |
	"1+" |
	"1-" |
	"<" |
	"<=" |
	"=" |
	">" |
	">=" |
	"ABS" |
	"ACL2-CUSTOMIZATION" |
	"ACL2-NUMBERP" |
	"ACL2-USER" |
	"ACONS" |
	"ADD-TO-SET" |
	"ADD-TO-SET-EQ" |
	"ADD-TO-SET-EQL" |
	"ADD-TO-SET-EQUAL" |
	"ALISTP" |
	"ALLOCATE-FIXNUM-RANGE" |
	"ALPHA-CHAR-P" |
	"ALPHORDER" |
	"AND" |
	"APPEND" |
	"ARRAYS" |
	"ASH" |
	"ASSERT$" |
	"ASSOC" |
	"ASSOC-EQ" |
	"ASSOC-EQUAL" |
	"ASSOC-KEYWORD" |
	"ASSOC-STRING-EQUAL" |
	"ATOM" |
	"ATOM-LISTP" |
	"BINARY-*" |
	"BINARY-+" |
	"BINARY-APPEND" |
	"BOOLE$" |
	"BOOLEANP" |
	"BUTLAST" |
	"CAAAAR" |
	"CAAADR" |
	"CAAAR" |
	"CAADAR" |
	"CAADDR" |
	"CAADR" |
	"CAAR" |
	"CADAAR" |
	"CADADR" |
	"CADAR" |
	"CADDAR" |
	"CADDDR" |
	"CADDR" |
	"CADR" |
	"CAR" |
	"CASE" |
	"CASE-MATCH" |
	"CDAAAR" |
	"CDAADR" |
	"CDAAR" |
	"CDADAR" |
	"CDADDR" |
	"CDADR" |
	"CDAR" |
	"CDDAAR" |
	"CDDADR" |
	"CDDAR" |
	"CDDDAR" |
	"CDDDDR" |
	"CDDDR" |
	"CDDR" |
	"CDR" |
	"CEILING" |
	"CERTIFY-BOOK" |
	"CHAR" |
	"CHAR-CODE" |
	"CHAR-DOWNCASE" |
	"CHAR-EQUAL" |
	"CHAR-UPCASE" |
	"CHAR<" |
	"CHAR<=" |
	"CHAR>" |
	"CHAR>=" |
	"CHARACTER-ALISTP" |
	"CHARACTER-LISTP" |
	"CHARACTERP" |
	"CHARACTERS" |
	"CLOSE-INPUT-CHANNEL" |
	"CLOSE-OUTPUT-CHANNEL" |
	"CODE-CHAR" |
	"COERCE" |
	"COMP" |
	"COMP-GCL" |
	"COMPILATION" |
	"COMPLEX" |
	"COMPLEX-RATIONALP" |
	"COMPLEX/COMPLEX-RATIONALP" |
	"CONCATENATE" |
	"COND" |
	"CONJUGATE" |
	"CONS" |
	"CONSP" |
	"COUNT" |
	"CPU-CORE-COUNT" |
	"CW" |
	"CW!" |
	"DECLARE" |
	"DEFMACRO-LAST" |
	"DELETE-ASSOC" |
	"DELETE-ASSOC-EQ" |
	"DELETE-ASSOC-EQUAL" |
	"DENOMINATOR" |
	"DIGIT-CHAR-P" |
	"DIGIT-TO-CHAR" |
	"E0-ORD-<" |
	"E0-ORDINALP" |
	"EC-CALL" |
	"EIGHTH" |
	"ENDP" |
	"EQ" |
	"EQL" |
	"EQLABLE-ALISTP" |
	"EQLABLE-LISTP" |
	"EQLABLEP" |
	"EQUAL" |
	"EQUALITY-VARIANTS" |
	"ER" |
	"ER-PROGN" |
	"ERROR1" |
	"EVENP" |
	"EXPLODE-NONNEGATIVE-INTEGER" |
	"EXPT" |
	"FIFTH" |
	"FIRST" |
	"FIX" |
	"FIX-TRUE-LIST" |
	"FLET" |
	"FLOOR" |
	"FMS" |
	"FMS!" |
	"FMS!-TO-STRING" |
	"FMS-TO-STRING" |
	"FMT" |
	"FMT!" |
	"FMT!-TO-STRING" |
	"FMT-TO-COMMENT-WINDOW" |
	"FMT-TO-STRING" |
	"FMT1" |
	"FMT1!" |
	"FMT1!-TO-STRING" |
	"FMT1-TO-STRING" |
	"FOURTH" |
	"GET-OUTPUT-STREAM-STRING$" |
	"GETENV$" |
	"GETPROP" |
	"GOOD-ATOM-LISTP" |
	"HARD-ERROR" |
	"IDENTITY" |
	"IF" |
	"IFF" |
	"IFIX" |
	"IGNORABLE" |
	"IGNORE" |
	"ILLEGAL" |
	"IMAGPART" |
	"IMPLIES" |
	"IMPROPER-CONSP" |
	"INT=" |
	"INTEGER-LENGTH" |
	"INTEGER-LISTP" |
	"INTEGERP" |
	"INTERN" |
	"INTERN$" |
	"INTERN-IN-PACKAGE-OF-SYMBOL" |
	"INTERSECTION$" |
	"INTERSECTION-EQ" |
	"INTERSECTION-EQUAL" |
	"INTERSECTP" |
	"INTERSECTP-EQ" |
	"INTERSECTP-EQUAL" |
	"IRRELEVANT-FORMALS" |
	"KEYWORD-VALUE-LISTP" |
	"KEYWORDP" |
	"KWOTE" |
	"KWOTE-LST" |
	"LAST" |
	"LD" |
	"LEN" |
	"LENGTH" |
	"LET" |
	"LET*" |
	"LEXORDER" |
	"LIST" |
	"LIST*" |
	"LISTP" |
	"LOGAND" |
	"LOGANDC1" |
	"LOGANDC2" |
	"LOGBITP" |
	"LOGCOUNT" |
	"LOGEQV" |
	"LOGIOR" |
	"LOGNAND" |
	"LOGNOR" |
	"LOGNOT" |
	"LOGORC1" |
	"LOGORC2" |
	"LOGTEST" |
	"LOGXOR" |
	"LOWER-CASE-P" |
	"MAKE-CHARACTER-LIST" |
	"MAKE-LIST" |
	"MAKE-ORD" |
	"MAX" |
	"MBE" |
	"MBE1" |
	"MBT" |
	"MEMBER" |
	"MEMBER-EQ" |
	"MEMBER-EQUAL" |
	"MIN" |
	"MINUSP" |
	"MOD" |
	"MOD-EXPT" |
	"MUST-BE-EQUAL" |
	"MUTUAL-RECURSION" |
	"MV" |
	"MV-LET" |
	"MV-LIST" |
	"MV-NTH" |
	"MV?" |
	"MV?-LET" |
	"NATP" |
	"NFIX" |
	"NINTH" |
	"NO-DUPLICATESP" |
	"NO-DUPLICATESP-EQ" |
	"NO-DUPLICATESP-EQUAL" |
	"NONNEGATIVE-INTEGER-QUOTIENT" |
	"NOT" |
	"NTH" |
	"NTHCDR" |
	"NULL" |
	"NUMERATOR" |
	"O-FINP" |
	"O-FIRST-COEFF" |
	"O-FIRST-EXPT" |
	"O-INFP" |
	"O-P" |
	"O-RST" |
	"O<" |
	"O<=" |
	"O>" |
	"O>=" |
	"OBSERVATION" |
	"OBSERVATION-CW" |
	"ODDP" |
	"OPEN-INPUT-CHANNEL" |
	"OPEN-INPUT-CHANNEL-P" |
	"OPEN-OUTPUT-CHANNEL" |
	"OPEN-OUTPUT-CHANNEL-P" |
	"OPTIMIZE" |
	"OR" |
	"PAIRLIS" |
	"PAIRLIS$" |
	"PEEK-CHAR$" |
	"PKG-IMPORTS" |
	"PKG-WITNESS" |
	"PLUSP" |
	"POSITION" |
	"POSITION-EQ" |
	"POSITION-EQUAL" |
	"POSP" |
	"PPROGN" |
	"PRINT-OBJECT$" |
	"PROG2$" |
	"PROGN$" |
	"PROOFS-CO" |
	"PROPER-CONSP" |
	"PUT-ASSOC" |
	"PUT-ASSOC-EQ" |
	"PUT-ASSOC-EQL" |
	"PUT-ASSOC-EQUAL" |
	"PUTPROP" |
	"QUOTE" |
	"R-EQLABLE-ALISTP" |
	"R-SYMBOL-ALISTP" |
	"RANDOM$" |
	"RASSOC" |
	"RASSOC-EQ" |
	"RASSOC-EQUAL" |
	"RATIONAL-LISTP" |
	"RATIONALP" |
	"READ-BYTE$" |
	"READ-CHAR$" |
	"READ-OBJECT" |
	"REAL/RATIONALP" |
	"REALFIX" |
	"REALPART" |
	"REM" |
	"REMOVE" |
	"REMOVE-DUPLICATES" |
	"REMOVE-DUPLICATES-EQ" |
	"REMOVE-DUPLICATES-EQUAL" |
	"REMOVE-EQ" |
	"REMOVE-EQUAL" |
	"REMOVE1" |
	"REMOVE1-EQ" |
	"REMOVE1-EQUAL" |
	"REST" |
	"RETURN-LAST" |
	"REVAPPEND" |
	"REVERSE" |
	"RFIX" |
	"ROUND" |
	"SEARCH" |
	"SECOND" |
	"SET-DIFFERENCE$" |
	"SET-DIFFERENCE-EQ" |
	"SET-DIFFERENCE-EQUAL" |
	"SETENV$" |
	"SEVENTH" |
	"SIGNUM" |
	"SIXTH" |
	"STANDARD-CHAR-LISTP" |
	"STANDARD-CHAR-P" |
	"STANDARD-STRING-ALISTP" |
	"STRING" |
	"STRING-APPEND" |
	"STRING-DOWNCASE" |
	"STRING-EQUAL" |
	"STRING-LISTP" |
	"STRING-UPCASE" |
	"STRING<" |
	"STRING<=" |
	"STRING>" |
	"STRING>=" |
	"STRINGP" |
	"STRIP-CARS" |
	"STRIP-CDRS" |
	"SUBLIS" |
	"SUBSEQ" |
	"SUBSETP" |
	"SUBSETP-EQ" |
	"SUBSETP-EQUAL" |
	"SUBST" |
	"SUBSTITUTE" |
	"SYMBOL-<" |
	"SYMBOL-ALISTP" |
	"SYMBOL-LISTP" |
	"SYMBOL-NAME" |
	"SYMBOL-PACKAGE-NAME" |
	"SYMBOLP" |
	"SYS-CALL" |
	"SYS-CALL-STATUS" |
	"TAKE" |
	"TENTH" |
	"THE" |
	"THIRD" |
	"TIME$" |
	"TRACE" |
	"TRUE-LIST-LISTP" |
	"TRUE-LISTP" |
	"TRUNCATE" |
	"TYPE" |
	"TYPE-SPEC" |
	"UNARY--" |
	"UNARY-/" |
	"UNION$" |
	"UNION-EQ" |
	"UNION-EQUAL" |
	"UPDATE-NTH" |
	"UPPER-CASE-P" |
	"WITH-LIVE-STATE" |
	"WRITE-BYTE$" |
	"XOR" |
	"ZEROP" |
	"ZIP" |
	"ZP" |
	"ZPF"		{ addToken(Token.RESERVED_WORD); }

	"ADD-CUSTOM-KEYWORD-HINT" |
	"ASSERT-EVENT" |
	"COMP" |
	"DEFABBREV" |
	"DEFATTACH" |
	"DEFAXIOM" |
	"DEFCHOOSE" |
	"DEFCONG" |
	"DEFCONST" |
	"DEFDOC" |
	"DEFEQUIV" |
	"DEFEVALUATOR" |
	"DEFEXEC" |
	"DEFINE-TRUSTED-CLAUSE-PROCESSOR" |
	"DEFLABEL" |
	"DEFMACRO" |
	"DEFPKG" |
	"DEFPROXY" |
	"DEFPUN" |
	"DEFREFINEMENT" |
	"DEFSTOBJ" |
	"DEFSTUB" |
	"DEFTHEORY" |
	"DEFTHM" |
	"DEFTHMD" |
	"DEFTTAG" |
	"DEFUN" |
	"DEFUN-NX" |
	"DEFUN-SK" |
	"DEFUND" |
	"ENCAPSULATE" |
	"EVISC-TABLE" |
	"IN-ARITHMETIC-THEORY" |
	"IN-THEORY" |
	"INCLUDE-BOOK" |
	"LOCAL" |
	"MAKE-EVENT" |
	"MEMOIZE" |
	"MUTUAL-RECURSION" |
	"PROFILE" |
	"PROGN" |
	"PROGN!" |
	"REDO-FLAT" |
	"REMOVE-CUSTOM-KEYWORD-HINT" |
	"SET-BODY" |
	"SHOW-CUSTOM-KEYWORD-HINT-EXPANSION" |
	"TABLE" |
	"THEORY-INVARIANT" |
	"UNMEMOIZE" |
	"VALUE-TRIPLE" |
	"VERIFY-GUARDS" |
	"VERIFY-TERMINATION" |
	"ADD-BINOP" |
	"ADD-DEFAULT-HINTS" |
	"ADD-DEFAULT-HINTS!" |
	"ADD-DIVE-INTO-MACRO" |
	"ADD-INCLUDE-BOOK-DIR" |
	"ADD-INVISIBLE-FNS" |
	"ADD-MACRO-ALIAS" |
	"ADD-MATCH-FREE-OVERRIDE" |
	"ADD-NTH-ALIAS" |
	"ADD-OVERRIDE-HINTS" |
	"ADD-OVERRIDE-HINTS!" |
	"BINOP-TABLE" |
	"DEFAULT-HINTS-TABLE" |
	"DEFAULT-VERIFY-GUARDS-EAGERNESS" |
	"DELETE-INCLUDE-BOOK-DIR" |
	"DIVE-INTO-MACROS-TABLE" |
	"INVISIBLE-FNS-TABLE" |
	"LOGIC" |
	"MACRO-ALIASES-TABLE" |
	"NTH-ALIASES-TABLE" |
	"PROGRAM" |
	"PUSH-UNTOUCHABLE" |
	"REMOVE-BINOP" |
	"REMOVE-DEFAULT-HINTS" |
	"REMOVE-DEFAULT-HINTS!" |
	"REMOVE-DIVE-INTO-MACRO" |
	"REMOVE-INVISIBLE-FNS" |
	"REMOVE-MACRO-ALIAS" |
	"REMOVE-NTH-ALIAS" |
	"REMOVE-OVERRIDE-HINTS" |
	"REMOVE-OVERRIDE-HINTS!" |
	"REMOVE-UNTOUCHABLE" |
	"RESET-PREHISTORY" |
	"RETURN-LAST-TABLE" |
	"RULER-EXTENDERS" |
	"SET-BACKCHAIN-LIMIT" |
	"SET-BOGUS-DEFUN-HINTS-OK" |
	"SET-BOGUS-MUTUAL-RECURSION-OK" |
	"SET-CASE-SPLIT-LIMITATIONS" |
	"SET-CHECKPOINT-SUMMARY-LIMIT" |
	"SET-COMPILE-FNS" |
	"SET-COMPILER-ENABLED" |
	"SET-DEBUGGER-ENABLE" |
	"SET-DEFAULT-BACKCHAIN-LIMIT" |
	"SET-DEFAULT-HINTS" |
	"SET-DEFAULT-HINTS!" |
	"SET-DEFERRED-TTAG-NOTES" |
	"SET-ENFORCE-REDUNDANCY" |
	"SET-GAG-MODE" |
	"SET-GUARD-CHECKING" |
	"SET-IGNORE-DOC-STRING-ERROR" |
	"SET-IGNORE-OK" |
	"SET-INHIBIT-OUTPUT-LST" |
	"SET-INHIBIT-WARNINGS" |
	"SET-INHIBITED-SUMMARY-TYPES" |
	"SET-INVISIBLE-FNS-TABLE" |
	"SET-IRRELEVANT-FORMALS-OK" |
	"SET-LD-KEYWORD-ALIASES" |
	"SET-LD-REDEFINITION-ACTION" |
	"SET-LD-SKIP-PROOFS" |
	"SET-LD-SKIP-PROOFSP" |
	"SET-LET*-ABSTRACTION" |
	"SET-LET*-ABSTRACTIONP" |
	"SET-MATCH-FREE-DEFAULT" |
	"SET-MATCH-FREE-ERROR" |
	"SET-MEASURE-FUNCTION" |
	"SET-NON-LINEAR" |
	"SET-NON-LINEARP" |
	"SET-NU-REWRITER-MODE" |
	"SET-OVERRIDE-HINTS" |
	"SET-OVERRIDE-HINTS!" |
	"SET-PRINT-CLAUSE-IDS" |
	"SET-PROVER-STEP-LIMIT" |
	"SET-RAW-MODE" |
	"SET-RAW-MODE-ON!" |
	"SET-RAW-PROOF-FORMAT" |
	"SET-REWRITE-STACK-LIMIT" |
	"SET-RULER-EXTENDERS" |
	"SET-RW-CACHE-STATE" |
	"SET-RW-CACHE-STATE!" |
	"SET-SAVED-OUTPUT" |
	"SET-STATE-OK" |
	"SET-TAINTED-OK" |
	"SET-TAINTED-OKP" |
	"SET-VERIFY-GUARDS-EAGERNESS" |
	"SET-WATERFALL-PARALLELISM" |
	"SET-WATERFALL-PRINTING" |
	"SET-WELL-FOUNDED-RELATION" |
	"SET-WRITE-ACL2X" |
	"TERM-TABLE" |
	"USER-DEFINED-FUNCTIONS-TABLE" |
	"VERIFY-GUARDS-EAGERNESS" |
	"WITH-GUARD-CHECKING" |
	"WITH-OUTPUT"		{ addToken(Token.RESERVED_WORD_2); }
		

	/* Literals */
	{IntegerLiteral}				{ addToken(Token.LITERAL_NUMBER_DECIMAL_INT); }
	{HexIntegerLiteral}				{ addToken(Token.LITERAL_NUMBER_HEXADECIMAL); }
	{RationalLiteral}				{ addToken(Token.LITERAL_NUMBER_FLOAT); }
	{HexRationalLiteral}			{ addToken(Token.LITERAL_NUMBER_HEXADECIMAL); }
	{CharacterLiteral}				{ addToken(Token.LITERAL_CHAR); }
	"t" |
	"nil" |
	"(" {WhiteSpace}* ")"			{ addToken(Token.LITERAL_BOOLEAN); }
	{Symbol}						{ addToken(Token.DATA_TYPE); }
	"'("							{ start = zzMarkedPos - 2; symbolParenLevel = 1; yybegin(SYMBOL_PAREN); }
	"`"								{ addToken(Token.DATA_TYPE); }
	","								{ addToken(Token.DATA_TYPE); }
	"("								{ addToken(Token.SEPARATOR); }
	")"								{ addToken(Token.SEPARATOR); }
	{LineTerminator}				{ addNullToken(); return firstToken; }
	{Identifier}					{ addToken(Token.IDENTIFIER); }
	{WhiteSpace}+					{ addToken(Token.WHITESPACE); }
	[\"]							{ start = zzMarkedPos-1; yybegin(STRING); }
	{MLCBegin}					{ start = zzMarkedPos-2; yybegin(MLC); }
	{LineCommentBegin}			{ start = zzMarkedPos-1; yybegin(EOL_COMMENT); }

	/* Ended with a line not in a string or comment. */
	<<EOF>>						{ addNullToken(); return firstToken; }

	/* Catch any other (unhandled) characters and flag them as bad. */
	.							{ addToken(Token.ERROR_IDENTIFIER); }

}


<STRING> {
	[^\n\\\"]+			{}
	\n					{ addToken(start,zzStartRead-1, Token.LITERAL_STRING_DOUBLE_QUOTE); return firstToken; }
	\\.?					{ /* Skip escaped chars. */ }
	\"					{ yybegin(YYINITIAL); addToken(start,zzStartRead, Token.LITERAL_STRING_DOUBLE_QUOTE); }
	<<EOF>>				{ addToken(start,zzStartRead-1, Token.LITERAL_STRING_DOUBLE_QUOTE); return firstToken; }
}


<MLC> {

	[^hwf\n\|]+				{}
	{URL}					{ int temp=zzStartRead; addToken(start,zzStartRead-1, Token.COMMENT_MULTILINE); addHyperlinkToken(temp,zzMarkedPos-1, Token.COMMENT_MULTILINE); start = zzMarkedPos; }
	[hwf]					{}

	\n						{ addToken(start,zzStartRead-1, Token.COMMENT_MULTILINE); return firstToken; }
	{MLCEnd}					{ yybegin(YYINITIAL); addToken(start,zzStartRead+1, Token.COMMENT_MULTILINE); }
	\|						{}
	<<EOF>>					{ addToken(start,zzStartRead-1, Token.COMMENT_MULTILINE); return firstToken; }

}


<EOL_COMMENT> {
	[^hwf\n]+				{ }
	{URL}					{ int temp=zzStartRead; addToken(start,zzStartRead-1, Token.COMMENT_EOL); addHyperlinkToken(temp,zzMarkedPos-1, Token.COMMENT_EOL); start = zzMarkedPos; }
	[hwf]					{ }
	\n						{ addToken(start,zzStartRead-1, Token.COMMENT_EOL); addNullToken(); return firstToken; }
	<<EOF>>					{ addToken(start,zzStartRead-1, Token.COMMENT_EOL); addNullToken(); return firstToken; }

}

<SYMBOL_PAREN> {
	"("						{ symbolParenLevel++; }
	\n						{ addToken(start, zzStartRead - 1, Token.DATA_TYPE);
	                          parenLevelForOffset.put(zzStartRead, symbolParenLevel);
	                          return firstToken; }
	")"						{ symbolParenLevel--;
	                          if (symbolParenLevel == 0) {
	                            yybegin(YYINITIAL);
	                            addToken(start, zzStartRead, Token.DATA_TYPE);
	                          }
	                        }
	[^()\n]+				{ }
	<<EOF>>					{ addToken(start, zzStartRead - 1, Token.DATA_TYPE);
		                      parenLevelForOffset.put(zzStartRead, symbolParenLevel);
		                      return firstToken; }

}


