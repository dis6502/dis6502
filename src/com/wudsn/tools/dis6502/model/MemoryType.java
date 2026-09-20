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
 * Ported from MemoryType.h, including its {@code version_22} namespace (see
 * {@link com.wudsn.tools.dis6502.model.version22.MemoryType}), which is kept
 * only to guarantee - by throwing at class load time otherwise - that this
 * enum's ordinals (used as raw bytes in a memory inspector's type buffer)
 * never silently drift from the encoding used since application version 2.2.
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
	SYMBOL, // New in version 3.0.
	FIXUP; // New in version 3.0.

	/**
	 * The values in enum declaration order, for indexing a type buffer by
	 * ordinal the same way the C++ code indexes by the underlying byte value.
	 */
	public static final MemoryType[] VALUES = values();

	/**
	 * Set to {@code true} once every {@link com.wudsn.tools.dis6502.model.version22.MemoryType}
	 * value has been confirmed to still have the same ordinal in this enum;
	 * otherwise class initialization fails with an {@link ExceptionInInitializerError}.
	 * Ensures binary compatibility of the type encoding, mirroring the C++
	 * test {@code MemoryInspectorTest::TestMemoryInspectorType}.
	 */
	public static final boolean ORDINALS_MATCH_VERSION_22;

	static {
		for (com.wudsn.tools.dis6502.model.version22.MemoryType legacyValue : com.wudsn.tools.dis6502.model.version22.MemoryType
				.values()) {
			MemoryType currentValue = MemoryType.valueOf(legacyValue.name());
			if (currentValue.ordinal() != legacyValue.ordinal()) {
				throw new ExceptionInInitializerError("MemoryType." + legacyValue.name() + " has ordinal "
						+ currentValue.ordinal() + " but must stay at ordinal " + legacyValue.ordinal()
						+ " to match the binary encoding used since application version 2.2.");
			}
		}
		ORDINALS_MATCH_VERSION_22 = true;
	}

	/**
	 * Ported from {@code MemoryInspectorControlImpl::Char}'s {@code if (cType ==
	 * SBYTE) { ... }} block - the ASCII-to-"internal" (Atari screen code)
	 * transform applied when typing a character into an {@link #SBYTE}-typed
	 * byte's cell, used by {@link MutableMemoryInspectorState#typeEditChar}. This
	 * is the WRITE direction, gated per-byte on its type and only ever given a
	 * typed ASCII character (0-127); it is deliberately kept separate from
	 * {@code com.wudsn.tools.dis6502.ui.MemoryInspectorGridPanel}'s
	 * {@code toInternalCode}, the DISPLAY direction, which is driven by the
	 * global "display as screen code" toggle and covers the full 0-255 byte
	 * range. The two are mathematical inverses of each other on the 0-127
	 * range but are distinct, differently-shaped formulas in the C++ source,
	 * so they stay distinct here too rather than sharing one generalized
	 * method.
	 */
	public static int toSbyteInternalCode(int asciiChar) {
		if (asciiChar < 32) {
			return asciiChar + 64;
		} else if (asciiChar < 96) {
			return asciiChar - 32;
		}
		return asciiChar;
	}
}
