# Remaining Gaps: C++ dis6502 vs. Java jdis6502 Port

This document inventories what still works in the C++ `dis6502` project
(`C:\jac\system\Windows\Programming\Repositories\dis6502`) but is missing,
incomplete, or broken in this Java port, as of 2026-09-21. It exists so a
future porting session can pick up where this snapshot left off without
re-auditing both codebases from scratch.

## Methodology and confidence level

This was compiled by two passes - one over the model/logic layer
(`src/*.cpp`/`src/systems/` vs. `com.wudsn.tools.dis6502.model`), one over the
UI layer (`src/ui/*.cpp` vs. `com.wudsn.tools.dis6502.ui`) - each matching
C++ files to Java classes by name/package correspondence, spot-checking
method/field lists and javadocs for stated limitations, and grepping for
`TODO`/`FIXME`/stub markers. This is **not** a full command-by-command diff
against every C++ `.rc` resource or a line-by-line diff of every method
body; several items below are flagged explicitly as unverified rather than
guessed at. Treat "fully ported" entries as spot-checked, not proven, and
re-verify against the current C++ source before relying on any single line
here - both repos continue to change.

## Confirmed gaps - model/logic layer

### 1. ~~`DisassemblyWriter` hardcodes the Atari800 return character for every computer system~~ - FIXED 2026-09-21

