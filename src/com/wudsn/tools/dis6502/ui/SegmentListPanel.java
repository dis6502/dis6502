/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import com.wudsn.tools.base.common.TextUtility;
import com.wudsn.tools.base.gui.ElementFactory;
import com.wudsn.tools.dis6502.Actions;
import com.wudsn.tools.dis6502.Texts;
import com.wudsn.tools.dis6502.model.Segment;
import com.wudsn.tools.dis6502.model.SegmentList;
import com.wudsn.tools.dis6502.model.Workspace;
import com.wudsn.tools.dis6502.model.WorkspaceChangedListener;
import com.wudsn.tools.dis6502.model.WorkspaceProperty;

/**
 * A list of the current workspace's segments.
 * <p>
 * The {@code updating} guard prevents reentrancy between the two
 * directions of selection sync: {@link #refresh}, which rebuilds the list
 * from the model, and {@link #selected}, which pushes a UI selection back
 * into the model. Each row's text is {@link Segment#toString()}, a single
 * string per segment. An earlier version of this class instead showed a
 * {@link javax.swing.JTable} with separate Title/Header/Begin/End/Size/
 * Binary columns; that was abandoned in favor of the current plain {@link
 * JList}.
 * <p>
 * {@link #list} uses {@link ListSelectionModel#MULTIPLE_INTERVAL_SELECTION},
 * letting {@link #deleteMenuItem} remove every selected segment in one step
 * via {@link #getSelectedSegmentIndices}/{@link
 * com.wudsn.tools.dis6502.model.SegmentList#deleteSegments} - every other
 * command ({@link #moveUpMenuItem}/{@link #moveDownMenuItem}/{@link
 * #saveNoHeaderMenuItem}/{@link #saveHeaderMenuItem}/{@link
 * #propertiesMenuItem}) is inherently single-segment and stays gated on
 * exactly one selected row (see {@link #updatePopupMenuState}), and {@link
 * #mergeMenuItem} is unaffected by selection - {@link
 * com.wudsn.tools.dis6502.model.SegmentList#mergeSegments} is a whole-list
 * sweep for adjacent, compatible segments that never looks at the
 * selection to begin with.
 * <p>
 * {@link #updatePopupMenuState}/{@link #maybeShowPopup} (including its "no
 * segments, no menu" guard) drive the popup; the popup's items are exposed
 * as public fields, with their commands wired up by {@code Dis6502} the
 * same way {@link MainMenu}'s items are, since running them (saving files,
 * editing a segment) needs things ({@code Application}, a parent {@link
 * java.awt.Frame}) this panel does not otherwise have. Each item is built
 * via {@code com.wudsn.tools.base.gui.ElementFactory} from an {@code
 * Action} in {@code com.wudsn.tools.dis6502.Actions}, the same pattern
 * {@link MainMenu} uses - see that class's/{@code Actions}' own javadoc.
 * <p>
 * This list only ever shows already-formatted metadata text, not raw byte
 * values, so {@link #setTextFont} needs none of {@link ComputerFont}'s
 * byte-indexed glyph lookup - but it still cannot just be {@code
 * list.setFont(...)} plus {@link JList}'s default renderer: on real screen
 * output (unlike the offscreen renders used to develop this font support),
 * the desktop applies its own subpixel text antialiasing to ordinary Swing
 * text painting, which blurs {@link ComputerFont}'s pixel-art font into
 * illegible dots. {@link ComputerFontListCellRenderer} (shared with {@link
 * XRefPanel}) gives this list explicit control by painting cell text
 * through {@link TextFont#drawText} itself instead of relying on the
 * default renderer's {@code g.drawString} - each {@link TextFont}
 * implementation renders itself correctly there, {@link ComputerFont}'s
 * antialiasing-off included, so a user-chosen {@link PlainTextFont} needs
 * no special handling here at all.
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
	private final DefaultListModel<Segment> listModel = new DefaultListModel<>();
	private final JList<Segment> list = new JList<>(listModel);
	private final ComputerFontListCellRenderer<Segment> cellRenderer = new ComputerFontListCellRenderer<>();
	private final PartHeaderPanel header = new PartHeaderPanel(new Color(255, 255, 0));
	private Workspace workspace;
	private boolean updating;
	private String fileName = "";

	public SegmentListPanel() {
		super(new BorderLayout());
		add(header, BorderLayout.NORTH);
		list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		list.setCellRenderer(cellRenderer);
		list.addListSelectionListener(e -> {
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
		list.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				maybeShowPopup(e);
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				maybeShowPopup(e);
			}
		});

		JScrollPane scrollPane = new JScrollPane(list);
		// Splitters already separate the part windows - the scroll pane's own
		// L&F-default border would just draw a redundant line right next to them.
		scrollPane.setBorder(BorderFactory.createEmptyBorder());
		add(scrollPane, BorderLayout.CENTER);
	}

	/** Call whenever the workspace's computer system, double-height setting, or chosen text font changes. */
	public void setTextFont(TextFont textFont) {
		list.setFont(textFont.getAwtFont());
		cellRenderer.setTextFont(textFont);
		header.setTextFont(textFont);
	}

	/**
	 * The currently loaded file's name, shown in {@link #header}. Call
	 * whenever {@code Dis6502}'s own current-file state changes; pass
	 * {@code null} once there is none.
	 */
	public void setFileName(String fileName) {
		this.fileName = fileName == null ? "" : fileName;
		updateHeaderText();
	}

	/** Shows the file name and segment count, or a "no segments loaded" message if the workspace is empty. */
	private void updateHeaderText() {
		boolean empty = workspace == null || workspace.getSegmentList().isEmpty();
		header.setText(empty ? Texts.SegmentListPanel_NoSegmentsLoadedTitle : TextUtility.format(Texts.SegmentListPanel_Title, fileName));
	}

	/** Shows the popup menu, unless there are no segments to act on. */
	private void maybeShowPopup(MouseEvent e) {
		if (!e.isPopupTrigger() || workspace == null || workspace.getSegmentList().isEmpty()) {
			return;
		}
		updatePopupMenuState();
		popupMenu.show(list, e.getX(), e.getY());
	}

	/**
	 * {@link #deleteMenuItem} is enabled for one or more selected rows (see
	 * this class's own javadoc for the multi-selection this enables), while
	 * every other item stays gated on exactly one selected row, since Move
	 * Up/Down, Save (with/without header) and Properties are inherently
	 * single-segment operations.
	 */
	private void updatePopupMenuState() {
		int segmentCount = workspace.getSegmentList().getCount();
		int selectedIndex = workspace.getSegmentList().getSelectedIndex();
		int selectedCount = list.getSelectedIndices().length;
		boolean singleSelected = selectedCount == 1;

		moveUpMenuItem.setEnabled(singleSelected && selectedIndex > 0);
		moveDownMenuItem.setEnabled(singleSelected && selectedIndex < segmentCount - 1);
		mergeMenuItem.setEnabled(segmentCount > 1);
		deleteMenuItem.setEnabled(selectedCount > 0);
		saveNoHeaderMenuItem.setEnabled(singleSelected);
		saveHeaderMenuItem.setEnabled(singleSelected);
		saveAllMenuItem.setEnabled(segmentCount > 0);
		propertiesMenuItem.setEnabled(singleSelected);
	}

	public void setWorkspace(Workspace workspace) {
		this.workspace = workspace;
		workspace.addListener(new WorkspaceChangedListener() {
			@Override
			public void handleWorkspaceChanged(Workspace changedWorkspace, List<WorkspaceProperty> properties) {
				// The !updating check matters more than it used to: with
				// multi-selection enabled, adding a second row to an
				// existing selection still calls setSelectedIndex with the
				// same (smallest-row) value in selected() below, which
				// unconditionally re-notifies SELECTED_SEGMENT - without
				// this guard, that self-inflicted notification would reach
				// refresh() and collapse the list's real, just-made
				// multi-row selection back down to one row before the user
				// even sees it.
				if (!updating
						&& (properties.contains(WorkspaceProperty.SEGMENTS) || properties.contains(WorkspaceProperty.SELECTED_SEGMENT))) {
					refresh();
				}
			}
		});
		refresh();
	}

	/** Rebuilds the list from the workspace's current segments and selection. */
	public void refresh() {
		updating = true;
		try {
			listModel.clear();
			if (workspace != null) {
				SegmentList segmentList = workspace.getSegmentList();
				for (int i = 0; i < segmentList.getCount(); i++) {
					listModel.addElement(segmentList.getSegment(i));
				}
			}
			int selectedIndex = workspace == null ? SegmentList.NO_SEGMENT_INDEX : workspace.getSegmentList().getSelectedIndex();
			if (selectedIndex < 0) {
				list.clearSelection();
			} else {
				list.setSelectedIndex(selectedIndex);
			}
			updateHeaderText();
		} finally {
			updating = false;
		}
	}

	/**
	 * {@code list.getSelectedIndex()} (the smallest selected index, by
	 * {@link JList}'s own contract) drives {@link
	 * Workspace#getSegmentList()}'s single {@code selectedIndex} even with
	 * multi-selection enabled, so the memory inspector/Properties/Save
	 * Segment/Move Up/Down - every inherently single-segment concept here -
	 * keep tracking one well-defined "current" segment, regardless of how
	 * many rows are actually selected.
	 */
	private void selected() {
		updating = true;
		try {
			int index = list.getSelectedIndex();
			int segmentIndex = index < 0 ? SegmentList.NO_SEGMENT_INDEX : index;
			workspace.getSegmentList().setSelectedIndex(segmentIndex);
		} finally {
			updating = false;
		}
	}

	/**
	 * The indices of every currently selected row, ascending - added
	 * alongside {@link #deleteMenuItem}'s multi-selection support (see this
	 * class's own javadoc) for {@code Dis6502} to delete them all via {@link
	 * SegmentList#deleteSegments}.
	 */
	public int[] getSelectedSegmentIndices() {
		return list.getSelectedIndices();
	}
}
