/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;

/**
 * A DOS 2.x file system driver for Atari disk images (.atr/.xfd): directory
 * traversal, VTOC (free-sector bitmap) allocation, and sector-chain
 * read/write.
 * <p>
 * Opens and closes the disk image file anew via {@link RandomAccessFile}
 * for every single operation, even within a loop over several sectors,
 * rather than keeping a file handle open across a whole traversal.
 * <p>
 * A missing disk image file is reported consistently: every method catches
 * the open failure and returns {@link AtariError#DISK_NOT_FOUND}.
 *
 * @author Peter Dell
 */
public final class AtariDOS {

	private static final int DIRECTORY_ENTRIES_PER_SECTOR = 8;

	private AtariDOS() {
	}

	public static String getFileName83(String fileName) {
		if (fileName.isEmpty()) {
			return "";
		}

		char[] result = "        .   ".toCharArray();
		int i = 0;
		int offset = 0;
		while (i < fileName.length() && fileName.charAt(i) != '.') {
			char c = fileName.charAt(i++);
			if (offset < 8) {
				result[offset++] = c;
			}
		}
		while (i < fileName.length() && fileName.charAt(i++) != '.') {
			// Skip to (and past) the dot, if any.
		}
		offset = 9;
		while (i < fileName.length() && offset < 12) {
			result[offset++] = fileName.charAt(i++);
		}

		return new String(result);
	}

	public static AtariDisk openAtariDisk(String diskImageFilePath) {
		return new AtariDisk(diskImageFilePath);
	}

	// ------------------------------------------------------------------
	// Methods using a RandomAccessFile.
	// ------------------------------------------------------------------

	/** BEWARE! {@code sectorNumber} starts from 0 instead of 1. */
	static AtariError seek(RandomAccessFile raf, long sectorNumber) throws IOException {
		// Determine if we have an XFD or ATR file, based on the file size.
		long size = raf.length();
		if (size == 92160L || size == 133120L) {
			raf.seek(sectorNumber * 128L);
		} else if (size == 92176L || size == 133136L) {
			raf.seek(sectorNumber * 128L + 16L);
		} else {
			return AtariError.SECTOR_NOT_FOUND;
		}
		return AtariError.OK;
	}

	static AtariError findDirEntry(RandomAccessFile raf, AtariFile info) throws IOException {
		// Go to the next entry in the directory.
		info.directoryIndex++;
		if (info.directoryIndex < 0 || info.directoryIndex > 63) {
			return AtariError.NO_ENTRY_FOUND;
		}

		byte[] sector = new byte[128];

		// Scan all directory sectors to find the first valid entry.
		int firstSectorNumber = 360 + info.directoryIndex / 8;
		for (int sectorNumber = firstSectorNumber; sectorNumber < 368; sectorNumber++) {
			// Go to the directory sector.
			if (seek(raf, sectorNumber) != AtariError.OK) {
				return AtariError.DIRECTORY_NOT_FOUND;
			}

			// The sector is read.
			if (raf.read(sector) != 128) {
				return AtariError.DIRECTORY_READ;
			}

			// Scan all entries of this directory sector (8 entries).
			int firstEntry = sectorNumber == firstSectorNumber ? info.directoryIndex % 8 : 0;

			for (int entry = firstEntry; entry < 8; entry++) {
				// The file status is checked to determine if the file is valid (not deleted).
				int offset = entry * 16;
				if (sector[offset] != 0 && (sector[offset] & FileAttribute.DELETED) == 0) {
					info.clear();
					int nameOffset = offset + 5;
					for (int index = 0; index < 8; index++) {
						char c = (char) (sector[nameOffset + index] & 0xFF);
						if (c != ' ') {
							info.fileName.append(c);
						} else {
							// SPACE defines file name end in DOS 2.x file names.
							break;
						}
					}

					info.fileName.append('.');
					int extOffset = offset + 13;
					for (int index = 0; index < 3; index++) {
						char c = (char) (sector[extOffset + index] & 0xFF);
						if (c != ' ') {
							info.fileName.append(c);
						} else {
							break;
						}
					}

					// Get the file size in number of sectors and keep the index of the file
					// entry so findNext knows where to start.
					info.directoryIndex = (sectorNumber - 360) * 8 + entry;
					int sectorOffset = entry * 16;
					info.fileAttribute = sector[sectorOffset] & 0xFF;
					info.sectorCount = (sector[sectorOffset + 2] & 0xFF) * 256 + (sector[sectorOffset + 1] & 0xFF);
					info.startSectorNumber = (sector[sectorOffset + 4] & 0xFF) * 256
							+ (sector[sectorOffset + 3] & 0xFF);

					return AtariError.OK;
				}
			}
		}

		return AtariError.NO_ENTRY_FOUND;
	}

