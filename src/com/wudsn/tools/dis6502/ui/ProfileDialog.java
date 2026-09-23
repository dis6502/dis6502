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
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import com.wudsn.tools.base.Actions;
import com.wudsn.tools.base.gui.ElementFactory;
import com.wudsn.tools.base.gui.ValueSetField;
import com.wudsn.tools.base.repository.DataType;
import com.wudsn.tools.dis6502.DataTypes;
import com.wudsn.tools.dis6502.Messages;
import com.wudsn.tools.dis6502.Texts;
import com.wudsn.tools.dis6502.model.Encoding;
import com.wudsn.tools.dis6502.model.FileType;
import com.wudsn.tools.dis6502.model.Profile;
import com.wudsn.tools.dis6502.model.ProfileLogic;
import com.wudsn.tools.dis6502.model.system.ComputerSystemType;

/**
 * A dialog for editing a {@link Profile}'s disassembly output syntax and
 * layout settings.
 * <p>
 * Reorganized into three titled sections ("General", "Directive Syntax",
 * "Disassembly Listing") stacked with plain Swing layout. Load/Save
 * button handlers fold a thin {@link ProfileLogic} plus file-chooser
 * wrapper directly into this class, rather than a separate class.
 * <p>
 * Enabled state is recomputed against a private scratch {@link Profile}
 * ({@link #workingProfile}), not the real one passed to {@link #show}:
 * recomputing directly against the real object would leave it partially
 * mutated if the user then clicked Cancel, since only the working copy
 * gets discarded. The real object is written to only when the user clicks
 * OK, so Cancel is always a true no-op - the same "decouple the edit
 * session from the real model until commit" pattern used by {@link
 * EquateDialog}.
 *
 * @author Peter Dell
 */
public final class ProfileDialog extends JDialog {

	private static final long serialVersionUID = 1L;


	// General.
	private final JTextField commentField = new JTextField(4);
	private final JTextField hexNotationField = new JTextField(4);
	private final JCheckBox illegalInstructionCheckBox = ElementFactory.createCheckBox(DataTypes.Profile_UseIllegalOpcodes);
	private final JCheckBox useHexCheckBox = ElementFactory.createCheckBox(DataTypes.Profile_UseHexNotation);
	private final JCheckBox alignInstructionCheckBox = ElementFactory.createCheckBox(DataTypes.Profile_AlignInstructions);
	private final JCheckBox lineNumberingCheckBox = ElementFactory.createCheckBox(DataTypes.Profile_UseLineNumbers);
	private final JCheckBox showLowerCaseCheckBox = ElementFactory.createCheckBox(DataTypes.Profile_ShowLowerCaseInstructions);
	private final JCheckBox showImpliciteCheckBox = ElementFactory.createCheckBox(DataTypes.Profile_ShowAInAccumulatorMode);
	private final JCheckBox showColonCheckBox = ElementFactory.createCheckBox(DataTypes.Profile_ShowColonAfterLabel);
	private final JCheckBox displayOpcodesCheckBox = ElementFactory.createCheckBox(DataTypes.Profile_ShowOpcodeAsComment);
	private final JCheckBox showByte0CheckBox = ElementFactory.createCheckBox(DataTypes.Profile_ShowBRKAsByte0);
	private final JCheckBox showZPAsByteCheckBox = ElementFactory.createCheckBox(DataTypes.Profile_ShowZPAbsoluteAsByte);
	private final JTextField forceAbsoluteField = new JTextField(4);
	private final JCheckBox nonASCIIAsBytesCheckBox = ElementFactory.createCheckBox(DataTypes.Profile_ShowNonASCIIChararactersAsBytes);
	private final JTextField bytePerLineField = new JTextField(3);
	private final JTextField wordPerLineField = new JTextField(3);
	private final JTextField charPerLineField = new JTextField(3);
	private final JTextField quoteForASCIIStringsField = new JTextField(3);

