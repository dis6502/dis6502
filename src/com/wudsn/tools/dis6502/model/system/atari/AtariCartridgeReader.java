/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model.system.atari;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.wudsn.tools.base.atari.CartridgeType;
import com.wudsn.tools.base.atari.Platform;
import com.wudsn.tools.base.common.TextUtility;
import com.wudsn.tools.dis6502.Texts;
import com.wudsn.tools.dis6502.model.Segment;
import com.wudsn.tools.dis6502.model.SegmentListInserter;
import com.wudsn.tools.dis6502.model.system.atari.CartridgeReader.Bank;
import com.wudsn.tools.dis6502.model.system.atari.CartridgeReader.Cartridge;

/**
 * Imports an Atari 800 or Atari 5200 cartridge image read by {@link
 * CartridgeReader} as one segment per bank.
 * <p>
 * A cartridge with one bank becomes one segment without title or label
 * prefix. A cartridge with several banks becomes one segment per bank,
 * titled with the type and bank number, and with the label prefix {@code
 * B<n>_}: banks share addresses, so without it the listing would define
 * the same label once per bank and not assemble. Cartridges larger than
 * {@link #MAX_CARTRIDGE_SIZE} are not imported - The!Cart's 32 to 128 MB
 * would be thousands of segments.
 *
 * @author Peter Dell
 */
public final class AtariCartridgeReader {

	/** The largest cartridge image imported, without header. */
	public static final int MAX_CARTRIDGE_SIZE = 4 * 1024 * 1024;

	/** The result of {@link #readCartridge}. */
	public record CartridgeImport(CartridgeType cartridgeType, List<Segment> segments, Segment initialSegment) {
	}

	private AtariCartridgeReader() {
	}

	/** Whether {@code cartridgeType} can be imported for {@code platform}: readable, and not larger than {@link #MAX_CARTRIDGE_SIZE}. */
	public static boolean isSupported(Platform platform, CartridgeType cartridgeType) {
		return CartridgeReader.isSupported(platform, cartridgeType) && cartridgeType.getSize() <= MAX_CARTRIDGE_SIZE;
	}

	/**
	 * The cartridge types of {@code platform} a file could be, for the user to
	 * choose from: empty if its type is known - from a CART header, or a raw
	 * image of a standard size - or no importable type has its size.
	 *
	 * @param header the file's first 16 bytes
	 */
	public static List<CartridgeType> getCandidateTypes(Platform platform, long fileSize, byte[] header) {
		if (CartridgeReader.hasCartridgeHeader(fileSize, header)
				|| CartridgeReader.detectCartridgeType(platform, fileSize, header) != CartridgeType.UNKNOWN) {
			return List.of();
		}
		List<CartridgeType> result = CartridgeReader.getCandidateTypes(platform, fileSize);
		result.removeIf(cartridgeType -> !isSupported(platform, cartridgeType));
		return result;
	}

	/**
	 * Reads a cartridge image of {@code fileSize} bytes, raw or with CART
	 * header, and inserts one segment per bank.
	 *
	 * @param cartridgeType the type of a raw image, as chosen by the user; or
	 *                      {@code null} to use the CART header's type or, for a
	 *                      raw image, the standard type for its size
	 */
	public static CartridgeImport readCartridge(Platform platform, CartridgeType cartridgeType,
			SegmentListInserter segmentListInserter, InputStream inputStream, long fileSize) throws IOException {
		Cartridge cartridge = CartridgeReader.readCartridge(platform, cartridgeType, inputStream, fileSize,
				MAX_CARTRIDGE_SIZE);
		List<Bank> banks = cartridge.getBanks();
		List<Segment> segments = new ArrayList<>(banks.size());
		Segment initialSegment = null;
		// TODO: Code reached through a mirror (e.g. the $A000 mirror of a 4 KB
		// cartridge at $B000, see BankRegion.getMirrorAddresses()) is not traced,
		// since each bank is a segment at one address only.
		for (Bank bank : banks) {
			boolean initial = bank == cartridge.getInitialBank();
			Segment segment = segmentListInserter.insertSegment();
			segment.wBegin = bank.getAddress();
			segment.wEnd = bank.getAddress() + bank.getSize() - 1;
			segment.bBinary = true;
			segment.createMemoryBlockWithSize(bank.getSize());
			segment.setData(0, cartridge.getContent(), bank.getOffset(), bank.getSize());
			if (banks.size() > 1) {
				segment.title = TextUtility.format(
						initial ? Texts.AtariCartridgeReader_InitialBankTitle : Texts.AtariCartridgeReader_BankTitle,
						cartridge.getCartridgeType().getText(), String.valueOf(bank.getNumber()));
				segment.labelPrefix = "B" + bank.getNumber() + "_";
			}
			if (initial) {
				initialSegment = segment;
			}
			segments.add(segment);
		}
		return new CartridgeImport(cartridge.getCartridgeType(), Collections.unmodifiableList(segments),
				initialSegment);
	}
}
