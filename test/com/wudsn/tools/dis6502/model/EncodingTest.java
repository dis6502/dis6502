/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * New (not ported) test for {@link Encoding} as a WUDSN Base value set: its
 * texts come from {@code ValueSets.properties}, its keys stay what {@link
 * Profile} files contain.
 *
 * @author Peter Dell
 */
public final class EncodingTest {

	private EncodingTest() {
	}

	public static void testEncoding() throws IOException {
		// Display texts - what the profile dialog's drop-down shows.
		Assert.stringEquals(Encoding.UTF8.getText(), "UTF-8");
		Assert.stringEquals(Encoding.UTF8.toString(), "UTF-8");
		Assert.stringEquals(Encoding.ATASCII.getText(), "ATASCII");
		Assert.stringEquals(Encoding.BINARY.getText(), "Binary");
		Assert.stringEquals(Encoding.UNKNOWN.getText(), "Unknown");

		// Keys - what a profile file contains. They must never follow the texts.
		Assert.stringEquals(Encoding.UTF8.getKey(), "UTF8");
		Assert.boolEquals(Encoding.fromKey("UTF8") == Encoding.UTF8, true);
		Assert.boolEquals(Encoding.fromKey("UTF-8") == Encoding.UNKNOWN, true);
		Assert.boolEquals(Encoding.fromKey("") == Encoding.UNKNOWN, true);

		// All values in declaration order; only real text encodings on offer.
		Assert.longEquals(Encoding.getValues().size(), 5);
		Assert.boolEquals(Encoding.getValues().get(0) == Encoding.UNKNOWN, true);
		Assert.boolEquals(Encoding.getValues().get(4) == Encoding.UTF8, true);
		Assert.longEquals(Encoding.getOutputValues().size(), 3);
		Assert.boolEquals(Encoding.getOutputValues().contains(Encoding.BINARY), false);
		Assert.boolEquals(Encoding.getOutputValues().contains(Encoding.UNKNOWN), false);

		// Through a profile file and back, by key.
		Profile profile = new Profile();
		profile.clear();
		profile.outputEncoding = Encoding.UTF8;
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		Xml.save(profile, "Profile", outputStream);
		Assert.boolEquals(outputStream.toString("UTF-8").contains("OutputEncoding=\"UTF8\""), true);

		File file = File.createTempFile("dis6502-encoding-", ".prf");
		file.deleteOnExit();
		Files.write(file.toPath(), outputStream.toByteArray());
		Profile loadedProfile = new Profile();
		Xml.load(loadedProfile, "Profile", file);
		Assert.boolEquals(loadedProfile.outputEncoding == Encoding.UTF8, true);

		// An encoding no listing can be written in falls back to ASCII when loaded.
		Files.write(file.toPath(), outputStream.toString("UTF-8").replace("OutputEncoding=\"UTF8\"", "OutputEncoding=\"BINARY\"").getBytes("UTF-8"));
		Xml.load(loadedProfile, "Profile", file);
		Assert.boolEquals(loadedProfile.outputEncoding == Encoding.ASCII, true);

		testAtasciiOutput();

		Assert.log("EncodingTest completed");
	}

	/**
	 * An ATASCII character is one byte: 255 is the last one that fits, and
	 * anything above must be refused instead of being truncated to some
	 * other character.
	 */
	private static void testAtasciiOutput() throws IOException {
		Profile profile = new Profile();
		profile.clear();
		profile.outputEncoding = Encoding.ATASCII;
		profile.useLineNumbers = false;

		File file = File.createTempFile("dis6502-atascii-", ".asm");
		file.deleteOnExit();
		DisassemblyResultWriter writer = new DisassemblyResultWriter(profile);
		writer.openFile(file);
		writer.printLine("Aÿ", false);
		writer.close();
		byte[] bytes = Files.readAllBytes(file.toPath());
		Assert.longEquals(bytes.length, 3);
		Assert.longEquals(bytes[0] & 0xFF, 'A');
		Assert.longEquals(bytes[1] & 0xFF, 0xFF);
		Assert.longEquals(bytes[2] & 0xFF, 0x9B); // The ATASCII end of line.

		writer = new DisassemblyResultWriter(profile);
		writer.openFile(file);
		boolean refused = false;
		try {
			writer.printLine("AĀ", false); // 256: would have been written as byte 0.
		} catch (IOException ex) {
			refused = true;
		} finally {
			writer.close();
		}
		Assert.boolEquals(refused, true);
	}
}
