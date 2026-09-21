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
import com.wudsn.tools.dis6502.model.FolderType;

/**
 * New (not ported) test for the parts of {@link FileChoosers} that need no
 * dialog on screen, and for what each {@link FileType} tells them.
 *
 * @author Peter Dell
 */
public final class FileChoosersTest {

	private FileChoosersTest() {
	}

	public static void testFileChoosers() throws IOException {
		testFileType();
		testInitialFolder();
		testDefaultExtension();

		Assert.log("FileChoosersTest completed");
	}

	private static void testFileType() {
		Assert.longEquals(FileType.getValues().size(), 12);
		for (FileType fileType : FileType.getValues()) {
			// The text comes from ValueSets.properties - a missing entry would leave the id as text.
			Assert.boolEquals(fileType.getText().isEmpty() || fileType.getText().equals(fileType.getKey()), false);
			Assert.boolEquals(FileType.fromKey(fileType.getKey()) == fileType, true);
			for (String extension : fileType.getFilterExtensions()) {
				Assert.boolEquals(extension.startsWith(".") && extension.equals(extension.toLowerCase()), true);
			}
			// A type with a filter must save under one of the extensions it filters for.
			Assert.boolEquals(fileType.getFilterExtensions().isEmpty()
					|| fileType.getFilterExtensions().contains(fileType.getDefaultExtension()), true);
		}

		FileType executable = FileType.EXECUTABLE_FILE;
		Assert.stringEquals(executable.getKey(), "EXECUTABLE_FILE"); // What the most-recently-used lists contain.
		Assert.stringEquals(executable.getText(), "Executable File");
		Assert.stringEquals(executable.getFilterText(), "Executable Files");
		Assert.stringEquals(executable.getDefaultExtension(), ".xex");
		Assert.boolEquals(executable.getFolderType() == FolderType.EXECUTABLE_FILES, true);
		Assert.boolEquals(executable.getFilterExtensions().contains(".prg"), true); // C64 - the C++ list was Atari-only.
		Assert.boolEquals(FileType.fromKey("no such key") == FileType.ANY_FILE, true);

		// Three file types, one kind of file on disk - and one filter text.
		Assert.boolEquals(FileType.DISK_IMAGE_SECTORS.getFolderType() == FolderType.DISK_IMAGE_FILES, true);
		Assert.boolEquals(FileType.DISK_IMAGE_BOOT_SECTORS.getFolderType() == FolderType.DISK_IMAGE_FILES, true);
		Assert.stringEquals(FileType.DISK_IMAGE_EXECUTABLE_FILE.getFilterText(), "Disk Image Files");

		// Any file can be a raw file: no filter.
		Assert.boolEquals(FileType.RAW_FILE.getFilterExtensions().isEmpty(), true);
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
		FileType workspace = FileType.WORKSPACE_FILE;

		Assert.stringEquals(FileChoosers.withDefaultExtension(new File(folder, "game"), workspace).getPath(),
				new File(folder, "game.wrk").getPath());
		// An extension the user typed is respected, whatever it is.
		Assert.stringEquals(FileChoosers.withDefaultExtension(new File(folder, "game.bak"), workspace).getPath(),
				new File(folder, "game.bak").getPath());
		// No sensible default: left alone.
		Assert.stringEquals(FileChoosers.withDefaultExtension(new File(folder, "game"), FileType.ANY_FILE).getPath(),
				new File(folder, "game").getPath());
	}
}
