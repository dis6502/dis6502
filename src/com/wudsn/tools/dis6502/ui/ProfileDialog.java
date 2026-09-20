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
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.wudsn.tools.base.Actions;
import com.wudsn.tools.base.gui.ElementFactory;
import com.wudsn.tools.dis6502.model.ComputerSystemTypeInfo;
import com.wudsn.tools.dis6502.model.Encoding;
import com.wudsn.tools.dis6502.model.Profile;
import com.wudsn.tools.dis6502.model.ProfileLogic;

/**
 * A dialog for editing a {@link Profile}'s disassembly output syntax and
 * layout settings.
 * <p>
 * Ported from ui/ProfileDialog.h / ProfileDialog.cpp and the {@code
 * PROFILEBOX} resource in dis6502.rc, reorganized into three titled
 * sections ("General", "Directive Syntax", "Disassembly Listing", matching
 * the .rc's group boxes) stacked with plain Swing layout instead of the
 * .rc's pixel-exact control coordinates - see the {@code ui} package's
 * general porting note on Swing idioms vs. literal Win32 translation.
 * {@code ProfilesController} (a thin {@link ProfileLogic} plus file-chooser
 * wrapper) is folded directly into this class's Load/Save button handlers
 * instead of being ported separately.
 * <p>
 * The C++ version originally had a bug here, found while porting it: its
 * checkbox/radio-button change handler called {@code
 * GetDialogValues(*profile)} - writing the widgets' current values straight
 * into the real, caller-owned {@code profile} - purely to recompute the
 * other widgets' enabled state via the following {@code
 * SetDialogValues(*profile)}. Since {@code OnCancel} never reverted this,
 * clicking Cancel after toggling even one checkbox left the real {@code
 * Profile} object partially mutated. This port recomputes enabled state
 * against a private scratch {@link Profile} ({@link #workingProfile}) and
 * only writes into the real one passed to {@link #show} when the user
 * clicks OK, so Cancel is always a true no-op - the same "decouple the edit
 * session from the real model until commit" fix already applied to {@link
 * EquateDialog}. The C++ source has since been fixed the same way (see that
 * commit), using its own {@code workingProfile} member kept in sync via the
 * same {@code SetDialogValues}/{@code GetDialogValues} round trip, since
 * {@code Profile} cannot be copy-assigned there ({@code XML::Serializable}
 * explicitly deletes copy/move).
 *
 * @author Peter Dell
 */
public final class ProfileDialog extends JDialog {

	private static final long serialVersionUID = 1L;

	private static final Encoding[] OUTPUT_ENCODINGS = { Encoding.ASCII, Encoding.ATASCII, Encoding.UTF8 };

	// General.
	private final JTextField commentField = new JTextField(4);
	private final JTextField hexNotationField = new JTextField(4);
	private final JCheckBox illegalInstructionCheckBox = new JCheckBox("Illegal Instructions");
	private final JCheckBox useHexCheckBox = new JCheckBox("Use Hex Notation");
	private final JCheckBox alignInstructionCheckBox = new JCheckBox("Align Instructions");
	private final JCheckBox lineNumberingCheckBox = new JCheckBox("Use Line Numbers");
	private final JCheckBox showLowerCaseCheckBox = new JCheckBox("Show Lowercase Instructions");
	private final JCheckBox showImpliciteCheckBox = new JCheckBox("Show 'A' in Accumulator Mode");
	private final JCheckBox showColonCheckBox = new JCheckBox("Show ':' after Labels");
	private final JCheckBox displayOpcodesCheckBox = new JCheckBox("Show Opcode as Comment");
	private final JCheckBox showByte0CheckBox = new JCheckBox("Show 'BRK' as '.BYTE $00'");
	private final JCheckBox showZPAsByteCheckBox = new JCheckBox("Show ZP Addr. in Absolute Mode as Bytes");
	private final JTextField forceAbsoluteField = new JTextField(4);
	private final JCheckBox nonASCIIAsBytesCheckBox = new JCheckBox("Show Non-ASCII Characters as Bytes");
	private final JTextField bytePerLineField = new JTextField(3);
	private final JTextField wordPerLineField = new JTextField(3);
	private final JTextField charPerLineField = new JTextField(3);
	private final JTextField quoteForASCIIStringsField = new JTextField(3);

