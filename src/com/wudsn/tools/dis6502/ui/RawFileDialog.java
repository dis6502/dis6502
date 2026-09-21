/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.ui;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.wudsn.tools.base.Actions;
import com.wudsn.tools.base.gui.ElementFactory;
import com.wudsn.tools.dis6502.DataTypes;
import com.wudsn.tools.dis6502.Texts;

/**
 * A dialog for adding a raw (headerless) file to the workspace as a single
 * segment: the whole file, or a chosen byte range of it, loaded at a chosen
 * address.
 * <p>
 * Ported from ui/RawFileDialog.h / RawFileDialog.cpp, with one structural
 * simplification: the C++ version lets the user pick the byte range by
 * dragging a selection across a {@code MemoryInspectorControl} hex dump -
 * that control is the not-yet-ported memory inspector itself (see the
 * {@code ui} package's other panel classes), so this port shows the same
 * read-only hex dump but replaces drag-selection with explicit "Start
 * Offset"/"End Offset" fields, pre-filled with the whole file (offset 0 to
 * the last byte) to match {@code MemoryInspectorControl::GetSelection}'s
 * {@code bDefaultAll} behavior when nothing is selected. Address, like the
 * C++ version's field, is plain (no "$" prefix) hexadecimal; the offset
 * fields are decimal, since they describe a position within the file, not
 * a 6502 address.
 *
 * @author Peter Dell
 */
public final class RawFileDialog extends JDialog {

	private static final long serialVersionUID = 1L;

	private static final int BYTES_PER_LINE = 16;

	private final JTextField filePathField = new JTextField();
	private final JTextArea hexDumpArea = new JTextArea();
	private final JTextField startOffsetField = new JTextField(8);
	private final JTextField endOffsetField = new JTextField(8);
	private final JTextField addressField = new JTextField(6);
	private final JButton okButton = ElementFactory.createButton(Actions.ButtonBar_OK, true);

	private byte[] fileBuffer = new byte[0];
	private int resultBegin;
	private int resultSize;
	private int resultAddress;
	private boolean confirmed;

