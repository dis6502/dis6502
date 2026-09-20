/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

import java.util.List;

/**
 * Ported from SegmentListChangedListener.h.
 *
 * @author Peter Dell
 */
public interface SegmentListChangedListener {
	void handleSegmentListChanged(SegmentList segmentList, List<SegmentList.Property> propertyChangeEvents);
}
