# Plan 17: The About dialogs on WUDSN Base's `ModalDialog`

Status: In progress - steps 1 to 3 done (2026-10-05), see "Progress".

## Goal

After plan 16, every OK/Cancel dialog of DIS6502 and RASTER Music Tracker
(RMT) extends WUDSN Base's `ModalDialog`. The About dialogs of DIS6502,
RMT and The!Cart Studio still build their own frame, though each is a
modal dialog with an OK button only. An OK-only variant of `ModalDialog`
gives them the same button bar, keys and disposal as every other dialog.

## The About dialogs today

| Project | Class | Frame | Shown by |
|---|---|---|---|
| DIS6502 | `ui.AboutDialog extends JDialog` | `setContentPane(contentPanel)`: a white panel with header (icon, title, Alfred), text lines (links clickable) and its own OK button panel, also white. OK with its mnemonic; Escape via `ElementUtilities.closeOnEscape`; `pack()`, `setResizable(false)` | `Dis6502.performAbout`: a new dialog per use, `setVisible(true)` |
| RMT | `ui.AboutDialog extends JDialog` | `GridBagLayout` content at `CENTER` (icon, version, author, repository link, folders, two read-only ASAP text boxes); own OK button (no mnemonic) in a right-aligned `FlowLayout` panel at `SOUTH`; Escape via `registerKeyboardAction`; `pack()`, `setResizable(false)` | `RmtMainWindow`: a new dialog per use, `setVisible(true)` |
| The!Cart Studio | `ui.AboutDialog extends SimpleDialog` | a `JLabel` with HTML content (version, cartridge database) at `CENTER`, clickable as a whole; `SimpleDialog`'s OK button bar (no mnemonic); `setSize(230, 200)` | `TheCartStudio.performAboutDialog`: created once, kept in a field and shown again on every use |

`SimpleDialog` (WUDSN Base) is an OK-only frame by composition, not a
`JDialog` subclass; its parent must be a non-null `JFrame`, and it keeps
its `JDialog` for the next `show()`. Its other user, The!Cart Studio's
`ContentTypesDialog`, is non-modal (`super(parent, title, false)`), so it
cannot move to `ModalDialog` and keeps `SimpleDialog`.

Not About dialogs, and not part of this plan: DIS6502's
`DisassemblyProgressDialog` (Cancel only, closed by the disassembly) and
`AssembleDialog` (Assemble/Close, repeatable) stay on `JDialog`.

## Design

### WUDSN Base: `ModalDialog` without Cancel

```java
public ModalDialog(Window owner, String title)                        // as today: OK and Cancel
public ModalDialog(Window owner, String title, boolean cancelButton)  // false: OK only
```

- The two-argument constructor calls the new one with `true` - RMT's and
  The!Cart Studio's existing subclasses and DIS6502's 17 dialogs do not
  change.
- Without Cancel, the button bar holds OK only. Escape and the close box
  still close the dialog with `okPressed` false - for an information
  dialog the caller ignores the result. The Escape binding uses the same
  internal cancel action as today, only without a button for it.
- `addButtonBarButton` and `close()` work as before; OK stays the default
  button.
- The class javadoc and the `ModalDialog` rule in WUDSN Base's
  `plans/RULES_WUDSN_BASE.md` mention the OK-only variant and that
  information dialogs (About) use it.

### DIS6502: `AboutDialog`

The pane nesting is adapted to the standard (the user's go-ahead,
2026-10-05): instead of replacing the content pane, the dialog adds its
white `contentPanel` - header and text lines, without the OK panel - at
`BorderLayout.CENTER`, like every other `ModalDialog`. `ModalDialog` owns
the bottom with its button bar.

- The white background: the button bar would be gray below a white
  content panel. The dialog sets the content pane's background to white
  as well; the button bar (a `Box`) is not opaque and shows it, and the
  empty `fieldsPane`/data panel at `NORTH` has no height. This is checked
  on a painted image (`rootPane.printAll`); if a gray strip remains, the
  dialog sets those panels non-opaque too, in the dialog, not in
  `ModalDialog`.
- `super(owner, Texts.AboutDialog_WindowTitle, false)`; `setResizable(false)`
  stays.
- A `show()` method calls `showModal(getOKButton())`;
  `Dis6502.performAbout` calls `show()` instead of `setVisible(true)`.
- `ElementUtilities.closeOnEscape` then has one user left,
  `DisassemblyProgressDialog`; its javadoc is updated.

### RMT: `AboutDialog`

- `super(owner, Texts.AboutDialog_Title, false)`; the `GridBagLayout`
  content stays at `CENTER`; the own OK panel and the Escape binding go.
