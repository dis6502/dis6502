/*
 * Copyright (C) 2025 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

/**
 * A comment attached to one memory offset within a {@link Segment}; its text
 * can be multi-line.
 * <p>
 * Ported from Comment.h / Comment.cpp. {@code SerializeTo}/{@code
 * DeserializeFrom} (XML persistence) are not ported yet.
 *
 * @author Peter Dell
 */
public final class Comment {

	private int offset;
	private String text = "";

	public void clear() {
		offset = 0;
		setText("");
	}

	public int getOffset() {
		return offset;
	}

	public void setOffset(int offset) {
		this.offset = offset;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text.trim(); // Trim trailing (and leading) whitespace.
	}
}
