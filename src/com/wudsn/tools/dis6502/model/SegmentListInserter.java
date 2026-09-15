/*
 * Copyright (C) 2025 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

/**
 * A scope-guarded batch of segment insertions into a {@link SegmentList}:
 * either all inserted segments are kept (via {@link #apply()}) or none are
 * (via {@link #cancel()}, or automatically via {@link #close()} if never
 * applied).
 * <p>
 * Ported from SegmentListInserter.h / SegmentListInserter.cpp. The C++
 * version cancels automatically in its destructor if still active when it
 * goes out of scope; Java has no equivalent deterministic destruction, so
 * this implements {@link AutoCloseable} instead - use it in a
 * try-with-resources block to get the same effect. The C++ "copy
 * constructor used only to support unique_ptr assignment" is not ported; it
 * has no Java equivalent use.
 *
 * @author Peter Dell
 */
public final class SegmentListInserter implements AutoCloseable {

	private final SegmentList segmentList;
	private int firstIndex;
	private int currentIndex;
	private boolean active;

	SegmentListInserter(SegmentList segmentList) {
		this.segmentList = segmentList;
		firstIndex = segmentList.getCount();
		currentIndex = firstIndex;
		segmentList.beginUpdate();
		active = true;
	}

	/** Throws IllegalStateException if no further segment can be inserted. */
	public Segment insertSegment() {
		assert active;

		if (currentIndex == SegmentList.MAX_SEGMENTS) {
			throw new IllegalStateException("No free segment available.");
		}
		Segment segment = segmentList.insertSegmentAt(currentIndex);
		currentIndex++;
		return segment;
	}

	public void removeSegment() {
		assert active;
		assert currentIndex > 0;

		currentIndex--;
		segmentList.deleteSegment(currentIndex);
	}

	public void apply() {
		assert active;

		if (currentIndex > firstIndex) {
			// Position to first added segment.
			segmentList.setSelectedIndex(firstIndex);

			// Currently the insert methods don't fire events directly.
			segmentList.notifyListeners(SegmentList.Property.SEGMENTS);
			firstIndex = segmentList.getCount();
			currentIndex = firstIndex;
			segmentList.endUpdate(); // Commit current transaction.
		}
	}

	public void cancel() {
		assert active;

		// Remove inserted segments in case there was no apply().
		if (currentIndex > firstIndex) {
			while (currentIndex > firstIndex) {
				removeSegment();
			}
			segmentList.endUpdate();
		}
		active = false;
	}

	@Override
	public void close() {
		if (active) {
			cancel();
		}
	}
}
