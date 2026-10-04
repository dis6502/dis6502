/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

import java.util.ArrayList;
import java.util.List;

import com.wudsn.tools.dis6502.Application;
import com.wudsn.tools.dis6502.model.system.ComputerSystemFactory;
import com.wudsn.tools.dis6502.model.system.ComputerSystemType;

/**
 * User comments appear in the listing before the instruction at their offset
 * - also on the last byte of a segment that another binary segment follows,
 * where the disassembler has already moved on to the next segment when it
 * looks the comment up, and on an instruction's operand bytes, which are
 * written before the instruction together with its opcode byte's comment -
 * for CPU instructions and for the address of a display list instruction.
 *
 * @author Peter Dell
 */
public final class DisassemblyUserCommentTest {

	private DisassemblyUserCommentTest() {
	}

	public static void testUserComments() {
		testSegmentEnd();
		testOperandBytes();
		testDisplayListAddress();
	}

	private static void testSegmentEnd() {
		Workspace workspace = new Workspace(new ComputerSystemFactory());
		workspace.setComputerSystemType(ComputerSystemType.ATARI800);
		addCodeSegment(workspace, 0, 0x2000, new int[] { 0xEA, 0xEA }, "First of A", "Last of A"); // NOP, NOP
		addCodeSegment(workspace, 1, 0x3000, new int[] { 0xEA, 0x60 }, "First of B", null); // NOP, RTS

		List<String> lines = disassemble(workspace);
		int firstOfA = indexOf(lines, "First of A");
		int lastOfA = indexOf(lines, "Last of A");
		int firstOfB = indexOf(lines, "First of B");
		Assert.boolEquals(firstOfA >= 0, true);
		Assert.boolEquals(lastOfA > firstOfA, true);
		Assert.boolEquals(firstOfB > lastOfA, true);
		// Each comment directly precedes its instruction.
		Assert.boolEquals(lines.get(firstOfA + 1).contains("NOP") || lines.get(firstOfA + 1).contains("nop"), true);
		Assert.boolEquals(lines.get(lastOfA + 1).contains("NOP") || lines.get(lastOfA + 1).contains("nop"), true);
	}

	/** LDA $1234, RTS: comments on both operand bytes come before the LDA, in byte order, after the opcode's. */
	private static void testOperandBytes() {
		Workspace workspace = new Workspace(new ComputerSystemFactory());
		workspace.setComputerSystemType(ComputerSystemType.ATARI800);
		Segment segment = addCodeSegment(workspace, 0, 0x2000, new int[] { 0xAD, 0x34, 0x12, 0x60 }, "On the opcode",
				"On the RTS");
		addComment(segment, 2, "On the high byte");
		addComment(segment, 1, "On the low byte");

		List<String> lines = disassemble(workspace);
		int opcode = indexOf(lines, "On the opcode");
		Assert.boolEquals(opcode >= 0, true);
		Assert.longEquals(indexOf(lines, "On the low byte"), opcode + 1);
		Assert.longEquals(indexOf(lines, "On the high byte"), opcode + 2);
		Assert.boolEquals(lines.get(opcode + 3).toUpperCase().contains("LDA"), true);
		Assert.longEquals(indexOf(lines, "On the RTS"), opcode + 4);
	}

	/**
	 * A display list - 8 blank lines, LMS mode 2 at $3000, jump and wait for
	 * VBLANK to $2000: each address is one word line, with the comments of
	 * both its bytes directly before it.
	 */
	private static void testDisplayListAddress() {
		Workspace workspace = new Workspace(new ComputerSystemFactory());
		workspace.setComputerSystemType(ComputerSystemType.ATARI800);
		int[] displayList = { 0x70, 0x42, 0x00, 0x30, 0x41, 0x00, 0x20 };
		Segment segment = addCodeSegment(workspace, 0, 0x2000, displayList, "On the blank lines", null);
		segment.setType(0, MemoryType.DLIST, displayList.length);
		addComment(segment, 1, "On the LMS");
		addComment(segment, 2, "On the LMS low byte");
		addComment(segment, 3, "On the LMS high byte");
		addComment(segment, 5, "On the jump low byte");
		addComment(segment, 6, "On the jump high byte");

		List<String> lines = disassemble(workspace);
		int lms = indexOf(lines, "On the LMS low byte");
		Assert.boolEquals(lms > indexOf(lines, "On the LMS"), true);
		Assert.longEquals(indexOf(lines, "On the LMS high byte"), lms + 1);
		Assert.boolEquals(lines.get(lms + 2).contains("3000"), true);

		int jump = indexOf(lines, "On the jump low byte");
		Assert.boolEquals(jump > lms, true);
		Assert.longEquals(indexOf(lines, "On the jump high byte"), jump + 1);
		Assert.boolEquals(lines.get(jump + 2).contains("2000"), true);
	}

	private static List<String> disassemble(Workspace workspace) {
		Disassembly disassembly = new Disassembly();
		disassembly.setWorkspace(workspace);
		DisassemblyProgressMonitor monitor = new DisassemblyProgressMonitor(new Application());
		disassembly.setProgressMonitor(monitor);
		monitor.startDisassembly(disassembly);

		List<String> lines = new ArrayList<>();
		for (DisassemblyResult.LineIterator i = workspace.getDisassemblyResult().createLineIterator(); i.hasNext();) {
			lines.add(i.next().getLine());
		}
		return lines;
	}

	private static Segment addCodeSegment(Workspace workspace, int index, int address, int[] code, String firstComment,
			String lastComment) {
		Segment segment = workspace.getSegmentList().insertSegmentAt(index);
		segment.setHeader(FileHeader.ATARI_BINARY);
		segment.bBinary = true;
		segment.wBegin = address;
		segment.wEnd = address + code.length - 1;
		segment.createMemoryBlockFromBeginToEnd();
		for (int i = 0; i < code.length; i++) {
			segment.setData(i, code[i]);
		}
		segment.setType(0, MemoryType.CODE, code.length);
		addComment(segment, 0, firstComment);
		addComment(segment, code.length - 1, lastComment);
		return segment;
	}

	private static void addComment(Segment segment, int offset, String text) {
		if (text != null) {
			Comment comment = segment.allocateComment();
			comment.setOffset(offset);
			comment.setText(text);
		}
	}

	private static int indexOf(List<String> lines, String text) {
		for (int i = 0; i < lines.size(); i++) {
			if (lines.get(i).contains(text)) {
				return i;
			}
		}
		return -1;
	}
}