	static AtariError getFreeDirEntry(RandomAccessFile raf, int[] directoryIndex) throws IOException {
		directoryIndex[0] = -1;

		byte[] sector = new byte[128];

		// Scan all directory sectors to find the first free entry.
		for (int sectorNumber = 360; sectorNumber < 368; sectorNumber++) {
			if (seek(raf, sectorNumber) != AtariError.OK) {
				return AtariError.DIRECTORY_NOT_FOUND;
			}
			if (raf.read(sector) != 128) {
				return AtariError.DIRECTORY_READ;
			}

			// Scan all entries of this directory sector (8 entries).
			for (int entry = 0; entry < DIRECTORY_ENTRIES_PER_SECTOR; entry++) {
				// The file status is checked to determine if the entry is free.
				if ((sector[entry * 16] & FileAttribute.IN_USE) == 0) {
					directoryIndex[0] = (sectorNumber - 360) * DIRECTORY_ENTRIES_PER_SECTOR + entry;
					return AtariError.OK;
				}
			}
		}

		return AtariError.NO_ENTRY_FOUND;
	}

	static AtariError writeDirEntry(RandomAccessFile raf, AtariFile info) throws IOException {
		// Find the directory sector.
		int sectorNumber = 360 + info.directoryIndex / 8;

		if (seek(raf, sectorNumber) != AtariError.OK) {
			return AtariError.DIRECTORY_NOT_FOUND;
		}

		byte[] sector = new byte[128];
		if (raf.read(sector) != 128) {
			return AtariError.DIRECTORY_READ;
		}

		// Find the directory entry in the sector.
		int entry = info.directoryIndex % 8;

		// Write the entry.
		sector[entry * 16] = (byte) info.fileAttribute;
		sector[entry * 16 + 1] = (byte) (info.sectorCount & 0xFF);
		sector[entry * 16 + 2] = (byte) ((info.sectorCount >> 8) & 0xFF);
		sector[entry * 16 + 3] = (byte) (info.startSectorNumber & 0xFF);
		sector[entry * 16 + 4] = (byte) ((info.startSectorNumber >> 8) & 0xFF);

		Arrays.fill(sector, entry * 16 + 5, entry * 16 + 16, (byte) ' ');

		String fileName = info.fileName.toString();
		int i = 0;
		while (i < 8 && i < fileName.length() && fileName.charAt(i) != '.') {
			sector[entry * 16 + 5 + i] = (byte) Character.toUpperCase(fileName.charAt(i));
			i++;
		}

		if (i < fileName.length() && fileName.charAt(i) == '.') {
			i++;
		}

		for (int ext = 0; ext < 3 && i < fileName.length(); ext++, i++) {
			sector[entry * 16 + 13 + ext] = (byte) Character.toUpperCase(fileName.charAt(i));
		}

		// Go to the directory sector.
		if (seek(raf, sectorNumber) != AtariError.OK) {
			return AtariError.DIRECTORY_NOT_FOUND;
		}

		raf.write(sector);

		return AtariError.OK;
	}

