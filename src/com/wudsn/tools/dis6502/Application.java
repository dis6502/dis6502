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
 * access.
 * <p>
 * Message methods take the already-resolved {@code String} directly (e.g.
 * {@link Texts#LogPanel_Title}) since {@link Texts}' fields already hold
 * their resolved text once the class is loaded (see its javadoc) - or,
 * for a {@link Messages} field, {@link #sendMessage}. Message sending is
 * also not a singleton accessed through a global here - callers that need
 * it (like {@link com.wudsn.tools.dis6502.model.WorkspaceLogic}) take an
 * {@code Application} instance through their constructor instead.
 * <p>
 * This class is concrete: a plain instance is all the model classes and
 * the unit tests need, and {@code com.wudsn.tools.dis6502.ui.UIApplication}
 * only adds routing the messages to the log panel on top. {@link
 * #getSettingsSection} is backed by {@link Preferences} rather than a
 * Windows INI file (see {@link ApplicationSettingsSection}'s javadoc), and
 * {@link #getModuleFilePath} resolves relative to the directory containing
 * this class's own jar/classes (the closest cross-platform equivalent of
 * "the running executable's own folder").
 *
 * @author Peter Dell
 */
public class Application {

	/**
	 * System property naming a child node of the application's {@link
	 * Preferences} node to keep all settings under, e.g. {@code
	 * -Ddis6502.settingsNode=test}. Set by {@code TestRunner} so that tests
	 * which start the real application (which builds its own {@link
	 * Application}) never touch the user's settings; the tests remove that
	 * node afterwards. Unset in normal use.
	 */
	public static final String SETTINGS_NODE_PROPERTY = "dis6502.settingsNode";

	private final Preferences settingsRoot;
	private final Map<String, ApplicationSettingsSection> settingsSections = new HashMap<>();

	/** Keeps its settings under {@link #getDefaultSettingsRoot()}. */
	public Application() {
		this(getDefaultSettingsRoot());
	}

	/** Keeps its settings under {@code settingsRoot} - a throw-away node, for a test. */
	public Application(Preferences settingsRoot) {
		if (settingsRoot == null) {
			throw new IllegalArgumentException("Parameter 'settingsRoot' must not be null.");
		}
		this.settingsRoot = settingsRoot;
	}

	/** The user's node for this application, or the child of it that {@link #SETTINGS_NODE_PROPERTY} names. */
	public static Preferences getDefaultSettingsRoot() {
		Preferences root = Preferences.userNodeForPackage(Application.class);
		String nodeName = System.getProperty(SETTINGS_NODE_PROPERTY, "");
		return nodeName.isEmpty() ? root : root.node(nodeName);
	}

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
		return settingsSections.computeIfAbsent(name, key -> new ApplicationSettingsSection(settingsRoot.node(key)));
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
