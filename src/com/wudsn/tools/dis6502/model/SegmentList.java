/*
 * Copyright (C) 2025 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.w3c.dom.Element;

import com.wudsn.tools.base.common.HexUtility;
import com.wudsn.tools.base.common.Log;

/**
 * The list of {@link Segment}s of a {@link Workspace}, plus the transient
 * "global segment" that holds address labels not owned by any real segment.
 * <p>
 * Ported from SegmentList.h / SegmentList.cpp.
 *
 * @author Peter Dell
 */
public final class SegmentList implements Xml.Serializable {

	public static final int MAX_SEGMENTS = 4096;
	public static final int NO_SEGMENT_INDEX = -1;

	public enum Property {
		SEGMENTS, SEGMENT_CONTENT, SELECTED_INDEX
	}

	private final Workspace workspace; // May be null if no equate handling is performed.
	private final List<Segment> segmentList = new ArrayList<>();
	private final Segment globalSegment = new Segment();

	private int selectedIndex = NO_SEGMENT_INDEX;

	// Event handling.
	private int updateCounter;
	private final List<Property> propertyChangeEvents = new ArrayList<>();
	private final List<SegmentListChangedListener> listeners = new ArrayList<>();

	public SegmentList(Workspace workspace) {
		this.workspace = workspace;
	}

	public void clear() {
		beginUpdate();
		setSelectedIndex(NO_SEGMENT_INDEX);
		segmentList.clear();
		globalSegment.clear();
		notifyListeners(Property.SEGMENTS);
		endUpdate();
	}

	public boolean isEmpty() {
		return segmentList.isEmpty();
	}

	/** Gets the number of used segments. */
	public int getCount() {
		return segmentList.size();
	}

	public int getSelectedIndex() {
		return selectedIndex;
	}

	public void setSelectedIndex(int segmentIndex) {
		selectedIndex = segmentIndex;
		notifyListeners(Property.SELECTED_INDEX);
	}

	public SegmentListInserter createInserter() {
		return new SegmentListInserter(this);
	}

	public Segment getGlobalSegment() {
		return globalSegment;
	}

	public int getSegmentIndex(Segment segment) {
		if (segment == null) {
			return NO_SEGMENT_INDEX;
		}
		int segmentCount = getCount();
		for (int segmentIndex = 0; segmentIndex < segmentCount; segmentIndex++) {
			if (segmentList.get(segmentIndex) == segment) {
				return segmentIndex;
			}
		}
		return NO_SEGMENT_INDEX;
	}

	public Segment getSegment(int segmentIndex) {
		return segmentList.get(segmentIndex);
	}

	private Segment addSegment() {
		return insertSegmentAt(getCount());
	}

	@Override
	public void serializeTo(Element element) {
		Xml.setWordAttribute(element, "Count", getCount());
		for (Segment segment : segmentList) {
			segment.serializeTo(Xml.addChildElement(element, "Segment"));
		}
	}

	@Override
	public void deserializeFrom(Element element) {
		beginUpdate();
		clear();
		int count = Xml.getWordAttribute(element, "Count", 0);
		if (count > 0) {
			Element segmentElement = Xml.getFirstChildElement(element);
			for (int segmentIndex = 0; segmentElement != null && segmentIndex < count; segmentIndex++) {
				Segment segment = addSegment();
				segment.deserializeFrom(segmentElement);
				segmentElement = Xml.getNextSiblingElement(segmentElement, "Segment");
			}
			setSelectedIndex(0);
		}
		endUpdate();
	}

	public Segment insertSegmentAt(int segmentIndex) {
		if (segmentIndex > getCount()) {
			throw new IndexOutOfBoundsException("Invalid index larger than the current size of the list.");
		}
		Segment segment = new Segment();
		segmentList.add(segmentIndex, segment);
		return segment;
	}

	void deleteSegment(int segmentIndex) {
		segmentList.remove(segmentIndex);
		notifyListeners(Property.SEGMENTS);
	}

	/** Checks if a fix-up exists for this address. */
	private Fixup getFixup(int segmentIndex, MemoryType disByteType, int pc, int address) {
		return segmentList.get(segmentIndex).findFixup(pc);
	}

