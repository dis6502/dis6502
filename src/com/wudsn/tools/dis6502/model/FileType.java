/*
 * Copyright (C) 2025 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

/**
 * The kind of file a {@link ComputerSystem} can read, write, or guess from
 * its content.
 * <p>
 * Ported from FileType.h. {@code FileTypeInfo}/{@code FileTypeFactory} (the
 * UI-facing key/text/filter lookup) is not ported yet.
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
	DISASSEMBLY_FILE
}
