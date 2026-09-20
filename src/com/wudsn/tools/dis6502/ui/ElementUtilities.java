/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.ui;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.KeyStroke;

import com.wudsn.tools.base.repository.DataType;

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

	/**
	 * Applies a {@link DataType}'s label text and mnemonic directly to a
	 * self-labeled button ({@code JCheckBox}/{@code JRadioButton}), mirroring
	 * {@code com.wudsn.tools.base.gui.ElementFactory#createLabel(DataType,
	 * JComponent)}'s '&amp;'-mnemonic handling but without a separate {@code
	 * JLabel}, since these buttons carry their own text instead of being
	 * paired with one.
	 */
	public static void applyLabel(AbstractButton button, DataType dataType) {
		String text = dataType.getLabel();
		int index = text.indexOf('&');
		if (index == -1) {
			throw new RuntimeException("No '&' contained in label text '" + text + "'.");
		}
		char c = text.charAt(index + 1);
		c = Character.toUpperCase(c);
		if (c < KeyEvent.VK_A || c > KeyEvent.VK_Z) {
			throw new RuntimeException(
					"Mnemonic character '" + c + "' contained in label text '" + text + "' is not between 'A' and 'Z'.");
		}
		button.setText(dataType.getLabelWithoutMnemonics());
		button.setMnemonic(c);
		button.setDisplayedMnemonicIndex(index);

		String toolTip = dataType.getToolTip();
		if (toolTip != null && !toolTip.isEmpty()) {
			button.setToolTipText(toolTip);
		}
	}
}
