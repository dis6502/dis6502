/*
 * Copyright (C) 2025 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

import java.util.Iterator;

import com.wudsn.tools.base.common.HexUtility;

/**
 * One disassembly run: decodes each segment's memory into {@link
 * DisassemblyLine}s, in six passes (label discovery via {@link Pass1},
 * label reservation, relative-label generation, listing generation,
 * non-code segments, cleanup).
 * <p>
 * Ported from Disassembly.h / Disassembly.cpp. This first slice covers
 * everything through Pass 3 (label discovery/reservation/alignment) plus
 * the equate/label/comment line-generation helpers; Pass 4 and Pass 5 (the
 * actual listing generation) need {@code DisassemblyWriter}, which is not
 * ported yet, so {@code Pass4}, {@code Pass5}, {@code SetPass}, {@code
 * StartDisassembly}, and {@code DisassembleInternal} (the pass-by-pass
 * orchestration) are deferred to that follow-up. {@code DebugSection} (a
 * pure diagnostic dump to the C++ {@code Debug} logger) is not ported.
 * <p>
 * Design deviations:
 * <ul>
 * <li>The C++ source uses preprocessor macros ({@code DIS_GET_NEXT_BYTE},
 * {@code DIS_GET_BYTE_IN_PASS_2}, {@code DIS_GET_WORD_IN_PASS_2}) whose
 * {@code break}/{@code return} statements unwind out of the current
 * instruction's processing (to the next loop iteration) or out of the
 * whole pass (on cancellation), by relying on exactly where they are
 * textually inlined relative to enclosing {@code switch} statements. Java
 * has no equivalent macro-based control-flow trick, so this uses two
 * lightweight, stack-trace-free exceptions instead - {@link
 * AbortInstructionException} (segment boundary crossed mid-instruction:
 * caught by the pass's loop, which moves on to the next iteration) and
 * {@link DisassemblyCancelledException} (caught by the pass method itself,
 * which returns immediately) - thrown by {@link #nextByteInPass23()}/
 * {@link #getNextByte()} at exactly the points the C++ macros would have
 * exited.</li>
 * <li>{@code segmentIndex}/{@code pc} are C++ locals threaded through
 * {@code DisInit}/{@code DisGetNextByte} by reference; since Java has no
 * reference parameters, and every call site within one pass needs to see
 * the same up-to-date position, they are instance fields here instead
 * (reset by {@link #disInit()} at the start of each pass, same as the C++
 * locals were re-initialized by each pass's own {@code DisInit} call).
 * {@code cDisByteType} is likewise promoted to the {@link #byteType}
 * field.</li>
 * </ul>
 *
 * @author Peter Dell
 */
public final class Disassembly {

	/** Matches the C++ {@code dis_k::NO_DUMP} constant. */
	private static final int NO_DUMP = 0xFFFF;

	/** Thrown to unwind to the next pass iteration when a segment boundary is crossed mid-instruction. */
	private static final class AbortInstructionException extends RuntimeException {
		private AbortInstructionException() {
			super(null, null, false, false);
		}
	}

	/** Thrown to unwind out of the current pass when disassembly is cancelled. */
	private static final class DisassemblyCancelledException extends RuntimeException {
		private DisassemblyCancelledException() {
			super(null, null, false, false);
		}
	}

	private static final AbortInstructionException ABORT_INSTRUCTION = new AbortInstructionException();
	private static final DisassemblyCancelledException DISASSEMBLY_CANCELLED = new DisassemblyCancelledException();

	private Workspace workspace;
	private Profile profile;
	private DisassemblyResult result;

	private DisassemblyProgressMonitor disassemblyProgressMonitor;
	private int pass;

	private MemoryBlockIterator memoryBlockIterator;
	private boolean disNewSegment;

	private final DisassemblyOpcodeBuffer opcodeBuffer = new DisassemblyOpcodeBuffer();

	private int markNextSegmentIndex;
	private int markSegmentIndex;
	private int markOffset;
	private int markSize;

	private final DisassemblyLineWriter lineWriter = new DisassemblyLineWriter();

	// Initialized in disInit() and used in addLine().
	private int absoluteAddress;
	private int systemAddress = 0x1234;

	// Current disassembly position and the type of the byte last read; see the class-level "Design deviations" note.
	private int segmentIndex;
	private int pc;
	private MemoryType byteType;
	private int lastByte;

