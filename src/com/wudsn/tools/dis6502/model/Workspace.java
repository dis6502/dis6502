/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.w3c.dom.Element;

import com.wudsn.tools.base.common.Log;

/**
 * A workspace: the currently loaded disassembly project.
 * <p>
 * Ported from Workspace.h / Workspace.cpp. Unlike the C++ version, which
 * privately inherits from {@code EquateListChangedListener}/{@code
 * SegmentListChangedListener} to keep {@code HandleEquateListChanged}/{@code
 * HandleSegmentListChanged} out of its public interface, Java interface
 * methods are always public - there is no private-inheritance equivalent.
 * Loading and saving a whole workspace file is not done here but by {@link
 * WorkspaceLogic} (and {@link Workspace1X} for the legacy binary format),
 * since it depends on application-level file I/O and logging.
 * <p>
 * {@link #getMemoryInspectorState()} is this workspace's single, owned
 * {@link MutableMemoryInspectorState} instance (constructed once, alongside {@link
 * #getSegmentList()}/{@link #getProfile()}) - not a satellite object built
 * and passed around by callers, even though {@code
 * com.wudsn.tools.dis6502.ui.MemoryInspectorPanel} still receives it as a
 * parameter (see that class's own javadoc). See {@link
 * MutableMemoryInspectorState}'s own javadoc for why its edit-mode half in
 * particular lives there and not as flat fields/methods on this class
 * directly.
 *
 * @author Peter Dell
 */
public final class Workspace implements Xml.Serializable, EquateListChangedListener, SegmentListChangedListener {

	private final ComputerSystemFactory computerSystemFactory;

	private String filePath = "";

	private boolean viewDisplayAsScreenCode; // MemoryInspector uses internal character set (ANTIC).
	private boolean viewNoDisassembly; // No disassembly launched if byte type is changed.
	private boolean viewDoubleHeight; // Double the font height of the display.

	private ComputerSystem computerSystem;

	private final InstructionSet instructionSetMOS6502 = new InstructionSetMOS6502();
	private final InstructionSet instructionSetMOS65C02 = new InstructionSetMOS65C02();

	private final EquateList systemEquateList = new EquateList(WorkspaceProperty.SYSTEM_EQUATES);
	private final EquateList userEquateList = new EquateList(WorkspaceProperty.USER_EQUATES);

	private final SegmentList segmentList = new SegmentList(this);
	private final Profile profile = new Profile();
	private final DisassemblyResult disassemblyResult = new DisassemblyResult();
	private final MutableMemoryInspectorState memoryInspectorState = new MutableMemoryInspectorState(this);

	// Event handling.
	private int updateCounter;
	private final List<WorkspaceProperty> propertyChangeEvents = new ArrayList<>();
	private final List<WorkspaceChangedListener> listeners = new ArrayList<>();

	public Workspace(ComputerSystemFactory computerSystemFactory) {
		this.computerSystemFactory = computerSystemFactory;
		this.computerSystem = computerSystemFactory.getComputerSystem(ComputerSystemType.UNKNOWN);

		systemEquateList.addListener(this);
		userEquateList.addListener(this);
		segmentList.addListener(this);

		init();
	}

	public void init() {
		beginUpdate();
		setFilePath("");
		segmentList.clear();
		systemEquateList.clear();
		userEquateList.clear();
		profile.clear();

		viewDisplayAsScreenCode = false;
		viewNoDisassembly = false;
		viewDoubleHeight = true;

		memoryInspectorState.clear();
		endUpdate();
	}

	public ComputerSystemFactory getComputerSystemFactory() {
		return computerSystemFactory;
	}

	public boolean isViewDisplayAsScreenCode() {
		return viewDisplayAsScreenCode;
	}

	public void setViewDisplayAsScreenCode(boolean value) {
		viewDisplayAsScreenCode = value;
	}

	public boolean isViewNoDisassembly() {
		return viewNoDisassembly;
	}

	public void setViewNoDisassembly(boolean value) {
		viewNoDisassembly = value;
	}

	public boolean isViewDoubleHeight() {
		return viewDoubleHeight;
	}

	public void setViewDoubleHeight(boolean value) {
		viewDoubleHeight = value;
	}

