/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

/**
 * A standalone byte-offset range selection: begin/end, swapped into order
 * and clamped to a caller-supplied size on every {@link #setSelection}. See
 * {@link ByteRangeSelection}'s javadoc for why this was extracted out of
 * {@link MutableMemoryInspectorState} - that class now holds one of these as
 * a private field and delegates to it, reproducing the same arithmetic it
 * always had; a second, fully independent instance is owned directly by
 * {@link com.wudsn.tools.dis6502.ui.DiskImageSectorsDialog} - this class is
 * meant to be reusable, with one independent instance per owner, never a
 * single shared one.
 *
 * @author Peter Dell
 */
public final class MutableByteRangeSelection implements ByteRangeSelection {

	private boolean selectionPresent;
	private int begin;
	private int end;

	public void clearSelection() {
		selectionPresent = false;
		begin = 0;
		end = 0;
	}

	@Override
	public boolean hasSelection() {
		return selectionPresent;
	}

	@Override
	public boolean isSelectionEmpty() {
		return !hasSelection();
	}

	/** Swaps {@code nBegin}/{@code nEnd} into order and clamps both to {@code [0, size-1]}. */
	public void setSelection(int nBegin, int nEnd, int size) {
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
}
