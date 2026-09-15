/*
 * Copyright (C) 2025 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.w3c.dom.Element;

import com.wudsn.tools.base.common.HexUtility;

/**
 * One contiguous block of a workspace's memory: a raw/binary/SDX file
 * segment, its disassembly type information, comments, and (transient,
 * rebuilt on every disassembly pass) symbols/fixups/address labels.
 * <p>
 * Ported from Segment.h / Segment.cpp. {@code Load14} (the
 * workspace-version-1X binary format) is not ported yet.
 *
 * @author Peter Dell
 */
public final class Segment implements Xml.Serializable {

	private static final int MAX_SEGMENT_SIZE = 0x10000;

	/** State machine used by {@link #allocateSymbol} to recognize a PRINTF-style format string. */
	private enum PrintfState {
		TEXT, PERCENT, FORMAT
	}

	// Set in constructor/clear().
	public String title = "";
	public int wBegin;   // Only for ATARI_BINARY and SDX_FIXED_BLK.
	public int wEnd;     // End address for ATARI_BINARY and SDX_FIXED_BLK. Size of fix-ups for SDX_SYM_REQUIRED.
	public boolean bBinary; // Is it a code segment to disassemble?
	public String labelPrefix = ""; // Optional prefix for labels, does not contain the separator.
	public int wSDXFixUpSize; // Only for SDX_SYM_REQUIRED and SDX_FIX_UP_BLK.
	public int bSDXBlockNumber; // SDX block number.
	public int bSDXControlByte; // SDX control byte for SDX_RELOC_BLK.
	public String sdxSymbol = ""; // SDX symbol name for SDX_SYM_REQUIRED and SDX_SYM_DEFINED.
	public ProcessorType processorType = ProcessorType.MOS6502;
	public final MemoryBlock memoryBlock = new MemoryBlock();
	public final List<Comment> comments = new ArrayList<>(); // List of comments in this segment.

	// Transient attributes which are not serialized to XML.
	public final List<Symbol> symbols = new ArrayList<>(); // List of SDX system symbols to fix up.
	public final List<Fixup> fixups = new ArrayList<>(); // List of addresses to fix up.

	private FileHeader wHeader = FileHeader.RAW; // Magic word for file header type.
	private final AddressLabelList fixupAddressLabels = new AddressLabelList(); // Addresses defined through fixup.
	private final AddressLabelList addressLabels = new AddressLabelList(); // Addresses referenced by code.
	private int firstLineNumber; // First line in CODE_SECTION part of listing of this segment.

	public Segment() {
		clear();
	}

	public void clear() {
		title = "";

		wHeader = FileHeader.RAW;
		wBegin = 0;
		wEnd = 0;

		bBinary = false;
		processorType = ProcessorType.MOS6502;
		labelPrefix = "";
		wSDXFixUpSize = 0;
		bSDXBlockNumber = 0;
		bSDXControlByte = 0;
		sdxSymbol = "";

		clearMemoryBlock();
		clearComments();
		symbols.clear();
		clearFixups();
		fixupAddressLabels.clear();
		addressLabels.clear();

		firstLineNumber = 0;
	}

	public FileHeader getHeader() {
		return wHeader;
	}

	public boolean isHeader(FileHeader header) {
		return wHeader == header;
	}

	public void setHeader(FileHeader header) {
		wHeader = header;
	}

	public int getSize() {
		if (memoryBlock.isEmpty()) {
			return 0;
		}
		return wEnd - wBegin + 1;
	}

	public boolean isEmpty() {
		return memoryBlock.isEmpty();
	}

	public boolean isSDX() {
		return wHeader == FileHeader.SDX_FIXED_BLK || wHeader == FileHeader.SDX_SYM_REQUIRED
				|| wHeader == FileHeader.SDX_SYM_DEFINED || wHeader == FileHeader.SDX_FIX_UP_BLK
				|| wHeader == FileHeader.SDX_RELOC_BLK;
	}