	// Directive Syntax.
	private final JCheckBox numOnlyInByteCheckBox = ElementFactory.createCheckBox(DataTypes.Profile_DirectiveBYTEOnlyNumbersAllowed);
	private final JTextField byteSyntaxField = new JTextField(6);
	private final JTextField byteSeparatorField = new JTextField(3);
	private final JCheckBox wordAllowedCheckBox = ElementFactory.createCheckBox(DataTypes.Profile_DirectiveWORDAllowed);
	private final JTextField wordSyntaxField = new JTextField(6);
	private final JCheckBox sByteAllowedCheckBox = ElementFactory.createCheckBox(DataTypes.Profile_DirectiveSBYTEAllowed);
	private final JTextField sByteSyntaxField = new JTextField(6);
	private final JTextField orgSyntaxField = new JTextField(6);
	private final JTextField lowHeadSyntaxField = new JTextField(6);
	private final JTextField lowTailSyntaxField = new JTextField(6);
	private final JTextField equSyntaxField = new JTextField(6);
	private final JTextField highHeadSyntaxField = new JTextField(6);
	private final JTextField highTailSyntaxField = new JTextField(6);
	private final JTextField endSyntaxField = new JTextField(8);
	private final JCheckBox endFilenameCheckBox = ElementFactory.createCheckBox(DataTypes.Profile_DirectiveENDNeedsFilename);
	private final JTextField endTailField = new JTextField(8);
	private final JCheckBox dsAllowedCheckBox = ElementFactory.createCheckBox(DataTypes.Profile_DirectiveDSAllowed);
	private final JTextField dsSyntaxField = new JTextField(6);

	// Disassembly Listing.
	private final ValueSetField<Encoding> outputEncodingField = new ValueSetField<Encoding>(Encoding.getOutputValues());
	private final JCheckBox removeUnusedLabelsCheckBox = ElementFactory.createCheckBox(DataTypes.Profile_OmitUnreferencedSystemLabels);
	private final JCheckBox includeAllowedCheckBox = ElementFactory.createCheckBox(DataTypes.Profile_DirectiveINCLUDEAllowed);
	private final JTextField includeHeadField = new JTextField(8);
	private final JTextField includeTailField = new JTextField(8);
	private final JRadioButton radioIncludeOneFile = ElementFactory.createRadioButton(DataTypes.Profile_DirectiveINCLUDEAllEquatesInOneIncludeFile);
	private final JRadioButton radioIncludeAllFiles = ElementFactory.createRadioButton(DataTypes.Profile_DirectiveINCLUDEAllIncludesInMainFile);
	private final JRadioButton radioIncludeNextFile = ElementFactory.createRadioButton(DataTypes.Profile_DirectiveINCLUDEEachFileIncludesNextFile);
	private final JTextField maxIncludeLinesField = new JTextField(6);

	private final ProfileLogic profileLogic;
	private final FileChoosers fileChoosers;
	private final Profile workingProfile = new Profile();
	private ComputerSystemType computerSystemType;
	private File lastProfileFile;
	private boolean confirmed;