	/** Returns the segment whose SDX block number is given, or {@code null}. */
	public Segment findBySDXBlockNumber(int sdxBlockNumber) {
		// Try to find a block with the same id.
		for (Segment segment : segmentList) {
			if (segment.isHeader(FileHeader.SDX_RELOC_BLK) && segment.bSDXBlockNumber == sdxBlockNumber) {
				return segment;
			}
		}

		// No block id match: get the n-th block of code.
		int blockNumber = 0;
		for (Segment segment : segmentList) {
			if (segment.isHeader(FileHeader.ATARI_BINARY) || segment.isHeader(FileHeader.SDX_FIXED_BLK)
					|| segment.isHeader(FileHeader.SDX_RELOC_BLK)) {
				blockNumber++;
				if (blockNumber == sdxBlockNumber) {
					return segment;
				}
			}
		}
		return null;
	}

	/** Returns the index of the segment containing the given address. */
	public int findByAddr(int address) {
		int segmentCount = getCount();
		for (int segmentIndex = 0; segmentIndex < segmentCount; segmentIndex++) {
			Segment segment = segmentList.get(segmentIndex);
			if (segment.isHeader(FileHeader.ATARI_BINARY) || segment.isHeader(FileHeader.SDX_FIXED_BLK)
					|| segment.isHeader(FileHeader.SDX_RELOC_BLK)) {
				if (segment.wBegin <= address && segment.wEnd >= address) {
					return segmentIndex;
				}
			}
		}
		return NO_SEGMENT_INDEX;
	}

	/** Returns the index of the segment which contains the address. */
	private int findSegmentByFixedAddr(int segmentIndex, int address) {
		// Try to find it in the current segment first.
		Segment segment = segmentList.get(segmentIndex);
		if (segment.containsAddress(address)) {
			return segmentIndex;
		}

		// Not found, try to find it in all segments.
		int segmentCount = getCount();
		for (int i = 0; i < segmentCount; i++) {
			Segment otherSegment = segmentList.get(i);
			if (otherSegment.isHeader(FileHeader.ATARI_BINARY) || otherSegment.isHeader(FileHeader.SDX_FIXED_BLK)) {
				if (otherSegment.containsAddress(address)) {
					return i;
				}
			}
		}
		return NO_SEGMENT_INDEX;
	}

	/** Returns the index of the segment for the target address but with the same kind of segment (file header). */
	private int findSegmentWithSameFileHeaderByAddr(int segmentIndex, int address) {
		Segment sourceSegment = segmentList.get(segmentIndex);
		if (sourceSegment.containsAddress(address)) {
			return segmentIndex;
		}

		int segmentCount = getCount();
		for (int otherSegmentIndex = 0; otherSegmentIndex < segmentCount; otherSegmentIndex++) {
			if (otherSegmentIndex != segmentIndex) {
				Segment otherSegment = segmentList.get(otherSegmentIndex);
				if (otherSegment.isHeader(sourceSegment.getHeader()) && otherSegment.containsAddress(address)) {
					return segmentIndex; // NOTE: returns segmentIndex, not otherSegmentIndex - matches the C++ source exactly.
				}
			}
		}
		return NO_SEGMENT_INDEX;
	}

	public int mergeSegments() {
		beginUpdate();

		int mergedCount = 0;
		int count = getCount();
		for (int segmentIndex = 0; segmentIndex < count - 1; segmentIndex++) {
			Segment segment = segmentList.get(segmentIndex);
			Segment nextSegment = segmentList.get(segmentIndex + 1);

			if (segment.canMergeWith(nextSegment)) {
				segment.mergeWith(nextSegment);
				deleteSegment(segmentIndex + 1);
				mergedCount++;
			}
		}
		if (mergedCount > 0) {
			setSelectedIndex(0);
		}
		endUpdate();
		return mergedCount;
	}

	public void splitSelectedSegment(int offset) {
		int segmentIndex = getSelectedIndex();
		if (segmentIndex == NO_SEGMENT_INDEX) {
			return;
		}
		Segment segment = segmentList.get(segmentIndex);

		beginUpdate();
		if (!segment.memoryBlock.isEmpty()) {
			Segment newSegment = insertSegmentAt(segmentIndex + 1);
			segment.splitAt(offset, newSegment);
			setSelectedIndex(segmentIndex + 1);
		}
		endUpdate();
	}

