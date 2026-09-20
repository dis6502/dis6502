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
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

import com.wudsn.tools.dis6502.Application;
import com.wudsn.tools.dis6502.Text;

/**
 * Loads and saves the modern text equates file format for an
 * {@link EquateList}.
 * <p>
 * Ported from EquateListLogic.h / EquateListLogic.cpp, which is itself a
 * thin wrapper delegating to {@code EquateList::Load}/{@code
 * EquateList::Save}. Those methods were not ported onto {@link EquateList}
 * itself - see its javadoc, they need application-level logging/file I/O -
 * so this class implements the file I/O directly, taking the
 * {@link Application} the C++ version reached through the {@code
 * g_Application} global.
 * <p>
 * Found and fixed a bug while porting {@link #save}: the C++ version's
 * non-XASM branch wrote each equate's {@code ToString()} one after another
 * with no separator, so every equate ended up concatenated onto a single
 * line instead of one per line - reloading such a file would then fail to
 * parse anything past the first equate, since {@link #load} (like the
 * C++ version's line reader) splits strictly on newlines. Fixed upstream
 * (see that commit) and correct here from the start: each line is written
 * with a trailing {@code "\n"}.
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
		equateList.clear();
		application.sendInfoMessage(Text.IDS_LOG_OPEN_EQUATE_FILE, filePath);

		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(new FileInputStream(filePath), StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				equateList.addEquate(line);
			}
		} catch (IOException ex) {
			application.sendErrorMessage(ex);
			return false;
		}

		application.sendInfoMessage(Text.IDS_EQUATE_LIST_LOADED, String.valueOf(equateList.getCount()),
				String.valueOf(equateList.getLabelCount()));
		equateList.notifyListeners();
		return true;
	}

	/** Saves equates to a text equates file, or (if {@code xasm}) an XASM 3.0.0 label table. Logs instead of throwing. */
	public void save(EquateList equateList, String filePath, boolean xasm) {
		application.sendInfoMessage(Text.IDS_LOG_SAVE_EQUATE_FILE, filePath);

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
