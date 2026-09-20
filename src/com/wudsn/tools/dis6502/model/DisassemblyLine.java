/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

/**
 * One line of a {@link DisassemblyResult}: the segment/offset/size of the
 * instruction (or equate) it was generated from, its rendered text, and UI
 * state (selected/referenced) plus XRef search-window bookkeeping.
 * <p>
 * Ported from DisassemblyLine.h / DisassemblyLine.cpp (the {@code DIS_LINE}
 * class). The C++ version stores its text as a wide-char string packed
 * directly after the struct in a hand-rolled {@code DIS_BUFFER} byte buffer,
 * to keep per-line memory allocation cheap; Java has no equivalent concern,
 * so this simply holds the text as a plain {@link String} field, and {@link
 * DisassemblySection} holds a plain {@code List<DisassemblyLine>} instead of
 * a list of fixed-size buffers. {@code DisassemblyBuffer} ({@code
 * DIS_BUFFER}) is not ported for the same reason, and neither is {@code
 * GetNext}/{@code GetConstNext} (buffer traversal has no Java equivalent -
 * see {@link DisassemblyResult.LineIterator}).
 *
 * @author Peter Dell
 */
public final class DisassemblyLine {

	private final DisassemblySection section;

	public int segmentIndex; // Segment index where the instruction starts.
	public int offset; // Offset of the instruction in the segment.
	public int size; // Size of the instruction.
	public int xrefLineNumber; // Line number of this line in the XRef search list window.
	public boolean selected; // Displayed in yellow background.
	public boolean referenced; // Displayed in grey if not referenced.
	public int address; // Absolute address to display as comment (in addition to the label).
	public int systemAddress; // Address is filled only for system equates.

	private String line = "";
	private int lineNumber;

	DisassemblyLine(DisassemblySection section) {
		this.section = section;
	}

	public DisassemblySection getSection() {
		return section;
	}

	/** @throws IllegalStateException if the line number has not been set yet. */
	public int getLineNumber() {
		if (lineNumber == 0) {
			throw new IllegalStateException("Line number not yet set.");
		}
		return lineNumber;
	}

	void setLineNumber(int lineNumber) {
		this.lineNumber = lineNumber;
	}

	public String getLine() {
		return line;
	}

	void setLine(String line) {
		this.line = line;
	}

	@Override
	public String toString() {
		return "segmentIndex=" + segmentIndex + ", offset=" + Memory.offsetToHexString(offset) + ", size="
				+ Memory.sizeToHexString(size) + ", xrefLineNumber=" + xrefLineNumber + ", selected=" + selected
				+ ", referenced=" + referenced + ", address=" + Memory.addressToHexString(address)
				+ ", systemAddress=" + Memory.addressToHexString(systemAddress) + ", lineNumber=" + lineNumber
				+ ", line=" + line;
	}
}