	@Override
	public String toString() {
		int size = getSize();

		switch (wHeader) {
		case SDX_RELOC_BLK:
			return getSDXBlockType() + " blk " + bSDXBlockNumber + " $" + HexUtility.getLongValueHexString(wBegin, 4)
					+ "-$" + HexUtility.getLongValueHexString(wEnd, 4) + " len " + size + " mem $"
					+ HexUtility.getByteValueHexString(bSDXControlByte);

		case SDX_FIX_UP_BLK:
			return "Fixups blk " + bSDXBlockNumber + " len " + size;

		case SDX_SYM_REQUIRED:
			return "SymReq fixup len " + padRight(String.valueOf(size), 4) + " " + sdxSymbol.trim();

		case SDX_SYM_DEFINED:
			return "SymDef blk " + bSDXBlockNumber + " ofs $" + HexUtility.getLongValueHexString(wBegin, 4) + " "
					+ sdxSymbol.trim();

		case ATARI_BINARY:
			return "StdBin " + HexUtility.getLongValueHexString(wBegin, 4) + "-$"
					+ HexUtility.getLongValueHexString(wEnd, 4) + " len " + HexUtility.getLongValueHexString(size, 4)
					+ " (" + size + ")";

		case SDX_FIXED_BLK:
			return "StdBlk $" + HexUtility.getLongValueHexString(wBegin, 4) + "-$"
					+ HexUtility.getLongValueHexString(wEnd, 4) + " len $"
					+ HexUtility.getLongValueHexString(size, 4) + " (" + size + ")";

		default:
			String kind = bBinary ? "Binary" : "Raw   ";
			return kind + " $" + HexUtility.getLongValueHexString(wBegin, 4) + "-$"
					+ HexUtility.getLongValueHexString(wEnd, 4) + " len $"
					+ HexUtility.getLongValueHexString(size, 4) + " (" + size + ")";
		}
	}

	private static String padRight(String value, int length) {
		StringBuilder builder = new StringBuilder(value);
		while (builder.length() < length) {
			builder.append(' ');
		}
		return builder.toString();
	}

	public boolean isSplittable() {
		return (isHeader(FileHeader.ATARI_BINARY) || isHeader(FileHeader.SDX_FIXED_BLK)) && !isEmpty();
	}

	public boolean canSplitAt(int offset) {
		return isSplittable() && offset > 0 && offset < getSize();
	}

	public void splitAt(int offset, Segment newSegment) {
		if (!canSplitAt(offset)) {
			throw new IllegalArgumentException("Invalid offset.");
		}
		newSegment.wHeader = wHeader;
		newSegment.wBegin = wBegin + offset;
		newSegment.wEnd = wEnd;
		newSegment.createMemoryBlockFromBeginToEnd();

		memoryBlock.copyTo(offset, newSegment.getSize(), newSegment.memoryBlock, 0);
		wEnd = wBegin + offset - 1;

		Iterator<Comment> it = comments.iterator();
		while (it.hasNext()) {
			Comment comment = it.next();
			int currentOffset = comment.getOffset();

			if (currentOffset >= offset) {
				it.remove();
				comment.setOffset(currentOffset - offset);
				newSegment.comments.add(comment);
			}
		}
	}

	public boolean isMergeable() {
		return isSplittable();
	}

	public boolean canMergeWith(Segment nextSegment) {
		if (isMergeable() && nextSegment.isMergeable() && wEnd + 1 == nextSegment.wBegin) {
			int newSize = getSize() + nextSegment.getSize();
			// Do not create segments with more than 64k.
			return newSize < MAX_SEGMENT_SIZE;
		}
		return false;
	}

	public void mergeWith(Segment nextSegment) {
		int size = getSize();
		int totalSize = size + nextSegment.getSize();

		if (totalSize > MAX_SEGMENT_SIZE) {
			throw new IllegalStateException("Merged segments must not exceed 64kb.");
		}

		MemoryBlock mergedBlock = new MemoryBlock();
		mergedBlock.create(totalSize);

		memoryBlock.copyTo(0, size, mergedBlock, 0);
		nextSegment.memoryBlock.copyTo(0, nextSegment.getSize(), mergedBlock, size);

		createMemoryBlockWithSize(totalSize);
		mergedBlock.copyTo(0, totalSize, memoryBlock, 0);

		wEnd = nextSegment.wEnd;

		for (Comment comment : nextSegment.comments) {
			Comment newComment = allocateComment();
			newComment.setOffset(size + comment.getOffset());
			newComment.setText(comment.getText());
		}

		// TODO: Transfer additional data (symbols/fixups/labels)?
	}

	public boolean isSDXRelocBlkWithData() {
		return isHeader(FileHeader.SDX_RELOC_BLK) && (bSDXControlByte & 0x80) == 0x00;
	}

	public boolean isSDXRelocBlkWithoutData() {
		return isHeader(FileHeader.SDX_RELOC_BLK) && (bSDXControlByte & 0x80) == 0x80;
	}

	public String getSDXBlockType() {
		switch (bSDXControlByte & 0x80) {
		case 0x00:
			return "RelBlk";
		case 0x80:
			return "RamBlk";
		default:
			return "";
		}
	}

