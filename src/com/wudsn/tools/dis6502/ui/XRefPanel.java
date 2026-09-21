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
import com.wudsn.tools.dis6502.Text;

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
 * ComputerFont}'s byte-indexed glyph lookup. It uses the shared {@link
 * ComputerFontListCellRenderer} (also used by {@link SegmentListPanel})
 * rather than plain {@code list.setFont(...)} - see that class's own
 * javadoc for why.
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
		header.setText(Text.IDS_XREF_TITLE_NO_LABEL_SELECTED);
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

	/** Ported from PartWindow::ApplyLayout's SetFont(partLayout->GetLayout()->GetFont()) - call whenever the workspace's computer system or double-height setting changes. */
	public void setComputerFont(ComputerFont computerFont) {
		list.setFont(computerFont.getAwtFont());
		cellRenderer.setComputerFont(computerFont);
		header.setComputerFont(computerFont);
	}

	/**
	 * Ported from MainXRef::UpdateList. {@code entries} is empty (matching
	 * an empty {@code findString} in C++) to show "No label selected"
	 * instead of a reference count/list. Now built from the actual {@code
	 * Text.IDS_XREF_TITLE_*} resources instead of a hand-written literal -
	 * switching to them also fixed the wording ("No label selected" vs.
	 * the resource's "No Label Selected", "N Reference(s) to" vs. the
	 * resource's "N reference(s) for") to match the C++ source exactly.
	 */
	public void updateList(String findString, List<Entry> entries) {
		listModel.clear();
		if (findString.isEmpty() || entries.isEmpty()) {
			header.setText(Text.IDS_XREF_TITLE_NO_LABEL_SELECTED);
		} else {
			String pattern = entries.size() == 1 ? Text.IDS_XREF_TITLE_LABEL_REFERENCE : Text.IDS_XREF_TITLE_LABEL_REFERENCES;
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
