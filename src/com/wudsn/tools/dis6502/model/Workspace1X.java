/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Reads the pre-3.0, pre-XML binary workspace format ({@code
 * DIS6502WRK14}), {@code Load14} only:
 * <ul>
 * <li>{@code Load10} ({@code DIS6502WRK10}, an even older format) is not
 * implemented - no real {@code WRK10} fixture file could be found to
 * validate a load against, and there is reason to doubt the format was
 * ever fully correct to begin with, so there is no well-defined "correct"
 * behavior to implement.</li>
 * <li>{@code Save14} (writing this legacy format) is not implemented -
 * nothing needs to write it going forward; {@link WorkspaceLogic#save}
 * always saves the modern XML format.</li>
 * <li>The legacy {@code binPath}/{@code diskPath} fields (a pair of file
 * paths not otherwise tied to a workspace) are read past but not stored
 * anywhere - nothing in {@link Workspace} has a place for them, and
 * nothing else needs them.</li>
 * </ul>
 * The exact byte layout below was verified against a real {@code
 * DIS6502WRK14} fixture file byte-for-byte, including a full walk of its
 * segment header, equate, and segment data sections landing exactly on
 * the file's end.
 *
 * @author Peter Dell
 */
public final class Workspace1X {

	public static final int MAGIC_SIZE = 12;
	public static final String MAGIC10 = "DIS6502WRK10";
	public static final String MAGIC14 = "DIS6502WRK14";

	private static final int MAX_SEGMENTS14 = 256;
	private static final int FILE_PATH1X_SIZE = 260;

	// SEGMENT14: ADDRESS_1X beginAddress; ADDRESS_1X endAddress; POINTER_32BIT data;
	// POINTER_32BIT type; LINE_NUMBER_1X firstLineNumber; BOOL_1X binary; CHAR_1X title[22];
	// - 42 bytes of fields, padded to 44 by MSVC's default struct alignment (the struct's
	// largest member is 4-byte aligned).
	private static final int SEGMENT14_SIZE = 44;

	private Workspace1X() {
	}

	/** Returns {@code true} for an all-zero 32 bit "pointer" field, used only as a null indicator. */
	static boolean isNull(long pointer32Bit) {
		return pointer32Bit == 0L;
	}

	/** Reads a workspace from {@code inputStream}, positioned right after the 12 byte magic. */
	public static void load14(Workspace workspace, InputStream inputStream) throws IOException {
		skipFully(inputStream, FILE_PATH1X_SIZE); // binPath: not tracked by the modern Workspace model.
		skipFully(inputStream, FILE_PATH1X_SIZE); // diskPath: likewise.

		SegmentList segmentList = workspace.getSegmentList();

		byte[] entry = new byte[SEGMENT14_SIZE];
		SegmentListInserter segmentListInserter = segmentList.createInserter();
		for (int segmentIndex = 0; segmentIndex < MAX_SEGMENTS14; segmentIndex++) {
			readFully(inputStream, entry);

			long data = readUnsignedIntLE(entry, 4);
			if (!isNull(data)) {
				int beginAddress = readUnsignedShortLE(entry, 0);
				int endAddress = readUnsignedShortLE(entry, 2);
				int firstLineNumber = (int) readUnsignedIntLE(entry, 12);
				boolean binary = readIntLE(entry, 16) != 0;
				String title = ansiStringAt(entry, 20, 22);

				Segment segment = segmentListInserter.insertSegment();
				segment.setHeader(FileHeader.ATARI_BINARY);
				segment.wBegin = beginAddress;
				segment.wEnd = endAddress;
				segment.setFirstLineNumber(firstLineNumber);
				segment.bBinary = binary;
				segment.title = title;
			}
		}
		segmentListInserter.apply();

		workspace.getUserEquateList().load1X(inputStream);

		for (int segmentIndex = 0; segmentIndex < segmentList.getCount(); segmentIndex++) {
			segmentList.getSegment(segmentIndex).load14(inputStream);
		}
	}

	private static int readUnsignedShortLE(byte[] buffer, int offset) {
		return (buffer[offset] & 0xFF) | ((buffer[offset + 1] & 0xFF) << 8);
	}

	private static long readUnsignedIntLE(byte[] buffer, int offset) {
		return (buffer[offset] & 0xFFL) | ((buffer[offset + 1] & 0xFFL) << 8) | ((buffer[offset + 2] & 0xFFL) << 16)
				| ((buffer[offset + 3] & 0xFFL) << 24);
	}

	private static int readIntLE(byte[] buffer, int offset) {
		return (int) readUnsignedIntLE(buffer, offset);
	}

	/** Decodes a fixed-size, NUL-terminated (or fully occupied) ANSI byte field as a string. */
	private static String ansiStringAt(byte[] buffer, int offset, int length) {
		int end = offset;
		while (end < offset + length && buffer[end] != 0) {
			end++;
		}
		return new String(buffer, offset, end - offset, StandardCharsets.ISO_8859_1);
	}

	private static void readFully(InputStream inputStream, byte[] buffer) throws IOException {
		int totalRead = 0;
		while (totalRead < buffer.length) {
			int read = inputStream.read(buffer, totalRead, buffer.length - totalRead);
			if (read < 0) {
				throw new EOFException("Unexpected end of file.");
			}
			totalRead += read;
		}
	}

	/** Skips exactly {@code count} bytes, falling back to reading and discarding since {@link InputStream#skip} may return less than requested. */
	static void skipFully(InputStream inputStream, long count) throws IOException {
		long remaining = count;
		while (remaining > 0) {
			long skipped = inputStream.skip(remaining);
			if (skipped > 0) {
				remaining -= skipped;
			} else if (inputStream.read() < 0) {
				throw new EOFException("Unexpected end of file.");
			} else {
				remaining--;
			}
		}
	}
}
