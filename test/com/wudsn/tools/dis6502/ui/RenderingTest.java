/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import com.wudsn.tools.dis6502.Application;
import com.wudsn.tools.dis6502.model.Assert;
import com.wudsn.tools.dis6502.model.ComputerSystemFactory;
import com.wudsn.tools.dis6502.model.ComputerSystemType;
import com.wudsn.tools.dis6502.model.Disassembly;
import com.wudsn.tools.dis6502.model.DisassemblyLine;
import com.wudsn.tools.dis6502.model.DisassemblyProgressMonitor;
import com.wudsn.tools.dis6502.model.DisassemblyResult;
import com.wudsn.tools.dis6502.model.DisassemblySectionType;
import com.wudsn.tools.dis6502.model.FileHeader;
import com.wudsn.tools.dis6502.model.MemoryType;
import com.wudsn.tools.dis6502.model.MutableByteRangeSelection;
import com.wudsn.tools.dis6502.model.Segment;
import com.wudsn.tools.dis6502.model.Workspace;

/**
 * New (not ported), the fourth test of {@code plans/UI_SMOKE_TESTS_PROPOSAL.md}:
 * what the two custom-painted grids and the disassembly popup produce, as
 * far as that can be asserted without golden images (which break with
 * every look-and-feel, font and DPI change):
 * <ul>
 * <li>the popup menu a right-click on a {@code jsr} line builds - its item
 * set, enabled states and the immediate-type submenu's check mark - read
 * from the real {@link JPopupMenu} after a synthesized popup-trigger
 * {@link MouseEvent} went through the grid's own mouse handling;</li>
 * <li>{@link DisassemblyGridPanel} and {@link HexGridPanel} painted into an
 * off-screen image: the highlighted line's row contains the highlight
 * color, an unreferenced equate's row the grey, and the rows around them
 * do not.</li>
 * </ul>
 * Needs a display (see {@link DialogTextsTest#isHeadless()}) - showing a
 * popup needs a showing component, and the {@code ui} package does not
 * load headless anyway.
 *
 * @author Peter Dell
 */
public final class RenderingTest {

	private static final Color HIGHLIGHT = Color.YELLOW;
	private static final Color UNREFERENCED = new Color(192, 192, 192);

	private RenderingTest() {
	}

	public static void testRendering() throws Exception {
		if (DialogTextsTest.isHeadless()) {
			Assert.log("RenderingTest skipped: no display");
			return;
		}
		Workspace workspace = disassembleSample();
		SwingUtilities.invokeAndWait(() -> {
			try {
				testPopupStructure(workspace);
				testDisassemblyGridPaint(workspace);
				testHexGridPaint(workspace);
			} catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		});
		Assert.log("RenderingTest completed");
	}

