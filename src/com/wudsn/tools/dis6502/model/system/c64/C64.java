/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model.system.c64;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.wudsn.tools.dis6502.Messages;
import com.wudsn.tools.dis6502.model.FileType;
import com.wudsn.tools.dis6502.model.Segment;
import com.wudsn.tools.dis6502.model.SegmentList;
import com.wudsn.tools.dis6502.model.SegmentListInserter;
import com.wudsn.tools.dis6502.model.system.ComputerSystem;
import com.wudsn.tools.dis6502.model.system.ComputerSystemType;

/**
 * The Commodore 64 computer system.
 * <ul>
 * <li>{@link #VECTOR_ADDRESSES}/{@link #BASE_ADDRESSES} hold the C64's
 * actual pointer locations, so code trace recognizes {@code LDA #<irq /
 * STA CINV / LDA #>irq / STA CINV+1} and follows it. All names are the
 * ones in {@code systems/C64.equ}.</li>
 * <li>{@link #readExecutableFile} marks the loaded segment as binary, so a
 * {@code .prg} is actually disassembled, rather than left as a raw segment
 * until the user changes that in the segment properties.</li>
 * <li>{@link #guessFileType} recognizes a {@code .prg}, so one can be
 * dropped on the window or passed on the command line.</li>
 * </ul>
 *
 * @author Peter Dell
 */
public final class C64 extends ComputerSystem {

	private static final int PRG_HEADER_SIZE = 2;
	private static final long PRG_MAX_SIZE = PRG_HEADER_SIZE + 0x10000L;

	/** Locations that hold a pointer to code - see {@link ComputerSystem#isVectorAddress}. */
	private static final Set<Integer> VECTOR_ADDRESSES = Set.of(0x028F /* KEYLOG */, 0x0300 /* IERROR */,
			0x0302 /* IMAIN */, 0x0304 /* ICRNCH */, 0x0306 /* IQPLOP */, 0x0308 /* IGONE */, 0x030A /* IEVAL */,
			0x0311 /* USRADD */, 0x0314 /* CINV */, 0x0316 /* CBINV */, 0x0318 /* NMINV */, 0x031A /* IOPEN */,
			0x031C /* ICLOSE */, 0x031E /* ICHKIN */, 0x0320 /* ICKOUT */, 0x0322 /* ICLRCH */, 0x0324 /* IBASIN */,
			0x0326 /* IBSOUT */, 0x0328 /* ISTOP */, 0x032A /* IGETIN */, 0x032C /* ICLALL */, 0x032E /* USRCMD */,
			0x0330 /* ILOAD */, 0x0332 /* ISAVE */,
			// The hardware vectors are writable, too: RAM under the KERNAL ROM once it is banked out.
			0xFFFA /* NMIVEC */, 0xFFFC /* RESVEC */, 0xFFFE /* IRQVEC */);

	/**
	 * Locations outside the zero page that hold a pointer of any kind: every
	 * vector, plus the pointers to data. Deliberately without the 16 bit
	 * hardware registers that are no addresses (CIA timers, SID frequency/
	 * pulse width) - their two immediate loads must stay plain numbers.
	 */
	private static final Set<Integer> BASE_ADDRESSES;
	static {
		Set<Integer> baseAddresses = new HashSet<>(VECTOR_ADDRESSES);
		baseAddresses.addAll(List.of(0x0281 /* MEMSTR */, 0x0283 /* MEMSIZK */));
		BASE_ADDRESSES = Set.copyOf(baseAddresses);
	}

	public C64(ComputerSystemType computerSystemType) {
		super(computerSystemType);
		returnCharacter = 0x0d;
		supportedFileTypes = List.of(FileType.RAW_FILE, FileType.EXECUTABLE_FILE);
	}

	@Override
	public boolean isBaseAddress(int address) {
		if (BASE_ADDRESSES.contains(address)) {
			return true;
		}
		return (address & 0xFF00) == 0; // Any zero page location may hold a pointer.
	}

	@Override
	public boolean isVectorAddress(int address) {
		return VECTOR_ADDRESSES.contains(address);
	}

	@Override
	public FileType guessFileType(long fileSize, byte[] content) {
		// A .prg has no magic bytes, just a 2 byte load address - so anything that would
		// load above the zero page/stack and still fit into memory counts as one.
		// Everything else is left for the caller to offer as a raw file.
		int address = (content[0] & 0xFF) | ((content[1] & 0xFF) << 8);
		long size = fileSize - PRG_HEADER_SIZE;
		if (address >= 0x0200 && size > 0 && address + size <= 0x10000L) {
			return FileType.EXECUTABLE_FILE;
		}
		return FileType.ANY_FILE;
	}

	@Override
	protected void readExecutableFile(SegmentListInserter segmentListInserter, InputStream inputStream, long fileSize)
			throws IOException {
		if (fileSize > PRG_MAX_SIZE) {
			// ERROR: File size of {0} bytes exceeds the maximum file size of executable files on C64.
			throw new IOException(Messages.E059.format(String.valueOf(fileSize)));
		}
		int address = readWordLE(inputStream);

		int size = (int) (fileSize - PRG_HEADER_SIZE);
		Segment segment = segmentListInserter.insertSegment();
		segment.bBinary = true;
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
				// ERROR: Executable files on C64 can only have one segment.
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
