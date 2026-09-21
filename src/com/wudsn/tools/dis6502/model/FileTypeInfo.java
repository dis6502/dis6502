/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.wudsn.tools.dis6502.Texts;

/**
 * What a file chooser needs to know about a {@link FileType}: its display
 * texts, the file extensions to filter for, the extension a file saved
 * without one gets, and which of the {@link DefaultFolders} its files are
 * looked for in.
 * <p>
 * Ported from FileType.h/FileType.cpp ({@code FileTypeInfo}/{@code
 * FileTypeFactory::GetInfo}). Deliberately kept apart from the {@link
 * FileType} enum itself: the texts are user-visible and come from the
 * localizable {@link Texts} repository, not from string literals in code.
 *
 * @author Peter Dell
 */
public final class FileTypeInfo {

	/** The "no sensible default extension" marker of {@link #defaultExtension}. */
	public static final String ANY_EXTENSION = ".*";

	public final FileType fileType;

	/** The singular display text, e.g. "Executable File". */
	public final String text;

	/** The plural text a file chooser's filter shows, e.g. "Executable Files". */
	public final String filterText;

	/** The extension (with leading dot) a file saved without one gets, or {@link #ANY_EXTENSION}. */
	public final String defaultExtension;

	public final FolderType folderType;

	/**
	 * The file extensions (lower case, with leading dot) a file chooser
	 * filters for. Empty if any file can be of this type - a raw file, an
	 * unknown one. Compared to the C++ version, executable files also cover
	 * {@code .prg} (C64) and {@code .tap} (Oric): its list was Atari-only.
	 */
	public final List<String> filterExtensions;

	private FileTypeInfo(FileType fileType, String text, String filterText, String defaultExtension, FolderType folderType,
			String... filterExtensions) {
		this.fileType = fileType;
		this.text = text;
		this.filterText = filterText;
		this.defaultExtension = defaultExtension;
		this.folderType = folderType;
		this.filterExtensions = Collections.unmodifiableList(Arrays.asList(filterExtensions));
	}

	public static FileTypeInfo get(FileType fileType) {
		switch (fileType) {
		case RAW_FILE:
			return new FileTypeInfo(fileType, Texts.FileType_RAW_FILE_Text, Texts.FileType_RAW_FILE_FilterText, ".bin", FolderType.RAW_FILES);
		case EXECUTABLE_FILE:
			return new FileTypeInfo(fileType, Texts.FileType_EXECUTABLE_FILE_Text, Texts.FileType_EXECUTABLE_FILE_FilterText, ".xex", FolderType.EXECUTABLE_FILES, ".bin", ".com", ".exe", ".prg", ".sys", ".tap", ".xex");
		case ROM_IMAGE_FILE:
			return new FileTypeInfo(fileType, Texts.FileType_ROM_IMAGE_FILE_Text, Texts.FileType_ROM_IMAGE_FILE_FilterText, ".rom", FolderType.ROM_IMAGE_FILES, ".bin", ".car", ".rom");
		case CASSETTE_IMAGE_FILE:
			return new FileTypeInfo(fileType, Texts.FileType_CASSETTE_IMAGE_FILE_Text, Texts.FileType_CASSETTE_IMAGE_FILE_FilterText, ".cas", FolderType.CASSETTE_IMAGE_FILES, ".cas");
		case DISK_IMAGE_EXECUTABLE_FILE:
			return new FileTypeInfo(fileType, Texts.FileType_DISK_IMAGE_EXECUTABLE_FILE_Text, Texts.FileType_DISK_IMAGE_EXECUTABLE_FILE_FilterText, ".atr", FolderType.DISK_IMAGE_FILES, ".atr", ".xfd");
		case DISK_IMAGE_BOOT_SECTORS:
			return new FileTypeInfo(fileType, Texts.FileType_DISK_IMAGE_BOOT_SECTORS_Text, Texts.FileType_DISK_IMAGE_BOOT_SECTORS_FilterText, ".atr", FolderType.DISK_IMAGE_FILES, ".atr", ".xfd");
		case DISK_IMAGE_SECTORS:
			return new FileTypeInfo(fileType, Texts.FileType_DISK_IMAGE_SECTORS_Text, Texts.FileType_DISK_IMAGE_SECTORS_FilterText, ".atr", FolderType.DISK_IMAGE_FILES, ".atr", ".xfd");
		case WORKSPACE_FILE:
			return new FileTypeInfo(fileType, Texts.FileType_WORKSPACE_FILE_Text, Texts.FileType_WORKSPACE_FILE_FilterText, ".wrk", FolderType.WORKSPACE_FILES, ".wrk");
		case EQUATES_FILE:
			return new FileTypeInfo(fileType, Texts.FileType_EQUATES_FILE_Text, Texts.FileType_EQUATES_FILE_FilterText, ".equ", FolderType.EQUATES_FILES, ".equ");
		case PROFILE_FILE:
			return new FileTypeInfo(fileType, Texts.FileType_PROFILE_FILE_Text, Texts.FileType_PROFILE_FILE_FilterText, ".prf", FolderType.PROFILE_FILES, ".prf");
		case DISASSEMBLY_FILE:
			return new FileTypeInfo(fileType, Texts.FileType_DISASSEMBLY_FILE_Text, Texts.FileType_DISASSEMBLY_FILE_FilterText, ".asm", FolderType.DISASSEMBLY_FILES, ".asm");
		default:
			return new FileTypeInfo(FileType.UNKNOWN_FILE, Texts.FileType_UNKNOWN_FILE_Text, Texts.FileType_UNKNOWN_FILE_FilterText,
					ANY_EXTENSION, FolderType.UNKNOWN_FILES);
		}
	}
}
