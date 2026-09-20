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
