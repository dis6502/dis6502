/*
 * Copyright (C) 2025 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.ui;

import java.awt.BorderLayout;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
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
 *
 * @author Peter Dell
 */
public final class XRefPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	private final TitledBorder titledBorder = BorderFactory.createTitledBorder("No label selected");
	private final DefaultListModel<Entry> listModel = new DefaultListModel<>();
	private final JList<Entry> list = new JList<>(listModel);

	private XRefSelectionListener selectionListener;

	public XRefPanel() {
		super(new BorderLayout());
		setBorder(titledBorder);
		add(new JScrollPane(list), BorderLayout.CENTER);

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
}
