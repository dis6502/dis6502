/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

/**
 * A processor's full set of 256 instructions, indexed by opcode.
 *
 * @author Peter Dell
 */
public abstract class InstructionSet {

	private final ProcessorType processorType;
	private final Instruction[] instructions;

	protected InstructionSet(ProcessorType processorType, Instruction[] instructions) {
		if (instructions.length != 256) {
			throw new IllegalArgumentException(
					"Parameter 'instructions' must have exactly 256 entries. Actual length is "
							+ instructions.length + ".");
		}
		this.processorType = processorType;
		this.instructions = instructions;
	}

	public Instruction[] getInstructions() {
		return instructions;
	}

	public Instruction getInstruction(int opcode) {
		return instructions[opcode & 0xFF];
	}

	/** The processor this is the instruction set of. */
	public ProcessorType getProcessorType() {
		return processorType;
	}

	public boolean isDualAddressingMode(int opcode) {
		switch (opcode & 0xFF) {
		case 0xAD:
		case 0xBD: // LDA
		case 0xAE:
		case 0xBE: // LDX
		case 0xAC:
		case 0xBC: // LDY
		case 0x8D:
		case 0x9D: // STA
		case 0x8E: // STX
		case 0x8C: // STY
		case 0x2C: // BIT
		case 0x6D:
		case 0x7D: // ADC
		case 0xED:
		case 0xFD: // SBC
		case 0x0E:
		case 0x1E: // ASL
		case 0x4E:
		case 0x5E: // LSR
		case 0x2E:
		case 0x3E: // ROL
		case 0x6E:
		case 0x7E: // ROR
		case 0xCE:
		case 0xDE: // DEC
		case 0xEE:
		case 0xFE: // INC
		case 0x0D:
		case 0x1D: // ORA
		case 0x2D:
		case 0x3D: // AND
		case 0x4D:
		case 0x5D: // EOR
			return true;
		default:
			return false;
		}
	}
}
