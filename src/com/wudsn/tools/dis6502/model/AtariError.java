/*
 * Copyright (C) 2025 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

/**
 * Error codes returned by {@link AtariDOS}/{@link AtariDisk} operations.
 * <p>
 * Ported from AtariDOS.h / AtariDOS.cpp. {@code AtariDOS::GetErrorCode}
 * (which just returned the constant's name as a string) is superseded by
 * {@link #name()}. Fixed two typos in {@code GetErrorText}'s strings while
 * porting: {@code END_OF_FILE}'s text had a stray trailing {@code ")"},
 * and the default case read "Unkown error" - both fixed upstream too (see
 * those commits).
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

	/** Human-readable description of this error. */
	public String getErrorText() {
		switch (this) {
		case OK:
			return "OK";
		case NO_ENTRY_FOUND:
			return "No matching directory entry found";
		case END_OF_FILE:
			return "End of file reached";
		case DISK_NOT_FOUND:
			return "Disk not found";
		case DIRECTORY_NOT_FOUND:
			return "Directory not found";
		case DIRECTORY_READ:
			return "Cannot read directory";
		case DIRECTORY_WRITE:
			return "Cannot write directory";
		case INVALID_VTOC_ENTRY:
			return "Invalid VTOC entry";
		case SECTOR_NOT_FOUND:
			return "Sector not found";
		case FILE_READ:
			return "Cannot read file";
		case FILE_WRITE:
			return "Cannot write file";
		case FILE_CORRUPTED:
			return "File structure is corrupted";
		case FILE_ALREADY_EXISTS:
			return "File already exists";
		case NO_FREE_SECTOR:
			return "No free sector";
		case BITMAP_READ:
			return "Cannot read bitmap";
		case BITMAP_WRITE:
			return "Cannot write bitmap";
		case SECTOR_ALREADY_FREE:
			return "Sector is already free";
		case FILE_SEEK:
			return "Cannot seek file position";
		}
		return "Unknown error";
	}
}
