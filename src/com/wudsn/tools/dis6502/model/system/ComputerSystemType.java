/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model.system;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.wudsn.tools.base.repository.ValueSet;
import com.wudsn.tools.dis6502.ValueSets;
import com.wudsn.tools.dis6502.model.FileType;

/**
 * The computer systems a workspace can be for.
 * <p>
 * A WUDSN Base {@link ValueSet} rather than a Java {@code enum} - like
 * {@link FileType}. {@link #getId()} is the technical name: what a
 * workspace file contains, what the settings of a system are stored under,
 * and what the command line accepts ({@code /C64}). {@link #getText()} is
 * the localizable display text from {@code ValueSets.properties}; {@link
 * #getFileName()} the base name of the system's resource files ({@code
 * Atari800.equ}).
 *
 * @author Peter Dell
 */
public final class ComputerSystemType extends ValueSet {

	public static final ComputerSystemType ATARI5200;
	public static final ComputerSystemType ATARI800;
	public static final ComputerSystemType C64;
	public static final ComputerSystemType ORIC;
	public static final ComputerSystemType UNKNOWN;

	private static final Map<String, ComputerSystemType> values;

	private final String fileName;

	static {
		values = new LinkedHashMap<String, ComputerSystemType>();

		ATARI5200 = add("ATARI5200", "Atari5200");
		ATARI800 = add("ATARI800", "Atari800");
		C64 = add("C64", "C64");
		ORIC = add("ORIC", "Oric");
		UNKNOWN = add("UNKNOWN", "Unknown");

		initializeClass(ComputerSystemType.class, ValueSets.class);
	}

	private ComputerSystemType(String id, int sortKey, String fileName) {
		super(id, id, sortKey);
		this.fileName = fileName;
	}

	private static ComputerSystemType add(String id, String fileName) {
		// The sort key keeps the declaration order - ValueSet would sort by text otherwise.
		ComputerSystemType result = new ComputerSystemType(id, values.size(), fileName);
		values.put(id, result);
		return result;
	}

	/** Gets the unmodifiable list of all values. */
	public static List<ComputerSystemType> getValues() {
		return Collections.unmodifiableList(new ArrayList<ComputerSystemType>(values.values()));
	}

	/** The systems a user can pick for a workspace: all but {@link #UNKNOWN}. */
	public static List<ComputerSystemType> getSelectableValues() {
		List<ComputerSystemType> result = new ArrayList<ComputerSystemType>(values.values());
		result.remove(UNKNOWN);
		return Collections.unmodifiableList(result);
	}

	/** The inverse of {@link #getId()}; {@link #UNKNOWN} for an id that is none. */
	public static ComputerSystemType fromId(String id) {
		ComputerSystemType result = values.get(id);
		return result == null ? UNKNOWN : result;
	}

	/** The base name of this system's resource files, e.g. {@code "Atari800"} for {@code Atari800.equ}. */
	public String getFileName() {
		return fileName;
	}
}
