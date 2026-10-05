# Plan 16: DIS6502's dialogs on WUDSN Base's `ModalDialog`

Status: Done (2026-10-05), see "Progress".

## Goal

DIS6502's OK/Cancel dialogs each build the same frame by hand. WUDSN
Base's `ModalDialog` provides that frame and is already the standard in
RASTER Music Tracker. DIS6502's dialogs move onto it, after the few
changes `ModalDialog` needs to serve all three projects.

## The dialogs today

### DIS6502: 20 dialogs, all `extends JDialog`

(`FindStringDialog` is not a dialog, despite its name: it is the
ASCII/hex conversion helper of `MemoryInspectorFindStringDialog`.)

Each OK/Cancel dialog repeats the same frame:

```java
super(owner, true);                       // owner is a Frame, null in DialogTextsTest
setDefaultCloseOperation(DISPOSE_ON_CLOSE);
setTitle(Texts.XxxDialog_Title);
... JPanel formPanel = new JPanel(new GridBagLayout()) ...
JButton okButton = ElementFactory.createButton(Actions.ButtonBar_OK, true);
okButton.addActionListener(e -> performOK());       // validates with JOptionPane, sets confirmed
JButton cancelButton = ElementFactory.createButton(Actions.ButtonBar_Cancel, true);
cancelButton.addActionListener(e -> { confirmed = false; setVisible(false); });
... buttonPanel, BorderLayout.CENTER/SOUTH ...
getRootPane().setDefaultButton(okButton);
ElementUtilities.closeOnEscape(this, cancelButton::doClick);

public boolean show(...) { fill fields; pack(); setLocationRelativeTo(getOwner()); setVisible(true); return confirmed; }
```

| Group | Dialogs |
|---|---|
| A. Form, OK/Cancel only (10) | `CommentDialog`, `DiskImageExecutableFileDialog`, `EquateContextsDialog`, `EquateRangeDialog`, `LowHighByteDialog`, `MemoryInspectorFindStringDialog`, `RawFileDialog`, `SegmentPropertiesDialog`, `SelectGraphicsDialog`, `WorkspaceDialog` |
| B. Buttons inside the form (3) | `DefaultFoldersDialog` (Browse per folder), `DiskImageSectorsDialog` (Add/Remove Sector), `EquateDialog` (Add/Modify, Delete) |
| C. Extra buttons in the button bar (3) | `OptionsDialog` (Restore Defaults, left), `ProfileDialog` (Load/Save Profile), `ROMTypeDialog` (Open as Raw File - a third result that closes the dialog) |
| D. OK performs the action (1) | `SegmentWriteBootDiskDialog`: `show` returns nothing; OK writes the boot disk |
| E. Not OK/Cancel dialogs (3) | `AboutDialog` (OK only), `AssembleDialog` (Assemble/Close, repeatable), `DisassemblyProgressDialog` (Cancel only, closed by the disassembly) |

### RASTER Music Tracker: 21 dialogs, 20 on `ModalDialog`

Every dialog except `AboutDialog` extends `ModalDialog` - the result of
its port plan 18 ("Standard ones on WUDSN Base `ModalDialog`"). The
pattern:

```java
ChangeMaxTrackLengthDialog(JFrame parent, ...) {
    super(parent, Texts.ChangeMaxTrackLengthDialog_Title);
    JPanel grid = new JPanel(new GridBagLayout());
    DialogSupport.add(grid, ..., x, y, width, stretch);   // RMT's own GridBag helper
    getContentPane().add(grid, BorderLayout.CENTER);
}
protected boolean validateOK() { return DialogSupport.validateInt(this, lengthField, 1, Track.TRACKLEN); }
int showDialog() { showModal(lengthField); return okPressed ? ... : -1; }
```

- None of the 20 uses `fieldsPane` (the `SpringLayout` panel); all add
  their own panel at `BorderLayout.CENTER` - exactly what DIS6502's
  dialogs do.
- 4 add buttons with `addButtonBarButton` (Tuning: Reset/Test; Options,
  Instrument Change, Block Effect).
- Hooks used: `validateOK` (6), `dataFromUi` (5), `dataToUi` (4),
  `okPressed` (most).
