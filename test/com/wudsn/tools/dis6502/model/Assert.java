/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

import com.wudsn.tools.base.common.Log;

/**
 * A minimal, dependency-free assertion helper for the hand-rolled test
 * harness.
 * <p>
 * Java's {@code long} covers every integer comparison the tests need, and
 * {@code java.lang.AssertionError} is thrown directly rather than a
 * dedicated class.
 *
 * @author Peter Dell
 */
public final class Assert {

	private Assert() {
	}

	public static void boolEquals(boolean actual, boolean expected) {
		if (actual != expected) {
			logValue("Actual Value  ", Boolean.toString(actual));
			logValue("Expected Value", Boolean.toString(expected));
			fail("Bool values not equal");
		}
	}

	public static void longEquals(long actual, long expected) {
		if (actual != expected) {
			logValue("Actual Value  ", longToString(actual));
			logValue("Expected Value", longToString(expected));
			fail("Long values not equal");
		}
	}

	private static String longToString(long value) {
		return value + " (0x" + Long.toHexString(value) + ")";
	}

	public static void stringEquals(String actual, String expected) {
		if (!actual.equals(expected)) {
			logValue("Actual Value  ", actual);
			logValue("Expected Value", expected);
			fail("String values not equal");
		}
	}

	public static void notNull(Object actual) {
		if (actual == null) {
			logValue("Actual Value  ", "null");
			logValue("Expected Value", "not null");
			fail("Value is null");
		}
	}

	public static void isNull(Object actual) {
		if (actual != null) {
			logValue("Actual Value  ", String.valueOf(actual));
			logValue("Expected Value", "null");
			fail("Value is not null");
		}
	}

	public static void log(String text) {
		Log.logInfo("{0}", new Object[] { text });
	}

	public static void logValue(String label, String value) {
		Log.logInfo("{0}: {1}", new Object[] { label, value });
	}

	public static void log(Throwable exception) {
		log(String.valueOf(exception.getMessage()));
	}

	public static void fail(String text) {
		log(text);
		throw new AssertionError(text);
	}

	public static void fail(Throwable exception) {
		fail(String.valueOf(exception.getMessage()));
	}
}
