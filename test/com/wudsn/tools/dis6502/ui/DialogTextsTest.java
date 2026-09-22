/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.ui;

import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;

import com.wudsn.tools.base.gui.ValueSetField;
import com.wudsn.tools.base.repository.ValueSet;
import com.wudsn.tools.dis6502.Application;
import com.wudsn.tools.dis6502.model.Assert;
import com.wudsn.tools.dis6502.model.ComputerSystemType;
import com.wudsn.tools.dis6502.model.Encoding;
import com.wudsn.tools.dis6502.model.ProcessorType;
import com.wudsn.tools.dis6502.model.ProfileLogic;

/**
 * New (not ported), the second test of {@code
 * plans/UI_SMOKE_TESTS_PROPOSAL.md}: every dialog, panel and the main menu
 * constructs, and every text it shows is a real text - not empty, not a
 * property key that failed to resolve, not a {@code "{0}"} template that
 * was meant to be filled in. A missing {@code Texts}/{@code Actions}/{@code
 * ValueSets} property ends the program at class load (see {@code
 * DataTypesTest} for {@code DataTypes}); this catches what a wrong key or a
 * forgotten {@code setText} would leave behind, and a dialog whose
 * constructor throws. The drop-downs built from a {@link ValueSet} must
 * list the expected values in the expected order and round-trip a value.
 * <p>
 * Skipped - not failed - when the JVM is headless: not only can a {@link
 * JDialog} not be constructed without a display, the WUDSN Base {@code
 * Actions} repository cannot even load there ({@code KeyStroke.M1} asks
 * the toolkit for the menu shortcut mask), so no class of the {@code ui}
 * package can. Every UI test of {@code plans/UI_SMOKE_TESTS_PROPOSAL.md}
 * therefore needs a display; see {@link #isHeadless()}.
 *
 * @author Peter Dell
 */
public final class DialogTextsTest {

	/** A property key that did not resolve looks like {@code Dis6502_OpenFileTitle}: word characters and underscores, no space. */
	private static final Pattern KEY_LIKE = Pattern.compile("^[A-Za-z0-9]+(_[A-Za-z0-9]+)+$");

	private DialogTextsTest() {
	}

	public static void testDialogTexts() throws Exception {
		if (isHeadless()) {
			Assert.log("DialogTextsTest skipped: no display");
			return;
		}
		SwingUtilities.invokeAndWait(() -> {
			try {
				testPanelsAndMenu();
				testDialogs();
				testValueSetFields();
			} catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		});
		Assert.log("DialogTextsTest completed");
	}

	/** Whether the UI tests must skip themselves: there is no display, so nothing of the {@code ui} package can be used. */
	public static boolean isHeadless() {
		return GraphicsEnvironment.isHeadless();
	}

	private static void testPanelsAndMenu() throws Exception {
		MainMenu mainMenu = new MainMenu();
		checkTexts("MainMenu", mainMenu.menuBar);
		int menuItems = 0;
		for (Component menu : mainMenu.menuBar.getComponents()) {
			menuItems += countMenuItems((JMenu) menu);
		}
		Assert.longEquals(menuItems, 36); // File 23 (with its Open/Add submenus, the Recent ones empty until filled), Equates 8, View 4, Help 1.

		checkTexts("DisassemblyPanel", new DisassemblyPanel());
		checkTexts("LogPanel", new LogPanel());
		checkTexts("XRefPanel", new XRefPanel());
		checkTexts("SegmentListPanel", new SegmentListPanel());
		checkTexts("MemoryInspectorPanel", new MemoryInspectorPanel());

		// The popup menus are not in the component tree until shown, but their items are public fields.
		checkPublicMenuItemFields("MemoryInspectorPanel", new MemoryInspectorPanel());
		checkPublicMenuItemFields("SegmentListPanel", new SegmentListPanel());
		checkPublicMenuItemFields("DisassemblyPanel", new DisassemblyPanel());
	}

	private static void testDialogs() {
		Application application = new Application();
		checkDialog("AboutDialog", () -> new AboutDialog(null));
		checkDialog("AssembleDialog", () -> new AssembleDialog(null));
		checkDialog("CommentDialog", () -> new CommentDialog(null));
		checkDialog("DefaultFoldersDialog", () -> new DefaultFoldersDialog(null));
		checkDialog("DisassemblyProgressDialog", () -> new DisassemblyProgressDialog(null, application));
		checkDialog("DiskImageExecutableFileDialog", () -> new DiskImageExecutableFileDialog(null));
		checkDialog("DiskImageSectorsDialog", () -> new DiskImageSectorsDialog(null));
		checkDialog("EquateDialog", () -> new EquateDialog(null));
		checkDialog("EquateRangeDialog", () -> new EquateRangeDialog(null));
		checkDialog("LowHighByteDialog", () -> new LowHighByteDialog(null));
		checkDialog("MemoryInspectorFindStringDialog", () -> new MemoryInspectorFindStringDialog(null));
		checkDialog("ProfileDialog", () -> new ProfileDialog(null, new ProfileLogic(application), null));
		checkDialog("RawFileDialog", () -> new RawFileDialog(null));
		checkDialog("SegmentPropertiesDialog", () -> new SegmentPropertiesDialog(null));
		checkDialog("SegmentWriteBootDiskDialog", () -> new SegmentWriteBootDiskDialog(null));
		checkDialog("SelectGraphicsDialog", () -> new SelectGraphicsDialog(null));
		checkDialog("WorkspaceDialog", () -> new WorkspaceDialog(null));
	}

