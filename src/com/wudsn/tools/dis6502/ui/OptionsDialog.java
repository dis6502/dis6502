/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.function.IntSupplier;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import com.wudsn.tools.base.gui.ElementFactory;
import com.wudsn.tools.base.gui.ModalDialog;
import com.wudsn.tools.dis6502.Actions;
import com.wudsn.tools.dis6502.DataTypes;
import com.wudsn.tools.dis6502.Options;
import com.wudsn.tools.dis6502.Texts;
import com.wudsn.tools.dis6502.model.CharacterSet;

/**
 * A general application-options dialog, named and structured to gain
 * further, unrelated preferences over time. Today it has two independent
 * groups, each with its own live preview drawn through the exact {@link
 * TextFont} a real panel ends up using:
 * <ul>
 * <li>Text Font: an installed mono-spaced family (see {@link
 * PlainTextFont#getAvailableFontFamilyNames}) and point size, for every
 * panel except the memory inspector's grid.</li>
 * <li>Memory Inspector Font: the pixel height of the computer's native
 * {@link ComputerFont}, which the memory inspector's grid always uses -
 * stepped in whole multiples of {@link ComputerFont#NATIVE_HEIGHT}.</li>
 * </ul>
 * Persisting the choice and calling {@code Dis6502.updateFonts()} is the
 * caller's job, matching every other settings dialog in this project -
 * {@link #show} only reports whether the user clicked OK; the {@code
 * getSelected...} getters then give the result.
 * <p>
 * {@link #restoreDefaultsButton} only resets this dialog's own controls to
 * the {@link Options} defaults - a pending edit like any other control
 * here: it takes a click on OK to actually delete the
 * persisted preferences, via {@link #isRestoreDefaultsRequested}.
 * Cancel/close discards it, same as any other edit.
 *
 * @author Peter Dell
 */
public final class OptionsDialog extends ModalDialog {

	private static final long serialVersionUID = 1L;

	private static final String TEXT_FONT_SAMPLE = "ABCXYZ 0123 abcxyz";
	private static final String NATIVE_FONT_SAMPLE = "0000: 48 65 6C 6C 6F  Hello";
	private static final int MIN_TEXT_FONT_SIZE = 6;
	private static final int MAX_TEXT_FONT_SIZE = 72;
	private static final int MAX_NATIVE_FONT_SIZE = 8 * ComputerFont.NATIVE_HEIGHT;

	private final JComboBox<String> fontComboBox = new JComboBox<>();
	private final JSpinner textFontSizeSpinner = new JSpinner(
			new SpinnerNumberModel(Options.TEXT_FONT_SIZE_DEFAULT, MIN_TEXT_FONT_SIZE, MAX_TEXT_FONT_SIZE, 1));
	private final JSpinner nativeFontSizeSpinner = new JSpinner(new SpinnerNumberModel(Options.NATIVE_FONT_SIZE_DEFAULT,
			ComputerFont.NATIVE_HEIGHT, MAX_NATIVE_FONT_SIZE, ComputerFont.NATIVE_HEIGHT));
	private final SamplePanel textFontPreview = new SamplePanel(TEXT_FONT_SAMPLE, () -> selectedTextFont().getGlyphWidth(),
			() -> selectedTextFont().getGlyphHeight(), (g2, text, x, y) -> selectedTextFont().drawText(g2, text, Color.BLACK, x, y));
	private final SamplePanel nativeFontPreview = new SamplePanel(NATIVE_FONT_SAMPLE, () -> selectedNativeFont().getGlyphWidth(),
			() -> selectedNativeFont().getGlyphHeight(),
			(g2, text, x, y) -> selectedNativeFont().drawText(g2, text, Color.BLACK, x, y));
	private final JButton restoreDefaultsButton = ElementFactory.createButton(
			Actions.OptionsDialog_RestoreDefaults, true);

	private CharacterSet characterSet;
	private boolean restoreDefaultsRequested;
	private String selectedFontFamilyName = "";
	private int selectedTextFontSize;
	private int selectedNativeFontSize;

