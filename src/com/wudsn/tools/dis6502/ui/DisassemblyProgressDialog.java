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
import com.wudsn.tools.dis6502.Application;
import com.wudsn.tools.dis6502.DataTypes;
import com.wudsn.tools.dis6502.Texts;
import com.wudsn.tools.dis6502.model.Disassembly;
import com.wudsn.tools.dis6502.model.DisassemblyProgressMonitor;

/**
 * A modal "please wait" dialog shown while a {@link Disassembly} runs, with
 * a Cancel button.
 * <p>
 * {@link Monitor#disassembleInternal} runs the actual work on a {@link
 * SwingWorker} background thread while this dialog blocks the EDT showing
 * progress, and {@link Monitor#isCancelled} reads a plain {@link
 * AtomicBoolean} the Cancel button's action listener sets. {@link
 * Disassembly} already checks {@code isCancelled()} frequently (once per
 * byte read, via {@code getNextByte}), throwing an internal {@code
 * DisassemblyCancelledException} each pass method already catches, so no
 * other change was needed to make cancellation actually stop the work.
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
	private final DisassemblyProgressMonitor monitor;

	public DisassemblyProgressDialog(Frame owner, Application application) {
		super(owner, "6502 Disassembler", true);
		monitor = new Monitor(application);
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

		JPanel content = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(4, 4, 4, 4);

		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 2;
		content.add(new JLabel(Texts.DisassemblyProgressDialog_PleaseWaitMessage), c);

		c.gridwidth = 1;
		c.gridy = 1;
		c.gridx = 0;
		c.anchor = GridBagConstraints.EAST;
		content.add(ElementFactory.createLabel(DataTypes.DisassemblyProgressDialog_Pass, passLabel), c);
		c.gridx = 1;
		c.anchor = GridBagConstraints.WEST;
		content.add(passLabel, c);

		c.gridy = 2;
		c.gridx = 0;
		c.anchor = GridBagConstraints.EAST;
		content.add(ElementFactory.createLabel(DataTypes.DisassemblyProgressDialog_Segment, segmentLabel), c);
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

		ElementUtilities.closeOnEscape(this, cancelButton::doClick);
	}

	public DisassemblyProgressMonitor getMonitor() {
		return monitor;
	}

	private final class Monitor extends DisassemblyProgressMonitor {

		Monitor(Application application) {
			super(application);
		}

		/** Runs the disassembly on a background thread, blocking this dialog until it finishes. */
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

		/**
		 * Sets the inherited {@code pass} field directly rather than calling
		 * {@code super.setPass(pass)}, so this override never logs - only
		 * updates the dialog's labels.
		 */
		@Override
		public void setPass(String pass) {
			this.pass = pass;
			SwingUtilities.invokeLater(() -> {
				passLabel.setText(pass);
				segmentLabel.setText("-");
			});
		}

		/** Like {@link #setPass}, deliberately never calls {@code super.setSegmentNumber(segmentNumber)}, so it doesn't log either. */
		@Override
		public void setSegmentNumber(int segmentNumber) {
			SwingUtilities.invokeLater(() -> segmentLabel.setText(String.valueOf(segmentNumber)));
		}

		/** Reads whatever the Cancel button's action listener last set. */
		@Override
		public boolean isCancelled() {
			return cancelled.get();
		}
	}
}
