/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import com.wudsn.tools.base.common.HexUtility;
import com.wudsn.tools.base.common.TextUtility;
import com.wudsn.tools.base.gui.ElementFactory;
import com.wudsn.tools.base.repository.Action;
import com.wudsn.tools.dis6502.Actions;
import com.wudsn.tools.dis6502.Texts;
import com.wudsn.tools.dis6502.model.FileHeader;
import com.wudsn.tools.dis6502.model.GuessCodeLogic;
import com.wudsn.tools.dis6502.model.MemoryInspectorState.EditCharResult;
import com.wudsn.tools.dis6502.model.MemoryInspectorState.EditCursorMovement;
import com.wudsn.tools.dis6502.model.MemoryInspectorState.EditPane;
import com.wudsn.tools.dis6502.model.MemoryType;
import com.wudsn.tools.dis6502.model.MutableMemoryInspectorState;
import com.wudsn.tools.dis6502.model.Segment;
import com.wudsn.tools.dis6502.model.SegmentList;

/**
 * A read-only hex/ASCII dump of the currently selected segment, with a
 * highlighted byte-range selection.
 * <p>
 * The ASCII column's byte-to-character transform for "internal" (Atari
 * ANTIC screen code) mode lives in {@link HexGridPanel}'s paint routine.
 * {@link HexGridPanel}'s own built-in drag-select mouse handling (a
 * {@link HexGridPanel.DragSelectionListener} set up in this class's
 * constructor, calling {@link #select}) lets the user click or drag in
 * the grid to select a byte range directly, on top of every other way
 * {@link #select} is already reached (Find, Select All, Select Graphics,
 * XRef navigation, a Split at Selection result). {@link
 * #findString}/{@link #findNextString}/{@link #canFind} are triggered
 * from {@link #findMenuItem}/{@link #findNextMenuItem}.
 * <p>
 * This panel has no toolbar of per-command buttons - almost every command is
 * reachable only from the right-click popup menu ({@link #maybeShowPopup},
 * attached to the grid), following {@link SegmentListPanel}'s pattern:
 * every item is a public {@code JMenuItem} field wired directly by {@code
 * Dis6502}, never a hidden {@code JButton} kept around only to be {@code
 * doClick()}d - this panel used to also have a toolbar with a button per
 * command, each popup item {@code doClick()}ing its button, until both
 * the redundant toolbar and that indirection were removed. {@link
 * #displayAsScreenCodeButton} is the one exception - a real header
 * button, not a popup item at all (see that field's own javadoc), but it
 * stays wired directly by {@code Dis6502} like everything else, not a
 * {@code doClick()} proxy. {@link #splitAtSelectionMenuItem} only splits
 * the segment list the same way {@code
 * com.wudsn.tools.dis6502.ui.SegmentListPanel}'s Move Up/Down/Merge/Delete
 * already do, without reinterpreting any byte's type, and {@link
 * #selectAll}/{@link #selectNextUnknownBlock}/{@link
 * #saveSelectionNoHeaderMenuItem}/{@link #saveSelectionHeaderMenuItem} are
 * non-mutating too - selecting, and writing out, bytes that already
 * exist. {@link #setType}/{@link #setUnknownBlockToByte} do mutate a
 * segment's understanding of its bytes' types, and are the first such
 * commands here - unlike the rest of this class, callers must re-run the
 * disassembly afterward (see their own javadoc). {@link
 * #copySelectionMenuItem} copies the selection as a plain hex string.
 * <p>
 * {@link #cutSelectionMenuItem}/{@link #copySelectionMenuItem}/{@link
 * #pasteSelectionMenuItem}/{@link #deleteSelectionMenuItem} use real
 * segment-buffer resizing - {@link
 * com.wudsn.tools.dis6502.model.Segment#deleteRange}/{@link
 * com.wudsn.tools.dis6502.model.Segment#insertRange}, wired from {@code
 * Dis6502#performDeleteMemoryInspectorSelection}/{@code
 * #performPasteMemoryInspectorSelection}. {@link #cutSelectionMenuItem} is
 * Copy+Delete composed at the call site. {@link #pasteSelectionMenuItem}
 * is a single insert-before-the-selection command.
 * <p>
 * {@link #selectGraphicsMenuItem} (ported as {@link
 * SelectGraphicsDialog}/{@link GraphicPanel}/{@link GraphicMode} - see
 * {@link SelectGraphicsDialog}'s own javadoc for why these are called
 * "Graphic", not "Sprite") is non-mutating like the other Select* items -
 * it just ends in a call to {@link #select}, {@link SelectGraphicsDialog}'s
 * own javadoc has the details. {@link #editCommentMenuItem} (ported as
 * {@link CommentDialog}) is wired only from here, not also from a plain
 * disassembly-line click with no byte selection - see {@link
 * CommentDialog}'s javadoc. {@link #assembleMenuItem} (ported as {@link
 * AssembleDialog}) is this port's one piece of direct byte-level editing -
 * not raw hex digit entry (there is no grid cell to type into), but typing
 * 6502 instructions to assemble in place. {@link #startCodeTraceMenuItem}
 * runs {@link GuessCodeLogic}, a large enough, UI-independent enough piece
 * of logic to get its own model-layer class instead of living directly
 * here - see that class's javadoc for what it does. The hex dump itself
 * is {@link HexGridPanel}, a custom-painted, read-only grid using the
 * real per-computer-system bitmap glyphs from {@link ComputerFont} - see
 * that class's javadoc for why a real font asset is needed at all (a
 * plain Java font cannot display ATASCII/PETSCII characters) and for
 * exactly what it implements (the paint routine, mouse-drag selection)
 * and what it does not (in-place editing). {@link #setComputerFont} must
 * be called by {@code Dis6502} whenever the workspace's computer system or
 * double-font-height setting changes.
 * <p>
 * The Change Type submenu is the one popup item that is not a single
 * {@code JMenuItem} field: with thirteen items, each already knowing exactly
 * which {@link MemoryType} it means, a public field per item (or worse, a
 * shared field driven by some now-deleted toolbar combo box) would be backwards
 * - instead its items report the chosen type via
 * {@link #setTypeSelectionListener}, matching {@link XRefPanel}'s {@code
 * XRefSelectionListener} pattern. The submenu's checkmarks reflect which
 * type(s) are actually present across the selection, including its
 * LOBYTE/HIBYTE-adjacency lookback for a byte whose own stored type is
 * unknown/invalid. {@link #cutSelectionMenuItem}/{@link
 * #pasteSelectionMenuItem}/{@link #deleteSelectionMenuItem} are plain
 * top-level popup items instead, not part of this submenu - see this
 * class's own note above on their design.
 * <p>
 * {@link #editMenuItem}/{@link #enterEditMode()}/{@link #quitEditMode()} and
 * the keyboard handling wired up in the constructor implement in-place
 * hex/ASCII editing of the selected byte(s), entered via F2, {@link
 * #editMenuItem}, or a double-click, exited via Esc or {@link
 * #quitEditModeMenuItem} (shown, while editing, in a separate ad hoc
 * popup that replaces the normal one). The actual cursor state and
 * navigation/writing logic - not just the on/off flag - live on {@link
 * #memoryInspectorState} ({@link
 * MutableMemoryInspectorState#moveEditCursor}/{@link
 * MutableMemoryInspectorState#typeEditChar}, applying the {@link
 * MemoryType#SBYTE} ASCII transform via {@link
 * MemoryType#toSbyteInternalCode}), not here or in {@link HexGridPanel} -
 * see {@link MutableMemoryInspectorState}'s own javadoc - so that logic
 * can be exercised by a plain, headless unit test. This class keeps only
 * the Swing-specific glue: {@link #handleEditKeyPressed}/{@link
 * #handleEditKeyTyped} translate a raw {@link java.awt.event.KeyEvent}
 * into a semantic call on {@link #memoryInspectorState}, then tell {@link
 * HexGridPanel} to notice via {@link
 * HexGridPanel#refreshEditCursor}/{@link HexGridPanel#refreshEditMode} -
 * that class reads the current selection/edit-mode values live off {@link
 * #memoryInspectorState} itself (through {@link
 * com.wudsn.tools.dis6502.model.MemoryInspectorState}, narrowing what a
 * pure painter can do to it) rather than being handed each value as it
 * changes; this class also owns focus/mouse handling, the popup-menu
 * swap, and the blink timer. {@link #quitEditMode()} is the single exit
 * point for leaving edit mode, so every exit path refreshes the
 * disassembly uniformly and resyncs the selection/title to the cursor's
 * final position, calling {@link #select} with that offset. {@link
 * MutableMemoryInspectorState#typeEditChar} accepts the full printable
 * ASCII range.
 *
 * @author Peter Dell
 */
