/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502;

import java.awt.event.KeyEvent;

import com.wudsn.tools.base.gui.KeyStroke;
import com.wudsn.tools.base.repository.Action;
import com.wudsn.tools.base.repository.NLS;

/**
 * Action repository: the main menu's per-item static properties (label with
 * an embedded "&amp;" mnemonic marker, tooltip, accelerator), each field
 * populated reflectively from {@code Actions.properties} when this class is
 * loaded - see {@link com.wudsn.tools.base.repository.NLS} and
 * {@link com.wudsn.tools.base.gui.ElementFactory}, which turns an
 * {@link Action} into an actual {@code JMenu}/{@code JMenuItem}. Field names
 * follow {@code MainMenu_<TopMenu>_<Item>} (nested submenus add one more
 * {@code _<SubMenu>} segment), matching the convention every other WUDSN
 * Swing tool's own {@code Actions} class already uses (e.g. {@code
 * com.wudsn.tools.thecartstudio.Actions}). Extends {@code
 * com.wudsn.tools.base.repository.NLS}, the same base {@link Text}/{@link
 * Texts} use, rather than the plain-{@code String}-only {@code
 * org.eclipse.osgi.util.NLS}, since only this richer base class supports
 * {@link Action}-typed fields.
 * <p>
 * Label text and mnemonic placement are copied verbatim from {@code
 * dis6502.rc}'s {@code MAIN_MENU} resource (dropping each item's
 * {@code \tAccelerator} hint suffix - the accelerator itself becomes a
 * {@link KeyStroke} on the {@link Action} instead), except {@link
 * #MainMenu_File_WriteBootDisk}, which keeps this port's existing, more
 * concise Java wording ("Write Boot Disk..." vs. the C++ source's "Save
 * Disk Image Boot Sectors...") rather than reverting a previous, deliberate
 * choice - only a mnemonic ({@code &amp;W}, unused elsewhere in the File
 * menu) was added, since {@link com.wudsn.tools.base.gui.ElementFactory}
 * requires one. Only the fields whose C++ menu item actually has an
 * accelerator are pre-initialized with one here ({@code new Action(keyCode,
 * modifiers)}); every other field is populated with text only. {@link
 * #MainMenu_File_AddExecutableFile}'s accelerator matches this port's
 * existing behavior (Ctrl+Insert, no Shift) and the {@code .rc} menu item's
 * own hint text, not its {@code ACCELERATORS} table entry (which has an
 * extra, seemingly stale Shift modifier) - not changed here, since this
 * class only carries the pattern forward, it does not re-decide existing
 * accelerator choices.
 * <p>
 * The top-level "File" and "Help" menus reuse {@code
 * com.wudsn.tools.base.Actions}' own {@code MainMenu_File}/{@code
 * MainMenu_Help} fields instead of duplicating them here, matching how
 * every other WUDSN Swing tool's main menu does the same; "Equates" and
 * "View" are specific to this application, so they are declared here
 * instead ({@link #MainMenu_Equates}/{@link #MainMenu_View}).
 * <p>
 * The {@code SegmentListPopupMenu_*}/{@code DisassemblyPopupMenu_*}/{@code
 * MemoryInspectorPopupMenu_*} fields are the same pattern applied to {@link
 * com.wudsn.tools.dis6502.ui.SegmentListPanel}/{@link
 * com.wudsn.tools.dis6502.ui.DisassemblyPanel}/{@link
 * com.wudsn.tools.dis6502.ui.MemoryInspectorPanel}'s right-click popup
 * menus, sourced from {@code dis6502.rc}'s {@code SEGMENT_LIST_POPUP_MENU}/
 * {@code DISASSEMBLY_POPUP_MENU}/{@code MEMORY_INSPECTOR_POPUP_MENU}/{@code
 * MEMORY_INSPECTOR_QUIT_EDIT_POPUP_MENU}, including every keystroke the
 * C++ source's {@code ACCELERATORS} table defines for a popup item
 * (see POPUP_MENU_ACCELERATORS_PLAN.md for how each one was verified,
 * including a found C++ bug at {@link #MemoryInspectorPopupMenu_ChangeType_Dlist}).
 * Populating these fields' accelerators is what makes them the single
 * source of truth for the corresponding keystroke - {@link
 * com.wudsn.tools.dis6502.ui.MemoryInspectorPanel}/{@link
 * com.wudsn.tools.dis6502.ui.DisassemblyPanel}'s window-level {@code
 * InputMap}/{@code ActionMap} bindings read {@code getAccelerator()} off
 * these same fields rather than duplicating the keystroke as a literal.
 * That said, an empirical smoke test (that plan's "Step 0") found a
 * standalone {@code JPopupMenu} item's {@code setAccelerator()} - which
 * {@code ElementFactory.createMenuItem} applies automatically from here -
 * only actually fires while that specific popup instance is open on
 * screen, unlike a {@code JMenuBar}'s items; both panels guard their
 * window-level bindings against double-firing in that narrow window (see
 * their own javadoc) rather than this class leaving the accelerator unset,
 * since the field is still useful as the single source of truth and for
 * the decorative shortcut-hint text {@code createMenuItem} appends to the
 * label. Several C++ menu items - {@code DisassemblyPopupMenu}'s
 * six {@code {0}}-templated label/reference items plus {@code
 * DisassemblyPopupMenu_EditComment}, and roughly half of {@code
 * MemoryInspectorPopupMenu}'s items ({@link
 * #MemoryInspectorPopupMenu_StartCodeTrace}, {@link
 * #MemoryInspectorPopupMenu_ChangeType}, and others) - have no {@code
 * &amp;} mnemonic at all in {@code dis6502.rc}; unlike the main menu (where
 * every added mnemonic already existed in the C++ source), these fields'
 * mnemonics were newly chosen here, picking an unused letter within each
 * popup - see each panel's own javadoc/source for exactly which items this
 * applies to. The six {@code {0}}-templated {@code DisassemblyPopupMenu}
 * fields keep their {@code {0}} placeholder in {@code .label} - {@code
 * DisassemblyPanel} formats it with the actual label name via {@code
 * com.wudsn.tools.base.common.TextUtility#format} each time the popup is
 * shown, then re-derives the mnemonic from the formatted text, since
 * {@link com.wudsn.tools.base.gui.ElementFactory} has no method for
 * applying an already-built {@link Action}'s mnemonic to text that isn't
 * the action's own literal label. {@link #MemoryInspectorPopupMenu_CutSelection}/
 * {@link #MemoryInspectorPopupMenu_PasteSelection}/{@link
 * #MemoryInspectorPopupMenu_DeleteSelection} have no {@code dis6502.rc}
 * counterpart at all - there is no working C++ Delete/Cut/Paste Selection
 * to port (see gap #3's history in {@code plans/REMAINING_GAPS_OVERVIEW.md}) -
 * so their Ctrl+X/Ctrl+V/Delete accelerators are this port's own choice,
 * following standard editor convention rather than any C++ source.
 *
 * @author Peter Dell
 */
