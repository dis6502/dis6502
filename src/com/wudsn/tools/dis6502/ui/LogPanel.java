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
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import com.wudsn.tools.dis6502.Texts;

/**
 * A scrolling, read-only log of application messages, with error lines
 * shown in a distinct color for at-a-glance scanning.
 * <p>
 * Ported from ui/LogListWindow.h / LogListWindow.cpp, but not its
 * mechanism or exact appearance - see the class's own note in {@code
 * plans/MEMORY.md} on why: C++'s {@code LogListWindow} is a plain,
 * single-column, uncolored {@code ListBox} with no owner-draw painting at
 * all (despite its "list view" name suggesting otherwise), and {@code
 * LogListWindow::AddText} - the only method that would ever put text into
 * it - is never actually called anywhere in the C++ codebase, so this
 * window is effectively dead code there. There is no C++ behavior to
 * match here; this class is the Java port's own design, chosen to close a
 * genuine usability gap - color-coding error lines makes them easy to spot
 * among routine info messages, which the current text-only log doesn't
 * support at all. A {@link JTextPane} with per-insert {@link
 * SimpleAttributeSet} styling was used instead of a colored {@link
 * javax.swing.JList} (the pattern {@link XRefPanel}/{@link
 * SegmentListPanel} use) specifically to keep plain-text selection/copy
 * working - a list of styled cells doesn't support that the same way a
 * text component does, and being able to select and copy a log line
 * (e.g. into a bug report) is worth keeping.
 * <p>
 * {@link #header}, the lavender {@code RGB(192,192,255)} title bar above
 * it, is ported separately from {@code Main::PaintMainWindow}'s own
 * title-bar painting - see {@link PartHeaderPanel}'s javadoc.
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
 * comment); {@link JTextPane}, like {@code JTextArea}, paints its own text
 * directly rather than delegating to a per-cell renderer component, so this
 * client property - which Swing's text painting reads from the component
 * itself - is enough on its own.
 *
 * @author Peter Dell
 */
public final class LogPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	private static final Color ERROR_COLOR = new Color(192, 0, 0);

	private final PartHeaderPanel header = new PartHeaderPanel(new Color(192, 192, 255));
	private final JTextPane textPane = new JTextPane();
	private final SimpleAttributeSet infoStyle = new SimpleAttributeSet();
	private final SimpleAttributeSet errorStyle = new SimpleAttributeSet();

	public LogPanel() {
		super(new BorderLayout());
		header.setText(Texts.LogPanel_Title);
		add(header, BorderLayout.NORTH);
		StyleConstants.setForeground(errorStyle, ERROR_COLOR);
		textPane.setEditable(false);
		textPane.setFont(new java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.PLAIN, 12));
		textPane.putClientProperty(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
		JScrollPane scrollPane = new JScrollPane(textPane);
		// Splitters already separate the part windows - the scroll pane's own
		// L&F-default border would just draw a redundant line right next to them.
		scrollPane.setBorder(BorderFactory.createEmptyBorder());
		add(scrollPane, BorderLayout.CENTER);
	}

	/** Ported from PartWindow::ApplyLayout's SetFont(partLayout->GetLayout()->GetFont()) - call whenever the workspace's computer system or double-height setting changes. */
	public void setComputerFont(ComputerFont computerFont) {
		textPane.setFont(computerFont.getAwtFont());
		header.setComputerFont(computerFont);
	}

	/** Appends an info-severity line. May be called from any thread. */
	public void appendLine(String text) {
		appendLine(text, infoStyle);
	}

	/** Appends an error-severity line, shown in {@link #ERROR_COLOR}. May be called from any thread. */
	public void appendErrorLine(String text) {
		appendLine(text, errorStyle);
	}

	private void appendLine(String text, SimpleAttributeSet style) {
		Runnable task = () -> {
			StyledDocument document = textPane.getStyledDocument();
			try {
				document.insertString(document.getLength(), text + "\n", style);
			} catch (BadLocationException ex) {
				throw new IllegalStateException(ex);
			}
			textPane.setCaretPosition(document.getLength());
		};
		if (SwingUtilities.isEventDispatchThread()) {
			task.run();
		} else {
			SwingUtilities.invokeLater(task);
		}
	}
}