- Owners are `JFrame`s; nested dialogs cast `(JFrame) getOwner()`.
- RMT's `DialogSupport` (package-private) holds the GridBag helper,
  `createGroup(title)`, and `parseInt`/`validateInt`/`validateDouble`
  with a message and focus on the bad field.

### The!Cart Studio: 4 dialogs

`OptionsDialog` and `WorkbookOptionsDialog` extend `ModalDialog` and do use
`fieldsPane`, filled with `SpringUtilities.createXxx(fieldsPane, ...)`.
`AboutDialog` and `ContentTypesDialog` extend `SimpleDialog`.

### What `ModalDialog` does

- `ModalDialog(JFrame parent, String title)`: modal; `fieldsPane` (a
  `SpringLayout` panel) at the top of the content pane; a button bar
  (`ElementFactory.createButtonBar()`: glue, then OK, Cancel) at the
  bottom; OK as default button and Escape as Cancel
  (`ElementFactory.setDialogDefaultButtons`).
- `showModal(focusField)`: `dataToUi()`, pack, center on the parent, focus
  the field, block.
- OK: `dataFromUi()`, `dataToUi()`, then `validateOK()` - if false, the
  dialog stays open. Cancel, Escape and the window's close box leave
  `okPressed` false.
- `addButtonBarButton(button)` adds a button at index 0 of the bar, i.e.
  left of the glue: at the far left.
- The OK button has no mnemonic (`createButton(..., false)`); Cancel's text
  is set by `ElementFactory.setButtonText`.
- The dialog is hidden, never disposed (`HIDE_ON_CLOSE`, the `JDialog`
  default).

## Gaps between `ModalDialog` and DIS6502

1. **Owner type.** DIS6502's dialogs take a `Frame` (`mainWindow.getFrame()`)
   and `DialogTextsTest` creates them with `null`; `ModalDialog` takes a
   `JFrame`. Change the parameter to `java.awt.Window`: source-compatible
   for RMT's and The!Cart Studio's `JFrame` arguments and their `(JFrame)
   getOwner()` casts; `null` remains allowed, as for `JDialog`.
2. **Mnemonics** (decision 1). DIS6502's OK and Cancel buttons have
   mnemonics (`createButton(..., true)`), `ModalDialog`'s OK has none.
3. **Disposal** (decision 3). DIS6502 creates a dialog per use and disposes it on the
   close box (`DISPOSE_ON_CLOSE`), but hides it on OK/Cancel - like
   `ModalDialog`. Each project creates its dialogs per use, so `ModalDialog`
   can dispose itself when `showModal` returns. Without it, every opened
   dialog keeps its native window until the owner is disposed. RMT and The!Cart
   Studio must be checked for a dialog shown twice (none found so far: RMT
   creates a new dialog for each use). RMT's `OptionsDialogsTest` creates
   dialogs with a `null` owner and calls `dataToUi`/`dataFromUi`/
   `validateOK` without showing them - disposal in `showModal` does not
   touch that.
