/*
 * Copyright (C) 2025 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Rectangle;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;

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
 * ui/DisassemblyControl(Impl).h/.cpp, drastically simplified for this first
 * pass to a plain, non-editable text area showing every line in order -
 * the C++ version's virtualized/scrollable custom-painted list (only
 * visible lines are ever rendered), inline editing, and popup menu
 * (ui/DisassemblyPopupMenu.h/.cpp) are not ported yet. {@link
 * #navigateToLine} (scrolling to and highlighting a line) is the one piece
 * of the C++ control's selection behavior this port does implement, since
 * {@link XRefPanel} needs it.
 *
 * @author Peter Dell
 */
public final class DisassemblyPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	private final JTextArea textArea = new JTextArea();
	public final JTextField findField = new JTextField(24);
	public final JButton findButton = new JButton("Find");
	public final JButton findNextButton = new JButton("Find Next");

	private final Map<Integer, Integer> lineNumberToOffset = new HashMap<>();
	private final Map<Integer, Integer> lineNumberToLength = new HashMap<>();
	private Object highlightTag;

	public DisassemblyPanel() {
		super(new BorderLayout());
		textArea.setEditable(false);
		textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

		JPanel findButtonsPanel = new JPanel();
		findButtonsPanel.add(findButton);
		findButtonsPanel.add(findNextButton);

		JPanel findPanel = new JPanel(new BorderLayout(4, 4));
		findPanel.add(findField, BorderLayout.CENTER);
		findPanel.add(findButtonsPanel, BorderLayout.EAST);

		add(findPanel, BorderLayout.NORTH);
		add(new JScrollPane(textArea), BorderLayout.CENTER);
		showPlaceholder();
	}

	private void showPlaceholder() {
		textArea.setText("No disassembly yet. Open a workspace or add a file to disassemble it.");
	}

	public void refresh(DisassemblyResult disassemblyResult) {
		highlightTag = null;
		lineNumberToOffset.clear();
		lineNumberToLength.clear();

		if (disassemblyResult == null || disassemblyResult.getLineCount() == 0) {
			showPlaceholder();
			return;
		}

		StringBuilder text = new StringBuilder();
		DisassemblyResult.LineIterator iterator = disassemblyResult.createLineIterator();
		while (iterator.hasNext()) {
			DisassemblyLine line = iterator.next();
			String lineText = line.getLine();
			lineNumberToOffset.put(line.getLineNumber(), text.length());
			lineNumberToLength.put(line.getLineNumber(), lineText.length());
			text.append(lineText).append('\n');
		}
		textArea.setText(text.toString());
		textArea.setCaretPosition(0);
	}

	/**
	 * Scrolls to and highlights the given disassembly line number, matching
	 * the effect of {@code DisassemblyControl::SelectLine}. Returns {@code
	 * false} if {@code lineNumber} is not part of the currently displayed
	 * disassembly (e.g. stale after a new one replaced it).
	 */
	public boolean navigateToLine(int lineNumber) {
		Integer offset = lineNumberToOffset.get(lineNumber);
		Integer length = lineNumberToLength.get(lineNumber);
		if (offset == null || length == null) {
			return false;
		}

		Highlighter highlighter = textArea.getHighlighter();
		if (highlightTag != null) {
			highlighter.removeHighlight(highlightTag);
			highlightTag = null;
		}
		try {
			highlightTag = highlighter.addHighlight(offset, offset + length,
					new DefaultHighlighter.DefaultHighlightPainter(Color.YELLOW));
			textArea.setCaretPosition(offset);
			Rectangle rectangle = textArea.modelToView(offset);
			if (rectangle != null) {
				textArea.scrollRectToVisible(rectangle);
			}
		} catch (BadLocationException ex) {
			return false;
		}
		return true;
	}
}
