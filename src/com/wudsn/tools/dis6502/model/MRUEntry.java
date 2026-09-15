/*
 * Copyright (C) 2025 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

/**
 * One "most recently used" file entry: its path plus the {@link FileType}
 * it was opened as.
 * <p>
 * Ported from MRUEntry.h / MRUEntry.cpp. The C++ version's {@code Create}
 * factory method (returning a raw, list-managed pointer) has no Java
 * equivalent need - a plain constructor is enough, since the JVM garbage
 * collector owns lifetime instead of {@link MRUList} explicitly deleting
 * entries.
 *
 * @author Peter Dell
 */
public final class MRUEntry {

	private final String filePath;
	private final FileType fileType;

	public MRUEntry(String filePath, FileType fileType) {
		this.filePath = filePath;
		this.fileType = fileType;
	}

	public String getFilePath() {
		return filePath;
	}

	public FileType getFileType() {
		return fileType;
	}
}