4. **A third result** (`ROMTypeDialog`'s "Open as Raw File"). A protected
   `close()` in `ModalDialog` (hide without OK) lets a subclass end the
   dialog from its own button and keep its own result flag.
5. **The button bar's order.** `addButtonBarButton` puts buttons at the far
   left - right for "Restore Defaults" (`OptionsDialog`, left today), and
   acceptable for "Load/Save Profile" (between the left edge and OK today).
6. **`dataToUi()` after `dataFromUi()` on OK.** It refreshes the fields
   with the values just read - harmless; DIS6502's dialogs fill their
   fields in `show(...)` before `showModal`, and `dataToUi` stays empty.

No change is needed for the layout: like RMT, DIS6502's dialogs add their
form at `BorderLayout.CENTER` and leave `fieldsPane` empty.

## Design

### WUDSN Base (`com.wudsn.tools.base.gui.ModalDialog`)

- Constructor `ModalDialog(Window owner, String title)` instead of
  `JFrame parent` (gap 1).
- `showModal(focusField)` disposes the dialog after it was closed (gap 3);
  its javadoc says that a `ModalDialog` is shown once.
- `protected final void close()`: closes without OK (gap 4).
- OK and Cancel get their mnemonics (`createButton(..., true)` for both),
  in all three projects (decision 1).
- Java 8 source level; class javadoc describing the subclass contract:
  form at `CENTER` or in `fieldsPane`, the three hooks, `okPressed`,
  `addButtonBarButton`, `close()`.
- RMT and The!Cart Studio are compiled against the changed class; their
  dialogs need no source change.

### DIS6502

Groups A-D move onto `ModalDialog`, each like this:

```java
public final class WorkspaceDialog extends ModalDialog {
    public WorkspaceDialog(Window owner) {
        super(owner, Texts.WorkspaceDialog_Title);
        ... JPanel formPanel = new JPanel(new GridBagLayout()) ...
        getContentPane().add(formPanel, BorderLayout.CENTER);
    }

    @Override
    protected boolean validateOK() { ... JOptionPane message, return false ... }   // former performOK checks

    public boolean show(Workspace workspace) {
        ... fill fields ...
        showModal(computerSystemField);
        if (okPressed) { ... commit to the model ... }
        return okPressed;
    }
}
```

- The `show(...)` signatures stay, so `Dis6502` and the tests do not
  change, except the owner type where a `Window` is now passed.
- The commit to the model stays where it is today (in `show` after OK, or
  in `validateOK` where the dialog checks and applies in one step, e.g.
  `EquateRangeDialog`).
- Group C uses `addButtonBarButton`; `ROMTypeDialog`'s raw-file button
  sets its flag and calls `close()`.
- Group D: `SegmentWriteBootDiskDialog` writes in `validateOK`, returning
  false if writing failed (the dialog stays open, as today).
- Group E stays on `JDialog` - none of them is an OK/Cancel dialog. They
  keep `ElementUtilities.closeOnEscape`, which then has only these users.

## Tests

- **DIS6502:** `DialogTextsTest` constructs every dialog with a `null`
  owner and checks its texts - it must pass unchanged, which also checks
  the owner change. `UIWiringTest` drives several dialogs through
  `Dis6502`. Both test modes must pass.
- **By hand, with screenshots** (native Look and Feel), for one dialog per
  group: layout and button bar, Enter = OK, Escape = Cancel, the close box,
  a validation error keeping the dialog open, mnemonics.
- **WUDSN Base:** a `ModalDialogTest` (with a display; skipped headless,
  as `JDialog` needs one): OK with `validateOK` false keeps it open, OK
  with true sets `okPressed`, Cancel/Escape/`close()` leave it false, the
  dialog is disposed after `showModal`, `addButtonBarButton` order.
- **RMT and The!Cart Studio:** both compile against the changed
  `ModalDialog`; RMT's tests pass.

## Steps

1. **WUDSN Base:** `ModalDialog` changes and `ModalDialogTest`; `javac
   --release 8`; install; compile RMT and The!Cart Studio. Commit in WUDSN
   Base.
2. **DIS6502, group A** (10 dialogs); tests; screenshots of two of them.
3. **DIS6502, groups B-D** (7 dialogs); tests; screenshots.
4. **Docs:** `RULES_WUDSN_BASE.md` gets the rule "an OK/Cancel dialog
   extends `ModalDialog`"; `ElementUtilities`' javadoc names its remaining
   users; `plans/README.md`. No `CHANGES.md` entry: DIS6502's dialogs
   look and behave as before. RMT's and The!Cart Studio's OK/Cancel
   buttons gain mnemonics - a note for their own change logs.

## Progress

**Step 1 (2026-10-05), WUDSN Base `7e88ea5`:** `ModalDialog` takes a
`Window`, gives OK and Cancel their mnemonics, disposes itself after
`showModal`, and has `close()`. Fixed on the way: after OK with a failing
check, the close box reported OK. `ModalDialogTest` (6 tests, with a
display) drives a shown dialog through every way of closing it; undoing the
disposal, the close-box fix or the OK mnemonic fails it. RMT (666 tests)
and The!Cart Studio build unchanged against it.

**Step 2 (2026-10-05):** the 10 dialogs of group A extend `ModalDialog`.
Their `performOK` became `validateOK`, returning false where the dialog
stays open (`EquateRangeDialog`, `SegmentPropertiesDialog`,
`MemoryInspectorFindStringDialog` with their messages); `show` calls
`showModal` and returns `okPressed`.

- A gap the plan missed: four dialogs enable OK only for complete input,
  and `DiskImageExecutableFileDialog` clicks it on a double click.
  `ModalDialog` got `getOKButton()` for that (with a test).
- `DiskImageExecutableFileDialog` and `RawFileDialog` had a fixed size,
  which `showModal`'s `pack()` would override; their scroll panes got a
  preferred size instead (the raw file dialog is 694x497 instead of
  700x500).
- `SelectGraphicsDialog`, `DiskImageExecutableFileDialog` and
  `RawFileDialog` had fields below their main area next to the buttons;
  these now sit in one panel at `CENTER`, above `ModalDialog`'s button bar.
- Visible difference: OK/Cancel are at the right instead of centered.
  Two layout details of WUDSN Base were fixed on the way, for all projects:
  the button bar's border is 5 pixels on all sides (was 5, 5, 0, 5 - the
  buttons nearly touched the bottom edge), and neighboring buttons are
  `ModalDialog.BUTTON_GAP` (5) pixels apart instead of touching (checked
  by `ModalDialogTest`).