	public void deleteSelectedSegment() {
		int segmentIndex = getSelectedIndex();
		if (segmentIndex == NO_SEGMENT_INDEX) {
			return;
		}

		beginUpdate();
		deleteSegment(segmentIndex);

		// Select the previous segment, if there is any.
		segmentIndex = (segmentIndex > 0) ? segmentIndex - 1 : NO_SEGMENT_INDEX;
		setSelectedIndex(segmentIndex);
		endUpdate();
	}

	public void moveSelectedSegmentUp() {
		int segmentIndex = getSelectedIndex();
		if (segmentIndex > 0) {
			int newSegmentIndex = segmentIndex - 1;
			swapSegments(segmentIndex, newSegmentIndex);
			notifyListeners(Property.SEGMENTS);
			setSelectedIndex(newSegmentIndex);
		}
	}

	public void moveSelectedSegmentDown() {
		int segmentIndex = getSelectedIndex();
		beginUpdate();
		if (segmentIndex < getCount() - 1) {
			int newSegmentIndex = segmentIndex + 1;
			swapSegments(segmentIndex, newSegmentIndex);
			notifyListeners(Property.SEGMENTS);
			setSelectedIndex(newSegmentIndex);
		}
		endUpdate();
	}

	private void swapSegments(int indexA, int indexB) {
		Segment a = segmentList.get(indexA);
		Segment b = segmentList.get(indexB);
		segmentList.set(indexA, b);
		segmentList.set(indexB, a);
	}

	public void freeAllSymbols() {
		for (Segment segment : segmentList) {
			segment.symbols.clear();
		}
	}

	public void freeAllFixups() {
		for (Segment segment : segmentList) {
			segment.clearFixups();
		}
	}

	public void freeAllFixupAddressLabels() {
		for (Segment segment : segmentList) {
			segment.getFixupAddressLabels().clear();
		}
	}

	public boolean allocateAddress(int segmentIndex, int pc, int address, MemoryType type, Instruction instruction) {
		return allocateAddress(segmentIndex, pc, address, type, instruction.getOperandMode(),
				instruction.getLabelAccess());
	}

	public boolean allocateAddress(int segmentIndex, int pc, int address, MemoryType type, OperandMode mode,
			int labelAccess) {
		if (type == MemoryType.SYMBOL) {
			return false;
		}
		// TODO: Why not check system equates, too?
		if (workspace.getUserEquateList().findEquateByAddress(address, labelAccess, true) != null) {
			return false;
		}
		if (getFixup(segmentIndex, type, pc, address) != null) {
			return false;
		}
		if (mode != OperandMode.Relative) {
			Segment segment = segmentList.get(segmentIndex);
			if (workspace.getSystemEquateList().findEquateByAddress(address, labelAccess, segment.isSDX()) != null) {
				return false;
			}
		} else {
			// For a relative branch, find in the same kind of segment first.
			int otherSegmentIndex = findSegmentWithSameFileHeaderByAddr(segmentIndex, address);
			if (otherSegmentIndex != NO_SEGMENT_INDEX) {
				allocateSegmentAddress(otherSegmentIndex, address);
				return true;
			}
		}
		int otherSegmentIndex = findSegmentByFixedAddr(segmentIndex, address);
		if (otherSegmentIndex == NO_SEGMENT_INDEX) {
			globalSegment.getAddressLabels().allocateAddressLabel(address);
		} else {
			allocateSegmentAddress(otherSegmentIndex, address);
		}
		return true;
	}

	private void allocateSegmentAddress(int segmentIndex, int address) {
		getSegment(segmentIndex).getAddressLabels().allocateAddressLabel(address);
	}

	public void freeAllAddresses() {
		for (Segment segment : segmentList) {
			segment.getAddressLabels().clear();
		}
		globalSegment.getAddressLabels().clear();
	}

	public void alignRamBlkLabelAddresses() {
		for (Segment segment : segmentList) {
			segment.alignRamBlkAddresses();
		}
	}

