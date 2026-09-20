/*
 * Copyright (C) 2025 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

/**
 * Headless coverage for the Memory Inspector edit mode's cursor navigation
 * and byte-writing logic, now that it lives on {@link Workspace} instead of
 * {@code com.wudsn.tools.dis6502.ui.MemoryInspectorPanel} - see that class's
 * (and {@link Workspace}'s) javadoc for why. Unlike {@code
 * MemoryInspectorGridPanelTest} (which still needs ad hoc Robot-driven smoke
 * tests for the mouse/keyboard/popup-menu wiring, since this project's
 * hand-rolled test harness cannot drive real {@link
 * java.awt.event.KeyEvent}/{@link java.awt.event.MouseEvent}s), every one of
 * these tests is a plain method call with no Swing dependency at all.
 *
 * @author Peter Dell
 */
public final class WorkspaceEditModeTest {

	private WorkspaceEditModeTest() {
	}

	public static void testWorkspaceEditMode() {
		testEnterQuit();
		testCursorNavigation();
		testHexDigitWrite();
		testAsciiWrite();
		testPrintableRangeIncludesTildeBraces();
		testNotHandledCases();

		Assert.log("WorkspaceEditModeTest completed");
	}

	private static Workspace newWorkspaceWithSelectedSegment(int size) {
		Workspace workspace = new Workspace(new ComputerSystemFactory());
		Segment segment = workspace.getSegmentList().insertSegmentAt(0);
		segment.wBegin = 0x2000;
		segment.wEnd = 0x2000 + size - 1;
		segment.createMemoryBlockFromBeginToEnd();
		workspace.getSegmentList().setSelectedIndex(0);
		return workspace;
	}

	private static void testEnterQuit() {
		Workspace workspace = new Workspace(new ComputerSystemFactory());
		Assert.boolEquals(workspace.enterMemoryInspectorEditMode(0, MemoryInspectorEditPane.HEX_HIGH), false); // No segment selected.
		Assert.boolEquals(workspace.isMemoryInspectorEditMode(), false);

		workspace = newWorkspaceWithSelectedSegment(4);
		Assert.boolEquals(workspace.enterMemoryInspectorEditMode(-1, MemoryInspectorEditPane.HEX_HIGH), false); // Offset out of range.
		Assert.boolEquals(workspace.enterMemoryInspectorEditMode(4, MemoryInspectorEditPane.HEX_HIGH), false); // Offset == size, out of range.

		Assert.boolEquals(workspace.enterMemoryInspectorEditMode(1, MemoryInspectorEditPane.HEX_LOW), true);
		Assert.boolEquals(workspace.isMemoryInspectorEditMode(), true);
		Assert.longEquals(workspace.getMemoryInspectorEditCursorOffset(), 1);
		Assert.boolEquals(workspace.getMemoryInspectorEditCursorPane() == MemoryInspectorEditPane.HEX_LOW, true);

		workspace.quitMemoryInspectorEditMode();
		Assert.boolEquals(workspace.isMemoryInspectorEditMode(), false);
		Assert.longEquals(workspace.getMemoryInspectorEditCursorOffset(), -1);

		// A second quit is a harmless no-op, matching MainController::QuitEditMode's oldEditMode check.
		workspace.quitMemoryInspectorEditMode();
		Assert.boolEquals(workspace.isMemoryInspectorEditMode(), false);

		// Every navigation/typing method is itself a no-op while not editing.
		Assert.boolEquals(workspace.moveMemoryInspectorEditCursor(MemoryInspectorEditCursorMovement.RIGHT), false);
		Assert.boolEquals(workspace.typeMemoryInspectorEditChar('5') == MemoryInspectorEditCharResult.NOT_HANDLED, true);
	}

