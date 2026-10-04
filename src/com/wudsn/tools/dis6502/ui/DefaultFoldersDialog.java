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
import java.util.HashMap;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.wudsn.tools.base.common.TextUtility;
import com.wudsn.tools.base.gui.ElementFactory;
import com.wudsn.tools.base.gui.ModalDialog;
import com.wudsn.tools.dis6502.Actions;
import com.wudsn.tools.dis6502.Texts;
import com.wudsn.tools.dis6502.model.DefaultFolders;
import com.wudsn.tools.dis6502.model.FolderType;

/**
 * A dialog for editing the remembered default folder for each
 * {@link FolderType} of one computer system.
 * <p>
 * Folder selection uses a {@link JFileChooser} in {@link
 * JFileChooser#DIRECTORIES_ONLY} mode. {@link FolderType#UNKNOWN} has no
 * field here - it is never user-editable, only ever set from the
 * application's own module path (see {@code
 * DefaultFoldersLogic.createDefaultFolders}).
 *
 * @author Peter Dell
 */
public final class DefaultFoldersDialog extends ModalDialog {

	private static final long serialVersionUID = 1L;

	private static final FolderType[] EDITABLE_FOLDER_TYPES = { FolderType.RAW_FILES, FolderType.EXECUTABLE_FILES,
			FolderType.CASSETTE_IMAGE_FILES, FolderType.ROM_IMAGE_FILES, FolderType.DISK_IMAGE_FILES,
			FolderType.WORKSPACE_FILES, FolderType.EQUATES_FILES, FolderType.PROFILE_FILES,
			FolderType.DISASSEMBLY_FILES };

	private final Map<FolderType, JTextField> fields = new HashMap<>(); // FolderType is a value set, not an enum.

	public DefaultFoldersDialog(Frame owner) {
		super(owner, ""); // The title names the computer system - see show.

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

			// No mnemonic: this same Action is reused for one button per row, all simultaneously visible.
			JButton browseButton = ElementFactory.createButton(Actions.DefaultFoldersDialog_Browse, false);
			browseButton.addActionListener(e -> browse(folderType, field));
			c.gridx = 2;
			c.fill = GridBagConstraints.NONE;
			c.weightx = 0;
			formPanel.add(browseButton, c);

			row++;
		}

		getContentPane().add(formPanel, BorderLayout.CENTER);
	}

	/** Opens a directory chooser for {@code folderType}'s field. */
	private void browse(FolderType folderType, JTextField field) {
		JFileChooser fileChooser = new JFileChooser(field.getText());
		fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		fileChooser.setDialogTitle(TextUtility.format(Texts.DefaultFoldersDialog_SubTitle, folderType.getText()));
		if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			field.setText(fileChooser.getSelectedFile().getPath());
		}
	}

	/**
	 * Opens the dialog as one blocking call - a WUDSN Base
	 * {@link ModalDialog}. Returns {@code true}, and writes the edited values
	 * back into {@code defaultFolders}, only if the user clicked OK.
	 */
	public boolean show(DefaultFolders defaultFolders) {
		setTitle(TextUtility.format(Texts.DefaultFoldersDialog_Title, defaultFolders.getComputerSystemType().getText()));
		for (FolderType folderType : EDITABLE_FOLDER_TYPES) {
			fields.get(folderType).setText(defaultFolders.getFolderPath(folderType));
		}
		showModal(fields.get(EDITABLE_FOLDER_TYPES[0]));

		if (okPressed) {
			for (FolderType folderType : EDITABLE_FOLDER_TYPES) {
				defaultFolders.setFolderPath(folderType, fields.get(folderType).getText());
			}
		}
		return okPressed;
	}
}
