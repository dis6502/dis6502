/*
 * Copyright (C) 2025 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
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
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import com.wudsn.tools.dis6502.model.ComputerSystemType;

/**
 * A computer system's real, authentic character set - Atari ATASCII (via
 * "Atari Classic" by Trixter/Kevin Savetz) or C64 PETSCII (via "C64
 * Classic" by Style) - loaded as a genuine TrueType font and drawn with
 * standard {@link Font}/{@link Graphics2D#drawString} calls.
 * <p>
 * Ported from ui/ComputerFont.h/.cpp, but not its mechanism: the C++
 * version registers one of the {@code systems/*&#47;*.fon} files (legacy
 * 16-bit Windows raster font resources) as a GDI private font resource,
 * which Java cannot load at all. An earlier version of this class instead
 * hand-rasterized each system's {@code .fon} file into a PNG glyph atlas
 * (via a small standalone GDI helper, {@code tools/FontRasterizer} in the
 * C++ repository) and blitted tinted sub-images - a real, correctly-
 * licensed TTF for each system replaces that custom-painted-bitmap
 * approach entirely, using standard Swing text rendering instead: a byte
 * value's <em>authentic hardware</em> glyph (see {@link #drawGlyph}) is
 * the character at {@code codePointBase + byteValue} in the relevant
 * font, drawn with a normal {@link Graphics2D#drawString} call, anti-
 * aliasing disabled to keep this pixel-art font's hard edges crisp. The
 * two fonts' byte-indexed ranges use two different conventions, not a
 * shared one: Atari Classic's Private Use Area block at U+E000 is
 * confirmed pixel-identical, byte value for byte value, to the earlier
 * GDI-rasterized Atari atlas; C64 Classic's block at U+0100 turned out,
 * after an initial mismatch (hex digits '0'-'9' looked right, 'A'-'F'
 * did not), to be indexed by real C64 hardware <em>screen code</em>
 * order rather than raw byte value (screen code 1 is 'A', not byte value
 * 65) - there never was a real earlier C64 atlas to compare against
 * anyway, since the C++ source's own {@code C64.fon} turned out to be a
 * byte-for-byte copy of {@code Atari800.fon} (a mislabeled asset, not a
 * genuine PETSCII character set) - so this is this port's first-ever
 * authentic PETSCII rendering, screen-code trade-off and all. Because of
 * that screen-code mismatch, only {@link #drawGlyph} (a single raw
 * memory byte) uses the shift; {@link #drawText} (this port's own
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
 * caller's transform; drawing that bitmap back onto the caller's
 * {@code Graphics2D} with {@link RenderingHints#VALUE_INTERPOLATION_NEAREST_NEIGHBOR}
 * scales it by simple pixel replication, which stays crisp and blocky at
 * any display scale instead of reintroducing hinting-related artifacts -
 * the same technique {@link SpritePanel#paintComponent} already uses for
 * the same reason.
 * <p>
 * No TTF is available for Oric/Unknown - those fall back to a plain
 * {@link Font#MONOSPACED} system font with no byte-index shift ({@link
 * #codePointBase} {@code < 0}, drawing each byte value as its raw Unicode
 * code point directly), matching {@code ComputerFont::Load}'s own
 * fallback to {@code "Courier New"} when a system's {@code .fon} file
 * cannot be loaded - the same idea, just Java has no {@code .fon} to fail
 * to load in the first place. This is a real accuracy trade-off accepted
 * for those two systems (their true character sets are known to differ,
 * see the earlier atlas-diffing history in this class's git log) in
 * exchange for one simple, standard rendering mechanism everywhere,
 * rather than keeping a second, bitmap-based code path alive for just
 * two systems.
 * <p>
 * {@link #getGlyphWidth}/{@link #getGlyphHeight} already include the 2x
 * on-screen scale {@link MemoryInspectorGridPanel}/{@link
 * DisassemblyGridPanel} need for legibility (baked into the point size
 * passed to {@link Font#deriveFont}, not a separate scaling step those
 * callers used to do themselves) - the C++ source's own {@code "TODO:
 * Test this, the actual dialog is too small!"} comment already justified
 * that adjustment for {@link SpritePanel}, and the same reasoning applies
 * here. {@link #getAwtFont()} exposes the plain derived {@link Font} for
 * components that just need normal Unicode text at the real system font's
 * style (e.g. {@link SegmentListPanel}'s segment metadata, which is
 * already-formatted text - titles, hex addresses - not raw byte values,
 * so it needs no {@link #codePointBase} shift at all).
 *
 * @author Peter Dell
 */
