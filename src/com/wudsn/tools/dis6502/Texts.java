/*
 * Copyright (C) 2025 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502;

import org.eclipse.osgi.util.NLS;

/**
 * UI text repository for strings this port introduces itself - dialog
 * titles, labels, body text - with no real Win32 {@code STRINGTABLE}/{@code
 * IDS_*} resource to mirror, unlike {@link Text}. Fields are named per
 * dialog ({@code <DialogName>_<Purpose>}, e.g. {@link #AboutDialog_Title}),
 * following the same convention {@code com.wudsn.tools.thecartstudio.Texts}
 * uses for its own dialog-local strings (its {@code AboutDialog_Content}/
 * {@code AboutDialog_URL} fields).
 * <p>
 * Populated the same reflective way as {@link Text} - see that class's own
 * javadoc - via {@code org.eclipse.osgi.util.NLS}, not {@code
 * com.wudsn.tools.base.repository.NLS} (what {@code
 * com.wudsn.tools.thecartstudio.Texts} itself actually extends): an
 * already-established, deliberate departure for this project, confirmed
 * when porting {@link Text} itself.
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
		NLS.initializeMessages(Texts.class.getName(), Texts.class);
	}
}
