/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model.system.atari800;

/**
 * Information about a .xfd/.atr disk image: its type, sector size, sector
 * count, and write-protect state, as filled in by {@link
 * DiskImage#getInfo}.
 * <p>
 * Fields are public and mutable, used as a plain out-parameter struct.
 *
 * @author Peter Dell
 */
public final class ImgInfo {

	public ImgError result = ImgError.DISK_ERROR;
	public int density;
	public int sectors;
	public boolean writeProtect;
}
