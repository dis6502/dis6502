/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502;

import java.awt.Component;
import java.awt.Container;
import java.awt.EventQueue;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.UIManager;

import com.wudsn.tools.dis6502.model.Assert;
import com.wudsn.tools.dis6502.model.DisassemblyLine;
import com.wudsn.tools.dis6502.model.DisassemblyResult;
import com.wudsn.tools.dis6502.model.DisassemblySectionType;
import com.wudsn.tools.dis6502.model.FileType;
import com.wudsn.tools.dis6502.model.MemoryType;
import com.wudsn.tools.dis6502.model.Segment;
import com.wudsn.tools.dis6502.model.SegmentList;
import com.wudsn.tools.dis6502.model.Workspace;
import com.wudsn.tools.dis6502.model.system.ComputerSystemType;
import com.wudsn.tools.dis6502.ui.DisassemblyPanel;
import com.wudsn.tools.dis6502.ui.MainMenu;
import com.wudsn.tools.dis6502.ui.MainWindow;
import com.wudsn.tools.dis6502.ui.MemoryInspectorPanel;
import com.wudsn.tools.dis6502.ui.UITest;

/**
 * The third test of {@code plans/UI_SMOKE_TESTS_PROPOSAL.md}: the
 * application as the user gets it - started through {@link
 * Dis6502#main}, its real menu items clicked, its real dialogs answered -
 * with the workspace, the menus and the dialogs asserted on afterwards.
 * This is where the wiring between model and UI lives, and where several
 * real defects sat (the computer system reset on open, code trace
 * ignoring C64 segments, menu items enabled during an edit); the headless
 * tests could not see any of them.
 * <p>
 * The scenarios are the former throw-away smoke programs, one method each.
 * The application is started once (it is a singleton) and the workspace
 * cleared between scenarios; its settings go to the test node {@code
 * TestRunner} sets up and removes. A dialog a scenario provokes is found
 * in {@link Window#getWindows()} and answered through its real buttons.
 * Needs a display, like every {@code ui} test - see {@link
 * UITest#isHeadless()}; expect a few seconds, the fixtures are real
 * programs.
 *
 * @author Peter Dell
 */
public final class UIWiringTest {

	private static final String RES = "test-resources/";
	private static final File AUTORUN_XEX = new File(RES + "disassembly/unit001/in/autorun.xex");
	private static final File MULTISEGMENT_XEX = new File(RES + "disassembly/unit002/in/multisegment.xex");
	private static final File AUTORUN_WRK = new File(RES + "disassembly/unit003/in/autorun.wrk");
	private static final File HELLOWORLD_PRG = new File(RES + "system/c64/HelloWorld.prg");

	private static Dis6502 app;
	private static Workspace workspace;
	private static MainWindow mainWindow;
	private static MainMenu menu;

	private UIWiringTest() {
	}

	public static void testUIWiring() throws Exception {
		if (UITest.isHeadless()) {
			Assert.log("UIWiringTest skipped: no display");
			return;
		}
		start();
		try {
			testOpenKeepsComputerSystem();
			testSystemEquatesFollowTheSystem();
			testOneDispatcherForEveryWayOfOpening();
			testFileMenuGating();
			testNavigateBackAndImmediateType();
			testCutCopyPasteDelete();
			testOpenAnyFile();
			testChooserFolderAndFilter();
		} finally {
			edt(() -> mainWindow.getFrame().dispose());
		}
		Assert.log("UIWiringTest completed");
	}

	// ------------------------------------------------------------------
	// The scenarios.
	// ------------------------------------------------------------------

	/** From KeepSystemSmoke: opening a file must not reset the workspace's computer system (it did, to ATARI800). */
	private static void testOpenKeepsComputerSystem() throws Exception {
		reset();
		edt(() -> workspace.setComputerSystemTypeID("C64"));
		edt(() -> Assert.boolEquals(app.openFile(HELLOWORLD_PRG, FileType.EXECUTABLE_FILE, false), true));
		Assert.boolEquals(workspace.getComputerSystem().getType() == ComputerSystemType.C64, true);
		Assert.longEquals(workspace.getSegmentList().getCount(), 1);
		Assert.longEquals(workspace.getSegmentList().getSegment(0).wBegin, 0x0801);
	}

