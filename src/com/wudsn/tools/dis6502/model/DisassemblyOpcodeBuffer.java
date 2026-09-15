/*
 * Copyright (C) 2025 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

/**
 * A circular 3-byte buffer of the most recently disassembled opcode bytes,
 * written out as a comment showing the raw bytes of an instruction.
 * <p>
 * Ported from DisassemblyOpcodeBuffer.h / DisassemblyOpcodeBuffer.cpp.
 *
 * @author Peter Dell
 */
public final class DisassemblyOpcodeBuffer {

	private int opcode0;
	private int opcode1;
	private int opcode2;

	public void clear() {
		opcode0 = opcode1 = opcode2 = 0;
	}

	/** Saves the last byte as opcode in the circular 3 byte buffer (to display opcodes as comment). */
	public void saveLastOpcode(int value) {
		opcode0 = opcode1;
		opcode1 = opcode2;
		opcode2 = value;
	}

	public void write(DisassemblyLineWriter lineWriter, int opcodeSize) {
		switch (opcodeSize) {
		case 1:
			lineWriter.byteValue(opcode2);
			break;
		case 2:
			lineWriter.byteValue(opcode1);
			lineWriter.space();
			lineWriter.byteValue(opcode2);
			break;
		case 3:
			lineWriter.byteValue(opcode0);
			lineWriter.space();
			lineWriter.byteValue(opcode1);
			lineWriter.space();
			lineWriter.byteValue(opcode2);
			break;
		default:
			throw new IllegalArgumentException("Invalid opcode size: " + opcodeSize + ".");
		}
	}
}
