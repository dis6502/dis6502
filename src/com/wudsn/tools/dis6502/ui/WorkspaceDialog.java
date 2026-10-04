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

import javax.swing.JPanel;

import com.wudsn.tools.base.gui.ElementFactory;
import com.wudsn.tools.base.gui.ModalDialog;
import com.wudsn.tools.base.gui.ValueSetField;
import com.wudsn.tools.dis6502.DataTypes;
import com.wudsn.tools.dis6502.Texts;
import com.wudsn.tools.dis6502.model.Workspace;
import com.wudsn.tools.dis6502.model.system.ComputerSystemType;

/**
 * A dialog for choosing the computer system a new workspace targets.
 * <p>
 * Shown by one blocking {@link #show} call - a WUDSN Base {@link
 * ModalDialog}.
 *
 * @author Peter Dell
 */
public final class WorkspaceDialog extends ModalDialog {

	private static final long serialVersionUID = 1L;

	private final ValueSetField<ComputerSystemType> computerSystemField = new ValueSetField<ComputerSystemType>(
			ComputerSystemType.getSelectableValues());

	private Workspace workspace;

	public WorkspaceDialog(Frame owner) {
		super(owner, Texts.WorkspaceDialog_Title);

		JPanel formPanel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(4, 4, 4, 4);
		c.anchor = GridBagConstraints.WEST;

		c.gridx = 0;
		c.gridy = 0;
		formPanel.add(ElementFactory.createLabel(DataTypes.WorkspaceDialog_ComputerSystem, computerSystemField), c);
		c.gridx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		formPanel.add(computerSystemField, c);

		getContentPane().add(formPanel, BorderLayout.CENTER);
	}

	/** Commits the selected computer system to the workspace and closes the dialog. */
	@Override
	protected boolean validateOK() {
		ComputerSystemType selected = computerSystemField.getValue();
		if (selected != null) {
			workspace.setComputerSystemType(selected);
		}
		return true;
	}

	/** Opens the dialog pre-selecting the workspace's current computer system, if selectable. */
	public boolean show(Workspace workspace) {
		this.workspace = workspace;

		// An unknown system is not on offer - leave the first one selected then.
		ComputerSystemType computerSystemType = workspace.getComputerSystem().getType();
		if (ComputerSystemType.getSelectableValues().contains(computerSystemType)) {
			computerSystemField.setValue(computerSystemType);
		}

		showModal(computerSystemField);
		return okPressed;
	}
}
