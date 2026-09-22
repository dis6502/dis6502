/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.wudsn.tools.base.repository.ValueSet;
import com.wudsn.tools.dis6502.ValueSets;

/**
 * The kind of folder a default-folders setting is remembered for.
 * <p>
 * A WUDSN Base {@link ValueSet} rather than a Java {@code enum} - like
 * {@link Encoding}: {@link #getId()} is the key the settings are stored
 * under, and each constant is named like its key; {@link #getText()} is the
 * localizable display text from {@code ValueSets.properties}.
 *
 * @author Peter Dell
 */
public final class FolderType extends ValueSet {

	public static final FolderType UNKNOWN;
	public static final FolderType RAW_FILES;
	public static final FolderType EXECUTABLE_FILES;
	public static final FolderType ROM_IMAGE_FILES;
	public static final FolderType CASSETTE_IMAGE_FILES;
	public static final FolderType DISK_IMAGE_FILES;
	public static final FolderType WORKSPACE_FILES;
	public static final FolderType EQUATES_FILES;
	public static final FolderType PROFILE_FILES;
	public static final FolderType DISASSEMBLY_FILES;

	private static final Map<String, FolderType> values;

	static {
		values = new LinkedHashMap<String, FolderType>();

		UNKNOWN = add("UNKNOWN");
		RAW_FILES = add("RAW_FILES");
		EXECUTABLE_FILES = add("EXECUTABLE_FILES");
		ROM_IMAGE_FILES = add("ROM_IMAGE_FILES");
		CASSETTE_IMAGE_FILES = add("CASSETTE_IMAGE_FILES");
		DISK_IMAGE_FILES = add("DISK_IMAGE_FILES");
		WORKSPACE_FILES = add("WORKSPACE_FILES");
		EQUATES_FILES = add("EQUATES_FILES");
		PROFILE_FILES = add("PROFILE_FILES");
		DISASSEMBLY_FILES = add("DISASSEMBLY_FILES");

		initializeClass(FolderType.class, ValueSets.class);
	}

	private FolderType(String id, int sortKey) {
		super(id, id, sortKey);
	}

	private static FolderType add(String id) {
		// The sort key keeps the declaration order - ValueSet would sort by text otherwise.
		FolderType result = new FolderType(id, values.size());
		values.put(id, result);
		return result;
	}

	/** Gets the unmodifiable list of all values. */
	public static List<FolderType> getValues() {
		return Collections.unmodifiableList(new ArrayList<FolderType>(values.values()));
	}

	/** The key a folder of this type is stored under in the settings. */
	public String getKey() {
		return getId();
	}

	/** The inverse of {@link #getKey()}; {@link #UNKNOWN} for a key that is none. */
	public static FolderType fromKey(String key) {
		FolderType result = values.get(key);
		return result == null ? UNKNOWN : result;
	}
}
