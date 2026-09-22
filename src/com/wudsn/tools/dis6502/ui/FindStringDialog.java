/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.ui;

/**
 * Keeps a search string's ASCII and hexadecimal-bytes representations in
 * sync, as typed into either of a find dialog's two linked text fields.
 * <p>
 * Despite the name, this is a plain value/conversion helper, not a dialog
 * itself - it is composed into an actual dialog ({@link
 * MemoryInspectorFindStringDialog}). Works on plain {@link String}s passed
 * in and returned by the caller.
 *
 * @author Peter Dell
 */
public final class FindStringDialog {

	private String asciiString = "";
	private String hexString = "";

	public String getAsciiString() {
		return asciiString;
	}

	public String getHexString() {
		return hexString;
	}

	/** Sets {@link #asciiString} and recomputes {@link #hexString} from it. */
	public void setAsciiString(String asciiString) {
		this.asciiString = asciiString;
		convertAsciiStringToHexString();
	}

	/** Updates both fields from a newly-typed ASCII string. */
	public boolean asciiStringToHexString(String asciiFieldText) {
		this.asciiString = asciiFieldText;
		return convertAsciiStringToHexString();
	}

	/** Updates both fields from a newly-typed hex string. */
	public boolean hexStringToAsciiString(String hexFieldText) {
		this.hexString = hexFieldText;
		return convertHexStringToAsciiString();
	}

	/** Recomputes {@link #hexString} from {@link #asciiString}. */
	private boolean convertAsciiStringToHexString() {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < asciiString.length(); i++) {
			builder.append(String.format("%02X ", asciiString.charAt(i) & 0xFF));
		}
		hexString = builder.length() > 0 ? builder.substring(0, builder.length() - 1) : "";
		return !hexString.isEmpty();
	}

	/**
	 * Each space-separated token is one or two hex digits - a lone digit is
	 * treated as the low nibble of a byte (high nibble 0). Returns {@code
	 * false}, leaving {@link #asciiString} unset, if any token is not valid
	 * hex.
	 */
	private boolean convertHexStringToAsciiString() {
		StringBuilder builder = new StringBuilder();
		int i = 0;
		int length = hexString.length();
		while (i < length) {
			char c = hexString.charAt(i);
			if (c == ' ') {
				i++;
				continue;
			}

			String token;
			if (i + 1 < length && hexString.charAt(i + 1) != ' ') {
				token = "" + c + hexString.charAt(i + 1);
				i += 2;
			} else {
				token = "0" + c;
				i += 1;
			}

			int value;
			try {
				value = Integer.parseInt(token, 16);
			} catch (NumberFormatException ex) {
				return false; // No valid hex string.
			}
			builder.append((char) value);
		}

		asciiString = builder.toString();
		return !asciiString.isEmpty();
	}
}
