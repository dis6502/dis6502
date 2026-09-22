/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

import com.wudsn.tools.dis6502.model.system.ComputerSystem;

/**
 * Follows the flow of a program from a starting address to guess which
 * bytes are code versus data, marking their {@link MemoryType} as it
 * goes.
 * <p>
 * The one piece of {@code com.wudsn.tools.dis6502.ui.MemoryInspectorPanel}'s
 * logic substantial enough, and independent enough of the UI, to warrant
 * its own model-layer class rather than living directly there the way this
 * port's other memory inspector actions do. {@link MemoryInspectorStack}
 * is used here exactly as intended - see its own javadoc - as a private,
 * per-guess work list of addresses still to trace, not shared with
 * anything else.
 * <p>
 * A summary of what the algorithm does: starting from one address, it
 * walks instructions marking each byte {@link MemoryType#CODE} until it hits an
 * unconditional exit (BRK/RTI/RTS/JMP (Indirect)) or a byte already
 * marked as something other than {@link MemoryType#UNKNOWN}. A JSR/JMP
 * pushes its target address onto the work list to trace next (and JMP
 * ends the current walk); a branch pushes its target too, but keeps
 * walking past it (both paths are live). Along the way, it approximately
 * tracks the accumulator/X/Y registers' last-known values ({@link
 * DumpRegister}/{@link DumpContext}) well enough to recognize the common
 * "build a 16 bit pointer in two 8 bit loads" idiom - {@code LDA
 * #<addr / STA ptr} immediately followed by {@code LDA #>addr / STA
 * ptr+1} (in either order) - and when it does ({@link #sortContext}),
 * retags the two loaded immediate bytes as {@link MemoryType#LOBYTE}/
 * {@link MemoryType#HIBYTE} and, if the pointer they built looks like a
 * known vector/display-list address for the current {@link
 * ComputerSystem}, either traces the pointed-to address as more code or
 * (for a display list) marks it with {@link #setDisplayList} instead.
 * {@code JSR PRINTF} is special-cased to mark the format string that
 * follows the call as {@link MemoryType#STRING} and, per an inline
 * printf-style format specifier, some of the bytes after the string as
 * {@link MemoryType#LABEL} - the same format-specifier state machine
 * {@link Segment#allocateSymbol} already implements for a symbol table
 * fix-up named "PRINTF" (see that method's javadoc); it is not reused
 * from there since the two call sites need different surrounding
 * bookkeeping.
 *
 * @author Peter Dell
 */
public final class GuessCodeLogic {

	private static final class DumpRegister {
		int reg = 0xFFFF; // 0xFFFF means "not known".
		int seg = SegmentList.NO_SEGMENT_INDEX;
		int ofs;
		int segData;
		int ofsData;

		void copyFrom(DumpRegister other) {
			reg = other.reg;
			seg = other.seg;
			ofs = other.ofs;
			segData = other.segData;
			ofsData = other.ofsData;
		}
	}

	private static final class DumpContext {
		final DumpRegister[] regs = { new DumpRegister(), new DumpRegister(), new DumpRegister() };
		int index;
		int addr = 0xFFFF;

		void reset() {
			for (DumpRegister reg : regs) {
				reg.reg = 0xFFFF;
				reg.seg = SegmentList.NO_SEGMENT_INDEX;
				reg.ofs = 0;
				reg.segData = 0;
				reg.ofsData = 0;
			}
			index = 0;
			addr = 0xFFFF;
		}

		void copyFrom(DumpContext other) {
			for (int i = 0; i < regs.length; i++) {
				regs[i].copyFrom(other.regs[i]);
			}
			index = other.index;
			addr = other.addr;
		}
	}

	private enum PrintfState {
		TEXT, PERCENT, FORMAT
	}

	private final Workspace workspace;
	private final SegmentList segmentList;
	private final MemoryInspectorStack stack = new MemoryInspectorStack();

	public GuessCodeLogic(Workspace workspace) {
		this.workspace = workspace;
		this.segmentList = workspace.getSegmentList();
	}

