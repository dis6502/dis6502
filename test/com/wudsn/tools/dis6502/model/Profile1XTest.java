/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import com.wudsn.tools.dis6502.Application;

/**
 * Loads the real {@code DIS6502PRF17} fixture ({@code
 * test-resources/profiles/PRF17/mads.prf}) and checks every field {@link
 * Profile1X}'s byte-offset analysis covered - see that class's javadoc.
 *
 * @author Peter Dell
 */
public final class Profile1XTest {

	private static final String FIXTURE_PATH = "test-resources/profiles/PRF17/mads.prf";

	private Profile1XTest() {
	}

	public static void testProfile1X() throws IOException, BackingStoreException {
		// Start from a clean slate: ApplicationSettingsSection is backed by real, persistent
		// java.util.prefs.Preferences, not an in-memory mock.
		Preferences.userNodeForPackage(Application.class).node("DefaultConfig").removeNode();

		Application application = new Application();

		byte[] buffer = Files.readAllBytes(new File(FIXTURE_PATH).toPath());
		Assert.boolEquals(buffer.length == 0, false);

		Profile profile = new Profile();
		Assert.boolEquals(Profile1X.load(profile, buffer, application), true);

		// Cross-checked against the real fixture's raw bytes; see Profile1X.java's javadoc for
		// how these offsets were verified.
		Assert.boolEquals(profile.alignInstructions, true);
		Assert.boolEquals(profile.useLineNumbers, false);
		Assert.boolEquals(profile.useHexNotation, true);
		Assert.boolEquals(profile.showAInAccumulatorMode, false);
		Assert.boolEquals(profile.showBRKAsByte0, true);
		Assert.boolEquals(profile.showColonAfterLabel, false);
		Assert.boolEquals(profile.showLowerCaseInstructions, true);
		Assert.boolEquals(profile.showZPAbsoluteAsByte, false);
		Assert.boolEquals(profile.directiveDSAllowed, true);

		Assert.stringEquals(profile.commentPrefix, ";");
		Assert.stringEquals(profile.hexNotationPrefix, "$");
		Assert.stringEquals(profile.directiveBYTE, ".byte");
		Assert.stringEquals(profile.directiveDS, ".ds");
		Assert.stringEquals(profile.directiveWORD, ".word");
		Assert.stringEquals(profile.directiveSBYTE, "sbyte");
		Assert.stringEquals(profile.directiveORG, "org");
		Assert.stringEquals(profile.directiveBYTESeparator, ",");
		Assert.stringEquals(profile.directiveLOWHead, "<");
		Assert.stringEquals(profile.directiveHIGHHead, ">");
		Assert.stringEquals(profile.directiveINCLUDEHead, ".include \"");

		Assert.longEquals(profile.directiveINCLUDEMaximumNumberOfLinesPerFile, 500);

		// These fall back to their "DefaultConfig" settings-section defaults, since the node was
		// just cleared above. directiveBYTENumberOfBytesPerLine in particular is a field with a
		// documented quirk - see Profile1X.java's javadoc.
		Assert.longEquals(profile.directiveBYTENumberOfBytesPerLine, 16);
		Assert.longEquals(profile.directiveBYTENumberOfCharactersPerString, 40);
		Assert.longEquals(profile.directiveWORDNumberOfWordsPerLine, 8);
		Assert.boolEquals(profile.showOpcodeAsComment, false);

		// A buffer with no valid magic must fail gracefully (return false) instead of throwing;
		// see Profile1X.java's javadoc.
		byte[] garbage = new byte[64];
		for (int i = 0; i < garbage.length; i++) {
			garbage[i] = (byte) ('A' + (i % 26));
		}
		Profile garbageProfile = new Profile();
		Assert.boolEquals(Profile1X.load(garbageProfile, garbage, application), false);
	}
}
