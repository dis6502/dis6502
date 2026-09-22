/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

/**
 * New (not ported) test for {@link Disassembly#setImmediateType} and its
 * counterpart {@link Disassembly#isInstructionWithImmediate}.
 *
 * @author Peter Dell
 */
public final class ImmediateTypeTest {

	private ImmediateTypeTest() {
	}

	public static void testImmediateType() {
		Workspace workspace = new Workspace(new ComputerSystemFactory());
		workspace.setComputerSystemType(ComputerSystemType.ATARI800);
		Segment segment = workspace.getSegmentList().insertSegmentAt(0);
		segment.setHeader(FileHeader.ATARI_BINARY);
		segment.bBinary = true;
		segment.wBegin = 0x2000;
		segment.wEnd = 0x2004;
		segment.createMemoryBlockFromBeginToEnd();
		int[] program = { 0xA9, 0x41, // 2000 LDA #$41
				0x8D, 0x00, 0x30 }; // 2002 STA $3000
		for (int i = 0; i < program.length; i++) {
			segment.setData(i, program[i]);
		}
		segment.setType(0, MemoryType.CODE, program.length);

		// Not an immediate-mode instruction: nothing reported, nothing changed.
		Assert.boolEquals(getImmediateType(workspace, 2) == null, true);
		Assert.boolEquals(Disassembly.setImmediateType(workspace, 0, 2, MemoryType.STRING, 0), false);
		Assert.boolEquals(segment.getType(3) == MemoryType.CODE, true);

		Assert.boolEquals(getImmediateType(workspace, 0) == MemoryType.CODE, true);

		// Low byte: the marker goes on the opcode, the other half of the address into the operand's type slot.
		Assert.boolEquals(Disassembly.setImmediateType(workspace, 0, 0, MemoryType.LOBYTE, 0x30), true);
		Assert.boolEquals(segment.getType(0) == MemoryType.LOBYTE, true);
		Assert.longEquals(segment.memoryBlock.getType()[1] & 0xFF, 0x30);
		Assert.boolEquals(getImmediateType(workspace, 0) == MemoryType.LOBYTE, true);

		// High byte -> char constant: the marker must come off the opcode again.
		Assert.boolEquals(Disassembly.setImmediateType(workspace, 0, 0, MemoryType.HIBYTE, 0x12), true);
		Assert.boolEquals(getImmediateType(workspace, 0) == MemoryType.HIBYTE, true);
		Assert.boolEquals(Disassembly.setImmediateType(workspace, 0, 0, MemoryType.STRING, 0), true);
		Assert.boolEquals(segment.getType(0) == MemoryType.CODE, true);
		Assert.boolEquals(segment.getType(1) == MemoryType.STRING, true);
		Assert.boolEquals(getImmediateType(workspace, 0) == MemoryType.STRING, true);

		// Back to plain code, then unknown: both bytes.
		Assert.boolEquals(Disassembly.setImmediateType(workspace, 0, 0, MemoryType.CODE, 0), true);
		Assert.boolEquals(segment.getType(0) == MemoryType.CODE && segment.getType(1) == MemoryType.CODE, true);
		Assert.boolEquals(Disassembly.setImmediateType(workspace, 0, 0, MemoryType.UNKNOWN, 0), true);
		Assert.boolEquals(segment.getType(0) == MemoryType.UNKNOWN && segment.getType(1) == MemoryType.UNKNOWN, true);

		// A type no immediate operand can have.
		Assert.boolEquals(Disassembly.setImmediateType(workspace, 0, 0, MemoryType.WORD, 0), false);

		Assert.log("ImmediateTypeTest completed");
	}

	private static MemoryType getImmediateType(Workspace workspace, int offset) {
		MemoryType[] type = new MemoryType[1];
		return Disassembly.isInstructionWithImmediate(workspace, 0, offset, new int[1], type) ? type[0] : null;
	}
}