	/** Ported from MemoryInspectorControlImpl::KeyDown's edit-mode branch. */
	private static void testCursorNavigation() {
		Workspace workspace = newWorkspaceWithSelectedSegment(0x21); // 33 bytes: two full 16-byte lines plus one.
		workspace.enterMemoryInspectorEditMode(0, MemoryInspectorEditPane.HEX_HIGH);

		// Right within a byte: high -> low nibble, same offset.
		workspace.moveMemoryInspectorEditCursor(MemoryInspectorEditCursorMovement.RIGHT);
		assertCursor(workspace, 0, MemoryInspectorEditPane.HEX_LOW);

		// Right from the low nibble crosses to the next byte's high nibble.
		workspace.moveMemoryInspectorEditCursor(MemoryInspectorEditCursorMovement.RIGHT);
		assertCursor(workspace, 1, MemoryInspectorEditPane.HEX_HIGH);

		// Left from the high nibble crosses back to the previous byte's low nibble.
		workspace.moveMemoryInspectorEditCursor(MemoryInspectorEditCursorMovement.LEFT);
		assertCursor(workspace, 0, MemoryInspectorEditPane.HEX_LOW);

		// Left at offset 0's low nibble goes back to its own high nibble, not offset -1.
		workspace.moveMemoryInspectorEditCursor(MemoryInspectorEditCursorMovement.LEFT);
		assertCursor(workspace, 0, MemoryInspectorEditPane.HEX_HIGH);
		workspace.moveMemoryInspectorEditCursor(MemoryInspectorEditCursorMovement.LEFT);
		assertCursor(workspace, 0, MemoryInspectorEditPane.HEX_HIGH); // No further effect.

		// Down/Up move a whole line (16 bytes), resetting to the high nibble.
		workspace.moveMemoryInspectorEditCursor(MemoryInspectorEditCursorMovement.DOWN);
		assertCursor(workspace, 16, MemoryInspectorEditPane.HEX_HIGH);
		workspace.moveMemoryInspectorEditCursor(MemoryInspectorEditCursorMovement.DOWN);
		assertCursor(workspace, 32, MemoryInspectorEditPane.HEX_HIGH); // Last valid offset (size 0x21 = 33).
		workspace.moveMemoryInspectorEditCursor(MemoryInspectorEditCursorMovement.DOWN);
		assertCursor(workspace, 32, MemoryInspectorEditPane.HEX_HIGH); // Would overshoot - no effect.
		workspace.moveMemoryInspectorEditCursor(MemoryInspectorEditCursorMovement.UP);
		assertCursor(workspace, 16, MemoryInspectorEditPane.HEX_HIGH);

		// Home/End jump to the segment's first/last offset.
		workspace.moveMemoryInspectorEditCursor(MemoryInspectorEditCursorMovement.END);
		assertCursor(workspace, 32, MemoryInspectorEditPane.HEX_HIGH);
		workspace.moveMemoryInspectorEditCursor(MemoryInspectorEditCursorMovement.HOME);
		assertCursor(workspace, 0, MemoryInspectorEditPane.HEX_HIGH);

		// The ASCII pane moves byte-wise for all six movements, no nibble concept.
		workspace.typeMemoryInspectorEditChar('\t'); // Switch to the ASCII pane.
		assertCursor(workspace, 0, MemoryInspectorEditPane.ASCII);
		workspace.moveMemoryInspectorEditCursor(MemoryInspectorEditCursorMovement.LEFT);
		assertCursor(workspace, 0, MemoryInspectorEditPane.ASCII); // No effect at offset 0.
		workspace.moveMemoryInspectorEditCursor(MemoryInspectorEditCursorMovement.RIGHT);
		assertCursor(workspace, 1, MemoryInspectorEditPane.ASCII);
		workspace.moveMemoryInspectorEditCursor(MemoryInspectorEditCursorMovement.DOWN);
		assertCursor(workspace, 17, MemoryInspectorEditPane.ASCII); // Stays on the ASCII pane, unlike the hex panes.
	}

	private static void assertCursor(Workspace workspace, int expectedOffset, MemoryInspectorEditPane expectedPane) {
		Assert.longEquals(workspace.getMemoryInspectorEditCursorOffset(), expectedOffset);
		Assert.boolEquals(workspace.getMemoryInspectorEditCursorPane() == expectedPane, true);
	}

	/** Ported from MemoryInspectorControlImpl::Char's hex-nibble write path. */
	private static void testHexDigitWrite() {
		Workspace workspace = newWorkspaceWithSelectedSegment(2);
		Segment segment = workspace.getSegmentList().getSegment(0);
		segment.setData(0, 0x00);
		segment.setData(1, 0x00);
		workspace.enterMemoryInspectorEditMode(0, MemoryInspectorEditPane.HEX_HIGH);

		Assert.boolEquals(workspace.typeMemoryInspectorEditChar('A') == MemoryInspectorEditCharResult.HANDLED, true);
		Assert.longEquals(segment.getData(0) & 0xFF, 0xA0);
		assertCursor(workspace, 0, MemoryInspectorEditPane.HEX_LOW);

		Assert.boolEquals(workspace.typeMemoryInspectorEditChar('5') == MemoryInspectorEditCharResult.HANDLED, true);
		Assert.longEquals(segment.getData(0) & 0xFF, 0xA5);
		assertCursor(workspace, 1, MemoryInspectorEditPane.HEX_HIGH); // Advanced to the next byte.

		// Reaching the last byte's low nibble reports HANDLED_AT_BUFFER_END and does not advance further.
		workspace.typeMemoryInspectorEditChar('F');
		MemoryInspectorEditCharResult result = workspace.typeMemoryInspectorEditChar('F');
		Assert.boolEquals(result == MemoryInspectorEditCharResult.HANDLED_AT_BUFFER_END, true);
		Assert.longEquals(segment.getData(1) & 0xFF, 0xFF);
		assertCursor(workspace, 1, MemoryInspectorEditPane.HEX_LOW); // Left exactly where it was, matching the exit path's own contract.
	}

