/*
 * Copyright (C) 2025 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

import java.util.Arrays;

/**
 * Everything about what the Memory Inspector is currently looking at and
 * doing: the selected segment and byte-offset range within it, plus its
 * in-place hex/ASCII edit-mode lock and cursor - owned directly by {@link
 * Workspace} ({@link Workspace#getMemoryInspectorState()}), the same as
 * {@link Workspace#getSegmentList()}/{@link Workspace#getProfile()}, rather
 * than a satellite object constructed and passed around by callers.
 * <p>
 * The segment/selection tracking is ported from
 * MemoryInspectorSelection.h/.cpp (this class's former name, before edit
 * mode merged into it - see below). {@code GetAddressRage} keeps its
 * original (misspelled) C++ name; unlike the C++ version, which returns the
 * address pair through {@code word&} out-parameters and a {@code bool}
 * success flag, {@link #getAddressRange} returns {@code null} when there is
 * no segment, since a null array is the more natural Java way to say "no
 * result" than an out-parameter pair.
 * <p>
 * Edit mode - {@link #isEditMode()}, {@link #enterEditMode}/{@link
 * #quitEditMode()}, {@link #moveEditCursor}/{@link #typeEditChar} - is
 * ported from {@code MemoryInspector::SetEditMode}/{@code
 * MainController::QuitEditMode}/{@code MemoryInspectorControlImpl::KeyDown}/
 * {@code Char}. In the C++ source this state lives on the UI control
 * instead; it was moved to the model layer (first onto {@link Workspace}
 * directly, then here) so it can be exercised by a plain, headless unit
 * test instead of needing a real {@link java.awt.event.KeyEvent}/{@link
 * java.awt.event.MouseEvent}-driving Swing test - see {@code
 * com.wudsn.tools.dis6502.ui.MemoryInspectorPanel}'s class javadoc for the
 * keyboard/focus/timer/popup-menu wiring that still lives there and drives
 * these methods. It belongs on this class, not {@code Workspace} itself:
 * conceptually it is a workspace-wide lock (blocking every other command
 * while active, matching {@code MainMemoryInspector::PerformCommands}'s
 * modal gate in the C++ source) applied at a byte offset within whatever
 * segment this same object already has selected - {@link #enterEditMode}/
 * {@link #moveEditCursor}/{@link #typeEditChar} read {@link #segment}
 * directly rather than re-resolving "the current segment" through {@link
 * Workspace#getSegmentList()} a second time.
 *
 * @author Peter Dell
 */
public final class MemoryInspectorState {

	/** Matches {@code MemoryInspectorGridPanel.BYTES_PER_LINE} - kept as its own constant since this class (model) must not depend on that UI class. */
	private static final int EDIT_BYTES_PER_LINE = 16;

	private final Workspace workspace;

	public Segment segment;

	private int segmentIndex;

	private boolean selectionPresent;
	private int nBegin;
	private int nEnd;

	private boolean editMode;
	private int editCursorOffset = -1;
	private MemoryInspectorEditPane editCursorPane = MemoryInspectorEditPane.HEX_HIGH;

	public MemoryInspectorState(Workspace workspace) {
		this.workspace = workspace;
		clear();
	}

	public Workspace getWorkspace() {
		return workspace;
	}

	public void clear() {
		segmentIndex = SegmentList.NO_SEGMENT_INDEX;
		segment = null;
		clearSelection();
		quitEditMode();
	}

	public int getSegmentIndex() {
		return segmentIndex;
	}

	public boolean hasSegment() {
		return segmentIndex != SegmentList.NO_SEGMENT_INDEX;
	}

	public void setSegmentIndex(int segmentIndex) {
		this.segmentIndex = segmentIndex;
		segment = (segmentIndex == SegmentList.NO_SEGMENT_INDEX) ? null
				: workspace.getSegmentList().getSegment(segmentIndex);
	}

	public Segment getSegment() {
		return segment;
	}

	public void clearSelection() {
		selectionPresent = false;
		nBegin = 0;
		nEnd = 0;
	}

	public boolean hasSelection() {
		return hasSegment() && !segment.isEmpty() && selectionPresent;
	}

	public boolean isEmpty() {
		return !hasSelection();
	}

	public void setSelection(int nBegin, int nEnd) {
		if (segment == null) {
			throw new IllegalStateException("No segment selected yet. Cannot set selection range.");
		}

		int size = segment.getSize();

		if (nBegin >= size) {
			nBegin = size - 1;
		}
		if (nEnd >= size) {
			nEnd = size - 1;
		}

		if (nBegin <= nEnd) {
			this.nBegin = nBegin;
			this.nEnd = nEnd;
		} else {
			this.nBegin = nEnd;
			this.nEnd = nBegin;
		}

		this.selectionPresent = true;
	}

