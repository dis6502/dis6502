/*
 * Copyright (C) 2025 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import com.wudsn.tools.dis6502.Application;
import com.wudsn.tools.dis6502.Text;

/**
 * Loads and saves a {@link Workspace}, and adds files to it. Ported from
 * WorkspaceLogic.h / WorkspaceLogic.cpp, scoped down to what needs only
 * already-ported pieces:
 * <ul>
 * <li>{@link #load} peeks at the file's 12 byte magic like the C++
 * version, but only dispatches to the modern XML loader or {@link
 * Workspace1X#load14} - {@code Workspace1X.Load10}/{@code Save14} are not
 * ported (see {@link Workspace1X}'s javadoc for why), so a {@code
 * DIS6502WRK10} file, like any other unrecognized magic, falls through to
 * the XML loader, fails to parse, and is reported as "not a valid
 * workspace" - the same user-visible outcome the C++ version's dedicated
 * magic check produces for a file it can't handle at all.</li>
 * <li>{@link #save} only writes the modern XML format ({@code
 * Workspace::Format::WORKSPACE36} in C++); there is no {@code
 * Workspace::Format} parameter, since nothing needs to write the legacy
 * format going forward.</li>
 * <li>{@code LoadSystemEquates} is not ported: it needs both {@code
 * EquateListLogic} (not ported) and {@code ComputerSystem}'s {@code
 * GetResourceFilePathByExtension} (also not ported - see {@code
 * ComputerSystem}'s javadoc - it needs an application install-folder path
 * that only the not-yet-ported {@code Application.GetModuleFilePath}
 * would provide).</li>
 * </ul>
 * Found and fixed a bug while porting {@link #load}: the C++ version's
 * error path calls {@code Workspace::Init} without first matching the
 * earlier {@code Workspace::BeginUpdate} with an {@code EndUpdate}, so a
 * failed load permanently leaves the workspace's update counter one too
 * high (event flushing silently stops working until some unrelated,
 * later, over-matched {@code EndUpdate} call happens to rebalance it) -
 * fixed upstream (see that commit) and correct here from the start.
 *
 * @author Peter Dell
 */
public final class WorkspaceLogic {

	private final Application application;

	public WorkspaceLogic(Application application) {
		this.application = application;
	}

	/** Loads a workspace from disk. Returns {@code false}, and logs, instead of throwing. */
	public boolean load(Workspace workspace, String filePath) {
		workspace.init();

		application.sendInfoMessage(Text.IDS_LOG_OPEN_WORK, filePath);

		File file = new File(filePath);
		workspace.beginUpdate();
		try {
			if (Workspace1X.MAGIC14.equals(readMagic(file))) {
				try (InputStream inputStream = new FileInputStream(file)) {
					Workspace1X.skipFully(inputStream, Workspace1X.MAGIC_SIZE);
					Workspace1X.load14(workspace, inputStream);
				}
			} else {
				Xml.load(workspace, "Workspace", file);
			}
			workspace.setFilePath(filePath);

			SegmentList segmentList = workspace.getSegmentList();
			if (segmentList.getCount() > 0) {
				segmentList.setSelectedIndex(0);
			}
			workspace.endUpdate();
			return true;
		} catch (IOException ex) {
			workspace.endUpdate();
			workspace.init();
			application.sendErrorMessage(Text.IDS_ERR_BAD_WORKSPACE);
			application.sendErrorMessage(ex);
		}

		return false;
	}

	/** Reads the file's first {@link Workspace1X#MAGIC_SIZE} bytes as ASCII, or {@code ""} if the file is shorter. */
	private static String readMagic(File file) throws IOException {
		byte[] magic = new byte[Workspace1X.MAGIC_SIZE];
		try (InputStream inputStream = new FileInputStream(file)) {
			int totalRead = 0;
			int read;
			while (totalRead < magic.length && (read = inputStream.read(magic, totalRead, magic.length - totalRead)) >= 0) {
				totalRead += read;
			}
			if (totalRead < magic.length) {
				return "";
			}
		}
		return new String(magic, StandardCharsets.US_ASCII);
	}

	/** Saves a workspace to disk. Returns {@code false}, and logs, instead of throwing. */
	public boolean save(Workspace workspace, String filePath) {
		application.sendInfoMessage(Text.IDS_LOG_SAVE_WORKSPACE_FILE, filePath);
		workspace.setFilePath(filePath);

		try (OutputStream outputStream = new FileOutputStream(filePath)) {
			Xml.save(workspace, "Workspace", outputStream);
			return true;
		} catch (IOException ex) {
			application.sendErrorMessage(ex);
			return false;
		}
	}

	public boolean addFile(Workspace workspace, FileType fileType, String filePath) {
		File file = new File(filePath);
		try (InputStream inputStream = new FileInputStream(file)) {
			return addFile(workspace, fileType, inputStream, file.length());
		} catch (IOException ex) {
			application.sendErrorMessage(ex);
			return false;
		}
	}

	public boolean addFile(Workspace workspace, FileType fileType, InputStream inputStream, long fileSize) {
		SegmentListInserter segmentListInserter = workspace.getSegmentList().createInserter();
		ComputerSystem computerSystem = workspace.getComputerSystem();
		try {
			computerSystem.readFile(fileType, inputStream, fileSize, segmentListInserter);
		} catch (IOException ex) {
			segmentListInserter.cancel();
			application.sendErrorMessage(ex);
			return false;
		}

		segmentListInserter.apply();
		return true;
	}

	public void addRawSegment(Workspace workspace, byte[] buffer, int offset, int size, int address) {
		SegmentListInserter segmentListInserter = workspace.getSegmentList().createInserter();
		try {
			Segment segment = segmentListInserter.insertSegment();
			segment.wBegin = address;
			segment.wEnd = address + size - 1;

			segment.createMemoryBlockFromBeginToEnd();

			segment.bBinary = true;
			segment.setData(0, buffer, offset, size);

			segmentListInserter.apply();
		} catch (RuntimeException ex) {
			segmentListInserter.cancel();
			application.sendErrorMessage(Text.IDS_ERR_READING_FILE, String.valueOf(ex.getMessage()));
		}
	}
}
