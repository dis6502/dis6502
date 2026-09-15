/*
 * Copyright (C) 2025 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

import com.wudsn.tools.base.common.HexUtility;

/**
 * Model of an 8-bit memory with a 16 bit address space and little endian
 * byte order.
 * <p>
 * Ported from Memory.h / Memory.cpp. Unlike C++, Java has no unsigned
 * integer types, so this class does not define separate "byte"/"word"/
 * "address"/"offset" type aliases: ported code uses a plain {@code int} for
 * all of them, following the same convention as
 * {@link com.wudsn.tools.base.common.HexUtility}. A "byte" value is always in
 * [0, 255], a "word"/"address"/"offset" value is always in [0, 65535].
 * "address_offset" (a signed relative offset) needs no special type either,
 * since Java's {@code int} is already signed.
 *
 * @author Peter Dell
 */
public final class Memory {

	/**
	 * Maximum size of a memory block: one more than the highest offset, since
	 * a block can span the full 16 bit address space.
	 */
	public static final int MAX_SIZE = 0x10000;

	/** Maximum offset within a memory block. */
	public static final int MAX_OFFSET = 0xFFFF;

	private Memory() {
	}

	/**
	 * Combines a low and a high byte into a word/address, assuming little
	 * endian byte order.
	 */
	public static int toWord(int low, int high) {
		return (low & 0xFF) | ((high & 0xFF) << 8);
	}

	/**
	 * Combines a low and a high byte into an address, assuming little endian
	 * byte order. Identical to {@link #toWord(int, int)}; kept as a separate
	 * method to mirror the C++ API.
	 */
	public static int toAddress(int low, int high) {
		return toWord(low, high);
	}

	/** Gets the low byte of a word/address. */
	public static int toLowByte(int word) {
		return word & 0xFF;
	}

	/** Gets the high byte of a word/address. */
	public static int toHighByte(int word) {
		return (word >> 8) & 0xFF;
	}

	/**
	 * Gets the 4 (or, for exactly {@link #MAX_SIZE}, 5) upper case hex digits
	 * for a memory block size.
	 */
	public static String sizeToHexString(int size) {
		if (size < 0 || size > MAX_SIZE) {
			throw new IllegalArgumentException(
					"Size " + HexUtility.getLongValueHexString(size) + " exceeds maximum size of "
							+ HexUtility.getLongValueHexString(MAX_SIZE) + ".");
		}
		return HexUtility.getLongValueHexString(size, 4);
	}

	/** Gets the decimal string for a memory block size. */
	public static String sizeToString(int size) {
		return Integer.toString(size);
	}

	/** Gets the 4 upper case hex digits for an offset within a memory block. */
	public static String offsetToHexString(int offset) {
		if (offset < 0 || offset > MAX_OFFSET) {
			throw new IllegalArgumentException("Offset " + HexUtility.getLongValueHexString(offset)
					+ " exceeds maximum size of " + HexUtility.getLongValueHexString(MAX_OFFSET) + ".");
		}
		return HexUtility.getLongValueHexString(offset, 4);
	}

	/** Gets the 2 upper case hex digits for a byte value. */
	public static String byteToHexString(int value) {
		return HexUtility.getByteValueHexString(value);
	}

	/** Gets the 4 upper case hex digits for an address. */
	public static String addressToHexString(int address) {
		if (address < 0 || address > MAX_OFFSET) {
			throw new IllegalArgumentException("Address " + HexUtility.getLongValueHexString(address)
					+ " exceeds maximum size of " + HexUtility.getLongValueHexString(MAX_OFFSET) + ".");
		}
		return HexUtility.getLongValueHexString(address, 4);
	}

	/**
	 * Gets the signed decimal string for a relative address offset: empty for
	 * 0, "+N" for a positive offset, "-N" for a negative offset.
	 */
	public static String addressOffsetToString(int addressOffset) {
		if (addressOffset == 0) {
			return "";
		}
		if (addressOffset > 0) {
			return "+" + addressOffset;
		}
		return Integer.toString(addressOffset); // "-" is the prefix already.
	}
}