	public ProfileDialog(Frame owner, ProfileLogic profileLogic, FileChoosers fileChoosers) {
		super(owner, true);
		this.profileLogic = profileLogic;
		this.fileChoosers = fileChoosers;
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setTitle(Texts.ProfileDialog_Title);

		ButtonGroup includeFileGroup = new ButtonGroup();
		includeFileGroup.add(radioIncludeOneFile);
		includeFileGroup.add(radioIncludeAllFiles);
		includeFileGroup.add(radioIncludeNextFile);

		// Recomputes enabled state against workingProfile on every toggle (see class javadoc).
		java.awt.event.ActionListener refreshListener = e -> refresh();
		useHexCheckBox.addActionListener(refreshListener);
		showZPAsByteCheckBox.addActionListener(refreshListener);
		sByteAllowedCheckBox.addActionListener(refreshListener);
		wordAllowedCheckBox.addActionListener(refreshListener);
		dsAllowedCheckBox.addActionListener(refreshListener);
		includeAllowedCheckBox.addActionListener(refreshListener);
		radioIncludeOneFile.addActionListener(refreshListener);
		radioIncludeAllFiles.addActionListener(refreshListener);
		radioIncludeNextFile.addActionListener(refreshListener);

		JPanel rightPanel = new JPanel(new GridBagLayout());
		GridBagConstraints rc = gbc(0, 0);
		rc.fill = GridBagConstraints.HORIZONTAL;
		rc.weightx = 1;
		rightPanel.add(createDirectiveSyntaxPanel(), rc);
		rc = gbc(0, 1);
		rc.fill = GridBagConstraints.HORIZONTAL;
		rc.weightx = 1;
		rightPanel.add(createDisassemblyListingPanel(), rc);

		JPanel contentPanel = new JPanel(new GridBagLayout());
		GridBagConstraints cc = gbc(0, 0);
		cc.fill = GridBagConstraints.BOTH;
		cc.weighty = 1;
		contentPanel.add(createGeneralPanel(), cc);
		cc = gbc(1, 0);
		cc.fill = GridBagConstraints.BOTH;
		cc.weighty = 1;
		contentPanel.add(rightPanel, cc);

		JButton loadButton = ElementFactory.createButton(com.wudsn.tools.dis6502.Actions.ProfileDialog_LoadProfile, true);
		loadButton.addActionListener(e -> performLoadProfile());
		JButton saveButton = ElementFactory.createButton(com.wudsn.tools.dis6502.Actions.ProfileDialog_SaveProfile, true);
		saveButton.addActionListener(e -> performSaveProfile());
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
		buttonPanel.add(loadButton);
		buttonPanel.add(saveButton);
		buttonPanel.add(okButton);
		buttonPanel.add(cancelButton);

		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(new JScrollPane(contentPanel), BorderLayout.CENTER);
		getContentPane().add(buttonPanel, BorderLayout.SOUTH);
		// pack() instead of a fixed setSize(...): this dialog's three titled
		// panels (17+7+8 rows) need more than 520px of height to show without
		// scrolling, so a fixed size showed a spurious vertical scrollbar on
		// first open - packing sizes the dialog to its actual content, and it
		// stays resizable (no setResizable(false) here) if the user wants to
		// shrink it afterward, at which point the JScrollPane's scrollbar is
		// the correct, expected behavior.
		pack();
		setLocationRelativeTo(owner);

		getRootPane().setDefaultButton(okButton);
		ElementUtilities.closeOnEscape(this, cancelButton::doClick);
	}

	private JPanel createGeneralPanel() {
		JPanel panel = new JPanel(new GridBagLayout());
		panel.setBorder(BorderFactory.createTitledBorder(Texts.ProfileDialog_GeneralGroupTitle));
		int row = 0;
		addLabeledField(panel, row++, DataTypes.Profile_CommentPrefix, commentField, DataTypes.Profile_HexNotationPrefix, hexNotationField);
		addFullWidth(panel, row++, illegalInstructionCheckBox);
		addFullWidth(panel, row++, useHexCheckBox);
		addFullWidth(panel, row++, alignInstructionCheckBox);
		addFullWidth(panel, row++, lineNumberingCheckBox);
		addFullWidth(panel, row++, showLowerCaseCheckBox);
		addFullWidth(panel, row++, showImpliciteCheckBox);
		addFullWidth(panel, row++, showColonCheckBox);
		addFullWidth(panel, row++, displayOpcodesCheckBox);
		addFullWidth(panel, row++, showByte0CheckBox);
		addFullWidth(panel, row++, showZPAsByteCheckBox);
		addLabeledField(panel, row++, DataTypes.Profile_DirectiveForceAbsolute, forceAbsoluteField);
		addFullWidth(panel, row++, nonASCIIAsBytesCheckBox);
		addLabeledField(panel, row++, DataTypes.Profile_DirectiveBYTENumberOfBytesPerLine, bytePerLineField);
		addLabeledField(panel, row++, DataTypes.Profile_DirectiveWORDNumberOfWordsPerLine, wordPerLineField);
		addLabeledField(panel, row++, DataTypes.Profile_DirectiveBYTENumberOfCharactersPerString, charPerLineField);
		addLabeledField(panel, row++, DataTypes.Profile_QuoteForASCIIStrings, quoteForASCIIStringsField);
		return panel;
	}

