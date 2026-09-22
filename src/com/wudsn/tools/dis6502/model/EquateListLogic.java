/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

import com.wudsn.tools.dis6502.Application;
import com.wudsn.tools.dis6502.Messages;

/**
 * Loads and saves the modern text equates file format for an
 * {@link EquateList}.
 * <p>
 * This is not implemented on {@link EquateList} itself - see its javadoc -
 * since it needs an {@link Application} for logging and does the file I/O
 * directly.
 * <p>
 * {@link #save}'s non-XASM branch writes each equate's {@code toString()}
 * followed by a trailing {@code "\n"}, one per line: {@link #load} splits
 * strictly on newlines, so a file without that separator would fail to
 * parse anything past its first equate on reload.
 *
 * @author Peter Dell
 */
public final class EquateListLogic {

	private final Application application;

	public EquateListLogic(Application application) {
		this.application = application;
	}

	/** Loads equates from a text equates file, appending to {@code equateList}. Returns {@code false}, and logs, instead of throwing. */
	public boolean load(EquateList equateList, String filePath) {
		try {
			return load(equateList, new FileInputStream(filePath), filePath);
		} catch (IOException ex) {
			equateList.clear();
			application.sendMessage(Messages.I009, filePath);
			application.sendErrorMessage(ex);
			return false;
		}
	}

	/**
	 * Same as {@link #load(EquateList, String)}, from an already-open stream
	 * (closed by this method) - used for the system equates, which are
	 * classpath resources rather than files. {@code displayName} is only
	 * used for logging.
	 */
	public boolean load(EquateList equateList, InputStream inputStream, String displayName) {
		equateList.clear();
		application.sendMessage(Messages.I009, displayName);

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				EquateList.EquateResult result = equateList.addEquate(line);
				if (!result.error.isEmpty()) {
					application.sendMessage(Messages.E004, line, result.error);
				}
			}
		} catch (IOException ex) {
			application.sendErrorMessage(ex);
			return false;
		}

		application.sendMessage(Messages.I008, String.valueOf(equateList.getCount()),
				String.valueOf(equateList.getLabelCount()));
		equateList.notifyListeners();
		return true;
	}

	/** Saves equates to a text equates file, or (if {@code xasm}) an XASM 3.0.0 label table. Logs instead of throwing. */
	public void save(EquateList equateList, String filePath, boolean xasm) {
		application.sendMessage(Messages.I012, filePath);

		try (Writer writer = new OutputStreamWriter(new FileOutputStream(filePath),
				xasm ? StandardCharsets.US_ASCII : StandardCharsets.UTF_8)) {
			if (!xasm) {
				for (Equate equate : equateList.getEquates()) {
					writer.write(equate.toString());
					writer.write("\n");
				}
			} else {
				writer.write("xasm 3.0.0\nLabel table:\n");
				for (Equate equate : equateList.getEquates()) {
					writer.write("        " + String.format("%04X", equate.getLabelValue() & 0xFFFF) + " "
							+ equate.getLabel() + "\n");
				}
			}
		} catch (IOException ex) {
			application.sendErrorMessage(ex);
		}
	}
}
