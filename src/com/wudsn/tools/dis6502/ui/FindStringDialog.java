/*
 * Copyright (C) 2025 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.ui;

/**
 * Keeps a search string's ASCII and hexadecimal-bytes representations in
 * sync, as typed into either of a find dialog's two linked text fields.
 * <p>
 * Ported from ui/FindStringDialog.h / FindStringDialog.cpp. Despite the
 * name (kept for traceability back to the C++ class), this is a plain
 * value/conversion helper, not a dialog itself - it is composed into an
 * actual dialog ({@link MemoryInspectorFindStringDialog}) the same way the
 * C++ version is composed into {@code MemoryInspectorFindStringDialog}/
 * {@code DisassemblyFindStringDialog}. Unlike the C++ version, which reads
 * and writes a dialog's edit controls directly (via a passed-in {@code
 * Dialog&}) and manages fixed-size buffers sized from {@code nMaxChars},
 * this works on plain {@link String}s passed in and returned by the
 * caller - {@code nMaxChars} has no Java equivalent, since a
 * {@link javax.swing.JTextField} does not need a preallocated buffer size.
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

	/** Ported from FindStringDialog::SetAsciiString. */
	public void setAsciiString(String asciiString) {
		this.asciiString = asciiString;
		convertAsciiStringToHexString();
	}

	/** Ported from FindStringDialog::AsciiStringToHexString: updates both fields from a newly-typed ASCII string. */
	public boolean asciiStringToHexString(String asciiFieldText) {
		this.asciiString = asciiFieldText;
		return convertAsciiStringToHexString();
	}

	/** Ported from FindStringDialog::HexStringToAsciiString: updates both fields from a newly-typed hex string. */
	public boolean hexStringToAsciiString(String hexFieldText) {
		this.hexString = hexFieldText;
		return convertHexStringToAsciiString();
	}

	/** Ported from FindStringDialog::ConvertAsciiStringToHexString. */
	private boolean convertAsciiStringToHexString() {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < asciiString.length(); i++) {
			builder.append(String.format("%02X ", asciiString.charAt(i) & 0xFF));
		}
		hexString = builder.length() > 0 ? builder.substring(0, builder.length() - 1) : "";
		return !hexString.isEmpty();
	}

	/**
	 * Ported from FindStringDialog::ConvertHexStringToAsciiString: each
	 * space-separated token is one or two hex digits - a lone digit is
	 * treated as the low nibble of a byte (high nibble 0), matching the
	 * C++ version's {@code "0x00"} padding. Returns {@code false}, leaving
	 * {@link #asciiString} unset, if any token is not valid hex.
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
