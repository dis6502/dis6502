/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

import com.wudsn.tools.dis6502.model.system.ComputerSystemFactory;
import com.wudsn.tools.dis6502.model.system.ComputerSystemType;

/**
 * {@link CharacterSet}: every {@code .chr} file loads, known glyphs have
 * their known bit patterns, the byte and text mappings, and each computer
 * system's default.
 *
 * @author Peter Dell
 */
public final class CharacterSetTest {

	private CharacterSetTest() {
	}

	public static void testCharacterSet() {
		Assert.longEquals(CharacterSet.getValues().size(), 5);
		for (CharacterSet characterSet : CharacterSet.getValues()) {
			// Screen code 255 is the last of 256 glyphs - loading fails on any other file size.
			Assert.longEquals(characterSet.getGlyph(255).length, 8);
		}

		assertGlyph(CharacterSet.ATASCII_STANDARD, 0x21, 0x00, 0x18, 0x3C, 0x66, 0x66, 0x7E, 0x66, 0x00); // 'A'.
		assertGlyph(CharacterSet.ATASCII_STANDARD, 0xA1, 0xFF, 0xE7, 0xC3, 0x99, 0x99, 0x81, 0x99, 0xFF); // Inverse 'A'.
		assertGlyph(CharacterSet.PETSCII_UPPERCASE, 0x01, 0x18, 0x3C, 0x66, 0x7E, 0x66, 0x66, 0x66, 0x00); // 'A'.
		assertGlyph(CharacterSet.ORIC_ASCII, 0x41, 0x08, 0x14, 0x22, 0x22, 0x3E, 0x22, 0x22, 0x00); // 'A', from the ROM.
		assertGlyph(CharacterSet.ORIC_ASCII, 0xC1, 0xF7, 0xEB, 0xDD, 0xDD, 0xC1, 0xDD, 0xDD, 0xFF); // Inverse 'A', whole cell.
		assertGlyph(CharacterSet.ORIC_ASCII, 0x01, 0, 0, 0, 0, 0, 0, 0, 0); // Attribute code: blank.
		assertGlyph(CharacterSet.PETSCII_LOWERCASE, 0x41, 0x18, 0x3C, 0x66, 0x7E, 0x66, 0x66, 0x66, 0x00); // 'A'.

		// ATASCII to ANTIC internal code, inverse bit kept.
		CharacterSet atari = CharacterSet.ATASCII_STANDARD;
		Assert.longEquals(atari.toScreenCode(0x00), 0x40);
		Assert.longEquals(atari.toScreenCode(0x20), 0x00);
		Assert.longEquals(atari.toScreenCode(0x41), 0x21);
		Assert.longEquals(atari.toScreenCode(0x61), 0x61);
		Assert.longEquals(atari.toScreenCode(0xC1), 0xA1);

		// PETSCII to C64 screen code.
		CharacterSet c64 = CharacterSet.PETSCII_UPPERCASE;
		Assert.longEquals(c64.toScreenCode(0x20), 0x20);
		Assert.longEquals(c64.toScreenCode(0x41), 0x01);
		Assert.longEquals(c64.toScreenCode(0x61), 0x41);
		Assert.longEquals(c64.toScreenCode(0xC1), 0x41);
		Assert.longEquals(c64.toScreenCode(0xFF), 0x5E);

		// ASCII: the byte is the screen code.
		CharacterSet oric = CharacterSet.ORIC_ASCII;
		Assert.longEquals(oric.toScreenCode(0x41), 0x41);
		Assert.longEquals(oric.toScreenCode(0xC1), 0xC1);
		Assert.longEquals(oric.textToScreenCode('|'), 0x7C);
		Assert.longEquals(oric.textToByte('a'), 0x61);
		Assert.longEquals(oric.getReturnByte(), 0x0D);

		// This port's own text: the glyph that looks like the ASCII character.
		Assert.longEquals(atari.textToScreenCode('A'), 0x21);
		Assert.longEquals(atari.textToScreenCode('0'), 0x10);
		Assert.longEquals(atari.textToScreenCode('|'), 0x7C);
		Assert.longEquals(c64.textToScreenCode('A'), 0x01);
		Assert.longEquals(c64.textToScreenCode('a'), 0x01);
		Assert.longEquals(c64.textToScreenCode('|'), 0x5D);
		Assert.longEquals(CharacterSet.PETSCII_LOWERCASE.textToScreenCode('A'), 0x41);
		Assert.longEquals(CharacterSet.PETSCII_LOWERCASE.textToScreenCode('a'), 0x01);

		// Typed text: the encoding's byte that shows the character, and the encoding's end-of-line byte.
		Assert.longEquals(atari.textToByte('A'), 0x41);
		Assert.longEquals(atari.textToByte('~'), 0x7E);
		Assert.longEquals(c64.textToByte('A'), 0x41);
		Assert.longEquals(c64.textToByte('a'), 0x41);
		Assert.longEquals(CharacterSet.PETSCII_LOWERCASE.textToByte('a'), 0x41);
		Assert.longEquals(CharacterSet.PETSCII_LOWERCASE.textToByte('A'), 0x61);
		for (char ch = ' '; ch < 0x7F; ch++) {
			for (CharacterSet characterSet : CharacterSet.getValues()) {
				Assert.longEquals(characterSet.toScreenCode(characterSet.textToByte(ch)), characterSet.textToScreenCode(ch));
			}
		}
		Assert.longEquals(atari.getReturnByte(), 0x9B);
		Assert.longEquals(c64.getReturnByte(), 0x0D);

		ComputerSystemFactory factory = new ComputerSystemFactory();
		Assert.boolEquals(factory.getComputerSystem(ComputerSystemType.ATARI800).getDefaultCharacterSet() == atari, true);
		Assert.boolEquals(factory.getComputerSystem(ComputerSystemType.ATARI5200).getDefaultCharacterSet() == atari, true);
		Assert.boolEquals(factory.getComputerSystem(ComputerSystemType.C64).getDefaultCharacterSet() == c64, true);
		Assert.boolEquals(factory.getComputerSystem(ComputerSystemType.ORIC).getDefaultCharacterSet() == CharacterSet.ORIC_ASCII, true);
		Assert.boolEquals(factory.getComputerSystem(ComputerSystemType.UNKNOWN).getDefaultCharacterSet() == atari, true);

		// The workspace follows the computer system's default.
		Workspace workspace = new Workspace(factory);
		Assert.boolEquals(workspace.getViewCharacterSet() == atari, true);
		workspace.setComputerSystemType(ComputerSystemType.C64);
		Assert.boolEquals(workspace.getViewCharacterSet() == c64, true);
		workspace.setViewCharacterSet(CharacterSet.PETSCII_LOWERCASE);
		Assert.boolEquals(workspace.getViewCharacterSet() == CharacterSet.PETSCII_LOWERCASE, true);
		workspace.setComputerSystemType(ComputerSystemType.ATARI800);
		Assert.boolEquals(workspace.getViewCharacterSet() == atari, true);

		Assert.log("CharacterSetTest completed");
	}

	private static void assertGlyph(CharacterSet characterSet, int screenCode, int... rows) {
		byte[] glyph = characterSet.getGlyph(screenCode);
		for (int i = 0; i < rows.length; i++) {
			Assert.longEquals(glyph[i] & 0xFF, rows[i]);
		}
	}
}
