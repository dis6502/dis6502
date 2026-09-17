/*
 * Copyright (C) 2025 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.ui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import com.wudsn.tools.dis6502.model.ComputerSystemType;

/**
 * A computer system's byte-indexed character set (ATASCII, PETSCII, etc.),
 * as a 16x16 grid of pre-rendered glyph images: image column/row {@code (b
 * % 16, b / 16)} is byte value {@code b}'s glyph.
 * <p>
 * Ported from ui/ComputerFont.h/.cpp - but not the actual font loading:
 * {@code ComputerFont::Load}/{@code RegisterFontResource} register one of
 * the {@code systems/*&#47;*.fon} files (legacy 16-bit Windows raster font
 * resources - an "MZ"-header NE executable, not TrueType/OpenType) as a GDI
 * private font resource, which has no Java equivalent - {@link
 * javax.imageio.ImageIO}/{@link java.awt.Font#createFont} cannot load that
 * format at all. Instead, each {@code .fon} file's 256 glyphs (at the same
 * 8px/16px pixel heights {@code ComputerFont::CreateFonts} requests, and
 * rendered the same {@code OEM_CHARSET}/{@code FIXED_PITCH} way, direct
 * byte value to glyph index with no code-page translation) were rasterized
 * once, on this Windows development machine, by a small standalone GDI
 * helper (not part of either build) into a PNG atlas per {@link
 * ComputerSystemType}/height, embedded here as a resource - see this
 * package's {@code fonts/} directory. This keeps the actual pixel data
 * genuinely sourced from the original asset files (not redrawn/approximated
 * by a substitute font) while working within what a headless-buildable Java
 * app can load.
 * <p>
 * Note: {@code systems/c64/C64.fon} and {@code systems/atari800/Atari800.fon}
 * are byte-for-byte identical files in the C++ source (as are {@code
 * systems/oric/Oric.fon} and {@code systems/unknown/Unknown.fon}), so
 * {@link ComputerSystemType#C64}/{@link ComputerSystemType#ATARI800} (and
 * {@link ComputerSystemType#ORIC}/{@link ComputerSystemType#UNKNOWN})
 * currently render identical Atari ATASCII glyphs in both this port and the
 * original - not a porting gap, just the current state of that C++ asset.
 * <p>
 * {@link #getTintedGlyph} is the shared building block both {@link
 * MemoryInspectorGridPanel} and {@link DisassemblyGridPanel} paint their
 * text with - recoloring a glyph is the one piece of per-character
 * rendering work both of those custom-painted, byte/character-grid panels
 * need identically ({@code PrintLine}'s per-{@code MemoryType} hex-byte
 * color and a disassembly line's plain black text are both just "this
 * glyph, in this color"), so it lives here rather than being duplicated in
 * each panel.
 *
 * @author Peter Dell
 */
public final class ComputerFont {

	private static final int COLUMNS = 16;
	private static final int ROWS = 16;

	private static final Map<ComputerSystemType, ComputerFont> NORMAL_INSTANCES = new EnumMap<>(ComputerSystemType.class);
	private static final Map<ComputerSystemType, ComputerFont> DOUBLE_HEIGHT_INSTANCES = new EnumMap<>(ComputerSystemType.class);

	private final BufferedImage atlas;
	private final int glyphWidth;
	private final int glyphHeight;
	private final Map<Long, BufferedImage> tintedGlyphCache = new HashMap<>();

	private ComputerFont(BufferedImage atlas) {
		this.atlas = atlas;
		this.glyphWidth = atlas.getWidth() / COLUMNS;
		this.glyphHeight = atlas.getHeight() / ROWS;
	}

	/** Ported from ComputerFont::Get, split by height instead of returning both via GetFont(bool). */
	public static ComputerFont get(ComputerSystemType type, boolean doubleHeight) {
		Map<ComputerSystemType, ComputerFont> instances = doubleHeight ? DOUBLE_HEIGHT_INSTANCES : NORMAL_INSTANCES;
		return instances.computeIfAbsent(type, t -> new ComputerFont(loadAtlas(t, doubleHeight)));
	}

	private static BufferedImage loadAtlas(ComputerSystemType type, boolean doubleHeight) {
		String fileName = fileNameFor(type) + (doubleHeight ? "-16.png" : "-8.png");
		try (InputStream in = ComputerFont.class.getResourceAsStream("fonts/" + fileName)) {
			if (in == null) {
				throw new IOException("Font atlas resource not found: " + fileName);
			}
			return ImageIO.read(in);
		} catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	private static String fileNameFor(ComputerSystemType type) {
		switch (type) {
		case ATARI5200:
			return "Atari5200";
		case ATARI800:
			return "Atari800";
		case C64:
			return "C64";
		case ORIC:
			return "Oric";
		case UNKNOWN:
		default:
			return "Unknown";
		}
	}

	public int getGlyphWidth() {
		return glyphWidth;
	}

	public int getGlyphHeight() {
		return glyphHeight;
	}

	/** Returns the atlas sub-image for byte value {@code value} (0-255), the glyph {@code PrintLine} would draw for it. */
	public BufferedImage getGlyph(int value) {
		int index = value & 0xFF;
		int column = index % COLUMNS;
		int row = index / COLUMNS;
		return atlas.getSubimage(column * glyphWidth, row * glyphHeight, glyphWidth, glyphHeight);
	}

	/**
	 * Returns byte value {@code value}'s glyph recolored to {@code color}:
	 * black pixels become {@code color}, white pixels become fully
	 * transparent (so it can be drawn over any background, including a
	 * selection highlight, and still show only its foreground strokes).
	 * Cached per (value, color) pair.
	 */
	public BufferedImage getTintedGlyph(int value, Color color) {
		long key = ((long) (value & 0xFF) << 32) | (color.getRGB() & 0xFFFFFFFFL);
		return tintedGlyphCache.computeIfAbsent(key, k -> tint(getGlyph(value), color));
	}

	private static BufferedImage tint(BufferedImage glyph, Color color) {
		int width = glyph.getWidth();
		int height = glyph.getHeight();
		BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		int foreground = color.getRGB() | 0xFF000000;
		for (int py = 0; py < height; py++) {
			for (int px = 0; px < width; px++) {
				int rgb = glyph.getRGB(px, py) & 0xFFFFFF;
				result.setRGB(px, py, rgb == 0 ? foreground : 0);
			}
		}
		return result;
	}

	/** Draws {@code text} as a horizontal run of glyphs starting at {@code (x, y)}, each {@code cellWidth}x{@code cellHeight} pixels. */
	public void drawText(Graphics2D g2, String text, Color color, int x, int y, int cellWidth, int cellHeight) {
		for (int i = 0; i < text.length(); i++) {
			BufferedImage tinted = getTintedGlyph(text.charAt(i) & 0xFF, color);
			g2.drawImage(tinted, x + i * cellWidth, y, cellWidth, cellHeight, null);
		}
	}
}
