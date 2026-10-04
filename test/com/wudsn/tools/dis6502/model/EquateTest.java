/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Covers {@link Equate} line parsing and writing, with and without contexts,
 * and the context-aware lookup of {@link EquateList}.
 *
 * @author Peter Dell
 */
public final class EquateTest {

	private EquateTest() {
	}

	public static void testEquate() throws Exception {
		testParsing();
		testContexts();
		testContextLookup();
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
				"Character \"=\" at position 2 is not a valid start character for a label name.");

		assertEquateEquals("DECIMAL", EquateType.LABEL, "DECIMAL", LabelAccess.UNKNOWN, 0, "",
				"No access qualifier specified.");
		assertEquateEquals("DECIMAL * ", EquateType.LABEL, "DECIMAL", LabelAccess.UNKNOWN, 0, "",
				"Character \"*\" at position 9 is not an access qualifier. Use \"=\", \"<\", \">\" or \"#\".");
		assertEquateEquals("DECIMAL = ", EquateType.LABEL, "DECIMAL", LabelAccess.READ_WRITE, 0, "",
				"No value specified.");
		assertEquateEquals("DECIMAL = ; Comment", EquateType.LABEL, "DECIMAL", LabelAccess.READ_WRITE, 0, "",
				"Characters \"; Comment\" at position 11 cannot be interpreted as a decimal number.");
		assertEquateEquals("DECIMAL = abc", EquateType.LABEL, "DECIMAL", LabelAccess.READ_WRITE, 0, "",
				"Characters \"abc\" at position 11 cannot be interpreted as a decimal number.");
		assertEquateEquals("HEX = $; Comment", EquateType.LABEL, "HEX", LabelAccess.READ_WRITE, 0, "",
				"Characters \"; Comment\" at position 8 cannot be interpreted as a hexadecimal number.");
		assertEquateEquals("HEX = $xyz", EquateType.LABEL, "HEX", LabelAccess.READ_WRITE, 0, "",
				"Characters \"xyz\" at position 8 cannot be interpreted as a hexadecimal number.");
	}

	private static void testContexts() throws Exception {
		assertContexts("S_FLAG = $0700", "S_FLAG = $0700");
		assertContexts("S_FLAG = $0700 [SDX]", "S_FLAG = $0700 [SDX]", "SDX");
		assertContexts("CASINI = $02 [ OS-AB ,OS-XL ]  ; Comment", "CASINI = $0002 [OS-AB, OS-XL]; Comment", "OS-AB",
				"OS-XL");
		assertContexts("X_1 # 1 [A_1,A_1];", "X_1 # $0001 [A_1]", "A_1");

		// Error cases.
		assertEquateEquals("S_FLAG = $0700 [SDX", EquateType.LABEL, "S_FLAG", LabelAccess.READ_WRITE, 0x0700, "",
				"The context list at position 16 is not closed with \"]\".");
		assertEquateEquals("S_FLAG = $0700 [", EquateType.LABEL, "S_FLAG", LabelAccess.READ_WRITE, 0x0700, "",
				"The context list at position 16 is not closed with \"]\".");
		assertEquateEquals("S_FLAG = $0700 [S.X]", EquateType.LABEL, "S_FLAG", LabelAccess.READ_WRITE, 0x0700, "",
				"Character \".\" at position 18 is not valid in a context name. Use letters, digits, \"_\" or \"-\".");
		assertEquateEquals("S_FLAG = $0700 []", EquateType.LABEL, "S_FLAG", LabelAccess.READ_WRITE, 0x0700, "",
				"Context name missing at position 17.");
		assertEquateEquals("S_FLAG = $0700 [SDX,]", EquateType.LABEL, "S_FLAG", LabelAccess.READ_WRITE, 0x0700, "",
				"Context name missing at position 21.");
		assertEquateEquals("S_FLAG = $0700 [SDX] x", EquateType.LABEL, "S_FLAG", LabelAccess.READ_WRITE, 0x0700, "",
				"Invalid character \"x\" after value found. Line end or comment expected.");
	}

	/** Parses {@code line}, checks its contexts and how it is written, and that the workspace XML keeps them. */
	private static void assertContexts(String line, String expectedString, String... expectedContexts)
			throws Exception {
		Equate.ReadResult result = Equate.readFrom(line);
		Assert.stringEquals(result.error, "");
		List<String> expected = Arrays.asList(expectedContexts);
		Assert.stringEquals(result.equate.getContexts().toString(), expected.toString());
		Assert.boolEquals(result.equate.isGlobal(), expected.isEmpty());
		Assert.stringEquals(result.equate.toString(), expectedString);
		Assert.stringEquals(Equate.readFrom(result.equate.toString()).equate.getContexts().toString(),
				expected.toString());

		Document document = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder()
				.newDocument();
		Element element = document.createElement("Equate");
		result.equate.serializeTo(element);
		Equate copy = new Equate();
		copy.deserializeFrom(element);
		Assert.stringEquals(copy.toString(), expectedString);
	}

	/**
	 * At one address, an equate of an active context wins over a global one,
	 * whatever the order; one of an inactive context never matches.
	 */
	private static void testContextLookup() {
		EquateList equateList = new EquateList(WorkspaceProperty.USER_EQUATES);
		equateList.addEquate("GLOBAL = $0700");
		equateList.addEquate("S_FLAG = $0700 [SDX]");
		equateList.addEquate("ONLY_SDX = $0701 [SDX, OTHER]");
		Set<String> none = Collections.emptySet();
		Set<String> sdx = new HashSet<>(Arrays.asList("SDX"));
		Set<String> other = new HashSet<>(Arrays.asList("OTHER"));

		Assert.stringEquals(equateList.findEquateByAddress(0x0700, LabelAccess.READ, none).getLabel(), "GLOBAL");
		Assert.stringEquals(equateList.findEquateByAddress(0x0700, LabelAccess.READ, sdx).getLabel(), "S_FLAG");
		Assert.stringEquals(equateList.findEquateByAddress(0x0700, LabelAccess.READ, other).getLabel(), "GLOBAL");
		Assert.isNull(equateList.findEquateByAddress(0x0701, LabelAccess.READ, none));
		Assert.stringEquals(equateList.findEquateByAddress(0x0701, LabelAccess.READ, other).getLabel(), "ONLY_SDX");
		Assert.stringEquals(equateList.getContextNames().toString(), "[OTHER, SDX]");
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
