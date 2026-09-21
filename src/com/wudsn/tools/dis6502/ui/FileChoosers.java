/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.ui;

import java.awt.Component;
import java.io.File;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.wudsn.tools.base.common.TextUtility;
import com.wudsn.tools.dis6502.Texts;
import com.wudsn.tools.dis6502.model.DefaultFolders;
import com.wudsn.tools.dis6502.model.FileType;
import com.wudsn.tools.dis6502.model.FileTypeInfo;

/**
 * Every "open"/"save" file chooser of the application, so they all behave
 * the same: which folder they start in, which files they show, and what
 * happens to the file name picked for saving.
 * <p>
 * Replaces the C++ version's {@code FileDialogs}, driven by the same {@link
 * FileTypeInfo}:
 * <ul>
 * <li>The folder to start in is, in this order: the suggested file's (when
 * saving something that already has a file); where a file of this type was
 * last picked in this session; where the most recently used file of this
 * type is ({@link MRUController#getLastFilePath}); the {@link
 * DefaultFolders} entry for the type's folder type. The C++ version has no
 * per-session memory, so its choosers for types that never reach the MRU
 * list (equates, profiles, disassembly files, saved segments) always start
 * in the default folder again.</li>
 * <li>The filter shows the type's extensions, e.g. "Executable Files
 * (*.bin, *.com, ...)"; "All Files" stays available. A type any file can
 * have (raw) has no filter.</li>
 * <li>A file saved without an extension gets the type's default one, and
 * overwriting an existing file has to be confirmed - {@code JFileChooser}
 * does neither on its own, where the Win32 dialog does both ({@code
 * lpstrDefExt}/{@code OFN_OVERWRITEPROMPT}).</li>
 * </ul>
 *
 * @author Peter Dell
 */
public final class FileChoosers {

	private final MRUController mruController;
	private final Supplier<DefaultFolders> defaultFoldersSupplier;
	private final Map<FileType, File> lastFolders = new EnumMap<>(FileType.class);

	/**
	 * @param defaultFoldersSupplier Supplies the default folders of the
	 *                               workspace's current computer system - a
	 *                               supplier, since they change with it.
	 */
	public FileChoosers(MRUController mruController, Supplier<DefaultFolders> defaultFoldersSupplier) {
		this.mruController = mruController;
		this.defaultFoldersSupplier = defaultFoldersSupplier;
	}

	/** Lets the user pick an existing file of the given type. Returns {@code null} if cancelled. */
	public File chooseOpenFile(Component parent, String title, FileType fileType) {
		JFileChooser fileChooser = createFileChooser(title, fileType, null);
		if (fileChooser.showOpenDialog(parent) != JFileChooser.APPROVE_OPTION) {
			return null;
		}
		File file = fileChooser.getSelectedFile();
		lastFolders.put(fileType, file.getParentFile());
		return file;
	}

	/**
	 * Lets the user pick the file to save something of the given type to.
	 * Returns {@code null} if cancelled, including by declining to overwrite
	 * an existing file.
	 *
	 * @param suggestedFile The file to preselect, or {@code null}.
	 */
	public File chooseSaveFile(Component parent, String title, FileType fileType, File suggestedFile) {
		JFileChooser fileChooser = createFileChooser(title, fileType, suggestedFile);
		if (fileChooser.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) {
			return null;
		}
		File file = withDefaultExtension(fileChooser.getSelectedFile(), FileTypeInfo.get(fileType));
		if (file.exists() && JOptionPane.showConfirmDialog(parent, TextUtility.format(Texts.FileChoosers_OverwriteMessage, file.getPath()),
				Texts.FileChoosers_OverwriteTitle, JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) {
			return null;
		}
		lastFolders.put(fileType, file.getParentFile());
		return file;
	}

	private JFileChooser createFileChooser(String title, FileType fileType, File suggestedFile) {
		FileTypeInfo fileTypeInfo = FileTypeInfo.get(fileType);

		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setDialogTitle(title);

		if (!fileTypeInfo.filterExtensions.isEmpty()) {
			String[] extensions = new String[fileTypeInfo.filterExtensions.size()];
			StringBuilder description = new StringBuilder(fileTypeInfo.filterText).append(" (");
			for (int i = 0; i < extensions.length; i++) {
				extensions[i] = fileTypeInfo.filterExtensions.get(i).substring(1); // Without the dot.
				description.append(i == 0 ? "*." : ", *.").append(extensions[i]);
			}
			fileChooser.setFileFilter(new FileNameExtensionFilter(description.append(")").toString(), extensions));
		}

		File mruFile = toFile(mruController.getLastFilePath(fileType));
		File initialFolder = getInitialFolder(suggestedFile, lastFolders.get(fileType), mruFile,
				toFile(defaultFoldersSupplier.get().getFolderPath(fileTypeInfo.folderType)));
		if (initialFolder != null) {
			fileChooser.setCurrentDirectory(initialFolder);
		}
		if (suggestedFile != null) {
			fileChooser.setSelectedFile(suggestedFile);
		}
		return fileChooser;
	}

	private static File toFile(String path) {
		return path == null || path.isEmpty() ? null : new File(path);
	}

	/**
	 * The folder a chooser starts in - see the class javadoc for the order.
	 * A candidate whose folder no longer exists is skipped. Returns {@code
	 * null} if none is left, for {@code JFileChooser}'s own default.
	 */
	static File getInitialFolder(File suggestedFile, File lastFolder, File mruFile, File defaultFolder) {
		File[] candidates = { suggestedFile == null ? null : suggestedFile.getAbsoluteFile().getParentFile(), lastFolder,
				mruFile == null ? null : mruFile.getAbsoluteFile().getParentFile(), defaultFolder };
		for (File candidate : candidates) {
			if (candidate != null && candidate.isDirectory()) {
				return candidate;
			}
		}
		return null;
	}

	/** Appends the type's default extension to a file name that has none - if the type has one. */
	static File withDefaultExtension(File file, FileTypeInfo fileTypeInfo) {
		if (file.getName().contains(".") || FileTypeInfo.ANY_EXTENSION.equals(fileTypeInfo.defaultExtension)) {
			return file;
		}
		return new File(file.getParentFile(), file.getName() + fileTypeInfo.defaultExtension);
	}
}
