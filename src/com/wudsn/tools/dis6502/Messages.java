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

	static {
		initializeClass(Messages.class, null);
	}
}
