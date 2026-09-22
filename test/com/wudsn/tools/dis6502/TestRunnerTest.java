/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502;

import junit.framework.TestCase;

/**
 * The one JUnit test of this project: a bridge that makes Maven's Surefire
 * run {@link TestRunner} on every build, so {@code mvn test} and {@code mvn
 * package} fail when a test does. The tests themselves stay framework-free
 * plain methods (see {@code TestRunner}'s javadoc for why); JUnit 3 is the
 * only test framework whose Surefire provider the offline repository has.
 *
 * @author Peter Dell
 */
public final class TestRunnerTest extends TestCase {

	public void testAll() {
		int failed = TestRunner.run();
		assertEquals("Failed tests - see the log above for which.", 0, failed);
	}
}
