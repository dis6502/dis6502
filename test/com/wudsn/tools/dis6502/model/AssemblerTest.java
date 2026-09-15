/*
 * Copyright (C) 2025 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

/**
 * Ported from AssemblerTest.h / AssemblerTest.cpp.
 *
 * @author Peter Dell
 */
public final class AssemblerTest {

	private AssemblerTest() {
	}

	private static void assertParsesTo(Workspace workspace, String line, int address, String expectedInstructionName,
			OperandMode expectedOperandMode, int expectedValue) {
		assertParsesTo(workspace, line, address, expectedInstructionName, expectedOperandMode, expectedValue, "");
	}

	private static void assertParsesTo(Workspace workspace, String line, int address, String expectedInstructionName,
			OperandMode expectedOperandMode, int expectedValue, String expectedComment) {
		Assembler.Result result = Assembler.parseLine(workspace, line, address, ProcessorType.MOS6502);

		if (!result.error.isEmpty()) {
			Assert.fail("Line '" + line + "' failed to parse: " + result.error);
		}
		if (result.instruction == null) {
			Assert.fail("Line '" + line + "' did not match any instruction.");
		}

		Assert.stringEquals(result.instruction.getName(), expectedInstructionName);
		Assert.boolEquals(result.instruction.getOperandMode() == expectedOperandMode, true);
		Assert.longEquals(result.value, expectedValue);
		Assert.stringEquals(result.comment, expectedComment);
	}

	private static void assertUnknownInstruction(Workspace workspace, String line, int address) {
		Assembler.Result result = Assembler.parseLine(workspace, line, address, ProcessorType.MOS6502);

		Assert.boolEquals(result.error.isEmpty(), true);
		Assert.isNull(result.instruction);
	}

	public static void testAssembler(Workspace workspace) {
		testAddressingModes(workspace);
		testLabels(workspace);
		testComments(workspace);
		testErrors(workspace);
	}

	private static void testAddressingModes(Workspace workspace) {
		assertParsesTo(workspace, "NOP", 0x0600, "NOP", OperandMode.Implied, 0);
		assertParsesTo(workspace, "ASL A", 0x0600, "ASL", OperandMode.Accumulator, 0);
		assertParsesTo(workspace, "LDA #$10", 0x0600, "LDA", OperandMode.Immediate, 0x10);
		assertParsesTo(workspace, "LDA $10", 0x0600, "LDA", OperandMode.ZeroPage, 0x10);
		assertParsesTo(workspace, "LDA $1000", 0x0600, "LDA", OperandMode.Absolute, 0x1000);
		assertParsesTo(workspace, "LDA $10,X", 0x0600, "LDA", OperandMode.ZeroPageX, 0x10);
		assertParsesTo(workspace, "LDA $1000,X", 0x0600, "LDA", OperandMode.AbsoluteX, 0x1000);
		assertParsesTo(workspace, "LDX $10,Y", 0x0600, "LDX", OperandMode.ZeroPageY, 0x10);
		assertParsesTo(workspace, "LDA $1000,Y", 0x0600, "LDA", OperandMode.AbsoluteY, 0x1000);
		assertParsesTo(workspace, "JMP ($1000)", 0x0600, "JMP", OperandMode.Indirect, 0x1000);
		assertParsesTo(workspace, "LDA ($10,X)", 0x0600, "LDA", OperandMode.IndexedIndirect, 0x10);
		assertParsesTo(workspace, "LDA ($10),Y", 0x0600, "LDA", OperandMode.IndirectIndexed, 0x10);

		// BEQ $0610 from address $0600: target - address - 2 = $0610 - $0600 - 2 = $0E.
		assertParsesTo(workspace, "BEQ $0610", 0x0600, "BEQ", OperandMode.Relative, 0x0E);
	}

	private static void testLabels(Workspace workspace) {
		workspace.getUserEquateList().addEquate("COUNT = $0080");

		assertParsesTo(workspace, "LDA COUNT", 0x0600, "LDA", OperandMode.ZeroPage, 0x0080);

		workspace.getUserEquateList().clear();
	}

	private static void testComments(Workspace workspace) {
		// ';' delimits a comment for every addressing mode, not just Immediate.
		assertParsesTo(workspace, "LDA #$10 ; load count", 0x0600, "LDA", OperandMode.Immediate, 0x10, "; load count");
		assertParsesTo(workspace, "LDA #$10", 0x0600, "LDA", OperandMode.Immediate, 0x10, "");
		assertParsesTo(workspace, "LDA #$10   ", 0x0600, "LDA", OperandMode.Immediate, 0x10, ""); // trailing whitespace is trimmed away
		assertParsesTo(workspace, "LDA $10 ; a zero page comment", 0x0600, "LDA", OperandMode.ZeroPage, 0x10,
				"; a zero page comment");
		assertParsesTo(workspace, "NOP ; implied comment", 0x0600, "NOP", OperandMode.Implied, 0, "; implied comment");

		// Genuine trailing garbage (no ';') still fails to parse, and there is no comment.
		Assembler.Result result = Assembler.parseLine(workspace, "LDA $10 XYZ", 0x0600, ProcessorType.MOS6502);
		Assert.boolEquals(result.error.isEmpty(), false);
		Assert.stringEquals(result.comment, "");
	}

	private static void testErrors(Workspace workspace) {
		assertUnknownInstruction(workspace, "XYZ", 0x0600);
	}
}
