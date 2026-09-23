/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

/**
 * This port's {@link DisassemblySection} appends lines directly via
 * {@link DisassemblySection#addLine}, with no buffer indirection - see
 * its javadoc.
 *
 * @author Peter Dell
 */
public final class DisassemblyResultTest {

	private DisassemblyResultTest() {
	}

	public static void testDisassemblyResult() {
		DisassemblyResult result = new DisassemblyResult();

		Assert.longEquals(result.getSectionCount(), 4);
		Assert.longEquals(result.getLineCount(), 0);

		DisassemblySection section = result.allocSection(DisassemblySectionType.SYSTEM_EQUATES);
		DisassemblyLine templateLine = new DisassemblyLine(section);

		String systemEquateLine1 = "; First System Equate";
		section.addLine(templateLine, systemEquateLine1);
		Assert.longEquals(result.getLineCount(), 1);

		String systemEquateLine2 = "; Second System Equate";
		section.addLine(templateLine, systemEquateLine2);
		Assert.longEquals(result.getLineCount(), 2);

		section = result.allocSection(DisassemblySectionType.USER_EQUATES);
		String userEquateLine1 = "; First User Equate";
		section.addLine(templateLine, userEquateLine1);
		Assert.longEquals(result.getLineCount(), 3);

		String userEquateLine2 = "; Second User Equate";
		section.addLine(templateLine, userEquateLine2);
		Assert.longEquals(result.getLineCount(), 4);

		DisassemblyResult.LineIterator resultIterator = result.createLineIterator();

		Assert.boolEquals(resultIterator.hasNext(), true);
		DisassemblyLine line = resultIterator.next();
		Assert.longEquals(line.getLineNumber(), 1);
		Assert.stringEquals(line.getLine(), systemEquateLine1);

		Assert.boolEquals(resultIterator.hasNext(), true);
		line = resultIterator.next();
		Assert.longEquals(line.getLineNumber(), 2);
		Assert.stringEquals(line.getLine(), systemEquateLine2);

		Assert.boolEquals(resultIterator.hasNext(), true);
		line = resultIterator.next();
		Assert.longEquals(line.getLineNumber(), 3);
		Assert.stringEquals(line.getLine(), userEquateLine1);

		Assert.boolEquals(resultIterator.hasNext(), true);
		line = resultIterator.next();
		Assert.longEquals(line.getLineNumber(), 4);
		Assert.stringEquals(line.getLine(), userEquateLine2);

		Assert.boolEquals(resultIterator.hasNext(), false);
	}

	/** Search is case-insensitive - "lda" matches both "LDA #$01" and "lda #$02", but not "STA $D000". */
	public static void testFindAndSelectLines() {
		DisassemblyResult result = new DisassemblyResult();
		DisassemblySection section = result.allocSection(DisassemblySectionType.CODE_LINES);
		DisassemblyLine templateLine = new DisassemblyLine(section);

		section.addLine(templateLine, "LDA #$01");
		section.addLine(templateLine, "STA $D000");
		section.addLine(templateLine, "lda #$02");

		int[] findFirstLineNumber = { 0 };
		Assert.boolEquals(result.findAndSelectLines(true, findFirstLineNumber, "lda"), true);
		Assert.longEquals(findFirstLineNumber[0], 1);

		DisassemblyResult.LineIterator iterator = result.createLineIterator();
		DisassemblyLine line1 = iterator.next();
		Assert.longEquals(line1.xrefLineNumber, 1);
		Assert.boolEquals(line1.selected, true);

		DisassemblyLine line2 = iterator.next();
		Assert.longEquals(line2.xrefLineNumber, 0);
		Assert.boolEquals(line2.selected, false);

		DisassemblyLine line3 = iterator.next();
		Assert.longEquals(line3.xrefLineNumber, 2);
		Assert.boolEquals(line3.selected, false);

		// Find next, also case-insensitive, continues forward from line 1 to line 3.
		Assert.boolEquals(result.findAndSelectLines(false, findFirstLineNumber, "LDA"), true);
		Assert.longEquals(findFirstLineNumber[0], 3);
	}

	/** Fills every section of {@code result} with placeholder lines, for {@link DisassemblyResultFileTest}. */
	static void generateDisassemblyResult(DisassemblyResult result, int linesPerSection) {
		result.clear();
		int lineNumber = 1;
		for (DisassemblySectionType disassemblySectionType : result.getSectionTypes()) {
			DisassemblySection section = result.allocSection(disassemblySectionType);
			DisassemblyLine templateLine = new DisassemblyLine(section);
			for (int j = 0; j < linesPerSection && j < 0xc000; j++) {
				templateLine.address = j;
				templateLine.systemAddress = 0x1000 + j;
				String line = "; Line number = " + lineNumber + ", section "
						+ DisassemblySection.getText(disassemblySectionType) + ", address 0x" + Integer.toHexString(j);
				section.addLine(templateLine, line);
				lineNumber++;
			}
		}
	}
}
