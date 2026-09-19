/*
 * Copyright (C) 2025 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.ui;

import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;

import com.wudsn.tools.dis6502.model.FileHeader;
import com.wudsn.tools.dis6502.model.GuessCodeLogic;
import com.wudsn.tools.dis6502.model.MemoryInspectorSelection;
import com.wudsn.tools.dis6502.model.MemoryType;
import com.wudsn.tools.dis6502.model.Segment;
import com.wudsn.tools.dis6502.model.SegmentList;

/**
 * A read-only hex/ASCII dump of the currently selected segment, with a
 * highlighted byte-range selection.
 * <p>
 * Ported from ui/MemoryInspectorWindow.h/.cpp and the display/selection/ find
 * parts of ui/MemoryInspector.h/.cpp - {@link #segmentChanged} from
 * {@code MemoryInspector::SegmentChanged},
 * {@link #select}/{@link #clearSelection} from
 * {@code MemoryInspector::Select}/{@code
 * ClearSelection}, {@link #setDisplayAsScreenCode} from {@code
 * MemoryInspector::ToggleDisplayAsScreenCode}/{@code
 * MemoryInspectorControlImpl::SetInternal} - the ASCII column's byte-to-
 * character transform for "internal" (Atari ANTIC screen code) mode is copied
 * verbatim from {@code MemoryInspectorControlImpl.cpp}'s paint routine, the one
 * piece of that routine's rendering this class replicates.
 * {@link #beginByteSelection}/{@link #extendByteSelection} port
 * {@code MemoryInspectorControlImpl::LButtonDown}/{@code
 * SetEndOfSelection} (via {@link MemoryInspectorGridPanel#offsetAtPoint}),
 * letting the user click or drag in the grid to select a byte range directly,
 * on top of every other way {@link #select} is already reached (Find, Select
 * All, Select Sprites, XRef navigation, a Split at Selection result).
 * {@link #findString}/{@link #findNextString}/{@link #canFind} from
 * {@code MemoryInspector::FindString}/{@code
 * FindNextString}/{@code CanFind}, triggered from {@link #findMenuItem}/
 * {@link #findNextMenuItem}.
 * <p>
 * This panel has no toolbar - every command is reachable only from the
 * right-click popup menu ({@link #maybeShowPopup}, attached to the grid),
 * ported from ui/MemoryInspectorPopupMenu.h/.cpp's {@code
 * MEMORY_INSPECTOR_POPUP_MENU} resource and following
 * {@link SegmentListPanel}'s pattern: every item is a public {@code JMenuItem}
 * field wired directly by {@code Dis6502}, never a hidden {@code JButton} kept
 * around only to be {@code doClick()}d - this panel used to also have a toolbar
 * with a button per command, each popup item {@code
 * doClick()}ing its button, until both the redundant toolbar and that
 * indirection were removed. {@link #splitAtSelectionMenuItem} (from
 * {@code MemoryInspector::SplitAtSelection}/ IDM_DUMP_SPLIT_AT_SELECTION) only
 * splits the segment list the same way
 * {@code com.wudsn.tools.dis6502.ui.SegmentListPanel}'s Move Up/Down/
 * Merge/Delete already do, without reinterpreting any byte's type, and
 * {@link #selectAll}/{@link #selectNextUnknownBlock}/{@link #saveSelectionNoHeaderMenuItem}/{@link #saveSelectionHeaderMenuItem}
 * (from {@code MemoryInspector::SelectAll}/{@code SelectNextUnknownBlock} and
 * {@code MainMemoryInspector::SaveWithoutHeader}/{@code SaveWithHeader}) are
 * non-mutating too - selecting, and writing out, bytes that already exist.
 * {@link #setType}/{@link #setUnknownBlockToByte} (from {@code
 * MemoryInspector::SetType}/{@code SetUnknownBlockToByte}) do mutate a
 * segment's understanding of its bytes' types, and are the first such commands
 * ported here - unlike the rest of this class, callers must re-run the
 * disassembly afterward (see their own javadoc). {@link #copySelectionMenuItem}
 * (from {@code MemoryInspector::CopySelection}) copies the selection as a plain
 * hex string, matching {@code
 * DatatypeUtility::ByteArrayToHexString(..., false)}'s format.
 * <p>
 * TODO: Delete/Cut/Paste Selection are deliberately NOT ported: {@code
 * MemoryInspector::DeleteSelection} never actually shrinks the segment's
 * underlying byte/type arrays (its own comment admits as much, and now carries
 * a second TODO added while porting this, documenting the knock-on effect
 * below), which also means its "delete the whole segment if it's now empty"
 * branch can't work, since {@code
 * Segment::IsEmpty}/{@link Segment#isEmpty} check that same never- shrunk
 * allocation; and {@code MemoryInspector::PasteAtSelection} is explicitly
 * broken in the C++ source (its own comment says so, and the code that would
 * apply the newly-built buffer back to the segment is commented out - also now
 * flagged there with a porting-context TODO). Porting either faithfully would
 * just carry the same brokenness forward, and fixing them needs real
 * segment-buffer resizing, which does not exist in
 * {@link com.wudsn.tools.dis6502.model.MemoryBlock} yet.
 * <p>
 * {@link #selectSpritesMenuItem} (from {@code
 * MemoryInspector::ShowSelectSpritesDialog}/IDM_DUMP_SELECT_SPRITES, ported as
 * {@link SelectSpritesDialog}/{@link SpritePanel}/{@link SpriteMode}) is
 * non-mutating like the other Select* items - it just ends in a call to
 * {@link #select}, {@link SelectSpritesDialog}'s own javadoc has the details.
 * {@link #editCommentMenuItem} (from {@code MemoryInspector::AddComment}/
 * IDM_DUMP_EDIT_COMMENT, ported as {@link CommentDialog}) is wired only from
 * here rather than also from a plain disassembly-line click with no byte
 * selection, the C++ version's other trigger path - see {@link CommentDialog}'s
 * javadoc. {@link #assembleMenuItem} (from {@code
 * MemoryInspector::Assemble}/IDM_DUMP_ASSEMBLE, ported as
 * {@link AssembleDialog}) is this port's one piece of direct byte-level editing
 * - not raw hex digit entry (there is no grid cell to type into), but typing
 * 6502 instructions to assemble in place, which is the C++ version's own
 * primary editing tool for binary segments; see {@link AssembleDialog}'s
 * javadoc for a dialog-closing bug found and fixed while porting it.
 * {@link #startCodeTraceMenuItem} (from {@code
 * MemoryInspector::Guess}/IDM_DUMP_START_CODE_TRACE) runs
 * {@link GuessCodeLogic}, a large enough, UI-independent enough piece of logic
 * to get its own model-layer class instead of living directly here - see that
 * class's javadoc for what it does and a stale-reference issue found (but only
 * fixed in this port, not the C++ source, which needs an interactive GUI run to
 * confirm) while porting it. The hex dump itself is
 * {@link MemoryInspectorGridPanel}, a custom-painted, read-only grid using the
 * real per-computer-system bitmap glyphs from {@link ComputerFont} - see that
 * class's javadoc for why a real font asset is needed at all (a plain Java font
 * cannot display ATASCII/PETSCII characters) and
 * {@link MemoryInspectorGridPanel}'s own javadoc for exactly which parts of
 * {@code MemoryInspectorControlImpl.cpp}'s custom control this replicates (the
 * paint routine) and which it does not (mouse-drag selection, in-place editing
 * - neither existed in this port before this rewrite either).
 * {@link #setComputerFont} must be called by {@code Dis6502} whenever the
 * workspace's computer system or double- font-height setting changes, matching
 * {@code
 * MemoryInspectorWindow}'s use of {@code WorkspaceFont::GetResizedFont}.
 * <p>
 * The Change Type submenu is the one popup item that is not a single
 * {@code JMenuItem} field: with thirteen items, each already knowing exactly
 * which {@link MemoryType} it means, a public field per item (or worse, a
 * shared field driven by some now-deleted toolbar combo box) would be backwards
 * - instead its items report the chosen type via
 * {@link #setTypeSelectionListener}, matching {@link XRefPanel}'s {@code
 * XRefSelectionListener} pattern. The submenu's checkmarks are ported from
 * {@code TypeSubMenu::Update} - which type(s) are actually present across the
 * selection, including its LOBYTE/HIBYTE-adjacency lookback for a byte whose
 * own stored type is unknown/invalid. "Edit bytes at selection", Cut, Paste
 * (before/after selection), and Delete are not in this menu, matching this
 * class's own note above on why Delete/Cut/ Paste Selection are not ported,
 * plus the general absence of any in-place hex-editing mode in this port (see
 * {@link MemoryInspectorGridPanel}'s javadoc).
 *
 * @author Peter Dell
 */
