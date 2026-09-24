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
 * Renders this port's own already-generated text (hex digits, addresses,
 * mnemonics, labels, comments, list entries, log lines) - the part of
 * {@link ComputerFont}'s job that is not computer-specific. Implemented by
 * {@link ComputerFont} itself (the workspace's native font, always used by
 * {@link HexGridPanel}/{@link MemoryInspectorPanel}/{@link
 * DiskImageSectorsDialog}) and by {@link PlainTextFont} (a user-chosen
 * mono-spaced font, for every other panel - see
 * {@code plans/CUSTOM_TEXT_FONT_PROPOSAL.md}). Deliberately excludes
 * {@link ComputerFont#drawGlyph}: no consumer of this interface ever draws
 * a raw byte's byte-indexed hardware glyph, and a plain chosen font has no
 * such range to shift into anyway.
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
	 * JTextPane}) should be given for this font to look right -
	 * {@code VALUE_TEXT_ANTIALIAS_OFF} for {@link ComputerFont}'s pixel-art
	 * glyphs, {@code VALUE_TEXT_ANTIALIAS_ON} for an ordinary font like
	 * {@link PlainTextFont}. {@link #drawText} callers/{@link
	 * ComputerFontListCellRenderer} need no such hook - each implementation
	 * already renders itself correctly there.
	 */
	Object getTextAntialiasingHint();
}
