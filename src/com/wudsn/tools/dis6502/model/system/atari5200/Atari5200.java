/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model.system.atari5200;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;

import com.wudsn.tools.dis6502.Messages;
import com.wudsn.tools.dis6502.model.FileType;
import com.wudsn.tools.dis6502.model.MemoryType;
import com.wudsn.tools.dis6502.model.Segment;
import com.wudsn.tools.dis6502.model.SegmentListInserter;
import com.wudsn.tools.dis6502.model.system.ComputerSystem;
import com.wudsn.tools.dis6502.model.system.ComputerSystemType;

/**
 * The Atari 5200 computer system.
 *
 * @author Peter Dell
 */
public final class Atari5200 extends ComputerSystem {

	private static final Set<Integer> BASE_ADDRESSES = Set.of(0x0005 /* SDLSTL */, 0x0200 /* VIMIRQ */,
			0x0202 /* VVBLKI */, 0x0204 /* VVBLKD */, 0x0206 /* VDLI */, 0x0208 /* VKEYBD */, 0x020A /* VKEYPAD */,
			0x020C /* VBREAK */, 0x020E /* VBRK */, 0x0210 /* VSERIN */, 0x0212 /* VSEROR */, 0x0214 /* VSEROC */,
			0x0216 /* VTIMR1 */, 0x0218 /* VTIMR2 */, 0x021A /* VTIMR4 */, 0xD402 /* DLISTL */);

	private static final Set<Integer> VECTOR_ADDRESSES = Set.of(0x0200 /* VIMIRQ */, 0x0202 /* VVBLKI */,
			0x0204 /* VVBLKD */, 0x0206 /* VDLI */, 0x0208 /* VKEYBD */, 0x020A /* VKEYPAD */, 0x020C /* VBREAK */,
			0x020E /* VBRK */, 0x0210 /* VSERIN */, 0x0212 /* VSEROR */, 0x0214 /* VSEROC */, 0x0216 /* VTIMR1 */,
			0x0218 /* VTIMR2 */, 0x021A /* VTIMR4 */);

	public Atari5200(ComputerSystemType computerSystemType) {
		super(computerSystemType);
		returnCharacter = 0x9b;
		supportedFileTypes = List.of(FileType.RAW_FILE, FileType.ROM_IMAGE_FILE);
	}

	@Override
	public boolean isBaseAddress(int address) {
		if (BASE_ADDRESSES.contains(address)) {
			return true;
		}
		return (address & 0xFF00) == 0 && address > 0x10;
	}

	@Override
	public boolean isVectorAddress(int address) {
		return VECTOR_ADDRESSES.contains(address);
	}

	@Override
	public boolean isDisplayListVectorAddress(int address) {
		return address == 0x0005 || address == 0xD402;
	}

	@Override
	public FileType guessFileType(long fileSize, byte[] content) {
		// See https://github.com/atari800/atari800/blob/master/DOC/cart.txt
		// Bank-switched cartridges and .CAR files are currently not yet supported.
		// 4k, 8k, 16k, 32k or 40k ROM?
		if (fileSize == 0x1000L || fileSize == 0x2000L || fileSize == 0x4000L || fileSize == 0x8000L
				|| fileSize == 0xa000L) {
			return FileType.ROM_IMAGE_FILE;
		}
		return FileType.ANY_FILE;
	}

	@Override
	protected void readROMFile(SegmentListInserter segmentListInserter, InputStream inputStream, long fileSize)
			throws IOException {
		// Check that the file length is less than 32K.
		if (fileSize > 32768L) {
			// ERROR: Only 32k ROMs are supported.
			throw new IOException(Messages.E050.format());
		}

		int memorySize = (int) fileSize;

		// Read segment data.
		Segment segment = segmentListInserter.insertSegment();

		segment.wBegin = 0xC000 - memorySize;
		segment.wEnd = 0xBFFF;
		segment.bBinary = true;
		segment.createMemoryBlockFromFile(memorySize, inputStream);

		// Extract title from data and convert it to ASCII.
		// There are 20 bytes at the end of the ROM, followed by two 16-bit vectors.
		// See https://atarihq.com/danb/files/5200BIOS.txt
		// FE63 : A2 13    ldx  #$13;
		// FE65 : BD D4 FE lda  $FED4, x; Read copywrite message
		// FE68 : 9D 94 3C sta  $3C94, x; Write to display memory
		// FE6B : BD E8 BF lda  $BFE8, x; Read cart title from cartridge
		// FE6E : 9D 80 3C sta  $3C80, x; Write to display memory
		// FE71 : CA       dex;
		// FE72 : 10 F1    bpl  $FE65;
		int titleOffset = segment.memoryBlock.getSize() - 24;
		int titleSize = 20;
		segment.setType(titleOffset, MemoryType.SBYTE, titleSize);
		int vectorOffset = segment.memoryBlock.getSize() - 4;
		int vectorSize = 4;
		segment.setType(vectorOffset, MemoryType.LABEL, vectorSize);

		// Convert Atari Screen Code to ASCII.
		StringBuilder title = new StringBuilder();
		for (int i = 0; i < titleSize; i++) {
			int b = segment.getData(titleOffset + i);

			if (b < 64) {
				b += 32;
			} else if (b < 96) {
				b -= 64;
			} else if (b >= 128 && b < 128 + 64) {
				b += 32;
			} else if (b >= 128 + 64 && b < 128 + 96) {
				b -= 64;
			}
			title.append(Character.toUpperCase((char) b));
		}

		segment.title = title.toString().trim();
	}
}
