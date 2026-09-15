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

import com.wudsn.tools.dis6502.Application;
import com.wudsn.tools.dis6502.Text;

/**
 * Loads and saves a {@link Workspace}, and adds files to it. Ported from
 * WorkspaceLogic.h / WorkspaceLogic.cpp, scoped down to what needs only
 * already-ported pieces:
 * <ul>
 * <li>{@link #load}/{@link #save} only support the modern XML workspace
 * format ({@code Workspace::Format::WORKSPACE36} in C++) - the legacy
 * binary "1X"/"14" formats ({@code Workspace1X}, and the {@code
 * Workspace::Format} parameter that picks between them) are not ported.
 * The C++ version peeks at the file's magic bytes to dispatch to whichever
 * loader applies; since only the XML loader exists here, {@link #load}
 * skips that check and simply attempts the XML load - a legacy-format
 * file fails to parse as XML and is reported as "not a valid workspace"
 * exactly like any other malformed workspace file, which is the same
 * user-visible behavior the C++ version's dedicated magic check produces
 * for a file it can't handle at all.</li>
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

		workspace.beginUpdate();
		try {
			Xml.load(workspace, "Workspace", new File(filePath));
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
