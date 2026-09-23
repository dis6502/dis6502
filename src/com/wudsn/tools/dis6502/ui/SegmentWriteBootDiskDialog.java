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
import java.io.IOException;
import java.util.Arrays;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.wudsn.tools.base.Actions;
import com.wudsn.tools.base.gui.ElementFactory;
import com.wudsn.tools.dis6502.DataTypes;
import com.wudsn.tools.dis6502.Messages;
import com.wudsn.tools.dis6502.Texts;
import com.wudsn.tools.dis6502.model.Segment;
import com.wudsn.tools.dis6502.model.system.atari800.AtariDOS;
import com.wudsn.tools.dis6502.model.system.atari800.AtariDisk;
import com.wudsn.tools.dis6502.model.system.atari800.AtariError;
import com.wudsn.tools.dis6502.model.system.atari800.AtariFile;
import com.wudsn.tools.dis6502.model.system.atari800.DiskImage;

/**
 * Turns a segment into a bootable Atari DOS disk: overwrites an existing
 * disk image's first sectors (bypassing its file system entirely) with the
 * segment's bytes, preceded by a 6-byte boot header (sector count, load
 * address, init address).
 * <p>
 * Folded into one {@link #show} call, as is idiomatic for a Swing modal
 * {@link JDialog}, the same way {@link CommentDialog}/{@link
 * AssembleDialog} etc. do. {@code writeBootDisk} stays here rather than
 * moving to the model layer. Like {@link AssembleDialog}, {@link
 * #performOK} only closes the dialog once a target file has actually been
 * written or a real error occurred - cancelling the save-file chooser
 * leaves the dialog open.
 * <p>
 * TODO: {@link DiskImage#writeAbsoluteSector} never reports a write
 * failure (e.g. a write-protected target disk image) back to {@link
 * #writeBootDisk}. This dialog can therefore report success while having
 * written nothing.
 *
 * @author Peter Dell
 */
public final class SegmentWriteBootDiskDialog extends JDialog {

	private static final long serialVersionUID = 1L;

	private final JTextField loadAddressField = new JTextField(4);
	private final JTextField initAddressField = new JTextField(4);
	private final JButton okButton = ElementFactory.createButton(Actions.ButtonBar_OK, true);

	private Segment segment;

	public SegmentWriteBootDiskDialog(Frame owner) {
		super(owner, true);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setTitle(Texts.SegmentWriteBootDiskDialog_Title);

		JPanel panel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(4, 4, 4, 4);
		c.anchor = GridBagConstraints.EAST;

		c.gridx = 0;
		c.gridy = 0;
		panel.add(ElementFactory.createLabel(DataTypes.SegmentWriteBootDiskDialog_LoadAddress, loadAddressField), c);
		c.gridx = 1;
		c.anchor = GridBagConstraints.WEST;
		panel.add(loadAddressField, c);

		c.gridx = 0;
		c.gridy = 1;
		c.anchor = GridBagConstraints.EAST;
		panel.add(ElementFactory.createLabel(DataTypes.SegmentWriteBootDiskDialog_InitAddress, initAddressField), c);
		c.gridx = 1;
		c.anchor = GridBagConstraints.WEST;
		panel.add(initAddressField, c);

		okButton.addActionListener(e -> performOK());
		JButton cancelButton = ElementFactory.createButton(Actions.ButtonBar_Cancel, true);
		cancelButton.addActionListener(e -> setVisible(false));

		JPanel buttonPanel = new JPanel();
		buttonPanel.add(okButton);
		buttonPanel.add(cancelButton);

		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(panel, BorderLayout.CENTER);
		getContentPane().add(buttonPanel, BorderLayout.SOUTH);

		DocumentListener updateOKEnabled = new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) {
				update();
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				update();
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				update();
			}

			private void update() {
				okButton.setEnabled(!loadAddressField.getText().isEmpty() && !initAddressField.getText().isEmpty());
			}
		};
		loadAddressField.getDocument().addDocumentListener(updateOKEnabled);
		initAddressField.getDocument().addDocumentListener(updateOKEnabled);

		pack();
		setLocationRelativeTo(owner);

		getRootPane().setDefaultButton(okButton);
		ElementUtilities.closeOnEscape(this, cancelButton::doClick);
	}

	/** Prompts for a target file and writes the boot disk to it. */
	private void performOK() {
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setDialogTitle(Texts.SegmentWriteBootDiskDialog_Title);
		if (fileChooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
			return;
		}
		File file = fileChooser.getSelectedFile();
		try {
			writeBootDisk(file.getPath());
			setVisible(false);
		} catch (IOException ex) {
			JOptionPane.showMessageDialog(this, ex.getMessage(), Texts.SegmentWriteBootDiskDialog_Title, JOptionPane.ERROR_MESSAGE);
			setVisible(false);
		}
	}

	/** Writes the boot header plus the segment's data across {@code filePath}'s first sectors. */
	private void writeBootDisk(String filePath) throws IOException {
		AtariFile info = new AtariFile();
		AtariDisk atariDisk = AtariDOS.openAtariDisk(filePath);
		AtariError error = atariDisk.findFirst(info);

		switch (error) {
		case NO_ENTRY_FOUND:
			// ERROR: No directory entries found in the disk image.
			throw new IOException(Messages.E067.format());

		case OK: {
			int headerSize = 6;
			int segmentOffset = 0;
			int dataSize = segment.getSize();
			int sectorCount = (dataSize + headerSize + 127) / 128;

			int loadAddress = Integer.parseInt(loadAddressField.getText(), 16);
			int initAddress = Integer.parseInt(initAddressField.getText(), 16);

			byte[] sector = new byte[128];
			sector[0] = 0;
			sector[1] = (byte) sectorCount; // Boot images can only have up to 255 sectors.
			sector[2] = (byte) (loadAddress & 0xFF);
			sector[3] = (byte) ((loadAddress >> 8) & 0xFF);
			sector[4] = (byte) (initAddress & 0xFF);
			sector[5] = (byte) ((initAddress >> 8) & 0xFF);

			for (int sectorNumber = 1; sectorNumber <= sectorCount; sectorNumber++) {
				int sectorSize = Math.min(128 - headerSize, dataSize);
				Arrays.fill(sector, headerSize, 128, (byte) 0);
				System.arraycopy(segment.memoryBlock.getData(), segmentOffset, sector, headerSize, sectorSize);

				DiskImage.writeAbsoluteSector(filePath, sectorNumber, sector);

				headerSize = 0;
				segmentOffset += sectorSize;
				dataSize -= sectorSize;
			}
			break;
		}

		case DISK_NOT_FOUND:
			// Messages.E007 ("Error: {0}") has one placeholder, filled with filePath.
			// ERROR: {0}
			throw new IOException(Messages.E007.format(filePath));

		default:
			// Messages.E036 has no placeholder, so filePath is silently dropped here.
			// ERROR: Disk image is corrupted or not an Atari single/enhanced density disk or file too big
			throw new IOException(Messages.E036.format());
		}
	}

	/** Opens the dialog with the segment's load address pre-filled. */
	public void show(Segment segment, boolean withInitAddress, int initAddress) {
		this.segment = segment;
		loadAddressField.setText(String.format("%04X", segment.wBegin));
		initAddressField.setText(withInitAddress ? String.format("%04X", initAddress) : "");
		okButton.setEnabled(!initAddressField.getText().isEmpty());
		setVisible(true); // Blocks until disposed/hidden - this is a modal dialog.
	}
}
