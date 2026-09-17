/*
 * Copyright (C) 2025 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.ui;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import com.wudsn.tools.dis6502.model.DisassemblyLine;
import com.wudsn.tools.dis6502.model.DisassemblyResult;

/**
 * A read-only view of the current {@link DisassemblyResult}'s lines, plus a
 * text search that drives the cross-reference list (see {@link
 * #findField}/{@link #findButton}/{@link #findNextButton}, wired up by
 * {@code Dis6502} to {@link DisassemblyResult#findAndSelectLines} and
 * {@link XRefPanel}) - {@code findButton} runs a fresh search (matching
 * {@code MainDisassembly::Find}), {@code findNextButton} continues it
 * (matching {@code MainDisassembly::FindNextString(false)}, bound in C++
 * to ID_DIS_FIND_NEXT).
 * <p>
 * Ported from ui/DisassemblyWindow.h / DisassemblyWindow.cpp and
 * ui/DisassemblyControl(Impl).h/.cpp. The listing itself is {@link
 * DisassemblyGridPanel}, a custom-painted, read-only list using the real
 * per-computer-system bitmap glyphs from {@link ComputerFont} - see that
 * class's javadoc for why (the C++ source draws this listing with the same
 * font as the memory inspector, not a plain system font) and {@link
 * DisassemblyGridPanel}'s own javadoc for exactly which parts of {@code
 * DisassemblyControlImpl.cpp} this replicates (plain text layout,
 * virtualized via Swing's clip-rect repaint) and which it does not (mouse
 * selection, inline editing, the popup menu - none of these existed in
 * this port before this rewrite either). {@link #navigateToLine} (scrolling
 * to and highlighting a line) is the one piece of the C++ control's
 * selection behavior this port implements, since {@link XRefPanel} needs
 * it. {@link #setComputerFont} must be called by {@code Dis6502} whenever
 * the workspace's computer system or double-font-height setting changes,
 * matching {@code DisassemblyWindow}'s use of {@code
 * WorkspaceFont::GetResizedFont}.
 *
 * @author Peter Dell
 */
public final class DisassemblyPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	private final DisassemblyGridPanel grid = new DisassemblyGridPanel();
	public final JTextField findField = new JTextField(24);
	public final JButton findButton = new JButton("Find");
	public final JButton findNextButton = new JButton("Find Next");

	private final Map<Integer, Integer> lineNumberToIndex = new HashMap<>();

	public DisassemblyPanel() {
		super(new BorderLayout());

		JPanel findButtonsPanel = new JPanel();
		findButtonsPanel.add(findButton);
		findButtonsPanel.add(findNextButton);

		JPanel findPanel = new JPanel(new BorderLayout(4, 4));
		findPanel.add(findField, BorderLayout.CENTER);
		findPanel.add(findButtonsPanel, BorderLayout.EAST);

		add(findPanel, BorderLayout.NORTH);
		add(new JScrollPane(grid), BorderLayout.CENTER);
	}

	/** Ported from DisassemblyWindow's use of WorkspaceFont::GetResizedFont - call whenever the workspace's computer system or double-height setting changes. */
	public void setComputerFont(ComputerFont computerFont) {
		grid.setComputerFont(computerFont);
	}

	public void refresh(DisassemblyResult disassemblyResult) {
		lineNumberToIndex.clear();

		if (disassemblyResult == null || disassemblyResult.getLineCount() == 0) {
			grid.setLines(Collections.emptyList());
			return;
		}

		List<String> lines = new ArrayList<>();
		DisassemblyResult.LineIterator iterator = disassemblyResult.createLineIterator();
		while (iterator.hasNext()) {
			DisassemblyLine line = iterator.next();
			lineNumberToIndex.put(line.getLineNumber(), lines.size());
			lines.add(line.getLine());
		}
		grid.setLines(lines);
	}

	/**
	 * Scrolls to and highlights the given disassembly line number, matching
	 * the effect of {@code DisassemblyControl::SelectLine}. Returns {@code
	 * false} if {@code lineNumber} is not part of the currently displayed
	 * disassembly (e.g. stale after a new one replaced it).
	 */
	public boolean navigateToLine(int lineNumber) {
		Integer index = lineNumberToIndex.get(lineNumber);
		if (index == null) {
			return false;
		}
		grid.highlightLine(index);
		return true;
	}
}
