/*
 * Copyright (C) 2026 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of dis6502.
 */
package com.wudsn.tools.dis6502.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import com.wudsn.tools.base.common.TextUtility;
import com.wudsn.tools.base.gui.ElementFactory;
import com.wudsn.tools.base.repository.Action;
import com.wudsn.tools.dis6502.Actions;
import com.wudsn.tools.dis6502.Texts;
import com.wudsn.tools.dis6502.model.DisassemblyLine;
import com.wudsn.tools.dis6502.model.DisassemblyResult;
import com.wudsn.tools.dis6502.model.Equate;
import com.wudsn.tools.dis6502.model.SegmentList;

/**
 * A read-only view of the current {@link DisassemblyResult}'s lines, plus a
 * text search that drives the cross-reference list (see {@link
 * #findField}/{@link #findButton}/{@link #findNextButton}, wired up by
 * {@code Dis6502} to {@link DisassemblyResult#findAndSelectLines} and
 * {@link XRefPanel}) - {@code findButton} runs a fresh search (matching
 * {@code MainDisassembly::Find}), {@code findNextButton} continues it
 * (matching {@code MainDisassembly::FindNextString(false)}, bound in C++
 * to ID_DIS_FIND_NEXT).
 * <p>
 * Ported from ui/DisassemblyWindow.h / DisassemblyWindow.cpp and
 * ui/DisassemblyControl(Impl).h/.cpp. The listing itself is {@link
 * DisassemblyGridPanel}, a custom-painted, read-only list using the real
 * per-computer-system bitmap glyphs from {@link ComputerFont} - see that
 * class's javadoc for why (the C++ source draws this listing with the same
 * font as the memory inspector, not a plain system font) and {@link
 * DisassemblyGridPanel}'s own javadoc for exactly which parts of {@code
 * DisassemblyControlImpl.cpp} this replicates (plain text layout,
 * virtualized via Swing's clip-rect repaint) and which it does not (inline
 * editing, the full popup menu - neither existed in this port before this
 * rewrite either). {@link #navigateToLine} (scrolling to and highlighting
 * a line, used by both Find/XRef navigation and {@link #selectLineAt})
 * and {@link #selectLineAt} itself (clicking or dragging in the listing to
 * select a line, reported to {@code Dis6502} via {@link
 * #setLineSelectionListener} - ported from {@code
 * DisassemblyControlImpl::MouseMove}'s per-line click handling, see that
 * method's javadoc for the {@code dwLastLine} correspondence) are the
 * pieces of the C++ control's selection behavior this port implements. A
 * plain click/drag never navigates away from the clicked line - only a
 * double-click or Return does, via {@link #navigateToDefinitionOfSelectedLine}/
 * {@link #setNavigateToDefinitionListener}, ported from {@code
 * DisassemblyControlImpl::FindReference}/{@code MainDisassembly::FindDef}
 * (see {@code Dis6502#performFindDisassemblyReferences}'s javadoc for a bug
 * this port used to have here: navigating on every plain click).
 * {@link #setComputerFont} must be called by {@code Dis6502} whenever the
 * workspace's computer system or double-font-height setting changes,
 * matching {@code DisassemblyWindow}'s use of {@code
 * WorkspaceFont::GetResizedFont}.
 * <p>
 * The right-click popup menu ({@link #maybeShowPopup}) is a still-reduced
 * port of ui/DisassemblyPopupMenu.h/.cpp's {@code DISASSEMBLY_POPUP_MENU}:
 * Add/Edit Comment, Find/Find Next, and - since {@link #findLabelInLine}
 * turned out to need nothing more than the clicked line's own text (see
 * that method's javadoc for why this is not the "mouse-position label
 * parsing" it first looked like) - Find Definition/References, Rename,
 * and Address Range too, reusing already-ported pieces ({@link
 * DisassemblyResult#findAndSelectLines}/{@link
 * DisassemblyResult#findDefinitionLineNumber}, {@code Equate.extractAddress}/
 * {@code isAutomaticLabel}, {@link EquateDialog}/{@link
 * EquateRangeDialog}) - see {@code Dis6502}'s wiring of {@link
 * #findDefMenuItem}/{@link #findRef1MenuItem}/{@link #findRef2MenuItem}/
 * {@link #renameDefMenuItem}/{@link #renameRefMenuItem}/{@link
 * #addrRangeDefMenuItem}/{@link #addrRangeRefMenuItem}. Still not ported:
 * Back in History (needs a navigation history stack this port does not
 * have) and the per-instruction Set Type submenu (a different, per-
 * clicked-instruction mechanism from the memory inspector popup's
 * selection-based one, needing "is this an immediate-mode instruction"
 * detection this port does not have either).
 * <p>
 * Every popup item is public and wired by {@code Dis6502} (unlike {@link
 * MemoryInspectorPanel}'s popup items, they need a parent {@link
 * java.awt.Frame}/{@code Workspace} this panel does not have), reading
 * {@link #getRightClickedLine}/{@link #getRightClickedLabelDefinition}/
 * {@link #getRightClickedLabelReference} - all three frozen at the moment
 * the popup was shown, matching {@code MainDisassembly::DrawMenu} capturing
 * {@code disSelection}/{@code labelDefinition}/{@code labelReference} once
 * per right-click rather than re-deriving them when a menu item is later
 * clicked. Every static item is built via {@code
 * com.wudsn.tools.base.gui.ElementFactory} from an {@code Action} in
 * {@code com.wudsn.tools.dis6502.Actions}, the same as {@link MainMenu};
 * the seven items whose label names an actual label ({@link #findDefMenuItem}
 * and friends) stay plain {@code JMenuItem}s built with no label at all,
 * since their text is {@code "{0}"}-templated and only resolved at
 * popup-show time - see {@link #setDynamicLabel} and {@code Actions}' own
 * javadoc. {@link #editCommentMenuItem}'s Add/Edit Comment, unlike {@code
 * MainDisassembly::AddComment} (which always passes the sentinel size
 * {@code 0xFFFF} for {@link CommentDialog} to snap to the enclosing
 * instruction via {@code DisassemblyResult::findOffsetAtStartOfInstruction}),
 * uses the clicked line's own {@code offset}/{@code size} directly -
 * correct for the common case of right-clicking an actual instruction
 * line, though not necessarily identical for a label/equate-only line
 * with no byte size of its own; that model-layer method is ported and
 * available, just not wired up for this yet.
 *
 * @author Peter Dell
 */