	/**
	 * @param defined single-element out parameter (index 0), set to whether a label was found/defined.
	 */
	public String defineLabelAtAddress(int segmentIndex, int address, boolean[] defined) {
		defined[0] = false;
		Equate equate = workspace.getUserEquateList().findAndMarkEquateByAddress(address, LabelAccess.READ);
		if (equate != null) {
			defined[0] = true;
			return equate.getLabel();
		}

		Segment segment = segmentList.get(segmentIndex);
		// TODO: This is redundant code for addresses/address labels.
		AddressLabel addressLabel = segment.getFixupAddressLabels().findNearestAddressLabel(address);
		boolean found = false;
		if (addressLabel != null) {
			address = addressLabel.isAligned() ? addressLabel.getAddress() : addressLabel.getNearestAddress();
			equate = workspace.getUserEquateList().findAndMarkEquateByAddress(address, LabelAccess.READ);
			if (equate != null) {
				defined[0] = true;
				return equate.getLabel();
			}
			found = true;
		} else {
			AddressLabel nearest = segment.getAddressLabels().findNearestAddressLabel(address);
			if (nearest != null) {
				address = nearest.isAligned() ? nearest.getAddress() : nearest.getNearestAddress();
				equate = workspace.getUserEquateList().findAndMarkEquateByAddress(address, LabelAccess.READ);
				if (equate != null) {
					return equate.getLabel();
				}
				found = true;
			}
		}
		if (found) {
			String label = (segment.labelPrefix.isEmpty() && segment.isSDX())
					? Segment.formatDefaultLabel(segmentIndex, address)
					: Segment.formatLabel(segment.labelPrefix, address);
			defined[0] = true;
			return label;
		}
		return "";
	}

	private String buildAddress(int segmentIndex, int address, int labelAccess, boolean noNearest) {
		Segment segment = segmentList.get(segmentIndex);
		AddressLabel addressLabel = segment.getAddressLabels().findAddressLabel(address);
		if (addressLabel != null) {
			int nearestAddr = addressLabel.getNearestAddress();
			if (addressLabel.isAligned() || address == nearestAddr || nearestAddr == 0 || noNearest) {
				address = addressLabel.getAddress();
				if (segment.isSDX()) {
					return Segment.formatDefaultLabel(segmentIndex, address);
				}
				return "L" + HexUtility.getLongValueHexString(address, 4);
			} else {
				address = addressLabel.getAddress();
				Equate equate = workspace.getUserEquateList().findEquateByAddress(nearestAddr, labelAccess, true);
				if (equate != null) {
					equate.addLabelReference(labelAccess);
					return equate.getLabel() + "+" + (address - nearestAddr);
				}
				if (segment.isSDX()) {
					return Segment.formatDefaultLabelWithOffset(segmentIndex, nearestAddr, address - nearestAddr);
				}
				return "L" + HexUtility.getLongValueHexString(nearestAddr, 4) + "+" + (address - nearestAddr);
			}
		}
		addressLabel = globalSegment.getAddressLabels().findAddressLabel(address);
		if (addressLabel != null) {
			return "L" + HexUtility.getLongValueHexString(address, 4);
		}
		return "";
	}

	public String getLabelAtAddress(int segmentIndex, int pc, int address, MemoryType type, int opcode) {
		Segment segment = segmentList.get(segmentIndex);
		InstructionSet instructionSet = workspace.getInstructionSet(segment.processorType);
		Instruction instruction = instructionSet.getInstruction(opcode);
		return getLabelAtAddress(segmentIndex, pc, address, type, instruction.getOperandMode(),
				instruction.getLabelAccess());
	}

	public String getLabelAtAddress(int segmentIndex, int pc, int address, MemoryType type, OperandMode mode,
			int labelAccess) {
		// Delegation method to place central breakpoints, kept to mirror the C++ structure.
		return getLabelAtAddressInternal(segmentIndex, pc, address, type, mode, labelAccess);
	}

