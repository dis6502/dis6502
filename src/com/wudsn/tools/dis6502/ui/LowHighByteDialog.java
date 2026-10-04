/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.ui;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.wudsn.tools.base.gui.ElementFactory;
import com.wudsn.tools.base.gui.ModalDialog;
import com.wudsn.tools.dis6502.DataTypes;
import com.wudsn.tools.dis6502.Texts;
import com.wudsn.tools.dis6502.model.MemoryType;

/**
 * A dialog asking for the missing half of an "assumed word" - shown when
 * the user marks one byte of an immediate operand as {@link
 * MemoryType#LOBYTE}/{@link MemoryType#HIBYTE}, to supply the other,
 * unknown half's value.
 * <p>
 * Shown by one blocking {@link #show} call - a WUDSN Base {@link
 * ModalDialog}.
 *
 * @author Peter Dell
 */
public final class LowHighByteDialog extends ModalDialog {

	private static final long serialVersionUID = 1L;

	private final JLabel knownByteLabel = new JLabel();
	private final JTextField knownByteField = new JTextField(4);
	private final JLabel unknownByteLabel = new JLabel();
	private final JTextField unknownByteField = new JTextField(4);

	private int unknownByte;

	public LowHighByteDialog(Frame owner) {
		super(owner, Texts.LowHighByteDialog_Title);

		knownByteField.setEditable(false);
		unknownByteField.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) {
				update();
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				update();
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				update();
			}

			private void update() {
				getOKButton().setEnabled(!unknownByteField.getText().isEmpty());
			}
		});

		JPanel formPanel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(4, 4, 4, 4);
		c.anchor = GridBagConstraints.WEST;

		c.gridx = 0;
		c.gridy = 0;
		formPanel.add(knownByteLabel, c);
		c.gridx = 1;
		formPanel.add(knownByteField, c);

		c.gridx = 0;
		c.gridy = 1;
		formPanel.add(unknownByteLabel, c);
		c.gridx = 1;
		formPanel.add(unknownByteField, c);

		getContentPane().add(formPanel, BorderLayout.CENTER);
	}

	/** Parses and commits {@link #unknownByte}. */
	@Override
	protected boolean validateOK() {
		try {
			unknownByte = Integer.parseInt(unknownByteField.getText().trim(), 16) & 0xFF;
		} catch (NumberFormatException ex) {
			return false; // The OK button is disabled while the field is empty; an invalid value just leaves the dialog open.
		}
		return true;
	}

	public int getUnknownByte() {
		return unknownByte;
	}

	/** Opens the dialog labeled for whichever half is known; returns whether the user clicked OK. */
	public boolean show(MemoryType memoryType, int knownByte) {
		boolean lowIsKnown = memoryType == MemoryType.LOBYTE;
		ElementFactory.applyLabel(knownByteLabel, lowIsKnown ? DataTypes.LowHighByteDialog_LowByte : DataTypes.LowHighByteDialog_HighByte,
				knownByteField);
		ElementFactory.applyLabel(unknownByteLabel, lowIsKnown ? DataTypes.LowHighByteDialog_HighByte : DataTypes.LowHighByteDialog_LowByte,
				unknownByteField);
		knownByteField.setText(String.format("%02X", knownByte));
		unknownByteField.setText("");
		getOKButton().setEnabled(false);

		showModal(unknownByteField);
		return okPressed;
	}
}
