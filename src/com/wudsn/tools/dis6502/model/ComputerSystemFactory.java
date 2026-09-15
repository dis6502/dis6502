/*
 * Copyright (C) 2025 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

/**
 * Creates and holds the singleton {@link ComputerSystem} instance for each
 * {@link ComputerSystemType}.
 * <p>
 * Ported from systems/ComputerSystemFactory.h / ComputerSystemFactory.cpp.
 * {@code ATARI800} is not ported yet ({@link Atari800} needs the much
 * larger AtariDOS/disk-image subsystem, deferred); {@link
 * #getComputerSystem(ComputerSystemType)} throws for it in the meantime.
 *
 * @author Peter Dell
 */
public final class ComputerSystemFactory {

	private static final ComputerSystemTypeInfo UNKNOWN_INFO = new ComputerSystemTypeInfo(ComputerSystemType.UNKNOWN,
			"UNKNOWN", "Unknown", "Unknown");
	private static final ComputerSystemTypeInfo ATARI800_INFO = new ComputerSystemTypeInfo(
			ComputerSystemType.ATARI800, "ATARI800", "Atari 800", "Atari800");
	private static final ComputerSystemTypeInfo ATARI5200_INFO = new ComputerSystemTypeInfo(
			ComputerSystemType.ATARI5200, "ATARI5200", "Atari 5200", "Atari5200");
	private static final ComputerSystemTypeInfo C64_INFO = new ComputerSystemTypeInfo(ComputerSystemType.C64, "C64",
			"C64", "C64");
	private static final ComputerSystemTypeInfo ORIC_INFO = new ComputerSystemTypeInfo(ComputerSystemType.ORIC,
			"ORIC", "Oric", "Oric");

	private final Atari5200 atari5200;
	private final C64 c64;
	private final Oric oric;
	private final Unknown unknown;

	public ComputerSystemFactory() {
		atari5200 = new Atari5200(getComputerSystemTypeInfo(ComputerSystemType.ATARI5200));
		c64 = new C64(getComputerSystemTypeInfo(ComputerSystemType.C64));
		oric = new Oric(getComputerSystemTypeInfo(ComputerSystemType.ORIC));
		unknown = new Unknown(getComputerSystemTypeInfo(ComputerSystemType.UNKNOWN));
	}

	public ComputerSystemType getComputerSystemType(String id) {
		switch (id) {
		case "ATARI5200":
			return ComputerSystemType.ATARI5200;
		case "ATARI800":
			return ComputerSystemType.ATARI800;
		case "C64":
			return ComputerSystemType.C64;
		case "ORIC":
			return ComputerSystemType.ORIC;
		default:
			return ComputerSystemType.UNKNOWN;
		}
	}

	public ComputerSystemTypeInfo getComputerSystemTypeInfo(ComputerSystemType type) {
		switch (type) {
		case UNKNOWN:
			return UNKNOWN_INFO;
		case ATARI800:
			return ATARI800_INFO;
		case ATARI5200:
			return ATARI5200_INFO;
		case C64:
			return C64_INFO;
		case ORIC:
			return ORIC_INFO;
		default:
			throw new IllegalArgumentException("Unknown computer system type: " + type + ".");
		}
	}

	public ComputerSystem getComputerSystem(ComputerSystemType type) {
		switch (type) {
		case UNKNOWN:
			return unknown;
		case ATARI5200:
			return atari5200;
		case C64:
			return c64;
		case ORIC:
			return oric;
		case ATARI800:
			throw new UnsupportedOperationException("Atari800 is not ported yet.");
		default:
			throw new IllegalArgumentException("Invalid computer system type: " + type + ".");
		}
	}
}