public final class ComputerFont {

	private static final int ZOOM = 1;
	private static final int NATIVE_HEIGHT = 8; // Pixels, matching the original 8px raster cell height.

	private static final Map<ComputerSystemType, ComputerFont> NORMAL_INSTANCES = new EnumMap<>(ComputerSystemType.class);
	private static final Map<ComputerSystemType, ComputerFont> DOUBLE_HEIGHT_INSTANCES = new EnumMap<>(ComputerSystemType.class);

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

	/** Ported from ComputerFont::Get, split by height instead of returning both via GetFont(bool). */
	public static synchronized ComputerFont get(ComputerSystemType type, boolean doubleHeight) {
		Map<ComputerSystemType, ComputerFont> instances = doubleHeight ? DOUBLE_HEIGHT_INSTANCES : NORMAL_INSTANCES;
		ComputerFont existing = instances.get(type);
		if (existing != null) {
			return existing;
		}
		ComputerFont created = create(type, doubleHeight);
		instances.put(type, created);
		return created;
	}

	private static ComputerFont create(ComputerSystemType type, boolean doubleHeight) {
		switch (type) {
		case ATARI5200:
		case ATARI800:
			return derive(getAtariClassicBase(), doubleHeight, 0xE000);
		case C64:
			return derive(getC64ClassicBase(), doubleHeight, 0x100);
		case ORIC:
		case UNKNOWN:
		default:
			return derive(new Font(Font.MONOSPACED, Font.PLAIN, 1), doubleHeight, -1);
		}
	}

	/**
	 * Ported from {@code ComputerFont::CreateFonts}'s two {@code CreateFont}
	 * calls - the C++ source only doubles the {@code nHeight} parameter for
	 * its double-height font, leaving width at its natural (auto) value, so
	 * double-height text there comes out taller but no wider, not simply a
	 * bigger font. {@link Font#deriveFont(float)} alone cannot do that (it
	 * scales a font uniformly), so double-height instead derives the normal-
	 * size font first, then applies a Y-only {@link AffineTransform} scale
	 * on top of it - {@link FontMetrics} correctly reflects the transform
	 * when measuring, so {@link #glyphWidth} still comes out equal to the
	 * normal instance's.
	 */
	private static ComputerFont derive(Font base, boolean doubleHeight, int codePointBase) {
		int pixelHeight = NATIVE_HEIGHT * ZOOM;
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
				throw new IOException("Font resource not found: " + resourceName);
			}
			return Font.createFont(Font.TRUETYPE_FONT, in);
		} catch (IOException ex) {
			throw new UncheckedIOException(ex);
		} catch (FontFormatException ex) {
			throw new UncheckedIOException(new IOException(ex));
		}
	}

	/** Already scaled for on-screen legibility - see this class's javadoc. */
	public int getGlyphWidth() {
		return glyphWidth;
	}

	/** Already scaled for on-screen legibility - see this class's javadoc. */
	public int getGlyphHeight() {
		return glyphHeight;
	}

	/** The plain derived font, for components that draw normal Unicode text rather than byte-indexed glyphs (see this class's javadoc). */
	public Font getAwtFont() {
		return font;
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
	public void drawText(Graphics2D g2, String text, Color color, int x, int y) {
		for (int i = 0; i < text.length(); i++) {
			drawChar(g2, text.charAt(i), color, x + i * glyphWidth, y);
		}
	}

	private void drawChar(Graphics2D g2, char ch, Color color, int x, int y) {
		BufferedImage glyphImage = getGlyphImage(ch, color);
		Object oldHint = g2.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
		g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
		g2.drawImage(glyphImage, x, y, glyphWidth, glyphHeight, null);
		if (oldHint != null) {
			g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, oldHint);
		}
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
