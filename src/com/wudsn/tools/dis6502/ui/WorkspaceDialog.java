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
import javax.swing.JPanel;

import com.wudsn.tools.base.Actions;
import com.wudsn.tools.base.gui.ElementFactory;
import com.wudsn.tools.dis6502.DataTypes;
import com.wudsn.tools.dis6502.Text;
import com.wudsn.tools.dis6502.model.ComputerSystemFactory;
import com.wudsn.tools.dis6502.model.ComputerSystemType;
import com.wudsn.tools.dis6502.model.ComputerSystemTypeInfo;
import com.wudsn.tools.dis6502.model.Workspace;

/**
 * A dialog for choosing the computer system a new workspace targets.
 * <p>
 * Ported from ui/WorkspaceDialog.h / WorkspaceDialog.cpp, folded into one
 * blocking {@link #show} call as is idiomatic for a Swing modal
 * {@link JDialog}. Offers the same four systems, in the same order, as
 * {@code CreateControls}.
 *
 * @author Peter Dell
 */
public final class WorkspaceDialog extends JDialog {

	private static final long serialVersionUID = 1L;

	private final JComboBox<ComputerSystemTypeInfo> computerSystemComboBox = new JComboBox<>();

	private Workspace workspace;
	private boolean confirmed;

	public WorkspaceDialog(Frame owner) {
		super(owner, true);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setTitle(Text.IDS_MAIN_FILE_NEW_WORKSPACE_TITLE);

		computerSystemComboBox.setRenderer(new DefaultListCellRenderer() {
			private static final long serialVersionUID = 1L;

			@Override
			public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
					boolean cellHasFocus) {
				String text = value instanceof ComputerSystemTypeInfo ? ((ComputerSystemTypeInfo) value).text : "";
				return super.getListCellRendererComponent(list, text, index, isSelected, cellHasFocus);
			}
		});

		JPanel formPanel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(4, 4, 4, 4);
		c.anchor = GridBagConstraints.WEST;

		c.gridx = 0;
		c.gridy = 0;
		formPanel.add(ElementFactory.createLabel(DataTypes.WorkspaceDialog_ComputerSystem, computerSystemComboBox), c);
		c.gridx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		formPanel.add(computerSystemComboBox, c);

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

	/** Ported from WorkspaceDialog::OnOK. */
	private void performOK() {
		ComputerSystemTypeInfo selected = (ComputerSystemTypeInfo) computerSystemComboBox.getSelectedItem();
		if (selected != null) {
			workspace.setComputerSystemType(selected.type);
		}
		confirmed = true;
		setVisible(false);
	}

	/** Ported from WorkspaceDialog::Show/InitDialog/CreateControls. */
	public boolean show(Workspace workspace) {
		this.workspace = workspace;

		ComputerSystemFactory computerSystemFactory = workspace.getComputerSystemFactory();
		computerSystemComboBox.removeAllItems();
		computerSystemComboBox.addItem(computerSystemFactory.getComputerSystemTypeInfo(ComputerSystemType.ATARI5200));
		computerSystemComboBox.addItem(computerSystemFactory.getComputerSystemTypeInfo(ComputerSystemType.ATARI800));
		computerSystemComboBox.addItem(computerSystemFactory.getComputerSystemTypeInfo(ComputerSystemType.C64));
		computerSystemComboBox.addItem(computerSystemFactory.getComputerSystemTypeInfo(ComputerSystemType.ORIC));
		computerSystemComboBox.setSelectedItem(workspace.getComputerSystem().getTypeInfo());

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