	public OptionsDialog(Frame owner) {
		super(owner, Texts.OptionsDialog_Title);

		fontComboBox.addActionListener(e -> textFontPreview.refresh());
		textFontSizeSpinner.addChangeListener(e -> textFontPreview.refresh());
		nativeFontSizeSpinner.addChangeListener(e -> nativeFontPreview.refresh());
		restoreDefaultsButton.addActionListener(e -> performRestoreDefaults());

		JPanel textFontPanel = createGroupPanel(Texts.OptionsDialog_TextFontGroupTitle);
		addRow(textFontPanel, 0, ElementFactory.createLabel(DataTypes.OptionsDialog_Font, fontComboBox), fontComboBox, true);
		addRow(textFontPanel, 1, ElementFactory.createLabel(DataTypes.OptionsDialog_Size, textFontSizeSpinner),
				textFontSizeSpinner, false);
		addPreview(textFontPanel, 2, textFontPreview);

		JPanel nativeFontPanel = createGroupPanel(Texts.OptionsDialog_NativeFontGroupTitle);
		addRow(nativeFontPanel, 0, ElementFactory.createLabel(DataTypes.OptionsDialog_NativeFontSize, nativeFontSizeSpinner),
				nativeFontSizeSpinner, false);
		addPreview(nativeFontPanel, 1, nativeFontPreview);

		JPanel groupsPanel = new JPanel();
		groupsPanel.setLayout(new BoxLayout(groupsPanel, BoxLayout.Y_AXIS));
		groupsPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 0, 8));
		groupsPanel.add(textFontPanel);
		groupsPanel.add(nativeFontPanel);

		addButtonBarButton(restoreDefaultsButton);
		getContentPane().add(groupsPanel, BorderLayout.CENTER);
	}

	private static JPanel createGroupPanel(String title) {
		JPanel panel = new JPanel(new GridBagLayout());
		panel.setBorder(BorderFactory.createTitledBorder(title));
		return panel;
	}

	private static void addRow(JPanel panel, int row, Component label, Component field, boolean fill) {
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(4, 4, 4, 4);
		c.anchor = GridBagConstraints.WEST;
		c.gridx = 0;
		c.gridy = row;
		panel.add(label, c);
		c.gridx = 1;
		c.weightx = 1;
		c.fill = fill ? GridBagConstraints.HORIZONTAL : GridBagConstraints.NONE;
		panel.add(field, c);
	}

	private static void addPreview(JPanel panel, int row, SamplePanel preview) {
		JScrollPane scrollPane = new JScrollPane(preview);
		scrollPane.setPreferredSize(new Dimension(400, 72));
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(4, 4, 4, 4);
		c.gridx = 0;
		c.gridy = row;
		c.gridwidth = 2;
		c.weightx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		panel.add(scrollPane, c);
	}

	private String selectedFontFamilyName() {
		Object selected = fontComboBox.getSelectedItem();
		return selected == null ? Options.TEXT_FONT_FAMILY_DEFAULT : (String) selected;
	}

	private TextFont selectedTextFont() {
		return PlainTextFont.get(selectedFontFamilyName(), (Integer) textFontSizeSpinner.getValue());
	}

	private ComputerFont selectedNativeFont() {
		return ComputerFont.get(characterSet, (Integer) nativeFontSizeSpinner.getValue());
	}

	@Override
	protected boolean validateOK() {
		selectedFontFamilyName = selectedFontFamilyName();
		selectedTextFontSize = (Integer) textFontSizeSpinner.getValue();
		selectedNativeFontSize = (Integer) nativeFontSizeSpinner.getValue();
		return true;
	}

	private void performRestoreDefaults() {
		restoreDefaultsRequested = true;
		fontComboBox.setSelectedItem(Options.TEXT_FONT_FAMILY_DEFAULT);
		textFontSizeSpinner.setValue(Options.TEXT_FONT_SIZE_DEFAULT);
		nativeFontSizeSpinner.setValue(Options.NATIVE_FONT_SIZE_DEFAULT);
	}

	/** An installed font family name - only meaningful after {@link #show} returned {@code true}. */
	public String getSelectedFontFamilyName() {
		return selectedFontFamilyName;
	}

	/** Only meaningful after {@link #show} returned {@code true}. */
	public int getSelectedTextFontSize() {
		return selectedTextFontSize;
	}

	/** The native font's pixel height - only meaningful after {@link #show} returned {@code true}. */
	public int getSelectedNativeFontSize() {
		return selectedNativeFontSize;
	}

	/**
	 * Whether {@link #restoreDefaultsButton} was clicked before OK - only
	 * meaningful after {@link #show} returned {@code true}. When {@code
	 * true}, the caller should delete its own persisted preferences (so a
	 * coded default added later also takes effect) rather than write the
	 * selected values, even though those already equal the current coded
	 * defaults either way.
	 */
	public boolean isRestoreDefaultsRequested() {
		return restoreDefaultsRequested;
	}

	/**
	 * Opens the dialog pre-selecting the current values; {@code
	 * characterSet} lets {@link #nativeFontPreview} build the exact {@link
	 * ComputerFont} the memory inspector uses. Returns whether the user
	 * clicked OK; on Cancel/close, none of the getters are updated.
	 */
	public boolean show(String currentFontFamilyName, int currentTextFontSize, int currentNativeFontSize,
			CharacterSet characterSet) {
		this.characterSet = characterSet;

		fontComboBox.removeAllItems();
		for (String familyName : PlainTextFont.getAvailableFontFamilyNames()) {
			fontComboBox.addItem(familyName);
		}
		fontComboBox.setSelectedItem(currentFontFamilyName);
		textFontSizeSpinner.setValue(Math.max(MIN_TEXT_FONT_SIZE, Math.min(MAX_TEXT_FONT_SIZE, currentTextFontSize)));
		nativeFontSizeSpinner.setValue(Math.max(ComputerFont.NATIVE_HEIGHT, Math.min(MAX_NATIVE_FONT_SIZE, currentNativeFontSize)));

		restoreDefaultsRequested = false;
		showModal(fontComboBox);
		return okPressed;
	}

	private interface SamplePainter {
		void drawText(Graphics2D g2, String text, int x, int y);
	}

	/** Draws a fixed sample text in the currently selected font, sized to it so the surrounding scroll pane can scroll a large one. */
	private static final class SamplePanel extends JPanel {

		private static final long serialVersionUID = 1L;
		private static final int MARGIN = 4;

		private final String sampleText;
		private final transient IntSupplier glyphWidth;
		private final transient IntSupplier glyphHeight;
		private final transient SamplePainter painter;

		SamplePanel(String sampleText, IntSupplier glyphWidth, IntSupplier glyphHeight, SamplePainter painter) {
			this.sampleText = sampleText;
			this.glyphWidth = glyphWidth;
			this.glyphHeight = glyphHeight;
			this.painter = painter;
			setBackground(Color.WHITE);
		}

		void refresh() {
			revalidate();
			repaint();
		}

		@Override
		public Dimension getPreferredSize() {
			return new Dimension(glyphWidth.getAsInt() * sampleText.length() + 2 * MARGIN,
					glyphHeight.getAsInt() + 2 * MARGIN);
		}

		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			painter.drawText((Graphics2D) g, sampleText, MARGIN, MARGIN);
		}
	}
}
