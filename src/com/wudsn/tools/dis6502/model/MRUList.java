/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.wudsn.tools.dis6502.Application;
import com.wudsn.tools.dis6502.ApplicationSettingsSection;

/**
 * A "most recently used" file list, persisted through an {@link
 * ApplicationSettingsSection}.
 * <p>
 * Ported from MRUList.h / MRUList.cpp. Found and fixed a bug in {@link
 * #save()} while porting it: the C++ version wrote both the file path and
 * the file type to the same {@code "FileType_" + suffix} key (the file
 * path should have gone to {@code "FilePath_" + suffix}, the key {@link
 * #load()} actually reads back), so the second write silently clobbered
 * the first and no file path was ever actually saved - fixed upstream (see
 * that commit) and correct here from the start.
 *
 * @author Peter Dell
 */
public final class MRUList {

	private final ApplicationSettingsSection settingsSection;
	private final int maxEntries;
	private final List<MRUEntry> entries = new ArrayList<>();

	public MRUList(Application application, String sectionName, int maxEntries) {
		this.settingsSection = application.getSettingsSection(sectionName);
		this.maxEntries = maxEntries;
	}

	public void clear() {
		entries.clear();
	}

	public void addFile(String filePath, FileType fileType) {
		addEntryAndReorder(new MRUEntry(filePath, fileType));
	}

	private void addEntryAndReorder(MRUEntry newEntry) {
		Iterator<MRUEntry> it = entries.iterator();
		while (it.hasNext()) {
			MRUEntry entry = it.next();
			if (entry.getFilePath().equalsIgnoreCase(newEntry.getFilePath())) {
				it.remove();
				break;
			}
		}

		if (entries.size() >= maxEntries) {
			entries.remove(entries.size() - 1);
		}

		entries.add(0, newEntry);
	}

	public List<MRUEntry> getEntries() {
		return Collections.unmodifiableList(entries);
	}

	public String getLastFilePath(FileType fileType) {
		for (MRUEntry entry : entries) {
			if (entry.getFileType() == fileType) {
				return entry.getFilePath();
			}
		}
		return "";
	}

	public void load() {
		clear();

		for (int i = 1; i <= maxEntries; i++) {
			String suffix = String.valueOf(i);

			String filePath = settingsSection.getString("FilePath_" + suffix, "");
			if (filePath.isEmpty()) {
				continue;
			}

			String fileTypeString = settingsSection.getString("FileType_" + suffix, "");
			if (fileTypeString.isEmpty()) {
				continue;
			}

			FileType fileType = FileType.fromKey(fileTypeString);
			if (fileType != FileType.ANY_FILE) {
				entries.add(new MRUEntry(filePath, fileType));
			}
		}
	}

	public void save() {
		int index = 1;

		for (MRUEntry entry : entries) {
			String suffix = String.valueOf(index);
			settingsSection.writeString("FilePath_" + suffix, entry.getFilePath());
			settingsSection.writeString("FileType_" + suffix, entry.getFileType().getKey());
			index++;
		}
	}
}
