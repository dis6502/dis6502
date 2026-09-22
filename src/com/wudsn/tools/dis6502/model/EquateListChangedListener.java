/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

/**
 * Callback for a change to an {@link EquateList}.
 *
 * @author Peter Dell
 */
public interface EquateListChangedListener {
	void handleEquateListChanged(EquateList equateList, WorkspaceProperty workspaceProperty);
}
