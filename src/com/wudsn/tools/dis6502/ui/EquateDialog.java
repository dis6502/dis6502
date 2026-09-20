/*
 * Copyright (C) 2025 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.ui;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.wudsn.tools.dis6502.model.Equate;
import com.wudsn.tools.dis6502.model.EquateList;
import com.wudsn.tools.dis6502.model.WorkspaceProperty;

/**
 * A dialog for viewing or editing an {@link EquateList}'s lines as plain
 * text.
 * <p>
 * Ported from ui/EquateDialog.h / EquateDialog.cpp, with three deviations:
 * <ul>
 * <li>The C++ source's {@code IDC_ADD_EQUATE} handler body is entirely
 * commented out (it calls {@code EquateList::AddEquate} expecting an
 * {@code int} index back, but that method now returns an {@code Equate*},
 * so the code as written would not even compile) - "Add/Modify" is
 * currently a silent no-op there. Reimplemented here using the parsing
 * half that is still real: {@link Equate#readFrom} via a scratch,
 * throwaway {@link EquateList} (since {@link Equate} itself can only be
 * constructed by {@link EquateList}, its C++ {@code friend class}), kept
 * separate from the real {@code equateList} until {@link #show} returns,
 * matching what {@code OnOK} already does (discard everything unless the
 * user clicks OK).</li>
 * <li>{@link #performAdd} actually implements the "Modify" half of that same
 * button's name: with exactly one line selected, committing replaces it in
 * place instead of appending a duplicate. Neither this dialog nor its C++
 * original ever did that - not even the commented-out {@code
 * IDC_ADD_EQUATE} body above, which only ever appended regardless of
 * selection - so editing an existing line's text and committing it used to
 * just add a second, stale copy alongside the original.</li>
 * <li>The C++ source's {@code editable} field is stored but never actually
 * read anywhere else in the file, so "Display System Equates" (called with
 * {@code editable=false}) would open the exact same fully-editable dialog
 * as "Edit User Equates" does. This port actually disables editing when
 * {@code editable} is {@code false}.</li>
 * </ul>
 *
 * @author Peter Dell
 */
public final class EquateDialog extends JDialog {

	private static final long serialVersionUID = 1L;

	private final DefaultListModel<String> listModel = new DefaultListModel<>();
	private final JList<String> equateJList = new JList<>(listModel);
	private final JTextField equateLineField = new JTextField();
	private final JButton addButton = new JButton("Add/Modify");
	private final JButton deleteButton = new JButton("Delete");

	private boolean editable;
	private boolean confirmed;

	public EquateDialog(Frame owner) {
		super(owner, true);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

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

		JButton okButton = new JButton("OK");
		okButton.addActionListener(e -> {
			performAdd();
			confirmed = true;
			setVisible(false);
		});
		JButton cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(e -> {
			confirmed = false;
			setVisible(false);
		});

		JPanel editRowPanel = new JPanel(new BorderLayout(4, 4));
		editRowPanel.add(new JLabel("Equate"), BorderLayout.WEST);
		editRowPanel.add(equateLineField, BorderLayout.CENTER);
		JPanel editButtonPanel = new JPanel();
		editButtonPanel.add(addButton);
		editButtonPanel.add(deleteButton);
		editRowPanel.add(editButtonPanel, BorderLayout.EAST);

		JPanel okCancelPanel = new JPanel();
		okCancelPanel.add(okButton);
		okCancelPanel.add(cancelButton);

		getContentPane().setLayout(new BorderLayout(4, 4));
		getContentPane().add(editRowPanel, BorderLayout.NORTH);
		getContentPane().add(new JScrollPane(equateJList), BorderLayout.CENTER);
		getContentPane().add(okCancelPanel, BorderLayout.SOUTH);
		setSize(480, 420);
		setLocationRelativeTo(owner);

		getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
				"cancel");
		getRootPane().getActionMap().put("cancel", new AbstractAction() {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				cancelButton.doClick();
			}
		});
	}

	private void updateAddButtonEnabled() {
		addButton.setEnabled(editable && !equateLineField.getText().trim().isEmpty());
	}

	/**
	 * Ported from EquateDialog::ProcessCommand's {@code IDC_ADD_EQUATE} case
	 * - see the class javadoc for why this is a real implementation instead
	 * of the C++ source's commented-out one. The "Modify" half of the
	 * button's own name is a further departure: neither this dialog nor its
	 * C++ original ever actually replaced the selected line in place - even
	 * the commented-out {@code IDC_ADD_EQUATE} body only ever appended,
	 * regardless of selection, so editing an existing line's text and
	 * committing it just added a duplicate rather than replacing the
	 * original. Now, with exactly one line selected, committing replaces
	 * that line instead of appending a new one; with none (or more than
	 * one, which has no well-defined single target to replace) selected, it
	 * still appends, exactly as before.
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
		EquateList scratch = new EquateList(WorkspaceProperty.USER_EQUATES);
		Equate equate = scratch.addEquate(text);
		if (equate == null) {
			return; // TODO: ERROR HANDLING - matches the C++ source's own unresolved TODO.
		}
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

	/** Ported from EquateDialog::ProcessCommand's {@code IDC_DELETE_EQUATE} case. */
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
	 * Ported from EquateDialog::Show/InitDialog/FillListbox/OnOK, folded into
	 * one blocking call as is idiomatic for a Swing modal {@link JDialog}.
	 * Returns {@code true}, and replaces {@code equateList}'s entire content
	 * with the dialog's edited lines (matching {@code OnOK}), only if the
	 * user clicked OK on an editable dialog.
	 */
	public boolean show(EquateList equateList, boolean editable, String address) {
		this.editable = editable;
		setTitle(editable ? "Edit Equates" : "Display Equates");

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
			SwingUtilities.invokeLater(equateLineField::requestFocusInWindow);
		}

		confirmed = false;
		setVisible(true); // Blocks until disposed/hidden - this is a modal dialog.

		boolean changed = confirmed && editable;
		if (changed) {
			equateList.clear();
			for (int i = 0; i < listModel.size(); i++) {
				equateList.addEquate(listModel.get(i));
			}
		}
		return changed;
	}
}
