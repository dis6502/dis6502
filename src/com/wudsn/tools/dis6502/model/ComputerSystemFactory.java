/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

/**
 * Creates and holds the singleton {@link ComputerSystem} instance for each
 * {@link ComputerSystemType}.
 * <p>
 * Ported from systems/ComputerSystemFactory.h / ComputerSystemFactory.cpp.
 *
 * @author Peter Dell
 */
public final class ComputerSystemFactory {

	private final Atari800 atari800;
	private final Atari5200 atari5200;
	private final C64 c64;
	private final Oric oric;
	private final Unknown unknown;

	public ComputerSystemFactory() {
		atari800 = new Atari800(ComputerSystemType.ATARI800);
		atari5200 = new Atari5200(ComputerSystemType.ATARI5200);
		c64 = new C64(ComputerSystemType.C64);
		oric = new Oric(ComputerSystemType.ORIC);
		unknown = new Unknown(ComputerSystemType.UNKNOWN);
	}

	public ComputerSystem getComputerSystem(ComputerSystemType type) {
		// An if chain, not a switch: ComputerSystemType is a ValueSet, not an enum.
		if (type == ComputerSystemType.UNKNOWN) {
			return unknown;
		} else if (type == ComputerSystemType.ATARI800) {
			return atari800;
		} else if (type == ComputerSystemType.ATARI5200) {
			return atari5200;
		} else if (type == ComputerSystemType.C64) {
			return c64;
		} else if (type == ComputerSystemType.ORIC) {
			return oric;
		}
		throw new IllegalArgumentException("Invalid computer system type: " + type + ".");
	}
}
