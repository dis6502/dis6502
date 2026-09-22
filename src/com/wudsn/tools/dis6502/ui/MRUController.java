/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.ui;

import java.util.List;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

import com.wudsn.tools.dis6502.Application;
import com.wudsn.tools.dis6502.model.FileType;
import com.wudsn.tools.dis6502.model.MRUEntry;
import com.wudsn.tools.dis6502.model.MRUList;

/**
 * Tracks recently used workspace and non-workspace files, and populates the
 * "Recent Workspaces"/"Recent Files" menus.
 * <p>
 * Ported from ui/MRUController.h / MRUController.cpp and ui/MRUMenu.h /
 * MRUMenu.cpp, folded together - {@code MRUMenu}'s only job (filling a
 * native menu with up to 5 numbered items and mapping a chosen item's
 * command ID back to its {@link MRUEntry}) is simple enough in Swing, where
 * each {@link JMenuItem} can just carry its own {@link
 * java.awt.event.ActionListener}, not to need a separate class.
 *
 * @author Peter Dell
 */
public final class MRUController {

	private static final int MRU_MAX_ENTRIES = 5;

	private final MRUList workspaceList;
	private final MRUList fileList;

	public MRUController(Application application) {
		workspaceList = new MRUList(application, "RecentWorkspaces", MRU_MAX_ENTRIES);
		fileList = new MRUList(application, "RecentFiles", MRU_MAX_ENTRIES);
	}

	public void load() {
		workspaceList.load();
		fileList.load();
	}

	public void save() {
		workspaceList.save();
		fileList.save();
	}

	public void addFile(String filePath, FileType fileType) {
		if (filePath.isEmpty()) {
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

	public String getLastFilePath(FileType fileType) {
		if (fileType == FileType.WORKSPACE_FILE) {
			return workspaceList.getLastFilePath(fileType);
		}
		return fileList.getLastFilePath(fileType);
	}

	/**
	 * Repopulates {@code menu} with the workspace or non-workspace recent
	 * entries (up to {@value #MRU_MAX_ENTRIES}, numbered as in the C++
	 * source's "&amp;1 ...", "&amp;2 ..." menu items), disabling it if there
	 * are none. Selecting an item invokes {@code onSelect} with its entry.
	 */
	public void fillMenu(JMenu menu, boolean workspaces, MRUEntrySelectionListener onSelect) {
		menu.removeAll();
		List<MRUEntry> entries = (workspaces ? workspaceList : fileList).getEntries();
		menu.setEnabled(!entries.isEmpty());

		int index = 1;
		for (MRUEntry entry : entries) {
			JMenuItem item = new JMenuItem(index + " " + entry.getFilePath());
			item.addActionListener(e -> onSelect.onSelect(entry));
			menu.add(item);
			index++;
		}
	}

	public interface MRUEntrySelectionListener {
		void onSelect(MRUEntry entry);
	}
}
