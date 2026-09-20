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
 * Ported from FileType.h. {@code FileTypeInfo}/{@code FileTypeFactory}'s
 * fuller UI-facing text/filter/folder-type lookup is not ported yet - only
 * {@link #getKey()}/{@link #fromKey(String)} (the key names, needed by
 * {@link MRUList}'s persistence) are, following the same pattern as {@link
 * LabelAccess}/{@link Encoding}/{@link FolderType}.
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
