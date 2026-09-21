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
 * by a plain sequence number - not a ported C++ {@code IDS_*} resource ID.
 * {@link #I001}, for instance, has no C++ resource name of its own to trace
 * back to (unlike every field in {@link Text}) - it was moved here from
 * {@code Text.IDS_LOG_BETA_MESSAGE}, which did.
 *
 * @author Peter Dell
 */
public final class Messages extends NLS {

	/** {@link Dis6502#run}'s startup warning message, sent via {@link Application#sendMessage}. */
	public static Message I001;

	static {
		initializeClass(Messages.class, null);
	}
}
