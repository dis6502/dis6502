/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.ui;

import javax.swing.JMenu;

import com.wudsn.tools.base.common.MRUList;
import com.wudsn.tools.base.gui.MRUMenu;
import com.wudsn.tools.dis6502.Application;
import com.wudsn.tools.dis6502.model.FileType;

/**
 * Tracks recently used workspace and non-workspace files in two WUDSN Base
 * {@link MRUList}s, and populates the "Recent Workspaces"/"Recent Files"
 * menus with {@link MRUMenu}.
 *
 * @author Peter Dell
 */
public final class MRUController {

	private static final int MRU_MAX_ENTRIES = 5;

	private final MRUList<FileType> workspaceList;
	private final MRUList<FileType> fileList;

	public MRUController(Application application) {
		workspaceList = new MRUList<>(FileType.class, application.getSettingsSection("RecentWorkspaces"),
				MRU_MAX_ENTRIES);
		fileList = new MRUList<>(FileType.class, application.getSettingsSection("RecentFiles"), MRU_MAX_ENTRIES);
	}

	public void load() {
		workspaceList.load();
		fileList.load();
	}

	public void save() {
		workspaceList.save();
		fileList.save();
	}

	/** Adds a file to the workspace or the file list - not one opened as {@link FileType#ANY_FILE}, which cannot be reopened as such. */
	public void addFile(String filePath, FileType fileType) {
		if (filePath.isEmpty() || fileType == FileType.ANY_FILE) {
			return;
		}
		if (fileType == FileType.WORKSPACE_FILE) {
			workspaceList.addFile(filePath, fileType);
		} else {
			fileList.addFile(filePath, fileType);
		}
	}

	/** The most recently used file of any type, workspaces included, or {@code ""}. */
	public String getLastFilePath() {
		if (!fileList.getEntries().isEmpty()) {
			return fileList.getEntries().get(0).getFilePath();
		}
		return workspaceList.getLastFilePath(FileType.WORKSPACE_FILE);
	}

	/** Whether the workspace or non-workspace recent list has at least one entry - see {@link #fillMenu}'s own disabling rule. */
	public boolean hasEntries(boolean workspaces) {
		return !(workspaces ? workspaceList : fileList).getEntries().isEmpty();
	}

	public String getLastFilePath(FileType fileType) {
		if (fileType == FileType.WORKSPACE_FILE) {
			return workspaceList.getLastFilePath(fileType);
		}
		return fileList.getLastFilePath(fileType);
	}

	/**
	 * Repopulates {@code menu} with the workspace or non-workspace recent
	 * entries (up to {@value #MRU_MAX_ENTRIES}) - see {@link MRUMenu#fill}.
	 * Selecting an item invokes {@code onSelect} with its entry.
	 */
	public void fillMenu(JMenu menu, boolean workspaces, MRUMenu.SelectionListener<FileType> onSelect) {
		MRUMenu.fill(menu, workspaces ? workspaceList : fileList, onSelect);
	}
}
