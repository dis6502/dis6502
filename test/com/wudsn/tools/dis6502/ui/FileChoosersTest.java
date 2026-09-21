/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.ui;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import com.wudsn.tools.dis6502.model.Assert;
import com.wudsn.tools.dis6502.model.FileType;
import com.wudsn.tools.dis6502.model.FileTypeInfo;
import com.wudsn.tools.dis6502.model.FolderType;

/**
 * New (not ported) test for the parts of {@link FileChoosers} that need no
 * dialog on screen, and for the {@link FileTypeInfo} table they are driven
 * by.
 *
 * @author Peter Dell
 */
public final class FileChoosersTest {

	private FileChoosersTest() {
	}

	public static void testFileChoosers() throws IOException {
		testFileTypeInfo();
		testInitialFolder();
		testDefaultExtension();

		Assert.log("FileChoosersTest completed");
	}

	private static void testFileTypeInfo() {
		for (FileType fileType : FileType.values()) {
			FileTypeInfo info = FileTypeInfo.get(fileType);
			Assert.boolEquals(info.fileType == fileType, true);
			// The texts come from Texts.properties - a missing entry would leave them null.
			Assert.notNull(info.text);
			Assert.notNull(info.filterText);
			Assert.boolEquals(info.text.isEmpty() || info.filterText.isEmpty(), false);
			for (String extension : info.filterExtensions) {
				Assert.boolEquals(extension.startsWith(".") && extension.equals(extension.toLowerCase()), true);
			}
			// A type with a filter must save under one of the extensions it filters for.
			Assert.boolEquals(info.filterExtensions.isEmpty() || info.filterExtensions.contains(info.defaultExtension), true);
		}

		FileTypeInfo executable = FileTypeInfo.get(FileType.EXECUTABLE_FILE);
		Assert.stringEquals(executable.text, "Executable File");
		Assert.stringEquals(executable.filterText, "Executable Files");
		Assert.stringEquals(executable.defaultExtension, ".xex");
		Assert.boolEquals(executable.folderType == FolderType.EXECUTABLE_FILES, true);
		Assert.boolEquals(executable.filterExtensions.contains(".prg"), true); // C64 - the C++ list was Atari-only.

		// Three file types, one kind of file on disk.
		Assert.boolEquals(FileTypeInfo.get(FileType.DISK_IMAGE_SECTORS).folderType == FolderType.DISK_IMAGE_FILES, true);
		Assert.boolEquals(FileTypeInfo.get(FileType.DISK_IMAGE_BOOT_SECTORS).folderType == FolderType.DISK_IMAGE_FILES, true);

		// Any file can be a raw file: no filter.
		Assert.boolEquals(FileTypeInfo.get(FileType.RAW_FILE).filterExtensions.isEmpty(), true);
	}

	private static void testInitialFolder() throws IOException {
		File suggestedFolder = Files.createTempDirectory("dis6502-suggested-").toFile();
		File lastFolder = Files.createTempDirectory("dis6502-last-").toFile();
		File mruFolder = Files.createTempDirectory("dis6502-mru-").toFile();
		File defaultFolder = Files.createTempDirectory("dis6502-default-").toFile();
		File suggestedFile = new File(suggestedFolder, "game.wrk"); // Need not exist - only its folder counts.
		File mruFile = new File(mruFolder, "game.xex");
		File goneFolder = new File(defaultFolder, "deleted-since");

		// In order of preference.
		Assert.stringEquals(FileChoosers.getInitialFolder(suggestedFile, lastFolder, mruFile, defaultFolder).getPath(),
				suggestedFolder.getPath());
		Assert.stringEquals(FileChoosers.getInitialFolder(null, lastFolder, mruFile, defaultFolder).getPath(), lastFolder.getPath());
		Assert.stringEquals(FileChoosers.getInitialFolder(null, null, mruFile, defaultFolder).getPath(), mruFolder.getPath());
		Assert.stringEquals(FileChoosers.getInitialFolder(null, null, null, defaultFolder).getPath(), defaultFolder.getPath());

		// A folder that no longer exists is skipped, not handed to the chooser.
		Assert.stringEquals(FileChoosers.getInitialFolder(null, goneFolder, new File(goneFolder, "game.xex"), defaultFolder).getPath(),
				defaultFolder.getPath());
		Assert.boolEquals(FileChoosers.getInitialFolder(null, null, null, goneFolder) == null, true);
		Assert.boolEquals(FileChoosers.getInitialFolder(null, null, null, null) == null, true);
	}

	private static void testDefaultExtension() {
		File folder = new File("some-folder");
		FileTypeInfo workspace = FileTypeInfo.get(FileType.WORKSPACE_FILE);

		Assert.stringEquals(FileChoosers.withDefaultExtension(new File(folder, "game"), workspace).getPath(),
				new File(folder, "game.wrk").getPath());
		// An extension the user typed is respected, whatever it is.
		Assert.stringEquals(FileChoosers.withDefaultExtension(new File(folder, "game.bak"), workspace).getPath(),
				new File(folder, "game.bak").getPath());
		// No sensible default: left alone.
		Assert.stringEquals(FileChoosers.withDefaultExtension(new File(folder, "game"), FileTypeInfo.get(FileType.UNKNOWN_FILE)).getPath(),
				new File(folder, "game").getPath());
	}
}
