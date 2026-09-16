/*
 * Copyright (C) 2025 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502;

import java.awt.EventQueue;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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
import com.wudsn.tools.dis6502.model.DisassemblyProgressMonitor;
import com.wudsn.tools.dis6502.model.DiskImage;
import com.wudsn.tools.dis6502.model.EquateList;
import com.wudsn.tools.dis6502.model.EquateListLogic;
import com.wudsn.tools.dis6502.model.FileType;
import com.wudsn.tools.dis6502.model.ImgInfo;
import com.wudsn.tools.dis6502.model.ImgRWPacket;
import com.wudsn.tools.dis6502.model.MRUEntry;
import com.wudsn.tools.dis6502.model.ProfileLogic;
import com.wudsn.tools.dis6502.model.Segment;
import com.wudsn.tools.dis6502.model.SegmentListInserter;
import com.wudsn.tools.dis6502.model.Workspace;
import com.wudsn.tools.dis6502.model.WorkspaceLogic;
import com.wudsn.tools.dis6502.model.WorkspaceProperty;
import com.wudsn.tools.dis6502.ui.DefaultFoldersDialog;
import com.wudsn.tools.dis6502.ui.DiskImageExecutableFileDialog;
import com.wudsn.tools.dis6502.ui.DiskImageSectorsDialog;
import com.wudsn.tools.dis6502.ui.EquateDialog;
import com.wudsn.tools.dis6502.ui.EquateRangeDialog;
import com.wudsn.tools.dis6502.ui.MainWindow;
import com.wudsn.tools.dis6502.ui.MRUController;
import com.wudsn.tools.dis6502.ui.ProfileDialog;
import com.wudsn.tools.dis6502.ui.RawFileDialog;
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
 * opening/adding an executable, ROM image, cassette image, raw, disk image
 * executable, disk image boot sectors, or disk image sectors file (see
 * {@link RawFileDialog}/{@link DiskImageExecutableFileDialog}/{@link
 * #performOpenDiskImageBootSectors}/{@link DiskImageSectorsDialog}),
 * loading/saving/clearing/exporting/editing equates and defining a user
 * equate address range (see {@link EquateDialog}/{@link
 * EquateRangeDialog}), the View menu's No Disassembly/Double Font Height
 * toggles and Default Folders/Profile dialogs (see {@link
 * DefaultFoldersDialog}/{@link ProfileDialog}), and Help &gt; About.
 * {@link #confirmClearWorkspace} mirrors {@code Main::PromptToClearWorkspace};
 * {@link #updateDisassembly} mirrors {@code Main::UpdateDisassembly},
 * called explicitly after each action instead of through the reactive
 * {@code Main::HandleWorkspaceChanged} dispatcher, which is not ported.
 * The memory inspector and cross-reference view are not wired up yet -
 * see the individual {@code ui} panel classes for what is and isn't
 * ported so far.
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
		profileLogic = new ProfileLogic(application);
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
