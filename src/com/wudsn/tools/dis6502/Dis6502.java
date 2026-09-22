/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502;

import java.awt.EventQueue;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
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

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.TransferHandler;
import javax.swing.UIManager;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import com.wudsn.tools.base.common.TextUtility;
import com.wudsn.tools.base.repository.Message;
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
import com.wudsn.tools.dis6502.model.DisassemblyWriter;
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
import com.wudsn.tools.dis6502.model.MutableMemoryInspectorState;
import com.wudsn.tools.dis6502.model.MemoryType;
import com.wudsn.tools.dis6502.model.OperandMode;
import com.wudsn.tools.dis6502.model.ProfileLogic;
import com.wudsn.tools.dis6502.model.Segment;
import com.wudsn.tools.dis6502.model.SegmentList;
import com.wudsn.tools.dis6502.model.SegmentListInserter;
import com.wudsn.tools.dis6502.model.Workspace;
import com.wudsn.tools.dis6502.model.WorkspaceLogic;
import com.wudsn.tools.dis6502.model.WorkspaceProperty;
import com.wudsn.tools.dis6502.ui.AboutDialog;
import com.wudsn.tools.dis6502.ui.AssembleDialog;
import com.wudsn.tools.dis6502.ui.CommentDialog;
import com.wudsn.tools.dis6502.ui.ComputerFont;
import com.wudsn.tools.dis6502.ui.DefaultFoldersDialog;
import com.wudsn.tools.dis6502.ui.DisassemblyPanel;
import com.wudsn.tools.dis6502.ui.DisassemblyProgressDialog;
import com.wudsn.tools.dis6502.ui.DiskImageExecutableFileDialog;
import com.wudsn.tools.dis6502.ui.DiskImageSectorsDialog;
import com.wudsn.tools.dis6502.ui.EquateDialog;
import com.wudsn.tools.dis6502.ui.EquateRangeDialog;
import com.wudsn.tools.dis6502.ui.FileChoosers;
import com.wudsn.tools.dis6502.ui.LowHighByteDialog;
import com.wudsn.tools.dis6502.ui.MRUController;
import com.wudsn.tools.dis6502.ui.MainMenu;
import com.wudsn.tools.dis6502.ui.MainWindow;
import com.wudsn.tools.dis6502.ui.MemoryInspectorFindStringDialog;
import com.wudsn.tools.dis6502.ui.ProfileDialog;
import com.wudsn.tools.dis6502.ui.RawFileDialog;
import com.wudsn.tools.dis6502.ui.SegmentPropertiesDialog;
import com.wudsn.tools.dis6502.ui.SegmentWriteBootDiskDialog;
import com.wudsn.tools.dis6502.ui.SelectGraphicsDialog;
import com.wudsn.tools.dis6502.ui.UIApplication;
import com.wudsn.tools.dis6502.ui.WorkspaceDialog;
import com.wudsn.tools.dis6502.ui.XRefPanel;

