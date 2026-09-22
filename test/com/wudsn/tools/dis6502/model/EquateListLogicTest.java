/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import com.wudsn.tools.dis6502.Application;

/**
 * Exercises {@link EquateListLogic} against the real {@code
 * systems/atari800/Atari800.equ} fixture and a save/reload round-trip -
 * including the newline-separator bug found and fixed in {@link
 * EquateListLogic#save} (see its javadoc).
 *
 * @author Peter Dell
 */
public final class EquateListLogicTest {

	private static final String FIXTURE_PATH = "test-resources/equates/Atari800.equ";

	private EquateListLogicTest() {
	}

	public static void testEquateListLogic() throws IOException {
		Application application = new Application();
		EquateListLogic equateListLogic = new EquateListLogic(application);

		EquateList equateList = new EquateList(WorkspaceProperty.SYSTEM_EQUATES);
		Assert.boolEquals(equateListLogic.load(equateList, FIXTURE_PATH), true);
		Assert.boolEquals(equateList.getCount() > 0, true);
		Assert.boolEquals(equateList.getLabelCount() > 0, true);

		Equate ichid = equateList.getEquateByLabel("ICHID");
		Assert.notNull(ichid);
		Assert.stringEquals(LabelAccess.getKey(ichid.getLabelAccess()), LabelAccess.getKey(LabelAccess.IMMEDIATE));
		Assert.longEquals(ichid.getLabelValue(), 0);

		Equate ngflag = equateList.getEquateByLabel("NGFLAG");
		Assert.notNull(ngflag);
		Assert.stringEquals(LabelAccess.getKey(ngflag.getLabelAccess()), LabelAccess.getKey(LabelAccess.READ_WRITE));
		Assert.longEquals(ngflag.getLabelValue(), 0x01);

		testSaveReloadRoundTrip(equateListLogic, equateList);
		testSaveXasm(equateListLogic, equateList);

		Assert.log("EquateListLogicTest completed");
	}

	/**
	 * Saves {@code equateList} to a temp file and reloads it into a fresh
	 * list, verifying every equate survives the round trip. This is the
	 * regression test for the bug found while porting {@link
	 * EquateListLogic#save}: without a trailing newline after each equate
	 * line, everything past the first equate would be lost on reload.
	 */
	private static void testSaveReloadRoundTrip(EquateListLogic equateListLogic, EquateList equateList)
			throws IOException {
		File tempFile = File.createTempFile("EquateListLogicTest", ".equ");
		tempFile.deleteOnExit();
		try {
			equateListLogic.save(equateList, tempFile.getPath(), false);

			EquateList reloaded = new EquateList(WorkspaceProperty.SYSTEM_EQUATES);
			Assert.boolEquals(equateListLogic.load(reloaded, tempFile.getPath()), true);
			Assert.longEquals(reloaded.getCount(), equateList.getCount());
			Assert.longEquals(reloaded.getLabelCount(), equateList.getLabelCount());

			Equate ichid = reloaded.getEquateByLabel("ICHID");
			Assert.notNull(ichid);
			Assert.longEquals(ichid.getLabelValue(), 0);

			Equate jkeyon = reloaded.getEquateByLabel("JKEYON");
			Assert.notNull(jkeyon);
			Assert.longEquals(jkeyon.getLabelValue(), 0xFFD5);
		} finally {
			Files.deleteIfExists(tempFile.toPath());
		}
	}

	/**
	 * Verifies the XASM 3.0.0 label table format's header and one formatted
	 * line. Every equate list entry is written, including the non-label
	 * (comment/empty) ones the fixture also has, rather than filtering by
	 * {@link EquateType#LABEL} - so the expected line is located by content
	 * rather than by a fixed line number.
	 */
	private static void testSaveXasm(EquateListLogic equateListLogic, EquateList equateList) throws IOException {
		File tempFile = File.createTempFile("EquateListLogicTest", ".xasm");
		tempFile.deleteOnExit();
		try {
			equateListLogic.save(equateList, tempFile.getPath(), true);

			List<String> lines = Files.readAllLines(tempFile.toPath());
			Assert.stringEquals(lines.get(0), "xasm 3.0.0");
			Assert.stringEquals(lines.get(1), "Label table:");
			Assert.boolEquals(lines.contains("        0000 ICHID"), true);
			Assert.boolEquals(lines.contains("        FFD5 JKEYON"), true);
		} finally {
			Files.deleteIfExists(tempFile.toPath());
		}
	}
}