	private JPanel createDirectiveSyntaxPanel() {
		JPanel panel = new JPanel(new GridBagLayout());
		panel.setBorder(BorderFactory.createTitledBorder(Texts.ProfileDialog_DirectiveSyntaxGroupTitle));
		int row = 0;
		addLabeledField(panel, row++, DataTypes.Profile_DirectiveBYTE, byteSyntaxField, DataTypes.Profile_DirectiveBYTESeparator, byteSeparatorField);
		addFullWidth(panel, row++, numOnlyInByteCheckBox);
		addLabeledField(panel, row++, wordAllowedCheckBox, wordSyntaxField);
		addLabeledField(panel, row++, sByteAllowedCheckBox, sByteSyntaxField);
		addLabeledField(panel, row++, DataTypes.Profile_DirectiveORG, orgSyntaxField, DataTypes.Profile_DirectiveLOWHead, lowHeadSyntaxField, lowTailSyntaxField);
		addLabeledField(panel, row++, DataTypes.Profile_DirectiveEQU, equSyntaxField, DataTypes.Profile_DirectiveHIGHHead, highHeadSyntaxField, highTailSyntaxField);
		addLabeledField(panel, row++, DataTypes.Profile_DirectiveENDHead, endSyntaxField, endFilenameCheckBox, endTailField);
		addLabeledField(panel, row++, dsAllowedCheckBox, dsSyntaxField);
		return panel;
	}

	private JPanel createDisassemblyListingPanel() {
		JPanel panel = new JPanel(new GridBagLayout());
		panel.setBorder(BorderFactory.createTitledBorder(Texts.ProfileDialog_DisassemblyListingGroupTitle));
		int row = 0;
		addLabeledField(panel, row++, DataTypes.Profile_OutputEncoding, outputEncodingField);
		addFullWidth(panel, row++, removeUnusedLabelsCheckBox);
		addFullWidth(panel, row++, includeAllowedCheckBox);
		addLabeledField(panel, row++, DataTypes.Profile_DirectiveINCLUDEHead, includeHeadField, DataTypes.Profile_DirectiveINCLUDETail, includeTailField);
		addFullWidth(panel, row++, radioIncludeOneFile);
		addFullWidth(panel, row++, radioIncludeAllFiles);
		addFullWidth(panel, row++, radioIncludeNextFile);
		addLabeledField(panel, row++, DataTypes.Profile_DirectiveINCLUDEMaximumNumberOfLinesPerFile, maxIncludeLinesField);
		return panel;
	}

	private static GridBagConstraints gbc(int x, int y) {
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = x;
		c.gridy = y;
		c.insets = new Insets(2, 4, 2, 4);
		c.anchor = GridBagConstraints.WEST;
		return c;
	}

	private static void addFullWidth(JPanel panel, int row, JComponent component) {
		GridBagConstraints c = gbc(0, row);
		c.gridwidth = 4;
		c.fill = GridBagConstraints.HORIZONTAL;
		panel.add(component, c);
	}

	private static void addLabeledField(JPanel panel, int row, DataType dataType, JComponent field) {
		GridBagConstraints lc = gbc(0, row);
		panel.add(ElementFactory.createLabel(dataType, field), lc);
		GridBagConstraints fc = gbc(1, row);
		fc.gridwidth = 3;
		fc.fill = GridBagConstraints.HORIZONTAL;
		fc.weightx = 1;
		panel.add(field, fc);
	}

	private static void addLabeledField(JPanel panel, int row, JComponent labelComponent, JComponent field) {
		GridBagConstraints lc = gbc(0, row);
		lc.gridwidth = 2;
		panel.add(labelComponent, lc);
		GridBagConstraints fc = gbc(2, row);
		fc.gridwidth = 2;
		fc.fill = GridBagConstraints.HORIZONTAL;
		fc.weightx = 1;
		panel.add(field, fc);
	}

