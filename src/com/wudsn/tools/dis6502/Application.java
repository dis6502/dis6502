/*
 * Copyright (C) 2025 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502;

import java.io.IOException;

import com.wudsn.tools.base.common.Log;
import com.wudsn.tools.base.common.TextUtility;

/**
 * Minimal application-wide message handling, ported from Application.h /
 * Application.cpp.
 * <p>
 * The C++ version is an abstract base class: {@code GetModuleFilePath}/
 * {@code GetSettingsSection} are pure virtual (implemented by the concrete
 * application class in {@code ui/}, not ported yet), and message methods
 * take a {@code Text::TextID} that gets resolved through {@code
 * Application::GetText}/{@code Text::Get} at call time - the base class's
 * own {@code GetText} is itself just a stub returning the numeric ID as a
 * string, with the real resource-table lookup only present in the concrete
 * subclass. Since {@link Text}'s fields already hold their resolved text
 * once the class is loaded (see its javadoc), that whole indirection is
 * unnecessary here: methods take the already-resolved {@code String} (e.g.
 * {@link Text#IDS_LOG_OPEN_WORK}) directly, so {@code SendXxxMessageWithID}
 * becomes {@code sendXxxMessage}. Message sending is also not a singleton
 * accessed through a {@code g_Application} global here - callers that need
 * it (like {@link com.wudsn.tools.dis6502.model.WorkspaceLogic}) take an
 * {@code Application} instance through their constructor instead.
 * <p>
 * This is concrete, not abstract, unlike the C++ version - there is no
 * ported UI layer yet to subclass it, and a plain instance is all any
 * ported logic class currently needs.
 *
 * @author Peter Dell
 */
public class Application {

	protected void sendLogMessage(String text) {
		Log.logInfo("{0}", new Object[] { text });
	}

	protected void sendErrorLogMessage(String text) {
		Log.logError("{0}", new Object[] { text }, null);
	}

	public void sendInfoMessage(String text, String... args) {
		sendLogMessage(TextUtility.format(text, args));
	}

	public void sendErrorMessage(String text, String... args) {
		sendErrorLogMessage(TextUtility.format(text, args));
	}

	public void throwErrorMessage(String text, String... args) throws IOException {
		throw new IOException(TextUtility.format(text, args));
	}

	public void sendErrorMessage(Throwable ex) {
		sendErrorMessage(Text.IDS_ERR_EXCEPTION, String.valueOf(ex.getMessage()));
	}
}
