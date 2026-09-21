/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

/**
 * Type of one line in an equates file / one entry in an {@link EquateList}.
 * <p>
 * Ported from EquateType.h. {@code EquateTypeInfo}/{@code EquateTypeFactory}
 * (the key/text lookup) has no counterpart - nothing shows an equate type to
 * the user, and the key is the enum constant's name.
 *
 * @author Peter Dell
 */
public enum EquateType {
	UNKNOWN,
	EMPTY,
	COMMENT,
	LABEL
}
