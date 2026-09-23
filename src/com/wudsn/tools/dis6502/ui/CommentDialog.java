/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.ui;

import java.awt.BorderLayout;
import java.awt.Frame;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import com.wudsn.tools.base.Actions;
import com.wudsn.tools.base.gui.ElementFactory;
import com.wudsn.tools.dis6502.Texts;
import com.wudsn.tools.dis6502.model.SegmentList;

/**
 * A dialog for editing the user comment attached to a byte range.
 * <p>
 * Folded into one blocking {@link #show} call, as is idiomatic for a Swing
 * modal {@link JDialog}. Wired from both the memory inspector's own byte
 * selection ({@code Dis6502#performEditMemoryInspectorComment}) and a
 * right-clicked disassembly line ({@code
 * Dis6502#performEditDisassemblyComment}); the disassembly path passes the
 * clicked line's own {@code offset}/{@code size} directly, without
 * snapping to the enclosing instruction - see {@link
 * com.wudsn.tools.dis6502.ui.DisassemblyPanel}'s javadoc. Like {@link
 * SegmentPropertiesDialog}/{@link WorkspaceDialog}, the mutation ({@link
 * SegmentList#setUserComment}) happens directly in {@link #performOK}, not
 * left to the caller.
 *
 * @author Peter Dell
 */
public final class CommentDialog extends JDialog {

	private static final long serialVersionUID = 1L;

	private final JTextArea commentArea = new JTextArea(6, 40);

	private SegmentList segmentList;
	private int segmentIndex;
	private int offset;
	private int size;
	private boolean confirmed;

	public CommentDialog(Frame owner) {
		super(owner, true);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setTitle(Texts.CommentDialog_Title);

		commentArea.setLineWrap(true);
		commentArea.setWrapStyleWord(true);

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

		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(new JScrollPane(commentArea), BorderLayout.CENTER);
		getContentPane().add(buttonPanel, BorderLayout.SOUTH);
		pack();
		setLocationRelativeTo(owner);

		getRootPane().setDefaultButton(okButton);
		ElementUtilities.closeOnEscape(this, cancelButton::doClick);
	}

	/** Commits the edited comment and closes the dialog. */
	private void performOK() {
		segmentList.setUserComment(segmentIndex, offset, size, commentArea.getText());
		confirmed = true;
		setVisible(false);
	}

	/** Opens the dialog pre-filled with the existing comment; returns whether the user clicked OK. */
	public boolean show(SegmentList segmentList, int segmentIndex, int offset, int size) {
		this.segmentList = segmentList;
		this.segmentIndex = segmentIndex;
		this.offset = offset;
		this.size = size;

		commentArea.setText(segmentList.getUserComment(segmentIndex, offset, size));

		confirmed = false;
		setVisible(true); // Blocks until disposed/hidden - this is a modal dialog.

		return confirmed;
	}
}
