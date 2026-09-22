/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

import com.wudsn.tools.dis6502.Application;

/**
 * New (not ported): each of the four notation settings of a {@link Profile}
 * that C++'s {@code MainTest::ExecuteVariant} swept in 16 combinations
 * must change the listing the way it says - checked on the listing text,
 * which a reassembly round trip cannot do for a comment or a string
 * constant (they leave the bytes alone). {@link ReassemblyRoundTripTest}
 * covers "and it still assembles" for the flags that change operands.
 *
 * @author Peter Dell
 */
public final class ProfileNotationTest {

	private ProfileNotationTest() {
	}

	public static void testProfileNotation() {
		// Defaults: hex, ZP absolute forced with ".w", non-ASCII as bytes, no opcode comment.
		List<String> lines = disassemble(profile -> {
		});
		Assert.stringEquals(lines.get(0), "org $2000");
		Assert.stringEquals(lines.get(1), "lda #$0C");
		Assert.stringEquals(lines.get(2), "lda.w L0080");
		Assert.stringEquals(lines.get(5), ".byte 'HI'");
		Assert.stringEquals(lines.get(6), ".byte $7E");

		// useHexNotation off: every number decimal.
		lines = disassemble(profile -> profile.useHexNotation = false);
		Assert.stringEquals(lines.get(0), "org 8192");
		Assert.stringEquals(lines.get(1), "lda #12");
		Assert.stringEquals(lines.get(6), ".byte 126");

		// showZPAbsoluteAsByte: an absolute instruction on a zero page address becomes its
		// three bytes (so an assembler cannot shorten it), the instruction as a comment.
		lines = disassemble(profile -> profile.showZPAbsoluteAsByte = true);
		Assert.stringEquals(lines.get(2), ".byte $AD,$80,$00      ; lda L0080"); // The comment starts at column 34.

		// showNonASCIIChararactersAsBytes off: '~' ($7E) joins the string constant.
		lines = disassemble(profile -> profile.showNonASCIIChararactersAsBytes = false);
		Assert.stringEquals(lines.get(5), ".byte 'HI~'");
		Assert.longEquals(lines.size(), 6);

		// showOpcodeAsComment: the instruction's bytes trail every code line.
		lines = disassemble(profile -> profile.showOpcodeAsComment = true);
		Assert.stringEquals(lines.get(1), "lda #$0C              ; $A9 $0C");
		Assert.stringEquals(lines.get(2), "lda.w L0080           ; $AD $80 $00");
		Assert.stringEquals(lines.get(4), "rts                   ; $60");
		Assert.stringEquals(lines.get(5), ".byte 'HI'"); // Data lines get none.

		Assert.log("ProfileNotationTest completed");
	}

	/**
	 * Disassembles a small program - an immediate, an absolute instruction
	 * on a zero page address, a plain absolute store, RTS, and a string with
	 * one non-ASCII character - with the given profile changes, and returns
	 * the code lines, trimmed, without blank and comment lines.
	 */
	private static List<String> disassemble(Consumer<Profile> profileChanges) {
		Workspace workspace = new Workspace(new ComputerSystemFactory());
		workspace.setComputerSystemType(ComputerSystemType.ATARI800);
		profileChanges.accept(workspace.getProfile());

		Segment segment = workspace.getSegmentList().insertSegmentAt(0);
		segment.setHeader(FileHeader.ATARI_BINARY);
		segment.bBinary = true;
		segment.wBegin = 0x2000;
		segment.wEnd = 0x200B;
		segment.createMemoryBlockFromBeginToEnd();
		int[] program = { 0xA9, 0x0C, // 2000 LDA #$0C
				0xAD, 0x80, 0x00, // 2002 LDA $0080 (absolute mode on a zero page address)
				0x8D, 0x00, 0x30, // 2005 STA $3000
				0x60, // 2008 RTS
				0x48, 0x49, 0x7E }; // 2009 "HI~"
		for (int i = 0; i < program.length; i++) {
			segment.setData(i, program[i]);
		}
		segment.setType(0, MemoryType.CODE, 9);
		segment.setType(9, MemoryType.STRING, 3);

		Disassembly disassembly = new Disassembly();
		disassembly.setWorkspace(workspace);
		DisassemblyProgressMonitor monitor = new DisassemblyProgressMonitor(new Application());
		disassembly.setProgressMonitor(monitor);
		monitor.startDisassembly(disassembly);

		List<String> lines = new ArrayList<>();
		for (Iterator<DisassemblyLine> i = workspace.getDisassemblyResult().createLineIterator(DisassemblySectionType.CODE_LINES); i
				.hasNext();) {
			String line = i.next().getLine().trim();
			if (!line.isEmpty() && !line.startsWith(";")) {
				lines.add(line);
			}
		}
		return lines;
	}
}
