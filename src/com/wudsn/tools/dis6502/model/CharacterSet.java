/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.wudsn.tools.base.repository.ValueSet;
import com.wudsn.tools.dis6502.Messages;
import com.wudsn.tools.dis6502.ValueSets;

/**
 * An 8x8 pixel character set the memory inspector renders with: 256
 * characters of 8 bytes each in a {@code .chr} file under {@code /fonts/} on
 * the classpath, one byte per pixel row with bit 7 the leftmost pixel,
 * ordered by screen code (the code the video hardware uses). The names
 * follow the byte encoding a set belongs to (ATASCII, PETSCII, ASCII), not
 * the file order: in {@code ATASCII-standard.chr}, 'A' is at internal code
 * {@code 0x21}, not at ATASCII {@code 0x41}. {@code Oric-ascii.chr} holds
 * the Oric ROM's 96 glyphs (codes 32-127, from {@code $FC78} of BASIC 1.1,
 * identical in 1.0), codes 0-31 blank (attribute codes show as a blank
 * cell), and codes 128-255 as the inverse of 0-127 over the whole 8-pixel
 * cell, so inverse text forms a solid bar. Every
 * character set can be shown for every computer system; a system only names
 * its default (see {@link com.wudsn.tools.dis6502.model.system.ComputerSystem#getDefaultCharacterSet}).
 * <p>
 * Each set knows how a byte of its own computer maps to a screen code
 * (ATASCII to ANTIC internal code, PETSCII to C64 screen code, ASCII
 * unchanged) and how this
 * port's own ASCII text (addresses, hex digits) maps to the screen code of
 * the glyph that looks like it.
 *
 * @author Peter Dell
 */
public final class CharacterSet extends ValueSet {

	// Private: the value set loader takes every public static final field for a value.
	private static final int CHARACTER_COUNT = 256;
	private static final int BYTES_PER_CHARACTER = 8;

	private enum Kind {
		ATASCII, PETSCII_UPPERCASE, PETSCII_LOWERCASE, ASCII
	}

	public static final CharacterSet ATASCII_STANDARD;
	public static final CharacterSet ATASCII_INTERNATIONAL;
	public static final CharacterSet PETSCII_UPPERCASE;
	public static final CharacterSet PETSCII_LOWERCASE;
	public static final CharacterSet ORIC_ASCII;

	private static final Map<String, CharacterSet> values;

	private final String fileName;
	private final Kind kind;
	private byte[] data;

	static {
		values = new LinkedHashMap<String, CharacterSet>();

		ATASCII_STANDARD = add("ATASCII_STANDARD", "ATASCII-standard.chr", Kind.ATASCII);
		ATASCII_INTERNATIONAL = add("ATASCII_INTERNATIONAL", "ATASCII-international.chr", Kind.ATASCII);
		PETSCII_UPPERCASE = add("PETSCII_UPPERCASE", "PETSCII-uppercase.chr", Kind.PETSCII_UPPERCASE);
		PETSCII_LOWERCASE = add("PETSCII_LOWERCASE", "PETSCII-lowercase.chr", Kind.PETSCII_LOWERCASE);
		ORIC_ASCII = add("ORIC_ASCII", "Oric-ascii.chr", Kind.ASCII);

		initializeClass(CharacterSet.class, ValueSets.class);
	}

	private CharacterSet(String id, int sortKey, String fileName, Kind kind) {
		super(id, id, sortKey);
		this.fileName = fileName;
		this.kind = kind;
	}

	private static CharacterSet add(String id, String fileName, Kind kind) {
		// The sort key keeps the declaration order - ValueSet would sort by text otherwise.
		CharacterSet result = new CharacterSet(id, values.size(), fileName, kind);
		values.put(id, result);
		return result;
	}

	/** Gets the unmodifiable list of all values. */
	public static List<CharacterSet> getValues() {
		return Collections.unmodifiableList(new ArrayList<CharacterSet>(values.values()));
	}

	/** The classpath resource name of this set's {@code .chr} file. */
	public String getResourceName() {
		return "/fonts/" + fileName;
	}

	/** The 8 pixel rows of the glyph with the given screen code, bit 7 leftmost. */
	public synchronized byte[] getGlyph(int screenCode) {
		if (data == null) {
			data = load();
		}
		int offset = (screenCode & 0xFF) * BYTES_PER_CHARACTER;
		byte[] result = new byte[BYTES_PER_CHARACTER];
		System.arraycopy(data, offset, result, 0, BYTES_PER_CHARACTER);
		return result;
	}

