/*
 * Copyright (C) 2025 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
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
 * Ported from ui/MemoryInspectorWindow.h/.cpp and the display/selection/
 * find parts of ui/MemoryInspector.h/.cpp - {@link #segmentChanged} from
 * {@code MemoryInspector::SegmentChanged}, {@link #select}/{@link
 * #clearSelection} from {@code MemoryInspector::Select}/{@code
 * ClearSelection}, {@link #setDisplayAsScreenCode} from {@code
 * MemoryInspector::ToggleDisplayAsScreenCode}/{@code
 * MemoryInspectorControlImpl::SetInternal} - the ASCII column's byte-to-
 * character transform for "internal" (Atari ANTIC screen code) mode is
 * copied verbatim from {@code MemoryInspectorControlImpl.cpp}'s paint
 * routine, the one piece of that routine's rendering this class
 * replicates - and {@link #findString}/{@link #findNextString}/{@link
 * #canFind} from {@code MemoryInspector::FindString}/{@code
 * FindNextString}/{@code CanFind}, triggered from {@link #findMenuItem}/
 * {@link #findNextMenuItem} (public fields wired up by {@code Dis6502}).
 * Unlike this class's other, toolbar-backed popup items, {@link
 * #findMenuItem}, {@link #findNextMenuItem} and {@link
 * #splitAtSelectionMenuItem} have no toolbar button - matching the C++
 * version's popup menu (ui/MemoryInspectorPopupMenu.h/.cpp), they are only
 * reachable from here - so they are public {@code JMenuItem} fields wired
 * directly by {@code Dis6502}, the same way {@link DisassemblyPanel}'s
 * label-navigation popup items are, rather than a hidden {@code JButton}
 * kept around only to be {@code doClick()}d.
 * {@link #splitAtSelectionMenuItem} (from {@code MemoryInspector::SplitAtSelection}/
 * IDM_DUMP_SPLIT_AT_SELECTION) only splits the segment list the same way
 * {@code com.wudsn.tools.dis6502.ui.SegmentListPanel}'s Move Up/Down/
 * Merge/Delete already do, without reinterpreting any byte's type, and
 * {@link #selectAll}/{@link #selectNextUnknownBlock}/{@link
 * #saveSelectionNoHeaderButton}/{@link #saveSelectionHeaderButton} (from
 * {@code MemoryInspector::SelectAll}/{@code SelectNextUnknownBlock} and
 * {@code MainMemoryInspector::SaveWithoutHeader}/{@code SaveWithHeader})
 * are non-mutating too - selecting, and writing out, bytes that already
 * exist. {@link #setType}/{@link #setUnknownBlockToByte} (from {@code
 * MemoryInspector::SetType}/{@code SetUnknownBlockToByte}) do mutate a
 * segment's understanding of its bytes' types, and are the first such
 * commands ported here - unlike the rest of this class, callers must
 * re-run the disassembly afterward (see their own javadoc).
 * {@link #copySelectionButton} (from {@code MemoryInspector::CopySelection})
 * copies the selection as a plain hex string, matching {@code
 * DatatypeUtility::ByteArrayToHexString(..., false)}'s format.
 * <p>
 * TODO: Delete/Cut/Paste Selection are deliberately NOT ported: {@code
 * MemoryInspector::DeleteSelection} never actually shrinks the segment's
 * underlying byte/type arrays (its own comment admits as much, and now
 * carries a second TODO added while porting this, documenting the
 * knock-on effect below), which also means its "delete the whole
 * segment if it's now empty" branch can't work, since {@code
 * Segment::IsEmpty}/{@link Segment#isEmpty} check that same never-
 * shrunk allocation; and {@code MemoryInspector::PasteAtSelection} is
 * explicitly broken in the C++ source (its own comment says so, and the
 * code that would apply the newly-built buffer back to the segment is
 * commented out - also now flagged there with a porting-context TODO).
 * Porting either faithfully would just carry the same brokenness
 * forward, and fixing them needs real segment-buffer resizing, which
 * does not exist in {@link com.wudsn.tools.dis6502.model.MemoryBlock}
 * yet.
 * <p>
 * {@link #selectSpritesButton} (from {@code
 * MemoryInspector::ShowSelectSpritesDialog}/IDM_DUMP_SELECT_SPRITES,
 * ported as {@link SelectSpritesDialog}/{@link SpritePanel}/{@link
 * SpriteMode}) is non-mutating like the other Select* buttons - it just
 * ends in a call to {@link #select}, {@link
 * SelectSpritesDialog}'s own javadoc has the details. {@link
 * #editCommentButton} (from {@code MemoryInspector::AddComment}/
 * IDM_DUMP_EDIT_COMMENT, ported as {@link CommentDialog}) is wired only
 * from here rather than also from a plain disassembly-line click with no
 * byte selection, the C++ version's other trigger path - see {@link
 * CommentDialog}'s javadoc. {@link #assembleButton} (from {@code
 * MemoryInspector::Assemble}/IDM_DUMP_ASSEMBLE, ported as {@link
 * AssembleDialog}) is this port's one piece of direct byte-level
 * editing - not raw hex digit entry (there is no grid cell to type
 * into), but typing 6502 instructions to assemble in place, which is
 * the C++ version's own primary editing tool for binary segments; see
 * {@link AssembleDialog}'s javadoc for a dialog-closing bug found and
 * fixed while porting it. {@link #guessButton} (from {@code
 * MemoryInspector::Guess}/IDM_DUMP_START_CODE_TRACE) runs {@link
 * GuessCodeLogic}, a large enough, UI-independent enough piece of logic
 * to get its own model-layer class instead of living directly here -
 * see that class's javadoc for what it does and a stale-reference issue
 * found (but only fixed in this port, not the C++ source, which needs
 * an interactive GUI run to confirm) while porting it. The hex dump
 * itself is {@link MemoryInspectorGridPanel}, a custom-painted, read-only
 * grid using the real per-computer-system bitmap glyphs from {@link
 * ComputerFont} - see that class's javadoc for why a real font asset is
 * needed at all (a plain Java font cannot display ATASCII/PETSCII
 * characters) and {@link MemoryInspectorGridPanel}'s own javadoc for
 * exactly which parts of {@code MemoryInspectorControlImpl.cpp}'s custom
 * control this replicates (the paint routine) and which it does not
 * (mouse-drag selection, in-place editing - neither existed in this port
 * before this rewrite either). {@link #setComputerFont} must be called by
 * {@code Dis6502} whenever the workspace's computer system or double-
 * font-height setting changes, matching {@code
 * MemoryInspectorWindow}'s use of {@code WorkspaceFont::GetResizedFont}.
 * <p>
 * The right-click popup menu ({@link #maybeShowPopup}, attached to the
 * grid) is ported from ui/MemoryInspectorPopupMenu.h/.cpp's {@code
 * MEMORY_INSPECTOR_POPUP_MENU} resource, following {@link
 * SegmentListPanel}'s pattern - most items here correspond to an already-
 * wired toolbar button/combo box, so those menu items are private and
 * simply {@code doClick()} the matching button (the Set Type submenu's
 * items set {@link #setTypeComboBox} then {@code doClick()} {@link
 * #setTypeButton}) rather than being separate public fields {@code
 * Dis6502} would need to wire up itself; {@link #findMenuItem}/{@link
 * #findNextMenuItem}/{@link #splitAtSelectionMenuItem} are the exception,
 * having no toolbar button to delegate to (see their own field comment).
 * The submenu's checkmarks are
 * ported from {@code TypeSubMenu::Update} - which type(s) are actually
 * present across the selection, including its LOBYTE/HIBYTE-adjacency
 * lookback for a byte whose own stored type is unknown/invalid. "Edit
 * bytes at selection", Cut, Paste (before/after selection), and Delete
 * are not in this menu, matching this class's own note above on why
 * Delete/Cut/Paste Selection are not ported, plus the general absence of
 * any in-place hex-editing mode in this port (see {@link
 * MemoryInspectorGridPanel}'s javadoc).
 *
 * @author Peter Dell
 */
