/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.wudsn.tools.base.gui.ElementFactory;
import com.wudsn.tools.base.gui.ModalDialog;
import com.wudsn.tools.dis6502.Actions;
import com.wudsn.tools.dis6502.DataTypes;
import com.wudsn.tools.dis6502.Messages;
import com.wudsn.tools.dis6502.Texts;
import com.wudsn.tools.dis6502.model.Equate;
import com.wudsn.tools.dis6502.model.EquateList;
import com.wudsn.tools.dis6502.model.WorkspaceProperty;

/**
 * A dialog for viewing or editing an {@link EquateList}'s lines as plain
 * text.
 * <p>
 * "Add/Modify" parses input via {@link Equate#readFrom} through a scratch,
 * throwaway {@link EquateList} (since {@link Equate} can only be constructed
 * by {@link EquateList}), kept separate from the real {@code equateList}
 * until {@link #show} returns - so every edit is discarded unless the user
 * clicks OK. With exactly one line selected, committing replaces it in place
 * instead of appending a duplicate; that is the "Modify" half of the
 * button's name. "Display System Equates" (called with {@code
 * editable=false}) disables editing entirely rather than opening the same
 * editable dialog "Edit User Equates" does.
 *
 * @author Peter Dell
 */
public final class EquateDialog extends ModalDialog {

	private static final long serialVersionUID = 1L;

	private final DefaultListModel<String> listModel = new DefaultListModel<>();
	private final JList<String> equateJList = new JList<>(listModel);
	private final JTextField equateLineField = new JTextField();
	private final JButton addButton = ElementFactory.createButton(Actions.EquateDialog_AddModify, true);
	private final JButton deleteButton = ElementFactory.createButton(Actions.EquateDialog_Delete, true);

	private boolean editable;

	public EquateDialog(Frame owner) {
		super(owner, Texts.EquateDialog_EditTitle); // Display or edit - see show.

		equateLineField.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) {
				updateAddButtonEnabled();
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				updateAddButtonEnabled();
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				updateAddButtonEnabled();
			}
		});

		equateJList.addListSelectionListener(e -> {
			if (!e.getValueIsAdjusting()) {
				int index = equateJList.getSelectedIndex();
				if (index >= 0) {
					equateLineField.setText(listModel.get(index));
				}
				deleteButton.setEnabled(editable && index >= 0);
			}
		});

		addButton.setEnabled(false);
		addButton.addActionListener(e -> performAdd());

		deleteButton.setEnabled(false);
		deleteButton.addActionListener(e -> performDelete());

		JPanel editRowPanel = new JPanel(new BorderLayout(4, 4));
		editRowPanel.add(ElementFactory.createLabel(DataTypes.EquateDialog_Equate, equateLineField), BorderLayout.WEST);
		editRowPanel.add(equateLineField, BorderLayout.CENTER);
		JPanel editButtonPanel = new JPanel();
		editButtonPanel.add(addButton);
		editButtonPanel.add(deleteButton);
		editRowPanel.add(editButtonPanel, BorderLayout.EAST);

		JScrollPane equateScrollPane = new JScrollPane(equateJList);
		equateScrollPane.setPreferredSize(new Dimension(460, 300));

		JPanel mainPanel = new JPanel(new BorderLayout(4, 4));
		mainPanel.add(editRowPanel, BorderLayout.NORTH);
		mainPanel.add(equateScrollPane, BorderLayout.CENTER);
		getContentPane().add(mainPanel, BorderLayout.CENTER);
	}

	/** OK also commits the line being edited, as Add/Modify would. */
	@Override
	protected boolean validateOK() {
		performAdd();
		return true;
	}

	private void updateAddButtonEnabled() {
		addButton.setEnabled(editable && !equateLineField.getText().trim().isEmpty());
	}

	/**
	 * With exactly one line selected, committing replaces that line instead
	 * of appending a new one; with none (or more than one, which has no
	 * well-defined single target to replace) selected, it appends.
	 */
	private void performAdd() {
		if (!editable) {
			return;
		}
		String text = equateLineField.getText().trim();
		equateLineField.setText(text);
		if (text.isEmpty()) {
			return;
		}
		EquateList.EquateResult result = parse(text);
		if (result.equate == null) {
			if (!result.error.isEmpty()) {
				JOptionPane.showMessageDialog(this,
						// ERROR: Cannot parse equate line "{0}". Error: {1}
						Messages.E004.format(text, result.error), Texts.EquateDialog_EditTitle,
						JOptionPane.ERROR_MESSAGE);
			}
			return;
		}
		Equate equate = result.equate;
		int[] selectedIndices = equateJList.getSelectedIndices();
		int newIndex;
		if (selectedIndices.length == 1) {
			newIndex = selectedIndices[0];
			listModel.set(newIndex, equate.toString());
		} else {
			listModel.addElement(equate.toString());
			newIndex = listModel.size() - 1;
		}
		equateJList.setSelectedIndex(newIndex);
		equateJList.ensureIndexIsVisible(newIndex);
		deleteButton.setEnabled(true);
	}

	/**
	 * Parses one equate line on its own: the equate, or the reason it cannot be
	 * parsed. Package-private for tests.
	 */
	static EquateList.EquateResult parse(String text) {
		return new EquateList(WorkspaceProperty.USER_EQUATES).addEquate(text);
	}

	/** Removes the currently selected lines from the list. */
	private void performDelete() {
		if (!editable) {
			return;
		}
		int[] selectedIndices = equateJList.getSelectedIndices();
		for (int i = selectedIndices.length - 1; i >= 0; i--) {
			listModel.remove(selectedIndices[i]);
		}
		// Prevent a value that was populated via line selection from being added by accident.
		equateLineField.setText("");
		deleteButton.setEnabled(false);
	}

	/**
	 * Opens the dialog as one blocking call - a WUDSN Base
	 * {@link ModalDialog}. Returns {@code true}, and replaces {@code equateList}'s
	 * entire content with the dialog's edited lines, only if the user clicked
	 * OK on an editable dialog.
	 */
	public boolean show(EquateList equateList, boolean editable, String address) {
		this.editable = editable;
		setTitle(editable ? Texts.EquateDialog_EditTitle : Texts.EquateDialog_DisplayTitle);

		listModel.clear();
		for (Equate equate : equateList.getEquates()) {
			listModel.addElement(equate.toString());
		}
		equateLineField.setText("");
		equateLineField.setEditable(editable);
		addButton.setVisible(editable);
		deleteButton.setVisible(editable);
		addButton.setEnabled(false);
		deleteButton.setEnabled(false);

		if (!address.isEmpty()) {
			equateLineField.setText("L" + address + " = $" + address);
		}

		showModal(editable ? equateLineField : equateJList);

		boolean changed = okPressed && editable;
		if (changed) {
			equateList.clear();
			for (int i = 0; i < listModel.size(); i++) {
				equateList.addEquate(listModel.get(i));
			}
		}
		return changed;
	}
}
