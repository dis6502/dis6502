/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.wudsn.tools.base.Actions;
import com.wudsn.tools.base.gui.ElementFactory;
import com.wudsn.tools.dis6502.DataTypes;
import com.wudsn.tools.dis6502.model.Equate;
import com.wudsn.tools.dis6502.model.EquateList;

/**
 * A dialog for defining a range of user equates ("LABEL+1", "LABEL+2", ...)
 * relative to a chosen base equate.
 * <p>
 * Ported from ui/EquateRangeDialog.h / EquateRangeDialog.cpp. The base
 * equate can be chosen from either the system or user equate list (both
 * are offered in {@link #baseEquateComboBox}, matching {@code
 * CreateControls}'s two {@code FillCombobox} calls), but {@link
 * EquateList#setRange} is always applied to the user equate list passed to
 * {@link #show}, matching {@code EquateRangeDialog::OnOK} and {@code
 * EquateListController::DefineUserAddressRange}.
 *
 * @author Peter Dell
 */
public final class EquateRangeDialog extends JDialog {

	private static final long serialVersionUID = 1L;

	private final JTextField startAddressField = new JTextField(6);
	private final JTextField endAddressField = new JTextField(6);
	private final JComboBox<Equate> baseEquateComboBox = new JComboBox<>();

	private EquateList userEquateList;
	private boolean confirmed;

	public EquateRangeDialog(Frame owner) {
		super(owner, true);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setTitle("Define Address Range");

		baseEquateComboBox.setRenderer(new DefaultListCellRenderer() {
			private static final long serialVersionUID = 1L;

			@Override
			public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
					boolean cellHasFocus) {
				String text = "";
				if (value instanceof Equate) {
					Equate equate = (Equate) value;
					text = equate.getLabel() + " ($" + String.format("%04X", equate.getLabelValue() & 0xFFFF) + ")";
				}
				return super.getListCellRendererComponent(list, text, index, isSelected, cellHasFocus);
			}
		});

		JPanel formPanel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(4, 4, 4, 4);
		c.anchor = GridBagConstraints.WEST;

		c.gridx = 0;
		c.gridy = 0;
		formPanel.add(ElementFactory.createLabel(DataTypes.EquateRangeDialog_StartAddress, startAddressField), c);
		c.gridx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		formPanel.add(startAddressField, c);

		c.gridx = 0;
		c.gridy = 1;
		c.fill = GridBagConstraints.NONE;
		c.weightx = 0;
		formPanel.add(ElementFactory.createLabel(DataTypes.EquateRangeDialog_EndAddress, endAddressField), c);
		c.gridx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		formPanel.add(endAddressField, c);

		c.gridx = 0;
		c.gridy = 2;
		c.fill = GridBagConstraints.NONE;
		c.weightx = 0;
		formPanel.add(ElementFactory.createLabel(DataTypes.EquateRangeDialog_RelativeToEquate, baseEquateComboBox), c);
		c.gridx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		formPanel.add(baseEquateComboBox, c);

		JButton okButton = ElementFactory.createButton(Actions.ButtonBar_OK, true);
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
	}

	/** Ported from EquateRangeDialog::OnOK. Unlike the C++ version, a validation failure only shows the error - it never closes the dialog either way, so no explicit "keep it open" step is needed. */
	private void performOK() {
		int startAddress = getAddress(startAddressField);
		int endAddress = getAddress(endAddressField);
		Equate selectedEquate = (Equate) baseEquateComboBox.getSelectedItem();

		String title = "Define address range";
		if (startAddress == 0) {
			JOptionPane.showMessageDialog(this, "Invalid start address.", title, JOptionPane.ERROR_MESSAGE);
		} else if (endAddress == 0) {
			JOptionPane.showMessageDialog(this, "Invalid end address.", title, JOptionPane.ERROR_MESSAGE);
		} else if (startAddress > endAddress) {
			JOptionPane.showMessageDialog(this, "Start address greater than end address.", title, JOptionPane.ERROR_MESSAGE);
		} else if (selectedEquate == null) {
			JOptionPane.showMessageDialog(this, "No base equate selected.", title, JOptionPane.ERROR_MESSAGE);
		} else {
			int baseAddress = selectedEquate.getLabelValue();
			if (baseAddress >= startAddress && baseAddress <= endAddress) {
				JOptionPane.showMessageDialog(this, "Equate address is inside range.", title, JOptionPane.ERROR_MESSAGE);
			} else {
				userEquateList.setRange(selectedEquate.getLabel(), baseAddress, startAddress, endAddress);
				confirmed = true;
				setVisible(false);
			}
		}
	}

	/** Ported from EditControl::GetAddress: an unparseable value is silently treated as 0, matching {@code swscanf(..., L"%04hX", ...)}'s behavior on no match. */
	private static int getAddress(JTextField field) {
		try {
			return Integer.parseInt(field.getText().trim(), 16) & 0xFFFF;
		} catch (NumberFormatException ex) {
			return 0;
		}
	}

	/** Ported from EquateRangeDialog::FillCombobox, including the pre-selection of an equate matching {@code address}. */
	private void fillComboBox(EquateList equateList, boolean addressSpecified, int address) {
		for (Equate equate : equateList.getEquates()) {
			if (!equate.isRange()) {
				baseEquateComboBox.addItem(equate);
				if (addressSpecified && equate.getLabelValue() == address) {
					baseEquateComboBox.setSelectedItem(equate);
				}
			}
		}
	}

	/**
	 * Ported from EquateRangeDialog::Show/InitDialog/OnOK, folded into one
	 * blocking call as is idiomatic for a Swing modal {@link JDialog}.
	 * {@code address}, if non-empty, is a plain (no "$" prefix) hexadecimal
	 * address used to pre-select a matching base equate - not exercised by
	 * any current caller (see {@code Dis6502}), same as the C++ version.
	 */
	public boolean show(EquateList systemEquateList, EquateList userEquateList, String address) {
		this.userEquateList = userEquateList;

		int parsedAddress = 0;
		boolean addressSpecified = false;
		if (!address.isEmpty()) {
			try {
				parsedAddress = Integer.parseInt(address, 16) & 0xFFFF;
				addressSpecified = true;
			} catch (NumberFormatException ex) {
				addressSpecified = false;
			}
		}

		startAddressField.setText("");
		endAddressField.setText("");
		baseEquateComboBox.removeAllItems();
		fillComboBox(systemEquateList, addressSpecified, parsedAddress);
		fillComboBox(userEquateList, addressSpecified, parsedAddress);

		// Packed here, not in the constructor: the combo box is still empty at
		// construction time, so packing then sized the dialog too narrow to
		// show the populated item text once items were added afterwards.
		pack();
		setLocationRelativeTo(getOwner());

		confirmed = false;
		setVisible(true); // Blocks until disposed/hidden - this is a modal dialog.

		return confirmed;
	}
}
