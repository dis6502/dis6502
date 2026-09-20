# Plan: wire up the popup-menu accelerators left out of the Actions/ElementFactory migration

**Status: implemented.** Step 0's empirical smoke test settled the open
question: a standalone `JPopupMenu`'s item accelerators only fire while that
specific popup instance is actually open on screen (confirmed for both
`MemoryInspectorPanel`'s never-rebuilt popup and `DisassemblyPanel`'s
rebuilt-per-click one) - useless for C++'s window-wide accelerator-table
semantics. All items in this plan were wired the "Mechanism B" way:
`Actions.java`'s accelerator fields are left unset, and each keystroke is
bound via `InputMap`/`ActionMap` on `grid` (`WHEN_IN_FOCUSED_WINDOW`),
calling `doClick()` on the already-correctly-wired, already-visible item -
`MemoryInspectorPanel.bindPopupMenuAccelerators`/`DisassemblyPanel`'s
constructor. Verified via a throwaway
`PopupMenuAcceleratorWiringSmokeTest`/`AcceleratorLivenessSmokeTest` (never
committed) covering: firing without the popup ever having opened, no
double-fire on repeated presses, and Ctrl+C/Ctrl+A not being hijacked while a
`JTextField` elsewhere in the same window has focus.

## Context

`ACTIONS_ELEMENT_FACTORY_MIGRATION.md` moved `SegmentListPanel`/`DisassemblyPanel`/
`MemoryInspectorPanel`'s right-click popup items onto `Actions`/`ElementFactory`,
but deliberately left every popup item's accelerator unset except the two that
already worked (`editMenuItem`/`quitEditModeMenuItem`, F2/Esc) - that decision
was made explicitly with the user to keep that change scoped to construction
only. This plan covers the follow-up: wiring up the remaining accelerators the
C++ source (`dis6502.rc`'s `ACCELERATORS` table) defines for
`MemoryInspectorPopupMenu`/`DisassemblyPopupMenu` items, none of which have ever
been live in this port.

`SegmentListPopupMenu`/`MAIN_MENU` are not part of this: the segment list popup
has no accelerators in the C++ source at all, and the main menu's accelerators
were already ported in the first migration step.

## Verified accelerator table

Re-derived from `dis6502.rc`'s `ACCELERATORS` block (not the popup menus' own
`\tHint` text - see "A found C++ bug" below for why that distinction matters)
and cross-checked against `MainMemoryInspector::PerformCommands`/
`MainDisassembly::Proc`'s `case ID_X: case IDM_X:` pairs, which confirm each
`ID_*` accelerator-table entry and its `IDM_*` popup-menu counterpart really
are the same logical command (e.g. `ID_DUMP_SELECT` pairs with
`IDM_DUMP_SELECT_NEXT_UNKNOWN_BLOCK`, not any other `Select*` item - the naming
alone is not reliable enough to assume this).

### MemoryInspectorPanel

| Field | Keystroke |
|---|---|
| `startCodeTraceMenuItem` | Ctrl+T |
| `assembleMenuItem` | F8 |
| `copySelectionMenuItem` | Ctrl+C |
| `findMenuItem` | Ctrl+F |
| `findNextMenuItem` | F3 |
| `selectNextUnknownBlockMenuItem` | F5 |
| `selectSpritesMenuItem` | F4 |
| `selectAllMenuItem` | Ctrl+A |
| Change Type: Code | Shift+C |
| Change Type: Low Byte | Shift+O |
| Change Type: High Byte | Shift+I |
| Change Type: Byte | Shift+B |
| Change Type: Word | Shift+W |
| Change Type: Label | Shift+L |
| Change Type: SpartaDos X Label (Symbol) | Shift+X |
| Change Type: SpartaDos X Address Fix-Up | Shift+F |
| Change Type: String | Shift+S |
| Change Type: Screen Byte (Sbyte) | Shift+Y |
| Change Type: Display List | **Shift+D** |
| Change Type: Data Store | **Shift+A** |
| Change Type: Unknown | Shift+U |

`editMenuItem` (F2) / `quitEditModeMenuItem` (Esc) already wired - unchanged.
No accelerator in C++ at all, so none is added: `setUnknownBlockToByteMenuItem`,
`editCommentMenuItem`, `splitAtSelectionMenuItem`, `saveSelectionNoHeaderMenuItem`,
`saveSelectionHeaderMenuItem`. Cut/Paste/Delete are unported entirely - out of
scope regardless of their C++ accelerators.

