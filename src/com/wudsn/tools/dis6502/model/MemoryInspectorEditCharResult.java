/*
 * Copyright (C) 2025 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

/**
 * The outcome of {@link Workspace#typeMemoryInspectorEditChar}.
 *
 * @author Peter Dell
 */
public enum MemoryInspectorEditCharResult {

	/**
	 * The character was not valid input for the current pane (not a hex digit
	 * in a hex pane, not Tab, not a printable character or Enter in the ASCII
	 * pane) - nothing changed.
	 */
	NOT_HANDLED,

	/**
	 * The character was written (or, for Tab, switched panes) and the cursor
	 * moved to the next cell within the segment.
	 */
	HANDLED,

	/**
	 * The character was written at the segment's last byte - there is nowhere
	 * further to advance to, matching {@code MemoryInspectorControlImpl::Char}'s
	 * own end-of-buffer branch. The caller is expected to exit edit mode; see
	 * {@code com.wudsn.tools.dis6502.ui.MemoryInspectorPanel}'s class javadoc
	 * for the C++ bug this port fixes on this exact path.
	 */
	HANDLED_AT_BUFFER_END
}