public final class Actions extends NLS {

	// Actions: Main Menu - top-level menus specific to this application.
	public static Action MainMenu_Equates;
	public static Action MainMenu_View;

	// Actions: Main Menu - File.
	public static Action MainMenu_File_NewWorkspace = new Action(KeyEvent.VK_N, KeyStroke.M1);
	public static Action MainMenu_File_OpenWorkspace = new Action(KeyEvent.VK_O, KeyStroke.M1);
	public static Action MainMenu_File_OpenFile;
	public static Action MainMenu_File_OpenCassetteImageFile;
	public static Action MainMenu_File_OpenDiskImageExecutableFile;
	public static Action MainMenu_File_OpenDiskImageBootSectors;
	public static Action MainMenu_File_OpenDiskImageSectors;
	public static Action MainMenu_File_OpenExecutableFile = new Action(KeyEvent.VK_O, KeyStroke.M1 | KeyStroke.M2);
	public static Action MainMenu_File_OpenRawFile;
	public static Action MainMenu_File_OpenROMImageFile;
	public static Action MainMenu_File_AddFile;
	public static Action MainMenu_File_AddCassetteImageFile;
	public static Action MainMenu_File_AddDiskImageExecutableFile;
	public static Action MainMenu_File_AddDiskImageBootSectors;
	public static Action MainMenu_File_AddDiskImageSectors;
	public static Action MainMenu_File_AddExecutableFile = new Action(KeyEvent.VK_INSERT, KeyStroke.M1);
	public static Action MainMenu_File_AddRawFile;
	public static Action MainMenu_File_AddROMImageFile;
	public static Action MainMenu_File_SaveWorkspace = new Action(KeyEvent.VK_S, KeyStroke.M1);
	public static Action MainMenu_File_SaveWorkspaceAs;
	public static Action MainMenu_File_SaveDisassemblyFiles;
	public static Action MainMenu_File_WriteBootDisk;
	public static Action MainMenu_File_RecentWorkspaces;
	public static Action MainMenu_File_RecentFiles;
	public static Action MainMenu_File_Exit;

