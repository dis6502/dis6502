/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

import java.util.HashMap;
import java.util.Map;

/**
 * The remembered default folder path for each {@link FolderType}, for one
 * {@link ComputerSystemType}.
 * <p>
 * Ported from DefaultFolders.h / DefaultFolders.cpp. {@code
 * CopyFolderPath} (copying into a fixed-size Win32 buffer) is not ported -
 * {@link #getFolderPath} already returns the path directly.
 *
 * @author Peter Dell
 */
public final class DefaultFolders {

	private final ComputerSystemTypeInfo computerSystemTypeInfo;
	private final Map<String, String> folders = new HashMap<>();

	public DefaultFolders(ComputerSystemTypeInfo computerSystemTypeInfo) {
		this.computerSystemTypeInfo = computerSystemTypeInfo;
	}

	public ComputerSystemTypeInfo getComputerSystemTypeInfo() {
		return computerSystemTypeInfo;
	}

	public String getFolderPath(FolderType folderType) {
		return folders.getOrDefault(folderType.getKey(), "");
	}

	public void setFolderPath(FolderType folderType, String folderPath) {
		folders.put(folderType.getKey(), folderPath);
	}
}
