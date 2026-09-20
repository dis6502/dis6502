/*
 * Copyright (C) 2025 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.ui;

import java.awt.BorderLayout;
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
import javax.swing.border.TitledBorder;

import com.wudsn.tools.base.gui.ElementFactory;
import com.wudsn.tools.base.repository.Action;
import com.wudsn.tools.dis6502.Actions;
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
 * All, Select Graphics, XRef navigation, a Split at Selection result).
 * {@link #findString}/{@link #findNextString}/{@link #canFind} from
 * {@code MemoryInspector::FindString}/{@code
 * FindNextString}/{@code CanFind}, triggered from {@link #findMenuItem}/
 * {@link #findNextMenuItem}.
 * <p>
 * This panel has no toolbar of per-command buttons - almost every command is
 * reachable only from the right-click popup menu ({@link #maybeShowPopup},
 * attached to the grid), ported from ui/MemoryInspectorPopupMenu.h/.cpp's
 * {@code MEMORY_INSPECTOR_POPUP_MENU} resource and following
 * {@link SegmentListPanel}'s pattern: every item is a public {@code JMenuItem}
 * field wired directly by {@code Dis6502}, never a hidden {@code JButton} kept
 * around only to be {@code doClick()}d - this panel used to also have a toolbar
 * with a button per command, each popup item {@code
 * doClick()}ing its button, until both the redundant toolbar and that
 * indirection were removed. {@link #displayAsScreenCodeButton} is the one
 * exception - a real header button, not a popup item at all, since the C++
 * source's own {@code ID_VIEW_DISPLAYASSCREENCODE} lives in the main menu,
 * not this panel's popup; moving it here (see that field's own javadoc) is
 * this port's own UI choice, not a fidelity port, but it stays wired
 * directly by {@code Dis6502} like everything else, not a {@code
 * doClick()} proxy. {@link #splitAtSelectionMenuItem} (from
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
 * {@link #selectGraphicsMenuItem} (from {@code
 * MemoryInspector::ShowSelectSpritesDialog}/IDM_DUMP_SELECT_SPRITES, ported as
 * {@link SelectGraphicsDialog}/{@link GraphicPanel}/{@link GraphicMode} -
 * renamed from the C++ source's "Sprite" terminology, see {@code
 * SelectGraphicsDialog}'s own javadoc for why) is non-mutating like the
 * other Select* items - it just ends in a call to {@link #select}, {@link
 * SelectGraphicsDialog}'s own javadoc has the details.
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
 * own stored type is unknown/invalid. Cut, Paste (before/after selection), and
 * Delete are not in this menu, matching this class's own note above on why
 * Delete/Cut/Paste Selection are not ported.
 * <p>
 * {@link #editMenuItem}/{@link #enterEditMode()}/{@link #quitEditMode()} and
 * the keyboard handling wired up in the constructor port
 * {@code MemoryInspector::SetEditMode}/{@code MainController::QuitEditMode}/
 * {@code MemoryInspectorControlImpl::KeyDown}/{@code Char}/{@code Timer}/
 * {@code LButtonDblClk} - in-place hex/ASCII editing of the selected byte(s),
 * entered via F2, {@link #editMenuItem}, or a double-click, exited via Esc or
 * {@link #quitEditModeMenuItem} (shown, while editing, in a separate ad hoc
 * popup that replaces the normal one - {@code MainMemoryInspector::
 * PerformCommands}'s modal gate on every other command while editing). Unlike
 * the C++ source, the actual cursor state and navigation/writing logic - not
 * just the on/off flag - live on {@link #memoryInspectorState} ({@link
 * MutableMemoryInspectorState#moveEditCursor}/{@link
 * MutableMemoryInspectorState#typeEditChar}, applying {@code Char}'s {@link
 * MemoryType#SBYTE} ASCII transform via {@link
 * MemoryType#toSbyteInternalCode}), not here or in {@link
 * MemoryInspectorGridPanel} - a deliberate departure from the C++ design (see
 * {@link MutableMemoryInspectorState}'s own javadoc) so that logic can be exercised
 * by a plain, headless unit test. This class keeps only the Swing-specific
 * glue: {@link #handleEditKeyPressed}/{@link #handleEditKeyTyped} translate
 * a raw {@link java.awt.event.KeyEvent} into a semantic call on {@link
 * #memoryInspectorState}, then tell {@link MemoryInspectorGridPanel} to
 * notice via {@link MemoryInspectorGridPanel#refreshEditCursor}/{@link
 * MemoryInspectorGridPanel#refreshEditMode} - that class reads the current
 * selection/edit-mode values live off {@link #memoryInspectorState} itself
 * (through {@link com.wudsn.tools.dis6502.model.MemoryInspectorState},
 * narrowing what a pure painter can do to it) rather than being handed each
 * value as it changes; this class also owns focus/mouse handling, the
 * popup-menu swap, and the blink timer. Two confirmed C++
 * quirks are deliberately fixed here rather than replicated: typing past the
 * end of the buffer bypasses {@code MainController::QuitEditMode} in C++, so
 * its disassembly refresh is skipped on that one exit path only (see the
 * TODO left in {@code MemoryInspectorControlImpl.cpp}'s {@code Char} method)
 * - {@link #quitEditMode()} is the single exit point here, so every exit
 * path refreshes uniformly; and C++ never resyncs the selection/title to the
 * cursor's final position on exit, leaving it at wherever editing started -
 * {@link #quitEditMode()} calls {@link #select} with the cursor's final
 * offset instead. A third quirk is deliberately NOT replicated: {@code
 * Char}'s printable-ASCII gate excludes {@code '~'}, {@code '{'}, {@code
 * '}'} for no evident reason (it looks like an unintentional leftover, not
 * designed behavior) - {@link MutableMemoryInspectorState#typeEditChar} accepts
 * the full printable range instead.
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
	private static final Action[] TYPE_SUBMENU_ACTIONS = { Actions.MemoryInspectorPopupMenu_ChangeType_Code,
			Actions.MemoryInspectorPopupMenu_ChangeType_LowByte, Actions.MemoryInspectorPopupMenu_ChangeType_HighByte,
			Actions.MemoryInspectorPopupMenu_ChangeType_Byte, Actions.MemoryInspectorPopupMenu_ChangeType_Word,
			Actions.MemoryInspectorPopupMenu_ChangeType_Label, Actions.MemoryInspectorPopupMenu_ChangeType_Symbol,
			Actions.MemoryInspectorPopupMenu_ChangeType_Fixup, Actions.MemoryInspectorPopupMenu_ChangeType_String,
			Actions.MemoryInspectorPopupMenu_ChangeType_Sbyte, Actions.MemoryInspectorPopupMenu_ChangeType_Dlist,
			Actions.MemoryInspectorPopupMenu_ChangeType_Store, Actions.MemoryInspectorPopupMenu_ChangeType_Unknown };

	// Flat empty base border instead of createTitledBorder(String)'s L&F-default
	// one, which paints a bevel/etched box around the whole panel on this
	// project's native (Windows) look and feel - not wanted, just the title text.
	private final TitledBorder titledBorder = BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), "No segment selected.");
	private final MemoryInspectorGridPanel grid = new MemoryInspectorGridPanel();

	/**
	 * Toggles {@link #setDisplayAsScreenCode} - moved here from a {@code
	 * View} main-menu checkbox item (which the C++ source's own equivalent,
	 * {@code ID_VIEW_DISPLAYASSCREENCODE}, still is), since it only ever
	 * affects this one panel. Built via {@link Actions#MemoryInspectorPanel_DisplayAsScreenCode}/
	 * {@link ElementFactory#createToggleButton}, the same {@code Action}-backed
	 * pattern every other control in this port uses - unlike {@link
	 * DisassemblyPanel#findButton}/{@link DisassemblyPanel#findNextButton},
	 * which predate that pattern and stay plain since they were never menu
	 * items even in the C++ source. {@code Dis6502} wires this directly, the
	 * same as every other control here - not a hidden {@code doClick()}
	 * proxy, since this button is now this command's only home.
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
	 * Actions}' own javadoc for label/mnemonic sourcing, including the several
	 * items with no mnemonic in {@code dis6502.rc} that were given one here.
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
	public final JMenuItem copySelectionMenuItem = ElementFactory.createMenuItem(Actions.MemoryInspectorPopupMenu_CopySelection,
			"copySelectionMenuItem");
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

	private MutableMemoryInspectorState memoryInspectorState;
	private int selectionAnchorOffset = -1;

	private Timer blinkTimer;

	private int findSegmentIndex = SegmentList.NO_SEGMENT_INDEX;
	private int findOffset;
	private int findSize;
	private boolean findAllSegments = true;
	private String findText = "";

	public MemoryInspectorPanel() {
		super(new BorderLayout());
		setBorder(titledBorder);

		JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		headerPanel.add(displayAsScreenCodeButton);
		add(headerPanel, BorderLayout.NORTH);

		JScrollPane scrollPane = new JScrollPane(grid);
		// Splitters already separate the part windows - the scroll pane's own
		// L&F-default border would just draw a redundant line right next to them.
		scrollPane.setBorder(BorderFactory.createEmptyBorder());
		add(scrollPane, BorderLayout.CENTER);

		grid.setFocusable(true);
		buildPopupMenu();
		editModePopupMenu.add(quitEditModeMenuItem);
		MouseAdapter mouseHandler = new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				grid.requestFocusInWindow();
				maybeShowPopup(e);
				if (SwingUtilities.isLeftMouseButton(e) && !e.isPopupTrigger() && !isEditMode()) {
					beginByteSelection(e);
				}
			}

			@Override
			public void mouseDragged(MouseEvent e) {
				if (SwingUtilities.isLeftMouseButton(e) && !isEditMode()) {
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

			@Override
			public void mouseClicked(MouseEvent e) {
				if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
					enterEditModeAtPoint(e.getX(), e.getY());
				}
			}
		};
		grid.addMouseListener(mouseHandler);
		grid.addMouseMotionListener(mouseHandler);

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
	 * {@code WHEN_FOCUSED}) - matching the C++ source's accelerator table, where
	 * {@code ID_DUMP_EDIT}/{@code ID_DUMP_QUIT_EDIT} work regardless of which
	 * child control currently has focus, as long as the window is active. The
	 * keystrokes themselves come from {@link Actions#MemoryInspectorPopupMenu_Edit}/
	 * {@link Actions#MemoryInspectorPopupMenu_QuitEditMode} - see
	 * {@link #bindPopupMenuAccelerators} for why every binding here (and there)
	 * is guarded by {@link #isAnyPopupMenuVisible}.
	 */
	private void bindEditModeKeys() {
		bindAccelerator(Actions.MemoryInspectorPopupMenu_Edit, this::enterEditMode);
		bindAccelerator(Actions.MemoryInspectorPopupMenu_QuitEditMode, this::quitEditMode);
	}

	/**
	 * Binds the remaining popup-menu accelerators from {@code dis6502.rc}'s
	 * {@code ACCELERATORS} table (see POPUP_MENU_ACCELERATORS_PLAN.md) at the
	 * window level, the same {@code WHEN_IN_FOCUSED_WINDOW} scope as
	 * {@link #bindEditModeKeys}. Each keystroke comes from the same {@link Action}
	 * in {@code com.wudsn.tools.dis6502.Actions} that built the corresponding
	 * menu item - not a literal duplicated here - so {@code Actions.java} stays
	 * the single source of truth for every item's keystroke, the same as it
	 * already is for its label/mnemonic.
	 * <p>
	 * Populating these {@link Action}s' accelerators also makes {@code
	 * ElementFactory.createMenuItem} apply them to the menu item itself (nice,
	 * free shortcut-hint text next to the label) - but an empirical smoke test
	 * written for that plan's "Step 0" found a standalone {@code JPopupMenu}
	 * item's accelerator this way only actually fires while that specific popup
	 * instance is open on screen, not window-wide like the C++ accelerator
	 * table. While {@link #popupMenu} (or {@link #editModePopupMenu}) is open,
	 * that means both the item's own live-but-narrow accelerator and this
	 * method's window-level binding could fire for the same keystroke - {@link
	 * #isAnyPopupMenuVisible} guards every binding here (and in
	 * {@link #bindEditModeKeys}) against that double-fire, deferring to the
	 * item's own accelerator whenever a popup happens to already be showing.
	 * Calling {@code doClick()} on the already-correctly-wired, already-visible
	 * item is the same sanctioned exception to avoiding hidden {@code
	 * doClick()} indirection {@link #bindEditModeKeys} relies on too (calling
	 * {@link #enterEditMode()}/{@link #quitEditMode()} directly), not a proxy to
	 * a hidden component.
	 * <p>
	 * One keystroke does not fit the "defer to the item's own live accelerator"
	 * story above: Esc, while {@link #popupMenu}/{@link #editModePopupMenu} is
	 * open, fires neither {@link #quitEditModeMenuItem}'s own accelerator nor
	 * this method's window-level binding (confirmed with a dedicated smoke
	 * test) - Swing's own menu-cancel handling (closing the open popup)
	 * consumes Esc before either accelerator mechanism sees it, the same way
	 * {@code TrackPopupMenu}'s internal modal loop consumes it in the C++
	 * source ({@code ui/MemoryInspector.cpp}) before it ever reaches {@code
	 * TranslateAccelerator}/the app's {@code ACCELERATORS} table. So pressing
	 * Esc while either popup happens to be open just closes that popup,
	 * leaving edit mode active - matching the C++ original's own limitation,
	 * not a Java-only regression; Esc still reliably exits edit mode the rest
	 * of the time, i.e. whenever neither popup is currently open.
	 */
	private void bindPopupMenuAccelerators() {
		bindAccelerator(Actions.MemoryInspectorPopupMenu_StartCodeTrace, startCodeTraceMenuItem);
		bindAccelerator(Actions.MemoryInspectorPopupMenu_Assemble, assembleMenuItem);
		bindAccelerator(Actions.MemoryInspectorPopupMenu_CopySelection, copySelectionMenuItem);
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
		popupMenu.add(copySelectionMenuItem);
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

	/**
	 * Ported from MemoryInspectorControlImpl::RButtonDown's notification, handled
	 * by MainMemoryInspector to show MemoryInspectorPopupMenu.
	 */
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
	 * Ports {@code TypeSubMenu::Update}'s enabled/checked logic for the Set Type
	 * submenu - every other popup item's enabled state is set directly in
	 * {@link #updatePopupMenuItemsState}.
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
	 * Reports that edit mode just ended, for any reason - ported from {@code
	 * MainController::QuitEditMode}'s {@code UpdateDisassembly()} call. {@code
	 * Dis6502} uses this to re-run the disassembly, matching
	 * {@code performShowAssembleDialog}/{@code performGuessCode}'s own
	 * mutate-then-refresh pattern.
	 */
	public void setEditModeExitedListener(EditModeExitedListener editModeExitedListener) {
		this.editModeExitedListener = editModeExitedListener;
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
	public void segmentChanged(MutableMemoryInspectorState memoryInspectorState) {
		quitEditMode(); // Ported from MainSegment::Selected/MainXRef's and Main::ClearWorkspace's QuitEditMode() calls.
		this.memoryInspectorState = memoryInspectorState;
		Segment segment = memoryInspectorState.getSegment();
		boolean hasData = segment != null && !segment.isHeader(FileHeader.SDX_SYM_DEFINED)
				&& !segment.isSDXRelocBlkWithoutData();

		grid.setMemoryInspectorState(memoryInspectorState);
		grid.setSegment(hasData ? segment : null);
		memoryInspectorState.clearSelection();
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
		Segment segment = memoryInspectorState == null ? null : memoryInspectorState.getSegment();
		if (segment == null) {
			titledBorder.setTitle("No segment selected.");
			return;
		}

		int segmentNumber = memoryInspectorState.getSegmentIndex() + 1;
		String prefix;
		int begin;
		int end;
		int size;
		String title;
		if (memoryInspectorState.hasSelection()) {
			prefix = "Selection";
			begin = segment.wBegin + memoryInspectorState.getBegin();
			end = segment.wBegin + memoryInspectorState.getEnd();
			size = memoryInspectorState.getSize();
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
		if (memoryInspectorState == null || memoryInspectorState.getSegment() == null
				|| memoryInspectorState.getSegment().isEmpty()) {
			return;
		}
		memoryInspectorState.setSelection(begin, end);
		grid.refreshSelection();
		updateTitle();
		updatePopupMenuItemsState();
		// grid.refreshSelection() only repaints the grid, a child component -
		// the titled border's text is painted by this panel itself, so it
		// needs its own repaint to actually show the new title on screen.
		repaint();
	}

	/** Ported from MemoryInspector::ClearSelection. */
	public void clearSelection() {
		if (memoryInspectorState != null) {
			memoryInspectorState.clearSelection();
		}
		grid.refreshSelection();
		updateTitle();
		updatePopupMenuItemsState();
		repaint();
	}

	/**
	 * Ported from MemoryInspector::SelectAll: selects the whole segment.
	 * {@code end} is passed as the segment's size (one past the last valid offset),
	 * matching the C++ version -
	 * {@link #select}/{@link MutableMemoryInspectorState#setSelection} clamp it back
	 * down to the last valid offset, the same way the C++ version's own
	 * {@code Select}/ {@code MemoryInspectorState::SetSelection} do.
	 */
	public void selectAll() {
		if (memoryInspectorState == null || memoryInspectorState.getSegment() == null
				|| memoryInspectorState.getSegment().isEmpty()) {
			return;
		}
		select(0, memoryInspectorState.getSegment().getSize());
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
	 * Ported from MemoryInspector::SetUnknownBlockToByte
	 * (IDM_DUMP_SET_UNKNOWN_BLOCK_TO_BYTE): reclassifies every byte in the
	 * selection that is still {@link MemoryType#UNKNOWN} - and not the repurposed
	 * type slot right after a {@link MemoryType#LOBYTE}/{@link MemoryType#HIBYTE}
	 * byte (see {@link MemoryType}'s javadoc) - as {@link MemoryType#BYTE}. Like
	 * {@link #setType}, the caller is responsible for re-running the disassembly
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
	 * Ported from MemoryInspector::Guess (IDM_DUMP_START_CODE_TRACE): runs
	 * {@link GuessCodeLogic} starting from the selection's first byte. Like
	 * {@link #setType}/{@link #setUnknownBlockToByte}, the caller is responsible
	 * for re-running the disassembly afterward.
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
	 * Ported from {@code MainMemoryInspector::Edit} (F2/{@code IDM_DUMP_EDIT}):
	 * enters edit mode at the current selection's first byte, snapping a
	 * multi-byte selection down to that one byte - matching {@code
	 * MemoryInspector::SetEditMode}'s {@code IsEmpty()} guard and its
	 * {@code SetSelection(nBegin, nBegin)} call - with the cursor starting on the
	 * hex pane's high nibble.
	 */
	public void enterEditMode() {
		enterEditModeAt(-1, EditPane.HEX_HIGH);
	}

	/**
	 * Ported from {@code MemoryInspectorControlImpl::LButtonDblClk}'s two-step
	 * sequence: enters edit mode the normal way (snapping to the selection's
	 * first byte), then immediately repositions the cursor to the exact nibble/
	 * character double-clicked.
	 */
	private void enterEditModeAtPoint(int x, int y) {
		MemoryInspectorGridPanel.CellHit hit = grid.cellAtPoint(x, y);
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
	}

	/**
	 * Ported from {@code MainController::QuitEditMode}: leaves edit mode, if it
	 * was active - a no-op otherwise, matching the C++ source's {@code
	 * oldEditMode} check. Unlike the C++ source, this is the single exit point
	 * for every way edit mode can end (Esc, {@link #quitEditModeMenuItem},
	 * typing past the end of the buffer, or the segment changing), so the
	 * selection-resync below and {@link #editModeExitedListener} both fire
	 * uniformly on every exit - see this class's javadoc for the two C++ exit
	 * quirks this fixes. {@link MutableMemoryInspectorState#quitEditMode()} only
	 * releases the model-level lock; the UI-facing consequences (the
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

		boolean hasSegment = memoryInspectorState != null && memoryInspectorState.hasSegment()
				&& !memoryInspectorState.getSegment().isEmpty();
		selectAllMenuItem.setEnabled(hasSegment);
		selectNextUnknownBlockMenuItem.setEnabled(hasSegment);
		selectGraphicsMenuItem.setEnabled(hasSegment);

		boolean hasSelection = memoryInspectorState != null && memoryInspectorState.hasSelection();
		saveSelectionNoHeaderMenuItem.setEnabled(hasSelection);
		saveSelectionHeaderMenuItem.setEnabled(hasSelection);
		setUnknownBlockToByteMenuItem.setEnabled(hasSelection);
		copySelectionMenuItem.setEnabled(hasSelection);
		editCommentMenuItem.setEnabled(hasSelection);
		editMenuItem.setEnabled(hasSelection);
		assembleMenuItem.setEnabled(hasSelection);
		startCodeTraceMenuItem.setEnabled(hasSelection && memoryInspectorState.getSegment()
				.isType(memoryInspectorState.getBegin(), MemoryType.UNKNOWN));
		splitAtSelectionMenuItem.setEnabled(hasSelection
				&& memoryInspectorState.getWorkspace().getSegmentList().getCount() < SegmentList.MAX_SEGMENTS
				&& memoryInspectorState.getSegment().canSplitAt(memoryInspectorState.getBegin()));
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
		if (memoryInspectorState == null || memoryInspectorState.getSegment() == null) {
			return false;
		}
		if (!first && findSize == 0) {
			return false;
		}
		return true;
	}

	/** Ported from MemoryInspector::FindString. */
	public boolean findString(String findAscii, boolean allSegments) {
		findSegmentIndex = allSegments ? 0 : memoryInspectorState.getSegmentIndex();
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