	static AtariError allocSector(RandomAccessFile raf, int[] sectorNumber, int directoryIndex) throws IOException {
		AtariError error = seek(raf, 359L);
		if (error != AtariError.OK) {
			return error;
		}

		byte[] sector = new byte[128];
		if (raf.read(sector) != 128) {
			return AtariError.FILE_READ;
		}

		// Check if there is a free sector.
		if (sector[3] == 0 && sector[4] == 0) {
			return AtariError.NO_FREE_SECTOR;
		}

		// Find a free sector.
		for (int index = 10; index <= 125; index++) {
			if (sector[index] != 0) {
				// Find the first set bit (MSB first) and clear it.
				int bitmask = sector[index] & 0xFF;
				int mask = 0x80;
				int bit = 0;
				for (int b = 0; b < 8; b++) {
					if ((bitmask & mask) != 0) {
						bitmask &= ~mask;
						bit = b;
						break;
					}
					mask >>= 1;
				}
				sector[index] = (byte) bitmask;

				// Return the sector number.
				sectorNumber[0] = (index - 10) * 8 + bit;

				// Write a blank sector with the directory entry number filled.
				byte[] data = new byte[128];
				data[125] = (byte) ((directoryIndex & 0x3F) << 2);

				error = seek(raf, sectorNumber[0] - 1);
				if (error != AtariError.OK) {
					return error;
				}
				raf.write(data);

				// Decrement the free sector count.
				if (sector[3] != 0) {
					sector[3] = (byte) (sector[3] - 1);
				} else {
					sector[3] = (byte) 0xFF;
					sector[4] = (byte) (sector[4] - 1);
				}

				error = seek(raf, 359L);
				if (error != AtariError.OK) {
					return error;
				}
				raf.write(sector);

				return AtariError.OK;
			}
		}

		return AtariError.NO_FREE_SECTOR;
	}

	static AtariError freeSector(RandomAccessFile raf, int sectorNumber) throws IOException {
		AtariError error = seek(raf, 359L);
		if (error != AtariError.OK) {
			return error;
		}

		byte[] sector = new byte[128];
		if (raf.read(sector) != 128) {
			return AtariError.BITMAP_READ;
		}

		// Determine the index into the sector and the bit number.
		int index = (sectorNumber - 1) / 8 + 10;
		int bit = (sectorNumber - 1) % 8;
		int mask = 0x80 >> bit;

		// Check if the sector is in use.
		if ((sector[index] & mask) != 0) {
			return AtariError.SECTOR_ALREADY_FREE;
		}

		// Turn the bit on.
		sector[index] = (byte) (sector[index] | mask);

		// Increment the free sector count.
		if ((sector[3] & 0xFF) != 0xFF) {
			sector[3] = (byte) (sector[3] + 1);
		} else {
			sector[3] = 0;
			sector[4] = (byte) (sector[4] + 1);
		}

		error = seek(raf, 359L);
		if (error != AtariError.OK) {
			return error;
		}
		raf.write(sector);

		return AtariError.OK;
	}

	static AtariError seekEndOfFile(RandomAccessFile raf, AtariFile info) throws IOException {
		AtariError error = seek(raf, info.nextSectorNumber - 1);
		if (error != AtariError.OK) {
			return error;
		}

		byte[] sector = new byte[128];
		if (raf.read(sector) != 128) {
			return AtariError.FILE_SEEK;
		}

		// If the sector is empty, we write this one. Otherwise, we must allocate a new one.
		if ((sector[127] & 0x7F) != 0) {
			int[] sectorNumber = new int[1];
			int directoryIndex = 0;
			error = allocSector(raf, sectorNumber, directoryIndex);
			if (error != AtariError.OK) {
				return error;
			}

			// The last sector must be linked with the new one.
			sector[125] = (byte) (sector[125] & ~0x03);
			sector[125] = (byte) (sector[125] | ((sectorNumber[0] >> 8) & 0x03));
			sector[126] = (byte) (sectorNumber[0] & 0xFF);
			error = seek(raf, info.nextSectorNumber - 1);
			if (error != AtariError.OK) {
				return error;
			}
			raf.write(sector);

			// Tell the caller which is the last sector.
			info.nextSectorNumber = sectorNumber[0];

			// Increment the sector count in the directory.
			info.sectorCount++;
			error = writeDirEntry(raf, info);
			if (error != AtariError.OK) {
				return error;
			}
		}

		return AtariError.OK;
	}

