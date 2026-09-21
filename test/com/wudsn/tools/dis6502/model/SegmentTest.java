/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

/**
 * Ported from SegmentTest.h / SegmentTest.cpp.
 *
 * @author Peter Dell
 */
public final class SegmentTest {

	private SegmentTest() {
	}

	public static void testSegment() {

		Segment segment = new Segment();
		Assert.longEquals(segment.getSize(), 0);
		Assert.boolEquals(segment.isEmpty(), true);

		segment.wBegin = 0x2000;
		segment.wEnd = 0x3fff;
		Assert.longEquals(segment.getSize(), 0);

		segment.createMemoryBlockFromBeginToEnd();
		Assert.longEquals(segment.getSize(), 0x2000);

		int offset = 0;
		segment.setData(offset++, 0x020);
		segment.setData(offset++, 0x004);
		segment.setData(offset++, 0x020);
		segment.setData(offset, 0x060);

		AddressLabelList addressLabels = new AddressLabelList();

		testAddressLabels(addressLabels);

		Assert.log("SegmentTest completed");
	}

	/**
	 * Covers {@link Segment#deleteRange}/{@link Segment#insertRange}/{@link
	 * Segment#canInsertRange} - the model-layer support for the Memory
	 * Inspector's Delete/Cut/Paste Selection commands (gap #3), a
	 * from-scratch Java design with no C++ reference behavior to recover
	 * concrete assertions from (see {@code
	 * com.wudsn.tools.dis6502.ui.MemoryInspectorPanel}'s class javadoc for
	 * why). Hand-computed exact byte/offset values throughout, matching this
	 * project's testing convention.
	 */
	public static void testSegmentRangeEdit() {
		testDeleteRangeFromMiddle();
		testDeleteRangeShrinksToEmpty();
		testInsertRangeIntoMiddle();
		testCanInsertRange();

		Assert.log("SegmentTest.testSegmentRangeEdit completed");
	}

	private static void testDeleteRangeFromMiddle() {
		Segment segment = newTestSegment(0x2000, new int[] { 0x00, 0x11, 0x22, 0x33, 0x44, 0x55 });
		Comment insideComment = segment.allocateComment();
		insideComment.setOffset(2); // Inside the deleted range [2,4) - must be dropped.
		insideComment.setText("inside");
		Comment afterComment = segment.allocateComment();
		afterComment.setOffset(4); // After the deleted range - must shift down by 2.
		afterComment.setText("after");

		segment.deleteRange(2, 2); // Remove the 0x22,0x33 pair.

		Assert.longEquals(segment.getSize(), 4);
		Assert.longEquals(segment.wBegin, 0x2000);
		Assert.longEquals(segment.wEnd, 0x2003);
		Assert.longEquals(segment.getData(0), 0x00);
		Assert.longEquals(segment.getData(1), 0x11);
		Assert.longEquals(segment.getData(2), 0x44);
		Assert.longEquals(segment.getData(3), 0x55);

		Assert.longEquals(segment.comments.size(), 1);
		Assert.longEquals(segment.comments.get(0).getOffset(), 2);
		Assert.stringEquals(segment.comments.get(0).getText(), "after");
	}

	private static void testDeleteRangeShrinksToEmpty() {
		Segment segment = newTestSegment(0x4000, new int[] { 0xAA, 0xBB, 0xCC });
		segment.deleteRange(0, 3);

		Assert.longEquals(segment.getSize(), 0);
		Assert.boolEquals(segment.isEmpty(), true);
	}

