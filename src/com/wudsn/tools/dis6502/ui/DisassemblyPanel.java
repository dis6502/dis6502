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
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.wudsn.tools.base.common.TextUtility;
import com.wudsn.tools.base.gui.ElementFactory;
import com.wudsn.tools.base.repository.Action;
import com.wudsn.tools.dis6502.Actions;
import com.wudsn.tools.dis6502.Texts;
import com.wudsn.tools.dis6502.model.DisassemblyLine;
import com.wudsn.tools.dis6502.model.DisassemblyResult;
import com.wudsn.tools.dis6502.model.Equate;
import com.wudsn.tools.dis6502.model.LineNumberHistory;
import com.wudsn.tools.dis6502.model.MemoryType;
import com.wudsn.tools.dis6502.model.SegmentList;

/**
 * A read-only view of the current {@link DisassemblyResult}'s lines, plus a
 * text search that drives the cross-reference list (see {@link
 * #findField}/{@link #findButton}/{@link #findNextButton}, wired up by
 * {@code Dis6502} to {@link DisassemblyResult#findAndSelectLines} and
 * {@link XRefPanel}) - {@code findButton} runs a fresh search, {@code
 * findNextButton} continues it.
 * <p>
 * The listing itself is {@link DisassemblyGridPanel}, a custom-painted,
 * read-only list using the real per-computer-system bitmap glyphs from
 * {@link ComputerFont} - see that class's javadoc for why - and {@link
 * DisassemblyGridPanel}'s own javadoc for exactly what it implements
 * (plain text layout, virtualized via Swing's clip-rect repaint) and what
 * it does not (inline editing, the full popup menu). {@link
 * #navigateToLine} (scrolling to and highlighting a line, used by both
 * Find/XRef navigation and {@link #selectLineAt}) and {@link
 * #selectLineAt} itself (clicking or dragging in the listing to select a
 * line, reported to {@code Dis6502} via {@link #setLineSelectionListener})
 * are the pieces of this listing's selection behavior this panel owns. A
 * plain click/drag never navigates away from the clicked line - only a
 * double-click or Return does, via {@link
 * #navigateToDefinitionOfSelectedLine}/{@link
 * #setNavigateToDefinitionListener} (see {@code
 * Dis6502#performFindDisassemblyReferences}'s javadoc for a bug this port
 * used to have here: navigating on every plain click). {@link
 * #setComputerFont} must be called by {@code Dis6502} whenever the
 * workspace's computer system or double-font-height setting changes.
 * <p>
 * The right-click popup menu ({@link #maybeShowPopup}) offers Add/Edit
 * Comment, Find/Find Next, and - since {@link #findLabelInLine} turned out
 * to need nothing more than the clicked line's own text (see that
 * method's javadoc for why this is not the "mouse-position label parsing"
 * it first looked like) - Find Definition/References, Rename, and Address
 * Range too, reusing {@link DisassemblyResult#findAndSelectLines}/{@link
 * DisassemblyResult#findDefinitionLineNumber}, {@code
 * Equate.extractAddress}/{@code isAutomaticLabel}, {@link
 * EquateDialog}/{@link EquateRangeDialog} - see {@code Dis6502}'s wiring
 * of {@link #findDefMenuItem}/{@link #findRef1MenuItem}/{@link
 * #findRef2MenuItem}/{@link #renameDefMenuItem}/{@link
 * #renameRefMenuItem}/{@link #addrRangeDefMenuItem}/{@link
 * #addrRangeRefMenuItem}. Navigate Back to Previous Position ({@link
 * #backInHistoryMenuItem}, Backspace) returns to where Navigate to
 * Definition was started from - see {@link
 * #navigateToDefinitionLine}/{@link #navigateBack} and {@link
 * LineNumberHistory}. The "Change type of immediate byte to" submenu works
 * on the right-clicked instruction, not on a byte selection like the
 * memory inspector popup's Change Type submenu; what it shows (enabled,
 * char constant possible, current type's check mark) needs the workspace
 * this panel does not have, so {@code Dis6502} supplies it per popup
 * through {@link #setImmediateTypeProvider} and performs the change
 * through {@link #setImmediateTypeListener}.
 * <p>
 * Every popup item is public and wired by {@code Dis6502} (unlike {@link
 * MemoryInspectorPanel}'s popup items, they need a parent {@link
 * java.awt.Frame}/{@code Workspace} this panel does not have), reading
 * {@link #getRightClickedLine}/{@link #getRightClickedLabelDefinition}/
 * {@link #getRightClickedLabelReference} - all three frozen at the moment
 * the popup was shown, rather than re-derived when a menu item is later
 * clicked. Every static item is built via {@code
 * com.wudsn.tools.base.gui.ElementFactory} from an {@code Action} in
 * {@code com.wudsn.tools.dis6502.Actions}, the same as {@link MainMenu};
 * the seven items whose label names an actual label ({@link #findDefMenuItem}
 * and friends) stay plain {@code JMenuItem}s built with no label at all,
 * since their text is {@code "{0}"}-templated and only resolved at
 * popup-show time - see {@link #setDynamicLabel} and {@code Actions}' own
 * javadoc. {@link #editCommentMenuItem}'s Add/Edit Comment uses the
 * clicked line's own {@code offset}/{@code size} directly, rather than
 * snapping to the enclosing instruction - correct for the common case of
 * right-clicking an actual instruction line, though not necessarily
 * identical for a label/equate-only line with no byte size of its own; a
 * model-layer method for that snapping is available, just not wired up
 * for this yet.
 *
 * @author Peter Dell
 */
