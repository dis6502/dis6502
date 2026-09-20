/*
 * Copyright (C) 2025 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import com.wudsn.tools.base.Actions;
import com.wudsn.tools.base.gui.Desktop;
import com.wudsn.tools.base.gui.ElementFactory;
import com.wudsn.tools.dis6502.Texts;

/**
 * The application's About box.
 * <p>
 * Ported from ui/AboutDialog.h/.cpp and the {@code ABOUTBOX} resource in
 * dis6502.rc: the dialog/product icon and alfred bitmap images (converted
 * from {@code dis6502.ico}/{@code alfred.bmp} to PNG - see {@code
 * MainMenu#applyIcons}'s javadoc for why - and loaded the same way, via
 * {@link ElementFactory#createImageIcon}) and the {@code CTEXT} lines - held
 * as {@link Texts#AboutDialog_WindowTitle}/{@link Texts#AboutDialog_Title}/
 * {@link Texts#AboutDialog_Subtitle}/{@link Texts#AboutDialog_Text} (the last
 * one, one line per {@code \n}) rather than literal strings here, matching
 * every other user-visible string in this project - see {@link Texts}'s own
 * javadoc for why these live there and not in {@link Text}, which is
 * reserved for real ported {@code IDS_*} mirrors. Laid out with plain Swing
 * layout instead of the .rc's pixel-exact control coordinates - see the
 * {@code ui} package's general porting note on Swing idioms vs. literal
 * Win32 translation. This dialog replaces {@code Dis6502#performAbout}'s
 * previous plain-text {@link javax.swing.JOptionPane}, which already carried
 * this same text forward, except for one already-established difference
 * kept as-is: "James Wilkinson,james@slor.net" in the .rc has no space
 * after the comma, apparently a typo, and the Java text already reads
 * "James Wilkinson, james@slor.net".
 * <p>
 * {@code IDC_LIST_VERSION}, the C++ source's per-module version/description
 * listbox, is not ported: it reads {@code DIS6502.exe}'s own Win32 file version
 * resource via {@code GetFileVersionInfo}, which has no meaningful equivalent
 * for a launched-from-a-jar Java application (there is no single versioned
 * native module to query the way a real .exe carries its own embedded version
 * resource) - dropped rather than faked with a placeholder.
 * {@code WM_CTLCOLORSTATIC}/{@code WM_CTLCOLORBTN}/{@code WM_CTLCOLORDLG}
 * forcing every control's background to white is matched by giving the content
 * pane an explicit white background instead of each control individually, since
 * Swing components already inherit their container's background by default.
 *
 * @author Peter Dell
 */
public final class AboutDialog extends JDialog {

	private static final long serialVersionUID = 1L;

	public AboutDialog(Frame owner) {
		super(owner, Texts.AboutDialog_WindowTitle, true);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		JPanel contentPanel = new JPanel(new BorderLayout(8, 8));
		contentPanel.setBackground(Color.WHITE);
		contentPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

		ImageIcon dis6502Icon = ElementFactory.createImageIcon("images/main-64x64.png");
		ImageIcon alfredBitmap = ElementFactory.createImageIcon("images/alfred.png");

		JPanel headerPanel = new JPanel(new BorderLayout(8, 0));
		headerPanel.setBackground(Color.WHITE);

		JPanel titlePanel = new JPanel(new GridLayout(2, 1));
		titlePanel.setBackground(Color.WHITE);
		JLabel titleLabel = new JLabel(Texts.AboutDialog_Title, SwingConstants.CENTER);
		titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 14f));
		JLabel subtitleLabel = new JLabel(Texts.AboutDialog_Subtitle, SwingConstants.CENTER);
		titlePanel.add(titleLabel);
		titlePanel.add(subtitleLabel);

		headerPanel.add(new JLabel(dis6502Icon), BorderLayout.WEST);
		headerPanel.add(titlePanel, BorderLayout.CENTER);
		headerPanel.add(new JLabel(alfredBitmap), BorderLayout.EAST);
		contentPanel.add(headerPanel, BorderLayout.NORTH);

		String[] textLines = Texts.AboutDialog_Text.split("\n");
		JPanel textPanel = new JPanel(new GridLayout(textLines.length, 1));
		textPanel.setBackground(Color.WHITE);
		for (String line : textLines) {
			textPanel.add(line.startsWith("https://") ? createLinkLabel(line) : new JLabel(line, SwingConstants.CENTER));
		}
		contentPanel.add(textPanel, BorderLayout.CENTER);

		JButton okButton = ElementFactory.createButton(Actions.ButtonBar_OK, true);
		okButton.addActionListener(e -> setVisible(false));
		JPanel buttonPanel = new JPanel();
		buttonPanel.setBackground(Color.WHITE);
		buttonPanel.add(okButton);
		contentPanel.add(buttonPanel, BorderLayout.SOUTH);

		setContentPane(contentPanel);
		getRootPane().setDefaultButton(okButton);
		ElementUtilities.closeOnEscape(this, okButton::doClick);

		pack();
		setResizable(false);
		setLocationRelativeTo(owner);
	}

	private static JLabel createLinkLabel(String url) {
		JLabel label = new JLabel("<html><a href=\"" + url + "\">" + url + "</a></html>", SwingConstants.CENTER);
		label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		label.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				Desktop.openBrowser(url);
			}
		});
		return label;
	}
}
