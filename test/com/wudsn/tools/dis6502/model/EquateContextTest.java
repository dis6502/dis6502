/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import com.wudsn.tools.dis6502.Application;
import com.wudsn.tools.dis6502.model.system.ComputerSystemFactory;

/**
 * Equate contexts on the real Atari 800 system equates, whose SpartaDOS X
 * labels are in the context "SDX":
 * <ul>
 * <li>A normal segment gets no SDX labels - neither on page 7 nor the jump
 * vectors at $FFC0-$FFD5 - and the listing defines none, even with
 * unreferenced system labels written.</li>
 * <li>With "SDX" active in the workspace, or in an SDX segment, it does.</li>
 * <li>A workspace keeps its active contexts and its equates' contexts, and
 * one saved before contexts existed gets the shipped system equates'
 * contexts when it is opened.</li>
 * </ul>
 *
 * @author Peter Dell
 */
public final class EquateContextTest {

	private static final int[] CODE = { 0x8D, 0x00, 0x07, 0x20, 0xC0, 0xFF, 0x60 }; // STA $0700, JSR $FFC0, RTS

	private EquateContextTest() {
	}

	public static void testEquateContexts() throws IOException, InterruptedException {
		testNormalSegment();
		testActiveInWorkspace();
		testSDXSegment();
		testWorkspaceFile();
	}

	private static void testNormalSegment() throws IOException, InterruptedException {
		Workspace workspace = EquateRangeTest.createWorkspace(CODE);
		workspace.getProfile().omitUnreferencedSystemLabels = false;
		Assert.stringEquals(workspace.getSystemEquateList().getEquateByLabel("S_FLAG").getContexts().toString(),
				"[SDX]");

		String listing = EquateRangeTest.disassemble(workspace, "normal");
		Assert.boolEquals(listing.contains("S_FLAG"), false);
		Assert.boolEquals(listing.contains("JGETTD"), false);
		Assert.boolEquals(listing.contains("COLPF0 equ $D016"), true); // Global labels are still all written.
		EquateRangeTest.assembleWithMADS(CODE);
	}

	private static void testActiveInWorkspace() throws IOException, InterruptedException {
		Workspace workspace = EquateRangeTest.createWorkspace(CODE);
		workspace.setActiveContexts(Collections.singleton("SDX"));

		String listing = EquateRangeTest.disassemble(workspace, "workspace");
		Assert.boolEquals(listing.contains("sta S_FLAG"), true);
		Assert.boolEquals(listing.contains("jsr JGETTD"), true);
		Assert.boolEquals(listing.contains("S_FLAG equ $0700"), true);
		EquateRangeTest.assembleWithMADS(CODE);
	}

	private static void testSDXSegment() throws IOException {
		Workspace workspace = EquateRangeTest.createWorkspace(CODE);
		Segment segment = workspace.getSegmentList().getSegment(0);
		Assert.stringEquals(workspace.getActiveContexts(segment).toString(), "[]");
		segment.setHeader(FileHeader.SDX_FIXED_BLK);
		Assert.stringEquals(workspace.getActiveContexts(segment).toString(), "[SDX]");

		String listing = EquateRangeTest.disassemble(workspace, "sdx");
		Assert.boolEquals(listing.contains("sta S_FLAG"), true);
		Assert.boolEquals(listing.contains("S_FLAG equ $0700"), true);
	}

	private static void testWorkspaceFile() throws IOException {
		Application application = new Application();
		WorkspaceLogic workspaceLogic = new WorkspaceLogic(application);
		File folder = Files.createTempDirectory("dis6502-equate-context-test-").toFile();

		// Active contexts and the equates' contexts are kept.
		Workspace workspace = EquateRangeTest.createWorkspace(CODE);
		workspace.setActiveContexts(new TreeSet<>(Arrays.asList("SDX", "OS-XL")));
		File file = new File(folder, "contexts.dis6502");
		Assert.boolEquals(workspaceLogic.save(workspace, file.getPath()), true);
		Workspace loaded = new Workspace(new ComputerSystemFactory());
		Assert.boolEquals(workspaceLogic.load(loaded, file.getPath()), true);
		Assert.stringEquals(loaded.getActiveContexts().toString(), "[OS-XL, SDX]");
		Assert.stringEquals(loaded.getSystemEquateList().getEquateByLabel("JGETTD").getContexts().toString(),
				"[SDX]");

		// System equates stored before contexts existed get the shipped ones' contexts.
		workspace = EquateRangeTest.createWorkspace(CODE);
		for (Equate equate : workspace.getSystemEquateList().getEquates()) {
			equate.setContexts(Collections.emptyList());
		}
		Assert.stringEquals(workspace.getContextNames().toString(), "[]");
		file = new File(folder, "old.dis6502");
		Assert.boolEquals(workspaceLogic.save(workspace, file.getPath()), true);
		loaded = new Workspace(new ComputerSystemFactory());
		Assert.boolEquals(workspaceLogic.load(loaded, file.getPath()), true);
		EquateList systemEquates = loaded.getSystemEquateList();
		Assert.stringEquals(systemEquates.getEquateByLabel("S_FLAG").getContexts().toString(), "[SDX]");
		Assert.stringEquals(systemEquates.getEquateByLabel("JGETTD").getContexts().toString(), "[SDX]");
		Assert.boolEquals(systemEquates.getEquateByLabel("COLPF0").isGlobal(), true);
		Set<String> none = Collections.emptySet();
		Assert.isNull(systemEquates.findEquateByAddress(0x0700, LabelAccess.WRITE, none));

		// Equates that already use contexts are left alone.
		Assert.boolEquals(systemEquates.copyContextsFrom(systemEquates), false);
	}
}
