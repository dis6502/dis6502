/*
 * Copyright (C) 2025 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

/**
 * Which part of an edited byte's cell the Memory Inspector's edit-mode
 * cursor is on - matches the C++ source's {@code wEditedPart} (0/1/2). Used
 * by {@link Workspace}'s edit-mode state/methods and by {@link
 * com.wudsn.tools.dis6502.ui.MemoryInspectorGridPanel}'s cursor painting.
 *
 * @author Peter Dell
 */
public enum MemoryInspectorEditPane {

	HEX_HIGH,
	HEX_LOW,
	ASCII
}
