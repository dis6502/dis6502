/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

import java.nio.charset.StandardCharsets;

import com.wudsn.tools.dis6502.Application;
import com.wudsn.tools.dis6502.ApplicationSettingsSection;

/**
 * Reads the pre-3.0, pre-XML binary profile format ({@code
 * DIS6502PRF10}/{@code DIS6502PRF13}/{@code DIS6502PRF16}/{@code
 * DIS6502PRF17}). Ported from Profile1X.h / Profile1X.cpp.
 * <p>
 * The four versions are strict field-appending supersets of each other
 * (each later version's struct starts with every field of the previous
 * one), so - like the C++ version - this always parses into a {@code
 * PROFILE17}-shaped 408 byte buffer: copying however many payload bytes a
 * given file actually has (12 to 408) into a zero-filled buffer naturally
 * leaves whatever trailing fields an older/shorter version doesn't have at
 * 0/false, without needing a separate code path per version.
 * <p>
 * The exact byte layout was verified against a real {@code
 * DIS6502PRF17} fixture ({@code profiles/PRF17/mads.prf}) - twelve
 * independently cross-checked fields (every boolean plus several strings)
 * matched the plain-text {@code profiles/mads.prf} profile's known values
 * exactly, at the offsets this analysis predicted.
 * <p>
 * Found and fixed three bugs in the C++ source while porting it - see the
 * fix commit for details - all wrong from the start here instead:
 * <ul>
 * <li>the byte-copy loop's bound was the whole file's length (magic
 * included) rather than the payload length, so loading any real file
 * always threw partway through, past the point the copy had actually
 * finished;</li>
 * <li>{@code hexNotationPrefix} was never copied from the struct at all,
 * so a loaded profile always kept whatever hex prefix was already set;</li>
 * <li>{@code directiveBYTENumberOfBytesPerLine} was set from the
 * unrelated {@code DisplayOpcodes} setting's value instead of its own
 * {@code NbBytesPerLine} lookup (its two siblings, {@code
 * NbCharPerString}/{@code NbWordsPerLine}, do look up their own
 * settings).</li>
 * </ul>
 *
 * @author Peter Dell
 */
public final class Profile1X {

	public static final int MAGIC_LENGTH = 12;
	private static final String MAGIC10 = "DIS6502PRF10";
	private static final String MAGIC13 = "DIS6502PRF13";
	private static final String MAGIC16 = "DIS6502PRF16";
	private static final String MAGIC17 = "DIS6502PRF17";

	// PROFILE17's field layout: 20 byte fixed ANSI strings and 4 byte ints/bools, laid out with
	// MSVC's default struct alignment (a 2 byte word field and 2 bytes of trailing padding
	// partway through) - see the field offset table below. Verified against a real fixture,
	// see this class's javadoc.
	private static final int STRUCT_SIZE = 408;

	private static final int STRING_LENGTH = 20;

	private Profile1X() {
	}

