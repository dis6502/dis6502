/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502;

import java.awt.Font;

import com.wudsn.tools.base.common.ApplicationSettingsSection;

/**
 * The persisted-preference section/key names and coded default values for
 * every option {@link com.wudsn.tools.dis6502.ui.OptionsDialog} manages -
 * the single source of truth both {@code Dis6502} (which reads/writes them
 * via {@link ApplicationSettingsSection}) and {@code OptionsDialog} (whose
 * controls' own initial/restored state must match the same defaults) refer
 * to, so the two can never drift apart, and so
 * {@link ApplicationSettingsSection#clear()} restoring {@link #SECTION} to
 * "nothing stored" is guaranteed to fall back to exactly these values.
 *
 * @author Peter Dell
 */
public final class Options {

	/** The {@link Application#getSettingsSection} name every key below lives under - app-wide, not per-workspace. */
	public static final String SECTION = "Display";

	/** An installed mono-spaced font family; {@link Font#MONOSPACED} is a logical family Java always provides. */
	public static final String TEXT_FONT_FAMILY_KEY = "TextFontFamily";
	public static final String TEXT_FONT_FAMILY_DEFAULT = Font.MONOSPACED;

	public static final String TEXT_FONT_SIZE_KEY = "TextFontSize";
	public static final int TEXT_FONT_SIZE_DEFAULT = 16;

	/** The memory inspector's native font height in pixels - see {@code ComputerFont.get} for why it is a multiple of 8. */
	public static final String NATIVE_FONT_SIZE_KEY = "NativeFontSize";
	public static final int NATIVE_FONT_SIZE_DEFAULT = 8;

	private Options() {
	}
}
