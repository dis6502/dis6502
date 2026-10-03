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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.wudsn.tools.base.atari.CartridgeFileUtility;
import com.wudsn.tools.base.atari.CartridgeType;
import com.wudsn.tools.base.atari.Platform;
import com.wudsn.tools.base.common.TextUtility;
import com.wudsn.tools.dis6502.Messages;
import com.wudsn.tools.dis6502.Texts;
import com.wudsn.tools.dis6502.model.Segment;
import com.wudsn.tools.dis6502.model.SegmentListInserter;

/**
 * Imports Atari 800 and Atari 5200 cartridge images, raw or with a {@code
 * CART} header, as one segment per bank. The cartridge types are WUDSN
 * Base's {@link CartridgeType}, whose numeric ids are the {@code CART}
 * header's type numbers; where each bank appears in memory follows
 * atari800's {@code DOC/cart.txt}, which {@link CartridgeType} itself cites.
 * <p>
 * A cartridge with one bank becomes one segment without title or label
 * prefix. A cartridge with several banks becomes one segment per bank,
 * titled with the type and bank number, and with the label prefix {@code
 * B<n>_}: banks share addresses, so without it the listing would define
 * the same label once per bank and not assemble.
 * <p>
 * Supported so far: every type whose banks all appear in one address
 * window. Cartridges larger than {@link #MAX_CARTRIDGE_SIZE} are not
 * imported - The!Cart's 32 to 128 MB would be thousands of segments.
 *
 * @author Peter Dell
 */
public final class AtariCartridgeReader {

	/** The largest cartridge image imported, without header. */
	public static final int MAX_CARTRIDGE_SIZE = 4 * 1024 * 1024;

	/** One bank of a cartridge image: where it is in the image and where it appears in memory. */
	record Bank(int offset, int size, int address) {
	}

	/** Where the banks of a cartridge type appear in memory. */
	private interface Layout {
		List<Bank> getBanks(int contentSize);
	}

	/** The result of {@link #readCartridge}. */
	public record CartridgeImport(CartridgeType cartridgeType, List<Segment> segments, Segment initialSegment) {
	}

	private static final Map<CartridgeType, Layout> LAYOUTS = new HashMap<>();

	static {
		// Atari 800, one bank.
		singleWindow(CartridgeType.CARTRIDGE_STD_2, 0xB800);
		singleWindow(CartridgeType.CARTRIDGE_STD_4, 0xB000);
		singleWindow(CartridgeType.CARTRIDGE_STD_8, 0xA000);
		singleWindow(CartridgeType.CARTRIDGE_STD_16, 0x8000);
		singleWindow(CartridgeType.CARTRIDGE_RIGHT_4, 0x9000);
		singleWindow(CartridgeType.CARTRIDGE_RIGHT_8, 0x8000);
		singleWindow(CartridgeType.CARTRIDGE_LOW_BANK_8, 0x8000);
		singleWindow(CartridgeType.CARTRIDGE_PHOENIX_8, 0xA000);
		// Mirrored at $A000 and $B000; $B000 holds the cartridge header at $BFFA.
		singleWindow(CartridgeType.CARTRIDGE_BLIZZARD_4, 0xB000);
		singleWindow(CartridgeType.CARTRIDGE_BLIZZARD_16, 0x8000);
		singleWindow(CartridgeType.CARTRIDGE_MEGA_16, 0x8000);

		// Atari 800, 8 KB banks at $A000.
		singleWindow(CartridgeType.CARTRIDGE_WILL_32, 0xA000);
		singleWindow(CartridgeType.CARTRIDGE_WILL_64, 0xA000);
		singleWindow(CartridgeType.CARTRIDGE_EXP_64, 0xA000);
		singleWindow(CartridgeType.CARTRIDGE_DIAMOND_64, 0xA000);
		singleWindow(CartridgeType.CARTRIDGE_SDX_64, 0xA000);
		singleWindow(CartridgeType.CARTRIDGE_SDX_128, 0xA000);
		singleWindow(CartridgeType.CARTRIDGE_ATRAX_DEC_128, 0xA000);
		singleWindow(CartridgeType.CARTRIDGE_ATMAX_128, 0xA000);
		singleWindow(CartridgeType.CARTRIDGE_ATMAX_1024, 0xA000);
		singleWindow(CartridgeType.CARTRIDGE_ATMAX_NEW_1024, 0xA000);
		singleWindow(CartridgeType.CARTRIDGE_TURBOSOFT_64, 0xA000);
		singleWindow(CartridgeType.CARTRIDGE_TURBOSOFT_128, 0xA000);
		singleWindow(CartridgeType.CARTRIDGE_ULTRACART_32, 0xA000);
		singleWindow(CartridgeType.CARTRIDGE_BLIZZARD_32, 0xA000);
		singleWindow(CartridgeType.CARTRIDGE_ADAWLIAH_32, 0xA000);
		singleWindow(CartridgeType.CARTRIDGE_ADAWLIAH_64, 0xA000);

		// Atari 800, 16 KB banks at $8000.
		singleWindow(CartridgeType.CARTRIDGE_MEGA_32, 0x8000);
		singleWindow(CartridgeType.CARTRIDGE_MEGA_64, 0x8000);
		singleWindow(CartridgeType.CARTRIDGE_MEGA_128, 0x8000);
		singleWindow(CartridgeType.CARTRIDGE_MEGA_256, 0x8000);
		singleWindow(CartridgeType.CARTRIDGE_MEGA_512, 0x8000);
		singleWindow(CartridgeType.CARTRIDGE_MEGA_1024, 0x8000);
		singleWindow(CartridgeType.CARTRIDGE_MEGA_2048, 0x8000);
		singleWindow(CartridgeType.CARTRIDGE_MEGA_4096, 0x8000);
		singleWindow(CartridgeType.CARTRIDGE_MEGAMAX_2048, 0x8000);

		// Atari 5200: the mirror that ends at $BFFF, which holds the title and vectors.
		singleWindow(CartridgeType.CARTRIDGE_5200_4, 0xB000);
		singleWindow(CartridgeType.CARTRIDGE_5200_8, 0xA000);
		singleWindow(CartridgeType.CARTRIDGE_5200_NS_16, 0x8000);
		singleWindow(CartridgeType.CARTRIDGE_5200_32, 0x4000);
		singleWindow(CartridgeType.CARTRIDGE_5200_SUPER_64, 0x4000);
		singleWindow(CartridgeType.CARTRIDGE_5200_SUPER_128, 0x4000);
		singleWindow(CartridgeType.CARTRIDGE_5200_SUPER_256, 0x4000);
		singleWindow(CartridgeType.CARTRIDGE_5200_SUPER_512, 0x4000);
	}