	public String getSDXMemoryType() {
		switch (bSDXControlByte & 0x7f) {
		case 0x00:
			return "main";
		case 0x02:
			return "extended";
		default:
			return "";
		}
	}

	public void createMemoryBlockFromBeginToEnd() {
		if (wEnd < wBegin) {
			throw new IllegalStateException("End must not be before begin.");
		}
		createMemoryBlockWithSize(wEnd - wBegin + 1);
	}

	public void createMemoryBlockWithSize(int size) {
		clearMemoryBlock(); // Free anything that is already there.
		memoryBlock.create(size);
	}

	public void createMemoryBlockFromFile(int size, InputStream inputStream) throws IOException {
		createMemoryBlockWithSize(size);
		memoryBlock.readData(inputStream, size);
	}

	/** Reads this segment's data and type buffers from the pre-3.0 binary workspace format. */
	public void load14(InputStream inputStream) throws IOException {
		createMemoryBlockFromBeginToEnd();

		int size = getSize();

		memoryBlock.readData(inputStream, size);
		memoryBlock.readType(inputStream, size);
	}

	public void clearMemoryBlock() {
		memoryBlock.clear();
	}

	public int getData(int offset) {
		return memoryBlock.getDataAt(offset);
	}

	public int getWord(int offset) {
		return Memory.toWord(getData(offset), getData(offset + 1));
	}

	public void setData(int offset, int data) {
		memoryBlock.setDataAt(offset, data);
	}

	public void setData(int offset, byte[] data, int dataOffset, int dataSize) {
		memoryBlock.setDataAt(offset, data, dataOffset, dataSize);
	}

	public MemoryType getType(int offset) {
		return memoryBlock.getTypeAt(offset);
	}

	public boolean isType(int offset, MemoryType memoryType) {
		return getType(offset) == memoryType;
	}

	public boolean isUnknown(int offset) {
		if (!isType(offset, MemoryType.UNKNOWN)) {
			return false;
		}
		// Offset 0 is never a low/high byte.
		if (offset == 0) {
			return true;
		}
		// Not a low byte and not a high byte.
		return !isType(offset - 1, MemoryType.LOBYTE) && !isType(offset - 1, MemoryType.HIBYTE);
	}

	public void setType(int offset, MemoryType memoryType) {
		memoryBlock.setTypeAt(offset, memoryType);
	}

	public void setType(int offset, MemoryType memoryType, int size) {
		memoryBlock.setTypeAt(offset, memoryType, size);
	}

	public boolean containsAddress(int address) {
		return !isEmpty() && wBegin <= address && address <= wEnd;
	}

	@Override
	public void serializeTo(Element element) {
		Xml.setStringAttribute(element, "Title", title);

		Xml.setWordAttributeHex(element, "Header", wHeader.getValue());
		Xml.setWordAttributeHex(element, "Begin", wBegin);
		Xml.setWordAttributeHex(element, "End", wEnd);

		Xml.setBoolAttribute(element, "Binary", bBinary);
		Xml.setStringAttribute(element, "LabelPrefix", labelPrefix);

		Xml.setWordAttribute(element, "SDXFixUpSize", wSDXFixUpSize);
		Xml.setByteAttribute(element, "SDXBlockNumber", bSDXBlockNumber);
		Xml.setByteAttributeHex(element, "SDXControlByte", bSDXControlByte);
		Xml.setStringAttribute(element, "SDXSymbol", sdxSymbol);

		Xml.setStringAttribute(element, "ProcessorType", processorType.name());

		Element contentElement = Xml.addChildElement(element, "Content");
		memoryBlock.serializeTo(contentElement);

		Element commentsElement = Xml.addChildElement(element, "Comments");
		for (Comment comment : comments) {
			comment.serializeTo(Xml.addChildElement(commentsElement, "Comment"));
		}
	}

