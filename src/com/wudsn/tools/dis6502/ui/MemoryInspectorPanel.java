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

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.TitledBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;

import com.wudsn.tools.dis6502.model.FileHeader;
import com.wudsn.tools.dis6502.model.MemoryInspectorSelection;
import com.wudsn.tools.dis6502.model.Segment;

/**
 * A read-only hex/ASCII dump of the currently selected segment, with a
 * highlighted byte-range selection.
 * <p>
 * Ported from ui/MemoryInspectorWindow.h/.cpp and the display/selection
 * parts of ui/MemoryInspector.h/.cpp - {@link #segmentChanged} from {@code
 * MemoryInspector::SegmentChanged}, {@link #select}/{@link
 * #clearSelection} from {@code MemoryInspector::Select}/{@code
 * ClearSelection}, {@link #setDisplayAsScreenCode} from {@code
 * MemoryInspector::ToggleDisplayAsScreenCode}/{@code
 * MemoryInspectorControlImpl::SetInternal} - the ASCII column's byte-to-
 * character transform for "internal" (Atari ANTIC screen code) mode is
 * copied verbatim from {@code MemoryInspectorControlImpl.cpp}'s paint
 * routine, the one piece of that routine's rendering this class
 * replicates. Drastically simplified for this first pass, the same way
 * {@link DisassemblyPanel} simplifies the disassembly view: a plain,
 * non-editable text area rather than the C++ version's virtualized/
 * custom-painted grid (so unlike the real ANTIC font, non-printable
 * character codes still show as {@code .} rather than their actual
 * glyph), and no inline byte-type editing, popup menu (ui/
 * MemoryInspectorPopupMenu.h/.cpp), find-string dialog (ui/
 * MemoryInspectorFindStringDialog.h/.cpp), or "guess code"/sprite tools -
 * those all mutate the disassembly's understanding of the data and are
 * out of scope here.
 *
 * @author Peter Dell
 */
