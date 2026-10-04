/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model.system;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import com.wudsn.tools.dis6502.model.Assert;

/**
 * Guards the isolation of system-specific code (see {@code
 * plans/RULES_SYSTEM_SUBPACKAGES_PLAN.md}): no source outside the Atari
 * system packages ({@code model.system.atari}, {@code atari800}, {@code
 * atari5200}) refers to WUDSN Base's Atari package {@code
 * com.wudsn.tools.base.atari}. The rest of dis6502 works with system-independent
 * types such as {@link ROMType} instead. Scans {@code src/} and {@code test/}
 * relative to the working directory, the repository root.
 *
 * @author Peter Dell
 */
public final class SystemIsolationTest {

	private static final String ATARI_PACKAGE = "com.wudsn.tools.base.atari";

	private SystemIsolationTest() {
	}

	public static void testSystemIsolation() throws IOException {
		List<String> violations = new ArrayList<>();
		int scanned = 0;
		for (String root : new String[] { "src", "test" }) {
			try (Stream<Path> paths = Files.walk(Paths.get(root))) {
				for (Path path : (Iterable<Path>) paths.filter(p -> p.toString().endsWith(".java"))::iterator) {
					scanned++;
					String unixPath = path.toString().replace('\\', '/');
					if (unixPath.contains("/model/system/atari")) {
						continue; // atari, atari800, atari5200.
					}
					String source = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
					if (source.contains(ATARI_PACKAGE + ".") || source.contains(ATARI_PACKAGE + ";")) {
						violations.add(unixPath);
					}
				}
			}
		}
		Assert.boolEquals(scanned > 100, true); // The scan really ran over the sources.
		Assert.stringEquals(String.join(", ", violations), "");
	}
}
