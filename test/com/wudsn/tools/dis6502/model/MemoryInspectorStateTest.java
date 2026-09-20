/*
 * Copyright (C) 2025 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

import com.wudsn.tools.dis6502.model.MemoryInspectorState.EditCharResult;
import com.wudsn.tools.dis6502.model.MemoryInspectorState.EditCursorMovement;
import com.wudsn.tools.dis6502.model.MemoryInspectorState.EditPane;

/**
 * Headless coverage for {@link MutableMemoryInspectorState}'s edit-mode cursor
 * navigation and byte-writing logic - moved here (out of {@code
 * com.wudsn.tools.dis6502.ui.MemoryInspectorPanel}) so it can be exercised
 * without any Swing dependency, see that class's (and {@link
 * MutableMemoryInspectorState}'s) javadoc for why. The mouse/keyboard/popup-menu
 * wiring that drives these methods in the real UI is covered separately by
 * ad hoc Robot-driven smoke tests instead, since this project's hand-rolled
 * test harness cannot drive real {@link java.awt.event.KeyEvent}/{@link
 * java.awt.event.MouseEvent}s (see {@code TestRunner}'s javadoc) - every one
 * of these tests is a plain method call.
 *
 * @author Peter Dell
 */
public final class MemoryInspectorStateTest {

	private MemoryInspectorStateTest() {
	}

	public static void testMemoryInspectorState() {
		testEnterQuit();
		testCursorNavigation();
		testHexDigitWrite();
		testAsciiWrite();
		testPrintableRangeIncludesTildeBraces();
		testNotHandledCases();

		Assert.log("MemoryInspectorStateTest completed");
	}

	private static MutableMemoryInspectorState newStateWithSelectedSegment(int size) {
		Workspace workspace = new Workspace(new ComputerSystemFactory());
		Segment segment = workspace.getSegmentList().insertSegmentAt(0);
		segment.wBegin = 0x2000;
		segment.wEnd = 0x2000 + size - 1;
		segment.createMemoryBlockFromBeginToEnd();
		workspace.getSegmentList().setSelectedIndex(0);
		MutableMemoryInspectorState state = workspace.getMemoryInspectorState();
		state.setSegmentIndex(0);
		return state;
	}

	private static void testEnterQuit() {
		MutableMemoryInspectorState state = new Workspace(new ComputerSystemFactory()).getMemoryInspectorState();
		Assert.boolEquals(state.enterEditMode(0, EditPane.HEX_HIGH), false); // No segment selected.
		Assert.boolEquals(state.isEditMode(), false);

		state = newStateWithSelectedSegment(4);
		Assert.boolEquals(state.enterEditMode(-1, EditPane.HEX_HIGH), false); // Offset out of range.
		Assert.boolEquals(state.enterEditMode(4, EditPane.HEX_HIGH), false); // Offset == size, out of range.

		Assert.boolEquals(state.enterEditMode(1, EditPane.HEX_LOW), true);
		Assert.boolEquals(state.isEditMode(), true);
		Assert.longEquals(state.getEditCursorOffset(), 1);
		Assert.boolEquals(state.getEditCursorPane() == EditPane.HEX_LOW, true);

		state.quitEditMode();
		Assert.boolEquals(state.isEditMode(), false);
		Assert.longEquals(state.getEditCursorOffset(), -1);

		// A second quit is a harmless no-op, matching MainController::QuitEditMode's oldEditMode check.
		state.quitEditMode();
		Assert.boolEquals(state.isEditMode(), false);

		// Every navigation/typing method is itself a no-op while not editing.
		Assert.boolEquals(state.moveEditCursor(EditCursorMovement.RIGHT), false);
		Assert.boolEquals(state.typeEditChar('5') == EditCharResult.NOT_HANDLED, true);
	}

