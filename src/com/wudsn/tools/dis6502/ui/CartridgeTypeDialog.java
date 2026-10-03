/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Frame;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.List;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;

import com.wudsn.tools.base.Actions;
import com.wudsn.tools.base.atari.CartridgeType;
import com.wudsn.tools.base.common.TextUtility;
import com.wudsn.tools.base.gui.ElementFactory;
import com.wudsn.tools.dis6502.DataTypes;
import com.wudsn.tools.dis6502.Texts;

/**
 * A dialog for choosing the cartridge type of a raw ROM image (no CART
 * header) whose size several types - or one type and plain data - share:
 * a raw image does not say how its banks are mapped. The user picks a type
 * from the candidates, opens the file as raw file instead, or cancels.
 * <p>
 * The candidates are listed with their text and type number (as in a CART
 * header and in emulators), in the order given, the first one selected.
 *
 * @author Peter Dell
 */
public final class CartridgeTypeDialog extends JDialog {

	private static final long serialVersionUID = 1L;

	private final JTextField filePathField = new JTextField();
	private final DefaultListModel<CartridgeType> listModel = new DefaultListModel<>();
	private final JList<CartridgeType> cartridgeTypeList = new JList<>(listModel);
	private final JButton okButton = ElementFactory.createButton(Actions.ButtonBar_OK, true);
	// Fully qualified: com.wudsn.tools.base.Actions is already imported as "Actions" for ButtonBar_OK/Cancel.
	private final JButton rawFileButton = ElementFactory
			.createButton(com.wudsn.tools.dis6502.Actions.CartridgeTypeDialog_OpenAsRawFile, true);
	private final JButton cancelButton = ElementFactory.createButton(Actions.ButtonBar_Cancel, true);

	private CartridgeType cartridgeType;
	private boolean confirmed;

	public CartridgeTypeDialog(Frame owner) {
		super(owner, true);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setTitle(Texts.CartridgeTypeDialog_Title);

		filePathField.setEditable(false);

		cartridgeTypeList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		cartridgeTypeList.setCellRenderer(new DefaultListCellRenderer() {
			private static final long serialVersionUID = 1L;

			@Override
			public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
					boolean cellHasFocus) {
				CartridgeType type = (CartridgeType) value;
				String text = TextUtility.format(Texts.CartridgeTypeDialog_CartridgeTypeText, type.getText(),
						String.valueOf(type.getNumericId()));
				return super.getListCellRendererComponent(list, text, index, isSelected, cellHasFocus);
			}
		});
		cartridgeTypeList.addListSelectionListener(e -> okButton.setEnabled(cartridgeTypeList.getSelectedValue() != null));
		cartridgeTypeList.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2 && okButton.isEnabled()) {
					okButton.doClick();
				}
			}
		});

		okButton.setEnabled(false);
		okButton.addActionListener(e -> close(true, cartridgeTypeList.getSelectedValue()));
		rawFileButton.addActionListener(e -> close(true, null));
		cancelButton.addActionListener(e -> close(false, null));

		JPanel topPanel = new JPanel(new BorderLayout(4, 4));
		topPanel.add(ElementFactory.createLabel(DataTypes.CartridgeTypeDialog_File, filePathField), BorderLayout.WEST);
		topPanel.add(filePathField, BorderLayout.CENTER);
		topPanel.add(ElementFactory.createLabel(DataTypes.CartridgeTypeDialog_CartridgeType, cartridgeTypeList),
				BorderLayout.SOUTH);

		JPanel buttonPanel = new JPanel();
		buttonPanel.add(okButton);
		buttonPanel.add(rawFileButton);
		buttonPanel.add(cancelButton);

		getContentPane().setLayout(new BorderLayout(4, 4));
		getContentPane().add(topPanel, BorderLayout.NORTH);
		getContentPane().add(new JScrollPane(cartridgeTypeList), BorderLayout.CENTER);
		getContentPane().add(buttonPanel, BorderLayout.SOUTH);
		setSize(460, 360);
		setLocationRelativeTo(owner);

		getRootPane().setDefaultButton(okButton);
		ElementUtilities.closeOnEscape(this, cancelButton::doClick);
	}

	private void close(boolean confirmed, CartridgeType cartridgeType) {
		this.confirmed = confirmed;
		this.cartridgeType = cartridgeType;
		setVisible(false);
	}

	/** Fills the dialog for {@link #show}; package-private so tests can fill it without the blocking modal call. */
	void setInput(File file, List<CartridgeType> candidates) {
		filePathField.setText(
				TextUtility.format(Texts.CartridgeTypeDialog_FileText, file.getPath(), String.valueOf(file.length())));
		listModel.clear();
		for (CartridgeType candidate : candidates) {
			listModel.addElement(candidate);
		}
		if (!candidates.isEmpty()) {
			cartridgeTypeList.setSelectedIndex(0);
		}
		okButton.setEnabled(!candidates.isEmpty());
		confirmed = false;
		cartridgeType = null;
	}

	/**
	 * Opens the dialog as one blocking call. Returns {@code false} if the user
	 * cancelled; otherwise {@link #getCartridgeType} gives the chosen type, or
	 * {@code null} if the file shall be opened as raw file.
	 */
	public boolean show(File file, List<CartridgeType> candidates) {
		setInput(file, candidates);
		setVisible(true); // Blocks until disposed/hidden - this is a modal dialog.
		return confirmed;
	}

	/** @return the chosen type, or {@code null} to open the file as raw file */
	public CartridgeType getCartridgeType() {
		return cartridgeType;
	}

	/** Whether the user confirmed with OK or "Open as Raw File"; package-private for tests. */
	boolean isConfirmed() {
		return confirmed;
	}

	/** For tests: the OK, "Open as Raw File" and Cancel buttons. */
	JButton[] getButtons() {
		return new JButton[] { okButton, rawFileButton, cancelButton };
	}

	/** For tests: the listed types. */
	JList<CartridgeType> getCartridgeTypeList() {
		return cartridgeTypeList;
	}
}
