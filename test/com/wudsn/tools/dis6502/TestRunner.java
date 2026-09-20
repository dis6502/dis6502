/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502;

import com.wudsn.tools.dis6502.model.AssemblerTest;
import com.wudsn.tools.dis6502.model.AtariDiskImageTest;
import com.wudsn.tools.dis6502.model.ByteRangeSelectionTest;
import com.wudsn.tools.dis6502.model.ComputerSystemFactory;
import com.wudsn.tools.dis6502.model.ComputerSystemTest;
import com.wudsn.tools.dis6502.model.DisassemblyResultFileTest;
import com.wudsn.tools.dis6502.model.DisassemblyResultTest;
import com.wudsn.tools.dis6502.model.EquateListLogicTest;
import com.wudsn.tools.dis6502.model.EquateTest;
import com.wudsn.tools.dis6502.model.MemoryInspectorStateTest;
import com.wudsn.tools.dis6502.model.MemoryInspectorTest;
import com.wudsn.tools.dis6502.model.Profile1XTest;
import com.wudsn.tools.dis6502.model.SegmentTest;
import com.wudsn.tools.dis6502.model.Workspace;

/**
 * Runs the ported unit tests and reports a pass/fail summary.
 * <p>
 * This is a new, minimal aggregator - not a port of MainTest.h/MainTest.cpp,
 * whose {@code Execute}/{@code ExecuteVariant} drive a much larger
 * integration suite (workspace load/save, an external MADS assembler
 * invocation, reference-file comparison across notation variants) that
 * depends on {@code WorkspaceLogic} and other application-level pieces not
 * ported yet. This only runs the tests that exercise already-ported model
 * classes: {@link AssemblerTest}, {@link EquateTest}, {@link SegmentTest},
 * {@link DisassemblyResultTest}, {@link DisassemblyResultFileTest}, {@link
 * ComputerSystemTest} (which in turn covers {@code Atari800Test} and the
 * C64 system), {@link Profile1XTest}, {@link EquateListLogicTest}, {@link
 * MemoryInspectorTest}, and {@link AtariDiskImageTest} (the last two ported
 * from C++ test helpers with no independent entry point of their own - see
 * their own javadoc for how their concrete assertions were recovered from
 * {@code MainTest.cpp}), plus {@link MemoryInspectorStateTest} and {@link
 * ByteRangeSelectionTest} - new (not ported) tests, since the in-place
 * hex-editing and standalone byte-range-selection logic they cover has no
 * concrete assertions to recover from any C++ test helper. {@code
 * CommonTest}/{@code FileIOTest}/{@code
 * StreamTest} are not ported for the same "only called from MainTest"
 * reason and additionally only test C++-specific infrastructure classes
 * ({@code ByteArray}, {@code DatatypeUtility}, a custom {@code FileIO}
 * wrapper) this port does not have.
 * <p>
 * There is no JUnit (or other) test framework dependency: the offline Maven
 * repository this project builds against is missing the pieces Surefire
 * needs to actually run JUnit 5 (the {@code surefire-junit-platform}
 * provider and {@code junit-platform-launcher} jars), so this follows the
 * C++ project's own approach instead - a hand-rolled {@link
 * com.wudsn.tools.dis6502.model.Assert} plus a plain runner with a {@code
 * main} method, compiled via {@code mvn -o test-compile} and run directly
 * (e.g. {@code java -cp target/classes;target/test-classes;<wudsn-base
 * jars> com.wudsn.tools.dis6502.TestRunner}).
 *
 * @author Peter Dell
 */
public final class TestRunner {

	private int totalCount;
	private int failedCount;

	public static void main(String[] args) {
		TestRunner runner = new TestRunner();
		runner.execute();
		if (runner.failedCount > 0) {
			System.exit(1);
		}
	}

	private interface TestCase {
		void run() throws Exception;
	}

	public void execute() {
		log("INFO: Starting unit tests.");

		runTest("AssemblerTest", () -> AssemblerTest.testAssembler(new Workspace(new ComputerSystemFactory())));
		runTest("EquateTest", EquateTest::testEquate);
		runTest("SegmentTest", SegmentTest::testSegment);
		runTest("DisassemblyResultTest", DisassemblyResultTest::testDisassemblyResult);
		runTest("DisassemblyResultFileTest", DisassemblyResultFileTest::testDisassemblyResultFile);
		runTest("ComputerSystemTest", () -> ComputerSystemTest.testSystems(new ComputerSystemFactory()));
		runTest("Profile1XTest", Profile1XTest::testProfile1X);
		runTest("EquateListLogicTest", EquateListLogicTest::testEquateListLogic);
		runTest("MemoryInspectorTest", MemoryInspectorTest::testMemoryInspectorType);
		runTest("AtariDiskImageTest", AtariDiskImageTest::testAtariDiskImage);
		runTest("MemoryInspectorStateTest", MemoryInspectorStateTest::testMemoryInspectorState);
		runTest("ByteRangeSelectionTest", ByteRangeSelectionTest::testByteRangeSelection);
		runTest("DataTypesTest", DataTypesTest::testDataTypes);

		if (failedCount == 0) {
			log("INFO: All " + totalCount + " unit tests were successful.");
		} else {
			log("ERROR: " + failedCount + " of " + totalCount + " unit tests failed.");
		}
	}

	private void runTest(String name, TestCase testCase) {
		totalCount++;
		log("INFO: Running " + name + ".");
		try {
			testCase.run();
			log("INFO: " + name + " successful.");
		} catch (Throwable ex) {
			failedCount++;
			log("ERROR: " + name + " failed: " + ex);
		}
	}

	private static void log(String text) {
		System.out.println(text);
	}
}
