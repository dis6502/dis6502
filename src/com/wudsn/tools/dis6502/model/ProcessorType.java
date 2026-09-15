/*
 * Copyright (C) 2025 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

/**
 * Ported from ProcessorType.h. {@code ProcessorTypeInfo}/{@code
 * ProcessorTypeFactory} (the UI-facing key/text lookup) is not ported yet.
 *
 * @author Peter Dell
 */
public enum ProcessorType {
	UNKNOWN,
	MOS6502,
	MOS65C02
}