	/** From SystemEquatesSmoke: the shipped system equates follow the computer system, and get referenced by a disassembly. */
	private static void testSystemEquatesFollowTheSystem() throws Exception {
		reset();
		Assert.longEquals(workspace.getSystemEquateList().getCount(), 899); // ATARI800
		Assert.boolEquals(menu.clearSystemEquatesMenuItem.isEnabled(), true);
		for (Object[] expected : new Object[][] { { "ORIC", 50 }, { "C64", 534 }, { "ATARI5200", 159 }, { "ATARI800", 899 } }) {
			edt(() -> {
				workspace.init();
				workspace.setComputerSystemTypeID((String) expected[0]);
			});
			Assert.longEquals(workspace.getSystemEquateList().getCount(), (Integer) expected[1]);
		}

		edt(() -> app.openFile(AUTORUN_XEX, FileType.EXECUTABLE_FILE, false));
		boolean iccomReferenced = false;
		for (Iterator<DisassemblyLine> i = workspace.getDisassemblyResult().createLineIterator(DisassemblySectionType.SYSTEM_EQUATES); i
				.hasNext();) {
			DisassemblyLine line = i.next();
			if (line.referenced && line.getLine().startsWith("ICCOM ")) {
				iccomReferenced = true;
			}
		}
		Assert.boolEquals(iccomReferenced, true); // "sta IOCB0+ICCOM,X" references ICCOM, too.
	}

	/** From OpenDispatchSmoke: menu, drop, Recent Files - one dispatcher, the type guessed where unknown. */
	private static void testOneDispatcherForEveryWayOfOpening() throws Exception {
		reset();
		// Two files "dropped": the first is opened, the second added.
		TransferHandler dropHandler = mainWindow.getFrame().getTransferHandler();
		Assert.notNull(dropHandler);
		Transferable files = fileList(AUTORUN_XEX, MULTISEGMENT_XEX);
		boolean[] accepted = new boolean[1];
		edt(() -> accepted[0] = dropHandler.importData(new TransferHandler.TransferSupport(mainWindow.getFrame(), files)));
		Assert.boolEquals(accepted[0], true);
		// The handler opens the files in a later event, and the disassembly's progress
		// dialog pumps events meanwhile - so wait for the outcome, not for an event.
		waitFor(() -> workspace.getSegmentList().getCount() == 8);

		// A .wrk of unknown type is recognized by its extension and replaces the workspace.
		reset();
		edt(() -> app.openFile(AUTORUN_WRK, FileType.ANY_FILE, false));
		Assert.longEquals(workspace.getSegmentList().getCount(), 2);
		Assert.stringEquals(new File(workspace.getFilePath()).getName(), AUTORUN_WRK.getName());

		// A file nobody recognizes is offered as a raw file; cancelling that leaves the workspace alone.
		reset();
		File junk = File.createTempFile("dis6502-junk-", ".dat");
		junk.deleteOnExit();
		Files.write(junk.toPath(), new byte[300]);
		EventQueue.invokeLater(() -> app.openFile(junk, FileType.ANY_FILE, false));
		JDialog rawFileDialog = waitForDialog(Texts.RawFileDialog_Title);
		click(findButton(rawFileDialog, UIManager.getString("OptionPane.cancelButtonText")));
		Assert.longEquals(workspace.getSegmentList().getCount(), 0);

		// Recent Files goes through the same dispatcher.
		reset();
		edt(() -> app.openRecentFile(new com.wudsn.tools.dis6502.model.MRUEntry(AUTORUN_XEX.getPath(), FileType.EXECUTABLE_FILE)));
		Assert.longEquals(workspace.getSegmentList().getCount(), 2);
	}

