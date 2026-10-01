/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.ui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

import com.wudsn.tools.dis6502.model.CharacterSet;

/**
 * Renders an 8x8 {@link CharacterSet} at a chosen pixel height - the font of
 * {@link HexGridPanel}, for both raw memory bytes ({@link #drawGlyph}) and
 * its own address/hex text ({@link #drawText}).
 * <p>
 * Each glyph is built directly from the set's 8 bytes: every set bit
 * becomes a {@code zoom} x {@code zoom} block, {@code zoom} being the pixel
 * height divided by {@link #NATIVE_HEIGHT}. That is plain pixel
 * replication of the real data, crisp at every size, with no rasterizer,
 * antialiasing or hinting involved.
 * <p>
 * Each glyph bitmap is cached per color and drawn 1:1 onto the caller's
 * {@code Graphics2D} via the unscaled {@code drawImage} overload, never
 * through a scaling call. On a real on-screen {@code Graphics2D} with a
 * fractional display scale (e.g. Windows at 150%), the scaled overload's
 * nearest-neighbor sampling was found to duplicate some pixel rows but not
 * others, an irregular pattern confirmed from a screenshot's raw pixels and
 * not reproducible offscreen; the unscaled overload has no scale factor to
 * round.
 *
 * @author Peter Dell
 */
public final class ComputerFont {

	/** Pixels, matching the 8px raster cell of the character sets. */
	public static final int NATIVE_HEIGHT = 8;

	/** Caches one instance per (character set, pixel height) combination. */
	private record InstanceKey(CharacterSet characterSet, int pixelHeight) {
	}

	private static final Map<InstanceKey, ComputerFont> INSTANCES = new HashMap<>();

	private final CharacterSet characterSet;
	private final int pixelHeight;

	// Bounded in practice: at most 256 screen codes times a small, fixed
	// palette of text colors, each a small bitmap - not worth an eviction
	// policy. Painting only ever happens on the EDT, so this needs no
	// synchronization.
	private final Map<Integer, Map<Color, BufferedImage>> glyphImageCache = new HashMap<>();

	private ComputerFont(CharacterSet characterSet, int pixelHeight) {
		this.characterSet = characterSet;
		this.pixelHeight = pixelHeight;
	}

	/**
	 * Caches one instance per {@link InstanceKey}. {@code pixelHeight} is
	 * rounded to the nearest whole multiple of {@link #NATIVE_HEIGHT} (at
	 * least one), so every pixel of the 8x8 glyph becomes a whole number of
	 * screen pixels.
	 */
	public static synchronized ComputerFont get(CharacterSet characterSet, int pixelHeight) {
		if (characterSet == null) {
			throw new IllegalArgumentException("Parameter 'characterSet' must not be null.");
		}
		int snappedPixelHeight = Math.max(1, Math.round((float) pixelHeight / NATIVE_HEIGHT)) * NATIVE_HEIGHT;
		return INSTANCES.computeIfAbsent(new InstanceKey(characterSet, snappedPixelHeight),
				key -> new ComputerFont(key.characterSet(), key.pixelHeight()));
	}

	public CharacterSet getCharacterSet() {
		return characterSet;
	}

	/** Already scaled for on-screen legibility - the glyph cell is square. */
	public int getGlyphWidth() {
		return pixelHeight;
	}

	/** Already scaled for on-screen legibility - the glyph cell is square. */
	public int getGlyphHeight() {
		return pixelHeight;
	}

	/**
	 * Draws the glyph of the raw memory byte {@code value} at {@code (x, y)}:
	 * as a screen code if {@code screenCode} is set, otherwise as a byte of
	 * the character set's own computer (ATASCII or PETSCII).
	 */
	public void drawGlyph(Graphics2D g2, int value, boolean screenCode, Color color, int x, int y) {
		drawScreenCode(g2, screenCode ? value & 0xFF : characterSet.toScreenCode(value), color, x, y);
	}

	/** Draws this port's own ASCII text (addresses, hex digits) as a horizontal run of glyphs starting at {@code (x, y)}. */
	public void drawText(Graphics2D g2, String text, Color color, int x, int y) {
		for (int i = 0; i < text.length(); i++) {
			drawScreenCode(g2, characterSet.textToScreenCode(text.charAt(i)), color, x + i * pixelHeight, y);
		}
	}

	private void drawScreenCode(Graphics2D g2, int screenCode, Color color, int x, int y) {
		Map<Color, BufferedImage> byColor = glyphImageCache.computeIfAbsent(screenCode, k -> new HashMap<>());
		BufferedImage glyphImage = byColor.computeIfAbsent(color, c -> renderGlyphImage(screenCode, c));
		g2.drawImage(glyphImage, x, y, null);
	}

	private BufferedImage renderGlyphImage(int screenCode, Color color) {
		int zoom = pixelHeight / NATIVE_HEIGHT;
		int rgb = color.getRGB();
		byte[] rows = characterSet.getGlyph(screenCode);
		BufferedImage glyphImage = new BufferedImage(pixelHeight, pixelHeight, BufferedImage.TYPE_INT_ARGB);
		for (int row = 0; row < NATIVE_HEIGHT; row++) {
			for (int column = 0; column < NATIVE_HEIGHT; column++) {
				if ((rows[row] & (0x80 >> column)) != 0) {
					for (int dy = 0; dy < zoom; dy++) {
						for (int dx = 0; dx < zoom; dx++) {
							glyphImage.setRGB(column * zoom + dx, row * zoom + dy, rgb);
						}
					}
				}
			}
		}
		return glyphImage;
	}
}
