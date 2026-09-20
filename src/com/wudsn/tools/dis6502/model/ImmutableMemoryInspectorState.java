/*
 * Copyright (C) 2025 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

/**
 * The read-only view of {@link MemoryInspectorState}: every query method
 * (which segment/offset range is selected, whether edit mode is active and
 * where its cursor is), without any of the methods that change that state
 * ({@link MemoryInspectorState#setSegmentIndex}/{@link
 * MemoryInspectorState#setSelection}/{@link MemoryInspectorState#clear}/
 * {@link MemoryInspectorState#clearSelection}, or the edit-mode {@link
 * MemoryInspectorState#enterEditMode}/{@link
 * MemoryInspectorState#quitEditMode}/{@link
 * MemoryInspectorState#moveEditCursor}/{@link
 * MemoryInspectorState#typeEditChar}). Lets a consumer that only ever reads
 * this state - painting the grid, formatting the title, deciding whether a
 * command is enabled - declare that in its own signature instead of
 * accepting the full read/write type, the same way {@link Segment} has no
 * such split (its callers are trusted to mutate it directly) but this
 * class's state transitions are narrow and deliberate enough to be worth
 * gating. Note this is a shallow read-only view: {@link #getSegment()}
 * still returns a fully mutable {@link Segment} - only which segment/
 * offset/mode is <i>current</i> is protected here, not a segment's byte
 * contents.
 *
 * @author Peter Dell
 */
public interface ImmutableMemoryInspectorState {

	Workspace getWorkspace();

	int getSegmentIndex();

	boolean hasSegment();

	Segment getSegment();

	boolean hasSelection();

	boolean isEmpty();

	int getBegin();

	int getEnd();

	int getSize();

	/** Returns the selection's begin/end addresses as a two-element array, or {@code null} if there is no segment. */
	int[] getAddressRange();

	byte[] getByteSequence();

	boolean isEditMode();

	int getEditCursorOffset();

	MemoryInspectorEditPane getEditCursorPane();
}
