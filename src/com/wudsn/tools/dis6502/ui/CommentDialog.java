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

import com.wudsn.tools.base.Actions;
import com.wudsn.tools.base.gui.ElementFactory;
import com.wudsn.tools.dis6502.model.SegmentList;

/**
 * A dialog for editing the user comment attached to a byte range.
 * <p>
 * Ported from ui/CommentDialog.h / CommentDialog.cpp, folded into one
 * blocking {@link #show} call as is idiomatic for a Swing modal
 * {@link JDialog}. Wired from both the memory inspector's own byte
 * selection ({@code Dis6502#performEditMemoryInspectorComment}) and a
 * right-clicked disassembly line ({@code Dis6502#performEditDisassemblyComment}),
 * matching the C++ version's two trigger paths - but unlike the C++
 * version, which always passes the sentinel {@code size} {@code 0xFFFF}
 * for the disassembly-line path (snapped via {@code
 * DisassemblyResult::FindOffsetAtStartOfInstruction} to the enclosing
 * instruction), the disassembly path here passes the clicked line's own
 * {@code offset}/{@code size} directly - that snapping is not ported, see
 * {@link com.wudsn.tools.dis6502.ui.DisassemblyPanel}'s javadoc. Like
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
