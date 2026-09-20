/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

import org.w3c.dom.Element;

/**
 * A disassembly output formatting profile: syntax and layout settings for
 * one target assembler, used to render labels, numbers, bytes, comments, and
 * directives.
 * <p>
 * Ported from Profile.h / Profile.cpp.
 *
 * @author Peter Dell
 */
public final class Profile implements Xml.Serializable {

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

	@Override
	public void serializeTo(Element element) {
		// Source Layout
		Xml.setBoolAttribute(element, "UseLineNumbers", useLineNumbers);
		Xml.setBoolAttribute(element, "AlignInstructions", alignInstructions);
		Xml.setBoolAttribute(element, "ShowLowerCaseInstructions", showLowerCaseInstructions);

		// Opcodes
		Xml.setBoolAttribute(element, "UseIllegalOpcodes", useIllegalOpcodes);
		Xml.setBoolAttribute(element, "ShowAInAccumulatorMode", showAInAccumulatorMode);
		Xml.setBoolAttribute(element, "ShowColonAfterLabel", showColonAfterLabel);
		Xml.setBoolAttribute(element, "ShowOpcodeAsComment", showOpcodeAsComment);
		Xml.setBoolAttribute(element, "ShowBRKAsByte0", showBRKAsByte0);
		Xml.setBoolAttribute(element, "ShowZPAbsoluteAsByte", showZPAbsoluteAsByte);
		Xml.setStringAttribute(element, "DirectiveForceAbsolute", directiveForceAbsolute);

		// Comments
		Xml.setStringAttribute(element, "CommentPrefix", commentPrefix);

		// Numbers
		Xml.setBoolAttribute(element, "UseHexNotation", useHexNotation);
		Xml.setStringAttribute(element, "HexNotationPrefix", hexNotationPrefix);

		// Strings
		Xml.setBoolAttribute(element, "ShowNonASCIIChararactersAsBytes", showNonASCIIChararactersAsBytes);
		Xml.setStringAttribute(element, "QuoteForASCIIStrings", quoteForASCIIStrings);

		// Directives
		Xml.setStringAttribute(element, "DirectiveLOWHead", directiveLOWHead);
		Xml.setStringAttribute(element, "DirectiveLOWTail", directiveLOWTail);
		Xml.setStringAttribute(element, "DirectiveHIGHHead", directiveHIGHHead);
		Xml.setStringAttribute(element, "DirectiveHIGHTail", directiveHIGHTail);

		Xml.setStringAttribute(element, "DirectiveBYTE", directiveBYTE);
		Xml.setStringAttribute(element, "DirectiveBYTESeparator", directiveBYTESeparator);
		Xml.setWordAttribute(element, "DirectiveBYTENumberOfBytesPerLine", directiveBYTENumberOfBytesPerLine);
		Xml.setWordAttribute(element, "DirectiveBYTENumberOfCharactersPerString",
				directiveBYTENumberOfCharactersPerString);
		Xml.setBoolAttribute(element, "DirectiveBYTEOnlyNumbersAllowed", directiveBYTEOnlyNumbersAllowed);
		Xml.setBoolAttribute(element, "DirectiveSBYTEAllowed", directiveSBYTEAllowed);
		Xml.setStringAttribute(element, "DirectiveSBYTE", directiveSBYTE);
		Xml.setBoolAttribute(element, "DirectiveWORDAllowed", directiveWORDAllowed);
		Xml.setStringAttribute(element, "DirectiveWORD", directiveWORD);
		Xml.setWordAttribute(element, "DirectiveWORDNumberOfWordsPerLine", directiveWORDNumberOfWordsPerLine);
		Xml.setBoolAttribute(element, "DirectiveDSAllowed", directiveDSAllowed);
		Xml.setStringAttribute(element, "DirectiveDS", directiveDS);

		// Source structure
		Xml.setStringAttribute(element, "DirectiveORG", directiveORG);
		Xml.setStringAttribute(element, "DirectiveEQU", directiveEQU);
		Xml.setStringAttribute(element, "DirectiveENDHead", directiveENDHead);
		Xml.setStringAttribute(element, "DirectiveENDTail", directiveENDTail);
		Xml.setBoolAttribute(element, "DirectiveENDNeedsFilename", directiveENDNeedsFilename);

		// Disassembly listing
		Xml.setStringAttribute(element, "OutputEncoding", outputEncoding.getKey());
		Xml.setBoolAttribute(element, "OmitUnreferencedSystemLabels", omitUnreferencedSystemLabels);

		// Include files
		Xml.setBoolAttribute(element, "DirectiveINCLUDEAllowed", directiveINCLUDEAllowed);
		Xml.setStringAttribute(element, "DirectiveINCLUDEHead", directiveINCLUDEHead);
		Xml.setStringAttribute(element, "DirectiveINCLUDETail", directiveINCLUDETail);
		Xml.setBoolAttribute(element, "DirectiveINCLUDEAllEquatesInOneIncludeFile",
				directiveINCLUDEAllEquatesInOneIncludeFile);
		Xml.setBoolAttribute(element, "DirectiveINCLUDEAllIncludesInMainFile",
				directiveINCLUDEAllIncludesInMainFile);
		Xml.setWordAttribute(element, "DirectiveINCLUDEMaximumNumberOfLinesPerFile",
				directiveINCLUDEMaximumNumberOfLinesPerFile);
	}

