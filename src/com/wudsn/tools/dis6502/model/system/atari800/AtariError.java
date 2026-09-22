/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model.system.atari800;

import com.wudsn.tools.dis6502.Messages;

/**
 * Error codes returned by {@link AtariDOS}/{@link AtariDisk} operations.
 * <p>
 * The texts are not string literals here but {@link Messages} entries,
 * like {@link ImgError}'s (see {@code DiskImage.displayError}), so that
 * they can be translated.
 *
 * @author Peter Dell
 */
public enum AtariError {
	OK,
	NO_ENTRY_FOUND,
	END_OF_FILE,
	DISK_NOT_FOUND,
	DIRECTORY_NOT_FOUND,
	DIRECTORY_READ,
	DIRECTORY_WRITE,
	INVALID_VTOC_ENTRY,
	SECTOR_NOT_FOUND,
	FILE_READ,
	FILE_WRITE,
	FILE_CORRUPTED,
	FILE_ALREADY_EXISTS,
	NO_FREE_SECTOR,
	BITMAP_READ,
	BITMAP_WRITE,
	SECTOR_ALREADY_FREE,
	FILE_SEEK;

	/** Human-readable, localizable description of this error - from {@code Messages.properties}. */
	public String getErrorText() {
		switch (this) {
		case OK:
			return Messages.I074.format();
		case NO_ENTRY_FOUND:
			return Messages.E075.format();
		case END_OF_FILE:
			return Messages.E076.format();
		case DISK_NOT_FOUND:
			return Messages.E077.format();
		case DIRECTORY_NOT_FOUND:
			return Messages.E078.format();
		case DIRECTORY_READ:
			return Messages.E079.format();
		case DIRECTORY_WRITE:
			return Messages.E080.format();
		case INVALID_VTOC_ENTRY:
			return Messages.E081.format();
		case SECTOR_NOT_FOUND:
			return Messages.E082.format();
		case FILE_READ:
			return Messages.E083.format();
		case FILE_WRITE:
			return Messages.E084.format();
		case FILE_CORRUPTED:
			return Messages.E085.format();
		case FILE_ALREADY_EXISTS:
			return Messages.E086.format();
		case NO_FREE_SECTOR:
			return Messages.E087.format();
		case BITMAP_READ:
			return Messages.E088.format();
		case BITMAP_WRITE:
			return Messages.E089.format();
		case SECTOR_ALREADY_FREE:
			return Messages.E090.format();
		case FILE_SEEK:
			return Messages.E091.format();
		}
		throw new IllegalStateException("No error text for " + name() + ".");
	}
}