	/** Raw images (no CART header) of these sizes are read as the standard type of their platform without asking. */
	private static final Map<Platform, Map<Integer, CartridgeType>> DEFAULT_TYPES = Map.of(Platform.ATARI_800,
			Map.of(0x0800, CartridgeType.CARTRIDGE_STD_2, 0x1000, CartridgeType.CARTRIDGE_STD_4, 0x2000,
					CartridgeType.CARTRIDGE_STD_8, 0x4000, CartridgeType.CARTRIDGE_STD_16),
			Platform.ATARI_5200,
			Map.of(0x1000, CartridgeType.CARTRIDGE_5200_4, 0x2000, CartridgeType.CARTRIDGE_5200_8, 0x4000,
					CartridgeType.CARTRIDGE_5200_NS_16, 0x8000, CartridgeType.CARTRIDGE_5200_32));

	private AtariCartridgeReader() {
	}

	/** Every bank of {@code cartridgeType}'s bank size at the same {@code address}. */
	private static void singleWindow(CartridgeType cartridgeType, int address) {
		int bankSize = cartridgeType.getBankSize();
		LAYOUTS.put(cartridgeType, contentSize -> {
			List<Bank> banks = new ArrayList<>();
			for (int offset = 0; offset < contentSize; offset += bankSize) {
				banks.add(new Bank(offset, bankSize, address));
			}
			return banks;
		});
	}

	/** Whether a file of {@code fileSize} bytes starts with a CART header ({@code header}: at least its first 4 bytes). */
	public static boolean hasCartridgeHeader(long fileSize, byte[] header) {
		long contentSize = fileSize - CartridgeFileUtility.CART_HEADER_SIZE;
		return contentSize > 0 && contentSize % 1024 == 0 && header.length >= 4 && header[0] == 'C'
				&& header[1] == 'A' && header[2] == 'R' && header[3] == 'T';
	}

	/**
	 * The cartridge type of a file: from its CART header if it has one
	 * ({@code header}: the file's first 16 bytes), otherwise the standard
	 * type of {@code platform} for the file's size. Returns {@link
	 * CartridgeType#UNKNOWN} if neither applies, e.g. for a header type
	 * number that WUDSN Base does not know.
	 */
	public static CartridgeType detectCartridgeType(Platform platform, long fileSize, byte[] header) {
		if (hasCartridgeHeader(fileSize, header)) {
			CartridgeType cartridgeType = header.length >= CartridgeFileUtility.CART_HEADER_SIZE
					? CartridgeType.getInstance(getCartridgeTypeNumericId(header))
					: null;
			return cartridgeType != null ? cartridgeType : CartridgeType.UNKNOWN;
		}
		Map<Integer, CartridgeType> defaultTypes = DEFAULT_TYPES.get(platform);
		CartridgeType cartridgeType = defaultTypes == null || fileSize > Integer.MAX_VALUE ? null
				: defaultTypes.get((int) fileSize);
		return cartridgeType != null ? cartridgeType : CartridgeType.UNKNOWN;
	}

