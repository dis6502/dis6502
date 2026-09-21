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
 * Ported from DisassemblyProgressMonitor.h / DisassemblyProgressMonitor.cpp.
 * {@code SetPass}/{@code SetSegmentNumber}/{@code SendInfo} log through the
 * global {@code Application} object there; {@link #setPass}/{@link
 * #setSegmentNumber}/{@link #sendInfo} do the equivalent here via {@link
 * Application#sendMessage}. {@link
 * com.wudsn.tools.dis6502.ui.DisassemblyProgressDialog}'s own {@code
 * Monitor} subclass - the only one actually used by the running
 * application - overrides {@link #setPass}/{@link #setSegmentNumber} to
 * update its own UI labels instead of logging, exactly like C++'s {@code
 * DisassemblyProgressDialog::SetPass}/{@code SetSegmentNumber} override the
 * base class without calling it: this class's own logging only actually
 * fires for a plain, undecorated monitor (matching C++, where only {@code
 * MainTest.cpp}'s test harness uses the base class directly).
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
		application.sendMessage(Messages.I069, pass);
	}

	public void setSegmentNumber(int segmentNumber) {
		application.sendMessage(Messages.I070, String.valueOf(segmentNumber));
	}

	public void sendInfo(String message) {
		application.sendMessage(Messages.I068, pass, message);
	}

	public boolean isCancelled() {
		return false;
	}
}
