# Proposal: which UI smoke tests become tests in the repository

Status: implemented 2026-09-22, in the suggested order - settings isolation
(`Application(Preferences)`, `dis6502.settingsNode`, `TestRunner.run`),
`DialogTextsTest`, `UIWiringTest` (the eight scenarios), `RenderingTest`,
and `TestRunner` in the Maven build through the JUnit 3 bridge
`TestRunnerTest`. Two things turned out differently from the text below:
nothing of the `ui` package loads headless (WUDSN Base's `Actions` needs
the toolkit), so all three UI tests skip without a display rather than
running "headless where possible"; and the CI build passes
`-Ddis6502.skipUITests=true`, since its Windows runner has a desktop but
nobody to look at the windows. The scratch programs the tests replaced can
go. The text below is the proposal as written.

**2026-09-23 update:** a later WUDSN Base fix (`KeyStroke.M1` no longer
throws headless) made the "loads headless" premise true after all for
everything except real dialogs - see `plans/HEADLESS_ACTIONS_TEST_PROPOSAL.md`.
`DialogTextsTest` was split accordingly: `PanelTextsTest` (the main menu
and every panel, plus per-menu mnemonic-uniqueness checks) now always
runs, headless or not; `DialogTextsTest` keeps only the real-dialog and
`ValueSetField` coverage, still skipped without a display. Their shared
component-tree/text-checking helpers moved to `UITest`. `RenderingTest`
was split the same way: its two grid-painting checks paint directly into
an off-screen image without ever adding the grid to a real window, so
they now always run too; only the popup-menu check (needs a real,
showing parent component to trigger a real `JPopupMenu`) moved out, into
the new `PopupStructureTest`, and stayed guarded. `UIWiringTest` has no
such split available: it starts the real application window as its very
first step, so every one of its scenarios needs a display from the
start.

## Background

During the last sessions, about 45 throw-away programs in
`C:\TEMP\claude\dis6502-smoketest` exercised the real application: they
launch `Dis6502.main`, reach into the running instance by reflection, drive
selection, menus and dialogs, and assert on the real `Workspace`, the real
Swing components and, in a few cases, on `Robot` screenshots. They found real
defects (the C64 `.prg` never becoming a binary segment, code trace ignoring
segments without an Atari header, the ATARI800 reset on open, the menu items
that stayed enabled during an edit) that none of the 24 headless tests could
have found, because those defects sit in the wiring between model and UI.

The scratch programs are not part of the repository and will be lost. This
proposal sorts them into what is worth keeping as a test and how.

## What the scratch programs are

| Group | Programs | What they exercised |
|---|---|---|
| A. Wiring through the real `Dis6502` | `OpenDispatchSmoke`, `KeepSystemSmoke`, `SystemEquatesSmoke`, `MenuGateSmoke`, `NavTypeSmoke`, `DeleteCutPasteSmoke`, `AnyFileSmoke`, `ChooserSmoke` | Launch the app, call `Dis6502.openFile`/`performXxx` or click the real menu items, assert on workspace state, menu enablement, chooser filters and start folders |
| B. Dialogs and panels stand-alone | `DropDownsSmoke`, `EncodingComboSmoke`, `FolderLabelsSmoke`, `StringsCheck`, `ActionsCheck`, `LogPanelSmoke`, `TitleSmoke` | Construct one dialog/panel without the app, inspect its components' texts, items and selection |
| C. Painted output | `ScreenPass`, `DragDropPass`, `HeaderScreenshot`, `Screenshot`, `StringsShot` | `Robot` screenshots of the real windows, a real OS-level drag and drop |
| D. Model probes | `C64Listing`, `Hello`, `FlagProbe`, `GenC64Wrk`, `Dbg*`, `MessagesSmoke*`, `TextsSmoke*`, `TitleFormatSmoke` | Headless model calls to look at output while developing |
| E. Settings hygiene | `MruDump`, `MruClear`, `PrefDump`, `PrefClean*` | Inspect and clean the Java `Preferences` the runs left behind |

