package com.calebegg.ide;

public class Expression {
	final public int height;
	final public String contents;
	final public int firstType;
	final public int index;
	final public int nextIndex;
	public int prevGapHeight = 0;
	public int nextGapHeight = 0;
	public Expression prev;
	public Expression(int height, String contents, int firstType, int index, int nextIndex, Expression prev) {
		this.height = height;
		this.contents = contents;
		this.firstType = firstType;
		this.index = index;
		this.nextIndex = nextIndex;
		this.prev = prev;
	}
	@Override
	public String toString() {
		return "height: " + height + ", contents: '" + contents + "', firstType: " + firstType + ", index: " + index;
	}
}
