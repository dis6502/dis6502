/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

/**
 * A single 6502/65C02 instruction: its opcode, mnemonic, addressing mode and
 * how it accesses its (optional) label operand.
 * <p>
 * Ported from InstructionSet.h / InstructionSet.cpp.
 *
 * @author Peter Dell
 */
public final class Instruction {

	private final int opcode;
	private final String name;
	private final boolean illegal;
	private final int labelAccess;
	private final OperandMode operandMode;

	public Instruction(int opcode, String name, boolean illegal, int labelAccess, OperandMode operandMode) {
		this.opcode = opcode;
		this.name = name;
		this.illegal = illegal;
		this.labelAccess = labelAccess;
		this.operandMode = operandMode;
	}

	public int getOpcode() {
		return opcode;
	}

	public String getName() {
		return name;
	}

	/** Gets the length of the instruction in bytes, including the opcode. */
	public int getLength() {
		switch (operandMode) {
		case Immediate:
		case ZeroPage:
		case IndexedIndirect:
		case IndirectIndexed:
		case ZeroPageX:
		case ZeroPageY:
		case Relative:
			return 2;

		case Absolute:
		case AbsoluteX:
		case AbsoluteY:
		case Indirect:
			return 3;

		default:
			return 1;
		}
	}

	public int getLabelAccess() {
		return labelAccess;
	}

	public OperandMode getOperandMode() {
		return operandMode;
	}

	public boolean isImmediateMode() {
		return operandMode == OperandMode.Immediate;
	}

	public boolean isUnsupportedInstruction() {
		return illegal;
	}
}
