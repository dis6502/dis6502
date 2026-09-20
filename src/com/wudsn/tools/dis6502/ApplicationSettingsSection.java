/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502;

import java.util.prefs.Preferences;

/**
 * One named section of persistent application settings (key/value pairs).
 * <p>
 * Ported from ApplicationSettingsSection.h / ApplicationSettingsSection.cpp.
 * The C++ version reads/writes a named section of a Windows INI-style file
 * via {@code GetPrivateProfileString}/{@code WritePrivateProfileString}
 * (a no-op reading the default on non-Windows platforms). This uses {@code
 * java.util.prefs.Preferences} instead - the JDK-built-in, cross-platform
 * equivalent of "small named sections of persistent key/value settings" -
 * with one {@code Preferences} node per section name, created by {@link
 * Application#getSettingsSection}. Unlike the C++ version's out-parameter
 * {@code GetString}/{@code GetUnsignedInt}, the getters here return the
 * value directly.
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
}
