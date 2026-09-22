/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.ui;

import java.awt.Component;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.wudsn.tools.base.common.TextUtility;
import com.wudsn.tools.dis6502.Texts;
import com.wudsn.tools.dis6502.model.DefaultFolders;
import com.wudsn.tools.dis6502.model.FileType;
import com.wudsn.tools.dis6502.model.system.ComputerSystem;

/**
 * Every "open"/"save" file chooser of the application, so they all behave
 * the same: which folder they start in, which files they show, and what
 * happens to the file name picked for saving.
 * <p>
 * Driven by what each {@link FileType} knows about itself:
 * <ul>
 * <li>The folder to start in is, in this order: the suggested file's (when
 * saving something that already has a file); where a file of this type was
 * last picked in this session; where the most recently used file of this
 * type is ({@link MRUController#getLastFilePath}); the {@link
 * DefaultFolders} entry for the type's folder type.</li>
 * <li>The filter shows the type's extensions, e.g. "Executable Files
 * (*.bin, *.com, ...)"; "All Files" stays available. A type any file can
 * have (raw) has no filter, and {@link FileType#ANY_FILE} - "open a file,
 * find out what it is" ({@link #chooseOpenAnyFile}) - filters for
 * everything the computer system can read at once.</li>
 * <li>A file saved without an extension gets the type's default one, and
 * overwriting an existing file has to be confirmed - neither is
 * {@code JFileChooser}'s default behavior.</li>
 * </ul>
 *
 * @author Peter Dell
 */
public final class FileChoosers {

	private final MRUController mruController;
	private final Supplier<DefaultFolders> defaultFoldersSupplier;
	private final Map<FileType, File> lastFolders = new HashMap<>(); // FileType is a value set, not an enum.

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
		return showOpenDialog(parent, createFileChooser(title, fileType, null), fileType);
	}

	/**
	 * Lets the user pick an existing file of any type {@code computerSystem}
	 * can read (or a workspace), for the caller to find out which - see
	 * {@code Dis6502.openFile}'s {@link FileType#ANY_FILE} handling. The
	 * filter is the union of those types' extensions. Returns {@code null}
	 * if cancelled.
	 */
	public File chooseOpenAnyFile(Component parent, String title, ComputerSystem computerSystem) {
		JFileChooser fileChooser = createFileChooser(title, FileType.ANY_FILE, null);
		List<String> extensions = new ArrayList<>();
		for (FileType fileType : FileType.getValues()) {
			if (fileType == FileType.WORKSPACE_FILE || computerSystem.isSupportedFileType(fileType)) {
				for (String extension : fileType.getFilterExtensions()) {
					if (!extensions.contains(extension)) {
						extensions.add(extension);
					}
				}
			}
		}
		fileChooser.setFileFilter(createFilter(Texts.FileChoosers_AllSupportedFilesFilterText, extensions));
		return showOpenDialog(parent, fileChooser, FileType.ANY_FILE);
	}

	private File showOpenDialog(Component parent, JFileChooser fileChooser, FileType fileType) {
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
		File file = withDefaultExtension(fileChooser.getSelectedFile(), fileType);
		if (file.exists() && JOptionPane.showConfirmDialog(parent, TextUtility.format(Texts.FileChoosers_OverwriteMessage, file.getPath()),
				Texts.FileChoosers_OverwriteTitle, JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) {
			return null;
		}
		lastFolders.put(fileType, file.getParentFile());
		return file;
	}

	private JFileChooser createFileChooser(String title, FileType fileType, File suggestedFile) {
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setDialogTitle(title);

		if (!fileType.getFilterExtensions().isEmpty()) {
			fileChooser.setFileFilter(createFilter(fileType.getFilterText(), fileType.getFilterExtensions()));
		}

		// For "any file", the most recently used file of whatever type is the best guess.
		File mruFile = toFile(fileType == FileType.ANY_FILE ? mruController.getLastFilePath() : mruController.getLastFilePath(fileType));
		File initialFolder = getInitialFolder(suggestedFile, lastFolders.get(fileType), mruFile,
				toFile(defaultFoldersSupplier.get().getFolderPath(fileType.getFolderType())));
		if (initialFolder != null) {
			fileChooser.setCurrentDirectory(initialFolder);
		}
		if (suggestedFile != null) {
			fileChooser.setSelectedFile(suggestedFile);
		}
		return fileChooser;
	}

	/** A filter showing {@code text} and its extensions: "Executable Files (*.bin, *.com, ...)". The extensions come with their dot. */
	private static FileNameExtensionFilter createFilter(String text, List<String> extensionsWithDot) {
		String[] extensions = new String[extensionsWithDot.size()];
		StringBuilder description = new StringBuilder(text).append(" (");
		for (int i = 0; i < extensions.length; i++) {
			extensions[i] = extensionsWithDot.get(i).substring(1);
			description.append(i == 0 ? "*." : ", *.").append(extensions[i]);
		}
		return new FileNameExtensionFilter(description.append(")").toString(), extensions);
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
	static File withDefaultExtension(File file, FileType fileType) {
		if (file.getName().contains(".") || !fileType.hasDefaultExtension()) {
			return file;
		}
		return new File(file.getParentFile(), file.getName() + fileType.getDefaultExtension());
	}
}
