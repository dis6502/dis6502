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
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.wudsn.tools.base.Actions;
import com.wudsn.tools.base.gui.ElementFactory;
import com.wudsn.tools.dis6502.DataTypes;
import com.wudsn.tools.dis6502.Texts;
import com.wudsn.tools.dis6502.model.MutableByteRangeSelection;
import com.wudsn.tools.dis6502.model.system.atari800.DiskImage;
import com.wudsn.tools.dis6502.model.system.atari800.ImgInfo;
import com.wudsn.tools.dis6502.model.system.atari800.ImgRWPacket;

/**
 * A dialog for picking one or more byte ranges, from any sector of a disk
 * image, to import as segments at chosen addresses.
 * <p>
 * A single class: its non-UI state ({@link ImgRWPacket}, the current
 * sector number/size, the picked items) has no reason to be a separate
 * object here. The sector's bytes are shown via the same {@link
 * HexGridPanel} the main Memory Inspector uses (fed by a {@link
 * DiskSectorByteSource} rather than a {@code Segment}, and its own
 * independent {@link MutableByteRangeSelection} rather than the
 * workspace's). {@link #performAddSector} reads the current selection
 * synchronously, defaulting to the whole sector if nothing was dragged.
 * Sector navigation is a plain {@link JSpinner} - matching the pattern
 * already used for {@link RawFileDialog}.
 * <p>
 * {@link #readSector} returns a sector's full data; the caller (see
 * {@code Dis6502.openDiskImageSectors}) copies {@code item.size} bytes
 * starting at {@code item.begin} out of that, rather than {@link
 * #readSector} itself returning an already-sliced, item-sized buffer -
 * the dialog's own sector buffer is reused across reads, not a
 * transferable per-item allocation, so slicing has to happen on a
 * caller-owned copy instead.
 *
 * @author Peter Dell
 */
public final class DiskImageSectorsDialog extends JDialog {

	private static final long serialVersionUID = 1L;

	private final JTextField diskImageFilePathField = new JTextField();
	private final SpinnerNumberModel sectorSpinnerModel = new SpinnerNumberModel(1, 1, 1, 1);
	private final JSpinner sectorSpinner = new JSpinner(sectorSpinnerModel);
	private final HexGridPanel grid = new HexGridPanel();
	private final MutableByteRangeSelection selection = new MutableByteRangeSelection();
	private final JTextField addressField = new JTextField(6);
	// Fully qualified: com.wudsn.tools.base.Actions is already imported as "Actions" for ButtonBar_OK/Cancel below.
	private final JButton addSectorButton = ElementFactory.createButton(com.wudsn.tools.dis6502.Actions.DiskImageSectorsDialog_AddSector, true);
	private final JButton removeSectorButton = ElementFactory.createButton(com.wudsn.tools.dis6502.Actions.DiskImageSectorsDialog_RemoveSector,
			true);
	private final JButton okButton = ElementFactory.createButton(Actions.ButtonBar_OK, true);
	private final DefaultListModel<Item> itemsListModel = new DefaultListModel<>();
	private final JList<Item> itemsList = new JList<>(itemsListModel);

	private final ImgRWPacket sector = new ImgRWPacket();
	private int currentSectorNumber;
	private int currentSectorSize;
	private boolean confirmed;

