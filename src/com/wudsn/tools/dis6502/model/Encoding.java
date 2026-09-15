/*
 * Copyright (C) 2025 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
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
	UTF8
}
