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
 * Text encoding used for the disassembly output.
 * <p>
 * Ported from Encoding.h/Encoding.cpp as a WUDSN Base {@link ValueSet}
 * rather than a Java {@code enum}: that is the C++ version's {@code
 * Encoding} and {@code EncodingInfo}/{@code EncodingFactory} in one - {@link
 * #getId()} is the key {@link Profile}'s serialization uses, {@link
 * #getText()} (also {@link #toString()}, so a combo box shows it) the
 * localizable display text from {@code ValueSets.properties}. The newline
 * per encoding stays {@link DisassemblyResultWriter}'s own business.
 *
 * @author Peter Dell
 */
public final class Encoding extends ValueSet {

	public static final Encoding UNKNOWN;
	public static final Encoding ASCII;
	public static final Encoding ATASCII;
	public static final Encoding BINARY;
	public static final Encoding UTF8;

	private static final Map<String, Encoding> values;

	static {
		values = new LinkedHashMap<String, Encoding>();

		UNKNOWN = add("UNKNOWN");
		ASCII = add("ASCII");
		ATASCII = add("ATASCII");
		BINARY = add("BINARY");
		UTF8 = add("UTF8");

		initializeClass(Encoding.class, ValueSets.class);
	}

	private Encoding(String id, int sortKey) {
		super(id, id, sortKey);
	}

	private static Encoding add(String id) {
		// The sort key keeps the declaration order - ValueSet would sort by text otherwise.
		Encoding result = new Encoding(id, values.size());
		values.put(id, result);
		return result;
	}

	/** Gets the unmodifiable list of all values. */
	public static List<Encoding> getValues() {
		return Collections.unmodifiableList(new ArrayList<Encoding>(values.values()));
	}

	/**
	 * The encodings a disassembly listing can be written in - what a user
	 * can choose from, and what {@link Profile} accepts from a file: not
	 * {@link #UNKNOWN}, and not {@link #BINARY}, which is no text encoding.
	 */
	public static List<Encoding> getOutputValues() {
		List<Encoding> result = new ArrayList<Encoding>();
		result.add(ASCII);
		result.add(ATASCII);
		result.add(UTF8);
		return Collections.unmodifiableList(result);
	}

	/** The XML attribute key for an encoding value, used by {@link Profile}'s serialization. */
	public String getKey() {
		return getId();
	}

	/** The inverse of {@link #getKey()}; {@link #UNKNOWN} for a key that is none. */
	public static Encoding fromKey(String key) {
		Encoding result = values.get(key);
		return result == null ? UNKNOWN : result;
	}
}
