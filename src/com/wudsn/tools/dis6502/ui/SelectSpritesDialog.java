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

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import com.wudsn.tools.dis6502.model.MemoryInspectorState;
import com.wudsn.tools.dis6502.model.Segment;

/**
 * A dialog for visually picking a byte range in the memory inspector's
 * current segment, by browsing it as a picture in one of the 8 Atari
 * ANTIC graphics modes ({@link SpriteMode}/{@link SpritePanel}).
 * <p>
 * Ported from ui/SelectSpritesDialog.h/.cpp, folded into one blocking
 * {@link #show} call as is idiomatic for a Swing modal {@link JDialog}.
 * {@code InitDialog}'s extensive manual control-repositioning code (the
 * C++ version's own comment flags its dialog template as "too small") is
 * not needed - {@link GridBagLayout}/{@link #pack} handle that. The
 * vertical scroll position ({@code SelectGoto}'s {@code wIndex}) and
 * bytes-per-line ({@code SelectMode}'s {@code wSpriteNbBytes}) are plain
 * {@link JScrollBar}/{@link JSpinner} controls pushed into {@link
 * SpritePanel} directly, rather than being owned by the picture control
 * itself - see that class's javadoc.
 *
 * @author Peter Dell
 */
public final class SelectSpritesDialog extends JDialog {

	private static final long serialVersionUID = 1L;

	private final SpritePanel spritePanel = new SpritePanel();
	private final JComboBox<SpriteMode> modeComboBox = new JComboBox<>(SpriteMode.values());
	private final JScrollBar indexScrollBar = new JScrollBar(JScrollBar.VERTICAL);
	private final JSpinner numberOfBytesPerLineSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 1, 1));
	private final JLabel addressLabel = new JLabel();

	private Segment segment;
	private int resultBegin;
	private int resultEnd;
	private boolean confirmed;

	public SelectSpritesDialog(Frame owner) {
		super(owner, true);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setTitle("Select Sprites");

		modeComboBox.addActionListener(e -> performModeChanged());
		indexScrollBar.getModel().addChangeListener(e -> {
			spritePanel.setIndex(indexScrollBar.getValue());
			updateAddressLabel();
		});
		numberOfBytesPerLineSpinner.addChangeListener(e -> {
			spritePanel.setNumberOfBytesPerLine((Integer) numberOfBytesPerLineSpinner.getValue());
			updateAddressLabel();
		});
		spritePanel.setSelectionChangedListener(this::updateAddressLabel);

		JPanel southPanel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(4, 4, 4, 4);
		c.anchor = GridBagConstraints.WEST;

		c.gridx = 0;
		c.gridy = 0;
		southPanel.add(new JLabel("Graphic Mode:"), c);
		c.gridx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		southPanel.add(modeComboBox, c);

		c.gridx = 0;
		c.gridy = 1;
		c.fill = GridBagConstraints.NONE;
		c.weightx = 0;
		southPanel.add(new JLabel("Bytes per Line:"), c);
		c.gridx = 1;
		southPanel.add(numberOfBytesPerLineSpinner, c);

		c.gridx = 0;
		c.gridy = 2;
		southPanel.add(new JLabel("Address:"), c);
		c.gridx = 1;
		southPanel.add(addressLabel, c);

		JButton okButton = new JButton("OK");
		okButton.addActionListener(e -> performOK());
		JButton cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(e -> {
			confirmed = false;
			setVisible(false);
		});
		JPanel buttonPanel = new JPanel();
		buttonPanel.add(okButton);
		buttonPanel.add(cancelButton);

		JPanel bottomPanel = new JPanel(new BorderLayout());
		bottomPanel.add(southPanel, BorderLayout.NORTH);
		bottomPanel.add(buttonPanel, BorderLayout.SOUTH);

		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(spritePanel, BorderLayout.CENTER);
		getContentPane().add(indexScrollBar, BorderLayout.EAST);
		getContentPane().add(bottomPanel, BorderLayout.SOUTH);
		pack();
		setLocationRelativeTo(owner);
	}

	/** Ported from SelectSpritesDialog::SelectMode. */
	private void performModeChanged() {
		SpriteMode mode = (SpriteMode) modeComboBox.getSelectedItem();
		spritePanel.setMode(mode);

		int current = (Integer) numberOfBytesPerLineSpinner.getValue();
		numberOfBytesPerLineSpinner.setModel(new SpinnerNumberModel(Math.min(current, mode.bytesPerLine), 1, mode.bytesPerLine, 1));
		spritePanel.setNumberOfBytesPerLine((Integer) numberOfBytesPerLineSpinner.getValue());

		updateAddressLabel();
	}

	/** Ported from the address-formatting part of SelectSpritesDialog::SelectGoto/ProcessCommand's IDC_GRAPHIC case. */
	private void updateAddressLabel() {
		int begin = spritePanel.getIndex();
		int end = spritePanel.getSelection();
		if (end != SpritePanel.NO_SELECTION && end >= begin) {
			addressLabel.setText(String.format("$%04X - $%04X", segment.wBegin + begin, segment.wBegin + end));
		} else {
			addressLabel.setText(String.format("$%04X", segment.wBegin + begin));
		}
	}

	/** Ported from SelectSpritesDialog::OnOK. */
	private void performOK() {
		int begin = spritePanel.getIndex();
		int end = spritePanel.getSelection();

		if (end != SpritePanel.NO_SELECTION && end >= begin) {
			resultBegin = begin;
			resultEnd = end;
		} else {
			resultBegin = resultEnd = SpritePanel.NO_SELECTION;
		}

		confirmed = true;
		setVisible(false);
	}

	/** Ported from SelectSpritesDialog::Show/InitDialog/SelectGoto. */
	public boolean show(MemoryInspectorState memoryInspectorState) {
		segment = memoryInspectorState.getSegment();
		spritePanel.setBuffer(segment.memoryBlock.getData());

		int begin = 0;
		int end = SpritePanel.NO_SELECTION;
		if (memoryInspectorState.hasSelection()) {
			begin = memoryInspectorState.getBegin();
			end = memoryInspectorState.getEnd();
		}

		indexScrollBar.setMinimum(0);
		indexScrollBar.setMaximum(segment.getSize());
		indexScrollBar.setValue(begin);
		spritePanel.setIndex(begin);
		spritePanel.setSelection(end);

		modeComboBox.setSelectedItem(SpriteMode.ANTIC_F); // Matches the C++ constructor's wSpriteMode = 15 default.
		performModeChanged();

		confirmed = false;
		setVisible(true); // Blocks until disposed/hidden - this is a modal dialog.

		return confirmed;
	}

	public int getBegin() {
		return resultBegin;
	}

	public int getEnd() {
		return resultEnd;
	}
}