public final class DisassemblyPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	private final DisassemblyGridPanel grid = new DisassemblyGridPanel();
	public final JTextField findField = new JTextField(24);
	public final JButton findButton = new JButton("Find");
	public final JButton findNextButton = new JButton("Find Next");
	public final JMenuItem editCommentMenuItem = ElementFactory.createMenuItem(Actions.DisassemblyPopupMenu_EditComment, "editCommentMenuItem");
	// findDefMenuItem/findRef1MenuItem/findRef2MenuItem/renameDefMenuItem/renameRefMenuItem/
	// addrRangeDefMenuItem/addrRangeRefMenuItem stay plain JMenuItems, not built via
	// ElementFactory.createMenuItem: their label is "{0}"-templated and only known once
	// the popup is about to show - see setDynamicLabel/maybeShowPopup.
	public final JMenuItem findDefMenuItem = new JMenuItem();
	public final JMenuItem findRef1MenuItem = new JMenuItem();
	public final JMenuItem findRef2MenuItem = new JMenuItem();
	public final JMenuItem renameDefMenuItem = new JMenuItem();
	public final JMenuItem renameRefMenuItem = new JMenuItem();
	public final JMenuItem addrRangeDefMenuItem = new JMenuItem();
	public final JMenuItem addrRangeRefMenuItem = new JMenuItem();

	private final PartHeaderPanel header = new PartHeaderPanel(new Color(0, 255, 0));
	private final JPopupMenu popupMenu = new JPopupMenu();
	private final JMenuItem popupFindMenuItem = ElementFactory.createMenuItem(Actions.DisassemblyPopupMenu_Find, "popupFindMenuItem");
	private final JMenuItem popupFindNextMenuItem = ElementFactory.createMenuItem(Actions.DisassemblyPopupMenu_FindNext,
			"popupFindNextMenuItem");

	private final Map<Integer, Integer> lineNumberToIndex = new HashMap<>();
	private List<DisassemblyLine> disassemblyLines = Collections.emptyList();
	private DisassemblyLine rightClickedLine;
	private String rightClickedLabelDefinition = "";
	private String rightClickedLabelReference = "";
	private String selectedLabelReference = "";
	private LineSelectionListener lineSelectionListener;
	private NavigateToDefinitionListener navigateToDefinitionListener;
	private int lastSelectedLineIndex = -1;

	public DisassemblyPanel() {
		super(new BorderLayout());
		header.setText(Texts.DisassemblyPanel_Title);

		JPanel findButtonsPanel = new JPanel();
		findButtonsPanel.add(findButton);
		findButtonsPanel.add(findNextButton);

		JPanel findPanel = new JPanel(new BorderLayout(4, 4));
		findPanel.add(findField, BorderLayout.CENTER);
		findPanel.add(findButtonsPanel, BorderLayout.EAST);

		JPanel topPanel = new JPanel(new BorderLayout());
		topPanel.add(header, BorderLayout.NORTH);
		topPanel.add(findPanel, BorderLayout.SOUTH);
		add(topPanel, BorderLayout.NORTH);

		JScrollPane scrollPane = new JScrollPane(grid);
		// Splitters already separate the part windows - the scroll pane's own
		// L&F-default border would just draw a redundant line right next to them.
		scrollPane.setBorder(BorderFactory.createEmptyBorder());
		add(scrollPane, BorderLayout.CENTER);

		popupFindMenuItem.addActionListener(e -> findButton.doClick());
		popupFindNextMenuItem.addActionListener(e -> findNextButton.doClick());

		grid.setFocusable(true);
		MouseAdapter mouseHandler = new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				grid.requestFocusInWindow();
				maybeShowPopup(e);
				if (SwingUtilities.isLeftMouseButton(e) && !e.isPopupTrigger()) {
					lastSelectedLineIndex = -1;
					selectLineAt(e);
				}
			}

			@Override
			public void mouseDragged(MouseEvent e) {
				if (SwingUtilities.isLeftMouseButton(e)) {
					selectLineAt(e);
				}
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				maybeShowPopup(e);
			}

			@Override
			public void mouseClicked(MouseEvent e) {
				if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
					navigateToDefinitionOfSelectedLine();
				}
			}
		};
		grid.addMouseListener(mouseHandler);
		grid.addMouseMotionListener(mouseHandler);

		// Bound WHEN_FOCUSED, not WHEN_IN_FOCUSED_WINDOW like MemoryInspectorPanel's
		// F2/Esc: unlike those, this action's state (selectedLabelReference) only
		// ever comes from clicking in this grid, so there is nothing to gain from a
		// window-wide accelerator, and scoping it to the grid's own focus avoids
		// stealing Enter from unrelated focused components elsewhere in the window.
		grid.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "navigateToDefinition");
		grid.getActionMap().put("navigateToDefinition", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				navigateToDefinitionOfSelectedLine();
			}
		});

		// Ctrl+Shift+F/Shift+F3, from Actions.DisassemblyPopupMenu_Find/_FindNext
		// (see POPUP_MENU_ACCELERATORS_PLAN.md for how they were verified), bound
		// WHEN_IN_FOCUSED_WINDOW like MemoryInspectorPanel's own popup-menu
		// accelerators - unlike Return above, these carry no risk of stealing a
		// common key from an unrelated focused component, so no such narrowing is
		// needed. Calling findButton/findNextButton.doClick() directly - not
		// popupFindMenuItem/popupFindNextMenuItem.doClick() - sidesteps relying on
		// those popup items' own liveness entirely: the same empirical finding
		// documented in MemoryInspectorPanel.bindPopupMenuAccelerators applies
		// here too (a standalone JPopupMenu's item accelerators, which {@code
		// ElementFactory.createMenuItem} applies automatically from these same
		// Actions, only fire while that popup instance is open), and this panel's
		// popup is rebuilt from scratch on every right-click besides - hence the
		// isPopupMenuVisible() guard below, deferring to the item's own
		// accelerator whenever the popup happens to already be showing.
		bindAccelerator(Actions.DisassemblyPopupMenu_Find, findButton::doClick);
		bindAccelerator(Actions.DisassemblyPopupMenu_FindNext, findNextButton::doClick);
	}

	private boolean isPopupMenuVisible() {
		return popupMenu.isVisible();
	}

	private int popupAcceleratorCounter;

	private void bindAccelerator(Action action, Runnable command) {
		String actionKey = "popupAccelerator" + popupAcceleratorCounter++;
		grid.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(action.getAccelerator(), actionKey);
		grid.getActionMap().put(actionKey, new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (!isPopupMenuVisible()) {
					command.run();
				}
			}
		});
	}

	/**
	 * Ported from {@code DisassemblyControlImpl::FindReference} (double-click)
	 * and {@code MainDisassembly::FindDef} (Return, {@code ID_DIS_FIND_DEF}) -
	 * both navigate to the definition of the currently selected line's
	 * referenced label, unlike a plain click/drag (see {@link #selectLineAt}),
	 * which only reports the selection and refreshes the XRef list, never
	 * navigating away from the clicked line.
	 */
	private void navigateToDefinitionOfSelectedLine() {
		if (navigateToDefinitionListener != null && !selectedLabelReference.isEmpty()) {
			navigateToDefinitionListener.onNavigateToDefinition(selectedLabelReference);
		}
	}

	/**
	 * Ported from the per-line click handling in {@code
	 * DisassemblyControlImpl::MouseMove} (reached here through a single
	 * click/drag rather than continuous mouse-capture tracking, since
	 * that's the idiomatic Swing shape for this - {@link
	 * #lastSelectedLineIndex} plays the same role as the C++ source's
	 * {@code dwLastLine}, only re-notifying {@link #lineSelectionListener}
	 * when the line under the cursor actually changes): highlights the
	 * clicked line and reports it - along with the label its operand
	 * references, or the one it defines if it has no reference (matching
	 * {@code MainDisassembly::Proc}'s DIS_XREF handler: {@code label =
	 * GetLabelReference(); if empty, label = GetLabelDefinition()}) - to
	 * {@link #lineSelectionListener}.
	 */
	private void selectLineAt(MouseEvent e) {
		int index = grid.lineIndexAtY(e.getY());
		if (index < 0 || index >= disassemblyLines.size() || index == lastSelectedLineIndex) {
			return;
		}
		lastSelectedLineIndex = index;
		grid.highlightLine(index);

		DisassemblyLine line = disassemblyLines.get(index);
		LabelsInLine labels = findLabelInLine(line.getLine());
		selectedLabelReference = labels.reference;

		if (lineSelectionListener != null) {
			String label = !labels.reference.isEmpty() ? labels.reference : labels.definition;
			lineSelectionListener.onLineSelected(line, label);
		}
	}

	/** Reports the line the user clicked or dragged to in the listing, and the label it defines/references - see {@link #selectLineAt}. */
	public void setLineSelectionListener(LineSelectionListener lineSelectionListener) {
		this.lineSelectionListener = lineSelectionListener;
	}

	/** Reports a request to navigate to a label's definition - see {@link #navigateToDefinitionOfSelectedLine}. */
	public void setNavigateToDefinitionListener(NavigateToDefinitionListener navigateToDefinitionListener) {
		this.navigateToDefinitionListener = navigateToDefinitionListener;
	}

	/**
	 * Ported from DisassemblyControlImpl::RButtonDown/MainDisassembly::DrawMenu.
	 * Rebuilds the popup's item set from scratch every time, the idiomatic
	 * Swing way to do what the C++ version's {@code DisassemblyPopupMenu::
	 * Update} does by editing a fixed .rc-defined template in place ({@code
	 * SetText}/{@code DeleteEntry}/{@code DeleteSepartor}).
	 */
	private void maybeShowPopup(MouseEvent e) {
		if (!e.isPopupTrigger()) {
			return;
		}
		int index = grid.lineIndexAtY(e.getY());
		rightClickedLine = (index >= 0 && index < disassemblyLines.size()) ? disassemblyLines.get(index) : null;
		LabelsInLine labels = findLabelInLine(rightClickedLine == null ? "" : rightClickedLine.getLine());
		rightClickedLabelDefinition = labels.definition;
		rightClickedLabelReference = labels.reference;

		boolean hasDefinition = !rightClickedLabelDefinition.isEmpty();
		boolean hasReference = !rightClickedLabelReference.isEmpty();
		boolean definitionAutomatic = hasDefinition && Equate.isAutomaticLabel(rightClickedLabelDefinition);
		boolean referenceAutomatic = hasReference && Equate.isAutomaticLabel(rightClickedLabelReference);

		popupMenu.removeAll();
		if (hasReference) {
			setDynamicLabel(findDefMenuItem, Actions.DisassemblyPopupMenu_FindDef, rightClickedLabelReference);
			popupMenu.add(findDefMenuItem);
			popupMenu.addSeparator();
		}

		editCommentMenuItem
				.setEnabled(rightClickedLine != null && rightClickedLine.segmentIndex != SegmentList.NO_SEGMENT_INDEX);
		popupMenu.add(editCommentMenuItem);
		popupMenu.addSeparator();

		popupFindMenuItem.setEnabled(findButton.isEnabled());
		popupMenu.add(popupFindMenuItem);
		popupFindNextMenuItem.setEnabled(findNextButton.isEnabled());
		popupMenu.add(popupFindNextMenuItem);
		if (hasDefinition) {
			setDynamicLabel(findRef2MenuItem, Actions.DisassemblyPopupMenu_FindRef2, rightClickedLabelDefinition);
			popupMenu.add(findRef2MenuItem);
		}
		if (hasReference) {
			setDynamicLabel(findRef1MenuItem, Actions.DisassemblyPopupMenu_FindRef1, rightClickedLabelReference);
			popupMenu.add(findRef1MenuItem);
		}

		if (definitionAutomatic || referenceAutomatic) {
			popupMenu.addSeparator();
			if (definitionAutomatic) {
				setDynamicLabel(renameDefMenuItem, Actions.DisassemblyPopupMenu_RenameDef, rightClickedLabelDefinition);
				popupMenu.add(renameDefMenuItem);
			}
			if (referenceAutomatic && !rightClickedLabelReference.equals(rightClickedLabelDefinition)) {
				setDynamicLabel(renameRefMenuItem, Actions.DisassemblyPopupMenu_RenameRef, rightClickedLabelReference);
				popupMenu.add(renameRefMenuItem);
			}
			popupMenu.addSeparator();
			if (definitionAutomatic) {
				setDynamicLabel(addrRangeDefMenuItem, Actions.DisassemblyPopupMenu_AddrRangeDef, rightClickedLabelDefinition);
				popupMenu.add(addrRangeDefMenuItem);
			}
			if (referenceAutomatic) {
				setDynamicLabel(addrRangeRefMenuItem, Actions.DisassemblyPopupMenu_AddrRangeRef, rightClickedLabelReference);
				popupMenu.add(addrRangeRefMenuItem);
			}
		}

		popupMenu.show(grid, e.getX(), e.getY());
	}

	/**
	 * Applies {@code action}'s {@code "{0}"}-templated label to {@code item},
	 * substituting {@code label} and re-deriving the mnemonic from the
	 * resulting text - see {@code com.wudsn.tools.dis6502.Actions}' own class
	 * javadoc for why this can't just be {@code
	 * ElementFactory.setButtonTextAndMnemonic(item, action)} directly (that
	 * would apply the un-substituted {@code "{0}"} template).
	 */
	private static void setDynamicLabel(JMenuItem item, Action action, String label) {
		String text = TextUtility.format(action.getLabel(), label);
		ElementFactory.setButtonTextAndMnemonic(item, new Action(text, action.getToolTip(), action.getAccelerator()));
	}

	/**
	 * The two labels {@link #findLabelInLine} can find in a disassembly line -
	 * the label it defines (a leading identifier at column 0) and the label its
	 * operand references - either of which may be {@code ""} if not present.
	 */
	private static final class LabelsInLine {
		final String definition;
		final String reference;

		LabelsInLine(String definition, String reference) {
			this.definition = definition;
			this.reference = reference;
		}
	}

	/**
	 * Splits {@code text} into the label it defines and the label its operand
	 * references. Ported from {@code DisassemblyControlImpl::FindLabelInLine} -
	 * despite that method's name suggesting it needs to know where in the line
	 * the mouse was, it only ever parses the whole line's text structurally
	 * (leading identifier = definition; skip the mnemonic; an operand starting
	 * with a letter/@/_ after an optional #/(/&gt;/&lt; prefix = reference), with
	 * no mouse position involved at all.
	 */
	private static LabelsInLine findLabelInLine(String text) {
		int[] index = { 0 };
		String labelDefinition = "";
		String labelReference = "";

		char c = charAt(text, index);
		if (isLabelStartChar(c)) {
			StringBuilder definition = new StringBuilder();
			while (isLabelChar(c)) {
				definition.append(c);
				c = charAt(text, index);
			}
			labelDefinition = definition.toString();
			while (c != '\0' && c != ' ') {
				c = charAt(text, index);
			}
			while (c == ' ') {
				c = charAt(text, index);
			}
		} else if (c != ' ') {
			return new LabelsInLine("", "");
		}

		// Skip the mnemonic.
		while (c == ' ') {
			c = charAt(text, index);
		}
		while (c != '\0' && c != ' ') {
			c = charAt(text, index);
		}
		while (c == ' ') {
			c = charAt(text, index);
		}

		if (c == '"') {
			return new LabelsInLine(labelDefinition, "");
		}
		if (c == '#' || c == '(') {
			c = charAt(text, index);
			while (c == ' ') {
				c = charAt(text, index);
			}
			if (c == '>' || c == '<') {
				c = charAt(text, index);
				while (c == ' ') {
					c = charAt(text, index);
				}
			}
		}
		if (isLabelStartChar(c)) {
			StringBuilder reference = new StringBuilder();
			while (isLabelChar(c)) {
				reference.append(c);
				c = charAt(text, index);
			}
			if (reference.length() > 1) {
				labelReference = reference.toString();
			}
		}
		return new LabelsInLine(labelDefinition, labelReference);
	}

	/** Package-visible so {@link DisassemblyGridPanel}'s syntax-coloring state machine can reuse it, rather than duplicating this same char-scanning idiom. */
	static char charAt(String text, int[] index) {
		char c = index[0] < text.length() ? text.charAt(index[0]) : '\0';
		index[0]++;
		return c;
	}

	/** Package-visible - see {@link #charAt}. */
	static boolean isLabelStartChar(char c) {
		return c == '@' || c == '_' || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
	}

	private static boolean isLabelChar(char c) {
		return isLabelStartChar(c) || (c >= '0' && c <= '9');
	}

	/** The disassembly line last right-clicked to show the popup menu, or {@code null} if it wasn't over a line tied to segment data. */
	public DisassemblyLine getRightClickedLine() {
		return rightClickedLine;
	}

	/** The label the right-clicked line defines (a leading identifier at column 0), or {@code ""} if it does not define one. */
	public String getRightClickedLabelDefinition() {
		return rightClickedLabelDefinition;
	}

	/** The label the right-clicked line's operand references, or {@code ""} if it does not reference one. */
	public String getRightClickedLabelReference() {
		return rightClickedLabelReference;
	}

	/** Ported from DisassemblyWindow's use of WorkspaceFont::GetResizedFont - call whenever the workspace's computer system or double-height setting changes. */
	public void setComputerFont(ComputerFont computerFont) {
		grid.setComputerFont(computerFont);
		header.setComputerFont(computerFont);
	}

	/** See {@link DisassemblyGridPanel#setLineNumbersActive} - call alongside every {@link #refresh}, matching {@code MainDisassembly::RefreshDisControl}. */
	public void setLineNumbersActive(boolean lineNumbersActive) {
		grid.setLineNumbersActive(lineNumbersActive);
	}

	public void refresh(DisassemblyResult disassemblyResult) {
		lineNumberToIndex.clear();
		rightClickedLine = null;
		lastSelectedLineIndex = -1;

		if (disassemblyResult == null || disassemblyResult.getLineCount() == 0) {
			disassemblyLines = Collections.emptyList();
			grid.setLines(Collections.emptyList());
			return;
		}

		List<DisassemblyLine> newDisassemblyLines = new ArrayList<>();
		DisassemblyResult.LineIterator iterator = disassemblyResult.createLineIterator();
		while (iterator.hasNext()) {
			DisassemblyLine line = iterator.next();
			lineNumberToIndex.put(line.getLineNumber(), newDisassemblyLines.size());
			newDisassemblyLines.add(line);
		}
		disassemblyLines = newDisassemblyLines;
		grid.setLines(newDisassemblyLines);
	}

	/**
	 * Scrolls to and highlights the given disassembly line number, matching
	 * the effect of {@code DisassemblyControl::SelectLine}. Returns {@code
	 * false} if {@code lineNumber} is not part of the currently displayed
	 * disassembly (e.g. stale after a new one replaced it).
	 */
	public boolean navigateToLine(int lineNumber) {
		Integer index = lineNumberToIndex.get(lineNumber);
		if (index == null) {
			return false;
		}
		grid.highlightLine(index);
		return true;
	}

	/** Reports a line the user clicked or dragged to - see {@link #selectLineAt}. */
	public interface LineSelectionListener {
		void onLineSelected(DisassemblyLine line, String label);
	}

	/** Reports a Return-key/double-click request to navigate to a label's definition - see {@link #setNavigateToDefinitionListener}. */
	public interface NavigateToDefinitionListener {
		void onNavigateToDefinition(String label);
	}
}