### DisassemblyPanel

| Field | Keystroke |
|---|---|
| `popupFindMenuItem` (→ `findButton`) | Ctrl+Shift+F |
| `popupFindNextMenuItem` (→ `findNextButton`) | Shift+F3 |

`findDefMenuItem`'s C++ hint (`RETURN or Double Click`) is already fully
covered by the existing Return/double-click handling - do not add a menu-item
accelerator for it (see "Wiring mechanism" below for why Return specifically
was deliberately kept off `WHEN_IN_FOCUSED_WINDOW` when it was first added).
No accelerator in C++ at all for `editCommentMenuItem`, `findRef1MenuItem`,
`findRef2MenuItem`, `renameDefMenuItem`, `renameRefMenuItem`,
`addrRangeDefMenuItem`, `addrRangeRefMenuItem`.

### A found C++ bug: Display List/Data Store accelerators don't match their menu hint text

`MEMORY_INSPECTOR_POPUP_MENU`'s own item text says `"&Display List\tShift+A"`
and gives `"D&ata Store"` no hint at all - but the `ACCELERATORS` table actually
binds `Shift+D` to `IDM_DUMP_SET_TYPE_DLIST` (Display List) and `Shift+A` to
`IDM_DUMP_SET_TYPE_STORE` (Data Store). The menu's displayed hint text for
Display List is stale/wrong; the accelerator table is what Windows actually
does when the user presses the key, so it is what this plan follows (Shift+D
for Display List, Shift+A for Data Store) - add a `// TODO` comment at the
Display List `MENUITEM` line in `dis6502.rc` noting the mismatch, matching
this project's established practice for documenting a confirmed C++ bug
in-place (no functional C++ change, since fixing a resource string doesn't
need it, and the Java port already gets the authoritative version).

## The one open technical question to resolve first

`ElementFactory.createMenuItem` already sets `.setAccelerator(action.getAccelerator())`
automatically, so on paper, simply pre-populating each `Action` field with
`new Action(keyCode, modifiers)` (as was already done for `editMenuItem`/
`quitEditModeMenuItem`) is all that is needed. The open question is whether a
`JMenuItem`'s accelerator is actually *live* (delivered as a global keystroke)
when that item belongs only to a standalone `JPopupMenu` that is not part of a
`JMenuBar` - unlike `MainMenu`'s items, which sit in a `JMenuBar` permanently
attached to the main `JFrame`. This matters even more for `DisassemblyPanel`,
whose popup is rebuilt from scratch on every right-click
(`popupMenu.removeAll()` then conditional re-`add`) - if accelerator
registration is tied to an item's container lifecycle, repeatedly removing and
re-adding the same items could leave the accelerator working unreliably (only
while the popup happens to be open, or not at all).

