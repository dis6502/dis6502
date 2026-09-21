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

	static {
		initializeClass(Messages.class, null);
	}
}
