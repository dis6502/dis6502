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
import com.wudsn.tools.dis6502.Text;
import com.wudsn.tools.dis6502.model.Segment;
import com.wudsn.tools.dis6502.model.SegmentList;
import com.wudsn.tools.dis6502.model.Workspace;
import com.wudsn.tools.dis6502.model.WorkspaceChangedListener;
import com.wudsn.tools.dis6502.model.WorkspaceProperty;

/**
 * A list of the current workspace's segments.
 * <p>
 * Ported from ui/SegmentListWindow.h / SegmentListWindow.cpp and the
 * selection handling part of ui/MainSegment.cpp - {@link #refresh} from
 * {@code MainSegment::UpdateList}, {@link #selected} from {@code
 * MainSegment::Selected} - simplified to a plain {@link JList} for this
 * first pass; the {@code updating} guard replicates {@code
 * MainSegment::updateCounter}'s reentrancy protection between the two
 * directions of selection sync. Each row's text is {@link
 * Segment#toString()}, a faithful port of {@code Segment::ToString}, the
 * exact same single-string-per-segment format {@code
 * MainSegment::UpdateList}'s own {@code segmentListWindow->AddSegment(
 * segment->ToString())} call builds the real Win32 {@code ListBox} from -
 * an earlier version of this class instead showed a {@link javax.swing.JTable}
 * with separate Title/Header/Begin/End/Size/Binary columns, which has no
 * C++ counterpart at all ({@code Segment::ToString} has no title in it
 * either - segment titles are shown nowhere in the C++ segment list).
 * <p>
 * One deliberate departure from the C++ source: {@code
 * SegmentListWindow.cpp} creates a plain single-selection Win32 {@code
 * ListBox} ({@code LB_GETCURSEL}/{@code LB_SETCURSEL}, no {@code
 * LBS_MULTIPLESEL}/{@code LBS_EXTENDEDSEL}), and {@code
 * SegmentList::DeleteSelectedSegment}/{@link
 * com.wudsn.tools.dis6502.model.SegmentList#deleteSelectedSegment} only
 * ever removes that one segment - there is no multi-segment delete in the
 * original at all. This port's {@link #list} uses {@link
 * ListSelectionModel#MULTIPLE_INTERVAL_SELECTION} instead, letting {@link
 * #deleteMenuItem} remove every selected segment in one step via {@link
 * #getSelectedSegmentIndices}/{@link
 * com.wudsn.tools.dis6502.model.SegmentList#deleteSegments} - every other
 * command ({@link #moveUpMenuItem}/{@link #moveDownMenuItem}/{@link
 * #saveNoHeaderMenuItem}/{@link #saveHeaderMenuItem}/{@link
 * #propertiesMenuItem}) is inherently single-segment and stays gated on
 * exactly one selected row (see {@link #updatePopupMenuState}), and {@link
 * #mergeMenuItem} is untouched - {@code SegmentList::MergeSegments}/{@link
 * com.wudsn.tools.dis6502.model.SegmentList#mergeSegments} was already a
 * whole-list sweep for adjacent, compatible segments that never looked at
 * the selection to begin with.
 * <p>
 * {@link #updatePopupMenuState} is ported
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
 * in C++ this happens automatically via {@code WM_SETFONT}. This list only
 * ever shows already-formatted metadata text, not raw byte values, so it
 * needs none of {@link ComputerFont}'s byte-indexed glyph lookup - but it
 * still cannot just be {@code list.setFont(...)} plus {@link JList}'s
 * default renderer: on real screen output (unlike the offscreen renders
 * used to develop this font support), Windows applies its own ClearType/
 * subpixel text antialiasing to ordinary Swing text painting, which blurs
 * this small pixel-art font into illegible dots. Every other {@code
 * ComputerFont}-driven panel avoids this because {@link
 * ComputerFont#drawText} explicitly disables antialiasing before drawing;
 * {@link ComputerFontListCellRenderer} (shared with {@link XRefPanel})
 * gives this list the same explicit control by painting cell text through
 * {@code drawText} itself instead of relying on the default renderer's
 * {@code g.drawString}.
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

	/** Ported from PartWindow::ApplyLayout's SetFont(partLayout->GetLayout()->GetFont()) - call whenever the workspace's computer system or double-height setting changes. */
	public void setComputerFont(ComputerFont computerFont) {
		list.setFont(computerFont.getAwtFont());
		cellRenderer.setComputerFont(computerFont);
		header.setComputerFont(computerFont);
	}

	/**
	 * The currently loaded file's name, shown in {@link #header} - ported
	 * from {@code Main::PaintMainWindow}'s {@code
	 * std::filesystem::path(binPath).filename()}. Call whenever {@code
	 * Dis6502}'s own current-file state changes; pass {@code null} once
	 * there is none.
	 */
	public void setFileName(String fileName) {
		this.fileName = fileName == null ? "" : fileName;
		updateHeaderText();
	}

	/** Ported from Main::PaintMainWindow's segment list title block (IDS_SEGMENT_TITLE/IDS_SEGMENT_TITLE_NO_SEGMENTS_LOADED). */
	private void updateHeaderText() {
		boolean empty = workspace == null || workspace.getSegmentList().isEmpty();
		header.setText(empty ? Text.IDS_SEGMENT_TITLE_NO_SEGMENTS_LOADED : TextUtility.format(Text.IDS_SEGMENT_TITLE, fileName));
	}

	/** Ported from MainSegment::RButtonDownProc (the "no edit mode" branch - there is no memory inspector edit mode to check here yet). */
	private void maybeShowPopup(MouseEvent e) {
		if (!e.isPopupTrigger() || workspace == null || workspace.getSegmentList().isEmpty()) {
			return;
		}
		updatePopupMenuState();
		popupMenu.show(list, e.getX(), e.getY());
	}

	/**
	 * Ported from SegmentListPopupMenu::Update, with one deliberate
	 * departure: {@link #deleteMenuItem} is enabled for one or more selected
	 * rows (see this class's own javadoc for the multi-selection this
	 * enables), while every other item stays gated on exactly one selected
	 * row, since Move Up/Down, Save (with/without header) and Properties are
	 * inherently single-segment operations - unchanged from C++, which never
	 * had more than one row to consider in the first place.
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

	/** Ported from MainSegment::UpdateList. */
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
	 * Ported from MainSegment::Selected - {@code
	 * list.getSelectedIndex()} (the smallest selected index, by {@link
	 * JList}'s own contract) still drives {@link Workspace#getSegmentList()}'s
	 * single {@code selectedIndex} with multi-selection enabled, so the
	 * memory inspector/Properties/Save Segment/Move Up/Down - every
	 * inherently single-segment concept in this port - keep tracking one
	 * well-defined "current" segment exactly as before, regardless of how
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
