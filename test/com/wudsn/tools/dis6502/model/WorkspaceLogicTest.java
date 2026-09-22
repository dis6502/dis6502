/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

import java.io.File;
import java.io.IOException;

import com.wudsn.tools.dis6502.Application;

/**
 * Covers {@link WorkspaceLogic}.
 *
 * @author Peter Dell
 */
public final class WorkspaceLogicTest {

	private static final String FIXTURE_PATH = "test-resources/workspace/SynCalc-(1993)-128K-Main.wrk";
	private static final String C64_FIXTURE_PATH = "test-resources/system/c64/HelloWorld.wrk";

	private WorkspaceLogicTest() {
	}

	public static void testWorkspaceLogic() throws IOException {
		testLoad();
		testSplitAtRebasesComments();
		testLoadSystemEquates();
		testC64Workspace();

		Assert.log("WorkspaceLogicTest completed");
	}

	/** Loading a real, non-trivial workspace file must succeed. */
	private static void testLoad() {
		Application application = new Application();
		WorkspaceLogic workspaceLogic = new WorkspaceLogic(application);
		Workspace workspace = new Workspace(new ComputerSystemFactory());

		Assert.boolEquals(workspaceLogic.load(workspace, FIXTURE_PATH), true);
		Assert.boolEquals(workspace.getSegmentList().getCount() > 0, true);
	}

	/**
	 * Every computer system that ships a system equates file must load it
	 * completely (every line of the file yields exactly one equate, so a
	 * count equal to the file's line count proves no line was rejected), a
	 * system without one must end up empty, and switching systems must
	 * replace - not append to - the previous system's equates.
	 */
	private static void testLoadSystemEquates() {
		Application application = new Application();
		WorkspaceLogic workspaceLogic = new WorkspaceLogic(application);
		Workspace workspace = new Workspace(new ComputerSystemFactory());
		EquateList systemEquateList = workspace.getSystemEquateList();

		workspace.setComputerSystemType(ComputerSystemType.ATARI800);
		workspaceLogic.loadSystemEquates(workspace);
		Assert.longEquals(systemEquateList.getCount(), 899); // One equate (label, comment or empty line) per line of Atari800.equ.
		Equate colbk = systemEquateList.getEquateByLabel("COLBK");
		Assert.notNull(colbk);
		Assert.longEquals(colbk.getLabelValue(), 0xD01A);

		workspace.setComputerSystemType(ComputerSystemType.C64);
		workspaceLogic.loadSystemEquates(workspace);
		Assert.longEquals(systemEquateList.getCount(), 534);
		Assert.boolEquals(systemEquateList.getEquateByLabel("COLBK") == null, true); // An Atari label - see loadSystemEquates' javadoc.
		Equate chrout = systemEquateList.getEquateByLabel("CHROUT");
		Assert.notNull(chrout);
		Assert.longEquals(chrout.getLabelValue(), 0xFFD2);

		workspace.setComputerSystemType(ComputerSystemType.ATARI5200);
		workspaceLogic.loadSystemEquates(workspace);
		Assert.longEquals(systemEquateList.getCount(), 159);

		workspace.setComputerSystemType(ComputerSystemType.ORIC);
		workspaceLogic.loadSystemEquates(workspace);
		Assert.longEquals(systemEquateList.getCount(), 50);

		workspace.setComputerSystemType(ComputerSystemType.UNKNOWN);
		workspaceLogic.loadSystemEquates(workspace);
		Assert.boolEquals(systemEquateList.isEmpty(), true);
	}

	/**
	 * A workspace for a system other than the Atari 800 - {@code
	 * HelloWorld.prg} loaded as C64, its BASIC stub marked as bytes, its
	 * machine code traced, two user comments added - must load with all of
	 * that intact, and must survive being saved and loaded again.
	 */
	private static void testC64Workspace() throws IOException {
		Application application = new Application();
		WorkspaceLogic workspaceLogic = new WorkspaceLogic(application);

		Workspace workspace = new Workspace(new ComputerSystemFactory());
		workspace.setComputerSystemType(ComputerSystemType.ATARI800); // The file, not the previous state, decides.
		Assert.boolEquals(workspaceLogic.load(workspace, C64_FIXTURE_PATH), true);
		assertC64Workspace(workspace);

		File savedFile = File.createTempFile("dis6502-c64-", ".wrk");
		savedFile.deleteOnExit();
		Assert.boolEquals(workspaceLogic.save(workspace, savedFile.getPath()), true);

		Workspace reloadedWorkspace = new Workspace(new ComputerSystemFactory());
		Assert.boolEquals(workspaceLogic.load(reloadedWorkspace, savedFile.getPath()), true);
		assertC64Workspace(reloadedWorkspace);
	}

	private static void assertC64Workspace(Workspace workspace) {
		Assert.boolEquals(workspace.getComputerSystem().getType() == ComputerSystemType.C64, true);
		Assert.longEquals(workspace.getSegmentList().getCount(), 1);

		Segment segment = workspace.getSegmentList().getSegment(0);
		Assert.longEquals(segment.wBegin, 0x0801);
		Assert.longEquals(segment.wEnd, 0x0818);
		Assert.boolEquals(segment.bBinary, true);
		Assert.longEquals(segment.getData(0x0F), 0xEE); // INC $D020
		for (int offset = 0; offset < segment.getSize(); offset++) {
			MemoryType expectedType = offset < 0x0F ? MemoryType.BYTE : MemoryType.CODE;
			Assert.boolEquals(segment.memoryBlock.getTypeAt(offset) == expectedType, true);
		}
		Assert.stringEquals(segment.findComment(0x00), "BASIC stub: 10 SYS 2064");
		Assert.stringEquals(segment.findComment(0x0F), "Flash the border color");

		// The workspace carries its own system equates - genuine C64 ones.
		EquateList systemEquateList = workspace.getSystemEquateList();
		Assert.longEquals(systemEquateList.getCount(), 534);
		Assert.longEquals(systemEquateList.getEquateByLabel("EXTCOL").getLabelValue(), 0xD020);
	}

	/**
	 * A segment with two user comments split at offset 0x80 must end up with
	 * each half's comment surviving at its correctly rebased offset - the
	 * same {@link Segment#splitAt} comment-rebasing loop {@link
	 * SegmentTest#testSegmentRangeEdit} covers for {@link
	 * Segment#deleteRange}/{@link Segment#insertRange}, checked here with a
	 * second, independent set of hand-computed values.
	 */
	private static void testSplitAtRebasesComments() {
		Workspace workspace = new Workspace(new ComputerSystemFactory());
		SegmentList segmentList = workspace.getSegmentList();

		Segment segment = segmentList.insertSegmentAt(0);
		segment.setHeader(FileHeader.ATARI_BINARY);
		segment.wBegin = 0x1000;
		segment.wEnd = 0x10ff;
		segment.createMemoryBlockFromBeginToEnd();
		segmentList.setUserComment(0, 0x40, 0x40, "Comment at 0x1040");
		segmentList.setUserComment(0, 0xC0, 0x40, "Comment at 0x10C0");

		Segment newSegment = segmentList.insertSegmentAt(1);
		segment.splitAt(0x80, newSegment);

		Assert.longEquals(segment.wBegin, 0x1000);
		Assert.longEquals(segment.wEnd, 0x107f);
		Assert.stringEquals(segment.findComment(0x40), "Comment at 0x1040");

		Assert.longEquals(newSegment.wBegin, 0x1080);
		Assert.longEquals(newSegment.wEnd, 0x10ff);
		Assert.stringEquals(newSegment.findComment(0x40), "Comment at 0x10C0");
	}
}
