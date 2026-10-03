/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model.system.atari;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import com.wudsn.tools.base.atari.CartridgeFileUtility;
import com.wudsn.tools.base.atari.CartridgeType;
import com.wudsn.tools.base.atari.Messages;
import com.wudsn.tools.base.atari.Platform;
import com.wudsn.tools.dis6502.model.Assert;
import com.wudsn.tools.dis6502.model.FileType;
import com.wudsn.tools.dis6502.model.MemoryType;
import com.wudsn.tools.dis6502.model.Segment;
import com.wudsn.tools.dis6502.model.SegmentList;
import com.wudsn.tools.dis6502.model.SegmentListInserter;
import com.wudsn.tools.dis6502.model.system.ComputerSystem;
import com.wudsn.tools.dis6502.model.system.ComputerSystemFactory;
import com.wudsn.tools.dis6502.model.system.ComputerSystemType;

/**
 * Imports synthetic cartridge images through {@link ComputerSystem#readFile}
 * and checks every segment's address range and content against hand-computed
 * values: every 4 KB page of an image starts with its page number as a word,
 * so each segment's file offset can be checked exactly, also for the mixed 4
 * and 8 KB banks of Bounty Bob. Cartridge type texts and the reading errors
 * come from WUDSN Base's localized repositories, so expected titles and
 * messages use {@link CartridgeType#getText()} and {@link Messages}.
 *
 * @author Peter Dell
 */
public final class AtariCartridgeReaderTest {

	private static final ComputerSystemFactory FACTORY = new ComputerSystemFactory();
	private static final ComputerSystem ATARI800 = FACTORY.getComputerSystem(ComputerSystemType.ATARI800);
	private static final ComputerSystem ATARI5200 = FACTORY.getComputerSystem(ComputerSystemType.ATARI5200);
	private static final ComputerSystem C64 = FACTORY.getComputerSystem(ComputerSystemType.C64);

	private AtariCartridgeReaderTest() {
	}

	public static void testAtariCartridgeReader() throws IOException {
		testDetection();
		testCandidates();
		testAtari800();
		testAtari800BankSwitching();
		testAtari5200();
		testErrors();
	}

	private static void testDetection() {
		// Raw images: only the standard sizes of each platform.
		Assert.boolEquals(detect(Platform.ATARI_800, 0x0800) == CartridgeType.CARTRIDGE_STD_2, true);
		Assert.boolEquals(detect(Platform.ATARI_800, 0x2000) == CartridgeType.CARTRIDGE_STD_8, true);
		Assert.boolEquals(detect(Platform.ATARI_800, 0x8000) == CartridgeType.UNKNOWN, true);
		Assert.boolEquals(detect(Platform.ATARI_5200, 0x1000) == CartridgeType.CARTRIDGE_5200_4, true);
		Assert.boolEquals(detect(Platform.ATARI_5200, 0x8000) == CartridgeType.CARTRIDGE_5200_32, true);
		Assert.boolEquals(detect(Platform.ATARI_5200, 0xA000) == CartridgeType.CARTRIDGE_5200_40, true);

		// CART header: its type wins, whatever the size.
		byte[] header = createCartridgeFile(CartridgeType.CARTRIDGE_WILL_64);
		Assert.boolEquals(CartridgeReader.detectCartridgeType(Platform.ATARI_800, header.length,
				header) == CartridgeType.CARTRIDGE_WILL_64, true);
		header[7] = (byte) 200; // No such type number.
		Assert.boolEquals(CartridgeReader.detectCartridgeType(Platform.ATARI_800, header.length,
				header) == CartridgeType.UNKNOWN, true);

		// File types.
		Assert.boolEquals(ATARI800.guessFileType(0x0800, new byte[16]) == FileType.ROM_IMAGE_FILE, true);
		Assert.boolEquals(ATARI800.guessFileType(0x3000, new byte[16]) == FileType.ANY_FILE, true);
		// A raw image of a size that cartridge types have is a ROM image; the user chooses the type.
		Assert.boolEquals(ATARI800.guessFileType(0x8000, new byte[16]) == FileType.ROM_IMAGE_FILE, true);
		// A 4 KB CART file was detected before step 1, but could not be read.
		byte[] std4 = createCartridgeFile(CartridgeType.CARTRIDGE_STD_4);
		Assert.boolEquals(ATARI800.guessFileType(std4.length, std4) == FileType.ROM_IMAGE_FILE, true);
		// A CART file of the other platform is a ROM image, so that reading it explains the mismatch.
		byte[] std32 = createCartridgeFile(CartridgeType.CARTRIDGE_5200_32);
		Assert.boolEquals(ATARI800.guessFileType(std32.length, std32) == FileType.ROM_IMAGE_FILE, true);
		// A raw 40 KB 5200 image is Bounty Bob.
		Assert.boolEquals(ATARI5200.guessFileType(0xA000, new byte[16]) == FileType.ROM_IMAGE_FILE, true);

		// Candidates for a raw 64 KB image, sorted by text ignoring case ("aDawliah" first).
		List<CartridgeType> candidates = CartridgeReader.getCandidateTypes(Platform.ATARI_800, 0x10000);
		List<CartridgeType> expected = Arrays.asList(CartridgeType.CARTRIDGE_WILL_64, CartridgeType.CARTRIDGE_EXP_64,
				CartridgeType.CARTRIDGE_DIAMOND_64, CartridgeType.CARTRIDGE_SDX_64,
				CartridgeType.CARTRIDGE_TURBOSOFT_64, CartridgeType.CARTRIDGE_ADAWLIAH_64,
				CartridgeType.CARTRIDGE_MEGA_64, CartridgeType.CARTRIDGE_XEGS_64, CartridgeType.CARTRIDGE_SWXEGS_64,
				CartridgeType.CARTRIDGE_XEGS_8F_64, CartridgeType.CARTRIDGE_ATRAX_SDX_64);
		Assert.longEquals(candidates.size(), expected.size());
		Assert.boolEquals(candidates.containsAll(expected), true);
		Assert.boolEquals(candidates.get(0) == CartridgeType.CARTRIDGE_ADAWLIAH_64, true);
		for (int i = 1; i < candidates.size(); i++) {
			Assert.boolEquals(candidates.get(i - 1).getText().compareToIgnoreCase(candidates.get(i).getText()) <= 0, true);
		}
		Assert.boolEquals(CartridgeReader.getCandidateTypes(Platform.ATARI_5200, 0x10000)
				.equals(List.of(CartridgeType.CARTRIDGE_5200_SUPER_64)), true);
	}

