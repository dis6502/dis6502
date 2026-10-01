/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.ui;

import java.awt.Font;
import java.awt.RenderingHints;

import com.wudsn.tools.dis6502.model.Assert;

/**
 * Headless - {@link PlainTextFont#get} only probes {@link
 * java.awt.FontMetrics} against an offscreen {@link
 * java.awt.image.BufferedImage}, the same way {@link ComputerFont#derive}
 * does, so it needs no real display.
 *
 * @author Peter Dell
 */
public final class PlainTextFontTest {

	private PlainTextFontTest() {
	}

	public static void testPlainTextFont() {
		PlainTextFont font = PlainTextFont.get(Font.MONOSPACED, 16);
		Assert.boolEquals(font.getGlyphWidth() > 0, true);
		Assert.boolEquals(font.getGlyphHeight() > 0, true);
		Assert.notNull(font.getAwtFont());
		Assert.longEquals(font.getAwtFont().getSize(), 16);
		Assert.boolEquals(font.getTextAntialiasingHint() == RenderingHints.VALUE_TEXT_ANTIALIAS_ON, true);

		Assert.log("PlainTextFontTest completed");
	}
}