	private static void addLabeledField(JPanel panel, int row, DataType dataType1, JComponent field1, DataType dataType2, JComponent field2) {
		GridBagConstraints l1 = gbc(0, row);
		panel.add(ElementFactory.createLabel(dataType1, field1), l1);
		GridBagConstraints f1 = gbc(1, row);
		f1.fill = GridBagConstraints.HORIZONTAL;
		panel.add(field1, f1);
		GridBagConstraints l2 = gbc(2, row);
		panel.add(ElementFactory.createLabel(dataType2, field2), l2);
		GridBagConstraints f2 = gbc(3, row);
		f2.fill = GridBagConstraints.HORIZONTAL;
		f2.weightx = 1;
		panel.add(field2, f2);
	}

	private static void addLabeledField(JPanel panel, int row, DataType dataType1, JComponent field1, DataType dataType2, JComponent field2,
			JComponent field3) {
		addLabeledField(panel, row, dataType1, field1, dataType2, field2);
		GridBagConstraints f3 = gbc(4, row);
		f3.fill = GridBagConstraints.HORIZONTAL;
		panel.add(field3, f3);
	}

	private static void addLabeledField(JPanel panel, int row, DataType dataType1, JComponent field1, JComponent checkBox2, JComponent field2) {
		GridBagConstraints l1 = gbc(0, row);
		panel.add(ElementFactory.createLabel(dataType1, field1), l1);
		GridBagConstraints f1 = gbc(1, row);
		f1.fill = GridBagConstraints.HORIZONTAL;
		panel.add(field1, f1);
		GridBagConstraints l2 = gbc(2, row);
		panel.add(checkBox2, l2);
		GridBagConstraints f2 = gbc(3, row);
		f2.fill = GridBagConstraints.HORIZONTAL;
		f2.weightx = 1;
		panel.add(field2, f2);
	}

	/** Recomputes enabled state after a checkbox/radio-button toggle, without touching the real profile. */
	private void refresh() {
		getDialogValues(workingProfile);
		setDialogValues(workingProfile);
	}

	/** Populates every field from {@code profile}. */
	private void setDialogValues(Profile profile) {
		commentField.setText(profile.commentPrefix);

		hexNotationField.setEnabled(profile.useHexNotation);
		hexNotationField.setText(profile.hexNotationPrefix);

		illegalInstructionCheckBox.setSelected(profile.useIllegalOpcodes);
		useHexCheckBox.setSelected(profile.useHexNotation);
		alignInstructionCheckBox.setSelected(profile.alignInstructions);
		lineNumberingCheckBox.setSelected(profile.useLineNumbers);

		showLowerCaseCheckBox.setSelected(profile.showLowerCaseInstructions);
		showImpliciteCheckBox.setSelected(profile.showAInAccumulatorMode);
		showColonCheckBox.setSelected(profile.showColonAfterLabel);
		displayOpcodesCheckBox.setSelected(profile.showOpcodeAsComment);

		showByte0CheckBox.setSelected(profile.showBRKAsByte0);
		showZPAsByteCheckBox.setSelected(profile.showZPAbsoluteAsByte);
		forceAbsoluteField.setEnabled(!profile.showZPAbsoluteAsByte);
		forceAbsoluteField.setText(profile.directiveForceAbsolute);
		nonASCIIAsBytesCheckBox.setSelected(profile.showNonASCIIChararactersAsBytes);

		bytePerLineField.setText(String.valueOf(profile.directiveBYTENumberOfBytesPerLine));
		wordPerLineField.setText(String.valueOf(profile.directiveWORDNumberOfWordsPerLine));
		charPerLineField.setText(String.valueOf(profile.directiveBYTENumberOfCharactersPerString));
		quoteForASCIIStringsField.setText(profile.quoteForASCIIStrings);

		orgSyntaxField.setText(profile.directiveORG);
		equSyntaxField.setText(profile.directiveEQU);
		endSyntaxField.setText(profile.directiveENDHead);
		endFilenameCheckBox.setSelected(profile.directiveENDNeedsFilename);
		endTailField.setText(profile.directiveENDTail);
		lowHeadSyntaxField.setText(profile.directiveLOWHead);
		lowTailSyntaxField.setText(profile.directiveLOWTail);
		highHeadSyntaxField.setText(profile.directiveHIGHHead);
		highTailSyntaxField.setText(profile.directiveHIGHTail);

		numOnlyInByteCheckBox.setSelected(profile.directiveBYTEOnlyNumbersAllowed);
		byteSyntaxField.setText(profile.directiveBYTE);
		byteSeparatorField.setText(profile.directiveBYTESeparator);
		sByteAllowedCheckBox.setSelected(profile.directiveSBYTEAllowed);
		sByteSyntaxField.setEnabled(profile.directiveSBYTEAllowed);
		sByteSyntaxField.setText(profile.directiveSBYTE);
		wordAllowedCheckBox.setSelected(profile.directiveWORDAllowed);
		wordSyntaxField.setEnabled(profile.directiveWORDAllowed);
		wordSyntaxField.setText(profile.directiveWORD);
		dsAllowedCheckBox.setSelected(profile.directiveDSAllowed);
		dsSyntaxField.setEnabled(profile.directiveDSAllowed);
		dsSyntaxField.setText(profile.directiveDS);

		outputEncodingField.setValue(Encoding.getOutputValues().contains(profile.outputEncoding) ? profile.outputEncoding : Encoding.ASCII);
		removeUnusedLabelsCheckBox.setSelected(profile.omitUnreferencedSystemLabels);
		boolean includeAllowed = profile.directiveINCLUDEAllowed;
		includeAllowedCheckBox.setSelected(includeAllowed);
		includeHeadField.setEnabled(includeAllowed);
		includeHeadField.setText(profile.directiveINCLUDEHead);
		includeTailField.setEnabled(includeAllowed);
		includeTailField.setText(profile.directiveINCLUDETail);
		radioIncludeOneFile.setEnabled(includeAllowed);
		radioIncludeAllFiles.setEnabled(includeAllowed);
		radioIncludeNextFile.setEnabled(includeAllowed);
		if (profile.directiveINCLUDEAllEquatesInOneIncludeFile) {
			radioIncludeOneFile.setSelected(true);
		} else if (profile.directiveINCLUDEAllIncludesInMainFile) {
			radioIncludeAllFiles.setSelected(true);
		} else {
			radioIncludeNextFile.setSelected(true);
		}
		maxIncludeLinesField.setEnabled(includeAllowed && !profile.directiveINCLUDEAllEquatesInOneIncludeFile);
		maxIncludeLinesField.setText(String.valueOf(profile.directiveINCLUDEMaximumNumberOfLinesPerFile));
	}

