/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Ported from systems/ComputerSystemTest.h / ComputerSystemTest.cpp. {@code
 * TestReadDiskImageFile} is not ported - see {@link Atari800Test}'s javadoc
 * for why the disk-image subsystem it exercises is out of scope.
 *
 * @author Peter Dell
 */
public final class ComputerSystemTest {

	private static final String TEST_RESOURCES_DIR = "test-resources";

	private ComputerSystemTest() {
	}

	public static void testSystems(ComputerSystemFactory computerSystemFactory) throws IOException {
		testAtari800(computerSystemFactory.getComputerSystem(ComputerSystemType.ATARI800));
		testC64(computerSystemFactory.getComputerSystem(ComputerSystemType.C64));
	}

	private static void testAtari800(ComputerSystem computerSystem) throws IOException {
		testReadFile(computerSystem, "system/atari800", "Segments-RUNAD.xex");
		testReadFile(computerSystem, "system/atari800", "Segments-SillyThings.xex");
	}

	private static void testC64(ComputerSystem computerSystem) throws IOException {
		testReadFile(computerSystem, "system/c64", "HelloWorld.prg");
		testC64Addresses(computerSystem);
		testC64CodeTraceFollowsVector(computerSystem);
	}

	/** New (not ported): the C64's real pointer locations - see {@link C64}'s javadoc. */
	private static void testC64Addresses(ComputerSystem computerSystem) {
		Assert.boolEquals(computerSystem.isVectorAddress(0x0314), true); // CINV
		Assert.boolEquals(computerSystem.isVectorAddress(0xFFFE), true); // IRQVEC
		Assert.boolEquals(computerSystem.isVectorAddress(0x0200), false); // The C++ version's dummy entry.
		Assert.boolEquals(computerSystem.isBaseAddress(0x0314), true);
		Assert.boolEquals(computerSystem.isBaseAddress(0x0281), true); // MEMSTR: a pointer, but to data.
		Assert.boolEquals(computerSystem.isVectorAddress(0x0281), false);
		Assert.boolEquals(computerSystem.isBaseAddress(0x00FB), true); // Zero page.
		Assert.boolEquals(computerSystem.isBaseAddress(0xDC04), false); // CIA timer: 16 bit, but no address.
	}

	/**
	 * New (not ported): a {@code .prg} must load as a binary segment, and
	 * code trace must recognize {@code LDA #<irq / STA CINV / LDA #>irq / STA
	 * CINV+1}, retag the two immediates as low/high byte of an address, and
	 * follow the vector into the IRQ handler that nothing else references.
	 */
	private static void testC64CodeTraceFollowsVector(ComputerSystem computerSystem) throws IOException {
		int[] program = { 0x00, 0xC0, // Load address $C000.
				0xA9, 0x0E, // C000 LDA #<$C00E
				0x8D, 0x14, 0x03, // C002 STA CINV
				0xA9, 0xC0, // C005 LDA #>$C00E
				0x8D, 0x15, 0x03, // C007 STA CINV+1
				0x60, // C00A RTS
				0x00, 0x00, 0x00, // C00B Data the trace must not touch.
				0xEE, 0x19, 0xD0, // C00E INC VICIRQ
				0x4C, 0x31, 0xEA }; // C011 JMP SYSIRQ
		byte[] bytes = new byte[program.length];
		for (int i = 0; i < program.length; i++) {
			bytes[i] = (byte) program[i];
		}
		File file = File.createTempFile("dis6502-c64-", ".prg");
		file.deleteOnExit();
		Files.write(file.toPath(), bytes);

		Workspace workspace = new Workspace(new ComputerSystemFactory());
		workspace.setComputerSystemType(ComputerSystemType.C64);
		WorkspaceLogic workspaceLogic = new WorkspaceLogic(new com.wudsn.tools.dis6502.Application());
		Assert.boolEquals(workspaceLogic.addFile(workspace, FileType.EXECUTABLE_FILE, file.getPath()), true);

		Segment segment = workspace.getSegmentList().getSegment(0);
		Assert.boolEquals(segment.bBinary, true);
		Assert.longEquals(segment.wBegin, 0xC000);

		new GuessCodeLogic(workspace).guess(segment, 0);

		// LOBYTE/HIBYTE ("code with low/high byte") are set on the LDA opcode itself, not on its operand.
		Assert.boolEquals(segment.memoryBlock.getTypeAt(0x00) == MemoryType.LOBYTE, true);
		Assert.boolEquals(segment.memoryBlock.getTypeAt(0x05) == MemoryType.HIBYTE, true);
		Assert.boolEquals(segment.memoryBlock.getTypeAt(0x02) == MemoryType.CODE, true);
		Assert.boolEquals(segment.memoryBlock.getTypeAt(0x0B) == MemoryType.UNKNOWN, true);
		Assert.boolEquals(segment.memoryBlock.getTypeAt(0x0E) == MemoryType.CODE, true); // Only reachable through CINV.
		Assert.boolEquals(segment.memoryBlock.getTypeAt(0x11) == MemoryType.CODE, true);
	}

	private static void testReadFile(ComputerSystem computerSystem, String areaPath, String fileName)
			throws IOException {
		String filePath = TEST_RESOURCES_DIR + "/" + areaPath + "/" + fileName;
		File outFolder = Files.createTempDirectory("dis6502-test-").toFile();
		String outFilePath = new File(outFolder, fileName).getPath();
		Atari800Test.assertSegmentListEquals(computerSystem, FileType.EXECUTABLE_FILE, filePath, outFilePath);
	}
}