	/**
	 * The caller is responsible for re-running the disassembly afterward,
	 * the same as {@code
	 * com.wudsn.tools.dis6502.ui.MemoryInspectorPanel#setType}/{@code
	 * setUnknownBlockToByte} already leave to their callers.
	 */
	public void guess(Segment segment, int beginOffset) {
		if (segment == null || segment.isEmpty() || !segment.bBinary) {
			return;
		}
		stack.pushAddress(segment.wBegin + beginOffset);
		guessCode();
		stack.clear();
	}

	/** The index of the segment containing {@code address}, or {@link SegmentList#NO_SEGMENT_INDEX}. */
	private int findSegmentByAbsoluteAddress(int address) {
		return segmentList.findByAddr(address);
	}

	/**
	 * {@code segmentIndex}/{@code offset} are single-element arrays used as
	 * out-parameters, mutated in place to cross into the next segment when
	 * {@code offset} runs past the current one's end.
	 */
	private boolean markOneByte(int[] segmentIndex, int[] offset, MemoryType memoryType, boolean abortIfNotUnknown) {
		Segment segment = segmentList.getSegment(segmentIndex[0]);
		int begin = segment.wBegin;
		int end = segment.wEnd;

		if (begin + offset[0] > end) {
			int nextSegmentIndex = segmentIndex[0] + 1;
			if (nextSegmentIndex >= segmentList.getCount()) {
				return false;
			}
			Segment nextSegment = segmentList.getSegment(nextSegmentIndex);
			if (nextSegment.isEmpty() || !nextSegment.bBinary) {
				return false;
			}
			if (nextSegment.wBegin != end + 1) {
				return false;
			}
			segmentIndex[0] = nextSegmentIndex;
			offset[0] = 0;
		}

		Segment markedSegment = segmentList.getSegment(segmentIndex[0]);

		if (abortIfNotUnknown) {
			if (!markedSegment.isType(offset[0], MemoryType.UNKNOWN)) {
				return false;
			}
			markedSegment.setType(offset[0], memoryType);
		} else {
			if (markedSegment.isType(offset[0], MemoryType.UNKNOWN)) {
				markedSegment.setType(offset[0], memoryType);
			}
		}

		return true;
	}

	private boolean markOneByte(int[] segmentIndex, int[] offset, MemoryType memoryType) {
		return markOneByte(segmentIndex, offset, memoryType, true);
	}

	/**
	 * Fills {@code wSrc}/{@code wDest} (single-element out-arrays) so the
	 * addresses are ordered, and returns whether the two contexts have
	 * consecutive addresses with known register values at a known base
	 * address. {@code wSrc}/{@code wDest} are set whenever this returns
	 * {@code true}, but the actual retagging (and clearing
	 * oldContext/newContext's address) only happens if both registers'
	 * value also came from a real, known memory location.
	 */
	private boolean sortContext(DumpContext oldContext, DumpContext newContext, int[] wSrc, int[] wDest) {
		DumpContext low = oldContext.addr < newContext.addr ? oldContext : newContext;
		DumpContext high = low == oldContext ? newContext : oldContext;

		int lowReg = low.regs[low.index].reg;
		int highReg = high.regs[high.index].reg;
		ComputerSystem computerSystem = workspace.getComputerSystem();

		boolean haveAddress = low.addr == ((high.addr - 1) & 0xFFFF) && lowReg != 0xFFFF && highReg != 0xFFFF
				&& computerSystem.isBaseAddress(low.addr);
		if (haveAddress) {
			wSrc[0] = ((highReg << 8) + lowReg) & 0xFFFF;
			wDest[0] = low.addr;

			int lowSeg = low.regs[low.index].seg;
			int lowOfs = low.regs[low.index].ofs;
			int highSeg = high.regs[high.index].seg;
			int highOfs = high.regs[high.index].ofs;

			if (lowSeg != SegmentList.NO_SEGMENT_INDEX && highSeg != SegmentList.NO_SEGMENT_INDEX) {
				int lowSegData = low.regs[low.index].segData;
				int lowOfsData = low.regs[low.index].ofsData;
				int highSegData = high.regs[high.index].segData;
				int highOfsData = high.regs[high.index].ofsData;

				Segment lowDataSegment = segmentList.getSegment(lowSegData);
				Segment highDataSegment = segmentList.getSegment(highSegData);
				int lowValue = lowDataSegment.getData(lowOfsData);
				int highValue = highDataSegment.getData(highOfsData);

				Segment lowSegment = segmentList.getSegment(lowSeg);
				Segment highSegment = segmentList.getSegment(highSeg);

				lowSegment.setType(lowOfs, MemoryType.LOBYTE);
				highSegment.setType(highOfs, MemoryType.HIBYTE);
				lowDataSegment.setData(lowOfsData, highValue);
				highDataSegment.setData(highOfsData, lowValue);

				oldContext.addr = 0xFFFF;
				newContext.addr = 0xFFFF;
			}
		}

		return haveAddress;
	}