- **Was**: `DisassemblyWriter.java:35` hardcoded
  `private final int returnCharacter = 0x9B;` (Atari800's value) regardless
  of which computer system the workspace actually used, with a stale
  class-javadoc claim that this was a placeholder "since `ComputerSystem` is
  not ported yet" - but `ComputerSystem`/`ComputerSystemFactory` had in fact
  already been fully ported and wired into `Workspace.getComputerSystem()`,
  with `ComputerSystem.getReturnCharacter()` already returning the correct
  per-system value (Atari800/Atari5200 = `0x9B`, C64/Oric = `0x0D`,
  Unknown = `0x0A`). The C++ constructor
  (`DisassemblyWriter.cpp:15`) reads this from
  `workspace.GetComputerSystem()->GetReturnCharacter()`; the equivalent Java
  wiring had simply never been done.
- **Fix**: `returnCharacter` is now set in the constructor from
  `workspace.getComputerSystem().getReturnCharacter()`, matching the C++
  wiring exactly; the stale javadoc paragraph was removed. Verified with
  `mvn -o compile`/`test-compile` and a full run of `TestRunner` (all 12
  tests still pass, including the `ComputerSystemTest`/`DisassemblyResult*`
  tests that exercise `DisassemblyWriter` via Atari800 and C64 fixtures).
- **Correction to the original write-up above**: the neighboring
  `isByteAllowedInString()` TODO -
  `// TODO: This actually depends on the character set of the computer system`
  (the `showNonASCIIChararactersAsBytes` check) - was described here as
  "related, same root cause, should be fixed alongside the above." That was
  wrong: this TODO comment exists **verbatim in the C++ source itself**
  (`DisassemblyWriter.cpp`'s own `IsByteAllowedInString`), so it's a
  pre-existing, shared C++ limitation, not a Java-specific gap. It was left
  untouched by this fix, correctly, per the porting guide's bug-handling
  policy (don't diverge from C++ where C++ itself has no established correct
  behavior to port).

### 2. ~~`EquateList.addEquate()` silently swallows parse errors instead of logging them~~ - FIXED 2026-09-21

- **Was**: `EquateList.java:296-299` had a javadoc admitting *"Unlike the C++
  version, a parse error is not yet reported anywhere (the C++ version sends
  it to the application's message log) - this is deferred until
  application-level logging is ported."* That was stale:
  `application.sendInfoMessage(...)`/`application.sendErrorMessage(...)` was
  already fully wired and used elsewhere in the model layer for exactly this
  kind of file-parse error/info reporting (`EquateListLogic.java`,
  `ProfileLogic.java`, `WorkspaceLogic.java`), and the exact message resource
  the C++ version sends (`IDS_ERR_CANNOT_PARSE_EQUATE_LINE`) was already
  ported into `Text.properties`/`Text.java` but referenced nowhere.
- **Fix**: `EquateList` itself still has no `Application` reference by
  design (it's kept a pure model class), so the fix threads the error back
  to the one caller that does have one, `EquateListLogic.load()`:
  - `Equate.ReadResult` (`Equate.java`) now carries the parsed, initialized
    `Equate` instance itself (`null` on error/`UNKNOWN`), computed once in
    its constructor via a new private `createEquate` helper.
  - `EquateList.addEquate(String)` now returns a new nested
    `EquateList.EquateResult` (`equate` + `error`) instead of a bare
    `Equate`, appending `result.equate` directly instead of re-deriving it.
  - `EquateListLogic.load()` inspects `result.error` per line and calls
    `application.sendErrorMessage(Text.IDS_ERR_CANNOT_PARSE_EQUATE_LINE,
    line, result.error)` - matching C++'s `EquateList::AddEquate(line)`
    exactly.
  - The one other caller, `EquateDialog.performAdd()`, was updated to
    unwrap `.equate` from the new return type; its own pre-existing
    `// TODO: ERROR HANDLING` (matching the C++ source's own unresolved
    TODO there) was left as-is - out of scope for this fix.
- Verified with a clean `mvn -o compile`/`test-compile` and a full
  `TestRunner` run (all 12 tests still pass, including
  `EquateListLogicTest`, which loads a 899-line real equates file with no
  new false-positive parse errors).

## Confirmed gaps - UI layer

### 3. Memory Inspector Delete/Cut/Paste Selection - not ported at all (deliberately, tracked as an open TODO)

- `MemoryInspectorPanel.java:102-115` documents the decision: the C++
  `MemoryInspector::DeleteSelection` never actually shrinks the segment's
  byte/type arrays (admitted in the C++ source's own comment), and
  `MemoryInspector::PasteAtSelection` is explicitly broken in C++ (the code
  that applies the paste buffer is commented out there). Porting either
  faithfully would just carry the brokenness forward.
- Fixing this for real needs actual segment-buffer resizing support, which
  `com.wudsn.tools.dis6502.model.MemoryBlock` doesn't have yet - this is a
  model-layer prerequisite for a UI-layer feature.
- Status: open TODO, not a closed/reviewed decision - worth scoping
  explicitly (per the porting guide's "decide feature scope per subsystem up
  front" rule) rather than leaving it as a standing comment indefinitely.

### 4. `LogPanel` is a plain `JTextArea`, not the C++'s multi-column, severity-colored list view

- `LogListWindow.cpp`'s C++ log window is a real list view: one row per log
  entry, columns, and severity-based coloring.
- `LogPanel.java`'s own javadoc says explicitly: "simplified... for this
  first pass - the C++ version's per-entry severity coloring/columns are not
  ported yet." It's currently a single scrolling `JTextArea`.
- Impact is cosmetic/usability (harder to scan for errors/warnings at a
  glance among info messages) rather than a functional loss - all messages
  are still logged and visible.

### 5. No Java equivalent of `MainUITest.cpp`'s interactive self-test harness

- C++ has a command-line self-test mode (`/TEST:DEEP`, `/TEST:FAST`),
  invoked by `build\test-dis6502-DEEP.bat` / `test-dis6502-FAST.bat`, that
  drives the UI programmatically in a loop.
- No Java class resembles this (no `/TEST:` argument handling, nothing under
  `test/` that mirrors an interactive/console self-test loop).
- The Java port does have a real JUnit suite, which is arguably a better
  substitute for regression coverage - but the specific interactive
  self-test-loop UX (useful for manual soak-testing against real program
  data while iterating) is absent. Worth an explicit "won't port, JUnit
  supersedes it" decision if that's the intent, rather than leaving it
  implicit.

### 6. ~~`DiskImageSectorsDialog`'s byte-range picker is Start/End offset fields, not a drag-selectable hex view~~ - FIXED 2026-09-21

- **Was**: the C++ dialog lets you drag-select a byte range directly in a
  hex-dump view (built on `MemoryInspectorControl`); the Java dialog used
  plain numeric Start/End Offset `JTextField`s instead, because a reusable,
  generic version of the memory inspector's grid control didn't exist yet
  (`MemoryInspectorGridPanel` was specific to the main Memory Inspector
  panel, hard-typed to `Segment`/`MemoryInspectorState`; renamed
  `HexGridPanel` the same day once generalized, see below - used by its
  current name throughout the rest of this entry).
- **Fix**: generalized the grid behind two new small
  abstractions and moved its drag-select mouse handling onto the grid
  itself (previously duplicated per-caller in `MemoryInspectorPanel`):
  - `HexGridByteSource` (new `ui` interface: `getSize()`/`getData()`/
    `getType()`/`getBaseAddress()`) replaces the grid's hard-typed `Segment`
    field. `SegmentByteSource` adapts a `Segment` (used by the main Memory
    Inspector, no changes to `Segment`'s own API); `DiskSectorByteSource`
    wraps a raw `byte[]` sector buffer (used by the dialog), always
    reporting `MemoryType.UNKNOWN` - already painted in plain black, so no
    separate "coloring off" mode was needed.
  - `ByteRangeSelection`/`MutableByteRangeSelection` (new `model` class
    pair, matching the existing `MemoryInspectorState`/
    `MutableMemoryInspectorState` read-only-interface/mutable-impl split)
    extract the pure begin/end swap-and-clamp arithmetic out of
    `MutableMemoryInspectorState`, which now delegates to an owned instance
    instead of tracking the fields itself (its own public API and the main
    Memory Inspector's behavior are unchanged). `DiskImageSectorsDialog`
    owns a second, fully independent instance - never touching
    `Workspace`, matching how the C++ `MemoryInspectorControl` is one
    reusable class with an independent instance per window.
  - `HexGridPanel.DragSelectionListener` (`onDragSelectionChanged`/
    `onDragSelectionFinished`) plus a `MouseAdapter` built into the grid's
    own constructor reproduce the anchor-tracking drag logic that used to
    live in `MemoryInspectorPanel` (`beginByteSelection`/
    `extendByteSelection`, now removed) - both callers now share the same
    code. `DiskImageSectorsDialog` only implements
    `onDragSelectionChanged` (a lambda): it pulls the current selection
    synchronously at "Add Sector" click time, defaulting to the whole
    sector if nothing was dragged, exactly matching C++'s
    `MemoryInspectorControl::GetSelection(..., bDefaultAll=true)` - the C++
    dialog never listens for `SELECTION_CHANGED` either.
  - Auto-scroll-past-viewport-edge while dragging stays out of scope
    (recorded in `HexGridPanel`'s class javadoc) - this was
    already a pre-existing, shipped divergence from C++'s
    `SetEndOfSelection`/`VScroll` behavior (`offsetAtPoint` just clamps),
    now simply shared by two callers instead of one, not a new gap.
  - Sector navigation still uses a `JSpinner` instead of a native
    scrollbar - unrelated, unchanged, still likely UX-equivalent/low risk.
- Verified with a clean `mvn -o compile`/`test-compile`, a full `TestRunner`
  run (13 tests, including a new `ByteRangeSelectionTest` covering the
  extracted swap/clamp arithmetic), and an ad hoc off-screen smoke test
  (scratch, not committed, per the porting guide's testing-strategy
  convention) that constructs `DiskImageSectorsDialog`, paints
  `HexGridPanel` with a synthetic `DiskSectorByteSource`, and
  confirms `offsetAtPoint` returns in-range offsets - full interactive
  drag-select/rendering verification in the real running app is still a
  manual follow-up.

### 7. ~~The main window's part-window headers are missing their C++ background coloring - and three of the five have no header at all~~ - FIXED 2026-09-21

- **Was**: C++'s `Main::PaintMainWindow` (`src/ui/Main.cpp:322-429`) paints a
  colored title bar above each of the five part windows (Segment List
  yellow, Disassembly green, Memory Inspector cyan, XRef light pink, Log
  light lavender - see the original write-up's exact `RGB()` values). In
  Java, only `MemoryInspectorPanel`/`XRefPanel` showed any header at all
  (a plain, uncolored `TitledBorder`); `SegmentListPanel`/`DisassemblyPanel`/
  `LogPanel` showed no header whatsoever, and the already-ported
  `Text.IDS_SEGMENT_TITLE*`/`IDS_DIS_TITLE`/`IDS_LOG_TITLE` resources were
  completely unused.
- **Fix**: new package-private `PartHeaderPanel` (`ui`) - a small
  `JComponent`, opaque, filling its whole background then painting text via
  `ComputerFont#drawText` (at `(1,1)`, matching `DC::ExtTextOut(1, 1, rc,
  title)`'s offset and its `ETO_OPAQUE` whole-rectangle fill) - added as a
  `BorderLayout.NORTH` child of all five part panels, each constructed with
  its C++ `RGB()` value as a `java.awt.Color`. The C++ source's
  focus-dependent black/gray memory-inspector text color was deliberately
  not replicated: that exact line carries its own `// TODO Detection of
  focus does not actually work.` comment, so it never actually returns
  anything but black in real C++ operation either - `PartHeaderPanel`
  always paints black text, matching real observed behavior rather than
  the broken-in-C++-too focus branch.
  - `SegmentListPanel`/`DisassemblyPanel`/`LogPanel` now wire up the
    previously-unused `Text.IDS_SEGMENT_TITLE`/
    `IDS_SEGMENT_TITLE_NO_SEGMENTS_LOADED`/`IDS_DIS_TITLE`/`IDS_LOG_TITLE`
    constants. `SegmentListPanel` gets a new `setFileName(String)`, called
    from `Dis6502.updateTitle()` (the same 11 call sites that already keep
    the main window's own frame title in sync with `currentFile`) - its
    `refresh()` recomputes the header text from the cached file name plus
    the segment list's current emptiness on every workspace change, so it
    self-corrects for segment-count changes that happen without a
    file-open/close event too (e.g. Disk Image Sectors' "Add Sector").
  - `MemoryInspectorPanel`/`XRefPanel` switched from a `TitledBorder` plus
    hand-written `String.format` literals to `PartHeaderPanel` plus the
    matching `Text.IDS_DUMP_TITLE_*`/`IDS_XREF_TITLE_*` resources - which
    surfaced and fixed two small pre-existing wording/fidelity divergences
    from the C++ resource text along the way: `MemoryInspectorPanel`'s
    "Selection" title never included the segment number the "Segment" title
    did, unlike `IDS_DUMP_TITLE_SELECTION`'s own `"Selection {0}: ..."`;
    `XRefPanel`'s hardcoded "No label selected"/"N Reference(s) to \"...\""
    didn't match the resource text's "No Label Selected"/"N reference(s)
    for \"...\"".
- Verified with a clean `mvn -o compile`/`test-compile`, the full
  `TestRunner` suite (13 tests, unaffected), and - since this environment
  has a real, non-headless display - an actual launch of the running app
  with a `java.awt.Robot` screenshot: all five headers render with the
  correct colors and text, matching the reference C++ screenshot exactly
  (including the corrected XRef wording).

### 8. Main window title never shows the current computer system's name

- **Found**: while auditing `Text.java` for constants no longer referenced
  anywhere in the Java source (2026-09-21) - `Text.IDS_MAIN_WINDOW_TITLE`/
  `IDS_MAIN_WINDOW_TITLE_NO_WORKSPACE_LOADED` are still real, actively-used
  C++ resources with no Java call site at all, unlike most of the other
  candidates that audit turned up (those had a working Java equivalent
  under a different mechanism - this one genuinely doesn't).
- **C++**: `Main::SetMainWindowTitle` (`src/ui/Main.cpp:89-101`) sets the
  title to `Text::Format(IDS_MAIN_WINDOW_TITLE_NO_WORKSPACE_LOADED,
  computerSystemText)` ("6502 Disassembler for {0}") when no file is
  loaded, or `Text::Format(IDS_MAIN_WINDOW_TITLE, computerSystemText,
  filePath)` ("6502 Disassembler for {0} {1}") once one is - always
  including `Workspace::GetComputerSystem()->GetTypeInfo()->text` (e.g.
  "Atari 800").
- **Java**: `Dis6502.updateTitle()` (`Dis6502.java:1748-1754`) just sets
  `"dis6502"` plus `" - " + currentFile.getName()` if a file is open - the
  computer system name is never shown at all, and the file portion uses
  just the file name rather than C++'s full path (a second, smaller
  divergence).
- **Fix sketch**: `workspace.getComputerSystem().getTypeInfo().text` is
  already used elsewhere in this exact class (`Dis6502.java:484`, in
  `performNewWorkspace`'s log message) - wiring it into `updateTitle()`
  (and deciding whether to keep the Java convention of a short file name
  vs. C++'s full path) should be a small, self-contained change. Not yet
  implemented as part of this audit since it wasn't the audit's purpose -
  listed here for a future session.

### 9. `DisassemblyProgressMonitor`'s base-class logging is still an unwired no-op

- **Found**: same `Text.java` audit as gap #8 -
  `Text.IDS_LOG_DISASSEMBLY_PROGRESS_MONITOR_INFO`/`_PASS`/`_SEGMENT` are
  real, actively-used C++ resources with no Java call site.
- **C++**: `DisassemblyProgressMonitor::SetPass`/`SetSegmentNumber`/
  `SendInfo` log through the global `Application` object.
- **Java**: `DisassemblyProgressMonitor.setPass`/`setSegmentNumber`/
  `sendInfo` (`model/DisassemblyProgressMonitor.java:45-53`) are no-ops by
  design - the class javadoc already documents this as deliberate,
  written before this port had an `Application`-based logging mechanism
  ("that is not ported yet, so they default to no-ops here - override
  them once application-level logging exists"). `Application` (with
  `sendMessage`/`sendInfoMessage`) has existed for a while now, so this
  TODO is actionable, just never revisited.
- **Caveat, lowering priority**: in C++ itself, the real GUI path
  (`DisassemblyProgressDialog`) overrides `SetPass`/`SetSegmentNumber` to
  update its own UI labels without ever calling into the logging base -
  only the plain `DisassemblyProgressMonitor` used directly by
  `MainTest.cpp` (C++'s unit-test harness) exercises the logging path at
  all, so this is a low real-world-impact gap, not a user-visible one.
- Not implemented as part of this audit - listed here for a future
  session; the existing javadoc TODO already documents the decision to
  defer it.

## Divergences where the Java port fixed a real C++ bug (not a Java gap)

Per the porting guide's bug-handling policy, these are intentional
divergences, not regressions - listed here so they aren't mistaken for
inconsistencies between the two codebases:

- **`DiskImageSectorsDialog` / `MainFile::OpenDiskImageSectors`**: an
  out-of-bounds read past an undersized buffer was found in the C++ logic.
  Java implements the evidently-correct behavior; the C++ side was left
  unchanged because confirming the fix needs interactive GUI testing that
  wasn't available (per the policy's "leave a TODO in C++, implement the
  correct behavior in Java" branch) - check `MainFile.cpp` for a `TODO:`
  comment describing this before assuming it's unnoticed.
- **`SegmentPropertiesDialog`**: carries forward a pre-existing C++ TODO
  about not supporting segments >64K faithfully (not a new Java bug).
- **`SegmentWriteBootDiskDialog`**: carries forward a pre-existing C++ TODO
  that `WriteAbsoluteSector` never reports write failure to the caller -
  documented per the bug-handling policy, not fixed (needs interactive GUI
  confirmation).

## Deliberate simplifications / architecture differences (not gaps)

Recorded so a future session doesn't "fix" something that was already a
reviewed, intentional decision:

- **`DisassemblyFindStringDialog.cpp`** has no direct Java counterpart - its
  modal find dialog was replaced by an always-visible inline find
  field/button directly on `DisassemblyPanel`
  (`performFindInDisassembly`/`performFindNextInDisassembly` in
  `Dis6502.java`). Functionally equivalent, deliberately different UI shape.
- **`FindStringDialog.java`** keeps the C++ name but is now a shared plain
  value/conversion helper (ASCII&#8596;hex sync) composed into
  `MemoryInspectorFindStringDialog`, not a dialog itself - mirrors how C++
  composes the same class into both `MemoryInspectorFindStringDialog` and
  `DisassemblyFindStringDialog`.
- **`Layout.cpp`** has no Java `Layout` class - its `Layout::Compute` logic
  was folded directly into `MainWindow.java`'s nested `JSplitPane`
  construction (see `plans/MEMORY.md`'s note on this).
- **`WorkspaceFont.cpp`** has no dedicated Java class - folded into
  `Dis6502.java`'s font-update logic (`setComputerFont` calls,
  `Dis6502.java:377`).
- **`MessageBoxDialog.cpp`** has no dedicated Java class - plain
  `JOptionPane` calls are used directly at each call site.
- **`SelectSpritesDialog.cpp` / `SpriteControl(Impl).cpp`** were deliberately
  renamed to `SelectGraphicsDialog` / `GraphicPanel` / `GraphicMode` (the
  renaming is explained in `SelectGraphicsDialog`'s own javadoc). This one
  appears fully and carefully ported, including mouse-drag row selection -
  not a gap despite the "sprite" name sounding exotic/rare.
- **`DiskImageSectorsController.cpp`, `EquateListController.cpp`,
  `ProfilesController.cpp`, `MainController.cpp`**'s dialog-scoped duties
  were folded into their owning dialog's own `performOK()`/handler methods,
  per the porting guide's explicit convention for simple, single-dialog
  controllers.
- **Win32 control/window wrapper base classes** (`Button.cpp`,
  `CheckBox.cpp`, `ComboBox.cpp`, `Control.cpp`, `DC.cpp`, `Dialog.cpp`,
  `EditControl.cpp`, `Font.cpp`, `ListBox.cpp`, `Menu.cpp`, `PartWindow.cpp`,
  `PopupMenu.cpp`, `RadioButtonGroup.cpp`, `ScrollBar.cpp`, `TextLabel.cpp`,
  `Window.cpp`) have no Java counterparts - Swing provides these natively.
  Not gaps.
- **`utils.cpp`** (1994 Microsoft sample-code DIB/palette utilities used only
  by the Win32 sprite bitmap rendering) is superseded by `GraphicPanel`'s
  `BufferedImage`-based approach. Not a gap.
- **`MRUMenu.cpp`** was folded into `MRUController.java` +
  `MainMenu.java`'s `recentFilesMenu`/`recentWorkspacesMenu`, fully wired in
  `Dis6502.java` - verified complete.
- **`ByteArray.h`/`ByteSequence.h`/`Byte.h`** &rarr; native Java `byte[]`
  throughout.
- **`File.h`/`FileIO.h`/`FileInputStream.h`/`InputStream.h`/`OutputStream.h`/
  `Stream.h`/`Writer.h`** &rarr; `java.io.*` plus WUDSN-Base's
  `com.wudsn.tools.base.common.FileUtility`.
- **`DisassemblyBuffer.h`** (`DIS_BUFFER`, a manual linked-list byte-packing
  scheme) and **`DisassemblyResultIterators.h`** (custom iterators over it)
  &rarr; confirmed superseded: `DisassemblyResult.java` uses a plain
  `List<DisassemblySectionType>` and standard Java collections; no
  packed-buffer structure exists anywhere in the Java model.
- **`MemoryInspectorSelection.h`** &rarr; merged into
  `MemoryInspectorState`/`MutableMemoryInspectorState.getByteSequence()`
  (returns `byte[]` directly) - matches the merge already documented in
  `plans/MEMORY.md`.
- **`DatatypeUtility.h`** (centralized hex-string conversion) is not
  centralized in Java; equivalent hex-string logic is distributed across
  `Memory.java`, `Segment.java`, `Equate.java`, `Disassembly*.java`,
  `Xml.java` individually. Functionally covered, architecturally different -
  a divergence from the usual 1:1 file match, not a functional gap.
- **`XRef.h`** is a trivial one-line typedef (`LineNumber = size_t`) with no
  porting need.
- **`GuessCodeLogic.java`** has no matching C++ model file by name - its
  logic actually originates from `ui/MemoryInspector.cpp`'s "guess code"
  feature; the Java port deliberately relocated it from the UI layer into
  the model layer. Not a gap.

## Fully ported, spot-verified

**Model/logic layer**: `AddressLabel(List)`, `Assembler`, `Comment`,
`ComputerSystem`/`ComputerSystemFactory`/`ComputerSystemType(Info)`,
`DefaultFolders(Logic)`, `Disassembly`, `DisassemblyLine`/`LineWriter`/
`OpcodeBuffer`/`ProgressMonitor`/`Result`/`ResultFile`/`ResultWriter`/
`Section(Type)`/`Writer`, `DiskImage`, `Encoding`, `Equate`/`EquateList`/
`EquateListLogic`/`EquateType`, `FileHeader`/`FileType`/`FolderType`,
`Fixup`/`FixupType`, `InstructionSet` (split into `InstructionSetMOS6502` /
`InstructionSetMOS65C02` - elaboration, not a gap), `LabelAccess`,
`MRUEntry`/`MRUList`, `Memory`/`MemoryBlock`/`MemoryBlockIterator`,
`MemoryInspectorStack`, `Pass1`, `ProcessorType`, `Profile`/`Profile1X`/
`ProfileLogic`, `Segment`/`SegmentList`/`SegmentListInserter`, `Symbol`,
`Workspace`/`Workspace1X`/`WorkspaceLogic`, `Xml`. All per-computer-system
classes (`Atari800`, `AtariDOS`, `AtariDiskImage`, `C64`, `Oric`, `Unknown`,
`Atari5200`) are present with method counts roughly matching their C++
counterparts.

**UI layer**: `AboutDialog`, `AssembleDialog`, `CommentDialog`,
`ComputerFont`, `DefaultFoldersDialog`, `DisassemblyProgressDialog`,
`DiskImageExecutableFileDialog` (looks complete, not deep-diffed
field-by-field), `EquateDialog`, `EquateRangeDialog`, `LowHighByteDialog`,
`MRUController`, `MainMenu`, `MainWindow` (layout verified against
`Layout::Compute` per `plans/MEMORY.md`), `ProfileDialog`, `RawFileDialog`,
`SegmentPropertiesDialog`, `SegmentWriteBootDiskDialog`, `UIApplication`,
`WorkspaceDialog`. The completed Actions/ElementFactory and popup-menu
accelerator migrations (`plans/ACTIONS_ELEMENT_FACTORY_MIGRATION.md`,
`plans/POPUP_MENU_ACCELERATORS_PLAN.md`) were spot-checked clean - no
literal `JButton("OK")`-style stragglers, no direct `setAccelerator` calls
bypassing the `InputMap`/`ActionMap` pattern.

**Opcode/addressing-mode table parity check - completed 2026-09-21, no gap
found.** `InstructionSetMOS6502.java` and `InstructionSetMOS65C02.java` were
diffed entry-by-entry (all 512 opcode rows combined) against C++'s
`InstructionSet.cpp`: mnemonic, `illegal` flag, `LabelAccess`, and
`OperandMode` all match exactly for every opcode in both tables. The
supporting `Instruction` class (`getLength()`/`isImmediateMode()`/
`isUnsupportedInstruction()`) also matches C++'s `Instruction::GetLength()`
etc. one-for-one, including a shared quirk: `GetLength()`'s `switch` never
gained cases for 65C02-only `OperandMode`s (`ZeroPageIndirect`,
`ZeroPageRelative`, `IndexedIndirectAbsolute`, `ReservedNop1Byte`/`2Byte`/
`3Byte`), falling through to its `default: return 1` for all of them in
*both* languages - but this is harmless in both, since actual instruction
length for those modes is computed by dedicated `OperandMode` switches
directly in `Disassembly.cpp`/`Disassembly.java` (confirmed present for all
six modes in both files), never through `Instruction::GetLength()`/
`getLength()`. Not a gap - `InstructionSet`'s own `GetLength()` is simply
unused for these modes in both codebases.

## Explicitly unverified - needs a follow-up pass

These were not checked deeply enough to classify as either "fine" or "a
gap" - flag them for a dedicated follow-up rather than assuming either way:

- **`Workspace1X`/`Profile1X`** (legacy-format loaders) - classes exist with
  plausible content, but not every legacy field/quirk C++ handles was
  verified as preserved.
- **`ImgError`/`ImgInfo`/`ImgRWPacket`/`AtariError`/`AtariFile`** - these
  Java classes have no like-named C++ header; they appear to be the Java
  port's own decomposition of `AtariDiskImage`/`AtariDOS`'s C-style
  output-parameter structs into proper value objects (an architecture
  improvement), but 1:1 field/error-code parity against the C++
  enums/structs was not verified.
- **`Application.h`/`ApplicationSettingsSection.h`** (the abstract C++ app
  class: settings-file sections, `SendInfoMessage`/`SendErrorMessage`/
  `ThrowErrorMessage` with Text IDs) has no `model`-package Java file, but an
  `application.sendInfoMessage(...)`/`sendErrorMessage(...)` abstraction
  clearly exists and is actively used (see gap #2 above) - it must live in
  the `ui` package. Full parity (settings persistence, every message
  variant) was not verified.
- **`OperatingSystem.h`** (`ExecuteCommand` - shell/process execution) has no
  Java model counterpart; likely superseded by `java.awt.Desktop`/
  `ProcessBuilder` somewhere in `ui`, not verified.
- **`DiskImageExecutableFileDialog`** and several of the "fully ported"
  dialogs listed above were confirmed present and non-trivial in size/content
  but not verified command-by-command against the C++ `.rc` resource /
  `ProcessCommand` switch.
- **`MainWindowMenu.cpp`'s per-item icon loading** (`AddIconToMenu`, 26
  call sites) appears present in `MainMenu.java` (29 `setIcon`/`Icon(`
  occurrences) but wasn't spot-verified icon-by-icon.

## Shared C++ limitations - not Java-specific gaps

These exist in the C++ source itself, so their presence/absence in Java
isn't a porting gap - don't "fix" the Java side to add something C++ itself
doesn't support, without a separate decision to add a genuinely new feature:

- **Atari5200 bank-switched cartridges/.CAR files** are not supported in
  C++ either - `Atari5200.cpp:79` and `Atari5200.java:59` carry the
  identical comment.
- **`tst/suite/disassembly/unit005`** (an SDX relocation-base test) is
  disabled in C++ itself - `MainTest.cpp:330` comments it out with
  `// TODO SDX does not work yet because of using $0000 as relocation base address`.
  Its absence from Java test coverage isn't a port gap.
- **`ComputerSystemTest`** in both languages only exercises Atari800 + C64 -
  C++'s own `ComputerSystemTest.cpp:10-11` never calls a
  `TestOric`/`TestAtari5200`/`TestUnknown`. Java parity confirmed
  (`ComputerSystemTest.java` also only has `testC64`); not a Java-specific
  coverage gap.
- **C++-only test classes `CommonTest`, `StreamTest`, `FileIOTest`** have no
  Java counterpart - consistent with the I/O-abstraction supersession noted
  above (`java.io.*` + WUDSN-Base), not a gap.

## Suggested priority order for closing these

1. ~~**Gap #1** (`DisassemblyWriter` return character)~~ - **fixed
   2026-09-21**, see above.
2. ~~**Gap #2** (`EquateList` swallowed parse errors)~~ - **fixed
   2026-09-21**, see above.
3. ~~**Opcode table parity check**~~ - **completed 2026-09-21, no gap
   found**, see above.
4. ~~**Gap #6** (`DiskImageSectorsDialog` drag-select)~~ - **fixed
   2026-09-21**, see above.
5. **Gap #3** (Memory Inspector Delete/Cut/Paste Selection) - explicitly
   blocked on `MemoryBlock` gaining real resize support; needs a scoping
   decision (faithful-but-broken port vs. a fixed reimplementation) before
   any code is written, per the porting guide's process-lesson rule.
6. ~~**Gap #7** (part-window header coloring, and three panels missing a
   header entirely)~~ - **fixed 2026-09-21**, see above.
7. **Gap #4** (`LogPanel` columns/coloring) - cosmetic, low risk, low
   urgency.
8. **Gap #5** (`MainUITest.cpp` self-test harness) - needs an explicit
   "superseded by JUnit, won't port" decision recorded somewhere (this
   document or a class javadoc) rather than staying an implicit gap.
9. **Gap #8** (main window title missing computer system name) - small,
   self-contained fix; found 2026-09-21 while auditing `Text.java` for
   unreferenced constants.
10. **Gap #9** (`DisassemblyProgressMonitor` logging never wired up) -
    low real-world impact (C++'s own GUI path barely exercises it either);
    found alongside gap #8.
