/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.ui;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.wudsn.tools.base.gui.ModalDialog;
import com.wudsn.tools.dis6502.Texts;
import com.wudsn.tools.dis6502.model.Workspace;

/**
 * A dialog for choosing the equate contexts active in the whole workspace:
 * one check box per context name the loaded system and user equates use.
 * The check boxes are built in {@link #show}, since the names are data, not
 * texts of the dialog.
 *
 * @author Peter Dell
 */
public final class EquateContextsDialog extends ModalDialog {

	private static final long serialVersionUID = 1L;

	private final JPanel checkBoxPanel = new JPanel(new GridLayout(0, 1));
	private final JLabel noContextsLabel = new JLabel(Texts.EquateContextsDialog_NoContexts);
	private final List<JCheckBox> checkBoxes = new ArrayList<>();

	private Workspace workspace;
	private boolean changed;

	public EquateContextsDialog(Frame owner) {
		super(owner, Texts.EquateContextsDialog_Title);

		JPanel formPanel = new JPanel(new BorderLayout(0, 8));
		formPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		formPanel.add(new JLabel(Texts.EquateContextsDialog_Description), BorderLayout.NORTH);
		formPanel.add(checkBoxPanel, BorderLayout.CENTER);

		getContentPane().add(formPanel, BorderLayout.CENTER);
	}

	/** Commits the checked contexts to the workspace. */
	@Override
	protected boolean validateOK() {
		Set<String> activeContexts = new TreeSet<>();
		for (JCheckBox checkBox : checkBoxes) {
			if (checkBox.isSelected()) {
				activeContexts.add(checkBox.getText());
			}
		}
		changed = !activeContexts.equals(workspace.getActiveContexts());
		workspace.setActiveContexts(activeContexts);
		return true;
	}

	/**
	 * Opens the dialog with the workspace's active contexts checked. An active
	 * context no loaded equate uses any more is still listed, so it can be
	 * unchecked. Returns whether the active contexts changed.
	 */
	public boolean show(Workspace workspace) {
		this.workspace = workspace;

		Set<String> contextNames = new TreeSet<>(workspace.getContextNames());
		contextNames.addAll(workspace.getActiveContexts());
		checkBoxPanel.removeAll();
		checkBoxes.clear();
		for (String contextName : contextNames) {
			JCheckBox checkBox = new JCheckBox(contextName, workspace.getActiveContexts().contains(contextName));
			checkBoxes.add(checkBox);
			checkBoxPanel.add(checkBox);
		}
		if (checkBoxes.isEmpty()) {
			checkBoxPanel.add(noContextsLabel);
		}

		changed = false;
		showModal(checkBoxes.isEmpty() ? getOKButton() : checkBoxes.get(0));
		return okPressed && changed;
	}
}
