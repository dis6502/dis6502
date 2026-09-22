/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import com.wudsn.tools.dis6502.model.AssemblerTest;
import com.wudsn.tools.dis6502.model.AtariDiskImageTest;
import com.wudsn.tools.dis6502.model.ByteRangeSelectionTest;
import com.wudsn.tools.dis6502.model.ComputerSystemFactory;
import com.wudsn.tools.dis6502.model.ComputerSystemTest;
import com.wudsn.tools.dis6502.model.DisassemblyResultFileTest;
import com.wudsn.tools.dis6502.model.DisassemblyResultTest;
import com.wudsn.tools.dis6502.model.EncodingTest;
import com.wudsn.tools.dis6502.model.EquateListLogicTest;
import com.wudsn.tools.dis6502.model.EquateTest;
import com.wudsn.tools.dis6502.model.FolderTypeTest;
import com.wudsn.tools.dis6502.model.ImmediateTypeTest;
import com.wudsn.tools.dis6502.model.LineNumberHistoryTest;
import com.wudsn.tools.dis6502.model.MemoryInspectorStateTest;
import com.wudsn.tools.dis6502.model.MemoryInspectorTest;
import com.wudsn.tools.dis6502.model.Profile1XTest;
import com.wudsn.tools.dis6502.model.ProfileNotationTest;
import com.wudsn.tools.dis6502.model.ReassemblyRoundTripTest;
import com.wudsn.tools.dis6502.model.SegmentTest;
import com.wudsn.tools.dis6502.model.Workspace;
import com.wudsn.tools.dis6502.model.WorkspaceLogicTest;
import com.wudsn.tools.dis6502.ui.DialogTextsTest;
import com.wudsn.tools.dis6502.ui.FileChoosersTest;
import com.wudsn.tools.dis6502.ui.ValueSetsTest;

/**
 * Runs the ported unit tests and reports a pass/fail summary.
 * <p>
 * This is a new, minimal aggregator - not a port of {@code
 * MainUITest.h}/{@code .cpp} (the console-mode {@code /TEST:DEV|FAST|
 * NORMAL|DEEP} self-test harness) or of most of {@code MainTest.h}/{@code
 * .cpp} it delegates to. That harness itself is deliberately not ported -
 * see gap #5's history in {@code plans/REMAINING_GAPS_OVERVIEW.md}: this
 * class already is the "run everything once, report pass/fail" tool a
 * console mode would otherwise provide, and the repeated-relaunch soak-test
 * loop {@code test-dis6502-DEEP.bat}/{@code -FAST.bat} drove has no obvious
 * Java equivalent need. What was recovered from {@code MainTest.cpp}
 * instead is its two genuinely non-redundant checks: {@link
 * WorkspaceLogicTest} (from {@code MainTest::TestWorkspace} - a real
 * workspace-file load plus {@link com.wudsn.tools.dis6502.model.Segment#splitAt}'s
 * comment-rebasing, previously untested in Java) and {@link
 * ReassemblyRoundTripTest} (from {@code MainTest::ExecuteUnitTestItem}/{@code
 * ExecuteVariant} - disassemble four real fixtures, reassemble each with the
 * real MADS assembler, and byte-compare against a reference binary; see its
 * own javadoc for the deliberately reduced variant-sweep scope). This also
 * runs the tests that exercise every other already-ported model class:
 * {@link AssemblerTest}, {@link EquateTest}, {@link SegmentTest}, {@link
 * DisassemblyResultTest}, {@link DisassemblyResultFileTest}, {@link
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

	/** The settings node every test - including the real application some of them start - writes to; removed again afterwards. */
	private static final String SETTINGS_NODE = "test";

	public static void main(String[] args) {
		System.setProperty(Application.SETTINGS_NODE_PROPERTY, SETTINGS_NODE);
		TestRunner runner = new TestRunner();
		try {
			runner.execute();
		} finally {
			removeTestSettings();
		}
		if (runner.failedCount > 0) {
			System.exit(1);
		}
	}

	/** Leaves the user's settings as they were: whatever the tests stored under the test node is dropped. */
	private static void removeTestSettings() {
		try {
			Preferences root = Preferences.userNodeForPackage(Application.class);
			if (root.nodeExists(SETTINGS_NODE)) {
				root.node(SETTINGS_NODE).removeNode();
				root.flush();
			}
		} catch (BackingStoreException ex) {
			log("ERROR: Could not remove the test settings: " + ex);
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
		runTest("SegmentTest.testSegmentRangeEdit", SegmentTest::testSegmentRangeEdit);
		runTest("DisassemblyResultTest", DisassemblyResultTest::testDisassemblyResult);
		runTest("DisassemblyResultFileTest", DisassemblyResultFileTest::testDisassemblyResultFile);
		runTest("ComputerSystemTest", () -> ComputerSystemTest.testSystems(new ComputerSystemFactory()));
		runTest("Profile1XTest", Profile1XTest::testProfile1X);
		runTest("EquateListLogicTest", EquateListLogicTest::testEquateListLogic);
		runTest("WorkspaceLogicTest", WorkspaceLogicTest::testWorkspaceLogic);
		runTest("MemoryInspectorTest", MemoryInspectorTest::testMemoryInspectorType);
		runTest("AtariDiskImageTest", AtariDiskImageTest::testAtariDiskImage);
		runTest("MemoryInspectorStateTest", MemoryInspectorStateTest::testMemoryInspectorState);
		runTest("ByteRangeSelectionTest", ByteRangeSelectionTest::testByteRangeSelection);
		runTest("DataTypesTest", DataTypesTest::testDataTypes);
		runTest("CommandLineArgumentsTest", CommandLineArgumentsTest::testCommandLineArguments);
		runTest("LineNumberHistoryTest", LineNumberHistoryTest::testLineNumberHistory);
		runTest("ImmediateTypeTest", ImmediateTypeTest::testImmediateType);
		runTest("FileChoosersTest", FileChoosersTest::testFileChoosers);
		runTest("EncodingTest", EncodingTest::testEncoding);
		runTest("FolderTypeTest", FolderTypeTest::testFolderType);
		runTest("ValueSetsTest", ValueSetsTest::testValueSets);
		runTest("ProfileNotationTest", ProfileNotationTest::testProfileNotation);
		runTest("ReassemblyRoundTripTest", ReassemblyRoundTripTest::testReassemblyRoundTrip);
		runTest("DialogTextsTest", DialogTextsTest::testDialogTexts);

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