	public DiskImageSectorsDialog(Frame owner) {
		super(owner, true);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setTitle(Texts.DiskImageSectorsDialog_Title);

		diskImageFilePathField.setEditable(false);
		itemsList.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

		grid.setSelection(selection);
		grid.setDragSelectionListener((begin, end) -> {
			selection.setSelection(begin, end, currentSectorSize);
			grid.refreshSelection();
		});

		sectorSpinner.addChangeListener(e -> loadAndDisplaySector((Integer) sectorSpinner.getValue()));

		DocumentListener addressListener = new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) {
				updateAddSectorButtonEnabled();
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				updateAddSectorButtonEnabled();
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				updateAddSectorButtonEnabled();
			}
		};
		addressField.getDocument().addDocumentListener(addressListener);

		addSectorButton.setEnabled(false);
		addSectorButton.addActionListener(e -> performAddSector());

		removeSectorButton.setEnabled(false);
		removeSectorButton.addActionListener(e -> performRemoveSector());
		itemsList.addListSelectionListener(e -> {
			if (!e.getValueIsAdjusting()) {
				removeSectorButton.setEnabled(!itemsList.isSelectionEmpty());
			}
		});

		okButton.setEnabled(false);
		okButton.addActionListener(e -> {
			confirmed = true;
			setVisible(false);
		});
		JButton cancelButton = ElementFactory.createButton(Actions.ButtonBar_Cancel, true);
		cancelButton.addActionListener(e -> {
			confirmed = false;
			setVisible(false);
		});

		JPanel topPanel = new JPanel(new BorderLayout(4, 4));
		topPanel.add(ElementFactory.createLabel(DataTypes.DiskImageSectorsDialog_DiskImageFile, diskImageFilePathField), BorderLayout.WEST);
		topPanel.add(diskImageFilePathField, BorderLayout.CENTER);
		topPanel.add(sectorSpinner, BorderLayout.EAST);

		JPanel fieldsPanel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(2, 4, 2, 4);
		c.anchor = GridBagConstraints.WEST;
		c.gridx = 0;
		c.gridy = 0;
		fieldsPanel.add(ElementFactory.createLabel(DataTypes.DiskImageSectorsDialog_Address, addressField), c);
		c.gridx = 1;
		fieldsPanel.add(addressField, c);
		c.gridx = 2;
		fieldsPanel.add(addSectorButton, c);

		JPanel centerPanel = new JPanel(new BorderLayout(4, 4));
		JScrollPane gridScrollPane = new JScrollPane(grid);
		gridScrollPane.setBorder(BorderFactory.createEmptyBorder());
		centerPanel.add(gridScrollPane, BorderLayout.CENTER);

		JPanel itemsPanel = new JPanel(new BorderLayout(4, 4));
		itemsPanel.add(new JLabel(Texts.DiskImageSectorsDialog_SectorsToAddLabel), BorderLayout.NORTH);
		itemsPanel.add(new JScrollPane(itemsList), BorderLayout.CENTER);
		itemsPanel.add(removeSectorButton, BorderLayout.SOUTH);

		JPanel middlePanel = new JPanel(new GridLayout(1, 2, 4, 4));
		middlePanel.add(centerPanel);
		middlePanel.add(itemsPanel);

		JPanel buttonPanel = new JPanel();
		buttonPanel.add(okButton);
		buttonPanel.add(cancelButton);

		JPanel southPanel = new JPanel(new BorderLayout());
		southPanel.add(fieldsPanel, BorderLayout.NORTH);
		southPanel.add(buttonPanel, BorderLayout.SOUTH);

		getContentPane().setLayout(new BorderLayout(4, 4));
		getContentPane().add(topPanel, BorderLayout.NORTH);
		getContentPane().add(middlePanel, BorderLayout.CENTER);
		getContentPane().add(southPanel, BorderLayout.SOUTH);
		setSize(1000, 560);
		setLocationRelativeTo(owner);

		getRootPane().setDefaultButton(okButton);
		ElementUtilities.closeOnEscape(this, cancelButton::doClick);
	}

	/** The font the sector's bytes are painted with - see {@link HexGridPanel#setComputerFont}. */
	public void setComputerFont(ComputerFont computerFont) {
		grid.setComputerFont(computerFont);
	}

	private void updateAddSectorButtonEnabled() {
		int[] ignored = new int[1];
		addSectorButton.setEnabled(tryParseAddress(addressField.getText(), ignored));
	}

	/** Parses a plain hexadecimal address into {@code address[0]}; returns whether it succeeded. */
	private static boolean tryParseAddress(String text, int[] address) {
		String trimmed = text.trim();
		if (trimmed.isEmpty()) {
			return false;
		}
		try {
			address[0] = Integer.parseInt(trimmed, 16) & 0xFFFF;
			return true;
		} catch (NumberFormatException ex) {
			return false;
		}
	}

	/** Adds the current selection (or the whole sector, if nothing is selected) as a new item, and advances to the next sector. */
	private void performAddSector() {
		int[] address = new int[1];
		if (!tryParseAddress(addressField.getText(), address)) {
			return;
		}

		// The whole current sector if nothing was dragged - begin/end are already
		// swapped/clamped by MutableByteRangeSelection.setSelection at drag time.
		int begin = 0;
		int end = Math.max(currentSectorSize - 1, 0);
		if (selection.hasSelection()) {
			begin = selection.getBegin();
			end = selection.getEnd();
		}
		int size = end - begin + 1;

		itemsListModel.addElement(new Item(currentSectorNumber, address[0], begin, size));
		okButton.setEnabled(true);

		addressField.setText(String.format("%04X", (address[0] + size) & 0xFFFF));

		if (currentSectorNumber != (Integer) sectorSpinnerModel.getMaximum()) {
			sectorSpinnerModel.setValue(currentSectorNumber + 1); // Fires the spinner's ChangeListener.
		}
	}

	/** Removes the selected items from the list. */
	private void performRemoveSector() {
		int[] selectedIndices = itemsList.getSelectedIndices();
		for (int i = selectedIndices.length - 1; i >= 0; i--) {
			itemsListModel.remove(selectedIndices[i]);
		}
		removeSectorButton.setEnabled(false);
		okButton.setEnabled(!itemsListModel.isEmpty());
	}

	/** Reads and displays sector {@code sectorNumber}, clearing any selection. */
	private void loadAndDisplaySector(int sectorNumber) {
		currentSectorNumber = sectorNumber;
		int[] size = new int[1];
		DiskImage.readAbsoluteSector(sector, sectorNumber, size);
		currentSectorSize = size[0];

		grid.setByteSource(new DiskSectorByteSource(sector.sectorData, currentSectorSize, 0));
		selection.clearSelection();
		grid.refreshSelection();
	}

	/**
	 * Reads a sector's full data - used by the caller to re-read each picked
	 * {@link Item}'s sector when building segments from {@link #getItems},
	 * since only the selected sub-range, not the whole sector, is kept in
	 * each item.
	 */
	public byte[] readSector(int sectorNumber, int[] sizeOut) {
		DiskImage.readAbsoluteSector(sector, sectorNumber, sizeOut);
		return sector.sectorData;
	}

	public List<Item> getItems() {
		List<Item> result = new ArrayList<>();
		for (int i = 0; i < itemsListModel.size(); i++) {
			result.add(itemsListModel.get(i));
		}
		return result;
	}

	/**
	 * Opens the dialog as one blocking call, idiomatic for a Swing modal
	 * {@link JDialog}. {@code diskInfo} must already be a successfully
	 * recognized disk image (checked by the caller via {@link
	 * DiskImage#getInfo}/{@link DiskImage#isError} before showing this
	 * dialog). Returns {@code true} if the user added at least one sector
	 * range and clicked OK; {@link #getItems} then gives the picked ranges.
	 */
	public boolean show(String diskImageFilePath, ImgInfo diskInfo) {
		diskImageFilePathField.setText(diskImageFilePath);

		sector.filePath = diskImageFilePath;
		sector.sectorSize = diskInfo.density;

		itemsListModel.clear();
		addressField.setText("");
		okButton.setEnabled(false);
		removeSectorButton.setEnabled(false);

		sectorSpinnerModel.setMinimum(1);
		sectorSpinnerModel.setMaximum(Math.max(diskInfo.sectors, 1));
		sectorSpinnerModel.setValue(1);
		loadAndDisplaySector(1);

		confirmed = false;
		setVisible(true); // Blocks until disposed/hidden - this is a modal dialog.

		return confirmed;
	}

	/** One picked byte range: sector, load address, and the offset/size within that sector. */
	public static final class Item {
		public final int sectorNumber;
		public final int address;
		public final int begin;
		public final int size;

		Item(int sectorNumber, int address, int begin, int size) {
			this.sectorNumber = sectorNumber;
			this.address = address;
			this.begin = begin;
			this.size = size;
		}

		@Override
		public String toString() {
			return String.format("%6d %04X %02X    %04X", sectorNumber, address, begin, size);
		}
	}
}