public final class MemoryInspectorPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	private static final int BYTES_PER_LINE = 16;

	private final TitledBorder titledBorder = BorderFactory.createTitledBorder("Memory Inspector");
	private final JTextArea hexDumpArea = new JTextArea();

	private MemoryInspectorSelection memoryInspectorSelection;
	private Segment currentSegment;
	private boolean displayAsScreenCode;
	private int[] byteCharOffsets = new int[0];
	private Object highlightTag;

	public MemoryInspectorPanel() {
		super(new BorderLayout());
		setBorder(titledBorder);
		hexDumpArea.setEditable(false);
		hexDumpArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
		add(new JScrollPane(hexDumpArea), BorderLayout.CENTER);
		showPlaceholder();
	}

	private void showPlaceholder() {
		hexDumpArea.setText("No segment selected.");
		currentSegment = null;
		byteCharOffsets = new int[0];
		highlightTag = null;
	}

	/**
	 * Ported from MemoryInspector::SegmentChanged. Some segments (an SDX
	 * symbol-table header, or an SDX relocation block with no data of its
	 * own) have nothing to display, matching the C++ version's {@code
	 * hasData} check.
	 */
	public void segmentChanged(MemoryInspectorSelection memoryInspectorSelection) {
		this.memoryInspectorSelection = memoryInspectorSelection;
		Segment segment = memoryInspectorSelection.getSegment();
		boolean hasData = segment != null && !segment.isHeader(FileHeader.SDX_SYM_DEFINED) && !segment.isSDXRelocBlkWithoutData();

		if (!hasData) {
			showPlaceholder();
			titledBorder.setTitle("Memory Inspector");
			revalidate();
			repaint();
			return;
		}

		titledBorder.setTitle(
				String.format("Memory Inspector - %s ($%04X-$%04X)", segment.title, segment.wBegin, segment.wEnd));
		buildHexDump(segment);
		memoryInspectorSelection.clearSelection();
		revalidate();
		repaint();
	}

	private void buildHexDump(Segment segment) {
		currentSegment = segment;
		int size = segment.getSize();
		byteCharOffsets = new int[size];

		StringBuilder text = new StringBuilder();
		for (int lineOffset = 0; lineOffset < size; lineOffset += BYTES_PER_LINE) {
			int lineEnd = Math.min(lineOffset + BYTES_PER_LINE, size);
			text.append(String.format("%04X: ", segment.wBegin + lineOffset));
			for (int i = lineOffset; i < lineOffset + BYTES_PER_LINE; i++) {
				if (i < lineEnd) {
					byteCharOffsets[i] = text.length();
					text.append(String.format("%02X ", segment.getData(i)));
				} else {
					text.append("   ");
				}
			}
			text.append(' ');
			for (int i = lineOffset; i < lineEnd; i++) {
				int value = displayAsScreenCode ? toInternalCode(segment.getData(i)) : segment.getData(i);
				text.append(value >= 32 && value < 127 ? (char) value : '.');
			}
			text.append('\n');
		}
		hexDumpArea.setText(text.toString());
		hexDumpArea.setCaretPosition(0);
		clearHighlight();
	}

	/**
	 * Ported verbatim from {@code MemoryInspectorControlImpl.cpp}'s paint
	 * routine's {@code bInternal} branch: converts a raw byte to the
	 * character its Atari internal (ANTIC screen code) representation
	 * would display as.
	 */
	private static int toInternalCode(int value) {
		if (value < 64) {
			return value + 32;
		} else if (value < 96) {
			return value - 64;
		} else if (value >= 128 && value < 128 + 64) {
			return value + 32;
		} else if (value >= 128 + 64 && value < 128 + 96) {
			return value - 64;
		}
		return value;
	}

	/**
	 * Ported from MemoryInspector::ToggleDisplayAsScreenCode/
	 * MemoryInspectorControlImpl::SetInternal. Switches the ASCII column
	 * between plain byte values and their Atari internal (ANTIC screen
	 * code) equivalent, re-rendering the currently displayed segment if
	 * there is one.
	 */
	public void setDisplayAsScreenCode(boolean displayAsScreenCode) {
		this.displayAsScreenCode = displayAsScreenCode;
		if (currentSegment != null) {
			buildHexDump(currentSegment);
		}
	}

	/** Ported from MemoryInspector::Select. */
	public void select(int begin, int end) {
		if (memoryInspectorSelection == null || memoryInspectorSelection.getSegment() == null
				|| memoryInspectorSelection.getSegment().isEmpty()) {
			return;
		}
		memoryInspectorSelection.setSelection(begin, end);
		highlightRange(memoryInspectorSelection.getBegin(), memoryInspectorSelection.getEnd());
	}

	/** Ported from MemoryInspector::ClearSelection. */
	public void clearSelection() {
		if (memoryInspectorSelection != null) {
			memoryInspectorSelection.clearSelection();
		}
		clearHighlight();
	}

	private void highlightRange(int begin, int end) {
		clearHighlight();
		if (byteCharOffsets.length == 0 || begin < 0 || begin >= byteCharOffsets.length) {
			return;
		}
		int endIndex = Math.min(end, byteCharOffsets.length - 1);
		int startOffset = byteCharOffsets[begin];
		int endOffset = byteCharOffsets[endIndex] + 2; // Each byte is rendered as exactly 2 hex digits.

		Highlighter highlighter = hexDumpArea.getHighlighter();
		try {
			highlightTag = highlighter.addHighlight(startOffset, endOffset, new DefaultHighlighter.DefaultHighlightPainter(Color.YELLOW));
			hexDumpArea.setCaretPosition(startOffset);
			Rectangle rectangle = hexDumpArea.modelToView(startOffset);
			if (rectangle != null) {
				hexDumpArea.scrollRectToVisible(rectangle);
			}
		} catch (BadLocationException ex) {
			highlightTag = null;
		}
	}

	private void clearHighlight() {
		if (highlightTag != null) {
			hexDumpArea.getHighlighter().removeHighlight(highlightTag);
			highlightTag = null;
		}
	}
}
