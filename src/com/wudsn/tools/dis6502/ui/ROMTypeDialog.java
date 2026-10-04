/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;

import com.wudsn.tools.base.common.TextUtility;
import com.wudsn.tools.base.gui.ElementFactory;
import com.wudsn.tools.base.gui.ModalDialog;
import com.wudsn.tools.dis6502.Actions;
import com.wudsn.tools.dis6502.DataTypes;
import com.wudsn.tools.dis6502.Texts;
import com.wudsn.tools.dis6502.model.system.ROMType;

/**
 * A dialog for choosing the {@link ROMType} of a ROM image whose type the
 * computer system cannot tell, e.g. a raw Atari cartridge image whose size
 * several cartridge types - or one type and plain data - share: a raw image
 * does not say how its banks are mapped. The user picks a ROM type, opens the
 * file as raw file instead, or cancels.
 * <p>
 * The ROM types are listed with their text, in the order given, the first one
 * selected. The dialog knows nothing about any computer system.
 *
 * @author Peter Dell
 */
public final class ROMTypeDialog extends ModalDialog {

	private static final long serialVersionUID = 1L;

	private final JTextField filePathField = new JTextField();
	private final DefaultListModel<ROMType> listModel = new DefaultListModel<>();
	private final JList<ROMType> romTypeList = new JList<>(listModel);
	private final JButton rawFileButton = ElementFactory
			.createButton(Actions.ROMTypeDialog_OpenAsRawFile, true);

	private ROMType romType;
	private boolean rawFile; // "Open as Raw File" was clicked.

	public ROMTypeDialog(Frame owner) {
		super(owner, Texts.ROMTypeDialog_Title);

		filePathField.setEditable(false);

		romTypeList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); // Shows ROMType.toString(), the text.
		romTypeList.addListSelectionListener(e -> getOKButton().setEnabled(romTypeList.getSelectedValue() != null));
		romTypeList.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2 && getOKButton().isEnabled()) {
					getOKButton().doClick();
				}
			}
		});

		getOKButton().setEnabled(false);
		rawFileButton.addActionListener(e -> {
			rawFile = true;
			romType = null;
			close();
		});
		addButtonBarButton(rawFileButton);

		JPanel topPanel = new JPanel(new BorderLayout(4, 4));
		topPanel.add(ElementFactory.createLabel(DataTypes.ROMTypeDialog_File, filePathField), BorderLayout.WEST);
		topPanel.add(filePathField, BorderLayout.CENTER);
		topPanel.add(ElementFactory.createLabel(DataTypes.ROMTypeDialog_ROMType, romTypeList), BorderLayout.SOUTH);

		JScrollPane romTypeScrollPane = new JScrollPane(romTypeList);
		romTypeScrollPane.setPreferredSize(new Dimension(440, 220));

		JPanel mainPanel = new JPanel(new BorderLayout(4, 4));
		mainPanel.add(topPanel, BorderLayout.NORTH);
		mainPanel.add(romTypeScrollPane, BorderLayout.CENTER);
		getContentPane().add(mainPanel, BorderLayout.CENTER);
	}

	/** OK: the selected ROM type. */
	@Override
	protected boolean validateOK() {
		romType = romTypeList.getSelectedValue();
		return romType != null;
	}

	/** Fills the dialog for {@link #show}; package-private so tests can fill it without the blocking modal call. */
	void setInput(File file, List<ROMType> romTypes) {
		filePathField.setText(
				TextUtility.format(Texts.ROMTypeDialog_FileText, file.getPath(), String.valueOf(file.length())));
		listModel.clear();
		for (ROMType candidate : romTypes) {
			listModel.addElement(candidate);
		}
		if (!romTypes.isEmpty()) {
			romTypeList.setSelectedIndex(0);
		}
		getOKButton().setEnabled(!romTypes.isEmpty());
		okPressed = false;
		rawFile = false;
		romType = null;
	}

	/**
	 * Opens the dialog as one blocking call. Returns {@code false} if the user
	 * cancelled; otherwise {@link #getROMType} gives the chosen ROM type, or
	 * {@code null} if the file shall be opened as raw file.
	 */
	public boolean show(File file, List<ROMType> romTypes) {
		setInput(file, romTypes);
		showModal(romTypeList);
		return isConfirmed();
	}

	/** @return the chosen ROM type, or {@code null} to open the file as raw file */
	public ROMType getROMType() {
		return romType;
	}

	/** Whether the user confirmed with OK or "Open as Raw File"; package-private for tests. */
	boolean isConfirmed() {
		return okPressed || rawFile;
	}

	/** For tests: the OK and "Open as Raw File" buttons. */
	JButton[] getButtons() {
		return new JButton[] { getOKButton(), rawFileButton };
	}

	/** For tests: the listed ROM types. */
	JList<ROMType> getROMTypeList() {
		return romTypeList;
	}
}
