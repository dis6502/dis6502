/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

/**
 * Saves a {@link DisassemblyResult} as one or more source listing files,
 * per {@link Profile}'s include-file settings: no includes at all, all
 * equates in one include file, all include files inlined into the main
 * file's directives, or one include file per chunk of code.
 * <p>
 * Ported from DisassemblyResultFile.h / DisassemblyResultFile.cpp. The
 * C++ version logs each opened file through the global {@code Application}
 * object in {@code OpenWriter}; that is not ported yet (see {@link
 * DisassemblyProgressMonitor}'s similarly deferred logging hooks), so
 * {@link #openWriter} is kept as a real method - matching the C++ call
 * sites that go through it rather than {@code DisassemblyResultWriter}
 * directly - as the natural place to add it later.
 *
 * @author Peter Dell
 */
public final class DisassemblyResultFile {

	/** {@code fileNumber} &gt;= 1 to have the number appended, 0 for a single include file. */
	static String createIncludeFilePath(String mainFilePath, int fileNumber) {
		if (fileNumber > 999) {
			throw new IllegalArgumentException("File number exceeds 3 digits.");
		}

		String result = mainFilePath;
		int index = result.lastIndexOf('.');
		if (index >= 0) {
			result = result.substring(0, index);
		}

		if (fileNumber > 0) {
			result += String.format(".%03d", fileNumber);
		} else {
			result += ".inc";
		}

		return result;
	}

	static File createIncludeFile(File mainFile, int fileNumber) {
		return new File(createIncludeFilePath(mainFile.getPath(), fileNumber));
	}

	static File createIncludeFile(File mainFile) {
		return createIncludeFile(mainFile, 0);
	}

	private DisassemblyResult result;
	private Profile profile;

	private void openWriter(DisassemblyResultWriter writer, File file) throws IOException {
		writer.openFile(file);
	}

	private void insertDisLine(DisassemblyResultWriter writer, DisassemblyLine disLine) throws IOException {
		if (disLine.getSection().getType() != DisassemblySectionType.SYSTEM_EQUATES || disLine.systemAddress == 0
				|| disLine.referenced || !profile.omitUnreferencedSystemLabels) {
			writer.printLine(disLine.getLine(), false);
		}
	}

	private void saveListingWithoutIncludes(DisassemblyResultWriter mainWriter, String atariFileName)
			throws IOException {
		for (Iterator<DisassemblyLine> i = result.createLineIterator(); i.hasNext();) {
			insertDisLine(mainWriter, i.next());
		}
		mainWriter.insertDirectiveEnd(atariFileName);
	}

	private void saveListingWithOneInclude(DisassemblyResultWriter mainWriter, String atariFileName)
			throws IOException {
		DisassemblyResultWriter includeWriter = new DisassemblyResultWriter(profile);
		openWriter(includeWriter, createIncludeFile(mainWriter.getFile()));
		for (Iterator<DisassemblyLine> i = result.createLineIterator(); i.hasNext();) {
			DisassemblyLine disLine = i.next();
			if (disLine.getSection().getType() != DisassemblySectionType.CODE_LINES) {
				insertDisLine(includeWriter, disLine);
			}
		}
		includeWriter.close();

		mainWriter.insertComment();
		mainWriter.insertIncludeFileStatement(createIncludeFilePath(atariFileName, 0));

		for (Iterator<DisassemblyLine> i = result.createLineIterator(DisassemblySectionType.CODE_LINES); i
				.hasNext();) {
			mainWriter.printLine(i.next().getLine(), false);
		}
		mainWriter.insertDirectiveEnd(atariFileName);
	}

	private void saveListingWithIncludesInMainFile(DisassemblyResultWriter mainWriter, String atariFileName,
			long maximumNumberOfLinesPerFile) throws IOException {
		DisassemblyResultWriter includeWriter = new DisassemblyResultWriter(profile);

		int fileNumber = 1;
		includeWriter.openFile(createIncludeFile(mainWriter.getFile(), fileNumber));

		for (Iterator<DisassemblyLine> i = result.createLineIterator(); i.hasNext();) {
			DisassemblyLine disLine = i.next();
			insertDisLine(includeWriter, disLine);

			if (includeWriter.getLineNumber() == maximumNumberOfLinesPerFile && i.hasNext()) {
				fileNumber++;
				includeWriter.close();
				includeWriter.openFile(createIncludeFile(mainWriter.getFile(), fileNumber));
			}
		}
		includeWriter.close();

		mainWriter.insertComment();
		mainWriter.insertComment(atariFileName);
		mainWriter.insertComment();
		for (int i = 1; i <= fileNumber; i++) {
			mainWriter.insertIncludeFileStatement(createIncludeFilePath(atariFileName, i));
		}

		mainWriter.insertDirectiveEnd(atariFileName);
	}

	private void saveListingWithIncludeInEachFile(DisassemblyResultWriter mainWriter, String atariFileName,
			long maximumNumberOfLinesPerFile) throws IOException {
		DisassemblyResultWriter includeWriter = new DisassemblyResultWriter(profile);
		int fileNumber = 0;

		DisassemblyResultWriter writer = mainWriter;

		for (Iterator<DisassemblyLine> i = result.createLineIterator(); i.hasNext();) {
			DisassemblyLine disLine = i.next();
			insertDisLine(writer, disLine);

			if (writer.getLineNumber() == maximumNumberOfLinesPerFile && i.hasNext()) {
				fileNumber++;

				writer.insertComment();
				writer.insertIncludeFileStatement(createIncludeFilePath(atariFileName, fileNumber));

				writer = includeWriter;
				writer.openFile(createIncludeFile(mainWriter.getFile(), fileNumber));
			}
		}

		writer.insertDirectiveEnd(atariFileName);
		includeWriter.close();
	}

	private void saveListing(DisassemblyResultWriter mainWriter, String atariFileName) throws IOException {
		if (!profile.directiveINCLUDEAllowed) {
			saveListingWithoutIncludes(mainWriter, atariFileName);
		} else if (profile.directiveINCLUDEAllEquatesInOneIncludeFile) {
			saveListingWithOneInclude(mainWriter, atariFileName);
		} else {
			// A limit of zero means no limit.
			long maximumNumberOfLinesPerFile = profile.directiveINCLUDEMaximumNumberOfLinesPerFile;
			if (maximumNumberOfLinesPerFile == 0) {
				maximumNumberOfLinesPerFile = Long.MAX_VALUE;
			}
			if (profile.directiveINCLUDEAllIncludesInMainFile) {
				saveListingWithIncludesInMainFile(mainWriter, atariFileName, maximumNumberOfLinesPerFile);
			} else {
				saveListingWithIncludeInEachFile(mainWriter, atariFileName, maximumNumberOfLinesPerFile);
			}
		}
	}

	/**
	 * Saves {@code result} as {@code mainFile}, split into include files as
	 * {@code profile} dictates. TODO: return the include files written.
	 */
	public void saveListing(DisassemblyResult result, Profile profile, File mainFile) throws IOException {
		this.result = result;
		this.profile = profile;

		DisassemblyResultWriter mainWriter = new DisassemblyResultWriter(profile);
		openWriter(mainWriter, mainFile);
		// Unlike the C++ source's catch-close-rethrow, a try/finally closes mainWriter
		// on every path (success or exception) without needing to catch and rethrow.
		try {
			String atariFileName = mainFile.getName();
			saveListing(mainWriter, atariFileName);
		} finally {
			mainWriter.close();
		}
	}
}
