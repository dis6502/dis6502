/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

/**
 * Properties of a {@code Workspace} that can change and be listened to.
 *
 * @author Peter Dell
 */
public enum WorkspaceProperty {
	COMPUTER_SYSTEM_TYPE,
	FONT,
	FILE_PATH,
	PROFILE,
	SEGMENTS,
	SELECTED_SEGMENT,
	SELECTED_MEMORY_RANGE,
	SYSTEM_EQUATES,
	USER_EQUATES
}