Both test modes pass. `UIWiringTest` failed twice with "cannot open system
clipboard" during a parallel Claude session for RMT, which used the
clipboard at the same time; repeated afterwards, it passed three of three.
Screenshots of the workspace, low/high byte, address range
and raw file dialogs checked the layout.

**Step 3 (2026-10-05):** the 7 dialogs of groups B-D extend `ModalDialog`;
only group E (About, Assemble, Disassembly Progress) stays on `JDialog`.

- `DialogTextsTest` clicks `ROMTypeDialog`'s buttons without showing the
  dialog; it cancels through the Escape action now, since `ModalDialog`'s
  Cancel button is private (a `getCancelButton()` for this test alone was
  added and removed again - a library method only tests need).
- Extra buttons go to the left of the button bar with
  `addButtonBarButton`: Restore Defaults (`OptionsDialog`), Load/Save
  Profile (`ProfileDialog`), Open as Raw File (`ROMTypeDialog`, which ended
  between OK and Cancel before). "Open as Raw File" sets its flag and calls
  `close()`; `isConfirmed()` is OK or raw file.
- `DefaultFoldersDialog` and `EquateDialog` set their title in `show`;
  they start with an empty or the edit title.
- `EquateDialog`, `ROMTypeDialog` and `DiskImageSectorsDialog` had fixed
  sizes; their list or middle area got a preferred size instead.
- Correction to the design: `SegmentWriteBootDiskDialog` closes after a
  write error too (it shows the error first), as before - the plan assumed
  it stayed open. Cancelling its file chooser keeps it open.
- With the base `Actions` import no longer needed for OK/Cancel, the
  dialogs refer to DIS6502's `Actions` by its simple name again.

Both test modes pass. The layout of all seven was checked on images the
dialogs painted themselves (`rootPane.printAll`) - a first attempt with
screen captures recorded other windows in front of the dialogs and was
deleted.

**Step 4 (2026-10-05):** `RULES_WUDSN_BASE.md` describes `ModalDialog` and
`MRUMenu` and has the rule "an OK/Cancel dialog extends `ModalDialog`"; its
rule for runtime-generated menu items points to `MRUMenu`.
`ElementUtilities.closeOnEscape`'s javadoc names its two remaining users.
`plans/MEMORY.md` has the lesson on checking dialog layouts with painted
images instead of screen captures. Afterwards, `RULES_WUDSN_BASE.md` moved
to WUDSN Base's `plans/` folder, generalized for every application on
WUDSN Base (the wiring rule describes DIS6502's direct wiring and RMT's
command dispatch side by side); DIS6502 and RMT refer to it there.

## Decisions (2026-10-05)

1. **Mnemonics on OK and Cancel for all projects:** `ModalDialog` creates
   both buttons with their mnemonics, so DIS6502 keeps them and RMT and
   The!Cart Studio gain them.
2. **RMT's `DialogSupport` stays in RMT for now.** This plan changes only
   the dialog frame, not how DIS6502's forms are built.
3. **`ModalDialog` disposes itself** when `showModal` returns: a modal
   dialog is created for one use.
