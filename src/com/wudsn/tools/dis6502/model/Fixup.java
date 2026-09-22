/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

import java.util.Comparator;

/**
 * An address to fix up in a {@link Segment}: the address of the word to fix
 * up, and the index of the segment where the referenced label is located.
 * <p>
 * Has no XML persistence: it is a transient attribute of {@code Segment},
 * rebuilt by every disassembly.
 *
 * @author Peter Dell
 */
public final class Fixup {

	public static final Comparator<Fixup> BY_ADDRESS = Comparator.comparingInt(Fixup::getAddress);

	private int address; // Address of the word to fix up in the segment.
	private int labelSegmentIndex; // Segment where this address is located.

	public int getAddress() {
		return address;
	}

	public void setAddress(int address) {
		this.address = address;
	}

	public int getLabelSegmentIndex() {
		return labelSegmentIndex;
	}

	public void setLabelSegmentIndex(int labelSegmentIndex) {
		this.labelSegmentIndex = labelSegmentIndex;
	}
}
