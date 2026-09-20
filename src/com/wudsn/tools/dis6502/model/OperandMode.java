/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

/**
 * Addressing mode of an operand.
 * <p>
 * Ported from InstructionSet.h.
 *
 * @author Peter Dell
 */
public enum OperandMode {
	Unknown,
	Immediate,
	Absolute,
	ZeroPage,
	Accumulator,
	Implied,
	IndexedIndirect,
	IndirectIndexed,
	ZeroPageX,
	ZeroPageY,
	AbsoluteX,
	AbsoluteY,
	Relative,
	Indirect,
	ZeroPageIndirect,
	ZeroPageRelative,
	IndexedIndirectAbsolute,
	ReservedNop1Byte,
	ReservedNop2Byte,
	ReservedNop3Byte
}
