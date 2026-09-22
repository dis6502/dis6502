/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;

import com.wudsn.tools.dis6502.Application;
import com.wudsn.tools.dis6502.ApplicationSettingsSection;
import com.wudsn.tools.dis6502.Messages;
import com.wudsn.tools.dis6502.model.system.ComputerSystemType;

/**
 * Loads and saves a {@link Profile}, trying the legacy binary format
 * ({@link Profile1X}) before falling back to the modern XML format.
 *
 * @author Peter Dell
 */
public final class ProfileLogic {

	private final Application application;

	public ProfileLogic(Application application) {
		this.application = application;
	}

	public void loadDefaultProfile(Profile profile, ComputerSystemType computerSystemType) {
		ApplicationSettingsSection settingsSection = application.getSettingsSection(computerSystemType.getId());
		String filePath = settingsSection.getString("LastProfile", "");
		if (!filePath.isEmpty()) {
			load(profile, filePath);
		}
	}

	/** Loads a profile from disk (the legacy binary format or the modern XML format). Returns {@code false}, and logs, instead of throwing. */
	public boolean load(Profile profile, String filePath) {
		application.sendMessage(Messages.I010, filePath);

		try {
			File file = new File(filePath);
			byte[] buffer = Files.readAllBytes(file.toPath());
			if (buffer.length == 0) {
				throw new IOException(Messages.E064.format(filePath));
			}
			if (!Profile1X.load(profile, buffer, application)) {
				Xml.load(profile, "Profile", file);
			}
			return true;
		} catch (IOException ex) {
			application.sendMessage(Messages.E002);
			application.sendErrorMessage(ex);
			return false;
		}
	}

	public boolean loadAndSetDefaultProfile(Profile profile, ComputerSystemType computerSystemType,
			String filePath) {
		if (load(profile, filePath)) {
			ApplicationSettingsSection settingsSection = application.getSettingsSection(computerSystemType.getId());
			settingsSection.writeString("LastProfile", filePath);
			return true;
		}
		return false;
	}

	public void save(Profile profile, String filePath) {
		application.sendMessage(Messages.I013, filePath);

		try (OutputStream outputStream = new FileOutputStream(filePath)) {
			Xml.save(profile, "Profile", outputStream);
		} catch (IOException ex) {
			application.sendErrorMessage(ex);
		}
	}
}
