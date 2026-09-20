/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

import java.io.IOException;

/**
 * An Atari DOS 2.x disk image (.atr/.xfd), opened by path: each operation
 * re-opens and closes the underlying file (matching the C++ source, which
 * does the same).
 * <p>
 * Ported from AtariDOS.h / AtariDOS.cpp ({@code AtariDisk}). Methods that
 * are primarily for iterating the directory delegate directly to {@link
 * AtariDOS}'s package-private methods.
 *
 * @author Peter Dell
 */
public final class AtariDisk {

	private final String diskImageFilePath;

	public AtariDisk(String diskImageFilePath) {
		this.diskImageFilePath = diskImageFilePath;
	}

	public String getDiskImageFilePath() {
		return diskImageFilePath;
	}

	public AtariError findFirst(AtariFile info) throws IOException {
		return AtariDOS.findFirst(diskImageFilePath, info);
	}

	public AtariError findNext(AtariFile info) throws IOException {
		return AtariDOS.findNext(diskImageFilePath, info);
	}

	public AtariError getFileFromIndex(AtariFile info, int directoryIndex) throws IOException {
		return AtariDOS.getFileFromIndex(diskImageFilePath, info, directoryIndex);
	}

	public AtariError readFirstSector(AtariFile info) throws IOException {
		return AtariDOS.readFirstSector(diskImageFilePath, info);
	}

	public AtariError readNextSector(AtariFile info) throws IOException {
		return AtariDOS.readNextSector(diskImageFilePath, info);
	}

	public AtariError checkFile(AtariFile info, String fileName) throws IOException {
		return AtariDOS.checkFile(diskImageFilePath, info, fileName);
	}

	public AtariError createFile(AtariFile info, String fileName) throws IOException {
		return AtariDOS.createFile(diskImageFilePath, info, fileName);
	}

	public AtariError writeSector(AtariFile info) throws IOException {
		return AtariDOS.writeSector(diskImageFilePath, info);
	}

	/** @param fileSize single-element out parameter (index 0). */
	public AtariError getFileSize(AtariFile info, long[] fileSize) throws IOException {
		return AtariDOS.getFileSize(diskImageFilePath, info, fileSize);
	}

	/** @param diskIndex single-element in/out parameter (index 0): pass {-1} to start reading from the beginning of the file. */
	public AtariError readFile(AtariFile info, int[] diskIndex, byte[] buffer, int fileSize) throws IOException {
		return AtariDOS.readFile(diskImageFilePath, info, diskIndex, buffer, fileSize);
	}

	/** Reads the first, non-deleted file with the given file name. */
	public byte[] readFile(String fileName) throws IOException {
		AtariFile atariFile = new AtariFile();

		AtariError error = checkFile(atariFile, fileName);
		if (error != AtariError.OK) {
			throw new IOException(error.getErrorText());
		}
		long[] atariFileSize = { 0 };
		error = getFileSize(atariFile, atariFileSize);
		if (error != AtariError.OK) {
			throw new IOException(error.getErrorText());
		}

		byte[] result = new byte[(int) atariFileSize[0]];
		if (atariFileSize[0] > 0) {
			int[] diskIndex = { -1 };
			error = readFile(atariFile, diskIndex, result, result.length);
			if (error != AtariError.OK) {
				throw new IOException(error.getErrorText());
			}
		}
		return result;
	}
}
