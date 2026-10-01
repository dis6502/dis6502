# Proposal: add ElementFactory.createCheckBox/createRadioButton(DataType)

**Status: done, 2026-09-23.** `ElementFactory.createCheckBox`/
`createRadioButton(DataType)` added to WUDSN Base as proposed;
`ElementUtilities.applyLabel(AbstractButton, DataType)` and all three
duplicated wrapper methods removed from dis6502, all 24 call sites
(`ProfileDialog` - 18 checkboxes + 3 radio buttons, `SegmentPropertiesDialog` -
1 checkbox, `MemoryInspectorFindStringDialog` - 2 radio buttons) switched
to the new factory methods directly. Verified with a real (non-headless)
`TestRunner` run - `DialogTextsTest` actually constructs all three
dialogs and checks their texts/mnemonics/tooltips, not just a compile
check.

**Follow-up, same day:** `ElementUtilities`'s remaining method,
`applyLabel(JLabel, DataType, JComponent)` (mutates an existing label in
place - `LowHighByteDialog`'s only remaining local helper), moved into
`ElementFactory` too, as a public overload of the same name. While
touching that code, `createLabel(DataType, JComponent)` was rewritten to
construct a `JLabel` and delegate to the new overload instead of carrying
its own separate copy of the `&`-mnemonic-parsing logic, and the
`AbstractButton` counterpart (previously a private `applyDataTypeLabel`
inside `ElementFactory`) was renamed to a third `applyLabel` overload and
made public too, for the same "mutate an existing instance" use case
`createCheckBox`/`createRadioButton` didn't need but a future caller
might. `ElementUtilities.java` now holds only `closeOnEscape` -
everything `DataType`-label-related lives in `ElementFactory`.

## What's actually duplicated

`ElementFactory` already has `createLabel(DataType dataType, JComponent
field)` - builds a `JLabel` whose text/mnemonic/tooltip come from a
`DataType`, for a form field's paired label. It has no equivalent for a
*self-labeled* control (a `JCheckBox`/`JRadioButton` that carries its own
text instead of being paired with a separate label). dis6502 filled that
gap itself, locally: `com.wudsn.tools.dis6502.ui.ElementUtilities.applyLabel(AbstractButton,
DataType)`, a ~20-line hand-rolled re-implementation of the same
`&`-mnemonic parsing `createLabel`/`setButtonTextAndMnemonic` already do
elsewhere.

That local helper is then wrapped by **three duplicated, near-identical
factory methods**, one per file that needs it:

```java
private static JCheckBox checkBox(DataType dataType) {
    JCheckBox checkBox = new JCheckBox();
    ElementUtilities.applyLabel(checkBox, dataType);
    return checkBox;
}
```

- `ProfileDialog.checkBox(DataType)` - **17 call sites** in that one file
  (every profile settings checkbox: illegal opcodes, hex notation, align
  instructions, line numbering, ...).
- `SegmentPropertiesDialog.checkBox(DataType)` - 1 call site
  (`binaryCheckBox`), identical body.
- `ProfileDialog.radioButton(DataType)` - 3 call sites (the three
  `INCLUDE` file-layout options), identical body but for `JRadioButton`.
- `MemoryInspectorFindStringDialog.radioButton(DataType)` - 2 call sites
  (All Segments / Selected Segment), identical body.

23 call sites total, across four files, all going through one of two
verbatim-duplicated three-line wrapper methods, which in turn both call
into one hand-maintained local re-implementation of logic `ElementFactory`
already has a working version of for the `JLabel` case.

## What's *not* actually a gap (corrects a stale claim)

`plans/RULES_WUDSN_BASE.md` previously said `ElementFactory` had no
`createCheckBoxMenuItem` - that was stale; it has had one for a while
(`com.wudsn.tools.dis6502.ui.MainMenu`/`DisassemblyPanel`/
`MemoryInspectorPanel` already all use it directly), and `createToggleButton`
too. Fixed that document alongside this proposal regardless of what's
decided here. Two other things worth ruling out explicitly since the user
asked about "menu items" broadly:

- Plain menu items (`JMenuItem`/`JMenu`) are already fully covered by
  `createMenuItem`/`createMenu` - no gap.
- `JRadioButtonMenuItem` is not used anywhere in dis6502 today. A factory
  method for it would be speculative, not a current simplification - not
  proposed here; add it if/when a real use case shows up.

## The proposal

Add to `com.wudsn.tools.base.gui.ElementFactory`, mirroring
`createLabel(DataType, JComponent)`'s existing shape:

```java
public static JCheckBox createCheckBox(DataType dataType) { ... }
public static JRadioButton createRadioButton(DataType dataType) { ... }
```

Implemented with the same `&`-mnemonic-parsing logic `createLabel`/
`setButtonTextAndMnemonic` already have (worth factoring the parsing
itself into one shared private helper while touching this code, since
`createLabel` currently hand-rolls its own copy of that logic separately
from `setButtonTextAndMnemonic`'s copy - a smaller, pre-existing
duplication inside `ElementFactory` itself, unrelated to dis6502).

Then in dis6502:

- Delete `ElementUtilities.applyLabel(AbstractButton, DataType)` (the
  overload taking a `JLabel` stays - it's a different, still-needed case:
  mutating an existing label in place for a dialog that re-labels one
  field depending on runtime state, e.g. `LowHighByteDialog`'s Low/High
  Byte swap).
- Delete all three duplicated wrapper methods.
- Update the 23 call sites from `checkBox(DataTypes.X)`/
  `radioButton(DataTypes.X)` to `ElementFactory.createCheckBox(DataTypes.X)`/
  `ElementFactory.createRadioButton(DataTypes.X)`.

## Net effect

- WUDSN Base gains two small, focused factory methods (reusable by any
  other WUDSN Swing tool with the same self-labeled-control-from-a-
  `DataType` need - none currently have it, based on a search of the
  `WUDSN-Base` repo, but neither did `createCheckBoxMenuItem` before it
  was added there for this project's own sake).
- dis6502 loses one ~20-line local helper method and three duplicated
  ~4-line wrapper methods (~32 lines total) from its own `ui/` package.
  Each of the 23 call sites gets a few characters longer
  (`ElementFactory.createCheckBox(...)` vs. the current bare
  `checkBox(...)`) but loses a layer of local indirection in exchange -
  a neutral-to-positive trade, not a meaningful call-site regression.
- No behavior change anywhere: this moves working, already-correct label/
  mnemonic/tooltip logic, it doesn't change it.

## Not recommended

- A `JRadioButtonMenuItem` factory method - no current call site to
  justify it (see above).
- Merging the `Action`-keyed and `DataType`-keyed factory method families
  into one generic thing. They're genuinely different repository types -
  `Action` carries an accelerator and is paired with an `actionCommand`;
  `DataType` carries neither and is paired with a field instead. Keeping
  them as parallel, separately-keyed method families (as `createMenuItem(Action,
  ...)` and `createLabel(DataType, ...)` already are) matches the existing
  design and `plans/RULES_WUDSN_BASE.md`'s own documented split between
  the two kinds of repository.
