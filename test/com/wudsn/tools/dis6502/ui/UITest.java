/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.ui;

import java.awt.GraphicsEnvironment;

/**
 * Shared support for every test that needs a real, showing top-level
 * window ({@link DialogTextsTest}'s dialog half, {@link RenderingTest},
 * {@link com.wudsn.tools.dis6502.UIWiringTest}).
 *
 * @author Peter Dell
 */
public final class UITest {

	private UITest() {
	}

	/**
	 * Whether such a test must skip itself: there is no display, so a
	 * {@link javax.swing.JDialog}/{@link javax.swing.JFrame} cannot be
	 * constructed - or the build asked for it ({@code
	 * -Ddis6502.skipUITests=true}, e.g. on a CI runner that has a desktop but
	 * nobody to look at the windows the tests open). A plain {@link
	 * javax.swing.JComponent} (a panel, a menu) needs no such check.
	 */
	public static boolean isHeadless() {
		return GraphicsEnvironment.isHeadless() || Boolean.getBoolean("dis6502.skipUITests");
	}
}
