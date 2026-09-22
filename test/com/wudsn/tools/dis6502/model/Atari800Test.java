/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;

/**
 * {@link #assertSegmentListEquals} is generic, despite the name, and used
 * by {@link ComputerSystemTest} to test every {@link ComputerSystem}, not
 * just Atari800.
 * <p>
 * Only the plain-file overload exists; reading a file out of a disk image
 * is not covered here - that subsystem is only ever exercised by tests,
 * never by production code (see {@code AtariDOS}/{@code AtariDisk}, which
 * is the from-scratch DOS 2.x directory reader actually used at runtime).
 * <p>
 * The produced XML is loaded back into a {@link SegmentList} and compared
 * to the reference structurally, not as raw text: the checked-in
 * reference XML fixtures predate this tool and have their own attribute
 * ordering, which {@code org.w3c.dom}'s output does not reproduce, so a
 * byte-for-byte text comparison would fail even for semantically
 * identical content. The exported binary file is still compared
 * byte-for-byte, since that format has no such ambiguity.
 *
 * @author Peter Dell
 */
public final class Atari800Test {

	private Atari800Test() {
	}

	public static void assertSegmentListEquals(ComputerSystem computerSystem, FileType fileType, String filePath,
			String outFilePath) throws IOException {
		File file = new File(filePath);
		long fileSize = file.length();
		try (InputStream inputStream = new FileInputStream(file)) {
			assertSegmentListEquals(computerSystem, fileType, filePath, inputStream, fileSize, outFilePath);
		}
	}

	private static void assertSegmentListEquals(ComputerSystem computerSystem, FileType fileType, String filePath,
			InputStream inputStream, long fileSize, String outFilePath) throws IOException {

		// Read the source file.
		Assert.log("Reading " + filePath + " as type " + fileType);
		SegmentList actualSegmentList = new SegmentList(null); // SegmentList without equate handling.
		SegmentListInserter segmentListInserter = actualSegmentList.createInserter();
		try {
			computerSystem.readFile(fileType, inputStream, fileSize, segmentListInserter);
			segmentListInserter.apply();
		} catch (IOException ex) {
			segmentListInserter.cancel();
			Assert.log(ex);
			Assert.fail("Computer system cannot parse input stream");
		}
		Assert.log(actualSegmentList.getCount() + " segments detected.");

		// Load the expected segment list from the checked-in reference XML.
		SegmentList expectedSegmentList = new SegmentList(null); // SegmentList without equate handling.
		Xml.load(expectedSegmentList, "Segments", new File(filePath + ".xml"));

		// Assert the segment lists are equal.
		assertSegmentListsEqual(actualSegmentList, expectedSegmentList);

		// Export the segments as a binary file, too.
		File exportedFile = new File(outFilePath + ".exported");
		File exportedFileFolder = exportedFile.getParentFile();
		if (exportedFileFolder != null && !exportedFileFolder.isDirectory() && !exportedFileFolder.mkdirs()) {
			throw new IOException("Cannot create folder '" + exportedFileFolder + "'.");
		}
		try (OutputStream outputStream = new FileOutputStream(exportedFile)) {
			computerSystem.writeExecutableFile(actualSegmentList, SegmentList.NO_SEGMENT_INDEX, true, outputStream);
		}

		// Assert the binary files are equal.
		File exportedRefFile = new File(filePath + ".exported");
		assertFileEquals(exportedFile, exportedRefFile);
	}

	private static void assertSegmentListsEqual(SegmentList actual, SegmentList expected) {
		Assert.longEquals(actual.getCount(), expected.getCount());
		for (int i = 0; i < expected.getCount(); i++) {
			assertSegmentEquals(actual.getSegment(i), expected.getSegment(i));
		}
	}

	private static void assertSegmentEquals(Segment actual, Segment expected) {
		Assert.stringEquals(actual.title, expected.title);
		Assert.longEquals(actual.getHeader().getValue(), expected.getHeader().getValue());
		Assert.longEquals(actual.wBegin, expected.wBegin);
		Assert.longEquals(actual.wEnd, expected.wEnd);
		Assert.boolEquals(actual.bBinary, expected.bBinary);
		Assert.stringEquals(actual.labelPrefix, expected.labelPrefix);
		Assert.longEquals(actual.wSDXFixUpSize, expected.wSDXFixUpSize);
		Assert.longEquals(actual.bSDXBlockNumber, expected.bSDXBlockNumber);
		Assert.longEquals(actual.bSDXControlByte, expected.bSDXControlByte);
		Assert.stringEquals(actual.sdxSymbol, expected.sdxSymbol);
		Assert.stringEquals(actual.processorType.getKey(), expected.processorType.getKey());

		Assert.longEquals(actual.memoryBlock.getSize(), expected.memoryBlock.getSize());
		for (int i = 0; i < expected.memoryBlock.getSize(); i++) {
			Assert.longEquals(actual.memoryBlock.getDataAt(i), expected.memoryBlock.getDataAt(i));
			Assert.longEquals(actual.memoryBlock.getTypeAt(i).ordinal(), expected.memoryBlock.getTypeAt(i).ordinal());
		}

		Assert.longEquals(actual.comments.size(), expected.comments.size());
		for (int i = 0; i < expected.comments.size(); i++) {
			Assert.longEquals(actual.comments.get(i).getOffset(), expected.comments.get(i).getOffset());
			Assert.stringEquals(actual.comments.get(i).getText(), expected.comments.get(i).getText());
		}
	}

	static void assertFileEquals(File actual, File reference) throws IOException {
		byte[] actualBytes = Files.readAllBytes(actual.toPath());
		byte[] referenceBytes = Files.readAllBytes(reference.toPath());
		Assert.longEquals(actualBytes.length, referenceBytes.length);
		for (int i = 0; i < referenceBytes.length; i++) {
			Assert.longEquals(actualBytes[i] & 0xFF, referenceBytes[i] & 0xFF);
		}
	}
}
