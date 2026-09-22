/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
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
 * {@link #getAddressRange} returns {@code null} when there is no segment,
 * since a null array is the more natural Java way to say "no result" than
 * an out-parameter pair.
 * <p>
 * Edit mode - {@link #isEditMode()}, {@link #enterEditMode}/{@link
 * #quitEditMode()}, {@link #moveEditCursor}/{@link #typeEditChar} - lives
 * on the model layer rather than the UI control, so it can be exercised by
 * a plain, headless unit test instead of needing a real {@link
 * java.awt.event.KeyEvent}/{@link java.awt.event.MouseEvent}-driving Swing
 * test - see {@code com.wudsn.tools.dis6502.ui.MemoryInspectorPanel}'s
 * class javadoc for the keyboard/focus/timer/popup-menu wiring that still
 * lives there and drives these methods. It belongs on this class, not
 * {@code Workspace} itself: conceptually it is a workspace-wide lock
 * (blocking every other command while active) applied at a byte offset
 * within whatever segment this same object already has selected - {@link
 * #enterEditMode}/{@link #moveEditCursor}/{@link #typeEditChar} read
 * {@link #segment} directly rather than re-resolving "the current segment"
 * through {@link Workspace#getSegmentList()} a second time.
 * <p>
 * Implements {@link MemoryInspectorState} - see that interface's
 * javadoc for why - so {@link #segment} is private (with no external
 * writers before this split, confirmed by grep) rather than the public
 * field it used to be: a public mutable field would have left the "read
 * only" side of that split meaningless for anyone still holding this
 * concrete type.
 *
 * @author Peter Dell
 */
public final class MutableMemoryInspectorState implements MemoryInspectorState {

	private final Workspace workspace;

	private int segmentIndex;
	private Segment segment;

	private final MutableByteRangeSelection selection = new MutableByteRangeSelection();

	private boolean editMode;
	private int editCursorOffset = -1;
	private EditPane editCursorPane = EditPane.HEX_HIGH;

	public MutableMemoryInspectorState(Workspace workspace) {
		this.workspace = workspace;
		clear();
	}

	@Override
	public Workspace getWorkspace() {
		return workspace;
	}

	public void clear() {
		segmentIndex = SegmentList.NO_SEGMENT_INDEX;
		segment = null;
		clearSelection();
		quitEditMode();
	}

	@Override
	public int getSegmentIndex() {
		return segmentIndex;
	}

	@Override
	public boolean hasSegment() {
		return segmentIndex != SegmentList.NO_SEGMENT_INDEX;
	}

	public void setSegmentIndex(int segmentIndex) {
		this.segmentIndex = segmentIndex;
		segment = (segmentIndex == SegmentList.NO_SEGMENT_INDEX) ? null
				: workspace.getSegmentList().getSegment(segmentIndex);
	}

	@Override
	public Segment getSegment() {
		return segment;
	}

	public void clearSelection() {
		selection.clearSelection();
	}

	@Override
	public boolean hasSelection() {
		return hasSegment() && !segment.isEmpty() && selection.hasSelection();
	}

	@Override
	public boolean isSelectionEmpty() {
		return !hasSelection();
	}

	public void setSelection(int nBegin, int nEnd) {
		if (segment == null) {
			throw new IllegalStateException("No segment selected yet. Cannot set selection range.");
		}
		selection.setSelection(nBegin, nEnd, segment.getSize());
	}

	@Override
	public int getBegin() {
		return selection.getBegin();
	}

	@Override
	public int getEnd() {
		return selection.getEnd();
	}

	@Override
	public int getSize() {
		if (isSelectionEmpty()) {
			return 0;
		}
		return getEnd() - getBegin() + 1;
	}

	@Override
	public ByteRangeSelection getSelection() {
		return selection;
	}

	@Override
	public int[] getAddressRange() {
		if (segment == null) {
			return null;
		}
		return new int[] { segment.wBegin + getBegin(), segment.wBegin + getEnd() };
	}

	@Override
	public byte[] getByteSequence() {
		if (hasSelection()) {
			return Arrays.copyOfRange(segment.memoryBlock.getData(), getBegin(), getBegin() + getSize());
		}
		return new byte[0];
	}

	@Override
	public boolean isEditMode() {
		return editMode;
	}

	@Override
	public int getEditCursorOffset() {
		return editCursorOffset;
	}

	@Override
	public EditPane getEditCursorPane() {
		return editCursorPane;
	}

	/**
	 * Enters edit mode as a workspace-wide lock: while active, every other
	 * command that would mutate the selected segment (or any other one) is
	 * expected to stay blocked. {@code offset} is a byte offset into {@link
	 * #segment} - this method does not decide which offset to start at (a
	 * fresh selection's first byte, or an exact double-clicked position);
	 * that stays the caller's job. Returns {@code false} (leaving this
	 * object unchanged) if there is no selected segment or {@code offset} is
	 * out of range for it.
	 */
	public boolean enterEditMode(int offset, EditPane pane) {
		if (segment == null || offset < 0 || offset >= segment.getSize()) {
			return false;
		}
		editMode = true;
		editCursorOffset = offset;
		editCursorPane = pane;
		return true;
	}

	/**
	 * Releases the lock {@link #enterEditMode} takes - a no-op if edit mode
	 * was not active. This only clears model state; resyncing the UI
	 * selection and re-running the disassembly are {@code
	 * com.wudsn.tools.dis6502.ui.MemoryInspectorPanel#quitEditMode}'s job,
	 * not this one's.
	 */
	public void quitEditMode() {
		editMode = false;
		editCursorOffset = -1;
		editCursorPane = EditPane.HEX_HIGH;
	}

	/**
	 * Ported from {@code MemoryInspectorControlImpl::KeyDown}'s edit-mode
	 * branch: pure cursor navigation, no data change. The hex panes ({@code
	 * HEX_HIGH}/{@code HEX_LOW}) move nibble-wise on {@code LEFT}/{@code
	 * RIGHT} (crossing to the adjacent byte's far nibble at a boundary) and a
	 * whole line on {@code UP}/{@code DOWN}, resetting to the high nibble; the
	 * ASCII pane moves byte-wise for all six movements, with no nibble
	 * concept. {@code bytesPerLine} is the grid's current line width - a
	 * UI-computed fact ({@code
	 * com.wudsn.tools.dis6502.ui.HexGridPanel#getBytesPerLine()})
	 * that this model class does not own, so it is passed in for the
	 * {@code UP}/{@code DOWN} whole-line jump rather than hardcoded. Returns
	 * {@code false} (a no-op) if edit mode is not active.
	 */
	public boolean moveEditCursor(EditCursorMovement movement, int bytesPerLine) {
		if (!editMode) {
			return false;
		}
		int size = segment.getSize();
		int offset = editCursorOffset;
		EditPane pane = editCursorPane;

		int newOffset = offset;
		EditPane newPane = pane;
		switch (movement) {
		case HOME:
			newOffset = 0;
			newPane = pane != EditPane.ASCII ? EditPane.HEX_HIGH : pane;
			break;
		case END:
			newOffset = size - 1;
			newPane = pane != EditPane.ASCII ? EditPane.HEX_HIGH : pane;
			break;
		case UP:
			if (offset - bytesPerLine >= 0) {
				newOffset = offset - bytesPerLine;
				newPane = pane != EditPane.ASCII ? EditPane.HEX_HIGH : pane;
			}
			break;
		case DOWN:
			if (offset + bytesPerLine < size) {
				newOffset = offset + bytesPerLine;
				newPane = pane != EditPane.ASCII ? EditPane.HEX_HIGH : pane;
			}
			break;
		case LEFT:
			if (pane == EditPane.ASCII) {
				if (offset > 0) {
					newOffset = offset - 1;
				}
			} else if (pane == EditPane.HEX_LOW) {
				newPane = EditPane.HEX_HIGH;
			} else if (offset > 0) {
				newOffset = offset - 1;
				newPane = EditPane.HEX_LOW;
			}
			break;
		case RIGHT:
			if (pane == EditPane.ASCII) {
				if (offset + 1 < size) {
					newOffset = offset + 1;
				}
			} else if (pane == EditPane.HEX_HIGH) {
				newPane = EditPane.HEX_LOW;
			} else if (offset + 1 < size) {
				newOffset = offset + 1;
				newPane = EditPane.HEX_HIGH;
			}
			break;
		}

		editCursorOffset = newOffset;
		editCursorPane = newPane;
		return true;
	}

	/**
	 * Hex-digit/ASCII data entry, plus Tab to switch panes. Every valid
	 * keystroke writes immediately via {@link Segment#setData} - there is no
	 * staging buffer or undo. Returns {@link EditCharResult#NOT_HANDLED} (a
	 * no-op) if edit mode is not active.
	 */
	public EditCharResult typeEditChar(char c) {
		if (!editMode) {
			return EditCharResult.NOT_HANDLED;
		}
		int offset = editCursorOffset;
		if (segment == null || offset < 0 || offset >= segment.getSize()) {
			return EditCharResult.NOT_HANDLED;
		}
		EditPane pane = editCursorPane;

		if (pane != EditPane.ASCII) {
			if (c == '\t') {
				editCursorPane = EditPane.ASCII;
				return EditCharResult.HANDLED;
			}
			int nibble = Character.digit(c, 16);
			if (nibble < 0) {
				return EditCharResult.NOT_HANDLED;
			}
			int oldValue = segment.getData(offset) & 0xFF;
			int updated = pane == EditPane.HEX_HIGH ? (oldValue & 0x0F) | (nibble << 4) : (oldValue & 0xF0) | nibble;
			segment.setData(offset, updated);
			if (pane == EditPane.HEX_HIGH) {
				editCursorPane = EditPane.HEX_LOW;
				return EditCharResult.HANDLED;
			} else if (offset + 1 < segment.getSize()) {
				editCursorOffset = offset + 1;
				editCursorPane = EditPane.HEX_HIGH;
				return EditCharResult.HANDLED;
			} else {
				return EditCharResult.HANDLED_AT_BUFFER_END;
			}
		} else {
			if (c == '\t') {
				editCursorPane = EditPane.HEX_HIGH;
				return EditCharResult.HANDLED;
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
				return EditCharResult.NOT_HANDLED;
			}
			segment.setData(offset, toWrite);
			if (offset + 1 < segment.getSize()) {
				editCursorOffset = offset + 1;
				editCursorPane = EditPane.ASCII;
				return EditCharResult.HANDLED;
			} else {
				return EditCharResult.HANDLED_AT_BUFFER_END;
			}
		}
	}
}