	/** From MenuGateSmoke: the File menu follows the computer system, the segment list, and the memory inspector's edit mode. */
	private static void testFileMenuGating() throws Exception {
		reset();
		edt(() -> workspace.setComputerSystemTypeID("C64"));
		Assert.boolEquals(menu.openCassetteImageFileMenuItem.isEnabled(), false);
		Assert.boolEquals(menu.openDiskImageSectorsMenuItem.isEnabled(), false);
		Assert.boolEquals(menu.openROMImageFileMenuItem.isEnabled(), false);
		Assert.boolEquals(menu.openExecutableFileMenuItem.isEnabled(), true);
		Assert.boolEquals(menu.openRawFileMenuItem.isEnabled(), true);
		Assert.boolEquals(menu.saveWorkspaceMenuItem.isEnabled(), false); // No segments.

		edt(() -> workspace.setComputerSystemTypeID("ATARI5200"));
		Assert.boolEquals(menu.openExecutableFileMenuItem.isEnabled(), false);
		Assert.boolEquals(menu.openROMImageFileMenuItem.isEnabled(), true);

		reset();
		edt(() -> app.openFile(AUTORUN_XEX, FileType.EXECUTABLE_FILE, false));
		Assert.boolEquals(menu.openCassetteImageFileMenuItem.isEnabled(), true);
		Assert.boolEquals(menu.saveWorkspaceMenuItem.isEnabled(), true);
		Assert.boolEquals(menu.saveDisassemblyFilesMenuItem.isEnabled(), true);

		MemoryInspectorPanel inspector = mainWindow.memoryInspectorPanel;
		edt(() -> {
			inspector.select(2, 2);
			inspector.enterEditMode();
		});
		Assert.boolEquals(inspector.isEditMode(), true);
		for (AbstractButton item : new AbstractButton[] { menu.newWorkspaceMenuItem, menu.openWorkspaceMenuItem, menu.openAnyFileMenuItem,
				menu.openExecutableFileMenuItem, menu.addExecutableFileMenuItem, menu.saveWorkspaceMenuItem, menu.saveWorkspaceAsMenuItem,
				menu.saveDisassemblyFilesMenuItem, menu.recentFilesMenu }) {
			Assert.boolEquals(item.isEnabled(), false);
		}
		edt(inspector::quitEditMode);
		Assert.boolEquals(menu.openExecutableFileMenuItem.isEnabled(), true);
		Assert.boolEquals(menu.saveWorkspaceMenuItem.isEnabled(), true);
	}

	/** From NavTypeSmoke: Navigate to Definition and Back; the immediate-type submenu's provider and its change. */
	private static void testNavigateBackAndImmediateType() throws Exception {
		reset();
		edt(() -> app.openFile(AUTORUN_XEX, FileType.EXECUTABLE_FILE, false));
		DisassemblyPanel panel = mainWindow.disassemblyPanel;
		DisassemblyResult result = workspace.getDisassemblyResult();

		DisassemblyLine jsr = findCodeLine(result, "jsr L");
		String label = jsr.getLine().trim().substring(4).trim();
		int definitionLineNumber = result.findDefinitionLineNumber(label);
		Assert.boolEquals(definitionLineNumber != 0, true);

		Assert.boolEquals(panel.canNavigateBack(), false);
		edt(() -> Assert.boolEquals(panel.navigateToDefinitionLine(definitionLineNumber, jsr), true));
		Assert.boolEquals(panel.canNavigateBack(), true);
		// The line navigated to is selected as if clicked: the memory inspector follows it.
		DisassemblyLine definition = findLine(result, definitionLineNumber);
		Assert.longEquals(workspace.getMemoryInspectorState().getBegin(), definition.offset);
		edt(panel::navigateBack);
		Assert.boolEquals(panel.canNavigateBack(), false);
		Assert.longEquals(workspace.getMemoryInspectorState().getBegin(), jsr.offset);

		// An immediate-mode instruction with a printable operand offers the submenu with Char Constant; a jsr does not.
		Assert.boolEquals(app.getDisassemblyImmediateType(jsr) == null, true);
		DisassemblyLine ldx = findCodeLine(result, "ldx #$30");
		DisassemblyPanel.ImmediateType immediateType = app.getDisassemblyImmediateType(ldx);
		Assert.notNull(immediateType);
		int segmentIndex = ldx.segmentIndex;
		int offset = ldx.offset;
		edt(() -> app.performSetDisassemblyImmediateType(ldx, MemoryType.STRING));
		String after = null;
		for (Iterator<DisassemblyLine> i = workspace.getDisassemblyResult().createLineIterator(DisassemblySectionType.CODE_LINES); i.hasNext();) {
			DisassemblyLine line = i.next();
			if (line.segmentIndex == segmentIndex && line.offset == offset) {
				after = line.getLine().trim();
			}
		}
		Assert.stringEquals(after, "ldx #'0'");
	}

