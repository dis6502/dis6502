/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

/**
 * Marker bytes found in an SDX fix-up block.
 *
 * @author Peter Dell
 */
public enum FixupType {

	ADD_250_BYTES(0xFF),
	SET_BLOCK_NUM(0xFE),
	SET_BLOCK_ADDR(0xFD),
	END(0xFC);

	private final int value;

	FixupType(int value) {
		this.value = value;
	}

	public int getValue() {
		return value;
	}

	/** Finds the constant for a marker byte value, or {@code null} if there is none. */
	public static FixupType valueOf(int value) {
		for (FixupType fixupType : values()) {
			if (fixupType.value == value) {
				return fixupType;
			}
		}
		return null;
	}
}