	/** Ported from MemoryInspectorControlImpl::KeyDown's edit-mode branch. */
	private static void testCursorNavigation() {
		MutableMemoryInspectorState state = newStateWithSelectedSegment(0x21); // 33 bytes: two full 16-byte lines plus one.
		state.enterEditMode(0, EditPane.HEX_HIGH);

		// Right within a byte: high -> low nibble, same offset.
		state.moveEditCursor(EditCursorMovement.RIGHT);
		assertCursor(state, 0, EditPane.HEX_LOW);

		// Right from the low nibble crosses to the next byte's high nibble.
		state.moveEditCursor(EditCursorMovement.RIGHT);
		assertCursor(state, 1, EditPane.HEX_HIGH);

		// Left from the high nibble crosses back to the previous byte's low nibble.
		state.moveEditCursor(EditCursorMovement.LEFT);
		assertCursor(state, 0, EditPane.HEX_LOW);

		// Left at offset 0's low nibble goes back to its own high nibble, not offset -1.
		state.moveEditCursor(EditCursorMovement.LEFT);
		assertCursor(state, 0, EditPane.HEX_HIGH);
		state.moveEditCursor(EditCursorMovement.LEFT);
		assertCursor(state, 0, EditPane.HEX_HIGH); // No further effect.

		// Down/Up move a whole line (16 bytes), resetting to the high nibble.
		state.moveEditCursor(EditCursorMovement.DOWN);
		assertCursor(state, 16, EditPane.HEX_HIGH);
		state.moveEditCursor(EditCursorMovement.DOWN);
		assertCursor(state, 32, EditPane.HEX_HIGH); // Last valid offset (size 0x21 = 33).
		state.moveEditCursor(EditCursorMovement.DOWN);
		assertCursor(state, 32, EditPane.HEX_HIGH); // Would overshoot - no effect.
		state.moveEditCursor(EditCursorMovement.UP);
		assertCursor(state, 16, EditPane.HEX_HIGH);

		// Home/End jump to the segment's first/last offset.
		state.moveEditCursor(EditCursorMovement.END);
		assertCursor(state, 32, EditPane.HEX_HIGH);
		state.moveEditCursor(EditCursorMovement.HOME);
		assertCursor(state, 0, EditPane.HEX_HIGH);

		// The ASCII pane moves byte-wise for all six movements, no nibble concept.
		state.typeEditChar('\t'); // Switch to the ASCII pane.
		assertCursor(state, 0, EditPane.ASCII);
		state.moveEditCursor(EditCursorMovement.LEFT);
		assertCursor(state, 0, EditPane.ASCII); // No effect at offset 0.
		state.moveEditCursor(EditCursorMovement.RIGHT);
		assertCursor(state, 1, EditPane.ASCII);
		state.moveEditCursor(EditCursorMovement.DOWN);
		assertCursor(state, 17, EditPane.ASCII); // Stays on the ASCII pane, unlike the hex panes.
	}

	private static void assertCursor(MutableMemoryInspectorState state, int expectedOffset, EditPane expectedPane) {
		Assert.longEquals(state.getEditCursorOffset(), expectedOffset);
		Assert.boolEquals(state.getEditCursorPane() == expectedPane, true);
	}

	/** Ported from MemoryInspectorControlImpl::Char's hex-nibble write path. */
	private static void testHexDigitWrite() {
		MutableMemoryInspectorState state = newStateWithSelectedSegment(2);
		Segment segment = state.getSegment();
		segment.setData(0, 0x00);
		segment.setData(1, 0x00);
		state.enterEditMode(0, EditPane.HEX_HIGH);

		Assert.boolEquals(state.typeEditChar('A') == EditCharResult.HANDLED, true);
		Assert.longEquals(segment.getData(0) & 0xFF, 0xA0);
		assertCursor(state, 0, EditPane.HEX_LOW);

		Assert.boolEquals(state.typeEditChar('5') == EditCharResult.HANDLED, true);
		Assert.longEquals(segment.getData(0) & 0xFF, 0xA5);
		assertCursor(state, 1, EditPane.HEX_HIGH); // Advanced to the next byte.

		// Reaching the last byte's low nibble reports HANDLED_AT_BUFFER_END and does not advance further.
		state.typeEditChar('F');
		EditCharResult result = state.typeEditChar('F');
		Assert.boolEquals(result == EditCharResult.HANDLED_AT_BUFFER_END, true);
		Assert.longEquals(segment.getData(1) & 0xFF, 0xFF);
		assertCursor(state, 1, EditPane.HEX_LOW); // Left exactly where it was, matching the exit path's own contract.
	}

