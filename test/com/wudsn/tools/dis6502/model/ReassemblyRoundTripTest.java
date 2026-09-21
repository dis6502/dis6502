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
 * genuine OS/hardware labels and must still reassemble byte-exactly. A
 * fifth, C64 unit (new, not ported) does the same for a system other than
 * the Atari 800, with {@code C64.equ}'s labels.
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

		// New (not ported): a C64 workspace, checked against the very .prg it was made from.
		testUnit("c64", FileType.WORKSPACE_FILE, "test-resources/system/c64/HelloWorld.wrk",
				"test-resources/system/c64/HelloWorld.prg");

		Assert.log("ReassemblyRoundTripTest completed");
	}

	/**
	 * MADS always writes an Atari binary ({@code $FFFF}, start address, end
	 * address, data), while a C64 {@code .prg} is just the start address
	 * followed by the data - converts the former into the latter, insisting
	 * on a single block, so the two can be compared byte by byte.
	 */
	private static byte[] atariBinaryToPrg(String unitName, byte[] atariBinary) {
		int start = (atariBinary[2] & 0xFF) | ((atariBinary[3] & 0xFF) << 8);
		int end = (atariBinary[4] & 0xFF) | ((atariBinary[5] & 0xFF) << 8);
		if ((atariBinary[0] & 0xFF) != 0xFF || (atariBinary[1] & 0xFF) != 0xFF || atariBinary.length != 6 + end - start + 1) {
			Assert.fail(unitName + ": reassembled file is not a single-block Atari binary.");
		}
		byte[] prg = new byte[atariBinary.length - 4];
		prg[0] = atariBinary[2];
		prg[1] = atariBinary[3];
		System.arraycopy(atariBinary, 6, prg, 2, atariBinary.length - 6);
		return prg;
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
		if (refFilePath.endsWith(".prg")) {
			actual = atariBinaryToPrg(unitName, actual);

			// The C64 has a system label at address $0000 (D6510). Nothing references it here, so it
			// must be omitted like any other unreferenced one - address 0 used to mean "always write".
			String includeFile = new String(Files.readAllBytes(new File(outFolder, unitName + ".inc").toPath()));
			Assert.boolEquals(includeFile.contains("EXTCOL"), true);
			Assert.boolEquals(includeFile.contains("D6510"), false);
		}
		if (!Arrays.equals(actual, expected)) {
			Assert.fail(unitName + ": reassembled '" + outputFileName + "' (" + actual.length + " bytes) does not match reference '"
					+ refFilePath + "' (" + expected.length + " bytes).");
		}
	}
}
