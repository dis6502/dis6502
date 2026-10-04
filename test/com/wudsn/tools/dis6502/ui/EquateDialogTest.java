/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.ui;

import com.wudsn.tools.dis6502.model.Assert;
import com.wudsn.tools.dis6502.model.EquateList;

/**
 * Add/Modify in {@link EquateDialog} reports why a line cannot be parsed
 * instead of ignoring it: {@link EquateDialog#parse} gives the equate or the
 * reason. Headless; the message box itself is a plain {@code JOptionPane}.
 *
 * @author Peter Dell
 */
public final class EquateDialogTest {

	private EquateDialogTest() {
	}

	public static void testEquateDialog() {
		EquateList.EquateResult result = EquateDialog.parse("COLBK = $D01A");
		Assert.notNull(result.equate);
		Assert.stringEquals(result.error, "");

		result = EquateDialog.parse("COLBK =");
		Assert.isNull(result.equate);
		Assert.stringEquals(result.error, "No value specified.");
	}
}
