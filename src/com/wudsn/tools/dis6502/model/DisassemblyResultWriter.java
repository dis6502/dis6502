/*
 * Copyright (C) 2025 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Writes one disassembly listing file: line-numbered, optionally
 * instruction-aligned text lines, plus the comment/include-statement/end-
 * directive helpers used when a listing is split across several files.
 * <p>
 * Ported from DisassemblyResultWriter.h / DisassemblyResultWriter.cpp (and
 * inlines the byte-level encoding logic of {@code OutputStream::WriteString}
 * from OutputStream.cpp). The C++ version is built on custom {@code File}/
 * {@code OutputStream} wrapper classes around a raw {@code FILE*}; this uses
 * {@code java.io.File}/{@code java.io.OutputStream} directly instead, the
 * same simplification already used elsewhere in this port for C++-specific
 * infrastructure classes Java's standard library already covers.
 *
 * @author Peter Dell
 */
public final class DisassemblyResultWriter implements AutoCloseable {

	private final Profile profile;

	private File file;
	private OutputStream outputStream;
	private String newline = "";
	private long lineNumber;

	public DisassemblyResultWriter(Profile profile) {
		this.profile = profile;
	}

	public void openFile(File file) throws IOException {
		this.file = file;
		Encoding encoding = profile.outputEncoding;
		if (encoding == Encoding.UNKNOWN) {
			throw new IOException("Cannot write files if encoding is unknown.");
		}
		File parent = file.getParentFile();
		if (parent != null && !parent.exists()) {
			parent.mkdirs();
		}
		outputStream = new FileOutputStream(file);
		newline = getNewline(encoding);
		lineNumber = 0;
	}

	public File getFile() {
		return file;
	}

	@Override
	public void close() throws IOException {
		if (outputStream != null) {
			outputStream.close();
		}
	}

	public long getLineNumber() {
		return lineNumber;
	}

	public void printLine(String line, boolean useAlignment) throws IOException {
		lineNumber++;

		if (outputStream == null) {
			throw new IllegalStateException("No output stream opened.");
		}

		String lineNumberString = "";
		if (profile.useLineNumbers) {
			lineNumberString = lineNumber + " ";
			writeString(lineNumberString);
		}

		// TODO Alignment disabled, because comments and labels cannot be aligned.
		if (useAlignment && !line.startsWith(profile.commentPrefix)) {
			int instructionAlignmentOffset = profile.alignInstructions ? 8 : 0;
			for (int i = lineNumberString.length(); i < instructionAlignmentOffset; i++) {
				writeString(" ");
			}
		}
		writeString(line);
		writeString(newline);
	}

	public void insertComment() throws IOException {
		insertComment("");
	}

	public void insertComment(String comment) throws IOException {
		if (profile.commentPrefix.isEmpty()) {
			return;
		}
		String line = profile.commentPrefix;
		if (!comment.isEmpty()) {
			line += " " + comment;
		}
		printLine(line, false);
	}

	public void insertIncludeFileStatement(String includeFileName) throws IOException {
		String line = profile.directiveINCLUDEHead + includeFileName + profile.directiveINCLUDETail;
		printLine(line, true);
	}

	public void insertDirectiveEnd(String atariFileName) throws IOException {
		if (profile.directiveENDHead.isEmpty()) {
			return;
		}
		insertComment();

		String line = profile.directiveENDHead;
		if (profile.directiveENDNeedsFilename) {
			line += atariFileName;
		}
		line += profile.directiveENDTail;
		printLine(line, true);
	}

	private void writeString(String value) throws IOException {
		switch (profile.outputEncoding) {
		case UNKNOWN:
			throw new IllegalStateException("Invalid encoding.");

		case BINARY:
			throw new IOException("Cannot write strings if encoding is binary.");

		case ASCII: {
			byte[] buffer = new byte[value.length()];
			for (int i = 0; i < value.length(); i++) {
				char c = value.charAt(i);
				if (c == 10 || c == 13 || (32 <= c && c <= 127)) {
					buffer[i] = (byte) c;
				} else {
					throw new IOException("Character '" + c + "' (" + (int) c + ") at position " + i
							+ " of string '" + value + "' is no ASCII character and cannot be written in ASCII encoding mode.");
				}
			}
			outputStream.write(buffer);
			break;
		}

		case ATASCII: {
			byte[] buffer = new byte[value.length()];
			for (int i = 0; i < value.length(); i++) {
				char c = value.charAt(i);
				if (c <= 2555) {
					buffer[i] = (byte) c;
				} else {
					throw new IOException("Character '" + c + "' (" + (int) c + ") at position " + i
							+ " of string '" + value + "' is no ASCII character and cannot be written in ATASCII encoding mode.");
				}
			}
			outputStream.write(buffer);
			break;
		}

		case UTF8: {
			outputStream.write(value.getBytes(StandardCharsets.UTF_8));
			break;
		}
		}
	}

	private static String getNewline(Encoding encoding) {
		switch (encoding) {
		case ASCII:
		case UTF8:
			return System.lineSeparator();
		case ATASCII:
			return "";
		default:
			return "";
		}
	}
}
