/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;

import com.wudsn.tools.dis6502.Messages;
import com.wudsn.tools.dis6502.model.system.ComputerSystemType;

/**
 * A computer system's real, authentic character set - Atari ATASCII (via
 * "Atari Classic" by Trixter/Kevin Savetz) or C64 PETSCII (via "C64
 * Classic" by Style) - loaded as a genuine TrueType font and drawn with
 * standard {@link Font}/{@link Graphics2D#drawString} calls.
 * <p>
 * An earlier version of this class hand-rasterized each system's legacy
 * raster font file into a PNG glyph atlas (via a small standalone helper
 * tool) and blitted tinted sub-images. A real, correctly-licensed TTF for
 * each system replaces that custom-painted-bitmap approach entirely, using
 * standard Swing text rendering instead: a byte value's <em>authentic
 * hardware</em> glyph (see {@link #drawGlyph}) is the character at
 * {@code codePointBase + byteValue} in the relevant font, drawn with a
 * normal {@link Graphics2D#drawString} call, anti-aliasing disabled to
 * keep this pixel-art font's hard edges crisp. The two fonts' byte-indexed
 * ranges use two different conventions, not a shared one: Atari Classic's
 * Private Use Area block at U+E000 is confirmed pixel-identical, byte
 * value for byte value, to the earlier rasterized Atari atlas; C64
 * Classic's block at U+0100 turned out, after an initial mismatch (hex
 * digits '0'-'9' looked right, 'A'-'F' did not), to be indexed by real C64
 * hardware <em>screen code</em> order rather than raw byte value (screen
 * code 1 is 'A', not byte value 65) - there never was a real earlier C64
 * atlas to compare against anyway, since no earlier asset for this system
 * was genuine PETSCII data to begin with - so this is this port's
 * first-ever authentic PETSCII rendering, screen-code trade-off and all.
 * Because of that screen-code mismatch, only {@link #drawGlyph} (a single
 * raw memory byte) uses the shift; {@link #drawText} (this port's own
 * generated text - hex digits, addresses, disassembly mnemonics) draws
 * every character at its own direct, unshifted code point instead, which
 * both fonts render correctly - see that method's own javadoc.
 * <p>
 * Each glyph is rasterized once, into a small cached {@link BufferedImage}
 * at its native pixel size, rather than calling {@link
 * Graphics2D#drawString} directly against the caller's own on-screen
 * {@link Graphics2D} the way this class used to. That change fixes a real
 * bug found on an actual HiDPI display (Windows at 150% scaling, not
 * reproducible on the offscreen renders used to develop this font
 * support): Swing/AWT applies the desktop's display-scale transform to
 * every on-screen {@code Graphics2D} automatically, and rasterizing this
 * pixel-art font's vector outline straight through a non-1.0, non-hinted
 * transform (antialiasing is off - see below - so there is no hinting to
 * correct for it either) produces inconsistent per-glyph stem coverage -
 * jagged, broken-looking characters, not a uniform blur. Caching each
 * glyph as a small bitmap sidesteps that: the bitmap itself is always
 * rasterized through a fresh {@link BufferedImage}'s own identity-
 * transform {@code Graphics2D}, so it is pixel-perfect regardless of the
 * caller's transform; {@link #drawChar} then draws that already-correctly-
 * sized bitmap back onto the caller's {@code Graphics2D} 1:1, unscaled (see
 * that method's own javadoc for why even a nominally-1:1 <em>scaled</em>
 * draw call was found to drop a pixel row on Windows' hardware-accelerated
 * pipeline) - the same overall bitmap-caching technique {@link
 * GraphicPanel#paintComponent} already uses for the HiDPI reason above,
 * though that class's own 2x on-screen zoom does still need an actual
 * scaled draw, just not through this exact bug's trigger.
 * <p>
 * No TTF is available for Oric/Unknown - those fall back to a plain
 * {@link Font#MONOSPACED} system font with no byte-index shift ({@link
 * #codePointBase} {@code < 0}, drawing each byte value as its raw Unicode
 * code point directly). This is a real accuracy trade-off accepted for
 * those two systems (their true character sets are known to differ, see
 * the earlier atlas-diffing history in this class's git log) in exchange
 * for one simple, standard rendering mechanism everywhere, rather than
 * keeping a second, bitmap-based code path alive for just two systems.
 * <p>
 * {@link #getGlyphWidth}/{@link #getGlyphHeight} already include the
 * caller-chosen {@code zoom} on-screen scale {@link HexGridPanel}/{@link
 * DisassemblyGridPanel} need for legibility (baked into the point size
 * passed to {@link Font#deriveFont}, not a separate scaling step those
 * callers used to do themselves) - see {@link
 * com.wudsn.tools.dis6502.Options#NATIVE_FONT_ZOOM_KEY}, the user's
 * persisted preference for it, unrelated to {@link
 * com.wudsn.tools.dis6502.Options#TEXT_FONT_SIZE_KEY} which only ever
 * applies to a chosen {@link PlainTextFont}. {@link #getAwtFont()} exposes the plain
 * derived {@link Font} for components that just need normal Unicode text
 * at the real system font's style (e.g. {@link SegmentListPanel}'s
 * segment metadata, which is already-formatted text - titles, hex
 * addresses - not raw byte values, so it needs no {@link #codePointBase}
 * shift at all).
 * <p>
 * Implements {@link TextFont} - the "generated text" half of this class's
 * job (everything but {@link #drawGlyph}) - so {@link HexGridPanel}/{@link
 * MemoryInspectorPanel}/{@link DiskImageSectorsDialog} (which always need
 * the workspace's authentic native font, glyphs included) can keep taking a
 * concrete {@code ComputerFont}, while every other panel takes a
 * {@link TextFont} and gets either this class (the default) or a
 * user-chosen {@link PlainTextFont} instead, transparently.
 *
 * @author Peter Dell
 */
