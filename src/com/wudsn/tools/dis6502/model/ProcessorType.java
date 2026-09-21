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
 * The processor a {@link Segment}'s code is for.
 * <p>
 * Ported from ProcessorType.h as a WUDSN Base {@link ValueSet} rather than
 * a Java {@code enum} - like {@link FileType}, this is the C++ version's
 * {@code ProcessorType} and {@code ProcessorTypeInfo}/{@code
 * ProcessorTypeFactory} in one: {@link #getId()} is the key a workspace
 * file contains, {@link #getText()} the localizable display text ("MOS
 * 6502") from {@code ValueSets.properties}.
 *
 * @author Peter Dell
 */
public final class ProcessorType extends ValueSet {

	public static final ProcessorType UNKNOWN;
	public static final ProcessorType MOS6502;
	public static final ProcessorType MOS65C02;

	private static final Map<String, ProcessorType> values;

	static {
		values = new LinkedHashMap<String, ProcessorType>();

		UNKNOWN = add("UNKNOWN");
		MOS6502 = add("MOS6502");
		MOS65C02 = add("MOS65C02");

		initializeClass(ProcessorType.class, ValueSets.class);
	}

	private ProcessorType(String id, int sortKey) {
		super(id, id, sortKey);
	}

	private static ProcessorType add(String id) {
		// The sort key keeps the declaration order - ValueSet would sort by text otherwise.
		ProcessorType result = new ProcessorType(id, values.size());
		values.put(id, result);
		return result;
	}

	/** Gets the unmodifiable list of all values. */
	public static List<ProcessorType> getValues() {
		return Collections.unmodifiableList(new ArrayList<ProcessorType>(values.values()));
	}

	/** The processors a user can pick for a segment: all but {@link #UNKNOWN}. */
	public static List<ProcessorType> getSelectableValues() {
		List<ProcessorType> result = new ArrayList<ProcessorType>(values.values());
		result.remove(UNKNOWN);
		return Collections.unmodifiableList(result);
	}

	/** The key a workspace file contains for this processor. */
	public String getKey() {
		return getId();
	}

	/** The inverse of {@link #getKey()}; {@link #UNKNOWN} for a key that is none. */
	public static ProcessorType fromKey(String key) {
		ProcessorType result = values.get(key);
		return result == null ? UNKNOWN : result;
	}
}
