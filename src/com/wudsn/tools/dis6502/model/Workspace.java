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

import com.wudsn.tools.base.common.Log;

/**
 * A workspace: the currently loaded disassembly project.
 * <p>
 * Ported from Workspace.h / Workspace.cpp. Unlike the C++ version, which
 * privately inherits from {@code EquateListChangedListener}/{@code
 * SegmentListChangedListener} to keep {@code HandleEquateListChanged}/{@code
 * HandleSegmentListChanged} out of its public interface, Java interface
 * methods are always public - there is no private-inheritance equivalent.
 * {@code Load1X}/{@code Save1X} and {@code Load}/{@code Save} of a whole
 * workspace file (the {@code Format} enum and {@code WorkspaceLogic}) are
 * not ported yet, since they depend on application-level file I/O and
 * logging.
 * <p>
 * {@link #isMemoryInspectorEditMode()} and the fields/methods around it are
 * transient, UI-adjacent state that is not serialized and does not fire a
 * {@link WorkspaceProperty} change event, the same as {@link
 * #isViewDisplayAsScreenCode()}/{@link #isViewNoDisassembly()}/{@link
 * #isViewDoubleHeight()} - but unlike those simple flags, edit mode carries
 * real navigation and byte-writing logic ({@link
 * #moveMemoryInspectorEditCursor}/{@link #typeMemoryInspectorEditChar}),
 * moved here (out of {@code com.wudsn.tools.dis6502.ui.MemoryInspectorPanel},
 * where the C++ source's equivalent state also lives, tied to its UI
 * control) so it can be exercised by a plain, headless unit test instead of
 * needing a real {@link java.awt.event.KeyEvent}/{@link
 * java.awt.event.MouseEvent}-driving Swing test. This is a deliberate
 * departure from the C++ source's own design, not a fidelity port - see
 * {@code MemoryInspectorPanel}'s class javadoc for the keyboard/focus/timer/
 * popup-menu wiring that still lives there and drives these methods.
 *
 * @author Peter Dell
 */
public final class Workspace implements Xml.Serializable, EquateListChangedListener, SegmentListChangedListener {

	private final ComputerSystemFactory computerSystemFactory;

	private String filePath = "";

	private boolean viewDisplayAsScreenCode; // MemoryInspector uses internal character set (ANTIC).
	private boolean viewNoDisassembly; // No disassembly launched if byte type is changed.
	private boolean viewDoubleHeight; // Double the font height of the display.

	/** Matches {@code MemoryInspectorGridPanel.BYTES_PER_LINE} - kept as its own constant since this class (model) must not depend on that UI class. */
	private static final int MEMORY_INSPECTOR_EDIT_BYTES_PER_LINE = 16;

	private boolean memoryInspectorEditMode;
	private int memoryInspectorEditCursorOffset = -1;
	private MemoryInspectorEditPane memoryInspectorEditCursorPane = MemoryInspectorEditPane.HEX_HIGH;

	private ComputerSystem computerSystem;

	private final InstructionSet instructionSetMOS6502 = new InstructionSetMOS6502("MOS 6502");
	private final InstructionSet instructionSetMOS65C02 = new InstructionSetMOS65C02("MOS 65C02");

	private final EquateList systemEquateList = new EquateList(WorkspaceProperty.SYSTEM_EQUATES);
	private final EquateList userEquateList = new EquateList(WorkspaceProperty.USER_EQUATES);

	private final SegmentList segmentList = new SegmentList(this);
	private final Profile profile = new Profile();
	private final DisassemblyResult disassemblyResult = new DisassemblyResult();

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

		memoryInspectorEditMode = false;
		memoryInspectorEditCursorOffset = -1;
		memoryInspectorEditCursorPane = MemoryInspectorEditPane.HEX_HIGH;
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

	public boolean isMemoryInspectorEditMode() {
		return memoryInspectorEditMode;
	}

	public int getMemoryInspectorEditCursorOffset() {
		return memoryInspectorEditCursorOffset;
	}

	public MemoryInspectorEditPane getMemoryInspectorEditCursorPane() {
		return memoryInspectorEditCursorPane;
	}

