/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502;

import com.wudsn.tools.base.repository.Message;
import com.wudsn.tools.base.repository.NLS;

/**
 * Message repository for structured, severity-typed messages ({@link
 * Message}) - unlike {@link Texts}, whose fields are plain {@code
 * String}s. Follows the same pattern {@code com.wudsn.tools.base.Messages}
 * already uses in the shared WUDSN library: a field's own name encodes its
 * severity via a leading {@code S}/{@code I}/{@code E} for {@link
 * Message#STATUS}/{@link Message#INFO}/{@link Message#ERROR} (see {@code
 * com.wudsn.tools.base.repository.NLS}'s field-loading switch), followed
 * by a plain sequence number, shared across every severity (an {@code E}
 * field's number does not restart at 1 - it just continues on from
 * whatever {@code I}/{@code S} number came before it). A message belongs
 * here rather than {@link Texts} once its severity needs to actually
 * drive dispatch via {@link Application#sendMessage}.
 *
 * @author Peter Dell
 */
public final class Messages extends NLS {

	/** {@link Dis6502#run}'s startup warning message. */
	public static Message I001;

	/** {@link com.wudsn.tools.dis6502.model.ProfileLogic#load}'s "not a valid profile" error. */
	public static Message E002;

	/** {@link com.wudsn.tools.dis6502.model.WorkspaceLogic#load}'s "not a valid workspace" error. */
	public static Message E003;

	/** {@link com.wudsn.tools.dis6502.model.EquateListLogic#load}'s per-line parse error. */
	public static Message E004;

	/** {@link Application#sendErrorMessage(Throwable)}'s generic exception wrapper. */
	public static Message E005;

	/** {@link Dis6502#performWriteBootDisk}'s "no segment in memory" error. */
	public static Message E006;

	/**
	 * {@link com.wudsn.tools.dis6502.model.WorkspaceLogic#addRawSegment}/
	 * {@link com.wudsn.tools.dis6502.model.WorkspaceLogic#addDiskImageBootSectorsSegment}'s
	 * file-read error.
	 */
	public static Message E007;

	/** {@link com.wudsn.tools.dis6502.model.EquateListLogic#load}'s equate-count summary. */
	public static Message I008;

	/** {@link com.wudsn.tools.dis6502.model.EquateListLogic#load}'s "opening file" log line. */
	public static Message I009;

	/** {@link com.wudsn.tools.dis6502.model.ProfileLogic#load}'s "opening file" log line. */
	public static Message I010;

	/** {@link com.wudsn.tools.dis6502.model.WorkspaceLogic#load}'s "opening file" log line. */
	public static Message I011;

	/** {@link com.wudsn.tools.dis6502.model.EquateListLogic#save}'s "saving file" log line. */
	public static Message I012;

	/** {@link com.wudsn.tools.dis6502.model.ProfileLogic#save}'s "saving file" log line. */
	public static Message I013;

	/** {@link com.wudsn.tools.dis6502.model.WorkspaceLogic#save}'s "saving file" log line. */
	public static Message I014;

	/** {@link Dis6502#performMergeSegments}'s merged-count summary. */
	public static Message I015;

	/** {@link Dis6502#performNewWorkspace}'s "new workspace prepared" log line. */
	public static Message I016;

	/** {@link Dis6502#performOpenFile}'s "opening file" log line for a cassette image. */
	public static Message I017;

	/** {@link Dis6502#openDiskImageBootSectors}'s "opening file" log line. */
	public static Message I018;

	/** {@link Dis6502#openDiskImageExecutableFile}'s "opening file" log line. */
	public static Message I019;

	/** {@link Dis6502#openDiskImageSectors}'s "opening file" log line. */
	public static Message I020;

	/** {@link Dis6502#performOpenFile}'s "opening file" log line for an executable file. */
	public static Message I021;

	/** {@link Dis6502#openRawFile}'s "opening file" log line. */
	public static Message I022;

	/** {@link Dis6502#performOpenFile}'s "opening file" log line for a ROM image. */
	public static Message I023;

	/** {@link com.wudsn.tools.dis6502.model.DisassemblyResultFile#openWriter}'s "saving file" log line. */
	public static Message I024;

	/** {@link Dis6502#performSetMemoryInspectorLoHiType}'s "LOBYTE/HIBYTE only valid for immediate operand" error. */
	public static Message E025;

	/** {@link com.wudsn.tools.dis6502.model.DiskImage#displayError}'s "bad ATR signature" error. */
	public static Message E026;

	/** {@link com.wudsn.tools.dis6502.model.DiskImage#displayError}'s "disk error" error. */
	public static Message E027;

	/** {@link com.wudsn.tools.dis6502.model.DiskImage#displayError}'s "file does not exist" error. */
	public static Message E028;

