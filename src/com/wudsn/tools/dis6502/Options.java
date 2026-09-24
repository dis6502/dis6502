/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502;

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

	/** Empty means "use the computer's native font" - see {@code Dis6502.getTextFont}. */
	public static final String TEXT_FONT_FAMILY_KEY = "TextFontFamily";
	public static final String TEXT_FONT_FAMILY_DEFAULT = "";

	/** An ordinary readable size - unrelated to {@code ComputerFont}'s tiny native-pixel-height derivation. */
	public static final String TEXT_FONT_SIZE_KEY = "TextFontSize";
	public static final int TEXT_FONT_SIZE_DEFAULT = 16;

	/**
	 * How many times {@code ComputerFont}'s authentic 8-pixel-tall glyph cell
	 * is scaled up for on-screen legibility - a separate preference from
	 * {@link #TEXT_FONT_SIZE_KEY}, which only ever applies to a chosen plain
	 * font, since the memory inspector's grid always uses the native font
	 * regardless of {@link #TEXT_FONT_FAMILY_KEY}.
	 */
	public static final String NATIVE_FONT_ZOOM_KEY = "NativeFontZoom";
	public static final int NATIVE_FONT_ZOOM_DEFAULT = 1;

	private Options() {
	}
}
