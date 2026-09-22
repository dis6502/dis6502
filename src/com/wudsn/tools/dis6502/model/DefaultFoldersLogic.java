/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

import java.util.List;

import com.wudsn.tools.dis6502.Application;
import com.wudsn.tools.dis6502.ApplicationSettingsSection;

/**
 * Creates, loads, and saves a {@link DefaultFolders} instance.
 *
 * @author Peter Dell
 */
public final class DefaultFoldersLogic {

	private static final List<FolderType> FOLDER_TYPES = FolderType.getValues();

	private final Application application;

	public DefaultFoldersLogic(Application application) {
		this.application = application;
	}

	public DefaultFolders createDefaultFolders(ComputerSystemType computerSystemType) {
		DefaultFolders defaultFolders = new DefaultFolders(computerSystemType);

		// Set default for all folder paths based on application module path.
		String folderPath = application.getModuleFilePath("");

		defaultFolders.setFolderPath(FolderType.RAW_FILES, folderPath);
		defaultFolders.setFolderPath(FolderType.EXECUTABLE_FILES, folderPath);
		defaultFolders.setFolderPath(FolderType.ROM_IMAGE_FILES, folderPath);
		defaultFolders.setFolderPath(FolderType.CASSETTE_IMAGE_FILES, folderPath);
		defaultFolders.setFolderPath(FolderType.DISK_IMAGE_FILES, folderPath);
		defaultFolders.setFolderPath(FolderType.WORKSPACE_FILES, folderPath);
		defaultFolders.setFolderPath(FolderType.EQUATES_FILES, folderPath);
		defaultFolders.setFolderPath(FolderType.DISASSEMBLY_FILES, folderPath);

		defaultFolders.setFolderPath(FolderType.PROFILE_FILES, application.getModuleFilePath("profiles"));

		return defaultFolders;
	}

	public void load(DefaultFolders defaultFolders) {
		ApplicationSettingsSection settings = application
				.getSettingsSection(defaultFolders.getComputerSystemType().getId());

		for (FolderType folderType : FOLDER_TYPES) {
			String folderPath = settings.getString(folderType.getKey(), "");
			if (!folderPath.isEmpty()) {
				defaultFolders.setFolderPath(folderType, folderPath);
			}
		}
	}

	public void save(DefaultFolders defaultFolders) {
		ApplicationSettingsSection settings = application
				.getSettingsSection(defaultFolders.getComputerSystemType().getId());

		for (FolderType folderType : FOLDER_TYPES) {
			settings.writeString(folderType.getKey(), defaultFolders.getFolderPath(folderType));
		}
	}
}
