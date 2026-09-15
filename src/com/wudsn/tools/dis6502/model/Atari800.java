/*
 * Copyright (C) 2025 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * The Atari 800 computer system: the primary/default target of this tool.
 * <p>
 * Ported from systems/atari800/Atari800.h / Atari800.cpp. {@code
 * ReadCassetteFile} (the .cas tape format) is not ported yet: unlike the
 * rest of this file, its bookkeeping doesn't add up on inspection (it
 * subtracts a description-text length from the byte budget without ever
 * skipping that text), which looks like a genuine bug rather than a quirk
 * to preserve - porting it faithfully needs figuring out what it was
 * actually supposed to do first, which is a task of its own.
 * <p>
 * Design deviations:
 * <ul>
 * <li>The C++ source's free-standing {@code Read}/{@code ReadData}/{@code
 * ReadSDXSymbol}/{@code WriteSDXSymbol} helpers (which thread a
 * bytes-remaining counter by reference) are consolidated into a private
 * {@link BoundedReader} nested class.</li>
 * <li>{@code ReadExecutableFile}'s segment-data read
 * ({@code Segment.createMemoryBlockFromFile}) is - exactly like the C++
 * source, per its own "TODO: Introduce bounded InputStream" comment - not
 * bounds-checked against the remaining byte budget before reading, unlike
 * every other read in this class.</li>
 * </ul>
 *
 * @author Peter Dell
 */
public final class Atari800 extends ComputerSystem {

	private static final int CAR_HEADER_SIZE = 16;
	private static final int SIZE_4K = 0x1000;
	private static final int SIZE_8K = 0x2000;
	private static final int SIZE_16K = 0x4000;
	private static final int SIZE_4K_CAR = SIZE_4K + CAR_HEADER_SIZE;
	private static final int SIZE_8K_CAR = SIZE_8K + CAR_HEADER_SIZE;
	private static final int SIZE_16K_CAR = SIZE_16K + CAR_HEADER_SIZE;

	private static final int SDX_SYMBOL_LEN = 8;

	private static final Set<Integer> BASE_ADDRESSES = Set.of(0x0200 /* VDSLST */, 0x0202 /* VPRCED */,
			0x0204 /* VINTER */, 0x0206 /* VBREAK */, 0x0208 /* VKEYBD */, 0x020A /* VSERIN */, 0x020C /* VSEROR */,
			0x020E /* VSEROC */, 0x0210 /* VTIMR1 */, 0x0212 /* VTIMR2 */, 0x0214 /* VTIMR4 */, 0x0216 /* VIMIRQ */,
			0x0222 /* VVBLKI */, 0x0224 /* VVBLKD */, 0x0226 /* CDTMA1 */, 0x0228 /* CDTMA2 */, 0x0230 /* SDLSTL */,
			0x0236 /* BRKKY */, 0x0238 /* VPIRQ */, 0x02E0 /* RUNAD */, 0x02E2 /* INITAD */, 0x02E5 /* MEMTOP */,
			0x02E7 /* MEMLO */, 0x0304 /* DBUFLO */, 0x0344 /* ICBAL0 */, 0x0354 /* ICBAL1 */, 0x0364 /* ICBAL2 */,
			0x0374 /* ICBAL3 */, 0x0384 /* ICBAL4 */, 0x0394 /* ICBAL5 */, 0x03A4 /* ICBAL6 */, 0x03B4 /* ICBAL7 */,
			0xD402 /* DLISTL */);

	private static final Set<Integer> VECTOR_ADDRESSES = Set.of(0x0200 /* VDSLST */, 0x0202 /* VPRCED */,
			0x0204 /* VINTER */, 0x0206 /* VBREAK */, 0x0208 /* VKEYBD */, 0x020A /* VSERIN */, 0x020C /* VSEROR */,
			0x020E /* VSEROC */, 0x0210 /* VTIMR1 */, 0x0212 /* VTIMR2 */, 0x0214 /* VTIMR4 */, 0x0216 /* VIMIRQ */,
			0x0222 /* VVBLKI */, 0x0224 /* VVBLKD */, 0x0226 /* CDTMA1 */, 0x0228 /* CDTMA2 */, 0x0236 /* BRKKY */,
			0x0238 /* VPIRQ */, 0x02E0 /* RUNAD */, 0x02E2 /* INITAD */);

