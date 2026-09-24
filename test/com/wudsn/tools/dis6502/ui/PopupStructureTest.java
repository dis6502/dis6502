/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.ui;

import java.awt.Component;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import com.wudsn.tools.dis6502.model.Assert;
import com.wudsn.tools.dis6502.model.DisassemblyLine;
import com.wudsn.tools.dis6502.model.DisassemblyResult;
import com.wudsn.tools.dis6502.model.DisassemblySectionType;
import com.wudsn.tools.dis6502.model.MemoryType;
import com.wudsn.tools.dis6502.model.Workspace;
import com.wudsn.tools.dis6502.model.system.ComputerSystemType;

/**
 * The popup menu a right-click on a {@code jsr} line builds - its item
 * set, enabled states and the immediate-type submenu's check mark - read
 * from the real {@link JPopupMenu} after a synthesized popup-trigger
 * {@link MouseEvent} went through the grid's own mouse handling.
 * <p>
 * Needs a display (see {@link UITest#isHeadless()}): showing a popup
 * needs a real, showing parent component, unlike {@link RenderingTest}'s
 * off-screen painting.
 *
 * @author Peter Dell
 */
public final class PopupStructureTest {

	private PopupStructureTest() {
	}

	public static void testPopupStructure() throws Exception {
		if (UITest.isHeadless()) {
			Assert.log("PopupStructureTest skipped: no display");
			return;
		}
		Workspace workspace = RenderingTest.disassembleSample();
		SwingUtilities.invokeAndWait(() -> {
			try {
				testPopupStructure(workspace);
			} catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		});
		Assert.log("PopupStructureTest completed");
	}

	/** The popup for a {@code jsr} line: Navigate to Definition offered, Back disabled (no history), the immediate submenu disabled. */
	private static void testPopupStructure(Workspace workspace) throws Exception {
		DisassemblyPanel panel = new DisassemblyPanel();
		ComputerFont font = ComputerFont.get(ComputerSystemType.ATARI800, false);
		panel.setTextFont(font);
		panel.setImmediateTypeProvider(line -> line.getLine().contains("#$") ? new DisassemblyPanel.ImmediateType(MemoryType.UNKNOWN, true) : null);
		panel.refresh(workspace.getDisassemblyResult());

		JFrame frame = new JFrame("PopupStructureTest");
		frame.add(panel);
		frame.setSize(600, 400);
		frame.setVisible(true);
		try {
			List<DisassemblyLine> lines = codeLines(workspace.getDisassemblyResult());
			int jsrIndex = indexOf(lines, "jsr L");
			int firstCodeLineIndex = panelIndexOf(workspace.getDisassemblyResult(), lines.get(0));
			int y = (firstCodeLineIndex + jsrIndex) * font.getGlyphHeight() + font.getGlyphHeight() / 2;
			rightClick(panel.getGrid(), 20, y);

			JPopupMenu popup = panel.getPopupMenu();
			Assert.boolEquals(popup.isVisible(), true);
			List<String> texts = new ArrayList<>();
			for (Component component : popup.getComponents()) {
				if (component instanceof JMenuItem) {
					texts.add(((JMenuItem) component).getText().trim() + (component.isEnabled() ? "" : " (disabled)")); // Some labels end in padding after an accelerator hint.
				}
			}
			String menu = String.join(" | ", texts);
			Assert.boolEquals(menu.contains("Navigate to Definition of Label L"), true);
			Assert.boolEquals(menu.contains("Navigate Back to Previous Position (disabled)"), true);
			Assert.boolEquals(menu.contains("Change type of immediate byte to (disabled)"), true);
			Assert.boolEquals(menu.contains("Find References for Label L"), true);
			Assert.boolEquals(panel.editCommentMenuItem.isEnabled(), true);
			popup.setVisible(false);

			// An immediate-mode line: the submenu is enabled and its check mark is on the current type.
			int ldaIndex = indexOf(lines, "lda #$");
			y = (firstCodeLineIndex + ldaIndex) * font.getGlyphHeight() + font.getGlyphHeight() / 2;
			rightClick(panel.getGrid(), 20, y);
			JMenu submenu = null;
			for (Component component : popup.getComponents()) {
				if (component instanceof JMenu) {
					submenu = (JMenu) component;
				}
			}
			Assert.notNull(submenu);
			Assert.boolEquals(submenu.isEnabled(), true);
			List<String> checked = new ArrayList<>();
			int enabledItems = 0;
			for (Component component : submenu.getMenuComponents()) {
				if (component instanceof JCheckBoxMenuItem) {
					JCheckBoxMenuItem item = (JCheckBoxMenuItem) component;
					if (item.isSelected()) {
						checked.add(item.getText());
					}
					if (item.isEnabled()) {
						enabledItems++;
					}
				}
			}
			Assert.stringEquals(String.join(",", checked), "Unknown");
			Assert.longEquals(enabledItems, 5); // Char Constant included: the provider said the operand is printable.
			popup.setVisible(false);
		} finally {
			frame.dispose();
		}
	}

	// ------------------------------------------------------------------

	private static List<DisassemblyLine> codeLines(DisassemblyResult result) {
		List<DisassemblyLine> lines = new ArrayList<>();
		for (Iterator<DisassemblyLine> i = result.createLineIterator(DisassemblySectionType.CODE_LINES); i.hasNext();) {
			lines.add(i.next());
		}
		return lines;
	}

	private static int indexOf(List<DisassemblyLine> lines, String startsWith) {
		for (int i = 0; i < lines.size(); i++) {
			if (lines.get(i).getLine().trim().startsWith(startsWith)) {
				return i;
			}
		}
		Assert.fail("No line starting with '" + startsWith + "'.");
		return -1;
	}

	/** The index of {@code line} among all lines the panel shows - the sections before the code lines count, too. */
	private static int panelIndexOf(DisassemblyResult result, DisassemblyLine line) {
		int index = 0;
		for (Iterator<DisassemblyLine> i = result.createLineIterator(); i.hasNext(); index++) {
			if (i.next() == line) {
				return index;
			}
		}
		Assert.fail("Line not shown.");
		return -1;
	}

	/** Windows shows popups on release: press, then release with the popup trigger flag, through the real event queue. */
	private static void rightClick(Component target, int x, int y) {
		target.dispatchEvent(new MouseEvent(target, MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(), InputEvent.BUTTON3_DOWN_MASK, x, y, 1,
				false, MouseEvent.BUTTON3));
		target.dispatchEvent(new MouseEvent(target, MouseEvent.MOUSE_RELEASED, System.currentTimeMillis(), 0, x, y, 1, true, MouseEvent.BUTTON3));
	}
}
