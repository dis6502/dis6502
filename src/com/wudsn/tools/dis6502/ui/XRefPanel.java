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
 * Placeholder for the cross-reference (label usage) list view.
 * <p>
 * Not ported yet: ui/XRefListWindow.h/.cpp and ui/MainXRef.h/.cpp - this
 * only reserves the panel's place in the main window layout.
 *
 * @author Peter Dell
 */
public final class XRefPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	public XRefPanel() {
		super(new BorderLayout());
		setBorder(BorderFactory.createTitledBorder("Cross Reference"));
		add(new JLabel("Not implemented yet.", SwingConstants.CENTER), BorderLayout.CENTER);
	}
}
