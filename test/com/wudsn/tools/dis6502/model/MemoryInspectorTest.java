/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

/**
 * Ported from MemoryInspectorTest.h / MemoryInspectorTest.cpp. The C++
 * version's {@code TestMemoryInspectorType} verifies that {@link
 * MemoryType}'s ordinals (used as raw bytes in a memory inspector's type
 * buffer) still match the binary encoding used since application version
 * 2.2 - {@link MemoryType} already performs that exact check at class-load
 * time ({@link MemoryType#ORDINALS_MATCH_VERSION_22}, throwing an {@link
 * ExceptionInInitializerError} otherwise), so this just asserts that flag
 * is {@code true}, giving the check its own visible entry in the test
 * runner's output the way the C++ suite does.
 *
 * @author Peter Dell
 */
public final class MemoryInspectorTest {

	private MemoryInspectorTest() {
	}

	public static void testMemoryInspectorType() {
		Assert.boolEquals(MemoryType.ORDINALS_MATCH_VERSION_22, true);
	}
}
