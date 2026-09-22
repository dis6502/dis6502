/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model.system.atari800;

/**
 * Status codes for {@link DiskImage} operations - the first two identify a
 * successfully recognized disk image type, the rest are error conditions
 * (see {@link DiskImage#isError}).
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
}