	public void setWorkspace(Workspace workspace) {
		this.workspace = workspace;
		this.profile = workspace.getProfile();
		this.result = workspace.getDisassemblyResult();
	}

	public void setProgressMonitor(DisassemblyProgressMonitor disassemblyProgressMonitor) {
		this.disassemblyProgressMonitor = disassemblyProgressMonitor;
	}

	/**
	 * Checks if a byte at a given segment/offset is the immediate operand of
	 * an {@code Immediate}-mode instruction, and if so, its value and the
	 * memory type of whichever of the two "assumed word" bytes carries the
	 * user's input (see {@link MemoryType#LOBYTE}/{@link MemoryType#HIBYTE}).
	 *
	 * @param immediateValue      single-element out parameter (index 0).
	 * @param immediateMemoryType single-element out parameter (index 0).
	 */
	public static boolean isInstructionWithImmediate(Workspace workspace, int segmentIndex, int offset,
			int[] immediateValue, MemoryType[] immediateMemoryType) {
		immediateValue[0] = 0;
		immediateMemoryType[0] = MemoryType.UNKNOWN;

		SegmentList segmentList = workspace.getSegmentList();
		Segment opcodeSegment = segmentList.getSegment(segmentIndex);

		// The code below only depends on the segment and the offset.
		if (opcodeSegment.bBinary && !opcodeSegment.isEmpty()) {
			int opcode = opcodeSegment.memoryBlock.getDataAt(offset);
			InstructionSet instructionSet = workspace.getInstructionSet(opcodeSegment.processorType);

			if (instructionSet.getInstruction(opcode).getOperandMode() == OperandMode.Immediate) {
				immediateValue[0] = opcodeSegment.memoryBlock.getDataAt(offset + 1);

				// The type of the immediate opcode before the known byte is set to
				// MemoryType.LOBYTE or MemoryType.HIBYTE. The user input for the unknown
				// byte in the assumed word is stored in the type of the operand.
				MemoryType type = opcodeSegment.getType(offset);
				if (type != MemoryType.LOBYTE && type != MemoryType.HIBYTE) {
					type = opcodeSegment.getType(offset + 1);
				}
				immediateMemoryType[0] = type;
				return true;
			}
		}
		return false;
	}

	// ------------------------------------------------------------------
	// Higher level operations.
	// ------------------------------------------------------------------

	private void addLabelWithAddress(String label, int address, DisassemblySectionType disassemblySectionType,
			int systemAddr, String comment) {
		lineWriter.clear().string(label).alignInstructions().string(profile.directiveEQU).space().address(address);

		if (!comment.isEmpty()) {
			lineWriter.comment(comment);
		}

		systemAddress = disassemblySectionType == DisassemblySectionType.SYSTEM_EQUATES ? systemAddr : 0;

		addLineWriter(disassemblySectionType);
	}

	private void addLabel(String label, int address, DisassemblySectionType disassemblySectionType, String comment) {
		addLabelWithAddress(label, address, disassemblySectionType, address, comment);
	}

	/** Adds a label which is defined as a value in the SYSTEM_EQUATES file. */
	private void addLabelValue(String label, int address, DisassemblySectionType disassemblySectionType,
			String comment) {
		int systemAddr = disassemblySectionType == DisassemblySectionType.SYSTEM_EQUATES ? 0xFFFF : 0;
		addLabelWithAddress(label, address, disassemblySectionType, systemAddr, comment);
	}

	private void addComment(String comment, DisassemblySectionType disassemblySectionType) {
		lineWriter.clear().comment(comment);

		systemAddress = disassemblySectionType == DisassemblySectionType.SYSTEM_EQUATES ? 0xFFFF : 0;
		addLineWriter(disassemblySectionType);
	}

	private void generateUserComment(int segmentIndex, int offset, int size) {
		Segment segment = workspace.getSegmentList().getSegment(segmentIndex);

		int endOffset = offset + size;
		while (offset < endOffset) {
			String comment = segment.findComment(offset);
			if (!comment.isEmpty()) {
				addUserComment(comment, DisassemblySectionType.CODE_LINES);
			}
			offset++;
		}
	}

