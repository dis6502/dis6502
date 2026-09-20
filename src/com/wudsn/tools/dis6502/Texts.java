/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502;

import com.wudsn.tools.base.repository.NLS;

/**
 * UI text repository for strings this port introduces itself - dialog
 * titles, labels, body text - with no real Win32 {@code STRINGTABLE}/{@code
 * IDS_*} resource to mirror, unlike {@link Text}. Fields are named per
 * dialog ({@code <DialogName>_<Purpose>}, e.g. {@link #AboutDialog_Title}),
 * following the same convention {@code com.wudsn.tools.thecartstudio.Texts}
 * uses for its own dialog-local strings (its {@code AboutDialog_Content}/
 * {@code AboutDialog_URL} fields) - including that class's own {@code
 * com.wudsn.tools.base.repository.NLS} base, the same one {@link Text}/
 * {@link Actions} use.
 *
 * @author Peter Dell
 */
public final class Texts extends NLS {

	/** {@link com.wudsn.tools.dis6502.ui.AboutDialog}'s window title. */
	public static String AboutDialog_WindowTitle;
	/** {@link com.wudsn.tools.dis6502.ui.AboutDialog}'s bold product-name label. */
	public static String AboutDialog_Title;
	/** {@link com.wudsn.tools.dis6502.ui.AboutDialog}'s subtitle label. */
	public static String AboutDialog_Subtitle;
	/** {@link com.wudsn.tools.dis6502.ui.AboutDialog}'s body text, one line per {@code \n} - split back into individual lines there. */
	public static String AboutDialog_Text;

	static {
		initializeClass(Texts.class, null);
	}
}
