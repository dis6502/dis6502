/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.ui;

import java.awt.Component;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;

import com.wudsn.tools.dis6502.model.Assert;

/**
 * The main menu and every panel construct, and every text they show is a
 * real text - not empty, not a property key that failed to resolve, not a
 * {@code "{0}"} template that was meant to be filled in (see {@link
 * UITest#checkText}). A missing {@code Texts}/{@code Actions}/{@code
 * ValueSets} property ends the program at class load instead (see {@code
 * DataTypesTest} for {@code DataTypes}). Every menu and popup also gets
 * its mnemonics checked: every item has one, and no two items in the same
 * menu (or the same panel's popup) share one - {@link
 * com.wudsn.tools.base.gui.ElementFactory} itself only ever checks that a
 * label has a mnemonic at all, not that it is unique among its siblings.
 * <p>
 * Always runs, headless or not: {@link MainMenu} and every panel are
 * plain {@link javax.swing.JComponent}s, not {@link java.awt.Window}s, so
 * they construct fine without a display - unlike {@link DialogTextsTest},
 * whose dialogs need one.
 *
 * @author Peter Dell
 */
public final class PanelTextsTest {

	private PanelTextsTest() {
	}

	public static void testPanelTexts() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			try {
				testPanelsAndMenu();
			} catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		});
		Assert.log("PanelTextsTest completed");
	}

	private static void testPanelsAndMenu() throws Exception {
		MainMenu mainMenu = new MainMenu();
		UITest.checkTexts("MainMenu", mainMenu.menuBar);
		checkMenuMnemonics(mainMenu.menuBar);
		int menuItems = 0;
		for (Component menu : mainMenu.menuBar.getComponents()) {
			menuItems += countMenuItems((JMenu) menu);
		}
		Assert.longEquals(menuItems, 37); // File 23 (with its Open/Add submenus, the Recent ones empty until filled), Equates 8, View 5, Help 1.

		UITest.checkTexts("DisassemblyPanel", new DisassemblyPanel());
		UITest.checkTexts("LogPanel", new LogPanel());
		UITest.checkTexts("XRefPanel", new XRefPanel());
		UITest.checkTexts("SegmentListPanel", new SegmentListPanel());
		UITest.checkTexts("MemoryInspectorPanel", new MemoryInspectorPanel());

		// The popup menus are not in the component tree until shown, but their items are public fields.
		checkPublicMenuItemFields("MemoryInspectorPanel", new MemoryInspectorPanel());
		checkPublicMenuItemFields("SegmentListPanel", new SegmentListPanel());
		checkPublicMenuItemFields("DisassemblyPanel", new DisassemblyPanel());
	}

	/**
	 * Every public {@link JMenuItem} field of {@code panel} carries a text
	 * and a mnemonic unique among the panel's other fields - except the
	 * ones whose text is only known when the popup shows.
	 */
	private static void checkPublicMenuItemFields(String name, Object panel) throws Exception {
		int count = 0;
		Set<Character> mnemonics = new HashSet<>();
		for (Field field : panel.getClass().getFields()) {
			if (Modifier.isStatic(field.getModifiers()) || !JMenuItem.class.isAssignableFrom(field.getType())) {
				continue;
			}
			JMenuItem item = (JMenuItem) field.get(panel);
			boolean dynamic = panel instanceof DisassemblyPanel && (field.getName().startsWith("find") || field.getName().startsWith("rename")
					|| field.getName().startsWith("addrRange"));
			if (!dynamic) {
				UITest.checkText(name + "." + field.getName(), item.getText());
				checkMnemonicUnique(name + "." + field.getName(), item.getMnemonic(), mnemonics);
				count++;
			}
		}
		Assert.boolEquals(count > 0, true);
	}

	/** Checks every top-level menu's own mnemonic, then recurses into it via {@link #checkMenuMnemonics(JMenu)}. */
	private static void checkMenuMnemonics(JMenuBar menuBar) {
		Set<Character> topLevelMnemonics = new HashSet<>();
		for (Component component : menuBar.getComponents()) {
			JMenu menu = (JMenu) component;
			checkMnemonicUnique("MainMenu." + menu.getText(), menu.getMnemonic(), topLevelMnemonics);
			checkMenuMnemonics(menu);
		}
	}

	/** Checks that {@code menu}'s direct children (items and submenu headers) have unique mnemonics among themselves, then recurses into every submenu. */
	private static void checkMenuMnemonics(JMenu menu) {
		Set<Character> mnemonics = new HashSet<>();
		for (Component component : menu.getPopupMenu().getComponents()) {
			if (component instanceof JMenuItem) {
				JMenuItem item = (JMenuItem) component;
				checkMnemonicUnique(menu.getText() + "." + item.getText(), item.getMnemonic(), mnemonics);
			}
			if (component instanceof JMenu) {
				checkMenuMnemonics((JMenu) component);
			}
		}
	}

	/** Fails if {@code mnemonic} is 0 (missing) or already in {@code mnemonics}; otherwise records it. */
	private static void checkMnemonicUnique(String where, int mnemonic, Set<Character> mnemonics) {
		if (mnemonic == 0) {
			Assert.fail(where + " has no mnemonic.");
		} else if (!mnemonics.add(Character.toUpperCase((char) mnemonic))) {
			Assert.fail(where + " has a mnemonic already used by another item in the same menu.");
		}
	}

	private static int countMenuItems(JMenu menu) {
		int count = 0;
		for (Component component : menu.getPopupMenu().getComponents()) {
			if (component instanceof JMenu) {
				count += countMenuItems((JMenu) component);
			} else if (component instanceof JMenuItem) {
				count++;
			}
		}
		return count;
	}
}
