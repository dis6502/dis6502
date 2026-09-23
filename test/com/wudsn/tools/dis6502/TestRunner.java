/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import com.wudsn.tools.dis6502.model.AssemblerTest;
import com.wudsn.tools.dis6502.model.ByteRangeSelectionTest;
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
import com.wudsn.tools.dis6502.model.system.ComputerSystemFactory;
import com.wudsn.tools.dis6502.model.system.ComputerSystemTest;
import com.wudsn.tools.dis6502.model.system.atari800.AtariDiskImageTest;
import com.wudsn.tools.dis6502.ui.DialogTextsTest;
import com.wudsn.tools.dis6502.ui.FileChoosersTest;
import com.wudsn.tools.dis6502.ui.PanelTextsTest;
import com.wudsn.tools.dis6502.ui.PopupStructureTest;
import com.wudsn.tools.dis6502.ui.RenderingTest;
import com.wudsn.tools.dis6502.ui.ValueSetsTest;

/**
 * Runs the unit tests and reports a pass/fail summary.
 * <p>
 * A minimal aggregator: the "run everything once, report pass/fail" tool
 * a console mode would otherwise provide - repeated-relaunch soak-testing
 * has no equivalent here (see gap #5's history in {@code
 * plans/REMAINING_GAPS_OVERVIEW.md}). Its two most substantial checks are
 * {@link WorkspaceLogicTest} (a real workspace-file load plus {@link
 * com.wudsn.tools.dis6502.model.Segment#splitAt}'s comment-rebasing) and
 * {@link ReassemblyRoundTripTest} (disassemble four real fixtures,
 * reassemble each with the real MADS assembler, and byte-compare against
 * a reference binary; see its own javadoc for the deliberately reduced
 * variant-sweep scope). This also runs the tests that exercise every
 * other model class: {@link AssemblerTest}, {@link EquateTest}, {@link
 * SegmentTest}, {@link DisassemblyResultTest}, {@link
 * DisassemblyResultFileTest}, {@link ComputerSystemTest} (which in turn
 * covers {@code Atari800Test} and the C64 system), {@link Profile1XTest},
 * {@link EquateListLogicTest}, {@link MemoryInspectorTest}, and {@link
 * AtariDiskImageTest} (the last two with no independent entry point of
 * their own - see their own javadoc), plus {@link
 * MemoryInspectorStateTest} and {@link ByteRangeSelectionTest} for the
 * in-place hex-editing and standalone byte-range-selection logic.
 * <p>
 * There is no JUnit (or other) test framework dependency: the offline Maven
 * repository this project builds against is missing the pieces Surefire
 * needs to actually run JUnit 5 (the {@code surefire-junit-platform}
 * provider and {@code junit-platform-launcher} jars), so this uses a
 * hand-rolled {@link com.wudsn.tools.dis6502.model.Assert} plus a plain
 * runner with a {@code main} method, compiled via {@code mvn -o
 * test-compile} and run directly (e.g. {@code java -cp
 * target/classes;target/test-classes;<wudsn-base jars>
 * com.wudsn.tools.dis6502.TestRunner}).
 *
 * @author Peter Dell
 */
public final class TestRunner {

	private int totalCount;
	private int failedCount;

	/** The settings node every test - including the real application some of them start - writes to; removed again afterwards. */
	private static final String SETTINGS_NODE = "test";

	public static void main(String[] args) {
		if (run() > 0) {
			System.exit(1);
		}
	}

	/** Runs the whole suite with its settings isolated and returns the number of failed tests - what {@code main} and {@link TestRunnerTest} share. */
	public static int run() {
		System.setProperty(Application.SETTINGS_NODE_PROPERTY, SETTINGS_NODE);
		TestRunner runner = new TestRunner();
		try {
			runner.execute();
		} finally {
			removeTestSettings();
		}
		return runner.failedCount;
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
		runTest("DisassemblyResultTest.testFindAndSelectLines", DisassemblyResultTest::testFindAndSelectLines);
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
		runTest("PanelTextsTest", PanelTextsTest::testPanelTexts);
		runTest("DialogTextsTest", DialogTextsTest::testDialogTexts);
		runTest("RenderingTest", RenderingTest::testRendering);
		runTest("PopupStructureTest", PopupStructureTest::testPopupStructure);
		runTest("UIWiringTest", UIWiringTest::testUIWiring);

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
