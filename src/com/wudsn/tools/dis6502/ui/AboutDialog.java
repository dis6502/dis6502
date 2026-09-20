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
import com.wudsn.tools.dis6502.Text;

/**
 * The application's About box.
 * <p>
 * Ported from ui/AboutDialog.h/.cpp and the {@code ABOUTBOX} resource in
 * dis6502.rc: the {@link #dis6502Icon}/{@link #alfredBitmap} images (converted
 * from {@code dis6502.ico}/{@code alfred.bmp} to PNG - see {@code
 * MainMenu#applyIcons}'s javadoc for why - and loaded the same way, via
 * {@link ElementFactory#createImageIcon}) and the {@code CTEXT} lines - held
 * as {@link Text#IDS_ABOUT_TEXT}, one line per {@code \n}, rather than a
 * literal array here, matching every other user-visible string in this
 * project (see {@link Text}'s own javadoc for why that field is this file's
 * one exception to "only real {@code IDS_*} mirrors live there") - laid out
 * with plain Swing layout instead of the .rc's pixel-exact control coordinates
 * - see the {@code ui} package's general porting note on Swing idioms vs.
 * literal Win32 translation. This dialog replaces {@code
 * Dis6502#performAbout}'s previous plain-text {@link
 * javax.swing.JOptionPane}, which already carried this same text forward,
 * except for one already-established difference kept as-is:
 * "James Wilkinson,james@slor.net" in the .rc has no space after the comma,
 * apparently a typo, and the Java text already reads "James Wilkinson,
 * james@slor.net".
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
		super(owner, "About DIS6502", true);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		JPanel contentPanel = new JPanel(new BorderLayout(8, 8));
		contentPanel.setBackground(Color.WHITE);
		contentPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

		ImageIcon dis6502Icon = ElementFactory.createImageIcon("images/main-32x32.png");
		ImageIcon alfredBitmap = ElementFactory.createImageIcon("images/alfred.png");

		JPanel headerPanel = new JPanel(new BorderLayout(8, 0));
		headerPanel.setBackground(Color.WHITE);

		JPanel titlePanel = new JPanel(new GridLayout(2, 1));
		titlePanel.setBackground(Color.WHITE);
		JLabel titleLabel = new JLabel("DIS6502", SwingConstants.CENTER);
		titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 14f));
		JLabel subtitleLabel = new JLabel("The Interactive MOS 6502 Disassembler", SwingConstants.CENTER);
		titlePanel.add(titleLabel);
		titlePanel.add(subtitleLabel);

		headerPanel.add(new JLabel(dis6502Icon), BorderLayout.WEST);
		headerPanel.add(titlePanel, BorderLayout.CENTER);
		headerPanel.add(new JLabel(alfredBitmap), BorderLayout.EAST);
		contentPanel.add(headerPanel, BorderLayout.NORTH);

		String[] textLines = Text.IDS_ABOUT_TEXT.split("\n");
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

		// No C++ counterpart to port: AboutDialog::ProcessDialogMessage never
		// handles WM_KEYDOWN/VK_ESCAPE, relying on the OS-level modal dialog
		// default of Esc mapping to IDCANCEL - which ProcessCommand treats the
		// same as IDOK (both just close the dialog, matching okButton's own
		// listener above).
		ElementUtilities.closeOnEscape(this, okButton::doClick);

		pack();
		setResizable(false);
		setLocationRelativeTo(owner);
	}

	/**
	 * No C++ counterpart: {@code CTEXT} is plain static text, not a clickable
	 * control, so the .rc source's URL line was never a real link either.
	 * {@code <html><a href="...">} gets {@link JLabel} to render the usual
	 * blue/underlined link look for free (Swing's own basic HTML support,
	 * per the user's own suggestion) - it does not make the label clickable
	 * by itself, so a {@link MouseAdapter} plus a hand {@link Cursor} do the
	 * rest, opening the URL via {@link Desktop#openBrowser}, the same
	 * shared helper {@code com.wudsn.tools.thecartstudio.ui.AboutDialog}
	 * already uses for its own About link.
	 */
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
