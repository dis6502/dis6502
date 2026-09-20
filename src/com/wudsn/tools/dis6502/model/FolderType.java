/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

/**
 * The kind of folder a default-folders setting is remembered for.
 * <p>
 * Ported from FolderType.h / FolderType.cpp. The C++ {@code
 * FolderTypeInfo}/{@code FolderTypeFactory} key/text lookup is folded into
 * {@link #getKey()}/{@link #getText()}/{@link #fromKey(String)}, the same
 * pattern used by {@link LabelAccess}/{@link Encoding}.
 *
 * @author Peter Dell
 */
public enum FolderType {
	UNKNOWN_FILES,
	RAW_FILES,
	EXECUTABLE_FILES,
	ROM_IMAGE_FILES,
	CASSETTE_IMAGE_FILES,
	DISK_IMAGE_FILES,
	WORKSPACE_FILES,
	EQUATES_FILES,
	PROFILE_FILES,
	DISASSEMBLY_FILES;

	public String getKey() {
		return this == UNKNOWN_FILES ? "UNKNOWN" : name();
	}

	public String getText() {
		switch (this) {
		case RAW_FILES:
			return "Raw Files";
		case EXECUTABLE_FILES:
			return "Executable Files";
		case ROM_IMAGE_FILES:
			return "ROM Image Files";
		case CASSETTE_IMAGE_FILES:
			return "Cassette Image Files";
		case DISK_IMAGE_FILES:
			return "Disk Image Files";
		case WORKSPACE_FILES:
			return "Workspace Files";
		case EQUATES_FILES:
			return "Equates Files";
		case PROFILE_FILES:
			return "Profile Files";
		case DISASSEMBLY_FILES:
			return "Disassembly Files";
		default:
			return "Unknown";
		}
	}

	public static FolderType fromKey(String key) {
		try {
			return valueOf(key);
		} catch (IllegalArgumentException e) {
			return UNKNOWN_FILES;
		}
	}
}
