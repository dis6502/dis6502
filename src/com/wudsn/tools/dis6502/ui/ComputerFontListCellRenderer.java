/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

/**
 * Paints a {@link JList} cell's {@code value.toString()} via {@link
 * TextFont#drawText} instead of the default renderer's {@code JLabel}/
 * {@code g.drawString} - shared by every {@code JList}-based part panel
 * that shows already-formatted text (not raw byte values, so no
 * byte-indexed glyph lookup is ever needed) - {@link XRefPanel}, {@link
 * SegmentListPanel}. By default {@link #textFont} is a {@link
 * ComputerFont}: real on-screen Windows ClearType/subpixel antialiasing
 * blurs that small pixel-art font under a plain {@code list.setFont(...)}/
 * default-renderer setup into illegible dots, which is why this class's own
 * {@link #paintComponent} relies on {@link TextFont#drawText} instead of
 * the default renderer's painting; a user-chosen {@link PlainTextFont}
 * renders correctly the same way, via ordinary antialiasing.
 *
 * @author Peter Dell
 */
final class ComputerFontListCellRenderer<T> extends JComponent implements ListCellRenderer<T> {

	private static final long serialVersionUID = 1L;

	private TextFont textFont;
	private String text = "";
	private Color foreground = Color.BLACK;
	private Color background = Color.WHITE;

	ComputerFontListCellRenderer() {
		setOpaque(true);
	}

	void setTextFont(TextFont textFont) {
		this.textFont = textFont;
	}

	@Override
	public Component getListCellRendererComponent(JList<? extends T> list, T value, int index, boolean isSelected, boolean cellHasFocus) {
		text = value == null ? "" : value.toString();
		background = isSelected ? list.getSelectionBackground() : list.getBackground();
		foreground = isSelected ? list.getSelectionForeground() : list.getForeground();
		setFont(list.getFont());
		return this;
	}

	@Override
	public Dimension getPreferredSize() {
		FontMetrics metrics = getFontMetrics(getFont());
		return new Dimension(metrics.stringWidth(text) + 4, metrics.getHeight() + 2);
	}

	@Override
	protected void paintComponent(Graphics g) {
		g.setColor(background);
		g.fillRect(0, 0, getWidth(), getHeight());
		if (textFont != null) {
			textFont.drawText((Graphics2D) g, text, foreground, 2, 1);
		} else {
			g.setColor(foreground);
			g.drawString(text, 2, g.getFontMetrics().getAscent() + 1);
		}
	}
}
