/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.ui;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import com.wudsn.tools.base.Actions;
import com.wudsn.tools.base.gui.ElementFactory;
import com.wudsn.tools.dis6502.DataTypes;
import com.wudsn.tools.dis6502.Texts;
import com.wudsn.tools.dis6502.model.system.atari800.AtariDisk;
import com.wudsn.tools.dis6502.model.system.atari800.AtariError;
import com.wudsn.tools.dis6502.model.system.atari800.AtariFile;

/**
 * A dialog for picking one executable file from an Atari DOS 2.x disk
 * image's directory.
 * <p>
 * Stores each entry's already-formatted display text and file name
 * directly (an {@link Entry}) at listing time, since {@link AtariFile} is
 * a mutable, reused out-parameter for {@link AtariDisk#findFirst}/{@link
 * AtariDisk#findNext} - keeping a reference to it across iterations would
 * alias every list item onto whatever the last directory entry happened to
 * be. The listed text uses {@link AtariFile#getDirectoryText()} (locked
 * flag, 8.3 name, sector count) rather than duplicating that formatting
 * logic here.
 *
 * @author Peter Dell
 */
public final class DiskImageExecutableFileDialog extends JDialog {

	private static final long serialVersionUID = 1L;

	private final JTextField diskImageFilePathField = new JTextField();
	private final DefaultListModel<Entry> listModel = new DefaultListModel<>();
	private final JList<Entry> filesList = new JList<>(listModel);
	private final JLabel fileNameLabel = new JLabel(" ");
	private final JButton okButton = ElementFactory.createButton(Actions.ButtonBar_OK, true);

	private String executableFileName = "";
	private boolean confirmed;

	public DiskImageExecutableFileDialog(Frame owner) {
		super(owner, true);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setTitle(Texts.DiskImageExecutableFileDialog_Title);

		diskImageFilePathField.setEditable(false);
		filesList.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

		filesList.addListSelectionListener(e -> {
			if (!e.getValueIsAdjusting()) {
				Entry selected = filesList.getSelectedValue();
				fileNameLabel.setText(selected != null ? selected.fileName : "");
				okButton.setEnabled(selected != null);
			}
		});
		filesList.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2 && okButton.isEnabled()) {
					okButton.doClick();
				}
			}
		});

		okButton.setEnabled(false);
		okButton.addActionListener(e -> performOK());
		JButton cancelButton = ElementFactory.createButton(Actions.ButtonBar_Cancel, true);
		cancelButton.addActionListener(e -> {
			confirmed = false;
			setVisible(false);
		});

		JPanel topPanel = new JPanel(new BorderLayout(4, 4));
		topPanel.add(ElementFactory.createLabel(DataTypes.DiskImageExecutableFileDialog_DiskImageFile, diskImageFilePathField), BorderLayout.WEST);
		topPanel.add(diskImageFilePathField, BorderLayout.CENTER);

		JPanel fileNamePanel = new JPanel(new BorderLayout(4, 4));
		fileNamePanel.add(ElementFactory.createLabel(DataTypes.DiskImageExecutableFileDialog_ExecutableFileName, fileNameLabel), BorderLayout.WEST);
		fileNamePanel.add(fileNameLabel, BorderLayout.CENTER);

		JPanel buttonPanel = new JPanel();
		buttonPanel.add(okButton);
		buttonPanel.add(cancelButton);

		JPanel southPanel = new JPanel(new BorderLayout());
		southPanel.add(fileNamePanel, BorderLayout.NORTH);
		southPanel.add(buttonPanel, BorderLayout.SOUTH);

		getContentPane().setLayout(new BorderLayout(4, 4));
		getContentPane().add(topPanel, BorderLayout.NORTH);
		getContentPane().add(new JScrollPane(filesList), BorderLayout.CENTER);
		getContentPane().add(southPanel, BorderLayout.SOUTH);
		setSize(420, 420);
		setLocationRelativeTo(owner);

		getRootPane().setDefaultButton(okButton);
		ElementUtilities.closeOnEscape(this, cancelButton::doClick);
	}

	/** Confirms the selected file and closes the dialog. */
	private void performOK() {
		Entry selected = filesList.getSelectedValue();
		if (selected == null) {
			return;
		}
		executableFileName = selected.fileName;
		confirmed = true;
		setVisible(false);
	}

	/**
	 * Opens the dialog as one blocking call, idiomatic for a Swing modal
	 * {@link JDialog}. Returns {@code true} if the user picked a file and
	 * clicked OK (or double-clicked it); {@link #getExecutableFileName} then
	 * gives its name.
	 */
	public boolean show(AtariDisk atariDisk) throws IOException {
		diskImageFilePathField.setText(atariDisk.getDiskImageFilePath());
		listModel.clear();
		fileNameLabel.setText("");
		okButton.setEnabled(false);

		AtariFile info = new AtariFile();
		AtariError error = atariDisk.findFirst(info);
		while (error == AtariError.OK) {
			listModel.addElement(new Entry(info.getDirectoryText(), info.getFileName()));
			error = atariDisk.findNext(info);
		}

		confirmed = false;
		setVisible(true); // Blocks until disposed/hidden - this is a modal dialog.

		return confirmed;
	}

	public String getExecutableFileName() {
		return executableFileName;
	}

	/** One directory listing entry, captured at listing time (see the class javadoc for why). */
	private static final class Entry {
		final String displayText;
		final String fileName;

		Entry(String displayText, String fileName) {
			this.displayText = displayText;
			this.fileName = fileName;
		}

		@Override
		public String toString() {
			return displayText;
		}
	}
}