	public Atari800(ComputerSystemTypeInfo computerSystemTypeInfo) {
		super(computerSystemTypeInfo);
		returnCharacter = 0x9B;
		supportedFileTypes = List.of(FileType.RAW_FILE, FileType.EXECUTABLE_FILE, FileType.ROM_IMAGE_FILE,
				FileType.CASSETTE_IMAGE_FILE, FileType.DISK_IMAGE_EXECUTABLE_FILE, FileType.DISK_IMAGE_BOOT_SECTORS,
				FileType.DISK_IMAGE_SECTORS);
	}

	@Override
	public boolean isBaseAddress(int address) {
		if (BASE_ADDRESSES.contains(address)) {
			return true;
		}
		return (address & 0xFF00) == 0;
	}

	@Override
	public boolean isVectorAddress(int address) {
		return VECTOR_ADDRESSES.contains(address);
	}

	@Override
	public boolean isDisplayListVectorAddress(int address) {
		return address == 0x0230 || address == 0xD402;
	}

	@Override
	public FileType guessFileType(long fileSize, byte[] content) {
		// DOS 2.5 ($ffff) or SDX header ($fffa, $fffe)?
		int headerValue = (content[0] & 0xFF) | ((content[1] & 0xFF) << 8);
		FileHeader header = FileHeader.valueOf(headerValue);
		if (header == FileHeader.ATARI_BINARY || header == FileHeader.SDX_FIXED_BLK
				|| header == FileHeader.SDX_RELOC_BLK) {
			return FileType.EXECUTABLE_FILE;
		}

		// 4k, 8k or 16k ROM image?
		if (fileSize == SIZE_4K || fileSize == SIZE_8K || fileSize == SIZE_16K) {
			return FileType.ROM_IMAGE_FILE;
		}
		if (fileSize == SIZE_4K_CAR || fileSize == SIZE_8K_CAR || fileSize == SIZE_16K_CAR) {
			return FileType.ROM_IMAGE_FILE;
		}

		// Cassette image?
		if (content[0] == 'F' && content[1] == 'U' && content[2] == 'J' && content[3] == 'I') {
			return FileType.CASSETTE_IMAGE_FILE;
		}

		// Disk image (SD, MD) without (XFD) and with (ATR) header.
		if (fileSize == 92160L || fileSize == 133120L || fileSize == 92176L || fileSize == 133136L
				|| ((content[0] & 0xFF) == 0x96 && (content[1] & 0xFF) == 0x02 && fileSize <= 133136L)) {
			return FileType.DISK_IMAGE_BOOT_SECTORS;
		}

		return FileType.UNKNOWN_FILE;
	}

	/** Allocates a 1 byte segment to mark it as being used. */
	private static void fakeSegment(Segment segment) {
		segment.createMemoryBlockWithSize(1);
	}

	/** Loads a fix-up list from an SDX binary file, reading byte after byte until an end code ({@link FixupType#END}) is found. */
	private static void loadFixUps(Segment segment, BoundedReader reader) throws IOException {
		int bufferSize = 8192; // Unknown size upfront, so allocate enough for fix-up data.
		byte[] buffer = new byte[bufferSize];
		int offset = 0;

		int b = reader.readByte();
		buffer[offset++] = (byte) b;

		while (b != FixupType.END.getValue() && offset < bufferSize - 3) {
			// Some bytes have parameters. Read them.
			if (b == FixupType.SET_BLOCK_NUM.getValue()) {
				b = reader.readByte();
				buffer[offset++] = (byte) b;
			} else if (b == FixupType.SET_BLOCK_ADDR.getValue()) {
				b = reader.readByte();
				buffer[offset++] = (byte) b;
				b = reader.readByte();
				buffer[offset++] = (byte) b;
			}

			// Read the next byte.
			b = reader.readByte();
			buffer[offset++] = (byte) b;
		}

		segment.wBegin = 0;
		segment.wEnd = offset - 1;
		segment.createMemoryBlockFromBeginToEnd();
		segment.setData(0, buffer, 0, offset);
	}

