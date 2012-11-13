package org.proofpad;

import org.proofpad.SExpUtils.ExpType;

public class Expression {
	final public int lines;
	final public String contents;
	final public ExpType firstType;
	final public int index;
	final public int nextIndex;
	public int prevGapHeight = 0;
	public int nextGapHeight = 0;
	public Expression prev;
	public int expNum;
	public Expression(int lines, String contents, ExpType firstType, int index, int nextIndex, Expression prev) {
		this.lines = lines;
		this.contents = contents;
		this.firstType = firstType;
		this.index = index;
		this.nextIndex = nextIndex;
		this.prev = prev;
	}

	@Override public String toString() {
		return "height: " + lines + ", contents: '" + contents + "', firstType: " + firstType
				+ ", index: " + index;
	}
}