	static AtariError readSector(RandomAccessFile raf, AtariFile info) throws IOException {
		// Check for end of file.
		info.sectorSize = 0;
		if (info.nextSectorNumber == 0) {
			return AtariError.END_OF_FILE;
		}

		AtariError error = seek(raf, info.nextSectorNumber - 1);
		if (error != AtariError.OK) {
			return error;
		}

		if (raf.read(info.sector) != 128) {
			return AtariError.FILE_READ;
		}

		// Check if the sector belongs to the file.
		int directoryIndex = (info.sector[125] >> 2) & 0x3F;
		if (directoryIndex != info.directoryIndex) {
			return AtariError.FILE_CORRUPTED;
		}

		// Determine the size of the data and the next sector.
		info.sectorSize = info.sector[127] & 0x7F;

		if ((info.sector[127] & 0x80) != 0) {
			info.nextSectorNumber = 0;
		} else {
			info.nextSectorNumber = ((info.sector[125] & 0x03) << 8) + (info.sector[126] & 0xFF);
		}

		return AtariError.OK;
	}

	// ------------------------------------------------------------------
	// Methods that open/close the disk image file by path.
	// ------------------------------------------------------------------

	/** Fills the structure with the first entry of the directory. Deleted or open-for-output files are skipped. */
	static AtariError findFirst(String diskImageFilePath, AtariFile info) throws IOException {
		try (RandomAccessFile raf = new RandomAccessFile(diskImageFilePath, "r")) {
			info.directoryIndex = -1;
			return findDirEntry(raf, info);
		} catch (FileNotFoundException e) {
			return AtariError.DISK_NOT_FOUND;
		}
	}

	/** Fills the structure with the next entry of the directory. Deleted or open-for-output files are skipped. */
	static AtariError findNext(String diskImageFilePath, AtariFile info) throws IOException {
		try (RandomAccessFile raf = new RandomAccessFile(diskImageFilePath, "r")) {
			return findDirEntry(raf, info);
		} catch (FileNotFoundException e) {
			return AtariError.DISK_NOT_FOUND;
		}
	}

	/** Fills the structure with the entry at the given index, or {@link AtariError#INVALID_VTOC_ENTRY} if there is none. */
	static AtariError getFileFromIndex(String diskImageFilePath, AtariFile info, int directoryIndex)
			throws IOException {
		try (RandomAccessFile raf = new RandomAccessFile(diskImageFilePath, "r")) {
			info.directoryIndex = directoryIndex - 1;
			AtariError error = findDirEntry(raf, info);
			if (error == AtariError.OK && info.directoryIndex != directoryIndex) {
				error = AtariError.INVALID_VTOC_ENTRY;
			}
			return error;
		} catch (FileNotFoundException e) {
			return AtariError.DISK_NOT_FOUND;
		}
	}

	/** Reads the first sector of a file (up to 125 bytes). */
	static AtariError readFirstSector(String diskImageFilePath, AtariFile info) throws IOException {
		try (RandomAccessFile raf = new RandomAccessFile(diskImageFilePath, "r")) {
			// Reset the Atari file pointer to the beginning of the file.
			info.nextSectorNumber = info.startSectorNumber;
			return readSector(raf, info);
		} catch (FileNotFoundException e) {
			return AtariError.DISK_NOT_FOUND;
		}
	}

	/** Reads the next sector of a file (up to 125 bytes). */
	static AtariError readNextSector(String diskImageFilePath, AtariFile info) throws IOException {
		try (RandomAccessFile raf = new RandomAccessFile(diskImageFilePath, "r")) {
			return readSector(raf, info);
		} catch (FileNotFoundException e) {
			return AtariError.DISK_NOT_FOUND;
		}
	}

	/** Checks that a file with the given name exists on the disk. */
	static AtariError checkFile(String diskImageFilePath, AtariFile info, String fileName) throws IOException {
		// Get the first file name of the disk.
		AtariError error = findFirst(diskImageFilePath, info);

		while (error == AtariError.OK) {
			// Check if the new file has the same name as one of the files on the disk.
			if (fileName.equalsIgnoreCase(info.fileName.toString())) {
				return AtariError.OK;
			}

			// Get the next file name.
			error = findNext(diskImageFilePath, info);
		}

		return error;
	}