	/** Adds a user comment into the buffer. The comment is split into several lines if needed. */
	private void addUserComment(String comment, DisassemblySectionType disassemblySectionType) {
		int oldMarkNextSegmentIndex = markNextSegmentIndex;
		int oldMarkSegmentIndex = markSegmentIndex;
		int oldMarkOffset = markOffset;
		int oldMarkSize = markSize;

		for (String line : comment.split("\n", -1)) {
			String text = line.endsWith("\r") ? line.substring(0, line.length() - 1) : line;
			markSize = 0;
			systemAddress = 0;
			addLine(profile.commentPrefix + " " + text, disassemblySectionType);
		}

		markNextSegmentIndex = oldMarkNextSegmentIndex;
		markSegmentIndex = oldMarkSegmentIndex;
		markOffset = oldMarkOffset;
		markSize = oldMarkSize;
	}

	/** Saves the position of a new segment in the disassembly listing. Line 1 is the first line of code. */
	private void setSegmentFirstLineNumber(int segmentIndex) {
		int lineCount = 0;
		DisassemblySection section = result.getSection(DisassemblySectionType.CODE_LINES);
		if (section != null) {
			lineCount += section.getLineCount();
		}

		workspace.getSegmentList().getSegment(segmentIndex).setFirstLineNumber(lineCount + 1);
	}

	/**
	 * Adjusts the position of all segments to their absolute line number in
	 * the disassembly listing. {@link #setSegmentFirstLineNumber} saved a
	 * line number relative to the start of code; this adds, to every
	 * segment's position, the number of lines used by labels.
	 */
	private void adjustSegmentFirstLineNumber() {
		DisassemblySectionType[] sectionTypes = { DisassemblySectionType.SYSTEM_EQUATES,
				DisassemblySectionType.USER_EQUATES, DisassemblySectionType.CODE_EQUATES };
		int equatesLineCount = 0;
		for (DisassemblySectionType sectionType : sectionTypes) {
			DisassemblySection section = result.getSection(sectionType);
			if (section != null) {
				equatesLineCount += section.getLineCount();
			}
		}

		SegmentList segmentList = workspace.getSegmentList();
		// Plus the equates for the code section, plus 3 for the "; Start of code ..." prelude.
		int offset = equatesLineCount + 3;
		for (int segmentIndex = 0; segmentIndex < segmentList.getCount(); segmentIndex++) {
			Segment segment = segmentList.getSegment(segmentIndex);
			int localLineNumber = segment.getFirstLineNumber();
			int globalLineNumber = localLineNumber + offset;
			segment.setFirstLineNumber(globalLineNumber);
		}
	}

	/** Sets the referenced flag on lines for SYSTEM_EQUATES without offset (sta LABEL). */
	private void setSystemEquateLinesReferencedBySystemAddress() {
		// TODO: Currently the referenced check does not distinguish the type of access.
		// Therefore if $80 is referenced, ZP and DL constants are equally referenced.
		int anyAccess = LabelAccess.IMMEDIATE | LabelAccess.READ_WRITE;

		EquateList systemEquateList = workspace.getSystemEquateList();
		for (Iterator<DisassemblyLine> i = result.createLineIterator(DisassemblySectionType.SYSTEM_EQUATES); i
				.hasNext();) {
			DisassemblyLine line = i.next();
			line.referenced = systemEquateList.isEquateAddressReferenced(line.systemAddress, anyAccess);
		}
	}

	/** Sets the referenced flag on the nearest SYSTEM_EQUATES line with address (sta LABEL+n). */
	private void setNearestSystemEquateLineReferencedByAddress(int address) {
		DisassemblyLine nearestLine = null;

		for (Iterator<DisassemblyLine> i = result.createLineIterator(DisassemblySectionType.SYSTEM_EQUATES); i
				.hasNext();) {
			DisassemblyLine line = i.next();

			if (line.systemAddress <= address
					&& (nearestLine == null || nearestLine.systemAddress < line.systemAddress)) {
				nearestLine = line;
			}
		}

		if (nearestLine != null) {
			nearestLine.referenced = true;
		}
	}

	/** Adds all symbol definitions in the CODE_EQUATES section for SDX binaries. */
	private void generateSDXSymbolDefinitions() {
		SegmentList segmentList = workspace.getSegmentList();
		for (int segmentIndex = 0; segmentIndex < segmentList.getCount(); segmentIndex++) {
			Segment segment = segmentList.getSegment(segmentIndex);
			if (segment.isHeader(FileHeader.SDX_SYM_REQUIRED)) {
				StringBuilder buffer = new StringBuilder();
				buffer.append(segment.sdxSymbol);

				if (profile.alignInstructions) {
					for (int i = 12 - segment.sdxSymbol.length(); i > 0; i--) {
						buffer.append(' ');
					}
				} else {
					buffer.append(' ');
				}

				buffer.append(profile.showLowerCaseInstructions ? "smb" : "SMB").append(' ')
						.append(profile.quoteForASCIIStrings).append(segment.sdxSymbol)
						.append(profile.quoteForASCIIStrings);

				addLine(buffer.toString(), DisassemblySectionType.CODE_EQUATES);
			}
		}
	}

