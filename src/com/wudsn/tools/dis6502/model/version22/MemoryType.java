/*
 * Copyright (C) 2025 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model.version22;

/**
 * Frozen snapshot of the ordinal layout {@code MemoryType} had in
 * application version 2.2. Values are serialized as raw bytes into a memory
 * inspector's type buffer, so their ordinals must never change - this copy
 * exists only so that
 * {@link com.wudsn.tools.dis6502.model.MemoryType#ORDINALS_MATCH_VERSION_22}
 * can verify, at class load time, that the current {@code MemoryType} still
 * agrees with this original layout for every value that existed back then.
 * Do not edit this enum: it must stay exactly as it was in version 2.2.
 * <p>
 * Ported from the {@code version_22} namespace in MemoryType.h.
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
	CODE
}
