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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.wudsn.tools.base.atari.CartridgeFileUtility;
import com.wudsn.tools.base.atari.CartridgeType;
import com.wudsn.tools.base.atari.CartridgeType.BankRegion;
import com.wudsn.tools.base.atari.Messages;
import com.wudsn.tools.base.atari.Platform;

/**
 * Reads Atari 800 and Atari 5200 cartridge images, raw or with a {@code CART}
 * header, and splits them into banks with their CPU addresses, as described
 * by {@link CartridgeType#getBankRegions()}. Interleaved Atrax images are
 * decoded first.
 * <p>
 * Depends only on the JDK and WUDSN Base, and is written at the Java 8 source
 * level of WUDSN Base: it is meant to move to {@code com.wudsn.tools.base.atari}.
 *
 * @author Peter Dell
 */
public final class CartridgeReader {

	/** One bank of a cartridge image: where it is in the image and where it appears in memory. */
	public static final class Bank {

		private final int number;
		private final int offset;
		private final int size;
		private final int address;

		Bank(int number, int offset, int size, int address) {
			this.number = number;
			this.offset = offset;
			this.size = size;
			this.address = address;
		}

		/** @return The number of the bank, counting from 0 in the order of the image. */
		public int getNumber() {
			return number;
		}

		/** @return The offset of the bank in the plain image. */
		public int getOffset() {
			return offset;
		}

		/** @return The size of the bank in bytes. */
		public int getSize() {
			return size;
		}

		/** @return The CPU address where the bank appears. */
		public int getAddress() {
			return address;
		}
	}

	/** A cartridge read by {@link CartridgeReader#readCartridge}. */
	public static final class Cartridge {

		private final CartridgeType cartridgeType;
		private final byte[] content;
		private final List<Bank> banks;
		private final Bank initialBank;

		Cartridge(CartridgeType cartridgeType, byte[] content, List<Bank> banks, Bank initialBank) {
			this.cartridgeType = cartridgeType;
			this.content = content;
			this.banks = Collections.unmodifiableList(banks);
			this.initialBank = initialBank;
		}

		/** @return The cartridge type, not <code>null</code>. */
		public CartridgeType getCartridgeType() {
			return cartridgeType;
		}

		/** @return The plain image, without CART header and decoded, not <code>null</code>. */
		public byte[] getContent() {
			return content;
		}

		/** @return The banks in the order of the image, not empty. */
		public List<Bank> getBanks() {
			return banks;
		}

		/**
		 * @return The bank with the cartridge header (Atari 800) or title and
		 *         vectors (Atari 5200) visible after power-on, not
		 *         <code>null</code>.
		 */
		public Bank getInitialBank() {
			return initialBank;
		}
	}

	/** Raw images (no CART header) of these sizes are read as the standard type of their platform without asking. */
	private static final Map<Platform, Map<Integer, CartridgeType>> DEFAULT_TYPES = new HashMap<Platform, Map<Integer, CartridgeType>>();

	static {
		Map<Integer, CartridgeType> atari800 = new HashMap<Integer, CartridgeType>();
		atari800.put(Integer.valueOf(0x0800), CartridgeType.CARTRIDGE_STD_2);
		atari800.put(Integer.valueOf(0x1000), CartridgeType.CARTRIDGE_STD_4);
		atari800.put(Integer.valueOf(0x2000), CartridgeType.CARTRIDGE_STD_8);
		atari800.put(Integer.valueOf(0x4000), CartridgeType.CARTRIDGE_STD_16);
		DEFAULT_TYPES.put(Platform.ATARI_800, atari800);

		Map<Integer, CartridgeType> atari5200 = new HashMap<Integer, CartridgeType>();
		atari5200.put(Integer.valueOf(0x1000), CartridgeType.CARTRIDGE_5200_4);
		atari5200.put(Integer.valueOf(0x2000), CartridgeType.CARTRIDGE_5200_8);
		atari5200.put(Integer.valueOf(0x4000), CartridgeType.CARTRIDGE_5200_NS_16);
		atari5200.put(Integer.valueOf(0x8000), CartridgeType.CARTRIDGE_5200_32);
		atari5200.put(Integer.valueOf(0xA000), CartridgeType.CARTRIDGE_5200_40);
		DEFAULT_TYPES.put(Platform.ATARI_5200, atari5200);
	}

