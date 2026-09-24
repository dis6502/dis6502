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
import com.wudsn.tools.dis6502.Texts;

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
 * ({@link #sizeSpinner}, disabled while the native font is selected -
 * that font's own size instead follows the existing "Double Font Height"
 * setting, unrelated to this dialog).
 * <p>
 * {@link #preview} shows a fixed sample string in the currently selected
 * font/size, updated live as either control changes, via the exact same
 * {@link TextFont} a real panel would end up using - so a distorted or
 * missing glyph in an installed font is visible before committing to it.
 * Persisting the choice and calling {@code Dis6502.updateFonts()} is the
 * caller's job, matching every other settings dialog in this project -
 * {@link #show} only reports whether the user clicked OK; {@link
 * #getSelectedFontFamilyName}/{@link #getSelectedPointSize} then give the
 * result, the same "boolean {@code show}, separate getters" shape {@link
 * DiskImageSectorsDialog}/{@link RawFileDialog} already use.
 * <p>
 * {@link #restoreDefaultsButton} applies immediately, independent of
 * OK/Cancel: it runs the caller-supplied {@code restoreDefaultsAction}
 * (which deletes every persisted preference this dialog manages, so the
 * coded defaults apply from then on and take effect right away, even
 * while this dialog stays open), then resets every control here back to
 * its own coded default so the dialog's own display matches. OK afterward
 * simply re-persists that already-applied default state; Cancel leaves
 * the already-applied restore in place, since "Restore Defaults" is a
 * direct action, not a pending edit gated by OK/Cancel like the other
 * controls.
 *
 * @author Peter Dell
 */
public final class OptionsDialog extends JDialog {

	private static final long serialVersionUID = 1L;

	private static final String SAMPLE_TEXT = "ABCXYZ 0123 abcxyz";
	private static final int MIN_POINT_SIZE = 6;
	private static final int MAX_POINT_SIZE = 72;

	/** The coded default point size for a user-chosen font - the single source of truth {@code Dis6502} also reads. */
	public static final int DEFAULT_POINT_SIZE = 16;

	private final JComboBox<String> fontComboBox = new JComboBox<>();
	private final JSpinner sizeSpinner = new JSpinner(new SpinnerNumberModel(DEFAULT_POINT_SIZE, MIN_POINT_SIZE, MAX_POINT_SIZE, 1));
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

	private ComputerFont nativeFont;
	private Runnable restoreDefaultsAction;
	private boolean confirmed;
	private String selectedFontFamilyName = "";
	private int selectedPointSize;

	public OptionsDialog(Frame owner) {
		super(owner, true);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setTitle(Texts.OptionsDialog_Title);

		fontComboBox.addActionListener(e -> {
			sizeSpinner.setEnabled(!isNativeFontSelected());
			preview.repaint();
		});
		sizeSpinner.addChangeListener(e -> preview.repaint());
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
			return nativeFont;
		}
		return PlainTextFont.get((String) fontComboBox.getSelectedItem(), (Integer) sizeSpinner.getValue(), false);
	}

	private void performOK() {
		selectedFontFamilyName = isNativeFontSelected() ? "" : (String) fontComboBox.getSelectedItem();
		selectedPointSize = (Integer) sizeSpinner.getValue();
		confirmed = true;
		setVisible(false);
	}

	private void performRestoreDefaults() {
		if (restoreDefaultsAction != null) {
			restoreDefaultsAction.run();
		}
		fontComboBox.setSelectedItem(Texts.OptionsDialog_NativeFont);
		sizeSpinner.setValue(DEFAULT_POINT_SIZE);
		sizeSpinner.setEnabled(false);
		preview.repaint();
	}

	/** {@code ""} for the native font, a real installed family name otherwise - only meaningful after {@link #show} returned {@code true}. */
	public String getSelectedFontFamilyName() {
		return selectedFontFamilyName;
	}

	/** Only meaningful after {@link #show} returned {@code true}; ignored by the native font, which follows "Double Font Height" instead. */
	public int getSelectedPointSize() {
		return selectedPointSize;
	}

	/**
	 * Opens the dialog pre-selecting {@code currentFontFamilyName} ({@code
	 * ""} = native font) and {@code currentPointSize}; {@code nativeFont}
	 * drives {@link #preview} while the native entry is selected. {@code
	 * restoreDefaultsAction} is the caller's own "delete every persisted
	 * preference this dialog manages" logic, run immediately when {@link
	 * #restoreDefaultsButton} is clicked (see its own javadoc for why that
	 * is not gated by OK/Cancel).
	 * <p>
	 * Returns whether the user clicked OK - {@link #getSelectedFontFamilyName}/
	 * {@link #getSelectedPointSize} then give the result; on Cancel/close,
	 * neither getter is updated, so the caller's own already-current values
	 * remain correct (unless Restore Defaults was clicked first, which
	 * already applied and persisted its own defaults independent of this
	 * return value).
	 */
	public boolean show(String currentFontFamilyName, int currentPointSize, ComputerFont nativeFont, Runnable restoreDefaultsAction) {
		this.nativeFont = nativeFont;
		this.restoreDefaultsAction = restoreDefaultsAction;

		fontComboBox.removeAllItems();
		fontComboBox.addItem(Texts.OptionsDialog_NativeFont);
		for (String familyName : PlainTextFont.getAvailableFontFamilyNames()) {
			fontComboBox.addItem(familyName);
		}
		fontComboBox.setSelectedItem(currentFontFamilyName.isEmpty() ? Texts.OptionsDialog_NativeFont : currentFontFamilyName);
		sizeSpinner.setValue(Math.max(MIN_POINT_SIZE, Math.min(MAX_POINT_SIZE, currentPointSize)));
		sizeSpinner.setEnabled(!isNativeFontSelected());

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
