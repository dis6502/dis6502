/*
 * Copyright (C) 2025 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.ui;

import javax.swing.JFrame;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.WindowConstants;

/**
 * The application's main window: a menu bar plus the five panels
 * {@code MainWindow.h}'s C++ counterpart composes ({@link
 * SegmentListPanel}, {@link MemoryInspectorPanel}, {@link
 * DisassemblyPanel}, {@link XRefPanel}, {@link LogPanel}).
 * <p>
 * Ported from ui/MainWindow.h / MainWindow.cpp and ui/Layout.h / Layout.cpp,
 * simplified for this first pass: the C++ version computes exact pixel
 * layout from the current font metrics ({@code Layout::Compute}); this
 * just nests {@link JSplitPane}s with reasonable proportions and lets
 * Swing handle resizing.
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
		frame.setJMenuBar(mainMenu.menuBar);
		frame.setContentPane(createContentPane());
		frame.setSize(1100, 750);
		frame.setLocationRelativeTo(null);
	}

	private JSplitPane createContentPane() {
		JTabbedPane rightTabs = new JTabbedPane();
		rightTabs.addTab("Memory Inspector", memoryInspectorPanel);
		rightTabs.addTab("Cross Reference", xrefPanel);

		JSplitPane topSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, disassemblyPanel, rightTabs);
		topSplit.setResizeWeight(0.7);

		JSplitPane centerSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, topSplit, logPanel);
		centerSplit.setResizeWeight(0.85);

		JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, segmentListPanel, centerSplit);
		mainSplit.setResizeWeight(0.2);
		return mainSplit;
	}

	public JFrame getFrame() {
		return frame;
	}

	public void setVisible(boolean visible) {
		frame.setVisible(visible);
	}
}