	/** From DeleteCutPasteSmoke: the memory inspector's Cut/Copy/Paste/Delete through their real popup items. */
	private static void testCutCopyPasteDelete() throws Exception {
		reset();
		edt(() -> app.openFile(new File(RES + "system/atari800/Segments-SillyThings.xex"), FileType.EXECUTABLE_FILE, false));
		SegmentList segmentList = workspace.getSegmentList();
		int largest = 0;
		for (int i = 0; i < segmentList.getCount(); i++) {
			if (segmentList.getSegment(i).getSize() > segmentList.getSegment(largest).getSize()) {
				largest = i;
			}
		}
		int segmentIndex = largest;
		edt(() -> segmentList.setSelectedIndex(segmentIndex));
		Segment segment = segmentList.getSegment(segmentIndex);
		int originalSize = segment.getSize();
		byte[] before = new byte[4];
		for (int i = 0; i < 4; i++) {
			before[i] = (byte) segment.getData(2 + i);
		}
		MemoryInspectorPanel inspector = mainWindow.memoryInspectorPanel;

		Transferable previousClipboard = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
		try {
			edt(() -> inspector.select(2, 5)); // Four bytes.
			Assert.boolEquals(inspector.cutSelectionMenuItem.isEnabled() && inspector.pasteSelectionMenuItem.isEnabled()
					&& inspector.deleteSelectionMenuItem.isEnabled(), true);
			click(inspector.copySelectionMenuItem);
			String clipboard = (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
			Assert.stringEquals(clipboard, hex(before));

			click(inspector.deleteSelectionMenuItem);
			Assert.longEquals(segment.getSize(), originalSize - 4);

			edt(() -> inspector.select(2, 2));
			click(inspector.pasteSelectionMenuItem);
			Assert.longEquals(segment.getSize(), originalSize);
			byte[] after = new byte[4];
			for (int i = 0; i < 4; i++) {
				after[i] = (byte) segment.getData(2 + i);
			}
			Assert.boolEquals(Arrays.equals(before, after), true);

			// Cutting all of a segment removes it from the list.
			int count = segmentList.getCount();
			int small = -1;
			for (int i = 0; i < count && small < 0; i++) {
				if (segmentList.getSegment(i).getSize() > 0 && segmentList.getSegment(i).getSize() <= 4) {
					small = i;
				}
			}
			Assert.boolEquals(small >= 0, true);
			int smallIndex = small;
			edt(() -> {
				segmentList.setSelectedIndex(smallIndex);
				inspector.select(0, segmentList.getSegment(smallIndex).getSize() - 1);
			});
			click(inspector.cutSelectionMenuItem);
			Assert.longEquals(segmentList.getCount(), count - 1);

			// Pasting what is not a hex string is refused with a dialog, not an exception.
			edt(() -> {
				segmentList.setSelectedIndex(0);
				inspector.select(0, 0);
			});
			Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection("NOT HEX!"), null);
			EventQueue.invokeLater(inspector.pasteSelectionMenuItem::doClick);
			JDialog error = waitForDialog(Texts.Dis6502_PasteSelectionTitle);
			click(findButton(error, UIManager.getString("OptionPane.okButtonText")));
		} finally {
			if (previousClipboard != null) {
				Toolkit.getDefaultToolkit().getSystemClipboard().setContents(previousClipboard, null);
			}
		}
	}

