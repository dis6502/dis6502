/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

/**
 * The four sections a {@link DisassemblyResult} is split into, in display
 * order.
 * <p>
 * Ported from DisassemblySectionType.h.
 *
 * @author Peter Dell
 */
public enum DisassemblySectionType {
	SYSTEM_EQUATES,
	USER_EQUATES,
	CODE_EQUATES,
	CODE_LINES
}
