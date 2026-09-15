/*
 * Copyright (C) 2025 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502;

import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.WindowConstants;

import com.wudsn.tools.base.common.Application;

/**
 * Application entry point. Follows the same bootstrap pattern as
 * com.wudsn.tools.thecartstudio.TheCartStudio: create the {@link Application}
 * singleton, then build the UI on the Swing event dispatch thread.
 * <p>
 * This currently only opens a placeholder main window; the actual
 * dis6502 UI (menus, disassembly view, dialogs) is ported incrementally.
 *
 * @author Peter Dell
 */
public final class Dis6502 {

	private static Dis6502 instance;

	private JFrame mainWindowFrame;

	public static void main(final String[] args) {

		// Use the event dispatch thread for Swing components.
		EventQueue.invokeLater(new Runnable() {

			@Override
			public void run() {

				Application.createInstance("https://www.wudsn.com/tools/dis6502/dis6502.zip", "dis6502.jar",
						Dis6502.class);
				instance = new Dis6502();
				instance.run(args);
			}
		});
	}

	Dis6502() {
	}

	void run(String[] args) {
		if (args == null) {
			throw new IllegalArgumentException("Parameter 'args' must not be null.");
		}

		mainWindowFrame = new JFrame("dis6502");
		mainWindowFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		mainWindowFrame.setSize(800, 600);
		mainWindowFrame.setLocationRelativeTo(null);
		mainWindowFrame.setVisible(true);
	}
}
