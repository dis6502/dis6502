/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

/**
 * Bit flags describing how a label is accessed. Values are combined with
 * bitwise OR (e.g. {@link #READ_WRITE} is {@link #READ}|{@link #WRITE}), so
 * this is a plain {@code int}-based flags class rather than a Java
 * {@code enum} - a discrete enum cannot represent the OR'd combinations the
 * original C++ code stores back into a single {@code LabelAccess} value (see
 * {@code Equate::AddLabelReference} in the C++ source).
 * <p>
 * Ported from LabelAccess.h. {@code LabelAccessInfo}/{@code
 * LabelAccessFactory} (the UI-facing key/text lookup and combo-box index
 * helper) are not ported yet - they are only needed once the UI layer is
 * ported.
 *
 * @author Peter Dell
 */
public final class LabelAccess {

	public static final int UNKNOWN = 0;
	public static final int READ = 1;
	public static final int WRITE = 2;
	public static final int READ_WRITE = READ | WRITE;
	public static final int IMMEDIATE = 4;

	private LabelAccess() {
	}

	/** Returns true if all bits of {@code labelAccess} are set in {@code value}. */
	public static boolean isSupported(int value, int labelAccess) {
		return (value & labelAccess) != 0;
	}

	/** Gets the single-character qualifier used in an equate's text representation. */
	public static String getQualifier(int labelAccess) {
		switch (labelAccess) {
		case READ:
			return "<";
		case WRITE:
			return ">";
		case READ_WRITE:
			return "=";
		case IMMEDIATE:
			return "#";
		default:
			return "?";
		}
	}

	/** The XML attribute key for a single label access value, used by {@link Equate}'s serialization. */
	public static String getKey(int labelAccess) {
		switch (labelAccess) {
		case READ:
			return "READ";
		case WRITE:
			return "WRITE";
		case READ_WRITE:
			return "READ_WRITE";
		case IMMEDIATE:
			return "IMMEDIATE";
		default:
			return "UNKNOWN";
		}
	}

	/** The inverse of {@link #getKey(int)}. */
	public static int fromKey(String key) {
		switch (key) {
		case "READ":
			return READ;
		case "WRITE":
			return WRITE;
		case "READ_WRITE":
			return READ_WRITE;
		case "IMMEDIATE":
			return IMMEDIATE;
		default:
			return UNKNOWN;
		}
	}
}
