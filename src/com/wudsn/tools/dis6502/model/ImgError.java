/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

/**
 * Status codes for {@link DiskImage} operations - the first two identify a
 * successfully recognized disk image type, the rest are error conditions
 * (see {@link DiskImage#isError}).
 * <p>
 * Ported from DiskImage.h ({@code ImgError}).
 *
 * @author Peter Dell
 */
public enum ImgError {
	/** XFD image detected. */
	XFD,
	/** ATR image detected. */
	ATR,
	/** File does not have a NICKATARI (ATR) signature and does not match a known XFD size. */
	BAD_MAGIC,
	/** File cannot be opened. */
	FILE_NOT_FOUND,
	/** Sector number out of range. */
	OUT_OF_RANGE,
	/** Disk read/write error occurred. */
	DISK_ERROR,
	/** Write denied: image is write-protected. */
	WRITE_PROTECT;

	/**
	 * Human-readable description, matching {@code DiskImage::DisplayError}'s
	 * {@code IDS_ERR_IMG_*} message texts (without their "Error: " prefix).
	 * Never actually shown for {@link #XFD}/{@link #ATR}, since those are
	 * not errors - see {@link DiskImage#isError}.
	 */
	public String getErrorText() {
		switch (this) {
		case XFD:
			return "XFD image detected";
		case ATR:
			return "ATR image detected";
		case BAD_MAGIC:
			return "ATR file does not have a \"NICKATARI\" signature";
		case FILE_NOT_FOUND:
			return "File does not exist";
		case OUT_OF_RANGE:
			return "Sector out of range";
		case DISK_ERROR:
			return "Disk error occurred";
		case WRITE_PROTECT:
			return "Disk image is write protected";
		default:
			return "Unknown error";
		}
	}
}