	private String getLabelAtAddressInternal(int segmentIndex, int pc, int address, MemoryType type,
			OperandMode mode, int labelAccess) {
		Equate equate = workspace.getUserEquateList().findEquateByAddress(address, labelAccess, true);
		if (equate != null) {
			equate.addLabelReference(labelAccess);
			return equate.getLabel();
		}

		boolean sdx = getSegment(segmentIndex).isSDX();
		if (mode == OperandMode.ZeroPageX || mode == OperandMode.ZeroPageY || mode == OperandMode.ZeroPage
				|| mode == OperandMode.IndexedIndirect || mode == OperandMode.IndirectIndexed) {
			equate = workspace.getSystemEquateList().findEquateByAddress(address, labelAccess, sdx);
			if (equate != null) {
				return equate.getLabel();
			}
			return buildAddress(segmentIndex, address, labelAccess, true);
		}

		Fixup fixup = getFixup(segmentIndex, type, pc, address);
		if (fixup != null) {
			segmentIndex = fixup.getLabelSegmentIndex();
			Segment segment = segmentList.get(segmentIndex);
			AddressLabel addressLabel = segment.getFixupAddressLabels().findAddressLabel(address);
			if (addressLabel != null) {
				int nearestAddr = addressLabel.getNearestAddress();
				if (addressLabel.isAligned() || address == nearestAddr) {
					address = addressLabel.getAddress();
					return Segment.formatDefaultLabel(segmentIndex, address);
				}
				address = addressLabel.getAddress();
				equate = workspace.getUserEquateList().findEquateByAddress(nearestAddr, labelAccess, true);
				int offset = address - nearestAddr;
				if (equate != null) {
					equate.addLabelReference(labelAccess);
					return equate.getLabel() + "+" + offset;
				}
				return Segment.formatDefaultLabelWithOffset(segmentIndex, nearestAddr, offset);
			}
			return "";
		}

		if (mode == OperandMode.Relative) {
			// For a relative branch, find in the same kind of segment first.
			int otherSegmentIndex = findSegmentWithSameFileHeaderByAddr(segmentIndex, address);
			if (otherSegmentIndex != NO_SEGMENT_INDEX) {
				String label = buildAddress(otherSegmentIndex, address, labelAccess, false);
				if (!label.isEmpty()) {
					return label;
				}
			}
		}

		int otherSegmentIndex = findSegmentByFixedAddr(segmentIndex, address);
		if (otherSegmentIndex != NO_SEGMENT_INDEX) {
			segmentIndex = otherSegmentIndex;
		}
		String label = buildAddress(segmentIndex, address, labelAccess, false);
		if (!label.isEmpty()) {
			return label;
		}
		equate = workspace.getSystemEquateList().findEquateByAddress(address, labelAccess, sdx);
		if (equate != null) {
			return equate.getLabel();
		}
		return "";
	}

	public String getUserComment(int segmentIndex, int offset, int size) {
		StringBuilder result = new StringBuilder();
		Segment segment = segmentList.get(segmentIndex);

		if (size != 0xFFFF) {
			if (size == 0) {
				size = 1;
			}
			// TODO: Is this correct?
			for (int relativeOffset = 0; relativeOffset < size; relativeOffset++) {
				String comment = segment.findComment(offset + relativeOffset);
				if (!comment.isEmpty()) {
					if (result.length() == 0) {
						result.append(comment);
					} else {
						result.append("\n").append(comment);
					}
				}
			}
		}
		return result.toString();
	}

	public void setUserComment(int segmentIndex, int offset, int size, String text) {
		if (size != 0xFFFF) {
			Segment segment = segmentList.get(segmentIndex);
			segment.deleteComments(offset, size);
			if (!text.isEmpty()) {
				Comment comment = segment.allocateComment();
				comment.setOffset(offset);
				comment.setText(text);
			}
		}
	}

	public void addListener(SegmentListChangedListener listener) {
		listeners.add(Objects.requireNonNull(listener));
	}

	public void removeListeners() {
		listeners.clear();
	}

	public void beginUpdate() {
		updateCounter++;
	}

	public void endUpdate() {
		assert updateCounter > 0;
		updateCounter--;
		if (updateCounter == 0) {
			flushEvents();
		}
	}

	public void notifySegmentContentChanged() {
		notifyListeners(Property.SEGMENT_CONTENT);
	}

	void notifyListeners(Property property) {
		Log.logInfo("SegmentList.notifyListeners: Property {0}", new Object[] { property });

		// Add each event only once.
		if (!propertyChangeEvents.contains(property)) {
			propertyChangeEvents.add(property);
			if (updateCounter == 0) {
				flushEvents();
			}
		}
	}

	private void flushEvents() {
		if (propertyChangeEvents.isEmpty()) {
			return;
		}

		StringBuilder text = new StringBuilder();
		for (Property property : propertyChangeEvents) {
			text.append(property).append(' ');
		}
		Log.logInfo("SegmentList.flushEvents: Properties {0}", new Object[] { text });

		List<Property> events = new ArrayList<>(propertyChangeEvents);
		for (SegmentListChangedListener listener : listeners) {
			listener.handleSegmentListChanged(this, events);
		}
		propertyChangeEvents.clear();
	}
}