	// Actions: Main Menu - Equates.
	public static Action MainMenu_Equates_ClearSystemEquates;
	public static Action MainMenu_Equates_DisplaySystemEquates;
	public static Action MainMenu_Equates_ClearUserEquates;
	public static Action MainMenu_Equates_EditUserEquates;
	public static Action MainMenu_Equates_DefineUserAddressRange;
	public static Action MainMenu_Equates_OpenUserEquates;
	public static Action MainMenu_Equates_SaveUserEquates;
	public static Action MainMenu_Equates_ExportUserEquates;

	// Actions: Main Menu - View.
	public static Action MainMenu_View_NoDisassembly;
	public static Action MainMenu_View_DoubleFontHeight;
	public static Action MainMenu_View_DefaultFolders;
	public static Action MainMenu_View_Profile;

	// Actions: Main Menu - Help.
	public static Action MainMenu_Help_About;

	// Actions: Segment List popup menu.
	public static Action SegmentListPopupMenu_MoveUp;
	public static Action SegmentListPopupMenu_MoveDown;
	public static Action SegmentListPopupMenu_Merge;
	public static Action SegmentListPopupMenu_Delete;
	public static Action SegmentListPopupMenu_SaveNoHeader;
	public static Action SegmentListPopupMenu_SaveHeader;
	public static Action SegmentListPopupMenu_SaveAll;
	public static Action SegmentListPopupMenu_Properties;

	// Actions: Disassembly popup menu.
	public static Action DisassemblyPopupMenu_FindDef;
	public static Action DisassemblyPopupMenu_EditComment;
	public static Action DisassemblyPopupMenu_Find = new Action(KeyEvent.VK_F, KeyStroke.M1 | KeyStroke.M2);
	public static Action DisassemblyPopupMenu_FindNext = new Action(KeyEvent.VK_F3, KeyStroke.M2);
	public static Action DisassemblyPopupMenu_FindRef2;
	public static Action DisassemblyPopupMenu_FindRef1;
	public static Action DisassemblyPopupMenu_RenameDef;
	public static Action DisassemblyPopupMenu_RenameRef;
	public static Action DisassemblyPopupMenu_AddrRangeDef;
	public static Action DisassemblyPopupMenu_AddrRangeRef;

	// Actions: Memory Inspector header. Not a popup item and not the main
	// menu (see this field's own javadoc on MemoryInspectorPanel for why).
	public static Action MemoryInspectorPanel_DisplayAsScreenCode;