	/** From AnyFileSmoke: Open/Add Any File through the real menu items and the real chooser. */
	private static void testOpenAnyFile() throws Exception {
		reset();
		EventQueue.invokeLater(menu.openAnyFileMenuItem::doClick);
		JFileChooser chooser = waitForChooser(Texts.Dis6502_OpenAnyFileTitle);
		String filter = chooser.getFileFilter().getDescription();
		Assert.boolEquals(filter.startsWith(Texts.FileChoosers_AllSupportedFilesFilterText), true);
		Assert.boolEquals(filter.contains("*.xex") && filter.contains("*.atr") && filter.contains("*.wrk"), true);
		edt(() -> {
			chooser.setSelectedFile(MULTISEGMENT_XEX.getAbsoluteFile());
			chooser.approveSelection();
		});
		waitFor(() -> workspace.getSegmentList().getCount() == 6);

		EventQueue.invokeLater(menu.addAnyFileMenuItem::doClick);
		JFileChooser addChooser = waitForChooser(Texts.Dis6502_AddAnyFileTitle);
		edt(() -> {
			addChooser.setSelectedFile(AUTORUN_XEX.getAbsoluteFile());
			addChooser.approveSelection();
		});
		waitFor(() -> workspace.getSegmentList().getCount() == 8);

		// The C64 cannot read cassette or disk images: its filter is shorter.
		reset();
		edt(() -> workspace.setComputerSystemTypeID("C64"));
		EventQueue.invokeLater(menu.openAnyFileMenuItem::doClick);
		JFileChooser c64Chooser = waitForChooser(Texts.Dis6502_OpenAnyFileTitle);
		String c64Filter = c64Chooser.getFileFilter().getDescription();
		Assert.boolEquals(c64Filter.contains("*.prg") && c64Filter.contains("*.wrk") && !c64Filter.contains("*.atr"), true);
		edt(c64Chooser::cancelSelection);
	}

	/** From ChooserSmoke and ScreenPass: a typed chooser's title, filter and start folder; Save As over an existing file. */
	private static void testChooserFolderAndFilter() throws Exception {
		reset();
		EventQueue.invokeLater(menu.openExecutableFileMenuItem::doClick);
		JFileChooser chooser = waitForChooser(Texts.Dis6502_OpenExecutableFileTitle);
		Assert.boolEquals(chooser.getFileFilter().getDescription().startsWith(FileType.EXECUTABLE_FILE.getFilterText()), true);
		// The last executable opened (in the previous scenario) decides where the chooser starts.
		Assert.stringEquals(chooser.getCurrentDirectory().getPath(), AUTORUN_XEX.getAbsoluteFile().getParentFile().getPath());
		edt(chooser::cancelSelection);

		edt(() -> app.openFile(AUTORUN_XEX, FileType.EXECUTABLE_FILE, false));
		File existing = File.createTempFile("dis6502-overwrite-", ".wrk");
		existing.deleteOnExit();
		Files.write(existing.toPath(), new byte[] { 1, 2, 3 });
		EventQueue.invokeLater(app::performSaveWorkspaceAs);
		JFileChooser saveChooser = waitForChooser(Texts.Dis6502_SaveWorkspaceFileAsTitle);
		Assert.boolEquals(saveChooser.getFileFilter().getDescription().startsWith(FileType.WORKSPACE_FILE.getFilterText()), true);
		edt(() -> {
			saveChooser.setSelectedFile(existing);
			saveChooser.approveSelection();
		});
		JDialog confirm = waitForDialog(Texts.FileChoosers_OverwriteTitle);
		click(findButton(confirm, UIManager.getString("OptionPane.yesButtonText")));
		waitFor(() -> existing.length() > 100);
		Assert.stringEquals(workspace.getFilePath(), existing.getPath());
	}

	// ------------------------------------------------------------------
	// Driving the application.
	// ------------------------------------------------------------------

	private static void start() throws Exception {
		if (Dis6502.getInstance() == null) {
			edt(() -> Dis6502.main(new String[0]));
			waitFor(() -> Dis6502.getInstance() != null && Dis6502.getInstance().getMainWindow() != null);
		}
		app = Dis6502.getInstance();
		workspace = app.getWorkspace();
		mainWindow = app.getMainWindow();
		menu = mainWindow.mainMenu;
	}