/**
 * Application entry point. Follows the same bootstrap pattern as
 * com.wudsn.tools.thecartstudio.TheCartStudio: create the wudsn-base
 * {@code Application} singleton, then build the UI on the Swing event
 * dispatch thread.
 * <p>
 * The main window shell (see {@link MainWindow}) plus workspace New (see
 * {@link #performNewWorkspace}/{@link WorkspaceDialog})/Open/Save/Save
 * As/Save Disassembly Files (see {@link #performSaveDisassemblyFiles})/
 * Write Boot Disk (see {@link #performWriteBootDisk}/{@link
 * SegmentWriteBootDiskDialog})/Exit, opening/adding an executable, ROM
 * image, cassette image, raw, disk image executable, disk image boot
 * sectors, or disk image sectors file (see {@link RawFileDialog}/{@link
 * DiskImageExecutableFileDialog}/{@link #openDiskImageBootSectors}/{@link
 * DiskImageSectorsDialog}), loading/saving/clearing/exporting/editing
 * equates and defining a user equate address range (see {@link
 * EquateDialog}/{@link EquateRangeDialog}), the View menu's Display as
 * Screen Code/No Disassembly/Double Font Height toggles and Default
 * Folders/Profile dialogs (see {@link DefaultFoldersDialog}/{@link
 * ProfileDialog}), and Help &gt; About. Every way of opening a file -
 * menu, Recent Files/Workspaces, the command line (see {@link
 * CommandLineArguments}), drag and drop - goes through {@link
 * #openFile}; every file chooser through {@link FileChoosers}. {@link
 * #updateDisassembly} is called explicitly after every action that could
 * affect it, rather than through a reactive change-dispatcher. {@link
 * #performFindInDisassembly}/{@link #performFindNextInDisassembly}/{@link
 * #performXRefSelected} wire the disassembly search field/buttons to
 * {@link XRefPanel}, and (via {@link #updateMemoryInspectorSegment}) keep
 * {@link com.wudsn.tools.dis6502.ui.MemoryInspectorPanel}'s hex dump in
 * sync with the selected segment - see that class's javadoc for what it
 * does and doesn't cover. The segment list's popup menu commands (Move
 * Up/Down, Merge, Delete, Save Segment/Save All Segments, Properties...)
 * are wired here too, from {@code
 * com.wudsn.tools.dis6502.ui.SegmentListPanel}'s exposed menu items, and
 * {@link #performShowSegmentProperties} uses {@link
 * SegmentPropertiesDialog}.
 * <p>
 * Every {@code com.wudsn.tools.dis6502.ui.MemoryInspectorPanel} command is
 * likewise wired here from a public popup menu item field, since that
 * panel has no toolbar of its own - see its class javadoc for why. {@link
 * #performShowMemoryInspectorFindDialog}/{@link
 * #performMemoryInspectorFindNext} wire its Find/Find Next items to
 * {@link MemoryInspectorFindStringDialog}, and {@link
 * #performSplitAtSelection} wires its Split at Selection item. {@link
 * #performSaveMemoryInspectorSelection} wires the Save Selection items;
 * the Select All/Select Next Unknown Block items just call {@code
 * com.wudsn.tools.dis6502.ui.MemoryInspectorPanel#selectAll}/{@code
 * #selectNextUnknownBlock} directly. {@link #performSetMemoryInspectorType}/
 * {@link #performSetMemoryInspectorLoHiType}/{@link
 * #performSetUnknownBlockToByte} wire the Change Type submenu (via {@code
 * setTypeSelectionListener}, since each of its items already knows its
 * own type - see that panel's javadoc) and Set Unknown Block to Byte item
 * - the first commands here that mutate a segment's byte types, calling
 * {@link #updateDisassembly} afterward to reflect the change, the same
 * way editing equates already does. {@link
 * #performCopyMemoryInspectorSelection} wires the Copy item; {@link
 * #performCutMemoryInspectorSelection}/{@link
 * #performPasteMemoryInspectorSelection}/{@link
 * #performDeleteMemoryInspectorSelection} wire Cut/Paste/Delete - see
 * {@code MemoryInspectorPanel}'s class javadoc for their design. {@link
 * #performEditMemoryInspectorComment} wires the Add/Edit comment... item
 * via {@link CommentDialog}. {@link #performShowAssembleDialog} wires the
 * Assemble at selection... item via {@link AssembleDialog}. {@link
 * #performGuessCode} wires the Start code trace at selection item via
 * {@code com.wudsn.tools.dis6502.model.GuessCodeLogic}. {@link
 * #performShowSelectGraphicsDialog} wires the Select Graphics... item via
 * {@link SelectGraphicsDialog} - see that class's own javadoc for why
 * these are called "Graphic", not "Sprite".
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
	private FileChoosers fileChoosers;
	private MutableMemoryInspectorState memoryInspectorState;
	private File currentFile;
	private File lastEquateFile;
	private final int[] findFirstLineNumber = { 0 };

	public static void main(final String[] args) {

		// Use the event dispatch thread for Swing components.
		EventQueue.invokeLater(new Runnable() {

			@Override
			public void run() {

				setNativeLookAndFeel();

				com.wudsn.tools.base.common.Application.createInstance(
						"https://www.wudsn.com/tools/dis6502/dis6502.zip", "dis6502.jar", Dis6502.class);
				instance = new Dis6502();
				instance.run(args);
			}
		});
	}

	/**
	 * Switches from Swing's default cross-platform "Metal" look and feel to
	 * the host OS's native one - failures are swallowed and left at the
	 * cross-platform default, matching {@link javax.swing.UIManager}'s own
	 * documented fallback behavior for a look and feel that cannot be
	 * instantiated on the current platform.
	 */
	private static void setNativeLookAndFeel() {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception ex) {
			// Ignore: keep whatever look and feel Swing already selected.
		}
	}

	Dis6502() {
	}

	// Package-private, for UIWiringTest (same package): the running instance and the
	// state its scenarios assert on. The perform methods it drives directly - the ones
	// without a public menu item to click - are package-private for the same reason.
	static Dis6502 getInstance() {
		return instance;
	}

	Workspace getWorkspace() {
		return workspace;
	}

	MainWindow getMainWindow() {
		return mainWindow;
	}

	void run(String[] args) {
		application = new UIApplication();
		ComputerSystemFactory computerSystemFactory = new ComputerSystemFactory();
		CommandLineArguments commandLineArguments = CommandLineArguments.parse(args);
		workspaceLogic = new WorkspaceLogic(application);
		equateListLogic = new EquateListLogic(application);
		defaultFoldersLogic = new DefaultFoldersLogic(application);
		profileLogic = new ProfileLogic(application);
		workspace = new Workspace(computerSystemFactory);
		workspace.setComputerSystemTypeID(
				commandLineArguments.computerSystemTypeID != null ? commandLineArguments.computerSystemTypeID : "ATARI800");
		mruController = new MRUController(application);
		mruController.load();
		fileChoosers = new FileChoosers(mruController, this::getDefaultFolders);
		memoryInspectorState = workspace.getMemoryInspectorState(); // Workspace owns this instance directly now, not a satellite object constructed here.

		mainWindow = new MainWindow();
		application.setLogPanel(mainWindow.logPanel);
		application.sendMessage(Messages.I001, Texts.Dis6502_Version, Texts.Dis6502_VersionDate);
		mainWindow.segmentListPanel.setWorkspace(workspace);
		mainWindow.segmentListPanel.moveUpMenuItem.addActionListener(e -> workspace.getSegmentList().moveSelectedSegmentUp());
		mainWindow.segmentListPanel.moveDownMenuItem.addActionListener(e -> workspace.getSegmentList().moveSelectedSegmentDown());
		mainWindow.segmentListPanel.mergeMenuItem.addActionListener(e -> performMergeSegments());
		mainWindow.segmentListPanel.deleteMenuItem.addActionListener(e -> performDeleteSelectedSegments());
		mainWindow.segmentListPanel.saveNoHeaderMenuItem.addActionListener(e -> performSaveSegment(false));
		mainWindow.segmentListPanel.saveHeaderMenuItem.addActionListener(e -> performSaveSegment(true));
		mainWindow.segmentListPanel.saveAllMenuItem.addActionListener(e -> performSaveAllSegments());
		mainWindow.segmentListPanel.propertiesMenuItem.addActionListener(e -> performShowSegmentProperties());
		workspace.addListener((changedWorkspace, properties) -> {
			if (properties.contains(WorkspaceProperty.SYSTEM_EQUATES) || properties.contains(WorkspaceProperty.USER_EQUATES)) {
				updateEquatesMenuState();
			}
			if (properties.contains(WorkspaceProperty.SEGMENTS) || properties.contains(WorkspaceProperty.COMPUTER_SYSTEM_TYPE)) {
				updateFileMenuState();
			}
			if (properties.contains(WorkspaceProperty.SELECTED_SEGMENT)) {
				updateMemoryInspectorSegment();
			}
			if (properties.contains(WorkspaceProperty.COMPUTER_SYSTEM_TYPE)) {
				loadSystemEquatesIfEmpty();
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
		mainWindow.mainMenu.openWorkspaceMenuItem.addActionListener(e -> performOpenFile(FileType.WORKSPACE_FILE, false));
		mainWindow.mainMenu.openAnyFileMenuItem.addActionListener(e -> performOpenFile(FileType.ANY_FILE, false));
		mainWindow.mainMenu.addAnyFileMenuItem.addActionListener(e -> performOpenFile(FileType.ANY_FILE, true));
		mainWindow.mainMenu.openCassetteImageFileMenuItem.addActionListener(e -> performOpenFile(FileType.CASSETTE_IMAGE_FILE, false));
		mainWindow.mainMenu.addCassetteImageFileMenuItem.addActionListener(e -> performOpenFile(FileType.CASSETTE_IMAGE_FILE, true));
		mainWindow.mainMenu.openExecutableFileMenuItem.addActionListener(e -> performOpenFile(FileType.EXECUTABLE_FILE, false));
		mainWindow.mainMenu.addExecutableFileMenuItem.addActionListener(e -> performOpenFile(FileType.EXECUTABLE_FILE, true));
		mainWindow.mainMenu.openROMImageFileMenuItem.addActionListener(e -> performOpenFile(FileType.ROM_IMAGE_FILE, false));
		mainWindow.mainMenu.addROMImageFileMenuItem.addActionListener(e -> performOpenFile(FileType.ROM_IMAGE_FILE, true));
		mainWindow.mainMenu.openRawFileMenuItem.addActionListener(e -> performOpenFile(FileType.RAW_FILE, false));
		mainWindow.mainMenu.addRawFileMenuItem.addActionListener(e -> performOpenFile(FileType.RAW_FILE, true));
		mainWindow.mainMenu.openDiskImageExecutableFileMenuItem.addActionListener(e -> performOpenFile(FileType.DISK_IMAGE_EXECUTABLE_FILE, false));
		mainWindow.mainMenu.addDiskImageExecutableFileMenuItem.addActionListener(e -> performOpenFile(FileType.DISK_IMAGE_EXECUTABLE_FILE, true));
		mainWindow.mainMenu.openDiskImageBootSectorsMenuItem.addActionListener(e -> performOpenFile(FileType.DISK_IMAGE_BOOT_SECTORS, false));
		mainWindow.mainMenu.addDiskImageBootSectorsMenuItem.addActionListener(e -> performOpenFile(FileType.DISK_IMAGE_BOOT_SECTORS, true));
		mainWindow.mainMenu.openDiskImageSectorsMenuItem.addActionListener(e -> performOpenFile(FileType.DISK_IMAGE_SECTORS, false));
		mainWindow.mainMenu.addDiskImageSectorsMenuItem.addActionListener(e -> performOpenFile(FileType.DISK_IMAGE_SECTORS, true));
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
				.addActionListener(e -> performFindDisassemblyLabelDefinition(mainWindow.disassemblyPanel.getRightClickedLabelReference(),
						mainWindow.disassemblyPanel.getRightClickedLine()));
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
		mainWindow.disassemblyPanel.setLineSelectionListener(this::performDisassemblyLineSelected);
		mainWindow.disassemblyPanel.setNavigateToDefinitionListener(label -> performFindDisassemblyLabelDefinition(label, null));
		mainWindow.disassemblyPanel.setImmediateTypeProvider(this::getDisassemblyImmediateType);
		mainWindow.disassemblyPanel.setImmediateTypeListener(this::performSetDisassemblyImmediateType);
		mainWindow.xrefPanel.setSelectionListener(this::performXRefSelected);

		mainWindow.memoryInspectorPanel.displayAsScreenCodeButton.setSelected(workspace.isViewDisplayAsScreenCode());
		mainWindow.memoryInspectorPanel.displayAsScreenCodeButton.addActionListener(e -> performToggleDisplayAsScreenCode());
		mainWindow.memoryInspectorPanel.findMenuItem.addActionListener(e -> performShowMemoryInspectorFindDialog());
		mainWindow.memoryInspectorPanel.findNextMenuItem.addActionListener(e -> performMemoryInspectorFindNext());
		mainWindow.memoryInspectorPanel.splitAtSelectionMenuItem.addActionListener(e -> performSplitAtSelection());
		mainWindow.memoryInspectorPanel.selectAllMenuItem.addActionListener(e -> mainWindow.memoryInspectorPanel.selectAll());
		mainWindow.memoryInspectorPanel.selectNextUnknownBlockMenuItem
				.addActionListener(e -> mainWindow.memoryInspectorPanel.selectNextUnknownBlock());
		mainWindow.memoryInspectorPanel.selectGraphicsMenuItem.addActionListener(e -> performShowSelectGraphicsDialog());
		mainWindow.memoryInspectorPanel.saveSelectionNoHeaderMenuItem.addActionListener(e -> performSaveMemoryInspectorSelection(false));
		mainWindow.memoryInspectorPanel.saveSelectionHeaderMenuItem.addActionListener(e -> performSaveMemoryInspectorSelection(true));
		mainWindow.memoryInspectorPanel.setTypeSelectionListener(this::performSetMemoryInspectorType);
		mainWindow.memoryInspectorPanel.setSelectionChangedListener(this::performMemoryInspectorSelectionChanged);
		mainWindow.memoryInspectorPanel.setUnknownBlockToByteMenuItem.addActionListener(e -> performSetUnknownBlockToByte());
		mainWindow.memoryInspectorPanel.cutSelectionMenuItem.addActionListener(e -> performCutMemoryInspectorSelection());
		mainWindow.memoryInspectorPanel.copySelectionMenuItem.addActionListener(e -> performCopyMemoryInspectorSelection());
		mainWindow.memoryInspectorPanel.pasteSelectionMenuItem.addActionListener(e -> performPasteMemoryInspectorSelection());
		mainWindow.memoryInspectorPanel.deleteSelectionMenuItem.addActionListener(e -> performDeleteMemoryInspectorSelection());
		mainWindow.memoryInspectorPanel.editCommentMenuItem.addActionListener(e -> performEditMemoryInspectorComment());
		mainWindow.memoryInspectorPanel.editMenuItem.addActionListener(e -> mainWindow.memoryInspectorPanel.enterEditMode());
		mainWindow.memoryInspectorPanel.quitEditModeMenuItem.addActionListener(e -> mainWindow.memoryInspectorPanel.quitEditMode());
		mainWindow.memoryInspectorPanel.setEditModeExitedListener(this::performMemoryInspectorEditModeExited);
		mainWindow.memoryInspectorPanel.setEditModeEnteredListener(this::updateFileMenuState);
		// Safety net: refreshes the File menu right before it opens, catching anything that changed since the last explicit update.
		mainWindow.mainMenu.fileMenu.addMenuListener(new MenuListener() {
			@Override
			public void menuSelected(MenuEvent e) {
				updateFileMenuState();
			}

			@Override
			public void menuDeselected(MenuEvent e) {
			}

			@Override
			public void menuCanceled(MenuEvent e) {
			}
		});
		mainWindow.memoryInspectorPanel.assembleMenuItem.addActionListener(e -> performShowAssembleDialog());
		mainWindow.memoryInspectorPanel.startCodeTraceMenuItem.addActionListener(e -> performGuessCode());

		loadSystemEquatesIfEmpty(); // The computer system was set before the workspace listener above existed.
		refreshMRUMenus();
		updateFileMenuState();
		updateEquatesMenuState();
		updateFonts();
		updateMemoryInspectorSegment();
		updateTitle();
		mainWindow.getFrame().setTransferHandler(new FileDropHandler());
		mainWindow.setVisible(true);

		if (commandLineArguments.file != null) {
			openFile(commandLineArguments.file, FileType.ANY_FILE, false);
		}
	}

	/**
	 * Files dropped anywhere on the main window: the first one is opened,
	 * any further ones are added to it, each with its type guessed by {@link
	 * #openFile}.
	 */
	private final class FileDropHandler extends TransferHandler {

		private static final long serialVersionUID = 1L;

		@Override
		public boolean canImport(TransferSupport support) {
			return support.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
		}

		@Override
		public boolean importData(TransferSupport support) {
			if (!canImport(support)) {
				return false;
			}
			final List<File> files = new ArrayList<>();
			try {
				for (Object file : (List<?>) support.getTransferable().getTransferData(DataFlavor.javaFileListFlavor)) {
					files.add((File) file);
				}
			} catch (UnsupportedFlavorException | IOException ex) {
				application.sendErrorMessage(ex);
				return false;
			}
			// Opening may show modal dialogs - do that after the drop itself has completed,
			// so the drag source (e.g. the Windows Explorer) is not kept waiting for them.
			EventQueue.invokeLater(() -> {
				boolean add = false;
				for (File file : files) {
					if (!openFile(file, FileType.ANY_FILE, add) && !add) {
						return; // Without the first file, there is nothing to add the others to.
					}
					add = true;
				}
			});
			return true;
		}
	}

	/**
	 * Gives the workspace the system equates shipped for its computer system
	 * (see {@link WorkspaceLogic#loadSystemEquates}) unless it already has
	 * some. Called wherever the list can have become empty or stale: at
	 * startup, when the computer system changes, after the workspace is
	 * cleared for a new/opened file, and after a workspace file is loaded. A
	 * workspace file that carries its own system equates keeps them - that is
	 * what the "if empty" is for; one that carries none gets the shipped ones.
	 */
	private void loadSystemEquatesIfEmpty() {
		if (workspace.getSystemEquateList().isEmpty()) {
			workspaceLogic.loadSystemEquates(workspace);
		}
	}

	/** Syncs the memory inspector's segment with the workspace's current selection. */
	private void updateMemoryInspectorSegment() {
		memoryInspectorState.setSegmentIndex(workspace.getSegmentList().getSelectedIndex());
		mainWindow.memoryInspectorPanel.segmentChanged(memoryInspectorState);
	}

	/**
	 * Reacts to a {@link WorkspaceProperty#COMPUTER_SYSTEM_TYPE}/{@link
	 * WorkspaceProperty#FONT} change - every part window shares one font
	 * set, matching {@link ComputerFont}'s javadoc.
	 */
	private void updateFonts() {
		ComputerFont computerFont = ComputerFont.get(workspace.getComputerSystem().getType(), workspace.isViewDoubleHeight());
		mainWindow.memoryInspectorPanel.setComputerFont(computerFont);
		mainWindow.disassemblyPanel.setComputerFont(computerFont);
		mainWindow.xrefPanel.setComputerFont(computerFont);
		mainWindow.logPanel.setComputerFont(computerFont);
		mainWindow.segmentListPanel.setComputerFont(computerFont);
	}

	/** Enables/disables the Equates menu's Clear/Save/Export items based on whether system/user equates exist. */
	private void updateEquatesMenuState() {
		boolean hasSystemEquates = !workspace.getSystemEquateList().isEmpty();
		boolean hasUserEquates = !workspace.getUserEquateList().isEmpty();
		mainWindow.mainMenu.clearSystemEquatesMenuItem.setEnabled(hasSystemEquates);
		mainWindow.mainMenu.clearUserEquatesMenuItem.setEnabled(hasUserEquates);
		mainWindow.mainMenu.saveUserEquatesMenuItem.setEnabled(hasUserEquates);
		mainWindow.mainMenu.exportUserEquatesMenuItem.setEnabled(hasUserEquates);
	}

	/**
	 * Enables the File menu's commands, following three rules:
	 * <ul>
	 * <li>Nothing that replaces, extends or saves the workspace while the
	 * memory inspector is in edit mode - the half-typed byte belongs to the
	 * workspace as it is now. (A disabled item's accelerator is dead too.)</li>
	 * <li>An Open/Add item only if the workspace's computer system can read
	 * that file type at all - no cassette or disk images for the C64, say.</li>
	 * <li>The Save items only if there is something to save - all of them,
	 * including "Save Disassembly Files" and "Write Boot Disk" (see {@link
	 * Actions}'s own javadoc for that naming), simply follow "has
	 * segments".</li>
	 * </ul>
	 */
	private void updateFileMenuState() {
		MainMenu mainMenu = mainWindow.mainMenu;
		ComputerSystem computerSystem = workspace.getComputerSystem();
		boolean notEditing = !mainWindow.memoryInspectorPanel.isEditMode();
		boolean hasSegments = !workspace.getSegmentList().isEmpty();

		mainMenu.newWorkspaceMenuItem.setEnabled(notEditing);
		setOpenAndAddEnabled(mainMenu.openAnyFileMenuItem, mainMenu.addAnyFileMenuItem, notEditing);
		mainMenu.openWorkspaceMenuItem.setEnabled(notEditing);
		mainMenu.recentWorkspacesMenu.setEnabled(notEditing);
		mainMenu.recentFilesMenu.setEnabled(notEditing);

		setOpenAndAddEnabled(mainMenu.openCassetteImageFileMenuItem, mainMenu.addCassetteImageFileMenuItem,
				notEditing && computerSystem.isSupportedFileType(FileType.CASSETTE_IMAGE_FILE));
		setOpenAndAddEnabled(mainMenu.openDiskImageExecutableFileMenuItem, mainMenu.addDiskImageExecutableFileMenuItem,
				notEditing && computerSystem.isSupportedFileType(FileType.DISK_IMAGE_EXECUTABLE_FILE));
		setOpenAndAddEnabled(mainMenu.openDiskImageBootSectorsMenuItem, mainMenu.addDiskImageBootSectorsMenuItem,
				notEditing && computerSystem.isSupportedFileType(FileType.DISK_IMAGE_BOOT_SECTORS));
		setOpenAndAddEnabled(mainMenu.openDiskImageSectorsMenuItem, mainMenu.addDiskImageSectorsMenuItem,
				notEditing && computerSystem.isSupportedFileType(FileType.DISK_IMAGE_SECTORS));
		setOpenAndAddEnabled(mainMenu.openExecutableFileMenuItem, mainMenu.addExecutableFileMenuItem,
				notEditing && computerSystem.isSupportedFileType(FileType.EXECUTABLE_FILE));
		setOpenAndAddEnabled(mainMenu.openRawFileMenuItem, mainMenu.addRawFileMenuItem,
				notEditing && computerSystem.isSupportedFileType(FileType.RAW_FILE));
		setOpenAndAddEnabled(mainMenu.openROMImageFileMenuItem, mainMenu.addROMImageFileMenuItem,
				notEditing && computerSystem.isSupportedFileType(FileType.ROM_IMAGE_FILE));

		mainMenu.saveWorkspaceMenuItem.setEnabled(notEditing && hasSegments);
		mainMenu.saveWorkspaceAsMenuItem.setEnabled(notEditing && hasSegments);
		mainMenu.saveDisassemblyFilesMenuItem.setEnabled(notEditing && hasSegments);
		mainMenu.writeBootDiskMenuItem.setEnabled(notEditing && hasSegments);
	}

	private static void setOpenAndAddEnabled(JMenuItem openMenuItem, JMenuItem addMenuItem, boolean enabled) {
		openMenuItem.setEnabled(enabled);
		addMenuItem.setEnabled(enabled);
	}

	/** Repopulates the "Recent Workspaces"/"Recent Files" menus from {@link #mruController}. */
	private void refreshMRUMenus() {
		mruController.fillMenu(mainWindow.mainMenu.recentWorkspacesMenu, true, this::openRecentWorkspace);
		mruController.fillMenu(mainWindow.mainMenu.recentFilesMenu, false, this::openRecentFile);
	}

	/** A "Recent Workspaces" selection - routed through {@link #openFile} like every other way of opening a file. */
	private void openRecentWorkspace(MRUEntry entry) {
		openFile(new File(entry.getFilePath()), FileType.WORKSPACE_FILE, false);
	}

	/**
	 * A "Recent Files" selection - routed through {@link #openFile}, so a
	 * raw file or disk image reopens through the same dialog (load address,
	 * file-within-the-image, sectors) the menu item that first opened it
	 * uses.
	 */
	void openRecentFile(MRUEntry entry) {
		openFile(new File(entry.getFilePath()), entry.getFileType(), false);
	}

	/**
	 * {@link #confirmClearWorkspace} plus {@code workspace.init()} together
	 * clear the workspace, then {@link WorkspaceDialog} lets the user pick
	 * the new workspace's computer system instead of always defaulting to
	 * Atari 800.
	 */
	private void performNewWorkspace() {
		if (!confirmClearWorkspace()) {
			return;
		}
		workspace.init();
		currentFile = null;

		if (new WorkspaceDialog(mainWindow.getFrame()).show(workspace)) {
			application.sendMessage(Messages.I016, workspace.getComputerSystem().getType().getText());
		}
		loadSystemEquatesIfEmpty(); // Already done by the workspace listener if the dialog changed the computer system.

		mainWindow.segmentListPanel.refresh();
		mainWindow.disassemblyPanel.refresh(null);
		updateTitle();
	}

	/**
	 * The File menu's Open/Add items: lets the user pick a file, then hands
	 * it to {@link #openFile} - the single entry point every way of opening
	 * a file shares (menu, Recent Files/Workspaces, command line, drag and
	 * drop).
	 */
	void performOpenFile(FileType fileType, boolean add) {
		String title = getFileTypeOpenTitle(fileType, add);
		File file = fileType == FileType.ANY_FILE
				? fileChoosers.chooseOpenAnyFile(mainWindow.getFrame(), title, workspace.getComputerSystem())
				: fileChoosers.chooseOpenFile(mainWindow.getFrame(), title, fileType);
		if (file != null) {
			openFile(file, fileType, add);
		}
	}

	/**
	 * Opens (replacing the workspace's content, after {@link
	 * #confirmClearWorkspace}) or adds a file of the given type. {@link
	 * FileType#ANY_FILE} - used by the command line and by drag and drop,
	 * which only have a path - is resolved via {@link
	 * ComputerSystem#guessFileType(File)} for the workspace's current
	 * computer system, then by the {@code .wrk} extension. A file that
	 * cannot be classified is offered as a raw file: {@link RawFileDialog}
	 * lets the user say where it belongs in memory, and every system
	 * supports that. Returns whether the file was actually opened/added.
	 */
	boolean openFile(File file, FileType fileType, boolean add) {
		// The menu commands are disabled while editing, but a dropped file gets here regardless.
		mainWindow.memoryInspectorPanel.quitEditMode();

		if (fileType == FileType.ANY_FILE) {
			if (file.getName().toLowerCase().endsWith(".wrk")) {
				fileType = FileType.WORKSPACE_FILE;
			} else {
				try {
					fileType = workspace.getComputerSystem().guessFileType(file);
				} catch (IOException ex) {
					application.sendErrorMessage(ex);
					return false;
				}
				if (fileType == FileType.ANY_FILE) {
					application.sendMessage(Messages.I073, file.getPath(), workspace.getComputerSystem().getType().getText());
					fileType = FileType.RAW_FILE;
				}
			}
		}
		if (fileType == FileType.WORKSPACE_FILE) {
			add = false; // A workspace always replaces the current one.
		}
		if (!add && !confirmClearWorkspace()) {
			return false;
		}

		// If chains, not switches, here and below: FileType is a ValueSet, not an enum.
		boolean opened;
		if (fileType == FileType.WORKSPACE_FILE) {
			opened = openWorkspaceFile(file);
		} else if (fileType == FileType.EXECUTABLE_FILE || fileType == FileType.ROM_IMAGE_FILE
				|| fileType == FileType.CASSETTE_IMAGE_FILE) {
			opened = openReadableFile(file, fileType, add);
		} else if (fileType == FileType.RAW_FILE) {
			opened = openRawFile(file, add);
		} else if (fileType == FileType.DISK_IMAGE_EXECUTABLE_FILE) {
			opened = openDiskImageExecutableFile(file, add);
		} else if (fileType == FileType.DISK_IMAGE_BOOT_SECTORS) {
			opened = openDiskImageBootSectors(file, add);
		} else if (fileType == FileType.DISK_IMAGE_SECTORS) {
			opened = openDiskImageSectors(file, add);
		} else {
			throw new IllegalArgumentException("Parameter 'fileType' has unsupported value " + fileType.getKey() + ".");
		}
		if (!opened) {
			return false;
		}

		if (fileType == FileType.WORKSPACE_FILE) {
			currentFile = file;
		}
		mruController.addFile(file.getPath(), fileType);
		mruController.save();
		refreshMRUMenus();
		mainWindow.segmentListPanel.refresh();
		updateDisassembly(true);
		updateTitle();
		return true;
	}

	/** Every {@code openXxx} method's "replace, don't add" step, done as late as possible so a cancelled dialog or unreadable file leaves the workspace alone. */
	private void clearWorkspaceUnlessAdding(boolean add) {
		if (!add) {
			workspace.init();
			loadSystemEquatesIfEmpty();
			currentFile = null;
		}
	}

	private boolean openWorkspaceFile(File file) {
		if (!workspaceLogic.load(workspace, file.getPath())) {
			JOptionPane.showMessageDialog(mainWindow.getFrame(), Messages.E038.format(file.getPath()),
					Texts.Dis6502_OpenWorkspaceTitle, JOptionPane.ERROR_MESSAGE);
			return false;
		}
		loadSystemEquatesIfEmpty();
		return true;
	}

	/**
	 * The file types {@link ComputerSystem#readFile} handles without a
	 * dedicated selection dialog of its own - {@link
	 * FileType#EXECUTABLE_FILE}, {@link FileType#ROM_IMAGE_FILE}, {@link
	 * FileType#CASSETTE_IMAGE_FILE}.
	 */
	private boolean openReadableFile(File file, FileType fileType, boolean add) {
		application.sendMessage(getFileTypeOpenMessage(fileType), file.getPath());

		clearWorkspaceUnlessAdding(add);

		if (!workspaceLogic.addFile(workspace, fileType, file.getPath())) {
			JOptionPane.showMessageDialog(mainWindow.getFrame(),
					(add ? Messages.E040 : Messages.E049).format(file.getPath()),
					getFileTypeOpenTitle(fileType, add), JOptionPane.ERROR_MESSAGE);
			return false;
		}
		return true;
	}

	/**
	 * A raw (headerless) file, via {@link RawFileDialog} for picking the
	 * byte range and load address. Unlike {@link #openReadableFile}, which
	 * routes through {@code WorkspaceLogic.addFile}/{@code
	 * ComputerSystem.readFile}, this calls {@code
	 * WorkspaceLogic.addRawSegment} directly, since a raw file has no
	 * format for a {@link ComputerSystem} to parse.
	 */
	private boolean openRawFile(File file, boolean add) {
		application.sendMessage(Messages.I022, file.getPath());

		RawFileDialog dialog = new RawFileDialog(mainWindow.getFrame());
		boolean confirmed;
		try {
			confirmed = dialog.show(file);
		} catch (IOException ex) {
			application.sendErrorMessage(ex);
			return false;
		}
		if (!confirmed) {
			return false;
		}

		clearWorkspaceUnlessAdding(add);

		workspaceLogic.addRawSegment(workspace, dialog.getFileBuffer(), dialog.getBegin(), dialog.getResultSize(), dialog.getAddress());
		return true;
	}

	/**
	 * An executable file picked from within an Atari DOS 2.x disk image, via
	 * {@link DiskImageExecutableFileDialog}. The picked file's bytes are
	 * read directly through {@link AtariDisk#readFile(String)} and fed to
	 * {@code WorkspaceLogic.addFile} the same way {@link #openReadableFile}
	 * feeds it a real file's {@link InputStream} - {@link
	 * AtariDisk#readFile(String)} returns the whole file as a {@code
	 * byte[]}, so a plain {@link ByteArrayInputStream} is enough.
	 */
	private boolean openDiskImageExecutableFile(File file, boolean add) {
		AtariDisk atariDisk = new AtariDisk(file.getPath());
		AtariFile info = new AtariFile();
		AtariError error;
		try {
			error = atariDisk.findFirst(info);
		} catch (IOException ex) {
			application.sendErrorMessage(ex);
			return false;
		}
		// Log-only (no dialog). AtariDisk#findFirst doesn't surface OS-level
		// error code/message details (it only distinguishes DISK_NOT_FOUND via
		// a caught FileNotFoundException, see AtariDOS's own class javadoc),
		// so the DISK_NOT_FOUND case keeps its own generic message instead.
		switch (error) {
		case OK:
			break;
		case DISK_NOT_FOUND:
			application.sendMessage(Messages.E092, file.getPath(), error.getErrorText());
			return false;
		case NO_ENTRY_FOUND:
			application.sendMessage(Messages.E033, file.getPath());
			return false;
		default:
			application.sendMessage(Messages.E036);
			return false;
		}

		DiskImageExecutableFileDialog dialog = new DiskImageExecutableFileDialog(mainWindow.getFrame());
		boolean confirmed;
		try {
			confirmed = dialog.show(atariDisk);
		} catch (IOException ex) {
			application.sendErrorMessage(ex);
			return false;
		}
		if (!confirmed) {
			return false;
		}
		application.sendMessage(Messages.I019, dialog.getExecutableFileName(), file.getPath());

		byte[] fileBuffer;
		try {
			fileBuffer = atariDisk.readFile(dialog.getExecutableFileName());
		} catch (IOException ex) {
			application.sendErrorMessage(ex);
			return false;
		}
		if (fileBuffer.length == 0) {
			JOptionPane.showMessageDialog(mainWindow.getFrame(), Messages.E041.format(),
					Texts.DiskImageExecutableFileDialog_Title, JOptionPane.ERROR_MESSAGE);
			return false;
		}

		clearWorkspaceUnlessAdding(add);

		boolean success;
		try (InputStream inputStream = new ByteArrayInputStream(fileBuffer)) {
			success = workspaceLogic.addFile(workspace, FileType.EXECUTABLE_FILE, inputStream, fileBuffer.length);
		} catch (IOException ex) {
			application.sendErrorMessage(ex);
			return false;
		}
		if (!success) {
			JOptionPane.showMessageDialog(mainWindow.getFrame(),
					(add ? Messages.E040 : Messages.E049).format(dialog.getExecutableFileName()),
					getFileTypeOpenTitle(FileType.DISK_IMAGE_EXECUTABLE_FILE, add), JOptionPane.ERROR_MESSAGE);
			return false;
		}
		return true;
	}

	/**
	 * A disk image's Atari DOS boot sectors as a single segment. The actual
	 * segment construction (including reading any boot sectors beyond the
	 * first) is {@link WorkspaceLogic#addDiskImageBootSectorsSegment}.
	 */
	private boolean openDiskImageBootSectors(File file, boolean add) {
		application.sendMessage(Messages.I018, file.getPath());

		ImgInfo info = new ImgInfo();
		DiskImage.getInfo(file.getPath(), info);
		if (DiskImage.displayError(application, info.result)) {
			return false;
		}

		ImgRWPacket sector = new ImgRWPacket();
		sector.filePath = file.getPath();
		sector.sectorNumber = 1;
		sector.sectorSize = 128; // Atari boot sectors are always 128 bytes long.
		DiskImage.readSector(sector);
		// Checks sector.result (the sector read that just happened), not
		// info.result (the disk-image-level result from getInfo()) again.
		if (DiskImage.displayError(application, sector.result)) {
			return false;
		}
		if ((sector.sectorData[1] & 0xFF) == 0) {
			application.sendMessage(Messages.E034, file.getPath());
			return false;
		}

		clearWorkspaceUnlessAdding(add);

		workspaceLogic.addDiskImageBootSectorsSegment(workspace, file.getPath(), sector.sectorData);
		return true;
	}

	/**
	 * One or more disk image sectors (or byte ranges within them) as
	 * segments, via {@link DiskImageSectorsDialog} - see that class's
	 * javadoc for why {@link DiskImageSectorsDialog#readSector} returns full
	 * sector data rather than an already-sliced buffer.
	 */
	private boolean openDiskImageSectors(File file, boolean add) {
		ImgInfo info = new ImgInfo();
		DiskImage.getInfo(file.getPath(), info);
		if (DiskImage.displayError(application, info.result)) {
			return false;
		}

		DiskImageSectorsDialog dialog = new DiskImageSectorsDialog(mainWindow.getFrame());
		dialog.setComputerFont(ComputerFont.get(workspace.getComputerSystem().getType(), workspace.isViewDoubleHeight()));
		if (!dialog.show(file.getPath(), info)) {
			return false;
		}
		List<DiskImageSectorsDialog.Item> items = dialog.getItems();
		if (items.isEmpty()) {
			return false;
		}
		application.sendMessage(Messages.I020, String.valueOf(items.size()), file.getPath());

		clearWorkspaceUnlessAdding(add);

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
			return false;
		}
		return true;
	}

	/** The per-{@link FileType}/add-or-open dialog title - separate, fully worded texts rather than a composed "Open " + text, which translate better. */
	private static String getFileTypeOpenTitle(FileType fileType, boolean add) {
		if (fileType == FileType.ANY_FILE) {
			return add ? Texts.Dis6502_AddAnyFileTitle : Texts.Dis6502_OpenAnyFileTitle;
		} else if (fileType == FileType.WORKSPACE_FILE) {
			return Texts.Dis6502_OpenWorkspaceFileTitle;
		} else if (fileType == FileType.EXECUTABLE_FILE) {
			return add ? Texts.Dis6502_AddExecutableFileTitle : Texts.Dis6502_OpenExecutableFileTitle;
		} else if (fileType == FileType.ROM_IMAGE_FILE) {
			return add ? Texts.Dis6502_AddRomImageFileTitle : Texts.Dis6502_OpenRomImageFileTitle;
		} else if (fileType == FileType.CASSETTE_IMAGE_FILE) {
			return add ? Texts.Dis6502_AddCassetteImageFileTitle : Texts.Dis6502_OpenCassetteImageFileTitle;
		} else if (fileType == FileType.RAW_FILE) {
			return add ? Texts.Dis6502_AddRawFileTitle : Texts.RawFileDialog_Title;
		} else if (fileType == FileType.DISK_IMAGE_EXECUTABLE_FILE) {
			return add ? Texts.Dis6502_AddDiskImageExecutableFileTitle : Texts.DiskImageExecutableFileDialog_Title;
		} else if (fileType == FileType.DISK_IMAGE_BOOT_SECTORS) {
			return add ? Texts.Dis6502_AddDiskImageBootSectorsTitle : Texts.Dis6502_OpenDiskImageBootSectorsTitle;
		} else if (fileType == FileType.DISK_IMAGE_SECTORS) {
			return add ? Texts.Dis6502_AddDiskImageSectorsTitle : Texts.DiskImageSectorsDialog_Title;
		}
		throw new IllegalArgumentException("Parameter 'fileType' has unsupported value " + fileType.getKey() + ".");
	}

	/** {@link #openReadableFile}'s per-{@link FileType} "opening file" log message. */
	private static Message getFileTypeOpenMessage(FileType fileType) {
		if (fileType == FileType.EXECUTABLE_FILE) {
			return Messages.I021;
		} else if (fileType == FileType.ROM_IMAGE_FILE) {
			return Messages.I023;
		} else if (fileType == FileType.CASSETTE_IMAGE_FILE) {
			return Messages.I017;
		}
		throw new IllegalArgumentException("Parameter 'fileType' has unsupported value " + fileType.getKey() + ".");
	}

	/** Confirms with the user, then clears {@code equateList} if there's anything to clear. */
	private void performClearEquates(EquateList equateList) {
		if (equateList.isEmpty()) {
			return;
		}
		String message = equateList.getProperty() == WorkspaceProperty.SYSTEM_EQUATES
				? Texts.Dis6502_ConfirmClearSystemEquatesMessage
				: Texts.Dis6502_ConfirmClearUserEquatesMessage;
		if (JOptionPane.showConfirmDialog(mainWindow.getFrame(), message, Texts.Dis6502_ClearEquatesTitle,
				JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
			equateList.clear();
			updateDisassembly(false); // Respects "No Disassembly" mode - skipped if that view toggle is on.
		}
	}

	/**
	 * Wraps the mutation {@link EquateDialog#show} performs on OK in {@code
	 * beginUpdate}/{@code endUpdate}, since {@link EquateList#clear()}
	 * (called first, to discard the list's old content before re-adding the
	 * edited lines) would otherwise fire an immediate "changed" notification
	 * while the list is still momentarily empty - before the re-add loop that
	 * follows it in the same method runs - matching the same "batch a
	 * multi-step mutation" pattern {@link WorkspaceLogic#load} already uses.
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
			updateDisassembly(false); // Respects "No Disassembly" mode - skipped if that view toggle is on.
		}
	}

	/** Opens {@link EquateRangeDialog} and refreshes the disassembly if it defined a range. */
	private void performDefineUserAddressRange() {
		EquateRangeDialog dialog = new EquateRangeDialog(mainWindow.getFrame());
		if (dialog.show(workspace.getSystemEquateList(), workspace.getUserEquateList(), "")) {
			updateDisassembly(false); // Respects "No Disassembly" mode - skipped if that view toggle is on.
		}
	}

	/** Lets the user pick a user-equates file to load. */
	private void performOpenUserEquates() {
		File file = fileChoosers.chooseOpenFile(mainWindow.getFrame(), Texts.Dis6502_OpenUserEquatesFileTitle, FileType.EQUATES_FILE);
		if (file == null) {
			return;
		}
		lastEquateFile = file;
		if (equateListLogic.load(workspace.getUserEquateList(), lastEquateFile.getPath())) {
			updateDisassembly(false); // Respects "No Disassembly" mode - skipped if that view toggle is on.
		}
	}

	/** Invoked for both "Save User Equates" ({@code xasm=false}) and "Export User Equates" ({@code xasm=true}). */
	private void performSaveUserEquates(boolean xasm) {
		// The exported label table is a different format: do not suggest overwriting the equates file with it.
		File file = fileChoosers.chooseSaveFile(mainWindow.getFrame(),
				xasm ? Texts.Dis6502_ExportUserEquatesFileTitle : Texts.Dis6502_SaveUserEquatesFileTitle, FileType.EQUATES_FILE,
				xasm ? null : lastEquateFile);
		if (file == null) {
			return;
		}
		if (!xasm) {
			lastEquateFile = file;
		}
		equateListLogic.save(workspace.getUserEquateList(), file.getPath(), xasm);
	}

	/** Merges adjacent, compatible segments, if there's more than one segment. */
	private void performMergeSegments() {
		if (workspace.getSegmentList().getCount() > 1) {
			int mergedCount = workspace.getSegmentList().mergeSegments();
			application.sendMessage(Messages.I015, String.valueOf(mergedCount));
		}
	}

	/**
	 * Deletes every selected segment - see {@link
	 * com.wudsn.tools.dis6502.ui.SegmentListPanel}'s own javadoc for the
	 * multi-selection this supports.
	 */
	private void performDeleteSelectedSegments() {
		workspace.getSegmentList().deleteSegments(mainWindow.segmentListPanel.getSelectedSegmentIndices());
	}

	/** Saves the selected segment, with or without an executable header. */
	private void performSaveSegment(boolean writeHeader) {
		int segmentIndex = workspace.getSegmentList().getSelectedIndex();
		if (segmentIndex < 0) {
			return;
		}
		File file = fileChoosers.chooseSaveFile(mainWindow.getFrame(), Texts.Dis6502_SaveSegmentTitle,
				writeHeader ? FileType.EXECUTABLE_FILE : FileType.RAW_FILE, null);
		if (file == null) {
			return;
		}
		try (FileOutputStream outputStream = new FileOutputStream(file)) {
			workspace.getComputerSystem().writeExecutableFile(workspace.getSegmentList(), segmentIndex, writeHeader, outputStream);
		} catch (IOException ex) {
			application.sendErrorMessage(ex);
		}
	}

	/** Saves every segment concatenated into one executable file. */
	private void performSaveAllSegments() {
		if (workspace.getSegmentList().getCount() == 0) {
			return;
		}
		File file = fileChoosers.chooseSaveFile(mainWindow.getFrame(), Texts.Dis6502_SaveAllSegmentsTitle, FileType.EXECUTABLE_FILE,
				null);
		if (file == null) {
			return;
		}
		try (FileOutputStream outputStream = new FileOutputStream(file)) {
			workspace.getComputerSystem().writeExecutableFile(workspace.getSegmentList(), SegmentList.NO_SEGMENT_INDEX, true, outputStream);
		} catch (IOException ex) {
			application.sendErrorMessage(ex);
		}
	}

	/** Opens {@link SegmentPropertiesDialog} for the selected segment. */
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

	/** Opens {@link MemoryInspectorFindStringDialog}. */
	private void performShowMemoryInspectorFindDialog() {
		new MemoryInspectorFindStringDialog(mainWindow.getFrame()).show(mainWindow.memoryInspectorPanel);
	}

	/**
	 * Shows the same "not found" alert {@link
	 * MemoryInspectorFindStringDialog#performOK} shows, since {@link
	 * com.wudsn.tools.dis6502.ui.MemoryInspectorPanel#findNextString} stays
	 * free of popups (see that class's javadoc).
	 */
	private void performMemoryInspectorFindNext() {
		if (!mainWindow.memoryInspectorPanel.findNextString()) {
			JOptionPane.showMessageDialog(mainWindow.getFrame(),
					TextUtility.format(Texts.MemoryInspectorFindStringDialog_StringNotFoundMessage,
							mainWindow.memoryInspectorPanel.getFindString()),
					Texts.MemoryInspectorFindStringDialog_NotFoundTitle, JOptionPane.INFORMATION_MESSAGE);
		}
	}

	/** Splits the selected segment into two at the current byte selection's start. */
	private void performSplitAtSelection() {
		if (memoryInspectorState.isSelectionEmpty()) {
			return;
		}
		workspace.getSegmentList().splitSelectedSegment(memoryInspectorState.getBegin());
	}

	/**
	 * Writes the memory inspector's current byte selection to a file,
	 * optionally prefixed with a plain Atari executable header ($FFFF, begin
	 * address, end address).
	 */
	private void performSaveMemoryInspectorSelection(boolean withHeader) {
		File file = fileChoosers.chooseSaveFile(mainWindow.getFrame(),
				withHeader ? Texts.Dis6502_SaveSelectionWithHeaderTitle : Texts.Dis6502_SaveSelectionNoHeaderTitle,
				withHeader ? FileType.EXECUTABLE_FILE : FileType.RAW_FILE, null);
		if (file == null) {
			return;
		}
		try (FileOutputStream outputStream = new FileOutputStream(file)) {
			if (withHeader) {
				int[] addressRange = memoryInspectorState.getAddressRange();
				writeWordLE(outputStream, FileHeader.ATARI_BINARY.getValue());
				writeWordLE(outputStream, addressRange[0]);
				writeWordLE(outputStream, addressRange[1]);
			}
			outputStream.write(memoryInspectorState.getByteSequence());
		} catch (IOException ex) {
			application.sendErrorMessage(ex);
		}
	}

	private static void writeWordLE(FileOutputStream outputStream, int value) throws IOException {
		outputStream.write(value & 0xFF);
		outputStream.write((value >> 8) & 0xFF);
	}

	/**
	 * Routes the LOBYTE/HIBYTE case to {@link
	 * #performSetMemoryInspectorLoHiType}, since it needs {@link
	 * LowHighByteDialog} and stricter validation; every other type goes
	 * straight through {@link
	 * com.wudsn.tools.dis6502.ui.MemoryInspectorPanel#setType}. Wired
	 * directly to the popup menu's Change Type submenu via {@code
	 * setTypeSelectionListener}, since each submenu item already knows its
	 * own type.
	 */
	private void performSetMemoryInspectorType(MemoryType type) {
		if (type == MemoryType.LOBYTE || type == MemoryType.HIBYTE) {
			performSetMemoryInspectorLoHiType(type);
			return;
		}
		mainWindow.memoryInspectorPanel.setType(type);
		updateDisassembly(false);
	}

	/**
	 * The marked byte must be alone at the start of an immediate-mode
	 * instruction's operand, one byte after the opcode - {@link
	 * LowHighByteDialog} then asks for the other, unknown half of the
	 * "assumed word", whose raw value is stored directly in the operand
	 * byte's type slot (see {@link MemoryType}'s javadoc on why {@code
	 * segment.memoryBlock.getType()} is written directly here instead of
	 * through {@link Segment#setType}, which only accepts a real {@link
	 * MemoryType} constant).
	 */
	private void performSetMemoryInspectorLoHiType(MemoryType type) {
		if (memoryInspectorState.isSelectionEmpty()) {
			return;
		}
		Segment segment = memoryInspectorState.getSegment();
		int begin = memoryInspectorState.getBegin();
		int size = memoryInspectorState.getSize();

		if (begin == 0) {
			JOptionPane.showMessageDialog(mainWindow.getFrame(), Messages.E031.format(), Texts.Dis6502_SetTypeTitle, JOptionPane.ERROR_MESSAGE);
			return;
		}
		if (size != 1) {
			JOptionPane.showMessageDialog(mainWindow.getFrame(), Messages.E032.format(), Texts.Dis6502_SetTypeTitle, JOptionPane.ERROR_MESSAGE);
			return;
		}

		int previousOpcode = segment.getData(begin - 1);
		InstructionSet instructionSet = workspace.getInstructionSet(segment.processorType);
		if (instructionSet.getInstruction(previousOpcode).getOperandMode() != OperandMode.Immediate) {
			JOptionPane.showMessageDialog(mainWindow.getFrame(), Messages.E025.format(), Texts.Dis6502_SetTypeTitle, JOptionPane.ERROR_MESSAGE);
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

	/** Reclassifies every still-unknown byte in the selection as {@link MemoryType#BYTE}. */
	private void performSetUnknownBlockToByte() {
		mainWindow.memoryInspectorPanel.setUnknownBlockToByte();
		updateDisassembly(false);
	}

	/**
	 * Copies the memory inspector's current byte selection to the system
	 * clipboard as a plain uppercase hex string with no separators or
	 * prefix. Uses {@link java.awt.datatransfer.Clipboard} directly here
	 * rather than through {@link UIApplication}.
	 */
	private void performCopyMemoryInspectorSelection() {
		if (memoryInspectorState.isSelectionEmpty()) {
			return;
		}
		StringBuilder hex = new StringBuilder();
		for (byte value : memoryInspectorState.getByteSequence()) {
			hex.append(String.format("%02X", value & 0xFF));
		}
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(hex.toString()), null);
	}

	/**
	 * Cut: {@link #performCopyMemoryInspectorSelection} followed by {@link
	 * #performDeleteMemoryInspectorSelection}, composed here at the call
	 * site. See {@code com.wudsn.tools.dis6502.ui.MemoryInspectorPanel}'s
	 * class javadoc for the Delete/Cut/Paste Selection design.
	 */
	private void performCutMemoryInspectorSelection() {
		if (memoryInspectorState.isSelectionEmpty()) {
			return;
		}
		performCopyMemoryInspectorSelection();
		performDeleteMemoryInspectorSelection();
	}

	/**
	 * Delete: removes the current byte selection from its segment via
	 * {@link Segment#deleteRange}, shrinking it in place. If that empties
	 * the segment entirely, it is removed from the workspace's {@link
	 * SegmentList} via {@link SegmentList#deleteSelectedSegment()}; see
	 * {@code MemoryInspectorPanel}'s class javadoc.
	 */
	private void performDeleteMemoryInspectorSelection() {
		if (memoryInspectorState.isSelectionEmpty()) {
			return;
		}
		Segment segment = memoryInspectorState.getSegment();
		int begin = memoryInspectorState.getBegin();
		int size = memoryInspectorState.getSize();

		segment.deleteRange(begin, size);

		if (segment.isEmpty()) {
			workspace.getSegmentList().deleteSelectedSegment();
		} else {
			memoryInspectorState.clearSelection();
			mainWindow.memoryInspectorPanel.segmentChanged(memoryInspectorState);
			updateDisassembly(false);
		}
	}

	/**
	 * Paste: inserts the system clipboard's content - the same plain
	 * uppercase hex string {@link #performCopyMemoryInspectorSelection}
	 * produces - before the current byte selection's first byte, via {@link
	 * Segment#insertRange}. Insert-only, not replace-selection, and a
	 * single command, not a "before"/"after" pair; see {@code
	 * MemoryInspectorPanel}'s class javadoc.
	 */
	private void performPasteMemoryInspectorSelection() {
		if (memoryInspectorState.isSelectionEmpty()) {
			return;
		}
		String clipboardText;
		try {
			clipboardText = (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
		} catch (UnsupportedFlavorException | IOException ex) {
			clipboardText = null;
		}
		byte[] bytes = clipboardText == null ? null : decodeHexString(clipboardText);
		if (bytes == null || bytes.length == 0) {
			JOptionPane.showMessageDialog(mainWindow.getFrame(), Messages.E071.format(), Texts.Dis6502_PasteSelectionTitle,
					JOptionPane.ERROR_MESSAGE);
			return;
		}

		Segment segment = memoryInspectorState.getSegment();
		if (!segment.canInsertRange(bytes.length)) {
			JOptionPane.showMessageDialog(mainWindow.getFrame(), Messages.E072.format(String.valueOf(bytes.length)),
					Texts.Dis6502_PasteSelectionTitle, JOptionPane.ERROR_MESSAGE);
			return;
		}

		segment.insertRange(memoryInspectorState.getBegin(), bytes);

		memoryInspectorState.clearSelection();
		mainWindow.memoryInspectorPanel.segmentChanged(memoryInspectorState);
		updateDisassembly(false);
	}

	/**
	 * Decodes a plain uppercase hex string with no separators/prefix back
	 * into bytes - the inverse of {@link #performCopyMemoryInspectorSelection}'s
	 * hand-written encode loop. There is no shared {@code
	 * com.wudsn.tools.base.common.HexUtility} decode method to reuse (only
	 * the encode direction exists there). Returns {@code null} if {@code
	 * text} (after trimming) is not an even-length string of hex digits.
	 */
	private static byte[] decodeHexString(String text) {
		String trimmed = text.trim();
		if (trimmed.isEmpty() || trimmed.length() % 2 != 0) {
			return null;
		}
		byte[] result = new byte[trimmed.length() / 2];
		for (int i = 0; i < result.length; i++) {
			int high = Character.digit(trimmed.charAt(i * 2), 16);
			int low = Character.digit(trimmed.charAt(i * 2 + 1), 16);
			if (high < 0 || low < 0) {
				return null;
			}
			result[i] = (byte) ((high << 4) | low);
		}
		return result;
	}

	/**
	 * Simplified to always use the memory inspector's own byte selection -
	 * see {@link CommentDialog}'s javadoc for the other trigger path (a
	 * right-clicked disassembly line, using the clicked line's own
	 * offset/size directly) - see {@link #performEditDisassemblyComment}.
	 */
	private void performEditMemoryInspectorComment() {
		if (memoryInspectorState.isSelectionEmpty()) {
			return;
		}
		CommentDialog dialog = new CommentDialog(mainWindow.getFrame());
		if (dialog.show(workspace.getSegmentList(), memoryInspectorState.getSegmentIndex(), memoryInspectorState.getBegin(),
				memoryInspectorState.getSize())) {
			updateDisassembly(false);
		}
	}

	/**
	 * Triggered by {@link DisassemblyPanel}'s right-click popup menu, using
	 * the right-clicked line's own segment/offset/size directly.
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

	/**
	 * {@code originLine} is the line Navigate Back to Previous Position
	 * should return to - the right-clicked line for the popup item, {@code
	 * null} (meaning the selected line) for Return/double-click; see {@link
	 * DisassemblyPanel#navigateToDefinitionLine}.
	 */
	private void performFindDisassemblyLabelDefinition(String label, DisassemblyLine originLine) {
		if (label.isEmpty()) {
			return;
		}
		int lineNumber = workspace.getDisassemblyResult().findDefinitionLineNumber(label);
		if (lineNumber != 0) {
			mainWindow.disassemblyPanel.navigateToDefinitionLine(lineNumber, originLine);
		}
	}

	/** What the disassembly popup's "Change type of immediate byte to" submenu shows for a line. */
	DisassemblyPanel.ImmediateType getDisassemblyImmediateType(DisassemblyLine line) {
		int[] immediateValue = new int[1];
		MemoryType[] immediateMemoryType = new MemoryType[1];
		if (!Disassembly.isInstructionWithImmediate(workspace, line.segmentIndex, line.offset, immediateValue,
				immediateMemoryType)) {
			return null;
		}
		boolean charAllowed = new DisassemblyWriter(new Disassembly(), workspace).isByteAllowedInString(immediateValue[0]);
		return new DisassemblyPanel.ImmediateType(immediateMemoryType[0], charAllowed);
	}

	/**
	 * {@link LowHighByteDialog} asks for the other half of the address for a
	 * low/high byte; the change itself is {@link
	 * Disassembly#setImmediateType}.
	 */
	void performSetDisassemblyImmediateType(DisassemblyLine line, MemoryType type) {
		int unknownByte = 0;
		if (type == MemoryType.LOBYTE || type == MemoryType.HIBYTE) {
			Segment segment = workspace.getSegmentList().getSegment(line.segmentIndex);
			LowHighByteDialog dialog = new LowHighByteDialog(mainWindow.getFrame());
			if (!dialog.show(type, segment.getData(line.offset + 1))) {
				return;
			}
			unknownByte = dialog.getUnknownByte();
		}
		if (Disassembly.setImmediateType(workspace, line.segmentIndex, line.offset, type, unknownByte)) {
			mainWindow.memoryInspectorPanel.segmentChanged(memoryInspectorState);
			updateDisassembly(false);
		}
	}

	/**
	 * Reached via {@link #performDisassemblyLineSelected} on every plain
	 * line click/drag - the same find-and-populate-XRef shape as {@link
	 * #performFindInDisassembly}, but keyed to a label instead of the find
	 * field's text, and using a fresh, local line-number holder so it does
	 * not disturb the ongoing Find/Find Next search state. This
	 * deliberately never navigates: navigating to a label's definition is
	 * a distinct, only explicitly user-requested action (Return or a
	 * double-click; see {@link
	 * com.wudsn.tools.dis6502.ui.DisassemblyPanel#setNavigateToDefinitionListener}),
	 * not this method. An earlier version of this port called {@link
	 * com.wudsn.tools.dis6502.ui.DisassemblyPanel#navigateToLine} here too,
	 * which wrongly jumped the listing away from whatever line the user had
	 * just clicked, every time that line happened to reference a label.
	 */
	private void performFindDisassemblyReferences(String label) {
		if (label.isEmpty()) {
			return;
		}
		DisassemblyResult disassemblyResult = workspace.getDisassemblyResult();
		int[] firstLineNumber = { 0 };
		disassemblyResult.findAndSelectLines(true, firstLineNumber, label);

		List<XRefPanel.Entry> entries = new ArrayList<>();
		for (DisassemblyResult.LineIterator i = disassemblyResult.createLineIterator(); i.hasNext();) {
			DisassemblyLine line = i.next();
			if (line.xrefLineNumber != 0) {
				entries.add(new XRefPanel.Entry(line.xrefLineNumber, line.getLine()));
			}
		}
		mainWindow.xrefPanel.updateList(label, entries);
	}

	/** Opens {@link EquateDialog} pre-filled for the equate {@code label} refers to. */
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

	/** Opens {@link EquateRangeDialog} pre-filled for the equate {@code label} refers to. */
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
	 * Unlike every other memory inspector action wired in this class, this
	 * calls {@link #updateDisassembly} unconditionally once the dialog
	 * closes, without checking whether anything was actually assembled.
	 */
	private void performShowAssembleDialog() {
		if (memoryInspectorState.isSelectionEmpty()) {
			return;
		}
		new AssembleDialog(mainWindow.getFrame()).show(workspace, memoryInspectorState.getSegment(), memoryInspectorState,
				mainWindow.memoryInspectorPanel);
		updateDisassembly(false);
	}

	/** Runs code-tracing on the memory inspector's current selection. */
	private void performGuessCode() {
		mainWindow.memoryInspectorPanel.guess();
		updateDisassembly(false);
	}

	/**
	 * Fired whenever the Memory Inspector's edit mode ends for any reason
	 * (Esc, the ad hoc Quit Edit Mode popup item, switching
	 * segment/segment list/XRef selection, clearing the workspace, or
	 * typing past the end of the buffer) - fires, and therefore always
	 * refreshes the disassembly, on every exit path uniformly, including
	 * typing off the end of the buffer.
	 */
	private void performMemoryInspectorEditModeExited() {
		updateFileMenuState();
		updateDisassembly(false);
	}

	/**
	 * Non-mutating, unlike its neighbors above - just ends in the same
	 * {@link com.wudsn.tools.dis6502.ui.MemoryInspectorPanel#select} every
	 * other Select* action uses, so no {@link #updateDisassembly} call
	 * follows.
	 */
	private void performShowSelectGraphicsDialog() {
		if (!memoryInspectorState.hasSegment()) {
			return;
		}
		SelectGraphicsDialog dialog = new SelectGraphicsDialog(mainWindow.getFrame());
		if (dialog.show(memoryInspectorState)) {
			mainWindow.memoryInspectorPanel.select(dialog.getBegin(), dialog.getEnd());
		}
	}

	/**
	 * Switches the Memory Inspector's ASCII column between plain byte values
	 * and their Atari internal (ANTIC screen code) equivalent.
	 */
	private void performToggleDisplayAsScreenCode() {
		workspace.setViewDisplayAsScreenCode(mainWindow.memoryInspectorPanel.displayAsScreenCodeButton.isSelected());
		mainWindow.memoryInspectorPanel.setDisplayAsScreenCode(workspace.isViewDisplayAsScreenCode());
	}

	/** Toggles the "No Disassembly" view mode. */
	private void performToggleViewDisassembly() {
		workspace.setViewNoDisassembly(mainWindow.mainMenu.noDisassemblyMenuItem.isSelected());
		updateDisassembly(false); // No-op if "No Disassembly" is now on.
	}

	/**
	 * {@link #updateFonts} reacts to this, switching both {@link
	 * com.wudsn.tools.dis6502.ui.MemoryInspectorPanel} and {@link
	 * com.wudsn.tools.dis6502.ui.DisassemblyPanel} between {@link
	 * ComputerFont#get}'s normal/double-height glyph atlases.
	 */
	private void performToggleViewDoubleFontHeight() {
		workspace.setViewDoubleHeight(mainWindow.mainMenu.doubleFontHeightMenuItem.isSelected());
		workspace.notifyFontChanged();
	}

	/**
	 * What is edited here is where {@link FileChoosers} starts when it
	 * knows no better place - see its javadoc.
	 */
	void performShowDefaultFolders() {
		DefaultFolders currentDefaultFolders = getDefaultFolders();
		if (new DefaultFoldersDialog(mainWindow.getFrame()).show(currentDefaultFolders)) {
			defaultFoldersLogic.save(currentDefaultFolders);
		}
	}

	/**
	 * The default folders of the workspace's current computer system - they
	 * are kept per system, loaded on demand: when the system has changed
	 * since the last call, the previous system's folders are saved and the
	 * new one's loaded.
	 */
	private DefaultFolders getDefaultFolders() {
		ComputerSystemType computerSystemType = workspace.getComputerSystem().getType();
		if (defaultFolders == null || defaultFolders.getComputerSystemType() != computerSystemType) {
			if (defaultFolders != null) {
				defaultFoldersLogic.save(defaultFolders);
			}
			defaultFolders = defaultFoldersLogic.createDefaultFolders(computerSystemType);
			defaultFoldersLogic.load(defaultFolders);
		}
		return defaultFolders;
	}

	/** Opens {@link ProfileDialog} and, if changed, notifies and forces a disassembly refresh. */
	private void performShowProfile() {
		ProfileDialog dialog = new ProfileDialog(mainWindow.getFrame(), profileLogic, fileChoosers);
		if (dialog.show(workspace.getProfile(), workspace.getComputerSystem().getType())) {
			workspace.notifyProfileChanged();
			updateDisassembly(true); // Forced: refreshes even while "No Disassembly" is on.
		}
	}

	/**
	 * Confirms with the user whether to discard the current workspace, saving
	 * it first if they ask, before it gets cleared/replaced - callers clear
	 * the workspace and call {@link #loadSystemEquatesIfEmpty} themselves.
	 * Returns {@code false} if the caller should abort (the user cancelled,
	 * or a requested save failed).
	 */
	private boolean confirmClearWorkspace() {
		if (workspace.getSegmentList().isEmpty()) {
			return true;
		}

		int result = JOptionPane.showConfirmDialog(mainWindow.getFrame(), Texts.Dis6502_NewWorkspaceMessage,
				Texts.WorkspaceDialog_Title, JOptionPane.YES_NO_CANCEL_OPTION);
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

	boolean performSaveWorkspaceAs() {
		File file = fileChoosers.chooseSaveFile(mainWindow.getFrame(), Texts.Dis6502_SaveWorkspaceFileAsTitle, FileType.WORKSPACE_FILE,
				currentFile);
		if (file == null) {
			return false;
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
	 * Writes the current disassembly listing (split into include files as
	 * {@link Workspace#getProfile()} dictates) via the already-complete
	 * {@link DisassemblyResultFile#saveListing}.
	 */
	private void performSaveDisassemblyFiles() {
		File file = fileChoosers.chooseSaveFile(mainWindow.getFrame(), Texts.Dis6502_SaveDisassemblyFilesTitle,
				FileType.DISASSEMBLY_FILE, null);
		if (file == null) {
			return;
		}
		try {
			new DisassemblyResultFile(application).saveListing(workspace.getDisassemblyResult(), workspace.getProfile(), file);
		} catch (IOException ex) {
			application.sendErrorMessage(ex);
		}
	}

	/**
	 * Turns the memory inspector's currently-selected segment into a bootable
	 * Atari DOS disk, via {@link SegmentWriteBootDiskDialog}. Stays a File
	 * menu action rather than a segment context-menu one.
	 */
	private void performWriteBootDisk() {
		Segment segment = memoryInspectorState.getSegment();
		if (segment == null) {
			application.sendMessage(Messages.E006);
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
	 * Uses the real {@link Disassembly} pipeline. Every call site here passes
	 * {@code force=true} except the "No Disassembly" toggle. Runs behind a
	 * {@link DisassemblyProgressDialog} - see that class's javadoc for how
	 * it runs the disassembly on a background thread.
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
		DisassemblyProgressDialog progressDialog = new DisassemblyProgressDialog(mainWindow.getFrame(), application);
		DisassemblyProgressMonitor progressMonitor = progressDialog.getMonitor();
		disassembly.setWorkspace(workspace);
		disassembly.setProgressMonitor(progressMonitor);
		try {
			progressMonitor.startDisassembly(disassembly);
		} catch (RuntimeException ex) {
			application.sendErrorMessage(ex);
		}
		// Pushes the current profile's useLineNumbers into the control on every refresh.
		mainWindow.disassemblyPanel.setLineNumbersActive(workspace.getProfile().useLineNumbers);
		mainWindow.disassemblyPanel.refresh(workspace.getDisassemblyResult());
		mainWindow.disassemblyPanel.findField.setText("");
		mainWindow.xrefPanel.updateList("", Collections.emptyList());
	}

	/**
	 * Searches the current disassembly for lines containing the {@link
	 * DisassemblyPanel#findField} text - this port uses an explicit search
	 * field rather than triggering from a label double-click, see {@link
	 * DisassemblyPanel}'s javadoc. Populates {@link XRefPanel} with every
	 * matching line and scrolls the disassembly view to the first one.
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

	/** Continues the last search from {@link #findFirstLineNumber} rather than starting over. */
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
			JOptionPane.showMessageDialog(mainWindow.getFrame(),
					TextUtility.format(Texts.MemoryInspectorFindStringDialog_StringNotFoundMessage, findString),
					Texts.MemoryInspectorFindStringDialog_NotFoundTitle, JOptionPane.INFORMATION_MESSAGE);
		}
	}

	/**
	 * Clicking the XRef entry for a label's own definition line should
	 * always navigate the disassembly view to that line, even when it has
	 * no segment - e.g. a system equate's {@code equ} line (a symbolic name
	 * for a hardware register address, not backed by any loaded segment).
	 * The segment/memory-inspector sync genuinely needs a segment, but
	 * scrolling the disassembly view to the line does not, so that guard is
	 * narrowed to just the sync below, letting every XRef entry navigate.
	 */
	private void performXRefSelected(int xrefLineNumber) {
		DisassemblyResult disassemblyResult = workspace.getDisassemblyResult();
		if (disassemblyResult == null) {
			return;
		}
		for (DisassemblyResult.LineIterator i = disassemblyResult.createLineIterator(); i.hasNext();) {
			DisassemblyLine line = i.next();
			if (line.xrefLineNumber == xrefLineNumber) {
				if (line.segmentIndex != SegmentList.NO_SEGMENT_INDEX) {
					// Selecting the segment refreshes the memory inspector's own hex
					// dump (via updateMemoryInspectorSegment, on the SELECTED_SEGMENT
					// listener) before the explicit select()/clearSelection() below.
					workspace.getSegmentList().setSelectedIndex(line.segmentIndex);
					if (line.size != 0) {
						mainWindow.memoryInspectorPanel.select(line.offset, line.offset + line.size - 1);
					} else {
						mainWindow.memoryInspectorPanel.clearSelection();
					}
				}
				mainWindow.disassemblyPanel.navigateToLine(line.getLineNumber());
				return;
			}
		}
	}

	/**
	 * Reached via {@link
	 * com.wudsn.tools.dis6502.ui.DisassemblyPanel#setLineSelectionListener} -
	 * {@code label} is already resolved to the clicked line's referenced
	 * label, or its defined one if it has no reference. Unlike {@link
	 * #performXRefSelected}, a line with no byte size (a label/equate-only
	 * line) leaves the memory inspector's selection alone rather than
	 * clearing it.
	 */
	private void performDisassemblyLineSelected(DisassemblyLine line, String label) {
		if (line.segmentIndex != SegmentList.NO_SEGMENT_INDEX && line.segmentIndex < workspace.getSegmentList().getCount()) {
			// Selecting the segment refreshes the memory inspector's own hex
			// dump (via updateMemoryInspectorSegment, on the SELECTED_SEGMENT
			// listener) before the explicit select() below.
			workspace.getSegmentList().setSelectedIndex(line.segmentIndex);
			if (line.size != 0) {
				mainWindow.memoryInspectorPanel.select(line.offset, line.offset + line.size - 1);
			}
		}

		if (!label.isEmpty()) {
			performFindDisassemblyReferences(label);
		}
	}

	/**
	 * Reached via {@link
	 * com.wudsn.tools.dis6502.ui.MemoryInspectorPanel#setSelectionChangedListener}
	 * after a mouse-driven memory inspector selection: not every byte
	 * offset starts its own disassembly line (mid-instruction bytes don't),
	 * so this walks backward from the selection's first byte, one offset
	 * at a time, until {@link DisassemblyResult#selectLine(int, int)} finds
	 * a line that actually starts there, then navigates to it. Highlighting
	 * every disassembly line up to the selection's end offset is not
	 * implemented, since {@code DisassemblyGridPanel} only ever tracks a
	 * single highlighted line, not a range (see that class's javadoc).
	 */
	private void performMemoryInspectorSelectionChanged() {
		if (memoryInspectorState.isSelectionEmpty()) {
			return;
		}
		DisassemblyResult disassemblyResult = workspace.getDisassemblyResult();
		if (disassemblyResult == null) {
			return;
		}
		int segmentIndex = memoryInspectorState.getSegmentIndex();
		int lineNumber = 0;
		for (int offset = memoryInspectorState.getBegin(); offset >= 0 && lineNumber == 0; offset--) {
			lineNumber = disassemblyResult.selectLine(segmentIndex, offset);
		}
		if (lineNumber != 0) {
			mainWindow.disassemblyPanel.navigateToLine(lineNumber);
		}
	}

	/** Saves any loaded {@link DefaultFolders} before exiting (the MRU lists are already saved incrementally, see {@link #mruController}). */
	private void performExit() {
		if (defaultFolders != null) {
			defaultFoldersLogic.save(defaultFolders);
		}
		System.exit(0);
	}

	/** Opens {@link AboutDialog} - see that class's own javadoc for details. */
	private void performAbout() {
		AboutDialog dialog = new AboutDialog(mainWindow.getFrame());
		dialog.setVisible(true);
	}

	/** Sets the main window's title (and the segment list's file name) from the current file/computer system. */
	private void updateTitle() {
		String computerSystemText = workspace.getComputerSystem().getType().getText();
		String title = currentFile == null ? TextUtility.format(Texts.Dis6502_WindowTitleNoWorkspaceLoaded, computerSystemText)
				: TextUtility.format(Texts.Dis6502_WindowTitle, computerSystemText, currentFile.getPath());
		mainWindow.getFrame().setTitle(title);
		mainWindow.segmentListPanel.setFileName(currentFile == null ? null : currentFile.getName());
	}
}
