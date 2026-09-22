/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

import com.wudsn.tools.base.common.HexUtility;

/**
 * File and file segment headers.
 * <p>
 * See <a href=
 * "http://sdx.atari8.info/sdx_files/4.49/SDX449_Programming_Guide_EN.pdf">the
 * SDX Programming Guide</a> for the details of the SDX file formats.
 * <p>
 * A Java enum has no way to bake an arbitrary value into its ordinal, so
 * each constant carries its 16 bit marker value explicitly.
 *
 * @author Peter Dell
 */
public enum FileHeader {

	RAW(0x0000),
	ATARI_BINARY(0xFFFF),
	SDX_FIXED_BLK(0xFFFA),
	SDX_SYM_REQUIRED(0xFFFB),
	SDX_SYM_DEFINED(0xFFFC),
	SDX_FIX_UP_BLK(0xFFFD),
	SDX_RELOC_BLK(0xFFFE),
	ORIC_BINARY(0x1616);

	private final int value;

	FileHeader(int value) {
		this.value = value;
	}

	public int getValue() {
		return value;
	}

	/** Gets the "0x"-prefixed 4 digit hex string for this header's marker value. */
	public String toHexString() {
		return "0x" + HexUtility.getLongValueHexString(value, 4);
	}

	/** Finds the constant for a 16 bit marker value, or {@code null} if there is none. */
	public static FileHeader valueOf(int value) {
		for (FileHeader fileHeader : values()) {
			if (fileHeader.value == value) {
				return fileHeader;
			}
		}
		return null;
	}
}
