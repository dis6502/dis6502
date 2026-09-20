/*
 * Copyright (C) 2025 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502;

import java.util.HashSet;
import java.util.Set;

import com.wudsn.tools.base.repository.DataType;
import com.wudsn.tools.dis6502.model.Assert;

/**
 * Headless coverage for {@link DataTypes}: every field's label carries
 * exactly one mnemonic, and mnemonics are unique within each of {@link
 * com.wudsn.tools.dis6502.ui.ProfileDialog}'s three titled panels - see
 * {@link DataTypes}'s own class javadoc for why uniqueness is scoped per
 * panel rather than across the whole dialog.
 *
 * @author Peter Dell
 */
public final class DataTypesTest {

	private DataTypesTest() {
	}

	private static final DataType[] GENERAL_PANEL = { DataTypes.Profile_CommentPrefix, DataTypes.Profile_HexNotationPrefix,
			DataTypes.Profile_UseIllegalOpcodes, DataTypes.Profile_UseHexNotation, DataTypes.Profile_AlignInstructions,
			DataTypes.Profile_UseLineNumbers, DataTypes.Profile_ShowLowerCaseInstructions, DataTypes.Profile_ShowAInAccumulatorMode,
			DataTypes.Profile_ShowColonAfterLabel, DataTypes.Profile_ShowOpcodeAsComment, DataTypes.Profile_ShowBRKAsByte0,
			DataTypes.Profile_ShowZPAbsoluteAsByte, DataTypes.Profile_DirectiveForceAbsolute,
			DataTypes.Profile_ShowNonASCIIChararactersAsBytes, DataTypes.Profile_DirectiveBYTENumberOfBytesPerLine,
			DataTypes.Profile_DirectiveWORDNumberOfWordsPerLine, DataTypes.Profile_DirectiveBYTENumberOfCharactersPerString,
			DataTypes.Profile_QuoteForASCIIStrings };

	private static final DataType[] DIRECTIVE_SYNTAX_PANEL = { DataTypes.Profile_DirectiveBYTE, DataTypes.Profile_DirectiveBYTESeparator,
			DataTypes.Profile_DirectiveBYTEOnlyNumbersAllowed, DataTypes.Profile_DirectiveWORDAllowed, DataTypes.Profile_DirectiveSBYTEAllowed,
			DataTypes.Profile_DirectiveORG, DataTypes.Profile_DirectiveLOWHead, DataTypes.Profile_DirectiveEQU,
			DataTypes.Profile_DirectiveHIGHHead, DataTypes.Profile_DirectiveENDHead, DataTypes.Profile_DirectiveENDNeedsFilename,
			DataTypes.Profile_DirectiveDSAllowed };

	private static final DataType[] DISASSEMBLY_LISTING_PANEL = { DataTypes.Profile_OutputEncoding,
			DataTypes.Profile_OmitUnreferencedSystemLabels, DataTypes.Profile_DirectiveINCLUDEAllowed, DataTypes.Profile_DirectiveINCLUDEHead,
			DataTypes.Profile_DirectiveINCLUDETail, DataTypes.Profile_DirectiveINCLUDEAllEquatesInOneIncludeFile,
			DataTypes.Profile_DirectiveINCLUDEAllIncludesInMainFile, DataTypes.Profile_DirectiveINCLUDEEachFileIncludesNextFile,
			DataTypes.Profile_DirectiveINCLUDEMaximumNumberOfLinesPerFile };

	public static void testDataTypes() {
		testEveryLabelHasExactlyOneMnemonic(GENERAL_PANEL);
		testEveryLabelHasExactlyOneMnemonic(DIRECTIVE_SYNTAX_PANEL);
		testEveryLabelHasExactlyOneMnemonic(DISASSEMBLY_LISTING_PANEL);

		testMnemonicsUniqueWithinPanel("General", GENERAL_PANEL);
		testMnemonicsUniqueWithinPanel("Directive Syntax", DIRECTIVE_SYNTAX_PANEL);
		testMnemonicsUniqueWithinPanel("Disassembly Listing", DISASSEMBLY_LISTING_PANEL);

		Assert.log("DataTypesTest completed");
	}

	private static void testEveryLabelHasExactlyOneMnemonic(DataType[] panel) {
		for (DataType dataType : panel) {
			String label = dataType.getLabel();
			int firstIndex = label.indexOf('&');
			if (firstIndex == -1) {
				Assert.fail("Label '" + label + "' does not contain a '&' mnemonic marker.");
			}
			int secondIndex = label.indexOf('&', firstIndex + 1);
			if (secondIndex != -1) {
				Assert.fail("Label '" + label + "' contains more than one '&' mnemonic marker.");
			}
			char mnemonic = Character.toUpperCase(label.charAt(firstIndex + 1));
			if (mnemonic < 'A' || mnemonic > 'Z') {
				Assert.fail("Mnemonic character '" + mnemonic + "' in label '" + label + "' is not between 'A' and 'Z'.");
			}
		}
	}

	private static void testMnemonicsUniqueWithinPanel(String panelName, DataType[] panel) {
		Set<Character> mnemonics = new HashSet<>();
		for (DataType dataType : panel) {
			String label = dataType.getLabel();
			char mnemonic = Character.toUpperCase(label.charAt(label.indexOf('&') + 1));
			if (!mnemonics.add(mnemonic)) {
				Assert.fail("Duplicate mnemonic '" + mnemonic + "' in panel '" + panelName + "', label '" + label + "'.");
			}
		}
	}
}
