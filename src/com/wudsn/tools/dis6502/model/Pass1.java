/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

/**
 * Pass 1 of the disassembly: attaches SDX label fix-ups to each segment.
 * <p>
 * Ported from Pass1.h / Pass1.cpp. The C++ switch over the fix-up marker
 * byte is translated to a chain of {@code if}s instead, since a data byte
 * that is not one of the four markers does not correspond to any
 * {@link FixupType} constant and cannot be switched on directly; the
 * {@code SET_BLOCK_ADDR} branch intentionally falls through into the
 * shared "apply data byte" step below it, exactly like the C++ source.
 *
 * @author Peter Dell
 */
public final class Pass1 {

	private Pass1() {
	}

	public static void execute(Workspace workspace) {
		SegmentList segmentList = workspace.getSegmentList();
		for (int segmentIndex = 0; segmentIndex < segmentList.getCount(); segmentIndex++) {
			Segment segment = segmentList.getSegment(segmentIndex);
			if (segment.isHeader(FileHeader.SDX_SYM_DEFINED) || segment.isSDXRelocBlkWithoutData()) {
				Segment labelSegment = segmentList.findBySDXBlockNumber(segment.bSDXBlockNumber);
				if (labelSegment != null) {
					labelSegment.getFixupAddressLabels().allocateAddressLabel(segment.wBegin);
				}
			} else if (segment.isHeader(FileHeader.SDX_SYM_REQUIRED) || segment.isHeader(FileHeader.SDX_FIX_UP_BLK)) {
				executeFixupBlock(segment, segmentList);
			}
		}
	}

	private static void executeFixupBlock(Segment segment, SegmentList segmentList) {
		int startAddr = 0;
		Segment blockSegment = null;

		MemoryBlockIterator iterator = new MemoryBlockIterator(segment.memoryBlock);
		while (iterator.hasNext() && iterator.getData() != FixupType.END.getValue()) {
			int data = iterator.nextData();
			FixupType fixupType = FixupType.valueOf(data);

			if (fixupType == FixupType.ADD_250_BYTES) {
				startAddr += 250;
				continue;
			}
			if (fixupType == FixupType.SET_BLOCK_NUM) {
				int sdxBlockNumber = iterator.nextData();
				blockSegment = segmentList.findBySDXBlockNumber(sdxBlockNumber);
				if (blockSegment != null) {
					startAddr = blockSegment.wBegin;
				}
				continue;
			}
			if (fixupType == FixupType.SET_BLOCK_ADDR) {
				startAddr = iterator.nextAddress();
				int blockSegmentIndex = segmentList.findByAddr(startAddr);
				blockSegment = (blockSegmentIndex != SegmentList.NO_SEGMENT_INDEX)
						? segmentList.getSegment(blockSegmentIndex)
						: null;
				data = 0;
				// Falls through intentionally, matching the C++ switch fall-through.
			}

			startAddr += data;
			if (blockSegment != null) {
				if (segment.isHeader(FileHeader.SDX_SYM_REQUIRED)) {
					blockSegment.allocateSymbol(startAddr, segment.sdxSymbol);
				} else if (segment.isHeader(FileHeader.SDX_FIX_UP_BLK)) {
					Segment labelSegment = segmentList.findBySDXBlockNumber(segment.bSDXBlockNumber);
					if (labelSegment != null) {
						blockSegment.allocateFixup(segmentList.getSegmentIndex(labelSegment), labelSegment, startAddr);
					}
				}
			}
		}
	}
}
