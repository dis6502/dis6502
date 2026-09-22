/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * A work list of addresses still to be traced, used by {@link
 * GuessCodeLogic} to walk a program's flow of control - its only real use
 * despite the generic name: it is not used for anything like "jump to
 * label, then go back".
 * <p>
 * A thin wrapper around {@link ArrayDeque}.
 *
 * @author Peter Dell
 */
public final class MemoryInspectorStack {

	private final Deque<Integer> stack = new ArrayDeque<>();

	public void clear() {
		stack.clear();
	}

	/** Returns the last pushed address, or {@code 0xFFFF} if the stack is empty. */
	public int popAddress() {
		Integer address = stack.pollLast();
		return address != null ? address : 0xFFFF;
	}

	public void pushAddress(int address) {
		stack.addLast(address);
	}
}
