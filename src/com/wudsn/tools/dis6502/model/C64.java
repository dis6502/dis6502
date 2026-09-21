/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Set;

import com.wudsn.tools.dis6502.Messages;

/**
 * The Commodore 64 computer system.
 * <p>
 * Ported from systems/c64/C64.h / C64.cpp.
 *
 * @author Peter Dell
 */
public final class C64 extends ComputerSystem {

	private static final int PRG_HEADER_SIZE = 2;
	private static final long PRG_MAX_SIZE = PRG_HEADER_SIZE + 0x10000L;

	private static final Set<Integer> BASE_ADDRESSES = Set.of(0x0200 /* DUMMY EXAMPLE */);
	private static final Set<Integer> VECTOR_ADDRESSES = Set.of(0x0200 /* DUMMY EXAMPLE */);

	public C64(ComputerSystemTypeInfo computerSystemTypeInfo) {
		super(computerSystemTypeInfo);
		returnCharacter = 0x0d;
		supportedFileTypes = List.of(FileType.RAW_FILE, FileType.EXECUTABLE_FILE);
	}

	@Override
	public boolean isBaseAddress(int address) {
		if (BASE_ADDRESSES.contains(address)) {
			return true;
		}
		return (address & 0xFF00) == 0; // TODO: Why are these base addresses?
	}

	@Override
	public boolean isVectorAddress(int address) {
		return VECTOR_ADDRESSES.contains(address);
	}

	@Override
	public FileType guessFileType(long fileSize, byte[] content) {
		return FileType.UNKNOWN_FILE;
	}

	@Override
	protected void readExecutableFile(SegmentListInserter segmentListInserter, InputStream inputStream, long fileSize)
			throws IOException {
		if (fileSize > PRG_MAX_SIZE) {
			throw new IOException(Messages.E059.format(String.valueOf(fileSize)));
		}
		int address = readWordLE(inputStream);

		int size = (int) (fileSize - PRG_HEADER_SIZE);
		Segment segment = segmentListInserter.insertSegment();
		segment.wBegin = address;
		segment.wEnd = address + size - 1;
		segment.createMemoryBlockFromFile(size, inputStream);
	}

	@Override
	public void writeExecutableFile(SegmentList segmentList, int firstSegmentIndex, boolean writeHeader,
			OutputStream outputStream) throws IOException {
		int segmentIndex;
		if (firstSegmentIndex == SegmentList.NO_SEGMENT_INDEX) {
			if (segmentList.getCount() > 1) {
				throw new IOException(Messages.E060.format());
			}
			segmentIndex = 0;
		} else {
			segmentIndex = firstSegmentIndex;
		}
		Segment segment = segmentList.getSegment(segmentIndex);
		int address = segment.wBegin;
		outputStream.write(address & 0xFF);
		outputStream.write((address >> 8) & 0xFF);
		segment.memoryBlock.writeData(outputStream);
	}
}
