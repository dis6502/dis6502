/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

/**
 * Covers {@link LineNumberHistory}.
 *
 * @author Peter Dell
 */
public final class LineNumberHistoryTest {

	private LineNumberHistoryTest() {
	}

	public static void testLineNumberHistory() {
		LineNumberHistory history = new LineNumberHistory(3);
		Assert.boolEquals(history.isEmpty(), true);
		Assert.longEquals(history.pop(), 0); // Nowhere to go back to.

		// Last in, first out.
		history.push(10);
		history.push(20);
		Assert.boolEquals(history.isEmpty(), false);
		Assert.longEquals(history.pop(), 20);
		Assert.longEquals(history.pop(), 10);
		Assert.boolEquals(history.isEmpty(), true);

		// The same position twice in a row is remembered once.
		history.push(10);
		history.push(10);
		Assert.longEquals(history.pop(), 10);
		Assert.boolEquals(history.isEmpty(), true);

		// Full: the oldest position is forgotten, not the newest refused.
		history.push(1);
		history.push(2);
		history.push(3);
		history.push(4);
		Assert.longEquals(history.pop(), 4);
		Assert.longEquals(history.pop(), 3);
		Assert.longEquals(history.pop(), 2);
		Assert.boolEquals(history.isEmpty(), true);

		history.push(5);
		history.clear();
		Assert.boolEquals(history.isEmpty(), true);

		Assert.log("LineNumberHistoryTest completed");
	}
}
