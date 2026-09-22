/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

import com.wudsn.tools.dis6502.Messages;

/**
 * A scope-guarded batch of segment insertions into a {@link SegmentList}:
 * either all inserted segments are kept (via {@link #apply()}) or none are
 * (via {@link #cancel()}, or automatically via {@link #close()} if never
 * applied).
 * <p>
 * Implements {@link AutoCloseable}: use it in a try-with-resources block,
 * so an unapplied batch is automatically cancelled when the block exits.
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

	/**
	 * Throws IllegalStateException ({@link Messages#E035}) if no further
	 * segment can be inserted, kept as an unchecked exception here since
	 * this class has no {@code Application} reference to log through and
	 * its call sites don't expect a checked one.
	 */
	public Segment insertSegment() {
		assert active;

		if (currentIndex == SegmentList.MAX_SEGMENTS) {
			throw new IllegalStateException(Messages.E035.format());
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