	/** Gives the user a chance to click a "Cancel" button. */
	private boolean isCancelled() {
		if (disassemblyProgressMonitor.isCancelled()) {
			result.clear();
			return true;
		}
		return false;
	}

	private void createMemoryBlockIterator(Segment segment) {
		memoryBlockIterator = new MemoryBlockIterator(segment.memoryBlock);
	}

	private void clearMemoryBlockIterator() {
		memoryBlockIterator = null;
	}

	// ------------------------------------------------------------------
	// Line operations.
	// ------------------------------------------------------------------

	/**
	 * Appends one line to {@code section}, then advances {@link
	 * #markSegmentIndex}/{@link #markOffset} to track the next line's
	 * position - replaces the C++ version's {@code DIS_BUFFER}-based
	 * {@code AddLineInBuffer} (see {@link DisassemblySection}).
	 */
	private void addLineInSection(DisassemblyLine templateLine, String text, DisassemblySection section) {
		section.addLine(templateLine, text);

		// Transition to a different segment.
		if (markSegmentIndex != markNextSegmentIndex) {
			if (markSegmentIndex == SegmentList.NO_SEGMENT_INDEX) {
				markOffset = 0;
				markSegmentIndex = markNextSegmentIndex;
			} else {
				Segment markSegment = workspace.getSegmentList().getSegment(markSegmentIndex);
				int markSegmentSize = markSegment.getSize();

				if (markOffset + markSize >= markSegmentSize) {
					if (markNextSegmentIndex != SegmentList.NO_SEGMENT_INDEX && markSegmentSize > 0) {
						markOffset = markOffset + markSize - markSegmentSize;
					} else {
						markOffset = NO_DUMP;
					}
					markSegmentIndex = markNextSegmentIndex;
				} else {
					markOffset += markSize;
				}
			}
		} else {
			markOffset += markSize;
		}

		markSize = 0;
	}

	private void addEmptyCommentLine() {
		addEmptyCommentLine(DisassemblySectionType.CODE_LINES);
	}

	private void addEmptyCommentLine(DisassemblySectionType disassemblySectionType) {
		addLine(profile.commentPrefix, disassemblySectionType);
	}

	private void addLine(String text) {
		addLine(text, DisassemblySectionType.CODE_LINES);
	}

	/** Adds a label/comment/code line into a section, creating and headering the section on first use. */
	private void addLine(String text, DisassemblySectionType disassemblySectionType) {
		if (pass != 4 && pass != 5 && pass != 6) {
			throw new IllegalStateException("addLine() must only be called in pass 4/5/6.");
		}

		boolean newSection = false;
		DisassemblySection section = result.getSection(disassemblySectionType);
		if (section == null) {
			newSection = true;
			section = result.allocSection(disassemblySectionType);
		}

		DisassemblyLine templateLine = new DisassemblyLine(section);
		templateLine.segmentIndex = markSegmentIndex;
		templateLine.offset = markOffset;
		templateLine.size = markSize;
		templateLine.address = absoluteAddress;
		templateLine.systemAddress = systemAddress;

		if (markSegmentIndex != SegmentList.NO_SEGMENT_INDEX) {
			Segment markSegment = workspace.getSegmentList().getSegment(markSegmentIndex);
			int markSegmentSize = markSegment.getSize();
			if (markOffset + markSize > markSegmentSize) {
				templateLine.size = markSegmentSize - markOffset;
			}
		}

		if (newSection) {
			int oldAbsoluteAddress = absoluteAddress;
			int oldSystemAddress = systemAddress;
			systemAddress = 0;
			String sectionText = DisassemblySection.getText(disassemblySectionType);

			switch (disassemblySectionType) {
			case SYSTEM_EQUATES:
			case USER_EQUATES:
			case CODE_EQUATES:
				markNextSegmentIndex = SegmentList.NO_SEGMENT_INDEX;
				markSegmentIndex = SegmentList.NO_SEGMENT_INDEX;
				markOffset = 0xFFFF;
				markSize = 0;
				break;
			case CODE_LINES:
				break;
			}
			addLineInSection(templateLine, profile.commentPrefix, section);
			addLineInSection(templateLine, profile.commentPrefix + " " + sectionText, section);
			addLineInSection(templateLine, profile.commentPrefix, section);

			absoluteAddress = oldAbsoluteAddress;
			systemAddress = oldSystemAddress;
		}

		addLineInSection(templateLine, text, section);
	}

