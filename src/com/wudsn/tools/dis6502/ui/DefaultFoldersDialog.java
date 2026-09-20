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
import java.util.EnumMap;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.wudsn.tools.base.Actions;
import com.wudsn.tools.base.common.TextUtility;
import com.wudsn.tools.base.gui.ElementFactory;
import com.wudsn.tools.dis6502.Text;
import com.wudsn.tools.dis6502.model.DefaultFolders;
import com.wudsn.tools.dis6502.model.FolderType;

/**
 * A dialog for editing the remembered default folder for each
 * {@link FolderType} of one computer system.
 * <p>
 * Ported from ui/DefaultFoldersDialog.h / DefaultFoldersDialog.cpp,
 * simplified: the C++ version's {@code ShellSelectFolder} (a raw
 * {@code IFileOpenDialog} COM call with {@code FOS_PICKFOLDERS}) becomes a
 * {@link JFileChooser} in {@link JFileChooser#DIRECTORIES_ONLY} mode. Like
 * the C++ version, {@link FolderType#UNKNOWN_FILES} has no field here - it
 * is never user-editable, only ever set from the application's own module
 * path (see {@code DefaultFoldersLogic.createDefaultFolders}).
 *
 * @author Peter Dell
 */
public final class DefaultFoldersDialog extends JDialog {

	private static final long serialVersionUID = 1L;

	private static final FolderType[] EDITABLE_FOLDER_TYPES = { FolderType.RAW_FILES, FolderType.EXECUTABLE_FILES,
			FolderType.CASSETTE_IMAGE_FILES, FolderType.ROM_IMAGE_FILES, FolderType.DISK_IMAGE_FILES,
			FolderType.WORKSPACE_FILES, FolderType.EQUATES_FILES, FolderType.PROFILE_FILES,
			FolderType.DISASSEMBLY_FILES };

	private final Map<FolderType, JTextField> fields = new EnumMap<>(FolderType.class);
	private boolean confirmed;

	public DefaultFoldersDialog(Frame owner) {
		super(owner, true);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		JPanel formPanel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(4, 4, 4, 4);

		int row = 0;
		for (FolderType folderType : EDITABLE_FOLDER_TYPES) {
			c.gridx = 0;
			c.gridy = row;
			c.anchor = GridBagConstraints.WEST;
			c.fill = GridBagConstraints.NONE;
			c.weightx = 0;
			formPanel.add(new JLabel(folderType.getText()), c);

			JTextField field = new JTextField(30);
			fields.put(folderType, field);
			c.gridx = 1;
			c.fill = GridBagConstraints.HORIZONTAL;
			c.weightx = 1;
			formPanel.add(field, c);

			JButton browseButton = new JButton("Browse...");
			browseButton.addActionListener(e -> browse(folderType, field));
			c.gridx = 2;
			c.fill = GridBagConstraints.NONE;
			c.weightx = 0;
			formPanel.add(browseButton, c);

			row++;
		}

		JButton okButton = ElementFactory.createButton(Actions.ButtonBar_OK, true);
		okButton.addActionListener(e -> {
			confirmed = true;
			setVisible(false);
		});
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

	/** Ported from DefaultFoldersDialog::SelectFolder. */
	private void browse(FolderType folderType, JTextField field) {
		JFileChooser fileChooser = new JFileChooser(field.getText());
		fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		fileChooser.setDialogTitle(TextUtility.format(Text.IDS_DEFAULT_FOLDERS_DIALOG_SUB_TITLE, folderType.getText()));
		if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			field.setText(fileChooser.getSelectedFile().getPath());
		}
	}

	/**
	 * Ported from DefaultFoldersDialog::Show/SetDialogValues/GetDialogValues/
	 * OnOK, folded into one blocking call as is idiomatic for a Swing modal
	 * {@link JDialog}. Returns {@code true}, and writes the edited values
	 * back into {@code defaultFolders}, only if the user clicked OK.
	 */
	public boolean show(DefaultFolders defaultFolders) {
		setTitle(TextUtility.format(Text.IDS_DEFAULT_FOLDERS_DIALOG_TITLE, defaultFolders.getComputerSystemTypeInfo().text));
		for (FolderType folderType : EDITABLE_FOLDER_TYPES) {
			fields.get(folderType).setText(defaultFolders.getFolderPath(folderType));
		}
		confirmed = false;

		setVisible(true); // Blocks until disposed/hidden - this is a modal dialog.

		if (confirmed) {
			for (FolderType folderType : EDITABLE_FOLDER_TYPES) {
				defaultFolders.setFolderPath(folderType, fields.get(folderType).getText());
			}
		}
		return confirmed;
	}
}
