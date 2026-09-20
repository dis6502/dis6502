/*
 * Copyright (C) 2025 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

/**
 * A semantic cursor movement the Memory Inspector's edit mode accepts - the
 * model-layer counterpart of the arrow/Home/End keys {@code
 * MemoryInspectorControlImpl::KeyDown}'s edit-mode branch handles. {@code
 * com.wudsn.tools.dis6502.ui.MemoryInspectorPanel} maps a raw {@link
 * java.awt.event.KeyEvent} to one of these before calling {@link
 * Workspace#moveMemoryInspectorEditCursor}, keeping the actual navigation
 * math in the model layer, free of any Swing dependency.
 *
 * @author Peter Dell
 */
public enum MemoryInspectorEditCursorMovement {

	HOME,
	END,
	UP,
	DOWN,
	LEFT,
	RIGHT
}
