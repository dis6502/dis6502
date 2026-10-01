/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

/**
 * Renders this port's own already-generated text (mnemonics, labels,
 * comments, list entries, log lines, part panel titles) in the user-chosen
 * {@link PlainTextFont} - see {@code plans/09_CUSTOM_TEXT_FONT_PROPOSAL.md}.
 * The memory inspector's grid uses {@link ComputerFont} instead, which
 * renders an 8x8 character set and has no AWT {@link Font} behind it.
 *
 * @author Peter Dell
 */
public interface TextFont {

	/** Already scaled for on-screen legibility - see the implementing class's own javadoc. */
	int getGlyphWidth();

	/** Already scaled for on-screen legibility - see the implementing class's own javadoc. */
	int getGlyphHeight();

	/** The plain derived font, for components that draw normal Unicode text via standard Swing text rendering. */
	Font getAwtFont();

	/** Draws {@code text} as a horizontal run of characters starting at {@code (x, y)}, at each character's own direct Unicode code point. */
	void drawText(Graphics2D g2, String text, Color color, int x, int y);

	/**
	 * The {@code RenderingHints.KEY_TEXT_ANTIALIASING} value a plain Swing
	 * text component (one that paints its own text directly instead of
	 * going through {@link #drawText}, e.g. {@link LogPanel}'s {@code
	 * JTextPane}) should be given for this font to look right. {@link
	 * #drawText} callers/{@link ComputerFontListCellRenderer} need no such
	 * hook - each implementation already renders itself correctly there.
	 */
	Object getTextAntialiasingHint();
}
