/*
 * Copyright (C) 2025 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.border.TitledBorder;

/**
 * A list of the disassembly lines matching the current search string
 * ({@link com.wudsn.tools.dis6502.model.DisassemblyResult#findAndSelectLines}),
 * letting the user jump back to any of them.
 * <p>
 * Ported from ui/MainXRef.h / MainXRef.cpp and ui/XRefListWindow.h/.cpp.
 * {@code MainXRef::HandleSelectionChanged}'s memory inspector byte-range
 * sync is not wired up yet - the segment/disassembly-line sync it also
 * does is, see {@code Dis6502.performXRefSelected}.
 * <p>
 * {@link #setComputerFont} is ported from {@code PartWindow::ApplyLayout}'s
 * blanket {@code SetFont(...)} call - {@code XRefListWindow} is a plain
 * native {@code ListBox} in C++, getting this automatically via {@code
 * WM_SETFONT}, matching {@link SegmentListPanel}'s own {@code
 * setComputerFont}: this list only ever shows already-formatted
 * disassembly line text, not raw byte values, so it needs none of {@link
 * ComputerFont}'s byte-indexed glyph lookup. It uses the same {@link
 * ComputerFontListCellRenderer} approach as {@link SegmentListPanel} rather
 * than plain {@code list.setFont(...)}, for the same reason documented on
 * that class: real on-screen Windows ClearType antialiasing blurs this
 * pixel-art font under {@code JList}'s default renderer, where explicitly
 * antialiasing-off {@link ComputerFont#drawText} does not.
 *
 * @author Peter Dell
 */
public final class XRefPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	// Flat empty base border instead of createTitledBorder(String)'s L&F-default
	// one, which paints a bevel/etched box around the whole panel on this
	// project's native (Windows) look and feel - not wanted, just the title text.
	private final TitledBorder titledBorder = BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), "No label selected");
	private final DefaultListModel<Entry> listModel = new DefaultListModel<>();
	private final JList<Entry> list = new JList<>(listModel);
	private final ComputerFontListCellRenderer cellRenderer = new ComputerFontListCellRenderer();

	private XRefSelectionListener selectionListener;

	public XRefPanel() {
		super(new BorderLayout());
		setBorder(titledBorder);
		list.setCellRenderer(cellRenderer);
		JScrollPane scrollPane = new JScrollPane(list);
		// Splitters already separate the part windows - the scroll pane's own
		// L&F-default border would just draw a redundant line right next to them.
		scrollPane.setBorder(BorderFactory.createEmptyBorder());
		add(scrollPane, BorderLayout.CENTER);

		list.addListSelectionListener(e -> {
			if (!e.getValueIsAdjusting() && selectionListener != null) {
				Entry selected = list.getSelectedValue();
				if (selected != null) {
					selectionListener.onXRefSelected(selected.xrefLineNumber);
				}
			}
		});
	}

	public void setSelectionListener(XRefSelectionListener selectionListener) {
		this.selectionListener = selectionListener;
	}

	/** Ported from PartWindow::ApplyLayout's SetFont(partLayout->GetLayout()->GetFont()) - call whenever the workspace's computer system or double-height setting changes. */
	public void setComputerFont(ComputerFont computerFont) {
		list.setFont(computerFont.getAwtFont());
		cellRenderer.setComputerFont(computerFont);
	}

	/**
	 * Ported from MainXRef::UpdateList. {@code entries} is empty (matching
	 * an empty {@code findString} in C++) to show "No label selected"
	 * instead of a reference count/list.
	 */
	public void updateList(String findString, List<Entry> entries) {
		listModel.clear();
		if (findString.isEmpty() || entries.isEmpty()) {
			titledBorder.setTitle("No label selected");
		} else {
			titledBorder.setTitle(entries.size() + (entries.size() == 1 ? " Reference to \"" : " References to \"") + findString + "\"");
			for (Entry entry : entries) {
				listModel.addElement(entry);
			}
		}
		revalidate();
		repaint();
	}

	public interface XRefSelectionListener {
		void onXRefSelected(int xrefLineNumber);
	}

	/** One matching line: its XRef list number (see {@code DisassemblyLine#xrefLineNumber}) and display text. */
	public static final class Entry {
		public final int xrefLineNumber;
		public final String text;

		public Entry(int xrefLineNumber, String text) {
			this.xrefLineNumber = xrefLineNumber;
			this.text = text;
		}

		@Override
		public String toString() {
			return text;
		}
	}

	/**
	 * Paints entry text via {@link ComputerFont#drawText} instead of {@code
	 * JLabel}'s own {@code g.drawString} - see the class comment for why
	 * plain {@code list.setFont(...)} is not enough on real screen output.
	 */
	private static final class ComputerFontListCellRenderer extends JComponent implements ListCellRenderer<Entry> {

		private static final long serialVersionUID = 1L;

		private ComputerFont computerFont;
		private String text = "";
		private Color foreground = Color.BLACK;
		private Color background = Color.WHITE;

		ComputerFontListCellRenderer() {
			setOpaque(true);
		}

		void setComputerFont(ComputerFont computerFont) {
			this.computerFont = computerFont;
		}

		@Override
		public Component getListCellRendererComponent(JList<? extends Entry> list, Entry value, int index, boolean isSelected, boolean cellHasFocus) {
			text = value == null ? "" : value.toString();
			background = isSelected ? list.getSelectionBackground() : list.getBackground();
			foreground = isSelected ? list.getSelectionForeground() : list.getForeground();
			setFont(list.getFont());
			return this;
		}

		@Override
		public java.awt.Dimension getPreferredSize() {
			java.awt.FontMetrics metrics = getFontMetrics(getFont());
			return new java.awt.Dimension(metrics.stringWidth(text) + 4, metrics.getHeight() + 2);
		}

		@Override
		protected void paintComponent(Graphics g) {
			g.setColor(background);
			g.fillRect(0, 0, getWidth(), getHeight());
			if (computerFont != null) {
				computerFont.drawText((Graphics2D) g, text, foreground, 2, 1);
			} else {
				g.setColor(foreground);
				g.drawString(text, 2, g.getFontMetrics().getAscent() + 1);
			}
		}
	}
}