	/** The types offered in the cartridge type dialog, and reading a raw image as the chosen type. */
	private static void testCandidates() throws IOException {
		// Raw 64 KB: every importable 64 KB type of the platform.
		List<CartridgeType> candidates = ATARI800.getCartridgeTypeCandidates(0x10000, new byte[16]);
		Assert.boolEquals(candidates.equals(CartridgeReader.getCandidateTypes(Platform.ATARI_800, 0x10000)), true);
		Assert.longEquals(candidates.size(), 11);
		Assert.boolEquals(ATARI5200.getCartridgeTypeCandidates(0x10000, new byte[16])
				.equals(List.of(CartridgeType.CARTRIDGE_5200_SUPER_64)), true);
		// One candidate is still a choice: the image may be plain data.
		Assert.boolEquals(ATARI800.getCartridgeTypeCandidates(0xA000, new byte[16])
				.equals(List.of(CartridgeType.CARTRIDGE_BBSB_40)), true);

		// No choice: the type is known from a CART header or a standard size, or no type has the size.
		byte[] file = createCartridgeFile(CartridgeType.CARTRIDGE_WILL_64);
		Assert.boolEquals(ATARI800.getCartridgeTypeCandidates(file.length, file).isEmpty(), true);
		Assert.boolEquals(ATARI800.getCartridgeTypeCandidates(0x2000, new byte[16]).isEmpty(), true);
		Assert.boolEquals(ATARI800.getCartridgeTypeCandidates(0x3000, new byte[16]).isEmpty(), true);
		// The!Cart 32 MB is larger than the import limit.
		Assert.boolEquals(ATARI800.getCartridgeTypeCandidates(0x2000000, new byte[16]).isEmpty(), true);
		// Systems without cartridges.
		Assert.boolEquals(C64.getCartridgeTypeCandidates(0x10000, new byte[16]).isEmpty(), true);

		// Raw 64 KB read as the chosen XEGS 64 KB: banks 0-6 at $8000, bank 7 at $A000.
		SegmentList segmentList = read(ATARI800, createContent(0x10000), CartridgeType.CARTRIDGE_XEGS_64);
		Assert.longEquals(segmentList.getCount(), 8);
		assertSegment(segmentList.getSegment(6), 0x8000, 0x9FFF, 0xC000);
		assertSegment(segmentList.getSegment(7), 0xA000, 0xBFFF, 0xE000);
		assertCartridgeHeaderTyped(segmentList.getSegment(7));
		// The same image read as the chosen Williams 64 KB: 8 banks at $A000.
		segmentList = read(ATARI800, createContent(0x10000), CartridgeType.CARTRIDGE_WILL_64);
		assertSegment(segmentList.getSegment(6), 0xA000, 0xBFFF, 0xC000);
	}

