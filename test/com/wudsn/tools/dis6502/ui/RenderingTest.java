/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import com.wudsn.tools.dis6502.Application;
import com.wudsn.tools.dis6502.Options;
import com.wudsn.tools.dis6502.model.Assert;
import com.wudsn.tools.dis6502.model.Disassembly;
import com.wudsn.tools.dis6502.model.DisassemblyLine;
import com.wudsn.tools.dis6502.model.DisassemblyProgressMonitor;
import com.wudsn.tools.dis6502.model.DisassemblySectionType;
import com.wudsn.tools.dis6502.model.FileHeader;
import com.wudsn.tools.dis6502.model.MemoryType;
import com.wudsn.tools.dis6502.model.MutableByteRangeSelection;
import com.wudsn.tools.dis6502.model.Segment;
import com.wudsn.tools.dis6502.model.Workspace;
import com.wudsn.tools.dis6502.model.system.ComputerSystemFactory;
import com.wudsn.tools.dis6502.model.system.ComputerSystemType;

/**
 * {@link DisassemblyGridPanel} and {@link HexGridPanel} painted into an
 * off-screen image, as far as that can be asserted without golden images
 * (which break with every look-and-feel, font and DPI change): the
 * highlighted line's row contains the highlight color, an unreferenced
 * equate's row the grey, and the rows around them do not; the selected
 * bytes' cells are painted yellow, the line after the selection is not.
 * <p>
 * Always runs, headless or not: both grids are plain {@link JComponent}s,
 * painted directly into a {@link BufferedImage} without ever being added
 * to a real {@link java.awt.Window} - unlike {@link PopupStructureTest},
 * which needs a real, showing popup.
 *
 * @author Peter Dell
 */
public final class RenderingTest {

	private static final Color HIGHLIGHT = Color.YELLOW;
	private static final Color UNREFERENCED = new Color(192, 192, 192);

	private RenderingTest() {
	}

	public static void testRendering() throws Exception {
		Workspace workspace = disassembleSample();
		SwingUtilities.invokeAndWait(() -> {
			try {
				testDisassemblyGridPaint(workspace);
				testHexGridPaint(workspace);
			} catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		});
		Assert.log("RenderingTest completed");
	}

	/** The highlighted line's row is painted yellow, an unreferenced equate's row grey, the rows in between neither. */
	private static void testDisassemblyGridPaint(Workspace workspace) {
		DisassemblyGridPanel grid = new DisassemblyGridPanel();
		ComputerFont font = ComputerFont.get(ComputerSystemType.ATARI800, false, Options.NATIVE_FONT_ZOOM_DEFAULT);
		grid.setTextFont(font);
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
		ComputerFont font = ComputerFont.get(ComputerSystemType.ATARI800, false, Options.NATIVE_FONT_ZOOM_DEFAULT);
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
	static Workspace disassembleSample() {
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