	private static void testValueSetFields() throws Exception {
		WorkspaceDialog workspaceDialog = new WorkspaceDialog(null);
		checkValueSetField(workspaceDialog, "computerSystemField", "Atari 5200 | Atari 800 | C64 | Oric", ComputerSystemType.C64);
		workspaceDialog.dispose();

		SegmentPropertiesDialog segmentPropertiesDialog = new SegmentPropertiesDialog(null);
		checkValueSetField(segmentPropertiesDialog, "processorField", "MOS 6502 | MOS 65C02", ProcessorType.MOS65C02);
		segmentPropertiesDialog.dispose();

		ProfileDialog profileDialog = new ProfileDialog(null, new ProfileLogic(new Application()), null);
		checkValueSetField(profileDialog, "outputEncodingField", "ASCII | ATASCII | UTF-8", Encoding.UTF8);
		profileDialog.dispose();

		SelectGraphicsDialog selectGraphicsDialog = new SelectGraphicsDialog(null);
		checkValueSetField(selectGraphicsDialog, "modeField", "ANTIC 8 ( 40 x  24 Pixels, 4 Colors) | ANTIC 9 ( 80 x  48 Pixels, 2 Colors) | "
				+ "ANTIC A ( 80 x  48 Pixels, 4 Colors) | ANTIC B (160 x  96 Pixels, 2 Colors) | ANTIC C (160 x 192 Pixels, 2 Colors) | "
				+ "ANTIC D (160 x  96 Pixels, 4 Colors) | ANTIC E (160 x 192 Pixels, 4 Colors) | ANTIC F (320 x 192 Pixels, 2 Colors)",
				GraphicMode.ANTIC_D);
		selectGraphicsDialog.dispose();
	}

	private static void checkDialog(String name, Supplier<JDialog> constructor) {
		JDialog dialog = constructor.get();
		try {
			if (dialog.getTitle() != null && !dialog.getTitle().isEmpty()) { // Some titles are only formatted when shown ("Default Folders for {0}").
				checkText(name + " title", dialog.getTitle());
			}
			checkTexts(name, dialog);
		} finally {
			dialog.dispose();
		}
	}

	/** Walks the component tree under {@code root} and checks every visible text on it. */
	private static void checkTexts(String name, Container root) {
		for (Component component : root.getComponents()) {
			if (component.getClass().getName().startsWith("javax.swing.plaf")) {
				continue; // Look-and-feel internals: a combo box's arrow button, a scroll bar's buttons.
			}
			if (component instanceof AbstractButton) {
				checkText(name + " " + component.getClass().getSimpleName(), ((AbstractButton) component).getText());
			} else if (component instanceof JLabel) {
				String text = ((JLabel) component).getText();
				if (text != null && text.trim().length() > 1) { // " ", "-" and "" are legitimate placeholders.
					checkText(name + " JLabel", text);
				}
			}
			if (component instanceof JComponent && ((JComponent) component).getBorder() instanceof TitledBorder) {
				checkText(name + " group", ((TitledBorder) ((JComponent) component).getBorder()).getTitle());
			}
			if (component instanceof JMenu) {
				checkTexts(name, ((JMenu) component).getPopupMenu());
			}
			if (component instanceof Container && !(component instanceof Window)) {
				checkTexts(name, (Container) component);
			}
		}
	}

	/** Every public {@link JMenuItem} field of {@code panel} carries a text - except the ones whose text is only known when the popup shows. */
	private static void checkPublicMenuItemFields(String name, Object panel) throws Exception {
		int count = 0;
		for (Field field : panel.getClass().getFields()) {
			if (Modifier.isStatic(field.getModifiers()) || !JMenuItem.class.isAssignableFrom(field.getType())) {
				continue;
			}
			JMenuItem item = (JMenuItem) field.get(panel);
			boolean dynamic = panel instanceof DisassemblyPanel && (field.getName().startsWith("find") || field.getName().startsWith("rename")
					|| field.getName().startsWith("addrRange"));
			if (!dynamic) {
				checkText(name + "." + field.getName(), item.getText());
				count++;
			}
		}
		Assert.boolEquals(count > 0, true);
	}

	private static void checkValueSetField(JDialog dialog, String fieldName, String expectedItems, ValueSet roundTripValue) throws Exception {
		Field field = dialog.getClass().getDeclaredField(fieldName);
		field.setAccessible(true);
		@SuppressWarnings("unchecked")
		ValueSetField<ValueSet> valueSetField = (ValueSetField<ValueSet>) field.get(dialog);

		List<String> items = new ArrayList<>();
		for (int i = 0; i < valueSetField.getItemCount(); i++) {
			items.add(valueSetField.getItemAt(i).getText());
		}
		Assert.stringEquals(String.join(" | ", items), expectedItems);

		valueSetField.setValue(roundTripValue);
		Assert.boolEquals(valueSetField.getValue() == roundTripValue, true);
	}

	private static void checkText(String where, String text) {
		if (text == null || text.trim().isEmpty()) {
			Assert.fail(where + " has no text.");
		} else if (KEY_LIKE.matcher(text).matches()) {
			Assert.fail(where + " shows a property key instead of a text: '" + text + "'.");
		} else if (text.contains("{0}")) {
			Assert.fail(where + " shows an unfilled template: '" + text + "'.");
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