public final class ComputerFont implements TextFont {

	private static final int NATIVE_HEIGHT = 8; // Pixels, matching the original 8px raster cell height.

	/** Caches one instance per (system, double height, zoom) combination - a value set, not an enum. */
	private record InstanceKey(ComputerSystemType type, boolean doubleHeight, int zoom) {
	}

	private static final Map<InstanceKey, ComputerFont> INSTANCES = new HashMap<>();

	private static Font atariClassicBase;
	private static Font c64ClassicBase;

	private final Font font;
	private final int glyphWidth;
	private final int glyphHeight;
	private final int codePointBase;

	// Bounded in practice: at most a few hundred distinct (character, color)
	// pairs ever get drawn (the font's own byte-indexed range plus a small,
	// fixed palette of text colors), each a tiny glyphWidth x glyphHeight
	// bitmap - not worth an eviction policy. Painting only ever happens on
	// the EDT, so this needs no synchronization.
	private final Map<Character, Map<Color, BufferedImage>> glyphImageCache = new HashMap<>();

	private ComputerFont(Font font, int glyphWidth, int glyphHeight, int codePointBase) {
		this.font = font;
		this.glyphWidth = glyphWidth;
		this.glyphHeight = glyphHeight;
		this.codePointBase = codePointBase;
	}

	/** Caches one instance per {@link InstanceKey}. {@code zoom} is the on-screen scale - see this class's own javadoc. */
	public static synchronized ComputerFont get(ComputerSystemType type, boolean doubleHeight, int zoom) {
		InstanceKey key = new InstanceKey(type, doubleHeight, zoom);
		ComputerFont existing = INSTANCES.get(key);
		if (existing != null) {
			return existing;
		}
		ComputerFont created = create(type, doubleHeight, zoom);
		INSTANCES.put(key, created);
		return created;
	}

	private static ComputerFont create(ComputerSystemType type, boolean doubleHeight, int zoom) {
		if (type == ComputerSystemType.ATARI5200 || type == ComputerSystemType.ATARI800) {
			return derive(getAtariClassicBase(), doubleHeight, 0xE000, zoom);
		} else if (type == ComputerSystemType.C64) {
			return derive(getC64ClassicBase(), doubleHeight, 0x100, zoom);
		}
		return derive(new Font(Font.MONOSPACED, Font.PLAIN, 1), doubleHeight, -1, zoom); // Oric, unknown.
	}

