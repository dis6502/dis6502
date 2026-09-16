/*
 * Copyright (C) 2025 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.ui;

import java.awt.BorderLayout;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import com.wudsn.tools.dis6502.model.Segment;
import com.wudsn.tools.dis6502.model.Workspace;
import com.wudsn.tools.dis6502.model.WorkspaceChangedListener;
import com.wudsn.tools.dis6502.model.WorkspaceProperty;

/**
 * A table of the current workspace's segments.
 * <p>
 * Ported from ui/SegmentListWindow.h / SegmentListWindow.cpp (and the
 * segment list part of ui/MainSegment.cpp), simplified to a plain {@link
 * JTable} for this first pass - selection handling, the segment properties
 * dialog, and the segment list popup menu (ui/SegmentListPopupMenu.h/.cpp)
 * are not ported yet.
 *
 * @author Peter Dell
 */
public final class SegmentListPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	private final Model model = new Model();
	private Workspace workspace;

	public SegmentListPanel() {
		super(new BorderLayout());
		JTable table = new JTable(model);
		add(new JScrollPane(table), BorderLayout.CENTER);
	}

	public void setWorkspace(Workspace workspace) {
		this.workspace = workspace;
		workspace.addListener(new WorkspaceChangedListener() {
			@Override
			public void handleWorkspaceChanged(Workspace changedWorkspace, List<WorkspaceProperty> properties) {
				if (properties.contains(WorkspaceProperty.SEGMENTS)) {
					refresh();
				}
			}
		});
		refresh();
	}

	public void refresh() {
		model.fireTableDataChanged();
	}

	private final class Model extends AbstractTableModel {

		private static final long serialVersionUID = 1L;

		private final String[] columnNames = { "Title", "Header", "Begin", "End", "Size", "Binary" };

		@Override
		public int getRowCount() {
			return workspace == null ? 0 : workspace.getSegmentList().getCount();
		}

		@Override
		public int getColumnCount() {
			return columnNames.length;
		}

		@Override
		public String getColumnName(int column) {
			return columnNames[column];
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			Segment segment = workspace.getSegmentList().getSegment(rowIndex);
			switch (columnIndex) {
			case 0:
				return segment.title;
			case 1:
				return segment.getHeader();
			case 2:
				return String.format("$%04X", segment.wBegin);
			case 3:
				return String.format("$%04X", segment.wEnd);
			case 4:
				return segment.getSize();
			case 5:
				return segment.bBinary;
			default:
				return "";
			}
		}
	}
}