	private void addLineWriter() {
		addLineWriter(DisassemblySectionType.CODE_LINES);
	}

	private void addLineWriter(DisassemblySectionType disassemblySectionType) {
		addLine(lineWriter.getLineBuffer(), disassemblySectionType);
	}

	// ------------------------------------------------------------------
	// Disassembly position tracking (DisInit / DisGetNextByte).
	// ------------------------------------------------------------------

	/** Initializes fields to the start of code. @return false if there is nothing to disassemble. */
	private boolean disInit() {
		pc = 0;
		absoluteAddress = 0;
		systemAddress = 0;

		disNewSegment = true;
		memoryBlockIterator = null;

		SegmentList segmentList = workspace.getSegmentList();

		for (segmentIndex = 0; segmentIndex < segmentList.getCount(); segmentIndex++) {
			Segment segment = segmentList.getSegment(segmentIndex);

			if (segment.bBinary && segment.getSize() > 0) {
				memoryBlockIterator = new MemoryBlockIterator(segment.memoryBlock);
				pc = segment.wBegin;
				disNewSegment = true;
				setSegmentFirstLineNumber(segmentIndex);
				markNextSegmentIndex = segmentIndex;
				disassemblyProgressMonitor.setSegmentNumber(segmentIndex + 1);
				return true;
			}
		}

		return false;
	}

	/**
	 * Reads the next byte into {@link #lastByte}/{@link #byteType}, advancing
	 * {@link #segmentIndex}/{@link #pc} (crossing into the next binary
	 * segment if the current one is exhausted). Mirrors {@code
	 * DisGetNextByte}; the cancellation check the {@code DIS_GET_NEXT_BYTE}
	 * macro performed after every call is {@link #getNextByte()}.
	 */
	private void disGetNextByte() {
		lastByte = memoryBlockIterator.getData();
		byteType = memoryBlockIterator.getType();
		memoryBlockIterator.next();
		markSize++;
		opcodeBuffer.saveLastOpcode(lastByte);

		if (disassemblyProgressMonitor.isVerbose()) {
			disassemblyProgressMonitor.sendInfo("segmentIndex=" + segmentIndex + ", wPC="
					+ Memory.addressToHexString(pc) + ", bByte=" + Memory.byteToHexString(lastByte) + ", memoryType="
					+ Memory.byteToHexString(byteType.ordinal()));
		}

		// End of segment reached?
		if (!memoryBlockIterator.hasNext()) {
			SegmentList segmentList = workspace.getSegmentList();
			Segment segment = segmentList.getSegment(segmentIndex);
			int savedSegmentIndex = segmentIndex;
			int lastEndAddress = segment.wEnd;
			FileHeader lastHeader = segment.getHeader();

			clearMemoryBlockIterator();

			do {
				segmentIndex++;
				if (segmentIndex >= segmentList.getCount()) {
					clearMemoryBlockIterator();
					segmentIndex = savedSegmentIndex;
					pc++; // TODO: Why?
					break;
				}

				segment = segmentList.getSegment(segmentIndex);
				if (segment.bBinary && !segment.isEmpty()) {
					disassemblyProgressMonitor.setSegmentNumber(segmentIndex + 1);

					createMemoryBlockIterator(segment);
					pc = segment.wBegin;

					if (lastEndAddress + 1 != pc || !segment.isHeader(lastHeader)
							|| (!segment.isHeader(FileHeader.ATARI_BINARY)
									&& !segment.isHeader(FileHeader.SDX_FIXED_BLK))) {
						disNewSegment = true;
					}

					setSegmentFirstLineNumber(segmentIndex); // TODO: Why is this called here/so often?
					markNextSegmentIndex = segmentIndex;
				}
			} while (memoryBlockIterator == null);
		} else {
			pc++;
		}
	}

