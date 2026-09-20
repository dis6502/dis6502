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
	}

	private static void testReadFile(ComputerSystem computerSystem, String areaPath, String fileName)
			throws IOException {
		String filePath = TEST_RESOURCES_DIR + "/" + areaPath + "/" + fileName;
		File outFolder = Files.createTempDirectory("dis6502-test-").toFile();
		String outFilePath = new File(outFolder, fileName).getPath();
		Atari800Test.assertSegmentListEquals(computerSystem, FileType.EXECUTABLE_FILE, filePath, outFilePath);
	}
}
