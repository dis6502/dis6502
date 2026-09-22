# Plan: headless-safe test coverage after KeyStroke's headless fix

Status: proposal, 2026-09-23. Nothing below is implemented.

## Background

WUDSN Base's `KeyStroke` used to compute `M1` unconditionally via
`Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()`, which throws
`HeadlessException` on a JVM with no display. Since
`com.wudsn.tools.dis6502.Actions` pre-initializes several `Action` fields
with `KeyStroke.M1` right in Java source (e.g. `new Action(KeyEvent.VK_N,
KeyStroke.M1)`), simply *loading* the `Actions` class failed on a headless
JVM - and so did anything that touches it, including `MainMenu` and every
panel that builds a popup menu via `ElementFactory`. `KeyStroke.M1` now
falls back to `ActionEvent.CTRL_MASK` when `GraphicsEnvironment.isHeadless()`,
so `Actions` (and anything depending on it) can load headless.

**This does not remove every headless blocker in `ui/`** - only the
class-loading one. Constructing an actual `JDialog`/`JFrame` (or anything
else extending `java.awt.Window`) still throws `HeadlessException` from
`Window`'s own constructor, unrelated to `KeyStroke`; showing a
`JPopupMenu` still needs a real, showing parent component. Both remain
genuine, unavoidable headless blockers.

## Verified

Confirmed empirically (a throwaway headless probe, `-Djava.awt.headless=true`,
constructing each class directly):

- `new MainMenu()` and `new DisassemblyPanel()`/`new LogPanel()`/
  `new XRefPanel()`/`new SegmentListPanel()`/`new MemoryInspectorPanel()`
  **now all construct successfully headless** - none of them extend
  `Window` (`MainMenu` is a plain class, the five panels extend `JPanel`),
  and the only thing that previously blocked them (`Actions` failing to
  load) is fixed.
- `new AboutDialog(null)` (and by the same reasoning every other dialog in
  `ui/`) **still throws `HeadlessException`** - `JDialog` extends
  `java.awt.Window`, which checks headlessness in its own constructor,
  independent of `KeyStroke`.
- This only works with a **rebuilt and reinstalled** `com.wudsn.tools.base`
  SNAPSHOT jar - the one in the local Maven repository was built 2026-09-20,
  before this fix (`mvn -o install -DskipTests` from
  `WUDSN-Base/com.wudsn.tools.base` refreshes it). Worth a note in
  `plans/PORTING_GUIDE.md`'s build instructions that a WUDSN Base source
  change needs a reinstall before dis6502's own build picks it up - true
  before this fix too, just not yet written down anywhere.

## Proposal

1. **Split `DialogTextsTest.testPanelsAndMenu()` out from behind the
   `isHeadless()` guard.** It builds `MainMenu` and the five panels and
   checks their texts/mnemonics/item count (including the hard-coded
   36-item assertion) - none of that needs a display now. Only
   `testDialogs()` (constructs every real `JDialog`) and
   `testValueSetFields()` (also constructs `JDialog`s, to reach their
   `ValueSetField`s) still need the guard. Splitting this out means the
   main menu's and five panels' wiring gets checked on every CI run, not
   only when a display happens to be available (or `-Ddis6502.skipUITests`
   happens to be unset) - a real, currently-silent coverage gap.
2. **Add a small, purely-reflective `ActionsTest`**, mirroring the
   existing headless `DataTypesTest`: iterate `Actions`' public static
   `Action` fields and check each label has exactly one `&` mnemonic
   marker, mnemonics are unique among the items of the same menu, and a
   field pre-initialized with an accelerator in Java source
   (`new Action(keyCode, modifiers)`) still has a non-null accelerator
   after `NLS` population (the "preserve the accelerator" contract
   documented in `plans/RULES_WUDSN_BASE.md`). `ElementFactory` already
   fails fast at build time if a label has no mnemonic at all, so this
   mainly adds the one check nothing else catches: two items in the same
   menu accidentally sharing a mnemonic letter. Entirely reflective, no
   Swing component construction needed at all - not even a `JPanel`.

## Not proposed

- `RenderingTest` and `UIWiringTest` keep their guards as-is: the former
  needs a real, showing component for its popup-trigger `MouseEvent` and
  off-screen grid painting, the latter starts the real application window
  and clicks real dialogs - both are genuine, unrelated headless blockers
  this fix does not touch.
- `testDialogs()`/`testValueSetFields()` inside `DialogTextsTest` keep
  their guard for the same reason (`JDialog` construction).

## Open questions

1. Implement the `DialogTextsTest` split now?
2. Add the new `ActionsTest`?
3. Note the WUDSN Base reinstall step in `plans/PORTING_GUIDE.md`?
