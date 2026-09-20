/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

/**
 * The read-only view of {@link MutableByteRangeSelection} - the selection
 * arithmetic (begin/end offset, swapped and clamped) extracted out of {@link
 * MutableMemoryInspectorState} so a byte-range selection can be used
 * standalone, without a {@link Workspace}/{@link Segment}, e.g. by {@link
 * com.wudsn.tools.dis6502.ui.DiskImageSectorsDialog}. {@link
 * MutableMemoryInspectorState} now owns one of these and delegates to it
 * instead of tracking begin/end itself; its own {@link
 * MemoryInspectorState#hasSelection()} still composes this interface's
 * {@link #hasSelection()} with its own segment-presence check, since that
 * composition is segment-specific and does not belong here.
 *
 * @author Peter Dell
 */
public interface ByteRangeSelection {

	boolean hasSelection();

	boolean isSelectionEmpty();

	int getBegin();

	int getEnd();

	int getSize();
}
