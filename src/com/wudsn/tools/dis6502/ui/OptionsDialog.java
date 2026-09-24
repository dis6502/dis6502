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
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import com.wudsn.tools.base.Actions;
import com.wudsn.tools.base.gui.ElementFactory;
import com.wudsn.tools.dis6502.DataTypes;
import com.wudsn.tools.dis6502.Options;
import com.wudsn.tools.dis6502.Texts;
import com.wudsn.tools.dis6502.model.system.ComputerSystemType;

/**
 * A general application-options dialog - today just the mono-spaced {@link
 * TextFont} used by every part panel except the memory inspector's grid
 * (see {@code plans/CUSTOM_TEXT_FONT_PROPOSAL.md}), but named and
 * structured to gain further, unrelated preferences over time rather than
 * staying a single-purpose "text font" dialog. The font choice is either
 * the computer's own native font (the default, {@link
 * Texts#OptionsDialog_NativeFont} in {@link #fontComboBox}) or any
 * installed mono-spaced system font (see {@link
 * PlainTextFont#getAvailableFontFamilyNames}), at a chosen point size
 * ({@link #sizeSpinner}, disabled while the native font is selected - a
 * point size has no meaning for it). {@link #zoomSpinner} is a separate,
 * always-enabled control: how large the native font's own 8-pixel-tall
 * glyph cell renders, since {@link MemoryInspectorPanel}'s grid always
 * uses the native font regardless of {@link #fontComboBox}'s choice - see
 * {@link Options#NATIVE_FONT_ZOOM_KEY}.
 * <p>
 * {@link #preview} shows a fixed sample string in the currently selected
 * font/size, updated live as any control changes, via the exact same
 * {@link TextFont} a real panel would end up using - so a distorted or
 * missing glyph in an installed font is visible before committing to it.
 * Persisting the choice and calling {@code Dis6502.updateFonts()} is the
 * caller's job, matching every other settings dialog in this project -
 * {@link #show} only reports whether the user clicked OK; {@link
 * #getSelectedFontFamilyName}/{@link #getSelectedPointSize}/{@link
 * #getSelectedZoom} then give the result, the same "boolean {@code show},
 * separate getters" shape {@link DiskImageSectorsDialog}/{@link
 * RawFileDialog} already use.
 * <p>
 * {@link #restoreDefaultsButton} only resets this dialog's own controls
 * back to their coded defaults (native font, {@link
 * Options#TEXT_FONT_SIZE_DEFAULT}, {@link Options#NATIVE_FONT_ZOOM_DEFAULT}) -
 * a pending edit like any other control here, not an immediate action: it
 * takes a click on {@link #okButton} to actually delete the persisted
 * preferences, via {@link #isRestoreDefaultsRequested}, exactly like every
 * other field only takes effect once the caller sees {@link #show} return
 * {@code true}. Cancel/close discards it, same as any other edit.
 *
 * @author Peter Dell
 */
public final class OptionsDialog extends JDialog {

	private static final long serialVersionUID = 1L;

	private static final String SAMPLE_TEXT = "ABCXYZ 0123 abcxyz";
	private static final int MIN_POINT_SIZE = 6;
	private static final int MAX_POINT_SIZE = 72;
	private static final int MIN_ZOOM = 1;
	private static final int MAX_ZOOM = 8;