	@Override
	public void deserializeFrom(Element element) {
		clear();

		title = Xml.getStringAttribute(element, "Title", title);

		// Unlike the C++ version, which C-style casts an uninitialized/unvalidated
		// word into the FileHeader enum, this falls back to RAW if the attribute is
		// missing or does not match a known header value.
		int headerValue = Xml.getWordAttribute(element, "Header", wHeader.getValue());
		FileHeader header = FileHeader.valueOf(headerValue);
		wHeader = header != null ? header : FileHeader.RAW;
		wBegin = Xml.getWordAttribute(element, "Begin", wBegin);
		wEnd = Xml.getWordAttribute(element, "End", wEnd);

		bBinary = Xml.getBoolAttribute(element, "Binary", bBinary);
		labelPrefix = Xml.getStringAttribute(element, "LabelPrefix", labelPrefix);
		wSDXFixUpSize = Xml.getWordAttribute(element, "SDXFixUpSize", wSDXFixUpSize);
		bSDXBlockNumber = Xml.getByteAttribute(element, "SDXBlockNumber", bSDXBlockNumber);
		bSDXControlByte = Xml.getByteAttribute(element, "SDXControlByte", bSDXControlByte);
		sdxSymbol = Xml.getStringAttribute(element, "SDXSymbol", sdxSymbol);

		String processorTypeString = Xml.getStringAttribute(element, "ProcessorType", "");
		try {
			processorType = ProcessorType.valueOf(processorTypeString);
		} catch (IllegalArgumentException e) {
			processorType = ProcessorType.UNKNOWN;
		}
		if (processorType == ProcessorType.UNKNOWN) {
			processorType = ProcessorType.MOS6502;
		}

		Element contentElement = Xml.getFirstChildElement(element, "Content");
		if (contentElement != null) {
			memoryBlock.deserializeFrom(contentElement);
		}

		Element commentsElement = Xml.getFirstChildElement(element, "Comments");
		if (commentsElement != null) {
			Element commentElement = Xml.getFirstChildElement(commentsElement, "Comment");
			while (commentElement != null) {
				Comment comment = allocateComment();
				comment.deserializeFrom(commentElement);
				commentElement = Xml.getNextSiblingElement(commentElement);
			}
		}
	}

	public void clearComments() {
		comments.clear();
	}

	public Comment allocateComment() {
		Comment comment = new Comment();
		comments.add(comment);
		return comment;
	}

	public String findComment(int offset) {
		for (Comment comment : comments) {
			if (comment.getOffset() == offset) {
				return comment.getText();
			}
		}
		return "";
	}

	public void deleteComments(int offset, int size) {
		Iterator<Comment> it = comments.iterator();
		while (it.hasNext()) {
			Comment comment = it.next();
			if (comment.getOffset() >= offset && comment.getOffset() < offset + size) {
				it.remove();
			}
		}
	}

	/**
	 * Allocates an SDX system symbol fix-up. If the symbol is "PRINTF" and
	 * immediately follows a JSR opcode, the format string bytes are marked
	 * as {@link MemoryType#STRING} and their trailing argument bytes as
	 * {@link MemoryType#LABEL}.
	 */
	public void allocateSymbol(int address, String name) {
		if (isEmpty()) {
			throw new IllegalStateException("Cannot allocate symbol in empty segment.");
		}
		symbols.add(new Symbol(address, name));

		if (address >= wBegin && address < wEnd) {
			int offset = address - wBegin;

			if (isUnknown(offset) || isType(offset, MemoryType.CODE)) {
				setType(offset++, MemoryType.SYMBOL);
				setType(offset++, MemoryType.SYMBOL);

				// Check if the fix-up is for a "jsr PRINTF". Then set the type of the
				// following bytes to STRING until the end-of-string character ('\0').
				if (offset > 2 && getData(offset - 3) == 0x20) { // JSR opcode.
					if ("PRINTF".equals(name)) {
						int bytesAsLabels = 0;
						PrintfState printState = PrintfState.TEXT;

						while (offset < getSize()) {
							if (isUnknown(offset)) {
								setType(offset, MemoryType.STRING);
							}

							int byteValue = getData(offset);
							offset++;
							if (byteValue == 0) {
								break;
							}

							char c = (char) byteValue;
							switch (printState) {
							case TEXT:
								if (c == '%') {
									printState = PrintfState.PERCENT;
								}
								break;

							case PERCENT:
								if (c == '%') {
									printState = PrintfState.TEXT;
								} else if (isPrintfConversion(c)) {
									printState = PrintfState.TEXT;
									bytesAsLabels += 2;
								} else {
									printState = PrintfState.FORMAT;
									bytesAsLabels += 2;
								}
								break;

							case FORMAT:
								if (c == '*') {
									bytesAsLabels += 2;
								} else if (isPrintfConversion(c)) {
									printState = PrintfState.TEXT;
								}
								break;
							}
						}

						while (bytesAsLabels > 0 && offset < getSize()) {
							if (isType(offset, MemoryType.UNKNOWN)) {
								setType(offset, MemoryType.LABEL);
							}
							offset++;
							bytesAsLabels--;
						}
					}
				}
			}
		}
	}

