/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

/**
 * Headless coverage for {@link MutableByteRangeSelection}'s swap/clamp
 * arithmetic - the standalone selection logic extracted out of {@link
 * MutableMemoryInspectorState}, see {@link ByteRangeSelection}'s javadoc.
 *
 * @author Peter Dell
 */
public final class ByteRangeSelectionTest {

	private ByteRangeSelectionTest() {
	}

	public static void testByteRangeSelection() {
		testInitialState();
		testSetSelection();
		testSwapWhenReversed();
		testClampToSize();
		testClearSelection();

		Assert.log("ByteRangeSelectionTest completed");
	}

	private static void testInitialState() {
		MutableByteRangeSelection selection = new MutableByteRangeSelection();
		Assert.boolEquals(selection.hasSelection(), false);
		Assert.boolEquals(selection.isSelectionEmpty(), true);
		Assert.longEquals(selection.getSize(), 0);
	}

	private static void testSetSelection() {
		MutableByteRangeSelection selection = new MutableByteRangeSelection();
		selection.setSelection(2, 5, 16);
		Assert.boolEquals(selection.hasSelection(), true);
		Assert.longEquals(selection.getBegin(), 2);
		Assert.longEquals(selection.getEnd(), 5);
		Assert.longEquals(selection.getSize(), 4);
	}

	private static void testSwapWhenReversed() {
		MutableByteRangeSelection selection = new MutableByteRangeSelection();
		selection.setSelection(5, 2, 16);
		Assert.longEquals(selection.getBegin(), 2);
		Assert.longEquals(selection.getEnd(), 5);
	}

	private static void testClampToSize() {
		MutableByteRangeSelection selection = new MutableByteRangeSelection();
		selection.setSelection(0, 100, 16);
		Assert.longEquals(selection.getBegin(), 0);
		Assert.longEquals(selection.getEnd(), 15); // Clamped to size-1.

		selection.setSelection(100, 200, 16);
		Assert.longEquals(selection.getBegin(), 15); // Both clamped to size-1, so begin == end.
		Assert.longEquals(selection.getEnd(), 15);
	}

	private static void testClearSelection() {
		MutableByteRangeSelection selection = new MutableByteRangeSelection();
		selection.setSelection(2, 5, 16);
		selection.clearSelection();
		Assert.boolEquals(selection.hasSelection(), false);
		Assert.longEquals(selection.getBegin(), 0);
		Assert.longEquals(selection.getEnd(), 0);
		Assert.longEquals(selection.getSize(), 0);
	}
}