	@Override
	protected void readExecutableFile(SegmentListInserter segmentListInserter, InputStream inputStream, long fileSize)
			throws IOException {
		BoundedReader reader = new BoundedReader(inputStream, fileSize);

		// Check that the file begins with a known header.
		FileHeader header = FileHeader.valueOf(reader.readWordLE());
		if (header != FileHeader.ATARI_BINARY && header != FileHeader.SDX_FIXED_BLK
				&& header != FileHeader.SDX_SYM_REQUIRED && header != FileHeader.SDX_SYM_DEFINED
				&& header != FileHeader.SDX_FIX_UP_BLK && header != FileHeader.SDX_RELOC_BLK) {
			throw new IOException("Unsupported file header " + header + ".");
		}

		// Read all segments.
		boolean firstSegment = true;
		FileHeader previousHeader = FileHeader.RAW;
		int begin = 0xFFFF;

		while (reader.getBytesRemaining() > 0) {
			Segment segment = segmentListInserter.insertSegment();

			// Read header depending on the type.
			switch (header) {
			case SDX_RELOC_BLK: {
				// Read block number definition.
				int sdxBlockNumber = reader.readByte();
				// Read control byte.
				int sdxControlByte = reader.readByte();
				// Read offset from the beginning of the memory block.
				begin = reader.readWordLE();
				// Read size to read or allocate.
				int blockSize = reader.readWordLE();
				int end = begin + blockSize - 1;

				segment.setHeader(header);
				segment.wBegin = begin;
				segment.wEnd = end;
				segment.bSDXBlockNumber = sdxBlockNumber;
				segment.bSDXControlByte = sdxControlByte;
				segment.bBinary = false;

				// Is there any data after the header?
				if (segment.isSDXRelocBlkWithData()) {
					segment.createMemoryBlockFromBeginToEnd();
					reader.readIntoMemoryBlock(segment);
					segment.bBinary = true;
				} else {
					// Mark the segment as being used by allocating a 1 byte buffer.
					fakeSegment(segment);
				}
				break;
			}

			case SDX_FIX_UP_BLK: {
				// Read block number to fix up.
				int sdxBlockNumber = reader.readByte();
				// Read length (never seen values other than 0!).
				int sdxFixUpSize = reader.readWordLE();

				segment.setHeader(header);
				segment.wBegin = 0; // wEnd will be set by loadFixUps.
				segment.bSDXBlockNumber = sdxBlockNumber;
				segment.wSDXFixUpSize = sdxFixUpSize;
				segment.bBinary = false;
				loadFixUps(segment, reader);
				break;
			}

			case SDX_SYM_REQUIRED: {
				String sdxSymbol = reader.readSDXSymbol();
				// Read length (never seen values other than 0!).
				int sdxFixUpSize = reader.readWordLE();

				segment.setHeader(header);
				segment.wBegin = 0; // wEnd will be set by loadFixUps.
				segment.wSDXFixUpSize = sdxFixUpSize;
				segment.bBinary = false;
				segment.sdxSymbol = sdxSymbol;

				loadFixUps(segment, reader);
				break;
			}

			case SDX_SYM_DEFINED: {
				// Read block number to fix up.
				int sdxBlockNumber = reader.readByte();
				begin = reader.readWordLE();
				String sdxSymbol = reader.readSDXSymbol();

				segment.setHeader(header);
				segment.wBegin = begin;
				segment.wEnd = begin;
				segment.bSDXBlockNumber = sdxBlockNumber;
				segment.bBinary = false;
				segment.sdxSymbol = sdxSymbol;

				// Mark the segment as being used by allocating a 1 byte buffer.
				fakeSegment(segment);
				break;
			}

			case ATARI_BINARY:
			case SDX_FIXED_BLK:
				// Read the segment start address.
				if (begin == 0xFFFF) {
					begin = reader.readWordLE();
				}
				previousHeader = header;
				// Falls through intentionally.

			default: {
				// Read the segment end address.
				FileHeader beginHeader = FileHeader.valueOf(begin);

				if (!firstSegment && (beginHeader == FileHeader.ATARI_BINARY
						|| beginHeader == FileHeader.SDX_FIXED_BLK)) {
					header = beginHeader;
					begin = reader.readWordLE();
				} else {
					header = previousHeader;
				}
				int end = reader.readWordLE();

				if (end < begin) {
					throw new IOException("Segment end address is lower than segment start address.");
				}

				// Check the data size.
				int size = end - begin + 1;
				if (size > reader.getBytesRemaining()) {
					throw new IOException("Stream has " + reader.getBytesRemaining()
							+ " bytes left and is too short for segment of size " + size + ".");
				}

				segment.setHeader(header);
				segment.wBegin = begin;
				segment.wEnd = end;
				segment.bBinary = true;
				segment.createMemoryBlockFromFile(size, inputStream);
				reader.consumeUnchecked(size); // TODO: Introduce bounded InputStream.

				// Change the segment type if the segment is 02E0 or 02E2.
				if ((begin == 0x2E0 && (size == 2 || size == 4)) || (begin == 0x2E2 && size == 2)) {
					segment.setType(0, MemoryType.LABEL, size);
				}

				previousHeader = header;
				break;
			}
			}

			if (reader.getBytesRemaining() == 0) {
				return;
			}

			// Read the next segment header (assume it is the begin address with no magic header).
			begin = reader.readWordLE();

			// If the begin address is a magic header, transfer it to header.
			FileHeader beginHeader = FileHeader.valueOf(begin);
			if (beginHeader == FileHeader.ATARI_BINARY || beginHeader == FileHeader.SDX_FIXED_BLK
					|| beginHeader == FileHeader.SDX_SYM_REQUIRED || beginHeader == FileHeader.SDX_SYM_DEFINED
					|| beginHeader == FileHeader.SDX_FIX_UP_BLK || beginHeader == FileHeader.SDX_RELOC_BLK) {
				header = beginHeader;
				begin = 0xFFFF;
			} else {
				header = previousHeader;
			}

			firstSegment = false;
		}
	}