	public int getBegin() {
		return nBegin;
	}

	public int getEnd() {
		return nEnd;
	}

	public int getSize() {
		if (isEmpty()) {
			return 0;
		}
		return getEnd() - getBegin() + 1;
	}

	/** Returns the selection's begin/end addresses as a two-element array, or {@code null} if there is no segment. */
	public int[] getAddressRange() {
		if (segment == null) {
			return null;
		}
		return new int[] { segment.wBegin + nBegin, segment.wBegin + nEnd };
	}

	public byte[] getByteSequence() {
		if (hasSelection()) {
			return Arrays.copyOfRange(segment.memoryBlock.getData(), getBegin(), getBegin() + getSize());
		}
		return new byte[0];
	}

	public boolean isEditMode() {
		return editMode;
	}

	public int getEditCursorOffset() {
		return editCursorOffset;
	}

	public MemoryInspectorEditPane getEditCursorPane() {
		return editCursorPane;
	}

	/**
	 * Ported from {@code MemoryInspector::SetEditMode(true)}/{@code
	 * MemoryInspectorControlImpl::LButtonDblClk}'s cursor positioning - a
	 * workspace-wide lock: while active, every other command that would
	 * mutate the selected segment (or any other one) is expected to stay
	 * blocked, matching {@code MainMemoryInspector::PerformCommands}'s modal
	 * gate in the C++ source. {@code offset} is a byte offset into {@link
	 * #segment} - this method does not decide which offset to start at (a
	 * fresh selection's first byte, or an exact double-clicked position);
	 * that stays the caller's job. Returns {@code false} (leaving this
	 * object unchanged) if there is no selected segment or {@code offset} is
	 * out of range for it.
	 */
	public boolean enterEditMode(int offset, MemoryInspectorEditPane pane) {
		if (segment == null || offset < 0 || offset >= segment.getSize()) {
			return false;
		}
		editMode = true;
		editCursorOffset = offset;
		editCursorPane = pane;
		return true;
	}

	/**
	 * Ported from {@code MainController::QuitEditMode}: releases the lock
	 * {@link #enterEditMode} takes - a no-op if edit mode was not active.
	 * Unlike {@code
	 * com.wudsn.tools.dis6502.ui.MemoryInspectorPanel#quitEditMode}, this
	 * only clears model state; resyncing the UI selection and re-running the
	 * disassembly are that method's job, not this one's.
	 */
	public void quitEditMode() {
		editMode = false;
		editCursorOffset = -1;
		editCursorPane = MemoryInspectorEditPane.HEX_HIGH;
	}

	/**
	 * Ported from {@code MemoryInspectorControlImpl::KeyDown}'s edit-mode
	 * branch: pure cursor navigation, no data change. The hex panes ({@code
	 * HEX_HIGH}/{@code HEX_LOW}) move nibble-wise on {@code LEFT}/{@code
	 * RIGHT} (crossing to the adjacent byte's far nibble at a boundary) and a
	 * whole line on {@code UP}/{@code DOWN}, resetting to the high nibble; the
	 * ASCII pane moves byte-wise for all six movements, with no nibble
	 * concept. Returns {@code false} (a no-op) if edit mode is not active.
	 */
	public boolean moveEditCursor(MemoryInspectorEditCursorMovement movement) {
		if (!editMode) {
			return false;
		}
		int size = segment.getSize();
		int offset = editCursorOffset;
		MemoryInspectorEditPane pane = editCursorPane;

		int newOffset = offset;
		MemoryInspectorEditPane newPane = pane;
		switch (movement) {
		case HOME:
			newOffset = 0;
			newPane = pane != MemoryInspectorEditPane.ASCII ? MemoryInspectorEditPane.HEX_HIGH : pane;
			break;
		case END:
			newOffset = size - 1;
			newPane = pane != MemoryInspectorEditPane.ASCII ? MemoryInspectorEditPane.HEX_HIGH : pane;
			break;
		case UP:
			if (offset - EDIT_BYTES_PER_LINE >= 0) {
				newOffset = offset - EDIT_BYTES_PER_LINE;
				newPane = pane != MemoryInspectorEditPane.ASCII ? MemoryInspectorEditPane.HEX_HIGH : pane;
			}
			break;
		case DOWN:
			if (offset + EDIT_BYTES_PER_LINE < size) {
				newOffset = offset + EDIT_BYTES_PER_LINE;
				newPane = pane != MemoryInspectorEditPane.ASCII ? MemoryInspectorEditPane.HEX_HIGH : pane;
			}
			break;
		case LEFT:
			if (pane == MemoryInspectorEditPane.ASCII) {
				if (offset > 0) {
					newOffset = offset - 1;
				}
			} else if (pane == MemoryInspectorEditPane.HEX_LOW) {
				newPane = MemoryInspectorEditPane.HEX_HIGH;
			} else if (offset > 0) {
				newOffset = offset - 1;
				newPane = MemoryInspectorEditPane.HEX_LOW;
			}
			break;
		case RIGHT:
			if (pane == MemoryInspectorEditPane.ASCII) {
				if (offset + 1 < size) {
					newOffset = offset + 1;
				}
			} else if (pane == MemoryInspectorEditPane.HEX_HIGH) {
				newPane = MemoryInspectorEditPane.HEX_LOW;
			} else if (offset + 1 < size) {
				newOffset = offset + 1;
				newPane = MemoryInspectorEditPane.HEX_HIGH;
			}
			break;
		}

		editCursorOffset = newOffset;
		editCursorPane = newPane;
		return true;
	}

