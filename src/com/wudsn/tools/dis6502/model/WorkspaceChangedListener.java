/*
 * Copyright (C) 2025 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.model;

import java.util.List;

/**
 * Ported from WorkspaceChangedListener.h.
 *
 * @author Peter Dell
 */
public interface WorkspaceChangedListener {
	void handleWorkspaceChanged(Workspace workspace, List<WorkspaceProperty> propertyChangeEvents);
}