	public RawFileDialog(Frame owner) {
		super(owner, true);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setTitle(Texts.RawFileDialog_Title);

		filePathField.setEditable(false);

		hexDumpArea.setEditable(false);
		hexDumpArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

		DocumentListener addressListener = new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) {
				updateOkButtonEnabled();
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				updateOkButtonEnabled();
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				updateOkButtonEnabled();
			}
		};
		addressField.getDocument().addDocumentListener(addressListener);

		okButton.setEnabled(false);
		okButton.addActionListener(e -> performOK());
		JButton cancelButton = ElementFactory.createButton(Actions.ButtonBar_Cancel, true);
		cancelButton.addActionListener(e -> {
			confirmed = false;
			setVisible(false);
		});

		JPanel topPanel = new JPanel(new BorderLayout(4, 4));
		topPanel.add(ElementFactory.createLabel(DataTypes.RawFileDialog_FilePath, filePathField), BorderLayout.WEST);
		topPanel.add(filePathField, BorderLayout.CENTER);

		JPanel fieldsPanel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(2, 4, 2, 4);
		c.anchor = GridBagConstraints.WEST;

		c.gridx = 0;
		c.gridy = 0;
		fieldsPanel.add(ElementFactory.createLabel(DataTypes.RawFileDialog_StartOffset, startOffsetField), c);
		c.gridx = 1;
		fieldsPanel.add(startOffsetField, c);
		c.gridx = 2;
		fieldsPanel.add(ElementFactory.createLabel(DataTypes.RawFileDialog_EndOffset, endOffsetField), c);
		c.gridx = 3;
		fieldsPanel.add(endOffsetField, c);
		c.gridx = 4;
		fieldsPanel.add(ElementFactory.createLabel(DataTypes.RawFileDialog_Address, addressField), c);
		c.gridx = 5;
		fieldsPanel.add(addressField, c);

		JPanel buttonPanel = new JPanel();
		buttonPanel.add(okButton);
		buttonPanel.add(cancelButton);

		JPanel southPanel = new JPanel(new BorderLayout());
		southPanel.add(fieldsPanel, BorderLayout.NORTH);
		southPanel.add(buttonPanel, BorderLayout.SOUTH);

		getContentPane().setLayout(new BorderLayout(4, 4));
		getContentPane().add(topPanel, BorderLayout.NORTH);
		getContentPane().add(new JScrollPane(hexDumpArea), BorderLayout.CENTER);
		getContentPane().add(southPanel, BorderLayout.SOUTH);
		setSize(700, 500);
		setLocationRelativeTo(owner);
	}

	private void updateOkButtonEnabled() {
		okButton.setEnabled(!addressField.getText().trim().isEmpty());
	}

	/** Ported from RawFileDialog::OnOK, including MemoryInspectorControl::GetSelection's begin/end swap and end-of-buffer clamp. */
	private void performOK() {
		int begin = getOffset(startOffsetField, 0);
		int end = getOffset(endOffsetField, fileBuffer.length - 1);
		if (begin > end) {
			int temp = begin;
			begin = end;
			end = temp;
		}
		if (begin < 0) {
			begin = 0;
		}
		if (end > fileBuffer.length - 1) {
			end = fileBuffer.length - 1;
		}

		resultBegin = begin;
		resultSize = end - begin + 1;
		resultAddress = getAddress(addressField);
		confirmed = true;
		setVisible(false);
	}

	/** An unparseable value falls back to {@code defaultValue} rather than 0, since 0 is not a sensible default for an end offset. */
	private static int getOffset(JTextField field, int defaultValue) {
		try {
			return Integer.parseInt(field.getText().trim());
		} catch (NumberFormatException ex) {
			return defaultValue;
		}
	}

	/** Ported from EditControl::GetAddress: an unparseable value is silently treated as 0, matching {@code swscanf(..., L"%04hX", ...)}'s behavior on no match. */
	private static int getAddress(JTextField field) {
		try {
			return Integer.parseInt(field.getText().trim(), 16) & 0xFFFF;
		} catch (NumberFormatException ex) {
			return 0;
		}
	}

	private static String buildHexDump(byte[] buffer) {
		StringBuilder text = new StringBuilder();
		for (int lineOffset = 0; lineOffset < buffer.length; lineOffset += BYTES_PER_LINE) {
			int lineEnd = Math.min(lineOffset + BYTES_PER_LINE, buffer.length);
			text.append(String.format("%06X: ", lineOffset));
			for (int i = lineOffset; i < lineOffset + BYTES_PER_LINE; i++) {
				text.append(i < lineEnd ? String.format("%02X ", buffer[i] & 0xFF) : "   ");
			}
			text.append(' ');
			for (int i = lineOffset; i < lineEnd; i++) {
				int value = buffer[i] & 0xFF;
				text.append(value >= 32 && value < 127 ? (char) value : '.');
			}
			text.append('\n');
		}
		return text.toString();
	}

	/**
	 * Ported from RawFileDialog::Show/InitDialog/OnOK, folded into one
	 * blocking call as is idiomatic for a Swing modal {@link JDialog}.
	 * Returns {@code true} if the user clicked OK; {@link #getFileBuffer}/
	 * {@link #getBegin}/{@link #getResultSize}/{@link #getAddress} then give the
	 * result, matching {@code GetFileBuffer}/{@code GetResult}.
	 */
	public boolean show(File file) throws IOException {
		fileBuffer = Files.readAllBytes(file.toPath());
		if (fileBuffer.length == 0) {
			JOptionPane.showMessageDialog(this, "File is empty.", "Open Raw File", JOptionPane.ERROR_MESSAGE);
			return false;
		}

		filePathField.setText(file.getPath());
		hexDumpArea.setText(buildHexDump(fileBuffer));
		hexDumpArea.setCaretPosition(0);
		startOffsetField.setText("0");
		endOffsetField.setText(String.valueOf(fileBuffer.length - 1));
		addressField.setText("");
		okButton.setEnabled(false);

		confirmed = false;
		setVisible(true); // Blocks until disposed/hidden - this is a modal dialog.

		return confirmed;
	}

	public byte[] getFileBuffer() {
		return fileBuffer;
	}

	public int getBegin() {
		return resultBegin;
	}

	public int getResultSize() {
		return resultSize;
	}

	public int getAddress() {
		return resultAddress;
	}
}
