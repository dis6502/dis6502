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
 * <p>
 * Constants below the {@code Profile_*} group belong to other dialogs and
 * are named {@code <DialogName>_<Purpose>} rather than
 * {@code <ModelClass>_<Field>}, since (unlike {@code Profile}'s fields) most
 * don't map 1:1 onto a shared model field - matching the same real-shared-
 * resource-vs-dialog-local-text split {@link Text}/{@link Texts} already
 * draws. Each dialog's mnemonics are unique only within that one dialog;
 * since these are all separate modal dialogs (never simultaneously visible),
 * cross-dialog mnemonic reuse is harmless, unlike the panel-scoping above.
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

	// AssembleDialog.
	public static DataType AssembleDialog_Address = new DataType(String.class);
	public static DataType AssembleDialog_Result = new DataType(String.class);

	// DisassemblyProgressDialog.
	public static DataType DisassemblyProgressDialog_Pass = new DataType(String.class);
	public static DataType DisassemblyProgressDialog_Segment = new DataType(String.class);

	// DiskImageExecutableFileDialog.
	public static DataType DiskImageExecutableFileDialog_DiskImageFile = new DataType(String.class);
	public static DataType DiskImageExecutableFileDialog_ExecutableFileName = new DataType(String.class);

	// DiskImageSectorsDialog.
	public static DataType DiskImageSectorsDialog_DiskImageFile = new DataType(String.class);
	public static DataType DiskImageSectorsDialog_StartOffset = new DataType(String.class);
	public static DataType DiskImageSectorsDialog_EndOffset = new DataType(String.class);
	public static DataType DiskImageSectorsDialog_Address = new DataType(String.class);

	// EquateDialog.
	public static DataType EquateDialog_Equate = new DataType(String.class);

	// EquateRangeDialog.
	public static DataType EquateRangeDialog_StartAddress = new DataType(String.class);
	public static DataType EquateRangeDialog_EndAddress = new DataType(String.class);
	public static DataType EquateRangeDialog_RelativeToEquate = new DataType(String.class);

	// LowHighByteDialog.
	public static DataType LowHighByteDialog_LowByte = new DataType(String.class);
	public static DataType LowHighByteDialog_HighByte = new DataType(String.class);

	// MemoryInspectorFindStringDialog.
	public static DataType MemoryInspectorFindStringDialog_AsciiString = new DataType(String.class);
	public static DataType MemoryInspectorFindStringDialog_Hex = new DataType(String.class);
	public static DataType MemoryInspectorFindStringDialog_AllSegments = new DataType(Boolean.class);
	public static DataType MemoryInspectorFindStringDialog_SelectedSegment = new DataType(Boolean.class);

	// RawFileDialog.
	public static DataType RawFileDialog_FilePath = new DataType(String.class);
	public static DataType RawFileDialog_StartOffset = new DataType(String.class);
	public static DataType RawFileDialog_EndOffset = new DataType(String.class);
	public static DataType RawFileDialog_Address = new DataType(String.class);

	// SegmentPropertiesDialog.
	public static DataType SegmentPropertiesDialog_Address = new DataType(String.class);
	public static DataType SegmentPropertiesDialog_LabelPrefix = new DataType(String.class);
	public static DataType SegmentPropertiesDialog_Processor = new DataType(String.class);
	public static DataType SegmentPropertiesDialog_Binary = new DataType(Boolean.class);

	// SegmentWriteBootDiskDialog.
	public static DataType SegmentWriteBootDiskDialog_LoadAddress = new DataType(String.class);
	public static DataType SegmentWriteBootDiskDialog_InitAddress = new DataType(String.class);

	// SelectGraphicsDialog.
	public static DataType SelectGraphicsDialog_GraphicMode = new DataType(String.class);
	public static DataType SelectGraphicsDialog_BytesPerLine = new DataType(String.class);
	public static DataType SelectGraphicsDialog_Address = new DataType(String.class);

	// WorkspaceDialog.
	public static DataType WorkspaceDialog_ComputerSystem = new DataType(String.class);

	static {
		initializeClass(DataTypes.class, null);
	}
}
