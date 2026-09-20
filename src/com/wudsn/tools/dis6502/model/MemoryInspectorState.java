/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

/**
 * The read-only view of {@link MutableMemoryInspectorState}: every query method
 * (which segment/offset range is selected, whether edit mode is active and
 * where its cursor is), without any of the methods that change that state
 * ({@link MutableMemoryInspectorState#setSegmentIndex}/{@link
 * MutableMemoryInspectorState#setSelection}/{@link MutableMemoryInspectorState#clear}/
 * {@link MutableMemoryInspectorState#clearSelection}, or the edit-mode {@link
 * MutableMemoryInspectorState#enterEditMode}/{@link
 * MutableMemoryInspectorState#quitEditMode}/{@link
 * MutableMemoryInspectorState#moveEditCursor}/{@link
 * MutableMemoryInspectorState#typeEditChar}). Lets a consumer that only ever reads
 * this state - painting the grid, formatting the title, deciding whether a
 * command is enabled - declare that in its own signature instead of
 * accepting the full read/write type, the same way {@link Segment} has no
 * such split (its callers are trusted to mutate it directly) but this
 * class's state transitions are narrow and deliberate enough to be worth
 * gating. Note this is a shallow read-only view: {@link #getSegment()}
 * still returns a fully mutable {@link Segment} - only which segment/
 * offset/mode is <i>current</i> is protected here, not a segment's byte
 * contents.
 * <p>
 * {@link EditPane}/{@link EditCursorMovement}/{@link EditCharResult} nest
 * here, dropping the {@code MemoryInspector} prefix their standalone former
 * selves had, since that prefix is now redundant with their enclosing type.
 *
 * @author Peter Dell
 */
public interface MemoryInspectorState {

	Workspace getWorkspace();

	int getSegmentIndex();

	boolean hasSegment();

	Segment getSegment();

	boolean hasSelection();

	boolean isSelectionEmpty();

	int getBegin();

	int getEnd();

	int getSize();

	/** Returns the selection's begin/end addresses as a two-element array, or {@code null} if there is no segment. */
	int[] getAddressRange();

	byte[] getByteSequence();

	boolean isEditMode();

	int getEditCursorOffset();

	EditPane getEditCursorPane();

	/**
	 * Which part of an edited byte's cell the Memory Inspector's edit-mode
	 * cursor is on - matches the C++ source's {@code wEditedPart} (0/1/2). Used
	 * by {@link MutableMemoryInspectorState}'s edit-mode state/methods and by
	 * {@link com.wudsn.tools.dis6502.ui.MemoryInspectorGridPanel}'s cursor
	 * painting.
	 */
	enum EditPane {

		HEX_HIGH,
		HEX_LOW,
		ASCII
	}

	/**
	 * A semantic cursor movement the Memory Inspector's edit mode accepts - the
	 * model-layer counterpart of the arrow/Home/End keys {@code
	 * MemoryInspectorControlImpl::KeyDown}'s edit-mode branch handles. {@code
	 * com.wudsn.tools.dis6502.ui.MemoryInspectorPanel} maps a raw {@link
	 * java.awt.event.KeyEvent} to one of these before calling {@link
	 * MutableMemoryInspectorState#moveEditCursor}, keeping the actual
	 * navigation math in the model layer, free of any Swing dependency.
	 */
	enum EditCursorMovement {

		HOME,
		END,
		UP,
		DOWN,
		LEFT,
		RIGHT
	}

	/** The outcome of {@link MutableMemoryInspectorState#typeEditChar}. */
	enum EditCharResult {

		/**
		 * The character was not valid input for the current pane (not a hex digit
		 * in a hex pane, not Tab, not a printable character or Enter in the ASCII
		 * pane) - nothing changed.
		 */
		NOT_HANDLED,

		/**
		 * The character was written (or, for Tab, switched panes) and the cursor
		 * moved to the next cell within the segment.
		 */
		HANDLED,

		/**
		 * The character was written at the segment's last byte - there is nowhere
		 * further to advance to, matching {@code MemoryInspectorControlImpl::Char}'s
		 * own end-of-buffer branch. The caller is expected to exit edit mode; see
		 * {@code com.wudsn.tools.dis6502.ui.MemoryInspectorPanel}'s class javadoc
		 * for the C++ bug this port fixes on this exact path.
		 */
		HANDLED_AT_BUFFER_END
	}
}
