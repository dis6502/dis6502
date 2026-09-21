/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

/**
 * The kind of file a {@link ComputerSystem} can read, write, or guess from
 * its content.
 * <p>
 * Ported from FileType.h. {@link #getKey()}/{@link #fromKey(String)} are
 * the key names {@link MRUList}'s persistence uses, following the same
 * pattern as {@link LabelAccess}/{@link Encoding}/{@link FolderType}. The
 * UI-facing texts, filter extensions and folder type live in {@link
 * FileTypeInfo}, so that the texts stay localizable.
 *
 * @author Peter Dell
 */
public enum FileType {
	UNKNOWN_FILE,
	RAW_FILE,
	EXECUTABLE_FILE,
	ROM_IMAGE_FILE,
	CASSETTE_IMAGE_FILE,
	DISK_IMAGE_EXECUTABLE_FILE,
	DISK_IMAGE_BOOT_SECTORS,
	DISK_IMAGE_SECTORS,
	WORKSPACE_FILE,
	EQUATES_FILE,
	PROFILE_FILE,
	DISASSEMBLY_FILE;

	public String getKey() {
		return name();
	}

	public static FileType fromKey(String key) {
		try {
			return valueOf(key);
		} catch (IllegalArgumentException e) {
			return UNKNOWN_FILE;
		}
	}
}