	/** Ported from MemoryInspectorControlImpl::Char's ASCII-pane write path, including the SBYTE transform. */
	private static void testAsciiWrite() {
		MutableMemoryInspectorState state = newStateWithSelectedSegment(3);
		Segment segment = state.getSegment();
		state.enterEditMode(0, EditPane.ASCII);

		Assert.boolEquals(state.typeEditChar('A') == EditCharResult.HANDLED, true);
		Assert.longEquals(segment.getData(0) & 0xFF, 'A'); // Not SBYTE-typed - written verbatim.
		assertCursor(state, 1, EditPane.ASCII);

		segment.setType(1, MemoryType.SBYTE);
		Assert.boolEquals(state.typeEditChar('A') == EditCharResult.HANDLED, true);
		Assert.longEquals(segment.getData(1) & 0xFF, MemoryType.toSbyteInternalCode('A')); // SBYTE-typed - transformed.
		assertCursor(state, 2, EditPane.ASCII);

		// Enter/Return writes the Atari end-of-line byte, at the segment's last offset, reporting HANDLED_AT_BUFFER_END.
		EditCharResult result = state.typeEditChar('\r');
		Assert.boolEquals(result == EditCharResult.HANDLED_AT_BUFFER_END, true);
		Assert.longEquals(segment.getData(2) & 0xFF, 0x9B);

		// Tab switches panes without touching data.
		state.enterEditMode(0, EditPane.ASCII);
		int before = segment.getData(0) & 0xFF;
		Assert.boolEquals(state.typeEditChar('\t') == EditCharResult.HANDLED, true);
		assertCursor(state, 0, EditPane.HEX_HIGH);
		Assert.longEquals(segment.getData(0) & 0xFF, before);
		Assert.boolEquals(state.typeEditChar('\t') == EditCharResult.HANDLED, true);
		assertCursor(state, 0, EditPane.ASCII); // Tab from ASCII always lands on hex-high, then back to ASCII here.
	}

	/**
	 * Deliberately NOT replicating MemoryInspectorControlImpl.cpp Char()'s
	 * exclusion of '~', '{', '}' from the printable range - see {@link
	 * MutableMemoryInspectorState}'s own javadoc.
	 */
	private static void testPrintableRangeIncludesTildeBraces() {
		MutableMemoryInspectorState state = newStateWithSelectedSegment(4); // One byte more than written, so every write below is a plain HANDLED, not HANDLED_AT_BUFFER_END.
		state.enterEditMode(0, EditPane.ASCII);
		Segment segment = state.getSegment();

		Assert.boolEquals(state.typeEditChar('~') == EditCharResult.HANDLED, true);
		Assert.longEquals(segment.getData(0) & 0xFF, '~');
		Assert.boolEquals(state.typeEditChar('{') == EditCharResult.HANDLED, true);
		Assert.longEquals(segment.getData(1) & 0xFF, '{');
		Assert.boolEquals(state.typeEditChar('}') == EditCharResult.HANDLED, true);
		Assert.longEquals(segment.getData(2) & 0xFF, '}');
	}

	private static void testNotHandledCases() {
		MutableMemoryInspectorState state = newStateWithSelectedSegment(2);
		state.enterEditMode(0, EditPane.HEX_HIGH);
		Assert.boolEquals(state.typeEditChar('G') == EditCharResult.NOT_HANDLED, true); // Not a hex digit.
		Assert.boolEquals(state.typeEditChar((char) 0) == EditCharResult.NOT_HANDLED, true);

		state.typeEditChar('\t'); // Switch to the ASCII pane.
		Assert.boolEquals(state.typeEditChar((char) 27) == EditCharResult.NOT_HANDLED, true); // Below ' '.
		Assert.boolEquals(state.typeEditChar((char) 128) == EditCharResult.NOT_HANDLED, true); // >= 128.
	}
}