	private byte[] load() {
		String resourceName = getResourceName();
		try (InputStream in = CharacterSet.class.getResourceAsStream(resourceName)) {
			if (in == null) {
				// ERROR: Font resource "{0}" not found.
				throw new IOException(Messages.E066.format(resourceName));
			}
			byte[] result = in.readAllBytes();
			if (result.length != CHARACTER_COUNT * BYTES_PER_CHARACTER) {
				throw new IllegalStateException(
						"Character set resource '" + resourceName + "' has " + result.length + " bytes instead of 2048.");
			}
			return result;
		} catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	/**
	 * The screen code of a byte of this set's computer: ATASCII to ANTIC
	 * internal code, PETSCII to C64 screen code; an ASCII set's screen code is
	 * the byte itself (the Oric has no separate screen code).
	 */
	public int toScreenCode(int value) {
		value &= 0xFF;
		switch (kind) {
		case ATASCII:
			return atasciiToInternal(value);
		case ASCII:
			return value;
		default:
			return petsciiToScreenCode(value);
		}
	}

	/** The screen code of the glyph that looks like the ASCII character {@code ch}, for this port's own text. */
	public int textToScreenCode(char ch) {
		switch (kind) {
		case ATASCII:
			return atasciiToInternal(ch & 0x7F);
		case ASCII:
			return ch & 0x7F;
		case PETSCII_UPPERCASE:
			if (ch == '|') {
				return 0x5D; // Vertical line graphic, PETSCII has no '|'.
			}
			if (ch >= 'a' && ch <= 'z') {
				ch = Character.toUpperCase(ch);
			}
			return asciiToC64ScreenCode(ch);
		case PETSCII_LOWERCASE:
			if (ch == '|') {
				return 0x5D;
			}
			if (ch >= 'a' && ch <= 'z') {
				return ch - 0x60;
			}
			if (ch >= 'A' && ch <= 'Z') {
				return ch;
			}
			return asciiToC64ScreenCode(ch);
		default:
			throw new IllegalStateException("Unknown kind " + kind + ".");
		}
	}

	/**
	 * The byte of this set's encoding (ATASCII, PETSCII or ASCII) that displays as
	 * the glyph of the ASCII character {@code ch} - the inverse of {@link
	 * #toScreenCode} for typed text. Printable bytes ({@code 0x20-0xFF}) are
	 * preferred over control bytes.
	 */
	public int textToByte(char ch) {
		int screenCode = textToScreenCode(ch);
		for (int i = 0x20; i < 0x120; i++) {
			int value = i & 0xFF;
			if (toScreenCode(value) == screenCode) {
				return value;
			}
		}
		// Every screen code textToScreenCode returns is reachable from some byte of both encodings.
		throw new IllegalStateException("No byte for screen code " + screenCode + ".");
	}

	/** The end-of-line byte of this set's encoding: {@code 0x9B} for ATASCII, {@code 0x0D} for PETSCII and ASCII. */
	public int getReturnByte() {
		return kind == Kind.ATASCII ? 0x9B : 0x0D;
	}

	private static int asciiToC64ScreenCode(char ch) {
		if (ch >= 0x20 && ch < 0x40) {
			return ch;
		}
		if (ch >= 0x40 && ch < 0x60) {
			return ch - 0x40;
		}
		return '?';
	}

	/** Bit 7 (inverse video) is kept; the lower 7 bits swap the ATASCII and internal code blocks. */
	private static int atasciiToInternal(int value) {
		int low = value & 0x7F;
		int result;
		if (low < 0x20) {
			result = low + 0x40;
		} else if (low < 0x60) {
			result = low - 0x20;
		} else {
			result = low;
		}
		return result | (value & 0x80);
	}

	private static int petsciiToScreenCode(int value) {
		if (value < 0x20) {
			return value + 0x80;
		} else if (value < 0x40) {
			return value;
		} else if (value < 0x60) {
			return value - 0x40;
		} else if (value < 0x80) {
			return value - 0x20;
		} else if (value < 0xA0) {
			return value + 0x40;
		} else if (value < 0xC0) {
			return value - 0x40;
		} else if (value < 0xFF) {
			return value - 0x80;
		}
		return 0x5E;
	}
}
