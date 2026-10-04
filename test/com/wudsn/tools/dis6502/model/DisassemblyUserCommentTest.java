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
 * looks the comment up.
 *
 * @author Peter Dell
 */
public final class DisassemblyUserCommentTest {

	private DisassemblyUserCommentTest() {
	}

	public static void testUserComments() {
		Workspace workspace = new Workspace(new ComputerSystemFactory());
		workspace.setComputerSystemType(ComputerSystemType.ATARI800);
		addCodeSegment(workspace, 0, 0x2000, new int[] { 0xEA, 0xEA }, "First of A", "Last of A"); // NOP, NOP
		addCodeSegment(workspace, 1, 0x3000, new int[] { 0xEA, 0x60 }, "First of B", null); // NOP, RTS

		Disassembly disassembly = new Disassembly();
		disassembly.setWorkspace(workspace);
		DisassemblyProgressMonitor monitor = new DisassemblyProgressMonitor(new Application());
		disassembly.setProgressMonitor(monitor);
		monitor.startDisassembly(disassembly);

		List<String> lines = new ArrayList<>();
		for (DisassemblyResult.LineIterator i = workspace.getDisassemblyResult().createLineIterator(); i.hasNext();) {
			lines.add(i.next().getLine());
		}
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

	private static void addCodeSegment(Workspace workspace, int index, int address, int[] code, String firstComment,
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
