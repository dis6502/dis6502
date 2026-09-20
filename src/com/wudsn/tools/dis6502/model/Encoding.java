/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

/**
 * Text encoding used for the disassembly output.
 * <p>
 * Ported from Encoding.h. {@code EncodingInfo}/{@code EncodingFactory} (the
 * UI-facing key/text/newline lookup) is not ported yet.
 *
 * @author Peter Dell
 */
public enum Encoding {
	UNKNOWN,
	ASCII,
	ATASCII,
	BINARY,
	UTF8;

	/** The XML attribute key for an encoding value, used by {@link Profile}'s serialization. */
	public String getKey() {
		return name();
	}

	/** The inverse of {@link #getKey()}. */
	public static Encoding fromKey(String key) {
		try {
			return valueOf(key);
		} catch (IllegalArgumentException e) {
			return UNKNOWN;
		}
	}
}
