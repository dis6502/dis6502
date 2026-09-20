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
 * ANTIC graphics modes ({@link GraphicMode}/{@link GraphicPanel}).
 * <p>
 * Ported from ui/SelectSpritesDialog.h/.cpp - this port deliberately
 * renames the C++ source's "Sprite" naming (the dialog resource {@code
 * FINDSPRITESBOX}, caption "Sprite Selection", class {@code
 * SelectSpritesDialog}, and the {@code SPRITE_*}/{@code Sprite*}
 * identifiers throughout {@code SpriteControl}/{@code SpriteControlImpl})
 * to "Graphic"/"Graphics" instead, since that term does not fit this
 * project's domain - a departure from C++ fidelity made deliberately, not
 * a mistranslation of the original (which genuinely uses "Sprite"
 * throughout, including its own "Graphic Mode:" field label being drawn
 * by a control whose window class is still named {@code
 * SpriteControlClass}). "Ported from"/"Matches" references elsewhere in
 * this class and {@link GraphicMode}/{@link GraphicPanel} keep citing the
 * real C++ identifiers verbatim regardless, since those are historical
 * facts about the C++ source, not names this port chose.
 * <p>
 * Folded into one blocking {@link #show} call as is idiomatic for a Swing
 * modal {@link JDialog}. {@code InitDialog}'s extensive manual control-
 * repositioning code (the C++ version's own comment flags its dialog
 * template as "too small") is not needed - {@link GridBagLayout}/{@link
 * #pack} handle that. The vertical scroll position ({@code SelectGoto}'s
 * {@code wIndex}) and bytes-per-line ({@code SelectMode}'s {@code
 * wSpriteNbBytes}) are plain {@link JScrollBar}/{@link JSpinner} controls
 * pushed into {@link GraphicPanel} directly, rather than being owned by
 * the picture control itself - see that class's javadoc.
 *
 * @author Peter Dell
 */
public final class SelectGraphicsDialog extends JDialog {

	private static final long serialVersionUID = 1L;

	private final GraphicPanel graphicPanel = new GraphicPanel();
	private final JComboBox<GraphicMode> modeComboBox = new JComboBox<>(GraphicMode.values());
	private final JScrollBar indexScrollBar = new JScrollBar(JScrollBar.VERTICAL);
	private final JSpinner numberOfBytesPerLineSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 1, 1));
	private final JLabel addressLabel = new JLabel();

	private Segment segment;
	private int resultBegin;
	private int resultEnd;
	private boolean confirmed;

	public SelectGraphicsDialog(Frame owner) {
		super(owner, true);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setTitle("Select Graphics");

		modeComboBox.addActionListener(e -> performModeChanged());
		indexScrollBar.getModel().addChangeListener(e -> {
			graphicPanel.setIndex(indexScrollBar.getValue());
			updateAddressLabel();
		});
		numberOfBytesPerLineSpinner.addChangeListener(e -> {
			graphicPanel.setNumberOfBytesPerLine((Integer) numberOfBytesPerLineSpinner.getValue());
			updateAddressLabel();
		});
		graphicPanel.setSelectionChangedListener(this::updateAddressLabel);

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
		getContentPane().add(graphicPanel, BorderLayout.CENTER);
		getContentPane().add(indexScrollBar, BorderLayout.EAST);
		getContentPane().add(bottomPanel, BorderLayout.SOUTH);
		pack();
		setLocationRelativeTo(owner);
	}

	/** Ported from SelectSpritesDialog::SelectMode. */
	private void performModeChanged() {
		GraphicMode mode = (GraphicMode) modeComboBox.getSelectedItem();
		graphicPanel.setMode(mode);

		int current = (Integer) numberOfBytesPerLineSpinner.getValue();
		numberOfBytesPerLineSpinner.setModel(new SpinnerNumberModel(Math.min(current, mode.bytesPerLine), 1, mode.bytesPerLine, 1));
		graphicPanel.setNumberOfBytesPerLine((Integer) numberOfBytesPerLineSpinner.getValue());

		updateAddressLabel();
	}

	/** Ported from the address-formatting part of SelectSpritesDialog::SelectGoto/ProcessCommand's IDC_GRAPHIC case. */
	private void updateAddressLabel() {
		int begin = graphicPanel.getIndex();
		int end = graphicPanel.getSelection();
		if (end != GraphicPanel.NO_SELECTION && end >= begin) {
			addressLabel.setText(String.format("$%04X - $%04X", segment.wBegin + begin, segment.wBegin + end));
		} else {
			addressLabel.setText(String.format("$%04X", segment.wBegin + begin));
		}
	}

	/** Ported from SelectSpritesDialog::OnOK. */
	private void performOK() {
		int begin = graphicPanel.getIndex();
		int end = graphicPanel.getSelection();

		if (end != GraphicPanel.NO_SELECTION && end >= begin) {
			resultBegin = begin;
			resultEnd = end;
		} else {
			resultBegin = resultEnd = GraphicPanel.NO_SELECTION;
		}

		confirmed = true;
		setVisible(false);
	}

	/** Ported from SelectSpritesDialog::Show/InitDialog/SelectGoto. */
	public boolean show(MemoryInspectorState memoryInspectorState) {
		segment = memoryInspectorState.getSegment();
		graphicPanel.setBuffer(segment.memoryBlock.getData());

		int begin = 0;
		int end = GraphicPanel.NO_SELECTION;
		if (memoryInspectorState.hasSelection()) {
			begin = memoryInspectorState.getBegin();
			end = memoryInspectorState.getEnd();
		}

		indexScrollBar.setMinimum(0);
		indexScrollBar.setMaximum(segment.getSize());
		indexScrollBar.setValue(begin);
		graphicPanel.setIndex(begin);
		graphicPanel.setSelection(end);

		modeComboBox.setSelectedItem(GraphicMode.ANTIC_F); // Matches the C++ constructor's wSpriteMode = 15 default.
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
