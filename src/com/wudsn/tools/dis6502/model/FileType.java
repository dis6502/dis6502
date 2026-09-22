/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.wudsn.tools.base.repository.ValueSet;
import com.wudsn.tools.dis6502.ValueSets;
import com.wudsn.tools.dis6502.model.system.ComputerSystem;

/**
 * The kind of file a {@link ComputerSystem} can read, write, or guess from
 * its content - and what a file chooser needs to know about it.
 * <p>
 * A WUDSN Base {@link ValueSet} rather than a Java {@code enum} - like
 * {@link Encoding}/{@link FolderType}: {@link #getId()} is the key {@link
 * MRUList}'s persistence uses; {@link #getText()} is the localizable
 * display text from {@code ValueSets.properties}; the default extension,
 * filter extensions and folder type are additional attributes of each
 * value, the way a value set is meant to carry them. A separate filter
 * text ("Disk Image Files") needs no entry of its own: for every type that
 * has a filter, it is the text of its {@link FolderType} - see
 * {@link #getFilterText()}.
 *
 * @author Peter Dell
 */
public final class FileType extends ValueSet {

	// Private: the WUDSN Base repository loader takes every public static final field of a value set for a value.
	private static final String ANY_EXTENSION = ".*";

	public static final FileType ANY_FILE;
	public static final FileType RAW_FILE;
	public static final FileType EXECUTABLE_FILE;
	public static final FileType ROM_IMAGE_FILE;
	public static final FileType CASSETTE_IMAGE_FILE;
	public static final FileType DISK_IMAGE_EXECUTABLE_FILE;
	public static final FileType DISK_IMAGE_BOOT_SECTORS;
	public static final FileType DISK_IMAGE_SECTORS;
	public static final FileType WORKSPACE_FILE;
	public static final FileType EQUATES_FILE;
	public static final FileType PROFILE_FILE;
	public static final FileType DISASSEMBLY_FILE;

	private static final Map<String, FileType> values;

	private final String defaultExtension;
	private final FolderType folderType;
	private final List<String> filterExtensions;

	static {
		values = new LinkedHashMap<String, FileType>();

		ANY_FILE = add("ANY_FILE", ANY_EXTENSION, FolderType.UNKNOWN);
		RAW_FILE = add("RAW_FILE", ".bin", FolderType.RAW_FILES);
		EXECUTABLE_FILE = add("EXECUTABLE_FILE", ".xex", FolderType.EXECUTABLE_FILES, ".bin", ".com", ".exe", ".prg", ".sys", ".tap", ".xex");
		ROM_IMAGE_FILE = add("ROM_IMAGE_FILE", ".rom", FolderType.ROM_IMAGE_FILES, ".bin", ".car", ".rom");
		CASSETTE_IMAGE_FILE = add("CASSETTE_IMAGE_FILE", ".cas", FolderType.CASSETTE_IMAGE_FILES, ".cas");
		DISK_IMAGE_EXECUTABLE_FILE = add("DISK_IMAGE_EXECUTABLE_FILE", ".atr", FolderType.DISK_IMAGE_FILES, ".atr", ".xfd");
		DISK_IMAGE_BOOT_SECTORS = add("DISK_IMAGE_BOOT_SECTORS", ".atr", FolderType.DISK_IMAGE_FILES, ".atr", ".xfd");
		DISK_IMAGE_SECTORS = add("DISK_IMAGE_SECTORS", ".atr", FolderType.DISK_IMAGE_FILES, ".atr", ".xfd");
		WORKSPACE_FILE = add("WORKSPACE_FILE", ".wrk", FolderType.WORKSPACE_FILES, ".wrk");
		EQUATES_FILE = add("EQUATES_FILE", ".equ", FolderType.EQUATES_FILES, ".equ");
		PROFILE_FILE = add("PROFILE_FILE", ".prf", FolderType.PROFILE_FILES, ".prf");
		DISASSEMBLY_FILE = add("DISASSEMBLY_FILE", ".asm", FolderType.DISASSEMBLY_FILES, ".asm");

		initializeClass(FileType.class, ValueSets.class);
	}

	private FileType(String id, int sortKey, String defaultExtension, FolderType folderType, String[] filterExtensions) {
		super(id, id, sortKey);
		this.defaultExtension = defaultExtension;
		this.folderType = folderType;
		this.filterExtensions = Collections.unmodifiableList(Arrays.asList(filterExtensions));
	}

	private static FileType add(String id, String defaultExtension, FolderType folderType, String... filterExtensions) {
		// The sort key keeps the declaration order - ValueSet would sort by text otherwise.
		FileType result = new FileType(id, values.size(), defaultExtension, folderType, filterExtensions);
		values.put(id, result);
		return result;
	}

	/** Gets the unmodifiable list of all values. */
	public static List<FileType> getValues() {
		return Collections.unmodifiableList(new ArrayList<FileType>(values.values()));
	}

	/** The key a file of this type is stored under in the most-recently-used lists. */
	public String getKey() {
		return getId();
	}

	/** The inverse of {@link #getKey()}; {@link #ANY_FILE} for a key that is none. */
	public static FileType fromKey(String key) {
		FileType result = values.get(key);
		return result == null ? ANY_FILE : result;
	}

	/** Whether a file of this type saved without an extension should get one - not if any file can be of this type. */
	public boolean hasDefaultExtension() {
		return !ANY_EXTENSION.equals(defaultExtension);
	}

	/** The extension (with leading dot) a file saved without one gets - see {@link #hasDefaultExtension()}. */
	public String getDefaultExtension() {
		return defaultExtension;
	}

	/** Which of the {@link DefaultFolders} files of this type are looked for in. */
	public FolderType getFolderType() {
		return folderType;
	}

	/**
	 * The file extensions (lower case, with leading dot) a file chooser
	 * filters for. Empty if any file can be of this type - a raw file, or
	 * {@link #ANY_FILE}. Executable files cover {@code .prg} (C64) and
	 * {@code .tap} (Oric) in addition to the Atari extensions.
	 */
	public List<String> getFilterExtensions() {
		return filterExtensions;
	}

	/**
	 * The plural text a file chooser's filter shows, e.g. "Disk Image Files"
	 * for all three disk image file types: the text of the {@link
	 * #getFolderType() folder type}.
	 */
	public String getFilterText() {
		return folderType.getText();
	}
}