	/**
	 * Reads the next byte via {@link #disGetNextByte()}, throwing {@link
	 * DisassemblyCancelledException} if disassembly was cancelled meanwhile -
	 * mirrors the {@code DIS_GET_NEXT_BYTE} macro.
	 */
	private void getNextByte() {
		disGetNextByte();
		if (isCancelled()) {
			throw DISASSEMBLY_CANCELLED;
		}
	}

	/**
	 * Mirrors the {@code DIS_GET_BYTE_IN_PASS_2} macro used by Pass 2 and
	 * Pass 3: throws {@link AbortInstructionException} if a segment boundary
	 * was already crossed, otherwise reads and returns the next byte.
	 */
	private int nextByteInPass23() {
		if (disNewSegment || memoryBlockIterator == null) {
			throw ABORT_INSTRUCTION;
		}
		getNextByte();
		return lastByte;
	}

	/** Mirrors the {@code DIS_GET_WORD_IN_PASS_2} macro. */
	private int nextWordInPass23() {
		int low = nextByteInPass23();
		int high = nextByteInPass23();
		return Memory.toAddress(low, high);
	}

	// ------------------------------------------------------------------
	// Pass 2: reserve all labels.
	// ------------------------------------------------------------------

	private void pass2() {
		SegmentList segmentList = workspace.getSegmentList();

		if (!disInit()) {
			return;
		}

		while (memoryBlockIterator != null) {
			try {
				pass2Step(segmentList);
			} catch (AbortInstructionException e) {
				// Segment boundary crossed mid-instruction: move on to the next iteration,
				// matching the C++ macro's break out to the end of the while loop body.
			} catch (DisassemblyCancelledException e) {
				return;
			}
		}
	}

	private void pass2Step(SegmentList segmentList) {
		disNewSegment = false;
		int oldSegmentIndex = segmentIndex;
		int oldPC = pc;
		Segment opcodeSegment = segmentList.getSegment(segmentIndex);
		InstructionSet instructionSet = workspace.getInstructionSet(opcodeSegment.processorType);

		getNextByte();
		int by = lastByte;

		switch (byteType) {

		// No label: this is a byte.
		case BYTE:
		case STRING:
		case SBYTE:
		case STORE:
			break;

		// No label: this is a word.
		case WORD:
		case SYMBOL:
			nextByteInPass23();
			break;

		// This is an address fix-up, or a label.
		case FIXUP:
		case LABEL: {
			int cLow = nextByteInPass23();
			int address = Memory.toAddress(by, cLow);
			segmentList.allocateAddress(oldSegmentIndex, oldPC, address, byteType, OperandMode.Accumulator,
					LabelAccess.READ);
			break;
		}

		// There may be a label: this is the display list.
		case DLIST:
			switch (by & 0x0F) {

			// Empty lines.
			case 0:
				break;

			// Jump.
			case 1: {
				int newSegmentIndex = segmentIndex;
				int newPC = pc;
				int address = nextWordInPass23();
				segmentList.allocateAddress(newSegmentIndex, newPC, address, byteType, OperandMode.Accumulator,
						LabelAccess.READ);
				break;
			}

			// Graphic lines.
			default:
				// Load Memory Scan.
				if ((by & 0x40) != 0) {
					int newSegmentIndex = segmentIndex;
					int newPC = pc;
					int address = nextWordInPass23();
					segmentList.allocateAddress(newSegmentIndex, newPC, address, byteType, OperandMode.Accumulator,
							LabelAccess.READ);
				}
				break;
			}
			break;

		// Code.
		case LOBYTE:
		case HIBYTE:
		case UNKNOWN:
		case CODE: {
			Instruction instruction = instructionSet.getInstruction(by);
			if (instruction.isUnsupportedInstruction() && !profile.useIllegalOpcodes) {
				break;
			}

			switch (instruction.getOperandMode()) {
			case Unknown:
				throw new IllegalStateException("Operand mode is unknown.");

			// Accumulator mode has no additional operands.
			case Accumulator:
				break;

			// Get absolute address.
			case Absolute:
			case Indirect:
			case AbsoluteX:
			case AbsoluteY:
			case IndexedIndirectAbsolute: {
				int newSegmentIndex = segmentIndex;
				int newPC = pc;
				int address = nextWordInPass23();
				segmentList.allocateAddress(newSegmentIndex, newPC, address, byteType, instruction);
				break;
			}

			// Get zero-page absolute address.
			case ZeroPageX:
			case ZeroPageY:
			case ZeroPage:
			case IndexedIndirect:
			case IndirectIndexed:
			case ZeroPageIndirect:
			case ZeroPageRelative: {
				int cLow = nextByteInPass23();
				segmentList.allocateAddress(oldSegmentIndex, oldPC, cLow, byteType, instruction);
				break;
			}

			// Get relative address and make an absolute one.
			case Relative: {
				int newSegmentIndex = segmentIndex;
				int newPC = pc;
				int address = newPC + 1;
				int cLow = nextByteInPass23();
				if (cLow > 127) {
					address += cLow - 256;
				} else {
					address += cLow;
				}
				segmentList.allocateAddress(newSegmentIndex, newPC, address, byteType, instruction);
				break;
			}

			// Other modes with a parameter that is not an address.
			case Immediate:
				// byteType is read again after nextByteInPass23() below, which overwrites it with
				// the operand byte's own type - matches the C++ source's reuse of cDisByteType
				// (also overwritten by its DIS_GET_NEXT_BYTE call) exactly, quirky as that is.
				if (byteType == MemoryType.LOBYTE) {
					int cLow = nextByteInPass23();
					int address = Memory.toAddress(cLow, byteType.ordinal());
					segmentList.allocateAddress(oldSegmentIndex, oldPC, address, byteType, instruction);
				} else if (byteType == MemoryType.HIBYTE) {
					int cLow = nextByteInPass23();
					int address = Memory.toAddress(byteType.ordinal(), cLow);
					segmentList.allocateAddress(oldSegmentIndex, oldPC, address, byteType, instruction);
				} else {
					nextByteInPass23();
				}
				break;

			// Implied mode has no additional operands.
			case Implied:
				break;

			// Reserved 65C02 NOP (1 byte) has no additional operands.
			case ReservedNop1Byte:
				break;

			// Reserved 65C02 NOP (2 bytes) has one additional operand.
			case ReservedNop2Byte:
				nextByteInPass23(); // Unused byte.
				break;

			// Reserved 65C02 NOP (3 bytes) has two additional operands.
			case ReservedNop3Byte:
				nextByteInPass23(); // Unused byte.
				nextByteInPass23(); // Unused byte.
				break;
			}
			break;
		}
		}
	}

