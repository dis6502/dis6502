/*
 * Copyright (C) 2025 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.ui;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

/**
 * A scrolling, read-only log of application messages.
 * <p>
 * Ported from ui/LogListWindow.h / LogListWindow.cpp, simplified from a
 * multi-column, color-coded list view to a plain text area for this first
 * pass - the C++ version's per-entry severity coloring/columns are not
 * ported yet.
 * <p>
 * {@link #setComputerFont} is ported from {@code PartWindow::ApplyLayout}'s
 * blanket {@code SetFont(...)} call - {@code LogListWindow} is a plain
 * native {@code ListBox} in C++, getting this automatically via {@code
 * WM_SETFONT}, matching {@link SegmentListPanel}'s own {@code
 * setComputerFont}: log messages are already-formatted text, not raw byte
 * values, so {@link ComputerFont#getAwtFont} needs no byte-index shift.
 * Falls back to a plain monospace font until the first call, the same
 * placeholder every {@code ComputerFont}-driven panel used before
 * {@code Dis6502} wired up real fonts.
 *
 * @author Peter Dell
 */
public final class LogPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	private final JTextArea textArea = new JTextArea();

	public LogPanel() {
		super(new BorderLayout());
		textArea.setEditable(false);
		textArea.setFont(new java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.PLAIN, 12));
		add(new JScrollPane(textArea), BorderLayout.CENTER);
	}

	/** Ported from PartWindow::ApplyLayout's SetFont(partLayout->GetLayout()->GetFont()) - call whenever the workspace's computer system or double-height setting changes. */
	public void setComputerFont(ComputerFont computerFont) {
		textArea.setFont(computerFont.getAwtFont());
	}

	/** May be called from any thread. */
	public void appendLine(String text) {
		Runnable task = () -> {
			textArea.append(text);
			textArea.append("\n");
			textArea.setCaretPosition(textArea.getDocument().getLength());
		};
		if (SwingUtilities.isEventDispatchThread()) {
			task.run();
		} else {
			SwingUtilities.invokeLater(task);
		}
	}
}
