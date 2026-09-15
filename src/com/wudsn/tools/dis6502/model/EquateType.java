/*
 * Copyright (C) 2025 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

/**
 * Type of one line in an equates file / one entry in an {@link EquateList}.
 * <p>
 * Ported from EquateType.h. {@code EquateTypeInfo}/{@code EquateTypeFactory}
 * (the UI-facing key/text lookup) is not ported yet.
 *
 * @author Peter Dell
 */
public enum EquateType {
	UNKNOWN,
	EMPTY,
	COMMENT,
	LABEL
}