	/** The popup for a {@code jsr} line: Navigate to Definition offered, Back disabled (no history), the immediate submenu disabled. */
	private static void testPopupStructure(Workspace workspace) throws Exception {
		DisassemblyPanel panel = new DisassemblyPanel();
		ComputerFont font = ComputerFont.get(ComputerSystemType.ATARI800, false);
		panel.setComputerFont(font);
		panel.setImmediateTypeProvider(line -> line.getLine().contains("#$") ? new DisassemblyPanel.ImmediateType(MemoryType.UNKNOWN, true) : null);
		panel.refresh(workspace.getDisassemblyResult());

		JFrame frame = new JFrame("RenderingTest");
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

	/** The highlighted line's row is painted yellow, an unreferenced equate's row grey, the rows in between neither. */
	private static void testDisassemblyGridPaint(Workspace workspace) {
		DisassemblyGridPanel grid = new DisassemblyGridPanel();
		ComputerFont font = ComputerFont.get(ComputerSystemType.ATARI800, false);
		grid.setComputerFont(font);
		List<DisassemblyLine> lines = new ArrayList<>();
		for (Iterator<DisassemblyLine> i = workspace.getDisassemblyResult().createLineIterator(); i.hasNext();) {
			lines.add(i.next());
		}
		grid.setLines(lines);
		int unreferencedIndex = -1;
		for (int i = 0; i < lines.size(); i++) {
			DisassemblyLine line = lines.get(i);
			if (line.getSection().getType() == DisassemblySectionType.SYSTEM_EQUATES && !line.referenced && line.systemAddress > 0
					&& unreferencedIndex < 0) {
				unreferencedIndex = i;
			}
		}
		int highlightedIndex = lines.size() - 1;
		grid.highlightLine(highlightedIndex);

		BufferedImage image = paint(grid);
		int cellH = font.getGlyphHeight();
		Assert.boolEquals(rowContains(image, highlightedIndex * cellH, cellH, HIGHLIGHT), true);
		Assert.boolEquals(rowContains(image, (highlightedIndex - 1) * cellH, cellH, HIGHLIGHT), false);
		if (unreferencedIndex >= 0) {
			Assert.boolEquals(rowContains(image, unreferencedIndex * cellH, cellH, UNREFERENCED), true);
		}
		Assert.boolEquals(rowContains(image, highlightedIndex * cellH, cellH, UNREFERENCED), false);
	}

	/** The selected bytes' cells are painted yellow, the line after the selection is not. */
	private static void testHexGridPaint(Workspace workspace) {
		HexGridPanel grid = new HexGridPanel();
		ComputerFont font = ComputerFont.get(ComputerSystemType.ATARI800, false);
		grid.setComputerFont(font);
		Segment segment = workspace.getSegmentList().getSegment(0);
		grid.setByteSource(new SegmentByteSource(segment));
		MutableByteRangeSelection selection = new MutableByteRangeSelection();
		selection.setSelection(2, 5, segment.getSize()); // Four bytes in the first line.
		grid.setSelection(selection);
		grid.setSize(grid.getPreferredSize());

		BufferedImage image = paint(grid);
		int cellH = font.getGlyphHeight();
		Assert.boolEquals(rowContains(image, 0, cellH, HIGHLIGHT), true);
		Assert.boolEquals(rowContains(image, cellH, cellH, HIGHLIGHT), false);

		selection.setSelection(0, 0, segment.getSize());
		selection.clearSelection();
		image = paint(grid);
		Assert.boolEquals(rowContains(image, 0, cellH, HIGHLIGHT), false);
	}

	// ------------------------------------------------------------------

	/** A small Atari 800 program: an immediate load, a subroutine call and its target, with the real system equates. */
	private static Workspace disassembleSample() {
		Workspace workspace = new Workspace(new ComputerSystemFactory());
		workspace.setComputerSystemType(ComputerSystemType.ATARI800);
		new com.wudsn.tools.dis6502.model.WorkspaceLogic(new Application()).loadSystemEquates(workspace);
		Segment segment = workspace.getSegmentList().insertSegmentAt(0);
		segment.setHeader(FileHeader.ATARI_BINARY);
		segment.bBinary = true;
		segment.wBegin = 0x2000;
		segment.wEnd = 0x200F;
		segment.createMemoryBlockFromBeginToEnd();
		int[] program = { 0xA9, 0x0C, // 2000 LDA #$0C
				0x20, 0x08, 0x20, // 2002 JSR L2008
				0x8D, 0x1A, 0xD0, // 2005 STA COLBK
				0x60, // 2008 RTS
				0xEA, 0xEA, 0xEA, 0xEA, 0xEA, 0xEA, 0xEA }; // NOPs
		for (int i = 0; i < program.length; i++) {
			segment.setData(i, program[i]);
		}
		segment.setType(0, MemoryType.CODE, program.length);

		Disassembly disassembly = new Disassembly();
		disassembly.setWorkspace(workspace);
		DisassemblyProgressMonitor monitor = new DisassemblyProgressMonitor(new Application());
		disassembly.setProgressMonitor(monitor);
		monitor.startDisassembly(disassembly);
		return workspace;
	}

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

	private static BufferedImage paint(JComponent component) {
		Dimension size = component.getPreferredSize();
		component.setSize(size);
		component.doLayout();
		BufferedImage image = new BufferedImage(Math.max(size.width, 1), Math.max(size.height, 1), BufferedImage.TYPE_INT_RGB);
		Graphics2D g = image.createGraphics();
		try {
			g.setColor(Color.WHITE);
			g.fillRect(0, 0, image.getWidth(), image.getHeight());
			component.paint(g);
		} finally {
			g.dispose();
		}
		return image;
	}

	private static boolean rowContains(BufferedImage image, int y, int height, Color color) {
		int rgb = color.getRGB() & 0xFFFFFF;
		for (int yy = Math.max(y, 0); yy < Math.min(y + height, image.getHeight()); yy++) {
			for (int x = 0; x < image.getWidth(); x++) {
				if ((image.getRGB(x, yy) & 0xFFFFFF) == rgb) {
					return true;
				}
			}
		}
		return false;
	}
}