public final class MemoryInspectorPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	public final JButton selectAllButton = new JButton("Select All");
	public final JButton selectNextUnknownBlockButton = new JButton("Select Next Unknown Block");
	public final JButton selectSpritesButton = new JButton("Select Sprites...");
	public final JButton saveSelectionNoHeaderButton = new JButton("Save Selection (No Header)...");
	public final JButton saveSelectionHeaderButton = new JButton("Save Selection (With Header)...");
	public final JComboBox<MemoryType> setTypeComboBox = new JComboBox<>(MemoryType.VALUES);
	public final JButton setTypeButton = new JButton("Set Type");
	public final JButton setUnknownBlockToByteButton = new JButton("Set Unknown Block to Byte");
	public final JButton copySelectionButton = new JButton("Copy Selection");
	public final JButton editCommentButton = new JButton("Comment...");
	public final JButton assembleButton = new JButton("Assemble...");
	public final JButton guessButton = new JButton("Guess Code");

	/** Ported from the type submenu's entries in MEMORY_INSPECTOR_POPUP_MENU, in their .rc order. */
	private static final MemoryType[] TYPE_SUBMENU_ORDER = { MemoryType.CODE, MemoryType.LOBYTE, MemoryType.HIBYTE, MemoryType.BYTE,
			MemoryType.WORD, MemoryType.LABEL, MemoryType.SYMBOL, MemoryType.FIXUP, MemoryType.STRING, MemoryType.SBYTE,
			MemoryType.DLIST, MemoryType.STORE, MemoryType.UNKNOWN };
	private static final String[] TYPE_SUBMENU_LABELS = { "Code", "Code with Low Byte", "Code with High Byte", "Byte", "Word", "Label",
			"SpartaDos X Label", "SpartaDos X Address Fix-Up", "String", "Screen Byte", "Display List", "Data Store", "Unknown" };

	private final TitledBorder titledBorder = BorderFactory.createTitledBorder("Memory Inspector");
	private final MemoryInspectorGridPanel grid = new MemoryInspectorGridPanel();

	/**
	 * Find/Find Next/Split at Selection have no toolbar button - unlike
	 * every other popup item here, which {@code doClick()}s an already-
	 * wired, visible toolbar button, these three are only ever reachable
	 * from this popup menu, so they are public fields wired directly by
	 * {@code Dis6502}, the same way {@link DisassemblyPanel#findButton} is
	 * - a hidden {@code JButton} kept around only to be {@code doClick()}d
	 * would be a pointless layer of indirection.
	 */
	public final JMenuItem findMenuItem = new JMenuItem("Find...");
	public final JMenuItem findNextMenuItem = new JMenuItem("Find next");
	public final JMenuItem splitAtSelectionMenuItem = new JMenuItem("Split at selection");

	private final JPopupMenu popupMenu = new JPopupMenu();
	private final JMenuItem startCodeTraceMenuItem = new JMenuItem("Start code trace at selection");
	private final JCheckBoxMenuItem[] typeMenuItems = new JCheckBoxMenuItem[TYPE_SUBMENU_ORDER.length];
	private final JMenuItem popupSetUnknownBlockToByteMenuItem = new JMenuItem("Set current block of Unknown type to Byte");
	private final JMenuItem popupEditCommentMenuItem = new JMenuItem("Add/Edit comment...");
	private final JMenuItem popupAssembleMenuItem = new JMenuItem("Assemble at selection...");
	private final JMenuItem popupCopySelectionMenuItem = new JMenuItem("Copy");
	private final JMenuItem popupSelectNextUnknownBlockMenuItem = new JMenuItem("Select next block of Unknown type");
	private final JMenuItem popupSelectSpritesMenuItem = new JMenuItem("Select Sprites...");
	private final JMenuItem popupSelectAllMenuItem = new JMenuItem("Select all");
	private final JMenuItem popupSaveSelectionNoHeaderMenuItem = new JMenuItem("Save selection without header...");
	private final JMenuItem popupSaveSelectionHeaderMenuItem = new JMenuItem("Save selection with header...");

	private MemoryInspectorSelection memoryInspectorSelection;

	private int findSegmentIndex = SegmentList.NO_SEGMENT_INDEX;
	private int findOffset;
	private int findSize;
	private boolean findAllSegments = true;
	private String findText = "";

	public MemoryInspectorPanel() {
		super(new BorderLayout());
		setBorder(titledBorder);

		JPanel toolBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
		toolBar.add(selectAllButton);
		toolBar.add(selectNextUnknownBlockButton);
		toolBar.add(selectSpritesButton);
		toolBar.add(saveSelectionNoHeaderButton);
		toolBar.add(saveSelectionHeaderButton);
		toolBar.add(setTypeComboBox);
		toolBar.add(setTypeButton);
		toolBar.add(setUnknownBlockToByteButton);
		toolBar.add(copySelectionButton);
		toolBar.add(editCommentButton);
		toolBar.add(assembleButton);
		toolBar.add(guessButton);
		add(toolBar, BorderLayout.NORTH);
		add(new JScrollPane(grid), BorderLayout.CENTER);

		buildPopupMenu();
		grid.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				maybeShowPopup(e);
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				maybeShowPopup(e);
			}
		});

		updateActionButtonsState();
	}

	private void buildPopupMenu() {
		popupMenu.add(startCodeTraceMenuItem);
		startCodeTraceMenuItem.addActionListener(e -> guessButton.doClick());

		JMenu changeTypeMenu = new JMenu("Change type of selected bytes to");
		for (int i = 0; i < TYPE_SUBMENU_ORDER.length; i++) {
			MemoryType type = TYPE_SUBMENU_ORDER[i];
			JCheckBoxMenuItem item = new JCheckBoxMenuItem(TYPE_SUBMENU_LABELS[i]);
			item.addActionListener(e -> {
				setTypeComboBox.setSelectedItem(type);
				setTypeButton.doClick();
			});
			typeMenuItems[i] = item;
			if (type == MemoryType.UNKNOWN) {
				changeTypeMenu.addSeparator();
			}
			changeTypeMenu.add(item);
		}
		popupMenu.add(changeTypeMenu);

		popupMenu.addSeparator();
		popupMenu.add(popupSetUnknownBlockToByteMenuItem);
		popupSetUnknownBlockToByteMenuItem.addActionListener(e -> setUnknownBlockToByteButton.doClick());

		popupMenu.addSeparator();
		popupMenu.add(popupEditCommentMenuItem);
		popupEditCommentMenuItem.addActionListener(e -> editCommentButton.doClick());
		popupMenu.add(popupAssembleMenuItem);
		popupAssembleMenuItem.addActionListener(e -> assembleButton.doClick());
		popupMenu.add(popupCopySelectionMenuItem);
		popupCopySelectionMenuItem.addActionListener(e -> copySelectionButton.doClick());
		popupMenu.add(splitAtSelectionMenuItem);

		popupMenu.addSeparator();
		popupMenu.add(findMenuItem);
		popupMenu.add(findNextMenuItem);

		popupMenu.addSeparator();
		popupMenu.add(popupSelectNextUnknownBlockMenuItem);
		popupSelectNextUnknownBlockMenuItem.addActionListener(e -> selectNextUnknownBlockButton.doClick());
		popupMenu.add(popupSelectSpritesMenuItem);
		popupSelectSpritesMenuItem.addActionListener(e -> selectSpritesButton.doClick());
		popupMenu.add(popupSelectAllMenuItem);
		popupSelectAllMenuItem.addActionListener(e -> selectAllButton.doClick());

		popupMenu.addSeparator();
		popupMenu.add(popupSaveSelectionNoHeaderMenuItem);
		popupSaveSelectionNoHeaderMenuItem.addActionListener(e -> saveSelectionNoHeaderButton.doClick());
		popupMenu.add(popupSaveSelectionHeaderMenuItem);
		popupSaveSelectionHeaderMenuItem.addActionListener(e -> saveSelectionHeaderButton.doClick());
	}

	/** Ported from MemoryInspectorControlImpl::RButtonDown's notification, handled by MainMemoryInspector to show MemoryInspectorPopupMenu. */
	private void maybeShowPopup(MouseEvent e) {
		if (!e.isPopupTrigger() || memoryInspectorSelection == null || !memoryInspectorSelection.hasSegment()) {
			return;
		}
		updateActionButtonsState();
		syncPopupMenuState();
		popupMenu.show(grid, e.getX(), e.getY());
	}

	/**
	 * Mirrors each popup item's enabled state from its already-updated
	 * toolbar counterpart, and ports {@code TypeSubMenu::Update}'s
	 * enabled/checked logic for the Set Type submenu.
	 */
	private void syncPopupMenuState() {
		startCodeTraceMenuItem.setEnabled(guessButton.isEnabled());
		popupSetUnknownBlockToByteMenuItem.setEnabled(setUnknownBlockToByteButton.isEnabled());
		popupEditCommentMenuItem.setEnabled(editCommentButton.isEnabled());
		popupAssembleMenuItem.setEnabled(assembleButton.isEnabled());
		popupCopySelectionMenuItem.setEnabled(copySelectionButton.isEnabled());
		popupSelectNextUnknownBlockMenuItem.setEnabled(selectNextUnknownBlockButton.isEnabled());
		popupSelectSpritesMenuItem.setEnabled(selectSpritesButton.isEnabled());
		popupSelectAllMenuItem.setEnabled(selectAllButton.isEnabled());
		popupSaveSelectionNoHeaderMenuItem.setEnabled(saveSelectionNoHeaderButton.isEnabled());
		popupSaveSelectionHeaderMenuItem.setEnabled(saveSelectionHeaderButton.isEnabled());

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

	/** Ported from MemoryInspectorWindow's use of WorkspaceFont::GetResizedFont - call whenever the workspace's computer system or double-height setting changes. */
	public void setComputerFont(ComputerFont computerFont) {
		grid.setComputerFont(computerFont);
	}

	/**
	 * Ported from MemoryInspector::SegmentChanged. Some segments (an SDX
	 * symbol-table header, or an SDX relocation block with no data of its
	 * own) have nothing to display, matching the C++ version's {@code
	 * hasData} check.
	 */
	public void segmentChanged(MemoryInspectorSelection memoryInspectorSelection) {
		this.memoryInspectorSelection = memoryInspectorSelection;
		Segment segment = memoryInspectorSelection.getSegment();
		boolean hasData = segment != null && !segment.isHeader(FileHeader.SDX_SYM_DEFINED) && !segment.isSDXRelocBlkWithoutData();

		if (!hasData) {
			grid.setSegment(null);
			titledBorder.setTitle("Memory Inspector");
			updateActionButtonsState();
			revalidate();
			repaint();
			return;
		}

		titledBorder.setTitle(
				String.format("Memory Inspector - %s ($%04X-$%04X)", segment.title, segment.wBegin, segment.wEnd));
		grid.setSegment(segment);
		memoryInspectorSelection.clearSelection();
		updateActionButtonsState();
		revalidate();
		repaint();
	}

	/**
	 * Ported from MemoryInspector::ToggleDisplayAsScreenCode/
	 * MemoryInspectorControlImpl::SetInternal. Switches the ASCII column
	 * between plain byte values and their Atari internal (ANTIC screen
	 * code) equivalent, re-rendering the currently displayed segment if
	 * there is one.
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
		updateActionButtonsState();
	}

	/** Ported from MemoryInspector::ClearSelection. */
	public void clearSelection() {
		if (memoryInspectorSelection != null) {
			memoryInspectorSelection.clearSelection();
		}
		clearHighlight();
		updateActionButtonsState();
	}

	/**
	 * Ported from MemoryInspector::SelectAll: selects the whole segment.
	 * {@code end} is passed as the segment's size (one past the last valid
	 * offset), matching the C++ version - {@link #select}/{@link
	 * MemoryInspectorSelection#setSelection} clamp it back down to the last
	 * valid offset, the same way the C++ version's own {@code Select}/
	 * {@code MemoryInspectorSelection::SetSelection} do.
	 */
	public void selectAll() {
		if (memoryInspectorSelection == null || memoryInspectorSelection.getSegment() == null
				|| memoryInspectorSelection.getSegment().isEmpty()) {
			return;
		}
		select(0, memoryInspectorSelection.getSegment().getSize());
	}

	/**
	 * Ported from MemoryInspector::SelectNextUnknownBlock: scans forward
	 * from just after the current selection (or the segment's start, if
	 * there is none) for the next run of bytes with an unrecognized type,
	 * across this segment and every later one in the segment list - never
	 * wrapping back around to earlier segments/offsets, matching the C++
	 * version, which simply stops (with nothing selected) once the
	 * segment list is exhausted.
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
	 * ({@link LowHighByteDialog}) to ask for the other, unknown half of
	 * the "assumed word", and stricter validation (a single-byte
	 * selection, not at offset 0, on an immediate-mode instruction's
	 * operand), so {@code Dis6502} handles that case directly instead of
	 * calling this method - see {@code Dis6502#performSetMemoryInspectorType}.
	 * The caller is responsible for re-running the disassembly afterward
	 * (matching {@code Refresh()}'s {@code UpdateDisassembly} call), since
	 * this panel does not trigger that itself - see {@link DisassemblyPanel}.
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
			if ((segment.isType(begin + size - 1, MemoryType.LOBYTE) || segment.isType(begin + size - 1, MemoryType.HIBYTE))
					&& begin + size < segment.getSize()) {
				segment.setType(begin + size, MemoryType.CODE);
			}
		}
		segment.setType(begin, type, size);
	}

	/**
	 * Ported from MemoryInspector::SetUnknownBlockToByte
	 * (IDM_DUMP_SET_UNKNOWN_BLOCK_TO_BYTE): reclassifies every byte in the
	 * selection that is still {@link MemoryType#UNKNOWN} - and not the
	 * repurposed type slot right after a {@link MemoryType#LOBYTE}/{@link
	 * MemoryType#HIBYTE} byte (see {@link MemoryType}'s javadoc) - as
	 * {@link MemoryType#BYTE}. Like {@link #setType}, the caller is
	 * responsible for re-running the disassembly afterward.
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
					&& (offset == 0 || (!segment.isType(offset - 1, MemoryType.LOBYTE) && !segment.isType(offset - 1, MemoryType.HIBYTE)))) {
				segment.setType(offset, MemoryType.BYTE);
			}
		}
	}

	/**
	 * Ported from MemoryInspector::Guess (IDM_DUMP_START_CODE_TRACE): runs
	 * {@link GuessCodeLogic} starting from the selection's first byte.
	 * Like {@link #setType}/{@link #setUnknownBlockToByte}, the caller is
	 * responsible for re-running the disassembly afterward.
	 */
	public void guess() {
		if (memoryInspectorSelection == null || !memoryInspectorSelection.hasSelection()) {
			return;
		}
		new GuessCodeLogic(memoryInspectorSelection.getWorkspace()).guess(memoryInspectorSelection.getSegment(),
				memoryInspectorSelection.getBegin());
	}

	/**
	 * Ported from the enablement logic in MemoryInspectorPopupMenu::Update
	 * for IDM_DUMP_FIND/IDM_DUMP_FIND_NEXT/IDM_DUMP_SELECT_ALL/
	 * IDM_DUMP_SELECT_NEXT_UNKNOWN_BLOCK/IDM_DUMP_SAVE_NO_HEADER/
	 * IDM_DUMP_SAVE_HEADER/IDM_DUMP_SPLIT_AT_SELECTION/
	 * IDM_DUMP_SET_UNKNOWN_BLOCK_TO_BYTE and the type submenu's enablement.
	 */
	private void updateActionButtonsState() {
		findMenuItem.setEnabled(canFind(true));
		findNextMenuItem.setEnabled(canFind(false));

		boolean hasSegment = memoryInspectorSelection != null && memoryInspectorSelection.hasSegment()
				&& !memoryInspectorSelection.getSegment().isEmpty();
		selectAllButton.setEnabled(hasSegment);
		selectNextUnknownBlockButton.setEnabled(hasSegment);
		selectSpritesButton.setEnabled(hasSegment);

		boolean hasSelection = memoryInspectorSelection != null && memoryInspectorSelection.hasSelection();
		saveSelectionNoHeaderButton.setEnabled(hasSelection);
		saveSelectionHeaderButton.setEnabled(hasSelection);
		setTypeButton.setEnabled(hasSelection);
		setUnknownBlockToByteButton.setEnabled(hasSelection);
		copySelectionButton.setEnabled(hasSelection);
		editCommentButton.setEnabled(hasSelection);
		assembleButton.setEnabled(hasSelection);
		guessButton.setEnabled(
				hasSelection && memoryInspectorSelection.getSegment().isType(memoryInspectorSelection.getBegin(), MemoryType.UNKNOWN));
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
	 * Ported from MemoryInspector::FindNextString, minus the "not found"
	 * alert - unlike the C++ version, which shows it itself via {@code
	 * FindStringDialog::ShowStringNotFoundMessage}, that is left to the
	 * caller here (see {@code Dis6502}), matching how the rest of this
	 * class stays free of its own popups.
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
		updateActionButtonsState();
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
}
