/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
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

import com.wudsn.tools.dis6502.model.DisassemblyLine;
import com.wudsn.tools.dis6502.model.DisassemblySectionType;
import com.wudsn.tools.dis6502.model.system.ComputerSystemType;

/**
 * A read-only, custom-painted list of disassembly listing lines, drawn with
 * the real per-computer-system font from {@link ComputerFont} - the same
 * reason {@link HexGridPanel} needs it (see that class's/{@link
 * ComputerFont}'s javadoc): a plain Java font would be visually wrong for
 * this listing, not just cosmetically inconsistent, since it can contain
 * literal ATASCII/PETSCII text and graphics characters from {@code
 * STRING}/{@code SBYTE} data.
 * <p>
 * Virtualized scrolling relies on Swing's clip-rect-based repaint the same
 * way {@link HexGridPanel} does. The popup menu is {@link
 * DisassemblyPanel}'s, not this class's; {@link #highlightLine}/{@link
 * #scrollLineToVisible} are used for both search-result/cross-reference
 * navigation and click/drag line selection, and {@link #lineIndexAtY} is
 * the pixel-to-line half of that selection mapping, used by {@link
 * DisassemblyPanel}'s own mouse handling. Cell dimensions come straight
 * from {@link ComputerFont#getGlyphWidth}/{@link
 * ComputerFont#getGlyphHeight} - already scaled for on-screen legibility,
 * see that class's javadoc - rather than this class applying its own zoom
 * factor.
 * <p>
 * {@link #paintLineInColor} does per-token syntax coloring
 * (mnemonic/number/string/comment/plain) - see that method's own javadoc
 * for the token classification. {@link #setLineNumbersActive} controls an
 * optional line-number column - see that method's own javadoc for why this
 * class reads {@link com.wudsn.tools.dis6502.model.Profile#useLineNumbers}
 * instead of storing it directly (that field is the real settings source:
 * a Profile dialog checkbox, not a menu item - {@code Dis6502} pushes it
 * in on every disassembly refresh).
 *
 * @author Peter Dell
 */
public final class DisassemblyGridPanel extends JPanel implements Scrollable {

	private static final long serialVersionUID = 1L;

	// "NNNN " - 4-digit zero-padded line number plus a space (more digits print as-is past 9999).
	private static final int LINE_NUMBER_PREFIX_LENGTH = 5;

	private List<DisassemblyLine> lines = Collections.emptyList();
	private ComputerFont computerFont;
	private int highlightedLine = -1;
	private int maxLineLength;
	private boolean lineNumbersActive;

	public DisassemblyGridPanel() {
		setBackground(Color.WHITE);
		setComputerFont(ComputerFont.get(ComputerSystemType.ATARI800, false));
	}

	public void setComputerFont(ComputerFont computerFont) {
		this.computerFont = computerFont;
		revalidate();
		repaint();
	}

	/**
	 * {@code Dis6502} calls this alongside every {@link
	 * DisassemblyPanel#refresh}, pushing in the current profile's {@code
	 * useLineNumbers} setting, rather than this panel reaching for a
	 * {@code Workspace}/{@code Profile} reference itself.
	 */
	public void setLineNumbersActive(boolean lineNumbersActive) {
		this.lineNumbersActive = lineNumbersActive;
		revalidate();
		repaint();
	}

	public void setLines(List<DisassemblyLine> lines) {
		this.lines = lines;
		this.highlightedLine = -1;
		this.maxLineLength = 1;
		for (DisassemblyLine line : lines) {
			maxLineLength = Math.max(maxLineLength, line.getLine().length());
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
		int lineLength = maxLineLength + (lineNumbersActive ? LINE_NUMBER_PREFIX_LENGTH : 0);
		return new Dimension(lineLength * computerFont.getGlyphWidth(), lineCount * computerFont.getGlyphHeight());
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
			paintLineInColor(g2, lines.get(index), 0, y);
		}
	}

