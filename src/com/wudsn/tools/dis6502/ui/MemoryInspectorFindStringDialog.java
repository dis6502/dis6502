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

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.wudsn.tools.base.Actions;
import com.wudsn.tools.base.common.TextUtility;
import com.wudsn.tools.base.gui.ElementFactory;
import com.wudsn.tools.base.repository.DataType;
import com.wudsn.tools.dis6502.DataTypes;
import com.wudsn.tools.dis6502.Texts;

/**
 * A dialog for searching the memory inspector for a byte sequence, typed
 * as either ASCII text or hexadecimal bytes - the two fields stay in sync
 * via {@link FindStringDialog}.
 * <p>
 * Folded into one blocking {@link #show} call, as is idiomatic for a Swing
 * modal {@link JDialog}. {@link #performOK}: on a successful search it
 * closes the dialog, on a failed one it shows a "not found" alert and
 * stays open - shown here rather than by the caller, since {@link
 * MemoryInspectorPanel} stays free of popups - see its javadoc.
 *
 * @author Peter Dell
 */
public final class MemoryInspectorFindStringDialog extends JDialog {

	private static final long serialVersionUID = 1L;

	private final FindStringDialog findStringDialog = new FindStringDialog();
	private final JTextField asciiField = new JTextField(20);
	private final JTextField hexField = new JTextField(20);
	private final JRadioButton allSegmentsRadioButton = radioButton(DataTypes.MemoryInspectorFindStringDialog_AllSegments);
	private final JRadioButton selectedSegmentRadioButton = radioButton(DataTypes.MemoryInspectorFindStringDialog_SelectedSegment);
	private final JButton okButton = ElementFactory.createButton(Actions.ButtonBar_OK, true);

	private MemoryInspectorPanel memoryInspectorPanel;
	private boolean updatingFields;
	private boolean confirmed;

	public MemoryInspectorFindStringDialog(Frame owner) {
		super(owner, true);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setTitle(Texts.MemoryInspectorFindStringDialog_Title);

		ButtonGroup scopeGroup = new ButtonGroup();
		scopeGroup.add(allSegmentsRadioButton);
		scopeGroup.add(selectedSegmentRadioButton);

		asciiField.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) {
				asciiChanged();
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				asciiChanged();
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				asciiChanged();
			}
		});
		hexField.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) {
				hexChanged();
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				hexChanged();
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				hexChanged();
			}
		});

		JPanel formPanel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(4, 4, 4, 4);
		c.anchor = GridBagConstraints.WEST;

		c.gridx = 0;
		c.gridy = 0;
		formPanel.add(ElementFactory.createLabel(DataTypes.MemoryInspectorFindStringDialog_AsciiString, asciiField), c);
		c.gridx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		formPanel.add(asciiField, c);

		c.gridx = 0;
		c.gridy = 1;
		c.fill = GridBagConstraints.NONE;
		c.weightx = 0;
		formPanel.add(ElementFactory.createLabel(DataTypes.MemoryInspectorFindStringDialog_Hex, hexField), c);
		c.gridx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		formPanel.add(hexField, c);

		JPanel scopePanel = new JPanel(new GridBagLayout());
		scopePanel.setBorder(BorderFactory.createTitledBorder(Texts.MemoryInspectorFindStringDialog_ScopeGroupTitle));
		GridBagConstraints sc = new GridBagConstraints();
		sc.anchor = GridBagConstraints.WEST;
		sc.gridx = 0;
		sc.gridy = 0;
		scopePanel.add(allSegmentsRadioButton, sc);
		sc.gridy = 1;
		scopePanel.add(selectedSegmentRadioButton, sc);

		c.gridx = 0;
		c.gridy = 2;
		c.gridwidth = 2;
		c.fill = GridBagConstraints.HORIZONTAL;
		formPanel.add(scopePanel, c);

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

	/** Builds a self-labeled radio button (text plus mnemonic) from a {@link DataType}, see {@link DataTypes}. */
	private static JRadioButton radioButton(DataType dataType) {
		JRadioButton radioButton = new JRadioButton();
		ElementUtilities.applyLabel(radioButton, dataType);
		return radioButton;
	}

	/** Keeps {@link #hexField} in sync as the user types in {@link #asciiField}. */
	private void asciiChanged() {
		if (updatingFields) {
			return;
		}
		boolean find = findStringDialog.asciiStringToHexString(asciiField.getText());
		updatingFields = true;
		hexField.setText(findStringDialog.getHexString());
		updatingFields = false;
		okButton.setEnabled(find);
	}

	/** Keeps {@link #asciiField} in sync as the user types in {@link #hexField}. */
	private void hexChanged() {
		if (updatingFields) {
			return;
		}
		boolean find = findStringDialog.hexStringToAsciiString(hexField.getText());
		updatingFields = true;
		asciiField.setText(findStringDialog.getAsciiString());
		updatingFields = false;
		okButton.setEnabled(find);
	}

	/** Runs the search; closes the dialog on a match, otherwise shows a "not found" alert. */
	private void performOK() {
		boolean find = findStringDialog.hexStringToAsciiString(hexField.getText());
		if (!find) {
			return;
		}

		boolean allSegments = allSegmentsRadioButton.isSelected();
		boolean found = memoryInspectorPanel.findString(findStringDialog.getAsciiString(), allSegments);
		if (found) {
			confirmed = true;
			setVisible(false);
		} else {
			JOptionPane.showMessageDialog(this,
					TextUtility.format(Texts.MemoryInspectorFindStringDialog_StringNotFoundMessage, findStringDialog.getAsciiString()),
					Texts.MemoryInspectorFindStringDialog_NotFoundTitle, JOptionPane.INFORMATION_MESSAGE);
		}
	}

	/** Opens the dialog pre-filled with the current search state; returns whether the user found a match. */
	public boolean show(MemoryInspectorPanel memoryInspectorPanel) {
		this.memoryInspectorPanel = memoryInspectorPanel;

		String initialFindString = memoryInspectorPanel.getFindString();
		boolean allSegments = memoryInspectorPanel.isFindAllSegments();

		updatingFields = true;
		findStringDialog.setAsciiString(initialFindString);
		asciiField.setText(findStringDialog.getAsciiString());
		hexField.setText(findStringDialog.getHexString());
		updatingFields = false;

		if (allSegments) {
			allSegmentsRadioButton.setSelected(true);
		} else {
			selectedSegmentRadioButton.setSelected(true);
		}
		okButton.setEnabled(!findStringDialog.getHexString().isEmpty());

		confirmed = false;
		setVisible(true); // Blocks until disposed/hidden - this is a modal dialog.

		return confirmed;
	}
}
