/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502;

import com.wudsn.tools.base.repository.DataType;
import com.wudsn.tools.base.repository.NLS;
import com.wudsn.tools.dis6502.model.Encoding;
import com.wudsn.tools.dis6502.model.Profile;

/**
 * UI metadata (label text plus keyboard mnemonic, and occasionally a
 * tooltip) for {@link Profile} fields edited by
 * {@link com.wudsn.tools.dis6502.ui.ProfileDialog}, following the same
 * {@code <ModelClass>_<Field>} naming convention
 * {@code com.wudsn.tools.thecartstudio.DataTypes} uses for its own model
 * fields. Fields are grouped and ordered to match
 * {@code ProfileDialog}'s three titled panels (General, Directive Syntax,
 * Disassembly Listing).
 * <p>
 * Every field's mnemonic is unique only <em>within its own panel</em>, not
 * across the whole dialog - with ~39 fields sharing one 26-letter alphabet
 * and each mnemonic constrained to a letter that actually appears in that
 * field's own label text, a dialog-wide-unique assignment was not always
 * achievable by hand. A duplicate mnemonic across two different panels is a
 * minor UX rough edge (Swing resolves Alt+key ambiguity deterministically,
 * it does not crash or silently do nothing), not a functional bug.
 *
 * @author Peter Dell
 */
public final class DataTypes extends NLS {

	// General.
	public static DataType Profile_CommentPrefix = new DataType(String.class);
	public static DataType Profile_HexNotationPrefix = new DataType(String.class);
	public static DataType Profile_UseIllegalOpcodes = new DataType(Boolean.class);
	public static DataType Profile_UseHexNotation = new DataType(Boolean.class);
	public static DataType Profile_AlignInstructions = new DataType(Boolean.class);
	public static DataType Profile_UseLineNumbers = new DataType(Boolean.class);
	public static DataType Profile_ShowLowerCaseInstructions = new DataType(Boolean.class);
	public static DataType Profile_ShowAInAccumulatorMode = new DataType(Boolean.class);
	public static DataType Profile_ShowColonAfterLabel = new DataType(Boolean.class);
	public static DataType Profile_ShowOpcodeAsComment = new DataType(Boolean.class);
	public static DataType Profile_ShowBRKAsByte0 = new DataType(Boolean.class);
	public static DataType Profile_ShowZPAbsoluteAsByte = new DataType(Boolean.class);
	public static DataType Profile_DirectiveForceAbsolute = new DataType(String.class);
	public static DataType Profile_ShowNonASCIIChararactersAsBytes = new DataType(Boolean.class);
	public static DataType Profile_DirectiveBYTENumberOfBytesPerLine = new DataType(Integer.class);
	public static DataType Profile_DirectiveWORDNumberOfWordsPerLine = new DataType(Integer.class);
	public static DataType Profile_DirectiveBYTENumberOfCharactersPerString = new DataType(Integer.class);
	public static DataType Profile_QuoteForASCIIStrings = new DataType(String.class);

	// Directive Syntax.
	public static DataType Profile_DirectiveBYTE = new DataType(String.class);
	public static DataType Profile_DirectiveBYTESeparator = new DataType(String.class);
	public static DataType Profile_DirectiveBYTEOnlyNumbersAllowed = new DataType(Boolean.class);
	public static DataType Profile_DirectiveWORDAllowed = new DataType(Boolean.class);
	public static DataType Profile_DirectiveSBYTEAllowed = new DataType(Boolean.class);
	public static DataType Profile_DirectiveORG = new DataType(String.class);
	public static DataType Profile_DirectiveLOWHead = new DataType(String.class);
	public static DataType Profile_DirectiveEQU = new DataType(String.class);
	public static DataType Profile_DirectiveHIGHHead = new DataType(String.class);
	public static DataType Profile_DirectiveENDHead = new DataType(String.class);
	public static DataType Profile_DirectiveENDNeedsFilename = new DataType(Boolean.class);
	public static DataType Profile_DirectiveDSAllowed = new DataType(Boolean.class);

	// Disassembly Listing.
	public static DataType Profile_OutputEncoding = new DataType(Encoding.class);
	public static DataType Profile_OmitUnreferencedSystemLabels = new DataType(Boolean.class);
	public static DataType Profile_DirectiveINCLUDEAllowed = new DataType(Boolean.class);
	public static DataType Profile_DirectiveINCLUDEHead = new DataType(String.class);
	public static DataType Profile_DirectiveINCLUDETail = new DataType(String.class);
	public static DataType Profile_DirectiveINCLUDEAllEquatesInOneIncludeFile = new DataType(Boolean.class);
	public static DataType Profile_DirectiveINCLUDEAllIncludesInMainFile = new DataType(Boolean.class);
	public static DataType Profile_DirectiveINCLUDEEachFileIncludesNextFile = new DataType(Boolean.class);
	public static DataType Profile_DirectiveINCLUDEMaximumNumberOfLinesPerFile = new DataType(Integer.class);

	static {
		initializeClass(DataTypes.class, null);
	}
}