	// Colors for the different parts of a disassembly line.
	private static final Color COLOR_NORMAL = Color.BLACK;
	private static final Color COLOR_COMMENT = new Color(0, 128, 0);
	private static final Color COLOR_NUMBER = new Color(128, 0, 0);
	private static final Color COLOR_STRING = new Color(128, 0, 128);
	private static final Color COLOR_INSTRUCTION = new Color(0, 0, 128);
	private static final Color COLOR_UNREFERENCED = new Color(192, 192, 192);

	/**
	 * A hand-written character scanner that classifies {@code
	 * disassemblyLine}'s text into runs - a leading label (or, if column 0
	 * isn't a label start character, the whole rest of the line is a
	 * comment), the instruction mnemonic (or, for an {@code =}/{@code *}
	 * equate/org line, plain text), an optional quoted string or {@code
	 * #}-immediate operand, any {@code $}-prefixed hex numbers
	 * (comma-separated), and a trailing {@code ;} comment - and draws each
	 * run in its own color via {@link #flushPartOfLine}. {@code referenced}
	 * (from {@link #isReferenced}) overrides every run's own color with a
	 * flat grey - used only for system/user equates sections, where an
	 * unreferenced equate is greyed out rather than removed.
	 * <p>
	 * When {@link #lineNumbersActive}, a leading {@code "NNNN "} run
	 * (4-digit zero-padded line number plus a space) is drawn first, in the
	 * same color the rest of this method would use for plain text -
	 * formatted directly from {@link DisassemblyLine#getLineNumber()}
	 * rather than prepending it to {@code text} and re-parsing the digits
	 * back out, since the real line number is already available as an
	 * {@code int} here.
	 * <p>
	 * The yellow selected-line background fill is not this method's
	 * responsibility - this panel already paints that separately via
	 * {@link #highlightedLine} before calling this method.
	 */
	private void paintLineInColor(Graphics2D g2, DisassemblyLine disassemblyLine, int xStart, int y) {
		String text = disassemblyLine.getLine();
		boolean referenced = isReferenced(disassemblyLine);
		int[] index = { 0 };
		int x = xStart;
		StringBuilder buf = new StringBuilder();

		if (lineNumbersActive) {
			String lineNumberPrefix = String.format("%04d ", disassemblyLine.getLineNumber());
			x = flushPartOfLine(g2, x, y, referenced ? COLOR_NORMAL : COLOR_UNREFERENCED, lineNumberPrefix);
		}

		char c = DisassemblyPanel.charAt(text, index);

		// Label on column 0 should begin with a letter, an @ or an _.
		// Otherwise the rest of the line is a comment.
		if (DisassemblyPanel.isLabelStartChar(c)) {
			while (c != '\0' && c != ' ') {
				buf.append(c);
				c = DisassemblyPanel.charAt(text, index);
			}
			while (c == ' ') {
				buf.append(c);
				c = DisassemblyPanel.charAt(text, index);
			}
			x = flushPartOfLine(g2, x, y, referenced ? COLOR_NORMAL : COLOR_UNREFERENCED, buf.toString());
			buf.setLength(0);
		} else if (c != ' ') {
			flushPartOfLine(g2, x, y, referenced ? COLOR_COMMENT : COLOR_UNREFERENCED, restOfLine(text, index));
			return;
		}

		// Now we have the instruction.
		Color instructionColor = COLOR_INSTRUCTION;
		while (c == ' ') {
			buf.append(c);
			c = DisassemblyPanel.charAt(text, index);
		}
		if (c == '=' || c == '*') {
			instructionColor = COLOR_NORMAL;
		}
		while (c != '\0' && c != ' ') {
			buf.append(c);
			c = DisassemblyPanel.charAt(text, index);
		}
		while (c == ' ') {
			buf.append(c);
			c = DisassemblyPanel.charAt(text, index);
		}
		x = flushPartOfLine(g2, x, y, referenced ? instructionColor : COLOR_UNREFERENCED, buf.toString());
		buf.setLength(0);

		// Now we have either a parameter or a comment.
		if (c == '"' || c == '\'') {
			char quote = c;
			buf.append(c);
			x = flushPartOfLine(g2, x, y, referenced ? COLOR_NORMAL : COLOR_UNREFERENCED, buf.toString());
			buf.setLength(0);
			c = DisassemblyPanel.charAt(text, index);
			while (c != '\0' && c != quote) {
				buf.append(c);
				c = DisassemblyPanel.charAt(text, index);
			}
			x = flushPartOfLine(g2, x, y, referenced ? COLOR_STRING : COLOR_UNREFERENCED, buf.toString());
			buf.setLength(0);
		} else if (c == '#') {
			buf.append(c);
			x = flushPartOfLine(g2, x, y, referenced ? COLOR_NORMAL : COLOR_UNREFERENCED, buf.toString());
			buf.setLength(0);
			c = DisassemblyPanel.charAt(text, index);
			while (c == ' ') {
				buf.append(c);
				c = DisassemblyPanel.charAt(text, index);
			}
		}
		do {
			if (c == ',') {
				buf.append(c);
				c = DisassemblyPanel.charAt(text, index);
				x = flushPartOfLine(g2, x, y, referenced ? COLOR_NORMAL : COLOR_UNREFERENCED, buf.toString());
				buf.setLength(0);
			}
			boolean isNumber = false;
			if (c == '$') {
				isNumber = true;
				buf.append(c);
				c = DisassemblyPanel.charAt(text, index);
				x = flushPartOfLine(g2, x, y, referenced ? COLOR_NORMAL : COLOR_UNREFERENCED, buf.toString());
				buf.setLength(0);
			}
			if (isNumber) {
				while (isHexDigit(c)) {
					buf.append(c);
					c = DisassemblyPanel.charAt(text, index);
				}
				x = flushPartOfLine(g2, x, y, referenced ? COLOR_NUMBER : COLOR_UNREFERENCED, buf.toString());
				buf.setLength(0);
			}
		} while (c == ',');

		// The rest of the line.
		if (c == ';') {
			flushPartOfLine(g2, x, y, referenced ? COLOR_COMMENT : COLOR_UNREFERENCED, restOfLine(text, index));
			return;
		}
		buf.append(c);
		c = DisassemblyPanel.charAt(text, index);
		while (c != '\0' && c != ';') {
			buf.append(c);
			c = DisassemblyPanel.charAt(text, index);
		}
		x = flushPartOfLine(g2, x, y, referenced ? COLOR_NORMAL : COLOR_UNREFERENCED, buf.toString());
		if (c == ';') {
			flushPartOfLine(g2, x, y, referenced ? COLOR_COMMENT : COLOR_UNREFERENCED, restOfLine(text, index));
		}
	}

	/** The text from the last character read (re-including it) to the end. */
	private static String restOfLine(String text, int[] index) {
		return text.substring(Math.min(index[0] - 1, text.length()));
	}

	private static boolean isHexDigit(char c) {
		return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
	}

	/**
	 * All sections other than the system/user equates are never greyed out,
	 * regardless of {@link DisassemblyLine#referenced}.
	 */
	private static boolean isReferenced(DisassemblyLine disassemblyLine) {
		DisassemblySectionType type = disassemblyLine.getSection().getType();
		if (type == DisassemblySectionType.SYSTEM_EQUATES || type == DisassemblySectionType.USER_EQUATES) {
			return disassemblyLine.referenced;
		}
		return true;
	}

	/**
	 * Draws one colored run and returns the x position just past it, using
	 * this font's own real glyph width - the same way every other call site
	 * in this class already uses {@link ComputerFont#drawText}.
	 */
	private int flushPartOfLine(Graphics2D g2, int x, int y, Color color, String text) {
		computerFont.drawText(g2, text, color, x, y);
		return x + text.length() * computerFont.getGlyphWidth();
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
