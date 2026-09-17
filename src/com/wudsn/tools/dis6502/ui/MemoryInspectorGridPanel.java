/*
 * Copyright (C) 2025 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JPanel;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;

import com.wudsn.tools.dis6502.model.MemoryType;
import com.wudsn.tools.dis6502.model.Segment;

/**
 * A read-only, custom-painted hex/ASCII dump of a segment's bytes, drawn
 * with the real per-computer-system bitmap glyphs from {@link
 * ComputerFont} instead of a Java system font - so every byte value (not
 * just the ones that happen to coincide with printable ASCII) renders as
 * its actual Atari ATASCII/C64 PETSCII character, matching what dis6502.exe
 * itself shows.
 * <p>
 * Ported from ui/MemoryInspectorControlImpl.cpp's {@code PrintLine}
 * (address, hex bytes color-coded by {@link MemoryType}, and the ASCII/
 * ATASCII column) - not the rest of that class: this renders every line of
 * the segment as one plain (if tall) component inside a {@link
 * javax.swing.JScrollPane}, relying on Swing's own clip-rect-based repaint
 * for virtualization instead of {@code PrintLine}/{@code ScrollUp}/{@code
 * ScrollDown}'s manual line-range/{@code BitBlt} scrolling; and mouse-drag
 * selection ({@code LButtonDown}/{@code MouseMove}/{@code SetEndOfSelection})
 * and in-place hex/ASCII editing ({@code Char}/{@code KeyDown}'s edit mode,
 * with its blinking-cursor {@code WM_TIMER}) are not ported - {@link
 * MemoryInspectorPanel} predates this class and never had either (selection
 * is programmatic only, via {@link #highlightRange}; editing goes through
 * {@link AssembleDialog} instead), and this rewrite only replaces how the
 * existing byte grid is drawn, not how it is interacted with.
 * <p>
 * {@code PrintLine}'s hex-byte/ASCII-column color, including its LOBYTE/
 * HIBYTE-adjacency-to-CODE-color rule, is ported verbatim (see {@link
 * #computeDisplayType}) since it directly affects what a real, already
 * pixel-accurate rendering should look like; its {@code cOldType} reset
 * once per displayed line (not once per segment) is preserved by resetting
 * {@code oldType} at the start of each line's row loop here too, the same
 * scope {@code PrintLine} has (it is called once per line).
 * <p>
 * Glyphs are scaled up {@link #ZOOM}x for on-screen legibility, the same
 * "native pixels are too small for a modern display" adjustment {@link
 * SpritePanel} already makes.
 *
 * @author Peter Dell
 */
public final class MemoryInspectorGridPanel extends JPanel implements Scrollable {

	private static final long serialVersionUID = 1L;

	public static final int BYTES_PER_LINE = 16;
	private static final int ZOOM = 2;

	/** Matches dwMemoryInspectorColor[], indexed by MemoryType.ordinal(). */
	private static final Color[] TYPE_COLORS = { new Color(0, 0, 0), new Color(192, 192, 192), new Color(128, 128, 128),
			new Color(128, 0, 0), new Color(128, 0, 128), new Color(128, 128, 0), new Color(255, 127, 0), new Color(0, 127, 255),
			new Color(0, 128, 0), new Color(255, 128, 255), new Color(0, 0, 128), new Color(255, 0, 128), new Color(255, 0, 255) };

	private static final Color HIGHLIGHT_COLOR = Color.YELLOW;

	private Segment segment;
	private ComputerFont computerFont;
	private boolean displayAsScreenCode;
	private int highlightBegin = -1;
	private int highlightEnd = -1;

	private final Map<ComputerFont, Map<Long, BufferedImage>> tintedGlyphCache = new HashMap<>();

	public MemoryInspectorGridPanel() {
		setBackground(Color.WHITE);
		setComputerFont(ComputerFont.get(com.wudsn.tools.dis6502.model.ComputerSystemType.ATARI800, false));
	}

	public void setComputerFont(ComputerFont computerFont) {
		this.computerFont = computerFont;
		revalidate();
		repaint();
	}

	public void setSegment(Segment segment) {
		this.segment = segment;
		this.highlightBegin = -1;
		this.highlightEnd = -1;
		revalidate();
		repaint();
	}

	public void setDisplayAsScreenCode(boolean displayAsScreenCode) {
		this.displayAsScreenCode = displayAsScreenCode;
		repaint();
	}

	public void highlightRange(int begin, int end) {
		this.highlightBegin = begin;
		this.highlightEnd = end;
		repaint();
		scrollLineToVisible(begin / BYTES_PER_LINE);
	}

	public void clearHighlight() {
		this.highlightBegin = -1;
		this.highlightEnd = -1;
		repaint();
	}

	private void scrollLineToVisible(int line) {
		int cellH = computerFont.getGlyphHeight() * ZOOM;
		Rectangle rectangle = new Rectangle(0, line * cellH, 1, cellH);
		scrollRectToVisible(rectangle);
	}

	/** Ported from PrintLine's cType computation (the LOBYTE/HIBYTE-adjacency-to-CODE-color rule). */
	private MemoryType computeDisplayType(int offset, MemoryType oldType) {
		MemoryType type = segment.getType(offset);
		if (offset > 0) {
			MemoryType prevType = segment.getType(offset - 1);
			if (oldType != MemoryType.LOBYTE && oldType != MemoryType.HIBYTE
					&& (prevType == MemoryType.LOBYTE || prevType == MemoryType.HIBYTE)) {
				type = prevType;
			} else if (type == MemoryType.LOBYTE || type == MemoryType.HIBYTE) {
				type = MemoryType.CODE;
			}
		} else if (type == MemoryType.LOBYTE || type == MemoryType.HIBYTE) {
			type = MemoryType.CODE;
		}
		return type;
	}

