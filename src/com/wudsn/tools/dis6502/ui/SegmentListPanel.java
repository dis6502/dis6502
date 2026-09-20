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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;

import com.wudsn.tools.base.gui.ElementFactory;
import com.wudsn.tools.dis6502.Actions;
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
 * java.awt.Frame}) this panel does not otherwise have. Each item is built
 * via {@code com.wudsn.tools.base.gui.ElementFactory} from an {@code
 * Action} in {@code com.wudsn.tools.dis6502.Actions} (label/mnemonic
 * sourced from {@code dis6502.rc}'s {@code SEGMENT_LIST_POPUP_MENU}), the
 * same pattern {@link MainMenu} uses - see that class's/{@code Actions}'
 * own javadoc.
 * <p>
 * {@link #setComputerFont} is ported from {@code PartWindow::ApplyLayout}'s
 * blanket {@code SetFont(partLayout->GetLayout()->GetFont())} call, which
 * every part window gets, not just the memory inspector/disassembly
 * listing - {@code SegmentListWindow} is a plain native {@code ListBox}, so
 * in C++ this happens automatically via {@code WM_SETFONT}. This table only
 * ever shows already-formatted metadata text (titles, hex addresses), not
 * raw byte values, so it needs none of {@link ComputerFont}'s byte-indexed
 * glyph lookup - but it still cannot just be {@code table.setFont(...)} plus
 * JTable's default cell renderer: on real screen output (unlike the
 * offscreen renders used to develop this font support), Windows applies its
 * own ClearType/subpixel text antialiasing to ordinary Swing text painting,
 * which blurs this small pixel-art font into illegible dots. Every other
 * {@code ComputerFont}-driven panel avoids this because {@link
 * ComputerFont#drawText} explicitly disables antialiasing before drawing;
 * {@link ComputerFontTableCellRenderer} gives this table the same explicit
 * control by painting cell text through {@code drawText} itself instead of
 * relying on the default renderer's {@code g.drawString}.
 *
 * @author Peter Dell
 */
public final class SegmentListPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	public final JMenuItem moveUpMenuItem = ElementFactory.createMenuItem(Actions.SegmentListPopupMenu_MoveUp, "moveUpMenuItem");
	public final JMenuItem moveDownMenuItem = ElementFactory.createMenuItem(Actions.SegmentListPopupMenu_MoveDown, "moveDownMenuItem");
	public final JMenuItem mergeMenuItem = ElementFactory.createMenuItem(Actions.SegmentListPopupMenu_Merge, "mergeMenuItem");
	public final JMenuItem deleteMenuItem = ElementFactory.createMenuItem(Actions.SegmentListPopupMenu_Delete, "deleteMenuItem");
	public final JMenuItem saveNoHeaderMenuItem = ElementFactory.createMenuItem(Actions.SegmentListPopupMenu_SaveNoHeader, "saveNoHeaderMenuItem");
	public final JMenuItem saveHeaderMenuItem = ElementFactory.createMenuItem(Actions.SegmentListPopupMenu_SaveHeader, "saveHeaderMenuItem");
	public final JMenuItem saveAllMenuItem = ElementFactory.createMenuItem(Actions.SegmentListPopupMenu_SaveAll, "saveAllMenuItem");
	public final JMenuItem propertiesMenuItem = ElementFactory.createMenuItem(Actions.SegmentListPopupMenu_Properties, "propertiesMenuItem");

	private final JPopupMenu popupMenu = new JPopupMenu();
	private final Model model = new Model();
	private final JTable table = new JTable(model);
	private final ComputerFontTableCellRenderer cellRenderer = new ComputerFontTableCellRenderer();
	private Workspace workspace;
	private boolean updating;

	public SegmentListPanel() {
		super(new BorderLayout());
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.setDefaultRenderer(Object.class, cellRenderer);
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

	/** Ported from PartWindow::ApplyLayout's SetFont(partLayout->GetLayout()->GetFont()) - call whenever the workspace's computer system or double-height setting changes. */
	public void setComputerFont(ComputerFont computerFont) {
		table.setFont(computerFont.getAwtFont());
		table.setRowHeight(computerFont.getGlyphHeight() + 2);
		cellRenderer.setComputerFont(computerFont);
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

	/**
	 * Paints cell text via {@link ComputerFont#drawText} instead of {@code
	 * JLabel}'s own {@code g.drawString}, so the same explicit
	 * antialiasing-off control every other {@code ComputerFont}-driven panel
	 * relies on also applies here - see the class comment for why plain
	 * {@code table.setFont(...)} is not enough on real screen output.
	 */
	private static final class ComputerFontTableCellRenderer extends JComponent implements TableCellRenderer {

		private static final long serialVersionUID = 1L;

		private ComputerFont computerFont;
		private String text = "";
		private Color foreground = Color.BLACK;
		private Color background = Color.WHITE;

		ComputerFontTableCellRenderer() {
			setOpaque(true);
		}

		void setComputerFont(ComputerFont computerFont) {
			this.computerFont = computerFont;
		}

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			text = value == null ? "" : String.valueOf(value);
			background = isSelected ? table.getSelectionBackground() : table.getBackground();
			foreground = isSelected ? table.getSelectionForeground() : table.getForeground();
			return this;
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