public final class DisassemblyPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	private final DisassemblyGridPanel grid = new DisassemblyGridPanel();
	public final JTextField findField = new JTextField(24);
	public final JButton findButton = ElementFactory.createButton(Actions.DisassemblyPanel_Find, false);
	public final JButton findNextButton = ElementFactory.createButton(Actions.DisassemblyPanel_FindNext, false);
	public final JMenuItem editCommentMenuItem = ElementFactory.createMenuItem(Actions.DisassemblyPopupMenu_EditComment, "editCommentMenuItem");
	// findDefMenuItem/findRef1MenuItem/findRef2MenuItem/renameDefMenuItem/renameRefMenuItem/
	// addrRangeDefMenuItem/addrRangeRefMenuItem stay plain JMenuItems, not built via
	// ElementFactory.createMenuItem: their label is "{0}"-templated and only known once
	// the popup is about to show - see setDynamicLabel/maybeShowPopup.
	public final JMenuItem findDefMenuItem = new JMenuItem();
	public final JMenuItem backInHistoryMenuItem = ElementFactory.createMenuItem(Actions.DisassemblyPopupMenu_BackInHistory,
			"backInHistoryMenuItem");
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

	private final LineNumberHistory history = new LineNumberHistory();

	/** The submenu's types, in menu order (a separator goes before the last one). */
	private static final MemoryType[] IMMEDIATE_TYPES = { MemoryType.CODE, MemoryType.LOBYTE, MemoryType.HIBYTE,
			MemoryType.STRING, MemoryType.UNKNOWN };
	private static final Action[] IMMEDIATE_TYPE_ACTIONS = { Actions.DisassemblyPopupMenu_ImmediateType_Code,
			Actions.DisassemblyPopupMenu_ImmediateType_LowByte, Actions.DisassemblyPopupMenu_ImmediateType_HighByte,
			Actions.DisassemblyPopupMenu_ImmediateType_String, Actions.DisassemblyPopupMenu_ImmediateType_Unknown };
	private final JMenu immediateTypeMenu = ElementFactory.createMenu(Actions.DisassemblyPopupMenu_ImmediateType);
	private final JCheckBoxMenuItem[] immediateTypeMenuItems = new JCheckBoxMenuItem[IMMEDIATE_TYPES.length];
	private ImmediateTypeProvider immediateTypeProvider;
	private ImmediateTypeListener immediateTypeListener;

	public DisassemblyPanel() {
		super(new BorderLayout());
		header.setText(Texts.DisassemblyPanel_Title);

		findField.setEnabled(false);
		findButton.setEnabled(false);
		findNextButton.setEnabled(false);
		findField.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) {
				updateFindButtonEnabled();
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				updateFindButtonEnabled();
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				updateFindButtonEnabled();
			}

			private void updateFindButtonEnabled() {
				findButton.setEnabled(!findField.getText().isEmpty());
			}
		});

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
		backInHistoryMenuItem.addActionListener(e -> navigateBack());

		for (int i = 0; i < IMMEDIATE_TYPES.length; i++) {
			final MemoryType type = IMMEDIATE_TYPES[i];
			JCheckBoxMenuItem item = ElementFactory.createCheckBoxMenuItem(IMMEDIATE_TYPE_ACTIONS[i]);
			item.addActionListener(e -> {
				if (immediateTypeListener != null && rightClickedLine != null) {
					immediateTypeListener.onImmediateTypeSelected(rightClickedLine, type);
				}
			});
			immediateTypeMenuItems[i] = item;
			if (type == MemoryType.UNKNOWN) {
				immediateTypeMenu.addSeparator();
			}
			immediateTypeMenu.add(item);
		}
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
		// Backspace is scoped to the grid's own focus for the same reason: window-wide, it
		// would be stolen from the find field and every other text field in the window.
		grid.getInputMap(JComponent.WHEN_FOCUSED).put(Actions.DisassemblyPopupMenu_BackInHistory.getAccelerator(), "navigateBack");
		grid.getActionMap().put("navigateBack", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				navigateBack();
			}
		});

		// Ctrl+Shift+F/Shift+F3, from Actions.DisassemblyPopupMenu_Find/_FindNext
		// (see POPUP_MENU_ACCELERATORS_PLAN.md for how they were verified), bound
		// WHEN_IN_FOCUSED_WINDOW like MemoryInspectorPanel's own popup-menu
		// accelerators - unlike Return above, these carry no risk of stealing a
		// common key from an unrelated focused component, so no such narrowing is
		// needed. isPopupMenuVisible() below defers to the item's own accelerator
		// whenever the popup happens to already be showing - the same empirical
		// finding documented in MemoryInspectorPanel.bindPopupMenuAccelerators
		// applies here too (a standalone JPopupMenu's item accelerators, which
		// {@code ElementFactory.createMenuItem} applies automatically from these
		// same Actions, only fire while that popup instance is open).
		// Ctrl+Shift+F itself doesn't run findButton.doClick() (unlike Shift+F3
		// below, or popupFindMenuItem's own click handler): the C++ original's
		// equivalent keystroke opened a modal find dialog for the user to type
		// into, and findField is this panel's replacement for that dialog (see
		// this class's own javadoc) - so the keyboard shortcut's job now is
		// putting the user into that field, not re-running whatever was last
		// typed into it.
		bindAccelerator(Actions.DisassemblyPopupMenu_Find, () -> {
			if (findField.isEnabled()) {
				findField.requestFocusInWindow();
				findField.selectAll();
			}
		});
		bindAccelerator(Actions.DisassemblyPopupMenu_FindNext, findNextButton::doClick);
	}

	private boolean isPopupMenuVisible() {
		return popupMenu.isVisible();
	}

	// Package-private, for RenderingTest: the popup as built by the last right-click, and the grid it was clicked on.
	JPopupMenu getPopupMenu() {
		return popupMenu;
	}

	DisassemblyGridPanel getGrid() {
		return grid;
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
	 * Navigates to the definition of the currently selected line's referenced
	 * label, unlike a plain click/drag (see {@link #selectLineAt}), which
	 * only reports the selection and refreshes the XRef list, never
	 * navigating away from the clicked line.
	 */
	private void navigateToDefinitionOfSelectedLine() {
		if (navigateToDefinitionListener != null && !selectedLabelReference.isEmpty()) {
			navigateToDefinitionListener.onNavigateToDefinition(selectedLabelReference);
		}
	}

	/**
	 * Goes to the line a label is defined at, remembering where that was
	 * started from for {@link #navigateBack}. The target line ends up
	 * selected exactly as if it had been clicked, so the memory inspector
	 * and the XRef list follow. "Where it was started from" is {@code
	 * originLine} if given (the right-clicked line, for the popup item),
	 * else the selected line (Return/double-click).
	 */
	public boolean navigateToDefinitionLine(int lineNumber, DisassemblyLine originLine) {
		Integer index = lineNumberToIndex.get(lineNumber);
		if (index == null) {
			return false;
		}
		if (originLine != null) {
			history.push(originLine.getLineNumber());
		} else if (lastSelectedLineIndex >= 0 && lastSelectedLineIndex < disassemblyLines.size()) {
			history.push(disassemblyLines.get(lastSelectedLineIndex).getLineNumber());
		}
		selectLineIndex(index);
		return true;
	}

	/** Navigate Back to Previous Position. Does nothing if there is no previous position. */
	public void navigateBack() {
		Integer index = lineNumberToIndex.get(history.pop());
		if (index != null) {
			selectLineIndex(index);
		}
	}

	/** Whether {@link #navigateBack} has anywhere to go. */
	public boolean canNavigateBack() {
		return !history.isEmpty();
	}

	/**
	 * Reached through a single click/drag rather than continuous
	 * mouse-capture tracking, since that's the idiomatic Swing shape for
	 * this - {@link #lastSelectedLineIndex} only re-notifies {@link
	 * #lineSelectionListener} when the line under the cursor actually
	 * changes: highlights the clicked line and reports it - along with the
	 * label its operand references, or the one it defines if it has no
	 * reference - to {@link #lineSelectionListener}.
	 */
	private void selectLineAt(MouseEvent e) {
		int index = grid.lineIndexAtY(e.getY());
		if (index < 0 || index >= disassemblyLines.size() || index == lastSelectedLineIndex) {
			return;
		}
		selectLineIndex(index);
	}

	/** Makes the line at {@code index} the selected one - scrolled to, highlighted, reported to {@link #lineSelectionListener} - however that came about: a click, or a navigation. */
	private void selectLineIndex(int index) {
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

	/** Rebuilds the popup's item set from scratch every time it is shown. */
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

		ImmediateType immediateType = rightClickedLine != null && immediateTypeProvider != null
				&& rightClickedLine.segmentIndex != SegmentList.NO_SEGMENT_INDEX
						? immediateTypeProvider.getImmediateType(rightClickedLine)
						: null;
		immediateTypeMenu.setEnabled(immediateType != null);
		for (int i = 0; i < IMMEDIATE_TYPES.length; i++) {
			immediateTypeMenuItems[i].setSelected(immediateType != null && immediateType.memoryType == IMMEDIATE_TYPES[i]);
			immediateTypeMenuItems[i].setEnabled(
					immediateType != null && (IMMEDIATE_TYPES[i] != MemoryType.STRING || immediateType.charAllowed));
		}
		popupMenu.add(immediateTypeMenu);

		if (hasReference) {
			setDynamicLabel(findDefMenuItem, Actions.DisassemblyPopupMenu_FindDef, rightClickedLabelReference);
			popupMenu.add(findDefMenuItem);
		}
		backInHistoryMenuItem.setEnabled(canNavigateBack());
		popupMenu.add(backInHistoryMenuItem);
		popupMenu.addSeparator();

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
	 * references. Despite this method's name possibly suggesting it needs to
	 * know where in the line the mouse was, it only ever parses the whole
	 * line's text structurally (leading identifier = definition; skip the
	 * mnemonic; an operand starting with a letter/@/_ after an optional
	 * #/(/&gt;/&lt; prefix = reference), with no mouse position involved at
	 * all.
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

	/** Call whenever the workspace's computer system or double-height setting changes. */
	public void setComputerFont(ComputerFont computerFont) {
		grid.setComputerFont(computerFont);
		header.setComputerFont(computerFont);
	}

	/** See {@link DisassemblyGridPanel#setLineNumbersActive} - call alongside every {@link #refresh}. */
	public void setLineNumbersActive(boolean lineNumbersActive) {
		grid.setLineNumbersActive(lineNumbersActive);
	}

	public void refresh(DisassemblyResult disassemblyResult) {
		// Remembered positions are line numbers. A new disassembly of the same length (a
		// comment or label was edited) leaves them valid; anything else would make "back"
		// land on some unrelated line, so it is better to have nowhere to go back to.
		int newLineCount = disassemblyResult == null ? 0 : disassemblyResult.getLineCount();
		if (newLineCount != disassemblyLines.size()) {
			history.clear();
		}
		lineNumberToIndex.clear();
		rightClickedLine = null;
		lastSelectedLineIndex = -1;

		// The find field/shortcut only make sense once there is something to search -
		// disabling it (and clearing any stale query) here, rather than relying on
		// every caller to remember to do so, keeps it correct regardless of which of
		// this method's own callers triggered the refresh.
		boolean hasDisassembly = disassemblyResult != null && !disassemblyResult.isEmpty();
		findField.setEnabled(hasDisassembly);
		if (!hasDisassembly) {
			findField.setText("");
		}
		findNextButton.setEnabled(false);

		if (!hasDisassembly) {
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
	 * Scrolls to and highlights the given disassembly line number. Returns
	 * {@code false} if {@code lineNumber} is not part of the currently
	 * displayed disassembly (e.g. stale after a new one replaced it).
	 */
	public boolean navigateToLine(int lineNumber) {
		Integer index = lineNumberToIndex.get(lineNumber);
		if (index == null) {
			return false;
		}
		grid.highlightLine(index);
		return true;
	}

	/** Supplies what the "Change type of immediate byte to" submenu shows for a line - see the class javadoc. */
	public void setImmediateTypeProvider(ImmediateTypeProvider immediateTypeProvider) {
		this.immediateTypeProvider = immediateTypeProvider;
	}

	/** Reports a pick from the "Change type of immediate byte to" submenu. */
	public void setImmediateTypeListener(ImmediateTypeListener immediateTypeListener) {
		this.immediateTypeListener = immediateTypeListener;
	}

	/** An immediate-mode instruction's operand, as far as the submenu cares. */
	public static final class ImmediateType {
		final MemoryType memoryType;
		final boolean charAllowed;

		public ImmediateType(MemoryType memoryType, boolean charAllowed) {
			this.memoryType = memoryType;
			this.charAllowed = charAllowed;
		}
	}

	public interface ImmediateTypeProvider {
		/** Returns {@code null} if the line is not an immediate-mode instruction. */
		ImmediateType getImmediateType(DisassemblyLine line);
	}

	public interface ImmediateTypeListener {
		void onImmediateTypeSelected(DisassemblyLine line, MemoryType memoryType);
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