	/** Marks an Atari display list starting at {@code address}, following its jump/wait instructions. */
	private void setDisplayList(int address) {
		int segmentIndex = findSegmentByAbsoluteAddress(address);
		if (segmentIndex == SegmentList.NO_SEGMENT_INDEX) {
			return;
		}
		Segment segment = segmentList.getSegment(segmentIndex);
		int[] segmentIndexRef = { segmentIndex };
		int[] offsetRef = { address - segment.wBegin };

		while (markOneByte(segmentIndexRef, offsetRef, MemoryType.DLIST)) {
			segment = segmentList.getSegment(segmentIndexRef[0]);
			int value = segment.getData(offsetRef[0]);
			offsetRef[0]++;

			switch (value & 0x0F) {
			case 0:
				break;

			case 1: {
				int savedWord = segment.getWord(offsetRef[0]);
				markOneByte(segmentIndexRef, offsetRef, MemoryType.DLIST);
				offsetRef[0]++;
				markOneByte(segmentIndexRef, offsetRef, MemoryType.DLIST);
				offsetRef[0]++;
				setDisplayList(savedWord);
				return;
			}

			default:
				if ((value & 0x40) != 0) {
					for (int i = 0; i < 2; i++) {
						markOneByte(segmentIndexRef, offsetRef, MemoryType.DLIST);
						offsetRef[0]++;
					}
				}
				break;
			}
		}
	}

