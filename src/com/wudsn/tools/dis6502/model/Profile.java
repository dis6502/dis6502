/*
 * Copyright (C) 2025 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

/**
 * A disassembly output formatting profile: syntax and layout settings for
 * one target assembler, used to render labels, numbers, bytes, comments, and
 * directives.
 * <p>
 * Ported from Profile.h / Profile.cpp. {@code SerializeTo}/{@code
 * DeserializeFrom} (XML persistence) are not ported yet, deferred to when
 * {@code Workspace}'s XML persistence is ported.
 *
 * @author Peter Dell
 */
public final class Profile {

	// Source Layout.
	public boolean useLineNumbers; // Since 1.0.
	public boolean alignInstructions; // Since 1.0.
	public boolean showLowerCaseInstructions; // Since 1.6.

	// Opcodes.
	public boolean useIllegalOpcodes; // Since 1.0.
	public boolean showAInAccumulatorMode; // Since 1.0.
	public boolean showColonAfterLabel; // Since 1.6.
	public boolean showOpcodeAsComment; // Since 3.0.
	public boolean showBRKAsByte0; // Since 1.6.
	public boolean showZPAbsoluteAsByte; // Since 1.7.
	public String directiveForceAbsolute = ""; // Since 3.6.

	// Comments.
	public String commentPrefix = ""; // Since 1.0.

	// Numbers.
	public boolean useHexNotation; // Since 1.0.
	public String hexNotationPrefix = ""; // Since 1.0.

	// Strings.
	public boolean showNonASCIIChararactersAsBytes; // Since 3.0.
	public String quoteForASCIIStrings = ""; // Since 3.0; string where only the first character is used.

	// Directives.
	public String directiveLOWHead = ""; // Since 1.0.
	public String directiveLOWTail = ""; // Since 1.0.
	public String directiveHIGHHead = ""; // Since 1.0.
	public String directiveHIGHTail = ""; // Since 1.0.

	public String directiveBYTE = ""; // Since 1.0.
	public String directiveBYTESeparator = ""; // Since 1.0.
	public int directiveBYTENumberOfBytesPerLine; // Since 3.0.
	public int directiveBYTENumberOfCharactersPerString; // Since 3.0.
	public boolean directiveBYTEOnlyNumbersAllowed; // Since 1.0.
	public boolean directiveSBYTEAllowed; // Since 1.0.
	public String directiveSBYTE = ""; // Since 1.0.
	public boolean directiveWORDAllowed; // Since 1.0.
	public String directiveWORD = ""; // Since 1.0.
	public int directiveWORDNumberOfWordsPerLine; // Since 3.0.
	public boolean directiveDSAllowed; // Since 1.3.
	public String directiveDS = ""; // Since 1.3.

	// Source Structure.
	public String directiveORG = ""; // Since 1.0.
	public String directiveEQU = ""; // Since 1.0.
	public String directiveENDHead = ""; // Since 1.0.
	public boolean directiveENDNeedsFilename;
	public String directiveENDTail = ""; // Since 1.0.

	// Disassembly Listing.
	public Encoding outputEncoding = Encoding.UNKNOWN; // Since 4.0.
	public boolean omitUnreferencedSystemLabels; // Since 3.0.
	public boolean directiveINCLUDEAllowed; // Since 1.0.
	public String directiveINCLUDEHead = ""; // Since 1.0.
	public String directiveINCLUDETail = ""; // Since 1.0.
	public boolean directiveINCLUDEAllEquatesInOneIncludeFile; // Since 3.0.
	public boolean directiveINCLUDEAllIncludesInMainFile; // Since 1.0.
	public int directiveINCLUDEMaximumNumberOfLinesPerFile; // Since 1.0.

	public Profile() {
		clear();
	}

	/** Resets all settings to the defaults used by MADS. */
	public void clear() {
		commentPrefix = ";";
		hexNotationPrefix = "$";
		useIllegalOpcodes = false;
		useLineNumbers = false;
		alignInstructions = true;
		useHexNotation = true;
		showAInAccumulatorMode = false;
		showBRKAsByte0 = true;
		showLowerCaseInstructions = true;
		showColonAfterLabel = false;
		showZPAbsoluteAsByte = false;
		directiveForceAbsolute = ".w";
		quoteForASCIIStrings = "'";
		directiveWORDNumberOfWordsPerLine = 1; // Because WORDs often mean labels and labels now have "SnnnLxxxx" format.
		directiveBYTENumberOfBytesPerLine = 16;
		directiveBYTENumberOfCharactersPerString = 40;
		showOpcodeAsComment = false;

		// Directive syntax.
		directiveBYTE = ".byte";
		directiveWORD = ".word";
		directiveSBYTE = ".sb";
		directiveORG = "org";
		directiveEQU = "equ";
		directiveENDHead = "";
		directiveENDTail = "";
		directiveLOWHead = "<";
		directiveLOWTail = "";
		directiveHIGHHead = ">";
		directiveHIGHTail = "";
		directiveBYTESeparator = ",";
		directiveBYTEOnlyNumbersAllowed = false;
		directiveWORDAllowed = true;
		directiveSBYTEAllowed = true;
		directiveENDNeedsFilename = false;
		showNonASCIIChararactersAsBytes = true;

		directiveDSAllowed = true;
		directiveDS = ".ds";

		// Disassembly listing.
		outputEncoding = Encoding.ASCII;
		omitUnreferencedSystemLabels = true;

		// Include files.
		directiveINCLUDEAllowed = true;
		directiveINCLUDEHead = "icl '";
		directiveINCLUDETail = "'";
		directiveINCLUDEAllEquatesInOneIncludeFile = true;
		directiveINCLUDEAllIncludesInMainFile = false;
		directiveINCLUDEMaximumNumberOfLinesPerFile = 0;
	}
}
