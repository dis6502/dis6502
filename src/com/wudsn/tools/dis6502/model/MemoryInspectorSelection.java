/*
 * Copyright (C) 2025 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

import java.util.Arrays;

/**
 * The memory inspector's current segment and byte-offset selection within
 * it.
 * <p>
 * Ported from MemoryInspectorSelection.h / MemoryInspectorSelection.cpp.
 * {@code GetAddressRage} keeps its original (misspelled) C++ name; unlike
 * the C++ version, which returns the address pair through {@code word&}
 * out-parameters and a {@code bool} success flag, this returns {@code
 * null} when there is no segment, since a null array is the more natural
 * Java way to say "no result" than an out-parameter pair.
 *
 * @author Peter Dell
 */
public final class MemoryInspectorSelection {

	private final Workspace workspace;

	public Segment segment;

	private int segmentIndex;

	private boolean selectionPresent;
	private int nBegin;
	private int nEnd;

	public MemoryInspectorSelection(Workspace workspace) {
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
}
