/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

import java.util.Comparator;

/**
 * An SDX system symbol to fix up in a {@link Segment}: the address of the
 * word to fix up, and the symbol name to write there.
 * <p>
 * Has no XML persistence: it is a transient attribute of {@code Segment},
 * rebuilt by every disassembly.
 *
 * @author Peter Dell
 */
public final class Symbol {

	public static final Comparator<Symbol> BY_ADDRESS = Comparator.comparingInt(Symbol::getAddress);

	private final int address; // Address of the word to fix up in the segment.
	private final String symbol; // Symbol name to fix up.

	public Symbol(int address, String symbol) {
		this.address = address;
		this.symbol = symbol;
	}

	public int getAddress() {
		return address;
	}

	public String getSymbol() {
		return symbol;
	}
}
