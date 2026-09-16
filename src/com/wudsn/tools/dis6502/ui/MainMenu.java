/*
 * Copyright (C) 2025 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.ui;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

/**
 * The application's main menu bar.
 * <p>
 * Ported from ui/MainWindowMenu.h / MainWindowMenu.cpp and dis6502.rc's
 * {@code MAIN_MENU} resource, simplified for this first pass: only File
 * &gt; New/Open/Save/Save As/Exit and Help &gt; About are wired to real
 * actions (see {@code Dis6502}); every other menu item is present
 * (matching the .rc structure, for visual completeness) but disabled,
 * since the dialogs/logic they need (equate editing, the Profile/Default
 * Folders dialogs, per-file-type open dialogs, MRU lists) are not ported
 * yet.
 *
 * @author Peter Dell
 */
public final class MainMenu {

	public final JMenuBar menuBar = new JMenuBar();

	public final JMenuItem newWorkspaceMenuItem = new JMenuItem("New Workspace");
	public final JMenuItem openWorkspaceMenuItem = new JMenuItem("Open Workspace...");
	public final JMenuItem saveWorkspaceMenuItem = new JMenuItem("Save Workspace");
	public final JMenuItem saveWorkspaceAsMenuItem = new JMenuItem("Save Workspace As...");
	public final JMenuItem exitMenuItem = new JMenuItem("Exit");

	public final JMenuItem aboutMenuItem = new JMenuItem("About...");

	public MainMenu() {
		menuBar.add(createFileMenu());
		menuBar.add(createDisabledMenu("Equates"));
		menuBar.add(createDisabledMenu("View"));
		menuBar.add(createHelpMenu());
	}

	private JMenu createFileMenu() {
		JMenu menu = new JMenu("File");

		newWorkspaceMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK));
		menu.add(newWorkspaceMenuItem);
		menu.addSeparator();

		openWorkspaceMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK));
		menu.add(openWorkspaceMenuItem);
		menu.add(createDisabledMenu("Open File"));
		menu.add(createDisabledMenu("Add File"));
		menu.addSeparator();

		saveWorkspaceMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
		menu.add(saveWorkspaceMenuItem);
		menu.add(saveWorkspaceAsMenuItem);
		menu.add(createDisabledMenuItem("Save Disassembly Files..."));
		menu.addSeparator();

		menu.add(createDisabledMenu("Recent Workspaces"));
		menu.add(createDisabledMenu("Recent Files"));
		menu.addSeparator();

		menu.add(exitMenuItem);

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