	// ------------------------------------------------------------------
	// Pass 3: update labels to generate relative labels (L2222+1 instead of L2223 if L2223 is inside an instruction).
	// ------------------------------------------------------------------

	private void pass3() {
		SegmentList segmentList = workspace.getSegmentList();

		// Determine which label address is at the start of an instruction (aligned).
		if (!disInit()) {
			return;
		}
		while (memoryBlockIterator != null) {
			Segment opcodeSegment = segmentList.getSegment(segmentIndex);
			opcodeSegment.defineAddressLabel(pc);
			try {
				pass3Step(segmentList);
			} catch (AbortInstructionException e) {
				// Segment boundary crossed mid-instruction.
			} catch (DisassemblyCancelledException e) {
				return;
			}
		}

		// For those non-aligned label addresses, find the immediate previous one.
		segmentList.alignRamBlkLabelAddresses();
		if (!disInit()) {
			return;
		}
		while (memoryBlockIterator != null) {
			Segment opcodeSegment = segmentList.getSegment(segmentIndex);
			opcodeSegment.alignAddressLabels(pc);
			try {
				pass3Step(segmentList);
			} catch (AbortInstructionException e) {
				// Segment boundary crossed mid-instruction.
			} catch (DisassemblyCancelledException e) {
				return;
			}
		}
	}