public final class MemoryInspectorPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	/**
	 * Ported from the type submenu's entries in MEMORY_INSPECTOR_POPUP_MENU, in
	 * their .rc order.
	 */
	private static final MemoryType[] TYPE_SUBMENU_ORDER = { MemoryType.CODE, MemoryType.LOBYTE, MemoryType.HIBYTE,
			MemoryType.BYTE, MemoryType.WORD, MemoryType.LABEL, MemoryType.SYMBOL, MemoryType.FIXUP, MemoryType.STRING,
			MemoryType.SBYTE, MemoryType.DLIST, MemoryType.STORE, MemoryType.UNKNOWN };
	private static final String[] TYPE_SUBMENU_LABELS = { "Code", "Code with Low Byte", "Code with High Byte", "Byte",
			"Word", "Label", "SpartaDos X Label", "SpartaDos X Address Fix-Up", "String", "Screen Byte", "Display List",
			"Data Store", "Unknown" };

	private final TitledBorder titledBorder = BorderFactory.createTitledBorder("No segment selected.");
	private final MemoryInspectorGridPanel grid = new MemoryInspectorGridPanel();

	/**
	 * Every popup menu item is a public field wired directly by {@code
	 * Dis6502}, the same way {@link DisassemblyPanel}'s label-navigation popup
	 * items are - a hidden {@code JButton} kept around only to be
	 * {@code doClick()}d would be a pointless layer of indirection. The Change Type
	 * submenu is the one exception: with thirteen items, each already knowing its
	 * own type, a public field per item would be backwards - its items report the
	 * type the user picked through {@link #setTypeSelectionListener} instead.
	 */
	public final JMenuItem findMenuItem = new JMenuItem("Find...");
	public final JMenuItem findNextMenuItem = new JMenuItem("Find next");
	public final JMenuItem splitAtSelectionMenuItem = new JMenuItem("Split at selection");
	public final JMenuItem startCodeTraceMenuItem = new JMenuItem("Start code trace at selection");
	public final JMenuItem setUnknownBlockToByteMenuItem = new JMenuItem("Set current block of Unknown type to Byte");
	public final JMenuItem editCommentMenuItem = new JMenuItem("Add/Edit comment...");
	public final JMenuItem assembleMenuItem = new JMenuItem("Assemble at selection...");
	public final JMenuItem copySelectionMenuItem = new JMenuItem("Copy");
	public final JMenuItem selectNextUnknownBlockMenuItem = new JMenuItem("Select next block of Unknown type");
	public final JMenuItem selectSpritesMenuItem = new JMenuItem("Select Sprites...");
	public final JMenuItem selectAllMenuItem = new JMenuItem("Select all");
	public final JMenuItem saveSelectionNoHeaderMenuItem = new JMenuItem("Save selection without header...");
	public final JMenuItem saveSelectionHeaderMenuItem = new JMenuItem("Save selection with header...");

	private final JPopupMenu popupMenu = new JPopupMenu();
	private final JCheckBoxMenuItem[] typeMenuItems = new JCheckBoxMenuItem[TYPE_SUBMENU_ORDER.length];
	private TypeSelectionListener typeSelectionListener;
	private SelectionChangedListener selectionChangedListener;

	private MemoryInspectorSelection memoryInspectorSelection;
	private int selectionAnchorOffset = -1;

	private int findSegmentIndex = SegmentList.NO_SEGMENT_INDEX;
	private int findOffset;
	private int findSize;
	private boolean findAllSegments = true;
	private String findText = "";

	public MemoryInspectorPanel() {
		super(new BorderLayout());
		setBorder(titledBorder);

		add(new JScrollPane(grid), BorderLayout.CENTER);

		buildPopupMenu();
		MouseAdapter mouseHandler = new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				maybeShowPopup(e);
				if (SwingUtilities.isLeftMouseButton(e) && !e.isPopupTrigger()) {
					beginByteSelection(e);
				}
			}

			@Override
			public void mouseDragged(MouseEvent e) {
				if (SwingUtilities.isLeftMouseButton(e)) {
					extendByteSelection(e);
				}
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				maybeShowPopup(e);
				if (SwingUtilities.isLeftMouseButton(e) && selectionAnchorOffset >= 0) {
					selectionAnchorOffset = -1;
					if (selectionChangedListener != null) {
						selectionChangedListener.onSelectionChanged();
					}
				}
			}
		};
		grid.addMouseListener(mouseHandler);
		grid.addMouseMotionListener(mouseHandler);

		updatePopupMenuItemsState();
	}

	/**
	 * Ported from MemoryInspectorControlImpl::LButtonDown's non-edit-mode branch:
	 * starts a new selection at the clicked byte.
	 */
	private void beginByteSelection(MouseEvent e) {
		int offset = grid.offsetAtPoint(e.getX(), e.getY());
		if (offset < 0) {
			return;
		}
		selectionAnchorOffset = offset;
		select(offset, offset);
	}

	/**
	 * Ported from MemoryInspectorControlImpl::SetEndOfSelection's non-edit- mode
	 * branch: extends the selection from the byte clicked in
	 * {@link #beginByteSelection} to the point currently under the cursor. Swing
	 * only delivers {@code mouseDragged} to the component that received the
	 * matching {@code mousePressed} while the button stays down, the same effect
	 * the C++ source gets from {@code SetCapture}/{@code
	 * bMemoryInspectorCapture} - so unlike the C++ source, no explicit capture flag
	 * is needed here.
	 */
	private void extendByteSelection(MouseEvent e) {
		if (selectionAnchorOffset < 0) {
			return;
		}
		int offset = grid.offsetAtPoint(e.getX(), e.getY());
		if (offset < 0) {
			return;
		}
		select(selectionAnchorOffset, offset);
	}

	private void buildPopupMenu() {
		popupMenu.add(startCodeTraceMenuItem);

		JMenu changeTypeMenu = new JMenu("Change type of selected bytes to");
		for (int i = 0; i < TYPE_SUBMENU_ORDER.length; i++) {
			MemoryType type = TYPE_SUBMENU_ORDER[i];
			JCheckBoxMenuItem item = new JCheckBoxMenuItem(TYPE_SUBMENU_LABELS[i]);
			item.addActionListener(e -> {
				if (typeSelectionListener != null) {
					typeSelectionListener.onTypeSelected(type);
				}
			});
			typeMenuItems[i] = item;
			if (type == MemoryType.UNKNOWN) {
				changeTypeMenu.addSeparator();
			}
			changeTypeMenu.add(item);
		}
		popupMenu.add(changeTypeMenu);

		popupMenu.addSeparator();
		popupMenu.add(setUnknownBlockToByteMenuItem);

		popupMenu.addSeparator();
		popupMenu.add(editCommentMenuItem);
		popupMenu.add(assembleMenuItem);
		popupMenu.add(copySelectionMenuItem);
		popupMenu.add(splitAtSelectionMenuItem);

		popupMenu.addSeparator();
		popupMenu.add(findMenuItem);
		popupMenu.add(findNextMenuItem);

		popupMenu.addSeparator();
		popupMenu.add(selectNextUnknownBlockMenuItem);
		popupMenu.add(selectSpritesMenuItem);
		popupMenu.add(selectAllMenuItem);

		popupMenu.addSeparator();
		popupMenu.add(saveSelectionNoHeaderMenuItem);
		popupMenu.add(saveSelectionHeaderMenuItem);
	}

	/**
	 * Ported from MemoryInspectorControlImpl::RButtonDown's notification, handled
	 * by MainMemoryInspector to show MemoryInspectorPopupMenu.
	 */
	private void maybeShowPopup(MouseEvent e) {
		if (!e.isPopupTrigger() || memoryInspectorSelection == null || !memoryInspectorSelection.hasSegment()) {
			return;
		}
		updatePopupMenuItemsState();
		syncPopupMenuState();
		popupMenu.show(grid, e.getX(), e.getY());
	}

	/**
	 * Ports {@code TypeSubMenu::Update}'s enabled/checked logic for the Set Type
	 * submenu - every other popup item's enabled state is set directly in
	 * {@link #updatePopupMenuItemsState}.
	 */
	private void syncPopupMenuState() {
		boolean hasSelection = memoryInspectorSelection != null && memoryInspectorSelection.hasSelection();
		boolean[] present = new boolean[TYPE_SUBMENU_ORDER.length];
		if (hasSelection) {
			Segment segment = memoryInspectorSelection.getSegment();
			int begin = memoryInspectorSelection.getBegin();
			int end = memoryInspectorSelection.getEnd();
			for (int offset = begin; offset <= end; offset++) {
				MemoryType type = segment.getType(offset);
				if (type == MemoryType.UNKNOWN) {
					if (offset == 0) {
						type = MemoryType.UNKNOWN;
					} else if (segment.isType(offset - 1, MemoryType.LOBYTE)) {
						type = MemoryType.LOBYTE;
					} else if (segment.isType(offset - 1, MemoryType.HIBYTE)) {
						type = MemoryType.HIBYTE;
					} else {
						type = MemoryType.UNKNOWN;
					}
				}
				for (int i = 0; i < TYPE_SUBMENU_ORDER.length; i++) {
					if (TYPE_SUBMENU_ORDER[i] == type) {
						present[i] = true;
					}
				}
			}
		}
		for (int i = 0; i < TYPE_SUBMENU_ORDER.length; i++) {
			boolean enabled = hasSelection;
			if (enabled && (TYPE_SUBMENU_ORDER[i] == MemoryType.LOBYTE || TYPE_SUBMENU_ORDER[i] == MemoryType.HIBYTE)) {
				enabled = memoryInspectorSelection.getBegin() > 0;
			}
			typeMenuItems[i].setEnabled(enabled);
			typeMenuItems[i].setState(present[i]);
		}
	}

	/**
	 * Ported from MemoryInspectorWindow's use of WorkspaceFont::GetResizedFont -
	 * call whenever the workspace's computer system or double-height setting
	 * changes.
	 */
	public void setComputerFont(ComputerFont computerFont) {
		grid.setComputerFont(computerFont);
	}

	/**
	 * Reports which type the user picked from the popup menu's Change Type submenu
	 * - see that field's comment on why this needs a listener instead of a public
	 * field per type.
	 */
	public void setTypeSelectionListener(TypeSelectionListener typeSelectionListener) {
		this.typeSelectionListener = typeSelectionListener;
	}

	/**
	 * Reports that a mouse-driven byte selection just finished - ported from
	 * {@code MemoryInspectorControlImpl::LButtonUp}'s {@code SELECTION_CHANGED}
	 * notification, which fires only once the mouse button is released, not on
	 * every intermediate {@code
	 * MouseMove} while dragging. {@code Dis6502} uses this to sync the disassembly
	 * listing to the selection, matching {@code
	 * MemoryInspector::SelectionChanged}.
	 */
	public void setSelectionChangedListener(SelectionChangedListener selectionChangedListener) {
		this.selectionChangedListener = selectionChangedListener;
	}

	/**
	 * Ported from MemoryInspector::SegmentChanged. Some segments (an SDX
	 * symbol-table header, or an SDX relocation block with no data of its own) have
	 * nothing to display, matching the C++ version's {@code
	 * hasData} check - the title is still shown for these (the segment itself is
	 * genuinely selected, just not byte-displayable), matching
	 * {@link #updateTitle}'s own C++ source, which only special-cases a null
	 * segment, not this narrower {@code hasData} condition.
	 */
	public void segmentChanged(MemoryInspectorSelection memoryInspectorSelection) {
		this.memoryInspectorSelection = memoryInspectorSelection;
		Segment segment = memoryInspectorSelection.getSegment();
		boolean hasData = segment != null && !segment.isHeader(FileHeader.SDX_SYM_DEFINED)
				&& !segment.isSDXRelocBlkWithoutData();

		grid.setSegment(hasData ? segment : null);
		memoryInspectorSelection.clearSelection();
		updateTitle();
		updatePopupMenuItemsState();
		revalidate();
		repaint();
	}

	/**
	 * Ported from {@code Main}'s {@code WM_PAINT} handler's "Print memory inspector
	 * window title" block (IDS_DUMP_TITLE_SEGMENT/{@code
	 * IDS_DUMP_TITLE_SELECTION}/{@code
	 * IDS_DUMP_TITLE_SEGMENT_NO_SEGMENT_SELECTED}): the selected segment's 1-based
	 * number, address range and size in hex and decimal - or, once a byte range
	 * within it is marked, the same for that selection instead (absolute addresses,
	 * {@code segment.wBegin}-relative). Unlike the C++ source, which repaints this
	 * from scratch on every {@code WM_PAINT} using whatever the selection happens
	 * to be at that moment, this is called explicitly wherever the segment or
	 * selection changes ({@link #segmentChanged}, {@link #select},
	 * {@link #clearSelection}).
	 */
	private void updateTitle() {
		Segment segment = memoryInspectorSelection == null ? null : memoryInspectorSelection.getSegment();
		if (segment == null) {
			titledBorder.setTitle("No segment selected.");
			return;
		}

		int segmentNumber = memoryInspectorSelection.getSegmentIndex() + 1;
		String prefix;
		int begin;
		int end;
		int size;
		String title;
		if (memoryInspectorSelection.hasSelection()) {
			prefix = "Selection";
			begin = segment.wBegin + memoryInspectorSelection.getBegin();
			end = segment.wBegin + memoryInspectorSelection.getEnd();
			size = memoryInspectorSelection.getSize();
			title = String.format("%s: $%04X-$%04X:$%04X / %d", prefix, begin, end, size, size);
		} else {
			prefix = "Segment";
			begin = segment.wBegin;
			end = segment.wEnd;
			size = segment.getSize();
			title = String.format("%s %d: $%04X-$%04X:$%04X / %d", prefix, segmentNumber, begin, end, size, size);
		}
		titledBorder.setTitle(title);
	}

	/**
	 * Ported from MemoryInspector::ToggleDisplayAsScreenCode/
	 * MemoryInspectorControlImpl::SetInternal. Switches the ASCII column between
	 * plain byte values and their Atari internal (ANTIC screen code) equivalent,
	 * re-rendering the currently displayed segment if there is one.
	 */
	public void setDisplayAsScreenCode(boolean displayAsScreenCode) {
		grid.setDisplayAsScreenCode(displayAsScreenCode);
	}

	/** Ported from MemoryInspector::Select. */
	public void select(int begin, int end) {
		if (memoryInspectorSelection == null || memoryInspectorSelection.getSegment() == null
				|| memoryInspectorSelection.getSegment().isEmpty()) {
			return;
		}
		memoryInspectorSelection.setSelection(begin, end);
		highlightRange(memoryInspectorSelection.getBegin(), memoryInspectorSelection.getEnd());
		updateTitle();
		updatePopupMenuItemsState();
		// highlightRange only repaints the grid, a child component - the
		// titled border's text is painted by this panel itself, so it needs
		// its own repaint to actually show the new title on screen.
		repaint();
	}

	/** Ported from MemoryInspector::ClearSelection. */
	public void clearSelection() {
		if (memoryInspectorSelection != null) {
			memoryInspectorSelection.clearSelection();
		}
		clearHighlight();
		updateTitle();
		updatePopupMenuItemsState();
		repaint();
	}

	/**
	 * Ported from MemoryInspector::SelectAll: selects the whole segment.
	 * {@code end} is passed as the segment's size (one past the last valid offset),
	 * matching the C++ version -
	 * {@link #select}/{@link MemoryInspectorSelection#setSelection} clamp it back
	 * down to the last valid offset, the same way the C++ version's own
	 * {@code Select}/ {@code MemoryInspectorSelection::SetSelection} do.
	 */
	public void selectAll() {
		if (memoryInspectorSelection == null || memoryInspectorSelection.getSegment() == null
				|| memoryInspectorSelection.getSegment().isEmpty()) {
			return;
		}
		select(0, memoryInspectorSelection.getSegment().getSize());
	}

	/**
	 * Ported from MemoryInspector::SelectNextUnknownBlock: scans forward from just
	 * after the current selection (or the segment's start, if there is none) for
	 * the next run of bytes with an unrecognized type, across this segment and
	 * every later one in the segment list - never wrapping back around to earlier
	 * segments/offsets, matching the C++ version, which simply stops (with nothing
	 * selected) once the segment list is exhausted.
	 */
	public void selectNextUnknownBlock() {
		if (memoryInspectorSelection == null || memoryInspectorSelection.getSegment() == null
				|| memoryInspectorSelection.getSegment().isEmpty() || !memoryInspectorSelection.getSegment().bBinary) {
			return;
		}

		int last = memoryInspectorSelection.hasSelection() ? memoryInspectorSelection.getBegin() + 1 : 0;

		SegmentList segmentList = memoryInspectorSelection.getWorkspace().getSegmentList();
		int count = segmentList.getCount();
		for (int segmentIndex = memoryInspectorSelection.getSegmentIndex(); segmentIndex < count; segmentIndex++) {
			Segment segment = segmentList.getSegment(segmentIndex);
			int end = segment.wEnd - segment.wBegin;

			for (int offset = last; offset <= end; offset++) {
				if (segment.isUnknown(offset)) {
					if (memoryInspectorSelection.getSegmentIndex() != segmentIndex) {
						memoryInspectorSelection.setSegmentIndex(segmentIndex);
						segmentList.setSelectedIndex(segmentIndex);
					}

					int begin = offset;
					last = begin;
					for (offset = begin + 1; offset <= end && segment.isUnknown(offset); offset++) {
						last = offset;
					}

					select(begin, last);
					return;
				}
			}

			last = 0;
		}
	}

	/**
	 * Ported from the non-LOBYTE/HIBYTE branch of {@code
	 * MemoryInspector::SetType} - the LOBYTE/HIBYTE case needs a dialog
	 * ({@link LowHighByteDialog}) to ask for the other, unknown half of the
	 * "assumed word", and stricter validation (a single-byte selection, not at
	 * offset 0, on an immediate-mode instruction's operand), so {@code Dis6502}
	 * handles that case directly instead of calling this method - see
	 * {@code Dis6502#performSetMemoryInspectorType}. The caller is responsible for
	 * re-running the disassembly afterward (matching {@code Refresh()}'s
	 * {@code UpdateDisassembly} call), since this panel does not trigger that
	 * itself - see {@link DisassemblyPanel}.
	 */
	public void setType(MemoryType type) {
		if (memoryInspectorSelection == null || !memoryInspectorSelection.hasSelection()) {
			return;
		}
		Segment segment = memoryInspectorSelection.getSegment();
		if (!segment.bBinary) {
			return;
		}
		int begin = memoryInspectorSelection.getBegin();
		int size = memoryInspectorSelection.getSize();

		if (begin > 0) {
			if (segment.isType(begin - 1, MemoryType.LOBYTE) || segment.isType(begin - 1, MemoryType.HIBYTE)) {
				segment.setType(begin - 1, MemoryType.CODE);
			}
			if ((segment.isType(begin + size - 1, MemoryType.LOBYTE)
					|| segment.isType(begin + size - 1, MemoryType.HIBYTE)) && begin + size < segment.getSize()) {
				segment.setType(begin + size, MemoryType.CODE);
			}
		}
		segment.setType(begin, type, size);
	}

	/**
	 * Ported from MemoryInspector::SetUnknownBlockToByte
	 * (IDM_DUMP_SET_UNKNOWN_BLOCK_TO_BYTE): reclassifies every byte in the
	 * selection that is still {@link MemoryType#UNKNOWN} - and not the repurposed
	 * type slot right after a {@link MemoryType#LOBYTE}/{@link MemoryType#HIBYTE}
	 * byte (see {@link MemoryType}'s javadoc) - as {@link MemoryType#BYTE}. Like
	 * {@link #setType}, the caller is responsible for re-running the disassembly
	 * afterward.
	 */
	public void setUnknownBlockToByte() {
		if (memoryInspectorSelection == null || !memoryInspectorSelection.hasSelection()) {
			return;
		}
		Segment segment = memoryInspectorSelection.getSegment();
		if (!segment.bBinary) {
			return;
		}
		int begin = memoryInspectorSelection.getBegin();
		int size = memoryInspectorSelection.getSize();

		for (int offset = begin; offset < begin + size; offset++) {
			if (segment.isType(offset, MemoryType.UNKNOWN)
					&& (offset == 0 || (!segment.isType(offset - 1, MemoryType.LOBYTE)
							&& !segment.isType(offset - 1, MemoryType.HIBYTE)))) {
				segment.setType(offset, MemoryType.BYTE);
			}
		}
	}

	/**
	 * Ported from MemoryInspector::Guess (IDM_DUMP_START_CODE_TRACE): runs
	 * {@link GuessCodeLogic} starting from the selection's first byte. Like
	 * {@link #setType}/{@link #setUnknownBlockToByte}, the caller is responsible
	 * for re-running the disassembly afterward.
	 */
	public void guess() {
		if (memoryInspectorSelection == null || !memoryInspectorSelection.hasSelection()) {
			return;
		}
		new GuessCodeLogic(memoryInspectorSelection.getWorkspace()).guess(memoryInspectorSelection.getSegment(),
				memoryInspectorSelection.getBegin());
	}

	/**
	 * Ported from the enablement logic in MemoryInspectorPopupMenu::Update for
	 * IDM_DUMP_FIND/IDM_DUMP_FIND_NEXT/IDM_DUMP_SELECT_ALL/
	 * IDM_DUMP_SELECT_NEXT_UNKNOWN_BLOCK/IDM_DUMP_SAVE_NO_HEADER/
	 * IDM_DUMP_SAVE_HEADER/IDM_DUMP_SPLIT_AT_SELECTION/
	 * IDM_DUMP_SET_UNKNOWN_BLOCK_TO_BYTE and the type submenu's enablement.
	 */
	private void updatePopupMenuItemsState() {
		findMenuItem.setEnabled(canFind(true));
		findNextMenuItem.setEnabled(canFind(false));

		boolean hasSegment = memoryInspectorSelection != null && memoryInspectorSelection.hasSegment()
				&& !memoryInspectorSelection.getSegment().isEmpty();
		selectAllMenuItem.setEnabled(hasSegment);
		selectNextUnknownBlockMenuItem.setEnabled(hasSegment);
		selectSpritesMenuItem.setEnabled(hasSegment);

		boolean hasSelection = memoryInspectorSelection != null && memoryInspectorSelection.hasSelection();
		saveSelectionNoHeaderMenuItem.setEnabled(hasSelection);
		saveSelectionHeaderMenuItem.setEnabled(hasSelection);
		setUnknownBlockToByteMenuItem.setEnabled(hasSelection);
		copySelectionMenuItem.setEnabled(hasSelection);
		editCommentMenuItem.setEnabled(hasSelection);
		assembleMenuItem.setEnabled(hasSelection);
		startCodeTraceMenuItem.setEnabled(hasSelection && memoryInspectorSelection.getSegment()
				.isType(memoryInspectorSelection.getBegin(), MemoryType.UNKNOWN));
		splitAtSelectionMenuItem.setEnabled(hasSelection
				&& memoryInspectorSelection.getWorkspace().getSegmentList().getCount() < SegmentList.MAX_SEGMENTS
				&& memoryInspectorSelection.getSegment().canSplitAt(memoryInspectorSelection.getBegin()));
	}

	private void highlightRange(int begin, int end) {
		grid.highlightRange(begin, end);
	}

	private void clearHighlight() {
		grid.clearHighlight();
	}

	/** Ported from MemoryInspector::HasFindString. */
	public boolean hasFindString() {
		return !findText.isEmpty();
	}

	public String getFindString() {
		return findText;
	}

	public boolean isFindAllSegments() {
		return findAllSegments;
	}

	/** Ported from MemoryInspector::CanFind. */
	public boolean canFind(boolean first) {
		if (memoryInspectorSelection == null || memoryInspectorSelection.getSegment() == null) {
			return false;
		}
		if (!first && findSize == 0) {
			return false;
		}
		return true;
	}

	/** Ported from MemoryInspector::FindString. */
	public boolean findString(String findAscii, boolean allSegments) {
		findSegmentIndex = allSegments ? 0 : memoryInspectorSelection.getSegmentIndex();
		findText = findAscii;
		findOffset = 0;
		findSize = findAscii.length();
		findAllSegments = allSegments;
		return findNextString();
	}

	/**
	 * Ported from MemoryInspector::FindNextString, minus the "not found" alert -
	 * unlike the C++ version, which shows it itself via {@code
	 * FindStringDialog::ShowStringNotFoundMessage}, that is left to the caller here
	 * (see {@code Dis6502}), matching how the rest of this class stays free of its
	 * own popups.
	 */
	public boolean findNextString() {
		if (findSegmentIndex != SegmentList.NO_SEGMENT_INDEX) {
			SegmentList segmentList = memoryInspectorSelection.getWorkspace().getSegmentList();
			int count = segmentList.getCount();
			for (int segmentIndex = findSegmentIndex; segmentIndex < count; segmentIndex++) {
				Segment segment = segmentList.getSegment(segmentIndex);

				if (searchString(segmentIndex, segment)) {
					return true; // select(), called by searchString, already updated button state.
				}

				if (!findAllSegments) {
					break;
				}
				findOffset = 0;
			}
		}
		updatePopupMenuItemsState();
		return false;
	}

	/** Ported from MemoryInspector::SearchString. */
	private boolean searchString(int segmentIndex, Segment segment) {
		int size = segment.getSize();
		if (size < findSize) {
			return false;
		}
		for (int offset = findOffset; offset <= size - findSize; offset++) {
			if (matchesAt(segment, offset)) {
				int begin = offset;
				int end = offset + findSize - 1;

				findSegmentIndex = segmentIndex;
				findOffset = offset + 1;

				if (findSegmentIndex != memoryInspectorSelection.getSegmentIndex()) {
					memoryInspectorSelection.getWorkspace().getSegmentList().setSelectedIndex(findSegmentIndex);
				}
				select(begin, end);
				return true;
			}
		}
		return false;
	}

	private boolean matchesAt(Segment segment, int offset) {
		for (int i = 0; i < findSize; i++) {
			if (segment.getData(offset + i) != (findText.charAt(i) & 0xFF)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Reports that a mouse-driven byte selection finished - see
	 * {@link #setSelectionChangedListener}.
	 */
	public interface SelectionChangedListener {
		void onSelectionChanged();
	}

	/**
	 * Reports a type picked from the popup menu's Change Type submenu, matching
	 * {@link XRefPanel.XRefSelectionListener}'s pattern.
	 */
	public interface TypeSelectionListener {
		void onTypeSelected(MemoryType type);
	}
}
