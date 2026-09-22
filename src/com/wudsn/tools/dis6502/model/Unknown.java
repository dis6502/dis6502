/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

import java.util.List;

/**
 * Fallback computer system for a workspace with no specific target system.
 *
 * @author Peter Dell
 */
public final class Unknown extends ComputerSystem {

	public Unknown(ComputerSystemType computerSystemType) {
		super(computerSystemType);
		returnCharacter = 0x0a;
		supportedFileTypes = List.of(FileType.RAW_FILE);
	}

	@Override
	public boolean isBaseAddress(int address) {
		return false;
	}

	@Override
	public boolean isVectorAddress(int address) {
		return false;
	}

	@Override
	public boolean isDisplayListVectorAddress(int address) {
		return false;
	}

	@Override
	public FileType guessFileType(long fileSize, byte[] content) {
		return FileType.ANY_FILE;
	}
}
