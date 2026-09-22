/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.wudsn.tools.base.repository.ValueSet;
import com.wudsn.tools.dis6502.ValueSets;

/**
 * The characteristics of one Atari ANTIC graphics mode (8 through 15), as
 * used to render a raw byte buffer as a picture in {@link GraphicPanel}.
 * <p>
 * See {@link SelectGraphicsDialog}'s own javadoc for why this port calls
 * these "Graphic" modes rather than "Sprite". This is a WUDSN Base {@link
 * ValueSet}, not a Java {@code enum}, with the per-mode numbers as
 * additional attributes of each value; {@link #getText()} comes from
 * {@code GraphicMode_ANTIC_8}..{@code _F} in {@code ValueSets.properties}.
 * {@code colors} is kept even though nothing actually uses it for pixel
 * decoding - that is fixed per mode via {@link #bitsPerPixel} instead.
 * Note {@link #ANTIC_F}'s text says "2 Colors" while {@code colors}
 * records 1 for it - both are defensible (it renders exactly two colors,
 * black and white, using 1 bit per pixel) and this keeps the mismatch as
 * is rather than guessing which one to "fix".
 *
 * @author Peter Dell
 */
public final class GraphicMode extends ValueSet {

	public static final GraphicMode ANTIC_8;
	public static final GraphicMode ANTIC_9;
	public static final GraphicMode ANTIC_A;
	public static final GraphicMode ANTIC_B;
	public static final GraphicMode ANTIC_C;
	public static final GraphicMode ANTIC_D;
	public static final GraphicMode ANTIC_E;
	public static final GraphicMode ANTIC_F;

	private static final Map<String, GraphicMode> values;

	/** The ANTIC mode number (8-15), matching {@code SetMode}'s expected range. */
	public final int anticMode;
	public final int colors;
	/** Maximum bytes per displayed line for this mode. */
	public final int bytesPerLine;
	public final int pixelsPerByte;
	/** Width, in screen pixels, of one logical pixel in this mode. */
	public final int pixelWidth;
	/** Height, in screen pixels, of one logical pixel in this mode - also the vertical step between displayed buffer lines. */
	public final int pixelHeight;

	static {
		values = new LinkedHashMap<String, GraphicMode>();

		ANTIC_8 = add("ANTIC_8", 8, 4, 10, 4, 8, 8);
		ANTIC_9 = add("ANTIC_9", 9, 2, 10, 8, 4, 4);
		ANTIC_A = add("ANTIC_A", 10, 4, 20, 4, 4, 4);
		ANTIC_B = add("ANTIC_B", 11, 2, 20, 8, 2, 2);
		ANTIC_C = add("ANTIC_C", 12, 2, 20, 8, 2, 1);
		ANTIC_D = add("ANTIC_D", 13, 4, 40, 4, 2, 2);
		ANTIC_E = add("ANTIC_E", 14, 4, 40, 4, 2, 1);
		ANTIC_F = add("ANTIC_F", 15, 1, 40, 8, 1, 1);

		initializeClass(GraphicMode.class, ValueSets.class);
	}

	private GraphicMode(String id, int anticMode, int colors, int bytesPerLine, int pixelsPerByte, int pixelWidth, int pixelHeight) {
		super(id, id, anticMode); // Sorted by mode number.
		this.anticMode = anticMode;
		this.colors = colors;
		this.bytesPerLine = bytesPerLine;
		this.pixelsPerByte = pixelsPerByte;
		this.pixelWidth = pixelWidth;
		this.pixelHeight = pixelHeight;
	}

	private static GraphicMode add(String id, int anticMode, int colors, int bytesPerLine, int pixelsPerByte, int pixelWidth,
			int pixelHeight) {
		GraphicMode result = new GraphicMode(id, anticMode, colors, bytesPerLine, pixelsPerByte, pixelWidth, pixelHeight);
		values.put(id, result);
		return result;
	}

	/** Gets the unmodifiable list of all values. */
	public static List<GraphicMode> getValues() {
		return Collections.unmodifiableList(new ArrayList<GraphicMode>(values.values()));
	}

	/** 2 bits for a 4-color mode, 1 bit otherwise. */
	public int bitsPerPixel() {
		return colors == 4 ? 2 : 1;
	}

	public static GraphicMode forAnticMode(int anticMode) {
		for (GraphicMode mode : values.values()) {
			if (mode.anticMode == anticMode) {
				return mode;
			}
		}
		throw new IllegalArgumentException("Invalid ANTIC mode: " + anticMode + ".");
	}
}
