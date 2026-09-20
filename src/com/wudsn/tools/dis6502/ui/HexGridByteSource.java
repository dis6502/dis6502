/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.ui;

import com.wudsn.tools.dis6502.model.MemoryType;

/**
 * The bytes {@link HexGridPanel} paints, independent of where
 * they come from - a workspace {@link com.wudsn.tools.dis6502.model.Segment}
 * (via {@link SegmentByteSource}) or a raw disk sector buffer (via
 * {@link DiskSectorByteSource}). Lets the grid stay a pure painter with no
 * dependency on the heavier, {@code Workspace}-tied {@code Segment} model
 * class.
 *
 * @author Peter Dell
 */
public interface HexGridByteSource {

	int getSize();

	/** Returns the unsigned byte value (0..255) at {@code offset}. */
	int getData(int offset);

	/** Returns {@link MemoryType#UNKNOWN} for a source with no type data, e.g. a raw disk sector. */
	MemoryType getType(int offset);

	/** The address shown in the grid's address gutter for offset 0. */
	int getBaseAddress();
}
