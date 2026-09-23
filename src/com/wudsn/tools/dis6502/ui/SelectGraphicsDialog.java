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

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import com.wudsn.tools.base.Actions;
import com.wudsn.tools.base.gui.ElementFactory;
import com.wudsn.tools.base.gui.ValueSetField;
import com.wudsn.tools.dis6502.DataTypes;
import com.wudsn.tools.dis6502.Texts;
import com.wudsn.tools.dis6502.model.MemoryInspectorState;
import com.wudsn.tools.dis6502.model.Segment;

/**
 * A dialog for visually picking a byte range in the memory inspector's
 * current segment, by browsing it as a picture in one of the 8 Atari
 * ANTIC graphics modes ({@link GraphicMode}/{@link GraphicPanel}).
 * <p>
 * Called "Graphic"/"Graphics" throughout this dialog and {@link
 * GraphicMode}/{@link GraphicPanel}, rather than "Sprite" - the original
 * terminology doesn't fit what this feature actually does (browsing raw
 * memory as an ANTIC-mode picture, not moving game sprites around).
 * <p>
 * Folded into one blocking {@link #show} call, as is idiomatic for a Swing
 * modal {@link JDialog}. {@link GridBagLayout}/{@link #pack} handle the
 * layout. The vertical scroll position and bytes-per-line are plain
 * {@link JScrollBar}/{@link JSpinner} controls pushed into {@link
 * GraphicPanel} directly, rather than being owned by the picture control
 * itself - see that class's javadoc.
 *
 * @author Peter Dell
 */
public final class SelectGraphicsDialog extends JDialog {

	private static final long serialVersionUID = 1L;

	private final GraphicPanel graphicPanel = new GraphicPanel();
	private final ValueSetField<GraphicMode> modeField = new ValueSetField<GraphicMode>(GraphicMode.class);
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
		setTitle(Texts.SelectGraphicsDialog_Title);

		modeField.addActionListener(e -> performModeChanged());
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
		southPanel.add(ElementFactory.createLabel(DataTypes.SelectGraphicsDialog_GraphicMode, modeField), c);
		c.gridx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		southPanel.add(modeField, c);

		c.gridx = 0;
		c.gridy = 1;
		c.fill = GridBagConstraints.NONE;
		c.weightx = 0;
		southPanel.add(ElementFactory.createLabel(DataTypes.SelectGraphicsDialog_BytesPerLine, numberOfBytesPerLineSpinner), c);
		c.gridx = 1;
		southPanel.add(numberOfBytesPerLineSpinner, c);

		c.gridx = 0;
		c.gridy = 2;
		southPanel.add(ElementFactory.createLabel(DataTypes.SelectGraphicsDialog_Address, addressLabel), c);
		c.gridx = 1;
		southPanel.add(addressLabel, c);

		JButton okButton = ElementFactory.createButton(Actions.ButtonBar_OK, true);
		okButton.addActionListener(e -> performOK());
		JButton cancelButton = ElementFactory.createButton(Actions.ButtonBar_Cancel, true);
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

		ElementUtilities.closeOnEscape(this, cancelButton::doClick);
	}

	/** Reconfigures the bytes-per-line spinner and graphic panel for the newly selected mode. */
	private void performModeChanged() {
		GraphicMode mode = modeField.getValue();
		graphicPanel.setMode(mode);

		int current = (Integer) numberOfBytesPerLineSpinner.getValue();
		numberOfBytesPerLineSpinner.setModel(new SpinnerNumberModel(Math.min(current, mode.bytesPerLine), 1, mode.bytesPerLine, 1));
		graphicPanel.setNumberOfBytesPerLine((Integer) numberOfBytesPerLineSpinner.getValue());

		updateAddressLabel();
	}

	/** Shows the current index (and selection, if any) as an address or address range. */
	private void updateAddressLabel() {
		int begin = graphicPanel.getIndex();
		int end = graphicPanel.getSelection();
		if (end != GraphicPanel.NO_SELECTION && end >= begin) {
			addressLabel.setText(String.format("$%04X - $%04X", segment.wBegin + begin, segment.wBegin + end));
		} else {
			addressLabel.setText(String.format("$%04X", segment.wBegin + begin));
		}
	}

	/** Commits the current selection (or lack of one) as the result and closes the dialog. */
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

	/** Opens the dialog on {@code memoryInspectorState}'s segment, pre-selecting its current byte range if any. */
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

		modeField.setValue(GraphicMode.ANTIC_F); // Default mode.
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
