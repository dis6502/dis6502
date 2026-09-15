/*
 * Copyright (C) 2025 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

/**
 * An address referenced by code, or defined through a fix-up, within a
 * {@link Segment}. Transient - not serialized to XML.
 * <p>
 * Ported from AddressLabel.h / AddressLabel.cpp.
 *
 * @author Peter Dell
 */
public final class AddressLabel {

	private final int address;
	private int nearestAddress;
	private boolean aligned;

	public AddressLabel(int address) {
		this.address = address;
	}

	public int getAddress() {
		return address;
	}

	public int getNearestAddress() {
		return nearestAddress;
	}

	public void setNearestAddress(int value) {
		nearestAddress = value;
	}

	public boolean isAligned() {
		return aligned;
	}

	public void setAligned(boolean value) {
		aligned = value;
	}
}
