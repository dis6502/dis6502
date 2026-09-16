/*
 * Copyright (C) 2025 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.ui;

import java.awt.BorderLayout;
import java.awt.Font;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import com.wudsn.tools.dis6502.model.DisassemblyLine;
import com.wudsn.tools.dis6502.model.DisassemblyResult;

/**
 * A read-only view of the current {@link DisassemblyResult}'s lines.
 * <p>
 * Ported from ui/DisassemblyWindow.h / DisassemblyWindow.cpp and
 * ui/DisassemblyControl(Impl).h/.cpp, drastically simplified for this first
 * pass to a plain, non-editable text area showing every line in order -
 * the C++ version's virtualized/scrollable custom-painted list (only
 * visible lines are ever rendered), inline editing, selection, and popup
 * menu (ui/DisassemblyPopupMenu.h/.cpp) are not ported yet.
 *
 * @author Peter Dell
 */
public final class DisassemblyPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	private final JTextArea textArea = new JTextArea();

	public DisassemblyPanel() {
		super(new BorderLayout());
		textArea.setEditable(false);
		textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
		add(new JScrollPane(textArea), BorderLayout.CENTER);
		showPlaceholder();
	}

	private void showPlaceholder() {
		textArea.setText("No disassembly yet. Open a workspace or add a file to disassemble it.");
	}

	public void refresh(DisassemblyResult disassemblyResult) {
		if (disassemblyResult == null || disassemblyResult.getLineCount() == 0) {
			showPlaceholder();
			return;
		}

		StringBuilder text = new StringBuilder();
		DisassemblyResult.LineIterator iterator = disassemblyResult.createLineIterator();
		while (iterator.hasNext()) {
			DisassemblyLine line = iterator.next();
			text.append(line.getLine()).append('\n');
		}
		textArea.setText(text.toString());
		textArea.setCaretPosition(0);
	}
}