	/** Writes every field's current value into {@code profile}. */
	private void getDialogValues(Profile profile) {
		profile.commentPrefix = commentField.getText();
		profile.hexNotationPrefix = hexNotationField.getText();
		profile.useIllegalOpcodes = illegalInstructionCheckBox.isSelected();
		profile.useHexNotation = useHexCheckBox.isSelected();
		profile.alignInstructions = alignInstructionCheckBox.isSelected();
		profile.useLineNumbers = lineNumberingCheckBox.isSelected();

		profile.showLowerCaseInstructions = showLowerCaseCheckBox.isSelected();
		profile.showAInAccumulatorMode = showImpliciteCheckBox.isSelected();
		profile.showColonAfterLabel = showColonCheckBox.isSelected();
		profile.showOpcodeAsComment = displayOpcodesCheckBox.isSelected();

		profile.showBRKAsByte0 = showByte0CheckBox.isSelected();
		profile.showZPAbsoluteAsByte = showZPAsByteCheckBox.isSelected();
		profile.directiveForceAbsolute = forceAbsoluteField.getText();
		profile.showNonASCIIChararactersAsBytes = nonASCIIAsBytesCheckBox.isSelected();

		profile.directiveBYTENumberOfBytesPerLine = getNumber(bytePerLineField);
		profile.directiveWORDNumberOfWordsPerLine = getNumber(wordPerLineField);
		profile.directiveBYTENumberOfCharactersPerString = getNumber(charPerLineField);
		profile.quoteForASCIIStrings = quoteForASCIIStringsField.getText();

		profile.directiveORG = orgSyntaxField.getText();
		profile.directiveENDHead = endSyntaxField.getText();
		profile.directiveENDNeedsFilename = endFilenameCheckBox.isSelected();
		profile.directiveENDTail = endTailField.getText();
		profile.directiveEQU = equSyntaxField.getText();
		profile.directiveLOWHead = lowHeadSyntaxField.getText();
		profile.directiveLOWTail = lowTailSyntaxField.getText();
		profile.directiveHIGHHead = highHeadSyntaxField.getText();
		profile.directiveHIGHTail = highTailSyntaxField.getText();

		profile.directiveBYTEOnlyNumbersAllowed = numOnlyInByteCheckBox.isSelected();
		profile.directiveBYTE = byteSyntaxField.getText();
		profile.directiveBYTESeparator = byteSeparatorField.getText();
		profile.directiveSBYTEAllowed = sByteAllowedCheckBox.isSelected();
		profile.directiveSBYTE = sByteSyntaxField.getText();
		profile.directiveWORDAllowed = wordAllowedCheckBox.isSelected();
		profile.directiveWORD = wordSyntaxField.getText();
		profile.directiveDSAllowed = dsAllowedCheckBox.isSelected();
		profile.directiveDS = dsSyntaxField.getText();

		Encoding selectedEncoding = outputEncodingField.getValue();
		profile.outputEncoding = selectedEncoding != null ? selectedEncoding : Encoding.ASCII;
		profile.omitUnreferencedSystemLabels = removeUnusedLabelsCheckBox.isSelected();

		profile.directiveINCLUDEAllowed = includeAllowedCheckBox.isSelected();
		profile.directiveINCLUDEHead = includeHeadField.getText();
		profile.directiveINCLUDETail = includeTailField.getText();
		profile.directiveINCLUDEAllEquatesInOneIncludeFile = radioIncludeOneFile.isSelected();
		profile.directiveINCLUDEAllIncludesInMainFile = radioIncludeAllFiles.isSelected();
		profile.directiveINCLUDEMaximumNumberOfLinesPerFile = getNumber(maxIncludeLinesField);
	}

