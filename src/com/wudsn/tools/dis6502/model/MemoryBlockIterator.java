/*
 * Copyright (C) 2025 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

/**
 * Sequential reader over a {@link MemoryBlock}'s raw byte content.
 * <p>
 * Ported from MemoryBlockIterator.h / MemoryBlockIterator.cpp.
 *
 * @author Peter Dell
 */
public final class MemoryBlockIterator {

	private final MemoryBlock memoryBlock;
	private int index;

	public MemoryBlockIterator(MemoryBlock memoryBlock) {
		this.memoryBlock = memoryBlock;
	}

	public boolean hasNext() {
		return index < memoryBlock.getSize();
	}

	public int getData() {
		return memoryBlock.getDataAt(index);
	}

	public int nextData() {
		int result = getData();
		next();
		return result;
	}

	public int nextAddress() {
		int low = getData();
		next();
		int high = getData();
		next();
		return Memory.toAddress(low, high);
	}

	public MemoryType getType() {
		return memoryBlock.getTypeAt(index);
	}

	public void next() {
		if (!hasNext()) {
			throw new IllegalStateException("End of memory block reached.");
		}
		index++;
	}
}
