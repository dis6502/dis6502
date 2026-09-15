/*
 * Copyright (C) 2025 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

import com.wudsn.tools.base.common.HexUtility;

/**
 * Builds one line of disassembly output text, one token at a time (labels,
 * numbers, bytes, addresses, comments, instructions), formatted according to
 * a {@link Profile}.
 * <p>
 * Ported from DisassemblyLineWriter.h / DisassemblyLineWriter.cpp. The C++
 * version writes into a fixed-size {@code wchar_t[]} buffer sized up front
 * (mirroring the original application's manual memory management) and
 * validates that every character is printable ASCII (32-127); this uses a
 * plain, unbounded {@link StringBuilder} instead, so the constructor's
 * buffer-size parameter and the character-range check have no Java
 * equivalent. The C++ {@code CString}/{@code String} overload pair (raw
 * {@code wchar_t*} vs. {@code wstring}) collapses into a single
 * {@link #string(String)}. {@code Byte} is renamed to {@link
 * #byteValue(int)} since {@code byte} is a reserved word in Java; it is
 * kept as a separate method from {@link #byteNumber(int)} even though both
 * currently have identical bodies, matching the (seemingly duplicated) C++
 * source exactly.
 *
 * @author Peter Dell
 */
public final class DisassemblyLineWriter {

	private final StringBuilder buffer = new StringBuilder();

	private Workspace workspace;
	private Profile profile;
	private String directiveForceAbsolute = "";

	/** Initializes the state before a disassembly run. */
	public void init(Workspace workspace) {
		this.workspace = workspace;
		this.profile = workspace.getProfile();

		// Cache formatter profile infos.
		directiveForceAbsolute = profile.directiveForceAbsolute;
		if (profile.showLowerCaseInstructions) {
			directiveForceAbsolute = directiveForceAbsolute.toLowerCase();
		}
	}

	public String getLineBuffer() {
		return buffer.toString();
	}

	public DisassemblyLineWriter clear() {
		buffer.setLength(0);
		return this;
	}

	public DisassemblyLineWriter ch(char value) {
		buffer.append(value);
		return this;
	}

	public DisassemblyLineWriter string(String value) {
		buffer.append(value);
		return this;
	}

	public DisassemblyLineWriter comment() {
		return comment("");
	}

	public DisassemblyLineWriter comment(String comment) {
		string(profile.commentPrefix);
		if (!comment.isEmpty()) {
			space();
			string(comment);
		}
		return this;
	}

	public DisassemblyLineWriter space() {
		return ch(' ');
	}

	public DisassemblyLineWriter spaceUntil34() {
		while (buffer.length() < 34) {
			space();
		}
		return this;
	}

	public DisassemblyLineWriter decimal(int value) {
		return string(Integer.toString(value));
	}

	public DisassemblyLineWriter number(int value) {
		if (profile.useHexNotation) {
			return string(profile.hexNotationPrefix + HexUtility.getLongValueHexString(value, 4));
		}
		return decimal(value);
	}

	public DisassemblyLineWriter byteNumber(int value) {
		if (profile.useHexNotation) {
			return string(profile.hexNotationPrefix + HexUtility.getByteValueHexString(value));
		}
		return decimal(value);
	}

	public DisassemblyLineWriter byteValue(int value) {
		if (profile.useHexNotation) {
			return string(profile.hexNotationPrefix + HexUtility.getByteValueHexString(value));
		}
		return decimal(value);
	}

	public DisassemblyLineWriter address(int address) {
		return number(address);
	}

	public DisassemblyLineWriter label(String label) {
		return string(label);
	}

	public DisassemblyLineWriter labelOrZeroPageAddress(SegmentList segmentList, int segmentIndex, int pc, int address,
			MemoryType type, int opcode) {
		if (address >= 0x0100) {
			throw new IllegalArgumentException("Address is not on zero page.");
		}
		String label = segmentList.getLabelAtAddress(segmentIndex, pc, address, type, opcode);
		if (!label.isEmpty()) {
			return label(label);
		}
		return byteValue(address & 0xFF);
	}

	public DisassemblyLineWriter labelOrAddress(SegmentList segmentList, int segmentIndex, int pc, int address,
			MemoryType type, int opcode) {
		String label = segmentList.getLabelAtAddress(segmentIndex, pc, address, type, opcode);
		if (!label.isEmpty()) {
			return label(label);
		}
		return address(address);
	}

	public DisassemblyLineWriter alignInstructions() {
		if (profile.alignInstructions) {
			int labelLength = buffer.length();
			int maxLength = Math.max(labelLength + 1, 12);
			for (int len = labelLength; len < maxLength; len++) {
				space();
			}
		}
		return this;
	}

	public DisassemblyLineWriter instruction(Segment segment, int opcode) {
		return instruction(segment, opcode, 0xFFFF);
	}

	public DisassemblyLineWriter instruction(Segment segment, int opcode, int address) {
		InstructionSet instructionSet = workspace.getInstructionSet(segment.processorType);
		boolean isDualMode = address < 0x100 && instructionSet.isDualAddressingMode(opcode)
				&& !profile.showZPAbsoluteAsByte && !directiveForceAbsolute.isEmpty();
		String opcodeName = instructionSet.getInstruction(opcode).getName();

		if (profile.showLowerCaseInstructions) {
			string(opcodeName.toLowerCase());
		} else {
			string(opcodeName);
		}

		if (isDualMode) {
			string(directiveForceAbsolute);
		}
		return this;
	}
}