	/**
	 * Ported from {@code MemoryInspector::SetEditMode(true)}/{@code
	 * MemoryInspectorControlImpl::LButtonDblClk}'s cursor positioning - a
	 * workspace-wide lock, not tied to any one segment or selection object:
	 * while active, every other command that would mutate the selected
	 * segment (or any other one) is expected to stay blocked, matching {@code
	 * MainMemoryInspector::PerformCommands}'s modal gate in the C++ source.
	 * {@code offset} is a byte offset into the currently selected segment
	 * ({@link SegmentList#getSelectedIndex()}) - this method does not decide
	 * which offset to start at (a fresh selection's first byte, or an exact
	 * double-clicked position); that stays the caller's job. Returns
	 * {@code false} (leaving the workspace unchanged) if there is no selected
	 * segment or {@code offset} is out of range for it.
	 */
	public boolean enterMemoryInspectorEditMode(int offset, MemoryInspectorEditPane pane) {
		Segment segment = getMemoryInspectorEditSegment();
		if (segment == null || offset < 0 || offset >= segment.getSize()) {
			return false;
		}
		memoryInspectorEditMode = true;
		memoryInspectorEditCursorOffset = offset;
		memoryInspectorEditCursorPane = pane;
		return true;
	}

	/**
	 * Ported from {@code MainController::QuitEditMode}: releases the lock
	 * {@link #enterMemoryInspectorEditMode} takes - a no-op if edit mode was
	 * not active. Unlike {@code
	 * com.wudsn.tools.dis6502.ui.MemoryInspectorPanel#quitEditMode}, this only
	 * clears model state; resyncing the UI selection and re-running the
	 * disassembly are that method's job, not this one's.
	 */
	public void quitMemoryInspectorEditMode() {
		memoryInspectorEditMode = false;
		memoryInspectorEditCursorOffset = -1;
		memoryInspectorEditCursorPane = MemoryInspectorEditPane.HEX_HIGH;
	}

	/**
	 * Ported from {@code MemoryInspectorControlImpl::KeyDown}'s edit-mode
	 * branch: pure cursor navigation, no data change. The hex panes ({@code
	 * HEX_HIGH}/{@code HEX_LOW}) move nibble-wise on {@code LEFT}/{@code
	 * RIGHT} (crossing to the adjacent byte's far nibble at a boundary) and a
	 * whole line on {@code UP}/{@code DOWN}, resetting to the high nibble; the
	 * ASCII pane moves byte-wise for all six movements, with no nibble
	 * concept. Returns {@code false} (a no-op) if edit mode is not active.
	 */
	public boolean moveMemoryInspectorEditCursor(MemoryInspectorEditCursorMovement movement) {
		if (!memoryInspectorEditMode) {
			return false;
		}
		Segment segment = getMemoryInspectorEditSegment();
		if (segment == null) {
			return false;
		}
		int size = segment.getSize();
		int offset = memoryInspectorEditCursorOffset;
		MemoryInspectorEditPane pane = memoryInspectorEditCursorPane;

		int newOffset = offset;
		MemoryInspectorEditPane newPane = pane;
		switch (movement) {
		case HOME:
			newOffset = 0;
			newPane = pane != MemoryInspectorEditPane.ASCII ? MemoryInspectorEditPane.HEX_HIGH : pane;
			break;
		case END:
			newOffset = size - 1;
			newPane = pane != MemoryInspectorEditPane.ASCII ? MemoryInspectorEditPane.HEX_HIGH : pane;
			break;
		case UP:
			if (offset - MEMORY_INSPECTOR_EDIT_BYTES_PER_LINE >= 0) {
				newOffset = offset - MEMORY_INSPECTOR_EDIT_BYTES_PER_LINE;
				newPane = pane != MemoryInspectorEditPane.ASCII ? MemoryInspectorEditPane.HEX_HIGH : pane;
			}
			break;
		case DOWN:
			if (offset + MEMORY_INSPECTOR_EDIT_BYTES_PER_LINE < size) {
				newOffset = offset + MEMORY_INSPECTOR_EDIT_BYTES_PER_LINE;
				newPane = pane != MemoryInspectorEditPane.ASCII ? MemoryInspectorEditPane.HEX_HIGH : pane;
			}
			break;
		case LEFT:
			if (pane == MemoryInspectorEditPane.ASCII) {
				if (offset > 0) {
					newOffset = offset - 1;
				}
			} else if (pane == MemoryInspectorEditPane.HEX_LOW) {
				newPane = MemoryInspectorEditPane.HEX_HIGH;
			} else if (offset > 0) {
				newOffset = offset - 1;
				newPane = MemoryInspectorEditPane.HEX_LOW;
			}
			break;
		case RIGHT:
			if (pane == MemoryInspectorEditPane.ASCII) {
				if (offset + 1 < size) {
					newOffset = offset + 1;
				}
			} else if (pane == MemoryInspectorEditPane.HEX_HIGH) {
				newPane = MemoryInspectorEditPane.HEX_LOW;
			} else if (offset + 1 < size) {
				newOffset = offset + 1;
				newPane = MemoryInspectorEditPane.HEX_HIGH;
			}
			break;
		}

		memoryInspectorEditCursorOffset = newOffset;
		memoryInspectorEditCursorPane = newPane;
		return true;
	}

