/*
 * Copyright (C) 2025 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;

import javax.swing.JPanel;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;

import com.wudsn.tools.dis6502.model.ComputerSystemType;
import com.wudsn.tools.dis6502.model.MemoryType;
import com.wudsn.tools.dis6502.model.Segment;

/**
 * A read-only, custom-painted hex/ASCII dump of a segment's bytes, drawn with
 * the real per-computer-system font from {@link ComputerFont} instead of a Java
 * system font - so every byte value (not just the ones that happen to coincide
 * with printable ASCII) renders as its actual Atari ATASCII/C64 PETSCII
 * character, matching what dis6502.exe itself shows.
 * <p>
 * Ported from ui/MemoryInspectorControlImpl.cpp's {@code PrintLine} (address,
 * hex bytes color-coded by {@link MemoryType}, and the ASCII/ ATASCII column) -
 * not the rest of that class: this renders every line of the segment as one
 * plain (if tall) component inside a {@link javax.swing.JScrollPane}, relying
 * on Swing's own clip-rect-based repaint for virtualization instead of
 * {@code PrintLine}/{@code ScrollUp}/{@code
 * ScrollDown}'s manual line-range/{@code BitBlt} scrolling.
 * {@link #offsetAtPoint} ports {@code LButtonDown}/{@code SetEndOfSelection}'s
 * pixel-to-byte mapping (the non-edit-mode case only), letting
 * {@link MemoryInspectorPanel} implement click/drag selection the idiomatic
 * Swing way, with
 * {@link java.awt.event.MouseListener}/{@link java.awt.event.MouseMotionListener}
 * instead of mouse capture and manual {@code SetCapture}/{@code ReleaseCapture}
 * bookkeeping; in-place hex/ASCII editing ({@code Char}/{@code KeyDown}'s edit
 * mode, with its blinking- cursor {@code WM_TIMER}) is not ported -
 * {@link MemoryInspectorPanel} predates this class and never had it (editing
 * goes through {@link AssembleDialog} instead), and this rewrite only replaces
 * how the existing byte grid is drawn and how its selection is set, not how
 * bytes are edited.
 * <p>
 * {@code PrintLine}'s hex-byte/ASCII-column color, including its LOBYTE/
 * HIBYTE-adjacency-to-CODE-color rule, is ported verbatim (see
 * {@link #computeDisplayType}) since it directly affects what a real, already
 * pixel-accurate rendering should look like; its {@code cOldType} reset once
 * per displayed line (not once per segment) is preserved by resetting
 * {@code oldType} at the start of each line's row loop here too, the same scope
 * {@code PrintLine} has (it is called once per line).
 * <p>
 * Cell dimensions come straight from {@link ComputerFont#getGlyphWidth}/
 * {@link ComputerFont#getGlyphHeight} - already scaled for on-screen
 * legibility, see that class's javadoc - rather than this class applying its
 * own zoom factor.
 *
 * @author Peter Dell
 */
public final class MemoryInspectorGridPanel extends JPanel implements Scrollable {

	private static final long serialVersionUID = 1L;

	public static final int BYTES_PER_LINE = 16;

	/** Matches dwMemoryInspectorColor[], indexed by MemoryType.ordinal(). */
	private static final Color[] TYPE_COLORS = { new Color(0, 0, 0), new Color(192, 192, 192), new Color(128, 128, 128),
			new Color(128, 0, 0), new Color(128, 0, 128), new Color(128, 128, 0), new Color(255, 127, 0),
			new Color(0, 127, 255), new Color(0, 128, 0), new Color(255, 128, 255), new Color(0, 0, 128),
			new Color(255, 0, 128), new Color(255, 0, 255) };

	private static final Color HIGHLIGHT_COLOR = Color.YELLOW;

	private Segment segment;
	private ComputerFont computerFont;
	private boolean displayAsScreenCode;
	private int highlightBegin = -1;
	private int highlightEnd = -1;

	public MemoryInspectorGridPanel() {
		setBackground(Color.WHITE);
		setComputerFont(ComputerFont.get(ComputerSystemType.ATARI800, false));
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
		int cellH = computerFont.getGlyphHeight();
		scrollRectToVisible(new Rectangle(0, line * cellH, 1, cellH));
	}

	/**
	 * Maps a point within this grid to the byte offset under it, ported from
	 * {@code MemoryInspectorControlImpl::LButtonDown}/{@code
	 * SetEndOfSelection}'s pixel-to-line/row mapping (the non-edit-mode case only -
	 * this port has no in-place hex editing) - clicking left of the hex pane (the
	 * address gutter) maps to row 0, matching the C++ source, and a point past the
	 * last line/row clamps to the nearest valid one rather than returning no match,
	 * so a drag that leaves the grid still extends the selection sensibly. Returns
	 * -1 if there is no segment displayed or it has no bytes.
	 */
	public int offsetAtPoint(int x, int y) {
		if (segment == null || computerFont == null) {
			return -1;
		}
		int lines = lineCount();
		if (lines == 0) {
			return -1;
		}
		int cellW = computerFont.getGlyphWidth();
		int cellH = computerFont.getGlyphHeight();
		int line = Math.max(0, Math.min(lines - 1, y / cellH));
		int column = Math.max(0, x / cellW);

		int row;
		if (column < 5) {
			row = 0;
		} else if (column >= 5 + BYTES_PER_LINE * 3) {
			row = column - (5 + BYTES_PER_LINE * 3);
		} else {
			row = (column - 5) / 3;
		}
		row = Math.max(0, Math.min(BYTES_PER_LINE - 1, row));

		return line * BYTES_PER_LINE + row;
	}

	/**
	 * Ported from PrintLine's cType computation (the
	 * LOBYTE/HIBYTE-adjacency-to-CODE-color rule).
	 */
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

	/**
	 * Ported from MemoryInspectorPanel's (formerly MemoryInspectorControlImpl.cpp
	 * PrintLine's) bInternal transform.
	 */
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

	/**
	 * Total width in glyph-cell units: address (4 digits + '|') + each byte's "XX "
	 * + the ASCII column.
	 */
	private static int totalUnits() {
		return 5 + BYTES_PER_LINE * 4;
	}

	@Override
	public Dimension getPreferredSize() {
		if (computerFont == null) {
			return super.getPreferredSize();
		}
		int lines = Math.max(lineCount(), 1);
		return new Dimension(totalUnits() * computerFont.getGlyphWidth(), lines * computerFont.getGlyphHeight());
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		if (segment == null) {
			return;
		}

		Graphics2D g2 = (Graphics2D) g;

		int cellW = computerFont.getGlyphWidth();
		int cellH = computerFont.getGlyphHeight();
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
		computerFont.drawText(g2, address, Color.BLACK, 0, y);

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
			computerFont.drawText(g2, hex, color, hexX, y);

			int displayValue = displayAsScreenCode ? toInternalCode(value) : value;
			computerFont.drawGlyph(g2, displayValue, color, charX, y);
		}

		// Vertical bar separating the hex and ASCII columns.
		computerFont.drawText(g2, "|", Color.BLACK, (5 + BYTES_PER_LINE * 3 - 1) * cellW, y);
	}

	private void drawBlank(Graphics2D g2, int x, int y, int width, int height) {
		g2.setColor(getBackground());
		g2.fillRect(x, y, width, height);
	}

	@Override
	public Dimension getPreferredScrollableViewportSize() {
		return getPreferredSize();
	}

	@Override
	public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
		return computerFont == null ? 16 : computerFont.getGlyphHeight();
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
