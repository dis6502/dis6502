/*
 * Copyright (C) 2025 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.ui;

/**
 * The characteristics of one Atari ANTIC graphics mode (8 through 15), as
 * used to render a raw byte buffer as a picture in {@link GraphicPanel}.
 * <p>
 * Ported from the {@code wSpriteNbRows}/{@code wSpriteNbLines}/{@code
 * wSpriteNbColors}/{@code wSpriteNbBytesPerLine}/{@code
 * wSpriteNbPixelsPerByte}/{@code wSpritePixelWidth}/{@code
 * wSpritePixelHeight} parallel arrays in ui/SpriteControlImpl.cpp, and the
 * {@code IDS_SPRITE_ANTIC_8}..{@code _F} resource strings (dis6502.rc) for
 * {@link #label} - see {@link SelectGraphicsDialog}'s own javadoc for why
 * this port renames the C++ source's "Sprite" naming to "Graphic"
 * throughout, despite those citations staying accurate to the real C++
 * identifiers. {@code colors} is kept for fidelity even though nothing
 * in this port (or, as far as {@link #bitsPerPixel} suggests, the C++
 * source either) actually uses it for pixel decoding - that is fixed per
 * mode via {@link #bitsPerPixel}, matching the C++ version's hardcoded
 * per-mode choice between its {@code SPRITE_GET_PIXEL_1}/{@code _2}
 * macros. Note {@link #ANTIC_F}'s label says "2 Colors" while {@code
 * wSpriteNbColors} records 1 for it - both are defensible (it renders
 * exactly two colors, black and white, using 1 bit per pixel) and this
 * keeps the mismatch exactly as found rather than guessing which one to
 * "fix".
 *
 * @author Peter Dell
 */
public enum GraphicMode {

	ANTIC_8(8, "ANTIC 8 ( 40 x  24 Pixels, 4 Colors)", 4, 10, 4, 8, 8),
	ANTIC_9(9, "ANTIC 9 ( 80 x  48 Pixels, 2 Colors)", 2, 10, 8, 4, 4),
	ANTIC_A(10, "ANTIC A ( 80 x  48 Pixels, 4 Colors)", 4, 20, 4, 4, 4),
	ANTIC_B(11, "ANTIC B (160 x  96 Pixels, 2 Colors)", 2, 20, 8, 2, 2),
	ANTIC_C(12, "ANTIC C (160 x 192 Pixels, 2 Colors)", 2, 20, 8, 2, 1),
	ANTIC_D(13, "ANTIC D (160 x  96 Pixels, 4 Colors)", 4, 40, 4, 2, 2),
	ANTIC_E(14, "ANTIC E (160 x 192 Pixels, 4 Colors)", 4, 40, 4, 2, 1),
	ANTIC_F(15, "ANTIC F (320 x 192 Pixels, 2 Colors)", 1, 40, 8, 1, 1);

	/** The ANTIC mode number (8-15), matching {@code SetMode}'s expected range. */
	public final int anticMode;
	public final String label;
	public final int colors;
	/** Maximum bytes per displayed line for this mode. */
	public final int bytesPerLine;
	public final int pixelsPerByte;
	/** Width, in screen pixels, of one logical pixel in this mode. */
	public final int pixelWidth;
	/** Height, in screen pixels, of one logical pixel in this mode - also the vertical step between displayed buffer lines. */
	public final int pixelHeight;

	GraphicMode(int anticMode, String label, int colors, int bytesPerLine, int pixelsPerByte, int pixelWidth, int pixelHeight) {
		this.anticMode = anticMode;
		this.label = label;
		this.colors = colors;
		this.bytesPerLine = bytesPerLine;
		this.pixelsPerByte = pixelsPerByte;
		this.pixelWidth = pixelWidth;
		this.pixelHeight = pixelHeight;
	}

	/** Ported from the choice between SPRITE_GET_PIXEL_1 (1 bit) and SPRITE_GET_PIXEL_2 (2 bits) in PaintAll's per-mode switch. */
	public int bitsPerPixel() {
		return colors == 4 ? 2 : 1;
	}

	public static GraphicMode forAnticMode(int anticMode) {
		for (GraphicMode mode : values()) {
			if (mode.anticMode == anticMode) {
				return mode;
			}
		}
		throw new IllegalArgumentException("Invalid ANTIC mode: " + anticMode + ".");
	}

	@Override
	public String toString() {
		return label;
	}
}
