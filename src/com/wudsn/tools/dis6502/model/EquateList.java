/*
 * Copyright (C) 2025 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import org.w3c.dom.Element;

/**
 * A list of {@link Equate}s, e.g. the system equates or the user equates of
 * a workspace.
 * <p>
 * Ported from EquateList.h / EquateList.cpp. Not ported yet, deferred to
 * when {@code Workspace} is ported:
 * <ul>
 * <li>{@code Load1X}/{@code Save1X} (the workspace-version-1X binary
 * format),</li>
 * <li>{@code Load(filePath)}/{@code Save(filePath, xasm)} (depend on
 * application-level logging and file I/O, ported alongside the rest of the
 * application wiring).</li>
 * </ul>
 * The C++ source's {@code DeserializeFrom} passed the wrong element to each
 * {@code Equate::DeserializeFrom} call (the outer {@code <EquateList>}
 * element instead of the individual {@code <Equate>} child) - fixed
 * upstream and correct here from the start; see the fix commit for
 * details.
 *
 * @author Peter Dell
 */
public final class EquateList implements Xml.Serializable {

	private final WorkspaceProperty property;
	private final List<Equate> equateList = new ArrayList<>();
	private final List<EquateListChangedListener> listeners = new ArrayList<>();

	public EquateList(WorkspaceProperty property) {
		this.property = property;
	}

	public WorkspaceProperty getProperty() {
		return property;
	}

	public List<Equate> getEquates() {
		return Collections.unmodifiableList(equateList);
	}

	public void addListener(EquateListChangedListener listener) {
		listeners.add(Objects.requireNonNull(listener));
	}

	public void removeListeners() {
		listeners.clear();
	}

	private void notifyListeners() {
		for (EquateListChangedListener listener : listeners) {
			listener.handleEquateListChanged(this, property);
		}
	}

	public void clear() {
		equateList.clear();
		notifyListeners();
	}

	public boolean isEmpty() {
		return equateList.isEmpty();
	}

	public int getCount() {
		return equateList.size();
	}

	public int getLabelCount() {
		int result = 0;
		for (Equate equate : equateList) {
			if (equate.getType() == EquateType.LABEL) {
				result++;
			}
		}
		return result;
	}

	/** Creates and appends a new, still-uninitialized equate. Does not fire events. */
	private Equate addEquate() {
		Equate equate = new Equate();
		equateList.add(equate);
		return equate;
	}

	@Override
	public void serializeTo(Element element) {
		Xml.setWordAttribute(element, "Count", getCount());

		for (Equate equate : equateList) {
			Element equateElement = Xml.addChildElement(element, "Equate");
			equate.serializeTo(equateElement);
		}
	}

	@Override
	public void deserializeFrom(Element element) {
		clear();

		int count = Xml.getWordAttribute(element, "Count", 0);

		if (count > 0) {
			Element equateElement = Xml.getFirstChildElement(element);
			for (int equateIndex = 0; equateElement != null && equateIndex < count; equateIndex++) {
				Equate equate = addEquate();
				equate.deserializeFrom(equateElement);
				equateElement = Xml.getNextSiblingElement(equateElement, "Equate");
			}
		}
	}

	/**
	 * Finds an equate at the given address that supports the given access,
	 * and marks it as referenced with that access. Address 0 is never a
	 * valid label (it is the "no label" value). Unless {@code sdx} is true,
	 * SDX page 7 ($0700-$07FF) is never matched, since it is not used for
	 * non-SDX segments.
	 * <p>
	 * TODO: We must mark the system equates in a certain way instead of
	 * hardcoding page 7 here, but right now it does the job.
	 */
	public Equate findEquateByAddress(int address, int labelAccess, boolean sdx) {
		if (address == 0) {
			return null;
		}
		if (!sdx && address >= 0x0700 && address <= 0x07FF) {
			return null;
		}
		for (Equate equate : equateList) {
			if (equate.getLabelValue() == address && equate.isLabelAccessSupported(labelAccess)) {
				equate.addLabelReference(labelAccess);
				return equate;
			}
		}
		return null;
	}

	public Equate findAndMarkEquateByAddress(int address, int labelAccess) {
		Equate equate = findEquateByAddress(address, labelAccess, true);
		if (equate != null) {
			equate.addDefinition();
		}
		return equate;
	}

	public Equate getEquateByLabel(String label) {
		for (Equate equate : equateList) {
			if (equate.equalsLabel(label)) {
				return equate;
			}
		}
		return null;
	}

	public void setRange(String label, int labelAddress, int startAddress, int endAddress) {
		removeRange(startAddress, endAddress);
		addRange(label, labelAddress, startAddress, endAddress);
		notifyListeners();
	}

	// TODO: Add labelAccess.
	private void addRange(String label, int labelAddress, int startAddress, int endAddress) {
		for (int address = startAddress; address <= endAddress; address++) {
			String rangeLabel = label + (address > labelAddress ? "+" : "-")
					+ Math.abs(address - labelAddress);
			Equate equate = addEquate();
			equate.init(EquateType.LABEL, rangeLabel, LabelAccess.READ_WRITE, address, "");
		}
	}

	private void removeRange(int startAddress, int endAddress) {
		Iterator<Equate> it = equateList.iterator();
		while (it.hasNext()) {
			Equate equate = it.next();
			int labelAddress = equate.getLabelValue();
			if (labelAddress >= startAddress && labelAddress <= endAddress) {
				it.remove();
			}
		}
	}

	/** Clears the transient definition/reference flags of all equates. */
	public void clearFlags() {
		for (Equate equate : equateList) {
			equate.clearDefinition();
			equate.clearReferences();
		}
	}

	/**
	 * Detects labels of the form EXAMPLE+$xxxx. When found, label EXAMPLE is
	 * marked as referenced.
	 * <p>
	 * TODO: Consider parent equate list. Also consider recursion.
	 */
	public void setBaseLabelsReferenced() {
		for (Equate equate : equateList) {
			int referencedAccess = equate.getReferencedLabelAccess();
			if (equate.isRange() && referencedAccess != LabelAccess.UNKNOWN) {
				Equate baseEquate = getEquateByLabel(equate.getBaseLabel());
				// Be tolerant wrt. inconsistent label definitions.
				if (baseEquate != null) {
					baseEquate.addLabelReference(referencedAccess);
				}
			}
		}
	}

	public boolean isEquateAddressReferenced(int address, int labelAccess) {
		if (address == 0) {
			return false;
		}
		for (Equate equate : equateList) {
			if (equate.getLabelValue() == address && equate.hasReferencedLabelAccess(labelAccess)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Parses one line of an equates file and, if valid, appends the
	 * resulting equate. Returns {@code null} without appending anything for
	 * an empty/unparseable line.
	 * <p>
	 * Unlike the C++ version, a parse error is not yet reported anywhere
	 * (the C++ version sends it to the application's message log) - this is
	 * deferred until application-level logging is ported.
	 */
	public Equate addEquate(String line) {
		Equate.ReadResult result = Equate.readFrom(line);

		if (!result.error.isEmpty()) {
			return null;
		}
		if (result.equateType == EquateType.UNKNOWN) {
			return null;
		}
		Equate equate = addEquate();
		equate.init(result.equateType, result.label, result.labelAccess, result.address, result.comment);
		return equate;
	}
}
