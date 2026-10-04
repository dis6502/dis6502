/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.ui;

import java.awt.BorderLayout;
import java.awt.Frame;

import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import com.wudsn.tools.base.gui.ModalDialog;
import com.wudsn.tools.dis6502.Texts;
import com.wudsn.tools.dis6502.model.SegmentList;

/**
 * A dialog for editing the user comment attached to a byte range.
 * <p>
 * Shown by one blocking {@link #show} call - a WUDSN Base {@link
 * ModalDialog}. Wired from both the memory inspector's own byte
 * selection ({@code Dis6502#performEditMemoryInspectorComment}) and a
 * right-clicked disassembly line ({@code
 * Dis6502#performEditDisassemblyComment}); the disassembly path passes the
 * clicked line's own {@code offset}/{@code size} directly, without
 * snapping to the enclosing instruction - see {@link
 * com.wudsn.tools.dis6502.ui.DisassemblyPanel}'s javadoc. Like {@link
 * SegmentPropertiesDialog}/{@link WorkspaceDialog}, the mutation ({@link
 * SegmentList#setUserComment}) happens directly in {@link #validateOK}, not
 * left to the caller.
 *
 * @author Peter Dell
 */
public final class CommentDialog extends ModalDialog {

	private static final long serialVersionUID = 1L;

	private final JTextArea commentArea = new JTextArea(6, 40);

	private SegmentList segmentList;
	private int segmentIndex;
	private int offset;
	private int size;

	public CommentDialog(Frame owner) {
		super(owner, Texts.CommentDialog_Title);

		commentArea.setLineWrap(true);
		commentArea.setWrapStyleWord(true);

		getContentPane().add(new JScrollPane(commentArea), BorderLayout.CENTER);
	}

	/** Commits the edited comment. */
	@Override
	protected boolean validateOK() {
		segmentList.setUserComment(segmentIndex, offset, size, commentArea.getText());
		return true;
	}

	/** Opens the dialog pre-filled with the existing comment; returns whether the user clicked OK. */
	public boolean show(SegmentList segmentList, int segmentIndex, int offset, int size) {
		this.segmentList = segmentList;
		this.segmentIndex = segmentIndex;
		this.offset = offset;
		this.size = size;

		commentArea.setText(segmentList.getUserComment(segmentIndex, offset, size));

		showModal(commentArea);
		return okPressed;
	}
}
