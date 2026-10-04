/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model.system.atari800;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
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
		testWriteAbsoluteSector();

		assertDirectoryEquals("test-resources/diskimage/DISK_NOT_FOUND.atr", "DISK_NOT_FOUND\n");
		assertDirectoryEquals("test-resources/diskimage/DOS_20S_SD.atr",
				"  DOS     .SYS 039\n  DUP     .SYS 042\n  AUTORUN .SYS 001\nNO_ENTRY_FOUND\n");
		assertDirectoryEquals("test-resources/diskimage/DOS_25_SD.atr", "  DOS     .SYS 037\n  DUP     .SYS 042\nNO_ENTRY_FOUND\n");
		assertDirectoryEquals("test-resources/diskimage/DOS_25_ED.atr",
				"* DOS     .SYS 037\n* DUP     .SYS 042\n* RAMDISK .COM 009\n* SETUP   .COM 070\n* COPY32  .COM 056\n* DISKFIX .COM 057\nNO_ENTRY_FOUND\n");
	}

	/**
	 * {@link DiskImage#writeAbsoluteSector} reports the result of a write: the
	 * disk image type on success, otherwise the error - which the boot disk
	 * dialog shows. Works on a copy of a test disk image.
	 */
	private static void testWriteAbsoluteSector() throws IOException {
		File copy = File.createTempFile("AtariDiskImageTest", ".atr");
		try {
			Files.copy(new File("test-resources/diskimage/DOS_25_SD.atr").toPath(), copy.toPath(),
					StandardCopyOption.REPLACE_EXISTING);
			byte[] data = new byte[128];
			Arrays.fill(data, (byte) 0x42);

			Assert.boolEquals(DiskImage.writeAbsoluteSector(copy.getPath(), 1, data) == ImgError.ATR, true);
			ImgRWPacket sector = new ImgRWPacket();
			sector.filePath = copy.getPath();
			DiskImage.readAbsoluteSector(sector, 1, new int[1]);
			Assert.longEquals(sector.sectorData[0] & 0xFF, 0x42);
			Assert.longEquals(sector.sectorData[127] & 0xFF, 0x42);

			Assert.boolEquals(DiskImage.writeAbsoluteSector(copy.getPath(), 99999, data) == ImgError.OUT_OF_RANGE,
					true);
			Assert.boolEquals(copy.setReadOnly(), true);
			Assert.boolEquals(DiskImage.writeAbsoluteSector(copy.getPath(), 1, data) == ImgError.WRITE_PROTECT, true);
			Assert.stringEquals(DiskImage.getErrorText(ImgError.WRITE_PROTECT), "Disk image is write protected.");
			Assert.stringEquals(DiskImage.getErrorText(ImgError.ATR), "");
		} finally {
			copy.setWritable(true);
			Files.delete(copy.toPath());
		}
		Assert.boolEquals(DiskImage.writeAbsoluteSector(copy.getPath(), 1, new byte[128]) == ImgError.FILE_NOT_FOUND,
				true);
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