	/** {@link com.wudsn.tools.dis6502.model.DiskImage#displayError}'s "sector out of range" error. */
	public static Message E029;

	/** {@link com.wudsn.tools.dis6502.model.DiskImage#displayError}'s "write protected" error. */
	public static Message E030;

	/** {@link Dis6502#performSetMemoryInspectorLoHiType}'s "LOBYTE/HIBYTE not allowed for first byte" error. */
	public static Message E031;

	/** {@link Dis6502#performSetMemoryInspectorLoHiType}'s "LOBYTE/HIBYTE on multi-byte selection" error. */
	public static Message E032;

	/** {@link Dis6502#openDiskImageExecutableFile}'s "no file found in disk image" error. */
	public static Message E033;

	/** {@link Dis6502#openDiskImageBootSectors}'s "disk image is not bootable" error. */
	public static Message E034;

	/**
	 * {@link com.wudsn.tools.dis6502.model.SegmentListInserter#insertSegment}'s
	 * "no free segment" error.
	 */
	public static Message E035;

	/** {@link Dis6502#openDiskImageExecutableFile}'s "disk image corrupted" fallback error. */
	public static Message E036;

	/** {@link com.wudsn.tools.dis6502.ui.SegmentPropertiesDialog#performOK}'s "segment overlap" error. */
	public static Message E037;

	/**
	 * {@link Dis6502#openRecentWorkspace}/{@link Dis6502#openWorkspaceFile}'s
	 * "could not open workspace" error dialog message.
	 */
	public static Message E038;

	/** {@link Dis6502#openRecentFile}'s "could not open file" error dialog message. */
	public static Message E039;

	/**
	 * {@link Dis6502#performOpenFile}/{@link Dis6502#openDiskImageExecutableFile}'s
	 * "could not add file" error dialog message - shared by both call
	 * sites' {@code add == true} case; see {@link #E049} for the
	 * {@code add == false} ("could not open file") case.
	 */
	public static Message E040;

	/** {@link Dis6502#openDiskImageExecutableFile}'s "file in disk image is empty" error dialog message. */
	public static Message E041;

	/** {@link com.wudsn.tools.dis6502.ui.EquateRangeDialog#performOK}'s "invalid start address" error dialog message. */
	public static Message E042;

	/** {@link com.wudsn.tools.dis6502.ui.EquateRangeDialog#performOK}'s "invalid end address" error dialog message. */
	public static Message E043;

	/** {@link com.wudsn.tools.dis6502.ui.EquateRangeDialog#performOK}'s "start address greater than end address" error dialog message. */
	public static Message E044;

	/** {@link com.wudsn.tools.dis6502.ui.EquateRangeDialog#performOK}'s "no base equate selected" error dialog message. */
	public static Message E045;

	/** {@link com.wudsn.tools.dis6502.ui.EquateRangeDialog#performOK}'s "equate address is inside range" error dialog message. */
	public static Message E046;

	/** {@link com.wudsn.tools.dis6502.ui.ProfileDialog#performLoadProfile}'s "could not load profile" error dialog message. */
	public static Message E047;

	/** {@link com.wudsn.tools.dis6502.ui.RawFileDialog#show}'s "file is empty" error dialog message. */
	public static Message E048;

	/**
	 * {@link Dis6502#performOpenFile}/{@link Dis6502#openDiskImageExecutableFile}'s
	 * "could not open file" error dialog message - the {@code add == false}
	 * counterpart of {@link #E040}, split out from it into its own field
	 * (was previously one field parameterized on the add/open verb).
	 */
	public static Message E049;

	/** {@link com.wudsn.tools.dis6502.model.Atari5200#readROMFile}'s "ROM too large" error. */
	public static Message E050;

	/**
	 * {@link com.wudsn.tools.dis6502.model.Atari800#readExecutableFile}'s
	 * "unsupported file header" error, shared with {@link
	 * com.wudsn.tools.dis6502.model.Oric#readExecutableFile}'s identically
	 * worded one.
	 */
	public static Message E051;

	/**
	 * {@link com.wudsn.tools.dis6502.model.Atari800#readExecutableFile}'s
	 * "segment end address lower than start address" error, shared with
	 * {@link com.wudsn.tools.dis6502.model.Oric#readExecutableFile}'s
	 * identically worded one.
	 */
	public static Message E052;

	/** {@link com.wudsn.tools.dis6502.model.Atari800#readExecutableFile}'s "stream too short for segment" error. */
	public static Message E053;

	/** {@link com.wudsn.tools.dis6502.model.Atari800#readROMFile}'s "not a CART stream" error. */
	public static Message E054;

	/** {@link com.wudsn.tools.dis6502.model.Atari800#readROMFile}'s "unsupported cartridge size" error. */
	public static Message E055;