	public MutableMemoryInspectorState getMemoryInspectorState() {
		return memoryInspectorState;
	}

	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
		notifyListeners(WorkspaceProperty.FILE_PATH);
	}

	public void setComputerSystemTypeID(String id) {
		ComputerSystemType computerSystemType = ComputerSystemType.fromId(id);
		if (computerSystemType == ComputerSystemType.UNKNOWN) {
			computerSystemType = ComputerSystemType.ATARI800;
		}
		setComputerSystemType(computerSystemType);
	}

	public void setComputerSystemType(ComputerSystemType computerSystemType) {
		if (computerSystem == null || computerSystem.getType() != computerSystemType) {
			computerSystem = computerSystemFactory.getComputerSystem(computerSystemType);
			notifyListeners(WorkspaceProperty.COMPUTER_SYSTEM_TYPE);
			notifyListeners(WorkspaceProperty.FONT);
		}
	}

	public ComputerSystem getComputerSystem() {
		return computerSystem;
	}

	public InstructionSet getInstructionSet(ProcessorType processorType) {
		// An if chain, not a switch: ProcessorType is a ValueSet, not an enum.
		if (processorType == ProcessorType.MOS6502) {
			return instructionSetMOS6502;
		} else if (processorType == ProcessorType.MOS65C02) {
			return instructionSetMOS65C02;
		}
		throw new IllegalArgumentException("Invalid processor type: " + processorType.getKey() + ".");
	}

	public Profile getProfile() {
		return profile;
	}

	public EquateList getSystemEquateList() {
		return systemEquateList;
	}

	public EquateList getUserEquateList() {
		return userEquateList;
	}

	/** Finds an equate in the user equate list and then the system equate list. Address 0 is never a valid label. */
	public Equate findEquateByAddress(int address, int labelAccess) {
		if (address == 0) {
			return null;
		}
		Equate equate = userEquateList.findEquateByAddress(address, labelAccess, true);
		if (equate == null) {
			equate = systemEquateList.findEquateByAddress(address, labelAccess, true);
		}
		return equate;
	}

	public Equate getEquateByLabel(String label) {
		Equate equate = userEquateList.getEquateByLabel(label);
		if (equate == null) {
			equate = systemEquateList.getEquateByLabel(label);
		}
		return equate;
	}

	/** Clears the transient definition/reference flags on all equates. Does not fire a "changed" event. */
	public void clearEquateFlags() {
		systemEquateList.clearFlags();
		userEquateList.clearFlags();
	}

	/** Finds the SDX symbol for a PC address, or {@code null} if there is none. */
	public String findSymbolByAddress(int segmentIndex, int address, int addressOffset) {
		Symbol symbol = segmentList.getSegment(segmentIndex).findSymbol(address);
		if (symbol != null) {
			return symbol.getSymbol() + Memory.addressOffsetToString(addressOffset);
		}
		return null;
	}

	public SegmentList getSegmentList() {
		return segmentList;
	}

	public Segment getSegment(int segmentIndex) {
		return segmentList.getSegment(segmentIndex);
	}

	public void notifyFontChanged() {
		notifyListeners(WorkspaceProperty.FONT);
	}

	public void notifyProfileChanged() {
		notifyListeners(WorkspaceProperty.PROFILE);
	}

	public void notifySelectedMemoryRangeChanged() {
		notifyListeners(WorkspaceProperty.SELECTED_MEMORY_RANGE);
	}

	public void addListener(WorkspaceChangedListener listener) {
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

	private void notifyListeners(WorkspaceProperty property) {
		// Log.logInfo("Workspace.notifyListeners: Property {0}", new Object[] { property });

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
		for (WorkspaceProperty property : propertyChangeEvents) {
			text.append(property).append(' ');
		}
		// Log.logInfo("Workspace.flushEvents: Properties {0}", new Object[] { text });

		List<WorkspaceProperty> events = new ArrayList<>(propertyChangeEvents);
		propertyChangeEvents.clear();
		for (WorkspaceChangedListener listener : listeners) {
			listener.handleWorkspaceChanged(this, events);
		}
	}

	@Override
	public void handleEquateListChanged(EquateList equateList, WorkspaceProperty workspaceProperty) {
		notifyListeners(workspaceProperty);
	}

	@Override
	public void handleSegmentListChanged(SegmentList segmentList, List<SegmentList.Property> propertyChangeEvents) {
		beginUpdate();
		for (SegmentList.Property property : propertyChangeEvents) {
			switch (property) {
			case SEGMENTS:
			case SEGMENT_CONTENT:
				notifyListeners(WorkspaceProperty.SEGMENTS);
				break;
			case SELECTED_INDEX:
				notifyListeners(WorkspaceProperty.SELECTED_SEGMENT);
				break;
			}
		}
		endUpdate();
	}

	@Override
	public void serializeTo(Element element) {
		Xml.setStringAttribute(element, "ComputerSystemTypeID", computerSystem.getType().getId());

		Element profileElement = Xml.addChildElement(element, "Profile");
		profile.serializeTo(profileElement);

		Element systemEquatesElement = Xml.addChildElement(element, "SystemEquates");
		systemEquateList.serializeTo(systemEquatesElement);

		Element userEquatesElement = Xml.addChildElement(element, "UserEquates");
		userEquateList.serializeTo(userEquatesElement);

		Element segmentsElement = Xml.addChildElement(element, "Segments");
		segmentList.serializeTo(segmentsElement);
	}

	@Override
	public void deserializeFrom(Element element) {
		init();

		String computerSystemTypeID = Xml.getStringAttribute(element, "ComputerSystemTypeID", "");
		setComputerSystemTypeID(computerSystemTypeID);

		Element profileElement = Xml.getFirstChildElement(element, "Profile");
		if (profileElement != null) {
			profile.deserializeFrom(profileElement);
			notifyProfileChanged();
		}

		Element systemEquatesElement = Xml.getFirstChildElement(element, "SystemEquates");
		if (systemEquatesElement != null) {
			systemEquateList.deserializeFrom(systemEquatesElement); // This will notify listeners.
		}

		Element userEquatesElement = Xml.getFirstChildElement(element, "UserEquates");
		if (userEquatesElement != null) {
			userEquateList.deserializeFrom(userEquatesElement); // This will notify listeners.
		}

		Element segmentsElement = Xml.getFirstChildElement(element, "Segments");
		if (segmentsElement != null) {
			segmentList.deserializeFrom(segmentsElement);
		}
	}

	public DisassemblyResult getDisassemblyResult() {
		return disassemblyResult;
	}
}
