/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

import com.wudsn.tools.dis6502.Application;

/**
 * Ported from {@code MainTest::TestWorkspace} (C++'s {@code MainTest.cpp},
 * itself only reachable through the console-mode self-test harness
 * ({@code MainUITest.cpp}/the {@code /TEST:} command-line modes) that this
 * port does not have - see gap #5's history in {@code
 * plans/REMAINING_GAPS_OVERVIEW.md} for why only this method's two concrete
 * checks were worth recovering, not the harness itself.
 *
 * @author Peter Dell
 */
public final class WorkspaceLogicTest {

	private static final String FIXTURE_PATH = "test-resources/workspace/SynCalc-(1993)-128K-Main.wrk";

	private WorkspaceLogicTest() {
	}

	public static void testWorkspaceLogic() {
		testLoad();
		testSplitAtRebasesComments();
		testLoadSystemEquates();

		Assert.log("WorkspaceLogicTest completed");
	}

	/** Ported from the first half of {@code MainTest::TestWorkspace}: loading a real, non-trivial workspace file must succeed. */
	private static void testLoad() {
		Application application = new Application();
		WorkspaceLogic workspaceLogic = new WorkspaceLogic(application);
		Workspace workspace = new Workspace(new ComputerSystemFactory());

		Assert.boolEquals(workspaceLogic.load(workspace, FIXTURE_PATH), true);
		Assert.boolEquals(workspace.getSegmentList().getCount() > 0, true);
	}

	/**
	 * New (not ported): every computer system that ships a system equates
	 * file must load it, a system without one must end up empty, and
	 * switching systems must replace - not append to - the previous system's
	 * equates. The C64 deliberately ships none - see {@link
	 * WorkspaceLogic#loadSystemEquates}.
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
		Assert.boolEquals(systemEquateList.isEmpty(), true);

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
	 * Ported from the second half of {@code MainTest::TestWorkspace}: a
	 * segment with two user comments split at offset 0x80 must end up with
	 * each half's comment surviving at its correctly rebased offset - the
	 * same {@link Segment#splitAt} comment-rebasing loop {@link
	 * SegmentTest#testSegmentRangeEdit} covers for {@link
	 * Segment#deleteRange}/{@link Segment#insertRange}, recovered here as a
	 * second, independent set of hand-computed values from the C++ source.
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
