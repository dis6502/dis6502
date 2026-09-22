/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import com.wudsn.tools.dis6502.Application;

/**
 * Exercises {@code saveListing} for every include-file variant without
 * crashing - it does not compare the produced listing against a reference
 * file. Writes into a freshly created temporary directory (logged, so it
 * can still be inspected manually).
 *
 * @author Peter Dell
 */
public final class DisassemblyResultFileTest {

	private DisassemblyResultFileTest() {
	}

	public static void testDisassemblyResultFile() throws IOException {

		Assert.stringEquals(DisassemblyResultFile.createIncludeFilePath("example", 0), "example.inc");
		Assert.stringEquals(DisassemblyResultFile.createIncludeFilePath("example.", 0), "example.inc");
		Assert.stringEquals(DisassemblyResultFile.createIncludeFilePath("example.ext", 0), "example.inc");

		Assert.stringEquals(DisassemblyResultFile.createIncludeFilePath("example", 1), "example.001");
		Assert.stringEquals(DisassemblyResultFile.createIncludeFilePath("example.", 2), "example.002");
		Assert.stringEquals(DisassemblyResultFile.createIncludeFilePath("example.ext", 3), "example.003");

		DisassemblyResult result = new DisassemblyResult();
		DisassemblyResultTest.generateDisassemblyResult(result, 10);

		File folder = Files.createTempDirectory("dis6502-test-").toFile();
		Assert.log("Writing disassembly result listings to " + folder.getPath());

		Profile profile = new Profile();
		profile.omitUnreferencedSystemLabels = false;

		profile.directiveINCLUDEAllowed = false;
		testSaveListing(folder, result, profile, "1-noinc");

		profile.directiveINCLUDEAllowed = true;
		profile.directiveINCLUDEAllEquatesInOneIncludeFile = true;
		testSaveListing(folder, result, profile, "2-oneinc");

		profile.directiveINCLUDEAllEquatesInOneIncludeFile = false;
		profile.directiveINCLUDEMaximumNumberOfLinesPerFile = 10;

		profile.directiveINCLUDEAllIncludesInMainFile = true;
		testSaveListing(folder, result, profile, "3-maininc");

		profile.directiveINCLUDEAllEquatesInOneIncludeFile = false;
		profile.directiveINCLUDEAllIncludesInMainFile = false;
		testSaveListing(folder, result, profile, "4-chaininc");
	}

	private static void testSaveListing(File folder, DisassemblyResult result, Profile profile, String variant)
			throws IOException {
		File variantFolder = new File(folder, variant);
		if (!variantFolder.mkdirs()) {
			throw new IOException("Cannot create folder '" + variantFolder + "'.");
		}
		File file = new File(variantFolder, "DisassemblyResultFileTest.asm");
		Assert.log("Saving disassembly result to " + file.getPath());
		DisassemblyResultFile disassemblyResultFile = new DisassemblyResultFile(new Application());
		disassemblyResultFile.saveListing(result, profile, file);
	}
}