	/**
	 * The supported types of {@code platform} with the size of a raw image of
	 * {@code fileSize} bytes, sorted by their text.
	 */
	public static List<CartridgeType> getCandidateTypes(Platform platform, long fileSize) {
		List<CartridgeType> result = new ArrayList<>();
		for (CartridgeType cartridgeType : LAYOUTS.keySet()) {
			if (cartridgeType.getPlatform() == platform && cartridgeType.getSizeInKB() * 1024L == fileSize) {
				result.add(cartridgeType);
			}
		}
		result.sort((a, b) -> a.getText().compareTo(b.getText()));
		return result;
	}

	/** Whether {@code cartridgeType} can be imported for {@code platform}. */
	public static boolean isSupported(Platform platform, CartridgeType cartridgeType) {
		return cartridgeType.getPlatform() == platform && LAYOUTS.containsKey(cartridgeType)
				&& cartridgeType.getSizeInKB() * 1024L <= MAX_CARTRIDGE_SIZE;
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
		if (fileSize > MAX_CARTRIDGE_SIZE + CartridgeFileUtility.CART_HEADER_SIZE) {
			// ERROR: Cartridge has {0} bytes, more than the supported maximum of {1} bytes.
			throw new IOException(Messages.E097.format(String.valueOf(fileSize), String.valueOf(MAX_CARTRIDGE_SIZE)));
		}
		byte[] content = inputStream.readNBytes((int) fileSize);

		if (hasCartridgeHeader(content.length, content)) {
			int numericId = getCartridgeTypeNumericId(content);
			cartridgeType = CartridgeType.getInstance(numericId);
			if (cartridgeType == null) {
				// ERROR: Cartridge type {0} in the CART header is unknown.
				throw new IOException(Messages.E098.format(String.valueOf(numericId)));
			}
			content = CartridgeFileUtility.getCartridgeContent(content);
		} else if (cartridgeType == null) {
			cartridgeType = detectCartridgeType(platform, content.length, content);
			if (cartridgeType == CartridgeType.UNKNOWN) {
				// ERROR: Unsupported cartridge size {0}.
				throw new IOException(Messages.E055.format(String.valueOf(content.length)));
			}
		}

		String typeNumber = String.valueOf(cartridgeType.getNumericId());
		if (cartridgeType.getPlatform() != platform) {
			// ERROR: Cartridge type {0} ({1}) is a cartridge for {2}, not for {3}.
			throw new IOException(Messages.E095.format(typeNumber, cartridgeType.getText(),
					cartridgeType.getPlatform().getText(), platform.getText()));
		}
		int typeSize = cartridgeType.getSizeInKB() * 1024;
		if (typeSize > MAX_CARTRIDGE_SIZE) {
			// ERROR: Cartridge has {0} bytes, more than the supported maximum of {1} bytes.
			throw new IOException(Messages.E097.format(String.valueOf(typeSize), String.valueOf(MAX_CARTRIDGE_SIZE)));
		}
		Layout layout = LAYOUTS.get(cartridgeType);
		if (layout == null) {
			// ERROR: Cartridge type {0} ({1}) is not supported.
			throw new IOException(Messages.E094.format(typeNumber, cartridgeType.getText()));
		}
		if (content.length != typeSize) {
			// ERROR: Cartridge has {0} bytes, but cartridge type {1} ({2}) has {3} bytes.
			throw new IOException(Messages.E096.format(String.valueOf(content.length), typeNumber,
					cartridgeType.getText(), String.valueOf(typeSize)));
		}

		List<Bank> banks = layout.getBanks(content.length);
		List<Segment> segments = new ArrayList<>(banks.size());
		Segment initialSegment = null;
		for (int bankNumber = 0; bankNumber < banks.size(); bankNumber++) {
			Bank bank = banks.get(bankNumber);
			boolean initial = bank.offset() <= cartridgeType.getInitialBankOffset()
					&& cartridgeType.getInitialBankOffset() < bank.offset() + bank.size();
			Segment segment = segmentListInserter.insertSegment();
			segment.wBegin = bank.address();
			segment.wEnd = bank.address() + bank.size() - 1;
			segment.bBinary = true;
			segment.createMemoryBlockWithSize(bank.size());
			segment.setData(0, content, bank.offset(), bank.size());
			if (banks.size() > 1) {
				segment.title = TextUtility.format(
						initial ? Texts.AtariCartridgeReader_InitialBankTitle : Texts.AtariCartridgeReader_BankTitle,
						cartridgeType.getText(), String.valueOf(bankNumber));
				segment.labelPrefix = "B" + bankNumber + "_";
			}
			if (initial) {
				initialSegment = segment;
			}
			segments.add(segment);
		}
		return new CartridgeImport(cartridgeType, Collections.unmodifiableList(segments), initialSegment);
	}

	/** The type number in a CART header: bytes 4 to 7, big-endian. */
	private static int getCartridgeTypeNumericId(byte[] header) {
		return ((header[4] & 0xFF) << 24) | ((header[5] & 0xFF) << 16) | ((header[6] & 0xFF) << 8)
				| (header[7] & 0xFF);
	}
}