	/** Ported from MemoryInspectorPanel's (formerly MemoryInspectorControlImpl.cpp PrintLine's) bInternal transform. */
	private static int toInternalCode(int value) {
		if (value < 64) {
			return value + 32;
		} else if (value < 96) {
			return value - 64;
		} else if (value >= 128 && value < 128 + 64) {
			return value + 32;
		} else if (value >= 128 + 64 && value < 128 + 96) {
			return value - 64;
		}
		return value;
	}

	private int lineCount() {
		if (segment == null) {
			return 0;
		}
		int size = segment.getSize();
		return (size + BYTES_PER_LINE - 1) / BYTES_PER_LINE;
	}

	/** Total width in glyph-cell units: address (4 digits + '|') + each byte's "XX " + the ASCII column. */
	private static int totalUnits() {
		return 5 + BYTES_PER_LINE * 4;
	}

	@Override
	public Dimension getPreferredSize() {
		if (computerFont == null) {
			return super.getPreferredSize();
		}
		int cellW = computerFont.getGlyphWidth() * ZOOM;
		int cellH = computerFont.getGlyphHeight() * ZOOM;
		int lines = Math.max(lineCount(), 1);
		return new Dimension(totalUnits() * cellW, lines * cellH);
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		if (segment == null) {
			g.setColor(Color.GRAY);
			g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
			g.drawString("No segment selected.", 8, 20);
			return;
		}

		Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

		int cellW = computerFont.getGlyphWidth() * ZOOM;
		int cellH = computerFont.getGlyphHeight() * ZOOM;
		int size = segment.getSize();
		int lines = lineCount();

		Rectangle clip = g2.getClipBounds();
		int firstLine = clip == null ? 0 : Math.max(0, clip.y / cellH);
		int lastLine = clip == null ? lines - 1 : Math.min(lines - 1, (clip.y + clip.height) / cellH);

		for (int line = firstLine; line <= lastLine; line++) {
			paintLine(g2, line, size, cellW, cellH);
		}
	}

	private void paintLine(Graphics2D g2, int line, int size, int cellW, int cellH) {
		int y = line * cellH;
		int lineStart = line * BYTES_PER_LINE;
		int lineEnd = Math.min(lineStart + BYTES_PER_LINE, size);
		int rowsInLine = lineEnd - lineStart;

		// Address, e.g. "0600|".
		String address = String.format("%04X|", segment.wBegin + lineStart);
		for (int i = 0; i < address.length(); i++) {
			drawGlyph(g2, address.charAt(i), Color.BLACK, i * cellW, y, cellW, cellH);
		}

		MemoryType oldType = null;
		for (int row = 0; row < BYTES_PER_LINE; row++) {
			int hexX = (5 + row * 3) * cellW;
			int charX = (5 + BYTES_PER_LINE * 3 + row) * cellW;

			if (row >= rowsInLine) {
				drawBlank(g2, hexX, y, cellW * 3, cellH);
				drawBlank(g2, charX, y, cellW, cellH);
				continue;
			}

			int offset = lineStart + row;
			MemoryType type = computeDisplayType(offset, oldType);
			oldType = type;
			Color color = TYPE_COLORS[type.ordinal()];

			boolean selected = highlightBegin != -1 && offset >= Math.min(highlightBegin, highlightEnd)
					&& offset <= Math.max(highlightBegin, highlightEnd);
			if (selected) {
				g2.setColor(HIGHLIGHT_COLOR);
				g2.fillRect(hexX, y, cellW * 3, cellH);
				g2.fillRect(charX, y, cellW, cellH);
			}

			int value = segment.getData(offset) & 0xFF;
			String hex = String.format("%02X ", value);
			for (int i = 0; i < hex.length(); i++) {
				drawGlyph(g2, hex.charAt(i), color, hexX + i * cellW, y, cellW, cellH);
			}

			int displayValue = displayAsScreenCode ? toInternalCode(value) : value;
			drawGlyph(g2, displayValue, color, charX, y, cellW, cellH);
		}

		// Vertical bar separating the hex and ASCII columns.
		drawGlyph(g2, '|', Color.BLACK, (5 + BYTES_PER_LINE * 3 - 1) * cellW, y, cellW, cellH);
	}

	private void drawBlank(Graphics2D g2, int x, int y, int width, int height) {
		g2.setColor(getBackground());
		g2.fillRect(x, y, width, height);
	}

	private void drawGlyph(Graphics2D g2, int value, Color color, int x, int y, int cellW, int cellH) {
		BufferedImage tinted = getTintedGlyph(value, color);
		g2.drawImage(tinted, x, y, cellW, cellH, null);
	}

	private BufferedImage getTintedGlyph(int value, Color color) {
		Map<Long, BufferedImage> byKey = tintedGlyphCache.computeIfAbsent(computerFont, f -> new HashMap<>());
		long key = ((long) (value & 0xFF) << 32) | (color.getRGB() & 0xFFFFFFFFL);
		return byKey.computeIfAbsent(key, k -> tint(computerFont.getGlyph(value), color));
	}

	/** Recolors a black-on-white glyph bitmap: black pixels become {@code color}, white pixels become transparent. */
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

	@Override
	public Dimension getPreferredScrollableViewportSize() {
		return getPreferredSize();
	}

	@Override
	public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
		return computerFont == null ? 16 : computerFont.getGlyphHeight() * ZOOM;
	}

	@Override
	public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
		return orientation == SwingConstants.HORIZONTAL ? visibleRect.width : visibleRect.height;
	}

	@Override
	public boolean getScrollableTracksViewportWidth() {
		return false;
	}

	@Override
	public boolean getScrollableTracksViewportHeight() {
		return false;
	}
}
