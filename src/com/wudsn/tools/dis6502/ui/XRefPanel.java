/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.wudsn.tools.base.common.TextUtility;
import com.wudsn.tools.dis6502.Texts;

/**
 * A list of the disassembly lines matching the current search string
 * ({@link com.wudsn.tools.dis6502.model.DisassemblyResult#findAndSelectLines}),
 * letting the user jump back to any of them.
 * <p>
 * The memory inspector byte-range sync on selection change is not wired
 * up yet - the segment/disassembly-line sync is, see {@code
 * Dis6502.performXRefSelected}.
 * <p>
 * This list only ever shows already-formatted disassembly line text, not
 * raw byte values, so {@link #setTextFont} needs none of {@link
 * ComputerFont}'s byte-indexed glyph lookup, matching {@link
 * SegmentListPanel}'s own {@code setTextFont}. It uses the shared
 * {@link ComputerFontListCellRenderer} (also used by {@link
 * SegmentListPanel}) rather than plain {@code list.setFont(...)} - see
 * that class's own javadoc for why.
 *
 * @author Peter Dell
 */
public final class XRefPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	private final PartHeaderPanel header = new PartHeaderPanel(new Color(255, 192, 192));
	private final DefaultListModel<Entry> listModel = new DefaultListModel<>();
	private final JList<Entry> list = new JList<>(listModel);
	private final ComputerFontListCellRenderer<Entry> cellRenderer = new ComputerFontListCellRenderer<>();

	private XRefSelectionListener selectionListener;

	public XRefPanel() {
		super(new BorderLayout());
		header.setText(Texts.XRefPanel_NoLabelSelectedTitle);
		add(header, BorderLayout.NORTH);
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

	/** Call whenever the workspace's computer system, double-height setting, or chosen text font changes. */
	public void setTextFont(TextFont textFont) {
		list.setFont(textFont.getAwtFont());
		cellRenderer.setTextFont(textFont);
		header.setTextFont(textFont);
	}

	/**
	 * {@code entries} empty shows "No label selected" instead of a reference
	 * count/list. Built from the {@code Texts.XRefPanel_*Title} fields.
	 */
	public void updateList(String findString, List<Entry> entries) {
		listModel.clear();
		if (findString.isEmpty() || entries.isEmpty()) {
			header.setText(Texts.XRefPanel_NoLabelSelectedTitle);
		} else {
			String pattern = entries.size() == 1 ? Texts.XRefPanel_LabelReferenceTitle : Texts.XRefPanel_LabelReferencesTitle;
			header.setText(TextUtility.format(pattern, String.valueOf(entries.size()), findString));
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

}