	private static void testInsertRangeIntoMiddle() {
		Segment segment = newTestSegment(0x6000, new int[] { 0x01, 0x02, 0x03, 0x04 });
		Comment beforeComment = segment.allocateComment();
		beforeComment.setOffset(1); // Before the insertion point - stays put.
		beforeComment.setText("before");
		Comment atOrAfterComment = segment.allocateComment();
		atOrAfterComment.setOffset(2); // At/after the insertion point - shifts up.
		atOrAfterComment.setText("at");

		segment.insertRange(2, new byte[] { (byte) 0xF0, (byte) 0xF1 });

		Assert.longEquals(segment.getSize(), 6);
		Assert.longEquals(segment.wBegin, 0x6000);
		Assert.longEquals(segment.wEnd, 0x6005);
		Assert.longEquals(segment.getData(0), 0x01);
		Assert.longEquals(segment.getData(1), 0x02);
		Assert.longEquals(segment.getData(2), 0xF0);
		Assert.longEquals(segment.getData(3), 0xF1);
		Assert.longEquals(segment.getData(4), 0x03);
		Assert.longEquals(segment.getData(5), 0x04);
		Assert.boolEquals(segment.isType(2, MemoryType.UNKNOWN), true);
		Assert.boolEquals(segment.isType(3, MemoryType.UNKNOWN), true);

		Assert.longEquals(beforeComment.getOffset(), 1);
		Assert.longEquals(atOrAfterComment.getOffset(), 4);
	}

	private static void testCanInsertRange() {
		Segment segment = new Segment();
		segment.wBegin = 0x0000;
		segment.wEnd = 0xFFFE; // 0xFFFF (65535) bytes - one short of the 64k (0x10000) limit.
		segment.createMemoryBlockFromBeginToEnd();

		Assert.boolEquals(segment.canInsertRange(1), true);
		Assert.boolEquals(segment.canInsertRange(2), false);
	}

	private static Segment newTestSegment(int begin, int[] data) {
		Segment segment = new Segment();
		segment.wBegin = begin;
		segment.wEnd = begin + data.length - 1;
		segment.createMemoryBlockFromBeginToEnd();
		for (int i = 0; i < data.length; i++) {
			segment.setData(i, data[i]);
		}
		return segment;
	}

	private static void testAddressLabels(AddressLabelList addressLabels) {

		addressLabels.clear();
		for (int address = 0x3000; address < 0x4000; address++) {
			// Deterministic shuffling.
			int effectiveAddress = address;
			if ((address & 0x80) != 0) {
				effectiveAddress = effectiveAddress ^ 0x07f;
			}
			if ((address & 0x200) != 0) {
				effectiveAddress = effectiveAddress ^ 0x133;
			}
			addressLabels.allocateAddressLabel(effectiveAddress);
		}

		for (int address = 0x2000; address < 0x4000; address++) {
			AddressLabel addressLabel = addressLabels.findAddressLabel(address);
			if (address < 0x3000) {
				Assert.boolEquals(addressLabel != null, false);
			} else {
				Assert.boolEquals(addressLabel != null, true);
				Assert.longEquals(addressLabel.getAddress(), address);
				Assert.boolEquals(addressLabel.isAligned(), false);
				Assert.longEquals(addressLabel.getNearestAddress(), 0x0000);
			}
		}

		// Ensure that the list is sorted.
		int address = 0x3000;
		for (AddressLabel addressLabel : addressLabels.enumerate()) {
			Assert.longEquals(addressLabel.getAddress(), address++);
		}

		addressLabels.alignAddressLabels(0x3800);

		for (address = 0x3000; address < 0x4000; address++) {
			AddressLabel addressLabel = addressLabels.findAddressLabel(address);
			Assert.notNull(addressLabel);
			if (address < 0x3800) {
				Assert.boolEquals(addressLabel.isAligned(), false);
				Assert.longEquals(addressLabel.getNearestAddress(), 0x0000);
			} else if (address == 0x3800) {
				Assert.boolEquals(addressLabel.isAligned(), true);
				Assert.longEquals(addressLabel.getNearestAddress(), 0x0000);
			} else {
				Assert.boolEquals(addressLabel.isAligned(), false);
				Assert.longEquals(addressLabel.getNearestAddress(), 0x3800);
			}
		}
	}
}
