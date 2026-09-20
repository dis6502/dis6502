/*
 * Copyright (C) 2025 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.ui;

import com.wudsn.tools.dis6502.model.Assert;
import com.wudsn.tools.dis6502.model.Segment;

/**
 * Pure-data coverage for the Memory Inspector edit mode's non-UI pieces -
 * the mouse/keyboard-driven parts (cursor navigation, blink timing, popup
 * swapping) are covered separately by ad hoc smoke tests instead, since this
 * project's hand-rolled test harness has no {@link java.awt.event.KeyEvent}/
 * {@link java.awt.event.MouseEvent}-driving support (see {@code TestRunner}'s
 * javadoc).
 *
 * @author Peter Dell
 */
public final class MemoryInspectorGridPanelTest {

	private MemoryInspectorGridPanelTest() {
	}

	public static void testMemoryInspectorGridPanel() {
		testToSbyteInternalCode();
		testNibbleWriteRoundTrip();

		Assert.log("MemoryInspectorGridPanelTest completed");
	}

	/** Ported from MemoryInspectorControlImpl::Char's SBYTE-typed-byte ASCII transform. */
	private static void testToSbyteInternalCode() {
		Assert.longEquals(MemoryInspectorGridPanel.toSbyteInternalCode(0), 64);
		Assert.longEquals(MemoryInspectorGridPanel.toSbyteInternalCode(31), 95);
		Assert.longEquals(MemoryInspectorGridPanel.toSbyteInternalCode(32), 0);
		Assert.longEquals(MemoryInspectorGridPanel.toSbyteInternalCode(65), 33); // 'A' -> 33, used by the smoke test too.
		Assert.longEquals(MemoryInspectorGridPanel.toSbyteInternalCode(95), 63);
		Assert.longEquals(MemoryInspectorGridPanel.toSbyteInternalCode(96), 96);
		Assert.longEquals(MemoryInspectorGridPanel.toSbyteInternalCode(127), 127);
	}

	/**
	 * Ported from MemoryInspectorControlImpl::Char's hex-nibble write path:
	 * masking a new high or low nibble into the current byte value, the same
	 * bit operations the key-typed handler in MemoryInspectorPanel applies
	 * before calling Segment#setData.
	 */
	private static void testNibbleWriteRoundTrip() {
		Segment segment = new Segment();
		segment.wBegin = 0x2000;
		segment.wEnd = 0x2000;
		segment.createMemoryBlockFromBeginToEnd();
		segment.setData(0, 0x00);

		int offset = 0;
		int highNibble = 0xA;
		int oldValue = segment.getData(offset) & 0xFF;
		segment.setData(offset, (oldValue & 0x0F) | (highNibble << 4));
		Assert.longEquals(segment.getData(offset) & 0xFF, 0xA0);

		int lowNibble = 0x5;
		oldValue = segment.getData(offset) & 0xFF;
		segment.setData(offset, (oldValue & 0xF0) | lowNibble);
		Assert.longEquals(segment.getData(offset) & 0xFF, 0xA5);
	}
}
