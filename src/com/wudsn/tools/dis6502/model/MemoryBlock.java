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

import org.w3c.dom.Element;

/**
 * A block of memory: the raw byte content plus, in parallel, a
 * {@link MemoryType} for every byte.
 * <p>
 * Ported from MemoryBlock.h / MemoryBlock.cpp. Unlike the C++ version, this
 * exposes its content directly as plain {@code byte[]} arrays instead of
 * through a {@code ByteSequence}/{@code ByteArray} abstraction - Java's
 * {@code byte[]} together with {@code System.arraycopy}/{@code
 * java.util.Arrays} already covers everything that abstraction existed for
 * in C++.
 *
 * @author Peter Dell
 */
public final class MemoryBlock implements Xml.Serializable {

	private int size;
	private byte[] data;
	private byte[] type;

	public void create(int size) {
		this.size = size;
		data = new byte[size];
		type = new byte[size]; // MemoryType.UNKNOWN has ordinal 0, so the zero-filled array already matches.
	}

	public void clear() {
		data = null;
		type = null;
		size = 0;
	}

	public int getSize() {
		return size;
	}

	public boolean isEmpty() {
		return size == 0;
	}

	/** Gets the raw byte content, mutable, exactly {@link #getSize()} bytes long. */
	public byte[] getData() {
		return data;
	}

	public int getDataAt(int offset) {
		return data[offset] & 0xFF;
	}

	public void setDataAt(int offset, int value) {
		data[offset] = (byte) value;
	}

	public void setDataAt(int offset, byte[] source, int sourceOffset, int dataSize) {
		System.arraycopy(source, sourceOffset, data, offset, dataSize);
	}

	public void readData(InputStream inputStream, int size) throws IOException {
		readFully(inputStream, data, 0, size);
	}

	public void writeData(OutputStream outputStream) throws IOException {
		outputStream.write(data, 0, size);
	}

	/** Gets the raw type content, mutable, exactly {@link #getSize()} bytes long. */
	public byte[] getType() {
		return type;
	}

	/**
	 * Decodes the type byte at {@code offset} as a {@link MemoryType}. Unlike
	 * the C++ version's {@code (MemoryType)type.at(offset)} - an unchecked
	 * C-style cast that silently tolerates a byte value outside the enum's
	 * defined constants, since it just never matches any named case in a
	 * later comparison - {@link MemoryType#VALUES}{@code [byte]} would throw
	 * for the same out-of-range byte. This returns {@link MemoryType#UNKNOWN}
	 * instead, which is observably identical: {@code UNKNOWN} likewise never
	 * matches any of the specific constants callers compare against. This
	 * matters in practice: the byte immediately after a {@link
	 * MemoryType#LOBYTE}/{@link MemoryType#HIBYTE} byte has its type slot
	 * repurposed to hold the missing address byte's raw value (see {@link
	 * MemoryType}'s javadoc), not a real {@code MemoryType} ordinal, and nothing
	 * skips decoding it before the disassembler moves on to read it as data.
	 */
	public MemoryType getTypeAt(int offset) {
		int value = type[offset] & 0xFF;
		return value < MemoryType.VALUES.length ? MemoryType.VALUES[value] : MemoryType.UNKNOWN;
	}

	public void setTypeAt(int offset, MemoryType memoryType) {
		type[offset] = (byte) memoryType.ordinal();
	}

	public void setTypeAt(int offset, MemoryType memoryType, int size) {
		Arrays.fill(type, offset, offset + size, (byte) memoryType.ordinal());
	}

	public void readType(InputStream inputStream, int size) throws IOException {
		readFully(inputStream, type, 0, size);
	}

	public void writeType(OutputStream outputStream) throws IOException {
		outputStream.write(type, 0, size);
	}

	public void copyTo(int startOffset, int size, MemoryBlock target, int targetOffset) {
		System.arraycopy(data, startOffset, target.data, targetOffset, size);
		System.arraycopy(type, startOffset, target.type, targetOffset, size);
	}

	@Override
	public void serializeTo(Element element) {
		Xml.setSizeAttributeHex(element, "Size", size);
		Xml.setByteArrayAttributeHex(element, "Data", data);
		Xml.setByteArrayAttributeHex(element, "Type", type);
	}

	@Override
	public void deserializeFrom(Element element) {
		clear();
		int newSize = (int) Xml.getSizeAttribute(element, "Size", 0);

		// "Dump" is the attribute name used by file format version 3.6.
		byte[] newData = Xml.getByteArrayAttribute(element, "Data");
		if (newData == null) {
			newData = Xml.getByteArrayAttribute(element, "Dump");
		}
		if (newData == null) {
			throw new IllegalStateException("Attribute \"Data\" of memory block is missing.");
		}
		if (newData.length != newSize) {
			throw new IllegalStateException("Size of content array is different from size of memory block.");
		}

		byte[] newType = Xml.getByteArrayAttribute(element, "Type");
		if (newType == null || newType.length != newSize) {
			throw new IllegalStateException("Size of type array is different from size of memory block.");
		}

		create(newSize);
		System.arraycopy(newData, 0, data, 0, newSize);
		System.arraycopy(newType, 0, type, 0, newSize);
	}

	/** Reads exactly {@code length} bytes, throwing {@link EOFException} if the stream ends first. */
	private static void readFully(InputStream inputStream, byte[] buffer, int offset, int length)
			throws IOException {
		int totalRead = 0;
		while (totalRead < length) {
			int read = inputStream.read(buffer, offset + totalRead, length - totalRead);
			if (read < 0) {
				throw new EOFException(
						"Expected " + length + " bytes but the stream ended after " + totalRead + ".");
			}
			totalRead += read;
		}
	}
}