	/**
	 * Attempts to load {@code profile} from {@code buffer} (the whole file's raw
	 * bytes, magic included). Returns {@code false}, leaving {@code profile}
	 * unchanged, if the magic doesn't match one of the four supported versions.
	 */
	public static boolean load(Profile profile, byte[] buffer, Application application) {
		if (buffer.length < MAGIC_LENGTH) {
			return false;
		}
		String magic = new String(buffer, 0, MAGIC_LENGTH, StandardCharsets.US_ASCII);
		if (!MAGIC10.equals(magic) && !MAGIC13.equals(magic) && !MAGIC16.equals(magic) && !MAGIC17.equals(magic)) {
			return false;
		}

		byte[] struct = new byte[STRUCT_SIZE];
		int payloadSize = Math.min(buffer.length - MAGIC_LENGTH, STRUCT_SIZE);
		System.arraycopy(buffer, MAGIC_LENGTH, struct, 0, payloadSize);

		// For compatibility with the previous storage system, some settings are defaulted
		// from the default config section.
		ApplicationSettingsSection defaultConfig = application.getSettingsSection("DefaultConfig");

		profile.alignInstructions = getBool(struct, 48);
		profile.directiveBYTEOnlyNumbersAllowed = getBool(struct, 300);
		profile.directiveDSAllowed = getBool(struct, 388);
		profile.directiveENDNeedsFilename = getBool(struct, 312);
		profile.directiveINCLUDEAllEquatesInOneIncludeFile = false; // Did not exist.
		profile.directiveINCLUDEAllIncludesInMainFile = getBool(struct, 360);
		profile.directiveINCLUDEAllowed = getBool(struct, 364);
		profile.directiveSBYTEAllowed = getBool(struct, 308);
		profile.directiveWORDAllowed = getBool(struct, 304);
		profile.omitUnreferencedSystemLabels = true; // Did not exist.
		profile.showAInAccumulatorMode = getBool(struct, 56);
		profile.showBRKAsByte0 = getBool(struct, 392);
		profile.showColonAfterLabel = getBool(struct, 400);
		profile.showLowerCaseInstructions = getBool(struct, 396);
		profile.showNonASCIIChararactersAsBytes = true; // Did not exist.
		profile.showOpcodeAsComment = defaultConfig.getUnsignedInt("DisplayOpcodes", 0) != 0;
		profile.showZPAbsoluteAsByte = getBool(struct, 404);
		profile.useHexNotation = getBool(struct, 52);
		profile.useIllegalOpcodes = getBool(struct, 40);
		profile.useLineNumbers = getBool(struct, 44);

		profile.commentPrefix = getString(struct, 0);
		profile.hexNotationPrefix = getString(struct, 20);
		profile.directiveBYTE = getString(struct, 60);
		profile.directiveBYTESeparator = getString(struct, 280);
		profile.directiveDS = getString(struct, 368);
		profile.directiveENDHead = getString(struct, 160);
		profile.directiveENDTail = getString(struct, 180);
		profile.directiveEQU = getString(struct, 140);
		profile.directiveHIGHHead = getString(struct, 240);
		profile.directiveHIGHTail = getString(struct, 260);
		profile.directiveINCLUDEHead = getString(struct, 316);
		profile.directiveINCLUDETail = getString(struct, 336);
		profile.directiveLOWHead = getString(struct, 200);
		profile.directiveLOWTail = getString(struct, 220);
		profile.directiveORG = getString(struct, 120);
		profile.directiveSBYTE = getString(struct, 100);
		profile.directiveWORD = getString(struct, 80);

		profile.directiveBYTENumberOfBytesPerLine = defaultConfig.getUnsignedInt("NbBytesPerLine", 16);
		profile.directiveBYTENumberOfCharactersPerString = defaultConfig.getUnsignedInt("NbCharPerString", 40);
		profile.directiveINCLUDEMaximumNumberOfLinesPerFile = getUnsignedShort(struct, 356);
		profile.directiveWORDNumberOfWordsPerLine = defaultConfig.getUnsignedInt("NbWordsPerLine", 8);

		return true;
	}

	private static boolean getBool(byte[] struct, int offset) {
		return getInt(struct, offset) != 0;
	}

	private static int getInt(byte[] struct, int offset) {
		return (struct[offset] & 0xFF) | ((struct[offset + 1] & 0xFF) << 8) | ((struct[offset + 2] & 0xFF) << 16)
				| ((struct[offset + 3] & 0xFF) << 24);
	}

	private static int getUnsignedShort(byte[] struct, int offset) {
		return (struct[offset] & 0xFF) | ((struct[offset + 1] & 0xFF) << 8);
	}

	/** Decodes a fixed 20 byte, NUL-terminated (or fully occupied) ANSI string field. */
	private static String getString(byte[] struct, int offset) {
		int end = offset;
		while (end < offset + STRING_LENGTH && struct[end] != 0) {
			end++;
		}
		return new String(struct, offset, end - offset, StandardCharsets.ISO_8859_1);
	}
}
