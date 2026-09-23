/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model.system.oric;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;

import com.wudsn.tools.dis6502.Messages;
import com.wudsn.tools.dis6502.model.FileHeader;
import com.wudsn.tools.dis6502.model.FileType;
import com.wudsn.tools.dis6502.model.Memory;
import com.wudsn.tools.dis6502.model.Segment;
import com.wudsn.tools.dis6502.model.SegmentListInserter;
import com.wudsn.tools.dis6502.model.system.ComputerSystem;
import com.wudsn.tools.dis6502.model.system.ComputerSystemType;

/**
 * The Oric computer system.
 * <p>
 * The Oric ".tap" file format, per
 * https://forum.defence-force.org/viewtopic.php?f=19&amp;t=201:
 *
 * <pre>
 * $16 $16 $16 (minimum of three $16's)
 * $24 (synchronisation byte)
 * $00 $00
 * $00 (Basic) or $80 (Assembler)
 * $00 (no autostart) or $80 (Basic autostart) or $C7 (assembly autostart)
 * high byte of the end address, low byte of the end address
 * high byte of the start address, low byte of the start address
 * $0
 * filename (up to 16 characters)
 * $0
 * program data
 * </pre>
 *
 * @author Peter Dell
 */
public final class Oric extends ComputerSystem {

	private static final Set<Integer> BASE_ADDRESSES = Set.of(0x0238 /* XVDU */, 0x023B /* XGETKY */,
			0x023E /* XPRTCH */, 0x0241 /* XSTOUT */, 0x0244 /* INTFS */, 0x02E0 /* BAPARM */, 0x02FC /* ASMADD */);

	private static final int END_OF_SYNC = 0x24;

	public Oric(ComputerSystemType computerSystemType) {
		super(computerSystemType);
		returnCharacter = 0x0d;
		supportedFileTypes = List.of(FileType.RAW_FILE, FileType.EXECUTABLE_FILE);
	}

	@Override
	public boolean isBaseAddress(int address) {
		if (BASE_ADDRESSES.contains(address)) {
			return true;
		}
		return (address & 0xFF00) == 0; // TODO: Are these really base addresses on Oric?
	}

	@Override
	public boolean isVectorAddress(int address) {
		return false;
	}

	@Override
	public FileType guessFileType(long fileSize, byte[] content) {
		// TODO: See Orix header format https://orix.oric.org/orix-header/
		// See tape header format https://forum.defence-force.org/viewtopic.php?t=201
		int headerValue = FileHeader.ORIC_BINARY.getValue();
		if ((content[0] & 0xFF) == (headerValue & 0xFF) && (content[1] & 0xFF) == ((headerValue >> 8) & 0xFF)) {
			return FileType.EXECUTABLE_FILE;
		}
		return FileType.ANY_FILE;
	}

	@Override
	protected void readExecutableFile(SegmentListInserter segmentListInserter, InputStream inputStream, long fileSize)
			throws IOException {
		// Check that the file begins with 2 0xFF (well, both bytes of $1616 happen to be equal).
		int header = readWordLE(inputStream);
		if (header != FileHeader.ORIC_BINARY.getValue()) {
			// ERROR: Unsupported file header {0}.
			throw new IOException(Messages.E051.format(String.valueOf(header)));
		}

		long bytesRemaining = fileSize - 2;

		while (bytesRemaining > 0) {
			// Read bytes until we find an end-of-synchronisation mark.
			int b;
			do {
				b = readByte(inputStream);
				bytesRemaining--;
			} while (b != END_OF_SYNC);

			// Skip 2 bytes, then read the program type (Basic or 6502), then skip 1 byte.
			readByte(inputStream);
			readByte(inputStream);
			int asmType = readByte(inputStream);
			readByte(inputStream);
			bytesRemaining -= 4;

			// Read the segment end and start address (big-endian on disk).
			int end = readWordBE(inputStream);
			int begin = readWordBE(inputStream);

			if (end < begin) {
				// ERROR: Segment end address is lower than segment start address.
				throw new IOException(Messages.E052.format());
			}
			bytesRemaining -= 4;

			// Read the name of the program/data.
			readByte(inputStream);
			bytesRemaining--;
			Segment segment = segmentListInserter.insertSegment();

			StringBuilder title = new StringBuilder();
			for (int nameIndex = 0; nameIndex < 16; nameIndex++) {
				int c = readByte(inputStream);
				bytesRemaining--;
				if (c == 0) {
					break;
				}
				title.append((char) c);
			}
			segment.title = title.toString();

			// Check the data size.
			int size = end - begin + 1;
			if (size > bytesRemaining) {
				size = (int) bytesRemaining;
			}

			segment.bBinary = asmType != 0;
			segment.wBegin = begin;
			segment.wEnd = end;
			segment.createMemoryBlockFromFile(size, inputStream);
			bytesRemaining -= size;
		}
	}

	/** Reads a big-endian 16 bit word (high byte first), as stored in an Oric .tap file. */
	private static int readWordBE(InputStream inputStream) throws IOException {
		int high = readByte(inputStream);
		int low = readByte(inputStream);
		return Memory.toAddress(low, high);
	}
}