	/** Ported from MemoryInspectorControlImpl::Char's ASCII-pane write path, including the SBYTE transform. */
	private static void testAsciiWrite() {
		Workspace workspace = newWorkspaceWithSelectedSegment(3);
		Segment segment = workspace.getSegmentList().getSegment(0);
		workspace.enterMemoryInspectorEditMode(0, MemoryInspectorEditPane.ASCII);

		Assert.boolEquals(workspace.typeMemoryInspectorEditChar('A') == MemoryInspectorEditCharResult.HANDLED, true);
		Assert.longEquals(segment.getData(0) & 0xFF, 'A'); // Not SBYTE-typed - written verbatim.
		assertCursor(workspace, 1, MemoryInspectorEditPane.ASCII);

		segment.setType(1, MemoryType.SBYTE);
		Assert.boolEquals(workspace.typeMemoryInspectorEditChar('A') == MemoryInspectorEditCharResult.HANDLED, true);
		Assert.longEquals(segment.getData(1) & 0xFF, MemoryType.toSbyteInternalCode('A')); // SBYTE-typed - transformed.
		assertCursor(workspace, 2, MemoryInspectorEditPane.ASCII);

		// Enter/Return writes the Atari end-of-line byte, at the segment's last offset, reporting HANDLED_AT_BUFFER_END.
		MemoryInspectorEditCharResult result = workspace.typeMemoryInspectorEditChar('\r');
		Assert.boolEquals(result == MemoryInspectorEditCharResult.HANDLED_AT_BUFFER_END, true);
		Assert.longEquals(segment.getData(2) & 0xFF, 0x9B);

		// Tab switches panes without touching data.
		workspace.enterMemoryInspectorEditMode(0, MemoryInspectorEditPane.ASCII);
		int before = segment.getData(0) & 0xFF;
		Assert.boolEquals(workspace.typeMemoryInspectorEditChar('\t') == MemoryInspectorEditCharResult.HANDLED, true);
		assertCursor(workspace, 0, MemoryInspectorEditPane.HEX_HIGH);
		Assert.longEquals(segment.getData(0) & 0xFF, before);
		Assert.boolEquals(workspace.typeMemoryInspectorEditChar('\t') == MemoryInspectorEditCharResult.HANDLED, true);
		assertCursor(workspace, 0, MemoryInspectorEditPane.ASCII); // Tab from ASCII always lands on hex-high, then back to ASCII here.
	}

	/**
	 * Deliberately NOT replicating MemoryInspectorControlImpl.cpp Char()'s
	 * exclusion of '~', '{', '}' from the printable range - see {@link
	 * Workspace}'s own javadoc.
	 */
	private static void testPrintableRangeIncludesTildeBraces() {
		Workspace workspace = newWorkspaceWithSelectedSegment(4); // One byte more than written, so every write below is a plain HANDLED, not HANDLED_AT_BUFFER_END.
		workspace.enterMemoryInspectorEditMode(0, MemoryInspectorEditPane.ASCII);
		Segment segment = workspace.getSegmentList().getSegment(0);

		Assert.boolEquals(workspace.typeMemoryInspectorEditChar('~') == MemoryInspectorEditCharResult.HANDLED, true);
		Assert.longEquals(segment.getData(0) & 0xFF, '~');
		Assert.boolEquals(workspace.typeMemoryInspectorEditChar('{') == MemoryInspectorEditCharResult.HANDLED, true);
		Assert.longEquals(segment.getData(1) & 0xFF, '{');
		Assert.boolEquals(workspace.typeMemoryInspectorEditChar('}') == MemoryInspectorEditCharResult.HANDLED, true);
		Assert.longEquals(segment.getData(2) & 0xFF, '}');
	}

	private static void testNotHandledCases() {
		Workspace workspace = newWorkspaceWithSelectedSegment(2);
		workspace.enterMemoryInspectorEditMode(0, MemoryInspectorEditPane.HEX_HIGH);
		Assert.boolEquals(workspace.typeMemoryInspectorEditChar('G') == MemoryInspectorEditCharResult.NOT_HANDLED, true); // Not a hex digit.
		Assert.boolEquals(workspace.typeMemoryInspectorEditChar((char) 0) == MemoryInspectorEditCharResult.NOT_HANDLED, true);

		workspace.typeMemoryInspectorEditChar('\t'); // Switch to the ASCII pane.
		Assert.boolEquals(workspace.typeMemoryInspectorEditChar((char) 27) == MemoryInspectorEditCharResult.NOT_HANDLED, true); // Below ' '.
		Assert.boolEquals(workspace.typeMemoryInspectorEditChar((char) 128) == MemoryInspectorEditCharResult.NOT_HANDLED, true); // >= 128.
	}
}
