/*
 * Copyright (C) 2025 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502;

import java.awt.EventQueue;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.wudsn.tools.dis6502.model.AtariDisk;
import com.wudsn.tools.dis6502.model.AtariError;
import com.wudsn.tools.dis6502.model.AtariFile;
import com.wudsn.tools.dis6502.model.ComputerSystem;
import com.wudsn.tools.dis6502.model.ComputerSystemFactory;
import com.wudsn.tools.dis6502.model.ComputerSystemType;
import com.wudsn.tools.dis6502.model.DefaultFolders;
import com.wudsn.tools.dis6502.model.DefaultFoldersLogic;
import com.wudsn.tools.dis6502.model.Disassembly;
import com.wudsn.tools.dis6502.model.DisassemblyLine;
import com.wudsn.tools.dis6502.model.DisassemblyProgressMonitor;
import com.wudsn.tools.dis6502.model.DisassemblyResult;
import com.wudsn.tools.dis6502.model.DisassemblyResultFile;
import com.wudsn.tools.dis6502.model.DiskImage;
import com.wudsn.tools.dis6502.model.Equate;
import com.wudsn.tools.dis6502.model.EquateList;
import com.wudsn.tools.dis6502.model.EquateListLogic;
import com.wudsn.tools.dis6502.model.FileHeader;
import com.wudsn.tools.dis6502.model.FileType;
import com.wudsn.tools.dis6502.model.ImgInfo;
import com.wudsn.tools.dis6502.model.ImgRWPacket;
import com.wudsn.tools.dis6502.model.InstructionSet;
import com.wudsn.tools.dis6502.model.MRUEntry;
import com.wudsn.tools.dis6502.model.MemoryInspectorSelection;
import com.wudsn.tools.dis6502.model.MemoryType;
import com.wudsn.tools.dis6502.model.OperandMode;
import com.wudsn.tools.dis6502.model.ProfileLogic;
import com.wudsn.tools.dis6502.model.Segment;
import com.wudsn.tools.dis6502.model.SegmentList;
import com.wudsn.tools.dis6502.model.SegmentListInserter;
import com.wudsn.tools.dis6502.model.Workspace;
import com.wudsn.tools.dis6502.model.WorkspaceLogic;
import com.wudsn.tools.dis6502.model.WorkspaceProperty;
import com.wudsn.tools.dis6502.ui.AssembleDialog;
import com.wudsn.tools.dis6502.ui.CommentDialog;
import com.wudsn.tools.dis6502.ui.ComputerFont;
import com.wudsn.tools.dis6502.ui.DefaultFoldersDialog;
import com.wudsn.tools.dis6502.ui.DisassemblyProgressDialog;
import com.wudsn.tools.dis6502.ui.DiskImageExecutableFileDialog;
import com.wudsn.tools.dis6502.ui.DiskImageSectorsDialog;
import com.wudsn.tools.dis6502.ui.EquateDialog;
import com.wudsn.tools.dis6502.ui.EquateRangeDialog;
import com.wudsn.tools.dis6502.ui.LowHighByteDialog;
import com.wudsn.tools.dis6502.ui.MainWindow;
import com.wudsn.tools.dis6502.ui.MemoryInspectorFindStringDialog;
import com.wudsn.tools.dis6502.ui.MRUController;
import com.wudsn.tools.dis6502.ui.ProfileDialog;
import com.wudsn.tools.dis6502.ui.RawFileDialog;
import com.wudsn.tools.dis6502.ui.SegmentPropertiesDialog;
import com.wudsn.tools.dis6502.ui.SegmentWriteBootDiskDialog;
import com.wudsn.tools.dis6502.ui.SelectSpritesDialog;
import com.wudsn.tools.dis6502.ui.UIApplication;
import com.wudsn.tools.dis6502.ui.WorkspaceDialog;
import com.wudsn.tools.dis6502.ui.XRefPanel;

/**
 * Application entry point. Follows the same bootstrap pattern as
 * com.wudsn.tools.thecartstudio.TheCartStudio: create the wudsn-base
 * {@code Application} singleton, then build the UI on the Swing event
 * dispatch thread.
 * <p>
 * Ported from ui/Main.h / Main.cpp / ui/MainController.h / MainController.cpp
 * / ui/MainFile.cpp / ui/MainMenu.cpp, reduced to a first working slice: the
 * main window shell (see {@link MainWindow}) plus workspace New (see {@link
 * #performNewWorkspace}/{@link WorkspaceDialog})/Open/Save/Save As/Save
 * Disassembly Files (see {@link #performSaveDisassemblyFiles})/Write Boot
 * Disk (see {@link #performWriteBootDisk}/{@link SegmentWriteBootDiskDialog})/
 * Exit, opening/adding an executable, ROM image, cassette image, raw, disk image
 * executable, disk image boot sectors, or disk image sectors file (see
 * {@link RawFileDialog}/{@link DiskImageExecutableFileDialog}/{@link
 * #performOpenDiskImageBootSectors}/{@link DiskImageSectorsDialog}),
 * loading/saving/clearing/exporting/editing equates and defining a user
 * equate address range (see {@link EquateDialog}/{@link
 * EquateRangeDialog}), the View menu's Display as Screen Code/No
 * Disassembly/Double Font Height toggles and Default Folders/Profile
 * dialogs (see {@link DefaultFoldersDialog}/{@link ProfileDialog}), and
 * Help &gt; About.
 * {@link #confirmClearWorkspace} mirrors {@code Main::PromptToClearWorkspace};
 * {@link #updateDisassembly} mirrors {@code Main::UpdateDisassembly},
 * called explicitly after each action instead of through the reactive
 * {@code Main::HandleWorkspaceChanged} dispatcher, which is not ported.
 * {@link #performFindInDisassembly}/{@link #performFindNextInDisassembly}/
 * {@link #performXRefSelected} wire the disassembly search field/buttons
 * to {@link XRefPanel}, ported from MainDisassembly::Find/FindNextString/
 * RefreshXRef/XRefSelected, and (via {@link
 * #updateMemoryInspectorSegment}) keep {@link
 * com.wudsn.tools.dis6502.ui.MemoryInspectorPanel}'s hex dump in sync with
 * the selected segment - see that class's javadoc for what its
 * drastically-simplified first pass does and doesn't cover. The segment
 * list's popup menu commands (Move Up/Down, Merge, Delete, Save Segment/
 * Save All Segments, Properties...) are wired here too, from {@code
 * com.wudsn.tools.dis6502.ui.SegmentListPanel}'s exposed menu items -
 * ported from ui/MainSegment.cpp - and {@link
 * #performShowSegmentProperties} uses {@link SegmentPropertiesDialog}.
 * {@link #performShowMemoryInspectorFindDialog}/{@link
 * #performMemoryInspectorFindNext} wire {@code
 * com.wudsn.tools.dis6502.ui.MemoryInspectorPanel}'s find buttons to
 * {@link MemoryInspectorFindStringDialog}, ported from the popup menu's
 * IDM_DUMP_FIND/IDM_DUMP_FIND_NEXT commands, and {@link
 * #performSplitAtSelection} wires its Split at Selection button, ported
 * from IDM_DUMP_SPLIT_AT_SELECTION/{@code MemoryInspector::SplitAtSelection}.
 * {@link #performSaveMemoryInspectorSelection} wires the Save Selection
 * buttons, ported from IDM_DUMP_SAVE_NO_HEADER/IDM_DUMP_SAVE_HEADER/{@code
 * MainMemoryInspector::SaveWithoutHeader}/{@code SaveWithHeader}; the
 * Select All/Select Next Unknown Block buttons just call {@code
 * com.wudsn.tools.dis6502.ui.MemoryInspectorPanel#selectAll}/{@code
 * #selectNextUnknownBlock} directly. {@link #performSetMemoryInspectorType}/
 * {@link #performSetMemoryInspectorLoHiType}/{@link
 * #performSetUnknownBlockToByte} wire the Set Type combo/button and Set
 * Unknown Block to Byte button, ported from the type submenu's
 * IDM_DUMP_SET_TYPE_&#42;/{@code MemoryInspector::SetType} and
 * IDM_DUMP_SET_UNKNOWN_BLOCK_TO_BYTE/{@code SetUnknownBlockToByte} - the
 * first commands ported here that mutate a segment's byte types, calling
 * {@link #updateDisassembly} afterward to reflect the change, the same
 * way editing equates already does. {@link #performCopyMemoryInspectorSelection}
 * wires the Copy Selection button, ported from
 * IDM_DUMP_COPY_SELECTION/{@code MemoryInspector::CopySelection} - see
 * that method's own javadoc for why Delete/Cut/Paste Selection are not
 * ported. {@link #performEditMemoryInspectorComment} wires the
 * Comment... button, ported from
 * IDM_DUMP_EDIT_COMMENT/{@code MainDisassembly::AddComment} via the new
 * {@link CommentDialog}. {@link #performShowAssembleDialog} wires the
 * Assemble... button, ported from
 * IDM_DUMP_ASSEMBLE/{@code MainMemoryInspector::PerformCommands} via the
 * new {@link AssembleDialog}. {@link #performGuessCode} wires the Guess
 * Code button, ported from
 * IDM_DUMP_START_CODE_TRACE/{@code MemoryInspector::Guess} via the new
 * {@code com.wudsn.tools.dis6502.model.GuessCodeLogic}.
 * {@link #performShowSelectSpritesDialog} wires the Select Sprites...
 * button, ported from
 * IDM_DUMP_SELECT_SPRITES/{@code MemoryInspector::ShowSelectSpritesDialog}
 * via the new {@link SelectSpritesDialog}.
 *
 * @author Peter Dell
 */