public final class MemoryInspectorPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	/** The type submenu's entries, in menu order. */
	private static final MemoryType[] TYPE_SUBMENU_ORDER = { MemoryType.CODE, MemoryType.LOBYTE, MemoryType.HIBYTE,
			MemoryType.BYTE, MemoryType.WORD, MemoryType.LABEL, MemoryType.SYMBOL, MemoryType.FIXUP, MemoryType.STRING,
			MemoryType.SBYTE, MemoryType.DLIST, MemoryType.STORE, MemoryType.UNKNOWN };
	private static final Action[] TYPE_SUBMENU_ACTIONS = { Actions.MemoryInspectorPopupMenu_ChangeType_Code,
			Actions.MemoryInspectorPopupMenu_ChangeType_LowByte, Actions.MemoryInspectorPopupMenu_ChangeType_HighByte,
			Actions.MemoryInspectorPopupMenu_ChangeType_Byte, Actions.MemoryInspectorPopupMenu_ChangeType_Word,
			Actions.MemoryInspectorPopupMenu_ChangeType_Label, Actions.MemoryInspectorPopupMenu_ChangeType_Symbol,
			Actions.MemoryInspectorPopupMenu_ChangeType_Fixup, Actions.MemoryInspectorPopupMenu_ChangeType_String,
			Actions.MemoryInspectorPopupMenu_ChangeType_Sbyte, Actions.MemoryInspectorPopupMenu_ChangeType_Dlist,
			Actions.MemoryInspectorPopupMenu_ChangeType_Store, Actions.MemoryInspectorPopupMenu_ChangeType_Unknown };

	private final PartHeaderPanel header = new PartHeaderPanel(new Color(0, 255, 255));
	private final HexGridPanel grid = new HexGridPanel();

	/**
	 * Toggles {@link #setDisplayAsScreenCode} - lives here rather than in the
	 * main menu, since it only ever affects this one panel. Built via {@link
	 * Actions#MemoryInspectorPanel_DisplayAsScreenCode}/{@link
	 * ElementFactory#createToggleButton}, the same {@code Action}-backed
	 * pattern every other control in this port uses - unlike {@link
	 * DisassemblyPanel#findButton}/{@link DisassemblyPanel#findNextButton},
	 * which predate that pattern and stay plain since they were never menu
	 * items. {@code Dis6502} wires this directly, the same as every other
	 * control here - not a hidden {@code doClick()} proxy, since this button
	 * is now this command's only home.
	 */
	public final JToggleButton displayAsScreenCodeButton = ElementFactory.createToggleButton(Actions.MemoryInspectorPanel_DisplayAsScreenCode,
			true);

	/**
	 * Every popup menu item is a public field wired directly by {@code
	 * Dis6502}, the same way {@link DisassemblyPanel}'s label-navigation popup
	 * items are - a hidden {@code JButton} kept around only to be
	 * {@code doClick()}d would be a pointless layer of indirection. The Change Type
	 * submenu is the one exception: with thirteen items, each already knowing its
	 * own type, a public field per item would be backwards - its items report the
	 * type the user picked through {@link #setTypeSelectionListener} instead. Each
	 * item is built via {@code com.wudsn.tools.base.gui.ElementFactory} from an
	 * {@code Action} in {@code com.wudsn.tools.dis6502.Actions} - see {@code
	 * Actions}' own javadoc for label/mnemonic sourcing.
	 */
	public final JMenuItem findMenuItem = ElementFactory.createMenuItem(Actions.MemoryInspectorPopupMenu_Find, "findMenuItem");
	public final JMenuItem findNextMenuItem = ElementFactory.createMenuItem(Actions.MemoryInspectorPopupMenu_FindNext, "findNextMenuItem");
	public final JMenuItem splitAtSelectionMenuItem = ElementFactory.createMenuItem(Actions.MemoryInspectorPopupMenu_SplitAtSelection,
			"splitAtSelectionMenuItem");
	public final JMenuItem startCodeTraceMenuItem = ElementFactory.createMenuItem(Actions.MemoryInspectorPopupMenu_StartCodeTrace,
			"startCodeTraceMenuItem");
	public final JMenuItem setUnknownBlockToByteMenuItem = ElementFactory
			.createMenuItem(Actions.MemoryInspectorPopupMenu_SetUnknownBlockToByte, "setUnknownBlockToByteMenuItem");
	public final JMenuItem editCommentMenuItem = ElementFactory.createMenuItem(Actions.MemoryInspectorPopupMenu_EditComment,
			"editCommentMenuItem");
	public final JMenuItem editMenuItem = ElementFactory.createMenuItem(Actions.MemoryInspectorPopupMenu_Edit, "editMenuItem");
	public final JMenuItem assembleMenuItem = ElementFactory.createMenuItem(Actions.MemoryInspectorPopupMenu_Assemble, "assembleMenuItem");
	public final JMenuItem cutSelectionMenuItem = ElementFactory.createMenuItem(Actions.MemoryInspectorPopupMenu_CutSelection,
			"cutSelectionMenuItem");
	public final JMenuItem copySelectionMenuItem = ElementFactory.createMenuItem(Actions.MemoryInspectorPopupMenu_CopySelection,
			"copySelectionMenuItem");
	public final JMenuItem pasteSelectionMenuItem = ElementFactory.createMenuItem(Actions.MemoryInspectorPopupMenu_PasteSelection,
			"pasteSelectionMenuItem");
	public final JMenuItem deleteSelectionMenuItem = ElementFactory.createMenuItem(Actions.MemoryInspectorPopupMenu_DeleteSelection,
			"deleteSelectionMenuItem");
	public final JMenuItem selectNextUnknownBlockMenuItem = ElementFactory
			.createMenuItem(Actions.MemoryInspectorPopupMenu_SelectNextUnknownBlock, "selectNextUnknownBlockMenuItem");
	public final JMenuItem selectGraphicsMenuItem = ElementFactory.createMenuItem(Actions.MemoryInspectorPopupMenu_SelectGraphics,
			"selectGraphicsMenuItem");
	public final JMenuItem selectAllMenuItem = ElementFactory.createMenuItem(Actions.MemoryInspectorPopupMenu_SelectAll, "selectAllMenuItem");
	public final JMenuItem saveSelectionNoHeaderMenuItem = ElementFactory
			.createMenuItem(Actions.MemoryInspectorPopupMenu_SaveSelectionNoHeader, "saveSelectionNoHeaderMenuItem");
	public final JMenuItem saveSelectionHeaderMenuItem = ElementFactory.createMenuItem(Actions.MemoryInspectorPopupMenu_SaveSelectionHeader,
			"saveSelectionHeaderMenuItem");

	/**
	 * The single item of the ad hoc popup shown, instead of {@link #popupMenu},
	 * while edit mode is active - ported from {@code MemoryInspector::DrawMenu},
	 * which likewise builds a separate menu rather than filtering the normal one,
	 * since every other command is modally blocked while editing (see
	 * {@code MainMemoryInspector::PerformCommands}).
	 */
	public final JMenuItem quitEditModeMenuItem = ElementFactory.createMenuItem(Actions.MemoryInspectorPopupMenu_QuitEditMode,
			"quitEditModeMenuItem");

	private final JPopupMenu popupMenu = new JPopupMenu();
	private final JPopupMenu editModePopupMenu = new JPopupMenu();
	private final JCheckBoxMenuItem[] typeMenuItems = new JCheckBoxMenuItem[TYPE_SUBMENU_ORDER.length];
	private TypeSelectionListener typeSelectionListener;
	private SelectionChangedListener selectionChangedListener;
	private EditModeExitedListener editModeExitedListener;
	private Runnable editModeEnteredListener;

	private MutableMemoryInspectorState memoryInspectorState;

	private Timer blinkTimer;

	private int findSegmentIndex = SegmentList.NO_SEGMENT_INDEX;
	private int findOffset;
	private int findSize;
	private boolean findAllSegments = true;
	private String findText = "";

	public MemoryInspectorPanel() {
		super(new BorderLayout());
		header.setText(Texts.MemoryInspectorPanel_NoSegmentSelectedTitle);

		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		buttonPanel.add(displayAsScreenCodeButton);

		JPanel topPanel = new JPanel(new BorderLayout());
		topPanel.add(header, BorderLayout.NORTH);
		topPanel.add(buttonPanel, BorderLayout.SOUTH);
		add(topPanel, BorderLayout.NORTH);

		JScrollPane scrollPane = new JScrollPane(grid);
		// Splitters already separate the part windows - the scroll pane's own
		// L&F-default border would just draw a redundant line right next to them.
		scrollPane.setBorder(BorderFactory.createEmptyBorder());
		add(scrollPane, BorderLayout.CENTER);

		buildPopupMenu();
		editModePopupMenu.add(quitEditModeMenuItem);
		MouseAdapter mouseHandler = new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				grid.requestFocusInWindow();
				maybeShowPopup(e);
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				maybeShowPopup(e);
			}

			@Override
			public void mouseClicked(MouseEvent e) {
				if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
					enterEditModeAtPoint(e.getX(), e.getY());
				}
			}
		};
		grid.addMouseListener(mouseHandler);
		grid.setDragSelectionListener(new HexGridPanel.DragSelectionListener() {
			@Override
			public void onDragSelectionChanged(int begin, int end) {
				select(begin, end);
			}

			@Override
			public void onDragSelectionFinished() {
				if (selectionChangedListener != null) {
					selectionChangedListener.onSelectionChanged();
				}
			}
		});

		bindEditModeKeys();
		bindPopupMenuAccelerators();
		grid.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				handleEditKeyPressed(e);
			}

			@Override
			public void keyTyped(KeyEvent e) {
				handleEditKeyTyped(e);
			}
		});

		updatePopupMenuItemsState();
	}

	/**
	 * Binds F2/Esc at the window level ({@code WHEN_IN_FOCUSED_WINDOW}, not
	 * {@code WHEN_FOCUSED}), so they work regardless of which child control
	 * currently has focus, as long as the window is active. The keystrokes
	 * themselves come from {@link Actions#MemoryInspectorPopupMenu_Edit}/
	 * {@link Actions#MemoryInspectorPopupMenu_QuitEditMode} - see
	 * {@link #bindPopupMenuAccelerators} for why every binding here (and there)
	 * is guarded by {@link #isAnyPopupMenuVisible}.
	 */
	private void bindEditModeKeys() {
		bindAccelerator(Actions.MemoryInspectorPopupMenu_Edit, this::enterEditMode);
		bindAccelerator(Actions.MemoryInspectorPopupMenu_QuitEditMode, this::quitEditMode);
	}

	/**
	 * Binds the remaining popup-menu accelerators (see
	 * POPUP_MENU_ACCELERATORS_PLAN.md) at the window level, the same {@code
	 * WHEN_IN_FOCUSED_WINDOW} scope as {@link #bindEditModeKeys}. Each
	 * keystroke comes from the same {@link Action} in {@code
	 * com.wudsn.tools.dis6502.Actions} that built the corresponding menu item
	 * - not a literal duplicated here - so {@code Actions.java} stays the
	 * single source of truth for every item's keystroke, the same as it
	 * already is for its label/mnemonic.
	 * <p>
	 * Populating these {@link Action}s' accelerators also makes {@code
	 * ElementFactory.createMenuItem} apply them to the menu item itself (nice,
	 * free shortcut-hint text next to the label) - but an empirical smoke test
	 * written for that plan's "Step 0" found a standalone {@code JPopupMenu}
	 * item's accelerator this way only actually fires while that specific
	 * popup instance is open on screen. While {@link #popupMenu} (or {@link
	 * #editModePopupMenu}) is open, that means both the item's own
	 * live-but-narrow accelerator and this method's window-level binding
	 * could fire for the same keystroke - {@link #isAnyPopupMenuVisible}
	 * guards every binding here (and in {@link #bindEditModeKeys}) against
	 * that double-fire, deferring to the item's own accelerator whenever a
	 * popup happens to already be showing. Calling {@code doClick()} on the
	 * already-correctly-wired, already-visible item is the same sanctioned
	 * exception to avoiding hidden {@code doClick()} indirection {@link
	 * #bindEditModeKeys} relies on too (calling {@link
	 * #enterEditMode()}/{@link #quitEditMode()} directly), not a proxy to a
	 * hidden component.
	 * <p>
	 * One keystroke does not fit the "defer to the item's own live accelerator"
	 * story above: Esc, while {@link #popupMenu}/{@link #editModePopupMenu} is
	 * open, fires neither {@link #quitEditModeMenuItem}'s own accelerator nor
	 * this method's window-level binding (confirmed with a dedicated smoke
	 * test) - Swing's own menu-cancel handling (closing the open popup)
	 * consumes Esc before either accelerator mechanism sees it. So pressing
	 * Esc while either popup happens to be open just closes that popup,
	 * leaving edit mode active; Esc still reliably exits edit mode the rest
	 * of the time, i.e. whenever neither popup is currently open.
	 */
	private void bindPopupMenuAccelerators() {
		bindAccelerator(Actions.MemoryInspectorPopupMenu_StartCodeTrace, startCodeTraceMenuItem);
		bindAccelerator(Actions.MemoryInspectorPopupMenu_Assemble, assembleMenuItem);
		bindAccelerator(Actions.MemoryInspectorPopupMenu_CutSelection, cutSelectionMenuItem);
		bindAccelerator(Actions.MemoryInspectorPopupMenu_CopySelection, copySelectionMenuItem);
		bindAccelerator(Actions.MemoryInspectorPopupMenu_PasteSelection, pasteSelectionMenuItem);
		bindAccelerator(Actions.MemoryInspectorPopupMenu_DeleteSelection, deleteSelectionMenuItem);
		bindAccelerator(Actions.MemoryInspectorPopupMenu_Find, findMenuItem);
		bindAccelerator(Actions.MemoryInspectorPopupMenu_FindNext, findNextMenuItem);
		bindAccelerator(Actions.MemoryInspectorPopupMenu_SelectNextUnknownBlock, selectNextUnknownBlockMenuItem);
		bindAccelerator(Actions.MemoryInspectorPopupMenu_SelectGraphics, selectGraphicsMenuItem);
		bindAccelerator(Actions.MemoryInspectorPopupMenu_SelectAll, selectAllMenuItem);

		for (int i = 0; i < TYPE_SUBMENU_ACTIONS.length; i++) {
			bindAccelerator(TYPE_SUBMENU_ACTIONS[i], typeMenuItems[i]);
		}
	}

	private boolean isAnyPopupMenuVisible() {
		return popupMenu.isVisible() || editModePopupMenu.isVisible();
	}

	private int popupAcceleratorCounter;

	private void bindAccelerator(Action action, JMenuItem item) {
		bindAccelerator(action, item::doClick);
	}

	private void bindAccelerator(Action action, Runnable command) {
		String actionKey = "popupAccelerator" + popupAcceleratorCounter++;
		grid.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(action.getAccelerator(), actionKey);
		grid.getActionMap().put(actionKey, new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (!isAnyPopupMenuVisible()) {
					command.run();
				}
			}
		});
	}

	private void buildPopupMenu() {
		popupMenu.add(startCodeTraceMenuItem);

		JMenu changeTypeMenu = ElementFactory.createMenu(Actions.MemoryInspectorPopupMenu_ChangeType);
		for (int i = 0; i < TYPE_SUBMENU_ORDER.length; i++) {
			MemoryType type = TYPE_SUBMENU_ORDER[i];
			JCheckBoxMenuItem item = ElementFactory.createCheckBoxMenuItem(TYPE_SUBMENU_ACTIONS[i]);
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
		popupMenu.add(editMenuItem);
		popupMenu.add(assembleMenuItem);
		popupMenu.add(cutSelectionMenuItem);
		popupMenu.add(copySelectionMenuItem);
		popupMenu.add(pasteSelectionMenuItem);
		popupMenu.add(deleteSelectionMenuItem);
		popupMenu.add(splitAtSelectionMenuItem);

		popupMenu.addSeparator();
		popupMenu.add(findMenuItem);
		popupMenu.add(findNextMenuItem);

		popupMenu.addSeparator();
		popupMenu.add(selectNextUnknownBlockMenuItem);
		popupMenu.add(selectGraphicsMenuItem);
		popupMenu.add(selectAllMenuItem);

		popupMenu.addSeparator();
		popupMenu.add(saveSelectionNoHeaderMenuItem);
		popupMenu.add(saveSelectionHeaderMenuItem);
	}

	/** Shows the normal or edit-mode popup, whichever applies. */
	private void maybeShowPopup(MouseEvent e) {
		if (!e.isPopupTrigger() || memoryInspectorState == null || !memoryInspectorState.hasSegment()) {
			return;
		}
		if (isEditMode()) {
			editModePopupMenu.show(grid, e.getX(), e.getY());
			return;
		}
		updatePopupMenuItemsState();
		syncPopupMenuState();
		popupMenu.show(grid, e.getX(), e.getY());
	}

	/**
	 * The enabled/checked logic for the Set Type submenu - every other popup
	 * item's enabled state is set directly in {@link
	 * #updatePopupMenuItemsState}.
	 */
	private void syncPopupMenuState() {
		boolean hasSelection = memoryInspectorState != null && memoryInspectorState.hasSelection();
		boolean[] present = new boolean[TYPE_SUBMENU_ORDER.length];
		if (hasSelection) {
			Segment segment = memoryInspectorState.getSegment();
			int begin = memoryInspectorState.getBegin();
			int end = memoryInspectorState.getEnd();
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
				enabled = memoryInspectorState.getBegin() > 0;
			}
			typeMenuItems[i].setEnabled(enabled);
			typeMenuItems[i].setState(present[i]);
		}
	}

	/** Call whenever the workspace's computer system or double-height setting changes. */
	public void setComputerFont(ComputerFont computerFont) {
		grid.setComputerFont(computerFont);
		header.setComputerFont(computerFont);
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
	 * Reports that edit mode just ended, for any reason - ported from {@code
	 * MainController::QuitEditMode}'s {@code UpdateDisassembly()} call. {@code
	 * Dis6502} uses this to re-run the disassembly, matching
	 * {@code performShowAssembleDialog}/{@code performGuessCode}'s own
	 * mutate-then-refresh pattern.
	 */
	public void setEditModeExitedListener(EditModeExitedListener editModeExitedListener) {
		this.editModeExitedListener = editModeExitedListener;
	}

	/** Reports that edit mode just began - {@code Dis6502} disables the File menu's open/add/save commands for its duration. */
	public void setEditModeEnteredListener(Runnable editModeEnteredListener) {
		this.editModeEnteredListener = editModeEnteredListener;
	}

	/**
	 * Some segments (an SDX symbol-table header, or an SDX relocation block
	 * with no data of its own) have nothing to display; the title is still
	 * shown for these (the segment itself is genuinely selected, just not
	 * byte-displayable) - see {@link #updateTitle}, which only special-cases
	 * a null segment, not this narrower "no data" condition.
	 */
	public void segmentChanged(MutableMemoryInspectorState memoryInspectorState) {
		quitEditMode();
		this.memoryInspectorState = memoryInspectorState;
		Segment segment = memoryInspectorState.getSegment();
		boolean hasData = segment != null && !segment.isHeader(FileHeader.SDX_SYM_DEFINED)
				&& !segment.isSDXRelocBlkWithoutData();

		grid.setMemoryInspectorState(memoryInspectorState);
		grid.setSelection(memoryInspectorState.getSelection());
		grid.setByteSource(hasData ? new SegmentByteSource(segment) : null);
		memoryInspectorState.clearSelection();
		updateTitle();
		updatePopupMenuItemsState();
		revalidate();
		repaint();
	}

	/**
	 * The selected segment's 1-based number, address range and size in hex
	 * and decimal - or, once a byte range within it is marked, the same for
	 * that selection instead (absolute addresses, {@code
	 * segment.wBegin}-relative). Called explicitly wherever the segment or
	 * selection changes ({@link #segmentChanged}, {@link #select}, {@link
	 * #clearSelection}), rather than recomputed on every repaint. Built
	 * from the {@code Texts.MemoryInspectorPanel_*Title} fields; the
	 * "Selection" title includes the segment number, the same as the
	 * "Segment" title does.
	 */
	private void updateTitle() {
		Segment segment = memoryInspectorState == null ? null : memoryInspectorState.getSegment();
		if (segment == null) {
			header.setText(Texts.MemoryInspectorPanel_NoSegmentSelectedTitle);
			return;
		}

		String segmentNumber = String.valueOf(memoryInspectorState.getSegmentIndex() + 1);
		String title;
		if (memoryInspectorState.hasSelection()) {
			int begin = segment.wBegin + memoryInspectorState.getBegin();
			int end = segment.wBegin + memoryInspectorState.getEnd();
			int size = memoryInspectorState.getSize();
			title = TextUtility.format(Texts.MemoryInspectorPanel_SelectionTitle, segmentNumber, hex(begin), hex(end), hex(size),
					String.valueOf(size));
		} else {
			int begin = segment.wBegin;
			int end = segment.wEnd;
			int size = segment.getSize();
			title = TextUtility.format(Texts.MemoryInspectorPanel_SegmentTitle, segmentNumber, hex(begin), hex(end), hex(size),
					String.valueOf(size));
		}
		header.setText(title);
	}

	private static String hex(int value) {
		return HexUtility.getLongValueHexString(value, 4);
	}

	/**
	 * Switches the ASCII column between plain byte values and their Atari
	 * internal (ANTIC screen code) equivalent, re-rendering the currently
	 * displayed segment if there is one.
	 */
	public void setDisplayAsScreenCode(boolean displayAsScreenCode) {
		grid.setDisplayAsScreenCode(displayAsScreenCode);
	}

	/** Sets the selection to [begin, end) and refreshes the grid/title/popup state. */
	public void select(int begin, int end) {
		if (memoryInspectorState == null || memoryInspectorState.getSegment() == null
				|| memoryInspectorState.getSegment().isEmpty()) {
			return;
		}
		memoryInspectorState.setSelection(begin, end);
		grid.refreshSelection();
		updateTitle();
		updatePopupMenuItemsState();
	}

	/** Clears the selection and refreshes the grid/title/popup state. */
	public void clearSelection() {
		if (memoryInspectorState != null) {
			memoryInspectorState.clearSelection();
		}
		grid.refreshSelection();
		updateTitle();
		updatePopupMenuItemsState();
	}

	/**
	 * Selects the whole segment. {@code end} is passed as the segment's size
	 * (one past the last valid offset) - {@link #select}/{@link
	 * MutableMemoryInspectorState#setSelection} clamp it back down to the
	 * last valid offset.
	 */
	public void selectAll() {
		if (memoryInspectorState == null || memoryInspectorState.getSegment() == null
				|| memoryInspectorState.getSegment().isEmpty()) {
			return;
		}
		select(0, memoryInspectorState.getSegment().getSize());
	}

	/**
	 * Scans forward from just after the current selection (or the segment's
	 * start, if there is none) for the next run of bytes with an
	 * unrecognized type, across this segment and every later one in the
	 * segment list - never wrapping back around to earlier segments/offsets;
	 * simply stops (with nothing selected) once the segment list is
	 * exhausted.
	 */
	public void selectNextUnknownBlock() {
		if (memoryInspectorState == null || memoryInspectorState.getSegment() == null
				|| memoryInspectorState.getSegment().isEmpty() || !memoryInspectorState.getSegment().bBinary) {
			return;
		}

		int last = memoryInspectorState.hasSelection() ? memoryInspectorState.getBegin() + 1 : 0;

		SegmentList segmentList = memoryInspectorState.getWorkspace().getSegmentList();
		int count = segmentList.getCount();
		for (int segmentIndex = memoryInspectorState.getSegmentIndex(); segmentIndex < count; segmentIndex++) {
			Segment segment = segmentList.getSegment(segmentIndex);
			int end = segment.wEnd - segment.wBegin;

			for (int offset = last; offset <= end; offset++) {
				if (segment.isUnknown(offset)) {
					if (memoryInspectorState.getSegmentIndex() != segmentIndex) {
						memoryInspectorState.setSegmentIndex(segmentIndex);
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
	 * The LOBYTE/HIBYTE case needs a dialog ({@link LowHighByteDialog}) to ask
	 * for the other, unknown half of the "assumed word", and stricter
	 * validation (a single-byte selection, not at offset 0, on an
	 * immediate-mode instruction's operand), so {@code Dis6502} handles that
	 * case directly instead of calling this method - see {@code
	 * Dis6502#performSetMemoryInspectorType}. The caller is responsible for
	 * re-running the disassembly afterward, since this panel does not
	 * trigger that itself - see {@link DisassemblyPanel}.
	 */
	public void setType(MemoryType type) {
		if (memoryInspectorState == null || !memoryInspectorState.hasSelection()) {
			return;
		}
		Segment segment = memoryInspectorState.getSegment();
		if (!segment.bBinary) {
			return;
		}
		int begin = memoryInspectorState.getBegin();
		int size = memoryInspectorState.getSize();

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
	 * Reclassifies every byte in the selection that is still {@link
	 * MemoryType#UNKNOWN} - and not the repurposed type slot right after a
	 * {@link MemoryType#LOBYTE}/{@link MemoryType#HIBYTE} byte (see {@link
	 * MemoryType}'s javadoc) - as {@link MemoryType#BYTE}. Like {@link
	 * #setType}, the caller is responsible for re-running the disassembly
	 * afterward.
	 */
	public void setUnknownBlockToByte() {
		if (memoryInspectorState == null || !memoryInspectorState.hasSelection()) {
			return;
		}
		Segment segment = memoryInspectorState.getSegment();
		if (!segment.bBinary) {
			return;
		}
		int begin = memoryInspectorState.getBegin();
		int size = memoryInspectorState.getSize();

		for (int offset = begin; offset < begin + size; offset++) {
			if (segment.isType(offset, MemoryType.UNKNOWN)
					&& (offset == 0 || (!segment.isType(offset - 1, MemoryType.LOBYTE)
							&& !segment.isType(offset - 1, MemoryType.HIBYTE)))) {
				segment.setType(offset, MemoryType.BYTE);
			}
		}
	}

	/**
	 * Runs {@link GuessCodeLogic} starting from the selection's first byte.
	 * Like {@link #setType}/{@link #setUnknownBlockToByte}, the caller is
	 * responsible for re-running the disassembly afterward.
	 */
	public void guess() {
		if (memoryInspectorState == null || !memoryInspectorState.hasSelection()) {
			return;
		}
		new GuessCodeLogic(memoryInspectorState.getWorkspace()).guess(memoryInspectorState.getSegment(),
				memoryInspectorState.getBegin());
	}

	/**
	 * Delegates to {@link MutableMemoryInspectorState#isEditMode()} - {@code false}
	 * if there is no {@link #memoryInspectorState} yet (e.g. before the
	 * first {@link #segmentChanged}), since edit mode can only ever have been
	 * entered once one exists.
	 */
	public boolean isEditMode() {
		return memoryInspectorState != null && memoryInspectorState.isEditMode();
	}

	/**
	 * Enters edit mode at the current selection's first byte, snapping a
	 * multi-byte selection down to that one byte, with the cursor starting
	 * on the hex pane's high nibble.
	 */
	public void enterEditMode() {
		enterEditModeAt(-1, EditPane.HEX_HIGH);
	}

	/**
	 * Enters edit mode the normal way (snapping to the selection's first
	 * byte), then immediately repositions the cursor to the exact nibble/
	 * character double-clicked.
	 */
	private void enterEditModeAtPoint(int x, int y) {
		HexGridPanel.CellHit hit = grid.cellAtPoint(x, y);
		if (hit == null) {
			return;
		}
		enterEditModeAt(hit.offset, hit.pane);
	}

	/**
	 * This method decides WHERE to start editing (a fresh selection's first
	 * byte, or an exact double-clicked position); {@link
	 * MutableMemoryInspectorState#enterEditMode} then owns whether that is actually
	 * allowed (a selected segment must exist and the offset must be in range
	 * for it) and the resulting lock state itself.
	 */
	private void enterEditModeAt(int offsetHint, EditPane paneHint) {
		if (memoryInspectorState == null || !memoryInspectorState.hasSelection()) {
			return;
		}
		int begin = memoryInspectorState.getBegin();
		select(begin, begin);
		int offset = offsetHint >= 0 ? offsetHint : begin;
		EditPane pane = offsetHint >= 0 ? paneHint : EditPane.HEX_HIGH;
		if (!memoryInspectorState.enterEditMode(offset, pane)) {
			return;
		}
		grid.refreshEditMode();
		grid.refreshEditCursor();
		grid.requestFocusInWindow();
		startBlinkTimer();
		if (editModeEnteredListener != null) {
			editModeEnteredListener.run();
		}
	}

	/**
	 * Leaves edit mode, if it was active - a no-op otherwise. This is the
	 * single exit point for every way edit mode can end (Esc, {@link
	 * #quitEditModeMenuItem}, typing past the end of the buffer, or the
	 * segment changing), so the selection-resync below and {@link
	 * #editModeExitedListener} both fire uniformly on every exit - see this
	 * class's javadoc. {@link MutableMemoryInspectorState#quitEditMode()}
	 * only releases the model-level lock; the UI-facing consequences (the
	 * selection resync, the grid/blink-timer state, notifying {@link
	 * #editModeExitedListener}) stay this method's job, not that one's.
	 */
	public void quitEditMode() {
		if (memoryInspectorState == null) {
			return; // Edit mode can only ever have been entered once a segment/selection exists.
		}
		boolean wasEditing = memoryInspectorState.isEditMode();
		int offset = memoryInspectorState.getEditCursorOffset();
		memoryInspectorState.quitEditMode();
		grid.refreshEditMode();
		grid.refreshEditCursor();
		stopBlinkTimer();
		if (wasEditing) {
			if (offset >= 0) {
				select(offset, offset);
			}
			if (editModeExitedListener != null) {
				editModeExitedListener.onEditModeExited();
			}
		}
	}

	private void startBlinkTimer() {
		if (blinkTimer == null) {
			blinkTimer = new Timer(250, e -> grid.advanceBlinkPhase());
		}
		blinkTimer.start();
	}

	private void stopBlinkTimer() {
		if (blinkTimer != null) {
			blinkTimer.stop();
		}
	}

	/**
	 * Translates a raw arrow/Home/End {@link KeyEvent} into a {@link
	 * EditCursorMovement} and hands the actual navigation math
	 * to {@link MutableMemoryInspectorState#moveEditCursor} - ported from {@code
	 * MemoryInspectorControlImpl::KeyDown}'s edit-mode branch, now split so
	 * that math is headlessly unit-testable, free of any Swing dependency.
	 */
	private void handleEditKeyPressed(KeyEvent e) {
		if (!isEditMode()) {
			return;
		}
		EditCursorMovement movement = toEditCursorMovement(e.getKeyCode());
		if (movement == null) {
			return;
		}
		memoryInspectorState.moveEditCursor(movement, grid.getBytesPerLine());
		grid.refreshEditCursor();
		e.consume();
	}

	private static EditCursorMovement toEditCursorMovement(int keyCode) {
		switch (keyCode) {
		case KeyEvent.VK_HOME:
			return EditCursorMovement.HOME;
		case KeyEvent.VK_END:
			return EditCursorMovement.END;
		case KeyEvent.VK_UP:
			return EditCursorMovement.UP;
		case KeyEvent.VK_DOWN:
			return EditCursorMovement.DOWN;
		case KeyEvent.VK_LEFT:
			return EditCursorMovement.LEFT;
		case KeyEvent.VK_RIGHT:
			return EditCursorMovement.RIGHT;
		default:
			return null;
		}
	}

	/**
	 * Hands the typed character straight to {@link
	 * MutableMemoryInspectorState#typeEditChar} - ported from {@code
	 * MemoryInspectorControlImpl::Char}: hex-digit/ASCII data entry, plus Tab
	 * to switch panes, now split so that logic is headlessly unit-testable,
	 * free of any Swing dependency. Deliberately does not call {@link
	 * KeyEvent#consume()} on the {@code HANDLED_AT_BUFFER_END} exit path,
	 * matching this method's own prior behavior.
	 */
	private void handleEditKeyTyped(KeyEvent e) {
		if (!isEditMode()) {
			return;
		}
		EditCharResult result = memoryInspectorState.typeEditChar(e.getKeyChar());
		if (result == EditCharResult.NOT_HANDLED) {
			return;
		}
		grid.refreshEditCursor();
		if (result == EditCharResult.HANDLED_AT_BUFFER_END) {
			quitEditMode();
			return;
		}
		e.consume();
	}

	/** Enables/disables every popup item (and the type submenu) based on whether a segment/selection exists. */
	private void updatePopupMenuItemsState() {
		findMenuItem.setEnabled(canFind(true));
		findNextMenuItem.setEnabled(canFind(false));

		boolean hasSegment = memoryInspectorState != null && memoryInspectorState.hasSegment()
				&& !memoryInspectorState.getSegment().isEmpty();
		selectAllMenuItem.setEnabled(hasSegment);
		selectNextUnknownBlockMenuItem.setEnabled(hasSegment);
		selectGraphicsMenuItem.setEnabled(hasSegment);

		boolean hasSelection = memoryInspectorState != null && memoryInspectorState.hasSelection();
		saveSelectionNoHeaderMenuItem.setEnabled(hasSelection);
		saveSelectionHeaderMenuItem.setEnabled(hasSelection);
		setUnknownBlockToByteMenuItem.setEnabled(hasSelection);
		cutSelectionMenuItem.setEnabled(hasSelection);
		copySelectionMenuItem.setEnabled(hasSelection);
		pasteSelectionMenuItem.setEnabled(hasSelection); // insertion point comes from the current selection's begin offset
		deleteSelectionMenuItem.setEnabled(hasSelection);
		editCommentMenuItem.setEnabled(hasSelection);
		editMenuItem.setEnabled(hasSelection);
		assembleMenuItem.setEnabled(hasSelection);
		startCodeTraceMenuItem.setEnabled(hasSelection && memoryInspectorState.getSegment()
				.isType(memoryInspectorState.getBegin(), MemoryType.UNKNOWN));
		splitAtSelectionMenuItem.setEnabled(hasSelection
				&& memoryInspectorState.getWorkspace().getSegmentList().getCount() < SegmentList.MAX_SEGMENTS
				&& memoryInspectorState.getSegment().canSplitAt(memoryInspectorState.getBegin()));
	}

	/** Whether a find string has been set. */
	public boolean hasFindString() {
		return !findText.isEmpty();
	}

	public String getFindString() {
		return findText;
	}

	public boolean isFindAllSegments() {
		return findAllSegments;
	}

	/** Whether {@link #findNextString} (or a first {@link #findString}) can currently run. */
	public boolean canFind(boolean first) {
		if (memoryInspectorState == null || memoryInspectorState.getSegment() == null) {
			return false;
		}
		if (!first && findSize == 0) {
			return false;
		}
		return true;
	}

	/** Starts a fresh search for {@code findAscii}, from the beginning. */
	public boolean findString(String findAscii, boolean allSegments) {
		findSegmentIndex = allSegments ? 0 : memoryInspectorState.getSegmentIndex();
		findText = findAscii;
		findOffset = 0;
		findSize = findAscii.length();
		findAllSegments = allSegments;
		return findNextString();
	}

	/**
	 * Continues the search, minus the "not found" alert - that is left to the
	 * caller (see {@code Dis6502}), matching how the rest of this class stays
	 * free of its own popups.
	 */
	public boolean findNextString() {
		if (findSegmentIndex != SegmentList.NO_SEGMENT_INDEX) {
			SegmentList segmentList = memoryInspectorState.getWorkspace().getSegmentList();
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

	/** Searches {@code segment} from {@link #findOffset}; selects and returns true on a match. */
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

				if (findSegmentIndex != memoryInspectorState.getSegmentIndex()) {
					memoryInspectorState.getWorkspace().getSegmentList().setSelectedIndex(findSegmentIndex);
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

	/** Reports that edit mode just ended - see {@link #setEditModeExitedListener}. */
	public interface EditModeExitedListener {
		void onEditModeExited();
	}

	/**
	 * Reports a type picked from the popup menu's Change Type submenu, matching
	 * {@link XRefPanel.XRefSelectionListener}'s pattern.
	 */
	public interface TypeSelectionListener {
		void onTypeSelected(MemoryType type);
	}
}