	@Override
	public void deserializeFrom(Element element) {
		clear();

		// Source Layout
		useLineNumbers = Xml.getBoolAttribute(element, "UseLineNumbers", useLineNumbers);
		alignInstructions = Xml.getBoolAttribute(element, "AlignInstructions", alignInstructions);
		showLowerCaseInstructions = Xml.getBoolAttribute(element, "ShowLowerCaseInstructions",
				showLowerCaseInstructions);

		// Opcodes
		useIllegalOpcodes = Xml.getBoolAttribute(element, "UseIllegalOpcodes", useIllegalOpcodes);
		showAInAccumulatorMode = Xml.getBoolAttribute(element, "ShowAInAccumulatorMode", showAInAccumulatorMode);
		showColonAfterLabel = Xml.getBoolAttribute(element, "ShowColonAfterLabel", showColonAfterLabel);
		showOpcodeAsComment = Xml.getBoolAttribute(element, "ShowOpcodeAsComment", showOpcodeAsComment);
		showBRKAsByte0 = Xml.getBoolAttribute(element, "ShowBRKAsByte0", showBRKAsByte0);
		showZPAbsoluteAsByte = Xml.getBoolAttribute(element, "ShowZPAbsoluteAsByte", showZPAbsoluteAsByte);
		directiveForceAbsolute = Xml.getStringAttribute(element, "DirectiveForceAbsolute", directiveForceAbsolute);

		// Comments
		commentPrefix = Xml.getStringAttribute(element, "CommentPrefix", commentPrefix);

		// Numbers
		useHexNotation = Xml.getBoolAttribute(element, "UseHexNotation", useHexNotation);
		hexNotationPrefix = Xml.getStringAttribute(element, "HexNotationPrefix", hexNotationPrefix);

		// Strings
		showNonASCIIChararactersAsBytes = Xml.getBoolAttribute(element, "ShowNonASCIIChararactersAsBytes",
				showNonASCIIChararactersAsBytes);
		quoteForASCIIStrings = Xml.getStringAttribute(element, "QuoteForASCIIStrings", quoteForASCIIStrings);

		// Directives
		directiveLOWHead = Xml.getStringAttribute(element, "DirectiveLOWHead", directiveLOWHead);
		directiveLOWTail = Xml.getStringAttribute(element, "DirectiveLOWTail", directiveLOWTail);
		directiveHIGHHead = Xml.getStringAttribute(element, "DirectiveHIGHHead", directiveHIGHHead);
		directiveHIGHTail = Xml.getStringAttribute(element, "DirectiveHIGHTail", directiveHIGHTail);

		directiveBYTE = Xml.getStringAttribute(element, "DirectiveBYTE", directiveBYTE);
		directiveBYTESeparator = Xml.getStringAttribute(element, "DirectiveBYTESeparator", directiveBYTESeparator);
		directiveBYTENumberOfBytesPerLine = Xml.getWordAttribute(element, "DirectiveBYTENumberOfBytesPerLine",
				directiveBYTENumberOfBytesPerLine);
		directiveBYTENumberOfCharactersPerString = Xml.getWordAttribute(element,
				"DirectiveBYTENumberOfCharactersPerString", directiveBYTENumberOfCharactersPerString);
		directiveBYTEOnlyNumbersAllowed = Xml.getBoolAttribute(element, "DirectiveBYTEOnlyNumbersAllowed",
				directiveBYTEOnlyNumbersAllowed);
		directiveSBYTEAllowed = Xml.getBoolAttribute(element, "DirectiveSBYTEAllowed", directiveSBYTEAllowed);
		directiveSBYTE = Xml.getStringAttribute(element, "DirectiveSBYTE", directiveSBYTE);
		directiveWORDAllowed = Xml.getBoolAttribute(element, "DirectiveWORDAllowed", directiveWORDAllowed);
		directiveWORD = Xml.getStringAttribute(element, "DirectiveWORD", directiveWORD);
		directiveWORDNumberOfWordsPerLine = Xml.getWordAttribute(element, "DirectiveWORDNumberOfWordsPerLine",
				directiveWORDNumberOfWordsPerLine);
		directiveDSAllowed = Xml.getBoolAttribute(element, "DirectiveDSAllowed", directiveDSAllowed);
		directiveDS = Xml.getStringAttribute(element, "DirectiveDS", directiveDS);

		// Source Structure
		directiveORG = Xml.getStringAttribute(element, "DirectiveORG", directiveORG);
		directiveEQU = Xml.getStringAttribute(element, "DirectiveEQU", directiveEQU);
		directiveENDHead = Xml.getStringAttribute(element, "DirectiveENDHead", directiveENDHead);
		directiveENDTail = Xml.getStringAttribute(element, "DirectiveENDTail", directiveENDTail);
		directiveENDNeedsFilename = Xml.getBoolAttribute(element, "DirectiveENDNeedsFilename",
				directiveENDNeedsFilename);

		// Disassembly listing
		String outputEncodingString = Xml.getStringAttribute(element, "OutputEncoding", "");
		outputEncoding = Encoding.fromKey(outputEncodingString);

		// Ignore unsuitable encodings.
		switch (outputEncoding) {
		case ASCII:
		case ATASCII:
		case UTF8:
			break;
		default:
			outputEncoding = Encoding.ASCII;
		}
		omitUnreferencedSystemLabels = Xml.getBoolAttribute(element, "OmitUnreferencedSystemLabels",
				omitUnreferencedSystemLabels);

		// Include files
		directiveINCLUDEAllowed = Xml.getBoolAttribute(element, "DirectiveINCLUDEAllowed", directiveINCLUDEAllowed);
		directiveINCLUDEHead = Xml.getStringAttribute(element, "DirectiveINCLUDEHead", directiveINCLUDEHead);
		directiveINCLUDETail = Xml.getStringAttribute(element, "DirectiveINCLUDETail", directiveINCLUDETail);
		directiveINCLUDEAllEquatesInOneIncludeFile = Xml.getBoolAttribute(element,
				"DirectiveINCLUDEAllEquatesInOneIncludeFile", directiveINCLUDEAllEquatesInOneIncludeFile);
		directiveINCLUDEAllIncludesInMainFile = Xml.getBoolAttribute(element, "DirectiveINCLUDEAllIncludesInMainFile",
				directiveINCLUDEAllIncludesInMainFile);
		directiveINCLUDEMaximumNumberOfLinesPerFile = Xml.getWordAttribute(element,
				"DirectiveINCLUDEMaximumNumberOfLinesPerFile", directiveINCLUDEMaximumNumberOfLinesPerFile);
	}
}
