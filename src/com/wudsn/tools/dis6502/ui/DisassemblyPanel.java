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
import com.wudsn.tools.dis6502.model.Equate;
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
 * The right-click popup menu ({@link #maybeShowPopup}) is a still-reduced
 * port of ui/DisassemblyPopupMenu.h/.cpp's {@code DISASSEMBLY_POPUP_MENU}:
 * Add/Edit Comment, Find/Find Next, and - since {@link #findLabelInLine}
 * turned out to need nothing more than the clicked line's own text (see
 * that method's javadoc for why this is not the "mouse-position label
 * parsing" it first looked like) - Find Definition/References, Rename,
 * and Address Range too, reusing already-ported pieces ({@link
 * DisassemblyResult#findAndSelectLines}/{@link
 * DisassemblyResult#findDefinitionLineNumber}, {@code Equate.extractAddress}/
 * {@code isAutomaticLabel}, {@link EquateDialog}/{@link
 * EquateRangeDialog}) - see {@code Dis6502}'s wiring of {@link
 * #findDefMenuItem}/{@link #findRef1MenuItem}/{@link #findRef2MenuItem}/
 * {@link #renameDefMenuItem}/{@link #renameRefMenuItem}/{@link
 * #addrRangeDefMenuItem}/{@link #addrRangeRefMenuItem}. Still not ported:
 * Back in History (needs a navigation history stack this port does not
 * have) and the per-instruction Set Type submenu (a different, per-
 * clicked-instruction mechanism from the memory inspector popup's
 * selection-based one, needing "is this an immediate-mode instruction"
 * detection this port does not have either).
 * <p>
 * Every popup item is public and wired by {@code Dis6502} (unlike {@link
 * MemoryInspectorPanel}'s popup items, they need a parent {@link
 * java.awt.Frame}/{@code Workspace} this panel does not have), reading
 * {@link #getRightClickedLine}/{@link #getRightClickedLabelDefinition}/
 * {@link #getRightClickedLabelReference} - all three frozen at the moment
 * the popup was shown, matching {@code MainDisassembly::DrawMenu} capturing
 * {@code disSelection}/{@code labelDefinition}/{@code labelReference} once
 * per right-click rather than re-deriving them when a menu item is later
 * clicked. {@link #editCommentMenuItem}'s Add/Edit Comment, unlike {@code
 * MainDisassembly::AddComment} (which always passes the sentinel size
 * {@code 0xFFFF} for {@link CommentDialog} to snap to the enclosing
 * instruction via {@code DisassemblyResult::findOffsetAtStartOfInstruction}),
 * uses the clicked line's own {@code offset}/{@code size} directly -
 * correct for the common case of right-clicking an actual instruction
 * line, though not necessarily identical for a label/equate-only line
 * with no byte size of its own; that model-layer method is ported and
 * available, just not wired up for this yet.
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
	public final JMenuItem findDefMenuItem = new JMenuItem();
	public final JMenuItem findRef1MenuItem = new JMenuItem();
	public final JMenuItem findRef2MenuItem = new JMenuItem();
	public final JMenuItem renameDefMenuItem = new JMenuItem();
	public final JMenuItem renameRefMenuItem = new JMenuItem();
	public final JMenuItem addrRangeDefMenuItem = new JMenuItem();
	public final JMenuItem addrRangeRefMenuItem = new JMenuItem();

	private final JPopupMenu popupMenu = new JPopupMenu();
	private final JMenuItem popupFindMenuItem = new JMenuItem("Find...");
	private final JMenuItem popupFindNextMenuItem = new JMenuItem("Find next");

	private final Map<Integer, Integer> lineNumberToIndex = new HashMap<>();
	private List<DisassemblyLine> disassemblyLines = Collections.emptyList();
	private DisassemblyLine rightClickedLine;
	private String rightClickedLabelDefinition = "";
	private String rightClickedLabelReference = "";

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

		popupFindMenuItem.addActionListener(e -> findButton.doClick());
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

	/**
	 * Ported from DisassemblyControlImpl::RButtonDown/MainDisassembly::DrawMenu.
	 * Rebuilds the popup's item set from scratch every time, the idiomatic
	 * Swing way to do what the C++ version's {@code DisassemblyPopupMenu::
	 * Update} does by editing a fixed .rc-defined template in place ({@code
	 * SetText}/{@code DeleteEntry}/{@code DeleteSepartor}).
	 */
	private void maybeShowPopup(MouseEvent e) {
		if (!e.isPopupTrigger()) {
			return;
		}
		int index = grid.lineIndexAtY(e.getY());
		rightClickedLine = (index >= 0 && index < disassemblyLines.size()) ? disassemblyLines.get(index) : null;
		String[] labels = findLabelInLine(rightClickedLine == null ? "" : rightClickedLine.getLine());
		rightClickedLabelDefinition = labels[0];
		rightClickedLabelReference = labels[1];

		boolean hasDefinition = !rightClickedLabelDefinition.isEmpty();
		boolean hasReference = !rightClickedLabelReference.isEmpty();
		boolean definitionAutomatic = hasDefinition && Equate.isAutomaticLabel(rightClickedLabelDefinition);
		boolean referenceAutomatic = hasReference && Equate.isAutomaticLabel(rightClickedLabelReference);

		popupMenu.removeAll();
		if (hasReference) {
			findDefMenuItem.setText("Navigate to Definition of Label " + rightClickedLabelReference);
			popupMenu.add(findDefMenuItem);
			popupMenu.addSeparator();
		}

		editCommentMenuItem
				.setEnabled(rightClickedLine != null && rightClickedLine.segmentIndex != SegmentList.NO_SEGMENT_INDEX);
		popupMenu.add(editCommentMenuItem);
		popupMenu.addSeparator();

		popupFindMenuItem.setEnabled(findButton.isEnabled());
		popupMenu.add(popupFindMenuItem);
		popupFindNextMenuItem.setEnabled(findNextButton.isEnabled());
		popupMenu.add(popupFindNextMenuItem);
		if (hasDefinition) {
			findRef2MenuItem.setText("Find References for Label " + rightClickedLabelDefinition);
			popupMenu.add(findRef2MenuItem);
		}
		if (hasReference) {
			findRef1MenuItem.setText("Find References for Label " + rightClickedLabelReference);
			popupMenu.add(findRef1MenuItem);
		}

		if (definitionAutomatic || referenceAutomatic) {
			popupMenu.addSeparator();
			if (definitionAutomatic) {
				renameDefMenuItem.setText("Rename Defined Label " + rightClickedLabelDefinition);
				popupMenu.add(renameDefMenuItem);
			}
			if (referenceAutomatic && !rightClickedLabelReference.equals(rightClickedLabelDefinition)) {
				renameRefMenuItem.setText("Rename Referenced Label " + rightClickedLabelReference);
				popupMenu.add(renameRefMenuItem);
			}
			popupMenu.addSeparator();
			if (definitionAutomatic) {
				addrRangeDefMenuItem.setText("Define Address Range Relative to " + rightClickedLabelDefinition + " ...");
				popupMenu.add(addrRangeDefMenuItem);
			}
			if (referenceAutomatic) {
				addrRangeRefMenuItem.setText("Define Address Range Relative to " + rightClickedLabelReference + " ...");
				popupMenu.add(addrRangeRefMenuItem);
			}
		}

		popupMenu.show(grid, e.getX(), e.getY());
	}

	/**
	 * Splits {@code text} into the label it defines (a leading identifier at
	 * column 0) and the label its operand references, as a two-element array
	 * (either may be {@code ""}). Ported from {@code
	 * DisassemblyControlImpl::FindLabelInLine} - despite that method's name
	 * suggesting it needs to know where in the line the mouse was, it only
	 * ever parses the whole line's text structurally (leading identifier =
	 * definition; skip the mnemonic; an operand starting with a letter/@/_
	 * after an optional #/(/&gt;/&lt; prefix = reference), with no mouse
	 * position involved at all.
	 */
	private static String[] findLabelInLine(String text) {
		int[] index = { 0 };
		String labelDefinition = "";
		String labelReference = "";

		char c = charAt(text, index);
		if (isLabelStartChar(c)) {
			StringBuilder definition = new StringBuilder();
			while (isLabelChar(c)) {
				definition.append(c);
				c = charAt(text, index);
			}
			labelDefinition = definition.toString();
			while (c != '\0' && c != ' ') {
				c = charAt(text, index);
			}
			while (c == ' ') {
				c = charAt(text, index);
			}
		} else if (c != ' ') {
			return new String[] { "", "" };
		}

		// Skip the mnemonic.
		while (c == ' ') {
			c = charAt(text, index);
		}
		while (c != '\0' && c != ' ') {
			c = charAt(text, index);
		}
		while (c == ' ') {
			c = charAt(text, index);
		}

		if (c == '"') {
			return new String[] { labelDefinition, "" };
		}
		if (c == '#' || c == '(') {
			c = charAt(text, index);
			while (c == ' ') {
				c = charAt(text, index);
			}
			if (c == '>' || c == '<') {
				c = charAt(text, index);
				while (c == ' ') {
					c = charAt(text, index);
				}
			}
		}
		if (isLabelStartChar(c)) {
			StringBuilder reference = new StringBuilder();
			while (isLabelChar(c)) {
				reference.append(c);
				c = charAt(text, index);
			}
			if (reference.length() > 1) {
				labelReference = reference.toString();
			}
		}
		return new String[] { labelDefinition, labelReference };
	}

	private static char charAt(String text, int[] index) {
		char c = index[0] < text.length() ? text.charAt(index[0]) : '\0';
		index[0]++;
		return c;
	}

	private static boolean isLabelStartChar(char c) {
		return c == '@' || c == '_' || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
	}

	private static boolean isLabelChar(char c) {
		return isLabelStartChar(c) || (c >= '0' && c <= '9');
	}

	/** The disassembly line last right-clicked to show the popup menu, or {@code null} if it wasn't over a line tied to segment data. */
	public DisassemblyLine getRightClickedLine() {
		return rightClickedLine;
	}

	/** The label the right-clicked line defines (a leading identifier at column 0), or {@code ""} if it does not define one. */
	public String getRightClickedLabelDefinition() {
		return rightClickedLabelDefinition;
	}

	/** The label the right-clicked line's operand references, or {@code ""} if it does not reference one. */
	public String getRightClickedLabelReference() {
		return rightClickedLabelReference;
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
