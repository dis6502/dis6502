/*
 * Copyright (C) 2025 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.ui;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.wudsn.tools.dis6502.model.ProcessorType;
import com.wudsn.tools.dis6502.model.Segment;
import com.wudsn.tools.dis6502.model.Workspace;

/**
 * A dialog for editing a segment's begin address, binary flag, label
 * prefix, and processor type.
 * <p>
 * Ported from ui/SegmentPropertiesDialog.h / SegmentPropertiesDialog.cpp,
 * folded into one blocking {@link #show} call as is idiomatic for a Swing
 * modal {@link JDialog}. {@link #performOK} mutates {@code segment}
 * directly, matching {@code SegmentPropertiesDialog::OnOK}.
 *
 * @author Peter Dell
 */
public final class SegmentPropertiesDialog extends JDialog {

	private static final long serialVersionUID = 1L;

	private static final List<ProcessorType> PROCESSOR_TYPES = List.of(ProcessorType.MOS6502, ProcessorType.MOS65C02);

	private final JTextField addressField = new JTextField(6);
	private final JCheckBox binaryCheckBox = new JCheckBox("Binary");
	private final JTextField labelPrefixField = new JTextField(10);
	private final JComboBox<String> processorComboBox = new JComboBox<>();

	private Segment segment;
	private boolean confirmed;

	public SegmentPropertiesDialog(Frame owner) {
		super(owner, true);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setTitle("Segment Properties");

		JPanel formPanel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(4, 4, 4, 4);
		c.anchor = GridBagConstraints.WEST;

		c.gridx = 0;
		c.gridy = 0;
		formPanel.add(new JLabel("Address ($):"), c);
		c.gridx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		formPanel.add(addressField, c);

		c.gridx = 0;
		c.gridy = 1;
		c.fill = GridBagConstraints.NONE;
		c.weightx = 0;
		formPanel.add(new JLabel("Label Prefix:"), c);
		c.gridx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		formPanel.add(labelPrefixField, c);

		c.gridx = 0;
		c.gridy = 2;
		c.fill = GridBagConstraints.NONE;
		c.weightx = 0;
		formPanel.add(new JLabel("Processor:"), c);
		c.gridx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		formPanel.add(processorComboBox, c);

		c.gridx = 1;
		c.gridy = 3;
		c.fill = GridBagConstraints.NONE;
		formPanel.add(binaryCheckBox, c);

		JButton okButton = new JButton("OK");
		okButton.addActionListener(e -> performOK());
		JButton cancelButton = new JButton("Cancel");
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

	/** Ported from SegmentPropertiesDialog::OnOK. */
	private void performOK() {
		int begin = getAddress(addressField);
		int end = begin + segment.getSize() - 1; // TODO Will not work with >64K.

		if (begin > end) { // In case of 64K overflow.
			JOptionPane.showMessageDialog(this, "Error: Start address too high.\nThe segment would overlap in memory.",
					"Segment Properties", JOptionPane.ERROR_MESSAGE);
			return;
		}

		segment.wBegin = begin;
		segment.wEnd = end;
		segment.bBinary = binaryCheckBox.isSelected();
		segment.labelPrefix = labelPrefixField.getText();
		segment.processorType = PROCESSOR_TYPES.get(processorComboBox.getSelectedIndex());

		confirmed = true;
		setVisible(false);
	}

	/** Ported from EditControl::GetAddress: an unparseable value is silently treated as 0, matching {@code swscanf(..., L"%04hX", ...)}'s behavior on no match. */
	private static int getAddress(JTextField field) {
		try {
			return Integer.parseInt(field.getText().trim(), 16) & 0xFFFF;
		} catch (NumberFormatException ex) {
			return 0;
		}
	}

	/** Ported from SegmentPropertiesDialog::Show/InitDialog/CreateControls. */
	public boolean show(Workspace workspace, Segment segment) {
		this.segment = segment;

		addressField.setText(String.format("%04X", segment.wBegin));
		binaryCheckBox.setSelected(segment.bBinary);
		labelPrefixField.setText(segment.labelPrefix);

		processorComboBox.removeAllItems();
		List<String> names = new ArrayList<>();
		int selectedIndex = 0;
		for (int i = 0; i < PROCESSOR_TYPES.size(); i++) {
			ProcessorType processorType = PROCESSOR_TYPES.get(i);
			names.add(workspace.getInstructionSet(processorType).getName());
			if (processorType == segment.processorType) {
				selectedIndex = i;
			}
		}
		for (String name : names) {
			processorComboBox.addItem(name);
		}
		processorComboBox.setSelectedIndex(selectedIndex);

		confirmed = false;
		setVisible(true); // Blocks until disposed/hidden - this is a modal dialog.

		return confirmed;
	}
}
