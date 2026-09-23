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
import javax.swing.JPanel;

import com.wudsn.tools.base.Actions;
import com.wudsn.tools.base.gui.ElementFactory;
import com.wudsn.tools.base.gui.ValueSetField;
import com.wudsn.tools.dis6502.DataTypes;
import com.wudsn.tools.dis6502.Texts;
import com.wudsn.tools.dis6502.model.Workspace;
import com.wudsn.tools.dis6502.model.system.ComputerSystemType;

/**
 * A dialog for choosing the computer system a new workspace targets.
 * <p>
 * Folded into one blocking {@link #show} call, as is idiomatic for a Swing
 * modal {@link JDialog}.
 *
 * @author Peter Dell
 */
public final class WorkspaceDialog extends JDialog {

	private static final long serialVersionUID = 1L;

	private final ValueSetField<ComputerSystemType> computerSystemField = new ValueSetField<ComputerSystemType>(
			ComputerSystemType.getSelectableValues());

	private Workspace workspace;
	private boolean confirmed;

	public WorkspaceDialog(Frame owner) {
		super(owner, true);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setTitle(Texts.WorkspaceDialog_Title);

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

		ElementUtilities.closeOnEscape(this, cancelButton::doClick);
	}

	/** Commits the selected computer system to the workspace and closes the dialog. */
	private void performOK() {
		ComputerSystemType selected = computerSystemField.getValue();
		if (selected != null) {
			workspace.setComputerSystemType(selected);
		}
		confirmed = true;
		setVisible(false);
	}

	/** Opens the dialog pre-selecting the workspace's current computer system, if selectable. */
	public boolean show(Workspace workspace) {
		this.workspace = workspace;

		// An unknown system is not on offer - leave the first one selected then.
		ComputerSystemType computerSystemType = workspace.getComputerSystem().getType();
		if (ComputerSystemType.getSelectableValues().contains(computerSystemType)) {
			computerSystemField.setValue(computerSystemType);
		}

		pack();
		setLocationRelativeTo(getOwner());

		confirmed = false;
		setVisible(true); // Blocks until disposed/hidden - this is a modal dialog.

		return confirmed;
	}
}
