/*
 * Copyright (C) 2025 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502;

import java.awt.EventQueue;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.wudsn.tools.dis6502.model.ComputerSystemFactory;
import com.wudsn.tools.dis6502.model.ComputerSystemType;
import com.wudsn.tools.dis6502.model.DefaultFolders;
import com.wudsn.tools.dis6502.model.DefaultFoldersLogic;
import com.wudsn.tools.dis6502.model.Disassembly;
import com.wudsn.tools.dis6502.model.DisassemblyProgressMonitor;
import com.wudsn.tools.dis6502.model.EquateList;
import com.wudsn.tools.dis6502.model.EquateListLogic;
import com.wudsn.tools.dis6502.model.FileType;
import com.wudsn.tools.dis6502.model.MRUEntry;
import com.wudsn.tools.dis6502.model.Workspace;
import com.wudsn.tools.dis6502.model.WorkspaceLogic;
import com.wudsn.tools.dis6502.model.WorkspaceProperty;
import com.wudsn.tools.dis6502.ui.DefaultFoldersDialog;
import com.wudsn.tools.dis6502.ui.MainWindow;
import com.wudsn.tools.dis6502.ui.MRUController;
import com.wudsn.tools.dis6502.ui.UIApplication;

/**
 * Application entry point. Follows the same bootstrap pattern as
 * com.wudsn.tools.thecartstudio.TheCartStudio: create the wudsn-base
 * {@code Application} singleton, then build the UI on the Swing event
 * dispatch thread.
 * <p>
 * Ported from ui/Main.h / Main.cpp / ui/MainController.h / MainController.cpp
 * / ui/MainFile.cpp, reduced to a first working slice: the main window
 * shell (see {@link MainWindow}) plus workspace New/Open/Save/Save As/Exit,
 * opening/adding an executable file, loading/saving/clearing/exporting
 * equates, the View menu's No Disassembly/Double Font Height toggles and
 * Default Folders dialog (see {@link DefaultFoldersDialog}), and Help &gt;
 * About. {@link #confirmClearWorkspace} mirrors {@code
 * Main::PromptToClearWorkspace}; {@link #updateDisassembly} mirrors {@code
 * Main::UpdateDisassembly}, called explicitly after each action instead of
 * through the reactive {@code Main::HandleWorkspaceChanged} dispatcher,
 * which is not ported. Raw/ROM/cassette/disk-image file opening, editing
 * equates through a dialog, the memory inspector, and cross-reference view
 * are not wired up yet - see the individual {@code ui} panel classes for
 * what is and isn't ported so far.
 *
 * @author Peter Dell
 */
public final class Dis6502 {

	private static Dis6502 instance;

	private UIApplication application;
	private WorkspaceLogic workspaceLogic;
	private EquateListLogic equateListLogic;
	private DefaultFoldersLogic defaultFoldersLogic;
	private Workspace workspace;
	private MainWindow mainWindow;
	private MRUController mruController;
	private DefaultFolders defaultFolders;
	private File currentFile;
	private File lastEquateFile;

	public static void main(final String[] args) {

		// Use the event dispatch thread for Swing components.
		EventQueue.invokeLater(new Runnable() {

			@Override
			public void run() {

				com.wudsn.tools.base.common.Application.createInstance(
						"https://www.wudsn.com/tools/dis6502/dis6502.zip", "dis6502.jar", Dis6502.class);
				instance = new Dis6502();
				instance.run(args);
			}
		});
	}

	Dis6502() {
	}

