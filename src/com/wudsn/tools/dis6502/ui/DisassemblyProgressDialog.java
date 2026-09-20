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
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import com.wudsn.tools.base.Actions;
import com.wudsn.tools.base.gui.ElementFactory;
import com.wudsn.tools.dis6502.model.Disassembly;
import com.wudsn.tools.dis6502.model.DisassemblyProgressMonitor;

/**
 * A modal "please wait" dialog shown while a {@link Disassembly} runs, with
 * a Cancel button.
 * <p>
 * Ported from ui/DisassemblyProgressDialog.h/.cpp, but not its mechanism:
 * the C++ version runs the disassembly synchronously on the dialog's own
 * message-handling thread ({@code InitDialog} posts {@code
 * WM_USER_COMMAND}, whose handler calls {@code disassembly->
 * DisassembleInternal()} directly) and lets the user click Cancel by
 * manually pumping the Windows message queue from inside {@code
 * IsCancelled} ({@code PeekMessage}/{@code TranslateMessage}/{@code
 * DispatchMessage}, every 16 calls) - a Win32-specific technique with no
 * good Swing equivalent (nested event-queue pumping on the EDT is fragile
 * and discouraged). Instead, {@link Monitor#disassembleInternal} runs the
 * actual work on a {@link SwingWorker} background thread while this dialog
 * blocks the EDT showing progress, and {@link Monitor#isCancelled} reads a
 * plain {@link AtomicBoolean} the Cancel button's action listener sets -
 * idiomatic Swing background-work handling in place of the C++ mechanism,
 * with the same effect: {@link Disassembly} already checks {@code
 * isCancelled()} frequently (once per byte read, via {@code getNextByte}),
 * throwing an internal {@code DisassemblyCancelledException} each pass
 * method already catches, so no other change was needed to make
 * cancellation actually stop the work.
 * <p>
 * {@link #getMonitor()} returns the {@link DisassemblyProgressMonitor} to
 * pass to {@link Disassembly#setProgressMonitor} - {@code Dis6502} creates
 * one of these per disassembly run and uses its monitor exactly where a
 * plain {@link DisassemblyProgressMonitor} was used before.
 *
 * @author Peter Dell
 */
public final class DisassemblyProgressDialog extends JDialog {

	private static final long serialVersionUID = 1L;

	private final JLabel passLabel = new JLabel("-");
	private final JLabel segmentLabel = new JLabel("-");
	private final AtomicBoolean cancelled = new AtomicBoolean();
	private final DisassemblyProgressMonitor monitor = new Monitor();

	public DisassemblyProgressDialog(Frame owner) {
		super(owner, "6502 Disassembler", true);
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

		JPanel content = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(4, 4, 4, 4);

		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 2;
		content.add(new JLabel("Disassembling - Please wait..."), c);

		c.gridwidth = 1;
		c.gridy = 1;
		c.gridx = 0;
		c.anchor = GridBagConstraints.EAST;
		content.add(new JLabel("Pass:"), c);
		c.gridx = 1;
		c.anchor = GridBagConstraints.WEST;
		content.add(passLabel, c);

		c.gridy = 2;
		c.gridx = 0;
		c.anchor = GridBagConstraints.EAST;
		content.add(new JLabel("Segment:"), c);
		c.gridx = 1;
		c.anchor = GridBagConstraints.WEST;
		content.add(segmentLabel, c);

		JButton cancelButton = ElementFactory.createButton(Actions.ButtonBar_Cancel, true);
		cancelButton.addActionListener(e -> cancelled.set(true));
		JPanel buttonPanel = new JPanel();
		buttonPanel.add(cancelButton);

		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(content, BorderLayout.CENTER);
		getContentPane().add(buttonPanel, BorderLayout.SOUTH);
		pack();
		setLocationRelativeTo(owner);
	}

	public DisassemblyProgressMonitor getMonitor() {
		return monitor;
	}

	private final class Monitor extends DisassemblyProgressMonitor {

		/** Ported from DisassemblyProgressDialog::DisassembleInternal/InitDialog - see this class's javadoc for why the mechanism differs. */
		@Override
		protected void disassembleInternal(Disassembly disassembly) {
			cancelled.set(false);
			SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
				@Override
				protected Void doInBackground() {
					disassembly.disassembleInternal();
					return null;
				}

				@Override
				protected void done() {
					setVisible(false);
				}
			};
			worker.execute();
			setVisible(true); // Blocks until done() hides the dialog - this is a modal dialog.
		}

		/** Ported from DisassemblyProgressDialog::SetPass. */
		@Override
		public void setPass(String pass) {
			super.setPass(pass);
			SwingUtilities.invokeLater(() -> {
				passLabel.setText(pass);
				segmentLabel.setText("-");
			});
		}

		/** Ported from DisassemblyProgressDialog::SetSegmentNumber. */
		@Override
		public void setSegmentNumber(int segmentNumber) {
			SwingUtilities.invokeLater(() -> segmentLabel.setText(String.valueOf(segmentNumber)));
		}

		/** Ported from DisassemblyProgressDialog::IsCancelled, minus the message-pumping - see this class's javadoc. */
		@Override
		public boolean isCancelled() {
			return cancelled.get();
		}
	}
}
