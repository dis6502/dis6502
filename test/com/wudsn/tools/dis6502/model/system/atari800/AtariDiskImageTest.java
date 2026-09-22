/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model.system.atari800;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

import com.wudsn.tools.dis6502.model.Assert;

/**
 * {@link #testAtariDiskImage} exercises real {@code .atr} test disk
 * images and their {@code .files} reference-content folders, copied into
 * {@code test-resources/diskimage/}. {@code DISK_NOT_FOUND.atr} is
 * deliberately not one of those copied files, so its absence exercises
 * {@link AtariError#DISK_NOT_FOUND}.
 * <p>
 * Exercises {@link AtariDisk#readFile(String)}, the entry point that
 * reads a named file's full content, using plain {@code byte[]}/{@code
 * java.nio.file} throughout.
 *
 * @author Peter Dell
 */
public final class AtariDiskImageTest {

	private AtariDiskImageTest() {
	}

	public static void testAtariDiskImage() throws IOException {
		assertFileNames83Equal();

		assertDirectoryEquals("test-resources/diskimage/DISK_NOT_FOUND.atr", "DISK_NOT_FOUND\n");
		assertDirectoryEquals("test-resources/diskimage/DOS_20S_SD.atr",
				"  DOS     .SYS 039\n  DUP     .SYS 042\n  AUTORUN .SYS 001\nNO_ENTRY_FOUND\n");
		assertDirectoryEquals("test-resources/diskimage/DOS_25_SD.atr", "  DOS     .SYS 037\n  DUP     .SYS 042\nNO_ENTRY_FOUND\n");
		assertDirectoryEquals("test-resources/diskimage/DOS_25_ED.atr",
				"* DOS     .SYS 037\n* DUP     .SYS 042\n* RAMDISK .COM 009\n* SETUP   .COM 070\n* COPY32  .COM 056\n* DISKFIX .COM 057\nNO_ENTRY_FOUND\n");
	}

	private static void assertFileNames83Equal() {
		assertFileName83Equals("", "");
		assertFileName83Equals("DOS.SYS", "DOS     .SYS");
		assertFileName83Equals("AUTORUN.SYS", "AUTORUN .SYS");
		assertFileName83Equals("ABCDEFGH.ABC", "ABCDEFGH.ABC");
	}

	private static void assertFileName83Equals(String fileName, String expectedFileName83) {
		Assert.log("Checking file name " + fileName);
		Assert.stringEquals(AtariDOS.getFileName83(fileName), expectedFileName83);
	}

	private static void assertDirectoryEquals(String diskImageFilePath, String expectedDirectory) throws IOException {
		Assert.log("Checking directory of disk image " + diskImageFilePath);
		Assert.stringEquals(getDirectory(diskImageFilePath), expectedDirectory);

		// Also compare the actual contents retrieved from the disk image with the reference copies in the ".files" folder.
		AtariDisk atariDisk = AtariDOS.openAtariDisk(diskImageFilePath);
		AtariFile info = new AtariFile();
		AtariError error = atariDisk.findFirst(info);
		while (error == AtariError.OK) {
			String fileName = info.getFileName();
			String expectedFilePath = diskImageFilePath + ".files/" + fileName;
			assertFileContentEquals(atariDisk, fileName, expectedFilePath);
			error = atariDisk.findNext(info);
		}
	}

	private static String getDirectory(String diskImageFilePath) throws IOException {
		StringBuilder result = new StringBuilder();
		AtariDisk atariDisk = AtariDOS.openAtariDisk(diskImageFilePath);
		AtariFile info = new AtariFile();
		AtariError error = atariDisk.findFirst(info);
		while (error == AtariError.OK) {
			result.append(info.getDirectoryText()).append('\n');
			error = atariDisk.findNext(info);
		}
		result.append(error.name()).append('\n');
		return result.toString();
	}

	private static void assertFileContentEquals(AtariDisk atariDisk, String fileName, String expectedFilePath) throws IOException {
		Assert.log("Checking file " + fileName);

		byte[] actualFileContent = atariDisk.readFile(fileName);
		byte[] expectedFileContent = Files.readAllBytes(Paths.get(expectedFilePath));

		Assert.longEquals(actualFileContent.length, expectedFileContent.length);
		if (!Arrays.equals(actualFileContent, expectedFileContent)) {
			Assert.fail("Different file content for " + fileName);
		}
	}
}
