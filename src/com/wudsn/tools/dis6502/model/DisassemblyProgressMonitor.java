/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
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
 * application-level logging exists.
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

	/** Runs {@code disassembly}'s disassembly. @return the elapsed time in microseconds. */
	public long startDisassembly(Disassembly disassembly) {
		long start = System.nanoTime();
		disassembleInternal(disassembly);
		long stop = System.nanoTime();
		return (stop - start) / 1000;
	}

	protected void disassembleInternal(Disassembly disassembly) {
		disassembly.disassembleInternal();
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
