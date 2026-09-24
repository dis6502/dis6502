/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JPanel;

import com.wudsn.tools.base.Actions;
import com.wudsn.tools.base.gui.ElementFactory;
import com.wudsn.tools.dis6502.DataTypes;
import com.wudsn.tools.dis6502.Texts;

/**
 * A dialog for picking the mono-spaced {@link TextFont} used by every part
 * panel except the memory inspector's grid (see
 * {@code plans/CUSTOM_TEXT_FONT_PROPOSAL.md}) - either the computer's own
 * native font (the default, {@link Texts#TextFontDialog_NativeFont} in
 * {@link #fontComboBox}) or any installed mono-spaced system font (see
 * {@link PlainTextFont#getAvailableFontFamilyNames}).
 * <p>
 * {@link #preview} shows a fixed sample string in the currently selected
 * font, updated live as the combo box selection changes, via the exact
 * same {@link TextFont} a real panel would end up using - so a distorted
 * or missing glyph in an installed font is visible before committing to
 * it. Persisting the choice and calling {@code Dis6502.updateFonts()} is
 * the caller's job, matching every other settings dialog in this project;
 * {@link #show} only returns the newly chosen font family name ({@code ""}
 * for the native font).
 *
 * @author Peter Dell
 */
public final class TextFontDialog extends JDialog {

	private static final long serialVersionUID = 1L;

	private static final String SAMPLE_TEXT = "ABCXYZ 0123 abcxyz";
	private static final int PREVIEW_POINT_SIZE = 16;

	private final JComboBox<String> fontComboBox = new JComboBox<>();
	private final JPanel preview = new JPanel() {
		private static final long serialVersionUID = 1L;

		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			selectedTextFont().drawText((Graphics2D) g, SAMPLE_TEXT, Color.BLACK, 4, 4);
		}
	};
	private final JButton okButton = ElementFactory.createButton(Actions.ButtonBar_OK, true);

	private ComputerFont nativeFont;
	private boolean confirmed;
	private String result = "";

	public TextFontDialog(Frame owner) {
		super(owner, true);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setTitle(Texts.TextFontDialog_Title);

		fontComboBox.addActionListener(e -> preview.repaint());

		preview.setBackground(Color.WHITE);
		preview.setPreferredSize(new Dimension(320, 40));

		JPanel formPanel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(4, 4, 4, 4);
		c.anchor = GridBagConstraints.WEST;
		c.gridx = 0;
		c.gridy = 0;
		formPanel.add(ElementFactory.createLabel(DataTypes.TextFontDialog_Font, fontComboBox), c);
		c.gridx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		formPanel.add(fontComboBox, c);

		okButton.addActionListener(e -> performOK());
		JButton cancelButton = ElementFactory.createButton(Actions.ButtonBar_Cancel, true);
		cancelButton.addActionListener(e -> setVisible(false));
		JPanel buttonPanel = new JPanel();
		buttonPanel.add(okButton);
		buttonPanel.add(cancelButton);

		getContentPane().setLayout(new BorderLayout(4, 4));
		getContentPane().add(formPanel, BorderLayout.NORTH);
		getContentPane().add(preview, BorderLayout.CENTER);
		getContentPane().add(buttonPanel, BorderLayout.SOUTH);

		getRootPane().setDefaultButton(okButton);
		ElementUtilities.closeOnEscape(this, cancelButton::doClick);
	}

	/** {@code ""} (the native font entry) or a real installed family name - never {@code null}. */
	private TextFont selectedTextFont() {
		Object selected = fontComboBox.getSelectedItem();
		if (selected == null || Texts.TextFontDialog_NativeFont.equals(selected)) {
			return nativeFont;
		}
		return PlainTextFont.get((String) selected, PREVIEW_POINT_SIZE, false);
	}

	private void performOK() {
		Object selected = fontComboBox.getSelectedItem();
		result = (selected == null || Texts.TextFontDialog_NativeFont.equals(selected)) ? "" : (String) selected;
		confirmed = true;
		setVisible(false);
	}

	/**
	 * Opens the dialog pre-selecting {@code currentFontFamilyName} ({@code
	 * ""} = native font); {@code nativeFont} drives {@link #preview} while
	 * that entry is selected. Returns the newly chosen family name if the
	 * user clicked OK ({@code ""} for the native font), or {@code
	 * currentFontFamilyName} unchanged if they clicked Cancel/closed the
	 * dialog - the caller only needs to act if the result differs from
	 * what it passed in.
	 */
	public String show(String currentFontFamilyName, ComputerFont nativeFont) {
		this.nativeFont = nativeFont;

		fontComboBox.removeAllItems();
		fontComboBox.addItem(Texts.TextFontDialog_NativeFont);
		for (String familyName : PlainTextFont.getAvailableFontFamilyNames()) {
			fontComboBox.addItem(familyName);
		}
		fontComboBox.setSelectedItem(currentFontFamilyName.isEmpty() ? Texts.TextFontDialog_NativeFont : currentFontFamilyName);

		// Packed here, not in the constructor: the combo box is still empty at
		// construction time, so packing then sized the dialog too narrow to
		// show the populated item text once items were added afterwards.
		pack();
		setLocationRelativeTo(getOwner());

		confirmed = false;
		result = currentFontFamilyName;
		setVisible(true); // Blocks until disposed/hidden - this is a modal dialog.

		return confirmed ? result : currentFontFamilyName;
	}
}