public final class Dis6502 {

	private static Dis6502 instance;

	private UIApplication application;
	private WorkspaceLogic workspaceLogic;
	private EquateListLogic equateListLogic;
	private DefaultFoldersLogic defaultFoldersLogic;
	private ProfileLogic profileLogic;
	private Workspace workspace;
	private MainWindow mainWindow;
	private MRUController mruController;
	private DefaultFolders defaultFolders;
	private MemoryInspectorSelection memoryInspectorSelection;
	private File currentFile;
	private File lastEquateFile;
	private final int[] findFirstLineNumber = { 0 };

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
		profileLogic = new ProfileLogic(application);
		workspace = new Workspace(computerSystemFactory);
		workspace.setComputerSystemTypeID("ATARI800");
		mruController = new MRUController(application);
		mruController.load();
		memoryInspectorSelection = new MemoryInspectorSelection(workspace);

		mainWindow = new MainWindow();
		application.setLogPanel(mainWindow.logPanel);
		mainWindow.segmentListPanel.setWorkspace(workspace);
		mainWindow.segmentListPanel.moveUpMenuItem.addActionListener(e -> workspace.getSegmentList().moveSelectedSegmentUp());
		mainWindow.segmentListPanel.moveDownMenuItem.addActionListener(e -> workspace.getSegmentList().moveSelectedSegmentDown());
		mainWindow.segmentListPanel.mergeMenuItem.addActionListener(e -> performMergeSegments());
		mainWindow.segmentListPanel.deleteMenuItem.addActionListener(e -> workspace.getSegmentList().deleteSelectedSegment());
		mainWindow.segmentListPanel.saveNoHeaderMenuItem.addActionListener(e -> performSaveSegment(false));
		mainWindow.segmentListPanel.saveHeaderMenuItem.addActionListener(e -> performSaveSegment(true));
		mainWindow.segmentListPanel.saveAllMenuItem.addActionListener(e -> performSaveAllSegments());
		mainWindow.segmentListPanel.propertiesMenuItem.addActionListener(e -> performShowSegmentProperties());
		workspace.addListener((changedWorkspace, properties) -> {
			if (properties.contains(WorkspaceProperty.SYSTEM_EQUATES) || properties.contains(WorkspaceProperty.USER_EQUATES)) {
				updateEquatesMenuState();
			}
			if (properties.contains(WorkspaceProperty.SELECTED_SEGMENT)) {
				updateMemoryInspectorSegment();
			}
			if (properties.contains(WorkspaceProperty.FONT) || properties.contains(WorkspaceProperty.COMPUTER_SYSTEM_TYPE)) {
				updateFonts();
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
		mainWindow.mainMenu.openCassetteImageFileMenuItem.addActionListener(e -> performOpenFile(FileType.CASSETTE_IMAGE_FILE, false));
		mainWindow.mainMenu.addCassetteImageFileMenuItem.addActionListener(e -> performOpenFile(FileType.CASSETTE_IMAGE_FILE, true));
		mainWindow.mainMenu.openExecutableFileMenuItem.addActionListener(e -> performOpenFile(FileType.EXECUTABLE_FILE, false));
		mainWindow.mainMenu.addExecutableFileMenuItem.addActionListener(e -> performOpenFile(FileType.EXECUTABLE_FILE, true));
		mainWindow.mainMenu.openROMImageFileMenuItem.addActionListener(e -> performOpenFile(FileType.ROM_IMAGE_FILE, false));
		mainWindow.mainMenu.addROMImageFileMenuItem.addActionListener(e -> performOpenFile(FileType.ROM_IMAGE_FILE, true));
		mainWindow.mainMenu.openRawFileMenuItem.addActionListener(e -> performOpenRawFile(false));
		mainWindow.mainMenu.addRawFileMenuItem.addActionListener(e -> performOpenRawFile(true));
		mainWindow.mainMenu.openDiskImageExecutableFileMenuItem.addActionListener(e -> performOpenDiskImageExecutableFile(false));
		mainWindow.mainMenu.addDiskImageExecutableFileMenuItem.addActionListener(e -> performOpenDiskImageExecutableFile(true));
		mainWindow.mainMenu.openDiskImageBootSectorsMenuItem.addActionListener(e -> performOpenDiskImageBootSectors(false));
		mainWindow.mainMenu.addDiskImageBootSectorsMenuItem.addActionListener(e -> performOpenDiskImageBootSectors(true));
		mainWindow.mainMenu.openDiskImageSectorsMenuItem.addActionListener(e -> performOpenDiskImageSectors(false));
		mainWindow.mainMenu.addDiskImageSectorsMenuItem.addActionListener(e -> performOpenDiskImageSectors(true));
		mainWindow.mainMenu.saveWorkspaceMenuItem.addActionListener(e -> performSaveWorkspace());
		mainWindow.mainMenu.saveWorkspaceAsMenuItem.addActionListener(e -> performSaveWorkspaceAs());
		mainWindow.mainMenu.saveDisassemblyFilesMenuItem.addActionListener(e -> performSaveDisassemblyFiles());
		mainWindow.mainMenu.writeBootDiskMenuItem.addActionListener(e -> performWriteBootDisk());
		mainWindow.mainMenu.exitMenuItem.addActionListener(e -> performExit());

		mainWindow.mainMenu.clearSystemEquatesMenuItem.addActionListener(e -> performClearEquates(workspace.getSystemEquateList()));
		mainWindow.mainMenu.displaySystemEquatesMenuItem.addActionListener(e -> performEditEquates(workspace.getSystemEquateList(), false));
		mainWindow.mainMenu.clearUserEquatesMenuItem.addActionListener(e -> performClearEquates(workspace.getUserEquateList()));
		mainWindow.mainMenu.editUserEquatesMenuItem.addActionListener(e -> performEditEquates(workspace.getUserEquateList(), true));
		mainWindow.mainMenu.defineUserAddressRangeMenuItem.addActionListener(e -> performDefineUserAddressRange());
		mainWindow.mainMenu.openUserEquatesMenuItem.addActionListener(e -> performOpenUserEquates());
		mainWindow.mainMenu.saveUserEquatesMenuItem.addActionListener(e -> performSaveUserEquates(false));
		mainWindow.mainMenu.exportUserEquatesMenuItem.addActionListener(e -> performSaveUserEquates(true));

		mainWindow.mainMenu.displayAsScreenCodeMenuItem.setSelected(workspace.isViewDisplayAsScreenCode());
		mainWindow.mainMenu.displayAsScreenCodeMenuItem.addActionListener(e -> performToggleDisplayAsScreenCode());
		mainWindow.mainMenu.noDisassemblyMenuItem.setSelected(workspace.isViewNoDisassembly());
		mainWindow.mainMenu.noDisassemblyMenuItem.addActionListener(e -> performToggleViewDisassembly());
		mainWindow.mainMenu.doubleFontHeightMenuItem.setSelected(workspace.isViewDoubleHeight());
		mainWindow.mainMenu.doubleFontHeightMenuItem.addActionListener(e -> performToggleViewDoubleFontHeight());
		mainWindow.mainMenu.defaultFoldersMenuItem.addActionListener(e -> performShowDefaultFolders());
		mainWindow.mainMenu.profileMenuItem.addActionListener(e -> performShowProfile());

		mainWindow.mainMenu.aboutMenuItem.addActionListener(e -> performAbout());

		mainWindow.disassemblyPanel.findButton.addActionListener(e -> performFindInDisassembly());
		mainWindow.disassemblyPanel.findField.addActionListener(e -> performFindInDisassembly());
		mainWindow.disassemblyPanel.findNextButton.addActionListener(e -> performFindNextInDisassembly());
		mainWindow.disassemblyPanel.editCommentMenuItem.addActionListener(e -> performEditDisassemblyComment());
		mainWindow.disassemblyPanel.findDefMenuItem
				.addActionListener(e -> performFindDisassemblyLabelDefinition(mainWindow.disassemblyPanel.getRightClickedLabelReference()));
		mainWindow.disassemblyPanel.findRef1MenuItem
				.addActionListener(e -> performFindDisassemblyReferences(mainWindow.disassemblyPanel.getRightClickedLabelReference()));
		mainWindow.disassemblyPanel.findRef2MenuItem
				.addActionListener(e -> performFindDisassemblyReferences(mainWindow.disassemblyPanel.getRightClickedLabelDefinition()));
		mainWindow.disassemblyPanel.renameDefMenuItem
				.addActionListener(e -> performRenameDisassemblyLabel(mainWindow.disassemblyPanel.getRightClickedLabelDefinition()));
		mainWindow.disassemblyPanel.renameRefMenuItem
				.addActionListener(e -> performRenameDisassemblyLabel(mainWindow.disassemblyPanel.getRightClickedLabelReference()));
		mainWindow.disassemblyPanel.addrRangeDefMenuItem
				.addActionListener(e -> performDefineAddressRangeForLabel(mainWindow.disassemblyPanel.getRightClickedLabelDefinition()));
		mainWindow.disassemblyPanel.addrRangeRefMenuItem
				.addActionListener(e -> performDefineAddressRangeForLabel(mainWindow.disassemblyPanel.getRightClickedLabelReference()));
		mainWindow.xrefPanel.setSelectionListener(this::performXRefSelected);

		mainWindow.memoryInspectorPanel.findButton.addActionListener(e -> performShowMemoryInspectorFindDialog());
		mainWindow.memoryInspectorPanel.findNextButton.addActionListener(e -> performMemoryInspectorFindNext());
		mainWindow.memoryInspectorPanel.splitAtSelectionButton.addActionListener(e -> performSplitAtSelection());
		mainWindow.memoryInspectorPanel.selectAllButton.addActionListener(e -> mainWindow.memoryInspectorPanel.selectAll());
		mainWindow.memoryInspectorPanel.selectNextUnknownBlockButton
				.addActionListener(e -> mainWindow.memoryInspectorPanel.selectNextUnknownBlock());
		mainWindow.memoryInspectorPanel.selectSpritesButton.addActionListener(e -> performShowSelectSpritesDialog());
		mainWindow.memoryInspectorPanel.saveSelectionNoHeaderButton.addActionListener(e -> performSaveMemoryInspectorSelection(false));
		mainWindow.memoryInspectorPanel.saveSelectionHeaderButton.addActionListener(e -> performSaveMemoryInspectorSelection(true));
		mainWindow.memoryInspectorPanel.setTypeButton.addActionListener(e -> performSetMemoryInspectorType());
		mainWindow.memoryInspectorPanel.setUnknownBlockToByteButton.addActionListener(e -> performSetUnknownBlockToByte());
		mainWindow.memoryInspectorPanel.copySelectionButton.addActionListener(e -> performCopyMemoryInspectorSelection());
		mainWindow.memoryInspectorPanel.editCommentButton.addActionListener(e -> performEditMemoryInspectorComment());
		mainWindow.memoryInspectorPanel.assembleButton.addActionListener(e -> performShowAssembleDialog());
		mainWindow.memoryInspectorPanel.guessButton.addActionListener(e -> performGuessCode());

		refreshMRUMenus();
		updateEquatesMenuState();
		updateFonts();
		updateMemoryInspectorSegment();
		updateTitle();
		mainWindow.setVisible(true);
	}

	/** Ported from MemoryInspector::SegmentChanged's trigger (Main::HandleWorkspaceChanged's SEGMENTS/SELECTED_SEGMENT handling). */
	private void updateMemoryInspectorSegment() {
		memoryInspectorSelection.setSegmentIndex(workspace.getSegmentList().getSelectedIndex());
		mainWindow.memoryInspectorPanel.segmentChanged(memoryInspectorSelection);
	}

	/**
	 * Ported from {@code PartWindow::ApplyLayout}'s reaction to a {@link
	 * WorkspaceProperty#COMPUTER_SYSTEM_TYPE}/{@link WorkspaceProperty#FONT}
	 * change (via {@code Main::SetLayoutFont}/{@code
	 * WorkspaceFont::GetResizedFont}) - every part window shares one font set
	 * on a common {@code Layout}, matching {@link ComputerFont}'s javadoc);
	 * {@link com.wudsn.tools.dis6502.ui.XRefPanel}/{@link
	 * com.wudsn.tools.dis6502.ui.LogPanel} are two more C++ part windows that
	 * get this same font but are not wired here yet.
	 */
	private void updateFonts() {
		ComputerFont computerFont = ComputerFont.get(workspace.getComputerSystem().getType(), workspace.isViewDoubleHeight());
		mainWindow.memoryInspectorPanel.setComputerFont(computerFont);
		mainWindow.disassemblyPanel.setComputerFont(computerFont);
		mainWindow.segmentListPanel.setComputerFont(computerFont);
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

	/**
	 * Ported from MainMenu::PerformFileMenuCommands's ID_FILE_NEW_WORKSPACE/
	 * ID_FILE_NEW case: {@link #confirmClearWorkspace} plus {@code
	 * workspace.init()} together mirror {@code Main::PromptToClearWorkspace}
	 * (which does the actual clearing itself in the C++ version, unlike
	 * this port's split - see that method's javadoc), then
	 * {@link WorkspaceDialog} lets the user pick the new workspace's
	 * computer system instead of always defaulting to Atari 800.
	 */
	private void performNewWorkspace() {
		if (!confirmClearWorkspace()) {
			return;
		}
		workspace.init();
		currentFile = null;

		if (new WorkspaceDialog(mainWindow.getFrame()).show(workspace)) {
			application.sendInfoMessage(Text.IDS_MAIN_FILE_LOG_NEW_WORKSPACE_PREPARED,
					workspace.getComputerSystem().getTypeInfo().text);
		}

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
	 * the file types {@link ComputerSystem#readFile} handles without a
	 * dedicated selection dialog of its own - {@link
	 * FileType#EXECUTABLE_FILE}, {@link FileType#ROM_IMAGE_FILE}, {@link
	 * FileType#CASSETTE_IMAGE_FILE}. {@link FileType#RAW_FILE} is handled by
	 * {@link #performOpenRawFile} instead (it needs {@link RawFileDialog});
	 * the three disk image file types still need their own not-yet-ported
	 * dialog (a file-within-the-image picker) - see the class javadoc.
	 */
	private void performOpenFile(FileType fileType, boolean add) {
		if (!add && !confirmClearWorkspace()) {
			return;
		}
		String fileTypeDisplayName = getFileTypeDisplayName(fileType);

		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setDialogTitle((add ? "Add " : "Open ") + fileTypeDisplayName);
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
					(add ? "Add " : "Open ") + fileTypeDisplayName, JOptionPane.ERROR_MESSAGE);
			return;
		}

		mruController.addFile(file.getPath(), fileType);
		mruController.save();
		refreshMRUMenus();
		mainWindow.segmentListPanel.refresh();
		updateDisassembly(true);
		updateTitle();
	}

	/**
	 * Opens or adds a raw (headerless) file, via {@link RawFileDialog} for
	 * picking the byte range and load address. Ported from
	 * MainFile::OpenRawFile: unlike {@link #performOpenFile}, which routes
	 * through {@code WorkspaceLogic.addFile}/{@code ComputerSystem.readFile},
	 * this calls {@code WorkspaceLogic.addRawSegment} directly, since a raw
	 * file has no format for a {@link ComputerSystem} to parse.
	 */
	private void performOpenRawFile(boolean add) {
		if (!add && !confirmClearWorkspace()) {
			return;
		}

		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setDialogTitle((add ? "Add " : "Open ") + "Raw File");
		if (currentFile != null) {
			fileChooser.setCurrentDirectory(currentFile.getParentFile());
		}
		if (fileChooser.showOpenDialog(mainWindow.getFrame()) != JFileChooser.APPROVE_OPTION) {
			return;
		}
		File file = fileChooser.getSelectedFile();

		RawFileDialog dialog = new RawFileDialog(mainWindow.getFrame());
		boolean confirmed;
		try {
			confirmed = dialog.show(file);
		} catch (IOException ex) {
			application.sendErrorMessage(ex);
			return;
		}
		if (!confirmed) {
			return;
		}

		if (!add) {
			workspace.init();
			workspace.setComputerSystemTypeID("ATARI800");
			currentFile = null;
		}

		workspaceLogic.addRawSegment(workspace, dialog.getFileBuffer(), dialog.getBegin(), dialog.getResultSize(), dialog.getAddress());

		mruController.addFile(file.getPath(), FileType.RAW_FILE);
		mruController.save();
		refreshMRUMenus();
		mainWindow.segmentListPanel.refresh();
		updateDisassembly(true);
		updateTitle();
	}

	/**
	 * Opens or adds an executable file picked from within an Atari DOS 2.x
	 * disk image, via {@link DiskImageExecutableFileDialog}. Ported from
	 * MainFile::OpenDiskImageExecutableFile: the picked file's bytes are
	 * read directly through the already-ported {@link
	 * AtariDisk#readFile(String)} and fed to {@code WorkspaceLogic.addFile}
	 * the same way {@link #performOpenFile} feeds it a real file's {@link
	 * InputStream} - unlike the C++ version, which needs its own {@code
	 * DiskImageFileInputStream} wrapper to stream a disk image file's
	 * sectors on demand, {@link AtariDisk#readFile(String)} already returns
	 * the whole file as a {@code byte[]}, so a plain {@link
	 * ByteArrayInputStream} is enough.
	 */
	private void performOpenDiskImageExecutableFile(boolean add) {
		if (!add && !confirmClearWorkspace()) {
			return;
		}

		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setDialogTitle((add ? "Add " : "Open ") + "Disk Image Executable File");
		if (currentFile != null) {
			fileChooser.setCurrentDirectory(currentFile.getParentFile());
		}
		if (fileChooser.showOpenDialog(mainWindow.getFrame()) != JFileChooser.APPROVE_OPTION) {
			return;
		}
		File file = fileChooser.getSelectedFile();

		AtariDisk atariDisk = new AtariDisk(file.getPath());
		AtariFile info = new AtariFile();
		AtariError error;
		try {
			error = atariDisk.findFirst(info);
		} catch (IOException ex) {
			application.sendErrorMessage(ex);
			return;
		}
		if (error != AtariError.OK) {
			JOptionPane.showMessageDialog(mainWindow.getFrame(),
					"Could not read disk image '" + file.getPath() + "': " + error.getErrorText(),
					"Open Disk Image Executable File", JOptionPane.ERROR_MESSAGE);
			return;
		}

		DiskImageExecutableFileDialog dialog = new DiskImageExecutableFileDialog(mainWindow.getFrame());
		boolean confirmed;
		try {
			confirmed = dialog.show(atariDisk);
		} catch (IOException ex) {
			application.sendErrorMessage(ex);
			return;
		}
		if (!confirmed) {
			return;
		}

		byte[] fileBuffer;
		try {
			fileBuffer = atariDisk.readFile(dialog.getExecutableFileName());
		} catch (IOException ex) {
			application.sendErrorMessage(ex);
			return;
		}
		if (fileBuffer.length == 0) {
			JOptionPane.showMessageDialog(mainWindow.getFrame(), "File in disk image is empty.",
					"Open Disk Image Executable File", JOptionPane.ERROR_MESSAGE);
			return;
		}

		if (!add) {
			workspace.init();
			workspace.setComputerSystemTypeID("ATARI800");
			currentFile = null;
		}

		boolean success;
		try (InputStream inputStream = new ByteArrayInputStream(fileBuffer)) {
			success = workspaceLogic.addFile(workspace, FileType.EXECUTABLE_FILE, inputStream, fileBuffer.length);
		} catch (IOException ex) {
			application.sendErrorMessage(ex);
			return;
		}
		if (!success) {
			JOptionPane.showMessageDialog(mainWindow.getFrame(),
					"Could not " + (add ? "add" : "open") + " file '" + dialog.getExecutableFileName()
							+ "'. See the log for details.",
					(add ? "Add " : "Open ") + "Disk Image Executable File", JOptionPane.ERROR_MESSAGE);
			return;
		}

		mruController.addFile(file.getPath(), FileType.DISK_IMAGE_EXECUTABLE_FILE);
		mruController.save();
		refreshMRUMenus();
		mainWindow.segmentListPanel.refresh();
		updateDisassembly(true);
		updateTitle();
	}

	/**
	 * Opens or adds a disk image's Atari DOS boot sectors as a single
	 * segment. Ported from MainFile::OpenDiskImageBootSectors; the actual
	 * segment construction (including reading any boot sectors beyond the
	 * first) is {@link WorkspaceLogic#addDiskImageBootSectorsSegment}.
	 */
	private void performOpenDiskImageBootSectors(boolean add) {
		if (!add && !confirmClearWorkspace()) {
			return;
		}

		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setDialogTitle((add ? "Add " : "Open ") + "Disk Image Boot Sectors");
		if (currentFile != null) {
			fileChooser.setCurrentDirectory(currentFile.getParentFile());
		}
		if (fileChooser.showOpenDialog(mainWindow.getFrame()) != JFileChooser.APPROVE_OPTION) {
			return;
		}
		File file = fileChooser.getSelectedFile();

		ImgInfo info = new ImgInfo();
		DiskImage.getInfo(file.getPath(), info);
		if (DiskImage.isError(info.result)) {
			JOptionPane.showMessageDialog(mainWindow.getFrame(),
					"Could not read disk image '" + file.getPath() + "': " + info.result.getErrorText(),
					"Open Disk Image Boot Sectors", JOptionPane.ERROR_MESSAGE);
			return;
		}

		ImgRWPacket sector = new ImgRWPacket();
		sector.filePath = file.getPath();
		sector.sectorNumber = 1;
		sector.sectorSize = 128; // Atari boot sectors are always 128 bytes long.
		DiskImage.readSector(sector);
		if (DiskImage.isError(sector.result)) {
			JOptionPane.showMessageDialog(mainWindow.getFrame(),
					"Could not read the boot sector of '" + file.getPath() + "': " + sector.result.getErrorText(),
					"Open Disk Image Boot Sectors", JOptionPane.ERROR_MESSAGE);
			return;
		}
		if ((sector.sectorData[1] & 0xFF) == 0) {
			JOptionPane.showMessageDialog(mainWindow.getFrame(), "Disk image '" + file.getPath() + "' is not bootable.",
					"Open Disk Image Boot Sectors", JOptionPane.ERROR_MESSAGE);
			return;
		}

		if (!add) {
			workspace.init();
			workspace.setComputerSystemTypeID("ATARI800");
			currentFile = null;
		}

		workspaceLogic.addDiskImageBootSectorsSegment(workspace, file.getPath(), sector.sectorData);

		mruController.addFile(file.getPath(), FileType.DISK_IMAGE_BOOT_SECTORS);
		mruController.save();
		refreshMRUMenus();
		mainWindow.segmentListPanel.refresh();
		updateDisassembly(true);
		updateTitle();
	}

	/**
	 * Opens or adds one or more disk image sectors (or byte ranges within
	 * them) as segments, via {@link DiskImageSectorsDialog}. Ported from
	 * MainFile::OpenDiskImageSectors, kept inline here rather than folded
	 * into {@link WorkspaceLogic} (unlike {@link
	 * #performOpenDiskImageBootSectors}'s segment building) since the C++
	 * source itself keeps this loop in {@code MainFile}, not in a separate
	 * method - see {@link DiskImageSectorsDialog}'s javadoc for the bug
	 * found (but not fixed in C++) while porting this loop's body.
	 */
	private void performOpenDiskImageSectors(boolean add) {
		if (!add && !confirmClearWorkspace()) {
			return;
		}

		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setDialogTitle((add ? "Add " : "Open ") + "Disk Image Sectors");
		if (currentFile != null) {
			fileChooser.setCurrentDirectory(currentFile.getParentFile());
		}
		if (fileChooser.showOpenDialog(mainWindow.getFrame()) != JFileChooser.APPROVE_OPTION) {
			return;
		}
		File file = fileChooser.getSelectedFile();

		ImgInfo info = new ImgInfo();
		DiskImage.getInfo(file.getPath(), info);
		if (DiskImage.isError(info.result)) {
			JOptionPane.showMessageDialog(mainWindow.getFrame(),
					"Could not read disk image '" + file.getPath() + "': " + info.result.getErrorText(),
					"Open Disk Image Sectors", JOptionPane.ERROR_MESSAGE);
			return;
		}

		DiskImageSectorsDialog dialog = new DiskImageSectorsDialog(mainWindow.getFrame());
		if (!dialog.show(file.getPath(), info)) {
			return;
		}
		List<DiskImageSectorsDialog.Item> items = dialog.getItems();
		if (items.isEmpty()) {
			return;
		}

		if (!add) {
			workspace.init();
			workspace.setComputerSystemTypeID("ATARI800");
			currentFile = null;
		}

		SegmentListInserter segmentListInserter = workspace.getSegmentList().createInserter();
		try {
			for (DiskImageSectorsDialog.Item item : items) {
				Segment segment = segmentListInserter.insertSegment();
				segment.bBinary = true;
				segment.wBegin = item.address;
				segment.wEnd = item.address + item.size - 1;
				segment.createMemoryBlockFromBeginToEnd();

				int[] sectorSize = new int[1];
				byte[] sectorData = dialog.readSector(item.sectorNumber, sectorSize);
				segment.setData(0, sectorData, item.begin, item.size);
			}
			segmentListInserter.apply();
		} catch (RuntimeException ex) {
			segmentListInserter.cancel();
			application.sendErrorMessage(ex);
			return;
		}

		mruController.addFile(file.getPath(), FileType.DISK_IMAGE_SECTORS);
		mruController.save();
		refreshMRUMenus();
		mainWindow.segmentListPanel.refresh();
		updateDisassembly(true);
		updateTitle();
	}

	/** A short, human-readable name for a {@link FileType}, for dialog titles/messages. Ported ad hoc; the C++ source's fuller {@code FileTypeFactory} text lookup is not ported. */
	private static String getFileTypeDisplayName(FileType fileType) {
		switch (fileType) {
		case EXECUTABLE_FILE:
			return "Executable File";
		case ROM_IMAGE_FILE:
			return "ROM Image File";
		case CASSETTE_IMAGE_FILE:
			return "Cassette Image File";
		default:
			return "File";
		}
	}

	/** Ported from EquateListController::Clear. */
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
			updateDisassembly(false); // Matches Main::HandleWorkspaceChanged's non-forced refresh on a SYSTEM_EQUATES/USER_EQUATES change.
		}
	}

	/**
	 * Ported from EquateListController::Edit. Wraps the mutation {@link
	 * EquateDialog#show} performs on OK in {@code beginUpdate}/{@code
	 * endUpdate}, since {@link EquateList#clear()} (called first, to discard
	 * the list's old content before re-adding the edited lines) would
	 * otherwise fire an immediate "changed" notification while the list is
	 * still momentarily empty - before the re-add loop that follows it in
	 * the same method runs - matching the same "batch a multi-step mutation"
	 * pattern {@link WorkspaceLogic#load} already uses.
	 */
	private void performEditEquates(EquateList equateList, boolean editable) {
		workspace.beginUpdate();
		boolean changed;
		try {
			changed = new EquateDialog(mainWindow.getFrame()).show(equateList, editable, "");
		} finally {
			workspace.endUpdate();
		}
		if (changed) {
			updateDisassembly(false); // Matches Main::HandleWorkspaceChanged's non-forced refresh on a SYSTEM_EQUATES/USER_EQUATES change.
		}
	}

	/** Ported from EquateListController::DefineUserAddressRange. */
	private void performDefineUserAddressRange() {
		EquateRangeDialog dialog = new EquateRangeDialog(mainWindow.getFrame());
		if (dialog.show(workspace.getSystemEquateList(), workspace.getUserEquateList(), "")) {
			updateDisassembly(false); // Matches Main::HandleWorkspaceChanged's non-forced refresh on a SYSTEM_EQUATES/USER_EQUATES change.
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
		if (equateListLogic.load(workspace.getUserEquateList(), lastEquateFile.getPath())) {
			updateDisassembly(false); // Matches Main::HandleWorkspaceChanged's non-forced refresh on a SYSTEM_EQUATES/USER_EQUATES change.
		}
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

	/** Ported from MainSegment::PerformCommands's IDM_SEGMENT_MERGE case. */
	private void performMergeSegments() {
		if (workspace.getSegmentList().getCount() > 1) {
			int mergedCount = workspace.getSegmentList().mergeSegments();
			application.sendInfoMessage(Text.IDS_LOG_SEGMENTS_MERGED, String.valueOf(mergedCount));
		}
	}

	/** Ported from MainSegment::SaveSegment. */
	private void performSaveSegment(boolean writeHeader) {
		int segmentIndex = workspace.getSegmentList().getSelectedIndex();
		if (segmentIndex < 0) {
			return;
		}
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setDialogTitle("Save Segment");
		if (currentFile != null) {
			fileChooser.setCurrentDirectory(currentFile.getParentFile());
		}
		if (fileChooser.showSaveDialog(mainWindow.getFrame()) != JFileChooser.APPROVE_OPTION) {
			return;
		}
		try (FileOutputStream outputStream = new FileOutputStream(fileChooser.getSelectedFile())) {
			workspace.getComputerSystem().writeExecutableFile(workspace.getSegmentList(), segmentIndex, writeHeader, outputStream);
		} catch (IOException ex) {
			application.sendErrorMessage(ex);
		}
	}

	/** Ported from MainSegment::SaveAllSegments. */
	private void performSaveAllSegments() {
		if (workspace.getSegmentList().getCount() == 0) {
			return;
		}
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setDialogTitle("Save All Segments");
		if (currentFile != null) {
			fileChooser.setCurrentDirectory(currentFile.getParentFile());
		}
		if (fileChooser.showSaveDialog(mainWindow.getFrame()) != JFileChooser.APPROVE_OPTION) {
			return;
		}
		try (FileOutputStream outputStream = new FileOutputStream(fileChooser.getSelectedFile())) {
			workspace.getComputerSystem().writeExecutableFile(workspace.getSegmentList(), SegmentList.NO_SEGMENT_INDEX, true, outputStream);
		} catch (IOException ex) {
			application.sendErrorMessage(ex);
		}
	}

	/** Ported from MainSegment::ShowPropertiesDialog. */
	private void performShowSegmentProperties() {
		int segmentIndex = workspace.getSegmentList().getSelectedIndex();
		if (segmentIndex < 0) {
			return;
		}
		SegmentPropertiesDialog dialog = new SegmentPropertiesDialog(mainWindow.getFrame());
		if (dialog.show(workspace, workspace.getSegmentList().getSegment(segmentIndex))) {
			workspace.getSegmentList().notifySegmentContentChanged();
		}
	}

	/** Ported from the IDM_DUMP_FIND popup menu command, which opens MemoryInspectorFindStringDialog. */
	private void performShowMemoryInspectorFindDialog() {
		new MemoryInspectorFindStringDialog(mainWindow.getFrame()).show(mainWindow.memoryInspectorPanel);
	}

	/**
	 * Ported from the IDM_DUMP_FIND_NEXT popup menu command, which calls
	 * MemoryInspector::FindNextString directly. Shows the same "not found"
	 * alert {@link MemoryInspectorFindStringDialog#performOK} shows, since
	 * {@link com.wudsn.tools.dis6502.ui.MemoryInspectorPanel#findNextString}
	 * stays free of popups (see that class's javadoc).
	 */
	private void performMemoryInspectorFindNext() {
		if (!mainWindow.memoryInspectorPanel.findNextString()) {
			JOptionPane.showMessageDialog(mainWindow.getFrame(),
					"String \"" + mainWindow.memoryInspectorPanel.getFindString() + "\" not found.", "Find String",
					JOptionPane.INFORMATION_MESSAGE);
		}
	}

	/**
	 * Ported from MemoryInspector::SplitAtSelection (IDM_DUMP_SPLIT_AT_SELECTION):
	 * splits the selected segment into two at the current byte selection's
	 * start.
	 */
	private void performSplitAtSelection() {
		if (memoryInspectorSelection.isEmpty()) {
			return;
		}
		workspace.getSegmentList().splitSelectedSegment(memoryInspectorSelection.getBegin());
	}

	/**
	 * Ported from MainMemoryInspector::SaveWithoutHeader/SaveWithHeader
	 * (IDM_DUMP_SAVE_NO_HEADER/IDM_DUMP_SAVE_HEADER): writes the memory
	 * inspector's current byte selection to a file, optionally prefixed
	 * with a plain Atari executable header ($FFFF, begin address, end
	 * address).
	 */
	private void performSaveMemoryInspectorSelection(boolean withHeader) {
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setDialogTitle(withHeader ? "Save Selection (With Header)" : "Save Selection (No Header)");
		if (fileChooser.showSaveDialog(mainWindow.getFrame()) != JFileChooser.APPROVE_OPTION) {
			return;
		}
		try (FileOutputStream outputStream = new FileOutputStream(fileChooser.getSelectedFile())) {
			if (withHeader) {
				int[] addressRange = memoryInspectorSelection.getAddressRange();
				writeWordLE(outputStream, FileHeader.ATARI_BINARY.getValue());
				writeWordLE(outputStream, addressRange[0]);
				writeWordLE(outputStream, addressRange[1]);
			}
			outputStream.write(memoryInspectorSelection.getByteSequence());
		} catch (IOException ex) {
			application.sendErrorMessage(ex);
		}
	}

	private static void writeWordLE(FileOutputStream outputStream, int value) throws IOException {
		outputStream.write(value & 0xFF);
		outputStream.write((value >> 8) & 0xFF);
	}

	/**
	 * Ported from the type submenu's commands (IDM_DUMP_SET_TYPE_*), which
	 * all funnel into {@code MemoryInspector::SetType}. Routes the
	 * LOBYTE/HIBYTE case to {@link #performSetMemoryInspectorLoHiType},
	 * since it needs {@link LowHighByteDialog} and stricter validation;
	 * every other type goes straight through {@link
	 * com.wudsn.tools.dis6502.ui.MemoryInspectorPanel#setType}.
	 */
	private void performSetMemoryInspectorType() {
		MemoryType type = (MemoryType) mainWindow.memoryInspectorPanel.setTypeComboBox.getSelectedItem();
		if (type == MemoryType.LOBYTE || type == MemoryType.HIBYTE) {
			performSetMemoryInspectorLoHiType(type);
			return;
		}
		mainWindow.memoryInspectorPanel.setType(type);
		updateDisassembly(false);
	}

	/**
	 * Ported from the LOBYTE/HIBYTE branch of MemoryInspector::SetType: the
	 * marked byte must be alone at the start of an immediate-mode
	 * instruction's operand, one byte after the opcode - {@link
	 * LowHighByteDialog} then asks for the other, unknown half of the
	 * "assumed word", whose raw value is stored directly in the operand
	 * byte's type slot (see {@link MemoryType}'s javadoc on why {@code
	 * segment.memoryBlock.getType()} is written directly here instead of
	 * through {@link Segment#setType}, which only accepts a real {@link
	 * MemoryType} constant).
	 */
	private void performSetMemoryInspectorLoHiType(MemoryType type) {
		if (memoryInspectorSelection.isEmpty()) {
			return;
		}
		Segment segment = memoryInspectorSelection.getSegment();
		int begin = memoryInspectorSelection.getBegin();
		int size = memoryInspectorSelection.getSize();

		if (begin == 0) {
			JOptionPane.showMessageDialog(mainWindow.getFrame(), Text.IDS_ERR_LOHI_FIRST, "Set Type", JOptionPane.ERROR_MESSAGE);
			return;
		}
		if (size != 1) {
			JOptionPane.showMessageDialog(mainWindow.getFrame(), Text.IDS_ERR_MULTI_LOHI, "Set Type", JOptionPane.ERROR_MESSAGE);
			return;
		}

		int previousOpcode = segment.getData(begin - 1);
		InstructionSet instructionSet = workspace.getInstructionSet(segment.processorType);
		if (instructionSet.getInstruction(previousOpcode).getOperandMode() != OperandMode.Immediate) {
			JOptionPane.showMessageDialog(mainWindow.getFrame(), Text.IDS_ERR_BAD_MODE_FOR_LOHI, "Set Type", JOptionPane.ERROR_MESSAGE);
			return;
		}

		LowHighByteDialog dialog = new LowHighByteDialog(mainWindow.getFrame());
		if (!dialog.show(type, segment.getData(begin))) {
			return;
		}

		segment.setType(begin - 1, type);
		segment.memoryBlock.getType()[begin] = (byte) dialog.getUnknownByte();

		updateDisassembly(false);
	}

	/** Ported from MemoryInspector::SetUnknownBlockToByte (IDM_DUMP_SET_UNKNOWN_BLOCK_TO_BYTE). */
	private void performSetUnknownBlockToByte() {
		mainWindow.memoryInspectorPanel.setUnknownBlockToByte();
		updateDisassembly(false);
	}

	/**
	 * Ported from MemoryInspector::CopySelection (IDM_DUMP_COPY_SELECTION):
	 * copies the memory inspector's current byte selection to the system
	 * clipboard as a plain uppercase hex string with no separators or
	 * prefix, matching {@code DatatypeUtility::ByteArrayToHexString(...,
	 * false)}. Unlike the C++ version, which has no portable clipboard
	 * API and so leaves {@code UIApplication::SetClipboardText} Win32-only
	 * (see that class's javadoc), {@link java.awt.datatransfer.Clipboard}
	 * is a real, standard Java equivalent, used directly here rather than
	 * through {@link UIApplication}.
	 */
	private void performCopyMemoryInspectorSelection() {
		if (memoryInspectorSelection.isEmpty()) {
			return;
		}
		StringBuilder hex = new StringBuilder();
		for (byte value : memoryInspectorSelection.getByteSequence()) {
			hex.append(String.format("%02X", value & 0xFF));
		}
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(hex.toString()), null);
	}

	/**
	 * Ported from MainDisassembly::AddComment (IDM_DUMP_EDIT_COMMENT),
	 * simplified to always use the memory inspector's own byte selection -
	 * see {@link CommentDialog}'s javadoc for why the C++ version's other
	 * trigger path (a plain disassembly-line click, snapping to the
	 * enclosing instruction) uses the clicked line's own offset/size
	 * directly instead - see {@link #performEditDisassemblyComment}.
	 */
	private void performEditMemoryInspectorComment() {
		if (memoryInspectorSelection.isEmpty()) {
			return;
		}
		CommentDialog dialog = new CommentDialog(mainWindow.getFrame());
		if (dialog.show(workspace.getSegmentList(), memoryInspectorSelection.getSegmentIndex(), memoryInspectorSelection.getBegin(),
				memoryInspectorSelection.getSize())) {
			updateDisassembly(false);
		}
	}

	/**
	 * Ported from {@code MainDisassembly::AddComment}, triggered by {@link
	 * DisassemblyPanel}'s right-click popup menu instead of a plain click
	 * (see that class's javadoc for why {@code DisassemblyResult::
	 * FindOffsetAtStartOfInstruction}'s snap-to-enclosing-instruction is not
	 * ported): uses the right-clicked line's own segment/offset/size
	 * directly.
	 */
	private void performEditDisassemblyComment() {
		DisassemblyLine line = mainWindow.disassemblyPanel.getRightClickedLine();
		if (line == null || line.segmentIndex == SegmentList.NO_SEGMENT_INDEX) {
			return;
		}
		CommentDialog dialog = new CommentDialog(mainWindow.getFrame());
		if (dialog.show(workspace.getSegmentList(), line.segmentIndex, line.offset, line.size)) {
			updateDisassembly(false);
		}
	}

	/** Ported from MainDisassembly::FindDef, minus the navigation-history save (Back in History is not ported). */
	private void performFindDisassemblyLabelDefinition(String label) {
		if (label.isEmpty()) {
			return;
		}
		int lineNumber = workspace.getDisassemblyResult().findDefinitionLineNumber(label);
		if (lineNumber != 0) {
			mainWindow.disassemblyPanel.navigateToLine(lineNumber);
		}
	}

	/**
	 * Ported from MainDisassembly::FindRef1/FindRef2 (both just call this
	 * with a different label) - the same find-and-populate-XRef shape as
	 * {@link #performFindInDisassembly}, but keyed to a label instead of the
	 * find field's text, and using a fresh, local line-number holder so it
	 * does not disturb the ongoing Find/Find Next search state, matching how
	 * the C++ version's {@code SelectAllReferences}/{@code RefreshXRef} never
	 * touch {@code findString}/{@code findFirstLineNumber} either.
	 */
	private void performFindDisassemblyReferences(String label) {
		if (label.isEmpty()) {
			return;
		}
		DisassemblyResult disassemblyResult = workspace.getDisassemblyResult();
		int[] firstLineNumber = { 0 };
		boolean found = disassemblyResult.findAndSelectLines(true, firstLineNumber, label);

		List<XRefPanel.Entry> entries = new ArrayList<>();
		for (DisassemblyResult.LineIterator i = disassemblyResult.createLineIterator(); i.hasNext();) {
			DisassemblyLine line = i.next();
			if (line.xrefLineNumber != 0) {
				entries.add(new XRefPanel.Entry(line.xrefLineNumber, line.getLine()));
			}
		}
		mainWindow.xrefPanel.updateList(label, entries);

		if (found) {
			mainWindow.disassemblyPanel.navigateToLine(firstLineNumber[0]);
		}
	}

	/** Ported from MainDisassembly::RenameDef/RenameRef (both just call this with a different label), via EquateListController::Edit. */
	private void performRenameDisassemblyLabel(String label) {
		String address = Equate.extractAddress(label);
		if (address.isEmpty()) {
			return;
		}
		EquateDialog dialog = new EquateDialog(mainWindow.getFrame());
		if (dialog.show(workspace.getUserEquateList(), true, address)) {
			updateDisassembly(false);
		}
	}

	/** Ported from MainDisassembly::AddrRangeDef/AddrRangeRef (both just call this with a different label), via EquateListController::DefineUserAddressRange. */
	private void performDefineAddressRangeForLabel(String label) {
		String address = Equate.extractAddress(label);
		if (address.isEmpty()) {
			return;
		}
		EquateRangeDialog dialog = new EquateRangeDialog(mainWindow.getFrame());
		if (dialog.show(workspace.getSystemEquateList(), workspace.getUserEquateList(), address)) {
			updateDisassembly(false);
		}
	}

	/**
	 * Ported from MainMemoryInspector::PerformCommands's IDM_DUMP_ASSEMBLE
	 * case: unlike every other memory inspector action wired in this
	 * class, this calls {@link #updateDisassembly} unconditionally once
	 * the dialog closes, matching the C++ call site, which does not check
	 * {@code AssembleDialog::Show}'s return value either.
	 */
	private void performShowAssembleDialog() {
		if (memoryInspectorSelection.isEmpty()) {
			return;
		}
		new AssembleDialog(mainWindow.getFrame()).show(workspace, memoryInspectorSelection.getSegment(), memoryInspectorSelection,
				mainWindow.memoryInspectorPanel);
		updateDisassembly(false);
	}

	/** Ported from MemoryInspector::Guess (IDM_DUMP_START_CODE_TRACE). */
	private void performGuessCode() {
		mainWindow.memoryInspectorPanel.guess();
		updateDisassembly(false);
	}

	/**
	 * Ported from MemoryInspector::ShowSelectSpritesDialog
	 * (IDM_DUMP_SELECT_SPRITES): non-mutating, unlike its neighbors above -
	 * just ends in the same {@link
	 * com.wudsn.tools.dis6502.ui.MemoryInspectorPanel#select} every other
	 * Select* action uses, so no {@link #updateDisassembly} call follows.
	 */
	private void performShowSelectSpritesDialog() {
		if (!memoryInspectorSelection.hasSegment()) {
			return;
		}
		SelectSpritesDialog dialog = new SelectSpritesDialog(mainWindow.getFrame());
		if (dialog.show(memoryInspectorSelection)) {
			mainWindow.memoryInspectorPanel.select(dialog.getBegin(), dialog.getEnd());
		}
	}

	/**
	 * Ported from MemoryInspector::ToggleDisplayAsScreenCode. Switches the
	 * Memory Inspector's ASCII column between plain byte values and their
	 * Atari internal (ANTIC screen code) equivalent.
	 */
	private void performToggleDisplayAsScreenCode() {
		workspace.setViewDisplayAsScreenCode(mainWindow.mainMenu.displayAsScreenCodeMenuItem.isSelected());
		mainWindow.memoryInspectorPanel.setDisplayAsScreenCode(workspace.isViewDisplayAsScreenCode());
	}

	/** Ported from Main::ToggleViewDisassembly. */
	private void performToggleViewDisassembly() {
		workspace.setViewNoDisassembly(mainWindow.mainMenu.noDisassemblyMenuItem.isSelected());
		updateDisassembly(false); // Will do nothing if it is now true, matching the C++ comment at the same spot.
	}

	/**
	 * Ported from Main::ToggleViewDoubleFontHeight. The font-resizing this
	 * notification is meant to trigger ({@code Main::SetLayoutFont}/{@code
	 * layout->Compute}) is fully ported: {@link #updateFonts} reacts to it,
	 * switching both {@link com.wudsn.tools.dis6502.ui.MemoryInspectorPanel}
	 * and {@link com.wudsn.tools.dis6502.ui.DisassemblyPanel} between {@link
	 * ComputerFont#get}'s normal/double-height glyph atlases.
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

	/** Ported from Main::ShowProfileDialog. */
	private void performShowProfile() {
		ProfileDialog dialog = new ProfileDialog(mainWindow.getFrame(), profileLogic);
		if (dialog.show(workspace.getProfile(), workspace.getComputerSystem().getTypeInfo())) {
			workspace.notifyProfileChanged();
			updateDisassembly(true); // Matches Main::HandleWorkspaceChanged's forced refresh on a PROFILE change.
		}
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
	 * Ported from MainFile::SaveDisassemblyFiles: writes the current
	 * disassembly listing (split into include files as {@link
	 * Workspace#getProfile()} dictates) via the already-complete {@link
	 * DisassemblyResultFile#saveListing}.
	 */
	private void performSaveDisassemblyFiles() {
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setDialogTitle("Save Disassembly Files");
		fileChooser.setFileFilter(new FileNameExtensionFilter("Assembler Files (*.asm)", "asm"));
		if (fileChooser.showSaveDialog(mainWindow.getFrame()) != JFileChooser.APPROVE_OPTION) {
			return;
		}
		File file = fileChooser.getSelectedFile();
		try {
			new DisassemblyResultFile().saveListing(workspace.getDisassemblyResult(), workspace.getProfile(), file);
		} catch (IOException ex) {
			application.sendErrorMessage(ex);
		}
	}

	/**
	 * Turns the memory inspector's currently-selected segment into a bootable
	 * Atari DOS disk, via {@link SegmentWriteBootDiskDialog}. Ported from the
	 * {@code ID_FILE_SAVE_DISK_IMAGE_BOOT_SECTORS} case in {@code
	 * MainMenu::ProcessCommand} - including its own {@code // TODO Move to
	 * segment context menu} comment (never acted on in the C++ source, so this
	 * stays a File menu action here too) and its {@code $02E0} run-address scan
	 * ({@code // TODO: This is Atari specific} in the original).
	 */
	private void performWriteBootDisk() {
		Segment segment = memoryInspectorSelection.getSegment();
		if (segment == null) {
			application.sendErrorMessage(Text.IDS_ERR_NO_SEGMENT);
			return;
		}

		boolean withRunAddress = false;
		int runAddress = 0;
		SegmentList segmentList = workspace.getSegmentList();
		for (int segmentIndex = 0; segmentIndex < segmentList.getCount(); segmentIndex++) {
			Segment candidate = segmentList.getSegment(segmentIndex);
			if (candidate.wBegin == 0x02E0) { // TODO: This is Atari specific.
				withRunAddress = true;
				runAddress = candidate.getWord(0);
			}
		}

		new SegmentWriteBootDiskDialog(mainWindow.getFrame()).show(segment, withRunAddress, runAddress);
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
	 * always forces. Runs behind a {@link DisassemblyProgressDialog}, matching
	 * {@code Main::UpdateDisassembly}'s own {@code DisassemblyProgressDialog}
	 * use - see that class's javadoc for why it runs the disassembly on a
	 * background thread instead of the C++ mechanism.
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
		DisassemblyProgressDialog progressDialog = new DisassemblyProgressDialog(mainWindow.getFrame());
		DisassemblyProgressMonitor progressMonitor = progressDialog.getMonitor();
		disassembly.setWorkspace(workspace);
		disassembly.setProgressMonitor(progressMonitor);
		try {
			progressMonitor.startDisassembly(disassembly);
		} catch (RuntimeException ex) {
			application.sendErrorMessage(ex);
		}
		mainWindow.disassemblyPanel.refresh(workspace.getDisassemblyResult());
		mainWindow.disassemblyPanel.findField.setText("");
		mainWindow.xrefPanel.updateList("", Collections.emptyList());
	}

	/**
	 * Searches the current disassembly for lines containing the {@link
	 * DisassemblyPanel#findField} text, ported from {@code
	 * MainDisassembly::Find}/{@code FindString} and {@code RefreshXRef}
	 * (triggered there from a label double-click; this port uses an
	 * explicit search field instead - see {@link DisassemblyPanel}'s
	 * javadoc). Populates {@link XRefPanel} with every matching line and
	 * scrolls the disassembly view to the first one.
	 */
	private void performFindInDisassembly() {
		DisassemblyResult disassemblyResult = workspace.getDisassemblyResult();
		if (disassemblyResult == null) {
			return;
		}
		String findString = mainWindow.disassemblyPanel.findField.getText();
		findFirstLineNumber[0] = 0;
		boolean found = disassemblyResult.findAndSelectLines(true, findFirstLineNumber, findString);

		List<XRefPanel.Entry> entries = new ArrayList<>();
		if (!findString.isEmpty()) {
			for (DisassemblyResult.LineIterator i = disassemblyResult.createLineIterator(); i.hasNext();) {
				DisassemblyLine line = i.next();
				if (line.xrefLineNumber != 0) {
					entries.add(new XRefPanel.Entry(line.xrefLineNumber, line.getLine()));
				}
			}
		}
		mainWindow.xrefPanel.updateList(findString, entries);

		if (found) {
			mainWindow.disassemblyPanel.navigateToLine(findFirstLineNumber[0]);
		}
	}

	/**
	 * Ported from {@code MainDisassembly::FindNextString(false)}, bound in
	 * C++ to ID_DIS_FIND_NEXT: continues the last search from {@link
	 * #findFirstLineNumber} rather than starting over, matching the "not
	 * found" alert {@code FindNextString} shows via {@code
	 * FindStringDialog::ShowStringNotFoundMessage}.
	 */
	private void performFindNextInDisassembly() {
		DisassemblyResult disassemblyResult = workspace.getDisassemblyResult();
		String findString = mainWindow.disassemblyPanel.findField.getText();
		if (disassemblyResult == null || findString.isEmpty()) {
			return;
		}
		boolean found = disassemblyResult.findAndSelectLines(false, findFirstLineNumber, findString);
		if (found) {
			mainWindow.disassemblyPanel.navigateToLine(findFirstLineNumber[0]);
		} else {
			JOptionPane.showMessageDialog(mainWindow.getFrame(), "String \"" + findString + "\" not found.", "Find String",
					JOptionPane.INFORMATION_MESSAGE);
		}
	}

	/** Ported from MainDisassembly::XRefSelected. */
	private void performXRefSelected(int xrefLineNumber) {
		DisassemblyResult disassemblyResult = workspace.getDisassemblyResult();
		if (disassemblyResult == null) {
			return;
		}
		for (DisassemblyResult.LineIterator i = disassemblyResult.createLineIterator(); i.hasNext();) {
			DisassemblyLine line = i.next();
			if (line.xrefLineNumber == xrefLineNumber && line.segmentIndex != SegmentList.NO_SEGMENT_INDEX) {
				// Selecting the segment refreshes the memory inspector's own hex
				// dump (via updateMemoryInspectorSegment, on the SELECTED_SEGMENT
				// listener) before the explicit select()/clearSelection() below.
				workspace.getSegmentList().setSelectedIndex(line.segmentIndex);
				if (line.size != 0) {
					mainWindow.memoryInspectorPanel.select(line.offset, line.offset + line.size - 1);
				} else {
					mainWindow.memoryInspectorPanel.clearSelection();
				}
				mainWindow.disassemblyPanel.navigateToLine(line.getLineNumber());
				return;
			}
		}
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
