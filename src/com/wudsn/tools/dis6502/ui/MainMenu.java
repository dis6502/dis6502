/*
 * Copyright (C) 2025 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.ui;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

/**
 * The application's main menu bar.
 * <p>
 * Ported from ui/MainWindowMenu.h / MainWindowMenu.cpp and dis6502.rc's
 * {@code MAIN_MENU} resource, simplified for this first pass: File &gt;
 * New/Open/Save/Save As/Exit, File &gt; Open File/Add File &gt; Executable
 * File, File &gt; Recent Workspaces/Recent Files (see {@link
 * MRUController}), Equates &gt; Clear/Display System Equates, Clear/Edit
 * User Equates, and Open/Save/Export User Equates (see {@link
 * EquateDialog}), View &gt; No Disassembly/Double Font Height/Default
 * Folders... (see {@link DefaultFoldersDialog}), and Help &gt; About are
 * wired to real actions (see {@code Dis6502}); every other menu item is
 * present (matching the .rc structure, for visual completeness) but
 * disabled, since the dialogs/logic they need (the Define Address Range
 * dialog, Display as Screen Code - needs the not-yet-ported memory
 * inspector -, the Profile dialog, the other per-file-type open dialogs -
 * raw/ROM/cassette/disk image) are not ported yet.
 *
 * @author Peter Dell
 */
public final class MainMenu {

	public final JMenuBar menuBar = new JMenuBar();

	public final JMenuItem newWorkspaceMenuItem = new JMenuItem("New Workspace");
	public final JMenuItem openWorkspaceMenuItem = new JMenuItem("Open Workspace...");
	public final JMenuItem openExecutableFileMenuItem = new JMenuItem("Open Executable File...");
	public final JMenuItem addExecutableFileMenuItem = new JMenuItem("Add Executable File...");
	public final JMenuItem saveWorkspaceMenuItem = new JMenuItem("Save Workspace");
	public final JMenuItem saveWorkspaceAsMenuItem = new JMenuItem("Save Workspace As...");
	public final JMenu recentWorkspacesMenu = new JMenu("Recent Workspaces");
	public final JMenu recentFilesMenu = new JMenu("Recent Files");
	public final JMenuItem exitMenuItem = new JMenuItem("Exit");

	public final JMenuItem clearSystemEquatesMenuItem = new JMenuItem("Clear System Equates");
	public final JMenuItem displaySystemEquatesMenuItem = new JMenuItem("Display System Equates");
	public final JMenuItem clearUserEquatesMenuItem = new JMenuItem("Clear User Equates");
	public final JMenuItem editUserEquatesMenuItem = new JMenuItem("Edit User Equates...");
	public final JMenuItem openUserEquatesMenuItem = new JMenuItem("Open User Equates...");
	public final JMenuItem saveUserEquatesMenuItem = new JMenuItem("Save User Equates...");
	public final JMenuItem exportUserEquatesMenuItem = new JMenuItem("Export User Equates...");

	public final JCheckBoxMenuItem noDisassemblyMenuItem = new JCheckBoxMenuItem("No Disassembly");
	public final JCheckBoxMenuItem doubleFontHeightMenuItem = new JCheckBoxMenuItem("Double Font Height");
	public final JMenuItem defaultFoldersMenuItem = new JMenuItem("Default Folders...");

	public final JMenuItem aboutMenuItem = new JMenuItem("About...");

	public MainMenu() {
		menuBar.add(createFileMenu());
		menuBar.add(createEquatesMenu());
		menuBar.add(createViewMenu());
		menuBar.add(createHelpMenu());
	}

	private JMenu createFileMenu() {
		JMenu menu = new JMenu("File");

		newWorkspaceMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK));
		menu.add(newWorkspaceMenuItem);
		menu.addSeparator();

		openWorkspaceMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK));
		menu.add(openWorkspaceMenuItem);

		JMenu openFileMenu = new JMenu("Open File");
		openExecutableFileMenuItem
				.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
		openFileMenu.add(openExecutableFileMenuItem);
		menu.add(openFileMenu);

		JMenu addFileMenu = new JMenu("Add File");
		addExecutableFileMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, InputEvent.CTRL_DOWN_MASK));
		addFileMenu.add(addExecutableFileMenuItem);
		menu.add(addFileMenu);
		menu.addSeparator();

		saveWorkspaceMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
		menu.add(saveWorkspaceMenuItem);
		menu.add(saveWorkspaceAsMenuItem);
		menu.add(createDisabledMenuItem("Save Disassembly Files..."));
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
		JMenu menu = new JMenu("Equates");

		menu.add(clearSystemEquatesMenuItem);
		menu.add(displaySystemEquatesMenuItem);
		menu.addSeparator();

		menu.add(clearUserEquatesMenuItem);
		menu.add(editUserEquatesMenuItem);
		menu.add(createDisabledMenuItem("Define Address Range..."));
		menu.addSeparator();

		menu.add(openUserEquatesMenuItem);
		menu.add(saveUserEquatesMenuItem);
		menu.addSeparator();

		menu.add(exportUserEquatesMenuItem);

		return menu;
	}

	private JMenu createViewMenu() {
		JMenu menu = new JMenu("View");

		menu.add(createDisabledMenuItem("Display as Screen Code"));
		menu.add(noDisassemblyMenuItem);
		menu.add(doubleFontHeightMenuItem);
		menu.addSeparator();

		menu.add(defaultFoldersMenuItem);
		menu.add(createDisabledMenuItem("Profile..."));

		return menu;
	}

	private JMenu createHelpMenu() {
		JMenu menu = new JMenu("Help");
		menu.add(aboutMenuItem);
		return menu;
	}

	private static JMenu createDisabledMenu(String text) {
		JMenu menu = new JMenu(text);
		menu.setEnabled(false);
		return menu;
	}

	private static JMenuItem createDisabledMenuItem(String text) {
		JMenuItem menuItem = new JMenuItem(text);
		menuItem.setEnabled(false);
		return menuItem;
	}
}
