/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

/**
 * A list of {@link AddressLabel}s, kept in address order and indexed by
 * address.
 * <p>
 * Ported from AddressLabelList.h / AddressLabelList.cpp. The C++ version
 * maintains a hand-sorted {@code vector} (for order) alongside a separate
 * {@code map} (for lookup by address); this is replaced with a single
 * {@link TreeMap}, which provides both address-ordered iteration and O(log n)
 * lookup.
 *
 * @author Peter Dell
 */
public final class AddressLabelList {

	private final TreeMap<Integer, AddressLabel> addressLabels = new TreeMap<>();

	/** Gets all address labels, in address order. */
	public List<AddressLabel> enumerate() {
		return new ArrayList<>(addressLabels.values());
	}

	public void clear() {
		addressLabels.clear();
	}

	public void allocateAddressLabel(int address) {
		addressLabels.computeIfAbsent(address, AddressLabel::new);
	}

	public AddressLabel findAddressLabel(int address) {
		return addressLabels.get(address);
	}

	/** Same as {@link #findAddressLabel(int)}; Java has no const-correctness to distinguish them. */
	public AddressLabel findMutableAddressLabel(int address) {
		return addressLabels.get(address);
	}

	/**
	 * For all not-yet-aligned labels: sets "aligned" if their label address
	 * matches the given address; otherwise sets the nearest address to the
	 * given address, if it is between the label address and the current
	 * nearest address.
	 */
	public void alignAddressLabels(int address) {
		for (AddressLabel addressLabel : addressLabels.values()) {
			if (!addressLabel.isAligned()) {
				if (addressLabel.getAddress() == address) {
					addressLabel.setAligned(true);
				} else if (addressLabel.getAddress() > address && addressLabel.getNearestAddress() < address) {
					addressLabel.setNearestAddress(address);
				}
			}
		}
	}

	/** Sets nearestAddress = address for every label. */
	public void alignNearestAddress() {
		for (AddressLabel addressLabel : addressLabels.values()) {
			addressLabel.setNearestAddress(addressLabel.getAddress());
		}
	}

	public AddressLabel findNearestAddressLabel(int address) {
		AddressLabel result = findAddressLabel(address);
		if (result != null && result.isAligned()) {
			return result;
		}

		for (AddressLabel addressLabel : addressLabels.values()) {
			if (addressLabel.getNearestAddress() == address) {
				return addressLabel;
			}
		}

		return null;
	}
}