	/** Creates a new, empty file. */
	static AtariError createFile(String diskImageFilePath, AtariFile info, String fileName) throws IOException {
		AtariError error = checkFile(diskImageFilePath, info, fileName);

		// Check if the file already exists.
		switch (error) {
		case OK:
			return AtariError.FILE_ALREADY_EXISTS;
		case NO_ENTRY_FOUND:
			break;
		default:
			return error;
		}

		try (RandomAccessFile raf = new RandomAccessFile(diskImageFilePath, "rw")) {
			// Find a free directory entry.
			int[] directoryIndex = new int[1];
			error = getFreeDirEntry(raf, directoryIndex);
			if (error != AtariError.OK) {
				return error;
			}
			info.directoryIndex = directoryIndex[0];

			// Find a free sector for the first data sector of the file.
			int[] startSectorNumber = new int[1];
			error = allocSector(raf, startSectorNumber, info.directoryIndex);
			if (error != AtariError.OK) {
				return error;
			}
			info.startSectorNumber = startSectorNumber[0];

			// Fill this entry.
			info.fileName.setLength(0);
			info.fileName.append(fileName);
			info.sectorCount = 1;
			info.fileAttribute = FileAttribute.IN_USE | FileAttribute.UNKNOWN;
			info.nextSectorNumber = info.startSectorNumber;

			// Write the entry into the directory.
			error = writeDirEntry(raf, info);
			if (error != AtariError.OK) {
				freeSector(raf, info.startSectorNumber);
			}

			return error;
		} catch (FileNotFoundException e) {
			return AtariError.DISK_NOT_FOUND;
		}
	}

	static AtariError writeSector(String diskImageFilePath, AtariFile info) throws IOException {
		try (RandomAccessFile raf = new RandomAccessFile(diskImageFilePath, "rw")) {
			// Find the last sector where we can write the data.
			AtariError error = seekEndOfFile(raf, info);
			if (error != AtariError.OK) {
				return error;
			}

			// Fill the sector information.
			info.sector[125] = (byte) ((info.directoryIndex & 0x3F) << 2);
			info.sector[126] = 0;
			info.sector[127] = (byte) (info.sectorSize & 0x7F);

			error = seek(raf, info.nextSectorNumber - 1);
			if (error != AtariError.OK) {
				return error;
			}

			raf.write(info.sector);

			return AtariError.OK;
		} catch (FileNotFoundException e) {
			return AtariError.DISK_NOT_FOUND;
		}
	}

	/** @param fileSize single-element out parameter (index 0). */
	static AtariError getFileSize(String diskImageFilePath, AtariFile info, long[] fileSize) throws IOException {
		// Read all sectors of the file.
		fileSize[0] = 0;
		AtariError error = readFirstSector(diskImageFilePath, info);

		while (error == AtariError.OK && info.sectorSize > 0) {
			fileSize[0] += info.sectorSize;
			error = readNextSector(diskImageFilePath, info);
		}

		if (error == AtariError.END_OF_FILE) {
			error = AtariError.OK;
		}

		if (error != AtariError.OK) {
			fileSize[0] = 0;
		}

		return error;
	}

	/** @param diskIndex single-element in/out parameter (index 0): pass {-1} to start reading from the beginning of the file. */
	static AtariError readFile(String diskImageFilePath, AtariFile info, int[] diskIndex, byte[] fileBuffer,
			int fileSize) throws IOException {
		AtariError error = AtariError.OK;

		// Read the first sector if we are at the beginning.
		if (diskIndex[0] == -1) {
			error = readFirstSector(diskImageFilePath, info);
			if (error != AtariError.OK) {
				return error;
			}
			diskIndex[0] = 0;
		}

		// Copy bytes from the sector to the caller's buffer up to the specified length.
		int destOffset = 0;
		int remaining = fileSize;
		int sectorLength = info.sectorSize - diskIndex[0];

		while (sectorLength < remaining) {
			if (sectorLength > 0) {
				System.arraycopy(info.sector, diskIndex[0], fileBuffer, destOffset, sectorLength);
				destOffset += sectorLength;
				remaining -= sectorLength;
			}

			diskIndex[0] = 0;
			error = readNextSector(diskImageFilePath, info);

			if (error != AtariError.OK) {
				info.sectorSize = 0;
				return error;
			}

			sectorLength = info.sectorSize - diskIndex[0];
		}

		System.arraycopy(info.sector, diskIndex[0], fileBuffer, destOffset, remaining);
		diskIndex[0] += remaining;

		return AtariError.OK;
	}
}