	/** An empty Atari 800 workspace, no edit mode, no dialog left open. */
	private static void reset() throws Exception {
		edt(() -> {
			mainWindow.memoryInspectorPanel.quitEditMode();
			workspace.init();
			workspace.setComputerSystemTypeID("ATARI800");
		});
		for (Window window : Window.getWindows()) {
			if (window instanceof JDialog && window.isVisible()) {
				edt(window::dispose);
			}
		}
	}

	private static void edt(Runnable runnable) throws Exception {
		Throwable[] failure = new Throwable[1];
		SwingUtilities.invokeAndWait(() -> {
			try {
				runnable.run();
			} catch (Throwable ex) {
				failure[0] = ex;
			}
		});
		if (failure[0] instanceof Exception) {
			throw (Exception) failure[0];
		} else if (failure[0] != null) {
			throw (Error) failure[0];
		}
	}

	private static void click(AbstractButton button) throws Exception {
		Assert.boolEquals(button.isEnabled(), true);
		edt(button::doClick);
	}

	private interface Condition {
		boolean holds() throws Exception;
	}

	private static void waitFor(Condition condition) throws Exception {
		for (int i = 0; i < 200; i++) {
			if (condition.holds()) {
				return;
			}
			Thread.sleep(50);
		}
		Assert.fail("Timed out waiting.");
	}

	private static JDialog waitForDialog(String title) throws Exception {
		JDialog[] found = new JDialog[1];
		waitFor(() -> {
			for (Window window : Window.getWindows()) {
				if (window instanceof JDialog && window.isVisible() && title.equals(((JDialog) window).getTitle())) {
					found[0] = (JDialog) window;
					return true;
				}
			}
			return false;
		});
		return found[0];
	}

	private static JFileChooser waitForChooser(String title) throws Exception {
		JDialog dialog = waitForDialog(title);
		JFileChooser chooser = find(dialog, JFileChooser.class, null);
		Assert.notNull(chooser);
		return chooser;
	}

	private static JButton findButton(Container container, String text) {
		JButton button = find(container, JButton.class, text);
		Assert.notNull(button);
		return button;
	}

	private static <T extends Component> T find(Container container, Class<T> type, String buttonText) {
		for (Component component : container.getComponents()) {
			if (type.isInstance(component)
					&& (buttonText == null || component instanceof AbstractButton && buttonText.equals(((AbstractButton) component).getText()))) {
				return type.cast(component);
			}
			if (component instanceof Container) {
				T result = find((Container) component, type, buttonText);
				if (result != null) {
					return result;
				}
			}
		}
		return null;
	}

	private static Transferable fileList(File... files) {
		List<File> list = new ArrayList<>(Arrays.asList(files));
		return new Transferable() {
			@Override
			public DataFlavor[] getTransferDataFlavors() {
				return new DataFlavor[] { DataFlavor.javaFileListFlavor };
			}

			@Override
			public boolean isDataFlavorSupported(DataFlavor flavor) {
				return DataFlavor.javaFileListFlavor.equals(flavor);
			}

			@Override
			public Object getTransferData(DataFlavor flavor) {
				return list;
			}
		};
	}

	private static DisassemblyLine findCodeLine(DisassemblyResult result, String startsWith) {
		for (Iterator<DisassemblyLine> i = result.createLineIterator(DisassemblySectionType.CODE_LINES); i.hasNext();) {
			DisassemblyLine line = i.next();
			if (line.getLine().trim().startsWith(startsWith)) {
				return line;
			}
		}
		Assert.fail("No code line starting with '" + startsWith + "'.");
		return null;
	}

	private static DisassemblyLine findLine(DisassemblyResult result, int lineNumber) {
		for (Iterator<DisassemblyLine> i = result.createLineIterator(); i.hasNext();) {
			DisassemblyLine line = i.next();
			if (line.getLineNumber() == lineNumber) {
				return line;
			}
		}
		Assert.fail("No line " + lineNumber + ".");
		return null;
	}

	private static String hex(byte[] bytes) {
		StringBuilder builder = new StringBuilder();
		for (byte b : bytes) {
			builder.append(String.format("%02X", b & 0xFF));
		}
		return builder.toString();
	}
}