	/** Parses a non-negative integer field; an unparseable value is silently treated as 0. */
	private static int getNumber(JTextField field) {
		try {
			int value = Integer.parseInt(field.getText().trim());
			return Math.max(value, 0);
		} catch (NumberFormatException ex) {
			return 0;
		}
	}

	/** Loads a profile file and applies it to the dialog's fields. */
	private void performLoadProfile() {
		File file = fileChoosers.chooseOpenFile(this, Texts.ProfileDialog_LoadFileTitle, FileType.PROFILE_FILE);
		if (file == null) {
			return;
		}
		lastProfileFile = file;

		Profile loadedProfile = new Profile();
		if (profileLogic.loadAndSetDefaultProfile(loadedProfile, computerSystemType, lastProfileFile.getPath())) {
			setDialogValues(loadedProfile);
			getDialogValues(workingProfile);
		} else {
			// ERROR: Could not load profile '{0}'. See the log for details.
			JOptionPane.showMessageDialog(this, Messages.E047.format(lastProfileFile.getPath()), Texts.ProfileDialog_LoadTitle,
					JOptionPane.ERROR_MESSAGE);
		}
	}

	/** Saves the dialog's current field values as a profile file. */
	private void performSaveProfile() {
		File file = fileChoosers.chooseSaveFile(this, Texts.ProfileDialog_SaveFileTitle, FileType.PROFILE_FILE, lastProfileFile);
		if (file == null) {
			return;
		}
		lastProfileFile = file;

		Profile currentProfile = new Profile();
		getDialogValues(currentProfile);
		profileLogic.save(currentProfile, lastProfileFile.getPath());
	}

	/**
	 * Opens the dialog as one blocking call, idiomatic for a Swing modal
	 * {@link JDialog}. Returns {@code true}, and writes the edited values
	 * back into {@code profile}, only if the user clicked OK - see the class
	 * javadoc for why that matters, not just as an implementation detail.
	 */
	public boolean show(Profile profile, ComputerSystemType computerSystemType) {
		this.computerSystemType = computerSystemType;

		setDialogValues(profile);
		getDialogValues(workingProfile);

		confirmed = false;
		setVisible(true); // Blocks until disposed/hidden - this is a modal dialog.

		if (confirmed) {
			getDialogValues(profile);
		}
		return confirmed;
	}
}