**Step 0, before writing any panel code:** a small throwaway smoke test (per
this project's convention, in `C:\TEMP\claude\jdis6502-smoketest\`) that:
1. Builds a real `MemoryInspectorPanel` (whose popup is built once, in the
   constructor, and never rebuilt) inside a real, visible `JFrame` - matching
   `PopupMenuActionsSmokeTest`'s existing pattern.
2. Gives `Actions.MemoryInspectorPopupMenu_StartCodeTrace` a real accelerator
   (Ctrl+T) and rebuilds the panel.
3. **Without ever right-clicking to open the popup**, dispatches a synthetic
   Ctrl+T `KeyEvent` at the frame/root level and checks whether
   `startCodeTraceMenuItem`'s `ActionListener` fired.
4. Repeats the same check after simulating `DisassemblyPanel`'s
   `popupMenu.removeAll()` + re-`add` cycle a few times in a row, for one of
   its statically-labeled items, to specifically probe the rebuild-churn risk.

The result decides the wiring mechanism:

- **If accelerators fire reliably in both cases:** rely on the `Action`'s
  accelerator alone (already automatic via `ElementFactory.createMenuItem`) -
  the only code changes needed are populating the `Action` fields in
  `Actions.java` per the table above, plus extending
  `ElementFactory.createCheckBoxMenuItem` to also set the accelerator from the
  `Action` (it currently only calls `setButtonTextAndMnemonic`, matching
  `createMenuItem`'s own shape) for the 12 Change Type items.
- **If they don't (most likely for `DisassemblyPanel`'s rebuilt popup, and
  possibly for `MemoryInspectorPanel`'s static one too):** do **not** populate
  the `Action`'s accelerator field for the affected items at all (setting it
  when the underlying mechanism turns out to be inert is at best a no-op, and
  risks a confusing double-fire if it turns out to be *partially* live e.g.
  only while the popup is open). Instead, wire each keystroke explicitly via
  `InputMap`/`ActionMap` on `grid`, exactly mirroring how F2/Esc (`MemoryInspectorPanel`)
  and Return (`DisassemblyPanel`) already do it - see "Wiring mechanism" below
  for what each binding should call. This is the safer default to assume
  going in, given the rebuild-churn concern is real and specific to this
  codebase, not hypothetical.

## Wiring mechanism (if Step 0 says to use `InputMap`/`ActionMap`)

Each new binding's `Action.actionPerformed` should call the exact same target
the corresponding menu item's own `ActionListener` already calls - reusing
`JMenuItem.doClick()` on the *already-correctly-wired, already-visible* menu
item field is appropriate here (unlike the hidden-toolbar-button anti-pattern
`jdis6502-avoid-hidden-doclick-indirection` warns about: this is a second,
deliberate *input path* to the same real, user-visible command, not a
mechanism-only proxy component). Concretely:

- `MemoryInspectorPanel`: bind each keystroke on
  `grid.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)` (matching F2/Esc's own
  scope - the C++ accelerator table is genuinely global within the main
  window, not scoped to whichever child has focus) to an action calling
  `startCodeTraceMenuItem.doClick()` / `assembleMenuItem.doClick()` / etc.;
  for the 13 Change Type items, `typeMenuItems[i].doClick()` indexed the same
  way `buildPopupMenu`'s existing loop already does.
- `DisassemblyPanel`: bind Ctrl+Shift+F/Shift+F3 the same way, calling
  `findButton.doClick()`/`findNextButton.doClick()` directly (what
  `popupFindMenuItem`/`popupFindNextMenuItem` themselves already do) rather
  than the popup items, sidestepping the rebuilt-popup question entirely.
  Scope choice: `WHEN_IN_FOCUSED_WINDOW`, matching C++'s own global
  accelerator table - unlike Return (`ID_DIS_FIND_DEF`, also technically
  global in C++), which was deliberately narrowed to `WHEN_FOCUSED` on `grid`
  when it was first added, specifically because Enter is common enough
  elsewhere in the window that stealing it globally was judged too risky;
  Ctrl+Shift+F/Shift+F3 carry no similar risk, so no such narrowing is needed
  here - flag this consistency call out for confirmation rather than assuming
  it silently, since it is a real, explicit deviation from strict "match
  Return's scope" symmetry.

## Verification

- `mvn -o compile`/`test-compile`; the existing `TestRunner` suite is
  unaffected (no model-layer change).
- Extend `PopupMenuActionsSmokeTest` (or a new smoke test) to dispatch each
  new keystroke via the same real, constructed panels and assert the
  corresponding command actually ran (e.g. for `selectAllMenuItem`'s Ctrl+A,
  assert the selection became the whole segment; for a Change Type item's
  Shift+key, assert `typeSelectionListener` fired with the right
  `MemoryType`) - covering both a "popup never opened yet" case and, for
  `DisassemblyPanel`, a "right-clicked and closed the popup at least once
  already" case, to directly exercise the rebuild-churn risk Step 0
  investigated.
- Explicitly test for double-firing: for any item where the `Action`'s
  accelerator was left populated *and* an `InputMap` binding also exists
  (should not happen after this plan, but worth a regression check), assert
  the listener fires exactly once per keystroke, not twice.
- Manual check: run the app, and for a representative sample (Ctrl+T, F8,
  Shift+D, Ctrl+Shift+F), confirm the shortcut works while the memory
  inspector/disassembly panel is visible but not focused, matching C++'s
  window-global behavior - and confirm Ctrl+C/Ctrl+A still let normal text
  copy/select-all work correctly in `findField`/other text fields elsewhere
  in the window (a specific, concrete collision risk for exactly those two
  generic shortcuts).
