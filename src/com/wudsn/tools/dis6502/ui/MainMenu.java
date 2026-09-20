/*
 * Copyright (C) 2025 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.ui;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

import com.wudsn.tools.base.gui.ElementFactory;
import com.wudsn.tools.dis6502.Actions;

/**
 * The application's main menu bar.
 * <p>
 * Ported from ui/MainWindowMenu.h / MainWindowMenu.cpp and dis6502.rc's
 * {@code MAIN_MENU} resource, simplified for this first pass: File &gt;
 * New/Open/Save/Save As/Save Disassembly Files.../Exit, File &gt; Open
 * File/Add File &gt; Executable/ROM Image/Cassette Image/Raw/Disk Image
 * Executable/Disk Image Boot Sectors/Disk Image Sectors File (see {@link
 * RawFileDialog}/{@link DiskImageExecutableFileDialog}/{@link
 * DiskImageSectorsDialog}), File &gt; Recent Workspaces/Recent Files (see
 * {@link MRUController}), Equates &gt;
 * Clear/Display System Equates, Clear/Edit User Equates, Define Address
 * Range..., and Open/Save/Export User Equates (see {@link EquateDialog}/
 * {@link EquateRangeDialog}), View &gt; Display as Screen Code/No
 * Disassembly/Double Font Height/Default Folders.../Profile... dialogs
 * (see {@link MemoryInspectorPanel}/{@link DefaultFoldersDialog}/{@link
 * ProfileDialog}), and Help &gt; About are wired to real actions (see
 * {@code Dis6502}); every other menu item is present (matching the .rc
 * structure, for visual completeness) but disabled, since the dialogs/
 * logic they need are not ported yet.
 * <p>
 * Every {@code JMenu}/{@code JMenuItem} is built from an {@link Action} via
 * {@link ElementFactory} instead of a literal string label - see {@link
 * Actions} for the per-item label/mnemonic/tooltip/accelerator repository
 * (backed by {@code Actions.properties}) and {@code
 * ACTIONS_ELEMENT_FACTORY_MIGRATION.md} at the repository root for the full
 * rationale. This changes only how each component is constructed, not how
 * it is wired: every field keeps its existing type/name/visibility, and
 * {@code Dis6502} still attaches its own listener to each one directly
 * (see that class's wiring block) - {@link ElementFactory} never attaches a
 * listener itself. The three {@link JCheckBoxMenuItem}s are built via
 * {@link ElementFactory#createCheckBoxMenuItem}, added to {@link
 * ElementFactory} itself (rather than kept as a private helper here) since
 * the same "manually construct, then apply {@link
 * ElementFactory#setButtonTextAndMnemonic}" idiom was already duplicated in
 * {@code com.wudsn.tools.base.gui.AttributeTableColumnChooser} for the same
 * reason - a proper factory method belongs in {@link ElementFactory} once
 * more than one caller needs it. The "Open File"/"Add File" submenu headers and
 * the top-level "Equates"/"View" menus are local to this class ({@code
 * Dis6502} never references them), so they stay plain local variables, not
 * fields - only their {@link Action}s live in {@link Actions}. "File" and
 * "Help" reuse {@code com.wudsn.tools.base.Actions}' own shared {@code
 * MainMenu_File}/{@code MainMenu_Help} fields instead of duplicating them,
 * matching every other WUDSN Swing tool's own main menu.
 *
 * @author Peter Dell
 */
public final class MainMenu {

	public final JMenuBar menuBar = new JMenuBar();

