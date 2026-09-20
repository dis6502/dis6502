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

	/** Matches {@code MemoryInspectorGridPanel.BYTES_PER_LINE} - kept as its own constant since this class (model) must not depend on that UI class. */
	private static final int EDIT_BYTES_PER_LINE = 16;

	private final Workspace workspace;

	private int segmentIndex;
	private Segment segment;

	private boolean selectionPresent;
	private int begin;
	private int end;

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
		selectionPresent = false;
		begin = 0;
		end = 0;
	}

	@Override
	public boolean hasSelection() {
		return hasSegment() && !segment.isEmpty() && selectionPresent;
	}

	@Override
	public boolean isSelectionEmpty() {
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
			this.begin = nBegin;
			this.end = nEnd;
		} else {
			this.begin = nEnd;
			this.end = nBegin;
		}

		this.selectionPresent = true;
	}

	@Override
	public int getBegin() {
		return begin;
	}

	@Override
	public int getEnd() {
		return end;
	}

	@Override
	public int getSize() {
		if (isSelectionEmpty()) {
			return 0;
		}
		return getEnd() - getBegin() + 1;
	}

	@Override
	public int[] getAddressRange() {
		if (segment == null) {
			return null;
		}
		return new int[] { segment.wBegin + begin, segment.wBegin + end };
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
		editCursorPane = EditPane.HEX_HIGH;
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
	public boolean moveEditCursor(EditCursorMovement movement) {
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
			if (offset - EDIT_BYTES_PER_LINE >= 0) {
				newOffset = offset - EDIT_BYTES_PER_LINE;
				newPane = pane != EditPane.ASCII ? EditPane.HEX_HIGH : pane;
			}
			break;
		case DOWN:
			if (offset + EDIT_BYTES_PER_LINE < size) {
				newOffset = offset + EDIT_BYTES_PER_LINE;
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
	 * Ported from {@code MemoryInspectorControlImpl::Char}: hex-digit/ASCII
	 * data entry, plus Tab to switch panes. Every valid keystroke writes
	 * immediately via {@link Segment#setData} - there is no staging buffer or
	 * undo, matching the C++ source. Returns {@link
	 * EditCharResult#NOT_HANDLED} (a no-op) if edit mode is not
	 * active.
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