	/** {@link com.wudsn.tools.dis6502.model.Atari800#readCassetteFile}'s "not a FUJI stream" error. */
	public static Message E056;

	/** {@link com.wudsn.tools.dis6502.model.Atari800}'s {@code writeSDXSymbol}'s "SDX symbol too long" error. */
	public static Message E057;

	/**
	 * {@link com.wudsn.tools.dis6502.model.Atari800}'s {@code
	 * BoundedReader}'s {@code checkedReadFully}/{@code readIntoMemoryBlock}
	 * "stream too short" error - one field for both, since they build the
	 * exact same template text by hand today.
	 */
	public static Message E058;

	/** {@link com.wudsn.tools.dis6502.model.C64#readExecutableFile}'s "file too large" error. */
	public static Message E059;

	/** {@link com.wudsn.tools.dis6502.model.C64#readExecutableFile}'s "more than one segment" error. */
	public static Message E060;

	/** {@link com.wudsn.tools.dis6502.model.DisassemblyResultWriter#openFile}'s "cannot write files, encoding unknown" error. */
	public static Message E061;

	/** {@link com.wudsn.tools.dis6502.model.DisassemblyResultWriter}'s {@code writeString}'s "cannot write strings, encoding is binary" error. */
	public static Message E062;

	/**
	 * {@link com.wudsn.tools.dis6502.model.DisassemblyResultWriter}'s
	 * {@code writeString}'s "character cannot be written in the current
	 * encoding" error - one field for both the ASCII- and ATASCII-encoding
	 * call sites, which build the exact same template text by hand today
	 * except for the encoding mode name, now a {@code {4}} placeholder.
	 */
	public static Message E063;

	/** {@link com.wudsn.tools.dis6502.model.ProfileLogic#load}'s "file is empty" error. */
	public static Message E064;

	/** {@link com.wudsn.tools.dis6502.model.Xml#load}'s "mismatched root element" error. */
	public static Message E065;

	/** {@link com.wudsn.tools.dis6502.ui.ComputerFont}'s {@code loadFont}'s "font resource not found" error. */
	public static Message E066;

	/**
	 * {@link com.wudsn.tools.dis6502.ui.SegmentWriteBootDiskDialog}'s {@code
	 * writeBootDisk}'s "no directory entries" error.
	 */
	public static Message E067;

	/**
	 * {@link com.wudsn.tools.dis6502.model.DisassemblyProgressMonitor#sendInfo}'s
	 * verbose per-byte trace log line.
	 */
	public static Message I068;

	/**
	 * {@link com.wudsn.tools.dis6502.model.DisassemblyProgressMonitor#setPass}'s
	 * "starting pass" log line.
	 */
	public static Message I069;

	/**
	 * {@link com.wudsn.tools.dis6502.model.DisassemblyProgressMonitor#setSegmentNumber}'s
	 * "starting segment" log line.
	 */
	public static Message I070;

	/** {@link Dis6502#performPasteMemoryInspectorSelection}'s "clipboard is not a valid hex byte sequence" error. */
	public static Message E071;

	/** {@link Dis6502#performPasteMemoryInspectorSelection}'s "would exceed the 64 KB segment size limit" error. */
	public static Message E072;

	/** {@link Dis6502#openFile}'s "file type could not be determined, offering it as a raw file" log line. */
	public static Message I073;

	// {@link com.wudsn.tools.dis6502.model.AtariError#getErrorText}'s texts, one per error, in the order of that enum.
	public static Message I074; // OK
	public static Message E075; // NO_ENTRY_FOUND
	public static Message E076; // END_OF_FILE
	public static Message E077; // DISK_NOT_FOUND
	public static Message E078; // DIRECTORY_NOT_FOUND
	public static Message E079; // DIRECTORY_READ
	public static Message E080; // DIRECTORY_WRITE
	public static Message E081; // INVALID_VTOC_ENTRY
	public static Message E082; // SECTOR_NOT_FOUND
	public static Message E083; // FILE_READ
	public static Message E084; // FILE_WRITE
	public static Message E085; // FILE_CORRUPTED
	public static Message E086; // FILE_ALREADY_EXISTS
	public static Message E087; // NO_FREE_SECTOR
	public static Message E088; // BITMAP_READ
	public static Message E089; // BITMAP_WRITE
	public static Message E090; // SECTOR_ALREADY_FREE
	public static Message E091; // FILE_SEEK

	/** {@link Dis6502#openDiskImageExecutableFile}'s "disk image could not be read" log line: the file, and the {@link com.wudsn.tools.dis6502.model.AtariError}'s text. */
	public static Message E092;
	public static Message E093;

	static {
		initializeClass(Messages.class, null);
	}
}
