/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

import java.util.ArrayList;
import java.util.List;

/**
 * One of the four sections a {@link DisassemblyResult} is split into: an
 * ordered list of {@link DisassemblyLine}s plus the section's type.
 * <p>
 * Backed directly by a plain {@code List<DisassemblyLine>} ({@link
 * #lines}), appended to via {@link #addLine}.
 *
 * @author Peter Dell
 */
public final class DisassemblySection {

	public static int getIndex(DisassemblySectionType disassemblySectionType) {
		switch (disassemblySectionType) {
		case SYSTEM_EQUATES:
			return 0;
		case USER_EQUATES:
			return 1;
		case CODE_EQUATES:
			return 2;
		case CODE_LINES:
			return 3;
		default:
			throw new IllegalArgumentException("Invalid disassemblySectionType: " + disassemblySectionType + ".");
		}
	}

	public static DisassemblySectionType getType(int index) {
		switch (index) {
		case 0:
			return DisassemblySectionType.SYSTEM_EQUATES;
		case 1:
			return DisassemblySectionType.USER_EQUATES;
		case 2:
			return DisassemblySectionType.CODE_EQUATES;
		case 3:
			return DisassemblySectionType.CODE_LINES;
		default:
			throw new IllegalArgumentException("Invalid index: " + index + ".");
		}
	}

	public static String getText(DisassemblySectionType disassemblySectionType) {
		switch (disassemblySectionType) {
		case SYSTEM_EQUATES:
			return "System equates";
		case USER_EQUATES:
			return "User equates";
		case CODE_EQUATES:
			return "Code equates";
		case CODE_LINES:
			return "Start of code";
		default:
			throw new IllegalArgumentException("Undefined disassemblySectionType: " + disassemblySectionType + ".");
		}
	}

	private final DisassemblyResult disassemblyResult;
	private final DisassemblySectionType type;

	public final List<DisassemblyLine> lines = new ArrayList<>();

	DisassemblySection(DisassemblyResult disassemblyResult, DisassemblySectionType type) {
		this.disassemblyResult = disassemblyResult;
		this.type = type;
	}

	public DisassemblyResult getDisassemblyResult() {
		return disassemblyResult;
	}

	public DisassemblySectionType getType() {
		return type;
	}

	public int getLineCount() {
		return lines.size();
	}

	/**
	 * Appends a new line: copies {@code templateLine}'s segment/offset/size/
	 * address/xrefLineNumber/selected/referenced fields and sets its text to
	 * {@code text}.
	 */
	public DisassemblyLine addLine(DisassemblyLine templateLine, String text) {
		DisassemblyLine line = new DisassemblyLine(this);
		line.segmentIndex = templateLine.segmentIndex;
		line.offset = templateLine.offset;
		line.size = templateLine.size;
		line.xrefLineNumber = templateLine.xrefLineNumber;
		line.selected = templateLine.selected;
		line.referenced = templateLine.referenced;
		line.address = templateLine.address;
		line.systemAddress = templateLine.systemAddress;
		line.setLine(text);
		lines.add(line);
		return line;
	}
}
