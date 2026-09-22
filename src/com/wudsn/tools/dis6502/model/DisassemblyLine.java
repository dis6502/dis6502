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
 * Holds its text as a plain {@link String} field, and {@link
 * DisassemblySection} holds a plain {@code List<DisassemblyLine>} of them -
 * see {@link DisassemblyResult.LineIterator} for how a line's position
 * within that structure is walked.
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
	/**
	 * {@link #systemAddress} of every line that is not a system equate's -
	 * such a line is never omitted as "unreferenced". Not 0: a system can
	 * have a label at address {@code $0000} (the C64's 6510 port), and that
	 * line must then always be written, referenced or not.
	 */
	public static final int NO_SYSTEM_ADDRESS = -1;

	public int systemAddress = NO_SYSTEM_ADDRESS; // Address is filled only for system equates.

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
				+ ", systemAddress=" + (systemAddress == NO_SYSTEM_ADDRESS ? "none" : Memory.addressToHexString(systemAddress)) + ", lineNumber=" + lineNumber
				+ ", line=" + line;
	}
}