	@Override
	public void writeExecutableFile(SegmentList segmentList, int firstSegmentIndex, boolean writeHeader,
			OutputStream outputStream) throws IOException {
		int firstSegment = firstSegmentIndex != SegmentList.NO_SEGMENT_INDEX ? firstSegmentIndex : 0;
		int lastSegment = firstSegmentIndex != SegmentList.NO_SEGMENT_INDEX ? firstSegmentIndex + 1
				: segmentList.getCount();
		boolean writeHeaderLocal = firstSegmentIndex == SegmentList.NO_SEGMENT_INDEX ? true : writeHeader;

		FileHeader lastHeader = FileHeader.RAW;

		for (int i = firstSegment; i < lastSegment; i++) {
			Segment segment = segmentList.getSegment(i);
			if (segment.isEmpty()) {
				continue;
			}

			boolean hasData = true;

			if (writeHeaderLocal) {
				FileHeader header = segment.getHeader();
				if (header != lastHeader || header != FileHeader.ATARI_BINARY) {
					writeWordLE(outputStream, header.getValue());
				}

				switch (header) {
				case SDX_RELOC_BLK:
					outputStream.write(segment.bSDXBlockNumber);
					outputStream.write(segment.bSDXControlByte);
					writeWordLE(outputStream, segment.wBegin);
					writeWordLE(outputStream, segment.getSize());
					hasData = segment.isSDXRelocBlkWithData();
					break;

				case SDX_FIX_UP_BLK:
					outputStream.write(segment.bSDXBlockNumber);
					writeWordLE(outputStream, segment.wSDXFixUpSize);
					break;

				case SDX_SYM_REQUIRED:
					writeSDXSymbol(segment.sdxSymbol, outputStream);
					writeWordLE(outputStream, segment.wSDXFixUpSize);
					break;

				case SDX_SYM_DEFINED:
					outputStream.write(segment.bSDXBlockNumber);
					writeWordLE(outputStream, segment.wBegin);
					writeSDXSymbol(segment.sdxSymbol, outputStream);
					break;

				case ATARI_BINARY:
				case SDX_FIXED_BLK:
				default:
					writeWordLE(outputStream, segment.wBegin);
					writeWordLE(outputStream, segment.wEnd);
					break;
				}

				lastHeader = header;
			}
			if (hasData) {
				segment.memoryBlock.writeData(outputStream);
			}
		}
	}

	@Override
	protected void readROMFile(SegmentListInserter segmentListInserter, InputStream inputStream, long fileSize)
			throws IOException {
		int base4K = 0xB000;
		int end4K = 0xBFFF;
		int base8K = 0xA000;
		int end8K = 0xBFFF;
		int base16K = 0x8000;
		int end16K = 0xBFFF;

		long bytesRemaining = fileSize;

		if (bytesRemaining == SIZE_8K_CAR || bytesRemaining == SIZE_16K_CAR) {
			byte[] carHeader = new byte[CAR_HEADER_SIZE];
			readFully(inputStream, carHeader);
			if (carHeader[0] != 'C' || carHeader[1] != 'A' || carHeader[2] != 'R' || carHeader[3] != 'T') {
				throw new IOException("Invalid stream header. Stream is not a CART stream.");
			}
			bytesRemaining -= CAR_HEADER_SIZE;
		}

		// Read only 4K, 8K, and 16K cartridges.
		int begin;
		int end;
		if (bytesRemaining == SIZE_4K) {
			begin = base4K;
			end = end4K;
		} else if (bytesRemaining == SIZE_8K) {
			begin = base8K;
			end = end8K;
		} else if (bytesRemaining == SIZE_16K) {
			begin = base16K;
			end = end16K;
		} else {
			throw new IOException("Unsupported cartridge size " + bytesRemaining + ".");
		}

		// Read segment data.
		Segment segment = segmentListInserter.insertSegment();

		segment.wBegin = begin;
		segment.wEnd = end;
		segment.bBinary = true;
		int size = segment.wEnd - segment.wBegin + 1;
		segment.createMemoryBlockFromFile(size, inputStream);

		// Change the type of the last 6 bytes.
		// TODO: Put this into a method for all Atari ROMs.
		int offset = (int) bytesRemaining - 6;
		segment.setType(offset++, MemoryType.LABEL);
		segment.setType(offset++, MemoryType.LABEL);
		segment.setType(offset++, MemoryType.BYTE);
		segment.setType(offset++, MemoryType.BYTE);
		segment.setType(offset++, MemoryType.LABEL);
		segment.setType(offset++, MemoryType.LABEL);
	}

