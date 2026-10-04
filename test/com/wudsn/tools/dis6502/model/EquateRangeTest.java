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

import com.wudsn.tools.dis6502.Application;
import com.wudsn.tools.dis6502.model.system.ComputerSystemFactory;
import com.wudsn.tools.dis6502.model.system.ComputerSystemType;

/**
 * User ranges (Equates > Define Address Range) in listings, on the real Atari
 * 800 system equates, with unreferenced system labels omitted:
 * <ul>
 * <li>A range based on a system equate marks that equate as referenced, so the
 * listing defines it and assembles - on Windows checked with MADS, whose
 * output must be the program's bytes.</li>
 * <li>A range takes its base equate's access: a range based on the {@code #}
 * constant ICCOM matches immediate values, but not memory accesses, so {@code
 * sta $03} stays {@code CASINI+1}.</li>
 * </ul>
 *
 * @author Peter Dell
 */
public final class EquateRangeTest {

	private static final String MADS_EXE_PATH = "test-resources/asm/mads/mads.exe";

	/** The main file of the listing last saved by {@link #disassemble}. */
	private static File asmFile;

	private EquateRangeTest() {
	}

	public static void testEquateRanges() throws IOException, InterruptedException {
		testRangeOnSystemEquate();
		testRangeAccess();
	}

	private static void testRangeOnSystemEquate() throws IOException, InterruptedException {
		int[] code = { 0x8D, 0x17, 0xD0, 0x60 }; // STA $D017, RTS
		Workspace workspace = createWorkspace(code);
		Equate colpf0 = workspace.getSystemEquateList().getEquateByLabel("COLPF0");
		Assert.notNull(colpf0);
		workspace.getUserEquateList().setRange("COLPF0", colpf0.getLabelValue(), colpf0.getLabelAccess(), 0xD017,
				0xD018);

		String listing = disassemble(workspace, "range");
		Assert.boolEquals(listing.contains("sta COLPF0+1"), true);
		Assert.boolEquals(listing.contains("COLPF0 equ $D016"), true);
		assembleWithMADS(code);
	}

	private static void testRangeAccess() throws IOException, InterruptedException {
		int[] code = { 0x85, 0x03, 0x60 }; // STA $03, RTS
		Workspace workspace = createWorkspace(code);
		Equate iccom = workspace.getSystemEquateList().getEquateByLabel("ICCOM");
		Assert.notNull(iccom);
		Assert.longEquals(iccom.getLabelAccess(), LabelAccess.IMMEDIATE);
		workspace.getUserEquateList().setRange("ICCOM", iccom.getLabelValue(), iccom.getLabelAccess(), 0x03, 0x04);

		String listing = disassemble(workspace, "access");
		Assert.boolEquals(listing.contains("sta CASINI+1"), true); // Not ICCOM+1: a memory access.
		Assert.boolEquals(listing.contains("ICCOM+1"), false);

		// An immediate operand only becomes a label once its byte is typed so; the range matches it then.
		EquateList userEquates = workspace.getUserEquateList();
		Assert.stringEquals(userEquates.findEquateByAddress(0x03, LabelAccess.IMMEDIATE, true).getLabel(), "ICCOM+1");
		Assert.isNull(userEquates.findEquateByAddress(0x03, LabelAccess.WRITE, true));
	}

	private static Workspace createWorkspace(int[] code) {
		Workspace workspace = new Workspace(new ComputerSystemFactory());
		workspace.setComputerSystemType(ComputerSystemType.ATARI800);
		new WorkspaceLogic(new Application()).loadSystemEquates(workspace);
		workspace.getProfile().omitUnreferencedSystemLabels = true;
		Segment segment = workspace.getSegmentList().insertSegmentAt(0);
		segment.setHeader(FileHeader.ATARI_BINARY);
		segment.bBinary = true;
		segment.wBegin = 0x2000;
		segment.wEnd = 0x2000 + code.length - 1;
		segment.createMemoryBlockFromBeginToEnd();
		for (int i = 0; i < code.length; i++) {
			segment.setData(i, code[i]);
		}
		segment.setType(0, MemoryType.CODE, code.length);
		return workspace;
	}

	/** Disassembles and saves the listing; returns all its files' text, blanks collapsed. */
	private static String disassemble(Workspace workspace, String name) throws IOException {
		Application application = new Application();
		Disassembly disassembly = new Disassembly();
		disassembly.setWorkspace(workspace);
		DisassemblyProgressMonitor monitor = new DisassemblyProgressMonitor(application);
		disassembly.setProgressMonitor(monitor);
		monitor.startDisassembly(disassembly);

		File folder = Files.createTempDirectory("dis6502-equate-range-test-").toFile();
		workspace.getProfile().useLineNumbers = false;
		asmFile = new File(folder, name + ".asm");
		new DisassemblyResultFile(application).saveListing(workspace.getDisassemblyResult(), workspace.getProfile(),
				asmFile);
		StringBuilder text = new StringBuilder();
		for (File file : folder.listFiles()) {
			text.append(new String(Files.readAllBytes(file.toPath()))).append('\n');
		}
		return text.toString().replaceAll("[ \\t]+", " ").replace("\n ", "\n");
	}

	/** On Windows, assembles the saved listing with MADS; the program's bytes must come out. */
	private static void assembleWithMADS(int[] code) throws IOException, InterruptedException {
		if (!System.getProperty("os.name", "").startsWith("Windows")) {
			Assert.log("EquateRangeTest: MADS check skipped, the vendored MADS is a Windows executable");
			return;
		}
		ProcessBuilder processBuilder = new ProcessBuilder(new File(MADS_EXE_PATH).getAbsolutePath(),
				asmFile.getAbsolutePath(), "-o:out.xex");
		processBuilder.directory(asmFile.getParentFile());
		processBuilder.redirectErrorStream(true);
		processBuilder.redirectOutput(new File(asmFile.getParentFile(), "mads.txt"));
		int exitCode = processBuilder.start().waitFor();
		String output = new String(Files.readAllBytes(new File(asmFile.getParentFile(), "mads.txt").toPath()));
		Assert.stringEquals(exitCode == 0 ? "" : output, "");

		byte[] xex = Files.readAllBytes(new File(asmFile.getParentFile(), "out.xex").toPath());
		byte[] expected = new byte[code.length];
		for (int i = 0; i < code.length; i++) {
			expected[i] = (byte) code[i];
		}
		// $FFFF, start, end, then the bytes.
		Assert.boolEquals(Arrays.equals(Arrays.copyOfRange(xex, 6, 6 + code.length), expected), true);
	}
}
