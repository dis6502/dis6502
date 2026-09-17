/*
 * Copyright (C) 2025 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.ui;

import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import com.wudsn.tools.dis6502.model.DisassemblyLine;
import com.wudsn.tools.dis6502.model.DisassemblyResult;
import com.wudsn.tools.dis6502.model.SegmentList;

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
 * <p>
 * The right-click popup menu ({@link #maybeShowPopup}) is a deliberately
 * reduced port of ui/DisassemblyPopupMenu.h/.cpp's {@code
 * DISASSEMBLY_POPUP_MENU}: only Add/Edit Comment and Find/Find Next,
 * which work off data this port already has. The rest of that menu -
 * Find Definition/References, Rename, Address Range, and Back in History -
 * all depend on {@code DisassemblyControlImpl}'s mouse-position "label
 * under cursor" text parsing ({@code GetLabelReference}/{@code
 * GetLabelDefinition}) and a navigation history stack, neither of which
 * exist in this port; its per-instruction Set Type submenu (a different,
 * per-clicked-instruction mechanism from the memory inspector popup's
 * selection-based one) needs "is this an immediate-mode instruction"
 * detection this port does not have either. None of these are ported here.
 * {@link #editCommentMenuItem} is public and wired by {@code Dis6502}
 * (unlike {@link MemoryInspectorPanel}'s popup items, it needs a parent
 * {@link java.awt.Frame} this panel does not have), reading the clicked
 * line via {@link #getRightClickedLine()}; unlike {@code
 * MainDisassembly::AddComment} (which always passes the sentinel size
 * {@code 0xFFFF} for {@link CommentDialog} to snap to the enclosing
 * instruction via {@code DisassemblyResult::FindOffsetAtStartOfInstruction},
 * not ported), this uses the clicked line's own {@code offset}/{@code
 * size} directly - correct for the common case of right-clicking an
 * actual instruction line, though not necessarily identical for a
 * label/equate-only line with no byte size of its own.
 *
 * @author Peter Dell
 */
public final class DisassemblyPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	private final DisassemblyGridPanel grid = new DisassemblyGridPanel();
	public final JTextField findField = new JTextField(24);
	public final JButton findButton = new JButton("Find");
	public final JButton findNextButton = new JButton("Find Next");
	public final JMenuItem editCommentMenuItem = new JMenuItem("Add/Edit comment...");

	private final JPopupMenu popupMenu = new JPopupMenu();
	private final JMenuItem popupFindMenuItem = new JMenuItem("Find...");
	private final JMenuItem popupFindNextMenuItem = new JMenuItem("Find next");

	private final Map<Integer, Integer> lineNumberToIndex = new HashMap<>();
	private List<DisassemblyLine> disassemblyLines = Collections.emptyList();
	private DisassemblyLine rightClickedLine;

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

		popupMenu.add(editCommentMenuItem);
		popupMenu.addSeparator();
		popupMenu.add(popupFindMenuItem);
		popupFindMenuItem.addActionListener(e -> findButton.doClick());
		popupMenu.add(popupFindNextMenuItem);
		popupFindNextMenuItem.addActionListener(e -> findNextButton.doClick());

		grid.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				maybeShowPopup(e);
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				maybeShowPopup(e);
			}
		});
	}

	/** Ported from DisassemblyControlImpl::RButtonDown/MainDisassembly::DrawMenu, minus everything listed in this class's javadoc as not ported. */
	private void maybeShowPopup(MouseEvent e) {
		if (!e.isPopupTrigger()) {
			return;
		}
		int index = grid.lineIndexAtY(e.getY());
		rightClickedLine = (index >= 0 && index < disassemblyLines.size()) ? disassemblyLines.get(index) : null;

		editCommentMenuItem
				.setEnabled(rightClickedLine != null && rightClickedLine.segmentIndex != SegmentList.NO_SEGMENT_INDEX);
		popupFindMenuItem.setEnabled(findButton.isEnabled());
		popupFindNextMenuItem.setEnabled(findNextButton.isEnabled());
		popupMenu.show(grid, e.getX(), e.getY());
	}

	/** The disassembly line last right-clicked to show the popup menu, or {@code null} if it wasn't over a line tied to segment data. */
	public DisassemblyLine getRightClickedLine() {
		return rightClickedLine;
	}

	/** Ported from DisassemblyWindow's use of WorkspaceFont::GetResizedFont - call whenever the workspace's computer system or double-height setting changes. */
	public void setComputerFont(ComputerFont computerFont) {
		grid.setComputerFont(computerFont);
	}

	public void refresh(DisassemblyResult disassemblyResult) {
		lineNumberToIndex.clear();
		rightClickedLine = null;

		if (disassemblyResult == null || disassemblyResult.getLineCount() == 0) {
			disassemblyLines = Collections.emptyList();
			grid.setLines(Collections.emptyList());
			return;
		}

		List<String> lines = new ArrayList<>();
		List<DisassemblyLine> newDisassemblyLines = new ArrayList<>();
		DisassemblyResult.LineIterator iterator = disassemblyResult.createLineIterator();
		while (iterator.hasNext()) {
			DisassemblyLine line = iterator.next();
			lineNumberToIndex.put(line.getLineNumber(), lines.size());
			lines.add(line.getLine());
			newDisassemblyLines.add(line);
		}
		disassemblyLines = newDisassemblyLines;
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
