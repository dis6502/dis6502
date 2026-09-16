/*
 * Copyright (C) 2025 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.ui;

import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;

import com.wudsn.tools.dis6502.model.Segment;
import com.wudsn.tools.dis6502.model.SegmentList;
import com.wudsn.tools.dis6502.model.Workspace;
import com.wudsn.tools.dis6502.model.WorkspaceChangedListener;
import com.wudsn.tools.dis6502.model.WorkspaceProperty;

/**
 * A table of the current workspace's segments.
 * <p>
 * Ported from ui/SegmentListWindow.h / SegmentListWindow.cpp and the
 * selection handling part of ui/MainSegment.cpp - {@link #refresh} from
 * {@code MainSegment::UpdateList}, {@link #selected} from {@code
 * MainSegment::Selected} - simplified to a plain {@link JTable} for this
 * first pass; the {@code updating} guard replicates {@code
 * MainSegment::updateCounter}'s reentrancy protection between the two
 * directions of selection sync. {@link #updatePopupMenuState} is ported
 * from {@code SegmentListPopupMenu::Update} and {@link #maybeShowPopup}
 * from {@code MainSegment::RButtonDownProc} (including its "no segments,
 * no menu" guard); the popup's items are exposed as public fields, with
 * their commands wired up by {@code Dis6502} the same way {@link
 * MainMenu}'s items are, since running them (saving files, editing a
 * segment) needs things ({@code Application}, a parent {@link
 * java.awt.Frame}) this panel does not otherwise have.
 *
 * @author Peter Dell
 */
public final class SegmentListPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	public final JMenuItem moveUpMenuItem = new JMenuItem("Move Up");
	public final JMenuItem moveDownMenuItem = new JMenuItem("Move Down");
	public final JMenuItem mergeMenuItem = new JMenuItem("Merge Segments");
	public final JMenuItem deleteMenuItem = new JMenuItem("Delete");
	public final JMenuItem saveNoHeaderMenuItem = new JMenuItem("Save Segment (No Header)...");
	public final JMenuItem saveHeaderMenuItem = new JMenuItem("Save Segment (With Header)...");
	public final JMenuItem saveAllMenuItem = new JMenuItem("Save All Segments...");
	public final JMenuItem propertiesMenuItem = new JMenuItem("Properties...");

	private final JPopupMenu popupMenu = new JPopupMenu();
	private final Model model = new Model();
	private final JTable table = new JTable(model);
	private Workspace workspace;
	private boolean updating;

	public SegmentListPanel() {
		super(new BorderLayout());
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.getSelectionModel().addListSelectionListener(e -> {
			if (!e.getValueIsAdjusting() && !updating) {
				selected();
			}
		});

		popupMenu.add(moveUpMenuItem);
		popupMenu.add(moveDownMenuItem);
		popupMenu.addSeparator();
		popupMenu.add(mergeMenuItem);
		popupMenu.add(deleteMenuItem);
		popupMenu.addSeparator();
		popupMenu.add(saveNoHeaderMenuItem);
		popupMenu.add(saveHeaderMenuItem);
		popupMenu.add(saveAllMenuItem);
		popupMenu.addSeparator();
		popupMenu.add(propertiesMenuItem);
		table.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				maybeShowPopup(e);
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				maybeShowPopup(e);
			}
		});

		add(new JScrollPane(table), BorderLayout.CENTER);
	}

	/** Ported from MainSegment::RButtonDownProc (the "no edit mode" branch - there is no memory inspector edit mode to check here yet). */
	private void maybeShowPopup(MouseEvent e) {
		if (!e.isPopupTrigger() || workspace == null || workspace.getSegmentList().isEmpty()) {
			return;
		}
		updatePopupMenuState();
		popupMenu.show(table, e.getX(), e.getY());
	}

	/** Ported from SegmentListPopupMenu::Update. */
	private void updatePopupMenuState() {
		int segmentCount = workspace.getSegmentList().getCount();
		int selectedIndex = workspace.getSegmentList().getSelectedIndex();
		boolean selected = selectedIndex >= 0;

		moveUpMenuItem.setEnabled(selectedIndex > 0);
		moveDownMenuItem.setEnabled(selectedIndex < segmentCount - 1);
		mergeMenuItem.setEnabled(segmentCount > 1);
		deleteMenuItem.setEnabled(selected);
		saveNoHeaderMenuItem.setEnabled(selected);
		saveHeaderMenuItem.setEnabled(selected);
		saveAllMenuItem.setEnabled(segmentCount > 0);
		propertiesMenuItem.setEnabled(selected);
	}

	public void setWorkspace(Workspace workspace) {
		this.workspace = workspace;
		workspace.addListener(new WorkspaceChangedListener() {
			@Override
			public void handleWorkspaceChanged(Workspace changedWorkspace, List<WorkspaceProperty> properties) {
				if (properties.contains(WorkspaceProperty.SEGMENTS) || properties.contains(WorkspaceProperty.SELECTED_SEGMENT)) {
					refresh();
				}
			}
		});
		refresh();
	}

	/** Ported from MainSegment::UpdateList. */
	public void refresh() {
		updating = true;
		try {
			model.fireTableDataChanged();
			int selectedIndex = workspace == null ? SegmentList.NO_SEGMENT_INDEX : workspace.getSegmentList().getSelectedIndex();
			if (selectedIndex < 0) {
				table.clearSelection();
			} else {
				table.setRowSelectionInterval(selectedIndex, selectedIndex);
			}
		} finally {
			updating = false;
		}
	}

	/** Ported from MainSegment::Selected. */
	private void selected() {
		updating = true;
		try {
			int rowIndex = table.getSelectedRow();
			int segmentIndex = rowIndex < 0 ? SegmentList.NO_SEGMENT_INDEX : rowIndex;
			workspace.getSegmentList().setSelectedIndex(segmentIndex);
		} finally {
			updating = false;
		}
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
