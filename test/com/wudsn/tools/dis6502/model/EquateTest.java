/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

/**
 * Ported from EquateTest.h / EquateTest.cpp. {@code TestFiles} is not
 * ported - the C++ version's body is entirely commented out.
 *
 * @author Peter Dell
 */
public final class EquateTest {

	private EquateTest() {
	}

	public static void testEquate() {
		testParsing();
	}

	private static void testParsing() {

		// Success cases.
		assertEquateEquals("", EquateType.EMPTY, "", LabelAccess.UNKNOWN, 0, "", "");
		assertEquateEquals(" ;Comment", EquateType.COMMENT, "", LabelAccess.UNKNOWN, 0, "Comment", "");
		assertEquateEquals("DECIMAL = 1234  ; Comment", EquateType.LABEL, "DECIMAL", LabelAccess.READ_WRITE, 1234,
				"Comment", "");
		assertEquateEquals("HEX = $1234 ; Comment", EquateType.LABEL, "HEX", LabelAccess.READ_WRITE, 0x1234,
				"Comment", "");
		assertEquateEquals("HEX+1 = $1234 ; Comment", EquateType.LABEL, "HEX+1", LabelAccess.READ_WRITE, 0x1234,
				"Comment", "");
		assertEquateEquals("HEX-12 = $1234 ; Comment", EquateType.LABEL, "HEX-12", LabelAccess.READ_WRITE, 0x1234,
				"Comment", "");

		// Trailing comment.
		assertEquateEquals("DECIMAL = 1234", EquateType.LABEL, "DECIMAL", LabelAccess.READ_WRITE, 1234, "", "");
		assertEquateEquals("DECIMAL = 1234;", EquateType.LABEL, "DECIMAL", LabelAccess.READ_WRITE, 1234, "", "");
		assertEquateEquals("DECIMAL = 1234 ;", EquateType.LABEL, "DECIMAL", LabelAccess.READ_WRITE, 1234, "", "");

		// Access qualifier.
		assertEquateEquals("DECIMAL < 1234  ; Comment", EquateType.LABEL, "DECIMAL", LabelAccess.READ, 1234,
				"Comment", "");
		assertEquateEquals("DECIMAL > 1234  ; Comment", EquateType.LABEL, "DECIMAL", LabelAccess.WRITE, 1234,
				"Comment", "");
		assertEquateEquals("DECIMAL # 1234  ; Comment", EquateType.LABEL, "DECIMAL", LabelAccess.IMMEDIATE, 1234,
				"Comment", "");

		// Error cases.
		assertEquateEquals(" = 123", EquateType.UNKNOWN, "", LabelAccess.UNKNOWN, 0, "",
				"Character '=' at position 1 is not a valid start character for a label name.");

		assertEquateEquals("DECIMAL", EquateType.LABEL, "DECIMAL", LabelAccess.UNKNOWN, 0, "",
				"No access qualifier specified.");
		assertEquateEquals("DECIMAL * ", EquateType.LABEL, "DECIMAL", LabelAccess.UNKNOWN, 0, "",
				"Character '*' at position 9 is not an access qualifier. Use '=', '<', '>' or '#'.");
		assertEquateEquals("DECIMAL = ", EquateType.LABEL, "DECIMAL", LabelAccess.READ_WRITE, 0, "",
				"No value specified.");
		assertEquateEquals("DECIMAL = ; Comment", EquateType.LABEL, "DECIMAL", LabelAccess.READ_WRITE, 0, "",
				"Characters '; Comment' at position 11 cannot be interpreted as a decimal number.");
		assertEquateEquals("DECIMAL = abc", EquateType.LABEL, "DECIMAL", LabelAccess.READ_WRITE, 0, "",
				"Characters 'abc' at position 11 cannot be interpreted as a decimal number.");
		assertEquateEquals("HEX = $; Comment", EquateType.LABEL, "HEX", LabelAccess.READ_WRITE, 0, "",
				"Characters '; Comment' at position 8 cannot be interpreted as a hexadecimal number.");
		assertEquateEquals("HEX = $xyz", EquateType.LABEL, "HEX", LabelAccess.READ_WRITE, 0, "",
				"Characters 'xyz' at position 8 cannot be interpreted as a hexadecimal number.");
	}

	private static void assertEquateEquals(String actualLine, EquateType expectedEquateType, String expectedLabel,
			int expectedLabelAccess, int expectedAddress, String expectedComment, String expectedError) {
		Equate.ReadResult result = Equate.readFrom(actualLine);
		try {
			Assert.stringEquals(result.equateType.name(), expectedEquateType.name());
			Assert.stringEquals(result.label, expectedLabel);
			Assert.stringEquals(LabelAccess.getKey(result.labelAccess), LabelAccess.getKey(expectedLabelAccess));
			Assert.longEquals(result.address, expectedAddress);
			Assert.stringEquals(result.comment, expectedComment);
			Assert.stringEquals(result.error, expectedError);
		} catch (AssertionError ex) {
			Assert.fail("Equate line '" + actualLine + "' was not read as expected.");
		}
	}
}
