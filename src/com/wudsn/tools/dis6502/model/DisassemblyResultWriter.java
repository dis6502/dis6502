/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import com.wudsn.tools.dis6502.Messages;

/**
 * Writes one disassembly listing file: line-numbered, optionally
 * instruction-aligned text lines, plus the comment/include-statement/end-
 * directive helpers used when a listing is split across several files.
 * <p>
 * Uses {@code java.io.File}/{@code java.io.OutputStream} directly, since
 * Java's standard library already covers everything a custom file/stream
 * wrapper would otherwise be needed for.
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
			throw new IOException(Messages.E061.format());
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
		// An if chain, not a switch: Encoding is a ValueSet, not an enum.
		Encoding encoding = profile.outputEncoding;
		if (encoding == Encoding.BINARY) {
			throw new IOException(Messages.E062.format());

		} else if (encoding == Encoding.ASCII) {
			byte[] buffer = new byte[value.length()];
			for (int i = 0; i < value.length(); i++) {
				char c = value.charAt(i);
				if (c == 10 || c == 13 || (32 <= c && c <= 127)) {
					buffer[i] = (byte) c;
				} else {
					throw new IOException(Messages.E063.format(String.valueOf(c), String.valueOf((int) c), String.valueOf(i), value,
							"ASCII"));
				}
			}
			outputStream.write(buffer);

		} else if (encoding == Encoding.ATASCII) {
			byte[] buffer = new byte[value.length()];
			for (int i = 0; i < value.length(); i++) {
				char c = value.charAt(i);
				// Anything above 255 does not fit into the one byte an ATASCII character is.
				if (c <= 255) {
					buffer[i] = (byte) c;
				} else {
					throw new IOException(Messages.E063.format(String.valueOf(c), String.valueOf((int) c), String.valueOf(i), value,
							"ATASCII"));
				}
			}
			outputStream.write(buffer);

		} else if (encoding == Encoding.UTF8) {
			outputStream.write(value.getBytes(StandardCharsets.UTF_8));

		} else {
			throw new IllegalStateException("Invalid encoding.");
		}
	}

	private static String getNewline(Encoding encoding) {
		if (encoding == Encoding.ASCII || encoding == Encoding.UTF8) {
			return System.lineSeparator();
		}
		if (encoding == Encoding.ATASCII) {
			return ""; // The ATASCII end of line character.
		}
		return "";
	}
}
