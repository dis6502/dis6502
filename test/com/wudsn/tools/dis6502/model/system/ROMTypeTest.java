/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model.system;

import com.wudsn.tools.dis6502.model.Assert;

/**
 * A {@link ROMType} is identified by its computer system type and its id: the
 * same id of two systems is a different ROM type; the text does not count.
 *
 * @author Peter Dell
 */
public final class ROMTypeTest {

	private ROMTypeTest() {
	}

	public static void testROMType() {
		ROMType xegs = new ROMType(ComputerSystemType.ATARI800, "CARTRIDGE_XEGS_64", "XEGS 64 KB (13)");
		Assert.boolEquals(xegs.getComputerSystemType() == ComputerSystemType.ATARI800, true);
		Assert.stringEquals(xegs.getId(), "CARTRIDGE_XEGS_64");
		Assert.stringEquals(xegs.getText(), "XEGS 64 KB (13)");
		Assert.stringEquals(xegs.toString(), "XEGS 64 KB (13)");

		ROMType sameIdOtherText = new ROMType(ComputerSystemType.ATARI800, "CARTRIDGE_XEGS_64", "XEGS");
		Assert.boolEquals(xegs.equals(sameIdOtherText), true);
		Assert.longEquals(xegs.hashCode(), sameIdOtherText.hashCode());

		ROMType sameIdOtherSystem = new ROMType(ComputerSystemType.C64, "CARTRIDGE_XEGS_64", "XEGS 64 KB (13)");
		Assert.boolEquals(xegs.equals(sameIdOtherSystem), false);
		Assert.boolEquals(xegs.equals(new ROMType(ComputerSystemType.ATARI800, "CARTRIDGE_WILL_64", "")), false);
		Assert.boolEquals(xegs.equals("CARTRIDGE_XEGS_64"), false);
	}
}
