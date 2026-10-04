/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import org.w3c.dom.Element;

/**
 * A list of {@link Equate}s, e.g. the system equates or the user equates of
 * a workspace.
 * <p>
 * {@code Save1X} is not implemented - see {@link Workspace1X}'s javadoc for
 * why writing the legacy binary format has no value going forward. The
 * modern text equates file load/save live on {@link EquateListLogic}
 * instead of here, since they need the application-level logging/file I/O
 * that class's {@code Application} parameter provides.
 *
 * @author Peter Dell
 */
public final class EquateList implements Xml.Serializable {

	// Load1X's record layout: 2 bytes little-endian address, 1 unused/padding byte
	// (its reason is lost to history - verified against a real DIS6502WRK14 fixture
	// file, see Workspace1X), 1 byte access, then a NUL-terminated ANSI label.
	private static final int MAX_BUF_LABEL_1X = 60000;
	private static final int LABEL_MEM_SIZE_1X = MAX_BUF_LABEL_1X + 32;

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

	/** Package-private so {@link EquateListLogic#load} can fire one notification after a bulk load. */
	void notifyListeners() {
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

	/** Reads user labels from the pre-3.0 binary workspace format. */
	public void load1X(InputStream inputStream) throws IOException {
		clear();

		byte[] marker = new byte[4];
		readFully(inputStream, marker);
		long equateArrayMarker = (marker[0] & 0xFFL) | ((marker[1] & 0xFFL) << 8) | ((marker[2] & 0xFFL) << 16)
				| ((marker[3] & 0xFFL) << 24);

		if (equateArrayMarker != 0L) {
			byte[] equateArray = new byte[LABEL_MEM_SIZE_1X];
			readFully(inputStream, equateArray);

			int p = 0;
			while (p < MAX_BUF_LABEL_1X) {
				int labelAddress = (equateArray[p] & 0xFF) | ((equateArray[p + 1] & 0xFF) << 8);
				if (labelAddress == 0) {
					break; // Unused equate slot.
				}
				p += 3;
				int labelAccess = equateArray[p] & 0xFF;
				p += 1;

				int labelStart = p;
				while (equateArray[p] != 0) {
					p++;
				}
				String label = new String(equateArray, labelStart, p - labelStart, StandardCharsets.ISO_8859_1);
				p += 1;

				Equate equate = addEquate();
				equate.init(EquateType.LABEL, label, labelAccess, labelAddress, "");
			}
		}
	}

	private static void readFully(InputStream inputStream, byte[] buffer) throws IOException {
		int totalRead = 0;
		while (totalRead < buffer.length) {
			int read = inputStream.read(buffer, totalRead, buffer.length - totalRead);
			if (read < 0) {
				throw new EOFException("Unexpected end of file.");
			}
			totalRead += read;
		}
	}

	/** The names of all contexts the equates use, sorted. */
	public Set<String> getContextNames() {
		Set<String> result = new TreeSet<>();
		for (Equate equate : equateList) {
			result.addAll(equate.getContexts());
		}
		return result;
	}

	/**
	 * Finds an equate at the given address that supports the given access,
	 * and marks it as referenced with that access. Address 0 is never a
	 * valid label (it is the "no label" value). Of the equates in one of the
	 * {@code activeContexts} and the global ones, the first in a context wins,
	 * else the first global one; equates of other contexts never match.
	 */
	public Equate findEquateByAddress(int address, int labelAccess, Set<String> activeContexts) {
		if (address == 0) {
			return null;
		}
		Equate globalEquate = null;
		for (Equate equate : equateList) {
			if (equate.getLabelValue() == address && equate.isLabelAccessSupported(labelAccess)) {
				if (equate.isInContext(activeContexts)) {
					equate.addLabelReference(labelAccess);
					return equate;
				}
				if (equate.isGlobal() && globalEquate == null) {
					globalEquate = equate;
				}
			}
		}
		if (globalEquate != null) {
			globalEquate.addLabelReference(labelAccess);
		}
		return globalEquate;
	}

	public Equate findAndMarkEquateByAddress(int address, int labelAccess, Set<String> activeContexts) {
		Equate equate = findEquateByAddress(address, labelAccess, activeContexts);
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

	/**
	 * Replaces the equates from {@code startAddress} to {@code endAddress} by
	 * range equates relative to the base label {@code label} at {@code
	 * labelAddress} ("LABEL+1", "LABEL-2", ...), with the base equate's {@code
	 * labelAccess}: a range on a {@code #} constant must not match memory
	 * accesses, one on a read-only register not writes.
	 */
	public void setRange(String label, int labelAddress, int labelAccess, int startAddress, int endAddress) {
		removeRange(startAddress, endAddress);
		addRange(label, labelAddress, labelAccess, startAddress, endAddress);
		notifyListeners();
	}

	private void addRange(String label, int labelAddress, int labelAccess, int startAddress, int endAddress) {
		for (int address = startAddress; address <= endAddress; address++) {
			String rangeLabel = label + (address > labelAddress ? "+" : "-")
					+ Math.abs(address - labelAddress);
			Equate equate = addEquate();
			equate.init(EquateType.LABEL, rangeLabel, labelAccess, address, "");
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

	/**
	 * Brings equates stored before contexts existed up to date: if none of
	 * them has contexts, each label gets the contexts of the label in {@code
	 * source} with the same name and value. Otherwise, or if nothing matches,
	 * nothing changes. Returns whether any equate changed.
	 */
	public boolean copyContextsFrom(EquateList source) {
		for (Equate equate : equateList) {
			if (!equate.isGlobal()) {
				return false;
			}
		}
		boolean changed = false;
		for (Equate equate : equateList) {
			if (equate.getType() == EquateType.LABEL) {
				Equate sourceEquate = source.getEquateByLabel(equate.getLabel());
				if (sourceEquate != null && sourceEquate.getLabelValue() == equate.getLabelValue()
						&& !sourceEquate.isGlobal()) {
					equate.setContexts(sourceEquate.getContexts());
					changed = true;
				}
			}
		}
		if (changed) {
			notifyListeners();
		}
		return changed;
	}

	/** Clears the transient definition/reference flags of all equates. */
	public void clearFlags() {
		for (Equate equate : equateList) {
			equate.clearDefinition();
			equate.clearReferences();
		}
	}

	/**
	 * Marks the base label of every referenced range label as referenced with
	 * the same access - "EXAMPLE+1" marks "EXAMPLE" - since the listing writes
	 * only the base. "IOCB0+ICCOM" also marks "ICCOM", which it uses just as
	 * much. A base is looked up in the range's own list first, then in the
	 * other lists: a user range can be based on a system equate. Repeats until
	 * nothing new is marked, since a base can itself be a range label; this
	 * ends, since each pass can only add access bits.
	 */
	public static void setBaseLabelsReferenced(EquateList... equateLists) {
		boolean changed;
		do {
			changed = false;
			for (EquateList equateList : equateLists) {
				for (Equate equate : equateList.equateList) {
					int referencedAccess = equate.getReferencedLabelAccess();
					if (equate.isRange() && referencedAccess != LabelAccess.UNKNOWN) {
						// Be tolerant wrt. inconsistent label definitions: a missing base is skipped.
						changed |= addLabelReference(
								getEquateByLabel(equate.getBaseLabel(), equateList, equateLists), referencedAccess);
						if (equate.getOffsetLabel() != null) {
							changed |= addLabelReference(
									getEquateByLabel(equate.getOffsetLabel(), equateList, equateLists),
									referencedAccess);
						}
					}
				}
			}
		} while (changed);
	}

	/** The equate with {@code label} in {@code ownList}, else in the first of {@code equateLists} that has one. */
	private static Equate getEquateByLabel(String label, EquateList ownList, EquateList[] equateLists) {
		Equate equate = ownList.getEquateByLabel(label);
		for (int i = 0; equate == null && i < equateLists.length; i++) {
			equate = equateLists[i].getEquateByLabel(label);
		}
		return equate;
	}

	/** Adds {@code labelAccess} to the equate's references; returns whether that added anything. */
	private static boolean addLabelReference(Equate equate, int labelAccess) {
		if (equate == null) {
			return false;
		}
		int before = equate.getReferencedLabelAccess();
		equate.addLabelReference(labelAccess);
		return equate.getReferencedLabelAccess() != before;
	}

	public boolean isEquateAddressReferenced(int address, int labelAccess) {
		if (address == DisassemblyLine.NO_SYSTEM_ADDRESS) {
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
	 * The result of {@link #addEquate(String)}: either the newly added
	 * {@link #equate}, or a non-empty {@link #error} describing why the
	 * line could not be parsed (nothing was appended in that case). The
	 * caller - not {@link EquateList}, which has no {@code Application}
	 * reference - is responsible for reporting a non-empty {@code error}.
	 */
	public static final class EquateResult {
		public final Equate equate;
		public final String error;

		EquateResult(Equate equate, String error) {
			this.equate = equate;
			this.error = error;
		}
	}

	/**
	 * Parses one line of an equates file and, if valid, appends the
	 * resulting equate.
	 */
	public EquateResult addEquate(String line) {
		Equate.ReadResult result = Equate.readFrom(line);

		if (!result.error.isEmpty()) {
			return new EquateResult(null, result.error);
		}
		if (result.equateType == EquateType.UNKNOWN) {
			return new EquateResult(null, "");
		}
		equateList.add(result.equate);
		return new EquateResult(result.equate, "");
	}
}
