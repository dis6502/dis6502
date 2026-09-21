/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JComponent;

/**
 * The flat-colored title bar shown above each of the main window's five part
 * panels, ported from {@code Main::PaintMainWindow} (src/ui/Main.cpp), which
 * paints one of these per part window - a fixed background color and (with
 * one exception, see below) black text, drawn with the same per-computer-
 * system {@link ComputerFont} as the part window's own content, not a plain
 * system font ({@code PaintMainWindow} selects {@code Main::GetResizedFont()}
 * before painting any of them). Reproduced here as a small custom-painted
 * component instead of a {@link javax.swing.border.TitledBorder} (used
 * elsewhere in this port for a plain, uncolored title) since {@code
 * TitledBorder} has no way to fill its own background.
 * <p>
 * The background fill is a plain {@link Graphics#fillRect}, matching {@code
 * DC::ExtTextOut}'s {@code ETO_OPAQUE} flag, which fills the whole passed
 * rectangle with the current background color before drawing the text -
 * not just the text's own bounding box.
 * <p>
 * The memory inspector's title text color in C++ actually depends on
 * whether {@code MemoryInspectorControlImpl} has focus (black if focused,
 * gray otherwise) - not reproduced here: the C++ source's own comment at
 * that exact line reads {@code "// TODO Detection of focus does not
 * actually work."}, so that branch never actually returns anything but
 * black in practice. Every header here is always painted with black text.
 *
 * @author Peter Dell
 */
final class PartHeaderPanel extends JComponent {

	private static final long serialVersionUID = 1L;

	private ComputerFont computerFont;
	private String text = "";

	PartHeaderPanel(Color background) {
		setOpaque(true);
		setBackground(background);
	}

	void setComputerFont(ComputerFont computerFont) {
		this.computerFont = computerFont;
		revalidate();
		repaint();
	}

	void setText(String text) {
		this.text = text;
		repaint();
	}

	@Override
	public Dimension getPreferredSize() {
		int height = computerFont == null ? 16 : computerFont.getGlyphHeight() + 2;
		return new Dimension(10, height);
	}

	@Override
	protected void paintComponent(Graphics g) {
		g.setColor(getBackground());
		g.fillRect(0, 0, getWidth(), getHeight());
		if (computerFont != null) {
			// Matches DC::ExtTextOut(1, 1, rc, title)'s offset from the rectangle's origin.
			computerFont.drawText((Graphics2D) g, text, Color.BLACK, 1, 1);
		}
	}
}
