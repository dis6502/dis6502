/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

import com.wudsn.tools.dis6502.Application;
import com.wudsn.tools.dis6502.Messages;

/**
 * Reports progress of a {@link Disassembly} run and allows cancelling it.
 * <p>
 * {@link #setPass}/{@link #setSegmentNumber}/{@link #sendInfo} log through
 * {@link Application#sendMessage}. {@link
 * com.wudsn.tools.dis6502.ui.DisassemblyProgressDialog}'s own {@code
 * Monitor} subclass - the only one actually used by the running
 * application - overrides {@link #setPass}/{@link #setSegmentNumber} to
 * update its own UI labels instead of logging, without calling the base
 * class: this class's own logging only actually fires for a plain,
 * undecorated monitor, such as in a test harness.
 *
 * @author Peter Dell
 */
public class DisassemblyProgressMonitor {

	private final Application application;

	protected String pass = "";

	private boolean verbose;

	public DisassemblyProgressMonitor(Application application) {
		this.application = application;
	}

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
		// INFO: Pass {0}
		application.sendMessage(Messages.I069, pass);
	}

	public void setSegmentNumber(int segmentNumber) {
		// INFO: Segment {0}
		application.sendMessage(Messages.I070, String.valueOf(segmentNumber));
	}

	public void sendInfo(String message) {
		// INFO: Pass {0} - {1}
		application.sendMessage(Messages.I068, pass, message);
	}

	public boolean isCancelled() {
		return false;
	}
}
