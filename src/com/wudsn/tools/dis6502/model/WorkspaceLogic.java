/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
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
import com.wudsn.tools.dis6502.Messages;
import com.wudsn.tools.dis6502.model.system.ComputerSystem;
import com.wudsn.tools.dis6502.model.system.atari800.DiskImage;
import com.wudsn.tools.dis6502.model.system.atari800.ImgRWPacket;

/**
 * Loads and saves a {@link Workspace}, and adds files to it.
 * <ul>
 * <li>{@link #load} peeks at the file's 12 byte magic, but only dispatches to
 * the modern XML loader or {@link Workspace1X#load14} - {@code
 * Workspace1X.Load10}/{@code Save14} are not implemented (see
 * {@link Workspace1X}'s javadoc for why), so a {@code
 * DIS6502WRK10} file, like any other unrecognized magic, falls through to the
 * XML loader, fails to parse, and is reported as "not a valid workspace".</li>
 * <li>{@link #save} only writes the modern XML format; there is no format
 * parameter, since nothing needs to write a legacy format going forward.</li>
 * <li>{@link #loadSystemEquates} reads the computer system's {@code .equ} file
 * from the classpath instead of from an application install folder - see
 * {@link ComputerSystem#openResourceByExtension}.</li>
 * </ul>
 * {@link #load}'s error path keeps {@link Workspace#beginUpdate}/{@link
 * Workspace#endUpdate} balanced even when loading fails, calling {@code
 * endUpdate} before {@code init()} on that branch: an unbalanced counter
 * would silently break event flushing until some unrelated, later,
 * over-matched {@code endUpdate} call happened to rebalance it.
 *
 * @author Peter Dell
 */
public final class WorkspaceLogic {

	private final Application application;

	public WorkspaceLogic(Application application) {
		this.application = application;
	}

	/**
	 * Replaces the workspace's system equates with the ones shipped for its current
	 * computer system (the hardware/OS labels that make a disassembly read
	 * {@code STA COLBK} instead of {@code STA $D01A}). A system without an equates
	 * file (the unknown system) simply ends up with an empty list. {@code C64.equ}'s
	 * KERNAL/BASIC source label names were taken from "Mapping the Commodore 64"
	 * rather than from any existing template, since a mislabeled Atari800
	 * template would have labeled the VIC-II's {@code $D01A} as Atari's
	 * {@code COLBK} - see {@code ComputerFont} for the same mislabeling on
	 * {@code C64.fon}.
	 */
	public void loadSystemEquates(Workspace workspace) {
		ComputerSystem computerSystem = workspace.getComputerSystem();
		EquateList systemEquateList = workspace.getSystemEquateList();
		InputStream inputStream = computerSystem.openResourceByExtension(".equ");
		if (inputStream == null) {
			systemEquateList.clear();
			return;
		}
		new EquateListLogic(application).load(systemEquateList, inputStream,
				computerSystem.getResourceNameByExtension(".equ"));
	}

	/**
	 * Loads a workspace from disk. Returns {@code false}, and logs, instead of
	 * throwing.
	 */
	public boolean load(Workspace workspace, String filePath) {
		workspace.init();

		// INFO: Loading workspace file "{0}".
		application.sendMessage(Messages.I011, filePath);

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
			// ERROR: Not a valid workspace.
			application.sendMessage(Messages.E003);
			application.sendErrorMessage(ex);
		}

		return false;
	}

	/**
	 * Reads the file's first {@link Workspace1X#MAGIC_SIZE} bytes as ASCII, or
	 * {@code ""} if the file is shorter.
	 */
	private static String readMagic(File file) throws IOException {
		byte[] magic = new byte[Workspace1X.MAGIC_SIZE];
		try (InputStream inputStream = new FileInputStream(file)) {
			int totalRead = 0;
			int read;
			while (totalRead < magic.length
					&& (read = inputStream.read(magic, totalRead, magic.length - totalRead)) >= 0) {
				totalRead += read;
			}
			if (totalRead < magic.length) {
				return "";
			}
		}
		return new String(magic, StandardCharsets.US_ASCII);
	}

	/**
	 * Saves a workspace to disk. Returns {@code false}, and logs, instead of
	 * throwing.
	 */
	public boolean save(Workspace workspace, String filePath) {
		// INFO: Saving workspace file "{0}".
		application.sendMessage(Messages.I014, filePath);
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
			application.sendErrorMessage(ex);
		}
	}

	/**
	 * Builds and inserts a single segment from a disk image's Atari DOS boot
	 * sectors: {@code firstSectorData} holds sector 1's already-read content in its
	 * first 128 bytes (byte 1 is the total boot sector count, bytes 2-3 the
	 * little-endian load address) - it may be longer, e.g. an
	 * {@link ImgRWPacket#sectorData}'s fixed 256-byte buffer, since only the first
	 * 128 bytes are ever read from it, matching boot sectors always being 128 bytes
	 * regardless of the disk's overall sector size. Any further sectors the chain
	 * needs are read directly via {@link DiskImage#readSector}.
	 * <p>
	 * Combines building the segment and reading its remaining sectors in one
	 * place, the same way {@link #addRawSegment} folds its inserter's whole
	 * lifecycle into a single method. The chunk sizes this walks (122 bytes for
	 * the payload already in sector 1, 128 for every following sector) always
	 * divide {@code sectorCount * 128 - 6} evenly down to exactly 0, so no
	 * bounds clamping is needed on the per-sector writes.
	 */
	public void addDiskImageBootSectorsSegment(Workspace workspace, String diskImageFilePath, byte[] firstSectorData) {
		SegmentListInserter segmentListInserter = workspace.getSegmentList().createInserter();
		try {
			Segment segment = segmentListInserter.insertSegment();

			int sectorCount = firstSectorData[1] & 0xFF;
			int size = sectorCount * 128 - 6;

			segment.wBegin = (firstSectorData[2] & 0xFF) + (firstSectorData[3] & 0xFF) * 256 + 6;
			segment.wEnd = segment.wBegin + size - 1;
			segment.createMemoryBlockFromBeginToEnd();
			segment.bBinary = true;

			int chunkSize = Math.min(128 - 6, size);
			int position = 0;
			segment.setData(position, firstSectorData, 6, chunkSize);

			size -= chunkSize;
			sectorCount--;
			int sectorNumber = 2;

			while (size > 0 && sectorCount > 0) {
				position += chunkSize;
				chunkSize = Math.min(128, size);

				ImgRWPacket sector = new ImgRWPacket();
				sector.filePath = diskImageFilePath;
				sector.sectorNumber = sectorNumber++;
				sector.sectorSize = 128;
				DiskImage.readSector(sector);

				if (DiskImage.isError(sector.result)) {
					break;
				}
				segment.setData(position, sector.sectorData, 0, sector.sectorSize);

				size -= chunkSize;
				sectorCount--;
			}

			segmentListInserter.apply();
		} catch (RuntimeException ex) {
			segmentListInserter.cancel();
			application.sendErrorMessage(ex);
		}
	}
}