	private static void testAtari800() throws IOException {
		// Raw 8 KB: one segment without title and prefix, cartridge header typed.
		SegmentList segmentList = read(ATARI800, createContent(0x2000));
		Assert.longEquals(segmentList.getCount(), 1);
		Segment segment = segmentList.getSegment(0);
		assertSegment(segment, 0xA000, 0xBFFF, 0);
		Assert.stringEquals(segment.title, "");
		Assert.stringEquals(segment.labelPrefix, "");
		assertCartridgeHeaderTyped(segment);

		// 4 KB CART file: at $B000.
		segmentList = read(ATARI800, createCartridgeFile(CartridgeType.CARTRIDGE_STD_4));
		Assert.longEquals(segmentList.getCount(), 1);
		assertSegment(segmentList.getSegment(0), 0xB000, 0xBFFF, 0);
		assertCartridgeHeaderTyped(segmentList.getSegment(0));

		// Right slot: the cartridge header is at $9FFA. Low bank: not in the right slot, no header typed.
		segmentList = read(ATARI800, createCartridgeFile(CartridgeType.CARTRIDGE_RIGHT_8));
		assertSegment(segmentList.getSegment(0), 0x8000, 0x9FFF, 0);
		assertCartridgeHeaderTyped(segmentList.getSegment(0));
		segmentList = read(ATARI800, createCartridgeFile(CartridgeType.CARTRIDGE_LOW_BANK_8));
		assertSegment(segmentList.getSegment(0), 0x8000, 0x9FFF, 0);
		Assert.boolEquals(segmentList.getSegment(0).isType(0x1FFA, MemoryType.LABEL), false);

		// Williams 64 KB: 8 banks of 8 KB at $A000, bank 0 initial.
		CartridgeType cartridgeType = CartridgeType.CARTRIDGE_WILL_64;
		segmentList = read(ATARI800, createCartridgeFile(cartridgeType));
		Assert.longEquals(segmentList.getCount(), 8);
		for (int bank = 0; bank < 8; bank++) {
			segment = segmentList.getSegment(bank);
			assertSegment(segment, 0xA000, 0xBFFF, bank * 0x2000);
			Assert.stringEquals(segment.labelPrefix, "B" + bank + "_");
			Assert.stringEquals(segment.title,
					cartridgeType.getText() + " - bank " + bank + (bank == 0 ? " (initial)" : ""));
		}
		assertCartridgeHeaderTyped(segmentList.getSegment(0));
		Assert.boolEquals(segmentList.getSegment(1).isType(0x1FFA, MemoryType.LABEL), false);

		// Atarimax 1 MB (old): 128 banks, bank 127 initial.
		segmentList = read(ATARI800, createCartridgeFile(CartridgeType.CARTRIDGE_ATMAX_1024));
		Assert.longEquals(segmentList.getCount(), 128);
		assertSegment(segmentList.getSegment(127), 0xA000, 0xBFFF, 127 * 0x2000);
		Assert.boolEquals(segmentList.getSegment(127).title.endsWith(" - bank 127 (initial)"), true);
		Assert.boolEquals(segmentList.getSegment(0).title.endsWith(" - bank 0"), true);
		assertCartridgeHeaderTyped(segmentList.getSegment(127));

		// Flash MegaCart 4 MB: 256 banks of 16 KB at $8000, bank 254 initial.
		segmentList = read(ATARI800, createCartridgeFile(CartridgeType.CARTRIDGE_MEGA_4096));
		Assert.longEquals(segmentList.getCount(), 256);
		assertSegment(segmentList.getSegment(255), 0x8000, 0xBFFF, 255 * 0x4000);
		Assert.boolEquals(segmentList.getSegment(254).title.endsWith(" (initial)"), true);
		assertCartridgeHeaderTyped(segmentList.getSegment(254));
	}