	/**
	 * Double-height text needs to come out taller but no wider, not simply a
	 * bigger font. {@link Font#deriveFont(float)} alone cannot do that (it
	 * scales a font uniformly), so double-height instead derives the normal-
	 * size font first, then applies a Y-only {@link AffineTransform} scale
	 * on top of it - {@link FontMetrics} correctly reflects the transform
	 * when measuring, so {@link #glyphWidth} still comes out equal to the
	 * normal instance's.
	 */
	private static ComputerFont derive(Font base, boolean doubleHeight, int codePointBase, int zoom) {
		int pixelHeight = NATIVE_HEIGHT * zoom;
		Font sized = base.deriveFont((float) pixelHeight);
		if (doubleHeight) {
			sized = sized.deriveFont(AffineTransform.getScaleInstance(1.0, 2.0));
		}
		BufferedImage probe = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2 = probe.createGraphics();
		try {
			FontMetrics metrics = g2.getFontMetrics(sized);
			int probeCodePoint = codePointBase < 0 ? 'M' : codePointBase;
			int width = metrics.charWidth(probeCodePoint);
			int height = doubleHeight ? pixelHeight * 2 : pixelHeight;
			return new ComputerFont(sized, width > 0 ? width : Math.max(1, pixelHeight / 2), height, codePointBase);
		} finally {
			g2.dispose();
		}
	}

	private static synchronized Font getAtariClassicBase() {
		if (atariClassicBase == null) {
			atariClassicBase = loadFont("fonts/AtariClassic-Regular.ttf");
		}
		return atariClassicBase;
	}

	private static synchronized Font getC64ClassicBase() {
		if (c64ClassicBase == null) {
			c64ClassicBase = loadFont("fonts/C64Classic-Regular.ttf");
		}
		return c64ClassicBase;
	}

	private static Font loadFont(String resourceName) {
		try (InputStream in = ComputerFont.class.getResourceAsStream(resourceName)) {
			if (in == null) {
				// ERROR: Font resource "{0}" not found.
				throw new IOException(Messages.E066.format(resourceName));
			}
			return Font.createFont(Font.TRUETYPE_FONT, in);
		} catch (IOException ex) {
			throw new UncheckedIOException(ex);
		} catch (FontFormatException ex) {
			throw new UncheckedIOException(new IOException(ex));
		}
	}

	/** Already scaled for on-screen legibility - see this class's javadoc. */
	@Override
	public int getGlyphWidth() {
		return glyphWidth;
	}

	/** Already scaled for on-screen legibility - see this class's javadoc. */
	@Override
	public int getGlyphHeight() {
		return glyphHeight;
	}

	/** The plain derived font, for components that draw normal Unicode text rather than byte-indexed glyphs (see this class's javadoc). */
	@Override
	public Font getAwtFont() {
		return font;
	}

	@Override
	public Object getTextAntialiasingHint() {
		return RenderingHints.VALUE_TEXT_ANTIALIAS_OFF;
	}

	/**
	 * Draws byte value {@code value}'s <em>authentic hardware</em> glyph at
	 * {@code (x, y)} - shifted by {@link #codePointBase} - matching {@code
	 * PrintLine}'s byte-indexed rendering of a raw memory byte's own on-
	 * screen appearance. Use this only for an actual byte value read from a
	 * segment (the memory inspector's ASCII/ATASCII column); for normal
	 * generated text, use {@link #drawText}.
	 */
	public void drawGlyph(Graphics2D g2, int value, Color color, int x, int y) {
		char ch = (char) (codePointBase < 0 ? (value & 0xFF) : codePointBase + (value & 0xFF));
		drawChar(g2, ch, color, x, y);
	}