	/** One instruction step shared by both Pass 3 loops: consumes one instruction's bytes to keep the segment/PC position in sync, without allocating any addresses (unlike {@link #pass2Step}). */
	private void pass3Step(SegmentList segmentList) {
		Segment opcodeSegment = segmentList.getSegment(segmentIndex);
		InstructionSet instructionSet = workspace.getInstructionSet(opcodeSegment.processorType);
		disNewSegment = false;

		getNextByte();
		int by = lastByte;

		switch (byteType) {

		// No label: this is a byte.
		case BYTE:
		case STRING:
		case SBYTE:
		case STORE:
			break;

		// No label: this is a word.
		case WORD:
		case FIXUP:
		case SYMBOL:
		case LABEL:
			nextByteInPass23();
			break;

		// There may be a label: this is the display list.
		case DLIST:
			switch (by & 0x0F) {
			case 0:
				break;
			case 1:
				nextWordInPass23();
				break;
			default:
				if ((by & 0x40) != 0) {
					nextWordInPass23();
				}
				break;
			}
			break;

		// Code.
		case LOBYTE:
		case HIBYTE:
		case UNKNOWN:
		case CODE: {
			Instruction instruction = instructionSet.getInstruction(by);
			if (instruction.isUnsupportedInstruction() && !profile.useIllegalOpcodes) {
				break;
			}

			switch (instruction.getOperandMode()) {
			case Unknown:
				throw new IllegalStateException("Operand mode is unknown.");

			// Modes without additional operands.
			case Accumulator:
			case Implied:
			case ReservedNop1Byte:
				break;

			// Get absolute address.
			case Absolute:
			case Indirect:
			case AbsoluteX:
			case AbsoluteY:
			case IndexedIndirectAbsolute:
			case ReservedNop3Byte:
				nextWordInPass23();
				break;

			// Get zero-page absolute address.
			case ZeroPageX:
			case ZeroPageY:
			case ZeroPage:
			case IndexedIndirect:
			case IndirectIndexed:
			case Relative:
			case Immediate:
			case ZeroPageIndirect:
			case ZeroPageRelative:
			case ReservedNop2Byte:
				nextByteInPass23();
				break;
			}
			break;
		}
		}
	}

	// ------------------------------------------------------------------
	// Result generation helpers used by Pass 4/5/6 (not ported yet).
	// ------------------------------------------------------------------

	private void addOrgOrBlock(int segmentIndex, int pc) {
		SegmentList segmentList = workspace.getSegmentList();
		Segment segment = segmentList.getSegment(segmentIndex);

		switch (segment.getHeader()) {
		case SDX_FIXED_BLK:
			lineWriter.string("blk sparta").space().address(pc);
			break;

		case SDX_RELOC_BLK: {
			String memoryType = segment.getSDXMemoryType();
			if (segment.isSDXRelocBlkWithData()) {
				lineWriter.string("blk reloc");
			} else {
				lineWriter.string("blk empty").space().number(segment.getSize());
			}
			lineWriter.space().string(memoryType).space().string(profile.commentPrefix).string(" num: ")
					.byteNumber(segment.bSDXBlockNumber).string(" mem: ").byteNumber(segment.bSDXControlByte);
			break;
		}

		case SDX_FIX_UP_BLK:
			lineWriter.string("blk update addresses");
			break;

		case SDX_SYM_REQUIRED:
			lineWriter.string("blk update symbols");
			break;

		case SDX_SYM_DEFINED: {
			Equate equate = workspace.getUserEquateList().findEquateByAddress(segment.wBegin, LabelAccess.READ, true);
			String label;
			if (equate == null) {
				Segment labelSegment = segmentList.findBySDXBlockNumber(segment.bSDXBlockNumber);
				label = Segment.formatDefaultLabel(segmentList.getSegmentIndex(labelSegment), segment.wBegin);
			} else {
				label = equate.getLabel();
			}
			lineWriter.string("blk update new").space().string(label).space().ch('\'').string(segment.sdxSymbol)
					.ch('\'');
			break;
		}

		default:
			lineWriter.string(profile.directiveORG).space().address(pc);
			break;
		}
	}

	/** Generates lines for system or user equates. */
	private void generateEquates(DisassemblySectionType disassemblySectionType, EquateList equateList) {
		for (Equate equate : equateList.getEquates()) {
			switch (equate.getType()) {
			case UNKNOWN:
				throw new IllegalStateException("Invalid access.");

			case EMPTY:
				addLine("", disassemblySectionType);
				break;

			case COMMENT:
				addComment(equate.getComment(), disassemblySectionType);
				break;

			case LABEL:
				// Ignoring those relative to a base label.
				if (!equate.isRange()) {
					addLabel(equate.getLabel(), equate.getLabelValue(), disassemblySectionType, equate.getComment());
				}
				break;
			}
		}
	}

	/** Generates lines for all code equates. */
	private void generateCodeEquates() {
		Segment segment = workspace.getSegmentList().getGlobalSegment();
		for (AddressLabel addressLabel : segment.getAddressLabels().enumerate()) {
			addLabel("L" + HexUtility.getLongValueHexString(addressLabel.getAddress(), 4), addressLabel.getAddress(),
					DisassemblySectionType.CODE_EQUATES, "");
		}
	}
}