	// Directive Syntax.
	private final JCheckBox numOnlyInByteCheckBox = new JCheckBox("Only Numbers in .BYTE");
	private final JTextField byteSyntaxField = new JTextField(6);
	private final JTextField byteSeparatorField = new JTextField(3);
	private final JCheckBox wordAllowedCheckBox = new JCheckBox(".WORD Allowed:");
	private final JTextField wordSyntaxField = new JTextField(6);
	private final JCheckBox sByteAllowedCheckBox = new JCheckBox(".SBYTE Allowed:");
	private final JTextField sByteSyntaxField = new JTextField(6);
	private final JTextField orgSyntaxField = new JTextField(6);
	private final JTextField lowHeadSyntaxField = new JTextField(6);
	private final JTextField lowTailSyntaxField = new JTextField(6);
	private final JTextField equSyntaxField = new JTextField(6);
	private final JTextField highHeadSyntaxField = new JTextField(6);
	private final JTextField highTailSyntaxField = new JTextField(6);
	private final JTextField endSyntaxField = new JTextField(8);
	private final JCheckBox endFilenameCheckBox = new JCheckBox("Add File Name");
	private final JTextField endTailField = new JTextField(8);
	private final JCheckBox dsAllowedCheckBox = new JCheckBox(".DS (Data Storage) Allowed:");
	private final JTextField dsSyntaxField = new JTextField(6);

	// Disassembly Listing.
	private final JComboBox<Encoding> outputEncodingComboBox = new JComboBox<>(OUTPUT_ENCODINGS);
	private final JCheckBox removeUnusedLabelsCheckBox = new JCheckBox("Omit Unreferenced System Labels");
	private final JCheckBox includeAllowedCheckBox = new JCheckBox("Include Files Allowed");
	private final JTextField includeHeadField = new JTextField(8);
	private final JTextField includeTailField = new JTextField(8);
	private final JRadioButton radioIncludeOneFile = new JRadioButton("One File for Equates included in Main File");
	private final JRadioButton radioIncludeAllFiles = new JRadioButton("All Files Included in Main File");
	private final JRadioButton radioIncludeNextFile = new JRadioButton("Each File Includes Next File");
	private final JTextField maxIncludeLinesField = new JTextField(6);

	private final ProfileLogic profileLogic;
	private final Profile workingProfile = new Profile();
	private ComputerSystemTypeInfo computerSystemTypeInfo;
	private File lastProfileFile;
	private boolean confirmed;

	public ProfileDialog(Frame owner, ProfileLogic profileLogic) {
		super(owner, true);
		this.profileLogic = profileLogic;
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setTitle("Profile");

		ButtonGroup includeFileGroup = new ButtonGroup();
		includeFileGroup.add(radioIncludeOneFile);
		includeFileGroup.add(radioIncludeAllFiles);
		includeFileGroup.add(radioIncludeNextFile);

		// Ported from ProcessCommand's recursion-preventing "GetDialogValues then
		// SetDialogValues" pattern, run against workingProfile (see class javadoc).
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

		JButton loadButton = new JButton("Load Profile...");
		loadButton.addActionListener(e -> performLoadProfile());
		JButton saveButton = new JButton("Save Profile...");
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
	}

	private JPanel createGeneralPanel() {
		JPanel panel = new JPanel(new GridBagLayout());
		panel.setBorder(BorderFactory.createTitledBorder("General"));
		int row = 0;
		addLabeledField(panel, row++, "Comment:", commentField, "Hex Prefix:", hexNotationField);
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
		addLabeledField(panel, row++, "Mnemonic to force Absolute Mode:", forceAbsoluteField);
		addFullWidth(panel, row++, nonASCIIAsBytesCheckBox);
		addLabeledField(panel, row++, "Number of Byte Values per Line:", bytePerLineField);
		addLabeledField(panel, row++, "Number of Word Values per Line:", wordPerLineField);
		addLabeledField(panel, row++, "Number of Characters per String:", charPerLineField);
		addLabeledField(panel, row++, "Quote for ASCII Strings:", quoteForASCIIStringsField);
		return panel;
	}

