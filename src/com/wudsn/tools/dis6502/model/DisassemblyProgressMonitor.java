/*
 * Copyright (C) 2025 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

/**
 * Reports progress of a {@link Disassembly} run and allows cancelling it.
 * <p>
 * Ported from DisassemblyProgressMonitor.h / DisassemblyProgressMonitor.cpp.
 * The C++ version's {@code SetPass}/{@code SetSegmentNumber}/{@code
 * SendInfo} log through the global {@code Application} object; that is not
 * ported yet, so they default to no-ops here - override them once
 * application-level logging exists. {@code StartDisassembly} (the timing
 * wrapper that calls {@code Disassembly::DisassembleInternal}) is not
 * ported yet either, deferred to when {@code Disassembly.disassembleInternal}
 * itself is ported.
 *
 * @author Peter Dell
 */
public class DisassemblyProgressMonitor {

	protected String pass = "";

	private boolean verbose;

	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}

	public boolean isVerbose() {
		return verbose;
	}

	public void setPass(String pass) {
		this.pass = pass;
	}

	public void setSegmentNumber(int segmentNumber) {
	}

	public void sendInfo(String message) {
	}

	public boolean isCancelled() {
		return false;
	}
}
