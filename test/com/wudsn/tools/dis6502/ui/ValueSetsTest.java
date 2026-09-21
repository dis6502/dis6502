/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.ui;

import com.wudsn.tools.base.repository.ValueSet;
import com.wudsn.tools.dis6502.model.Assert;
import com.wudsn.tools.dis6502.model.AtariError;
import com.wudsn.tools.dis6502.model.ComputerSystemType;
import com.wudsn.tools.dis6502.model.ProcessorType;

/**
 * New (not ported) test for the value sets {@link ComputerSystemType},
 * {@link ProcessorType} and {@link GraphicMode} (see {@code EncodingTest},
 * {@code FolderTypeTest} and {@code FileChoosersTest} for the others), and
 * for {@link AtariError}'s texts, which come from {@code Messages} instead.
 * A value whose text were missing from {@code ValueSets.properties} would
 * not get this far: the repository loader ends the program at class load.
 *
 * @author Peter Dell
 */
public final class ValueSetsTest {

	private ValueSetsTest() {
	}

	public static void testValueSets() {
		// ComputerSystemType: the id is what workspace files, settings and the command line use.
		Assert.stringEquals(ComputerSystemType.ATARI800.getId(), "ATARI800");
		Assert.stringEquals(ComputerSystemType.ATARI800.getText(), "Atari 800");
		Assert.stringEquals(ComputerSystemType.ATARI800.getFileName(), "Atari800");
		Assert.stringEquals(ComputerSystemType.ORIC.toString(), "Oric");
		Assert.boolEquals(ComputerSystemType.fromId("C64") == ComputerSystemType.C64, true);
		Assert.boolEquals(ComputerSystemType.fromId("Atari 800") == ComputerSystemType.UNKNOWN, true); // A text is no id.
		Assert.longEquals(ComputerSystemType.getValues().size(), 5);
		Assert.longEquals(ComputerSystemType.getSelectableValues().size(), 4);
		Assert.boolEquals(ComputerSystemType.getSelectableValues().contains(ComputerSystemType.UNKNOWN), false);
		// The order the New Workspace dialog offers them in.
		Assert.boolEquals(ValueSet.getValues(ComputerSystemType.class).get(0) == ComputerSystemType.ATARI5200, true);

		// ProcessorType: the key is what a workspace file contains.
		Assert.stringEquals(ProcessorType.MOS65C02.getKey(), "MOS65C02");
		Assert.stringEquals(ProcessorType.MOS65C02.getText(), "MOS 65C02");
		Assert.boolEquals(ProcessorType.fromKey("MOS6502") == ProcessorType.MOS6502, true);
		Assert.boolEquals(ProcessorType.fromKey("Z80") == ProcessorType.UNKNOWN, true);
		Assert.longEquals(ProcessorType.getSelectableValues().size(), 2);

		// GraphicMode: eight modes, offered in mode number order, each with its numbers.
		Assert.longEquals(GraphicMode.getValues().size(), 8);
		Assert.boolEquals(ValueSet.getValues(GraphicMode.class).get(0) == GraphicMode.ANTIC_8, true);
		Assert.boolEquals(ValueSet.getValues(GraphicMode.class).get(7) == GraphicMode.ANTIC_F, true);
		Assert.stringEquals(GraphicMode.ANTIC_F.getText(), "ANTIC F (320 x 192 Pixels, 2 Colors)");
		Assert.boolEquals(GraphicMode.forAnticMode(13) == GraphicMode.ANTIC_D, true);
		Assert.longEquals(GraphicMode.ANTIC_D.bytesPerLine, 40);
		Assert.longEquals(GraphicMode.ANTIC_D.bitsPerPixel(), 2);
		Assert.longEquals(GraphicMode.ANTIC_F.bitsPerPixel(), 1);

		// AtariError: every error has a text of its own.
		java.util.Set<String> texts = new java.util.HashSet<String>();
		for (AtariError error : AtariError.values()) {
			Assert.boolEquals(error.getErrorText().isEmpty(), false);
			texts.add(error.getErrorText());
		}
		Assert.longEquals(texts.size(), AtariError.values().length);
		Assert.stringEquals(AtariError.DISK_NOT_FOUND.getErrorText(), "Disk not found");

		Assert.log("ValueSetsTest completed");
	}
}
