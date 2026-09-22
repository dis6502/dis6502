/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

import org.w3c.dom.Element;

/**
 * A comment attached to one memory offset within a {@link Segment}; its text
 * can be multi-line.
 *
 * @author Peter Dell
 */
public final class Comment implements Xml.Serializable {

	private int offset; // Memory offset of the byte owning the comment.
	private String text = ""; // Text of the comment (can be multi-line).

	public void clear() {
		offset = 0;
		setText("");
	}

	@Override
	public void serializeTo(Element element) {
		Xml.setWordAttributeHex(element, "Offset", offset);
		Xml.setStringAttribute(element, "Text", text);
	}

	@Override
	public void deserializeFrom(Element element) {
		offset = Xml.getWordAttribute(element, "Offset", offset);
		text = Xml.getStringAttribute(element, "Text", text);
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
