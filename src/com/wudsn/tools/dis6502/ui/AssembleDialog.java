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
import javax.swing.JTextField;

import com.wudsn.tools.base.gui.ElementFactory;
import com.wudsn.tools.dis6502.Actions;
import com.wudsn.tools.dis6502.DataTypes;
import com.wudsn.tools.dis6502.model.Assembler;
import com.wudsn.tools.dis6502.model.Instruction;
import com.wudsn.tools.dis6502.model.MutableMemoryInspectorState;
import com.wudsn.tools.dis6502.model.Segment;
import com.wudsn.tools.dis6502.model.Workspace;

/**
 * A small assembler console for typing 6502 instructions directly into a
 * segment at the memory inspector's current selection, one at a time,
 * advancing the selection after each one.
 * <p>
 * Ported from ui/AssembleDialog.h / AssembleDialog.cpp, folded into one
 * blocking {@link #show} call as is idiomatic for a Swing modal
 * {@link JDialog}, on top of the already-complete {@link Assembler#parseLine}.
 * {@link #performAssemble} re-reads {@code memoryInspectorState}'s
 * begin offset on every call (matching {@code
 * MemoryInspectorControl::GetNonEmptySelection} in {@code OnOK}), rather
 * than tracking its own copy, since a successful assemble advances that
 * same selection via {@link MemoryInspectorPanel#select} - the position
 * genuinely lives there, not in the dialog.
 * <p>
 * The C++ dialog's own resource ({@code ASSEMBLEBOX}) labels its {@code
 * IDOK} button "&amp;Assemble" and pairs it with a separate "&amp;Close"
 * ({@code IDCANCEL}) button - a repeatable action, matching how {@code
 * OnOK} resets the edit field and advances the address label ready for
 * the next instruction - but {@code OnOK} closed the dialog after every
 * single press regardless, making that reset unreachable in practice.
 * Fixed here (and in the C++ source, in a separate commit): {@link
 * #assembleButton} (and pressing Enter in {@link #instructionField})
 * only close this dialog via {@link #closeButton}.
 *
 * @author Peter Dell
 */
public final class AssembleDialog extends JDialog {

	private static final long serialVersionUID = 1L;

	private final JTextField instructionField = new JTextField(30);
	private final JLabel addressLabel = new JLabel();
	private final JLabel resultLabel = new JLabel("Enter instruction, e.g. LDA #1");
	private final JButton assembleButton = ElementFactory.createButton(Actions.AssembleDialog_Assemble, true);
	private final JButton closeButton = ElementFactory.createButton(Actions.AssembleDialog_Close, true);

	private Workspace workspace;
	private Segment segment;
	private MutableMemoryInspectorState memoryInspectorState;
	private MemoryInspectorPanel memoryInspectorPanel;
	private boolean assembledAny;

	public AssembleDialog(Frame owner) {
		super(owner, true);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setTitle("Assemble");

		JPanel formPanel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(4, 4, 4, 4);
		c.anchor = GridBagConstraints.WEST;

		c.gridx = 0;
		c.gridy = 0;
		formPanel.add(ElementFactory.createLabel(DataTypes.AssembleDialog_Address, addressLabel), c);
		c.gridx = 1;
		formPanel.add(addressLabel, c);

		c.gridx = 0;
		c.gridy = 1;
		c.gridwidth = 2;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		formPanel.add(instructionField, c);

		c.gridx = 0;
		c.gridy = 2;
		c.gridwidth = 1;
		c.fill = GridBagConstraints.NONE;
		c.weightx = 0;
		formPanel.add(ElementFactory.createLabel(DataTypes.AssembleDialog_Result, resultLabel), c);
		c.gridx = 1;
		formPanel.add(resultLabel, c);

		instructionField.addActionListener(e -> performAssemble());
		assembleButton.addActionListener(e -> performAssemble());
		closeButton.addActionListener(e -> setVisible(false));
		JPanel buttonPanel = new JPanel();
		buttonPanel.add(assembleButton);
		buttonPanel.add(closeButton);

		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(formPanel, BorderLayout.CENTER);
		getContentPane().add(buttonPanel, BorderLayout.SOUTH);
		pack();
		setLocationRelativeTo(owner);
	}

	/** Ported from AssembleDialog::OnOK, minus the dialog-closing bug described in this class's javadoc. */
	private void performAssemble() {
		int beginOffset = memoryInspectorState.getBegin();
		int address = segment.wBegin + beginOffset;

		String line = instructionField.getText();
		Assembler.Result parseResult = Assembler.parseLine(workspace, line, address, segment.processorType);

		String result;
		if (!parseResult.error.isEmpty()) {
			result = parseResult.error;
		} else if (parseResult.instruction == null) {
			result = "ERROR: Unknown instruction or addressing mode.";
		} else {
			Instruction instruction = parseResult.instruction;
			int value = parseResult.value;
			String comment = parseResult.comment;
			int length = instruction.getLength();

			if (beginOffset + length > segment.getSize()) {
				result = "ERROR: Instruction does not fit in segment";
			} else {
				int opcode = instruction.getOpcode();
				segment.setData(beginOffset, opcode);
				switch (length) {
				case 1:
					result = String.format("$%02X ; %s", opcode, comment);
					break;
				case 2:
					segment.setData(beginOffset + 1, value & 0xFF);
					result = String.format("$%02X $%02X ; %s", opcode, value & 0xFF, comment);
					break;
				case 3:
					segment.setData(beginOffset + 1, value & 0xFF);
					segment.setData(beginOffset + 2, (value >> 8) & 0xFF);
					result = String.format("$%02X $%02X $%02X ; %s", opcode, value & 0xFF, (value >> 8) & 0xFF, comment);
					break;
				default:
					result = "ERROR: Invalid instruction length.";
				}

				assembledAny = true;
				beginOffset += length;
				memoryInspectorPanel.select(beginOffset, beginOffset);

				instructionField.setText("");
				address += length;
				addressLabel.setText(String.format("$%04X", address & 0xFFFF));
			}
		}

		resultLabel.setText(result);
	}

	/** Ported from AssembleDialog::Show/InitDialog. Returns whether at least one instruction was actually assembled. */
	public boolean show(Workspace workspace, Segment segment, MutableMemoryInspectorState memoryInspectorState,
			MemoryInspectorPanel memoryInspectorPanel) {
		this.workspace = workspace;
		this.segment = segment;
		this.memoryInspectorState = memoryInspectorState;
		this.memoryInspectorPanel = memoryInspectorPanel;

		instructionField.setText("");
		resultLabel.setText("Enter instruction, e.g. LDA #1");
		addressLabel.setText(String.format("$%04X", (segment.wBegin + memoryInspectorState.getBegin()) & 0xFFFF));

		assembledAny = false;
		setVisible(true); // Blocks until disposed/hidden - this is a modal dialog.

		return assembledAny;
	}
}
