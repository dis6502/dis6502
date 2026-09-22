/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

import java.util.HashMap;
import java.util.Map;

import com.wudsn.tools.dis6502.model.system.ComputerSystemType;

/**
 * The remembered default folder path for each {@link FolderType}, for one
 * {@link ComputerSystemType}.
 *
 * @author Peter Dell
 */
public final class DefaultFolders {

	private final ComputerSystemType computerSystemType;
	private final Map<String, String> folders = new HashMap<>();

	public DefaultFolders(ComputerSystemType computerSystemType) {
		this.computerSystemType = computerSystemType;
	}

	public ComputerSystemType getComputerSystemType() {
		return computerSystemType;
	}

	public String getFolderPath(FolderType folderType) {
		return folders.getOrDefault(folderType.getKey(), "");
	}

	public void setFolderPath(FolderType folderType, String folderPath) {
		folders.put(folderType.getKey(), folderPath);
	}
}
