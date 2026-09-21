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

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.wudsn.tools.base.Actions;
import com.wudsn.tools.base.gui.ElementFactory;
import com.wudsn.tools.dis6502.DataTypes;
import com.wudsn.tools.dis6502.Texts;
import com.wudsn.tools.dis6502.model.MemoryType;

/**
 * A dialog asking for the missing half of an "assumed word" - shown when
 * the user marks one byte of an immediate operand as {@link
 * MemoryType#LOBYTE}/{@link MemoryType#HIBYTE}, to supply the other,
 * unknown half's value.
 * <p>
 * Ported from ui/LowHighByteDialog.h / LowHighByteDialog.cpp, folded into
 * one blocking {@link #show} call as is idiomatic for a Swing modal
 * {@link JDialog}.
 *
 * @author Peter Dell
 */
public final class LowHighByteDialog extends JDialog {

	private static final long serialVersionUID = 1L;

	private final JLabel knownByteLabel = new JLabel();
	private final JTextField knownByteField = new JTextField(4);
	private final JLabel unknownByteLabel = new JLabel();
	private final JTextField unknownByteField = new JTextField(4);
	private final JButton okButton = ElementFactory.createButton(Actions.ButtonBar_OK, true);

	private int unknownByte;
	private boolean confirmed;

	public LowHighByteDialog(Frame owner) {
		super(owner, true);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setTitle(Texts.LowHighByteDialog_Title);

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
				okButton.setEnabled(!unknownByteField.getText().isEmpty());
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

		okButton.addActionListener(e -> performOK());
		JButton cancelButton = ElementFactory.createButton(Actions.ButtonBar_Cancel, true);
		cancelButton.addActionListener(e -> {
			confirmed = false;
			setVisible(false);
		});
		JPanel buttonPanel = new JPanel();
		buttonPanel.add(okButton);
		buttonPanel.add(cancelButton);

		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(formPanel, BorderLayout.CENTER);
		getContentPane().add(buttonPanel, BorderLayout.SOUTH);
		pack();
		setLocationRelativeTo(owner);
	}

	/** Ported from LowHighByteDialog::OnOK. */
	private void performOK() {
		try {
			unknownByte = Integer.parseInt(unknownByteField.getText().trim(), 16) & 0xFF;
		} catch (NumberFormatException ex) {
			return; // The OK button is disabled while the field is empty; an invalid value just leaves the dialog open.
		}
		confirmed = true;
		setVisible(false);
	}

	public int getUnknownByte() {
		return unknownByte;
	}

	/** Ported from LowHighByteDialog::Show/InitDialog. */
	public boolean show(MemoryType memoryType, int knownByte) {
		boolean lowIsKnown = memoryType == MemoryType.LOBYTE;
		ElementUtilities.applyLabel(knownByteLabel, lowIsKnown ? DataTypes.LowHighByteDialog_LowByte : DataTypes.LowHighByteDialog_HighByte,
				knownByteField);
		ElementUtilities.applyLabel(unknownByteLabel, lowIsKnown ? DataTypes.LowHighByteDialog_HighByte : DataTypes.LowHighByteDialog_LowByte,
				unknownByteField);
		knownByteField.setText(String.format("%02X", knownByte));
		unknownByteField.setText("");
		okButton.setEnabled(false);
		pack();

		confirmed = false;
		setVisible(true); // Blocks until disposed/hidden - this is a modal dialog.

		return confirmed;
	}
}