	private CartridgeReader() {
	}

	/**
	 * Determines if a file starts with a CART header.
	 *
	 * @param fileSize The size of the file in bytes.
	 * @param header   The first bytes of the file, at least 4, not
	 *                 <code>null</code>.
	 * @return <code>true</code> if the file has a CART header.
	 */
	public static boolean hasCartridgeHeader(long fileSize, byte[] header) {
		if (header == null) {
			throw new IllegalArgumentException("Parameter 'header' must not be null.");
		}
		long contentSize = fileSize - CartridgeFileUtility.CART_HEADER_SIZE;
		return contentSize > 0 && contentSize % 1024 == 0 && header.length >= 4 && header[0] == 'C'
				&& header[1] == 'A' && header[2] == 'R' && header[3] == 'T';
	}

	/**
	 * Determines the cartridge type of a file: from its CART header if it has
	 * one, otherwise the standard type of the platform for the file's size.
	 *
	 * @param platform The platform, not <code>null</code>.
	 * @param fileSize The size of the file in bytes.
	 * @param header   The first 16 bytes of the file, not <code>null</code>.
	 * @return The cartridge type, or {@link CartridgeType#UNKNOWN} if neither
	 *         applies, e.g. for a header type number that is not known.
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
				: defaultTypes.get(Integer.valueOf((int) fileSize));
		return cartridgeType != null ? cartridgeType : CartridgeType.UNKNOWN;
	}

	/**
	 * Gets the supported cartridge types of a platform with the size of a raw
	 * image, for the user to choose from.
	 *
	 * @param platform The platform, not <code>null</code>.
	 * @param fileSize The size of the raw image in bytes.
	 * @return The modifiable list of types, sorted by their text ignoring case, may be empty.
	 */
	public static List<CartridgeType> getCandidateTypes(Platform platform, long fileSize) {
		List<CartridgeType> result = new ArrayList<CartridgeType>();
		for (CartridgeType cartridgeType : CartridgeType.getValues()) {
			if (isSupported(platform, cartridgeType) && cartridgeType.getSize() == fileSize) {
				result.add(cartridgeType);
			}
		}
		Collections.sort(result, new Comparator<CartridgeType>() {
			@Override
			public int compare(CartridgeType a, CartridgeType b) {
				return a.getText().compareToIgnoreCase(b.getText());
			}
		});
		return result;
	}

	/**
	 * Determines if a cartridge type can be read for a platform: it belongs to
	 * the platform and its bank regions are known.
	 *
	 * @param platform      The platform, not <code>null</code>.
	 * @param cartridgeType The cartridge type, not <code>null</code>.
	 * @return <code>true</code> if the type can be read.
	 */
	public static boolean isSupported(Platform platform, CartridgeType cartridgeType) {
		return cartridgeType.getPlatform() == platform && !cartridgeType.getBankRegions().isEmpty();
	}