	private static void testAtari800BankSwitching() throws IOException {
		// XEGS 128 KB: banks 0-14 switched at $8000, bank 15 fixed at $A000 and initial.
		SegmentList segmentList = read(ATARI800, createCartridgeFile(CartridgeType.CARTRIDGE_XEGS_128));
		Assert.longEquals(segmentList.getCount(), 16);
		for (int bank = 0; bank < 15; bank++) {
			assertSegment(segmentList.getSegment(bank), 0x8000, 0x9FFF, bank * 0x2000);
		}
		assertSegment(segmentList.getSegment(15), 0xA000, 0xBFFF, 15 * 0x2000);
		Assert.boolEquals(segmentList.getSegment(15).title.endsWith(" - bank 15 (initial)"), true);
		assertCartridgeHeaderTyped(segmentList.getSegment(15));

		// DB 32 KB: banks 0-2 at $8000, bank 3 fixed at $A000.
		segmentList = read(ATARI800, createCartridgeFile(CartridgeType.CARTRIDGE_DB_32));
		Assert.longEquals(segmentList.getCount(), 4);
		assertSegment(segmentList.getSegment(2), 0x8000, 0x9FFF, 0x4000);
		assertSegment(segmentList.getSegment(3), 0xA000, 0xBFFF, 0x6000);
		assertCartridgeHeaderTyped(segmentList.getSegment(3));

		// OSS one chip 16 KB (M091): bank 0 fixed at $B000, banks 1-3 at $A000.
		segmentList = read(ATARI800, createCartridgeFile(CartridgeType.CARTRIDGE_OSS_M091_16));
		Assert.longEquals(segmentList.getCount(), 4);
		assertSegment(segmentList.getSegment(0), 0xB000, 0xBFFF, 0);
		for (int bank = 1; bank < 4; bank++) {
			assertSegment(segmentList.getSegment(bank), 0xA000, 0xAFFF, bank * 0x1000);
		}
		assertCartridgeHeaderTyped(segmentList.getSegment(0));

		// OSS two chip 16 KB (034M): banks 0-2 at $A000, bank 3 fixed at $B000.
		segmentList = read(ATARI800, createCartridgeFile(CartridgeType.CARTRIDGE_OSS_034M_16));
		assertSegment(segmentList.getSegment(3), 0xB000, 0xBFFF, 0x3000);
		assertCartridgeHeaderTyped(segmentList.getSegment(3));

		// Bounty Bob Strikes Back: 4 KB banks 0-3 at $8000 and 4-7 at $9000, the last 8 KB fixed at $A000.
		segmentList = read(ATARI800, createCartridgeFile(CartridgeType.CARTRIDGE_BBSB_40));
		Assert.longEquals(segmentList.getCount(), 9);
		for (int bank = 0; bank < 4; bank++) {
			assertSegment(segmentList.getSegment(bank), 0x8000, 0x8FFF, bank * 0x1000);
			assertSegment(segmentList.getSegment(bank + 4), 0x9000, 0x9FFF, (bank + 4) * 0x1000);
		}
		assertSegment(segmentList.getSegment(8), 0xA000, 0xBFFF, 0x8000);
		assertCartridgeHeaderTyped(segmentList.getSegment(8));

		// SIC! 128 KB: 8 KB banks alternating between $8000 and $A000, bank 1 initial.
		segmentList = read(ATARI800, createCartridgeFile(CartridgeType.CARTRIDGE_SIC_128));
		Assert.longEquals(segmentList.getCount(), 16);
		for (int bank = 0; bank < 16; bank++) {
			int address = bank % 2 == 0 ? 0x8000 : 0xA000;
			assertSegment(segmentList.getSegment(bank), address, address + 0x1FFF, bank * 0x2000);
		}
		assertCartridgeHeaderTyped(segmentList.getSegment(1));

		// Atrax SDX 64 KB: the interleaved image is decoded; then like SpartaDOS X 64 KB.
		CartridgeType cartridgeType = CartridgeType.CARTRIDGE_ATRAX_SDX_64;
		byte[] encoded = CartridgeFileUtility.encodeAtraxContent(cartridgeType, createContent(0x10000));
		segmentList = read(ATARI800,
				concat(CartridgeFileUtility.createCartridgeHeaderWithCheckSum(cartridgeType.getNumericId(), encoded),
						encoded));
		Assert.longEquals(segmentList.getCount(), 8);
		for (int bank = 0; bank < 8; bank++) {
			assertSegment(segmentList.getSegment(bank), 0xA000, 0xBFFF, bank * 0x2000);
		}
	}

