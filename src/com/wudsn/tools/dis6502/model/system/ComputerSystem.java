/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model.system;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import com.wudsn.tools.dis6502.model.FileType;
import com.wudsn.tools.dis6502.model.Memory;
import com.wudsn.tools.dis6502.model.SegmentList;
import com.wudsn.tools.dis6502.model.SegmentListInserter;

/**
 * A target computer system (Atari 800, Atari 5200, C64, Oric, or an unknown
 * system): its base/vector address knowledge, and how to read/write its
 * native file formats into/from a {@link SegmentList}.
 * <p>
 * {@link #openResourceByExtension} reads a per-system resource file
 * ({@code <fileName><extension>}, next to the concrete subclass, e.g.
 * {@code Atari800.equ} next to {@code Atari800.class}) from the classpath,
 * since a Java application has no install folder to rely on and the
 * resource travels inside the jar this way. {@link #guessFileType(File)}
 * reads only the file's 4-byte header directly, not the whole file.
 *
 * @author Peter Dell
 */
public abstract class ComputerSystem {

	protected final ComputerSystemType computerSystemType;
	protected int returnCharacter;
	protected List<FileType> supportedFileTypes = new ArrayList<>();

	protected ComputerSystem(ComputerSystemType computerSystemType) {
		this.computerSystemType = computerSystemType;
	}

	public ComputerSystemType getType() {
		return computerSystemType;
	}

	/** Gets the return character used to indicate line ends in strings. */
	public int getReturnCharacter() {
		return returnCharacter;
	}

	public abstract boolean isBaseAddress(int address);

	/**
	 * Also used in code trace: this is how the program finds LO_BYTE/HI_BYTE
	 * pairs like
	 *
	 * <pre>
	 * lda #$34
	 * sta VKEYBD
	 * lda #$12
	 * sta VKEYBD+1
	 * </pre>
	 *
	 * If such a pattern is found and {@code isVectorAddress(VKEYBD)} is true,
	 * then {@code $1234} is considered an address, a label {@code "L1234"} is
	 * created for it, and the two {@code lda} instructions are rewritten as
	 * {@code lda #<L1234}/{@code lda #>L1234}.
	 */
	public abstract boolean isVectorAddress(int address);

	/** Returns true if the address is a display list pointer address. Default: false. */
	public boolean isDisplayListVectorAddress(int address) {
		return false;
	}

	/** Guesses the file type from the file's name, size, and content. */
	public FileType guessFileType(File filePath) throws IOException {
		long fileSize = filePath.length();
		if (fileSize < 4) {
			return FileType.ANY_FILE;
		}
		byte[] header = new byte[4];
		try (DataInputStream in = new DataInputStream(new FileInputStream(filePath))) {
			in.readFully(header);
		}
		return guessFileType(fileSize, header);
	}

	/** The classpath name of this system's resource file with the given extension (e.g. {@code ".equ"}), relative to the concrete subclass. */
	public String getResourceNameByExtension(String extension) {
		return computerSystemType.getFileName() + extension;
	}

	/**
	 * Opens this system's resource file with the given extension, or returns
	 * {@code null} if this system has none (e.g. the unknown system has no
	 * system equates).
	 */
	public InputStream openResourceByExtension(String extension) {
		return getClass().getResourceAsStream(getResourceNameByExtension(extension));
	}

	/** Guesses the file type from the file's size and its first 4 bytes of content. */
	public abstract FileType guessFileType(long fileSize, byte[] content);

	public boolean isSupportedFileType(FileType fileType) {
		return supportedFileTypes.contains(fileType);
	}

	/** Reads a file and adds one or more segments to the segment list. */
	public void readFile(FileType fileType, InputStream inputStream, long fileSize,
			SegmentListInserter segmentListInserter) throws IOException {
		if (!isSupportedFileType(fileType)) {
			throw new UnsupportedOperationException("File type is not supported.");
		}

		// An if chain, not a switch: FileType is a ValueSet, not an enum.
		if (fileType == FileType.CASSETTE_IMAGE_FILE) {
			readCassetteFile(segmentListInserter, inputStream, fileSize);
		} else if (fileType == FileType.EXECUTABLE_FILE) {
			readExecutableFile(segmentListInserter, inputStream, fileSize);
		} else if (fileType == FileType.ROM_IMAGE_FILE) {
			readROMFile(segmentListInserter, inputStream, fileSize);
		} else {
			throw new UnsupportedOperationException("File type is not supported.");
		}
	}

	/** Writes a segment, or all segments if {@code firstSegmentIndex} is {@link SegmentList#NO_SEGMENT_INDEX}, to an executable file. */
	public void writeExecutableFile(SegmentList segmentList, int firstSegmentIndex, boolean writeHeader,
			OutputStream outputStream) throws IOException {
		throw new UnsupportedOperationException("Operation is not supported.");
	}

	protected void readCassetteFile(SegmentListInserter segmentListInserter, InputStream inputStream, long fileSize)
			throws IOException {
		throw new UnsupportedOperationException("Operation is not supported.");
	}

	protected void readExecutableFile(SegmentListInserter segmentListInserter, InputStream inputStream, long fileSize)
			throws IOException {
		throw new UnsupportedOperationException("Operation is not supported.");
	}

	protected void readROMFile(SegmentListInserter segmentListInserter, InputStream inputStream, long fileSize)
			throws IOException {
		throw new UnsupportedOperationException("Operation is not supported.");
	}

	/** Reads one unsigned byte, throwing {@link EOFException} if the stream has ended. */
	protected static int readByte(InputStream inputStream) throws IOException {
		int value = inputStream.read();
		if (value < 0) {
			throw new EOFException("Unexpected end of file.");
		}
		return value;
	}

	/** Reads a little-endian 16 bit word (low byte first). */
	protected static int readWordLE(InputStream inputStream) throws IOException {
		int low = readByte(inputStream);
		int high = readByte(inputStream);
		return Memory.toAddress(low, high);
	}
}