	/**
	 * Ported from {@code MemoryInspectorControlImpl::Char}: hex-digit/ASCII
	 * data entry, plus Tab to switch panes. Every valid keystroke writes
	 * immediately via {@link Segment#setData} - there is no staging buffer or
	 * undo, matching the C++ source. Returns {@link
	 * MemoryInspectorEditCharResult#NOT_HANDLED} (a no-op) if edit mode is not
	 * active.
	 */
	public MemoryInspectorEditCharResult typeEditChar(char c) {
		if (!editMode) {
			return MemoryInspectorEditCharResult.NOT_HANDLED;
		}
		int offset = editCursorOffset;
		if (segment == null || offset < 0 || offset >= segment.getSize()) {
			return MemoryInspectorEditCharResult.NOT_HANDLED;
		}
		MemoryInspectorEditPane pane = editCursorPane;

		if (pane != MemoryInspectorEditPane.ASCII) {
			if (c == '\t') {
				editCursorPane = MemoryInspectorEditPane.ASCII;
				return MemoryInspectorEditCharResult.HANDLED;
			}
			int nibble = Character.digit(c, 16);
			if (nibble < 0) {
				return MemoryInspectorEditCharResult.NOT_HANDLED;
			}
			int oldValue = segment.getData(offset) & 0xFF;
			int updated = pane == MemoryInspectorEditPane.HEX_HIGH ? (oldValue & 0x0F) | (nibble << 4) : (oldValue & 0xF0) | nibble;
			segment.setData(offset, updated);
			if (pane == MemoryInspectorEditPane.HEX_HIGH) {
				editCursorPane = MemoryInspectorEditPane.HEX_LOW;
				return MemoryInspectorEditCharResult.HANDLED;
			} else if (offset + 1 < segment.getSize()) {
				editCursorOffset = offset + 1;
				editCursorPane = MemoryInspectorEditPane.HEX_HIGH;
				return MemoryInspectorEditCharResult.HANDLED;
			} else {
				return MemoryInspectorEditCharResult.HANDLED_AT_BUFFER_END;
			}
		} else {
			if (c == '\t') {
				editCursorPane = MemoryInspectorEditPane.HEX_HIGH;
				return MemoryInspectorEditCharResult.HANDLED;
			}
			int toWrite;
			if (c == '\r' || c == '\n') {
				toWrite = 0x9B; // Atari end-of-line byte, written like any other typed character.
			} else if (c >= ' ' && c < 128) {
				// Deliberately NOT replicating MemoryInspectorControlImpl.cpp Char()'s
				// exclusion of '~', '{', '}' from the printable range - see
				// com.wudsn.tools.dis6502.ui.MemoryInspectorPanel's class javadoc.
				toWrite = c;
				if (segment.isType(offset, MemoryType.SBYTE)) {
					toWrite = MemoryType.toSbyteInternalCode(toWrite);
				}
			} else {
				return MemoryInspectorEditCharResult.NOT_HANDLED;
			}
			segment.setData(offset, toWrite);
			if (offset + 1 < segment.getSize()) {
				editCursorOffset = offset + 1;
				editCursorPane = MemoryInspectorEditPane.ASCII;
				return MemoryInspectorEditCharResult.HANDLED;
			} else {
				return MemoryInspectorEditCharResult.HANDLED_AT_BUFFER_END;
			}
		}
	}
}