	void run(String[] args) {
		if (args == null) {
			throw new IllegalArgumentException("Parameter 'args' must not be null.");
		}

		application = new UIApplication();
		ComputerSystemFactory computerSystemFactory = new ComputerSystemFactory();
		workspaceLogic = new WorkspaceLogic(application);
		equateListLogic = new EquateListLogic(application);
		defaultFoldersLogic = new DefaultFoldersLogic(application);
		workspace = new Workspace(computerSystemFactory);
		workspace.setComputerSystemTypeID("ATARI800");
		mruController = new MRUController(application);
		mruController.load();

		mainWindow = new MainWindow();
		application.setLogPanel(mainWindow.logPanel);
		mainWindow.segmentListPanel.setWorkspace(workspace);
		workspace.addListener((changedWorkspace, properties) -> {
			if (properties.contains(WorkspaceProperty.SYSTEM_EQUATES) || properties.contains(WorkspaceProperty.USER_EQUATES)) {
				updateEquatesMenuState();
			}
		});

		mainWindow.getFrame().addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				performExit();
			}
		});

		mainWindow.mainMenu.newWorkspaceMenuItem.addActionListener(e -> performNewWorkspace());
		mainWindow.mainMenu.openWorkspaceMenuItem.addActionListener(e -> performOpenWorkspace());
		mainWindow.mainMenu.openExecutableFileMenuItem.addActionListener(e -> performOpenFile(FileType.EXECUTABLE_FILE, false));
		mainWindow.mainMenu.addExecutableFileMenuItem.addActionListener(e -> performOpenFile(FileType.EXECUTABLE_FILE, true));
		mainWindow.mainMenu.saveWorkspaceMenuItem.addActionListener(e -> performSaveWorkspace());
		mainWindow.mainMenu.saveWorkspaceAsMenuItem.addActionListener(e -> performSaveWorkspaceAs());
		mainWindow.mainMenu.exitMenuItem.addActionListener(e -> performExit());

		mainWindow.mainMenu.clearSystemEquatesMenuItem.addActionListener(e -> performClearEquates(workspace.getSystemEquateList()));
		mainWindow.mainMenu.clearUserEquatesMenuItem.addActionListener(e -> performClearEquates(workspace.getUserEquateList()));
		mainWindow.mainMenu.openUserEquatesMenuItem.addActionListener(e -> performOpenUserEquates());
		mainWindow.mainMenu.saveUserEquatesMenuItem.addActionListener(e -> performSaveUserEquates(false));
		mainWindow.mainMenu.exportUserEquatesMenuItem.addActionListener(e -> performSaveUserEquates(true));

		mainWindow.mainMenu.noDisassemblyMenuItem.setSelected(workspace.isViewNoDisassembly());
		mainWindow.mainMenu.noDisassemblyMenuItem.addActionListener(e -> performToggleViewDisassembly());
		mainWindow.mainMenu.doubleFontHeightMenuItem.setSelected(workspace.isViewDoubleHeight());
		mainWindow.mainMenu.doubleFontHeightMenuItem.addActionListener(e -> performToggleViewDoubleFontHeight());
		mainWindow.mainMenu.defaultFoldersMenuItem.addActionListener(e -> performShowDefaultFolders());

		mainWindow.mainMenu.aboutMenuItem.addActionListener(e -> performAbout());

		refreshMRUMenus();
		updateEquatesMenuState();
		updateTitle();
		mainWindow.setVisible(true);
	}

	/** Ported from Main::UpdateMenuState's Equates-menu part (ui/Main.cpp). */
	private void updateEquatesMenuState() {
		boolean hasSystemEquates = !workspace.getSystemEquateList().isEmpty();
		boolean hasUserEquates = !workspace.getUserEquateList().isEmpty();
		mainWindow.mainMenu.clearSystemEquatesMenuItem.setEnabled(hasSystemEquates);
		mainWindow.mainMenu.clearUserEquatesMenuItem.setEnabled(hasUserEquates);
		mainWindow.mainMenu.saveUserEquatesMenuItem.setEnabled(hasUserEquates);
		mainWindow.mainMenu.exportUserEquatesMenuItem.setEnabled(hasUserEquates);
	}

	/** Repopulates the "Recent Workspaces"/"Recent Files" menus from {@link #mruController}. */
	private void refreshMRUMenus() {
		mruController.fillMenu(mainWindow.mainMenu.recentWorkspacesMenu, true, this::openRecentWorkspace);
		mruController.fillMenu(mainWindow.mainMenu.recentFilesMenu, false, this::openRecentFile);
	}

	/** Ported from MRUController's use in MainController::OnCommand for a "Recent Workspaces" selection. */
	private void openRecentWorkspace(MRUEntry entry) {
		if (!confirmClearWorkspace()) {
			return;
		}
		if (!workspaceLogic.load(workspace, entry.getFilePath())) {
			JOptionPane.showMessageDialog(mainWindow.getFrame(),
					"Could not open workspace '" + entry.getFilePath() + "'. See the log for details.",
					"Open Workspace", JOptionPane.ERROR_MESSAGE);
			return;
		}
		currentFile = new File(entry.getFilePath());
		mruController.addFile(entry.getFilePath(), FileType.WORKSPACE_FILE);
		mruController.save();
		refreshMRUMenus();
		mainWindow.segmentListPanel.refresh();
		updateDisassembly(true);
		updateTitle();
	}

	/** Ported from MRUController's use in MainController::OnCommand for a "Recent Files" selection. */
	private void openRecentFile(MRUEntry entry) {
		if (!confirmClearWorkspace()) {
			return;
		}
		workspace.init();
		workspace.setComputerSystemTypeID("ATARI800");
		currentFile = null;

		if (!workspaceLogic.addFile(workspace, entry.getFileType(), entry.getFilePath())) {
			JOptionPane.showMessageDialog(mainWindow.getFrame(),
					"Could not open file '" + entry.getFilePath() + "'. See the log for details.", "Open File",
					JOptionPane.ERROR_MESSAGE);
			return;
		}
		mruController.addFile(entry.getFilePath(), entry.getFileType());
		mruController.save();
		refreshMRUMenus();
		mainWindow.segmentListPanel.refresh();
		updateDisassembly(true);
		updateTitle();
	}

	private void performNewWorkspace() {
		if (!confirmClearWorkspace()) {
			return;
		}
		workspace.init();
		workspace.setComputerSystemTypeID("ATARI800");
		currentFile = null;
		mainWindow.segmentListPanel.refresh();
		mainWindow.disassemblyPanel.refresh(null);
		updateTitle();
	}

	private void performOpenWorkspace() {
		if (!confirmClearWorkspace()) {
			return;
		}

		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setDialogTitle("Open Workspace File");
		fileChooser.setFileFilter(new FileNameExtensionFilter("Workspace Files (*.wrk)", "wrk"));
		if (currentFile != null) {
			fileChooser.setCurrentDirectory(currentFile.getParentFile());
		}
		if (fileChooser.showOpenDialog(mainWindow.getFrame()) != JFileChooser.APPROVE_OPTION) {
			return;
		}
		File file = fileChooser.getSelectedFile();
		if (!workspaceLogic.load(workspace, file.getPath())) {
			JOptionPane.showMessageDialog(mainWindow.getFrame(),
					"Could not open workspace '" + file.getPath() + "'. See the log for details.", "Open Workspace",
					JOptionPane.ERROR_MESSAGE);
			return;
		}
		currentFile = file;
		mruController.addFile(file.getPath(), FileType.WORKSPACE_FILE);
		mruController.save();
		refreshMRUMenus();
		mainWindow.segmentListPanel.refresh();
		updateDisassembly(true);
		updateTitle();
	}

	/**
	 * Opens or adds a file of the given type. Ported from MainFile::OpenFile
	 * (and the individual OpenXxxFile methods it dispatches to), scoped to
	 * {@link FileType#EXECUTABLE_FILE} only for this first pass - see the
	 * class javadoc.
	 */
	private void performOpenFile(FileType fileType, boolean add) {
		if (!add && !confirmClearWorkspace()) {
			return;
		}

		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setDialogTitle(add ? "Add Executable File" : "Open Executable File");
		if (currentFile != null) {
			fileChooser.setCurrentDirectory(currentFile.getParentFile());
		}
		if (fileChooser.showOpenDialog(mainWindow.getFrame()) != JFileChooser.APPROVE_OPTION) {
			return;
		}
		File file = fileChooser.getSelectedFile();

		if (!add) {
			workspace.init();
			workspace.setComputerSystemTypeID("ATARI800");
			currentFile = null;
		}

		if (!workspaceLogic.addFile(workspace, fileType, file.getPath())) {
			JOptionPane.showMessageDialog(mainWindow.getFrame(),
					"Could not " + (add ? "add" : "open") + " file '" + file.getPath() + "'. See the log for details.",
					add ? "Add File" : "Open File", JOptionPane.ERROR_MESSAGE);
			return;
		}

		mruController.addFile(file.getPath(), fileType);
		mruController.save();
		refreshMRUMenus();
		mainWindow.segmentListPanel.refresh();
		updateDisassembly(true);
		updateTitle();
	}

	/** Ported from EquateListController::Clear, without the not-yet-ported "Display System Equates" callers care about. */
	private void performClearEquates(EquateList equateList) {
		if (equateList.isEmpty()) {
			return;
		}
		String message = equateList.getProperty() == WorkspaceProperty.SYSTEM_EQUATES
				? Text.IDS_EQUATES_CONFIRM_CLEAR_SYSTEM_EQUATES
				: Text.IDS_EQUATES_CONFIRM_CLEAR_USER_EQUATES;
		if (JOptionPane.showConfirmDialog(mainWindow.getFrame(), message, "Clear Equates",
				JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
			equateList.clear();
		}
	}

	/** Ported from EquateListController::LoadUserEquates. */
	private void performOpenUserEquates() {
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setDialogTitle("Open User Equates File");
		fileChooser.setFileFilter(new FileNameExtensionFilter("Equate Files (*.equ)", "equ"));
		if (lastEquateFile != null) {
			fileChooser.setCurrentDirectory(lastEquateFile.getParentFile());
		}
		if (fileChooser.showOpenDialog(mainWindow.getFrame()) != JFileChooser.APPROVE_OPTION) {
			return;
		}
		lastEquateFile = fileChooser.getSelectedFile();
		equateListLogic.load(workspace.getUserEquateList(), lastEquateFile.getPath());
	}

	/** Ported from EquateListController::Save, invoked for both "Save User Equates" ({@code xasm=false}) and "Export User Equates" ({@code xasm=true}). */
	private void performSaveUserEquates(boolean xasm) {
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setDialogTitle(xasm ? "Export User Equates File" : "Save User Equates File");
		fileChooser.setFileFilter(new FileNameExtensionFilter("Equate Files (*.equ)", "equ"));
		if (lastEquateFile != null) {
			fileChooser.setCurrentDirectory(lastEquateFile.getParentFile());
		}
		if (fileChooser.showSaveDialog(mainWindow.getFrame()) != JFileChooser.APPROVE_OPTION) {
			return;
		}
		lastEquateFile = fileChooser.getSelectedFile();
		equateListLogic.save(workspace.getUserEquateList(), lastEquateFile.getPath(), xasm);
	}

	/** Ported from Main::ToggleViewDisassembly. */
	private void performToggleViewDisassembly() {
		workspace.setViewNoDisassembly(mainWindow.mainMenu.noDisassemblyMenuItem.isSelected());
		updateDisassembly(false); // Will do nothing if it is now true, matching the C++ comment at the same spot.
	}

	/**
	 * Ported from Main::ToggleViewDoubleFontHeight. The font-resizing this
	 * notification is meant to trigger ({@code Main::SetLayoutFont}/{@code
	 * layout->Compute}) is not ported - {@link
	 * com.wudsn.tools.dis6502.ui.DisassemblyPanel} does not yet respond to a
	 * {@link WorkspaceProperty#FONT} change - so toggling this has no
	 * visible effect yet; it still updates real, already-ported
	 * {@link Workspace} state and fires the same notification C++ does.
	 */
	private void performToggleViewDoubleFontHeight() {
		workspace.setViewDoubleHeight(mainWindow.mainMenu.doubleFontHeightMenuItem.isSelected());
		workspace.notifyFontChanged();
	}

	/**
	 * Ported from Main::ShowDefaultFoldersDialog. {@code
	 * FileDialogs::SetDefaultFolders} is not ported - there is no Java
	 * equivalent of the C++ {@code FileDialogs} abstraction yet, so editing
	 * the default folders here does not yet influence the directory the
	 * various "Open"/"Save" {@link JFileChooser}s start in.
	 */
	private void performShowDefaultFolders() {
		if (defaultFolders == null) {
			defaultFolders = defaultFoldersLogic.createDefaultFolders(workspace.getComputerSystem().getTypeInfo());
			defaultFoldersLogic.load(defaultFolders);
		}
		new DefaultFoldersDialog(mainWindow.getFrame()).show(defaultFolders);
	}

	/**
	 * Ported from Main::PromptToClearWorkspace, simplified: always passes
	 * {@code loadSystemEquates=false} (system equate loading - {@code
	 * WorkspaceLogic.loadSystemEquates} - is not ported yet, see its
	 * javadoc). Returns {@code false} if the caller should abort (the user
	 * cancelled, or a requested save failed).
	 */
	private boolean confirmClearWorkspace() {
		if (workspace.getSegmentList().isEmpty()) {
			return true;
		}

		int result = JOptionPane.showConfirmDialog(mainWindow.getFrame(), Text.IDS_MAIN_FILE_NEW_WORKSPACE_MESSAGE,
				Text.IDS_MAIN_FILE_NEW_WORKSPACE_TITLE, JOptionPane.YES_NO_CANCEL_OPTION);
		if (result == JOptionPane.CANCEL_OPTION || result == JOptionPane.CLOSED_OPTION) {
			return false;
		}
		if (result == JOptionPane.YES_OPTION && !performSaveWorkspace()) {
			return false; // The save (or its "Save As" dialog) was cancelled/failed.
		}
		return true;
	}

	private boolean performSaveWorkspace() {
		if (currentFile == null) {
			return performSaveWorkspaceAs();
		}
		return workspaceLogic.save(workspace, currentFile.getPath());
	}

	private boolean performSaveWorkspaceAs() {
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setDialogTitle("Save Workspace File As");
		fileChooser.setFileFilter(new FileNameExtensionFilter("Workspace Files (*.wrk)", "wrk"));
		if (currentFile != null) {
			fileChooser.setSelectedFile(currentFile);
		}
		if (fileChooser.showSaveDialog(mainWindow.getFrame()) != JFileChooser.APPROVE_OPTION) {
			return false;
		}
		File file = fileChooser.getSelectedFile();
		if (!file.getName().contains(".")) {
			file = new File(file.getPath() + ".wrk");
		}
		boolean saved = workspaceLogic.save(workspace, file.getPath());
		if (saved) {
			currentFile = file;
			mruController.addFile(file.getPath(), FileType.WORKSPACE_FILE);
			mruController.save();
			refreshMRUMenus();
			updateTitle();
		}
		return saved;
	}

	/**
	 * Ported from Main::UpdateDisassembly, using the real {@link Disassembly}
	 * pipeline the way {@code MainTest::ExecuteUnitTestItem} does. Unlike the
	 * C++ version, whose callers compute {@code bForce} from which {@link
	 * WorkspaceProperty} changed (see {@code Main::HandleWorkspaceChanged} -
	 * that whole reactive dispatcher is not ported, {@code Dis6502} instead
	 * calls this explicitly after each action), every call site here passes
	 * {@code force=true} except the "No Disassembly" toggle - matching the
	 * C++ behavior for the actions currently wired: opening/adding a file or
	 * a workspace always corresponds to a {@code SEGMENTS} change, which C++
	 * always forces.
	 */
	private void updateDisassembly(boolean force) {
		if (workspace.getComputerSystem().getType() == ComputerSystemType.UNKNOWN) {
			return;
		}
		if (!force && workspace.isViewNoDisassembly()) {
			return;
		}
		if (workspace.getSegmentList().isEmpty()) {
			return;
		}
		Disassembly disassembly = new Disassembly();
		DisassemblyProgressMonitor progressMonitor = new DisassemblyProgressMonitor();
		disassembly.setWorkspace(workspace);
		disassembly.setProgressMonitor(progressMonitor);
		try {
			progressMonitor.startDisassembly(disassembly);
		} catch (RuntimeException ex) {
			application.sendErrorMessage(ex);
		}
		mainWindow.disassemblyPanel.refresh(workspace.getDisassemblyResult());
	}

	/** Ported from the exit path in Main::Execute, which saves the MRU lists (already saved incrementally here, see {@link #mruController}) and any loaded {@link DefaultFolders}. */
	private void performExit() {
		if (defaultFolders != null) {
			defaultFoldersLogic.save(defaultFolders);
		}
		System.exit(0);
	}

	private void performAbout() {
		String message = "6502 Disassembler\n\n" + "The purpose of this software is to disassemble a 6502\n"
				+ "binary file and generate a listing ready to assemble.\n\n"
				+ "(c) 1997-2024 Eric Bacher, atari@ebacher.info\n"
				+ "Win32 Port - 2005 by James Wilkinson, james@slor.net\n"
				+ "Win32 Fixes - 2015-2024 by Peter Dell, jac@wudsn.com\n\n"
				+ "Feel free to send any comments, new ideas, or\n" + "bug reports on SourceForge.\n"
				+ "http://sourceforge.net/projects/dis6502";
		JOptionPane.showMessageDialog(mainWindow.getFrame(), message, "About DIS6502",
				JOptionPane.INFORMATION_MESSAGE);
	}

	private void updateTitle() {
		String title = "dis6502";
		if (currentFile != null) {
			title += " - " + currentFile.getName();
		}
		mainWindow.getFrame().setTitle(title);
	}
}