	private static void testAtari5200() throws IOException {
		// Raw 4 KB: at $B000, title and vectors typed, title taken from $BFE8.
		byte[] content = createContent(0x1000);
		byte[] title = { 0x33, 0x34, 0x21, 0x32 }; // "STAR" in screen codes.
		System.arraycopy(title, 0, content, 0x1000 - 24, title.length);
		SegmentList segmentList = read(ATARI5200, content);
		Assert.longEquals(segmentList.getCount(), 1);
		Segment segment = segmentList.getSegment(0);
		Assert.longEquals(segment.wBegin, 0xB000);
		Assert.longEquals(segment.wEnd, 0xBFFF);
		Assert.boolEquals(segment.isType(0x1000 - 24, MemoryType.SBYTE), true);
		Assert.boolEquals(segment.isType(0x1000 - 4, MemoryType.LABEL), true);
		Assert.boolEquals(segment.title.startsWith("STAR"), true);

		// Raw 16 KB and 32 KB, as before.
		assertSegment(read(ATARI5200, createContent(0x4000)).getSegment(0), 0x8000, 0xBFFF, 0);
		assertSegment(read(ATARI5200, createContent(0x8000)).getSegment(0), 0x4000, 0xBFFF, 0);

		// Super Cart 128 KB: 4 banks of 32 KB at $4000, bank 3 initial.
		segmentList = read(ATARI5200, createCartridgeFile(CartridgeType.CARTRIDGE_5200_SUPER_128));
		Assert.longEquals(segmentList.getCount(), 4);
		for (int bank = 0; bank < 4; bank++) {
			assertSegment(segmentList.getSegment(bank), 0x4000, 0xBFFF, bank * 0x8000);
		}
		Assert.boolEquals(segmentList.getSegment(3).isType(0x8000 - 4, MemoryType.LABEL), true);
		Assert.boolEquals(segmentList.getSegment(0).isType(0x8000 - 4, MemoryType.LABEL), false);

		// Raw 40 KB Bounty Bob: 4 KB banks 0-3 at $4000 and 4-7 at $5000, the last 8 KB at $A000.
		segmentList = read(ATARI5200, createContent(0xA000));
		Assert.longEquals(segmentList.getCount(), 9);
		assertSegment(segmentList.getSegment(3), 0x4000, 0x4FFF, 0x3000);
		assertSegment(segmentList.getSegment(4), 0x5000, 0x5FFF, 0x4000);
		assertSegment(segmentList.getSegment(8), 0xA000, 0xBFFF, 0x8000);
		Assert.boolEquals(segmentList.getSegment(8).isType(0x2000 - 4, MemoryType.LABEL), true);

		// Two chip 16 KB: the first 8 KB at $4000, the second at $A000 with the vectors.
		segmentList = read(ATARI5200, createCartridgeFile(CartridgeType.CARTRIDGE_5200_EE_16));
		Assert.longEquals(segmentList.getCount(), 2);
		assertSegment(segmentList.getSegment(0), 0x4000, 0x5FFF, 0);
		assertSegment(segmentList.getSegment(1), 0xA000, 0xBFFF, 0x2000);
		Assert.boolEquals(segmentList.getSegment(1).isType(0x2000 - 4, MemoryType.LABEL), true);
	}

	private static void testErrors() {
		// Raw image of a size without standard type.
		assertError(ATARI800, new byte[0x3000], Messages.E700.format("12288"));

		// Unknown type number in the CART header.
		byte[] file = createCartridgeFile(CartridgeType.CARTRIDGE_STD_8);
		file[7] = (byte) 200;
		assertError(ATARI800, file, Messages.E701.format("200"));

		// CART file of the other platform.
		CartridgeType cartridgeType = CartridgeType.CARTRIDGE_5200_32;
		assertError(ATARI800, createCartridgeFile(cartridgeType), Messages.E702.format("4", cartridgeType.getText(),
				Platform.ATARI_5200.getText(), Platform.ATARI_800.getText()));

		// Type without bank regions.
		cartridgeType = CartridgeType.CARTRIDGE_AST_32;
		assertError(ATARI800, createCartridgeFile(cartridgeType), Messages.E703.format("47", cartridgeType.getText()));

		// Content size does not match the type.
		cartridgeType = CartridgeType.CARTRIDGE_STD_8;
		byte[] content = new byte[0x4000];
		file = concat(CartridgeFileUtility.createCartridgeHeaderWithCheckSum(cartridgeType.getNumericId(), content),
				content);
		assertError(ATARI800, file, Messages.E704.format("16384", "1", cartridgeType.getText(), "8192"));

		// Larger than 4 MB: by the type in the CART header, and by the file size alone.
		file = concat(CartridgeFileUtility.createCartridgeHeaderWithCheckSum(
				CartridgeType.CARTRIDGE_THECART_32M.getNumericId(), new byte[0x2000]), new byte[0x2000]);
		assertError(ATARI800, file, Messages.E705.format("33554432", "4194304"));
		try {
			AtariCartridgeReader.readCartridge(Platform.ATARI_800, null, new SegmentList(null).createInserter(),
					new ByteArrayInputStream(new byte[0]), 0x500000);
			Assert.fail("Expected an error for a 5 MB file.");
		} catch (IOException ex) {
			Assert.stringEquals(ex.getMessage(), Messages.E705.format("5242880", "4194304"));
		}
	}

