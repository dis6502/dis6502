/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.ui;

import com.wudsn.tools.dis6502.model.MemoryType;

/**
 * A {@link HexGridByteSource} backed by a raw disk sector buffer, used by
 * {@link DiskImageSectorsDialog}. A disk sector has no {@link MemoryType}
 * classification, so {@link #getType} always returns {@link
 * MemoryType#UNKNOWN} - {@link MemoryInspectorGridPanel} already paints that
 * type in plain black, so no separate "coloring off" mode is needed.
 *
 * @author Peter Dell
 */
public final class DiskSectorByteSource implements HexGridByteSource {

	private final byte[] data;
	private final int size;
	private final int baseAddress;

	public DiskSectorByteSource(byte[] data, int size, int baseAddress) {
		this.data = data;
		this.size = size;
		this.baseAddress = baseAddress;
	}

	@Override
	public int getSize() {
		return size;
	}

	@Override
	public int getData(int offset) {
		return data[offset] & 0xFF;
	}

	@Override
	public MemoryType getType(int offset) {
		return MemoryType.UNKNOWN;
	}

	@Override
	public int getBaseAddress() {
		return baseAddress;
	}
}
