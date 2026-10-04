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
 * A user equate at a code address is defined once, by its line in the user
 * equates section, and only referenced in the code: defining it again as the
 * label of the instruction at its address made the listing fail to assemble
 * ("Label declared twice"). An address label without user equate is still
 * defined at its instruction.
 *
 * @author Peter Dell
 */
public final class DisassemblyLabelTest {

	private DisassemblyLabelTest() {
	}

	public static void testLabels() {
		int[] code = { 0x20, 0x06, 0x20, // 2000 JSR $2006 (user equate MYSUB)
				0x20, 0x07, 0x20, // 2003 JSR $2007 (address label L2007)
				0x60, // 2006 RTS
				0x60 }; // 2007 RTS
		Workspace workspace = new Workspace(new ComputerSystemFactory());
		workspace.setComputerSystemType(ComputerSystemType.ATARI800);
		Segment segment = workspace.getSegmentList().insertSegmentAt(0);
		segment.setHeader(FileHeader.ATARI_BINARY);
		segment.bBinary = true;
		segment.wBegin = 0x2000;
		segment.wEnd = 0x2000 + code.length - 1;
		segment.createMemoryBlockFromBeginToEnd();
		for (int i = 0; i < code.length; i++) {
			segment.setData(i, code[i]);
		}
		segment.setType(0, MemoryType.CODE, code.length);
		workspace.getUserEquateList().addEquate("MYSUB = $2006");

		Disassembly disassembly = new Disassembly();
		disassembly.setWorkspace(workspace);
		DisassemblyProgressMonitor monitor = new DisassemblyProgressMonitor(new Application());
		disassembly.setProgressMonitor(monitor);
		monitor.startDisassembly(disassembly);

		List<String> codeLines = new ArrayList<>();
		int myDefinitions = 0;
		for (DisassemblyResult.LineIterator i = workspace.getDisassemblyResult().createLineIterator(); i.hasNext();) {
			DisassemblyLine line = i.next();
			String text = line.getLine();
			if (text.startsWith("MYSUB")) {
				myDefinitions++;
			}
			if (line.getSection().getType() == DisassemblySectionType.CODE_LINES && !text.trim().isEmpty()
					&& !text.trim().startsWith(";") && !text.trim().startsWith("org")) {
				codeLines.add(text.trim().replaceAll("\\s+", " "));
			}
		}
		Assert.longEquals(myDefinitions, 1); // Only "MYSUB equ $2006" in the user equates.
		Assert.stringEquals(String.join(" | ", codeLines), "jsr MYSUB | jsr L2007 | rts | L2007 rts");
	}
}
