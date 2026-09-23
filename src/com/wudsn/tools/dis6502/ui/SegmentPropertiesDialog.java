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
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.wudsn.tools.base.Actions;
import com.wudsn.tools.base.gui.ElementFactory;
import com.wudsn.tools.base.gui.ValueSetField;
import com.wudsn.tools.dis6502.DataTypes;
import com.wudsn.tools.dis6502.Messages;
import com.wudsn.tools.dis6502.Texts;
import com.wudsn.tools.dis6502.model.ProcessorType;
import com.wudsn.tools.dis6502.model.Segment;
import com.wudsn.tools.dis6502.model.Workspace;

/**
 * A dialog for editing a segment's begin address, binary flag, label
 * prefix, and processor type.
 * <p>
 * Folded into one blocking {@link #show} call, as is idiomatic for a Swing
 * modal {@link JDialog}. {@link #performOK} mutates {@code segment}
 * directly.
 *
 * @author Peter Dell
 */
public final class SegmentPropertiesDialog extends JDialog {

	private static final long serialVersionUID = 1L;


	private final JTextField addressField = new JTextField(6);
	private final JCheckBox binaryCheckBox = ElementFactory.createCheckBox(DataTypes.SegmentPropertiesDialog_Binary);
	private final JTextField labelPrefixField = new JTextField(10);
	private final ValueSetField<ProcessorType> processorField = new ValueSetField<ProcessorType>(ProcessorType.getSelectableValues());

	private Segment segment;
	private boolean confirmed;

	public SegmentPropertiesDialog(Frame owner) {
		super(owner, true);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setTitle(Texts.SegmentPropertiesDialog_Title);

		JPanel formPanel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(4, 4, 4, 4);
		c.anchor = GridBagConstraints.WEST;

		c.gridx = 0;
		c.gridy = 0;
		formPanel.add(ElementFactory.createLabel(DataTypes.SegmentPropertiesDialog_Address, addressField), c);
		c.gridx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		formPanel.add(addressField, c);

		c.gridx = 0;
		c.gridy = 1;
		c.fill = GridBagConstraints.NONE;
		c.weightx = 0;
		formPanel.add(ElementFactory.createLabel(DataTypes.SegmentPropertiesDialog_LabelPrefix, labelPrefixField), c);
		c.gridx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		formPanel.add(labelPrefixField, c);

		c.gridx = 0;
		c.gridy = 2;
		c.fill = GridBagConstraints.NONE;
		c.weightx = 0;
		formPanel.add(ElementFactory.createLabel(DataTypes.SegmentPropertiesDialog_Processor, processorField), c);
		c.gridx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		formPanel.add(processorField, c);

		c.gridx = 1;
		c.gridy = 3;
		c.fill = GridBagConstraints.NONE;
		formPanel.add(binaryCheckBox, c);

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

	/** Validates the address and, if it checks out, commits every field to the segment and closes the dialog. */
	private void performOK() {
		int begin = getAddress(addressField);
		int end = begin + segment.getSize() - 1; // TODO Will not work with >64K.

		if (begin > end) { // In case of 64K overflow.
			JOptionPane.showMessageDialog(this, Messages.E037.format(), Texts.SegmentPropertiesDialog_Title, JOptionPane.ERROR_MESSAGE);
			return;
		}

		segment.wBegin = begin;
		segment.wEnd = end;
		segment.bBinary = binaryCheckBox.isSelected();
		segment.labelPrefix = labelPrefixField.getText();
		segment.processorType = processorField.getValue();

		confirmed = true;
		setVisible(false);
	}

	/** Parses a plain hexadecimal address field; an unparseable value is silently treated as 0. */
	private static int getAddress(JTextField field) {
		try {
			return Integer.parseInt(field.getText().trim(), 16) & 0xFFFF;
		} catch (NumberFormatException ex) {
			return 0;
		}
	}

	/** Opens the dialog pre-filled with the segment's current properties. */
	public boolean show(Workspace workspace, Segment segment) {
		this.segment = segment;

		addressField.setText(String.format("%04X", segment.wBegin));
		binaryCheckBox.setSelected(segment.bBinary);
		labelPrefixField.setText(segment.labelPrefix);

		processorField.setValue(segment.processorType);

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
