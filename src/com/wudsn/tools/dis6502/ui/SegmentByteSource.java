/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.ui;

import com.wudsn.tools.dis6502.model.MemoryType;
import com.wudsn.tools.dis6502.model.Segment;

/**
 * A {@link HexGridByteSource} backed by a workspace {@link Segment} - a thin
 * adapter, added only so {@link MemoryInspectorGridPanel} does not need to
 * depend on {@link Segment} directly; nothing was added to {@link Segment}'s
 * own public API for this.
 *
 * @author Peter Dell
 */
public final class SegmentByteSource implements HexGridByteSource {

	private final Segment segment;

	public SegmentByteSource(Segment segment) {
		this.segment = segment;
	}

	@Override
	public int getSize() {
		return segment.getSize();
	}

	@Override
	public int getData(int offset) {
		return segment.getData(offset);
	}

	@Override
	public MemoryType getType(int offset) {
		return segment.getType(offset);
	}

	@Override
	public int getBaseAddress() {
		return segment.wBegin;
	}
}
