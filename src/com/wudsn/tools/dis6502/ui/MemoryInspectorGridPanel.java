/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.ui;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.JPanel;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;

import com.wudsn.tools.dis6502.model.ComputerSystemType;
import com.wudsn.tools.dis6502.model.MemoryInspectorState;
import com.wudsn.tools.dis6502.model.MemoryInspectorState.EditPane;
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
 * bookkeeping. {@link #cellAtPoint} is that same mapping's edit-mode sibling,
 * additionally resolving which hex nibble or ASCII character a point falls in
 * ({@link EditPane}), needed to position the in-place edit cursor precisely.
 * <p>
 * {@link #getBytesPerLine} is responsive, not the fixed 16 it used to be:
 * ported from {@code Layout::Compute}'s own {@code
 * memoryInspectorNumberOfBytesPerLine} decision, which drops from 16 to 8
 * once the *whole* main window is narrower than a fixed 152-column
 * threshold (computed once per window resize by that hand-rolled manual
 * layout system, which positions every panel from the window's raw pixel
 * dimensions). This class has no equivalent whole-window computation to
 * hook into - {@code MainWindow} nests ordinary {@link
 * javax.swing.JSplitPane}s instead - so {@link #updateBytesPerLine} reacts
 * to this component's own enclosing {@link javax.swing.JScrollPane}
 * viewport's width instead (already reduced correctly for whatever room
 * the surrounding split panes end up giving this panel): wide enough to
 * fit all 16 bytes/line without horizontal scrolling, and it does; too
 * narrow, and it drops to 8. {@link MutableMemoryInspectorState#moveEditCursor}'s
 * Up/Down navigation needs this same value for its whole-line jump, so it
 * takes it as a parameter from {@link MemoryInspectorPanel} rather than
 * owning any bytes-per-line concept of its own - a UI-computed fact like
 * this has no business being model state.
 * <p>
 * The selection range and edit-mode cursor are read live from {@link
 * #setMemoryInspectorState}'s {@link MemoryInspectorState} - this
 * class does not own or mirror that state as its own fields, it just paints
 * whatever it currently says (matching {@code Char}/{@code KeyDown}'s edit
 * mode and its blinking-cursor {@code WM_TIMER}, ported in {@link #paintLine}
 * and {@link #advanceBlinkPhase}) - see {@link MemoryInspectorPanel} for the
 * keyboard/focus/timer wiring that mutates the state and then calls {@link
 * #refreshSelection}/{@link #refreshEditMode}/{@link #refreshEditCursor} to
 * ask this class to notice. {@link #segment}, by contrast, stays an explicit,
 * separately-pushed field via {@link #setSegment} rather than read from
 * {@code MemoryInspectorState.getSegment()} directly: {@code
 * MemoryInspectorPanel} sometimes needs this class to show nothing even
 * though a segment actually is selected (an SDX symbol-table header, for
 * instance - see {@code MemoryInspectorPanel#segmentChanged}'s {@code
 * hasData} check), a decision that belongs to that class, not this pure
 * painter.
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

	/** Matches dwMemoryInspectorColor[], indexed by MemoryType.ordinal(). */
	private static final Color[] TYPE_COLORS = { new Color(0, 0, 0), new Color(192, 192, 192), new Color(128, 128, 128),
			new Color(128, 0, 0), new Color(128, 0, 128), new Color(128, 128, 0), new Color(255, 127, 0),
			new Color(0, 127, 255), new Color(0, 128, 0), new Color(255, 128, 255), new Color(0, 0, 128),
			new Color(255, 0, 128), new Color(255, 0, 255) };

	private static final Color HIGHLIGHT_COLOR = Color.YELLOW;

	private Segment segment;
	private ComputerFont computerFont;
	private boolean displayAsScreenCode;

	private MemoryInspectorState memoryInspectorState;
	private int previousEditCursorOffset = -1;
	private int blinkPhase;

	private int bytesPerLine = 16;
	private final ComponentAdapter parentResizeListener = new ComponentAdapter() {
		@Override
		public void componentResized(ComponentEvent e) {
			updateBytesPerLine();
		}
	};

	public MemoryInspectorGridPanel() {
		setBackground(Color.WHITE);
		setComputerFont(ComputerFont.get(ComputerSystemType.ATARI800, false));
	}

	/**
	 * Tracks the enclosing {@link javax.swing.JScrollPane} viewport (this
	 * component's {@link #getParent()} once actually placed in one) so
	 * {@link #updateBytesPerLine} runs whenever it resizes - see this
	 * class's own javadoc for why that, not the whole window, is what this
	 * port reacts to.
	 */
	@Override
	public void addNotify() {
		super.addNotify();
		Container parent = getParent();
		if (parent != null) {
			parent.addComponentListener(parentResizeListener);
		}
		updateBytesPerLine();
	}

	@Override
	public void removeNotify() {
		Container parent = getParent();
		if (parent != null) {
			parent.removeComponentListener(parentResizeListener);
		}
		super.removeNotify();
	}

	public int getBytesPerLine() {
		return bytesPerLine;
	}

	/**
	 * Ported from {@code Layout::Compute}'s {@code
	 * memoryInspectorNumberOfBytesPerLine} decision - see this class's own
	 * javadoc for the full explanation of what this reacts to instead of
	 * the C++ source's whole-window column count. A no-op until {@link
	 * #computerFont} is known (glyph width isn't available yet to size the
	 * comparison), and whenever the result doesn't actually change from
	 * the current value.
	 */
	private void updateBytesPerLine() {
		if (computerFont == null) {
			return;
		}
		Container parent = getParent();
		int availableWidth = parent != null ? parent.getWidth() : getWidth();
		int neededFor16BytesPerLine = totalUnitsFor(16) * computerFont.getGlyphWidth();
		int newBytesPerLine = availableWidth >= neededFor16BytesPerLine ? 16 : 8;
		if (newBytesPerLine != bytesPerLine) {
			bytesPerLine = newBytesPerLine;
			previousEditCursorOffset = -1; // Stale cursor-line bookkeeping once the column count itself changes.
			revalidate();
			repaint();
		}
	}

	public void setComputerFont(ComputerFont computerFont) {
		this.computerFont = computerFont;
		updateBytesPerLine();
		revalidate();
		repaint();
	}

	public void setSegment(Segment segment) {
		this.segment = segment;
		this.previousEditCursorOffset = -1;
		revalidate();
		repaint();
	}

	/**
	 * Points this panel at the {@link MemoryInspectorState} to read
	 * the selection range and edit-mode cursor from at paint time - see this
	 * class's own javadoc for why {@link #segment} stays a separate, explicit
	 * field instead of also being read from here. Does not itself trigger a
	 * repaint: call {@link #refreshSelection}/{@link #refreshEditMode}/{@link
	 * #refreshEditCursor} once the state has actually changed.
	 */
	public void setMemoryInspectorState(MemoryInspectorState memoryInspectorState) {
		this.memoryInspectorState = memoryInspectorState;
	}

	public void setDisplayAsScreenCode(boolean displayAsScreenCode) {
		this.displayAsScreenCode = displayAsScreenCode;
		repaint();
	}

	/**
	 * Ported from the selection-highlight side of {@code PrintLine}/{@code
	 * Refresh}: call after {@link #setMemoryInspectorState}'s selection range
	 * changes (a new selection, or it being cleared). Repaints unconditionally
	 * (cheap: the highlight itself, not the whole grid's content, is what
	 * changed) and, only while there actually is a selection, scrolls its
	 * first byte into view - matching {@code MemoryInspector::Select}'s own
	 * scroll-to-selection behavior; clearing a selection never scrolls.
	 */
	public void refreshSelection() {
		repaint();
		if (memoryInspectorState != null && memoryInspectorState.hasSelection()) {
			scrollLineToVisible(memoryInspectorState.getBegin() / bytesPerLine);
		}
	}

	private void scrollLineToVisible(int line) {
		int cellH = computerFont.getGlyphHeight();
		scrollRectToVisible(new Rectangle(0, line * cellH, 1, cellH));
	}

	/**
	 * Ported from {@code MemoryInspector::SetEditMode}'s effect on the control:
	 * switches the cursor-highlight painting on/off (mutually exclusive with the
	 * plain selection highlight - see {@code !bEditMode}'s gate on
	 * {@code PrintLine}'s selection-highlight block) and resets the blink phase,
	 * matching {@code SetEditMode(TRUE, ...)}'s {@code SetTimerCount(0)}. Call
	 * after {@link #setMemoryInspectorState}'s {@code isEditMode()} changes.
	 */
	public void refreshEditMode() {
		this.blinkPhase = 0;
		repaint();
	}

	public boolean isEditMode() {
		return memoryInspectorState != null && memoryInspectorState.isEditMode();
	}

	/**
	 * Call after {@link #setMemoryInspectorState}'s edit cursor offset/pane
	 * changes: scrolls the new position into view and repaints just the old
	 * and new cursor lines - not the whole grid, unlike {@code
	 * MemoryInspectorControlImpl::Refresh}'s full-panel repaint on every
	 * change.
	 */
	public void refreshEditCursor() {
		int offset = getEditCursorOffset();
		if (previousEditCursorOffset >= 0 && previousEditCursorOffset != offset) {
			repaintCursorLine(previousEditCursorOffset);
		}
		if (offset >= 0) {
			scrollLineToVisible(offset / bytesPerLine);
			repaintCursorLine(offset);
		}
		previousEditCursorOffset = offset;
	}

	public int getEditCursorOffset() {
		return memoryInspectorState == null ? -1 : memoryInspectorState.getEditCursorOffset();
	}

	public EditPane getEditCursorPane() {
		return memoryInspectorState == null ? EditPane.HEX_HIGH : memoryInspectorState.getEditCursorPane();
	}

	/**
	 * Advances the blink phase (matching {@code wTimerCount = (wTimerCount+1)%4})
	 * and repaints only the cursor's line, ported from {@code
	 * MemoryInspectorControlImpl::Timer} - unlike that method, which calls
	 * {@code Refresh()} to repaint the whole control on every 250ms tick, this
	 * uses a targeted {@link #repaint(Rectangle)} since {@link #paintComponent}
	 * already supports clip-rect-limited repainting.
	 */
	public void advanceBlinkPhase() {
		blinkPhase = (blinkPhase + 1) % 4;
		int offset = getEditCursorOffset();
		if (offset >= 0) {
			repaintCursorLine(offset);
		}
	}

	private void repaintCursorLine(int offset) {
		if (computerFont == null) {
			return;
		}
		int cellH = computerFont.getGlyphHeight();
		int line = offset / bytesPerLine;
		repaint(new Rectangle(0, line * cellH, totalUnits() * computerFont.getGlyphWidth(), cellH));
	}

	/** The result of {@link #cellAtPoint}: a byte offset plus which part of its cell was hit. */
	public static final class CellHit {
		public final int offset;
		public final EditPane pane;

		CellHit(int offset, EditPane pane) {
			this.offset = offset;
			this.pane = pane;
		}
	}

	/**
	 * Maps a point to the exact hex nibble or ASCII character under it, ported
	 * from {@code MemoryInspectorControlImpl::LButtonDown}/{@code
	 * SetEndOfSelection}'s edit-mode pixel math - {@link #offsetAtPoint}'s
	 * sibling, used only for positioning the in-place edit cursor (double-click-
	 * to-edit-at-position). Unlike the C++ source's {@code WORD} (unsigned
	 * 16-bit) arithmetic, which relies on wraparound-then-clamp for an
	 * out-of-range low x, this clamps both ends explicitly, since Java's signed
	 * int arithmetic would otherwise produce a small negative row instead of a
	 * huge positive one. Returns {@code null} if there is no segment displayed.
	 */
	public CellHit cellAtPoint(int x, int y) {
		if (segment == null || computerFont == null) {
			return null;
		}
		int lines = lineCount();
		if (lines == 0) {
			return null;
		}
		int cellW = computerFont.getGlyphWidth();
		int cellH = computerFont.getGlyphHeight();
		int line = Math.max(0, Math.min(lines - 1, y / cellH));

		int startOfAsciiPaneInPixel = (bytesPerLine * 3 + 5) * cellW;
		int row;
		EditPane pane;
		if (x > startOfAsciiPaneInPixel) {
			row = Math.max(0, x - 1 - startOfAsciiPaneInPixel) / cellW;
			pane = EditPane.ASCII;
		} else {
			int relative = Math.max(0, x - 1 - 4 * cellW - cellW / 2);
			row = relative / (3 * cellW);
			pane = (relative % (3 * cellW)) > (3 * cellW) / 2 ? EditPane.HEX_LOW : EditPane.HEX_HIGH;
		}
		row = Math.max(0, Math.min(bytesPerLine - 1, row));

		return new CellHit(line * bytesPerLine + row, pane);
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
		} else if (column >= 5 + bytesPerLine * 3) {
			row = column - (5 + bytesPerLine * 3);
		} else {
			row = (column - 5) / 3;
		}
		row = Math.max(0, Math.min(bytesPerLine - 1, row));

		return line * bytesPerLine + row;
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
		return (size + bytesPerLine - 1) / bytesPerLine;
	}

	/**
	 * Total width in glyph-cell units: address (4 digits + '|') + each byte's "XX "
	 * + the ASCII column.
	 */
	private int totalUnits() {
		return totalUnitsFor(bytesPerLine);
	}

	private static int totalUnitsFor(int bytesPerLine) {
		return 5 + bytesPerLine * 4;
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
		int lineStart = line * bytesPerLine;
		int lineEnd = Math.min(lineStart + bytesPerLine, size);
		int rowsInLine = lineEnd - lineStart;

		// Address, e.g. "0600|".
		String address = String.format("%04X|", segment.wBegin + lineStart);
		computerFont.drawText(g2, address, Color.BLACK, 0, y);

		boolean editMode = isEditMode();
		int editCursorOffset = getEditCursorOffset();
		boolean hasSelection = !editMode && memoryInspectorState != null && memoryInspectorState.hasSelection();
		int selectionBegin = hasSelection ? memoryInspectorState.getBegin() : -1;
		int selectionEnd = hasSelection ? memoryInspectorState.getEnd() : -1;

		MemoryType oldType = null;
		for (int row = 0; row < bytesPerLine; row++) {
			int hexX = (5 + row * 3) * cellW;
			int charX = (5 + bytesPerLine * 3 + row) * cellW;

			if (row >= rowsInLine) {
				drawBlank(g2, hexX, y, cellW * 3, cellH);
				drawBlank(g2, charX, y, cellW, cellH);
				continue;
			}

			int offset = lineStart + row;
			MemoryType type = computeDisplayType(offset, oldType);
			oldType = type;
			Color color = TYPE_COLORS[type.ordinal()];

			boolean selected = hasSelection && offset >= selectionBegin && offset <= selectionEnd;
			if (selected) {
				g2.setColor(HIGHLIGHT_COLOR);
				g2.fillRect(hexX, y, cellW * 3, cellH);
				g2.fillRect(charX, y, cellW, cellH);
			}

			int value = segment.getData(offset) & 0xFF;
			String hex = String.format("%02X ", value);
			int displayValue = displayAsScreenCode ? toInternalCode(value) : value;

			if (editMode && offset == editCursorOffset) {
				paintCursorHexCell(g2, hex, color, hexX, cellW, cellH, y);
				paintCursorAsciiCell(g2, displayValue, charX, cellW, cellH, y);
			} else {
				computerFont.drawText(g2, hex, color, hexX, y);
				computerFont.drawGlyph(g2, displayValue, color, charX, y);
			}
		}

		// Vertical bar separating the hex and ASCII columns.
		computerFont.drawText(g2, "|", Color.BLACK, (5 + bytesPerLine * 3 - 1) * cellW, y);
	}

	/**
	 * Paints the hex pane's two-nibble cell for the byte the edit cursor is on,
	 * ported from {@code PrintLine}'s edit-cursor block (lines ~581-619): the
	 * nibble actually being edited gets a yellow background and blinks (hidden
	 * on {@code blinkPhase == 0}); if the ASCII pane is active instead, both
	 * nibbles get a steady (non-blinking) yellow background; otherwise the
	 * sibling nibble is left completely unhighlighted.
	 */
	private void paintCursorHexCell(Graphics2D g2, String hex, Color color, int hexX, int cellW, int cellH, int y) {
		EditPane editCursorPane = getEditCursorPane();
		boolean asciiActive = editCursorPane == EditPane.ASCII;
		paintCursorSubCell(g2, hex.charAt(0), color, hexX, y, cellW, cellH,
				asciiActive || editCursorPane == EditPane.HEX_HIGH, editCursorPane == EditPane.HEX_HIGH);
		paintCursorSubCell(g2, hex.charAt(1), color, hexX + cellW, y, cellW, cellH,
				asciiActive || editCursorPane == EditPane.HEX_LOW, editCursorPane == EditPane.HEX_LOW);
		computerFont.drawText(g2, String.valueOf(hex.charAt(2)), color, hexX + cellW * 2, y);
	}

	private void paintCursorSubCell(Graphics2D g2, char ch, Color color, int x, int y, int cellW, int cellH,
			boolean highlighted, boolean blinking) {
		if (!highlighted) {
			computerFont.drawText(g2, String.valueOf(ch), color, x, y);
			return;
		}
		g2.setColor(HIGHLIGHT_COLOR);
		g2.fillRect(x, y, cellW, cellH);
		if (!(blinking && blinkPhase == 0)) {
			computerFont.drawText(g2, String.valueOf(ch), Color.BLACK, x, y);
		}
	}

	/**
	 * Paints the ASCII pane's one-character cell for the byte the edit cursor is
	 * on, ported from {@code PrintLine}'s edit-cursor block (lines ~640-652):
	 * always a yellow background while the cursor is on this byte's row, but the
	 * glyph itself only blinks (hidden on {@code blinkPhase == 0}) when the
	 * ASCII pane is the one actually being edited - otherwise it is shown
	 * steadily.
	 */
	private void paintCursorAsciiCell(Graphics2D g2, int displayValue, int charX, int cellW, int cellH, int y) {
		g2.setColor(HIGHLIGHT_COLOR);
		g2.fillRect(charX, y, cellW, cellH);
		boolean blinking = getEditCursorPane() == EditPane.ASCII;
		if (!(blinking && blinkPhase == 0)) {
			computerFont.drawGlyph(g2, displayValue, Color.BLACK, charX, y);
		}
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