	public final JMenuItem newWorkspaceMenuItem = ElementFactory.createMenuItem(Actions.MainMenu_File_NewWorkspace, "newWorkspaceMenuItem");
	public final JMenuItem openWorkspaceMenuItem = ElementFactory.createMenuItem(Actions.MainMenu_File_OpenWorkspace, "openWorkspaceMenuItem");
	public final JMenuItem openCassetteImageFileMenuItem = ElementFactory.createMenuItem(Actions.MainMenu_File_OpenCassetteImageFile,
			"openCassetteImageFileMenuItem");
	public final JMenuItem addCassetteImageFileMenuItem = ElementFactory.createMenuItem(Actions.MainMenu_File_AddCassetteImageFile,
			"addCassetteImageFileMenuItem");
	public final JMenuItem openExecutableFileMenuItem = ElementFactory.createMenuItem(Actions.MainMenu_File_OpenExecutableFile,
			"openExecutableFileMenuItem");
	public final JMenuItem addExecutableFileMenuItem = ElementFactory.createMenuItem(Actions.MainMenu_File_AddExecutableFile,
			"addExecutableFileMenuItem");
	public final JMenuItem openROMImageFileMenuItem = ElementFactory.createMenuItem(Actions.MainMenu_File_OpenROMImageFile,
			"openROMImageFileMenuItem");
	public final JMenuItem addROMImageFileMenuItem = ElementFactory.createMenuItem(Actions.MainMenu_File_AddROMImageFile,
			"addROMImageFileMenuItem");
	public final JMenuItem openRawFileMenuItem = ElementFactory.createMenuItem(Actions.MainMenu_File_OpenRawFile, "openRawFileMenuItem");
	public final JMenuItem addRawFileMenuItem = ElementFactory.createMenuItem(Actions.MainMenu_File_AddRawFile, "addRawFileMenuItem");
	public final JMenuItem openDiskImageExecutableFileMenuItem = ElementFactory
			.createMenuItem(Actions.MainMenu_File_OpenDiskImageExecutableFile, "openDiskImageExecutableFileMenuItem");
	public final JMenuItem addDiskImageExecutableFileMenuItem = ElementFactory
			.createMenuItem(Actions.MainMenu_File_AddDiskImageExecutableFile, "addDiskImageExecutableFileMenuItem");
	public final JMenuItem openDiskImageBootSectorsMenuItem = ElementFactory.createMenuItem(Actions.MainMenu_File_OpenDiskImageBootSectors,
			"openDiskImageBootSectorsMenuItem");
	public final JMenuItem addDiskImageBootSectorsMenuItem = ElementFactory.createMenuItem(Actions.MainMenu_File_AddDiskImageBootSectors,
			"addDiskImageBootSectorsMenuItem");
	public final JMenuItem openDiskImageSectorsMenuItem = ElementFactory.createMenuItem(Actions.MainMenu_File_OpenDiskImageSectors,
			"openDiskImageSectorsMenuItem");
	public final JMenuItem addDiskImageSectorsMenuItem = ElementFactory.createMenuItem(Actions.MainMenu_File_AddDiskImageSectors,
			"addDiskImageSectorsMenuItem");
	public final JMenuItem saveWorkspaceMenuItem = ElementFactory.createMenuItem(Actions.MainMenu_File_SaveWorkspace, "saveWorkspaceMenuItem");
	public final JMenuItem saveWorkspaceAsMenuItem = ElementFactory.createMenuItem(Actions.MainMenu_File_SaveWorkspaceAs,
			"saveWorkspaceAsMenuItem");
	public final JMenuItem saveDisassemblyFilesMenuItem = ElementFactory.createMenuItem(Actions.MainMenu_File_SaveDisassemblyFiles,
			"saveDisassemblyFilesMenuItem");
	public final JMenuItem writeBootDiskMenuItem = ElementFactory.createMenuItem(Actions.MainMenu_File_WriteBootDisk, "writeBootDiskMenuItem");
	public final JMenu recentWorkspacesMenu = ElementFactory.createMenu(Actions.MainMenu_File_RecentWorkspaces);
	public final JMenu recentFilesMenu = ElementFactory.createMenu(Actions.MainMenu_File_RecentFiles);
	public final JMenuItem exitMenuItem = ElementFactory.createMenuItem(Actions.MainMenu_File_Exit, "exitMenuItem");

	public final JMenuItem clearSystemEquatesMenuItem = ElementFactory.createMenuItem(Actions.MainMenu_Equates_ClearSystemEquates,
			"clearSystemEquatesMenuItem");
	public final JMenuItem displaySystemEquatesMenuItem = ElementFactory.createMenuItem(Actions.MainMenu_Equates_DisplaySystemEquates,
			"displaySystemEquatesMenuItem");
	public final JMenuItem clearUserEquatesMenuItem = ElementFactory.createMenuItem(Actions.MainMenu_Equates_ClearUserEquates,
			"clearUserEquatesMenuItem");
	public final JMenuItem editUserEquatesMenuItem = ElementFactory.createMenuItem(Actions.MainMenu_Equates_EditUserEquates,
			"editUserEquatesMenuItem");
	public final JMenuItem defineUserAddressRangeMenuItem = ElementFactory.createMenuItem(Actions.MainMenu_Equates_DefineUserAddressRange,
			"defineUserAddressRangeMenuItem");
	public final JMenuItem openUserEquatesMenuItem = ElementFactory.createMenuItem(Actions.MainMenu_Equates_OpenUserEquates,
			"openUserEquatesMenuItem");
	public final JMenuItem saveUserEquatesMenuItem = ElementFactory.createMenuItem(Actions.MainMenu_Equates_SaveUserEquates,
			"saveUserEquatesMenuItem");
	public final JMenuItem exportUserEquatesMenuItem = ElementFactory.createMenuItem(Actions.MainMenu_Equates_ExportUserEquates,
			"exportUserEquatesMenuItem");

