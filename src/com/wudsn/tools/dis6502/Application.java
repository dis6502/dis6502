/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.prefs.Preferences;

import com.wudsn.tools.base.common.Log;
import com.wudsn.tools.base.common.TextUtility;
import com.wudsn.tools.base.repository.Message;

/**
 * Minimal application-wide message handling plus settings/module-path
 * access, ported from Application.h / Application.cpp.
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
 * {@link Text#IDS_LOG_TITLE}) directly, so {@code SendXxxMessageWithID}
 * becomes {@code sendXxxMessage} - or, for a {@link Messages} field, {@link
 * #sendMessage}. Message sending is also not a singleton
 * accessed through a {@code g_Application} global here - callers that need
 * it (like {@link com.wudsn.tools.dis6502.model.WorkspaceLogic}) take an
 * {@code Application} instance through their constructor instead.
 * <p>
 * This is concrete, not abstract, unlike the C++ version - there is no
 * ported UI layer yet to subclass it, and a plain instance is all any
 * ported logic class currently needs. {@link #getSettingsSection} is
 * backed by {@link Preferences} rather than a Windows INI file (see
 * {@link ApplicationSettingsSection}'s javadoc), and {@link
 * #getModuleFilePath} resolves relative to the directory containing this
 * class's own jar/classes (the closest cross-platform equivalent of "the
 * running executable's own folder") instead of using the Win32 module
 * handle APIs.
 *
 * @author Peter Dell
 */
public class Application {

	private final Map<String, ApplicationSettingsSection> settingsSections = new HashMap<>();

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

	/**
	 * Sends a {@link Message} (from {@link Messages}, unlike {@link
	 * Text}'s plain {@code String} fields) to the log at its own severity -
	 * {@link Message#ERROR} routes to {@link #sendErrorLogMessage}, every
	 * other severity ({@link Message#STATUS}/{@link Message#INFO}) to
	 * {@link #sendLogMessage} - instead of the caller having to pick {@link
	 * #sendInfoMessage}/{@link #sendErrorMessage} itself.
	 */
	public void sendMessage(Message message, String... args) {
		String text = message.format(args);
		if (message.getSeverity() == Message.ERROR) {
			sendErrorLogMessage(text);
		} else {
			sendLogMessage(text);
		}
	}

	public void sendErrorMessage(Throwable ex) {
		sendMessage(Messages.E005, String.valueOf(ex.getMessage()));
	}

	public ApplicationSettingsSection getSettingsSection(String name) {
		return settingsSections.computeIfAbsent(name,
				key -> new ApplicationSettingsSection(Preferences.userNodeForPackage(Application.class).node(key)));
	}

	/** Resolves {@code relativeFilePath} against the directory containing this application's own jar/classes. */
	public String getModuleFilePath(String relativeFilePath) {
		File baseDirectory = getModuleBaseDirectory();
		return relativeFilePath.isEmpty() ? baseDirectory.getPath() : new File(baseDirectory, relativeFilePath).getPath();
	}

	private static File getModuleBaseDirectory() {
		try {
			File location = new File(Application.class.getProtectionDomain().getCodeSource().getLocation().toURI());
			return location.isDirectory() ? location : location.getParentFile();
		} catch (URISyntaxException | NullPointerException | SecurityException ex) {
			return new File(".");
		}
	}
}
