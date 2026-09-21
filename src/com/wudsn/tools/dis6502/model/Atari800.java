/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
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

import com.wudsn.tools.dis6502.Messages;

/**
 * The Atari 800 computer system: the primary/default target of this tool.
 * <p>
 * Ported from systems/atari800/Atari800.h / Atari800.cpp.
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
 * <li>{@code ReadCassetteFile} had three bugs in the C++ source, all fixed
 * upstream and matched here rather than ported faithfully-but-broken: the
 * FUJI chunk's title text was never actually skipped (only accounted for
 * in the byte budget), the 2 bytes of the title's own length field were
 * never subtracted from that budget either, and a multi-chunk segment's
 * previously-accumulated bytes were discarded (read into uninitialized
 * memory) every time the accumulation buffer grew for a new chunk. See
 * the upstream Atari800.cpp commit history for the details.</li>
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

	private static final byte[] FUJI = { 'F', 'U', 'J', 'I' };
	private static final byte[] DATA_CHUNK = { 'd', 'a', 't', 'a' };
	private static final int CAS_HEADER_SIZE = 4; // The "FUJI"/"data" name only.
	private static final int CAS_CHUNK_HEADER_SIZE = 8; // name (4) + length (2) + aux (2).

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
			throw new IOException(Messages.E051.format(String.valueOf(header)));
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
					throw new IOException(Messages.E052.format());
				}

				// Check the data size.
				int size = end - begin + 1;
				if (size > reader.getBytesRemaining()) {
					throw new IOException(Messages.E053.format(String.valueOf(reader.getBytesRemaining()), String.valueOf(size)));
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
				throw new IOException(Messages.E054.format());
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
			throw new IOException(Messages.E055.format(String.valueOf(bytesRemaining)));
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

	/**
	 * Reads a .cas tape image: a "FUJI" header (name/title, skipped) followed
	 * by zero or more chunks, of which only "data" chunks matter here. A
	 * segment can span several consecutive "data" chunks - {@code
	 * CAS_SEGMENT.segmentCount}, read from the first chunk's payload, says
	 * how many chunks belong to the segment currently being accumulated -
	 * so segment data is built up in a growing buffer across chunks and only
	 * turned into an actual {@link Segment} once that count reaches zero
	 * (matching the class-level "Design deviations" note: the C++ source had
	 * three bugs in this accumulation, all fixed here rather than preserved).
	 * <p>
	 * A new (mostly unused, if this isn't a "data" chunk) {@link Segment} is
	 * inserted on every loop iteration, matching the C++ source exactly,
	 * quirky as that looks - preserved as observed since it's unclear
	 * whether this is intentional.
	 */
	@Override
	protected void readCassetteFile(SegmentListInserter segmentListInserter, InputStream inputStream, long fileSize)
			throws IOException {
		byte[] casHeader = new byte[CAS_HEADER_SIZE];
		readFully(inputStream, casHeader);
		if (!Arrays.equals(casHeader, FUJI)) {
			throw new IOException(Messages.E056.format());
		}

		long bytesRemaining = fileSize - CAS_HEADER_SIZE; // Consumed: name (4 bytes).

		// Skip the title of the software.
		int titleLength = readWordLEUnchecked(inputStream);
		bytesRemaining -= 2; // Consumed: length (2 bytes).

		skipFully(inputStream, 2);
		bytesRemaining -= 2; // Consumed: aux (2 bytes).

		skipFully(inputStream, titleLength);
		bytesRemaining -= titleLength; // Consumed: title/description (titleLength bytes).

		// Read all the data chunks.
		boolean firstSegment = true;
		int expectedSegmentCount = 0;
		int offset = 0;
		byte[] buffer = null;

		while (bytesRemaining > CAS_CHUNK_HEADER_SIZE) {
			Segment segment = segmentListInserter.insertSegment();

			// Read a header.
			byte[] name = new byte[4];
			readFully(inputStream, name);
			int length = readWordLEUnchecked(inputStream);
			skipFully(inputStream, 2); // aux/gap - unused here.
			bytesRemaining -= CAS_CHUNK_HEADER_SIZE;

			if (Arrays.equals(name, DATA_CHUNK)) {
				// Save the previous segment.
				if (expectedSegmentCount == 0 && buffer != null && offset > 0) {
					int loadAddress = Memory.toAddress(buffer[2] & 0xFF, buffer[3] & 0xFF);

					// Allocate memory for data and for byte type.
					segment.createMemoryBlockWithSize(offset);
					segment.setData(0, buffer, 0, offset);

					if (firstSegment) {
						segment.setType(0, MemoryType.BYTE);
						segment.setType(1, MemoryType.BYTE);
						segment.setType(2, MemoryType.LABEL);
						segment.setType(3, MemoryType.LABEL);
						segment.setType(4, MemoryType.LABEL);
						segment.setType(5, MemoryType.LABEL);
					}

					// Create a new segment.
					segment.wBegin = loadAddress;
					segment.wEnd = segment.wBegin + offset - 1;
					segment.bBinary = true;

					// Forget this segment.
					firstSegment = false;
					offset = 0;
				}

				// Skip the first 3 bytes of the data (usually $55 $55 $FC).
				skipFully(inputStream, 3);

				bytesRemaining -= 4;
				length -= 4;

				// Allocate a buffer to hold the real data, preserving any data already accumulated.
				byte[] newBuffer = new byte[offset + length];
				if (buffer != null) {
					System.arraycopy(buffer, 0, newBuffer, 0, offset);
				}
				buffer = newBuffer;

				// Read data excluding the last byte (probably a checksum byte).
				readFully(inputStream, buffer, offset, length);

				offset += length;
				bytesRemaining -= length;

				// Skip the last byte of the data (probably a checksum byte).
				skipFully(inputStream, 1);

				// Check if we have a new segment.
				if (expectedSegmentCount == 0) {
					expectedSegmentCount = buffer[1] & 0xFF; // CAS_SEGMENT.segmentCount.
					if (expectedSegmentCount == 0) {
						expectedSegmentCount = 256;
					}
				}

				expectedSegmentCount--;
			}
		}

		// Save the previous segment.
		if (buffer != null && offset > 0) {
			// Check if the segment is valid.
			int unused = buffer[0] & 0xFF;
			int segmentCount = buffer[1] & 0xFF;
			int loadAddress = Memory.toAddress(buffer[2] & 0xFF, buffer[3] & 0xFF);
			int initAddress = Memory.toAddress(buffer[4] & 0xFF, buffer[5] & 0xFF);

			if (segmentCount != 0 || unused != 0 || initAddress != 0 || loadAddress != 0) {
				// Allocate memory for data and for byte type.
				Segment segment = segmentListInserter.insertSegment();
				segment.createMemoryBlockWithSize(offset);
				segment.setData(0, buffer, 0, offset);

				if (firstSegment) {
					segment.setType(0, MemoryType.BYTE);
					segment.setType(1, MemoryType.BYTE);
					segment.setType(2, MemoryType.LABEL);
					segment.setType(3, MemoryType.LABEL);
					segment.setType(4, MemoryType.LABEL);
					segment.setType(5, MemoryType.LABEL);
				}

				// Create a new segment.
				segment.wBegin = loadAddress;
				segment.wEnd = segment.wBegin + offset - 1;
				segment.bBinary = true;
			}
		}
	}

	private static void readFully(InputStream inputStream, byte[] buffer) throws IOException {
		readFully(inputStream, buffer, 0, buffer.length);
	}

	private static void readFully(InputStream inputStream, byte[] buffer, int offset, int length)
			throws IOException {
		int totalRead = 0;
		while (totalRead < length) {
			int read = inputStream.read(buffer, offset + totalRead, length - totalRead);
			if (read < 0) {
				throw new EOFException("Unexpected end of file.");
			}
			totalRead += read;
		}
	}

	private static int readWordLEUnchecked(InputStream inputStream) throws IOException {
		byte[] buffer = new byte[2];
		readFully(inputStream, buffer);
		return Memory.toAddress(buffer[0] & 0xFF, buffer[1] & 0xFF);
	}

	/** Skips exactly {@code count} bytes, falling back to reading and discarding since {@link InputStream#skip} may return less than requested. */
	private static void skipFully(InputStream inputStream, long count) throws IOException {
		long remaining = count;
		while (remaining > 0) {
			long skipped = inputStream.skip(remaining);
			if (skipped > 0) {
				remaining -= skipped;
			} else if (inputStream.read() < 0) {
				throw new EOFException("Unexpected end of file.");
			} else {
				remaining--;
			}
		}
	}

	private static void writeWordLE(OutputStream outputStream, int value) throws IOException {
		outputStream.write(value & 0xFF);
		outputStream.write((value >> 8) & 0xFF);
	}

	private static void writeSDXSymbol(String symbol, OutputStream outputStream) throws IOException {
		if (symbol.length() > SDX_SYMBOL_LEN) {
			throw new IOException(Messages.E057.format(symbol, String.valueOf(SDX_SYMBOL_LEN)));
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
				throw new IOException(Messages.E058.format(String.valueOf(bytesRemaining), String.valueOf(length)));
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
				throw new IOException(Messages.E058.format(String.valueOf(bytesRemaining), String.valueOf(size)));
			}
			segment.memoryBlock.readData(inputStream, size);
			bytesRemaining -= size;
		}
	}
}
