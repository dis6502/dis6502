/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * The disassembly listing's navigation history: the line numbers "Navigate
 * to Definition" was started from, most recent last, for "Navigate Back to
 * Previous Position" to return to.
 * <p>
 * Bounded: once full, the oldest position is forgotten to make room for a
 * new one, rather than refusing to record any further position (which
 * would otherwise leave "back" returning to some position from long ago
 * after a long session).
 *
 * @author Peter Dell
 */
public final class LineNumberHistory {

	public static final int DEFAULT_MAX_SIZE = 100;

	private final int maxSize;
	private final Deque<Integer> lineNumbers = new ArrayDeque<>();

	public LineNumberHistory() {
		this(DEFAULT_MAX_SIZE);
	}

	public LineNumberHistory(int maxSize) {
		if (maxSize < 1) {
			throw new IllegalArgumentException("Parameter 'maxSize' must be positive.");
		}
		this.maxSize = maxSize;
	}

	public boolean isEmpty() {
		return lineNumbers.isEmpty();
	}

	public void clear() {
		lineNumbers.clear();
	}

	/** Remembers a position. Returning to the position just remembered twice in a row is pointless, so that is ignored. */
	public void push(int lineNumber) {
		if (!lineNumbers.isEmpty() && lineNumbers.peekLast() == lineNumber) {
			return;
		}
		if (lineNumbers.size() == maxSize) {
			lineNumbers.removeFirst();
		}
		lineNumbers.addLast(lineNumber);
	}

	/** Returns and forgets the most recently remembered position, or 0 (no valid line number) if there is none. */
	public int pop() {
		Integer lineNumber = lineNumbers.pollLast();
		return lineNumber == null ? 0 : lineNumber;
	}
}