Group D is already covered: everything it probed is asserted by
`ProfileNotationTest`, `ReassemblyRoundTripTest`, `WorkspaceLogicTest`,
`ValueSetsTest`, `EncodingTest`, `FolderTypeTest` and `FileChoosersTest`.
Group E is not a test; it becomes infrastructure (see "Isolating the
settings").

## What should become a test

### 1. `UIWiringTest` - the application wired up, headless where possible (group A)

One test class that starts the real `Dis6502` once (as `ScreenPass` does),
with the frame created but not shown when the environment is headless, and
runs these scenarios against it:

| Scenario | From | Asserts |
|---|---|---|
| Open keeps the computer system | `KeepSystemSmoke` | after `setComputerSystemTypeID("C64")`, opening `HelloWorld.prg` through `openRecentFile` leaves C64 and yields the `$0801` segment |
| System equates follow the system | `SystemEquatesSmoke` | 899/50/534/159 equates after switching to ATARI800/ORIC/C64/ATARI5200; Clear System Equates enabled; IOCB offset labels referenced after opening `autorun.xex` |
| One dispatcher for every way of opening | `OpenDispatchSmoke` | command line `/c64 HelloWorld.prg`; two dropped files (first opened, second added, 8 segments); an untyped `.wrk` recognized by extension; an unrecognizable file offered as raw file, and cancelling leaves the workspace untouched |
| File menu gating | `MenuGateSmoke` | per system (C64: no cassette/disk/ROM items; Atari 5200: raw and ROM only), Save items only with segments, everything off during edit mode and back afterwards |
| Navigate back and immediate type | `NavTypeSmoke` | Navigate to Definition from a `jsr`, Back returns, history empty; `ldx #$30` reported immediate with char allowed, `jsr` not; Code with Char Constant yields `ldx #'0'` |
| Cut/Copy/Paste/Delete | `DeleteCutPasteSmoke` | Copy puts the hex string on the clipboard; Delete shrinks by the selection; Paste restores the bytes; Cut of a whole small segment removes it; invalid clipboard content shows the error dialog |
| Open/Add Any File | `AnyFileSmoke` | the filter lists every readable type of the current system plus `.wrk`; picking an `.xex` opens it, Add adds |
| Chooser start folder and filter | `ChooserSmoke` | title, start folder and filter description of the open/save choosers per file type |

How the scenarios reach the private parts of `Dis6502`: today by
reflection (`getDeclaredField`/`getDeclaredMethod`, `setAccessible`). That
works and needs no production change, but it breaks silently on a rename.
Better: a package-private `Dis6502.TestAccess` (or making the handful of
`performXxx` methods and the `workspace`/`mainWindow` fields package-private
and putting the test in the same package, which `TestRunner` already is).
The test then reads like the scratch programs without the reflection noise.

Two scenarios need a dialog to appear and be dismissed (the error dialog in
Cut/Copy/Paste, the raw file dialog in the dispatcher scenario, the
choosers). The pattern from `ScreenPass` covers that headlessly enough: open
via `invokeLater`, find the `JDialog` in `Window.getWindows()`, act on its
real buttons with `doClick()`/`cancelSelection()`. No `Robot` is needed.

What stays out of it: the screenshots.

### 2. `DialogTextsTest` - every dialog constructs and shows its texts (group B)

For each dialog and panel class: construct it with `null` owner, walk its
component tree, and assert that

- no `JButton`, `JLabel`, `JCheckBox`, `JMenuItem` or `TitledBorder` shows
  an empty text, a raw property key or a text containing `{0}` that was not
  meant to stay templated;
- every `ValueSetField` lists the expected values in the expected order
  (`DropDownsSmoke`: "Atari 5200 | Atari 800 | C64 | Oric", "MOS 6502 |
  MOS 65C02", the eight ANTIC lines, "ASCII | ATASCII | UTF-8");
- a `ValueSetField` round-trips: `setValue`, then `getValue` is the same
  instance (`EncodingComboSmoke`).

This is the cheapest test with the widest reach: it catches a missing
`Texts`/`Actions`/`ValueSets` property (which today aborts the program at
class load - `DataTypesTest` covers `DataTypes` the same way already), a
dialog whose constructor throws, and a combo box built from the wrong list.
It runs headless: constructing Swing components needs no display.

### 3. `RenderingTest` - what is painted (group C), the one part that needs a display

`ScreenPass` and `DragDropPass` are the only tests that saw pixels: the
popup with its check marks, the overwrite prompt, a real drag from another
window. Screenshots cannot be asserted on without golden images, and golden
images break with every look-and-feel, font and DPI change. The pieces
worth keeping as assertions do not need pixels:

- popup menu structure after a right-click on a `jsr` line: the item set,
  their enabled states and the submenu's check mark - all readable from the
  `JPopupMenu` once `maybeShowPopup` ran (a synthesized popup-trigger
  `MouseEvent` through `dispatchEvent`, as `ScreenPass` does);
- `DisassemblyGridPanel` and `HexGridPanel` paint into an off-screen
  `BufferedImage` without a display: assert that a selected line's row
  contains the highlight color and an unreferenced equate's row the grey,
  nothing more.

The OS-level drag and drop stays a manual check; it needs a real desktop
and a second window, and its Java side (`TransferHandler.importData`) is
already covered by the dispatcher scenario feeding the handler a file list.

Everything that needs `Robot` or a visible window is skipped, not failed,
when `GraphicsEnvironment.isHeadless()` is true, so the suite passes on the
CI runner (`release.yml` runs `mvn install`, which does not run
`TestRunner` today - see "Running").

## What should not become a test

- `Dbg*`, `Hello`, `C64Listing`, `FlagProbe`, `GenC64Wrk`: one-off probes,
  their findings are in the headless tests now. `GenC64Wrk` is worth keeping
  as a documented script for regenerating `HelloWorld.wrk` - put its body in
  a comment of `WorkspaceLogicTest` or in `test-resources/system/c64/README`.
- `MessagesSmoke*`, `TextsSmoke*`, `TitleSmoke`, `TitleFormatSmoke`: from
  the `Text`→`Texts`/`Messages` migration; superseded by `DataTypesTest` and
  the class-load check.
- `HeaderScreenshot`, `Screenshot`, `StringsShot`: screenshot helpers.
- `MruDump/Clear`, `PrefDump/Clean*`: infrastructure, below.

## Isolating the settings

The scratch runs wrote into the user's real Java `Preferences` (Recent
lists, default folders) and had to be cleaned by hand afterwards. A UI test
in the repository must not do that. `Application.getSettingsSection` uses
`Preferences.userNodeForPackage(Application.class)`; the test needs a
switch to a throw-away node. Two options:

1. A system property (`dis6502.settingsNode=test`) that `Application`
   appends to the node path, set by `TestRunner` before anything loads;
   the test removes the node in a `finally`.
2. `Application` takes the root `Preferences` node as a constructor
   argument (default `userNodeForPackage`); `UIApplication` gets a
   package-private constructor for tests.

Option 2 is cleaner and testable itself; option 1 is one line. Either way
`MruList`/`DefaultFoldersLogic` are untouched.

`Dis6502` is a singleton (`instance`), so the wiring test starts the
application once for all its scenarios and calls `workspace.init()` between
them - as the scratch programs did - rather than restarting it.

## Running

`TestRunner` is a plain `main`; `mvn install` on CI does not run it (no
Surefire provider in the offline repository, see `TestRunner`'s javadoc).
The UI tests should join `TestRunner` like every other test, with the two
`Robot`-free classes (`UIWiringTest`, `DialogTextsTest`) always run and
`RenderingTest` skipping itself when headless. Wiring `TestRunner` into the
build (an `exec-maven-plugin` step, or a Surefire provider once one is
available offline) is a separate, small change worth doing at the same
time; until then the suite stays a manual `java -cp ... TestRunner` call.

`UIWiringTest` starts the whole application and loads real fixtures; expect
5-10 seconds. Keep it last in `TestRunner` so the fast headless tests report
first.

## Suggested order

1. Settings isolation (prerequisite; otherwise a failing UI test leaves junk
   in the user's preferences).
2. `DialogTextsTest` - smallest, widest.
3. `UIWiringTest` - the eight scenarios above, converted one at a time from
   the scratch programs while they still exist.
4. `RenderingTest` - the popup structure and the two off-screen paints.
5. `TestRunner` in the Maven build.