	/**
	 * Ported from {@code MemoryInspectorControlImpl::Char}: hex-digit/ASCII
	 * data entry, plus Tab to switch panes. Every valid keystroke writes
	 * immediately via {@link Segment#setData} - there is no staging buffer or
	 * undo, matching the C++ source. Returns {@link
	 * MemoryInspectorEditCharResult#NOT_HANDLED} (a no-op) if edit mode is not
	 * active.
	 */
	public MemoryInspectorEditCharResult typeMemoryInspectorEditChar(char c) {
		if (!memoryInspectorEditMode) {
			return MemoryInspectorEditCharResult.NOT_HANDLED;
		}
		Segment segment = getMemoryInspectorEditSegment();
		int offset = memoryInspectorEditCursorOffset;
		if (segment == null || offset < 0 || offset >= segment.getSize()) {
			return MemoryInspectorEditCharResult.NOT_HANDLED;
		}
		MemoryInspectorEditPane pane = memoryInspectorEditCursorPane;

		if (pane != MemoryInspectorEditPane.ASCII) {
			if (c == '\t') {
				memoryInspectorEditCursorPane = MemoryInspectorEditPane.ASCII;
				return MemoryInspectorEditCharResult.HANDLED;
			}
			int nibble = Character.digit(c, 16);
			if (nibble < 0) {
				return MemoryInspectorEditCharResult.NOT_HANDLED;
			}
			int oldValue = segment.getData(offset) & 0xFF;
			int updated = pane == MemoryInspectorEditPane.HEX_HIGH ? (oldValue & 0x0F) | (nibble << 4) : (oldValue & 0xF0) | nibble;
			segment.setData(offset, updated);
			if (pane == MemoryInspectorEditPane.HEX_HIGH) {
				memoryInspectorEditCursorPane = MemoryInspectorEditPane.HEX_LOW;
				return MemoryInspectorEditCharResult.HANDLED;
			} else if (offset + 1 < segment.getSize()) {
				memoryInspectorEditCursorOffset = offset + 1;
				memoryInspectorEditCursorPane = MemoryInspectorEditPane.HEX_HIGH;
				return MemoryInspectorEditCharResult.HANDLED;
			} else {
				return MemoryInspectorEditCharResult.HANDLED_AT_BUFFER_END;
			}
		} else {
			if (c == '\t') {
				memoryInspectorEditCursorPane = MemoryInspectorEditPane.HEX_HIGH;
				return MemoryInspectorEditCharResult.HANDLED;
			}
			int toWrite;
			if (c == '\r' || c == '\n') {
				toWrite = 0x9B; // Atari end-of-line byte, written like any other typed character.
			} else if (c >= ' ' && c < 128) {
				// Deliberately NOT replicating MemoryInspectorControlImpl.cpp Char()'s
				// exclusion of '~', '{', '}' from the printable range - see
				// com.wudsn.tools.dis6502.ui.MemoryInspectorPanel's class javadoc.
				toWrite = c;
				if (segment.isType(offset, MemoryType.SBYTE)) {
					toWrite = MemoryType.toSbyteInternalCode(toWrite);
				}
			} else {
				return MemoryInspectorEditCharResult.NOT_HANDLED;
			}
			segment.setData(offset, toWrite);
			if (offset + 1 < segment.getSize()) {
				memoryInspectorEditCursorOffset = offset + 1;
				memoryInspectorEditCursorPane = MemoryInspectorEditPane.ASCII;
				return MemoryInspectorEditCharResult.HANDLED;
			} else {
				return MemoryInspectorEditCharResult.HANDLED_AT_BUFFER_END;
			}
		}
	}

	private Segment getMemoryInspectorEditSegment() {
		int index = segmentList.getSelectedIndex();
		return index == SegmentList.NO_SEGMENT_INDEX ? null : segmentList.getSegment(index);
	}

	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
		notifyListeners(WorkspaceProperty.FILE_PATH);
	}

	public void setComputerSystemTypeID(String id) {
		ComputerSystemType computerSystemType = computerSystemFactory.getComputerSystemType(id);
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
		switch (processorType) {
		case MOS6502:
			return instructionSetMOS6502;
		case MOS65C02:
			return instructionSetMOS65C02;
		default:
			throw new IllegalArgumentException("Invalid processor type: " + processorType + ".");
		}
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
		Log.logInfo("Workspace.notifyListeners: Property {0}", new Object[] { property });

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
		Log.logInfo("Workspace.flushEvents: Properties {0}", new Object[] { text });

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
		Xml.setStringAttribute(element, "ComputerSystemTypeID", computerSystem.getTypeInfo().id);

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