	/**
	 * Reads a cartridge image, raw or with CART header.
	 *
	 * @param platform      The platform, not <code>null</code>.
	 * @param cartridgeType The type of a raw image, as chosen by the user; or
	 *                      <code>null</code> to use the CART header's type or, for
	 *                      a raw image, the standard type for its size.
	 * @param inputStream   The input stream, not <code>null</code>.
	 * @param fileSize      The size of the file in bytes.
	 * @param maximumSize   The largest image read, without header.
	 * @return The cartridge, not <code>null</code>.
	 * @throws IOException If the stream cannot be read, or the image is not a
	 *                     cartridge of a supported type.
	 */
	public static Cartridge readCartridge(Platform platform, CartridgeType cartridgeType, InputStream inputStream,
			long fileSize, long maximumSize) throws IOException {
		if (fileSize > maximumSize + CartridgeFileUtility.CART_HEADER_SIZE) {
			// ERROR: Cartridge has {0} bytes, more than the supported maximum of {1} bytes.
			throw new IOException(Messages.E705.format(String.valueOf(fileSize), String.valueOf(maximumSize)));
		}
		byte[] content = readUpTo(inputStream, (int) fileSize);

		if (hasCartridgeHeader(content.length, content)) {
			int numericId = getCartridgeTypeNumericId(content);
			cartridgeType = CartridgeType.getInstance(numericId);
			if (cartridgeType == null) {
				// ERROR: Cartridge type {0} in the CART header is unknown.
				throw new IOException(Messages.E701.format(String.valueOf(numericId)));
			}
			content = CartridgeFileUtility.getCartridgeContent(content);
		} else if (cartridgeType == null) {
			cartridgeType = detectCartridgeType(platform, content.length, content);
			if (cartridgeType == CartridgeType.UNKNOWN) {
				// ERROR: Unsupported cartridge size {0}.
				throw new IOException(Messages.E700.format(String.valueOf(content.length)));
			}
		}

		String typeNumber = String.valueOf(cartridgeType.getNumericId());
		if (cartridgeType.getPlatform() != platform) {
			// ERROR: Cartridge type {0} ({1}) is a cartridge for {2}, not for {3}.
			throw new IOException(Messages.E702.format(typeNumber, cartridgeType.getText(),
					cartridgeType.getPlatform().getText(), platform.getText()));
		}
		int typeSize = cartridgeType.getSize();
		if (typeSize > maximumSize) {
			// ERROR: Cartridge has {0} bytes, more than the supported maximum of {1} bytes.
			throw new IOException(Messages.E705.format(String.valueOf(typeSize), String.valueOf(maximumSize)));
		}
		if (cartridgeType.getBankRegions().isEmpty()) {
			// ERROR: Cartridge type {0} ({1}) is not supported.
			throw new IOException(Messages.E703.format(typeNumber, cartridgeType.getText()));
		}
		if (content.length != typeSize) {
			// ERROR: Cartridge has {0} bytes, but cartridge type {1} ({2}) has {3} bytes.
			throw new IOException(Messages.E704.format(String.valueOf(content.length), typeNumber,
					cartridgeType.getText(), String.valueOf(typeSize)));
		}
		if (cartridgeType.isAtraxInterleaved()) {
			content = CartridgeFileUtility.decodeAtraxContent(cartridgeType, content);
		}

		// The bank with the last byte of the initial bank holds the vectors; for the
		// two chip 5200 cartridge, the initial bank is the whole image.
		int initialOffset = cartridgeType.getInitialBankOffset() + cartridgeType.getBankSize() - 1;
		List<Bank> banks = new ArrayList<Bank>();
		Bank initialBank = null;
		for (BankRegion region : cartridgeType.getBankRegions()) {
			List<Integer> addresses = region.getAddresses();
			for (int i = 0; i < region.getSize() / region.getBankSize(); i++) {
				int offset = region.getOffset() + i * region.getBankSize();
				Bank bank = new Bank(banks.size(), offset, region.getBankSize(),
						addresses.get(i % addresses.size()).intValue());
				if (offset <= initialOffset && initialOffset < offset + bank.getSize()) {
					initialBank = bank;
				}
				banks.add(bank);
			}
		}
		if (initialBank == null) {
			throw new IllegalStateException("Cartridge type " + cartridgeType + " has no bank at the initial bank offset.");
		}
		return new Cartridge(cartridgeType, content, banks, initialBank);
	}

	/** The type number in a CART header: bytes 4 to 7, big-endian. */
	private static int getCartridgeTypeNumericId(byte[] header) {
		return ((header[4] & 0xFF) << 24) | ((header[5] & 0xFF) << 16) | ((header[6] & 0xFF) << 8)
				| (header[7] & 0xFF);
	}

	/** Reads up to {@code size} bytes; fewer if the stream ends before. */
	private static byte[] readUpTo(InputStream inputStream, int size) throws IOException {
		byte[] buffer = new byte[size];
		int length = 0;
		while (length < size) {
			int read = inputStream.read(buffer, length, size - length);
			if (read < 0) {
				byte[] result = new byte[length];
				System.arraycopy(buffer, 0, result, 0, length);
				return result;
			}
			length += read;
		}
		return buffer;
	}
}
