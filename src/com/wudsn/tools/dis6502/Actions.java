/*
 * Copyright (C) 2025 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
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
 * com.wudsn.tools.thecartstudio.Actions}), and - unlike {@link Text}, which
 * extends {@code org.eclipse.osgi.util.NLS} - extends {@code
 * com.wudsn.tools.base.repository.NLS} instead, the richer of the two base
 * classes this project uses, since only it supports {@link Action}-typed
 * fields.
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
	public static Action MainMenu_View_DisplayAsScreenCode;
	public static Action MainMenu_View_NoDisassembly;
	public static Action MainMenu_View_DoubleFontHeight;
	public static Action MainMenu_View_DefaultFolders;
	public static Action MainMenu_View_Profile;

	// Actions: Main Menu - Help.
	public static Action MainMenu_Help_About;

	static {
		initializeClass(Actions.class, null);
	}
}
