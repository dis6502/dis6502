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

/**
 * Ported from the one part of {@code MainTest::ExecuteUnitTestItem}/{@code
 * ExecuteVariant} (C++'s {@code MainTest.cpp}, reachable only through the
 * console-mode self-test harness this port does not have - see gap #5's
 * history in {@code plans/REMAINING_GAPS_OVERVIEW.md}) worth recovering on
 * its own: for each of four real fixture programs, disassemble it, feed the
 * disassembly listing back through the real MADS assembler
 * ({@code test-resources/asm/mads/mads.exe}, the same binary C++'s own test
 * suite vendors under {@code tst/suite/asm/mads}), and assert the
 * reassembled binary is byte-for-byte identical to a known-good reference -
 * an end-to-end check that the disassembler's output is actually correct,
 * not just that individual methods behave as expected.
 * <p>
 * Unlike C++'s {@code ExecuteVariant}, which sweeps up to 16 notation-flag
 * combinations per fixture, this only exercises the one combination the
 * default {@link Profile} already produces ({@code useHexNotation}/{@code
 * showNonASCIIChararactersAsBytes} true, {@code showZPAbsoluteAsByte}/{@code
 * showOpcodeAsComment} false - matching the values C++'s {@code DEV}/{@code
 * FAST} test mode used) - a deliberate scope reduction, not an oversight;
 * see the gap #5 proposal for why. The real Atari 800 system equates are
 * loaded the same way the application does it, so the listing references
 * genuine OS/hardware labels and must still reassemble byte-exactly.
 *
 * @author Peter Dell
 */
public final class ReassemblyRoundTripTest {

	private static final String MADS_EXE_PATH = "test-resources/asm/mads/mads.exe";

	private ReassemblyRoundTripTest() {
	}

	public static void testReassemblyRoundTrip() throws IOException, InterruptedException {
		testUnit("unit001", FileType.EXECUTABLE_FILE, "test-resources/disassembly/unit001/in/autorun.xex",
				"test-resources/disassembly/unit001/ref/autorun.xex");
		testUnit("unit002", FileType.EXECUTABLE_FILE, "test-resources/disassembly/unit002/in/multisegment.xex",
				"test-resources/disassembly/unit002/ref/multisegment.xex");
		testUnit("unit003", FileType.WORKSPACE_FILE, "test-resources/disassembly/unit003/in/autorun.wrk",
				"test-resources/disassembly/unit003/ref/autorun.xex");
		testUnit("unit004", FileType.WORKSPACE_FILE, "test-resources/disassembly/unit004/in/predux-220810.wrk",
				"test-resources/disassembly/unit004/ref/predux-220810.xex");

		Assert.log("ReassemblyRoundTripTest completed");
	}

	private static void testUnit(String unitName, FileType fileType, String inFilePath, String refFilePath)
			throws IOException, InterruptedException {
		Application application = new Application();
		WorkspaceLogic workspaceLogic = new WorkspaceLogic(application);
		Workspace workspace = new Workspace(new ComputerSystemFactory());
		workspace.setComputerSystemTypeID("ATARI800");

		boolean loaded = fileType == FileType.WORKSPACE_FILE ? workspaceLogic.load(workspace, inFilePath)
				: workspaceLogic.addFile(workspace, fileType, inFilePath);
		Assert.boolEquals(loaded, true);

		// A workspace file may carry its own system equates; a plain executable never does.
		if (workspace.getSystemEquateList().isEmpty()) {
			workspaceLogic.loadSystemEquates(workspace);
		}

		Disassembly disassembly = new Disassembly();
		disassembly.setWorkspace(workspace);
		DisassemblyProgressMonitor monitor = new DisassemblyProgressMonitor(application);
		disassembly.setProgressMonitor(monitor);
		monitor.startDisassembly(disassembly);

		File outFolder = Files.createTempDirectory("dis6502-reassembly-test-").toFile();
		Assert.log(unitName + ": writing disassembly listing and running MADS in " + outFolder.getPath());

		File mainAsmFile = new File(outFolder, unitName + ".asm");
		DisassemblyResultFile disassemblyResultFile = new DisassemblyResultFile(application);
		disassemblyResultFile.saveListing(workspace.getDisassemblyResult(), workspace.getProfile(), mainAsmFile);

		String outputFileName = unitName + ".xex";
		ProcessBuilder processBuilder = new ProcessBuilder(new File(MADS_EXE_PATH).getAbsolutePath(),
				mainAsmFile.getAbsolutePath(), "-o:" + outputFileName, "-t:" + unitName + ".lab", "-l:" + unitName + ".lst");
		processBuilder.directory(outFolder);
		processBuilder.redirectOutput(new File(outFolder, "stdout.txt"));
		processBuilder.redirectError(new File(outFolder, "stderr.txt"));
		Process process = processBuilder.start();
		int exitCode = process.waitFor();
		if (exitCode != 0) {
			String stdout = new String(Files.readAllBytes(new File(outFolder, "stdout.txt").toPath()));
			String stderr = new String(Files.readAllBytes(new File(outFolder, "stderr.txt").toPath()));
			Assert.fail(unitName + ": MADS exited with code " + exitCode + ".\nstdout:\n" + stdout + "\nstderr:\n" + stderr);
			return;
		}

		File outputFile = new File(outFolder, outputFileName);
		Assert.boolEquals(outputFile.isFile(), true);

		byte[] actual = Files.readAllBytes(outputFile.toPath());
		byte[] expected = Files.readAllBytes(new File(refFilePath).toPath());
		if (!Arrays.equals(actual, expected)) {
			Assert.fail(unitName + ": reassembled '" + outputFileName + "' (" + actual.length + " bytes) does not match reference '"
					+ refFilePath + "' (" + expected.length + " bytes).");
		}
	}
}
