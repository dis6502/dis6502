/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502;

import java.io.File;

import com.wudsn.tools.dis6502.model.Assert;
import com.wudsn.tools.dis6502.model.ComputerSystemFactory;

/**
 * New (not ported) test for {@link CommandLineArguments}.
 *
 * @author Peter Dell
 */
public final class CommandLineArgumentsTest {

	private CommandLineArgumentsTest() {
	}

	public static void testCommandLineArguments() {
		ComputerSystemFactory factory = new ComputerSystemFactory();

		CommandLineArguments none = CommandLineArguments.parse(new String[0], factory);
		Assert.boolEquals(none.computerSystemTypeID == null, true);
		Assert.boolEquals(none.file == null, true);

		CommandLineArguments systemOnly = CommandLineArguments.parse(new String[] { "/C64" }, factory);
		Assert.stringEquals(systemOnly.computerSystemTypeID, "C64");
		Assert.boolEquals(systemOnly.file == null, true);

		// Case-insensitive, and "-" works like "/".
		CommandLineArguments both = CommandLineArguments.parse(new String[] { "-atari5200", "game.rom" }, factory);
		Assert.stringEquals(both.computerSystemTypeID, "ATARI5200");
		Assert.stringEquals(both.file.getPath(), new File("game.rom").getAbsolutePath());

		CommandLineArguments fileOnly = CommandLineArguments.parse(new String[] { "game.xex" }, factory);
		Assert.boolEquals(fileOnly.computerSystemTypeID == null, true);
		Assert.stringEquals(fileOnly.file.getPath(), new File("game.xex").getAbsolutePath());

		// A Unix absolute path starts with "/" too, but names no computer system: it is the file.
		CommandLineArguments unixPath = CommandLineArguments.parse(new String[] { "/home/me/game.xex" }, factory);
		Assert.boolEquals(unixPath.computerSystemTypeID == null, true);
		Assert.stringEquals(unixPath.file.getPath(), new File("/home/me/game.xex").getAbsolutePath());

		// An unquoted path with blanks arrives in pieces and is put back together.
		CommandLineArguments blanks = CommandLineArguments.parse(new String[] { "/ORIC", "my", "game.tap" }, factory);
		Assert.stringEquals(blanks.computerSystemTypeID, "ORIC");
		Assert.stringEquals(blanks.file.getPath(), new File("my game.tap").getAbsolutePath());

		Assert.log("CommandLineArgumentsTest completed");
	}
}
