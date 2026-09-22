/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

/**
 * Covers {@link FolderType} as a WUDSN Base value set: its texts come
 * from {@code ValueSets.properties}, its keys stay what the settings
 * contain.
 *
 * @author Peter Dell
 */
public final class FolderTypeTest {

	private FolderTypeTest() {
	}

	public static void testFolderType() {
		// Display texts - the default folders dialog's row labels.
		Assert.stringEquals(FolderType.EXECUTABLE_FILES.getText(), "Executable Files");
		Assert.stringEquals(FolderType.ROM_IMAGE_FILES.toString(), "ROM Image Files");
		Assert.stringEquals(FolderType.UNKNOWN.getText(), "Unknown");
		for (FolderType folderType : FolderType.getValues()) {
			// A missing ValueSets.properties entry would leave the id as text.
			Assert.boolEquals(folderType.getText().isEmpty() || folderType.getText().equals(folderType.getKey()), false);
		}

		// Keys - what the settings are stored under.
		Assert.stringEquals(FolderType.EXECUTABLE_FILES.getKey(), "EXECUTABLE_FILES");
		Assert.stringEquals(FolderType.UNKNOWN.getKey(), "UNKNOWN");
		Assert.boolEquals(FolderType.fromKey("DISK_IMAGE_FILES") == FolderType.DISK_IMAGE_FILES, true);
		Assert.boolEquals(FolderType.fromKey("UNKNOWN") == FolderType.UNKNOWN, true);
		Assert.boolEquals(FolderType.fromKey("no such key") == FolderType.UNKNOWN, true);

		// All ten, in declaration order.
		Assert.longEquals(FolderType.getValues().size(), 10);
		Assert.boolEquals(FolderType.getValues().get(0) == FolderType.UNKNOWN, true);
		Assert.boolEquals(FolderType.getValues().get(9) == FolderType.DISASSEMBLY_FILES, true);

		// As a key of the default folders.
		DefaultFolders defaultFolders = new DefaultFolders(ComputerSystemType.C64);
		Assert.stringEquals(defaultFolders.getFolderPath(FolderType.WORKSPACE_FILES), "");
		defaultFolders.setFolderPath(FolderType.WORKSPACE_FILES, "some-folder");
		Assert.stringEquals(defaultFolders.getFolderPath(FolderType.WORKSPACE_FILES), "some-folder");
		Assert.stringEquals(defaultFolders.getFolderPath(FolderType.EQUATES_FILES), "");

		Assert.log("FolderTypeTest completed");
	}
}
