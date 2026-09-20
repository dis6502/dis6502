/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

/**
 * Ported from ComputerSystemType.h ({@code ComputerSystemTypeInfo}).
 *
 * @author Peter Dell
 */
public final class ComputerSystemTypeInfo {

	public final ComputerSystemType type;
	public final String id;
	public final String text;
	public final String fileName;

	public ComputerSystemTypeInfo(ComputerSystemType type, String id, String text, String fileName) {
		this.type = type;
		this.id = id;
		this.text = text;
		this.fileName = fileName;
	}
}
