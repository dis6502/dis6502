/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

/**
 * DOS 2.x directory entry file attribute bits.
 * <p>
 * A plain {@code int}-based flags class rather than a Java {@code enum},
 * since values are combined with bitwise OR - the same reasoning as {@link
 * LabelAccess}.
 *
 * @author Peter Dell
 */
public final class FileAttribute {

	public static final int OPEN_FOR_OUTPUT = 0x01;
	public static final int UNKNOWN = 0x02;
	public static final int LOCKED = 0x20;
	public static final int IN_USE = 0x40;
	public static final int DELETED = 0x80;

	private FileAttribute() {
	}
}