	private static CartridgeType detect(Platform platform, long fileSize) {
		return CartridgeReader.detectCartridgeType(platform, fileSize, new byte[16]);
	}

	/** {@code size} bytes where every 4 KB page starts with its page number as a little-endian word. */
	private static byte[] createContent(int size) {
		byte[] content = new byte[size];
		for (int page = 0; page * 0x1000 < size; page++) {
			content[page * 0x1000] = (byte) page;
			content[page * 0x1000 + 1] = (byte) (page >> 8);
		}
		return content;
	}

	/** A CART file of {@code cartridgeType}, with {@link #createContent} as content. */
	private static byte[] createCartridgeFile(CartridgeType cartridgeType) {
		byte[] content = createContent(cartridgeType.getSize());
		return concat(CartridgeFileUtility.createCartridgeHeaderWithCheckSum(cartridgeType.getNumericId(), content),
				content);
	}

	private static byte[] concat(byte[] a, byte[] b) {
		byte[] result = Arrays.copyOf(a, a.length + b.length);
		System.arraycopy(b, 0, result, a.length, b.length);
		return result;
	}

	private static SegmentList read(ComputerSystem computerSystem, byte[] file) throws IOException {
		return read(computerSystem, file, null);
	}

	private static SegmentList read(ComputerSystem computerSystem, byte[] file, CartridgeType cartridgeType)
			throws IOException {
		SegmentList segmentList = new SegmentList(null);
		SegmentListInserter segmentListInserter = segmentList.createInserter();
		computerSystem.readFile(FileType.ROM_IMAGE_FILE, new ByteArrayInputStream(file), file.length,
				segmentListInserter, cartridgeType);
		segmentListInserter.apply();
		return segmentList;
	}

	/** The segment's range, and that each of its 4 KB pages comes from the image at {@code fileOffset}. */
	private static void assertSegment(Segment segment, int begin, int end, int fileOffset) {
		Assert.longEquals(segment.wBegin, begin);
		Assert.longEquals(segment.wEnd, end);
		Assert.boolEquals(segment.bBinary, true);
		Assert.longEquals(segment.getSize(), end - begin + 1);
		for (int offset = 0; offset < segment.getSize(); offset += 0x1000) {
			Assert.longEquals(segment.getWord(offset), (fileOffset + offset) / 0x1000);
		}
	}

	/** Run address, cartridge present flag and option byte, init address. */
	private static void assertCartridgeHeaderTyped(Segment segment) {
		int offset = segment.getSize() - 6;
		Assert.boolEquals(segment.isType(offset, MemoryType.LABEL), true);
		Assert.boolEquals(segment.isType(offset + 1, MemoryType.LABEL), true);
		Assert.boolEquals(segment.isType(offset + 2, MemoryType.BYTE), true);
		Assert.boolEquals(segment.isType(offset + 3, MemoryType.BYTE), true);
		Assert.boolEquals(segment.isType(offset + 4, MemoryType.LABEL), true);
		Assert.boolEquals(segment.isType(offset + 5, MemoryType.LABEL), true);
	}

	private static void assertError(ComputerSystem computerSystem, byte[] file, String expectedMessage) {
		try {
			read(computerSystem, file);
			Assert.fail("Expected error: " + expectedMessage);
		} catch (IOException ex) {
			Assert.stringEquals(ex.getMessage(), expectedMessage);
		}
	}
}
