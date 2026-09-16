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
import com.wudsn.tools.dis6502.model.Disassembly;
import com.wudsn.tools.dis6502.model.DisassemblyProgressMonitor;
import com.wudsn.tools.dis6502.model.FileType;
import com.wudsn.tools.dis6502.model.Workspace;
import com.wudsn.tools.dis6502.model.WorkspaceLogic;
import com.wudsn.tools.dis6502.ui.MainWindow;
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
 * opening/adding an executable file, and Help &gt; About. {@link
 * #confirmClearWorkspace} mirrors {@code Main::PromptToClearWorkspace}.
 * Raw/ROM/cassette/disk-image file opening, equate editing, the memory
 * inspector, and cross-reference view are not wired up yet - see the
 * individual {@code ui} panel classes for what is and isn't ported so far.
 *
 * @author Peter Dell
 */
public final class Dis6502 {

	private static Dis6502 instance;

	private UIApplication application;
	private WorkspaceLogic workspaceLogic;
	private Workspace workspace;
	private MainWindow mainWindow;
	private File currentFile;

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
		workspace = new Workspace(computerSystemFactory);
		workspace.setComputerSystemTypeID("ATARI800");

		mainWindow = new MainWindow();
		application.setLogPanel(mainWindow.logPanel);
		mainWindow.segmentListPanel.setWorkspace(workspace);

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
		mainWindow.mainMenu.aboutMenuItem.addActionListener(e -> performAbout());

		updateTitle();
		mainWindow.setVisible(true);
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
		mainWindow.segmentListPanel.refresh();
		performDisassemble();
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

		mainWindow.segmentListPanel.refresh();
		performDisassemble();
		updateTitle();
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
			updateTitle();
		}
		return saved;
	}

	/** Ported from Disassembly's use in MainTest::ExecuteUnitTestItem - not yet triggered from a menu command like ui/MainFile.cpp's real flow. */
	private void performDisassemble() {
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

	private void performExit() {
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
