/*
 * Copyright (C) 2025 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.ui;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.KeyStroke;

/**
 * Small Swing helpers shared by more than one of this project's own dialogs.
 * Unlike {@code com.wudsn.tools.base.gui.ElementFactory} (whose helpers are
 * shared across every WUDSN Swing tool), nothing here is needed outside
 * dis6502, so this stays local to {@code com.wudsn.tools.dis6502.ui} instead
 * of that shared library.
 *
 * @author Peter Dell
 */
public final class ElementUtilities {

	private ElementUtilities() {
	}

	/**
	 * Wires Esc, for as long as {@code dialog}'s window has focus, to run
	 * {@code closeAction} - the same {@link AbstractAction}-on-the-root-
	 * pane's-input/action-map idiom {@link EquateDialog} and {@link
	 * AboutDialog} each used to duplicate individually. Swing has no
	 * built-in Esc-closes-dialog mapping the way a native Win32 modal
	 * dialog defaults {@code IDCANCEL} for, so every dialog that wants this
	 * needs to wire it explicitly - pass the same {@link Runnable} the
	 * dialog's own Cancel/OK button's {@code ActionListener} already runs,
	 * so Esc behaves exactly like clicking that button.
	 */
	public static void closeOnEscape(JDialog dialog, Runnable closeAction) {
		dialog.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
				"close");
		dialog.getRootPane().getActionMap().put("close", new AbstractAction() {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				closeAction.run();
			}
		});
	}
}
