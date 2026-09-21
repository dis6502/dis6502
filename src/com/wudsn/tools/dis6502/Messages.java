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
 * Message}) - unlike {@link Text}/{@link Texts}, whose fields are plain
 * {@code String}s. Follows the same pattern {@code
 * com.wudsn.tools.base.Messages} already uses in the shared WUDSN library:
 * a field's own name encodes its severity via a leading {@code S}/{@code
 * I}/{@code E} for {@link Message#STATUS}/{@link Message#INFO}/{@link
 * Message#ERROR} (see {@code
 * com.wudsn.tools.base.repository.NLS}'s field-loading switch), followed
 * by a plain sequence number, shared across every severity (an {@code E}
 * field's number does not restart at 1 - it just continues on from
 * whatever {@code I}/{@code S} number came before it) - not a ported C++
 * {@code IDS_*} resource ID, even where (like every field here so far) the
 * message text itself was moved from one: any real C++ {@code
 * IDS_ERR_*}/{@code IDS_LOG_*} constant whose only use was a {@code
 * sendErrorMessage}/{@code sendInfoMessage} call belongs here instead of
 * {@link Text}, once it needs its severity to actually drive dispatch via
 * {@link Application#sendMessage}.
 *
 * @author Peter Dell
 */
public final class Messages extends NLS {

	/** {@link Dis6502#run}'s startup warning message - moved from {@code Text.IDS_LOG_BETA_MESSAGE}. */
	public static Message I001;

	/** {@link com.wudsn.tools.dis6502.model.ProfileLogic#load}'s "not a valid profile" error - moved from {@code Text.IDS_ERR_BAD_PROFILE}. */
	public static Message E002;

	/** {@link com.wudsn.tools.dis6502.model.WorkspaceLogic#load}'s "not a valid workspace" error - moved from {@code Text.IDS_ERR_BAD_WORKSPACE}. */
	public static Message E003;

	/** {@link com.wudsn.tools.dis6502.model.EquateListLogic#load}'s per-line parse error - moved from {@code Text.IDS_ERR_CANNOT_PARSE_EQUATE_LINE}. */
	public static Message E004;

	/** {@link Application#sendErrorMessage(Throwable)}'s generic exception wrapper - moved from {@code Text.IDS_ERR_EXCEPTION}. */
	public static Message E005;

	/** {@link Dis6502#performWriteBootDisk}'s "no segment in memory" error - moved from {@code Text.IDS_ERR_NO_SEGMENT}. */
	public static Message E006;

	/**
	 * {@link com.wudsn.tools.dis6502.model.WorkspaceLogic#addRawSegment}/
	 * {@link com.wudsn.tools.dis6502.model.WorkspaceLogic#addDiskImageBootSectorsSegment}'s
	 * file-read error - moved from {@code Text.IDS_ERR_READING_FILE}.
	 */
	public static Message E007;

	/** {@link com.wudsn.tools.dis6502.model.EquateListLogic#load}'s equate-count summary - moved from {@code Text.IDS_EQUATE_LIST_LOADED}. */
	public static Message I008;

	/** {@link com.wudsn.tools.dis6502.model.EquateListLogic#load}'s "opening file" log line - moved from {@code Text.IDS_LOG_OPEN_EQUATE_FILE}. */
	public static Message I009;

	/** {@link com.wudsn.tools.dis6502.model.ProfileLogic#load}'s "opening file" log line - moved from {@code Text.IDS_LOG_OPEN_PROFILE_FILE}. */
	public static Message I010;

	/** {@link com.wudsn.tools.dis6502.model.WorkspaceLogic#load}'s "opening file" log line - moved from {@code Text.IDS_LOG_OPEN_WORK}. */
	public static Message I011;

	/** {@link com.wudsn.tools.dis6502.model.EquateListLogic#save}'s "saving file" log line - moved from {@code Text.IDS_LOG_SAVE_EQUATE_FILE}. */
	public static Message I012;

	/** {@link com.wudsn.tools.dis6502.model.ProfileLogic#save}'s "saving file" log line - moved from {@code Text.IDS_LOG_SAVE_PROFILE_FILE}. */
	public static Message I013;

	/** {@link com.wudsn.tools.dis6502.model.WorkspaceLogic#save}'s "saving file" log line - moved from {@code Text.IDS_LOG_SAVE_WORKSPACE_FILE}. */
	public static Message I014;

	/** {@link Dis6502#performMergeSegments}'s merged-count summary - moved from {@code Text.IDS_LOG_SEGMENTS_MERGED}. */
	public static Message I015;

	/** {@link Dis6502#performNewWorkspace}'s "new workspace prepared" log line - moved from {@code Text.IDS_MAIN_FILE_LOG_NEW_WORKSPACE_PREPARED}. */
	public static Message I016;

	/** {@link Dis6502#performOpenFile}'s "opening file" log line for a cassette image - moved from {@code Text.IDS_LOG_OPEN_CASSETTE_FILE}. */
	public static Message I017;

	/** {@link Dis6502#performOpenDiskImageBootSectors}'s "opening file" log line - moved from {@code Text.IDS_LOG_OPEN_DISK_IMAGE_BOOT_SECTORS}. */
	public static Message I018;

	/** {@link Dis6502#performOpenDiskImageExecutableFile}'s "opening file" log line - moved from {@code Text.IDS_LOG_OPEN_DISK_IMAGE_EXECUTABLE_FILE}. */
	public static Message I019;

	/**
	 * {@link Dis6502#performOpenDiskImageSectors}'s "opening file" log line -
	 * moved from {@code Text.IDS_LOG_OPEN_DISK_IMAGE_SECTORS}, fixing a real
	 * bug found while moving it: the C++ resource string ({@code dis6502.rc})
	 * uses {@code {0}} twice (item count and disk path) instead of {@code {0}}/
	 * {@code {1}}, even though the C++ call site already passes two distinct
	 * arguments - fixed here and, separately, in the C++ {@code .rc} source
	 * too (see that repo's own commit).
	 */
	public static Message I020;

	/** {@link Dis6502#performOpenFile}'s "opening file" log line for an executable file - moved from {@code Text.IDS_LOG_OPEN_EXECUTABLE_FILE}. */
	public static Message I021;

	/** {@link Dis6502#performOpenRawFile}'s "opening file" log line - moved from {@code Text.IDS_LOG_OPEN_RAW_FILE}. */
	public static Message I022;

	/** {@link Dis6502#performOpenFile}'s "opening file" log line for a ROM image - moved from {@code Text.IDS_LOG_OPEN_ROM_IMAGE_FILE}. */
	public static Message I023;

	/** {@link com.wudsn.tools.dis6502.model.DisassemblyResultFile#openWriter}'s "saving file" log line - moved from {@code Text.IDS_LOG_SAVE_DISASSEMBLY}. */
	public static Message I024;

	/** {@link Dis6502#performSetMemoryInspectorLoHiType}'s "LOBYTE/HIBYTE only valid for immediate operand" error - moved from {@code Text.IDS_ERR_BAD_MODE_FOR_LOHI}. */
	public static Message E025;

	/** {@link com.wudsn.tools.dis6502.model.DiskImage#displayError}'s "bad ATR signature" error - moved from {@code Text.IDS_ERR_IMG_BAD_MAGIC}. */
	public static Message E026;

	/** {@link com.wudsn.tools.dis6502.model.DiskImage#displayError}'s "disk error" error - moved from {@code Text.IDS_ERR_IMG_DISK_ERROR}. */
	public static Message E027;

	/** {@link com.wudsn.tools.dis6502.model.DiskImage#displayError}'s "file does not exist" error - moved from {@code Text.IDS_ERR_IMG_FILE_NOT_FOUND}. */
	public static Message E028;

	/** {@link com.wudsn.tools.dis6502.model.DiskImage#displayError}'s "sector out of range" error - moved from {@code Text.IDS_ERR_IMG_OUT_OF_RANGE}. */
	public static Message E029;

	/** {@link com.wudsn.tools.dis6502.model.DiskImage#displayError}'s "write protected" error - moved from {@code Text.IDS_ERR_IMG_WRITE_PROTECT}. */
	public static Message E030;

	/** {@link Dis6502#performSetMemoryInspectorLoHiType}'s "LOBYTE/HIBYTE not allowed for first byte" error - moved from {@code Text.IDS_ERR_LOHI_FIRST}. */
	public static Message E031;

	/** {@link Dis6502#performSetMemoryInspectorLoHiType}'s "LOBYTE/HIBYTE on multi-byte selection" error - moved from {@code Text.IDS_ERR_MULTI_LOHI}. */
	public static Message E032;

	/** {@link Dis6502#performOpenDiskImageExecutableFile}'s "no file found in disk image" error - moved from {@code Text.IDS_ERR_NO_ATARI_FILE}. */
	public static Message E033;

	/** {@link Dis6502#performOpenDiskImageBootSectors}'s "disk image is not bootable" error - moved from {@code Text.IDS_ERR_NO_BOOT}. */
	public static Message E034;

	/**
	 * {@link com.wudsn.tools.dis6502.model.SegmentListInserter#insertSegment}'s
	 * "no free segment" error - moved from {@code Text.IDS_ERR_NO_FREE_SEG},
	 * fixing a real bug found while moving it: the C++ resource string ({@code
	 * dis6502.rc}) says the maximum is 256, but {@code SegmentList::MAX_SEGMENTS}
	 * (checked by the very code that throws this error) is actually 4096 - fixed
	 * here and, separately, in the C++ {@code .rc} source too (see that repo's
	 * own commit).
	 */
	public static Message E035;

	/** {@link Dis6502#performOpenDiskImageExecutableFile}'s "disk image corrupted" fallback error - moved from {@code Text.IDS_ERR_READING_ATR}. */
	public static Message E036;

	/** {@link com.wudsn.tools.dis6502.ui.SegmentPropertiesDialog#performOK}'s "segment overlap" error - moved from {@code Text.IDS_ERR_SEGMENT_OVERLAP}. */
	public static Message E037;

	static {
		initializeClass(Messages.class, null);
	}
}
