/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.RenderingHints;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import com.wudsn.tools.dis6502.Text;

/**
 * A scrolling, read-only log of application messages.
 * <p>
 * Ported from ui/LogListWindow.h / LogListWindow.cpp, simplified from a
 * multi-column, color-coded list view to a plain text area for this first
 * pass - the C++ version's per-entry severity coloring/columns are not
 * ported yet. {@link #header}, the lavender {@code RGB(192,192,255)} title
 * bar above it, is ported separately from {@code Main::PaintMainWindow}'s
 * own title-bar painting - see {@link PartHeaderPanel}'s javadoc.
 * <p>
 * {@link #setComputerFont} is ported from {@code PartWindow::ApplyLayout}'s
 * blanket {@code SetFont(...)} call - {@code LogListWindow} is a plain
 * native {@code ListBox} in C++, getting this automatically via {@code
 * WM_SETFONT}, matching {@link SegmentListPanel}'s own {@code
 * setComputerFont}: log messages are already-formatted text, not raw byte
 * values, so {@link ComputerFont#getAwtFont} needs no byte-index shift.
 * Falls back to a plain monospace font until the first call, the same
 * placeholder every {@code ComputerFont}-driven panel used before
 * {@code Dis6502} wired up real fonts. The {@code
 * RenderingHints.KEY_TEXT_ANTIALIASING} client property forces the same
 * antialiasing-off rendering {@link ComputerFont#drawText} uses explicitly
 * elsewhere - without it, real on-screen Windows ClearType antialiasing
 * blurs this pixel-art font illegible (see {@link SegmentListPanel}'s class
 * comment); unlike {@code JTable}/{@code JList}, {@code JTextArea} paints
 * its own text directly rather than delegating to a per-cell renderer
 * component, so this client property - which Swing's text painting reads
 * from the component itself - is enough on its own.
 *
 * @author Peter Dell
 */
public final class LogPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	private final PartHeaderPanel header = new PartHeaderPanel(new Color(192, 192, 255));
	private final JTextArea textArea = new JTextArea();

	public LogPanel() {
		super(new BorderLayout());
		header.setText(Text.IDS_LOG_TITLE);
		add(header, BorderLayout.NORTH);
		textArea.setEditable(false);
		textArea.setFont(new java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.PLAIN, 12));
		textArea.putClientProperty(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
		JScrollPane scrollPane = new JScrollPane(textArea);
		// Splitters already separate the part windows - the scroll pane's own
		// L&F-default border would just draw a redundant line right next to them.
		scrollPane.setBorder(BorderFactory.createEmptyBorder());
		add(scrollPane, BorderLayout.CENTER);
	}

	/** Ported from PartWindow::ApplyLayout's SetFont(partLayout->GetLayout()->GetFont()) - call whenever the workspace's computer system or double-height setting changes. */
	public void setComputerFont(ComputerFont computerFont) {
		textArea.setFont(computerFont.getAwtFont());
		header.setComputerFont(computerFont);
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
