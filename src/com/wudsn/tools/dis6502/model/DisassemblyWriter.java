/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

import com.wudsn.tools.base.common.HexUtility;

/**
 * Used in Pass 4: accumulates consecutive .BYTE/.WORD/.SBYTE/.DS/label
 * entries for the listing, flushing them into a single disassembly line
 * once the per-line limit is reached, the entry kind changes, or a real
 * instruction line needs to be added.
 * <p>
 * Reaches {@link Disassembly}'s {@code lineWriter}/{@code absoluteAddress}/
 * {@code markSize} fields and {@code addLine}/{@code addLineWriter} methods,
 * which are package-private for that - the same pattern as {@link
 * SegmentList}/{@link SegmentListInserter}.
 *
 * @author Peter Dell
 */
public final class DisassemblyWriter {

	private final Disassembly disassembly;
	private final Profile profile;
	private final int returnCharacter;
	private final char quoteCharacter;

	private int disNbBytes;
	private MemoryType memoryType = MemoryType.UNKNOWN;
	private final StringBuilder bytesLine = new StringBuilder();

	public DisassemblyWriter(Disassembly disassembly, Workspace workspace) {
		this.disassembly = disassembly;
		this.returnCharacter = workspace.getComputerSystem().getReturnCharacter();
		this.profile = workspace.getProfile();
		this.quoteCharacter = profile.quoteForASCIIStrings.charAt(0);
	}

	private String getLineBuffer() {
		return disassembly.lineWriter.getLineBuffer();
	}

	/** Returns true if the given byte is allowed to be represented as a string; false if it should appear as a hex byte. */
	public boolean isByteAllowedInString(int value) {
		if (value == 0 || value == returnCharacter || value == quoteCharacter) {
			return false;
		}

		// TODO: This actually depends on the character set of the computer system.
		if (profile.showNonASCIIChararactersAsBytes
				&& (value < 0x20 || value >= 0x7D || value == 0x60 || value == 0x7B)) {
			return false;
		}

		return true;
	}

	/** Adds a line of code to the disassembly listing but, first, flushes any pending .byte directive. */
	public void flushAndAddLine(int address) {
		flushBytes();
		disassembly.absoluteAddress = address;
		disassembly.addLineWriter();
	}

	/** Adds a line of code to the disassembly listing but, first, flushes any pending .byte directive. */
	public void flushAndAddLineWithComment(DisassemblyOpcodeBuffer opcodeBuffer, int opcodeSize, int address) {
		if (profile.showOpcodeAsComment) {
			DisassemblyLineWriter lineWriter = disassembly.lineWriter;
			lineWriter.spaceUntil34();
			lineWriter.string(profile.commentPrefix);
			lineWriter.space();
			opcodeBuffer.write(lineWriter, opcodeSize);
		}
		flushAndAddLine(address);
	}

	/** Flushes all collected .DS/.BYTE/.SBYTE/.WORD directives into the disassembly listing. */
	public void flushBytes() {
		if (disNbBytes > 0) {
			int savedSize = disassembly.markSize;

			switch (memoryType) {
			case STRING:
			case SBYTE:
				bytesLine.append(profile.quoteForASCIIStrings);
				break;
			case STORE:
				bytesLine.append(disNbBytes);
				break;
			default:
				break;
			}

			disassembly.markSize = disNbBytes;
			disassembly.addLine(bytesLine.toString());
			bytesLine.setLength(0);

			disassembly.markSize = savedSize >= disNbBytes ? savedSize - disNbBytes : 0;

			disNbBytes = 0;
			memoryType = MemoryType.UNKNOWN;
		}
	}

	/** Skips another byte with a .DS directive. */
	public void dumpStore() {
		if (memoryType != MemoryType.STORE) {
			flushBytes();
		}

		memoryType = MemoryType.STORE;

		if (disNbBytes == 0) {
			bytesLine.append(getLineBuffer()).append(profile.directiveDS).append(' ');
		}

		disNbBytes++;
	}

