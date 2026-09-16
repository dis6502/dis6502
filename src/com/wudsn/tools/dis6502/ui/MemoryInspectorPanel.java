/*
 * Copyright (C) 2025 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.ui;

import java.awt.BorderLayout;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * Placeholder for the memory inspector (hex dump editor) view.
 * <p>
 * Not ported yet: ui/MemoryInspectorWindow.h/.cpp, ui/MemoryInspectorControl
 * (Impl).h/.cpp (the custom-painted hex/ASCII dump grid with inline byte
 * editing), and {@link com.wudsn.tools.dis6502.model.MemoryInspectorSelection}/
 * {@link com.wudsn.tools.dis6502.model.MemoryInspectorStack}'s UI wiring -
 * this only reserves the panel's place in the main window layout.
 *
 * @author Peter Dell
 */
public final class MemoryInspectorPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	public MemoryInspectorPanel() {
		super(new BorderLayout());
		setBorder(BorderFactory.createTitledBorder("Memory Inspector"));
		add(new JLabel("Not implemented yet.", SwingConstants.CENTER), BorderLayout.CENTER);
	}
}
