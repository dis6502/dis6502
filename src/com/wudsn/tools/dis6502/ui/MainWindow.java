/*
 * Copyright (C) 2025 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.ui;

import javax.swing.JFrame;
import javax.swing.JSplitPane;
import javax.swing.WindowConstants;

import com.wudsn.tools.base.gui.ElementFactory;

/**
 * The application's main window: a menu bar plus the five panels
 * {@code MainWindow.h}'s C++ counterpart composes ({@link
 * SegmentListPanel}, {@link MemoryInspectorPanel}, {@link
 * DisassemblyPanel}, {@link XRefPanel}, {@link LogPanel}).
 * <p>
 * Ported from ui/MainWindow.h / MainWindow.cpp and ui/Layout.h / Layout.cpp,
 * simplified for this first pass: the C++ version computes exact pixel
 * layout from the current font metrics ({@code Layout::Compute}); this
 * just nests {@link JSplitPane}s with reasonable proportions (not
 * font-metric-derived pixel sizes) and lets Swing handle resizing. The
 * arrangement itself matches {@code Layout::Compute}'s: {@link
 * #segmentListPanel} stacked above {@link #memoryInspectorPanel} in a
 * narrow left column (a small segment list over a much taller memory
 * inspector, matching {@code segmentHeight}/{@code memoryInspectorHeight}),
 * {@link #disassemblyPanel} stacked above {@link #xrefPanel} in the
 * remaining width (a tall disassembly view over a short cross-reference
 * list, matching {@code disHeight}/{@code xrefHeight}), and {@link
 * #logPanel} spanning the full width at the bottom - not a {@link
 * javax.swing.JTabbedPane} pairing the memory inspector with the
 * cross-reference list, which the C++ layout never does (they occupy
 * different corners, both always visible at once).
 *
 * @author Peter Dell
 */
public final class MainWindow {

	private final JFrame frame = new JFrame("dis6502");

	public final MainMenu mainMenu = new MainMenu();
	public final SegmentListPanel segmentListPanel = new SegmentListPanel();
	public final MemoryInspectorPanel memoryInspectorPanel = new MemoryInspectorPanel();
	public final DisassemblyPanel disassemblyPanel = new DisassemblyPanel();
	public final XRefPanel xrefPanel = new XRefPanel();
	public final LogPanel logPanel = new LogPanel();

	public MainWindow() {
		frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		// Matches IDI_DIS6502 (dis6502.ico, converted to PNG - see MainMenu#applyIcons's
		// javadoc for why) being the app's own icon resource in the .rc; the window/taskbar
		// icon has no dedicated C++ call site to port from - Win32 picks IDI_DIS6502 up
		// automatically as the main window class's registered icon.
		frame.setIconImage(ElementFactory.createImageIcon("images/dis6502.png").getImage());
		frame.setJMenuBar(mainMenu.menuBar);
		frame.setContentPane(createContentPane());
		frame.setSize(1100, 750);
		frame.setLocationRelativeTo(null);
	}

	private JSplitPane createContentPane() {
		JSplitPane leftSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, segmentListPanel, memoryInspectorPanel);
		leftSplit.setResizeWeight(0.2);
		leftSplit.setDividerLocation(160);

		JSplitPane rightSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, disassemblyPanel, xrefPanel);
		rightSplit.setResizeWeight(0.8);
		rightSplit.setDividerLocation(500);

		JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftSplit, rightSplit);
		mainSplit.setResizeWeight(0.25);
		mainSplit.setDividerLocation(320);

		JSplitPane centerSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, mainSplit, logPanel);
		centerSplit.setResizeWeight(0.85);
		centerSplit.setDividerLocation(600);
		return centerSplit;
	}

	public JFrame getFrame() {
		return frame;
	}

	public void setVisible(boolean visible) {
		frame.setVisible(visible);
	}
}
