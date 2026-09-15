/*
 * Copyright (C) 2025 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * The full disassembly output: one {@link DisassemblySection} per {@link
 * DisassemblySectionType} (system equates, user equates, code equates, code
 * lines), each holding an ordered list of {@link DisassemblyLine}s. A
 * section is {@code null} until {@link #allocSection} is called for it.
 * <p>
 * Ported from DisassemblyResult.h / DisassemblyResult.cpp. The C++ version
 * has separate mutable/const line iterator classes ({@code
 * DisassemblyResultLineIterator}/{@code DisassemblyResultConstLineIterator})
 * for const-correctness; this has a single {@link Iterator}-based {@link
 * LineIterator} instead, the same simplification already used elsewhere in
 * this port (see {@code AddressLabelList.findMutableAddressLabel}).
 *
 * @author Peter Dell
 */
public final class DisassemblyResult {

	private static final List<DisassemblySectionType> SECTION_TYPES = List.of(DisassemblySectionType.SYSTEM_EQUATES,
			DisassemblySectionType.USER_EQUATES, DisassemblySectionType.CODE_EQUATES,
			DisassemblySectionType.CODE_LINES);

	private final DisassemblySection[] sections = new DisassemblySection[SECTION_TYPES.size()];

	public List<DisassemblySectionType> getSectionTypes() {
		return SECTION_TYPES;
	}

	/** Iterates every line in every section, in section order. */
	public LineIterator createLineIterator() {
		return new LineIterator(this, 0, getSectionCount() - 1);
	}

	/** Iterates the lines of a single section. */
	public LineIterator createLineIterator(DisassemblySectionType disassemblySectionType) {
		int index = DisassemblySection.getIndex(disassemblySectionType);
		return new LineIterator(this, index, index);
	}

	public void clear() {
		for (int i = 0; i < sections.length; i++) {
			sections[i] = null;
		}
	}

	public int getSectionCount() {
		return sections.length;
	}

	public DisassemblySection getSection(int index) {
		return sections[index];
	}

	public DisassemblySection getSection(DisassemblySectionType disassemblySectionType) {
		return getSection(DisassemblySection.getIndex(disassemblySectionType));
	}

	/** Clears the section for {@code disassemblySectionType} (drops all its lines). */
	public void clearSection(DisassemblySectionType disassemblySectionType) {
		sections[DisassemblySection.getIndex(disassemblySectionType)] = null;
	}

	/** Clears the system and user equate sections. */
	public void clearEquateSections() {
		clearSection(DisassemblySectionType.SYSTEM_EQUATES);
		clearSection(DisassemblySectionType.USER_EQUATES);
	}

	/** Allocates (or replaces) the section for {@code disassemblySectionType}. */
	public DisassemblySection allocSection(DisassemblySectionType disassemblySectionType) {
		int index = DisassemblySection.getIndex(disassemblySectionType);
		DisassemblySection section = new DisassemblySection(this, disassemblySectionType);
		sections[index] = section;
		return section;
	}

	public int getLineCount() {
		int lineCount = 0;
		for (DisassemblySection section : sections) {
			if (section != null) {
				lineCount += section.getLineCount();
			}
		}
		return lineCount;
	}

	/**
	 * Selects the line with the given line number, deselecting every other
	 * line. Returns the selected line, or {@code null} if none matched.
	 */
	public DisassemblyLine selectLine(int lineNumber) {
		DisassemblyLine selectedLine = null;
		for (LineIterator i = createLineIterator(); i.hasNext();) {
			DisassemblyLine line = i.next();
			if (line.getLineNumber() == lineNumber) {
				selectedLine = line;
				line.selected = true;
			} else {
				line.selected = false;
			}
		}
		return selectedLine;
	}

	/**
	 * Selects the line at the given segment/offset, deselecting every other
	 * line. Returns the selected line's number, or 0 if none matched.
	 */
	public int selectLine(int segmentIndex, int offset) {
		int selectedLineNumber = 0;
		for (LineIterator i = createLineIterator(); i.hasNext();) {
			DisassemblyLine line = i.next();
			if (selectedLineNumber == 0 && line.segmentIndex == segmentIndex && line.offset == offset) {
				selectedLineNumber = line.getLineNumber();
				line.selected = true;
			} else {
				line.selected = false;
			}
		}
		return selectedLineNumber;
	}

	/**
	 * Extends the current selection up to and including the line at the given
	 * segment/offset. Returns {@code false} without changing anything if no
	 * line matches that segment/offset.
	 */
	public boolean extendSelectionTo(int segmentIndex, int offset) {
		int selectedLineNumber = 0;
		for (LineIterator i = createLineIterator(); i.hasNext();) {
			DisassemblyLine line = i.next();
			if (line.segmentIndex == segmentIndex && line.offset == offset) {
				selectedLineNumber = line.getLineNumber();
			}
		}

		if (selectedLineNumber != 0) {
			boolean selected = false;
			for (LineIterator i = createLineIterator(); i.hasNext();) {
				DisassemblyLine line = i.next();
				if (line.getLineNumber() > selectedLineNumber) {
					break;
				}
				if (line.selected) {
					selected = true;
				}
				line.selected = selected;
			}
			return true;
		}
		return false;
	}