	/** Saves a byte in a temporary buffer, to be written in a .BYTE directive in hexadecimal format. */
	public void dumpByte(int value) {
		if (disNbBytes >= profile.directiveBYTENumberOfBytesPerLine || memoryType != MemoryType.BYTE) {
			flushBytes();
		}

		memoryType = MemoryType.BYTE;

		if (profile.useHexNotation) {
			if (disNbBytes == 0) {
				bytesLine.append(getLineBuffer()).append(profile.directiveBYTE).append(' ')
						.append(profile.hexNotationPrefix).append(HexUtility.getByteValueHexString(value));
			} else {
				bytesLine.append(profile.directiveBYTESeparator).append(profile.hexNotationPrefix)
						.append(HexUtility.getByteValueHexString(value));
			}
		} else {
			if (disNbBytes == 0) {
				bytesLine.append(getLineBuffer()).append(profile.directiveBYTE).append(' ').append(value);
			} else {
				bytesLine.append(profile.directiveBYTESeparator).append(value);
			}
		}

		disNbBytes++;
	}

	/** Saves a word in a temporary buffer, to be written in a .WORD directive in hexadecimal format. */
	public void dumpWord(int value) {
		if (disNbBytes >= profile.directiveWORDNumberOfWordsPerLine * 2 || memoryType != MemoryType.WORD) {
			flushBytes();
		}

		memoryType = MemoryType.WORD;

		if (profile.useHexNotation) {
			if (disNbBytes == 0) {
				bytesLine.append(getLineBuffer()).append(profile.directiveWORD).append(' ')
						.append(profile.hexNotationPrefix).append(HexUtility.getLongValueHexString(value, 4));
			} else {
				bytesLine.append(profile.directiveBYTESeparator).append(profile.hexNotationPrefix)
						.append(HexUtility.getLongValueHexString(value, 4));
			}
		} else {
			if (disNbBytes == 0) {
				bytesLine.append(getLineBuffer()).append(profile.directiveWORD).append(' ').append(value);
			} else {
				bytesLine.append(profile.directiveBYTESeparator).append(value);
			}
		}

		disNbBytes += 2;
	}

	/** Saves a character in a temporary buffer, to be written in a .BYTE directive in string format (ASCII code). */
	public void dumpString(int value) {
		if (!isByteAllowedInString(value)) {
			dumpByte(value);
			return;
		}

		if (disNbBytes >= profile.directiveBYTENumberOfCharactersPerString || memoryType != MemoryType.STRING) {
			flushBytes();
		}

		memoryType = MemoryType.STRING;

		if (disNbBytes == 0) {
			bytesLine.append(getLineBuffer()).append(profile.directiveBYTE).append(' ')
					.append(profile.quoteForASCIIStrings).append((char) value);
		} else {
			bytesLine.append((char) value);
		}

		disNbBytes++;
	}

	/** Saves a character in a temporary buffer, to be written in a .SBYTE directive in string format (internal code). */
	public void dumpSByte(int value) {
		int internal = value;

		if (internal < 64) {
			internal += 32;
		} else if (internal < 96) {
			internal -= 64;
		} else if (internal >= 128 && internal < 128 + 64) {
			internal += 32;
		} else if (internal >= 128 + 64 && internal < 128 + 96) {
			internal -= 64;
		}

		if (internal == returnCharacter || internal == 0x22 || internal == 0) {
			dumpByte(value);
			return;
		}

		if (disNbBytes >= profile.directiveBYTENumberOfCharactersPerString || memoryType != MemoryType.SBYTE) {
			flushBytes();
		}

		memoryType = MemoryType.SBYTE;

		if (disNbBytes == 0) {
			bytesLine.append(getLineBuffer()).append(profile.directiveSBYTE).append(' ')
					.append(profile.quoteForASCIIStrings).append((char) internal);
		} else {
			bytesLine.append((char) internal);
		}

		disNbBytes++;
	}

	/** Saves a label in a temporary buffer, to be written in a .WORD directive. */
	public void dumpLabel(String label) {
		if (disNbBytes >= profile.directiveWORDNumberOfWordsPerLine * 2 || memoryType != MemoryType.LABEL) {
			flushBytes();
		}

		memoryType = MemoryType.LABEL;

		if (disNbBytes == 0) {
			bytesLine.append(getLineBuffer()).append(profile.directiveWORD).append(' ').append(label);
		} else {
			bytesLine.append(profile.directiveBYTESeparator).append(label);
		}

		disNbBytes += 2;
	}
}