	private static boolean isPrintfConversion(char c) {
		return c == 'c' || c == 's' || c == 'p' || c == 'x' || c == 'b' || c == 'd' || c == 'e' || c == 'l'
				|| c == 't';
	}

	public Symbol findSymbol(int address) {
		for (Symbol symbol : symbols) {
			if (symbol.getAddress() == address) {
				return symbol;
			}
		}
		return null;
	}

	public void clearFixups() {
		fixups.clear();
	}

	/**
	 * Allocates a fix-up address in this segment, keeping {@link #fixups}
	 * sorted by address, and marks the corresponding word as
	 * {@link MemoryType#FIXUP} if it is currently unknown or code.
	 */
	public Fixup allocateFixup(int labelSegmentIndex, Segment labelSegment, int address) {
		if (memoryBlock.isEmpty()) {
			return null;
		}

		Fixup fixup = new Fixup();
		fixup.setAddress(address);
		fixup.setLabelSegmentIndex(labelSegmentIndex);

		int insertIndex;
		if (fixups.isEmpty() || address < fixups.get(0).getAddress()) {
			insertIndex = 0;
		} else {
			insertIndex = fixups.size();
			// Starts at 1, not 0: a tie with the first entry's address is intentionally
			// not matched here, so it is inserted right after that entry rather than before it.
			for (int i = 1; i < fixups.size(); i++) {
				if (fixups.get(i).getAddress() >= address) {
					insertIndex = i;
					break;
				}
			}
		}
		fixups.add(insertIndex, fixup);

		if (address >= wBegin && address < wEnd) { // "<" because it must be a word at offset/offset+1.
			int offset = address - wBegin;

			if (isUnknown(offset) || isType(offset, MemoryType.CODE)) {
				setType(offset, MemoryType.FIXUP);
				setType(offset + 1, MemoryType.FIXUP);
			}

			int adjustAddress = getWord(offset);
			labelSegment.fixupAddressLabels.allocateAddressLabel(adjustAddress);
		}
		return fixup;
	}

	public Fixup findFixup(int address) {
		for (Fixup fixup : fixups) {
			if (fixup.getAddress() == address) {
				return fixup;
			}
		}
		return null;
	}

	public AddressLabelList getFixupAddressLabels() {
		return fixupAddressLabels;
	}

	public AddressLabelList getAddressLabels() {
		return addressLabels;
	}

	/**
	 * Defines an address label: if there is a matching fix-up address label,
	 * it is set to aligned; otherwise a matching code address label is set
	 * to aligned.
	 */
	public void defineAddressLabel(int address) {
		AddressLabel addressLabel = fixupAddressLabels.findMutableAddressLabel(address);
		if (addressLabel != null) {
			addressLabel.setAligned(true);
			return;
		}

		addressLabel = addressLabels.findMutableAddressLabel(address);
		if (addressLabel != null) {
			addressLabel.setAligned(true);
		}
	}

	public void alignAddressLabels(int address) {
		fixupAddressLabels.alignAddressLabels(address);
		addressLabels.alignAddressLabels(address);
	}

	public void alignRamBlkAddresses() {
		if (isSDXRelocBlkWithoutData()) {
			fixupAddressLabels.alignNearestAddress();
			addressLabels.alignNearestAddress();
		}
	}

	public void setFirstLineNumber(int firstLineNumber) {
		this.firstLineNumber = firstLineNumber;
	}

	/** @return the first line in the CODE_SECTION part of the listing of this segment; 0 if not set at all. */
	public int getFirstLineNumber() {
		return firstLineNumber;
	}

	/** Formats a label as "&lt;prefix&gt;L&lt;address&gt;", e.g. "L0480". */
	public static String formatLabel(String labelPrefix, int address) {
		return labelPrefix + "L" + HexUtility.getLongValueHexString(address, 4);
	}

	/** Formats a default label as "S&lt;segmentIndex&gt;L&lt;address&gt;", e.g. "S001L0480". */
	public static String formatDefaultLabel(int segmentIndex, int address) {
		return "S" + HexUtility.getLongValueHexString(segmentIndex, 3) + "L"
				+ HexUtility.getLongValueHexString(address, 4);
	}

	/** Formats a label with an offset, e.g. "L0480+1". */
	public static String formatLabelWithOffset(String labelPrefix, int address, int offset) {
		return formatLabel(labelPrefix, address) + "+" + offset;
	}

	/** Formats a default label with an offset, e.g. "S001L0480+1". */
	public static String formatDefaultLabelWithOffset(int segmentIndex, int address, int offset) {
		return formatDefaultLabel(segmentIndex, address) + "+" + offset;
	}
}