	/** Drains {@link #stack}, tracing each address per this class's javadoc. */
	private void guessCode() {
		DumpRegister reg = new DumpRegister();
		DumpContext newContext = new DumpContext();
		DumpContext oldContext = new DumpContext();
		int[] wSrc = { 0 };
		int[] wDest = { 0 };
		int saveInstruction = 0;
		int[] param = { 0, 0 };
		int savedWord = 0;

		ComputerSystem computerSystem = workspace.getComputerSystem();

		int address = stack.popAddress();
		while (address != 0xFFFF) {
			int segmentIndex = findSegmentByAbsoluteAddress(address);

			if (segmentIndex != SegmentList.NO_SEGMENT_INDEX) {
				Segment segment = segmentList.getSegment(segmentIndex);
				InstructionSet instructionSet = workspace.getInstructionSet(segment.processorType);

				oldContext.reset();
				newContext.reset();
				int[] segmentIndexRef = { segmentIndex };
				int[] offsetRef = { address - segment.wBegin };

				while (markOneByte(segmentIndexRef, offsetRef, MemoryType.CODE)) {
					segmentIndex = segmentIndexRef[0];
					segment = segmentList.getSegment(segmentIndex);

					reg.reg = 0xFFFF;
					reg.seg = segmentIndex;
					reg.ofs = offsetRef[0];

					int opcode = segment.getData(offsetRef[0]);
					int opcodeLength = instructionSet.getInstruction(opcode).getLength();
					offsetRef[0]++;

					for (int i = 1; i < opcodeLength; i++) {
						markOneByte(segmentIndexRef, offsetRef, MemoryType.CODE);
						segment = segmentList.getSegment(segmentIndexRef[0]);
						param[i - 1] = segment.getData(offsetRef[0]);

						if (i == 1) {
							reg.reg = param[0] & 0xFF;
							reg.segData = segmentIndexRef[0];
							reg.ofsData = offsetRef[0];
						}

						offsetRef[0]++;
					}
					segmentIndex = segmentIndexRef[0];

					if (opcodeLength == 3) {
						savedWord = (param[0] & 0xFF) + ((param[1] & 0xFF) << 8);
					}

					if (opcode == 0x20 /* JSR */ || opcode == 0x4C /* JMP */) {
						// Do not push the address if it is an SDX symbol.
						if (!segment.isType(offsetRef[0] - 2, MemoryType.SYMBOL)) {
							int target = (param[0] & 0xFF) + ((param[1] & 0xFF) << 8);
							if (target != 0xFFFF) {
								stack.pushAddress(target);
							}
						}

						if (opcode == 0x4C) {
							break;
						}

						String label = null;
						if (segment.isType(offsetRef[0] - 2, MemoryType.SYMBOL)) {
							label = workspace.findSymbolByAddress(segmentIndex, segment.wBegin + offsetRef[0] - 2, 0);
						}
						if (label != null && !label.isEmpty() && label.equals("PRINTF")) {
							PrintfState printState = PrintfState.TEXT;
							int bytesAsLabels = 0;
							boolean notEnd;

							while ((notEnd = markOneByte(segmentIndexRef, offsetRef, MemoryType.STRING, false))) {
								segment = segmentList.getSegment(segmentIndexRef[0]);
								int value = segment.getData(offsetRef[0]);
								offsetRef[0]++;

								if (value == 0) {
									break;
								}

								switch (printState) {
								case TEXT:
									if (value == '%') {
										printState = PrintfState.PERCENT;
									}
									break;

								case PERCENT:
									if (value == '%') {
										printState = PrintfState.TEXT;
									} else if (value == 'c' || value == 's' || value == 'p' || value == 'x' || value == 'b'
											|| value == 'd' || value == 'e' || value == 'l' || value == 't') {
										printState = PrintfState.TEXT;
										bytesAsLabels += 2;
									} else {
										printState = PrintfState.FORMAT;
										bytesAsLabels += 2;
									}
									break;

								case FORMAT:
									if (value == '*') {
										bytesAsLabels += 2;
									} else if (value == 'c' || value == 's' || value == 'p' || value == 'x' || value == 'b'
											|| value == 'd' || value == 'e' || value == 'l' || value == 't') {
										printState = PrintfState.TEXT;
									}
									break;
								}
							}
							segmentIndex = segmentIndexRef[0];

							if (notEnd) {
								while (bytesAsLabels > 0 && markOneByte(segmentIndexRef, offsetRef, MemoryType.LABEL, false)) {
									offsetRef[0]++;
									bytesAsLabels--;
								}
								segmentIndex = segmentIndexRef[0];
							}
						}

						oldContext.reset();
						newContext.reset();
					} else if (opcode == 0x10 /* BPL */ || opcode == 0x30 /* BMI */ || opcode == 0x50 /* BVC */
							|| opcode == 0x70 /* BVS */ || opcode == 0x90 /* BCC */ || opcode == 0xB0 /* BCS */
							|| opcode == 0xD0 /* BNE */ || opcode == 0xF0 /* BEQ */) {
						int branchTarget;
						if ((param[0] & 0x80) != 0) {
							branchTarget = (segment.wBegin + offsetRef[0] - (256 - (param[0] & 0xFF))) & 0xFFFF;
						} else {
							branchTarget = (segment.wBegin + offsetRef[0] + param[0]) & 0xFFFF;
						}

						if (branchTarget != 0xFFFF) {
							stack.pushAddress(branchTarget);
						}
					}

					if (opcode == 0x00 /* BRK */ || opcode == 0x40 /* RTI */ || opcode == 0x60 /* RTS */
							|| opcode == 0x6C /* JMP (Indirect) */) {
						break;
					}

					if (opcode == 0xA9 /* LDA # */ && reg.reg != 0xFFFF) {
						newContext.regs[0].copyFrom(reg);
					} else if (opcode == 0xA2 /* LDX # */ && reg.reg != 0xFFFF) {
						newContext.regs[1].copyFrom(reg);
					} else if (opcode == 0xA0 /* LDY # */ && reg.reg != 0xFFFF) {
						newContext.regs[2].copyFrom(reg);
					} else if (opcode == 0xE8 /* INX */) {
						if (newContext.regs[1].reg != 0xFFFF) {
							newContext.regs[1].reg = (newContext.regs[1].reg + 1) & 0xFF;
						}
						newContext.regs[1].seg = SegmentList.NO_SEGMENT_INDEX;
					} else if (opcode == 0xCA /* DEX */) {
						if (newContext.regs[1].reg != 0xFFFF) {
							newContext.regs[1].reg = (newContext.regs[1].reg - 1) & 0xFF;
						}
						newContext.regs[1].seg = SegmentList.NO_SEGMENT_INDEX;
					} else if (opcode == 0xC8 /* INY */) {
						if (newContext.regs[2].reg != 0xFFFF) {
							newContext.regs[2].reg = (newContext.regs[2].reg + 1) & 0xFF;
						}
						newContext.regs[2].seg = SegmentList.NO_SEGMENT_INDEX;
					} else if (opcode == 0x88 /* DEY */) {
						if (newContext.regs[2].reg != 0xFFFF) {
							newContext.regs[2].reg = (newContext.regs[2].reg - 1) & 0xFF;
						}
						newContext.regs[2].seg = SegmentList.NO_SEGMENT_INDEX;
					} else if (opcode == 0x0A /* ASL */) {
						if (newContext.regs[0].reg != 0xFFFF) {
							newContext.regs[0].reg = (newContext.regs[0].reg << 1) & 0xFF;
						}
						newContext.regs[0].seg = SegmentList.NO_SEGMENT_INDEX;
					} else if (opcode == 0x4A /* LSR */) {
						if (newContext.regs[0].reg != 0xFFFF) {
							newContext.regs[0].reg = (newContext.regs[0].reg >> 1) & 0xFF;
						}
						newContext.regs[0].seg = SegmentList.NO_SEGMENT_INDEX;
					} else if (opcode == 0x09 /* ORA # */) {
						if (newContext.regs[0].reg != 0xFFFF) {
							newContext.regs[0].reg = (newContext.regs[0].reg | param[0]) & 0xFF;
						}
						newContext.regs[0].seg = SegmentList.NO_SEGMENT_INDEX;
					} else if (opcode == 0x29 /* AND # */) {
						if (newContext.regs[0].reg != 0xFFFF) {
							newContext.regs[0].reg = (newContext.regs[0].reg & param[0]) & 0xFF;
						}
						newContext.regs[0].seg = SegmentList.NO_SEGMENT_INDEX;
					} else if (opcode == 0x49 /* EOR # */) {
						if (newContext.regs[0].reg != 0xFFFF) {
							newContext.regs[0].reg = (newContext.regs[0].reg ^ param[0]) & 0xFF;
						}
						newContext.regs[0].seg = SegmentList.NO_SEGMENT_INDEX;
					} else if (opcode == 0x8A /* TXA */) {
						newContext.regs[0].reg = newContext.regs[1].reg;
						newContext.regs[0].seg = SegmentList.NO_SEGMENT_INDEX;
					} else if (opcode == 0x98 /* TYA */) {
						newContext.regs[0].reg = newContext.regs[2].reg;
						newContext.regs[0].seg = SegmentList.NO_SEGMENT_INDEX;
					} else if (opcode == 0xAA /* TAX */) {
						newContext.regs[1].reg = newContext.regs[0].reg;
						newContext.regs[1].seg = SegmentList.NO_SEGMENT_INDEX;
					} else if (opcode == 0xA8 /* TAY */) {
						newContext.regs[2].reg = newContext.regs[0].reg;
						newContext.regs[2].seg = SegmentList.NO_SEGMENT_INDEX;
					} else if (opcode == 0x01 /* ORA */ || opcode == 0x05 /* ORA */ || opcode == 0x0D /* ORA */
							|| opcode == 0x11 /* ORA */ || opcode == 0x15 /* ORA */ || opcode == 0x19 /* ORA */
							|| opcode == 0x1D /* ORA */ || opcode == 0x21 /* AND */ || opcode == 0x25 /* AND */
							|| opcode == 0x2A /* ROL */ || opcode == 0x2D /* AND */ || opcode == 0x31 /* AND */
							|| opcode == 0x35 /* AND */ || opcode == 0x39 /* AND */ || opcode == 0x3D /* AND */
							|| opcode == 0x41 /* EOR */ || opcode == 0x45 /* EOR */ || opcode == 0x4D /* EOR */
							|| opcode == 0x51 /* EOR */ || opcode == 0x55 /* EOR */ || opcode == 0x59 /* EOR */
							|| opcode == 0x5D /* EOR */ || opcode == 0x61 /* ADC */ || opcode == 0x65 /* ADC */
							|| opcode == 0x68 /* PLA */ || opcode == 0x6A /* ROR */ || opcode == 0x6D /* ADC */
							|| opcode == 0x71 /* ADC */ || opcode == 0x75 /* ADC */ || opcode == 0x79 /* ADC */
							|| opcode == 0x7D /* ADC */ || opcode == 0xA1 /* LDA */ || opcode == 0xA5 /* LDA */
							|| opcode == 0xAD /* LDA */ || opcode == 0xB1 /* LDA */ || opcode == 0xB5 /* LDA */
							|| opcode == 0xB9 /* LDA */ || opcode == 0xBD /* LDA */ || opcode == 0xE1 /* SBC */
							|| opcode == 0xE5 /* SBC */ || opcode == 0xE9 /* SBC */ || opcode == 0xED /* SBC */
							|| opcode == 0xF1 /* SBC */ || opcode == 0xF5 /* SBC */ || opcode == 0xF9 /* SBC */
							|| opcode == 0xFD /* SBC */) {
						newContext.regs[0].reg = 0xFFFF;
						newContext.regs[0].seg = SegmentList.NO_SEGMENT_INDEX;
					} else if (opcode == 0xA6 /* LDX */ || opcode == 0xAE /* LDX */ || opcode == 0xB6 /* LDX */
							|| opcode == 0xBA /* TSX */ || opcode == 0xBE /* LDX */) {
						newContext.regs[1].reg = 0xFFFF;
						newContext.regs[1].seg = SegmentList.NO_SEGMENT_INDEX;
					} else if (opcode == 0xA4 /* LDY */ || opcode == 0xAC /* LDY */ || opcode == 0xB4 /* LDY */
							|| opcode == 0xBC /* LDY */) {
						newContext.regs[2].reg = 0xFFFF;
						newContext.regs[2].seg = SegmentList.NO_SEGMENT_INDEX;
					} else if (opcode == 0x85 /* STA Zpg */ && newContext.regs[0].reg != 0xFFFF && opcode != saveInstruction) {
						newContext.addr = param[0] & 0xFF;
						newContext.index = 0;
						if (oldContext.addr != 0xFFFF && sortContext(oldContext, newContext, wSrc, wDest)) {
							if (computerSystem.isDisplayListVectorAddress(wDest[0])) {
								setDisplayList(wSrc[0]);
							} else if (computerSystem.isVectorAddress(wDest[0])) {
								stack.pushAddress(wSrc[0]);
							}
						}
						oldContext.copyFrom(newContext);
						newContext.addr = 0xFFFF;
					} else if (opcode == 0x8D /* STA Abs */ && newContext.regs[0].reg != 0xFFFF && opcode != saveInstruction) {
						newContext.addr = savedWord;
						newContext.index = 0;
						if (oldContext.addr != 0xFFFF && sortContext(oldContext, newContext, wSrc, wDest)) {
							if (computerSystem.isDisplayListVectorAddress(wDest[0])) {
								setDisplayList(wSrc[0]);
							} else if (computerSystem.isVectorAddress(wDest[0])) {
								stack.pushAddress(wSrc[0]);
							}
						}
						oldContext.copyFrom(newContext);
						newContext.addr = 0xFFFF;
					} else if (opcode == 0x9D /* STA Abs,X */ && newContext.regs[0].reg != 0xFFFF && newContext.regs[1].reg != 0xFFFF
							&& opcode != saveInstruction) {
						newContext.addr = (savedWord + newContext.regs[1].reg) & 0xFFFF;
						newContext.index = 0;
						if (oldContext.addr != 0xFFFF && sortContext(oldContext, newContext, wSrc, wDest)) {
							if (computerSystem.isDisplayListVectorAddress(wDest[0])) {
								setDisplayList(wSrc[0]);
							} else if (computerSystem.isVectorAddress(wDest[0])) {
								stack.pushAddress(wSrc[0]);
							}
						}
						oldContext.copyFrom(newContext);
						newContext.addr = 0xFFFF;
					} else if (opcode == 0x99 /* STA Abs,Y */ && newContext.regs[0].reg != 0xFFFF && newContext.regs[2].reg != 0xFFFF
							&& opcode != saveInstruction) {
						newContext.addr = (savedWord + newContext.regs[2].reg) & 0xFFFF;
						newContext.index = 0;
						if (oldContext.addr != 0xFFFF && sortContext(oldContext, newContext, wSrc, wDest)) {
							if (computerSystem.isDisplayListVectorAddress(wDest[0])) {
								setDisplayList(wSrc[0]);
							} else if (computerSystem.isVectorAddress(wDest[0])) {
								stack.pushAddress(wSrc[0]);
							}
						}
						oldContext.copyFrom(newContext);
						newContext.addr = 0xFFFF;
					} else if (opcode == 0x86 /* STX Zpg */ && newContext.regs[1].reg != 0xFFFF && opcode != saveInstruction) {
						newContext.addr = param[0] & 0xFF;
						newContext.index = 1;
						if (oldContext.addr != 0xFFFF && sortContext(oldContext, newContext, wSrc, wDest)) {
							if (computerSystem.isDisplayListVectorAddress(wDest[0])) {
								setDisplayList(wSrc[0]);
							} else if (computerSystem.isVectorAddress(wDest[0])) {
								stack.pushAddress(wSrc[0]);
							}
						}
						oldContext.copyFrom(newContext);
						newContext.addr = 0xFFFF;
					} else if (opcode == 0x8E /* STX Abs */ && newContext.regs[1].reg != 0xFFFF && opcode != saveInstruction) {
						newContext.addr = savedWord;
						newContext.index = 1;
						if (oldContext.addr != 0xFFFF && sortContext(oldContext, newContext, wSrc, wDest)) {
							if (computerSystem.isDisplayListVectorAddress(wDest[0])) {
								setDisplayList(wSrc[0]);
							} else if (computerSystem.isVectorAddress(wDest[0])) {
								stack.pushAddress(wSrc[0]);
							}
						}
						oldContext.copyFrom(newContext);
						newContext.addr = 0xFFFF;
					} else if (opcode == 0x84 /* STY Zpg */ && newContext.regs[2].reg != 0xFFFF && opcode != saveInstruction) {
						newContext.addr = param[0] & 0xFF;
						newContext.index = 2;
						if (oldContext.addr != 0xFFFF && sortContext(oldContext, newContext, wSrc, wDest)) {
							if (computerSystem.isDisplayListVectorAddress(wDest[0])) {
								setDisplayList(wSrc[0]);
							} else if (computerSystem.isVectorAddress(wDest[0])) {
								stack.pushAddress(wSrc[0]);
							}
						}
						oldContext.copyFrom(newContext);
						newContext.addr = 0xFFFF;
					} else if (opcode == 0x8C /* STY Abs */ && newContext.regs[2].reg != 0xFFFF && opcode != saveInstruction) {
						newContext.addr = savedWord;
						newContext.index = 2;
						if (oldContext.addr != 0xFFFF && sortContext(oldContext, newContext, wSrc, wDest)) {
							if (computerSystem.isDisplayListVectorAddress(wDest[0])) {
								setDisplayList(wSrc[0]);
							} else if (computerSystem.isVectorAddress(wDest[0])) {
								stack.pushAddress(wSrc[0]);
							}
						}
						oldContext.copyFrom(newContext);
						newContext.addr = 0xFFFF;
					} else if (instructionSet.getInstruction(opcode).isUnsupportedInstruction()) {
						oldContext.reset();
						newContext.reset();
					}

					saveInstruction = opcode;
				}
			}

			address = stack.popAddress();
		}
	}
}
