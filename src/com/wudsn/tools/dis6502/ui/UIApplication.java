/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.ui;

import com.wudsn.tools.dis6502.Application;

/**
 * The concrete {@link Application} used by the Swing UI: routes log/error
 * messages to a {@link LogPanel} once one is attached, in addition to the
 * base class's {@code com.wudsn.tools.base.common.Log} logging.
 * <p>
 * Ported from ui/UIApplication.h / UIApplication.cpp, reduced to what the
 * base {@link Application} leaves for a subclass to add: {@code
 * GetModuleFilePath}/{@code GetSettingsSection} are already concrete in the
 * base class (see its javadoc), so only the log-routing responsibility
 * ({@code SetLogListWindow} in C++) is left here. {@code GetInstanceHandle}
 * is Win32-specific (an {@code HINSTANCE}) with no Java equivalent need.
 * {@code SetClipboardText} does have a real Java equivalent -
 * {@link java.awt.datatransfer.Clipboard} - but it is used directly where
 * needed (see {@code Dis6502#performCopyMemoryInspectorSelection}) rather
 * than through this class, since Swing/AWT clipboard access needs
 * nothing {@link Application} provides.
 *
 * @author Peter Dell
 */
public final class UIApplication extends Application {

	private LogPanel logPanel;

	public void setLogPanel(LogPanel logPanel) {
		this.logPanel = logPanel;
	}

	@Override
	protected void sendLogMessage(String text) {
		super.sendLogMessage(text);
		if (logPanel != null) {
			logPanel.appendLine(text);
		}
	}

	@Override
	protected void sendErrorLogMessage(String text) {
		super.sendErrorLogMessage(text);
		if (logPanel != null) {
			logPanel.appendErrorLine("ERROR: " + text);
		}
	}
}
