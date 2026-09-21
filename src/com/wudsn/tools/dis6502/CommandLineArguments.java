/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502;

import java.io.File;

import com.wudsn.tools.dis6502.model.ComputerSystemType;

/**
 * The parsed command line: {@code [/SYSTEMID] [file]}, e.g. {@code /C64
 * game.prg} - an optional computer system to start with, and an optional
 * file to open, which is what makes "Open with", a file type association,
 * or a per-system desktop shortcut work.
 * <p>
 * Same syntax as the C++ version's {@code Main::InitApplication}, with two
 * differences. A leading {@code -} is accepted in place of {@code /}, and
 * the first argument only counts as a system ID if it actually names one
 * (case-insensitively) - elsewhere than on Windows an absolute file path
 * starts with {@code /} too, and {@code /home/me/game.xex} must stay a
 * file. And since Java already delivers the arguments split, a file path
 * needs no re-joining - though an unquoted path containing blanks, which
 * arrives as several arguments, is still put back together, as the C++
 * version does. The C++ version's {@code /TEST:} and {@code /DEBUG}
 * options have no counterpart - see {@code TestRunner}.
 *
 * @author Peter Dell
 */
public final class CommandLineArguments {

	/** The ID of the computer system to start with (e.g. {@code "C64"}), or {@code null} if none was given. */
	public final String computerSystemTypeID;

	/** The file to open, as an absolute file, or {@code null} if none was given. */
	public final File file;

	private CommandLineArguments(String computerSystemTypeID, File file) {
		this.computerSystemTypeID = computerSystemTypeID;
		this.file = file;
	}

	public static CommandLineArguments parse(String[] args) {
		if (args == null) {
			throw new IllegalArgumentException("Parameter 'args' must not be null.");
		}

		String computerSystemTypeID = null;
		int index = 0;
		if (args.length > 0 && (args[0].startsWith("/") || args[0].startsWith("-"))) {
			String id = args[0].substring(1).toUpperCase();
			if (ComputerSystemType.fromId(id) != ComputerSystemType.UNKNOWN) {
				computerSystemTypeID = id;
				index = 1;
			}
		}

		StringBuilder filePath = new StringBuilder();
		for (; index < args.length; index++) {
			if (filePath.length() > 0) {
				filePath.append(' ');
			}
			filePath.append(args[index]);
		}
		File file = filePath.length() == 0 ? null : new File(filePath.toString()).getAbsoluteFile();

		return new CommandLineArguments(computerSystemTypeID, file);
	}
}