	/**
	 * Finds and selects lines containing {@code findString}. On {@code first},
	 * scans every line from the start, numbering every match for the XRef
	 * search window and selecting the first one found; otherwise scans
	 * forward from {@code findFirstLineNumber[0]} for the next match.
	 * {@code findFirstLineNumber[0]} is updated to the found line's number.
	 */
	public boolean findAndSelectLines(boolean first, int[] findFirstLineNumber, String findString) {
		boolean found = false;
		int xrefLineNumber = 1;

		for (LineIterator i = createLineIterator(); i.hasNext();) {
			DisassemblyLine line = i.next();

			// Reset selection flag on the current line.
			line.selected = false;

			if (first) {
				line.xrefLineNumber = 0;

				// Do we have the substring in the line?
				if (line.getLine().contains(findString)) {
					// This is the good line. Mark it as selected.
					line.xrefLineNumber = xrefLineNumber++;

					// Keep the first line for the next search operation.
					if (findFirstLineNumber[0] == 0) {
						line.selected = true;
						findFirstLineNumber[0] = line.getLineNumber();
					}

					// Continue the loop to reset other selection flags on other lines.
					found = true;
				}
			} else {
				// Ignore all lines before the line found in a previous search, and after the line has been found.
				if (line.getLineNumber() >= findFirstLineNumber[0] && !found) {
					// Do we have the substring in the line?
					if (line.getLine().contains(findString)) {
						// This is the good line. Mark it as selected.
						line.selected = true;
						// Keep the first line for the next search operation.
						findFirstLineNumber[0] = line.getLineNumber();
						// Continue the loop to reset other selection flags on other lines.
						found = true;
					}
				}
			}
		}
		return found;
	}

	/**
	 * Finds the offset/size of the CODE_LINES instruction in
	 * {@code segmentIndex} that spans offset 0, writing them to
	 * {@code offset[0]}/{@code size[0]} (both reset to 0 first, and left at 0
	 * if nothing matches) - a faithful port of the C++ signature, where
	 * {@code offset} is passed by reference purely as an out-parameter despite
	 * its name.
	 */
	public void findOffsetAtStartOfInstruction(int segmentIndex, int[] offset, int[] size) {
		offset[0] = 0;
		size[0] = 0;
		for (LineIterator i = createLineIterator(DisassemblySectionType.CODE_LINES); i.hasNext();) {
			DisassemblyLine line = i.next();

			if (line.segmentIndex == segmentIndex && line.size > 0 && line.offset <= offset[0]
					&& line.offset + line.size > offset[0]) {
				offset[0] = line.offset;
				size[0] = line.size;
				return;
			}
		}
	}

	/**
	 * Sequentially iterates every line across a contiguous range of sections.
	 * Also assigns each returned line's line number as a running counter
	 * starting at 1 - matching the C++ source's {@code
	 * DisassemblyResultLineIterator::Next}, which is the only place {@code
	 * DIS_LINE::SetLineNumber} is ever called. A line's number is therefore
	 * only meaningful right after being visited by a full traversal (e.g.
	 * {@link #createLineIterator()} run to completion); an earlier traversal
	 * (say, only over one section) leaves other lines' numbers stale or
	 * unset, exactly as in the C++ version.
	 */
	public static final class LineIterator implements Iterator<DisassemblyLine> {

		private final DisassemblyResult result;
		private final int endSectionIndex;
		private int sectionIndex;
		private int lineIndex;
		private int lineNumber;

		private LineIterator(DisassemblyResult result, int startSectionIndex, int endSectionIndex) {
			this.result = result;
			this.sectionIndex = startSectionIndex;
			this.endSectionIndex = endSectionIndex;
			advanceToNextLine();
		}

		private void advanceToNextLine() {
			while (sectionIndex <= endSectionIndex) {
				DisassemblySection section = result.getSection(sectionIndex);
				if (section != null && lineIndex < section.lines.size()) {
					return;
				}
				sectionIndex++;
				lineIndex = 0;
			}
		}

		@Override
		public boolean hasNext() {
			return sectionIndex <= endSectionIndex;
		}

		@Override
		public DisassemblyLine next() {
			if (!hasNext()) {
				throw new NoSuchElementException();
			}
			DisassemblyLine line = result.getSection(sectionIndex).lines.get(lineIndex);
			lineIndex++;
			lineNumber++;
			line.setLineNumber(lineNumber);
			advanceToNextLine();
			return line;
		}
	}
}
