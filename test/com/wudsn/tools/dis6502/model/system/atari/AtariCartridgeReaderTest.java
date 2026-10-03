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
 * values: every byte of bank <i>n</i> holds <i>n</i>, so a bank that lands
 * in the wrong segment or at the wrong address is caught. Cartridge type
 * texts come from WUDSN Base's localized value sets, so expected titles and
 * messages use {@link CartridgeType#getText()} for the type name.
 *
 * @author Peter Dell
 */
public final class AtariCartridgeReaderTest {

	private static final ComputerSystemFactory FACTORY = new ComputerSystemFactory();
	private static final ComputerSystem ATARI800 = FACTORY.getComputerSystem(ComputerSystemType.ATARI800);
	private static final ComputerSystem ATARI5200 = FACTORY.getComputerSystem(ComputerSystemType.ATARI5200);

	private AtariCartridgeReaderTest() {
	}

	public static void testAtariCartridgeReader() throws IOException {
		testDetection();
		testAtari800();
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

		// CART header: its type wins, whatever the size.
		byte[] header = createCartridgeFile(CartridgeType.CARTRIDGE_WILL_64);
		Assert.boolEquals(AtariCartridgeReader.detectCartridgeType(Platform.ATARI_800, header.length,
				header) == CartridgeType.CARTRIDGE_WILL_64, true);
		header[7] = (byte) 200; // No such type number.
		Assert.boolEquals(AtariCartridgeReader.detectCartridgeType(Platform.ATARI_800, header.length,
				header) == CartridgeType.UNKNOWN, true);

		// File types.
		Assert.boolEquals(ATARI800.guessFileType(0x0800, new byte[16]) == FileType.ROM_IMAGE_FILE, true);
		Assert.boolEquals(ATARI800.guessFileType(0x8000, new byte[16]) == FileType.ANY_FILE, true);
		// A 4 KB CART file was detected before, but could not be read.
		byte[] std4 = createCartridgeFile(CartridgeType.CARTRIDGE_STD_4);
		Assert.boolEquals(ATARI800.guessFileType(std4.length, std4) == FileType.ROM_IMAGE_FILE, true);
		// A CART file of the other platform is a ROM image, so that reading it explains the mismatch.
		byte[] std32 = createCartridgeFile(CartridgeType.CARTRIDGE_5200_32);
		Assert.boolEquals(ATARI800.guessFileType(std32.length, std32) == FileType.ROM_IMAGE_FILE, true);
		// A raw 40 KB 5200 image was detected before, but could not be read. Until Bounty Bob is supported, it is no ROM image.
		Assert.boolEquals(ATARI5200.guessFileType(0xA000, new byte[16]) == FileType.ANY_FILE, true);

		// Candidates for a raw 64 KB image, sorted by text.
		List<CartridgeType> candidates = AtariCartridgeReader.getCandidateTypes(Platform.ATARI_800, 0x10000);
		Assert.longEquals(candidates.size(), 7);
		for (CartridgeType cartridgeType : Arrays.asList(CartridgeType.CARTRIDGE_WILL_64,
				CartridgeType.CARTRIDGE_EXP_64, CartridgeType.CARTRIDGE_DIAMOND_64, CartridgeType.CARTRIDGE_SDX_64,
				CartridgeType.CARTRIDGE_TURBOSOFT_64, CartridgeType.CARTRIDGE_ADAWLIAH_64,
				CartridgeType.CARTRIDGE_MEGA_64)) {
			Assert.boolEquals(candidates.contains(cartridgeType), true);
		}
		for (int i = 1; i < candidates.size(); i++) {
			Assert.boolEquals(candidates.get(i - 1).getText().compareTo(candidates.get(i).getText()) <= 0, true);
		}
		Assert.boolEquals(AtariCartridgeReader.getCandidateTypes(Platform.ATARI_5200, 0x10000)
				.equals(List.of(CartridgeType.CARTRIDGE_5200_SUPER_64)), true);
	}

	private static void testAtari800() throws IOException {
		// Raw 8 KB: one segment without title and prefix, cartridge header typed.
		SegmentList segmentList = read(ATARI800, createContent(0x2000, 0x2000));
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
			assertSegment(segment, 0xA000, 0xBFFF, bank);
			Assert.stringEquals(segment.labelPrefix, "B" + bank + "_");
			Assert.stringEquals(segment.title,
					cartridgeType.getText() + " - bank " + bank + (bank == 0 ? " (initial)" : ""));
		}
		assertCartridgeHeaderTyped(segmentList.getSegment(0));
		Assert.boolEquals(segmentList.getSegment(1).isType(0x1FFA, MemoryType.LABEL), false);

		// Atarimax 1 MB (old): 128 banks, bank 127 initial.
		segmentList = read(ATARI800, createCartridgeFile(CartridgeType.CARTRIDGE_ATMAX_1024));
		Assert.longEquals(segmentList.getCount(), 128);
		assertSegment(segmentList.getSegment(127), 0xA000, 0xBFFF, 127);
		Assert.boolEquals(segmentList.getSegment(127).title.endsWith(" - bank 127 (initial)"), true);
		Assert.boolEquals(segmentList.getSegment(0).title.endsWith(" - bank 0"), true);
		assertCartridgeHeaderTyped(segmentList.getSegment(127));

		// Flash MegaCart 4 MB: 256 banks of 16 KB at $8000, bank 254 initial.
		segmentList = read(ATARI800, createCartridgeFile(CartridgeType.CARTRIDGE_MEGA_4096));
		Assert.longEquals(segmentList.getCount(), 256);
		assertSegment(segmentList.getSegment(255), 0x8000, 0xBFFF, 255);
		Assert.boolEquals(segmentList.getSegment(254).title.endsWith(" (initial)"), true);
		assertCartridgeHeaderTyped(segmentList.getSegment(254));
	}

	private static void testAtari5200() throws IOException {
		// Raw 4 KB: at $B000, title and vectors typed, title taken from $BFE8.
		byte[] content = createContent(0x1000, 0x1000);
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
		assertSegment(read(ATARI5200, createContent(0x4000, 0x4000)).getSegment(0), 0x8000, 0xBFFF, 0);
		assertSegment(read(ATARI5200, createContent(0x8000, 0x8000)).getSegment(0), 0x4000, 0xBFFF, 0);

		// Super Cart 128 KB: 4 banks of 32 KB at $4000, bank 3 initial.
		segmentList = read(ATARI5200, createCartridgeFile(CartridgeType.CARTRIDGE_5200_SUPER_128));
		Assert.longEquals(segmentList.getCount(), 4);
		for (int bank = 0; bank < 4; bank++) {
			assertSegment(segmentList.getSegment(bank), 0x4000, 0xBFFF, bank);
		}
		Assert.boolEquals(segmentList.getSegment(3).title.endsWith(" (initial)"), false); // Replaced by the ROM title.
		Assert.boolEquals(segmentList.getSegment(3).isType(0x8000 - 4, MemoryType.LABEL), true);
		Assert.boolEquals(segmentList.getSegment(0).isType(0x8000 - 4, MemoryType.LABEL), false);
	}

	private static void testErrors() {
		// Raw image of a size without standard type.
		assertError(ATARI800, new byte[0x3000], "Unsupported cartridge size 12288.");

		// Unknown type number in the CART header.
		byte[] file = createCartridgeFile(CartridgeType.CARTRIDGE_STD_8);
		file[7] = (byte) 200;
		assertError(ATARI800, file, "Cartridge type 200 in the CART header is unknown.");

		// CART file of the other platform.
		CartridgeType cartridgeType = CartridgeType.CARTRIDGE_5200_32;
		assertError(ATARI800, createCartridgeFile(cartridgeType), "Cartridge type 4 (" + cartridgeType.getText()
				+ ") is a cartridge for " + Platform.ATARI_5200.getText() + ", not for " + Platform.ATARI_800.getText()
				+ ".");

		// Type without layout.
		cartridgeType = CartridgeType.CARTRIDGE_AST_32;
		assertError(ATARI800, createCartridgeFile(cartridgeType),
				"Cartridge type 47 (" + cartridgeType.getText() + ") is not supported.");

		// Content size does not match the type.
		cartridgeType = CartridgeType.CARTRIDGE_STD_8;
		byte[] content = new byte[0x4000];
		file = concat(CartridgeFileUtility.createCartridgeHeaderWithCheckSum(cartridgeType.getNumericId(), content),
				content);
		assertError(ATARI800, file,
				"Cartridge has 16384 bytes, but cartridge type 1 (" + cartridgeType.getText() + ") has 8192 bytes.");

		// Larger than 4 MB: by the type in the CART header, and by the file size alone.
		file = concat(CartridgeFileUtility.createCartridgeHeaderWithCheckSum(
				CartridgeType.CARTRIDGE_THECART_32M.getNumericId(), new byte[0x2000]), new byte[0x2000]);
		assertError(ATARI800, file, "Cartridge has 33554432 bytes, more than the supported maximum of 4194304 bytes.");
		try {
			AtariCartridgeReader.readCartridge(Platform.ATARI_800, null, new SegmentList(null).createInserter(),
					new ByteArrayInputStream(new byte[0]), 0x500000);
			Assert.fail("Expected an error for a 5 MB file.");
		} catch (IOException ex) {
			Assert.stringEquals(ex.getMessage(),
					"Cartridge has 5242880 bytes, more than the supported maximum of 4194304 bytes.");
		}
	}

	private static CartridgeType detect(Platform platform, long fileSize) {
		return AtariCartridgeReader.detectCartridgeType(platform, fileSize, new byte[16]);
	}

	/** {@code size} bytes where every byte holds the number of its {@code bankSize} bank. */
	private static byte[] createContent(int size, int bankSize) {
		byte[] content = new byte[size];
		for (int i = 0; i < size; i++) {
			content[i] = (byte) (i / bankSize);
		}
		return content;
	}

	/** A CART file of {@code cartridgeType}, with {@link #createContent} as content. */
	private static byte[] createCartridgeFile(CartridgeType cartridgeType) {
		byte[] content = createContent(cartridgeType.getSizeInKB() * 1024, cartridgeType.getBankSize());
		return concat(CartridgeFileUtility.createCartridgeHeaderWithCheckSum(cartridgeType.getNumericId(), content),
				content);
	}

	private static byte[] concat(byte[] a, byte[] b) {
		byte[] result = Arrays.copyOf(a, a.length + b.length);
		System.arraycopy(b, 0, result, a.length, b.length);
		return result;
	}

	private static SegmentList read(ComputerSystem computerSystem, byte[] file) throws IOException {
		SegmentList segmentList = new SegmentList(null);
		SegmentListInserter segmentListInserter = segmentList.createInserter();
		computerSystem.readFile(FileType.ROM_IMAGE_FILE, new ByteArrayInputStream(file), file.length,
				segmentListInserter);
		segmentListInserter.apply();
		return segmentList;
	}

	private static void assertSegment(Segment segment, int begin, int end, int bank) {
		Assert.longEquals(segment.wBegin, begin);
		Assert.longEquals(segment.wEnd, end);
		Assert.boolEquals(segment.bBinary, true);
		Assert.longEquals(segment.getSize(), end - begin + 1);
		for (int offset = 0; offset < segment.getSize(); offset += 0x100) {
			Assert.longEquals(segment.getData(offset), bank & 0xFF);
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
