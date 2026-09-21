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

import org.w3c.dom.Element;

/**
 * A list of {@link Equate}s, e.g. the system equates or the user equates of
 * a workspace.
 * <p>
 * Ported from EquateList.h / EquateList.cpp. {@code Save1X} is not ported -
 * see {@link Workspace1X}'s javadoc for why writing the legacy binary
 * format has no value going forward. The modern text equates file {@code
 * Load(filePath)}/{@code Save(filePath, xasm)} are ported onto
 * {@link EquateListLogic} instead of here, since they need the
 * application-level logging/file I/O that class's {@code Application}
 * parameter provides.
 * <p>
 * The C++ source's {@code DeserializeFrom} passed the wrong element to each
 * {@code Equate::DeserializeFrom} call (the outer {@code <EquateList>}
 * element instead of the individual {@code <Equate>} child) - fixed
 * upstream and correct here from the start; see the fix commit for
 * details.
 *
 * @author Peter Dell
 */
public final class EquateList implements Xml.Serializable {

	// Load1X's record layout: 2 bytes little-endian address, 1 unused/padding byte
	// (the original C++ source advances 3 bytes past a 2-byte field for reasons lost
	// to history - verified against a real DIS6502WRK14 fixture file, see Workspace1X),
	// 1 byte access, then a NUL-terminated ANSI label.
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

	/** Package-private so {@link EquateListLogic#load} can fire one notification after a bulk load, matching {@code EquateList::Load}'s single trailing {@code NotifyListeners()} call in C++. */
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
				// "IOCB0+ICCOM" uses ICCOM just as much as IOCB0 - without this, ICCOM is
				// omitted as unreferenced and the listing no longer assembles.
				if (equate.getOffsetLabel() != null) {
					Equate offsetEquate = getEquateByLabel(equate.getOffsetLabel());
					if (offsetEquate != null) {
						offsetEquate.addLabelReference(referencedAccess);
					}
				}
			}
		}
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
	 * reference - is responsible for reporting a non-empty {@code error},
	 * mirroring how C++'s {@code EquateList::AddEquate(line)} reports it
	 * directly via {@code g_Application->SendErrorMessageWithID(
	 * IDS_ERR_CANNOT_PARSE_EQUATE_LINE, line, errorString)}.
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