	private static void readFully(InputStream inputStream, byte[] buffer) throws IOException {
		int totalRead = 0;
		while (totalRead < buffer.length) {
			int read = inputStream.read(buffer, totalRead, buffer.length - totalRead);
			if (read < 0) {
				throw new EOFException("Unexpected end of file.");
			}
			totalRead += read;
		}
	}

	private static void writeWordLE(OutputStream outputStream, int value) throws IOException {
		outputStream.write(value & 0xFF);
		outputStream.write((value >> 8) & 0xFF);
	}

	private static void writeSDXSymbol(String symbol, OutputStream outputStream) throws IOException {
		if (symbol.length() > SDX_SYMBOL_LEN) {
			throw new IOException(
					"Length of SDX symbol '" + symbol + "' exceeds maximum length " + SDX_SYMBOL_LEN + ".");
		}
		byte[] buffer = new byte[SDX_SYMBOL_LEN];
		Arrays.fill(buffer, (byte) ' ');
		for (int i = 0; i < symbol.length(); i++) {
			buffer[i] = (byte) symbol.charAt(i);
		}
		outputStream.write(buffer);
	}

	/**
	 * Reads little-endian words/bytes/SDX symbols while tracking a
	 * bytes-remaining budget, matching the C++ source's free-standing
	 * {@code Read}/{@code ReadData}/{@code ReadSDXSymbol} helpers (which
	 * thread the counter through a reference parameter instead).
	 */
	private static final class BoundedReader {

		private final InputStream inputStream;
		private long bytesRemaining;

		BoundedReader(InputStream inputStream, long bytesRemaining) {
			this.inputStream = inputStream;
			this.bytesRemaining = bytesRemaining;
		}

		long getBytesRemaining() {
			return bytesRemaining;
		}

		/** Marks {@code size} bytes as consumed without reading or bounds-checking them. */
		void consumeUnchecked(long size) {
			bytesRemaining -= size;
		}

		private void checkedReadFully(byte[] buffer, int length) throws IOException {
			if (length > bytesRemaining) {
				throw new IOException("Computed remaining length of stream of " + bytesRemaining
						+ " is smaller than requested amount of " + length + " to read.");
			}
			Atari800.readFully(inputStream, buffer);
			bytesRemaining -= length;
		}

		int readByte() throws IOException {
			byte[] buffer = new byte[1];
			checkedReadFully(buffer, 1);
			return buffer[0] & 0xFF;
		}

		int readWordLE() throws IOException {
			int low = readByte();
			int high = readByte();
			return Memory.toAddress(low, high);
		}

		/** Reads a fixed-length SDX symbol, trimming its trailing spaces. */
		String readSDXSymbol() throws IOException {
			byte[] buffer = new byte[SDX_SYMBOL_LEN];
			checkedReadFully(buffer, SDX_SYMBOL_LEN);
			StringBuilder result = new StringBuilder();
			for (int i = 0; i < SDX_SYMBOL_LEN; i++) {
				char c = (char) (buffer[i] & 0xFF);
				if (c != ' ') {
					result.append(c);
				} else {
					break;
				}
			}
			return result.toString();
		}

		/** Bounds-checked fill of an already-sized segment's memory block. */
		void readIntoMemoryBlock(Segment segment) throws IOException {
			int size = segment.getSize();
			if (size > bytesRemaining) {
				throw new IOException("Computed remaining length of stream of " + bytesRemaining
						+ " is smaller than requested amount of " + size + " to read.");
			}
			segment.memoryBlock.readData(inputStream, size);
			bytesRemaining -= size;
		}
	}
}
