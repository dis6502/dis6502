/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model.system.atari800;

/**
 * A request/result packet for reading one sector from a disk image via
 * {@link DiskImage}.
 * <p>
 * {@code sectorSize} is not set by {@link DiskImage#readSector} itself -
 * callers set it once (from an {@link ImgInfo#density} they already
 * fetched) and it stays fixed for the packet's lifetime; see {@link
 * DiskImage}'s javadoc for why that is harmless. {@code sectorData} is
 * fixed at 256 bytes, the largest sector size the format supports.
 *
 * @author Peter Dell
 */
public final class ImgRWPacket {

	public String filePath = "";
	public int sectorNumber;
	public final byte[] sectorData = new byte[256];
	public int sectorSize;
	public ImgError result = ImgError.DISK_ERROR;
}
