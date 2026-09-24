/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * One named section of persistent application settings (key/value pairs).
 * <p>
 * Backed by {@code java.util.prefs.Preferences} - the JDK-built-in,
 * cross-platform mechanism for small named sections of persistent
 * key/value settings - with one {@code Preferences} node per section
 * name, created by {@link Application#getSettingsSection}.
 *
 * @author Peter Dell
 */
public final class ApplicationSettingsSection {

	private final Preferences preferences;

	ApplicationSettingsSection(Preferences preferences) {
		this.preferences = preferences;
	}

	public String getString(String keyName, String defaultValue) {
		return preferences.get(keyName, defaultValue);
	}

	public void writeString(String keyName, String value) {
		preferences.put(keyName, value);
	}

	public int getUnsignedInt(String keyName, int defaultValue) {
		return preferences.getInt(keyName, defaultValue);
	}

	public void writeUnsignedInt(String keyName, int value) {
		preferences.putInt(keyName, value);
	}

	/** Deletes every key in this section, so a later {@code getXxx} call falls back to its own coded default again. */
	public void clear() {
		try {
			preferences.clear();
		} catch (BackingStoreException ex) {
			throw new IllegalStateException(ex);
		}
	}
}
