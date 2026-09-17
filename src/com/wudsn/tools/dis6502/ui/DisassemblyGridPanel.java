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
import java.util.Collections;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;

import com.wudsn.tools.dis6502.model.ComputerSystemType;

/**
 * A read-only, custom-painted list of disassembly listing lines, drawn with
 * the real per-computer-system font from {@link ComputerFont} - the same
 * reason {@link MemoryInspectorGridPanel} needs it (see that class's/{@link
 * ComputerFont}'s javadoc): the C++ source draws the disassembly listing
 * with the very same {@code ComputerFont}-derived font as the memory
 * inspector ({@code DisassemblyWindow.cpp}'s {@code
 * disassemblyControl->SetFont(partLayout->GetLayout()->GetFont())} uses the
 * same shared {@code Layout} font {@code Main::SetLayoutFont}/{@code
 * WorkspaceFont::GetResizedFont} sets for the whole window), not a plain
 * system font - a plain Java font would be visually wrong for this listing
 * too, not just cosmetically inconsistent, since it can contain literal
 * ATASCII/PETSCII text and graphics characters from {@code STRING}/{@code
 * SBYTE} data.
 * <p>
 * Ported from ui/DisassemblyControlImpl.cpp's paint routine, restricted to
 * plain text layout (one {@link ComputerFont#drawText} call per visible
 * line, all in black - unlike the memory inspector's grid, a disassembly
 * line has no per-character {@code MemoryType} coloring): virtualized
 * scrolling relies on Swing's clip-rect-based repaint the same way {@link
 * MemoryInspectorGridPanel} does, and inline editing/the full popup menu
 * are not ported - {@link DisassemblyPanel} predates this class and never
 * had them either; {@link #highlightLine}/{@link #scrollLineToVisible}
 * carry over from it, used for both search-result/cross-reference
 * navigation and (new here) click/drag line selection, and {@link
 * #lineIndexAtY} is the pixel-to-line half of that selection mapping, used
 * by {@link DisassemblyPanel}'s own mouse handling. Cell dimensions come straight
 * from {@link ComputerFont#getGlyphWidth}/{@link
 * ComputerFont#getGlyphHeight} - already scaled for on-screen legibility,
 * see that class's javadoc - rather than this class applying its own zoom
 * factor.
 *
 * @author Peter Dell
 */
public final class DisassemblyGridPanel extends JPanel implements Scrollable {

	private static final long serialVersionUID = 1L;

	private List<String> lines = Collections.emptyList();
	private ComputerFont computerFont;
	private int highlightedLine = -1;
	private int maxLineLength;

	public DisassemblyGridPanel() {
		setBackground(Color.WHITE);
		setComputerFont(ComputerFont.get(ComputerSystemType.ATARI800, false));
	}

	public void setComputerFont(ComputerFont computerFont) {
		this.computerFont = computerFont;
		revalidate();
		repaint();
	}

	public void setLines(List<String> lines) {
		this.lines = lines;
		this.highlightedLine = -1;
		this.maxLineLength = 1;
		for (String line : lines) {
			maxLineLength = Math.max(maxLineLength, line.length());
		}
		revalidate();
		repaint();
	}

	public void highlightLine(int index) {
		this.highlightedLine = index;
		repaint();
		scrollLineToVisible(index);
	}

	public void clearHighlight() {
		this.highlightedLine = -1;
		repaint();
	}

	public void scrollLineToVisible(int index) {
		int cellH = computerFont.getGlyphHeight();
		scrollRectToVisible(new Rectangle(0, index * cellH, 1, cellH));
	}

	/** The line index a given Y pixel coordinate (e.g. a mouse event's) falls in - for right-click hit-testing. */
	public int lineIndexAtY(int y) {
		return y / computerFont.getGlyphHeight();
	}

	@Override
	public Dimension getPreferredSize() {
		if (computerFont == null) {
			return super.getPreferredSize();
		}
		int lineCount = Math.max(lines.size(), 1);
		return new Dimension(maxLineLength * computerFont.getGlyphWidth(), lineCount * computerFont.getGlyphHeight());
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		if (lines.isEmpty()) {
			g.setColor(Color.GRAY);
			g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
			g.drawString("No disassembly yet. Open a workspace or add a file to disassemble it.", 8, 20);
			return;
		}

		Graphics2D g2 = (Graphics2D) g;

		int cellH = computerFont.getGlyphHeight();

		Rectangle clip = g2.getClipBounds();
		int firstLine = clip == null ? 0 : Math.max(0, clip.y / cellH);
		int lastLine = clip == null ? lines.size() - 1 : Math.min(lines.size() - 1, (clip.y + clip.height) / cellH);

		for (int index = firstLine; index <= lastLine; index++) {
			int y = index * cellH;
			if (index == highlightedLine) {
				g2.setColor(Color.YELLOW);
				g2.fillRect(0, y, getWidth(), cellH);
			}
			computerFont.drawText(g2, lines.get(index), Color.BLACK, 0, y);
		}
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