	/**
	 * Draws {@code text} as a horizontal run of glyphs starting at {@code
	 * (x, y)}, at each character's own direct Unicode code point - no
	 * {@link #codePointBase} shift. Correct for normal text this port
	 * itself generates (hex digits, addresses, disassembly mnemonics/
	 * labels/comments): both fonts' direct ASCII range already renders
	 * correctly (confirmed identical to the byte-indexed range for Atari
	 * Classic; C64 Classic's byte-indexed range turned out to use real C64
	 * hardware screen-code order instead of raw byte value - 'A' lives at
	 * screen code 1, not 65 - so shifting normal text through it would
	 * have made hex digits and the whole disassembly listing unreadable
	 * for C64). A disassembly line's own embedded literal ATASCII/PETSCII
	 * string data (e.g. an {@code .SBYTE "..."} directive's quoted text)
	 * therefore only renders authentically for its printable-range bytes,
	 * not the graphics-range ones a raw byte dump would still show
	 * correctly via {@link #drawGlyph} - a real but minor trade-off, since
	 * a byte run the disassembler classified as a STRING is by definition
	 * mostly printable already.
	 */
	@Override
	public void drawText(Graphics2D g2, String text, Color color, int x, int y) {
		for (int i = 0; i < text.length(); i++) {
			drawChar(g2, text.charAt(i), color, x + i * glyphWidth, y);
		}
	}

	/**
	 * {@code glyphImage} is always exactly {@link #glyphWidth} x {@link
	 * #glyphHeight} already (see {@link #renderGlyphImage}), so this draws
	 * it 1:1 via the plain, unscaled {@link Graphics2D#drawImage(Image, int,
	 * int, java.awt.image.ImageObserver)} overload rather than the
	 * width/height-taking one - deliberately, even though the numbers would
	 * come out identical either way: on a real on-screen {@code Graphics2D}
	 * (never reproduced through an offscreen {@link BufferedImage}), the
	 * scaled overload still goes through a texture-sampling path whose
	 * nearest-neighbor rounding is sensitive to the ambient device-scale
	 * transform even at a nominal 1:1 logical scale - confirmed from a
	 * reported screenshot's raw pixels: most source rows appeared exactly
	 * twice but some only once, an irregular duplication pattern, not a
	 * clean doubling - the signature of a fractional scale factor being
	 * rounded per-row rather than applied uniformly. Small at the smallest
	 * zoom levels, where every row is a larger fraction of the glyph;
	 * effectively invisible at double height, where genuine intentional
	 * duplication already dominates. The unscaled overload has no scale
	 * factor to round at all.
	 */
	private void drawChar(Graphics2D g2, char ch, Color color, int x, int y) {
		BufferedImage glyphImage = getGlyphImage(ch, color);
		g2.drawImage(glyphImage, x, y, null);
	}

	private BufferedImage getGlyphImage(char ch, Color color) {
		Map<Color, BufferedImage> byColor = glyphImageCache.computeIfAbsent(ch, k -> new HashMap<>());
		BufferedImage glyphImage = byColor.get(color);
		if (glyphImage == null) {
			glyphImage = renderGlyphImage(ch, color);
			byColor.put(color, glyphImage);
		}
		return glyphImage;
	}

	/**
	 * Rasterizes {@code ch} into a new, small bitmap via a fresh {@link
	 * BufferedImage}'s own identity-transform {@code Graphics2D} - see this
	 * class's javadoc for why that, rather than drawing directly onto the
	 * caller's own (possibly display-scaled) {@code Graphics2D}, is what
	 * keeps this pixel-art font crisp on a HiDPI display.
	 */
	private BufferedImage renderGlyphImage(char ch, Color color) {
		BufferedImage glyphImage = new BufferedImage(glyphWidth, glyphHeight, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2 = glyphImage.createGraphics();
		try {
			g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
			g2.setFont(font);
			g2.setColor(color);
			FontMetrics metrics = g2.getFontMetrics(font);
			g2.drawString(String.valueOf(ch), 0, metrics.getAscent());
		} finally {
			g2.dispose();
		}
		return glyphImage;
	}
}
