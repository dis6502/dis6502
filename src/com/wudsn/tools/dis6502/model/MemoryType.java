/*
 * Copyright (C) 2025 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

/**
 * Type of bytes found in the type buffer that qualifies the memory inspector
 * buffer. {@link #LOBYTE} and {@link #HIBYTE} are followed by a byte that
 * represents the missing part of the address. For example, if we have $A9
 * $04 in the source (and it should be disassembled as LDA # &gt;L0480), we
 * will have {@link #HIBYTE} followed by $80 in the type buffer to say that we
 * have a HIBYTE ($04) in the memory inspector buffer and the LOBYTE is $80.
 * <p>
 * Ported from MemoryType.h. The legacy "version 2.2" variant of this enum
 * (used only for reading very old workspace files) is not ported yet.
 *
 * @author Peter Dell
 */
public enum MemoryType {

	UNKNOWN,
	LOBYTE,
	HIBYTE,
	BYTE,
	WORD,
	LABEL,
	STRING,
	SBYTE,
	DLIST,
	STORE,
	CODE,
	SYMBOL,
	FIXUP;

	/**
	 * The values in enum declaration order, for indexing a type buffer by
	 * ordinal the same way the C++ code indexes by the underlying byte value.
	 */
	public static final MemoryType[] VALUES = values();
}
