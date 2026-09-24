/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

/**
 * A {@link TextFont} wrapping an arbitrary, user-chosen mono-spaced system
 * font - the counterpart to {@link ComputerFont} for every panel that does
 * not need the workspace's authentic native font (see
 * {@code plans/CUSTOM_TEXT_FONT_PROPOSAL.md}).
 * <p>
 * Deliberately much simpler than {@link ComputerFont}: no per-character
 * bitmap cache, no forced-off antialiasing. Those exist in
 * {@link ComputerFont} to keep the tiny pixel-art retro TTFs crisp at any
 * display scale - a concern that does not apply to an ordinary system font,
 * where plain antialiased {@link Graphics2D#drawString} is what actually
 * looks right. A single {@code drawString} call per {@link #drawText} is
 * also enough to keep every character at this font's own uniform advance
 * width, since only genuinely mono-spaced fonts are ever offered as a
 * choice - unlike {@link ComputerFont}, which cannot assume that about the
 * retro TTFs' authentic byte-indexed glyphs and so places each character
 * itself.
 *
 * @author Peter Dell
 */
public final class PlainTextFont implements TextFont {

	private final Font font;
	private final int glyphWidth;
	private final int glyphHeight;

	private PlainTextFont(Font font, int glyphWidth, int glyphHeight) {
		this.font = font;
		this.glyphWidth = glyphWidth;
		this.glyphHeight = glyphHeight;
	}

	/**
	 * Derives a {@link PlainTextFont} from an installed font family at
	 * {@code pointSize} - an ordinary text size the caller chooses, not
	 * {@link ComputerFont}'s tiny native-pixel-height derivation, since a
	 * user-chosen readable font wants a normal point size. {@code
	 * doubleHeight} applies the same Y-only {@link AffineTransform} scale
	 * {@link ComputerFont#derive} uses, so double-height mode stretches a
	 * custom font the same way it stretches the native one.
	 */
	public static PlainTextFont get(String fontFamilyName, int pointSize, boolean doubleHeight) {
		Font font = new Font(fontFamilyName, Font.PLAIN, pointSize);
		if (doubleHeight) {
			font = font.deriveFont(AffineTransform.getScaleInstance(1.0, 2.0));
		}
		BufferedImage probe = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2 = probe.createGraphics();
		try {
			FontMetrics metrics = g2.getFontMetrics(font);
			return new PlainTextFont(font, metrics.charWidth('M'), metrics.getHeight());
		} finally {
			g2.dispose();
		}
	}

	@Override
	public int getGlyphWidth() {
		return glyphWidth;
	}

	@Override
	public int getGlyphHeight() {
		return glyphHeight;
	}

	@Override
	public Font getAwtFont() {
		return font;
	}

	@Override
	public Object getTextAntialiasingHint() {
		return RenderingHints.VALUE_TEXT_ANTIALIAS_ON;
	}

	@Override
	public void drawText(Graphics2D g2, String text, Color color, int x, int y) {
		Object oldHint = g2.getRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING);
		g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g2.setFont(font);
		g2.setColor(color);
		g2.drawString(text, x, y + g2.getFontMetrics(font).getAscent());
		if (oldHint != null) {
			g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, oldHint);
		}
	}
}