	public final JCheckBoxMenuItem displayAsScreenCodeMenuItem = ElementFactory.createCheckBoxMenuItem(Actions.MainMenu_View_DisplayAsScreenCode);
	public final JCheckBoxMenuItem noDisassemblyMenuItem = ElementFactory.createCheckBoxMenuItem(Actions.MainMenu_View_NoDisassembly);
	public final JCheckBoxMenuItem doubleFontHeightMenuItem = ElementFactory.createCheckBoxMenuItem(Actions.MainMenu_View_DoubleFontHeight);
	public final JMenuItem defaultFoldersMenuItem = ElementFactory.createMenuItem(Actions.MainMenu_View_DefaultFolders, "defaultFoldersMenuItem");
	public final JMenuItem profileMenuItem = ElementFactory.createMenuItem(Actions.MainMenu_View_Profile, "profileMenuItem");

	public final JMenuItem aboutMenuItem = ElementFactory.createMenuItem(Actions.MainMenu_Help_About, "aboutMenuItem");

	public MainMenu() {
		menuBar.add(createFileMenu());
		menuBar.add(createEquatesMenu());
		menuBar.add(createViewMenu());
		menuBar.add(createHelpMenu());
	}

	private JMenu createFileMenu() {
		JMenu menu = ElementFactory.createMenu(com.wudsn.tools.base.Actions.MainMenu_File);

		menu.add(newWorkspaceMenuItem);
		menu.addSeparator();

		menu.add(openWorkspaceMenuItem);

		JMenu openFileMenu = ElementFactory.createMenu(Actions.MainMenu_File_OpenFile);
		openFileMenu.add(openCassetteImageFileMenuItem);
		openFileMenu.add(openDiskImageExecutableFileMenuItem);
		openFileMenu.add(openDiskImageBootSectorsMenuItem);
		openFileMenu.add(openDiskImageSectorsMenuItem);
		openFileMenu.add(openExecutableFileMenuItem);
		openFileMenu.add(openRawFileMenuItem);
		openFileMenu.add(openROMImageFileMenuItem);
		menu.add(openFileMenu);

		JMenu addFileMenu = ElementFactory.createMenu(Actions.MainMenu_File_AddFile);
		addFileMenu.add(addCassetteImageFileMenuItem);
		addFileMenu.add(addDiskImageExecutableFileMenuItem);
		addFileMenu.add(addDiskImageBootSectorsMenuItem);
		addFileMenu.add(addDiskImageSectorsMenuItem);
		addFileMenu.add(addExecutableFileMenuItem);
		addFileMenu.add(addRawFileMenuItem);
		addFileMenu.add(addROMImageFileMenuItem);
		menu.add(addFileMenu);
		menu.addSeparator();

		menu.add(saveWorkspaceMenuItem);
		menu.add(saveWorkspaceAsMenuItem);
		menu.add(saveDisassemblyFilesMenuItem);
		menu.add(writeBootDiskMenuItem);
		menu.addSeparator();

		recentWorkspacesMenu.setEnabled(false);
		menu.add(recentWorkspacesMenu);
		recentFilesMenu.setEnabled(false);
		menu.add(recentFilesMenu);
		menu.addSeparator();

		menu.add(exitMenuItem);

		return menu;
	}

	private JMenu createEquatesMenu() {
		JMenu menu = ElementFactory.createMenu(Actions.MainMenu_Equates);

		menu.add(clearSystemEquatesMenuItem);
		menu.add(displaySystemEquatesMenuItem);
		menu.addSeparator();

		menu.add(clearUserEquatesMenuItem);
		menu.add(editUserEquatesMenuItem);
		menu.add(defineUserAddressRangeMenuItem);
		menu.addSeparator();

		menu.add(openUserEquatesMenuItem);
		menu.add(saveUserEquatesMenuItem);
		menu.addSeparator();

		menu.add(exportUserEquatesMenuItem);

		return menu;
	}

	private JMenu createViewMenu() {
		JMenu menu = ElementFactory.createMenu(Actions.MainMenu_View);

		menu.add(displayAsScreenCodeMenuItem);
		menu.add(noDisassemblyMenuItem);
		menu.add(doubleFontHeightMenuItem);
		menu.addSeparator();

		menu.add(defaultFoldersMenuItem);
		menu.add(profileMenuItem);

		return menu;
	}

	private JMenu createHelpMenu() {
		JMenu menu = ElementFactory.createMenu(com.wudsn.tools.base.Actions.MainMenu_Help);
		menu.add(aboutMenuItem);
		return menu;
	}

}
