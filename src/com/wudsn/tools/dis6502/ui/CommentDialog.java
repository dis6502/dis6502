/*
 * Copyright (C) 2025 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
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

import com.wudsn.tools.dis6502.model.SegmentList;

/**
 * A dialog for editing the user comment attached to a byte range.
 * <p>
 * Ported from ui/CommentDialog.h / CommentDialog.cpp, folded into one
 * blocking {@link #show} call as is idiomatic for a Swing modal
 * {@link JDialog} - unlike the C++ version, which is reachable both from
 * a disassembly line (with {@code size} possibly the sentinel
 * {@code 0xFFFF}, snapped via {@code DisassemblyResult::FindOffsetAtStartOfInstruction}
 * to the enclosing instruction) and from the memory inspector's own byte
 * selection, this is wired only from the memory inspector (see {@code
 * Dis6502#performEditMemoryInspectorComment}), so {@code size} here is
 * always a real byte count - the snapping case does not apply. Like
 * {@link SegmentPropertiesDialog}/{@link WorkspaceDialog}, the mutation
 * ({@link SegmentList#setUserComment}) happens directly in {@link
 * #performOK}, not left to the caller.
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
		setTitle("Comment");

		commentArea.setLineWrap(true);
		commentArea.setWrapStyleWord(true);

		JButton okButton = new JButton("OK");
		okButton.addActionListener(e -> performOK());
		JButton cancelButton = new JButton("Cancel");
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
	}

	/** Ported from CommentDialog::OnOK, folded together with its Show's final SetUserComment call. */
	private void performOK() {
		segmentList.setUserComment(segmentIndex, offset, size, commentArea.getText());
		confirmed = true;
		setVisible(false);
	}

	/** Ported from CommentDialog::Show/InitDialog. */
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
