/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;

/**
 * Low-level API to read/write sectors of an Atari .atr/.xfd disk image, by
 * sector number (1-based).
 * <p>
 * Ported from DiskImage.h / DiskImage.cpp. {@link #writeAbsoluteSector} also
 * folds in {@code AtariDiskImage::WriteAbsoluteSector} (used by {@link
 * com.wudsn.tools.dis6502.ui.SegmentWriteBootDiskDialog}), the same way
 * {@link #readAbsoluteSector} already folds in {@code
 * AtariDiskImage::ReadAbsoluteSector}.
 * <p>
 * {@link #write} checks {@link ImgInfo#writeProtect} up front instead of
 * opening the file and inspecting the resulting exception for an
 * access-denied error, unlike the C++ version's {@code Write} (which opens
 * with {@code "r+b"} and maps an {@code EACCES} failure to {@code
 * ImgError.WRITE_PROTECT}) - the two ways of detecting the same condition
 * produce the same {@link ImgError}.
 * <p>
 * {@link #readSector} reads {@link ImgInfo#density} bytes starting at the
 * seek position computed for the sector - for sector numbers 1-3 on a
 * double-density (256 bytes/sector) disk, that position assumes the
 * standard 128-byte boot sector size, so the read spills 128 bytes into
 * the next sector's data. This matches the C++ source's {@code Read}
 * exactly (it uses {@code info.wDensity} for the actual read, not the
 * locally adjusted density {@code Seek} computes) and is harmless: {@link
 * #readAbsoluteSector} always caps the reported size at 128 for those
 * sector numbers, so the spilled-over bytes are never actually used -
 * matching {@code ImgRWPacket::cSectorData}'s fixed 256-byte capacity,
 * large enough to hold the spillover without overflowing.
 *
 * @author Peter Dell
 */
public final class DiskImage {

	private static final int ATR_MAGIC1 = 0x96;
	private static final int ATR_MAGIC2 = 0x02;
	private static final int ATR_HEADER_SIZE = 16;

	private DiskImage() {
	}

	/** Determines the disk image's type (XFD/ATR), sector size, and sector count from its content/size. */
	public static void getInfo(String filePath, ImgInfo info) {
		File file = new File(filePath);
		if (!file.exists()) {
			info.result = ImgError.FILE_NOT_FOUND;
			return;
		}

		info.result = ImgError.BAD_MAGIC;
		info.writeProtect = !file.canWrite();

		byte[] fileContent;
		try {
			fileContent = Files.readAllBytes(file.toPath());
		} catch (IOException ex) {
			info.result = ImgError.DISK_ERROR;
			return;
		}
		if (fileContent.length < ATR_HEADER_SIZE) {
			info.result = ImgError.DISK_ERROR;
			return;
		}

		int magic1 = fileContent[0] & 0xFF;
		int magic2 = fileContent[1] & 0xFF;
		if (magic1 == ATR_MAGIC1 && magic2 == ATR_MAGIC2) {
			info.result = ImgError.ATR;
			info.density = readWordLE(fileContent, 4);

			long sectorCount = readWordLE(fileContent, 2) | ((long) readWordLE(fileContent, 6) << 16);
			sectorCount >>= 3;
			if (info.density == 256) {
				sectorCount += 3;
				sectorCount >>= 1;
			}
			info.sectors = (int) sectorCount;
			return;
		}

		// Not an ATR file - check if the plain size matches a known XFD layout.
		int size = fileContent.length;
		if (size == 40 * 18 * 128) {
			info.result = ImgError.XFD;
			info.sectors = 40 * 18;
			info.density = 128;
		} else if (size == 40 * 26 * 128) {
			info.result = ImgError.XFD;
			info.sectors = 40 * 26;
			info.density = 128;
		} else if (size == 40 * 18 * 256) {
			info.result = ImgError.XFD;
			info.sectors = 40 * 18;
			info.density = 256;
		} else if (size == 80 * 18 * 256) {
			info.result = ImgError.XFD;
			info.sectors = 80 * 18;
			info.density = 256;
		} else {
			info.result = ImgError.BAD_MAGIC;
			info.sectors = 0;
			info.density = 0;
		}
	}

	private static int readWordLE(byte[] buffer, int offset) {
		return (buffer[offset] & 0xFF) | ((buffer[offset + 1] & 0xFF) << 8);
	}

