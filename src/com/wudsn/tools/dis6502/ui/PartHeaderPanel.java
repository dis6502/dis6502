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
 * panels: a fixed background color and black text, drawn with the same
 * per-computer-system {@link ComputerFont} as the part window's own
 * content, not a plain system font. A small custom-painted component
 * rather than a {@link javax.swing.border.TitledBorder} (used elsewhere in
 * this port for a plain, uncolored title) since {@code TitledBorder} has
 * no way to fill its own background.
 * <p>
 * The background fill is a plain {@link Graphics#fillRect}, filling the
 * whole rectangle with the background color before drawing the text - not
 * just the text's own bounding box.
 * <p>
 * Every header here is always painted with black text; there is no
 * focus-dependent color change.
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
			// 1 pixel offset from the rectangle's origin.
			computerFont.drawText((Graphics2D) g, text, Color.BLACK, 1, 1);
		}
	}
}