- OK gets its mnemonic (it had none - as for all RMT dialogs since plan
  16).
- A `showDialog()` method (RMT's naming) calls `showModal(getOKButton())`;
  `RmtMainWindow` calls it.

### The!Cart Studio: `AboutDialog` - later (decision 2)

For now, only a `TODO` in The!Cart Studio's `AboutDialog.java` records the
change below; the dialog stays on `SimpleDialog`. `TheCartStudio.java`,
which the change also touches, has uncommitted local changes.


- Extends `ModalDialog` with `cancelButton` false instead of
  `SimpleDialog`; `initComponents`/`dataToUI` become the constructor and a
  `showDialog()` that fills the label and calls `showModal(getOKButton())`.
- `setSize(230, 200)` goes - `showModal` packs; the label gets a preferred
  size only if the packed dialog turns out too small.
- `TheCartStudio.performAboutDialog` creates a new dialog per use instead
  of keeping one in the field `aboutDialog` (a `ModalDialog` disposes
  itself after `showModal`); the field goes.
- `SimpleDialog` keeps one user, `ContentTypesDialog`.

## Tests

- **WUDSN Base:** `ModalDialogTest` gets a case for the OK-only dialog: no
  Cancel button in the bar, OK closes with `okPressed` true, Escape and
  the close box close with false.
- **DIS6502:** `DialogTextsTest` already constructs `AboutDialog` with a
  `null` owner and checks its texts; it must pass unchanged. The layout -
  white background up to the button bar, OK at the right - is checked on a
  painted image.
- **RMT:** its tests must pass; the About dialog is checked on a painted
  image.
- **The!Cart Studio:** compiles with the `TODO` (no code change).

## Steps

1. **WUDSN Base:** the constructor, `ModalDialogTest`, the javadoc and
   `RULES_WUDSN_BASE.md`; `javac --release 8`; install; commit.
2. **DIS6502:** `AboutDialog`, `Dis6502.performAbout`, `ElementUtilities`'
   javadoc; both test modes; painted image; this plan's progress.
3. **RMT:** `AboutDialog`, `RmtMainWindow`; tests; painted image.
4. **The!Cart Studio:** the `TODO` in `AboutDialog.java` only; commit that
   file alone.

Each repository is committed separately; no `CHANGES.md` entry in
DIS6502 beyond what users see (the About dialog's OK at the right, in the
standard button bar).

## Progress

**Step 1 (2026-10-05), WUDSN Base `1332f03`:** `ModalDialog(owner, title,
false)` has OK only; the Cancel button is created only when wanted, the
Escape binding uses the internal cancel action in both variants.
`ModalDialogTest.testOKOnly` checks one button in the bar, OK closing with
`okPressed` true, Escape and the close box with false; it fails when
Cancel is always added. `RULES_WUDSN_BASE.md` describes the variant; its
DIS6502 example of dialogs that stay on `JDialog` is now `AssembleDialog`
and `DisassemblyProgressDialog` (it claimed both use `closeOnEscape`; only
the latter does). RMT's 666 tests pass unchanged.

**Step 2 (2026-10-05):** DIS6502's `AboutDialog` extends the OK-only
`ModalDialog`. Its white panel sits at the content pane's center; the
content pane is white, so the button bar below shows white - checked on a
painted image: no gray strip, OK at the right, the default button; Escape
closes and disposes it. The method is `showDialog()`, not `show()`: a
public `show()` overrides AWT's `Dialog.show()`, which `setVisible(true)`
calls - `showModal` would recurse. `ElementUtilities.closeOnEscape` keeps
one user, `DisassemblyProgressDialog`. Both test modes pass.

**Step 3 (2026-10-05):** RMT's `AboutDialog` extends the OK-only
`ModalDialog`; its `GridBagLayout` content stays at the center, its own OK
panel and Escape binding are gone, OK has its mnemonic. `showDialog()`
(RMT's naming, and not `show()` - see step 2) is called by
`RmtMainWindow.showAbout`. RMT's 666 tests pass; a painted image shows the
content unchanged and OK at the right of the standard button bar, OK the
default button, Escape closing and disposing the dialog.

## Decisions (2026-10-05)

1. **A plain `boolean cancelButton`** as the third constructor argument,
   documented in the javadoc - the call sites are few.
2. **The!Cart Studio's About dialog is left out for now:** only a `TODO`
   in its `AboutDialog.java` names the change (extend `ModalDialog` with
   `cancelButton` false, create the dialog per use in
   `TheCartStudio.performAboutDialog`), since `TheCartStudio.java` has
   uncommitted local changes.