	private final JComboBox<String> fontComboBox = new JComboBox<>();
	private final JSpinner sizeSpinner = new JSpinner(
			new SpinnerNumberModel(Options.TEXT_FONT_SIZE_DEFAULT, MIN_POINT_SIZE, MAX_POINT_SIZE, 1));
	private final JSpinner zoomSpinner = new JSpinner(
			new SpinnerNumberModel(Options.NATIVE_FONT_ZOOM_DEFAULT, MIN_ZOOM, MAX_ZOOM, 1));
	private final JPanel preview = new JPanel() {
		private static final long serialVersionUID = 1L;

		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			selectedTextFont().drawText((Graphics2D) g, SAMPLE_TEXT, Color.BLACK, 4, 4);
		}
	};
	private final JButton okButton = ElementFactory.createButton(Actions.ButtonBar_OK, true);
	// Fully qualified: com.wudsn.tools.base.Actions is already imported as "Actions" for ButtonBar_OK/Cancel above.
	private final JButton restoreDefaultsButton = ElementFactory.createButton(
			com.wudsn.tools.dis6502.Actions.OptionsDialog_RestoreDefaults, true);

	private ComputerSystemType computerSystemType;
	private boolean doubleHeight;
	private boolean confirmed;
	private boolean restoreDefaultsRequested;
	private String selectedFontFamilyName = "";
	private int selectedPointSize;
	private int selectedZoom;

	public OptionsDialog(Frame owner) {
		super(owner, true);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setTitle(Texts.OptionsDialog_Title);

		fontComboBox.addActionListener(e -> {
			sizeSpinner.setEnabled(!isNativeFontSelected());
			preview.repaint();
		});
		sizeSpinner.addChangeListener(e -> preview.repaint());
		zoomSpinner.addChangeListener(e -> preview.repaint());
		restoreDefaultsButton.addActionListener(e -> performRestoreDefaults());

		preview.setBackground(Color.WHITE);
		preview.setPreferredSize(new Dimension(320, 40));

		JPanel formPanel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(4, 4, 4, 4);
		c.anchor = GridBagConstraints.WEST;
		c.gridx = 0;
		c.gridy = 0;
		formPanel.add(ElementFactory.createLabel(DataTypes.OptionsDialog_Font, fontComboBox), c);
		c.gridx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		formPanel.add(fontComboBox, c);

		c.gridx = 0;
		c.gridy = 1;
		c.fill = GridBagConstraints.NONE;
		c.weightx = 0;
		formPanel.add(ElementFactory.createLabel(DataTypes.OptionsDialog_Size, sizeSpinner), c);
		c.gridx = 1;
		formPanel.add(sizeSpinner, c);

		c.gridx = 0;
		c.gridy = 2;
		formPanel.add(ElementFactory.createLabel(DataTypes.OptionsDialog_NativeFontZoom, zoomSpinner), c);
		c.gridx = 1;
		formPanel.add(zoomSpinner, c);

		okButton.addActionListener(e -> performOK());
		JButton cancelButton = ElementFactory.createButton(Actions.ButtonBar_Cancel, true);
		cancelButton.addActionListener(e -> setVisible(false));

		JPanel restorePanel = new JPanel();
		restorePanel.add(restoreDefaultsButton);
		JPanel okCancelPanel = new JPanel();
		okCancelPanel.add(okButton);
		okCancelPanel.add(cancelButton);
		JPanel buttonPanel = new JPanel(new BorderLayout());
		buttonPanel.add(restorePanel, BorderLayout.WEST);
		buttonPanel.add(okCancelPanel, BorderLayout.EAST);

		getContentPane().setLayout(new BorderLayout(4, 4));
		getContentPane().add(formPanel, BorderLayout.NORTH);
		getContentPane().add(preview, BorderLayout.CENTER);
		getContentPane().add(buttonPanel, BorderLayout.SOUTH);

		getRootPane().setDefaultButton(okButton);
		ElementUtilities.closeOnEscape(this, cancelButton::doClick);
	}

	private boolean isNativeFontSelected() {
		Object selected = fontComboBox.getSelectedItem();
		return selected == null || Texts.OptionsDialog_NativeFont.equals(selected);
	}

	private TextFont selectedTextFont() {
		if (isNativeFontSelected()) {
			return ComputerFont.get(computerSystemType, doubleHeight, (Integer) zoomSpinner.getValue());
		}
		return PlainTextFont.get((String) fontComboBox.getSelectedItem(), (Integer) sizeSpinner.getValue(), false);
	}

	private void performOK() {
		selectedFontFamilyName = isNativeFontSelected() ? "" : (String) fontComboBox.getSelectedItem();
		selectedPointSize = (Integer) sizeSpinner.getValue();
		selectedZoom = (Integer) zoomSpinner.getValue();
		confirmed = true;
		setVisible(false);
	}

	private void performRestoreDefaults() {
		restoreDefaultsRequested = true;
		fontComboBox.setSelectedItem(Texts.OptionsDialog_NativeFont);
		sizeSpinner.setValue(Options.TEXT_FONT_SIZE_DEFAULT);
		sizeSpinner.setEnabled(false);
		zoomSpinner.setValue(Options.NATIVE_FONT_ZOOM_DEFAULT);
		preview.repaint();
	}

	/** {@code ""} for the native font, a real installed family name otherwise - only meaningful after {@link #show} returned {@code true}. */
	public String getSelectedFontFamilyName() {
		return selectedFontFamilyName;
	}

	/** Only meaningful after {@link #show} returned {@code true}; ignored by the native font, which follows {@link #getSelectedZoom} instead. */
	public int getSelectedPointSize() {
		return selectedPointSize;
	}

	/**
	 * How large the native font's glyph cell renders - only meaningful after
	 * {@link #show} returned {@code true}. Applies regardless of {@link
	 * #getSelectedFontFamilyName}, since the memory inspector's grid always
	 * uses the native font.
	 */
	public int getSelectedZoom() {
		return selectedZoom;
	}

	/**
	 * Whether {@link #restoreDefaultsButton} was clicked before OK - only
	 * meaningful after {@link #show} returned {@code true}. When {@code
	 * true}, the caller should delete its own persisted preferences (so a
	 * coded default added later also takes effect) rather than write
	 * {@link #getSelectedFontFamilyName}/{@link #getSelectedPointSize}/
	 * {@link #getSelectedZoom}'s values, even though those already equal the
	 * current coded defaults either way.
	 */
	public boolean isRestoreDefaultsRequested() {
		return restoreDefaultsRequested;
	}

	/**
	 * Opens the dialog pre-selecting {@code currentFontFamilyName} ({@code
	 * ""} = native font), {@code currentPointSize} and {@code currentZoom};
	 * {@code computerSystemType}/{@code doubleHeight} let {@link #preview}
	 * build the exact native {@link ComputerFont} a real panel would use,
	 * at whatever zoom is currently dialed in, while the native entry is
	 * selected.
	 * <p>
	 * Returns whether the user clicked OK - {@link #getSelectedFontFamilyName}/
	 * {@link #getSelectedPointSize}/{@link #getSelectedZoom}/{@link
	 * #isRestoreDefaultsRequested} then give the result; on Cancel/close,
	 * none of them are updated, so the caller's own already-current values
	 * remain correct.
	 */
	public boolean show(String currentFontFamilyName, int currentPointSize, int currentZoom, ComputerSystemType computerSystemType,
			boolean doubleHeight) {
		this.computerSystemType = computerSystemType;
		this.doubleHeight = doubleHeight;

		fontComboBox.removeAllItems();
		fontComboBox.addItem(Texts.OptionsDialog_NativeFont);
		for (String familyName : PlainTextFont.getAvailableFontFamilyNames()) {
			fontComboBox.addItem(familyName);
		}
		fontComboBox.setSelectedItem(currentFontFamilyName.isEmpty() ? Texts.OptionsDialog_NativeFont : currentFontFamilyName);
		sizeSpinner.setValue(Math.max(MIN_POINT_SIZE, Math.min(MAX_POINT_SIZE, currentPointSize)));
		sizeSpinner.setEnabled(!isNativeFontSelected());
		zoomSpinner.setValue(Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, currentZoom)));

		// Packed here, not in the constructor: the combo box is still empty at
		// construction time, so packing then sized the dialog too narrow to
		// show the populated item text once items were added afterwards.
		pack();
		setLocationRelativeTo(getOwner());

		confirmed = false;
		restoreDefaultsRequested = false;
		setVisible(true); // Blocks until disposed/hidden - this is a modal dialog.

		return confirmed;
	}
}
