/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model.system.atari800;

import java.util.Arrays;

import com.wudsn.tools.dis6502.model.FileAttribute;
import com.wudsn.tools.dis6502.model.SegmentList;
import com.wudsn.tools.dis6502.model.SegmentListInserter;

/**
 * A DOS 2.x directory entry, plus the transient sector-chain-walking state
 * used while reading/writing its data (see {@link AtariDOS}).
 * <p>
 * Its fields are package-private, reachable only from within {@link
 * AtariDOS} - the same pattern as {@link SegmentList}/{@link
 * SegmentListInserter}.
 *
 * @author Peter Dell
 */
public final class AtariFile {

	static final int NO_DIRECTORY_INDEX = -1;

	int directoryIndex;
	int fileAttribute;
	int sectorCount;
	int startSectorNumber;
	final StringBuilder fileName = new StringBuilder();

	// Current directory sector data.
	final byte[] sector = new byte[128];
	// Number of bytes used in the sector buffer.
	int sectorSize;
	// Next sector where the file continues, or 0 if this is the last sector.
	int nextSectorNumber;

	public AtariFile() {
		clear();
	}

	public void clear() {
		directoryIndex = NO_DIRECTORY_INDEX;
		fileAttribute = 0;
		sectorCount = 0;
		startSectorNumber = 0;
		fileName.setLength(0);

		Arrays.fill(sector, (byte) 0);
		sectorSize = 0;
		nextSectorNumber = 0;
	}

	public int getDirectoryIndex() {
		return directoryIndex;
	}

	// File attributes.

	public boolean isOpenForOutput() {
		return isAttributeSet(FileAttribute.OPEN_FOR_OUTPUT);
	}

	public boolean isUnknown() {
		return isAttributeSet(FileAttribute.UNKNOWN);
	}

	public boolean isLocked() {
		return isAttributeSet(FileAttribute.LOCKED);
	}

	public boolean isInUse() {
		return isAttributeSet(FileAttribute.IN_USE);
	}

	public boolean isDeleted() {
		return isAttributeSet(FileAttribute.DELETED);
	}

	private boolean isAttributeSet(int fileAttributeBit) {
		return (fileAttribute & fileAttributeBit) == fileAttributeBit;
	}

	public int getSectorCount() {
		return sectorCount;
	}

	public int getStartSectorNumber() {
		return startSectorNumber;
	}

	/** Formatted, e.g. "FILENAME.EXT". */
	public String getFileName() {
		return fileName.toString();
	}

	/** Unformatted 8.3 characters, e.g. "FILENAME.EXT" padded with spaces. */
	public String getFileName83() {
		return AtariDOS.getFileName83(fileName.toString());
	}

	/** Formatted as "* FILENAME.EXT 123". */
	public String getDirectoryText() {
		return (isLocked() ? "*" : " ") + " " + getFileName83() + " " + String.format("%03d", getSectorCount());
	}
}
