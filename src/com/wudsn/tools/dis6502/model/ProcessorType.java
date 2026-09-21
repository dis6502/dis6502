/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

/**
 * Ported from ProcessorType.h. {@code ProcessorTypeInfo}/{@code
 * ProcessorTypeFactory} (the key/text lookup) has no counterpart: the key
 * is the enum constant's name, and the text the segment properties dialog
 * shows is the {@link InstructionSet}'s own name.
 *
 * @author Peter Dell
 */
public enum ProcessorType {
	UNKNOWN,
	MOS6502,
	MOS65C02
}