	private JPanel createDirectiveSyntaxPanel() {
		JPanel panel = new JPanel(new GridBagLayout());
		panel.setBorder(BorderFactory.createTitledBorder("Directive Syntax"));
		int row = 0;
		addLabeledField(panel, row++, ".BYTE:", byteSyntaxField, "Separator:", byteSeparatorField);
		addFullWidth(panel, row++, numOnlyInByteCheckBox);
		addLabeledField(panel, row++, wordAllowedCheckBox, wordSyntaxField);
		addLabeledField(panel, row++, sByteAllowedCheckBox, sByteSyntaxField);
		addLabeledField(panel, row++, ".ORG:", orgSyntaxField, "Low Byte (ADDR):", lowHeadSyntaxField, lowTailSyntaxField);
		addLabeledField(panel, row++, "EQU:", equSyntaxField, "High Byte (ADDR):", highHeadSyntaxField, highTailSyntaxField);
		addLabeledField(panel, row++, ".END:", endSyntaxField, endFilenameCheckBox, endTailField);
		addLabeledField(panel, row++, dsAllowedCheckBox, dsSyntaxField);
		return panel;
	}

	private JPanel createDisassemblyListingPanel() {
		JPanel panel = new JPanel(new GridBagLayout());
		panel.setBorder(BorderFactory.createTitledBorder("Disassembly Listing"));
		int row = 0;
		addLabeledField(panel, row++, "Encoding:", outputEncodingComboBox);
		addFullWidth(panel, row++, removeUnusedLabelsCheckBox);
		addFullWidth(panel, row++, includeAllowedCheckBox);
		addLabeledField(panel, row++, ".INCLUDE:", includeHeadField, "FILENAME:", includeTailField);
		addFullWidth(panel, row++, radioIncludeOneFile);
		addFullWidth(panel, row++, radioIncludeAllFiles);
		addFullWidth(panel, row++, radioIncludeNextFile);
		addLabeledField(panel, row++, "Maximum Number of Lines per Include File:", maxIncludeLinesField);
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

	private static void addLabeledField(JPanel panel, int row, String label, JComponent field) {
		GridBagConstraints lc = gbc(0, row);
		panel.add(new JLabel(label), lc);
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

	private static void addLabeledField(JPanel panel, int row, String label1, JComponent field1, String label2, JComponent field2) {
		GridBagConstraints l1 = gbc(0, row);
		panel.add(new JLabel(label1), l1);
		GridBagConstraints f1 = gbc(1, row);
		f1.fill = GridBagConstraints.HORIZONTAL;
		panel.add(field1, f1);
		GridBagConstraints l2 = gbc(2, row);
		panel.add(new JLabel(label2), l2);
		GridBagConstraints f2 = gbc(3, row);
		f2.fill = GridBagConstraints.HORIZONTAL;
		f2.weightx = 1;
		panel.add(field2, f2);
	}

	private static void addLabeledField(JPanel panel, int row, String label1, JComponent field1, String label2, JComponent field2,
			JComponent field3) {
		addLabeledField(panel, row, label1, field1, label2, field2);
		GridBagConstraints f3 = gbc(4, row);
		f3.fill = GridBagConstraints.HORIZONTAL;
		panel.add(field3, f3);
	}

	private static void addLabeledField(JPanel panel, int row, String label1, JComponent field1, JComponent checkBox2, JComponent field2) {
		GridBagConstraints l1 = gbc(0, row);
		panel.add(new JLabel(label1), l1);
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

	/** Ported from ProfileDialog::ProcessCommand's checkbox/radio-button "value changed" handling. */
	private void refresh() {
		getDialogValues(workingProfile);
		setDialogValues(workingProfile);
	}

	/** Ported from ProfileDialog::SetDialogValues. */
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

		outputEncodingComboBox.setSelectedItem(isKnownOutputEncoding(profile.outputEncoding) ? profile.outputEncoding : Encoding.ASCII);
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

	private static boolean isKnownOutputEncoding(Encoding encoding) {
		for (Encoding candidate : OUTPUT_ENCODINGS) {
			if (candidate == encoding) {
				return true;
			}
		}
		return false;
	}

	/** Ported from ProfileDialog::GetDialogValues. */
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

		Encoding selectedEncoding = (Encoding) outputEncodingComboBox.getSelectedItem();
		profile.outputEncoding = selectedEncoding != null ? selectedEncoding : Encoding.ASCII;
		profile.omitUnreferencedSystemLabels = removeUnusedLabelsCheckBox.isSelected();

		profile.directiveINCLUDEAllowed = includeAllowedCheckBox.isSelected();
		profile.directiveINCLUDEHead = includeHeadField.getText();
		profile.directiveINCLUDETail = includeTailField.getText();
		profile.directiveINCLUDEAllEquatesInOneIncludeFile = radioIncludeOneFile.isSelected();
		profile.directiveINCLUDEAllIncludesInMainFile = radioIncludeAllFiles.isSelected();
		profile.directiveINCLUDEMaximumNumberOfLinesPerFile = getNumber(maxIncludeLinesField);
	}

	/** Ported from EditControl::GetNumber: an unparseable value is silently treated as 0, matching {@code swscanf(..., L"%u", ...)}'s behavior on no match. */
	private static int getNumber(JTextField field) {
		try {
			int value = Integer.parseInt(field.getText().trim());
			return Math.max(value, 0);
		} catch (NumberFormatException ex) {
			return 0;
		}
	}

	/** Ported from ProfileDialog::ProcessCommand's {@code ID_LOAD_PROFILE} case. */
	private void performLoadProfile() {
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setDialogTitle("Load Profile File");
		fileChooser.setFileFilter(new FileNameExtensionFilter("Profile Files (*.prf)", "prf"));
		if (lastProfileFile != null) {
			fileChooser.setCurrentDirectory(lastProfileFile.getParentFile());
		}
		if (fileChooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
			return;
		}
		lastProfileFile = fileChooser.getSelectedFile();

		Profile loadedProfile = new Profile();
		if (profileLogic.loadAndSetDefaultProfile(loadedProfile, computerSystemTypeInfo, lastProfileFile.getPath())) {
			setDialogValues(loadedProfile);
			getDialogValues(workingProfile);
		} else {
			JOptionPane.showMessageDialog(this, "Could not load profile '" + lastProfileFile.getPath() + "'. See the log for details.",
					"Load Profile", JOptionPane.ERROR_MESSAGE);
		}
	}

	/** Ported from ProfileDialog::ProcessCommand's {@code ID_SAVE_PROFILE} case. */
	private void performSaveProfile() {
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setDialogTitle("Save Profile File");
		fileChooser.setFileFilter(new FileNameExtensionFilter("Profile Files (*.prf)", "prf"));
		if (lastProfileFile != null) {
			fileChooser.setSelectedFile(lastProfileFile);
		}
		if (fileChooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
			return;
		}
		lastProfileFile = fileChooser.getSelectedFile();

		Profile currentProfile = new Profile();
		getDialogValues(currentProfile);
		profileLogic.save(currentProfile, lastProfileFile.getPath());
	}

	/**
	 * Ported from ProfileDialog::Show/InitDialog/OnOK/OnCancel, folded into
	 * one blocking call as is idiomatic for a Swing modal {@link JDialog}.
	 * Returns {@code true}, and writes the edited values back into {@code
	 * profile}, only if the user clicked OK - see the class javadoc for why
	 * that is a real behavioral improvement over the C++ version, not just
	 * an implementation detail.
	 */
	public boolean show(Profile profile, ComputerSystemTypeInfo computerSystemTypeInfo) {
		this.computerSystemTypeInfo = computerSystemTypeInfo;

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