	/** Reads {@code sector.sectorNumber} into {@code sector.sectorData}, setting {@code sector.result}. */
	public static void readSector(ImgRWPacket sector) {
		ImgInfo info = new ImgInfo();
		getInfo(sector.filePath, info);

		if (isError(info.result)) {
			sector.result = info.result;
		} else {
			sector.result = read(info, sector);
		}
	}

	/** Reads sector {@code sectorNumber}, returning the actual byte count read (128 for sector 1-3, otherwise the disk's sector size) in {@code size[0]}, or 0 on error. */
	public static void readAbsoluteSector(ImgRWPacket sector, int sectorNumber, int[] size) {
		sector.sectorNumber = sectorNumber;
		size[0] = 0;

		readSector(sector);

		if (!isError(sector.result)) {
			size[0] = sectorNumber <= 3 ? 128 : sector.sectorSize;
		}
	}

	/** Writes {@code sector.sectorData} to {@code sector.sectorNumber}, setting {@code sector.result}. */
	public static void writeSector(ImgRWPacket sector) {
		ImgInfo info = new ImgInfo();
		getInfo(sector.filePath, info);

		if (isError(info.result)) {
			sector.result = info.result;
		} else {
			sector.result = write(info, sector);
		}
	}

	/**
	 * Writes {@code sectorData} (up to {@link ImgInfo#density} bytes, zero-padded)
	 * to sector {@code sectorNumber} of {@code filePath}. Ported from {@code
	 * AtariDiskImage::WriteAbsoluteSector}, merged into {@link DiskImage} the same
	 * way {@link #readAbsoluteSector} already is - see this class's javadoc.
	 * <p>
	 * Like the C++ version, a failure to determine the disk image's info (a
	 * missing/corrupt file) is silently ignored rather than reported back to the
	 * caller - {@code AtariDiskImage::WriteAbsoluteSector} is {@code void} and
	 * never surfaces {@code DiskImage::WriteSector}'s result either.
	 */
	public static void writeAbsoluteSector(String filePath, int sectorNumber, byte[] sectorData) {
		ImgInfo info = new ImgInfo();
		getInfo(filePath, info);
		if (isError(info.result)) {
			return;
		}

		ImgRWPacket sector = new ImgRWPacket();
		sector.filePath = filePath;
		sector.sectorNumber = sectorNumber;
		sector.sectorSize = info.density;
		System.arraycopy(sectorData, 0, sector.sectorData, 0, Math.min(sectorData.length, sector.sectorData.length));

		writeSector(sector);
	}

	private static ImgError write(ImgInfo info, ImgRWPacket sector) {
		if (sector.sectorNumber < 1 || sector.sectorNumber > info.sectors) {
			return ImgError.OUT_OF_RANGE;
		}
		if (info.writeProtect) {
			return ImgError.WRITE_PROTECT;
		}

		int seekDensity = (info.density == 256 && sector.sectorNumber <= 3) ? 128 : info.density;
		long offset;
		switch (info.result) {
		case XFD:
			offset = (long) (sector.sectorNumber - 1) * seekDensity;
			break;
		case ATR:
			offset = ATR_HEADER_SIZE + (long) (sector.sectorNumber - 1) * seekDensity;
			break;
		default:
			return ImgError.DISK_ERROR;
		}

		try (RandomAccessFile file = new RandomAccessFile(sector.filePath, "rw")) {
			file.seek(offset);
			file.write(sector.sectorData, 0, info.density);
			return info.result;
		} catch (IOException ex) {
			return ImgError.DISK_ERROR;
		}
	}

	private static ImgError read(ImgInfo info, ImgRWPacket sector) {
		if (sector.sectorNumber < 1 || sector.sectorNumber > info.sectors) {
			return ImgError.OUT_OF_RANGE;
		}

		int seekDensity = (info.density == 256 && sector.sectorNumber <= 3) ? 128 : info.density;
		long offset;
		switch (info.result) {
		case XFD:
			offset = (long) (sector.sectorNumber - 1) * seekDensity;
			break;
		case ATR:
			offset = ATR_HEADER_SIZE + (long) (sector.sectorNumber - 1) * seekDensity;
			break;
		default:
			return ImgError.DISK_ERROR;
		}

		try (RandomAccessFile file = new RandomAccessFile(sector.filePath, "r")) {
			file.seek(offset);
			file.readFully(sector.sectorData, 0, info.density);
			return info.result;
		} catch (IOException ex) {
			return ImgError.DISK_ERROR;
		}
	}

	public static boolean isError(ImgError error) {
		return error != ImgError.XFD && error != ImgError.ATR;
	}
}
