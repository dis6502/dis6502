/*
 * Copyright (C) 2025 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import com.wudsn.tools.base.gui.ElementFactory;

/**
 * The application's About box.
 * <p>
 * Ported from ui/AboutDialog.h/.cpp and the {@code ABOUTBOX} resource in
 * dis6502.rc: the {@link #dis6502Icon}/{@link #alfredBitmap} images
 * (converted from {@code dis6502.ico}/{@code alfred.bmp} to PNG - see {@code
 * MainMenu#applyIcons}'s javadoc for why - and loaded the same way, via
 * {@link ElementFactory#createImageIcon}) and the {@code CTEXT} lines, laid
 * out with plain Swing layout instead of the .rc's pixel-exact control
 * coordinates - see the {@code ui} package's general porting note on Swing
 * idioms vs. literal Win32 translation. The C++ source's text is reused
 * verbatim here (this dialog replaces {@code Dis6502#performAbout}'s
 * previous plain-text {@link javax.swing.JOptionPane}, which already carried
 * that same text forward from the .rc), except for one already-established
 * difference kept as-is: "James Wilkinson,james@slor.net" in the .rc has no
 * space after the comma, apparently a typo, and the Java text already reads
 * "James Wilkinson, james@slor.net".
 * <p>
 * {@code IDC_LIST_VERSION}, the C++ source's per-module version/description
 * listbox, is not ported: it reads {@code DIS6502.exe}'s own Win32 file
 * version resource via {@code GetFileVersionInfo}, which has no meaningful
 * equivalent for a launched-from-a-jar Java application (there is no single
 * versioned native module to query the way a real .exe carries its own
 * embedded version resource) - dropped rather than faked with a placeholder.
 * {@code WM_CTLCOLORSTATIC}/{@code WM_CTLCOLORBTN}/{@code WM_CTLCOLORDLG}
 * forcing every control's background to white is matched by giving the
 * content pane an explicit white background instead of each control
 * individually, since Swing components already inherit their container's
 * background by default.
 *
 * @author Peter Dell
 */
public final class AboutDialog extends JDialog {

	private static final long serialVersionUID = 1L;

	public AboutDialog(Frame owner) {
		super(owner, "About DIS6502", true);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		JPanel contentPanel = new JPanel(new BorderLayout(8, 8));
		contentPanel.setBackground(Color.WHITE);
		contentPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

		ImageIcon dis6502Icon = ElementFactory.createImageIcon("images/dis6502.png");
		ImageIcon alfredBitmap = ElementFactory.createImageIcon("images/alfred.png");

		JPanel headerPanel = new JPanel(new BorderLayout(8, 0));
		headerPanel.setBackground(Color.WHITE);

		JPanel titlePanel = new JPanel(new GridLayout(2, 1));
		titlePanel.setBackground(Color.WHITE);
		JLabel titleLabel = new JLabel("DIS6502", SwingConstants.CENTER);
		titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 14f));
		JLabel subtitleLabel = new JLabel("The 6502 Disassembler", SwingConstants.CENTER);
		titlePanel.add(titleLabel);
		titlePanel.add(subtitleLabel);

		headerPanel.add(new JLabel(dis6502Icon), BorderLayout.WEST);
		headerPanel.add(titlePanel, BorderLayout.CENTER);
		headerPanel.add(new JLabel(alfredBitmap), BorderLayout.EAST);
		contentPanel.add(headerPanel, BorderLayout.NORTH);

		String[] textLines = { " ", "http://sourceforge.net/projects/dis6502", " ",
				"(c) 1997-2024 Eric Bacher, atari@ebacher.info", "Win32 Port - 2005 by James Wilkinson, james@slor.net",
				"Win32 Fixes - 2015-2024 by Peter Dell, jac@wudsn.com", " ",
				"The purpose of this software is to disassemble a 6502", "binary file and generate a listing ready to assemble.",
				" ", "Feel free to send any comments, new ideas, or", "bug reports on SourceForge." };
		JPanel textPanel = new JPanel(new GridLayout(textLines.length, 1));
		textPanel.setBackground(Color.WHITE);
		for (String line : textLines) {
			textPanel.add(new JLabel(line, SwingConstants.CENTER));
		}
		contentPanel.add(textPanel, BorderLayout.CENTER);

		JButton okButton = new JButton("OK");
		okButton.addActionListener(e -> setVisible(false));
		JPanel buttonPanel = new JPanel();
		buttonPanel.setBackground(Color.WHITE);
		buttonPanel.add(okButton);
		contentPanel.add(buttonPanel, BorderLayout.SOUTH);

		setContentPane(contentPanel);
		getRootPane().setDefaultButton(okButton);
		pack();
		setResizable(false);
		setLocationRelativeTo(owner);
	}
}
