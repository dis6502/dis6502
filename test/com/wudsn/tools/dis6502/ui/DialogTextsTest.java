/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.ui;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import javax.swing.JDialog;
import javax.swing.SwingUtilities;

import com.wudsn.tools.base.gui.ValueSetField;
import com.wudsn.tools.base.repository.ValueSet;
import com.wudsn.tools.dis6502.Application;
import com.wudsn.tools.dis6502.model.Assert;
import com.wudsn.tools.dis6502.model.Encoding;
import com.wudsn.tools.dis6502.model.ProcessorType;
import com.wudsn.tools.dis6502.model.ProfileLogic;
import com.wudsn.tools.dis6502.model.system.ComputerSystemType;

/**
 * Every dialog constructs, and every text it shows is a real text (see
 * {@link UITest#checkText}) - a dialog whose constructor throws fails this
 * test too. The drop-downs built from a {@link ValueSet} must list the
 * expected values in the expected order and round-trip a value.
 * <p>
 * Skipped - not failed - when the JVM is headless: a {@link JDialog}
 * extends {@link java.awt.Window}, which cannot be constructed without a
 * display; see {@link UITest#isHeadless()}. {@link PanelTextsTest} covers
 * the same ground for panels and the main menu, which need no display.
 *
 * @author Peter Dell
 */
public final class DialogTextsTest {

	private DialogTextsTest() {
	}

	public static void testDialogTexts() throws Exception {
		if (UITest.isHeadless()) {
			Assert.log("DialogTextsTest skipped: no display");
			return;
		}
		SwingUtilities.invokeAndWait(() -> {
			try {
				testDialogs();
				testValueSetFields();
			} catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		});
		Assert.log("DialogTextsTest completed");
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
				UITest.checkText(name + " title", dialog.getTitle());
			}
			UITest.checkTexts(name, dialog);
		} finally {
			dialog.dispose();
		}
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
}