	// Actions: Memory Inspector popup menu.
	public static Action MemoryInspectorPopupMenu_StartCodeTrace = new Action(KeyEvent.VK_T, KeyStroke.M1);
	public static Action MemoryInspectorPopupMenu_ChangeType;
	public static Action MemoryInspectorPopupMenu_ChangeType_Code = new Action(KeyEvent.VK_C, KeyStroke.M2);
	public static Action MemoryInspectorPopupMenu_ChangeType_LowByte = new Action(KeyEvent.VK_O, KeyStroke.M2);
	public static Action MemoryInspectorPopupMenu_ChangeType_HighByte = new Action(KeyEvent.VK_I, KeyStroke.M2);
	public static Action MemoryInspectorPopupMenu_ChangeType_Byte = new Action(KeyEvent.VK_B, KeyStroke.M2);
	public static Action MemoryInspectorPopupMenu_ChangeType_Word = new Action(KeyEvent.VK_W, KeyStroke.M2);
	public static Action MemoryInspectorPopupMenu_ChangeType_Label = new Action(KeyEvent.VK_L, KeyStroke.M2);
	public static Action MemoryInspectorPopupMenu_ChangeType_Symbol = new Action(KeyEvent.VK_X, KeyStroke.M2);
	public static Action MemoryInspectorPopupMenu_ChangeType_Fixup = new Action(KeyEvent.VK_F, KeyStroke.M2);
	public static Action MemoryInspectorPopupMenu_ChangeType_String = new Action(KeyEvent.VK_S, KeyStroke.M2);
	public static Action MemoryInspectorPopupMenu_ChangeType_Sbyte = new Action(KeyEvent.VK_Y, KeyStroke.M2);
	// The .rc's own displayed hint for Display List says Shift+A, but the
	// ACCELERATORS table actually binds Shift+D to it (and Shift+A to Data
	// Store) - see the TODO left at its MENUITEM line in dis6502.rc.
	public static Action MemoryInspectorPopupMenu_ChangeType_Dlist = new Action(KeyEvent.VK_D, KeyStroke.M2);
	public static Action MemoryInspectorPopupMenu_ChangeType_Store = new Action(KeyEvent.VK_A, KeyStroke.M2);
	public static Action MemoryInspectorPopupMenu_ChangeType_Unknown = new Action(KeyEvent.VK_U, KeyStroke.M2);
	public static Action MemoryInspectorPopupMenu_SetUnknownBlockToByte;
	public static Action MemoryInspectorPopupMenu_EditComment;
	public static Action MemoryInspectorPopupMenu_Edit = new Action(KeyEvent.VK_F2, 0);
	public static Action MemoryInspectorPopupMenu_Assemble = new Action(KeyEvent.VK_F8, 0);
	public static Action MemoryInspectorPopupMenu_CutSelection = new Action(KeyEvent.VK_X, KeyStroke.M1);
	public static Action MemoryInspectorPopupMenu_CopySelection = new Action(KeyEvent.VK_C, KeyStroke.M1);
	public static Action MemoryInspectorPopupMenu_PasteSelection = new Action(KeyEvent.VK_V, KeyStroke.M1);
	public static Action MemoryInspectorPopupMenu_DeleteSelection = new Action(KeyEvent.VK_DELETE, 0);
	public static Action MemoryInspectorPopupMenu_SplitAtSelection;
	public static Action MemoryInspectorPopupMenu_Find = new Action(KeyEvent.VK_F, KeyStroke.M1);
	public static Action MemoryInspectorPopupMenu_FindNext = new Action(KeyEvent.VK_F3, 0);
	public static Action MemoryInspectorPopupMenu_SelectNextUnknownBlock = new Action(KeyEvent.VK_F5, 0);
	public static Action MemoryInspectorPopupMenu_SelectGraphics = new Action(KeyEvent.VK_F4, 0);
	public static Action MemoryInspectorPopupMenu_SelectAll = new Action(KeyEvent.VK_A, KeyStroke.M1);
	public static Action MemoryInspectorPopupMenu_SaveSelectionNoHeader;
	public static Action MemoryInspectorPopupMenu_SaveSelectionHeader;
	public static Action MemoryInspectorPopupMenu_QuitEditMode = new Action(KeyEvent.VK_ESCAPE, 0);

	// Actions: Dialog command buttons that are not part of the shared
	// ButtonBar_OK/ButtonBar_Cancel pattern (com.wudsn.tools.base.Actions).
	// AssembleDialog's two labels are copied verbatim from the C++ ASSEMBLEBOX
	// resource (see that class's own javadoc). DefaultFoldersDialog_Browse has
	// no '&' mnemonic at all: the same Action is reused for one button per
	// FolderType row (built via ElementFactory.createButton(action, false)),
	// and every row is visible simultaneously, so a shared mnemonic would
	// collide with itself across rows.
	public static Action AssembleDialog_Assemble;
	public static Action AssembleDialog_Close;
	public static Action EquateDialog_AddModify;
	public static Action EquateDialog_Delete;
	public static Action DiskImageSectorsDialog_AddSector;
	public static Action DiskImageSectorsDialog_RemoveSector;
	public static Action DefaultFoldersDialog_Browse;

	static {
		initializeClass(Actions.class, null);
	}
}
